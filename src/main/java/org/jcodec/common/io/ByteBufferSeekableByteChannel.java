package org.jcodec.common.io;
import js.io.IOException;
import js.nio.ByteBuffer;

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

    public ByteBufferSeekableByteChannel(ByteBuffer backing) {
        this.backing = backing;
        this.contentLength = backing.remaining();
        this.open = true;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() throws IOException {
        open = false;
    }

    public int read(ByteBuffer dst) throws IOException {
        if (!backing.hasRemaining()) {
            return -1;
        }
        int toRead = Math.min(backing.remaining(), dst.remaining());
        dst.putBuf(NIOUtils.read(backing, toRead));
        contentLength = Math.max(contentLength, backing.position());
        return toRead;
    }

    public int write(ByteBuffer src) throws IOException {
        int toWrite = Math.min(backing.remaining(), src.remaining());
        backing.putBuf(NIOUtils.read(src, toWrite));
        contentLength = Math.max(contentLength, backing.position());
        return toWrite;
    }

    public long position() throws IOException {
        return backing.position();
    }

    public SeekableByteChannel setPosition(long newPosition) throws IOException {
        backing.setPosition((int) newPosition);
        contentLength = Math.max(contentLength, backing.position());
        return this;
    }

    public long size() throws IOException {
        return contentLength;
    }

    public SeekableByteChannel truncate(long size) throws IOException {
        contentLength = (int) size;
        return this;
    }

    public ByteBuffer getContents() {
        ByteBuffer contents = backing.duplicate();
        contents.setPosition(0);
        contents.setLimit(contentLength);
        return contents;
    }
}
