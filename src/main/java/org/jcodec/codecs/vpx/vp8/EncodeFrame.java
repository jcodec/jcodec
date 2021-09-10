package org.jcodec.codecs.vpx.vp8;

import java.util.Arrays;

import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.OnyxInt;
import org.jcodec.codecs.vpx.vp8.data.PickInfoReturn;
import org.jcodec.codecs.vpx.vp8.data.QuantCommon;
import org.jcodec.codecs.vpx.vp8.data.ReferenceCounts;
import org.jcodec.codecs.vpx.vp8.data.TokenExtra;
import org.jcodec.codecs.vpx.vp8.data.UsecTimer;
import org.jcodec.codecs.vpx.vp8.data.YV12buffer;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.Tuning;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;
import org.jcodec.codecs.vpx.vp8.subpixfns.BilinearPredict;
import org.jcodec.codecs.vpx.vp8.subpixfns.SixtapPredict;
import org.jcodec.codecs.vpx.vp8.subpixfns.SubPixFnCollector;

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
public class EncodeFrame {
    public static final int VP8_ACTIVITY_AVG_MIN = 64;

    static int alt_activity_measure(Compressor cpi, Macroblock x, boolean use_dc_pred) {
        return EncodeIntra.vp8_encode_intra(cpi, x, use_dc_pred);
    }

    static int mb_activity_measure(Compressor cpi, Macroblock x, int mb_row, int mb_col) {
        int mb_activity;

        boolean use_dc_pred = (mb_col != 0 || mb_row != 0) && (mb_col == 0 || mb_row == 0);

        /* Or use and alternative. */
        mb_activity = alt_activity_measure(cpi, x, use_dc_pred);

        if (mb_activity < VP8_ACTIVITY_AVG_MIN)
            mb_activity = VP8_ACTIVITY_AVG_MIN;

        return mb_activity;
    }

    /* Calculate an "average" mb activity value for the frame */
    static void calc_av_activity(Compressor cpi, long activity_sum) {
        /* Simple mean for now */
        cpi.activity_avg = (int) (activity_sum / cpi.common.MBs);

        if (cpi.activity_avg < VP8_ACTIVITY_AVG_MIN) {
            cpi.activity_avg = VP8_ACTIVITY_AVG_MIN;
        }

        /* Experimental code: return fixed value normalized for several clips */
        cpi.activity_avg = 100000;
    }

    /*
     * Loop through all MBs. Note activity of each, average activity and calculate a
     * normalized activity for each
     */
    static void build_activity_map(Compressor cpi) {
        Macroblock x = cpi.mb;
        MacroblockD xd = x.e_mbd;
        CommonData cm = cpi.common;

        YV12buffer new_yv12 = cm.yv12_fb[cm.new_fb_idx];
        int recon_y_stride = new_yv12.y_stride;

        int mb_row, mb_col;
        int mb_activity;
        long activity_sum = 0;
        xd.dst.y_buffer = new_yv12.y_buffer.shallowCopy();

        /* for each macroblock row in image */
        for (mb_row = 0; mb_row < cm.mb_rows; ++mb_row) {
            /* reset above block coeffs */
            xd.up_available = (mb_row != 0);
            xd.dst.y_buffer.incBy(mb_row * recon_y_stride * 16);
            /* for each macroblock col in image */
            for (mb_col = 0; mb_col < cm.mb_cols; ++mb_col) {
                xd.left_available = (mb_col != 0);
                /* Copy current mb to a buffer */
                CommonUtils.vp8_copy_mem16x16(x.src.y_buffer, x.src.y_stride, x.thismb, 16);

                /* measure activity */
                mb_activity = mb_activity_measure(cpi, x, mb_row, mb_col);

                /* Keep frame sum */
                activity_sum += mb_activity;

                /* Store MB level activity details. */
                x.mb_activity_ptr.setAndInc((short) mb_activity);

                /* adjust to the next column of source macroblocks */
                x.src.y_buffer.incBy(16);
                xd.dst.y_buffer.incBy(16);
            }
            xd.dst.y_buffer.incBy(16 * cm.mb_cols);

            /* adjust to the next row of mbs */
            x.src.y_buffer.incBy(16 * x.src.y_stride - 16 * cm.mb_cols);

            /* extend the recon for intra prediction */
            Extend.vp8_extend_mb_row(new_yv12, xd.dst.y_buffer.shallowCopyWithPosInc(16),
                    xd.dst.u_buffer.shallowCopyWithPosInc(8), xd.dst.v_buffer.shallowCopyWithPosInc(8));
        }

        /* Calculate an "average" MB activity */
        calc_av_activity(cpi, activity_sum);
    }

    /* Macroblock activity masking */
    static void vp8_activity_masking(Compressor cpi, Macroblock x) {
        long a;
        long b;
        long act = x.mb_activity_ptr.get();

        /* Apply the masking to the RD multiplier. */
        a = act + (2 * cpi.activity_avg);
        b = (2 * act) + cpi.activity_avg;

        x.rdmult = (int) (((long) x.rdmult * b + (a >> 1)) / a);
        x.errorperbit = x.rdmult * 100 / (110 * x.rddiv);
        x.errorperbit += (x.errorperbit == 0 ? 1 : 0);

        /* Activity based Zbin adjustment */
        adjust_act_zbin(cpi, x);
    }

    static void adjust_act_zbin(Compressor cpi, Macroblock x) {
        long a;
        long b;
        long act = x.mb_activity_ptr.get();

        /* Apply the masking to the RD multiplier. */
        a = act + 4 * cpi.activity_avg;
        b = 4 * act + cpi.activity_avg;

        if (act > cpi.activity_avg) {
            x.act_zbin_adj = (int) (((long) b + (a >> 1)) / a) - 1;
        } else {
            x.act_zbin_adj = 1 - (int) (((long) a + (b >> 1)) / b);
        }
    }

    static void sum_intra_stats(Compressor cpi, Macroblock x) {
        final ModeInfo mi = x.e_mbd.mode_info_context.get();
        final MBPredictionMode m = mi.mbmi.mode;
        final MBPredictionMode uvm = mi.mbmi.uv_mode;

        ++x.ymode_count[m.ordinal()];
        ++x.uv_mode_count[uvm.ordinal()];
    }

    static long vp8cx_encode_intra_macroblock(Compressor cpi, Macroblock x, FullAccessGenArrPointer<TokenExtra> t) {
        MacroblockD xd = x.e_mbd;
        long rate;
        ModeInfo mi = x.e_mbd.mode_info_context.get();

        if (cpi.sf.RD && cpi.compressor_speed != 2) {
            rate = RDOpt.vp8_rd_pick_intra_mode(x);
        } else {
            rate = x.interPicker.vp8_pick_intra_mode(x);
        }

        if (cpi.oxcf.tuning == Tuning.TUNE_SSIM) {
            adjust_act_zbin(cpi, x);
            Quantize.vp8_update_zbin_extra(cpi, x);
        }

        if (mi.mbmi.mode == MBPredictionMode.B_PRED) {
            EncodeIntra.vp8_encode_intra4x4mby(x);
        } else {
            EncodeIntra.vp8_encode_intra16x16mby(x);
        }

        EncodeIntra.vp8_encode_intra16x16mbuv(x);

        sum_intra_stats(cpi, x);

        Tokenize.vp8_tokenize_mb(cpi, x, t);

        if (mi.mbmi.mode != MBPredictionMode.B_PRED)
            InvTrans.vp8_inverse_transform_mby(xd);

        IDCTBlk.vp8_dequant_idct_add_uv_block(xd.qcoeff.shallowCopyWithPosInc(16 * 16), xd.dequant_uv, xd.dst.u_buffer,
                xd.dst.v_buffer, xd.dst.uv_stride, xd.eobs.shallowCopyWithPosInc(16));
        return rate;
    }

    static int vp8cx_encode_inter_macroblock(Compressor cpi, Macroblock x, FullAccessGenArrPointer<TokenExtra> t,
            int recon_yoffset, int recon_uvoffset, int mb_row, int mb_col) {
        MacroblockD xd = x.e_mbd;
        PickInfoReturn retDetails = new PickInfoReturn();
        x.skip = false;
        ModeInfo mi = xd.mode_info_context.get();

        if (xd.segmentation_enabled != 0) {
            x.encode_breakout = cpi.segment_encode_breakout[mi.mbmi.segment_id];
        } else {
            x.encode_breakout = cpi.oxcf.encode_breakout;
        }

        if (cpi.sf.RD) {
            boolean zbin_mode_boost_enabled = x.zbin_mode_boost_enabled;

            /* Are we using the fast quantizer for the mode selection? */
            if (cpi.sf.use_fastquant_for_pick) {
                x.quantize_b = Quantize.fastQuant;

                /*
                 * the fast quantizer does not use zbin_extra, so do not recalculate
                 */
                x.zbin_mode_boost_enabled = false;
            }

            RDOpt.vp8_rd_pick_inter_mode(cpi, x, recon_yoffset, recon_uvoffset, retDetails, mb_row, mb_col);
            /* switch back to the regular quantizer for the encode */
            if (cpi.sf.improved_quant) {
                x.quantize_b = Quantize.regularQuant;
            }

            /* restore cpi.zbin_mode_boost_enabled */
            x.zbin_mode_boost_enabled = zbin_mode_boost_enabled;

        } else {

            x.interPicker.pickInterMode(cpi, x, recon_yoffset, recon_uvoffset, retDetails, mb_row, mb_col);
        }

        x.prediction_error += retDetails.distortion;
        x.intra_error += x.skip ? 0 : retDetails.intra;

        if (cpi.oxcf.tuning == Tuning.TUNE_SSIM) {
            /* Adjust the zbin based on this MB rate. */
            adjust_act_zbin(cpi, x);
        }

        /* MB level adjutment to quantizer setup */
        if (xd.segmentation_enabled != 0) {
            /* If cyclic update enabled */
            if (cpi.current_layer == 0 && cpi.cyclic_refresh_mode_enabled) {
                /* Clear segment_id back to 0 if not coded (last frame 0,0) */
                if ((mi.mbmi.segment_id == 1) && ((mi.mbmi.ref_frame != MVReferenceFrame.LAST_FRAME)
                        || (mi.mbmi.mode != MBPredictionMode.ZEROMV))) {
                    mi.mbmi.segment_id = 0;

                    /* segment_id changed, so update */
                    Quantize.vp8cx_mb_init_quantizer(cpi, x, true);
                }
            }
        }

        {
            /*
             * Experimental code. Special case for gf and arf zeromv modes, for 1 temporal
             * layer. Increase zbin size to supress noise.
             */
            x.zbin_mode_boost = 0;
            if (x.zbin_mode_boost_enabled) {
                if (mi.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
                    if (mi.mbmi.mode == MBPredictionMode.ZEROMV) {
                        if (mi.mbmi.ref_frame != MVReferenceFrame.LAST_FRAME && cpi.oxcf.number_of_layers == 1) {
                            x.zbin_mode_boost = OnyxInt.GF_ZEROMV_ZBIN_BOOST;
                        } else {
                            x.zbin_mode_boost = OnyxInt.LF_ZEROMV_ZBIN_BOOST;
                        }
                    } else if (mi.mbmi.mode == MBPredictionMode.SPLITMV) {
                        x.zbin_mode_boost = 0;
                    } else {
                        x.zbin_mode_boost = OnyxInt.MV_ZBIN_BOOST;
                    }
                }
            }

            /*
             * The fast quantizer doesn't use zbin_extra, only do so with the regular
             * quantizer.
             */
            if (cpi.sf.improved_quant)
                Quantize.vp8_update_zbin_extra(cpi, x);
        }

        {
            MVReferenceFrame rf = mi.mbmi.ref_frame;
            x.count_mb_ref_frame_usage.put(rf, x.count_mb_ref_frame_usage.get(rf) + 1);
        }

        if (mi.mbmi.ref_frame == MVReferenceFrame.INTRA_FRAME) {
            EncodeIntra.vp8_encode_intra16x16mbuv(x);

            if (mi.mbmi.mode == MBPredictionMode.B_PRED) {
                EncodeIntra.vp8_encode_intra4x4mby(x);
            } else {
                EncodeIntra.vp8_encode_intra16x16mby(x);
            }

            sum_intra_stats(cpi, x);
        } else {
            int ref_fb_idx;
            ref_fb_idx = cpi.common.frameIdxs.get(mi.mbmi.ref_frame);

            xd.pre.y_buffer = cpi.common.yv12_fb[ref_fb_idx].y_buffer.shallowCopyWithPosInc(recon_yoffset);
            xd.pre.u_buffer = cpi.common.yv12_fb[ref_fb_idx].u_buffer.shallowCopyWithPosInc(recon_uvoffset);
            xd.pre.v_buffer = cpi.common.yv12_fb[ref_fb_idx].v_buffer.shallowCopyWithPosInc(recon_uvoffset);

            if (!x.skip) {
                EncodeMB.vp8_encode_inter16x16(x);
            } else {
                ReconInter.vp8_build_inter16x16_predictors_mb(xd, xd.dst.y_buffer, xd.dst.u_buffer, xd.dst.v_buffer,
                        xd.dst.y_stride, xd.dst.uv_stride);
            }
        }

        if (!x.skip) {

            Tokenize.vp8_tokenize_mb(cpi, x, t);

            if (mi.mbmi.mode != MBPredictionMode.B_PRED) {
                InvTrans.vp8_inverse_transform_mby(xd);
            }

            IDCTBlk.vp8_dequant_idct_add_uv_block(xd.qcoeff.shallowCopyWithPosInc(16 * 16), xd.dequant_uv,
                    xd.dst.u_buffer, xd.dst.v_buffer, xd.dst.uv_stride, xd.eobs.shallowCopyWithPosInc(16));
        } else {
            /* always set mb_skip_coeff as it is needed by the loopfilter */
            mi.mbmi.mb_skip_coeff = true;

            if (cpi.common.mb_no_coeff_skip) {
                x.skip_true_count++;
                Tokenize.vp8_fix_contexts(xd);
            } else {
                Tokenize.vp8_stuff_mb(cpi, x, t);
            }
        }

        return retDetails.rate;
    }

    static void encode_mb_row(Compressor cpi, CommonData cm, int mb_row, Macroblock x, MacroblockD xd,
            FullAccessGenArrPointer<TokenExtra> tp, int[] segment_counts, long[] totalrate) {
        int recon_yoffset, recon_uvoffset;
        int mb_col;
        int ref_fb_idx = cm.frameIdxs.get(MVReferenceFrame.LAST_FRAME);
        int dst_fb_idx = cm.new_fb_idx;
        int recon_y_stride = cm.yv12_fb[ref_fb_idx].y_stride;
        int recon_uv_stride = cm.yv12_fb[ref_fb_idx].uv_stride;
        int map_index = (mb_row * cpi.common.mb_cols);

        /* reset above block coeffs */
        xd.above_context = cm.above_context.shallowCopy();

        xd.up_available = (mb_row != 0);
        recon_yoffset = (mb_row * recon_y_stride * 16);
        recon_uvoffset = (mb_row * recon_uv_stride * 8);
        xd.dst.y_buffer = cm.yv12_fb[dst_fb_idx].y_buffer.shallowCopyWithPosInc(recon_yoffset);
        xd.dst.u_buffer = cm.yv12_fb[dst_fb_idx].u_buffer.shallowCopyWithPosInc(recon_uvoffset);
        xd.dst.v_buffer = cm.yv12_fb[dst_fb_idx].v_buffer.shallowCopyWithPosInc(recon_uvoffset);

        cpi.tplist[mb_row].start = tp.shallowCopy();

        /*
         * Distance of Mb to the top & bottom edges, specified in 1/8th pel units as
         * they are always compared to values that are in 1/8th pel
         */
        xd.mb_to_top_edge = -((mb_row * 16) << 3);
        xd.mb_to_bottom_edge = ((cm.mb_rows - 1 - mb_row) * 16) << 3;

        /*
         * Set up limit values for vertical motion vector components to prevent them
         * extending beyond the UMV borders
         */
        x.mv_row_min = (short) (-((mb_row * 16) + (YV12buffer.VP8BORDERINPIXELS - 16)));
        x.mv_row_max = (short) (((cm.mb_rows - 1 - mb_row) * 16) + (YV12buffer.VP8BORDERINPIXELS - 16));

        /* Set the mb activity pointer to the start of the row. */
        x.mb_activity_ptr = cpi.mb_activity_map.shallowCopyWithPosInc(map_index);

        /* for each macroblock col in image */
        for (mb_col = 0; mb_col < cm.mb_cols; ++mb_col) {
            ModeInfo mi = xd.mode_info_context.get();
            /*
             * Distance of Mb to the left & right edges, specified in 1/8th pel units as
             * they are always compared to values that are in 1/8th pel units
             */
            xd.mb_to_left_edge = -((mb_col * 16) << 3);
            xd.mb_to_right_edge = ((cm.mb_cols - 1 - mb_col) * 16) << 3;

            /*
             * Set up limit values for horizontal motion vector components to prevent them
             * extending beyond the UMV borders
             */
            x.mv_col_min = (short) (-((mb_col * 16) + (YV12buffer.VP8BORDERINPIXELS - 16)));
            x.mv_col_max = (short) (((cm.mb_cols - 1 - mb_col) * 16) + (YV12buffer.VP8BORDERINPIXELS - 16));

            xd.left_available = (mb_col != 0);

            x.rddiv = cpi.RDDIV;
            x.rdmult = cpi.RDMULT;

            /* Copy current mb to a buffer */
            CommonUtils.vp8_copy_mem16x16(x.src.y_buffer, x.src.y_stride, x.thismb, 16);

            if (cpi.oxcf.tuning == Tuning.TUNE_SSIM)
                vp8_activity_masking(cpi, x);

            /* Is segmentation enabled */
            /* MB level adjustment to quantizer */
            if (xd.segmentation_enabled != 0) {
                /*
                 * Code to set segment id in xd.mbmi.segment_id for current MB (with range
                 * checking)
                 */
                if (cpi.segmentation_map[map_index + mb_col] <= 3) {
                    mi.mbmi.segment_id = cpi.segmentation_map[map_index + mb_col];
                } else {
                    mi.mbmi.segment_id = 0;
                }

                Quantize.vp8cx_mb_init_quantizer(cpi, x, true);
            } else {
                /* Set to Segment 0 by default */
                mi.mbmi.segment_id = 0;
            }

            x.active_ptr = cpi.active_map.shallowCopyWithPosInc(map_index + mb_col);
            if (cm.frame_type == FrameType.KEY_FRAME) {
                totalrate[0] += vp8cx_encode_intra_macroblock(cpi, x, tp);
            } else {
                totalrate[0] += vp8cx_encode_inter_macroblock(cpi, x, tp, recon_yoffset, recon_uvoffset, mb_row,
                        mb_col);

// Keep track of how many (consecutive) times a  block is coded
// as ZEROMV_LASTREF, for base layer frames.
// Reset to 0 if its coded as anything else.
                if (cpi.current_layer == 0) {
                    if (mi.mbmi.mode == MBPredictionMode.ZEROMV && mi.mbmi.ref_frame == MVReferenceFrame.LAST_FRAME) {
// Increment, check for wrap-around.
                        if (cpi.consec_zero_last[map_index + mb_col] < 255) {
                            cpi.consec_zero_last[map_index + mb_col] += 1;
                        }
                        if (cpi.consec_zero_last_mvbias[map_index + mb_col] < 255) {
                            cpi.consec_zero_last_mvbias[map_index + mb_col] += 1;
                        }
                    } else {
                        cpi.consec_zero_last[map_index + mb_col] = 0;
                        cpi.consec_zero_last_mvbias[map_index + mb_col] = 0;
                    }
                    if (x.zero_last_dot_suppress) {
                        cpi.consec_zero_last_mvbias[map_index + mb_col] = 0;
                    }
                }

                /*
                 * Special case code for cyclic refresh If cyclic update enabled then copy
                 * xd.mbmi.segment_id; (which may have been updated based on mode during
                 * vp8cx_encode_inter_macroblock()) back into the global segmentation map
                 */
                if ((cpi.current_layer == 0) && (cpi.cyclic_refresh_mode_enabled && xd.segmentation_enabled != 0)) {
                    cpi.segmentation_map[map_index + mb_col] = mi.mbmi.segment_id;

                    /*
                     * If the block has been refreshed mark it as clean (the magnitude of the -ve
                     * influences how long it will be before we consider another refresh): Else if
                     * it was coded (last frame 0,0) and has not already been refreshed then mark it
                     * as a candidate for cleanup next time (marked 0) else mark it as dirty (1).
                     */
                    if (mi.mbmi.segment_id != 0) {
                        cpi.cyclic_refresh_map[map_index + mb_col] = -1;
                    } else if ((mi.mbmi.mode == MBPredictionMode.ZEROMV)
                            && (mi.mbmi.ref_frame == MVReferenceFrame.LAST_FRAME)) {
                        if (cpi.cyclic_refresh_map[map_index + mb_col] == 1) {
                            cpi.cyclic_refresh_map[map_index + mb_col] = 0;
                        }
                    } else {
                        cpi.cyclic_refresh_map[map_index + mb_col] = 1;
                    }
                }
            }

            cpi.tplist[mb_row].stop = tp.shallowCopy();

            /* Increment pointer into gf usage flags structure. */
            x.gf_active_ptr.inc();

            /* Increment the activity mask pointers. */
            x.mb_activity_ptr.inc();

            /* adjust to the next column of macroblocks */
            x.src.y_buffer.incBy(16);
            x.src.u_buffer.incBy(8);
            x.src.v_buffer.incBy(8);

            recon_yoffset += 16;
            recon_uvoffset += 8;

            xd.dst.y_buffer.incBy(16);
            xd.dst.u_buffer.incBy(8);
            xd.dst.v_buffer.incBy(8);

            /* Keep track of segment usage */
            segment_counts[mi.mbmi.segment_id]++;

            /* skip to next mb */
            xd.mode_info_context.inc();
            x.partition_info.inc();
            xd.above_context.inc();
        }

        /* extend the recon for intra prediction */
        Extend.vp8_extend_mb_row(cm.yv12_fb[dst_fb_idx], xd.dst.y_buffer, xd.dst.u_buffer, xd.dst.v_buffer);

        /* this is to account for the border */
        xd.mode_info_context.inc();
        x.partition_info.inc();
    }

    static void init_encode_frame_mb_context(Compressor cpi) {
        cpi.mb.e_mbd.init_encode_frame_mbd_context(cpi);
        cpi.mb.init_encode_frame_mb_context(cpi);
        CommonData cm = cpi.common;
        /* reset intra mode contexts */
        if (cm.frame_type == FrameType.KEY_FRAME)
            cm.fc.vp8_init_mbmode_probs();

        /* set up frame for intra coded blocks */
        cm.yv12_fb[cm.new_fb_idx].vp8_setup_intra_recon();

        for (int kk = 0; kk < cm.mb_cols; kk++) {
            cm.above_context.getRel(kk).reset();
        }
    }

    static void vp8_encode_frame(Compressor cpi) {
        int mb_row;
        Macroblock x = cpi.mb;
        CommonData cm = cpi.common;
        MacroblockD xd = x.e_mbd;
        FullAccessGenArrPointer<TokenExtra> tp = cpi.tok.shallowCopy();
        int[] segment_counts = new int[xd.segmentation_enabled != 0 ? BlockD.MAX_MB_SEGMENTS : 1];
        long[] totalrate = new long[1];
        CommonUtils.vp8_zero(segment_counts);
        if (cpi.compressor_speed == 2) {
            if (cpi.oxcf.getCpu_used() < 0) {
                cpi.Speed = -(cpi.oxcf.getCpu_used());
            } else {
                RDOpt.vp8_auto_select_speed(cpi);
            }
        }

        /* Functions setup for all frame types so we can use MC in AltRef */
        SubPixFnCollector spfncollector = cm.use_bilinear_mc_filter ? BilinearPredict.bilinear : SixtapPredict.sixtap;

        xd.subpixel_predict = spfncollector.get4x4();
        xd.subpixel_predict8x4 = spfncollector.get8x4();
        xd.subpixel_predict8x8 = spfncollector.get8x8();
        xd.subpixel_predict16x16 = spfncollector.get16x16();

        cpi.mb.skip_true_count = 0;
        cpi.tok_count = 0;

        xd.mode_info_context = cm.mi.shallowCopy();

        CommonUtils.vp8_zero(cpi.mb.MVcount);

        Quantize.vp8cx_frame_init_quantizer(cpi);

        RDOpt.vp8_initialize_rd_consts(cpi, x,
                QuantCommon.doLookup(cm, CommonData.Quant.Y1, CommonData.Comp.DC, cm.base_qindex));

        x.vp8cx_initialize_me_consts(cm.base_qindex);

        if (cpi.oxcf.tuning == Tuning.TUNE_SSIM) {
            /* Initialize encode frame context. */
            init_encode_frame_mb_context(cpi);

            /* Build a frame level activity map */
            build_activity_map(cpi);
        }

        /* re-init encode frame context. */
        init_encode_frame_mb_context(cpi);

        {
            UsecTimer emr_timer = new UsecTimer();
            emr_timer.timerStart();
            {

                /* for each macroblock row in image */
                for (mb_row = 0; mb_row < cm.mb_rows; ++mb_row) {
                    CommonUtils.vp8_zero(cm.left_context.panes);
                    encode_mb_row(cpi, cm, mb_row, x, xd, tp, segment_counts, totalrate);

                    /* adjust to the next row of mbs */
                    x.src.y_buffer.incBy(16 * x.src.y_stride - 16 * cm.mb_cols);
                    x.src.u_buffer.incBy(8 * x.src.uv_stride - 8 * cm.mb_cols);
                    x.src.v_buffer.incBy(8 * x.src.uv_stride - 8 * cm.mb_cols);
                }

                cpi.tok_count = cpi.tok.pointerDiff(tp);
            }

            emr_timer.mark();
            cpi.time_encode_mb_row += emr_timer.elapsed();
        }

        // Work out the segment probabilities if segmentation is enabled
        // and needs to be updated
        if (xd.segmentation_enabled != 0 && xd.update_mb_segmentation_map) {
            int tot_count;
            int i;

            /* Set to defaults */
            Arrays.fill(xd.mb_segment_tree_probs, 255);

            tot_count = segment_counts[0] + segment_counts[1] + segment_counts[2] + segment_counts[3];

            if (tot_count != 0) {
                xd.mb_segment_tree_probs[0] = ((segment_counts[0] + segment_counts[1]) * 255) / tot_count;

                tot_count = segment_counts[0] + segment_counts[1];

                if (tot_count > 0) {
                    xd.mb_segment_tree_probs[1] = (segment_counts[0] * 255) / tot_count;
                }

                tot_count = segment_counts[2] + segment_counts[3];

                if (tot_count > 0) {
                    xd.mb_segment_tree_probs[2] = (segment_counts[2] * 255) / tot_count;
                }

                /* Zero probabilities not allowed */
                for (i = 0; i < BlockD.MB_FEATURE_TREE_PROBS; ++i) {
                    if (xd.mb_segment_tree_probs[i] == 0)
                        xd.mb_segment_tree_probs[i] = 1;
                }
            }
        }

        /* projected_frame_size in units of BYTES */
        cpi.projected_frame_size = (int) (totalrate[0] >> 8);

        /* Make a note of the percentage MBs coded Intra. */
        if (cm.frame_type == FrameType.KEY_FRAME) {
            cpi.this_frame_percent_intra = 100;
        } else {
            final ReferenceCounts rf = cpi.mb.sumReferenceCounts();
            if (rf.total != 0) {
                cpi.this_frame_percent_intra = rf.intra * 100 / rf.total;
            }
        }

        /*
         * Adjust the projected reference frame usage probability numbers to reflect
         * what we have just seen. This may be useful when we make multiple iterations
         * of the recode loop rather than continuing to use values from the previous
         * frame.
         */
        if ((cm.frame_type != FrameType.KEY_FRAME)
                && ((cpi.oxcf.number_of_layers > 1) || (!cm.refresh_alt_ref_frame && !cm.refresh_golden_frame))) {
            BitStream.vp8_convert_rfct_to_prob(cpi);
        }
    }
}
