package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class ClearApertureBox(atom: Header) : FullBox(atom) {
    var width = 0f
        protected set
    var height = 0f
        protected set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        width = input.int / 65536f
        height = input.int / 65536f
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt((width * 65536f).toInt())
        out.putInt((height * 65536f).toInt())
    }

    override fun estimateSize(): Int {
        return 20
    }

    companion object {
        const val CLEF = "clef"
        @JvmStatic
        fun createClearApertureBox(width: Int, height: Int): ClearApertureBox {
            val clef = ClearApertureBox(Header(CLEF))
            clef.width = width.toFloat()
            clef.height = height.toFloat()
            return clef
        }
    }
}