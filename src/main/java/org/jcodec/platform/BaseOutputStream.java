package org.jcodec.platform;

import java.io.IOException;
import java.io.OutputStream;

public abstract class BaseOutputStream extends OutputStream {

    protected abstract void writeByte(int b) throws IOException;

    @Override
    public void write(int b) throws IOException {
        writeByte(b);
    }

}
