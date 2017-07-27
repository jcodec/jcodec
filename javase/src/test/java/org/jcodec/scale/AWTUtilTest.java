package org.jcodec.scale;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Assert;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class AWTUtilTest {

    @Test
    public void testToBufferedImage8Bit() {
        Picture8Bit src = Picture8Bit.create(16, 16, ColorSpace.YUV420J);
        Arrays.fill(src.getPlaneData(0), (byte) (169 - 128));
        Arrays.fill(src.getPlaneData(1), (byte) (45 - 128));
        Arrays.fill(src.getPlaneData(2), (byte) (103 - 128));

        BufferedImage image = AWTUtil.toBufferedImage8Bit(src);
        for (int i = 0; i < 256; i++) {
            int rgb = image.getRGB(i % 16, i / 16);
            Assert.assertEquals((rgb >> 16) & 0xff, 134);
            Assert.assertEquals((rgb >> 8) & 0xff, 215);
            Assert.assertEquals((rgb) & 0xff, 22);
        }
    }

    @Test
    public void testFromBufferedImage8Bit() {
        BufferedImage src = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for(int i = 0; i < 256; i++) {
            src.setRGB(i % 16, i / 16, (0xff << 24) | (134 << 16) | (215 << 8) | 22);
        }
        
        Picture8Bit dst = AWTUtil.fromBufferedImage8Bit(src, ColorSpace.YUV420J);
        for (int i = 0; i < 256; i += 3) {
            Assert.assertEquals(168, dst.getPlaneData(0)[i] + 128);
            Assert.assertEquals(45, dst.getPlaneData(1)[i >> 2] + 128);
            Assert.assertEquals(103, dst.getPlaneData(2)[i >> 2] + 128);
        }
    }
}
