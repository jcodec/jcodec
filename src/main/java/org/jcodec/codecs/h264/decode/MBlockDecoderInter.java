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
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderInter extends MBlockDecoderBase {
    private Mapper mapper;

    public MBlockDecoderInter(Mapper mapper, BitstreamParser parser, SliceHeader sh, DeblockerInput di, int poc,
            DecoderState decoderState) {
        super(parser, sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode16x16(Picture8Bit mb, Frame[][] refs, int mbIdx, MBType prevMbType, PartPred p0, MBType curMBType) {

        MBlockP16x16 mBlock = new MBlockP16x16();

        readInter16x16(mbIdx, prevMbType, p0, curMBType, mBlock);

        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);

        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);
        int address = mapper.getAddress(mbIdx);
        int[][][] x = new int[2][][];
        int xx = mbX << 2;
        Picture8Bit[] mbb = { Picture8Bit.create(16, 16, s.chromaFormat), Picture8Bit.create(16, 16, s.chromaFormat) };
        for (int list = 0; list < 2; list++) {
            predictInter16x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, x, xx, list, p0);
        }

        PredictionMerger.mergePrediction(sh, x[0][0][2], x[1][0][2], p0, 0, mbb[0].getPlaneData(0),
                mbb[1].getPlaneData(0), 0, 16, 16, 16, mb.getPlaneData(0), refs, poc);

        s.predModeLeft[0] = s.predModeLeft[1] = s.predModeTop[mbX << 1] = s.predModeTop[(mbX << 1) + 1] = p0;

        PartPred[] partPreds = new PartPred[] { p0, p0, p0, p0 };
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 1, mb, partPreds);
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 2, mb, partPreds);

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, x, partPreds, mapper.getAddress(mbIdx),
                prevMbType, curMBType);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = s.topMBType[mbX] = s.leftMBType = curMBType;
    }

    private void readInter16x16(int mbIdx, MBType prevMbType, PartPred p0, MBType curMBType, MBlockP16x16 mBlock) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);

        mBlock.refIdx = new int[] { 0, 0 };
        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && s.numRef[list] > 1)
                mBlock.refIdx[list] = parser.readRefIdx(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                        s.predModeLeft[0], s.predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 4, list);
        }
        for (int list = 0; list < 2; list++) {
            readPredictionInter16x16(mBlock, mbX, leftAvailable, topAvailable, list, p0);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY, prevMbType, curMBType);
    }

    private void predictInter8x16(MBlockP8x16 mBlock, Picture8Bit mb, Picture8Bit[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int[][][] x,
            int list, PartPred p0, PartPred p1) {
        int xx = mbX << 2;

        int mvX1 = 0, mvY1 = 0, r1 = -1, mvX2 = 0, mvY2 = 0, r2 = -1;
        if (p0.usesList(list)) {
            int mvpX1 = calcMVPrediction8x16Left(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 2], s.mvTopLeft[list], leftAvailable, topAvailable, topAvailable,
                    tlAvailable, mBlock.refIdx1[list], 0);
            int mvpY1 = calcMVPrediction8x16Left(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 2], s.mvTopLeft[list], leftAvailable, topAvailable, topAvailable,
                    tlAvailable, mBlock.refIdx1[list], 1);

            mvX1 = mBlock.mvdX1[list] + mvpX1;
            mvY1 = mBlock.mvdY1[list] + mvpY1;

            debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mBlock.mvdX1 + ", " + mBlock.mvdY1 + "), MV: ("
                    + mvX1 + "," + mvY1 + "," + mBlock.refIdx1[list] + ")");

            BlockInterpolator.getBlockLuma(references[list][mBlock.refIdx1[list]], mb, 0, (mbX << 6) + mvX1, (mbY << 6)
                    + mvY1, 8, 16);
            r1 = mBlock.refIdx1[list];
        }
        int[] v1 = { mvX1, mvY1, r1 };

        if (p1.usesList(list)) {

            int mvpX2 = calcMVPrediction8x16Right(v1, s.mvTop[list][(mbX << 2) + 2], s.mvTop[list][(mbX << 2) + 4],
                    s.mvTop[list][(mbX << 2) + 1], true, topAvailable, trAvailable, topAvailable, mBlock.refIdx2[list],
                    0);
            int mvpY2 = calcMVPrediction8x16Right(v1, s.mvTop[list][(mbX << 2) + 2], s.mvTop[list][(mbX << 2) + 4],
                    s.mvTop[list][(mbX << 2) + 1], true, topAvailable, trAvailable, topAvailable, mBlock.refIdx2[list],
                    1);

            mvX2 = mBlock.mvdX2[list] + mvpX2;
            mvY2 = mBlock.mvdY2[list] + mvpY2;

            debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mBlock.mvdX2 + ", " + mBlock.mvdY2 + "), MV: ("
                    + mvX2 + "," + mvY2 + "," + mBlock.refIdx2[list] + ")");

            BlockInterpolator.getBlockLuma(references[list][mBlock.refIdx2[list]], mb, 8, (mbX << 6) + 32 + mvX2,
                    (mbY << 6) + mvY2, 8, 16);
            r2 = mBlock.refIdx2[list];
        }
        int[] v2 = { mvX2, mvY2, r2 };

        copyVect(s.mvTopLeft[list], s.mvTop[list][xx + 3]);
        saveVect(s.mvTop[list], xx, xx + 2, mvX1, mvY1, r1);
        saveVect(s.mvTop[list], xx + 2, xx + 4, mvX2, mvY2, r2);
        saveVect(s.mvLeft[list], 0, 4, mvX2, mvY2, r2);

        x[list] = new int[][] { v1, v1, v2, v2, v1, v1, v2, v2, v1, v1, v2, v2, v1, v1, v2, v2 };
    }

    private void readPredInter8x16(MBlockP8x16 mBlock, int mbX, boolean leftAvailable, boolean topAvailable, int list,
            PartPred p0, PartPred p1) {
        int blk8x8X = (mbX << 1);

        if (p0.usesList(list)) {
            mBlock.mvdX1[list] = parser.readMVD(0, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);
            mBlock.mvdY1[list] = parser.readMVD(1, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);

        }

        if (p1.usesList(list)) {
            mBlock.mvdX2[list] = parser.readMVD(0, true, topAvailable, MBType.P_8x16, s.topMBType[mbX], p0,
                    s.predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list);
            mBlock.mvdY2[list] = parser.readMVD(1, true, topAvailable, MBType.P_8x16, s.topMBType[mbX], p0,
                    s.predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list);

        }
    }

    private void predictInter16x8(MBlockP16x8 mBlock, Picture8Bit mb, Picture8Bit[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int xx, int[][][] x,
            PartPred p0, PartPred p1, int list) {

        int mvX1 = 0, mvY1 = 0, mvX2 = 0, mvY2 = 0, r1 = -1, r2 = -1;
        if (p0.usesList(list)) {

            int mvpX1 = calcMVPrediction16x8Top(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 4], s.mvTopLeft[list], leftAvailable, topAvailable, trAvailable,
                    tlAvailable, mBlock.refIdx1[list], 0);
            int mvpY1 = calcMVPrediction16x8Top(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 4], s.mvTopLeft[list], leftAvailable, topAvailable, trAvailable,
                    tlAvailable, mBlock.refIdx1[list], 1);

            mvX1 = mBlock.mvdX1[list] + mvpX1;
            mvY1 = mBlock.mvdY1[list] + mvpY1;

            debugPrint("MVP: (" + mvpX1 + ", " + mvpY1 + "), MVD: (" + mBlock.mvdX1 + ", " + mBlock.mvdY1 + "), MV: ("
                    + mvX1 + "," + mvY1 + "," + mBlock.refIdx1[list] + ")");

            BlockInterpolator.getBlockLuma(references[list][mBlock.refIdx1[list]], mb, 0, (mbX << 6) + mvX1, (mbY << 6)
                    + mvY1, 16, 8);
            r1 = mBlock.refIdx1[list];
        }
        int[] v1 = { mvX1, mvY1, r1 };

        if (p1.usesList(list)) {
            int mvpX2 = calcMVPrediction16x8Bottom(s.mvLeft[list][2], v1, null, s.mvLeft[list][1], leftAvailable, true,
                    false, leftAvailable, mBlock.refIdx2[list], 0);
            int mvpY2 = calcMVPrediction16x8Bottom(s.mvLeft[list][2], v1, null, s.mvLeft[list][1], leftAvailable, true,
                    false, leftAvailable, mBlock.refIdx2[list], 1);

            mvX2 = mBlock.mvdX2[list] + mvpX2;
            mvY2 = mBlock.mvdY2[list] + mvpY2;

            debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mBlock.mvdX2 + ", " + mBlock.mvdY2 + "), MV: ("
                    + mvX2 + "," + mvY2 + "," + mBlock.refIdx2[list] + ")");

            BlockInterpolator.getBlockLuma(references[list][mBlock.refIdx2[list]], mb, 128, (mbX << 6) + mvX2,
                    (mbY << 6) + 32 + mvY2, 16, 8);
            r2 = mBlock.refIdx2[list];
        }
        int[] v2 = { mvX2, mvY2, r2 };

        copyVect(s.mvTopLeft[list], s.mvTop[list][xx + 3]);
        saveVect(s.mvLeft[list], 0, 2, mvX1, mvY1, r1);
        saveVect(s.mvLeft[list], 2, 4, mvX2, mvY2, r2);
        saveVect(s.mvTop[list], xx, xx + 4, mvX2, mvY2, r2);

        x[list] = new int[][] { v1, v1, v1, v1, v1, v1, v1, v1, v2, v2, v2, v2, v2, v2, v2, v2 };
    }

    private void readPredictionInter16x8(MBlockP16x8 mBlock, int mbX, boolean leftAvailable, boolean topAvailable,
            PartPred p0, PartPred p1, int list) {
        int blk8x8X = mbX << 1;
        if (p0.usesList(list)) {

            mBlock.mvdX1[list] = parser.readMVD(0, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);
            mBlock.mvdY1[list] = parser.readMVD(1, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);
        }

        if (p1.usesList(list)) {
            mBlock.mvdX2[list] = parser.readMVD(0, leftAvailable, true, s.leftMBType, MBType.P_16x8, s.predModeLeft[1],
                    p0, p1, mbX, 0, 2, 4, 2, list);
            mBlock.mvdY2[list] = parser.readMVD(1, leftAvailable, true, s.leftMBType, MBType.P_16x8, s.predModeLeft[1],
                    p0, p1, mbX, 0, 2, 4, 2, list);
        }
    }

    public void decode16x8(Picture8Bit mb, Frame[][] refs, int mbIdx, MBType prevMbType, PartPred p0, PartPred p1,
            MBType curMBType) {

        MBlockP16x8 mBlock = new MBlockP16x8();

        readInter16x8(mbIdx, prevMbType, p0, p1, curMBType, mBlock);

        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);
        int address = mapper.getAddress(mbIdx);
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

        s.predModeLeft[0] = p0;
        s.predModeLeft[1] = s.predModeTop[mbX << 1] = s.predModeTop[(mbX << 1) + 1] = p1;

        PartPred[] partPreds = new PartPred[] { p0, p0, p1, p1 };
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 1, mb, partPreds);
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 2, mb, partPreds);

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, x, partPreds, mapper.getAddress(mbIdx),
                prevMbType, curMBType);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = s.topMBType[mbX] = s.leftMBType = curMBType;
    }

    private void readInter16x8(int mbIdx, MBType prevMbType, PartPred p0, PartPred p1, MBType curMBType,
            MBlockP16x8 mBlock) {
        mBlock.refIdx1 = new int[] { 0, 0 };
        mBlock.refIdx2 = new int[] { 0, 0 };
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);

        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && s.numRef[list] > 1)
                mBlock.refIdx1[list] = parser.readRefIdx(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                        s.predModeLeft[0], s.predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 2, list);
            if (p1.usesList(list) && s.numRef[list] > 1)
                mBlock.refIdx2[list] = parser.readRefIdx(leftAvailable, true, s.leftMBType, curMBType,
                        s.predModeLeft[1], p0, p1, mbX, 0, 2, 4, 2, list);
        }

        for (int list = 0; list < 2; list++) {
            readPredictionInter16x8(mBlock, mbX, leftAvailable, topAvailable, p0, p1, list);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY, prevMbType, curMBType);
    }

    public void decode8x16(Picture8Bit mb, Frame[][] refs, int mbIdx, MBType prevMbType, PartPred p0, PartPred p1,
            MBType curMBType) {

        MBlockP8x16 mBlock = new MBlockP8x16();

        readIntra8x16(mbIdx, prevMbType, p0, p1, curMBType, mBlock);

        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        boolean topLeftAvailable = mapper.topLeftAvailable(mbIdx);
        boolean topRightAvailable = mapper.topRightAvailable(mbIdx);
        int address = mapper.getAddress(mbIdx);

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

        s.predModeTop[mbX << 1] = p0;
        s.predModeTop[(mbX << 1) + 1] = s.predModeLeft[0] = s.predModeLeft[1] = p1;

        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, x, predType, mapper.getAddress(mbIdx),
                prevMbType, curMBType);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
                mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = s.topMBType[mbX] = s.leftMBType = curMBType;
    }

    private void readIntra8x16(int mbIdx, MBType prevMbType, PartPred p0, PartPred p1, MBType curMBType,
            MBlockP8x16 mBlock) {
        int mbX = mapper.getMbX(mbIdx);
        int mbY = mapper.getMbY(mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mbIdx);
        boolean topAvailable = mapper.topAvailable(mbIdx);
        mBlock.refIdx1 = new int[] { 0, 0 };
        mBlock.refIdx2 = new int[] { 0, 0 };
        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && s.numRef[list] > 1)
                mBlock.refIdx1[list] = parser.readRefIdx(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                        s.predModeLeft[0], s.predModeTop[mbX << 1], p0, mbX, 0, 0, 2, 4, list);
            if (p1.usesList(list) && s.numRef[list] > 1)
                mBlock.refIdx2[list] = parser.readRefIdx(true, topAvailable, curMBType, s.topMBType[mbX], p0,
                        s.predModeTop[(mbX << 1) + 1], p1, mbX, 2, 0, 2, 4, list);
        }

        for (int list = 0; list < 2; list++) {
            readPredInter8x16(mBlock, mbX, leftAvailable, topAvailable, list, p0, p1);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY, prevMbType, curMBType);
    }

    void predictInter16x16(MBlockP16x16 mBlock, Picture8Bit mb, Picture8Bit[][] references, int mbX, int mbY,
            boolean leftAvailable, boolean topAvailable, boolean tlAvailable, boolean trAvailable, int[][][] x, int xx,
            int list, PartPred curPred) {

        int mvX = 0, mvY = 0, r = -1;
        if (curPred.usesList(list)) {
            int mvpX = calcMVPredictionMedian(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 4], s.mvTopLeft[list], leftAvailable, topAvailable, trAvailable,
                    tlAvailable, mBlock.refIdx[list], 0);
            int mvpY = calcMVPredictionMedian(s.mvLeft[list][0], s.mvTop[list][mbX << 2],
                    s.mvTop[list][(mbX << 2) + 4], s.mvTopLeft[list], leftAvailable, topAvailable, trAvailable,
                    tlAvailable, mBlock.refIdx[list], 1);

            mvX = mBlock.mvdX[list] + mvpX;
            mvY = mBlock.mvdY[list] + mvpY;

            debugPrint("MVP: (" + mvpX + ", " + mvpY + "), MVD: (" + mBlock.mvdX + ", " + mBlock.mvdY + "), MV: ("
                    + mvX + "," + mvY + "," + mBlock.refIdx[list] + ")");
            r = mBlock.refIdx[list];

            BlockInterpolator.getBlockLuma(references[list][r], mb, 0, (mbX << 6) + mvX, (mbY << 6) + mvY, 16, 16);
        }

        copyVect(s.mvTopLeft[list], s.mvTop[list][xx + 3]);
        saveVect(s.mvTop[list], xx, xx + 4, mvX, mvY, r);
        saveVect(s.mvLeft[list], 0, 4, mvX, mvY, r);

        int[] v = { mvX, mvY, r };
        x[list] = new int[][] { v, v, v, v, v, v, v, v, v, v, v, v, v, v, v, v };
    }

    private void readPredictionInter16x16(MBlockP16x16 mBlock, int mbX, boolean leftAvailable, boolean topAvailable,
            int list, PartPred curPred) {
        int blk8x8X = (mbX << 1);

        if (curPred.usesList(list)) {
            mBlock.mvdX[list] = parser.readMVD(0, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list);
            mBlock.mvdY[list] = parser.readMVD(1, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list);
        }
    }

    private void residualInter(MBlock mBlock, Frame[][] refs, boolean leftAvailable, boolean topAvailable, int mbX,
            int mbY, int[][][] x, PartPred[] pp, int mbAddr, MBType prevMbType, MBType curMbType) {

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY, curMbType, mBlock.transform8x8Used, s.tf8x8Left,
                s.tf8x8Top[mbX]);

        saveMvs(di, x, mbX, mbY);

        if (s.chromaFormat != MONO) {
            int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
            int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

            decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, mBlock.cbpChroma(), qp1, qp2, curMbType);

            di.mbQps[1][mbAddr] = qp1;
            di.mbQps[2][mbAddr] = qp2;
        }

        s.topCBPLuma[mbX] = s.leftCBPLuma = mBlock.cbpLuma();
        s.topCBPChroma[mbX] = s.leftCBPChroma = mBlock.cbpChroma();
        s.tf8x8Left = s.tf8x8Top[mbX] = mBlock.transform8x8Used;
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
    }

    private void readResidualInter(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            MBType prevMbType, MBType curMbType) {
        mBlock.cbp = parser.readCodedBlockPatternInter(leftAvailable, topAvailable, s.leftCBPLuma
                | (s.leftCBPChroma << 4), s.topCBPLuma[mbX] | (s.topCBPChroma[mbX] << 4), s.leftMBType,
                s.topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (mBlock.cbpLuma() != 0 && s.transform8x8) {
            mBlock.transform8x8Used = parser.readTransform8x8Flag(leftAvailable, topAvailable, s.leftMBType,
                    s.topMBType[mbX], s.tf8x8Left, s.tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = parser.readMBQpDelta(prevMbType);
        }

        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY, curMbType, mBlock.transform8x8Used);

        if (s.chromaFormat != MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mBlock.cbpChroma(), curMbType);
        }
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
