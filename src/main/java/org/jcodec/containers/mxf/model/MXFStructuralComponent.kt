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
open class MXFStructuralComponent(ul: UL?) : MXFInterchangeObject(ul) {
    var duration: Long = 0
        private set
    var dataDefinitionUL: UL? = null
        private set

    override fun read(tags: MutableMap<Int, ByteBuffer>) {
        val it: MutableIterator<Map.Entry<Int, ByteBuffer>> = tags.entries.iterator()
        loop@ while (it.hasNext()) {
            val entry = it.next()
            when (entry.key) {
                0x0202 -> duration = entry.value.long
                0x0201 -> dataDefinitionUL = read(entry.value)
                else -> {
                    Logger.warn(String.format("Unknown tag [ $ul]: %04x", entry.key))
                    continue@loop
                }
            }
            it.remove()
        }
    }

}