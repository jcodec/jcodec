package org.jcodec.codecs.h264.decode.deblock;

import static java.lang.Math.abs;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvRef;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvX;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvY;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.h264.decode.DeblockerInput;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

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
 * Builds a parameter for deblocking filter based on the properties of specific
 * macroblocks.
 * 
 * A parameter specifies the behavior of deblocking filter on each of 8 edges
 * that need to filtered for a macroblock.
 * 
 * For each edge the following things are evaluated on it's both sides: presence
 * of DCT coded residual; motion vector difference; spatial location.
 * 
 * 
 * @author The JCodec project
 * 
 */
public class DeblockingFilter {

    public static int[] alphaTab = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 5, 6, 7, 8, 9, 10, 12,
            13, 15, 17, 20, 22, 25, 28, 32, 36, 40, 45, 50, 56, 63, 71, 80, 90, 101, 113, 127, 144, 162, 182, 203, 226,
            255, 255 };
    public static int[] betaTab = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 6,
            6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18 };

    public static int[][] tcs = new int[][] {
            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                    1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13 },

            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
                    2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 10, 11, 12, 13, 15, 17 },

            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3,
                    3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13, 14, 16, 18, 20, 23, 25 } };

    private DeblockerInput di;

    public DeblockingFilter(int bitDepthLuma, int bitDepthChroma, DeblockerInput di) {
        this.di = di;
    }

    public void deblockFrame(Picture result) {
        ColorSpace color = result.getColor();
        // for (int i = 0; i < shs.length; i++)
        // printMB(result.getPlaneData(2), result.getPlaneWidth(2), i, shs[i],
        // "!--!--!--!--!--!--!--!--!--!--!--!");
        // printMB(result.getPlaneData(0), result.getPlaneWidth(0), 0, shs[0],
        // "!--!--!--!--!--!--!--!--!--!--!--!");
        int[][] bsV = new int[4][4], bsH = new int[4][4];
        for (int i = 0; i < di.shs.length; i++) {
            calcBsH(result, i, bsH);
            calcBsV(result, i, bsV);
            for (int c = 0; c < color.nComp; c++) {
                fillVerticalEdge(result, c, i, bsV);
                fillHorizontalEdge(result, c, i, bsH);
                // printMB(result.getPlaneData(1), result.getPlaneWidth(1), i,
                // shs[i],
                // "!**!**!**!**!--!--!--!--!--!--!--!");
            }
        }
        // printMB(result.getPlaneData(0), result.getPlaneWidth(0), 235,
        // shs[235], "!**!**!**!**!--!--!--!--!--!--!--!");
    }

    // private void printMB(int[] is, int stride, int mbAddr, SliceHeader sh,
    // String delim) {
    // int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;
    // int mbX = mbAddr % mbWidth;
    // int mbY = mbAddr / mbWidth;
    //
    // System.out.println("MB: " + mbX + ", " + mbY);
    // System.out.println(delim);
    // for (int j = 0; j < 16; j++) {
    // for (int i = 0; i < 16; i++)
    // System.out.print(String.format("%3d,", is[((mbY << 4) + j) * stride +
    // (mbX << 4) + i]));
    // System.out.println();
    // }
    // System.out.println(delim);
    // }

    static int[] inverse = new int[] { 0, 1, 4, 5, 2, 3, 6, 7, 8, 9, 12, 13, 10, 11, 14, 15 };

    private int calcBoundaryStrenth(boolean atMbBoundary, boolean leftIntra, boolean rightIntra, int leftCoeff,
            int rightCoeff, int mvA0, int mvB0, int mvA1, int mvB1, int mbAddrA, int mbAddrB) {

        if (atMbBoundary && (leftIntra || rightIntra))
            return 4;
        else if (leftIntra || rightIntra)
            return 3;
        else {

            if (leftCoeff > 0 || rightCoeff > 0)
                return 2;

            int nA = (mvRef(mvA0) == -1 ? 0 : 1) + (mvRef(mvA1) == -1 ? 0 : 1);
            int nB = (mvRef(mvB0) == -1 ? 0 : 1) + (mvRef(mvB1) == -1 ? 0 : 1);

            if (nA != nB)
                return 1;

            Picture ra0 = mvRef(mvA0) < 0 ? null : di.refsUsed[mbAddrA][0][mvRef(mvA0)];
            Picture ra1 = mvRef(mvA1) < 0 ? null : di.refsUsed[mbAddrA][1][mvRef(mvA1)];

            Picture rb0 = mvRef(mvB0) < 0 ? null : di.refsUsed[mbAddrB][0][mvRef(mvB0)];
            Picture rb1 = mvRef(mvB1) < 0 ? null : di.refsUsed[mbAddrB][1][mvRef(mvB1)];

            if (ra0 != rb0 && ra0 != rb1 || ra1 != rb0 && ra1 != rb1 || rb0 != ra0 && rb0 != ra1 || rb1 != ra0
                    && rb1 != ra1)
                return 1;

            if (ra0 == ra1 && ra1 == rb0 && rb0 == rb1) {
                return ra0 != null
                        && (mvThresh(mvA0, mvB0) || mvThresh(mvA1, mvB0) || mvThresh(mvA0, mvB1) || mvThresh(mvA1, mvB1)) ? 1
                        : 0;
            } else if (ra0 == rb0 && ra1 == rb1) {
                return ra0 != null && mvThresh(mvA0, mvB0) || ra1 != null && mvThresh(mvA1, mvB1) ? 1 : 0;
            } else if (ra0 == rb1 && ra1 == rb0) {
                return ra0 != null && mvThresh(mvA0, mvB1) || ra1 != null && mvThresh(mvA1, mvB0) ? 1 : 0;
            }
        }

        return 0;
    }

    private boolean mvThresh(int v0, int v1) {
        return abs(mvX(v0) - mvX(v1)) >= 4 || abs(mvY(v0) - mvY(v1)) >= 4;
    }

    private static int getIdxBeta(int sliceBetaOffset, int avgQp) {
        return MathUtil.clip(avgQp + sliceBetaOffset, 0, 51);
    }

    private static int getIdxAlpha(int sliceAlphaC0Offset, int avgQp) {
        return MathUtil.clip(avgQp + sliceAlphaC0Offset, 0, 51);
    }

    private void calcBsH(Picture pic, int mbAddr, int[][] bs) {
        SliceHeader sh = di.shs[mbAddr];
        int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        int mbX = mbAddr % mbWidth;
        int mbY = mbAddr / mbWidth;

        boolean topAvailable = mbY > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - mbWidth] == sh);
        boolean thisIntra = di.mbTypes[mbAddr] != null && di.mbTypes[mbAddr].isIntra();

        if (topAvailable) {
            boolean topIntra = di.mbTypes[mbAddr - mbWidth] != null && di.mbTypes[mbAddr - mbWidth].isIntra();
            for (int blkX = 0; blkX < 4; blkX++) {
                int thisBlkX = (mbX << 2) + blkX;
                int thisBlkY = (mbY << 2);

                bs[0][blkX] = calcBoundaryStrenth(true, topIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                        di.nCoeff[thisBlkY - 1][thisBlkX], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                        di.mvs.getMv(thisBlkX, thisBlkY - 1, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                        di.mvs.getMv(thisBlkX, thisBlkY - 1, 1), mbAddr, mbAddr - mbWidth);

            }
        }

        for (int blkY = 1; blkY < 4; blkY++) {
            for (int blkX = 0; blkX < 4; blkX++) {
                int thisBlkX = (mbX << 2) + blkX;
                int thisBlkY = (mbY << 2) + blkY;

                bs[blkY][blkX] = calcBoundaryStrenth(false, thisIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                        di.nCoeff[thisBlkY - 1][thisBlkX], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                        di.mvs.getMv(thisBlkX, thisBlkY - 1, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                        di.mvs.getMv(thisBlkX, thisBlkY - 1, 1), mbAddr, mbAddr);
            }
        }
    }

    private void fillHorizontalEdge(Picture pic, int comp, int mbAddr, int[][] bs) {
        SliceHeader sh = di.shs[mbAddr];
        int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        int alpha = sh.sliceAlphaC0OffsetDiv2 << 1;
        int beta = sh.sliceBetaOffsetDiv2 << 1;

        int mbX = mbAddr % mbWidth;
        int mbY = mbAddr / mbWidth;

        boolean topAvailable = mbY > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - mbWidth] == sh);
        int curQp = di.mbQps[comp][mbAddr];

        int cW = 2 - pic.getColor().compWidth[comp];
        int cH = 2 - pic.getColor().compHeight[comp];
        if (topAvailable) {
            int topQp = di.mbQps[comp][mbAddr - mbWidth];
            int avgQp = (topQp + curQp + 1) >> 1;
            for (int blkX = 0; blkX < 4; blkX++) {
                int thisBlkX = (mbX << 2) + blkX;
                int thisBlkY = (mbY << 2);

                filterBlockEdgeHoris(pic, comp, thisBlkX << cW, thisBlkY << cH, getIdxAlpha(alpha, avgQp),
                        getIdxBeta(beta, avgQp), bs[0][blkX], 1 << cW);
            }
        }

        boolean skip4x4 = comp == 0 && di.tr8x8Used[mbAddr] || cH == 1;

        for (int blkY = 1; blkY < 4; blkY++) {
            if (skip4x4 && (blkY & 1) == 1)
                continue;

            for (int blkX = 0; blkX < 4; blkX++) {
                int thisBlkX = (mbX << 2) + blkX;
                int thisBlkY = (mbY << 2) + blkY;

                filterBlockEdgeHoris(pic, comp, thisBlkX << cW, thisBlkY << cH, getIdxAlpha(alpha, curQp),
                        getIdxBeta(beta, curQp), bs[blkY][blkX], 1 << cW);
            }
        }
    }

    private void calcBsV(Picture pic, int mbAddr, int[][] bs) {

        SliceHeader sh = di.shs[mbAddr];
        int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        int mbX = mbAddr % mbWidth;
        int mbY = mbAddr / mbWidth;

        boolean leftAvailable = mbX > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - 1] == sh);
        boolean thisIntra = di.mbTypes[mbAddr] != null && di.mbTypes[mbAddr].isIntra();

        if (leftAvailable) {
            boolean leftIntra = di.mbTypes[mbAddr - 1] != null && di.mbTypes[mbAddr - 1].isIntra();
            for (int blkY = 0; blkY < 4; blkY++) {
                int thisBlkX = (mbX << 2);
                int thisBlkY = (mbY << 2) + blkY;
                bs[blkY][0] = calcBoundaryStrenth(true, leftIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                        di.nCoeff[thisBlkY][thisBlkX - 1], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                        di.mvs.getMv(thisBlkX - 1, thisBlkY, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                        di.mvs.getMv(thisBlkX - 1, thisBlkY, 1), mbAddr, mbAddr - 1);
            }
        }

        for (int blkX = 1; blkX < 4; blkX++) {
            for (int blkY = 0; blkY < (1 << 2); blkY++) {
                int thisBlkX = (mbX << 2) + blkX;
                int thisBlkY = (mbY << 2) + blkY;
                bs[blkY][blkX] = calcBoundaryStrenth(false, thisIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                        di.nCoeff[thisBlkY][thisBlkX - 1], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                        di.mvs.getMv(thisBlkX - 1, thisBlkY, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                        di.mvs.getMv(thisBlkX - 1, thisBlkY, 1), mbAddr, mbAddr);
            }
        }
    }

    private void fillVerticalEdge(Picture pic, int comp, int mbAddr, int[][] bs) {

        SliceHeader sh = di.shs[mbAddr];
        int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        int alpha = sh.sliceAlphaC0OffsetDiv2 << 1;
        int beta = sh.sliceBetaOffsetDiv2 << 1;

        int mbX = mbAddr % mbWidth;
        int mbY = mbAddr / mbWidth;

        boolean leftAvailable = mbX > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - 1] == sh);
        int curQp = di.mbQps[comp][mbAddr];

        int cW = 2 - pic.getColor().compWidth[comp];
        int cH = 2 - pic.getColor().compHeight[comp];
        if (leftAvailable) {
            int leftQp = di.mbQps[comp][mbAddr - 1];
            int avgQpV = (leftQp + curQp + 1) >> 1;
            for (int blkY = 0; blkY < 4; blkY++) {
                int thisBlkX = (mbX << 2);
                int thisBlkY = (mbY << 2) + blkY;
                filterBlockEdgeVert(pic, comp, thisBlkX << cW, thisBlkY << cH, getIdxAlpha(alpha, avgQpV),
                        getIdxBeta(beta, avgQpV), bs[blkY][0], 1 << cH);
            }
        }
        boolean skip4x4 = comp == 0 && di.tr8x8Used[mbAddr] || cW == 1;

        for (int blkX = 1; blkX < 4; blkX++) {
            if (skip4x4 && (blkX & 1) == 1)
                continue;
            for (int blkY = 0; blkY < 4; blkY++) {
                int thisBlkX = (mbX << 2) + blkX;
                int thisBlkY = (mbY << 2) + blkY;
                filterBlockEdgeVert(pic, comp, thisBlkX << cW, thisBlkY << cH, getIdxAlpha(alpha, curQp),
                        getIdxBeta(beta, curQp), bs[blkY][blkX], 1 << cH);
            }
        }
    }

    private void filterBlockEdgeHoris(Picture pic, int comp, int x, int y, int indexAlpha, int indexBeta, int bs,
            int blkW) {

        int stride = pic.getPlaneWidth(comp);
        int offset = y * stride + x;

        for (int pixOff = 0; pixOff < blkW; pixOff++) {
            int p2Idx = offset - 3 * stride + pixOff;
            int p1Idx = offset - 2 * stride + pixOff;
            int p0Idx = offset - stride + pixOff;
            int q0Idx = offset + pixOff;
            int q1Idx = offset + stride + pixOff;
            int q2Idx = offset + 2 * stride + pixOff;

            if (bs == 4) {
                int p3Idx = offset - 4 * stride + pixOff;
                int q3Idx = offset + 3 * stride + pixOff;

                filterBs4(indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p3Idx, p2Idx, p1Idx,
                        p0Idx, q0Idx, q1Idx, q2Idx, q3Idx, comp != 0);
            } else if (bs > 0) {

                filterBs(bs, indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p2Idx, p1Idx,
                        p0Idx, q0Idx, q1Idx, q2Idx, comp != 0);
            }
        }
    }

    private void filterBlockEdgeVert(Picture pic, int comp, int x, int y, int indexAlpha, int indexBeta, int bs,
            int blkH) {

        int stride = pic.getPlaneWidth(comp);
        for (int i = 0; i < blkH; i++) {
            int offsetQ = (y + i) * stride + x;
            int p2Idx = offsetQ - 3;
            int p1Idx = offsetQ - 2;
            int p0Idx = offsetQ - 1;
            int q0Idx = offsetQ;
            int q1Idx = offsetQ + 1;
            int q2Idx = offsetQ + 2;

            if (bs == 4) {
                int p3Idx = offsetQ - 4;
                int q3Idx = offsetQ + 3;
                filterBs4(indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p3Idx, p2Idx, p1Idx,
                        p0Idx, q0Idx, q1Idx, q2Idx, q3Idx, comp != 0);
            } else if (bs > 0) {
                filterBs(bs, indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p2Idx, p1Idx,
                        p0Idx, q0Idx, q1Idx, q2Idx, comp != 0);
            }
        }
    }

    public static void filterBs(int bs, int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p2Idx, int p1Idx,
            int p0Idx, int q0Idx, int q1Idx, int q2Idx, boolean isChroma) {

        int p1 = pelsP[p1Idx];
        int p0 = pelsP[p0Idx];
        int q0 = pelsQ[q0Idx];
        int q1 = pelsQ[q1Idx];

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
        p0n = p0n < -128 ? -128 : p0n;
        int q0n = q0 - sigma;
        q0n = q0n < -128 ? -128 : q0n;

        if (conditionP) {
            int p2 = pelsP[p2Idx];

            int diff = (p2 + ((p0 + q0 + 1) >> 1) - (p1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : (diff > tC0 ? tC0 : diff);
            int p1n = p1 + diff;
            pelsP[p1Idx] = (byte)clip(p1n, -128, 127);
        }

        if (conditionQ) {
            int q2 = pelsQ[q2Idx];
            int diff = (q2 + ((p0 + q0 + 1) >> 1) - (q1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : (diff > tC0 ? tC0 : diff);
            int q1n = q1 + diff;
            pelsQ[q1Idx] = (byte)clip(q1n, -128, 127);
        }

        pelsQ[q0Idx] = (byte)clip(q0n, -128, 127);
        pelsP[p0Idx] = (byte)clip(p0n, -128, 127);

    }

    public static void filterBs4(int indexAlpha, int indexBeta, byte[] pelsP, byte[] pelsQ, int p3Idx, int p2Idx,
            int p1Idx, int p0Idx, int q0Idx, int q1Idx, int q2Idx, int q3Idx, boolean isChroma) {
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
            pelsP[p0Idx] = (byte)clip(p0n, -128, 127);
            pelsP[p1Idx] = (byte)clip(p1n, -128, 127);
            pelsP[p2Idx] = (byte)clip(p2n, -128, 127);
        } else {
            int p0n = (2 * p1 + p0 + q1 + 2) >> 2;
            pelsP[p0Idx] = (byte)clip(p0n, -128, 127);
        }

        if (conditionQ && !isChroma) {
            int q2 = pelsQ[q2Idx];
            int q3 = pelsQ[q3Idx];
            int q0n = (p1 + 2 * p0 + 2 * q0 + 2 * q1 + q2 + 4) >> 3;
            int q1n = (p0 + q0 + q1 + q2 + 2) >> 2;
            int q2n = (2 * q3 + 3 * q2 + q1 + q0 + p0 + 4) >> 3;
            pelsQ[q0Idx] = (byte)clip(q0n, -128, 127);
            pelsQ[q1Idx] = (byte)clip(q1n, -128, 127);
            pelsQ[q2Idx] = (byte)clip(q2n, -128, 127);
        } else {
            int q0n = (2 * q1 + q0 + p1 + 2) >> 2;
            pelsQ[q0Idx] = (byte)clip(q0n, -128, 127);
        }
    }
}