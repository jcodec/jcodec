package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import org.jcodec.platform.Platform
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MdtaBox(header: Header) : Box(header) {
    var key: String? = null
        private set

    override fun parse(buf: ByteBuffer) {
        key = Platform.stringFromBytes(NIOUtils.toArray(NIOUtils.readBuf(buf)))
    }

    override fun doWrite(out: ByteBuffer) {
        out.put(key!!.toByteArray())
    }

    override fun estimateSize(): Int {
        return key!!.toByteArray().size
    }

    companion object {
        private const val FOURCC = "mdta"
        @JvmStatic
        fun createMdtaBox(key: String?): MdtaBox {
            val box = MdtaBox(Header.createHeader(FOURCC, 0))
            box.key = key
            return box
        }

        @JvmStatic
        fun fourcc(): String {
            return FOURCC
        }
    }
}