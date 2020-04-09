package org.jcodec.scale

import org.jcodec.common.model.Picture
import org.jcodec.common.model.Size
import org.jcodec.common.tools.MathUtil

/**
 * Resamples image interpolating points using Lanczos sinc over sine windowed
 * filter.
 *
 * @author Stanislav Vitvitskiy
 */
abstract class BaseResampler(private val fromSize: Size, private val toSize: Size) {
    private val tempBuffers: ThreadLocal<IntArray> = ThreadLocal()
    private val scaleFactorX: Double = fromSize.width.toDouble() / toSize.width
    private val scaleFactorY: Double = fromSize.height.toDouble() / toSize.height
    protected abstract fun getTapsX(dstX: Int): ShortArray
    protected abstract fun getTapsY(dstY: Int): ShortArray
    protected abstract fun nTaps(): Int

    /**
     * Interpolates points using a 2d convolution
     */
    //Wrong usage of Javascript keyword:in
    fun resample(src: Picture, dst: Picture) {
        var temp = tempBuffers.get()
        val taps = nTaps()
        if (temp == null) {
            temp = IntArray(toSize.width * (fromSize.height + taps))
            tempBuffers.set(temp)
        }
        for (p in 0 until src.color.nComp) {
            // Horizontal pass
            for (y in 0 until src.getPlaneHeight(p) + taps) {
                for (x in 0 until dst.getPlaneWidth(p)) {
                    val tapsXs = getTapsX(x)
                    val srcX = (scaleFactorX * x).toInt() - taps / 2 + 1
                    var sum = 0
                    for (i in 0 until taps) {
                        sum += (getPel(src, p, srcX + i, y - taps / 2 + 1) + 128) * tapsXs[i]
                    }
                    temp[y * toSize.width + x] = sum
                }
            }

            // Vertical pass
            for (y in 0 until dst.getPlaneHeight(p)) {
                for (x in 0 until dst.getPlaneWidth(p)) {
                    val tapsYs = getTapsY(y)
                    val srcY = (scaleFactorY * y).toInt()
                    var sum = 0
                    for (i in 0 until taps) {
                        sum += temp[x + (srcY + i) * toSize.width] * tapsYs[i]
                    }
                    dst.getPlaneData(p)[y * dst.getPlaneWidth(p) + x] = (MathUtil.clip(sum + 8192 shr 14, 0,
                            255) - 128).toByte()
                }
            }
        }
    }

    companion object {
        private fun getPel(pic: Picture, plane: Int, x: Int, y: Int): Byte {
            var x = x
            var y = y
            if (x < 0) x = 0
            if (y < 0) y = 0
            val w = pic.getPlaneWidth(plane)
            if (x > w - 1) x = w - 1
            val h = pic.getPlaneHeight(plane)
            if (y > h - 1) y = h - 1
            return pic.data[plane][x + y * w]
        }

        /**
         * Converts floating point taps to fixed precision taps.
         *
         * @param taps
         * The 64 bit double representation
         * @param precBits
         * Precision bits
         * @param out
         * Taps converted to fixed precision
         */
        @JvmStatic
        fun normalizeAndGenerateFixedPrecision(taps: DoubleArray, precBits: Int, out: ShortArray) {
            var sum = 0.0
            for (i in taps.indices) {
                sum += taps[i]
            }
            var sumFix = 0
            val precNum = 1 shl precBits
            for (i in taps.indices) {
                val d = taps[i] * precNum / sum + precNum
                val s = d.toInt()
                taps[i] = d - s
                out[i] = (s - precNum).toShort()
                sumFix += out[i]
            }
            var tapsTaken: Long = 0
            while (sumFix < precNum) {
                var maxI = -1
                for (i in taps.indices) {
                    if (tapsTaken and (1 shl i).toLong() == 0L && (maxI == -1 || taps[i] > taps[maxI])) maxI = i
                }
                out[maxI]++
                sumFix++
                tapsTaken = tapsTaken or (1 shl maxI).toLong()
            }
            for (i in taps.indices) {
                taps[i] = taps[i] + out[i]
                if (tapsTaken and (1 shl i).toLong() != 0L) {
                    taps[i] = taps[i] - 1
                }
            }
        }
    }
}