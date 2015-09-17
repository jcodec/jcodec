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
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.NULL_VECTOR;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.calcMVPredictionMedian;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.mergeResidual;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveMvs;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.savePrediction8x8;
import static org.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;
import static org.jcodec.codecs.h264.io.model.MBType.B_8x8;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x8;

import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderInter8x8 extends MBlockDecoderBase {
    private Mapper mapper;
    private MBlockDecoderBDirect bDirectDecoder;

    public MBlockDecoderInter8x8(Mapper mapper, BitstreamParser parser, MBlockDecoderBDirect bDirectDecoder,
            SliceHeader sh, DeblockerInput di, int poc, DecoderState decoderState) {
        super(parser, sh, di, poc, decoderState);
        this.mapper = mapper;
        this.bDirectDecoder = bDirectDecoder;
    }

    public void decode(int mb_type, Frame[][] references, Picture8Bit mb, SliceType sliceType, int mbIdx,
            boolean mb_field_decoding_flag, MBType prevMbType, boolean ref0) {

        MBlockP8x8 mBlock = new MBlockP8x8();

        readMBlock8x8(sliceType, mbIdx, prevMbType, ref0, mBlock);

        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        int mbAddr = mapper.getAddress(mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);
        int[][][] x = new int[2][16][3];
        PartPred[] pp = new PartPred[4];
        MBType curMBType;
        for (int i = 0; i < 16; i++)
            x[0][i][2] = x[1][i][2] = -1;

        if (sliceType == SliceType.P) {
            predict8x8P(mBlock, references[0], mb, ref0, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, x, pp);
            curMBType = P_8x8;
        } else {
            predict8x8B(mBlock, references, mb, ref0, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, x, pp);
            curMBType = B_8x8;
        }

        predictChromaInter(references, x, mbX << 3, mbY << 3, 1, mb, pp);
        predictChromaInter(references, x, mbX << 3, mbY << 3, 2, mb, pp);

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY, curMBType, mBlock.transform8x8Used, s.tf8x8Left,
                s.tf8x8Top[mbX]);

        saveMvs(di, x, mbX, mbY);

        int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
        int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

        decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, mBlock.cbpChroma(), qp1, qp2, MBType.P_8x8);

        di.mbQps[1][mbAddr] = qp1;
        di.mbQps[2][mbAddr] = qp2;

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[mbAddr] = s.topMBType[mbX] = s.leftMBType = curMBType;
        s.topCBPLuma[mbX] = s.leftCBPLuma = mBlock.cbpLuma();
        s.topCBPChroma[mbX] = s.leftCBPChroma = mBlock.cbpChroma();
        s.tf8x8Left = s.tf8x8Top[mbX] = mBlock.transform8x8Used;
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
    }

    private void readMBlock8x8(SliceType sliceType, int mbIdx, MBType prevMbType, boolean ref0, MBlockP8x8 mBlock) {
        MBType curMBType;
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);

        boolean noSubMBLessThen8x8;
        if (sliceType == SliceType.P) {
            readPrediction8x8P(mBlock, ref0, mbX, leftAvailable, topAvailable);
            curMBType = P_8x8;
            noSubMBLessThen8x8 = mBlock.subMbTypes[0] == 0 && mBlock.subMbTypes[1] == 0 && mBlock.subMbTypes[2] == 0
                    && mBlock.subMbTypes[3] == 0;
        } else {
            readPrediction8x8B(mBlock, mbX, leftAvailable, topAvailable);
            curMBType = B_8x8;
            noSubMBLessThen8x8 = bSubMbTypes[mBlock.subMbTypes[0]] == 0 && bSubMbTypes[mBlock.subMbTypes[1]] == 0
                    && bSubMbTypes[mBlock.subMbTypes[2]] == 0 && bSubMbTypes[mBlock.subMbTypes[3]] == 0;
        }

        mBlock.cbp = parser.readCodedBlockPatternInter(leftAvailable, topAvailable, s.leftCBPLuma
                | (s.leftCBPChroma << 4), s.topCBPLuma[mbX] | (s.topCBPChroma[mbX] << 4), s.leftMBType,
                s.topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (s.transform8x8 && mBlock.cbpLuma() != 0 && noSubMBLessThen8x8) {
            mBlock.transform8x8Used = parser.readTransform8x8Flag(leftAvailable, topAvailable, s.leftMBType,
                    s.topMBType[mbX], s.tf8x8Left, s.tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = parser.readMBQpDelta(prevMbType);
        }
        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY, curMBType, mBlock.transform8x8Used);
        readChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mBlock.cbpChroma(), MBType.P_8x8);
    }

    private void predict8x8P(MBlockP8x8 mBlock, Picture8Bit[] references, Picture8Bit mb, boolean ref0, int mbX,
            int mbY, boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean topRightAvailable,
            int[][][] x, PartPred[] pp) {

        decodeSubMb8x8(mBlock, 0, mBlock.subMbTypes[0], references, mbX << 6, mbY << 6, x[0], s.mvTopLeft[0],
                s.mvTop[0][mbX << 2], s.mvTop[0][(mbX << 2) + 1], s.mvTop[0][(mbX << 2) + 2], s.mvLeft[0][0],
                s.mvLeft[0][1], tlAvailable, topAvailable, topAvailable, leftAvailable, x[0][0], x[0][1], x[0][4],
                x[0][5], mBlock.refIdx[0][0], mb, 0, 0, 0, mbX, s.leftMBType, s.topMBType[mbX], P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(mBlock, 1, mBlock.subMbTypes[1], references, (mbX << 6) + 32, mbY << 6, x[0],
                s.mvTop[0][(mbX << 2) + 1], s.mvTop[0][(mbX << 2) + 2], s.mvTop[0][(mbX << 2) + 3],
                s.mvTop[0][(mbX << 2) + 4], x[0][1], x[0][5], topAvailable, topAvailable, topRightAvailable, true,
                x[0][2], x[0][3], x[0][6], x[0][7], mBlock.refIdx[0][1], mb, 8, 2, 0, mbX, P_8x8, s.topMBType[mbX],
                P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(mBlock, 2, mBlock.subMbTypes[2], references, mbX << 6, (mbY << 6) + 32, x[0], s.mvLeft[0][1],
                x[0][4], x[0][5], x[0][6], s.mvLeft[0][2], s.mvLeft[0][3], leftAvailable, true, true, leftAvailable,
                x[0][8], x[0][9], x[0][12], x[0][13], mBlock.refIdx[0][2], mb, 128, 0, 2, mbX, s.leftMBType, P_8x8,
                P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(mBlock, 3, mBlock.subMbTypes[3], references, (mbX << 6) + 32, (mbY << 6) + 32, x[0], x[0][5],
                x[0][6], x[0][7], null, x[0][9], x[0][13], true, true, false, true, x[0][10], x[0][11], x[0][14],
                x[0][15], mBlock.refIdx[0][3], mb, 136, 2, 2, mbX, P_8x8, P_8x8, P_8x8, L0, L0, L0, 0);

        savePrediction8x8(s, mbX, x[0], 0);
        Arrays.fill(pp, L0);
        int blk8x8X = mbX << 1;

        s.predModeLeft[0] = s.predModeLeft[1] = s.predModeTop[blk8x8X] = s.predModeTop[blk8x8X + 1] = L0;
    }

    private void readPrediction8x8P(MBlockP8x8 mBlock, boolean ref0, int mbX, boolean leftAvailable,
            boolean topAvailable) {
        mBlock.subMbTypes = new int[4];
        for (int i = 0; i < 4; i++) {
            mBlock.subMbTypes[i] = parser.readSubMBTypeP();
        }

        mBlock.refIdx = new int[1][4];
        if (s.numRef[0] > 1 && !ref0) {
            mBlock.refIdx[0][0] = parser.readRefIdx(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX], L0,
                    L0, L0, mbX, 0, 0, 2, 2, 0);
            mBlock.refIdx[0][1] = parser.readRefIdx(true, topAvailable, P_8x8, s.topMBType[mbX], L0, L0, L0, mbX, 2, 0,
                    2, 2, 0);
            mBlock.refIdx[0][2] = parser.readRefIdx(leftAvailable, true, s.leftMBType, P_8x8, L0, L0, L0, mbX, 0, 2, 2,
                    2, 0);
            mBlock.refIdx[0][3] = parser.readRefIdx(true, true, P_8x8, P_8x8, L0, L0, L0, mbX, 2, 2, 2, 2, 0);
        }

        readSubMb8x8(mBlock, 0, topAvailable, leftAvailable, 0, 0, mbX, s.leftMBType, s.topMBType[mbX], P_8x8, L0, L0,
                L0, 0);
        readSubMb8x8(mBlock, 1, topAvailable, true, 2, 0, mbX, P_8x8, s.topMBType[mbX], P_8x8, L0, L0, L0, 0);
        readSubMb8x8(mBlock, 2, true, leftAvailable, 0, 2, mbX, s.leftMBType, P_8x8, P_8x8, L0, L0, L0, 0);
        readSubMb8x8(mBlock, 3, true, true, 2, 2, mbX, P_8x8, P_8x8, P_8x8, L0, L0, L0, 0);
    }

    private void predict8x8B(MBlockP8x8 mBlock, Frame[][] refs, Picture8Bit mb, boolean ref0, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean topRightAvailable, int[][][] x,
            PartPred[] p) {

        for (int i = 0; i < 4; i++) {
            p[i] = bPartPredModes[mBlock.subMbTypes[i]];
        }

        int blk8x8X = mbX << 1;

        Picture8Bit[] mbb = { Picture8Bit.create(16, 16, s.chromaFormat), Picture8Bit.create(16, 16, s.chromaFormat) };

        PartPred[] _pp = new PartPred[4];
        for (int i = 0; i < 4; i++) {
            if (p[i] == Direct)
                bDirectDecoder.predictBDirect(refs, mbX, mbY, leftAvailable, topAvailable, tlAvailable,
                        topRightAvailable, x, _pp, mb, ARRAY[i]);
        }

        for (int list = 0; list < 2; list++) {
            if (p[0].usesList(list)) {
                decodeSubMb8x8(mBlock, 0, bSubMbTypes[mBlock.subMbTypes[0]], refs[list], mbX << 6, mbY << 6, x[list],
                        s.mvTopLeft[list], s.mvTop[list][mbX << 2], s.mvTop[list][(mbX << 2) + 1],
                        s.mvTop[list][(mbX << 2) + 2], s.mvLeft[list][0], s.mvLeft[list][1], tlAvailable, topAvailable,
                        topAvailable, leftAvailable, x[list][0], x[list][1], x[list][4], x[list][5],
                        mBlock.refIdx[list][0], mbb[list], 0, 0, 0, mbX, s.leftMBType, s.topMBType[mbX], B_8x8,
                        s.predModeLeft[0], s.predModeTop[blk8x8X], p[0], list);
            }
            if (p[1].usesList(list)) {
                decodeSubMb8x8(mBlock, 1, bSubMbTypes[mBlock.subMbTypes[1]], refs[list], (mbX << 6) + 32, mbY << 6,
                        x[list], s.mvTop[list][(mbX << 2) + 1], s.mvTop[list][(mbX << 2) + 2],
                        s.mvTop[list][(mbX << 2) + 3], s.mvTop[list][(mbX << 2) + 4], x[list][1], x[list][5],
                        topAvailable, topAvailable, topRightAvailable, true, x[list][2], x[list][3], x[list][6],
                        x[list][7], mBlock.refIdx[list][1], mbb[list], 8, 2, 0, mbX, B_8x8, s.topMBType[mbX], B_8x8,
                        p[0], s.predModeTop[blk8x8X + 1], p[1], list);
            }

            if (p[2].usesList(list)) {
                decodeSubMb8x8(mBlock, 2, bSubMbTypes[mBlock.subMbTypes[2]], refs[list], mbX << 6, (mbY << 6) + 32,
                        x[list], s.mvLeft[list][1], x[list][4], x[list][5], x[list][6], s.mvLeft[list][2],
                        s.mvLeft[list][3], leftAvailable, true, true, leftAvailable, x[list][8], x[list][9],
                        x[list][12], x[list][13], mBlock.refIdx[list][2], mbb[list], 128, 0, 2, mbX, s.leftMBType,
                        B_8x8, B_8x8, s.predModeLeft[1], p[0], p[2], list);
            }

            if (p[3].usesList(list)) {
                decodeSubMb8x8(mBlock, 3, bSubMbTypes[mBlock.subMbTypes[3]], refs[list], (mbX << 6) + 32,
                        (mbY << 6) + 32, x[list], x[list][5], x[list][6], x[list][7], null, x[list][9], x[list][13],
                        true, true, false, true, x[list][10], x[list][11], x[list][14], x[list][15],
                        mBlock.refIdx[list][3], mbb[list], 136, 2, 2, mbX, B_8x8, B_8x8, B_8x8, p[2], p[1], p[3], list);
            }
        }

        for (int i = 0; i < 4; i++) {
            int blk4x4 = BLK8x8_BLOCKS[i][0];
            mergePrediction(sh, x[0][blk4x4][2], x[1][blk4x4][2], p[i], 0, mbb[0].getPlaneData(0),
                    mbb[1].getPlaneData(0), BLK_8x8_MB_OFF_LUMA[i], 16, 8, 8, mb.getPlaneData(0), refs, poc);
        }

        s.predModeLeft[0] = p[1];
        s.predModeTop[blk8x8X] = p[2];
        s.predModeLeft[1] = s.predModeTop[blk8x8X + 1] = p[3];

        savePrediction8x8(s, mbX, x[0], 0);
        savePrediction8x8(s, mbX, x[1], 1);

        for (int i = 0; i < 4; i++)
            if (p[i] == Direct)
                p[i] = _pp[i];
    }

    private void readPrediction8x8B(MBlockP8x8 mBlock, int mbX, boolean leftAvailable, boolean topAvailable) {
        mBlock.subMbTypes = new int[4];
        PartPred[] p = new PartPred[4];
        for (int i = 0; i < 4; i++) {
            mBlock.subMbTypes[i] = parser.readSubMBTypeB();
            p[i] = bPartPredModes[mBlock.subMbTypes[i]];
        }

        mBlock.refIdx = new int[2][4];
        for (int list = 0; list < 2; list++) {
            if (s.numRef[list] <= 1)
                continue;
            if (p[0].usesList(list))
                mBlock.refIdx[list][0] = parser.readRefIdx(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                        s.predModeLeft[0], s.predModeTop[mbX << 1], p[0], mbX, 0, 0, 2, 2, list);
            if (p[1].usesList(list))
                mBlock.refIdx[list][1] = parser.readRefIdx(true, topAvailable, B_8x8, s.topMBType[mbX], p[0],
                        s.predModeTop[(mbX << 1) + 1], p[1], mbX, 2, 0, 2, 2, list);
            if (p[2].usesList(list))
                mBlock.refIdx[list][2] = parser.readRefIdx(leftAvailable, true, s.leftMBType, B_8x8, s.predModeLeft[1],
                        p[0], p[2], mbX, 0, 2, 2, 2, list);
            if (p[3].usesList(list))
                mBlock.refIdx[list][3] = parser.readRefIdx(true, true, B_8x8, B_8x8, p[2], p[1], p[3], mbX, 2, 2, 2, 2,
                        list);
        }

        int blk8x8X = mbX << 1;
        for (int list = 0; list < 2; list++) {
            if (p[0].usesList(list)) {
                readSubMb8x8(mBlock, 0, topAvailable, leftAvailable, 0, 0, mbX, s.leftMBType, s.topMBType[mbX], B_8x8,
                        s.predModeLeft[0], s.predModeTop[blk8x8X], p[0], list);
            }
            if (p[1].usesList(list)) {
                readSubMb8x8(mBlock, 1, topAvailable, true, 2, 0, mbX, B_8x8, s.topMBType[mbX], B_8x8, p[0],
                        s.predModeTop[blk8x8X + 1], p[1], list);
            }

            if (p[2].usesList(list)) {
                readSubMb8x8(mBlock, 2, true, leftAvailable, 0, 2, mbX, s.leftMBType, B_8x8, B_8x8, s.predModeLeft[1],
                        p[0], p[2], list);
            }

            if (p[3].usesList(list)) {
                readSubMb8x8(mBlock, 3, true, true, 2, 2, mbX, B_8x8, B_8x8, B_8x8, p[2], p[1], p[3], list);
            }
        }
    }

    private void decodeSubMb8x8(MBlockP8x8 mBlock, int partNo, int subMbType, Picture8Bit[] references, int offX,
            int offY, int[][] x, int[] tl, int[] t0, int[] t1, int[] tr, int[] l0, int[] l1, boolean tlAvb,
            boolean tAvb, boolean trAvb, boolean lAvb, int[] x00, int[] x01, int[] x10, int[] x11, int refIdx,
            Picture8Bit mb, int off, int blk8x8X, int blk8x8Y, int mbX, MBType leftMBType, MBType topMBType,
            MBType curMBType, PartPred leftPred, PartPred topPred, PartPred partPred, int list) {

        x00[2] = x01[2] = x10[2] = x11[2] = refIdx;

        switch (subMbType) {
        case 3:
            decodeSub4x4(mBlock, partNo, references, offX, offY, tl, t0, t1, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x00,
                    x01, x10, x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
            break;
        case 2:
            decodeSub4x8(mBlock, partNo, references, offX, offY, tl, t0, t1, tr, l0, tlAvb, tAvb, trAvb, lAvb, x00,
                    x01, x10, x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
            break;
        case 1:
            decodeSub8x4(mBlock, partNo, references, offX, offY, tl, t0, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x00,
                    x01, x10, x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
            break;
        case 0:
            decodeSub8x8(mBlock, partNo, references, offX, offY, tl, t0, tr, l0, tlAvb, tAvb, trAvb, lAvb, x00, x01,
                    x10, x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
        }
    }

    private void readSubMb8x8(MBlockP8x8 mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        switch (mBlock.subMbTypes[partNo]) {
        case 3:
            readSub4x4(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
            break;
        case 2:
            readSub4x8(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
            break;
        case 1:
            readSub8x4(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
            break;
        case 0:
            readSub8x8(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, leftPred, topPred,
                    partPred, list);
        }
    }

    private void decodeSub8x8(MBlockP8x8 mBlock, int partNo, Picture8Bit[] references, int offX, int offY, int[] tl,
            int[] t0, int[] tr, int[] l0, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, int[] x00,
            int[] x01, int[] x10, int[] x11, int refIdx, Picture8Bit mb, int off, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvpX = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        int mvpY = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        x00[0] = x01[0] = x10[0] = x11[0] = mBlock.mvdX1[partNo] + mvpX;
        x00[1] = x01[1] = x10[1] = x11[1] = mBlock.mvdY1[partNo] + mvpY;

        debugPrint("MVP: (" + mvpX + ", " + mvpY + "), MVD: (" + mBlock.mvdX1[partNo] + ", " + mBlock.mvdY1[partNo]
                + "), MV: (" + x00[0] + "," + x00[1] + "," + refIdx + ")");
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 8, 8);
    }

    private void readSub8x8(MBlockP8x8 mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, PartPred leftPred, PartPred topPred, PartPred partPred,
            int list) {
        mBlock.mvdX1[partNo] = parser.readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 2, list);
        mBlock.mvdY1[partNo] = parser.readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 2, list);
    }

    private void decodeSub8x4(MBlockP8x8 mBlock, int partNo, Picture8Bit[] references, int offX, int offY, int[] tl,
            int[] t0, int[] tr, int[] l0, int[] l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb,
            int[] x00, int[] x01, int[] x10, int[] x11, int refIdx, Picture8Bit mb, int off, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvpX1 = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        x00[0] = x01[0] = mBlock.mvdX1[partNo] + mvpX1;
        x00[1] = x01[1] = mBlock.mvdY1[partNo] + mvpY1;

        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mBlock.mvdX1[partNo] + ", " + mBlock.mvdY1[partNo]
                + "), MV: (" + x00[0] + "," + x00[1] + "," + refIdx + ")");

        int mvpX2 = calcMVPredictionMedian(l1, x00, NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(l1, x00, NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 1);

        x10[0] = x11[0] = mBlock.mvdX2[partNo] + mvpX2;
        x10[1] = x11[1] = mBlock.mvdY2[partNo] + mvpY2;

        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mBlock.mvdX2[partNo] + ", " + mBlock.mvdY2[partNo]
                + "), MV: (" + x10[0] + "," + x10[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 8, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + x10[0], offY + x10[1]
                + 16, 8, 4);
    }

    private void readSub8x4(MBlockP8x8 mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        mBlock.mvdX1[partNo] = parser.readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 1, list);
        mBlock.mvdY1[partNo] = parser.readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 1, list);

        mBlock.mvdX2[partNo] = parser.readMVD(0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 2, 1, list);
        mBlock.mvdY2[partNo] = parser.readMVD(1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 2, 1, list);
    }

    private void decodeSub4x8(MBlockP8x8 mBlock, int partNo, Picture8Bit[] references, int offX, int offY, int[] tl,
            int[] t0, int[] t1, int[] tr, int[] l0, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb,
            int[] x00, int[] x01, int[] x10, int[] x11, int refIdx, Picture8Bit mb, int off, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvpX1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        x00[0] = x10[0] = mBlock.mvdX1[partNo] + mvpX1;
        x00[1] = x10[1] = mBlock.mvdY1[partNo] + mvpY1;

        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mBlock.mvdX1 + ", " + mBlock.mvdY1 + "), MV: ("
                + x00[0] + "," + x00[1] + "," + refIdx + ")");

        int mvpX2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        x01[0] = x11[0] = mBlock.mvdX2[partNo] + mvpX2;
        x01[1] = x11[1] = mBlock.mvdY2[partNo] + mvpY2;

        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mBlock.mvdX2[partNo] + ", " + mBlock.mvdY2[partNo]
                + "), MV: (" + x01[0] + "," + x01[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 4, 8);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + x01[0] + 16, offY + x01[1], 4, 8);
    }

    private void readSub4x8(MBlockP8x8 mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        mBlock.mvdX1[partNo] = parser.readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 2, list);
        mBlock.mvdY1[partNo] = parser.readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 2, list);
        mBlock.mvdX2[partNo] = parser.readMVD(0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 2, list);
        mBlock.mvdY2[partNo] = parser.readMVD(1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 2, list);
    }

    private void decodeSub4x4(MBlockP8x8 mBlock, int partNo, Picture8Bit[] references, int offX, int offY, int[] tl,
            int[] t0, int[] t1, int[] tr, int[] l0, int[] l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb,
            int[] x00, int[] x01, int[] x10, int[] x11, int refIdx, Picture8Bit mb, int off, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvpX1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        x00[0] = mBlock.mvdX1[partNo] + mvpX1;
        x00[1] = mBlock.mvdY1[partNo] + mvpY1;
        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mBlock.mvdX1[partNo] + ", " + mBlock.mvdY1[partNo]
                + "), MV: (" + x00[0] + "," + x00[1] + "," + refIdx + ")");

        int mvpX2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        x01[0] = mBlock.mvdX2[partNo] + mvpX2;
        x01[1] = mBlock.mvdY2[partNo] + mvpY2;
        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mBlock.mvdX2 + ", " + mBlock.mvdY2 + "), MV: ("
                + x01[0] + "," + x01[1] + "," + refIdx + ")");

        int mvpX3 = calcMVPredictionMedian(l1, x00, x01, l0, lAvb, true, true, lAvb, refIdx, 0);
        int mvpY3 = calcMVPredictionMedian(l1, x00, x01, l0, lAvb, true, true, lAvb, refIdx, 1);

        x10[0] = mBlock.mvdX3[partNo] + mvpX3;
        x10[1] = mBlock.mvdY3[partNo] + mvpY3;

        debugPrint("MVP: (" + mvpX3 + ", " + mvpY3 + "), MVD: (" + mBlock.mvdX3[partNo] + ", " + mBlock.mvdY3[partNo]
                + "), MV: (" + x10[0] + "," + x10[1] + "," + refIdx + ")");

        int mvpX4 = calcMVPredictionMedian(x10, x01, NULL_VECTOR, x00, true, true, false, true, refIdx, 0);
        int mvpY4 = calcMVPredictionMedian(x10, x01, NULL_VECTOR, x00, true, true, false, true, refIdx, 1);

        x11[0] = mBlock.mvdX4[partNo] + mvpX4;
        x11[1] = mBlock.mvdY4[partNo] + mvpY4;

        debugPrint("MVP: (" + mvpX4 + ", " + mvpY4 + "), MVD: (" + mBlock.mvdX4[partNo] + ", " + mBlock.mvdY4[partNo]
                + "), MV: (" + x11[0] + "," + x11[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + x01[0] + 16, offY + x01[1], 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + x10[0], offY + x10[1]
                + 16, 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4 + 4, offX + x11[0] + 16, offY
                + x11[1] + 16, 4, 4);
    }

    private void readSub4x4(MBlockP8x8 mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        mBlock.mvdX1[partNo] = parser.readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 1, list);
        mBlock.mvdY1[partNo] = parser.readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 1, list);
        mBlock.mvdX2[partNo] = parser.readMVD(0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 1, list);
        mBlock.mvdY2[partNo] = parser.readMVD(1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 1, list);
        mBlock.mvdX3[partNo] = parser.readMVD(0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 1, 1, list);
        mBlock.mvdY3[partNo] = parser.readMVD(1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 1, 1, list);
        mBlock.mvdX4[partNo] = parser.readMVD(0, true, true, curMBType, curMBType, partPred, partPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y + 1, 1, 1, list);
        mBlock.mvdY4[partNo] = parser.readMVD(1, true, true, curMBType, curMBType, partPred, partPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y + 1, 1, 1, list);
    }
}
