package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.decode.aso.Mapper
import org.jcodec.common.model.Picture

/**
 * A decoder for Intra PCM macroblocks
 *
 * @author The JCodec project
 */
class MBlockDecoderIPCM(private val mapper: Mapper, private val s: DecoderState) {
    fun decode(mBlock: MBlock, mb: Picture?) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        MBlockDecoderUtils.collectPredictors(s, mb, mbX)
        MBlockDecoderUtils.saveVectIntra(s, mapper.getMbX(mBlock.mbIdx))
    }

}