package org.jcodec.codecs.h264.io;

import static org.jcodec.codecs.h264.H264Const.PartPred.Bi;
import static org.jcodec.codecs.h264.H264Const.PartPred.Direct;
import static org.jcodec.codecs.h264.io.CABAC.BlockType.CHROMA_AC;
import static org.jcodec.codecs.h264.io.CABAC.BlockType.CHROMA_DC;
import static org.jcodec.codecs.h264.io.CABAC.BlockType.LUMA_16_DC;
import static org.jcodec.codecs.h264.io.model.MBType.B_Direct_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;
import static org.jcodec.codecs.h264.io.model.MBType.I_NxN;
import static org.jcodec.codecs.h264.io.model.MBType.I_PCM;
import static org.jcodec.common.tools.MathUtil.clip;
import static org.jcodec.common.tools.MathUtil.sign;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.common.biari.MEncoder;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.decode.CABACContst;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author JCodec project
 * 
 */
public class CABAC {

    public enum BlockType {
        LUMA_16_DC(85, 105, 166, 277, 338, 227, 0), LUMA_15_AC(89, 120, 181, 292, 353, 237, 0), LUMA_16(93, 134, 195,
                306, 367, 247, 0), CHROMA_DC(97, 149, 210, 321, 382, 257, 1), CHROMA_AC(101, 152, 213, 324, 385, 266, 0), LUMA_64(
                1012, 402, 417, 436, 451, 426, 0), CB_16_DC(460, 484, 572, 776, 864, 952, 0), CB_15x16_AC(464, 499,
                587, 791, 879, 962, 0), CB_16(468, 513, 601, 805, 893, 972, 0), CB_64(1016, 660, 690, 675, 699, 708, 0), CR_16_DC(
                472, 528, 616, 820, 908, 982, 0), CR_15x16_AC(476, 543, 631, 835, 923, 992, 0), CR_16(480, 557, 645,
                849, 937, 1002, 0), CR_64(1020, 718, 748, 733, 757, 766, 0);

        public int codedBlockCtxOff;
        public int sigCoeffFlagCtxOff;
        public int lastSigCoeffCtxOff;
        public int sigCoeffFlagFldCtxOff;
        public int lastSigCoeffFldCtxOff;
        public int coeffAbsLevelCtxOff;
        public int coeffAbsLevelAdjust;

        private BlockType(int codecBlockCtxOff, int sigCoeffCtxOff, int lastSigCoeffCtxOff, int sigCoeffFlagFldCtxOff,
                int lastSigCoeffFldCtxOff, int coeffAbsLevelCtxOff, int coeffAbsLevelAdjust) {
            this.codedBlockCtxOff = codecBlockCtxOff;
            this.sigCoeffFlagCtxOff = sigCoeffCtxOff;
            this.lastSigCoeffCtxOff = lastSigCoeffCtxOff;
            this.sigCoeffFlagFldCtxOff = sigCoeffFlagFldCtxOff;
            this.lastSigCoeffFldCtxOff = sigCoeffFlagFldCtxOff;
            this.coeffAbsLevelCtxOff = coeffAbsLevelCtxOff;
            this.coeffAbsLevelAdjust = coeffAbsLevelAdjust;
        }
    }

    private int chromaPredModeLeft;
    private int[] chromaPredModeTop;
    private int prevMbQpDelta;
    private int prevCBP;

    private int[][] codedBlkLeft;
    private int[][] codedBlkTop;

    private int[] codedBlkDCLeft;
    private int[][] codedBlkDCTop;

    private int[][] refIdxLeft;
    private int[][] refIdxTop;

    private boolean skipFlagLeft;
    private boolean[] skipFlagsTop;

    private int[][][] mvdTop;
    private int[][][] mvdLeft;

    public int[] tmp;

    public CABAC(int mbWidth) {
        this.tmp = new int[16];
        this.chromaPredModeLeft = 0;
        this.chromaPredModeTop = new int[mbWidth];
        this.codedBlkLeft = new int[][] { new int[4], new int[2], new int[2] };
        this.codedBlkTop = new int[][] { new int[mbWidth << 2], new int[mbWidth << 1], new int[mbWidth << 1] };

        this.codedBlkDCLeft = new int[3];
        this.codedBlkDCTop = new int[3][mbWidth];

        this.refIdxLeft = new int[2][4];
        this.refIdxTop = new int[2][mbWidth << 2];

        this.skipFlagsTop = new boolean[mbWidth];

        this.mvdTop = new int[2][2][mbWidth << 2];
        this.mvdLeft = new int[2][2][4];
    }

    public int readCoeffs(MDecoder decoder, BlockType blockType, int[] out, int first, int num, int[] reorder,
            int[] scMapping, int[] lscMapping) {
        boolean sigCoeff[] = new boolean[num];
        int numCoeff;
        for (numCoeff = 0; numCoeff < num - 1; numCoeff++) {
            sigCoeff[numCoeff] = decoder.decodeBin(blockType.sigCoeffFlagCtxOff + scMapping[numCoeff]) == 1;
            if (sigCoeff[numCoeff] && decoder.decodeBin(blockType.lastSigCoeffCtxOff + lscMapping[numCoeff]) == 1)
                break;
        }
        sigCoeff[numCoeff++] = true;

        int numGt1 = 0, numEq1 = 0;
        for (int j = numCoeff - 1; j >= 0; j--) {
            if (!sigCoeff[j])
                continue;
            int absLev = readCoeffAbsLevel(decoder, blockType, numGt1, numEq1);
            if (absLev == 0)
                ++numEq1;
            else
                ++numGt1;
            out[reorder[j + first]] = MathUtil.toSigned(absLev + 1, -decoder.decodeBinBypass());
        }
        // System.out.print("[");
        // for (int i = 0; i < out.length; i++)
        // System.out.print(out[i] + ",");
        // System.out.println("]");

        return numGt1 + numEq1;
    }

    private int readCoeffAbsLevel(MDecoder decoder, BlockType blockType, int numDecodAbsLevelGt1,
            int numDecodAbsLevelEq1) {
        int incB0 = ((numDecodAbsLevelGt1 != 0) ? 0 : Math.min(4, 1 + numDecodAbsLevelEq1));
        int incBN = 5 + Math.min(4 - blockType.coeffAbsLevelAdjust, numDecodAbsLevelGt1);

        int val, b = decoder.decodeBin(blockType.coeffAbsLevelCtxOff + incB0);
        for (val = 0; b != 0 && val < 13; val++)
            b = decoder.decodeBin(blockType.coeffAbsLevelCtxOff + incBN);
        val += b;

        if (val == 14) {
            int log = -2, add = 0, sum = 0;
            do {
                log++;
                b = decoder.decodeBinBypass();
            } while (b != 0);

            for (; log >= 0; log--) {
                add |= decoder.decodeBinBypass() << log;
                sum += 1 << log;
            }

            val += add + sum;
        }

        return val;
    }

    public void writeCoeffs(MEncoder encoder, BlockType blockType, int[] _out, int first, int num, int[] reorder) {

        for (int i = 0; i < num; i++)
            tmp[i] = _out[reorder[first + i]];

        int numCoeff = 0;
        for (int i = 0; i < num; i++) {
            if (tmp[i] != 0)
                numCoeff = i + 1;
        }
        for (int i = 0; i < Math.min(numCoeff, num - 1); i++) {
            if (tmp[i] != 0) {
                encoder.encodeBin(blockType.sigCoeffFlagCtxOff + i, 1);
                encoder.encodeBin(blockType.lastSigCoeffCtxOff + i, i == numCoeff - 1 ? 1 : 0);
            } else {
                encoder.encodeBin(blockType.sigCoeffFlagCtxOff + i, 0);
            }
        }

        int numGt1 = 0, numEq1 = 0;
        for (int j = numCoeff - 1; j >= 0; j--) {
            if (tmp[j] == 0)
                continue;
            int absLev = MathUtil.abs(tmp[j]) - 1;
            writeCoeffAbsLevel(encoder, blockType, numGt1, numEq1, absLev);
            if (absLev == 0)
                ++numEq1;
            else
                ++numGt1;
            encoder.encodeBinBypass(sign(tmp[j]));
        }
    }

    private void writeCoeffAbsLevel(MEncoder encoder, BlockType blockType, int numDecodAbsLevelGt1,
            int numDecodAbsLevelEq1, int absLev) {
        int incB0 = ((numDecodAbsLevelGt1 != 0) ? 0 : Math.min(4, 1 + numDecodAbsLevelEq1));
        int incBN = 5 + Math.min(4 - blockType.coeffAbsLevelAdjust, numDecodAbsLevelGt1);

        if (absLev == 0) {
            encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incB0, 0);
        } else {
            encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incB0, 1);
            if (absLev < 14) {
                for (int i = 1; i < absLev; i++)
                    encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incBN, 1);
                encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incBN, 0);
            } else {
                for (int i = 1; i < 14; i++)
                    encoder.encodeBin(blockType.coeffAbsLevelCtxOff + incBN, 1);
                absLev -= 14;
                int sufLen, pow;
                for (sufLen = 0, pow = 1; absLev >= pow; sufLen++, pow = (1 << sufLen)) {
                    encoder.encodeBinBypass(1);
                    absLev -= pow;
                }
                encoder.encodeBinBypass(0);
                for (sufLen--; sufLen >= 0; sufLen--)
                    encoder.encodeBinBypass((absLev >> sufLen) & 1);
            }
        }
    }

    public void initModels(int[][] cm, SliceType sliceType, int cabacIdc, int sliceQp) {
        // System.out.println("INIT slice qp: "+sliceQp+", cabac init ids: "+cabacIdc+", slicetype : "
        // + sliceType);
        int[] tabA = sliceType.isIntra() ? CABACContst.cabac_context_init_I_A
                : CABACContst.cabac_context_init_PB_A[cabacIdc];
        int[] tabB = sliceType.isIntra() ? CABACContst.cabac_context_init_I_B
                : CABACContst.cabac_context_init_PB_B[cabacIdc];

        for (int i = 0; i < 1024; i++) {
            int preCtxState = clip(((tabA[i] * clip(sliceQp, 0, 51)) >> 4) + tabB[i], 1, 126);
            if (preCtxState <= 63) {
                cm[0][i] = 63 - preCtxState;
                cm[1][i] = 0;
            } else {
                cm[0][i] = preCtxState - 64;
                cm[1][i] = 1;
            }
        }
    }

    public int readMBTypeI(MDecoder decoder, MBType left, MBType top, boolean leftAvailable, boolean topAvailable) {
        int ctx = 3;
        ctx += !leftAvailable || left == I_NxN ? 0 : 1;
        ctx += !topAvailable || top == I_NxN ? 0 : 1;

        if (decoder.decodeBin(ctx) == 0) {
            return 0;
        } else {
            return decoder.decodeFinalBin() == 1 ? 25 : 1 + readMBType16x16(decoder);
        }
    }

    private int readMBType16x16(MDecoder decoder) {
        int type = decoder.decodeBin(6) * 12;
        if (decoder.decodeBin(7) == 0) {
            return type + (decoder.decodeBin(9) << 1) + decoder.decodeBin(10);
        } else {
            return type + (decoder.decodeBin(8) << 2) + (decoder.decodeBin(9) << 1) + decoder.decodeBin(10) + 4;
        }
    }

    public int readMBTypeP(MDecoder decoder) {

        if (decoder.decodeBin(14) == 1) {
            return 5 + readIntraP(decoder, 17);
        } else {
            if (decoder.decodeBin(15) == 0) {
                return decoder.decodeBin(16) == 0 ? 0 : 3;
            } else {
                return decoder.decodeBin(17) == 0 ? 2 : 1;
            }
        }
    }

    private int readIntraP(MDecoder decoder, int ctxOff) {
        if (decoder.decodeBin(ctxOff) == 0) {
            return 0;
        } else {
            return decoder.decodeFinalBin() == 1 ? 25 : 1 + readMBType16x16P(decoder, ctxOff);
        }
    }

    private int readMBType16x16P(MDecoder decoder, int ctxOff) {
        ctxOff++;
        int type = decoder.decodeBin(ctxOff) * 12;
        ctxOff++;
        if (decoder.decodeBin(ctxOff) == 0) {
            ctxOff++;
            return type + (decoder.decodeBin(ctxOff) << 1) + decoder.decodeBin(ctxOff);
        } else {
            return type + (decoder.decodeBin(ctxOff) << 2) + (decoder.decodeBin(ctxOff + 1) << 1)
                    + decoder.decodeBin(ctxOff + 1) + 4;
        }
    }

    public int readMBTypeB(MDecoder mDecoder, MBType left, MBType top, boolean leftAvailable, boolean topAvailable) {
        int ctx = 27;
        ctx += !leftAvailable || left == null || left == B_Direct_16x16 ? 0 : 1;
        ctx += !topAvailable || top == null || top == B_Direct_16x16 ? 0 : 1;

        if (mDecoder.decodeBin(ctx) == 0)
            return 0; // B Direct
        if (mDecoder.decodeBin(30) == 0)
            return 1 + mDecoder.decodeBin(32);

        int b1 = mDecoder.decodeBin(31);
        if (b1 == 0) {
            return 3 + ((mDecoder.decodeBin(32) << 2) | (mDecoder.decodeBin(32) << 1) | mDecoder.decodeBin(32));
        } else {
            if (mDecoder.decodeBin(32) == 0) {
                return 12 + ((mDecoder.decodeBin(32) << 2) | (mDecoder.decodeBin(32) << 1) | mDecoder.decodeBin(32));
            } else {
                switch ((mDecoder.decodeBin(32) << 1) + mDecoder.decodeBin(32)) {
                case 0:
                    return 20 + mDecoder.decodeBin(32);
                case 1:
                    return 23 + readIntraP(mDecoder, 32);
                case 2:
                    return 11;
                case 3:
                    return 22;
                }
            }
        }

        return 0;
    }

    public void writeMBTypeI(MEncoder encoder, MBType left, MBType top, boolean leftAvailable, boolean topAvailable,
            int mbType) {
        int ctx = 3;
        ctx += !leftAvailable || left == I_NxN ? 0 : 1;
        ctx += !topAvailable || top == I_NxN ? 0 : 1;

        if (mbType == 0)
            encoder.encodeBin(ctx, 0);
        else {
            encoder.encodeBin(ctx, 1);
            if (mbType == 25)
                encoder.encodeBinFinal(1);
            else {
                encoder.encodeBinFinal(0);
                writeMBType16x16(encoder, mbType - 1);
            }
        }
    }

    private void writeMBType16x16(MEncoder encoder, int mbType) {
        if (mbType < 12) {
            encoder.encodeBin(6, 0);
        } else {
            encoder.encodeBin(6, 1);
            mbType -= 12;
        }
        if (mbType < 4) {
            encoder.encodeBin(7, 0);
            encoder.encodeBin(9, mbType >> 1);
            encoder.encodeBin(10, mbType & 1);
        } else {
            mbType -= 4;
            encoder.encodeBin(7, 1);
            encoder.encodeBin(8, mbType >> 2);
            encoder.encodeBin(9, (mbType >> 1) & 1);
            encoder.encodeBin(10, mbType & 1);
        }
    }

    public int readMBQpDelta(MDecoder decoder, MBType prevMbType) {
        int ctx = 60;
        ctx += prevMbType == null || prevMbType == I_PCM || (prevMbType != I_16x16 && prevCBP == 0)
                || prevMbQpDelta == 0 ? 0 : 1;

        int val = 0;
        if (decoder.decodeBin(ctx) == 1) {
            val++;
            if (decoder.decodeBin(62) == 1) {
                val++;
                while (decoder.decodeBin(63) == 1)
                    val++;
            }
        }
        prevMbQpDelta = H264Utils.golomb2Signed(val);

        return prevMbQpDelta;
    }

    public void writeMBQpDelta(MEncoder encoder, MBType prevMbType, int mbQpDelta) {
        int ctx = 60;
        ctx += prevMbType == null || prevMbType == I_PCM || (prevMbType != I_16x16 && prevCBP == 0)
                || prevMbQpDelta == 0 ? 0 : 1;

        prevMbQpDelta = mbQpDelta;
        if (mbQpDelta-- == 0)
            encoder.encodeBin(ctx, 0);
        else {
            encoder.encodeBin(ctx, 1);
            if (mbQpDelta-- == 0)
                encoder.encodeBin(62, 0);
            else {
                while (mbQpDelta-- > 0)
                    encoder.encodeBin(63, 1);
                encoder.encodeBin(63, 0);
            }
        }
    }

    public int readIntraChromaPredMode(MDecoder decoder, int mbX, MBType left, MBType top, boolean leftAvailable,
            boolean topAvailable) {
        int ctx = 64;
        ctx += !leftAvailable || left == null || !left.isIntra() || chromaPredModeLeft == 0 ? 0 : 1;
        ctx += !topAvailable || top == null || !top.isIntra() || chromaPredModeTop[mbX] == 0 ? 0 : 1;
        int mode;
        if (decoder.decodeBin(ctx) == 0)
            mode = 0;
        else if (decoder.decodeBin(67) == 0)
            mode = 1;
        else if (decoder.decodeBin(67) == 0)
            mode = 2;
        else
            mode = 3;
        chromaPredModeLeft = chromaPredModeTop[mbX] = mode;

        return mode;
    }

    public void writeIntraChromaPredMode(MEncoder encoder, int mbX, MBType left, MBType top, boolean leftAvailable,
            boolean topAvailable, int mode) {
        int ctx = 64;
        ctx += !leftAvailable || !left.isIntra() || chromaPredModeLeft == 0 ? 0 : 1;
        ctx += !topAvailable || !top.isIntra() || chromaPredModeTop[mbX] == 0 ? 0 : 1;
        encoder.encodeBin(ctx, mode-- == 0 ? 0 : 1);
        for (int i = 0; mode >= 0 && i < 2; i++)
            encoder.encodeBin(67, mode-- == 0 ? 0 : 1);
        chromaPredModeLeft = chromaPredModeTop[mbX] = mode;
    }

    public int condTerm(MBType mbCur, boolean nAvb, MBType mbN, boolean nBlkAvb, int cbpN) {
        if (!nAvb)
            return mbCur.isIntra() ? 1 : 0;
        if (mbN == I_PCM)
            return 1;
        if (!nBlkAvb)
            return 0;
        return cbpN;
    }

    public int readCodedBlockFlagLumaDC(MDecoder decoder, int mbX, MBType left, MBType top, boolean leftAvailable,
            boolean topAvailable, MBType cur) {
        int tLeft = condTerm(cur, leftAvailable, left, left == I_16x16, codedBlkDCLeft[0]);
        int tTop = condTerm(cur, topAvailable, top, top == I_16x16, codedBlkDCTop[0][mbX]);

        int decoded = decoder.decodeBin(LUMA_16_DC.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkDCLeft[0] = decoded;
        codedBlkDCTop[0][mbX] = decoded;

        return decoded;
    }

    public int readCodedBlockFlagChromaDC(MDecoder decoder, int mbX, int comp, MBType left, MBType top,
            boolean leftAvailable, boolean topAvailable, int leftCBPChroma, int topCBPChroma, MBType cur) {
        int tLeft = condTerm(cur, leftAvailable, left, left != null && leftCBPChroma != 0, codedBlkDCLeft[comp]);
        int tTop = condTerm(cur, topAvailable, top, top != null && topCBPChroma != 0, codedBlkDCTop[comp][mbX]);

        int decoded = decoder.decodeBin(CHROMA_DC.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkDCLeft[comp] = decoded;
        codedBlkDCTop[comp][mbX] = decoded;

        return decoded;
    }

    public int readCodedBlockFlagLumaAC(MDecoder decoder, BlockType blkType, int blkX, int blkY, int comp, MBType left,
            MBType top, boolean leftAvailable, boolean topAvailable, int leftCBPLuma, int topCBPLuma, int curCBPLuma,
            MBType cur) {
        int blkOffLeft = blkX & 3, blkOffTop = blkY & 3;

        int tLeft;
        if (blkOffLeft == 0)
            tLeft = condTerm(cur, leftAvailable, left, left != null && left != I_PCM && cbp(leftCBPLuma, 3, blkOffTop),
                    codedBlkLeft[comp][blkOffTop]);
        else
            tLeft = condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft - 1, blkOffTop), codedBlkLeft[comp][blkOffTop]);

        int tTop;
        if (blkOffTop == 0)
            tTop = condTerm(cur, topAvailable, top, top != null && top != I_PCM && cbp(topCBPLuma, blkOffLeft, 3),
                    codedBlkTop[comp][blkX]);
        else
            tTop = condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft, blkOffTop - 1), codedBlkTop[comp][blkX]);

        int decoded = decoder.decodeBin(blkType.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkLeft[comp][blkOffTop] = decoded;
        codedBlkTop[comp][blkX] = decoded;

        return decoded;
    }

    public int readCodedBlockFlagLuma64(MDecoder decoder, int blkX, int blkY, int comp, MBType left, MBType top,
            boolean leftAvailable, boolean topAvailable, int leftCBPLuma, int topCBPLuma, int curCBPLuma, MBType cur,
            boolean is8x8Left, boolean is8x8Top) {

        int blkOffLeft = blkX & 3, blkOffTop = blkY & 3;

        int tLeft;
        if (blkOffLeft == 0)
            tLeft = condTerm(cur, leftAvailable, left,
                    left != null && left != I_PCM && is8x8Left && cbp(leftCBPLuma, 3, blkOffTop),
                    codedBlkLeft[comp][blkOffTop]);
        else
            tLeft = condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft - 1, blkOffTop), codedBlkLeft[comp][blkOffTop]);

        int tTop;
        if (blkOffTop == 0)
            tTop = condTerm(cur, topAvailable, top,
                    top != null && top != I_PCM && is8x8Top && cbp(topCBPLuma, blkOffLeft, 3), codedBlkTop[comp][blkX]);
        else
            tTop = condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft, blkOffTop - 1), codedBlkTop[comp][blkX]);

        int decoded = decoder.decodeBin(BlockType.LUMA_64.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkLeft[comp][blkOffTop] = decoded;
        codedBlkTop[comp][blkX] = decoded;

        return decoded;
    }

    private boolean cbp(int cbpLuma, int blkX, int blkY) {
        int x8x8 = (blkY & 2) + (blkX >> 1);

        return ((cbpLuma >> x8x8) & 1) == 1;
    }

    public int readCodedBlockFlagChromaAC(MDecoder decoder, int blkX, int blkY, int comp, MBType left, MBType top,
            boolean leftAvailable, boolean topAvailable, int leftCBPChroma, int topCBPChroma, MBType cur) {
        int blkOffLeft = blkX & 1, blkOffTop = blkY & 1;

        int tLeft;
        if (blkOffLeft == 0)
            tLeft = condTerm(cur, leftAvailable, left, left != null && left != I_PCM && (leftCBPChroma & 2) != 0,
                    codedBlkLeft[comp][blkOffTop]);
        else
            tLeft = condTerm(cur, true, cur, true, codedBlkLeft[comp][blkOffTop]);
        int tTop;
        if (blkOffTop == 0)
            tTop = condTerm(cur, topAvailable, top, top != null && top != I_PCM && (topCBPChroma & 2) != 0,
                    codedBlkTop[comp][blkX]);
        else
            tTop = condTerm(cur, true, cur, true, codedBlkTop[comp][blkX]);

        int decoded = decoder.decodeBin(CHROMA_AC.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkLeft[comp][blkOffTop] = decoded;
        codedBlkTop[comp][blkX] = decoded;

        return decoded;
    }

    public boolean prev4x4PredModeFlag(MDecoder decoder) {
        return decoder.decodeBin(68) == 1;
    }

    public int rem4x4PredMode(MDecoder decoder) {
        return decoder.decodeBin(69) | (decoder.decodeBin(69) << 1) | (decoder.decodeBin(69) << 2);
    }

    public int codedBlockPatternIntra(MDecoder mDecoder, boolean leftAvailable, boolean topAvailable, int cbpLeft,
            int cbpTop, MBType mbLeft, MBType mbTop) {
        int cbp0 = mDecoder.decodeBin(73 + condTerm(leftAvailable, mbLeft, (cbpLeft >> 1) & 1) + 2
                * condTerm(topAvailable, mbTop, (cbpTop >> 2) & 1));
        int cbp1 = mDecoder.decodeBin(73 + (1 - cbp0) + 2 * condTerm(topAvailable, mbTop, (cbpTop >> 3) & 1));
        int cbp2 = mDecoder.decodeBin(73 + condTerm(leftAvailable, mbLeft, (cbpLeft >> 3) & 1) + 2 * (1 - cbp0));
        int cbp3 = mDecoder.decodeBin(73 + (1 - cbp2) + 2 * (1 - cbp1));

        int cr0 = mDecoder.decodeBin(77 + condTermCr0(leftAvailable, mbLeft, cbpLeft >> 4) + 2
                * condTermCr0(topAvailable, mbTop, cbpTop >> 4));
        int cr1 = cr0 != 0 ? mDecoder.decodeBin(81 + condTermCr1(leftAvailable, mbLeft, cbpLeft >> 4) + 2
                * condTermCr1(topAvailable, mbTop, cbpTop >> 4)) : 0;

        return cbp0 | (cbp1 << 1) | (cbp2 << 2) | (cbp3 << 3) | (cr0 << 4) | (cr1 << 5);
    }

    private int condTermCr0(boolean avb, MBType mbt, int cbpChroma) {
        return avb && (mbt == I_PCM || mbt != null && cbpChroma != 0) ? 1 : 0;
    }

    private int condTermCr1(boolean avb, MBType mbt, int cbpChroma) {
        return avb && (mbt == I_PCM || mbt != null && (cbpChroma & 2) != 0) ? 1 : 0;
    }

    private int condTerm(boolean avb, MBType mbt, int cbp) {
        return !avb || mbt == I_PCM || (mbt != null && cbp == 1) ? 0 : 1;
    }

    public void setPrevCBP(int prevCBP) {
        this.prevCBP = prevCBP;
    }

    public int readMVD(MDecoder decoder, int comp, boolean leftAvailable, boolean topAvailable, MBType leftType,
            MBType topType, PartPred leftPred, PartPred topPred, PartPred curPred, int mbX, int partX, int partY,
            int partW, int partH, int list) {
        int ctx = comp == 0 ? 40 : 47;

        int partAbsX = (mbX << 2) + partX;

        boolean predEqA = leftPred != null && leftPred != Direct
                && (leftPred == Bi || leftPred == curPred || (curPred == Bi && leftPred.usesList(list)));
        boolean predEqB = topPred != null && topPred != Direct
                && (topPred == Bi || topPred == curPred || (curPred == Bi && topPred.usesList(list)));

        // prefix and suffix as given by UEG3 with signedValFlag=1, uCoff=9
        int absMvdComp = !leftAvailable || leftType == null || leftType.isIntra() || !predEqA ? 0 : Math
                .abs(mvdLeft[list][comp][partY]);
        absMvdComp += !topAvailable || topType == null || topType.isIntra() || !predEqB ? 0 : Math
                .abs(mvdTop[list][comp][partAbsX]);

        int val, b = decoder.decodeBin(ctx + (absMvdComp < 3 ? 0 : (absMvdComp > 32 ? 2 : 1)));
        for (val = 0; b != 0 && val < 8; val++)
            b = decoder.decodeBin(Math.min(ctx + val + 3, ctx + 6));
        val += b;

        if (val != 0) {
            if (val == 9) {
                int log = 2, add = 0, sum = 0, leftover = 0;
                do {
                    sum += leftover;
                    log++;
                    b = decoder.decodeBinBypass();
                    leftover = 1 << log;
                } while (b != 0);

                --log;

                for (; log >= 0; log--) {
                    add |= decoder.decodeBinBypass() << log;
                }
                val += add + sum;
            }

            val = MathUtil.toSigned(val, -decoder.decodeBinBypass());
        }

        for (int i = 0; i < partW; i++) {
            mvdTop[list][comp][partAbsX + i] = val;
        }
        for (int i = 0; i < partH; i++) {
            mvdLeft[list][comp][partY + i] = val;
        }

        return val;
    }

    public int readRefIdx(MDecoder mDecoder, boolean leftAvailable, boolean topAvailable, MBType leftType,
            MBType topType, PartPred leftPred, PartPred topPred, PartPred curPred, int mbX, int partX, int partY,
            int partW, int partH, int list) {
        int partAbsX = (mbX << 2) + partX;

        boolean predEqA = leftPred != null && leftPred != Direct
                && (leftPred == Bi || leftPred == curPred || (curPred == Bi && leftPred.usesList(list)));
        boolean predEqB = topPred != null && topPred != Direct
                && (topPred == Bi || topPred == curPred || (curPred == Bi && topPred.usesList(list)));

        int ctA = !leftAvailable || leftType == null || leftType.isIntra() || !predEqA || refIdxLeft[list][partY] == 0 ? 0
                : 1;
        int ctB = !topAvailable || topType == null || topType.isIntra() || !predEqB || refIdxTop[list][partAbsX] == 0 ? 0
                : 1;
        int b0 = mDecoder.decodeBin(54 + ctA + 2 * ctB);
        int val;
        if (b0 == 0)
            val = 0;
        else {
            int b1 = mDecoder.decodeBin(58);
            if (b1 == 0)
                val = 1;
            else {
                for (val = 2; mDecoder.decodeBin(59) == 1; val++)
                    ;
            }
        }

        for (int i = 0; i < partW; i++) {
            refIdxTop[list][partAbsX + i] = val;
        }

        for (int i = 0; i < partH; i++) {
            refIdxLeft[list][partY + i] = val;
        }

        return val;
    }

    public boolean readMBSkipFlag(MDecoder mDecoder, SliceType slType, boolean leftAvailable, boolean topAvailable,
            int mbX) {
        int base = slType == SliceType.P ? 11 : 24;

        boolean ret = mDecoder.decodeBin(base + (leftAvailable && !skipFlagLeft ? 1 : 0)
                + (topAvailable && !skipFlagsTop[mbX] ? 1 : 0)) == 1;

        skipFlagLeft = skipFlagsTop[mbX] = ret;

        return ret;
    }

    public int readSubMbTypeP(MDecoder mDecoder) {
        if (mDecoder.decodeBin(21) == 1)
            return 0;
        else if (mDecoder.decodeBin(22) == 0)
            return 1;
        else if (mDecoder.decodeBin(23) == 1)
            return 2;
        else
            return 3;
    }

    public int readSubMbTypeB(MDecoder mDecoder) {
        if (mDecoder.decodeBin(36) == 0)
            return 0; // direct
        if (mDecoder.decodeBin(37) == 0)
            return 1 + mDecoder.decodeBin(39);
        if (mDecoder.decodeBin(38) == 0)
            return 3 + (mDecoder.decodeBin(39) << 1) + mDecoder.decodeBin(39);

        if (mDecoder.decodeBin(39) == 0)
            return 7 + (mDecoder.decodeBin(39) << 1) + mDecoder.decodeBin(39);

        return 11 + mDecoder.decodeBin(39);
    }

    public boolean readTransform8x8Flag(MDecoder mDecoder, boolean leftAvailable, boolean topAvailable,
            MBType leftType, MBType topType, boolean is8x8Left, boolean is8x8Top) {
        int ctx = 399 + (leftAvailable && leftType != null && is8x8Left ? 1 : 0)
                + (topAvailable && topType != null && is8x8Top ? 1 : 0);
        return mDecoder.decodeBin(ctx) == 1;
    }

    public void setCodedBlock(int blkX, int blkY) {
        codedBlkLeft[0][blkY & 0x3] = codedBlkTop[0][blkX] = 1;
    }
}