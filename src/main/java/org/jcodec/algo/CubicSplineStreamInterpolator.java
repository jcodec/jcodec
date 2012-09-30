package org.jcodec.algo;

import org.jcodec.common.model.Rational;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class CubicSplineStreamInterpolator extends StreamInterpolator {

    public CubicSplineStreamInterpolator(Rational ratio) {
        super(ratio);
    }

    public int[] interpolate(int[] in) {
        throw new UnsupportedOperationException();
    }
}
