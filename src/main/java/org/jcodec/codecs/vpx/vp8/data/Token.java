package org.jcodec.codecs.vpx.vp8.data;

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
public class Token {
    public static final Token[] vp8_bmode_encodings = { new Token(0, 1), new Token(2, 2), new Token(6, 3),
            new Token(28, 5), new Token(30, 5), new Token(58, 6), new Token(59, 6), new Token(62, 6), new Token(126, 7),
            new Token(127, 7) };

    public static final Token[] vp8_ymode_encodings = { new Token(0, 1), new Token(4, 3), new Token(5, 3),
            new Token(6, 3), new Token(7, 3) };

    public static final Token[] vp8_kf_ymode_encodings = { new Token(4, 3), new Token(5, 3), new Token(6, 3),
            new Token(7, 3), new Token(0, 1) };

    public static final Token[] vp8_uv_mode_encodings = { new Token(0, 1), new Token(2, 2), new Token(6, 3),
            new Token(7, 3) };

    public static final Token[] vp8_mbsplit_encodings = { new Token(6, 3), new Token(7, 3), new Token(2, 2),
            new Token(0, 1) };

    public static final Token[] vp8_mv_ref_encoding_array = { new Token(2, 2), new Token(6, 3), new Token(0, 1),
            new Token(14, 4), new Token(15, 4) };

    public static final Token[] vp8_sub_mv_ref_encoding_array = { new Token(0, 1), new Token(2, 2), new Token(6, 3),
            new Token(7, 3) };

    public static final Token[] vp8_small_mvencodings = { new Token(0, 3), new Token(1, 3), new Token(2, 3),
            new Token(3, 3), new Token(4, 3), new Token(5, 3), new Token(6, 3), new Token(7, 3) };

    public int value;
    public int len;

    public Token(int v, int l) {
        value = v;
        len = l;
    }
}
