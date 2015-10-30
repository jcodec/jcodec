package org.jcodec.scale;

import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv420jToRgb8Bit implements Transform8Bit {

    public Yuv420jToRgb8Bit() {
    }

    public final void transform(Picture8Bit src, Picture8Bit dst) {
        byte[] y = src.getPlaneData(0);
        byte[] u = src.getPlaneData(1);
        byte[] v = src.getPlaneData(2);
        byte[] data = dst.getPlaneData(0);

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

    public final static void YUVJtoRGB(byte y, byte cb, byte cr, byte[] data, int off) {
        int y_ = (y + 128) << SCALEBITS;
        int add_r = FIX_1_402 * cr + ONE_HALF;
        int add_g = _FIX_0_34414 * cb - FIX_0_71414 * cr + ONE_HALF;
        int add_b = FIX_1_772 * cb + ONE_HALF;

        int r = (y_ + add_r) >> SCALEBITS;
        int g = (y_ + add_g) >> SCALEBITS;
        int b = (y_ + add_b) >> SCALEBITS;
        data[off] = (byte) clip(b - 128, -128, 127);
        data[off + 1] = (byte) clip(g - 128, -128, 127);
        data[off + 2] = (byte) clip(r - 128, -128, 127);
    }
}