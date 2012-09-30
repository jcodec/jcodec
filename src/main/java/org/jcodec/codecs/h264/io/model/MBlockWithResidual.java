package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Macroblock with residual data (all macroblock types except I_16x16
 * variations)
 * 
 * @author Jay Codec
 * 
 */
public abstract class MBlockWithResidual extends CodedMacroblock {
    private ResidualBlock[] luma;

    public MBlockWithResidual(int qpDelta, CodedChroma chroma, CoeffToken[] lumaTokens, ResidualBlock[] luma) {
        super(qpDelta, chroma, lumaTokens);
        this.luma = luma;
    }

    public MBlockWithResidual(MBlockWithResidual other) {
        super(other);

        this.luma = other.luma;
    }

    public ResidualBlock[] getLuma() {
        return luma;
    }
}