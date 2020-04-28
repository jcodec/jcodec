package org.jcodec.codecs.h264.encode;

import static java.lang.Math.min;
import static org.jcodec.codecs.h264.encode.H264EncoderUtils.median;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Estimates motion using diagonal search
 * 
 * @author Stanislav Vitvitskyy
 */
public class MotionEstimator {
    private int maxSearchRange;
    private int[] mvTopX;
    private int[] mvTopY;
    private int[] mvTopR;
    private int mvLeftX;
    private int mvLeftY;
    private int mvLeftR;
    private int mvTopLeftX;
    private int mvTopLeftY;
    private int mvTopLeftR;
    private SeqParameterSet sps;
    private Picture ref;

    public MotionEstimator(Picture ref, SeqParameterSet sps, int maxSearchRange) {
        this.sps = sps;
        this.ref = ref;
        mvTopX = new int[sps.picWidthInMbsMinus1 + 1];
        mvTopY = new int[sps.picWidthInMbsMinus1 + 1];
        mvTopR = new int[sps.picWidthInMbsMinus1 + 1];
        this.maxSearchRange = maxSearchRange;
    }

    public int[] mvEstimate(Picture pic, int mbX, int mbY) {
        int refIdx = 1;
        byte[] patch = new byte[256];
        boolean trAvb = mbY > 0 && mbX < sps.picWidthInMbsMinus1;
        boolean tlAvb = mbX > 0 && mbY > 0;

        int ax = mvLeftX;
        int ay = mvLeftY;
        boolean ar = mvLeftR == refIdx;

        int bx = mvTopX[mbX];
        int by = mvTopY[mbX];
        boolean br = mvTopR[mbX] == refIdx;
        
        int cx = trAvb ? mvTopX[mbX + 1] : 0;
        int cy = trAvb ? mvTopY[mbX + 1] : 0;
        boolean cr = trAvb ? mvTopR[mbX + 1] == refIdx : false;
        
        int dx = tlAvb ? mvTopLeftX : 0;
        int dy = tlAvb ? mvTopLeftY : 0;
        boolean dr = tlAvb ? mvTopLeftR == refIdx : false;
        
        int mvpx = median(ax, ar, bx, br, cx, cr, dx, dr, mbX > 0, mbY > 0, trAvb, tlAvb);
        int mvpy = median(ay, ar, by, br, cy, cr, dy, dr, mbX > 0, mbY > 0, trAvb, tlAvb);
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4,
                patch, 16, 16);
        int[] fullPix = estimateFullPix(ref, patch, mbX, mbY, mvpx, mvpy);
        return estimateQPix(ref, patch, fullPix, mbX, mbY);
    }

    private static final int[] SUB_X_OFF = { 0,   -2,  2,  0,  0, -2, -2,  2,  2,   -1, 1,  0,  0,   -1, -2, -1, -2,  1,  2,  1,  2, -1,  1, -1,  1 };
    private static final int[] SUB_Y_OFF = { 0,    0,  0, -2,  2, -2,  2, -2,  2,    0, 0, -1,  1,   -2, -1,  2,  1, -2, -1,  2,  1, -1, -1,  1,  1 };

    public static int[] estimateQPix(Picture ref, byte[] patch, int[] fullPix, int mbX, int mbY) {
        int fullX = (mbX << 4) + (fullPix[0] >> 2);
        int fullY = (mbY << 4) + (fullPix[1] >> 2);
        if (fullX < 3 || fullY < 3)
            return fullPix;
        byte[] sp = new byte[22 * 22];
        MBEncoderHelper.take(ref.getPlaneData(0), ref.getPlaneWidth(0), ref.getPlaneHeight(0), fullX - 3, fullY - 3, sp,
                22, 22);
        // Calculating half pen
        int[] pp = new int[352];
        int[] pn = new int[352];
        int scores[] = new int[25];
        for (int j = 0, dOff = 0, sOff = 0; j < 22; j++) {
            for (int i = 0; i < 16; i++, dOff++, sOff++) {
                {
                    int a = sp[sOff + 0] + sp[sOff + 5];
                    int b = sp[sOff + 1] + sp[sOff + 4];
                    int c = sp[sOff + 2] + sp[sOff + 3];

                    pn[dOff] = a + 5 * ((c << 2) - b);
                }
                {
                    int a = sp[sOff + 1] + sp[sOff + 6];
                    int b = sp[sOff + 2] + sp[sOff + 5];
                    int c = sp[sOff + 3] + sp[sOff + 4];
                    pp[dOff] = a + 5 * ((c << 2) - b);
                }
            }
            sOff += 6;
        }
        for (int j = 0, sof = 0, off = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++, sof++) {
                scores[0] += MathUtil.abs(patch[off] - sp[sof + 69]);
                int horN20 = clip((pn[off + 48] + 16) >> 5, -128, 127);
                int horP20 = clip((pp[off + 48] + 16) >> 5, -128, 127);
                scores[1] += MathUtil.abs(patch[off] - horN20);
                scores[2] += MathUtil.abs(patch[off] - horP20);
                int horN10 = (horN20 + sp[sof + 69] + 1) >> 1;
                int horP10 = (horP20 + sp[sof + 69] + 1) >> 1;
                scores[9] += MathUtil.abs(patch[off] - horN10);
                scores[10] += MathUtil.abs(patch[off] - horP10);
                int verN20;
                {
                    int a = sp[3 + sof + 0]  + sp[3 + sof + 110];
                    int b = sp[3 + sof + 22] + sp[3 + sof + 88];
                    int c = sp[3 + sof + 44] + sp[3 + sof + 66];

                    int verNeg = a + 5 * ((c << 2) - b);
                    verN20 = clip((verNeg + 16) >> 5, -128, 127);
                    int verN10 = (verN20 + sp[sof + 69] + 1) >> 1;
                    int dnn = (verN20 + horN20 + 1) >> 1;
                    int dpn = (verN20 + horP20 + 1) >> 1;
                    scores[3] += MathUtil.abs(patch[off] - verN20);
                    scores[11] += MathUtil.abs(patch[off] - verN10);
                    scores[21] += MathUtil.abs(patch[off] - dnn);
                    scores[22] += MathUtil.abs(patch[off] - dpn);
                }
                int verP20;
                {
                    int a = sp[3 + sof + 22] + sp[3 + sof + 132];
                    int b = sp[3 + sof + 44] + sp[3 + sof + 110];
                    int c = sp[3 + sof + 66] + sp[3 + sof + 88];
                    int verPos = a + 5 * ((c << 2) - b);

                    verP20 = clip((verPos + 16) >> 5, -128, 127);
                    int verP10 = (verP20 + sp[sof + 69] + 1) >> 1;
                    int dnp = (verP20 + horN20 + 1) >> 1;
                    int dpp = (verP20 + horP20 + 1) >> 1;

                    scores[4] += MathUtil.abs(patch[off] - verP20);
                    scores[12] += MathUtil.abs(patch[off] - verP10);
                    scores[23] += MathUtil.abs(patch[off] - dnp);
                    scores[24] += MathUtil.abs(patch[off] - dpp);
                }
                
                {
                    int a = pn[off + 0] + pn[off + 80];
                    int b = pn[off + 16] + pn[off + 64];
                    int c = pn[off + 32] + pn[off + 48];

                    int interpNeg = a + 5 * ((c << 2) - b);
                    int diagNN = clip((interpNeg + 512) >> 10, -128, 127);
                    int ver = (diagNN + verN20 + 1) >> 1;
                    int hor = (diagNN + horN20 + 1) >> 1;
                    
                    scores[5] += MathUtil.abs(patch[off] - diagNN);
                    scores[13] += MathUtil.abs(patch[off] - ver);
                    scores[14] += MathUtil.abs(patch[off] - hor);
                }
                {
                    int a = pn[off + 16] + pn[off + 96];
                    int b = pn[off + 32] + pn[off + 80];
                    int c = pn[off + 48] + pn[off + 64];
                    int interpPos = a + 5 * ((c << 2) - b);

                    int diagNP = clip((interpPos + 512) >> 10, -128, 127);
                    int ver = (diagNP + verP20 + 1) >> 1;
                    int hor = (diagNP + horN20 + 1) >> 1;
                    scores[6] += MathUtil.abs(patch[off] - diagNP);
                    scores[15] += MathUtil.abs(patch[off] - ver);
                    scores[16] += MathUtil.abs(patch[off] - hor);
                }
                {
                    int a = pp[off + 0]  + pp[off + 80];
                    int b = pp[off + 16] + pp[off + 64];
                    int c = pp[off + 32] + pp[off + 48];

                    int interpNeg = a + 5 * ((c << 2) - b);
                    int diagPN = clip((interpNeg + 512) >> 10, -128, 127);
                    int ver = (diagPN + verN20 + 1) >> 1;
                    int hor = (diagPN + horP20 + 1) >> 1;
                    scores[7] += MathUtil.abs(patch[off] - diagPN);
                    scores[17] += MathUtil.abs(patch[off] - ver);
                    scores[18] += MathUtil.abs(patch[off] - hor);
                }
                {
                    int a = pp[off + 16] + pp[off + 96];
                    int b = pp[off + 32] + pp[off + 80];
                    int c = pp[off + 48] + pp[off + 64];
                    int interpPos = a + 5 * ((c << 2) - b);

                    int diagPP = clip((interpPos + 512) >> 10, -128, 127);
                    int ver = (diagPP + verP20 + 1) >> 1;
                    int hor = (diagPP + horP20 + 1) >> 1;
                    scores[8] += MathUtil.abs(patch[off] - diagPP);
                    scores[19] += MathUtil.abs(patch[off] - ver);
                    scores[20] += MathUtil.abs(patch[off] - hor);
                }
            }
            sof += 6;
        }

        
        int m0 = Math.min(scores[1], scores[2]);
        int m1 = Math.min(scores[3], scores[4]);
        int m2 = Math.min(scores[5], scores[6]);
        
        int m3 = Math.min(scores[7], scores[8]);
        int m4 = Math.min(scores[9], scores[10]);
        int m5 = Math.min(scores[11], scores[12]);
        
        int m6 = Math.min(scores[13], scores[14]);
        int m7 = Math.min(scores[15], scores[16]);
        int m8 = Math.min(scores[17], scores[18]);
        
        int m9 = Math.min(scores[19], scores[20]);
        int m10 = Math.min(scores[21], scores[22]);
        int m11 = Math.min(scores[23], scores[24]);
        
        m0 = Math.min(m0, m1);
        m2 = Math.min(m2, m3);
        m4 = Math.min(m4, m5);
        m6 = Math.min(m6, m7);
        m8 = Math.min(m8, m9);
        m10 = Math.min(m10, m11);
        
        m0 = Math.min(m0, m2);
        m4 = Math.min(m4, m6);
        m8 = Math.min(m8, m10);
        
        int mf0 = Math.min(scores[0], m0);
        int mf1 = Math.min(m4, m8);
        int mf2 = Math.min(mf0, mf1);

        int sel = 0;
        for (int i = 0; i < 25; i++) {
            if (mf2 == scores[i]) {
                sel = i;
                break;
            }
        }

        return new int[] { fullPix[0] + SUB_X_OFF[sel], fullPix[1] + SUB_Y_OFF[sel] };
    }

    public void mvSave(int mbX, int mbY, int[] mv) {
        mvTopLeftX = mvTopX[mbX];
        mvTopLeftY = mvTopY[mbX];
        mvTopLeftR = mvTopR[mbX];
        mvTopX[mbX] = mv[0];
        mvTopY[mbX] = mv[1];
        mvTopR[mbX] = mv[2];
        mvLeftX = mv[0];
        mvLeftY = mv[1];
        mvLeftR = mv[2];
    }

    private int[] estimateFullPix(Picture ref, byte[] patch, int mbX, int mbY, int mvpx, int mvpy) {
        byte[] searchPatch = new byte[(maxSearchRange * 2 + 16) * (maxSearchRange * 2 + 16)];

        int mvX0 = 0, mvX1 = 0, mvY0 = 0, mvY1 = 0, mvS0 = 0, mvS1 = 0;
        // Search area 0: mb position
        int startX = (mbX << 4);
        int startY = (mbY << 4);
        for (int area = 0; area < 2; area++) {
            int patchTlX = Math.max(startX - maxSearchRange, 0);
            int patchTlY = Math.max(startY - maxSearchRange, 0);
            int patchBrX = Math.min(startX + maxSearchRange + 16, ref.getPlaneWidth(0));
            int patchBrY = Math.min(startY + maxSearchRange + 16, ref.getPlaneHeight(0));
    
            int inPatchX = startX - patchTlX;
            int inPatchY = startY - patchTlY;
            int patchW = patchBrX - patchTlX;
            int patchH = patchBrY - patchTlY;
            // TODO: border fill?
            MBEncoderHelper.takeSafe(ref.getPlaneData(0), ref.getPlaneWidth(0), ref.getPlaneHeight(0), patchTlX, patchTlY,
                    searchPatch, patchW, patchH);
    
            int bestMvX = inPatchX, bestMvY = inPatchY;
            int bestScore = sad(searchPatch, patchW, patch, bestMvX, bestMvY);
            // Diagonal search
            for (int i = 0; i < maxSearchRange; i++) {
                int score1 = bestMvX > 0 ? sad(searchPatch, patchW, patch, bestMvX - 1, bestMvY) : Integer.MAX_VALUE;
                int score2 = bestMvX < patchW - 1 ? sad(searchPatch, patchW, patch, bestMvX + 1, bestMvY)
                        : Integer.MAX_VALUE;
                int score3 = bestMvY > 0 ? sad(searchPatch, patchW, patch, bestMvX, bestMvY - 1) : Integer.MAX_VALUE;
                int score4 = bestMvY < patchH - 1 ? sad(searchPatch, patchW, patch, bestMvX, bestMvY + 1)
                        : Integer.MAX_VALUE;
                int min = min(min(min(score1, score2), score3), score4);
                if (min > bestScore)
                    break;
                bestScore = min;
                if (score1 == min) {
                    --bestMvX;
                } else if (score2 == min) {
                    ++bestMvX;
                } else if (score3 == min) {
                    --bestMvY;
                } else {
                    ++bestMvY;
                }
            }
            if (area == 0) {
              mvX0 = ((bestMvX - inPatchX) << 2);
              mvY0 = ((bestMvY - inPatchY) << 2);
              mvS0 = bestScore;
            } else {
              mvX1 = ((bestMvX - inPatchX) << 2) + mvpx;
              mvY1 = ((bestMvY - inPatchY) << 2) + mvpy;
              mvS1 = bestScore;
            }
            
            // Search area 1: mb predictor
            startX = (mbX << 4) + (mvpx >> 2);
            startY = (mbY << 4) + (mvpy >> 2);
            // for now
            break;
        }

        return new int[] { mvS0 < mvS1 ? mvX0 : mvX1, mvS0 < mvS1 ? mvY0 : mvY1};
    }

    private int sad(byte[] big, int bigStride, byte[] small, int offX, int offY) {
        int score = 0, bigOff = offY * bigStride + offX, smallOff = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++, ++bigOff, ++smallOff) {
                score += MathUtil.abs(big[bigOff] - small[smallOff]);
            }
            bigOff += bigStride - 16;
        }
        return score;
    }
}