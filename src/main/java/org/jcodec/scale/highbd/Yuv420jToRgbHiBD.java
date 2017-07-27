package org.jcodec.scale.highbd;

import org.jcodec.common.model.PictureHiBD;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv420jToRgbHiBD implements TransformHiBD {

    public Yuv420jToRgbHiBD() {
    }

    public final void transform(PictureHiBD src, PictureHiBD dst) {
        int[] y = src.getPlaneData(0);
        int[] u = src.getPlaneData(1);
        int[] v = src.getPlaneData(2);
        int[] data = dst.getPlaneData(0);

        int offLuma = 0, offChroma = 0;
        int stride = dst.getWidth();
        for (int i = 0; i < (dst.getHeight() >> 1); i++) {
            for (int k = 0; k < (dst.getWidth() >> 1); k++) {
                int j = k << 1;
                YUVJtoRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUVJtoRGB(y[offLuma + j + 1], u[offChroma], v[offChroma], data, (offLuma + j + 1) * 3);

                YUVJtoRGB(y[offLuma + j + stride], u[offChroma], v[offChroma], data, (offLuma + j + stride) * 3);
                YUVJtoRGB(y[offLuma + j + stride + 1], u[offChroma], v[offChroma], data, (offLuma + j + stride + 1) * 3);

                ++offChroma;
            }
            if ((dst.getWidth() & 0x1) != 0) {
                int j = dst.getWidth() - 1;

                YUVJtoRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUVJtoRGB(y[offLuma + j + stride], u[offChroma], v[offChroma], data, (offLuma + j + stride) * 3);

                ++offChroma;
            }

            offLuma += 2 * stride;
        }
        if ((dst.getHeight() & 0x1) != 0) {
            for (int k = 0; k < (dst.getWidth() >> 1); k++) {
                int j = k << 1;
                YUVJtoRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUVJtoRGB(y[offLuma + j + 1], u[offChroma], v[offChroma], data, (offLuma + j + 1) * 3);

                ++offChroma;
            }
            if ((dst.getWidth() & 0x1) != 0) {
                int j = dst.getWidth() - 1;

                YUVJtoRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);

                ++offChroma;
            }
        }
    }

    private static final int SCALEBITS = 10;
    private static final int ONE_HALF = (1 << (SCALEBITS - 1));

    private final static int FIX(double x) {
        return ((int) ((x) * (1 << SCALEBITS) + 0.5));
    }

    private static final int FIX_0_71414 = FIX(0.71414);
    private static final int FIX_1_772 = FIX(1.77200);
    private static final int _FIX_0_34414 = -FIX(0.34414);
    private static final int FIX_1_402 = FIX(1.40200);

    public final static void YUVJtoRGB(int y, int cb, int cr, int[] data, int off) {
        y = y << SCALEBITS;
        cb = cb - 128;
        cr = cr - 128;
        int add_r = FIX_1_402 * cr + ONE_HALF;
        int add_g = _FIX_0_34414 * cb - FIX_0_71414 * cr + ONE_HALF;
        int add_b = FIX_1_772 * cb + ONE_HALF;

        int r = (y + add_r) >> SCALEBITS;
        int g = (y + add_g) >> SCALEBITS;
        int b = (y + add_b) >> SCALEBITS;
        data[off] = MathUtil.clip(r, 0, 255);
        data[off + 1] = MathUtil.clip(g, 0, 255);
        data[off + 2] = MathUtil.clip(b, 0, 255);
    }
}