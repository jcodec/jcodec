package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

class URIMetaSampleEntry(atom: Header) : MetaDataSampleEntry(atom) {
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        parseExtensions(input)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        writeExtensions(out)
    }
}