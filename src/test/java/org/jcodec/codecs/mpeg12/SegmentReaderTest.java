package org.jcodec.codecs.mpeg12;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.jcodec.codecs.mpeg12.SegmentReader.State;
import org.junit.Assert;
import org.junit.Test;

public class SegmentReaderTest {

    @Test
    public void testToNextMarkerPartial1() throws IOException {
        byte[] bytes = new byte[] { 0, 0, 1, 42, 43, 44, 45, 46, 0, 0, 1, 43 };

        for (int i = 0; i < bytes.length + 1; i++) {
            ReadableByteChannel ch = Channels.newChannel(new ByteArrayInputStream(bytes));
            byte[] outBytes = new byte[i];
            SegmentReader reader = new SegmentReader(ch, 1);
            ByteBuffer out = ByteBuffer.wrap(outBytes);
            State state = reader.readToNextMarkerPartial(out);
            Assert.assertEquals(i >= 8 ? State.DONE : State.MORE_DATA, state);
            byte[] expected = new byte[i];
            System.arraycopy(bytes, 0, expected, 0, Math.min(i, 8));
            Assert.assertArrayEquals(expected, outBytes);
        }
    }
    
    @Test
    public void testToNextMarkerPartial2() throws IOException {
        byte[] bytes = new byte[] { 0, 0, 1, 42, 43, 44, 45, 46, 0, 0, 1, 43, 52, 54 };

        for (int i = 0; i < bytes.length + 1; i++) {
            ReadableByteChannel ch = Channels.newChannel(new ByteArrayInputStream(bytes));
            byte[] outBytes = new byte[i];
            SegmentReader reader = new SegmentReader(ch, 1);
            ByteBuffer out = ByteBuffer.wrap(outBytes);
            State state = reader.readToNextMarkerPartial(out);
            Assert.assertEquals(i >= 8 ? State.DONE : State.MORE_DATA, state);
            byte[] expected = new byte[i];
            System.arraycopy(bytes, 0, expected, 0, Math.min(i, 8));
            Assert.assertArrayEquals(expected, outBytes);
        }
    }

    @Test
    public void testToNextMarkerPartial3() throws IOException {
        byte[] bytes = new byte[] { 42, 43, 44, 45, 46, 0, 0, 1, 43, 52, 54 };

        for (int i = 0; i < 6; i++) {
            ReadableByteChannel ch = Channels.newChannel(new ByteArrayInputStream(bytes));
            byte[] outBytes = new byte[i];
            SegmentReader reader = new SegmentReader(ch, 1);
            ByteBuffer out = ByteBuffer.wrap(outBytes);
            State state = reader.readToNextMarkerPartial(out);
            Assert.assertEquals(i >= 5 ? State.DONE : State.MORE_DATA, state);
            byte[] expected = new byte[i];
            System.arraycopy(bytes, 0, expected, 0, Math.min(i, 5));
            Assert.assertArrayEquals(expected, outBytes);
        }
    }

    @Test
    public void testToNextMarkerPartial4() throws IOException {
        byte[] bytes = new byte[] { 0, 0, 1, 42, 43, 44, 45, 46, 0, 0, 1, 43, 52, 54 };

        for (int i = 0; i < 7; i++) {
            ReadableByteChannel ch = Channels.newChannel(new ByteArrayInputStream(bytes));
            byte[] outBytes = new byte[i];
            SegmentReader reader = new SegmentReader(ch, 1);
            ByteBuffer out = ByteBuffer.wrap(outBytes);
            State state = reader.readToNextMarkerPartial(ByteBuffer.allocate(128));
            state = reader.readToNextMarkerPartial(out);
            Assert.assertEquals(i >= 6 ? State.STOP : State.MORE_DATA, state);
            byte[] expected = new byte[i];
            System.arraycopy(bytes, 8, expected, 0, Math.min(i, 6));
            Assert.assertArrayEquals(expected, outBytes);
        }
    }
    
    @Test
    public void testToNextMarkerPartial5() throws IOException {
        byte[] bytes = new byte[] { 0, 0, 1, 42, 43, 44, 45, 46, 0, 0, 1, 43, 52, 54, 0, 0, 1 };

        for (int i = 0; i < 7; i++) {
            ReadableByteChannel ch = Channels.newChannel(new ByteArrayInputStream(bytes));
            byte[] outBytes = new byte[i];
            SegmentReader reader = new SegmentReader(ch, 1);
            ByteBuffer out = ByteBuffer.wrap(outBytes);
            State state = reader.readToNextMarkerPartial(ByteBuffer.allocate(128));
            state = reader.readToNextMarkerPartial(out);
            Assert.assertEquals(i >= 6 ? State.DONE : State.MORE_DATA, state);
            byte[] expected = new byte[i];
            System.arraycopy(bytes, 8, expected, 0, Math.min(i, 6));
            Assert.assertArrayEquals(expected, outBytes);
        }
    }
    
    @Test
    public void testToNextMarkerPartialVar1() throws IOException {
        byte[] bytes = new byte[] { 0, 0, 1, 42, 43, 44, 45, 46, 0, 0, 1, 43 };

        ReadableByteChannel ch = Channels.newChannel(new ByteArrayInputStream(bytes));
        SegmentReader reader = new SegmentReader(ch, 1);
        reader.setBufferIncrement(1);
        ByteBuffer buf1 = reader.readToNextMarkerNewBuffer();
        ByteBuffer buf2 = reader.readToNextMarkerNewBuffer();
        ByteBuffer buf3 = reader.readToNextMarkerNewBuffer();
        Assert.assertEquals(ByteBuffer.wrap(bytes, 0, 8), buf1);
        Assert.assertEquals(ByteBuffer.wrap(bytes, 8, 4), buf2);
        Assert.assertNull(buf3);
    }
    
    @Test
    public void testToNextMarkerPartialVar2() throws IOException {
        byte[] bytes = new byte[] { 0, 0, 1, 42, 43, 44, 45, 46, 0, 0, 1, 43, 52, 54 };

        ReadableByteChannel ch = Channels.newChannel(new ByteArrayInputStream(bytes));
        SegmentReader reader = new SegmentReader(ch, 1);
        reader.setBufferIncrement(1);
        ByteBuffer buf1 = reader.readToNextMarkerNewBuffer();
        ByteBuffer buf2 = reader.readToNextMarkerNewBuffer();
        ByteBuffer buf3 = reader.readToNextMarkerNewBuffer();
        Assert.assertEquals(ByteBuffer.wrap(bytes, 0, 8), buf1);
        Assert.assertEquals(ByteBuffer.wrap(bytes, 8, 6), buf2);
        Assert.assertNull(buf3);
    }
    
    @Test
    public void testToNextMarkerPartialVar3() throws IOException {
        byte[] bytes = new byte[] { 42, 43, 44, 45, 46, 0, 0, 1, 43, 52, 54 };

        ReadableByteChannel ch = Channels.newChannel(new ByteArrayInputStream(bytes));
        SegmentReader reader = new SegmentReader(ch, 1);
        reader.setBufferIncrement(1);
        ByteBuffer buf1 = reader.readToNextMarkerNewBuffer();
        ByteBuffer buf2 = reader.readToNextMarkerNewBuffer();
        ByteBuffer buf3 = reader.readToNextMarkerNewBuffer();
        Assert.assertEquals(ByteBuffer.wrap(bytes, 0, 5), buf1);
        Assert.assertEquals(ByteBuffer.wrap(bytes, 5, 6), buf2);
        Assert.assertNull(buf3);
    }

}
