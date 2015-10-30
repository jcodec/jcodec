package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_8x8_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_8x8_LUT;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.NULL_VECTOR;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.calcMVPredictionMedian;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.copyVect;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.mergeResidual;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvs;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVect;
import static org.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;
import static org.jcodec.common.model.ColorSpace.MONO;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderInter extends MBlockDecoderBase {
    private Mapper mapper;

    public MBlockDecoderInter(Mapper mapper, SliceHeader sh, DeblockerInput di, int poc, DecoderState decoderState) {
        super(sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode16x16(MBlock mBlock, Picture8Bit mb, Frame[][] refs, PartPred p0) {

        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        int address = mapper.getAddress(mBlock.mbIdx);
        int[][][] x = new int[2][][];
        int xx = mbX << 2;
        Picture8Bit[] mbb = { Picture8Bit.create(16, 16, s.chromaFormat), Picture8Bit.create(16, 16, s.chromaFormat) };
        for (int list = 0; list < 2; list++) {
            predictInter16x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, x, xx, list, p0);
        }

        PredictionMerger.mergePrediction(sh, x[0][0][2], x[1][0][2], p0, 0, mbb[0].getPlaneData(0),
                mbb[1].getPlaneData(0), 0, 16, 16, 16, mb.getPlaneData(0), refs, poc);

        PartPred[] partPreds = new PartPred[] { p0, p0, p0, p0 };
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 1, mb, partPreds);
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 2, mb, partPreds);

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, x, partPreds,
                mapper.getAddress(mBlock.mbIdx));

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    private void predictInter8x16(MBlock mBlock, Picture8Bit mb, Picture8Bit[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int[][][] x,
            int list, PartPred p0, PartPred p1) {
        int xx = mbX << 2;

        int mvX1 = 0, mvY1 = 0, r1 = -1, mvX2 = 0, mvY2 = 0, r2 = -1;
        if (p0.usesList(list)) {
            int mvpX1 = calcMVPrediction8x16Left(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 2], s.mvTopLeft[list], leftAvailable, topAvailable, topAvailable,
                    tlAvailable, mBlock.pb168x168.refIdx1[list], 0);
            int mvpY1 = calcMVPrediction8x16Left(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 2], s.mvTopLeft[list], leftAvailable, topAvailable, topAvailable,
                    tlAvailable, mBlock.pb168x168.refIdx1[list], 1);

            mvX1 = mBlock.pb168x168.mvdX1[list] + mvpX1;
            mvY1 = mBlock.pb168x168.mvdY1[list] + mvpY1;

            debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb168x168.mvdX1[list],
                    mBlock.pb168x168.mvdY1[list], mvX1, mvY1, mBlock.pb168x168.refIdx1[list]);

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx1[list]], mb, 0, (mbX << 6) + mvX1,
                    (mbY << 6) + mvY1, 8, 16);
            r1 = mBlock.pb168x168.refIdx1[list];
        }
        int[] v1 = { mvX1, mvY1, r1 };

        if (p1.usesList(list)) {

            int mvpX2 = calcMVPrediction8x16Right(v1, s.mvTop[list][(mbX << 2) + 2], s.mvTop[list][(mbX << 2) + 4],
                    s.mvTop[list][(mbX << 2) + 1], true, topAvailable, trAvailable, topAvailable,
                    mBlock.pb168x168.refIdx2[list], 0);
            int mvpY2 = calcMVPrediction8x16Right(v1, s.mvTop[list][(mbX << 2) + 2], s.mvTop[list][(mbX << 2) + 4],
                    s.mvTop[list][(mbX << 2) + 1], true, topAvailable, trAvailable, topAvailable,
                    mBlock.pb168x168.refIdx2[list], 1);

            mvX2 = mBlock.pb168x168.mvdX2[list] + mvpX2;
            mvY2 = mBlock.pb168x168.mvdY2[list] + mvpY2;

            debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mBlock.pb168x168.mvdX2[list] + ", "
                    + mBlock.pb168x168.mvdY2[list] + "), MV: (" + mvX2 + "," + mvY2 + ","
                    + mBlock.pb168x168.refIdx2[list] + ")");

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx2[list]], mb, 8, (mbX << 6) + 32
                    + mvX2, (mbY << 6) + mvY2, 8, 16);
            r2 = mBlock.pb168x168.refIdx2[list];
        }
        int[] v2 = { mvX2, mvY2, r2 };

        copyVect(s.mvTopLeft[list], s.mvTop[list][xx + 3]);
        saveVect(s.mvTop[list], xx, xx + 2, mvX1, mvY1, r1);
        saveVect(s.mvTop[list], xx + 2, xx + 4, mvX2, mvY2, r2);
        saveVect(s.mvLeft[list], 0, 4, mvX2, mvY2, r2);

        x[list] = new int[][] { v1, v1, v2, v2, v1, v1, v2, v2, v1, v1, v2, v2, v1, v1, v2, v2 };
    }

    private void predictInter16x8(MBlock mBlock, Picture8Bit mb, Picture8Bit[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int xx, int[][][] x,
            PartPred p0, PartPred p1, int list) {

        int mvX1 = 0, mvY1 = 0, mvX2 = 0, mvY2 = 0, r1 = -1, r2 = -1;
        if (p0.usesList(list)) {

            int mvpX1 = calcMVPrediction16x8Top(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 4], s.mvTopLeft[list], leftAvailable, topAvailable, trAvailable,
                    tlAvailable, mBlock.pb168x168.refIdx1[list], 0);
            int mvpY1 = calcMVPrediction16x8Top(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 4], s.mvTopLeft[list], leftAvailable, topAvailable, trAvailable,
                    tlAvailable, mBlock.pb168x168.refIdx1[list], 1);

            mvX1 = mBlock.pb168x168.mvdX1[list] + mvpX1;
            mvY1 = mBlock.pb168x168.mvdY1[list] + mvpY1;

            debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb168x168.mvdX1[list],
                    mBlock.pb168x168.mvdY1[list], mvX1, mvY1, mBlock.pb168x168.refIdx1[list]);

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx1[list]], mb, 0, (mbX << 6) + mvX1,
                    (mbY << 6) + mvY1, 16, 8);
            r1 = mBlock.pb168x168.refIdx1[list];
        }
        int[] v1 = { mvX1, mvY1, r1 };

        if (p1.usesList(list)) {
            int mvpX2 = calcMVPrediction16x8Bottom(s.mvLeft[list][2], v1, null, s.mvLeft[list][1], leftAvailable, true,
                    false, leftAvailable, mBlock.pb168x168.refIdx2[list], 0);
            int mvpY2 = calcMVPrediction16x8Bottom(s.mvLeft[list][2], v1, null, s.mvLeft[list][1], leftAvailable, true,
                    false, leftAvailable, mBlock.pb168x168.refIdx2[list], 1);

            mvX2 = mBlock.pb168x168.mvdX2[list] + mvpX2;
            mvY2 = mBlock.pb168x168.mvdY2[list] + mvpY2;

            debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb168x168.mvdX2[list],
                    mBlock.pb168x168.mvdY2[list], mvX2, mvY2, mBlock.pb168x168.refIdx2[list]);

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx2[list]], mb, 128,
                    (mbX << 6) + mvX2, (mbY << 6) + 32 + mvY2, 16, 8);
            r2 = mBlock.pb168x168.refIdx2[list];
        }
        int[] v2 = { mvX2, mvY2, r2 };

        copyVect(s.mvTopLeft[list], s.mvTop[list][xx + 3]);
        saveVect(s.mvLeft[list], 0, 2, mvX1, mvY1, r1);
        saveVect(s.mvLeft[list], 2, 4, mvX2, mvY2, r2);
        saveVect(s.mvTop[list], xx, xx + 4, mvX2, mvY2, r2);

        x[list] = new int[][] { v1, v1, v1, v1, v1, v1, v1, v1, v2, v2, v2, v2, v2, v2, v2, v2 };
    }

    public void decode16x8(MBlock mBlock, Picture8Bit mb, Frame[][] refs, PartPred p0, PartPred p1) {

        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        int address = mapper.getAddress(mBlock.mbIdx);
        int[][][] x = new int[2][][];
        int xx = mbX << 2;

        Picture8Bit[] mbb = { Picture8Bit.create(16, 16, s.chromaFormat), Picture8Bit.create(16, 16, s.chromaFormat) };
        for (int list = 0; list < 2; list++) {
            predictInter16x8(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, xx, x, p0, p1, list);
        }

        mergePrediction(sh, x[0][0][2], x[1][0][2], p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 0, 16, 16,
                8, mb.getPlaneData(0), refs, poc);
        mergePrediction(sh, x[0][8][2], x[1][8][2], p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 128, 16, 16,
                8, mb.getPlaneData(0), refs, poc);

        PartPred[] partPreds = new PartPred[] { p0, p0, p1, p1 };
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 1, mb, partPreds);
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 2, mb, partPreds);

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, x, partPreds,
                mapper.getAddress(mBlock.mbIdx));

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    public void decode8x16(MBlock mBlock, Picture8Bit mb, Frame[][] refs, PartPred p0, PartPred p1) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        int address = mapper.getAddress(mBlock.mbIdx);

        int[][][] x = new int[2][][];
        Picture8Bit[] mbb = { Picture8Bit.create(16, 16, s.chromaFormat), Picture8Bit.create(16, 16, s.chromaFormat) };

        for (int list = 0; list < 2; list++) {
            predictInter8x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, x, list, p0, p1);
        }

        mergePrediction(sh, x[0][0][2], x[1][0][2], p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 0, 16, 8,
                16, mb.getPlaneData(0), refs, poc);
        mergePrediction(sh, x[0][2][2], x[1][2][2], p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), 8, 16, 8,
                16, mb.getPlaneData(0), refs, poc);

        PartPred[] predType = new PartPred[] { p0, p1, p0, p1 };

        predictChromaInter(refs, x, mbX << 3, mbY << 3, 1, mb, predType);
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 2, mb, predType);

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, x, predType, mapper.getAddress(mBlock.mbIdx));

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    void predictInter16x16(MBlock mBlock, Picture8Bit mb, Picture8Bit[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int[][][] x, int xx,
            int list, PartPred curPred) {

        int mvX = 0, mvY = 0, r = -1;
        if (curPred.usesList(list)) {
            int mvpX = calcMVPredictionMedian(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 4], s.mvTopLeft[list], leftAvailable, topAvailable, trAvailable,
                    tlAvailable, mBlock.pb16x16.refIdx[list], 0);
            int mvpY = calcMVPredictionMedian(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 4], s.mvTopLeft[list], leftAvailable, topAvailable, trAvailable,
                    tlAvailable, mBlock.pb16x16.refIdx[list], 1);

            mvX = mBlock.pb16x16.mvdX[list] + mvpX;
            mvY = mBlock.pb16x16.mvdY[list] + mvpY;

            debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX, mvpY, mBlock.pb16x16.mvdX[list],
                    mBlock.pb16x16.mvdY[list], mvX, mvY, mBlock.pb16x16.refIdx[list]);
            r = mBlock.pb16x16.refIdx[list];

            interpolator.getBlockLuma(references[list][r], mb, 0, (mbX << 6) + mvX, (mbY << 6) + mvY, 16, 16);
        }

        copyVect(s.mvTopLeft[list], s.mvTop[list][xx + 3]);
        saveVect(s.mvTop[list], xx, xx + 4, mvX, mvY, r);
        saveVect(s.mvLeft[list], 0, 4, mvX, mvY, r);

        int[] v = { mvX, mvY, r };
        x[list] = new int[][] { v, v, v, v, v, v, v, v, v, v, v, v, v, v, v, v };
    }

    private void residualInter(MBlock mBlock, Frame[][] refs, boolean leftAvailable, boolean topAvailable, int mbX,
            int mbY, int[][][] x, PartPred[] pp, int mbAddr) {

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);

        saveMvs(di, x, mbX, mbY);

        if (s.chromaFormat != MONO) {
            int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
            int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

            decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, qp1, qp2);

            di.mbQps[1][mbAddr] = qp1;
            di.mbQps[2][mbAddr] = qp2;
        }

        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
    }

    public int calcMVPrediction16x8Top(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {
        if (bAvb && b[2] == refIdx)
            return b[comp];
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction16x8Bottom(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {

        if (aAvb && a[2] == refIdx)
            return a[comp];
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction8x16Left(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {

        if (aAvb && a[2] == refIdx)
            return a[comp];
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction8x16Right(int[] a, int[] b, int[] c, int[] d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {
        int[] lc = cAvb ? c : (dAvb ? d : NULL_VECTOR);

        if (lc[2] == refIdx)
            return lc[comp];
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

}
