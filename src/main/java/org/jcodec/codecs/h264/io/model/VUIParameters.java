package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class VUIParameters {

    public static class BitstreamRestriction {

        public boolean motion_vectors_over_pic_boundaries_flag;
        public int max_bytes_per_pic_denom;
        public int max_bits_per_mb_denom;
        public int log2_max_mv_length_horizontal;
        public int log2_max_mv_length_vertical;
        public int num_reorder_frames;
        public int max_dec_frame_buffering;

    }

    public boolean aspect_ratio_info_present_flag;
    public int sar_width;
    public int sar_height;
    public boolean overscan_info_present_flag;
    public boolean overscan_appropriate_flag;
    public boolean video_signal_type_present_flag;
    public int video_format;
    public boolean video_full_range_flag;
    public boolean colour_description_present_flag;
    public int colour_primaries;
    public int transfer_characteristics;
    public int matrix_coefficients;
    public boolean chroma_loc_info_present_flag;
    public int chroma_sample_loc_type_top_field;
    public int chroma_sample_loc_type_bottom_field;
    public boolean timing_info_present_flag;
    public int num_units_in_tick;
    public int time_scale;
    public boolean fixed_frame_rate_flag;
    public boolean low_delay_hrd_flag;
    public boolean pic_struct_present_flag;
    public HRDParameters nalHRDParams;
    public HRDParameters vclHRDParams;

    public BitstreamRestriction bitstreamRestriction;
    public AspectRatio aspect_ratio;
    public boolean isAspect_ratio_info_present_flag() {
        return aspect_ratio_info_present_flag;
    }
    public int getSar_width() {
        return sar_width;
    }
    public int getSar_height() {
        return sar_height;
    }
    public boolean isOverscan_info_present_flag() {
        return overscan_info_present_flag;
    }
    public boolean isOverscan_appropriate_flag() {
        return overscan_appropriate_flag;
    }
    public boolean isVideo_signal_type_present_flag() {
        return video_signal_type_present_flag;
    }
    public int getVideo_format() {
        return video_format;
    }
    public boolean isVideo_full_range_flag() {
        return video_full_range_flag;
    }
    public boolean isColour_description_present_flag() {
        return colour_description_present_flag;
    }
    public int getColour_primaries() {
        return colour_primaries;
    }
    public int getTransfer_characteristics() {
        return transfer_characteristics;
    }
    public int getMatrix_coefficients() {
        return matrix_coefficients;
    }
    public boolean isChroma_loc_info_present_flag() {
        return chroma_loc_info_present_flag;
    }
    public int getChroma_sample_loc_type_top_field() {
        return chroma_sample_loc_type_top_field;
    }
    public int getChroma_sample_loc_type_bottom_field() {
        return chroma_sample_loc_type_bottom_field;
    }
    public boolean isTiming_info_present_flag() {
        return timing_info_present_flag;
    }
    public int getNum_units_in_tick() {
        return num_units_in_tick;
    }
    public int getTime_scale() {
        return time_scale;
    }
    public boolean isFixed_frame_rate_flag() {
        return fixed_frame_rate_flag;
    }
    public boolean isLow_delay_hrd_flag() {
        return low_delay_hrd_flag;
    }
    public boolean isPic_struct_present_flag() {
        return pic_struct_present_flag;
    }
    public HRDParameters getNalHRDParams() {
        return nalHRDParams;
    }
    public HRDParameters getVclHRDParams() {
        return vclHRDParams;
    }
    public BitstreamRestriction getBitstreamRestriction() {
        return bitstreamRestriction;
    }
    public AspectRatio getAspect_ratio() {
        return aspect_ratio;
    }
}
