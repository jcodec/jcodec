package org.jcodec.scale;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class Yuv444jToYuv420jTest {
    
    @Test
    public void testEvenEven() {
        byte[][] srcPix = {{30, 50, 100, 120}, {40, 90, 20, 50}, {30, 60, 100, 120}};
        byte[][] expPix = {{30, 50, 100, 120}, {50}, {78}};
        Picture src = Picture.createPicture(2, 2, srcPix, ColorSpace.YUV444J);
        Picture dst = Picture.create(2, 2, ColorSpace.YUV420J);
        Transform transform = ColorUtil.getTransform(src.getColor(), dst.getColor());
        Assert.assertNotNull(transform);
        transform.transform(src, dst);
        Assert.assertArrayEquals(expPix[1], dst.getPlaneData(1));
        Assert.assertArrayEquals(expPix[2], dst.getPlaneData(2));
    }
}
