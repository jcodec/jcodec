package org.jcodec.codecs.vpx.vp8;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.EntropyMV;
import org.jcodec.codecs.vpx.vp8.data.MBModeInfo;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.OnyxInt;
import org.jcodec.codecs.vpx.vp8.data.PickInfoReturn;
import org.jcodec.codecs.vpx.vp8.data.QualityMetrics;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.VarWithNum;
import org.jcodec.codecs.vpx.vp8.data.VarianceFNs;
import org.jcodec.codecs.vpx.vp8.data.VarianceResults;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.BlockEnum;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.SearchMethods;
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
public class PickInter {

    public static int[] nearsaddx_proto = { 0, 1, 2, 3, 4, 5, 6, 7 };

    final public MV[] best_ref_mv_sb = { new MV(), new MV() };
    final public MV[][] mode_mv_sb = new MV[2][MBPredictionMode.count];
    final public MV best_ref_mv = new MV();
    public MV[] mode_mv;
    final public MV mvp = new MV();
    final EnumMap<MBPredictionMode, int[]> pred_error = new EnumMap<MBPredictionMode, int[]>(MBPredictionMode.class);
    final int[] mdCounts = new int[4];
    final int[] nearsad = new int[nearsaddx_proto.length];

    public PickInter() {
        for (int i = 0; i < mode_mv_sb.length; i++) {
            for (int j = 0; j < mode_mv_sb[i].length; j++) {
                mode_mv_sb[i][j] = new MV();
            }
        }
        for (MBPredictionMode m : MBPredictionMode.nonBlockPred)
            pred_error.put(m, new int[1]);
    }

    public void reset() {
        for (int i = 0; i < best_ref_mv_sb.length; i++) {
            best_ref_mv_sb[i].setZero();
        }
        for (int i = 0; i < mode_mv_sb.length; i++) {
            for (int j = 0; j < mode_mv_sb[i].length; j++) {
                mode_mv_sb[i][j].setZero();
            }
        }
        best_ref_mv.setZero();
        mvp.setZero();
        CommonUtils.vp8_zero(nearsad);
        CommonUtils.vp8_copy(nearsaddx_proto, nearsad);
        for (MBPredictionMode m : MBPredictionMode.nonBlockPred)
            pred_error.get(m)[0] = 0;
    }

    static int macroblock_corner_grad(ReadOnlyIntArrPointer signal, int stride, int offsetx, int offsety, int sgnx,
            int sgny) {
        final int baseStride = offsetx * stride;
        final int sgnExtStride = baseStride + sgnx * stride;
        final int sgnYExty = offsety + sgny;
        final int y1 = signal.getRel(baseStride + offsety);
        final int y2 = signal.getRel(baseStride + sgnYExty);
        final int y3 = signal.getRel(sgnExtStride + offsety);
        final int y4 = signal.getRel(sgnExtStride + sgnYExty);
        return Math.max(Math.max(Math.abs(y1 - y2), Math.abs(y1 - y3)), Math.abs(y1 - y4));
    }

    static void update_mvcount(Macroblock x, MV best_ref_mv) {
        /*
         * Split MV modes currently not supported when RD is nopt enabled, therefore,
         * only need to modify MVcount in NEWMV mode.
         */
        MBModeInfo mbmi = x.e_mbd.mode_info_context.get().mbmi;
        if (mbmi.mode == MBPredictionMode.NEWMV) {
            MV mv = mbmi.mv;
            MV bmv = best_ref_mv;
            x.MVcount[0][EntropyMV.mv_max + ((mv.row - bmv.row) >> 1)]++;
            x.MVcount[1][EntropyMV.mv_max + ((mv.col - bmv.col) >> 1)]++;
        }
    }

    public void pick_intra_mbuv_mode(final Macroblock mb) {
        final MacroblockD x = mb.e_mbd;
        final FullAccessIntArrPointer usrc_ptr = mb.block.getRel(16).getSrcPtr();
        final FullAccessIntArrPointer vsrc_ptr = mb.block.getRel(20).getSrcPtr();
        final int uvsrc_stride = mb.block.getRel(16).src_stride;
        final int utop_left = x.dst.u_buffer.getRel(-x.dst.uv_stride - 1);
        final int vtop_left = x.dst.v_buffer.getRel(-x.dst.uv_stride - 1);
        int expected_udc;
        int expected_vdc;
        reset();

        if (!x.up_available && !x.left_available) {
            expected_udc = 128;
            expected_vdc = 128;
        } else {
            int shift = 2;
            int uaverage = 0, vaverage = 0;

            if (x.up_available) {
                for (int i = 0, relI = -x.dst.uv_stride; i < 8; ++i, relI++) {
                    uaverage += x.dst.u_buffer.getRel(relI);
                    vaverage += x.dst.v_buffer.getRel(relI);
                }

                shift++;
            }

            if (x.left_available) {
                for (int i = 0; i < 8; ++i) {
                    uaverage += x.dst.u_buffer.getRel(i * x.dst.uv_stride - 1);
                    vaverage += x.dst.v_buffer.getRel(i * x.dst.uv_stride - 1);
                }

                shift++;
            }

            expected_udc = CommonUtils.roundPowerOfTwo(uaverage, shift);
            expected_vdc = CommonUtils.roundPowerOfTwo(vaverage, shift);
        }

        for (int i = 0; i < 8; ++i) {
            for (int j = 0, relJ = -x.dst.uv_stride; j < 8; j++, relJ++) {
                final short uab = x.dst.u_buffer.getRel(relJ);
                final short vab = x.dst.v_buffer.getRel(relJ);
                final short ule = x.dst.u_buffer.getRel(i * x.dst.uv_stride - 1);
                final short vle = x.dst.v_buffer.getRel(i * x.dst.uv_stride - 1);
                final short predu = CommonUtils.clipPixel((short) (ule + uab - utop_left));
                final short predv = CommonUtils.clipPixel((short) (vle + vab - vtop_left));
                final short u_p = usrc_ptr.getRel(j);
                final short v_p = vsrc_ptr.getRel(j);

                short diffU = (short) (u_p - expected_udc);
                short diffV = (short) (v_p - expected_vdc);
                pred_error.get(MBPredictionMode.DC_PRED)[0] += diffU * diffU + diffV * diffV;

                diffU = (short) (u_p - uab);
                diffV = (short) (v_p - vab);
                pred_error.get(MBPredictionMode.V_PRED)[0] += diffU * diffU + diffV * diffV;

                diffU = (short) (u_p - ule);
                diffV = (short) (v_p - vle);
                pred_error.get(MBPredictionMode.H_PRED)[0] += diffU * diffU + diffV * diffV;

                diffU = (short) (u_p - predu);
                diffV = (short) (v_p - predv);
                pred_error.get(MBPredictionMode.TM_PRED)[0] += diffU * diffU + diffV * diffV;
            }

            usrc_ptr.incBy(uvsrc_stride);
            vsrc_ptr.incBy(uvsrc_stride);

            if (i == 3) {
                final Block ublk = mb.block.getRel(18);
                final Block vblk = mb.block.getRel(22);
                usrc_ptr.setPos(ublk.base_src.getPos() + ublk.src);
                vsrc_ptr.setPos(vblk.base_src.getPos() + vblk.src);
            }
        }

        int best_error = Integer.MAX_VALUE;
        MBPredictionMode best_mode = null;
        for (MBPredictionMode pm : MBPredictionMode.nonBlockPred) {
            final int pe = pred_error.get(pm)[0];
            if (best_error > pe) {
                best_error = pe;
                best_mode = pm;
            }
        }

        assert (best_mode != null);
        mb.e_mbd.mode_info_context.get().mbmi.uv_mode = best_mode;
    }

    static int get_prediction_error(Block be, BlockD b) {
        return Variance.vpx_get4x4sse_cs(be.getSrcPtr(), be.src_stride, b.predictor, 16);
    }

    static void pick_intra4x4block(Macroblock x, BPredictionMode[] bestmode, int ib,
            final EnumMap<BPredictionMode, Integer> mode_costs, QualityMetrics best) {
        BlockD b = x.e_mbd.block.getRel(ib);
        Block be = x.block.getRel(ib);
        int dst_stride = x.e_mbd.dst.y_stride;
        best.error = Long.MAX_VALUE;

        FullAccessIntArrPointer Above = b.getOffsetPointer(x.e_mbd.dst.y_buffer).shallowCopyWithPosInc(-dst_stride);
        FullAccessIntArrPointer yleft = b.getOffsetPointer(x.e_mbd.dst.y_buffer).shallowCopyWithPosInc(-1);
        final short top_left = Above.getRel(-1);

        for (BPredictionMode mode : BPredictionMode.basicbmodes) {
            long this_rd;

            int rate = mode_costs.get(mode);

            x.recon.vp8_intra4x4_predict(Above, yleft, dst_stride, mode, b.predictor, 16, top_left);
            long distortion = get_prediction_error(be, b);
            this_rd = RDOpt.RDCOST(x.rdmult, x.rddiv, rate, distortion);

            if (this_rd < best.error) {
                best.rateBase = rate;
                best.distortion = distortion;
                best.error = this_rd;
                bestmode[0] = mode;
            }
        }

        b.bmi.as_mode(bestmode[0]);
        EncodeIntra.vp8_encode_intra4x4block(x, ib);
    }

    static void pick_intra4x4mby_modes(Macroblock mb, QualityMetrics best) {
        final MacroblockD xd = mb.e_mbd;
        int i;
        int cost = mb.mbmode_cost.get(xd.frame_type).get(MBPredictionMode.B_PRED);
        long distortion = 0;
        EnumMap<BPredictionMode, Integer> bmode_costs;

        ReconIntra.intra_prediction_down_copy(xd);

        bmode_costs = mb.inter_bmode_costs;
        QualityMetrics curr = new QualityMetrics();
        for (i = 0; i < 16; ++i) {
            FullAccessGenArrPointer<ModeInfo> mic = xd.mode_info_context;
            final int mis = xd.mode_info_stride;
            BPredictionMode[] best_mode = new BPredictionMode[1];
            best_mode[0] = null;

            if (mb.e_mbd.frame_type == FrameType.KEY_FRAME) {
                final BPredictionMode A = FindNearMV.above_block_mode(mic, i, mis);
                final BPredictionMode L = FindNearMV.left_block_mode(mic, i);

                bmode_costs = mb.bmode_costs.get(A).get(L);
            }

            pick_intra4x4block(mb, best_mode, i, bmode_costs, curr);

            cost += curr.rateBase;
            distortion += curr.distortion;
            assert (best_mode[0] != null);

            mic.get().bmi[i].as_mode(best_mode[0]);

            /*
             * Break out case where we have already exceeded best so far value that was
             * passed in
             */
            if (distortion > best.distortion)
                break;
        }

        best.rateBase = cost;

        if (i == 16) {
            best.distortion = distortion;
            best.error = RDOpt.RDCOST(mb.rdmult, mb.rddiv, cost, distortion);
        } else {
            best.distortion = Integer.MAX_VALUE;
            best.error = Integer.MAX_VALUE;
        }
    }

    private static boolean dotArtifactHelper(Macroblock x, ReadOnlyIntArrPointer target_last, int stride,
            ReadOnlyIntArrPointer last_ref, int shiftx, int shifty, int signx, int signy) {
        final int threshold1 = 6;
        final int threshold2 = 3;
        int grad_last = macroblock_corner_grad(last_ref, stride, shiftx, shifty, signx, signy);
        int grad_source = macroblock_corner_grad(target_last, stride, shiftx, shifty, signx, signy);
        if (grad_last >= threshold1 && grad_source <= threshold2) {
            x.mbs_zero_last_dot_suppress++;
            return true;
        }
        return false;
    }

    static boolean check_dot_artifact_candidate(Compressor cpi, Macroblock x, ReadOnlyIntArrPointer target_last,
            int stride, ReadOnlyIntArrPointer last_ref, int mb_row, int mb_col, int channel) {
        int max_num = (cpi.common.MBs) / 10;
        int index = mb_row * cpi.common.mb_cols + mb_col;
// Threshold for #consecutive (base layer) frames using zero_last mode.
        int num_frames = 30;
        int shift = 15;
        if (channel > 0) {
            shift = 7;
        }
        if (cpi.oxcf.number_of_layers > 1) {
            num_frames = 20;
        }
        x.zero_last_dot_suppress = false;
// Blocks on base layer frames that have been using ZEROMV_LAST repeatedly
// (i.e, at least |x| consecutive frames are candidates for increasing the
// rd adjustment for zero_last mode.
// Only allow this for at most |max_num| blocks per frame.
// Don't allow this for screen content input.
        if (cpi.current_layer == 0 && cpi.consec_zero_last_mvbias[index] > num_frames
                && x.mbs_zero_last_dot_suppress < max_num && cpi.oxcf.screen_content_mode == 0) {
// If this block is checked here, label it so we don't check it again until
// ~|x| framaes later.
            x.zero_last_dot_suppress = true;
// Dot artifact is noticeable as strong gradient at corners of macroblock,
// for flat areas. As a simple detector for now, we look for a high
// corner gradient on last ref, and a smaller gradient on source.
// Check 4 corners, return if any satisfy condition.
            return /* Top-left */ dotArtifactHelper(x, target_last, stride, last_ref, 0, 0, 1, 1) ||
            /* Top-right */ dotArtifactHelper(x, target_last, stride, last_ref, 0, shift, 1, -1) ||
            /* Bottom-left */ dotArtifactHelper(x, target_last, stride, last_ref, shift, 0, -1, 1) ||
            /* Bottom-right */ dotArtifactHelper(x, target_last, stride, last_ref, shift, shift, -1, -1);

        }
        return false;
    }

    private static int updLocalMC(ModeInfo mi, int local_motion_check) {
        if (mi.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
            MV r = mi.mbmi.mv;
            if (Math.abs(r.row) < 8 && Math.abs(r.col) < 8) {
                local_motion_check++;
            }
        }
        return local_motion_check;
    }

    static int calculate_zeromv_rd_adjustment(Compressor cpi, Macroblock x, int rd_adjustment) {
        FullAccessGenArrPointer<ModeInfo> mic = new FullAccessGenArrPointer<ModeInfo>(x.e_mbd.mode_info_context);
        int local_motion_check = 0;
        if (cpi.lf_zeromv_pct > 40) {
            /* left mb */
            mic.dec();
            local_motion_check = updLocalMC(mic.get(), local_motion_check);

            /* above-left mb */
            mic.incBy(-x.e_mbd.mode_info_stride);
            local_motion_check = updLocalMC(mic.get(), local_motion_check);

            /* above mb */
            mic.inc();
            local_motion_check = updLocalMC(mic.get(), local_motion_check);

            if (((x.e_mbd.mb_to_top_edge == 0 || x.e_mbd.mb_to_left_edge == 0) && local_motion_check > 0)
                    || local_motion_check > 2) {
                return 80;
            } else if (local_motion_check > 0) {
                return 90;
            }
        }
        return rd_adjustment;
    }

    static void vp8_get_inter_mbpred_error(Macroblock mb, VarianceFNs vfp, VarianceResults ret, MV this_mv) {
        final Block b = mb.block.get();
        final BlockD d = mb.e_mbd.block.get();
        final FullAccessIntArrPointer what = b.getSrcPtr();
        final int what_stride = b.src_stride;
        final int pre_stride = mb.e_mbd.pre.y_stride;
        final FullAccessIntArrPointer in_what = d.getOffsetPointer(mb.e_mbd.pre.y_buffer)
                .shallowCopyWithPosInc((this_mv.row >> 3) * pre_stride + (this_mv.col >> 3));
        final int xoffset = this_mv.col & 7;
        final int yoffset = this_mv.row & 7;

        if ((xoffset | yoffset) != 0) {
            vfp.svf.call(in_what, pre_stride, xoffset, yoffset, what, what_stride, ret);
        } else {
            vfp.vf.call(what, what_stride, in_what, pre_stride, ret);
        }
    }

    static void check_for_encode_breakout(final VarianceResults vr, final Compressor cpi) {
        final Macroblock x = cpi.mb;
        final MacroblockD xd = x.e_mbd;

        final int threshold = Math.max(x.encode_breakout,
                (xd.block.get().dequant.getRel(1) * xd.block.get().dequant.getRel(1) >> 4));

        if (vr.sse < threshold) {
            /* Check u and v to make sure skip is ok */
            if ((RDOpt.VP8_UVSSE(cpi) << 1) < x.encode_breakout) {
                x.skip = true;
            } else {
                x.skip = false;
            }
        }
    }

    static long evaluate_inter_mode(VarianceResults ret, int rate2, Compressor cpi, Macroblock x, int rd_adj) {
        ModeInfo currentMI = x.e_mbd.mode_info_context.get();
        MBPredictionMode this_mode = currentMI.mbmi.mode;
        MV mv = currentMI.mbmi.mv;
        long this_rd;
        /*
         * Exit early and don't compute the distortion if this macroblock is marked
         * inactive.
         */
        if (cpi.repeatFrameDetected || (cpi.active_map_enabled && x.active_ptr.get() == 0)) {
            ret.variance = 0;
            ret.sse = 0;
            x.skip = true;
            return Integer.MAX_VALUE;
        }

        if ((this_mode != MBPredictionMode.NEWMV) || !(cpi.sf.half_pixel_search) || cpi.common.full_pixel) {
            vp8_get_inter_mbpred_error(x, cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), ret, mv);
        }

        this_rd = RDOpt.RDCOST(x.rdmult, x.rddiv, rate2, ret.variance);

// Adjust rd for ZEROMV and LAST, if LAST is the closest reference frame.
// TODO: We should also add condition on distance of closest to current.
        if (cpi.oxcf.screen_content_mode == 0 && this_mode == MBPredictionMode.ZEROMV
                && currentMI.mbmi.ref_frame == MVReferenceFrame.LAST_FRAME
                && (cpi.closest_reference_frame == MVReferenceFrame.LAST_FRAME)) {
// No adjustment if block is considered to be skin area.
            if (x.is_skin)
                rd_adj = 100;

            this_rd = (int) (((long) this_rd) * rd_adj / 100);
        }

        check_for_encode_breakout(ret, cpi);
        return this_rd;
    }

    void pickInterMode(Compressor cpi, Macroblock x, int recon_yoffset, int recon_uvoffset, PickInfoReturn ret,
            int mb_row, int mb_col) {
        Block b = x.block.get();
        MacroblockD xd = x.e_mbd;
        BlockD d = xd.block.get();
        MBModeInfo best_mbmode = new MBModeInfo();
        ModeInfo currentMI = x.e_mbd.mode_info_context.get();
        reset();

        MBPredictionMode this_mode;
        long best_rd = Long.MAX_VALUE;
        int rd_adjustment = 100;
        long best_intra_rd = Long.MAX_VALUE;
        int mode_index;
        QualityMetrics currRde = new QualityMetrics();
        int rate2;
        VarianceResults varRes = new VarianceResults();
        long bestsme = Long.MAX_VALUE;
        int best_mode_index = 0;
        varRes.sse = Integer.MAX_VALUE;
        int best_rd_sse = Integer.MAX_VALUE;

        boolean sf_improved_mv_pred = cpi.sf.improved_mv_pred;

        boolean saddone = false;
        /* search range got from mv_pred(). It uses step_param levels. (0-7) */
        int sr = 0;

        FullAccessIntArrPointer[][] plane = new FullAccessIntArrPointer[4][3];
        MVReferenceFrame[] ref_frame_map = new MVReferenceFrame[4];
        int sign_bias = 0;
        boolean dot_artifact_candidate = false;
        RDOpt.get_predictor_pointers(cpi, plane, recon_yoffset, recon_uvoffset);

// If the current frame is using LAST as a reference, check for
// biasing the mode selection for dot artifacts.
        if (cpi.ref_frame_flags.contains(MVReferenceFrame.LAST_FRAME)) {
            FullAccessIntArrPointer target_y = x.src.y_buffer;
            FullAccessIntArrPointer target_u = x.block.getRel(16).getSrcPtr();
            FullAccessIntArrPointer target_v = x.block.getRel(20).getSrcPtr();
            int stride = x.src.y_stride;
            int stride_uv = x.block.getRel(16).src_stride;
            assert (plane[MVReferenceFrame.LAST_FRAME.ordinal()][0] != null);
            dot_artifact_candidate = check_dot_artifact_candidate(cpi, x, target_y, stride,
                    plane[MVReferenceFrame.LAST_FRAME.ordinal()][0], mb_row, mb_col, 0);
// If not found in Y channel, check UV channel.
            if (!dot_artifact_candidate) {
                assert (plane[MVReferenceFrame.LAST_FRAME.ordinal()][1] != null);
                dot_artifact_candidate = check_dot_artifact_candidate(cpi, x, target_u, stride_uv,
                        plane[MVReferenceFrame.LAST_FRAME.ordinal()][1], mb_row, mb_col, 1);
                if (!dot_artifact_candidate) {
                    assert (plane[MVReferenceFrame.LAST_FRAME.ordinal()][2] != null);
                    dot_artifact_candidate = check_dot_artifact_candidate(cpi, x, target_v, stride_uv,
                            plane[MVReferenceFrame.LAST_FRAME.ordinal()][2], mb_row, mb_col, 2);
                }
            }
        }

// Check if current macroblock is in skin area.
        x.is_skin = false;
        if (cpi.oxcf.screen_content_mode == 0) {
            int block_index = mb_row * cpi.common.mb_cols + mb_col;
            x.is_skin = cpi.skin_map[block_index];
        }

        mode_mv = mode_mv_sb[sign_bias];

        /* Setup search priorities */
        RDOpt.get_reference_search_order(cpi, ref_frame_map);

        /*
         * Check to see if there is at least 1 valid reference frame that we need to
         * calculate near_mvs.
         */
        if (ref_frame_map[1] != null) {
            sign_bias = FindNearMV.vp8_find_near_mvs_bias(x.e_mbd, x.e_mbd.mode_info_context, mode_mv_sb,
                    best_ref_mv_sb, mdCounts, ref_frame_map[1], cpi.common.ref_frame_sign_bias) ? 1 : 0;

            mode_mv = mode_mv_sb[sign_bias];
            best_ref_mv.set(best_ref_mv_sb[sign_bias]);
        }

        /* Count of the number of MBs tested so far this frame */
        x.mbs_tested_so_far++;

        ret.intra = Integer.MAX_VALUE;
        x.skip = false;

        currentMI.mbmi.ref_frame = MVReferenceFrame.INTRA_FRAME;

        /*
         * If the frame has big static background and current MB is in low motion area,
         * its mode decision is biased to ZEROMV mode. No adjustment if cpu_used is <=
         * -12 (i.e., cpi.Speed >= 12). At such speed settings, ZEROMV is already
         * heavily favored.
         */
        if (cpi.Speed < 12) {
            rd_adjustment = calculate_zeromv_rd_adjustment(cpi, x, rd_adjustment);
        }

        if (dot_artifact_candidate) {
// Bias against ZEROMV_LAST mode.
            rd_adjustment = 150;
        }

        /*
         * if we encode a new mv this is important find the best new motion vector
         */
        for (mode_index = 0; mode_index < Block.MAX_MODES; ++mode_index) {
            int frame_cost;
            long this_rd = Long.MAX_VALUE;
            MVReferenceFrame this_ref_frame = ref_frame_map[RDOpt.vp8_ref_frame_order[mode_index]];

            if (best_rd <= x.rd_threshes[mode_index])
                continue;

            if (this_ref_frame == null)
                continue;

            currentMI.mbmi.ref_frame = this_ref_frame;

            /* everything but intra */
            if (currentMI.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
                x.e_mbd.pre.y_buffer = plane[this_ref_frame.ordinal()][0];
                x.e_mbd.pre.u_buffer = plane[this_ref_frame.ordinal()][1];
                x.e_mbd.pre.v_buffer = plane[this_ref_frame.ordinal()][2];

                if ((sign_bias != 0) != cpi.common.ref_frame_sign_bias.get(this_ref_frame)) {
                    sign_bias = cpi.common.ref_frame_sign_bias.get(this_ref_frame) ? 1 : 0;
                    mode_mv = mode_mv_sb[sign_bias];
                    best_ref_mv.set(best_ref_mv_sb[sign_bias]);
                }
            }

            /*
             * Check to see if the testing frequency for this mode is at its max If so then
             * prevent it from being tested and increase the threshold for its testing
             */
            if (x.mode_test_hit_counts[mode_index] != 0 && (cpi.mode_check_freq[mode_index] > 1)) {
                if (x.mbs_tested_so_far <= (cpi.mode_check_freq[mode_index] * x.mode_test_hit_counts[mode_index])) {
                    /*
                     * Increase the threshold for coding this mode to make it less likely to be
                     * chosen
                     */
                    x.rd_thresh_mult[mode_index] += 4;

                    if (x.rd_thresh_mult[mode_index] > OnyxInt.MAX_THRESHMULT) {
                        x.rd_thresh_mult[mode_index] = OnyxInt.MAX_THRESHMULT;
                    }

                    x.rd_threshes[mode_index] = (cpi.rd_baseline_thresh[mode_index] >> 7)
                            * x.rd_thresh_mult[mode_index];
                    continue;
                }
            }

            /*
             * We have now reached the point where we are going to test the current mode so
             * increment the counter for the number of times it has been tested
             */
            x.mode_test_hit_counts[mode_index]++;

            rate2 = 0;
            varRes.variance = 0;

            this_mode = RDOpt.vp8_mode_order[mode_index];

            currentMI.mbmi.mode = this_mode;
            currentMI.mbmi.uv_mode = MBPredictionMode.DC_PRED;

            /* Work out the cost assosciated with selecting the reference frame */
            frame_cost = x.ref_frame_cost[currentMI.mbmi.ref_frame.ordinal()];
            rate2 += frame_cost;

            /*
             * Only consider ZEROMV/ALTREF_FRAME for alt ref frame, unless ARNR filtering is
             * enabled in which case we want an unfiltered alternative
             */
            if (cpi.is_src_frame_alt_ref && (cpi.oxcf.arnr_max_frames == 0)) {
                if (this_mode != MBPredictionMode.ZEROMV || currentMI.mbmi.ref_frame != MVReferenceFrame.ALTREF_FRAME) {
                    continue;
                }
            }

            switch (this_mode) {
            case B_PRED: {
                /* Pass best so far to pick_intra4x4mby_modes to use as breakout */
                varRes.variance = best_rd_sse;
                pick_intra4x4mby_modes(x, currRde);

                if (currRde.distortion == Integer.MAX_VALUE) {
                    this_rd = Integer.MAX_VALUE;
                } else {
                    rate2 += currRde.rateBase;
                    Variance.variance(b.base_src, b.src_stride, x.e_mbd.predictor, 16, varRes, 16, 16);
                    this_rd = RDOpt.RDCOST(x.rdmult, x.rddiv, rate2, varRes.variance);

                    if (this_rd < best_intra_rd) {
                        best_intra_rd = this_rd;
                        ret.intra = varRes.variance;
                    }
                }

                break;
            }
            case SPLITMV:

                /* Split MV modes currently not supported when RD is not enabled. */
                break;

            case DC_PRED:
            case V_PRED:
            case H_PRED:
            case TM_PRED:
                x.recon.vp8_build_intra_predictors_mby_s(xd, xd.dst.y_buffer.shallowCopyWithPosInc(-xd.dst.y_stride),
                        xd.dst.y_buffer.shallowCopyWithPosInc(-1), xd.dst.y_stride, xd.predictor, 16);

                Variance.variance(b.base_src, b.src_stride, x.e_mbd.predictor, 16, varRes, 16, 16);
                rate2 += x.mbmode_cost.get(x.e_mbd.frame_type).get(currentMI.mbmi.mode);
                this_rd = RDOpt.RDCOST(x.rdmult, x.rddiv, rate2, varRes.variance);

                if (this_rd < best_intra_rd) {
                    best_intra_rd = this_rd;
                    ret.intra = varRes.variance;
                }
                break;

            case NEWMV: {
                long thissme;
                int step_param;
                int further_steps;
                int n = 0;
                int sadpb = x.sadperbit16;
                MV mvp_full = new MV();

                short col_min = (short) (((best_ref_mv.col + 7) >> 3) - MComp.MAX_FULL_PEL_VAL);
                short row_min = (short) (((best_ref_mv.row + 7) >> 3) - MComp.MAX_FULL_PEL_VAL);
                short col_max = (short) ((best_ref_mv.col >> 3) + MComp.MAX_FULL_PEL_VAL);
                short row_max = (short) ((best_ref_mv.row >> 3) + MComp.MAX_FULL_PEL_VAL);

                short tmp_col_min = x.mv_col_min;
                short tmp_col_max = x.mv_col_max;
                short tmp_row_min = x.mv_row_min;
                short tmp_row_max = x.mv_row_max;

                int speed_adjust = (cpi.Speed > 5) ? ((cpi.Speed >= 8) ? 3 : 2) : 1;

                /* Further step/diamond searches as necessary */
                step_param = cpi.sf.first_step + speed_adjust;

                {
                    if (sf_improved_mv_pred) {
                        if (!saddone) {
                            RDOpt.vp8_cal_sad(cpi, xd, x, recon_yoffset, nearsad);
                            saddone = true;
                        }

                        sr = RDOpt.vp8_mv_pred(cpi, x.e_mbd, x.e_mbd.mode_info_context, mvp, currentMI.mbmi.ref_frame,
                                cpi.common.ref_frame_sign_bias, sr, nearsad);

                        sr += speed_adjust;
                        /* adjust search range according to sr from mv prediction */
                        if (sr > step_param)
                            step_param = sr;
                        mvp_full.set(mvp.div8());
                    } else {
                        mvp.set(best_ref_mv);
                        mvp_full.set(mvp.div8());
                    }
                }

                {
                    /*
                     * Get intersection of UMV window and valid MV window to reduce # of checks in
                     * diamond search.
                     */
                    if (x.mv_col_min < col_min)
                        x.mv_col_min = col_min;
                    if (x.mv_col_max > col_max)
                        x.mv_col_max = col_max;
                    if (x.mv_row_min < row_min)
                        x.mv_row_min = row_min;
                    if (x.mv_row_max > row_max)
                        x.mv_row_max = row_max;

                    further_steps = (cpi.Speed >= 8) ? 0 : (cpi.sf.max_step_search_steps - 1 - step_param);

                    if (cpi.sf.search_method == SearchMethods.HEX) {
                        bestsme = x.hex.apply(x, true, cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), mvp_full, best_ref_mv,
                                d.bmi.mv);
                        mode_mv[MBPredictionMode.NEWMV.ordinal()].set(d.bmi.mv);
                    } else {
                        VarWithNum varAndNum = new VarWithNum();
                        cpi.diamond_search_sad.call(x, b, d, mvp_full, d.bmi.mv, step_param, sadpb, varAndNum,
                                cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), x.mvcost, best_ref_mv);
                        bestsme = varAndNum.var;
                        mode_mv[MBPredictionMode.NEWMV.ordinal()].set(d.bmi.mv);

                        /* Further step/diamond searches as necessary */
                        n = varAndNum.num00;
                        varAndNum.num00 = 0;

                        while (n < further_steps) {
                            n++;

                            if (varAndNum.num00 != 0) {
                                varAndNum.num00--;
                            } else {
                                cpi.diamond_search_sad.call(x, b, d, mvp_full, d.bmi.mv, step_param + n, sadpb,
                                        varAndNum, cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), x.mvcost, best_ref_mv);
                                thissme = varAndNum.var;
                                if (thissme < bestsme) {
                                    bestsme = thissme;
                                    mode_mv[MBPredictionMode.NEWMV.ordinal()].set(d.bmi.mv);
                                } else {
                                    d.bmi.mv.set(mode_mv[MBPredictionMode.NEWMV.ordinal()]);
                                }
                            }
                        }
                    }

                    x.mv_col_min = tmp_col_min;
                    x.mv_col_max = tmp_col_max;
                    x.mv_row_min = tmp_row_min;
                    x.mv_row_max = tmp_row_max;

                    if (bestsme < Integer.MAX_VALUE) {
                        cpi.find_fractional_mv_step.call(x, b, d, d.bmi.mv, best_ref_mv, x.errorperbit,
                                cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), cpi.mb.mvcost, varRes);
                    }
                }

                mode_mv[MBPredictionMode.NEWMV.ordinal()].set(d.bmi.mv);
// The clamp below is not necessary from the perspective
// of VP8 bitstream, but is added to improve ChromeCast
// mirroring's robustness. Please do not remove.
                FindNearMV.vp8_clamp_mv2(mode_mv[this_mode.ordinal()], xd);
                /* mv cost; */
                rate2 += MComp.vp8_mv_bit_cost(mode_mv[MBPredictionMode.NEWMV.ordinal()], best_ref_mv, cpi.mb.mvcost,
                        128);
            }
// fall through

            case NEARESTMV:
            case NEARMV:
                if (mode_mv[this_mode.ordinal()].isZero())
                    continue;
// fall through

            case ZEROMV:

                /*
                 * Trap vectors that reach beyond the UMV borders Note that ALL New MV, Nearest
                 * MV Near MV and Zero MV code drops through to this point because of the lack
                 * of break statements in the previous two cases.
                 */
                MV tmpmvzmv = mode_mv[this_mode.ordinal()];
                if (((tmpmvzmv.row >> 3) < x.mv_row_min) || ((tmpmvzmv.row >> 3) > x.mv_row_max)
                        || ((tmpmvzmv.col >> 3) < x.mv_col_min) || ((tmpmvzmv.col >> 3) > x.mv_col_max)) {
                    continue;
                }

                rate2 += RDOpt.vp8_cost_mv_ref(this_mode, mdCounts);
                currentMI.mbmi.mv.set(mode_mv[this_mode.ordinal()]);
                this_rd = evaluate_inter_mode(varRes, rate2, cpi, x, rd_adjustment);

                break;
            default:
                break;
            }

            if (this_rd < best_rd || x.skip) {
                /* Note index of best mode */
                best_mode_index = mode_index;

                ret.rate = rate2;
                ret.distortion = varRes.variance;
                best_rd_sse = varRes.sse;
                best_rd = this_rd;
                best_mbmode.copyIn(currentMI.mbmi);

                /*
                 * Testing this mode gave rise to an improvement in best error score. Lower
                 * threshold a bit for next time
                 */
                x.rd_thresh_mult[mode_index] = (x.rd_thresh_mult[mode_index] >= (OnyxInt.MIN_THRESHMULT + 2))
                        ? x.rd_thresh_mult[mode_index] - 2
                        : OnyxInt.MIN_THRESHMULT;
                x.rd_threshes[mode_index] = (cpi.rd_baseline_thresh[mode_index] >> 7) * x.rd_thresh_mult[mode_index];
            }

            /*
             * If the mode did not help improve the best error case then raise the threshold
             * for testing that mode next time around.
             */
            else {
                x.rd_thresh_mult[mode_index] += 4;

                if (x.rd_thresh_mult[mode_index] > OnyxInt.MAX_THRESHMULT) {
                    x.rd_thresh_mult[mode_index] = OnyxInt.MAX_THRESHMULT;
                }

                x.rd_threshes[mode_index] = (cpi.rd_baseline_thresh[mode_index] >> 7) * x.rd_thresh_mult[mode_index];
            }

            if (x.skip)
                break;
        }

        RDOpt.reduceActivationThreshold(cpi, best_mode_index);

        {
            long this_rdbin = (ret.distortion >> 7);

            if (this_rdbin >= 1024) {
                this_rdbin = 1023;
            }

            x.error_bins[(int) this_rdbin]++;
        }

        if (RDOpt.adjustToZeroMVForAltref(cpi, best_mbmode))
            return;

        /*
         * set to the best mb mode, this copy can be skip if x.skip since it already has
         * the right content
         */
        if (!x.skip) {
            currentMI.mbmi.copyIn(best_mbmode);
        }

        if (MBPredictionMode.basicModes.contains(best_mbmode.mode)) {
            /* set mode_info_context.mbmi.uv_mode */
            pick_intra_mbuv_mode(x);
        }

        if ((sign_bias != 0) != cpi.common.ref_frame_sign_bias.get(currentMI.mbmi.ref_frame)) {
            best_ref_mv.set(best_ref_mv_sb[sign_bias != 0 ? 0 : 1]);
        }

        update_mvcount(x, best_ref_mv);
    }

    public long vp8_pick_intra_mode(Macroblock x) {
        long error16x16 = Long.MAX_VALUE;
        QualityMetrics curr = new QualityMetrics();
        curr.distortion = Long.MAX_VALUE;
        long best_rate = 0;
        MBPredictionMode best_mode = MBPredictionMode.DC_PRED;
        long this_rd;
        Block b = x.block.get();
        MacroblockD xd = x.e_mbd;
        VarianceResults varRes = new VarianceResults();
        final MBModeInfo mbmi = xd.mode_info_context.get().mbmi;

        mbmi.ref_frame = MVReferenceFrame.INTRA_FRAME;

        pick_intra_mbuv_mode(x);

        ReadOnlyIntArrPointer yabove = xd.dst.y_buffer.shallowCopyWithPosInc(-xd.dst.y_stride);
        ReadOnlyIntArrPointer yleft = xd.dst.y_buffer.shallowCopyWithPosInc(-1);

        for (MBPredictionMode mode : MBPredictionMode.nonBlockPred) {
            mbmi.mode = mode;
            x.recon.vp8_build_intra_predictors_mby_s(xd, yabove, yleft, xd.dst.y_stride, xd.predictor, 16);
            Variance.variance(b.base_src, b.src_stride, xd.predictor, 16, varRes, 16, 16);
            curr.rateBase = x.mbmode_cost.get(xd.frame_type).get(mode);
            this_rd = RDOpt.RDCOST(x.rdmult, x.rddiv, curr.rateBase, varRes.variance);

            if (error16x16 > this_rd) {
                error16x16 = this_rd;
                best_mode = mode;
                curr.distortion = varRes.sse;
                best_rate = curr.rateBase;
            }
        }
        mbmi.mode = best_mode;

        pick_intra4x4mby_modes(x, curr);
        if (curr.error < error16x16) {
            mbmi.mode = MBPredictionMode.B_PRED;
            best_rate = curr.rateBase;
        }

        return best_rate;
    }

}
