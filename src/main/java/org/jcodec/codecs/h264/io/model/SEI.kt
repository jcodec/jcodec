package org.jcodec.codecs.h264.io.model

import org.jcodec.codecs.h264.io.write.CAVLCWriter
import org.jcodec.common.io.BitWriter
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Supplementary Enhanced Information entity of H264 bitstream
 *
 * capable to serialize and deserialize with CAVLC bitstream
 *
 * @author The JCodec project
 */
class SEI(var messages: Array<SEIMessage?>?) {
    class SEIMessage(var payloadType: Int, var payloadSize: Int, var payload: ByteArray)

    fun write(out: ByteBuffer) {
        val writer = BitWriter(out)
        // TODO Auto-generated method stub
        CAVLCWriter.writeTrailingBits(writer)
    }

    companion object {
        fun read(`is`: ByteBuffer): SEI {
            val messages: MutableList<SEIMessage> = ArrayList()
            var msg: SEIMessage?
            do {
                msg = sei_message(`is`)
                if (msg != null) messages.add(msg)
            } while (msg != null)
            return SEI(messages.toTypedArray())
        }

        private fun sei_message(`is`: ByteBuffer): SEIMessage? {
            var payloadType = 0
            var b = 0
            while (`is`.hasRemaining() && (`is`.get().toInt() and 0xff).also({ b = it }) == 0xff) {
                payloadType += 255
            }
            if (!`is`.hasRemaining()) return null
            payloadType += b
            var payloadSize = 0
            while (`is`.hasRemaining() && (`is`.get().toInt() and 0xff).also({ b = it }) == 0xff) {
                payloadSize += 255
            }
            if (!`is`.hasRemaining()) return null
            payloadSize += b
            val payload = sei_payload(payloadType, payloadSize, `is`)
            return if (payload.size != payloadSize) null else SEIMessage(payloadType, payloadSize, payload)
        }

        private fun sei_payload(payloadType: Int, payloadSize: Int, `is`: ByteBuffer): ByteArray {
            val res = ByteArray(payloadSize)
            `is`[res]
            return res
        }
    }

}