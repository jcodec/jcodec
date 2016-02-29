package org.jcodec.common.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This class is analogous to DataInputStream, it's backed by the Channel and
 * buffers the IO
 * 
 * @author The JCodec project
 * 
 */
public class DataReader implements Closeable {
    private static final int DEFAULT_BUFFER_SIZE = 1 << 20; // 1 megabyte

    private SeekableByteChannel channel;
    private ByteBuffer buffer;

    public static DataReader createDataReader(SeekableByteChannel channel, ByteOrder order) {
        return new DataReader(channel, order, DEFAULT_BUFFER_SIZE);
    }

    public DataReader(SeekableByteChannel channel, ByteOrder order, int bufferSize) {
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(bufferSize);
        this.buffer.limit(0);
        this.buffer.order(order);
    }

    public int readFully3(byte[] b, int off, int len) throws IOException {
        int initOff = off;
        while (len > 0) {
            fetchIfNeeded(len);

            if (buffer.remaining() == 0)
                break;

            int toRead = Math.min(buffer.remaining(), len);
            buffer.get(b, off, toRead);
            off += toRead;
            len -= toRead;
        }
        return off - initOff;
    }

    public int skipBytes(int n) throws IOException {
        long oldPosition = position();
        if (n < buffer.remaining()) {
            buffer.position(buffer.position() + n);
        } else {
            setPosition(oldPosition + n);
        }
        return (int) (position() - oldPosition);
    }

    public byte readByte() throws IOException {
        fetchIfNeeded(1);
        return buffer.get();
    }

    public short readShort() throws IOException {
        fetchIfNeeded(2);
        return buffer.getShort();
    }

    public char readChar() throws IOException {
        fetchIfNeeded(2);
        return buffer.getChar();
    }

    public int readInt() throws IOException {
        fetchIfNeeded(4);
        return buffer.getInt();
    }

    public long readLong() throws IOException {
        fetchIfNeeded(8);
        return buffer.getLong();
    }

    public float readFloat() throws IOException {
        fetchIfNeeded(4);
        return buffer.getFloat();
    }

    public double readDouble() throws IOException {
        fetchIfNeeded(8);
        return buffer.getDouble();
    }

    public long position() throws IOException {
        return channel.position() - buffer.limit() + buffer.position();
    }

    public long setPosition(long newPos) throws IOException {
        int relative = (int) (newPos - (channel.position() - buffer.limit()));
        if (relative >= 0 && relative < buffer.limit()) {
            buffer.position(relative);
        } else {
            buffer.limit(0);
            channel.setPosition(newPos);
        }
        return position();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    private void fetchIfNeeded(int length) throws IOException {
        if (buffer.remaining() < length) {
            moveRemainderToTheStart(buffer);
            channel.read(buffer);
        }
    }

    private static void moveRemainderToTheStart(ByteBuffer readBuf) {
        int rem = readBuf.remaining();
        for (int i = 0; i < rem; i++) {
            readBuf.put(i, readBuf.get());
        }
        readBuf.clear();
        readBuf.position(rem);
    }

    public long size() throws IOException {
        return channel.size();
    }

    public int readFully(byte[] b) throws IOException {
        return readFully3(b, 0, b.length);
    }
}