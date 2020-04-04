package org.jcodec.containers.mp4.boxes

import org.jcodec.containers.mp4.TimeUtil
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A media header atom
 *
 * @author The JCodec project
 */
class MediaHeaderBox(atom: Header) : FullBox(atom) {
    var created: Long = 0
        private set
    var modified: Long = 0
        private set
    private var timescale = 0
    private var duration: Long = 0
    var language = 0
        private set
    var quality = 0
        private set

    fun getTimescale(): Int {
        return timescale
    }

    fun getDuration(): Long {
        return duration
    }

    fun setDuration(duration: Long) {
        this.duration = duration
    }

    fun setTimescale(timescale: Int) {
        this.timescale = timescale
    }

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        if (version.toInt() == 0) {
            created = TimeUtil.fromMovTime(input.int)
            modified = TimeUtil.fromMovTime(input.int)
            timescale = input.int
            duration = input.int.toLong()
        } else if (version.toInt() == 1) {
            created = TimeUtil.fromMovTime(input.long.toInt())
            modified = TimeUtil.fromMovTime(input.long.toInt())
            timescale = input.int
            duration = input.long
        } else {
            throw RuntimeException("Unsupported version")
        }
        language = input.short.toInt()
        quality = input.short.toInt()
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        if (version.toInt() == 0) {
            out.putInt(TimeUtil.toMovTime(created))
            out.putInt(TimeUtil.toMovTime(modified))
            out.putInt(timescale)
            out.putInt(duration.toInt())
        } else if (version.toInt() == 1) {
            out.putLong(TimeUtil.toMovTime(created).toLong())
            out.putLong(TimeUtil.toMovTime(modified).toLong())
            out.putInt(timescale)
            out.putLong(duration)
        }
        out.putShort(language.toShort())
        out.putShort(quality.toShort())
    }

    override fun estimateSize(): Int {
        return 32
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "mdhd"
        }

        @JvmStatic
        fun createMediaHeaderBox(timescale: Int, duration: Long, language: Int, created: Long,
                                 modified: Long, quality: Int): MediaHeaderBox {
            val mdhd = MediaHeaderBox(Header(fourcc()))
            mdhd.timescale = timescale
            mdhd.duration = duration
            mdhd.language = language
            mdhd.created = created
            mdhd.modified = modified
            mdhd.quality = quality
            return mdhd
        }
    }
}