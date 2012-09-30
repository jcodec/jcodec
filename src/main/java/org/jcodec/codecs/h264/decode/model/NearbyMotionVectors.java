package org.jcodec.codecs.h264.decode.model;

import org.jcodec.codecs.h264.io.model.Vector;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Contains motion vectors of nearby macroblocks
 * 
 * Arrays 'left and 'top' contain exactly 4 (four) vector, one for 4x4 block
 * 
 * @author Jay Codec
 * 
 */
public class NearbyMotionVectors {
    private Vector a[];
    private Vector b[];
    private Vector c;
    private Vector d;
    private boolean hasA;
    private boolean hasB;
    private boolean hasC;
    private boolean hasD;

    public NearbyMotionVectors(Vector[] a, Vector[] b, Vector c, Vector d, boolean hasA, boolean hasB, boolean hasC,
            boolean hasD) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.hasA = hasA;
        this.hasB = hasB;
        this.hasC = hasC;
        this.hasD = hasD;
    }

    public Vector[] getA() {
        return a;
    }

    public Vector[] getB() {
        return b;
    }

    public Vector getC() {
        return c;
    }

    public Vector getD() {
        return d;
    }

    public boolean hasA() {
        return hasA;
    }

    public boolean hasB() {
        return hasB;
    }

    public boolean hasC() {
        return hasC;
    }

    public boolean hasD() {
        return hasD;
    }
}
