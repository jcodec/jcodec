package org.jcodec.common.dct

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
object SimpleIDCT10Bit {
    private const val ROUND_COL = 8192
    private const val ROUND_ROW = 32768
    private const val SHIFT_COL = 14
    private const val SHIFT_ROW = 16
    const val C0 = 23170
    const val C1 = 32138
    const val C2 = 27246
    const val C3 = 18205
    const val C4 = 6393
    const val C5 = 30274
    const val C6 = 12540
    var W1 = 90901
    var W2 = 85627
    var W3 = 77062
    var W4 = 65535
    var W5 = 51491
    var W6 = 35468
    var W7 = 18081
    var ROW_SHIFT = 15
    var COL_SHIFT = 20
    @JvmStatic
    fun idct10(buf: IntArray, off: Int) {
        for (i in 0..7) idctRow(buf, off + (i shl 3))
        for (i in 0..7) idctCol(buf, off + i)
    }

    private fun idctCol(buf: IntArray, off: Int) {
        var a0: Int
        var a1: Int
        var a2: Int
        var a3: Int
        var b0: Int
        var b1: Int
        var b2: Int
        var b3: Int
        a0 = W4 * (buf[off + 8 * 0] + (1 shl COL_SHIFT - 1) / W4)
        a1 = a0
        a2 = a0
        a3 = a0
        a0 += W2 * buf[off + 8 * 2]
        a1 += W6 * buf[off + 8 * 2]
        a2 += -W6 * buf[off + 8 * 2]
        a3 += -W2 * buf[off + 8 * 2]
        b0 = W1 * buf[off + 8 * 1]
        b1 = W3 * buf[off + 8 * 1]
        b2 = W5 * buf[off + 8 * 1]
        b3 = W7 * buf[off + 8 * 1]
        b0 += W3 * buf[off + 8 * 3]
        b1 += -W7 * buf[off + 8 * 3]
        b2 += -W1 * buf[off + 8 * 3]
        b3 += -W5 * buf[off + 8 * 3]
        if (buf[off + 8 * 4] != 0) {
            a0 += W4 * buf[off + 8 * 4]
            a1 += -W4 * buf[off + 8 * 4]
            a2 += -W4 * buf[off + 8 * 4]
            a3 += W4 * buf[off + 8 * 4]
        }
        if (buf[off + 8 * 5] != 0) {
            b0 += W5 * buf[off + 8 * 5]
            b1 += -W1 * buf[off + 8 * 5]
            b2 += W7 * buf[off + 8 * 5]
            b3 += W3 * buf[off + 8 * 5]
        }
        if (buf[off + 8 * 6] != 0) {
            a0 += W6 * buf[off + 8 * 6]
            a1 += -W2 * buf[off + 8 * 6]
            a2 += W2 * buf[off + 8 * 6]
            a3 += -W6 * buf[off + 8 * 6]
        }
        if (buf[off + 8 * 7] != 0) {
            b0 += W7 * buf[off + 8 * 7]
            b1 += -W5 * buf[off + 8 * 7]
            b2 += W3 * buf[off + 8 * 7]
            b3 += -W1 * buf[off + 8 * 7]
        }
        buf[off] = a0 + b0 shr COL_SHIFT
        buf[off + 8] = a1 + b1 shr COL_SHIFT
        buf[off + 16] = a2 + b2 shr COL_SHIFT
        buf[off + 24] = a3 + b3 shr COL_SHIFT
        buf[off + 32] = a3 - b3 shr COL_SHIFT
        buf[off + 40] = a2 - b2 shr COL_SHIFT
        buf[off + 48] = a1 - b1 shr COL_SHIFT
        buf[off + 56] = a0 - b0 shr COL_SHIFT
    }

    private fun idctRow(buf: IntArray, off: Int) {
        var a0: Int
        var a1: Int
        var a2: Int
        var a3: Int
        var b0: Int
        var b1: Int
        var b2: Int
        var b3: Int
        a0 = W4 * buf[off] + (1 shl ROW_SHIFT - 1)
        a1 = a0
        a2 = a0
        a3 = a0
        a0 += W2 * buf[off + 2]
        a1 += W6 * buf[off + 2]
        a2 -= W6 * buf[off + 2]
        a3 -= W2 * buf[off + 2]
        b0 = W1 * buf[off + 1]
        b0 += W3 * buf[off + 3]
        b1 = W3 * buf[off + 1]
        b1 += -W7 * buf[off + 3]
        b2 = W5 * buf[off + 1]
        b2 += -W1 * buf[off + 3]
        b3 = W7 * buf[off + 1]
        b3 += -W5 * buf[off + 3]
        if (buf[off + 4] != 0 || buf[off + 5] != 0 || buf[off + 6] != 0 || buf[off + 7] != 0) {
            a0 += W4 * buf[off + 4] + W6 * buf[off + 6]
            a1 += -W4 * buf[off + 4] - W2 * buf[off + 6]
            a2 += -W4 * buf[off + 4] + W2 * buf[off + 6]
            a3 += W4 * buf[off + 4] - W6 * buf[off + 6]
            b0 += W5 * buf[off + 5]
            b0 += W7 * buf[off + 7]
            b1 += -W1 * buf[off + 5]
            b1 += -W5 * buf[off + 7]
            b2 += W7 * buf[off + 5]
            b2 += W3 * buf[off + 7]
            b3 += W3 * buf[off + 5]
            b3 += -W1 * buf[off + 7]
        }
        buf[off + 0] = a0 + b0 shr ROW_SHIFT
        buf[off + 7] = a0 - b0 shr ROW_SHIFT
        buf[off + 1] = a1 + b1 shr ROW_SHIFT
        buf[off + 6] = a1 - b1 shr ROW_SHIFT
        buf[off + 2] = a2 + b2 shr ROW_SHIFT
        buf[off + 5] = a2 - b2 shr ROW_SHIFT
        buf[off + 3] = a3 + b3 shr ROW_SHIFT
        buf[off + 4] = a3 - b3 shr ROW_SHIFT
    }

    @JvmStatic
    fun fdctProres10(block: IntArray, off: Int) {
        for (j in 0..7) {
            fdctCol(block, off + j)
        }
        var i = 0
        while (i < 64) {
            fdctRow(block, off + i)
            i += 8
        }
    }

    private fun fdctRow(block: IntArray, off: Int) {
        val z0 = block[off + 0] - block[off + 7]
        val z1 = block[off + 1] - block[off + 6]
        val z2 = block[off + 2] - block[off + 5]
        val z3 = block[off + 3] - block[off + 4]
        val z4 = block[off + 0] + block[off + 7]
        val z5 = block[off + 3] + block[off + 4]
        val z6 = block[off + 1] + block[off + 6]
        val z7 = block[off + 2] + block[off + 5]
        val u0 = z4 - z5
        val u1 = z6 - z7
        val c0 = (z4 + z5) * C0
        val c1 = (z6 + z7) * C0
        val c2 = u0 * C5
        val c3 = u1 * C6
        val c4 = u0 * C6
        val c5 = u1 * C5
        block[1 + off] = z0 * C1 + z1 * C2 + z2 * C3 + z3 * C4 + ROUND_ROW shr SHIFT_ROW
        block[3 + off] = z0 * C2 - z1 * C4 - z2 * C1 - z3 * C3 + ROUND_ROW shr SHIFT_ROW
        block[5 + off] = z0 * C3 - z1 * C1 + z2 * C4 + z3 * C2 + ROUND_ROW shr SHIFT_ROW
        block[7 + off] = z0 * C4 - z1 * C3 + z2 * C2 - z3 * C1 + ROUND_ROW shr SHIFT_ROW
        block[0 + off] = c0 + c1 + ROUND_ROW shr SHIFT_ROW
        block[2 + off] = c2 + c3 + ROUND_ROW shr SHIFT_ROW
        block[4 + off] = c0 - c1 + ROUND_ROW shr SHIFT_ROW
        block[6 + off] = c4 - c5 + ROUND_ROW shr SHIFT_ROW
    }

    private fun fdctCol(block: IntArray, off: Int) {
        val z0 = block[off + 0] - block[off + 56]
        val z1 = block[off + 8] - block[off + 48]
        val z2 = block[off + 16] - block[off + 40]
        val z3 = block[off + 24] - block[off + 32]
        val z4 = block[off + 0] + block[off + 56]
        val z5 = block[off + 24] + block[off + 32]
        val z6 = block[off + 8] + block[off + 48]
        val z7 = block[off + 16] + block[off + 40]
        val u0 = z4 - z5
        val u1 = z6 - z7
        val c0 = (z4 + z5) * C0
        val c1 = (z6 + z7) * C0
        val c2 = u0 * C5
        val c3 = u1 * C6
        val c4 = u0 * C6
        val c5 = u1 * C5
        block[8 + off] = z0 * C1 + z1 * C2 + z2 * C3 + z3 * C4 + ROUND_COL shr SHIFT_COL
        block[24 + off] = z0 * C2 - z1 * C4 - z2 * C1 - z3 * C3 + ROUND_COL shr SHIFT_COL
        block[40 + off] = z0 * C3 - z1 * C1 + z2 * C4 + z3 * C2 + ROUND_COL shr SHIFT_COL
        block[56 + off] = z0 * C4 - z1 * C3 + z2 * C2 - z3 * C1 + ROUND_COL shr SHIFT_COL
        block[0 + off] = c0 + c1 + ROUND_COL shr SHIFT_COL
        block[16 + off] = c2 + c3 + ROUND_COL shr SHIFT_COL
        block[32 + off] = c0 - c1 + ROUND_COL shr SHIFT_COL
        block[48 + off] = c4 - c5 + ROUND_COL shr SHIFT_COL
    }
}