package org.jcodec.codecs.raw

import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * The encoder for yuv 10 bit 422
 *
 * x|x|9876543210(cr0)|9876543210(y0) |9876543210(cb0) x|x|9876543210(y2)
 * |9876543210(cb1)|9876543210(y1) x|x|9876543210(cb2)|9876543210(y3)
 * |9876543210(cr1) x|x|9876543210(y5) |9876543210(cr2)|9876543210(y4)
 *
 * @author The JCodec project
 */
class V210Encoder {
    @Throws(IOException::class)
    fun encodeFrame(_out: ByteBuffer, frame: Picture): ByteBuffer {
        val out = _out.duplicate()
        out.order(ByteOrder.LITTLE_ENDIAN)
        val tgtStride = (frame.getPlaneWidth(0) + 47) / 48 * 48
        val data = frame.data
        val tmpY = ByteArray(tgtStride)
        val tmpCb = ByteArray(tgtStride shr 1)
        val tmpCr = ByteArray(tgtStride shr 1)
        var yOff = 0
        var cbOff = 0
        var crOff = 0
        for (yy in 0 until frame.height) {
            System.arraycopy(data[0], yOff, tmpY, 0, frame.getPlaneWidth(0))
            System.arraycopy(data[1], cbOff, tmpCb, 0, frame.getPlaneWidth(1))
            System.arraycopy(data[2], crOff, tmpCr, 0, frame.getPlaneWidth(2))
            var yi = 0
            var cbi = 0
            var cri = 0
            while (yi < tgtStride) {
                var i = 0
                i = i or (clip(tmpCr[cri++]) shl 20)
                i = i or (clip(tmpY[yi++]) shl 10)
                i = i or clip(tmpCb[cbi++])
                out.putInt(i)
                i = 0
                i = i or clip(tmpY[yi++])
                i = i or (clip(tmpY[yi++]) shl 20)
                i = i or (clip(tmpCb[cbi++]) shl 10)
                out.putInt(i)
                i = 0
                i = i or (clip(tmpCb[cbi++]) shl 20)
                i = i or (clip(tmpY[yi++]) shl 10)
                i = i or clip(tmpCr[cri++])
                out.putInt(i)
                i = 0
                i = i or clip(tmpY[yi++])
                i = i or (clip(tmpY[yi++]) shl 20)
                i = i or (clip(tmpCr[cri++]) shl 10)
                out.putInt(i)
            }
            yOff += frame.getPlaneWidth(0)
            cbOff += frame.getPlaneWidth(1)
            crOff += frame.getPlaneWidth(2)
        }
        out.flip()
        return out
    }

    companion object {
        fun clip(b: Byte): Int {
            return MathUtil.clip(b + 128 shl 2, 8, 1019)
        }
    }
}