package org.jcodec.common.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class FileRAOutputStream extends RAOutputStream {

    private RandomAccessFile raf;
    private DataOutputStream dos;
    private long start;
    private long count;

    public FileRAOutputStream(File file) throws IOException {
        raf = new RandomAccessFile(file, "rw");
        dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(raf.getFD())));
    }

    @Override
    public long getPos() {
        return start + count;
    }

    @Override
    public void seek(long pos) throws IOException {
        dos.flush();
        raf.seek(pos);
        start = raf.getFilePointer();
        count = 0;
        dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(raf.getFD())));
    }

    @Override
    public void write(int b) throws IOException {
        ++count;
        dos.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        count += b.length;
        dos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        count += len;
        dos.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        dos.flush();
    }

    @Override
    public void close() throws IOException {
        dos.close();
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        ++count;
        dos.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        ++count;
        dos.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        count += 2;
        dos.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        count += 2;
        dos.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        count += 4;
        dos.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        count += 8;
        dos.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        count += 4;
        dos.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        count += 8;
        dos.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        count += s.length();
        dos.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        count += (s.length() << 1);
        dos.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        count += s.getBytes("utf8").length;
        dos.writeUTF(s);
    }
}