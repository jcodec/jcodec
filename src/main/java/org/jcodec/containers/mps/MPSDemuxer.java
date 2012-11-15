package org.jcodec.containers.mps;

import static java.lang.Math.max;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jcodec.codecs.mpeg12.MPEGES;
import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.RAInputStream;
import org.jcodec.common.io.ReaderBE;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPSDemuxer {
    public static final int VIDEO_MIN = 0xE0;
    public static final int VIDEO_MAX = 0xEF;

    public static final int AUDIO_MIN = 0xC0;
    public static final int AUDIO_MAX = 0xDF;

    public static final int PRIVATE_1 = 0xBD;
    public static final int PRIVATE_2 = 0xBF;

    private static final int BUFFER_SIZE = 4096;

    private static final int TIMESCALE = 90000;

    private Map<Integer, PES> streams = new HashMap<Integer, PES>();
    private RAInputStream input;
    private Buffer buf;

    public MPSDemuxer(RAInputStream input) throws IOException {
        this.input = input;
        buf = fetchBuffer();
        findStreams();
    }

    protected MPSDemuxer() throws IOException {
    }

    protected void findStreams() throws IOException {
        if (buf == null)
            buf = fetchBuffer();
        for (int i = 0; i == 0 || i < 3 * streams.size(); i++) {
            PESPacket nextPacket = nextPacket();
            if (nextPacket == null)
                break;
            addToStream(nextPacket);
        }
    }

    public static class PESPacket {
        public Buffer data;
        public long pts;
        public int streamId;
        public int length;
        public int headerLen;

        public PESPacket(Buffer data, long pts, int streamId, int length, int headerLen) {
            this.data = data;
            this.pts = pts;
            this.streamId = streamId;
            this.length = length;
            this.headerLen = headerLen;
        }
    }

    public class PES {
        private List<PESPacket> pending = new ArrayList<PESPacket>();
        private int streamId;
        private MPEGES es;

        public PES(int streamId) {
            this.streamId = streamId;
        }

        public void pending(PESPacket pkt) {
            pending.add(pkt);
        }

        private PESPacket getPacket() throws IOException {
            if (pending.size() > 0)
                return pending.remove(0);
            PESPacket pkt;
            while ((pkt = nextPacket()) != null) {
                if (pkt.pts != -1)
                    es.curPts = pkt.pts;
                if (pkt.streamId == streamId)
                    return pkt;
                else
                    addToStream(pkt);
            }
            return null;
        }

        public Packet getFrame() throws IOException {
            if (es == null) {
                es = new MPEGES() {
                    protected Buffer fetchBuffer() throws IOException {
                        PESPacket packet = getPacket();
                        return packet != null ? packet.data : null;
                    }
                };
            }
            return es.getFrame();
        }

        public SequenceHeader getSequenceHeader() {
            return es.getSequenceHeader();
        }

        public GOPHeader getGroupHeader() {
            return es.getGroupheader();
        }
    }

    private void addToStream(PESPacket pkt) {
        PES pes = streams.get(pkt.streamId);
        if (pes == null) {
            pes = new PES(pkt.streamId);
            streams.put(pkt.streamId, pes);
        }
        pes.pending(pkt);
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

    public PESPacket nextPacket() throws IOException {
        if (buf == null)
            buf = fetchBuffer();
        int streamId;
        do {
            if (!skipToMarker())
                return null;
            streamId = buf.get(3);
            buf.skip(4);
        } while (!mediaStream(streamId));

        fetchData(260);

        PESPacket pkt = readPES(streamId, buf.is());
        if (input != null && streamId >= VIDEO_MIN && streamId <= VIDEO_MAX && pkt.pts != -1)
            map.put(pkt.pts, input.getPos() - buf.remaining() - pkt.headerLen - 4);
        if (pkt.length <= 0) {
            if (streamId >= 0xe0 && streamId <= 0xef) {
                pkt.data = readToMarker();
            } else
                throw new RuntimeException("0-len PES packet with non-video payload");
        } else {
            fetchData(pkt.length);
            pkt.data = buf.read(pkt.length);
        }
        return pkt;
    }

    private Buffer readToMarker() throws IOException {
        int ind, pos = 0;
        do {
            ind = buf.searchFrom(pos, 0, 0, 1);
            if (ind == -1) {
                Buffer b = fetchBuffer();
                if (b == null)
                    break;
                buf.extendWith(b);
            } else {
                pos = ind + 4;
                if (pos >= buf.remaining()) {
                    buf.extendWith(fetchBuffer());
                }
            }
        } while (ind == -1 || buf.get(pos - 1) < 0xB9);

        return buf.read(ind == -1 ? buf.remaining() : ind);
    }

    public static PESPacket readPES(int streamId, InputStream iss) throws IOException {
        int len = (int) ReaderBE.readInt16(iss);
        int b0 = iss.read();
        if ((b0 & 0xc0) == 0x80)
            return mpeg2Pes(b0, len, streamId, iss);
        else
            return mpeg1Pes(b0, len, streamId, iss);
    }

    private void fetchData(int n) throws IOException {
        Buffer b;
        while (buf.remaining() < n && (b = fetchBuffer()) != null)
            buf.extendWith(b);
    }

    public static PESPacket mpeg1Pes(int b0, int len, int streamId, InputStream is) throws IOException {
        int c = b0, consumed = 0;
        while (c == 0xff) {
            c = is.read();
            consumed++;
        }

        if ((c & 0xc0) == 0x40) {
            is.read();
            c = is.read();
            consumed += 2;
        }
        long pts = -1, dts = -1;
        if ((c & 0xf0) == 0x20) {
            pts = readTs(is, c);
            consumed += 5;
        } else if ((c & 0xf0) == 0x30) {
            pts = readTs(is, c);
            dts = readTs(is);
            consumed += 10;
        } else {
            if (c != 0x0f)
                throw new RuntimeException("Invalid data");
            consumed++;
        }

        return new PESPacket(null, pts, streamId, len - consumed, consumed + 2);
    }

    public static long readTs(InputStream is, int c) throws IOException {
        return (((long) c & 0x0e) << 29) | (is.read() << 22) | ((is.read() >> 1) << 15) | (is.read() << 7)
                | (is.read() >> 1);
    }

    public static PESPacket mpeg2Pes(int b0, int len, int streamId, InputStream is) throws IOException {
        int flags1 = b0;
        int flags2 = is.read();
        int header_len = is.read();

        long pts = -1, dts = -1;
        if ((flags2 & 0xc0) == 0x80) {
            pts = readTs(is);
            is.skip(header_len - 5);
        } else if ((flags2 & 0xc0) == 0xc0) {
            pts = readTs(is);
            dts = readTs(is);
            is.skip(header_len - 10);
        }

        return new PESPacket(null, pts, streamId, len - header_len - 3, header_len + 5);
    }

    public static long readTs(InputStream is) throws IOException {
        return (((long) is.read() & 0x0e) << 29) | (is.read() << 22) | ((is.read() >> 1) << 15) | (is.read() << 7)
                | (is.read() >> 1);
    }

    private boolean skipToMarker() throws IOException {
        if (buf.remaining() < 4)
            buf.extendWith(fetchBuffer());
        int ind;
        do {
            ind = buf.search(0, 0, 1);
            if (ind == -1)
                buf.extendWith(fetchBuffer());
        } while (ind == -1);
        buf = buf.from(ind);
        return true;
    }

    public List<PES> getTracks() {
        return new ArrayList<PES>(streams.values());
    }

    public List<PES> getVideoTracks() {
        return getTracks(VIDEO_MIN, VIDEO_MAX);
    }

    public List<PES> getAudioTracks() {
        return getTracks(AUDIO_MIN, AUDIO_MAX);
    }

    private List<PES> getTracks(int min, int max) {
        ArrayList<PES> result = new ArrayList<PES>();
        for (PES p : streams.values()) {
            if (p.streamId >= min && p.streamId <= max)
                result.add(p);
        }
        return result;
    }

    protected Buffer fetchBuffer() throws IOException {
        return Buffer.fetchFrom(input, BUFFER_SIZE);
    }

    TreeMap<Long, Long> map = new TreeMap<Long, Long>();

    public long seek(long pts) throws IOException {
        long oldPos = input.getPos();
        Buffer oldBuf = buf;

        long result = trySeek(pts);
        if (result == -1) {
            input.seek(oldPos);
            buf = oldBuf;
        } else {
            buf = fetchBuffer();
            for (PES pes : streams.values()) {
                pes.es = null;
                pes.pending.clear();
            }
        }

        return result;
    }

    public long trySeek(long pts) throws IOException {

        Entry<Long, Long> before = map.floorEntry(pts);
        if (before == null)
            return -1;
        Entry<Long, Long> after = map.ceilingEntry(pts);

        long toSeek;
        if (pts - before.getKey() < 2 * TIMESCALE) {
            toSeek = before.getValue();
        } else if (after == null) {
            long bOff = before != null ? before.getValue() : 0;
            toSeek = (bOff + input.length()) / 2;
        } else {
            long bOff = before.getValue();
            long aOff = after.getValue();
            long bTv = before.getKey();
            long atv = after.getKey();
            long ts = atv - pts < TIMESCALE ? max(0, atv - TIMESCALE) : pts;

            toSeek = bOff + (aOff - bOff) * (ts - bTv) / (atv - bTv);
        }

        input.seek(toSeek);
        buf = fetchBuffer();
        PESPacket pkt;
        do {
            pkt = nextPes();
            if (pkt == null)
                return -1;
            if (pkt.pts != -1) {
                long pos = input.getPos() - buf.remaining() - pkt.headerLen - 4;
                map.put(pkt.pts, pos);
            }
        } while (pkt.pts == -1);

        long dist = pts - pkt.pts;
        if (dist >= 0 && dist < 2 * TIMESCALE) {
            while (pkt.pts < pts) {
                pkt = nextPes();
                if (pkt == null)
                    return -1;
            }
            input.seek(input.getPos() - buf.remaining() - pkt.headerLen - 4);
            return pkt.pts;
        } else {
            return trySeek(pts);
        }
    }

    private PESPacket nextPes() throws IOException {
        int streamId;
        do {
            if (!skipToMarker())
                return null;
            streamId = buf.get(3);
            buf.skip(4);
        } while (!(streamId >= 0xe0 && streamId <= 0xef));

        fetchData(260);

        PESPacket pkt = readPES(streamId, buf.is());
        return pkt;
    }

    public static int probe(final Buffer b) {
        int marker = 0xffffffff;

        int score = 0;
        boolean inVideoPes = false, hasHeader = false, slicesStarted = false;
        for (int i = b.pos; i < b.limit; i++) {
            int code = b.buffer[i] & 0xff;
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
}