package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.BLK8x8_BLOCKS;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_MB_OFF_CHROMA;
import static org.jcodec.codecs.h264.H264Const.BLK_INV_MAP;
import static org.jcodec.codecs.h264.H264Const.QP_SCALE_CR;
import static org.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;
import static org.jcodec.common.model.ColorSpace.MONO;

import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Base macroblock decoder that contains routines shared by many decoders
 * 
 * @author The JCodec project
 * 
 */
public class MBlockDecoderBase {
    protected DecoderState s;
    protected BitstreamParser parser;
    protected SliceHeader sh;
    protected DeblockerInput di;
    protected int poc;

    public MBlockDecoderBase(BitstreamParser parser, SliceHeader sh, DeblockerInput di, int poc,
            DecoderState decoderState) {
        this.s = decoderState;
        this.parser = parser;
        this.sh = sh;
        this.di = di;
        this.poc = poc;
    }

    void residualLuma(boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int codedBlockPattern,
            MBType mbType, boolean transform8x8Used, boolean is8x8Left, boolean is8x8Top, int[][] residualOut) {
        if (!transform8x8Used)
            residualLuma(leftAvailable, topAvailable, mbX, mbY, codedBlockPattern, mbType, residualOut);
        else if (sh.pps.entropy_coding_mode_flag)
            residualLuma8x8CABAC(leftAvailable, topAvailable, mbX, mbY, codedBlockPattern, mbType, is8x8Left, is8x8Top,
                    residualOut);
        else
            residualLuma8x8CAVLC(leftAvailable, topAvailable, mbX, mbY, codedBlockPattern, mbType, residualOut);
    }

    private void residualLuma(boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int codedBlockPattern,
            MBType curMbType, int[][] residualOut) {

        int cbpLuma = codedBlockPattern & 0xf;

        for (int i = 0; i < 16; i++) {
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((cbpLuma & (1 << (i >> 2))) == 0) {
                if (!sh.pps.entropy_coding_mode_flag)
                    parser.setZeroCoeff(0, blkX, blkOffTop);
                continue;
            }
            int[] ac = residualOut[i];

            parser.readResidualAC(leftAvailable, topAvailable, mbX, curMbType, cbpLuma, blkOffLeft, blkOffTop, blkX,
                    blkY, ac);

            CoeffTransformer.dequantizeAC(ac, s.qp);
            CoeffTransformer.idct4x4(ac);
        }

        parser.savePrevCBP(codedBlockPattern);
    }

    private void residualLuma8x8CABAC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            int codedBlockPattern, MBType curMbType, boolean is8x8Left, boolean is8x8Top, int[][] residualOut) {

        int cbpLuma = codedBlockPattern & 0xf;

        for (int i = 0; i < 4; i++) {
            int blkOffLeft = (i & 1) << 1;
            int blkOffTop = i & 2;
            int blkX = (mbX << 2) + blkOffLeft;
            int blkY = (mbY << 2) + blkOffTop;

            if ((cbpLuma & (1 << i)) == 0) {
                continue;
            }
            int[] ac = residualOut[i];

            parser.readLumaAC8x8(blkX, blkY, ac);

            CoeffTransformer.dequantizeAC8x8(ac, s.qp);
            CoeffTransformer.idct8x8(ac);
        }

        parser.savePrevCBP(codedBlockPattern);
    }

    private void residualLuma8x8CAVLC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY,
            int codedBlockPattern, MBType curMbType, int[][] residualOut) {

        int cbpLuma = codedBlockPattern & 0xf;

        for (int i = 0; i < 4; i++) {
            int blk8x8OffLeft = (i & 1) << 1;
            int blk8x8OffTop = i & 2;
            int blkX = (mbX << 2) + blk8x8OffLeft;
            int blkY = (mbY << 2) + blk8x8OffTop;

            if ((cbpLuma & (1 << i)) == 0) {
                parser.setZeroCoeff(0, blkX, blk8x8OffTop);
                parser.setZeroCoeff(0, blkX + 1, blk8x8OffTop);
                parser.setZeroCoeff(0, blkX, blk8x8OffTop + 1);
                parser.setZeroCoeff(0, blkX + 1, blk8x8OffTop + 1);
                continue;
            }
            int[] ac64 = residualOut[i];
            int coeffs = 0;
            for (int j = 0; j < 4; j++) {
                int[] ac16 = new int[16];
                int blkOffLeft = blk8x8OffLeft + (j & 1);
                int blkOffTop = blk8x8OffTop + (j >> 1);
                coeffs += parser.readLumaAC(leftAvailable, topAvailable, mbX, curMbType, blkX, j, ac16, blkOffLeft,
                        blkOffTop);
                for (int k = 0; k < 16; k++)
                    ac64[CoeffTransformer.zigzag8x8[(k << 2) + j]] = ac16[k];
            }
            di.nCoeff[blkY][blkX] = di.nCoeff[blkY][blkX + 1] = di.nCoeff[blkY + 1][blkX] = di.nCoeff[blkY + 1][blkX + 1] = coeffs;

            CoeffTransformer.dequantizeAC8x8(ac64, s.qp);
            CoeffTransformer.idct8x8(ac64);
        }
    }

    public void decodeChroma(int pattern, int chromaMode, int mbX, int mbY, boolean leftAvailable,
            boolean topAvailable, Picture8Bit mb, int qp, MBType curMbType) {

        if (s.chromaFormat == MONO) {
            Arrays.fill(mb.getPlaneData(1), (byte) 0);
            Arrays.fill(mb.getPlaneData(2), (byte) 0);
            return;
        }

        int[][] residualCb = new int[4][16];
        int[][] residualCr = new int[4][16];
        int qp1 = calcQpChroma(qp, s.chromaQpOffset[0]);
        int qp2 = calcQpChroma(qp, s.chromaQpOffset[1]);
        if (pattern != 0) {
            decodeChromaResidual(leftAvailable, topAvailable, mbX, mbY, pattern, qp1, qp2, curMbType, residualCb,
                    residualCr);
        } else if (!sh.pps.entropy_coding_mode_flag) {
            parser.setZeroCoeff(1, mbX << 1, 0);
            parser.setZeroCoeff(1, (mbX << 1) + 1, 1);
            parser.setZeroCoeff(2, mbX << 1, 0);
            parser.setZeroCoeff(2, (mbX << 1) + 1, 1);
        }
        int addr = mbY * (sh.sps.pic_width_in_mbs_minus1 + 1) + mbX;
        di.mbQps[1][addr] = qp1;
        di.mbQps[2][addr] = qp2;
        ChromaPredictionBuilder.predictWithMode(residualCb, chromaMode, mbX, leftAvailable, topAvailable, s.leftRow[1],
                s.topLine[1], s.topLeft[1], mb.getPlaneData(1));
        ChromaPredictionBuilder.predictWithMode(residualCr, chromaMode, mbX, leftAvailable, topAvailable, s.leftRow[2],
                s.topLine[2], s.topLeft[2], mb.getPlaneData(2));
    }

    void decodeChromaResidual(boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int pattern, int crQp1,
            int crQp2, MBType curMbType, int[][] residualCbOut, int[][] residualCrOut) {
        int[] dc1 = new int[(16 >> s.chromaFormat.compWidth[1]) >> s.chromaFormat.compHeight[1]];
        int[] dc2 = new int[(16 >> s.chromaFormat.compWidth[2]) >> s.chromaFormat.compHeight[2]];
        if ((pattern & 3) > 0) {
            chromaDC(mbX, leftAvailable, topAvailable, dc1, 1, crQp1, curMbType);
            chromaDC(mbX, leftAvailable, topAvailable, dc2, 2, crQp2, curMbType);
        }
        chromaAC(leftAvailable, topAvailable, mbX, mbY, dc1, 1, crQp1, curMbType, (pattern & 2) > 0, residualCbOut);
        chromaAC(leftAvailable, topAvailable, mbX, mbY, dc2, 2, crQp2, curMbType, (pattern & 2) > 0, residualCrOut);
    }

    private void chromaDC(int mbX, boolean leftAvailable, boolean topAvailable, int[] dc, int comp, int crQp,
            MBType curMbType) {
        parser.readChromaDC(mbX, leftAvailable, topAvailable, dc, comp, curMbType);

        CoeffTransformer.invDC2x2(dc);
        CoeffTransformer.dequantizeDC2x2(dc, crQp);
    }

    private void chromaAC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int[] dc, int comp, int crQp,
            MBType curMbType, boolean codedAC, int[][] residualOut) {
        for (int i = 0; i < dc.length; i++) {
            int[] ac = residualOut[i];
            int blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i];
            int blkOffTop = H264Const.MB_BLK_OFF_TOP[i];

            int blkX = (mbX << 1) + blkOffLeft;
            // int blkY = (mbY << 1) + blkOffTop;

            if (codedAC) {
                parser.readChromaAC(leftAvailable, topAvailable, mbX, comp, curMbType, ac, blkOffLeft, blkOffTop, blkX);
                CoeffTransformer.dequantizeAC(ac, crQp);
            } else {
                if (!sh.pps.entropy_coding_mode_flag)
                    parser.setZeroCoeff(comp, blkX, blkOffTop);
            }
            ac[0] = dc[i];
            CoeffTransformer.idct4x4(ac);
        }
    }

    int calcQpChroma(int qp, int crQpOffset) {
        return QP_SCALE_CR[MathUtil.clip(qp + crQpOffset, 0, 51)];
    }

    public void decodeChromaInter(BitReader reader, int pattern, Frame[][] refs, int[][][] x, PartPred[] predType,
            boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int mbAddr, int qp, Picture8Bit mb1,
            int[][] residualCbOut, int[][] residualCrOut) {

        predictChromaInter(refs, x, mbX << 3, mbY << 3, 1, mb1, predType);
        predictChromaInter(refs, x, mbX << 3, mbY << 3, 2, mb1, predType);

        int qp1 = calcQpChroma(qp, s.chromaQpOffset[0]);
        int qp2 = calcQpChroma(qp, s.chromaQpOffset[1]);

        decodeChromaResidual(leftAvailable, topAvailable, mbX, mbY, pattern, qp1, qp2, MBType.P_16x16, residualCbOut,
                residualCrOut);

        di.mbQps[1][mbAddr] = qp1;
        di.mbQps[2][mbAddr] = qp2;
    }

    public void predictChromaInter(Frame[][] refs, int[][][] vectors, int x, int y, int comp, Picture8Bit mb,
            PartPred[] predType) {

        Picture8Bit[] mbb = { Picture8Bit.create(16, 16, s.chromaFormat), Picture8Bit.create(16, 16, s.chromaFormat) };

        for (int blk8x8 = 0; blk8x8 < 4; blk8x8++) {
            for (int list = 0; list < 2; list++) {
                if (!predType[blk8x8].usesList(list))
                    continue;
                for (int blk4x4 = 0; blk4x4 < 4; blk4x4++) {
                    int i = BLK_INV_MAP[(blk8x8 << 2) + blk4x4];
                    int[] mv = vectors[list][i];
                    Picture8Bit ref = refs[list][mv[2]];

                    int blkPox = (i & 3) << 1;
                    int blkPoy = (i >> 2) << 1;

                    int xx = ((x + blkPox) << 3) + mv[0];
                    int yy = ((y + blkPoy) << 3) + mv[1];

                    BlockInterpolator.getBlockChroma(ref.getPlaneData(comp), ref.getPlaneWidth(comp),
                            ref.getPlaneHeight(comp), mbb[list].getPlaneData(comp), blkPoy * mb.getPlaneWidth(comp)
                                    + blkPox, mb.getPlaneWidth(comp), xx, yy, 2, 2);
                }
            }

            int blk4x4 = BLK8x8_BLOCKS[blk8x8][0];
            mergePrediction(sh, vectors[0][blk4x4][2], vectors[1][blk4x4][2], predType[blk8x8], comp,
                    mbb[0].getPlaneData(comp), mbb[1].getPlaneData(comp), BLK_8x8_MB_OFF_CHROMA[blk8x8],
                    mb.getPlaneWidth(comp), 4, 4, mb.getPlaneData(comp), refs, poc);
        }
    }
}
