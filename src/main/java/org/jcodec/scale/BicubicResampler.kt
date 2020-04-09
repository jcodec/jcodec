package org.jcodec.scale

import org.jcodec.common.model.Size

/**
 * Resamples image interpolating points using bicubic filter.
 *
 * The difference from Lanczoc resampler is that a separate set of taps are
 * generated for each destination point position.
 *
 * @author Stanislav Vitvitskiy
 */
class BicubicResampler(from: Size, to: Size) : BaseResampler(from, to) {
    private val horizontalTaps: Array<ShortArray>
    private val verticalTaps: Array<ShortArray>
    override fun getTapsX(dstX: Int): ShortArray {
        return horizontalTaps[dstX]
    }

    override fun getTapsY(dstY: Int): ShortArray {
        return verticalTaps[dstY]
    }

    override fun nTaps(): Int {
        return 4
    }

    companion object {
        private const val alpha = 0.6
        private fun buildFilterTaps(to: Int, from: Int): Array<ShortArray> {
            val taps = DoubleArray(4)
            val tapsOut = Array(to) { ShortArray(4) }
            val ratio = from.toDouble() / to
            val toByFrom = to.toDouble() / from
            var srcPos = 0.0
            for (i in 0 until to) {
                val fraction = srcPos - srcPos.toInt()
                for (t in -1..2) {
                    var d = t - fraction
                    if (to < from) {
                        d *= toByFrom
                    }
                    val x = Math.abs(d)
                    val xx = x * x
                    val xxx = xx * x
                    if (d >= -1 && d <= 1) {
                        taps[t + 1] = (2 - alpha) * xxx + (-3 + alpha) * xx + 1
                    } else if (d < -2 || d > 2) {
                        taps[t + 1] = 0.0
                    } else {
                        taps[t + 1] = -alpha * xxx + 5 * alpha * xx - 8 * alpha * x + 4 * alpha
                    }
                }
                normalizeAndGenerateFixedPrecision(taps, 7, tapsOut[i])
                srcPos += ratio
            }
            return tapsOut
        }
    }

    init {
        horizontalTaps = buildFilterTaps(to.width, from.width)
        verticalTaps = buildFilterTaps(to.height, from.height)
    }
}