package org.jcodec.codecs.h264.decode;

import static org.jcodec.common.tools.MathUtil.clip;

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

    public static void predictWithMode(int mode, int[] residual, boolean leftAvailable, boolean topAvailable,
            boolean topRightAvailable, int[] leftRow, int[] topLine, int topLeft[], int mbOffX, int blkX, int blkY) {
        switch (mode) {
        case 0:
            predictVertical(residual, topAvailable, topLine, mbOffX, blkX, blkY);
            break;
        case 1:
            predictHorizontal(residual, leftAvailable, leftRow, mbOffX, blkX, blkY);
            break;
        case 2:
            predictDC(residual, leftAvailable, topAvailable, leftRow, topLine, mbOffX, blkX, blkY);
            break;
        case 3:
            predictDiagonalDownLeft(residual, topAvailable, topRightAvailable, topLine, mbOffX,
                    blkX, blkY);
            break;
        case 4:
            predictDiagonalDownRight(residual, leftAvailable, topAvailable, leftRow, topLine, topLeft, mbOffX, blkX, blkY);
            break;
        case 5:
            predictVerticalRight(residual, leftAvailable, topAvailable, leftRow, topLine, topLeft, mbOffX, blkX, blkY);
            break;
        case 6:
            predictHorizontalDown(residual, leftAvailable, topAvailable, leftRow, topLine, topLeft, mbOffX, blkX, blkY);
            break;
        case 7:
            predictVerticalLeft(residual, topAvailable, topRightAvailable, topLine, mbOffX,
                    blkX, blkY);
            break;
        case 8:
            predictHorizontalUp(residual, leftAvailable, leftRow, mbOffX, blkX, blkY);
            break;
        }

        int oo1 = mbOffX + blkX;
        int off1 = (blkY << 4) + blkX + 3;
        
        topLeft[blkY >> 2] = topLine[oo1 + 3];
        
        leftRow[blkY] = residual[off1];
        leftRow[blkY + 1] = residual[off1 + 16];
        leftRow[blkY + 2] = residual[off1 + 32];
        leftRow[blkY + 3] = residual[off1 + 48];

        int off2 = (blkY << 4) + blkX + 48;
        topLine[oo1] = residual[off2];
        topLine[oo1 + 1] = residual[off2 + 1];
        topLine[oo1 + 2] = residual[off2 + 2];
        topLine[oo1 + 3] = residual[off2 + 3];
    }

    public static void predictVertical(int[] residual, boolean topAvailable,
            int[] topLine, int mbOffX, int blkX, int blkY) {

        int off = (blkY << 4) + blkX;
        int toff = mbOffX + blkX;
        for (int j = 0; j < 4; j++) {
            residual[off] = clip(residual[off] + topLine[toff], 0, 255);
            residual[off + 1] = clip(residual[off + 1] + topLine[toff + 1], 0, 255);
            residual[off + 2] = clip(residual[off + 2] + topLine[toff + 2], 0, 255);
            residual[off + 3] = clip(residual[off + 3] + topLine[toff + 3], 0, 255);
            off += 16;
        }
    }

    public static void predictHorizontal(int[] residual, boolean leftAvailable, int[] leftRow,
            int mbOffX, int blkX, int blkY) {

        int off = (blkY << 4) + blkX;
        for (int j = 0; j < 4; j++) {
            int l = leftRow[blkY + j];
            residual[off] = clip(residual[off] + l, 0, 255);
            residual[off + 1] = clip(residual[off + 1] + l, 0, 255);
            residual[off + 2] = clip(residual[off + 2] + l, 0, 255);
            residual[off + 3] = clip(residual[off + 3] + l, 0, 255);
            off += 16;
        }
    }

    public static void predictDC(int[] residual, boolean leftAvailable, boolean topAvailable, int[] leftRow,
            int[] topLine, int mbOffX, int blkX, int blkY) {

        int val;
        if (leftAvailable && topAvailable) {
            val = (leftRow[blkY] + leftRow[blkY + 1] + leftRow[blkY + 2] + leftRow[blkY + 3]
                    + topLine[mbOffX + blkX] + topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2]
                    + topLine[mbOffX + blkX + 3] + 4) >> 3;
        } else if (leftAvailable) {
            val = (leftRow[blkY] + leftRow[blkY + 1] + leftRow[blkY + 2] + leftRow[blkY + 3] + 2) >> 2;
        } else if (topAvailable) {
            val = (topLine[mbOffX + blkX] + topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2]
                    + topLine[mbOffX + blkX + 3] + 2) >> 2;
        } else {
            val = 128;
        }

        int off = (blkY << 4) + blkX;
        for (int j = 0; j < 4; j++) {
            residual[off] = clip(residual[off] + val, 0, 255);
            residual[off + 1] = clip(residual[off + 1] + val, 0, 255);
            residual[off + 2] = clip(residual[off + 2] + val, 0, 255);
            residual[off + 3] = clip(residual[off + 3] + val, 0, 255);
            off += 16;
        }
    }

    public static void predictDiagonalDownLeft(int[] residual, boolean topAvailable,
            boolean topRightAvailable, int[] topLine, int mbOffX, int blkX, int blkY) {
        
        int to = mbOffX + blkX;
        int tr0 = topLine[to+3],
        tr1 = topLine[to+3],
        tr2 = topLine[to+3],
        tr3 = topLine[to+3];
        if(topRightAvailable) {
            tr0 = topLine[to+4];
            tr1 = topLine[to+5];
            tr2 = topLine[to+6];
            tr3 = topLine[to+7];
        }
        
        int c0 = ((topLine[to] + topLine[to+2] + 2*(topLine[to+1]) + 2) >> 2);
        int c1 = ((topLine[to+1] + topLine[to+3] + 2*(topLine[to+2]) + 2) >> 2);
        int c2 = ((topLine[to+2] + tr0 + 2*(topLine[to+3]) + 2) >> 2);
        int c3 = ((topLine[to+3] + tr1 + 2*(tr0) + 2) >> 2);
        int c4 = ((tr0 + tr2 + 2*(tr1) + 2) >> 2);
        int c5 = ((tr1 + tr3 + 2*(tr2) + 2) >> 2);
        int c6 = ((tr2 + 3*(tr3) + 2) >> 2);
        
        int off = (blkY << 4) + blkX;
        residual[off] = clip(residual[off] + c0, 0, 255);
        residual[off+1] = clip(residual[off+1] + c1, 0, 255);
        residual[off+2] = clip(residual[off+2] + c2, 0, 255);
        residual[off+3] = clip(residual[off+3] + c3, 0, 255);
        
        residual[off+16] = clip(residual[off+16] + c1, 0, 255);
        residual[off+17] = clip(residual[off+17] + c2, 0, 255);
        residual[off+18] = clip(residual[off+18] + c3, 0, 255);
        residual[off+19] = clip(residual[off+19] + c4, 0, 255);
        
        residual[off+32] = clip(residual[off+32] + c2, 0, 255);
        residual[off+33] = clip(residual[off+33] + c3, 0, 255);
        residual[off+34] = clip(residual[off+34] + c4, 0, 255);
        residual[off+35] = clip(residual[off+35] + c5, 0, 255);
        
        residual[off+48] = clip(residual[off+48] + c3, 0, 255);
        residual[off+49] = clip(residual[off+49] + c4, 0, 255);
        residual[off+50] = clip(residual[off+50] + c5, 0, 255);
        residual[off+51] = clip(residual[off+51] + c6, 0, 255);
    }

    public static void predictDiagonalDownRight(int[] residual, boolean leftAvailable, boolean topAvailable,
            int[] leftRow, int[] topLine, int[] topLeft, int mbOffX, int blkX, int blkY) {

        int off = (blkY << 4) + blkX;
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                if (x > y) {
                    int t1;
                    if (x - y - 2 == -1)
                        t1 = topLeft[blkY >> 2];
                    else
                        t1 = topLine[mbOffX + blkX + x - y - 2];

                    int t2;
                    if (x - y - 1 == -1)
                        t2 = topLeft[blkY >> 2];
                    else
                        t2 = topLine[mbOffX + blkX + x - y - 1];

                    int t3;
                    if (x - y == -1)
                        t3 = topLeft[blkY >> 2];
                    else
                        t3 = topLine[mbOffX + blkX + x - y];

                    residual[off + x] = clip(residual[off + x] + ((t1 + 2 * t2 + t3 + 2) >> 2), 0, 255);
                } else if (x < y) {
                    int l1;
                    if (y - x - 2 == -1)
                        l1 = topLeft[blkY >> 2];
                    else
                        l1 = leftRow[blkY + y - x - 2];

                    int l2;
                    if (y - x - 1 == -1)
                        l2 = topLeft[blkY >> 2];
                    else
                        l2 = leftRow[blkY + y - x - 1];

                    int l3;
                    if (y - x == -1)
                        l3 = topLeft[blkY >> 2];
                    else
                        l3 = leftRow[blkY + y - x];

                    residual[off + x] = clip(residual[off + x] + ((l1 + 2 * l2 + l3 + 2) >> 2), 0, 255);
                } else
                    residual[off + x] = clip(residual[off + x]
                            + ((topLine[mbOffX + blkX + 0] + 2 * topLeft[blkY>>2] + leftRow[blkY] + 2) >> 2), 0, 255);
            }
            off += 16;
        }
    }

    public static void predictVerticalRight(int[] residual, boolean leftAvailable, boolean topAvailable, int[] leftRow,
            int[] topLine, int[] topLeft, int mbOffX, int blkX, int blkY) {

        int v1 = (topLeft[blkY >> 2] + topLine[mbOffX + blkX + 0] + 1) >> 1;
        int v2 = (topLine[mbOffX + blkX + 0] + topLine[mbOffX + blkX + 1] + 1) >> 1;
        int v3 = (topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 1) >> 1;
        int v4 = (topLine[mbOffX + blkX + 2] + topLine[mbOffX + blkX + 3] + 1) >> 1;
        int v5 = (leftRow[blkY] + 2 * topLeft[blkY >> 2] + topLine[mbOffX + blkX + 0] + 2) >> 2;
        int v6 = (topLeft[blkY >> 2] + 2 * topLine[mbOffX + blkX + 0] + topLine[mbOffX + blkX + 1] + 2) >> 2;
        int v7 = (topLine[mbOffX + blkX + 0] + 2 * topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 2) >> 2;
        int v8 = (topLine[mbOffX + blkX + 1] + 2 * topLine[mbOffX + blkX + 2] + topLine[mbOffX + blkX + 3] + 2) >> 2;
        int v9 = (topLeft[blkY >> 2] + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2;
        int v10 = (leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2) >> 2;

        int off = (blkY << 4) + blkX;
        residual[off] = clip(residual[off] + v1, 0, 255);
        residual[off + 1] = clip(residual[off + 1] + v2, 0, 255);
        residual[off + 2] = clip(residual[off + 2] + v3, 0, 255);
        residual[off + 3] = clip(residual[off + 3] + v4, 0, 255);
        residual[off + 16] = clip(residual[off + 16] + v5, 0, 255);
        residual[off + 17] = clip(residual[off + 17] + v6, 0, 255);
        residual[off + 18] = clip(residual[off + 18] + v7, 0, 255);
        residual[off + 19] = clip(residual[off + 19] + v8, 0, 255);
        residual[off + 32] = clip(residual[off + 32] + v9, 0, 255);
        residual[off + 33] = clip(residual[off + 33] + v1, 0, 255);
        residual[off + 34] = clip(residual[off + 34] + v2, 0, 255);
        residual[off + 35] = clip(residual[off + 35] + v3, 0, 255);
        residual[off + 48] = clip(residual[off + 48] + v10, 0, 255);
        residual[off + 49] = clip(residual[off + 49] + v5, 0, 255);
        residual[off + 50] = clip(residual[off + 50] + v6, 0, 255);
        residual[off + 51] = clip(residual[off + 51] + v7, 0, 255);
    }

    public static void predictHorizontalDown(int[] residual, boolean leftAvailable, boolean topAvailable,
            int[] leftRow, int[] topLine, int[] topLeft, int mbOffX, int blkX, int blkY) {

        int c0 = (topLeft[blkY>>2] + leftRow[blkY] + 1) >> 1;
        int c1 = (leftRow[blkY] + 2 * topLeft[blkY>>2] + topLine[mbOffX + blkX + 0] + 2) >> 2;
        int c2 = (topLeft[blkY>>2] + 2 * topLine[mbOffX + blkX + 0] + topLine[mbOffX + blkX + 1] + 2) >> 2;
        int c3 = (topLine[mbOffX + blkX + 0] + 2 * topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 2) >> 2;
        int c4 = (leftRow[blkY] + leftRow[blkY + 1] + 1) >> 1;
        int c5 = (topLeft[blkY>>2] + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2;
        int c6 = (leftRow[blkY + 1] + leftRow[blkY + 2] + 1) >> 1;
        int c7 = (leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2) >> 2;
        int c8 = (leftRow[blkY + 2] + leftRow[blkY + 3] + 1) >> 1;
        int c9 = (leftRow[blkY + 1] + 2 * leftRow[blkY + 2] + leftRow[blkY + 3] + 2) >> 2;

        int off = (blkY << 4) + blkX;
        residual[off] = clip(residual[off] + c0, 0, 255);
        residual[off + 1] = clip(residual[off + 1] + c1, 0, 255);
        residual[off + 2] = clip(residual[off + 2] + c2, 0, 255);
        residual[off + 3] = clip(residual[off + 3] + c3, 0, 255);
        residual[off + 16] = clip(residual[off + 16] + c4, 0, 255);
        residual[off + 17] = clip(residual[off + 17] + c5, 0, 255);
        residual[off + 18] = clip(residual[off + 18] + c0, 0, 255);
        residual[off + 19] = clip(residual[off + 19] + c1, 0, 255);
        residual[off + 32] = clip(residual[off + 32] + c6, 0, 255);
        residual[off + 33] = clip(residual[off + 33] + c7, 0, 255);
        residual[off + 34] = clip(residual[off + 34] + c4, 0, 255);
        residual[off + 35] = clip(residual[off + 35] + c5, 0, 255);
        residual[off + 48] = clip(residual[off + 48] + c8, 0, 255);
        residual[off + 49] = clip(residual[off + 49] + c9, 0, 255);
        residual[off + 50] = clip(residual[off + 50] + c6, 0, 255);
        residual[off + 51] = clip(residual[off + 51] + c7, 0, 255);
    }

    public static void predictVerticalLeft(int[] residual, boolean topAvailable,
            boolean topRightAvailable, int[] topLine, int mbOffX, int blkX, int blkY) {
        
        int to = mbOffX + blkX;
        int tr0 = topLine[to+3],
        tr1 = topLine[to+3],
        tr2 = topLine[to+3];
        if(topRightAvailable) {
            tr0 = topLine[to+4];
            tr1 = topLine[to+5];
            tr2 = topLine[to+6];
        }
        
        int c0 = ((topLine[to] + topLine[to+1] + 1) >> 1);
        int c1 = ((topLine[to+1] + topLine[to+2] + 1) >> 1);
        int c2 = ((topLine[to+2] + topLine[to+3] + 1) >> 1);
        int c3 = ((topLine[to+3] + tr0 + 1) >> 1);
        int c4 = ((tr0 + tr1 + 1) >> 1);
        int c5 = ((topLine[to] + 2*topLine[to+1] + topLine[to+2] + 2) >> 2);
        int c6 = ((topLine[to+1] + 2*topLine[to+2] + topLine[to+3] + 2) >> 2);
        int c7 = ((topLine[to+2] + 2*topLine[to+3] + tr0 + 2) >> 2);
        int c8 = ((topLine[to+3] + 2*tr0 + tr1 + 2) >> 2);
        int c9 = ((tr0 + 2*tr1 + tr2 + 2) >> 2);
        
        int off = (blkY << 4) + blkX;
        residual[off] = clip(residual[off] + c0, 0, 255);
        residual[off+1] = clip(residual[off+1] + c1, 0, 255);
        residual[off+2] = clip(residual[off+2] + c2, 0, 255);
        residual[off+3] = clip(residual[off+3] + c3, 0, 255);
        
        residual[off+16] = clip(residual[off+16] + c5, 0, 255);
        residual[off+17] = clip(residual[off+17] + c6, 0, 255);
        residual[off+18] = clip(residual[off+18] + c7, 0, 255);
        residual[off+19] = clip(residual[off+19] + c8, 0, 255);
        
        residual[off+32] = clip(residual[off+32] + c1, 0, 255);
        residual[off+33] = clip(residual[off+33] + c2, 0, 255);
        residual[off+34] = clip(residual[off+34] + c3, 0, 255);
        residual[off+35] = clip(residual[off+35] + c4, 0, 255);
        
        residual[off+48] = clip(residual[off+48] + c6, 0, 255);
        residual[off+49] = clip(residual[off+49] + c7, 0, 255);
        residual[off+50] = clip(residual[off+50] + c8, 0, 255);
        residual[off+51] = clip(residual[off+51] + c9, 0, 255);
    }

    public static void predictHorizontalUp(int[] residual, boolean leftAvailable, int[] leftRow,
            int mbOffX, int blkX, int blkY) {
        
        int c0 = ((leftRow[blkY] + leftRow[blkY+1] + 1) >> 1);
        int c1 = ((leftRow[blkY] + (leftRow[blkY+1] << 1) + leftRow[blkY+2] + 2) >> 2);
        int c2 = ((leftRow[blkY+1] + leftRow[blkY+2] + 1) >> 1);
        int c3 = ((leftRow[blkY+1] + (leftRow[blkY+2] << 1) + leftRow[blkY+3] + 2) >> 2);
        int c4 = ((leftRow[blkY+2] + leftRow[blkY+3] + 1) >> 1);
        int c5 = ((leftRow[blkY+2] + (leftRow[blkY+3] << 1) + leftRow[blkY+3] + 2) >> 2);
        int c6 = leftRow[blkY+3];
        
        int off = (blkY << 4) + blkX;
        residual[off] = clip(residual[off] + c0, 0, 255);
        residual[off+1] = clip(residual[off+1] + c1, 0, 255);
        residual[off+2] = clip(residual[off+2] + c2, 0, 255);
        residual[off+3] = clip(residual[off+3] + c3, 0, 255);
        
        residual[off+16] = clip(residual[off+16] + c2, 0, 255);
        residual[off+17] = clip(residual[off+17] + c3, 0, 255);
        residual[off+18] = clip(residual[off+18] + c4, 0, 255);
        residual[off+19] = clip(residual[off+19] + c5, 0, 255);
        
        residual[off+32] = clip(residual[off+32] + c4, 0, 255);
        residual[off+33] = clip(residual[off+33] + c5, 0, 255);
        residual[off+34] = clip(residual[off+34] + c6, 0, 255);
        residual[off+35] = clip(residual[off+35] + c6, 0, 255);
        
        residual[off+48] = clip(residual[off+48] + c6, 0, 255);
        residual[off+49] = clip(residual[off+49] + c6, 0, 255);
        residual[off+50] = clip(residual[off+50] + c6, 0, 255);
        residual[off+51] = clip(residual[off+51] + c6, 0, 255);
    }
}