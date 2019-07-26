package org.jcodec.codecs.h264.encode;

import static java.lang.Math.min;
import static org.jcodec.codecs.h264.encode.H264EncoderUtils.median;

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
    private int mvLeftX;
    private int mvLeftY;
    private int mvTopLeftX;
    private int mvTopLeftY;
    private SeqParameterSet sps;
    private Picture ref;

    public MotionEstimator(Picture ref, SeqParameterSet sps, int maxSearchRange) {
        this.sps = sps;
        this.ref = ref;
        mvTopX     = new int[sps.picWidthInMbsMinus1 + 1];
        mvTopY     = new int[sps.picWidthInMbsMinus1 + 1];
        this.maxSearchRange = maxSearchRange;
    }
    
    public int[] mvEstimate(Picture pic, int mbX, int mbY) {
        byte[] patch = new byte[256];
        boolean trAvb = mbY > 0 && mbX < sps.picWidthInMbsMinus1;
        boolean tlAvb = mbX > 0 && mbY > 0;
        int mvpx = median(mvLeftX, mvTopX[mbX], trAvb ? mvTopX[mbX + 1] : 0, tlAvb ? mvTopLeftX : 0, mbX > 0, mbY > 0,
                trAvb, tlAvb);
        int mvpy = median(mvLeftY, mvTopY[mbX], trAvb ? mvTopY[mbX + 1] : 0, tlAvb ? mvTopLeftY : 0, mbX > 0, mbY > 0,
                trAvb, tlAvb);
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4,
                patch, 16, 16);
        return estimateFullPix(ref, patch, mbX, mbY, mvpx, mvpy);
    }
    
    public void mvSave(int mbX, int mbY, int[] mv) {
        mvTopLeftX = mvTopX[mbX];
        mvTopLeftY = mvTopY[mbX];
        mvTopX[mbX] = mv[0];
        mvTopY[mbX] = mv[1];
        mvLeftX = mv[0];
        mvLeftY = mv[1];
    }

    private int[] estimateFullPix(Picture ref, byte[] patch, int mbX, int mbY, int mvpx, int mvpy) {
        byte[] searchPatch = new byte[(maxSearchRange * 2 + 16) * (maxSearchRange * 2 + 16)];

        int startX = (mbX << 4) /* + (mvpx >> 2)*/;
        int startY = (mbY << 4) /* + (mvpy >> 2)*/;

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
            int score2 = bestMvX < patchW - 1 ? sad(searchPatch, patchW, patch, bestMvX + 1, bestMvY) : Integer.MAX_VALUE;
            int score3 = bestMvY > 0 ? sad(searchPatch, patchW, patch, bestMvX, bestMvY - 1) : Integer.MAX_VALUE;
            int score4 = bestMvY < patchH - 1 ? sad(searchPatch, patchW, patch, bestMvX, bestMvY + 1) : Integer.MAX_VALUE;
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

        return new int[] { ((bestMvX - centerX) << 2)/* + mvpx*/, ((bestMvY - centerY) << 2)/* + mvpy*/ };
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