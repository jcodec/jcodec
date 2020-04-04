package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mp4.Boxes
import org.jcodec.containers.mp4.IBoxFactory
import org.jcodec.containers.mp4.boxes.DataBox
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class IListBox(atom: Header) : Box(atom) {
    private var values: MutableMap<Int, List<Box>>
    private val factory: IBoxFactory

    private class LocalBoxes internal constructor() : Boxes() {
        //Initializing blocks are not supported by Javascript.
        init {
            mappings[DataBox.fourcc()] = DataBox::class.java
        }
    }

    override fun parse(buf: ByteBuffer) {
        val input = buf
        while (input.remaining() >= 4) {
            val size = input.int
            val local = NIOUtils.read(input, size - 4)
            val index = local.int
            val children: MutableList<Box> = ArrayList()
            values[index] = children
            while (local.hasRemaining()) {
                val childAtom = Header.read(local)
                if (childAtom != null && local.remaining() >= childAtom.bodySize) {
                    val box = parseBox(NIOUtils.read(local, childAtom.bodySize.toInt()), childAtom, factory)
                    children.add(box)
                }
            }
        }
    }

    fun getValues(): Map<Int, List<Box>> {
        return values
    }

    override fun doWrite(out: ByteBuffer) {
        for ((key, value) in values) {
            val fork = out.duplicate()
            out.putInt(0)
            out.putInt(key)
            for (box in value) {
                box.write(out)
            }
            fork.putInt(out.position() - fork.position())
        }
    }

    override fun estimateSize(): Int {
        var sz = 8
        for ((_, value) in values) {
            for (box in value) {
                sz += 8 + box.estimateSize()
            }
        }
        return sz
    }

    companion object {
        private const val FOURCC = "ilst"
        @JvmStatic
        fun createIListBox(values: MutableMap<Int, List<Box>>): IListBox {
            val box = IListBox(Header.createHeader(FOURCC, 0))
            box.values = values
            return box
        }

        @JvmStatic
        fun fourcc(): String {
            return FOURCC
        }
    }

    init {
        factory = SimpleBoxFactory(LocalBoxes())
        values = LinkedHashMap()
    }
}