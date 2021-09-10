package org.jcodec.codecs.vpx.vp8;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeContexts;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;

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
public class FindNearMV {
    static enum Cntmodes {
        CNT_INTRA, CNT_NEAREST, CNT_NEAR, CNT_SPLITMV
    };

    public static final short LEFT_TOP_MARGIN = (16 << 3);
    public static final short RIGHT_BOTTOM_MARGIN = (16 << 3);

    public static int[][] vp8_mbsplit_offset = { { 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 2, 8, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 } };

    static void mv_bias(boolean refmb_ref_frame_sign_bias, MVReferenceFrame refframe, MV mvp,
            final EnumMap<MVReferenceFrame, Boolean> ref_frame_sign_bias) {
        if (refmb_ref_frame_sign_bias != ref_frame_sign_bias.get(refframe)) {
            mvp.row *= -1;
            mvp.col *= -1;
        }
    }

    /*
     * Predict motion vectors using those from already-decoded nearby blocks. Note
     * that we only consider one 4x4 subblock from each candidate 16x16 macroblock.
     */
    static void vp8_find_near_mvs(MacroblockD xd, FullAccessGenArrPointer<ModeInfo> m, MV nearest, MV nearby,
            MV best_mv, int[] near_mv_ref_cnts, MVReferenceFrame refframe,
            EnumMap<MVReferenceFrame, Boolean> ref_frame_sign_bias) {
        ModeInfo above = m.getRel(-xd.mode_info_stride);
        ModeInfo left = m.getRel(-1);
        ModeInfo aboveleft = m.getRel(-xd.mode_info_stride - 1);
        MV[] near_mvs = new MV[4];
        MV this_mv = new MV();
        /* Zero accumulators */
        for (int i = 0; i < near_mvs.length; i++) {
            near_mv_ref_cnts[i] = 0;
        }
        int nearMVidx = 0;
        int rcntIdx = 0;

        /* Process above */
        if (above.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
            if (!above.mbmi.mv.isZero()) {
                near_mvs[++nearMVidx] = new MV(above.mbmi.mv);
                mv_bias(ref_frame_sign_bias.get(above.mbmi.ref_frame), refframe, near_mvs[nearMVidx],
                        ref_frame_sign_bias);
                ++rcntIdx;
            }

            near_mv_ref_cnts[rcntIdx] += 2;
        }

        /* Process left */
        if (left.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
            if (!left.mbmi.mv.isZero()) {
                this_mv.set(left.mbmi.mv);

                mv_bias(ref_frame_sign_bias.get(left.mbmi.ref_frame), refframe, this_mv, ref_frame_sign_bias);

                if (!this_mv.equals(near_mvs[nearMVidx])) {
                    near_mvs[++nearMVidx] = new MV(this_mv);
                    ++rcntIdx;
                }

                near_mv_ref_cnts[rcntIdx] += 2;
            } else {
                near_mv_ref_cnts[Cntmodes.CNT_INTRA.ordinal()] += 2;
            }
        }

        /* Process above left */
        if (aboveleft.mbmi.ref_frame != MVReferenceFrame.INTRA_FRAME) {
            if (!aboveleft.mbmi.mv.isZero()) {
                this_mv.set(aboveleft.mbmi.mv);
                mv_bias(ref_frame_sign_bias.get(aboveleft.mbmi.ref_frame), refframe, this_mv, ref_frame_sign_bias);

                if (!this_mv.equals(near_mvs[nearMVidx])) {
                    near_mvs[++nearMVidx] = new MV(this_mv);
                    ++rcntIdx;
                }
                near_mv_ref_cnts[rcntIdx]++;
            } else {
                near_mv_ref_cnts[Cntmodes.CNT_INTRA.ordinal()] += 1;
            }
        }

        /* If we have three distinct MV's ... */
        if (near_mv_ref_cnts[Cntmodes.CNT_SPLITMV.ordinal()] != 0) {
            /* See if above-left MV can be merged with NEAREST */
            if (near_mvs[nearMVidx].equals(near_mvs[Cntmodes.CNT_NEAREST.ordinal()]))
                near_mv_ref_cnts[Cntmodes.CNT_NEAREST.ordinal()] += 1;
        }

        near_mv_ref_cnts[Cntmodes.CNT_SPLITMV.ordinal()] = (((above.mbmi.mode == MBPredictionMode.SPLITMV) ? 1 : 0)
                + ((left.mbmi.mode == MBPredictionMode.SPLITMV) ? 1 : 0)) * 2
                + ((aboveleft.mbmi.mode == MBPredictionMode.SPLITMV) ? 1 : 0);

        /* Swap near and nearest if necessary */
        if (near_mv_ref_cnts[Cntmodes.CNT_NEAR.ordinal()] > near_mv_ref_cnts[Cntmodes.CNT_NEAREST.ordinal()]) {
            int tmp;
            tmp = near_mv_ref_cnts[Cntmodes.CNT_NEAREST.ordinal()];
            near_mv_ref_cnts[Cntmodes.CNT_NEAREST.ordinal()] = near_mv_ref_cnts[Cntmodes.CNT_NEAR.ordinal()];
            near_mv_ref_cnts[Cntmodes.CNT_NEAR.ordinal()] = tmp;
            this_mv.set(near_mvs[Cntmodes.CNT_NEAREST.ordinal()]);
            near_mvs[Cntmodes.CNT_NEAREST.ordinal()].set(near_mvs[Cntmodes.CNT_NEAR.ordinal()]);
            near_mvs[Cntmodes.CNT_NEAR.ordinal()].set(this_mv);
        }

        /* Use near_mvs[0] to store the "best" MV */
        if (near_mv_ref_cnts[Cntmodes.CNT_NEAREST.ordinal()] >= near_mv_ref_cnts[Cntmodes.CNT_INTRA.ordinal()]) {
            near_mvs[Cntmodes.CNT_INTRA.ordinal()] = near_mvs[Cntmodes.CNT_NEAREST.ordinal()];
        }

        /* Set up return values */
        best_mv.set(near_mvs[0]);
        nearest.set(near_mvs[Cntmodes.CNT_NEAREST.ordinal()]);
        nearby.set(near_mvs[Cntmodes.CNT_NEAR.ordinal()]);
    }

    public static void vp8_clamp_mv(MV m, short mb_to_left_edge, short mb_to_right_edge, short mb_to_top_edge,
            short mb_to_bottom_edge) {
        m.col = (m.col < mb_to_left_edge) ? mb_to_left_edge : m.col;
        m.col = (m.col > mb_to_right_edge) ? mb_to_right_edge : m.col;
        m.row = (m.row < mb_to_top_edge) ? mb_to_top_edge : m.row;
        m.row = (m.row > mb_to_bottom_edge) ? mb_to_bottom_edge : m.row;
    }

    static void vp8_clamp_mv2(MV m, MacroblockD xd) {
        if (m.col < (xd.mb_to_left_edge - LEFT_TOP_MARGIN)) {
            m.col = (short) (xd.mb_to_left_edge - LEFT_TOP_MARGIN);
        } else if (m.col > xd.mb_to_right_edge + RIGHT_BOTTOM_MARGIN) {
            m.col = (short) (xd.mb_to_right_edge + RIGHT_BOTTOM_MARGIN);
        }

        if (m.row < (xd.mb_to_top_edge - LEFT_TOP_MARGIN)) {
            m.row = (short) (xd.mb_to_top_edge - LEFT_TOP_MARGIN);
        } else if (m.row > xd.mb_to_bottom_edge + RIGHT_BOTTOM_MARGIN) {
            m.row = (short) (xd.mb_to_bottom_edge + RIGHT_BOTTOM_MARGIN);
        }
    }

    static short[] vp8_mv_ref_probs(short[] p, final int[] near_mv_ref_ct) {
        p[0] = ModeContexts.vp8_mode_contexts[near_mv_ref_ct[0]][0];
        p[1] = ModeContexts.vp8_mode_contexts[near_mv_ref_ct[1]][1];
        p[2] = ModeContexts.vp8_mode_contexts[near_mv_ref_ct[2]][2];
        p[3] = ModeContexts.vp8_mode_contexts[near_mv_ref_ct[3]][3];
        /*
         * p[3] = vp8_mode_contexts[near_mv_ref_ct[1] + near_mv_ref_ct[2] +
         * near_mv_ref_ct[3]][3];
         */
        return p;
    }

    static MV left_block_mv(FullAccessGenArrPointer<ModeInfo> mic, int b) {
        ModeInfo mi = mic.get();
        if ((b & 3) == 0) {
            /* On L edge, get from MB to left of us */
            mi = mic.getRel(-1);
            if (mi.mbmi.mode != MBPredictionMode.SPLITMV)
                return mi.mbmi.mv.copy();
            b += 4;
        }
        return mi.bmi[b - 1].mv.copy();
    }

    static MV above_block_mv(FullAccessGenArrPointer<ModeInfo> mic, int b, int mi_stride) {
        ModeInfo mi = mic.get();
        if ((b >> 2) == 0) {
            /* On top edge, get from MB above us */
            mi = mic.getRel(-mi_stride);

            if (mi.mbmi.mode != MBPredictionMode.SPLITMV)
                return mi.mbmi.mv.copy();
            b += 16;
        }
        return mi.bmi[b - 4].mv.copy();
    }

    static BPredictionMode above_block_mode(FullAccessGenArrPointer<ModeInfo> mic, int b, int mi_stride) {
        if ((b >> 2) == 0) {
            /* On top edge, get from MB above us */
            ModeInfo mi = mic.getRel(-mi_stride);

            switch (mi.mbmi.mode) {
            case B_PRED:
                return mi.bmi[b + 12].as_mode();
            case DC_PRED:
                return BPredictionMode.B_DC_PRED;
            case V_PRED:
                return BPredictionMode.B_VE_PRED;
            case H_PRED:
                return BPredictionMode.B_HE_PRED;
            case TM_PRED:
                return BPredictionMode.B_TM_PRED;
            default:
                return BPredictionMode.B_DC_PRED;
            }
        }
        return mic.get().bmi[b - 4].as_mode();
    }

    static BPredictionMode left_block_mode(FullAccessGenArrPointer<ModeInfo> mic, int b) {
        if ((b & 3) == 0) {
            /* On L edge, get from MB to left of us */
            ModeInfo mi = mic.getRel(-1);
            switch (mi.mbmi.mode) {
            case B_PRED:
                return mi.bmi[b + 3].as_mode();
            case DC_PRED:
                return BPredictionMode.B_DC_PRED;
            case V_PRED:
                return BPredictionMode.B_VE_PRED;
            case H_PRED:
                return BPredictionMode.B_HE_PRED;
            case TM_PRED:
                return BPredictionMode.B_TM_PRED;
            default:
                return BPredictionMode.B_DC_PRED;
            }
        }
        return mic.get().bmi[b - 1].as_mode();
    }

    static void invert_and_clamp_mvs(MV inv, MV src, MacroblockD xd) {
        inv.row = (short) -src.row;
        inv.col = (short) -src.col;
        vp8_clamp_mv2(inv, xd);
        vp8_clamp_mv2(src, xd);
    }

    static boolean vp8_find_near_mvs_bias(MacroblockD xd, FullAccessGenArrPointer<ModeInfo> mi, MV[][] mode_mv_sb,
            MV[] best_mv_sb, int[] cnt, MVReferenceFrame refframe,
            EnumMap<MVReferenceFrame, Boolean> ref_frame_sign_bias) {
        boolean sign_bias = ref_frame_sign_bias.get(refframe);
        int sbNorm = sign_bias ? 1 : 0, sbNeg = sign_bias ? 0 : 1;

        vp8_find_near_mvs(xd, mi, mode_mv_sb[sbNorm][MBPredictionMode.NEARESTMV.ordinal()],
                mode_mv_sb[sbNorm][MBPredictionMode.NEARMV.ordinal()], best_mv_sb[sbNorm], cnt, refframe,
                ref_frame_sign_bias);

        invert_and_clamp_mvs(mode_mv_sb[sbNeg][MBPredictionMode.NEARESTMV.ordinal()],
                mode_mv_sb[sbNorm][MBPredictionMode.NEARESTMV.ordinal()], xd);
        invert_and_clamp_mvs(mode_mv_sb[sbNeg][MBPredictionMode.NEARMV.ordinal()],
                mode_mv_sb[sbNorm][MBPredictionMode.NEARMV.ordinal()], xd);
        invert_and_clamp_mvs(best_mv_sb[sbNeg], best_mv_sb[sbNorm], xd);

        return sign_bias;
    }

}
