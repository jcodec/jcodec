package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Prediction for inter 8x8 macroblock
 * 
 * Contains a submacroblock prediction for each part as well as prediction modes
 * 
 * @author Jay Codec
 * 
 */
public class Inter8x8Prediction {
    private SubMBType[] subMbTypes;
    private int[] refIdxL0;
    private int[] refIdxL1;
    private Vector[][] decodedMVsL0;
    private Vector[][] decodedMVsL1;

    public Inter8x8Prediction(SubMBType[] subMbTypes, int[] refIdxL0, int[] refIdxL1, Vector[][] decodedMVsL0,
            Vector[][] decodedMVsL1) {
        this.subMbTypes = subMbTypes;
        this.refIdxL0 = refIdxL0;
        this.refIdxL1 = refIdxL1;
        this.decodedMVsL0 = decodedMVsL0;
        this.decodedMVsL1 = decodedMVsL1;
    }

    public SubMBType[] getSubMbTypes() {
        return subMbTypes;
    }

    public int[] getRefIdxL0() {
        return refIdxL0;
    }

    public int[] getRefIdxL1() {
        return refIdxL1;
    }

    public Vector[][] getDecodedMVsL0() {
        return decodedMVsL0;
    }

    public Vector[][] getDecodedMVsL1() {
        return decodedMVsL1;
    }
}
