package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.H264Utils.Mv
import org.jcodec.codecs.h264.H264Utils.MvList
import org.jcodec.codecs.h264.decode.aso.Mapper
import org.jcodec.codecs.h264.io.model.Frame
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.common.model.Picture
import java.util.*

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 *
 * @author The JCodec project
 */
class MBlockDecoderInter8x8(private val mapper: Mapper, private val bDirectDecoder: MBlockDecoderBDirect, sh: SliceHeader?, di: DeblockerInput?,
                            poc: Int, decoderState: DecoderState?) : MBlockDecoderBase(sh!!, di!!, poc, decoderState!!) {
    fun decode(mBlock: MBlock, references: Array<Array<Frame?>?>?, mb: Picture, sliceType: SliceType, ref0: Boolean) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        val mbAddr = mapper.getAddress(mBlock.mbIdx)
        val topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx)
        val topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx)
        if (sliceType == SliceType.P) {
            predict8x8P(mBlock, references!![0], mb, ref0, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, mBlock.x, mBlock.partPreds)
        } else {
            predict8x8B(mBlock, references, mb, ref0, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                    topRightAvailable, mBlock.x, mBlock.partPreds)
        }
        predictChromaInter(references, mBlock.x, mbX shl 3, mbY shl 3, 1, mb, mBlock.partPreds)
        predictChromaInter(references, mBlock.x, mbX shl 3, mbY shl 3, 2, mb, mBlock.partPreds)
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52
        }
        di.mbQps[0][mbAddr] = s.qp
        residualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY)
        MBlockDecoderUtils.saveMvs(di, mBlock.x, mbX, mbY)
        val qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0])
        val qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1])
        decodeChromaResidual(mBlock, leftAvailable, topAvailable, mbX, mbY, qp1, qp2)
        di.mbQps[1][mbAddr] = qp1
        di.mbQps[2][mbAddr] = qp2
        MBlockDecoderUtils.mergeResidual(mb, mBlock.ac, if (mBlock.transform8x8Used) H264Const.COMP_BLOCK_8x8_LUT else H264Const.COMP_BLOCK_4x4_LUT,
                if (mBlock.transform8x8Used) H264Const.COMP_POS_8x8_LUT else H264Const.COMP_POS_4x4_LUT)
        MBlockDecoderUtils.collectPredictors(s, mb, mbX)
        di.mbTypes[mbAddr] = mBlock.curMbType
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used
    }

    private fun predict8x8P(mBlock: MBlock, references: Array<Frame?>?, mb: Picture, ref0: Boolean, mbX: Int, mbY: Int,
                            leftAvailable: Boolean, topAvailable: Boolean, tlAvailable: Boolean, topRightAvailable: Boolean, x: MvList,
                            pp: Array<PartPred?>) {
        decodeSubMb8x8(mBlock, 0, mBlock.pb8x8.subMbTypes[0], references, mbX shl 6, mbY shl 6, s.mvTopLeft.getMv(0, 0),
                s.mvTop.getMv(mbX shl 2, 0), s.mvTop.getMv((mbX shl 2) + 1, 0), s.mvTop.getMv((mbX shl 2) + 2, 0),
                s.mvLeft.getMv(0, 0), s.mvLeft.getMv(1, 0), tlAvailable, topAvailable, topAvailable, leftAvailable,
                mBlock.x, 0, 1, 4, 5, mBlock.pb8x8.refIdx[0][0], mb, 0, 0)
        decodeSubMb8x8(mBlock, 1, mBlock.pb8x8.subMbTypes[1], references, (mbX shl 6) + 32, mbY shl 6,
                s.mvTop.getMv((mbX shl 2) + 1, 0), s.mvTop.getMv((mbX shl 2) + 2, 0), s.mvTop.getMv((mbX shl 2) + 3, 0),
                s.mvTop.getMv((mbX shl 2) + 4, 0), x.getMv(1, 0), x.getMv(5, 0), topAvailable, topAvailable,
                topRightAvailable, true, x, 2, 3, 6, 7, mBlock.pb8x8.refIdx[0][1], mb, 8, 0)
        decodeSubMb8x8(mBlock, 2, mBlock.pb8x8.subMbTypes[2], references, mbX shl 6, (mbY shl 6) + 32,
                s.mvLeft.getMv(1, 0), x.getMv(4, 0), x.getMv(5, 0), x.getMv(6, 0), s.mvLeft.getMv(2, 0),
                s.mvLeft.getMv(3, 0), leftAvailable, true, true, leftAvailable, x, 8, 9, 12, 13,
                mBlock.pb8x8.refIdx[0][2], mb, 128, 0)
        decodeSubMb8x8(mBlock, 3, mBlock.pb8x8.subMbTypes[3], references, (mbX shl 6) + 32, (mbY shl 6) + 32,
                x.getMv(5, 0), x.getMv(6, 0), x.getMv(7, 0), MBlockDecoderUtils.NULL_VECTOR, x.getMv(9, 0), x.getMv(13, 0), true, true,
                false, true, x, 10, 11, 14, 15, mBlock.pb8x8.refIdx[0][3], mb, 136, 0)
        for (i in 0..3) {
            // TODO(stan): refactor this
            val blk4x4 = H264Const.BLK8x8_BLOCKS[i][0]
            PredictionMerger.weightPrediction(sh, x.mv0R(blk4x4), 0, mb.getPlaneData(0), H264Const.BLK_8x8_MB_OFF_LUMA[i], 16, 8,
                    8, mb.getPlaneData(0))
        }
        MBlockDecoderUtils.savePrediction8x8(s, mbX, x)
        Arrays.fill(pp, PartPred.L0)
    }

    private fun predict8x8B(mBlock: MBlock, refs: Array<Array<Frame?>?>?, mb: Picture, ref0: Boolean, mbX: Int, mbY: Int,
                            leftAvailable: Boolean, topAvailable: Boolean, tlAvailable: Boolean, topRightAvailable: Boolean, x: MvList,
                            p: Array<PartPred?>) {
        for (i in 0..3) {
            p[i] = H264Const.bPartPredModes[mBlock.pb8x8.subMbTypes[i]]
        }
        for (i in 0..3) {
            if (p[i] == PartPred.Direct) bDirectDecoder.predictBDirect(refs, mbX, mbY, leftAvailable, topAvailable, tlAvailable,
                    topRightAvailable, x, p, mb, H264Const.ARRAY[i])
        }
        for (list in 0..1) {
            if (H264Const.usesList(H264Const.bPartPredModes[mBlock.pb8x8.subMbTypes[0]], list)) {
                decodeSubMb8x8(mBlock, 0, H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[0]], refs!![list], mbX shl 6, mbY shl 6,
                        s.mvTopLeft.getMv(0, list), s.mvTop.getMv(mbX shl 2, list), s.mvTop.getMv((mbX shl 2) + 1, list),
                        s.mvTop.getMv((mbX shl 2) + 2, list), s.mvLeft.getMv(0, list), s.mvLeft.getMv(1, list),
                        tlAvailable, topAvailable, topAvailable, leftAvailable, x, 0, 1, 4, 5,
                        mBlock.pb8x8.refIdx[list][0], mbb[list], 0, list)
            }
            if (H264Const.usesList(H264Const.bPartPredModes[mBlock.pb8x8.subMbTypes[1]], list)) {
                decodeSubMb8x8(mBlock, 1, H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[1]], refs!![list], (mbX shl 6) + 32,
                        mbY shl 6, s.mvTop.getMv((mbX shl 2) + 1, list), s.mvTop.getMv((mbX shl 2) + 2, list),
                        s.mvTop.getMv((mbX shl 2) + 3, list), s.mvTop.getMv((mbX shl 2) + 4, list), x.getMv(1, list),
                        x.getMv(5, list), topAvailable, topAvailable, topRightAvailable, true, x, 2, 3, 6, 7,
                        mBlock.pb8x8.refIdx[list][1], mbb[list], 8, list)
            }
            if (H264Const.usesList(H264Const.bPartPredModes[mBlock.pb8x8.subMbTypes[2]], list)) {
                decodeSubMb8x8(mBlock, 2, H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[2]], refs!![list], mbX shl 6,
                        (mbY shl 6) + 32, s.mvLeft.getMv(1, list), x.getMv(4, list), x.getMv(5, list), x.getMv(6, list),
                        s.mvLeft.getMv(2, list), s.mvLeft.getMv(3, list), leftAvailable, true, true, leftAvailable, x,
                        8, 9, 12, 13, mBlock.pb8x8.refIdx[list][2], mbb[list], 128, list)
            }
            if (H264Const.usesList(H264Const.bPartPredModes[mBlock.pb8x8.subMbTypes[3]], list)) {
                decodeSubMb8x8(mBlock, 3, H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[3]], refs!![list], (mbX shl 6) + 32,
                        (mbY shl 6) + 32, x.getMv(5, list), x.getMv(6, list), x.getMv(7, list), MBlockDecoderUtils.NULL_VECTOR,
                        x.getMv(9, list), x.getMv(13, list), true, true, false, true, x, 10, 11, 14, 15,
                        mBlock.pb8x8.refIdx[list][3], mbb[list], 136, list)
            }
        }
        for (i in 0..3) {
            val blk4x4 = H264Const.BLK8x8_BLOCKS[i][0]
            PredictionMerger.mergePrediction(sh, x.mv0R(blk4x4), x.mv1R(blk4x4), H264Const.bPartPredModes[mBlock.pb8x8.subMbTypes[i]], 0,
                    mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), H264Const.BLK_8x8_MB_OFF_LUMA[i], 16, 8, 8,
                    mb.getPlaneData(0), refs, poc)
        }
        MBlockDecoderUtils.savePrediction8x8(s, mbX, x)
    }

    private fun decodeSubMb8x8(mBlock: MBlock, partNo: Int, subMbType: Int, references: Array<Frame?>?, offX: Int, offY: Int,
                               tl: Int, t0: Int, t1: Int, tr: Int, l0: Int, l1: Int, tlAvb: Boolean, tAvb: Boolean, trAvb: Boolean, lAvb: Boolean,
                               x: MvList, i00: Int, i01: Int, i10: Int, i11: Int, refIdx: Int, mb: Picture, off: Int, list: Int) {
        when (subMbType) {
            3 -> decodeSub4x4(mBlock, partNo, references, offX, offY, tl, t0, t1, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x,
                    i00, i01, i10, i11, refIdx, mb, off, list)
            2 -> decodeSub4x8(mBlock, partNo, references, offX, offY, tl, t0, t1, tr, l0, tlAvb, tAvb, trAvb, lAvb, x, i00,
                    i01, i10, i11, refIdx, mb, off, list)
            1 -> decodeSub8x4(mBlock, partNo, references, offX, offY, tl, t0, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x, i00,
                    i01, i10, i11, refIdx, mb, off, list)
            0 -> decodeSub8x8(mBlock, partNo, references, offX, offY, tl, t0, tr, l0, tlAvb, tAvb, trAvb, lAvb, x, i00, i01,
                    i10, i11, refIdx, mb, off, list)
        }
    }

    private fun decodeSub8x8(mBlock: MBlock, partNo: Int, references: Array<Frame?>?, offX: Int, offY: Int, tl: Int,
                             t0: Int, tr: Int, l0: Int, tlAvb: Boolean, tAvb: Boolean, trAvb: Boolean, lAvb: Boolean, x: MvList, i00: Int,
                             i01: Int, i10: Int, i11: Int, refIdx: Int, mb: Picture, off: Int, list: Int) {
        val mvpX = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0)
        val mvpY = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1)
        val mv = Mv.packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX, mBlock.pb8x8.mvdY1[list][partNo] + mvpY, refIdx)
        x.setMv(i00, list, mv)
        x.setMv(i01, list, mv)
        x.setMv(i10, list, mv)
        x.setMv(i11, list, mv)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX, mvpY, mBlock.pb8x8.mvdX1[list][partNo],
                mBlock.pb8x8.mvdY1[list][partNo], Mv.mvX(mv), Mv.mvY(mv), refIdx)
        interpolator.getBlockLuma(references!![refIdx]!!, mb, off, offX + Mv.mvX(mv), offY + Mv.mvY(mv), 8, 8)
    }

    private fun decodeSub8x4(mBlock: MBlock, partNo: Int, references: Array<Frame?>?, offX: Int, offY: Int, tl: Int,
                             t0: Int, tr: Int, l0: Int, l1: Int, tlAvb: Boolean, tAvb: Boolean, trAvb: Boolean, lAvb: Boolean, x: MvList,
                             i00: Int, i01: Int, i10: Int, i11: Int, refIdx: Int, mb: Picture, off: Int, list: Int) {
        val mvpX1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0)
        val mvpY1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1)

        // TODO(stan): check if MVs need to be clipped
        val mv1 = Mv.packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx)
        x.setMv(i00, list, mv1)
        x.setMv(i01, list, mv1)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb8x8.mvdX1[list][partNo],
                mBlock.pb8x8.mvdY1[list][partNo], Mv.mvX(mv1), Mv.mvY(mv1), refIdx)
        val mvpX2 = MBlockDecoderUtils.calcMVPredictionMedian(l1, mv1, MBlockDecoderUtils.NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 0)
        val mvpY2 = MBlockDecoderUtils.calcMVPredictionMedian(l1, mv1, MBlockDecoderUtils.NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 1)
        val mv2 = Mv.packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx)
        x.setMv(i10, list, mv2)
        x.setMv(i11, list, mv2)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb8x8.mvdX2[list][partNo],
                mBlock.pb8x8.mvdY2[list][partNo], Mv.mvX(mv2), Mv.mvY(mv2), refIdx)
        interpolator.getBlockLuma(references!![refIdx]!!, mb, off, offX + Mv.mvX(mv1), offY + Mv.mvY(mv1), 8, 4)
        interpolator.getBlockLuma(references[refIdx]!!, mb, off + mb.width * 4, offX + Mv.mvX(mv2),
                offY + Mv.mvY(mv2) + 16, 8, 4)
    }

    private fun decodeSub4x8(mBlock: MBlock, partNo: Int, references: Array<Frame?>?, offX: Int, offY: Int, tl: Int, t0: Int,
                             t1: Int, tr: Int, l0: Int, tlAvb: Boolean, tAvb: Boolean, trAvb: Boolean, lAvb: Boolean, x: MvList, i00: Int,
                             i01: Int, i10: Int, i11: Int, refIdx: Int, mb: Picture, off: Int, list: Int) {
        val mvpX1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0)
        val mvpY1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1)
        val mv1 = Mv.packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx)
        x.setMv(i00, list, mv1)
        x.setMv(i10, list, mv1)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb8x8.mvdX1[list][partNo],
                mBlock.pb8x8.mvdY1[list][partNo], Mv.mvX(mv1), Mv.mvY(mv1), refIdx)
        val mvpX2 = MBlockDecoderUtils.calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0)
        val mvpY2 = MBlockDecoderUtils.calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1)
        val mv2 = Mv.packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx)
        x.setMv(i01, list, mv2)
        x.setMv(i11, list, mv2)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb8x8.mvdX2[list][partNo],
                mBlock.pb8x8.mvdY2[list][partNo], Mv.mvX(mv2), Mv.mvY(mv2), refIdx)
        interpolator.getBlockLuma(references!![refIdx]!!, mb, off, offX + Mv.mvX(mv1), offY + Mv.mvY(mv1), 4, 8)
        interpolator.getBlockLuma(references[refIdx]!!, mb, off + 4, offX + Mv.mvX(mv2) + 16, offY + Mv.mvY(mv2), 4, 8)
    }

    private fun decodeSub4x4(mBlock: MBlock, partNo: Int, references: Array<Frame?>?, offX: Int, offY: Int, tl: Int,
                             t0: Int, t1: Int, tr: Int, l0: Int, l1: Int, tlAvb: Boolean, tAvb: Boolean, trAvb: Boolean, lAvb: Boolean, x: MvList,
                             i00: Int, i01: Int, i10: Int, i11: Int, refIdx: Int, mb: Picture, off: Int, list: Int) {
        val mvpX1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0)
        val mvpY1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1)
        val mv1 = Mv.packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx)
        x.setMv(i00, list, mv1)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX1, mvpY1, mBlock.pb8x8.mvdX1[list][partNo],
                mBlock.pb8x8.mvdY1[list][partNo], Mv.mvX(mv1), Mv.mvY(mv1), refIdx)
        val mvpX2 = MBlockDecoderUtils.calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0)
        val mvpY2 = MBlockDecoderUtils.calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1)
        val mv2 = Mv.packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx)
        x.setMv(i01, list, mv2)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX2, mvpY2, mBlock.pb8x8.mvdX2[list][partNo],
                mBlock.pb8x8.mvdY2[list][partNo], Mv.mvX(mv2), Mv.mvY(mv2), refIdx)
        val mvpX3 = MBlockDecoderUtils.calcMVPredictionMedian(l1, mv1, mv2, l0, lAvb, true, true, lAvb, refIdx, 0)
        val mvpY3 = MBlockDecoderUtils.calcMVPredictionMedian(l1, mv1, mv2, l0, lAvb, true, true, lAvb, refIdx, 1)
        val mv3 = Mv.packMv(mBlock.pb8x8.mvdX3[list][partNo] + mvpX3, mBlock.pb8x8.mvdY3[list][partNo] + mvpY3, refIdx)
        x.setMv(i10, list, mv3)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX3, mvpY3, mBlock.pb8x8.mvdX3[list][partNo],
                mBlock.pb8x8.mvdY3[list][partNo], Mv.mvX(mv3), Mv.mvY(mv3), refIdx)
        val mvpX4 = MBlockDecoderUtils.calcMVPredictionMedian(mv3, mv2, MBlockDecoderUtils.NULL_VECTOR, mv1, true, true, false, true, refIdx, 0)
        val mvpY4 = MBlockDecoderUtils.calcMVPredictionMedian(mv3, mv2, MBlockDecoderUtils.NULL_VECTOR, mv1, true, true, false, true, refIdx, 1)
        val mv4 = Mv.packMv(mBlock.pb8x8.mvdX4[list][partNo] + mvpX4, mBlock.pb8x8.mvdY4[list][partNo] + mvpY4, refIdx)
        x.setMv(i11, list, mv4)
        MBlockDecoderUtils.debugPrint("MVP: (%d, %d), MVD: (%d, %d), MV: (%d,%d,%d)", mvpX4, mvpY4, mBlock.pb8x8.mvdX4[list][partNo],
                mBlock.pb8x8.mvdY4[list][partNo], Mv.mvX(mv4), Mv.mvY(mv4), refIdx)
        interpolator.getBlockLuma(references!![refIdx]!!, mb, off, offX + Mv.mvX(mv1), offY + Mv.mvY(mv1), 4, 4)
        interpolator.getBlockLuma(references[refIdx]!!, mb, off + 4, offX + Mv.mvX(mv2) + 16, offY + Mv.mvY(mv2), 4, 4)
        interpolator.getBlockLuma(references[refIdx]!!, mb, off + mb.width * 4, offX + Mv.mvX(mv3), offY + Mv.mvY(mv3)
                + 16, 4, 4)
        interpolator.getBlockLuma(references[refIdx]!!, mb, off + mb.width * 4 + 4, offX + Mv.mvX(mv4) + 16, offY
                + Mv.mvY(mv4) + 16, 4, 4)
    }

}