package org.jcodec.common.dct;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IDCT2x2 {

    public static void idct(int[] blk, int off) {
        int x0 = blk[off], x1 = blk[off + 1], x2 = blk[off + 2], x3 = blk[off + 3];
        
        int t0 = x0 + x2;
        int t2 = x0 - x2;
        
        int t1 = x1 + x3;
        int t3 = x1 - x3;
        
        blk[off] = (t0 + t1) >> 3;
        blk[off + 1] = (t0 - t1) >> 3;
        blk[off + 2] = (t2 + t3) >> 3;
        blk[off + 3] = (t2 - t3) >> 3;
    }
}