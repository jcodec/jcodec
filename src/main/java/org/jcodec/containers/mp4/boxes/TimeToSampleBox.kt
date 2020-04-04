package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A box containing sample presentation time information
 *
 * @author The JCodec project
 */
class TimeToSampleBox(atom: Header) : FullBox(atom) {
    class TimeToSampleEntry(var sampleCount: Int, var sampleDuration: Int) {

        val segmentDuration: Long
            get() = (sampleCount * sampleDuration).toLong()

    }

    private var entries: Array<TimeToSampleEntry?> = emptyArray()
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        val foo = input.int
        entries = arrayOfNulls(foo)
        for (i in 0 until foo) {
            entries[i] = TimeToSampleEntry(input.int, input.int)
        }
    }

    fun getEntries(): Array<TimeToSampleEntry?> {
        return entries
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(entries.size)
        for (i in entries.indices) {
            val timeToSampleEntry = entries[i]
            out.putInt(timeToSampleEntry!!.sampleCount)
            out.putInt(timeToSampleEntry.sampleDuration)
        }
    }

    override fun estimateSize(): Int {
        return 16 + entries.size * 8
    }

    fun setEntries(entries: Array<TimeToSampleEntry?>) {
        this.entries = entries
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "stts"
        }

        @JvmStatic
        fun createTimeToSampleBox(timeToSamples: Array<TimeToSampleEntry?>): TimeToSampleBox {
            val box = TimeToSampleBox(Header(fourcc()))
            box.entries = timeToSamples
            return box
        }
    }
}