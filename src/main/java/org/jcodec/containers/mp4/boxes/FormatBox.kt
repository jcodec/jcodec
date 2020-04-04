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
class FormatBox(header: Header) : Box(header) {
    private var fmt: String? = null
    override fun parse(buf: ByteBuffer) {
        fmt = NIOUtils.readString(buf, 4)
    }

    override fun doWrite(out: ByteBuffer) {
        out.put(JCodecUtil2.asciiString(fmt))
    }

    override fun estimateSize(): Int {
        return JCodecUtil2.asciiString(fmt).size + 8
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "frma"
        }

        fun createFormatBox(fmt: String?): FormatBox {
            val frma = FormatBox(Header(fourcc()))
            frma.fmt = fmt
            return frma
        }
    }
}