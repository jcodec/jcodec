package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class HRDParameters {

    // cpb_cnt_minus1
    public int cpbCntMinus1;
    // bit_rate_scale
    public int bitRateScale;
    // cpb_size_scale
    public int cpbSizeScale;
    // bit_rate_value_minus1
    public int[] bitRateValueMinus1;
    // cpb_size_value_minus1
    public int[] cpbSizeValueMinus1;
    // cbr_flag
    public boolean[] cbrFlag;
    // initial_cpb_removal_delay_length_minus1
    public int initialCpbRemovalDelayLengthMinus1;
    // cpb_removal_delay_length_minus1
    public int cpbRemovalDelayLengthMinus1;
    // dpb_output_delay_length_minus1
    public int dpbOutputDelayLengthMinus1;
    // time_offset_length
    public int timeOffsetLength;

}
