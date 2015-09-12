package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.identityMapping16;
import static org.jcodec.codecs.h264.H264Const.last_sig_coeff_map_8x8;
import static org.jcodec.codecs.h264.H264Const.sig_coeff_map_8x8;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readTE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUE;
import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.CABAC.BlockType;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.io.BitReader;

/**
 * Contains methods for reading high-level symbols out of H.264 bitstream
 * 
 * @author The JCodec Project
 */
public class BitstreamParser {

    private PictureParameterSet activePps;
    private CABAC cabac;
    private MDecoder mDecoder;
    private DecoderState sharedState;
    private CAVLC[] cavlc;
    private BitReader reader;
    private DeblockerInput di;

    public BitstreamParser(PictureParameterSet activePps, CABAC cabac, CAVLC[] cavlc, MDecoder mDecoder,
            BitReader reader, DeblockerInput di, DecoderState sharedContext) {
        this.activePps = activePps;
        this.cabac = cabac;
        this.mDecoder = mDecoder;
        this.sharedState = sharedContext;
        this.cavlc = cavlc;
        this.reader = reader;
        this.di = di;
    }

    int readMBQpDelta(BitReader reader, MBType prevMbType) {
        int mbQPDelta;
        if (!activePps.entropy_coding_mode_flag) {
            mbQPDelta = readSE(reader, "mb_qp_delta");
        } else {
            mbQPDelta = cabac.readMBQpDelta(mDecoder, prevMbType);
        }
        return mbQPDelta;
    }

    int readChromaPredMode(BitReader reader, int mbX, boolean leftAvailable, boolean topAvailable) {
        int chromaPredictionMode;
        if (!activePps.entropy_coding_mode_flag) {
            chromaPredictionMode = readUE(reader, "MBP: intra_chroma_pred_mode");
        } else {
            chromaPredictionMode = cabac.readIntraChromaPredMode(mDecoder, mbX, sharedState.leftMBType,
                    sharedState.topMBType[mbX], leftAvailable, topAvailable);
        }
        return chromaPredictionMode;
    }

    boolean readTransform8x8Flag(BitReader reader, boolean leftAvailable, boolean topAvailable, MBType leftType,
            MBType topType, boolean is8x8Left, boolean is8x8Top) {
        if (!activePps.entropy_coding_mode_flag)
            return readBool(reader, "transform_size_8x8_flag");
        else
            return cabac.readTransform8x8Flag(mDecoder, leftAvailable, topAvailable, leftType, topType, is8x8Left,
                    is8x8Top);
    }

    protected int readCodedBlockPatternIntra(BitReader reader, boolean leftAvailable, boolean topAvailable,
            int leftCBP, int topCBP, MBType leftMB, MBType topMB) {

        if (!activePps.entropy_coding_mode_flag)
            return H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR[readUE(reader, "coded_block_pattern")];
        else
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
    }

    protected int readCodedBlockPatternInter(BitReader reader, boolean leftAvailable, boolean topAvailable,
            int leftCBP, int topCBP, MBType leftMB, MBType topMB) {
        if (!activePps.entropy_coding_mode_flag)
            return H264Const.CODED_BLOCK_PATTERN_INTER_COLOR[readUE(reader, "coded_block_pattern")];
        else
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
    }

    int readRefIdx(BitReader reader, boolean leftAvailable, boolean topAvailable, MBType leftType, MBType topType,
            PartPred leftPred, PartPred topPred, PartPred curPred, int mbX, int partX, int partY, int partW, int partH,
            int list) {
        if (!activePps.entropy_coding_mode_flag)
            return readTE(reader, sharedState.numRef[list] - 1);
        else
            return cabac.readRefIdx(mDecoder, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                    curPred, mbX, partX, partY, partW, partH, list);
    }

    int readMVD(BitReader reader, int comp, boolean leftAvailable, boolean topAvailable, MBType leftType,
            MBType topType, PartPred leftPred, PartPred topPred, PartPred curPred, int mbX, int partX, int partY,
            int partW, int partH, int list) {
        if (!activePps.entropy_coding_mode_flag)
            return readSE(reader, "mvd_l0_x");
        else
            return cabac.readMVD(mDecoder, comp, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                    curPred, mbX, partX, partY, partW, partH, list);
    }

    int readPredictionI4x4Block(BitReader reader, boolean leftAvailable, boolean topAvailable, MBType leftMBType,
            MBType topMBType, int blkX, int blkY, int mbX) {
        int mode = 2;
        if ((leftAvailable || blkX > 0) && (topAvailable || blkY > 0)) {
            int predModeB = topMBType == MBType.I_NxN || blkY > 0 ? sharedState.i4x4PredTop[(mbX << 2) + blkX] : 2;
            int predModeA = leftMBType == MBType.I_NxN || blkX > 0 ? sharedState.i4x4PredLeft[blkY] : 2;
            mode = Math.min(predModeB, predModeA);
        }
        if (!prev4x4PredMode(reader)) {
            int rem_intra4x4_pred_mode = rem4x4PredMode(reader);
            mode = rem_intra4x4_pred_mode + (rem_intra4x4_pred_mode < mode ? 0 : 1);
        }
        sharedState.i4x4PredTop[(mbX << 2) + blkX] = sharedState.i4x4PredLeft[blkY] = mode;
        return mode;
    }

    int rem4x4PredMode(BitReader reader) {
        if (!activePps.entropy_coding_mode_flag)
            return readNBit(reader, 3, "MB: rem_intra4x4_pred_mode");
        else
            return cabac.rem4x4PredMode(mDecoder);
    }

    boolean prev4x4PredMode(BitReader reader) {
        if (!activePps.entropy_coding_mode_flag)
            return readBool(reader, "MBP: prev_intra4x4_pred_mode_flag");
        else
            return cabac.prev4x4PredModeFlag(mDecoder);
    }

    void read16x16DC(boolean leftAvailable, boolean topAvailable, int mbX, int[] dc) {
        if (!activePps.entropy_coding_mode_flag)
            cavlc[0].readLumaDCBlock(reader, dc, mbX, leftAvailable, sharedState.leftMBType, topAvailable,
                    sharedState.topMBType[mbX], CoeffTransformer.zigzag4x4);
        else {
            if (cabac.readCodedBlockFlagLumaDC(mDecoder, mbX, sharedState.leftMBType, sharedState.topMBType[mbX],
                    leftAvailable, topAvailable, MBType.I_16x16) == 1)
                cabac.readCoeffs(mDecoder, BlockType.LUMA_16_DC, dc, 0, 16, CoeffTransformer.zigzag4x4,
                        identityMapping16, identityMapping16);
        }
    }

    void read16x16AC(boolean leftAvailable, boolean topAvailable, int mbX, int cbpLuma, int[] ac, int blkOffLeft,
            int blkOffTop, int blkX, int blkY) {
        if (!activePps.entropy_coding_mode_flag) {
            di.nCoeff[blkY][blkX] = cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    blkOffLeft == 0 ? sharedState.leftMBType : I_16x16, blkOffTop != 0 || topAvailable,
                    blkOffTop == 0 ? sharedState.topMBType[mbX] : I_16x16, 1, 15, CoeffTransformer.zigzag4x4);
        } else {
            if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_15_AC, blkX, blkOffTop, 0,
                    sharedState.leftMBType, sharedState.topMBType[mbX], leftAvailable, topAvailable,
                    sharedState.leftCBPLuma, sharedState.topCBPLuma[mbX], cbpLuma, MBType.I_16x16) == 1)
                di.nCoeff[blkY][blkX] = cabac.readCoeffs(mDecoder, BlockType.LUMA_15_AC, ac, 1, 15,
                        CoeffTransformer.zigzag4x4, identityMapping16, identityMapping16);
        }
    }

    void readResidualAC(boolean leftAvailable, boolean topAvailable, int mbX, MBType curMbType, int cbpLuma,
            int blkOffLeft, int blkOffTop, int blkX, int blkY, int[] ac) {
        if (!activePps.entropy_coding_mode_flag) {
            di.nCoeff[blkY][blkX] = cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    blkOffLeft == 0 ? sharedState.leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                    blkOffTop == 0 ? sharedState.topMBType[mbX] : curMbType, 0, 16, CoeffTransformer.zigzag4x4);
        } else {
            if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_16, blkX, blkOffTop, 0, sharedState.leftMBType,
                    sharedState.topMBType[mbX], leftAvailable, topAvailable, sharedState.leftCBPLuma,
                    sharedState.topCBPLuma[mbX], cbpLuma, curMbType) == 1)
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

    public int readLumaAC(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, MBType curMbType,
            int blkX, int j, int[] ac16, int blkOffLeft, int blkOffTop) {
        return cavlc[0].readACBlock(reader, ac16, blkX + (j & 1), blkOffTop, blkOffLeft != 0 || leftAvailable,
                blkOffLeft == 0 ? sharedState.leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                blkOffTop == 0 ? sharedState.topMBType[mbX] : curMbType, 0, 16, H264Const.identityMapping16);
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

    public int readSubMBTypeP(BitReader reader) {
        if (!activePps.entropy_coding_mode_flag)
            return readUE(reader, "SUB: sub_mb_type");
        else
            return cabac.readSubMbTypeP(mDecoder);
    }

    public int readSubMBTypeB(BitReader reader) {
        if (!activePps.entropy_coding_mode_flag)
            return readUE(reader, "SUB: sub_mb_type");
        else
            return cabac.readSubMbTypeB(mDecoder);
    }

    public void readChromaDC(BitReader reader, int mbX, boolean leftAvailable, boolean topAvailable, int[] dc,
            int comp, MBType curMbType) {
        if (!activePps.entropy_coding_mode_flag)
            cavlc[comp].readChromaDCBlock(reader, dc, leftAvailable, topAvailable);
        else {
            if (cabac.readCodedBlockFlagChromaDC(mDecoder, mbX, comp, sharedState.leftMBType,
                    sharedState.topMBType[mbX], leftAvailable, topAvailable, sharedState.leftCBPChroma,
                    sharedState.topCBPChroma[mbX], curMbType) == 1)
                cabac.readCoeffs(mDecoder, BlockType.CHROMA_DC, dc, 0, 4, identityMapping16, identityMapping16,
                        identityMapping16);
        }
    }

    public void readChromaAC(BitReader reader, boolean leftAvailable, boolean topAvailable, int mbX, int comp,
            MBType curMbType, int[] ac, int blkOffLeft, int blkOffTop, int blkX) {
        if (!activePps.entropy_coding_mode_flag)
            cavlc[comp].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    blkOffLeft == 0 ? sharedState.leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                    blkOffTop == 0 ? sharedState.topMBType[mbX] : curMbType, 1, 15, CoeffTransformer.zigzag4x4);
        else {
            if (cabac.readCodedBlockFlagChromaAC(mDecoder, blkX, blkOffTop, comp, sharedState.leftMBType,
                    sharedState.topMBType[mbX], leftAvailable, topAvailable, sharedState.leftCBPChroma,
                    sharedState.topCBPChroma[mbX], curMbType) == 1)
                cabac.readCoeffs(mDecoder, BlockType.CHROMA_AC, ac, 1, 15, CoeffTransformer.zigzag4x4,
                        identityMapping16, identityMapping16);
        }
    }

    public int decodeMBTypeI(int mbIdx, BitReader reader, boolean leftAvailable, boolean topAvailable,
            MBType leftMBType, MBType topMBType) {
        int mbType;
        if (!activePps.entropy_coding_mode_flag)
            mbType = readUE(reader, "MB: mb_type");
        else
            mbType = cabac.readMBTypeI(mDecoder, leftMBType, topMBType, leftAvailable, topAvailable);
        return mbType;
    }

    public int readMBTypeP(BitReader reader) {
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

    public int decodeFinalBin() {
        return mDecoder.decodeFinalBin();
    }
}
