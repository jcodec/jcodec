package org.jcodec.common.io;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ByteArrayRAInputStream extends RAInputStream {

    private byte[] bytes;
    private int pos;

    public ByteArrayRAInputStream(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public int read() throws IOException {
        return pos < bytes.length ? (bytes[pos++] & 0xff) : -1;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int toRead = Math.min(bytes.length - pos, len);
        System.arraycopy(bytes, pos, buf, off, toRead);
        pos += toRead;

        return pos == bytes.length && toRead == 0 ? -1 : toRead;
    }

    @Override
    public long skip(long i) throws IOException {
        long toSkip = Math.min(bytes.length - pos, i);
        pos += toSkip;
        return toSkip;
    }

    @Override
    public void seek(long where) throws IOException {
        if (where < bytes.length)
            pos = (int) where;
    }

    @Override
    public long getPos() throws IOException {
        return pos;
    }

    @Override
    public long length() throws IOException {
        return bytes.length;
    }
}