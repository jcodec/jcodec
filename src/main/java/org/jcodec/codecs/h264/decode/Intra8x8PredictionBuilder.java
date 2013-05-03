package org.jcodec.codecs.h264.decode;

import static org.jcodec.common.tools.MathUtil.clip;

import org.junit.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Builds intra prediction for intra 8x8 coded macroblocks
 * 
 * @author Jay Codec
 * 
 */
public class Intra8x8PredictionBuilder {

    static int[] topBuf = new int[16];
    static int[] leftBuf = new int[8];
    static int[] genBuf = new int[24];

    public static void predictWithMode(int mode, int[] residual, boolean leftAvailable, boolean topAvailable,
            boolean topLeftAvailable, boolean topRightAvailable, int[] leftRow, int[] topLine, int topLeft[],
            int mbOffX, int blkX, int blkY) {
        switch (mode) {
        case 0:
            Assert.assertTrue(topAvailable);
            predictVertical(residual, topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX, blkX, blkY);
            break;
        case 1:
            Assert.assertTrue(leftAvailable);
            predictHorizontal(residual, topLeftAvailable, topLeft, leftRow, mbOffX, blkX, blkY);
            break;
        case 2:
            predictDC(residual, topLeftAvailable, topRightAvailable, leftAvailable, topAvailable, topLeft, leftRow,
                    topLine, mbOffX, blkX, blkY);
            break;
        case 3:
            Assert.assertTrue(topAvailable);
            predictDiagonalDownLeft(residual, topLeftAvailable, topAvailable, topRightAvailable, topLeft, topLine,
                    mbOffX, blkX, blkY);
            break;
        case 4:
            Assert.assertTrue(topAvailable && leftAvailable && topLeftAvailable);
            predictDiagonalDownRight(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY);
            break;
        case 5:
            Assert.assertTrue(topAvailable && leftAvailable && topLeftAvailable);
            predictVerticalRight(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY);
            break;
        case 6:
            Assert.assertTrue(topAvailable && leftAvailable && topLeftAvailable);
            predictHorizontalDown(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY);
            break;
        case 7:
            Assert.assertTrue(topAvailable);
            predictVerticalLeft(residual, topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX, blkX, blkY);
            break;
        case 8:
            Assert.assertTrue(leftAvailable);
            predictHorizontalUp(residual, topLeftAvailable, topLeft, leftRow, mbOffX, blkX, blkY);
            break;
        }

        int oo1 = mbOffX + blkX;
        int off1 = (blkY << 4) + blkX + 7;

        topLeft[blkY >> 2] = topLine[oo1 + 7];

        for (int i = 0; i < 8; i++)
            leftRow[blkY + i] = residual[off1 + (i << 4)];

        int off2 = (blkY << 4) + blkX + 112;
        for (int i = 0; i < 8; i++)
            topLine[oo1 + i] = residual[off2 + i];
        
        topLeft[(blkY >> 2) + 1] = leftRow[blkY + 3];
    }

    private static void interpolateTop(boolean topLeftAvailable, boolean topRightAvailable, int[] topLeft,
            int[] topLine, int blkX, int blkY, int[] out) {
        int a = topLeftAvailable ? topLeft[blkY >> 2] : topLine[blkX];

        out[0] = (a + (topLine[blkX] << 1) + topLine[blkX + 1] + 2) >> 2;
        int i;
        for (i = 1; i < 7; i++)
            out[i] = (topLine[blkX + i - 1] + (topLine[blkX + i] << 1) + topLine[blkX + i + 1] + 2) >> 2;

        if (topRightAvailable) {
            for (; i < 15; i++)
                out[i] = (topLine[blkX + i - 1] + (topLine[blkX + i] << 1) + topLine[blkX + i + 1] + 2) >> 2;
            out[15] = (topLine[blkX + 14] + (topLine[blkX + 15] << 1) + topLine[blkX + 15] + 2) >> 2;
        } else {
            out[7] = (topLine[blkX + 6] + (topLine[blkX + 7] << 1) + topLine[blkX + 7] + 2) >> 2;
            for (i = 8; i < 16; i++)
                out[i] = topLine[blkX + 7];
        }
    }

    private static void interpolateLeft(boolean topLeftAvailable, int[] topLeft, int[] leftRow, int blkY, int[] out) {
        int a = topLeftAvailable ? topLeft[blkY >> 2] : leftRow[0];

        out[0] = (a + (leftRow[blkY] << 1) + leftRow[blkY + 1] + 2) >> 2;
        for (int i = 1; i < 7; i++)
            out[i] = (leftRow[blkY + i - 1] + (leftRow[blkY + i] << 1) + leftRow[blkY + i + 1] + 2) >> 2;

        out[7] = (leftRow[blkY + 6] + (leftRow[blkY + 7] << 1) + leftRow[blkY + 7] + 2) >> 2;
    }

    private static int interpolateTopLeft(boolean topAvailable, boolean leftAvailable, int[] topLeft, int[] topLine,
            int[] leftRow, int mbOffX, int blkX, int blkY) {
        int a = topLeft[blkY >> 2];
        int b = topAvailable ? topLine[mbOffX + blkX] : a;
        int c = leftAvailable ? leftRow[blkY] : a;
        int aa = a << 1;

        return (aa + b + c + 2) >> 2;
    }

    public static void copyAdd(int[] src, int srcOff, int[] dst, int dstOff) {
        dst[dstOff] = clip(dst[dstOff] + src[srcOff], 0, 255);
        dst[dstOff + 1] = clip(dst[dstOff + 1] + src[srcOff + 1], 0, 255);
        dst[dstOff + 2] = clip(dst[dstOff + 2] + src[srcOff + 2], 0, 255);
        dst[dstOff + 3] = clip(dst[dstOff + 3] + src[srcOff + 3], 0, 255);
        dst[dstOff + 4] = clip(dst[dstOff + 4] + src[srcOff + 4], 0, 255);
        dst[dstOff + 5] = clip(dst[dstOff + 5] + src[srcOff + 5], 0, 255);
        dst[dstOff + 6] = clip(dst[dstOff + 6] + src[srcOff + 6], 0, 255);
        dst[dstOff + 7] = clip(dst[dstOff + 7] + src[srcOff + 7], 0, 255);
    }

    public static void fillAdd(int[] dst, int off, int val) {
        for (int i = 0; i < 8; i++, off += 16) {
            dst[off] = clip(dst[off] + val, 0, 255);
            dst[off + 1] = clip(dst[off + 1] + val, 0, 255);
            dst[off + 2] = clip(dst[off + 2] + val, 0, 255);
            dst[off + 3] = clip(dst[off + 3] + val, 0, 255);
            dst[off + 4] = clip(dst[off + 4] + val, 0, 255);
            dst[off + 5] = clip(dst[off + 5] + val, 0, 255);
            dst[off + 6] = clip(dst[off + 6] + val, 0, 255);
            dst[off + 7] = clip(dst[off + 7] + val, 0, 255);
        }
    }

    private static void predictVertical(int[] residual, boolean topLeftAvailable, boolean topRightAvailable,
            int[] topLeft, int[] topLine, int mbOffX, int blkX, int blkY) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
        for (int i = 0, off = (blkY << 4) + blkX; i < 8; i++, off += 16) {
            residual[off] = clip(residual[off] + topBuf[0], 0, 255);
            residual[off + 1] = clip(residual[off + 1] + topBuf[1], 0, 255);
            residual[off + 2] = clip(residual[off + 2] + topBuf[2], 0, 255);
            residual[off + 3] = clip(residual[off + 3] + topBuf[3], 0, 255);
            residual[off + 4] = clip(residual[off + 4] + topBuf[4], 0, 255);
            residual[off + 5] = clip(residual[off + 5] + topBuf[5], 0, 255);
            residual[off + 6] = clip(residual[off + 6] + topBuf[6], 0, 255);
            residual[off + 7] = clip(residual[off + 7] + topBuf[7], 0, 255);
        }
    }

    private static void predictHorizontal(int[] residual, boolean topLeftAvailable, int[] topLeft, int[] leftRow,
            int mbOffX, int blkX, int blkY) {
        interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf);
        for (int i = 0, off = (blkY << 4) + blkX; i < 8; i++, off += 16) {
            residual[off] = clip(residual[off] + leftBuf[i], 0, 255);
            residual[off + 1] = clip(residual[off + 1] + leftBuf[i], 0, 255);
            residual[off + 2] = clip(residual[off + 2] + leftBuf[i], 0, 255);
            residual[off + 3] = clip(residual[off + 3] + leftBuf[i], 0, 255);
            residual[off + 4] = clip(residual[off + 4] + leftBuf[i], 0, 255);
            residual[off + 5] = clip(residual[off + 5] + leftBuf[i], 0, 255);
            residual[off + 6] = clip(residual[off + 6] + leftBuf[i], 0, 255);
            residual[off + 7] = clip(residual[off + 7] + leftBuf[i], 0, 255);
        }
    }

    private static void predictDC(int[] residual, boolean topLeftAvailable, boolean topRightAvailable,
            boolean leftAvailable, boolean topAvailable, int topLeft[], int[] leftRow, int[] topLine, int mbOffX,
            int blkX, int blkY) {
        if (topAvailable && leftAvailable) {
            interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
            interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf);
            int sum1 = topBuf[0] + topBuf[1] + topBuf[2] + topBuf[3];
            int sum2 = topBuf[4] + topBuf[5] + topBuf[6] + topBuf[7];
            int sum3 = leftBuf[0] + leftBuf[1] + leftBuf[2] + leftBuf[3];
            int sum4 = leftBuf[4] + leftBuf[5] + leftBuf[6] + leftBuf[7];
            fillAdd(residual, (blkY << 4) + blkX, (sum1 + sum2 + sum3 + sum4 + 8) >> 4);
        } else if (leftAvailable) {
            interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf);
            int sum3 = leftBuf[0] + leftBuf[1] + leftBuf[2] + leftBuf[3];
            int sum4 = leftBuf[4] + leftBuf[5] + leftBuf[6] + leftBuf[7];
            fillAdd(residual, (blkY << 4) + blkX, (sum3 + sum4 + 4) >> 3);
        } else if (topAvailable) {
            interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
            int sum1 = topBuf[0] + topBuf[1] + topBuf[2] + topBuf[3];
            int sum2 = topBuf[4] + topBuf[5] + topBuf[6] + topBuf[7];
            fillAdd(residual, (blkY << 4) + blkX, (sum1 + sum2 + 4) >> 3);
        } else {
            fillAdd(residual, (blkY << 4) + blkX, 128);
        }
    }

    private static void predictDiagonalDownLeft(int[] residual, boolean topLeftAvailable, boolean topAvailable,
            boolean topRightAvailable, int[] topLeft, int[] topLine, int mbOffX, int blkX, int blkY) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);

        genBuf[0] = ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[1] = ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[2] = ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[3] = ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[4] = ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);
        genBuf[5] = ((topBuf[5] + topBuf[7] + ((topBuf[6]) << 1) + 2) >> 2);
        genBuf[6] = ((topBuf[6] + topBuf[8] + ((topBuf[7]) << 1) + 2) >> 2);
        genBuf[7] = ((topBuf[7] + topBuf[9] + ((topBuf[8]) << 1) + 2) >> 2);
        genBuf[8] = ((topBuf[8] + topBuf[10] + ((topBuf[9]) << 1) + 2) >> 2);
        genBuf[9] = ((topBuf[9] + topBuf[11] + ((topBuf[10]) << 1) + 2) >> 2);
        genBuf[10] = ((topBuf[10] + topBuf[12] + ((topBuf[11]) << 1) + 2) >> 2);
        genBuf[11] = ((topBuf[11] + topBuf[13] + ((topBuf[12]) << 1) + 2) >> 2);
        genBuf[12] = ((topBuf[12] + topBuf[14] + ((topBuf[13]) << 1) + 2) >> 2);
        genBuf[13] = ((topBuf[13] + topBuf[15] + ((topBuf[14]) << 1) + 2) >> 2);
        genBuf[14] = ((topBuf[14] + topBuf[15] + ((topBuf[15]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 0, residual, off);
        copyAdd(genBuf, 1, residual, off + 16);
        copyAdd(genBuf, 2, residual, off + 32);
        copyAdd(genBuf, 3, residual, off + 48);
        copyAdd(genBuf, 4, residual, off + 64);
        copyAdd(genBuf, 5, residual, off + 80);
        copyAdd(genBuf, 6, residual, off + 96);
        copyAdd(genBuf, 7, residual, off + 112);
    }

    private static void predictDiagonalDownRight(int[] residual, boolean topRightAvailable, int[] topLeft,
            int[] leftRow, int[] topLine, int mbOffX, int blkX, int blkY) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf);
        int tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY);

        genBuf[0] = ((leftBuf[7] + leftBuf[5] + ((leftBuf[6]) << 1) + 2) >> 2);
        genBuf[1] = ((leftBuf[6] + leftBuf[4] + ((leftBuf[5]) << 1) + 2) >> 2);
        genBuf[2] = ((leftBuf[5] + leftBuf[3] + ((leftBuf[4]) << 1) + 2) >> 2);
        genBuf[3] = ((leftBuf[4] + leftBuf[2] + ((leftBuf[3]) << 1) + 2) >> 2);
        genBuf[4] = ((leftBuf[3] + leftBuf[1] + ((leftBuf[2]) << 1) + 2) >> 2);
        genBuf[5] = ((leftBuf[2] + leftBuf[0] + ((leftBuf[1]) << 1) + 2) >> 2);
        genBuf[6] = ((leftBuf[1] + tl + ((leftBuf[0]) << 1) + 2) >> 2);
        genBuf[7] = ((leftBuf[0] + topBuf[0] + ((tl) << 1) + 2) >> 2);
        genBuf[8] = ((tl + topBuf[1] + ((topBuf[0]) << 1) + 2) >> 2);
        genBuf[9] = ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[10] = ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[11] = ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[12] = ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[13] = ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);
        genBuf[14] = ((topBuf[5] + topBuf[7] + ((topBuf[6]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 7, residual, off);
        copyAdd(genBuf, 6, residual, off + 16);
        copyAdd(genBuf, 5, residual, off + 32);
        copyAdd(genBuf, 4, residual, off + 48);
        copyAdd(genBuf, 3, residual, off + 64);
        copyAdd(genBuf, 2, residual, off + 80);
        copyAdd(genBuf, 1, residual, off + 96);
        copyAdd(genBuf, 0, residual, off + 112);
    }

    private static void predictVerticalRight(int[] residual, boolean topRightAvailable, int[] topLeft, int[] leftRow,
            int[] topLine, int mbOffX, int blkX, int blkY) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf);
        int tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY);

        genBuf[0] = ((leftBuf[5] + leftBuf[3] + ((leftBuf[4]) << 1) + 2) >> 2);
        genBuf[1] = ((leftBuf[3] + leftBuf[1] + ((leftBuf[2]) << 1) + 2) >> 2);
        genBuf[2] = ((leftBuf[1] + tl + ((leftBuf[0]) << 1) + 2) >> 2);
        genBuf[3] = ((tl + topBuf[0] + 1) >> 1);
        genBuf[4] = ((topBuf[0] + topBuf[1] + 1) >> 1);
        genBuf[5] = ((topBuf[1] + topBuf[2] + 1) >> 1);
        genBuf[6] = ((topBuf[2] + topBuf[3] + 1) >> 1);
        genBuf[7] = ((topBuf[3] + topBuf[4] + 1) >> 1);
        genBuf[8] = ((topBuf[4] + topBuf[5] + 1) >> 1);
        genBuf[9] = ((topBuf[5] + topBuf[6] + 1) >> 1);
        genBuf[10] = ((topBuf[6] + topBuf[7] + 1) >> 1);
        genBuf[11] = ((leftBuf[6] + leftBuf[4] + ((leftBuf[5]) << 1) + 2) >> 2);
        genBuf[12] = ((leftBuf[4] + leftBuf[2] + ((leftBuf[3]) << 1) + 2) >> 2);
        genBuf[13] = ((leftBuf[2] + leftBuf[0] + ((leftBuf[1]) << 1) + 2) >> 2);
        genBuf[14] = ((leftBuf[0] + topBuf[0] + ((tl) << 1) + 2) >> 2);
        genBuf[15] = ((tl + topBuf[1] + ((topBuf[0]) << 1) + 2) >> 2);
        genBuf[16] = ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[17] = ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[18] = ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[19] = ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[20] = ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);
        genBuf[21] = ((topBuf[5] + topBuf[7] + ((topBuf[6]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 3, residual, off);
        copyAdd(genBuf, 14, residual, off + 16);
        copyAdd(genBuf, 2, residual, off + 32);
        copyAdd(genBuf, 13, residual, off + 48);
        copyAdd(genBuf, 1, residual, off + 64);
        copyAdd(genBuf, 12, residual, off + 80);
        copyAdd(genBuf, 0, residual, off + 96);
        copyAdd(genBuf, 11, residual, off + 112);
    }

    private static void predictHorizontalDown(int[] residual, boolean topRightAvailable, int[] topLeft, int[] leftRow,
            int[] topLine, int mbOffX, int blkX, int blkY) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf);
        int tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY);

        genBuf[0] = ((leftBuf[7] + leftBuf[6] + 1) >> 1);
        genBuf[1] = ((leftBuf[5] + leftBuf[7] + (leftBuf[6] << 1) + 2) >> 2);
        genBuf[2] = ((leftBuf[6] + leftBuf[5] + 1) >> 1);
        genBuf[3] = ((leftBuf[4] + leftBuf[6] + ((leftBuf[5]) << 1) + 2) >> 2);
        genBuf[4] = ((leftBuf[5] + leftBuf[4] + 1) >> 1);
        genBuf[5] = ((leftBuf[3] + leftBuf[5] + ((leftBuf[4]) << 1) + 2) >> 2);
        genBuf[6] = ((leftBuf[4] + leftBuf[3] + 1) >> 1);
        genBuf[7] = ((leftBuf[2] + leftBuf[4] + ((leftBuf[3]) << 1) + 2) >> 2);
        genBuf[8] = ((leftBuf[3] + leftBuf[2] + 1) >> 1);
        genBuf[9] = ((leftBuf[1] + leftBuf[3] + ((leftBuf[2]) << 1) + 2) >> 2);
        genBuf[10] = ((leftBuf[2] + leftBuf[1] + 1) >> 1);
        genBuf[11] = ((leftBuf[0] + leftBuf[2] + ((leftBuf[1]) << 1) + 2) >> 2);
        genBuf[12] = ((leftBuf[1] + leftBuf[0] + 1) >> 1);
        genBuf[13] = ((tl + leftBuf[1] + ((leftBuf[0]) << 1) + 2) >> 2);
        genBuf[14] = ((leftBuf[0] + tl + 1) >> 1);
        genBuf[15] = ((leftBuf[0] + topBuf[0] + ((tl) << 1) + 2) >> 2);
        genBuf[16] = ((tl + topBuf[1] + ((topBuf[0]) << 1) + 2) >> 2);
        genBuf[17] = ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[18] = ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[19] = ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[20] = ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[21] = ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 14, residual, off);
        copyAdd(genBuf, 12, residual, off + 16);
        copyAdd(genBuf, 10, residual, off + 32);
        copyAdd(genBuf, 8, residual, off + 48);
        copyAdd(genBuf, 6, residual, off + 64);
        copyAdd(genBuf, 4, residual, off + 80);
        copyAdd(genBuf, 2, residual, off + 96);
        copyAdd(genBuf, 0, residual, off + 112);
    }

    private static void predictVerticalLeft(int[] residual, boolean topLeftAvailable, boolean topRightAvailable,
            int[] topLeft, int[] topLine, int mbOffX, int blkX, int blkY) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);

        genBuf[0] = ((topBuf[0] + topBuf[1] + 1) >> 1);
        genBuf[1] = ((topBuf[1] + topBuf[2] + 1) >> 1);
        genBuf[2] = ((topBuf[2] + topBuf[3] + 1) >> 1);
        genBuf[3] = ((topBuf[3] + topBuf[4] + 1) >> 1);
        genBuf[4] = ((topBuf[4] + topBuf[5] + 1) >> 1);
        genBuf[5] = ((topBuf[5] + topBuf[6] + 1) >> 1);
        genBuf[6] = ((topBuf[6] + topBuf[7] + 1) >> 1);
        genBuf[7] = ((topBuf[7] + topBuf[8] + 1) >> 1);
        genBuf[8] = ((topBuf[8] + topBuf[9] + 1) >> 1);
        genBuf[9] = ((topBuf[9] + topBuf[10] + 1) >> 1);
        genBuf[10] = ((topBuf[10] + topBuf[11] + 1) >> 1);
        genBuf[11] = ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[12] = ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[13] = ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[14] = ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[15] = ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);
        genBuf[16] = ((topBuf[5] + topBuf[7] + ((topBuf[6]) << 1) + 2) >> 2);
        genBuf[17] = ((topBuf[6] + topBuf[8] + ((topBuf[7]) << 1) + 2) >> 2);
        genBuf[18] = ((topBuf[7] + topBuf[9] + ((topBuf[8]) << 1) + 2) >> 2);
        genBuf[19] = ((topBuf[8] + topBuf[10] + ((topBuf[9]) << 1) + 2) >> 2);
        genBuf[20] = ((topBuf[9] + topBuf[11] + ((topBuf[10]) << 1) + 2) >> 2);
        genBuf[21] = ((topBuf[10] + topBuf[12] + ((topBuf[11]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 0, residual, off);
        copyAdd(genBuf, 11, residual, off + 16);
        copyAdd(genBuf, 1, residual, off + 32);
        copyAdd(genBuf, 12, residual, off + 48);
        copyAdd(genBuf, 2, residual, off + 64);
        copyAdd(genBuf, 13, residual, off + 80);
        copyAdd(genBuf, 3, residual, off + 96);
        copyAdd(genBuf, 14, residual, off + 112);
    }

    private static void predictHorizontalUp(int[] residual, boolean topLeftAvailable, int[] topLeft, int[] leftRow,
            int mbOffX, int blkX, int blkY) {
        interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf);

        genBuf[0] = ((leftBuf[0] + leftBuf[1] + 1) >> 1);
        genBuf[1] = ((leftBuf[2] + leftBuf[0] + ((leftBuf[1]) << 1) + 2) >> 2);
        genBuf[2] = ((leftBuf[1] + leftBuf[2] + 1) >> 1);
        genBuf[3] = ((leftBuf[3] + leftBuf[1] + ((leftBuf[2]) << 1) + 2) >> 2);
        genBuf[4] = ((leftBuf[2] + leftBuf[3] + 1) >> 1);
        genBuf[5] = ((leftBuf[4] + leftBuf[2] + ((leftBuf[3]) << 1) + 2) >> 2);
        genBuf[6] = ((leftBuf[3] + leftBuf[4] + 1) >> 1);
        genBuf[7] = ((leftBuf[5] + leftBuf[3] + ((leftBuf[4]) << 1) + 2) >> 2);
        genBuf[8] = ((leftBuf[4] + leftBuf[5] + 1) >> 1);
        genBuf[9] = ((leftBuf[6] + leftBuf[4] + ((leftBuf[5]) << 1) + 2) >> 2);
        genBuf[10] = ((leftBuf[5] + leftBuf[6] + 1) >> 1);
        genBuf[11] = ((leftBuf[7] + leftBuf[5] + ((leftBuf[6]) << 1) + 2) >> 2);
        genBuf[12] = ((leftBuf[6] + leftBuf[7] + 1) >> 1);
        genBuf[13] = ((leftBuf[6] + leftBuf[7] + ((leftBuf[7]) << 1) + 2) >> 2);
        genBuf[14] = genBuf[15] = genBuf[16] = genBuf[17] = genBuf[18] = genBuf[19] = genBuf[20] = genBuf[21] = leftBuf[7];

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 0, residual, off);
        copyAdd(genBuf, 2, residual, off + 16);
        copyAdd(genBuf, 4, residual, off + 32);
        copyAdd(genBuf, 6, residual, off + 48);
        copyAdd(genBuf, 8, residual, off + 64);
        copyAdd(genBuf, 10, residual, off + 80);
        copyAdd(genBuf, 12, residual, off + 96);
        copyAdd(genBuf, 14, residual, off + 112);
    }
}