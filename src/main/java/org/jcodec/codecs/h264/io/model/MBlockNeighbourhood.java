package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Neighbourhood of the current macroblock needed for predictive decoding
 * 
 * @author Jay Codec
 * 
 */
public class MBlockNeighbourhood {
    private boolean topAvailable;
    private boolean leftAvailable;

    private CoeffToken[] lumaLeft;
    private CoeffToken[] lumaTop;
    private CoeffToken[] cbLeft;
    private CoeffToken[] cbTop;
    private CoeffToken[] crLeft;
    private CoeffToken[] crTop;
    private IntraNxNPrediction predLeft;
    private IntraNxNPrediction predTop;

    public MBlockNeighbourhood(CoeffToken[] lumaLeft, CoeffToken[] lumaTop, CoeffToken[] cbLeft, CoeffToken[] cbTop,
            CoeffToken[] crLeft, CoeffToken[] crTop, IntraNxNPrediction predLeft, IntraNxNPrediction predTop,
            boolean leftAvailable, boolean topAvailable) {
        this.lumaLeft = lumaLeft;
        this.lumaTop = lumaTop;

        this.cbLeft = cbLeft;
        this.cbTop = cbTop;

        this.crLeft = crLeft;
        this.crTop = crTop;

        this.predLeft = predLeft;
        this.predTop = predTop;

        this.topAvailable = topAvailable;
        this.leftAvailable = leftAvailable;
    }

    public CoeffToken[] getLumaLeft() {
        return lumaLeft;
    }

    public CoeffToken[] getLumaTop() {
        return lumaTop;
    }

    public CoeffToken[] getCbLeft() {
        return cbLeft;
    }

    public CoeffToken[] getCbTop() {
        return cbTop;
    }

    public CoeffToken[] getCrLeft() {
        return crLeft;
    }

    public CoeffToken[] getCrTop() {
        return crTop;
    }

    public IntraNxNPrediction getPredLeft() {
        return predLeft;
    }

    public IntraNxNPrediction getPredTop() {
        return predTop;
    }

    public boolean isTopAvailable() {
        return topAvailable;
    }

    public boolean isLeftAvailable() {
        return leftAvailable;
    }
}
