package org.jcodec.scale;

import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Transforms Picture in RGB colorspace ( one plane, 3 integers per pixel ) to
 * Yuv420 colorspace output picture ( 3 planes, luma - 0th plane, cb - 1th
 * plane, cr - 2nd plane; cb and cr planes are half width and half haight )
 * 
 * TODO: implement jpeg colorspace instead of NTSC
 * 
 * @author The JCodec project
 * 
 */
public class RgbToYuv420j8Bit implements Transform8Bit {

    public RgbToYuv420j8Bit() {
    }

    public void transform(Picture8Bit img, Picture8Bit dst) {

        byte[] y = img.getData()[0];
        byte[][] dstData = dst.getData();

        int offChr = 0, offLuma = 0, offSrc = 0, strideSrc = img.getWidth() * 3, strideDst = dst.getWidth();
        for (int i = 0; i < img.getHeight() >> 1; i++) {
            for (int j = 0; j < img.getWidth() >> 1; j++) {
                dstData[1][offChr] = 0;
                dstData[2][offChr] = 0;

                rgb2yuv(y[offSrc], y[offSrc + 1], y[offSrc + 2], dstData[0], offLuma, dstData[1], offChr, dstData[2],
                        offChr);
                dstData[0][offLuma] = dstData[0][offLuma];

                rgb2yuv(y[offSrc + strideSrc], y[offSrc + strideSrc + 1], y[offSrc + strideSrc + 2], dstData[0],
                        offLuma + strideDst, dstData[1], offChr, dstData[2], offChr);
                dstData[0][offLuma + strideDst] = dstData[0][offLuma + strideDst];

                ++offLuma;

                rgb2yuv(y[offSrc + 3], y[offSrc + 4], y[offSrc + 5], dstData[0], offLuma, dstData[1], offChr,
                        dstData[2], offChr);
                dstData[0][offLuma] = dstData[0][offLuma];

                rgb2yuv(y[offSrc + strideSrc + 3], y[offSrc + strideSrc + 4], y[offSrc + strideSrc + 5], dstData[0],
                        offLuma + strideDst, dstData[1], offChr, dstData[2], offChr);
                dstData[0][offLuma + strideDst] = dstData[0][offLuma + strideDst];
                ++offLuma;

                dstData[1][offChr] = (byte) (dstData[1][offChr] >> 2);
                dstData[2][offChr] = (byte) (dstData[2][offChr] >> 2);

                ++offChr;
                offSrc += 6;
            }
            offLuma += strideDst;
            offSrc += strideSrc;
        }
    }

    public static final void rgb2yuv(byte r, byte g, byte b, byte[] Y, int offY, byte[] U, int offU, byte[] V, int offV) {
        int y = 66 * r + 129 * g + 25 * b;
        int u = -38 * (r + 128) - 74 * (g + 128) + 112 * (b + 128);
        int v = 112 * (r + 128) - 94 * (g + 128) - 18 * (b + 128);
        y = (y + 128) >> 8;
        u = (u + 128) >> 8;
        v = (v + 128) >> 8;

        Y[offY] = (byte) clip(y + 16, -128, 127);
        U[offU] += (byte) clip(u, -128, 127);
        V[offV] += (byte) clip(v, -128, 127);
    }
}