package org.jcodec.codecs.raw

import org.jcodec.common.VideoEncoder
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Encodes image data into raw bytes according to the color space.
 *
 * @author The JCodec project
 */
class RAWVideoEncoder : VideoEncoder() {
    override fun encodeFrame(pic: Picture, _out: ByteBuffer): EncodedFrame? {
        val dup = _out.duplicate()
        val color = pic.color
        if (color.planar) {
            for (plane in 0 until color.nComp) {
                val width = pic.width shr color.compWidth!![plane]
                val startX = pic.startX
                val startY = pic.startY
                val cropW = pic.croppedWidth shr color.compWidth!![plane]
                val cropH = pic.croppedHeight shr color.compHeight!![plane]
                val planeData = pic.getPlaneData(plane)
                var pos = width * startY + startX
                for (y in 0 until cropH) {
                    for (x in 0 until cropW) {
                        dup.put((planeData[pos + x] + 128).toByte())
                    }
                    pos += width
                }
            }
        } else {
            val bytesPerPixel = color.bitsPerPixel + 7 shr 3
            val stride = pic.width * bytesPerPixel
            val startX = pic.startX
            val startY = pic.startY
            val cropW = pic.croppedWidth
            val cropH = pic.croppedHeight
            val planeData = pic.getPlaneData(0)
            var pos = stride * startY + startX * bytesPerPixel
            for (y in 0 until cropH) {
                var x = 0
                var off = 0
                while (x < cropW) {
                    for (b in 0 until bytesPerPixel) dup.put((planeData[pos + off + b] + 128).toByte())
                    x++
                    off += bytesPerPixel
                }
                pos += stride
            }
        }
        dup.flip()
        return EncodedFrame(dup, true)
    }

    override fun getSupportedColorSpaces(): Array<ColorSpace> {
        return emptyArray()
    }

    override fun estimateBufferSize(frame: Picture): Int {
        val fullPlaneSize = frame.width * frame.croppedHeight
        val color = frame.color
        var totalSize = 0
        for (i in 0 until color.nComp) {
            totalSize += fullPlaneSize shr color.compWidth!![i] shr color.compHeight!![i]
        }
        return totalSize
    }

    override fun finish() {
        // TODO Auto-generated method stub
    }
}