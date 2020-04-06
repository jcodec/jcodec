package org.jcodec.codecs.h264.decode

import org.jcodec.common.ArrayUtil
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Integer DCT 4x4 base implementation
 *
 * @author The JCodec project
 */
object CoeffTransformer {
    @JvmField
    var zigzag4x4 = intArrayOf(0, 1, 4, 8, 5, 2, 3, 6, 9, 12, 13, 10, 7, 11, 14, 15)
    var invZigzag4x4 = IntArray(16)
    var dequantCoef = arrayOf(intArrayOf(10, 13, 10, 13, 13, 16, 13, 16, 10, 13, 10, 13, 13, 16, 13, 16), intArrayOf(11, 14, 11, 14, 14, 18, 14, 18, 11, 14, 11, 14, 14, 18, 14, 18), intArrayOf(13, 16, 13, 16, 16, 20, 16, 20, 13, 16, 13, 16, 16, 20, 16, 20), intArrayOf(14, 18, 14, 18, 18, 23, 18, 23, 14, 18, 14, 18, 18, 23, 18, 23), intArrayOf(16, 20, 16, 20, 20, 25, 20, 25, 16, 20, 16, 20, 20, 25, 20, 25), intArrayOf(18, 23, 18, 23, 23, 29, 23, 29, 18, 23, 18, 23, 23, 29, 23, 29))
    var dequantCoef8x8 = Array(6) { IntArray(64) }
    var initDequantCoeff8x8 = arrayOf(intArrayOf(20, 18, 32, 19, 25, 24), intArrayOf(22, 19, 35, 21, 28, 26), intArrayOf(26, 23, 42, 24, 33, 31), intArrayOf(28, 25, 45, 26, 35, 33), intArrayOf(32, 28, 51, 30, 40, 38), intArrayOf(36, 32, 58, 34, 46, 43))
    @JvmField
    var zigzag8x8 = intArrayOf(0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33,
            40, 48, 41, 34, 27, 20, 13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44,
            51, 58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63)
    var invZigzag8x8 = IntArray(64)
    private val quantCoeff = arrayOf(intArrayOf(13107, 8066, 13107, 8066, 8066, 5243, 8066, 5243, 13107, 8066, 13107, 8066, 8066, 5243, 8066, 5243), intArrayOf(11916, 7490, 11916, 7490, 7490, 4660, 7490, 4660, 11916, 7490, 11916, 7490, 7490, 4660, 7490, 4660), intArrayOf(10082, 6554, 10082, 6554, 6554, 4194, 6554, 4194, 10082, 6554, 10082, 6554, 6554, 4194, 6554, 4194), intArrayOf(9362, 5825, 9362, 5825, 5825, 3647, 5825, 3647, 9362, 5825, 9362, 5825, 5825, 3647, 5825, 3647), intArrayOf(8192, 5243, 8192, 5243, 5243, 3355, 5243, 3355, 8192, 5243, 8192, 5243, 5243, 3355, 5243, 3355), intArrayOf(7282, 4559, 7282, 4559, 4559, 2893, 4559, 2893, 7282, 4559, 7282, 4559, 4559, 2893, 4559, 2893))

    /**
     * Inverce integer DCT transform for 4x4 block
     *
     * @param block
     * @return
     */
    @JvmStatic
    fun idct4x4(block: IntArray) {
        _idct4x4(block, block)
    }

    fun _idct4x4(block: IntArray, out: IntArray) {
        // Horisontal
        run {
            var i = 0
            while (i < 16) {
                val e0 = block[i] + block[i + 2]
                val e1 = block[i] - block[i + 2]
                val e2 = (block[i + 1] shr 1) - block[i + 3]
                val e3 = block[i + 1] + (block[i + 3] shr 1)
                out[i] = e0 + e3
                out[i + 1] = e1 + e2
                out[i + 2] = e1 - e2
                out[i + 3] = e0 - e3
                i += 4
            }
        }

        // Vertical
        for (i in 0..3) {
            val g0 = out[i] + out[i + 8]
            val g1 = out[i] - out[i + 8]
            val g2 = (out[i + 4] shr 1) - out[i + 12]
            val g3 = out[i + 4] + (out[i + 12] shr 1)
            out[i] = g0 + g3
            out[i + 4] = g1 + g2
            out[i + 8] = g1 - g2
            out[i + 12] = g0 - g3
        }

        // scale down
        for (i in 0..15) {
            out[i] = out[i] + 32 shr 6
        }
    }

    @JvmStatic
    fun fdct4x4(block: IntArray) {
        // Horizontal
        run {
            var i = 0
            while (i < 16) {
                val t0 = block[i] + block[i + 3]
                val t1 = block[i + 1] + block[i + 2]
                val t2 = block[i + 1] - block[i + 2]
                val t3 = block[i] - block[i + 3]
                block[i] = t0 + t1
                block[i + 1] = (t3 shl 1) + t2
                block[i + 2] = t0 - t1
                block[i + 3] = t3 - (t2 shl 1)
                i += 4
            }
        }

        // Vertical
        for (i in 0..3) {
            val t0 = block[i] + block[i + 12]
            val t1 = block[i + 4] + block[i + 8]
            val t2 = block[i + 4] - block[i + 8]
            val t3 = block[i] - block[i + 12]
            block[i] = t0 + t1
            block[i + 4] = t2 + (t3 shl 1)
            block[i + 8] = t0 - t1
            block[i + 12] = t3 - (t2 shl 1)
        }
    }

    /**
     * Inverse Hadamard transform
     *
     * @param scaled
     */
    @JvmStatic
    fun invDC4x4(scaled: IntArray) {
        // Horisontal
        run {
            var i = 0
            while (i < 16) {
                val e0 = scaled[i] + scaled[i + 2]
                val e1 = scaled[i] - scaled[i + 2]
                val e2 = scaled[i + 1] - scaled[i + 3]
                val e3 = scaled[i + 1] + scaled[i + 3]
                scaled[i] = e0 + e3
                scaled[i + 1] = e1 + e2
                scaled[i + 2] = e1 - e2
                scaled[i + 3] = e0 - e3
                i += 4
            }
        }

        // Vertical
        for (i in 0..3) {
            val g0 = scaled[i] + scaled[i + 8]
            val g1 = scaled[i] - scaled[i + 8]
            val g2 = scaled[i + 4] - scaled[i + 12]
            val g3 = scaled[i + 4] + scaled[i + 12]
            scaled[i] = g0 + g3
            scaled[i + 4] = g1 + g2
            scaled[i + 8] = g1 - g2
            scaled[i + 12] = g0 - g3
        }
    }

    /**
     * Forward Hadamard transform
     *
     * @param scaled
     */
    @JvmStatic
    fun fvdDC4x4(scaled: IntArray) {
        // Horizontal
        run {
            var i = 0
            while (i < 16) {
                val t0 = scaled[i] + scaled[i + 3]
                val t1 = scaled[i + 1] + scaled[i + 2]
                val t2 = scaled[i + 1] - scaled[i + 2]
                val t3 = scaled[i] - scaled[i + 3]
                scaled[i] = t0 + t1
                scaled[i + 1] = t3 + t2
                scaled[i + 2] = t0 - t1
                scaled[i + 3] = t3 - t2
                i += 4
            }
        }

        // Vertical
        for (i in 0..3) {
            val t0 = scaled[i] + scaled[i + 12]
            val t1 = scaled[i + 4] + scaled[i + 8]
            val t2 = scaled[i + 4] - scaled[i + 8]
            val t3 = scaled[i] - scaled[i + 12]
            scaled[i] = t0 + t1 shr 1
            scaled[i + 4] = t2 + t3 shr 1
            scaled[i + 8] = t0 - t1 shr 1
            scaled[i + 12] = t3 - t2 shr 1
        }
    }

    @JvmStatic
    fun dequantizeAC(coeffs: IntArray, qp: Int, scalingList: IntArray?) {
        val group = qp % 6
        if (scalingList == null) {
            val qbits = qp / 6
            for (i in 0..15) coeffs[i] = coeffs[i] * dequantCoef[group][i] shl qbits
        } else {
            if (qp >= 24) {
                val qbits = qp / 6 - 4
                for (i in 0..15) coeffs[i] = coeffs[i] * dequantCoef[group][i] * scalingList[invZigzag4x4[i]] shl qbits
            } else {
                val qbits = 4 - qp / 6
                val addition = 1 shl 3 - qp / 6
                for (i in 0..15) coeffs[i] = coeffs[i] * scalingList[invZigzag4x4[i]] * dequantCoef[group][i] + addition shr qbits
            }
        }
    }

    @JvmStatic
    fun quantizeAC(coeffs: IntArray, qp: Int) {
        val level = qp / 6
        val offset = qp % 6
        val addition = 682 shl qp / 6 + 4
        val qbits = 15 + level
        if (qp < 10) {
            for (i in 0..15) {
                val sign = coeffs[i] shr 31
                coeffs[i] = ((Math.min(((coeffs[i] xor sign) - sign) * quantCoeff[offset][i] + addition shr qbits, 2063) xor sign)
                        - sign)
            }
        } else {
            for (i in 0..15) {
                val sign = coeffs[i] shr 31
                coeffs[i] = (((coeffs[i] xor sign) - sign) * quantCoeff[offset][i] + addition shr qbits xor sign) - sign
            }
        }
    }

    @JvmStatic
    fun dequantizeDC4x4(coeffs: IntArray, qp: Int, scalingList: IntArray?) {
        val group = qp % 6
        if (qp >= 36) {
            val qbits = qp / 6 - 6
            if (scalingList == null) {
                for (i in 0..15) coeffs[i] = coeffs[i] * (dequantCoef[group][0] shl 4) shl qbits
            } else {
                for (i in 0..15) coeffs[i] = coeffs[i] * dequantCoef[group][0] * scalingList[0] shl qbits
            }
        } else {
            val qbits = 6 - qp / 6
            val addition = 1 shl 5 - qp / 6
            if (scalingList == null) {
                for (i in 0..15) coeffs[i] = coeffs[i] * (dequantCoef[group][0] shl 4) + addition shr qbits
            } else {
                for (i in 0..15) coeffs[i] = coeffs[i] * dequantCoef[group][0] * scalingList[0] + addition shr qbits
            }
        }
    }

    @JvmStatic
    fun quantizeDC4x4(coeffs: IntArray, qp: Int) {
        val level = qp / 6
        val offset = qp % 6
        val addition = 682 shl qp / 6 + 5
        val qbits = 16 + level
        if (qp < 10) {
            for (i in 0..15) {
                val sign = coeffs[i] shr 31
                coeffs[i] = ((Math.min(((coeffs[i] xor sign) - sign) * quantCoeff[offset][0] + addition shr qbits, 2063) xor sign)
                        - sign)
            }
        } else {
            for (i in 0..15) {
                val sign = coeffs[i] shr 31
                coeffs[i] = (((coeffs[i] xor sign) - sign) * quantCoeff[offset][0] + addition shr qbits xor sign) - sign
            }
        }
    }

    /**
     * Inverse Hadamard 2x2
     *
     * @param block
     */
    @JvmStatic
    fun invDC2x2(block: IntArray) {
        val t0: Int
        val t1: Int
        val t2: Int
        val t3: Int
        t0 = block[0] + block[1]
        t1 = block[0] - block[1]
        t2 = block[2] + block[3]
        t3 = block[2] - block[3]
        block[0] = t0 + t2
        block[1] = t1 + t3
        block[2] = t0 - t2
        block[3] = t1 - t3
    }

    /**
     * Forward Hadamard 2x2
     *
     * @param dc2
     */
    @JvmStatic
    fun fvdDC2x2(block: IntArray) {
        invDC2x2(block)
    }

    @JvmStatic
    fun dequantizeDC2x2(transformed: IntArray, qp: Int, scalingList: IntArray?) {
        val group = qp % 6
        if (scalingList == null) {
            val shift = qp / 6
            for (i in 0..3) {
                transformed[i] = transformed[i] * dequantCoef[group][0] shl shift shr 1
            }
        } else {
            if (qp >= 24) {
                val qbits = qp / 6 - 4
                for (i in 0..3) {
                    transformed[i] = transformed[i] * dequantCoef[group][0] * scalingList[0] shl qbits shr 1
                }
            } else {
                val qbits = 4 - qp / 6
                val addition = 1 shl 3 - qp / 6
                for (i in 0..3) {
                    transformed[i] = transformed[i] * dequantCoef[group][0] * scalingList[0] + addition shr qbits shr 1
                }
            }
        }
    }

    @JvmStatic
    fun quantizeDC2x2(coeffs: IntArray, qp: Int) {
        val level = qp / 6
        val offset = qp % 6
        val addition = 682 shl qp / 6 + 5
        val qbits = 16 + level
        if (qp < 4) {
            for (i in 0..3) {
                val sign = coeffs[i] shr 31
                coeffs[i] = ((Math.min(((coeffs[i] xor sign) - sign) * quantCoeff[offset][0] + addition shr qbits, 2063) xor sign)
                        - sign)
            }
        } else {
            for (i in 0..3) {
                val sign = coeffs[i] shr 31
                coeffs[i] = (((coeffs[i] xor sign) - sign) * quantCoeff[offset][0] + addition shr qbits xor sign) - sign
            }
        }
    }

    @JvmStatic
    fun reorderDC4x4(dc: IntArray?) {
        ArrayUtil.swap(dc, 2, 4)
        ArrayUtil.swap(dc, 3, 5)
        ArrayUtil.swap(dc, 10, 12)
        ArrayUtil.swap(dc, 11, 13)
    }

    @JvmStatic
    fun fvdDC4x2(dc: IntArray?) {}
    @JvmStatic
    fun quantizeDC4x2(dc: IntArray?, qp: Int) {}
    @JvmStatic
    fun invDC4x2(dc: IntArray?) {
        // TODO Auto-generated method stub
    }

    @JvmStatic
    fun dequantizeDC4x2(dc: IntArray?, qp: Int) {
        // TODO Auto-generated method stub
    }

    /**
     * Coefficients are <<4 on exit
     *
     * @param coeffs
     * @param qp
     */
    fun dequantizeAC8x8(coeffs: IntArray, qp: Int, scalingList: IntArray?) {
        val group = qp % 6
        if (qp >= 36) {
            val qbits = qp / 6 - 6
            if (scalingList == null) {
                for (i in 0..63) coeffs[i] = coeffs[i] * dequantCoef8x8[group][i] shl 4 shl qbits
            } else {
                for (i in 0..63) coeffs[i] = coeffs[i] * dequantCoef8x8[group][i] * scalingList[invZigzag8x8[i]] shl qbits
            }
        } else {
            val qbits = 6 - qp / 6
            val addition = 1 shl 5 - qp / 6
            if (scalingList == null) {
                for (i in 0..63) coeffs[i] = coeffs[i] * (dequantCoef8x8[group][i] shl 4) + addition shr qbits
            } else {
                for (i in 0..63) coeffs[i] = coeffs[i] * dequantCoef8x8[group][i] * scalingList[invZigzag8x8[i]] + addition shr qbits
            }
        }
    }

    fun idct8x8(ac: IntArray) {
        var off = 0

        // Horizontal
        for (row in 0..7) {
            val e0 = ac[off] + ac[off + 4]
            val e1 = -ac[off + 3] + ac[off + 5] - ac[off + 7] - (ac[off + 7] shr 1)
            val e2 = ac[off] - ac[off + 4]
            val e3 = ac[off + 1] + ac[off + 7] - ac[off + 3] - (ac[off + 3] shr 1)
            val e4 = (ac[off + 2] shr 1) - ac[off + 6]
            val e5 = -ac[off + 1] + ac[off + 7] + ac[off + 5] + (ac[off + 5] shr 1)
            val e6 = ac[off + 2] + (ac[off + 6] shr 1)
            val e7 = ac[off + 3] + ac[off + 5] + ac[off + 1] + (ac[off + 1] shr 1)
            val f0 = e0 + e6
            val f1 = e1 + (e7 shr 2)
            val f2 = e2 + e4
            val f3 = e3 + (e5 shr 2)
            val f4 = e2 - e4
            val f5 = (e3 shr 2) - e5
            val f6 = e0 - e6
            val f7 = e7 - (e1 shr 2)
            ac[off] = f0 + f7
            ac[off + 1] = f2 + f5
            ac[off + 2] = f4 + f3
            ac[off + 3] = f6 + f1
            ac[off + 4] = f6 - f1
            ac[off + 5] = f4 - f3
            ac[off + 6] = f2 - f5
            ac[off + 7] = f0 - f7
            off += 8
        }

        // Vertical
        for (col in 0..7) {
            val e0 = ac[col] + ac[col + 32]
            val e1 = -ac[col + 24] + ac[col + 40] - ac[col + 56] - (ac[col + 56] shr 1)
            val e2 = ac[col] - ac[col + 32]
            val e3 = ac[col + 8] + ac[col + 56] - ac[col + 24] - (ac[col + 24] shr 1)
            val e4 = (ac[col + 16] shr 1) - ac[col + 48]
            val e5 = -ac[col + 8] + ac[col + 56] + ac[col + 40] + (ac[col + 40] shr 1)
            val e6 = ac[col + 16] + (ac[col + 48] shr 1)
            val e7 = ac[col + 24] + ac[col + 40] + ac[col + 8] + (ac[col + 8] shr 1)
            val f0 = e0 + e6
            val f1 = e1 + (e7 shr 2)
            val f2 = e2 + e4
            val f3 = e3 + (e5 shr 2)
            val f4 = e2 - e4
            val f5 = (e3 shr 2) - e5
            val f6 = e0 - e6
            val f7 = e7 - (e1 shr 2)
            ac[col] = f0 + f7
            ac[col + 8] = f2 + f5
            ac[col + 16] = f4 + f3
            ac[col + 24] = f6 + f1
            ac[col + 32] = f6 - f1
            ac[col + 40] = f4 - f3
            ac[col + 48] = f2 - f5
            ac[col + 56] = f0 - f7
        }

        // scale down
        for (i in 0..63) {
            ac[i] = ac[i] + 32 shr 6
        }
    }

    init {
        for (i in 0..15) invZigzag4x4[zigzag4x4[i]] = i
        for (i in 0..63) invZigzag8x8[zigzag8x8[i]] = i
    }

    init {
        for (g in 0..5) {
            Arrays.fill(dequantCoef8x8[g], initDequantCoeff8x8[g][5])
            run {
                var i = 0
                while (i < 8) {
                    var j = 0
                    while (j < 8) {
                        dequantCoef8x8[g][(i shl 3) + j] = initDequantCoeff8x8[g][0]
                        j += 4
                    }
                    i += 4
                }
            }
            run {
                var i = 1
                while (i < 8) {
                    var j = 1
                    while (j < 8) {
                        dequantCoef8x8[g][(i shl 3) + j] = initDequantCoeff8x8[g][1]
                        j += 2
                    }
                    i += 2
                }
            }
            run {
                var i = 2
                while (i < 8) {
                    var j = 2
                    while (j < 8) {
                        dequantCoef8x8[g][(i shl 3) + j] = initDequantCoeff8x8[g][2]
                        j += 4
                    }
                    i += 4
                }
            }
            run {
                var i = 0
                while (i < 8) {
                    var j = 1
                    while (j < 8) {
                        dequantCoef8x8[g][(i shl 3) + j] = initDequantCoeff8x8[g][3]
                        j += 2
                    }
                    i += 4
                }
            }
            run {
                var i = 1
                while (i < 8) {
                    var j = 0
                    while (j < 8) {
                        dequantCoef8x8[g][(i shl 3) + j] = initDequantCoeff8x8[g][3]
                        j += 4
                    }
                    i += 2
                }
            }
            run {
                var i = 0
                while (i < 8) {
                    var j = 2
                    while (j < 8) {
                        dequantCoef8x8[g][(i shl 3) + j] = initDequantCoeff8x8[g][4]
                        j += 4
                    }
                    i += 4
                }
            }
            var i = 2
            while (i < 8) {
                var j = 0
                while (j < 8) {
                    dequantCoef8x8[g][(i shl 3) + j] = initDequantCoeff8x8[g][4]
                    j += 4
                }
                i += 4
            }
        }
    }
}