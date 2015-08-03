package org.jcodec.codecs.h264.encode;

import static java.lang.Math.abs;

import org.jcodec.codecs.h264.decode.deblock.DeblockingFilter;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * Contains various deblocking filter routines for deblocking on MB bases
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class MBDeblocker {

    private static int[][] P_POS_V = buildPPosV();
    private static int[][] Q_POS_V = buildQPosV();
    private static int[][] P_POS_H = buildPPosH();
    private static int[][] Q_POS_H = buildQPosH();

    private static int[][] P_POS_V_CHR = { { 0, 8, 16, 24, 32, 40, 48, 56 }, { 4, 12, 20, 28, 36, 44, 52, 60 } };
    private static int[][] Q_POS_V_CHR = { { 3, 11, 19, 27, 35, 43, 51, 59 }, { 7, 15, 23, 31, 39, 47, 55, 63 } };

    private static int[][] P_POS_H_CHR = { { 0, 1, 2, 3, 4, 5, 6, 7 }, { 32, 33, 34, 35, 36, 37, 38, 39 } };
    private static int[][] Q_POS_H_CHR = { { 24, 25, 26, 27, 28, 29, 30, 31 }, { 56, 57, 58, 59, 60, 61, 62, 63 } };

    private static int[][] LOOKUP_IDX_P_V = new int[][] { { 3, 7, 11, 15 }, { 0, 4, 8, 12 }, { 1, 5, 9, 13 },
            { 2, 6, 10, 14 } };
    private static int[][] LOOKUP_IDX_Q_V = new int[][] { { 0, 4, 8, 12 }, { 1, 5, 9, 13 }, { 2, 6, 10, 14 },
            { 3, 7, 11, 15 } };
    private static int[][] LOOKUP_IDX_P_H = new int[][] { { 12, 13, 14, 15 }, { 0, 1, 2, 3 }, { 4, 5, 6, 7 },
            { 8, 9, 10, 11 } };
    private static int[][] LOOKUP_IDX_Q_H = new int[][] { { 0, 1, 2, 3 }, { 4, 5, 6, 7 }, { 8, 9, 10, 11 },
            { 12, 13, 14, 15 } };

    // Luma Chroma
    // 4444 44
    // 4333 43
    // 4333
    // 4333
    private static int[][] BS_I = { { 4, 4, 4, 4 }, { 3, 3, 3, 3 }, { 3, 3, 3, 3 }, { 3, 3, 3, 3 } };

    /**
     * Deblocks bottom edge of topOutMB, right edge of leftOutMB and left/top
     * and inner block edges of outMB
     * 
     * @param curPix
     *            Pixels of the current MB
     * @param leftPix
     *            Pixels of the leftMB
     * @param topPix
     *            Pixels of the tipMB
     * 
     * @param vertStrength
     *            Border strengths for vertical edges (filtered first)
     * @param horizStrength
     *            Border strengths for the horizontal edges
     * 
     * @param curQp
     *            Current MB's qp
     * @param leftQp
     *            Left MB's qp
     * @param topQp
     *            Top MB's qp
     */
    public static void deblockMBGeneric(EncodedMB curMB, EncodedMB leftMB, EncodedMB topMB, int[][] vertStrength,
            int horizStrength[][]) {
        Picture curPix = curMB.getPixels();
        if (leftMB != null) {
            Picture leftPix = leftMB.getPixels();
            int avgQp = MathUtil.clip((leftMB.getQp() + curMB.getQp() + 1) >> 1, 0, 51);
            deblockBorderVertical(vertStrength[0], avgQp, leftPix.getPlaneData(0), 3, curPix.getPlaneData(0), 0,
                    P_POS_V, Q_POS_V);
            deblockBorderVerticalChroma(vertStrength[0], avgQp, leftPix.getPlaneData(1), 1, curPix.getPlaneData(1), 0,
                    P_POS_V_CHR, Q_POS_V_CHR);
            deblockBorderVerticalChroma(vertStrength[0], avgQp, leftPix.getPlaneData(2), 1, curPix.getPlaneData(2), 0,
                    P_POS_V_CHR, Q_POS_V_CHR);
        }
        for (int i = 0; i < 3; i++) {
            deblockBorderVertical(vertStrength[i + 1], curMB.getQp(), curPix.getPlaneData(0), i,
                    curPix.getPlaneData(0), i + 1, P_POS_V, Q_POS_V);
        }
        deblockBorderVerticalChroma(vertStrength[2], curMB.getQp(), curPix.getPlaneData(1), 0, curPix.getPlaneData(1),
                1, P_POS_V_CHR, Q_POS_V_CHR);
        deblockBorderVerticalChroma(vertStrength[2], curMB.getQp(), curPix.getPlaneData(2), 0, curPix.getPlaneData(2),
                1, P_POS_V_CHR, Q_POS_V_CHR);

        if (topMB != null) {
            Picture topPix = topMB.getPixels();
            int avgQp = MathUtil.clip((topMB.getQp() + curMB.getQp() + 1) >> 1, 0, 51);
            deblockBorderVertical(horizStrength[0], avgQp, topPix.getPlaneData(0), 3, curPix.getPlaneData(0), 0,
                    P_POS_H, Q_POS_H);
            deblockBorderVerticalChroma(horizStrength[0], avgQp, topPix.getPlaneData(1), 1, curPix.getPlaneData(1), 0,
                    P_POS_H_CHR, Q_POS_H_CHR);
            deblockBorderVerticalChroma(horizStrength[0], avgQp, topPix.getPlaneData(2), 1, curPix.getPlaneData(2), 0,
                    P_POS_H_CHR, Q_POS_H_CHR);
        }
        for (int i = 0; i < 3; i++) {
            deblockBorderVertical(horizStrength[i + 1], curMB.getQp(), curPix.getPlaneData(0), i,
                    curPix.getPlaneData(0), i + 1, P_POS_H, Q_POS_H);
        }
        deblockBorderVerticalChroma(horizStrength[2], curMB.getQp(), curPix.getPlaneData(1), 0, curPix.getPlaneData(1),
                1, P_POS_H_CHR, Q_POS_H_CHR);
        deblockBorderVerticalChroma(horizStrength[2], curMB.getQp(), curPix.getPlaneData(2), 0, curPix.getPlaneData(2),
                1, P_POS_H_CHR, Q_POS_H_CHR);
    }

    public static void deblockMBI(EncodedMB outMB, EncodedMB leftOutMB, EncodedMB topOutMB) {
        MBDeblocker.deblockMBGeneric(outMB, leftOutMB, topOutMB, BS_I, BS_I);
    }

    /**
     * Deblocks P-macroblock
     * 
     * @param curMB
     *            Pixels and parameters of encoded and reconstructed current
     *            macroblock
     * @param leftOutMB
     *            Pixels and parameters of encoded and reconstructed left
     *            macroblock
     * @param topOutMB
     *            Pixels and parameters of encoded and reconstructed top
     *            macroblock
     */
    public static void deblockMBP(EncodedMB curMB, EncodedMB leftOutMB, EncodedMB topOutMB) {
        int[][] vertStrength = new int[4][4];
        int[][] horizStrength = new int[4][4];

        calcStrengthForBlocks(curMB, topOutMB, horizStrength, LOOKUP_IDX_P_V, LOOKUP_IDX_Q_V);
        calcStrengthForBlocks(curMB, topOutMB, horizStrength, LOOKUP_IDX_P_H, LOOKUP_IDX_Q_H);

        MBDeblocker.deblockMBGeneric(curMB, leftOutMB, topOutMB, vertStrength, horizStrength);
    }

    private static void deblockBorderVertical(int[] boundary, int qp, int[] q, int qi, int[] p, int pi, int[][] pTab,
            int[][] qTab) {
        for (int b = 0; b < 4; b++) {
            if (boundary[b] == 4) {
                for (int i = 0, ii = b << 2; i < 4; ++i, ++ii)
                    DeblockingFilter.filterBs4(qp, qp, q, pTab[qi][ii] + 3, pTab[qi][ii] + 2, pTab[qi][ii] + 1,
                            pTab[qi][ii], qTab[qi][ii], qTab[qi][ii] - 1, qTab[qi][ii] - 2, qTab[qi][ii] - 3, false);
            } else if (boundary[b] > 0) {
                for (int i = 0, ii = b << 2; i < 4; ++i, ++ii)
                    DeblockingFilter.filterBs(boundary[b], qp, qp, q, pTab[qi][ii] + 2, pTab[qi][ii] + 1, pTab[qi][ii],
                            qTab[qi][ii], qTab[qi][ii] - 1, qTab[qi][ii] - 2, false);

            }
        }
    }

    private static void deblockBorderVerticalChroma(int[] boundary, int qp, int[] q, int qi, int[] p, int pi,
            int[][] pTab, int[][] qTab) {
        for (int b = 0; b < 2; b++) {
            if (boundary[b] == 4) {
                for (int i = 0, ii = b << 2; i < 4; ++i, ++ii)
                    DeblockingFilter.filterBs4(qp, qp, q, pTab[qi][ii] + 3, pTab[qi][ii] + 2, pTab[qi][ii] + 1,
                            pTab[qi][ii], qTab[qi][ii], qTab[qi][ii] - 1, qTab[qi][ii] - 2, qTab[qi][ii] - 3, true);
            } else if (boundary[b] > 0) {
                for (int i = 0, ii = b << 2; i < 4; ++i, ++ii)
                    DeblockingFilter.filterBs(boundary[b], qp, qp, q, pTab[qi][ii] + 2, pTab[qi][ii] + 1, pTab[qi][ii],
                            qTab[qi][ii], qTab[qi][ii] - 1, qTab[qi][ii] - 2, true);
            }
        }
    }

    private static int[][] buildPPosH() {
        int[][] pPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                pPos[i][j] = j + (i << 6);
            }
        }
        return pPos;
    }

    private static int[][] buildQPosH() {
        int[][] qPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                qPos[i][j] = j + (i << 6) + 48;
            }
        }
        return qPos;
    }

    private static int[][] buildPPosV() {
        int[][] pPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                pPos[i][j] = (j << 4) + (i << 2);
            }
        }
        return pPos;
    }

    private static int[][] buildQPosV() {
        int[][] qPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                qPos[i][j] = (j << 4) + (i << 2) + 3;
            }
        }
        return qPos;
    }

    private static void calcStrengthForBlocks(EncodedMB mbThis, EncodedMB mbOthe, int[][] outStrength,
            int[][] LOOKUP_IDX_P, int[][] LOOKUP_IDX_Q) {
        if (mbOthe != null) {
            for (int i = 0; i < 4; ++i) {
                outStrength[i][0] = MathUtil.max3(
                        strengthMv(mbOthe.getMx()[LOOKUP_IDX_P[0][i]], mbThis.getMx()[LOOKUP_IDX_Q[0][i]]),
                        strengthMv(mbOthe.getMy()[LOOKUP_IDX_P[0][i]], mbThis.getMy()[LOOKUP_IDX_Q[0][i]]),
                        strengthNc(mbOthe.getNc()[LOOKUP_IDX_P[0][i]], mbThis.getNc()[LOOKUP_IDX_Q[0][i]]));
            }
        }

        for (int i = 1; i < 4; i++) {
            for (int j = 0; j < 4; ++j) {
                outStrength[j][i] = MathUtil.max3(
                        strengthMv(mbThis.getMx()[LOOKUP_IDX_P[i][j]], mbThis.getMx()[LOOKUP_IDX_Q[i][j]]),
                        strengthMv(mbThis.getMy()[LOOKUP_IDX_P[i][j]], mbThis.getMy()[LOOKUP_IDX_Q[i][j]]),
                        strengthNc(mbThis.getNc()[LOOKUP_IDX_P[i][j]], mbThis.getNc()[LOOKUP_IDX_Q[i][j]]));
            }

        }
    }

    private static int strengthNc(int ncA, int ncB) {
        return ncA > 0 || ncB > 0 ? 2 : 0;
    }

    private static int strengthMv(int v0, int v1) {
        return abs(v0 - v1) >= 4 ? 1 : 0;
    }
}
