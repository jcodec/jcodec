package org.jcodec.containers.mp4.boxes

import org.jcodec.platform.Platform
import java.nio.ByteBuffer

//@formatter:off
/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Track fragment run
 *
 * To crate new box:
 *
 * <pre>
 *
 * Box box = TrunBox
 * .create(2)
 * .dataOffset(20)
 * .sampleCompositionOffset(new int[] { 11, 12 })
 * .sampleDuration(new int[] { 15, 16 })
 * .sampleFlags(new int[] { 100, 200 })
 * .sampleSize(new int[] { 30, 40 })
 * .create();
 *
</pre> *
 *
 * @author The JCodec project
 */
//@formatter:on
class TrunBox(header: Header) : FullBox(header) {
    // @formatter:on
    private var sampleCount = 0
    private var dataOffset: Long = 0
    var firstSampleFlags = 0
        private set
    var sampleDurations: IntArray = IntArray(0)
        private set
    var sampleSizes: IntArray = IntArray(0)
        private set
    var samplesFlags: IntArray = IntArray(0)
        private set
    var sampleCompositionOffsets: IntArray = IntArray(0)
        private set

    fun setDataOffset(dataOffset: Long) {
        this.dataOffset = dataOffset
    }

    class Factory(private var box: TrunBox?) {
        fun dataOffset(dataOffset: Long): Factory {
            box!!.flags = box!!.flags or DATA_OFFSET_AVAILABLE
            box!!.dataOffset = (dataOffset.toInt()).toLong()
            return this
        }

        fun firstSampleFlags(firstSampleFlags: Int): Factory {
            check(!box!!.isSampleFlagsAvailable) { "Sample flags already set on this object" }
            box!!.flags = box!!.flags or FIRST_SAMPLE_FLAGS_AVAILABLE
            box!!.firstSampleFlags = firstSampleFlags
            return this
        }

        fun sampleDuration(sampleDuration: IntArray): Factory {
            require(sampleDuration.size == box!!.sampleCount) { "Argument array length not equal to sampleCount" }
            box!!.flags = box!!.flags or SAMPLE_DURATION_AVAILABLE
            box!!.sampleDurations = sampleDuration
            return this
        }

        fun sampleSize(sampleSize: IntArray): Factory {
            require(sampleSize.size == box!!.sampleCount) { "Argument array length not equal to sampleCount" }
            box!!.flags = box!!.flags or SAMPLE_SIZE_AVAILABLE
            box!!.sampleSizes = sampleSize
            return this
        }

        fun sampleFlags(sampleFlags: IntArray): Factory {
            require(sampleFlags.size == box!!.sampleCount) { "Argument array length not equal to sampleCount" }
            check(!box!!.isFirstSampleFlagsAvailable) { "First sample flags already set on this object" }
            box!!.flags = box!!.flags or SAMPLE_FLAGS_AVAILABLE
            box!!.samplesFlags = sampleFlags
            return this
        }

        fun sampleCompositionOffset(sampleCompositionOffset: IntArray): Factory {
            require(sampleCompositionOffset.size == box!!.sampleCount) { "Argument array length not equal to sampleCount" }
            box!!.flags = box!!.flags or SAMPLE_COMPOSITION_OFFSET_AVAILABLE
            box!!.sampleCompositionOffsets = sampleCompositionOffset
            return this
        }

        fun create(): TrunBox? {
            return try {
                box
            } finally {
                box = null
            }
        }

    }

    fun getSampleCount(): Long {
        return Platform.unsignedInt(sampleCount)
    }

    fun getDataOffset(): Long {
        return dataOffset
    }

    fun getSampleDuration(i: Int): Long {
        return Platform.unsignedInt(sampleDurations[i])
    }

    fun getSampleSize(i: Int): Long {
        return Platform.unsignedInt(sampleSizes[i])
    }

    fun getSampleFlags(i: Int): Int {
        return samplesFlags[i]
    }

    fun getSampleCompositionOffset(i: Int): Long {
        return Platform.unsignedInt(sampleCompositionOffsets[i])
    }

    val isDataOffsetAvailable: Boolean
        get() = flags and DATA_OFFSET_AVAILABLE != 0

    val isSampleCompositionOffsetAvailable: Boolean
        get() = flags and SAMPLE_COMPOSITION_OFFSET_AVAILABLE != 0

    val isSampleFlagsAvailable: Boolean
        get() = flags and SAMPLE_FLAGS_AVAILABLE != 0

    val isSampleSizeAvailable: Boolean
        get() = flags and SAMPLE_SIZE_AVAILABLE != 0

    val isSampleDurationAvailable: Boolean
        get() = flags and SAMPLE_DURATION_AVAILABLE != 0

    val isFirstSampleFlagsAvailable: Boolean
        get() = flags and FIRST_SAMPLE_FLAGS_AVAILABLE != 0

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        if (isSampleFlagsAvailable && isFirstSampleFlagsAvailable) throw RuntimeException("Broken stream")
        sampleCount = input.int
        if (isDataOffsetAvailable) dataOffset = (input.int.toLong() and 0xffffffffL)
        if (isFirstSampleFlagsAvailable) firstSampleFlags = input.int
        if (isSampleDurationAvailable) sampleDurations = IntArray(sampleCount)
        if (isSampleSizeAvailable) sampleSizes = IntArray(sampleCount)
        if (isSampleFlagsAvailable) samplesFlags = IntArray(sampleCount)
        if (isSampleCompositionOffsetAvailable) sampleCompositionOffsets = IntArray(sampleCount)
        for (i in 0 until sampleCount) {
            if (isSampleDurationAvailable) sampleDurations[i] = input.int
            if (isSampleSizeAvailable) sampleSizes[i] = input.int
            if (isSampleFlagsAvailable) samplesFlags[i] = input.int
            if (isSampleCompositionOffsetAvailable) sampleCompositionOffsets[i] = input.int
        }
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(sampleCount)
        if (isDataOffsetAvailable) out.putInt(dataOffset.toInt())
        if (isFirstSampleFlagsAvailable) out.putInt(firstSampleFlags)
        for (i in 0 until sampleCount) {
            if (isSampleDurationAvailable) out.putInt(sampleDurations[i])
            if (isSampleSizeAvailable) out.putInt(sampleSizes[i])
            if (isSampleFlagsAvailable) out.putInt(samplesFlags[i])
            if (isSampleCompositionOffsetAvailable) out.putInt(sampleCompositionOffsets[i])
        }
    }

    override fun estimateSize(): Int {
        return 24 + sampleCount * 16
    }

    companion object {
        // @formatter:off
        private const val DATA_OFFSET_AVAILABLE = 0x000001
        private const val FIRST_SAMPLE_FLAGS_AVAILABLE = 0x000004
        private const val SAMPLE_DURATION_AVAILABLE = 0x000100
        private const val SAMPLE_SIZE_AVAILABLE = 0x000200
        private const val SAMPLE_FLAGS_AVAILABLE = 0x000400
        private const val SAMPLE_COMPOSITION_OFFSET_AVAILABLE = 0x000800
        @JvmStatic
        fun fourcc(): String {
            return "trun"
        }

        @JvmStatic
        fun create(sampleCount: Int): Factory {
            return Factory(createTrunBox1(sampleCount))
        }

        fun copy(other: TrunBox): Factory {
            val box = createTrunBox2(other.sampleCount, other.dataOffset, other.firstSampleFlags, other.sampleDurations, other.sampleSizes, other.samplesFlags, other.sampleCompositionOffsets)
            box.flags = other.flags
            box.version = other.version
            return Factory(box)
        }

        fun createTrunBox1(sampleCount: Int): TrunBox {
            val trun = TrunBox(Header(fourcc()))
            trun.sampleCount = sampleCount
            return trun
        }

        fun createTrunBox2(sampleCount: Int, dataOffset: Long, firstSampleFlags: Int, sampleDuration: IntArray,
                           sampleSize: IntArray, sampleFlags: IntArray, sampleCompositionOffset: IntArray): TrunBox {
            val trun = TrunBox(Header(fourcc()))
            trun.sampleCount = sampleCount
            trun.dataOffset = dataOffset
            trun.firstSampleFlags = firstSampleFlags
            trun.sampleDurations = sampleDuration
            trun.sampleSizes = sampleSize
            trun.samplesFlags = sampleFlags
            trun.sampleCompositionOffsets = sampleCompositionOffset
            return trun
        }

        fun flagsGetSampleDependsOn(flags: Int): Int {
            return flags shr 6 and 0x3
        }

        fun flagsGetSampleIsDependedOn(flags: Int): Int {
            return flags shr 8 and 0x3
        }

        fun flagsGetSampleHasRedundancy(flags: Int): Int {
            return flags shr 10 and 0x3
        }

        fun flagsGetSamplePaddingValue(flags: Int): Int {
            return flags shr 12 and 0x7
        }

        fun flagsGetSampleIsDifferentSample(flags: Int): Int {
            return flags shr 15 and 0x1
        }

        fun flagsGetSampleDegradationPriority(flags: Int): Int {
            return flags shr 16 and 0xffff
        }

        @JvmStatic
        fun createTrunBox(): TrunBox {
            return TrunBox(Header(fourcc()))
        }
    }
}