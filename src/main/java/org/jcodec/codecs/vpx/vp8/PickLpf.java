package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.YV12buffer;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.MBLvlFeatures;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;

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
public class PickLpf {
    static void yv12_copy_partial_frame(YV12buffer src_ybc, YV12buffer dst_ybc) {
        int yheight;
        int ystride;
        int yoffset;
        int linestocopy;

        yheight = src_ybc.y_height;
        ystride = src_ybc.y_stride;

        /* number of MB rows to use in partial filtering */
        linestocopy = (yheight >> 4) / LoopFilter.PARTIAL_FRAME_FRACTION;
        linestocopy = linestocopy != 0 ? linestocopy << 4 : 16; /* 16 lines per MB */

        /*
         * Copy extra 4 so that full filter context is available if filtering done on
         * the copied partial frame and not original. Partial filter does mb filtering
         * for top row also, which can modify3 pixels above.
         */
        linestocopy += 4;
        /* partial image starts at ~middle of frame (macroblock border) */
        yoffset = ystride * (((yheight >> 5) * 16) - 4);
        dst_ybc.y_buffer.memcopyin(yoffset, src_ybc.y_buffer, yoffset, ystride * linestocopy);
    }

    /* Enforce a minimum filter level based upon baseline Q */
    static short get_min_filter_level(Compressor cpi, short base_qindex) {
        short min_filter_level;

        if (cpi.source_alt_ref_active && cpi.common.refresh_golden_frame && !cpi.common.refresh_alt_ref_frame) {
            min_filter_level = 0;
        } else {
            if (base_qindex <= 6) {
                min_filter_level = 0;
            } else if (base_qindex <= 16) {
                min_filter_level = 1;
            } else {
                min_filter_level = (short) (base_qindex / 8);
            }
        }

        return min_filter_level;
    }

    static long calc_partial_ssl_err(YV12buffer source, YV12buffer dest) {
        int srcoffset, dstoffset;

        int linestocopy;

        /* number of MB rows to use in partial filtering */
        linestocopy = (source.y_height >> 4) / LoopFilter.PARTIAL_FRAME_FRACTION;
        linestocopy = linestocopy != 0 ? linestocopy << 4 : 16; /* 16 lines per MB */

        /* partial image starts at ~middle of frame (macroblock border) */
        srcoffset = source.y_stride * ((dest.y_height >> 5) * 16);
        dstoffset = dest.y_stride * ((dest.y_height >> 5) * 16);
        FullAccessIntArrPointer src = source.y_buffer.shallowCopyWithPosInc(srcoffset);
        FullAccessIntArrPointer dst = dest.y_buffer.shallowCopyWithPosInc(dstoffset);
        return OnyxIf.vp8_calc_ss_err(src, source.y_stride, dst, dest.y_stride, linestocopy, source.y_width);
    }

    static void vp8cx_pick_filter_level_fast(YV12buffer sd, Compressor cpi) {
        CommonData cm = cpi.common;

        long best_err = 0;
        long filt_err = 0;
        short min_filter_level = get_min_filter_level(cpi, cm.base_qindex);
        short max_filter_level = LoopFilter.MAX_LOOP_FILTER;
        short filt_val;
        short best_filt_val;
        YV12buffer saved_frame = cm.frame_to_show;

        /* Replace unfiltered frame buffer with a new one */
        cm.frame_to_show = cpi.pick_lf_lvl_frame;

        cm.sharpness_level = cm.frame_type == FrameType.KEY_FRAME ? 0 : cpi.oxcf.Sharpness;

        if (cm.sharpness_level != cm.last_sharpness_level) {
            cm.lf_info.vp8_loop_filter_update_sharpness(cm.sharpness_level);
            cm.last_sharpness_level = cm.sharpness_level;
        }

        /*
         * Start the search at the previous frame filter level unless it is now out of
         * range.
         */
        if (cm.filter_level < min_filter_level) {
            cm.filter_level = min_filter_level;
        } else if (cm.filter_level > max_filter_level) {
            cm.filter_level = max_filter_level;
        }

        filt_val = cm.filter_level;
        best_filt_val = filt_val;

        /* Get the err using the previous frame's filter value. */

        /* Copy the unfiltered / processed recon buffer to the new buffer */
        yv12_copy_partial_frame(saved_frame, cm.frame_to_show);
        LoopFilter.vp8_loop_filter_partial_frame(cm, cpi.mb.e_mbd, filt_val);

        best_err = calc_partial_ssl_err(sd, cm.frame_to_show);

        filt_val -= 1 + (filt_val > 10 ? 1 : 0);

        /* Search lower filter levels */
        while (filt_val >= min_filter_level) {
            /* Apply the loop filter */
            yv12_copy_partial_frame(saved_frame, cm.frame_to_show);
            LoopFilter.vp8_loop_filter_partial_frame(cm, cpi.mb.e_mbd, filt_val);

            /* Get the err for filtered frame */
            filt_err = calc_partial_ssl_err(sd, cm.frame_to_show);

            /* Update the best case record or exit loop. */
            if (filt_err < best_err) {
                best_err = filt_err;
                best_filt_val = filt_val;
            } else {
                break;
            }

            /* Adjust filter level */
            filt_val -= 1 + (filt_val > 10 ? 1 : 0);
        }

        /* Search up (note that we have already done filt_val = cm.filter_level) */
        filt_val = (short) (cm.filter_level + 1 + (filt_val > 10 ? 1 : 0));

        if (best_filt_val == cm.filter_level) {
            /* Resist raising filter level for very small gains */
            best_err -= (best_err >> 10);

            while (filt_val < max_filter_level) {
                /* Apply the loop filter */
                yv12_copy_partial_frame(saved_frame, cm.frame_to_show);

                LoopFilter.vp8_loop_filter_partial_frame(cm, cpi.mb.e_mbd, filt_val);

                /* Get the err for filtered frame */
                filt_err = calc_partial_ssl_err(sd, cm.frame_to_show);

                /* Update the best case record or exit loop. */
                if (filt_err < best_err) {
                    /*
                     * Do not raise filter level if improvement is < 1 part in 4096
                     */
                    best_err = filt_err - (filt_err >> 10);

                    best_filt_val = filt_val;
                } else {
                    break;
                }

                /* Adjust filter level */
                filt_val += 1 + (filt_val > 10 ? 1 : 0);
            }
        }

        cm.filter_level = best_filt_val;

        if (cm.filter_level < min_filter_level)
            cm.filter_level = min_filter_level;

        if (cm.filter_level > max_filter_level)
            cm.filter_level = max_filter_level;

        /* restore unfiltered frame pointer */
        cm.frame_to_show = saved_frame;
    }

    /* Stub function for now Alt LF not used */
    static void vp8cx_set_alt_lf_level(Compressor cpi, int filt_val) {
        CommonUtils.vp8_copy(cpi.segment_feature_data[MBLvlFeatures.ALT_LF.ordinal()],
                cpi.mb.e_mbd.segment_feature_data[MBLvlFeatures.ALT_LF.ordinal()]);
    }

    static void vp8cx_pick_filter_level(YV12buffer sd, Compressor cpi) {
        CommonData cm = cpi.common;

        long best_err = 0;
        long filt_err = 0;
        short min_filter_level = get_min_filter_level(cpi, cm.base_qindex);
        short max_filter_level = LoopFilter.MAX_LOOP_FILTER;

        short filter_step;
        short filt_high = 0;
        short filt_mid;
        short filt_low = 0;
        short filt_best;
        int filt_direction = 0;

        /* Bias against raising loop filter and in favor of lowering it */
        long Bias = 0;

        long[] ss_err = new long[LoopFilter.MAX_LOOP_FILTER + 1];

        YV12buffer saved_frame = cm.frame_to_show;

        /* Replace unfiltered frame buffer with a new one */
        cm.frame_to_show = cpi.pick_lf_lvl_frame;

        if (cm.frame_type == FrameType.KEY_FRAME) {
            cm.sharpness_level = 0;
        } else {
            cm.sharpness_level = cpi.oxcf.Sharpness;
        }

        /*
         * Start the search at the previous frame filter level unless it is now out of
         * range.
         */
        filt_mid = cm.filter_level;

        if (filt_mid < min_filter_level) {
            filt_mid = min_filter_level;
        } else if (filt_mid > max_filter_level) {
            filt_mid = max_filter_level;
        }

        /* Define the initial step size */
        filter_step = (short) ((filt_mid < 16) ? 4 : filt_mid / 4);

        /* Get baseline error score */

        /* Copy the unfiltered / processed recon buffer to the new buffer */
        YV12buffer.copyY(saved_frame, cm.frame_to_show);

        vp8cx_set_alt_lf_level(cpi, filt_mid);
        LoopFilter.vp8_loop_filter_frame_yonly(cm, cpi.mb.e_mbd, filt_mid);

        best_err = OnyxIf.vp8_calc_ss_err(sd, cm.frame_to_show);

        ss_err[filt_mid] = best_err;

        filt_best = filt_mid;

        while (filter_step > 0) {
            Bias = (best_err >> (15 - (filt_mid / 8))) * filter_step;

            filt_high = (short) (((filt_mid + filter_step) > max_filter_level) ? max_filter_level
                    : (filt_mid + filter_step));
            filt_low = (short) (((filt_mid - filter_step) < min_filter_level) ? min_filter_level
                    : (filt_mid - filter_step));

            if ((filt_direction <= 0) && (filt_low != filt_mid)) {
                if (ss_err[filt_low] == 0) {
                    /* Get Low filter error score */
                    YV12buffer.copyY(saved_frame, cm.frame_to_show);
                    vp8cx_set_alt_lf_level(cpi, filt_low);
                    LoopFilter.vp8_loop_filter_frame_yonly(cm, cpi.mb.e_mbd, filt_low);

                    filt_err = OnyxIf.vp8_calc_ss_err(sd, cm.frame_to_show);
                    ss_err[filt_low] = filt_err;
                } else {
                    filt_err = ss_err[filt_low];
                }

                /*
                 * If value is close to the best so far then bias towards a lower loop filter
                 * value.
                 */
                if ((filt_err - Bias) < best_err) {
                    /* Was it actually better than the previous best? */
                    if (filt_err < best_err)
                        best_err = filt_err;

                    filt_best = filt_low;
                }
            }

            /* Now look at filt_high */
            if ((filt_direction >= 0) && (filt_high != filt_mid)) {
                if (ss_err[filt_high] == 0) {
                    YV12buffer.copyY(saved_frame, cm.frame_to_show);
                    vp8cx_set_alt_lf_level(cpi, filt_high);
                    LoopFilter.vp8_loop_filter_frame_yonly(cm, cpi.mb.e_mbd, filt_high);

                    filt_err = OnyxIf.vp8_calc_ss_err(sd, cm.frame_to_show);
                    ss_err[filt_high] = filt_err;
                } else {
                    filt_err = ss_err[filt_high];
                }

                /* Was it better than the previous best? */
                if (filt_err < (best_err - Bias)) {
                    best_err = filt_err;
                    filt_best = filt_high;
                }
            }

            /*
             * Half the step distance if the best filter value was the same as last time
             */
            if (filt_best == filt_mid) {
                filter_step = (short) (filter_step / 2);
                filt_direction = 0;
            } else {
                filt_direction = (filt_best < filt_mid) ? -1 : 1;
                filt_mid = filt_best;
            }
        }

        cm.filter_level = filt_best;

        /* restore unfiltered frame pointer */
        cm.frame_to_show = saved_frame;
    }

}
