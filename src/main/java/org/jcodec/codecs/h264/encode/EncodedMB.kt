package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class EncodedMB {
    @JvmField
    val pixels: Picture
    @JvmField
    var type: MBType? = null
    @JvmField
    var qp = 0
    @JvmField
    var nc: IntArray
    @JvmField
    val mx: IntArray
    @JvmField
    val my: IntArray
    @JvmField
    val mr: IntArray
    @JvmField
    var mbX = 0
    var mbY = 0

    fun setPos(mbX: Int, mbY: Int) {
        this.mbX = mbX
        this.mbY = mbY
    }

    init {
        pixels = Picture.create(16, 16, ColorSpace.YUV420J)
        nc = IntArray(16)
        mx = IntArray(16)
        my = IntArray(16)
        mr = IntArray(16)
    }
}