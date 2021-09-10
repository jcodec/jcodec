package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.Token;
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
public class TreeWriter {

    public static int vp8_cost_zero(int p) {
        return BoolEncoder.vp8_prob_cost[p];
    }

    public static int vp8_cost_one(int p) {
        return vp8_cost_zero(TreeWriter.vp8_complement(p));
    }

    public static int vp8_cost_bit(int x, int b) {
        return vp8_cost_zero(b != 0 ? TreeWriter.vp8_complement(x) : x);
    }

    public static /* unsigned */ int vp8_cost_branch(final /* unsigned */ int[] ct, int p) {
        return (/* unsigned */ int) (((((long) ct[0]) * vp8_cost_zero(p)) + (((long) ct[1]) * vp8_cost_one(p))) >> 8);
    }

    public static void vp8_write_literal(BoolEncoder bc, int data, int bits) {
        bc.vp8_encode_value(data, bits);
    }

    public static void vp8_treed_write(BoolEncoder w, ReadOnlyIntArrPointer t, ReadOnlyIntArrPointer p, int v,
            int n) { /* number of bits in v, assumed nonzero */
        int i = 0;

        do {
            final int b = (v >> --n) & 1;
            w.vp8_encode_bool(b == 1, p.getRel(i >> 1));
            i = t.getRel(i + b);
        } while (n != 0);
    }

    public static void vp8_write_token(BoolEncoder w, ReadOnlyIntArrPointer t, ReadOnlyIntArrPointer p, Token x) {
        vp8_treed_write(w, t, p, x.value, x.len);
    }

    public static int vp8_treed_cost(ReadOnlyIntArrPointer t, ReadOnlyIntArrPointer p, int v,
            int n) { /* number of bits in v, assumed nonzero */
        int c = 0;
        int i = 0;
        do {
            final int b = (v >> --n) & 1;
            c += vp8_cost_bit(p.getRel(i >> 1), b);
            i = t.getRel(i + b);
        } while (n != 0);

        return c;
    }

    static int vp8_cost_token(ReadOnlyIntArrPointer t, ReadOnlyIntArrPointer p, Token x) {
        return vp8_treed_cost(t, p, x.value, x.len);
    }

    static void cost(int[] C, ReadOnlyIntArrPointer T, ReadOnlyIntArrPointer P, int i, int c) {
        int p = P.getRel(i >> 1);

        do {
            int j = T.getRel(i);
            int d = c + vp8_cost_bit(p, i & 1);

            if (j <= 0) {
                C[-j] = d;
            } else {
                cost(C, T, P, j, d);
            }
        } while ((++i & 1) != 0);
    }

    public static void vp8_cost_tokens(int[] c, ReadOnlyIntArrPointer p, ReadOnlyIntArrPointer t) {
        cost(c, t, p, 0, 0);
    }

    static void vp8_cost_tokens2(int[] c, ReadOnlyIntArrPointer p, ReadOnlyIntArrPointer t, int start) {
        cost(c, t, p, start, 0);
    }

    static int vp8_complement(int p) {
        return 255 - p;
    }

}
