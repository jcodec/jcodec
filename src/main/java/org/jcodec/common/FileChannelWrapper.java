package org.jcodec.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class FileChannelWrapper implements SeekableByteChannel {

    private FileChannel ch;

    public FileChannelWrapper(FileChannel ch) throws FileNotFoundException {
        this.ch = ch;
    }

    @Override
    public int read(ByteBuffer arg0) throws IOException {
        return ch.read(arg0);
    }

    @Override
    public void close() throws IOException {
        ch.close();
    }

    @Override
    public boolean isOpen() {
        return ch.isOpen();
    }

    @Override
    public int write(ByteBuffer arg0) throws IOException {
        return ch.write(arg0);
    }

    @Override
    public long position() throws IOException {
        return ch.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        ch.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return ch.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        ch.truncate(size);
        return this;
    }
}