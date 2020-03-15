package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_8x8_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_8x8_LUT;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvC;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvRef;
import static org.jcodec.codecs.h264.H264Utils.Mv.packMv;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.NULL_VECTOR;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.calcMVPredictionMedian;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.mergeResidual;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvs;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVect;
import static org.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;
import static org.jcodec.common.model.ColorSpace.MONO;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.H264Utils.MvList;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Picture;

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

    public void decode16x16(MBlock mBlock, Picture mb, Frame[][] refs, PartPred p0) {

        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        int address = mapper.getAddress(mBlock.mbIdx);
        int xx = mbX << 2;

        for (int list = 0; list < 2; list++) {
            predictInter16x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, mBlock.x, xx, list, p0);
        }

        PredictionMerger.mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0),
                mbb[1].getPlaneData(0), 0, 16, 16, 16, mb.getPlaneData(0), refs, poc);

        mBlock.partPreds[0] = mBlock.partPreds[1] = mBlock.partPreds[2] = mBlock.partPreds[3] = p0;
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, mapper.getAddress(mBlock.mbIdx));

        saveMvs(di, mBlock.x, mbX, mbY);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    private void predictInter8x16(MBlock mBlock, Picture mb, Picture[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, MvList x,
            int list, PartPred p0, PartPred p1) {
        int xx = mbX << 2;

        int mvX1 = 0, mvY1 = 0, r1 = -1, mvX2 = 0, mvY2 = 0, r2 = -1;
        if (H264Const.usesList(p0, list)) {
            int mvpX1 = calcMVPrediction8x16Left(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                    s.mvTop.getMv((mbX << 2) + 2, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    topAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 0);
            int mvpY1 = calcMVPrediction8x16Left(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                    s.mvTop.getMv((mbX << 2) + 2, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    topAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 1);

            mvX1 = mBlock.pb168x168.mvdX1[list] + mvpX1;
            mvY1 = mBlock.pb168x168.mvdY1[list] + mvpY1;

            debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb168x168.mvdX1[list],
                    mBlock.pb168x168.mvdY1[list], mvX1, mvY1, mBlock.pb168x168.refIdx1[list]);

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx1[list]], mb, 0, (mbX << 6) + mvX1,
                    (mbY << 6) + mvY1, 8, 16);
            r1 = mBlock.pb168x168.refIdx1[list];
        }

        // Horizontal motion vector range does not exceed the range of -2048 to 2047.75, inclusive, in units of luma
        // samples. Vertical MV [-512,+511.75]. I.e. 14 + 12 bits = 26 bits. Ref Idx 6 bit ?  
        int v1 = packMv(mvX1, mvY1, r1);
        if (H264Const.usesList(p1, list)) {
            int mvpX2 = calcMVPrediction8x16Right(v1, s.mvTop.getMv((mbX << 2) + 2, list),
                    s.mvTop.getMv((mbX << 2) + 4, list), s.mvTop.getMv((mbX << 2) + 1, list), true, topAvailable,
                    trAvailable, topAvailable, mBlock.pb168x168.refIdx2[list], 0);
            int mvpY2 = calcMVPrediction8x16Right(v1, s.mvTop.getMv((mbX << 2) + 2, list),
                    s.mvTop.getMv((mbX << 2) + 4, list), s.mvTop.getMv((mbX << 2) + 1, list), true, topAvailable,
                    trAvailable, topAvailable, mBlock.pb168x168.refIdx2[list], 1);

            mvX2 = mBlock.pb168x168.mvdX2[list] + mvpX2;
            mvY2 = mBlock.pb168x168.mvdY2[list] + mvpY2;

            debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mBlock.pb168x168.mvdX2[list] + ", "
                    + mBlock.pb168x168.mvdY2[list] + "), MV: (" + mvX2 + "," + mvY2 + ","
                    + mBlock.pb168x168.refIdx2[list] + ")");

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx2[list]], mb, 8, (mbX << 6) + 32
                    + mvX2, (mbY << 6) + mvY2, 8, 16);
            r2 = mBlock.pb168x168.refIdx2[list];
        }
        int v2 = packMv(mvX2, mvY2, r2);

        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list));
        saveVect(s.mvTop, list, xx, xx + 2, v1);
        saveVect(s.mvTop, list, xx + 2, xx + 4, v2);
        saveVect(s.mvLeft, list, 0, 4, v2);
        
        for (int i = 0; i < 16; i += 4) {
            x.setMv(i, list, v1);
            x.setMv(i + 1, list, v1);
            x.setMv(i + 2, list, v2);
            x.setMv(i + 3, list, v2);
        }
    }

    private void predictInter16x8(MBlock mBlock, Picture mb, Picture[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int xx, MvList x,
            PartPred p0, PartPred p1, int list) {

        int mvX1 = 0, mvY1 = 0, mvX2 = 0, mvY2 = 0, r1 = -1, r2 = -1;
        if (H264Const.usesList(p0, list)) {

            int mvpX1 = calcMVPrediction16x8Top(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                    s.mvTop.getMv((mbX << 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    trAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 0);
            int mvpY1 = calcMVPrediction16x8Top(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                    s.mvTop.getMv((mbX << 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    trAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 1);

            mvX1 = mBlock.pb168x168.mvdX1[list] + mvpX1;
            mvY1 = mBlock.pb168x168.mvdY1[list] + mvpY1;

            debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb168x168.mvdX1[list],
                    mBlock.pb168x168.mvdY1[list], mvX1, mvY1, mBlock.pb168x168.refIdx1[list]);

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx1[list]], mb, 0, (mbX << 6) + mvX1,
                    (mbY << 6) + mvY1, 16, 8);
            r1 = mBlock.pb168x168.refIdx1[list];
        }
        int v1 = packMv( mvX1, mvY1, r1 );

        if (H264Const.usesList(p1, list)) {
            int mvpX2 = calcMVPrediction16x8Bottom(s.mvLeft.getMv(2, list), v1, NULL_VECTOR, s.mvLeft.getMv(1, list),
                    leftAvailable, true, false, leftAvailable, mBlock.pb168x168.refIdx2[list], 0);
            int mvpY2 = calcMVPrediction16x8Bottom(s.mvLeft.getMv(2, list), v1, NULL_VECTOR, s.mvLeft.getMv(1, list),
                    leftAvailable, true, false, leftAvailable, mBlock.pb168x168.refIdx2[list], 1);

            mvX2 = mBlock.pb168x168.mvdX2[list] + mvpX2;
            mvY2 = mBlock.pb168x168.mvdY2[list] + mvpY2;

            debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb168x168.mvdX2[list],
                    mBlock.pb168x168.mvdY2[list], mvX2, mvY2, mBlock.pb168x168.refIdx2[list]);

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx2[list]], mb, 128,
                    (mbX << 6) + mvX2, (mbY << 6) + 32 + mvY2, 16, 8);
            r2 = mBlock.pb168x168.refIdx2[list];
        }
        int v2 = packMv( mvX2, mvY2, r2 );

        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list));
        saveVect(s.mvLeft, list, 0, 2, v1);
        saveVect(s.mvLeft, list, 2, 4, v2);
        saveVect(s.mvTop, list, xx, xx + 4, v2);
        
        for (int i = 0; i < 8; i++) {
            x.setMv(i, list, v1);
        }
        for (int i = 8; i < 16; i++) {
            x.setMv(i, list, v2);
        }
    }

    public void decode16x8(MBlock mBlock, Picture mb, Frame[][] refs, PartPred p0, PartPred p1) {

        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        int address = mapper.getAddress(mBlock.mbIdx);
        int xx = mbX << 2;

        for (int list = 0; list < 2; list++) {
            predictInter16x8(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, xx, mBlock.x, p0, p1, list);
        }

        mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                0, 16, 16, 8, mb.getPlaneData(0), refs, poc);
        mergePrediction(sh, mBlock.x.mv0R(8), mBlock.x.mv1R(8), p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                128, 16, 16, 8, mb.getPlaneData(0), refs, poc);

        mBlock.partPreds[0] = mBlock.partPreds[1] = p0;
        mBlock.partPreds[2] = mBlock.partPreds[3] = p1;
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, mapper.getAddress(mBlock.mbIdx));

        saveMvs(di, mBlock.x, mbX, mbY);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    public void decode8x16(MBlock mBlock, Picture mb, Frame[][] refs, PartPred p0, PartPred p1) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        int address = mapper.getAddress(mBlock.mbIdx);

        for (int list = 0; list < 2; list++) {
            predictInter8x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, mBlock.x, list, p0, p1);
        }

        mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                0, 16, 8, 16, mb.getPlaneData(0), refs, poc);
        mergePrediction(sh, mBlock.x.mv0R(2), mBlock.x.mv1R(2), p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                8, 16, 8, 16, mb.getPlaneData(0), refs, poc);

        mBlock.partPreds[0] = mBlock.partPreds[2] = p0;
        mBlock.partPreds[1] = mBlock.partPreds[3] = p1;

        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, mapper.getAddress(mBlock.mbIdx));

        saveMvs(di, mBlock.x, mbX, mbY);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    void predictInter16x16(MBlock mBlock, Picture mb, Picture[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, MvList x, int xx,
            int list, PartPred curPred) {

        int mvX = 0, mvY = 0, r = -1;
        if (H264Const.usesList(curPred, list)) {
            int mvpX = calcMVPredictionMedian(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                    s.mvTop.getMv((mbX << 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    trAvailable, tlAvailable, mBlock.pb16x16.refIdx[list], 0);
            int mvpY = calcMVPredictionMedian(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                    s.mvTop.getMv((mbX << 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    trAvailable, tlAvailable, mBlock.pb16x16.refIdx[list], 1);
            mvX = mBlock.pb16x16.mvdX[list] + mvpX;
            mvY = mBlock.pb16x16.mvdY[list] + mvpY;

            debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX, mvpY, mBlock.pb16x16.mvdX[list],
                    mBlock.pb16x16.mvdY[list], mvX, mvY, mBlock.pb16x16.refIdx[list]);
            r = mBlock.pb16x16.refIdx[list];

            interpolator.getBlockLuma(references[list][r], mb, 0, (mbX << 6) + mvX, (mbY << 6) + mvY, 16, 16);
        }

        int v = packMv(mvX, mvY, r);
        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list));
        saveVect(s.mvTop, list, xx, xx + 4, v);
        saveVect(s.mvLeft, list, 0, 4, v);
        
        for (int i = 0; i < 16; i++) {
            x.setMv(i, list, v);
        }
    }

    private void residualInter(MBlock mBlock, Frame[][] refs, boolean leftAvailable, boolean topAvailable, int mbX,
            int mbY, int mbAddr) {

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);

        if (s.chromaFormat != MONO) {
            int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
            int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

            decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, qp1, qp2);

            di.mbQps[1][mbAddr] = qp1;
            di.mbQps[2][mbAddr] = qp2;
        }

        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
    }

    public int calcMVPrediction16x8Top(int a, int b, int c, int d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {
        if (bAvb && mvRef(b) == refIdx)
            return mvC(b, comp);
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction16x8Bottom(int a, int b, int c, int d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {

        if (aAvb && mvRef(a) == refIdx)
            return mvC(a, comp);
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction8x16Left(int a, int b, int c, int d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {

        if (aAvb && mvRef(a) == refIdx)
            return mvC(a, comp);
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction8x16Right(int a, int b, int c, int d, boolean aAvb, boolean bAvb, boolean cAvb,
            boolean dAvb, int refIdx, int comp) {
        int lc = cAvb ? c : (dAvb ? d : NULL_VECTOR);

        if (mvRef(lc) == refIdx)
            return mvC(lc, comp);
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }
}
