package org.jcodec.scale;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv422pToRgb implements Transform {

    private int downShift;
    private int upShift;

    public Yuv422pToRgb(int downShift, int upShift) {
        this.downShift = downShift;
        this.upShift = upShift;
    }

    public void transform(Picture src, Picture dst) {
        int[] y = src.getPlaneData(0);
        int[] u = src.getPlaneData(1);
        int[] v = src.getPlaneData(2);

        int[] data = dst.getPlaneData(0);

        int offLuma = 0, offChroma = 0;
        for (int i = 0; i < dst.getHeight(); i++) {
            for (int j = 0; j < dst.getWidth(); j += 2) {
                YUV444toRGB888((y[offLuma] << upShift) >> downShift, (u[offChroma] << upShift) >> downShift,
                        (v[offChroma] << upShift) >> downShift, data, offLuma * 3);
                YUV444toRGB888((y[offLuma + 1] << upShift) >> downShift, (u[offChroma] << upShift) >> downShift,
                        (v[offChroma] << upShift) >> downShift, data, (offLuma + 1) * 3);
                offLuma += 2;
                ++offChroma;
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
        data[off] = crop(b);
        data[off + 1] = crop(g);
        data[off + 2] = crop(r);
    }

    private static int crop(int val) {
        return val < 0 ? 0 : (val > 255 ? 255 : val);
    }
}
