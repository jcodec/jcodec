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
public class Yuv422pToRgb8Bit implements Transform8Bit {

    @Override
    public void transform(Picture8Bit src, Picture8Bit dst) {
        byte[] y = src.getPlaneData(0);
        byte[] u = src.getPlaneData(1);
        byte[] v = src.getPlaneData(2);

        byte[] data = dst.getPlaneData(0);

        int offLuma = 0, offChroma = 0;
        for (int i = 0; i < dst.getHeight(); i++) {
            for (int j = 0; j < dst.getWidth(); j += 2) {
                YUV444toRGB888(y[offLuma], u[offChroma], v[offChroma], data, offLuma * 3);
                YUV444toRGB888(y[offLuma + 1], u[offChroma], v[offChroma], data, (offLuma + 1) * 3);
                offLuma += 2;
                ++offChroma;
            }
        }

    }

    public static final void YUV444toRGB888(final byte y, final byte u, final byte v, byte[] data, int off) {
        final int c = y + 112;
        final int d = u;
        final int e = v;

        final int r = (298 * c + 409 * e + 128) >> 8;
        final int g = (298 * c - 100 * d - 208 * e + 128) >> 8;
        final int b = (298 * c + 516 * d + 128) >> 8;
        data[off] = (byte) (clip(r, 0, 255) - 128);
        data[off + 1] = (byte) (clip(g, 0, 255) - 128);
        data[off + 2] = (byte) (clip(b, 0, 255) - 128);
    }
}
