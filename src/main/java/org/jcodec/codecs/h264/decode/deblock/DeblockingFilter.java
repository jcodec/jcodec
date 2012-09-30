package org.jcodec.codecs.h264.decode.deblock;

import org.jcodec.codecs.h264.decode.deblock.FilterParameter.Threshold;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A filter that removes DCT artifacts on block boundaries.
 * 
 * It's operation is dependant on QP and is designed the way that the strenth is
 * adjusted to the likelyhood of appearence of blocking artifacts on the
 * specific edges.
 * 
 * 
 * @author Jay Codec
 * 
 */
public class DeblockingFilter {

    static int[] alphaTab = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 5, 6, 7, 8, 9, 10, 12,
            13, 15, 17, 20, 22, 25, 28, 32, 36, 40, 45, 50, 56, 63, 71, 80, 90, 101, 113, 127, 144, 162, 182, 203, 226,
            255, 255 };
    static int[] betaTab = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 6,
            6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18 };

    static int[][] tcs = new int[][] {
            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13 },

            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
                    2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 10, 11, 12, 13, 15, 17 },

            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3,
                    3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13, 14, 16, 18, 20, 23, 25 } };

    private int maxLuma;
    private int maxChroma;
    private int picWidthInMbs;
    private int picHeightInMbs;

    public DeblockingFilter(int picWidthInMbs, int picHeightInMbs, int bitDepthLuma, int bitDepthChroma) {
        this.maxLuma = (1 << bitDepthLuma) - 1;
        this.maxChroma = (1 << bitDepthChroma) - 1;

        this.picWidthInMbs = picWidthInMbs;
        this.picHeightInMbs = picHeightInMbs;
    }

    public void applyDeblocking(DecodedMBlock[] decoded, FilterParameter[] FilterParameters) {

        for (int mbY = 0; mbY < picHeightInMbs; mbY++) {
            for (int mbX = 0; mbX < picWidthInMbs; mbX++) {

                int mbAddr = mbY * picWidthInMbs + mbX;

                DecodedMBlock leftMb = null;
                if (mbX > 0) {
                    leftMb = decoded[mbAddr - 1];
                }

                DecodedMBlock topMb = null;
                if (mbY > 0) {
                    topMb = decoded[mbAddr - picWidthInMbs];
                }

                DecodedMBlock curMb = decoded[mbAddr];

                fillVerticalEdge(leftMb, curMb, FilterParameters[mbAddr], mbY, mbX);
                fillHorizontalEdge(topMb, curMb, FilterParameters[mbAddr], mbY, mbX);
            }
        }
    }

    private void fillHorizontalEdge(DecodedMBlock topMb, DecodedMBlock curMb, FilterParameter param, int mbY, int mbX) {

        if (!param.isEnabled())
            return;

        DecodedMBlock mbP, mbQ = curMb;
        int blkY;
        if (param.isFilterTop()) {
            blkY = 0;
            mbP = topMb;
        } else {
            blkY = 1;
            mbP = curMb;
        }

        for (; blkY < 4; blkY++) {

            for (int blkX = 0; blkX < 4; blkX++) {

                int bs = param.getBsH()[(blkY << 2) + blkX];

                Threshold lumaThresh = param.getLumaThresh();
                filterBlockEdgeHoris(mbP.getLuma(), mbQ.getLuma(), blkX, blkY, lumaThresh.getAlphaH()[blkY],
                        lumaThresh.getBetaH()[blkY], bs, false);

                // Chroma
                if ((blkY % 2) == 0) {

                    Threshold cbThresh = param.getCbThresh();
                    filterBlockEdgeHoris(mbP.getChroma().getCb(), mbQ.getChroma().getCb(), blkX, blkY,
                            cbThresh.getAlphaH()[blkY], param.getCbThresh().getBetaH()[blkY], bs, true);

                    Threshold crThresh = param.getCrThresh();
                    filterBlockEdgeHoris(mbP.getChroma().getCr(), mbQ.getChroma().getCr(), blkX, blkY,
                            crThresh.getAlphaH()[blkY], crThresh.getBetaH()[blkY], bs, true);
                }
            }

            mbP = curMb;
        }
    }

    private void fillVerticalEdge(DecodedMBlock leftMb, DecodedMBlock curMb, FilterParameter param, int mbY, int mbX) {

        if (!param.isEnabled())
            return;

        DecodedMBlock mbP, mbQ = curMb;
        int blkX;
        if (param.isFilterLeft()) {
            blkX = 0;
            mbP = leftMb;
        } else {
            blkX = 1;
            mbP = curMb;
        }

        for (; blkX < 4; blkX++) {

            for (int blkY = 0; blkY < 4; blkY++) {

                int bs = param.getBsV()[(blkY << 2) + blkX];

                Threshold lumaThresh = param.getLumaThresh();
                filterBlockEdgeVert(mbP.getLuma(), mbQ.getLuma(), blkX, blkY, lumaThresh.getAlphaV()[blkX],
                        lumaThresh.getBetaV()[blkX], bs, false);

                // Chroma
                if ((blkX % 2) == 0) {

                    Threshold cbThresh = param.getCbThresh();
                    filterBlockEdgeVert(mbP.getChroma().getCb(), mbQ.getChroma().getCb(), blkX, blkY,
                            cbThresh.getAlphaV()[blkX], cbThresh.getBetaV()[blkX], bs, true);

                    Threshold crThresh = param.getCrThresh();
                    filterBlockEdgeVert(mbP.getChroma().getCr(), mbQ.getChroma().getCr(), blkX, blkY,
                            crThresh.getAlphaV()[blkX], crThresh.getBetaV()[blkX], bs, true);
                }
            }

            mbP = curMb;
        }
    }

    private void filterBlockEdgeHoris(int[] pelsP, int[] pelsQ, int blkX, int blkY, int indexAlpha, int indexBeta,
            int bs, boolean isChroma) {
        int pelsInBlock = isChroma ? 2 : 4;
        int stride = isChroma ? 8 : 16;

        int pBlkY = ((blkY + 3) % 4) + 1;
        int offsetQ = (blkY * pelsInBlock) * stride + blkX * pelsInBlock;
        int offsetP = (pBlkY * pelsInBlock) * stride + blkX * pelsInBlock;

        for (int pixOff = 0; pixOff < pelsInBlock; pixOff++) {
            int p2Idx = offsetP - 3 * stride + pixOff;
            int p1Idx = offsetP - 2 * stride + pixOff;
            int p0Idx = offsetP - stride + pixOff;
            int q0Idx = offsetQ + pixOff;
            int q1Idx = offsetQ + stride + pixOff;
            int q2Idx = offsetQ + 2 * stride + pixOff;

            if (bs == 4) {
                int p3Idx = offsetP - 4 * stride + pixOff;
                int q3Idx = offsetQ + 3 * stride + pixOff;

                filterBs4(indexAlpha, indexBeta, pelsP, pelsQ, p3Idx, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, q3Idx,
                        isChroma);
            } else if (bs > 0) {

                filterBs(bs, indexAlpha, indexBeta, pelsP, pelsQ, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, isChroma);
            }
        }
    }

    private void filterBlockEdgeVert(int[] pelsP, int[] pelsQ, int blkX, int blkY, int indexAlpha, int indexBeta,
            int bs, boolean isChroma) {
        int pBlkX = ((blkX + 3) % 4) + 1;

        int pelsInBlock = isChroma ? 2 : 4;
        int stride = isChroma ? 8 : 16;
        for (int i = 0; i < pelsInBlock; i++) {
            int offsetQ = (blkY * pelsInBlock + i) * stride + blkX * pelsInBlock;
            int offsetP = (blkY * pelsInBlock + i) * stride + pBlkX * pelsInBlock;
            int p2Idx = offsetP - 3;
            int p1Idx = offsetP - 2;
            int p0Idx = offsetP - 1;
            int q0Idx = offsetQ;
            int q1Idx = offsetQ + 1;
            int q2Idx = offsetQ + 2;
            if (bs == 4) {
                int p3Idx = offsetP - 4;
                int q3Idx = offsetQ + 3;
                filterBs4(indexAlpha, indexBeta, pelsP, pelsQ, p3Idx, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, q3Idx,
                        isChroma);
            } else if (bs > 0) {
                filterBs(bs, indexAlpha, indexBeta, pelsP, pelsQ, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, isChroma);
            }
        }
    }

    private void filterBs(int bs, int indexAlpha, int indexBeta, int[] pelsP, int[] pelsQ, int p2Idx, int p1Idx,
            int p0Idx, int q0Idx, int q1Idx, int q2Idx, boolean isChroma) {

        int p1 = pelsP[p1Idx];
        int p0 = pelsP[p0Idx];
        int q0 = pelsQ[q0Idx];
        int q1 = pelsQ[q1Idx];

        int maxPel = isChroma ? maxChroma : maxLuma;

        int alphaThresh = alphaTab[indexAlpha];
        int betaThresh = betaTab[indexBeta];

        boolean filterEnabled = abs(p0 - q0) < alphaThresh && abs(p1 - p0) < betaThresh && abs(q1 - q0) < betaThresh;

        if (!filterEnabled)
            return;

        // System.out.printf("%h %h %h %h %h %h %h %h\n", q3, q2, q1, q0, p0,
        // p1, p2, p3);

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
        p0n = p0n < 0 ? 0 : p0n;
        int q0n = q0 - sigma;
        q0n = q0n < 0 ? 0 : q0n;

        if (conditionP) {
            int p2 = pelsP[p2Idx];

            int diff = (p2 + ((p0 + q0 + 1) >> 1) - (p1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : (diff > tC0 ? tC0 : diff);
            int p1n = p1 + diff;
            pelsP[p1Idx] = p1n <= maxPel ? p1n : maxPel;
        }

        if (conditionQ) {
            int q2 = pelsQ[q2Idx];
            int diff = (q2 + ((p0 + q0 + 1) >> 1) - (q1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : (diff > tC0 ? tC0 : diff);
            int q1n = q1 + diff;
            pelsQ[q1Idx] = q1n <= maxPel ? q1n : maxPel;
        }

        pelsQ[q0Idx] = q0n <= maxPel ? q0n : maxPel;
        pelsP[p0Idx] = p0n <= maxPel ? p0n : maxPel;

    }

    private void filterBs4(int indexAlpha, int indexBeta, int[] pelsP, int[] pelsQ, int p3Idx, int p2Idx, int p1Idx,
            int p0Idx, int q0Idx, int q1Idx, int q2Idx, int q3Idx, boolean isChroma) {
        int p0 = pelsP[p0Idx];
        int q0 = pelsQ[q0Idx];
        int p1 = pelsP[p1Idx];
        int q1 = pelsQ[q1Idx];

        int maxPel = isChroma ? maxChroma : maxLuma;

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
            pelsP[p0Idx] = p0n <= maxPel ? p0n : maxPel;
            pelsP[p1Idx] = p1n <= maxPel ? p1n : maxPel;
            pelsP[p2Idx] = p2n <= maxPel ? p2n : maxPel;
        } else {
            int p0n = (2 * p1 + p0 + q1 + 2) >> 2;
            pelsP[p0Idx] = p0n <= maxPel ? p0n : maxPel;
        }

        if (conditionQ && !isChroma) {
            int q2 = pelsQ[q2Idx];
            int q3 = pelsQ[q3Idx];
            int q0n = (p1 + 2 * p0 + 2 * q0 + 2 * q1 + q2 + 4) >> 3;
            int q1n = (p0 + q0 + q1 + q2 + 2) >> 2;
            int q2n = (2 * q3 + 3 * q2 + q1 + q0 + p0 + 4) >> 3;
            pelsQ[q0Idx] = q0n <= maxPel ? q0n : maxPel;
            pelsQ[q1Idx] = q1n <= maxPel ? q1n : maxPel;
            pelsQ[q2Idx] = q2n <= maxPel ? q2n : maxPel;
        } else {
            int q0n = (2 * q1 + q0 + p1 + 2) >> 2;
            pelsQ[q0Idx] = q0n <= maxPel ? q0n : maxPel;
        }
    }

    static private int abs(int i) {
        return i > 0 ? i : -i;
    }
}