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
class TextConfigBox(atom: Header) : FullBox(atom) {
    var textConfig: String? = null
        private set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        textConfig = NIOUtils.readNullTermStringCharset(input, Platform.UTF_8)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        NIOUtils.writeNullTermString(out, textConfig)
    }

    override fun estimateSize(): Int {
        return 13 + Platform.getBytesForCharset(textConfig, Platform.UTF_8).size
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "txtC"
        }

        fun createTextConfigBox(textConfig: String?): TextConfigBox {
            val box = TextConfigBox(Header(fourcc()))
            box.textConfig = textConfig
            return box
        }
    }
}