package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.EntropyMV;
import org.jcodec.codecs.vpx.vp8.data.EntropyMode;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.MVContext;
import org.jcodec.codecs.vpx.vp8.data.Token;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
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
public class EncodeMV {

    static void encode_mvcomponent(final BoolEncoder w, final int v, final MVContext mvc) {
        PositionableIntArrPointer p = mvc.prob.positionableOnly();
        int x = v < 0 ? -v : v;

        if (x < EntropyMV.mvnum_short) { /* Small */
            w.vp8_encode_bool(false, p.getRel(EntropyMV.mvpis_short));
            p.incBy(EntropyMV.MVPshort);
            TreeWriter.vp8_treed_write(w, EntropyMode.vp8_small_mvtree, p, x, 3);
            p.rewind();
            if (x == 0)
                return; /* no sign bit */
        } else { /* Large */
            int i = 0;

            w.vp8_encode_bool(true, p.getRel(EntropyMV.mvpis_short));
            p.incBy(EntropyMV.MVPbits);

            do {
                w.vp8_encode_bool(((x >> i) & 1) == 1, p.getRel(i));
            } while (++i < 3);

            i = EntropyMV.mvlong_width - 1; /* Skip bit 3, which is sometimes implicit */

            do {
                w.vp8_encode_bool(((x >> i) & 1) == 1, p.getRel(i));

            } while (--i > 3);

            if ((x & 0xFFF0) > 0)
                w.vp8_encode_bool(((x >> 3) & 1) == 1, p.getRel(3));
            p.rewind();
        }

        w.vp8_encode_bool(v < 0, p.getRel(EntropyMV.MVPsign));
    }

    static void vp8_encode_motion_vector(BoolEncoder w, final MV mv, final MVContext[] mvc) {
        encode_mvcomponent(w, mv.row >> 1, mvc[0]);
        encode_mvcomponent(w, mv.col >> 1, mvc[1]);
    }

    static short calc_prob(short p, int[] ct) {
        final int tot = ct[0] + ct[1];

        if (tot != 0) {
            final int x = ((ct[0] * 255) / tot) & -2;
            return (short) (x != 0 ? x : 1);
        } else {
            return p;
        }
    }

    static int cost_mvcomponent(final int v, MVContext mvc) {
        PositionableIntArrPointer p = mvc.prob.positionableOnly();
        final int x = v;
        int cost;

        if (x < EntropyMV.mvnum_short) {
            cost = TreeWriter.vp8_cost_zero(p.getRel(EntropyMV.mvpis_short));
            p.incBy(EntropyMV.MVPshort);
            cost += TreeWriter.vp8_treed_cost(EntropyMode.vp8_small_mvtree, p, x, 3);
            if (x == 0)
                return cost;
        } else {
            int i = 0;
            cost = TreeWriter.vp8_cost_one(p.getRel(EntropyMV.mvpis_short));
            p.incBy(EntropyMV.MVPbits);
            do {
                cost += TreeWriter.vp8_cost_bit(p.getRel(i), (x >> i) & 1);

            } while (++i < 3);

            i = EntropyMV.mvlong_width - 1; /* Skip bit 3, which is sometimes implicit */

            do {
                cost += TreeWriter.vp8_cost_bit(p.getRel(i), (x >> i) & 1);

            } while (--i > 3);

            if ((x & 0xFFF0) != 0)
                cost += TreeWriter.vp8_cost_bit(p.getRel(3), (x >> 3) & 1);
        }

        return cost; /* + vp8_cost_bit( p [MVPsign], v < 0); */
    }

    static void vp8_build_component_cost_table(FullAccessIntArrPointer[] mvcost, MVContext[] mvc, boolean[] mvc_flag) {
        int i = 1;
        int cost0 = 0;
        int cost1 = 0;

        if (mvc_flag[0]) {
            mvcost[0].set((short) (cost_mvcomponent(0, mvc[0])));

            do {
                cost0 = cost_mvcomponent(i, mvc[0]);

                mvcost[0].setRel(i, (short) (cost0 + TreeWriter.vp8_cost_zero(mvc[0].prob.getRel(EntropyMV.MVPsign))));
                mvcost[0].setRel(-i, (short) (cost0 + TreeWriter.vp8_cost_one(mvc[0].prob.getRel(EntropyMV.MVPsign))));
            } while (++i <= EntropyMV.mv_max);
        }

        i = 1;

        if (mvc_flag[1]) {
            mvcost[1].set((short) (cost_mvcomponent(0, mvc[1])));

            do {
                cost1 = cost_mvcomponent(i, mvc[1]);

                mvcost[1].setRel(i, (short) (cost1 + TreeWriter.vp8_cost_zero(mvc[1].prob.getRel(EntropyMV.MVPsign))));
                mvcost[1].setRel(-i, (short) (cost1 + TreeWriter.vp8_cost_one(mvc[1].prob.getRel(EntropyMV.MVPsign))));
            } while (++i <= EntropyMV.mv_max);
        }
    }

    static boolean update(final BoolEncoder w, int[] ct, FullAccessIntArrPointer cur_p, int idx, short new_p,
            int update_p) {
        int cur_b = TreeWriter.vp8_cost_branch(ct, cur_p.getRel(idx));
        int new_b = TreeWriter.vp8_cost_branch(ct, new_p);
        int cost = 6 + ((TreeWriter.vp8_cost_one(update_p) - TreeWriter.vp8_cost_zero(update_p) + 128) >> 8);
        if (cur_b - new_b > cost) {
            cur_p.setRel(idx, new_p);
            w.vp8_encode_bool(true, update_p);
            TreeWriter.vp8_write_literal(w, new_p >> 1, 7);
            return true;

        } else
            w.vp8_encode_bool(false, update_p);
        return false;
    }

    static boolean write_component_probs(final BoolEncoder w, MVContext cur_mvc, MVContext default_mvc_,
            MVContext update_mvc, int[] events, int rc) {
        boolean updated = false;
        FullAccessIntArrPointer Pcur = cur_mvc.prob;
        ReadOnlyIntArrPointer Pupdate = update_mvc.prob;
        int updateidx = 0;
        int[] is_short_ct = new int[2], sign_ct = new int[2];

        int[][] bit_ct = new int[EntropyMV.mvlong_width][2];

        int[] short_ct = new int[EntropyMV.mvnum_short];
        int[][] short_bct = new int[EntropyMV.mvnum_short - 1][2];

        short[] Pnew = new short[EntropyMV.MVPcount];
        default_mvc_.prob.memcopyout(0, Pnew, 0, EntropyMV.MVPcount);

        /* j=0 */
        {
            final int c = events[EntropyMV.mv_max];

            is_short_ct[0] += c; /* Short vector */
            short_ct[0] += c; /* Magnitude distribution */
        }

        /* j: 1 ~ mv_max (1023) */
        {
            int j = 1;

            do {
                final int c1 = events[EntropyMV.mv_max + j]; /* positive */
                final int c2 = events[EntropyMV.mv_max - j]; /* negative */
                final int c = c1 + c2;
                int a = j;

                sign_ct[0] += c1;
                sign_ct[1] += c2;

                if (a < EntropyMV.mvnum_short) {
                    is_short_ct[0] += c; /* Short vector */
                    short_ct[a] += c; /* Magnitude distribution */
                } else {
                    int k = EntropyMV.mvlong_width - 1;
                    is_short_ct[1] += c; /* Long vector */

                    /* bit 3 not always encoded. */
                    do {
                        bit_ct[k][(a >> k) & 1] += c;

                    } while (--k >= 0);
                }
            } while (++j <= EntropyMV.mv_max);
        }

        Pnew[EntropyMV.mvpis_short] = calc_prob(Pnew[EntropyMV.mvpis_short], is_short_ct);

        Pnew[EntropyMV.MVPsign] = calc_prob(Pnew[EntropyMV.MVPsign], sign_ct);

        {
            short[] p = new short[EntropyMV.mvnum_short - 1]; /* actually only need branch ct */
            int j = 0;

            TreeCoder.vp8_tree_probs_from_distribution(8, Token.vp8_small_mvencodings, EntropyMode.vp8_small_mvtree, p,
                    short_bct, short_ct, 256, true);

            do {
                Pnew[EntropyMV.MVPshort + j] = calc_prob(Pnew[EntropyMV.MVPshort + j], short_bct[j]);

            } while (++j < EntropyMV.mvnum_short - 1);
        }

        {
            int j = 0;

            do {
                Pnew[EntropyMV.MVPbits + j] = calc_prob(Pnew[EntropyMV.MVPbits + j], bit_ct[j]);

            } while (++j < EntropyMV.mvlong_width);
        }

        updated |= update(w, is_short_ct, Pcur, EntropyMV.mvpis_short, Pnew[EntropyMV.mvpis_short],
                Pupdate.getRel(updateidx++));

        updated |= update(w, sign_ct, Pcur, EntropyMV.MVPsign, Pnew[EntropyMV.MVPsign], Pupdate.getRel(updateidx++));

        {
            int j = 0;

            do {
                updated |= update(w, short_bct[j], Pcur, j + EntropyMV.MVPshort, Pnew[j + EntropyMV.MVPshort],
                        Pupdate.getRel(updateidx++));

            } while (++j < EntropyMV.mvnum_short - 1);
        }

        {

            int j = 0;

            do {
                updated |= update(w, bit_ct[j], Pcur, j + EntropyMV.MVPbits, Pnew[j + EntropyMV.MVPbits],
                        Pupdate.getRel(updateidx++));

            } while (++j < EntropyMV.mvlong_width);
        }
        return updated;
    }

    static void vp8_write_mvprobs(Compressor cpi) {
        final BoolEncoder w = cpi.bc[0];
        MVContext[] mvc = cpi.common.fc.mvc;
        boolean[] flags = { false, false };
        flags[0] = write_component_probs(w, mvc[0], MVContext.vp8_default_mv_context[0],
                MVContext.vp8_mv_update_probs[0], cpi.mb.MVcount[0], 0);
        flags[1] = write_component_probs(w, mvc[1], MVContext.vp8_default_mv_context[1],
                MVContext.vp8_mv_update_probs[1], cpi.mb.MVcount[1], 1);

        if (flags[0] || flags[1]) {
            vp8_build_component_cost_table(cpi.mb.mvcost, cpi.common.fc.mvc, flags);
        }

    }
}
