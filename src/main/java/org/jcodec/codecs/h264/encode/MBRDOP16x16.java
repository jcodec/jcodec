package org.jcodec.codecs.h264.encode;

import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_LEFT;
import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_TOP;
import static org.jcodec.codecs.h264.encode.H264EncoderUtils.median;
import static org.jcodec.codecs.h264.encode.H264EncoderUtils.mse;
import static org.jcodec.codecs.h264.io.model.MBType.P_16x16;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.decode.BlockInterpolator;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Calculates RD score for a macroblock when encoded with P16x16 mode
 * 
 * @author Stanislav Vitvitskyy
 */
public class MBRDOP16x16 {

    private CAVLCRate[] cavlc;
    private SeqParameterSet sps;
    private Picture ref;
    private int[] mvTopX;
    private int[] mvTopY;
    private int mvLeftX;
    private int mvLeftY;
    private int mvTopLeftX;
    private int mvTopLeftY;

    private BlockInterpolator interpolator;
    private int lambda;

    public MBRDOP16x16(SeqParameterSet sps, Picture ref, CAVLCRate[] cavlc, int lambda) {
        this.sps    = sps;
        this.ref    = ref;
        this.cavlc  = cavlc;
        this.lambda = lambda;
        mvTopX      = new int[sps.picWidthInMbsMinus1 + 1];
        mvTopY      = new int[sps.picWidthInMbsMinus1 + 1];
      
        interpolator = new BlockInterpolator();
    }
    
    public int rateMacroblock(Picture pic, int mbX, int mbY, int qp, int qpDelta, int[] mv) {
        int rate = 0;
        if (sps.numRefFrames > 1) {
            int refIdx = decideRef();
            rate += CAVLCRate.rateTE(refIdx, sps.numRefFrames - 1);
        }

        boolean trAvb = mbY > 0 && mbX < sps.picWidthInMbsMinus1;
        boolean tlAvb = mbX > 0 && mbY > 0;
        int mvpx = median(mvLeftX, mvTopX[mbX], trAvb ? mvTopX[mbX + 1] : 0, tlAvb ? mvTopLeftX : 0, mbX > 0, mbY > 0,
                trAvb, tlAvb);
        int mvpy = median(mvLeftY, mvTopY[mbX], trAvb ? mvTopY[mbX + 1] : 0, tlAvb ? mvTopLeftY : 0, mbX > 0, mbY > 0,
                trAvb, tlAvb);

        // Motion estimation for the current macroblock
        mvTopLeftX = mvTopX[mbX];
        mvTopLeftY = mvTopY[mbX];
        mvTopX[mbX] = mv[0];
        mvTopY[mbX] = mv[1];
        mvLeftX = mv[0];
        mvLeftY = mv[1];
        rate += CAVLCRate.rateSE(mv[0] - mvpx); // mvdx
        rate += CAVLCRate.rateSE(mv[1] - mvpy); // mvdy

        Picture mbRef = Picture.create(16, 16, sps.chromaFormatIdc);
        int[][] mb = new int[][] { new int[256], new int[64], new int[64] };
        int[][] mbo = new int[][] { new int[256], new int[64], new int[64] };

        interpolator.getBlockLuma(ref, mbRef, 0, (mbX << 6) + mv[0], (mbY << 6) + mv[1], 16, 16);

        BlockInterpolator.getBlockChroma(ref.getPlaneData(1), ref.getPlaneWidth(1), ref.getPlaneHeight(1),
                mbRef.getPlaneData(1), 0, mbRef.getPlaneWidth(1), (mbX << 6) + mv[0], (mbY << 6) + mv[1], 8, 8);
        BlockInterpolator.getBlockChroma(ref.getPlaneData(2), ref.getPlaneWidth(2), ref.getPlaneHeight(2),
                mbRef.getPlaneData(2), 0, mbRef.getPlaneWidth(2), (mbX << 6) + mv[0], (mbY << 6) + mv[1], 8, 8);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4,
                mbY << 4, mb[0], mbRef.getPlaneData(0), 16, 16);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(1), pic.getPlaneWidth(1), pic.getPlaneHeight(1), mbX << 3,
                mbY << 3, mb[1], mbRef.getPlaneData(1), 8, 8);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(2), pic.getPlaneWidth(2), pic.getPlaneHeight(2), mbX << 3,
                mbY << 3, mb[2], mbRef.getPlaneData(2), 8, 8);

        int codedBlockPattern = getCodedBlockPattern();
        rate += CAVLCRate.rateUE(H264Const.CODED_BLOCK_PATTERN_INTER_COLOR_INV[codedBlockPattern]);
        rate += CAVLCRate.rateSE(qpDelta);

        rate += luma(mb[0], mbo[0], mbX, mbY, qp);
        rate += chroma(mb[1], mb[2], mbo[1], mbo[2], mbX, mbY, qp);
        
        int dist = mse(mb[0], mbo[0], 16, 16);
        dist += mse(mb[1], mbo[1], 8, 8);
        dist += mse(mb[2], mbo[2], 8, 8);
        
        return rate + lambda * dist;
    }

    private int getCodedBlockPattern() {
        return 47;
    }

    /**
     * Decides which reference to use
     * 
     * @return
     */
    private int decideRef() {
        return 0;
    }

    private int luma(int[] pix, int[] pixo, int mbX, int mbY, int qp) {
        int[][] ac = new int[16][16];
        for (int i = 0; i < ac.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_4x4[i].length; j++) {
                ac[i][j] = pix[H264Const.PIX_MAP_SPLIT_4x4[i][j]];
            }
            CoeffTransformer.fdct4x4(ac[i]);
        }

        int rate = rateAC(0, mbX, mbY, mbX << 2, mbY << 2, ac, qp);

        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.dequantizeAC(ac[i], qp, null);
            CoeffTransformer.idct4x4(ac[i]);
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_4x4[i].length; j++)
                pixo[H264Const.PIX_MAP_SPLIT_4x4[i][j]] = ac[i][j];
        }
        return rate;
    }

    private int chroma(int[] pix1, int[] pix2, int[] pixo1, int[] pixo2, int mbX, int mbY, int qp) {
        int[][] ac1 = new int[4][16];
        int[][] ac2 = new int[4][16];
        for (int i = 0; i < ac1.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                ac1[i][j] = pix1[H264Const.PIX_MAP_SPLIT_2x2[i][j]];
        }
        for (int i = 0; i < ac2.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                ac2[i][j] = pix2[H264Const.PIX_MAP_SPLIT_2x2[i][j]];
        }
        int rate = MBRDOI16x16.chromaResidual(mbX, mbY, qp, ac1, ac2, cavlc[1], cavlc[2], P_16x16, P_16x16);

        for (int i = 0; i < ac1.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                pixo1[H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac1[i][j];
        }
        for (int i = 0; i < ac2.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                pixo2[H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac2[i][j];
        }
        return rate;
    }

    private int rateAC(int comp, int mbX, int mbY, int mbLeftBlk, int mbTopBlk, int[][] ac, int qp) {
        int rate = 0;
        for (int i = 0; i < ac.length; i++) {
            int blkI = H264Const.BLK_INV_MAP[i];
            CoeffTransformer.quantizeAC(ac[blkI], qp);
            rate += cavlc[comp].rateACBlock(mbLeftBlk + MB_BLK_OFF_LEFT[i], mbTopBlk + MB_BLK_OFF_TOP[i], P_16x16,
                    P_16x16, ac[blkI], H264Const.totalZeros16, 0, 16, CoeffTransformer.zigzag4x4);
        }
        return rate;
    }
}