package org.jcodec.codecs.common.biari;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Packed4BitList {

    private static int[] CLEAR_MASK = new int[] { ~(0xf | (0xf << 28)), ~(0xf | (0xf0 << 28)), ~(0xf | (0xf00 << 28)),
            ~(0xf | (0xf000 << 28)), ~(0xf | (0xf0000 << 28)), ~(0xf | (0xf00000 << 28)), ~(0xf | (0xf000000 << 28)) };

    /**
     * Creates packed 4bit list with 7 values in it
     * 
     * @return
     */
    public static int _7(int val0, int val1, int val2, int val3, int val4, int val5, int val6) {
        return (7 << 28) | ((val0 & 0xf) << 24) | ((val1 & 0xf) << 20) | ((val2 & 0xf) << 16) | ((val3 & 0xf) << 12)
                | ((val4 & 0xf) << 8) | ((val5 & 0xf) << 4) | ((val6 & 0xf));
    }
    
    public static int _3(int val0, int val1, int val2) {
        return _7(val0, val1, val2, 0, 0, 0, 0);
    }

    /**
     * Sets a 4 bit value into the list
     * 
     * @param list
     * @param val
     * @param n
     * @return
     */
    public static int set(int list, int val, int n) {
        int cnt = (list >> 28) & 0xf;
        int newc = n + 1;
        cnt = newc > cnt ? newc : cnt;
        return (list & CLEAR_MASK[n]) | ((val & 0xff) << (n << 2)) | (cnt << 28);
    }

    public static int get(int list, int n) {
        if (n > 6)
            return 0;
        return (list >> (n << 2)) & 0xff;
    }
}
