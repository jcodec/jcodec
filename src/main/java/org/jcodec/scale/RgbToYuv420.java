package org.jcodec.scale;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
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
        int[][] dstData = img.getData();

        int off = 0, offSrc = 0;
        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth() >> 1; j++) {
                int offY = off << 2;

                rgb2yuv(y[offSrc++], y[offSrc++], y[offSrc++], dstData[0], offY, dstData[1], off, dstData[2], off);
                dstData[0][offY] = (dstData[0][offY] << upShift) >> downShift;
                rgb2yuv(y[offSrc++], y[offSrc++], y[offSrc++], dstData[0], offY + 1, dstData[1], off, dstData[2], off);
                dstData[0][offY + 1] = (dstData[0][offY + 1] << upShift) >> downShift;
                rgb2yuv(y[offSrc++], y[offSrc++], y[offSrc++], dstData[0], offY + 2, dstData[1], off, dstData[2], off);
                dstData[0][offY + 2] = (dstData[0][offY + 2] << upShift) >> downShift;
                rgb2yuv(y[offSrc++], y[offSrc++], y[offSrc++], dstData[0], offY + 3, dstData[1], off, dstData[2], off);
                dstData[0][offY + 3] = (dstData[0][offY + 3] << upShift) >> downShift;

                dstData[1][off] = (dstData[1][off] << upShift) >> downShiftChr;
                dstData[2][off] = (dstData[2][off] << upShift) >> downShiftChr;

                ++off;
            }
        }
    }

    public static final void rgb2yuv(int r, int g, int b, int[] Y, int offY, int[] U, int offU, int[] V, int offV) {
        int y = 66 * r + 129 * g + 25 * b;
        int u = -38 * r - 74 * g + 112 * b;
        int v = 112 * r - 94 * g - 18 * b;
        y = (y + 128) >> 8;
        u = (u + 128) >> 8;
        v = (v + 128) >> 8;

        Y[offY] += clip(y + 16);
        U[offU] += clip(u + 128);
        V[offV] += clip(v + 128);
    }

    private static final int clip(int val) {
        return val < 0 ? 0 : (val > 255 ? 255 : val);
    }
}