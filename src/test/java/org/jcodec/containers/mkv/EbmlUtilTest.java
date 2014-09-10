package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.util.EbmlUtil.computeLength;
import static org.jcodec.containers.mkv.util.EbmlUtil.ebmlEncode;
import static org.jcodec.containers.mkv.util.EbmlUtil.ebmlLength;
import static org.jcodec.containers.mkv.util.EbmlUtil.toHexString;

import java.io.FileInputStream;
import java.nio.ByteBuffer;

import org.jcodec.common.FileChannelWrapper;
import org.junit.Assert;
import org.junit.Test;

public class EbmlUtilTest {

    @Test
    public void testReadingOverLimit() throws Exception {
        FileInputStream fis = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        try {
            FileChannelWrapper source = new FileChannelWrapper(fis.getChannel());
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.limit(1);
            source.read(buffer);
            System.out.println(toHexString(buffer.array()));

            buffer.limit(2);
            source.read(buffer);
            System.out.println(toHexString(buffer.array()));
        } finally {
            fis.close();
        }
    }

    @Test
    public void testComputeLength() throws Exception {
        Assert.assertEquals(1, computeLength((byte) 0x80));
        Assert.assertEquals(2, computeLength((byte) 0x40));
        Assert.assertEquals(3, computeLength((byte) 0x20));
        Assert.assertEquals(4, computeLength((byte) 0x10));
        Assert.assertEquals(5, computeLength((byte) 0x08));
        Assert.assertEquals(6, computeLength((byte) 0x04));
        Assert.assertEquals(7, computeLength((byte) 0x02));
        Assert.assertEquals(8, computeLength((byte) 0x01));
        try {
            computeLength((byte) 0x00);
            Assert.fail("Max length is 8");
        } catch (RuntimeException re) {
            System.out.println(re.getMessage());
        }
    }
    
    
    @Test
    public void testEbmlBytes() throws Exception {
        Assert.assertArrayEquals(new byte[]{(byte)0x81}, ebmlEncode(1));
        Assert.assertArrayEquals(new byte[]{0x40, (byte)0x80}, ebmlEncode(128));
        Assert.assertArrayEquals(new byte[]{0x20, 0x40, 0x00}, ebmlEncode(16384));
    }
    
    @Test
    public void testEbmlLength() {
        Assert.assertEquals(1, ebmlLength(0x00));
        Assert.assertEquals(1, ebmlLength(0x7F));
        Assert.assertEquals(2, ebmlLength(0x80));
        Assert.assertEquals(2, ebmlLength(128));
    }
}
