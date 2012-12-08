package org.jcodec.common.io;

import static java.lang.Math.min;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Multipurpose byte buffer
 * 
 * @author The JCodec project
 * 
 */
public class Buffer {
    public byte[] buffer;
    public int pos;
    public int limit;
    private DataInput dinp;
    private DataOutput dout;

    public Buffer(byte[] buffer) {
        this.buffer = buffer;
        this.limit = buffer.length;
    }

    public Buffer(byte[] buffer, int pos, int limit) {
        this.buffer = buffer;
        this.pos = pos;
        this.limit = limit;
    }

    public Buffer(int size) {
        this(new byte[size]);
    }

    public final int next() {
        return pos < limit ? buffer[pos++] & 0xff : -1;
    }

    public final int nextIgnore() {
        return pos < limit ? buffer[pos++] & 0xff : 0;
    }

    public final int search(byte... param) {
        for (int i = pos; i < limit - param.length + 1; i++) {
            if (buffer[i] == param[0]) {
                int j = 1;
                for (; j < param.length; j++)
                    if (buffer[i + j] != param[j])
                        break;
                if (j == param.length)
                    return i - pos;
            }
        }
        return -1;
    }

    public Buffer read(int nl) {
        Buffer ret = new Buffer(buffer, pos, min(limit, pos + nl));
        pos += nl;
        return ret;
    }

    public final int get(int i) {
        return pos + i < limit ? buffer[pos + i] & 0xff : -1;
    }

    public final int nextIntLE() {
        if (pos < limit - 3) {
            return ((buffer[pos++] & 0xff) << 24) | ((buffer[pos++] & 0xff) << 16) | ((buffer[pos++] & 0xff) << 8)
                    | (buffer[pos++] & 0xff);
        } else {
            return (nextIgnore() << 24) | (nextIgnore() << 16) | (nextIgnore() << 8) | nextIgnore();
        }
    }

    public static Buffer fetchFrom(InputStream is, int size) throws IOException {
        byte[] buffer = new byte[size];
        return new Buffer(buffer, 0, read(buffer, 0, size, is));
    }

    public static Buffer fetchFrom(RandomAccessFile file, int size) throws IOException {
        byte[] buffer = new byte[size];
        return new Buffer(buffer, 0, read(buffer, 0, size, file));
    }

    public static Buffer fetchFrom(File file) throws IOException {
        long len = file.length();
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return fetchFrom(is, (int)len);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static int read(byte[] buffer, int pos, int size, InputStream is) throws IOException {
        int read, total = 0;

        while (size > 0 && (read = is.read(buffer, pos, size)) != -1) {
            pos += read;
            size -= read;
            total += read;
        }

        return total;
    }

    private static int read(byte[] buffer, int pos, int size, RandomAccessFile is) throws IOException {
        int read, total = 0;

        while (size > 0 && (read = is.read(buffer, pos, size)) != -1) {
            pos += read;
            size -= read;
            total += read;
        }

        return total;
    }

    public final void skip(int i) {
        pos = min(pos + i, limit);
    }

    public int remaining() {
        return limit - pos;
    }

    public static Buffer combine(Iterable<Buffer> picture) {
        int size = 0;
        for (Buffer byteBuffer : picture) {
            size += byteBuffer.remaining();
        }
        Buffer result = new Buffer(size);
        for (Buffer byteBuffer : picture) {
            result.write(byteBuffer);
        }
        return result.flip();
    }

    public void write(Buffer other) {
        if (other.remaining() > remaining())
            throw new IndexOutOfBoundsException("No space left in buffer");
        System.arraycopy(other.buffer, other.pos, buffer, pos, other.remaining());
        pos += other.remaining();
    }

    public void extendFrom(InputStream is, int bufSize) throws IOException {
        int pos = remaining();
        extend(pos + bufSize);
        limit = pos + read(buffer, pos, bufSize, is);
    }

    private void extend(int bufSize) {
        if (bufSize <= remaining())
            return;
        byte[] newBuf = new byte[bufSize];
        System.arraycopy(buffer, pos, newBuf, 0, remaining());
        buffer = newBuf;
        pos = 0;
        limit = bufSize;
    }

    public Buffer from(int i) {
        if (remaining() < i)
            throw new IndexOutOfBoundsException("buffer overrun");
        return new Buffer(buffer, pos + i, limit);
    }

    public Buffer fork() {
        return from(0);
    }

    public void extendWith(Buffer other) {
        if (other == null)
            return;
        int pos = remaining();
        extend(pos + other.remaining());
        System.arraycopy(other.buffer, other.pos, buffer, pos, other.remaining());
    }

    public InputStream is() {
        return new InputStream() {
            public int read() throws IOException {
                return next();
            }
        };
    }

    public OutputStream os() {
        return new OutputStream() {

            public void write(byte[] b, int off, int len) throws IOException {
                System.arraycopy(b, off, buffer, pos, len);
                pos += len;
            }

            public void write(int b) throws IOException {
                buffer[pos++] = (byte) b;
            }
        };
    }

    public int searchFrom(int from, byte... params) {
        int found = from(from).search(params);
        return found == -1 ? -1 : from + found;
    }

    public int searchFrom(int from, int... params) {
        return searchFrom(from, toIntArray(params));
    }

    public void print(PrintStream out) {
        for (int i = pos; i < limit; i++) {
            out.print(buffer[i] & 0xff);
            out.print(i < limit - 1 ? ", " : "");
        }
    }

    public DataInput dinp() {
        if (dinp == null)
            dinp = new DataInputStream(is());
        return dinp;
    }

    public DataOutput dout() {
        if (dout == null)
            dout = new DataOutputStream(os());
        return dout;
    }

    public byte[] toArray() {
        byte[] result = new byte[limit - pos];
        System.arraycopy(buffer, pos, result, 0, result.length);
        return result;
    }

    public void toArray(byte[] result, int off, int len) {
        System.arraycopy(buffer, pos, result, off, len);
    }

    public void writeTo(RandomAccessFile file) throws IOException {
        file.write(buffer, pos, limit - pos);
    }

    public void writeTo(OutputStream file) throws IOException {
        file.write(buffer, pos, limit - pos);
    }

    public void writeTo(File file) throws IOException {
        FileUtils.writeByteArrayToFile(file, toArray());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Buffer))
            return false;
        return Arrays.equals(toArray(), ((Buffer) obj).toArray());
    }

    public static Buffer combine(Buffer... buffers) {
        return combine(Arrays.asList(buffers));
    }

    public boolean startsWith(Buffer other) {
        int i, j;
        for (i = other.pos, j = pos; i < other.limit && j < limit; i++, j++)
            if (other.buffer[i] != buffer[i])
                return false;
        return i == other.limit;
    }

    public void write(byte b) {
        buffer[pos++] = b;
    }

    public void write(int i) {
        buffer[pos++] = (byte) i;
    }

    public int search(int... ints) {
        return search(toIntArray(ints));
    }

    private byte[] toIntArray(int... ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++)
            bytes[i] = (byte) ints[i];
        return bytes;
    }

    public Buffer flip() {
        return new Buffer(buffer, 0, pos);
    }
}