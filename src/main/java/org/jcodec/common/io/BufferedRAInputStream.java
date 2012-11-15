package org.jcodec.common.io;

import java.io.BufferedInputStream;
import java.io.IOException;

public class BufferedRAInputStream extends RAInputStream {
    private RAInputStream src;
    private BufferedInputStream bin;
    private long count;
    private long start;

    public BufferedRAInputStream(RAInputStream src) throws IOException {
        this.src = src;
        bin = new BufferedInputStream(src, 0x10000);
    }

    @Override
    public long getPos() throws IOException {
        return start + count;
    }

    @Override
    public int read() throws IOException {
        int read = bin.read();
        count += (read >> 31) + 1;
        return read;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        int read = bin.read(buf);
        count += read > 0 ? read : 0;
        return read;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int read = bin.read(buf, off, len);
        count += read > 0 ? read : 0;
        return read;
    }

    @Override
    public void seek(long where) throws IOException {
        src.seek(where);
        count = 0;
        start = src.getPos();
        bin = new BufferedInputStream(src);
    }

    @Override
    public long skip(long i) throws IOException {
        long skip = bin.skip(i);
        count += skip;
        return skip;
    }

    @Override
    public void close() throws IOException {
        src.close();
    }

    @Override
    public long length() throws IOException {
        return src.length();
    }
}
