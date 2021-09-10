package org.jcodec.codecs.vpx.vp8.subpixfns;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
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
public class BilinearPredict extends Filter {
    public static final int[][] vp8_bilinear_filters = { { 128, 0 }, { 112, 16 }, { 96, 32 }, { 80, 48 }, { 64, 64 },
            { 48, 80 }, { 32, 96 }, { 16, 112 } };

    public static final SubPixFnCollector bilinear = new SubPixFnCollector() {

        @Override
        public SubpixFN get8x8() {
            return new BilinearPredict(8, 8);
        }

        @Override
        public SubpixFN get8x4() {
            return new BilinearPredict(8, 4);
        }

        @Override
        public SubpixFN get4x4() {
            return new BilinearPredict(4, 4);
        }

        @Override
        public SubpixFN get16x16() {
            return new BilinearPredict(16, 16);
        }
    };

    private static final int bilinearWeight = Filter.VP8_FILTER_WEIGHT >> 1;

    private int[] VFilter, HFilter;

    private final int width, height;
    private FullAccessIntArrPointer FData = new FullAccessIntArrPointer(17 * 16);

    private BilinearPredict(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    protected short applyFilterCore(ReadOnlyIntArrPointer src_ptr, int pixel_step, int[] vp8_filter) {
        return (short) (((src_ptr.get() * vp8_filter[0]) + (src_ptr.getRel(1) * vp8_filter[1])
                + (bilinearWeight)) >> Filter.VP8_FILTER_SHIFT);
    }

    @Override
    public void call(ReadOnlyIntArrPointer src, int src_pixels_per_line, int xoffset, int yoffset,
            FullAccessIntArrPointer dst_ptr, int dst_pitch) {
        getFilters(xoffset, yoffset);
        filter_block2d_bil(src, dst_ptr, src_pixels_per_line, dst_pitch);
    }

    private void getFilters(int xoff, int yoff) {
        // This represents a copy and is not required to be handled by optimizations.
        assert ((xoff | yoff) != 0);
        HFilter = vp8_bilinear_filters[xoff];
        VFilter = vp8_bilinear_filters[yoff];
    }

    private void filter_block2d_bil(ReadOnlyIntArrPointer src_ptr, FullAccessIntArrPointer dst_ptr, int src_pitch,
            int dst_pitch) {
        FData.rewind();
        /* First filter 1-D horizontally... */
        filter_block2d_single_pass(src_ptr, FData, width, width, src_pitch, height + 1, width, HFilter);

        /* then 1-D vertically... */
        filter_block2d_single_pass(FData, dst_ptr, width, width, dst_pitch, height, width, VFilter);
    }

}
