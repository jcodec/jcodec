package org.jcodec.common.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A source that internally reads from random access file
 * 
 * @author The JCodec project
 * 
 */
public class FileRAInputStream extends RAInputStream {

    private RandomAccessFile src;
    private FileInputStream fin;

    public FileRAInputStream(File file) throws IOException {
        src = new RandomAccessFile(file, "r");
        fin = new FileInputStream(src.getFD());
    }

    @Override
    public long getPos() throws IOException {
        return src.getFilePointer();
    }

    @Override
    public int read() throws IOException {
        return fin.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return fin.read(buf);
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        return fin.read(buf, off, len);
    }

    @Override
    public void seek(long where) throws IOException {
        src.seek(where);
    }

    @Override
    public long skip(long i) throws IOException {
        return fin.skip(i);
    }

    @Override
    public void close() throws IOException {
        fin.close();
    }

    @Override
    public long length() throws IOException {
        return src.length();
    }
}