package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.BLK8x8_BLOCKS;
import static org.jcodec.codecs.h264.H264Const.BLK_4x4_MB_OFF_LUMA;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_IND;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_MB_OFF_LUMA;
import static org.jcodec.codecs.h264.H264Const.BLK_INV_MAP;
import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_8x8_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_8x8_LUT;
import static org.jcodec.codecs.h264.H264Const.identityMapping4;
import static org.jcodec.codecs.h264.H264Const.PartPred.Bi;
import static org.jcodec.codecs.h264.H264Const.PartPred.Direct;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.H264Const.PartPred.L1;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.calcMVPredictionMedian;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.mergeResidual;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvs;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.savePrediction8x8;
import static org.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;
import static org.jcodec.common.tools.MathUtil.abs;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MathUtil;

/**
 * A decoder for B direct macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderBDirect extends MBlockDecoderBase {
    private Mapper mapper;

    public MBlockDecoderBDirect(Mapper mapper, BitstreamParser parser, SliceHeader sh, DeblockerInput di, int poc,
            DecoderState decoderState) {
        super(parser, sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode(int mbIdx, boolean field, MBType prevMbType, Picture8Bit mb, Frame[][] references) {

        MBlock mBlock = new MBlock();
        
        readMBlockBDirect(mbIdx, prevMbType, mBlock);

        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean lAvb = mapper.leftAvailable(mbIdx);
        boolean tAvb = mapper.topAvailable(mbIdx);
        int mbAddr = mapper.getAddress(mbIdx);
        boolean tlAvb = mapper.topLeftAvailable(mbIdx);
        boolean trAvb = mapper.topRightAvailable(mbIdx);
        int[][][] x = new int[2][16][3];
        for (int i = 0; i < 16; i++)
            x[0][i][2] = x[1][i][2] = -1;

        PartPred[] pp = new PartPred[4];

        predictBDirect(references, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, identityMapping4);

        predictChromaInter(references, x, mbX << 3, mbY << 3, 1, mb, pp);
        predictChromaInter(references, x, mbX << 3, mbY << 3, 2, mb, pp);

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock, lAvb, tAvb, mbX, mbY, MBType.B_Direct_16x16, mBlock.transform8x8Used, s.tf8x8Left,
                s.tf8x8Top[mbX]);

        savePrediction8x8(s, mbX, x[0], 0);
        savePrediction8x8(s, mbX, x[1], 1);
        saveMvs(di, x, mbX, mbY);

        int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
        int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

        decodeChromaResidual(mBlock, lAvb, tAvb, mbX, mbY, mBlock.cbpChroma(), qp1, qp2, MBType.B_Direct_16x16);

        di.mbQps[1][mbAddr] = qp1;
        di.mbQps[2][mbAddr] = qp2;

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[mbAddr] = s.topMBType[mbX] = s.leftMBType = MBType.B_Direct_16x16;
        s.topCBPLuma[mbX] = s.leftCBPLuma = mBlock.cbpLuma();
        s.topCBPChroma[mbX] = s.leftCBPChroma = mBlock.cbpChroma();
        s.tf8x8Left = s.tf8x8Top[mbX] = mBlock.transform8x8Used;
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
        s.predModeTop[mbX << 1] = s.predModeTop[(mbX << 1) + 1] = s.predModeLeft[0] = s.predModeLeft[1] = Direct;
    }

    private void readMBlockBDirect(int mbIdx, MBType prevMbType, MBlock mBlock) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean lAvb = mapper.leftAvailable(mbIdx);
        boolean tAvb = mapper.topAvailable(mbIdx);
        mBlock.cbp = parser.readCodedBlockPatternInter(lAvb, tAvb, s.leftCBPLuma | (s.leftCBPChroma << 4),
                s.topCBPLuma[mbX] | (s.topCBPChroma[mbX] << 4), s.leftMBType, s.topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (s.transform8x8 && mBlock.cbpLuma() != 0 && sh.sps.direct_8x8_inference_flag) {
            mBlock.transform8x8Used = parser.readTransform8x8Flag(lAvb, tAvb, s.leftMBType, s.topMBType[mbX],
                    s.tf8x8Left, s.tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = parser.readMBQpDelta(prevMbType);
        }
        readResidualLuma(mBlock, lAvb, tAvb, mbX, mbY, MBType.B_Direct_16x16, mBlock.transform8x8Used);
        readChromaResidual(mBlock, lAvb, tAvb, mbX, mBlock.cbpChroma(), MBType.B_Direct_16x16);
    }

    public void predictBDirect(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int[][][] x, PartPred[] pp, Picture8Bit mb, int[] blocks) {
        if (sh.direct_spatial_mv_pred_flag)
            predictBSpatialDirect(refs, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, blocks);
        else
            predictBTemporalDirect(refs, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, blocks);
    }

    private void predictBTemporalDirect(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int[][][] x, PartPred[] pp, Picture8Bit mb, int[] blocks8x8) {

        Picture8Bit mb0 = Picture8Bit.create(16, 16, s.chromaFormat), mb1 = Picture8Bit.create(16, 16, s.chromaFormat);
        for (int blk8x8 : blocks8x8) {
            int blk4x4_0 = H264Const.BLK8x8_BLOCKS[blk8x8][0];
            pp[blk8x8] = Bi;

            if (!sh.sps.direct_8x8_inference_flag)
                for (int blk4x4 : BLK8x8_BLOCKS[blk8x8]) {
                    predTemp4x4(refs, mbX, mbY, x, blk4x4);

                    int blkIndX = blk4x4 & 3;
                    int blkIndY = blk4x4 >> 2;

                    debugPrint("DIRECT_4x4 [" + blkIndY + ", " + blkIndX + "]: (" + x[0][blk4x4][0] + ","
                            + x[0][blk4x4][1] + "," + x[0][blk4x4][2] + "), (" + x[1][blk4x4][0] + ","
                            + x[1][blk4x4][1] + "," + x[1][blk4x4][2] + ")");

                    int blkPredX = (mbX << 6) + (blkIndX << 4);
                    int blkPredY = (mbY << 6) + (blkIndY << 4);

                    BlockInterpolator.getBlockLuma(refs[0][x[0][blk4x4][2]], mb0, BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x[0][blk4x4][0], blkPredY + x[0][blk4x4][1], 4, 4);
                    BlockInterpolator.getBlockLuma(refs[1][0], mb1, BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x[1][blk4x4][0], blkPredY + x[1][blk4x4][1], 4, 4);
                }
            else {
                int blk4x4Pred = BLK_INV_MAP[blk8x8 * 5];
                predTemp4x4(refs, mbX, mbY, x, blk4x4Pred);
                propagatePred(x, blk8x8, blk4x4Pred);

                int blkIndX = blk4x4_0 & 3;
                int blkIndY = blk4x4_0 >> 2;

                debugPrint("DIRECT_8x8 [" + blkIndY + ", " + blkIndX + "]: (" + x[0][blk4x4_0][0] + ","
                        + x[0][blk4x4_0][1] + "," + x[0][blk4x4_0][2] + "), (" + x[1][blk4x4_0][0] + ","
                        + x[1][blk4x4_0][1] + "," + x[0][blk4x4_0][2] + ")");

                int blkPredX = (mbX << 6) + (blkIndX << 4);
                int blkPredY = (mbY << 6) + (blkIndY << 4);

                BlockInterpolator.getBlockLuma(refs[0][x[0][blk4x4_0][2]], mb0, BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x[0][blk4x4_0][0], blkPredY + x[0][blk4x4_0][1], 8, 8);
                BlockInterpolator.getBlockLuma(refs[1][0], mb1, BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x[1][blk4x4_0][0], blkPredY + x[1][blk4x4_0][1], 8, 8);
            }
            mergePrediction(sh, x[0][blk4x4_0][2], x[1][blk4x4_0][2], Bi, 0, mb0.getPlaneData(0), mb1.getPlaneData(0),
                    BLK_4x4_MB_OFF_LUMA[blk4x4_0], 16, 8, 8, mb.getPlaneData(0), refs, poc);
        }
    }

    private void predTemp4x4(Frame[][] refs, int mbX, int mbY, int[][][] x, int blk4x4) {
        int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;

        Frame picCol = refs[1][0];
        int blkIndX = blk4x4 & 3;
        int blkIndY = blk4x4 >> 2;

        int blkPosX = (mbX << 2) + blkIndX;
        int blkPosY = (mbY << 2) + blkIndY;

        int[] mvCol = picCol.getMvs()[0][blkPosY][blkPosX];
        Frame refL0;
        int refIdxL0;
        if (mvCol[2] == -1) {
            mvCol = picCol.getMvs()[1][blkPosY][blkPosX];
            if (mvCol[2] == -1) {
                refIdxL0 = 0;
                refL0 = refs[0][0];
            } else {
                refL0 = picCol.getRefsUsed()[mbY * mbWidth + mbX][1][mvCol[2]];
                refIdxL0 = findPic(refs[0], refL0);
            }
        } else {
            refL0 = picCol.getRefsUsed()[mbY * mbWidth + mbX][0][mvCol[2]];
            refIdxL0 = findPic(refs[0], refL0);
        }

        x[0][blk4x4][2] = refIdxL0;
        x[1][blk4x4][2] = 0;

        int td = MathUtil.clip(picCol.getPOC() - refL0.getPOC(), -128, 127);
        if (!refL0.isShortTerm() || td == 0) {
            x[0][blk4x4][0] = mvCol[0];
            x[0][blk4x4][1] = mvCol[1];
            x[1][blk4x4][0] = 0;
            x[1][blk4x4][1] = 0;
        } else {
            int tb = MathUtil.clip(poc - refL0.getPOC(), -128, 127);
            int tx = (16384 + Math.abs(td / 2)) / td;
            int dsf = clip((tb * tx + 32) >> 6, -1024, 1023);

            x[0][blk4x4][0] = (dsf * mvCol[0] + 128) >> 8;
            x[0][blk4x4][1] = (dsf * mvCol[1] + 128) >> 8;
            x[1][blk4x4][0] = (x[0][blk4x4][0] - mvCol[0]);
            x[1][blk4x4][1] = (x[0][blk4x4][1] - mvCol[1]);
        }
    }

    private int findPic(Frame[] frames, Frame refL0) {
        for (int i = 0; i < frames.length; i++)
            if (frames[i] == refL0)
                return i;
        Logger.error("RefPicList0 shall contain refPicCol");
        return 0;
    }

    private void predictBSpatialDirect(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int[][][] x, PartPred[] pp, Picture8Bit mb, int[] blocks8x8) {

        int[] a0 = s.mvLeft[0][0], a1 = s.mvLeft[1][0];
        int[] b0 = s.mvTop[0][mbX << 2], b1 = s.mvTop[1][mbX << 2];
        int[] c0 = s.mvTop[0][(mbX << 2) + 4], c1 = s.mvTop[1][(mbX << 2) + 4];
        int[] d0 = s.mvTopLeft[0];
        int[] d1 = s.mvTopLeft[1];

        int refIdxL0 = calcRef(a0, b0, c0, d0, lAvb, tAvb, tlAvb, trAvb, mbX);
        int refIdxL1 = calcRef(a1, b1, c1, d1, lAvb, tAvb, tlAvb, trAvb, mbX);

        Picture8Bit mb0 = Picture8Bit.create(16, 16, s.chromaFormat), mb1 = Picture8Bit.create(16, 16, s.chromaFormat);

        if (refIdxL0 < 0 && refIdxL1 < 0) {
            for (int blk8x8 : blocks8x8) {
                for (int blk4x4 : BLK8x8_BLOCKS[blk8x8]) {
                    x[0][blk4x4][0] = x[0][blk4x4][1] = x[0][blk4x4][2] = x[1][blk4x4][0] = x[1][blk4x4][1] = x[1][blk4x4][2] = 0;
                }
                pp[blk8x8] = Bi;

                int blkOffX = (blk8x8 & 1) << 5;
                int blkOffY = (blk8x8 >> 1) << 5;
                BlockInterpolator.getBlockLuma(refs[0][0], mb0, BLK_8x8_MB_OFF_LUMA[blk8x8], (mbX << 6) + blkOffX,
                        (mbY << 6) + blkOffY, 8, 8);
                BlockInterpolator.getBlockLuma(refs[1][0], mb1, BLK_8x8_MB_OFF_LUMA[blk8x8], (mbX << 6) + blkOffX,
                        (mbY << 6) + blkOffY, 8, 8);
                PredictionMerger.mergePrediction(sh, 0, 0, PartPred.Bi, 0, mb0.getPlaneData(0), mb1.getPlaneData(0),
                        BLK_8x8_MB_OFF_LUMA[blk8x8], 16, 8, 8, mb.getPlaneData(0), refs, poc);
                debugPrint("DIRECT_8x8 [" + (blk8x8 & 2) + ", " + ((blk8x8 << 1) & 2) + "]: (0,0,0), (0,0,0)");
            }
            return;
        }
        int mvX0 = calcMVPredictionMedian(a0, b0, c0, d0, lAvb, tAvb, trAvb, tlAvb, refIdxL0, 0);
        int mvY0 = calcMVPredictionMedian(a0, b0, c0, d0, lAvb, tAvb, trAvb, tlAvb, refIdxL0, 1);
        int mvX1 = calcMVPredictionMedian(a1, b1, c1, d1, lAvb, tAvb, trAvb, tlAvb, refIdxL1, 0);
        int mvY1 = calcMVPredictionMedian(a1, b1, c1, d1, lAvb, tAvb, trAvb, tlAvb, refIdxL1, 1);

        Frame col = refs[1][0];
        PartPred partPred = refIdxL0 >= 0 && refIdxL1 >= 0 ? Bi : (refIdxL0 >= 0 ? L0 : L1);
        for (int blk8x8 : blocks8x8) {
            int blk4x4_0 = H264Const.BLK8x8_BLOCKS[blk8x8][0];

            if (!sh.sps.direct_8x8_inference_flag)
                for (int blk4x4 : BLK8x8_BLOCKS[blk8x8]) {
                    pred4x4(mbX, mbY, x, pp, refIdxL0, refIdxL1, mvX0, mvY0, mvX1, mvY1, col, partPred, blk4x4);

                    int blkIndX = blk4x4 & 3;
                    int blkIndY = blk4x4 >> 2;

                    debugPrint("DIRECT_4x4 [" + blkIndY + ", " + blkIndX + "]: (" + x[0][blk4x4][0] + ","
                            + x[0][blk4x4][1] + "," + refIdxL0 + "), (" + x[1][blk4x4][0] + "," + x[1][blk4x4][1] + ","
                            + refIdxL1 + ")");

                    int blkPredX = (mbX << 6) + (blkIndX << 4);
                    int blkPredY = (mbY << 6) + (blkIndY << 4);

                    if (refIdxL0 >= 0)
                        BlockInterpolator.getBlockLuma(refs[0][refIdxL0], mb0, BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                                + x[0][blk4x4][0], blkPredY + x[0][blk4x4][1], 4, 4);
                    if (refIdxL1 >= 0)
                        BlockInterpolator.getBlockLuma(refs[1][refIdxL1], mb1, BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                                + x[1][blk4x4][0], blkPredY + x[1][blk4x4][1], 4, 4);
                }
            else {
                int blk4x4Pred = BLK_INV_MAP[blk8x8 * 5];
                pred4x4(mbX, mbY, x, pp, refIdxL0, refIdxL1, mvX0, mvY0, mvX1, mvY1, col, partPred, blk4x4Pred);
                propagatePred(x, blk8x8, blk4x4Pred);

                int blkIndX = blk4x4_0 & 3;
                int blkIndY = blk4x4_0 >> 2;

                debugPrint("DIRECT_8x8 [" + blkIndY + ", " + blkIndX + "]: (" + x[0][blk4x4_0][0] + ","
                        + x[0][blk4x4_0][1] + "," + refIdxL0 + "), (" + x[1][blk4x4_0][0] + "," + x[1][blk4x4_0][1]
                        + "," + refIdxL1 + ")");

                int blkPredX = (mbX << 6) + (blkIndX << 4);
                int blkPredY = (mbY << 6) + (blkIndY << 4);

                if (refIdxL0 >= 0)
                    BlockInterpolator.getBlockLuma(refs[0][refIdxL0], mb0, BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                            + x[0][blk4x4_0][0], blkPredY + x[0][blk4x4_0][1], 8, 8);
                if (refIdxL1 >= 0)
                    BlockInterpolator.getBlockLuma(refs[1][refIdxL1], mb1, BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                            + x[1][blk4x4_0][0], blkPredY + x[1][blk4x4_0][1], 8, 8);
            }
            PredictionMerger.mergePrediction(sh, x[0][blk4x4_0][2], x[1][blk4x4_0][2],
                    refIdxL0 >= 0 ? (refIdxL1 >= 0 ? Bi : L0) : L1, 0, mb0.getPlaneData(0), mb1.getPlaneData(0),
                    BLK_4x4_MB_OFF_LUMA[blk4x4_0], 16, 8, 8, mb.getPlaneData(0), refs, poc);
        }
    }

    private int calcRef(int[] a0, int[] b0, int[] c0, int[] d0, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, int mbX) {
        return minPos(minPos(lAvb ? a0[2] : -1, tAvb ? b0[2] : -1), trAvb ? c0[2] : (tlAvb ? d0[2] : -1));
    }

    private void propagatePred(int[][][] x, int blk8x8, int blk4x4Pred) {
        int b0 = BLK8x8_BLOCKS[blk8x8][0];
        int b1 = BLK8x8_BLOCKS[blk8x8][1];
        int b2 = BLK8x8_BLOCKS[blk8x8][2];
        int b3 = BLK8x8_BLOCKS[blk8x8][3];
        x[0][b0][0] = x[0][b1][0] = x[0][b2][0] = x[0][b3][0] = x[0][blk4x4Pred][0];
        x[0][b0][1] = x[0][b1][1] = x[0][b2][1] = x[0][b3][1] = x[0][blk4x4Pred][1];
        x[0][b0][2] = x[0][b1][2] = x[0][b2][2] = x[0][b3][2] = x[0][blk4x4Pred][2];
        x[1][b0][0] = x[1][b1][0] = x[1][b2][0] = x[1][b3][0] = x[1][blk4x4Pred][0];
        x[1][b0][1] = x[1][b1][1] = x[1][b2][1] = x[1][b3][1] = x[1][blk4x4Pred][1];
        x[1][b0][2] = x[1][b1][2] = x[1][b2][2] = x[1][b3][2] = x[1][blk4x4Pred][2];
    }

    private void pred4x4(int mbX, int mbY, int[][][] x, PartPred[] pp, int refL0, int refL1, int mvX0, int mvY0,
            int mvX1, int mvY1, Frame col, PartPred partPred, int blk4x4) {
        int blkIndX = blk4x4 & 3;
        int blkIndY = blk4x4 >> 2;

        int blkPosX = (mbX << 2) + blkIndX;
        int blkPosY = (mbY << 2) + blkIndY;

        x[0][blk4x4][2] = refL0;
        x[1][blk4x4][2] = refL1;

        int[] mvCol = col.getMvs()[0][blkPosY][blkPosX];
        if (mvCol[2] == -1)
            mvCol = col.getMvs()[1][blkPosY][blkPosX];

        boolean colZero = col.isShortTerm() && mvCol[2] == 0 && (abs(mvCol[0]) >> 1) == 0 && (abs(mvCol[1]) >> 1) == 0;

        if (refL0 > 0 || !colZero) {
            x[0][blk4x4][0] = mvX0;
            x[0][blk4x4][1] = mvY0;
        }
        if (refL1 > 0 || !colZero) {
            x[1][blk4x4][0] = mvX1;
            x[1][blk4x4][1] = mvY1;
        }

        pp[BLK_8x8_IND[blk4x4]] = partPred;
    }

    private int minPos(int a, int b) {
        return a >= 0 && b >= 0 ? Math.min(a, b) : Math.max(a, b);
    }
}
