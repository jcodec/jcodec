package org.jcodec.codecs.wav;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public abstract class WriterLE {
    public static void writeShort(OutputStream out, short s) throws IOException {
        out.write(s & 0xff);
        out.write((s >> 8) & 0xff);
    }

    public static void writeInt(OutputStream out, int i) throws IOException {
        out.write(i & 0xff);
        out.write((i >> 8) & 0xff);
        out.write((i >> 16) & 0xff);
        out.write((i >> 24) & 0xff);
    }

    public static void writeLong(OutputStream out, long l) throws IOException {
        out.write((int)(l & 0xff));
        out.write((int)((l >> 8) & 0xff));
        out.write((int)((l >> 16) & 0xff));
        out.write((int)((l >> 24) & 0xff));
        out.write((int)((l >> 32) & 0xff));
        out.write((int)((l >> 40) & 0xff));
        out.write((int)((l >> 48) & 0xff));
        out.write((int)((l >> 56) & 0xff));
    }

}
