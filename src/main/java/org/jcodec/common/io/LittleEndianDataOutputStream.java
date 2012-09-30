package org.jcodec.common.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class LittleEndianDataOutputStream extends OutputStream implements DataOutput {

    private OutputStream out;

    public LittleEndianDataOutputStream(OutputStream out) {
        this.out = out;
    }

    public void writeBoolean(boolean v) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void writeByte(int v) throws IOException {
        out.write((byte) v);
    }

    public void writeShort(int v) throws IOException {
        out.write((byte) (v & 0xff));
        out.write((byte) (v >> 8) & 0xff);
    }

    public void writeChar(int v) throws IOException {
        out.write((byte) (v & 0xff));
        out.write((byte) (v >> 8) & 0xff);
    }

    public void writeInt(int v) throws IOException {
        out.write((v >>> 0) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }

    public void writeLong(long v) throws IOException {
        out.write((byte) (v & 0xff));
        out.write((byte) (v >> 8) & 0xff);
        out.write((byte) (v >> 16) & 0xff);
        out.write((byte) (v >> 24) & 0xff);
        out.write((byte) (v >> 32) & 0xff);
        out.write((byte) (v >> 40) & 0xff);
        out.write((byte) (v >> 48) & 0xff);
        out.write((byte) (v >> 56) & 0xff);
    }

    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeBytes(String s) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void writeChars(String s) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void writeUTF(String s) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void write(int b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }
}