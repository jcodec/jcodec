package org.jcodec.common.io;

import js.io.Closeable;
import js.io.IOException;
import js.nio.channels.ByteChannel;
import js.nio.channels.Channel;
import js.nio.channels.ReadableByteChannel;
import js.nio.channels.WritableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface SeekableByteChannel extends ByteChannel, Channel, Closeable, ReadableByteChannel, WritableByteChannel {
    long position() throws IOException;

    SeekableByteChannel setPosition(long newPosition) throws IOException;

    long size() throws IOException;

    SeekableByteChannel truncate(long size) throws IOException;
}
