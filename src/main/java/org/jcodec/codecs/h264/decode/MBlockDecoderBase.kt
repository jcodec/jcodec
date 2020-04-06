package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.H264Utils.Mv
import org.jcodec.codecs.h264.H264Utils.MvList
import org.jcodec.codecs.h264.io.model.Frame
import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Base macroblock decoder that contains routines shared by many decoders
 *
 * @author The JCodec project
 */
open class MBlockDecoderBase(sh: SliceHeader, di: DeblockerInput, poc: Int, decoderState: DecoderState) {
    protected var s: DecoderState
    protected var sh: SliceHeader
    protected var di: DeblockerInput
    protected var poc: Int
    protected var interpolator: BlockInterpolator
    protected var mbb: Array<Picture>
    protected var scalingMatrix: Array<IntArray?>?
    fun residualLuma(mBlock: MBlock, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, mbY: Int) {
        if (!mBlock.transform8x8Used) {
            residualLuma4x4(mBlock)
        } else if (sh.pps!!.isEntropyCodingModeFlag) {
            residualLuma8x8CABAC(mBlock)
        } else {
            residualLuma8x8CAVLC(mBlock)
        }
    }

    private fun residualLuma4x4(mBlock: MBlock) {
        for (i in 0..15) {
            if (mBlock.cbpLuma() and (1 shl (i shr 2)) == 0) {
                continue
            }
            CoeffTransformer.dequantizeAC(mBlock.ac[0][i], s.qp, getScalingList(if (mBlock.curMbType!!.isIntra) 0 else 3))
            CoeffTransformer.idct4x4(mBlock.ac[0][i])
        }
    }

    protected fun getScalingList(which: Int): IntArray? {
        return if (scalingMatrix == null) null else scalingMatrix!![which]
    }

    private fun residualLuma8x8CABAC(mBlock: MBlock) {
        for (i in 0..3) {
            if (mBlock.cbpLuma() and (1 shl i) == 0) {
                continue
            }
            CoeffTransformer.dequantizeAC8x8(mBlock.ac[0][i], s.qp, getScalingList(if (mBlock.curMbType!!.isIntra) 6 else 7))
            CoeffTransformer.idct8x8(mBlock.ac[0][i])
        }
    }

    private fun residualLuma8x8CAVLC(mBlock: MBlock) {
        for (i in 0..3) {
            if (mBlock.cbpLuma() and (1 shl i) == 0) {
                continue
            }
            CoeffTransformer.dequantizeAC8x8(mBlock.ac[0][i], s.qp, getScalingList(if (mBlock.curMbType!!.isIntra) 6 else 7))
            CoeffTransformer.idct8x8(mBlock.ac[0][i])
        }
    }

    fun decodeChroma(mBlock: MBlock, mbX: Int, mbY: Int, leftAvailable: Boolean, topAvailable: Boolean,
                     mb: Picture, qp: Int) {
        if (s.chromaFormat == ColorSpace.MONO) {
            Arrays.fill(mb.getPlaneData(1), 0.toByte())
            Arrays.fill(mb.getPlaneData(2), 0.toByte())
            return
        }
        val qp1 = calcQpChroma(qp, s.chromaQpOffset[0])
        val qp2 = calcQpChroma(qp, s.chromaQpOffset[1])
        if (mBlock.cbpChroma() != 0) {
            decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, qp1, qp2)
        }
        val addr = mbY * (sh.sps!!.picWidthInMbsMinus1 + 1) + mbX
        di.mbQps[1][addr] = qp1
        di.mbQps[2][addr] = qp2
        ChromaPredictionBuilder.predictWithMode(mBlock.ac[1], mBlock.chromaPredictionMode, mbX, leftAvailable,
                topAvailable, s.leftRow[1], s.topLine[1], s.topLeft[1], mb.getPlaneData(1))
        ChromaPredictionBuilder.predictWithMode(mBlock.ac[2], mBlock.chromaPredictionMode, mbX, leftAvailable,
                topAvailable, s.leftRow[2], s.topLine[2], s.topLeft[2], mb.getPlaneData(2))
    }

    fun decodeChromaResidual(mBlock: MBlock, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, mbY: Int, crQp1: Int,
                             crQp2: Int) {
        if (mBlock.cbpChroma() != 0) {
            if (mBlock.cbpChroma() and 3 > 0) {
                chromaDC(mbX, leftAvailable, topAvailable, mBlock.dc1, 1, crQp1, mBlock.curMbType)
                chromaDC(mbX, leftAvailable, topAvailable, mBlock.dc2, 2, crQp2, mBlock.curMbType)
            }
            chromaAC(leftAvailable, topAvailable, mbX, mbY, mBlock.dc1, 1, crQp1, mBlock.curMbType,
                    mBlock.cbpChroma() and 2 > 0, mBlock.ac[1])
            chromaAC(leftAvailable, topAvailable, mbX, mbY, mBlock.dc2, 2, crQp2, mBlock.curMbType,
                    mBlock.cbpChroma() and 2 > 0, mBlock.ac[2])
        }
    }

    private fun chromaDC(mbX: Int, leftAvailable: Boolean, topAvailable: Boolean, dc: IntArray, comp: Int, crQp: Int,
                         curMbType: MBType?) {
        CoeffTransformer.invDC2x2(dc)
        CoeffTransformer.dequantizeDC2x2(dc, crQp, getScalingList((if (curMbType!!.isIntra) 6 else 7) + comp * 2))
    }

    private fun chromaAC(leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, mbY: Int, dc: IntArray, comp: Int, crQp: Int,
                         curMbType: MBType?, codedAC: Boolean, residualOut: Array<IntArray>) {
        for (i in dc.indices) {
            val ac = residualOut[i]
            if (codedAC) {
                CoeffTransformer.dequantizeAC(ac, crQp, getScalingList((if (curMbType!!.isIntra) 0 else 3) + comp))
            }
            ac[0] = dc[i]
            CoeffTransformer.idct4x4(ac)
        }
    }

    fun predictChromaInter(refs: Array<Array<Frame?>>, vectors: MvList, x: Int, y: Int, comp: Int, mb: Picture,
                           predType: Array<PartPred?>) {
        for (blk8x8 in 0..3) {
            for (list in 0..1) {
                if (!H264Const.usesList(predType[blk8x8], list)) continue
                for (blk4x4 in 0..3) {
                    val i = H264Const.BLK_INV_MAP[(blk8x8 shl 2) + blk4x4]
                    val mv = vectors.getMv(i, list)
                    val ref = refs[list][Mv.mvRef(mv)]!!
                    val blkPox = i and 3 shl 1
                    val blkPoy = i shr 2 shl 1
                    val xx = (x + blkPox shl 3) + Mv.mvX(mv)
                    val yy = (y + blkPoy shl 3) + Mv.mvY(mv)
                    BlockInterpolator.getBlockChroma(ref.getPlaneData(comp), ref.getPlaneWidth(comp),
                            ref.getPlaneHeight(comp), mbb[list].getPlaneData(comp), blkPoy * mb.getPlaneWidth(comp)
                            + blkPox, mb.getPlaneWidth(comp), xx, yy, 2, 2)
                }
            }
            val blk4x4 = H264Const.BLK8x8_BLOCKS[blk8x8][0]
            PredictionMerger.mergePrediction(sh, vectors.mv0R(blk4x4), vectors.mv1R(blk4x4), predType[blk8x8], comp,
                    mbb[0].getPlaneData(comp), mbb[1].getPlaneData(comp), H264Const.BLK_8x8_MB_OFF_CHROMA[blk8x8],
                    mb.getPlaneWidth(comp), 4, 4, mb.getPlaneData(comp), refs, poc)
        }
    }

    companion object {
        protected fun initScalingMatrix(sh2: SliceHeader): Array<IntArray?>? {
            if (sh2.sps!!.scalingMatrix == null && (sh2.pps!!.extended == null || sh2.pps!!.extended!!.scalingMatrix == null)) return null
            val merged = arrayOf(H264Const.defaultScalingList4x4Intra, null, null,
                    H264Const.defaultScalingList4x4Inter, null, null, H264Const.defaultScalingList8x8Intra,
                    H264Const.defaultScalingList8x8Inter, null, null, null, null)
            for (i in 0..7) {
                if (sh2.sps!!.scalingMatrix != null && sh2.sps!!.scalingMatrix!![i] != null) merged[i] = sh2.sps!!.scalingMatrix!![i]
                if (sh2.pps!!.extended != null && sh2.pps!!.extended!!.scalingMatrix != null && sh2.pps!!.extended!!.scalingMatrix!![i] != null) merged[i] = sh2.pps!!.extended!!.scalingMatrix!![i]
            }
            if (merged[1] == null) merged[1] = merged[0]
            if (merged[2] == null) merged[2] = merged[0]
            if (merged[4] == null) merged[4] = merged[3]
            if (merged[5] == null) merged[5] = merged[3]
            if (merged[8] == null) merged[8] = merged[6]
            if (merged[10] == null) merged[10] = merged[6]
            if (merged[9] == null) merged[9] = merged[7]
            if (merged[11] == null) merged[11] = merged[7]
            return merged
        }

        fun calcQpChroma(qp: Int, crQpOffset: Int): Int {
            return H264Const.QP_SCALE_CR[MathUtil.clip(qp + crQpOffset, 0, 51)]
        }
    }

    init {
        interpolator = BlockInterpolator()
        s = decoderState
        this.sh = sh
        this.di = di
        this.poc = poc
        mbb = arrayOf(Picture.create(16, 16, s.chromaFormat), Picture.create(16, 16, s.chromaFormat))
        scalingMatrix = initScalingMatrix(sh)
    }
}