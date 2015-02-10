package org.jcodec.containers.flv;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.flv.FLVPacket.Type;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * FLV ( Flash Media Video ) muxer
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class FLVMuxer {
    // Write buffer, 1M
    private static final int WRITE_BUFFER_SIZE = 0x100000;

    private int startOfLastPacket = 9;
    private SeekableByteChannel out;
    private ByteBuffer writeBuf;
    private byte[] prevMetadata;

    public FLVMuxer(SeekableByteChannel out) {
        this.out = out;
        writeBuf = ByteBuffer.allocate(WRITE_BUFFER_SIZE);
        writeHeader(writeBuf);
    }

    /**
     * Add a packet to the underlying file
     * 
     * @param pkt
     * @throws IOException
     */
    public void addPacket(FLVPacket pkt) throws IOException {
        if (!writePacket(writeBuf, pkt)) {
            writeBuf.flip();
            startOfLastPacket -= out.write(writeBuf);
            writeBuf.clear();
            if (!writePacket(writeBuf, pkt))
                throw new RuntimeException("Unexpected");
        }
    }

    /**
     * Finish muxing and write the remaining data
     * 
     * @throws IOException
     */
    public void finish() throws IOException {
        writeBuf.flip();
        out.write(writeBuf);
    }

    private boolean writePacket(ByteBuffer writeBuf, FLVPacket pkt) {
        for (;;) {
            int pktType = pkt.getType() == Type.VIDEO ? 0x9 : 0x8;
            int dataLen = pkt.getData().remaining();
            
            if (pkt.getMetadata() != prevMetadata) {
                pktType = 0x12;
                dataLen = pkt.getMetadata().length;
            }

            if (writeBuf.remaining() < 15 + dataLen)
                return false;

            writeBuf.putInt(writeBuf.position() - startOfLastPacket);
            startOfLastPacket = writeBuf.position();
            
            writeBuf.put((byte) pktType);
            writeBuf.putShort((short) (dataLen >> 8));
            writeBuf.put((byte) (dataLen & 0xff));

            writeBuf.putShort((short) ((pkt.getPts() >> 8) & 0xffff));
            writeBuf.put((byte) (pkt.getPts() & 0xff));
            writeBuf.put((byte) ((pkt.getPts() >> 24) & 0xff));

            writeBuf.putShort((short) 0);
            writeBuf.put((byte) 0);

            if (pkt.getMetadata() != prevMetadata) {
                writeBuf.put(pkt.getMetadata());
                prevMetadata = pkt.getMetadata();
            } else {
                NIOUtils.write(writeBuf, pkt.getData().duplicate());
                return true;
            }
        }
    }

    private static void writeHeader(ByteBuffer writeBuf) {
        writeBuf.put((byte) 'F');
        writeBuf.put((byte) 'L');
        writeBuf.put((byte) 'V');
        writeBuf.put((byte) 1);
        writeBuf.put((byte) 5);
        writeBuf.putInt(9);
    }
}
