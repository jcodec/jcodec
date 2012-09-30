package org.jcodec.common.io;

import java.io.BufferedInputStream;
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
public class RandomAccessFileInputStream extends RandomAccessInputStream {

    private RandomAccessFile src;
    private FileInputStream fin;
    private BufferedInputStream bin;
    private long count;
    private long start;

    public RandomAccessFileInputStream(File file) throws IOException {
        src = new RandomAccessFile(file, "r");
        fin = new FileInputStream(src.getFD());
        bin = new BufferedInputStream(fin, 0x10000);
    }

    @Override
    public long getPos() throws IOException {
        return start + count;
    }

    @Override
    public int read() throws IOException {
        int read = bin.read();
        count += (read >> 31) + 1;
        return read;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        int read = bin.read(buf);
        count += read > 0 ? read : 0;
        return read;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int read = bin.read(buf, off, len);
        count += read > 0 ? read : 0;
        return read;
    }

    @Override
    public void seek(long where) throws IOException {
        src.seek(where);
        count = 0;
        start = src.getFilePointer();
        bin = new BufferedInputStream(fin);
    }

    @Override
    public long skip(long i) throws IOException {
        long skip = bin.skip(i);
        count += skip;
        return skip;
    }
    
    @Override
    public void close() throws IOException {
        src.close();
    }

    @Override
    public long length() throws IOException {
        return src.length();
    }
}