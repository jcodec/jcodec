package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
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
    
    public int getCpb_cnt_minus1() {
        return cpb_cnt_minus1;
    }
    public int getBit_rate_scale() {
        return bit_rate_scale;
    }
    public int getCpb_size_scale() {
        return cpb_size_scale;
    }
    public int[] getBit_rate_value_minus1() {
        return bit_rate_value_minus1;
    }
    public int[] getCpb_size_value_minus1() {
        return cpb_size_value_minus1;
    }
    public boolean[] getCbr_flag() {
        return cbr_flag;
    }
    public int getInitial_cpb_removal_delay_length_minus1() {
        return initial_cpb_removal_delay_length_minus1;
    }
    public int getCpb_removal_delay_length_minus1() {
        return cpb_removal_delay_length_minus1;
    }
    public int getDpb_output_delay_length_minus1() {
        return dpb_output_delay_length_minus1;
    }
    public int getTime_offset_length() {
        return time_offset_length;
    }
}
