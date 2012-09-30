package org.jcodec.codecs.h264.decode.deblock;

import org.jcodec.codecs.h264.decode.deblock.FilterParameter.Threshold;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.MVMatrix;
import org.jcodec.codecs.h264.io.model.CodedMacroblock;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.MBlockIntra16x16;
import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
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
 * @author Jay Codec
 * 
 */
public class FilterParameterBuilder {

    static int[] inverse = new int[] { 0, 1, 4, 5, 2, 3, 6, 7, 8, 9, 12, 13, 10, 11, 14, 15 };

    public static FilterParameter calcParameterForMB(int disableDeblockingFilterIdc, int alphaC0Offset, int betaOffset,
            DecodedMBlock curDec, DecodedMBlock leftDec, DecodedMBlock topDec, boolean leftAvailable,
            boolean topAvailable) {

        Threshold thresLuma = getLumaThreshold(alphaC0Offset, betaOffset, curDec.getQp(),
                leftDec != null ? leftDec.getQp() : null, topDec != null ? topDec.getQp() : null);
        Threshold thresCb = getLumaThreshold(alphaC0Offset, betaOffset, curDec.getChroma().getQpCb(),
                leftDec != null ? leftDec.getChroma().getQpCb() : null, topDec != null ? topDec.getChroma().getQpCb()
                        : null);
        Threshold thresCr = getLumaThreshold(alphaC0Offset, betaOffset, curDec.getChroma().getQpCr(),
                leftDec != null ? leftDec.getChroma().getQpCr() : null, topDec != null ? topDec.getChroma().getQpCr()
                        : null);

        int[] bsV = new int[16];
        if (leftDec != null) {
            bsV[0] = calcBoundaryStrenth(leftDec, curDec, 3, 0, true);
            bsV[4] = calcBoundaryStrenth(leftDec, curDec, 7, 4, true);
            bsV[8] = calcBoundaryStrenth(leftDec, curDec, 11, 8, true);
            bsV[12] = calcBoundaryStrenth(leftDec, curDec, 15, 12, true);
        }

        bsV[1] = calcBoundaryStrenth(curDec, curDec, 0, 1, false);
        bsV[2] = calcBoundaryStrenth(curDec, curDec, 1, 2, false);
        bsV[3] = calcBoundaryStrenth(curDec, curDec, 2, 3, false);

        bsV[5] = calcBoundaryStrenth(curDec, curDec, 4, 5, false);
        bsV[6] = calcBoundaryStrenth(curDec, curDec, 5, 6, false);
        bsV[7] = calcBoundaryStrenth(curDec, curDec, 6, 7, false);

        bsV[9] = calcBoundaryStrenth(curDec, curDec, 8, 9, false);
        bsV[10] = calcBoundaryStrenth(curDec, curDec, 9, 10, false);
        bsV[11] = calcBoundaryStrenth(curDec, curDec, 10, 11, false);

        bsV[13] = calcBoundaryStrenth(curDec, curDec, 12, 13, false);
        bsV[14] = calcBoundaryStrenth(curDec, curDec, 13, 14, false);
        bsV[15] = calcBoundaryStrenth(curDec, curDec, 14, 15, false);

        int[] bsH = new int[16];
        if (topDec != null) {
            bsH[0] = calcBoundaryStrenth(topDec, curDec, 12, 0, true);
            bsH[1] = calcBoundaryStrenth(topDec, curDec, 13, 1, true);
            bsH[2] = calcBoundaryStrenth(topDec, curDec, 14, 2, true);
            bsH[3] = calcBoundaryStrenth(topDec, curDec, 15, 3, true);
        }

        bsH[4] = calcBoundaryStrenth(curDec, curDec, 0, 4, false);
        bsH[5] = calcBoundaryStrenth(curDec, curDec, 1, 5, false);
        bsH[6] = calcBoundaryStrenth(curDec, curDec, 2, 6, false);
        bsH[7] = calcBoundaryStrenth(curDec, curDec, 3, 7, false);

        bsH[8] = calcBoundaryStrenth(curDec, curDec, 4, 8, false);
        bsH[9] = calcBoundaryStrenth(curDec, curDec, 5, 9, false);
        bsH[10] = calcBoundaryStrenth(curDec, curDec, 6, 10, false);
        bsH[11] = calcBoundaryStrenth(curDec, curDec, 7, 11, false);

        bsH[12] = calcBoundaryStrenth(curDec, curDec, 8, 12, false);
        bsH[13] = calcBoundaryStrenth(curDec, curDec, 9, 13, false);
        bsH[14] = calcBoundaryStrenth(curDec, curDec, 10, 14, false);
        bsH[15] = calcBoundaryStrenth(curDec, curDec, 11, 15, false);

        boolean enabled;
        boolean filterLeft;
        boolean filterTop;
        if (disableDeblockingFilterIdc == 0) {
            enabled = true;
            filterLeft = leftDec != null;
            filterTop = topDec != null;
        } else if (disableDeblockingFilterIdc == 1) {
            enabled = false;
            filterLeft = false;
            filterTop = false;
        } else {
            enabled = true;
            filterLeft = leftAvailable;
            filterTop = topAvailable;
        }

        return new FilterParameter(filterLeft, filterTop, enabled, thresLuma, thresCb, thresCr, bsH, bsV);
    }

    private static Threshold getLumaThreshold(int alphaC0Offset, int betaOffset, int curQp, Integer leftQp,
            Integer topQp) {

        int[] alphaV = new int[4];
        int[] betaV = new int[4];
        if (leftQp != null) {
            int avgQpV = (leftQp + curQp + 1) >> 1;
            alphaV[0] = getIdxAlpha(alphaC0Offset, avgQpV);
            betaV[0] = getIdxBeta(betaOffset, avgQpV);
        }

        int[] alphaH = new int[4];
        int[] betaH = new int[4];
        if (topQp != null) {
            int avgQpH = (topQp + curQp + 1) >> 1;
            alphaH[0] = getIdxAlpha(alphaC0Offset, avgQpH);
            betaH[0] = getIdxBeta(betaOffset, avgQpH);
        }

        alphaV[1] = alphaV[2] = alphaV[3] = alphaH[1] = alphaH[2] = alphaH[3] = getIdxAlpha(alphaC0Offset, curQp);
        betaV[1] = betaV[2] = betaV[3] = betaH[1] = betaH[2] = betaH[3] = getIdxBeta(betaOffset, curQp);

        return new FilterParameter.Threshold(alphaH, betaH, alphaV, betaV);
    }

    private static int calcBoundaryStrenth(DecodedMBlock mbA, DecodedMBlock mbB, int blkAAddr, int blkBAddr,
            boolean atMbBoundary) {

        CodedMacroblock codedA = mbA.getCoded();
        CodedMacroblock codedB = mbB.getCoded();

        boolean leftIntra = (codedA instanceof MBlockIntra16x16) || (codedA instanceof MBlockIntraNxN);
        boolean rightIntra = (codedB instanceof MBlockIntra16x16) || (codedB instanceof MBlockIntraNxN);

        if (atMbBoundary && (leftIntra || rightIntra))
            return 4;
        else if (leftIntra || rightIntra)
            return 3;
        else {
            CoeffToken leftToken;
            if (codedA != null && (codedA instanceof CodedMacroblock)) {
                leftToken = ((CodedMacroblock) codedA).getLumaTokens()[inverse[blkAAddr]];
            } else {
                leftToken = new CoeffToken(0, 0);
            }
            CoeffToken rightToken;

            if (codedB != null && (codedB instanceof CodedMacroblock)) {
                rightToken = ((CodedMacroblock) codedB).getLumaTokens()[inverse[blkBAddr]];
            } else {
                rightToken = new CoeffToken(0, 0);
            }

            if (leftToken.totalCoeff > 0 || rightToken.totalCoeff > 0)
                return 2;

            MVMatrix mvA = mbA.getDecodedMVs();
            MVMatrix mvB = mbB.getDecodedMVs();

            if (mvA == null && mvB != null)
                return 1;
            else if (mvA != null && mvB == null)
                return 1;
            else if (mvA != null && mvB != null) {

                Vector leftMV = mvA.getVectors()[inverse[blkAAddr]];
                Vector rightMV = mvB.getVectors()[inverse[blkBAddr]];

                Picture[] refListA = mbA.getRefList();
                Picture[] refListB = mbB.getRefList();

                Picture leftRef = refListA[leftMV.getRefId()];
                Picture rightRef = refListB[rightMV.getRefId()];

                if (leftRef != rightRef)
                    return 1;

                if (abs(leftMV.getX() - rightMV.getX()) >= 4 || abs(leftMV.getY() - rightMV.getY()) >= 4)
                    return 1;
            }
        }

        return 0;
    }

    private static int getIdxBeta(int sliceBetaOffset, int avgQp) {
        int idxB = avgQp + sliceBetaOffset;
        idxB = idxB > 51 ? idxB = 51 : (idxB < 0 ? idxB = 0 : idxB);

        return idxB;
    }

    private static int getIdxAlpha(int sliceAlphaC0Offset, int avgQp) {
        int idxA = avgQp + sliceAlphaC0Offset;
        idxA = idxA > 51 ? idxA = 51 : (idxA < 0 ? idxA = 0 : idxA);

        return idxA;
    }

    static private int abs(int i) {
        return i > 0 ? i : -i;
    }
}
