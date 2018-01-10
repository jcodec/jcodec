package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVectIntra;

import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.common.model.Picture;

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

    public void decode(MBlock mBlock, Picture mb) {
        byte[] luma = mb.getPlaneData(0);
        for (int i = 0; i < 256; i++) {
            luma[i] = (byte)(mBlock.ipcm.samplesLuma[i] - 128);
        }
        for (int pl = 0, off = 0; pl < 2; pl++, off += 64) {
            byte[] chroma = mb.getPlaneData(pl + 1);
            for (int i = 0; i < 64; i++) {
                chroma[i] = (byte)(mBlock.ipcm.samplesChroma[i + off] - 128);
            }
        }
        int mbX = mapper.getMbX(mBlock.mbIdx);
        collectPredictors(s, mb, mbX);
        saveVectIntra(s, mapper.getMbX(mBlock.mbIdx));
    }
}
