package org.jcodec.codecs.mpeg12;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 1/2 decoder interframe motion compensation routines.
 * 
 * Quad subpixel interpolator which is just a sub-case of octal subpixel
 * interpolator.
 * 
 * @author The JCodec project
 * 
 */
public class MPEGPredQuad extends MPEGPredOct {
    public MPEGPredQuad(MPEGPred other) {
        super(other);
    }

    // TODO: This interpolation uses sinc at the very lowest (half-pel -- 1/8) level as opposed to linear as specified by the standard.
    // this may be a result of color greening out in long GOPs.
    @Override
    public void predictPlane(byte[] ref, int refX, int refY, int refW, int refH, int refVertStep, int refVertOff,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        super.predictPlane(ref, refX, refY, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW << 1, tgtH << 1,
                tgtVertStep);
    }
}