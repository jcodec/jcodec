package org.jcodec.codecs.h264.io.read;

import static java.lang.Math.min;
import static org.jcodec.common.tools.MathUtil.clip;

import java.io.IOException;
import java.io.InputStream;

import org.jcodec.codecs.common.biari.Context;
import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.h264.decode.MBlockDecoderInter;
import org.jcodec.codecs.h264.decode.model.MVMatrix;
import org.jcodec.codecs.h264.io.model.CodedMacroblock;
import org.jcodec.codecs.h264.io.model.Inter8x8Prediction;
import org.jcodec.codecs.h264.io.model.InterPrediction;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.MBlockBDirect16x16;
import org.jcodec.codecs.h264.io.model.MBlockIPCM;
import org.jcodec.codecs.h264.io.model.MBlockInter;
import org.jcodec.codecs.h264.io.model.MBlockInter8x8;
import org.jcodec.codecs.h264.io.model.MBlockIntra16x16;
import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;
import org.jcodec.codecs.h264.io.model.MBlockWithResidual;
import org.jcodec.codecs.h264.io.model.Macroblock;
import org.jcodec.codecs.h264.io.model.ResidualBlock;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.io.model.SubMBType;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.common.model.Point;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author JCodec project
 * 
 */
public class CABACReader {

    private Context[] contexts;
    private MDecoder decoder;
    private SliceType sliceType;

    public CABACReader(InputStream in, SliceType sliceType, int cabacIdc, int sliceQp) throws IOException {
        decoder = new MDecoder(in);
        this.sliceType = sliceType;

        int[] tabA = sliceType.isIntra() ? CABACContst.cabac_context_init_I_A
                : CABACContst.cabac_context_init_PB_A[cabacIdc];
        int[] tabB = sliceType.isIntra() ? CABACContst.cabac_context_init_I_B
                : CABACContst.cabac_context_init_PB_B[cabacIdc];

        contexts = new Context[1024];
        for (int i = 0; i < 1024; i++) {
            int preCtxState = clip(((tabA[i] * clip(sliceQp, 0, 51)) >> 4) + tabB[i], 1, 126);
            if (preCtxState <= 63)
                contexts[i] = new Context(63 - preCtxState, 0);
            else
                contexts[i] = new Context(preCtxState - 64, 1);
        }
    }

    public int readCBP16x16() throws IOException {
        int ctx = sliceType.isIntra() ? 6 : (sliceType == SliceType.B ? 33 : 18);

        int cbpLuma = decoder.decodeBin(contexts[ctx++]);
        if (sliceType.isIntra()) {
            int cbpChroma = decoder.decodeBin(contexts[ctx++]);
            if (cbpChroma != 0) {
                cbpChroma += decoder.decodeBin(contexts[ctx++]);
            }
            int predMode = (decoder.decodeBin(contexts[ctx++]) << 1) | decoder.decodeBin(contexts[ctx]);
        } else {
            int cbpChroma = decoder.decodeBin(contexts[ctx]);
            if (cbpChroma != 0) {
                cbpChroma += decoder.decodeBin(contexts[ctx]);
            }
            ctx++;
            int predMode = (decoder.decodeBin(contexts[ctx]) << 1) | decoder.decodeBin(contexts[ctx]);
        }
        throw new RuntimeException();
    }

    public MBType readMBType(Macroblock left, Macroblock top) throws IOException {
        switch (sliceType) {
        case SI:
            return readMBTypeSI(left, top);
        case I:
            return readMBTypeI(left, top);
        case P:
        case SP:
            return readMBTypeP(left, top);
        case B:
            return readMBTypeB(left, top);
        default:
            return null;
        }
    }

    private MBType readMBTypeB(Macroblock left, Macroblock top) throws IOException {
        int ctx = 27;
        ctx += (left == null) || (left instanceof MBlockBDirect16x16) ? 0 : 1;
        ctx += (top == null) || (top instanceof MBlockBDirect16x16) ? 0 : 1;

        if (decoder.decodeBin(contexts[ctx]) == 0)
            return MBType.B_Direct_16x16;

        if (decoder.decodeBin(contexts[30]) == 0) {
            return decoder.decodeBin(contexts[32]) == 0 ? MBType.B_L0_16x16 : MBType.B_L1_16x16;
        }

        if (decoder.decodeBin(contexts[31]) == 0) {
            switch ((decoder.decodeBin(contexts[32]) << 2) | (decoder.decodeBin(contexts[32]) << 1)
                    | decoder.decodeBin(contexts[32])) {
            case 0:
                return MBType.B_Bi_16x16;
            case 1:
                return MBType.B_L0_L0_16x8;
            case 2:
                return MBType.B_L0_L0_8x16;
            case 3:
                return MBType.B_L1_L1_16x8;
            case 4:
                return MBType.B_L1_L1_8x16;
            case 5:
                return MBType.B_L0_L1_16x8;
            case 6:
                return MBType.B_L0_L1_8x16;
            case 7:
                return MBType.B_L1_L0_16x8;
            default:
                return null;
            }
        } else {
            int tmp = (decoder.decodeBin(contexts[32]) << 2) | (decoder.decodeBin(contexts[32]) << 1)
                    | decoder.decodeBin(contexts[32]);
            if (tmp == 5)
                return readMBTypeI_Inter(32);
            else if (tmp == 6)
                return MBType.B_L1_L0_8x16;
            else if (tmp == 7)
                return MBType.B_8x8;
            else {
                switch ((tmp << 1) | decoder.decodeBin(contexts[32])) {
                case 0:
                    return MBType.B_L0_Bi_16x8;
                case 1:
                    return MBType.B_L0_Bi_8x16;
                case 2:
                    return MBType.B_L1_Bi_16x8;
                case 3:
                    return MBType.B_L1_Bi_8x16;
                case 4:
                    return MBType.B_Bi_L0_16x8;
                case 5:
                    return MBType.B_Bi_L0_8x16;
                case 6:
                    return MBType.B_Bi_L1_16x8;
                case 7:
                    return MBType.B_Bi_L1_8x16;
                case 8:
                    return MBType.B_Bi_Bi_16x8;
                case 9:
                    return MBType.B_Bi_Bi_8x16;
                default:
                    return null;
                }
            }
        }
    }

    private MBType readMBTypeP(Macroblock left, Macroblock top) throws IOException {
        int ctx = 14;

        if (decoder.decodeBin(contexts[ctx++]) == 1) {
            return readMBTypeI_Inter(17);
        }

        int b1 = decoder.decodeBin(contexts[ctx++]);
        int b2 = decoder.decodeBin(contexts[ctx + b1]);

        switch ((b1 << 1) + b2) {
        case 0:
            return MBType.P_L0_16x16;
        case 1:
            return MBType.P_8x8;
        case 2:
            return MBType.P_L0_L0_8x16;
        default:
            return MBType.P_L0_L0_16x8;
        }
    }

    private MBType readMBTypeI(Macroblock left, Macroblock top) throws IOException {
        int ctx = 3;
        ctx += (left == null) || (left instanceof MBlockIntraNxN) ? 0 : 1;
        ctx += (top == null) || (top instanceof MBlockIntraNxN) ? 0 : 1;

        if (decoder.decodeBin(contexts[ctx]) == 0) {
            return MBType.I_NxN;
        } else {
            return decoder.decodeFinalBin() == 1 ? MBType.I_PCM : MBType.I_16x16;
        }
    }

    private MBType readMBTypeI_Inter(int ctx) throws IOException {
        if (decoder.decodeBin(contexts[ctx]) == 0) {
            return MBType.I_NxN;
        } else {
            return decoder.decodeFinalBin() == 1 ? MBType.I_PCM : MBType.I_16x16;
        }
    }

    private MBType readMBTypeSI(Macroblock left, Macroblock top) {
        throw new UnsupportedOperationException();
    }

    public boolean readMBSkipFlag() throws IOException {
        if (sliceType == SliceType.B)
            return decoder.decodeBin(contexts[24]) == 1;
        else
            return decoder.decodeBin(contexts[11]) == 1;
    }

    public int readMVD(int baseCtx, Macroblock left, Macroblock top, Macroblock cur, int blkIdx, int subBlkIdx, int comp)
            throws IOException {

        Vector[] cool = cool(left, top, cur, blkIdx, subBlkIdx);

        // todo: should be mvd, not a full vector
        int absMVDLeft = left == null ? 0 : (comp == 0 ? cool[0].getX() : cool[0].getY());
        int absMVDTop = top == null ? 0 : (comp == 0 ? cool[1].getX() : cool[1].getY());

        int tot = absMVDLeft + absMVDTop;
        int inc = tot < 3 ? 0 : (tot > 32 ? 2 : 1);

        int bit = decoder.decodeBin(contexts[baseCtx + inc]);
        int n = 0, ctx = baseCtx + 3, ctxMax = baseCtx + 6;
        while (bit == 1) {
            n++;
            if (n == 9)
                break;
            bit = decoder.decodeBin(contexts[ctx++]);
            if (ctx > ctxMax)
                ctx = ctxMax;
        }

        return n < 9 ? n : n + readGolomb(3);
    }

    public int readCodedBlockFlagChromaDC(BlockType blkType, Macroblock left, Macroblock top, Macroblock cur, boolean cb)
            throws IOException {

        ResidualBlock dcLeft = left != null && left instanceof CodedMacroblock ? (cb ? ((CodedMacroblock) left)
                .getChroma().getCbDC() : ((CodedMacroblock) left).getChroma().getCrDC()) : null;
        ResidualBlock dcTop = top != null && top instanceof CodedMacroblock ? (cb ? ((CodedMacroblock) top).getChroma()
                .getCbDC() : ((CodedMacroblock) top).getChroma().getCrDC()) : null;

        return decoder.decodeBin(contexts[blkType.codedBlockFlagCtx + transBlockToCBF(left, cur, dcLeft) + 2
                * transBlockToCBF(top, cur, dcTop)]);

    }

    public int readCodedBlockFlagChromaAC(BlockType blkType, Macroblock left, Macroblock top, Macroblock cur,
            int blkIdx, boolean cb) throws IOException {
        ResidualBlock blkLeft = null;
        if (left != null && left instanceof CodedMacroblock) {
            CodedMacroblock mb = (CodedMacroblock) (leftChromaSameMB[blkIdx] ? cur : left);
            ResidualBlock[] AC = cb ? mb.getChroma().getCbAC() : mb.getChroma().getCrAC();
            blkLeft = AC[leftChromaIdx[blkIdx]];
        }
        ResidualBlock blkTop = null;
        if (top != null && top instanceof CodedMacroblock) {
            CodedMacroblock mb = (CodedMacroblock) (topChromaSameMB[blkIdx] ? cur : top);
            ResidualBlock[] AC = cb ? mb.getChroma().getCbAC() : mb.getChroma().getCrAC();
            blkLeft = AC[topChromaIdx[blkIdx]];
        }
        return decoder.decodeBin(contexts[blkType.codedBlockFlagCtx + transBlockToCBF(left, cur, blkLeft) + 2
                * transBlockToCBF(top, cur, blkTop)]);
    }

    public int readCodedBlockFlag16x16DC(BlockType blkType, Macroblock left, Macroblock top, Macroblock cur)
            throws IOException {
        if (blkType != BlockType.LUMA_16_DC)
            throw new RuntimeException("Chroma 444 not supported");

        ResidualBlock dcLeft = left != null && left instanceof MBlockIntra16x16 ? ((MBlockIntra16x16) left).getLumaDC()
                : null;
        ResidualBlock dcTop = top != null && top instanceof MBlockIntra16x16 ? ((MBlockIntra16x16) top).getLumaDC()
                : null;

        return decoder.decodeBin(contexts[blkType.codedBlockFlagCtx + transBlockToCBF(left, cur, dcLeft) + 2
                * transBlockToCBF(top, cur, dcTop)]);
    }

    public int readCodedBlockFlag16x16AC(BlockType blkType, int blkIdx, Macroblock cur, Macroblock left, Macroblock top)
            throws IOException {
        if (blkType != BlockType.LUMA_15x16_AC)
            throw new RuntimeException("Chroma 444 not supported");

        Macroblock srcLeft = leftBlockSameMB[blkIdx] ? cur : left;
        Macroblock srcTop = topBlockSameMB[blkIdx] ? cur : top;
        ResidualBlock blkLeft = null;
        if (srcLeft instanceof MBlockIntra16x16) {
            blkLeft = ((MBlockIntra16x16) srcLeft).getLumaAC()[leftBlockIdx[blkIdx]];
        }
        ResidualBlock blkTop = null;
        if (srcTop instanceof MBlockIntra16x16) {
            blkTop = ((MBlockIntra16x16) srcTop).getLumaAC()[topBlockIdx[blkIdx]];
        }
        return decoder.decodeBin(contexts[blkType.codedBlockFlagCtx + transBlockToCBF(left, cur, blkLeft) + 2
                * transBlockToCBF(top, cur, blkTop)]);
    }

    public int readCodedBlockFlagNxN(BlockType blkType, int blkIdx, Macroblock cur, Macroblock left, Macroblock top)
            throws IOException {
        if (blkType != BlockType.LUMA_16)
            throw new RuntimeException("Chroma 444 not supported");
        Macroblock srcLeft = leftBlockSameMB[blkIdx] ? cur : left;
        Macroblock srcTop = topBlockSameMB[blkIdx] ? cur : top;
        ResidualBlock blkLeft = null;
        if (srcLeft instanceof MBlockWithResidual) {
            blkLeft = ((MBlockWithResidual) srcLeft).getLuma()[leftBlockIdx[blkIdx]];
        }
        ResidualBlock blkTop = null;
        if (srcTop instanceof MBlockWithResidual) {
            blkTop = ((MBlockWithResidual) srcTop).getLuma()[topBlockIdx[blkIdx]];
        }
        return decoder.decodeBin(contexts[blkType.codedBlockFlagCtx + transBlockToCBF(left, cur, blkLeft) + 2
                * transBlockToCBF(top, cur, blkTop)]);
    }

    private int transBlockToCBF(Macroblock mbN, Macroblock mbCur, ResidualBlock transBlk) {
        if (mbN == null && ((mbCur instanceof MBlockIntraNxN) || (mbCur instanceof MBlockIntra16x16))
                || (mbN instanceof MBlockIPCM))
            return 1;
        if (mbN == null && ((mbCur instanceof MBlockInter) || (mbCur instanceof MBlockInter8x8)) || mbN != null
                && transBlk == null && !(mbN instanceof MBlockIPCM))
            return 0;
        return transBlk == null ? 0 : 1;
    }

    boolean[] leftBlockSameMB = new boolean[] { false, true, false, true, true, true, true, true, false, true, false,
            true, true, true, true, true };
    boolean[] topBlockSameMB = new boolean[] { false, false, true, true, false, false, true, true, true, true, true,
            true, true, true, true, true };

    int[] leftBlockIdx = new int[] { 5, 0, 7, 2, 1, 4, 3, 6, 13, 8, 15, 10, 9, 12, 11, 14 };

    int[] topBlockIdx = new int[] { 10, 11, 0, 1, 14, 15, 4, 5, 2, 3, 8, 9, 6, 7, 12, 13 };

    boolean[] leftChromaSameMB = new boolean[] { false, true, false, true };
    boolean[] topChromaSameMB = new boolean[] { false, false, true, true };

    int[] leftChromaIdx = new int[] { 1, 0, 3, 2 };

    int[] topChromaIdx = new int[] { 2, 3, 0, 1 };

    public int readSigCoeffFlagChromaDC(BlockType blkType, int coeffIdx, int numChromaDC, boolean field)
            throws IOException {
        return decoder.decodeBin(contexts[(field ? blkType.sigCoeffFlagFldCtx : blkType.sigCoeffFlagCtx)
                + Math.min(coeffIdx / numChromaDC, 2)]);
    }

    public int readSigCoeffFlag8x8(BlockType blkType, int coeffIdx, boolean field) throws IOException {
        return decoder.decodeBin(contexts[(field ? blkType.sigCoeffFlagFldCtx : blkType.sigCoeffFlagCtx)
                + (field ? sigCoeff64To16Fld[coeffIdx] : sigCoeff64To16[coeffIdx])]);
    }

    public int readSigCoeffFlag(BlockType blkType, int coeffIdx, boolean field) throws IOException {
        return decoder.decodeBin(contexts[(field ? blkType.sigCoeffFlagFldCtx : blkType.sigCoeffFlagCtx) + coeffIdx]);
    }

    public int readLastSigCoeffFlagChromaDC(BlockType blkType, int coeffIdx, int numChromaDC, boolean field)
            throws IOException {
        return decoder.decodeBin(contexts[(field ? blkType.lastSigCoeffFlagFldCtx : blkType.lastSigCoeffFlagCtx)
                + Math.min(coeffIdx / numChromaDC, 2)]);
    }

    public int readLastSigCoeffFlag8x8(BlockType blkType, int coeffIdx, int numChromaDC, boolean field)
            throws IOException {
        return decoder.decodeBin(contexts[(field ? blkType.lastSigCoeffFlagFldCtx : blkType.lastSigCoeffFlagCtx)
                + lastSigCoeff64To16[coeffIdx]]);
    }

    public int readLastSigCoeffFlag(BlockType blkType, int coeffIdx, int numChromaDC, boolean field) throws IOException {
        return decoder.decodeBin(contexts[(field ? blkType.lastSigCoeffFlagFldCtx : blkType.lastSigCoeffFlagCtx)
                + coeffIdx]);
    }

    int sigCoeff64To16[] = { 0, 1, 2, 3, 4, 5, 5, 4, 4, 3, 3, 4, 4, 4, 5, 5, 4, 4, 4, 4, 3, 3, 6, 7, 7, 7, 8, 9, 10, 9,
            8, 7, 7, 6, 11, 12, 13, 11, 6, 7, 8, 9, 14, 10, 9, 8, 6, 11, 12, 13, 11, 6, 9, 14, 10, 9, 11, 12, 13, 11,
            14, 10, 12 };

    int sigCoeff64To16Fld[] = { 0, 1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 7, 7, 8, 4, 5, 6, 9, 10, 10, 8, 11, 12, 11, 9, 9, 10,
            10, 8, 11, 12, 11, 9, 9, 10, 10, 8, 11, 12, 11, 9, 9, 10, 10, 8, 13, 13, 9, 9, 10, 10, 8, 13, 13, 9, 9, 10,
            10, 14, 14, 14, 14, 14 };

    int lastSigCoeff64To16[] = { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8 };
    private boolean[] sameMb8x8Left;
    private int[] blk8x8Left;
    private boolean[] sameMb8x8Top;
    private int[] blk8x8Top;

    public int readCoeffAbsLevel(BlockType blkType, int eq1, int gt1) throws IOException {
        int n = 0;
        int bit = decoder.decodeBin(contexts[blkType.coeffLevelCtx + (gt1 != 0 ? 0 : min(4, 1 + eq1))]);
        Context bn = contexts[blkType.coeffLevelCtx + (5 + min(blkType.maxGt1, gt1))];
        while (bit == 1) {
            n++;
            if (n == 14)
                break;
            bit = decoder.decodeBin(bn);
        }

        return n < 14 ? n : n + readGolomb(0);
    }

    private int readGolomb(int k) throws IOException {
        while (decoder.decodeBinBypass() == 1)
            k++;

        int result = 0;
        while (k-- > 0) {
            result = (result << 1) | decoder.decodeBinBypass();
        }

        return result;
    }

    public static enum BlockType {
        LUMA_16_DC(0, 85, 105, 166, 277, 338, 227, 4), LUMA_15x16_AC(1, 89, 120, 181, 292, 353, 237, 4), LUMA_16(2, 93,
                134, 195, 306, 367, 247, 4), CHROMA_DC(3, 97, 149, 210, 321, 382, 257, 3), CHROMA_AC(4, 101, 152, 213,
                324, 385, 266, 4), LUMA_64(5, 1012, 402, 417, 436, 451, 426, 4), CB_16_DC(6, 460, 484, 572, 776, 864,
                952, 4), CB_15x16_AC(7, 464, 499, 587, 791, 879, 962, 4), CB_16(8, 468, 513, 601, 805, 893, 972, 4), CB_64(
                9, 1016, 660, 690, 675, 699, 708, 4), CR_16_DC(10, 472, 528, 616, 820, 908, 982, 4), CR_15x16_AC(11,
                476, 543, 631, 835, 923, 992, 4), CR_16(12, 480, 557, 645, 849, 937, 1002, 4), CR_64(13, 1020, 718,
                748, 733, 757, 766, 4);

        int cat;
        int codedBlockFlagCtx;

        int sigCoeffFlagCtx;
        int lastSigCoeffFlagCtx;

        int sigCoeffFlagFldCtx;
        int lastSigCoeffFlagFldCtx;

        int coeffLevelCtx;
        int maxGt1;

        private BlockType(int cat, int codedBlockFlag, int sigCoeffFlag, int lastSigCoeffFlag, int sigCoeffFldFlag,
                int lastSigCoeffFldFlag, int coeffLevel, int maxGt1) {
            this.cat = cat;
            this.codedBlockFlagCtx = codedBlockFlag;
            this.sigCoeffFlagCtx = sigCoeffFlag;
            this.lastSigCoeffFlagCtx = lastSigCoeffFlag;
            this.coeffLevelCtx = coeffLevel;
            this.maxGt1 = maxGt1;
        }

        public boolean is8x8() {
            return this == LUMA_64 || this == CB_64 || this == CR_64;
        }

        public boolean is4x4() {
            return this == LUMA_15x16_AC || this == LUMA_16 || this == CB_15x16_AC || this == CB_16
                    || this == CR_15x16_AC || this == CR_16;
        }

        public boolean is16x16DC() {
            return this == LUMA_16_DC || this == CB_16_DC || this == CR_16_DC;
        }
    }

    public SubMBType readSubMbTypeP() throws IOException {
        if (decoder.decodeBin(contexts[21]) == 1)
            return SubMBType.L0_8x8;
        else if (decoder.decodeBin(contexts[22]) == 0) {
            return SubMBType.L0_8x4;
        } else {
            return decoder.decodeBin(contexts[23]) == 1 ? SubMBType.L0_4x8 : SubMBType.L0_4x4;
        }
    }

    public static SubMBType bSubMB[] = { SubMBType.Bi_8x8, null, SubMBType.L0_8x4, null, SubMBType.L0_4x8, null,
            SubMBType.L1_8x4, null, SubMBType.L1_4x8, SubMBType.Bi_8x4, SubMBType.Bi_4x8, SubMBType.L0_4x4,
            SubMBType.L1_4x4, null, SubMBType.Bi_4x4 };

    public SubMBType readSubMbTypeB() throws IOException {
        if (decoder.decodeBin(contexts[36]) == 0)
            return SubMBType.Direct_8x8;
        else if (decoder.decodeBin(contexts[37]) == 0) {
            return decoder.decodeBin(contexts[39]) == 0 ? SubMBType.L0_8x8 : SubMBType.L1_8x8;
        } else {
            int tmp = (decoder.decodeBin(contexts[38]) << 3) | (decoder.decodeBin(contexts[39]) << 2)
                    | (decoder.decodeBin(contexts[39]) << 1);
            if (tmp == 8 || tmp == 10)
                tmp |= decoder.decodeBin(contexts[39]);
            return bSubMB[tmp];
        }
    }

    public Vector[] cool(Macroblock left, Macroblock top, Macroblock cur, int blkIdx, int subBlkIdx) {
        MVMatrix leftMvs = getMvs(left);
        MVMatrix topMvs = getMvs(top);
        MVMatrix curMvs = getMvs(cur);
        Point coord = getBlockCoord(cur, blkIdx, subBlkIdx);
        Vector topVect = new Vector[] { topMvs.getVectors()[12], topMvs.getVectors()[13], topMvs.getVectors()[14],
                topMvs.getVectors()[15], curMvs.getVectors()[0], curMvs.getVectors()[1], curMvs.getVectors()[2],
                curMvs.getVectors()[3], curMvs.getVectors()[4], curMvs.getVectors()[5], curMvs.getVectors()[6],
                curMvs.getVectors()[7], curMvs.getVectors()[8], curMvs.getVectors()[9], curMvs.getVectors()[10],
                curMvs.getVectors()[11] }[(coord.getY() << 2) + coord.getX()];

        Vector leftVect = new Vector[] { leftMvs.getVectors()[3], curMvs.getVectors()[0], curMvs.getVectors()[1],
                curMvs.getVectors()[2], leftMvs.getVectors()[7], curMvs.getVectors()[4], curMvs.getVectors()[5],
                curMvs.getVectors()[6], leftMvs.getVectors()[11], curMvs.getVectors()[8], curMvs.getVectors()[9],
                curMvs.getVectors()[10], leftMvs.getVectors()[15], curMvs.getVectors()[12], curMvs.getVectors()[13],
                curMvs.getVectors()[14] }[(coord.getY() << 2) + coord.getX()];

        return new Vector[] { leftVect, topVect };
    }

    public int readRefIdx(Macroblock left, Macroblock top, Macroblock cur, int blkIdx, int subBlkIdx)
            throws IOException {

        Vector[] cool = cool(left, top, cur, blkIdx, subBlkIdx);

        int bin0Off = (cool[0] == null || cool[0].getRefId() == 0 ? 0 : 1)
                + ((cool[1] == null || cool[1].getRefId() == 0 ? 0 : 1) << 1);
        if (decoder.decodeBin(contexts[54 + bin0Off]) == 0)
            return 0;
        if (decoder.decodeBin(contexts[58]) == 0)
            return 1;
        int num = 2;
        while (decoder.decodeBin(contexts[59]) == 1)
            num++;
        return num;
    }

    private Point getBlockCoord(Macroblock mb, int blkIdx, int subBlkIdx) {
        if (mb instanceof MBlockInter8x8) {
            Inter8x8Prediction prediction = ((MBlockInter8x8) mb).getPrediction();
            return getSubBlockCoord(prediction.getSubMbTypes()[blkIdx], subBlkIdx, (blkIdx & 0x1) << 1,
                    (blkIdx >> 1) << 1);
        } else if (mb instanceof MBlockInter) {
            switch (((MBlockInter) mb).getType()) {
            case MB_16x8:
                return new Point[] { new Point(0, 0), new Point(0, 2) }[blkIdx];
            case MB_8x16:
                return new Point[] { new Point(0, 0), new Point(2, 0) }[blkIdx];
            case MB_16x16:
                return new Point(0, 0);
            }
        }
        return null;
    }

    private Point getSubBlockCoord(SubMBType subMBType, int subBlkIdx, int baseX, int baseY) {
        switch (subMBType) {
        case L0_8x8:
            return new Point(baseX, baseY);
        case L0_8x4:
            return new Point[] { new Point(baseX, baseY), new Point(baseX, baseY + 1) }[subBlkIdx];
        case L0_4x8:
            return new Point[] { new Point(baseX, baseY), new Point(baseX + 1, baseY) }[subBlkIdx];
        case L0_4x4:
            return new Point[] { new Point(baseX, baseY), new Point(baseX + 1, baseY), new Point(baseX, baseY + 1),
                    new Point(baseX + 1, baseY + 1) }[subBlkIdx];
        }
        return null;
    }

    private MVMatrix getMvs(Macroblock mb) {
        if (mb instanceof MBlockInter8x8) {
            Inter8x8Prediction prediction = ((MBlockInter8x8) mb).getPrediction();
            return MBlockDecoderInter.calcFor8x8(prediction.getSubMbTypes(), prediction.getDecodedMVsL0());
        } else if (mb instanceof MBlockInter) {
            InterPrediction prediction = ((MBlockInter) mb).getPrediction();
            switch (((MBlockInter) mb).getType()) {
            case MB_16x8:
                return MBlockDecoderInter.calcForInter16x8(prediction.getDecodedMVsL0());
            case MB_8x16:
                return MBlockDecoderInter.calcForInter8x16(prediction.getDecodedMVsL0());
            case MB_16x16:
                return MBlockDecoderInter.calcForInter16x16(prediction.getDecodedMVsL0());
            }
        }
        return new MVMatrix(new Vector[16]);
    }

    public int readMBQpDelta() {
        // 60
        throw new UnsupportedOperationException();
    }

    public int readIntraChromaPredMode() {
        // 64
        throw new UnsupportedOperationException();
    }

    public int readPrevIntraPredModeFlag() {
        // 68
        throw new UnsupportedOperationException();
    }

    public int readRemIntraPredMode() {
        // 69
        throw new UnsupportedOperationException();
    }

    public int readMBFieldDecodingFlag() {
        // 70
        throw new UnsupportedOperationException();
    }

    public int readCBPLuma(Macroblock left, Macroblock top, Macroblock cur, int blkIdx) throws IOException {
        int blk8x8Idx = blkIdx >> 2;

        Macroblock mbLeft = sameMb8x8Left[blk8x8Idx] ? cur : left;
        int condTermLeft = mbLeft == null || (mbLeft instanceof MBlockIPCM)
                || ((MBlockWithResidual) mbLeft).getLuma()[blk8x8Left[blk8x8Idx] << 2] != null ? 0 : 1;
        Macroblock mbTop = sameMb8x8Top[blk8x8Idx] ? cur : top;
        int condTermTop = mbTop == null || (mbTop instanceof MBlockIPCM)
                || ((MBlockWithResidual) mbTop).getLuma()[blk8x8Top[blk8x8Idx] << 2] != null ? 0 : 1;
        int off = condTermLeft + 2 * condTermTop;
        return (decoder.decodeBin(contexts[73 + off]) << 3) | (decoder.decodeBin(contexts[73 + off]) << 2)
                | (decoder.decodeBin(contexts[73 + off]) << 1) | decoder.decodeBin(contexts[73 + off]);
    }

    public int readCBPChroma(Macroblock left, Macroblock top) throws IOException {
        int condTermLeft0 = left == null
                || ((left instanceof CodedMacroblock) && ((CodedMacroblock) left).getCbpChroma() == 0) ? 0 : 1;
        int condTermTop0 = top == null
                || ((top instanceof CodedMacroblock) && ((CodedMacroblock) top).getCbpChroma() == 0) ? 0 : 1;

        if (decoder.decodeBin(contexts[77 + condTermLeft0 + 2 * condTermTop0]) == 0)
            return 0;
        else {
            int condTermLeft2 = left == null
                    || ((left instanceof CodedMacroblock) && ((CodedMacroblock) left).getCbpChroma() == 2) ? 0 : 1;
            int condTermTop2 = top == null
                    || ((top instanceof CodedMacroblock) && ((CodedMacroblock) top).getCbpChroma() == 2) ? 0 : 1;
            return 1 + decoder.decodeBin(contexts[81 + condTermLeft2 + 2 * condTermTop2]);
        }
    }
}