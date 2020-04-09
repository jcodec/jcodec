package org.jcodec.codecs.mpeg4

import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MPEG4Interpolator {
    private val qpi = ByteArray(16 * 17)
    fun fulpel8x8(dst: ByteArray, dstOff: Int, dstStride: Int, src: ByteArray, srcCol: Int, srcRow: Int,
                  srcWidth: Int, srcHeight: Int, srcStride: Int) {
        var dstOff = dstOff
        if (srcCol < 0 || srcRow < 0 || srcCol > srcWidth - 8 || srcRow > srcHeight - 8) {
            // Unsafe
            var j = 0
            while (j < 8) {
                for (i in 0..7) {
                    val y = MathUtil.clip(srcRow + j, 0, srcHeight - 1)
                    val x = MathUtil.clip(srcCol + i, 0, srcWidth - 1)
                    dst[dstOff + i] = src[srcStride * y + x]
                }
                j++
                dstOff += dstStride
            }
        } else {
            var srcOffset = srcRow * srcStride + srcCol
            var j = 0
            while (j < 8) {
                for (i in 0..7) {
                    dst[dstOff + i] = src[srcOffset + i]
                }
                j++
                dstOff += dstStride
                srcOffset += srcStride
            }
        }
    }

    fun fulpel16x16(dst: ByteArray, src: ByteArray, srcCol: Int, srcRow: Int, srcWidth: Int, srcHeight: Int,
                    srcStride: Int) {
        if (srcCol < 0 || srcRow < 0 || srcCol > srcWidth - 16 || srcRow > srcHeight - 16) {
            // Unsafe
            for (j in 0..15) {
                for (i in 0..15) {
                    val y = MathUtil.clip(srcRow + j, 0, srcHeight - 1)
                    val x = MathUtil.clip(srcCol + i, 0, srcWidth - 1)
                    dst[(j shl 4) + i] = src[srcStride * y + x]
                }
            }
        } else {
            val srcOffset = srcRow * srcStride + srcCol
            for (j in 0..15) {
                for (i in 0..15) {
                    dst[(j shl 4) + i] = src[srcOffset + (j * srcStride + i)]
                }
            }
        }
    }

    @JvmStatic
    fun interpolate16x16QP(dst: ByteArray, ref: ByteArray, x: Int, y: Int, w: Int, h: Int,
                           dx: Int, dy: Int, refs: Int, rounding: Boolean) {
        val xRef = x * 4 + dx
        val yRef = y * 4 + dy
        val location = dx and 3 or (dy and 3 shl 2)
        var xFull = xRef / 4
        if (xRef < 0 && xRef and 3 != 0) {
            xFull--
        }
        var yFull = yRef / 4
        if (yRef < 0 && yRef and 3 != 0) {
            yFull--
        }
        when (location) {
            0 -> fulpel16x16(dst, ref, xFull, yFull, w, h, refs)
            1 -> {
                horzMiddle16(dst, ref, xFull, yFull, w, h, 16, refs, if (rounding) 1 else 0)
                qOff(dst, ref, xFull, yFull, w, h, 16, refs, if (rounding) 1 else 0)
            }
            2 -> horzMiddle16(dst, ref, xFull, yFull, w, h, 16, refs, if (rounding) 1 else 0)
            3 -> {
                horzMiddle16(dst, ref, xFull, yFull, w, h, 16, refs, if (rounding) 1 else 0)
                qOff(dst, ref, xFull + 1, yFull, w, h, 16, refs, if (rounding) 1 else 0)
            }
            4 -> {
                vertMiddle16(dst, ref, xFull, yFull, w, h, 16, refs, if (rounding) 1 else 0)
                qOff(dst, ref, xFull, yFull, w, h, 16, refs, if (rounding) 1 else 0)
            }
            5 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                qOff(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
                qOffSafe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
            }
            6 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
                qOffSafe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
            }
            7 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                qOff(qpi, ref, xFull + 1, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
                qOffSafe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
            }
            8 -> vertMiddle16(dst, ref, xFull, yFull, w, h, 16, refs, if (rounding) 1 else 0)
            9 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                qOff(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
            }
            10 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
            }
            11 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                qOff(qpi, ref, xFull + 1, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
            }
            12 -> {
                vertMiddle16(dst, ref, xFull, yFull, w, h, 16, refs, if (rounding) 1 else 0)
                qOff(dst, ref, xFull, yFull + 1, w, h, 16, refs, if (rounding) 1 else 0)
            }
            13 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                qOff(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
                qOffSafe(dst, qpi, 16, 16, 16, if (rounding) 1 else 0)
            }
            14 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
                qOffSafe(dst, qpi, 16, 16, 16, if (rounding) 1 else 0)
            }
            15 -> {
                horzMiddle16(qpi, ref, xFull, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                qOff(qpi, ref, xFull + 1, yFull, w, h, 17, refs, if (rounding) 1 else 0)
                vertMiddle16Safe(dst, qpi, 0, 16, 16, if (rounding) 1 else 0)
                qOffSafe(dst, qpi, 16, 16, 16, if (rounding) 1 else 0)
            }
        }
    }

    private fun qOffSafe(dst: ByteArray, src: ByteArray, srcOffset: Int, height: Int, srcStride: Int, round: Int) {
        var srcOffset = srcOffset
        var row = 0
        var dstOff = 0
        while (row < height) {
            var col = 0
            while (col < 16) {
                dst[dstOff] = (dst[dstOff] + src[srcOffset + col] + 1 shr 1).toByte()
                col++
                dstOff++
            }
            row++
            srcOffset += srcStride
        }
    }

    private fun qOff(dst: ByteArray, src: ByteArray, x: Int, y: Int, w: Int, h: Int, height: Int, srcStride: Int, round: Int) {
        if (x < 0 || y < 0 || x > w - 16 || y > h - height) {
            // Unsafe
            var row = 0
            var dstOff = 0
            while (row < height) {
                val o0 = MathUtil.clip(y + row, 0, h - 1) * srcStride
                var col = 0
                while (col < 16) {
                    val srcOffset = o0 + MathUtil.clip(x + col, 0, w - 1)
                    dst[dstOff] = (dst[dstOff] + src[srcOffset] + 1 shr 1).toByte()
                    col++
                    dstOff++
                }
                row++
            }
        } else {
            qOffSafe(dst, src, y * srcStride + x, height, srcStride, round)
        }
    }

    private fun qOff8x8Safe(dst: ByteArray, dstOff: Int, src: ByteArray, srcOffset: Int, height: Int, srcStride: Int, round: Int) {
        var dstOff = dstOff
        var srcOffset = srcOffset
        var row = 0
        while (row < height) {
            var col = 0
            while (col < 8) {
                dst[dstOff] = (dst[dstOff] + src[srcOffset + col] + 1 shr 1).toByte()
                col++
                dstOff++
            }
            row++
            srcOffset += srcStride
            dstOff += 8
        }
    }

    private fun qOff8x8(dst: ByteArray, dstOff: Int, src: ByteArray, x: Int, y: Int, w: Int, h: Int, height: Int,
                        srcStride: Int, round: Int) {
        var dstOff = dstOff
        if (x < 0 || y < 0 || x > w - 8 || y > h - height) {
            // Unsafe
            var row = 0
            while (row < height) {
                val o0 = MathUtil.clip(y + row, 0, h - 1) * srcStride
                var col = 0
                while (col < 8) {
                    val srcOffset = o0 + MathUtil.clip(x + col, 0, w - 1)
                    dst[dstOff] = (dst[dstOff] + src[srcOffset] + 1 shr 1).toByte()
                    col++
                    dstOff++
                }
                row++
                dstOff += 8
            }
        } else {
            qOff8x8Safe(dst, dstOff, src, y * srcStride + x, height, srcStride, round)
        }
    }

    @JvmStatic
    fun interpolate8x8QP(dst: ByteArray, dstO: Int, ref: ByteArray,
                         x: Int, y: Int, w: Int, h: Int, dx: Int, dy: Int, refs: Int,
                         rounding: Boolean) {
        val xRef = x * 4 + dx
        val yRef = y * 4 + dy
        val quads = dx and 3 or (dy and 3 shl 2)
        var xInt = xRef / 4
        if (xRef < 0 && xRef % 4 != 0) {
            xInt--
        }
        var yInt = yRef / 4
        if (yRef < 0 && yRef % 4 != 0) {
            yInt--
        }
        when (quads) {
            0 -> fulpel8x8(dst, dstO, 16, ref, xInt, yInt, w, h, refs)
            1 -> {
                horzMiddle8(dst, dstO, ref, xInt, yInt, w, h, 8, refs, if (rounding) 1 else 0)
                qOff8x8(dst, dstO, ref, xInt, yInt, w, h, 8, refs, if (rounding) 1 else 0)
            }
            2 -> horzMiddle8(dst, dstO, ref, xInt, yInt, w, h, 8, refs, if (rounding) 1 else 0)
            3 -> {
                horzMiddle8(dst, dstO, ref, xInt, yInt, w, h, 8, refs, if (rounding) 1 else 0)
                qOff8x8(dst, dstO, ref, xInt + 1, yInt, w, h, 8, refs, if (rounding) 1 else 0)
            }
            4 -> {
                vertMiddle8(dst, dstO, ref, xInt, yInt, w, h, 8, refs, if (rounding) 1 else 0)
                qOff8x8(dst, dstO, ref, xInt, yInt, w, h, 8, refs, if (rounding) 1 else 0)
            }
            5 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                qOff8x8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
                qOff8x8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
            }
            6 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
                qOff8x8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
            }
            7 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                qOff8x8(qpi, 0, ref, xInt + 1, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
                qOff8x8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
            }
            8 -> vertMiddle8(dst, dstO, ref, xInt, yInt, w, h, 8, refs, if (rounding) 1 else 0)
            9 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                qOff8x8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
            }
            10 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
            }
            11 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                qOff8x8(qpi, 0, ref, xInt + 1, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
            }
            12 -> {
                vertMiddle8(dst, dstO, ref, xInt, yInt, w, h, 8, refs, if (rounding) 1 else 0)
                qOff8x8(dst, dstO, ref, xInt, yInt + 1, w, h, 8, refs, if (rounding) 1 else 0)
            }
            13 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                qOff8x8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
                qOff8x8Safe(dst, dstO, qpi, 16, 8, 16, if (rounding) 1 else 0)
            }
            14 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
                qOff8x8Safe(dst, dstO, qpi, 16, 8, 16, if (rounding) 1 else 0)
            }
            15 -> {
                horzMiddle8(qpi, 0, ref, xInt, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                qOff8x8(qpi, 0, ref, xInt + 1, yInt, w, h, 9, refs, if (rounding) 1 else 0)
                vertMiddle8Safe(dst, dstO, qpi, 0, 8, 16, if (rounding) 1 else 0)
                qOff8x8Safe(dst, dstO, qpi, 16, 8, 16, if (rounding) 1 else 0)
            }
        }
    }

    private fun horzMiddle8(dst: ByteArray, dstOffset: Int, src: ByteArray, x: Int, y: Int, w: Int, h: Int, height: Int, srcStride: Int, rounding: Int) {
        var dstOffset = dstOffset
        if (x < 0 || y < 0 || x > w - 9 || y > h - height) {
            // unsafe
            for (row in 0 until height) {
                for (i in 0..3) {
                    var sum0 = 0
                    var sum1 = 0
                    val o0 = MathUtil.clip(y + row, 0, h - 1) * srcStride
                    for (k in 0 until 5 + i) {
                        val o1 = MathUtil.clip(x + k, 0, w - 1)
                        val o2 = MathUtil.clip(x + 8 - k, 0, w - 1)
                        sum0 += MPEG4Consts.FILTER_TAB[i][k] * src[o0 + o1]
                        sum1 += MPEG4Consts.FILTER_TAB[i][k] * src[o0 + o2]
                    }
                    dst[dstOffset + i] = MathUtil.clip(sum0 + 16 - rounding shr 5, -128, 127).toByte()
                    dst[dstOffset + 7 - i] = MathUtil.clip(sum1 + 16 - rounding shr 5, -128, 127).toByte()
                }
                dstOffset += 16
            }
        } else {
            // safe
            var srcOffset = y * srcStride + x
            for (row in 0 until height) {
                for (i in 0..3) {
                    var sum0 = 0
                    var sum1 = 0
                    for (k in 0 until 5 + i) {
                        sum0 += MPEG4Consts.FILTER_TAB[i][k] * src[srcOffset + k]
                        sum1 += MPEG4Consts.FILTER_TAB[i][k] * src[srcOffset + 8 - k]
                    }
                    dst[dstOffset + i] = MathUtil.clip(sum0 + 16 - rounding shr 5, -128, 127).toByte()
                    dst[dstOffset + 7 - i] = MathUtil.clip(sum1 + 16 - rounding shr 5, -128, 127).toByte()
                }
                srcOffset += srcStride
                dstOffset += 16
            }
        }
    }

    private fun horzMiddle16(dst: ByteArray, src: ByteArray, x: Int, y: Int, w: Int, h: Int, height: Int, srcStride: Int, rounding: Int) {
        if (x < 0 || y < 0 || x > w - 17 || y > h - height) {
            // unsafe
            var dstOffset = 0
            for (row in 0 until height) {
                val o0 = MathUtil.clip(y + row, 0, h - 1) * srcStride
                for (i in 0..3) {
                    var sum0 = 0
                    var sum1 = 0
                    for (k in 0 until 5 + i) {
                        val srcOffset0 = o0 + MathUtil.clip(x + k, 0, w - 1)
                        val srcOffset1 = o0 + MathUtil.clip(x + 16 - k, 0, w - 1)
                        sum0 += MPEG4Consts.FILTER_TAB[i][k] * src[srcOffset0]
                        sum1 += MPEG4Consts.FILTER_TAB[i][k] * src[srcOffset1]
                    }
                    dst[dstOffset + i] = MathUtil.clip(sum0 + 16 - rounding shr 5, -128, 127).toByte()
                    dst[dstOffset + 15 - i] = MathUtil.clip(sum1 + 16 - rounding shr 5, -128, 127).toByte()
                }
                for (i in 0..7) {
                    var sum = 0
                    for (k in 0..7) {
                        val srcOffset = o0 + MathUtil.clip(x + k + i + 1, 0, w - 1)
                        sum += MPEG4Consts.FILTER_TAB[3][k] * src[srcOffset]
                    }
                    dst[dstOffset + i + 4] = MathUtil.clip(sum + 16 - rounding shr 5, -128, 127).toByte()
                }
                dstOffset += 16
            }
        } else {
            // safe
            var srcOffset = y * srcStride + x
            var dstOffset = 0
            for (row in 0 until height) {
                for (i in 0..3) {
                    var sum0 = 0
                    var sum1 = 0
                    for (k in 0 until 5 + i) {
                        sum0 += MPEG4Consts.FILTER_TAB[i][k] * src[srcOffset + k]
                        sum1 += MPEG4Consts.FILTER_TAB[i][k] * src[srcOffset + 16 - k]
                    }
                    dst[dstOffset + i] = MathUtil.clip(sum0 + 16 - rounding shr 5, -128, 127).toByte()
                    dst[dstOffset + 15 - i] = MathUtil.clip(sum1 + 16 - rounding shr 5, -128, 127).toByte()
                }
                for (i in 0..7) {
                    var sum = 0
                    for (k in 0..7) {
                        sum += MPEG4Consts.FILTER_TAB[3][k] * src[srcOffset + k + i + 1]
                    }
                    dst[dstOffset + i + 4] = MathUtil.clip(sum + 16 - rounding shr 5, -128, 127).toByte()
                }
                srcOffset += srcStride
                dstOffset += 16
            }
        }
    }

    private fun vertMiddle16Safe(dst: ByteArray, src: ByteArray, srcOffset: Int, width: Int, srcStride: Int, rounding: Int) {
        var srcOffset = srcOffset
        var dstOffset = 0
        for (col in 0 until width) {
            var dstStart = dstOffset
            var dstEnd = dstOffset + 240
            run {
                var i = 0
                while (i < 4) {
                    var sum0 = 0
                    var sum1 = 0
                    var ss = srcOffset
                    var es = srcOffset + (srcStride shl 4)
                    for (k in 0 until 5 + i) {
                        sum0 += MPEG4Consts.FILTER_TAB[i][k] * src[ss]
                        sum1 += MPEG4Consts.FILTER_TAB[i][k] * src[es]
                        ss += srcStride
                        es -= srcStride
                    }
                    dst[dstStart] = MathUtil.clip(sum0 + 16 - rounding shr 5, -128, 127).toByte()
                    dst[dstEnd] = MathUtil.clip(sum1 + 16 - rounding shr 5, -128, 127).toByte()
                    ++i
                    dstStart += 16
                    dstEnd -= 16
                }
            }
            dstStart = dstOffset + 64
            var srcCoeff0Pos = srcOffset + srcStride
            var i = 0
            while (i < 8) {
                var sum = 0
                var srcPos = srcCoeff0Pos
                var k = 0
                while (k < 8) {
                    sum += MPEG4Consts.FILTER_TAB[3][k] * src[srcPos]
                    ++k
                    srcPos += srcStride
                }
                dst[dstStart] = MathUtil.clip(sum + 16 - rounding shr 5, -128, 127).toByte()
                ++i
                dstStart += 16
                srcCoeff0Pos += srcStride
            }
            srcOffset++
            dstOffset++
        }
    }

    private fun vertMiddle16(dst: ByteArray, src: ByteArray, x: Int, y: Int, w: Int, h: Int, width: Int, srcStride: Int,
                             rounding: Int) {
        if (x < 0 || y < 0 || x > w - width || y > h - 17) {
            // unsafe
            var dstOffset = 0
            for (col in 0 until width) {
                var dstStart = dstOffset
                var dstEnd = dstOffset + 240
                run {
                    var i = 0
                    while (i < 4) {
                        var sum0 = 0
                        var sum1 = 0
                        for (k in 0 until 5 + i) {
                            val ss = MathUtil.clip(y + k, 0, h - 1) * srcStride + MathUtil.clip(x + col, 0, w - 1)
                            val es = MathUtil.clip(y - k + 16, 0, h - 1) * srcStride + MathUtil.clip(x + col, 0, w - 1)
                            sum0 += MPEG4Consts.FILTER_TAB[i][k] * src[ss]
                            sum1 += MPEG4Consts.FILTER_TAB[i][k] * src[es]
                        }
                        dst[dstStart] = MathUtil.clip(sum0 + 16 - rounding shr 5, -128, 127).toByte()
                        dst[dstEnd] = MathUtil.clip(sum1 + 16 - rounding shr 5, -128, 127).toByte()
                        ++i
                        dstStart += 16
                        dstEnd -= 16
                    }
                }
                dstStart = dstOffset + 64
                var i = 0
                while (i < 8) {
                    var sum = 0
                    for (k in 0..7) {
                        val srcPos = (MathUtil.clip(y + i + k + 1, 0, h - 1) * srcStride
                                + MathUtil.clip(x + col, 0, w - 1))
                        sum += MPEG4Consts.FILTER_TAB[3][k] * src[srcPos]
                    }
                    dst[dstStart] = MathUtil.clip(sum + 16 - rounding shr 5, -128, 127).toByte()
                    ++i
                    dstStart += 16
                }
                dstOffset++
            }
        } else {
            vertMiddle16Safe(dst, src, y * srcStride + x, width, srcStride, rounding)
        }
    }

    private fun vertMiddle8Safe(dst: ByteArray, dstOffset: Int, src: ByteArray, srcOffset: Int, width: Int,
                                srcStride: Int, rounding: Int) {
        var dstOffset = dstOffset
        var srcOffset = srcOffset
        for (col in 0 until width) {
            for (i in 0..3) {
                var sum0 = 0
                var sum1 = 0
                var os = srcOffset
                var of = srcOffset + (srcStride shl 3)
                for (k in 0 until 5 + i) {
                    sum0 += MPEG4Consts.FILTER_TAB[i][k] * src[os]
                    sum1 += MPEG4Consts.FILTER_TAB[i][k] * src[of]
                    os += srcStride
                    of -= srcStride
                }
                dst[dstOffset + i * 16] = MathUtil.clip(sum0 + 16 - rounding shr 5, -128, 127).toByte()
                dst[dstOffset + (7 - i) * 16] = MathUtil.clip(sum1 + 16 - rounding shr 5, -128, 127).toByte()
            }
            srcOffset++
            dstOffset++
        }
    }

    private fun vertMiddle8(dst: ByteArray, dstOffset: Int, src: ByteArray, x: Int, y: Int, w: Int, h: Int, width: Int,
                            srcStride: Int, rounding: Int) {
        var dstOffset = dstOffset
        if (x < 0 || y < 0 || x > w - width || y > h - 9) {
            // unsafe
            for (col in 0 until width) {
                for (i in 0..3) {
                    var sum0 = 0
                    var sum1 = 0
                    for (k in 0 until 5 + i) {
                        val os = MathUtil.clip(y + k, 0, h - 1) * srcStride + MathUtil.clip(x + col, 0, w - 1)
                        val of = MathUtil.clip(y + 8 - k, 0, h - 1) * srcStride + MathUtil.clip(x + col, 0, w - 1)
                        sum0 += MPEG4Consts.FILTER_TAB[i][k] * src[os]
                        sum1 += MPEG4Consts.FILTER_TAB[i][k] * src[of]
                    }
                    dst[dstOffset + i * 16] = MathUtil.clip(sum0 + 16 - rounding shr 5, -128, 127).toByte()
                    dst[dstOffset + (7 - i) * 16] = MathUtil.clip(sum1 + 16 - rounding shr 5, -128, 127).toByte()
                }
                dstOffset++
            }
        } else {
            vertMiddle8Safe(dst, dstOffset, src, y * srcStride + x, width,
                    srcStride, rounding)
        }
    }

    @JvmStatic
    fun interpolate16x16Planar(dst: ByteArray, refn: ByteArray, x: Int,
                               y: Int, w: Int, h: Int, dx: Int, dy: Int, stride: Int, rounding: Boolean) {
        interpolate8x8Planar(dst, 0, 16, refn, x, y, w, h, dx, dy, stride, rounding)
        interpolate8x8Planar(dst, 8, 16, refn, x + 8, y, w, h, dx, dy, stride, rounding)
        interpolate8x8Planar(dst, 128, 16, refn, x, y + 8, w, h, dx, dy, stride, rounding)
        interpolate8x8Planar(dst, 136, 16, refn, x + 8, y + 8, w, h, dx, dy, stride, rounding)
    }

    @JvmStatic
    fun interpolate8x8Planar(dst: ByteArray, dstOff: Int, dstStride: Int, refn: ByteArray, x: Int, y: Int, w: Int, h: Int,
                             dx: Int, dy: Int, stride: Int, rounding: Boolean) {
        val x_ = x + (dx shr 1)
        val y_ = y + (dy shr 1)
        when ((dx and 1 shl 1) + (dy and 1)) {
            0 -> fulpel8x8(dst, dstOff, dstStride, refn, x_, y_, w, h, stride)
            1 -> interpolate8PlanarVer(dst, dstOff, dstStride, refn, x_, y_, w, h, stride, rounding)
            2 -> interpolate8x8PlanarHor(dst, dstOff, dstStride, refn, x_, y_, w, h, stride, rounding)
            else -> interpolate8x8PlanarBoth(dst, dstOff, dstStride, refn, x_, y_, w, h, stride, rounding)
        }
    }

    private fun interpolate8x8PlanarHor(dst: ByteArray, dstOffset: Int, dstStride: Int, src: ByteArray,
                                        x: Int, y: Int, w: Int, h: Int, stride: Int, rounding: Boolean) {
        val rnd = if (rounding) 0 else 1
        if (x < 0 || y < 0 || x > w - 9 || y > h - 8) {
            // unsafe
            var j = 0
            var d = dstOffset
            while (j < 8) {
                for (i in 0..7) {
                    val srcOffset0 = MathUtil.clip(y + j, 0, h - 1) * stride + MathUtil.clip(x + i, 0, w - 1)
                    val srcOffset1 = MathUtil.clip(y + j, 0, h - 1) * stride + MathUtil.clip(x + i + 1, 0, w - 1)
                    dst[d + i] = (src[srcOffset0] + src[srcOffset1] + rnd shr 1).toByte()
                }
                j++
                d += dstStride
            }
        } else {
            // safe
            val srcOffset = y * stride + x
            var j = 0
            var d = dstOffset
            while (j < 8 * stride) {
                for (i in 0..7) dst[d + i] = (src[srcOffset + j + i] + src[srcOffset + j + i + 1] + rnd shr 1).toByte()
                j += stride
                d += dstStride
            }
        }
    }

    private fun interpolate8PlanarVer(dst: ByteArray, dstOff: Int, dstStride: Int, src: ByteArray, x: Int, y: Int, w: Int, h: Int, stride: Int,
                                      rounding: Boolean) {
        val rnd = if (rounding) 0 else 1
        if (x < 0 || y < 0 || x > w - 8 || y > h - 9) {
            // unsafe
            var j = 0
            var d = dstOff
            while (j < 8) {
                for (i in 0..7) {
                    val srcOffset0 = MathUtil.clip(y + j, 0, h - 1) * stride + MathUtil.clip(x + i, 0, w - 1)
                    val srcOffset1 = MathUtil.clip(y + j + 1, 0, h - 1) * stride + MathUtil.clip(x + i, 0, w - 1)
                    dst[d + i] = (src[srcOffset0] + src[srcOffset1] + rnd shr 1).toByte()
                }
                j++
                d += dstStride
            }
        } else {
            // safe
            val srcOffset = y * stride + x
            var j = 0
            var d = dstOff
            while (j < 8 * stride) {
                for (i in 0..7) dst[d + i] = (src[srcOffset + j + i] + src[srcOffset + j + stride + i] + rnd shr 1).toByte()
                j += stride
                d += dstStride
            }
        }
    }

    private fun interpolate8x8PlanarBoth(dst: ByteArray, dstOff: Int, dstStride: Int, src: ByteArray, x: Int, y: Int, w: Int, h: Int, stride: Int,
                                         rounding: Boolean) {
        val rnd = if (rounding) 1 else 2
        if (x < 0 || y < 0 || x > w - 9 || y > h - 9) {
            // unsafe
            var j = 0
            var d = dstOff
            while (j < 8) {
                for (i in 0..7) {
                    val srcOffset0 = MathUtil.clip(y + j, 0, h - 1) * stride + MathUtil.clip(x + i, 0, w - 1)
                    val srcOffset1 = MathUtil.clip(y + j, 0, h - 1) * stride + MathUtil.clip(x + i + 1, 0, w - 1)
                    val srcOffset2 = MathUtil.clip(y + j + 1, 0, h - 1) * stride + MathUtil.clip(x + i, 0, w - 1)
                    val srcOffset3 = MathUtil.clip(y + j + 1, 0, h - 1) * stride + MathUtil.clip(x + i + 1, 0, w - 1)
                    dst[d + i] = ((src[srcOffset0] + src[srcOffset1] + src[srcOffset2] + src[srcOffset3]
                            + rnd) shr 2).toByte()
                }
                j++
                d += dstStride
            }
        } else {
            // safe
            val srcOffset = y * stride + x
            var j = 0
            var d = dstOff
            while (j < 8 * stride) {
                for (i in 0..7) dst[d + i] = ((src[srcOffset + j + i] + src[srcOffset + j + i + 1]
                        + src[srcOffset + j + stride + i] + src[srcOffset + j + stride + i + 1] + rnd) shr 2).toByte()
                j += stride
                d += dstStride
            }
        }
    }
}