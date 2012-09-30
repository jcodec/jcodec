package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * I_4x4, I_8x8 macroblocks
 * 
 * 
 * @author Jay Codec
 * 
 */
public class MBlockIntraNxN extends MBlockWithResidual {

    private IntraNxNPrediction prediction;

    public MBlockIntraNxN(MBlockWithResidual other, IntraNxNPrediction prediction) {
        super(other);
        this.prediction = prediction;
    }

    public MBlockIntraNxN(int qpDelta, CodedChroma chroma, CoeffToken[] lumaTokens, ResidualBlock[] luma,
            IntraNxNPrediction prediction) {
        super(qpDelta, chroma, lumaTokens, luma);
        this.prediction = prediction;
    }

    public IntraNxNPrediction getPrediction() {
        return prediction;
    }
}
