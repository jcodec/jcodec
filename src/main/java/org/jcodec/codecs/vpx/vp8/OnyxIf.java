package org.jcodec.codecs.vpx.vp8;

import java.util.Arrays;
import java.util.EnumSet;

import org.jcodec.codecs.vpx.vp8.RateCtrl.FrameLimits;
import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.CodecPkt;
import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.Config;
import org.jcodec.codecs.vpx.vp8.data.FrameContext;
import org.jcodec.codecs.vpx.vp8.data.LayerContext;
import org.jcodec.codecs.vpx.vp8.data.Lookahead;
import org.jcodec.codecs.vpx.vp8.data.LookaheadEntry;
import org.jcodec.codecs.vpx.vp8.data.MBModeInfo;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.OnyxInt;
import org.jcodec.codecs.vpx.vp8.data.Psnr;
import org.jcodec.codecs.vpx.vp8.data.ReferenceCounts;
import org.jcodec.codecs.vpx.vp8.data.SpeedFeatures;
import org.jcodec.codecs.vpx.vp8.data.TokenExtra;
import org.jcodec.codecs.vpx.vp8.data.TokenList;
import org.jcodec.codecs.vpx.vp8.data.UsecTimer;
import org.jcodec.codecs.vpx.vp8.data.VarianceResults;
import org.jcodec.codecs.vpx.vp8.data.YV12buffer;
import org.jcodec.codecs.vpx.vp8.enums.EndUsage;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.FrameTypeFlags;
import org.jcodec.codecs.vpx.vp8.enums.LoopFilterType;
import org.jcodec.codecs.vpx.vp8.enums.MBLvlFeatures;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.PacketKind;
import org.jcodec.codecs.vpx.vp8.enums.Scaling;
import org.jcodec.codecs.vpx.vp8.enums.SearchMethods;
import org.jcodec.codecs.vpx.vp8.enums.SkinDetectionBlockSize;
import org.jcodec.codecs.vpx.vp8.enums.ThrModes;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;
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
public class OnyxIf {

    static final int[] thresh_mult_map_znn = {
            /* map common to zero, nearest, and near */
            0, GOOD(2), 1500, GOOD(3), 2000, RT(0), 1000, RT(2), 2000, Integer.MAX_VALUE };

    static final int[] thresh_mult_map_vhpred = { 1000, GOOD(2), 1500, GOOD(3), 2000, RT(0), 1000, RT(1), 2000, RT(7),
            Integer.MAX_VALUE, Integer.MAX_VALUE };

    static final int[] thresh_mult_map_bpred = { 2000, GOOD(0), 2500, GOOD(2), 5000, GOOD(3), 7500, RT(0), 2500, RT(1),
            5000, RT(6), Integer.MAX_VALUE, Integer.MAX_VALUE };

    static final int[] thresh_mult_map_tm = { 1000, GOOD(2), 1500, GOOD(3), 2000, RT(0), 0, RT(1), 1000, RT(2), 2000,
            RT(7), Integer.MAX_VALUE, Integer.MAX_VALUE };

    static final int[] thresh_mult_map_new1 = { 1000, GOOD(2), 2000, RT(0), 2000, Integer.MAX_VALUE };

    static final int[] thresh_mult_map_new2 = { 1000, GOOD(2), 2000, GOOD(3), 2500, GOOD(5), 4000, RT(0), 2000, RT(2),
            2500, RT(5), 4000, Integer.MAX_VALUE };

    static final int[] thresh_mult_map_split1 = { 2500, GOOD(0), 1700, GOOD(2), 10000, GOOD(3), 25000, GOOD(4),
            Integer.MAX_VALUE, RT(0), 5000, RT(1), 10000, RT(2), 25000, RT(3), Integer.MAX_VALUE, Integer.MAX_VALUE };

    static final int[] thresh_mult_map_split2 = { 5000, GOOD(0), 4500, GOOD(2), 20000, GOOD(3), 50000, GOOD(4),
            Integer.MAX_VALUE, RT(0), 10000, RT(1), 20000, RT(2), 50000, RT(3), Integer.MAX_VALUE, Integer.MAX_VALUE };

    static final int[] mode_check_freq_map_zn2 = {
            /* {zero,nearest}{2,3} */
            0, RT(10), 1 << 1, RT(11), 1 << 2, RT(12), 1 << 3, Integer.MAX_VALUE };

    static final int[] mode_check_freq_map_vhbpred = { 0, GOOD(5), 2, RT(0), 0, RT(3), 2, RT(5), 4, Integer.MAX_VALUE };

    static final int[] mode_check_freq_map_near2 = { 0, GOOD(5), 2, RT(0), 0, RT(3), 2, RT(10), 1 << 2, RT(11), 1 << 3,
            RT(12), 1 << 4, Integer.MAX_VALUE };

    static final int[] mode_check_freq_map_new1 = { 0, RT(10), 1 << 1, RT(11), 1 << 2, RT(12), 1 << 3,
            Integer.MAX_VALUE };

    static final int[] mode_check_freq_map_new2 = { 0, GOOD(5), 4, RT(0), 0, RT(3), 4, RT(10), 1 << 3, RT(11), 1 << 4,
            RT(12), 1 << 5, Integer.MAX_VALUE };

    static final int[] mode_check_freq_map_split1 = { 0, GOOD(2), 2, GOOD(3), 7, RT(1), 2, RT(2), 7,
            Integer.MAX_VALUE };

    static final int[] mode_check_freq_map_split2 = { 0, GOOD(1), 2, GOOD(2), 4, GOOD(3), 15, RT(1), 4, RT(2), 15,
            Integer.MAX_VALUE };

    static final short[] kf_high_motion_minq = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5,
            5, 5, 6, 6, 6, 6, 7, 7, 8, 8, 8, 8, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 13, 13, 13, 13, 14, 14,
            15, 15, 15, 15, 16, 16, 16, 16, 17, 17, 18, 18, 18, 18, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 22, 22, 23,
            23, 24, 25, 25, 26, 26, 27, 28, 28, 29, 30 };
    static final short[] gf_high_motion_minq = { 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6,
            7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 19,
            19, 20, 20, 21, 21, 22, 22, 23, 23, 24, 24, 25, 25, 26, 26, 27, 27, 28, 28, 29, 29, 30, 30, 31, 31, 32, 32,
            33, 33, 34, 34, 35, 35, 36, 36, 37, 37, 38, 38, 39, 39, 40, 40, 41, 41, 42, 42, 43, 44, 45, 46, 47, 48, 49,
            50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80 };

    static final short[] inter_minq = { 0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 6, 6, 7, 8, 8, 9, 9, 10, 11, 11, 12, 13, 13, 14,
            15, 15, 16, 17, 17, 18, 19, 20, 20, 21, 22, 22, 23, 24, 24, 25, 26, 27, 27, 28, 29, 30, 30, 31, 32, 33, 33,
            34, 35, 36, 36, 37, 38, 39, 39, 40, 41, 42, 42, 43, 44, 45, 46, 46, 47, 48, 49, 50, 50, 51, 52, 53, 54, 55,
            55, 56, 57, 58, 59, 60, 60, 61, 62, 63, 64, 65, 66, 67, 67, 68, 69, 70, 71, 72, 73, 74, 75, 75, 76, 77, 78,
            79, 80, 81, 82, 83, 84, 85, 86, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100 };

    static int GOOD(final int x) {
        return x + 1;
    }

    static int RT(final int x) {
        return x + 7;
    }

    static int speed_map(int speed, int[] map) {
        int res;
        int idx = 0;

        do {
            res = map[idx++];
        } while (speed >= map[idx++]);
        return res;
    }

    public static void vp8_set_speed_features(Compressor cpi) {
        SpeedFeatures sf = cpi.sf;
        int Mode = cpi.compressor_speed;
        int Speed = cpi.Speed;
        int Speed2;
        int i;
        CommonData cm = cpi.common;
        int ref_frames;

        /* Initialise default mode frequency sampling variables */
        for (i = 0; i < Block.MAX_MODES; ++i) {
            cpi.mode_check_freq[i] = 0;
        }

        cpi.mb.resetSpeedFeatures();

        /* best quality defaults */
        sf.RD = true;
        sf.search_method = SearchMethods.NSTEP;
        sf.improved_quant = true;
        sf.improved_dct = true;
        sf.auto_filter = true;
        sf.recode_loop = 1;
        sf.quarter_pixel_search = true;
        sf.half_pixel_search = true;
        sf.iterative_sub_pixel = true;
        sf.optimize_coefficients = true;
        sf.use_fastquant_for_pick = false;
        sf.no_skip_block4x4_search = true;

        sf.first_step = 0;
        sf.max_step_search_steps = MComp.MAX_MVSEARCH_STEPS;
        sf.improved_mv_pred = true;

        /* default thresholds to 0 */
        for (i = 0; i < Block.MAX_MODES; ++i)
            sf.thresh_mult[i] = 0;

        /* Count enabled references */
        ref_frames = 1;
        if (cpi.ref_frame_flags.contains(MVReferenceFrame.LAST_FRAME))
            ref_frames++;
        if (cpi.ref_frame_flags.contains(MVReferenceFrame.GOLDEN_FRAME))
            ref_frames++;
        if (cpi.ref_frame_flags.contains(MVReferenceFrame.ALTREF_FRAME))
            ref_frames++;

        /* Convert speed to continuous range, with clamping */
        if (Mode == 0) {
            Speed = 0;
        } else if (Mode == 2) {
            Speed = RT(Speed);
        } else {
            if (Speed > 5)
                Speed = 5;
            Speed = GOOD(Speed);
        }

        sf.thresh_mult[ThrModes.THR_ZERO1
                .ordinal()] = sf.thresh_mult[ThrModes.THR_NEAREST1.ordinal()] = sf.thresh_mult[ThrModes.THR_NEAR1
                        .ordinal()] = sf.thresh_mult[ThrModes.THR_DC.ordinal()] = 0; /* always */

        sf.thresh_mult[ThrModes.THR_ZERO2.ordinal()] = sf.thresh_mult[ThrModes.THR_ZERO3
                .ordinal()] = sf.thresh_mult[ThrModes.THR_NEAREST2.ordinal()] = sf.thresh_mult[ThrModes.THR_NEAREST3
                        .ordinal()] = sf.thresh_mult[ThrModes.THR_NEAR2.ordinal()] = sf.thresh_mult[ThrModes.THR_NEAR3
                                .ordinal()] = speed_map(Speed, thresh_mult_map_znn);

        sf.thresh_mult[ThrModes.THR_V_PRED.ordinal()] = sf.thresh_mult[ThrModes.THR_H_PRED.ordinal()] = speed_map(Speed,
                thresh_mult_map_vhpred);
        sf.thresh_mult[ThrModes.THR_B_PRED.ordinal()] = speed_map(Speed, thresh_mult_map_bpred);
        sf.thresh_mult[ThrModes.THR_TM.ordinal()] = speed_map(Speed, thresh_mult_map_tm);
        sf.thresh_mult[ThrModes.THR_NEW1.ordinal()] = speed_map(Speed, thresh_mult_map_new1);
        sf.thresh_mult[ThrModes.THR_NEW2.ordinal()] = sf.thresh_mult[ThrModes.THR_NEW3.ordinal()] = speed_map(Speed,
                thresh_mult_map_new2);
        sf.thresh_mult[ThrModes.THR_SPLIT1.ordinal()] = speed_map(Speed, thresh_mult_map_split1);
        sf.thresh_mult[ThrModes.THR_SPLIT2.ordinal()] = sf.thresh_mult[ThrModes.THR_SPLIT3.ordinal()] = speed_map(Speed,
                thresh_mult_map_split2);

        // Special case for temporal layers.
        // Reduce the thresholds for zero/nearest/near for GOLDEN, if GOLDEN is
        // used as second reference. We don't modify thresholds for ALTREF case
        // since ALTREF is usually used as long-term reference in temporal layers.
        if ((cpi.Speed <= 6) && (cpi.oxcf.number_of_layers > 1)
                && (cpi.ref_frame_flags.contains(MVReferenceFrame.LAST_FRAME))
                && (cpi.ref_frame_flags.contains(MVReferenceFrame.GOLDEN_FRAME))) {
            if (cpi.closest_reference_frame == MVReferenceFrame.GOLDEN_FRAME) {
                sf.thresh_mult[ThrModes.THR_ZERO2.ordinal()] >>= 3;
                sf.thresh_mult[ThrModes.THR_NEAREST2.ordinal()] >>= 3;
                sf.thresh_mult[ThrModes.THR_NEAR2.ordinal()] >>= 3;
            } else {
                sf.thresh_mult[ThrModes.THR_ZERO2.ordinal()] >>= 1;
                sf.thresh_mult[ThrModes.THR_NEAREST2.ordinal()] >>= 1;
                sf.thresh_mult[ThrModes.THR_NEAR2.ordinal()] >>= 1;
            }
        }

        cpi.mode_check_freq[ThrModes.THR_ZERO1.ordinal()] = cpi.mode_check_freq[ThrModes.THR_NEAREST1
                .ordinal()] = cpi.mode_check_freq[ThrModes.THR_NEAR1.ordinal()] = cpi.mode_check_freq[ThrModes.THR_TM
                        .ordinal()] = cpi.mode_check_freq[ThrModes.THR_DC.ordinal()] = 0; /* always */

        cpi.mode_check_freq[ThrModes.THR_ZERO2.ordinal()] = cpi.mode_check_freq[ThrModes.THR_ZERO3
                .ordinal()] = cpi.mode_check_freq[ThrModes.THR_NEAREST2
                        .ordinal()] = cpi.mode_check_freq[ThrModes.THR_NEAREST3.ordinal()] = speed_map(Speed,
                                mode_check_freq_map_zn2);

        cpi.mode_check_freq[ThrModes.THR_NEAR2.ordinal()] = cpi.mode_check_freq[ThrModes.THR_NEAR3
                .ordinal()] = speed_map(Speed, mode_check_freq_map_near2);

        cpi.mode_check_freq[ThrModes.THR_V_PRED.ordinal()] = cpi.mode_check_freq[ThrModes.THR_H_PRED
                .ordinal()] = cpi.mode_check_freq[ThrModes.THR_B_PRED.ordinal()] = speed_map(Speed,
                        mode_check_freq_map_vhbpred);

        // For real-time mode at speed 10 keep the mode_check_freq threshold
        // for NEW1 similar to that of speed 9.
        Speed2 = Speed;
        if (cpi.Speed == 10 && Mode == 2)
            Speed2 = RT(9);
        cpi.mode_check_freq[ThrModes.THR_NEW1.ordinal()] = speed_map(Speed2, mode_check_freq_map_new1);

        cpi.mode_check_freq[ThrModes.THR_NEW2.ordinal()] = cpi.mode_check_freq[ThrModes.THR_NEW3.ordinal()] = speed_map(
                Speed, mode_check_freq_map_new2);

        cpi.mode_check_freq[ThrModes.THR_SPLIT1.ordinal()] = speed_map(Speed, mode_check_freq_map_split1);
        cpi.mode_check_freq[ThrModes.THR_SPLIT2.ordinal()] = cpi.mode_check_freq[ThrModes.THR_SPLIT3
                .ordinal()] = speed_map(Speed, mode_check_freq_map_split2);
        Speed = cpi.Speed;
        switch (Mode) {
        case 0: /* best quality mode */
            sf.first_step = 0;
            sf.max_step_search_steps = MComp.MAX_MVSEARCH_STEPS;
            break;
        case 1:
        case 3:
            if (Speed > 0) {
                /* Disable coefficient optimization above speed 0 */
                sf.optimize_coefficients = false;
                sf.use_fastquant_for_pick = true;
                sf.no_skip_block4x4_search = false;

                sf.first_step = 1;
            }

            if (Speed > 2) {
                sf.improved_quant = false;
                sf.improved_dct = false;

                /*
                 * Only do recode loop on key frames, golden frames and alt ref frames
                 */
                sf.recode_loop = 2;
            }

            if (Speed > 3) {
                sf.auto_filter = true;
                sf.recode_loop = 0; /* recode loop off */
                sf.RD = false; /* Turn rd off */
            }

            if (Speed > 4) {
                sf.auto_filter = false; /* Faster selection of loop filter */
            }

            break;
        case 2:
            sf.optimize_coefficients = false;
            sf.recode_loop = 0;
            sf.auto_filter = true;
            sf.iterative_sub_pixel = true;
            sf.search_method = SearchMethods.NSTEP;

            if (Speed > 0) {
                sf.improved_quant = false;
                sf.improved_dct = false;

                sf.use_fastquant_for_pick = true;
                sf.no_skip_block4x4_search = false;
                sf.first_step = 1;
            }

            if (Speed > 2)
                sf.auto_filter = false; /* Faster selection of loop filter */

            if (Speed > 3) {
                sf.RD = false;
                sf.auto_filter = true;
            }

            if (Speed > 4) {
                sf.auto_filter = false; /* Faster selection of loop filter */
                sf.search_method = SearchMethods.HEX;
                sf.iterative_sub_pixel = false;
            }

            if (Speed > 6) {
                int sum = 0;
                int total_mbs = cm.MBs;
                int thresh;
                int total_skip;

                int min = 2000;

                if (cpi.oxcf.encode_breakout > 2000)
                    min = cpi.oxcf.encode_breakout;

                min >>= 7;

                for (i = 0; i < min; ++i) {
                    sum += cpi.mb.error_bins[i];
                }

                total_skip = sum;
                sum = 0;

                /* i starts from 2 to make sure thresh started from 2048 */
                for (; i < 1024; ++i) {
                    sum += cpi.mb.error_bins[i];

                    if (10 * sum >= (int) (cpi.Speed - 6) * (total_mbs - total_skip)) {
                        break;
                    }
                }

                i--;
                thresh = (i << 7);

                if (thresh < 2000)
                    thresh = 2000;

                if (ref_frames > 1) {
                    sf.thresh_mult[ThrModes.THR_NEW1.ordinal()] = thresh;
                    sf.thresh_mult[ThrModes.THR_NEAREST1.ordinal()] = thresh >> 1;
                    sf.thresh_mult[ThrModes.THR_NEAR1.ordinal()] = thresh >> 1;
                }

                if (ref_frames > 2) {
                    sf.thresh_mult[ThrModes.THR_NEW2.ordinal()] = thresh << 1;
                    sf.thresh_mult[ThrModes.THR_NEAREST2.ordinal()] = thresh;
                    sf.thresh_mult[ThrModes.THR_NEAR2.ordinal()] = thresh;
                }

                if (ref_frames > 3) {
                    sf.thresh_mult[ThrModes.THR_NEW3.ordinal()] = thresh << 1;
                    sf.thresh_mult[ThrModes.THR_NEAREST3.ordinal()] = thresh;
                    sf.thresh_mult[ThrModes.THR_NEAR3.ordinal()] = thresh;
                }

                sf.improved_mv_pred = false;
            }

            if (Speed > 8)
                sf.quarter_pixel_search = false;

            if (cm.getVersion() == 0) {
                cm.filter_type = LoopFilterType.NORMAL;

                if (Speed >= 14)
                    cm.filter_type = LoopFilterType.SIMPLE;
            } else {
                cm.filter_type = LoopFilterType.SIMPLE;
            }

            /* This has a big hit on quality. Last resort */
            if (Speed >= 15)
                sf.half_pixel_search = false;

            CommonUtils.vp8_zero(cpi.mb.error_bins);

        }

        cpi.mb.changeFNs(cpi);

        if (cpi.sf.iterative_sub_pixel) {
            cpi.find_fractional_mv_step = MComp.vp8_find_best_sub_pixel_step_iteratively;
        } else if (cpi.sf.quarter_pixel_search) {
            cpi.find_fractional_mv_step = MComp.vp8_find_best_sub_pixel_step;
        } else if (cpi.sf.half_pixel_search) {
            cpi.find_fractional_mv_step = MComp.vp8_find_best_half_pixel_step;
        } else {
            cpi.find_fractional_mv_step = MComp.vp8_skip_fractional_mv_step;
        }

        if (cpi.common.full_pixel) {
            cpi.find_fractional_mv_step = MComp.vp8_skip_fractional_mv_step;
        }

    }

    static void save_layer_context(Compressor cpi) {
        LayerContext lc = cpi.layer_context[cpi.current_layer];

        /* Save layer dependent coding state */
        lc.target_bandwidth = cpi.target_bandwidth;
        lc.starting_buffer_level = cpi.oxcf.starting_buffer_level;
        lc.optimal_buffer_level = cpi.oxcf.optimal_buffer_level;
        lc.maximum_buffer_size = cpi.oxcf.maximum_buffer_size;
        lc.starting_buffer_level_in_ms = cpi.oxcf.starting_buffer_level_in_ms;
        lc.optimal_buffer_level_in_ms = cpi.oxcf.optimal_buffer_level_in_ms;
        lc.maximum_buffer_size_in_ms = cpi.oxcf.maximum_buffer_size_in_ms;
        lc.buffer_level = cpi.buffer_level;
        lc.bits_off_target = cpi.bits_off_target;
        lc.total_actual_bits = cpi.total_actual_bits;
        lc.worst_quality = cpi.worst_quality;
        lc.active_worst_quality = cpi.active_worst_quality;
        lc.best_quality = cpi.best_quality;
        lc.active_best_quality = cpi.active_best_quality;
        lc.ni_av_qi = cpi.ni_av_qi;
        lc.ni_tot_qi = cpi.ni_tot_qi;
        lc.ni_frames = cpi.ni_frames;
        lc.avg_frame_qindex = cpi.avg_frame_qindex;
        lc.rate_correction_factor = cpi.rate_correction_factor;
        lc.key_frame_rate_correction_factor = cpi.key_frame_rate_correction_factor;
        lc.gf_rate_correction_factor = cpi.gf_rate_correction_factor;
        lc.zbin_over_quant = cpi.mb.zbin_over_quant;
        lc.inter_frame_target = cpi.inter_frame_target;
        lc.total_byte_count = cpi.total_byte_count;
        lc.filter_level = cpi.common.filter_level;
        lc.frames_since_last_drop_overshoot = cpi.frames_since_last_drop_overshoot;
        lc.force_maxqp = cpi.force_maxqp;
        lc.last_frame_percent_intra = cpi.last_frame_percent_intra;
        lc.last_q[0] = cpi.last_q[0];
        lc.last_q[1] = cpi.last_q[1];

        lc.count_mb_ref_frame_usage.clear();
        lc.count_mb_ref_frame_usage.putAll(cpi.mb.count_mb_ref_frame_usage);
    }

    static void restore_layer_context(Compressor cpi, int layer) {
        LayerContext lc = cpi.layer_context[layer];

        /* Restore layer dependent coding state */
        cpi.current_layer = layer;
        cpi.target_bandwidth = lc.target_bandwidth;
        cpi.oxcf.target_bandwidth = lc.target_bandwidth;
        cpi.oxcf.starting_buffer_level = lc.starting_buffer_level;
        cpi.oxcf.optimal_buffer_level = lc.optimal_buffer_level;
        cpi.oxcf.maximum_buffer_size = lc.maximum_buffer_size;
        cpi.oxcf.starting_buffer_level_in_ms = lc.starting_buffer_level_in_ms;
        cpi.oxcf.optimal_buffer_level_in_ms = lc.optimal_buffer_level_in_ms;
        cpi.oxcf.maximum_buffer_size_in_ms = lc.maximum_buffer_size_in_ms;
        cpi.buffer_level = lc.buffer_level;
        cpi.bits_off_target = lc.bits_off_target;
        cpi.total_actual_bits = lc.total_actual_bits;
        cpi.active_worst_quality = lc.active_worst_quality;
        cpi.active_best_quality = lc.active_best_quality;
        cpi.ni_av_qi = lc.ni_av_qi;
        cpi.ni_tot_qi = lc.ni_tot_qi;
        cpi.ni_frames = lc.ni_frames;
        cpi.avg_frame_qindex = lc.avg_frame_qindex;
        cpi.rate_correction_factor = lc.rate_correction_factor;
        cpi.key_frame_rate_correction_factor = lc.key_frame_rate_correction_factor;
        cpi.gf_rate_correction_factor = lc.gf_rate_correction_factor;
        cpi.mb.zbin_over_quant = lc.zbin_over_quant;
        cpi.inter_frame_target = lc.inter_frame_target;
        cpi.total_byte_count = lc.total_byte_count;
        cpi.common.filter_level = lc.filter_level;
        cpi.frames_since_last_drop_overshoot = lc.frames_since_last_drop_overshoot;
        cpi.force_maxqp = lc.force_maxqp;
        cpi.last_frame_percent_intra = lc.last_frame_percent_intra;
        cpi.last_q[0] = lc.last_q[0];
        cpi.last_q[1] = lc.last_q[1];

        cpi.mb.count_mb_ref_frame_usage.clear();
        cpi.mb.count_mb_ref_frame_usage.putAll(lc.count_mb_ref_frame_usage);
    }

    public static int rescale(int val, int num, int denom) {
        long llnum = num;
        long llden = denom;
        long llval = val;

        return (int) (llval * llnum / llden);
    }

    // Upon a run-time change in temporal layers, reset the layer context parameters
    // for any "new" layers. For "existing" layers, let them inherit the parameters
    // from the previous layer state (at the same layer #). In future we may want
    // to better map the previous layer state(s) to the "new" ones.
    public static void reset_temporal_layer_change(Compressor cpi, Config oxcf, int prev_num_layers) {
        int i;
        double prev_layer_framerate = 0;
        final int curr_num_layers = cpi.oxcf.number_of_layers;
        // If the previous state was 1 layer, get current layer context from cpi.
        // We need this to set the layer context for the new layers below.
        if (prev_num_layers == 1) {
            cpi.current_layer = 0;
            save_layer_context(cpi);
        }
        for (i = 0; i < curr_num_layers; ++i) {
            LayerContext lc = cpi.layer_context[i];
            if (i >= prev_num_layers) {
                cpi.init_temporal_layer_context(i, prev_layer_framerate);
            }
            // The initial buffer levels are set based on their starting levels.
            // We could set the buffer levels based on the previous state (normalized
            // properly by the layer bandwidths) but we would need to keep track of
            // the previous set of layer bandwidths (i.e., target_bitrate[i])
            // before the layer change. For now, reset to the starting levels.
            lc.buffer_level = cpi.oxcf.starting_buffer_level_in_ms * cpi.oxcf.target_bitrate[i];
            lc.bits_off_target = lc.buffer_level;
            // TDOD(marpan): Should we set the rate_correction_factor and
            // active_worst/best_quality to values derived from the previous layer
            // state (to smooth-out quality dips/rate fluctuation at transition)?

            // We need to treat the 1 layer case separately: oxcf.target_bitrate[i]
            // is not set for 1 layer, and the restore_layer_context/save_context()
            // are not called in the encoding loop, so we need to call it here to
            // pass the layer context state to |cpi|.
            if (curr_num_layers == 1) {
                lc.target_bandwidth = cpi.oxcf.target_bandwidth;
                lc.buffer_level = cpi.oxcf.starting_buffer_level_in_ms * lc.target_bandwidth / 1000;
                lc.bits_off_target = lc.buffer_level;
                restore_layer_context(cpi, 0);
            }
            prev_layer_framerate = cpi.output_framerate / cpi.oxcf.rate_decimator[i];
        }
    }

    static void dealloc_compressor_data(Compressor cpi) {
        cpi.tplist = null;

        /* Delete last frame MV storage buffers */
        cpi.lfmv = null;

        cpi.lf_ref_frame_sign_bias = null;

        cpi.lf_ref_frame = null;

        /* Delete sementation map */
        cpi.segmentation_map = null;

        cpi.active_map = null;

        cpi.common.vp8_de_alloc_frame_buffers();

        cpi.pick_lf_lvl_frame = null;
        cpi.scaled_source = null;
        dealloc_raw_frame_buffers(cpi);

        cpi.tok = null;

        /* Structure used to monitor GF usage */
        cpi.gf_active_flags = null;

        /* Activity mask based per mb zbin adjustments */
        cpi.mb_activity_map = null;

        cpi.mb.pip = null;

    }

    static void enable_segmentation(Compressor cpi) {
        /* Set the appropriate feature bit */
        cpi.mb.e_mbd.segmentation_enabled = 1;
        cpi.mb.e_mbd.update_mb_segmentation_map = true;
        cpi.mb.e_mbd.update_mb_segmentation_data = true;
    }

    static void disable_segmentation(Compressor cpi) {
        /* Clear the appropriate feature bit */
        cpi.mb.e_mbd.segmentation_enabled = 0;
    }

    /*
     * Valid values for a segment are 0 to 3 Segmentation map is arrange as
     * [Rows][Columns]
     */
    static void set_segmentation_map(Compressor cpi, int[] segmentation_map) {
        /* Copy in the new segmentation map */
        CommonUtils.vp8_copy(segmentation_map, cpi.segmentation_map);

        /* Signal that the map should be updated. */
        cpi.mb.e_mbd.update_mb_segmentation_map = true;
        cpi.mb.e_mbd.update_mb_segmentation_data = true;
    }

    /*
     * The values given for each segment can be either deltas (from the default
     * value chosen for the frame) or absolute values.
     *
     * Valid range for abs values is: (0-127 for MB_LVL_ALT_Q), (0-63 for
     * SEGMENT_ALT_LF) Valid range for delta values are: (+/-127 for MB_LVL_ALT_Q),
     * (+/-63 for SEGMENT_ALT_LF)
     *
     * abs_delta = SEGMENT_DELTADATA (deltas) abs_delta = SEGMENT_ABSDATA (use the
     * absolute values given).
     *
     */
    static void set_segment_data(Compressor cpi, short[][] feature_data, boolean abs_delta) {
        cpi.mb.e_mbd.mb_segement_abs_delta = abs_delta;
        CommonUtils.vp8_copy(feature_data, cpi.segment_feature_data);
    }

    public static void dealloc_raw_frame_buffers(Compressor cpi) {
        cpi.alt_ref_buffer = null;
        if (cpi.lookahead != null)
            cpi.lookahead.vp8_lookahead_destroy();
    }

    /* A simple function to cyclically refresh the background at a lower Q */
    static void cyclic_background_refresh(Compressor cpi, short Q, short lf_adjustment) {
        int[] seg_map = cpi.segmentation_map;
        short[][] feature_data = new short[MBLvlFeatures.featureCount][BlockD.MAX_MB_SEGMENTS];
        int i;
        int block_count = cpi.cyclic_refresh_mode_max_mbs_perframe;
        int mbs_in_frame = cpi.common.mb_rows * cpi.common.mb_cols;

        cpi.cyclic_refresh_q = Q / 2;

        if (cpi.oxcf.screen_content_mode != 0) {
            // Modify quality ramp-up based on Q. Above some Q level, increase the
            // number of blocks to be refreshed, and reduce it below the thredhold.
            // Turn-off under certain conditions (i.e., away from key frame, and if
            // we are at good quality (low Q) and most of the blocks were
            // skipped-encoded
            // in previous frame.
            int qp_thresh = (cpi.oxcf.screen_content_mode == 2) ? 80 : 100;
            if (Q >= qp_thresh) {
                cpi.cyclic_refresh_mode_max_mbs_perframe = (cpi.common.mb_rows * cpi.common.mb_cols) / 10;
            } else if (cpi.frames_since_key > 250 && Q < 20 && cpi.mb.skip_true_count > (int) (0.95 * mbs_in_frame)) {
                cpi.cyclic_refresh_mode_max_mbs_perframe = 0;
            } else {
                cpi.cyclic_refresh_mode_max_mbs_perframe = (cpi.common.mb_rows * cpi.common.mb_cols) / 20;
            }
            block_count = cpi.cyclic_refresh_mode_max_mbs_perframe;
        }

        // Set every macroblock to be eligible for update.
        // For key frame this will reset seg map to 0.
        Arrays.fill(cpi.segmentation_map, 0, 0, mbs_in_frame);

        if (cpi.common.frame_type != FrameType.KEY_FRAME && block_count > 0) {
            /* Cycle through the macro_block rows */
            /* MB loop to set local segmentation map */
            i = cpi.cyclic_refresh_mode_index;
            assert (i < mbs_in_frame);
            do {
                /*
                 * If the MB is as a candidate for clean up then mark it for possible
                 * boost/refresh (segment 1) The segment id may get reset to 0 later if the MB
                 * gets coded anything other than last frame 0,0 as only (last frame 0,0) MBs
                 * are eligable for refresh : that is to say Mbs likely to be background blocks.
                 */
                if (cpi.cyclic_refresh_map[i] == 0) {
                    seg_map[i] = 1;
                    block_count--;
                } else if (cpi.cyclic_refresh_map[i] < 0) {
                    cpi.cyclic_refresh_map[i]++;
                }

                i++;
                if (i == mbs_in_frame)
                    i = 0;

            } while (block_count != 0 && i != cpi.cyclic_refresh_mode_index);

            cpi.cyclic_refresh_mode_index = i;

        }

        /* Activate segmentation. */
        cpi.mb.e_mbd.update_mb_segmentation_map = true;
        cpi.mb.e_mbd.update_mb_segmentation_data = true;
        enable_segmentation(cpi);

        /* Set up the quant segment data */
        feature_data[MBLvlFeatures.ALT_Q.ordinal()][0] = 0;
        feature_data[MBLvlFeatures.ALT_Q.ordinal()][1] = (short) (cpi.cyclic_refresh_q - Q);
        feature_data[MBLvlFeatures.ALT_Q.ordinal()][2] = 0;
        feature_data[MBLvlFeatures.ALT_Q.ordinal()][3] = 0;

        /* Set up the loop segment data */
        feature_data[MBLvlFeatures.ALT_LF.ordinal()][0] = 0;
        feature_data[MBLvlFeatures.ALT_LF.ordinal()][1] = lf_adjustment;
        feature_data[MBLvlFeatures.ALT_LF.ordinal()][2] = 0;
        feature_data[MBLvlFeatures.ALT_LF.ordinal()][3] = 0;

        /* Initialise the feature data structure */
        set_segment_data(cpi, feature_data, BlockD.SEGMENT_DELTADATA);
    }

    static void compute_skin_map(Compressor cpi) {
        int mb_row, mb_col, num_bl;
        CommonData cm = cpi.common;
        FullAccessIntArrPointer src_y = cpi.sourceYV12.y_buffer.shallowCopy();
        FullAccessIntArrPointer src_u = cpi.sourceYV12.u_buffer.shallowCopy();
        FullAccessIntArrPointer src_v = cpi.sourceYV12.v_buffer.shallowCopy();
        final int src_ystride = cpi.sourceYV12.y_stride;
        final int src_uvstride = cpi.sourceYV12.uv_stride;

        final SkinDetectionBlockSize bsize = (cm.Width * cm.Height <= 352 * 288) ? SkinDetectionBlockSize.SKIN_8x8
                : SkinDetectionBlockSize.SKIN_16x16;

        for (mb_row = 0; mb_row < cm.mb_rows; mb_row++) {
            num_bl = 0;
            for (mb_col = 0; mb_col < cm.mb_cols; mb_col++) {
                final int bl_index = mb_row * cm.mb_cols + mb_col;
                cpi.skin_map[bl_index] = SkinDetect.vp8_compute_skin_block(src_y, src_u, src_v, src_ystride,
                        src_uvstride, bsize, cpi.consec_zero_last[bl_index], 0);
                num_bl++;
                src_y.incBy(16);
                src_u.incBy(8);
                src_v.incBy(8);
            }
            src_y.incBy((src_ystride << 4) - (num_bl << 4));
            src_u.incBy((src_uvstride << 3) - (num_bl << 3));
            src_v.incBy((src_uvstride << 3) - (num_bl << 3));
        }

        // Remove isolated skin blocks (none of its neighbors are skin) and isolated
        // non-skin blocks (all of its neighbors are skin). Skip the boundary.
        for (mb_row = 1; mb_row < cm.mb_rows - 1; mb_row++) {
            for (mb_col = 1; mb_col < cm.mb_cols - 1; mb_col++) {
                final int bl_index = mb_row * cm.mb_cols + mb_col;
                int num_neighbor = 0;
                int mi, mj;
                int non_skin_threshold = 8;

                for (mi = -1; mi <= 1; mi += 1) {
                    for (mj = -1; mj <= 1; mj += 1) {
                        int bl_neighbor_index = (mb_row + mi) * cm.mb_cols + mb_col + mj;
                        if (cpi.skin_map[bl_neighbor_index])
                            num_neighbor++;
                    }
                }

                if (cpi.skin_map[bl_index] && num_neighbor < 2)
                    cpi.skin_map[bl_index] = false;
                if (!cpi.skin_map[bl_index] && num_neighbor == non_skin_threshold)
                    cpi.skin_map[bl_index] = true;
            }
        }
    }

    public static void alloc_raw_frame_buffers(Compressor cpi) {
        int width = (cpi.oxcf.Width + 15) & ~15;
        int height = (cpi.oxcf.Height + 15) & ~15;

        cpi.lookahead = new Lookahead(cpi.oxcf.Width, cpi.oxcf.Height, cpi.oxcf.lag_in_frames);
        cpi.alt_ref_buffer = new YV12buffer(width, height);
    }

    public static void vp8_alloc_compressor_data(Compressor cpi) {
        CommonData cm = cpi.common;

        int width = cm.Width;
        int height = cm.Height;

        cm.vp8_alloc_frame_buffers(width, height);

        if ((width & 0xf) != 0)
            width += 16 - (width & 0xf);

        if ((height & 0xf) != 0)
            height += 16 - (height & 0xf);

        cpi.pick_lf_lvl_frame = new YV12buffer(width, height);
        cpi.scaled_source = new YV12buffer(width, height);

        {
            int tokens = cm.mb_rows * cm.mb_cols * 24 * 16;
            cpi.tok = new FullAccessGenArrPointer<TokenExtra>(tokens);
            for (int i = 0; i < tokens; i++) {
                cpi.tok.setRel(i, null);
            }
        }

        /* Data used for real time vc mode to see if gf needs refreshing */
        cpi.zeromv_count = 0;

        /* Structures used to monitor GF usage */
        cpi.gf_active_flags = new FullAccessIntArrPointer(cm.mb_rows * cm.mb_cols);
        cpi.gf_active_count = cm.mb_rows * cm.mb_cols;

        cpi.mb_activity_map = new FullAccessIntArrPointer(cpi.gf_active_count);

        /* allocate memory for storing last frame's MVs for MV prediction. */
        cpi.lfmv = new MV[(cm.mb_rows + 2) * (cm.mb_cols + 2)];
        for (int i = 0; i < cpi.lfmv.length; i++) {
            cpi.lfmv[i] = new MV();
        }
        cpi.lf_ref_frame_sign_bias = new boolean[cpi.lfmv.length];
        cpi.lf_ref_frame = new MVReferenceFrame[cpi.lfmv.length];

        /* Create the encoder segmentation map and set all entries to 0 */
        cpi.segmentation_map = new int[cpi.gf_active_count];
        cpi.cyclic_refresh_mode_index = 0;
        cpi.active_map = new FullAccessIntArrPointer(cpi.gf_active_count);
        cpi.active_map.memset(0, (short) 1, cpi.gf_active_count);
        cpi.tplist = new TokenList[cm.mb_rows];
        for (int i = 0; i < cm.mb_rows; i++) {
            cpi.tplist[i] = new TokenList();
        }
    }

    /* Quant MOD */
    public static final short q_trans[] = { 0, 1, 2, 3, 4, 5, 7, 8, 9, 10, 12, 13, 15, 17, 18, 19, 20, 21, 23, 24, 25,
            26, 27, 28, 29, 30, 31, 33, 35, 37, 39, 41, 43, 45, 47, 49, 51, 53, 55, 57, 59, 61, 64, 67, 70, 73, 76, 79,
            82, 85, 88, 91, 94, 97, 100, 103, 106, 109, 112, 115, 118, 121, 124, 127, };

    static int vp8_reverse_trans(int x) {
        int i;

        for (i = 0; i < 64; ++i) {
            if (q_trans[i] >= x)
                return i;
        }

        return 63;
    }

    static void update_layer_contexts(Compressor cpi) {
        Config oxcf = cpi.oxcf;

        /* Update snapshots of the layer contexts to reflect new parameters */
        if (oxcf.number_of_layers > 1) {
            int i;
            double prev_layer_framerate = 0;

            assert (oxcf.number_of_layers <= OnyxIf.VPX_TS_MAX_LAYERS);
            for (i = 0; i < oxcf.number_of_layers && i < OnyxIf.VPX_TS_MAX_LAYERS; ++i) {
                LayerContext lc = cpi.layer_context[i];

                lc.framerate = cpi.ref_framerate / oxcf.rate_decimator[i];
                lc.target_bandwidth = oxcf.target_bitrate[i] * 1000;

                lc.starting_buffer_level = rescale((int) oxcf.starting_buffer_level_in_ms, lc.target_bandwidth, 1000);

                if (oxcf.optimal_buffer_level == 0) {
                    lc.optimal_buffer_level = lc.target_bandwidth / 8;
                } else {
                    lc.optimal_buffer_level = rescale((int) oxcf.optimal_buffer_level_in_ms, lc.target_bandwidth, 1000);
                }

                if (oxcf.maximum_buffer_size == 0) {
                    lc.maximum_buffer_size = lc.target_bandwidth / 8;
                } else {
                    lc.maximum_buffer_size = rescale((int) oxcf.maximum_buffer_size_in_ms, lc.target_bandwidth, 1000);
                }

                /* Work out the average size of a frame within this layer */
                if (i > 0) {
                    lc.avg_frame_size_for_layer = (int) ((oxcf.target_bitrate[i] - oxcf.target_bitrate[i - 1]) * 1000
                            / (lc.framerate - prev_layer_framerate));
                }

                prev_layer_framerate = lc.framerate;
            }
        }
    }

    public static double log2f(double x) {
        final double log2e = 0.693147180559945309417;
        return Math.log(x) / log2e;
    }

    static long calc_plane_error(FullAccessIntArrPointer orig, int orig_stride, FullAccessIntArrPointer recon,
            int recon_stride, int cols, int rows) {
        int row, col;
        long total_sse = 0;
        int diff;
        orig = orig.shallowCopy();
        recon = recon.shallowCopy();
        VarianceResults sse = new VarianceResults();

        for (row = 0; row + 16 <= rows; row += 16) {
            for (col = 0; col + 16 <= cols; col += 16) {
                orig.incBy(col);
                recon.incBy(col);
                Variance.vpx_mse16x16.call(orig, orig_stride, recon, recon_stride, sse);
                orig.incBy(-col);
                recon.incBy(-col);
                total_sse += sse.sse;
            }

            /* Handle odd-sized width */
            if (col < cols) {
                final int bordOrigPos = orig.getPos();
                final int bordRecPos = recon.getPos();

                for (int border_row = 0; border_row < 16; ++border_row) {
                    for (int border_col = col; border_col < cols; ++border_col) {
                        diff = orig.getRel(border_col) - recon.getRel(border_col);
                        total_sse += diff * diff;
                    }

                    orig.incBy(orig_stride);
                    recon.incBy(recon_stride);
                }
                orig.setPos(bordOrigPos);
                recon.setPos(bordRecPos);
            }

            orig.incBy(orig_stride * 16);
            recon.incBy(recon_stride * 16);
        }

        /* Handle odd-sized height */
        for (; row < rows; ++row) {
            for (col = 0; col < cols; ++col) {
                diff = orig.getRel(col) - recon.getRel(col);
                total_sse += diff * diff;
            }

            orig.incBy(orig_stride);
            recon.incBy(recon_stride);
        }

        return total_sse;
    }

    static void generate_psnr_packet(Compressor cpi) {
        YV12buffer orig = cpi.sourceYV12;
        YV12buffer recon = cpi.common.frame_to_show;
        CodecPkt pkt = new CodecPkt();
        long sse;
        int i;
        int width = cpi.common.Width;
        int height = cpi.common.Height;

        pkt.kind = PacketKind.PSNR_PKT;
        sse = calc_plane_error(orig.y_buffer, orig.y_stride, recon.y_buffer, recon.y_stride, width, height);
        CodecPkt.PSNRPacket psnrp = new CodecPkt.PSNRPacket();
        psnrp.sse[0] = sse;
        psnrp.sse[1] = sse;
        psnrp.samples[0] = width * height;
        psnrp.samples[1] = width * height;

        width = (width + 1) / 2;
        height = (height + 1) / 2;

        sse = calc_plane_error(orig.u_buffer, orig.uv_stride, recon.u_buffer, recon.uv_stride, width, height);
        psnrp.sse[0] += sse;
        psnrp.sse[2] = sse;
        psnrp.samples[0] += width * height;
        psnrp.samples[2] = width * height;

        sse = calc_plane_error(orig.v_buffer, orig.uv_stride, recon.v_buffer, recon.uv_stride, width, height);
        psnrp.sse[0] += sse;
        psnrp.sse[3] = sse;
        psnrp.samples[0] += width * height;
        psnrp.samples[3] = width * height;

        for (i = 0; i < 4; ++i) {
            psnrp.psnr[i] = Psnr.vpx_sse_to_psnr(psnrp.samples[i], 255.0, (double) (psnrp.sse[i]));
        }

        pkt.packet = psnrp;
        cpi.output_pkt_list.add(pkt);
    }

    static void vp8_use_as_reference(Compressor cpi, EnumSet<MVReferenceFrame> ref_frame_flags) {
        cpi.ref_frame_flags = EnumSet.copyOf(ref_frame_flags);
    }

    static void vp8_update_reference(Compressor cpi, EnumSet<MVReferenceFrame> ref_frame_flags) {
        cpi.common.refresh_golden_frame = false;
        cpi.common.refresh_alt_ref_frame = false;
        cpi.common.refresh_last_frame = false;

        if (ref_frame_flags.contains(MVReferenceFrame.LAST_FRAME))
            cpi.common.refresh_last_frame = true;

        if (ref_frame_flags.contains(MVReferenceFrame.GOLDEN_FRAME))
            cpi.common.refresh_golden_frame = true;

        if (ref_frame_flags.contains(MVReferenceFrame.ALTREF_FRAME))
            cpi.common.refresh_alt_ref_frame = true;

        cpi.ext_refresh_frame_flags_pending = true;
    }

    static void vp8_get_reference(Compressor cpi, MVReferenceFrame ref_frame_flag, YV12buffer sd) {
        YV12buffer.copyFrame(cpi.common.yv12_fb[getFBIdx(cpi.common, ref_frame_flag)], sd);
    }

    private static int getFBIdx(CommonData cm, MVReferenceFrame ref_frame_flag) {
        return cm.frameIdxs.get(ref_frame_flag);
    }

    static void vp8_set_reference(Compressor cpi, MVReferenceFrame ref_frame_flag, YV12buffer sd) {
        YV12buffer.copyFrame(sd, cpi.common.yv12_fb[getFBIdx(cpi.common, ref_frame_flag)]);
    }

    static void vp8_update_entropy(Compressor cpi, boolean update) {
        cpi.common.refresh_entropy_probs = update;
    }

    static void scale_and_extend_source(YV12buffer sd, Compressor cpi) {
        CommonData cm = cpi.common;

        /* are we resizing the image */
        if (cm.horiz_scale != Scaling.NORMAL || cm.vert_scale != Scaling.NORMAL) {
        } else {
            cpi.sourceYV12 = sd;
        }
    }

    /*
     * This function updates the reference frame probability estimates that will be
     * used during mode selection
     */
    static void update_rd_ref_frame_probs(Compressor cpi) {
        CommonData cm = cpi.common;

        ReferenceCounts rf = cpi.mb.sumReferenceCounts();

        if (cm.frame_type == FrameType.KEY_FRAME) {
            cpi.prob_intra_coded = 255;
            cpi.prob_last_coded = 128;
            cpi.prob_gf_coded = 128;
        } else if (rf.total == 0) {
            cpi.prob_intra_coded = 63;
            cpi.prob_last_coded = 128;
            cpi.prob_gf_coded = 128;
        }

        /*
         * update reference frame costs since we can do better than what we got last
         * frame.
         */
        if (cpi.oxcf.number_of_layers == 1) {
            if (cpi.common.refresh_alt_ref_frame) {
                cpi.prob_intra_coded += 40;
                if (cpi.prob_intra_coded > 255)
                    cpi.prob_intra_coded = 255;
                cpi.prob_last_coded = 200;
                cpi.prob_gf_coded = 1;
            } else if (cpi.frames_since_golden == 0) {
                cpi.prob_last_coded = 214;
            } else if (cpi.frames_since_golden == 1) {
                cpi.prob_last_coded = 192;
                cpi.prob_gf_coded = 220;
            } else if (cpi.source_alt_ref_active) {
                cpi.prob_gf_coded -= 20;

                if (cpi.prob_gf_coded < 10)
                    cpi.prob_gf_coded = 10;
            }
            if (!cpi.source_alt_ref_active)
                cpi.prob_gf_coded = 255;
        }
    }

    static boolean decide_key_frame(Compressor cpi) {
        CommonData cm = cpi.common;

        cpi.kf_boost = 0;

        if (cpi.Speed > 11)
            return false;

        if ((cpi.compressor_speed == 2) && (cpi.Speed >= 5) && (!cpi.sf.RD)) {
            double change = 1.0 * Math.abs((int) (cpi.mb.intra_error - cpi.last_intra_error))
                    / (1 + cpi.last_intra_error);
            double change2 = 1.0 * Math.abs((int) (cpi.mb.prediction_error - cpi.last_prediction_error))
                    / (1 + cpi.last_prediction_error);
            double minerror = cm.MBs * 256;

            cpi.last_intra_error = cpi.mb.intra_error;
            cpi.last_prediction_error = cpi.mb.prediction_error;

            if (10 * cpi.mb.intra_error / (1 + cpi.mb.prediction_error) < 15 && cpi.mb.prediction_error > minerror
                    && (change > .25 || change2 > .25)) {
                /*
                 * (change > 1.4 || change < .75)&& cpi.this_frame_percent_intra >
                 * cpi.last_frame_percent_intra + 3
                 */
                return true;
            }

            return false;
        }

        /* If the following are true we might as well code a key frame */
        if (((cpi.this_frame_percent_intra == 100)
                && (cpi.this_frame_percent_intra > (cpi.last_frame_percent_intra + 2)))
                || ((cpi.this_frame_percent_intra > 95)
                        && (cpi.this_frame_percent_intra >= (cpi.last_frame_percent_intra + 5)))) {
            return true;
        }
        /*
         * in addition if the following are true and this is not a golden frame then
         * code a key frame Note that on golden frames there often seems to be a pop in
         * intra useage anyway hence this restriction is designed to prevent spurious
         * key frames. The Intra pop needs to be investigated.
         */
        else if (((cpi.this_frame_percent_intra > 60)
                && (cpi.this_frame_percent_intra > (cpi.last_frame_percent_intra * 2)))
                || ((cpi.this_frame_percent_intra > 75)
                        && (cpi.this_frame_percent_intra > (cpi.last_frame_percent_intra * 3 / 2)))
                || ((cpi.this_frame_percent_intra > 90)
                        && (cpi.this_frame_percent_intra > (cpi.last_frame_percent_intra + 10)))) {
            if (!cm.refresh_golden_frame)
                return true;
        }

        return false;
    }

    static long vp8_calc_ss_err(FullAccessIntArrPointer src, int systride, FullAccessIntArrPointer dst, int dystride,
            int yheight, int ywidth) {
        int i, j;
        long Total = 0;
        /*
         * Loop through the Y plane raw and reconstruction data summing (square
         * differences)
         */
        VarianceResults sse = new VarianceResults();
        src = src.shallowCopy();
        dst = dst.shallowCopy();
        for (i = 0; i < yheight; i += 16) {
            for (j = 0; j < ywidth; j += 16) {
                src.incBy(j);
                dst.incBy(j);
                Variance.vpx_mse16x16.call(src, systride, dst, dystride, sse);
                Total += sse.sse;
                src.incBy(-j);
                dst.incBy(-j);
            }
            src.incBy(16 * systride);
            dst.incBy(16 * dystride);
        }
        return Total;
    }

    static long vp8_calc_ss_err(YV12buffer source, YV12buffer dest) {
        return vp8_calc_ss_err(source.y_buffer, source.y_stride, dest.y_buffer, dest.y_stride, source.y_height,
                source.y_width);
    }

    /*
     * Function to test for conditions that indeicate we should loop back and recode
     * a frame.
     */
    static boolean recode_loop_test(Compressor cpi, int high_limit, int low_limit, int q, int maxq, int minq) {
        boolean force_recode = false;
        CommonData cm = cpi.common;

        /*
         * Is frame recode allowed at all Yes if either recode mode 1 is selected or
         * mode two is selcted and the frame is a key frame. golden frame or
         * alt_ref_frame
         */
        if ((cpi.sf.recode_loop == 1) || ((cpi.sf.recode_loop == 2)
                && ((cm.frame_type == FrameType.KEY_FRAME) || cm.refresh_golden_frame || cm.refresh_alt_ref_frame))) {
            /* General over and under shoot tests */
            if (((cpi.projected_frame_size > high_limit) && (q < maxq))
                    || ((cpi.projected_frame_size < low_limit) && (q > minq))) {
                force_recode = true;
            }
            /* Special finalrained quality tests */
            else if (cpi.oxcf.end_usage == EndUsage.CONSTRAINED_QUALITY) {
                /* Undershoot and below auto cq level */
                if ((q > cpi.cq_target_quality) && (cpi.projected_frame_size < ((cpi.this_frame_target * 7) >> 3))) {
                    force_recode = true;
                }
                /* Severe undershoot and between auto and user cq level */
                else if ((q > cpi.oxcf.cq_level) && (cpi.projected_frame_size < cpi.min_frame_bandwidth)
                        && (cpi.active_best_quality > cpi.oxcf.cq_level)) {
                    force_recode = true;
                    cpi.active_best_quality = cpi.oxcf.cq_level;
                }
            }
        }

        return force_recode;
    }

    static int encode_frame_to_data_rate(Compressor cpi, FullAccessIntArrPointer dest, ReadOnlyIntArrPointer dest_end,
            EnumSet<FrameTypeFlags> frame_flags) {
        short Q;
        int size;
        FrameLimits limits = new FrameLimits();

        boolean Loop = false;

        CommonData cm = cpi.common;
        boolean active_worst_qchanged = false;

        short q_low;
        short q_high;
        int zbin_oq_high;
        int zbin_oq_low = 0;
        int top_index;
        int bottom_index;
        boolean overshoot_seen = false;
        boolean undershoot_seen = false;

        int drop_mark = (int) (cpi.oxcf.drop_frames_water_mark * cpi.oxcf.optimal_buffer_level / 100);
        int drop_mark75 = drop_mark * 2 / 3;
        int drop_mark50 = drop_mark / 4;
        int drop_mark25 = drop_mark / 8;

        if (cpi.force_next_frame_intra) {
            cm.frame_type = FrameType.KEY_FRAME; /* delayed intra frame */
            cpi.force_next_frame_intra = false;
        }

        cpi.per_frame_bandwidth = (int) (cpi.target_bandwidth / cpi.output_framerate);

        /* Default turn off buffer to buffer copying */
        cm.copy_buffer_to_gf = 0;
        cm.copy_buffer_to_arf = 0;

        /* Clear zbin over-quant value and mode boost values. */
        cpi.mb.zbin_over_quant = 0;
        cpi.mb.zbin_mode_boost = 0;

        /*
         * Enable or disable mode based tweaking of the zbin For 2 Pass Only used where
         * GF/ARF prediction quality is above a threshold
         */
        cpi.mb.zbin_mode_boost_enabled = true;

        /* Current default encoder behaviour for the altref sign bias */
        cpi.common.ref_frame_sign_bias.put(MVReferenceFrame.ALTREF_FRAME, cpi.source_alt_ref_active);

        /*
         * Check to see if a key frame is signaled
         */
        if ((cm.current_video_frame == 0) || (cm.frame_flags.contains(FrameTypeFlags.Key)
                || (cpi.oxcf.auto_key && (cpi.frames_since_key % cpi.key_frame_frequency == 0)))) {
            /* Key frame from VFW/auto-keyframe/first frame */
            cm.frame_type = FrameType.KEY_FRAME;
        }

        // Find the reference frame closest to the current frame.
        cpi.closest_reference_frame = MVReferenceFrame.LAST_FRAME;
        if (cm.frame_type != FrameType.KEY_FRAME) {
            MVReferenceFrame closest_ref = MVReferenceFrame.INTRA_FRAME;
            if (cpi.ref_frame_flags.contains(MVReferenceFrame.LAST_FRAME)) {
                closest_ref = MVReferenceFrame.LAST_FRAME;
            } else if (cpi.ref_frame_flags.contains(MVReferenceFrame.GOLDEN_FRAME)) {
                closest_ref = MVReferenceFrame.GOLDEN_FRAME;
            } else if (cpi.ref_frame_flags.contains(MVReferenceFrame.ALTREF_FRAME)) {
                closest_ref = MVReferenceFrame.ALTREF_FRAME;
            }
            for (MVReferenceFrame ref_frame_type : MVReferenceFrame.interFrames) {
                if (cpi.ref_frame_flags.contains(ref_frame_type)) {
                    if ((cm.current_video_frame - cpi.current_ref_frames.get(ref_frame_type)) < (cm.current_video_frame
                            - cpi.current_ref_frames.get(closest_ref))) {
                        closest_ref = ref_frame_type;
                    }
                }
            }
            cpi.closest_reference_frame = closest_ref;
        }

        /* Set various flags etc to special state if it is a key frame */
        if (cm.frame_type == FrameType.KEY_FRAME) {
            int i;

            // Set the loop filter deltas and segmentation map update
            cpi.mb.e_mbd.setup_features(cpi);

            /* The alternate reference frame cannot be active for a key frame */
            cpi.source_alt_ref_active = false;

            /* Reset the RD threshold multipliers to default of * 1 (128) */
            for (i = 0; i < Block.MAX_MODES; ++i) {
                cpi.mb.rd_thresh_mult[i] = 128;
            }

            // Reset the zero_last counter to 0 on key frame.
            Arrays.fill(cpi.consec_zero_last, 0, cm.mb_rows * cm.mb_cols, 0);
            Arrays.fill(cpi.consec_zero_last_mvbias, 0, cpi.common.mb_rows * cpi.common.mb_cols, 0);
        } else if (cpi.repeatFrameDetected) {
            cm.refresh_entropy_probs = false;
            cm.refresh_last_frame = false;
        }
        update_rd_ref_frame_probs(cpi);

        if (cpi.drop_frames_allowed) {
            /*
             * The reset to decimation 0 is only done here for one pass. Once it is set two
             * pass leaves decimation on till the next kf.
             */
            if ((cpi.buffer_level > drop_mark) && (cpi.decimation_factor > 0)) {
                cpi.decimation_factor--;
            }

            if (cpi.buffer_level > drop_mark75 && cpi.decimation_factor > 0) {
                cpi.decimation_factor = 1;

            } else if (cpi.buffer_level < drop_mark25 && (cpi.decimation_factor == 2 || cpi.decimation_factor == 3)) {
                cpi.decimation_factor = 3;
            } else if (cpi.buffer_level < drop_mark50 && (cpi.decimation_factor == 1 || cpi.decimation_factor == 2)) {
                cpi.decimation_factor = 2;
            } else if (cpi.buffer_level < drop_mark75 && (cpi.decimation_factor == 0 || cpi.decimation_factor == 1)) {
                cpi.decimation_factor = 1;
            }
        }

        /*
         * The following decimates the frame rate according to a regular pattern (i.e.
         * to 1/2 or 2/3 frame rate) This can be used to help prevent buffer under-run
         * in CBR mode. Alternatively it might be desirable in some situations to drop
         * frame rate but throw more bits at each frame.
         *
         * Note that dropping a key frame can be problematic if spatial resampling is
         * also active
         */
        if (cpi.decimation_factor > 0) {
            switch (cpi.decimation_factor) {
            case 1:
                cpi.per_frame_bandwidth = cpi.per_frame_bandwidth * 3 / 2;
                break;
            case 2:
                cpi.per_frame_bandwidth = cpi.per_frame_bandwidth * 5 / 4;
                break;
            case 3:
                cpi.per_frame_bandwidth = cpi.per_frame_bandwidth * 5 / 4;
                break;
            }

            /*
             * Note that we should not throw out a key frame (especially when spatial
             * resampling is enabled).
             */
            if (cm.frame_type == FrameType.KEY_FRAME) {
                cpi.decimation_count = cpi.decimation_factor;
            } else if (cpi.decimation_count > 0) {
                cpi.decimation_count--;

                cpi.bits_off_target += cpi.av_per_frame_bandwidth;
                if (cpi.bits_off_target > cpi.oxcf.maximum_buffer_size) {
                    cpi.bits_off_target = cpi.oxcf.maximum_buffer_size;
                }

                cm.current_video_frame++;
                cpi.frames_since_key++;
                cpi.ext_refresh_frame_flags_pending = false;
                // We advance the temporal pattern for dropped frames.
                cpi.temporal_pattern_counter++;

                cpi.buffer_level = cpi.bits_off_target;

                if (cpi.oxcf.number_of_layers > 1) {
                    int i;

                    /*
                     * Propagate bits saved by dropping the frame to higher layers
                     */
                    for (i = cpi.current_layer + 1; i < cpi.oxcf.number_of_layers; ++i) {
                        LayerContext lc = cpi.layer_context[i];
                        lc.bits_off_target += (int) (lc.target_bandwidth / lc.framerate);
                        if (lc.bits_off_target > lc.maximum_buffer_size) {
                            lc.bits_off_target = lc.maximum_buffer_size;
                        }
                        lc.buffer_level = lc.bits_off_target;
                    }
                }

                return 0;
            } else {
                cpi.decimation_count = cpi.decimation_factor;
            }
        } else {
            cpi.decimation_count = 0;
        }

        /* Decide how big to make the frame */
        if (!RateCtrl.vp8_pick_frame_size(cpi)) {
            /* TODO: 2 drop_frame and return code could be put together. */
            cm.current_video_frame++;
            cpi.frames_since_key++;
            cpi.ext_refresh_frame_flags_pending = false;
            // We advance the temporal pattern for dropped frames.
            cpi.temporal_pattern_counter++;
            return 0;
        }

        /*
         * Reduce active_worst_allowed_q for CBR if our buffer is getting too full. This
         * has a knock on effect on active best quality as well. For CBR if the buffer
         * reaches its maximum level then we can no longer save up bits for later frames
         * so we might as well use them up on the current frame.
         */
        if ((cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER) && (cpi.buffer_level >= cpi.oxcf.optimal_buffer_level)
                && cpi.buffered_mode) {
            /* Max adjustment is 1/4 */
            int Adjustment = cpi.active_worst_quality / 4;

            if (Adjustment != 0) {
                int buff_lvl_step;

                if (cpi.buffer_level < cpi.oxcf.maximum_buffer_size) {
                    buff_lvl_step = (int) ((cpi.oxcf.maximum_buffer_size - cpi.oxcf.optimal_buffer_level) / Adjustment);

                    if (buff_lvl_step != 0) {
                        Adjustment = (int) ((cpi.buffer_level - cpi.oxcf.optimal_buffer_level) / buff_lvl_step);
                    } else {
                        Adjustment = 0;
                    }
                }

                cpi.active_worst_quality -= Adjustment;

                if (cpi.active_worst_quality < cpi.active_best_quality) {
                    cpi.active_worst_quality = cpi.active_best_quality;
                }
            }
        }

        /*
         * Set an active best quality and if necessary active worst quality There is
         * some odd behavior for one pass here that needs attention.
         */
        if ((cpi.ni_frames > 150)) {

            Q = cpi.active_worst_quality;

            if (cm.frame_type == FrameType.KEY_FRAME) {
                cpi.active_best_quality = kf_high_motion_minq[Q];
            }

            else if (cpi.oxcf.number_of_layers == 1 && (cm.refresh_golden_frame || cpi.common.refresh_alt_ref_frame)) {
                /*
                 * Use the lower of cpi.active_worst_quality and recent average Q as basis for
                 * GF/ARF Q limit unless last frame was a key frame.
                 */
                if ((cpi.frames_since_key > 1) && (cpi.avg_frame_qindex < cpi.active_worst_quality)) {
                    Q = cpi.avg_frame_qindex;
                }

                /* For finalrained quality dont allow Q less than the cq level */
                if ((cpi.oxcf.end_usage == EndUsage.CONSTRAINED_QUALITY) && (Q < cpi.cq_target_quality)) {
                    Q = cpi.cq_target_quality;
                }

                cpi.active_best_quality = gf_high_motion_minq[Q];
            } else {
                cpi.active_best_quality = inter_minq[Q];

                /*
                 * For the finalant/finalrained quality mode we dont want q to fall below the cq
                 * level.
                 */
                if ((cpi.oxcf.end_usage == EndUsage.CONSTRAINED_QUALITY)
                        && (cpi.active_best_quality < cpi.cq_target_quality)) {
                    /*
                     * If we are strongly undershooting the target rate in the last frames then use
                     * the user passed in cq value not the auto cq value.
                     */
                    if (cpi.rolling_actual_bits < cpi.min_frame_bandwidth) {
                        cpi.active_best_quality = cpi.oxcf.cq_level;
                    } else {
                        cpi.active_best_quality = cpi.cq_target_quality;
                    }
                }
            }

            /*
             * If CBR and the buffer is as full then it is reasonable to allow higher
             * quality on the frames to prevent bits just going to waste.
             */
            if (cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER) {
                /*
                 * Note that the use of >= here elliminates the risk of a devide by 0 error in
                 * the else if clause
                 */
                if (cpi.buffer_level >= cpi.oxcf.maximum_buffer_size) {
                    cpi.active_best_quality = cpi.best_quality;

                } else if (cpi.buffer_level > cpi.oxcf.optimal_buffer_level) {
                    int Fraction = (int) (((cpi.buffer_level - cpi.oxcf.optimal_buffer_level) * 128)
                            / (cpi.oxcf.maximum_buffer_size - cpi.oxcf.optimal_buffer_level));
                    int min_qadjustment = ((cpi.active_best_quality - cpi.best_quality) * Fraction) / 128;

                    cpi.active_best_quality -= min_qadjustment;
                }
            }
        }
        /*
         * Make sure finalrained quality mode limits are adhered to for the first few
         * frames of one pass encodes
         */
        else if (cpi.oxcf.end_usage == EndUsage.CONSTRAINED_QUALITY) {
            if ((cm.frame_type == FrameType.KEY_FRAME) || cm.refresh_golden_frame || cpi.common.refresh_alt_ref_frame) {
                cpi.active_best_quality = cpi.best_quality;
            } else if (cpi.active_best_quality < cpi.cq_target_quality) {
                cpi.active_best_quality = cpi.cq_target_quality;
            }
        }

        /* Clip the active best and worst quality values to limits */
        if (cpi.active_worst_quality > cpi.worst_quality) {
            cpi.active_worst_quality = cpi.worst_quality;
        }

        if (cpi.active_best_quality < cpi.best_quality) {
            cpi.active_best_quality = cpi.best_quality;
        }

        if (cpi.active_worst_quality < cpi.active_best_quality) {
            cpi.active_worst_quality = cpi.active_best_quality;
        }

        /* Determine initial Q to try */
        Q = RateCtrl.vp8_regulate_q(cpi, cpi.this_frame_target);

        /* Set highest allowed value for Zbin over quant */
        if (cm.frame_type == FrameType.KEY_FRAME) {
            zbin_oq_high = 0;
        } else if ((cpi.oxcf.number_of_layers == 1)
                && ((cm.refresh_alt_ref_frame || (cm.refresh_golden_frame && !cpi.source_alt_ref_active)))) {
            zbin_oq_high = 16;
        } else {
            zbin_oq_high = OnyxInt.ZBIN_OQ_MAX;
        }

        if (!cpi.repeatFrameDetected) {
            compute_skin_map(cpi);
        }

        /*
         * Setup background Q adjustment for error resilient mode. For multi-layer
         * encodes only enable this for the base layer.
         */
        if (cpi.cyclic_refresh_mode_enabled) {
            // Special case for screen_content_mode with golden frame updates.
            boolean disable_cr_gf = (cpi.oxcf.screen_content_mode == 2 && cm.refresh_golden_frame);
            if (cpi.current_layer == 0 && cpi.force_maxqp == 0 && !disable_cr_gf) {
                cyclic_background_refresh(cpi, Q, (short) 0);
            } else {
                disable_segmentation(cpi);
            }
        }

        RateCtrl.vp8_compute_frame_size_bounds(cpi, limits);

        /* Limit Q range for the adaptive loop. */
        bottom_index = cpi.active_best_quality;
        top_index = cpi.active_worst_quality;
        q_low = cpi.active_best_quality;
        q_high = cpi.active_worst_quality;

        cpi.coding_context.vp8_save_coding_context(cpi);

        scale_and_extend_source(cpi.un_scaled_source, cpi);

        do {

            Quantize.vp8_set_quantizer(cpi, Q);

            /* setup skip prob for costing in mode/mv decision */
            if (cpi.common.mb_no_coeff_skip) {
                cpi.prob_skip_false = cpi.base_skip_false_prob[Q];

                if (cm.frame_type != FrameType.KEY_FRAME) {
                    if (cpi.common.refresh_alt_ref_frame) {
                        if (cpi.last_skip_false_probs[2] != 0) {
                            cpi.prob_skip_false = cpi.last_skip_false_probs[2];
                        }

                    } else if (cpi.common.refresh_golden_frame) {
                        if (cpi.last_skip_false_probs[1] != 0) {
                            cpi.prob_skip_false = cpi.last_skip_false_probs[1];
                        }

                    } else {
                        if (cpi.last_skip_false_probs[0] != 0) {
                            cpi.prob_skip_false = cpi.last_skip_false_probs[0];
                        }

                    }

                    /*
                     * as this is for cost estimate, let's make sure it does not go extreme eitehr
                     * way
                     */
                    if (cpi.prob_skip_false < 5)
                        cpi.prob_skip_false = 5;

                    if (cpi.prob_skip_false > 250)
                        cpi.prob_skip_false = 250;

                    if (cpi.oxcf.number_of_layers == 1 && cpi.is_src_frame_alt_ref) {
                        cpi.prob_skip_false = 1;
                    }
                }
            }

            if (cm.frame_type == FrameType.KEY_FRAME) {

                RateCtrl.vp8_setup_key_frame(cpi);
            }

            /* transform / motion compensation build reconstruction frame */
            EncodeFrame.vp8_encode_frame(cpi);

            if (cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER) {
                if (RateCtrl.vp8_drop_encodedframe_overshoot(cpi, Q)) {
                    return 0;
                }
                if (cm.frame_type != FrameType.KEY_FRAME)
                    cpi.last_pred_err_mb = (int) (cpi.mb.prediction_error / cpi.common.MBs);
            }

            cpi.projected_frame_size -= BitStream.vp8_estimate_entropy_savings(cpi);
            cpi.projected_frame_size = (cpi.projected_frame_size > 0) ? cpi.projected_frame_size : 0;

            /*
             * Test to see if the stats generated for this frame indicate that we should
             * have coded a key frame (assuming that we didn't)!
             */

            if (cpi.oxcf.auto_key && cm.frame_type != FrameType.KEY_FRAME && cpi.compressor_speed != 2) {
                if (decide_key_frame(cpi)) {
                    /* Reset all our sizing numbers and recode */
                    cm.frame_type = FrameType.KEY_FRAME;

                    RateCtrl.vp8_pick_frame_size(cpi);

                    /*
                     * Clear the Alt reference frame active flag when we have a key frame
                     */
                    cpi.source_alt_ref_active = false;

                    // Set the loop filter deltas and segmentation map update
                    cpi.mb.e_mbd.setup_features(cpi);

                    cpi.coding_context.vp8_restore_coding_context(cpi);

                    Q = RateCtrl.vp8_regulate_q(cpi, cpi.this_frame_target);

                    RateCtrl.vp8_compute_frame_size_bounds(cpi, limits);

                    /* Limit Q range for the adaptive loop. */
                    bottom_index = cpi.active_best_quality;
                    top_index = cpi.active_worst_quality;
                    q_low = cpi.active_best_quality;
                    q_high = cpi.active_worst_quality;

                    Loop = true;

                    continue;
                }
            }

            if (limits.frame_over_shoot_limit == 0)
                limits.frame_over_shoot_limit = 1;

            /* Are we are overshooting and up against the limit of active max Q. */
            if (((cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER)) && (Q == cpi.active_worst_quality)
                    && (cpi.active_worst_quality < cpi.worst_quality)
                    && (cpi.projected_frame_size > limits.frame_over_shoot_limit)) {
                int over_size_percent = ((cpi.projected_frame_size - limits.frame_over_shoot_limit) * 100)
                        / limits.frame_over_shoot_limit;

                /* If so is there any scope for relaxing it */
                while ((cpi.active_worst_quality < cpi.worst_quality) && (over_size_percent > 0)) {
                    cpi.active_worst_quality++;
                    /* Assume 1 qstep = about 4% on frame size. */
                    over_size_percent = (int) (over_size_percent * 0.96);
                }
                top_index = cpi.active_worst_quality;
                /*
                 * If we have updated the active max Q do not call
                 * vp8_update_rate_correction_factors() this loop.
                 */
                active_worst_qchanged = true;
            } else {
                active_worst_qchanged = false;
            }

            /* Special case handling for forced key frames */
            if ((cm.frame_type == FrameType.KEY_FRAME) && cpi.this_key_frame_forced) {
                int last_q = Q;
                long kf_err = vp8_calc_ss_err(cpi.sourceYV12, cm.yv12_fb[cm.new_fb_idx]);

                /* The key frame is not good enough */
                if (kf_err > ((cpi.ambient_err * 7) >> 3)) {
                    /* Lower q_high */
                    q_high = (short) ((Q > q_low) ? (Q - 1) : q_low);

                    /* Adjust Q */
                    Q = (short) ((q_high + q_low) >> 1);
                }
                /* The key frame is much better than the previous frame */
                else if (kf_err < (cpi.ambient_err >> 1)) {
                    /* Raise q_low */
                    q_low = (short) ((Q < q_high) ? (Q + 1) : q_high);

                    /* Adjust Q */
                    Q = (short) ((q_high + q_low + 1) >> 1);
                }

                /* Clamp Q to upper and lower limits: */
                if (Q > q_high) {
                    Q = q_high;
                } else if (Q < q_low) {
                    Q = q_low;
                }

                Loop = Q != last_q;
            }

            /*
             * Is the projected frame size out of range and are we allowed to attempt to
             * recode.
             */
            else if (recode_loop_test(cpi, limits.frame_over_shoot_limit, limits.frame_under_shoot_limit, Q, top_index,
                    bottom_index)) {
                int last_q = Q;
                int Retries = 0;

                /*
                 * Frame size out of permitted range. Update correction factor & compute new Q
                 * to try...
                 */

                /* Frame is too large */
                if (cpi.projected_frame_size > cpi.this_frame_target) {
                    /* Raise Qlow as to at least the current value */
                    q_low = (short) ((Q < q_high) ? (Q + 1) : q_high);

                    /* If we are using over quant do the same for zbin_oq_low */
                    if (cpi.mb.zbin_over_quant > 0) {
                        zbin_oq_low = (cpi.mb.zbin_over_quant < zbin_oq_high) ? (cpi.mb.zbin_over_quant + 1)
                                : zbin_oq_high;
                    }

                    if (undershoot_seen) {
                        /*
                         * Update rate_correction_factor unless cpi.active_worst_quality has changed.
                         */
                        if (!active_worst_qchanged) {
                            RateCtrl.vp8_update_rate_correction_factors(cpi, 1);
                        }

                        Q = (short) ((q_high + q_low + 1) >> 1);

                        /*
                         * Adjust cpi.zbin_over_quant (only allowed when Q is max)
                         */
                        if (Q < OnyxInt.MAXQ) {
                            cpi.mb.zbin_over_quant = 0;
                        } else {
                            zbin_oq_low = (cpi.mb.zbin_over_quant < zbin_oq_high) ? (cpi.mb.zbin_over_quant + 1)
                                    : zbin_oq_high;
                            cpi.mb.zbin_over_quant = (zbin_oq_high + zbin_oq_low) / 2;
                        }
                    } else {
                        /*
                         * Update rate_correction_factor unless cpi.active_worst_quality has changed.
                         */
                        if (!active_worst_qchanged) {
                            RateCtrl.vp8_update_rate_correction_factors(cpi, 0);
                        }

                        Q = RateCtrl.vp8_regulate_q(cpi, cpi.this_frame_target);

                        while (((Q < q_low) || (cpi.mb.zbin_over_quant < zbin_oq_low)) && (Retries < 10)) {
                            RateCtrl.vp8_update_rate_correction_factors(cpi, 0);
                            Q = RateCtrl.vp8_regulate_q(cpi, cpi.this_frame_target);
                            Retries++;
                        }
                    }

                    overshoot_seen = true;
                }
                /* Frame is too small */
                else {
                    if (cpi.mb.zbin_over_quant == 0) {
                        /* Lower q_high if not using over quant */
                        q_high = (short) ((Q > q_low) ? (Q - 1) : q_low);
                    } else {
                        /* else lower zbin_oq_high */
                        zbin_oq_high = (cpi.mb.zbin_over_quant > zbin_oq_low) ? (cpi.mb.zbin_over_quant - 1)
                                : zbin_oq_low;
                    }

                    if (overshoot_seen) {
                        /*
                         * Update rate_correction_factor unless cpi.active_worst_quality has changed.
                         */
                        if (!active_worst_qchanged) {
                            RateCtrl.vp8_update_rate_correction_factors(cpi, 1);
                        }

                        Q = (short) ((q_high + q_low) >> 1);

                        /*
                         * Adjust cpi.zbin_over_quant (only allowed when Q is max)
                         */
                        if (Q < OnyxInt.MAXQ) {
                            cpi.mb.zbin_over_quant = 0;
                        } else {
                            cpi.mb.zbin_over_quant = (zbin_oq_high + zbin_oq_low) / 2;
                        }
                    } else {
                        /*
                         * Update rate_correction_factor unless cpi.active_worst_quality has changed.
                         */
                        if (!active_worst_qchanged) {
                            RateCtrl.vp8_update_rate_correction_factors(cpi, 0);
                        }

                        Q = RateCtrl.vp8_regulate_q(cpi, cpi.this_frame_target);

                        /*
                         * Special case reset for qlow for finalrained quality. This should only trigger
                         * where there is very substantial undershoot on a frame and the auto cq level
                         * is above the user passsed in value.
                         */
                        if ((cpi.oxcf.end_usage == EndUsage.CONSTRAINED_QUALITY) && (Q < q_low)) {
                            q_low = Q;
                        }

                        while (((Q > q_high) || (cpi.mb.zbin_over_quant > zbin_oq_high)) && (Retries < 10)) {
                            RateCtrl.vp8_update_rate_correction_factors(cpi, 0);
                            Q = RateCtrl.vp8_regulate_q(cpi, cpi.this_frame_target);
                            Retries++;
                        }
                    }

                    undershoot_seen = true;
                }

                /* Clamp Q to upper and lower limits: */
                if (Q > q_high) {
                    Q = q_high;
                } else if (Q < q_low) {
                    Q = q_low;
                }

                /* Clamp cpi.zbin_over_quant */
                cpi.mb.zbin_over_quant = (cpi.mb.zbin_over_quant < zbin_oq_low) ? zbin_oq_low
                        : (cpi.mb.zbin_over_quant > zbin_oq_high) ? zbin_oq_high : cpi.mb.zbin_over_quant;

                Loop = Q != last_q;
            } else {
                Loop = false;
            }

            if (cpi.is_src_frame_alt_ref)
                Loop = false;

            if (Loop) {
                cpi.coding_context.vp8_restore_coding_context(cpi);
            }
        } while (Loop);

        /*
         * This frame's MVs are saved and will be used in next frame's MV predictor.
         * Last frame has one more line(add to bottom) and one more column(add to right)
         * than cm.mip. The edge elements are initialized to 0.
         */
        if (cm.show_frame) /* do not save for altref frame */
        {

            if (cm.frame_type != FrameType.KEY_FRAME) {
                /* Point to beginning of allocated MODE_INFO arrays. */
                FullAccessGenArrPointer<ModeInfo> tmp = cm.mip.shallowCopy();
                for (int mb_row = 0; mb_row < cm.mb_rows + 1; ++mb_row) {
                    for (int mb_col = 0; mb_col < cm.mb_cols + 1; ++mb_col) {
                        final MBModeInfo mbmi = tmp.get().mbmi;
                        if (mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
                            cpi.lfmv[mb_col + mb_row * (cm.mode_info_stride + 1)].set(mbmi.mv);
                        }

                        cpi.lf_ref_frame_sign_bias[mb_col + mb_row * (cm.mode_info_stride + 1)] = cm.ref_frame_sign_bias
                                .get(mbmi.ref_frame);
                        cpi.lf_ref_frame[mb_col + mb_row * (cm.mode_info_stride + 1)] = mbmi.ref_frame;
                        tmp.inc();
                    }
                }
            }
        }

        /* Count last ref frame 0,0 usage on current encoded frame. */
        {
            /* Point to beginning of MODE_INFO arrays. */

            cpi.zeromv_count = 0;

            if (cm.frame_type != FrameType.KEY_FRAME) {
                FullAccessGenArrPointer<ModeInfo> tmp = cm.mi.shallowCopy();
                for (int mb_row = 0; mb_row < cm.mb_rows; ++mb_row) {
                    for (int mb_col = 0; mb_col < cm.mb_cols; ++mb_col) {
                        MBModeInfo mbmi = tmp.get().mbmi;
                        if (mbmi.mode == MBPredictionMode.ZEROMV && mbmi.ref_frame == MVReferenceFrame.LAST_FRAME) {
                            cpi.zeromv_count++;
                        }
                        tmp.inc();
                    }
                    tmp.inc();
                }
            }
        }

        /*
         * Update the GF useage maps. This is done after completing the compression of a
         * frame when all modes etc. are finalized but before loop filter
         */
        if (cpi.oxcf.number_of_layers == 1) {
            Segmentation.vp8_update_gf_useage_maps(cpi, cm, cpi.mb);
        }

        if (cm.frame_type == FrameType.KEY_FRAME)
            cm.refresh_last_frame = true;

        /*
         * For inter frames the current default behavior is that when
         * cm.refresh_golden_frame is set we copy the old GF over to the ARF buffer This
         * is purely an encoder decision at present. Avoid this behavior when refresh
         * flags are set by the user.
         */
        if (!cpi.oxcf.error_resilient_mode && cm.refresh_golden_frame && !cpi.ext_refresh_frame_flags_pending) {
            cm.copy_buffer_to_arf = 2;
        } else {
            cm.copy_buffer_to_arf = 0;
        }

        cm.frame_to_show = cm.yv12_fb[cm.new_fb_idx];

        vp8_loopfilter_frame(cpi, cm);

        update_reference_frames(cpi);

        if (cpi.oxcf.error_resilient_mode) {
            cm.refresh_entropy_probs = false;
        }

        /* build the bitstream */
        size = BitStream.vp8_pack_bitstream(cpi, dest, dest_end);

        /*
         * Move storing frame_type out of the above loop since it is also needed in
         * motion search besides loopfilter
         */
        cm.last_frame_type = cm.frame_type;

        /* Update rate control heuristics */
        cpi.total_byte_count += size;
        cpi.projected_frame_size = (int) (size << 3);

        if (cpi.oxcf.number_of_layers > 1) {
            int i;
            for (i = cpi.current_layer + 1; i < cpi.oxcf.number_of_layers; ++i) {
                cpi.layer_context[i].total_byte_count += size;
            }
        }

        if (!active_worst_qchanged)
            RateCtrl.vp8_update_rate_correction_factors(cpi, 2);

        cpi.last_q[cm.frame_type.ordinal()] = cm.base_qindex;

        if (cm.frame_type == FrameType.KEY_FRAME) {
            RateCtrl.vp8_adjust_key_frame_context(cpi);
        }

        /* Keep a record of ambient average Q. */
        if (cm.frame_type != FrameType.KEY_FRAME) {
            cpi.avg_frame_qindex = (short) ((2 + 3 * cpi.avg_frame_qindex + cm.base_qindex) >> 2);
        }

        /*
         * Keep a record from which we can calculate the average Q excluding GF updates
         * and key frames
         */
        if ((cm.frame_type != FrameType.KEY_FRAME)
                && ((cpi.oxcf.number_of_layers > 1) || (!cm.refresh_golden_frame && !cm.refresh_alt_ref_frame))) {
            cpi.ni_frames++;

            /*
             * Calculate the average Q for normal inter frames (not key or GFU frames).
             */
            /* Damp value for first few frames */
            if (cpi.ni_frames > 150) {
                cpi.ni_tot_qi += Q;
                cpi.ni_av_qi = (short) (cpi.ni_tot_qi / cpi.ni_frames);
            }
            /*
             * For one pass, early in the clip ... average the current frame Q value with
             * the worstq entered by the user as a dampening measure
             */
            else {
                cpi.ni_tot_qi += Q;
                cpi.ni_av_qi = (short) (((cpi.ni_tot_qi / cpi.ni_frames) + cpi.worst_quality + 1) >> 1);
            }

            /*
             * If the average Q is higher than what was used in the last frame (after going
             * through the recode loop to keep the frame size within range) then use the
             * last frame value - 1. The -1 is designed to stop Q and hence the data rate,
             * from progressively falling away during difficult sections, but at the same
             * time reduce the number of itterations around the recode loop.
             */
            if (Q > cpi.ni_av_qi)
                cpi.ni_av_qi = (short) (Q - 1);

        }

        /* Update the buffer level variable. */
        /* Non-viewable frames are a special case and are treated as pure overhead. */
        if (!cm.show_frame) {
            cpi.bits_off_target -= cpi.projected_frame_size;
        } else {
            cpi.bits_off_target += cpi.av_per_frame_bandwidth - cpi.projected_frame_size;
        }

        /* Clip the buffer level to the maximum specified buffer size */
        if (cpi.bits_off_target > cpi.oxcf.maximum_buffer_size) {
            cpi.bits_off_target = cpi.oxcf.maximum_buffer_size;
        }

        // If the frame dropper is not enabled, don't let the buffer level go below
        // some threshold, given here by -|maximum_buffer_size|. For now we only do
        // this for screen content input.
        if (!cpi.drop_frames_allowed && cpi.oxcf.screen_content_mode != 0
                && cpi.bits_off_target < -cpi.oxcf.maximum_buffer_size) {
            cpi.bits_off_target = -cpi.oxcf.maximum_buffer_size;
        }

        /*
         * Rolling monitors of whether we are over or underspending used to help
         * regulate min and Max Q in two pass.
         */
        cpi.rolling_target_bits = (int) CommonUtils
                .roundPowerOfTwo((long) cpi.rolling_target_bits * 3 + cpi.this_frame_target, 2);
        cpi.rolling_actual_bits = (int) CommonUtils
                .roundPowerOfTwo((long) cpi.rolling_actual_bits * 3 + cpi.projected_frame_size, 2);
        cpi.long_rolling_target_bits = (int) CommonUtils
                .roundPowerOfTwo((long) cpi.long_rolling_target_bits * 31 + cpi.this_frame_target, 5);
        cpi.long_rolling_actual_bits = (int) CommonUtils
                .roundPowerOfTwo((long) cpi.long_rolling_actual_bits * 31 + cpi.projected_frame_size, 5);

        /* Actual bits spent */
        cpi.total_actual_bits += cpi.projected_frame_size;

        cpi.buffer_level = cpi.bits_off_target;

        /* Propagate values to higher temporal layers */
        if (cpi.oxcf.number_of_layers > 1) {
            int i;

            for (i = cpi.current_layer + 1; i < cpi.oxcf.number_of_layers; ++i) {
                LayerContext lc = cpi.layer_context[i];
                int bits_off_for_this_layer = (int) (lc.target_bandwidth / lc.framerate - cpi.projected_frame_size);

                lc.bits_off_target += bits_off_for_this_layer;

                /* Clip buffer level to maximum buffer size for the layer */
                if (lc.bits_off_target > lc.maximum_buffer_size) {
                    lc.bits_off_target = lc.maximum_buffer_size;
                }

                lc.total_actual_bits += cpi.projected_frame_size;
                lc.total_target_vs_actual += bits_off_for_this_layer;
                lc.buffer_level = lc.bits_off_target;
            }
        }

        if (cm.frame_type != FrameType.KEY_FRAME) {
            if (cpi.common.refresh_alt_ref_frame) {
                cpi.last_skip_false_probs[2] = cpi.prob_skip_false;
                cpi.last_skip_probs_q[2] = cm.base_qindex;
            } else if (cpi.common.refresh_golden_frame) {
                cpi.last_skip_false_probs[1] = cpi.prob_skip_false;
                cpi.last_skip_probs_q[1] = cm.base_qindex;
            } else {
                cpi.last_skip_false_probs[0] = cpi.prob_skip_false;
                cpi.last_skip_probs_q[0] = cm.base_qindex;

                /* update the baseline */
                cpi.base_skip_false_prob[cm.base_qindex] = cpi.prob_skip_false;
            }
        }

        cpi.ext_refresh_frame_flags_pending = false;

        if (cm.refresh_golden_frame) {
            cm.frame_flags.add(FrameTypeFlags.Golden);
        } else {
            cm.frame_flags.remove(FrameTypeFlags.Golden);
        }

        if (cm.refresh_alt_ref_frame) {
            cm.frame_flags.add(FrameTypeFlags.AltRef);
        } else {
            cm.frame_flags.remove(FrameTypeFlags.AltRef);
        }

        if (cm.refresh_last_frame & cm.refresh_golden_frame) { /* both refreshed */
            cpi.gold_is_last = true;
        } else if (cm.refresh_last_frame ^ cm.refresh_golden_frame) {
            /* 1 refreshed but not the other */
            cpi.gold_is_last = false;
        }

        if (cm.refresh_last_frame & cm.refresh_alt_ref_frame) { /* both refreshed */
            cpi.alt_is_last = true;
        } else if (cm.refresh_last_frame ^ cm.refresh_alt_ref_frame) {
            /* 1 refreshed but not the other */
            cpi.alt_is_last = false;
        }

        if (cm.refresh_alt_ref_frame & cm.refresh_golden_frame) { /* both refreshed */
            cpi.gold_is_alt = true;
        } else if (cm.refresh_alt_ref_frame ^ cm.refresh_golden_frame) {
            /* 1 refreshed but not the other */
            cpi.gold_is_alt = false;
        }

        cpi.ref_frame_flags = EnumSet.copyOf(MVReferenceFrame.interFrames);

        if (cpi.gold_is_last)
            cpi.ref_frame_flags.remove(MVReferenceFrame.GOLDEN_FRAME);

        if (cpi.alt_is_last)
            cpi.ref_frame_flags.remove(MVReferenceFrame.ALTREF_FRAME);

        if (cpi.gold_is_alt)
            cpi.ref_frame_flags.remove(MVReferenceFrame.ALTREF_FRAME);

        if (!cpi.oxcf.error_resilient_mode) {
            if (cpi.oxcf.play_alternate && cm.refresh_alt_ref_frame && (cm.frame_type != FrameType.KEY_FRAME)) {
                /* Update the alternate reference frame stats as appropriate. */
                update_alt_ref_frame_stats(cpi);
            } else {
                /* Update the Golden frame stats as appropriate. */
                update_golden_frame_stats(cpi);
            }
        }

        if (cm.frame_type == FrameType.KEY_FRAME) {
            /* Tell the caller that the frame was coded as a key frame */
            frame_flags.clear();
            frame_flags.addAll(cm.frame_flags);
            frame_flags.add(FrameTypeFlags.Key);

            /* As this frame is a key frame the next defaults to an inter frame. */
            cm.frame_type = FrameType.INTER_FRAME;

            cpi.last_frame_percent_intra = 100;
        } else {
            frame_flags.clear();
            frame_flags.addAll(cm.frame_flags);
            frame_flags.remove(FrameTypeFlags.Key);

            cpi.last_frame_percent_intra = cpi.this_frame_percent_intra;
        }

        /*
         * Clear the one shot update flags for segmentation map and mode/ref loop filter
         * deltas.
         */
        cpi.mb.e_mbd.update_mb_segmentation_map = false;
        cpi.mb.e_mbd.update_mb_segmentation_data = false;
        cpi.mb.e_mbd.mode_ref_lf_delta_update = false;

        /*
         * Dont increment frame counters if this was an altref buffer update not a real
         * frame
         */
        if (cm.show_frame) {
            cm.current_video_frame++;
            cpi.frames_since_key++;
            cpi.temporal_pattern_counter++;
        }
        return size;
    }

    static class TimeStampRange {
        long time_stamp;
        long time_end;
    }

    static int vp8_get_compressed_data(Compressor cpi, EnumSet<FrameTypeFlags> frame_flags,
            FullAccessIntArrPointer dest, ReadOnlyIntArrPointer dest_end, TimeStampRange time, boolean flush) {
        CommonData cm;
        UsecTimer tsctimer = new UsecTimer();
        UsecTimer ticktimer = new UsecTimer();
        UsecTimer cmptimer = new UsecTimer();
        YV12buffer force_src_buffer = null;
        int size;

        if (cpi == null) {
            return -1;
        }

        cm = cpi.common;

        cmptimer.timerStart();

        cpi.sourceLAE = null;

        /* Should we code an alternate reference frame */
        if (!cpi.oxcf.error_resilient_mode && cpi.oxcf.play_alternate && cpi.source_alt_ref_pending) {
            if ((cpi.sourceLAE = cpi.lookahead.vp8_lookahead_peek(cpi.frames_till_gf_update_due,
                    Lookahead.PEEK_FORWARD)) != null) {
                cpi.alt_ref_source = cpi.sourceLAE;
                if (cpi.oxcf.arnr_max_frames > 0) {
                    TemporalFilter.vp8_temporal_filter_prepare(cpi, cpi.frames_till_gf_update_due);
                    force_src_buffer = cpi.alt_ref_buffer;
                }
                cpi.frames_till_alt_ref_frame = cpi.frames_till_gf_update_due;
                cm.refresh_alt_ref_frame = true;
                cm.refresh_golden_frame = false;
                cm.refresh_last_frame = false;
                cm.show_frame = false;
                /* Clear Pending alt Ref flag. */
                cpi.source_alt_ref_pending = false;
                cpi.is_src_frame_alt_ref = false;
            }
        }
        if (cpi.sourceLAE == null) {
            /* Read last frame source if we are encoding first pass. */
            if ((cpi.sourceLAE = cpi.lookahead.vp8_lookahead_pop(flush)) != null) {
                cm.show_frame = true;

                cpi.is_src_frame_alt_ref = cpi.alt_ref_source != null && (cpi.sourceLAE == cpi.alt_ref_source);

                if (cpi.is_src_frame_alt_ref)
                    cpi.alt_ref_source = null;
            }
        }

        if (cpi.sourceLAE != null) {
            cpi.sourceYV12 = force_src_buffer != null ? force_src_buffer : cpi.sourceLAE.img;
            cpi.un_scaled_source = cpi.sourceYV12;
            time.time_stamp = cpi.sourceLAE.ts_start;
            time.time_end = cpi.sourceLAE.ts_end;
            frame_flags.addAll(cpi.sourceLAE.flags);

        } else {
            return -1;
        }

        if (cpi.sourceLAE.ts_start < cpi.first_time_stamp_ever) {
            cpi.first_time_stamp_ever = cpi.sourceLAE.ts_start;
            cpi.last_end_time_stamp_seen = cpi.sourceLAE.ts_start;
        }

        /* adjust frame rates based on timestamps given */
        if (cm.show_frame) {
            long this_duration;
            int step = 0;

            if (cpi.sourceLAE.ts_start == cpi.first_time_stamp_ever) {
                this_duration = cpi.sourceLAE.ts_end - cpi.sourceLAE.ts_start;
                step = 1;
            } else {
                long last_duration;

                this_duration = cpi.sourceLAE.ts_end - cpi.last_end_time_stamp_seen;
                last_duration = cpi.last_end_time_stamp_seen - cpi.last_time_stamp_seen;
                /* do a step update if the duration changes by 10% */
                if (last_duration != 0) {
                    step = (int) (((this_duration - last_duration) * 10 / last_duration));
                }
            }

            if (this_duration != 0) {
                if (step != 0) {
                    cpi.ref_framerate = 10000000.0 / this_duration;
                } else {
                    double avg_duration, interval;

                    /*
                     * Average this frame's rate into the last second's average frame rate. If we
                     * haven't seen 1 second yet, then average over the whole interval seen.
                     */
                    interval = (double) (cpi.sourceLAE.ts_end - cpi.first_time_stamp_ever);
                    if (interval > 10000000.0)
                        interval = 10000000;

                    avg_duration = 10000000.0 / cpi.ref_framerate;
                    avg_duration *= (interval - avg_duration + this_duration);
                    avg_duration /= interval;

                    cpi.ref_framerate = 10000000.0 / avg_duration;
                }
                if (cpi.oxcf.number_of_layers > 1) {
                    int i;

                    /* Update frame rates for each layer */
                    assert (cpi.oxcf.number_of_layers <= OnyxIf.VPX_TS_MAX_LAYERS);
                    for (i = 0; i < cpi.oxcf.number_of_layers && i < OnyxIf.VPX_TS_MAX_LAYERS; ++i) {
                        LayerContext lc = cpi.layer_context[i];
                        lc.framerate = cpi.ref_framerate / cpi.oxcf.rate_decimator[i];
                    }
                } else {
                    cpi.vp8_new_framerate(cpi.ref_framerate);
                }
            }

            cpi.last_time_stamp_seen = cpi.sourceLAE.ts_start;
            cpi.last_end_time_stamp_seen = cpi.sourceLAE.ts_end;
        }

        if (cpi.oxcf.number_of_layers > 1)

        {
            int layer;

            update_layer_contexts(cpi);

            /* Restore layer specific context & set frame rate */
            if (cpi.temporal_layer_id >= 0) {
                layer = cpi.temporal_layer_id;
            } else {
                layer = cpi.oxcf.layer_id[cpi.temporal_pattern_counter % cpi.oxcf.periodicity];
            }
            restore_layer_context(cpi, layer);
            cpi.vp8_new_framerate(cpi.layer_context[layer].framerate);
        }

        if (cpi.compressor_speed == 2) {
            tsctimer.timerStart();
            ticktimer.timerStart();
        }

        cpi.lf_zeromv_pct = (cpi.zeromv_count * 100) / cm.MBs;

        /* start with a 0 size frame */
        size = 0;

        cm.frame_type = FrameType.INTER_FRAME;
        cm.frame_flags = EnumSet.copyOf(frame_flags);

        /* find a free buffer for the new frame */
        {
            int i = 0;
            for (; i < CommonData.NUM_YV12_BUFFERS; ++i) {
                if (cm.yv12_fb[i].flags.isEmpty()) {
                    cm.new_fb_idx = i;
                    break;
                }
            }

            assert (i < CommonData.NUM_YV12_BUFFERS);
        }

        size = encode_frame_to_data_rate(cpi, dest, dest_end, frame_flags);

        if (cpi.compressor_speed == 2) {
            int duration, duration2;
            tsctimer.mark();
            ticktimer.mark();

            duration = (int) (ticktimer.elapsed());
            duration2 = (int) ((double) duration / 2);

            if (cm.frame_type != FrameType.KEY_FRAME) {
                if (cpi.avg_encode_time == 0) {
                    cpi.avg_encode_time = duration;
                } else {
                    cpi.avg_encode_time = (7 * cpi.avg_encode_time + duration) >> 3;
                }
            }

            if (duration2 != 0) {
                {
                    if (cpi.avg_pick_mode_time == 0) {
                        cpi.avg_pick_mode_time = duration2;
                    } else {
                        cpi.avg_pick_mode_time = (7 * cpi.avg_pick_mode_time + duration2) >> 3;
                    }
                }
            }
        }

        if (!cm.refresh_entropy_probs)
            cm.fc = new FrameContext(cm.lfc);

        /* Save the contexts separately for alt ref, gold and last. */
        /* (TODO jbb . Optimize this with pointers to avoid extra copies. ) */
        if (cm.refresh_alt_ref_frame)
            cpi.lfc_a = new FrameContext(cm.fc);

        if (cm.refresh_golden_frame)
            cpi.lfc_g = new FrameContext(cm.fc);

        if (cm.refresh_last_frame)
            cpi.lfc_n = new FrameContext(cm.fc);

        /* if its a dropped frame honor the requests on subsequent frames */
        if (size > 0) {
            cpi.droppable = !frame_is_reference(cpi);

            /* return to normal state */
            cm.refresh_entropy_probs = true;
            cm.refresh_alt_ref_frame = false;
            cm.refresh_golden_frame = false;
            cm.refresh_last_frame = true;
            cm.frame_type = FrameType.INTER_FRAME;
        }

        /* Save layer specific state */
        if (cpi.oxcf.number_of_layers > 1)
            save_layer_context(cpi);

        cmptimer.mark();
        cpi.time_compress_data += cmptimer.elapsed();

        if (cpi.b_calculate_psnr && cm.show_frame) {
            generate_psnr_packet(cpi);
        }
        return size;
    }

    static boolean frame_is_reference(Compressor cpi) {
        final CommonData cm = cpi.common;
        final MacroblockD xd = cpi.mb.e_mbd;

        return cm.frame_type == FrameType.KEY_FRAME || cm.refresh_last_frame || cm.refresh_golden_frame
                || cm.refresh_alt_ref_frame || cm.copy_buffer_to_gf != 0 || cm.copy_buffer_to_arf != 0
                || cm.refresh_entropy_probs || xd.mode_ref_lf_delta_update || xd.update_mb_segmentation_map
                || xd.update_mb_segmentation_data;
    }

    static void vp8_loopfilter_frame(Compressor cpi, CommonData cm) {
        FrameType frame_type = cm.frame_type;

        boolean update_any_ref_buffers = true;
        if (!cpi.common.refresh_last_frame && !cpi.common.refresh_golden_frame && !cpi.common.refresh_alt_ref_frame) {
            update_any_ref_buffers = false;
        }

        if (cm.no_lpf) {
            cm.filter_level = 0;
        } else {
            UsecTimer timer = new UsecTimer();

            timer.timerStart();
            if (!cpi.sf.auto_filter) {
                PickLpf.vp8cx_pick_filter_level_fast(cpi.sourceYV12, cpi);
            } else {
                PickLpf.vp8cx_pick_filter_level(cpi.sourceYV12, cpi);
            }

            if (cm.filter_level > 0) {
                PickLpf.vp8cx_set_alt_lf_level(cpi, cm.filter_level);
            }

            timer.mark();
            cpi.time_pick_lpf += timer.elapsed();
        }

        // No need to apply loop-filter if the encoded frame does not update
        // any reference buffers.
        if (cm.filter_level > 0 && update_any_ref_buffers) {
            LoopFilter.vp8_loop_filter_frame(cm, cpi.mb.e_mbd, frame_type);
        }

        cm.frame_to_show.extend_frame_borders();
    }

    static void update_reference_frames(Compressor cpi) {
        CommonData cm = cpi.common;

        /*
         * At this point the new frame has been encoded. If any buffer copy / swapping
         * is signaled it should be done here.
         */

        if (cm.frame_type == FrameType.KEY_FRAME) {
            refreshFBWithNewFrame(cpi, MVReferenceFrame.GOLDEN_FRAME);
            refreshFBWithNewFrame(cpi, MVReferenceFrame.ALTREF_FRAME);
        } else {
            if (cm.refresh_alt_ref_frame) {
                assert (cm.copy_buffer_to_arf == 0);
                refreshFBWithNewFrame(cpi, MVReferenceFrame.ALTREF_FRAME);
            } else if (cm.copy_buffer_to_arf != 0) {

                if (cm.copy_buffer_to_arf == 1) {
                    if (cm.frameIdxs.get(MVReferenceFrame.ALTREF_FRAME) != cm.frameIdxs
                            .get(MVReferenceFrame.LAST_FRAME)) {
                        refreshFBWithOtherFB(cpi, MVReferenceFrame.ALTREF_FRAME, MVReferenceFrame.LAST_FRAME);

                    }
                } else {
                    if (cm.frameIdxs.get(MVReferenceFrame.ALTREF_FRAME) != cm.frameIdxs
                            .get(MVReferenceFrame.GOLDEN_FRAME)) {
                        refreshFBWithOtherFB(cpi, MVReferenceFrame.ALTREF_FRAME, MVReferenceFrame.GOLDEN_FRAME);
                    }
                }
            }

            if (cm.refresh_golden_frame) {
                assert (cm.copy_buffer_to_gf == 0);
                refreshFBWithNewFrame(cpi, MVReferenceFrame.GOLDEN_FRAME);
            } else if (cm.copy_buffer_to_gf != 0) {

                if (cm.copy_buffer_to_gf == 1) {
                    if (cm.frameIdxs.get(MVReferenceFrame.GOLDEN_FRAME) != cm.frameIdxs
                            .get(MVReferenceFrame.LAST_FRAME)) {
                        refreshFBWithOtherFB(cpi, MVReferenceFrame.GOLDEN_FRAME, MVReferenceFrame.LAST_FRAME);
                    }
                } else {
                    if (cm.frameIdxs.get(MVReferenceFrame.ALTREF_FRAME) != cm.frameIdxs
                            .get(MVReferenceFrame.GOLDEN_FRAME)) {
                        refreshFBWithOtherFB(cpi, MVReferenceFrame.GOLDEN_FRAME, MVReferenceFrame.ALTREF_FRAME);
                    }
                }
            }
        }

        if (cm.refresh_last_frame) {
            refreshFBWithNewFrame(cpi, MVReferenceFrame.LAST_FRAME);
        }
    }

    private static void refreshFBWithNewFrame(Compressor cpi, MVReferenceFrame whichFb) {
        CommonData cm = cpi.common;
        cm.yv12_fb[cm.new_fb_idx].flags.add(whichFb);
        cm.yv12_fb[cm.frameIdxs.get(whichFb)].flags.remove(whichFb);
        cm.frameIdxs.put(whichFb, cm.new_fb_idx);
        cpi.current_ref_frames.put(whichFb, cm.current_video_frame);
    }

    private static void refreshFBWithOtherFB(Compressor cpi, MVReferenceFrame whichFb, MVReferenceFrame otherFb) {
        CommonData cm = cpi.common;
        YV12buffer[] yv12_fb = cm.yv12_fb;
        yv12_fb[cm.frameIdxs.get(otherFb)].flags.add(whichFb);
        yv12_fb[cm.frameIdxs.get(whichFb)].flags.remove(whichFb);
        cm.frameIdxs.put(whichFb, cm.frameIdxs.get(otherFb));
        cpi.current_ref_frames.put(whichFb, cpi.current_ref_frames.get(otherFb));
    }

    static void update_alt_ref_frame_stats(Compressor cpi) {
        CommonData cm = cpi.common;

        /* Select an interval before next GF or altref */
        if (!cpi.auto_gold)
            cpi.frames_till_gf_update_due = OnyxInt.DEFAULT_GF_INTERVAL;

        if (cpi.frames_till_gf_update_due != 0) {
            cpi.current_gf_interval = cpi.frames_till_gf_update_due;

            /*
             * Set the bits per frame that we should try and recover in subsequent inter
             * frames to account for the extra GF spend... note that his does not apply for
             * GF updates that occur coincident with a key frame as the extra cost of key
             * frames is dealt with elsewhere.
             */
            cpi.gf_overspend_bits += cpi.projected_frame_size;
            cpi.non_gf_bitrate_adjustment = cpi.gf_overspend_bits / cpi.frames_till_gf_update_due;
        }

        /* Update data structure that monitors level of reference to last GF */
        cpi.gf_active_flags.memset(0, (short) 1, (cm.mb_rows * cm.mb_cols));
        cpi.gf_active_count = cm.mb_rows * cm.mb_cols;

        /* this frame refreshes means next frames don't unless specified by user */
        cpi.frames_since_golden = 0;

        /* Clear the alternate reference update pending flag. */
        cpi.source_alt_ref_pending = false;

        /* Set the alternate reference frame active flag */
        cpi.source_alt_ref_active = true;
    }

    static void update_golden_frame_stats(Compressor cpi) {
        CommonData cm = cpi.common;

        /* Update the Golden frame usage counts. */
        if (cm.refresh_golden_frame) {
            /* Select an interval before next GF */
            if (!cpi.auto_gold)
                cpi.frames_till_gf_update_due = OnyxInt.DEFAULT_GF_INTERVAL;

            if ((cpi.frames_till_gf_update_due > 0)) {
                cpi.current_gf_interval = cpi.frames_till_gf_update_due;

                /*
                 * Set the bits per frame that we should try and recover in subsequent inter
                 * frames to account for the extra GF spend... note that his does not apply for
                 * GF updates that occur coincident with a key frame as the extra cost of key
                 * frames is dealt with elsewhere.
                 */
                if ((cm.frame_type != FrameType.KEY_FRAME) && !cpi.source_alt_ref_active) {
                    /*
                     * Calcluate GF bits to be recovered Projected size - av frame bits available
                     * for inter frames for clip as a whole
                     */
                    cpi.gf_overspend_bits += (cpi.projected_frame_size - cpi.inter_frame_target);
                }

                cpi.non_gf_bitrate_adjustment = cpi.gf_overspend_bits / cpi.frames_till_gf_update_due;
            }

            /* Update data structure that monitors level of reference to last GF */
            cpi.gf_active_flags.memset(0, (short) 1, (cm.mb_rows * cm.mb_cols));
            cpi.gf_active_count = cm.mb_rows * cm.mb_cols;

            /*
             * this frame refreshes means next frames don't unless specified by user
             */
            cm.refresh_golden_frame = false;
            cpi.frames_since_golden = 0;
            for (MVReferenceFrame rf : MVReferenceFrame.validFrames) {
                cpi.recent_ref_frame_usage.put(rf, 1);
            }

            /* ******** Fixed Q test code only ************ */
            /*
             * If we are going to use the ALT reference for the next group of frames set a
             * flag to say so.
             */
            if (cpi.oxcf.fixed_q >= 0 && cpi.oxcf.play_alternate && !cpi.common.refresh_alt_ref_frame) {
                cpi.source_alt_ref_pending = true;
                cpi.frames_till_gf_update_due = cpi.baseline_gf_interval;
            }

            if (!cpi.source_alt_ref_pending)
                cpi.source_alt_ref_active = false;

            /* Decrement count down till next gf */
            if (cpi.frames_till_gf_update_due > 0)
                cpi.frames_till_gf_update_due--;

        } else if (!cpi.common.refresh_alt_ref_frame) {
            /* Decrement count down till next gf */
            if (cpi.frames_till_gf_update_due > 0)
                cpi.frames_till_gf_update_due--;

            if (cpi.frames_till_alt_ref_frame != 0)
                cpi.frames_till_alt_ref_frame--;

            cpi.frames_since_golden++;

            if (cpi.frames_since_golden > 1) {
                for (MVReferenceFrame rf : MVReferenceFrame.validFrames) {
                    cpi.recent_ref_frame_usage.put(rf,
                            cpi.recent_ref_frame_usage.get(rf) + cpi.mb.count_mb_ref_frame_usage.get(rf));
                }
            }
        }
    }

    static int vp8_receive_raw_frame(Compressor cpi, EnumSet<FrameTypeFlags> frame_flags, YV12buffer sd,
            long time_stamp, long end_time) {
        UsecTimer timer = new UsecTimer();
        int res = 0;
        timer.timerStart();

        /* Reinit the lookahead buffer if the frame size changes */
        if (sd.y_width != cpi.oxcf.Width || sd.y_height != cpi.oxcf.Height) {
            assert (cpi.oxcf.lag_in_frames < 2);
            dealloc_raw_frame_buffers(cpi);
            alloc_raw_frame_buffers(cpi);
        }

        if (cpi.lookahead.vp8_lookahead_push(sd, time_stamp, end_time, frame_flags,
                cpi.active_map_enabled ? cpi.active_map : null)) {
            res = -1;
        }
        if (cpi.oxcf.screen_content_mode > 0 && cpi.sourceYV12 != null) {
            if (cpi.oxcf.hinter != null) {
                cpi.repeatFrameDetected = cpi.oxcf.hinter.isFrameRepeat();
            } else {
                LookaheadEntry lent = cpi.lookahead.vp8_lookahead_peek(0, -1);
                cpi.repeatFrameDetected = cpi.sourceYV12.y_buffer.compareTo(lent.img.y_buffer) == 0;
            }
        }
        timer.mark();
        cpi.time_receive_data += timer.elapsed();

        return res;
    }

    public static final int VPX_TS_MAX_LAYERS = 5;

    public static final int VPX_TS_MAX_PERIODICITY = 16;

}
