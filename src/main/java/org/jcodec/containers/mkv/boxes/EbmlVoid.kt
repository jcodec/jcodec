package org.jcodec.containers.mkv.boxes

import org.jcodec.common.io.SeekableByteChannel
import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class EbmlVoid(id: ByteArray) : EbmlBase(id) {
    override fun getData(): ByteBuffer? {
        return null
    }

    @Throws(IOException::class)
    fun skip(`is`: SeekableByteChannel) {
        `is`.setPosition(dataOffset + _dataLen)
    }
}