package org.jcodec.containers.mkv;

import static junit.framework.Assert.assertEquals;
import static org.jcodec.containers.mkv.Reader.bytesToLong;
import static org.jcodec.containers.mkv.Reader.getVIntEbmlBytes;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Assert;
import org.junit.Test;

public class ReaderTest {

    @Test
    public void testBytesToLong() throws Exception {
        byte[] b = new byte[]{0x42, (byte) 0x86};
        
        assertEquals(17030l, bytesToLong(b));
        assertEquals(49798l, bytesToLong(new byte[]{(byte)0xC2, (byte) 0x86}));
    }
    
    @Test
    public void testFirstElementAndSize() throws Exception {
        FileInputStream fis = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        try {
            FileChannel channel = fis.getChannel();
            Assert.assertArrayEquals(Type.EBML.id, Reader.getRawEbmlBytes(channel));
            Assert.assertEquals(31, Reader.getEbmlVInt(channel));
        } finally {
            fis.close();
        }
    }
    
    @Test
    public void testFirstElementAndSizeAsBytes() throws Exception {
        FileInputStream fis = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        try {
            FileChannel channel = fis.getChannel();
            Assert.assertArrayEquals(Type.EBML.id, Reader.getRawEbmlBytes(channel));
            Assert.assertArrayEquals(new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F}, Reader.getRawEbmlBytes(channel));
        } finally {
            fis.close();
        }
    }
    
    @Test
    public void testFirstElement() throws Exception {
        FileInputStream fis = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        try {
            Assert.assertArrayEquals(Type.EBML.id, Reader.getRawEbmlBytes(fis.getChannel()));
        } finally {
            fis.close();
        }
    }
    
    @Test
    public void testReadingFromByteBuffer() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{0x60, 0x5F});
        Assert.assertArrayEquals(new byte[]{0x20, 0x5F}, getVIntEbmlBytes(bb));
        
        bb = ByteBuffer.wrap(new byte[]{(byte)0x81, (byte)0xFF, 0x60});
        Assert.assertArrayEquals(new byte[]{0x01}, getVIntEbmlBytes(bb));
        
        bb = ByteBuffer.wrap(new byte[]{0x1A, 0x45, (byte)0xDF, (byte)0xA3});
        Assert.assertArrayEquals(new byte[]{0x0A, 0x45, (byte)0xDF, (byte)0xA3}, getVIntEbmlBytes(bb));
    }
}
