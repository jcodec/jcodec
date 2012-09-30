package org.jcodec.common.io;

import java.io.File;
import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Random access file output stream with automatic resource management
 * 
 * @author The JCodec project
 */
public class AutoRandomAccessFileInputStream extends RandomAccessInputStream implements AutoResource {

    private static final long THRESHOLD = 5000; // five seconds
    private File file;
    private RandomAccessFileInputStream stream;
    private long accessTime;
    private long curTime;
    private long savedPos;

    public AutoRandomAccessFileInputStream(File file) throws IOException {
        this.file = file;
        this.curTime = System.currentTimeMillis();
        AutoPool.getInstance().add(this);
    }

    @Override
    public void seek(long where) throws IOException {
        ensureOpen();
        stream.seek(where);
    }

    private final void ensureOpen() throws IOException {
        accessTime = curTime;
        if (stream == null) {
//            System.out.println("Re-opening stream");
            stream = new RandomAccessFileInputStream(file);
            if (savedPos != 0)
                stream.seek(savedPos);
        }
    }

    @Override
    public long getPos() throws IOException {
        return stream != null ? stream.getPos() : savedPos;
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        return stream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        ensureOpen();
        return stream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        return stream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        return stream.skip(n);
    }

    @Override
    public void close() throws IOException {
        if (stream == null)
            return;
//        System.out.println("Closing stream");
        savedPos = stream.getPos();
        stream.close();
        stream = null;
    }

    @Override
    public void setCurTime(long curTime) {
        this.curTime = curTime;
        if (stream != null && curTime - accessTime > THRESHOLD) {
            try {
                close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public long length() throws IOException {
        return stream.length();
    }
}