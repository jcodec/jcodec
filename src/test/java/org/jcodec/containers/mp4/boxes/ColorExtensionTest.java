package org.jcodec.containers.mp4.boxes;
import org.junit.Assert;
import org.junit.Test;

import js.nio.ByteBuffer;

public class ColorExtensionTest {
    @Test
    public void testColorIsom() throws Exception {
        ColorExtension color = ColorExtension.createColr();
        color.setColorRange((byte) 2);
        ByteBuffer buf = ByteBuffer.allocate(64);
        color.write(buf);
        buf.flip();
        Assert.assertEquals(19, buf.remaining());
    }
    
    @Test
    public void testColorMov() throws Exception {
        ColorExtension color = ColorExtension.createColr();
        ByteBuffer buf = ByteBuffer.allocate(64);
        color.write(buf);
        buf.flip();
        Assert.assertEquals(18, buf.remaining());
    }

}
