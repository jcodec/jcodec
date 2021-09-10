package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.enums.SkinDetectionBlockSize;
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
public class SkinDetect {

    static int avg_2x2(ReadOnlyIntArrPointer s, int p) {
        int i, j;
        int sum = 0;
        for (i = 0; i < 2; ++i) {
            for (j = 0; j < 2; ++j) {
                sum += s.getRel(p * i + j);
            }
        }
        return (sum + 2) >> 2;
    }

    static boolean vp8_compute_skin_block(ReadOnlyIntArrPointer y, ReadOnlyIntArrPointer u, ReadOnlyIntArrPointer v,
            int stride, int strideuv, SkinDetectionBlockSize bsize, int consec_zeromv, int curr_motion_magn) {
        // No skin if block has been zero/small motion for long consecutive time.
        if (consec_zeromv > 60 && curr_motion_magn == 0) {
            return false;
        } else {
            int motion = 1;
            if (consec_zeromv > 25 && curr_motion_magn == 0)
                motion = 0;

            final PositionableIntArrPointer yptr = PositionableIntArrPointer.makePositionable(y);
            final PositionableIntArrPointer uptr = PositionableIntArrPointer.makePositionable(u);
            final PositionableIntArrPointer vptr = PositionableIntArrPointer.makePositionable(v);
            if (bsize == SkinDetectionBlockSize.SKIN_16x16) {
                // Take the average of center 2x2 pixels.
                yptr.incBy(7 * stride + 7);
                uptr.incBy(3 * strideuv + 3);
                vptr.incBy(3 * strideuv + 3);
                int ysource = avg_2x2(yptr, stride);
                int usource = avg_2x2(uptr, strideuv);
                int vsource = avg_2x2(vptr, strideuv);
                return vpx_skin_pixel(ysource, usource, vsource, motion);
            } else {
                int num_skin = 0;
                int i, j;
                for (i = 0; i < 2; i++) {
                    for (j = 0; j < 2; j++) {
                        // Take the average of center 2x2 pixels.
                        yptr.savePos();
                        uptr.savePos();
                        vptr.savePos();
                        yptr.incBy(3 * stride + 3);
                        uptr.incBy(strideuv + 1);
                        vptr.incBy(strideuv + 1);
                        int ysource = avg_2x2(yptr, stride);
                        int usource = avg_2x2(uptr, strideuv);
                        int vsource = avg_2x2(vptr, strideuv);
                        num_skin += vpx_skin_pixel(ysource, usource, vsource, motion) ? 1 : 0;
                        if (num_skin >= 2)
                            return true;
                        yptr.incBy(8);
                        uptr.incBy(4);
                        vptr.incBy(4);
                    }
                    yptr.incBy((stride << 3) - 16);
                    uptr.incBy((strideuv << 2) - 8);
                    vptr.incBy((strideuv << 2) - 8);
                }

                return false;
            }
        }
    }

    // Fixed-point skin color model parameters.
    static final int[][] skin_mean = { { 7463, 9614 }, { 6400, 10240 }, { 7040, 10240 }, { 8320, 9280 },
            { 6800, 9614 } };
    static final int[] skin_inv_cov = { 4107, 1663, 1663, 2157 }; // q16
    static final int[] skin_threshold = { 1570636, 1400000, 800000, 800000, 800000, 800000 }; // q18

    // Thresholds on luminance.
    static final int y_low = 40;
    static final int y_high = 220;

    // Evaluates the Mahalanobis distance measure for the input CbCr values.
    static int vpx_evaluate_skin_color_difference(int cb, int cr, int idx) {
        int cb_q6 = cb << 6;
        int cr_q6 = cr << 6;
        int cb_diff_q12 = (cb_q6 - skin_mean[idx][0]) * (cb_q6 - skin_mean[idx][0]);
        int cbcr_diff_q12 = (cb_q6 - skin_mean[idx][0]) * (cr_q6 - skin_mean[idx][1]);
        int cr_diff_q12 = (cr_q6 - skin_mean[idx][1]) * (cr_q6 - skin_mean[idx][1]);
        int cb_diff_q2 = (cb_diff_q12 + (1 << 9)) >> 10;
        int cbcr_diff_q2 = (cbcr_diff_q12 + (1 << 9)) >> 10;
        int cr_diff_q2 = (cr_diff_q12 + (1 << 9)) >> 10;
        int skin_diff = skin_inv_cov[0] * cb_diff_q2 + skin_inv_cov[1] * cbcr_diff_q2 + skin_inv_cov[2] * cbcr_diff_q2
                + skin_inv_cov[3] * cr_diff_q2;
        return skin_diff;
    }

    // Checks if the input yCbCr values corresponds to skin color.
    static boolean vpx_skin_pixel(int y, int cb, int cr, int motion) {
        // Exit on grey.
        // Exit on very strong cb.
        if (y < y_low || y > y_high || (cb == 128 && cr == 128) || (cb > 150 && cr < 110)) {
            return false;
        } else {
            int i = 0;
            for (; i < 5; ++i) {
                int skin_color_diff = vpx_evaluate_skin_color_difference(cb, cr, i);
                if (skin_color_diff < skin_threshold[i + 1]) {
                    if (y < 60 && skin_color_diff > 3 * (skin_threshold[i + 1] >> 2)) {
                        return false;
                    } else if (motion == 0 && skin_color_diff > (skin_threshold[i + 1] >> 1)) {
                        return false;
                    } else {
                        return true;
                    }
                }
                // Exit if difference is much large than the threshold.
                if (skin_color_diff > (skin_threshold[i + 1] << 3)) {
                    return false;
                }
            }
            return false;
        }
    }

}
