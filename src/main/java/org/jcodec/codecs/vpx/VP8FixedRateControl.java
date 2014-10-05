package org.jcodec.codecs.vpx;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Makes all frames of a fixed size
 * 
 * @author The JCodec project
 * 
 */
public class VP8FixedRateControl implements RateControl {

    private int rate;

    public VP8FixedRateControl(int rate) {
        this.rate = rate;
    }

    @Override
    public int[] getSegmentQps() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getSegment() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void report(int bits) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }
}
