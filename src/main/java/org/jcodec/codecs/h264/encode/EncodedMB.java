package org.jcodec.codecs.h264.encode;

import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

public class EncodedMB {
    private Picture pixels;
    private MBType type;
    private int qp;
    private int[] nc;
    private int[] mx;
    private int[] my;

    public EncodedMB() {
        pixels = Picture.create(16, 16, ColorSpace.YUV420J);
        nc = new int[16];
        mx = new int[16];
        my = new int[16];
    }

    public Picture getPixels() {
        return pixels;
    }

    public MBType getType() {
        return type;
    }

    public void setType(MBType type) {
        this.type = type;
    }

    public int getQp() {
        return qp;
    }

    public void setQp(int qp) {
        this.qp = qp;
    }

    public int[] getNc() {
        return nc;
    }

    public int[] getMx() {
        return mx;
    }

    public int[] getMy() {
        return my;
    }
}
