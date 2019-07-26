package org.jcodec.codecs.h264.encode;

import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.common.io.VLC;

/**
 * Returns the number of bits it would take to write certain types of CAVLC
 * symbols
 * 
 * @author Stanislav Vitvitskyy
 */
public class CAVLCRate {

    public static int rateTE(int refIdx, int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static int rateSE(int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static int rateUE(int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int rateACBlock(int i, int j, MBType p16x16, MBType p16x162, int[] ks, VLC[] totalzeros16, int k, int l,
            int[] zigzag4x4) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int rateChrDCBlock(int[] dc, VLC[] totalzeros4, int i, int length, int[] js) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int rateLumaDCBlock(int mbLeftBlk, int mbTopBlk, MBType leftMBType, MBType topMBType,
            int[] dc, VLC[] totalzeros16, int i, int j, int[] zigzag4x4) {
        // TODO Auto-generated method stub
        return 0;
    }
}
