package org.jcodec.codecs.h264.io.write

import org.jcodec.codecs.h264.io.model.NALUnit
import org.jcodec.common.io.NIOUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class NALUnitWriter(private val to: WritableByteChannel) {

    companion object {
        private val _MARKER = ByteBuffer.allocate(4)

        init {
            _MARKER.putInt(1)
            _MARKER.flip()
        }
    }

    @Throws(IOException::class)
    fun writeUnit(nal: NALUnit, data: ByteBuffer) {
        val emprev = ByteBuffer.allocate(data.remaining() + 1024)
        NIOUtils.write(emprev, _MARKER)
        nal.write(emprev)
        emprev(emprev, data)
        emprev.flip()
        to.write(emprev)
    }

    private fun emprev(emprev: ByteBuffer, data: ByteBuffer) {
        val dd = data.duplicate()
        var prev1 = 1
        var prev2 = 1
        while (dd.hasRemaining()) {
            val b = dd.get()
            if (prev1 == 0 && prev2 == 0 && b.toInt() and 0x3 == b.toInt()) {
                prev2 = prev1
                prev1 = 3
                emprev.put(3.toByte())
            }
            prev2 = prev1
            prev1 = b.toInt()
            emprev.put(b)
        }
    }

}