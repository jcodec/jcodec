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
class SourceClip(ul: UL?) : MXFStructuralComponent(ul) {
    var startPosition: Long = 0
        private set
    var sourceTrackId = 0
        private set
    var sourcePackageUid: UL? = null
        private set

    override fun read(tags: MutableMap<Int, ByteBuffer>) {
        super.read(tags)
        val it: MutableIterator<Map.Entry<Int, ByteBuffer>> = tags.entries.iterator()
        loop@ while (it.hasNext()) {
            val entry = it.next()
            val _bb = entry.value
            when (entry.key) {
                0x1201 -> startPosition = _bb.long
                0x1101 -> sourcePackageUid = read(_bb)
                0x1102 -> sourceTrackId = _bb.int
                else -> {
                    Logger.warn(String.format("Unknown tag [ $ul]: %04x", entry.key))
                    continue@loop
                }
            }
            it.remove()
        }
    }

}