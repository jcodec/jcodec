package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.common.io.VLC

/**
 * Returns the number of bits it would take to write certain types of CAVLC
 * symbols
 *
 * @author Stanislav Vitvitskyy
 */
class CAVLCRate {
    fun rateACBlock(i: Int, j: Int, p16x16: MBType?, p16x162: MBType?, ks: IntArray?, totalzeros16: Array<VLC?>?, k: Int, l: Int,
                    zigzag4x4: IntArray?): Int {
        // TODO Auto-generated method stub
        return 0
    }

    fun rateChrDCBlock(dc: IntArray?, totalzeros4: Array<VLC?>?, i: Int, length: Int, js: IntArray?): Int {
        // TODO Auto-generated method stub
        return 0
    }

    fun rateLumaDCBlock(mbLeftBlk: Int, mbTopBlk: Int, leftMBType: MBType?, topMBType: MBType?,
                        dc: IntArray?, totalzeros16: Array<VLC?>?, i: Int, j: Int, zigzag4x4: IntArray?): Int {
        // TODO Auto-generated method stub
        return 0
    }

    companion object {
        fun rateTE(refIdx: Int, i: Int): Int {
            // TODO Auto-generated method stub
            return 0
        }

        fun rateSE(i: Int): Int {
            // TODO Auto-generated method stub
            return 0
        }

        fun rateUE(i: Int): Int {
            // TODO Auto-generated method stub
            return 0
        }
    }
}