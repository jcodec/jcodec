package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class IntraNxNPrediction {

    private int chromaMode;
    private int[] lumaModes;

    public IntraNxNPrediction(int[] intra4x4PredModes, int intraChromaPredMode) {
        chromaMode = intraChromaPredMode;
        lumaModes = intra4x4PredModes;
    }

    public int getChromaMode() {
        return chromaMode;
    }

    public int[] getLumaModes() {
        return lumaModes;
    }
}
