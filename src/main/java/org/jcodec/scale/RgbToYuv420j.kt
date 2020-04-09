package org.jcodec.scale

import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Transforms Picture in RGB colorspace ( one plane, 3 integers per pixel ) to
 * Yuv420 colorspace output picture ( 3 planes, luma - 0th plane, cb - 1th
 * plane, cr - 2nd plane; cb and cr planes are half width and half haight )
 *
 * TODO: implement jpeg colorspace instead of NTSC
 *
 * @author The JCodec project
 */
class RgbToYuv420j : Transform {
    override fun transform(img: Picture, dst: Picture) {
        val y = img.data[0]
        val dstData = dst.data
        val out = Array(4) { IntArray(3) }
        var offChr = 0
        var offLuma = 0
        var offSrc = 0
        val strideSrc = img.width * 3
        val strideDst = dst.width
        for (i in 0 until (img.height shr 1)) {
            for (j in 0 until (img.width shr 1)) {
                dstData[1][offChr] = 0
                dstData[2][offChr] = 0
                rgb2yuv(y[offSrc], y[offSrc + 1], y[offSrc + 2], out[0])
                dstData[0][offLuma] = out[0][0].toByte()
                rgb2yuv(y[offSrc + strideSrc], y[offSrc + strideSrc + 1], y[offSrc + strideSrc + 2], out[1])
                dstData[0][offLuma + strideDst] = out[1][0].toByte()
                ++offLuma
                rgb2yuv(y[offSrc + 3], y[offSrc + 4], y[offSrc + 5], out[2])
                dstData[0][offLuma] = out[2][0].toByte()
                rgb2yuv(y[offSrc + strideSrc + 3], y[offSrc + strideSrc + 4], y[offSrc + strideSrc + 5], out[3])
                dstData[0][offLuma + strideDst] = out[3][0].toByte()
                ++offLuma
                dstData[1][offChr] = (out[0][1] + out[1][1] + out[2][1] + out[3][1] + 2 shr 2).toByte()
                dstData[2][offChr] = (out[0][2] + out[1][2] + out[2][2] + out[3][2] + 2 shr 2).toByte()
                ++offChr
                offSrc += 6
            }
            offLuma += strideDst
            offSrc += strideSrc
        }
    }

    companion object {
        fun rgb2yuv(r: Byte, g: Byte, b: Byte, out: IntArray) {
            val rS = r + 128
            val gS = g + 128
            val bS = b + 128
            var y = 77 * rS + 150 * gS + 15 * bS
            var u = -43 * rS - 85 * gS + 128 * bS
            var v = 128 * rS - 107 * gS - 21 * bS
            y = y + 128 shr 8
            u = u + 128 shr 8
            v = v + 128 shr 8
            out[0] = MathUtil.clip(y - 128, -128, 127)
            out[1] = MathUtil.clip(u, -128, 127)
            out[2] = MathUtil.clip(v, -128, 127)
        }
    }
}