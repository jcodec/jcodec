package org.jcodec.scale

import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class RgbToBgr : Transform {
    override fun transform(src: Picture, dst: Picture) {
        require(!(src.color != ColorSpace.RGB && src.color != ColorSpace.BGR
                || dst.color != ColorSpace.RGB && dst.color != ColorSpace.BGR)) { "Expected RGB or BGR inputs, was: " + src.color + ", " + dst.color }
        val dataSrc = src.getPlaneData(0)
        val dataDst = dst.getPlaneData(0)
        var i = 0
        while (i < dataSrc.size) {
            val tmp = dataSrc[i + 2]
            dataDst[i + 2] = dataSrc[i]
            dataDst[i] = tmp
            dataDst[i + 1] = dataSrc[i + 1]
            i += 3
        }
    }
}