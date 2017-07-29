package org.jcodec.scale;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv420pToRgb implements Transform {

    public Yuv420pToRgb() {
    }

    @Override
    public final void transform(Picture src, Picture dst) {
        byte[] y = src.getPlaneData(0);
        byte[] u = src.getPlaneData(1);
        byte[] v = src.getPlaneData(2);
        byte[] data = dst.getPlaneData(0);

        int offLuma = 0, offChroma = 0;
        int stride = dst.getWidth();
        for (int i = 0; i < (dst.getHeight() >> 1); i++) {
            for (int k = 0; k < (dst.getWidth() >> 1); k++) {
                int j = k << 1;
                YUV420pToRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUV420pToRGB(y[offLuma + j + 1], u[offChroma], v[offChroma], data, (offLuma + j + 1) * 3);

                YUV420pToRGB(y[offLuma + j + stride], u[offChroma], v[offChroma], data, (offLuma + j + stride) * 3);
                YUV420pToRGB(y[offLuma + j + stride + 1], u[offChroma], v[offChroma], data,
                        (offLuma + j + stride + 1) * 3);

                ++offChroma;
            }
            if ((dst.getWidth() & 0x1) != 0) {
                int j = dst.getWidth() - 1;

                YUV420pToRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUV420pToRGB(y[offLuma + j + stride], u[offChroma], v[offChroma], data, (offLuma + j + stride) * 3);

                ++offChroma;
            }

            offLuma += 2 * stride;
        }
        if ((dst.getHeight() & 0x1) != 0) {
            for (int k = 0; k < (dst.getWidth() >> 1); k++) {
                int j = k << 1;
                YUV420pToRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUV420pToRGB(y[offLuma + j + 1], u[offChroma], v[offChroma], data, (offLuma + j + 1) * 3);

                ++offChroma;
            }
            if ((dst.getWidth() & 0x1) != 0) {
                int j = dst.getWidth() - 1;

                YUV420pToRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);

                ++offChroma;
            }
        }
    }

    public static final void YUV420pToRGB(final byte y, final byte u, final byte v, byte[] data, int off) {
        final int c = y + 112;
        final int r = (298 * c + 409 * v + 128) >> 8;
        final int g = (298 * c - 100 * u - 208 * v + 128) >> 8;
        final int b = (298 * c + 516 * u + 128) >> 8;
        data[off] = (byte) (crop(r) - 128);
        data[off + 1] = (byte) (crop(g) - 128);
        data[off + 2] = (byte) (crop(b) - 128);
    }

    private static int crop(int val) {
        return val < 0 ? 0 : (val > 255 ? 255 : val);
    }
}