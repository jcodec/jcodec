package org.jcodec.scale;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class Yuv420pToRgbTest {

    public static final byte[] rgb = { 123, -128, -43, 127, 19, 127, 0, -128, 7, 127, 11, 127, 122, -128, -44, 127,
            -97, 26, 127, -14, 127, 127, -3, 127, -128, 33, -128, -8, 127, -88, -128, -21, -128, -3, 127, -14, -128,
            100, -128, -54, 127, -128, -54, 127, -66, 9, 127, -3 };
    public static final byte[] y = { -58, 100, -102, 79, -59, 1, 57, 67, -70, 109, -86, 86, -12, 70, 42, 96 };
    public static final byte[] u = { 11, 61, -108, -58 };
    public static final byte[] v = { 118, 73, -86, -66 };

    public static final byte[] rgbH = { -121, -25, -126, -51, 45, -56, 127, 8, 127, 127, 46, 127, -128, -94, -128,
            -103, -8, -108, 127, -6, 127, 127, 14, 127, 98, 65, 70, 127, 127, 127, 57, -128, 97, 127, -128, 127, 90,
            57, 62, 92, 58, 63, 127, -128, 127, 127, 19, 127, };
    public static final byte[] rgbL = { 0, -2, 0, 1, -1, 1, 3, 0, 3, 3, 0, 3, 0, -1, 0, -1, 1, -1, 3, -1, 3, 3, -1, 3,
            1, 0, 0, 3, 3, 3, -2, 0, 0, 3, 0, 3, 0, -1, -1, -2, 1, 1, 3, 0, 3, 3, -2, 3 };
    public static final byte[] yH = { -58, 2, 69, 102, -117, -43, 57, 74, 63, 118, -100, -15, 56, 57, -17, 123 };
    public static final byte[] yL = { -1, 0, 1, 0, -1, 0, 0, 1, -1, 0, 1, -1, -1, 0, 0, 1 };
    public static final byte[] uH = { -30, 55, -3, 105 };
    public static final byte[] uL = { 0, 1, 1, -2 };
    public static final byte[] vH = { -35, 66, 14, 107 };
    public static final byte[] vL = { 1, -1, 1, -2 };

    @Test
    public void testYuv420pToRgbN2N() {
        Yuv420pToRgb transform = new Yuv420pToRgb();
        Picture _in = Picture.createPicture(4, 4, new byte[][] { y, u, v }, ColorSpace.YUV420);
        Picture out = Picture.create(4, 4, ColorSpace.RGB);
        transform.transform(_in, out);

        Assert.assertArrayEquals(rgb, out.getPlaneData(0));
    }
    
    @Test
    public void testYuv420pToRgbH2N() {
        Yuv420pToRgb transform = new Yuv420pToRgb();
        Picture _in = Picture.createPictureHiBD(4, 4, new byte[][] { y, u, v }, new byte[][] { null, null, null },
                ColorSpace.YUV420, 2);
        Picture out = Picture.create(4, 4, ColorSpace.RGB);
        transform.transform(_in, out);

        Assert.assertArrayEquals(rgb, out.getPlaneData(0));
    }
    
    @Test
    public void testYuv420pToRgbN2H() {
        Yuv420pToRgb transform = new Yuv420pToRgb();
        Picture _in = Picture.createPicture(4, 4, new byte[][] { y, u, v }, ColorSpace.YUV420);
        Picture out = Picture.createCropped(4, 4, 2, ColorSpace.RGB, null);
        transform.transform(_in, out);

        Assert.assertArrayEquals(rgb, out.getPlaneData(0));
    }

    @Test
    public void testYuv420pToRgbH2H() {
        Yuv420pToRgb transform = new Yuv420pToRgb();
        Picture _in = Picture.createPictureHiBD(4, 4, new byte[][] { yH, uH, vH }, new byte[][] { yL, uL, vL },
                ColorSpace.YUV420, 2);
        Picture out = Picture.createCropped(4, 4, 2, ColorSpace.RGB, null);
        transform.transform(_in, out);

        Assert.assertArrayEquals(rgbH, out.getPlaneData(0));
        Assert.assertArrayEquals(rgbL, out.getLowBits()[0]);
    }
}
