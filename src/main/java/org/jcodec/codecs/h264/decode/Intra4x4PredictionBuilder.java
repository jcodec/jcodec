package org.jcodec.codecs.h264.decode;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Builds intra prediction for intra 4x4 coded macroblocks
 * 
 * @author Jay Codec
 * 
 */
public class Intra4x4PredictionBuilder {

    public static void predictWithMode(int mode, boolean leftAvailable, boolean topAvailable,
            boolean topRightAvailable, int[] leftRow, int[] topLine, int topLeft[], int blkAbsX, int blkOffY, int[] out) {
        switch (mode) {
        case 0:
            predictVertical(topAvailable, topLine, blkAbsX, blkOffY, out);
            break;
        case 1:
            predictHorizontal(leftAvailable, leftRow, blkAbsX, blkOffY, out);
            break;
        case 2:
            predictDC(leftAvailable, topAvailable, leftRow, topLine, blkAbsX, blkOffY, out);
            break;
        case 3:
            predictDiagonalDownLeft(topAvailable, topRightAvailable, topLine, blkAbsX, blkOffY, out);
            break;
        case 4:
            predictDiagonalDownRight(leftAvailable, topAvailable, leftRow, topLine, topLeft, blkAbsX, blkOffY, out);
            break;
        case 5:
            predictVerticalRight(leftAvailable, topAvailable, leftRow, topLine, topLeft, blkAbsX, blkOffY, out);
            break;
        case 6:
            predictHorizontalDown(leftAvailable, topAvailable, leftRow, topLine, topLeft, blkAbsX, blkOffY, out);
            break;
        case 7:
            predictVerticalLeft(topAvailable, topRightAvailable, topLine, blkAbsX, blkOffY, out);
            break;
        case 8:
            predictHorizontalUp(leftAvailable, leftRow, blkAbsX, blkOffY, out);
            break;
        }
    }

    public static void predictVertical(boolean topAvailable, int[] topLine, int blkAbsX, int blkY, int[] out) {

        for (int j = 0, off = 0; j < 4; j++, off += 4) {
            out[off] = topLine[blkAbsX];
            out[off + 1] = topLine[blkAbsX + 1];
            out[off + 2] = topLine[blkAbsX + 2];
            out[off + 3] = topLine[blkAbsX + 3];
        }
    }

    public static void predictHorizontal(boolean leftAvailable, int[] leftRow, int blkAbsX, int blkY, int[] out) {

        for (int j = 0, off = 0; j < 4; j++, off += 4) {
            int l = leftRow[blkY + j];
            out[off] = out[off + 1] = out[off + 2] = out[off + 3] = l;
        }
    }

    public static void predictDC(boolean leftAvailable, boolean topAvailable, int[] leftRow, int[] topLine,
            int blkAbsX, int blkY, int[] out) {

        int val;
        if (leftAvailable && topAvailable) {
            val = (leftRow[blkY] + leftRow[blkY + 1] + leftRow[blkY + 2] + leftRow[blkY + 3] + topLine[blkAbsX]
                    + topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + topLine[blkAbsX + 3] + 4) >> 3;
        } else if (leftAvailable) {
            val = (leftRow[blkY] + leftRow[blkY + 1] + leftRow[blkY + 2] + leftRow[blkY + 3] + 2) >> 2;
        } else if (topAvailable) {
            val = (topLine[blkAbsX] + topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + topLine[blkAbsX + 3] + 2) >> 2;
        } else {
            val = 128;
        }

        Arrays.fill(out, val);
    }

    public static void predictDiagonalDownLeft(boolean topAvailable, boolean topRightAvailable, int[] topLine,
            int blkAbsX, int blkY, int[] out) {

        int tr0 = topLine[blkAbsX + 3], tr1 = topLine[blkAbsX + 3], tr2 = topLine[blkAbsX + 3], tr3 = topLine[blkAbsX + 3];
        if (topRightAvailable) {
            tr0 = topLine[blkAbsX + 4];
            tr1 = topLine[blkAbsX + 5];
            tr2 = topLine[blkAbsX + 6];
            tr3 = topLine[blkAbsX + 7];
        }

        out[0] = ((topLine[blkAbsX] + topLine[blkAbsX + 2] + 2 * (topLine[blkAbsX + 1]) + 2) >> 2);
        out[1] = out[4] = ((topLine[blkAbsX + 1] + topLine[blkAbsX + 3] + 2 * (topLine[blkAbsX + 2]) + 2) >> 2);
        out[2] = out[5] = out[8] = ((topLine[blkAbsX + 2] + tr0 + 2 * (topLine[blkAbsX + 3]) + 2) >> 2);
        out[3] = out[6] = out[9] = out[12] = ((topLine[blkAbsX + 3] + tr1 + 2 * (tr0) + 2) >> 2);
        out[7] = out[10] = out[13] = ((tr0 + tr2 + 2 * (tr1) + 2) >> 2);
        out[11] = out[14] = ((tr1 + tr3 + 2 * (tr2) + 2) >> 2);
        out[15] = ((tr2 + 3 * (tr3) + 2) >> 2);
    }

    public static void predictDiagonalDownRight(boolean leftAvailable, boolean topAvailable, int[] leftRow,
            int[] topLine, int[] topLeft, int blkAbsX, int blkY, int[] out) {

        int TL = topLeft[blkY];

        out[0] = out[5] = out[10] = out[15] = (topLine[blkAbsX] + 2 * TL + leftRow[blkY] + 2) >> 2;
        out[1] = ((TL + 2 * topLine[blkAbsX] + topLine[blkAbsX + 1] + 2) >> 2);
        out[2] = ((topLine[blkAbsX] + 2 * topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + 2) >> 2);
        out[3] = ((topLine[blkAbsX + 1] + 2 * topLine[blkAbsX + 2] + topLine[blkAbsX + 3] + 2) >> 2);
        out[4] = ((TL + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2);
        out[6] = ((TL + 2 * topLine[blkAbsX] + topLine[blkAbsX + 1] + 2) >> 2);
        out[7] = ((topLine[blkAbsX] + 2 * topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + 2) >> 2);
        out[8] = ((leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2) >> 2);
        out[9] = ((TL + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2);
        out[11] = ((TL + 2 * topLine[blkAbsX] + topLine[blkAbsX + 1] + 2) >> 2);
        out[12] = ((leftRow[blkY + 1] + 2 * leftRow[blkY + 2] + leftRow[blkY + 3] + 2) >> 2);
        out[13] = ((leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2) >> 2);
        out[14] = ((TL + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2);
    }

    public static void predictVerticalRight(boolean leftAvailable, boolean topAvailable, int[] leftRow, int[] topLine,
            int[] topLeft, int blkAbsX, int blkY, int[] out) {

        int TL = topLeft[blkY];
        out[0] = out[9] = (TL + topLine[blkAbsX + 0] + 1) >> 1;
        out[1] = out[10] = (topLine[blkAbsX + 0] + topLine[blkAbsX + 1] + 1) >> 1;
        out[2] = out[11] = (topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + 1) >> 1;
        out[3] = (topLine[blkAbsX + 2] + topLine[blkAbsX + 3] + 1) >> 1;
        out[4] = out[13] = (leftRow[blkY] + 2 * TL + topLine[blkAbsX + 0] + 2) >> 2;
        out[5] = out[14] = (TL + 2 * topLine[blkAbsX + 0] + topLine[blkAbsX + 1] + 2) >> 2;
        out[6] = out[15] = (topLine[blkAbsX + 0] + 2 * topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + 2) >> 2;
        out[7] = (topLine[blkAbsX + 1] + 2 * topLine[blkAbsX + 2] + topLine[blkAbsX + 3] + 2) >> 2;
        out[8] = (TL + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2;
        out[12] = (leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2) >> 2;
    }

    public static void predictHorizontalDown(boolean leftAvailable, boolean topAvailable, int[] leftRow, int[] topLine,
            int[] topLeft, int blkAbsX, int blkY, int[] out) {

        int TL = topLeft[blkY];

        out[0] = out[6] = (TL + leftRow[blkY] + 1) >> 1;
        out[1] = out[7] = (leftRow[blkY] + 2 * TL + topLine[blkAbsX + 0] + 2) >> 2;
        out[2] = (TL + 2 * topLine[blkAbsX + 0] + topLine[blkAbsX + 1] + 2) >> 2;
        out[3] = (topLine[blkAbsX + 0] + 2 * topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + 2) >> 2;
        out[4] = out[10] = (leftRow[blkY] + leftRow[blkY + 1] + 1) >> 1;
        out[5] = out[11] = (TL + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2;
        out[8] = out[14] = (leftRow[blkY + 1] + leftRow[blkY + 2] + 1) >> 1;
        out[9] = out[15] = (leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2) >> 2;
        out[12] = (leftRow[blkY + 2] + leftRow[blkY + 3] + 1) >> 1;
        out[13] = (leftRow[blkY + 1] + 2 * leftRow[blkY + 2] + leftRow[blkY + 3] + 2) >> 2;
    }

    public static void predictVerticalLeft(boolean topAvailable, boolean topRightAvailable, int[] topLine,
            int blkAbsX, int blkY, int[] out) {

        int tr0 = topLine[blkAbsX + 3], tr1 = topLine[blkAbsX + 3], tr2 = topLine[blkAbsX + 3];
        if (topRightAvailable) {
            tr0 = topLine[blkAbsX + 4];
            tr1 = topLine[blkAbsX + 5];
            tr2 = topLine[blkAbsX + 6];
        }

        out[0] = ((topLine[blkAbsX] + topLine[blkAbsX + 1] + 1) >> 1);
        out[1] = out[8] = ((topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + 1) >> 1);
        out[2] = out[9] = ((topLine[blkAbsX + 2] + topLine[blkAbsX + 3] + 1) >> 1);
        out[3] = out[10] = ((topLine[blkAbsX + 3] + tr0 + 1) >> 1);
        out[11] = ((tr0 + tr1 + 1) >> 1);
        out[4] = ((topLine[blkAbsX] + 2 * topLine[blkAbsX + 1] + topLine[blkAbsX + 2] + 2) >> 2);
        out[5] = out[12] = ((topLine[blkAbsX + 1] + 2 * topLine[blkAbsX + 2] + topLine[blkAbsX + 3] + 2) >> 2);
        out[6] = out[13] = ((topLine[blkAbsX + 2] + 2 * topLine[blkAbsX + 3] + tr0 + 2) >> 2);
        out[7] = out[14] = ((topLine[blkAbsX + 3] + 2 * tr0 + tr1 + 2) >> 2);
        out[15] = ((tr0 + 2 * tr1 + tr2 + 2) >> 2);
    }

    public static void predictHorizontalUp(boolean leftAvailable, int[] leftRow, int blkAbsX, int blkY,
            int[] out) {

        out[0] = ((leftRow[blkY] + leftRow[blkY + 1] + 1) >> 1);
        out[1] = ((leftRow[blkY] + (leftRow[blkY + 1] << 1) + leftRow[blkY + 2] + 2) >> 2);
        out[2] = out[4] = ((leftRow[blkY + 1] + leftRow[blkY + 2] + 1) >> 1);
        out[3] = out[5] = ((leftRow[blkY + 1] + (leftRow[blkY + 2] << 1) + leftRow[blkY + 3] + 2) >> 2);
        out[6] = out[8] = ((leftRow[blkY + 2] + leftRow[blkY + 3] + 1) >> 1);
        out[7] = out[9] = ((leftRow[blkY + 2] + (leftRow[blkY + 3] << 1) + leftRow[blkY + 3] + 2) >> 2);
        out[10] = out[11] = out[12] = out[13] = out[14] = out[15] = leftRow[blkY + 3];
    }
}