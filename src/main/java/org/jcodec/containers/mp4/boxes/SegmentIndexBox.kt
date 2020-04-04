package org.jcodec.containers.mp4.boxes

import org.jcodec.platform.Platform
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class SegmentIndexBox(atom: Header) : FullBox(atom) {
    var reference_ID: Long = 0
    @JvmField
    var timescale: Long = 0
    var earliest_presentation_time: Long = 0
    var first_offset: Long = 0
    var reserved = 0
    @JvmField
    var reference_count = 0
    @JvmField
    var references: Array<Reference?> = emptyArray()

    class Reference {
        var reference_type = false
        var referenced_size: Long = 0
        var subsegment_duration: Long = 0
        @JvmField
        var starts_with_SAP = false
        var SAP_type = 0
        var SAP_delta_time: Long = 0
        override fun toString(): String {
            return ("Reference [reference_type=" + reference_type + ", referenced_size=" + referenced_size
                    + ", subsegment_duration=" + subsegment_duration + ", starts_with_SAP=" + starts_with_SAP
                    + ", SAP_type=" + SAP_type + ", SAP_delta_time=" + SAP_delta_time + "]")
        }
    }

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        reference_ID = Platform.unsignedInt(input.int)
        timescale = Platform.unsignedInt(input.int)
        if (version.toInt() == 0) {
            earliest_presentation_time = Platform.unsignedInt(input.int)
            first_offset = Platform.unsignedInt(input.int)
        } else {
            earliest_presentation_time = input.long
            first_offset = input.long
        }
        reserved = input.short.toInt()
        reference_count = input.short.toInt() and 0xffff
        references = arrayOfNulls(reference_count)
        for (i in 0 until reference_count) {
            val i0 = Platform.unsignedInt(input.int)
            val i1 = Platform.unsignedInt(input.int)
            val i2 = Platform.unsignedInt(input.int)
            val ref = Reference()
            ref.reference_type = i0 ushr 31 and 1 == 1L
            ref.referenced_size = i0 and 0x7fffffffL
            ref.subsegment_duration = i1
            ref.starts_with_SAP = i2 ushr 31 and 1 == 1L
            ref.SAP_type = (i2 ushr 28 and 7).toInt()
            ref.SAP_delta_time = i2 and 0xFFFFFFFL
            references[i] = ref
        }
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(reference_ID.toInt())
        out.putInt(timescale.toInt())
        if (version.toInt() == 0) {
            out.putInt(earliest_presentation_time.toInt())
            out.putInt(first_offset.toInt())
        } else {
            out.putLong(earliest_presentation_time)
            out.putLong(first_offset)
        }
        out.putShort(reserved.toShort())
        out.putShort(reference_count.toShort())
        for (i in 0 until reference_count) {
            val ref = references[i]
            val i1 = ref!!.subsegment_duration.toInt()
            var i2 = 0
            if (ref.starts_with_SAP) {
                i2 = i2 or (1 shl 31)
            }
            i2 = i2 or (ref.SAP_type and 7 shl 28)
            i2 = i2 or (ref.SAP_delta_time and 0xFFFFFFFL).toInt()
            out.putInt(((if (ref!!.reference_type) 1 else 0) shl 31 or ref.referenced_size.toInt()))
            out.putInt(i1)
            out.putInt(i2)
        }
    }

    override fun estimateSize(): Int {
        return 40 + reference_count * 12
    }

    override fun toString(): String {
        return ("SegmentIndexBox [reference_ID=" + reference_ID + ", timescale=" + timescale
                + ", earliest_presentation_time=" + earliest_presentation_time + ", first_offset=" + first_offset
                + ", reserved=" + reserved + ", reference_count=" + reference_count + ", references="
                + Platform.arrayToString(references) + ", version=" + version + ", flags=" + flags + ", header="
                + header + "]")
    }

    companion object {
        @JvmStatic
        fun createSegmentIndexBox(): SegmentIndexBox {
            return SegmentIndexBox(Header(fourcc()))
        }

        @JvmStatic
        fun fourcc(): String {
            return "sidx"
        }
    }
}