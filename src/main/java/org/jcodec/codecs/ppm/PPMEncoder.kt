package org.jcodec.codecs.ppm

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import java.nio.ByteBuffer

/**
 *
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class PPMEncoder {
    fun encodeFrame(picture: Picture): ByteBuffer {
        require(picture.color == ColorSpace.RGB) { "Only RGB image can be stored in PPM" }
        val buffer = ByteBuffer.allocate(picture.width * picture.height * 3 + 200)
        buffer.put(JCodecUtil2.asciiString("P6 ${picture.width} ${picture.height} 255\n"))
        val data = picture.data
        var i = 0
        while (i < picture.width * picture.height * 3) {
            buffer.put((data[0][i + 2] + 128).toByte())
            buffer.put((data[0][i + 1] + 128).toByte())
            buffer.put((data[0][i] + 128).toByte())
            i += 3
        }
        buffer.flip()
        return buffer
    }
}