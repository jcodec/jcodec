package org.jcodec.codecs.h264.tweak;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.io.model.SliceHeader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Abstract handler interface for the parsed H.264 bitstream
 * 
 * @author The JCodec project
 * 
 */
public interface H264Handler {
    
    /**
     * Beginning of a new slice
     * 
     * @param sh
     */
    void slice(SliceHeader sh);

    /**
     * Macroblock NxN
     * 
     * An intra coded macroblock that idividually predicts each of it's 16 4x4
     * sub-blocks using intra prediction with 9 modes.
     * 
     * @param mbIdx Index of a macroblock in a slice
     * @param qpDelta
     * @param lumaResidual
     * @param chromaDC
     * @param chromaAC
     * @param lumaModes
     * @param chromaMode
     */
    void macroblockINxN(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC,
            int[] lumaModes, int chromaMode);

    /**
     * Macroblock 16x16
     * 
     * An intra coded macroblock that uses intra prediction with 4 modes ( DC,
     * VERT, HOR, PLANE ) for the whole block of 16x16 pixels.
     * 
     * @param mbIdx Index of a macroblock in a slice
     * @param qpDelta
     * @param lumaDC
     * @param lumaAC
     * @param lumaMode
     * @param chromaMode
     * @param chromaDC
     * @param chromaAC
     */
    void macroblockI16x16(int mbIdx, int qp, int[] lumaDC, int[][] lumaAC, int lumaMode, int chromaMode,
            int[][] chromaDC, int[][][] chromaAC);

    /**
     * Macroblock inter 16x16
     * 
     * An inter macroblock that carries 1 motion vector for the whole 16x16
     * block.
     * 
     * @param mbIdx Index of a macroblock in a slice
     * @param qp
     * @param lumaResidual
     * @param chromaDC
     * @param chromaAC
     * @param mvs
     * @param pred
     */
    void mblockPB16x16(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC, int[][] mvs,
            PartPred pred);

    /**
     * Macroblock inter 16x8
     * 
     * An inter macroblock that carries 2 motion vectors, one per each of it's
     * 16x8 partitions.
     * 
     * @param mbIdx Index of a macroblock in a slice
     * @param qp
     * @param lumaResidual
     * @param chromaDC
     * @param chromaAC
     * @param mvs
     * @param p0
     * @param p1
     */
    void mblockPB16x8(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC, int[][] mvs,
            PartPred p0, PartPred p1);

    /**
     * Macroblock 8x16
     * 
     * An inter macroblock that carries 2 motion vectors, one per each of it's
     * 8x16 partitions.
     * 
     * @param mbIdx Index of a macroblock in a slice
     * @param qp
     * @param lumaResidual
     * @param chromaDC
     * @param chromaAC
     * @param mvs
     * @param p0
     * @param p1
     */
    void mblockPB8x16(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC, int[][] mvs,
            PartPred p0, PartPred p1);

    /**
     * Macroblock 8x8
     * 
     * An inter macroblock that may further subdivide each of it's 4 8x8
     * partitions. Each of the 4 partitions may be coded as 8x8, 8x4, 4x8 or 4x4
     * carying 1, 2 or 4 motion vectors respectingly.
     * 
     * @param mbIdx Index of a macroblock in a slice
     * @param qp
     * @param lumaResidual
     * @param chromaDC
     * @param chromaAC
     * @param mvs
     * @param subMbTypes
     * @param l0
     * @param l02
     * @param l03
     * @param l04
     */
    void mblockPB8x8(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC, int[][][] mvs,
            int[] subMbTypes, PartPred l0, PartPred l02, PartPred l03, PartPred l04);

    /**
     * Direct macroblock
     * 
     * A macroblock that has motion predicted and residual transmitted
     * 
     * @param mbIdx Index of a macroblock in a slice
     * @param qp
     * @param lumaResidual
     * @param chromaDC
     * @param chromaAC
     */
    void mblockBDirect(int mbIdx, int qp, int[][] lumaResidual, int[][] chromaDC, int[][][] chromaAC);

    /**
     * Skipped macroblock
     * 
     * A macroblock that has motion predicted ( using median ) and no residual
     * 
     * @param mbIdx
     * @param mvX
     * @param mvY
     */
    void mblockPBSkip(int mbIdx, int mvX, int mvY);
}