package org.jcodec.codecs.h264.decode;

import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Interpolator that operates on block level
 * 
 * @author The JCodec project
 * 
 */
public class BlockInterpolator {

    private int[] tmp1 = new int[1024];
    private int[] tmp2 = new int[1024];
    private byte[] tmp3 = new byte[1024];

    /**
     * Get block of ( possibly interpolated ) luma pixels
     */
    public void getBlockLuma(Picture8Bit pic, Picture8Bit out, int off, int x, int y, int w, int h) {
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

    public void getBlockChroma(byte[] pels, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
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
    private void getLuma00(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {

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
    private void getLuma00Unsafe(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        for (int j = 0; j < blkH; j++) {
            int lineStart = clip(j + y, 0, maxH) * picW;

            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = pic[lineStart + clip(x + i, 0, maxW)];
            }
            blkOff += blkStride;
        }
    }

    /**
     * Halfpel (2,0) horizontal, int argument version
     */
    private void getLuma20NoRound(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            int off1 = -2;
            for (int i = 0; i < blkW; i++) {
                int a = pic[off + off1] + pic[off + off1 + 5];
                int b = pic[off + off1 + 1] + pic[off + off1 + 4];
                int c = pic[off + off1 + 2] + pic[off + off1 + 3];
                blk[blkOff + i] = a + 5 * ((c << 2) - b);
                ++off1;
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Halfpel (2,0) horizontal
     */
    private void getLuma20NoRound(byte[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            int off1 = -2;
            for (int i = 0; i < blkW; i++) {
                int a = pic[off + off1] + pic[off + off1 + 5];
                int b = pic[off + off1 + 1] + pic[off + off1 + 4];
                int c = pic[off + off1 + 2] + pic[off + off1 + 3];
                blk[blkOff + i] = a + 5 * ((c << 2) - b);
                ++off1;
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    private void getLuma20(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {
        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            int off1 = -2;
            for (int i = 0; i < blkW; i++) {
                int a = pic[off + off1] + pic[off + off1 + 5];
                int b = pic[off + off1 + 1] + pic[off + off1 + 4];
                int c = pic[off + off1 + 2] + pic[off + off1 + 3];
                blk[blkOff + i] = (byte) clip((a + 5 * ((c << 2) - b) + 16) >> 5, -128, 127);
                ++off1;
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Halfpel (2, 0) horizontal unsafe
     */
    private void getLuma20UnsafeNoRound(byte[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int maxW = picW - 1;
        int maxH = picH - 1;

        for (int i = 0; i < blkW; i++) {
            int ipos_m2 = clip(x + i - 2, 0, maxW);
            int ipos_m1 = clip(x + i - 1, 0, maxW);
            int ipos = clip(x + i, 0, maxW);
            int ipos_p1 = clip(x + i + 1, 0, maxW);
            int ipos_p2 = clip(x + i + 2, 0, maxW);
            int ipos_p3 = clip(x + i + 3, 0, maxW);

            int boff = blkOff;
            for (int j = 0; j < blkH; j++) {
                int lineStart = clip(j + y, 0, maxH) * picW;

                int a = pic[lineStart + ipos_m2] + pic[lineStart + ipos_p3];
                int b = pic[lineStart + ipos_m1] + pic[lineStart + ipos_p2];
                int c = pic[lineStart + ipos] + pic[lineStart + ipos_p1];

                blk[boff + i] = a + 5 * ((c << 2) - b);

                boff += blkStride;
            }
        }
    }

    private void getLuma20Unsafe(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {

        getLuma20UnsafeNoRound(pic, picW, picH, tmp1, blkOff, blkStride, x, y, blkW, blkH);

        for (int i = 0; i < blkW; i++) {
            int boff = blkOff;
            for (int j = 0; j < blkH; j++) {
                blk[boff + i] = (byte) clip((tmp1[boff + i] + 16) >> 5, -128, 127);
                boff += blkStride;
            }
        }
    }

    /**
     * Halfpel (0, 2) vertical
     */
    private void getLuma02NoRound(int[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        int off = (y - 2) * picW + x, picWx2 = picW + picW, picWx3 = picWx2 + picW, picWx4 = picWx3 + picW, picWx5 = picWx4
                + picW;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int a = pic[off + i] + pic[off + i + picWx5];
                int b = pic[off + i + picW] + pic[off + i + picWx4];
                int c = pic[off + i + picWx2] + pic[off + i + picWx3];

                blk[blkOff + i] = a + 5 * ((c << 2) - b);
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Halfpel (0, 2) vertical
     */
    private void getLuma02NoRound(byte[] pic, int picW, int[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        int off = (y - 2) * picW + x, picWx2 = picW + picW, picWx3 = picWx2 + picW, picWx4 = picWx3 + picW, picWx5 = picWx4
                + picW;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int a = pic[off + i] + pic[off + i + picWx5];
                int b = pic[off + i + picW] + pic[off + i + picWx4];
                int c = pic[off + i + picWx2] + pic[off + i + picWx3];

                blk[blkOff + i] = a + 5 * ((c << 2) - b);
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    private void getLuma02(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {

        int off = (y - 2) * picW + x, picWx2 = picW + picW, picWx3 = picWx2 + picW, picWx4 = picWx3 + picW, picWx5 = picWx4
                + picW;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int a = pic[off + i] + pic[off + i + picWx5];
                int b = pic[off + i + picW] + pic[off + i + picWx4];
                int c = pic[off + i + picWx2] + pic[off + i + picWx3];
                blk[blkOff + i] = (byte) clip((a + 5 * ((c << 2) - b) + 16) >> 5, -128, 127);
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Hpel (0, 2) vertical unsafe
     */
    private void getLuma02UnsafeNoRound(byte[] pic, int picW, int picH, int[] blk, int blkOff, int blkStride, int x,
            int y, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        for (int j = 0; j < blkH; j++) {
            int offP0 = clip(y + j - 2, 0, maxH) * picW;
            int offP1 = clip(y + j - 1, 0, maxH) * picW;
            int offP2 = clip(y + j, 0, maxH) * picW;
            int offP3 = clip(y + j + 1, 0, maxH) * picW;
            int offP4 = clip(y + j + 2, 0, maxH) * picW;
            int offP5 = clip(y + j + 3, 0, maxH) * picW;

            for (int i = 0; i < blkW; i++) {
                int pres_x = clip(x + i, 0, maxW);

                int a = pic[pres_x + offP0] + pic[pres_x + offP5];
                int b = pic[pres_x + offP1] + pic[pres_x + offP4];
                int c = pic[pres_x + offP2] + pic[pres_x + offP3];

                blk[blkOff + i] = a + 5 * ((c << 2) - b);
            }
            blkOff += blkStride;
        }
    }

    private void getLuma02Unsafe(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {

        getLuma02UnsafeNoRound(pic, picW, picH, tmp1, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) clip((tmp1[blkOff + i] + 16) >> 5, -128, 127);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Qpel: (1,0) horizontal
     */
    private void getLuma10(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {

        getLuma20(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {

            for (int i = 0; i < blkW; i++) {

                blk[blkOff + i] = (byte) ((blk[blkOff + i] + pic[off + i] + 1) >> 1);
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Qpel: (1,0) horizontal unsafe
     */
    private void getLuma10Unsafe(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        getLuma20Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            int lineStart = clip(j + y, 0, maxH) * picW;

            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) ((blk[blkOff + i] + pic[lineStart + clip(x + i, 0, maxW)] + 1) >> 1);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Qpel (3,0) horizontal
     */
    private void getLuma30(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {

        getLuma20(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) ((pic[off + i + 1] + blk[blkOff + i] + 1) >> 1);
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Qpel horizontal (3, 0) unsafe
     */
    private void getLuma30Unsafe(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        getLuma20Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            int lineStart = clip(j + y, 0, maxH) * picW;

            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) ((blk[blkOff + i] + pic[lineStart + clip(x + i + 1, 0, maxW)] + 1) >> 1);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Qpel vertical (0, 1)
     */
    private void getLuma01(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {

        getLuma02(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) ((blk[blkOff + i] + pic[off + i] + 1) >> 1);
            }
            off += picW;
            blkOff += blkStride;
        }
    }

    /**
     * Qpel vertical (0, 1) unsafe
     */
    private void getLuma01Unsafe(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        getLuma02Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            int lineStart = clip(y + j, 0, maxH) * picW;
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) ((blk[blkOff + i] + pic[lineStart + clip(x + i, 0, maxW)] + 1) >> 1);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Qpel vertical (0, 3)
     */
    private void getLuma03(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {

        getLuma02(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);

        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) ((blk[blkOff + i] + pic[off + i + picW] + 1) >> 1);
            }
            off += picW;
            blkOff += blkStride;

        }
    }

    /**
     * Qpel vertical (0, 3) unsafe
     */
    private void getLuma03Unsafe(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        getLuma02Unsafe(pic, picW, picH, blk, blkOff, blkStride, x, y, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            int lineStart = clip(y + j + 1, 0, maxH) * picW;
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) ((blk[blkOff + i] + pic[lineStart + clip(x + i, 0, maxW)] + 1) >> 1);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Hpel horizontal, Qpel vertical (2, 1)
     * 
     */
    private void getLuma21(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH);

        int off = blkW << 1;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
                int rounded2 = clip((tmp1[off + i] + 16) >> 5, -128, 127);
                blk[blkOff + i] = (byte) ((rounded + rounded2 + 1) >> 1);
            }
            blkOff += blkStride;
            off += blkW;
        }
    }

    /**
     * Qpel vertical (2, 1) unsafe
     */
    private void getLuma21Unsafe(byte[] pic, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH);

        int off = blkW << 1;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
                int rounded2 = clip((tmp1[off + i] + 16) >> 5, -128, 127);
                blk[blkOff + i] = (byte) ((rounded + rounded2 + 1) >> 1);
            }
            blkOff += blkStride;
            off += blkW;
        }
    }

    /**
     * Hpel horizontal, Hpel vertical (2, 2)
     */
    private void getLuma22(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) (clip((tmp2[blkOff + i] + 512) >> 10, -128, 127));
            }
            blkOff += blkStride;
        }
    }

    /**
     * Hpel (2, 2) unsafe
     */
    private void getLuma22Unsafe(byte[] pic, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH);

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Hpel horizontal, Qpel vertical (2, 3)
     * 
     */
    private void getLuma23(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {
        getLuma20NoRound(pic, picW, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH);

        int off = blkW << 1;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
                int rounded2 = clip((tmp1[off + i + blkW] + 16) >> 5, -128, 127);
                blk[blkOff + i] = (byte) ((rounded + rounded2 + 1) >> 1);
            }
            blkOff += blkStride;
            off += blkW;
        }
    }

    /**
     * Qpel (2, 3) unsafe
     */
    private void getLuma23Unsafe(byte[] pic, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        getLuma20UnsafeNoRound(pic, picW, imgH, tmp1, 0, blkW, x, y - 2, blkW, blkH + 7);
        getLuma02NoRound(tmp1, blkW, tmp2, blkOff, blkStride, 0, 2, blkW, blkH);

        int off = blkW << 1;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
                int rounded2 = clip((tmp1[off + i + blkW] + 16) >> 5, -128, 127);
                blk[blkOff + i] = (byte) ((rounded + rounded2 + 1) >> 1);
            }
            blkOff += blkStride;
            off += blkW;
        }
    }

    /**
     * Qpel horizontal, Hpel vertical (1, 2)
     */
    private void getLuma12(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {

        int tmpW = blkW + 7;

        getLuma02NoRound(pic, picW, tmp1, 0, tmpW, x - 2, y, tmpW, blkH);
        getLuma20NoRound(tmp1, tmpW, tmp2, blkOff, blkStride, 2, 0, blkW, blkH);

        int off = 2;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
                int rounded2 = clip((tmp1[off + i] + 16) >> 5, -128, 127);
                blk[blkOff + i] = (byte) ((rounded + rounded2 + 1) >> 1);
            }
            blkOff += blkStride;
            off += tmpW;
        }
    }

    /**
     * Qpel (1, 2) unsafe
     */
    private void getLuma12Unsafe(byte[] pic, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int tmpW = blkW + 7;

        getLuma02UnsafeNoRound(pic, picW, imgH, tmp1, 0, tmpW, x - 2, y, tmpW, blkH);
        getLuma20NoRound(tmp1, tmpW, tmp2, blkOff, blkStride, 2, 0, blkW, blkH);

        int off = 2;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
                int rounded2 = clip((tmp1[off + i] + 16) >> 5, -128, 127);
                blk[blkOff + i] = (byte) ((rounded + rounded2 + 1) >> 1);
            }
            blkOff += blkStride;
            off += tmpW;
        }
    }

    /**
     * Qpel horizontal, Hpel vertical (3, 2)
     */
    private void getLuma32(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {
        int tmpW = blkW + 7;

        getLuma02NoRound(pic, picW, tmp1, 0, tmpW, x - 2, y, tmpW, blkH);
        getLuma20NoRound(tmp1, tmpW, tmp2, blkOff, blkStride, 2, 0, blkW, blkH);

        int off = 2;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
                int rounded2 = clip((tmp1[off + i + 1] + 16) >> 5, -128, 127);
                blk[blkOff + i] = (byte) ((rounded + rounded2 + 1) >> 1);
            }
            blkOff += blkStride;
            off += tmpW;
        }
    }

    /**
     * Qpel (3, 2) unsafe
     */
    private void getLuma32Unsafe(byte[] pic, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int tmpW = blkW + 7;

        getLuma02UnsafeNoRound(pic, picW, imgH, tmp1, 0, tmpW, x - 2, y, tmpW, blkH);
        getLuma20NoRound(tmp1, tmpW, tmp2, blkOff, blkStride, 2, 0, blkW, blkH);

        int off = 2;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int rounded = clip((tmp2[blkOff + i] + 512) >> 10, -128, 127);
                int rounded2 = clip((tmp1[off + i + 1] + 16) >> 5, -128, 127);
                blk[blkOff + i] = (byte) ((rounded + rounded2 + 1) >> 1);
            }
            blkOff += blkStride;
            off += tmpW;
        }
    }

    /**
     * Qpel horizontal, Qpel vertical (3, 3)
     */
    private void getLuma33(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y + 1, blkW, blkH);
        getLuma02(pic, picW, tmp3, 0, blkW, x + 1, y, blkW, blkH);

        merge(blk, tmp3, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel (3, 3) unsafe
     */
    private void getLuma33Unsafe(byte[] pic, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y + 1, blkW, blkH);
        getLuma02Unsafe(pic, picW, imgH, tmp3, 0, blkW, x + 1, y, blkW, blkH);

        merge(blk, tmp3, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel horizontal, Qpel vertical (1, 1)
     */
    private void getLuma11(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        getLuma02(pic, picW, tmp3, 0, blkW, x, y, blkW, blkH);

        merge(blk, tmp3, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel (1, 1) unsafe
     */
    private void getLuma11Unsafe(byte[] pic, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        getLuma02Unsafe(pic, picW, imgH, tmp3, 0, blkW, x, y, blkW, blkH);

        merge(blk, tmp3, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel horizontal, Qpel vertical (1, 3)
     */
    private void getLuma13(byte[] pic, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW, int blkH) {
        getLuma20(pic, picW, blk, blkOff, blkStride, x, y + 1, blkW, blkH);
        getLuma02(pic, picW, tmp3, 0, blkW, x, y, blkW, blkH);

        merge(blk, tmp3, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel (1, 3) unsafe
     */
    private void getLuma13Unsafe(byte[] pic, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        getLuma20Unsafe(pic, picW, imgH, blk, blkOff, blkStride, x, y + 1, blkW, blkH);
        getLuma02Unsafe(pic, picW, imgH, tmp3, 0, blkW, x, y, blkW, blkH);

        merge(blk, tmp3, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel horizontal, Qpel vertical (3, 1)
     */
    private void getLuma31(byte[] pels, int picW, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW,
            int blkH) {
        getLuma20(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        getLuma02(pels, picW, tmp3, 0, blkW, x + 1, y, blkW, blkH);

        merge(blk, tmp3, blkOff, blkStride, blkW, blkH);
    }

    /**
     * Qpel (3, 1) unsafe
     */
    private void getLuma31Unsafe(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        getLuma20Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        getLuma02Unsafe(pels, picW, imgH, tmp3, 0, blkW, x + 1, y, blkW, blkH);

        merge(blk, tmp3, blkOff, blkStride, blkW, blkH);
    }

    private void merge(byte[] first, byte[] second, int blkOff, int blkStride, int blkW, int blkH) {
        int tOff = 0;
        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                first[blkOff + i] = (byte) ((first[blkOff + i] + second[tOff + i] + 1) >> 1);
            }
            blkOff += blkStride;
            tOff += blkW;
        }
    }

    /**
     * Chroma (0,0)
     */
    private void getChroma00(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int off = y * picW + x;
        for (int j = 0; j < blkH; j++) {
            System.arraycopy(pic, off, blk, blkOff, blkW);
            off += picW;
            blkOff += blkStride;
        }
    }

    private void getChroma00Unsafe(byte[] pic, int picW, int picH, byte[] blk, int blkOff, int blkStride, int x, int y,
            int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        for (int j = 0; j < blkH; j++) {
            int lineStart = clip(j + y, 0, maxH) * picW;

            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = pic[lineStart + clip(x + i, 0, maxW)];
            }
            blkOff += blkStride;
        }
    }

    /**
     * Chroma (X,0)
     */
    private void getChroma0X(byte[] pels, int picW, int picH, byte[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracY, int blkW, int blkH) {
        int w00 = fullY * picW + fullX;
        int w01 = w00 + (fullY < picH - 1 ? picW : 0);
        int eMy = 8 - fracY;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {

                blk[blkOff + i] = (byte) ((eMy * pels[w00 + i] + fracY * pels[w01 + i] + 4) >> 3);
            }
            w00 += picW;
            w01 += picW;
            blkOff += blkStride;
        }
    }

    private void getChroma0XUnsafe(byte[] pels, int picW, int picH, byte[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracY, int blkW, int blkH) {

        int maxW = picW - 1;
        int maxH = picH - 1;
        int eMy = 8 - fracY;

        for (int j = 0; j < blkH; j++) {
            int off00 = clip(fullY + j, 0, maxH) * picW;
            int off01 = clip(fullY + j + 1, 0, maxH) * picW;

            for (int i = 0; i < blkW; i++) {
                int w00 = clip(fullX + i, 0, maxW) + off00;
                int w01 = clip(fullX + i, 0, maxW) + off01;

                blk[blkOff + i] = (byte) ((eMy * pels[w00] + fracY * pels[w01] + 4) >> 3);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Chroma (X,0)
     */
    private void getChromaX0(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracX, int blkW, int blkH) {
        int w00 = fullY * picW + fullX;
        int w10 = w00 + (fullX < picW - 1 ? 1 : 0);
        int eMx = 8 - fracX;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                blk[blkOff + i] = (byte) ((eMx * pels[w00 + i] + fracX * pels[w10 + i] + 4) >> 3);
            }
            w00 += picW;
            w10 += picW;
            blkOff += blkStride;
        }
    }

    private void getChromaX0Unsafe(byte[] pels, int picW, int picH, byte[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracX, int blkW, int blkH) {
        int eMx = 8 - fracX;
        int maxW = picW - 1;
        int maxH = picH - 1;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int w00 = clip(fullY + j, 0, maxH) * picW + clip(fullX + i, 0, maxW);
                int w10 = clip(fullY + j, 0, maxH) * picW + clip(fullX + i + 1, 0, maxW);

                blk[blkOff + i] = (byte) ((eMx * pels[w00] + fracX * pels[w10] + 4) >> 3);
            }
            blkOff += blkStride;
        }
    }

    /**
     * Chroma (X,X)
     */
    private void getChromaXX(byte[] pels, int picW, int picH, byte[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracX, int fracY, int blkW, int blkH) {
        int w00 = fullY * picW + fullX;
        int w01 = w00 + (fullY < picH - 1 ? picW : 0);
        int w10 = w00 + (fullX < picW - 1 ? 1 : 0);
        int w11 = w10 + w01 - w00;
        int eMx = 8 - fracX;
        int eMy = 8 - fracY;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {

                blk[blkOff + i] = (byte) ((eMx * eMy * pels[w00 + i] + fracX * eMy * pels[w10 + i] + eMx * fracY
                        * pels[w01 + i] + fracX * fracY * pels[w11 + i] + 32) >> 6);
            }
            blkOff += blkStride;
            w00 += picW;
            w01 += picW;
            w10 += picW;
            w11 += picW;
        }
    }

    private void getChromaXXUnsafe(byte[] pels, int picW, int picH, byte[] blk, int blkOff, int blkStride, int fullX,
            int fullY, int fracX, int fracY, int blkW, int blkH) {
        int maxH = picH - 1;
        int maxW = picW - 1;

        int eMx = 8 - fracX;
        int eMy = 8 - fracY;

        for (int j = 0; j < blkH; j++) {
            for (int i = 0; i < blkW; i++) {
                int w00 = clip(fullY + j, 0, maxH) * picW + clip(fullX + i, 0, maxW);
                int w01 = clip(fullY + j + 1, 0, maxH) * picW + clip(fullX + i, 0, maxW);
                int w10 = clip(fullY + j, 0, maxH) * picW + clip(fullX + i + 1, 0, maxW);
                int w11 = clip(fullY + j + 1, 0, maxH) * picW + clip(fullX + i + 1, 0, maxW);

                blk[blkOff + i] = (byte) ((eMx * eMy * pels[w00] + fracX * eMy * pels[w10] + eMx * fracY * pels[w01]
                        + fracX * fracY * pels[w11] + 32) >> 6);
            }
            blkOff += blkStride;
        }
    }

    private interface LumaInterpolator {
        void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y, int blkW,
                int blkH);
    }

    private LumaInterpolator[] safe = new LumaInterpolator[] {

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma00(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma10(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma20(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma30(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma01(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma11(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma21(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma31(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma02(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma12(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma22(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma32(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma03(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma13(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma23(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma33(pels, picW, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    } };

    private LumaInterpolator[] unsafe = new LumaInterpolator[] {

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma00Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma10Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma20Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma30Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma01Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma11Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma21Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma31Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma02Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma12Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma22Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma32Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma03Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma13Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    },

    new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma23Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    }, new LumaInterpolator() {
        public void getLuma(byte[] pels, int picW, int imgH, byte[] blk, int blkOff, int blkStride, int x, int y,
                int blkW, int blkH) {
            getLuma33Unsafe(pels, picW, imgH, blk, blkOff, blkStride, x, y, blkW, blkH);
        }
    } };
}
