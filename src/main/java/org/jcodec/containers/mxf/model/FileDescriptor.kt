package org.jcodec.containers.mxf.model

import org.jcodec.common.logging.Logger
import org.jcodec.common.model.Rational
import org.jcodec.containers.mxf.model.UL.Companion.read
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class FileDescriptor(ul: UL?) : GenericDescriptor(ul) {
    var linkedTrackId = 0
        private set
    var sampleRate: Rational? = null
        private set
    var containerDuration: Long = 0
        private set
    var essenceContainer: UL? = null
        private set
    var codec: UL? = null
        private set

    override fun read(tags: MutableMap<Int, ByteBuffer>) {
        super.read(tags)
        val it: MutableIterator<Map.Entry<Int, ByteBuffer>> = tags.entries.iterator()
        loop@ while (it.hasNext()) {
            val entry = it.next()
            val _bb = entry.value
            when (entry.key) {
                0x3006 -> linkedTrackId = _bb.int
                0x3001 -> sampleRate = Rational(_bb.int, _bb.int)
                0x3002 -> containerDuration = _bb.long
                0x3004 -> essenceContainer = read(_bb)
                0x3005 -> codec = read(_bb)
                else -> {
                    Logger.warn(String.format("Unknown tag [ $ul]: %04x", entry.key))
                    continue@loop
                }
            }
            it.remove()
        }
    }

}