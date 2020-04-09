package org.jcodec.scale

import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class Yuv420pToRgb : Transform {
    override fun transform(src: Picture, dst: Picture) {
        val yh = src.getPlaneData(0)
        val uh = src.getPlaneData(1)
        val vh = src.getPlaneData(2)
        var yl: ByteArray? = null
        var ul: ByteArray? = null
        var vl: ByteArray? = null
        val low = src.lowBits
        if (low != null) {
            yl = low[0]
            ul = low[1]
            vl = low[2]
        }
        val data = dst.getPlaneData(0)
        val lowBits = if (dst.lowBits == null) null else dst.lowBits[0]
        val hbd = src.isHiBD && dst.isHiBD
        val lowBitsNumSrc = src.lowBitsNum
        val lowBitsNumDst = dst.lowBitsNum
        var offLuma = 0
        var offChroma = 0
        val stride = dst.width
        for (i in 0 until (dst.height shr 1)) {
            for (k in 0 until (dst.width shr 1)) {
                val j = k shl 1
                if (hbd) {
                    YUV420pToRGBH2H(yh[offLuma + j], yl!![offLuma + j], uh[offChroma], ul!![offChroma], vh[offChroma],
                            vl!![offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst, (offLuma + j) * 3)
                    YUV420pToRGBH2H(yh[offLuma + j + 1], yl[offLuma + j + 1], uh[offChroma], ul[offChroma],
                            vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
                            (offLuma + j + 1) * 3)
                    YUV420pToRGBH2H(yh[offLuma + j + stride], yl[offLuma + j + stride], uh[offChroma], ul[offChroma],
                            vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
                            (offLuma + j + stride) * 3)
                    YUV420pToRGBH2H(yh[offLuma + j + stride + 1], yl[offLuma + j + stride + 1], uh[offChroma],
                            ul[offChroma], vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
                            (offLuma + j + stride + 1) * 3)
                } else {
                    YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3)
                    YUV420pToRGBN2N(yh[offLuma + j + 1], uh[offChroma], vh[offChroma], data, (offLuma + j + 1) * 3)
                    YUV420pToRGBN2N(yh[offLuma + j + stride], uh[offChroma], vh[offChroma], data,
                            (offLuma + j + stride) * 3)
                    YUV420pToRGBN2N(yh[offLuma + j + stride + 1], uh[offChroma], vh[offChroma], data, (offLuma + j
                            + stride + 1) * 3)
                }
                ++offChroma
            }
            if (dst.width and 0x1 != 0) {
                val j = dst.width - 1
                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3)
                YUV420pToRGBN2N(yh[offLuma + j + stride], uh[offChroma], vh[offChroma], data, (offLuma + j + stride) * 3)
                ++offChroma
            }
            offLuma += 2 * stride
        }
        if (dst.height and 0x1 != 0) {
            for (k in 0 until (dst.width shr 1)) {
                val j = k shl 1
                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3)
                YUV420pToRGBN2N(yh[offLuma + j + 1], uh[offChroma], vh[offChroma], data, (offLuma + j + 1) * 3)
                ++offChroma
            }
            if (dst.width and 0x1 != 0) {
                val j = dst.width - 1
                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3)
                ++offChroma
            }
        }
    }

    companion object {
        fun YUV420pToRGBN2N(y: Byte, u: Byte, v: Byte, data: ByteArray, off: Int) {
            val c = y + 112
            val r = 298 * c + 409 * v + 128 shr 8
            val g = 298 * c - 100 * u - 208 * v + 128 shr 8
            val b = 298 * c + 516 * u + 128 shr 8
            data[off] = (MathUtil.clip(r, 0, 255) - 128).toByte()
            data[off + 1] = (MathUtil.clip(g, 0, 255) - 128).toByte()
            data[off + 2] = (MathUtil.clip(b, 0, 255) - 128).toByte()
        }

        fun YUV420pToRGBH2H(yh: Byte, yl: Byte, uh: Byte, ul: Byte, vh: Byte, vl: Byte, nlbi: Int,
                            data: ByteArray, lowBits: ByteArray?, nlbo: Int, off: Int) {
            val clipMax = (1 shl nlbo shl 8) - 1
            val round = 1 shl nlbo shr 1
            val c = (yh + 128 shl nlbi) + yl - 64
            val d = (uh + 128 shl nlbi) + ul - 512
            val e = (vh + 128 shl nlbi) + vl - 512
            val r = MathUtil.clip(298 * c + 409 * e + 128 shr 8, 0, clipMax)
            val g = MathUtil.clip(298 * c - 100 * d - 208 * e + 128 shr 8, 0, clipMax)
            val b = MathUtil.clip(298 * c + 516 * d + 128 shr 8, 0, clipMax)
            val valR = MathUtil.clip(r + round shr nlbo, 0, 255)
            data[off] = (valR - 128).toByte()
            lowBits!![off] = (r - (valR shl nlbo)).toByte()
            val valG = MathUtil.clip(g + round shr nlbo, 0, 255)
            data[off + 1] = (valG - 128).toByte()
            lowBits[off + 1] = (g - (valG shl nlbo)).toByte()
            val valB = MathUtil.clip(b + round shr nlbo, 0, 255)
            data[off + 2] = (valB - 128).toByte()
            lowBits[off + 2] = (b - (valB shl nlbo)).toByte()
        }
    }
}