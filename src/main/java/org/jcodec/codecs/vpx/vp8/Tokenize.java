package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.VP8Util;
import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.DCTValueConstants;
import org.jcodec.codecs.vpx.vp8.data.EntropyContextPlanes;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.TokenExtra;
import org.jcodec.codecs.vpx.vp8.data.TokenValue;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.enums.PlaneType;
import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;

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
public class Tokenize {

    private static TokenExtra configureToken(final FullAccessGenArrPointer<TokenExtra> tp, final TokenAlphabet tok,
            final Compressor cpi, final PlaneType type, final int band, final int pt, final boolean skip) {
        final int typ = type.ordinal();
        TokenExtra t = tp.get();
        if (t == null) {
            t = new TokenExtra();
            tp.setAndInc(t);
        } else {
            tp.inc();
        }
        t.Token = tok;
        t.context_tree = cpi.common.fc.coef_probs[typ][band][pt];
        t.skip_eob_node = skip;
        ++cpi.mb.coef_counts[typ][band][pt][tok.ordinal()];
        return t;
    }

    private static void configureAsEob(final FullAccessGenArrPointer<TokenExtra> tp, final Compressor cpi,
            final PlaneType type, final int band, final int pt, final boolean skip) {
        configureToken(tp, TokenAlphabet.DCT_EOB_TOKEN, cpi, type, band, pt, skip);
    }

    private static int configureAsGeneric(final FullAccessGenArrPointer<TokenExtra> tp, final Compressor cpi, final int v,
            final PlaneType type, final int band, final int pt, final boolean skip) {
        final TokenValue tv = DCTValueConstants.getTokenValue(v);
        final TokenAlphabet token = tv.token;
        final TokenExtra t = configureToken(tp, token, cpi, type, band, pt, skip);
        t.Extra = tv.extra;
        return token.previousTokenClass;
    }

    static void tokenize2nd_order_b(Macroblock x, FullAccessGenArrPointer<TokenExtra> tp, Compressor cpi) {
        MacroblockD xd = x.e_mbd;
        singleBlockfinalizeTokenize(PlaneType.Y2, tp, cpi, 24, xd.above_context.get().panes, xd.left_context.panes);
    }

    static void tokenize1st_order_b(Macroblock x, FullAccessGenArrPointer<TokenExtra> tp, PlaneType type,
            Compressor cpi) {
        final MacroblockD xd = x.e_mbd;
        final FullAccessIntArrPointer a = xd.above_context.get().panes, l = xd.left_context.panes;

        /* Luma */
        for (int block = 0; block < 16; block++) {
            singleBlockfinalizeTokenize(type, tp, cpi, block, a, l);
        }

        /* Chroma */
        for (int block = 16; block < 24; block++) {
            singleBlockfinalizeTokenize(PlaneType.UV, tp, cpi, block, a, l);
        }
    }

    private static void singleBlockfinalizeTokenize(final PlaneType type, final FullAccessGenArrPointer<TokenExtra> tp,
            final Compressor cpi, final int block, final FullAccessIntArrPointer a, final FullAccessIntArrPointer l) {
        final BlockD bd = cpi.mb.e_mbd.block.getRel(block);
        final int eob = bd.eob.get();
        final FullAccessIntArrPointer qcoeff_ptr = bd.qcoeff;
        final int aPos = BlockD.vp8_block2above[block];
        final int lPos = BlockD.vp8_block2left[block];
        int pt = a.getRel(aPos) + l.getRel(lPos); /* near block/prev token context index */
        int c = type.start_coeff;
        if (c >= eob) {
            /* c = band for this case */
            configureAsEob(tp, cpi, type, c, pt, false);
            a.setRel(aPos, l.setRel(lPos, (short) 0));
            return;
        }
        int v = qcoeff_ptr.getRel(c);
        pt = configureAsGeneric(tp, cpi, v, type, c, pt, false);
        c++;

        assert (eob <= 16);
        for (; c < eob; ++c) {
            int rc = VPXConst.zigzag[c];
            v = qcoeff_ptr.getRel(rc);
            pt = configureAsGeneric(tp, cpi, v, type, VP8Util.SubblockConstants.vp8CoefBands[c], pt, pt == 0);
        }
        if (c < 16) {
            configureAsEob(tp, cpi, type, VP8Util.SubblockConstants.vp8CoefBands[c], pt, false);
        }
        a.setRel(aPos, l.setRel(lPos, (short) 1));
    }

    private static void stuffOrderHelper(FullAccessGenArrPointer<TokenExtra> tp, FullAccessIntArrPointer a,
            FullAccessIntArrPointer l, Compressor cpi, Macroblock x, PlaneType plane, int band) {
        final int pt = a.get() + l.get(); /* near block/prev token context index */
        configureAsEob(tp, cpi, plane, band, pt, false);
        a.set(l.set((short) 0));
    }

    static void stuff2nd_order_b(FullAccessGenArrPointer<TokenExtra> tp, FullAccessIntArrPointer a,
            FullAccessIntArrPointer l, Compressor cpi, Macroblock x) {
        stuffOrderHelper(tp, a, l, cpi, x, PlaneType.Y2, 0);
    }

    static void stuff1st_order_b(FullAccessGenArrPointer<TokenExtra> tp, FullAccessIntArrPointer a,
            FullAccessIntArrPointer l, PlaneType type, Compressor cpi, Macroblock x) {
        stuffOrderHelper(tp, a, l, cpi, x, type, (type == PlaneType.Y_NO_DC ? 0 : 1));
    }

    static void stuff1st_order_buv(FullAccessGenArrPointer<TokenExtra> tp, FullAccessIntArrPointer a,
            FullAccessIntArrPointer l, Compressor cpi, Macroblock x) {
        stuffOrderHelper(tp, a, l, cpi, x, PlaneType.UV, 0);
    }

    static void vp8_stuff_mb(Compressor cpi, Macroblock x, FullAccessGenArrPointer<TokenExtra> t) {
        MacroblockD xd = x.e_mbd;
        EntropyContextPlanes A = xd.above_context.get();
        EntropyContextPlanes L = xd.left_context;
        PlaneType plane_type = PlaneType.Y_WITH_DC;
        int b;
        if (xd.hasSecondOrder()) {
            stuff2nd_order_b(t, A.panes.shallowCopyWithPosInc(BlockD.vp8_block2above[24]),
                    L.panes.shallowCopyWithPosInc(BlockD.vp8_block2left[24]), cpi, x);
            plane_type = PlaneType.Y_NO_DC;
        }

        for (b = 0; b < 16; ++b) {
            stuff1st_order_b(t, A.panes.shallowCopyWithPosInc(BlockD.vp8_block2above[b]),
                    L.panes.shallowCopyWithPosInc(BlockD.vp8_block2left[b]), plane_type, cpi, x);
        }

        for (b = 16; b < 24; ++b) {
            stuff1st_order_buv(t, A.panes.shallowCopyWithPosInc(BlockD.vp8_block2above[b]),
                    L.panes.shallowCopyWithPosInc(BlockD.vp8_block2left[b]), cpi, x);
        }
    }

    static boolean mb_is_skippable(MacroblockD x, boolean has_y2_block) {
        boolean skip = true;
        int i = 0;
        int imax = 24;

        if (has_y2_block) {
            imax++;
            for (i = 0; i < 16 && skip; ++i)
                skip &= (x.eobs.getRel(i) < 2);
        }

        for (; i < imax && skip; ++i)
            skip &= (x.eobs.getRel(i) == 0);

        return skip;
    }

    static void vp8_fix_contexts(MacroblockD x) {
        /* Clear entropy contexts for Y2 blocks */
        if (x.hasSecondOrder()) {
            x.above_context.get().panes.memset(0, (short) 0, x.above_context.get().panes.size());
            x.left_context.panes.memset(0, (short) 0, x.left_context.panes.size());
        } else {
            x.above_context.get().panes.memset(0, (short) 0, x.above_context.get().panes.size() - 1);
            x.left_context.panes.memset(0, (short) 0, x.left_context.panes.size() - 1);
        }
    }

    static void vp8_tokenize_mb(Compressor cpi, Macroblock x, FullAccessGenArrPointer<TokenExtra> t) {
        MacroblockD xd = x.e_mbd;
        PlaneType plane_type;
        boolean has_y2_block;

        has_y2_block = xd.hasSecondOrder();

        if (xd.mode_info_context.get().mbmi.mb_skip_coeff = mb_is_skippable(xd, has_y2_block)) {
            if (!cpi.common.mb_no_coeff_skip) {
                vp8_stuff_mb(cpi, x, t);
            } else {
                vp8_fix_contexts(xd);
                x.skip_true_count++;
            }

            return;
        }

        plane_type = PlaneType.Y_WITH_DC;
        if (has_y2_block) {
            tokenize2nd_order_b(x, t, cpi);
            plane_type = PlaneType.Y_NO_DC;
        }

        tokenize1st_order_b(x, t, plane_type, cpi);
    }

}
