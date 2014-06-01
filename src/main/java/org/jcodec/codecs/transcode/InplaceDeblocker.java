package org.jcodec.codecs.transcode;

import static java.lang.Math.abs;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

public class InplaceDeblocker {

    private int[] topQp;
    private int leftQp;
    private int[] leftNCoeff;
    private int[][] topNCoeff;
    private boolean[] topIntra;
    private boolean leftIntra;
    private int mbWidth;
    private int[][] leftMvs;
    private int[][][] topMvs;
    private Picture[] leftRefs;
    private Picture[][] topRefs;

    public InplaceDeblocker(int mbWidth) {
        this.mbWidth = mbWidth;
        topQp = new int[mbWidth];
        topNCoeff = new int[mbWidth][];
        topIntra = new boolean[mbWidth];
        topMvs = new int[mbWidth][][];
        topRefs = new Picture[mbWidth][];
    }

    private static int getIdxBeta(int sliceBetaOffset, int avgQp) {
        return MathUtil.clip(avgQp + sliceBetaOffset, 0, 51);
    }

    private static int getIdxAlpha(int sliceAlphaC0Offset, int avgQp) {
        return MathUtil.clip(avgQp + sliceAlphaC0Offset, 0, 51);
    }

    private void deblockVert(SliceHeader sh, int[] leftPix, int leftBlk, int leftQp, int[] leftNCoeff,
            boolean leftIntra, int[][] leftMvs, Picture[] leftRefs, int[] rightPix, int rightBlk, int rightQp,
            int[] rightNCoeff, boolean rightIntra, int[][] rightMvs, Picture[] rightRefs, boolean boudary) {

        int alpha = sh.slice_alpha_c0_offset_div2 << 1;
        int beta = sh.slice_beta_offset_div2 << 1;
        int avgQpV = (leftQp + rightQp + 1) >> 1;

        boolean intra = leftIntra || rightIntra;

        int idxAlpha = getIdxAlpha(alpha, avgQpV);
        int idxBeta = getIdxBeta(beta, avgQpV);

        if (boudary && intra) {
            for (int i = 0, leftCol = (leftBlk << 2) + 3, rightCol = rightBlk << 2; i < 16; i++, leftCol += 16, rightCol += 16)
                filterStrong(idxAlpha, idxBeta, leftPix, rightPix, leftCol - 3, leftCol - 2, leftCol - 1, leftCol,
                        rightCol, rightCol + 1, rightCol + 2, rightCol + 3, false);
        } else {
            for (int blk = 0, leftCol = (leftBlk << 2) + 3, rightCol = rightBlk << 2; blk < 4; blk++, leftBlk += 4, rightBlk += 4) {
                int bs = intra ? 3 : calcBs(leftNCoeff[leftBlk], rightNCoeff[rightBlk], leftMvs[leftBlk],
                        rightMvs[rightBlk], leftRefs, rightRefs);
                if (bs > 0) {
                    for (int i = 0; i < 4; i++, leftCol += 16, rightCol += 16)
                        filterWeak(bs, idxAlpha, idxBeta, leftPix, rightPix, leftCol - 2, leftCol - 1, leftCol,
                                rightCol, rightCol + 1, rightCol + 2, false);
                }
            }
        }
    }

    private void deblockHor(SliceHeader sh, int[] topPix, int topBlk, int topQp, int[] topNCoeff, boolean topIntra,
            int[][] topMvs, Picture[] topRefs, int[] botPix, int botBlk, int botQp, int[] botNCoeff, boolean botIntra,
            int[][] botMvs, Picture[] botRefs, boolean boundary) {
        int alpha = sh.slice_alpha_c0_offset_div2 << 1;
        int beta = sh.slice_beta_offset_div2 << 1;
        int avgQpV = (topQp + botQp + 1) >> 1;

        boolean intra = topIntra || botIntra;
        int idxAlpha = getIdxAlpha(alpha, avgQpV);
        int idxBeta = getIdxBeta(beta, avgQpV);

        if (boundary && intra) {
            for (int i = 0, topRow = (topBlk << 4) + 48, botRow = botBlk << 4; i < 16; i++, topRow++, botRow++)
                filterStrong(idxAlpha, idxBeta, topPix, botPix, topRow - 48, topRow - 32, topRow - 16, topRow, botRow,
                        botRow + 16, botRow + 32, botRow + 48, false);
        } else {
            for (int blk = 0, topRow = (topBlk << 4) + 48, botRow = botBlk << 4; blk < 4; blk++, topBlk++, botBlk++) {
                int bs = intra ? 3 : calcBs(topNCoeff[topBlk], botNCoeff[botBlk], topMvs[topBlk], botMvs[botBlk],
                        topRefs, botRefs);
                if (bs > 0) {
                    for (int i = 0; i < 4; i++, topRow++, botRow++)
                        filterWeak(bs, idxAlpha, idxBeta, topPix, botPix, topRow - 32, topRow - 16, topRow, botRow,
                                botRow + 16, botRow + 32, false);
                }
            }
        }
    }

    public static boolean mvThresh(int[] v0, int[] v1) {
        return abs(v0[0] - v1[0]) >= 4 || abs(v0[1] - v1[1]) >= 4;
    }

    private int calcBs(int leftCoeff, int rightCoeff, int[] mvPL0, int[] mvQL0, Picture[] refsP, Picture[] refsQ) {
        if (leftCoeff > 0 || rightCoeff > 0)
            return 2;

        int nA = (mvPL0[2] == -1 ? 0 : 1);
        int nB = (mvQL0[2] == -1 ? 0 : 1);

        if (nA != nB)
            return 1;

        Picture ra0 = mvPL0[2] < 0 ? null : refsP[mvPL0[2]];
        Picture rb0 = mvQL0[2] < 0 ? null : refsQ[mvQL0[2]];

        if (ra0 != rb0)
            return 1;

        return mvThresh(mvPL0, mvQL0) ? 1 : 0;
    }

    public static void filterStrong(int indexAlpha, int indexBeta, int[] pelsP, int[] pelsQ, int p3Idx, int p2Idx,
            int p1Idx, int p0Idx, int q0Idx, int q1Idx, int q2Idx, int q3Idx, boolean isChroma) {
        int p0 = pelsP[p0Idx];
        int q0 = pelsQ[q0Idx];
        int p1 = pelsP[p1Idx];
        int q1 = pelsQ[q1Idx];

        int alphaThresh = H264Const.alphaTab[indexAlpha];
        int betaThresh = H264Const.betaTab[indexBeta];

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
            pelsP[p0Idx] = clip(p0n, 0, 255);
            pelsP[p1Idx] = clip(p1n, 0, 255);
            pelsP[p2Idx] = clip(p2n, 0, 255);
        } else {
            int p0n = (2 * p1 + p0 + q1 + 2) >> 2;
            pelsP[p0Idx] = clip(p0n, 0, 255);
        }

        if (conditionQ && !isChroma) {
            int q2 = pelsQ[q2Idx];
            int q3 = pelsQ[q3Idx];
            int q0n = (p1 + 2 * p0 + 2 * q0 + 2 * q1 + q2 + 4) >> 3;
            int q1n = (p0 + q0 + q1 + q2 + 2) >> 2;
            int q2n = (2 * q3 + 3 * q2 + q1 + q0 + p0 + 4) >> 3;
            pelsQ[q0Idx] = clip(q0n, 0, 255);
            pelsQ[q1Idx] = clip(q1n, 0, 255);
            pelsQ[q2Idx] = clip(q2n, 0, 255);
        } else {
            int q0n = (2 * q1 + q0 + p1 + 2) >> 2;
            pelsQ[q0Idx] = clip(q0n, 0, 255);
        }
    }

    public static void filterWeak(int bs, int indexAlpha, int indexBeta, int[] pelsP, int[] pelsQ, int p2Idx,
            int p1Idx, int p0Idx, int q0Idx, int q1Idx, int q2Idx, boolean isChroma) {

        int p1 = pelsP[p1Idx];
        int p0 = pelsP[p0Idx];
        int q0 = pelsQ[q0Idx];
        int q1 = pelsQ[q1Idx];

        int alphaThresh = H264Const.alphaTab[indexAlpha];
        int betaThresh = H264Const.betaTab[indexBeta];

        boolean filterEnabled = abs(p0 - q0) < alphaThresh && abs(p1 - p0) < betaThresh && abs(q1 - q0) < betaThresh;

        if (!filterEnabled)
            return;

        int tC0 = H264Const.tcs[bs - 1][indexAlpha];

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
        p0n = p0n < 0 ? 0 : p0n;
        int q0n = q0 - sigma;
        q0n = q0n < 0 ? 0 : q0n;

        if (conditionP) {
            int p2 = pelsP[p2Idx];

            int diff = (p2 + ((p0 + q0 + 1) >> 1) - (p1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : (diff > tC0 ? tC0 : diff);
            int p1n = p1 + diff;
            pelsP[p1Idx] = clip(p1n, 0, 255);
        }

        if (conditionQ) {
            int q2 = pelsQ[q2Idx];
            int diff = (q2 + ((p0 + q0 + 1) >> 1) - (q1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : (diff > tC0 ? tC0 : diff);
            int q1n = q1 + diff;
            pelsQ[q1Idx] = clip(q1n, 0, 255);
        }

        pelsQ[q0Idx] = clip(q0n, 0, 255);
        pelsP[p0Idx] = clip(p0n, 0, 255);

    }

    public void deblock(SliceHeader sh, int mbAddr, int[] leftPix, int[] topPix, int[] thisPix, int qp, int[] nCoeff,
            int[][] mvs, Picture[] refs, boolean intra) {
        int mbX = mbAddr % mbWidth;

        if (leftPix != null)
            deblockVert(sh, leftPix, 3, leftQp, leftNCoeff, leftIntra, leftMvs, leftRefs, thisPix, 0, qp, nCoeff,
                    intra, mvs, refs, true);
        deblockVert(sh, thisPix, 1, qp, nCoeff, intra, mvs, refs, thisPix, 2, qp, nCoeff, intra, mvs, refs, false);
        deblockVert(sh, thisPix, 0, qp, nCoeff, intra, mvs, refs, thisPix, 1, qp, nCoeff, intra, mvs, refs, false);
        deblockVert(sh, thisPix, 2, qp, nCoeff, intra, mvs, refs, thisPix, 3, qp, nCoeff, intra, mvs, refs, false);

        if (topPix != null)
            deblockHor(sh, topPix, 12, topQp[mbX], topNCoeff[mbX], topIntra[mbX], topMvs[mbX], topRefs[mbX], thisPix,
                    0, qp, nCoeff, intra, mvs, refs, true);
        deblockHor(sh, thisPix, 4, qp, nCoeff, intra, mvs, refs, thisPix, 8, qp, nCoeff, intra, mvs, refs, false);
        deblockHor(sh, thisPix, 0, qp, nCoeff, intra, mvs, refs, thisPix, 4, qp, nCoeff, intra, mvs, refs, false);
        deblockHor(sh, thisPix, 8, qp, nCoeff, intra, mvs, refs, thisPix, 12, qp, nCoeff, intra, mvs, refs, false);

        topQp[mbX] = leftQp = qp;
        topNCoeff[mbX] = leftNCoeff = nCoeff;
        topIntra[mbX] = leftIntra = intra;
        topMvs[mbX] = leftMvs = mvs;
        topRefs[mbX] = leftRefs = refs;
    }
}