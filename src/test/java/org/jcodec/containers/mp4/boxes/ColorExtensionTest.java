package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class ColorExtensionTest {
    @Test
    public void testColorIsom() throws Exception {
        ColorExtension color = new ColorExtension();
        color.setColorRange((byte) 2);
        ByteBuffer buf = ByteBuffer.allocate(64);
        color.write(buf);
        buf.flip();
        Assert.assertEquals(19, buf.remaining());
    }
    
    @Test
    public void testColorMov() throws Exception {
        ColorExtension color = new ColorExtension();
        ByteBuffer buf = ByteBuffer.allocate(64);
        color.write(buf);
        buf.flip();
        Assert.assertEquals(18, buf.remaining());
    }

}
