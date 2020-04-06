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
 * A decoder for P skip macroblocks
 *
 * @author The JCodec project
 */
class MBlockSkipDecoder(private val mapper: Mapper, private val bDirectDecoder: MBlockDecoderBDirect,
                        sh: SliceHeader?, di: DeblockerInput?, poc: Int, sharedState: DecoderState?) : MBlockDecoderBase(sh!!, di!!, poc, sharedState!!) {
    fun decodeSkip(mBlock: MBlock, refs: Array<Array<Frame?>>, mb: Picture, sliceType: SliceType) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val mbAddr = mapper.getAddress(mBlock.mbIdx)
        if (sliceType == SliceType.P) {
            predictPSkip(refs, mbX, mbY, mapper.leftAvailable(mBlock.mbIdx), mapper.topAvailable(mBlock.mbIdx),
                    mapper.topLeftAvailable(mBlock.mbIdx), mapper.topRightAvailable(mBlock.mbIdx), mBlock.x, mb)
            Arrays.fill(mBlock.partPreds, PartPred.L0)
        } else {
            bDirectDecoder.predictBDirect(refs, mbX, mbY, mapper.leftAvailable(mBlock.mbIdx),
                    mapper.topAvailable(mBlock.mbIdx), mapper.topLeftAvailable(mBlock.mbIdx),
                    mapper.topRightAvailable(mBlock.mbIdx), mBlock.x, mBlock.partPreds, mb, H264Const.identityMapping4)
            MBlockDecoderUtils.savePrediction8x8(s, mbX, mBlock.x)
        }
        decodeChromaSkip(refs, mBlock.x, mBlock.partPreds, mbX, mbY, mb)
        MBlockDecoderUtils.collectPredictors(s, mb, mbX)
        MBlockDecoderUtils.saveMvs(di, mBlock.x, mbX, mbY)
        di.mbTypes[mbAddr] = mBlock.curMbType
        di.mbQps[0][mbAddr] = s.qp
        di.mbQps[1][mbAddr] = calcQpChroma(s.qp, s.chromaQpOffset[0])
        di.mbQps[2][mbAddr] = calcQpChroma(s.qp, s.chromaQpOffset[1])
    }

    fun predictPSkip(refs: Array<Array<Frame?>>, mbX: Int, mbY: Int, lAvb: Boolean, tAvb: Boolean, tlAvb: Boolean,
                     trAvb: Boolean, x: MvList, mb: Picture) {
        var mvX = 0
        var mvY = 0
        if (lAvb && tAvb) {
            val b = s.mvTop.getMv(mbX shl 2, 0)
            val a = s.mvLeft.getMv(0, 0)
            if (a != 0 && b != 0) {
                mvX = MBlockDecoderUtils.calcMVPredictionMedian(a, b, s.mvTop.getMv((mbX shl 2) + 4, 0), s.mvTopLeft.getMv(0, 0), lAvb,
                        tAvb, trAvb, tlAvb, 0, 0)
                mvY = MBlockDecoderUtils.calcMVPredictionMedian(a, b, s.mvTop.getMv((mbX shl 2) + 4, 0), s.mvTopLeft.getMv(0, 0), lAvb,
                        tAvb, trAvb, tlAvb, 0, 1)
            }
        }
        val xx = mbX shl 2
        s.mvTopLeft.copyPair(0, s.mvTop, xx + 3)
        MBlockDecoderUtils.saveVect(s.mvTop, 0, xx, xx + 4, Mv.packMv(mvX, mvY, 0))
        MBlockDecoderUtils.saveVect(s.mvLeft, 0, 0, 4, Mv.packMv(mvX, mvY, 0))
        MBlockDecoderUtils.saveVect(s.mvTop, 1, xx, xx + 4, MBlockDecoderUtils.NULL_VECTOR)
        MBlockDecoderUtils.saveVect(s.mvLeft, 1, 0, 4, MBlockDecoderUtils.NULL_VECTOR)
        for (i in 0..15) {
            x.setMv(i, 0, Mv.packMv(mvX, mvY, 0))
        }
        interpolator.getBlockLuma(refs[0][0]!!, mb, 0, (mbX shl 6) + mvX, (mbY shl 6) + mvY, 16, 16)
        PredictionMerger.mergePrediction(sh, 0, 0, PartPred.L0, 0, mb.getPlaneData(0), null, 0, 16, 16, 16, mb.getPlaneData(0),
                refs, poc)
    }

    fun decodeChromaSkip(reference: Array<Array<Frame?>>?, vectors: MvList?, pp: Array<PartPred?>?, mbX: Int, mbY: Int, mb: Picture?) {
        predictChromaInter(reference!!, vectors!!, mbX shl 3, mbY shl 3, 1, mb!!, pp!!)
        predictChromaInter(reference, vectors, mbX shl 3, mbY shl 3, 2, mb, pp)
    }

}