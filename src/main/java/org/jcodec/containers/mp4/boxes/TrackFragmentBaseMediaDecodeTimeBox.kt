package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * The Track Fragment Base Media Decode Time Box provides the absolute decode
 * time, measured on the media timeline, of the first sample in decode order in
 * the track fragment. This can be useful, for example, when performing random
 * access in a file; it is not necessary to sum the sample durations of all
 * preceding samples in previous fragments to find this value (where the sample
 * durations are the deltas in the Decoding Time to Sample Box and the
 * sample_durations in the preceding track runs). The Track Fragment Base Media
 * Decode Time Box, if present, shall be positioned after the Track Fragment
 * Header Box and before the first Track Fragment Run box.
 *
 * @author The JCodec project
 */
class TrackFragmentBaseMediaDecodeTimeBox(atom: Header) : FullBox(atom) {
    private var baseMediaDecodeTime: Long = 0
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        baseMediaDecodeTime = if (version.toInt() == 0) {
            input.int.toLong()
        } else if (version.toInt() == 1) {
            input.long
        } else throw RuntimeException("Unsupported tfdt version")
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        if (version.toInt() == 0) {
            out.putInt(baseMediaDecodeTime.toInt())
        } else if (version.toInt() == 1) {
            out.putLong(baseMediaDecodeTime)
        } else throw RuntimeException("Unsupported tfdt version")
    }

    override fun estimateSize(): Int {
        return 20
    }

    fun getBaseMediaDecodeTime(): Long {
        return baseMediaDecodeTime
    }

    fun setBaseMediaDecodeTime(baseMediaDecodeTime: Long) {
        this.baseMediaDecodeTime = baseMediaDecodeTime
    }

    class Factory(other: TrackFragmentBaseMediaDecodeTimeBox) {
        private var box: TrackFragmentBaseMediaDecodeTimeBox?
        fun baseMediaDecodeTime(`val`: Long): Factory {
            box!!.baseMediaDecodeTime = `val`
            return this
        }

        fun create(): TrackFragmentBaseMediaDecodeTimeBox? {
            return try {
                box
            } finally {
                box = null
            }
        }

        init {
            box = createTrackFragmentBaseMediaDecodeTimeBox(other.baseMediaDecodeTime)
            box!!.version = other.version
            box!!.flags = other.flags
        }
    }

    companion object {
        fun createTrackFragmentBaseMediaDecodeTimeBox(
                baseMediaDecodeTime: Long): TrackFragmentBaseMediaDecodeTimeBox {
            val box = TrackFragmentBaseMediaDecodeTimeBox(Header(fourcc()))
            box.baseMediaDecodeTime = baseMediaDecodeTime
            if (box.baseMediaDecodeTime > Int.MAX_VALUE) {
                box.version = 1
            }
            return box
        }

        @JvmStatic
        fun fourcc(): String {
            return "tfdt"
        }

        fun copy(other: TrackFragmentBaseMediaDecodeTimeBox): Factory {
            return Factory(other)
        }
    }
}