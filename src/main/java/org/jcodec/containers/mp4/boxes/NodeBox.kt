package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mp4.IBoxFactory
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A node box
 *
 * A box containing children, no data
 *
 * @author The JCodec project
 */
open class NodeBox(atom: Header) : Box(atom) {
    protected var _boxes: MutableList<Box>
    protected var _factory: IBoxFactory? = null
    open fun setFactory(factory: IBoxFactory) {
        this._factory = factory
    }

    override fun parse(input: ByteBuffer) {
        while (input.remaining() >= 8) {
            val child = parseChildBox(input, _factory)
            if (child != null) _boxes.add(child)
        }
    }

    fun getBoxes(): MutableList<Box> {
        return _boxes
    }

    fun add(box: Box) {
        _boxes.add(box)
    }

    override fun doWrite(out: ByteBuffer) {
        for (box in _boxes) {
            box.write(out)
        }
    }

    override fun estimateSize(): Int {
        var total = 0
        for (box in _boxes) {
            total += box.estimateSize()
        }
        return total + Header.estimateHeaderSize(total)
    }

    fun addFirst(box: MovieHeaderBox) {
        _boxes.add(0, box)
    }

    fun replace(fourcc: String, box: Box) {
        removeChildren(arrayOf(fourcc))
        add(box)
    }

    fun replaceBox(box: Box) {
        removeChildren(arrayOf(box.fourcc))
        add(box)
    }

    override fun dump(sb: StringBuilder) {
        sb.append("{\"tag\":\"" + header.fourcc + "\",")
        sb.append("\"boxes\": [")
        dumpBoxes(sb)
        sb.append("]")
        sb.append("}")
    }

    protected fun dumpBoxes(sb: StringBuilder) {
        for (i in _boxes.indices) {
            _boxes[i].dump(sb)
            if (i < _boxes.size - 1) sb.append(",")
        }
    }

    fun removeChildren(fourcc: Array<String>) {
        val it = _boxes.iterator()
        while (it.hasNext()) {
            val box = it.next()
            val fcc = box.fourcc
            for (i in fourcc.indices) {
                val cand = fourcc[i]
                if (cand == fcc) {
                    it.remove()
                    break
                }
            }
        }
    }

    companion object {
        fun parseChildBox(input: ByteBuffer?, factory: IBoxFactory?): Box? {
            val fork = input!!.duplicate()
            while (input.remaining() >= 4 && fork.int == 0) input.int
            if (input.remaining() < 4) return null
            var ret: Box? = null
            val childAtom = Header.read(input)
            if (childAtom != null && input.remaining() >= childAtom.bodySize) {
                ret = parseBox(NIOUtils.read(input, childAtom.bodySize.toInt()), childAtom, factory!!)
            }
            return ret
        }

        fun doCloneBox(box: Box, approxSize: Int, bf: IBoxFactory?): Box? {
            val buf = ByteBuffer.allocate(approxSize)
            box.write(buf)
            buf.flip()
            return parseChildBox(buf, bf)
        }

        @JvmStatic
        fun cloneBox(box: Box, approxSize: Int, bf: IBoxFactory?): Box? {
            return doCloneBox(box, approxSize, bf)
        }

        inline fun <reified T : Box?> findDeep(box: Box?, class1: Class<T>?, name: String): Array<T> {
            val storage: MutableList<T> = ArrayList()
            findDeepInner(box, class1, name, storage)
            return storage.toTypedArray()
        }

        fun <T : Box?> findDeepInner(box: Box?, class1: Class<T>?, name: String, storage: MutableList<T>) {
            if (box == null) return
            if (name == box.header.fourcc) {
                storage.add(box as T)
                return
            }
            if (box is NodeBox) {
                for (candidate in box.getBoxes()) {
                    findDeepInner(candidate, class1, name, storage)
                }
            }
        }

        @JvmStatic
        fun findAll(box: Box?, path: String): Array<Box?> {
            return findAllPath(box, arrayOf(path))
        }

        @JvmStatic
        fun findFirst(box: NodeBox?, path: String): Box? {
            return findFirstPath(box, arrayOf(path))
        }

        @JvmStatic
        fun findFirstPath(box: NodeBox?, path: Array<String?>): Box? {
            val result = findAllPath(box, path)
            return if (result.size > 0) result[0] else null
        }

        @JvmStatic
        fun findAllPath(box: Box?, path: Array<String?>): Array<Box?> {
            val result: MutableList<Box?> = LinkedList()
            findBox(box, ArrayList(listOf(*path)), result)
            val it = result.listIterator()
            while (it.hasNext()) {
                val next = it.next()
                if (next == null) {
                    it.remove()
                }
            }
            return result.toTypedArray()
        }

        fun findBox(root: Box?, path: MutableList<String?>, result: MutableCollection<Box?>) {
            if (path.size > 0) {
                val head = path.removeAt(0)
                if (root is NodeBox) {
                    for (candidate in root.getBoxes()) {
                        if (head == null || head == candidate.header.fourcc) {
                            findBox(candidate, path, result)
                        }
                    }
                }
                path.add(0, head)
            } else {
                result.add(root)
            }
        }
    }

    init {
        _boxes = LinkedList()
    }
}