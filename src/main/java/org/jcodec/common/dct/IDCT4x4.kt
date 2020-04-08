package org.jcodec.common.dct

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object IDCT4x4 {
    @JvmStatic
    fun idct(blk: IntArray, off: Int) {
        var i: Int
        i = 0
        while (i < 4) {
            idct4row(blk, off + (i shl 2))
            i++
        }
        i = 0
        while (i < 4) {
            idct4col_add(blk, off + i)
            i++
        }
    }

    const val CN_SHIFT = 12
    fun C_FIX(x: Double): Int {
        return (x * 1.414213562 * (1 shl CN_SHIFT) + 0.5).toInt()
    }

    val C1 = C_FIX(0.6532814824)
    val C2 = C_FIX(0.2705980501)
    val C3 = C_FIX(0.5)
    const val C_SHIFT = 4 + 2 + 12
    private fun idct4col_add(blk: IntArray, off: Int) {
        val c0: Int
        val c1: Int
        val c2: Int
        val c3: Int
        val a0: Int
        val a1: Int
        val a2: Int
        val a3: Int
        a0 = blk[off]
        a1 = blk[off + 4]
        a2 = blk[off + 8]
        a3 = blk[off + 12]
        c0 = (a0 + a2) * C3 + (1 shl C_SHIFT - 1)
        c2 = (a0 - a2) * C3 + (1 shl C_SHIFT - 1)
        c1 = a1 * C1 + a3 * C2
        c3 = a1 * C2 - a3 * C1
        blk[off] = c0 + c1 shr C_SHIFT
        blk[off + 4] = c2 + c3 shr C_SHIFT
        blk[off + 8] = c2 - c3 shr C_SHIFT
        blk[off + 12] = c0 - c1 shr C_SHIFT
    }

    const val RN_SHIFT = 15
    fun R_FIX(x: Double): Int {
        return (x * 1.414213562 * (1 shl RN_SHIFT) + 0.5).toInt()
    }

    val R1 = R_FIX(0.6532814824)
    val R2 = R_FIX(0.2705980501)
    val R3 = R_FIX(0.5)
    const val R_SHIFT = 11
    private fun idct4row(blk: IntArray, off: Int) {
        val c0: Int
        val c1: Int
        val c2: Int
        val c3: Int
        val a0: Int
        val a1: Int
        val a2: Int
        val a3: Int
        a0 = blk[off]
        a1 = blk[off + 1]
        a2 = blk[off + 2]
        a3 = blk[off + 3]
        c0 = (a0 + a2) * R3 + (1 shl R_SHIFT - 1)
        c2 = (a0 - a2) * R3 + (1 shl R_SHIFT - 1)
        c1 = a1 * R1 + a3 * R2
        c3 = a1 * R2 - a3 * R1
        blk[off] = c0 + c1 shr R_SHIFT
        blk[off + 1] = c2 + c3 shr R_SHIFT
        blk[off + 2] = c2 - c3 shr R_SHIFT
        blk[off + 3] = c0 - c1 shr R_SHIFT
    }
}