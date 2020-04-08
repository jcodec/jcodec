package org.jcodec.codecs.h264.encode

import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MBEncoderHelper {
    fun takeSubtract(planeData: ByteArray, planeWidth: Int, planeHeight: Int, x: Int, y: Int, coeff: IntArray,
                     pred: ByteArray, blkW: Int, blkH: Int) {
        if (x + blkW < planeWidth && y + blkH < planeHeight) takeSubtractSafe(planeData, planeWidth, planeHeight, x, y, coeff, pred, blkW, blkH) else takeSubtractUnsafe(planeData, planeWidth, planeHeight, x, y, coeff, pred, blkW, blkH)
    }

    fun takeSubtractSafe(planeData: ByteArray, planeWidth: Int, planeHeight: Int, x: Int, y: Int,
                         coeff: IntArray, pred: ByteArray, blkW: Int, blkH: Int) {
        var i = 0
        var srcOff = y * planeWidth + x
        var dstOff = 0
        while (i < blkH) {
            var j = 0
            var srcOff1 = srcOff
            while (j < blkW) {
                coeff[dstOff] = planeData[srcOff1] - pred[dstOff]
                coeff[dstOff + 1] = planeData[srcOff1 + 1] - pred[dstOff + 1]
                coeff[dstOff + 2] = planeData[srcOff1 + 2] - pred[dstOff + 2]
                coeff[dstOff + 3] = planeData[srcOff1 + 3] - pred[dstOff + 3]
                j += 4
                dstOff += 4
                srcOff1 += 4
            }
            i++
            srcOff += planeWidth
        }
    }

    @JvmStatic
    fun take(planeData: ByteArray, planeWidth: Int, planeHeight: Int, x: Int, y: Int, patch: ByteArray,
             blkW: Int, blkH: Int) {
        if (x + blkW < planeWidth && y + blkH < planeHeight) takeSafe(planeData, planeWidth, planeHeight, x, y, patch, blkW, blkH) else takeExtendBorder(planeData, planeWidth, planeHeight, x, y, patch, blkW, blkH)
    }

    fun takeSafe(planeData: ByteArray, planeWidth: Int, planeHeight: Int, x: Int, y: Int, patch: ByteArray,
                 blkW: Int, blkH: Int) {
        var i = 0
        var srcOff = y * planeWidth + x
        var dstOff = 0
        while (i < blkH) {
            var j = 0
            var srcOff1 = srcOff
            while (j < blkW) {
                patch[dstOff] = planeData[srcOff1]
                ++j
                ++dstOff
                ++srcOff1
            }
            i++
            srcOff += planeWidth
        }
    }

    fun takeExtendBorder(planeData: ByteArray, planeWidth: Int, planeHeight: Int, x: Int, y: Int, patch: ByteArray,
                         blkW: Int, blkH: Int) {
        var outOff = 0
        var i: Int
        i = y
        while (i < Math.min(y + blkH, planeHeight)) {
            var off = i * planeWidth + Math.min(x, planeWidth)
            var j: Int
            j = x
            while (j < Math.min(x + blkW, planeWidth)) {
                patch[outOff] = planeData[off]
                j++
                outOff++
                off++
            }
            --off
            while (j < x + blkW) {
                patch[outOff] = planeData[off]
                j++
                outOff++
            }
            i++
        }
        while (i < y + blkH) {
            var off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth)
            var j: Int
            j = x
            while (j < Math.min(x + blkW, planeWidth)) {
                patch[outOff] = planeData[off]
                j++
                outOff++
                off++
            }
            --off
            while (j < x + blkW) {
                patch[outOff] = planeData[off]
                j++
                outOff++
            }
            i++
        }
    }

    fun takeSafe2(planeData: ByteArray, planeWidth: Int, planeHeight: Int, x: Int, y: Int, coeff: IntArray,
                  blkW: Int, blkH: Int) {
        var i = 0
        var srcOff = y * planeWidth + x
        var dstOff = 0
        while (i < blkH) {
            var j = 0
            var srcOff1 = srcOff
            while (j < blkW) {
                coeff[dstOff] = planeData[srcOff1].toInt()
                ++j
                ++dstOff
                ++srcOff1
            }
            i++
            srcOff += planeWidth
        }
    }

    fun takeSubtractUnsafe(planeData: ByteArray, planeWidth: Int, planeHeight: Int, x: Int, y: Int,
                           coeff: IntArray, pred: ByteArray, blkW: Int, blkH: Int) {
        var outOff = 0
        var i: Int
        i = y
        while (i < Math.min(y + blkH, planeHeight)) {
            var off = i * planeWidth + Math.min(x, planeWidth)
            var j: Int
            j = x
            while (j < Math.min(x + blkW, planeWidth)) {
                coeff[outOff] = planeData[off] - pred[outOff]
                j++
                outOff++
                off++
            }
            --off
            while (j < x + blkW) {
                coeff[outOff] = planeData[off] - pred[outOff]
                j++
                outOff++
            }
            i++
        }
        while (i < y + blkH) {
            var off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth)
            var j: Int
            j = x
            while (j < Math.min(x + blkW, planeWidth)) {
                coeff[outOff] = planeData[off] - pred[outOff]
                j++
                outOff++
                off++
            }
            --off
            while (j < x + blkW) {
                coeff[outOff] = planeData[off] - pred[outOff]
                j++
                outOff++
            }
            i++
        }
    }

    fun putBlk(planeData: ByteArray, block: IntArray, pred: ByteArray, log2stride: Int, blkX: Int, blkY: Int,
               blkW: Int, blkH: Int) {
        val stride = 1 shl log2stride
        var line = 0
        var srcOff = 0
        var dstOff = (blkY shl log2stride) + blkX
        while (line < blkH) {
            var dstOff1 = dstOff
            var row = 0
            while (row < blkW) {
                planeData[dstOff1] = MathUtil.clip(block[srcOff] + pred[srcOff], -128, 127).toByte()
                planeData[dstOff1 + 1] = MathUtil.clip(block[srcOff + 1] + pred[srcOff + 1], -128, 127).toByte()
                planeData[dstOff1 + 2] = MathUtil.clip(block[srcOff + 2] + pred[srcOff + 2], -128, 127).toByte()
                planeData[dstOff1 + 3] = MathUtil.clip(block[srcOff + 3] + pred[srcOff + 3], -128, 127).toByte()
                srcOff += 4
                dstOff1 += 4
                row += 4
            }
            dstOff += stride
            line++
        }
    }

    @JvmStatic
    fun putBlkPic(dest: Picture, src: Picture, x: Int, y: Int) {
        if (dest.color != src.color) throw RuntimeException("Incompatible color")
        for (c in 0 until dest.color.nComp) {
            pubBlkOnePlane(dest.getPlaneData(c), dest.getPlaneWidth(c), src.getPlaneData(c), src.getPlaneWidth(c),
                    src.getPlaneHeight(c), x shr dest.color.compWidth!![c], y shr dest.color.compHeight!![c])
        }
    }

    private fun pubBlkOnePlane(dest: ByteArray, destWidth: Int, src: ByteArray, srcWidth: Int, srcHeight: Int, x: Int, y: Int) {
        var destOff = y * destWidth + x
        var srcOff = 0
        for (i in 0 until srcHeight) {
            var j = 0
            while (j < srcWidth) {
                dest[destOff] = src[srcOff]
                j++
                ++destOff
                ++srcOff
            }
            destOff += destWidth - srcWidth
        }
    }
}