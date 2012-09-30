package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A macroblock syntax element of H264 bitstream
 * 
 * 
 * @author Jay Codec
 * 
 */
public abstract class CodedMacroblock extends Macroblock {
    private int qpDelta;
    private CodedChroma chroma;
    private int cbpChroma;
    private CoeffToken[] lumaTokens;

    public CodedMacroblock(int qpDelta, CodedChroma chroma, CoeffToken[] lumaTokens) {
        this.qpDelta = qpDelta;
        this.chroma = chroma;
        this.lumaTokens = lumaTokens;
    }

    public CodedMacroblock(CodedMacroblock other) {
        this.qpDelta = other.qpDelta;
        this.chroma = other.chroma;
        this.lumaTokens = other.lumaTokens;
    }

    public int getQpDelta() {
        return qpDelta;
    }

    public CodedChroma getChroma() {
        return chroma;
    }

    public CoeffToken[] getLumaTokens() {
        return lumaTokens;
    }

    public int getCbpChroma() {
        return cbpChroma;
    }
}