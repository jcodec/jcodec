package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class ClipRegionBox(atom: Header) : Box(atom) {
    var rgnSize: Short = 0
        private set
    var y: Short = 0
        private set
    var x: Short = 0
        private set
    var height: Short = 0
        private set
    var width: Short = 0
        private set

    override fun parse(input: ByteBuffer) {
        rgnSize = input.short
        y = input.short
        x = input.short
        height = input.short
        width = input.short
    }

    override fun doWrite(out: ByteBuffer) {
        out.putShort(rgnSize)
        out.putShort(y)
        out.putShort(x)
        out.putShort(height)
        out.putShort(width)
    }

    override fun estimateSize(): Int {
        return 10 + 8
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "crgn"
        }

        @JvmStatic
        fun createClipRegionBox(x: Short, y: Short, width: Short, height: Short): ClipRegionBox {
            val b = ClipRegionBox(Header(fourcc()))
            b.rgnSize = 10
            b.x = x
            b.y = y
            b.width = width
            b.height = height
            return b
        }
    }
}