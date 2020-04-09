package org.jcodec.containers.mxf.model

import org.jcodec.common.logging.Logger
import org.jcodec.containers.mxf.model.UL.Companion.read
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class GenericPackage(ul: UL?) : MXFInterchangeObject(ul) {
    var tracks: Array<UL?> = emptyArray()
        private set
    var packageUID: UL? = null
        private set
    var name: String? = null
        private set
    var packageModifiedDate: Date? = null
        private set
    var packageCreationDate: Date? = null
        private set

    override fun read(tags: MutableMap<Int, ByteBuffer>) {
        val it: MutableIterator<Map.Entry<Int, ByteBuffer>> = tags.entries.iterator()
        loop@ while (it.hasNext()) {
            val entry = it.next()
            val _bb = entry.value
            when (entry.key) {
                0x4401 -> packageUID = read(_bb)
                0x4402 -> name = readUtf16String(_bb)
                0x4403 -> tracks = readULBatch(_bb)
                0x4404 -> packageModifiedDate = readDate(_bb)
                0x4405 -> packageCreationDate = readDate(_bb)
                else -> {
                    Logger.warn(String.format("Unknown tag [ $ul]: %04x", entry.key))
                    continue@loop
                }
            }
            it.remove()
        }
    }

}