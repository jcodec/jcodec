package org.jcodec.codecs.common.biari

import org.jcodec.common.and
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * H264 CABAC M-Coder ( decoder module )
 *
 * @author The JCodec project
 */
open class MDecoder(private val _in: ByteBuffer, private val cm: Array<IntArray>) {
    private var range = 510
    private var code = 0
    private var nBitsPending = 0

    /**
     * Initializes code register. Loads 9 bits from the stream into working area
     * of code register ( bits 8 - 16) leaving 7 bits in the pending area of
     * code register (bits 0 - 7)
     *
     * @throws IOException
     */
    protected open fun initCodeRegister() {
        readOneByte()
        if (nBitsPending != 8) throw RuntimeException("Empty stream")
        code = code shl 8
        readOneByte()
        code = code shl 1
        nBitsPending -= 9
    }

    protected open fun readOneByte() {
        if (!_in.hasRemaining()) return
        val b: Int = _in.get() and 0xff
        code = code or b
        nBitsPending += 8
    }

    /**
     * Decodes one bin from arithmetice code word
     *
     * @param cm
     * @return
     * @throws IOException
     */
    open fun decodeBin(m: Int): Int {
        val bin: Int
        val qIdx = range shr 6 and 0x3
        val rLPS = MConst.rangeLPS[qIdx][cm[0][m]]
        range -= rLPS
        val rs8 = range shl 8
        if (code < rs8) {
            // MPS
            if (cm[0][m] < 62) cm[0][m]++
            renormalize()
            bin = cm[1][m]
        } else {
            // LPS
            range = rLPS
            code -= rs8
            renormalize()
            bin = 1 - cm[1][m]
            if (cm[0][m] == 0) cm[1][m] = 1 - cm[1][m]
            cm[0][m] = MConst.transitLPS[cm[0][m]]
        }

//        System.out.println("CABAC BIT [" + m + "]: " + bin);
        return bin
    }

    /**
     * Special decoding process for 'end of slice' flag. Uses probability state
     * 63.
     *
     * @param cm
     * @return
     * @throws IOException
     */
    open fun decodeFinalBin(): Int {
        range -= 2
        return if (code < range shl 8) {
            renormalize()
            //            System.out.println("CABAC BIT [-2]: 0");
            0
        } else {
//            System.out.println("CABAC BIT [-2]: 1");
            1
        }
    }

    /**
     * Special decoding process for symbols with uniform distribution
     *
     * @return
     * @throws IOException
     */
    open fun decodeBinBypass(): Int {
        code = code shl 1
        --nBitsPending
        if (nBitsPending <= 0) readOneByte()
        val tmp = code - (range shl 8)
        return if (tmp < 0) {
//            System.out.println("CABAC BIT [-1]: 0");
            0
        } else {
//            System.out.println("CABAC BIT [-1]: 1");
            code = tmp
            1
        }
    }

    /**
     * Shifts the current interval to either 1/2 or 0 (code = (code << 1) &
     * 0x1ffff) and scales it by 2 (range << 1).
     *
     * Reads new byte from the input stream into code value if there are no more
     * bits pending
     *
     * @throws IOException
     */
    private fun renormalize() {
        while (range < 256) {
            range = range shl 1
            code = code shl 1
            code = code and 0x1ffff
            --nBitsPending
            if (nBitsPending <= 0) readOneByte()
        }
    }

    init {
        initCodeRegister()
    }
}