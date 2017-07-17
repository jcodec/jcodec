package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 */
public class VUIParameters {

    public static class BitstreamRestriction {
        // motion_vectors_over_pic_boundaries_flag
        public boolean motionVectorsOverPicBoundariesFlag;
        // max_bytes_per_pic_denom
        public int maxBytesPerPicDenom;
        // max_bits_per_mb_denom
        public int maxBitsPerMbDenom;
        // log2_max_mv_length_horizontal
        public int log2MaxMvLengthHorizontal;
        // log2_max_mv_length_vertical
        public int log2MaxMvLengthVertical;
        // num_reorder_frames
        public int numReorderFrames;
        // max_dec_frame_buffering
        public int maxDecFrameBuffering;
    }

    // boolean aspect_ratio_info_present_flag
    public boolean aspectRatioInfoPresentFlag;
    // int sar_width
    public int sarWidth;
    // int sar_height
    public int sarHeight;
    // overscan_info_present_flag
    public boolean overscanInfoPresentFlag;
    // overscan_appropriate_flag;
    public boolean overscanAppropriateFlag;
    // video_signal_type_present_flag;
    public boolean videoSignalTypePresentFlag;
    // video_format
    public int videoFormat;
    // video_full_range_flag
    public boolean videoFullRangeFlag;
    // colour_description_present_flag
    public boolean colourDescriptionPresentFlag;
    // colour_primaries
    public int colourPrimaries;
    // transfer_characteristics
    public int transferCharacteristics;
    // matrix_coefficients
    public int matrixCoefficients;
    // chroma_loc_info_present_flag
    public boolean chromaLocInfoPresentFlag;
    // chroma_sample_loc_type_top_field
    public int chromaSampleLocTypeTopField;
    // chroma_sample_loc_type_bottom_field
    public int chromaSampleLocTypeBottomField;
    // timing_info_present_flag
    public boolean timingInfoPresentFlag;
    // num_units_in_tick
    public int numUnitsInTick;
    // time_scale
    public int timeScale;
    // fixed_frame_rate_flag
    public boolean fixedFrameRateFlag;
    // low_delay_hrd_flag
    public boolean lowDelayHrdFlag;
    // pic_struct_present_flag
    public boolean picStructPresentFlag;
    public HRDParameters nalHRDParams;
    public HRDParameters vclHRDParams;

    public BitstreamRestriction bitstreamRestriction;
    // aspect_ratio
    public AspectRatio aspectRatio;

}
