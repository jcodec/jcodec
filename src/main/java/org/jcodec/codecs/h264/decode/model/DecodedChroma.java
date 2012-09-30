package org.jcodec.codecs.h264.decode.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class DecodedChroma {
    private int[] cb;
    private int[] cr;

    private int qpCb;
    private int qpCr;

    public DecodedChroma(int[] cb, int[] cr, int qpCb, int qpCr) {
        this.cb = cb;
        this.cr = cr;
        this.qpCb = qpCb;
        this.qpCr = qpCr;
    }

    public int[] getCb() {
        return cb;
    }

    public int[] getCr() {
        return cr;
    }

    public int getQpCb() {
        return qpCb;
    }

    public int getQpCr() {
        return qpCr;
    }
}
