package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.Box
import java.util.*

abstract class Boxes {
    @JvmField
    protected val mappings: MutableMap<String, Class<out Box?>>
    fun toClass(fourcc: String): Class<out Box?>? {
        return mappings[fourcc]
    }

    fun override(fourcc: String, cls: Class<out Box?>) {
        mappings[fourcc] = cls
    }

    fun clear() {
        mappings.clear()
    }

    init {
        mappings = HashMap()
    }
}