package org.jcodec.containers.mxf.model

import org.jcodec.common.logging.Logger
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class TimecodeComponent(ul: UL?) : MXFStructuralComponent(ul) {
    var start: Long = 0
        private set
    var base = 0
        private set
    var dropFrame = 0
        private set

    override fun read(tags: MutableMap<Int, ByteBuffer>) {
        super.read(tags)
        val it: MutableIterator<Map.Entry<Int, ByteBuffer>> = tags.entries.iterator()
        loop@ while (it.hasNext()) {
            val entry = it.next()
            val _bb = entry.value
            when (entry.key) {
                0x1501 -> start = _bb.long
                0x1502 -> base = _bb.short.toInt()
                0x1503 -> dropFrame = _bb.get().toInt()
                else -> {
                    Logger.warn(String.format("Unknown tag [ $ul]: %04x", entry.key))
                    continue@loop
                }
            }
            it.remove()
        }
    }

}