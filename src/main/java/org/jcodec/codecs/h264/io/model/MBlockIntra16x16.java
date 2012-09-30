package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Intra16x16 macroblock base class
 * 
 * @author Jay Codec
 * 
 */
public class MBlockIntra16x16 extends CodedMacroblock {
    private ResidualBlock lumaDC;
    private ResidualBlock[] lumaAC;
    private int lumaMode;
    private int chromaMode;

    public MBlockIntra16x16(CodedChroma chroma, int qpDelta, ResidualBlock lumaDC, ResidualBlock[] lumaAC,
            CoeffToken[] lumaTokens, int lumaMode, int chromaMode) {
        super(qpDelta, chroma, lumaTokens);
        this.lumaDC = lumaDC;
        this.lumaAC = lumaAC;
        this.lumaMode = lumaMode;
        this.chromaMode = chromaMode;
    }

    public ResidualBlock getLumaDC() {
        return lumaDC;
    }

    public ResidualBlock[] getLumaAC() {
        return lumaAC;
    }

    public int getLumaMode() {
        return lumaMode;
    }

    public int getChromaMode() {
        return chromaMode;
    }
}