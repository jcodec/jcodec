package org.jcodec.codecs.vpx;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface RateControl {

    int[] getSegmentQps();

    int getSegment();

    void report(int bits);

    void reset();
}
