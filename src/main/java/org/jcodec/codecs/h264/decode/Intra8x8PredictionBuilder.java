package org.jcodec.codecs.h264.decode;

import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.common.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Builds intra prediction for intra 8x8 coded macroblocks
 * 
 * @author The JCodec project
 * 
 */
public class Intra8x8PredictionBuilder {

    byte[] topBuf;
    byte[] leftBuf;
    byte[] genBuf;
    
    public Intra8x8PredictionBuilder() {
        this.topBuf = new byte[16];
        this.leftBuf = new byte[8];
        this.genBuf = new byte[24];
    }
    
    public void predictWithMode(int mode, int[] residual, boolean leftAvailable, boolean topAvailable,
            boolean topLeftAvailable, boolean topRightAvailable, byte[] leftRow, byte[] topLine, byte[] topLeft,
            int mbOffX, int blkX, int blkY, byte[] pixOut) {
        switch (mode) {
        case 0:
            Assert.assertTrue(topAvailable);
            predictVertical(residual, topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX, blkX, blkY, pixOut);
            break;
        case 1:
            Assert.assertTrue(leftAvailable);
            predictHorizontal(residual, topLeftAvailable, topLeft, leftRow, mbOffX, blkX, blkY, pixOut);
            break;
        case 2:
            predictDC(residual, topLeftAvailable, topRightAvailable, leftAvailable, topAvailable, topLeft, leftRow,
                    topLine, mbOffX, blkX, blkY, pixOut);
            break;
        case 3:
            Assert.assertTrue(topAvailable);
            predictDiagonalDownLeft(residual, topLeftAvailable, topAvailable, topRightAvailable, topLeft, topLine,
                    mbOffX, blkX, blkY, pixOut);
            break;
        case 4:
            Assert.assertTrue(topAvailable && leftAvailable && topLeftAvailable);
            predictDiagonalDownRight(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY, pixOut);
            break;
        case 5:
            Assert.assertTrue(topAvailable && leftAvailable && topLeftAvailable);
            predictVerticalRight(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY, pixOut);
            break;
        case 6:
            Assert.assertTrue(topAvailable && leftAvailable && topLeftAvailable);
            predictHorizontalDown(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY, pixOut);
            break;
        case 7:
            Assert.assertTrue(topAvailable);
            predictVerticalLeft(residual, topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX, blkX, blkY,
                    pixOut);
            break;
        case 8:
            Assert.assertTrue(leftAvailable);
            predictHorizontalUp(residual, topLeftAvailable, topLeft, leftRow, mbOffX, blkX, blkY, pixOut);
            break;
        }

        int oo1 = mbOffX + blkX;
        int off1 = (blkY << 4) + blkX + 7;

        topLeft[blkY >> 2] = topLine[oo1 + 7];

        for (int i = 0; i < 8; i++)
            leftRow[blkY + i] = pixOut[off1 + (i << 4)];

        int off2 = (blkY << 4) + blkX + 112;
        for (int i = 0; i < 8; i++)
            topLine[oo1 + i] = pixOut[off2 + i];

        topLeft[(blkY >> 2) + 1] = leftRow[blkY + 3];
    }

    private void interpolateTop(boolean topLeftAvailable, boolean topRightAvailable, byte[] topLeft, byte[] topLine,
            int blkX, int blkY, byte[] out) {
        int a = topLeftAvailable ? topLeft[blkY >> 2] : topLine[blkX];

        out[0] = (byte) ((a + (topLine[blkX] << 1) + topLine[blkX + 1] + 2) >> 2);
        int i;
        for (i = 1; i < 7; i++)
            out[i] = (byte) ((topLine[blkX + i - 1] + (topLine[blkX + i] << 1) + topLine[blkX + i + 1] + 2) >> 2);

        if (topRightAvailable) {
            for (; i < 15; i++)
                out[i] = (byte) ((topLine[blkX + i - 1] + (topLine[blkX + i] << 1) + topLine[blkX + i + 1] + 2) >> 2);
            out[15] = (byte) ((topLine[blkX + 14] + (topLine[blkX + 15] << 1) + topLine[blkX + 15] + 2) >> 2);
        } else {
            out[7] = (byte) ((topLine[blkX + 6] + (topLine[blkX + 7] << 1) + topLine[blkX + 7] + 2) >> 2);
            for (i = 8; i < 16; i++)
                out[i] = topLine[blkX + 7];
        }
    }

    private void interpolateLeft(boolean topLeftAvailable, byte[] topLeft, byte[] leftRow, int blkY, byte[] out) {
        int a = topLeftAvailable ? topLeft[blkY >> 2] : leftRow[0];

        out[0] = (byte) ((a + (leftRow[blkY] << 1) + leftRow[blkY + 1] + 2) >> 2);
        for (int i = 1; i < 7; i++)
            out[i] = (byte) ((leftRow[blkY + i - 1] + (leftRow[blkY + i] << 1) + leftRow[blkY + i + 1] + 2) >> 2);

        out[7] = (byte) ((leftRow[blkY + 6] + (leftRow[blkY + 7] << 1) + leftRow[blkY + 7] + 2) >> 2);
    }

    private int interpolateTopLeft(boolean topAvailable, boolean leftAvailable, byte[] topLeft, byte[] topLine,
            byte[] leftRow, int mbOffX, int blkX, int blkY) {
        int a = topLeft[blkY >> 2];
        int b = topAvailable ? topLine[mbOffX + blkX] : a;
        int c = leftAvailable ? leftRow[blkY] : a;
        int aa = a << 1;

        return (aa + b + c + 2) >> 2;
    }

    public void copyAdd(byte[] pred, int srcOff, int[] residual, int pixOff, int rOff, byte[] out) {
        out[pixOff] = (byte) clip(residual[rOff] + pred[srcOff], -128, 127);
        out[pixOff + 1] = (byte) clip(residual[rOff + 1] + pred[srcOff + 1], -128, 127);
        out[pixOff + 2] = (byte) clip(residual[rOff + 2] + pred[srcOff + 2], -128, 127);
        out[pixOff + 3] = (byte) clip(residual[rOff + 3] + pred[srcOff + 3], -128, 127);
        out[pixOff + 4] = (byte) clip(residual[rOff + 4] + pred[srcOff + 4], -128, 127);
        out[pixOff + 5] = (byte) clip(residual[rOff + 5] + pred[srcOff + 5], -128, 127);
        out[pixOff + 6] = (byte) clip(residual[rOff + 6] + pred[srcOff + 6], -128, 127);
        out[pixOff + 7] = (byte) clip(residual[rOff + 7] + pred[srcOff + 7], -128, 127);
    }

    public void fillAdd(int[] residual, int pixOff, int val, byte[] pixOut) {
        int rOff = 0;
        for (int i = 0; i < 8; i++) {
            pixOut[pixOff] = (byte) clip(residual[rOff] + val, -128, 127);
            pixOut[pixOff + 1] = (byte) clip(residual[rOff + 1] + val, -128, 127);
            pixOut[pixOff + 2] = (byte) clip(residual[rOff + 2] + val, -128, 127);
            pixOut[pixOff + 3] = (byte) clip(residual[rOff + 3] + val, -128, 127);
            pixOut[pixOff + 4] = (byte) clip(residual[rOff + 4] + val, -128, 127);
            pixOut[pixOff + 5] = (byte) clip(residual[rOff + 5] + val, -128, 127);
            pixOut[pixOff + 6] = (byte) clip(residual[rOff + 6] + val, -128, 127);
            pixOut[pixOff + 7] = (byte) clip(residual[rOff + 7] + val, -128, 127);
            pixOff += 16;
            rOff += 8;
        }
    }

    public void predictVertical(int[] residual, boolean topLeftAvailable, boolean topRightAvailable, byte[] topLeft,
            byte[] topLine, int mbOffX, int blkX, int blkY, byte[] pixOut) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
        int pixOff = (blkY << 4) + blkX;
        int rOff = 0;
        for (int i = 0; i < 8; i++) {
            pixOut[pixOff] = (byte) clip(residual[rOff] + topBuf[0], -128, 127);
            pixOut[pixOff + 1] = (byte) clip(residual[rOff + 1] + topBuf[1], -128, 127);
            pixOut[pixOff + 2] = (byte) clip(residual[rOff + 2] + topBuf[2], -128, 127);
            pixOut[pixOff + 3] = (byte) clip(residual[rOff + 3] + topBuf[3], -128, 127);
            pixOut[pixOff + 4] = (byte) clip(residual[rOff + 4] + topBuf[4], -128, 127);
            pixOut[pixOff + 5] = (byte) clip(residual[rOff + 5] + topBuf[5], -128, 127);
            pixOut[pixOff + 6] = (byte) clip(residual[rOff + 6] + topBuf[6], -128, 127);
            pixOut[pixOff + 7] = (byte) clip(residual[rOff + 7] + topBuf[7], -128, 127);
            pixOff += 16;
            rOff += 8;
        }
    }

    public void predictHorizontal(int[] residual, boolean topLeftAvailable, byte[] topLeft, byte[] leftRow, int mbOffX,
            int blkX, int blkY, byte[] pixOut) {
        interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf);
        int pixOff = (blkY << 4) + blkX;
        int rOff = 0;
        for (int i = 0; i < 8; i++) {
            pixOut[pixOff] = (byte) clip(residual[rOff] + leftBuf[i], -128, 127);
            pixOut[pixOff + 1] = (byte) clip(residual[rOff + 1] + leftBuf[i], -128, 127);
            pixOut[pixOff + 2] = (byte) clip(residual[rOff + 2] + leftBuf[i], -128, 127);
            pixOut[pixOff + 3] = (byte) clip(residual[rOff + 3] + leftBuf[i], -128, 127);
            pixOut[pixOff + 4] = (byte) clip(residual[rOff + 4] + leftBuf[i], -128, 127);
            pixOut[pixOff + 5] = (byte) clip(residual[rOff + 5] + leftBuf[i], -128, 127);
            pixOut[pixOff + 6] = (byte) clip(residual[rOff + 6] + leftBuf[i], -128, 127);
            pixOut[pixOff + 7] = (byte) clip(residual[rOff + 7] + leftBuf[i], -128, 127);
            pixOff += 16;
            rOff += 8;
        }
    }

    public void predictDC(int[] residual, boolean topLeftAvailable, boolean topRightAvailable, boolean leftAvailable,
            boolean topAvailable, byte[] topLeft, byte[] leftRow, byte[] topLine, int mbOffX, int blkX, int blkY,
            byte[] pixOut) {
        if (topAvailable && leftAvailable) {
            interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
            interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf);
            int sum1 = topBuf[0] + topBuf[1] + topBuf[2] + topBuf[3];
            int sum2 = topBuf[4] + topBuf[5] + topBuf[6] + topBuf[7];
            int sum3 = leftBuf[0] + leftBuf[1] + leftBuf[2] + leftBuf[3];
            int sum4 = leftBuf[4] + leftBuf[5] + leftBuf[6] + leftBuf[7];
            fillAdd(residual, (blkY << 4) + blkX, (sum1 + sum2 + sum3 + sum4 + 8) >> 4, pixOut);
        } else if (leftAvailable) {
            interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf);
            int sum3 = leftBuf[0] + leftBuf[1] + leftBuf[2] + leftBuf[3];
            int sum4 = leftBuf[4] + leftBuf[5] + leftBuf[6] + leftBuf[7];
            fillAdd(residual, (blkY << 4) + blkX, (sum3 + sum4 + 4) >> 3, pixOut);
        } else if (topAvailable) {
            interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
            int sum1 = topBuf[0] + topBuf[1] + topBuf[2] + topBuf[3];
            int sum2 = topBuf[4] + topBuf[5] + topBuf[6] + topBuf[7];
            fillAdd(residual, (blkY << 4) + blkX, (sum1 + sum2 + 4) >> 3, pixOut);
        } else {
            fillAdd(residual, (blkY << 4) + blkX, 0, pixOut);
        }
    }

    public void predictDiagonalDownLeft(int[] residual, boolean topLeftAvailable, boolean topAvailable,
            boolean topRightAvailable, byte[] topLeft, byte[] topLine, int mbOffX, int blkX, int blkY, byte[] pixOut) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);

        genBuf[0] = (byte) ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[1] = (byte) ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[2] = (byte) ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[3] = (byte) ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[4] = (byte) ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);
        genBuf[5] = (byte) ((topBuf[5] + topBuf[7] + ((topBuf[6]) << 1) + 2) >> 2);
        genBuf[6] = (byte) ((topBuf[6] + topBuf[8] + ((topBuf[7]) << 1) + 2) >> 2);
        genBuf[7] = (byte) ((topBuf[7] + topBuf[9] + ((topBuf[8]) << 1) + 2) >> 2);
        genBuf[8] = (byte) ((topBuf[8] + topBuf[10] + ((topBuf[9]) << 1) + 2) >> 2);
        genBuf[9] = (byte) ((topBuf[9] + topBuf[11] + ((topBuf[10]) << 1) + 2) >> 2);
        genBuf[10] = (byte) ((topBuf[10] + topBuf[12] + ((topBuf[11]) << 1) + 2) >> 2);
        genBuf[11] = (byte) ((topBuf[11] + topBuf[13] + ((topBuf[12]) << 1) + 2) >> 2);
        genBuf[12] = (byte) ((topBuf[12] + topBuf[14] + ((topBuf[13]) << 1) + 2) >> 2);
        genBuf[13] = (byte) ((topBuf[13] + topBuf[15] + ((topBuf[14]) << 1) + 2) >> 2);
        genBuf[14] = (byte) ((topBuf[14] + topBuf[15] + ((topBuf[15]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 0, residual, off, 0, pixOut);
        copyAdd(genBuf, 1, residual, off + 16, 8, pixOut);
        copyAdd(genBuf, 2, residual, off + 32, 16, pixOut);
        copyAdd(genBuf, 3, residual, off + 48, 24, pixOut);
        copyAdd(genBuf, 4, residual, off + 64, 32, pixOut);
        copyAdd(genBuf, 5, residual, off + 80, 40, pixOut);
        copyAdd(genBuf, 6, residual, off + 96, 48, pixOut);
        copyAdd(genBuf, 7, residual, off + 112, 56, pixOut);
    }

    public void predictDiagonalDownRight(int[] residual, boolean topRightAvailable, byte[] topLeft, byte[] leftRow,
            byte[] topLine, int mbOffX, int blkX, int blkY, byte[] pixOut) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf);
        int tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY);

        genBuf[0] = (byte) ((leftBuf[7] + leftBuf[5] + ((leftBuf[6]) << 1) + 2) >> 2);
        genBuf[1] = (byte) ((leftBuf[6] + leftBuf[4] + ((leftBuf[5]) << 1) + 2) >> 2);
        genBuf[2] = (byte) ((leftBuf[5] + leftBuf[3] + ((leftBuf[4]) << 1) + 2) >> 2);
        genBuf[3] = (byte) ((leftBuf[4] + leftBuf[2] + ((leftBuf[3]) << 1) + 2) >> 2);
        genBuf[4] = (byte) ((leftBuf[3] + leftBuf[1] + ((leftBuf[2]) << 1) + 2) >> 2);
        genBuf[5] = (byte) ((leftBuf[2] + leftBuf[0] + ((leftBuf[1]) << 1) + 2) >> 2);
        genBuf[6] = (byte) ((leftBuf[1] + tl + ((leftBuf[0]) << 1) + 2) >> 2);
        genBuf[7] = (byte) ((leftBuf[0] + topBuf[0] + ((tl) << 1) + 2) >> 2);
        genBuf[8] = (byte) ((tl + topBuf[1] + ((topBuf[0]) << 1) + 2) >> 2);
        genBuf[9] = (byte) ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[10] = (byte) ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[11] = (byte) ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[12] = (byte) ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[13] = (byte) ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);
        genBuf[14] = (byte) ((topBuf[5] + topBuf[7] + ((topBuf[6]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 7, residual, off, 0, pixOut);
        copyAdd(genBuf, 6, residual, off + 16, 8, pixOut);
        copyAdd(genBuf, 5, residual, off + 32, 16, pixOut);
        copyAdd(genBuf, 4, residual, off + 48, 24, pixOut);
        copyAdd(genBuf, 3, residual, off + 64, 32, pixOut);
        copyAdd(genBuf, 2, residual, off + 80, 40, pixOut);
        copyAdd(genBuf, 1, residual, off + 96, 48, pixOut);
        copyAdd(genBuf, 0, residual, off + 112, 56, pixOut);
    }

    public void predictVerticalRight(int[] residual, boolean topRightAvailable, byte[] topLeft, byte[] leftRow,
            byte[] topLine, int mbOffX, int blkX, int blkY, byte[] pixOut) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf);
        int tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY);

        genBuf[0] = (byte) ((leftBuf[5] + leftBuf[3] + ((leftBuf[4]) << 1) + 2) >> 2);
        genBuf[1] = (byte) ((leftBuf[3] + leftBuf[1] + ((leftBuf[2]) << 1) + 2) >> 2);
        genBuf[2] = (byte) ((leftBuf[1] + tl + ((leftBuf[0]) << 1) + 2) >> 2);
        genBuf[3] = (byte) ((tl + topBuf[0] + 1) >> 1);
        genBuf[4] = (byte) ((topBuf[0] + topBuf[1] + 1) >> 1);
        genBuf[5] = (byte) ((topBuf[1] + topBuf[2] + 1) >> 1);
        genBuf[6] = (byte) ((topBuf[2] + topBuf[3] + 1) >> 1);
        genBuf[7] = (byte) ((topBuf[3] + topBuf[4] + 1) >> 1);
        genBuf[8] = (byte) ((topBuf[4] + topBuf[5] + 1) >> 1);
        genBuf[9] = (byte) ((topBuf[5] + topBuf[6] + 1) >> 1);
        genBuf[10] = (byte) ((topBuf[6] + topBuf[7] + 1) >> 1);
        genBuf[11] = (byte) ((leftBuf[6] + leftBuf[4] + ((leftBuf[5]) << 1) + 2) >> 2);
        genBuf[12] = (byte) ((leftBuf[4] + leftBuf[2] + ((leftBuf[3]) << 1) + 2) >> 2);
        genBuf[13] = (byte) ((leftBuf[2] + leftBuf[0] + ((leftBuf[1]) << 1) + 2) >> 2);
        genBuf[14] = (byte) ((leftBuf[0] + topBuf[0] + ((tl) << 1) + 2) >> 2);
        genBuf[15] = (byte) ((tl + topBuf[1] + ((topBuf[0]) << 1) + 2) >> 2);
        genBuf[16] = (byte) ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[17] = (byte) ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[18] = (byte) ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[19] = (byte) ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[20] = (byte) ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);
        genBuf[21] = (byte) ((topBuf[5] + topBuf[7] + ((topBuf[6]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 3, residual, off, 0, pixOut);
        copyAdd(genBuf, 14, residual, off + 16, 8, pixOut);
        copyAdd(genBuf, 2, residual, off + 32, 16, pixOut);
        copyAdd(genBuf, 13, residual, off + 48, 24, pixOut);
        copyAdd(genBuf, 1, residual, off + 64, 32, pixOut);
        copyAdd(genBuf, 12, residual, off + 80, 40, pixOut);
        copyAdd(genBuf, 0, residual, off + 96, 48, pixOut);
        copyAdd(genBuf, 11, residual, off + 112, 56, pixOut);
    }

    public void predictHorizontalDown(int[] residual, boolean topRightAvailable, byte[] topLeft, byte[] leftRow,
            byte[] topLine, int mbOffX, int blkX, int blkY, byte[] pixOut) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf);
        int tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY);

        genBuf[0] = (byte) ((leftBuf[7] + leftBuf[6] + 1) >> 1);
        genBuf[1] = (byte) ((leftBuf[5] + leftBuf[7] + (leftBuf[6] << 1) + 2) >> 2);
        genBuf[2] = (byte) ((leftBuf[6] + leftBuf[5] + 1) >> 1);
        genBuf[3] = (byte) ((leftBuf[4] + leftBuf[6] + ((leftBuf[5]) << 1) + 2) >> 2);
        genBuf[4] = (byte) ((leftBuf[5] + leftBuf[4] + 1) >> 1);
        genBuf[5] = (byte) ((leftBuf[3] + leftBuf[5] + ((leftBuf[4]) << 1) + 2) >> 2);
        genBuf[6] = (byte) ((leftBuf[4] + leftBuf[3] + 1) >> 1);
        genBuf[7] = (byte) ((leftBuf[2] + leftBuf[4] + ((leftBuf[3]) << 1) + 2) >> 2);
        genBuf[8] = (byte) ((leftBuf[3] + leftBuf[2] + 1) >> 1);
        genBuf[9] = (byte) ((leftBuf[1] + leftBuf[3] + ((leftBuf[2]) << 1) + 2) >> 2);
        genBuf[10] = (byte) ((leftBuf[2] + leftBuf[1] + 1) >> 1);
        genBuf[11] = (byte) ((leftBuf[0] + leftBuf[2] + ((leftBuf[1]) << 1) + 2) >> 2);
        genBuf[12] = (byte) ((leftBuf[1] + leftBuf[0] + 1) >> 1);
        genBuf[13] = (byte) ((tl + leftBuf[1] + ((leftBuf[0]) << 1) + 2) >> 2);
        genBuf[14] = (byte) ((leftBuf[0] + tl + 1) >> 1);
        genBuf[15] = (byte) ((leftBuf[0] + topBuf[0] + ((tl) << 1) + 2) >> 2);
        genBuf[16] = (byte) ((tl + topBuf[1] + ((topBuf[0]) << 1) + 2) >> 2);
        genBuf[17] = (byte) ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[18] = (byte) ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[19] = (byte) ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[20] = (byte) ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[21] = (byte) ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 14, residual, off, 0, pixOut);
        copyAdd(genBuf, 12, residual, off + 16, 8, pixOut);
        copyAdd(genBuf, 10, residual, off + 32, 16, pixOut);
        copyAdd(genBuf, 8, residual, off + 48, 24, pixOut);
        copyAdd(genBuf, 6, residual, off + 64, 32, pixOut);
        copyAdd(genBuf, 4, residual, off + 80, 40, pixOut);
        copyAdd(genBuf, 2, residual, off + 96, 48, pixOut);
        copyAdd(genBuf, 0, residual, off + 112, 56, pixOut);
    }

    public void predictVerticalLeft(int[] residual, boolean topLeftAvailable, boolean topRightAvailable,
            byte[] topLeft, byte[] topLine, int mbOffX, int blkX, int blkY, byte[] pixOut) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf);

        genBuf[0] = (byte) ((topBuf[0] + topBuf[1] + 1) >> 1);
        genBuf[1] = (byte) ((topBuf[1] + topBuf[2] + 1) >> 1);
        genBuf[2] = (byte) ((topBuf[2] + topBuf[3] + 1) >> 1);
        genBuf[3] = (byte) ((topBuf[3] + topBuf[4] + 1) >> 1);
        genBuf[4] = (byte) ((topBuf[4] + topBuf[5] + 1) >> 1);
        genBuf[5] = (byte) ((topBuf[5] + topBuf[6] + 1) >> 1);
        genBuf[6] = (byte) ((topBuf[6] + topBuf[7] + 1) >> 1);
        genBuf[7] = (byte) ((topBuf[7] + topBuf[8] + 1) >> 1);
        genBuf[8] = (byte) ((topBuf[8] + topBuf[9] + 1) >> 1);
        genBuf[9] = (byte) ((topBuf[9] + topBuf[10] + 1) >> 1);
        genBuf[10] = (byte) ((topBuf[10] + topBuf[11] + 1) >> 1);
        genBuf[11] = (byte) ((topBuf[0] + topBuf[2] + ((topBuf[1]) << 1) + 2) >> 2);
        genBuf[12] = (byte) ((topBuf[1] + topBuf[3] + ((topBuf[2]) << 1) + 2) >> 2);
        genBuf[13] = (byte) ((topBuf[2] + topBuf[4] + ((topBuf[3]) << 1) + 2) >> 2);
        genBuf[14] = (byte) ((topBuf[3] + topBuf[5] + ((topBuf[4]) << 1) + 2) >> 2);
        genBuf[15] = (byte) ((topBuf[4] + topBuf[6] + ((topBuf[5]) << 1) + 2) >> 2);
        genBuf[16] = (byte) ((topBuf[5] + topBuf[7] + ((topBuf[6]) << 1) + 2) >> 2);
        genBuf[17] = (byte) ((topBuf[6] + topBuf[8] + ((topBuf[7]) << 1) + 2) >> 2);
        genBuf[18] = (byte) ((topBuf[7] + topBuf[9] + ((topBuf[8]) << 1) + 2) >> 2);
        genBuf[19] = (byte) ((topBuf[8] + topBuf[10] + ((topBuf[9]) << 1) + 2) >> 2);
        genBuf[20] = (byte) ((topBuf[9] + topBuf[11] + ((topBuf[10]) << 1) + 2) >> 2);
        genBuf[21] = (byte) ((topBuf[10] + topBuf[12] + ((topBuf[11]) << 1) + 2) >> 2);

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 0, residual, off, 0, pixOut);
        copyAdd(genBuf, 11, residual, off + 16, 8, pixOut);
        copyAdd(genBuf, 1, residual, off + 32, 16, pixOut);
        copyAdd(genBuf, 12, residual, off + 48, 24, pixOut);
        copyAdd(genBuf, 2, residual, off + 64, 32, pixOut);
        copyAdd(genBuf, 13, residual, off + 80, 40, pixOut);
        copyAdd(genBuf, 3, residual, off + 96, 48, pixOut);
        copyAdd(genBuf, 14, residual, off + 112, 56, pixOut);
    }

    public void predictHorizontalUp(int[] residual, boolean topLeftAvailable, byte[] topLeft, byte[] leftRow,
            int mbOffX, int blkX, int blkY, byte[] pixOut) {
        interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf);

        genBuf[0] = (byte) ((leftBuf[0] + leftBuf[1] + 1) >> 1);
        genBuf[1] = (byte) ((leftBuf[2] + leftBuf[0] + ((leftBuf[1]) << 1) + 2) >> 2);
        genBuf[2] = (byte) ((leftBuf[1] + leftBuf[2] + 1) >> 1);
        genBuf[3] = (byte) ((leftBuf[3] + leftBuf[1] + ((leftBuf[2]) << 1) + 2) >> 2);
        genBuf[4] = (byte) ((leftBuf[2] + leftBuf[3] + 1) >> 1);
        genBuf[5] = (byte) ((leftBuf[4] + leftBuf[2] + ((leftBuf[3]) << 1) + 2) >> 2);
        genBuf[6] = (byte) ((leftBuf[3] + leftBuf[4] + 1) >> 1);
        genBuf[7] = (byte) ((leftBuf[5] + leftBuf[3] + ((leftBuf[4]) << 1) + 2) >> 2);
        genBuf[8] = (byte) ((leftBuf[4] + leftBuf[5] + 1) >> 1);
        genBuf[9] = (byte) ((leftBuf[6] + leftBuf[4] + ((leftBuf[5]) << 1) + 2) >> 2);
        genBuf[10] = (byte) ((leftBuf[5] + leftBuf[6] + 1) >> 1);
        genBuf[11] = (byte) ((leftBuf[7] + leftBuf[5] + ((leftBuf[6]) << 1) + 2) >> 2);
        genBuf[12] = (byte) ((leftBuf[6] + leftBuf[7] + 1) >> 1);
        genBuf[13] = (byte) ((leftBuf[6] + leftBuf[7] + ((leftBuf[7]) << 1) + 2) >> 2);
        genBuf[14] = genBuf[15] = genBuf[16] = genBuf[17] = genBuf[18] = genBuf[19] = genBuf[20] = genBuf[21] = leftBuf[7];

        int off = (blkY << 4) + blkX;
        copyAdd(genBuf, 0, residual, off, 0, pixOut);
        copyAdd(genBuf, 2, residual, off + 16, 8, pixOut);
        copyAdd(genBuf, 4, residual, off + 32, 16, pixOut);
        copyAdd(genBuf, 6, residual, off + 48, 24, pixOut);
        copyAdd(genBuf, 8, residual, off + 64, 32, pixOut);
        copyAdd(genBuf, 10, residual, off + 80, 40, pixOut);
        copyAdd(genBuf, 12, residual, off + 96, 48, pixOut);
        copyAdd(genBuf, 14, residual, off + 112, 56, pixOut);
    }
}