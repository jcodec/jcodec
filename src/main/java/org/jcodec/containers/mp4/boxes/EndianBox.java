package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.containers.mp4.boxes.EndianBox.Endian;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class EndianBox extends Box {

    public static enum Endian {
        LITTLE_ENDIAN, BIG_ENDIAN
    };

    private Endian endian;

    public static String fourcc() {
        return "enda";
    }

    public static EndianBox createEndianBox(Endian endian) {
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
            this.endian = Endian.LITTLE_ENDIAN;
        } else {
            this.endian = Endian.BIG_ENDIAN;
        }
    }

    protected void doWrite(ByteBuffer out) {
        out.putShort((short) (endian == Endian.LITTLE_ENDIAN ? 1 : 0));
    }

    public Endian getEndian() {
        return endian;
    }

    protected int calcSize() {
        return 2;
    }
}