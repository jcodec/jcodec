package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVectIntra;

import org.jcodec.codecs.h264.decode.aso.Mapper;

/**
 * A decoder for Intra PCM macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderIPCM {
    private Mapper mapper;
    private DecoderState s;

    public MBlockDecoderIPCM(Mapper mapper,  DecoderState decoderState) {
        this.mapper = mapper;
        this.s = decoderState;
    }

    public void decode(CodedMBlock mBlock, DecodedMBlock mb) {
        int mbX = mapper.getMbX(mBlock.mbIdx);
        collectPredictors(s, mb.mb, mbX);
        saveVectIntra(s, mapper.getMbX(mBlock.mbIdx));
    }
}
