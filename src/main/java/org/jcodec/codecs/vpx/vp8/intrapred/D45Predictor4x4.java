package org.jcodec.codecs.vpx.vp8.intrapred;

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
public class D45Predictor4x4 extends D45Predictor4x4Base {
    public D45Predictor4x4() {
        super(false);
    }
}
