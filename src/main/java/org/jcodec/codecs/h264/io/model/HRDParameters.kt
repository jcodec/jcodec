package org.jcodec.codecs.h264.io.model

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
class HRDParameters {
    // cpb_cnt_minus1
    @JvmField
    var cpbCntMinus1 = 0

    // bit_rate_scale
    @JvmField
    var bitRateScale = 0

    // cpb_size_scale
    @JvmField
    var cpbSizeScale = 0

    // bit_rate_value_minus1
    @JvmField
    var bitRateValueMinus1: IntArray? = null

    // cpb_size_value_minus1
    @JvmField
    var cpbSizeValueMinus1: IntArray? = null

    // cbr_flag
    @JvmField
    var cbrFlag: BooleanArray? = null

    // initial_cpb_removal_delay_length_minus1
    @JvmField
    var initialCpbRemovalDelayLengthMinus1 = 0

    // cpb_removal_delay_length_minus1
    @JvmField
    var cpbRemovalDelayLengthMinus1 = 0

    // dpb_output_delay_length_minus1
    @JvmField
    var dpbOutputDelayLengthMinus1 = 0

    // time_offset_length
    @JvmField
    var timeOffsetLength = 0
}