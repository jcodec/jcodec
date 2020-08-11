package org.jcodec.common.io;

import static java.lang.Math.min;
import static org.jcodec.platform.Platform.stringFromBytes;

import org.jcodec.common.ArrayUtil;
import org.jcodec.common.AutoFileChannelWrapper;
import org.jcodec.platform.Platform;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class NIOUtils {

    public static ByteBuffer search(ByteBuffer buffer, int n, byte[] param) {
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
        ((java.nio.Buffer)slice).limit(limit);
        ((java.nio.Buffer)buffer).position(limit);
        return slice;
    }

    public static ByteBuffer fetchFromFile(File file) throws IOException {
        return NIOUtils.fetchFromFileL(file, (int) file.length());
    }

    public static ByteBuffer fetchFromChannel(ReadableByteChannel ch, int size) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(size);
        NIOUtils.readFromChannel(ch, buf);
        ((java.nio.Buffer)buf).flip();
        return buf;
    }

    public static ByteBuffer fetchAllFromChannel(SeekableByteChannel ch) throws IOException {
        List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
        ByteBuffer buf;
        do {
            buf = fetchFromChannel(ch, 1 << 20);
            buffers.add(buf);
        } while (buf.hasRemaining());

        return combineBuffers(buffers);
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
        NIOUtils.readFromChannel(ch, result);
        result.flip();
        return result;
    }

    public static ByteBuffer fetchFromFileOL(File file, int off, int length) throws IOException {
        FileChannel is = null;
        try {
            is = new FileInputStream(file).getChannel();
            is.position(off);
            return fetchFromChannel(is, length);
        } finally {
            closeQuietly(is);
        }
    }

    public static ByteBuffer fetchFromFileL(File file, int length) throws IOException {
        FileChannel is = null;
        try {
            is = new FileInputStream(file).getChannel();
            return fetchFromChannel(is, length);
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

    public static byte[] toArrayL(ByteBuffer buffer, int count) {
        byte[] result = new byte[Math.min(buffer.remaining(), count)];
        buffer.duplicate().get(result);
        return result;
    }

    public static int readL(ReadableByteChannel channel, ByteBuffer buffer, int length) throws IOException {
        ByteBuffer fork = buffer.duplicate();
        ((java.nio.Buffer)fork).limit(min(fork.position() + length, fork.limit()));
        while (channel.read(fork) != -1 && fork.hasRemaining())
            ;
        ((java.nio.Buffer)buffer).position(fork.position());
        return buffer.position() == 0 ? -1 : buffer.position();
    }

    public static int readFromChannel(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        int rem = buffer.position();
        while (channel.read(buffer) != -1 && buffer.hasRemaining())
            ;
        return buffer.position() - rem;
    }

    public static void write(ByteBuffer to, ByteBuffer from) {
        if (from.hasArray()) {
            to.put(from.array(), from.arrayOffset() + from.position(), Math.min(to.remaining(), from.remaining()));
        } else {
            to.put(toArrayL(from, to.remaining()));
        }
    }

    public static void writeL(ByteBuffer to, ByteBuffer from, int count) {
        if (from.hasArray()) {
            to.put(from.array(), from.arrayOffset() + from.position(), Math.min(from.remaining(), count));
        } else {
            to.put(toArrayL(from, count));
        }
    }

    public static void fill(ByteBuffer buffer, byte val) {
        while (buffer.hasRemaining())
            buffer.put(val);
    }

    public static final MappedByteBuffer map(String fileName) throws IOException {
        return mapFile(new File(fileName));
    }

    public static final MappedByteBuffer mapFile(File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        MappedByteBuffer map = is.getChannel().map(MapMode.READ_ONLY, 0, file.length());
        is.close();
        return map;
    }

    public static int skip(ByteBuffer buffer, int count) {
        int toSkip = Math.min(buffer.remaining(), count);
        ((Buffer)buffer).position(buffer.position() + toSkip);
        return toSkip;
    }

    public static ByteBuffer from(ByteBuffer buffer, int offset) {
        ByteBuffer dup = buffer.duplicate();
        dup.position(dup.position() + offset);
        return dup;
    }

    public static ByteBuffer combineBuffers(Iterable<ByteBuffer> picture) {
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

    public static boolean combineBuffersInto(ByteBuffer dup, List<ByteBuffer> buffers) {
        throw new RuntimeException("Stan");
    }

    public static String readString(ByteBuffer buffer, int len) {
        return stringFromBytes(toArray(read(buffer, len)));
    }

    public static String readPascalStringL(ByteBuffer buffer, int maxLen) {
        ByteBuffer sub = read(buffer, maxLen + 1);
        return stringFromBytes(toArray(NIOUtils.read(sub, Math.min(sub.get() & 0xff, maxLen))));
    }

    public static void writePascalStringL(ByteBuffer buffer, String string, int maxLen) {
        buffer.put((byte) string.length());
        buffer.put(asciiString(string));
        skip(buffer, maxLen - string.length());
    }

    public static byte[] asciiString(String fourcc) {
        return Platform.getBytes(fourcc);
    }

    public static void writePascalString(ByteBuffer buffer, String name) {
        buffer.put((byte) name.length());
        buffer.put(asciiString(name));
    }

    public static String readPascalString(ByteBuffer buffer) {
        return readString(buffer, buffer.get() & 0xff);
    }

    public static String readNullTermString(ByteBuffer buffer) {
        return readNullTermStringCharset(buffer, Platform.UTF_8);
    }

    public static String readNullTermStringCharset(ByteBuffer buffer, String charset) {
        ByteBuffer fork = buffer.duplicate();
        while (buffer.hasRemaining() && buffer.get() != 0) {
        }
        ((java.nio.Buffer)fork).limit(buffer.position() - 1);
        return Platform.stringFromCharset(toArray(fork), charset);
    }

    public static void writeNullTermString(ByteBuffer buffer, String str) {
        writeNullTermStringCharset(buffer, str, Platform.UTF_8);
    }

    public static void writeNullTermStringCharset(ByteBuffer buffer, String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            buffer.put(bytes);
            buffer.put((byte) 0);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer readBuf(ByteBuffer buffer) {
        ByteBuffer result = buffer.duplicate();
        ((java.nio.Buffer)buffer).position(buffer.limit());
        return result;
    }

    public static void copy(ReadableByteChannel _in, WritableByteChannel out, long amount) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(0x10000);
        int read;
        do {
            buf.position(0);
            buf.limit((int) Math.min(amount, buf.capacity()));
            read = _in.read(buf);
            if (read != -1) {
                buf.flip();
                out.write(buf);
                amount -= read;
            }
        } while (read != -1 && amount > 0);
    }

    public static void copyAll(ReadableByteChannel in, WritableByteChannel out) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(0x10000);
        int read;
        do {
            ((java.nio.Buffer)buf).position(0);
            read = in.read(buf);
            if (read != -1) {
                ((java.nio.Buffer)buf).flip();
                out.write(buf);
            }
        } while (read != -1);
    }

    public static void closeQuietly(Closeable channel) {
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

    public static int readIntOrder(ReadableByteChannel channel, ByteOrder order) throws IOException {
        ByteBuffer buf = (ByteBuffer) ByteBuffer.allocate(4).order(order);
        channel.read(buf);
        buf.flip();
        return buf.getInt();
    }

    public static void writeByte(WritableByteChannel channel, byte value) throws IOException {
        channel.write((ByteBuffer) ByteBuffer.allocate(1).put(value).flip());
    }

    public static void writeIntOrder(WritableByteChannel channel, int value, ByteOrder order) throws IOException {
        ByteBuffer order2 = (ByteBuffer) ByteBuffer.allocate(4).order(order);
        channel.write((ByteBuffer) order2.putInt(value).flip());
    }

    public static void writeIntLE(WritableByteChannel channel, int value) throws IOException {
        ByteBuffer allocate = ByteBuffer.allocate(4);
        allocate.order(ByteOrder.LITTLE_ENDIAN);
        channel.write((ByteBuffer) allocate.putInt(value).flip());
    }

    public static void writeInt(WritableByteChannel channel, int value) throws IOException {
        ByteBuffer putInt = ByteBuffer.allocate(4).putInt(value);
        channel.write((ByteBuffer) ((java.nio.Buffer)putInt).flip());
    }

    public static void writeLong(WritableByteChannel channel, long value) throws IOException {
        ByteBuffer putLong = ByteBuffer.allocate(8).putLong(value);
        channel.write((ByteBuffer) ((java.nio.Buffer)putLong).flip());
    }

    public static FileChannelWrapper readableChannel(File file) throws FileNotFoundException {
        return new FileChannelWrapper(new FileInputStream(file).getChannel());
    }

    public static FileChannelWrapper writableChannel(File file) throws FileNotFoundException {
        return new FileChannelWrapper(new FileOutputStream(file).getChannel());
    }

    public static FileChannelWrapper rwChannel(File file) throws FileNotFoundException {
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
        ((java.nio.Buffer)out).flip();
        return out;
    }

    public static int find(List<ByteBuffer> catalog, ByteBuffer key) {
        byte[] keyA = toArray(key);
        for (int i = 0; i < catalog.size(); i++) {
            if (Platform.arrayEqualsByte(toArray(catalog.get(i)), keyA))
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

        protected abstract void done();

        public void readChannel(SeekableByteChannel ch, int bufferSize, FileReaderListener listener)
                throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(bufferSize);
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
            done();
        }

        public void readFile(File source, int bufferSize, FileReaderListener listener) throws IOException {
            SeekableByteChannel ch = null;
            try {
                ch = NIOUtils.readableChannel(source);
                readChannel(ch, bufferSize, listener);
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

    public static ByteBuffer clone(ByteBuffer byteBuffer) {
        ByteBuffer result = ByteBuffer.allocate(byteBuffer.remaining());
        result.put(byteBuffer.duplicate());
        result.flip();
        return result;
    }

    public static ByteBuffer asByteBuffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    public static ByteBuffer asByteBufferInt(int[] ints) {
        return asByteBuffer(ArrayUtil.toByteArray(ints));
    }

    public static void relocateLeftover(ByteBuffer bb) {
        int pos;
        for (pos = 0; bb.hasRemaining(); pos++) {
            bb.put(pos, bb.get());
        }
        bb.position(pos);
        bb.limit(bb.capacity());
    }

    public static boolean fetchUrl(URL urlInit, File fm) throws IOException {
        ReadableByteChannel in = null;
        SeekableByteChannel out = null;
        try {
            in = Channels.newChannel(urlInit.openConnection().getInputStream());
            out = NIOUtils.writableChannel(fm);
            NIOUtils.copyAll(in, out);
        } catch (FileNotFoundException e) {
            return false;
        } finally {
            NIOUtils.closeQuietly(in);
            NIOUtils.closeQuietly(out);
        }
        return true;
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileChannelWrapper in = null;
        FileChannelWrapper out = null;
        try {
            in = readableChannel(src);
            out = writableChannel(dst);
            copyAll(in, out);
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }
}
