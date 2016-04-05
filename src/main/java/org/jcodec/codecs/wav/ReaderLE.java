package org.jcodec.codecs.wav;
import js.io.IOException;
import js.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */

public abstract class ReaderLE {
    public static short readShort(InputStream input) throws IOException {
        int b2 = input.read();
        int b1 = input.read();

        if (b1 == -1 || b2 == -1)
            return -1;

        return (short) ((b1 << 8) + b2);
    }

    public static int readInt(InputStream input) throws IOException {
        long b4 = input.read();
        long b3 = input.read();
        long b2 = input.read();
        long b1 = input.read();

        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1)
            return -1;

        return (int) ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
    }

    public static long readLong(InputStream input) throws IOException {
        long b8 = input.read();
        long b7 = input.read();
        long b6 = input.read();
        long b5 = input.read();
        long b4 = input.read();
        long b3 = input.read();
        long b2 = input.read();
        long b1 = input.read();

        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1 || b5 == -1 || b6 == -1 || b7 == -1 || b8 == -1)
            return -1;

        return (int) ((b1 << 56) + (b2 << 48) + (b3 << 40) + (b4 << 32) + (b5 << 24) + (b6 << 16) + (b7 << 8) + b8);
    }
}
