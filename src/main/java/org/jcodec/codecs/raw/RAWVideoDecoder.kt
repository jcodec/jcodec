package org.jcodec.codecs.raw

import org.jcodec.common.VideoCodecMeta
import org.jcodec.common.VideoDecoder
import org.jcodec.common.and
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Size
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class RAWVideoDecoder(private val width: Int, private val height: Int) : VideoDecoder() {
    override fun decodeFrame(data: ByteBuffer, buffer: Array<ByteArray>): Picture {
        val create = Picture.createPicture(width, height, buffer, ColorSpace.YUV420)
        val pix = data.duplicate()
        copy(pix, create.getPlaneData(0), width * height)
        copy(pix, create.getPlaneData(1), width * height / 4)
        copy(pix, create.getPlaneData(2), width * height / 4)
        return create
    }

    fun copy(b: ByteBuffer, ii: ByteArray, size: Int) {
        var i = 0
        while (b.hasRemaining() && i < size) {
            ii[i] = ((b.get() and 0xff) - 128).toByte()
            i++
        }
    }

    override fun getCodecMeta(data: ByteBuffer): VideoCodecMeta {
        return VideoCodecMeta.createSimpleVideoCodecMeta(Size(width, height), ColorSpace.YUV420)
    }

}