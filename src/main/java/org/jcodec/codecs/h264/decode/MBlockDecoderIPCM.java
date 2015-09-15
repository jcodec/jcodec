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
    private DecoderState s;
    private BitReader reader;

    public MBlockDecoderIPCM(Mapper mapper, BitReader reader, DecoderState decoderState) {
        this.mapper = mapper;
        this.reader = reader;
        this.s = decoderState;
    }

    public void decode(int mbIndex, Picture8Bit mb) {
        int mbX = mapper.getMbX(mbIndex);

        reader.align();

        int[] samplesLuma = new int[256];
        for (int i = 0; i < 256; i++) {
            samplesLuma[i] = reader.readNBit(8);
        }
        int MbWidthC = 16 >> s.chromaFormat.compWidth[1];
        int MbHeightC = 16 >> s.chromaFormat.compHeight[1];

        int[] samplesChroma = new int[2 * MbWidthC * MbHeightC];
        for (int i = 0; i < 2 * MbWidthC * MbHeightC; i++) {
            samplesChroma[i] = reader.readNBit(8);
        }
        collectPredictors(s, mb, mbX);
        saveVectIntra(s, mapper.getMbX(mbIndex));
    }
}
