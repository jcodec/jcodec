package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.BLK8x8_BLOCKS;
import static org.jcodec.codecs.h264.H264Const.BLK_4x4_MB_OFF_LUMA;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_IND;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_MB_OFF_LUMA;
import static org.jcodec.codecs.h264.H264Const.BLK_DISP_MAP;
import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_8x8_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_8x8_LUT;
import static org.jcodec.codecs.h264.H264Const.identityMapping4;
import static org.jcodec.codecs.h264.H264Const.PartPred.Bi;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.H264Const.PartPred.L1;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvRef;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvX;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvY;
import static org.jcodec.codecs.h264.H264Utils.Mv.packMv;
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
import org.jcodec.codecs.h264.H264Utils.MvList;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * A decoder for B direct macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderBDirect extends MBlockDecoderBase {
    private Mapper mapper;

    public MBlockDecoderBDirect(Mapper mapper, SliceHeader sh, DeblockerInput di, int poc, DecoderState decoderState) {
        super(sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode(MBlock mBlock, Picture mb, Frame[][] references) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean lAvb = mapper.leftAvailable(mBlock.mbIdx);
        boolean tAvb = mapper.topAvailable(mBlock.mbIdx);
        int mbAddr = mapper.getAddress(mBlock.mbIdx);
        boolean tlAvb = mapper.topLeftAvailable(mBlock.mbIdx);
        boolean trAvb = mapper.topRightAvailable(mBlock.mbIdx);

        predictBDirect(references, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, mBlock.x, mBlock.partPreds, mb, identityMapping4);

        predictChromaInter(references, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(references, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock, lAvb, tAvb, mbX, mbY);

        savePrediction8x8(s, mbX, mBlock.x);
        saveMvs(di, mBlock.x, mbX, mbY);

        int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
        int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

        decodeChromaResidual(mBlock, lAvb, tAvb, mbX, mbY, qp1, qp2);

        di.mbQps[1][mbAddr] = qp1;
        di.mbQps[2][mbAddr] = qp2;

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[mbAddr] = mBlock.curMbType;
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
    }

    public void predictBDirect(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, MvList x, PartPred[] pp, Picture mb, int[] blocks) {
        if (sh.directSpatialMvPredFlag)
            predictBSpatialDirect(refs, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, blocks);
        else
            predictBTemporalDirect(refs, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, blocks);
    }

    private void predictBTemporalDirect(Frame[][] refs, int mbX, int mbY, boolean lAvb, boolean tAvb, boolean tlAvb,
            boolean trAvb, MvList x, PartPred[] pp, Picture mb, int[] blocks8x8) {

        for (int i = 0; i < blocks8x8.length; i++) {
            int blk8x8 = blocks8x8[i];
            int blk4x4_0 = H264Const.BLK8x8_BLOCKS[blk8x8][0];
            pp[blk8x8] = Bi;

            if (!sh.sps.direct8x8InferenceFlag) {
                int[] js = BLK8x8_BLOCKS[blk8x8];
                for (int j = 0; j < js.length; j++) {
                    int blk4x4 = js[j];
                    predTemp4x4(refs, mbX, mbY, x, blk4x4);

                    int blkIndX = blk4x4 & 3;
                    int blkIndY = blk4x4 >> 2;

                    debugPrint("DIRECT_4x4 [%d, %d]: (%d,%d,%d), (%d,%d,%d)", blkIndY, blkIndX, x.mv0X(blk4x4),
                            x.mv0Y(blk4x4), x.mv0R(blk4x4), x.mv1X(blk4x4), x.mv1Y(blk4x4), x.mv1R(blk4x4));

                    int blkPredX = (mbX << 6) + (blkIndX << 4);
                    int blkPredY = (mbY << 6) + (blkIndY << 4);

                    interpolator.getBlockLuma(refs[0][x.mv0R(blk4x4)], mbb[0], BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x.mv0X(blk4x4), blkPredY + x.mv0Y(blk4x4), 4, 4);
                    interpolator.getBlockLuma(refs[1][0], mbb[1], BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x.mv1X(blk4x4), blkPredY + x.mv1Y(blk4x4), 4, 4);
                }
            } else {
                int blk4x4Pred = BLK_DISP_MAP[blk8x8 * 5];
                predTemp4x4(refs, mbX, mbY, x, blk4x4Pred);
                propagatePred(x, blk8x8, blk4x4Pred);

                int blkIndX = blk4x4_0 & 3;
                int blkIndY = blk4x4_0 >> 2;

                debugPrint("DIRECT_8x8 [%d, %d]: (%d,%d,%d), (%d,%d)", blkIndY, blkIndX, x.mv0X(blk4x4_0),
                        x.mv0Y(blk4x4_0), x.mv0R(blk4x4_0), x.mv1X(blk4x4_0), x.mv1Y(blk4x4_0), x.mv1R(blk4x4_0));

                int blkPredX = (mbX << 6) + (blkIndX << 4);
                int blkPredY = (mbY << 6) + (blkIndY << 4);

                interpolator.getBlockLuma(refs[0][x.mv0R(blk4x4_0)], mbb[0], BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x.mv0X(blk4x4_0), blkPredY + x.mv0Y(blk4x4_0), 8, 8);
                interpolator.getBlockLuma(refs[1][0], mbb[1], BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x.mv1X(blk4x4_0), blkPredY + x.mv1Y(blk4x4_0), 8, 8);
            }
            mergePrediction(sh, x.mv0R(blk4x4_0), x.mv1R(blk4x4_0), Bi, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                    BLK_4x4_MB_OFF_LUMA[blk4x4_0], 16, 8, 8, mb.getPlaneData(0), refs, poc);
        }
    }

    private void predTemp4x4(Frame[][] refs, int mbX, int mbY, MvList x, int blk4x4) {
        int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        Frame picCol = refs[1][0];
        int blkIndX = blk4x4 & 3;
        int blkIndY = blk4x4 >> 2;

        int blkPosX = (mbX << 2) + blkIndX;
        int blkPosY = (mbY << 2) + blkIndY;

        int mvCol = picCol.getMvs().getMv(blkPosX, blkPosY, 0);
        Frame refL0;
        int refIdxL0;
        if (mvRef(mvCol) == -1) {
            mvCol = picCol.getMvs().getMv(blkPosX, blkPosY, 1);
            if (mvRef(mvCol) == -1) {
                refIdxL0 = 0;
                refL0 = refs[0][0];
            } else {
                refL0 = picCol.getRefsUsed()[mbY * mbWidth + mbX][1][mvRef(mvCol)];
                refIdxL0 = findPic(refs[0], refL0);
            }
        } else {
            refL0 = picCol.getRefsUsed()[mbY * mbWidth + mbX][0][mvRef(mvCol)];
            refIdxL0 = findPic(refs[0], refL0);
        }
        
        int td = MathUtil.clip(picCol.getPOC() - refL0.getPOC(), -128, 127);
        if (!refL0.isShortTerm() || td == 0) {
            x.setPair(blk4x4, packMv(mvX(mvCol), mvY(mvCol), refIdxL0), 0);
        } else {
            int tb = MathUtil.clip(poc - refL0.getPOC(), -128, 127);
            int tx = (16384 + Math.abs(td / 2)) / td;
            int dsf = clip((tb * tx + 32) >> 6, -1024, 1023);

            x.setPair(blk4x4, packMv((dsf * mvX(mvCol) + 128) >> 8, (dsf * mvY(mvCol) + 128) >> 8, refIdxL0),
                    packMv((x.mv0X(blk4x4) - mvX(mvCol)), (x.mv0Y(blk4x4) - mvY(mvCol)), 0));
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
            boolean trAvb, MvList x, PartPred[] pp, Picture mb, int[] blocks8x8) {

        int a0 = s.mvLeft.getMv(0, 0), a1 = s.mvLeft.getMv(0, 1);
        int b0 = s.mvTop.getMv(mbX << 2, 0), b1 = s.mvTop.getMv(mbX << 2, 1);
        int c0 = s.mvTop.getMv((mbX << 2) + 4, 0), c1 = s.mvTop.getMv((mbX << 2) + 4, 1);
        int d0 = s.mvTopLeft.getMv(0, 0);
        int d1 = s.mvTopLeft.getMv(0, 1);

        int refIdxL0 = calcRef(a0, b0, c0, d0, lAvb, tAvb, tlAvb, trAvb, mbX);
        int refIdxL1 = calcRef(a1, b1, c1, d1, lAvb, tAvb, tlAvb, trAvb, mbX);

        if (refIdxL0 < 0 && refIdxL1 < 0) {
            for (int i = 0; i < blocks8x8.length; i++) {
                int blk8x8 = blocks8x8[i];
                int[] js = BLK8x8_BLOCKS[blk8x8];
                for (int j = 0; j < js.length; j++) {
                    int blk4x4 = js[j];
                    x.setPair(blk4x4, 0, 0);
                }
                pp[blk8x8] = Bi;

                int blkOffX = (blk8x8 & 1) << 5;
                int blkOffY = (blk8x8 >> 1) << 5;
                interpolator.getBlockLuma(refs[0][0], mbb[0], BLK_8x8_MB_OFF_LUMA[blk8x8], (mbX << 6) + blkOffX,
                        (mbY << 6) + blkOffY, 8, 8);
                interpolator.getBlockLuma(refs[1][0], mbb[1], BLK_8x8_MB_OFF_LUMA[blk8x8], (mbX << 6) + blkOffX,
                        (mbY << 6) + blkOffY, 8, 8);
                PredictionMerger.mergePrediction(sh, 0, 0, PartPred.Bi, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                        BLK_8x8_MB_OFF_LUMA[blk8x8], 16, 8, 8, mb.getPlaneData(0), refs, poc);
                debugPrint("DIRECT_8x8 [%d, %d]: (0,0,0), (0,0,0)", (blk8x8 & 2), ((blk8x8 << 1) & 2));
            }
            return;
        }
        int mvX0 = calcMVPredictionMedian(a0, b0, c0, d0, lAvb, tAvb, trAvb, tlAvb, refIdxL0, 0);
        int mvY0 = calcMVPredictionMedian(a0, b0, c0, d0, lAvb, tAvb, trAvb, tlAvb, refIdxL0, 1);
        int mvX1 = calcMVPredictionMedian(a1, b1, c1, d1, lAvb, tAvb, trAvb, tlAvb, refIdxL1, 0);
        int mvY1 = calcMVPredictionMedian(a1, b1, c1, d1, lAvb, tAvb, trAvb, tlAvb, refIdxL1, 1);

        Frame col = refs[1][0];
        PartPred partPred = refIdxL0 >= 0 && refIdxL1 >= 0 ? Bi : (refIdxL0 >= 0 ? L0 : L1);
        for (int i = 0; i < blocks8x8.length; i++) {
            int blk8x8 = blocks8x8[i];
            int blk4x4_0 = H264Const.BLK8x8_BLOCKS[blk8x8][0];

            if (!sh.sps.direct8x8InferenceFlag) {
                int[] js = BLK8x8_BLOCKS[blk8x8];
                for (int j = 0; j < js.length; j++) {
                    int blk4x4 = js[j];
                    pred4x4(mbX, mbY, x, pp, refIdxL0, refIdxL1, mvX0, mvY0, mvX1, mvY1, col, partPred, blk4x4);

                    int blkIndX = blk4x4 & 3;
                    int blkIndY = blk4x4 >> 2;

                    debugPrint("DIRECT_4x4 [%d, %d]: (%d,%d,%d), (%d,%d," + refIdxL1 + ")", blkIndY, blkIndX,
                            x.mv0X(blk4x4), x.mv0Y(blk4x4), refIdxL0, x.mv1X(blk4x4), x.mv1Y(blk4x4));

                    int blkPredX = (mbX << 6) + (blkIndX << 4);
                    int blkPredY = (mbY << 6) + (blkIndY << 4);

                    if (refIdxL0 >= 0)
                        interpolator.getBlockLuma(refs[0][refIdxL0], mbb[0], BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                                + x.mv0X(blk4x4), blkPredY + x.mv0Y(blk4x4), 4, 4);
                    if (refIdxL1 >= 0)
                        interpolator.getBlockLuma(refs[1][refIdxL1], mbb[1], BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                                + x.mv1X(blk4x4), blkPredY + x.mv1Y(blk4x4), 4, 4);
                }
            } else {
                int blk4x4Pred = BLK_DISP_MAP[blk8x8 * 5];
                pred4x4(mbX, mbY, x, pp, refIdxL0, refIdxL1, mvX0, mvY0, mvX1, mvY1, col, partPred, blk4x4Pred);
                propagatePred(x, blk8x8, blk4x4Pred);

                int blkIndX = blk4x4_0 & 3;
                int blkIndY = blk4x4_0 >> 2;

                debugPrint("DIRECT_8x8 [%d, %d]: (%d,%d,%d), (%d,%d,%d)", blkIndY, blkIndX, x.mv0X(blk4x4_0),
                        x.mv0Y(blk4x4_0), refIdxL0, x.mv1X(blk4x4_0), x.mv1Y(blk4x4_0), refIdxL1);

                int blkPredX = (mbX << 6) + (blkIndX << 4);
                int blkPredY = (mbY << 6) + (blkIndY << 4);

                if (refIdxL0 >= 0)
                    interpolator.getBlockLuma(refs[0][refIdxL0], mbb[0], BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                            + x.mv0X(blk4x4_0), blkPredY + x.mv0Y(blk4x4_0), 8, 8);
                if (refIdxL1 >= 0)
                    interpolator.getBlockLuma(refs[1][refIdxL1], mbb[1], BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                            + x.mv1X(blk4x4_0), blkPredY + x.mv1Y(blk4x4_0), 8, 8);
            }
            PredictionMerger.mergePrediction(sh, x.mv0R(blk4x4_0), x.mv1R(blk4x4_0),
                    refIdxL0 >= 0 ? (refIdxL1 >= 0 ? Bi : L0) : L1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                    BLK_4x4_MB_OFF_LUMA[blk4x4_0], 16, 8, 8, mb.getPlaneData(0), refs, poc);
        }
    }

    private int calcRef(int a0, int b0, int c0, int d0, boolean lAvb, boolean tAvb, boolean tlAvb, boolean trAvb,
            int mbX) {
        return minPos(minPos(lAvb ? mvRef(a0) : -1, tAvb ? mvRef(b0) : -1), trAvb ? mvRef(c0) : (tlAvb ? mvRef(d0) : -1));
    }

    private void propagatePred(MvList x, int blk8x8, int blk4x4Pred) {
        int b0 = BLK8x8_BLOCKS[blk8x8][0];
        int b1 = BLK8x8_BLOCKS[blk8x8][1];
        int b2 = BLK8x8_BLOCKS[blk8x8][2];
        int b3 = BLK8x8_BLOCKS[blk8x8][3];
        x.copyPair(b0, x, blk4x4Pred);
        x.copyPair(b1, x, blk4x4Pred);
        x.copyPair(b2, x, blk4x4Pred);
        x.copyPair(b3, x, blk4x4Pred);
    }

    private void pred4x4(int mbX, int mbY, MvList x, PartPred[] pp, int refL0, int refL1, int mvX0, int mvY0,
            int mvX1, int mvY1, Frame col, PartPred partPred, int blk4x4) {
        int blkIndX = blk4x4 & 3;
        int blkIndY = blk4x4 >> 2;

        int blkPosX = (mbX << 2) + blkIndX;
        int blkPosY = (mbY << 2) + blkIndY;

        int mvCol = col.getMvs().getMv(blkPosX, blkPosY, 0);
        if (mvRef(mvCol) == -1)
            mvCol = col.getMvs().getMv(blkPosX, blkPosY, 1);

        boolean colZero = col.isShortTerm() && mvRef(mvCol) == 0 && (abs(mvX(mvCol)) >> 1) == 0
                && (abs(mvY(mvCol)) >> 1) == 0;

        int x0 = packMv(0, 0, refL0), x1 = packMv(0, 0, refL1);
        if (refL0 > 0 || !colZero) {
            x0 = packMv(mvX0, mvY0, refL0);
        }
        if (refL1 > 0 || !colZero) {
            x1 = packMv(mvX1, mvY1, refL1);
        }
        x.setPair(blk4x4, x0, x1);

        pp[BLK_8x8_IND[blk4x4]] = partPred;
    }

    private int minPos(int a, int b) {
        return a >= 0 && b >= 0 ? Math.min(a, b) : Math.max(a, b);
    }
}
