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
import org.jcodec.common.DemuxerTrack;
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
public class MPSDemuxer extends SegmentReader {
    public static final int VIDEO_MIN = 0x1E0;
    public static final int VIDEO_MAX = 0x1EF;

    public static final int AUDIO_MIN = 0x1C0;
    public static final int AUDIO_MAX = 0x1DF;

    public static final int PRIVATE_1 = 0x1BD;
    public static final int PRIVATE_2 = 0x1BF;

    private Map<Integer, Track> streams = new HashMap<Integer, Track>();
    private SeekableByteChannel channel;

    public MPSDemuxer(SeekableByteChannel channel) throws IOException {
        super(channel);
        this.channel = channel;
        findStreams();
    }

    protected void findStreams() throws IOException {
        for (int i = 0; i == 0 || i < 3 * streams.size(); i++) {
            PESPacket nextPacket = nextPacket(ByteBuffer.allocate(0x10000));
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

    public class Track implements ReadableByteChannel {
        private List<PESPacket> pending = new ArrayList<PESPacket>();
        private int streamId;
        private MPEGES es;

        public Track(int streamId) throws IOException {
            this.streamId = streamId;
            this.es = new MPEGES(this);
        }

        public boolean isOpen() {
            return true;
        }

        public int getSid() {
            return streamId;
        }

        public MPEGES getES() {
            return es;
        }

        public void close() throws IOException {
        }

        public int read(ByteBuffer arg0) throws IOException {
            PESPacket pes = pending.size() > 0 ? pending.remove(0) : getPacket();
            NIOUtils.write(arg0, pes.data);
            return pes.data.remaining();
        }

        public void pending(PESPacket pkt) {
            pending.add(pkt);
        }

        public List<PESPacket> getPending() {
            return pending;
        }

        private PESPacket getPacket() throws IOException {
            if (pending.size() > 0)
                return pending.remove(0);
            PESPacket pkt;
            while ((pkt = nextPacket(ByteBuffer.allocate(0x10000))) != null) {
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

        public Packet nextFrame(ByteBuffer buf) throws IOException {
            return es.getFrame(buf);
        }

        public DemuxerTrackMeta getMeta() {
            return new DemuxerTrackMeta(videoStream(streamId) ? VIDEO : (audioStream(streamId) ? AUDIO : OTHER), null,
                    0, 0, null);
        }
    }

    public void seekByte(long offset) throws IOException {
        channel.position(offset);
        for (Track track : streams.values()) {
            track.pending.clear();
        }
    }

    private void addToStream(PESPacket pkt) throws IOException {
        Track pes = streams.get(pkt.streamId);
        if (pes == null) {
            pes = new Track(pkt.streamId);
            streams.put(pkt.streamId, pes);
        }
        pes.pending(pkt);
    }

    public PESPacket nextPacket(ByteBuffer out) throws IOException {
        ByteBuffer dup = out.duplicate();

        while (curMarker < PRIVATE_1 || curMarker > VIDEO_MAX)
            skipToMarker();

        readToNextMarker(dup);
        ByteBuffer fork = NIOUtils.from(dup, 4);
        PESPacket pkt = readPES(fork, curPos());
        if (pkt.length == 0) {
            while ((curMarker < PRIVATE_1 || curMarker > VIDEO_MAX) && readToNextMarker(dup))
                ;
        } else {
            read(dup, pkt.length - (fork.position() - dup.position() - 4));
        }
        dup.flip();
        pkt.data = dup;
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
        }

        return new PESPacket(null, pts, streamId, len, pos);
    }

    public static long readTs(ByteBuffer is) {
        return (((long) is.get() & 0x0e) << 29) | ((is.get() & 0xff) << 22) | (((is.get() & 0xff) >> 1) << 15)
                | ((is.get() & 0xff) << 7) | ((is.get() & 0xff) >> 1);
    }

    public static final boolean mediaStream(int streamId) {
        return (streamId >= AUDIO_MIN && streamId <= VIDEO_MAX) || streamId == PRIVATE_1 || streamId == PRIVATE_2;
    }

    public static final boolean videoStream(int streamId) {
        return streamId >= VIDEO_MIN && streamId <= VIDEO_MAX;
    }

    public static boolean audioStream(Integer streamId) {
        return streamId >= AUDIO_MIN && streamId <= AUDIO_MAX || streamId == PRIVATE_1 || streamId == PRIVATE_2;
    }

    public List<Track> getTracks() {
        return new ArrayList<Track>(streams.values());
    }

    public List<Track> getVideoTracks() {
        return getTracks(VIDEO_MIN, VIDEO_MAX);
    }

    public List<Track> getAudioTracks() {
        return getTracks(AUDIO_MIN, AUDIO_MAX);
    }

    private List<Track> getTracks(int min, int max) {
        ArrayList<Track> result = new ArrayList<Track>();
        for (Track p : streams.values()) {
            if (p.streamId >= min && p.streamId <= max)
                result.add(p);
        }
        return result;
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

            if (code >= VIDEO_MIN && code <= VIDEO_MAX) {
                if (inVideoPes)
                    break;
                else
                    inVideoPes = true;
            } else if (code >= 0xB0 && code <= 0xB8 && inVideoPes) {
                if ((hasHeader && code != 0xB5 && code != 0xB2) || slicesStarted)
                    break;
                score += 5;
            } else if (code == 0 && inVideoPes) {
                if (slicesStarted)
                    break;
                hasHeader = true;
            } else if (code > 0 && code < 0xB0) {
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