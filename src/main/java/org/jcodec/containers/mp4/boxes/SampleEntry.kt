package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Creates MP4 file out of a set of samples
 *
 * @author The JCodec project
 */
open class SampleEntry(header: Header) : NodeBox(header) {
    open var drefInd: Short = 0
    override fun parse(input: ByteBuffer) {
        input.int
        input.short
        drefInd = input.short
    }

    protected fun parseExtensions(input: ByteBuffer) {
        super.parse(input)
    }

    override fun doWrite(out: ByteBuffer) {
        out!!.put(byteArrayOf(0, 0, 0, 0, 0, 0))
        out.putShort(drefInd) // data ref index
    }

    protected fun writeExtensions(out: ByteBuffer) {
        super.doWrite(out)
    }

    fun setMediaType(mediaType: String?) {
        header = Header(mediaType)
    }

    override fun estimateSize(): Int {
        return 8 + super.estimateSize()
    }
}