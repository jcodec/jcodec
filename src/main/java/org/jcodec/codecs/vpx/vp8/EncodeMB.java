package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.VP8Util;
import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.DCTValueConstants;
import org.jcodec.codecs.vpx.vp8.data.EntropyContextPlanes;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.TokenState;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.PlaneType;
import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;
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
public class EncodeMB {
    static void vp8_subtract_b(Block be, BlockD bd, int pitch) {
        Subtract.vpx_subtract_block(4, 4, be.src_diff, pitch, be.getSrcPtr(), be.src_stride, bd.predictor, pitch);
    }

    static void vp8_subtract_mby(FullAccessIntArrPointer diff, ReadOnlyIntArrPointer src, int src_stride,
            ReadOnlyIntArrPointer pred, int pred_stride) {
        Subtract.vpx_subtract_block(16, 16, diff, 16, src, src_stride, pred, pred_stride);
    }

    static void build_dcblock(Macroblock x) {
        for (int i = 0, j = 384; i < 16; ++i, ++j) {
            x.src_diff.setAbs(j, x.coeff.getRel(i << 4));
        }
    }

    static void vp8_transform_intra_mby(Macroblock x) {
        int i;

        for (i = 0; i < 16; i += 2) {
            x.short_fdct8x4.call(x.block.getRel(i).src_diff, x.block.getRel(i).coeff, 32);
        }

        /* build dc block from 16 y dc values */
        build_dcblock(x);

        /* do 2nd order transform on the dc block */
        x.short_walsh4x4.call(x.block.getRel(24).src_diff, x.block.getRel(24).coeff, 8);
    }

    static int RDCOST(int RM, int DM, int R, int D) {
        return ((128 + R * RM) >> 8) + DM * D;
    }

    static int RDTRUNC(int RM, int DM, int R, int D) {
        return (128 + R * RM) & 0xFF;
    }

    static void optimize_b(Macroblock mb, int ib, PlaneType type, FullAccessIntArrPointer a,
            FullAccessIntArrPointer l) {
        Block b;
        BlockD d;
        TokenState[][] tokens = new TokenState[17][2];
        int[] best_mask = new int[2];
        ReadOnlyIntArrPointer dequant_ptr;
        ReadOnlyIntArrPointer coeff_ptr;
        FullAccessIntArrPointer qcoeff_ptr;
        FullAccessIntArrPointer dqcoeff_ptr;
        int eob;
        int i0;
        int rc;
        int x;
        int sz = 0;
        int next;
        int rdmult;
        int rddiv;
        int final_eob;
        int rd_cost0;
        int rd_cost1;
        int rate0;
        int rate1;
        int error0;
        int error1;
        TokenAlphabet t0;
        TokenAlphabet t1;
        int best;
        int band;
        int pt;
        int i;
        int err_mult = type.rd_mult;

        b = mb.block.getRel(ib);
        d = mb.e_mbd.block.getRel(ib);

        dequant_ptr = d.dequant.readOnly();
        coeff_ptr = b.coeff;
        qcoeff_ptr = d.qcoeff.shallowCopy();
        dqcoeff_ptr = d.dqcoeff.shallowCopy();
        i0 = type.start_coeff;
        eob = d.eob.get();

        /* Now set up a Viterbi trellis to evaluate alternative roundings. */
        rdmult = mb.rdmult * err_mult;
        if (mb.e_mbd.mode_info_context.get().mbmi.ref_frame == MVReferenceFrame.INTRA_FRAME) {
            rdmult = (rdmult * 9) >> 4;
        }

        rddiv = mb.rddiv;
        best_mask[0] = best_mask[1] = 0;
        /* Initialize the sentinel node of the trellis. */
        tokens[eob][0] = new TokenState(0, 0, 16, TokenAlphabet.DCT_EOB_TOKEN, 0);
        tokens[eob][1] = new TokenState(tokens[eob][0]);
        next = eob;
        for (i = eob; i-- > i0;) {
            int base_bits;
            int d2;
            int dx;

            rc = VPXConst.zigzag[i];
            x = qcoeff_ptr.getRel(rc);
            /* Only add a trellis state for non-zero coefficients. */
            if (x != 0) {
                boolean shortcut = false;
                error0 = tokens[next][0].error;
                error1 = tokens[next][1].error;
                /* Evaluate the first possibility for this state. */
                rate0 = tokens[next][0].rate;
                rate1 = tokens[next][1].rate;
                t0 = DCTValueConstants.getTokenValue(x).token;
                /* Consider both possible successor states. */
                if (next < 16) {
                    band = VP8Util.SubblockConstants.vp8CoefBands[i + 1];
                    rate0 += mb.token_costs[type.ordinal()][band][t0.previousTokenClass][tokens[next][0].token
                            .ordinal()];
                    rate1 += mb.token_costs[type.ordinal()][band][t0.previousTokenClass][tokens[next][1].token
                            .ordinal()];
                }
                rd_cost0 = RDCOST(rdmult, rddiv, rate0, error0);
                rd_cost1 = RDCOST(rdmult, rddiv, rate1, error1);
                if (rd_cost0 == rd_cost1) {
                    rd_cost0 = RDTRUNC(rdmult, rddiv, rate0, error0);
                    rd_cost1 = RDTRUNC(rdmult, rddiv, rate1, error1);
                }
                /* And pick the best. */
                best = (rd_cost1 < rd_cost0) ? 1 : 0;
                base_bits = DCTValueConstants.getValueCost(x);
                dx = dqcoeff_ptr.getRel(rc) - coeff_ptr.getRel(rc);
                d2 = dx * dx;
                tokens[i][0] = new TokenState(base_bits + (best > 0 ? rate1 : rate0), d2 + (best > 0 ? error1 : error0),
                        next, t0, x);
                best_mask[0] |= best << i;
                /* Evaluate the second possibility for this state. */
                rate0 = tokens[next][0].rate;
                rate1 = tokens[next][1].rate;

                if ((Math.abs(x) * dequant_ptr.getRel(rc) > Math.abs(coeff_ptr.getRel(rc))) && (Math.abs(x)
                        * dequant_ptr.getRel(rc) < Math.abs(coeff_ptr.getRel(rc)) + dequant_ptr.getRel(rc))) {
                    shortcut = true;
                }

                if (shortcut) {
                    sz = x < 0 ? -1 : 0;
                    x -= 2 * sz + 1;
                }

                /* Consider both possible successor states. */
                if (x == 0) {
                    /*
                     * If we reduced this coefficient to zero, check to see if we need to move the
                     * EOB back here.
                     */
                    t0 = tokens[next][0].token == TokenAlphabet.DCT_EOB_TOKEN ? TokenAlphabet.DCT_EOB_TOKEN
                            : TokenAlphabet.ZERO_TOKEN;
                    t1 = tokens[next][1].token == TokenAlphabet.DCT_EOB_TOKEN ? TokenAlphabet.DCT_EOB_TOKEN
                            : TokenAlphabet.ZERO_TOKEN;
                } else {
                    t0 = t1 = DCTValueConstants.getTokenValue(x).token;
                }
                if (next < 16) {
                    band = VP8Util.SubblockConstants.vp8CoefBands[i + 1];
                    if (t0 != TokenAlphabet.DCT_EOB_TOKEN) {
                        rate0 += mb.token_costs[type.ordinal()][band][t0.previousTokenClass][tokens[next][0].token
                                .ordinal()];
                    }
                    if (t1 != TokenAlphabet.DCT_EOB_TOKEN) {
                        rate1 += mb.token_costs[type.ordinal()][band][t1.previousTokenClass][tokens[next][1].token
                                .ordinal()];
                    }
                }

                rd_cost0 = RDCOST(rdmult, rddiv, rate0, error0);
                rd_cost1 = RDCOST(rdmult, rddiv, rate1, error1);
                if (rd_cost0 == rd_cost1) {
                    rd_cost0 = RDTRUNC(rdmult, rddiv, rate0, error0);
                    rd_cost1 = RDTRUNC(rdmult, rddiv, rate1, error1);
                }
                /* And pick the best. */
                best = (rd_cost1 < rd_cost0) ? 1 : 0;
                base_bits = DCTValueConstants.getValueCost(x);

                if (shortcut) {
                    dx -= (dequant_ptr.getRel(rc) + sz) ^ sz;
                    d2 = dx * dx;
                }
                tokens[i][1] = new TokenState(base_bits + (best > 0 ? rate1 : rate0), d2 + (best > 0 ? error1 : error0),
                        next, best > 0 ? t1 : t0, x);
                best_mask[1] |= best << i;
                /* Finally, make this the new head of the trellis. */
                next = i;
            }
            /*
             * There's no choice to make for a zero coefficient, so we don't add a new
             * trellis node, but we do need to update the costs.
             */
            else {
                band = VP8Util.SubblockConstants.vp8CoefBands[i + 1];
                t0 = tokens[next][0].token;
                t1 = tokens[next][1].token;
                /* Update the cost of each path if we're past the EOB token. */
                if (t0 != TokenAlphabet.DCT_EOB_TOKEN) {
                    tokens[next][0].rate += mb.token_costs[type.ordinal()][band][0][t0.ordinal()];
                    tokens[next][0].token = TokenAlphabet.ZERO_TOKEN;
                }
                if (t1 != TokenAlphabet.DCT_EOB_TOKEN) {
                    tokens[next][1].rate += mb.token_costs[type.ordinal()][band][0][t1.ordinal()];
                    tokens[next][1].token = TokenAlphabet.ZERO_TOKEN;
                }
                /* Don't update next, because we didn't add a new node. */
            }
        }

        /* Now pick the best path through the whole trellis. */
        band = VP8Util.SubblockConstants.vp8CoefBands[i + 1];
        pt = a.get() + l.get();
        rate0 = tokens[next][0].rate;
        rate1 = tokens[next][1].rate;
        error0 = tokens[next][0].error;
        error1 = tokens[next][1].error;
        t0 = tokens[next][0].token;
        t1 = tokens[next][1].token;
        rate0 += mb.token_costs[type.ordinal()][band][pt][t0.ordinal()];
        rate1 += mb.token_costs[type.ordinal()][band][pt][t1.ordinal()];
        rd_cost0 = RDCOST(rdmult, rddiv, rate0, error0);
        rd_cost1 = RDCOST(rdmult, rddiv, rate1, error1);
        if (rd_cost0 == rd_cost1) {
            rd_cost0 = RDTRUNC(rdmult, rddiv, rate0, error0);
            rd_cost1 = RDTRUNC(rdmult, rddiv, rate1, error1);
        }
        best = (rd_cost1 < rd_cost0) ? 1 : 0;
        final_eob = i0 - 1;
        for (i = next; i < eob; i = next) {
            x = tokens[i][best].qc;
            if (x != 0)
                final_eob = i;
            rc = VPXConst.zigzag[i];
            qcoeff_ptr.setRel(rc, (short) x);
            dqcoeff_ptr.setRel(rc, (short) (x * dequant_ptr.getRel(rc)));
            next = tokens[i][best].next;
            best = (best_mask[best] >> i) & 1;
        }
        final_eob++;

        a.set(l.set((short) (((final_eob != type.start_coeff)) ? 1 : 0)));
        d.eob.set((short) final_eob);
    }

    public static void check_reset_2nd_coeffs(MacroblockD x, PlaneType type, FullAccessIntArrPointer a,
            FullAccessIntArrPointer l) {
        int sum = 0;
        int i;
        BlockD bd = x.block.getRel(24);

        if (bd.dequant.get() >= 35 && bd.dequant.get() >= 35)
            return;

        for (i = 0; i < bd.eob.get(); ++i) {
            int coef = bd.dqcoeff.getRel(VPXConst.zigzag[i]);
            sum += (coef >= 0) ? coef : -coef;
            if (sum >= 35)
                return;
        }
        /**************************************************************************
         * our inverse hadamard transform effectively is weighted sum of all 16 inputs
         * with weight either 1 or -1. It has a last stage scaling of (sum+3)>>3. And dc
         * only idct is (dc+4)>>3. So if all the sums are between -35 and 29, the output
         * after inverse wht and idct will be all zero. A sum of absolute value smaller
         * than 35 guarantees all 16 different (+1/-1) weighted sums in wht fall between
         * -35 and +35.
         **************************************************************************/
        if (sum < 35) {
            for (i = 0; i < bd.eob.get(); ++i) {
                int rc = VPXConst.zigzag[i];
                bd.qcoeff.setRel(rc, (short) 0);
                bd.dqcoeff.setRel(rc, (short) 0);
            }
            bd.eob.set((short) 0);
            a.set(l.set((short) (((bd.eob.get() != type.start_coeff) ? 1 : 0))));
        }
    }

    static void vp8_optimize_mby(Macroblock x) {
        int b;
        PlaneType type;
        boolean has_2nd_order;

        EntropyContextPlanes t_above, t_left;
        FullAccessIntArrPointer ta;
        FullAccessIntArrPointer tl;

        if (x.e_mbd.above_context == null || x.e_mbd.left_context == null)
            return;
        t_above = new EntropyContextPlanes(x.e_mbd.above_context.get());
        t_left = new EntropyContextPlanes(x.e_mbd.left_context);

        ta = t_above.panes;
        tl = t_left.panes;

        has_2nd_order = x.e_mbd.hasSecondOrder();
        type = has_2nd_order ? PlaneType.Y_NO_DC : PlaneType.Y_WITH_DC;

        for (b = 0; b < 16; ++b) {
            ta.setPos(BlockD.vp8_block2above[b]);
            tl.setPos(BlockD.vp8_block2left[b]);
            optimize_b(x, b, type, ta, tl);
        }

        if (has_2nd_order) {
            b = 24;
            ta.setPos(BlockD.vp8_block2above[b]);
            tl.setPos(BlockD.vp8_block2left[b]);
            optimize_b(x, b, PlaneType.Y2, ta, tl);
            check_reset_2nd_coeffs(x.e_mbd, PlaneType.Y2, ta, tl);
        }
    }

    static void vp8_subtract_mbuv(FullAccessIntArrPointer diff, ReadOnlyIntArrPointer usrc, ReadOnlyIntArrPointer vsrc,
            int src_stride, ReadOnlyIntArrPointer upred, ReadOnlyIntArrPointer vpred, int pred_stride) {
        FullAccessIntArrPointer udiff = diff.shallowCopyWithPosInc(MacroblockD.USHIFT);
        FullAccessIntArrPointer vdiff = diff.shallowCopyWithPosInc(MacroblockD.VSHIFT);

        Subtract.vpx_subtract_block(8, 8, udiff, 8, usrc, src_stride, upred, pred_stride);
        Subtract.vpx_subtract_block(8, 8, vdiff, 8, vsrc, src_stride, vpred, pred_stride);
    }

    static void vp8_transform_mbuv(Macroblock x) {
        int i;

        for (i = 16; i < 24; i += 2) {
            x.short_fdct8x4.call(x.block.getRel(i).src_diff, x.block.getRel(i).coeff, 16);
        }
    }

    static void vp8_optimize_mbuv(Macroblock x) {
        int b;
        EntropyContextPlanes t_above, t_left;
        FullAccessIntArrPointer ta;
        FullAccessIntArrPointer tl;

        if (x.e_mbd.above_context.get() == null || x.e_mbd.left_context == null)
            return;
        t_above = new EntropyContextPlanes(x.e_mbd.above_context.get());
        t_left = new EntropyContextPlanes(x.e_mbd.left_context);

        ta = t_above.panes;
        tl = t_left.panes;

        for (b = 16; b < 24; ++b) {
            ta.setPos(BlockD.vp8_block2above[b]);
            tl.setPos(BlockD.vp8_block2left[b]);
            optimize_b(x, b, PlaneType.UV, ta, tl);
        }
    }

    static void vp8_subtract_mb(Macroblock x) {
        Block b = x.block.get();

        vp8_subtract_mby(x.src_diff, b.base_src, b.src_stride, x.e_mbd.dst.y_buffer, x.e_mbd.dst.y_stride);
        vp8_subtract_mbuv(x.src_diff, x.src.u_buffer, x.src.v_buffer, x.src.uv_stride, x.e_mbd.dst.u_buffer,
                x.e_mbd.dst.v_buffer, x.e_mbd.dst.uv_stride);
    }

    static void transform_mb(Macroblock x) {
        transform_mby(x);

        for (int i = 16; i < 24; i += 2) {
            x.short_fdct8x4.call(x.block.getRel(i).src_diff, x.block.getRel(i).coeff, 16);
        }

    }

    static void transform_mby(Macroblock x) {

        for (int i = 0; i < 16; i += 2) {
            final Block b = x.block.getRel(i);
            x.short_fdct8x4.call(b.src_diff, b.coeff, 32);
        }

        /* build dc block from 16 y dc values */
        if (x.e_mbd.mode_info_context.get().mbmi.mode != MBPredictionMode.SPLITMV) {
            build_dcblock(x);
            /* do 2nd order transform on the dc block */
            final Block b = x.block.getRel(24);
            x.short_walsh4x4.call(b.src_diff, b.coeff, 8);
        }
    }

    static void optimize_mb(Macroblock x) {
        int b;
        PlaneType type;
        boolean has_2nd_order;

        EntropyContextPlanes t_above = new EntropyContextPlanes(x.e_mbd.above_context.get()),
                t_left = new EntropyContextPlanes(x.e_mbd.left_context);
        FullAccessIntArrPointer ta, tl;

        ta = t_above.panes;
        tl = t_left.panes;

        has_2nd_order = x.e_mbd.hasSecondOrder();
        type = has_2nd_order ? PlaneType.Y_NO_DC : PlaneType.Y_WITH_DC;

        for (b = 0; b < 16; ++b) {
            ta.setPos(BlockD.vp8_block2above[b]);
            tl.setPos(BlockD.vp8_block2left[b]);
            optimize_b(x, b, type, ta, tl);
        }

        for (b = 16; b < 24; ++b) {
            ta.setPos(BlockD.vp8_block2above[b]);
            tl.setPos(BlockD.vp8_block2left[b]);
            optimize_b(x, b, PlaneType.UV, ta, tl);
        }

        if (has_2nd_order) {
            b = 24;
            ta.setPos(BlockD.vp8_block2above[b]);
            tl.setPos(BlockD.vp8_block2left[b]);
            optimize_b(x, b, PlaneType.Y2, ta, tl);
            check_reset_2nd_coeffs(x.e_mbd, PlaneType.Y2, ta, tl);
        }
    }

    static void vp8_encode_inter16x16(Macroblock x) {
        ReconInter.vp8_build_inter_predictors_mb(x.e_mbd);

        vp8_subtract_mb(x);

        transform_mb(x);

        Quantize.vp8_quantize_mb(x);

        if (x.optimize)
            optimize_mb(x);
    }

    /* this funciton is used by first pass only */
    static void vp8_encode_inter16x16y(Macroblock x) {
        Block b = x.block.get();

        ReconInter.vp8_build_inter16x16_predictors_mby(x.e_mbd, x.e_mbd.dst.y_buffer, x.e_mbd.dst.y_stride);

        vp8_subtract_mby(x.src_diff, b.base_src, b.src_stride, x.e_mbd.dst.y_buffer, x.e_mbd.dst.y_stride);

        transform_mby(x);

        Quantize.vp8_quantize_mby(x);

        InvTrans.vp8_inverse_transform_mby(x.e_mbd);
    }

}
