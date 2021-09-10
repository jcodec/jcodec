package org.jcodec.codecs.vpx.vp8;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.VP8Util;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.CoefUpdateProbs;
import org.jcodec.codecs.vpx.vp8.data.DefaultCoefCounts;
import org.jcodec.codecs.vpx.vp8.data.Entropy;
import org.jcodec.codecs.vpx.vp8.data.EntropyMode;
import org.jcodec.codecs.vpx.vp8.data.FrameContext;
import org.jcodec.codecs.vpx.vp8.data.MBModeInfo;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.MVContext;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.ReferenceCounts;
import org.jcodec.codecs.vpx.vp8.data.Token;
import org.jcodec.codecs.vpx.vp8.data.TokenExtra;
import org.jcodec.codecs.vpx.vp8.data.TokenList;
import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.Header;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.LoopFilterType;
import org.jcodec.codecs.vpx.vp8.enums.MBLvlFeatures;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.Sumvfref;
import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;
import org.jcodec.codecs.vpx.vp8.enums.TokenPartition;
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
public class BitStream {
    public final static int[] vp8cx_base_skip_false_prob = { 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
            255, 255, 251, 248, 244, 240, 236, 232, 229, 225, 221, 217, 213, 208, 204, 199, 194, 190, 187, 183, 179,
            175, 172, 168, 164, 160, 157, 153, 149, 145, 142, 138, 134, 130, 127, 124, 120, 117, 114, 110, 107, 104,
            101, 98, 95, 92, 89, 86, 83, 80, 77, 74, 71, 68, 65, 62, 59, 56, 53, 50, 47, 44, 41, 38, 35, 32, 30, 28, 26,
            24, 22, 20, 18, 16, };
    final static Token[] vp8_coef_encodings;

    static {
        vp8_coef_encodings = new Token[TokenAlphabet.entropyTokenCount];
        int i = 0;
        for (TokenAlphabet ta : TokenAlphabet.values()) {
            vp8_coef_encodings[i++] = ta.coefEncoding;
        }
    }

    public static void update_mode(BoolEncoder w, int n, Token[] tok, ReadOnlyIntArrPointer tree, short[] Pnew,
            FullAccessIntArrPointer Pcur, /* unsigned */ int[][] bct, /* unsigned */ int[] num_events) {
        /* unsigned */ int new_b = 0, old_b = 0;
        int i = 0;

        TreeCoder.vp8_tree_probs_from_distribution(n--, tok, tree, Pnew, bct, num_events, 256, true);

        do {
            new_b += TreeWriter.vp8_cost_branch(bct[i], Pnew[i]);
            old_b += TreeWriter.vp8_cost_branch(bct[i], Pcur.getRel(i));
        } while (++i < n);

        if (new_b + (n << 8) < old_b) {
            int j = 0;

            w.vp8_write_bit(true);

            do {
                final short p = Pnew[j];

                TreeWriter.vp8_write_literal(w, (Pcur.setRel(j, p)) > 0 ? p : 1, 8);
            } while (++j < n);
        } else
            w.vp8_write_bit(false);
    }

    static void update_mbintra_mode_probs(Compressor cpi) {
        final CommonData x = cpi.common;

        final BoolEncoder w = cpi.bc[0];
        short[] Pnew = new short[BlockD.VP8_YMODES - 1];
        int[][] bct = new int[BlockD.VP8_YMODES - 1][2]; // uint

        update_mode(w, BlockD.VP8_YMODES, Token.vp8_ymode_encodings, EntropyMode.vp8_ymode_tree, Pnew, x.fc.ymode_prob,
                bct, cpi.mb.ymode_count);

        CommonUtils.vp8_zero(Pnew);
        CommonUtils.vp8_zero(bct);
        update_mode(w, BlockD.VP8_UV_MODES, Token.vp8_uv_mode_encodings, EntropyMode.vp8_uv_mode_tree, Pnew,
                x.fc.uv_mode_prob, bct, cpi.mb.uv_mode_count);
    }

    static void write_ymode(BoolEncoder bc, MBPredictionMode m, ReadOnlyIntArrPointer p) {
        TreeWriter.vp8_write_token(bc, EntropyMode.vp8_ymode_tree, p, Token.vp8_ymode_encodings[m.ordinal()]);
    }

    static void kfwrite_ymode(BoolEncoder bc, MBPredictionMode m, ReadOnlyIntArrPointer p) {
        TreeWriter.vp8_write_token(bc, EntropyMode.vp8_kf_ymode_tree, p, Token.vp8_kf_ymode_encodings[m.ordinal()]);
    }

    static void write_uv_mode(BoolEncoder bc, MBPredictionMode m, ReadOnlyIntArrPointer p) {
        TreeWriter.vp8_write_token(bc, EntropyMode.vp8_uv_mode_tree, p, Token.vp8_uv_mode_encodings[m.ordinal()]);
    }

    static void write_bmode(BoolEncoder bc, BPredictionMode m, ReadOnlyIntArrPointer p) {
        TreeWriter.vp8_write_token(bc, EntropyMode.vp8_bmode_tree, p, Token.vp8_bmode_encodings[m.ordinal()]);
    }

    static void write_split(BoolEncoder bc, int x) {
        TreeWriter.vp8_write_token(bc, EntropyMode.vp8_mbsplit_tree, EntropyMode.vp8_mbsplit_probs,
                Token.vp8_mbsplit_encodings[x]);
    }

    private static void encodeTokenPart(final BoolEncoder w, final int v, int n, int i,
            final ReadOnlyIntArrPointer probs, final ReadOnlyIntArrPointer tree) {
        do {
            final int bb = (v >> --n) & 1;
            w.vp8_encode_bool(bb != 0, probs.getRel(i >> 1));
            i = tree.getRel(i + bb);
        } while (n != 0);
    }

    static void vp8_pack_tokens(final BoolEncoder w, final TokenList p) {
        FullAccessGenArrPointer<TokenExtra> curr = p.start.shallowCopy();

        while (!curr.equals(p.stop)) {
            TokenExtra text = curr.get();
            TokenAlphabet t = text.Token;
            final int i;
            final int n;

            if (text.skip_eob_node) {
                n = t.coefEncoding.len - 1;
                i = 2;
            } else {
                n = t.coefEncoding.len;
                i = 0;
            }

            encodeTokenPart(w, t.coefEncoding.value, n, i, new ReadOnlyIntArrPointer(text.context_tree, 0),
                    Entropy.vp8_coef_tree);

            if (t.base_val != 0) {
                final int e = text.Extra, L = t.len;

                if (L != 0) {
                    /* number of bits in v2, assumed nonzero */
                    encodeTokenPart(w, e >> 1, L, 0, t.prob, t.tree);
                }
                w.vp8_encode_extra(e);
            }

            curr.inc();
        }
    }

    static void write_partition_size(FullAccessIntArrPointer cx_data, int size) {
        cx_data.set((short) (size & 0xff));
        cx_data.setRel(1, (short) ((size >> 8) & 0xff));
        cx_data.setRel(2, (short) ((size >> 16) & 0xff));
    }

    static void pack_tokens_into_partitions(Compressor cpi, FullAccessIntArrPointer cx_data, ReadOnlyIntArrPointer end,
            int num_part) {
        for (int i = 0; i < num_part; ++i) {
            int mb_row;

            BoolEncoder w = cpi.bc[i + 1];

            w.vp8_start_encode(cx_data, end);

            for (mb_row = i; mb_row < cpi.common.mb_rows; mb_row += num_part) {
                vp8_pack_tokens(w, cpi.tplist[mb_row]);
            }

            w.vp8_stop_encode();
            cx_data.incBy(w.getPos());
        }
    }

    static void write_mv_ref(BoolEncoder w, MBPredictionMode m, ReadOnlyIntArrPointer p) {
        assert (MBPredictionMode.NEARESTMV.ordinal() <= m.ordinal()
                && m.ordinal() <= MBPredictionMode.SPLITMV.ordinal());
        TreeWriter.vp8_write_token(w, EntropyMode.vp8_mv_ref_tree, p,
                Token.vp8_mv_ref_encoding_array[m.ordinal() - MBPredictionMode.NEARESTMV.ordinal()]);
    }

    static void write_sub_mv_ref(BoolEncoder w, BPredictionMode m, ReadOnlyIntArrPointer p) {
        assert (BPredictionMode.LEFT4X4.ordinal() <= m.ordinal() && m.ordinal() <= BPredictionMode.NEW4X4.ordinal());
        TreeWriter.vp8_write_token(w, EntropyMode.vp8_sub_mv_ref_tree, p,
                Token.vp8_sub_mv_ref_encoding_array[m.ordinal() - BPredictionMode.LEFT4X4.ordinal()]);
    }

    static void write_mv(BoolEncoder w, final MV mv, MV ref, final MVContext[] mvc) {
        MV e = ref.copy();
        e.row = (short) (mv.row - e.row);
        e.col = (short) (mv.col - e.col);
        EncodeMV.vp8_encode_motion_vector(w, e, mvc);
    }

    static void write_mb_features(BoolEncoder w, final MBModeInfo mi, final MacroblockD x) {
        /* Encode the MB segment id. */
        if (x.segmentation_enabled != 0 && x.update_mb_segmentation_map) {
            switch (mi.segment_id) {
            case 0:
                w.vp8_encode_bool(false, x.mb_segment_tree_probs[0]);
                w.vp8_encode_bool(false, x.mb_segment_tree_probs[1]);
                break;
            case 1:
                w.vp8_encode_bool(false, x.mb_segment_tree_probs[0]);
                w.vp8_encode_bool(true, x.mb_segment_tree_probs[1]);
                break;
            case 2:
                w.vp8_encode_bool(true, x.mb_segment_tree_probs[0]);
                w.vp8_encode_bool(false, x.mb_segment_tree_probs[2]);
                break;
            case 3:
                w.vp8_encode_bool(true, x.mb_segment_tree_probs[0]);
                w.vp8_encode_bool(true, x.mb_segment_tree_probs[2]);
                break;

            /* TRAP.. This should not happen */
            default:
                w.vp8_encode_bool(false, x.mb_segment_tree_probs[0]);
                w.vp8_encode_bool(false, x.mb_segment_tree_probs[1]);
                break;
            }
        }
    }

    static void vp8_convert_rfct_to_prob(final Compressor cpi) {
        cpi.vp8_convert_rfct_to_prob();
    }

    static void pack_inter_mode_mvs(final Compressor cpi) {
        final CommonData pc = cpi.common;
        final BoolEncoder w = cpi.bc[0];
        final MVContext[] mvc = pc.fc.mvc;
        int[] ct = new int[4];
        MV n1 = new MV(), n2 = new MV();
        MV best_mv = new MV();
        short[] mv_ref_p = new short[BlockD.VP8_MVREFS - 1];
        ReadOnlyIntArrPointer refpAsPointer = new ReadOnlyIntArrPointer(mv_ref_p, 0);

        FullAccessGenArrPointer<ModeInfo> m = pc.mi;
        m.savePos();
        final int mis = pc.mode_info_stride;
        int mb_row = -1;

        int prob_skip_false = 0;

        cpi.mb.partition_info = cpi.mb.pi.shallowCopy();

        cpi.vp8_convert_rfct_to_prob();

        if (pc.mb_no_coeff_skip) {
            int total_mbs = pc.mb_rows * pc.mb_cols;

            prob_skip_false = (total_mbs - cpi.mb.skip_true_count) * 256 / total_mbs;

            if (prob_skip_false <= 1)
                prob_skip_false = 1;

            if (prob_skip_false > 255)
                prob_skip_false = 255;

            cpi.prob_skip_false = prob_skip_false;
            TreeWriter.vp8_write_literal(w, prob_skip_false, 8);
        }

        TreeWriter.vp8_write_literal(w, cpi.prob_intra_coded, 8);
        TreeWriter.vp8_write_literal(w, cpi.prob_last_coded, 8);
        TreeWriter.vp8_write_literal(w, cpi.prob_gf_coded, 8);

        update_mbintra_mode_probs(cpi);

        EncodeMV.vp8_write_mvprobs(cpi);

        while (++mb_row < pc.mb_rows) {
            int mb_col = -1;

            while (++mb_col < pc.mb_cols) {
                MBModeInfo mi = m.get().mbmi;
                MVReferenceFrame rf = mi.ref_frame;
                MBPredictionMode mode = mi.mode;

                MacroblockD xd = cpi.mb.e_mbd;

                /*
                 * Distance of Mb to the various image edges. These specified to 8th pel as they
                 * are always compared to MV values that are in 1/8th pel units
                 */
                xd.mb_to_left_edge = -((mb_col * 16) << 3);
                xd.mb_to_right_edge = ((pc.mb_cols - 1 - mb_col) * 16) << 3;
                xd.mb_to_top_edge = -((mb_row * 16) << 3);
                xd.mb_to_bottom_edge = ((pc.mb_rows - 1 - mb_row) * 16) << 3;

                if (cpi.mb.e_mbd.update_mb_segmentation_map) {
                    write_mb_features(w, mi, cpi.mb.e_mbd);
                }

                if (pc.mb_no_coeff_skip) {
                    w.vp8_encode_bool(mi.mb_skip_coeff, prob_skip_false);
                }

                if (rf == MVReferenceFrame.INTRA_FRAME) {
                    w.vp8_encode_bool(false, cpi.prob_intra_coded);
                    write_ymode(w, mode, pc.fc.ymode_prob);

                    if (mode == MBPredictionMode.B_PRED) {
                        int j = 0;
                        do {
                            write_bmode(w, m.get().bmi[j].as_mode(), pc.fc.bmode_prob);
                        } while (++j < 16);
                    }

                    write_uv_mode(w, mi.uv_mode, pc.fc.uv_mode_prob);
                } else { /* inter coded */
                    CommonUtils.vp8_zero(mv_ref_p);
                    best_mv.setZero();
                    w.vp8_encode_bool(true, cpi.prob_intra_coded);

                    if (rf == MVReferenceFrame.LAST_FRAME)
                        w.vp8_encode_bool(false, cpi.prob_last_coded);
                    else {
                        w.vp8_encode_bool(true, cpi.prob_last_coded);
                        w.vp8_encode_bool(rf != MVReferenceFrame.GOLDEN_FRAME, cpi.prob_gf_coded);
                    }

                    {
                        n1.setZero();
                        n2.setZero();
                        CommonUtils.vp8_zero(ct);
                        FindNearMV.vp8_find_near_mvs(xd, m, n1, n2, best_mv, ct, rf, cpi.common.ref_frame_sign_bias);
                        FindNearMV.vp8_clamp_mv2(best_mv, xd);

                        FindNearMV.vp8_mv_ref_probs(mv_ref_p, ct);
                    }

                    write_mv_ref(w, mode, refpAsPointer);

                    switch (mode) /* new, split require MVs */
                    {
                    case NEWMV:
                        write_mv(w, mi.mv, best_mv, mvc);
                        break;

                    case SPLITMV: {
                        int j = 0;

                        write_split(w, mi.partitioning.ordinal());

                        do {
                            BPredictionMode blockmode;
                            MV blockmv;
                            final int[] L = EntropyMode.vp8_mbsplits[mi.partitioning.ordinal()];
                            int k = -1; /* first block in subset j */
                            Sumvfref mv_contz;
                            MV leftmv, abovemv;

                            blockmode = cpi.mb.partition_info.get().bmi[j].mode;
                            blockmv = cpi.mb.partition_info.get().bmi[j].mv.copy();
                            while (j != L[++k]) {
                                assert (k < 16);
                            }
                            leftmv = FindNearMV.left_block_mv(m, k);
                            abovemv = FindNearMV.above_block_mv(m, k, mis);
                            mv_contz = EntropyMode.vp8_mv_cont(leftmv, abovemv);

                            write_sub_mv_ref(w, blockmode, EntropyMode.vp8_sub_mv_ref_prob2.get(mv_contz));

                            if (blockmode == BPredictionMode.NEW4X4) {
                                write_mv(w, blockmv, best_mv, mvc);
                            }
                        } while (++j < cpi.mb.partition_info.get().count);
                        break;
                    }
                    default:
                        break;
                    }
                }

                m.inc();
                cpi.mb.partition_info.inc();
            }

            m.inc(); /* skip L prediction border */
            cpi.mb.partition_info.inc();
        }
        m.rewindToSaved();
    }

    static void write_kfmodes(Compressor cpi) {
        BoolEncoder bc = cpi.bc[0];
        CommonData c = cpi.common;
        /* const */
        FullAccessGenArrPointer<ModeInfo> m = c.mi;
        int mPos = c.mi.getPos();

        int mb_row = -1;
        int prob_skip_false = 0;

        if (c.mb_no_coeff_skip) {
            int total_mbs = c.mb_rows * c.mb_cols;

            prob_skip_false = (total_mbs - cpi.mb.skip_true_count) * 256 / total_mbs;

            if (prob_skip_false <= 1)
                prob_skip_false = 1;

            if (prob_skip_false >= 255)
                prob_skip_false = 255;

            cpi.prob_skip_false = prob_skip_false;
            TreeWriter.vp8_write_literal(bc, prob_skip_false, 8);
        }

        while (++mb_row < c.mb_rows) {
            int mb_col = -1;

            while (++mb_col < c.mb_cols) {
                final MBPredictionMode ym = m.get().mbmi.mode;

                if (cpi.mb.e_mbd.update_mb_segmentation_map) {
                    write_mb_features(bc, m.get().mbmi, cpi.mb.e_mbd);
                }

                if (c.mb_no_coeff_skip) {
                    bc.vp8_encode_bool(m.get().mbmi.mb_skip_coeff, prob_skip_false);
                }

                kfwrite_ymode(bc, ym, EntropyMode.vp8_kf_ymode_prob);

                if (ym == MBPredictionMode.B_PRED) {
                    final int mis = c.mode_info_stride;
                    int i = 0;

                    do {
                        BPredictionMode A = FindNearMV.above_block_mode(m, i, mis);
                        BPredictionMode L = FindNearMV.left_block_mode(m, i);
                        final BPredictionMode bm = m.get().bmi[i].as_mode();
                        write_bmode(bc, bm, new ReadOnlyIntArrPointer(
                                VP8Util.SubblockConstants.keyFrameSubblockModeProb[A.ordinal()][L.ordinal()], 0));
                    } while (++i < 16);
                }

                write_uv_mode(bc, m.getAndInc().mbmi.uv_mode, EntropyMode.vp8_kf_uv_mode_prob);
            }

            m.inc(); /* skip L prediction border */
        }
        m.setPos(mPos);
    }

    static void sum_probs_over_prev_coef_context(final int[][] probs, int[] out) {
        int i, j;
        for (i = 0; i < TokenAlphabet.entropyTokenCount; ++i) {
            for (j = 0; j < Entropy.PREV_COEF_CONTEXTS; ++j) {
                int tmp = out[i];
                out[i] += probs[j][i];
                /* check for wrap */
                if (out[i] < tmp)
                    out[i] = Integer.MAX_VALUE;
            }
        }
    }

    static int prob_update_savings(final int[] ct, final int oldp, final int newp, final int upd) {
        int old_b = TreeWriter.vp8_cost_branch(ct, oldp);
        int new_b = TreeWriter.vp8_cost_branch(ct, newp);
        int update_b = 8 + ((TreeWriter.vp8_cost_one(upd) - TreeWriter.vp8_cost_zero(upd)) >> 8);
        return old_b - new_b - update_b;
    }

    static int independent_coef_context_savings(Compressor cpi) {
        Macroblock x = cpi.mb;
        int savings = 0;
        int i = 0;
        int[] prev_coef_count_sum = new int[TokenAlphabet.entropyTokenCount];
        int[] prev_coef_savings = new int[TokenAlphabet.entropyTokenCount];
        do {
            int j = 0;
            do {
                int k = 0;
                CommonUtils.vp8_zero(prev_coef_savings);
                CommonUtils.vp8_zero(prev_coef_count_sum);
                int[][] probs;
                /*
                 * Calculate new probabilities given the constraint that they must be equal over
                 * the prev coef contexts
                 */

                probs = x.coef_counts[i][j];

                /* Reset to default probabilities at key frames */
                if (cpi.common.frame_type == FrameType.KEY_FRAME) {
                    probs = DefaultCoefCounts.default_coef_counts[i][j];
                }

                sum_probs_over_prev_coef_context(probs, prev_coef_count_sum);

                do {
                    /* at every context */

                    /* calc probs and branch cts for this frame only */
                    int t = 0; /* token/prob index */

                    TreeCoder.vp8_tree_probs_from_distribution(TokenAlphabet.entropyTokenCount, vp8_coef_encodings,
                            Entropy.vp8_coef_tree, cpi.frame_coef_probs[i][j][k], cpi.frame_branch_ct[i][j][k],
                            prev_coef_count_sum, 256, true);

                    do {
                        final int[] ct = cpi.frame_branch_ct[i][j][k][t];
                        int newp = cpi.frame_coef_probs[i][j][k][t];
                        int oldp = cpi.common.fc.coef_probs[i][j][k][t];
                        int upd = CoefUpdateProbs.vp8_coef_update_probs[i][j][k][t];
                        final int s = prob_update_savings(ct, oldp, newp, upd);

                        if (cpi.common.frame_type != FrameType.KEY_FRAME
                                || (cpi.common.frame_type == FrameType.KEY_FRAME && newp != oldp)) {
                            prev_coef_savings[t] += s;
                        }
                    } while (++t < Entropy.ENTROPY_NODES);
                } while (++k < Entropy.PREV_COEF_CONTEXTS);
                k = 0;
                do {
                    /*
                     * We only update probabilities if we can save bits, except for key frames where
                     * we have to update all probabilities to get the equal probabilities across the
                     * prev coef contexts.
                     */
                    if (prev_coef_savings[k] > 0 || cpi.common.frame_type == FrameType.KEY_FRAME) {
                        savings += prev_coef_savings[k];
                    }
                } while (++k < Entropy.ENTROPY_NODES);
            } while (++j < Entropy.COEF_BANDS);
        } while (++i < Entropy.BLOCK_TYPES);
        return savings;
    }

    static int default_coef_context_savings(Compressor cpi) {
        final Macroblock x = cpi.mb;
        int savings = 0;
        int i = 0;
        do {
            int j = 0;
            do {
                int k = 0;
                do {
                    /* at every context */

                    /* calc probs and branch cts for this frame only */
                    int t = 0; /* token/prob index */

                    TreeCoder.vp8_tree_probs_from_distribution(TokenAlphabet.entropyTokenCount, vp8_coef_encodings,
                            Entropy.vp8_coef_tree, cpi.frame_coef_probs[i][j][k], cpi.frame_branch_ct[i][j][k],
                            x.coef_counts[i][j][k], 256, true);

                    do {
                        final int[] ct = cpi.frame_branch_ct[i][j][k][t];
                        final int newp = cpi.frame_coef_probs[i][j][k][t];
                        final int oldp = cpi.common.fc.coef_probs[i][j][k][t];
                        final int upd = CoefUpdateProbs.vp8_coef_update_probs[i][j][k][t];
                        final int s = prob_update_savings(ct, oldp, newp, upd);

                        if (s > 0) {
                            savings += s;
                        }
                    } while (++t < Entropy.ENTROPY_NODES);
                } while (++k < Entropy.PREV_COEF_CONTEXTS);
            } while (++j < Entropy.COEF_BANDS);
        } while (++i < Entropy.BLOCK_TYPES);
        return savings;
    }

    public static void vp8_calc_ref_frame_costs(int[] ref_frame_cost, int prob_intra, int prob_last, int prob_garf) {
        assert (prob_intra >= 0);
        assert (prob_intra <= 255);
        assert (prob_last >= 0);
        assert (prob_last <= 255);
        assert (prob_garf >= 0);
        assert (prob_garf <= 255);
        ref_frame_cost[MVReferenceFrame.INTRA_FRAME.ordinal()] = TreeWriter.vp8_cost_zero(prob_intra);
        ref_frame_cost[MVReferenceFrame.LAST_FRAME.ordinal()] = TreeWriter.vp8_cost_one(prob_intra)
                + TreeWriter.vp8_cost_zero(prob_last);
        ref_frame_cost[MVReferenceFrame.GOLDEN_FRAME.ordinal()] = TreeWriter.vp8_cost_one(prob_intra)
                + TreeWriter.vp8_cost_one(prob_last) + TreeWriter.vp8_cost_zero(prob_garf);
        ref_frame_cost[MVReferenceFrame.ALTREF_FRAME.ordinal()] = TreeWriter.vp8_cost_one(prob_intra)
                + TreeWriter.vp8_cost_one(prob_last) + TreeWriter.vp8_cost_one(prob_garf);
    }

    static int vp8_estimate_entropy_savings(Compressor cpi) {
        int savings = 0;
        final ReferenceCounts rf = cpi.mb.sumReferenceCounts();
        final EnumMap<MVReferenceFrame, Integer> rfct = cpi.mb.count_mb_ref_frame_usage;
        int new_intra, new_last, new_garf, oldtotal, newtotal;
        int[] ref_frame_cost = new int[MVReferenceFrame.count];

        if (cpi.common.frame_type != FrameType.KEY_FRAME) {
            if ((new_intra = rf.intra * 255 / (rf.intra + rf.inter)) == 0)
                new_intra = 1;

            new_last = rf.inter != 0 ? (rfct.get(MVReferenceFrame.LAST_FRAME) * 255) / rf.inter : 128;

            new_garf = rfct.get(MVReferenceFrame.GOLDEN_FRAME) + rfct.get(MVReferenceFrame.ALTREF_FRAME) != 0
                    ? (rfct.get(MVReferenceFrame.GOLDEN_FRAME) * 255)
                            / (rfct.get(MVReferenceFrame.GOLDEN_FRAME) + rfct.get(MVReferenceFrame.ALTREF_FRAME))
                    : 128;

            vp8_calc_ref_frame_costs(ref_frame_cost, new_intra, new_last, new_garf);

            newtotal = rfct.get(MVReferenceFrame.INTRA_FRAME) * ref_frame_cost[MVReferenceFrame.INTRA_FRAME.ordinal()]
                    + rfct.get(MVReferenceFrame.LAST_FRAME) * ref_frame_cost[MVReferenceFrame.LAST_FRAME.ordinal()]
                    + rfct.get(MVReferenceFrame.GOLDEN_FRAME) * ref_frame_cost[MVReferenceFrame.GOLDEN_FRAME.ordinal()]
                    + rfct.get(MVReferenceFrame.ALTREF_FRAME) * ref_frame_cost[MVReferenceFrame.ALTREF_FRAME.ordinal()];

            /* old costs */
            vp8_calc_ref_frame_costs(ref_frame_cost, cpi.prob_intra_coded, cpi.prob_last_coded, cpi.prob_gf_coded);

            oldtotal = rfct.get(MVReferenceFrame.INTRA_FRAME) * ref_frame_cost[MVReferenceFrame.INTRA_FRAME.ordinal()]
                    + rfct.get(MVReferenceFrame.LAST_FRAME) * ref_frame_cost[MVReferenceFrame.LAST_FRAME.ordinal()]
                    + rfct.get(MVReferenceFrame.GOLDEN_FRAME) * ref_frame_cost[MVReferenceFrame.GOLDEN_FRAME.ordinal()]
                    + rfct.get(MVReferenceFrame.ALTREF_FRAME) * ref_frame_cost[MVReferenceFrame.ALTREF_FRAME.ordinal()];

            savings += (oldtotal - newtotal) / 256;
        }
        if (cpi.oxcf.error_resilient_mode) {
            savings += independent_coef_context_savings(cpi);
        } else {
            savings += default_coef_context_savings(cpi);
        }

        return savings;
    }

    static void vp8_update_coef_probs(Compressor cpi) {
        int i = 0;
        BoolEncoder w = cpi.bc[0];
        int[] prev_coef_savings = new int[Entropy.ENTROPY_NODES];
        do {
            int j = 0;

            do {
                int k = 0;
                CommonUtils.vp8_zero(prev_coef_savings);
                if (cpi.oxcf.error_resilient_mode) {
                    for (k = 0; k < Entropy.PREV_COEF_CONTEXTS; ++k) {
                        int t; /* token/prob index */
                        for (t = 0; t < Entropy.ENTROPY_NODES; ++t) {
                            int[] ct = cpi.frame_branch_ct[i][j][k][t];
                            int newp = cpi.frame_coef_probs[i][j][k][t];
                            int oldp = cpi.common.fc.coef_probs[i][j][k][t];
                            int upd = CoefUpdateProbs.vp8_coef_update_probs[i][j][k][t];

                            prev_coef_savings[t] += prob_update_savings(ct, oldp, newp, upd);
                        }
                    }
                    k = 0;
                }
                do {
                    /*
                     * note: use result from vp8_estimate_entropy_savings, so no need to call
                     * vp8_tree_probs_from_distribution here.
                     */

                    /* at every context */

                    /* calc probs and branch cts for this frame only */
                    int t = 0; /* token/prob index */

                    do {
                        final short newp = cpi.frame_coef_probs[i][j][k][t];

                        final int Pold = cpi.common.fc.coef_probs[i][j][k][t];
                        final int upd = CoefUpdateProbs.vp8_coef_update_probs[i][j][k][t];

                        int s = prev_coef_savings[t];
                        boolean u = false;

                        if (!cpi.oxcf.error_resilient_mode) {
                            s = prob_update_savings(cpi.frame_branch_ct[i][j][k][t], Pold, newp, upd);
                        }

                        if (s > 0)
                            u = true;

                        /*
                         * Force updates on key frames if the new is different, so that we can be sure
                         * we end up with equal probabilities over the prev coef contexts.
                         */
                        if (cpi.oxcf.error_resilient_mode && cpi.common.frame_type == FrameType.KEY_FRAME
                                && newp != Pold) {
                            u = true;
                        }

                        w.vp8_encode_bool(u, upd);

                        if (u) {
                            /* send/use new probability */
                            cpi.common.fc.coef_probs[i][j][k][t] = newp;
                            TreeWriter.vp8_write_literal(w, newp, 8);
                        }
                    } while (++t < Entropy.ENTROPY_NODES);
                } while (++k < Entropy.PREV_COEF_CONTEXTS);
            } while (++j < Entropy.COEF_BANDS);
        } while (++i < Entropy.BLOCK_TYPES);
    }

    static void put_delta_q(BoolEncoder bc, int delta_q) {
        if (delta_q != 0) {
            bc.vp8_write_bit(true);
            TreeWriter.vp8_write_literal(bc, Math.abs(delta_q), 4);
            bc.vp8_write_bit(delta_q < 0);
        } else
            bc.vp8_write_bit(false);
    }

    static int vp8_pack_bitstream(Compressor cpi, FullAccessIntArrPointer dest, ReadOnlyIntArrPointer dest_end) {
        int i, j, size;
        Header oh = new Header();
        CommonData pc = cpi.common;
        BoolEncoder[] bc = cpi.bc;
        MacroblockD xd = cpi.mb.e_mbd;
        int extra_bytes_packed = 0;

        FullAccessIntArrPointer cx_data = dest.shallowCopy();
        int mbfeaturedatabitsIdx = 0;

        oh.show_frame = pc.show_frame;
        oh.type = pc.frame_type;
        oh.version = pc.getVersion();
        oh.first_partition_length_in_bytes = 0;

        BoolEncoder.validate_buffer(cx_data, 3, dest_end);
        cx_data.incBy(3);

        /*
         * every keyframe send startcode, width, height, scale factor, clamp and color
         * type
         */
        if (oh.type == FrameType.KEY_FRAME) {
            int v;

            BoolEncoder.validate_buffer(cx_data, 7, dest_end);

            /* Start / synch code */
            cx_data.setAndInc((short) 0x9D);
            cx_data.setAndInc((short) 0x01);
            cx_data.setAndInc((short) 0x2a);

            /*
             * Pack scale and frame size into 16 bits. Store it 8 bits at a time.
             * https://tools.ietf.org/html/rfc6386 9.1. Uncompressed Data Chunk 16 bits : (2
             * bits Horizontal Scale << 14) | Width (14 bits) 16 bits : (2 bits Vertical
             * Scale << 14) | Height (14 bits)
             */
            v = (pc.horiz_scale.ordinal() << 14) | pc.Width;
            cx_data.setAndInc((short) (v & 0xff));
            cx_data.setAndInc((short) (v >> 8));

            v = (pc.vert_scale.ordinal() << 14) | pc.Height;
            cx_data.setAndInc((short) (v & 0xff));
            cx_data.setAndInc((short) (v >> 8));

            extra_bytes_packed = 7;

            bc[0].vp8_start_encode(cx_data, dest_end);

            /* signal clr type */
            bc[0].vp8_write_bit(false);
            // clamping is always required:
            bc[0].vp8_write_bit(false);

        } else {
            bc[0].vp8_start_encode(cx_data, dest_end);
        }

        /* Signal whether or not Segmentation is enabled */
        bc[0].vp8_write_bit(xd.segmentation_enabled != 0);

        /* Indicate which features are enabled */
        if (xd.segmentation_enabled != 0) {
            /* Signal whether or not the segmentation map is being updated. */
            bc[0].vp8_write_bit(xd.update_mb_segmentation_map);
            bc[0].vp8_write_bit(xd.update_mb_segmentation_data);

            if (xd.update_mb_segmentation_data) {
                int Data;

                bc[0].vp8_write_bit(xd.mb_segement_abs_delta);

                /* For each segmentation feature (Quant and loop filter level) */
                for (i = 0; i < MBLvlFeatures.featureCount; ++i) {
                    /* For each of the segments */
                    for (j = 0; j < BlockD.MAX_MB_SEGMENTS; ++j) {
                        Data = xd.segment_feature_data[i][j];

                        /* Frame level data */
                        if (Data != (byte) 0) {
                            bc[0].vp8_write_bit(true);

                            if (Data < 0) {
                                Data = -Data;
                                TreeWriter.vp8_write_literal(bc[0], Data,
                                        Entropy.vp8_mb_feature_data_bits[mbfeaturedatabitsIdx + i]);
                                bc[0].vp8_write_bit(true);
                            } else {
                                TreeWriter.vp8_write_literal(bc[0], Data,
                                        Entropy.vp8_mb_feature_data_bits[mbfeaturedatabitsIdx + i]);
                                bc[0].vp8_write_bit(false);
                            }
                        } else
                            bc[0].vp8_write_bit(false);
                    }
                }
            }

            if (xd.update_mb_segmentation_map) {
                /* Write the probs used to decode the segment id for each mb */
                for (i = 0; i < BlockD.MB_FEATURE_TREE_PROBS; ++i) {
                    int Data = xd.mb_segment_tree_probs[i];

                    if (Data != 255) {
                        bc[0].vp8_write_bit(true);
                        TreeWriter.vp8_write_literal(bc[0], Data, 8);
                    } else
                        bc[0].vp8_write_bit(false);
                }
            }
        }

        bc[0].vp8_write_bit(pc.filter_type == LoopFilterType.SIMPLE);
        TreeWriter.vp8_write_literal(bc[0], pc.filter_level, 6);
        TreeWriter.vp8_write_literal(bc[0], pc.sharpness_level, 3);

        /*
         * Write out loop filter deltas applied at the MB level based on mode or ref
         * frame (if they are enabled).
         */
        bc[0].vp8_write_bit(xd.mode_ref_lf_delta_enabled);

        if (xd.mode_ref_lf_delta_enabled) {
            /* Do the deltas need to be updated */
            boolean send_update = xd.mode_ref_lf_delta_update || cpi.oxcf.error_resilient_mode;

            bc[0].vp8_write_bit(send_update);
            if (send_update) {
                int Data;
                byte[][] updater = { xd.ref_lf_deltas, xd.mode_lf_deltas };
                byte[][] lastUpd = { xd.last_ref_lf_deltas, xd.last_mode_lf_deltas };
                for (int k = 0; k < updater.length; k++) {
                    /* Send update */
                    for (i = 0; i < BlockD.MAX_REF_LF_DELTAS; ++i) {
                        Data = updater[k][i];

                        /* Frame level data */
                        if (Data != lastUpd[k][i] || cpi.oxcf.error_resilient_mode) {
                            lastUpd[k][i] = updater[k][i];
                            bc[0].vp8_write_bit(true);

                            boolean sign = false;
                            if (Data < 0) {
                                Data = -Data;
                                sign = true;
                            }
                            TreeWriter.vp8_write_literal(bc[0], (Data & 0x3F), 6);
                            bc[0].vp8_write_bit(sign); /* sign */

                        } else
                            bc[0].vp8_write_bit(false);
                    }
                }
            }
        }

        /* signal here is multi token partition is enabled */
        TreeWriter.vp8_write_literal(bc[0], pc.multi_token_partition.ordinal(), 2);

        /* Frame Qbaseline quantizer index */
        TreeWriter.vp8_write_literal(bc[0], pc.base_qindex, 7);

        /* Transmit Dc, Second order and Uv quantizer delta information */
        put_delta_q(bc[0], pc.delta_q.get(CommonData.Quant.Y1).get(CommonData.Comp.DC));
        put_delta_q(bc[0], pc.delta_q.get(CommonData.Quant.Y2).get(CommonData.Comp.DC));
        put_delta_q(bc[0], pc.delta_q.get(CommonData.Quant.Y2).get(CommonData.Comp.AC));
        put_delta_q(bc[0], pc.delta_q.get(CommonData.Quant.UV).get(CommonData.Comp.DC));
        put_delta_q(bc[0], pc.delta_q.get(CommonData.Quant.UV).get(CommonData.Comp.AC));

        /*
         * When there is a key frame all reference buffers are updated using the new key
         * frame
         */
        if (pc.frame_type != FrameType.KEY_FRAME) {
            /*
             * Should the GF or ARF be updated using the transmitted frame or buffer
             */
            bc[0].vp8_write_bit(pc.refresh_golden_frame);
            bc[0].vp8_write_bit(pc.refresh_alt_ref_frame);

            /*
             * If not being updated from current frame should either GF or ARF be updated
             * from another buffer
             */
            if (!pc.refresh_golden_frame)
                TreeWriter.vp8_write_literal(bc[0], pc.copy_buffer_to_gf, 2);

            if (!pc.refresh_alt_ref_frame)
                TreeWriter.vp8_write_literal(bc[0], pc.copy_buffer_to_arf, 2);

            /*
             * Indicate reference frame sign bias for Golden and ARF frames (always 0 for
             * last frame buffer)
             */
            bc[0].vp8_write_bit(pc.ref_frame_sign_bias.get(MVReferenceFrame.GOLDEN_FRAME));
            bc[0].vp8_write_bit(pc.ref_frame_sign_bias.get(MVReferenceFrame.ALTREF_FRAME));
        }

        if (cpi.oxcf.error_resilient_mode) {
            if (pc.frame_type == FrameType.KEY_FRAME) {
                pc.refresh_entropy_probs = true;
            } else {
                pc.refresh_entropy_probs = false;
            }
        }

        bc[0].vp8_write_bit(pc.refresh_entropy_probs);

        if (pc.frame_type != FrameType.KEY_FRAME)
            bc[0].vp8_write_bit(pc.refresh_last_frame);

        if (!pc.refresh_entropy_probs) {
            /* save a copy for later refresh */
            cpi.common.lfc = new FrameContext(cpi.common.fc);
        }

        vp8_update_coef_probs(cpi);

        /* Write out the mb_no_coeff_skip flag */
        bc[0].vp8_write_bit(pc.mb_no_coeff_skip);

        if (pc.frame_type == FrameType.KEY_FRAME) {
            write_kfmodes(cpi);
        } else {
            pack_inter_mode_mvs(cpi);
        }

        bc[0].vp8_stop_encode();

        cx_data.incBy(bc[0].getPos());

        oh.first_partition_length_in_bytes = cpi.bc[0].getPos();

        /* update frame tag */
        {
            short[] ohAsbytes = oh.asThreeBytes();
            dest.memcopyin(0, ohAsbytes, 0, ohAsbytes.length);
        }

        size = Header.VP8_HEADER_SIZE + extra_bytes_packed + cpi.bc[0].getPos();

        cpi.partition_sz[0] = size;

        if (pc.multi_token_partition != TokenPartition.ONE_PARTITION) {
            int num_part = 1 << pc.multi_token_partition.ordinal();

            /* partition size table at the end of first partition */
            cpi.partition_sz[0] += (size += 3 * (num_part - 1));

            BoolEncoder.validate_buffer(cx_data, 3 * (num_part - 1), dest_end);

            pack_tokens_into_partitions(cpi, cx_data.shallowCopyWithPosInc(3 * (num_part - 1)), dest_end, num_part);

            for (i = 1; i < num_part; ++i) {
                cpi.partition_sz[i] = cpi.bc[i].getPos();
                write_partition_size(cx_data, cpi.partition_sz[i]);
                cx_data.incBy(3);
                size += cpi.partition_sz[i]; /* add to total */
            }

            /* add last partition to total size */
            cpi.partition_sz[i] = cpi.bc[i].getPos();
            size += cpi.partition_sz[i];
        } else {

            cpi.bc[1].vp8_start_encode(cx_data, dest_end);

            TokenList tlist = new TokenList();
            tlist.start = cpi.tok;
            tlist.stop = cpi.tok.shallowCopyWithPosInc(cpi.tok_count);
            vp8_pack_tokens(cpi.bc[1], tlist);

            cpi.bc[1].vp8_stop_encode();

            cpi.partition_sz[1] = (size += cpi.bc[1].getPos());
        }
        return size;
    }
}
