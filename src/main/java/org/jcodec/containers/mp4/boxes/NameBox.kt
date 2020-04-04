package org.jcodec.containers.mp4.boxes

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class NameBox(header: Header) : Box(header) {
    var name: String? = null
        private set

    override fun parse(buf: ByteBuffer) {
        name = NIOUtils.readNullTermString(buf)
    }

    override fun doWrite(out: ByteBuffer) {
        out.put(JCodecUtil2.asciiString(name))
        out.putInt(0)
    }

    override fun estimateSize(): Int {
        return 12 + JCodecUtil2.asciiString(name).size
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "name"
        }

        @JvmStatic
        fun createNameBox(name: String?): NameBox {
            val box = NameBox(Header(fourcc()))
            box.name = name
            return box
        }
    }
}