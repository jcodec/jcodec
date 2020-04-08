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

        int cx = trAvb ? mvTopX[mbX + 1] : 0;
        int cy = trAvb ? mvTopY[mbX + 1] : 0;
        boolean cr = trAvb ? mvTopR[mbX + 1] == refIdx : false;
        int dx = tlAvb ? mvTopLeftX : 0;
        int dy = tlAvb ? mvTopLeftY : 0;
        boolean dr = tlAvb ? mvTopLeftY == refIdx : false;
        int mvpx = median(mvLeftX, mvLeftR == refIdx, mvTopX[mbX], mvTopR[mbX] == refIdx, cx, cr, dx, dr, mbX > 0,
                mbY > 0, trAvb, tlAvb);
        int mvpy = median(mvLeftY, mvLeftR == refIdx, mvTopY[mbX], mvTopR[mbX] == refIdx, cy, cr, dy, dr, mbX > 0,
                mbY > 0, trAvb, tlAvb);
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4,
                patch, 16, 16);
        int[] fullPix = estimateFullPix(ref, patch, mbX, mbY, mvpx, mvpy);
        return estimateQPix(ref, patch, fullPix, mbX, mbY);
    }

    private static final int[] SUB_X_OFF = { 0, -2, 2, 0, 0, -2, -2, 2, 2 };
    private static final int[] SUB_Y_OFF = { 0, 0, 0, -2, 2, -2, 2, -2, 2 };

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
        int scores[] = new int[9];
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
                scores[1] += MathUtil.abs(patch[off] - clip((pn[off + 48] + 16) >> 5, -128, 127));
                scores[2] += MathUtil.abs(patch[off] - clip((pp[off + 48] + 16) >> 5, -128, 127));
                {
                    int a = sp[3 + sof + 0] + sp[3 + sof + 110];
                    int b = sp[3 + sof + 22] + sp[3 + sof + 88];
                    int c = sp[3 + sof + 44] + sp[3 + sof + 66];

                    int verNeg = a + 5 * ((c << 2) - b);
                    scores[3] += MathUtil.abs(patch[off] - clip((verNeg + 16) >> 5, -128, 127));
                    //1 -5 20 20 -5 1
                }
                {
                    int a = sp[3 + sof + 22] + sp[3 + sof + 132];
                    int b = sp[3 + sof + 44] + sp[3 + sof + 110];
                    int c = sp[3 + sof + 66] + sp[3 + sof + 88];
                    int verPos = a + 5 * ((c << 2) - b);

                    scores[4] += MathUtil.abs(patch[off] - clip((verPos + 16) >> 5, -128, 127));
                }
                {
                    int a = pn[off + 0] + pn[off + 80];
                    int b = pn[off + 16] + pn[off + 64];
                    int c = pn[off + 32] + pn[off + 48];

                    int interpNeg = a + 5 * ((c << 2) - b);
                    scores[5] += MathUtil.abs(patch[off] - clip((interpNeg + 512) >> 10, -128, 127));
                }
                {
                    int a = pn[off + 16] + pn[off + 96];
                    int b = pn[off + 32] + pn[off + 80];
                    int c = pn[off + 48] + pn[off + 64];
                    int interpPos = a + 5 * ((c << 2) - b);

                    scores[6] += MathUtil.abs(patch[off] - clip((interpPos + 512) >> 10, -128, 127));
                }
                {
                    int a = pp[off + 0] + pp[off + 80];
                    int b = pp[off + 16] + pp[off + 64];
                    int c = pp[off + 32] + pp[off + 48];

                    int interpNeg = a + 5 * ((c << 2) - b);
                    scores[7] += MathUtil.abs(patch[off] - clip((interpNeg + 512) >> 10, -128, 127));
                }
                {
                    int a = pp[off + 16] + pp[off + 96];
                    int b = pp[off + 32] + pp[off + 80];
                    int c = pp[off + 48] + pp[off + 64];
                    int interpPos = a + 5 * ((c << 2) - b);

                    scores[8] += MathUtil.abs(patch[off] - clip((interpPos + 512) >> 10, -128, 127));
                }
            }
            sof += 6;
        }

        int m0 = Math.min(scores[1], scores[2]);
        int m1 = Math.min(scores[3], scores[4]);
        int m2 = Math.min(scores[5], scores[6]);
        int m3 = Math.min(scores[7], scores[8]);
        int m4 = Math.min(m0, m1);
        int m5 = Math.min(m2, m3);
        int m6 = Math.min(m4, m5);
        int mf = Math.min(m6, scores[0]);

        int sel = 0;
        for (int i = 0; i < 9; i++) {
            if (mf == scores[i]) {
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

        int startX = (mbX << 4) /* + (mvpx >> 2) */;
        int startY = (mbY << 4) /* + (mvpy >> 2) */;

        int patchTlX = Math.max(startX - maxSearchRange, 0);
        int patchTlY = Math.max(startY - maxSearchRange, 0);
        int patchBrX = Math.min(startX + maxSearchRange + 16, ref.getPlaneWidth(0));
        int patchBrY = Math.min(startY + maxSearchRange + 16, ref.getPlaneHeight(0));

        int centerX = startX - patchTlX;
        int centerY = startY - patchTlY;

        int patchW = patchBrX - patchTlX;
        int patchH = patchBrY - patchTlY;
        MBEncoderHelper.takeSafe(ref.getPlaneData(0), ref.getPlaneWidth(0), ref.getPlaneHeight(0), patchTlX, patchTlY,
                searchPatch, patchW, patchH);

        int bestMvX = centerX, bestMvY = centerY;
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

        return new int[] { ((bestMvX - centerX) << 2)/* + mvpx */, ((bestMvY - centerY) << 2)/* + mvpy */ };
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