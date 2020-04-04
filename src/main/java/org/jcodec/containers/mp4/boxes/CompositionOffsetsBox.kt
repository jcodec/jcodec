package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Default box factory
 *
 * @author The JCodec project
 */
class CompositionOffsetsBox(header: Header) : FullBox(header) {
    var entries: Array<Entry?> = emptyArray()
        private set

    class Entry(var count: Int, var offset: Int)

    class LongEntry(var count: Long, var offset: Long)

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        val num = input.int
        entries = arrayOfNulls(num)
        for (i in 0 until num) {
            entries[i] = Entry(input.int, input.int)
        }
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(entries.size)
        for (i in entries.indices) {
            out.putInt(entries[i]!!.count)
            out.putInt(entries[i]!!.offset)
        }
    }

    override fun estimateSize(): Int {
        return 12 + 4 + entries.size * 8
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "ctts"
        }

        @JvmStatic
        fun createCompositionOffsetsBox(entries: Array<Entry?>): CompositionOffsetsBox {
            val ctts = CompositionOffsetsBox(Header(fourcc()))
            ctts.entries = entries
            return ctts
        }
    }
}