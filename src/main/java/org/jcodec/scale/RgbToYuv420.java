package org.jcodec.scale;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Transforms Picture in RGB colorspace ( one plane, 3 integers per pixel ) to
 * Yuv420 colorspace output picture ( 3 planes, luma - 0th plane, cb - 1th
 * plane, cr - 2nd plane; cb and cr planes are half width and half height )
 * 
 * @author The JCodec project
 * 
 */
public class RgbToYuv420 implements Transform {

    private int upShift;
    private int downShift;
    private int downShiftChr;

    public RgbToYuv420(int upShift, int downShift) {
        this.upShift = upShift;
        this.downShift = downShift;
        this.downShiftChr = downShift + 2;
    }

    public void transform(Picture img, Picture dst) {

        int[] y = img.getData()[0];
        int[][] dstData = dst.getData();

        int offChr = 0, offLuma = 0, offSrc = 0, strideSrc = img.getWidth() * 3, strideDst = dst.getWidth();
        for (int i = 0; i < img.getHeight() >> 1; i++) {
            for (int j = 0; j < img.getWidth() >> 1; j++) {
                dstData[1][offChr] = 0;
                dstData[2][offChr] = 0;

                rgb2yuv(y[offSrc], y[offSrc + 1], y[offSrc + 2], dstData[0], offLuma, dstData[1], offChr, dstData[2],
                        offChr);
                dstData[0][offLuma] = (dstData[0][offLuma] << upShift) >> downShift;

                rgb2yuv(y[offSrc + strideSrc], y[offSrc + strideSrc + 1], y[offSrc + strideSrc + 2], dstData[0],
                        offLuma + strideDst, dstData[1], offChr, dstData[2], offChr);
                dstData[0][offLuma + strideDst] = (dstData[0][offLuma + strideDst] << upShift) >> downShift;

                ++offLuma;

                rgb2yuv(y[offSrc + 3], y[offSrc + 4], y[offSrc + 5], dstData[0], offLuma, dstData[1], offChr,
                        dstData[2], offChr);
                dstData[0][offLuma] = (dstData[0][offLuma] << upShift) >> downShift;

                rgb2yuv(y[offSrc + strideSrc + 3], y[offSrc + strideSrc + 4], y[offSrc + strideSrc + 5], dstData[0],
                        offLuma + strideDst, dstData[1], offChr, dstData[2], offChr);
                dstData[0][offLuma + strideDst] = (dstData[0][offLuma + strideDst] << upShift) >> downShift;
                ++offLuma;

                dstData[1][offChr] = (dstData[1][offChr] << upShift) >> downShiftChr;
                dstData[2][offChr] = (dstData[2][offChr] << upShift) >> downShiftChr;

                ++offChr;
                offSrc += 6;
            }
            offLuma += strideDst;
            offSrc += strideSrc;
        }
    }

    public static final void rgb2yuv(int r, int g, int b, int[] Y, int offY, int[] U, int offU, int[] V, int offV) {
        int y = 66 * r + 129 * g + 25 * b;
        int u = -38 * r - 74 * g + 112 * b;
        int v = 112 * r - 94 * g - 18 * b;
        y = (y + 128) >> 8;
        u = (u + 128) >> 8;
        v = (v + 128) >> 8;

        Y[offY] = clip(y + 16);
        U[offU] += clip(u + 128);
        V[offV] += clip(v + 128);
    }

    private static final int clip(int val) {
        return val < 0 ? 0 : (val > 255 ? 255 : val);
    }
}
