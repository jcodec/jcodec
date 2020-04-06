package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.H264Utils.Mv
import org.jcodec.codecs.h264.H264Utils.MvList
import org.jcodec.codecs.h264.decode.aso.Mapper
import org.jcodec.codecs.h264.io.model.Frame
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 *
 * @author The JCodec project
 */
class MBlockDecoderInter(private val mapper: Mapper, sh: SliceHeader?, di: DeblockerInput?, poc: Int, decoderState: DecoderState?) : MBlockDecoderBase(sh!!, di!!, poc, decoderState!!) {
    fun decode16x16(mBlock: MBlock, mb: Picture, refs: Array<Array<Frame?>?>?, p0: PartPred?) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        val topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx)
        val topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx)
        val address = mapper.getAddress(mBlock.mbIdx)
        val xx = mbX shl 2
        for (list in 0..1) {
            predictInter16x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, mBlock.x, xx, list, p0)
        }
        PredictionMerger.mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0),
                mbb[1].getPlaneData(0), 0, 16, 16, 16, mb.getPlaneData(0), refs, poc)
        mBlock.partPreds[3] = p0
        mBlock.partPreds[2] = mBlock.partPreds[3]
        mBlock.partPreds[1] = mBlock.partPreds[2]
        mBlock.partPreds[0] = mBlock.partPreds[1]
        predictChromaInter(refs, mBlock.x, mbX shl 3, mbY shl 3, 1, mb, mBlock.partPreds)
        predictChromaInter(refs, mBlock.x, mbX shl 3, mbY shl 3, 2, mb, mBlock.partPreds)
        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, mapper.getAddress(mBlock.mbIdx))
        MBlockDecoderUtils.saveMvs(di, mBlock.x, mbX, mbY)
        MBlockDecoderUtils.mergeResidual(mb, mBlock.ac, if (mBlock.transform8x8Used) H264Const.COMP_BLOCK_8x8_LUT else H264Const.COMP_BLOCK_4x4_LUT,
                if (mBlock.transform8x8Used) H264Const.COMP_POS_8x8_LUT else H264Const.COMP_POS_4x4_LUT)
        MBlockDecoderUtils.collectPredictors(s, mb, mbX)
        di.mbTypes[address] = mBlock.curMbType
    }

    private fun predictInter8x16(mBlock: MBlock, mb: Picture, references: Array<Array<Frame?>?>?, mbX: Int, mbY: Int,
                                 leftAvailable: Boolean, topAvailable: Boolean, tlAvailable: Boolean, trAvailable: Boolean, x: MvList,
                                 list: Int, p0: PartPred, p1: PartPred) {
        val xx = mbX shl 2
        var mvX1 = 0
        var mvY1 = 0
        var r1 = -1
        var mvX2 = 0
        var mvY2 = 0
        var r2 = -1
        if (H264Const.usesList(p0, list)) {
            val mvpX1 = calcMVPrediction8x16Left(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX shl 2, list),
                    s.mvTop.getMv((mbX shl 2) + 2, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    topAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 0)
            val mvpY1 = calcMVPrediction8x16Left(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX shl 2, list),
                    s.mvTop.getMv((mbX shl 2) + 2, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    topAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 1)
            mvX1 = mBlock.pb168x168.mvdX1[list] + mvpX1
            mvY1 = mBlock.pb168x168.mvdY1[list] + mvpY1
            MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb168x168.mvdX1[list],
                    mBlock.pb168x168.mvdY1[list], mvX1, mvY1, mBlock.pb168x168.refIdx1[list])
            interpolator.getBlockLuma(references!![list]!![mBlock.pb168x168.refIdx1[list]]!!, mb, 0, (mbX shl 6) + mvX1,
                    (mbY shl 6) + mvY1, 8, 16)
            r1 = mBlock.pb168x168.refIdx1[list]
        }

        // Horizontal motion vector range does not exceed the range of -2048 to 2047.75, inclusive, in units of luma
        // samples. Vertical MV [-512,+511.75]. I.e. 14 + 12 bits = 26 bits. Ref Idx 6 bit ?
        val v1 = Mv.packMv(mvX1, mvY1, r1)
        if (H264Const.usesList(p1, list)) {
            val mvpX2 = calcMVPrediction8x16Right(v1, s.mvTop.getMv((mbX shl 2) + 2, list),
                    s.mvTop.getMv((mbX shl 2) + 4, list), s.mvTop.getMv((mbX shl 2) + 1, list), true, topAvailable,
                    trAvailable, topAvailable, mBlock.pb168x168.refIdx2[list], 0)
            val mvpY2 = calcMVPrediction8x16Right(v1, s.mvTop.getMv((mbX shl 2) + 2, list),
                    s.mvTop.getMv((mbX shl 2) + 4, list), s.mvTop.getMv((mbX shl 2) + 1, list), true, topAvailable,
                    trAvailable, topAvailable, mBlock.pb168x168.refIdx2[list], 1)
            mvX2 = mBlock.pb168x168.mvdX2[list] + mvpX2
            mvY2 = mBlock.pb168x168.mvdY2[list] + mvpY2
            MBlockDecoderUtils.debugPrint("MVP: (" + mvpX2 + ", " + mvpY2 + "), MVD: (" + mBlock.pb168x168.mvdX2[list] + ", "
                    + mBlock.pb168x168.mvdY2[list] + "), MV: (" + mvX2 + "," + mvY2 + ","
                    + mBlock.pb168x168.refIdx2[list] + ")")
            interpolator.getBlockLuma(references!![list]!![mBlock.pb168x168.refIdx2[list]]!!, mb, 8, (mbX shl 6) + 32
                    + mvX2, (mbY shl 6) + mvY2, 8, 16)
            r2 = mBlock.pb168x168.refIdx2[list]
        }
        val v2 = Mv.packMv(mvX2, mvY2, r2)
        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list))
        MBlockDecoderUtils.saveVect(s.mvTop, list, xx, xx + 2, v1)
        MBlockDecoderUtils.saveVect(s.mvTop, list, xx + 2, xx + 4, v2)
        MBlockDecoderUtils.saveVect(s.mvLeft, list, 0, 4, v2)
        var i = 0
        while (i < 16) {
            x.setMv(i, list, v1)
            x.setMv(i + 1, list, v1)
            x.setMv(i + 2, list, v2)
            x.setMv(i + 3, list, v2)
            i += 4
        }
    }

    private fun predictInter16x8(mBlock: MBlock, mb: Picture, references: Array<Array<Frame?>?>?, mbX: Int, mbY: Int,
                                 leftAvailable: Boolean, topAvailable: Boolean, tlAvailable: Boolean, trAvailable: Boolean, xx: Int, x: MvList,
                                 p0: PartPred, p1: PartPred, list: Int) {
        var mvX1 = 0
        var mvY1 = 0
        var mvX2 = 0
        var mvY2 = 0
        var r1 = -1
        var r2 = -1
        if (H264Const.usesList(p0, list)) {
            val mvpX1 = calcMVPrediction16x8Top(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX shl 2, list),
                    s.mvTop.getMv((mbX shl 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    trAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 0)
            val mvpY1 = calcMVPrediction16x8Top(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX shl 2, list),
                    s.mvTop.getMv((mbX shl 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    trAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 1)
            mvX1 = mBlock.pb168x168.mvdX1[list] + mvpX1
            mvY1 = mBlock.pb168x168.mvdY1[list] + mvpY1
            MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb168x168.mvdX1[list],
                    mBlock.pb168x168.mvdY1[list], mvX1, mvY1, mBlock.pb168x168.refIdx1[list])
            interpolator.getBlockLuma(references!![list]!![mBlock.pb168x168.refIdx1[list]]!!, mb, 0, (mbX shl 6) + mvX1,
                    (mbY shl 6) + mvY1, 16, 8)
            r1 = mBlock.pb168x168.refIdx1[list]
        }
        val v1 = Mv.packMv(mvX1, mvY1, r1)
        if (H264Const.usesList(p1, list)) {
            val mvpX2 = calcMVPrediction16x8Bottom(s.mvLeft.getMv(2, list), v1, MBlockDecoderUtils.NULL_VECTOR, s.mvLeft.getMv(1, list),
                    leftAvailable, true, false, leftAvailable, mBlock.pb168x168.refIdx2[list], 0)
            val mvpY2 = calcMVPrediction16x8Bottom(s.mvLeft.getMv(2, list), v1, MBlockDecoderUtils.NULL_VECTOR, s.mvLeft.getMv(1, list),
                    leftAvailable, true, false, leftAvailable, mBlock.pb168x168.refIdx2[list], 1)
            mvX2 = mBlock.pb168x168.mvdX2[list] + mvpX2
            mvY2 = mBlock.pb168x168.mvdY2[list] + mvpY2
            MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb168x168.mvdX2[list],
                    mBlock.pb168x168.mvdY2[list], mvX2, mvY2, mBlock.pb168x168.refIdx2[list])
            interpolator.getBlockLuma(references!![list]!![mBlock.pb168x168.refIdx2[list]]!!, mb, 128,
                    (mbX shl 6) + mvX2, (mbY shl 6) + 32 + mvY2, 16, 8)
            r2 = mBlock.pb168x168.refIdx2[list]
        }
        val v2 = Mv.packMv(mvX2, mvY2, r2)
        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list))
        MBlockDecoderUtils.saveVect(s.mvLeft, list, 0, 2, v1)
        MBlockDecoderUtils.saveVect(s.mvLeft, list, 2, 4, v2)
        MBlockDecoderUtils.saveVect(s.mvTop, list, xx, xx + 4, v2)
        for (i in 0..7) {
            x.setMv(i, list, v1)
        }
        for (i in 8..15) {
            x.setMv(i, list, v2)
        }
    }

    fun decode16x8(mBlock: MBlock, mb: Picture, refs: Array<Array<Frame?>?>?, p0: PartPred, p1: PartPred) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        val topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx)
        val topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx)
        val address = mapper.getAddress(mBlock.mbIdx)
        val xx = mbX shl 2
        for (list in 0..1) {
            predictInter16x8(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, xx, mBlock.x, p0, p1, list)
        }
        PredictionMerger.mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                0, 16, 16, 8, mb.getPlaneData(0), refs, poc)
        PredictionMerger.mergePrediction(sh, mBlock.x.mv0R(8), mBlock.x.mv1R(8), p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                128, 16, 16, 8, mb.getPlaneData(0), refs, poc)
        mBlock.partPreds[1] = p0
        mBlock.partPreds[0] = mBlock.partPreds[1]
        mBlock.partPreds[3] = p1
        mBlock.partPreds[2] = mBlock.partPreds[3]
        predictChromaInter(refs, mBlock.x, mbX shl 3, mbY shl 3, 1, mb, mBlock.partPreds)
        predictChromaInter(refs, mBlock.x, mbX shl 3, mbY shl 3, 2, mb, mBlock.partPreds)
        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, mapper.getAddress(mBlock.mbIdx))
        MBlockDecoderUtils.saveMvs(di, mBlock.x, mbX, mbY)
        MBlockDecoderUtils.mergeResidual(mb, mBlock.ac, if (mBlock.transform8x8Used) H264Const.COMP_BLOCK_8x8_LUT else H264Const.COMP_BLOCK_4x4_LUT,
                if (mBlock.transform8x8Used) H264Const.COMP_POS_8x8_LUT else H264Const.COMP_POS_4x4_LUT)
        MBlockDecoderUtils.collectPredictors(s, mb, mbX)
        di.mbTypes[address] = mBlock.curMbType
    }

    fun decode8x16(mBlock: MBlock, mb: Picture, refs: Array<Array<Frame?>?>?, p0: PartPred, p1: PartPred) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        val topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx)
        val topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx)
        val address = mapper.getAddress(mBlock.mbIdx)
        for (list in 0..1) {
            predictInter8x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, mBlock.x, list, p0, p1)
        }
        PredictionMerger.mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                0, 16, 8, 16, mb.getPlaneData(0), refs, poc)
        PredictionMerger.mergePrediction(sh, mBlock.x.mv0R(2), mBlock.x.mv1R(2), p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
                8, 16, 8, 16, mb.getPlaneData(0), refs, poc)
        mBlock.partPreds[2] = p0
        mBlock.partPreds[0] = mBlock.partPreds[2]
        mBlock.partPreds[3] = p1
        mBlock.partPreds[1] = mBlock.partPreds[3]
        predictChromaInter(refs, mBlock.x, mbX shl 3, mbY shl 3, 1, mb, mBlock.partPreds)
        predictChromaInter(refs, mBlock.x, mbX shl 3, mbY shl 3, 2, mb, mBlock.partPreds)
        residualInter(mBlock, refs, leftAvailable, topAvailable, mbX, mbY, mapper.getAddress(mBlock.mbIdx))
        MBlockDecoderUtils.saveMvs(di, mBlock.x, mbX, mbY)
        MBlockDecoderUtils.mergeResidual(mb, mBlock.ac, if (mBlock.transform8x8Used) H264Const.COMP_BLOCK_8x8_LUT else H264Const.COMP_BLOCK_4x4_LUT,
                if (mBlock.transform8x8Used) H264Const.COMP_POS_8x8_LUT else H264Const.COMP_POS_4x4_LUT)
        MBlockDecoderUtils.collectPredictors(s, mb, mbX)
        di.mbTypes[address] = mBlock.curMbType
    }

    fun predictInter16x16(mBlock: MBlock, mb: Picture?, references: Array<Array<Frame?>?>?, mbX: Int, mbY: Int,
                          leftAvailable: Boolean, topAvailable: Boolean, tlAvailable: Boolean, trAvailable: Boolean, x: MvList, xx: Int,
                          list: Int, curPred: PartPred?) {
        var mvX = 0
        var mvY = 0
        var r = -1
        if (H264Const.usesList(curPred, list)) {
            val mvpX = MBlockDecoderUtils.calcMVPredictionMedian(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX shl 2, list),
                    s.mvTop.getMv((mbX shl 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    trAvailable, tlAvailable, mBlock.pb16x16.refIdx[list], 0)
            val mvpY = MBlockDecoderUtils.calcMVPredictionMedian(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX shl 2, list),
                    s.mvTop.getMv((mbX shl 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                    trAvailable, tlAvailable, mBlock.pb16x16.refIdx[list], 1)
            mvX = mBlock.pb16x16.mvdX[list] + mvpX
            mvY = mBlock.pb16x16.mvdY[list] + mvpY
            MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX, mvpY, mBlock.pb16x16.mvdX[list],
                    mBlock.pb16x16.mvdY[list], mvX, mvY, mBlock.pb16x16.refIdx[list])
            r = mBlock.pb16x16.refIdx[list]
            interpolator.getBlockLuma(references!![list]!![r]!!, mb!!, 0, (mbX shl 6) + mvX, (mbY shl 6) + mvY, 16, 16)
        }
        val v = Mv.packMv(mvX, mvY, r)
        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list))
        MBlockDecoderUtils.saveVect(s.mvTop, list, xx, xx + 4, v)
        MBlockDecoderUtils.saveVect(s.mvLeft, list, 0, 4, v)
        for (i in 0..15) {
            x.setMv(i, list, v)
        }
    }

    private fun residualInter(mBlock: MBlock, refs: Array<Array<Frame?>?>?, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int,
                              mbY: Int, mbAddr: Int) {
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52
        }
        di.mbQps[0][mbAddr] = s.qp
        residualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY)
        if (s.chromaFormat != ColorSpace.MONO) {
            val qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0])
            val qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1])
            decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, qp1, qp2)
            di.mbQps[1][mbAddr] = qp1
            di.mbQps[2][mbAddr] = qp2
        }
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used
    }

    fun calcMVPrediction16x8Top(a: Int, b: Int, c: Int, d: Int, aAvb: Boolean, bAvb: Boolean, cAvb: Boolean,
                                dAvb: Boolean, refIdx: Int, comp: Int): Int {
        return if (bAvb && Mv.mvRef(b) == refIdx) Mv.mvC(b, comp) else MBlockDecoderUtils.calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp)
    }

    fun calcMVPrediction16x8Bottom(a: Int, b: Int, c: Int, d: Int, aAvb: Boolean, bAvb: Boolean, cAvb: Boolean,
                                   dAvb: Boolean, refIdx: Int, comp: Int): Int {
        return if (aAvb && Mv.mvRef(a) == refIdx) Mv.mvC(a, comp) else MBlockDecoderUtils.calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp)
    }

    fun calcMVPrediction8x16Left(a: Int, b: Int, c: Int, d: Int, aAvb: Boolean, bAvb: Boolean, cAvb: Boolean,
                                 dAvb: Boolean, refIdx: Int, comp: Int): Int {
        return if (aAvb && Mv.mvRef(a) == refIdx) Mv.mvC(a, comp) else MBlockDecoderUtils.calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp)
    }

    fun calcMVPrediction8x16Right(a: Int, b: Int, c: Int, d: Int, aAvb: Boolean, bAvb: Boolean, cAvb: Boolean,
                                  dAvb: Boolean, refIdx: Int, comp: Int): Int {
        val lc = if (cAvb) c else if (dAvb) d else MBlockDecoderUtils.NULL_VECTOR
        return if (Mv.mvRef(lc) == refIdx) Mv.mvC(lc, comp) else MBlockDecoderUtils.calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp)
    }

}