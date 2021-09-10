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
public enum Scaling {
    NORMAL(1, 1), FOURFIVE(4, 5), THREEFIVE(3, 5), ONETWO(1, 2);

    public final int hr, hs;

    private Scaling(int r, int s) {
        hr = r;
        hs = s;
    }
}