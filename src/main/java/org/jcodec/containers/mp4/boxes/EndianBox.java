package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class EndianBox extends Box {

    private ByteOrder endian;

    public static String fourcc() {
        return "enda";
    }

    public static EndianBox createEndianBox(ByteOrder endian) {
        EndianBox endianBox = new EndianBox(new Header(fourcc()));
        endianBox.endian = endian;
        return endianBox;
    }

    public EndianBox(Header header) {
        super(header);
    }

    public void parse(ByteBuffer input) {
        long end = input.getShort();
        if (end == 1) {
            this.endian = ByteOrder.LITTLE_ENDIAN;
        } else {
            this.endian = ByteOrder.BIG_ENDIAN;
        }
    }

    protected void doWrite(ByteBuffer out) {
        out.putShort((short) (endian == ByteOrder.LITTLE_ENDIAN ? 1 : 0));
    }

    public ByteOrder getEndian() {
        return endian;
    }

    protected int calcSize() {
        return 2;
    }
}