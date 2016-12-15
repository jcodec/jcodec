package org.jcodec.containers.mps;

import static org.jcodec.codecs.h264.io.model.NALUnitType.IDR_SLICE;
import static org.jcodec.codecs.h264.io.model.NALUnitType.NON_IDR_SLICE;
import static org.jcodec.codecs.h264.io.model.NALUnitType.PPS;
import static org.jcodec.codecs.h264.io.model.NALUnitType.SPS;
import static org.jcodec.common.TrackType.AUDIO;
import static org.jcodec.common.TrackType.OTHER;
import static org.jcodec.common.TrackType.VIDEO;
import static org.jcodec.common.io.NIOUtils.asByteBufferInt;
import static org.jcodec.containers.mps.MPSUtils.audioStream;
import static org.jcodec.containers.mps.MPSUtils.psMarker;
import static org.jcodec.containers.mps.MPSUtils.readPESHeader;
import static org.jcodec.containers.mps.MPSUtils.videoStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.mpeg12.MPEGES;
import org.jcodec.codecs.mpeg12.SegmentReader;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer for MPEG Program Stream format
 * 
 * @author The JCodec project
 * 
 */
public class MPSDemuxer extends SegmentReader implements MPEGDemuxer {
    private static final int BUFFER_SIZE = 0x100000;

    private Map<Integer, BaseTrack> streams;
    private ReadableByteChannel channel;
    private List<ByteBuffer> bufPool;

    public MPSDemuxer(ReadableByteChannel channel) throws IOException {
        super(channel, 4096);
        this.streams = new HashMap<Integer, BaseTrack>();
        this.channel = channel;
        this.bufPool = new ArrayList<ByteBuffer>();

        findStreams();
    }

    protected void findStreams() throws IOException {
        for (int i = 0; i == 0 || i < 5 * streams.size() && streams.size() < 2; i++) {
            PESPacket nextPacket = nextPacket(getBuffer());
            if (nextPacket == null)
                break;
            addToStream(nextPacket);
        }
    }

    public static class PESPacket {
        public ByteBuffer data;
        public long pts;
        public int streamId;
        public int length;
        public long pos;
        public long dts;

        public PESPacket(ByteBuffer data, long pts, int streamId, int length, long pos, long dts) {
            this.data = data;
            this.pts = pts;
            this.streamId = streamId;
            this.length = length;
            this.pos = pos;
            this.dts = dts;
        }
    }

    public ByteBuffer getBuffer() {
        synchronized (bufPool) {
            if (bufPool.size() > 0) {
                return bufPool.remove(0);
            }
        }
        return ByteBuffer.allocate(BUFFER_SIZE);
    }

    public void putBack(ByteBuffer buffer) {
        buffer.clear();
        synchronized (bufPool) {
            bufPool.add(buffer);
        }
    }

    public static abstract class BaseTrack implements MPEGDemuxer.MPEGDemuxerTrack {
        protected int streamId;
        protected List<PESPacket> _pending;
        protected MPSDemuxer demuxer;

        public BaseTrack(MPSDemuxer demuxer, int streamId, PESPacket pkt) throws IOException {
            this._pending = new ArrayList<PESPacket>();
            this.demuxer = demuxer;
            this.streamId = streamId;
            this._pending.add(pkt);
        }

        public int getSid() {
            return streamId;
        }

        public void pending(PESPacket pkt) {
            if (_pending != null)
                _pending.add(pkt);
            else
                demuxer.putBack(pkt.data);
        }

        public List<PESPacket> getPending() {
            return _pending;
        }

        @Override
        public void ignore() {
            if (_pending == null)
                return;
            for (PESPacket pesPacket : _pending) {
                demuxer.putBack(pesPacket.data);
            }
            _pending = null;
        }
    }

    public static class MPEGTrack extends BaseTrack implements ReadableByteChannel {

        private MPEGES es;

        public MPEGTrack(MPSDemuxer demuxer, int streamId, PESPacket pkt) throws IOException {
            super(demuxer, streamId, pkt);
            this.es = new MPEGES(this, 4096);
        }

        public boolean isOpen() {
            return true;
        }

        public MPEGES getES() {
            return es;
        }

        public void close() throws IOException {
        }

        public int read(ByteBuffer arg0) throws IOException {
            PESPacket pes = _pending.size() > 0 ? _pending.remove(0) : getPacket();
            if (pes == null || !pes.data.hasRemaining())
                return -1;
            int toRead = Math.min(arg0.remaining(), pes.data.remaining());
            arg0.put(NIOUtils.read(pes.data, toRead));

            if (pes.data.hasRemaining())
                _pending.add(0, pes);
            else
                demuxer.putBack(pes.data);

            return toRead;
        }

        private PESPacket getPacket() throws IOException {
            if (_pending.size() > 0)
                return _pending.remove(0);
            PESPacket pkt;
            while ((pkt = demuxer.nextPacket(demuxer.getBuffer())) != null) {
                if (pkt.streamId == streamId) {
                    if (pkt.pts != -1) {
                        es.curPts = pkt.pts;
                    }
                    return pkt;
                } else {
                    demuxer.addToStream(pkt);
                }
            }
            return null;
        }

        @Override
        public Packet nextFrameWithBuffer(ByteBuffer buf) throws IOException {
            return es.getFrame(buf);
        }
        
        @Override
        public Packet nextFrame() throws IOException {
            return es.getFrame();
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            TrackType t = videoStream(streamId) ? VIDEO : (audioStream(streamId) ? AUDIO : OTHER);
            return new DemuxerTrackMeta(t, null, null, 0, 0, null, null);
        }
    }

    public static class PlainTrack extends BaseTrack {
        private int frameNo;

        public PlainTrack(MPSDemuxer demuxer, int streamId, PESPacket pkt) throws IOException {
            super(demuxer, streamId, pkt);
        }

        public boolean isOpen() {
            return true;
        }

        public void close() throws IOException {
        }

        @Override
        public Packet nextFrameWithBuffer(ByteBuffer buf) throws IOException {
            PESPacket pkt;
            if (_pending.size() > 0) {
                pkt = _pending.remove(0);
            } else {
                while ((pkt = demuxer.nextPacket(demuxer.getBuffer())) != null && pkt.streamId != streamId)
                    demuxer.addToStream(pkt);
            }
            return pkt == null ? null : Packet.createPacket(pkt.data, pkt.pts, 90000, 0, frameNo++, true, null);
        }
        
        @Override
        public Packet nextFrame() throws IOException {
            return nextFrameWithBuffer(null);
        }

        public DemuxerTrackMeta getMeta() {
            TrackType t = videoStream(streamId) ? VIDEO : (audioStream(streamId) ? AUDIO : OTHER);
            return new DemuxerTrackMeta(t, null, null, 0, 0, null, null);
        }
    }

    public void reset() {
        for (BaseTrack track : streams.values()) {
            track._pending.clear();
        }
    }

    private void addToStream(PESPacket pkt) throws IOException {
        BaseTrack pes = streams.get(pkt.streamId);
        if (pes == null) {
            if (isMPEG(pkt.data))
                pes = new MPEGTrack(this, pkt.streamId, pkt);
            else
                pes = new PlainTrack(this, pkt.streamId, pkt);
            streams.put(pkt.streamId, pes);
        } else {
            pes.pending(pkt);
        }
    }

    public PESPacket nextPacket(ByteBuffer out) throws IOException {
        ByteBuffer dup = out.duplicate();

        while (!psMarker(curMarker)) {
            if (!skipToMarker())
                return null;
        }

        ByteBuffer fork = dup.duplicate();
        readToNextMarker(dup);
        PESPacket pkt = readPESHeader(fork, curPos());
        if (pkt.length == 0) {
            while (!psMarker(curMarker) && readToNextMarker(dup))
                ;
        } else {
            read(dup, pkt.length - dup.position() + 6);
        }
        fork.limit(dup.position());
        pkt.data = fork;
        return pkt;
    }

    public List<MPEGDemuxerTrack> getTracks() {
        return new ArrayList<MPEGDemuxerTrack>(streams.values());
    }

    public List<MPEGDemuxerTrack> getVideoTracks() {
        List<MPEGDemuxerTrack> result = new ArrayList<MPEGDemuxerTrack>();
        for (BaseTrack p : streams.values()) {
            if (videoStream(p.streamId))
                result.add(p);
        }
        return result;
    }

    public List<MPEGDemuxerTrack> getAudioTracks() {
        List<MPEGDemuxerTrack> result = new ArrayList<MPEGDemuxerTrack>();
        for (BaseTrack p : streams.values()) {
            if (audioStream(p.streamId))
                result.add(p);
        }
        return result;
    }

    private boolean isMPEG(ByteBuffer _data) {
        ByteBuffer b = _data.duplicate();
        int marker = 0xffffffff;

        int score = 0;
        boolean hasHeader = false, slicesStarted = false;
        while (b.hasRemaining()) {
            int code = b.get() & 0xff;
            marker = (marker << 8) | code;
            if (marker < 0x100 || marker > 0x1b8)
                continue;

            if (marker >= 0x1B0 && marker <= 0x1B8) {
                if ((hasHeader && marker != 0x1B5 && marker != 0x1B2) || slicesStarted)
                    break;
                score += 5;
            } else if (marker == 0x100) {
                if (slicesStarted)
                    break;
                hasHeader = true;
            } else if (marker > 0x100 && marker < 0x1B0) {
                if (!hasHeader)
                    break;
                if (!slicesStarted) {
                    score += 50;
                    slicesStarted = true;
                }
                score += 1;
            }
        }
        return score > 50;
    }

    public static int probe(ByteBuffer b) {
        int marker = 0xffffffff;

        int score = 0;
        boolean inVideoPes = false, hasHeader = false, slicesStarted = false, inNALUnit = false;
        List<NALUnit> nuSeq = new ArrayList<NALUnit>();
        while (b.hasRemaining()) {
            int code = b.get() & 0xff;
            marker = (marker << 8) | code;

            if (inNALUnit) {
                NALUnit nu = NALUnit.read(asByteBufferInt(code));
                if (nu.type != null)
                    nuSeq.add(nu);
                inNALUnit = false;
            }

            if (inVideoPes && marker == 0x1) {
                inNALUnit = true; // h.264 case
                continue;
            } else if (marker < 0x100 || marker > 0x1ff)
                continue;

            if (MPSUtils.videoMarker(marker)) {
                if (inVideoPes)
                    break;
                else
                    inVideoPes = true;
            } else if (marker >= 0x1B0 && marker <= 0x1B8 && inVideoPes) {
                if ((hasHeader && marker != 0x1B5 && marker != 0x1B2) || slicesStarted)
                    break;
                score += 5;
            } else if (marker == 0x100 && inVideoPes) {
                if (slicesStarted)
                    break;
                hasHeader = true;
            } else if (marker > 0x100 && marker < 0x1B0) {
                if (!hasHeader)
                    continue;
                if (!slicesStarted) {
                    score += 50;
                    slicesStarted = true;
                }
                score += 1;
            }
        }

        return !nuSeq.isEmpty() ? rateSeq(nuSeq) : score;
    }

    private static int rateSeq(List<NALUnit> nuSeq) {
        int score = 0;
        boolean hasSps = false, hasPps = false, hasSlice = false;
        for (NALUnit nalUnit : nuSeq) {
            if (SPS == nalUnit.type) {
                if (hasSps && !hasSlice)
                    score -= 30;
                else
                    score += 30;
                hasSps = true;
            } else if (PPS == nalUnit.type) {
                if (hasPps && !hasSlice)
                    score -= 30;
                if (hasSps)
                    score += 20;
                hasPps = true;
            } else if (IDR_SLICE == nalUnit.type || NON_IDR_SLICE == nalUnit.type) {
                if (!hasSlice)
                    score += 50;
                hasSlice = true;
            } else {
                score += 3;
            }
        }
        return score;
    }

    public static class MPEGPacket extends Packet {
        private long offset;
        private ByteBuffer seq;
        private int gop;
        private int timecode;

        public MPEGPacket(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean keyFrame,
                TapeTimecode tapeTimecode) {
            super(data, pts, timescale, duration, frameNo, keyFrame, tapeTimecode, 0);
        }

        public long getOffset() {
            return offset;
        }

        public ByteBuffer getSeq() {
            return seq;
        }

        public int getGOP() {
            return gop;
        }

        public int getTimecode() {
            return timecode;
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}