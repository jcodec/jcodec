package org.jcodec.codecs.vpx.vp8.enums;

import org.jcodec.codecs.vpx.VP8Util;
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
public enum TokenAlphabet {

    ZERO_TOKEN(null, null, 0, 0, (short) 0, new Token(2, 2)) /* 0 Extra Bits 0+0 */,
    ONE_TOKEN(null, null, 0, 1, (short) 1, new Token(6, 3)) /* 1 Extra Bits 0+1 */,
    TWO_TOKEN(null, null, 0, 2, (short) 2, new Token(28, 5)) /* 2 Extra Bits 0+1 */,
    THREE_TOKEN(null, null, 0, 3, (short) 2, new Token(58, 6)) /* 3 Extra Bits 0+1 */,
    FOUR_TOKEN(null, null, 0, 4, (short) 2, new Token(59, 6)) /* 4 Extra Bits 0+1 */,
    DCT_VAL_CATEGORY1(new short[] { 0, 0 }, VP8Util.SubblockConstants.Pcat1, 1, 5, (short) 2,
            new Token(60, 6)) /* 5-6 Extra Bits 1+1 */,
    DCT_VAL_CATEGORY2(new short[] { 2, 2, 0, 0 }, VP8Util.SubblockConstants.Pcat2, 2, 7, (short) 2,
            new Token(61, 6)) /* 7-10 Extra Bits 2+1 */,
    DCT_VAL_CATEGORY3(new short[] { 2, 2, 4, 4, 0, 0 }, VP8Util.SubblockConstants.Pcat3, 3, 11, (short) 2,
            new Token(124, 7)) /* 11-18 Extra Bits 3+1 */,
    DCT_VAL_CATEGORY4(new short[] { 2, 2, 4, 4, 6, 6, 0, 0 }, VP8Util.SubblockConstants.Pcat4, 4, 19, (short) 2,
            new Token(125, 7)) /* 19-34 Extra Bits 4+1 */,
    DCT_VAL_CATEGORY5(new short[] { 2, 2, 4, 4, 6, 6, 8, 8, 0, 0 }, VP8Util.SubblockConstants.Pcat5, 5, 35, (short) 2,
            new Token(126, 7)) /* 35-66 Extra Bits 5+1 */,
    DCT_VAL_CATEGORY6(new short[] { 2, 2, 4, 4, 6, 6, 8, 8, 10, 10, 12, 12, 14, 14, 16, 16, 18, 18, 20, 20, 0, 0 },
            VP8Util.SubblockConstants.Pcat6, 11, 67, (short) 2, new Token(127, 7)) /* 67+ Extra Bits 11+1 */,
    DCT_EOB_TOKEN(null, null, 0, 0, (short) 0, new Token(0, 1)) /* EOB Extra Bits 0+0 */;

    public static final int entropyTokenCount = TokenAlphabet.values().length;

    public final short previousTokenClass;
    public final int len;
    public final int base_val;
    public final ReadOnlyIntArrPointer tree;
    public final ReadOnlyIntArrPointer prob;
    public final Token coefEncoding;

    private TokenAlphabet(short[] t, short[] p, int l, int b, short ptc, Token coef) {
        tree = t == null ? null : new ReadOnlyIntArrPointer(t, 0);
        prob = p == null ? null : new ReadOnlyIntArrPointer(p, 0);
        len = l;
        base_val = b;
        previousTokenClass = ptc;
        coefEncoding = coef;
    }
}