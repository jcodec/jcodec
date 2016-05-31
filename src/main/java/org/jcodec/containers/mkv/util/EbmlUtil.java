package org.jcodec.containers.mkv.util;
import java.lang.StringBuilder;
/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class EbmlUtil {

    /**
     * Encodes unsigned integer with given length
     * 
     * @param value
     *            unsigned integer to be encoded
     * @param length
     *            ebml sequence length
     * @return
     */
    public static byte[] ebmlEncodeLen(long value, int length) {
        byte[] b = new byte[length];
        for (int idx = 0; idx < length; idx++) {
            // Rightmost bytes should go to end of array to preserve big-endian notation
            b[length - idx - 1] = (byte) ((value >>> (8 * idx)) & 0xFFL);
        }

        b[0] |= 0x80 >>> (length - 1);
        return b;
    }

    /**
     * Encodes unsigned integer value according to ebml convention
     * 
     * @param value
     *            unsigned integer to be encoded
     * @return
     */
    public static byte[] ebmlEncode(long value) {
        return ebmlEncodeLen(value, ebmlLength(value));
    }

    public static final byte[] lengthOptions = { 0, (byte) 0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01 };

    /**
     * This method is used mostly during reading EBML bitstream. It asnwers the question "What is the length of an integer (signed/unsigned) encountered in the bitstream"
     * 
     * @param b
     * @return
     */
    static public int computeLength(byte b) {
        if (b == 0x00)
            throw new RuntimeException("Invalid head element for ebml sequence");

        int i = 1;
        while ((b & lengthOptions[i]) == 0)
            i++;

        return i;
    }

    public static final long one = 0x7F;
    // 0x3F 0x80
    public static final long two = 0x3F80;
    // 0x1F 0xC0 0x00
    public static final long three = 0x1FC000;
    // 0x0F 0xE0 0x00 0x00
    public static final long four = 0x0FE00000;
    // 0x07 0xF0 0x00 0x00 0x00
    public static final long five = 0x07F0000000L;
    // 0x03 0xF8 0x00 0x00 0x00 0x00
    public static final long six = 0x03F800000000L;
    // 0x01 0xFC 0x00 0x00 0x00 0x00 0x00
    public static final long seven = 0x01FC0000000000L;
    // 0x00 0xFE 0x00 0x00 0x00 0x00 0x00 0x00
    public static final long eight = 0xFE000000000000L;
    public static final long[] ebmlLengthMasks = new long[] { 0, one, two, three, four, five, six, seven, eight };

    /**
     * This method is used mostly during writing EBML bitstream. It answers the following question "How many bytes should be used to encode unsigned integer value"
     * 
     * @param v
     *            unsigned integer to be encoded
     * @return
     */
    public static int ebmlLength(long v) {
        if (v == 0)
            return 1;

        int length = 8;

        while (length > 0 && (v & ebmlLengthMasks[length]) == 0)
            length--;

        return length;
    }

    public static String toHexString(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for (byte b : a)
            sb.append(String.format("0x%02x ", b & 0xff));
        return sb.toString();
    }

}
