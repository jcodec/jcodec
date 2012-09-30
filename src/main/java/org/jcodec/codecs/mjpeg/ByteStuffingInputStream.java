package org.jcodec.codecs.mjpeg;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class ByteStuffingInputStream extends FilterInputStream {

    private final PushbackInputStream pushBack;

    public ByteStuffingInputStream(PushbackInputStream in) {
        super(in);
        pushBack = in;
    }

    public int read() throws IOException {
        int b = super.read();
        if (b != 0xff) {
            return b;
        }
        int b2 = super.read();
        if (b2 == 0x0) {
            return 0xff;
        } else {
            pushBack.unread(b2);
            pushBack.unread(b);
            return -1;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int count = 0;
        for (int i = off; i < off + len; i++) {
            int r = read();
            if (r != -1) {
                count++;
                b[i] = (byte) r;
            } else {
                if (count == 0)
                    return -1;
                break;
            }
        }
        return count;
    }

}
