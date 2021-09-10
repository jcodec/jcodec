package org.jcodec.codecs.vpx.vp8;

import static org.jcodec.codecs.vpx.vp8.data.CommonData.Comp.AC;
import static org.jcodec.codecs.vpx.vp8.data.CommonData.Comp.DC;
import static org.jcodec.codecs.vpx.vp8.data.CommonData.Quant.UV;
import static org.jcodec.codecs.vpx.vp8.data.CommonData.Quant.Y1;
import static org.jcodec.codecs.vpx.vp8.data.CommonData.Quant.Y2;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.OnyxInt;
import org.jcodec.codecs.vpx.vp8.data.QuantCommon;
import org.jcodec.codecs.vpx.vp8.data.QuantDetails;
import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.enums.MBLvlFeatures;
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
public class Quantize {

    public static interface Quant {
        void call(Block b, BlockD d);
    }

    public static final Quant fastQuant = new Quant() {
        @Override
        public void call(Block b, BlockD d) {
            vp8_fast_quantize_b(b, d);
        }
    };

    public static final Quant regularQuant = new Quant() {
        @Override
        public void call(Block b, BlockD d) {
            vp8_regular_quantize_b(b, d);
        }
    };

    static final int qrounding_factor = 48;

    static final EnumMap<CommonData.Quant, int[]> qzbin_factors = new EnumMap<CommonData.Quant, int[]>(
            CommonData.Quant.class);

    static {
        int[] factors = new int[129];
        for (int i = 0; i < 129; i++) {
            factors[i] = i < 48 ? 84 : 80;
        }
        qzbin_factors.put(Y1, factors);
        qzbin_factors.put(Y2, factors);
        qzbin_factors.put(UV, factors);
    }

    static void vp8_quantize_mby(final Macroblock x) {
        quantizeblockRange(x, 0, 16);

        if (x.e_mbd.hasSecondOrder())
            quantizeblockRange(x, 24, 25);
    }

    static void vp8_quantize_mbuv(Macroblock x) {
        quantizeblockRange(x, 16, 24);
    }

    private static void quantizeblockRange(final Macroblock x, final int st, final int stop) {
        for (int i = st; i < stop; i++)
            x.quantize_b.call(x.block.getRel(i), x.e_mbd.block.getRel(i));
    }

    static void vp8_quantize_mb(Macroblock x) {
        final int has_2nd_order = x.e_mbd.hasSecondOrder() ? 1 : 0;
        quantizeblockRange(x, 0, 24 + has_2nd_order);
    }

    static void vp8cx_mb_init_quantizer(Compressor cpi, Macroblock x, boolean ok_to_skip) {
        int i;
        short QIndex;
        MacroblockD xd = x.e_mbd;
        ModeInfo mi = x.e_mbd.mode_info_context.get();

        /* Select the baseline MB Q index. */
        if (xd.segmentation_enabled != 0) {
            /* Abs Value */
            if (xd.mb_segement_abs_delta) {
                QIndex = xd.segment_feature_data[MBLvlFeatures.ALT_Q.ordinal()][mi.mbmi.segment_id];
                /* Delta Value */
            } else {
                QIndex = (short) (cpi.common.base_qindex
                        + xd.segment_feature_data[MBLvlFeatures.ALT_Q.ordinal()][mi.mbmi.segment_id]);
                /* Clamp to valid range */
                QIndex = CommonUtils.clamp(QIndex, OnyxInt.MINQ, OnyxInt.MAXQ);
            }
        } else {
            QIndex = cpi.common.base_qindex;
        }

        /*
         * This initialization should be called at least once. Use ok_to_skip to decide
         * if it is ok to skip. Before encoding a frame, this function is always called
         * with ok_to_skip =0, which means no skiping of calculations. The "last" values
         * are initialized at that time.
         */
        if (!ok_to_skip || QIndex != x.q_index) {
            xd.dequant_y1_dc.set((short) 1);
            xd.dequant_y1.set(cpi.common.dequant.get(Y1)[QIndex].get());
            xd.dequant_y2.set(cpi.common.dequant.get(Y2)[QIndex].get());
            xd.dequant_uv.set(cpi.common.dequant.get(UV)[QIndex].get());

            for (i = 1; i < 16; ++i) {
                xd.dequant_y1_dc.setRel(i, xd.dequant_y1.setRel(i, cpi.common.dequant.get(Y1)[QIndex].getRel(1)));
                xd.dequant_y2.setRel(i, cpi.common.dequant.get(Y2)[QIndex].getRel(1));
                xd.dequant_uv.setRel(i, cpi.common.dequant.get(UV)[QIndex].getRel(1));
            }
            /*
             * TODO: Remove dequant from BLOCKD. This is a temporary solution until the
             * quantizer code uses a passed in pointer to the dequant constants.
             */
            for (i = 0; i < 16; ++i)
                x.e_mbd.block.getRel(i).dequant = xd.dequant_y1;
            for (i = 16; i < 24; ++i)
                x.e_mbd.block.getRel(i).dequant = xd.dequant_uv;
            x.e_mbd.block.getRel(24).dequant = xd.dequant_y2;

            vp8_update_zbin_extra(cpi, x);

            /* Y */
            for (i = 0; i < 16; ++i) {
                cpi.q.get(Y1).shallowCopyTo(x.block.getRel(i), QIndex);
            }

            /* UV */

            for (; i < 24; ++i) {
                cpi.q.get(UV).shallowCopyTo(x.block.getRel(i), QIndex);
            }

            /* Y2 */
            cpi.q.get(Y2).shallowCopyTo(x.block.getRel(i), QIndex);

            /* save this macroblock QIndex for vp8_update_zbin_extra() */
            x.q_index = QIndex;

            x.last_zbin_over_quant = x.zbin_over_quant;
            x.last_zbin_mode_boost = x.zbin_mode_boost;
            x.last_act_zbin_adj = x.act_zbin_adj;

        } else if (x.last_zbin_over_quant != x.zbin_over_quant || x.last_zbin_mode_boost != x.zbin_mode_boost
                || x.last_act_zbin_adj != x.act_zbin_adj) {
            vp8_update_zbin_extra(cpi, x);
            x.last_zbin_over_quant = x.zbin_over_quant;
            x.last_zbin_mode_boost = x.zbin_mode_boost;
            x.last_act_zbin_adj = x.act_zbin_adj;
        }
    }

    static void vp8_update_zbin_extra(Compressor cpi, Macroblock x) {
        int i;
        int QIndex = x.q_index;
        int zbin_extra;

        /* Y */
        zbin_extra = ((cpi.common.dequant.get(Y1)[QIndex].getRel(1)
                * (x.zbin_over_quant + x.zbin_mode_boost + x.act_zbin_adj)) >> 7);

        for (i = 0; i < 16; ++i)
            x.block.getRel(i).zbin_extra = zbin_extra;

        /* UV */
        zbin_extra = ((cpi.common.dequant.get(UV)[QIndex].getRel(1)
                * (x.zbin_over_quant + x.zbin_mode_boost + x.act_zbin_adj)) >> 7);

        for (; i < 24; ++i)
            x.block.getRel(i).zbin_extra = zbin_extra;

        /* Y2 */
        zbin_extra = ((cpi.common.dequant.get(Y2)[QIndex].getRel(1)
                * ((x.zbin_over_quant / 2) + x.zbin_mode_boost + x.act_zbin_adj)) >> 7);
        x.block.getRel(i).zbin_extra = zbin_extra;
    }

    static void vp8_fast_quantize_b(Block b, BlockD d) {
        int i, rc, eob;
        int x, y, z, sz;
        final ReadOnlyIntArrPointer coeff_ptr = b.coeff;
        final ReadOnlyIntArrPointer round_ptr = b.round;
        final ReadOnlyIntArrPointer quant_ptr = b.quant_fast;
        final FullAccessIntArrPointer qcoeff_ptr = d.qcoeff;
        final FullAccessIntArrPointer dqcoeff_ptr = d.dqcoeff;
        final ReadOnlyIntArrPointer dequant_ptr = d.dequant;

        eob = -1;
        for (i = 0; i < 16; ++i) {
            rc = VPXConst.zigzag[i];
            z = coeff_ptr.getRel(rc);

            sz = (z >> 31); /* sign of z */
            x = (z ^ sz) - sz; /* x = abs(z) */

            y = ((x + round_ptr.getRel(rc)) * quant_ptr.getRel(rc)) >> 16; /* quantize (x) */
            x = (y ^ sz) - sz; /* get the sign back */
            qcoeff_ptr.setRel(rc, (short) x); /* write to destination */
            dqcoeff_ptr.setRel(rc, (short) (x * dequant_ptr.getRel(rc))); /* dequantized value */

            if (y != 0) {
                eob = i; /* last nonzero coeffs */
            }
        }
        d.eob.set((short) (eob + 1));
    }

    static void vp8_regular_quantize_b(final Block b, final BlockD d) {
        int i, rc, eob;
        int zbin;
        int x, y, z, sz;
        final PositionableIntArrPointer zbin_boost_ptr = b.zrun_zbin_boost.positionableOnly();
        final ReadOnlyIntArrPointer coeff_ptr = b.coeff;
        final ReadOnlyIntArrPointer zbin_ptr = b.zbin;
        final ReadOnlyIntArrPointer round_ptr = b.round;
        final ReadOnlyIntArrPointer quant_ptr = b.quant_fast;
        final FullAccessIntArrPointer qcoeff_ptr = d.qcoeff;
        final FullAccessIntArrPointer dqcoeff_ptr = d.dqcoeff;
        final ReadOnlyIntArrPointer dequant_ptr = d.dequant;
        final ReadOnlyIntArrPointer quant_shift_ptr = b.quant_shift;
        final int zbin_oq_value = b.zbin_extra;

        qcoeff_ptr.memset(0, (short) 0, 16);
        dqcoeff_ptr.memset(0, (short) 0, 16);

        eob = -1;

        for (i = 0; i < 16; ++i) {
            rc = VPXConst.zigzag[i];
            z = coeff_ptr.getRel(rc);

            zbin = zbin_ptr.getRel(rc) + zbin_boost_ptr.get() + zbin_oq_value;

            zbin_boost_ptr.inc();
            sz = (z >> 31); /* sign of z */
            x = (z ^ sz) - sz; /* x = abs(z) */

            if (x >= zbin) {
                x += round_ptr.getRel(rc);
                y = ((((x * quant_ptr.getRel(rc)) >> 16) + x) * quant_shift_ptr.getRel(rc)) >> 16; /* quantize (x) */
                x = (y ^ sz) - sz; /* get the sign back */
                qcoeff_ptr.setRel(rc, (short) x); /* write to destination */
                dqcoeff_ptr.setRel(rc, (short) (x * dequant_ptr.getRel(rc))); /* dequantized value */

                if (y != 0) {
                    eob = i; /* last nonzero coeffs */
                    zbin_boost_ptr.rewindToSaved(); /* reset zero runlength */
                }
            }
        }

        d.eob.set((short) (eob + 1));
    }

    static void vp8cx_frame_init_quantizer(Compressor cpi) {
        /* Clear Zbin mode boost for default case */
        cpi.mb.zbin_mode_boost = 0;

        /* MB level quantizer setup */
        vp8cx_mb_init_quantizer(cpi, cpi.mb, false);
    }

    static void invert_quant(final boolean improved_quant, final FullAccessIntArrPointer quant,
            final FullAccessIntArrPointer shift, final int d) {
        if (improved_quant) {
            int l, m;
            l = Integer.SIZE - Integer.numberOfLeadingZeros(d);
            m = 1 + (1 << (16 + l)) / d;
            quant.set((short) ((m - (1 << 16)) & 0xffff));
            shift.set((short) l);
            /* use multiplication and constant shift by 16 */
            shift.set((short) (1 << (16 - shift.get())));
        } else {
            quant.set((short) ((1 << 16) / d));
            shift.set((short) 0);
        }
    }

    public static void vp8cx_init_quantizer(Compressor cpi) {

        short[] zbin_boost = { 0, 0, 8, 10, 12, 14, 16, 20, 24, 28, 32, 36, 40, 44, 44, 44 };
        for (short Q = 0; Q < OnyxInt.QINDEX_RANGE; ++Q) {

            for (CommonData.Quant qenum : CommonData.Quant.values()) {
                QuantDetails q = cpi.q.get(qenum);
                // Setting the DC and the first AC value
                for (CommonData.Comp comp : CommonData.Comp.values()) {
                    short quant_val = QuantCommon.lookup.get(qenum).get(comp).call(Q,
                            cpi.common.delta_q.get(qenum).get(comp));
                    q.quant_fast[Q].setRel(comp.baseIndex, (short) ((1 << 16) / quant_val));
                    invert_quant(cpi.sf.improved_quant, q.quant[Q].shallowCopyWithPosInc(comp.baseIndex),
                            q.quant_shift[Q].shallowCopyWithPosInc(comp.baseIndex), quant_val);
                    q.zbin[Q].setRel(comp.baseIndex, (short) (((qzbin_factors.get(qenum)[Q] * quant_val) + 64) >> 7));
                    q.round[Q].setRel(comp.baseIndex, (short) ((qrounding_factor * quant_val) >> 7));
                    cpi.common.dequant.get(qenum)[Q].setRel(comp.baseIndex, quant_val);
                    q.zrun_zbin_boost[Q].setRel(comp.baseIndex,
                            (short) ((quant_val * zbin_boost[comp.baseIndex]) >> 7));
                }

                // Propagating the first AC value for the rest of the quant arrays
                q.quant_fast[Q].memset(2, q.quant_fast[Q].getRel(1), 14);
                q.quant[Q].memset(2, q.quant[Q].getRel(1), 14);
                q.quant_shift[Q].memset(2, q.quant_shift[Q].getRel(1), 14);
                q.zbin[Q].memset(2, q.zbin[Q].getRel(1), 14);
                q.round[Q].memset(2, q.round[Q].getRel(1), 14);
                for (int j = 2; j < q.zrun_zbin_boost[Q].size(); j++) {
                    q.zrun_zbin_boost[Q].setRel(j,
                            (short) ((cpi.common.dequant.get(qenum)[Q].getRel(1) * zbin_boost[j]) >> 7));
                }
            }
        }
    }

    static void vp8_set_quantizer(Compressor cpi, short Q) {
        CommonData cm = cpi.common;
        MacroblockD mbd = cpi.mb.e_mbd;
        boolean update = false;
        short new_delta_q;
        short new_uv_delta_q;
        cm.base_qindex = Q;

        /* if any of the delta_q values are changing update flag has to be set */
        /* currently only y2dc_delta_q may change */
        cm.delta_q.get(Y1).put(DC, (short) 0);
        cm.delta_q.get(Y2).put(AC, (short) 0);

        if (Q < 4) {
            new_delta_q = (short) (4 - Q);
        } else {
            new_delta_q = 0;
        }

        update |= cm.delta_q.get(Y2).get(DC) != new_delta_q;
        cm.delta_q.get(Y2).put(DC, new_delta_q);

        new_uv_delta_q = 0;
        // For screen content, lower the q value for UV channel. For now, select
        // conservative delta; same delta for dc and ac, and decrease it with lower
        // Q, and set to 0 below some threshold. May want to condition this in
        // future on the variance/energy in UV channel.
        if (cpi.oxcf.screen_content_mode != 0 && Q > 40) {
            new_uv_delta_q = (short) (-0.15 * Q);
            // Check range: magnitude of delta is 4 bits.
            if (new_uv_delta_q < -15) {
                new_uv_delta_q = -15;
            }
        }
        update |= cm.delta_q.get(UV).get(DC) != new_uv_delta_q;
        cm.delta_q.get(UV).put(DC, new_uv_delta_q);
        cm.delta_q.get(UV).put(AC, new_uv_delta_q);

        /* Set Segment specific quatizers */
        mbd.segment_feature_data[MBLvlFeatures.ALT_Q
                .ordinal()][0] = cpi.segment_feature_data[MBLvlFeatures.ALT_Q.ordinal()][0];
        mbd.segment_feature_data[MBLvlFeatures.ALT_Q
                .ordinal()][1] = cpi.segment_feature_data[MBLvlFeatures.ALT_Q.ordinal()][1];
        mbd.segment_feature_data[MBLvlFeatures.ALT_Q
                .ordinal()][2] = cpi.segment_feature_data[MBLvlFeatures.ALT_Q.ordinal()][2];
        mbd.segment_feature_data[MBLvlFeatures.ALT_Q
                .ordinal()][3] = cpi.segment_feature_data[MBLvlFeatures.ALT_Q.ordinal()][3];

        /* quantizer has to be reinitialized for any delta_q changes */
        if (update)
            vp8cx_init_quantizer(cpi);
    }

}
