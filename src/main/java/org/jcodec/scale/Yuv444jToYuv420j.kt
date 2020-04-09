package org.jcodec.scale

import org.jcodec.common.model.Picture

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author Stanislav Vitvitskyy
 */
class Yuv444jToYuv420j : Transform {
    override fun transform(src: Picture, dst: Picture) {
        val size = src.width * src.height
        System.arraycopy(src.getPlaneData(0), 0, dst.getPlaneData(0), 0, size)
        for (plane in 1..2) {
            val srcPl = src.getPlaneData(plane)
            val dstPl = dst.getPlaneData(plane)
            val srcStride = src.getPlaneWidth(plane)
            var y = 0
            var srcOff = 0
            var dstOff = 0
            while (y < src.height) {
                var x = 0
                while (x < src.width) {
                    dstPl[dstOff] = ((srcPl[srcOff] + srcPl[srcOff + 1] + srcPl[srcOff + srcStride]
                            + srcPl[srcOff + srcStride + 1] + 2) shr 2).toByte()
                    x += 2
                    srcOff += 2
                    dstOff++
                }
                y += 2
                srcOff += srcStride
            }
        }
    }
}