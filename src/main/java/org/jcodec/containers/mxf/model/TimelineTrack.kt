package org.jcodec.containers.mxf.model

import org.jcodec.common.logging.Logger
import org.jcodec.common.model.Rational
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class TimelineTrack(ul: UL?) : GenericTrack(ul) {
    var editRate: Rational? = null
        private set
    var origin: Long = 0
        private set

    override fun read(tags: MutableMap<Int, ByteBuffer>) {
        super.read(tags)
        val it: MutableIterator<Map.Entry<Int, ByteBuffer>> = tags.entries.iterator()
        loop@ while (it.hasNext()) {
            val entry = it.next()
            val _bb = entry.value
            when (entry.key) {
                0x4B01 -> editRate = Rational(_bb.int, _bb.int)
                0x4b02 -> origin = _bb.long
                else -> {
                    Logger.warn(String.format("Unknown tag [ $ul]: %04x", entry.key))
                    continue@loop
                }
            }
            it.remove()
        }
    }

}