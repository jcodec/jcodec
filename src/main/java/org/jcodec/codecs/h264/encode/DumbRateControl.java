package org.jcodec.codecs.h264.encode;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Dumb rate control policy, always maintains the same QP for the whole video
 * 
 * @author The JCodec project
 * 
 */
public class DumbRateControl implements RateControl {
    private static final int QP = 20;

    @Override
    public int getInitQp() {
        return QP;
    }

    @Override
    public int getQpDelta() {
        return 0;
    }

    @Override
    public boolean accept(int bits) {
        return true;
    }

    @Override
    public void reset() {
        // Do nothing, remember we are dumb
    }
}
