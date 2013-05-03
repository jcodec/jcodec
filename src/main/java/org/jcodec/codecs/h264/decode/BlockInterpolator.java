package org.jcodec.codecs.h264.decode;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Interpolator that operates on block level
 * 
 * @author Jay Codec
 * 
 */
public class BlockInterpolator {

    private static int[] tmp1 = new int[1024];

    /**
     * Get block of ( possibly interpolated ) luma pixels
     */
    public static void getBlockLuma(Picture pic, Picture out, int off, int x, int y, int w, int h) {
        int xInd = x & 0x3;
        int yInd = y & 0x3;

        int xFp = x >> 2;
        int yFp = y >> 2;
        if (xFp < 2 || yFp < 2 || xFp > pic.getWidth() - w - 5 || yFp > pic.getHeight() - h - 5) {
            unsafe[(yInd << 2) + xInd].getLuma(pic.getData()[0], pic.getWidth(), pic.getHeight(), out.getPlaneData(0),
                    off, out.getPlaneWidth(0), xFp, yFp, w, h);
        } else {
            safe[(yInd << 2) + xInd].getLuma(pic.getData()[0], pic.getWidth(), pic.getHeight(), out.getPlaneData(0),
                    off, out.getPlaneWidth(0), xFp, yFp, w, h);
        }
    }

    public static void getBlockChroma(int[] pels, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
         int xInd = x & 0x7;
         int yInd = y & 0x7;
        
         int xFull = x >> 3;
         int yFull = y >> 3;

        if (xFull < 0 || xFull > picW - blkW - 1 || yFull < 0 || yFull > picH - blkH - 1) {
            if (xInd == 0 && yInd == 0) {
                getChroma00Unsafe(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, blkW, blkH);
            } else if (yInd == 0) {

                getChromaX0Unsafe(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, xInd, blkW, blkH);

            } else if (xInd == 0) {
                getChroma0XUnsafe(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, yInd, blkW, blkH);
            } else {
                getChromaXXUnsafe(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, xInd, yInd, blkW, blkH);
            }
        } else {
            if (xInd == 0 && yInd == 0) {
                getChroma00(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, blkW, blkH);
            } else if (yInd == 0) {

                getChromaX0(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, xInd, blkW, blkH);

            } else if (xInd == 0) {
                getChroma0X(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, yInd, blkW, blkH);
            } else {
                getChromaXX(pels, picW, picH, blk, blkOff, blkStride, xFull, yFull, xInd, yInd, blkW, blkH);
            }
        }
    }

    /**
     * Fullpel (0, 0)
     */
    private static void getLuma00(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            System.arraycopy(pic, off, blk, blkOff, blkW);
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Fullpel (0, 0) unsafe
     */
    private static void getLuma00Unsafe(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        for (int j = 0; j < blkH; j++) {
            int lineStart = iClip3(0, maxH, j + y) * picW;

            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = pic[lineStart + iClip3(0, maxW, x + i)];
            }
            blkOff += blkStride;
        }
    }

    /**
     * Halfpel (2,0) horizontal
     */
    private static void getLuma20NoRound(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {

            int off1 = -2;
            for (int i = 0; i < blkW; i++) {
                int val = (pic[off + off1] + pic[off + off1 + 5]) - 5 * (pic[off + off1 + 1] + pic[off + off1 + 4])
                        + 20 * (pic[off + off1 + 2] + pic[off + off1 + 3]);
                ++off1;

                blk[blkOff + i] = val;
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    private static void getLuma20(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        getLuma20NoRound(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = iClip3(0, 255, (blk[blkOff + i] + 16) >> 5);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Halfpel (2, 0) horizontal unsafe
     */
    private static void getLuma20UnsafeNoRound(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride,
            int x, int y, int blkW, int blkH) {
        int maxW = picW - 1;
        int maxH = picH - 1;

        for (int i = 0; i < blkW; i++) {
            int ipos_m2 = iClip3(0, maxW, x + i - 2);
            int ipos_m1 = iClip3(0, maxW, x + i - 1);
            int ipos = iClip3(0, maxW, x + i);
            int ipos_p1 = iClip3(0, maxW, x + i + 1);
            int ipos_p2 = iClip3(0, maxW, x + i + 2);
            int ipos_p3 = iClip3(0, maxW, x + i + 3);

            int boff = blkOff;
            for (int j = 0; j < blkH; j++) {
                int lineStart = iClip3(0, maxH, j + y) * picW;

                int result = (pic[lineStart + ipos_m2] + pic[lineStart + ipos_p3])
                        - (pic[lineStart + ipos_m1] + pic[lineStart + ipos_p2]) * 5
                        + (pic[lineStart + ipos] + pic[lineStart + ipos_p1]) * 20;

                blk[boff + i] = result;
                boff += blkStride;
            }
        }
    }

    private static void getLuma20Unsafe(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {

        getLuma20UnsafeNoRound(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int i = 0; i < blkW; i++) {
            int boff = blkOff;
            for (int j = 0; j < blkH; j++) {
                blk[boff + i] = iClip3(0, 255, (blk[boff + i] + 16) >> 5);
                boff += blkStride;
            }
        }
    }

    /**
     * Halfpel (0, 2) vertical
     */
    private static void getLuma02NoRound(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int off = (y - 2) * picW + x;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int val = (pic[off + i] + pic[off + i + 5 * picW]) - 5
                        * (pic[off + i + picW] + pic[off + i + 4 * picW]) + 20
                        * (pic[off + i + 2 * picW] + pic[off + i + 3 * picW]);
                blk[blkOff + i] = val;
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    private static void getLuma02(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma02NoRound(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {

                blk[blkOff + i] = iClip3(0, 255, (blk[blkOff + i] + 16) >> 5);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Hpel (0, 2) vertical unsafe
     */
    private static void getLuma02UnsafeNoRound(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride,
            int x, int y, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        for (int j = 0; j < blkH; j++) {
            int offP0 = iClip3(0, maxH, y + j - 2) * picW;
            int offP1 = iClip3(0, maxH, y + j - 1) * picW;
            int offP2 = iClip3(0, maxH, y + j) * picW;
            int offP3 = iClip3(0, maxH, y + j + 1) * picW;
            int offP4 = iClip3(0, maxH, y + j + 2) * picW;
            int offP5 = iClip3(0, maxH, y + j + 3) * picW;

            for (int i = 0; i < blkW; i++) {
                int pres_x = iClip3(0, maxW, x + i);

                int result = (pic[pres_x + offP0] + pic[pres_x + offP5]) - (pic[pres_x + offP1] + pic[pres_x + offP4])
                        * 5 + (pic[pres_x + offP2] + pic[pres_x + offP3]) * 20;
                blk[blkOff + i] = result;
            }
            blkOff += blkStride;
        }
    }

    private static void getLuma02Unsafe(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {

        getLuma02UnsafeNoRound(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = iClip3(0, 255, (blk[blkOff + i] + 16) >> 5);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Qpel: (1,0) horizontal
     */
    private static void getLuma10(int[] oic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        getLuma20(oic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {

            for (int i = 0; i < blkW; i++) {

                blk[blkOff + i] = (blk[blkOff + i] + oic[off + i] + 1) >> 1;
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Qpel: (1,0) horizontal unsafe
     */
    private static void getLuma10Unsafe(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        getLuma20Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            int lineStart = iClip3(0, maxH, j + y) * picW;

            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[lineStart + iClip3(0, maxW, x + i)] + 1) >> 1;
            }
            blkOff += blkStride;
        }
    }

    /**
     * Qpel (3,0) horizontal
     */
    private static void getLuma30(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        getLuma20(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (pic[off + i + 1] + blk[blkOff + i] + 1) >> 1;
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Qpel horizontal (3, 0) unsafe
     */
    private static void getLuma30Unsafe(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        getLuma20Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            int lineStart = iClip3(0, maxH, j + y) * picW;

            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[lineStart + iClip3(0, maxW, x + i + 1)] + 1) >> 1;
            }
            blkOff += blkStride;
        }
    }

    /**
     * Qpel vertical (0, 1)
     */
    private static void getLuma01(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        getLuma02(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[off + i] + 1) >> 1;
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Qpel vertical (0, 1) unsafe
     */
    private static void getLuma01Unsafe(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        getLuma02Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            int lineStart = iClip3(0, maxH, y + j) * picW;
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[lineStart + iClip3(0, maxW, x + i)] + 1) >> 1;
            }
            blkOff += blkStride;
        }
    }

    /**
     * Qpel vertical (0, 3)
     */
    private static void getLuma03(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        getLuma02(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[off + i + picW] + 1) >> 1;
            }
            off += picW;
            blkOff += blkStride;

        }
    }

    /**
     * Qpel vertical (0, 3) unsafe
     */
    private static void getLuma03Unsafe(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        getLuma02Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            int lineStart = iClip3(0, maxH, y + j + 1) * picW;
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (blk[blkOff + i] + pic[lineStart + iClip3(0, maxW, x + i)] + 1) >> 1;
            }
            blkOff += blkStride;
        }
    }

    /**
     * Hpel horizontal, Qpel vertical (2, 1)
     * 
     */
    private static void getLuma21(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, blk, blkOff, blkStride, 0, 2, blkW, blkH);

        int off = blkW << 1;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
                int rounded2 = iClip3(0, 255, (tmp1[off + i] + 16) >> 5);
                blk[blkOff + i] = (rounded + rounded2 + 1) >> 1;
            }
            blkOff += blkStride;
            off += blkW;
        }
    }

    /**
     * Qpel vertical (2, 1) unsafe
     */
    private static void getLuma21Unsafe(int[] pic, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, blk, blkOff, blkStride, 0, 2, blkW, blkH);

        int off = blkW << 1;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
                int rounded2 = iClip3(0, 255, (tmp1[off + i] + 16) >> 5);
                blk[blkOff + i] = (rounded + rounded2 + 1) >> 1;
            }
            blkOff += blkStride;
            off += blkW;
        }
    }

    /**
     * Hpel horizontal, Hpel vertical (2, 2)
     */
    private static void getLuma22(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, blk, blkOff, blkStride, 0, 2, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Hpel (2, 2) unsafe
     */
    private static void getLuma22Unsafe(int[] pic, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, blk, blkOff, blkStride, 0, 2, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Hpel horizontal, Qpel vertical (2, 3)
     * 
     */
    private static void getLuma23(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, blk, blkOff, blkStride, 0, 2, blkW, blkH);

        int off = blkW << 1;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
                int rounded2 = iClip3(0, 255, (tmp1[off + i + blkW] + 16) >> 5);
                blk[blkOff + i] = (rounded + rounded2 + 1) >> 1;
            }
            blkOff += blkStride;
            off += blkW;
        }
    }

    /**
     * Qpel (2, 3) unsafe
     */
    private static void getLuma23Unsafe(int[] pic, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, blk, blkOff, blkStride, 0, 2, blkW, blkH);

        int off = blkW << 1;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
                int rounded2 = iClip3(0, 255, (tmp1[off + i + blkW] + 16) >> 5);
                blk[blkOff + i] = (rounded + rounded2 + 1) >> 1;
            }
            blkOff += blkStride;
            off += blkW;
        }
    }

    /**
     * Qpel horizontal, Hpel vertical (1, 2)
     */
    private static void getLuma12(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        int tmpW = blkW + 7;

        getLuma02NoRound(pic, picW, tmp1, 0, tmpW, x - 2, y, tmpW, blkH);
        getLuma20NoRound(tmp1, tmpW, blk, blkOff, blkStride, 2, 0, blkW, blkH);

        int off = 2;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
                int rounded2 = iClip3(0, 255, (tmp1[off + i] + 16) >> 5);
                blk[blkOff + i] = (rounded + rounded2 + 1) >> 1;
            }
            blkOff += blkStride;
            off += tmpW;
        }
    }

    /**
     * Qpel (1, 2) unsafe
     */
    private static void getLuma12Unsafe(int[] pic, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int tmpW = blkW + 7;

        getLuma02UnsafeNoRound(pic, picW, imgH, tmp1, 0, tmpW, x - 2, y, tmpW, blkH);
        getLuma20NoRound(tmp1, tmpW, blk, blkOff, blkStride, 2, 0, blkW, blkH);

        int off = 2;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
                int rounded2 = iClip3(0, 255, (tmp1[off + i] + 16) >> 5);
                blk[blkOff + i] = (rounded + rounded2 + 1) >> 1;
            }
            blkOff += blkStride;
            off += tmpW;
        }
    }

    /**
     * Qpel horizontal, Hpel vertical (3, 2)
     */
    private static void getLuma32(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        int tmpW = blkW + 7;

        getLuma02NoRound(pic, picW, tmp1, 0, tmpW, x - 2, y, tmpW, blkH);
        getLuma20NoRound(tmp1, tmpW, blk, blkOff, blkStride, 2, 0, blkW, blkH);

        int off = 2;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
                int rounded2 = iClip3(0, 255, (tmp1[off + i + 1] + 16) >> 5);
                blk[blkOff + i] = (rounded + rounded2 + 1) >> 1;
            }
            blkOff += blkStride;
            off += tmpW;
        }
    }

    /**
     * Qpel (3, 2) unsafe
     */
    private static void getLuma32Unsafe(int[] pic, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int tmpW = blkW + 7;

        getLuma02UnsafeNoRound(pic, picW, imgH, tmp1, 0, tmpW, x - 2, y, tmpW, blkH);
        getLuma20NoRound(tmp1, tmpW, blk, blkOff, blkStride, 2, 0, blkW, blkH);

        int off = 2;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = iClip3(0, 255, (blk[blkOff + i] + 512) >> 10);
                int rounded2 = iClip3(0, 255, (tmp1[off + i + 1] + 16) >> 5);
                blk[blkOff + i] = (rounded + rounded2 + 1) >> 1;
            }
            blkOff += blkStride;
            off += tmpW;
        }
    }

    /**
     * Qpel horizontal, Qpel vertical (3, 3)
     */
    private static void getLuma33(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y + 1, blkW, blkH);
        getLuma02(pic, picW, tmp1, 0, blkW, x + 1, y, blkW, blkH);

        mergeCrap(blk, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel (3, 3) unsafe
     */
    private static void getLuma33Unsafe(int[] pic, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y + 1, blkW, blkH);
        getLuma02Unsafe(pic, picW, imgH, tmp1, 0, blkW, x + 1, y, blkW, blkH);

        mergeCrap(blk, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel horizontal, Qpel vertical (1, 1)
     */
    private static void getLuma11(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        getLuma02(pic, picW, tmp1, 0, blkW, x, y, blkW, blkH);

        mergeCrap(blk, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel (1, 1) unsafe
     */
    private static void getLuma11Unsafe(int[] pic, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        getLuma02Unsafe(pic, picW, imgH, tmp1, 0, blkW, x, y, blkW, blkH);

        mergeCrap(blk, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel horizontal, Qpel vertical (1, 3)
     */
    private static void getLuma13(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y + 1, blkW, blkH);
        getLuma02(pic, picW, tmp1, 0, blkW, x, y, blkW, blkH);

        mergeCrap(blk, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel (1, 3) unsafe
     */
    private static void getLuma13Unsafe(int[] pic, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y + 1, blkW, blkH);
        getLuma02Unsafe(pic, picW, imgH, tmp1, 0, blkW, x, y, blkW, blkH);

        mergeCrap(blk, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel horizontal, Qpel vertical (3, 1)
     */
    private static void getLuma31(int[] pels, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma20(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        getLuma02(pels, picW, tmp1, 0, blkW, x + 1, y, blkW, blkH);

        mergeCrap(blk, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel (3, 1) unsafe
     */
    private static void getLuma31Unsafe(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        getLuma20Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        getLuma02Unsafe(pels, picW, imgH, tmp1, 0, blkW, x + 1, y, blkW, blkH);

        mergeCrap(blk, blkOff, blkStride, blkW, blkH);
    }

    private static int iClip3(int min, int max, int val) {
        return val < min ? min : (val > max ? max : val);
    }

    private static void mergeCrap(int[] blk, int blkOff, int blkStride, int blkW, int blkH) {
        int tOff = 0;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (blk[blkOff + i] + tmp1[tOff + i] + 1) >> 1;
            }
            blkOff += blkStride;
            tOff += blkW;
        }
    }

    /**
     * Chroma (0,0)
     */
    private static void getChroma00(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            System.arraycopy(pic, off, blk, blkOff, blkW);
            off += picW;
            blkOff += blkStride;
        }
    }

    private static void getChroma00Unsafe(int[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        for (int j = 0; j < blkH; j++) {
            int lineStart = iClip3(0, maxH, j + y) * picW;

            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = pic[lineStart + iClip3(0, maxW, x + i)];
            }
            blkOff += blkStride;
        }
    }

    /**
     * Chroma (X,0)
     */
    private static void getChroma0X(int[] pels, int picW, int picH, int[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracY, int blkW, int blkH) {
        int w00 = fullY * picW + fullX;
        int w01 = w00 + (fullY < picH - 1 ? picW : 0);
        int eMy = 8 - fracY;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {

                blk[blkOff + i] = (eMy * pels[w00 + i] + fracY * pels[w01 + i] + 4) >> 3;
            }
            w00 += picW;
            w01 += picW;
            blkOff += blkStride;
        }
    }

    private static void getChroma0XUnsafe(int[] pels, int picW, int picH, int[] blk, int blkOff, int blkStride,
            int fullX, int fullY, int fracY, int blkW, int blkH) {

        int maxW = picW - 1;
        int maxH = picH - 1;
        int eMy = 8 - fracY;

        for (int j = 0; j < blkH; j++) {
            int off00 = iClip3(0, maxH, fullY + j) * picW;
            int off01 = iClip3(0, maxH, fullY + j + 1) * picW;

            for (int i = 0; i < blkW; i++) {
                int w00 = iClip3(0, maxW, fullX + i) + off00;
                int w01 = iClip3(0, maxW, fullX + i) + off01;

                blk[blkOff + i] = (eMy * pels[w00] + fracY * pels[w01] + 4) >> 3;
            }
            blkOff += blkStride;
        }
    }

    /**
     * Chroma (X,0)
     */
    private static void getChromaX0(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracX, int blkW, int blkH) {
        int w00 = fullY * picW + fullX;
        int w10 = w00 + (fullX < picW - 1 ? 1 : 0);
        int eMx = 8 - fracX;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (eMx * pels[w00 + i] + fracX * pels[w10 + i] + 4) >> 3;
            }
            w00 += picW;
            w10 += picW;
            blkOff += blkStride;
        }
    }

    private static void getChromaX0Unsafe(int[] pels, int picW, int picH, int[] blk, int blkOff, int blkStride,
            int fullX, int fullY, int fracX, int blkW, int blkH) {
        int eMx = 8 - fracX;
        int maxW = picW - 1;
        int maxH = picH - 1;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int w00 = iClip3(0, maxH, fullY + j) * picW + iClip3(0, maxW, fullX + i);
                int w10 = iClip3(0, maxH, fullY + j) * picW + iClip3(0, maxW, fullX + i + 1);

                blk[blkOff + i] = (eMx * pels[w00] + fracX * pels[w10] + 4) >> 3;
            }
            blkOff += blkStride;
        }
    }

    /**
     * Chroma (X,X)
     */
    private static void getChromaXX(int[] pels, int picW, int picH, int[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracX, int fracY, int blkW, int blkH) {
        int w00 = fullY * picW + fullX;
        int w01 = w00 + (fullY < picH - 1 ? picW : 0);
        int w10 = w00 + (fullX < picW - 1 ? 1 : 0);
        int w11 = w10 + w01 - w00;
        int eMx = 8 - fracX;
        int eMy = 8 - fracY;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {

                blk[blkOff + i] = (eMx * eMy * pels[w00 + i] + fracX * eMy * pels[w10 + i] + eMx * fracY
                        * pels[w01 + i] + fracX * fracY * pels[w11 + i] + 32) >> 6;
            }
            blkOff += blkStride;
            w00 += picW;
            w01 += picW;
            w10 += picW;
            w11 += picW;
        }
    }

    private static void getChromaXXUnsafe(int[] pels, int picW, int picH, int[] blk, int blkOff, int blkStride,
            int fullX, int fullY, int fracX, int fracY, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        int eMx = 8 - fracX;
        int eMy = 8 - fracY;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int w00 = iClip3(0, maxH, fullY + j) * picW + iClip3(0, maxW, fullX + i);
                int w01 = iClip3(0, maxH, fullY + j + 1) * picW + iClip3(0, maxW, fullX + i);
                int w10 = iClip3(0, maxH, fullY + j) * picW + iClip3(0, maxW, fullX + i + 1);
                int w11 = iClip3(0, maxH, fullY + j + 1) * picW + iClip3(0, maxW, fullX + i + 1);

                blk[blkOff + i] = (eMx * eMy * pels[w00] + fracX * eMy * pels[w10] + eMx * fracY * pels[w01] + fracX
                        * fracY * pels[w11] + 32) >> 6;
            }
            blkOff += blkStride;
        }
    }

    private static interface LumaInterpolator {
        void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
                int blkH);
    }

    private static LumaInterpolator[] safe = new LumaInterpolator[] {

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma00(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma10(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma20(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma30(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma01(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma11(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma21(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma31(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma02(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma12(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma22(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma32(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma03(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma13(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma23(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma33(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    } };

    private static LumaInterpolator[] unsafe = new LumaInterpolator[] {

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma00Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma10Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma20Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma30Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma01Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma11Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma21Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma31Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma02Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma12Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma22Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma32Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma03Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma13Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma23Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(int[] pels, int picW, int imgH, int[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma33Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    } };
}
