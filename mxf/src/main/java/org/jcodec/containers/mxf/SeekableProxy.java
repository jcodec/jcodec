package org.jcodec.containers.mxf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

import org.jcodec.common.SeekableByteChannel;
import com.vg.io.SeekableInputStream;

public class SeekableProxy extends SeekableInputStream {

    private SeekableByteChannel ch;
    private InputStream is;
    private long length;

    public SeekableProxy(SeekableByteChannel ch) throws IOException {
        this.ch = ch;
        this.is = Channels.newInputStream(ch);
        this.length = ch.size();
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public long position() {
        try {
            return ch.position();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public long seek(long arg0) throws IOException {
        ch.position(arg0);
        return ch.position();
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return is.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return is.read(b);
    }
}