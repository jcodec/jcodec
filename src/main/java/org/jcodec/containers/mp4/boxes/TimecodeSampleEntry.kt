package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Describes timecode payload sample
 *
 * @author The JCodec project
 */
class TimecodeSampleEntry(header: Header) : SampleEntry(header) {
    var flags = 0
        private set
    var timescale = 0
        private set
    var frameDuration = 0
        private set
    var numFrames: Byte = 0
        private set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        NIOUtils.skip(input, 4)
        flags = input.int
        timescale = input.int
        frameDuration = input.int
        numFrames = input.get()
        NIOUtils.skip(input, 1)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(0)
        out.putInt(flags)
        out.putInt(timescale)
        out.putInt(frameDuration)
        out.put(numFrames)
        out.put(207.toByte())
    }

    val isDropFrame: Boolean
        get() = flags and FLAG_DROPFRAME != 0

    companion object {
        private const val TMCD = "tmcd"

        //@formatter:off
        const val FLAG_DROPFRAME = 0x1
        const val FLAG_24HOURMAX = 0x2
        const val FLAG_NEGATIVETIMEOK = 0x4
        const val FLAG_COUNTER = 0x8

        //@formatter:on
        @JvmStatic
        fun createTimecodeSampleEntry(flags: Int, timescale: Int, frameDuration: Int,
                                      numFrames: Int): TimecodeSampleEntry {
            val tmcd = TimecodeSampleEntry(Header(TMCD))
            tmcd.flags = flags
            tmcd.timescale = timescale
            tmcd.frameDuration = frameDuration
            tmcd.numFrames = numFrames.toByte()
            return tmcd
        }
    }
}