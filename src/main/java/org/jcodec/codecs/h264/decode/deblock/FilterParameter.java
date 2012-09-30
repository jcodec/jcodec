package org.jcodec.codecs.h264.decode.deblock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class FilterParameter {

    public static class Threshold {
        private int[] alphaH;
        private int[] betaH;
        private int[] alphaV;
        private int[] betaV;

        public Threshold(int[] alphaH, int[] betaH, int[] alphaV, int[] betaV) {
            this.alphaH = alphaH;
            this.betaH = betaH;
            this.alphaV = alphaV;
            this.betaV = betaV;
        }

        public int[] getAlphaH() {
            return alphaH;
        }

        public int[] getBetaH() {
            return betaH;
        }

        public int[] getAlphaV() {
            return alphaV;
        }

        public int[] getBetaV() {
            return betaV;
        }
    }

    private boolean filterLeft;
    private boolean filterTop;
    private boolean enabled;
    private Threshold lumaThresh;
    private Threshold cbThresh;
    private Threshold crThresh;
    private int[] bsH;
    private int[] bsV;

    public FilterParameter(boolean filterLeft, boolean filterTop, boolean enabled, Threshold lumaThresh,
            Threshold cbThresh, Threshold crThresh, int[] bsH, int[] bsV) {
        this.filterLeft = filterLeft;
        this.filterTop = filterTop;
        this.enabled = enabled;

        this.lumaThresh = lumaThresh;
        this.cbThresh = cbThresh;
        this.crThresh = crThresh;

        this.bsH = bsH;
        this.bsV = bsV;
    }

    public boolean isFilterLeft() {
        return filterLeft;
    }

    public boolean isFilterTop() {
        return filterTop;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Threshold getLumaThresh() {
        return lumaThresh;
    }

    public Threshold getCbThresh() {
        return cbThresh;
    }

    public Threshold getCrThresh() {
        return crThresh;
    }

    public int[] getBsH() {
        return bsH;
    }

    public int[] getBsV() {
        return bsV;
    }

};