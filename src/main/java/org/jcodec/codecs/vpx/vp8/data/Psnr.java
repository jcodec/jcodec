package org.jcodec.codecs.vpx.vp8.data;

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
public class Psnr {

    public static final double MAX_PSNR = 100.0;

    public static double vpx_sse_to_psnr(double samples, double peak, double sse) {
        if (sse > 0.0) {
            double psnr = 10.0 * Math.log10(samples * peak * peak / sse);
            return Math.min(psnr, MAX_PSNR);
        } else {
            return MAX_PSNR;
        }
    }

}
