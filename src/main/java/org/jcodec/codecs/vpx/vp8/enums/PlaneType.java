package org.jcodec.codecs.vpx.vp8.enums;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public enum PlaneType {
    Y_NO_DC(4, 1), Y2(16, 0), UV(2, 0), Y_WITH_DC(4, 0);

    public final int rd_mult;
    public final int start_coeff;

    private PlaneType(int rdm, int sc) {
        rd_mult = rdm;
        start_coeff = sc;
    }
}
