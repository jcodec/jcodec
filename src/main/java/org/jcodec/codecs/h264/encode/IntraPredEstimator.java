package org.jcodec.codecs.h264.encode;

import static org.jcodec.codecs.h264.H264Const.BLK_DISP_MAP;
import static org.jcodec.common.tools.MathUtil.clip;

import java.util.Arrays;

import org.jcodec.codecs.h264.decode.ChromaPredictionBuilder;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.decode.Intra16x16PredictionBuilder;
import org.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IntraPredEstimator {
    public static int[] getLumaPred4x4(Picture pic, EncodingContext ctx, int mbX, int mbY, int qp) {
        byte[] patch = new byte[256];
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4,
                patch, 16, 16);
        int[] predModes = new int[16];
        byte[] predLeft = Arrays.copyOf(ctx.leftRow[0], 16);
        byte[] predTop = Arrays.copyOfRange(ctx.topLine[0], mbX << 4,
                (mbX << 4) + 16 + (mbX < ctx.mbWidth - 1 ? 4 : 0));
        byte[] predTopLeft = new byte[] { ctx.topLeft[0], ctx.leftRow[0][3], ctx.leftRow[0][7], ctx.leftRow[0][11] };
        int[] resi = new int[16];
        byte[] pred = new byte[16];
        int[] bresi = new int[16];
        byte[] bpred = new byte[16];

        for (int bInd = 0; bInd < 16; bInd++) {
            int minSad = Integer.MAX_VALUE;

            int dInd = BLK_DISP_MAP[bInd];
            boolean hasLeft = (dInd & 0x3) != 0 || mbX != 0;
            boolean hasTop = dInd >= 4 || mbY != 0;
            boolean hasTr = ((bInd == 0 || bInd == 1 || bInd == 4) && mbY != 0) || (bInd == 5 && mbX < ctx.mbWidth - 1)
                    || bInd == 2 || bInd == 6 || bInd == 8 || bInd == 9 || bInd == 10 || bInd == 12 || bInd == 14;
            predModes[bInd] = 2;
            int blkX = (dInd & 0x3) << 2;
            int blkY = (dInd >> 2) << 2;

            for (int predType = 0; predType < 9; predType++) {
                boolean available = Intra4x4PredictionBuilder.lumaPred(predType, hasLeft, hasTop, hasTr, predLeft,
                        predTop, predTopLeft[dInd >> 2], blkX, blkY, pred);

                if (available) {
                    int sad = 0;
                    for (int i = 0; i < 16; i++) {
                        int x = blkX + (i & 0x3);
                        int y = blkY + (i >> 2);
                        resi[i] = patch[(y << 4) + x] - pred[i];
                        sad += MathUtil.abs(resi[i]);
                    }

                    if (sad < minSad) {
                        minSad = sad;
                        predModes[bInd] = predType;

                        // Distort coeffs
                        CoeffTransformer.fdct4x4(resi);
                        CoeffTransformer.quantizeAC(resi, qp);
                        CoeffTransformer.dequantizeAC(resi, qp, null);
                        CoeffTransformer.idct4x4(resi);
                        System.arraycopy(pred, 0, bpred, 0, 16);
                        System.arraycopy(resi, 0, bresi, 0, 16);
                    }
                }
            }
            predTopLeft[dInd >> 2] = predTop[blkX + 3];
            for (int p = 0; p < 4; p++) {
                predLeft[blkY + p] = (byte) clip(bresi[3 + (p << 2)] + bpred[3 + (p << 2)], -128, 127);
                predTop[blkX + p] = (byte) clip(bresi[12 + p] + bpred[12 + p], -128, 127);
            }
        }
        return predModes;
    }

    public static int getLumaMode(Picture pic, EncodingContext ctx, int mbX, int mbY) {
        byte[] patch = new byte[256];
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4,
                patch, 16, 16);
        int minSad = Integer.MAX_VALUE;
        int predMode = -1;
        for (int predType = 0; predType < 4; predType++) {
            int sad = Intra16x16PredictionBuilder.lumaPredSAD(predType, mbX != 0, mbY != 0, ctx.leftRow[0],
                    ctx.topLine[0], ctx.topLeft[0], mbX << 4, patch);
            if (sad < minSad) {
                minSad = sad;
                predMode = predType;
            }
        }
        return predMode;
    }

    public static int getChromaMode(Picture pic, EncodingContext ctx, int mbX, int mbY) {
        byte[] patch0 = new byte[64];
        byte[] patch1 = new byte[64];
        MBEncoderHelper.take(pic.getPlaneData(1), pic.getPlaneWidth(1), pic.getPlaneHeight(1), mbX << 3, mbY << 3,
                patch0, 8, 8);
        MBEncoderHelper.take(pic.getPlaneData(2), pic.getPlaneWidth(2), pic.getPlaneHeight(2), mbX << 3, mbY << 3,
                patch1, 8, 8);
        int minSad = Integer.MAX_VALUE;
        int predMode = -1;
        for (int predType = 0; predType < 4; predType++) {
            if(!ChromaPredictionBuilder.predAvb(predType, mbX != 0, mbY != 0))
                continue;
            int sad0 = ChromaPredictionBuilder.predSAD(predType, mbX, mbX != 0, mbY != 0, ctx.leftRow[1],
                    ctx.topLine[1], ctx.topLeft[1], patch0);
            int sad1 = ChromaPredictionBuilder.predSAD(predType, mbX, mbX != 0, mbY != 0, ctx.leftRow[2],
                    ctx.topLine[2], ctx.topLeft[2], patch1);
            if (sad0 + sad1 < minSad) {
                minSad = sad0 + sad1;
                predMode = predType;
            }
        }
        return predMode;
    }
}
