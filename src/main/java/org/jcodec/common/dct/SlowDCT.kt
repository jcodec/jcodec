package org.jcodec.common.dct

import org.jcodec.scale.ImageConvert

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
class SlowDCT : DCT {
    override fun decode(orig: IntArray): IntArray {
        val res = IntArray(64)
        var i = 0
        for (y in 0..7) {
            for (x in 0..7) {
                var sum = 0.0
                var pixOffset = 0
                for (u in 0..7) {
                    val cu: Double = if (u == 0) rSqrt2 else 1.0
                    for (v in 0..7) {
                        val cv: Double = if (v == 0) rSqrt2 else 1.0
                        val svu = orig[pixOffset].toDouble()
                        val c1 = (2 * x + 1) * v * Math.PI / 16.0
                        val c2 = (2 * y + 1) * u * Math.PI / 16.0
                        sum += cu * cv * svu * Math.cos(c1) * Math.cos(c2)
                        pixOffset++
                    }
                }
                sum *= 0.25
                sum = Math.round(sum + 128).toDouble()
                val isum = sum.toInt()
                res[i++] = ImageConvert.icrop(isum)
            }
        }
        return res
    }

    companion object {
        @JvmField
        val INSTANCE = SlowDCT()

        /** r - Reciprocal  */
        private val rSqrt2 = 1.0 / Math.sqrt(2.0)
    }
}