package org.jcodec.codecs.h264.decode.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class NearbyPixels {
    public static class Plane {
        private int[] mbLeft;
        private int[] mbTop;
        private int[] mbTopLeft;
        private int[] mbTopRight;

        public Plane(int[] mbLeft, int[] mbTop, int[] mbTopLeft, int[] mbTopRight) {
            this.mbLeft = mbLeft;
            this.mbTop = mbTop;
            this.mbTopLeft = mbTopLeft;
            this.mbTopRight = mbTopRight;
        }

        public int[] getMbLeft() {
            return mbLeft;
        }

        public int[] getMbTop() {
            return mbTop;
        }

        public int[] getMbTopLeft() {
            return mbTopLeft;
        }

        public int[] getMbTopRight() {
            return mbTopRight;
        }
    }

    private Plane luma;
    private Plane cb;
    private Plane cr;

    public NearbyPixels(Plane luma, Plane cb, Plane cr) {
        this.luma = luma;
        this.cb = cb;
        this.cr = cr;
    }

    public Plane getLuma() {
        return luma;
    }

    public Plane getCb() {
        return cb;
    }

    public Plane getCr() {
        return cr;
    }
}