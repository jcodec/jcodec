package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.FrameContext;
import org.jcodec.codecs.vpx.vp8.data.LayerContext;
import org.jcodec.codecs.vpx.vp8.data.OnyxInt;
import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.enums.EndUsage;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.FrameTypeFlags;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;

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
public class RateCtrl {
    public static final int[][] vp8_bits_per_mb = {
            /* Intra case 450000/Qintra */
            { 1125000, 900000, 750000, 642857, 562500, 500000, 450000, 450000, 409090, 375000, 346153, 321428, 300000,
                    281250, 264705, 264705, 250000, 236842, 225000, 225000, 214285, 214285, 204545, 204545, 195652,
                    195652, 187500, 180000, 180000, 173076, 166666, 160714, 155172, 150000, 145161, 140625, 136363,
                    132352, 128571, 125000, 121621, 121621, 118421, 115384, 112500, 109756, 107142, 104651, 102272,
                    100000, 97826, 97826, 95744, 93750, 91836, 90000, 88235, 86538, 84905, 83333, 81818, 80357, 78947,
                    77586, 76271, 75000, 73770, 72580, 71428, 70312, 69230, 68181, 67164, 66176, 65217, 64285, 63380,
                    62500, 61643, 60810, 60000, 59210, 59210, 58441, 57692, 56962, 56250, 55555, 54878, 54216, 53571,
                    52941, 52325, 51724, 51136, 50561, 49450, 48387, 47368, 46875, 45918, 45000, 44554, 44117, 43269,
                    42452, 41666, 40909, 40178, 39473, 38793, 38135, 36885, 36290, 35714, 35156, 34615, 34090, 33582,
                    33088, 32608, 32142, 31468, 31034, 30405, 29801, 29220, 28662, },
            /* Inter case 285000/Qinter */
            { 712500, 570000, 475000, 407142, 356250, 316666, 285000, 259090, 237500, 219230, 203571, 190000, 178125,
                    167647, 158333, 150000, 142500, 135714, 129545, 123913, 118750, 114000, 109615, 105555, 101785,
                    98275, 95000, 91935, 89062, 86363, 83823, 81428, 79166, 77027, 75000, 73076, 71250, 69512, 67857,
                    66279, 64772, 63333, 61956, 60638, 59375, 58163, 57000, 55882, 54807, 53773, 52777, 51818, 50892,
                    50000, 49137, 47500, 45967, 44531, 43181, 41911, 40714, 39583, 38513, 37500, 36538, 35625, 34756,
                    33928, 33139, 32386, 31666, 30978, 30319, 29687, 29081, 28500, 27941, 27403, 26886, 26388, 25909,
                    25446, 25000, 24568, 23949, 23360, 22800, 22265, 21755, 21268, 20802, 20357, 19930, 19520, 19127,
                    18750, 18387, 18037, 17701, 17378, 17065, 16764, 16473, 16101, 15745, 15405, 15079, 14766, 14467,
                    14179, 13902, 13636, 13380, 13133, 12895, 12666, 12445, 12179, 11924, 11632, 11445, 11220, 11003,
                    10795, 10594, 10401, 10215, 10035, } };

    public static final int[] kf_boost_qadjustment = { 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140,
            141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161,
            162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182,
            183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 200, 201, 201,
            202, 203, 203, 203, 204, 204, 205, 205, 206, 206, 207, 207, 208, 208, 209, 209, 210, 210, 211, 211, 212,
            212, 213, 213, 214, 214, 215, 215, 216, 216, 217, 217, 218, 218, 219, 219, 220, 220, 220, 220, 220, 220,
            220, 220, 220, 220, 220, 220, 220, 220, 220, 220, };
    public static final int[] vp8_gf_boost_qadjustment = { 80, 82, 84, 86, 88, 90, 92, 94, 96, 97, 98, 99, 100, 101,
            102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122,
            123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143,
            144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164,
            165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 184,
            185, 185, 186, 186, 187, 187, 188, 188, 189, 189, 190, 190, 191, 191, 192, 192, 193, 193, 194, 194, 194,
            194, 195, 195, 196, 196, 197, 197, 198, 198 };
    public static final int[] gf_intra_usage_adjustment = { 125, 120, 115, 110, 105, 100, 95, 85, 80, 75, 70, 65, 60,
            55, 50, 50, 50, 50, 50, 50, };
    public static final int[] gf_adjust_table = { 100, 115, 130, 145, 160, 175, 190, 200, 210, 220, 230, 240, 260, 270,
            280, 290, 300, 310, 320, 330, 340, 350, 360, 370, 380, 390, 400, 400, 400, 400, 400, 400, 400, 400, 400,
            400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400,
            400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400,
            400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400,
            400, 400, 400, };
    public static final int[] kf_gf_boost_qlimits = { 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200, 205, 210,
            215, 220, 225, 230, 235, 240, 245, 250, 255, 260, 265, 270, 275, 280, 285, 290, 295, 300, 305, 310, 320,
            330, 340, 350, 360, 370, 380, 390, 400, 410, 420, 430, 440, 450, 460, 470, 480, 490, 500, 510, 520, 530,
            540, 550, 560, 570, 580, 590, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600,
            600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600,
            600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600, 600,
            600, 600, 600, 600, 600, 600, 600, 600, 600, 600, };
    public static final int[] gf_interval_table = { 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 9, 9,
            9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
            10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, };
    public static final int[] prior_key_frame_weight = { 1, 2, 3, 4, 5 };

    public static final int BPER_MB_NORMBITS = 9;
    public static final double MIN_BPB_FACTOR = 0.01;
    public static final double MAX_BPB_FACTOR = 50;

    static void vp8_setup_key_frame(Compressor cpi) {
        /* Setup for Key frame: */

        cpi.common.fc.toDefault();
        EncodeMV.vp8_build_component_cost_table(cpi.mb.mvcost, cpi.common.fc.mvc, new boolean[] { true, true });

        /*
         * Make sure we initialize separate contexts for altref,gold, and normal. TODO
         * shouldn't need 3 different copies of structure to do this!
         */
        cpi.lfc_a = new FrameContext(cpi.common.fc);
        cpi.lfc_g = new FrameContext(cpi.common.fc);
        cpi.lfc_n = new FrameContext(cpi.common.fc);

        cpi.common.filter_level = (short) (cpi.common.base_qindex * 3 / 8);

        /* Provisional interval before next GF */
        if (cpi.auto_gold) {
            cpi.frames_till_gf_update_due = cpi.baseline_gf_interval;
        } else {
            cpi.frames_till_gf_update_due = OnyxInt.DEFAULT_GF_INTERVAL;
        }

        cpi.common.refresh_golden_frame = true;
        cpi.common.refresh_alt_ref_frame = true;
    }

    static int estimate_bits_at_q(MVReferenceFrame frame_kind, int Q, int MBs, double correction_factor) {
        int Bpm = (int) (.5
                + correction_factor * vp8_bits_per_mb[frame_kind == MVReferenceFrame.INTRA_FRAME ? 0 : 1][Q]);

        /*
         * Attempt to retain reasonable accuracy without overflow. The cutoff is chosen
         * such that the maximum product of Bpm and MBs fits 31 bits. The largest Bpm
         * takes 20 bits.
         */
        if (MBs > (1 << 11)) {
            return (Bpm >> BPER_MB_NORMBITS) * MBs;
        } else {
            return (Bpm * MBs) >> BPER_MB_NORMBITS;
        }
    }

    static void calc_iframe_target_size(Compressor cpi) {
        /* boost defaults to half second */
        int kf_boost;
        long target;

        if (cpi.oxcf.fixed_q >= 0) {
            int Q = cpi.oxcf.key_q;

            target = estimate_bits_at_q(MVReferenceFrame.INTRA_FRAME, Q, cpi.common.MBs,
                    cpi.key_frame_rate_correction_factor);
        }
        /* First Frame is a special case */
        else if (cpi.common.current_video_frame == 0) {
            /*
             * 1 Pass there is no information on which to base size so use bandwidth per
             * second * fraction of the initial buffer level
             */
            target = cpi.oxcf.starting_buffer_level / 2;

            if (target > cpi.oxcf.target_bandwidth * 3 / 2) {
                target = cpi.oxcf.target_bandwidth * 3 / 2;
            }
        } else {
            /* if this keyframe was forced, use a more recent Q estimate */
            int Q = (cpi.common.frame_flags.contains(FrameTypeFlags.Key)) ? cpi.avg_frame_qindex : cpi.ni_av_qi;

            int initial_boost = 32; /* |3.0 * per_frame_bandwidth| */
            /* Boost depends somewhat on frame rate: only used for 1 layer case. */
            if (cpi.oxcf.number_of_layers == 1) {
                kf_boost = Math.max(initial_boost, (int) (2 * cpi.output_framerate - 16));
            } else {
                /* Initial factor: set target size to: |3.0 * per_frame_bandwidth|. */
                kf_boost = initial_boost;
            }

            /* adjustment up based on q: this factor ranges from ~1.2 to 2.2. */
            kf_boost = kf_boost * kf_boost_qadjustment[Q] / 100;

            /* frame separation adjustment ( down) */
            if (cpi.frames_since_key < cpi.output_framerate / 2) {
                kf_boost = (int) (kf_boost * cpi.frames_since_key / (cpi.output_framerate / 2));
            }

            /* Minimal target size is |2* per_frame_bandwidth|. */
            if (kf_boost < 16)
                kf_boost = 16;

            target = ((16 + kf_boost) * cpi.per_frame_bandwidth) >> 4;
        }

        if (cpi.oxcf.rc_max_intra_bitrate_pct != 0) {
            int max_rate = cpi.per_frame_bandwidth * cpi.oxcf.rc_max_intra_bitrate_pct / 100;

            if (target > max_rate)
                target = max_rate;
        }

        cpi.this_frame_target = (int) target;

        /*
         * TODO: if we separate rate targeting from Q targeting, move this. Reset the
         * active worst quality to the baseline value for key frames.
         */
        cpi.active_worst_quality = cpi.worst_quality;

    }

    private static int getGoldenFrameUsage(Compressor cpi) {
        int gf_frame_useage = 0; /* Golden frame useage since last GF */
        int tot_mbs = 0;
        for (MVReferenceFrame mvrf : MVReferenceFrame.validFrames) {
            cpi.recent_ref_frame_usage.get(mvrf);
        }

        int pct_gf_active = (100 * cpi.gf_active_count) / (cpi.common.mb_rows * cpi.common.mb_cols);

        if (tot_mbs != 0) {
            gf_frame_useage = (cpi.recent_ref_frame_usage.get(MVReferenceFrame.GOLDEN_FRAME)
                    + cpi.recent_ref_frame_usage.get(MVReferenceFrame.ALTREF_FRAME)) * 100 / tot_mbs;
        }

        if (pct_gf_active > gf_frame_useage)
            gf_frame_useage = pct_gf_active;
        return gf_frame_useage;
    }

    /*
     * Do the best we can to define the parameters for the next GF based on what
     * information we have available.
     */
    static void calc_gf_params(Compressor cpi) {
        int Q = (cpi.oxcf.fixed_q < 0) ? cpi.last_q[1] : cpi.oxcf.fixed_q;
        int Boost = 0;

        int gf_frame_useage = getGoldenFrameUsage(cpi);
        /* Single Pass compression: Has to use current and historical data */

        /* Adjust boost based upon ambient Q */
        Boost = vp8_gf_boost_qadjustment[Q];

        /* Adjust based upon most recently measure intra useage */
        Boost = Boost
                * gf_intra_usage_adjustment[(cpi.this_frame_percent_intra < 15) ? cpi.this_frame_percent_intra : 14]
                / 100;

        /* Adjust gf boost based upon GF usage since last GF */
        Boost = Boost * gf_adjust_table[gf_frame_useage] / 100;

        /*
         * golden frame boost without recode loop often goes awry. be safe by keeping
         * numbers down.
         */
        if (cpi.sf.recode_loop == 0) {
            if (cpi.compressor_speed == 2)
                Boost = Boost / 2;
        }

        /* Apply an upper limit based on Q for 1 pass encodes */
        if (Boost > kf_gf_boost_qlimits[Q]) {
            Boost = kf_gf_boost_qlimits[Q];

            /* Apply lower limits to boost. */
        } else if (Boost < 110) {
            Boost = 110;
        }

        /* Note the boost used */
        cpi.last_boost = Boost;

        /*
         * Estimate next interval This is updated once the real frame size/boost is
         * known.
         */
        if (cpi.oxcf.fixed_q == -1) {
            cpi.frames_till_gf_update_due = cpi.baseline_gf_interval;

            if (cpi.last_boost > 750)
                cpi.frames_till_gf_update_due++;

            if (cpi.last_boost > 1000)
                cpi.frames_till_gf_update_due++;

            if (cpi.last_boost > 1250)
                cpi.frames_till_gf_update_due++;

            if (cpi.last_boost >= 1500)
                cpi.frames_till_gf_update_due++;

            if (gf_interval_table[gf_frame_useage] > cpi.frames_till_gf_update_due) {
                cpi.frames_till_gf_update_due = gf_interval_table[gf_frame_useage];
            }

            if (cpi.frames_till_gf_update_due > cpi.max_gf_interval) {
                cpi.frames_till_gf_update_due = cpi.max_gf_interval;
            }

        } else {
            cpi.frames_till_gf_update_due = cpi.baseline_gf_interval;
        }

        cpi.source_alt_ref_pending = false;
    }

    static void calc_pframe_target_size(Compressor cpi) {
        int min_frame_target;
        int old_per_frame_bandwidth = cpi.per_frame_bandwidth;

        if (cpi.current_layer > 0) {
            cpi.per_frame_bandwidth = cpi.layer_context[cpi.current_layer].avg_frame_size_for_layer;
        }

        min_frame_target = 0;

        if (min_frame_target < cpi.per_frame_bandwidth / 4) {
            min_frame_target = cpi.per_frame_bandwidth / 4;
        }

        /* Special alt reference frame case */
        if (!((cpi.common.refresh_alt_ref_frame) && (cpi.oxcf.number_of_layers == 1))) {
            /* Normal frames (gf,and inter) */
            int Adjustment;
            /*
             * Make rate adjustment to recover bits spent in key frame Test to see if the
             * key frame inter data rate correction should still be in force
             */
            if (cpi.kf_overspend_bits > 0) {
                Adjustment = (cpi.kf_bitrate_adjustment <= cpi.kf_overspend_bits) ? cpi.kf_bitrate_adjustment
                        : cpi.kf_overspend_bits;

                if (Adjustment > (cpi.per_frame_bandwidth - min_frame_target)) {
                    Adjustment = (cpi.per_frame_bandwidth - min_frame_target);
                }

                cpi.kf_overspend_bits -= Adjustment;

                /*
                 * Calculate an inter frame bandwidth target for the next few frames designed to
                 * recover any extra bits spent on the key frame.
                 */
                cpi.this_frame_target = cpi.per_frame_bandwidth - Adjustment;
                if (cpi.this_frame_target < min_frame_target) {
                    cpi.this_frame_target = min_frame_target;
                }
            } else {
                cpi.this_frame_target = cpi.per_frame_bandwidth;
            }

            /*
             * If appropriate make an adjustment to recover bits spent on a recent GF
             */
            if ((cpi.gf_overspend_bits > 0) && (cpi.this_frame_target > min_frame_target)) {
                Adjustment = (cpi.non_gf_bitrate_adjustment <= cpi.gf_overspend_bits) ? cpi.non_gf_bitrate_adjustment
                        : cpi.gf_overspend_bits;

                if (Adjustment > (cpi.this_frame_target - min_frame_target)) {
                    Adjustment = (cpi.this_frame_target - min_frame_target);
                }

                cpi.gf_overspend_bits -= Adjustment;
                cpi.this_frame_target -= Adjustment;
            }

            /* Apply small + and - boosts for non gf frames */
            if ((cpi.last_boost > 150) && (cpi.frames_till_gf_update_due > 0)
                    && (cpi.current_gf_interval >= (OnyxInt.MIN_GF_INTERVAL << 1))) {
                /* % Adjustment limited to the range 1% to 10% */
                Adjustment = (cpi.last_boost - 100) >> 5;

                if (Adjustment < 1) {
                    Adjustment = 1;
                } else if (Adjustment > 10) {
                    Adjustment = 10;
                }

                /* Convert to bits */
                Adjustment = (cpi.this_frame_target * Adjustment) / 100;

                if (Adjustment > (cpi.this_frame_target - min_frame_target)) {
                    Adjustment = (cpi.this_frame_target - min_frame_target);
                }

                if (cpi.frames_since_golden == (cpi.current_gf_interval >> 1)) {
                    Adjustment = (cpi.current_gf_interval - 1) * Adjustment;
                    // Limit adjustment to 10% of current target.
                    if (Adjustment > (10 * cpi.this_frame_target) / 100) {
                        Adjustment = (10 * cpi.this_frame_target) / 100;
                    }
                    cpi.this_frame_target += Adjustment;
                } else {
                    cpi.this_frame_target -= Adjustment;
                }

            }

        }

        /*
         * Sanity check that the total sum of adjustments is not above the maximum
         * allowed That is that having allowed for KF and GF penalties we have not
         * pushed the current interframe target to low. If the adjustment we apply here
         * is not capable of recovering all the extra bits we have spent in the KF or GF
         * then the remainder will have to be recovered over a longer time span via
         * other buffer / rate control mechanisms.
         */
        if (cpi.this_frame_target < min_frame_target) {
            cpi.this_frame_target = min_frame_target;
        }

        if (!cpi.common.refresh_alt_ref_frame) {
            /* Note the baseline target data rate for this inter frame. */
            cpi.inter_frame_target = cpi.this_frame_target;
        }

        /* Adapt target frame size with respect to any buffering constraints: */
        if (cpi.buffered_mode) {
            int one_percent_bits = (int) (1 + cpi.oxcf.optimal_buffer_level / 100);

            if ((cpi.buffer_level < cpi.oxcf.optimal_buffer_level)
                    || (cpi.bits_off_target < cpi.oxcf.optimal_buffer_level)) {
                int percent_low = 0;

                /*
                 * Decide whether or not we need to adjust the frame data rate target.
                 *
                 * If we are are below the optimal buffer fullness level and adherence to
                 * buffering constraints is important to the end usage then adjust the per frame
                 * target.
                 */
                if ((cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER)
                        && (cpi.buffer_level < cpi.oxcf.optimal_buffer_level)) {
                    percent_low = (int) ((cpi.oxcf.optimal_buffer_level - cpi.buffer_level) / one_percent_bits);
                }
                /* Are we overshooting the long term clip data rate... */
                else if (cpi.bits_off_target < 0) {
                    /* Adjust per frame data target downwards to compensate. */
                    percent_low = (int) (100 * -cpi.bits_off_target / (cpi.total_byte_count * 8));
                }

                if (percent_low > cpi.oxcf.under_shoot_pct) {
                    percent_low = cpi.oxcf.under_shoot_pct;
                } else if (percent_low < 0) {
                    percent_low = 0;
                }

                /* lower the target bandwidth for this frame. */
                cpi.this_frame_target -= (cpi.this_frame_target * percent_low) / 200;

                /*
                 * Are we using allowing control of active_worst_allowed_q according to buffer
                 * level.
                 */
                if (cpi.auto_worst_q && cpi.ni_frames > 150) {
                    long critical_buffer_level;

                    /*
                     * For streaming applications the most important factor is cpi.buffer_level as
                     * this takes into account the specified short term buffering constraints.
                     * However, hitting the long term clip data rate target is also important.
                     */
                    if (cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER) {
                        /*
                         * Take the smaller of cpi.buffer_level and cpi.bits_off_target
                         */
                        critical_buffer_level = (cpi.buffer_level < cpi.bits_off_target) ? cpi.buffer_level
                                : cpi.bits_off_target;
                    }
                    /*
                     * For local file playback short term buffering constraints are less of an issue
                     */
                    else {
                        /*
                         * Consider only how we are doing for the clip as a whole
                         */
                        critical_buffer_level = cpi.bits_off_target;
                    }

                    /*
                     * Set the active worst quality based upon the selected buffer fullness number.
                     */
                    if (critical_buffer_level < cpi.oxcf.optimal_buffer_level) {
                        if (critical_buffer_level > (cpi.oxcf.optimal_buffer_level >> 2)) {
                            long qadjustment_range = cpi.worst_quality - cpi.ni_av_qi;
                            long above_base = (critical_buffer_level - (cpi.oxcf.optimal_buffer_level >> 2));

                            /*
                             * Step active worst quality down from cpi.ni_av_qi when (critical_buffer_level
                             * == cpi.optimal_buffer_level) to cpi.worst_quality when (critical_buffer_level
                             * == cpi.optimal_buffer_level >> 2)
                             */
                            cpi.active_worst_quality = (short) (cpi.worst_quality
                                    - (int) ((qadjustment_range * above_base)
                                            / (cpi.oxcf.optimal_buffer_level * 3 >> 2)));
                        } else {
                            cpi.active_worst_quality = cpi.worst_quality;
                        }
                    } else {
                        cpi.active_worst_quality = cpi.ni_av_qi;
                    }
                } else {
                    cpi.active_worst_quality = cpi.worst_quality;
                }
            } else {
                int percent_high = 0;

                if ((cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER)
                        && (cpi.buffer_level > cpi.oxcf.optimal_buffer_level)) {
                    percent_high = (int) ((cpi.buffer_level - cpi.oxcf.optimal_buffer_level) / one_percent_bits);
                } else if (cpi.bits_off_target > cpi.oxcf.optimal_buffer_level) {
                    percent_high = (int) ((100 * cpi.bits_off_target) / (cpi.total_byte_count * 8));
                }

                if (percent_high > cpi.oxcf.over_shoot_pct) {
                    percent_high = cpi.oxcf.over_shoot_pct;
                } else if (percent_high < 0) {
                    percent_high = 0;
                }

                cpi.this_frame_target += (cpi.this_frame_target * percent_high) / 200;

                /*
                 * Are we allowing control of active_worst_allowed_q according to buffer level.
                 */
                if (cpi.auto_worst_q && cpi.ni_frames > 150) {
                    /*
                     * When using the relaxed buffer model stick to the user specified value
                     */
                    cpi.active_worst_quality = cpi.ni_av_qi;
                } else {
                    cpi.active_worst_quality = cpi.worst_quality;
                }
            }

            /* Set active_best_quality to prevent quality rising too high */
            cpi.active_best_quality = cpi.best_quality;

            /* Worst quality obviously must not be better than best quality */
            if (cpi.active_worst_quality <= cpi.active_best_quality) {
                cpi.active_worst_quality = (short) (cpi.active_best_quality + 1);
            }

            if (cpi.active_worst_quality > 127)
                cpi.active_worst_quality = 127;
        }
        /* Unbuffered mode (eg. video conferencing) */
        else {
            /* Set the active worst quality */
            cpi.active_worst_quality = cpi.worst_quality;
        }

        /*
         * Special trap for constrained quality mode "active_worst_quality" may never
         * drop below cq level for any frame type.
         */
        if (cpi.oxcf.end_usage == EndUsage.CONSTRAINED_QUALITY && cpi.active_worst_quality < cpi.cq_target_quality) {
            cpi.active_worst_quality = cpi.cq_target_quality;
        }

        /*
         * Test to see if we have to drop a frame The auto-drop frame code is only used
         * in buffered mode. In unbufferd mode (eg vide conferencing) the descision to
         * code or drop a frame is made outside the codec in response to real world
         * comms or buffer considerations.
         */
        if (cpi.drop_frames_allowed && (cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER)
                && ((cpi.common.frame_type != FrameType.KEY_FRAME))) {
            /*
             * Check for a buffer underun-crisis in which case we have to drop a frame
             */
            if ((cpi.buffer_level < 0)) {
                cpi.drop_frame = true;

                /* Update the buffer level variable. */
                cpi.bits_off_target += cpi.av_per_frame_bandwidth;
                if (cpi.bits_off_target > cpi.oxcf.maximum_buffer_size) {
                    cpi.bits_off_target = (int) cpi.oxcf.maximum_buffer_size;
                }
                cpi.buffer_level = cpi.bits_off_target;

                if (cpi.oxcf.number_of_layers > 1) {
                    int i;

                    // Propagate bits saved by dropping the frame to higher layers.
                    for (i = cpi.current_layer + 1; i < cpi.oxcf.number_of_layers; ++i) {
                        LayerContext lc = cpi.layer_context[i];
                        lc.bits_off_target += (int) (lc.target_bandwidth / lc.framerate);
                        if (lc.bits_off_target > lc.maximum_buffer_size) {
                            lc.bits_off_target = lc.maximum_buffer_size;
                        }
                        lc.buffer_level = lc.bits_off_target;
                    }
                }
            }
        }

        /* Adjust target frame size for Golden Frames: */
        if (!cpi.oxcf.error_resilient_mode && (cpi.frames_till_gf_update_due == 0) && !cpi.drop_frame) {
            if (!cpi.gf_update_onepass_cbr) {
                int Q = (cpi.oxcf.fixed_q < 0) ? cpi.last_q[1] : cpi.oxcf.fixed_q;

                int gf_frame_useage = getGoldenFrameUsage(cpi);

                /* Is a fixed manual GF frequency being used */
                if (cpi.auto_gold) {
                    /*
                     * throw a GF if recent frame intra useage is low or the GF useage is high
                     */
                    if ((cpi.this_frame_percent_intra < 15 || gf_frame_useage >= 5)) {
                        cpi.common.refresh_golden_frame = true;
                    }
                }

                if (cpi.common.refresh_golden_frame) {
                    if (cpi.auto_adjust_gold_quantizer) {
                        calc_gf_params(cpi);
                    }

                    /*
                     * If we are using alternate ref instead of gf then do not apply the boost It
                     * will instead be applied to the altref update Jims modified boost
                     */
                    if (!cpi.source_alt_ref_active) {
                        if (cpi.oxcf.fixed_q < 0) {
                            int Boost = cpi.last_boost;
                            int frames_in_section = cpi.frames_till_gf_update_due + 1;
                            int allocation_chunks = (frames_in_section * 100) + (Boost - 100);
                            int bits_in_section = cpi.inter_frame_target * frames_in_section;

                            /*
                             * Normalize Altboost and allocations chunck down to prevent overflow
                             */
                            while (Boost > 1000) {
                                Boost /= 2;
                                allocation_chunks /= 2;
                            }

                            /* Avoid loss of precision but avoid overflow */
                            if ((bits_in_section >> 7) > allocation_chunks) {
                                cpi.this_frame_target = Boost * (bits_in_section / allocation_chunks);
                            } else {
                                cpi.this_frame_target = (Boost * bits_in_section) / allocation_chunks;
                            }

                        } else {
                            cpi.this_frame_target = (estimate_bits_at_q(MVReferenceFrame.GOLDEN_FRAME, Q,
                                    cpi.common.MBs, 1.0) * cpi.last_boost) / 100;
                        }
                    } else {
                        /*
                         * If there is an active ARF at this location use the minimum bits on this frame
                         * even if it is a contructed arf. The active maximum quantizer insures that an
                         * appropriate number of bits will be spent if needed for contstructed ARFs.
                         */
                        cpi.this_frame_target = 0;
                    }

                    cpi.current_gf_interval = cpi.frames_till_gf_update_due;
                }
            } else {
                // Special case for 1 pass CBR: fixed gf period.
                // TODO(marpan): Adjust this boost/interval logic.
                // If gf_cbr_boost_pct is small (below threshold) set the flag
                // gf_noboost_onepass_cbr = 1, which forces the gf to use the same
                // rate correction factor as last.
                cpi.gf_noboost_onepass_cbr = (cpi.oxcf.gf_cbr_boost_pct <= 100);
                cpi.baseline_gf_interval = cpi.gf_interval_onepass_cbr;
                // Skip this update if the zero_mvcount is low.
                if (cpi.zeromv_count > (cpi.common.MBs >> 1)) {
                    cpi.common.refresh_golden_frame = true;
                    cpi.this_frame_target = (cpi.this_frame_target * (100 + cpi.oxcf.gf_cbr_boost_pct)) / 100;
                }
                cpi.frames_till_gf_update_due = cpi.baseline_gf_interval;
                cpi.current_gf_interval = cpi.frames_till_gf_update_due;
            }
        }

        cpi.per_frame_bandwidth = old_per_frame_bandwidth;
    }

    static class FrameLimits {
        int frame_under_shoot_limit;
        int frame_over_shoot_limit;
    }

    static void vp8_compute_frame_size_bounds(Compressor cpi, FrameLimits limits) {
        /* Set-up bounds on acceptable frame size: */
        if (cpi.oxcf.fixed_q >= 0) {
            /*
             * Fixed Q scenario: frame size never outranges target (there is no target!)
             */
            limits.frame_under_shoot_limit = 0;
            limits.frame_over_shoot_limit = Integer.MAX_VALUE;
        } else {
            final long this_frame_target = cpi.this_frame_target;
            long over_shoot_limit, under_shoot_limit;

            if (cpi.common.frame_type == FrameType.KEY_FRAME) {
                over_shoot_limit = this_frame_target * 9 / 8;
                under_shoot_limit = this_frame_target * 7 / 8;
            } else {
                if (cpi.oxcf.number_of_layers > 1 || cpi.common.refresh_alt_ref_frame
                        || cpi.common.refresh_golden_frame) {
                    over_shoot_limit = this_frame_target * 9 / 8;
                    under_shoot_limit = this_frame_target * 7 / 8;
                } else {
                    /* For CBR take buffer fullness into account */
                    if (cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER) {
                        if (cpi.buffer_level >= ((cpi.oxcf.optimal_buffer_level + cpi.oxcf.maximum_buffer_size) >> 1)) {
                            /*
                             * Buffer is too full so relax overshoot and tighten undershoot
                             */
                            over_shoot_limit = this_frame_target * 12 / 8;
                            under_shoot_limit = this_frame_target * 6 / 8;
                        } else if (cpi.buffer_level <= (cpi.oxcf.optimal_buffer_level >> 1)) {
                            /*
                             * Buffer is too low so relax undershoot and tighten overshoot
                             */
                            over_shoot_limit = this_frame_target * 10 / 8;
                            under_shoot_limit = this_frame_target * 4 / 8;
                        } else {
                            over_shoot_limit = this_frame_target * 11 / 8;
                            under_shoot_limit = this_frame_target * 5 / 8;
                        }
                    }
                    /* VBR and CQ mode */
                    /*
                     * Note that tighter restrictions here can help quality but hurt encode speed
                     */
                    else {
                        /* Stron overshoot limit for constrained quality */
                        if (cpi.oxcf.end_usage == EndUsage.CONSTRAINED_QUALITY) {
                            over_shoot_limit = this_frame_target * 11 / 8;
                            under_shoot_limit = this_frame_target * 2 / 8;
                        } else {
                            over_shoot_limit = this_frame_target * 11 / 8;
                            under_shoot_limit = this_frame_target * 5 / 8;
                        }
                    }
                }
            }

            /*
             * For very small rate targets where the fractional adjustment (eg * 7/8) may be
             * tiny make sure there is at least a minimum range.
             */
            over_shoot_limit += 200;
            under_shoot_limit -= 200;
            if (under_shoot_limit < 0)
                under_shoot_limit = 0;
            if (under_shoot_limit > Integer.MAX_VALUE)
                under_shoot_limit = Integer.MAX_VALUE;
            if (over_shoot_limit > Integer.MAX_VALUE)
                over_shoot_limit = Integer.MAX_VALUE;
            limits.frame_under_shoot_limit = (int) under_shoot_limit;
            limits.frame_over_shoot_limit = (int) over_shoot_limit;
        }
    }

    /* return of 0 means drop frame */
    static boolean vp8_pick_frame_size(Compressor cpi) {
        CommonData cm = cpi.common;

        if (cm.frame_type == FrameType.KEY_FRAME) {
            calc_iframe_target_size(cpi);
        } else {
            calc_pframe_target_size(cpi);

            /* Check if we're dropping the frame: */
            if (cpi.drop_frame) {
                cpi.drop_frame = false;
                return false;
            }
        }
        return true;
    }

    static short limit_q_cbr_inter(short last_q, short current_q) {
        final short limit_down = 12;
        if (last_q - current_q > limit_down)
            return (short) (last_q - limit_down);
        else
            return current_q;
    }

    static short vp8_regulate_q(Compressor cpi, int target_bits_per_frame) {
        short Q = cpi.active_worst_quality;

        if (cpi.force_maxqp == 1) {
            cpi.active_worst_quality = cpi.worst_quality;
            return cpi.worst_quality;
        }
        /* Reset Zbin OQ value */
        cpi.mb.zbin_over_quant = 0;

        if (cpi.oxcf.fixed_q >= 0) {
            Q = cpi.oxcf.fixed_q;

            if (cpi.common.frame_type == FrameType.KEY_FRAME) {
                Q = cpi.oxcf.key_q;
            } else if (cpi.oxcf.number_of_layers == 1 && cpi.common.refresh_alt_ref_frame
                    && !cpi.gf_noboost_onepass_cbr) {
                Q = cpi.oxcf.alt_q;
            } else if (cpi.oxcf.number_of_layers == 1 && cpi.common.refresh_golden_frame
                    && !cpi.gf_noboost_onepass_cbr) {
                Q = cpi.oxcf.gold_q;
            }
        } else {
            short i;
            int last_error = Integer.MAX_VALUE;
            int target_bits_per_mb;
            int bits_per_mb_at_this_q;
            double correction_factor;

            /* Select the appropriate correction factor based upon type of frame. */
            if (cpi.common.frame_type == FrameType.KEY_FRAME) {
                correction_factor = cpi.key_frame_rate_correction_factor;
            } else {
                if (cpi.oxcf.number_of_layers == 1 && !cpi.gf_noboost_onepass_cbr
                        && (cpi.common.refresh_alt_ref_frame || cpi.common.refresh_golden_frame)) {
                    correction_factor = cpi.gf_rate_correction_factor;
                } else {
                    correction_factor = cpi.rate_correction_factor;
                }
            }

            /*
             * Calculate required scaling factor based on target frame size and size of
             * frame produced using previous Q
             */
            if (target_bits_per_frame >= (Integer.MAX_VALUE >> BPER_MB_NORMBITS)) {
                /* Case where we would overflow int */
                target_bits_per_mb = (target_bits_per_frame / cpi.common.MBs) << BPER_MB_NORMBITS;
            } else {
                target_bits_per_mb = (target_bits_per_frame << BPER_MB_NORMBITS) / cpi.common.MBs;
            }

            i = cpi.active_best_quality;

            do {
                bits_per_mb_at_this_q = (int) (.5
                        + correction_factor * vp8_bits_per_mb[cpi.common.frame_type.ordinal()][i]);

                if (bits_per_mb_at_this_q <= target_bits_per_mb) {
                    if ((target_bits_per_mb - bits_per_mb_at_this_q) <= last_error) {
                        Q = i;
                    } else {
                        Q = (short) (i - 1);
                    }

                    break;
                } else {
                    last_error = bits_per_mb_at_this_q - target_bits_per_mb;
                }
            } while (++i <= cpi.active_worst_quality);

            /*
             * If we are at MAXQ then enable Q over-run which seeks to claw back additional
             * bits through things like the RD multiplier and zero bin size.
             */
            if (Q >= OnyxInt.MAXQ) {
                int zbin_oqmax;

                double Factor = 0.99;
                double factor_adjustment = 0.01 / 256.0;

                if (cpi.common.frame_type == FrameType.KEY_FRAME) {
                    zbin_oqmax = 0;
                } else if (cpi.oxcf.number_of_layers == 1 && !cpi.gf_noboost_onepass_cbr
                        && (cpi.common.refresh_alt_ref_frame
                                || (cpi.common.refresh_golden_frame && !cpi.source_alt_ref_active))) {
                    zbin_oqmax = 16;
                } else {
                    zbin_oqmax = OnyxInt.ZBIN_OQ_MAX;
                }

                /*
                 * Each incrment in the zbin is assumed to have a fixed effect on bitrate. This
                 * is not of course true. The effect will be highly clip dependent and may well
                 * have sudden steps. The idea here is to acheive higher effective quantizers
                 * than the normal maximum by expanding the zero bin and hence decreasing the
                 * number of low magnitude non zero coefficients.
                 */
                while (cpi.mb.zbin_over_quant < zbin_oqmax) {
                    cpi.mb.zbin_over_quant++;

                    if (cpi.mb.zbin_over_quant > zbin_oqmax) {
                        cpi.mb.zbin_over_quant = zbin_oqmax;
                    }

                    /* Adjust bits_per_mb_at_this_q estimate */
                    bits_per_mb_at_this_q = (int) (Factor * bits_per_mb_at_this_q);
                    Factor += factor_adjustment;

                    if (Factor >= 0.999)
                        Factor = 0.999;

                    /* Break out if we get down to the target rate */
                    if (bits_per_mb_at_this_q <= target_bits_per_mb)
                        break;
                }
            }
        }

        // Limit decrease in Q for 1 pass CBR screen content mode.
        if (cpi.common.frame_type != FrameType.KEY_FRAME && cpi.oxcf.end_usage == EndUsage.STREAM_FROM_SERVER
                && cpi.oxcf.screen_content_mode != 0)
            Q = limit_q_cbr_inter(cpi.last_q[1], Q);

        return Q;
    }

    // If this just encoded frame (mcomp/transform/quant, but before loopfilter and
    // pack_bitstream) has large overshoot, and was not being encoded close to the
    // max QP, then drop this frame and force next frame to be encoded at max QP.
    // Allow this for screen_content_mode = 2, or if drop frames is allowed.
    // TODO(marpan): Should do this exit condition during the encode_frame
    // (i.e., halfway during the encoding of the frame) to save cycles.
    static boolean vp8_drop_encodedframe_overshoot(Compressor cpi, int Q) {
        if (cpi.common.frame_type != FrameType.KEY_FRAME && (cpi.oxcf.screen_content_mode == 2
                || (cpi.drop_frames_allowed && ((cpi.rate_correction_factor < (8.0f * MIN_BPB_FACTOR)
                        && cpi.frames_since_last_drop_overshoot > (int) cpi.framerate))))) {
            // Note: the "projected_frame_size" from encode_frame() only gives estimate
            // of mode/motion vector rate (in non-rd mode): so below we only require
            // that projected_frame_size is somewhat greater than per-frame-bandwidth,
            // but add additional condition with high threshold on prediction residual.

            // QP threshold: only allow dropping if we are not close to qp_max.
            int thresh_qp = 3 * cpi.worst_quality >> 2;
            // Rate threshold, in bytes.
            int thresh_rate = 2 * (cpi.av_per_frame_bandwidth >> 3);
            // Threshold for the average (over all macroblocks) of the pixel-sum
            // residual error over 16x16 block.
            int thresh_pred_err_mb = (200 << 4);
            int pred_err_mb = (int) (cpi.mb.prediction_error / cpi.common.MBs);
            // Reduce/ignore thresh_rate if pred_err_mb much larger than its threshold,
            // give more weight to pred_err metric for overshoot detection.
            if (cpi.drop_frames_allowed && pred_err_mb > (thresh_pred_err_mb << 4))
                thresh_rate = thresh_rate >> 3;
            if ((Q < thresh_qp && cpi.projected_frame_size > thresh_rate && pred_err_mb > thresh_pred_err_mb
                    && pred_err_mb > 2 * cpi.last_pred_err_mb)) {
                int i;
                double new_correction_factor;
                int target_bits_per_mb;
                final int target_size = cpi.av_per_frame_bandwidth;
                // Flag to indicate we will force next frame to be encoded at max QP.
                cpi.force_maxqp = 1;
                // Reset the buffer levels.
                cpi.buffer_level = cpi.oxcf.optimal_buffer_level;
                cpi.bits_off_target = cpi.oxcf.optimal_buffer_level;
                // Compute a new rate correction factor, corresponding to the current
                // target frame size and max_QP, and adjust the rate correction factor
                // upwards, if needed.
                // This is to prevent a bad state where the re-encoded frame at max_QP
                // undershoots significantly, and then we end up dropping every other
                // frame because the QP/rate_correction_factor may have been too low
                // before the drop and then takes too long to come up.
                if (target_size >= (Integer.MAX_VALUE >> BPER_MB_NORMBITS)) {
                    target_bits_per_mb = (target_size / cpi.common.MBs) << BPER_MB_NORMBITS;
                } else {
                    target_bits_per_mb = (target_size << BPER_MB_NORMBITS) / cpi.common.MBs;
                }
                // Rate correction factor based on target_size_per_mb and max_QP.
                new_correction_factor = (double) target_bits_per_mb / (double) vp8_bits_per_mb[1][cpi.worst_quality];
                if (new_correction_factor > cpi.rate_correction_factor) {
                    cpi.rate_correction_factor = Math.min(2.0 * cpi.rate_correction_factor, new_correction_factor);
                }
                if (cpi.rate_correction_factor > MAX_BPB_FACTOR) {
                    cpi.rate_correction_factor = MAX_BPB_FACTOR;
                }
                // Drop this frame: update frame counters.
                cpi.common.current_video_frame++;
                cpi.frames_since_key++;
                cpi.temporal_pattern_counter++;
                cpi.frames_since_last_drop_overshoot = 0;
                if (cpi.oxcf.number_of_layers > 1) {
                    // Set max_qp and rate correction for all temporal layers if overshoot
                    // is detected.
                    for (i = 0; i < cpi.oxcf.number_of_layers; ++i) {
                        LayerContext lc = cpi.layer_context[i];
                        lc.force_maxqp = 1;
                        lc.frames_since_last_drop_overshoot = 0;
                        lc.rate_correction_factor = cpi.rate_correction_factor;
                    }
                }
                return true;
            }
            cpi.force_maxqp = 0;
            cpi.frames_since_last_drop_overshoot++;
            return false;
        }
        cpi.force_maxqp = 0;
        cpi.frames_since_last_drop_overshoot++;
        return false;
    }

    static void vp8_update_rate_correction_factors(Compressor cpi, int damp_var) {
        int Q = cpi.common.base_qindex;
        int correction_factor = 100;
        double rate_correction_factor;
        double adjustment_limit;

        int projected_size_based_on_q = 0;

        if (cpi.common.frame_type == FrameType.KEY_FRAME) {
            rate_correction_factor = cpi.key_frame_rate_correction_factor;
        } else {
            if (cpi.oxcf.number_of_layers == 1 && !cpi.gf_noboost_onepass_cbr
                    && (cpi.common.refresh_alt_ref_frame || cpi.common.refresh_golden_frame)) {
                rate_correction_factor = cpi.gf_rate_correction_factor;
            } else {
                rate_correction_factor = cpi.rate_correction_factor;
            }
        }

        /*
         * Work out how big we would have expected the frame to be at this Q given the
         * current correction factor. Stay in double to avoid int overflow when values
         * are large
         */
        projected_size_based_on_q = (int) (((.5
                + rate_correction_factor * vp8_bits_per_mb[cpi.common.frame_type.ordinal()][Q]) * cpi.common.MBs)
                / (1 << BPER_MB_NORMBITS));

        /* Make some allowance for cpi.zbin_over_quant */
        if (cpi.mb.zbin_over_quant > 0) {
            int Z = cpi.mb.zbin_over_quant;
            double Factor = 0.99;
            double factor_adjustment = 0.01 / 256.0;

            while (Z > 0) {
                Z--;
                projected_size_based_on_q = (int) (Factor * projected_size_based_on_q);
                Factor += factor_adjustment;

                if (Factor >= 0.999)
                    Factor = 0.999;
            }
        }

        /* Work out a size correction factor. */
        if (projected_size_based_on_q > 0) {
            correction_factor = (100 * cpi.projected_frame_size) / projected_size_based_on_q;
        }

        /*
         * More heavily damped adjustment used if we have been oscillating either side
         * of target
         */
        switch (damp_var) {
        case 0:
            adjustment_limit = 0.75;
            break;
        case 1:
            adjustment_limit = 0.375;
            break;
        case 2:
        default:
            adjustment_limit = 0.25;
            break;
        }

        if (correction_factor > 102) {
            /* We are not already at the worst allowable quality */
            correction_factor = (int) (100.5 + ((correction_factor - 100) * adjustment_limit));
            rate_correction_factor = ((rate_correction_factor * correction_factor) / 100);

            /* Keep rate_correction_factor within limits */
            if (rate_correction_factor > MAX_BPB_FACTOR) {
                rate_correction_factor = MAX_BPB_FACTOR;
            }
        } else if (correction_factor < 99) {
            /* We are not already at the best allowable quality */
            correction_factor = (int) (100.5 - ((100 - correction_factor) * adjustment_limit));
            rate_correction_factor = ((rate_correction_factor * correction_factor) / 100);

            /* Keep rate_correction_factor within limits */
            if (rate_correction_factor < MIN_BPB_FACTOR) {
                rate_correction_factor = MIN_BPB_FACTOR;
            }
        }

        if (cpi.common.frame_type == FrameType.KEY_FRAME) {
            cpi.key_frame_rate_correction_factor = rate_correction_factor;
        } else {
            if (cpi.oxcf.number_of_layers == 1 && !cpi.gf_noboost_onepass_cbr
                    && (cpi.common.refresh_alt_ref_frame || cpi.common.refresh_golden_frame)) {
                cpi.gf_rate_correction_factor = rate_correction_factor;
            } else {
                cpi.rate_correction_factor = rate_correction_factor;
            }
        }
    }

    static int estimate_keyframe_frequency(Compressor cpi) {
        int i;

        /* Average key frame frequency */
        int av_key_frame_frequency = 0;

        /*
         * First key frame at start of sequence is a special case. We have no frequency
         * data.
         */
        if (cpi.key_frame_count == 1) {
            /*
             * Assume a default of 1 kf every 2 seconds, or the max kf interval, whichever
             * is smaller.
             */
            int key_freq = cpi.oxcf.key_freq > 0 ? cpi.oxcf.key_freq : 1;
            av_key_frame_frequency = 1 + (int) cpi.output_framerate * 2;

            if (cpi.oxcf.auto_key && av_key_frame_frequency > key_freq) {
                av_key_frame_frequency = key_freq;
            }

            cpi.prior_key_frame_distance[OnyxInt.KEY_FRAME_CONTEXT - 1] = av_key_frame_frequency;
        } else {
            int total_weight = 0;
            int last_kf_interval = (cpi.frames_since_key > 0) ? cpi.frames_since_key : 1;

            /*
             * reset keyframe context and calculate weighted average of last
             * KEY_FRAME_CONTEXT keyframes
             */
            for (i = 0; i < OnyxInt.KEY_FRAME_CONTEXT; ++i) {
                if (i < OnyxInt.KEY_FRAME_CONTEXT - 1) {
                    cpi.prior_key_frame_distance[i] = cpi.prior_key_frame_distance[i + 1];
                } else {
                    cpi.prior_key_frame_distance[i] = last_kf_interval;
                }

                av_key_frame_frequency += prior_key_frame_weight[i] * cpi.prior_key_frame_distance[i];
                total_weight += prior_key_frame_weight[i];
            }

            av_key_frame_frequency /= total_weight;
        }
        // TODO (marpan): Given the checks above, |av_key_frame_frequency|
        // should always be above 0. But for now we keep the sanity check in.
        if (av_key_frame_frequency == 0)
            av_key_frame_frequency = 1;
        return av_key_frame_frequency;
    }

    static void vp8_adjust_key_frame_context(Compressor cpi) {

        /* Do we have any key frame overspend to recover? */
        if ((cpi.projected_frame_size > cpi.per_frame_bandwidth)) {
            int overspend;
            /*
             * Update the count of key frame overspend to be recovered in subsequent frames.
             * A portion of the KF overspend is treated as gf overspend (and hence recovered
             * more quickly) as the kf is also a gf. Otherwise the few frames following each
             * kf tend to get more bits allocated than those following other gfs.
             */
            overspend = (cpi.projected_frame_size - cpi.per_frame_bandwidth);

            if (cpi.oxcf.number_of_layers > 1) {
                cpi.kf_overspend_bits += overspend;
            } else {
                cpi.kf_overspend_bits += overspend * 7 / 8;
                cpi.gf_overspend_bits += overspend * 1 / 8;
            }

            /* Work out how much to try and recover per frame. */
            cpi.kf_bitrate_adjustment = cpi.kf_overspend_bits / estimate_keyframe_frequency(cpi);
        }

        cpi.frames_since_key = 0;
        cpi.key_frame_count++;
    }

}
