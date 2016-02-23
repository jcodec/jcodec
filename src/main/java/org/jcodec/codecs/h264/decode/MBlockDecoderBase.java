package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.BLK8x8_BLOCKS;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_MB_OFF_CHROMA;
import static org.jcodec.codecs.h264.H264Const.BLK_INV_MAP;
import static org.jcodec.codecs.h264.H264Const.QP_SCALE_CR;
import static org.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;
import static org.jcodec.common.model.ColorSpace.MONO;

import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SliceHeader;
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
    protected SliceHeader sh;
    protected DeblockerInput di;
    protected int poc;
    protected BlockInterpolator interpolator;

    public MBlockDecoderBase(SliceHeader sh, DeblockerInput di, int poc, DecoderState decoderState) {
        this.interpolator = new BlockInterpolator();
        this.s = decoderState;
        this.sh = sh;
        this.di = di;
        this.poc = poc;
    }

    void residualLuma(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY) {
        if (!mBlock.transform8x8Used) {
            residualLuma(mBlock);
        } else if (sh.pps.entropy_coding_mode_flag) {
            residualLuma8x8CABAC(mBlock);
        } else {
            residualLuma8x8CAVLC(mBlock);
        }
    }

    private void residualLuma(MBlock mBlock) {

        for (int i = 0; i < 16; i++) {
            if ((mBlock.cbpLuma() & (1 << (i >> 2))) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC(mBlock.ac[0][i], s.qp);
            CoeffTransformer.idct4x4(mBlock.ac[0][i]);
        }
    }

    private void residualLuma8x8CABAC(MBlock mBlock) {

        for (int i = 0; i < 4; i++) {
            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC8x8(mBlock.ac[0][i], s.qp);
            CoeffTransformer.idct8x8(mBlock.ac[0][i]);
        }
    }

    private void residualLuma8x8CAVLC(MBlock mBlock) {

        for (int i = 0; i < 4; i++) {
            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC8x8(mBlock.ac[0][i], s.qp);
            CoeffTransformer.idct8x8(mBlock.ac[0][i]);
        }
    }

    public void decodeChroma(MBlock mBlock, int mbX, int mbY, boolean leftAvailable, boolean topAvailable,
            Picture8Bit mb, int qp) {

        if (s.chromaFormat == MONO) {
            Arrays.fill(mb.getPlaneData(1), (byte) 0);
            Arrays.fill(mb.getPlaneData(2), (byte) 0);
            return;
        }

        int qp1 = calcQpChroma(qp, s.chromaQpOffset[0]);
        int qp2 = calcQpChroma(qp, s.chromaQpOffset[1]);

        if (mBlock.cbpChroma() != 0) {
            decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, qp1, qp2);
        }
        int addr = mbY * (sh.sps.pic_width_in_mbs_minus1 + 1) + mbX;
        di.mbQps[1][addr] = qp1;
        di.mbQps[2][addr] = qp2;
        ChromaPredictionBuilder.predictWithMode(mBlock.ac[1], mBlock.chromaPredictionMode, mbX, leftAvailable,
                topAvailable, s.leftRow[1], s.topLine[1], s.topLeft[1], mb.getPlaneData(1));
        ChromaPredictionBuilder.predictWithMode(mBlock.ac[2], mBlock.chromaPredictionMode, mbX, leftAvailable,
                topAvailable, s.leftRow[2], s.topLine[2], s.topLeft[2], mb.getPlaneData(2));
    }

    void decodeChromaResidual(MBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int crQp1,
            int crQp2) {
        if (mBlock.cbpChroma() != 0) {
            if ((mBlock.cbpChroma() & 3) > 0) {
                chromaDC(mbX, leftAvailable, topAvailable, mBlock.dc1, 1, crQp1, mBlock.curMbType);
                chromaDC(mbX, leftAvailable, topAvailable, mBlock.dc2, 2, crQp2, mBlock.curMbType);
            }

            chromaAC(leftAvailable, topAvailable, mbX, mbY, mBlock.dc1, 1, crQp1, mBlock.curMbType,
                    (mBlock.cbpChroma() & 2) > 0, mBlock.ac[1]);
            chromaAC(leftAvailable, topAvailable, mbX, mbY, mBlock.dc2, 2, crQp2, mBlock.curMbType,
                    (mBlock.cbpChroma() & 2) > 0, mBlock.ac[2]);
        }
    }

    private void chromaDC(int mbX, boolean leftAvailable, boolean topAvailable, int[] dc, int comp, int crQp,
            MBType curMbType) {
        CoeffTransformer.invDC2x2(dc);
        CoeffTransformer.dequantizeDC2x2(dc, crQp);
    }

    private void chromaAC(boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int[] dc, int comp, int crQp,
            MBType curMbType, boolean codedAC, int[][] residualOut) {
        for (int i = 0; i < dc.length; i++) {
            int[] ac = residualOut[i];

            if (codedAC) {
                CoeffTransformer.dequantizeAC(ac, crQp);
            }
            ac[0] = dc[i];
            CoeffTransformer.idct4x4(ac);
        }
    }

    int calcQpChroma(int qp, int crQpOffset) {
        return QP_SCALE_CR[MathUtil.clip(qp + crQpOffset, 0, 51)];
    }

    // public void decodeChromaInter(MBlock mBlock, MBType curMbType, int
    // pattern, Frame[][] refs, int[][][] x,
    // PartPred[] predType, boolean leftAvailable, boolean topAvailable, int
    // mbX, int mbY, int mbAddr, int qp,
    // Picture8Bit mb1, int[][] residualCbOut, int[][] residualCrOut) {
    //
    // predictChromaInter(refs, x, mbX << 3, mbY << 3, 1, mb1, predType);
    // predictChromaInter(refs, x, mbX << 3, mbY << 3, 2, mb1, predType);
    //
    // parser.readChromaResidual(mBlock, leftAvailable, topAvailable, mbX,
    // mBlock.cbpChroma(), curMbType);
    //
    // int qp1 = calcQpChroma(qp, s.chromaQpOffset[0]);
    // int qp2 = calcQpChroma(qp, s.chromaQpOffset[1]);
    //
    // decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY,
    // pattern, qp1, qp2, curMbType);
    //
    // di.mbQps[1][mbAddr] = qp1;
    // di.mbQps[2][mbAddr] = qp2;
    // }

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

                    interpolator.getBlockChroma(ref.getPlaneData(comp), ref.getPlaneWidth(comp),
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
