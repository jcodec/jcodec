package org.jcodec.common.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class RandomAccessByteOutputStream extends RandomAccessOutputStream {

    private static final int INC_SIZE = 65536 << 3;
    private byte[] buffer = new byte[INC_SIZE];
    private int pos;
    private int len;
    private DataOutputStream dos = new DataOutputStream(this);

    @Override
    public void writeBoolean(boolean v) throws IOException {
        dos.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        dos.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        dos.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        dos.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        dos.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        dos.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        dos.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        dos.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        dos.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        dos.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        dos.writeUTF(s);
    }

    @Override
    public long getPos() {
        return pos;
    }

    @Override
    public void seek(long pos) throws IOException {
        if (pos < buffer.length)
            this.pos = (int) pos;
    }

    @Override
    public void write(int b) throws IOException {
        if (pos >= buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length + INC_SIZE);
        }
        buffer[pos++] = (byte) b;
        len++;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, len);
    }
}
