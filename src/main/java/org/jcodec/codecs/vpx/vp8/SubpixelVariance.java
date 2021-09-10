package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.VarianceFNs;
import org.jcodec.codecs.vpx.vp8.data.VarianceResults;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
import org.jcodec.codecs.vpx.vp8.subpixfns.BilinearPredict;

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
public class SubpixelVariance implements VarianceFNs.SVF {
    static final int FILTER_BITS = 7;

    // Applies a 1-D 2-tap bilinear filter to the source block in either horizontal
    // or vertical direction to produce the filtered output block. Used to implement
    // the first-pass of 2-D separable filter.
    //
    // Produces int16_t output to retain precision for the next pass. Two filter
    // taps should sum to FILTER_WEIGHT. pixel_step defines whether the filter is
    // applied horizontally (pixel_step = 1) or vertically (pixel_step = stride).
    // It defines the offset required to move from one input to the next.
    static void var_filter_block2d_bil(final PositionableIntArrPointer src_ptr, final FullAccessIntArrPointer ref_ptr,
            final int src_pixels_per_line, final int pixel_step, final int output_height, final int output_width,
            final int[] filter) {
        int baseSrc = 0, baseRef = 0;

        for (int i = 0; i < output_height; ++i) {
            final int basePS = baseSrc + pixel_step;
            for (int j = 0; j < output_width; ++j) {
                ref_ptr.setRel(baseRef + j, (short) (CommonUtils.roundPowerOfTwo(
                        (int) src_ptr.getRel(baseSrc + j) * filter[0] + (int) src_ptr.getRel(basePS + j) * filter[1],
                        FILTER_BITS)));
            }
            baseSrc += src_pixels_per_line;
            baseRef += output_width;
        }
    }

    private final int w, h;

    private final FullAccessIntArrPointer biliX;
    private final FullAccessIntArrPointer biliY;

    public SubpixelVariance(int w, int h) {
        this.w = w;
        this.h = h;
        biliX = new FullAccessIntArrPointer((h + 1) * w);
        biliY = new FullAccessIntArrPointer(h * w);
    }

    @Override
    public void call(PositionableIntArrPointer src_ptr, int src_stride, int xoff, int yoff,
            PositionableIntArrPointer ref_ptr, int ref_stride, VarianceResults sse) {
        var_filter_block2d_bil(src_ptr, biliX, src_stride, 1, h + 1, w, BilinearPredict.vp8_bilinear_filters[xoff]);
        var_filter_block2d_bil(biliX, biliY, w, w, h, w, BilinearPredict.vp8_bilinear_filters[yoff]);

        Variance.variance(biliY, w, ref_ptr, ref_stride, sse, w, h);
    }

}
