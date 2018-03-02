package org.jcodec.scale.highbd;

import org.jcodec.common.model.PictureHiBD;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv444pToRgb implements TransformHiBD {
    private int downShift;
    private int upShift;

    public Yuv444pToRgb(int downShift, int upShift) {
        this.downShift = downShift;
        this.upShift = upShift;
    }

    public void transform(PictureHiBD src, PictureHiBD dst) {
        int[] y = src.getPlaneData(0);
        int[] u = src.getPlaneData(1);
        int[] v = src.getPlaneData(2);

        int[] data = dst.getPlaneData(0);

        for (int i = 0, srcOff = 0, dstOff = 0; i < dst.getHeight(); i++) {
            for (int j = 0; j < dst.getWidth(); j++, srcOff++, dstOff += 3) {
                YUV444toRGB888((y[srcOff] << upShift) >> downShift, (u[srcOff] << upShift) >> downShift,
                        (v[srcOff] << upShift) >> downShift, data, dstOff);
            }
        }
    }

    public static final void YUV444toRGB888(final int y, final int u, final int v, int[] data, int off) {
        final int c = y - 16;
        final int d = u - 128;
        final int e = v - 128;

        final int r = (298 * c + 409 * e + 128) >> 8;
        final int g = (298 * c - 100 * d - 208 * e + 128) >> 8;
        final int b = (298 * c + 516 * d + 128) >> 8;
        data[off] = crop(r);
        data[off + 1] = crop(g);
        data[off + 2] = crop(b);
    }

    private static int crop(int val) {
        return val < 0 ? 0 : (val > 255 ? 255 : val);
    }
}
