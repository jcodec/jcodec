package org.jcodec.codecs.vpx.vp8;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jcodec.codecs.vpx.VP8Util;
import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.codecs.vpx.vp8.data.BestMode;
import org.jcodec.codecs.vpx.vp8.data.BestSegInfo;
import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.DCTValueConstants;
import org.jcodec.codecs.vpx.vp8.data.Entropy;
import org.jcodec.codecs.vpx.vp8.data.EntropyMV;
import org.jcodec.codecs.vpx.vp8.data.EntropyMode;
import org.jcodec.codecs.vpx.vp8.data.EntropyContextPlanes;
import org.jcodec.codecs.vpx.vp8.data.FrameContext;
import org.jcodec.codecs.vpx.vp8.data.MBModeInfo;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.OnyxInt;
import org.jcodec.codecs.vpx.vp8.data.Partition_Info;
import org.jcodec.codecs.vpx.vp8.data.PickInfoReturn;
import org.jcodec.codecs.vpx.vp8.data.QuantCommon;
import org.jcodec.codecs.vpx.vp8.data.QualityMetrics;
import org.jcodec.codecs.vpx.vp8.data.Token;
import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.VarWithNum;
import org.jcodec.codecs.vpx.vp8.data.VarianceFNs;
import org.jcodec.codecs.vpx.vp8.data.VarianceResults;
import org.jcodec.codecs.vpx.vp8.data.YV12buffer;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.BlockEnum;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.PlaneType;
import org.jcodec.codecs.vpx.vp8.enums.ThrModes;
import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;
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
public class RDOpt {

    /*
     * This table determines the search order in reference frame priority order,
     * which may not necessarily match INTRA,LAST,GOLDEN,ARF
     */
    public static final int[] vp8_ref_frame_order = { 1, 0, 1, 1, 2, 2, 3, 3, 2, 3, 0, 0, 0, 1, 2, 3, 1, 2, 3, 0, };
    public static MBPredictionMode[] vp8_mode_order = { MBPredictionMode.ZEROMV, MBPredictionMode.DC_PRED,
            MBPredictionMode.NEARESTMV, MBPredictionMode.NEARMV, MBPredictionMode.ZEROMV, MBPredictionMode.NEARESTMV,
            MBPredictionMode.ZEROMV, MBPredictionMode.NEARESTMV, MBPredictionMode.NEARMV, MBPredictionMode.NEARMV,
            MBPredictionMode.V_PRED, MBPredictionMode.H_PRED, MBPredictionMode.TM_PRED, MBPredictionMode.NEWMV,
            MBPredictionMode.NEWMV, MBPredictionMode.NEWMV, MBPredictionMode.SPLITMV, MBPredictionMode.SPLITMV,
            MBPredictionMode.SPLITMV, MBPredictionMode.B_PRED, };

    static final int[] segmentation_to_sseshift = { 3, 3, 2, 0 };
    static final int[] auto_speed_thresh = { 1000, 200, 150, 130, 150, 125, 120, 115, 115, 115, 115, 115, 115, 115, 115,
            115, 105 };

    static final int[] rd_iifactor = { 4, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0 };

    static long RDCOST(int RM, int DM, int R, long D) {
        return ((128 + R * RM) >> 8) + DM * D;
    }

    static void get_plane_pointers(final YV12buffer fb, final FullAccessIntArrPointer[] plane, final int recon_yoffset,
            final int recon_uvoffset) {
        plane[0] = fb.y_buffer.shallowCopyWithPosInc(recon_yoffset);
        plane[1] = fb.u_buffer.shallowCopyWithPosInc(recon_uvoffset);
        plane[2] = fb.v_buffer.shallowCopyWithPosInc(recon_uvoffset);
    }

    static void get_predictor_pointers(final Compressor cpi, final FullAccessIntArrPointer[][] plane,
            final int recon_yoffset, final int recon_uvoffset) {
        for (final MVReferenceFrame rf : MVReferenceFrame.interFrames) {
            if (cpi.ref_frame_flags.contains(rf)) {
                get_plane_pointers(cpi.common.yv12_fb[cpi.common.frameIdxs.get(rf)], plane[rf.ordinal()], recon_yoffset,
                        recon_uvoffset);
            }
        }
    }

    static void get_reference_search_order(Compressor cpi, MVReferenceFrame[] ref_frame_map) {
        int i = 0;
        for (MVReferenceFrame rf : MVReferenceFrame.values()) {
            if (MVReferenceFrame.validFrames.contains(rf)) {
                if (cpi.ref_frame_flags.contains(rf) || MVReferenceFrame.INTRA_FRAME.equals(rf)) {
                    ref_frame_map[i++] = rf;
                }
            }
        }
        for (; i < 4; i++)
            ref_frame_map[i] = null;
    }

    static int rd_cost_mbuv(Macroblock mb) {
        int b;
        int cost = 0;
        MacroblockD x = mb.e_mbd;
        EntropyContextPlanes t_above = new EntropyContextPlanes(x.above_context.get());
        EntropyContextPlanes t_left = new EntropyContextPlanes(x.left_context);

        for (b = 16; b < 24; ++b) {
            cost += cost_coeffs(mb, x.block.getRel(b), PlaneType.UV,
                    t_above.panes.shallowCopyWithPosInc(BlockD.vp8_block2above[b]),
                    t_left.panes.shallowCopyWithPosInc(BlockD.vp8_block2left[b]));
        }

        return cost;
    }

    static int vp8_rdcost_mby(Macroblock mb) {
        int cost = 0;
        int b;
        MacroblockD x = mb.e_mbd;
        EntropyContextPlanes t_above = new EntropyContextPlanes(mb.e_mbd.above_context.get()),
                t_left = new EntropyContextPlanes(mb.e_mbd.left_context);

        for (b = 0; b < 16; ++b) {
            cost += cost_coeffs(mb, x.block.getRel(b), PlaneType.Y_NO_DC,
                    t_above.panes.shallowCopyWithPosInc(BlockD.vp8_block2above[b]),
                    t_left.panes.shallowCopyWithPosInc(BlockD.vp8_block2left[b]));
        }

        cost += cost_coeffs(mb, x.block.getRel(24), PlaneType.Y2,
                t_above.panes.shallowCopyWithPosInc(BlockD.vp8_block2above[24]),
                t_left.panes.shallowCopyWithPosInc(BlockD.vp8_block2left[24]));
        return cost;
    }

    static int cost_coeffs(Macroblock mb, BlockD b, PlaneType type, FullAccessIntArrPointer a,
            FullAccessIntArrPointer l) {
        int c = type.start_coeff; /* start at coef 0, unless Y with Y2 */
        int corig = c;
        int eob = b.eob.get();
        short pt; /* surrounding block/prev coef predictor */
        int cost = 0;
        ReadOnlyIntArrPointer qcoeff_ptr = b.qcoeff;
        pt = (short) (a.get() + l.get());

        assert (eob <= 16);
        for (; c < eob; ++c) {
            final int v = qcoeff_ptr.getRel(VPXConst.zigzag[c]);
            final TokenAlphabet t = DCTValueConstants.getTokenValue(v).token;
            cost += mb.token_costs[type.ordinal()][VP8Util.SubblockConstants.vp8CoefBands[c]][pt][t.ordinal()];
            cost += DCTValueConstants.getValueCost(v);
            pt = t.previousTokenClass;
        }

        if (c < 16) {
            cost += mb.token_costs[type
                    .ordinal()][VP8Util.SubblockConstants.vp8CoefBands[c]][pt][TokenAlphabet.DCT_EOB_TOKEN.ordinal()];
        }

        pt = (short) (c != corig ? 1 : 0); /* is eob first coefficient; */
        a.set(l.set(pt));

        return cost;
    }

    static int vp8_block_error(ReadOnlyIntArrPointer coeff, ReadOnlyIntArrPointer dqcoeff) {
        int i;
        int error = 0;

        for (i = 0; i < 16; ++i) {
            int this_diff = coeff.getRel(i) - dqcoeff.getRel(i);
            error += this_diff * this_diff;
        }

        return error;
    }

    static int vp8_mbuverror(Macroblock mb) {
        Block be;
        BlockD bd;

        int i;
        int error = 0;

        for (i = 16; i < 24; ++i) {
            be = mb.block.getRel(i);
            bd = mb.e_mbd.block.getRel(i);

            error += vp8_block_error(be.coeff, bd.dqcoeff);
        }

        return error;
    }

    static int labels2mode(Macroblock x, int[] labelings, int which_label, BPredictionMode this_mode, MV this_mv,
            MV best_ref_mv, ReadOnlyIntArrPointer[] mvcost) {
        MacroblockD xd = x.e_mbd;
        FullAccessGenArrPointer<ModeInfo> mic = xd.mode_info_context;
        int mis = xd.mode_info_stride;

        int cost = 0;
        int thismvcost = 0;

        /*
         * We have to be careful retrieving previously-encoded motion vectors. Ones from
         * this macroblock have to be pulled from the BLOCKD array as they have not yet
         * made it to the bmi array in our MB_MODE_INFO.
         */

        int i = 0;

        do {
            int row = i >> 2, col = i & 3;

            BPredictionMode m;

            if (labelings[i] != which_label)
                continue;

            if (col != 0 && labelings[i] == labelings[i - 1]) {
                m = BPredictionMode.LEFT4X4;
            } else if (row != 0 && labelings[i] == labelings[i - 4]) {
                m = BPredictionMode.ABOVE4X4;
            } else {
                /*
                 * the only time we should do costing for new motion vector or mode is when we
                 * are on a new label (jbb May 08, 2007)
                 */
                switch (m = this_mode) {
                case NEW4X4:
                    thismvcost = MComp.vp8_mv_bit_cost(this_mv, best_ref_mv, mvcost, 102);
                    break;
                case LEFT4X4:
                    this_mv.set(col != 0 ? xd.block.getRel(i - 1).bmi.mv : FindNearMV.left_block_mv(mic, i));
                    break;
                case ABOVE4X4:
                    this_mv.set(row != 0 ? xd.block.getRel(i - 4).bmi.mv : FindNearMV.above_block_mv(mic, i, mis));
                    break;
                case ZERO4X4:
                    this_mv.setZero();
                    break;
                default:
                    break;
                }

                if (m == BPredictionMode.ABOVE4X4) { /* replace above with left if same */
                    MV left_mv = new MV(col != 0 ? xd.block.getRel(i - 1).bmi.mv : FindNearMV.left_block_mv(mic, i));

                    if (left_mv.equals(this_mv))
                        m = BPredictionMode.LEFT4X4;
                }

                cost = x.inter_bmode_costs.get(m);
            }

            xd.block.getRel(i).bmi.mv.set(this_mv);

            x.partition_info.get().bmi[i].mode = m;
            x.partition_info.get().bmi[i].mv.set(this_mv);

        } while (++i < 16);

        cost += thismvcost;
        return cost;
    }

    static int rdcost_mbsegment_y(Macroblock mb, int[] labels, int which_label, FullAccessIntArrPointer ta,
            FullAccessIntArrPointer tl) {
        int cost = 0;
        int b;
        MacroblockD x = mb.e_mbd;

        for (b = 0; b < 16; ++b) {
            if (labels[b] == which_label) {
                cost += cost_coeffs(mb, x.block.getRel(b), PlaneType.Y_WITH_DC,
                        ta.shallowCopyWithPosInc(BlockD.vp8_block2above[b]),
                        tl.shallowCopyWithPosInc(BlockD.vp8_block2left[b]));
            }
        }

        return cost;
    }

    static int vp8_encode_inter_mb_segment(Macroblock x, int[] labels, int which_label) {
        int i;
        int distortion = 0;
        int pre_stride = x.e_mbd.pre.y_stride;
        FullAccessIntArrPointer base_pre = x.e_mbd.pre.y_buffer.shallowCopy();

        for (i = 0; i < 16; ++i) {
            if (labels[i] == which_label) {
                BlockD bd = x.e_mbd.block.getRel(i);
                Block be = x.block.getRel(i);

                ReconInter.vp8_build_inter_predictors_b(bd, 16, base_pre, pre_stride, x.e_mbd.subpixel_predict);
                EncodeMB.vp8_subtract_b(be, bd, 16);
                x.short_fdct4x4.call(be.src_diff, be.coeff, 32);
                x.quantize_b.call(be, bd);

                distortion += vp8_block_error(be.coeff, bd.dqcoeff);
            }
        }

        return distortion;
    }

    static void rd_check_segment(Compressor cpi, Macroblock x, BestSegInfo bsi, BlockEnum segmentation) {
        int i;
        int[] labels;
        int br = 0;
        int bd = 0;

        int label_count;
        int this_segment_rd = 0;
        int label_mv_thresh;
        int rate = 0;
        int sbr = 0;
        int sbd = 0;
        int segmentyrate = 0;

        VarianceFNs v_fn_ptr;

        EntropyContextPlanes t_above = new EntropyContextPlanes(x.e_mbd.above_context.get()),
                t_left = new EntropyContextPlanes(x.e_mbd.left_context);
        EntropyContextPlanes t_above_b = new EntropyContextPlanes(), t_left_b = new EntropyContextPlanes();

        br = 0;
        bd = 0;

        v_fn_ptr = cpi.fn_ptr.get(segmentation);
        labels = EntropyMode.vp8_mbsplits[segmentation.ordinal()];
        label_count = EntropyMode.vp8_mbsplit_count[segmentation.ordinal()];

        /*
         * 64 makes this threshold really big effectively making it so that we very
         * rarely check mvs on segments. setting this to 1 would make mv thresh roughly
         * equal to what it is for macroblocks
         */
        label_mv_thresh = 1 * bsi.mvthresh / label_count;

        /* Segmentation method overheads */
        rate = TreeWriter.vp8_cost_token(EntropyMode.vp8_mbsplit_tree, EntropyMode.vp8_mbsplit_probs,
                Token.vp8_mbsplit_encodings[segmentation.ordinal()]);
        rate += vp8_cost_mv_ref(MBPredictionMode.SPLITMV, bsi.mdcounts);
        this_segment_rd += RDCOST(x.rdmult, x.rddiv, rate, 0);
        br += rate;

        for (i = 0; i < label_count; ++i) {
            MV[] mode_mv = new MV[BPredictionMode.bpredModecount];
            for (int j = 0; j < mode_mv.length; j++) {
                mode_mv[j] = new MV();
            }
            long best_label_rd = Long.MAX_VALUE;
            BPredictionMode mode_selected = BPredictionMode.ZERO4X4;
            int bestlabelyrate = 0;

            /* search for the best motion vector on this segment */
            for (BPredictionMode this_mode : BPredictionMode.fourfour) {
                long this_rd;
                int distortion;
                int labelyrate;
                EntropyContextPlanes t_above_s = new EntropyContextPlanes(t_above),
                        t_left_s = new EntropyContextPlanes(t_left);
                FullAccessIntArrPointer ta_s, tl_s;

                ta_s = t_above_s.panes.shallowCopy();
                tl_s = t_left_s.panes.shallowCopy();

                if (this_mode == BPredictionMode.NEW4X4) {
                    int sseshift;
                    int step_param = 0;
                    int further_steps;
                    int n;
                    long thissme;
                    long bestsme = Long.MAX_VALUE;
                    MV temp_mv = new MV();
                    Block b;
                    BlockD d;

                    /*
                     * Is the best so far sufficiently good that we cant justify doing a new motion
                     * search.
                     */
                    if (best_label_rd < label_mv_thresh)
                        break;

                    if (cpi.compressor_speed != 0) {
                        if (segmentation == BlockEnum.BLOCK_8X16 || segmentation == BlockEnum.BLOCK_16X8) {
                            bsi.mvp.set(bsi.sv_mvp[i]);
                            if (i == 1 && segmentation == BlockEnum.BLOCK_16X8) {
                                bsi.mvp.set(bsi.sv_mvp[2]);
                            }

                            step_param = bsi.sv_istep.getRel(i);
                        }

                        /*
                         * use previous block's result as next block's MV predictor.
                         */
                        if (segmentation == BlockEnum.BLOCK_4X4 && i > 0) {
                            bsi.mvp.set(x.e_mbd.block.getRel(i - 1).bmi.mv);
                            if (i == 4 || i == 8 || i == 12) {
                                bsi.mvp.set(x.e_mbd.block.getRel(i - 4).bmi.mv);
                            }
                            step_param = 2;
                        }
                    }

                    further_steps = (MComp.MAX_MVSEARCH_STEPS - 1) - step_param;

                    {
                        int sadpb = x.sadperbit4;
                        MV mvp_full = bsi.mvp.div8();

                        /* find first label */
                        n = FindNearMV.vp8_mbsplit_offset[segmentation.ordinal()][i];

                        b = x.block.getRel(n);
                        d = x.e_mbd.block.getRel(n);

                        {
                            VarWithNum varAndNum = new VarWithNum();
                            cpi.diamond_search_sad.call(x, b, d, mvp_full, mode_mv[BPredictionMode.NEW4X4.ordinal()],
                                    step_param, sadpb, varAndNum, v_fn_ptr, x.mvcost, bsi.ref_mv);
                            bestsme = varAndNum.var;

                            n = varAndNum.num00;
                            varAndNum.num00 = 0;

                            while (n < further_steps) {
                                n++;

                                if (varAndNum.num00 != 0) {
                                    varAndNum.num00--;
                                } else {
                                    cpi.diamond_search_sad.call(x, b, d, mvp_full, temp_mv, step_param + n, sadpb,
                                            varAndNum, v_fn_ptr, x.mvcost, bsi.ref_mv);
                                    thissme = varAndNum.var;
                                    if (thissme < bestsme) {
                                        bestsme = thissme;
                                        mode_mv[BPredictionMode.NEW4X4.ordinal()].set(temp_mv);
                                    }
                                }
                            }
                        }

                        sseshift = segmentation_to_sseshift[segmentation.ordinal()];

                        /* Should we do a full search (best quality only) */
                        if ((cpi.compressor_speed == 0) && (bestsme >> sseshift) > 4000) {
                            /* Check if mvp_full is within the range. */
                            FindNearMV.vp8_clamp_mv(mvp_full, x.mv_col_min, x.mv_col_max, x.mv_row_min, x.mv_row_max);

                            thissme = cpi.full_search_sad.call(x, b, d, mvp_full, sadpb, 16, v_fn_ptr, x.mvcost,
                                    bsi.ref_mv);

                            if (thissme < bestsme) {
                                bestsme = thissme;
                                mode_mv[BPredictionMode.NEW4X4.ordinal()].set(d.bmi.mv);
                            } else {
                                /*
                                 * The full search result is actually worse so re-instate the previous best
                                 * vector
                                 */
                                d.bmi.mv.set(mode_mv[BPredictionMode.NEW4X4.ordinal()]);
                            }
                        }
                    }

                    if (bestsme < Integer.MAX_VALUE) {
                        VarianceResults res = new VarianceResults();
                        cpi.find_fractional_mv_step.call(x, b, d, mode_mv[BPredictionMode.NEW4X4.ordinal()], bsi.ref_mv,
                                x.errorperbit, v_fn_ptr, x.mvcost, res);
                    }
                } /* NEW4X4 */

                rate = labels2mode(x, labels, i, this_mode, mode_mv[this_mode.ordinal()], bsi.ref_mv, x.mvcost);

                /* Trap vectors that reach beyond the UMV borders */
                if (((mode_mv[this_mode.ordinal()].row >> 3) < x.mv_row_min)
                        || ((mode_mv[this_mode.ordinal()].row >> 3) > x.mv_row_max)
                        || ((mode_mv[this_mode.ordinal()].col >> 3) < x.mv_col_min)
                        || ((mode_mv[this_mode.ordinal()].col >> 3) > x.mv_col_max)) {
                    continue;
                }

                distortion = vp8_encode_inter_mb_segment(x, labels, i) / 4;

                labelyrate = rdcost_mbsegment_y(x, labels, i, ta_s, tl_s);
                rate += labelyrate;

                this_rd = RDCOST(x.rdmult, x.rddiv, rate, distortion);

                if (this_rd < best_label_rd) {
                    sbr = rate;
                    sbd = distortion;
                    bestlabelyrate = labelyrate;
                    mode_selected = this_mode;
                    best_label_rd = this_rd;
                    t_above.panes.memcopyin(0, t_above_s.panes, 0, t_above.panes.size());
                    t_left.panes.memcopyin(0, t_left_s.panes, 0, t_left.panes.size());
                }
            } /* for each 4x4 mode */

            t_above.panes.memcopyin(0, t_above_b.panes, 0, t_above.panes.size());
            t_left.panes.memcopyin(0, t_left_b.panes, 0, t_left.panes.size());

            labels2mode(x, labels, i, mode_selected, mode_mv[mode_selected.ordinal()], bsi.ref_mv, x.mvcost);

            br += sbr;
            bd += sbd;
            segmentyrate += bestlabelyrate;
            this_segment_rd += best_label_rd;

            if (this_segment_rd >= bsi.segment_rd)
                break;

        } /* for each label */

        if (this_segment_rd < bsi.segment_rd) {
            bsi.r = br;
            bsi.d = bd;
            bsi.segment_yrate = segmentyrate;
            bsi.segment_rd = this_segment_rd;
            bsi.segment_num = segmentation;

            /* store everything needed to come back to this!! */
            for (i = 0; i < 16; ++i) {
                bsi.mvs[i].set(x.partition_info.get().bmi[i].mv);
                bsi.modes[i] = x.partition_info.get().bmi[i].mode;
                bsi.eobs[i] = x.e_mbd.eobs.getRel(i);
            }
        }
    }

    static void vp8_cal_step_param(short sr, FullAccessIntArrPointer sp) {
        short step = 0;

        if (sr > MComp.MAX_FIRST_STEP) {
            sr = MComp.MAX_FIRST_STEP;
        } else if (sr < 1) {
            sr = 1;
        }

        while ((sr >>= 1) != 0)
            step++;

        sp.set((short) (MComp.MAX_MVSEARCH_STEPS - 1 - step));
    }

    private static short seghelper(Compressor cpi, Macroblock x, BestSegInfo bsi, BlockEnum bekind) {
        int mvp1, mvp2;
        if (bekind == BlockEnum.BLOCK_8X16) {
            mvp1 = 2;
            mvp2 = 1;
        } else {
            mvp1 = 1;
            mvp2 = 2;
        }
        short sr = (short) Math.max((Math.abs(bsi.sv_mvp[0].row - bsi.sv_mvp[mvp1].row)) >> 3,
                (Math.abs(bsi.sv_mvp[0].col - bsi.sv_mvp[mvp1].col)) >> 3);
        vp8_cal_step_param(sr, bsi.sv_istep);
        bsi.sv_istep.inc();

        sr = (short) Math.max((Math.abs(bsi.sv_mvp[mvp2].row - bsi.sv_mvp[3].row)) >> 3,
                (Math.abs(bsi.sv_mvp[mvp2].col - bsi.sv_mvp[3].col)) >> 3);
        vp8_cal_step_param(sr, bsi.sv_istep);
        bsi.sv_istep.dec();

        rd_check_segment(cpi, x, bsi, bekind);
        return sr;

    }

    static void vp8_rd_pick_best_mbsegmentation(Compressor cpi, Macroblock x, MV best_ref_mv, long best_rd,
            int[] mdcounts, QualityMetrics best, int mvthresh) {
        int i;
        BestSegInfo bsi = new BestSegInfo(best_rd, best_ref_mv, mvthresh, mdcounts);

        if (cpi.compressor_speed == 0) {
            /*
             * for now, we will keep the original segmentation order when in best quality
             * mode
             */
            for (BlockEnum be : BlockEnum.allBut1616) {
                rd_check_segment(cpi, x, bsi, be);
            }
        } else {
            short sr;

            rd_check_segment(cpi, x, bsi, BlockEnum.BLOCK_8X8);

            if (bsi.segment_rd < best_rd) {
                short col_min = (short) (((best_ref_mv.col + 7) >> 3) - MComp.MAX_FULL_PEL_VAL);
                short row_min = (short) (((best_ref_mv.row + 7) >> 3) - MComp.MAX_FULL_PEL_VAL);
                short col_max = (short) ((best_ref_mv.col >> 3) + MComp.MAX_FULL_PEL_VAL);
                short row_max = (short) ((best_ref_mv.row >> 3) + MComp.MAX_FULL_PEL_VAL);

                short tmp_col_min = x.mv_col_min;
                short tmp_col_max = x.mv_col_max;
                short tmp_row_min = x.mv_row_min;
                short tmp_row_max = x.mv_row_max;

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

                /* Get 8x8 result */
                bsi.sv_mvp[0].set(bsi.mvs[0]);
                bsi.sv_mvp[1].set(bsi.mvs[2]);
                bsi.sv_mvp[2].set(bsi.mvs[8]);
                bsi.sv_mvp[3].set(bsi.mvs[10]);

                /*
                 * Use 8x8 result as 16x8/8x16's predictor MV. Adjust search range according to
                 * the closeness of 2 MV.
                 */
                /* block 8X16 */
                {
                    sr = seghelper(cpi, x, bsi, BlockEnum.BLOCK_8X16);
                    sr = (short) Math.max((Math.abs(bsi.sv_mvp[0].row - bsi.sv_mvp[2].row)) >> 3,
                            (Math.abs(bsi.sv_mvp[0].col - bsi.sv_mvp[2].col)) >> 3);
                    vp8_cal_step_param(sr, bsi.sv_istep);

                    sr = (short) Math.max((Math.abs(bsi.sv_mvp[1].row - bsi.sv_mvp[3].row)) >> 3,
                            (Math.abs(bsi.sv_mvp[1].col - bsi.sv_mvp[3].col)) >> 3);
                    bsi.sv_istep.inc();
                    vp8_cal_step_param(sr, bsi.sv_istep);
                    bsi.sv_istep.dec();

                    rd_check_segment(cpi, x, bsi, BlockEnum.BLOCK_8X16);
                }

                /* block 16X8 */
                {
                    sr = seghelper(cpi, x, bsi, BlockEnum.BLOCK_16X8);
                }

                /* If 8x8 is better than 16x8/8x16, then do 4x4 search */
                /* Not skip 4x4 if speed=0 (good quality) */
                if (cpi.sf.no_skip_block4x4_search || bsi.segment_num == BlockEnum.BLOCK_8X8)
                /* || (sv_segment_rd8x8-bsi.segment_rd) < sv_segment_rd8x8>>5) */
                {
                    bsi.mvp.set(bsi.sv_mvp[0]);
                    rd_check_segment(cpi, x, bsi, BlockEnum.BLOCK_4X4);
                }

                /* restore UMV window */
                x.mv_col_min = tmp_col_min;
                x.mv_col_max = tmp_col_max;
                x.mv_row_min = tmp_row_min;
                x.mv_row_max = tmp_row_max;
            }
        }

        /* set it to the best */
        for (i = 0; i < 16; ++i) {
            BlockD bd = x.e_mbd.block.getRel(i);

            bd.bmi.mv.set(bsi.mvs[i]);
            bd.eob.set(bsi.eobs[i]);
        }

        best.rateBase = bsi.r;
        best.distortion = bsi.d;
        best.rateComp = bsi.segment_yrate;

        /* save partitions */
        x.e_mbd.mode_info_context.get().mbmi.partitioning = bsi.segment_num;
        Partition_Info partInfo = x.partition_info.get();
        partInfo.count = EntropyMode.vp8_mbsplit_count[bsi.segment_num.ordinal()];

        for (i = 0; i < partInfo.count; ++i) {
            int j;

            j = FindNearMV.vp8_mbsplit_offset[bsi.segment_num.ordinal()][i];

            partInfo.bmi[i].mode = bsi.modes[j];
            partInfo.bmi[i].mv.set(bsi.mvs[j]);
        }
        partInfo.bmi[15].mv.set(bsi.mvs[15]);
        best.error = bsi.segment_rd;
    }

    static void rd_inter4x4_uv(Compressor cpi, Macroblock x, QualityMetrics best, boolean fullpixel) {

        ReconInter.vp8_build_inter4x4_predictors_mbuv(x.e_mbd);
        EncodeMB.vp8_subtract_mbuv(x.src_diff, x.src.u_buffer, x.src.v_buffer, x.src.uv_stride,
                x.e_mbd.getFreshUPredPtr(), x.e_mbd.getFreshVPredPtr(), 8);

        EncodeMB.vp8_transform_mbuv(x);
        Quantize.vp8_quantize_mbuv(x);

        best.rateBase = rd_cost_mbuv(x);
        best.distortion = vp8_mbuverror(x) >> 2;
        best.error = RDCOST(x.rdmult, x.rddiv, best.rateBase, best.distortion);
    }

    static void rd_pick_intra_mbuv_mode(Macroblock x, QualityMetrics rd) {
        MBPredictionMode mode_selected = null;
        rd.error = Long.MAX_VALUE;
        int rate_to;
        final MacroblockD xd = x.e_mbd;
        final FullAccessIntArrPointer vpred_ptr = xd.getFreshVPredPtr();
        final FullAccessIntArrPointer upred_ptr = xd.getFreshUPredPtr();
        final ReadOnlyIntArrPointer uabove = xd.dst.u_buffer.shallowCopyWithPosInc(-xd.dst.uv_stride);
        final ReadOnlyIntArrPointer vabove = xd.dst.v_buffer.shallowCopyWithPosInc(-xd.dst.uv_stride);
        final ReadOnlyIntArrPointer uleft = xd.dst.u_buffer.shallowCopyWithPosInc(-1);
        final ReadOnlyIntArrPointer vleft = xd.dst.v_buffer.shallowCopyWithPosInc(-1);
        ModeInfo mi = xd.mode_info_context.get();

        for (MBPredictionMode mode : MBPredictionMode.nonBlockPred) {
            int this_rate;
            int this_distortion;
            long this_rd;

            mi.mbmi.uv_mode = mode;

            x.recon.vp8_build_intra_predictors_mbuv_s(xd, uabove, vabove, uleft, vleft, xd.dst.uv_stride, upred_ptr,
                    vpred_ptr, 8);

            EncodeMB.vp8_subtract_mbuv(x.src_diff, x.src.u_buffer, x.src.v_buffer, x.src.uv_stride, upred_ptr,
                    vpred_ptr, 8);
            EncodeMB.vp8_transform_mbuv(x);
            Quantize.vp8_quantize_mbuv(x);

            rate_to = rd_cost_mbuv(x);
            this_rate = rate_to + x.intra_uv_mode_cost[xd.frame_type.ordinal()][mi.mbmi.uv_mode.ordinal()];

            this_distortion = vp8_mbuverror(x) / 4;

            this_rd = RDCOST(x.rdmult, x.rddiv, this_rate, this_distortion);

            if (this_rd < rd.error) {
                rd.error = this_rd;
                rd.distortion = this_distortion;
                rd.rateComp = this_rate;
                rd.rateBase = rate_to;
                mode_selected = mode;
            }
        }

        assert (mode_selected != null);
        mi.mbmi.uv_mode = mode_selected;
    }

    static int vp8_mbblock_error(Macroblock mb, int dc) {
        Block be;
        BlockD bd;
        int i, j;
        int berror, error = 0;

        for (i = 0; i < 16; ++i) {
            be = mb.block.getRel(i);
            bd = mb.e_mbd.block.getRel(i);

            berror = 0;

            for (j = dc; j < 16; ++j) {
                int this_diff = be.coeff.getRel(j) - bd.dqcoeff.getRel(j);
                berror += this_diff * this_diff;
            }

            error += berror;
        }

        return error;
    }

    static void macro_block_yrd(Macroblock mb, QualityMetrics best) {
        int b;
        MacroblockD x = mb.e_mbd;
        Block mb_y2 = mb.block.getRel(24);
        BlockD x_y2 = x.block.getRel(24);
        FullAccessIntArrPointer Y2DCPtr = mb_y2.src_diff.shallowCopy();
        int d;

        EncodeMB.vp8_subtract_mby(mb.src_diff, mb.block.get().base_src, mb.block.get().src_stride, mb.e_mbd.predictor,
                16);

        /* Fdct and building the 2nd order block */
        for (int bi = 0; bi < 16; bi += 2) {
            mb.short_fdct8x4.call(mb.block.getRel(bi).src_diff, mb.block.getRel(bi).coeff, 32);
            Y2DCPtr.setAndInc(mb.block.getRel(bi).coeff.get());
            Y2DCPtr.setAndInc(mb.block.getRel(bi).coeff.getRel(16));
        }

        /* 2nd order fdct */
        mb.short_walsh4x4.call(mb_y2.src_diff, mb_y2.coeff, 8);

        /* Quantization */
        for (b = 0; b < 16; ++b) {
            mb.quantize_b.call(mb.block.getRel(b), mb.e_mbd.block.getRel(b));
        }

        /* DC predication and Quantization of 2nd Order block */
        mb.quantize_b.call(mb_y2, x_y2);

        /* Distortion */
        d = vp8_mbblock_error(mb, 1) << 2;
        d += vp8_block_error(mb_y2.coeff, x_y2.dqcoeff);

        best.distortion = d >> 4;

        /* rate */
        best.rateBase = vp8_rdcost_mby(mb);
    }

    static void rd_pick_intra16x16mby_mode(Macroblock x, QualityMetrics rd) {
        MBPredictionMode mode_selected = null;
        long this_rd;
        int temp;
        MacroblockD xd = x.e_mbd;
        QualityMetrics curr = new QualityMetrics();
        rd.error = Long.MAX_VALUE;
        ModeInfo mi = xd.mode_info_context.get();
        /* Y Search for 16x16 intra prediction mode */
        for (MBPredictionMode mode : MBPredictionMode.nonBlockPred) {
            mi.mbmi.mode = mode;

            x.recon.vp8_build_intra_predictors_mby_s(xd, xd.dst.y_buffer.shallowCopyWithPosInc(-xd.dst.y_stride),
                    xd.dst.y_buffer.shallowCopyWithPosInc(-1), xd.dst.y_stride, xd.predictor, 16);
            macro_block_yrd(x, curr);
            temp = curr.rateBase + x.mbmode_cost.get(xd.frame_type).get(mi.mbmi.mode);

            this_rd = RDCOST(x.rdmult, x.rddiv, temp, curr.distortion);

            if (this_rd < rd.error) {
                mode_selected = mode;
                rd.error = this_rd;
                rd.rateBase = temp;
                rd.rateComp = curr.rateComp;
                rd.distortion = curr.distortion;
            }
        }

        assert (mode_selected != null);
        mi.mbmi.mode = mode_selected;
    }

    static void copy_predictor(FullAccessIntArrPointer dst, ReadOnlyIntArrPointer predictor) {
        for (int i = 0; i < 13; i += 4)
            dst.setRel(i, predictor.getRel(i));
    }

    static void rd_pick_intra4x4block(Macroblock x, Block be, BlockD b, BPredictionMode[] bestmode,
            EnumMap<BPredictionMode, Integer> bmode_costs, FullAccessIntArrPointer a, FullAccessIntArrPointer l,
            QualityMetrics best) {
        int rate = 0;
        int distortion;
        best.error = Long.MAX_VALUE;

        FullAccessIntArrPointer ta = a, tempa = a;
        FullAccessIntArrPointer tl = l, templ = l;
        /*
         * The predictor buffer is a 2d buffer with a stride of 16. Create a temp buffer
         * that meets the stride requirements, but we are only interested in the left
         * 4x4 block
         */
        FullAccessIntArrPointer best_predictor = new FullAccessIntArrPointer(16 * 4);
        FullAccessIntArrPointer best_dqcoeff = new FullAccessIntArrPointer(16);
        int dst_stride = x.e_mbd.dst.y_stride;
        FullAccessIntArrPointer dst = b.getOffsetPointer(x.e_mbd.dst.y_buffer);

        FullAccessIntArrPointer Above = dst.shallowCopyWithPosInc(-dst_stride);
        FullAccessIntArrPointer yleft = dst.shallowCopyWithPosInc(-1);
        final short top_left = Above.getRel(-1);

        for (BPredictionMode mode : BPredictionMode.bintramodes) {
            long this_rd;
            int ratey;

            rate = bmode_costs.get(mode);

            x.recon.vp8_intra4x4_predict(Above, yleft, dst_stride, mode, b.predictor, 16, top_left);
            EncodeMB.vp8_subtract_b(be, b, 16);
            x.short_fdct4x4.call(be.src_diff, be.coeff, 32);
            x.quantize_b.call(be, b);

            tempa = ta.shallowCopy();
            templ = tl.shallowCopy();

            ratey = cost_coeffs(x, b, PlaneType.Y_WITH_DC, tempa, templ);
            rate += ratey;
            distortion = vp8_block_error(be.coeff, b.dqcoeff) >> 2;

            this_rd = RDCOST(x.rdmult, x.rddiv, rate, distortion);

            if (this_rd < best.error) {
                best.rateBase = rate;
                best.rateComp = ratey;
                best.distortion = distortion;
                best.error = this_rd;
                bestmode[0] = mode;
                a.set(tempa.get());
                l.set(templ.get());
                copy_predictor(best_predictor, b.predictor);
                best_dqcoeff.memcopyin(0, b.dqcoeff, 0, 16);
            }
        }
        b.bmi.as_mode(bestmode[0]);

        IDCTllm.vp8_short_idct4x4llm(best_dqcoeff, best_predictor, 16, dst, dst_stride);
    }

    static void rd_pick_intra4x4mby_modes(Macroblock mb, QualityMetrics best) {
        MacroblockD xd = mb.e_mbd;
        int i;
        int cost = mb.mbmode_cost.get(xd.frame_type).get(MBPredictionMode.B_PRED);
        int distortion = 0;
        int tot_rate_y = 0;
        long total_rd = 0;
        EntropyContextPlanes t_above = new EntropyContextPlanes(mb.e_mbd.above_context.get()),
                t_left = new EntropyContextPlanes(mb.e_mbd.left_context);
        EnumMap<BPredictionMode, Integer> bmode_costs;

        ReconIntra.intra_prediction_down_copy(xd);

        bmode_costs = mb.inter_bmode_costs;

        QualityMetrics rdTemp = new QualityMetrics();
        ModeInfo mi = xd.mode_info_context.get();
        for (i = 0; i < 16; ++i) {
            final int mis = xd.mode_info_stride;
            BPredictionMode[] best_mode = new BPredictionMode[] { null };

            if (mb.e_mbd.frame_type == FrameType.KEY_FRAME) {
                final BPredictionMode A = FindNearMV.above_block_mode(xd.mode_info_context, i, mis);
                final BPredictionMode L = FindNearMV.left_block_mode(xd.mode_info_context, i);

                bmode_costs = mb.bmode_costs.get(A).get(L);
            }
            rd_pick_intra4x4block(mb, mb.block.getRel(i), xd.block.getRel(i), best_mode, bmode_costs,
                    t_above.panes.shallowCopyWithPosInc(BlockD.vp8_block2above[i]),
                    t_left.panes.shallowCopyWithPosInc(BlockD.vp8_block2left[i]), rdTemp);
            total_rd += rdTemp.error;
            cost += rdTemp.rateBase;
            distortion += rdTemp.distortion;
            tot_rate_y += rdTemp.rateComp;

            assert (best_mode[0] != null);
            mi.bmi[i].as_mode(best_mode[0]);

            if (total_rd >= best.error)
                break;
        }

        if (total_rd >= best.error) {
            best.error = Integer.MAX_VALUE;
            return;
        }

        best.rateBase = cost;
        best.rateComp = tot_rate_y;
        best.distortion = distortion;
        best.error = RDCOST(mb.rdmult, mb.rddiv, cost, distortion);
    }

    static void vp8_rd_pick_inter_mode(Compressor cpi, Macroblock x, int recon_yoffset, int recon_uvoffset,
            PickInfoReturn ret, int mb_row, int mb_col) {
        Block b = x.block.get();
        BlockD d = x.e_mbd.block.get();
        ModeInfo mi = x.e_mbd.mode_info_context.get();
        MacroblockD xd = x.e_mbd;
        PickInter mvDetails = x.interPicker;
        mvDetails.reset();
        MBPredictionMode this_mode;
        int best_mode_index = 0;
        BestMode best_mode = new BestMode();

        int i;
        int mode_index;
        QualityMetrics rdy = new QualityMetrics();
        QualityMetrics rduv = new QualityMetrics();
        QualityMetrics rduvComplete = new QualityMetrics();
        int uv_intra_tteob = 0;
        boolean uv_intra_done = false;

        MBPredictionMode uv_intra_mode = MBPredictionMode.DC_PRED;
        boolean saddone = false;
        /* search range got from mv_pred(). It uses step_param levels. (0-7) */
        int sr = 0;

        FullAccessIntArrPointer[][] plane = new FullAccessIntArrPointer[4][3];
        MVReferenceFrame[] ref_frame_map = new MVReferenceFrame[4];
        boolean sign_bias = false;

        int intra_rd_penalty = 10
                * QuantCommon.doLookup(cpi.common, CommonData.Quant.Y1, CommonData.Comp.DC, cpi.common.base_qindex);

// _uv variables are not set consistantly before calling update_best_mode.

        mvDetails.mode_mv = mvDetails.mode_mv_sb[sign_bias ? 1 : 0];

        /* Setup search priorities */
        get_reference_search_order(cpi, ref_frame_map);

        /*
         * Check to see if there is at least 1 valid reference frame that we need to
         * calculate near_mvs.
         */
        if (ref_frame_map[1] != MVReferenceFrame.INTRA_FRAME) {
            boolean psb = sign_bias;
            sign_bias = FindNearMV.vp8_find_near_mvs_bias(x.e_mbd, x.e_mbd.mode_info_context, mvDetails.mode_mv_sb,
                    mvDetails.best_ref_mv_sb, mvDetails.mdCounts, ref_frame_map[1], cpi.common.ref_frame_sign_bias);
            if (psb != sign_bias) {
                mvDetails.mode_mv = mvDetails.mode_mv_sb[sign_bias ? 1 : 0];
            }
            mvDetails.best_ref_mv.set(mvDetails.best_ref_mv_sb[sign_bias ? 1 : 0]);
        }

        get_predictor_pointers(cpi, plane, recon_yoffset, recon_uvoffset);

        ret.intra = Integer.MAX_VALUE;
        /* Count of the number of MBs tested so far this frame */
        x.mbs_tested_so_far++;

        x.skip = false;

        for (mode_index = 0; mode_index < Block.MAX_MODES; ++mode_index) {
            long this_rd = Long.MAX_VALUE;
            boolean[] disable_skip = { false };
            AtomicInteger other_cost = new AtomicInteger();
            MVReferenceFrame this_ref_frame = ref_frame_map[vp8_ref_frame_order[mode_index]];

            /* Test best rd so far against threshold for trying this mode. */
            if (best_mode.rd <= x.rd_threshes[mode_index])
                continue;

            if (this_ref_frame == null)
                continue;

            /*
             * These variables hold are rolling total cost and distortion for this mode
             */

            this_mode = vp8_mode_order[mode_index];
//            if (this_mode != MBPredictionMode.ZEROMV && cpi.repeatFrameDetected)
//                break;

            mi.mbmi.mode = this_mode;
            mi.mbmi.ref_frame = this_ref_frame;

            /*
             * Only consider ZEROMV/ALTREF_FRAME for alt ref frame, unless ARNR filtering is
             * enabled in which case we want an unfiltered alternative
             */
            if (cpi.is_src_frame_alt_ref && (cpi.oxcf.arnr_max_frames == 0)) {
                if (this_mode != MBPredictionMode.ZEROMV || mi.mbmi.ref_frame != MVReferenceFrame.ALTREF_FRAME) {
                    continue;
                }
            }

            if (mi.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
                assert (plane[this_ref_frame.ordinal()][0] != null && plane[this_ref_frame.ordinal()][1] != null
                        && plane[this_ref_frame.ordinal()][2] != null);
                x.e_mbd.pre.y_buffer = plane[this_ref_frame.ordinal()][0];
                x.e_mbd.pre.u_buffer = plane[this_ref_frame.ordinal()][1];
                x.e_mbd.pre.v_buffer = plane[this_ref_frame.ordinal()][2];

                if (sign_bias != cpi.common.ref_frame_sign_bias.get(this_ref_frame)) {
                    sign_bias = cpi.common.ref_frame_sign_bias.get(this_ref_frame);
                    mvDetails.mode_mv = mvDetails.mode_mv_sb[sign_bias ? 1 : 0];
                    mvDetails.best_ref_mv.set(mvDetails.best_ref_mv_sb[sign_bias ? 1 : 0]);
                }
            }

            /*
             * Check to see if the testing frequency for this mode is at its max If so then
             * prevent it from being tested and increase the threshold for its testing
             */
            if (x.mode_test_hit_counts[mode_index] != 0 && (cpi.mode_check_freq[mode_index] > 1)) {
                if (x.mbs_tested_so_far <= cpi.mode_check_freq[mode_index] * x.mode_test_hit_counts[mode_index]) {
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

            /*
             * Experimental code. Special case for gf and arf zeromv modes. Increase zbin
             * size to supress noise
             */
            if (x.zbin_mode_boost_enabled) {
                if (this_ref_frame == MVReferenceFrame.INTRA_FRAME) {
                    x.zbin_mode_boost = 0;
                } else {
                    if (vp8_mode_order[mode_index] == MBPredictionMode.ZEROMV) {
                        if (this_ref_frame != MVReferenceFrame.LAST_FRAME) {
                            x.zbin_mode_boost = OnyxInt.GF_ZEROMV_ZBIN_BOOST;
                        } else {
                            x.zbin_mode_boost = OnyxInt.LF_ZEROMV_ZBIN_BOOST;
                        }
                    } else if (vp8_mode_order[mode_index] == MBPredictionMode.SPLITMV) {
                        x.zbin_mode_boost = 0;
                    } else {
                        x.zbin_mode_boost = OnyxInt.MV_ZBIN_BOOST;
                    }
                }

                Quantize.vp8_update_zbin_extra(cpi, x);
            }

            if (!uv_intra_done && this_ref_frame == MVReferenceFrame.INTRA_FRAME) {
                rd_pick_intra_mbuv_mode(x, rduv);
                uv_intra_mode = mi.mbmi.uv_mode;

                /*
                 * Total of the eobs is used later to further adjust rate2. Since uv block's
                 * intra eobs will be overwritten when we check inter modes, we need to save
                 * uv_intra_tteob here.
                 */
                for (i = 16; i < 24; ++i)
                    uv_intra_tteob += x.e_mbd.eobs.getRel(i);

                uv_intra_done = true;
            }

            switch (this_mode) {
            case B_PRED: {
                /*
                 * Note the rate value returned here includes the cost of coding the BPRED mode:
                 * x.mbmode_cost[x.e_mbd.frame_type][BPRED]
                 */
                QualityMetrics rdtemp = new QualityMetrics();
                rdtemp.error = best_mode.yrd;
                rdtemp.rateBase = rdy.rateBase;
                rd_pick_intra4x4mby_modes(x, rdtemp);
                rdy.rateBase += rdtemp.rateBase;
                rdy.distortion += rdtemp.distortion;
                rdy.rateComp = rdtemp.rateComp;

                if (rdtemp.error < best_mode.yrd) {
                    assert (uv_intra_done);
                    rdy.rateBase += rduv.rateBase;
                    rduvComplete.rateComp = rduv.rateComp;
                    rdy.distortion += rduv.distortion;
                    rduvComplete.distortion = rduv.distortion;
                } else {
                    this_rd = Integer.MAX_VALUE;
                    disable_skip[0] = true;
                }
                break;
            }

            case SPLITMV: {
                int this_rd_thresh;
                QualityMetrics rryde = new QualityMetrics();
                rryde.rateComp = rdy.rateComp;
                this_rd_thresh = (vp8_ref_frame_order[mode_index] == 1) ? x.rd_threshes[ThrModes.THR_NEW1.ordinal()]
                        : x.rd_threshes[ThrModes.THR_NEW3.ordinal()];
                this_rd_thresh = (vp8_ref_frame_order[mode_index] == 2) ? x.rd_threshes[ThrModes.THR_NEW2.ordinal()]
                        : this_rd_thresh;

                vp8_rd_pick_best_mbsegmentation(cpi, x, mvDetails.best_ref_mv, best_mode.yrd, mvDetails.mdCounts, rryde,
                        this_rd_thresh);

                rdy.rateBase += rryde.rateBase;
                rdy.distortion += rryde.distortion;
                rdy.rateComp = rryde.rateComp;

                /*
                 * If even the 'Y' rd value of split is higher than best so far then dont bother
                 * looking at UV
                 */
                if (rryde.error < best_mode.yrd) {
                    /* Now work out UV cost and add it in */
                    QualityMetrics rde = new QualityMetrics();
                    rd_inter4x4_uv(cpi, x, rde, cpi.common.full_pixel);
                    rdy.rateBase = rde.rateBase;
                    rdy.distortion = rde.distortion;
                } else {
                    this_rd = Integer.MAX_VALUE;
                    disable_skip[0] = true;
                }
                break;
            }
            case DC_PRED:
            case V_PRED:
            case H_PRED:
            case TM_PRED: {
                mi.mbmi.ref_frame = MVReferenceFrame.INTRA_FRAME;

                x.recon.vp8_build_intra_predictors_mby_s(xd, xd.dst.y_buffer.shallowCopyWithPosInc(-xd.dst.y_stride),
                        xd.dst.y_buffer.shallowCopyWithPosInc(-1), xd.dst.y_stride, xd.predictor, 16);
                QualityMetrics rde = new QualityMetrics();
                macro_block_yrd(x, rde);
                rdy.rateBase += rde.rateComp;
                rdy.distortion += rde.distortion;
                rdy.rateComp += x.mbmode_cost.get(x.e_mbd.frame_type).get(mi.mbmi.mode);
                assert (uv_intra_done);
                rdy.rateBase += rduv.rateBase;
                rduvComplete.rateBase = rduv.rateBase;
                rduvComplete.distortion += rduv.distortion;
                rduvComplete.distortion = rduv.distortion;
                break;
            }

            case NEWMV: {
                long thissme;
                long bestsme = Long.MAX_VALUE;
                int step_param = cpi.sf.first_step;
                int further_steps;
                int n;
                /*
                 * If last step (1-away) of n-step search doesn't pick the center point as the
                 * best match, we will do a final 1-away diamond refining search
                 */
                int do_refine = 1;

                int sadpb = x.sadperbit16;
                MV mvp_full = new MV();

                short col_min = (short) (((mvDetails.best_ref_mv.col + 7) >> 3) - MComp.MAX_FULL_PEL_VAL);
                short row_min = (short) (((mvDetails.best_ref_mv.row + 7) >> 3) - MComp.MAX_FULL_PEL_VAL);
                short col_max = (short) ((mvDetails.best_ref_mv.col >> 3) + MComp.MAX_FULL_PEL_VAL);
                short row_max = (short) ((mvDetails.best_ref_mv.row >> 3) + MComp.MAX_FULL_PEL_VAL);

                short tmp_col_min = x.mv_col_min;
                short tmp_col_max = x.mv_col_max;
                short tmp_row_min = x.mv_row_min;
                short tmp_row_max = x.mv_row_max;

                if (!saddone) {
                    vp8_cal_sad(cpi, xd, x, recon_yoffset, mvDetails.nearsad);
                    saddone = true;
                }

                vp8_mv_pred(cpi, x.e_mbd, x.e_mbd.mode_info_context, mvDetails.mvp, mi.mbmi.ref_frame,
                        cpi.common.ref_frame_sign_bias, sr, mvDetails.nearsad);

                mvp_full.set(mvDetails.mvp.div8());

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

                /* adjust search range according to sr from mv prediction */
                if (sr > step_param)
                    step_param = sr;

                /* Initial step/diamond search */
                {
                    VarWithNum varAndNum = new VarWithNum();
                    cpi.diamond_search_sad.call(x, b, d, mvp_full, d.bmi.mv, step_param, sadpb, varAndNum,
                            cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), x.mvcost, mvDetails.best_ref_mv);
                    bestsme = varAndNum.var;
                    mvDetails.mode_mv[MBPredictionMode.NEWMV.ordinal()].set(d.bmi.mv);

                    /* Further step/diamond searches as necessary */
                    further_steps = (cpi.sf.max_step_search_steps - 1) - step_param;

                    n = varAndNum.num00;
                    varAndNum.num00 = 0;

                    /*
                     * If there won't be more n-step search, check to see if refining search is
                     * needed.
                     */
                    if (n > further_steps)
                        do_refine = 0;

                    while (n < further_steps) {
                        n++;

                        if (varAndNum.num00 != 0) {
                            varAndNum.num00--;
                        } else {
                            cpi.diamond_search_sad.call(x, b, d, mvp_full, d.bmi.mv, step_param + n, sadpb, varAndNum,
                                    cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), x.mvcost, mvDetails.best_ref_mv);
                            thissme = varAndNum.var;
                            /* check to see if refining search is needed. */
                            if (varAndNum.num00 > (further_steps - n))
                                do_refine = 0;

                            if (thissme < bestsme) {
                                bestsme = thissme;
                                mvDetails.mode_mv[MBPredictionMode.NEWMV.ordinal()].set(d.bmi.mv);
                            } else {
                                d.bmi.mv.set(mvDetails.mode_mv[MBPredictionMode.NEWMV.ordinal()]);
                            }
                        }
                    }
                }

                /* final 1-away diamond refining search */
                if (do_refine == 1) {
                    int search_range;

                    search_range = 8;

                    thissme = cpi.refining_search_sad.call(x, b, d, d.bmi.mv, sadpb, search_range,
                            cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), x.mvcost, mvDetails.best_ref_mv);

                    if (thissme < bestsme) {
                        bestsme = thissme;
                        mvDetails.mode_mv[MBPredictionMode.NEWMV.ordinal()].set(d.bmi.mv);
                    } else {
                        d.bmi.mv.set(mvDetails.mode_mv[MBPredictionMode.NEWMV.ordinal()]);
                    }
                }

                x.mv_col_min = tmp_col_min;
                x.mv_col_max = tmp_col_max;
                x.mv_row_min = tmp_row_min;
                x.mv_row_max = tmp_row_max;

                if (bestsme < Integer.MAX_VALUE) {
                    /* TODO: use dis in distortion calculation later. */
                    VarianceResults res = new VarianceResults();
                    cpi.find_fractional_mv_step.call(x, b, d, d.bmi.mv, mvDetails.best_ref_mv, x.errorperbit,
                            cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), x.mvcost, res);
                }

                mvDetails.mode_mv[MBPredictionMode.NEWMV.ordinal()].set(d.bmi.mv);

                /* Add the new motion vector cost to our rolling cost variable */
                rdy.rateBase += MComp.vp8_mv_bit_cost(mvDetails.mode_mv[MBPredictionMode.NEWMV.ordinal()],
                        mvDetails.best_ref_mv, x.mvcost, 96);
            }
// fall through

            case NEARESTMV:
            case NEARMV:
                /*
                 * Clip "next_nearest" so that it does not extend to far out of image
                 */
                FindNearMV.vp8_clamp_mv2(mvDetails.mode_mv[this_mode.ordinal()], xd);

                /*
                 * Do not bother proceeding if the vector (from newmv, nearest or near) is 0,0
                 * as this should then be coded using the zeromv mode.
                 */
                if (((this_mode == MBPredictionMode.NEARMV) || (this_mode == MBPredictionMode.NEARESTMV))
                        && (mvDetails.mode_mv[this_mode.ordinal()].isZero())) {
                    continue;
                }
// fall through

            case ZEROMV:

                /*
                 * Trap vectors that reach beyond the UMV borders Note that ALL New MV, Nearest
                 * MV Near MV and Zero MV code drops through to this point because of the lack
                 * of break statements in the previous two cases.
                 */
                if (((mvDetails.mode_mv[this_mode.ordinal()].row >> 3) < x.mv_row_min)
                        || ((mvDetails.mode_mv[this_mode.ordinal()].row >> 3) > x.mv_row_max)
                        || ((mvDetails.mode_mv[this_mode.ordinal()].col >> 3) < x.mv_col_min)
                        || ((mvDetails.mode_mv[this_mode.ordinal()].col >> 3) > x.mv_col_max)) {
                    continue;
                }

                vp8_set_mbmode_and_mvs(x, this_mode, mvDetails.mode_mv[this_mode.ordinal()]);
                this_rd = evaluate_inter_mode_rd(mvDetails.mdCounts, rdy, rduvComplete, disable_skip, cpi, x);
                break;

            default:
                break;
            }

            this_rd = calculate_final_rd_costs(this_rd, rdy, rduvComplete, other_cost, disable_skip[0], uv_intra_tteob,
                    intra_rd_penalty, cpi, x);

            /* Keep record of best intra distortion */
            if ((mi.mbmi.ref_frame == MVReferenceFrame.INTRA_FRAME) && (this_rd < best_mode.intra_rd)) {
                best_mode.intra_rd = this_rd;
                ret.intra = rdy.distortion;
            }

            /* Did this mode help.. i.i is it the new best mode */
            if (this_rd < best_mode.rd || x.skip) {
                /* Note index of best mode so far */
                best_mode_index = mode_index;
                ret.rate = rdy.rateBase;
                ret.distortion = rdy.distortion;
                if (MBPredictionMode.basicModes.contains(this_mode)) {
                    mi.mbmi.uv_mode = uv_intra_mode;
                    /* required for left and above block mv */
                    mi.mbmi.mv.setZero();
                }
                update_best_mode(best_mode, this_rd, rdy, rduvComplete, other_cost.get(), x);

                /*
                 * Testing this mode gave rise to an improvement in best error score. Lower
                 * threshold a bit for next time
                 */
                x.rd_thresh_mult[mode_index] = (x.rd_thresh_mult[mode_index] >= (OnyxInt.MIN_THRESHMULT + 2))
                        ? x.rd_thresh_mult[mode_index] - 2
                        : OnyxInt.MIN_THRESHMULT;
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
            }
            x.rd_threshes[mode_index] = (cpi.rd_baseline_thresh[mode_index] >> 7) * x.rd_thresh_mult[mode_index];

            if (x.skip)
                break;
        }

        reduceActivationThreshold(cpi, best_mode_index);

        if (adjustToZeroMVForAltref(cpi, best_mode.mbmode))
            return;

        /* macroblock modes */
        mi.mbmi.copyIn(best_mode.mbmode);

        if (best_mode.mbmode.mode == MBPredictionMode.B_PRED) {
            for (i = 0; i < 16; ++i) {
                mi.bmi[i].as_mode(best_mode.bmodes[i].as_mode());
            }
        }

        if (best_mode.mbmode.mode == MBPredictionMode.SPLITMV) {
            for (i = 0; i < 16; ++i) {
                mi.bmi[i].mv.set(best_mode.bmodes[i].mv);
            }
            x.partition_info.get().copyin(best_mode.partition);

            mi.mbmi.mv.set(x.partition_info.get().bmi[15].mv);
        }

        if (sign_bias != cpi.common.ref_frame_sign_bias.get(mi.mbmi.ref_frame)) {
            mvDetails.best_ref_mv.set(mvDetails.best_ref_mv_sb[sign_bias ? 0 : 1]);
        }

        rd_update_mvcount(x, mvDetails.best_ref_mv);
    }

    static void vp8_set_mbmode_and_mvs(Macroblock x, MBPredictionMode mb, MV mv) {
        ModeInfo mi = x.e_mbd.mode_info_context.get();
        mi.mbmi.mode = mb;
        mi.mbmi.mv.set(mv);
    }

    static long vp8_rd_pick_intra_mode(Macroblock x) {
        ModeInfo mi = x.e_mbd.mode_info_context.get();
        mi.mbmi.ref_frame = MVReferenceFrame.INTRA_FRAME;
        QualityMetrics resUV = new QualityMetrics();
        rd_pick_intra_mbuv_mode(x, resUV);
        QualityMetrics res16 = new QualityMetrics();
        rd_pick_intra16x16mby_mode(x, res16);
        QualityMetrics res4 = new QualityMetrics();
        res4.error = res16.error;
        rd_pick_intra4x4mby_modes(x, res4);
        if (res4.error < res16.error) {
            mi.mbmi.mode = MBPredictionMode.B_PRED;
            res16.rateComp = res4.rateComp;
        }

        return resUV.rateComp + res16.rateComp;
    }

    static void insertsortsad(long arr[], int idx[], int len) {
        int i, j, k;

        for (i = 1; i <= len - 1; ++i) {
            for (j = 0; j < i; ++j) {
                if (arr[j] > arr[i]) {
                    long temp;
                    int tempi;

                    temp = arr[i];
                    tempi = idx[i];

                    for (k = i; k > j; k--) {
                        arr[k] = arr[k - 1];
                        idx[k] = idx[k - 1];
                    }

                    arr[j] = temp;
                    idx[j] = tempi;
                }
            }
        }
    }

    static void insertsortmv(int arr[], int len) {
        int i, j, k;

        for (i = 1; i <= len - 1; ++i) {
            for (j = 0; j < i; ++j) {
                if (arr[j] > arr[i]) {
                    int temp;

                    temp = arr[i];

                    for (k = i; k > j; k--)
                        arr[k] = arr[k - 1];

                    arr[j] = temp;
                }
            }
        }
    }

    /* The improved MV prediction */
    static int vp8_mv_pred(Compressor cpi, MacroblockD xd, FullAccessGenArrPointer<ModeInfo> herePtr, MV mvp,
            MVReferenceFrame refframe, EnumMap<MVReferenceFrame, Boolean> ref_frame_sign_bias, int sr,
            int[] near_sadidx) {
        ModeInfo here = herePtr.get();
        ModeInfo above = herePtr.getRel(-xd.mode_info_stride);
        ModeInfo left = herePtr.getRel(-1);
        ModeInfo aboveleft = herePtr.getRel(-xd.mode_info_stride - 1);
        ModeInfo[] around = { above, left, aboveleft };
        MV[] near_mvs = new MV[8];
        MVReferenceFrame[] near_ref = new MVReferenceFrame[8];
        MV mv = new MV();
        int vcnt = 0;
        boolean find = false;
        int mb_offset;

        int[] mvx = new int[8];
        int[] mvy = new int[8];
        int i;

        if (here.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
            for (int j = 0; j < near_mvs.length; j++) {
                near_mvs[j] = new MV();
                near_ref[j] = MVReferenceFrame.INTRA_FRAME;
            }

            /*
             * read in 3 nearby block's MVs from current frame as prediction candidates.
             */
            for (int j = 0; j < around.length; j++, vcnt++) {
                if (around[j].mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
                    near_mvs[vcnt].set(around[j].mbmi.mv);
                    FindNearMV.mv_bias(ref_frame_sign_bias.get(around[j].mbmi.ref_frame), refframe, near_mvs[vcnt],
                            ref_frame_sign_bias);
                    near_ref[vcnt] = around[j].mbmi.ref_frame;
                }
            }

            /* read in 5 nearby block's MVs from last frame. */
            if (cpi.common.last_frame_type != FrameType.KEY_FRAME) {
                mb_offset = (-xd.mb_to_top_edge / 128 + 1) * (xd.mode_info_stride + 1)
                        + (-xd.mb_to_left_edge / 128 + 1);

                int[] shiftsForNearby = { 0 // current in last frame
                        , -xd.mode_info_stride - 1 // above in last frame
                        , -1 // left in last frame
                        , 1 // right in last frame
                        , xd.mode_info_stride + 1 // below in last frame
                };

                for (int j = 0; j < shiftsForNearby.length; j++, vcnt++) {
                    if (cpi.lf_ref_frame[mb_offset + shiftsForNearby[j]] != MVReferenceFrame.INTRA_FRAME) {
                        near_mvs[vcnt].set(cpi.lfmv[mb_offset + shiftsForNearby[j]]);
                        FindNearMV.mv_bias(cpi.lf_ref_frame_sign_bias[mb_offset + shiftsForNearby[j]], refframe,
                                near_mvs[vcnt], ref_frame_sign_bias);
                        near_ref[vcnt] = cpi.lf_ref_frame[mb_offset + shiftsForNearby[j]];
                    }
                }
            }

            for (i = 0; i < vcnt; ++i) {
                if (near_ref[near_sadidx[i]] != MVReferenceFrame.INTRA_FRAME) {
                    if (here.mbmi.ref_frame == near_ref[near_sadidx[i]]) {
                        mv.set(near_mvs[near_sadidx[i]]);
                        find = true;
                        if (i < 3) {
                            sr = 3;
                        } else {
                            sr = 2;
                        }
                        break;
                    }
                }
            }

            if (!find) {
                for (i = 0; i < vcnt; ++i) {
                    mvx[i] = near_mvs[i].row;
                    mvy[i] = near_mvs[i].col;
                }

                insertsortmv(mvx, vcnt);
                insertsortmv(mvy, vcnt);
                mv.row = (short) mvx[vcnt / 2];
                mv.col = (short) mvy[vcnt / 2];

                /*
                 * sr is set to 0 to allow calling function to decide the search range.
                 */
                sr = 0;
            }
        }

        /* Set up return values */
        mvp.set(mv);
        FindNearMV.vp8_clamp_mv2(mvp, xd);
        return sr;
    }

    static void vp8_cal_sad(Compressor cpi, MacroblockD xd, Macroblock x, int recon_yoffset, int[] near_sadidx) {
        /*
         * near_sad indexes: 0-cf above, 1-cf left, 2-cf aboveleft, 3-lf current, 4-lf
         * above, 5-lf left, 6-lf right, 7-lf below
         */
        long[] near_sad = new long[8];
        Block b = x.block.get();
        FullAccessIntArrPointer src_y_ptr = b.base_src;

        /* calculate sad for current frame 3 nearby MBs. */
        if (xd.mb_to_top_edge == 0 && xd.mb_to_left_edge == 0) {
            near_sad[0] = near_sad[1] = near_sad[2] = Integer.MAX_VALUE;
        } else if (xd.mb_to_top_edge == 0) { /* only has left MB for sad calculation. */
            near_sad[0] = near_sad[2] = Integer.MAX_VALUE;
            near_sad[1] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                    xd.dst.y_buffer.shallowCopyWithPosInc(-16), xd.dst.y_stride);
        } else if (xd.mb_to_left_edge == 0) { /* only has left MB for sad calculation. */
            near_sad[1] = near_sad[2] = Integer.MAX_VALUE;
            near_sad[0] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                    xd.dst.y_buffer.shallowCopyWithPosInc(-xd.dst.y_stride * 16), xd.dst.y_stride);
        } else {
            near_sad[0] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                    xd.dst.y_buffer.shallowCopyWithPosInc(-xd.dst.y_stride * 16), xd.dst.y_stride);
            near_sad[1] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                    xd.dst.y_buffer.shallowCopyWithPosInc(-16), xd.dst.y_stride);
            near_sad[2] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                    xd.dst.y_buffer.shallowCopyWithPosInc(-xd.dst.y_stride * 16 - 16), xd.dst.y_stride);
        }

        if (cpi.common.last_frame_type != FrameType.KEY_FRAME) {
            /* calculate sad for last frame 5 nearby MBs. */
            int lfrIdx = cpi.common.frameIdxs.get(MVReferenceFrame.LAST_FRAME);
            FullAccessIntArrPointer pre_y_buffer = cpi.common.yv12_fb[lfrIdx].y_buffer
                    .shallowCopyWithPosInc(recon_yoffset);
            int pre_y_stride = cpi.common.yv12_fb[lfrIdx].y_stride;

            if (xd.mb_to_top_edge == 0)
                near_sad[4] = Integer.MAX_VALUE;
            if (xd.mb_to_left_edge == 0)
                near_sad[5] = Integer.MAX_VALUE;
            if (xd.mb_to_right_edge == 0)
                near_sad[6] = Integer.MAX_VALUE;
            if (xd.mb_to_bottom_edge == 0)
                near_sad[7] = Integer.MAX_VALUE;

            if (near_sad[4] != Integer.MAX_VALUE) {
                near_sad[4] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                        pre_y_buffer.shallowCopyWithPosInc(-pre_y_stride * 16), pre_y_stride);
            }
            if (near_sad[5] != Integer.MAX_VALUE) {
                near_sad[5] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                        pre_y_buffer.shallowCopyWithPosInc(-16), pre_y_stride);
            }
            near_sad[3] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride, pre_y_buffer,
                    pre_y_stride);
            if (near_sad[6] != Integer.MAX_VALUE) {
                near_sad[6] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                        pre_y_buffer.shallowCopyWithPosInc(16), pre_y_stride);
            }
            if (near_sad[7] != Integer.MAX_VALUE) {
                near_sad[7] = cpi.fn_ptr.get(BlockEnum.BLOCK_16X16).sdf.call(src_y_ptr, b.src_stride,
                        pre_y_buffer.shallowCopyWithPosInc(pre_y_stride * 16), pre_y_stride);
            }
        }

        if (cpi.common.last_frame_type != FrameType.KEY_FRAME) {
            insertsortsad(near_sad, near_sadidx, 8);
        } else {
            insertsortsad(near_sad, near_sadidx, 3);
        }
    }

    static int vp8_cost_mv_ref(MBPredictionMode m, int[] near_mv_ref_ct) {
        short[] p = new short[BlockD.VP8_MVREFS - 1];
        assert (MBPredictionMode.NEARESTMV.ordinal() <= m.ordinal()
                && m.ordinal() <= MBPredictionMode.SPLITMV.ordinal());
        FindNearMV.vp8_mv_ref_probs(p, near_mv_ref_ct);
        return TreeWriter.vp8_cost_token(EntropyMode.vp8_mv_ref_tree, new ReadOnlyIntArrPointer(p, 0),
                Token.vp8_mv_ref_encoding_array[(m.ordinal() - MBPredictionMode.NEARESTMV.ordinal())]);
    }

    static int VP8_UVSSE(Compressor cpi) {
        Macroblock x = cpi.mb;
        FullAccessIntArrPointer uptr, vptr;
        FullAccessIntArrPointer upred_ptr = x.block.getRel(16).getSrcPtr();
        FullAccessIntArrPointer vpred_ptr = x.block.getRel(20).getSrcPtr();
        int uv_stride = x.block.getRel(16).src_stride;

        VarianceResults ret1 = new VarianceResults();
        VarianceResults ret2 = new VarianceResults();
        ModeInfo mi = x.e_mbd.mode_info_context.get();
        int mv_row = mi.mbmi.mv.row;
        int mv_col = mi.mbmi.mv.col;
        int offset;
        int pre_stride = x.e_mbd.pre.uv_stride;

        if (mv_row < 0) {
            mv_row -= 1;
        } else {
            mv_row += 1;
        }

        if (mv_col < 0) {
            mv_col -= 1;
        } else {
            mv_col += 1;
        }

        mv_row /= 2;
        mv_col /= 2;

        offset = (mv_row >> 3) * pre_stride + (mv_col >> 3);
        uptr = x.e_mbd.pre.u_buffer.shallowCopyWithPosInc(offset);
        vptr = x.e_mbd.pre.v_buffer.shallowCopyWithPosInc(offset);

        if (((mv_row | mv_col) & 7) != 0) {
            VarianceFNs.SVF svfn = cpi.varFns.default_fn_ptr.get(BlockEnum.BLOCK_8X8).svf;
            svfn.call(uptr, pre_stride, mv_col & 7, mv_row & 7, upred_ptr, uv_stride, ret2);
            svfn.call(vptr, pre_stride, mv_col & 7, mv_row & 7, vpred_ptr, uv_stride, ret1);
        } else {
            Variance.variance(uptr, pre_stride, upred_ptr, uv_stride, ret2, 8, 8);
            Variance.variance(vptr, pre_stride, vpred_ptr, uv_stride, ret1, 8, 8);
        }

        return ret2.sse + ret1.sse;
    }

    static void rd_inter16x16_uv(Compressor cpi, Macroblock x, QualityMetrics ret, boolean fullpixel) {
        ReconInter.vp8_build_inter16x16_predictors_mbuv(x.e_mbd);
        EncodeMB.vp8_subtract_mbuv(x.src_diff, x.src.u_buffer, x.src.v_buffer, x.src.uv_stride,
                x.e_mbd.getFreshUPredPtr(), x.e_mbd.getFreshVPredPtr(), 8);

        EncodeMB.vp8_transform_mbuv(x);
        Quantize.vp8_quantize_mbuv(x);
        ret.rateBase = rd_cost_mbuv(x);
        ret.distortion = vp8_mbuverror(x) >> 4;

        ret.error = RDCOST(x.rdmult, x.rddiv, ret.rateBase, ret.distortion);
    }

    static long evaluate_inter_mode_rd(int[] mdcounts, QualityMetrics rdy, QualityMetrics rduv, boolean[] disable_skip, Compressor cpi,
            Macroblock x) {
        MBPredictionMode this_mode = x.e_mbd.mode_info_context.get().mbmi.mode;
        Block b = x.block.get();
        MacroblockD xd = x.e_mbd;
        ReconInter.vp8_build_inter16x16_predictors_mby(x.e_mbd, x.e_mbd.predictor, 16);

        if (cpi.active_map_enabled && x.active_ptr.get() == 0) {
            x.skip = true;
        } else if (x.encode_breakout != 0) {
            VarianceResults res = new VarianceResults();
            int threshold = (xd.block.get().dequant.getRel(1) * xd.block.get().dequant.getRel(1) >> 4);

            if (threshold < x.encode_breakout)
                threshold = x.encode_breakout;

            Variance.variance(b.base_src, b.src_stride, x.e_mbd.predictor, 16, res, 16, 16);

            if (res.sse < threshold) {
                int q2dc = xd.block.getRel(24).dequant.get();
                /*
                 * If theres is no codeable 2nd order dc or a very small uniform pixel change
                 * change
                 */
                if ((res.sse - res.variance < q2dc * q2dc >> 4)
                        || (res.sse / 2 > res.variance && res.sse - res.variance < 64)) {
                    /* Check u and v to make sure skip is ok */
                    int sse2 = VP8_UVSSE(cpi);
                    if ((sse2 << 1) < threshold) {
                        x.skip = true;
                        rdy.distortion = res.sse + sse2;
                        rdy.rateBase = 500;

                        /* for best_yrd calculation */
                        rduv.rateBase = 0;
                        rduv.distortion = sse2;

                        disable_skip[0] = true;
                        return RDCOST(x.rdmult, x.rddiv, rdy.rateBase, rdy.distortion);
                    }
                }
            }
        }

        /* Add in the Mv/mode cost */
        rdy.rateBase += vp8_cost_mv_ref(this_mode, mdcounts);

        /* Y cost and distortion */
        QualityMetrics rde = new QualityMetrics();
        macro_block_yrd(x, rde);
        rdy.rateBase += rde.rateComp;
        rdy.distortion += rde.distortion;

        /* UV cost and distortion */
        rd_inter16x16_uv(cpi, x, rde, cpi.common.full_pixel);
        rduv.rateBase += rde.rateComp;
        rdy.distortion += rde.distortion;
        return Integer.MAX_VALUE;
    }

    static long calculate_final_rd_costs(long this_rd, QualityMetrics rdy, QualityMetrics rduv, AtomicInteger other_cost,
            boolean disable_skip, int uv_intra_tteob, int intra_rd_penalty, Compressor cpi, Macroblock x) {
        ModeInfo mi = x.e_mbd.mode_info_context.get();
        MBPredictionMode this_mode = mi.mbmi.mode;

        /*
         * Where skip is allowable add in the default per mb cost for the no skip case.
         * where we then decide to skip we have to delete this and replace it with the
         * cost of signalling a skip
         */
        if (cpi.common.mb_no_coeff_skip) {
            other_cost.addAndGet(TreeWriter.vp8_cost_bit(cpi.prob_skip_false, 0));
            rdy.rateBase += other_cost.get();
        }

        /*
         * Estimate the reference frame signaling cost and add it to the rolling cost
         * variable.
         */
        rdy.rateBase += x.ref_frame_cost[mi.mbmi.ref_frame.ordinal()];

        if (!disable_skip) {
            /*
             * Test for the condition where skip block will be activated because there are
             * no non zero coefficients and make any necessary adjustment for rate
             */
            if (cpi.common.mb_no_coeff_skip) {
                int i;
                int tteob;
                boolean has_y2_block = !MBPredictionMode.has_no_y_block.contains(this_mode);

                tteob = 0;
                if (has_y2_block)
                    tteob += x.e_mbd.eobs.getRel(24);

                for (i = 0; i < 16; ++i)
                    if (x.e_mbd.eobs.getRel(i) > (has_y2_block ? 1 : 0))
                        tteob++;

                if (mi.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
                    for (i = 16; i < 24; ++i)
                        tteob += x.e_mbd.eobs.getRel(i);
                } else {
                    tteob += uv_intra_tteob;
                }

                if (tteob == 0) {
                    rdy.rateBase -= rdy.rateComp + rduv.rateComp;
                    /* for best_yrd calculation */
                    rduv.rateComp = 0;

                    /* Back out no skip flag costing and add in skip flag costing */
                    if (cpi.prob_skip_false != 0) {
                        int prob_skip_cost;

                        prob_skip_cost = TreeWriter.vp8_cost_bit(cpi.prob_skip_false, 1);
                        prob_skip_cost -= (int) TreeWriter.vp8_cost_bit(cpi.prob_skip_false, 0);
                        rdy.rateBase += prob_skip_cost;
                        other_cost.addAndGet(prob_skip_cost);
                    }
                }
            }
            /* Calculate the final RD estimate for this mode */
            this_rd = RDCOST(x.rdmult, x.rddiv, rdy.rateBase, rdy.distortion);
            if (this_rd < Integer.MAX_VALUE && mi.mbmi.ref_frame == MVReferenceFrame.INTRA_FRAME) {
                this_rd += intra_rd_penalty;
            }
        }
        return this_rd;
    }

    static void update_best_mode(BestMode best_mode, long this_rd, QualityMetrics rdy, QualityMetrics rduv, int other_cost, Macroblock x) {
        ModeInfo mi = x.e_mbd.mode_info_context.get();
        MBPredictionMode this_mode = mi.mbmi.mode;

        other_cost += x.ref_frame_cost[mi.mbmi.ref_frame.ordinal()];

        /* Calculate the final y RD estimate for this mode */
        best_mode.yrd = RDCOST(x.rdmult, x.rddiv, (rdy.rateComp - rduv.rateComp - other_cost),
                (rdy.distortion - rduv.distortion));

        best_mode.rd = this_rd;
        best_mode.mbmode.copyIn(mi.mbmi);
        best_mode.partition.copyin(x.partition_info.get());

        if (MBPredictionMode.has_no_y_block.contains(this_mode)) {
            int i;
            for (i = 0; i < 16; ++i) {
                best_mode.bmodes[i] = x.e_mbd.block.getRel(i).bmi;
            }
        }
    }

    static void rd_update_mvcount(Macroblock x, MV best_ref_mv) {
        ModeInfo mi = x.e_mbd.mode_info_context.get();
        if (mi.mbmi.mode == MBPredictionMode.SPLITMV) {
            int i;

            for (i = 0; i < x.partition_info.get().count; ++i) {
                if (x.partition_info.get().bmi[i].mode == BPredictionMode.NEW4X4) {
                    x.MVcount[0][EntropyMV.mv_max + ((x.partition_info.get().bmi[i].mv.row - best_ref_mv.row) >> 1)]++;
                    x.MVcount[1][EntropyMV.mv_max + ((x.partition_info.get().bmi[i].mv.col - best_ref_mv.col) >> 1)]++;
                }
            }
        } else if (mi.mbmi.mode == MBPredictionMode.NEWMV) {
            x.MVcount[0][EntropyMV.mv_max + ((mi.mbmi.mv.row - best_ref_mv.row) >> 1)]++;
            x.MVcount[1][EntropyMV.mv_max + ((mi.mbmi.mv.col - best_ref_mv.col) >> 1)]++;
        }
    }

    static void vp8_auto_select_speed(Compressor cpi) {
        int milliseconds_for_compress = (int) (1000000 / cpi.framerate);

        milliseconds_for_compress = milliseconds_for_compress * (16 - cpi.oxcf.getCpu_used()) / 16;

        if (cpi.avg_pick_mode_time < milliseconds_for_compress
                && (cpi.avg_encode_time - cpi.avg_pick_mode_time) < milliseconds_for_compress) {
            if (cpi.avg_pick_mode_time == 0) {
                cpi.Speed = 4;
            } else {
                if (milliseconds_for_compress * 100 < cpi.avg_encode_time * 95) {
                    cpi.Speed += 2;
                    cpi.avg_pick_mode_time = 0;
                    cpi.avg_encode_time = 0;

                    if (cpi.Speed > 16) {
                        cpi.Speed = 16;
                    }
                }

                if (milliseconds_for_compress * 100 > cpi.avg_encode_time * auto_speed_thresh[cpi.Speed]) {
                    cpi.Speed -= 1;
                    cpi.avg_pick_mode_time = 0;
                    cpi.avg_encode_time = 0;

                    /* In real-time mode, cpi.speed is in [4, 16]. */
                    if (cpi.Speed < 4) {
                        cpi.Speed = 4;
                    }
                }
            }
        } else {
            cpi.Speed += 4;

            if (cpi.Speed > 16)
                cpi.Speed = 16;

            cpi.avg_pick_mode_time = 0;
            cpi.avg_encode_time = 0;
        }
    }

    static void fill_token_costs(int[][][][] c, short[][][][] p) {
        int i, j, k;

        for (i = 0; i < Entropy.BLOCK_TYPES; ++i) {
            for (j = 0; j < Entropy.COEF_BANDS; ++j) {
                for (k = 0; k < Entropy.PREV_COEF_CONTEXTS; ++k) {
                    /*
                     * check for pt=0 and band > 1 if block type 0 and 0 if blocktype 1
                     */
                    if (k == 0 && j > (i == 0 ? 1 : 0)) {
                        TreeWriter.vp8_cost_tokens2(c[i][j][k], new ReadOnlyIntArrPointer(p[i][j][k], 0),
                                Entropy.vp8_coef_tree, 2);
                    } else {
                        TreeWriter.vp8_cost_tokens(c[i][j][k], new ReadOnlyIntArrPointer(p[i][j][k], 0),
                                Entropy.vp8_coef_tree);
                    }
                }
            }
        }
    }

    static void vp8_initialize_rd_consts(Compressor cpi, Macroblock x, int Qvalue) {
        int q;
        int i;
        double capped_q = (Qvalue < 160) ? (double) Qvalue : 160.0;
        double rdconst = 2.80;

        /*
         * Further tests required to see if optimum is different for key frames, golden
         * frames and arf frames.
         */
        cpi.RDMULT = (int) (rdconst * (capped_q * capped_q));

        /* Extend rate multiplier along side quantizer zbin increases */
        if (cpi.mb.zbin_over_quant > 0) {
            double oq_factor;
            double modq;
            /*
             * Experimental code using the same basic equation as used for Q above The units
             * of cpi.mb.zbin_over_quant are 1/128 of Q bin size
             */
            oq_factor = 1.0 + ((double) 0.0015625 * cpi.mb.zbin_over_quant);
            modq = (int) ((double) capped_q * oq_factor);
            cpi.RDMULT = (int) (rdconst * (modq * modq));
        }

        cpi.mb.errorperbit = Math.max((cpi.RDMULT / 110), 1);

        OnyxIf.vp8_set_speed_features(cpi);

        for (i = 0; i < Block.MAX_MODES; ++i) {
            x.mode_test_hit_counts[i] = 0;
        }

        q = (int) Math.pow(Qvalue, 1.25);

        if (q < 8)
            q = 8;

        if (cpi.RDMULT > 1000) {
            cpi.RDDIV = 1;
            cpi.RDMULT /= 100;

            for (i = 0; i < Block.MAX_MODES; ++i) {
                if (cpi.sf.thresh_mult[i] < Integer.MAX_VALUE) {
                    x.rd_threshes[i] = cpi.sf.thresh_mult[i] * q / 100;
                } else {
                    x.rd_threshes[i] = Integer.MAX_VALUE;
                }

                cpi.rd_baseline_thresh[i] = x.rd_threshes[i];
            }
        } else {
            cpi.RDDIV = 100;

            for (i = 0; i < Block.MAX_MODES; ++i) {
                if (cpi.sf.thresh_mult[i] < (Integer.MAX_VALUE / q)) {
                    x.rd_threshes[i] = cpi.sf.thresh_mult[i] * q;
                } else {
                    x.rd_threshes[i] = Integer.MAX_VALUE;
                }

                cpi.rd_baseline_thresh[i] = x.rd_threshes[i];
            }
        }

        {
            /* build token cost array for the type of frame we have now */
            FrameContext l = cpi.lfc_n;

            if (cpi.common.refresh_alt_ref_frame) {
                l = cpi.lfc_a;
            } else if (cpi.common.refresh_golden_frame) {
                l = cpi.lfc_g;
            }

            fill_token_costs(cpi.mb.token_costs, l.coef_probs);
            /*
             * fill_token_costs( cpi.mb.token_costs, (const vp8_prob( *)[8][3][11])
             * cpi.common.fc.coef_probs);
             */

            /* TODO make these mode costs depend on last,alt or gold too. (jbb) */
            cpi.rd_costs.vp8_init_mode_costs(cpi);
        }
    }

    static void reduceActivationThreshold(final Compressor cpi, final int best_mode_index) {
        /* Reduce the activation RD thresholds for the best choice mode */
        if ((cpi.rd_baseline_thresh[best_mode_index] > 0)
                && (cpi.rd_baseline_thresh[best_mode_index] < (Integer.MAX_VALUE >> 2))) {
            final Macroblock x = cpi.mb;
            final int best_adjustment = (x.rd_thresh_mult[best_mode_index] >> 3);
            x.rd_thresh_mult[best_mode_index] = (x.rd_thresh_mult[best_mode_index] >= (OnyxInt.MIN_THRESHMULT
                    + best_adjustment)) ? x.rd_thresh_mult[best_mode_index] - best_adjustment : OnyxInt.MIN_THRESHMULT;
            x.rd_threshes[best_mode_index] = (cpi.rd_baseline_thresh[best_mode_index] >> 7)
                    * x.rd_thresh_mult[best_mode_index];
        }
    }

    static boolean adjustToZeroMVForAltref(final Compressor cpi, MBModeInfo best_mbmode) {
        if (cpi.is_src_frame_alt_ref && (best_mbmode.mode != MBPredictionMode.ZEROMV
                || best_mbmode.ref_frame != MVReferenceFrame.ALTREF_FRAME)) {
            final Macroblock x = cpi.mb;
            ModeInfo mi = x.e_mbd.mode_info_context.get();
            mi.mbmi.mode = MBPredictionMode.ZEROMV;
            mi.mbmi.ref_frame = MVReferenceFrame.ALTREF_FRAME;
            mi.mbmi.mv.setZero();
            mi.mbmi.uv_mode = MBPredictionMode.DC_PRED;
            mi.mbmi.mb_skip_coeff = (cpi.common.mb_no_coeff_skip);
            mi.mbmi.partitioning = BlockEnum.BLOCK_16X8;

            return true;
        }
        return false;

    }

}
