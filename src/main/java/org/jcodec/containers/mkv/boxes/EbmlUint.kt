package org.jcodec.containers.mkv.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 *
 * EBML IO implementation
 *
 * @author The JCodec project
 */
class EbmlUint(id: ByteArray) : EbmlBin(id) {

    var uint: Long
        get() {
            var l: Long = 0
            var tmp: Long = 0
            val d = _data!!
            for (i in 0 until d.limit()) {
                tmp = d[d.limit() - 1 - i].toLong() shl 56
                tmp = tmp ushr 56 - i * 8
                l = l or tmp
            }
            return l
        }
        set(value) {
            _data = ByteBuffer.wrap(longToBytes(value))
            _dataLen = _data!!.limit()
        }

    companion object {
        @JvmStatic
        fun createEbmlUint(id: ByteArray, value: Long): EbmlUint {
            val e = EbmlUint(id)
            e.uint = value
            return e
        }

        @JvmStatic
        fun longToBytes(value: Long): ByteArray {
            val b = ByteArray(calculatePayloadSize(value))
            for (i in b.indices.reversed()) {
                b[i] = (value ushr 8 * (b.size - i - 1)).toByte()
            }
            return b
        }

        @JvmStatic
        fun calculatePayloadSize(value: Long): Int {
            if (value == 0L) return 1
            return if (value <= 0x7fffffffL) {
                4 - (Integer.numberOfLeadingZeros(value.toInt()) shr 3)
            } else 8 - (java.lang.Long.numberOfLeadingZeros(value) shr 3)
        }
    }
}