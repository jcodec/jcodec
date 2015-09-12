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
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderInter8x8 extends MBlockDecoderBase {
    private Mapper mapper;
    private MBlockDecoderBDirect bDirectDecoder;

    public MBlockDecoderInter8x8(Mapper mapper, BitstreamParser parser, MBlockDecoderBDirect bDirectDecoder, SliceHeader sh,
            DeblockerInput di, int poc, DecoderState sharedState) {
        super(parser, sh, di, poc, sharedState);
        this.mapper = mapper;
        this.bDirectDecoder = bDirectDecoder;
    }

    public void decode(BitReader reader, int mb_type, Frame[][] references, Picture8Bit mb,
            SliceType sliceType, int mbIdx, boolean mb_field_decoding_flag, MBType prevMbType, boolean ref0) {

        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        int mbAddr = mapper.getAddress(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);

        int[][][] x = new int[2][16][3];
        PartPred[] pp = new PartPred[4];
        for (int i = 0; i < 16; i++)
            x[0][i][2] = x[1][i][2] = -1;

        MBType curMBType;
        boolean noSubMBLessThen8x8;
        if (sliceType == SliceType.P) {
            noSubMBLessThen8x8 = predict8x8P(reader, references[0], mb, ref0, mbX, mbY, leftAvailable, topAvailable,
                    topLeftAvailable, topRightAvailable, x, pp);
            curMBType = P_8x8;
        } else {
            noSubMBLessThen8x8 = predict8x8B(reader, references, mb, ref0, mbX, mbY, leftAvailable, topAvailable,
                    topLeftAvailable, topRightAvailable, x, pp);
            curMBType = B_8x8;
        }

        predictChromaInter(references, x, mbX << 3, mbY << 3, 1, mb, pp);
        predictChromaInter(references, x, mbX << 3, mbY << 3, 2, mb, pp);

        int codedBlockPattern = parser.readCodedBlockPatternInter(reader, leftAvailable, topAvailable,
                sharedState.leftCBPLuma | (sharedState.leftCBPChroma << 4), sharedState.topCBPLuma[mbX]
                        | (sharedState.topCBPChroma[mbX] << 4), sharedState.leftMBType, sharedState.topMBType[mbX]);

        int cbpLuma = codedBlockPattern & 0xf;
        int cbpChroma = codedBlockPattern >> 4;

        boolean transform8x8Used = false;
        if (sharedState.transform8x8 && cbpLuma != 0 && noSubMBLessThen8x8) {
            transform8x8Used = parser.readTransform8x8Flag(reader, leftAvailable, topAvailable, sharedState.leftMBType,
                    sharedState.topMBType[mbX], sharedState.tf8x8Left, sharedState.tf8x8Top[mbX]);
        }

        if (cbpLuma > 0 || cbpChroma > 0) {
            sharedState.qp = (sharedState.qp + parser.readMBQpDelta(reader, prevMbType) + 52) % 52;
        }
        di.mbQps[0][mbAddr] = sharedState.qp;

        int[][][] residual = { transform8x8Used ? new int[4][64] : new int[16][16], new int[4][16], new int[4][16] };

        residualLuma(reader, leftAvailable, topAvailable, mbX, mbY, codedBlockPattern, curMBType, transform8x8Used,
                sharedState.tf8x8Left, sharedState.tf8x8Top[mbX], residual[0]);

        saveMvs(di, x, mbX, mbY);

        int qp1 = calcQpChroma(sharedState.qp, sharedState.chromaQpOffset[0]);
        int qp2 = calcQpChroma(sharedState.qp, sharedState.chromaQpOffset[1]);

        decodeChromaResidual(reader, leftAvailable, topAvailable, mbX, mbY, codedBlockPattern >> 4, qp1, qp2,
                MBType.P_16x16, residual[1], residual[2]);

        di.mbQps[1][mbAddr] = qp1;
        di.mbQps[2][mbAddr] = qp2;

        mergeResidual(mb, residual, transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(sharedState, mb, mbX);

        di.mbTypes[mbAddr] = sharedState.topMBType[mbX] = sharedState.leftMBType = curMBType;
        sharedState.topCBPLuma[mbX] = sharedState.leftCBPLuma = cbpLuma;
        sharedState.topCBPChroma[mbX] = sharedState.leftCBPChroma = cbpChroma;
        sharedState.tf8x8Left = sharedState.tf8x8Top[mbX] = transform8x8Used;
        di.tr8x8Used[mbAddr] = transform8x8Used;
    }

    private boolean predict8x8P(BitReader reader, Picture8Bit[] references, Picture8Bit mb, boolean ref0, int mbX,
            int mbY, boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean topRightAvailable,
            int[][][] x, PartPred[] pp) {
        int[] subMbTypes = new int[4];
        for (int i = 0; i < 4; i++) {
            subMbTypes[i] = parser.readSubMBTypeP(reader);
        }
        Arrays.fill(pp, L0);
        int blk8x8X = mbX << 1;

        int[] refIdx = new int[4];
        if (sharedState.numRef[0] > 1 && !ref0) {
            refIdx[0] = parser.readRefIdx(reader, leftAvailable, topAvailable, sharedState.leftMBType,
                    sharedState.topMBType[mbX], L0, L0, L0, mbX, 0, 0, 2, 2, 0);
            refIdx[1] = parser.readRefIdx(reader, true, topAvailable, P_8x8, sharedState.topMBType[mbX], L0, L0, L0,
                    mbX, 2, 0, 2, 2, 0);
            refIdx[2] = parser.readRefIdx(reader, leftAvailable, true, sharedState.leftMBType, P_8x8, L0, L0, L0, mbX,
                    0, 2, 2, 2, 0);
            refIdx[3] = parser.readRefIdx(reader, true, true, P_8x8, P_8x8, L0, L0, L0, mbX, 2, 2, 2, 2, 0);
        }

        decodeSubMb8x8(reader, subMbTypes[0], references, mbX << 6, mbY << 6, x[0], sharedState.mvTopLeft[0],
                sharedState.mvTop[0][mbX << 2], sharedState.mvTop[0][(mbX << 2) + 1],
                sharedState.mvTop[0][(mbX << 2) + 2], sharedState.mvLeft[0][0], sharedState.mvLeft[0][1], tlAvailable,
                topAvailable, topAvailable, leftAvailable, x[0][0], x[0][1], x[0][4], x[0][5], refIdx[0], mb, 0, 0, 0,
                mbX, sharedState.leftMBType, sharedState.topMBType[mbX], P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(reader, subMbTypes[1], references, (mbX << 6) + 32, mbY << 6, x[0],
                sharedState.mvTop[0][(mbX << 2) + 1], sharedState.mvTop[0][(mbX << 2) + 2],
                sharedState.mvTop[0][(mbX << 2) + 3], sharedState.mvTop[0][(mbX << 2) + 4], x[0][1], x[0][5],
                topAvailable, topAvailable, topRightAvailable, true, x[0][2], x[0][3], x[0][6], x[0][7], refIdx[1], mb,
                8, 2, 0, mbX, P_8x8, sharedState.topMBType[mbX], P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(reader, subMbTypes[2], references, mbX << 6, (mbY << 6) + 32, x[0], sharedState.mvLeft[0][1],
                x[0][4], x[0][5], x[0][6], sharedState.mvLeft[0][2], sharedState.mvLeft[0][3], leftAvailable, true,
                true, leftAvailable, x[0][8], x[0][9], x[0][12], x[0][13], refIdx[2], mb, 128, 0, 2, mbX,
                sharedState.leftMBType, P_8x8, P_8x8, L0, L0, L0, 0);

        decodeSubMb8x8(reader, subMbTypes[3], references, (mbX << 6) + 32, (mbY << 6) + 32, x[0], x[0][5], x[0][6],
                x[0][7], null, x[0][9], x[0][13], true, true, false, true, x[0][10], x[0][11], x[0][14], x[0][15],
                refIdx[3], mb, 136, 2, 2, mbX, P_8x8, P_8x8, P_8x8, L0, L0, L0, 0);

        savePrediction8x8(sharedState, mbX, x[0], 0);

        sharedState.predModeLeft[0] = sharedState.predModeLeft[1] = sharedState.predModeTop[blk8x8X] = sharedState.predModeTop[blk8x8X + 1] = L0;

        return subMbTypes[0] == 0 && subMbTypes[1] == 0 && subMbTypes[2] == 0 && subMbTypes[3] == 0;
    }

    private boolean predict8x8B(BitReader reader, Frame[][] refs, Picture8Bit mb, boolean ref0,
            int mbX, int mbY, boolean leftAvailable, boolean topAvailable, boolean tlAvailable,
            boolean topRightAvailable, int[][][] x, PartPred[] p) {

        int[] subMbTypes = new int[4];
        for (int i = 0; i < 4; i++) {
            subMbTypes[i] = parser.readSubMBTypeB(reader);
            p[i] = bPartPredModes[subMbTypes[i]];
        }

        int[][] refIdx = new int[2][4];
        for (int list = 0; list < 2; list++) {
            if (sharedState.numRef[list] <= 1)
                continue;
            if (p[0].usesList(list))
                refIdx[list][0] = parser.readRefIdx(reader, leftAvailable, topAvailable, sharedState.leftMBType,
                        sharedState.topMBType[mbX], sharedState.predModeLeft[0], sharedState.predModeTop[mbX << 1],
                        p[0], mbX, 0, 0, 2, 2, list);
            if (p[1].usesList(list))
                refIdx[list][1] = parser.readRefIdx(reader, true, topAvailable, B_8x8, sharedState.topMBType[mbX],
                        p[0], sharedState.predModeTop[(mbX << 1) + 1], p[1], mbX, 2, 0, 2, 2, list);
            if (p[2].usesList(list))
                refIdx[list][2] = parser.readRefIdx(reader, leftAvailable, true, sharedState.leftMBType, B_8x8,
                        sharedState.predModeLeft[1], p[0], p[2], mbX, 0, 2, 2, 2, list);
            if (p[3].usesList(list))
                refIdx[list][3] = parser.readRefIdx(reader, true, true, B_8x8, B_8x8, p[2], p[1], p[3], mbX, 2, 2, 2,
                        2, list);
        }

        Picture8Bit[] mbb = { Picture8Bit.create(16, 16, sharedState.chromaFormat),
                Picture8Bit.create(16, 16, sharedState.chromaFormat) };

        PartPred[] _pp = new PartPred[4];
        for (int i = 0; i < 4; i++) {
            if (p[i] == Direct)
                bDirectDecoder.predictBDirect(refs, mbX, mbY, leftAvailable, topAvailable, tlAvailable,
                        topRightAvailable, x, _pp, mb, ARRAY[i]);
        }

        int blk8x8X = mbX << 1;
        for (int list = 0; list < 2; list++) {
            if (p[0].usesList(list))
                decodeSubMb8x8(reader, bSubMbTypes[subMbTypes[0]], refs[list], mbX << 6, mbY << 6, x[list],
                        sharedState.mvTopLeft[list], sharedState.mvTop[list][mbX << 2],
                        sharedState.mvTop[list][(mbX << 2) + 1], sharedState.mvTop[list][(mbX << 2) + 2],
                        sharedState.mvLeft[list][0], sharedState.mvLeft[list][1], tlAvailable, topAvailable,
                        topAvailable, leftAvailable, x[list][0], x[list][1], x[list][4], x[list][5], refIdx[list][0],
                        mbb[list], 0, 0, 0, mbX, sharedState.leftMBType, sharedState.topMBType[mbX], B_8x8,
                        sharedState.predModeLeft[0], sharedState.predModeTop[blk8x8X], p[0], list);

            if (p[1].usesList(list))
                decodeSubMb8x8(reader, bSubMbTypes[subMbTypes[1]], refs[list], (mbX << 6) + 32, mbY << 6, x[list],
                        sharedState.mvTop[list][(mbX << 2) + 1], sharedState.mvTop[list][(mbX << 2) + 2],
                        sharedState.mvTop[list][(mbX << 2) + 3], sharedState.mvTop[list][(mbX << 2) + 4], x[list][1],
                        x[list][5], topAvailable, topAvailable, topRightAvailable, true, x[list][2], x[list][3],
                        x[list][6], x[list][7], refIdx[list][1], mbb[list], 8, 2, 0, mbX, B_8x8,
                        sharedState.topMBType[mbX], B_8x8, p[0], sharedState.predModeTop[blk8x8X + 1], p[1], list);

            if (p[2].usesList(list))
                decodeSubMb8x8(reader, bSubMbTypes[subMbTypes[2]], refs[list], mbX << 6, (mbY << 6) + 32, x[list],
                        sharedState.mvLeft[list][1], x[list][4], x[list][5], x[list][6], sharedState.mvLeft[list][2],
                        sharedState.mvLeft[list][3], leftAvailable, true, true, leftAvailable, x[list][8], x[list][9],
                        x[list][12], x[list][13], refIdx[list][2], mbb[list], 128, 0, 2, mbX, sharedState.leftMBType,
                        B_8x8, B_8x8, sharedState.predModeLeft[1], p[0], p[2], list);

            if (p[3].usesList(list))
                decodeSubMb8x8(reader, bSubMbTypes[subMbTypes[3]], refs[list], (mbX << 6) + 32, (mbY << 6) + 32,
                        x[list], x[list][5], x[list][6], x[list][7], null, x[list][9], x[list][13], true, true, false,
                        true, x[list][10], x[list][11], x[list][14], x[list][15], refIdx[list][3], mbb[list], 136, 2,
                        2, mbX, B_8x8, B_8x8, B_8x8, p[2], p[1], p[3], list);
        }

        for (int i = 0; i < 4; i++) {
            int blk4x4 = BLK8x8_BLOCKS[i][0];
            mergePrediction(sh, x[0][blk4x4][2], x[1][blk4x4][2], p[i], 0, mbb[0].getPlaneData(0),
                    mbb[1].getPlaneData(0), BLK_8x8_MB_OFF_LUMA[i], 16, 8, 8, mb.getPlaneData(0), refs,
                    poc);
        }

        sharedState.predModeLeft[0] = p[1];
        sharedState.predModeTop[blk8x8X] = p[2];
        sharedState.predModeLeft[1] = sharedState.predModeTop[blk8x8X + 1] = p[3];

        savePrediction8x8(sharedState, mbX, x[0], 0);
        savePrediction8x8(sharedState, mbX, x[1], 1);

        for (int i = 0; i < 4; i++)
            if (p[i] == Direct)
                p[i] = _pp[i];

        return bSubMbTypes[subMbTypes[0]] == 0 && bSubMbTypes[subMbTypes[1]] == 0 && bSubMbTypes[subMbTypes[2]] == 0
                && bSubMbTypes[subMbTypes[3]] == 0;
    }

    private void decodeSubMb8x8(BitReader reader, int subMbType, Picture8Bit[] references, int offX, int offY,
            int[][] x, int[] tl, int[] t0, int[] t1, int[] tr, int[] l0, int[] l1, boolean tlAvb, boolean tAvb,
            boolean trAvb, boolean lAvb, int[] x00, int[] x01, int[] x10, int[] x11, int refIdx, Picture8Bit mb,
            int off, int blk8x8X, int blk8x8Y, int mbX, MBType leftMBType, MBType topMBType, MBType curMBType,
            PartPred leftPred, PartPred topPred, PartPred partPred, int list) {

        x00[2] = x01[2] = x10[2] = x11[2] = refIdx;

        switch (subMbType) {
        case 3:
            decodeSub4x4(reader, references, offX, offY, tl, t0, t1, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x00, x01,
                    x10, x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list);
            break;
        case 2:
            decodeSub4x8(reader, references, offX, offY, tl, t0, t1, tr, l0, tlAvb, tAvb, trAvb, lAvb, x00, x01, x10,
                    x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred,
                    partPred, list);
            break;
        case 1:
            decodeSub8x4(reader, references, offX, offY, tl, t0, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x00, x01, x10,
                    x11, refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred,
                    partPred, list);
            break;
        case 0:
            decodeSub8x8(reader, references, offX, offY, tl, t0, tr, l0, tlAvb, tAvb, trAvb, lAvb, x00, x01, x10, x11,
                    refIdx, mb, off, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred,
                    partPred, list);
        }
    }

    private void decodeSub8x8(BitReader reader, Picture8Bit[] references, int offX, int offY, int[] tl, int[] t0,
            int[] tr, int[] l0, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, int[] x00, int[] x01,
            int[] x10, int[] x11, int refIdx, Picture8Bit mb, int off, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvdX = parser.readMVD(reader, 0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 2, list);
        int mvdY = parser.readMVD(reader, 1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 2, list);

        int mvpX = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        int mvpY = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        x00[0] = x01[0] = x10[0] = x11[0] = mvdX + mvpX;
        x00[1] = x01[1] = x10[1] = x11[1] = mvpY + mvdY;

        debugPrint("MVP: (" + mvpX + ", " + mvpY + "), MVD: (" + mvdX + ", " + mvdY + "), MV: (" + x00[0] + ","
                + x00[1] + "," + refIdx + ")");
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 8, 8);
    }

    private void decodeSub8x4(BitReader reader, Picture8Bit[] references, int offX, int offY, int[] tl, int[] t0,
            int[] tr, int[] l0, int[] l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, int[] x00,
            int[] x01, int[] x10, int[] x11, int refIdx, Picture8Bit mb, int off, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvdX1 = parser.readMVD(reader, 0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 1, list);
        int mvdY1 = parser.readMVD(reader, 1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 1, list);

        int mvpX1 = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        x00[0] = x01[0] = mvdX1 + mvpX1;
        x00[1] = x01[1] = mvdY1 + mvpY1;

        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mvdX1 + ", " + mvdY1 + "), MV: (" + x00[0] + ","
                + x00[1] + "," + refIdx + ")");

        int mvdX2 = parser.readMVD(reader, 0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 2, 1, list);
        int mvdY2 = parser.readMVD(reader, 1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 2, 1, list);

        int mvpX2 = calcMVPredictionMedian(l1, x00, NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(l1, x00, NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 1);

        x10[0] = x11[0] = mvdX2 + mvpX2;
        x10[1] = x11[1] = mvdY2 + mvpY2;

        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mvdX2 + ", " + mvdY2 + "), MV: (" + x10[0] + ","
                + x10[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 8, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + x10[0], offY + x10[1]
                + 16, 8, 4);
    }

    private void decodeSub4x8(BitReader reader, Picture8Bit[] references, int offX, int offY, int[] tl, int[] t0,
            int[] t1, int[] tr, int[] l0, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb, int[] x00,
            int[] x01, int[] x10, int[] x11, int refIdx, Picture8Bit mb, int off, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {

        int mvdX1 = parser.readMVD(reader, 0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 2, list);
        int mvdY1 = parser.readMVD(reader, 1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 2, list);

        int mvpX1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        x00[0] = x10[0] = mvdX1 + mvpX1;
        x00[1] = x10[1] = mvdY1 + mvpY1;

        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mvdX1 + ", " + mvdY1 + "), MV: (" + x00[0] + ","
                + x00[1] + "," + refIdx + ")");

        int mvdX2 = parser.readMVD(reader, 0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 2, list);
        int mvdY2 = parser.readMVD(reader, 1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 2, list);

        int mvpX2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        x01[0] = x11[0] = mvdX2 + mvpX2;
        x01[1] = x11[1] = mvdY2 + mvpY2;

        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mvdX2 + ", " + mvdY2 + "), MV: (" + x01[0] + ","
                + x01[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 4, 8);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + x01[0] + 16, offY + x01[1], 4, 8);
    }

    private void decodeSub4x4(BitReader reader, Picture8Bit[] references, int offX, int offY, int[] tl, int[] t0,
            int[] t1, int[] tr, int[] l0, int[] l1, boolean tlAvb, boolean tAvb, boolean trAvb, boolean lAvb,
            int[] x00, int[] x01, int[] x10, int[] x11, int refIdx, Picture8Bit mb, int off, int blk8x8X, int blk8x8Y,
            int mbX, MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        int mvdX1 = parser.readMVD(reader, 0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 1, list);
        int mvdY1 = parser.readMVD(reader, 1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 1, list);

        int mvpX1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        int mvpY1 = calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        x00[0] = mvdX1 + mvpX1;
        x00[1] = mvdY1 + mvpY1;
        debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mvdX1 + ", " + mvdY1 + "), MV: (" + x00[0] + ","
                + x00[1] + "," + refIdx + ")");

        int mvdX2 = parser.readMVD(reader, 0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 1, list);
        int mvdY2 = parser.readMVD(reader, 1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 1, list);

        int mvpX2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        int mvpY2 = calcMVPredictionMedian(x00, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        x01[0] = mvdX2 + mvpX2;
        x01[1] = mvdY2 + mvpY2;
        debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mvdX2 + ", " + mvdY2 + "), MV: (" + x01[0] + ","
                + x01[1] + "," + refIdx + ")");

        int mvdX3 = parser.readMVD(reader, 0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 1, 1, list);
        int mvdY3 = parser.readMVD(reader, 1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 1, 1, list);

        int mvpX3 = calcMVPredictionMedian(l1, x00, x01, l0, lAvb, true, true, lAvb, refIdx, 0);
        int mvpY3 = calcMVPredictionMedian(l1, x00, x01, l0, lAvb, true, true, lAvb, refIdx, 1);

        x10[0] = mvdX3 + mvpX3;
        x10[1] = mvdY3 + mvpY3;

        debugPrint("MVP: (" + mvpX3 + ", " + mvpY3 + "), MVD: (" + mvdX3 + ", " + mvdY3 + "), MV: (" + x10[0] + ","
                + x10[1] + "," + refIdx + ")");

        int mvdX4 = parser.readMVD(reader, 0, true, true, curMBType, curMBType, partPred, partPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y + 1, 1, 1, list);
        int mvdY4 = parser.readMVD(reader, 1, true, true, curMBType, curMBType, partPred, partPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y + 1, 1, 1, list);

        int mvpX4 = calcMVPredictionMedian(x10, x01, NULL_VECTOR, x00, true, true, false, true, refIdx, 0);
        int mvpY4 = calcMVPredictionMedian(x10, x01, NULL_VECTOR, x00, true, true, false, true, refIdx, 1);

        x11[0] = mvdX4 + mvpX4;
        x11[1] = mvdY4 + mvpY4;

        debugPrint("MVP: (" + mvpX4 + ", " + mvpY4 + "), MVD: (" + mvdX4 + ", " + mvdY4 + "), MV: (" + x11[0] + ","
                + x11[1] + "," + refIdx + ")");

        BlockInterpolator.getBlockLuma(references[refIdx], mb, off, offX + x00[0], offY + x00[1], 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + x01[0] + 16, offY + x01[1], 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + x10[0], offY + x10[1]
                + 16, 4, 4);
        BlockInterpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4 + 4, offX + x11[0] + 16, offY
                + x11[1] + 16, 4, 4);
    }
}
