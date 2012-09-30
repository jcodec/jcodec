package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

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

    public EndianBox(Box other) {
        super(other);
    }

    public static String fourcc() {
        return "enda";
    }

    public EndianBox(Header header) {
        super(header);
    }

    public EndianBox(Endian endian) {
        super(new Header(fourcc()));
        this.endian = endian;
    }

    public void parse(InputStream input) throws IOException {
        long end = ReaderBE.readInt16(input);
        if (end == 1) {
            this.endian = Endian.LITTLE_ENDIAN;
        } else {
            this.endian = Endian.BIG_ENDIAN;
        }
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.writeShort(endian == Endian.LITTLE_ENDIAN ? 1 : 0);
    }

    public Endian getEndian() {
        return endian;
    }
}