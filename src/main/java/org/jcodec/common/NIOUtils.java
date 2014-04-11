package org.jcodec.common;

import static java.lang.Math.min;
import static org.jcodec.common.JCodecUtil.asciiString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class NIOUtils {

    public static ByteBuffer search(ByteBuffer buffer, int n, byte... param) {
        ByteBuffer result = buffer.duplicate();
        int step = 0, rem = buffer.position();
        while (buffer.hasRemaining()) {
            int b = buffer.get();
            if (b == param[step]) {
                ++step;
                if (step == param.length) {
                    if (n == 0) {
                        buffer.position(rem);
                        result.limit(buffer.position());
                        break;
                    }
                    n--;
                    step = 0;
                }
            } else {
                if (step != 0) {
                    step = 0;
                    ++rem;
                    buffer.position(rem);
                } else
                    rem = buffer.position();
            }
        }
        return result;
    }

    public static final ByteBuffer read(ByteBuffer buffer, int count) {
        ByteBuffer slice = buffer.duplicate();
        int limit = buffer.position() + count;
        slice.limit(limit);
        buffer.position(limit);
        return slice;
    }

    public static ByteBuffer fetchFrom(File file) throws IOException {
        return NIOUtils.fetchFrom(file, (int) file.length());
    }

    public static ByteBuffer fetchFrom(ReadableByteChannel ch, int size) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(size);
        NIOUtils.read(ch, buf);
        buf.flip();
        return buf;
    }

    /**
     * Reads size amount of bytes from ch into a new ByteBuffer allocated from a
     * buffer buf
     * 
     * @param buf
     * @param ch
     * @param size
     * @return
     * @throws IOException
     */
    public static ByteBuffer fetchFrom(ByteBuffer buf, ReadableByteChannel ch, int size) throws IOException {
        ByteBuffer result = buf.duplicate();
        result.limit(size);
        NIOUtils.read(ch, result);
        result.flip();
        return result;
    }

    public static ByteBuffer fetchFrom(File file, int length) throws IOException {
        FileChannel is = null;
        try {
            is = new FileInputStream(file).getChannel();
            return fetchFrom(is, length);
        } finally {
            closeQuietly(is);
        }
    }

    public static void writeTo(ByteBuffer buffer, File file) throws IOException {
        FileChannel out = null;
        try {
            out = new FileOutputStream(file).getChannel();
            out.write(buffer);
        } finally {
            closeQuietly(out);
        }
    }

    public static byte[] toArray(ByteBuffer buffer) {
        byte[] result = new byte[buffer.remaining()];
        buffer.duplicate().get(result);
        return result;
    }

    public static byte[] toArray(ByteBuffer buffer, int count) {
        byte[] result = new byte[Math.min(buffer.remaining(), count)];
        buffer.duplicate().get(result);
        return result;
    }

    public static int read(ReadableByteChannel channel, ByteBuffer buffer, int length) throws IOException {
        ByteBuffer fork = buffer.duplicate();
        fork.limit(min(fork.position() + length, fork.limit()));
        while (channel.read(fork) != -1 && fork.hasRemaining())
            ;
        int read = fork.position() - buffer.position();
        buffer.position(fork.position());
        return read;
    }

    public static int read(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        int rem = buffer.position();
        while (channel.read(buffer) != -1 && buffer.hasRemaining())
            ;
        return buffer.position() - rem;
    }

    public static void write(ByteBuffer to, ByteBuffer from) {
        if (from.hasArray()) {
            to.put(from.array(), from.arrayOffset() + from.position(), Math.min(to.remaining(), from.remaining()));
        } else {
            to.put(toArray(from, to.remaining()));
        }
    }

    public static void write(ByteBuffer to, ByteBuffer from, int count) {
        if (from.hasArray()) {
            to.put(from.array(), from.arrayOffset() + from.position(), Math.min(from.remaining(), count));
        } else {
            to.put(toArray(from, count));
        }
    }

    public static void fill(ByteBuffer buffer, byte val) {
        while (buffer.hasRemaining())
            buffer.put(val);
    }

    public static final MappedByteBuffer map(String fileName) throws IOException {
        return map(new File(fileName));
    }

    public static final MappedByteBuffer map(File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        MappedByteBuffer map = is.getChannel().map(MapMode.READ_ONLY, 0, file.length());
        is.close();
        return map;
    }

    public static int skip(ByteBuffer buffer, int count) {
        int toSkip = Math.min(buffer.remaining(), count);
        buffer.position(buffer.position() + toSkip);
        return toSkip;
    }

    public static ByteBuffer from(ByteBuffer buffer, int offset) {
        ByteBuffer dup = buffer.duplicate();
        dup.position(dup.position() + offset);
        return dup;
    }

    public static ByteBuffer combine(Iterable<ByteBuffer> picture) {
        int size = 0;
        for (ByteBuffer byteBuffer : picture) {
            size += byteBuffer.remaining();
        }
        ByteBuffer result = ByteBuffer.allocate(size);
        for (ByteBuffer byteBuffer : picture) {
            write(result, byteBuffer);
        }
        result.flip();
        return result;
    }

    public static ByteBuffer combine(ByteBuffer... buffer) {
        return combine(buffer);
    }

    public static String readString(ByteBuffer buffer, int len) {
        return new String(toArray(read(buffer, len)));
    }

    public static String readPascalString(ByteBuffer buffer, int maxLen) {
        ByteBuffer sub = read(buffer, maxLen + 1);
        return new String(toArray(NIOUtils.read(sub, Math.min(sub.get() & 0xff, maxLen))));
    }

    public static void writePascalString(ByteBuffer buffer, String string, int maxLen) {
        buffer.put((byte) string.length());
        buffer.put(asciiString(string));
        skip(buffer, maxLen - string.length());
    }

    public static void writePascalString(ByteBuffer buffer, String name) {
        buffer.put((byte) name.length());
        buffer.put(JCodecUtil.asciiString(name));
    }

    public static String readPascalString(ByteBuffer buffer) {
        return readString(buffer, buffer.get() & 0xff);
    }

    public static String readNullTermString(ByteBuffer buffer) {
        return readNullTermString(buffer, Charset.defaultCharset());
    }

    public static String readNullTermString(ByteBuffer buffer, Charset charset) {
        ByteBuffer fork = buffer.duplicate();
        while (buffer.hasRemaining() && buffer.get() != 0)
            ;
        if (buffer.hasRemaining())
            fork.limit(buffer.position() - 1);
        return new String(toArray(fork), charset);
    }

    public static ByteBuffer read(ByteBuffer buffer) {
        ByteBuffer result = buffer.duplicate();
        buffer.position(buffer.limit());
        return result;
    }

    public static void copy(ReadableByteChannel in, WritableByteChannel out, long amount) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(0x10000);
        int read;
        do {
            buf.position(0);
            buf.limit((int) Math.min(amount, buf.capacity()));
            read = in.read(buf);
            if (read != -1) {
                buf.flip();
                out.write(buf);
                amount -= read;
            }
        } while (read != -1 && amount > 0);
    }

    public static void closeQuietly(ReadableByteChannel channel) {
        if (channel == null)
            return;
        try {
            channel.close();
        } catch (IOException e) {
        }
    }

    public static byte readByte(ReadableByteChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1);
        channel.read(buf);
        buf.flip();
        return buf.get();
    }

    public static byte[] readNByte(ReadableByteChannel channel, int n) throws IOException {
        byte[] result = new byte[n];
        channel.read(ByteBuffer.wrap(result));
        return result;
    }

    public static int readInt(ReadableByteChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4);
        channel.read(buf);
        buf.flip();
        return buf.getInt();
    }

    public static int readInt(ReadableByteChannel channel, ByteOrder order) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4).order(order);
        channel.read(buf);
        buf.flip();
        return buf.getInt();
    }

    public static void writeByte(WritableByteChannel channel, byte value) throws IOException {
        channel.write((ByteBuffer) ByteBuffer.allocate(1).put(value).flip());
    }

    public static void writeInt(WritableByteChannel channel, int value, ByteOrder order) throws IOException {
        channel.write((ByteBuffer) ByteBuffer.allocate(4).order(order).putInt(value).flip());
    }

    public static void writeInt(WritableByteChannel channel, int value) throws IOException {
        channel.write((ByteBuffer) ByteBuffer.allocate(4).putInt(value).flip());
    }

    public static void writeLong(WritableByteChannel channel, long value) throws IOException {
        channel.write((ByteBuffer) ByteBuffer.allocate(8).putLong(value).flip());
    }

    public static FileChannelWrapper readableFileChannel(File file) throws FileNotFoundException {
        return new FileChannelWrapper(new FileInputStream(file).getChannel());
    }

    public static FileChannelWrapper writableFileChannel(File file) throws FileNotFoundException {
        return new FileChannelWrapper(new FileOutputStream(file).getChannel());
    }

    public static FileChannelWrapper rwFileChannel(File file) throws FileNotFoundException {
        return new FileChannelWrapper(new RandomAccessFile(file, "rw").getChannel());
    }

    public static FileChannelWrapper readableFileChannel(String file) throws FileNotFoundException {
        return new FileChannelWrapper(new FileInputStream(file).getChannel());
    }

    public static FileChannelWrapper writableFileChannel(String file) throws FileNotFoundException {
        return new FileChannelWrapper(new FileOutputStream(file).getChannel());
    }

    public static FileChannelWrapper rwFileChannel(String file) throws FileNotFoundException {
        return new FileChannelWrapper(new RandomAccessFile(file, "rw").getChannel());
    }

    public static AutoFileChannelWrapper autoChannel(File file) throws IOException {
        return new AutoFileChannelWrapper(file);
    }

    public static ByteBuffer duplicate(ByteBuffer bb) {
        ByteBuffer out = ByteBuffer.allocate(bb.remaining());
        out.put(bb.duplicate());
        out.flip();
        return out;
    }

    public static int find(List<ByteBuffer> catalog, ByteBuffer key) {
        byte[] keyA = toArray(key);
        for (int i = 0; i < catalog.size(); i++) {
            if (Arrays.equals(toArray(catalog.get(i)), keyA))
                return i;
        }
        return -1;
    }

    public static interface FileReaderListener {
        void progress(int percentDone);
    }

    public static abstract class FileReader {
        private int oldPd;

        protected abstract void data(ByteBuffer data, long filePos);

        public void readFile(File source, int bufferSize, FileReaderListener listener) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(bufferSize);
            SeekableByteChannel ch = null;
            try {
                ch = NIOUtils.readableFileChannel(source);
                long size = ch.size();
                for (long pos = ch.position(); ch.read(buf) != -1; pos = ch.position()) {
                    buf.flip();
                    data(buf, pos);
                    buf.flip();
                    if (listener != null) {
                        int newPd = (int) (100 * pos / size);
                        if (newPd != oldPd)
                            listener.progress(newPd);
                        oldPd = newPd;
                    }
                }
            } finally {
                NIOUtils.closeQuietly(ch);
            }
        }
    }
    
    public static byte getRel(ByteBuffer bb, int rel) {
        return bb.get(bb.position() + rel);
    }

	public static ByteBuffer cloneBuffer(ByteBuffer pesBuffer) {
		ByteBuffer res = ByteBuffer.allocate(pesBuffer.remaining());
		res.put(pesBuffer.duplicate());
		res.clear();
		return res;
	}
}