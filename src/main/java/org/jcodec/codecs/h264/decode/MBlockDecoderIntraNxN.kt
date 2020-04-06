package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.decode.aso.Mapper
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.common.model.Picture

/**
 * A decoder for I16x16 macroblocks
 *
 * @author The JCodec project
 */
class MBlockDecoderIntraNxN(private val mapper: Mapper, sh: SliceHeader?, di: DeblockerInput?, poc: Int,
                            decoderState: DecoderState?) : MBlockDecoderBase(sh!!, di!!, poc, decoderState!!) {
    private val prediction8x8Builder: Intra8x8PredictionBuilder
    fun decode(mBlock: MBlock, mb: Picture) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val mbAddr = mapper.getAddress(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        val topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx)
        val topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx)
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52
        }
        di.mbQps[0][mbAddr] = s.qp
        residualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY)
        if (!mBlock.transform8x8Used) {
            for (i in 0..15) {
                val blkX = i and 3 shl 2
                val blkY = i and 3.inv()
                val bi = H264Const.BLK_INV_MAP[i]
                val trAvailable = ((bi == 0 || bi == 1 || bi == 4) && topAvailable
                        || bi == 5 && topRightAvailable || bi == 2 || bi == 6 || bi == 8 || bi == 9 || bi == 10 || bi == 12 || bi == 14)
                Intra4x4PredictionBuilder.predictWithMode(mBlock.lumaModes[bi], mBlock.ac[0][bi],
                        if (blkX == 0) leftAvailable else true, if (blkY == 0) topAvailable else true, trAvailable, s.leftRow[0],
                        s.topLine[0], s.topLeft[0], mbX shl 4, blkX, blkY, mb.getPlaneData(0))
            }
        } else {
            for (i in 0..3) {
                val blkX = i and 1 shl 1
                val blkY = i and 2
                val trAvailable = i == 0 && topAvailable || i == 1 && topRightAvailable || i == 2
                val tlAvailable = if (i == 0) topLeftAvailable else if (i == 1) topAvailable else if (i == 2) leftAvailable else true
                prediction8x8Builder.predictWithMode(mBlock.lumaModes[i], mBlock.ac[0][i],
                        if (blkX == 0) leftAvailable else true, if (blkY == 0) topAvailable else true, tlAvailable, trAvailable,
                        s.leftRow[0], s.topLine[0], s.topLeft[0], mbX shl 4, blkX shl 2, blkY shl 2, mb.getPlaneData(0))
            }
        }
        decodeChroma(mBlock, mbX, mbY, leftAvailable, topAvailable, mb, s.qp)
        di.mbTypes[mbAddr] = mBlock.curMbType
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used
        MBlockDecoderUtils.collectChromaPredictors(s, mb, mbX)
        MBlockDecoderUtils.saveMvsIntra(di, mbX, mbY)
        MBlockDecoderUtils.saveVectIntra(s, mapper.getMbX(mBlock.mbIdx))
    }

    init {
        prediction8x8Builder = Intra8x8PredictionBuilder()
    }
}