package org.jcodec.platform;

import js.io.IOException;
import js.io.OutputStream;

public abstract class BaseOutputStream extends OutputStream {

    protected abstract void writeByte(int b) throws IOException;

//    @Override
//    public void write(int b) throws IOException {
//        writeByte(b);
//    }

}
