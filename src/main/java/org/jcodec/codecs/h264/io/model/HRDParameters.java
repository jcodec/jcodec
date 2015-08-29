package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class HRDParameters {

    public int cpb_cnt_minus1;
    public int bit_rate_scale;
    public int cpb_size_scale;
    public int[] bit_rate_value_minus1;
    public int[] cpb_size_value_minus1;
    public boolean[] cbr_flag;
    public int initial_cpb_removal_delay_length_minus1;
    public int cpb_removal_delay_length_minus1;
    public int dpb_output_delay_length_minus1;
    public int time_offset_length;

}
