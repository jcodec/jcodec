package org.jcodec.platform;

import java.io.IOException;
import java.io.InputStream;

public class BaseInputStream extends InputStream {
    
    protected int readByte() throws IOException {
        throw new RuntimeException("TODO BaseInputStream.readByte");
    }
    
    protected int readBuffer(byte[] b, int from, int len) throws IOException {
        throw new RuntimeException("TODO BaseInputStream.readBuffer");
    }

}
