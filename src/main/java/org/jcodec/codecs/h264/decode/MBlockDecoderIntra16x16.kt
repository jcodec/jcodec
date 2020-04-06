package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.decode.aso.Mapper
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.common.model.Picture

/**
 * A decoder for I16x16 macroblocks
 *
 * @author The JCodec project
 */
class MBlockDecoderIntra16x16(private val mapper: Mapper, sh: SliceHeader?, di: DeblockerInput?, poc: Int,
                              decoderState: DecoderState?) : MBlockDecoderBase(sh, di, poc, decoderState) {
    fun decode(mBlock: MBlock, mb: Picture) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val address = mapper.getAddress(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52
        di.mbQps[0][address] = s.qp
        residualLumaI16x16(mBlock, leftAvailable, topAvailable, mbX, mbY)
        Intra16x16PredictionBuilder.predictWithMode(mBlock.luma16x16Mode, mBlock.ac[0], leftAvailable, topAvailable,
                s.leftRow[0], s.topLine[0], s.topLeft[0], mbX shl 4, mb.getPlaneData(0))
        decodeChroma(mBlock, mbX, mbY, leftAvailable, topAvailable, mb, s.qp)
        di.mbTypes[address] = mBlock.curMbType
        MBlockDecoderUtils.collectPredictors(s, mb, mbX)
        MBlockDecoderUtils.saveMvsIntra(di, mbX, mbY)
        MBlockDecoderUtils.saveVectIntra(s, mapper.getMbX(mBlock.mbIdx))
    }

    private fun residualLumaI16x16(mBlock: MBlock, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, mbY: Int) {
        CoeffTransformer.invDC4x4(mBlock.dc)
        val scalingList = getScalingList(0)
        CoeffTransformer.dequantizeDC4x4(mBlock.dc, s.qp, scalingList)
        CoeffTransformer.reorderDC4x4(mBlock.dc)
        for (i in 0..15) {
            if (mBlock.cbpLuma() and (1 shl (i shr 2)) != 0) {
                CoeffTransformer.dequantizeAC(mBlock.ac[0][i], s.qp, scalingList)
            }
            mBlock.ac[0][i][0] = mBlock.dc[i]
            CoeffTransformer.idct4x4(mBlock.ac[0][i])
        }
    }

}