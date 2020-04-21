package org.jcodec.codecs.raw

import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * The decoder for yuv 10 bit 422
 *
 * x|x|9876543210(cr0)|9876543210(y0) |9876543210(cb0)
 * x|x|9876543210(y2) |9876543210(cb1)|9876543210(y1)
 * x|x|9876543210(cb2)|9876543210(y3) |9876543210(cr1)
 * x|x|9876543210(y5) |9876543210(cr2)|9876543210(y4)
 *
 * @author The JCodec project
 */
class V210Decoder(private val width: Int, private val height: Int) {
    fun decode(data: ByteArray?): Picture {
        val littleEndian = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN) as ByteBuffer
        val dat = littleEndian.asIntBuffer()
        val y = ByteBuffer.wrap(ByteArray(width * height))
        val cb = ByteBuffer.wrap(ByteArray(width * height / 2))
        val cr = ByteBuffer.wrap(ByteArray(width * height / 2))
        while (dat.hasRemaining()) {
            var i = dat.get()
            cr.put(to8Bit(i shr 20))
            y.put(to8Bit(i shr 10 and 0x3ff))
            cb.put(to8Bit(i and 0x3ff))
            i = dat.get()
            y.put(to8Bit(i and 0x3ff))
            y.put(to8Bit(i shr 20))
            cb.put(to8Bit(i shr 10 and 0x3ff))
            i = dat.get()
            cb.put(to8Bit(i shr 20))
            y.put(to8Bit(i shr 10 and 0x3ff))
            cr.put(to8Bit(i and 0x3ff))
            i = dat.get()
            y.put(to8Bit(i and 0x3ff))
            y.put(to8Bit(i shr 20))
            cr.put(to8Bit(i shr 10 and 0x3ff))
        }
        return Picture.createPicture(width, height, arrayOf(y.array(), cb.array(), cr.array()), ColorSpace.YUV422)
    }

    private fun to8Bit(i: Int): Byte {
        return ((i + 2 shr 2) - 128).toByte()
    }

}