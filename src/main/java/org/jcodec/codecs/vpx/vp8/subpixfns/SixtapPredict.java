package org.jcodec.codecs.vpx.vp8.subpixfns;

import org.jcodec.codecs.vpx.vp8.CommonUtils;
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
public class SixtapPredict extends Filter {

    public static SubPixFnCollector sixtap = new SubPixFnCollector() {

        @Override
        public SubpixFN get8x8() {
            return new SixtapPredict(13 * 16, 13, 8, 8);
        }

        @Override
        public SubpixFN get8x4() {
            return new SixtapPredict(13 * 16, 9, 8, 4);
        }

        @Override
        public SubpixFN get4x4() {
            return new SixtapPredict(9 * 4, 4, 4, 4);
        }

        @Override
        public SubpixFN get16x16() {
            return new SixtapPredict(21 * 24, 21, 16, 16);
        }
    };

    private static final int[][] vp8_sub_pel_filters = { { 0, 0, 128, 0, 0, 0 }, /*
                                                                                  * note that 1/8 pel positions are just
                                                                                  * as per alpha -0.5 bicubic
                                                                                  */
            { 0, -6, 123, 12, -1, 0 }, { 2, -11, 108, 36, -8, 1 }, /* New 1/4 pel 6 tap filter */
            { 0, -9, 93, 50, -6, 0 }, { 3, -16, 77, 77, -16, 3 }, /* New 1/2 pel 6 tap filter */
            { 0, -6, 50, 93, -9, 0 }, { 1, -8, 36, 108, -11, 2 }, /* New 1/4 pel 6 tap filter */
            { 0, -1, 12, 123, -6, 0 }, };

    private static final int sixtapWeight = Filter.VP8_FILTER_WEIGHT >> 1;

    private final int width, height, vfOnlyHeight, vfFdataShift;
    private int[] HFilter, VFilter;
    private final FullAccessIntArrPointer FData; /* Temp data buffer used in filtering */

    private SixtapPredict(int fdatasize, int height, int width, int vfH) {
        this.width = width;
        this.height = height;
        this.vfOnlyHeight = vfH;
        this.vfFdataShift = width << 1;
        FData = new FullAccessIntArrPointer(fdatasize);

    }

    @Override
    protected short applyFilterCore(ReadOnlyIntArrPointer src_ptr, int pixel_step, int[] vp8_filter) {
        int Temp = 0;
        for (int k = -2, fi = 0; k < 4; k++, fi++) {
            Temp += src_ptr.getRel(k * pixel_step) * vp8_filter[fi];
        }
        Temp += sixtapWeight; // Rounding
        /* Normalize back to 0-255 */
        return CommonUtils.clipPixel((short) (Temp >> Filter.VP8_FILTER_SHIFT));
    }

    private void getFilters(int xoff, int yoff) {
        HFilter = vp8_sub_pel_filters[xoff]; /* 6 tap */
        VFilter = vp8_sub_pel_filters[yoff]; /* 6 tap */
    }

    void filter_block2d(ReadOnlyIntArrPointer src_ptr, FullAccessIntArrPointer output_ptr, int src_pixels_per_line,
            int output_pitch) {
        FData.rewind();

        /* First filter 1-D horizontally... */
        filter_block2d_single_pass(
                PositionableIntArrPointer.makePositionableAndInc(src_ptr, -(2 * src_pixels_per_line)), FData, width,
                src_pixels_per_line, 1, height, width, HFilter);
        FData.incBy(vfFdataShift);

        /* then filter verticaly... */
        filter_block2d_single_pass(FData, output_ptr, output_pitch, width, width, vfOnlyHeight, width, VFilter);
    }

    @Override
    public void call(ReadOnlyIntArrPointer src, int src_pixels_per_line, int xoffset, int yoffset,
            FullAccessIntArrPointer dst_ptr, int dst_pitch) {
        getFilters(xoffset, yoffset);
        filter_block2d(src, dst_ptr, src_pixels_per_line, dst_pitch);
    }

}
