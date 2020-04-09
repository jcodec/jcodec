package org.jcodec.containers.mkv.boxes

import org.jcodec.containers.mkv.util.EbmlUtil.ebmlEncode
import org.jcodec.containers.mkv.util.EbmlUtil.ebmlLength
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * EBML IO implementation
 *
 * @author The JCodec project
 */
open class EbmlMaster(id: ByteArray) : EbmlBase(id) {
    protected var usedSize: Long = 0
    @JvmField
    val children: ArrayList<EbmlBase>?
    fun add(elem: EbmlBase?) {
        if (elem == null) return
        elem.parent = this
        children!!.add(elem)
        //        dataLen += elem.size();
    }

    override fun getData(): ByteBuffer {
        val size = dataLen
        if (size > Int.MAX_VALUE) println("EbmlMaster.getData: id.length " + id.size + "  EbmlUtil.ebmlLength(" + size + "): " + ebmlLength(size) + " size: " + size)
        val bb = ByteBuffer.allocate((id.size + ebmlLength(size) + size).toInt())
        bb.put(id)
        bb.put(ebmlEncode(size))
        for (i in children!!.indices) bb.put(children[i].getData())
        bb.flip()
        return bb
    }

    protected val dataLen: Long
        protected get() {
            if (children == null || children.isEmpty()) return _dataLen.toLong()
            var dataLength: Long = 0
            for (e in children) dataLength += e.size()
            return dataLength
        }

    override fun size(): Long {
        var size = dataLen
        size += ebmlLength(size).toLong()
        size += id.size.toLong()
        return size
    }

    companion object {
        @JvmField
        val CLUSTER_ID = byteArrayOf(0x1F, 0x43.toByte(), 0xB6.toByte(), 0x75.toByte())
    }

    init {
        children = ArrayList()
        this.id = id
    }
}