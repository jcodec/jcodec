package org.jcodec.codecs.vpx.vp8.subpixfns;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;

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
public abstract class Filter implements SubpixFN {

    public static final int BLOCK_HEIGHT_WIDTH = 4;
    public static final int VP8_FILTER_WEIGHT = 128;
    public static final int VP8_FILTER_SHIFT = 7;

    void filter_block2d_single_pass(final ReadOnlyIntArrPointer srcp, final FullAccessIntArrPointer output_ptr,
            final int output_pitch, final int src_pixels_per_line, final int pixel_step, final int output_height,
            final int output_width, final int[] vp8_filter) {
        final PositionableIntArrPointer src_ptr = PositionableIntArrPointer.makePositionable(srcp);

        for (int i = 0; i < output_height; ++i) {
            final int outbase = i * output_pitch;
            for (int j = 0; j < output_width; ++j) {
                output_ptr.setRel(outbase + j, applyFilterCore(src_ptr, pixel_step, vp8_filter));
                src_ptr.inc();
            }

            /* Next row... */
            src_ptr.incBy(src_pixels_per_line - output_width);
        }
    }

    protected abstract short applyFilterCore(ReadOnlyIntArrPointer src_ptr, int pixel_step, int[] vp8_filter);

}
