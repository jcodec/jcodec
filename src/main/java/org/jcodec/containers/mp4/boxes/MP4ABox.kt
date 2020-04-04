package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MP4ABox(header: Header) : Box(header) {
    private var `val` = 0
    override fun doWrite(out: ByteBuffer) {
        out.putInt(`val`)
    }

    override fun parse(buf: ByteBuffer) {
        `val` = buf.int
    }

    override fun estimateSize(): Int {
        return 12
    }
}