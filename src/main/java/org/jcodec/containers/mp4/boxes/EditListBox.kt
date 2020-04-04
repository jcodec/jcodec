package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class EditListBox(atom: Header) : FullBox(atom) {
    private var edits: MutableList<Edit>? = null
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        val edits = ArrayList<Edit>()
        val num = input.int.toLong()
        for (i in 0 until num) {
            val duration = input.int
            val mediaTime = input.int
            val rate = input.int / 65536f
            edits.add(Edit(duration.toLong(), mediaTime.toLong(), rate))
        }
        this.edits = edits
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(edits!!.size)
        for (edit in edits!!) {
            out.putInt(edit.duration.toInt())
            out.putInt(edit.mediaTime.toInt())
            out.putInt((edit.rate * 65536).toInt())
        }
    }

    override fun estimateSize(): Int {
        return 12 + 4 + edits!!.size * 12
    }

    fun getEdits(): List<Edit>? {
        return edits
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "elst"
        }

        @JvmStatic
        fun createEditListBox(edits: MutableList<Edit>?): EditListBox {
            val elst = EditListBox(Header(fourcc()))
            elst.edits = edits
            return elst
        }
    }
}