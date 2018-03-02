package org.jcodec.codecs.vpx.vp9;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Functions to work with long storing up to two complete 31-bit motion vectors
 * with the number of vectors stored.
 * 
 * @author The JCodec project
 * 
 */
public class MVList {
    private static long LO_MASK = 0x7fffffff;
    private static long HI_MASK = LO_MASK << 31;
    private static long HI_MASK_NEG = ~(HI_MASK | (3L << 62));
    private static long LO_MASK_NEG = ~(LO_MASK | (3L << 62));

    public static long create(int mv0, int mv1) {
        return ((long) 2 << 62) | ((long) mv1 << 31) | (mv0 & LO_MASK);
    }

    public static long addUniq(long list, int mv) {
        long cnt = (list >> 62) & 0x3;
        if (cnt == 2)
            return list;
        if (cnt == 0) {
            return ((long) 1 << 62) | (list & LO_MASK_NEG) | (mv & LO_MASK);
        } else {
            int first = (int) (list & LO_MASK);
            if (first != mv)
                return ((long) 2 << 62) | (list & HI_MASK_NEG) | (((long) mv << 31) & HI_MASK);
            else
                return list;
        }
    }

    public static long add(long list, int mv) {
        long cnt = (list >> 62) & 0x3;
        if (cnt == 2)
            return list;
        if (cnt == 0) {
            return ((long) 1 << 62) | (list & LO_MASK_NEG) | (mv & LO_MASK);
        } else {
            return ((long) 2 << 62) | (list & HI_MASK_NEG) | (((long) mv << 31) & HI_MASK);
        }
    }

    public static int get(long list, int n) {
        if (n == 0)
            return (int) (list & LO_MASK);
        else
            return (int) ((list >> 31) & LO_MASK);
    }

    public static long set(long list, int n, int mv) {
        long cnt = (list >> 62) & 0x3;
        long newc = n + 1;
        cnt = newc > cnt ? newc : cnt;
        if (n == 0) {
            return (cnt << 62) | (list & LO_MASK_NEG) | (mv & LO_MASK);
        } else {
            return (cnt << 62) | (list & HI_MASK_NEG) | (((long) mv << 31) & HI_MASK);
        }
    }

    public static int size(long list) {
        return (int) ((list >> 62) & 0x3);
    }
}
