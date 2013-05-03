package org.jcodec.containers.mps;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mps.MPSDemuxer.Track;
import org.junit.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG TS demuxer
 * 
 * @author The JCodec project
 * 
 */
public class MTSDemuxer {
    private int guid = -1;
    private MPSDemuxer psDemuxer;

    public MTSDemuxer(final SeekableByteChannel channel) throws IOException {
        psDemuxer = new MPSDemuxer(new SeekableByteChannel() {
            public boolean isOpen() {
                return true;
            }

            public void close() throws IOException {
            }

            public int read(ByteBuffer dst) throws IOException {
                MTSPacket packet = getPacket(channel);
                int rem = packet.payload.remaining();
                NIOUtils.write(dst, packet.payload);
                return rem - packet.payload.remaining();
            }

            public int write(ByteBuffer src) throws IOException {
                return 0;
            }

            public long position() throws IOException {
                return 0;
            }

            public SeekableByteChannel position(long newPosition) throws IOException {
                return null;
            }

            public long size() throws IOException {
                return 0;
            }

            public SeekableByteChannel truncate(long size) throws IOException {
                return null;
            }
        });
    }

    public List<Track> getTracks() {
        return psDemuxer.getTracks();
    }

    public List<Track> getVideoTracks() {
        return psDemuxer.getVideoTracks();
    }

    public List<Track> getAudioTracks() {
        return psDemuxer.getAudioTracks();
    }

    protected MTSPacket getPacket(ReadableByteChannel channel) throws IOException {
        MTSPacket pkt;
        do {
            pkt = readPacket(channel);
            if (pkt == null)
                return null;
        } while (pkt.pid <= 0xf || pkt.pid == 0x1fff);

        while (guid == -1) {
            ByteBuffer payload = pkt.payload;
            if (payload.get(0) == 0 && payload.get(1) == 0 && payload.get(2) == 1) {
                guid = pkt.pid;
                break;
            }
            pkt = readPacket(channel);
            if (pkt == null)
                return null;
        }

        while (pkt.pid != guid) {
            pkt = readPacket(channel);
            if (pkt == null)
                return null;
        }

        // pkt.payload.print(System.out);
        // System.out.println(",");

        return pkt;
    }

    public static class MTSPacket {
        public ByteBuffer payload;
        public boolean payloadStart;
        public int pid;

        public MTSPacket(int guid, boolean payloadStart, ByteBuffer payload) {
            this.pid = guid;
            this.payloadStart = payloadStart;
            this.payload = payload;
        }
    }

    public static MTSPacket readPacket(ReadableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(188);
        if (NIOUtils.read(channel, buffer) != 188)
            return null;
        buffer.flip();
        return parsePacket(buffer);
    }

    public static MTSPacket parsePacket(ByteBuffer buffer) {

        int marker = buffer.get() & 0xff;
        Assert.assertEquals(0x47, marker);
        int guidFlags = buffer.getShort();
        int guid = (int) guidFlags & 0x1fff;
        int payloadStart = (guidFlags >> 14) & 0x1;
        int b0 = buffer.get() & 0xff;
        int counter = b0 & 0xf;
        if ((b0 & 0x20) != 0) {
            int taken = 0;
            taken = (buffer.get() & 0xff) + 1;
            NIOUtils.skip(buffer, taken - 1);
        }
        return new MTSPacket(guid, payloadStart == 1, ((b0 & 0x10) != 0) ? buffer : null);
    }

    public static int probe(final ByteBuffer b) {
        TIntObjectHashMap<List<ByteBuffer>> streams = new TIntObjectHashMap<List<ByteBuffer>>();
        while (true) {
            try {
                ByteBuffer sub = NIOUtils.read(b, 188);
                if (sub.remaining() < 188)
                    break;

                MTSPacket tsPkt = parsePacket(sub);
                List<ByteBuffer> data = streams.get(tsPkt.pid);
                if (data == null) {
                    data = new ArrayList<ByteBuffer>();
                    streams.put(tsPkt.pid, data);
                }
                data.add(tsPkt.payload);
            } catch (Throwable t) {
                break;
            }
        }
        int maxScore = 0;
        int[] keys = streams.keys();
        for (int i : keys) {
            int score = MPSDemuxer.probe(NIOUtils.combine(streams.get(i)));
            if (score > maxScore)
                maxScore = score;
        }
        return maxScore;
    }
}