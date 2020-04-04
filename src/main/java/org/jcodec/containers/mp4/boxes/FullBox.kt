package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
abstract class FullBox(atom: Header) : Box(atom) {
    var version: Byte = 0
    var flags = 0
    override fun parse(input: ByteBuffer) {
        val vf = input.int
        version = (vf shr 24 and 0xff).toByte()
        flags = vf and 0xffffff
    }

    override fun doWrite(out: ByteBuffer) {
        out!!.putInt(version.toInt() shl 24 or (flags and 0xffffff))
    }

}