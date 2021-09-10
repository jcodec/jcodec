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
public class TreeCoder {
    static void vp8_tree_probs_from_distribution(int n, /* n = size of alphabet */
            Token[] tok, ReadOnlyIntArrPointer tree, short[] probs, /* unsigned */ int[][] branch_ct,
            /* unsigned */ int[] num_events, /* unsigned */ int Pfactor, boolean Round) {
        final int tree_len = n - 1;
        int t = 0;

        branch_counts(n, tok, tree, branch_ct, num_events);

        do {
            final /* unsigned */ int[] c = branch_ct[t];
            final /* unsigned */ int tot = c[0] + c[1];

            if (tot != 0) {
                final /* unsigned */ short p = (/* unsigned */short) ((((long) c[0] * Pfactor) + (Round ? tot >> 1 : 0))
                        / tot);
                probs[t] = CommonUtils.clamp(p, (short) 1, (short) 255); /* agree w/old version for now */
            } else {
                probs[t] = BoolEncoder.vp8_prob_half;
            }
        } while (++t < tree_len);
    }

    static void branch_counts(int n, /* n = size of alphabet */
            Token[] tok, ReadOnlyIntArrPointer tree, /* unsigned */ int[][] branch_ct,
            /* unsigned */ int[] num_events) {
        final int tree_len = n - 1;
        int t = 0;
        assert (tree_len != 0);

        do {
            branch_ct[t][0] = branch_ct[t][1] = 0;
        } while (++t < tree_len);

        t = 0;

        do {
            int L = tok[t].len;
            final int enc = tok[t].value;
            final /* unsigned */ int ct = num_events[t];

            int i = 0;

            do {
                final int b = (enc >> --L) & 1;
                final int j = i >> 1;
                assert (j < tree_len && 0 <= L);

                branch_ct[j][b] += ct;
                i = tree.getRel(i + b);
            } while (i > 0);

            assert L == 0;
        } while (++t < n);
    }

}
