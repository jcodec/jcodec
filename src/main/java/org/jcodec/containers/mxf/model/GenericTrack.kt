package org.jcodec.containers.mxf.model

import org.jcodec.common.logging.Logger
import org.jcodec.containers.mxf.model.UL.Companion.read
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class GenericTrack(ul: UL?) : MXFInterchangeObject(ul) {
    var trackId = 0
        private set
    var name: String? = null
        private set
    var sequenceRef: UL? = null
        private set
    var trackNumber = 0
        private set

    override fun read(tags: MutableMap<Int, ByteBuffer>) {
        val it: MutableIterator<Map.Entry<Int, ByteBuffer>> = tags.entries.iterator()
        loop@ while (it.hasNext()) {
            val entry = it.next()
            val _bb = entry.value
            when (entry.key) {
                0x4801 -> trackId = _bb.int
                0x4802 -> name = readUtf16String(_bb)
                0x4803 -> sequenceRef = read(_bb)
                0x4804 -> trackNumber = _bb.int
                else -> {
                    Logger.warn(String.format("Unknown tag [ $ul]: %04x", entry.key))
                    continue@loop
                }
            }
            it.remove()
        }
    }

}