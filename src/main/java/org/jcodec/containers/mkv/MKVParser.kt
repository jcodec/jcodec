package org.jcodec.containers.mkv

import org.jcodec.common.and
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mkv.boxes.EbmlBase
import org.jcodec.containers.mkv.boxes.EbmlBin
import org.jcodec.containers.mkv.boxes.EbmlMaster
import org.jcodec.containers.mkv.boxes.EbmlVoid
import org.jcodec.containers.mkv.util.EbmlUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 *
 * EBML IO implementation
 *
 * @author The JCodec project
 */
class MKVParser(private val channel: SeekableByteChannel) {
    private val trace: LinkedList<EbmlMaster?>

    @Throws(IOException::class)
    fun parse(): List<EbmlMaster?> {
        val tree: MutableList<EbmlMaster?> = ArrayList()
        var e: EbmlBase? = null
        while (nextElement().also { e = it } != null) {
            if (!isKnownType(e!!.id)) System.err.println("Unspecified header: " + EbmlUtil.toHexString(e!!.id) + " at " + e!!.offset)
            while (!possibleChild(trace.peekFirst(), e)) closeElem(trace.removeFirst(), tree)
            openElem(e)
            if (e is EbmlMaster) {
                trace.push(e as EbmlMaster?)
            } else if (e is EbmlBin) {
                val bin = e as EbmlBin
                val traceTop = trace.peekFirst()
                if (traceTop!!.dataOffset + traceTop.dataLen < bin.dataOffset + bin.dataLen) {
                    channel.setPosition(traceTop.dataOffset + traceTop.dataLen)
                } else try {
                    bin.readChannel(channel)
                } catch (oome: OutOfMemoryError) {
                    throw RuntimeException(bin.type.toString() + " 0x" + EbmlUtil.toHexString(bin.id) + " size: " + java.lang.Long.toHexString(bin.dataLen.toLong()) + " offset: 0x" + java.lang.Long.toHexString(bin.offset), oome)
                }
                trace.peekFirst()!!.add(e)
            } else if (e is EbmlVoid) {
                (e as EbmlVoid).skip(channel)
            } else {
                throw RuntimeException("Currently there are no elements that are neither Master nor Binary, should never actually get here")
            }
        }
        while (trace.peekFirst() != null) closeElem(trace.removeFirst(), tree)
        return tree
    }

    private fun possibleChild(parent: EbmlMaster?, child: EbmlBase?): Boolean {
        return if (parent != null && MKVType.Cluster == parent.type && child != null && MKVType.Cluster != child.type && MKVType.Info != child.type && MKVType.SeekHead != child.type && MKVType.Tracks != child.type
                && MKVType.Cues != child.type && MKVType.Attachments != child.type && MKVType.Tags != child.type && MKVType.Chapters != child.type) true else MKVType.possibleChild(parent, child!!)
    }

    private fun openElem(e: EbmlBase?) {
        /*
         * Whatever logging you would like to have. Here's just one example
         */
        // System.out.println(e.type.name() + (e instanceof EbmlMaster ? " master " : "") + " id: " + printAsHex(e.id) + " off: 0x" + toHexString(e.offset).toUpperCase() + " data off: 0x" +
        // toHexString(e.dataOffset).toUpperCase() + " len: 0x" + toHexString(e.dataLen).toUpperCase());
    }

    private fun closeElem(e: EbmlMaster?, tree: MutableList<EbmlMaster?>) {
        if (trace.peekFirst() == null) {
            tree.add(e)
        } else {
            trace.peekFirst()!!.add(e)
        }
    }

    @Throws(IOException::class)
    private fun nextElement(): EbmlBase? {
        var offset = channel.position()
        if (offset >= channel.size()) return null
        var typeId = readEbmlId(channel)
        while (typeId == null && !isKnownType(typeId) && offset < channel.size()) {
            offset++
            channel.setPosition(offset)
            typeId = readEbmlId(channel)
        }
        val dataLen = readEbmlInt(channel)
        val elem = MKVType.createById<EbmlBase>(typeId, offset)
        elem.offset = offset
        elem.typeSizeLength = (channel.position() - offset).toInt()
        elem.dataOffset = channel.position()
        elem.dataLen = dataLen.toInt()
        return elem
    }

    fun isKnownType(b: ByteArray?): Boolean {
        return if (!trace.isEmpty() && MKVType.Cluster == trace.peekFirst()!!.type) true else MKVType.isSpecifiedHeader(b)
    }

    companion object {
        /**
         * Reads an EBML id from the channel. EBML ids have length encoded inside of them For instance, all one-byte ids have first byte set to '1', like 0xA3 or 0xE7, whereas the two-byte ids have first
         * byte set to '0' and second byte set to '1', thus: 0x42 0x86  or 0x42 0xF7
         *
         * @return byte array filled with the ebml id
         * @throws IOException
         */
        @JvmStatic
        @Throws(IOException::class)
        fun readEbmlId(source: SeekableByteChannel): ByteArray? {
            if (source.position() == source.size()) return null
            val buffer = ByteBuffer.allocate(8)
            buffer.limit(1)
            source.read(buffer)
            buffer.flip()
            val firstByte = buffer.get()
            val numBytes = EbmlUtil.computeLength(firstByte)
            if (numBytes == 0) return null
            if (numBytes > 1) {
                buffer.limit(numBytes)
                source.read(buffer)
            }
            buffer.flip()
            val `val` = ByteBuffer.allocate(buffer.remaining())
            `val`.put(buffer)
            return `val`.array()
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readEbmlInt(source: SeekableByteChannel): Long {
            val buffer = ByteBuffer.allocate(8)
            buffer.limit(1)
            source.read(buffer)
            buffer.flip()

            // read the first byte
            val firstByte = buffer.get()
            var length = EbmlUtil.computeLength(firstByte)
            if (length == 0) throw RuntimeException("Invalid ebml integer size.")

            // read the reset
            buffer.limit(length)
            source.read(buffer)
            buffer.position(1)

            // use the first byte
            var value: Long = (firstByte and (0xFF ushr length)).toLong()
            length--

            // use the reset
            while (length > 0) {
                value = value shl 8 or (buffer.get() and 0xff).toLong()
                length--
            }
            return value
        }
    }

    init {
        trace = LinkedList()
    }
}