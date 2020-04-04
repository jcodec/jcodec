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
class UrlBox(atom: Header) : FullBox(atom) {
    var url: String? = null
        private set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        if (flags and 0x1 != 0) return
        url = NIOUtils.readNullTermStringCharset(input, Platform.UTF_8)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        if (url != null) {
            NIOUtils.write(out, ByteBuffer.wrap(Platform.getBytesForCharset(url, Platform.UTF_8)))
            out.put(0.toByte())
        }
    }

    override fun estimateSize(): Int {
        var sz = 13
        if (url != null) {
            sz += Platform.getBytesForCharset(url, Platform.UTF_8).size
        }
        return sz
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "url "
        }

        @JvmStatic
        fun createUrlBox(url: String?): UrlBox {
            val urlBox = UrlBox(Header(fourcc()))
            urlBox.url = url
            return urlBox
        }
    }
}