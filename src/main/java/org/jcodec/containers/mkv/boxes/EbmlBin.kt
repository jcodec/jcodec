package org.jcodec.containers.mkv.boxes

import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mkv.util.EbmlUtil.ebmlEncodeLen
import org.jcodec.containers.mkv.util.EbmlUtil.ebmlLength
import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 *
 * EBML IO implementation
 *
 * @author The JCodec project
 */
open class EbmlBin(id: ByteArray) : EbmlBase(id) {
    @JvmField
    var _data: ByteBuffer? = null
    protected var dataRead = false

    @Throws(IOException::class)
    open fun readChannel(`is`: SeekableByteChannel) {
        val bb = ByteBuffer.allocate(_dataLen)
        `is`.read(bb)
        bb.flip()
        read(bb)
    }

    open fun read(source: ByteBuffer) {
        _data = source.slice()
        _data!!.limit(_dataLen)
        dataRead = true
    }

    fun skip(source: ByteBuffer) {
        if (!dataRead) {
            source.position((dataOffset + _dataLen).toInt())
            dataRead = true
        }
    }

    override fun size(): Long {
        if (_data == null || _data!!.limit() == 0) return super.size()
        var totalSize = _data!!.limit().toLong()
        totalSize += ebmlLength(_data!!.limit().toLong()).toLong()
        totalSize += id.size.toLong()
        return totalSize
    }

    fun setBuf(data: ByteBuffer) {
        _data = data.slice()
        _dataLen = _data!!.limit()
    }

    override fun getData(): ByteBuffer {
        val sizeSize = ebmlLength(_data!!.limit().toLong())
        val size = ebmlEncodeLen(_data!!.limit().toLong(), sizeSize)
        val bb = ByteBuffer.allocate(id.size + sizeSize + _data!!.limit())
        bb.put(id)
        bb.put(size)
        bb.put(_data)
        bb.flip()
        _data!!.flip()
        return bb
    }
}