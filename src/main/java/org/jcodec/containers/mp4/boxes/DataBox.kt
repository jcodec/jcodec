package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class DataBox(header: Header) : Box(header) {
    var type = 0
        private set
    var locale = 0
        private set
    var data: ByteArray = ByteArray(0)
        private set

    override fun parse(buf: ByteBuffer) {
        type = buf.int
        locale = buf.int
        data = NIOUtils.toArray(NIOUtils.readBuf(buf))
    }

    override fun doWrite(out: ByteBuffer) {
        out.putInt(type)
        out.putInt(locale)
        out.put(data)
    }

    override fun estimateSize(): Int {
        return 16 + data.size
    }

    companion object {
        private const val FOURCC = "data"
        @JvmStatic
        fun createDataBox(type: Int, locale: Int, data: ByteArray): DataBox {
            val box = DataBox(Header.createHeader(FOURCC, 0))
            box.type = type
            box.locale = locale
            box.data = data
            return box
        }

        @JvmStatic
        fun fourcc(): String {
            return FOURCC
        }
    }
}