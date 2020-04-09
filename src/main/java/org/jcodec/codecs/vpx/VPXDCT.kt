package org.jcodec.codecs.vpx

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object VPXDCT {
    @JvmStatic
    fun fdct4x4(coef: IntArray) {
        run {
            var i = 0
            while (i < 16) {
                val a1 = coef[i] + coef[i + 3] shl 3
                val b1 = coef[i + 1] + coef[i + 2] shl 3
                val c1 = coef[i + 1] - coef[i + 2] shl 3
                val d1 = coef[i] - coef[i + 3] shl 3
                coef[i] = a1 + b1
                coef[i + 2] = a1 - b1
                coef[i + 1] = c1 * 2217 + d1 * 5352 + 14500 shr 12
                coef[i + 3] = d1 * 2217 - c1 * 5352 + 7500 shr 12
                i += 4
            }
        }
        for (i in 0..3) {
            val a1 = coef[i] + coef[i + 12]
            val b1 = coef[i + 4] + coef[i + 8]
            val c1 = coef[i + 4] - coef[i + 8]
            val d1 = coef[i] - coef[i + 12]
            coef[i] = a1 + b1 + 7 shr 4
            coef[i + 8] = a1 - b1 + 7 shr 4
            coef[i + 4] = (c1 * 2217 + d1 * 5352 + 12000 shr 16) + if (d1 != 0) 1 else 0
            coef[i + 12] = d1 * 2217 - c1 * 5352 + 51000 shr 16
        }
    }

    @JvmStatic
    fun walsh4x4(coef: IntArray) {
        run {
            var i = 0
            while (i < 16) {
                val a1 = coef[i] + coef[i + 2] shl 2
                val d1 = coef[i + 1] + coef[i + 3] shl 2
                val c1 = coef[i + 1] - coef[i + 3] shl 2
                val b1 = coef[i] - coef[i + 2] shl 2
                coef[i] = a1 + d1 + if (a1 != 0) 1 else 0
                coef[i + 1] = b1 + c1
                coef[i + 2] = b1 - c1
                coef[i + 3] = a1 - d1
                i += 4
            }
        }
        for (i in 0..3) {
            val a1 = coef[i] + coef[i + 8]
            val d1 = coef[i + 4] + coef[i + 12]
            val c1 = coef[i + 4] - coef[i + 12]
            val b1 = coef[i] - coef[i + 8]
            var a2 = a1 + d1
            var b2 = b1 + c1
            var c2 = b1 - c1
            var d2 = a1 - d1
            a2 += if (a2 < 0) 1 else 0
            b2 += if (b2 < 0) 1 else 0
            c2 += if (c2 < 0) 1 else 0
            d2 += if (d2 < 0) 1 else 0
            coef[i] = a2 + 3 shr 3
            coef[i + 4] = b2 + 3 shr 3
            coef[i + 8] = c2 + 3 shr 3
            coef[i + 12] = d2 + 3 shr 3
        }
    }

    const val cospi8sqrt2minus1 = 20091
    const val sinpi8sqrt2 = 35468
    @JvmStatic
    fun idct4x4(coef: IntArray) {
        for (i in 0..3) {
            val a1 = coef[i] + coef[i + 8]
            val b1 = coef[i] - coef[i + 8]
            var temp1 = coef[i + 4] * sinpi8sqrt2 shr 16
            var temp2 = coef[i + 12] + (coef[i + 12] * cospi8sqrt2minus1 shr 16)
            val c1 = temp1 - temp2
            temp1 = coef[i + 4] + (coef[i + 4] * cospi8sqrt2minus1 shr 16)
            temp2 = coef[i + 12] * sinpi8sqrt2 shr 16
            val d1 = temp1 + temp2
            coef[i] = a1 + d1
            coef[i + 12] = a1 - d1
            coef[i + 4] = b1 + c1
            coef[i + 8] = b1 - c1
        }
        var i = 0
        while (i < 16) {
            val a1 = coef[i] + coef[i + 2]
            val b1 = coef[i] - coef[i + 2]
            var temp1 = coef[i + 1] * sinpi8sqrt2 shr 16
            var temp2 = coef[i + 3] + (coef[i + 3] * cospi8sqrt2minus1 shr 16)
            val c1 = temp1 - temp2
            temp1 = coef[i + 1] + (coef[i + 1] * cospi8sqrt2minus1 shr 16)
            temp2 = coef[i + 3] * sinpi8sqrt2 shr 16
            val d1 = temp1 + temp2
            coef[i] = a1 + d1 + 4 shr 3
            coef[i + 3] = a1 - d1 + 4 shr 3
            coef[i + 1] = b1 + c1 + 4 shr 3
            coef[i + 2] = b1 - c1 + 4 shr 3
            i += 4
        }
    }

    @JvmStatic
    fun iwalsh4x4(coef: IntArray) {
        for (i in 0..3) {
            val a1 = coef[i] + coef[i + 12]
            val b1 = coef[i + 4] + coef[i + 8]
            val c1 = coef[i + 4] - coef[i + 8]
            val d1 = coef[i] - coef[i + 12]
            coef[i] = a1 + b1
            coef[i + 4] = c1 + d1
            coef[i + 8] = a1 - b1
            coef[i + 12] = d1 - c1
        }
        var i = 0
        while (i < 16) {
            val a1 = coef[i] + coef[i + 3]
            val b1 = coef[i + 1] + coef[i + 2]
            val c1 = coef[i + 1] - coef[i + 2]
            val d1 = coef[i] - coef[i + 3]
            val a2 = a1 + b1
            val b2 = c1 + d1
            val c2 = a1 - b1
            val d2 = d1 - c1
            coef[i] = a2 + 3 shr 3
            coef[i + 1] = b2 + 3 shr 3
            coef[i + 2] = c2 + 3 shr 3
            coef[i + 3] = d2 + 3 shr 3
            i += 4
        }
    }
}