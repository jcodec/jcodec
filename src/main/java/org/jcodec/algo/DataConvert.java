package org.jcodec.algo;

import org.jcodec.api.NotSupportedException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Integer to byte and vice versa conversion routines
 * 
 * @author The JCodec project
 * 
 */
public class DataConvert {

    public static int[] from16BE(byte[] b) {
        int[] result = new int[b.length >> 1];
        int off = 0;
        for (int i = 0; i < result.length; i++) {
            result[i] = ((b[off++] & 0xff) << 8) | (b[off++] & 0xff);
        }

        return result;
    }

    public static int[] from24BE(byte[] b) {
        int[] result = new int[b.length / 3];
        int off = 0;
        for (int i = 0; i < result.length; i++) {
            result[i] = ((b[off++] & 0xff) << 16) | ((b[off++] & 0xff) << 8) | (b[off++] & 0xff);
        }

        return result;
    }

    public static int[] from16LE(byte[] b) {
        int[] result = new int[b.length >> 1];
        int off = 0;
        for (int i = 0; i < result.length; i++) {
            result[i] = (b[off++] & 0xff) | ((b[off++] & 0xff) << 8);
        }

        return result;
    }

    public static int[] from24LE(byte[] b) {
        int[] result = new int[b.length / 3];
        int off = 0;
        for (int i = 0; i < result.length; i++) {
            result[i] = (b[off++] & 0xff) | ((b[off++] & 0xff) << 8) | ((b[off++] & 0xff) << 16);
        }

        return result;
    }

    public static byte[] to16BE(int[] ia) {
        byte[] result = new byte[ia.length << 1];
        int off = 0;
        for (int i = 0; i < ia.length; i++) {
            result[off++] = (byte) ((ia[i] >> 8) & 0xff);
            result[off++] = (byte) (ia[i] & 0xff);
        }

        return result;
    }

    public static byte[] to24BE(int[] ia) {
        byte[] result = new byte[ia.length * 3];
        int off = 0;
        for (int i = 0; i < ia.length; i++) {
            result[off++] = (byte) ((ia[i] >> 16) & 0xff);
            result[off++] = (byte) ((ia[i] >> 8) & 0xff);
            result[off++] = (byte) (ia[i] & 0xff);
        }

        return result;
    }

    public static byte[] to16LE(int[] ia) {
        byte[] result = new byte[ia.length << 1];
        int off = 0;
        for (int i = 0; i < ia.length; i++) {
            result[off++] = (byte) (ia[i] & 0xff);
            result[off++] = (byte) ((ia[i] >> 8) & 0xff);
        }

        return result;
    }

    public static byte[] to24LE(int[] ia) {
        byte[] result = new byte[ia.length * 3];
        int off = 0;
        for (int i = 0; i < ia.length; i++) {
            result[off++] = (byte) (ia[i] & 0xff);
            result[off++] = (byte) ((ia[i] >> 8) & 0xff);
            result[off++] = (byte) ((ia[i] >> 16) & 0xff);
        }

        return result;
    }

    /**
     * Generic byte-array to integer-array conversion
     * 
     * Converts each depth-bit sequence from the input byte array into integer
     * 
     * @param b
     *            Input bytes
     * @param depth
     *            Bit depth of the integers
     * @param isBe
     *            If integers are big-endian or little-endian
     * @return
     */
    public static int[] fromByte(byte[] b, int depth, boolean isBe) {
        if (depth == 24)
            if (isBe)
                return from24BE(b);
            else
                return from24LE(b);
        else if (depth == 16)
            if (isBe)
                return from16BE(b);
            else
                return from16LE(b);
        throw new NotSupportedException("Conversion from " + depth + "bit "
                + (isBe ? "big endian" : "little endian") + " is not supported.");
    }

    /**
     * Generic integer-array to byte-array conversion
     * 
     * Converts each integer into depth-bit sequence in the output byte array
     * 
     * @param ia
     *            Integer array
     * @param depth
     *            Bit depth of the integers
     * @param isBe
     *            If integers are big-endian or little-endian
     * @return
     */
    public static byte[] toByte(int[] ia, int depth, boolean isBe) {
        if (depth == 24)
            if (isBe)
                return to24BE(ia);
            else
                return to24LE(ia);
        else if (depth == 16)
            if (isBe)
                return to16BE(ia);
            else
                return to16LE(ia);
        throw new NotSupportedException("Conversion to " + depth + "bit " + (isBe ? "big endian" : "little endian")
                + " is not supported.");
    }
}
