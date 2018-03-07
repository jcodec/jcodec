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

import org.jcodec.codecs.aac.AACConts;
import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.codecs.aac.ADTSParser.Header;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg12.MPEGES;
import org.jcodec.codecs.mpeg12.SegmentReader;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.IntIntHistogram;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.TrackType;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;

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

        @Override
        public int getSid() {
            return streamId;
        }

        public void pending(PESPacket pkt) {
            if (_pending != null)
                _pending.add(pkt);
            else
                demuxer.putBack(pkt.data);
        }

        @Override
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
        // PTS estimation machinery
        private LongArrayList ptsSeen;
        private long lastPts;
        private int lastSeq;
        private int lastSeqSeen;
        private int seqWrap;
        private IntIntHistogram durationHistogram;

        public MPEGTrack(MPSDemuxer demuxer, int streamId, PESPacket pkt) throws IOException {
            super(demuxer, streamId, pkt);
            this.es = new MPEGES(this, 4096);
            ptsSeen = new LongArrayList(32);
            lastSeq = Integer.MIN_VALUE;
            lastSeqSeen = Integer.MAX_VALUE - 1000;
            seqWrap = Integer.MAX_VALUE - 1000;
            durationHistogram = new IntIntHistogram();
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
                        ptsSeen.add(pkt.pts);
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
            return es.frame(buf);
        }

        @Override
        public Packet nextFrame() throws IOException {
            MPEGPacket pkt = es.getFrame();
            if (pkt == null)
                return null;
            int seq = MPEGDecoder.getSequenceNumber(pkt.getData());
            if (seq == 0)
                seqWrap = lastSeqSeen + 1;
            lastSeqSeen = seq;
            if (ptsSeen.size() <= 0) {
                pkt.setPts(Math.min(seq - lastSeq, seq - lastSeq + seqWrap) * durationHistogram.max() + lastPts);
            } else {
                pkt.setPts(ptsSeen.shift());
                if (lastSeq >= 0 && seq > lastSeq) {
                    durationHistogram.increment((int) (pkt.getPts() - lastPts)
                            / Math.min(seq - lastSeq, seq - lastSeq + seqWrap));
                }
                lastPts = pkt.getPts();
                lastSeq = seq;
            }
            pkt.setDuration(durationHistogram.max());
            System.out.println(seq);
            return pkt;
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            return null;
        }
    }

    public static class PlainTrack extends BaseTrack {
        private int frameNo;
        private Packet lastFrame;
        private long lastKnownDuration = 3003; // Dummy value that matches video

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
            return pkt == null ? null : Packet.createPacket(pkt.data, pkt.pts, 90000, 0, frameNo++, FrameType.UNKNOWN,
                    null);
        }

        @Override
        public Packet nextFrame() throws IOException {
            if (lastFrame == null)
                lastFrame = nextFrameWithBuffer(null);
            if (lastFrame == null)
                return null;
            Packet toReturn = lastFrame;
            lastFrame = nextFrameWithBuffer(null);
            if (lastFrame != null) {
                lastKnownDuration = lastFrame.getPts() - toReturn.getPts();
            }
            toReturn.setDuration(lastKnownDuration);

            return toReturn;
        }

        public DemuxerTrackMeta getMeta() {
            TrackType t = videoStream(streamId) ? VIDEO : (audioStream(streamId) ? AUDIO : OTHER);
            return null;
        }
    }

    public static class AACTrack extends PlainTrack {
        private List<Packet> audioStash;

        public AACTrack(MPSDemuxer demuxer, int streamId, PESPacket pkt) throws IOException {
            super(demuxer, streamId, pkt);
            audioStash = new ArrayList<Packet>();
        }

        @Override
        public Packet nextFrame() throws IOException {
            if (audioStash.size() == 0) {
                Packet nextFrame = nextFrameWithBuffer(null);
                if (nextFrame != null) {
                    ByteBuffer data = nextFrame.getData();
                    Header adts = ADTSParser.read(data.duplicate());
                    long nextPts = nextFrame.getPts();
                    while (data.hasRemaining()) {
                        ByteBuffer data2 = NIOUtils.read(data, adts.getSize());
                        Packet pkt = Packet.createPacketWithData(nextFrame, data2);
                        pkt.setDuration((pkt.getTimescale() * 1024)
                                / AACConts.AAC_SAMPLE_RATES[adts.getSamplingIndex()]);
                        pkt.setPts(nextPts);
                        nextPts += pkt.getDuration();
                        audioStash.add(pkt);
                        if (data.hasRemaining())
                            adts = ADTSParser.read(data.duplicate());
                    }
                }
            }
            if (audioStash.size() == 0)
                return null;
            return audioStash.remove(0);
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
            else if (isAAC(pkt.data)) {
                pes = new AACTrack(this, pkt.streamId, pkt);
            } else
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

    private boolean isAAC(ByteBuffer _data) {
        Header read = ADTSParser.read(_data.duplicate());
        return read != null;
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

    @UsedViaReflection
    public static int probe(ByteBuffer b_) {
        ByteBuffer b = b_.duplicate();
        int marker = 0xffffffff;
        int sliceSize = 0;

        boolean videoPes = false;
        int state = 0;
        int errors = 0;
        boolean inNALUnit = false;
        List<NALUnit> nuSeq = new ArrayList<NALUnit>();
        while (b.hasRemaining()) {
            int code = b.get() & 0xff;
            if (state >= 3) {
                sliceSize ++;
            }
            marker = (marker << 8) | code;

            if (inNALUnit) {
                NALUnit nu = NALUnit.read(asByteBufferInt(code));
                if (nu.type != null)
                    nuSeq.add(nu);
                inNALUnit = false;
            }

            if (videoPes && marker == 0x1) {
                inNALUnit = true; // h.264 case
                continue;
            } else if (marker < 0x100 || marker > 0x1ff)
                continue;
            
            // PS marker
            if(marker >= 0x1ba) {
                videoPes = MPSUtils.videoMarker(marker);
                continue;
            }
            
            if (!videoPes)
                continue;
            
            boolean stop = false;
            switch(state) {
            case 0:
                if (marker >= 0x1B0 && marker <= 0x1B8)
                    state = 1;
                else if(marker == 0x100)
                    state = 2;
                else
                    state = 0;
                break;
            case 1:
                if(marker == 0x100)
                    state = 2;
                else if (marker >= 0x1B0 && marker <= 0x1B8)
                    state = 1;
                else
                    errors ++;
                break;
            case 2:
                if (marker == 0x101)
                    state = 3;
                else if(marker == 0x1B5 || marker == 0x1B2)
                    state = 2;
                else
                    errors ++;
                break;
            default:
                if (state > 3 && sliceSize < 1) {
                    errors ++;
                }
                sliceSize = 0;
                if (state - 1 == marker - 0x100)
                    state = marker - 0x100 + 2;
                else if(marker == 0x100 || marker >= 0x1B0)
                    stop = true;
            }
            if(stop)
                break;
        }
        return Math.max(rateSeq(nuSeq), state >= 3 ? 100 / (1 + errors) : 0);
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
                    score += 20;
                hasSlice = true;
            }
        }
        return score;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}