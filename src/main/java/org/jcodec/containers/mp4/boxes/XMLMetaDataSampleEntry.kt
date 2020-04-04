package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer

class XMLMetaDataSampleEntry(atom: Header) : MetaDataSampleEntry(atom) {
    private var contentEncoding // optional
            : String? = null
    private var namespace: String? = null
    private var schemaLocation // optional
            : String? = null

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        contentEncoding = NIOUtils.readNullTermString(input)
        namespace = NIOUtils.readNullTermString(input)
        schemaLocation = NIOUtils.readNullTermString(input)
        parseExtensions(input)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        NIOUtils.writeNullTermString(out, contentEncoding)
        NIOUtils.writeNullTermString(out, namespace)
        NIOUtils.writeNullTermString(out, schemaLocation)
        writeExtensions(out)
    }
}