package org.jcodec.codecs.mjpeg;

import org.jcodec.common.io.VLC;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class CodedImage {
    FrameHeader frame;
    ScanHeader scan;
    private byte[] data;
    private VLC ydc = JpegConst.YDC_DEFAULT;
    private VLC yac = JpegConst.YAC_DEFAULT;
    private VLC cdc = JpegConst.CDC_DEFAULT;
    private VLC cac = JpegConst.CAC_DEFAULT;
    private QuantTable quantLum;
    private QuantTable quantChrom;

    public int getWidth() {
        return frame.x;
    }

    public int getHeight() {
        return frame.y;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public VLC getYdc() {
        return ydc;
    }

    public void setYdc(VLC ydc) {
        this.ydc = ydc;
    }

    public VLC getYac() {
        return yac;
    }

    public void setYac(VLC yac) {
        this.yac = yac;
    }

    public VLC getCdc() {
        return cdc;
    }

    public void setCdc(VLC cdc) {
        this.cdc = cdc;
    }

    public VLC getCac() {
        return cac;
    }

    public void setCac(VLC cac) {
        this.cac = cac;
    }

    public QuantTable getQuantLum() {
        return quantLum;
    }

    public void setQuantLum(QuantTable quant1) {
        this.quantLum = quant1;
    }

    public QuantTable getQuantChrom() {
        return quantChrom;
    }

    public void setQuantChrom(QuantTable quant2) {
        this.quantChrom = quant2;
    }
}