package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumSet;

import org.jcodec.codecs.vpx.vp8.BoolEncoder;
import org.jcodec.codecs.vpx.vp8.TreeWriter;
import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;

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
public class DCTValueConstants {
    private static final int[] dct_value_cost = new int[2048 * 2];
    private static final TokenValue[] dct_value_tokens = new TokenValue[2048 * 2];

    public static int getValueCost(int v) {
        return dct_value_cost[Entropy.DCT_MAX_VALUE + v];
    }

    public static TokenValue getTokenValue(int v) {
        return dct_value_tokens[Entropy.DCT_MAX_VALUE + v];
    }

    static {
        int i = -Entropy.DCT_MAX_VALUE;
        int sign = 1;

        do {
            if (i == 0)
                sign = 0;
            TokenAlphabet selected = null;
            int eb = sign;
            {
                int a = sign != 0 ? -i : i;
                if (a > 4) {
                    for (TokenAlphabet ta : EnumSet.range(TokenAlphabet.DCT_VAL_CATEGORY1,
                            TokenAlphabet.DCT_VAL_CATEGORY6)) {
                        if (ta.base_val > a) {
                            break;
                        }
                        selected = ta;
                    }
                    eb |= (a - selected.base_val) << 1;
                } else {
                    for (TokenAlphabet ta : EnumSet.range(TokenAlphabet.ZERO_TOKEN, TokenAlphabet.FOUR_TOKEN)) {
                        if (a == 0) {
                            selected = ta;
                        }
                        a--;
                    }
                }
                dct_value_tokens[Entropy.DCT_MAX_VALUE + i] = new TokenValue(selected, eb);
            }

            // initialize the cost for extra bits for all possible coefficient value.
            {
                int cost = 0;

                if (selected.base_val != 0) {
                    if (selected.len != 0)
                        cost += TreeWriter.vp8_treed_cost(selected.tree, selected.prob, eb >> 1, selected.len);

                    cost += TreeWriter.vp8_cost_bit(BoolEncoder.vp8_prob_half, eb & 1); // sign
                    dct_value_cost[Entropy.DCT_MAX_VALUE + i] = cost;
                }

            }

        } while (++i < Entropy.DCT_MAX_VALUE);

    }
}
