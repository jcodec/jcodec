package org.jcodec.platform;

import java.io.IOException;
import java.io.InputStream;

public abstract class BaseInputStream extends InputStream {

    protected abstract int readByte() throws IOException;

    protected abstract int readBuffer(byte[] b, int from, int len) throws IOException;

    @Override
    public int read(byte[] b) throws IOException {
        return readBuffer(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return readBuffer(b, off, len);
    }

    @Override
    public int read() throws IOException {
        return readByte();
    }

}
