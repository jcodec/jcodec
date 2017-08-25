package org.jcodec.containers.mps;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.common.Assert;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;

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
    private static final int MTS_PACKET_SIZE = 188;
    private SeekableByteChannel channel;
    private Map<Integer, ProgramChannel> programs = new HashMap<Integer, ProgramChannel>();

    public Set<Integer> getPrograms() {
        return programs.keySet();
    }

    public Set<Integer> findPrograms(SeekableByteChannel src) throws IOException {
        long rem = src.position();
        Set<Integer> guids = new HashSet<Integer>();
        for (int i = 0; guids.size() == 0 || i < guids.size() * 500; i++) {
            MTSPacket pkt = readPacket(src);
            if (pkt == null)
                break;
            if (pkt.payload == null)
                continue;
            ByteBuffer payload = pkt.payload;
            if (!guids.contains(pkt.pid) && (payload.duplicate().getInt() & ~0xff) == 0x100) {
                guids.add(pkt.pid);
            }
        }
        src.setPosition(rem);
        return guids;
    }

    public MTSDemuxer(SeekableByteChannel src) throws IOException {
        this.channel = src;
        for (int pid : findPrograms(src)) {
            programs.put(pid, new ProgramChannel());
        }
        src.setPosition(0);
    }

    public ReadableByteChannel getProgram(int pid) {
        return programs.get(pid);
    }

    private class ProgramChannel implements ReadableByteChannel {
        private List<ByteBuffer> data;
        private boolean closed;

        public ProgramChannel() {
            this.data = new ArrayList<ByteBuffer>();
        }

        @Override
        public boolean isOpen() {
            return !closed && channel.isOpen();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            data.clear();
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int bytesRead = 0;
            while (dst.hasRemaining()) {
                while (data.size() == 0) {
                    if (!readAndDispatchNextTSPacket())
                        return bytesRead > 0 ? bytesRead : -1;
                }
                ByteBuffer first = data.get(0);
                int toRead = Math.min(dst.remaining(), first.remaining());
                dst.put(NIOUtils.read(first, toRead));
                if (!first.hasRemaining())
                    data.remove(0);
                bytesRead += toRead;
            }
            return bytesRead;
        }

        public void storePacket(MTSPacket pkt) {
            if(closed)
                return;
            data.add(pkt.payload);
        }
    }

    private boolean readAndDispatchNextTSPacket() throws IOException {
        MTSPacket pkt = readPacket(channel);
        if (pkt == null)
            return false;
        ProgramChannel program = programs.get(pkt.pid);
        if (program != null) {
            program.storePacket(pkt);
        }
        return true;
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

    private List<MTSPacket> packets = new LinkedList<MTSPacket>();
    private boolean eof;
    
    public MTSPacket readPacket(ReadableByteChannel channel) throws IOException {
        if (eof)
            return null;
        
        if (!packets.isEmpty())
            return packets.remove(0);
        
        ByteBuffer buffer = ByteBuffer.allocate(MTS_PACKET_SIZE * 100);
        if (channel.read(buffer) == -1) {
            eof = true;
            return null;
        }
        buffer.flip();

        while (buffer.hasRemaining() && (buffer.get(buffer.position()) & 0xff) == 0x47) {
            packets.add(parsePacket(NIOUtils.read(buffer, MTS_PACKET_SIZE)));
        }
        
        if (buffer.hasRemaining()) {
            // We are potentially out of sync, try to resync
            findSync(channel, buffer);
            
            if (buffer.remaining() >= MTS_PACKET_SIZE) {
                while (buffer.hasRemaining() && (buffer.get(buffer.position()) & 0xff) == 0x47) {
                    packets.add(parsePacket(NIOUtils.read(buffer, MTS_PACKET_SIZE)));
                }
            }
        }

        return packets.isEmpty() ? null : packets.remove(0);
    }
    
    private boolean refill(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        NIOUtils.relocateTail(buffer);
        buffer.position(buffer.limit());
        buffer.limit(buffer.capacity());
        boolean refilled = channel.read(buffer) != -1;
        buffer.flip();
        return refilled;
    }

    private void findSync(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        boolean sync = false;
        if (!refill(channel, buffer))
            return;
        while (!sync) {
            sync = findSyncOffset(buffer);
            if (!refill(channel, buffer))
                break;
        }
    }

    private static final int ITERATE_OVER = 10;
    
    private boolean findSyncOffset(ByteBuffer buffer) {
        while(buffer.remaining() > ITERATE_OVER*MTS_PACKET_SIZE) {
            int b = buffer.get() & 0xff;
            if (b != 0x47)
                continue;
            int j = 0;
            for (; j < ITERATE_OVER; j++) {
                int b1 = buffer.get(buffer.position() + j * MTS_PACKET_SIZE);
                if (b1 != 0x47) {
                    break;
                }
            }
            if (j == ITERATE_OVER)
                return true;
        }
        return false;
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

    public static int probe(final ByteBuffer b_) {
        ByteBuffer b = b_.duplicate();
        IntObjectMap<List<ByteBuffer>> streams = new IntObjectMap<List<ByteBuffer>>();
        while (true) {
            try {
                ByteBuffer sub = NIOUtils.read(b, 188);
                if (sub.remaining() < 188)
                    break;

                MTSPacket tsPkt = parsePacket(sub);
                if (tsPkt == null)
                    break;
                List<ByteBuffer> data = streams.get(tsPkt.pid);
                if (data == null) {
                    data = new ArrayList<ByteBuffer>();
                    streams.put(tsPkt.pid, data);
                }

                if (tsPkt.payload != null)
                    data.add(tsPkt.payload);
            } catch (Throwable t) {
                break;
            }
        }
        int maxScore = 0;
        int[] keys = streams.keys();
        for (int i : keys) {
            List<ByteBuffer> packets = streams.get(i);
            int score = MPSDemuxer.probe(NIOUtils.combineBuffers(packets));
            if (score > maxScore) {
                maxScore = score + (packets.size() > 20 ? 50 : 0);
            }
        }
        return maxScore;
    }
}