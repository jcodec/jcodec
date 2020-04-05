package org.jcodec.codecs.h264.io.model

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * NAL unit type
 *
 * @author The JCodec project
 */
class NALUnitType private constructor(val value: Int, private val _name: String, val name: String) {
    companion object {
        @JvmField
        val NON_IDR_SLICE = NALUnitType(1, "NON_IDR_SLICE", "non IDR slice")
        val SLICE_PART_A = NALUnitType(2, "SLICE_PART_A", "slice part a")
        val SLICE_PART_B = NALUnitType(3, "SLICE_PART_B", "slice part b")
        val SLICE_PART_C = NALUnitType(4, "SLICE_PART_C", "slice part c")
        @JvmField
        val IDR_SLICE = NALUnitType(5, "IDR_SLICE", "idr slice")
        val SEI = NALUnitType(6, "SEI", "sei")
        @JvmField
        val SPS = NALUnitType(7, "SPS", "sequence parameter set")
        @JvmField
        val PPS = NALUnitType(8, "PPS", "picture parameter set")
        val ACC_UNIT_DELIM = NALUnitType(9, "ACC_UNIT_DELIM", "access unit delimiter")
        val END_OF_SEQ = NALUnitType(10, "END_OF_SEQ", "end of sequence")
        val END_OF_STREAM = NALUnitType(11, "END_OF_STREAM", "end of stream")
        val FILLER_DATA = NALUnitType(12, "FILLER_DATA", "filler data")
        val SEQ_PAR_SET_EXT = NALUnitType(13, "SEQ_PAR_SET_EXT", "sequence parameter set extension")
        val AUX_SLICE = NALUnitType(19, "AUX_SLICE", "auxilary slice")
        private val lut: Array<NALUnitType?>
        private val _values: Array<NALUnitType>
        fun fromValue(value: Int): NALUnitType? {
            return if (value < lut.size) lut[value] else null
        }

        init {
            _values = arrayOf(NON_IDR_SLICE, SLICE_PART_A, SLICE_PART_B, SLICE_PART_C, IDR_SLICE, SEI, SPS, PPS,
                    ACC_UNIT_DELIM, END_OF_SEQ, END_OF_STREAM, FILLER_DATA, SEQ_PAR_SET_EXT, AUX_SLICE)
            lut = arrayOfNulls(256)
            for (i in _values.indices) {
                val nalUnitType = _values[i]
                lut[nalUnitType.value] = nalUnitType
            }
        }
    }

    override fun toString(): String {
        return _name
    }

}