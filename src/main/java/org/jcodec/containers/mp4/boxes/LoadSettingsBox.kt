package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Load setting atom
 *
 * @author The JCodec project
 */
class LoadSettingsBox(header: Header) : Box(header) {
    var preloadStartTime = 0
        private set
    var preloadDuration = 0
        private set
    var preloadFlags = 0
        private set
    var defaultHints = 0
        private set

    override fun parse(input: ByteBuffer) {
        preloadStartTime = input.int
        preloadDuration = input.int
        preloadFlags = input.int
        defaultHints = input.int
    }

    override fun doWrite(out: ByteBuffer) {
        out.putInt(preloadStartTime)
        out.putInt(preloadDuration)
        out.putInt(preloadFlags)
        out.putInt(defaultHints)
    }

    override fun estimateSize(): Int {
        return 24
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "load"
        }
    }
}