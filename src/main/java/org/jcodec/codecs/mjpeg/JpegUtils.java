package org.jcodec.codecs.mjpeg;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class JpegUtils {

    private final static int[] naturalOrder = new int[] { 0, 1, 8, 16, 9, 2, 3,
            10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34,
            27, 20, 13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29,
            22, 15, 23, 30, 37, 44, 51, 58, 59, 52, 45, 38, 31, 39, 46, 53, 60,
            61, 54, 47, 55, 62, 63 };
    private final static int[] zigzagOrder = new int[] { 0, 1, 5, 6, 14, 15,
            27, 28, 2, 4, 7, 13, 16, 26, 29, 42, 3, 8, 12, 17, 25, 30, 41, 43,
            9, 11, 18, 24, 31, 40, 44, 53, 10, 19, 23, 32, 39, 45, 52, 54, 20,
            22, 33, 38, 46, 51, 55, 60, 21, 34, 37, 47, 50, 56, 59, 61, 35, 36,
            48, 49, 57, 58, 62, 63, };

    public static void zigzagDecodeAll(int[][] src) {
        for (int i = 0; i < src.length; i++) {
            src[i] = zigzagDecode(src[i]);
        }
    }

    public static int[] zigzagDecode(int[] src) {
        int[] dst = new int[64];
        for (int i = 0; i < naturalOrder.length; i++) {
            dst[naturalOrder[i]] = src[i];
        }
        return dst;
    }

    public static byte[] zigzagEncode(byte[] data) {
        byte[] result = new byte[64];
        for (int i = 0; i < naturalOrder.length; i++) {
            result[i] = data[naturalOrder[i]];
        }
        return result;
    }

}
