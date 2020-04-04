package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Track fragment header box
 *
 * @author The JCodec project
 */
class TrackFragmentHeaderBox(atom: Header) : FullBox(atom) {
    //@formatter:on
    private var trackId = 0
    var baseDataOffset: Long = 0
        private set
    var sampleDescriptionIndex = 0
        private set
    var defaultSampleDuration = 0
        private set
    var defaultSampleSize = 0
        private set
    private var defaultSampleFlags = 0

    class Factory(private var box: TrackFragmentHeaderBox?) {
        fun baseDataOffset(baseDataOffset: Long): Factory {
            box!!.flags = box!!.flags or FLAG_BASE_DATA_OFFSET
            box!!.baseDataOffset = baseDataOffset
            return this
        }

        fun sampleDescriptionIndex(sampleDescriptionIndex: Long): Factory {
            box!!.flags = box!!.flags or FLAG_SAMPLE_DESCRIPTION_INDEX
            box!!.sampleDescriptionIndex = sampleDescriptionIndex.toInt()
            return this
        }

        fun defaultSampleDuration(defaultSampleDuration: Long): Factory {
            box!!.flags = box!!.flags or FLAG_DEFAILT_SAMPLE_DURATION
            box!!.defaultSampleDuration = defaultSampleDuration.toInt()
            return this
        }

        fun defaultSampleSize(defaultSampleSize: Long): Factory {
            box!!.flags = box!!.flags or FLAG_DEFAULT_SAMPLE_SIZE
            box!!.defaultSampleSize = defaultSampleSize.toInt()
            return this
        }

        fun defaultSampleFlags(defaultSampleFlags: Long): Factory {
            box!!.flags = box!!.flags or FLAG_DEFAILT_SAMPLE_FLAGS
            box!!.defaultSampleFlags = defaultSampleFlags.toInt()
            return this
        }

        fun create(): TrackFragmentHeaderBox? {
            return try {
                box
            } finally {
                box = null
            }
        }

    }

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        trackId = input.int
        if (isBaseDataOffsetAvailable) baseDataOffset = input.long
        if (isSampleDescriptionIndexAvailable) sampleDescriptionIndex = input.int
        if (isDefaultSampleDurationAvailable) defaultSampleDuration = input.int
        if (isDefaultSampleSizeAvailable) defaultSampleSize = input.int
        if (isDefaultSampleFlagsAvailable) defaultSampleFlags = input.int
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(trackId)
        if (isBaseDataOffsetAvailable) out.putLong(baseDataOffset)
        if (isSampleDescriptionIndexAvailable) out.putInt(sampleDescriptionIndex)
        if (isDefaultSampleDurationAvailable) out.putInt(defaultSampleDuration)
        if (isDefaultSampleSizeAvailable) out.putInt(defaultSampleSize)
        if (isDefaultSampleFlagsAvailable) out.putInt(defaultSampleFlags)
    }

    override fun estimateSize(): Int {
        return 40
    }

    fun getTrackId(): Int {
        return trackId
    }

    fun getDefaultSampleFlags(): Int {
        return defaultSampleFlags
    }

    val isBaseDataOffsetAvailable: Boolean
        get() = flags and FLAG_BASE_DATA_OFFSET != 0

    val isSampleDescriptionIndexAvailable: Boolean
        get() = flags and FLAG_SAMPLE_DESCRIPTION_INDEX != 0

    val isDefaultSampleDurationAvailable: Boolean
        get() = flags and FLAG_DEFAILT_SAMPLE_DURATION != 0

    val isDefaultSampleSizeAvailable: Boolean
        get() = flags and FLAG_DEFAULT_SAMPLE_SIZE != 0

    val isDefaultSampleFlagsAvailable: Boolean
        get() = flags and FLAG_DEFAILT_SAMPLE_FLAGS != 0

    fun setTrackId(trackId: Int) {
        this.trackId = trackId
    }

    fun setDefaultSampleFlags(defaultSampleFlags: Int) {
        this.defaultSampleFlags = defaultSampleFlags
    }

    companion object {
        //@formatter:off
        const val FLAG_BASE_DATA_OFFSET = 0x01
        const val FLAG_SAMPLE_DESCRIPTION_INDEX = 0x02
        const val FLAG_DEFAILT_SAMPLE_DURATION = 0x08
        const val FLAG_DEFAULT_SAMPLE_SIZE = 0x10
        const val FLAG_DEFAILT_SAMPLE_FLAGS = 0x20
        @JvmStatic
        fun fourcc(): String {
            return "tfhd"
        }

        fun tfhd(trackId: Int, baseDataOffset: Long, sampleDescriptionIndex: Int, defaultSampleDuration: Int, defaultSampleSize: Int, defaultSampleFlags: Int): TrackFragmentHeaderBox {
            val box = TrackFragmentHeaderBox(Header(fourcc()))
            box.trackId = trackId
            box.baseDataOffset = baseDataOffset
            box.sampleDescriptionIndex = sampleDescriptionIndex
            box.defaultSampleDuration = defaultSampleDuration
            box.defaultSampleSize = defaultSampleSize
            box.defaultSampleFlags = defaultSampleFlags
            return box
        }

        fun create(trackId: Int): Factory {
            return Factory(createTrackFragmentHeaderBoxWithId(trackId))
        }

        fun copy(other: TrackFragmentHeaderBox): Factory {
            val box = tfhd(other.trackId, other.baseDataOffset, other.sampleDescriptionIndex, other.defaultSampleDuration, other.defaultSampleSize, other.defaultSampleFlags)
            box.flags = other.flags
            box.version = other.version
            return Factory(box)
        }

        fun createTrackFragmentHeaderBoxWithId(trackId: Int): TrackFragmentHeaderBox {
            val box = TrackFragmentHeaderBox(Header(fourcc()))
            box.trackId = trackId
            return box
        }

        fun createTrackFragmentHeaderBox(): TrackFragmentHeaderBox {
            return TrackFragmentHeaderBox(Header(fourcc()))
        }
    }
}