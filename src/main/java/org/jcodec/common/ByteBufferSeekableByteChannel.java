package org.jcodec.common;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class ByteBufferSeekableByteChannel implements SeekableByteChannel {

    private ByteBuffer backing;
    private boolean open;
    private int maxPos;

    public ByteBufferSeekableByteChannel(ByteBuffer backing) {
        this.backing = backing;
        this.open = true;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() throws IOException {
        open = false;
    }

    public int read(ByteBuffer dst) throws IOException {
        int toRead = Math.min(backing.remaining(), dst.remaining());
        dst.put(NIOUtils.read(backing, toRead));
        maxPos = Math.max(maxPos, backing.position());
        return toRead;
    }

    public int write(ByteBuffer src) throws IOException {
        int toWrite = Math.min(backing.remaining(), src.remaining());
        backing.put(NIOUtils.read(src, toWrite));
        maxPos = Math.max(maxPos, backing.position());
        return toWrite;
    }

    public long position() throws IOException {
        return backing.position();
    }

    public SeekableByteChannel position(long newPosition) throws IOException {
        backing.position((int) newPosition);
        maxPos = Math.max(maxPos, backing.position());
        return this;
    }

    public long size() throws IOException {
        return maxPos;
    }

    public SeekableByteChannel truncate(long size) throws IOException {
        maxPos = (int)size;
        return this;
    }

    public ByteBuffer getContents() {
        ByteBuffer contents = backing.duplicate();
        contents.position(0);
        contents.limit(maxPos);
        return contents;
    }
}
