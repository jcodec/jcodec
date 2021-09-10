package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;

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
public class Entropy {
    public final static int BLOCK_TYPES = 4;
    public final static int COEF_BANDS = 8;
    public final static int PREV_COEF_CONTEXTS = 3;
    public final static int ENTROPY_NODES = 11;
    public final static int DCT_MAX_VALUE = 2048;

    public final static PositionableIntArrPointer vp8_coef_tree = new PositionableIntArrPointer(
            new short[] { (short) -TokenAlphabet.DCT_EOB_TOKEN.ordinal(), 2, /* 0 = EOB */
                    (short) -TokenAlphabet.ZERO_TOKEN.ordinal(), 4, /* 1 = ZERO */
                    (short) -TokenAlphabet.ONE_TOKEN.ordinal(), 6, /* 2 = ONE */
                    8, 12, /* 3 = LOW_VAL */
                    (short) -TokenAlphabet.TWO_TOKEN.ordinal(), 10, /* 4 = TWO */
                    (short) -TokenAlphabet.THREE_TOKEN.ordinal(), (short) -TokenAlphabet.FOUR_TOKEN.ordinal(), /*
                                                                                                                * 5 =
                                                                                                                * THREE
                                                                                                                */
                    14, 16, /* 6 = HIGH_LOW */
                    (short) -TokenAlphabet.DCT_VAL_CATEGORY1.ordinal(),
                    (short) -TokenAlphabet.DCT_VAL_CATEGORY2.ordinal(), /*
                                                                         * 7 = CAT_ONE
                                                                         */
                    18, 20, /* 8 = CAT_THREEFOUR */
                    (short) -TokenAlphabet.DCT_VAL_CATEGORY3.ordinal(),
                    (short) -TokenAlphabet.DCT_VAL_CATEGORY4.ordinal(), /*
                                                                         * 9 = CAT_THREE
                                                                         */
                    (short) -TokenAlphabet.DCT_VAL_CATEGORY5.ordinal(),
                    (short) -TokenAlphabet.DCT_VAL_CATEGORY6.ordinal() /* 10 = CAT_FIVE */
            }, 0);

    public final static int[] vp8_mb_feature_data_bits = { 7, 6 };
}
