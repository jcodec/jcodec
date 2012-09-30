package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * P_8x8, P_8x8ref0, B_8x8 Macroblocks of H264
 * 
 * Inter coded macroblock with prediction that is further subdivided into
 * subpredictions.
 * 
 * @author Jay Codec
 * 
 */
public class MBlockInter8x8 extends MBlockWithResidual {
    private Inter8x8Prediction prediction;

    public MBlockInter8x8(MBlockWithResidual other, Inter8x8Prediction prediction) {
        super(other);

        this.prediction = prediction;
    }

    public MBlockInter8x8(int qpDelta, CodedChroma chroma, CoeffToken[] lumaTokens, ResidualBlock[] luma,
            Inter8x8Prediction prediction) {
        super(qpDelta, chroma, lumaTokens, luma);
        this.prediction = prediction;
    }

    public Inter8x8Prediction getPrediction() {
        return prediction;
    }
}