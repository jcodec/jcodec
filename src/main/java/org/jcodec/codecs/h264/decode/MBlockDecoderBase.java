package org.jcodec.codecs.h264.decode;
import static org.jcodec.codecs.h264.H264Const.BLK8x8_BLOCKS;
import static org.jcodec.codecs.h264.H264Const.BLK_8x8_MB_OFF_CHROMA;
import static org.jcodec.codecs.h264.H264Const.BLK_INV_MAP;
import static org.jcodec.codecs.h264.H264Const.QP_SCALE_CR;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvRef;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvX;
import static org.jcodec.codecs.h264.H264Utils.Mv.mvY;
import static org.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;
import static org.jcodec.common.model.ColorSpace.MONO;

import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.H264Utils.MvList;
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
    protected int poc;
    protected BlockInterpolator interpolator;
    protected Picture8Bit[] mbb;

    public MBlockDecoderBase(SliceHeader sh, int poc, DecoderState decoderState) {
        this.interpolator = new BlockInterpolator();
        this.s = decoderState;
        this.sh = sh;
        this.poc = poc;
        this.mbb = new Picture8Bit[] { Picture8Bit.create(16, 16, s.chromaFormat), Picture8Bit.create(16, 16, s.chromaFormat) };
    }

    void residualLuma(CodedMBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY) {
        if (!mBlock.transform8x8Used) {
            _residualLuma(mBlock);
        } else if (sh.pps.entropyCodingModeFlag) {
            residualLuma8x8CABAC(mBlock);
        } else {
            residualLuma8x8CAVLC(mBlock);
        }
    }

    private void _residualLuma(CodedMBlock mBlock) {

        for (int i = 0; i < 16; i++) {
            if ((mBlock.cbpLuma() & (1 << (i >> 2))) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC(mBlock.ac[0][i], s.qp);
            CoeffTransformer.idct4x4(mBlock.ac[0][i]);
        }
    }

    private void residualLuma8x8CABAC(CodedMBlock mBlock) {

        for (int i = 0; i < 4; i++) {
            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC8x8(mBlock.ac[0][i], s.qp);
            CoeffTransformer.idct8x8(mBlock.ac[0][i]);
        }
    }

    private void residualLuma8x8CAVLC(CodedMBlock mBlock) {

        for (int i = 0; i < 4; i++) {
            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC8x8(mBlock.ac[0][i], s.qp);
            CoeffTransformer.idct8x8(mBlock.ac[0][i]);
        }
    }

    public void decodeChroma(CodedMBlock mBlock, int mbX, int mbY, boolean leftAvailable, boolean topAvailable,
            DecodedMBlock mb, int qp) {

        if (s.chromaFormat == MONO) {
            Arrays.fill(mb.mb.getPlaneData(1), (byte) 0);
            Arrays.fill(mb.mb.getPlaneData(2), (byte) 0);
            return;
        }

        int qp1 = calcQpChroma(qp, s.chromaQpOffset[0]);
        int qp2 = calcQpChroma(qp, s.chromaQpOffset[1]);

        if (mBlock.cbpChroma() != 0) {
            decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, qp1, qp2);
        }
        mb.mbQps[1] = qp1;
        mb.mbQps[2] = qp2;
        ChromaPredictionBuilder.predictWithMode(mBlock.ac[1], mBlock.chromaPredictionMode, mbX, leftAvailable,
                topAvailable, s.leftRow[1], s.topLine[1], s.topLeft[1], mb.mb.getPlaneData(1));
        ChromaPredictionBuilder.predictWithMode(mBlock.ac[2], mBlock.chromaPredictionMode, mbX, leftAvailable,
                topAvailable, s.leftRow[2], s.topLine[2], s.topLeft[2], mb.mb.getPlaneData(2));
    }

    void decodeChromaResidual(CodedMBlock mBlock, boolean leftAvailable, boolean topAvailable, int mbX, int mbY, int crQp1,
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

    public void predictChromaInter(Frame[][] refs, MvList vectors, int x, int y, int comp, Picture8Bit mb,
            PartPred[] predType) {

        for (int blk8x8 = 0; blk8x8 < 4; blk8x8++) {
            for (int list = 0; list < 2; list++) {
                if (!H264Const.usesList(predType[blk8x8], list))
                    continue;
                for (int blk4x4 = 0; blk4x4 < 4; blk4x4++) {
                    int i = BLK_INV_MAP[(blk8x8 << 2) + blk4x4];
                    int mv = vectors.getMv(i, list);
                    Picture8Bit ref = refs[list][mvRef(mv)];

                    int blkPox = (i & 3) << 1;
                    int blkPoy = (i >> 2) << 1;

                    int xx = ((x + blkPox) << 3) + mvX(mv);
                    int yy = ((y + blkPoy) << 3) + mvY(mv);

                    interpolator.getBlockChroma(ref.getPlaneData(comp), ref.getPlaneWidth(comp),
                            ref.getPlaneHeight(comp), mbb[list].getPlaneData(comp), blkPoy * mb.getPlaneWidth(comp)
                                    + blkPox, mb.getPlaneWidth(comp), xx, yy, 2, 2);
                }
            }

            int blk4x4 = BLK8x8_BLOCKS[blk8x8][0];
            mergePrediction(sh, vectors.mv0R(blk4x4), vectors.mv1R(blk4x4), predType[blk8x8], comp,
                    mbb[0].getPlaneData(comp), mbb[1].getPlaneData(comp), BLK_8x8_MB_OFF_CHROMA[blk8x8],
                    mb.getPlaneWidth(comp), 4, 4, mb.getPlaneData(comp), refs, poc);
        }
    }
}
