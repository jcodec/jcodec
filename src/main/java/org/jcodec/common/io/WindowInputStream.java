package org.jcodec.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An input stream that ensures all reads within a given window
 * 
 * @author The JCodec project
 * 
 */
public class WindowInputStream extends InputStream {
    private long remaining = 0;
    private InputStream proxied;

    public WindowInputStream(InputStream in, long remaining) {
        this.remaining = remaining;
        this.proxied = in;
    }

    public void openWindow(int size) {
        remaining = size;
    }

    @Override
    public int read() throws IOException {
        if (remaining > 0) {
            --remaining;
            return proxied.read();
        } else {
            return -1;
        }
    }

    public void skipRemaining() throws IOException {
        byte[] b = new byte[4096];
        while (read(b) != -1)
            ;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        if (remaining == 0)
            return -1;

        int toRead = (int) Math.min(remaining, buf.length), totalRead = 0;
        while (toRead > 0) {
            int read = proxied.read(buf, totalRead, toRead);
            if (read == -1)
                break;
            toRead -= read;
            remaining -= read;
            totalRead += read;
        }

        return totalRead;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (remaining == 0)
            return -1;

        long toRead = remaining > len ? len : remaining;

        int res = proxied.read(buf, off, (int) toRead);
        if (res != -1)
            remaining -= res;
        return res;
    }

    @Override
    public long skip(long i) throws IOException {
        long toSkip = remaining > i ? i : remaining;
        long skipped = proxied.skip((int) toSkip);
        remaining -= skipped;
        return skipped;
    }
}