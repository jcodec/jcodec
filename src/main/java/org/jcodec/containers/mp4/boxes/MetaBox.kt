package org.jcodec.containers.mp4.boxes

import org.jcodec.containers.mp4.boxes.DataBox.Companion.createDataBox
import org.jcodec.containers.mp4.boxes.HandlerBox.Companion.createHandlerBox
import org.jcodec.containers.mp4.boxes.Header.Companion.createHeader
import org.jcodec.containers.mp4.boxes.IListBox.Companion.createIListBox
import org.jcodec.containers.mp4.boxes.KeysBox.Companion.createKeysBox
import org.jcodec.containers.mp4.boxes.MdtaBox.Companion.createMdtaBox
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class MetaBox(atom: Header) : NodeBox(atom) {
    var keyedMeta: Map<String, MetaValue>
        get() {
            val result: MutableMap<String, MetaValue> = LinkedHashMap()
            val ilst = findFirst(this, IListBox.fourcc()) as IListBox?
            val findAllPath = findAllPath(this, arrayOf(KeysBox.fourcc(), MdtaBox.fourcc()))
            val keys: List<MdtaBox?> = findAllPath.map { it as? MdtaBox? }
            if (ilst == null || keys.size == 0) return result
            for ((key, value1) in ilst.getValues()) {
                val index = key ?: continue
                val db = getDataBox(value1) ?: continue
                val value = MetaValue.createOtherWithLocale(db.type, db.locale, db.data)
                if (index > 0 && index <= keys.size) {
                    result[keys[index - 1]!!.key!!] = value
                }
            }
            return result
        }
        set(map) {
            if (map.isEmpty()) return
            val keys = createKeysBox()
            val data: MutableMap<Int, List<Box>> = LinkedHashMap()
            var i = 1
            for ((key, v) in map) {
                keys.add(createMdtaBox(key))
                val children: MutableList<Box> = ArrayList()
                children.add(createDataBox(v.type, v.locale, v.data))
                data[i] = children
                ++i
            }
            val ilst = createIListBox(data)
            replaceBox(keys)
            replaceBox(ilst)
        }

    private fun getDataBox(value: List<Box>): DataBox? {
        for (box in value) {
            if (box is DataBox) {
                return box
            }
        }
        return null
    }

    override fun parse(input: ByteBuffer) {
        super.parse(input)
    }

    // Updating values

    // Adding values

    // Dropping values
    var itunesMeta: Map<Int, MetaValue>
        get() {
            val result: MutableMap<Int, MetaValue> = LinkedHashMap()
            val ilst = findFirst(this, IListBox.fourcc()) as? IListBox? ?: return result
            for ((key, value1) in ilst.getValues()) {
                val index = key ?: continue
                val db = getDataBox(value1) ?: continue
                val value = MetaValue.createOtherWithLocale(db.type, db.locale, db.data)
                result[index] = value
            }
            return result
        }
        set(map) {
            if (map.isEmpty()) return
            val copy: MutableMap<Int, MetaValue> = LinkedHashMap()
            copy.putAll(map)
            val ilst = findFirst(this, IListBox.fourcc()) as? IListBox?
            val data: MutableMap<Int, MutableList<Box>>
            if (ilst == null) {
                data = LinkedHashMap()
            } else {
                data = ilst.getValues().mapValues { it.value.toMutableList() }.toMutableMap()

                // Updating values
                for ((index, value) in data) {
                    val v = copy[index]
                    if (v != null) {
                        val dataBox = createDataBox(v.type, v.locale, v.data)
                        dropChildBox(value, DataBox.fourcc())
                        value.add(dataBox)
                        copy.remove(index)
                    }
                }
            }

            // Adding values
            for ((index, v) in copy) {
                val dataBox = createDataBox(v.type, v.locale, v.data)
                val children: MutableList<Box> = ArrayList()
                data.put(index, children)
                children.add(dataBox)
            }

            // Dropping values
            val keySet: MutableSet<Int> = HashSet(data.keys)
            keySet.removeAll(map.keys)
            for (dropped in keySet) {
                data.remove(dropped)
            }
            replaceBox(createHandlerBox(null, "mdir", "appl", 0, 0))
            replaceBox(createIListBox(data.mapValues { it.value.toList() }.toMutableMap()))
        }

    private fun dropChildBox(children: MutableList<Box>, fourcc2: String) {
        val listIterator = children.listIterator()
        while (listIterator.hasNext()) {
            val next = listIterator.next()
            if (fourcc2 == next.fourcc) {
                listIterator.remove()
            }
        }
    }

    companion object {
        private const val FOURCC = "meta"
        @JvmStatic
        fun createMetaBox(): MetaBox {
            return MetaBox(createHeader(fourcc(), 0))
        }

        @JvmStatic
        fun fourcc(): String {
            return FOURCC
        }
    }
}