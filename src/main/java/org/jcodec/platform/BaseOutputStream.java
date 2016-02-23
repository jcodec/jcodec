package org.jcodec.platform;

import java.io.IOException;
import java.io.OutputStream;

public class BaseOutputStream extends OutputStream {

    protected void writeByte(int b) throws IOException {
        throw new RuntimeException("TODO");
    }

}
