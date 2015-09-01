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

    @Override
    public void predictPlane(int[] ref, int refX, int refY, int refW, int refH, int refVertStep, int refVertOff,
            int[] tgt, int tgtY, int tgtW, int tgtH, int tgtVertStep) {
        super.predictPlane(ref, refX << 1, refY << 1, refW, refH, refVertStep, refVertOff, tgt, tgtY, tgtW, tgtH,
                tgtVertStep);
    }
}