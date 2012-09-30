package org.jcodec.common.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reader for big endian number
 * 
 * @author The JCodec project
 * 
 */
public abstract class ReaderBE {

    public static long readInt16(InputStream input) throws IOException {
        long b1 = input.read();
        long b2 = input.read();

        if (b1 == -1 || b2 == -1)
            return -1;

        return (b1 << 8) + b2;
    }

    public static long readInt32(InputStream input) throws IOException {
        long b1 = input.read();
        long b2 = input.read();
        long b3 = input.read();
        long b4 = input.read();

        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1)
            return -1;

        return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
    }

    public static long readInt64(InputStream input) throws IOException {
        long b1 = input.read();
        long b2 = input.read();
        long b3 = input.read();
        long b4 = input.read();
        long b5 = input.read();
        long b6 = input.read();
        long b7 = input.read();
        long b8 = input.read();

        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1 || b5 == -1 || b6 == -1 || b7 == -1 || b8 == -1)
            return -1;

        return (b1 << 56) + (b2 << 48) + (b3 << 40) + (b4 << 32) + (b5 << 24) + (b6 << 16) + (b7 << 8) + b8;
    }

    public static String readPascalString(InputStream input) throws IOException {
        int len = input.read();
        if (len == -1)
            return null;
        return readString(input, len);
    }

    public static String readPascalString(InputStream input, int maxLen) throws IOException {
        int len = input.read();
        byte[] bytes = sureRead(input, maxLen);
        return bytes == null ? null : new String(bytes, 0, len);
    }

    public static String readString(InputStream input, int len) throws IOException {
        byte[] bs = sureRead(input, len);
        return bs == null ? null : new String(bs, "iso8859-1");
    }

    public static byte[] sureRead(InputStream input, int len) throws IOException {
        byte[] res = new byte[len];
        int read = 0;
        while (read < len) {
            int tmp = input.read(res, read, len - read);
            if (tmp == -1)
                return null;
            read += tmp;
        }
        return res;
    }

    public static String readNullTermString(InputStream in) throws IOException {
        int rb, i = 0;
        byte[] res = new byte[1024];
        while ((rb = in.read()) != 0 && rb != -1) {
            res[i++] = (byte) rb;
        }
        return new String(res, 0, i);
    }

    public static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while((b = input.read()) != -1)
            baos.write(b);
            
        return baos.toByteArray();
    }
}