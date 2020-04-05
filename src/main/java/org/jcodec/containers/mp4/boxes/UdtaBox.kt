package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mp4.IBoxFactory
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class UdtaBox(atom: Header) : NodeBox(atom) {
    companion object {
        private const val META_GPS = "©xyz"
        private const val LOCALE_EN_US = 0x15c7
        private val knownMetadata: MutableMap<String, Int> = HashMap()
        private const val FOURCC = "udta"

        @JvmStatic
        fun createUdtaBox(): UdtaBox {
            return UdtaBox(Header.createHeader(fourcc(), 0))
        }

        @JvmStatic
        fun fourcc(): String {
            return FOURCC
        }

        init {
            knownMetadata[META_GPS] = MetaValue.TYPE_STRING_UTF8
        }
    }

    override fun setFactory(_factory: IBoxFactory) {
        this._factory = object : IBoxFactory {
            override fun newBox(header: Header): Box {
                if (header.fourcc == UdtaMetaBox.fourcc()) {
                    val box = UdtaMetaBox(header)
                    box.setFactory(_factory)
                    return box
                }
                return _factory.newBox(header)
            }
        }
    }

    fun meta(): MetaBox {
        return findFirst(this, MetaBox.fourcc()) as MetaBox
    }

    fun getUserDataString(id: String?): List<MetaValue>? {
        val leafBox = findFirst(this, id!!) as? LeafBox? ?: return null
        val bb = leafBox.getData()
        return parseStringData(bb)
    }

    private fun parseStringData(bb: ByteBuffer): List<MetaValue> {
        val result: MutableList<MetaValue> = ArrayList()
        while (bb.remaining() >= 4) {
            var lang = 0
            var sz = NIOUtils.duplicate(bb).int
            if (4 + sz > bb.remaining()) {
                sz = bb.short.toInt()
                if (2 + sz > bb.remaining()) break
                lang = bb.short.toInt()
            }
            if (sz != 0) {
                val bytes = ByteArray(sz)
                bb[bytes]
                result.add(MetaValue.createStringWithLocale(String(bytes), lang))
            }
        }
        return result
    }

    fun serializeStringData(data: List<MetaValue>): ByteBuffer? {
        var totalLen = 0
        for (mv in data) {
            val string = mv.string
            if (string != null) totalLen += string.toByteArray().size + 4
        }
        if (totalLen == 0) return null
        val bytes = ByteArray(totalLen)
        val bb = ByteBuffer.wrap(bytes)
        for (mv in data) {
            val string = mv.string
            if (string != null) {
                bb.putShort(string.length.toShort())
                // en_US
                bb.putShort(mv.locale.toShort())
                bb.put(string.toByteArray())
            }
        }
        bb.flip()
        return bb
    }

    fun setUserDataString(id: String?, data: List<MetaValue>) {
        val bb = serializeStringData(data)
        val box = createLeafBox(Header.createHeader(id, bb!!.remaining() + 4.toLong()), bb)
        replaceBox(box)
    }

    fun latlng(): String? {
        val data = getUserDataString(META_GPS)
        return if (data == null || data.isEmpty()) null else data[0].string
    }

    fun setLatlng(value: String?) {
        val list: MutableList<MetaValue> = ArrayList()
        // en_US
        list.add(MetaValue.createStringWithLocale(value, LOCALE_EN_US))
        setUserDataString(META_GPS, list)
    }

    // only string for now :(
    var metadata: Map<Int, MetaValue>
        get() {
            val result: MutableMap<Int, MetaValue> = HashMap()
            val boxes = getBoxes()
            for (box in boxes) {
                if (knownMetadata.containsKey(box.fourcc) && box is LeafBox) {
                    val data = box.getData()
                    val type = knownMetadata[box.fourcc]!!
                    // only string for now :(
                    if (type != MetaValue.TYPE_STRING_UTF8) continue
                    val value = parseStringData(data)
                    var bytes: ByteArray?
                    bytes = try {
                        box.fourcc.toByteArray(charset("iso8859-1"))
                    } catch (e: UnsupportedEncodingException) {
                        null
                    }
                    if (bytes != null && value != null && !value.isEmpty()) result[ByteBuffer.wrap(bytes).int] = value[0]
                }
            }
            return result
        }
        set(udata) {
            val entrySet = udata.entries
            for ((key, value) in entrySet) {
                val lst: MutableList<MetaValue> = LinkedList()
                lst.add(value)
                val bb = serializeStringData(lst)
                val bytes = ByteArray(4)
                ByteBuffer.wrap(bytes).putInt(key)
                var box: LeafBox = createLeafBox(Header.createHeader(String(bytes, Charset.forName("iso8859-1")), bb!!.remaining() + 4.toLong()),
                        bb)
                replaceBox(box)
            }
        }

}