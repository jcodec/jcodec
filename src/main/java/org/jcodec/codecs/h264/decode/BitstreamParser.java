package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.bPartPredModes;
import static org.jcodec.codecs.h264.H264Const.bSubMbTypes;
import static org.jcodec.codecs.h264.H264Const.identityMapping16;
import static org.jcodec.codecs.h264.H264Const.last_sig_coeff_map_8x8;
import static org.jcodec.codecs.h264.H264Const.sig_coeff_map_8x8;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.decode.CAVLCReader.moreRBSPData;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readTE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUE;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint;
import static org.jcodec.codecs.h264.io.model.MBType.B_8x8;
import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.P_8x8;
import static org.jcodec.common.model.ColorSpace.MONO;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.CABAC.BlockType;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;

/**
 * Contains methods for reading high-level symbols out of H.264 bitstream
 * 
 * @author The JCodec Project
 */
public class BitstreamParser {

    private PictureParameterSet activePps;
    private CABAC cabac;
    private MDecoder mDecoder;
    private DecoderState s;
    private CAVLC[] cavlc;
    private BitReader reader;
    private DeblockerInput di;
    private Mapper mapper;
    private SliceHeader sh;

    public BitstreamParser(PictureParameterSet activePps, CABAC cabac, CAVLC[] cavlc, MDecoder mDecoder,
            BitReader reader, DeblockerInput di, Mapper mapper, SliceHeader sh, DecoderState sharedContext) {
        this.activePps = activePps;
        this.cabac = cabac;
        this.mDecoder = mDecoder;
        this.s = sharedContext;
        this.cavlc = cavlc;
        this.reader = reader;
        this.mapper = mapper;
        this.sh = sh;
        this.di = di;
    }

    int readMBQpDelta(MBType prevMbType) {
        int mbQPDelta;
        if (!activePps.entropy_coding_mode_flag) {
            mbQPDelta = readSE(reader, "mb_qp_delta");
        } else {
            mbQPDelta = cabac.readMBQpDelta(mDecoder, prevMbType);
        }
        return mbQPDelta;
    }

    int readChromaPredMode(int mbX, boolean leftAvailable, boolean topAvailable) {
        int chromaPredictionMode;
        if (!activePps.entropy_coding_mode_flag) {
            chromaPredictionMode = readUE(reader, "MBP: intra_chroma_pred_mode");
        } else {
            chromaPredictionMode = cabac.readIntraChromaPredMode(mDecoder, mbX, s.leftMBType, s.topMBType[mbX],
                    leftAvailable, topAvailable);
        }
        return chromaPredictionMode;
    }

    boolean readTransform8x8Flag(boolean leftAvailable, boolean topAvailable, MBType leftType, MBType topType,
            boolean is8x8Left, boolean is8x8Top) {
        if (!activePps.entropy_coding_mode_flag)
            return readBool(reader, "transform_size_8x8_flag");
        else
            return cabac.readTransform8x8Flag(mDecoder, leftAvailable, topAvailable, leftType, topType, is8x8Left,
                    is8x8Top);
    }

    protected int readCodedBlockPatternIntra(boolean leftAvailable, boolean topAvailable, int leftCBP, int topCBP,
            MBType leftMB, MBType topMB) {

        if (!activePps.entropy_coding_mode_flag)
            return H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR[readUE(reader, "coded_block_pattern")];
        else
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
    }

    protected int readCodedBlockPatternInter(boolean leftAvailable, boolean topAvailable, int leftCBP, int topCBP,
            MBType leftMB, MBType topMB) {
        if (!activePps.entropy_coding_mode_flag)
            return H264Const.CODED_BLOCK_PATTERN_INTER_COLOR[readUE(reader, "coded_block_pattern")];
        else
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
    }

    int readRefIdx(boolean leftAvailable, boolean topAvailable, MBType leftType, MBType topType, PartPred leftPred,
            PartPred topPred, PartPred curPred, int mbX, int partX, int partY, int partW, int partH, int list) {
        if (!activePps.entropy_coding_mode_flag)
            return readTE(reader, s.numRef[list] - 1);
        else
            return cabac.readRefIdx(mDecoder, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                    curPred, mbX, partX, partY, partW, partH, list);
    }

    int readMVD(int comp, boolean leftAvailable, boolean topAvailable, MBType leftType, MBType topType,
            PartPred leftPred, PartPred topPred, PartPred curPred, int mbX, int partX, int partY, int partW, int partH,
            int list) {
        if (!activePps.entropy_coding_mode_flag)
            return readSE(reader, "mvd_l0_x");
        else
            return cabac.readMVD(mDecoder, comp, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                    curPred, mbX, partX, partY, partW, partH, list);
    }

    int readPredictionI4x4Block(boolean leftAvailable, boolean topAvailable, MBType leftMBType, MBType topMBType,
            int blkX, int blkY, int mbX) {
        int mode = 2;
        if ((leftAvailable || blkX > 0) && (topAvailable || blkY > 0)) {
            int predModeB = topMBType == MBType.I_NxN || blkY > 0 ? s.i4x4PredTop[(mbX << 2) + blkX] : 2;
            int predModeA = leftMBType == MBType.I_NxN || blkX > 0 ? s.i4x4PredLeft[blkY] : 2;
            mode = Math.min(predModeB, predModeA);
        }
        if (!prev4x4PredMode()) {
            int rem_intra4x4_pred_mode = rem4x4PredMode();
            mode = rem_intra4x4_pred_mode + (rem_intra4x4_pred_mode < mode ? 0 : 1);
        }
        s.i4x4PredTop[(mbX << 2) + blkX] = s.i4x4PredLeft[blkY] = mode;
        return mode;
    }

    int rem4x4PredMode() {
        if (!activePps.entropy_coding_mode_flag)
            return readNBit(reader, 3, "MB: rem_intra4x4_pred_mode");
        else
            return cabac.rem4x4PredMode(mDecoder);
    }

    boolean prev4x4PredMode() {
        if (!activePps.entropy_coding_mode_flag)
            return readBool(reader, "MBP: prev_intra4x4_pred_mode_flag");
        else
            return cabac.prev4x4PredModeFlag(mDecoder);
    }

    void read16x16DC(boolean leftAvailable, boolean topAvailable, int mbX, int[] dc) {
        if (!activePps.entropy_coding_mode_flag)
            cavlc[0].readLumaDCBlock(reader, dc, mbX, leftAvailable, s.leftMBType, topAvailable, s.topMBType[mbX],
                    CoeffTransformer.zigzag4x4);
        else {
            if (cabac.readCodedBlockFlagLumaDC(mDecoder, mbX, s.leftMBType, s.topMBType[mbX], leftAvailable,
                    topAvailable, MBType.I_16x16) == 1)
                cabac.readCoeffs(mDecoder, BlockType.LUMA_16_DC, dc, 0, 16, CoeffTransformer.zigzag4x4,
                        identityMapping16, identityMapping16);
        }
    }

    void read16x16AC(boolean leftAvailable, boolean topAvailable, int mbX, int cbpLuma, int[] ac, int blkOffLeft,
            int blkOffTop, int blkX, int blkY) {
        if (!activePps.entropy_coding_mode_flag) {
            di.nCoeff[blkY][blkX] = cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    blkOffLeft == 0 ? s.leftMBType : I_16x16, blkOffTop != 0 || topAvailable,
                    blkOffTop == 0 ? s.topMBType[mbX] : I_16x16, 1, 15, CoeffTransformer.zigzag4x4);
        } else {
            if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_15_AC, blkX, blkOffTop, 0, s.leftMBType,
                    s.topMBType[mbX], leftAvailable, topAvailable, s.leftCBPLuma, s.topCBPLuma[mbX], cbpLuma,
                    MBType.I_16x16) == 1)
                di.nCoeff[blkY][blkX] = cabac.readCoeffs(mDecoder, BlockType.LUMA_15_AC, ac, 1, 15,
                        CoeffTransformer.zigzag4x4, identityMapping16, identityMapping16);
        }
    }

    void readResidualAC(boolean leftAvailable, boolean topAvailable, int mbX, MBType curMbType, int cbpLuma,
            int blkOffLeft, int blkOffTop, int blkX, int blkY, int[] ac) {
        if (!activePps.entropy_coding_mode_flag) {
            di.nCoeff[blkY][blkX] = cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    blkOffLeft == 0 ? s.leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                    blkOffTop == 0 ? s.topMBType[mbX] : curMbType, 0, 16, CoeffTransformer.zigzag4x4);
        } else {
            if (cabac
                    .readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_16, blkX, blkOffTop, 0, s.leftMBType,
                            s.topMBType[mbX], leftAvailable, topAvailable, s.leftCBPLuma, s.topCBPLuma[mbX], cbpLuma,
                            curMbType) == 1)
                di.nCoeff[blkY][blkX] = cabac.readCoeffs(mDecoder, BlockType.LUMA_16, ac, 0, 16,
                        CoeffTransformer.zigzag4x4, identityMapping16, identityMapping16);
        }
    }

    public void setZeroCoeff(int comp, int blkX, int blkOffTop) {
        cavlc[comp].setZeroCoeff(blkX, blkOffTop);
    }

    public void savePrevCBP(int codedBlockPattern) {
        if (activePps.entropy_coding_mode_flag)
            cabac.setPrevCBP(codedBlockPattern);
    }

    public int readLumaAC(boolean leftAvailable, boolean topAvailable, int mbX, MBType curMbType, int blkX, int j,
            int[] ac16, int blkOffLeft, int blkOffTop) {
        return cavlc[0].readACBlock(reader, ac16, blkX + (j & 1), blkOffTop, blkOffLeft != 0 || leftAvailable,
                blkOffLeft == 0 ? s.leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                blkOffTop == 0 ? s.topMBType[mbX] : curMbType, 0, 16, H264Const.identityMapping16);
    }

    public void readLumaAC8x8(int blkX, int blkY, int[] ac) {
        int readCoeffs = cabac.readCoeffs(mDecoder, BlockType.LUMA_64, ac, 0, 64, CoeffTransformer.zigzag8x8,
                sig_coeff_map_8x8, last_sig_coeff_map_8x8);
        di.nCoeff[blkY][blkX] = di.nCoeff[blkY][blkX + 1] = di.nCoeff[blkY + 1][blkX] = di.nCoeff[blkY + 1][blkX + 1] = readCoeffs;
        cabac.setCodedBlock(blkX, blkY);
        cabac.setCodedBlock(blkX + 1, blkY);
        cabac.setCodedBlock(blkX, blkY + 1);
        cabac.setCodedBlock(blkX + 1, blkY + 1);
    }

    public int readSubMBTypeP() {
        if (!activePps.entropy_coding_mode_flag)
            return readUE(reader, "SUB: sub_mb_type");
        else
            return cabac.readSubMbTypeP(mDecoder);
    }

    public int readSubMBTypeB() {
        if (!activePps.entropy_coding_mode_flag)
            return readUE(reader, "SUB: sub_mb_type");
        else
            return cabac.readSubMbTypeB(mDecoder);
    }

    public void readChromaDC(int mbX, boolean leftAvailable, boolean topAvailable, int[] dc, int comp, MBType curMbType) {
        if (!activePps.entropy_coding_mode_flag)
            cavlc[comp].readChromaDCBlock(reader, dc, leftAvailable, topAvailable);
        else {
            if (cabac.readCodedBlockFlagChromaDC(mDecoder, mbX, comp, s.leftMBType, s.topMBType[mbX], leftAvailable,
                    topAvailable, s.leftCBPChroma, s.topCBPChroma[mbX], curMbType) == 1)
                cabac.readCoeffs(mDecoder, BlockType.CHROMA_DC, dc, 0, 4, identityMapping16, identityMapping16,
                        identityMapping16);
        }
    }

    public void readChromaAC(boolean leftAvailable, boolean topAvailable, int mbX, int comp, MBType curMbType,
            int[] ac, int blkOffLeft, int blkOffTop, int blkX) {
        if (!activePps.entropy_coding_mode_flag)
            cavlc[comp].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    blkOffLeft == 0 ? s.leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                    blkOffTop == 0 ? s.topMBType[mbX] : curMbType, 1, 15, CoeffTransformer.zigzag4x4);
        else {
            if (cabac.readCodedBlockFlagChromaAC(mDecoder, blkX, blkOffTop, comp, s.leftMBType, s.topMBType[mbX],
                    leftAvailable, topAvailable, s.leftCBPChroma, s.topCBPChroma[mbX], curMbType) == 1)
                cabac.readCoeffs(mDecoder, BlockType.CHROMA_AC, ac, 1, 15, CoeffTransformer.zigzag4x4,
                        identityMapping16, identityMapping16);
        }
    }

    public int decodeMBTypeI(int mbIdx, boolean leftAvailable, boolean topAvailable, MBType leftMBType, MBType topMBType) {
        int mbType;
        if (!activePps.entropy_coding_mode_flag)
            mbType = readUE(reader, "MB: mb_type");
        else
            mbType = cabac.readMBTypeI(mDecoder, leftMBType, topMBType, leftAvailable, topAvailable);
        return mbType;
    }

    public int readMBTypeP() {
        int mbType;
        if (!activePps.entropy_coding_mode_flag)
            mbType = readUE(reader, "MB: mb_type");
        else
            mbType = cabac.readMBTypeP(mDecoder);
        return mbType;
    }

    public int readMBTypeB(int mbIdx, boolean leftAvailable, boolean topAvailable, MBType leftMBType, MBType topMBType) {
        int mbType;
        if (!activePps.entropy_coding_mode_flag)
            mbType = readUE(reader, "MB: mb_type");
        else
            mbType = cabac.readMBTypeB(mDecoder, leftMBType, topMBType, leftAvailable, topAvailable);
        return mbType;
    }

    public boolean readMBSkipFlag(SliceType slType, boolean leftAvailable, boolean topAvailable, int mbX) {
        return cabac.readMBSkipFlag(mDecoder, slType, leftAvailable, topAvailable, mbX);
    }

    public void readIntra16x16(int mbType, MBlock mBlock) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        mBlock.cbp((mbType / 12) * 15, (mbType / 4) % 3);
        mBlock.luma16x16Mode = mbType % 4;
        mBlock.chromaPredictionMode = readChromaPredMode(mbX, leftAvailable, topAvailable);
        mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        read16x16DC(leftAvailable, topAvailable, mbX, mBlock.dc);
        for (int i = 0; i < 16; i++) {
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((mBlock.cbpLuma() & (1 << (i >> 2))) != 0) {
                read16x16AC(leftAvailable, topAvailable, mbX, mBlock.cbpLuma(), mBlock.ac[0][i], blkOffLeft, blkOffTop,
                        blkX, blkY);
            } else {
                if (!sh.pps.entropy_coding_mode_flag)
                    setZeroCoeff(0, blkX, blkOffTop);
            }
        }

        if (s.chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX);
        }
    }

    public void readMBlockBDirect(MBlock mBlock) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean lAvb = mapper.leftAvailable(mBlock.mbIdx);
        boolean tAvb = mapper.topAvailable(mBlock.mbIdx);
        mBlock.cbp = readCodedBlockPatternInter(lAvb, tAvb, s.leftCBPLuma | (s.leftCBPChroma << 4), s.topCBPLuma[mbX]
                | (s.topCBPChroma[mbX] << 4), s.leftMBType, s.topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (s.transform8x8 && mBlock.cbpLuma() != 0 && sh.sps.direct_8x8_inference_flag) {
            mBlock.transform8x8Used = readTransform8x8Flag(lAvb, tAvb, s.leftMBType, s.topMBType[mbX], s.tf8x8Left,
                    s.tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        }
        readResidualLuma(mBlock, lAvb, tAvb, mbX, mbY);
        readChromaResidual(mBlock, lAvb, tAvb, mbX);
    }

    public void readInter16x16(PartPred p0, MBlock mBlock) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        mBlock.pb16x16.refIdx = new int[] { 0, 0 };
        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && s.numRef[list] > 1)
                mBlock.pb16x16.refIdx[list] = readRefIdx(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                        s.predModeLeft[0], s.predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 4, list);
        }
        for (int list = 0; list < 2; list++) {
            readPredictionInter16x16(mBlock, mbX, leftAvailable, topAvailable, list, p0);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY);
    }

    private void readPredInter8x16(MBlock mBlock, int mbX, boolean leftAvailable, boolean topAvailable, int list,
            PartPred p0, PartPred p1) {
        int blk8x8X = (mbX << 1);

        if (p0.usesList(list)) {
            mBlock.pb168x168.mvdX1[list] = readMVD(0, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);
            mBlock.pb168x168.mvdY1[list] = readMVD(1, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);

        }

        if (p1.usesList(list)) {
            mBlock.pb168x168.mvdX2[list] = readMVD(0, true, topAvailable, MBType.P_8x16, s.topMBType[mbX], p0,
                    s.predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list);
            mBlock.pb168x168.mvdY2[list] = readMVD(1, true, topAvailable, MBType.P_8x16, s.topMBType[mbX], p0,
                    s.predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list);

        }
    }

    private void readPredictionInter16x8(MBlock mBlock, int mbX, boolean leftAvailable, boolean topAvailable,
            PartPred p0, PartPred p1, int list) {
        int blk8x8X = mbX << 1;
        if (p0.usesList(list)) {

            mBlock.pb168x168.mvdX1[list] = readMVD(0, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);
            mBlock.pb168x168.mvdY1[list] = readMVD(1, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);
        }

        if (p1.usesList(list)) {
            mBlock.pb168x168.mvdX2[list] = readMVD(0, leftAvailable, true, s.leftMBType, MBType.P_16x8,
                    s.predModeLeft[1], p0, p1, mbX, 0, 2, 4, 2, list);
            mBlock.pb168x168.mvdY2[list] = readMVD(1, leftAvailable, true, s.leftMBType, MBType.P_16x8,
                    s.predModeLeft[1], p0, p1, mbX, 0, 2, 4, 2, list);
        }
    }

    public void readInter16x8(PartPred p0, PartPred p1, MBlock mBlock) {
        mBlock.pb168x168.refIdx1 = new int[] { 0, 0 };
        mBlock.pb168x168.refIdx2 = new int[] { 0, 0 };
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && s.numRef[list] > 1)
                mBlock.pb168x168.refIdx1[list] = readRefIdx(leftAvailable, topAvailable, s.leftMBType,
                        s.topMBType[mbX], s.predModeLeft[0], s.predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 2, list);
            if (p1.usesList(list) && s.numRef[list] > 1)
                mBlock.pb168x168.refIdx2[list] = readRefIdx(leftAvailable, true, s.leftMBType, mBlock.curMbType,
                        s.predModeLeft[1], p0, p1, mbX, 0, 2, 4, 2, list);
        }

        for (int list = 0; list < 2; list++) {
            readPredictionInter16x8(mBlock, mbX, leftAvailable, topAvailable, p0, p1, list);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY);
    }

    public void readIntra8x16(PartPred p0, PartPred p1, MBlock mBlock) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        mBlock.pb168x168.refIdx1 = new int[] { 0, 0 };
        mBlock.pb168x168.refIdx2 = new int[] { 0, 0 };
        for (int list = 0; list < 2; list++) {
            if (p0.usesList(list) && s.numRef[list] > 1)
                mBlock.pb168x168.refIdx1[list] = readRefIdx(leftAvailable, topAvailable, s.leftMBType,
                        s.topMBType[mbX], s.predModeLeft[0], s.predModeTop[mbX << 1], p0, mbX, 0, 0, 2, 4, list);
            if (p1.usesList(list) && s.numRef[list] > 1)
                mBlock.pb168x168.refIdx2[list] = readRefIdx(true, topAvailable, mBlock.curMbType, s.topMBType[mbX], p0,
                        s.predModeTop[(mbX << 1) + 1], p1, mbX, 2, 0, 2, 4, list);
        }

        for (int list = 0; list < 2; list++) {
            readPredInter8x16(mBlock, mbX, leftAvailable, topAvailable, list, p0, p1);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY);
    }

    private void readPredictionInter16x16(MBlock mBlock, int mbX, boolean leftAvailable, boolean topAvailable,
            int list, PartPred curPred) {
        int blk8x8X = (mbX << 1);

        if (curPred.usesList(list)) {
            mBlock.pb16x16.mvdX[list] = readMVD(0, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list);
            mBlock.pb16x16.mvdY[list] = readMVD(1, leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.predModeLeft[0], s.predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list);
        }
    }

    private void readResidualInter(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY) {
        mBlock.cbp = readCodedBlockPatternInter(leftAvailable, topAvailable, s.leftCBPLuma | (s.leftCBPChroma << 4),
                s.topCBPLuma[mbX] | (s.topCBPChroma[mbX] << 4), s.leftMBType, s.topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (mBlock.cbpLuma() != 0 && s.transform8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.tf8x8Left, s.tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        }

        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);

        if (s.chromaFormat != MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX);
        }
    }

    public void readMBlock8x8(MBlock mBlock) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        boolean noSubMBLessThen8x8;
        if (mBlock.curMbType == P_8x8 || mBlock.curMbType == MBType.P_8x8ref0) {
            readPrediction8x8P(mBlock, mbX, leftAvailable, topAvailable);

            noSubMBLessThen8x8 = mBlock.pb8x8.subMbTypes[0] == 0 && mBlock.pb8x8.subMbTypes[1] == 0
                    && mBlock.pb8x8.subMbTypes[2] == 0 && mBlock.pb8x8.subMbTypes[3] == 0;
        } else {
            readPrediction8x8B(mBlock, mbX, leftAvailable, topAvailable);
            noSubMBLessThen8x8 = bSubMbTypes[mBlock.pb8x8.subMbTypes[0]] == 0
                    && bSubMbTypes[mBlock.pb8x8.subMbTypes[1]] == 0 && bSubMbTypes[mBlock.pb8x8.subMbTypes[2]] == 0
                    && bSubMbTypes[mBlock.pb8x8.subMbTypes[3]] == 0;
        }

        mBlock.cbp = readCodedBlockPatternInter(leftAvailable, topAvailable, s.leftCBPLuma | (s.leftCBPChroma << 4),
                s.topCBPLuma[mbX] | (s.topCBPChroma[mbX] << 4), s.leftMBType, s.topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (s.transform8x8 && mBlock.cbpLuma() != 0 && noSubMBLessThen8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.tf8x8Left, s.tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        }
        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);
        readChromaResidual(mBlock, leftAvailable, topAvailable, mbX);
    }

    private void readPrediction8x8P(MBlock mBlock, int mbX, boolean leftAvailable, boolean topAvailable) {
        mBlock.pb8x8.subMbTypes = new int[4];
        for (int i = 0; i < 4; i++) {
            mBlock.pb8x8.subMbTypes[i] = readSubMBTypeP();
        }

        mBlock.pb8x8.refIdx = new int[1][4];
        if (s.numRef[0] > 1 && mBlock.curMbType != MBType.P_8x8ref0) {
            mBlock.pb8x8.refIdx[0][0] = readRefIdx(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX], L0, L0,
                    L0, mbX, 0, 0, 2, 2, 0);
            mBlock.pb8x8.refIdx[0][1] = readRefIdx(true, topAvailable, P_8x8, s.topMBType[mbX], L0, L0, L0, mbX, 2, 0,
                    2, 2, 0);
            mBlock.pb8x8.refIdx[0][2] = readRefIdx(leftAvailable, true, s.leftMBType, P_8x8, L0, L0, L0, mbX, 0, 2, 2,
                    2, 0);
            mBlock.pb8x8.refIdx[0][3] = readRefIdx(true, true, P_8x8, P_8x8, L0, L0, L0, mbX, 2, 2, 2, 2, 0);
        }

        readSubMb8x8(mBlock, 0, topAvailable, leftAvailable, 0, 0, mbX, s.leftMBType, s.topMBType[mbX], P_8x8, L0, L0,
                L0, 0);
        readSubMb8x8(mBlock, 1, topAvailable, true, 2, 0, mbX, P_8x8, s.topMBType[mbX], P_8x8, L0, L0, L0, 0);
        readSubMb8x8(mBlock, 2, true, leftAvailable, 0, 2, mbX, s.leftMBType, P_8x8, P_8x8, L0, L0, L0, 0);
        readSubMb8x8(mBlock, 3, true, true, 2, 2, mbX, P_8x8, P_8x8, P_8x8, L0, L0, L0, 0);
    }

    private void readPrediction8x8B(MBlock mBlock, int mbX, boolean leftAvailable, boolean topAvailable) {
        mBlock.pb8x8.subMbTypes = new int[4];
        PartPred[] p = new PartPred[4];
        for (int i = 0; i < 4; i++) {
            mBlock.pb8x8.subMbTypes[i] = readSubMBTypeB();
            p[i] = bPartPredModes[mBlock.pb8x8.subMbTypes[i]];
        }

        mBlock.pb8x8.refIdx = new int[2][4];
        for (int list = 0; list < 2; list++) {
            if (s.numRef[list] <= 1)
                continue;
            if (p[0].usesList(list))
                mBlock.pb8x8.refIdx[list][0] = readRefIdx(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                        s.predModeLeft[0], s.predModeTop[mbX << 1], p[0], mbX, 0, 0, 2, 2, list);
            if (p[1].usesList(list))
                mBlock.pb8x8.refIdx[list][1] = readRefIdx(true, topAvailable, B_8x8, s.topMBType[mbX], p[0],
                        s.predModeTop[(mbX << 1) + 1], p[1], mbX, 2, 0, 2, 2, list);
            if (p[2].usesList(list))
                mBlock.pb8x8.refIdx[list][2] = readRefIdx(leftAvailable, true, s.leftMBType, B_8x8, s.predModeLeft[1],
                        p[0], p[2], mbX, 0, 2, 2, 2, list);
            if (p[3].usesList(list))
                mBlock.pb8x8.refIdx[list][3] = readRefIdx(true, true, B_8x8, B_8x8, p[2], p[1], p[3], mbX, 2, 2, 2, 2,
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

    private void readSubMb8x8(MBlock mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        switch (mBlock.pb8x8.subMbTypes[partNo]) {
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

    private void readSub8x8(MBlock mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, PartPred leftPred, PartPred topPred, PartPred partPred, int list) {
        mBlock.pb8x8.mvdX1[partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 2, list);
        mBlock.pb8x8.mvdY1[partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 2, list);
    }

    private void readSub8x4(MBlock mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        mBlock.pb8x8.mvdX1[partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 1, list);
        mBlock.pb8x8.mvdY1[partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 2, 1, list);

        mBlock.pb8x8.mvdX2[partNo] = readMVD(0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 2, 1, list);
        mBlock.pb8x8.mvdY2[partNo] = readMVD(1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 2, 1, list);
    }

    private void readSub4x8(MBlock mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        mBlock.pb8x8.mvdX1[partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 2, list);
        mBlock.pb8x8.mvdY1[partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 2, list);
        mBlock.pb8x8.mvdX2[partNo] = readMVD(0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 2, list);
        mBlock.pb8x8.mvdY2[partNo] = readMVD(1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 2, list);
    }

    private void readSub4x4(MBlock mBlock, int partNo, boolean tAvb, boolean lAvb, int blk8x8X, int blk8x8Y, int mbX,
            MBType leftMBType, MBType topMBType, MBType curMBType, PartPred leftPred, PartPred topPred,
            PartPred partPred, int list) {
        mBlock.pb8x8.mvdX1[partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 1, list);
        mBlock.pb8x8.mvdY1[partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred, mbX,
                blk8x8X, blk8x8Y, 1, 1, list);
        mBlock.pb8x8.mvdX2[partNo] = readMVD(0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 1, list);
        mBlock.pb8x8.mvdY2[partNo] = readMVD(1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y, 1, 1, list);
        mBlock.pb8x8.mvdX3[partNo] = readMVD(0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 1, 1, list);
        mBlock.pb8x8.mvdY3[partNo] = readMVD(1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred, mbX,
                blk8x8X, blk8x8Y + 1, 1, 1, list);
        mBlock.pb8x8.mvdX4[partNo] = readMVD(0, true, true, curMBType, curMBType, partPred, partPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y + 1, 1, 1, list);
        mBlock.pb8x8.mvdY4[partNo] = readMVD(1, true, true, curMBType, curMBType, partPred, partPred, partPred, mbX,
                blk8x8X + 1, blk8x8Y + 1, 1, 1, list);
    }

    public void readIntraNxN(MBlock mBlock) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        int mbY = mapper.getMbY(mBlock.mbIdx);
        boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        mBlock.transform8x8Used = false;
        if (s.transform8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, s.leftMBType, s.topMBType[mbX],
                    s.tf8x8Left, s.tf8x8Top[mbX]);
        }

        if (!mBlock.transform8x8Used) {
            for (int i = 0; i < 16; i++) {
                int blkX = H264Const.MB_BLK_OFF_LEFT[i];
                int blkY = H264Const.MB_BLK_OFF_TOP[i];
                mBlock.lumaModes[i] = readPredictionI4x4Block(leftAvailable, topAvailable, s.leftMBType,
                        s.topMBType[mbX], blkX, blkY, mbX);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                int blkX = (i & 1) << 1;
                int blkY = i & 2;
                mBlock.lumaModes[i] = readPredictionI4x4Block(leftAvailable, topAvailable, s.leftMBType,
                        s.topMBType[mbX], blkX, blkY, mbX);
                s.i4x4PredLeft[blkY + 1] = s.i4x4PredLeft[blkY];
                s.i4x4PredTop[(mbX << 2) + blkX + 1] = s.i4x4PredTop[(mbX << 2) + blkX];
            }
        }
        mBlock.chromaPredictionMode = readChromaPredMode(mbX, leftAvailable, topAvailable);

        mBlock.cbp = readCodedBlockPatternIntra(leftAvailable, topAvailable, s.leftCBPLuma | (s.leftCBPChroma << 4),
                s.topCBPLuma[mbX] | (s.topCBPChroma[mbX] << 4), s.leftMBType, s.topMBType[mbX]);

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        }
        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);
        if (s.chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX);
        }
    }

    public void readResidualLuma(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY) {
        if (!mBlock.transform8x8Used) {
            readLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);
        } else if (sh.pps.entropy_coding_mode_flag) {
            readLuma8x8CABAC(mBlock, mbX, mbY);
        } else {
            readLuma8x8CAVLC(mBlock, leftAvailable, topAvailable, mbX, mbY);
        }
    }

    private void readLuma(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY) {
        for (int i = 0; i < 16; i++) {
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((mBlock.cbpLuma() & (1 << (i >> 2))) == 0) {
                if (!sh.pps.entropy_coding_mode_flag)
                    setZeroCoeff(0, blkX, blkOffTop);
                continue;
            }

            readResidualAC(leftAvailable, topAvailable, mbX, mBlock.curMbType, mBlock.cbpLuma(), blkOffLeft, blkOffTop,
                    blkX, blkY, mBlock.ac[0][i]);
        }

        savePrevCBP(mBlock.cbp);
    }

    private void readLuma8x8CABAC(MBlock mBlock, int mbX, int mbY) {
        for (int i = 0; i < 4; i++) {
            int blkOffLeft = (i & 1) << 1;
            int blkOffTop = i & 2;
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                continue;
            }

            readLumaAC8x8(blkX, blkY, mBlock.ac[0][i]);
        }
        savePrevCBP(mBlock.cbp);
    }

    private void readLuma8x8CAVLC(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY) {
        for (int i = 0; i < 4; i++) {
            int blk8x8OffLeft = (i & 1) << 1;
            int blk8x8OffTop = i & 2;
            int blkX = (mbX << 2) + blk8x8OffLeft;
            int blkY = (mbY << 2) + blk8x8OffTop;

            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                setZeroCoeff(0, blkX, blk8x8OffTop);
                setZeroCoeff(0, blkX + 1, blk8x8OffTop);
                setZeroCoeff(0, blkX, blk8x8OffTop + 1);
                setZeroCoeff(0, blkX + 1, blk8x8OffTop + 1);
                continue;
            }
            int coeffs = 0;
            for (int j = 0; j < 4; j++) {
                int[] ac16 = new int[16];
                int blkOffLeft = blk8x8OffLeft + (j & 1);
                int blkOffTop = blk8x8OffTop + (j >> 1);
                coeffs += readLumaAC(leftAvailable, topAvailable, mbX, mBlock.curMbType, blkX, j, ac16, blkOffLeft,
                        blkOffTop);
                for (int k = 0; k < 16; k++)
                    mBlock.ac[0][i][CoeffTransformer.zigzag8x8[(k << 2) + j]] = ac16[k];
            }
            di.nCoeff[blkY][blkX] = di.nCoeff[blkY][blkX + 1] = di.nCoeff[blkY + 1][blkX] = di.nCoeff[blkY + 1][blkX + 1] = coeffs;
        }
    }

    public void readChromaResidual(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX) {
        if (mBlock.cbpChroma() != 0) {
            mBlock.dc1 = new int[(16 >> s.chromaFormat.compWidth[1]) >> s.chromaFormat.compHeight[1]];
            mBlock.dc2 = new int[(16 >> s.chromaFormat.compWidth[2]) >> s.chromaFormat.compHeight[2]];
            if ((mBlock.cbpChroma() & 3) > 0) {
                readChromaDC(mbX, leftAvailable, topAvailable, mBlock.dc1, 1, mBlock.curMbType);
                readChromaDC(mbX, leftAvailable, topAvailable, mBlock.dc2, 2, mBlock.curMbType);
            }
            readChromaAC(leftAvailable, topAvailable, mbX, mBlock.dc1, 1, mBlock.curMbType,
                    (mBlock.cbpChroma() & 2) > 0, mBlock.ac[1]);
            readChromaAC(leftAvailable, topAvailable, mbX, mBlock.dc2, 2, mBlock.curMbType,
                    (mBlock.cbpChroma() & 2) > 0, mBlock.ac[2]);
        } else if (!sh.pps.entropy_coding_mode_flag) {
            setZeroCoeff(1, mbX << 1, 0);
            setZeroCoeff(1, (mbX << 1) + 1, 1);
            setZeroCoeff(2, mbX << 1, 0);
            setZeroCoeff(2, (mbX << 1) + 1, 1);
        }
    }

    private void readChromaAC(boolean leftAvailable, boolean topAvailable, int mbX, int[] dc, int comp,
            MBType curMbType, boolean codedAC, int[][] residualOut) {
        for (int i = 0; i < dc.length; i++) {
            int[] ac = residualOut[i];
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];

            int blkX = (mbX << 1) + blkOffLeft;

            if (codedAC) {
                readChromaAC(leftAvailable, topAvailable, mbX, comp, curMbType, ac, blkOffLeft, blkOffTop, blkX);
            } else {
                if (!sh.pps.entropy_coding_mode_flag)
                    setZeroCoeff(comp, blkX, blkOffTop);
            }
        }
    }

    private void readIPCM(MBlock mBlock) {
        reader.align();

        mBlock.ipcm.samplesLuma = new int[256];
        for (int i = 0; i < 256; i++) {
            mBlock.ipcm.samplesLuma[i] = reader.readNBit(8);
        }
        int MbWidthC = 16 >> s.chromaFormat.compWidth[1];
        int MbHeightC = 16 >> s.chromaFormat.compHeight[1];

        mBlock.ipcm.samplesChroma = new int[2 * MbWidthC * MbHeightC];
        for (int i = 0; i < 2 * MbWidthC * MbHeightC; i++) {
            mBlock.ipcm.samplesChroma[i] = reader.readNBit(8);
        }
    }

    public void readMBlock(MBlock mBlock, SliceType sliceType) {

        if (sliceType == SliceType.I) {
            readMBlockI(mBlock);
        } else if (sliceType == SliceType.P) {
            readMBlockP(mBlock);
        } else {
            readMBlockB(mBlock);
        }
    }

    private void readMBlockI(MBlock mBlock) {

        mBlock.mbType = decodeMBTypeI(mBlock.mbIdx, mapper.leftAvailable(mBlock.mbIdx),
                mapper.topAvailable(mBlock.mbIdx), s.leftMBType, s.topMBType[mapper.getMbX(mBlock.mbIdx)]);

        readMBlockIInt(mBlock, mBlock.mbType);
    }

    private void readMBlockIInt(MBlock mBlock, int mbType) {
        if (mbType == 0) {
            mBlock.curMbType = MBType.I_NxN;
            readIntraNxN(mBlock);
        } else if (mbType >= 1 && mbType <= 24) {
            mBlock.curMbType = MBType.I_16x16;
            readIntra16x16(mbType - 1, mBlock);
        } else {
            Logger.warn("IPCM macroblock found. Not tested, may cause unpredictable behavior.");
            mBlock.curMbType = MBType.I_PCM;
            readIPCM(mBlock);
        }
    }

    private void readMBlockP(MBlock mBlock) {
        mBlock.mbType = readMBTypeP();

        switch (mBlock.mbType) {
        case 0:
            mBlock.curMbType = MBType.P_16x16;
            readInter16x16(L0, mBlock);
            break;
        case 1:
            mBlock.curMbType = MBType.P_16x8;
            readInter16x8(L0, L0, mBlock);
            break;
        case 2:
            mBlock.curMbType = MBType.P_8x16;
            readIntra8x16(L0, L0, mBlock);
            break;
        case 3:
            mBlock.curMbType = MBType.P_8x8;
            readMBlock8x8(mBlock);
            break;
        case 4:
            mBlock.curMbType = MBType.P_8x8ref0;
            readMBlock8x8(mBlock);
            break;
        default:
            readMBlockIInt(mBlock, mBlock.mbType - 5);
        }
    }

    private void readMBlockB(MBlock mBlock) {
        mBlock.mbType = readMBTypeB(mBlock.mbIdx, mapper.leftAvailable(mBlock.mbIdx),
                mapper.topAvailable(mBlock.mbIdx), s.leftMBType, s.topMBType[mapper.getMbX(mBlock.mbIdx)]);
        if (mBlock.mbType >= 23) {
            readMBlockIInt(mBlock, mBlock.mbType - 23);
        } else {
            mBlock.curMbType = H264Const.bMbTypes[mBlock.mbType];

            if (mBlock.mbType == 0) {
                readMBlockBDirect(mBlock);
            } else if (mBlock.mbType <= 3) {
                readInter16x16(H264Const.bPredModes[mBlock.mbType][0], mBlock);
            } else if (mBlock.mbType == 22) {
                readMBlock8x8(mBlock);
            } else if ((mBlock.mbType & 1) == 0) {
                readInter16x8(H264Const.bPredModes[mBlock.mbType][0], H264Const.bPredModes[mBlock.mbType][1], mBlock);
            } else {
                readIntra8x16(H264Const.bPredModes[mBlock.mbType][0], H264Const.bPredModes[mBlock.mbType][1], mBlock);
            }
        }
    }

    private boolean prevMbSkipped = false;
    private int mbIdx;
    private MBType prevMBType = null;
    private int mbSkipRun;
    private boolean endOfData;

    public MBlock readMacroblock() {
        if (endOfData && mbSkipRun == 0)
            return null;

        MBlock mBlock = new MBlock();
        mBlock.mbIdx = mbIdx;
        mBlock.prevMbType = prevMBType;

        int mbWidth1 = sh.sps.pic_width_in_mbs_minus1 + 1;
        boolean mbaffFrameFlag = (sh.sps.mb_adaptive_frame_field_flag && !sh.field_pic_flag);

        if (sh.slice_type.isInter() && !activePps.entropy_coding_mode_flag) {
            if (!prevMbSkipped && mbSkipRun == 0) {
                mbSkipRun = readUE(reader, "mb_skip_run");
                if (!moreRBSPData(reader)) {
                    endOfData = true;
                }
            }

            if (mbSkipRun > 0) {
                --mbSkipRun;
                int mbAddr = mapper.getAddress(mbIdx);
                prevMbSkipped = true;
                prevMBType = null;
                debugPrint("---------------------- MB (" + (mbAddr % mbWidth1) + "," + (mbAddr / mbWidth1)
                        + ") ---------------------");
                mBlock.skipped = true;
                ++mbIdx;
                return mBlock;
            } else {
                prevMbSkipped = false;
            }
        }

        int mbAddr = mapper.getAddress(mbIdx);
        int mbX = mbAddr % mbWidth1;
        int mbY = mbAddr / mbWidth1;
        debugPrint("---------------------- MB (" + mbX + "," + mbY + ") ---------------------");

        if (sh.slice_type.isIntra()
                || (!activePps.entropy_coding_mode_flag || !readMBSkipFlag(sh.slice_type, mapper.leftAvailable(mbIdx),
                        mapper.topAvailable(mbIdx), mbX))) {

            boolean mb_field_decoding_flag = false;
            if (mbaffFrameFlag && (mbIdx % 2 == 0 || (mbIdx % 2 == 1 && prevMbSkipped))) {
                mb_field_decoding_flag = readBool(reader, "mb_field_decoding_flag");
            }

            mBlock.fieldDecoding = mb_field_decoding_flag;
            readMBlock(mBlock, sh.slice_type);

            prevMBType = mBlock.curMbType;

        } else {
            prevMBType = null;
            prevMbSkipped = true;
            mBlock.skipped = true;
        }

        endOfData = (activePps.entropy_coding_mode_flag && mDecoder.decodeFinalBin() == 1)
                || (!activePps.entropy_coding_mode_flag && !moreRBSPData(reader));

        ++mbIdx;

        return mBlock;
    }
}
