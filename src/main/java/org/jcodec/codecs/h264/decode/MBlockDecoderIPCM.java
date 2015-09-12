package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.collectPredictors;
import static org.jcodec.codecs.h264.decode.MBlockDecoderUtils.saveVectIntra;

import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.Picture8Bit;

/**
 * A decoder for Intra PCM macroblocks
 * 
 * @author The JCodec project
 */
public class MBlockDecoderIPCM {
    private Mapper mapper;
    private DecoderState sharedState;

    public MBlockDecoderIPCM(Mapper mapper, DecoderState sharedState) {
        this.mapper = mapper;
        this.sharedState = sharedState;
    }

    public void decode(BitReader reader, int mbIndex, Picture8Bit mb) {
        int mbX = mapper.getMbX(mbIndex);

        reader.align();

        int[] samplesLuma = new int[256];
        for (int i = 0; i < 256; i++) {
            samplesLuma[i] = reader.readNBit(8);
        }
        int MbWidthC = 16 >> sharedState.chromaFormat.compWidth[1];
        int MbHeightC = 16 >> sharedState.chromaFormat.compHeight[1];

        int[] samplesChroma = new int[2 * MbWidthC * MbHeightC];
        for (int i = 0; i < 2 * MbWidthC * MbHeightC; i++) {
            samplesChroma[i] = reader.readNBit(8);
        }
        collectPredictors(sharedState, mb, mbX);
        saveVectIntra(sharedState, mapper.getMbX(mbIndex));
    }
}
