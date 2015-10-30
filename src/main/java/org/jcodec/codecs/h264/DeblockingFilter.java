package org.jcodec.codecs.h264;

import static java.lang.Math.abs;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Contains various deblocking filter routines for deblocking on MB bases
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class DeblockingFilter {

    public static int[] alphaTab = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 5, 6, 7, 8, 9, 10,
            12, 13, 15, 17, 20, 22, 25, 28, 32, 36, 40, 45, 50, 56, 63, 71, 80, 90, 101, 113, 127, 144, 162, 182, 203,
            226, 255, 255 };
    public static int[] betaTab = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 3, 3, 3, 3, 4,
            4, 4, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18 };

    public static int[][] tcs = new int[][] {
            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13 },

            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
                    2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 10, 11, 12, 13, 15, 17 },

            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3,
                    3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13, 14, 16, 18, 20, 23, 25 } };

    private static int[][] P_POS_V = buildPPosV();
    private static int[][] Q_POS_V = buildQPosV();
    private static int[][] P_POS_H = buildPPosH();
    private static int[][] Q_POS_H = buildQPosH();

    private static int[][] P_POS_V_CHR = buildPPosVChr();
    private static int[][] Q_POS_V_CHR = buildQPosVChr();

    private static int[][] P_POS_H_CHR = buildPPosHChr();
    private static int[][] Q_POS_H_CHR = buildQPosHChr();

    static int[][] LOOKUP_IDX_P_V = new int[][] { { 3, 7, 11, 15 }, { 0, 4, 8, 12 }, { 1, 5, 9, 13 }, { 2, 6, 10, 14 } };
    static int[][] LOOKUP_IDX_Q_V = new int[][] { { 0, 4, 8, 12 }, { 1, 5, 9, 13 }, { 2, 6, 10, 14 }, { 3, 7, 11, 15 } };
    static int[][] LOOKUP_IDX_P_H = new int[][] { { 12, 13, 14, 15 }, { 0, 1, 2, 3 }, { 4, 5, 6, 7 }, { 8, 9, 10, 11 } };
    static int[][] LOOKUP_IDX_Q_H = new int[][] { { 0, 1, 2, 3 }, { 4, 5, 6, 7 }, { 8, 9, 10, 11 }, { 12, 13, 14, 15 } };

    // Luma Chroma
    // 4444 44
    // 4333 43
    // 4333
    // 4333
    // private static int[][] BS_I = { { 4, 4, 4, 4 }, { 3, 3, 3, 3 }, { 3, 3,
    // 3, 3 }, { 3, 3, 3, 3 } };

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
    public void deblockMBGeneric(DecodedMBlock curMB, DecodedMBlock leftMB, DecodedMBlock topMB, int[][] vertStrength,
            int horizStrength[][]) {
//        System.out.println("===========================================================================");
//        System.out.println(String.format("BSV: %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d",
//                vertStrength[0][0], vertStrength[1][0], vertStrength[2][0], vertStrength[3][0], vertStrength[0][1],
//                vertStrength[1][1], vertStrength[2][1], vertStrength[3][1], vertStrength[0][2], vertStrength[1][2],
//                vertStrength[2][2], vertStrength[3][2], vertStrength[0][3], vertStrength[1][3], vertStrength[2][3],
//                vertStrength[3][3]));

//        System.out.println(String.format("BSH: %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d",
//                horizStrength[0][0], horizStrength[0][1], horizStrength[0][2], horizStrength[0][3],
//                horizStrength[1][0], horizStrength[1][1], horizStrength[1][2], horizStrength[1][3],
//                horizStrength[2][0], horizStrength[2][1], horizStrength[2][2], horizStrength[2][3],
//                horizStrength[3][0], horizStrength[3][1], horizStrength[3][2], horizStrength[3][3]));

        Picture8Bit curPix = curMB.getPixels();
        if (leftMB != null) {
            Picture8Bit leftPix = leftMB.getPixels();
            int avgQp0 = MathUtil.clip((leftMB.getQp(0) + curMB.getQp(0) + 1) >> 1, 0, 51);
            deblockBorder(vertStrength[0], getIdxAlpha(curMB.getAlphaC0Offset(), avgQp0),
                    getIdxBeta(curMB.getBetaOffset(), avgQp0), leftPix.getPlaneData(0), 3, curPix.getPlaneData(0), 0,
                    P_POS_V, Q_POS_V, false);
            int avgQp1 = MathUtil.clip((leftMB.getQp(1) + curMB.getQp(1) + 1) >> 1, 0, 51);
            deblockBorderChroma(vertStrength[0], getIdxAlpha(curMB.getAlphaC0Offset(), avgQp1),
                    getIdxBeta(curMB.getBetaOffset(), avgQp1), leftPix.getPlaneData(1), 1, curPix.getPlaneData(1), 0,
                    P_POS_V_CHR, Q_POS_V_CHR, false);
            int avgQp2 = MathUtil.clip((leftMB.getQp(2) + curMB.getQp(2) + 1) >> 1, 0, 51);
            deblockBorderChroma(vertStrength[0], getIdxAlpha(curMB.getAlphaC0Offset(), avgQp2),
                    getIdxBeta(curMB.getBetaOffset(), avgQp2), leftPix.getPlaneData(2), 1, curPix.getPlaneData(2), 0,
                    P_POS_V_CHR, Q_POS_V_CHR, false);
        }
        for (int i = 0; i < 3; i++) {
            deblockBorder(vertStrength[i + 1], getIdxAlpha(curMB.getAlphaC0Offset(), curMB.getQp(0)),
                    getIdxBeta(curMB.getBetaOffset(), curMB.getQp(0)), curPix.getPlaneData(0), i,
                    curPix.getPlaneData(0), i + 1, P_POS_V, Q_POS_V, false);
        }
        deblockBorderChroma(vertStrength[2], getIdxAlpha(curMB.getAlphaC0Offset(), curMB.getQp(1)),
                getIdxBeta(curMB.getBetaOffset(), curMB.getQp(1)), curPix.getPlaneData(1), 0, curPix.getPlaneData(1),
                1, P_POS_V_CHR, Q_POS_V_CHR, false);
        deblockBorderChroma(vertStrength[2], getIdxAlpha(curMB.getAlphaC0Offset(), curMB.getQp(2)),
                getIdxBeta(curMB.getBetaOffset(), curMB.getQp(2)), curPix.getPlaneData(2), 0, curPix.getPlaneData(2),
                1, P_POS_V_CHR, Q_POS_V_CHR, false);

        if (topMB != null) {
            Picture8Bit topPix = topMB.getPixels();
            int avgQp0 = MathUtil.clip((topMB.getQp(0) + curMB.getQp(0) + 1) >> 1, 0, 51);
            deblockBorder(horizStrength[0], getIdxAlpha(curMB.getAlphaC0Offset(), avgQp0),
                    getIdxBeta(curMB.getBetaOffset(), avgQp0), topPix.getPlaneData(0), 3, curPix.getPlaneData(0), 0,
                    P_POS_H, Q_POS_H, true);
            int avgQp1 = MathUtil.clip((topMB.getQp(1) + curMB.getQp(1) + 1) >> 1, 0, 51);
            deblockBorderChroma(horizStrength[0], getIdxAlpha(curMB.getAlphaC0Offset(), avgQp1),
                    getIdxBeta(curMB.getBetaOffset(), avgQp1), topPix.getPlaneData(1), 1, curPix.getPlaneData(1), 0,
                    P_POS_H_CHR, Q_POS_H_CHR, true);
            int avgQp2 = MathUtil.clip((topMB.getQp(2) + curMB.getQp(2) + 1) >> 1, 0, 51);
            deblockBorderChroma(horizStrength[0], getIdxAlpha(curMB.getAlphaC0Offset(), avgQp2),
                    getIdxBeta(curMB.getBetaOffset(), avgQp2), topPix.getPlaneData(2), 1, curPix.getPlaneData(2), 0,
                    P_POS_H_CHR, Q_POS_H_CHR, true);
        }
        for (int i = 0; i < 3; i++) {
            deblockBorder(horizStrength[i + 1], getIdxAlpha(curMB.getAlphaC0Offset(), curMB.getQp(0)),
                    getIdxBeta(curMB.getBetaOffset(), curMB.getQp(0)), curPix.getPlaneData(0), i,
                    curPix.getPlaneData(0), i + 1, P_POS_H, Q_POS_H, true);
        }
        deblockBorderChroma(horizStrength[2], getIdxAlpha(curMB.getAlphaC0Offset(), curMB.getQp(1)),
                getIdxBeta(curMB.getBetaOffset(), curMB.getQp(1)), curPix.getPlaneData(1), 0, curPix.getPlaneData(1),
                1, P_POS_H_CHR, Q_POS_H_CHR, true);
        deblockBorderChroma(horizStrength[2], getIdxAlpha(curMB.getAlphaC0Offset(), curMB.getQp(2)),
                getIdxBeta(curMB.getBetaOffset(), curMB.getQp(2)), curPix.getPlaneData(2), 0, curPix.getPlaneData(2),
                1, P_POS_H_CHR, Q_POS_H_CHR, true);
    }

    // public void deblockMbI(DecodedMBlock outMB, DecodedMBlock leftOutMB,
    // DecodedMBlock topOutMB) {
    // deblockMBGeneric(outMB, leftOutMB, topOutMB, BS_I, BS_I);
    // }

    /**
     * Deblocks P-macroblock
     * 
     * @param cur
     *            Pixels and parameters of encoded and reconstructed current
     *            macroblock
     * @param left
     *            Pixels and parameters of encoded and reconstructed left
     *            macroblock
     * @param top
     *            Pixels and parameters of encoded and reconstructed top
     *            macroblock
     */
    public void deblockMbP(DecodedMBlock cur, DecodedMBlock left, DecodedMBlock top) {
        int[][] vertStrength = new int[4][4];
        int[][] horizStrength = new int[4][4];

        calcStrengthForBlocks(cur, left, vertStrength, LOOKUP_IDX_P_V, LOOKUP_IDX_Q_V);
        calcStrengthForBlocks(cur, top, horizStrength, LOOKUP_IDX_P_H, LOOKUP_IDX_Q_H);

        deblockMBGeneric(cur, left, top, vertStrength, horizStrength);
    }

    private void deblockBorder(int[] boundary, int idxAlpha, int idxBeta, byte[] p, int pi, byte[] q, int qi,
            int[][] pTab, int[][] qTab, boolean horiz) {
        int inc1 = horiz ? 16 : 1, inc2 = inc1 * 2, inc3 = inc1 * 3;
        for (int b = 0; b < 4; b++) {
            if (boundary[b] == 4) {
                for (int i = 0, ii = b << 2; i < 4; ++i, ++ii)
                    filterBs4(idxAlpha, idxBeta, p, q, pTab[pi][ii] - inc3, pTab[pi][ii] - inc2, pTab[pi][ii] - inc1,
                            pTab[pi][ii], qTab[qi][ii], qTab[qi][ii] + inc1, qTab[qi][ii] + inc2, qTab[qi][ii] + inc3);
            } else if (boundary[b] > 0) {
                for (int i = 0, ii = b << 2; i < 4; ++i, ++ii)
                    filterBs(boundary[b], idxAlpha, idxBeta, p, q, pTab[pi][ii] - inc2, pTab[pi][ii] - inc1,
                            pTab[pi][ii], qTab[qi][ii], qTab[qi][ii] + inc1, qTab[qi][ii] + inc2);

            }
        }
    }

    protected void filterBs4Chr(int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p1Idx, int p0Idx,
            int q0Idx, int q1Idx) {
        filterBs4(indexAlpha, indexBeta, pelsP, pelsQ, 0, 0, p1Idx, p0Idx, q0Idx, q1Idx, 0, 0, true);
    }

    protected void filterBsChr(int bs, int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p1Idx, int p0Idx,
            int q0Idx, int q1Idx) {
        filterBs(bs, indexAlpha, indexBeta, pelsP, pelsQ, 0, p1Idx, p0Idx, q0Idx, q1Idx, 0, true);
    }

    protected void filterBs4(int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p3Idx, int p2Idx,
            int p1Idx, int p0Idx, int q0Idx, int q1Idx, int q2Idx, int q3Idx) {
        filterBs4(indexAlpha, indexBeta, pelsP, pelsQ, p3Idx, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, q3Idx, false);
    }

    protected void filterBs(int bs, int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p2Idx, int p1Idx,
            int p0Idx, int q0Idx, int q1Idx, int q2Idx) {
        filterBs(bs, indexAlpha, indexBeta, pelsP, pelsQ, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, false);
    }

    protected void filterBs4(int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p3Idx, int p2Idx,
            int p1Idx, int p0Idx, int q0Idx, int q1Idx, int q2Idx, int q3Idx, boolean isChroma) {
//        System.out
//                .println(String
//                        .format("filterBs(indexAlpha=%d, indexBeta=%d, p3Idx=%d, p2Idx=%d, p1Idx=%d, p0Idx=%d, q0Idx=%d, q1Idx=%d, q2Idx=%d, q3Idx=%d)",
//                                indexAlpha, indexBeta, p3Idx, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, q3Idx));

//        System.out.println(String.format("IN: %d, %d, %d, %d, %d, %d, %d, %d", (pelsP[p3Idx] + 128),
//                (pelsP[p2Idx] + 128), (pelsP[p1Idx] + 128), (pelsP[p0Idx] + 128), (pelsQ[q0Idx] + 128),
//                (pelsQ[q1Idx] + 128), (pelsQ[q2Idx] + 128), (pelsQ[q3Idx] + 128)));

        int p0 = pelsP[p0Idx];
        int q0 = pelsQ[q0Idx];
        int p1 = pelsP[p1Idx];
        int q1 = pelsQ[q1Idx];

        int alphaThresh = alphaTab[indexAlpha];
        int betaThresh = betaTab[indexBeta];

        boolean filterEnabled = abs(p0 - q0) < alphaThresh && abs(p1 - p0) < betaThresh && abs(q1 - q0) < betaThresh;

        if (!filterEnabled)
            return;

        boolean conditionP, conditionQ;

        if (isChroma) {
            conditionP = false;
            conditionQ = false;
        } else {
            int ap = abs(pelsP[p2Idx] - p0);
            int aq = abs(pelsQ[q2Idx] - q0);

            conditionP = ap < betaThresh && abs(p0 - q0) < ((alphaThresh >> 2) + 2);
            conditionQ = aq < betaThresh && abs(p0 - q0) < ((alphaThresh >> 2) + 2);

        }

        if (conditionP) {
            int p3 = pelsP[p3Idx];
            int p2 = pelsP[p2Idx];

            int p0n = (p2 + 2 * p1 + 2 * p0 + 2 * q0 + q1 + 4) >> 3;
            int p1n = (p2 + p1 + p0 + q0 + 2) >> 2;
            int p2n = (2 * p3 + 3 * p2 + p1 + p0 + q0 + 4) >> 3;
            pelsP[p0Idx] = (byte) clip(p0n, -128, 127);
            pelsP[p1Idx] = (byte) clip(p1n, -128, 127);
            pelsP[p2Idx] = (byte) clip(p2n, -128, 127);
        } else {
            int p0n = (2 * p1 + p0 + q1 + 2) >> 2;
            pelsP[p0Idx] = (byte) clip(p0n, -128, 127);
        }

        if (conditionQ && !isChroma) {
            int q2 = pelsQ[q2Idx];
            int q3 = pelsQ[q3Idx];
            int q0n = (p1 + 2 * p0 + 2 * q0 + 2 * q1 + q2 + 4) >> 3;
            int q1n = (p0 + q0 + q1 + q2 + 2) >> 2;
            int q2n = (2 * q3 + 3 * q2 + q1 + q0 + p0 + 4) >> 3;
            pelsQ[q0Idx] = (byte) clip(q0n, -128, 127);
            pelsQ[q1Idx] = (byte) clip(q1n, -128, 127);
            pelsQ[q2Idx] = (byte) clip(q2n, -128, 127);
        } else {
            int q0n = (2 * q1 + q0 + p1 + 2) >> 2;
            pelsQ[q0Idx] = (byte) clip(q0n, -128, 127);
        }

//        System.out.println(String.format("OUT: %d, %d, %d, %d, %d, %d, %d, %d", (pelsP[p3Idx] + 128),
//                (pelsP[p2Idx] + 128), (pelsP[p1Idx] + 128), (pelsP[p0Idx] + 128), (pelsQ[q0Idx] + 128),
//                (pelsQ[q1Idx] + 128), (pelsQ[q2Idx] + 128), (pelsQ[q3Idx] + 128)));
    }

    protected void filterBs(int bs, int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p2Idx, int p1Idx,
            int p0Idx, int q0Idx, int q1Idx, int q2Idx, boolean isChroma) {
//        System.out
//                .println(String
//                        .format("filterBs(bs=%d, indexAlpha=%d, indexBeta=%d, p2Idx=%d, p1Idx=%d, p0Idx=%d, q0Idx=%d, q1Idx=%d, q2Idx=%d)",
//                                bs, indexAlpha, indexBeta, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx));
//        System.out.println(String.format("IN: %d, %d, %d, %d, %d, %d", (pelsP[p2Idx] + 128), (pelsP[p1Idx] + 128),
//                (pelsP[p0Idx] + 128), (pelsQ[q0Idx] + 128), (pelsQ[q1Idx] + 128), (pelsQ[q2Idx] + 128)));

        int p1 = pelsP[p1Idx];
        int p0 = pelsP[p0Idx];
        int q0 = pelsQ[q0Idx];
        int q1 = pelsQ[q1Idx];

        int alphaThresh = alphaTab[indexAlpha];
        int betaThresh = betaTab[indexBeta];

        boolean filterEnabled = abs(p0 - q0) < alphaThresh && abs(p1 - p0) < betaThresh && abs(q1 - q0) < betaThresh;

        if (!filterEnabled)
            return;

        int tC0 = tcs[bs - 1][indexAlpha];

        boolean conditionP, conditionQ;
        int tC;
        if (!isChroma) {
            int ap = abs(pelsP[p2Idx] - p0);
            int aq = abs(pelsQ[q2Idx] - q0);
            tC = tC0 + ((ap < betaThresh) ? 1 : 0) + ((aq < betaThresh) ? 1 : 0);
            conditionP = ap < betaThresh;
            conditionQ = aq < betaThresh;
        } else {
            tC = tC0 + 1;
            conditionP = false;
            conditionQ = false;
        }

        int sigma = ((((q0 - p0) << 2) + (p1 - q1) + 4) >> 3);
        sigma = sigma < -tC ? -tC : (sigma > tC ? tC : sigma);

        int p0n = p0 + sigma;
        p0n = p0n < -128 ? -128 : p0n;
        int q0n = q0 - sigma;
        q0n = q0n < -128 ? -128 : q0n;

        if (conditionP) {
            int p2 = pelsP[p2Idx];

            int diff = (p2 + ((p0 + q0 + 1) >> 1) - (p1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : (diff > tC0 ? tC0 : diff);
            int p1n = p1 + diff;
            pelsP[p1Idx] = (byte) clip(p1n, -128, 127);
        }

        if (conditionQ) {
            int q2 = pelsQ[q2Idx];
            int diff = (q2 + ((p0 + q0 + 1) >> 1) - (q1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : (diff > tC0 ? tC0 : diff);
            int q1n = q1 + diff;
            pelsQ[q1Idx] = (byte) clip(q1n, -128, 127);
        }

        pelsQ[q0Idx] = (byte) clip(q0n, -128, 127);
        pelsP[p0Idx] = (byte) clip(p0n, -128, 127);

//        System.out.println(String.format("OUT: %d, %d, %d, %d, %d, %d", (pelsP[p2Idx] + 128), (pelsP[p1Idx] + 128),
//                (pelsP[p0Idx] + 128), (pelsQ[q0Idx] + 128), (pelsQ[q1Idx] + 128), (pelsQ[q2Idx] + 128)));
    }

    private void deblockBorderChroma(int[] boundary, int idxAlpha, int idxBeta, byte[] p, int pi, byte[] q, int qi,
            int[][] pTab, int[][] qTab, boolean horiz) {
        int inc1 = horiz ? 8 : 1;
        for (int b = 0; b < 4; b++) {
            if (boundary[b] == 4) {
                for (int i = 0, ii = b << 1; i < 2; ++i, ++ii)
                    filterBs4Chr(idxAlpha, idxBeta, p, q, pTab[pi][ii] - inc1, pTab[pi][ii], qTab[qi][ii], qTab[qi][ii]
                            + inc1);
            } else if (boundary[b] > 0) {
                for (int i = 0, ii = b << 1; i < 2; ++i, ++ii)
                    filterBsChr(boundary[b], idxAlpha, idxBeta, p, q, pTab[pi][ii] - inc1, pTab[pi][ii], qTab[qi][ii],
                            qTab[qi][ii] + inc1);
            }
        }
    }

    private static int[][] buildPPosH() {
        int[][] qPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                qPos[i][j] = j + (i << 6) + 48;
            }
        }
        return qPos;
    }

    private static int[][] buildQPosH() {
        int[][] pPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                pPos[i][j] = j + (i << 6);
            }
        }
        return pPos;
    }

    private static int[][] buildPPosV() {
        int[][] pPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                pPos[i][j] = (j << 4) + (i << 2) + 3;
            }
        }
        return pPos;
    }

    private static int[][] buildQPosV() {
        int[][] qPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                qPos[i][j] = (j << 4) + (i << 2);
            }
        }
        return qPos;
    }

    private static int[][] buildPPosHChr() {
        int[][] pPos = new int[2][8];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                pPos[i][j] = j + (i << 5) + 24;
            }
        }
        return pPos;
    }

    private static int[][] buildQPosHChr() {
        int[][] qPos = new int[2][8];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                qPos[i][j] = j + (i << 5);
            }
        }
        return qPos;
    }

    private static int[][] buildPPosVChr() {
        int[][] pPos = new int[2][8];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                pPos[i][j] = (j << 3) + (i << 2) + 3;
            }
        }
        return pPos;
    }

    private static int[][] buildQPosVChr() {
        int[][] qPos = new int[2][8];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                qPos[i][j] = (j << 3) + (i << 2);
            }
        }
        return qPos;
    }

    static void calcStrengthForBlocks(DecodedMBlock cur, DecodedMBlock other, int[][] outStrength,
            int[][] LOOKUP_IDX_P, int[][] LOOKUP_IDX_Q) {
        if (other != null) {
            for (int i = 0; i < 4; ++i) {
                outStrength[0][i] = strengthIntra(other, cur) ? 4 : (strengthNc(other.getNc()[LOOKUP_IDX_P[0][i]],
                        cur.getNc()[LOOKUP_IDX_Q[0][i]]) ? 2 : strengthMvBased(other, LOOKUP_IDX_P[0][i], cur,
                        LOOKUP_IDX_Q[0][i]));
            }
        }

        for (int i = 1; i < 4; i++) {
            for (int j = 0; j < 4; ++j) {
                outStrength[i][j] = strengthIntra(cur) ? 3 : (strengthNc(cur.getNc()[LOOKUP_IDX_P[i][j]],
                        cur.getNc()[LOOKUP_IDX_Q[i][j]]) ? 2 : strengthMvBased(cur, LOOKUP_IDX_P[i][j], cur,
                        LOOKUP_IDX_Q[i][j]));
            }

        }
    }

    private static int strengthMvBased(DecodedMBlock other, int iOther, DecodedMBlock cur, int iCur) {
        int otherPOCL0 = other.getRefPOCL0()[iOther];
        int otherPOCL1 = other.getRefPOCL1()[iOther];
        int curPOCL0 = cur.getRefPOCL0()[iCur];
        int curPOCL1 = cur.getRefPOCL1()[iCur];

        int refCountOther = (otherPOCL0 == Frame.MVS.POC_NO_REFERENCE ? 0 : 1)
                + (otherPOCL1 == Frame.MVS.POC_NO_REFERENCE ? 0 : 1);
        int refCountCur = (curPOCL0 == Frame.MVS.POC_NO_REFERENCE ? 0 : 1)
                + (curPOCL1 == Frame.MVS.POC_NO_REFERENCE ? 0 : 1);
        if (refCountOther != refCountCur)
            return 1;

        boolean a = otherPOCL0 != curPOCL0;
        boolean b = otherPOCL0 != curPOCL1;
        boolean c = otherPOCL1 != curPOCL0;
        boolean d = otherPOCL1 != curPOCL1;

        if (a && b || c && d || a && c || b && d)
            return 1;

        int mxA0 = other.getMxL0()[iOther];
        int mxA1 = other.getMxL1()[iOther];
        int mxB0 = cur.getMxL0()[iCur];
        int mxB1 = cur.getMxL1()[iCur];

        int myA0 = other.getMyL0()[iOther];
        int myA1 = other.getMyL1()[iOther];
        int myB0 = cur.getMyL0()[iCur];
        int myB1 = cur.getMyL1()[iCur];

        if (otherPOCL0 == otherPOCL1 && otherPOCL1 == curPOCL0 && curPOCL0 == curPOCL1) {
            return otherPOCL0 != Frame.MVS.POC_NO_REFERENCE ? MathUtil.max8(strengthMv(mxA0, mxB0),
                    strengthMv(mxA1, mxB0), strengthMv(mxA0, mxB1), strengthMv(mxA1, mxB1), strengthMv(myA0, myB0),
                    strengthMv(myA1, myB0), strengthMv(myA0, myB1), strengthMv(myA1, myB1)

            ) : 0;
        } else if (otherPOCL0 == curPOCL0 && otherPOCL1 == curPOCL1) {
            return Math.max(
                    otherPOCL0 != Frame.MVS.POC_NO_REFERENCE ? Math.max(strengthMv(mxA0, mxB0), strengthMv(myA0, myB0))
                            : 0,
                    otherPOCL1 != Frame.MVS.POC_NO_REFERENCE ? Math.max(strengthMv(mxA1, mxB1), strengthMv(myA1, myB1))
                            : 0);
        } else if (otherPOCL0 == curPOCL1 && otherPOCL1 == curPOCL0) {
            return Math.max(
                    otherPOCL0 != Frame.MVS.POC_NO_REFERENCE ? Math.max(strengthMv(mxA0, mxB1), strengthMv(myA0, myB1))
                            : 0,
                    otherPOCL1 != Frame.MVS.POC_NO_REFERENCE ? Math.max(strengthMv(mxA1, mxB0), strengthMv(myA1, myB0))
                            : 0);
        }

        return 0;
    }

    private static boolean strengthIntra(DecodedMBlock other, DecodedMBlock cur) {
        return cur.getType() != null && cur.getType().isIntra() || other.getType() != null && other.getType().isIntra();
    }

    private static boolean strengthIntra(DecodedMBlock cur) {
        return cur.getType() != null && cur.getType().isIntra();
    }

    private static int getIdxBeta(int sliceBetaOffset, int qp) {
        return MathUtil.clip(qp + sliceBetaOffset, 0, 51);
    }

    private static int getIdxAlpha(int sliceAlphaC0Offset, int qp) {
        return MathUtil.clip(qp + sliceAlphaC0Offset, 0, 51);
    }

    private static boolean strengthNc(int ncA, int ncB) {
        return ncA > 0 || ncB > 0;
    }

    private static int strengthMv(int v0, int v1) {
        return abs(v0 - v1) >= 4 ? 1 : 0;
    }
}
