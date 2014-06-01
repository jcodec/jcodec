package org.jcodec.codecs.vpx;

import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class VP8InterPrediction {

    public static void getBlock(int[] src, int srcW, int srcH, int[] dst, int dstOff,int dstW, int mvX, int mvY,
            int blkW, int blkH) {
        int xr = mvX >> 3, yr = mvY >> 3;
        int xim = mvX & 7, yim = mvY & 7;

        if (xr < 0 || yr < 0 || xr + blkW > srcW - 1 || yr + blkH > srcH - 1) {
            int maxH = srcH - 1;
            int maxW = srcW - 1;

            for (int j = 0, o1 = dstOff; j < blkH + 1; j++) {
                int lineStart = MathUtil.clip(j + yr, 0, maxH) * srcW;

                for (int i = 0; i < blkW + 1; i++) 
                    dst[o1 + i] = src[lineStart + MathUtil.clip(xr + i, 0, maxW)];
                o1 += dstW;
            }
        } else {
            for (int j = 0, off = yr * srcW + xr, o1 = dstOff; j < blkH + 1; j++, off += srcW, o1 += dstW) {
                System.arraycopy(src, off, dst, o1, blkW + 1);
            }
        }

        if (xim != 0) {
            int s1 = xim, s2 = 8 - s1;
            for (int i = 0, off = dstOff; i < blkH + 1; i++, off += dstW) {
                for (int j = 0; j < blkW; j++) {
                    dst[off + j] = ((dst[off + j] * s2) + (dst[off + j + 1] * s1) + 4) >> 3;
                }
            }
        }
        if (yim != 0) {
            int s1 = yim, s2 = 8 - s1;
            for (int j = 0; j < blkW; j++) {
                for (int i = 0, off = dstOff; i < blkH; i++, off += dstW) {
                    dst[off + j] = ((dst[off + j] * s2) + (dst[off + j + dstW] * s1) + 4) >> 3;
                }
            }
        }
    }
}
