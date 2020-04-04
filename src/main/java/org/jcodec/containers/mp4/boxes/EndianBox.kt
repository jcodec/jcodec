package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class EndianBox(header: Header) : Box(header) {
    var endian: ByteOrder? = null
        private set

    override fun parse(buf: ByteBuffer) {
        val end = buf.short.toLong()
        if (end == 1L) {
            endian = ByteOrder.LITTLE_ENDIAN
        } else {
            endian = ByteOrder.BIG_ENDIAN
        }
    }

    override fun doWrite(out: ByteBuffer) {
        out.putShort((if (endian == ByteOrder.LITTLE_ENDIAN) 1 else 0).toShort())
    }

    override fun estimateSize(): Int {
        return 2 + 8
    }

    protected fun calcSize(): Int {
        return 2
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "enda"
        }

        fun createEndianBox(endian: ByteOrder?): EndianBox {
            val endianBox = EndianBox(Header(fourcc()))
            endianBox.endian = endian
            return endianBox
        }
    }
}