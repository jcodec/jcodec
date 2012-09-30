package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Macroblock with inter prediction
 * 
 * 
 * @author Jay Codec
 * 
 */
public class MBlockInter extends MBlockWithResidual {

    public enum Type {
        MB_16x16, MB_16x8, MB_8x16
    }

    private Type type;

    private InterPrediction prediction;

    public MBlockInter(MBlockWithResidual mb, InterPrediction prediction, Type type) {
        super(mb);
        this.prediction = prediction;
        this.type = type;
    }

    public MBlockInter(int qpDelta, CodedChroma chroma, CoeffToken[] lumaTokens, ResidualBlock[] luma,
            InterPrediction prediction, Type type) {
        super(qpDelta, chroma, lumaTokens, luma);
        this.prediction = prediction;
        this.type = type;
    }

    public InterPrediction getPrediction() {
        return prediction;
    }

    public Type getType() {
        return type;
    }
}
