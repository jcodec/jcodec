package org.jcodec.common.model

import org.jcodec.common.StringUtils

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Tape timecode
 *
 * @author The JCodec project
 */
class TapeTimecode(val hour: Short, val minute: Byte, val second: Byte, val frame: Byte, val isDropFrame: Boolean, val tapeFps: Int) {

    override fun toString(): String {
        return StringUtils.zeroPad2(hour.toInt()) + ":" +
                StringUtils.zeroPad2(minute.toInt()) + ":" +
                StringUtils.zeroPad2(second.toInt()) + (if (isDropFrame) ";" else ":") +
                StringUtils.zeroPad2(frame.toInt())
    }

    companion object {
        @JvmField
        val ZERO_TAPE_TIMECODE = TapeTimecode(0.toShort(), 0.toByte(), 0.toByte(), 0.toByte(), false, 0)
        @JvmStatic
        fun tapeTimecode(frame: Long, dropFrame: Boolean, tapeFps: Int): TapeTimecode {
            var frame = frame
            if (dropFrame) {
                val D = frame / 17982
                val M = frame % 17982
                frame += 18 * D + 2 * ((M - 2) / 1798)
            }
            val sec = frame / tapeFps
            return TapeTimecode((sec / 3600).toShort(), (sec / 60 % 60).toByte(), (sec % 60).toByte(),
                    (frame % tapeFps).toByte(), dropFrame, tapeFps)
        }
    }

}