package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class URIInitBox(atom: Header) : FullBox(atom) {
    var data: ByteArray = ByteArray(0)
        private set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        data = NIOUtils.toArray(input)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.put(data)
    }

    override fun estimateSize(): Int {
        return 13 + data.size
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "uriI"
        }

        fun createURIInitBox(data: ByteArray): URIInitBox {
            val uriInitBox = URIInitBox(Header(fourcc()))
            uriInitBox.data = data
            return uriInitBox
        }
    }
}