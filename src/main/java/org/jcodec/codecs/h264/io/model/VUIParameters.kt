package org.jcodec.codecs.h264.io.model

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
class VUIParameters {
    class BitstreamRestriction {
        // motion_vectors_over_pic_boundaries_flag
        @JvmField
        var motionVectorsOverPicBoundariesFlag = false

        // max_bytes_per_pic_denom
        @JvmField
        var maxBytesPerPicDenom = 0

        // max_bits_per_mb_denom
        @JvmField
        var maxBitsPerMbDenom = 0

        // log2_max_mv_length_horizontal
        @JvmField
        var log2MaxMvLengthHorizontal = 0

        // log2_max_mv_length_vertical
        @JvmField
        var log2MaxMvLengthVertical = 0

        // num_reorder_frames
        @JvmField
        var numReorderFrames = 0

        // max_dec_frame_buffering
        @JvmField
        var maxDecFrameBuffering = 0
    }

    // boolean aspect_ratio_info_present_flag
    @JvmField
    var aspectRatioInfoPresentFlag = false

    // int sar_width
    var sarWidth = 0

    // int sar_height
    var sarHeight = 0

    // overscan_info_present_flag
    @JvmField
    var overscanInfoPresentFlag = false

    // overscan_appropriate_flag;
    var overscanAppropriateFlag = false

    // video_signal_type_present_flag;
    @JvmField
    var videoSignalTypePresentFlag = false

    // video_format
    var videoFormat = 0

    // video_full_range_flag
    var videoFullRangeFlag = false

    // colour_description_present_flag
    var colourDescriptionPresentFlag = false

    // colour_primaries
    var colourPrimaries = 0

    // transfer_characteristics
    var transferCharacteristics = 0

    // matrix_coefficients
    var matrixCoefficients = 0

    // chroma_loc_info_present_flag
    @JvmField
    var chromaLocInfoPresentFlag = false

    // chroma_sample_loc_type_top_field
    var chromaSampleLocTypeTopField = 0

    // chroma_sample_loc_type_bottom_field
    var chromaSampleLocTypeBottomField = 0

    // timing_info_present_flag
    @JvmField
    var timingInfoPresentFlag = false

    // num_units_in_tick
    @JvmField
    var numUnitsInTick = 0

    // time_scale
    @JvmField
    var timeScale = 0

    // fixed_frame_rate_flag
    @JvmField
    var fixedFrameRateFlag = false

    // low_delay_hrd_flag
    var lowDelayHrdFlag = false

    // pic_struct_present_flag
    @JvmField
    var picStructPresentFlag = false
    @JvmField
    var nalHRDParams: HRDParameters? = null
    @JvmField
    var vclHRDParams: HRDParameters? = null
    @JvmField
    var bitstreamRestriction: BitstreamRestriction? = null

    // aspect_ratio
    var aspectRatio: AspectRatio? = null
}