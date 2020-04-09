package org.jcodec.scale

import org.jcodec.common.model.Size

/**
 * Resamples image interpolating points using Lanczos sinc over sine windowed
 * filter.
 *
 * @author Stanislav Vitvitskiy
 */
class LanczosResampler(from: Size, to: Size) : BaseResampler(from, to) {
    private val precision = 256
    private val tapsXs: Array<ShortArray>
    private val tapsYs: Array<ShortArray>
    private val _scaleFactorX: Double
    private val _scaleFactorY: Double
    override fun getTapsX(dstX: Int): ShortArray {
        val oi = ((dstX * precision).toFloat() / _scaleFactorX).toInt()
        val sub_pel = oi % precision
        return tapsXs[sub_pel]
    }

    override fun getTapsY(dstY: Int): ShortArray {
        val oy = ((dstY * precision).toFloat() / _scaleFactorY).toInt()
        val sub_pel = oy % precision
        return tapsYs[sub_pel]
    }

    override fun nTaps(): Int {
        return _nTaps
    }

    companion object {
        //The type (or one of its parents) contains already a method called [nTaps]
        private const val _nTaps = 6
        private fun sinc(x: Double): Double {
            return (if (x == 0.0) 1.0 else Math.sin(x) / x).toDouble()
        }

        private fun buildTaps(nTaps: Int, precision: Int, scaleFactor: Double, tapsOut: Array<ShortArray>) {
            val taps = DoubleArray(nTaps)
            for (i in 0 until precision) {
                val o = i.toDouble() / precision
                var j = -nTaps / 2 + 1
                var t = 0
                while (j < nTaps / 2 + 1) {
                    val x = -o + j
                    val sinc_val = scaleFactor * sinc(scaleFactor * x * Math.PI)
                    val wnd_val = Math.sin(x * Math.PI / (nTaps - 1) + Math.PI / 2)
                    taps[t] = sinc_val * wnd_val
                    j++
                    t++
                }
                normalizeAndGenerateFixedPrecision(taps, 7, tapsOut[i])
            }
        }
    }

    init {
        _scaleFactorX = to.width.toDouble() / from.width
        _scaleFactorY = to.height.toDouble() / from.height
        tapsXs = Array(precision) { ShortArray(_nTaps) }
        tapsYs = Array(precision) { ShortArray(_nTaps) }
        buildTaps(_nTaps, precision, _scaleFactorX, tapsXs)
        buildTaps(_nTaps, precision, _scaleFactorY, tapsYs)
    }
}