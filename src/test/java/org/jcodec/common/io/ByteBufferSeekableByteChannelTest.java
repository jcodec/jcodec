package org.jcodec.common.io;

import static org.jcodec.common.io.ByteBufferSeekableByteChannel.readFromByteBuffer;
import static org.jcodec.common.io.ByteBufferSeekableByteChannel.writeToByteBuffer;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteBufferSeekableByteChannelTest {
    @Test
    public void testSize() throws Exception {
        int sz = 42;
        ByteBufferSeekableByteChannel channelEmpty = writeToByteBuffer(ByteBuffer.allocate(42));
        assertEquals(0L, channelEmpty.size());

        ByteBufferSeekableByteChannel channelFull = readFromByteBuffer(ByteBuffer.allocate(42));
        assertEquals(sz, channelFull.size());
    }

    @Test
    public void testWrite() throws Exception {
        int sz = 42;
        ByteBufferSeekableByteChannel channelEmpty = writeToByteBuffer(ByteBuffer.allocate(sz));
        assertEquals(0L, channelEmpty.size());
        assertFalse(channelEmpty.getContents().hasRemaining());
        assertEquals(3, channelEmpty.write(ByteBuffer.wrap(new byte[] { 1, 2, 3 })));
        assertEquals(3, channelEmpty.getContents().remaining());

        ByteBufferSeekableByteChannel channelFull = readFromByteBuffer(ByteBuffer.allocate(sz));
        assertEquals(sz, channelFull.size());
        assertTrue(channelFull.getContents().hasRemaining());
        assertEquals(3, channelFull.write(ByteBuffer.wrap(new byte[] { 1, 2, 3 })));
        assertEquals(42, channelFull.getContents().remaining());
    }

    @Test
    public void testRead() throws Exception {
        byte[] initial = new byte[] { 0, 1, 2, 3, 4, 5 };
        ByteBuffer buf = ByteBuffer.wrap(initial);
        ByteBufferSeekableByteChannel channelEmpty = writeToByteBuffer(buf.duplicate());
        assertEquals(-1, channelEmpty.read(ByteBuffer.allocate(3)));
        assertEquals(0, channelEmpty.size());

        ByteBufferSeekableByteChannel channelFull = readFromByteBuffer(buf);
        assertEquals(3, channelFull.read(ByteBuffer.allocate(3)));
        assertEquals(initial.length, channelFull.size());
    }

}
