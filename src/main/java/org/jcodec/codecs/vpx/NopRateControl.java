package org.jcodec.codecs.vpx;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Rate control that does nothing
 * 
 * @author The JCodec project
 * 
 */
public class NopRateControl implements RateControl {
    
    private int qp;

    public NopRateControl(int qp) {
        this.qp = qp;
    }

    @Override
    public int[] getSegmentQps() {
        return new int[] { qp };
    }

    @Override
    public int getSegment() {
        return 0;
    }

    @Override
    public void report(int bits) {
    }

    @Override
    public void reset() {

    }
}
