package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.H264Utils.Mv
import org.jcodec.codecs.h264.H264Utils.MvList
import org.jcodec.codecs.h264.decode.aso.Mapper
import org.jcodec.codecs.h264.io.model.Frame
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

/**
 * A decoder for B direct macroblocks
 *
 * @author The JCodec project
 */
class MBlockDecoderBDirect(private val mapper: Mapper, sh: SliceHeader?, di: DeblockerInput?, poc: Int, decoderState: DecoderState?) : MBlockDecoderBase(sh, di, poc, decoderState) {
    fun decode(mBlock: MBlock, mb: Picture, references: Array<Array<Frame?>>) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val lAvb = mapper.leftAvailable(mBlock.mbIdx)
        val tAvb = mapper.topAvailable(mBlock.mbIdx)
        val mbAddr = mapper.getAddress(mBlock.mbIdx)
        val tlAvb = mapper.topLeftAvailable(mBlock.mbIdx)
        val trAvb = mapper.topRightAvailable(mBlock.mbIdx)
        predictBDirect(references, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, mBlock.x, mBlock.partPreds, mb, H264Const.identityMapping4)
        predictChromaInter(references, mBlock.x, mbX shl 3, mbY shl 3, 1, mb, mBlock.partPreds)
        predictChromaInter(references, mBlock.x, mbX shl 3, mbY shl 3, 2, mb, mBlock.partPreds)
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52
        }
        di.mbQps[0][mbAddr] = s.qp
        residualLuma(mBlock, lAvb, tAvb, mbX, mbY)
        MBlockDecoderUtils.savePrediction8x8(s, mbX, mBlock.x)
        MBlockDecoderUtils.saveMvs(di, mBlock.x, mbX, mbY)
        val qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0])
        val qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1])
        decodeChromaResidual(mBlock, lAvb, tAvb, mbX, mbY, qp1, qp2)
        di.mbQps[1][mbAddr] = qp1
        di.mbQps[2][mbAddr] = qp2
        MBlockDecoderUtils.mergeResidual(mb, mBlock.ac, if (mBlock.transform8x8Used) H264Const.COMP_BLOCK_8x8_LUT else H264Const.COMP_BLOCK_4x4_LUT,
                if (mBlock.transform8x8Used) H264Const.COMP_POS_8x8_LUT else H264Const.COMP_POS_4x4_LUT)
        MBlockDecoderUtils.collectPredictors(s, mb, mbX)
        di.mbTypes[mbAddr] = mBlock.curMbType
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used
    }

    fun predictBDirect(refs: Array<Array<Frame?>>, mbX: Int, mbY: Int, lAvb: Boolean, tAvb: Boolean, tlAvb: Boolean,
                       trAvb: Boolean, x: MvList, pp: Array<PartPred?>, mb: Picture, blocks: IntArray) {
        if (sh.directSpatialMvPredFlag) predictBSpatialDirect(refs, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, blocks) else predictBTemporalDirect(refs, mbX, mbY, lAvb, tAvb, tlAvb, trAvb, x, pp, mb, blocks)
    }

    private fun predictBTemporalDirect(refs: Array<Array<Frame?>>, mbX: Int, mbY: Int, lAvb: Boolean, tAvb: Boolean, tlAvb: Boolean,
                                       trAvb: Boolean, x: MvList, pp: Array<PartPred?>, mb: Picture, blocks8x8: IntArray) {
        for (i in blocks8x8.indices) {
            val blk8x8 = blocks8x8[i]
            val blk4x4_0 = H264Const.BLK8x8_BLOCKS[blk8x8][0]
            pp[blk8x8] = PartPred.Bi
            if (!sh.sps!!.isDirect8x8InferenceFlag) {
                val js = H264Const.BLK8x8_BLOCKS[blk8x8]
                for (j in js.indices) {
                    val blk4x4 = js[j]
                    predTemp4x4(refs, mbX, mbY, x, blk4x4)
                    val blkIndX = blk4x4 and 3
                    val blkIndY = blk4x4 shr 2
                    MBlockDecoderUtils.debugPrint("DIRECT_4x4 [%d, %d]: (%d,%d,%d), (%d,%d,%d)", blkIndY, blkIndX, x.mv0X(blk4x4),
                            x.mv0Y(blk4x4), x.mv0R(blk4x4), x.mv1X(blk4x4), x.mv1Y(blk4x4), x.mv1R(blk4x4))
                    val blkPredX = (mbX shl 6) + (blkIndX shl 4)
                    val blkPredY = (mbY shl 6) + (blkIndY shl 4)
                    interpolator.getBlockLuma(refs[0][x.mv0R(blk4x4)], mbb[0], H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x.mv0X(blk4x4), blkPredY + x.mv0Y(blk4x4), 4, 4)
                    interpolator.getBlockLuma(refs[1][0], mbb[1], H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x.mv1X(blk4x4), blkPredY + x.mv1Y(blk4x4), 4, 4)
                }
            } else {
                val blk4x4Pred = H264Const.BLK_INV_MAP[blk8x8 * 5]
                predTemp4x4(refs, mbX, mbY, x, blk4x4Pred)
                propagatePred(x, blk8x8, blk4x4Pred)
                val blkIndX = blk4x4_0 and 3
                val blkIndY = blk4x4_0 shr 2
                MBlockDecoderUtils.debugPrint("DIRECT_8x8 [%d, %d]: (%d,%d,%d), (%d,%d)", blkIndY, blkIndX, x.mv0X(blk4x4_0),
                        x.mv0Y(blk4x4_0), x.mv0R(blk4x4_0), x.mv1X(blk4x4_0), x.mv1Y(blk4x4_0), x.mv1R(blk4x4_0))
                val blkPredX = (mbX shl 6) + (blkIndX shl 4)
                val blkPredY = (mbY shl 6) + (blkIndY shl 4)
                interpolator.getBlockLuma(refs[0][x.mv0R(blk4x4_0)], mbb[0], H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x.mv0X(blk4x4_0), blkPredY + x.mv0Y(blk4x4_0), 8, 8)
                interpolator.getBlockLuma(refs[1][0], mbb[1], H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x.mv1X(blk4x4_0), blkPredY + x.mv1Y(blk4x4_0), 8, 8)
            }
            PredictionMerger.mergePrediction(sh, x.mv0R(blk4x4_0), x.mv1R(blk4x4_0), PartPred.Bi, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                    H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4_0], 16, 8, 8, mb.getPlaneData(0), refs, poc)
        }
    }

    private fun predTemp4x4(refs: Array<Array<Frame?>>, mbX: Int, mbY: Int, x: MvList, blk4x4: Int) {
        val mbWidth = sh.sps!!.picWidthInMbsMinus1 + 1
        val picCol = refs[1][0]!!
        val blkIndX = blk4x4 and 3
        val blkIndY = blk4x4 shr 2
        val blkPosX = (mbX shl 2) + blkIndX
        val blkPosY = (mbY shl 2) + blkIndY
        var mvCol = picCol.mvs.getMv(blkPosX, blkPosY, 0)
        val refL0: Frame?
        val refIdxL0: Int
        if (Mv.mvRef(mvCol) == -1) {
            mvCol = picCol.mvs.getMv(blkPosX, blkPosY, 1)
            if (Mv.mvRef(mvCol) == -1) {
                refIdxL0 = 0
                refL0 = refs[0][0]
            } else {
                refL0 = picCol.refsUsed[mbY * mbWidth + mbX]!![1]!![Mv.mvRef(mvCol)]!!
                refIdxL0 = findPic(refs[0], refL0)
            }
        } else {
            refL0 = picCol.refsUsed[mbY * mbWidth + mbX]!![0]!![Mv.mvRef(mvCol)]!!
            refIdxL0 = findPic(refs[0], refL0)
        }
        val td = MathUtil.clip(picCol.pOC - refL0!!.pOC, -128, 127)
        if (!refL0.isShortTerm || td == 0) {
            x.setPair(blk4x4, Mv.packMv(Mv.mvX(mvCol), Mv.mvY(mvCol), refIdxL0), 0)
        } else {
            val tb = MathUtil.clip(poc - refL0.pOC, -128, 127)
            val tx = (16384 + Math.abs(td / 2)) / td
            val dsf = MathUtil.clip(tb * tx + 32 shr 6, -1024, 1023)
            x.setPair(blk4x4, Mv.packMv(dsf * Mv.mvX(mvCol) + 128 shr 8, dsf * Mv.mvY(mvCol) + 128 shr 8, refIdxL0),
                    Mv.packMv(x.mv0X(blk4x4) - Mv.mvX(mvCol), x.mv0Y(blk4x4) - Mv.mvY(mvCol), 0))
        }
    }

    private fun findPic(frames: Array<Frame?>, refL0: Frame): Int {
        for (i in frames.indices) if (frames[i] === refL0) return i
        Logger.error("RefPicList0 shall contain refPicCol")
        return 0
    }

    private fun predictBSpatialDirect(refs: Array<Array<Frame?>>, mbX: Int, mbY: Int, lAvb: Boolean, tAvb: Boolean, tlAvb: Boolean,
                                      trAvb: Boolean, x: MvList, pp: Array<PartPred?>, mb: Picture, blocks8x8: IntArray) {
        val a0 = s.mvLeft.getMv(0, 0)
        val a1 = s.mvLeft.getMv(0, 1)
        val b0 = s.mvTop.getMv(mbX shl 2, 0)
        val b1 = s.mvTop.getMv(mbX shl 2, 1)
        val c0 = s.mvTop.getMv((mbX shl 2) + 4, 0)
        val c1 = s.mvTop.getMv((mbX shl 2) + 4, 1)
        val d0 = s.mvTopLeft.getMv(0, 0)
        val d1 = s.mvTopLeft.getMv(0, 1)
        val refIdxL0 = calcRef(a0, b0, c0, d0, lAvb, tAvb, tlAvb, trAvb, mbX)
        val refIdxL1 = calcRef(a1, b1, c1, d1, lAvb, tAvb, tlAvb, trAvb, mbX)
        if (refIdxL0 < 0 && refIdxL1 < 0) {
            for (i in blocks8x8.indices) {
                val blk8x8 = blocks8x8[i]
                val js = H264Const.BLK8x8_BLOCKS[blk8x8]
                for (j in js.indices) {
                    val blk4x4 = js[j]
                    x.setPair(blk4x4, 0, 0)
                }
                pp[blk8x8] = PartPred.Bi
                val blkOffX = blk8x8 and 1 shl 5
                val blkOffY = blk8x8 shr 1 shl 5
                interpolator.getBlockLuma(refs[0][0], mbb[0], H264Const.BLK_8x8_MB_OFF_LUMA[blk8x8], (mbX shl 6) + blkOffX,
                        (mbY shl 6) + blkOffY, 8, 8)
                interpolator.getBlockLuma(refs[1][0], mbb[1], H264Const.BLK_8x8_MB_OFF_LUMA[blk8x8], (mbX shl 6) + blkOffX,
                        (mbY shl 6) + blkOffY, 8, 8)
                PredictionMerger.mergePrediction(sh, 0, 0, PartPred.Bi, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                        H264Const.BLK_8x8_MB_OFF_LUMA[blk8x8], 16, 8, 8, mb.getPlaneData(0), refs, poc)
                MBlockDecoderUtils.debugPrint("DIRECT_8x8 [%d, %d]: (0,0,0), (0,0,0)", blk8x8 and 2, blk8x8 shl 1 and 2)
            }
            return
        }
        val mvX0 = MBlockDecoderUtils.calcMVPredictionMedian(a0, b0, c0, d0, lAvb, tAvb, trAvb, tlAvb, refIdxL0, 0)
        val mvY0 = MBlockDecoderUtils.calcMVPredictionMedian(a0, b0, c0, d0, lAvb, tAvb, trAvb, tlAvb, refIdxL0, 1)
        val mvX1 = MBlockDecoderUtils.calcMVPredictionMedian(a1, b1, c1, d1, lAvb, tAvb, trAvb, tlAvb, refIdxL1, 0)
        val mvY1 = MBlockDecoderUtils.calcMVPredictionMedian(a1, b1, c1, d1, lAvb, tAvb, trAvb, tlAvb, refIdxL1, 1)
        val col = refs[1][0]
        val partPred = if (refIdxL0 >= 0 && refIdxL1 >= 0) PartPred.Bi else if (refIdxL0 >= 0) PartPred.L0 else PartPred.L1
        for (i in blocks8x8.indices) {
            val blk8x8 = blocks8x8[i]
            val blk4x4_0 = H264Const.BLK8x8_BLOCKS[blk8x8][0]
            if (!sh.sps!!.isDirect8x8InferenceFlag) {
                val js = H264Const.BLK8x8_BLOCKS[blk8x8]
                for (j in js.indices) {
                    val blk4x4 = js[j]
                    pred4x4(mbX, mbY, x, pp, refIdxL0, refIdxL1, mvX0, mvY0, mvX1, mvY1, col!!, partPred, blk4x4)
                    val blkIndX = blk4x4 and 3
                    val blkIndY = blk4x4 shr 2
                    MBlockDecoderUtils.debugPrint("DIRECT_4x4 [%d, %d]: (%d,%d,%d), (%d,%d,$refIdxL1)", blkIndY, blkIndX,
                            x.mv0X(blk4x4), x.mv0Y(blk4x4), refIdxL0, x.mv1X(blk4x4), x.mv1Y(blk4x4))
                    val blkPredX = (mbX shl 6) + (blkIndX shl 4)
                    val blkPredY = (mbY shl 6) + (blkIndY shl 4)
                    if (refIdxL0 >= 0) interpolator.getBlockLuma(refs[0][refIdxL0], mbb[0], H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x.mv0X(blk4x4), blkPredY + x.mv0Y(blk4x4), 4, 4)
                    if (refIdxL1 >= 0) interpolator.getBlockLuma(refs[1][refIdxL1], mbb[1], H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4], blkPredX
                            + x.mv1X(blk4x4), blkPredY + x.mv1Y(blk4x4), 4, 4)
                }
            } else {
                val blk4x4Pred = H264Const.BLK_INV_MAP[blk8x8 * 5]
                pred4x4(mbX, mbY, x, pp, refIdxL0, refIdxL1, mvX0, mvY0, mvX1, mvY1, col!!, partPred, blk4x4Pred)
                propagatePred(x, blk8x8, blk4x4Pred)
                val blkIndX = blk4x4_0 and 3
                val blkIndY = blk4x4_0 shr 2
                MBlockDecoderUtils.debugPrint("DIRECT_8x8 [%d, %d]: (%d,%d,%d), (%d,%d,%d)", blkIndY, blkIndX, x.mv0X(blk4x4_0),
                        x.mv0Y(blk4x4_0), refIdxL0, x.mv1X(blk4x4_0), x.mv1Y(blk4x4_0), refIdxL1)
                val blkPredX = (mbX shl 6) + (blkIndX shl 4)
                val blkPredY = (mbY shl 6) + (blkIndY shl 4)
                if (refIdxL0 >= 0) interpolator.getBlockLuma(refs[0][refIdxL0], mbb[0], H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x.mv0X(blk4x4_0), blkPredY + x.mv0Y(blk4x4_0), 8, 8)
                if (refIdxL1 >= 0) interpolator.getBlockLuma(refs[1][refIdxL1], mbb[1], H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4_0], blkPredX
                        + x.mv1X(blk4x4_0), blkPredY + x.mv1Y(blk4x4_0), 8, 8)
            }
            PredictionMerger.mergePrediction(sh, x.mv0R(blk4x4_0), x.mv1R(blk4x4_0),
                    if (refIdxL0 >= 0) if (refIdxL1 >= 0) PartPred.Bi else PartPred.L0 else PartPred.L1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                    H264Const.BLK_4x4_MB_OFF_LUMA[blk4x4_0], 16, 8, 8, mb.getPlaneData(0), refs, poc)
        }
    }

    private fun calcRef(a0: Int, b0: Int, c0: Int, d0: Int, lAvb: Boolean, tAvb: Boolean, tlAvb: Boolean, trAvb: Boolean,
                        mbX: Int): Int {
        return minPos(minPos(if (lAvb) Mv.mvRef(a0) else -1, if (tAvb) Mv.mvRef(b0) else -1), if (trAvb) Mv.mvRef(c0) else if (tlAvb) Mv.mvRef(d0) else -1)
    }

    private fun propagatePred(x: MvList, blk8x8: Int, blk4x4Pred: Int) {
        val b0 = H264Const.BLK8x8_BLOCKS[blk8x8][0]
        val b1 = H264Const.BLK8x8_BLOCKS[blk8x8][1]
        val b2 = H264Const.BLK8x8_BLOCKS[blk8x8][2]
        val b3 = H264Const.BLK8x8_BLOCKS[blk8x8][3]
        x.copyPair(b0, x, blk4x4Pred)
        x.copyPair(b1, x, blk4x4Pred)
        x.copyPair(b2, x, blk4x4Pred)
        x.copyPair(b3, x, blk4x4Pred)
    }

    private fun pred4x4(mbX: Int, mbY: Int, x: MvList, pp: Array<PartPred?>, refL0: Int, refL1: Int, mvX0: Int, mvY0: Int,
                        mvX1: Int, mvY1: Int, col: Frame, partPred: PartPred, blk4x4: Int) {
        val blkIndX = blk4x4 and 3
        val blkIndY = blk4x4 shr 2
        val blkPosX = (mbX shl 2) + blkIndX
        val blkPosY = (mbY shl 2) + blkIndY
        var mvCol = col.mvs.getMv(blkPosX, blkPosY, 0)
        if (Mv.mvRef(mvCol) == -1) mvCol = col.mvs.getMv(blkPosX, blkPosY, 1)
        val colZero = col.isShortTerm && Mv.mvRef(mvCol) == 0 && MathUtil.abs(Mv.mvX(mvCol)) shr 1 == 0 && MathUtil.abs(Mv.mvY(mvCol)) shr 1 == 0
        var x0 = Mv.packMv(0, 0, refL0)
        var x1 = Mv.packMv(0, 0, refL1)
        if (refL0 > 0 || !colZero) {
            x0 = Mv.packMv(mvX0, mvY0, refL0)
        }
        if (refL1 > 0 || !colZero) {
            x1 = Mv.packMv(mvX1, mvY1, refL1)
        }
        x.setPair(blk4x4, x0, x1)
        pp[H264Const.BLK_8x8_IND[blk4x4]] = partPred
    }

    private fun minPos(a: Int, b: Int): Int {
        return if (a >= 0 && b >= 0) Math.min(a, b) else Math.max(a, b)
    }

}