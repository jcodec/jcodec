package org.jcodec.containers.mps;

import static org.jcodec.common.DemuxerTrackMeta.Type.AUDIO;
import static org.jcodec.common.DemuxerTrackMeta.Type.OTHER;
import static org.jcodec.common.DemuxerTrackMeta.Type.VIDEO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.codecs.mpeg12.MPEGES;
import org.jcodec.codecs.mpeg12.SegmentReader;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
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
    public static final int VIDEO_MIN = 0x1E0;
    public static final int VIDEO_MAX = 0x1EF;

    public static final int AUDIO_MIN = 0x1C0;
    public static final int AUDIO_MAX = 0x1DF;

    public static final int PRIVATE_1 = 0x1BD;
    public static final int PRIVATE_2 = 0x1BF;

    private Map<Integer, BaseTrack> streams = new HashMap<Integer, BaseTrack>();
    private SeekableByteChannel channel;

    public MPSDemuxer(SeekableByteChannel channel) throws IOException {
        super(channel);
        this.channel = channel;
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

        public PESPacket(ByteBuffer data, long pts, int streamId, int length, long pos) {
            this.data = data;
            this.pts = pts;
            this.streamId = streamId;
            this.length = length;
            this.pos = pos;
        }
    }

    private List<ByteBuffer> bufPool = new ArrayList<ByteBuffer>();

    public ByteBuffer getBuffer() {
        synchronized (bufPool) {
            if (bufPool.size() > 0) {
                return bufPool.remove(0);
            }
        }
        System.out.println("creating buffer");
        return ByteBuffer.allocate(BUFFER_SIZE);
    }

    public void putBack(ByteBuffer buffer) {
        buffer.clear();
        synchronized (bufPool) {
            bufPool.add(buffer);
        }
    }

    public abstract class BaseTrack implements MPEGDemuxer.MPEGDemuxerTrack {
        protected int streamId;
        protected List<PESPacket> pending = new ArrayList<PESPacket>();

        public BaseTrack(int streamId, PESPacket pkt) throws IOException {
            this.streamId = streamId;
            this.pending.add(pkt);
        }

        public int getSid() {
            return streamId;
        }

        public void pending(PESPacket pkt) {
            if (pending != null)
                pending.add(pkt);
            else
                putBack(pkt.data);
        }

        public List<PESPacket> getPending() {
            return pending;
        }

        @Override
        public void ignore() {
            if(pending == null)
                return;
            for (PESPacket pesPacket : pending) {
                putBack(pesPacket.data);
            }
            pending = null;
        }
    }

    public class MPEGTrack extends BaseTrack implements ReadableByteChannel {

        private MPEGES es;

        public MPEGTrack(int streamId, PESPacket pkt) throws IOException {
            super(streamId, pkt);
            this.es = new MPEGES(this);
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
            PESPacket pes = pending.size() > 0 ? pending.remove(0) : getPacket();
            if (pes == null || !pes.data.hasRemaining())
                return -1;
            int toRead = Math.min(arg0.remaining(), pes.data.remaining());
            arg0.put(NIOUtils.read(pes.data, toRead));

            if (pes.data.hasRemaining())
                pending.add(0, pes);
            else
                putBack(pes.data);

            return toRead;
        }

        private PESPacket getPacket() throws IOException {
            if (pending.size() > 0)
                return pending.remove(0);
            PESPacket pkt;
            while ((pkt = nextPacket(getBuffer())) != null) {
                if (pkt.streamId == streamId) {
                    if (pkt.pts != -1) {
                        es.curPts = pkt.pts;
                    }
                    return pkt;
                } else
                    addToStream(pkt);
            }
            return null;
        }

        @Override
        public Packet nextFrame(ByteBuffer buf) throws IOException {
            return es.getFrame(buf);
        }

        public DemuxerTrackMeta getMeta() {
            return new DemuxerTrackMeta(videoStream(streamId) ? VIDEO : (audioStream(streamId) ? AUDIO : OTHER), null,
                    0, 0, null);
        }
    }

    public class PlainTrack extends BaseTrack {
        private int frameNo;

        public PlainTrack(int streamId, PESPacket pkt) throws IOException {
            super(streamId, pkt);
        }

        public boolean isOpen() {
            return true;
        }

        public void close() throws IOException {
        }

        public Packet nextFrame(ByteBuffer buf) throws IOException {
            PESPacket pkt;
            if (pending.size() > 0) {
                pkt = pending.remove(0);
            } else {
                while ((pkt = nextPacket(getBuffer())) != null && pkt.streamId != streamId)
                    addToStream(pkt);
            }
            return pkt == null ? null : new Packet(pkt.data, pkt.pts, 90000, 0, frameNo++, true, null);
        }

        public DemuxerTrackMeta getMeta() {
            return new DemuxerTrackMeta(videoStream(streamId) ? VIDEO : (audioStream(streamId) ? AUDIO : OTHER), null,
                    0, 0, null);
        }
    }

    public void seekByte(long offset) throws IOException {
        channel.position(offset);
        reset();
    }

    public void reset() {
        for (BaseTrack track : streams.values()) {
            track.pending.clear();
        }
    }

    private void addToStream(PESPacket pkt) throws IOException {
        BaseTrack pes = streams.get(pkt.streamId);
        if (pes == null) {
            if (isMPEG(pkt.data))
                pes = new MPEGTrack(pkt.streamId, pkt);
            else
                pes = new PlainTrack(pkt.streamId, pkt);
            streams.put(pkt.streamId, pes);
        } else {
            pes.pending(pkt);
        }
    }

    public PESPacket nextPacket(ByteBuffer out) throws IOException {
        ByteBuffer dup = out.duplicate();

        while (curMarker < PRIVATE_1 || curMarker > VIDEO_MAX) {
            if (!skipToMarker())
                return null;
        }

        ByteBuffer fork = dup.duplicate();
        readToNextMarker(dup);
        PESPacket pkt = readPES(fork, curPos());
        if (pkt.length == 0) {
            while ((curMarker < PRIVATE_1 || curMarker > VIDEO_MAX) && readToNextMarker(dup))
                ;
        } else {
            read(dup, pkt.length - dup.position() + 6);
        }
        fork.limit(dup.position());
        pkt.data = fork;
        return pkt;
    }

    public static PESPacket readPES(ByteBuffer iss, long pos) {
        int streamId = iss.getInt() & 0xff;
        int len = iss.getShort();
        int b0 = iss.get() & 0xff;
        if ((b0 & 0xc0) == 0x80)
            return mpeg2Pes(b0, len, streamId, iss, pos);
        else
            return mpeg1Pes(b0, len, streamId, iss, pos);
    }

    public static PESPacket mpeg1Pes(int b0, int len, int streamId, ByteBuffer is, long pos) {
        int c = b0;
        while (c == 0xff) {
            c = is.get() & 0xff;
        }

        if ((c & 0xc0) == 0x40) {
            is.get();
            c = is.get() & 0xff;
        }
        long pts = -1, dts = -1;
        if ((c & 0xf0) == 0x20) {
            pts = readTs(is, c);
        } else if ((c & 0xf0) == 0x30) {
            pts = readTs(is, c);
            dts = readTs(is);
        } else {
            if (c != 0x0f)
                throw new RuntimeException("Invalid data");
        }

        return new PESPacket(null, pts, streamId, len, pos);
    }

    public static long readTs(ByteBuffer is, int c) {
        return (((long) c & 0x0e) << 29) | ((is.get() & 0xff) << 22) | (((is.get() & 0xff) >> 1) << 15)
                | ((is.get() & 0xff) << 7) | ((is.get() & 0xff) >> 1);
    }

    public static PESPacket mpeg2Pes(int b0, int len, int streamId, ByteBuffer is, long pos) {
        int flags1 = b0;
        int flags2 = is.get() & 0xff;
        int header_len = is.get() & 0xff;

        long pts = -1, dts = -1;
        if ((flags2 & 0xc0) == 0x80) {
            pts = readTs(is);
            NIOUtils.skip(is, header_len - 5);
        } else if ((flags2 & 0xc0) == 0xc0) {
            pts = readTs(is);
            dts = readTs(is);
            NIOUtils.skip(is, header_len - 10);
        } else
            NIOUtils.skip(is, header_len);

        return new PESPacket(null, pts, streamId, len, pos);
    }

    public static long readTs(ByteBuffer is) {
        return (((long) is.get() & 0x0e) << 29) | ((is.get() & 0xff) << 22) | (((is.get() & 0xff) >> 1) << 15)
                | ((is.get() & 0xff) << 7) | ((is.get() & 0xff) >> 1);
    }

    static int $(int marker) {
        return marker & 0xff;
    }

    public static final boolean mediaStream(int streamId) {
        return (streamId >= $(AUDIO_MIN) && streamId <= $(VIDEO_MAX) || streamId == $(PRIVATE_1) || streamId == $(PRIVATE_2));
    }

    public static final boolean videoStream(int streamId) {
        return streamId >= $(VIDEO_MIN) && streamId <= $(VIDEO_MAX);
    }

    public static boolean audioStream(Integer streamId) {
        return streamId >= $(AUDIO_MIN) && streamId <= $(AUDIO_MAX) || streamId == $(PRIVATE_1)
                || streamId == $(PRIVATE_2);
    }

    public List<MPEGDemuxerTrack> getTracks() {
        return new ArrayList<MPEGDemuxerTrack>(streams.values());
    }

    public List<MPEGDemuxerTrack> getVideoTracks() {
        return getTracks(VIDEO_MIN, VIDEO_MAX);
    }

    public List<MPEGDemuxerTrack> getAudioTracks() {
        return getTracks(AUDIO_MIN, AUDIO_MAX);
    }

    private List<MPEGDemuxerTrack> getTracks(int min, int max) {
        min &= 0xff;
        max &= 0xff;
        List<MPEGDemuxerTrack> result = new ArrayList<MPEGDemuxerTrack>();
        for (BaseTrack p : streams.values()) {
            if (p.streamId >= min && p.streamId <= max)
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
        boolean inVideoPes = false, hasHeader = false, slicesStarted = false;
        while (b.hasRemaining()) {
            int code = b.get() & 0xff;
            marker = (marker << 8) | code;
            if (marker < 0x100 || marker > 0x1ff)
                continue;

            if (marker >= VIDEO_MIN && marker <= VIDEO_MAX) {
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
                    break;
                if (!slicesStarted) {
                    score += 50;
                    slicesStarted = true;
                }
                score += 1;
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
            super(data, pts, timescale, duration, frameNo, keyFrame, tapeTimecode);
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
}