package org.jcodec.common.io;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Implements a seekable byte channel that wraps a byte buffer
 * 
 * @author The JCodec project
 * 
 */
public class ByteBufferSeekableByteChannel implements SeekableByteChannel {

    private ByteBuffer backing;
    private boolean open;
    private int contentLength;

    public ByteBufferSeekableByteChannel(ByteBuffer backing, int contentLength) {
        this.backing = backing;
        this.contentLength = contentLength;
        this.open = true;
    }
    
    public static ByteBufferSeekableByteChannel writeToByteBuffer(ByteBuffer buf) {
        return new ByteBufferSeekableByteChannel(buf, 0);
    }
    
    public static ByteBufferSeekableByteChannel readFromByteBuffer(ByteBuffer buf) {
        return new ByteBufferSeekableByteChannel(buf, buf.remaining());
    }
    
    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        open = false;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!backing.hasRemaining() || contentLength <= 0) {
            return -1;
        }
        int toRead = Math.min(backing.remaining(), dst.remaining());
        toRead = Math.min(toRead, contentLength);
        dst.put(NIOUtils.read(backing, toRead));
        contentLength = Math.max(contentLength, backing.position());
        return toRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int toWrite = Math.min(backing.remaining(), src.remaining());
        backing.put(NIOUtils.read(src, toWrite));
        contentLength = Math.max(contentLength, backing.position());
        return toWrite;
    }

    @Override
    public long position() throws IOException {
        return backing.position();
    }

    @Override
    public SeekableByteChannel setPosition(long newPosition) throws IOException {
        backing.position((int) newPosition);
        contentLength = Math.max(contentLength, backing.position());
        return this;
    }

    @Override
    public long size() throws IOException {
        return contentLength;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        contentLength = (int) size;
        return this;
    }

    public ByteBuffer getContents() {
        ByteBuffer contents = backing.duplicate();
        contents.position(0);
        contents.limit(contentLength);
        return contents;
    }
}
