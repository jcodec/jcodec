package org.jcodec.codecs.h264.decode;
import static org.jcodec.codecs.h264.H264Const.ARRAY;
import static org.jcodec.codecs.h264.H264Const.BLK8x8_BLOCKS;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_MB_OFF_LUMA;
import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_BLOCK_8x8_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_4x4_LUT;
import static org.jcodec.codecs.h264.H264Const.COMP_POS_8x8_LUT;
import static org.jcodec.codecs.h264.H264Const.bPartPredModes;
import static org.jcodec.codecs.h264.H264Const.bSubMbTypes;
import static org.jcodec.codecs.h264.H264Const.PartPred.Direct;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvX;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvY;
import static org.jcodec.codecs.h264.H264Utils.Mv.packMv;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.NULL_VECTOR;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.calcMVPredictionMedian;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.mergeResidual;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvs;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.savePrediction8x8;
import static org.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;
import static org.jcodec.codecs.h264.decode.PredictionMerger.weightPrediction;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.H264Utils.MvList;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.model.Picture;

import js.util.Arrays;

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderInter8x8 extends MBlockDecoderBase {
    private Mapper mapper;
    private MBlockDecoderBDirect bDirectDecoder;

    public MBlockDecoderInter8x8(Mapper mapper, MBlockDecoderBDirect bDirectDecoder, SliceHeader sh, DeblockerInput di,
            int poc, DecoderState decoderState) {
        super(sh, di, poc, decoderState);
        this.mapper = mapper;
        this.bDirectDecoder = bDirectDecoder;
    }

    public void decode(MBlock mBlock, Frame[][] references, Picture mb, SliceType sliceType, boolean ref0) {

        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        int mbAddr = mapper.getAddress(mBlock.mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);

        if (sliceType == SliceType.P) {
            predict8x8P(mBlock, references[0], mb, ref0, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, mBlock.x, mBlock.partPreds);
        } else {
            predict8x8B(mBlock, references, mb, ref0, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, mBlock.x, mBlock.partPreds);
        }

        predictChromaInter(references, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(references, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);

        saveMvs(di, mBlock.x, mbX, mbY);

        int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
        int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

        decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, qp1, qp2);

        di.mbQps[1][mbAddr] = qp1;
        di.mbQps[2][mbAddr] = qp2;

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);
        
        collectPredictors(s, mb, mbX);

        di.mbTypes[mbAddr] = mBlock.curMbType;
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
    }

    private void predict8x8P(MBlock mBlock, Picture[] references, Picture mb, boolean ref0, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean topRightAvailable, MvList x,
            PartPred[] pp) {

        decodeSubMb8x8(mBlock, 0, mBlock.pb8x8.subMbTypes[0], references, mbX << 6, mbY << 6, s.mvTopLeft.getMv(0, 0),
                s.mvTop.getMv(mbX << 2, 0), s.mvTop.getMv((mbX << 2) + 1, 0), s.mvTop.getMv((mbX << 2) + 2, 0),
                s.mvLeft.getMv(0, 0), s.mvLeft.getMv(1, 0), tlAvailable, topAvailable, topAvailable, leftAvailable,
                mBlock.x, 0, 1, 4, 5, mBlock.pb8x8.refIdx[0][0], mb, 0, 0);

        decodeSubMb8x8(mBlock, 1, mBlock.pb8x8.subMbTypes[1], references, (mbX << 6) + 32, mbY << 6,
                s.mvTop.getMv((mbX << 2) + 1, 0), s.mvTop.getMv((mbX << 2) + 2, 0), s.mvTop.getMv((mbX << 2) + 3, 0),
                s.mvTop.getMv((mbX << 2) + 4, 0), x.getMv(1, 0), x.getMv(5, 0), topAvailable, topAvailable,
                topRightAvailable, true, x, 2, 3, 6, 7, mBlock.pb8x8.refIdx[0][1], mb, 8, 0);

        decodeSubMb8x8(mBlock, 2, mBlock.pb8x8.subMbTypes[2], references, mbX << 6, (mbY << 6) + 32,
                s.mvLeft.getMv(1, 0), x.getMv(4, 0), x.getMv(5, 0), x.getMv(6, 0), s.mvLeft.getMv(2, 0),
                s.mvLeft.getMv(3, 0), leftAvailable, true, true, leftAvailable, x, 8, 9, 12, 13,
                mBlock.pb8x8.refIdx[0][2], mb, 128, 0);

        decodeSubMb8x8(mBlock, 3, mBlock.pb8x8.subMbTypes[3], references, (mbX << 6) + 32, (mbY << 6) + 32,
                x.getMv(5, 0), x.getMv(6, 0), x.getMv(7, 0), NULL_VECTOR, x.getMv(9, 0), x.getMv(13, 0), true, true,
                false, true, x, 10, 11, 14, 15, mBlock.pb8x8.refIdx[0][3], mb, 136, 0);
        
        for (int i = 0; i < 4; i++) {
            // TODO(stan): refactor this
            int blk4x4 = BLK8x8_BLOCKS[i][0];
            weightPrediction(sh, x.mv0R(blk4x4), 0, mb.getPlaneData(0), BLK_8x8_MB_OFF_LUMA[i], 16, 8,
                    8, mb.getPlaneData(0));
        }

        savePrediction8x8(s, mbX, x);
        Arrays.fill(pp, L0);
    }

    private void predict8x8B(MBlock mBlock, Frame[][] refs, Picture mb, boolean ref0, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean topRightAvailable, MvList x,
            PartPred[] p) {

        for (int i = 0; i < 4; i++) {
            p[i] = bPartPredModes[mBlock.pb8x8.subMbTypes[i]];
        }

        for (int i = 0; i < 4; i++) {
            if (p[i] == Direct)
                bDirectDecoder.predictBDirect(refs, mbX, mbY, leftAvailable, topAvailable, tlAvailable,
                        topRightAvailable, x, p, mb, ARRAY[i]);
        }

        for (int list = 0; list < 2; list++) {
            if (H264Const.usesList(bPartPredModes[mBlock.pb8x8.subMbTypes[0]], list)) {
                decodeSubMb8x8(mBlock, 0, bSubMbTypes[mBlock.pb8x8.subMbTypes[0]], refs[list], mbX << 6, mbY << 6,
                        s.mvTopLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list), s.mvTop.getMv((mbX << 2) + 1, list),
                        s.mvTop.getMv((mbX << 2) + 2, list), s.mvLeft.getMv(0, list), s.mvLeft.getMv(1, list),
                        tlAvailable, topAvailable, topAvailable, leftAvailable, x, 0, 1, 4, 5,
                        mBlock.pb8x8.refIdx[list][0], mbb[list], 0, list);
            }
            if (H264Const.usesList(bPartPredModes[mBlock.pb8x8.subMbTypes[1]], list)) {
                decodeSubMb8x8(mBlock, 1, bSubMbTypes[mBlock.pb8x8.subMbTypes[1]], refs[list], (mbX << 6) + 32,
                        mbY << 6, s.mvTop.getMv((mbX << 2) + 1, list), s.mvTop.getMv((mbX << 2) + 2, list),
                        s.mvTop.getMv((mbX << 2) + 3, list), s.mvTop.getMv((mbX << 2) + 4, list), x.getMv(1, list),
                        x.getMv(5, list), topAvailable, topAvailable, topRightAvailable, true, x, 2, 3, 6, 7,
                        mBlock.pb8x8.refIdx[list][1], mbb[list], 8, list);
            }

            if (H264Const.usesList(bPartPredModes[mBlock.pb8x8.subMbTypes[2]], list)) {
                decodeSubMb8x8(mBlock, 2, bSubMbTypes[mBlock.pb8x8.subMbTypes[2]], refs[list], mbX << 6,
                        (mbY << 6) + 32, s.mvLeft.getMv(1, list), x.getMv(4, list), x.getMv(5, list), x.getMv(6, list),
                        s.mvLeft.getMv(2, list), s.mvLeft.getMv(3, list), leftAvailable, true, true, leftAvailable, x,
                        8, 9, 12, 13, mBlock.pb8x8.refIdx[list][2], mbb[list], 128, list);
            }

            if (H264Const.usesList(bPartPredModes[mBlock.pb8x8.subMbTypes[3]], list)) {
                decodeSubMb8x8(mBlock, 3, bSubMbTypes[mBlock.pb8x8.subMbTypes[3]], refs[list], (mbX << 6) + 32,
                        (mbY << 6) + 32, x.getMv(5, list), x.getMv(6, list), x.getMv(7, list), NULL_VECTOR,
                        x.getMv(9, list), x.getMv(13, list), true, true, false, true, x, 10, 11, 14, 15,
                        mBlock.pb8x8.refIdx[list][3], mbb[list], 136, list);
            }
        }

        for (int i = 0; i < 4; i++) {
            int blk4x4 = BLK8x8_BLOCKS[i][0];
            mergePrediction(sh, x.mv0R(blk4x4), x.mv1R(blk4x4), bPartPredModes[mBlock.pb8x8.subMbTypes[i]], 0,
                    mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), BLK_8x8_MB_OFF_LUMA[i], 16, 8, 8,
                    mb.getPlaneData(0), refs, poc);
        }

        savePrediction8x8(s, mbX, x);
    }

    private void decodeSubMb8x8(MBlock mBlock, int partNo, int subMbType, Picture[] references, int offX, int offY,
            int tl, int t0, int t1, int tr, int l0, int l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb,
            MvList x, int i00, int i01, int i10, int i11, int refIdx, Picture mb, int off, int list) {

        switch (subMbType) {
        case 3:
            decodeSub4x4(mBlock, partNo, references, offX, offY, tl, t0, t1, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x,
                    i00, i01, i10, i11, refIdx, mb, off, list);
            break;
        case 2:
            decodeSub4x8(mBlock, partNo, references, offX, offY, tl, t0, t1, tr, l0, tlAvb, tAvb, trAvb, lAvb, x, i00,
                    i01, i10, i11, refIdx, mb, off, list);
            break;
        case 1:
            decodeSub8x4(mBlock, partNo, references, offX, offY, tl, t0, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x, i00,
                    i01, i10, i11, refIdx, mb, off, list);
            break;
        case 0:
            decodeSub8x8(mBlock, partNo, references, offX, offY, tl, t0, tr, l0, tlAvb, tAvb, trAvb, lAvb, x, i00, i01,
                    i10, i11, refIdx, mb, off, list);
        }
    }

    private void decodeSub8x8(MBlock mBlock, int partNo, Picture[] references, int offX, int offY, int tl,
            int t0, int tr, int l0, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, MvList x, int i00,
            int i01, int i10, int i11, int refIdx, Picture mb, int off, int list) {

        int mvpX = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        int mvpY = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);
        
        int mv = packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX, mBlock.pb8x8.mvdY1[list][partNo] + mvpY, refIdx);
        
        x.setMv(i00, list, mv);
        x.setMv(i01, list, mv);
        x.setMv(i10, list, mv);
        x.setMv(i11, list, mv);

        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX, mvpY, mBlock.pb8x8.mvdX1[list][partNo],
                mBlock.pb8x8.mvdY1[list][partNo], mvX(mv), mvY(mv), refIdx);
        
        interpolator.getBlockLuma(references[refIdx], mb, off, offX + mvX(mv), offY + mvY(mv), 8, 8);
    }

    private void decodeSub8x4(MBlock mBlock, int partNo, Picture[] references, int offX, int offY, int tl,
            int t0, int tr, int l0, int l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, MvList x,
            int i00, int i01, int i10, int i11, int refIdx, Picture mb, int off, int list) {

        int mvpX1 = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        // TODO(stan): check if MVs need to be clipped
        int mv1 = packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx);
        x.setMv(i00, list, mv1);
        x.setMv(i01, list, mv1);
        
        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb8x8.mvdX1[list][partNo],
                mBlock.pb8x8.mvdY1[list][partNo], mvX(mv1), mvY(mv1), refIdx);

        int mvpX2 = calcMVPredictionMedian(l1, mv1, NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(l1, mv1, NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 1);

        int mv2 = packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx);
        x.setMv(i10, list, mv2);
        x.setMv(i11, list, mv2);

        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb8x8.mvdX2[list][partNo],
                mBlock.pb8x8.mvdY2[list][partNo], mvX(mv2), mvY(mv2), refIdx);

        interpolator.getBlockLuma(references[refIdx], mb, off, offX + mvX(mv1), offY + mvY(mv1), 8, 4);
        interpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + mvX(mv2),
                offY + mvY(mv2) + 16, 8, 4);
    }

    private void decodeSub4x8(MBlock mBlock, int partNo, Picture[] references, int offX, int offY, int tl, int t0,
            int t1, int tr, int l0, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, MvList x, int i00,
            int i01, int i10, int i11, int refIdx, Picture mb, int off, int list) {

        int mvpX1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        int mv1 = packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx);
        x.setMv(i00, list, mv1);
        x.setMv(i10, list, mv1);

        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb8x8.mvdX1[list][partNo],
                mBlock.pb8x8.mvdY1[list][partNo], mvX(mv1), mvY(mv1), refIdx);

        int mvpX2 = calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        int mv2 = packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx);

        x.setMv(i01, list, mv2);
        x.setMv(i11, list, mv2);

        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb8x8.mvdX2[list][partNo],
                mBlock.pb8x8.mvdY2[list][partNo], mvX(mv2), mvY(mv2), refIdx);

        interpolator.getBlockLuma(references[refIdx], mb, off, offX + mvX(mv1), offY + mvY(mv1), 4, 8);
        interpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + mvX(mv2) + 16, offY + mvY(mv2), 4, 8);
    }

    private void decodeSub4x4(MBlock mBlock, int partNo, Picture[] references, int offX, int offY, int tl,
            int t0, int t1, int tr, int l0, int l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, MvList x,
            int i00, int i01, int i10, int i11, int refIdx, Picture mb, int off, int list) {

        int mvpX1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        int mv1 = packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx);
        x.setMv(i00, list, mv1);
        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb8x8.mvdX1[list][partNo],
                mBlock.pb8x8.mvdY1[list][partNo], mvX(mv1), mvY(mv1), refIdx);

        int mvpX2 = calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        int mv2 = packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx);
        x.setMv(i01, list, mv2);
        
        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb8x8.mvdX2[list][partNo],
                mBlock.pb8x8.mvdY2[list][partNo], mvX(mv2), mvY(mv2), refIdx);

        int mvpX3 = calcMVPredictionMedian(l1, mv1, mv2, l0, lAvb, true, true, lAvb, refIdx, 0);
        int mvpY3 = calcMVPredictionMedian(l1, mv1, mv2, l0, lAvb, true, true, lAvb, refIdx, 1);

        int mv3 = packMv(mBlock.pb8x8.mvdX3[list][partNo] + mvpX3, mBlock.pb8x8.mvdY3[list][partNo] + mvpY3, refIdx);
        x.setMv(i10, list, mv3);

        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX3, mvpY3, mBlock.pb8x8.mvdX3[list][partNo],
                mBlock.pb8x8.mvdY3[list][partNo], mvX(mv3), mvY(mv3), refIdx);

        int mvpX4 = calcMVPredictionMedian(mv3, mv2, NULL_VECTOR, mv1, true, true, false, true, refIdx, 0);
        int mvpY4 = calcMVPredictionMedian(mv3, mv2, NULL_VECTOR, mv1, true, true, false, true, refIdx, 1);

        int mv4 = packMv(mBlock.pb8x8.mvdX4[list][partNo] + mvpX4, mBlock.pb8x8.mvdY4[list][partNo] + mvpY4, refIdx);
        x.setMv(i11, list, mv4);

        debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX4, mvpY4, mBlock.pb8x8.mvdX4[list][partNo],
                mBlock.pb8x8.mvdY4[list][partNo], mvX(mv4), mvY(mv4), refIdx);

        interpolator.getBlockLuma(references[refIdx], mb, off, offX + mvX(mv1), offY + mvY(mv1), 4, 4);
        interpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + mvX(mv2) + 16, offY + mvY(mv2), 4, 4);
        interpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + mvX(mv3), offY + mvY(mv3)
                + 16, 4, 4);
        interpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4 + 4, offX + mvX(mv4) + 16, offY
                + mvY(mv4) + 16, 4, 4);
    }
}
