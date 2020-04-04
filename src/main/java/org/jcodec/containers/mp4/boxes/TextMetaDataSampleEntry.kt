package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Describes video payload sample
 *
 * @author The JCodec project
 */
class TextMetaDataSampleEntry : MetaDataSampleEntry {
    var contentEncoding // optional
            : String? = null
        private set
    var mimeFormat: String? = null
        private set

    constructor(contentEncoding: String?, mimeFormat: String?) : super(Header.createHeader("mett", 0)) {
        this.contentEncoding = contentEncoding
        this.mimeFormat = mimeFormat
    }

    constructor(atom: Header) : super(atom) {}

    override fun parse(input: ByteBuffer) {
        val asString = String(NIOUtils.toArray(input))
        if (asString.startsWith("application")) {
            drefInd = 1.toShort()
        } else {
            super.parse(input)
        }
        contentEncoding = NIOUtils.readNullTermString(input)
        mimeFormat = NIOUtils.readNullTermString(input)
        parseExtensions(input)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        NIOUtils.writeNullTermString(out, contentEncoding)
        NIOUtils.writeNullTermString(out, mimeFormat)
        writeExtensions(out)
    }

}