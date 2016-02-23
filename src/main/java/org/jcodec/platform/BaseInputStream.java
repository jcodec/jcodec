package org.jcodec.platform;

import java.io.IOException;
import java.io.InputStream;

public abstract class BaseInputStream extends InputStream {

    protected abstract int readByte() throws IOException;

    protected int readBuffer(byte[] b, int off, int len) throws IOException {
        return super.read(b, off, len);
    }

    @Override
    public int read() throws IOException {
        return readByte();
    }

}
