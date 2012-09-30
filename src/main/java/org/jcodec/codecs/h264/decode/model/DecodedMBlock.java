package org.jcodec.codecs.h264.decode.model;

import org.jcodec.codecs.h264.io.model.CodedMacroblock;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Contains pixels of decoded macroblock and intermediate information needed for
 * correct work of deblocking filter. This information is block qp and decoded
 * motion vectors.
 * 
 * @author Jay Codec
 * 
 */
public class DecodedMBlock {
    private int[] luma;
    private DecodedChroma chroma;
    private int qp;
    private MVMatrix decodedMVs;
    private Picture[] refList;
    private CodedMacroblock coded;

    public DecodedMBlock(int[] luma, DecodedChroma chroma, int qp, MVMatrix decodedMVs, Picture[] refList,
            CodedMacroblock coded) {
        this.luma = luma;
        this.chroma = chroma;
        this.qp = qp;
        this.decodedMVs = decodedMVs;
        this.refList = refList;
        this.coded = coded;
    }

    public int[] getLuma() {
        return luma;
    }

    public DecodedChroma getChroma() {
        return chroma;
    }

    public int getQp() {
        return qp;
    }

    public MVMatrix getDecodedMVs() {
        return decodedMVs;
    }

    public Picture[] getRefList() {
        return refList;
    }

    public CodedMacroblock getCoded() {
        return coded;
    }
}
