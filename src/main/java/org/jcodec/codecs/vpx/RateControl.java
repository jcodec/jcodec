package org.jcodec.codecs.vpx;

public interface RateControl {

    int[] getSegmentQps();

    int getSegment();

    void report(int bits);

    void reset();
}
