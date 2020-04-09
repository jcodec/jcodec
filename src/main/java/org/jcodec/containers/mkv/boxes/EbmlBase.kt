package org.jcodec.containers.mkv.boxes

import org.jcodec.common.UsedViaReflection
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mkv.MKVType
import org.jcodec.containers.mkv.util.EbmlUtil.ebmlLength
import org.jcodec.platform.Platform
import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 *
 * EBML IO implementation
 *
 * @author The JCodec project
 */
abstract class EbmlBase @UsedViaReflection constructor(@JvmField var id: ByteArray) {
    var parent: EbmlMaster? = null
    @JvmField
    var type: MKVType? = null
    @JvmField
    var _dataLen = 0
    @JvmField
    var offset: Long = 0
    @JvmField
    var dataOffset: Long = 0
    var typeSizeLength = 0
    fun equalId(typeId: ByteArray?): Boolean {
        return Platform.arrayEqualsByte(id, typeId)
    }

    abstract fun getData(): ByteBuffer?

    open fun size(): Long {
        return (_dataLen + ebmlLength(_dataLen.toLong()) + id.size).toLong()
    }

    @Throws(IOException::class)
    fun mux(os: SeekableByteChannel): Long {
        val bb = getData()
        return os.write(bb).toLong()
    }

}