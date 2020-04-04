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
class URIBox(atom: Header) : FullBox(atom) {
    var uri: String? = null
        private set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        uri = NIOUtils.readNullTermStringCharset(input, Platform.UTF_8)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        NIOUtils.writeNullTermString(out, uri)
    }

    override fun estimateSize(): Int {
        return 13 + Platform.getBytesForCharset(uri, Platform.UTF_8).size
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "uri "
        }

        fun createUriBox(uri: String?): URIBox {
            val uriBox = URIBox(Header(fourcc()))
            uriBox.uri = uri
            return uriBox
        }
    }
}