package org.jcodec.common.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Output stream backed by buffer
 * 
 * @author The JCodec project
 * 
 */
public class BackedOutputStream extends OutputStream {
    private byte[] buffer;
    private int ptr;

    public BackedOutputStream(int bufSize) {
        buffer = new byte[bufSize];
    }

    public BackedOutputStream(byte[] buf) {
        buffer = buf;
    }

    @Override
    public void write(int b) throws IOException {
        buffer[ptr++] = (byte) b;
    }

    @Override
    public void write(byte[] b) throws IOException {
        System.arraycopy(b, 0, buffer, ptr, b.length);
        ptr += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        System.arraycopy(b, off, buffer, ptr, len);
        ptr += len;
    }

    public int getPos() {
        return ptr;
    }

    public int setPos(int pos) {
        int rem = ptr;
        ptr = pos;
        return rem;
    }

    public void writeTo(OutputStream os, int count) throws IOException {
        os.write(buffer, 0, count);
    }
}
