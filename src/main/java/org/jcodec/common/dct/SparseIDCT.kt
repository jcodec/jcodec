package org.jcodec.common.dct

import org.jcodec.common.dct.SimpleIDCT10Bit.idct10
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Performs IDCT of 8x8 block.
 *
 * See MPEGDecoder for example.
 *
 * @author The JCodec project
 */
object SparseIDCT {
    val COEFF = arrayOfNulls<IntArray>(64)
    const val PRECISION = 13
    const val DC_SHIFT = PRECISION - 3

    /**
     * Starts DCT reconstruction
     *
     * Faster then call to 'coeff' with ind = 0
     *
     * @param block
     * @param dc
     */
    @JvmStatic
    fun start(block: IntArray, dc: Int) {
        var dc = dc
        dc = dc shl DC_SHIFT
        var i = 0
        while (i < 64) {
            block[i + 0] = dc
            block[i + 1] = dc
            block[i + 2] = dc
            block[i + 3] = dc
            i += 4
        }
    }

    /**
     * Recalculates image based on new DCT coefficient
     *
     * @param block
     * @param ind
     * @param level
     */
    @JvmStatic
    fun coeff(block: IntArray, ind: Int, level: Int) {
        var i = 0
        while (i < 64) {
            block[i] += COEFF[ind]!![i] * level
            block[i + 1] += COEFF[ind]!![i + 1] * level
            block[i + 2] += COEFF[ind]!![i + 2] * level
            block[i + 3] += COEFF[ind]!![i + 3] * level
            i += 4
        }
    }

    /**
     * Finalizes DCT calculation
     *
     * @param block
     */
    @JvmStatic
    fun finish(block: IntArray) {
        var i = 0
        while (i < 64) {
            block[i] = div(block[i])
            block[i + 1] = div(block[i + 1])
            block[i + 2] = div(block[i + 2])
            block[i + 3] = div(block[i + 3])
            i += 4
        }
    }

    private operator fun div(x: Int): Int {
        val m = x shr 31
        val n = x ushr 31
        return ((x xor m) + n shr PRECISION xor m) + n
    }

    init {
        COEFF[0] = IntArray(64)
        COEFF[0]!!.fill(1 shl DC_SHIFT)
        val ac = 1 shl PRECISION
        for (i in 1..63) {
            COEFF[i] = IntArray(64)
            COEFF[i]!![i] = ac
            idct10(COEFF[i]!!, 0)
        }
    }
}