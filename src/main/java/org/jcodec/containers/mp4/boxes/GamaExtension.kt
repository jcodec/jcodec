package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 *
 * @author The JCodec project
 */
class GamaExtension(header: Header) : Box(header) {
    var gamma = 0f
        private set

    override fun parse(buf: ByteBuffer) {
        val g = buf.int.toFloat()
        gamma = g / 65536f
    }

    override fun doWrite(out: ByteBuffer) {
        out.putInt((gamma * 65536).toInt())
    }

    override fun estimateSize(): Int {
        return 12
    }

    companion object {
        fun createGamaExtension(gamma: Float): GamaExtension {
            val gamaExtension = GamaExtension(Header(fourcc()))
            gamaExtension.gamma = gamma
            return gamaExtension
        }

        @JvmStatic
        fun fourcc(): String {
            return "gama"
        }
    }
}