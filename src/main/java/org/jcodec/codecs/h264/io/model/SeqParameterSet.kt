package org.jcodec.codecs.h264.io.model

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.decode.CAVLCReader
import org.jcodec.codecs.h264.io.model.AspectRatio.Companion.fromValue
import org.jcodec.codecs.h264.io.model.VUIParameters.BitstreamRestriction
import org.jcodec.codecs.h264.io.write.CAVLCWriter
import org.jcodec.common.io.BitReader
import org.jcodec.common.io.BitWriter
import org.jcodec.common.model.ColorSpace
import org.jcodec.platform.Platform
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Sequence Parameter Set structure of h264 bitstream
 *
 * capable to serialize and deserialize with CAVLC bitstream
 *
 * @author The JCodec project
 */
class SeqParameterSet {
    // pic_order_cnt_type
    @JvmField
    var picOrderCntType = 0

    // field_pic_flag
    @JvmField
    var isFieldPicFlag = false

    // delta_pic_order_always_zero_flag
    @JvmField
    var isDeltaPicOrderAlwaysZeroFlag = false

    // mb_adaptive_frame_field_flag
    @JvmField
    var isMbAdaptiveFrameFieldFlag = false

    // direct_8x8_inference_flag
    @JvmField
    var isDirect8x8InferenceFlag = false

    // chroma_format_idc
    @JvmField
    var chromaFormatIdc: ColorSpace? = null

    // log2_max_frame_num_minus4
    @JvmField
    var log2MaxFrameNumMinus4 = 0

    // log2_max_pic_order_cnt_lsb_minus4
    @JvmField
    var log2MaxPicOrderCntLsbMinus4 = 0

    // pic_height_in_map_units_minus1
    @JvmField
    var picHeightInMapUnitsMinus1 = 0

    // pic_width_in_mbs_minus1
    @JvmField
    var picWidthInMbsMinus1 = 0

    // bit_depth_luma_minus8
    @JvmField
    var bitDepthLumaMinus8 = 0

    // bit_depth_chroma_minus8
    @JvmField
    var bitDepthChromaMinus8 = 0

    // qpprime_y_zero_transform_bypass_flag
    @JvmField
    var isQpprimeYZeroTransformBypassFlag = false

    // profile_idc
    @JvmField
    var profileIdc = 0

    // constraint_set0_flag
    @JvmField
    var isConstraintSet0Flag = false

    // constraint_set1_flag
    @JvmField
    var isConstraintSet1Flag = false

    // constraint_set2_flag
    @JvmField
    var isConstraintSet2Flag = false

    // constraint_set_3_flag
    @JvmField
    var isConstraintSet3Flag = false

    // constraint_set_4_flag
    @JvmField
    var isConstraintSet4Flag = false

    // constraint_set_5_flag
    @JvmField
    var isConstraintSet5Flag = false

    // level_idc
    @JvmField
    var levelIdc = 0

    // seq_parameter_set_id
    @JvmField
    var seqParameterSetId = 0

    /**
     * separate_colour_plane_flag. When a picture is coded using three separate
     * colour planes (separate_colour_plane_flag is equal to 1), a slice
     * contains only macroblocks of one colour component being identified by the
     * corresponding value of colour_plane_id, and each colour component array
     * of a picture consists of slices having the same colour_plane_id value.
     * Coded slices with different values of colour_plane_id within an access
     * unit can be interleaved with each other under the constraint that for
     * each value of colour_plane_id, the coded slice NAL units with that value
     * colour_plane_id shall be in the order of increasing macroblock address
     * for the first macroblock of each coded slice NAL unit.
     */
    var isResidualColorTransformFlag = false

    /**
     * offset_for_non_ref_pic is used to calculate the picture order count of a
     * non-reference picture as specified in 8.2.1. The value of
     * offset_for_non_ref_pic shall be in the range of -231 to 231 - 1,
     * inclusive.
     */
    @JvmField
    var offsetForNonRefPic = 0

    /**
     * offset_for_top_to_bottom_field is used to calculate the picture order
     * count of a bottom field as specified in subclause 8.2.1. The value of
     * offset_for_top_to_bottom_field shall be in the range of -231 to 231 - 1,
     * inclusive.
     */
    var offsetForTopToBottomField = 0

    // num_ref_frames
    @JvmField
    var numRefFrames = 0

    /**
     * gaps_in_frame_num_value_allowed_flag specifies the allowed values of
     * frame_num as specified in subclause 7.4.3 and the decoding process in
     * case of an inferred gap between values of frame_num as specified in
     * subclause 8.2.5.2.
     */
    @JvmField
    var isGapsInFrameNumValueAllowedFlag = false

    /**
     * frame_mbs_only_flag equal to 0 specifies that coded pictures of the coded
     * video sequence may either be coded fields or coded frames.
     * frame_mbs_only_flag equal to 1 specifies that every coded picture of the
     * coded video sequence is a coded frame containing only frame macroblocks.
     */
    @JvmField
    var isFrameMbsOnlyFlag = false

    // frame_cropping_flag
    @JvmField
    var isFrameCroppingFlag = false

    // frame_crop_left_offset
    @JvmField
    var frameCropLeftOffset = 0

    // frame_crop_right_offset
    @JvmField
    var frameCropRightOffset = 0

    // frame_crop_top_offset
    @JvmField
    var frameCropTopOffset = 0

    // frame_crop_bottom_offset
    @JvmField
    var frameCropBottomOffset = 0
    @JvmField
    var offsetForRefFrame: IntArray? = null
    @JvmField
    var vuiParams: VUIParameters? = null
    @JvmField
    var scalingMatrix: Array<IntArray?>? = null

    // num_ref_frames_in_pic_order_cnt_cycle
    @JvmField
    var numRefFramesInPicOrderCntCycle = 0
    fun write(out: ByteBuffer?) {
        val writer = BitWriter(out)
        CAVLCWriter.writeNBit(writer, profileIdc.toLong(), 8, "SPS: profile_idc")
        CAVLCWriter.writeBool(writer, isConstraintSet0Flag, "SPS: constraint_set_0_flag")
        CAVLCWriter.writeBool(writer, isConstraintSet1Flag, "SPS: constraint_set_1_flag")
        CAVLCWriter.writeBool(writer, isConstraintSet2Flag, "SPS: constraint_set_2_flag")
        CAVLCWriter.writeBool(writer, isConstraintSet3Flag, "SPS: constraint_set_3_flag")
        CAVLCWriter.writeBool(writer, isConstraintSet4Flag, "SPS: constraint_set_4_flag")
        CAVLCWriter.writeBool(writer, isConstraintSet5Flag, "SPS: constraint_set_5_flag")
        CAVLCWriter.writeNBit(writer, 0, 2, "SPS: reserved")
        CAVLCWriter.writeNBit(writer, levelIdc.toLong(), 8, "SPS: level_idc")
        CAVLCWriter.writeUEtrace(writer, seqParameterSetId, "SPS: seq_parameter_set_id")
        if (profileIdc == 100 || profileIdc == 110 || profileIdc == 122 || profileIdc == 144) {
            CAVLCWriter.writeUEtrace(writer, fromColor(chromaFormatIdc), "SPS: chroma_format_idc")
            if (chromaFormatIdc == ColorSpace.YUV444) {
                CAVLCWriter.writeBool(writer, isResidualColorTransformFlag, "SPS: residual_color_transform_flag")
            }
            CAVLCWriter.writeUEtrace(writer, bitDepthLumaMinus8, "SPS: ")
            CAVLCWriter.writeUEtrace(writer, bitDepthChromaMinus8, "SPS: ")
            CAVLCWriter.writeBool(writer, isQpprimeYZeroTransformBypassFlag, "SPS: qpprime_y_zero_transform_bypass_flag")
            CAVLCWriter.writeBool(writer, scalingMatrix != null, "SPS: ")
            if (scalingMatrix != null) {
                for (i in 0..7) {
                    CAVLCWriter.writeBool(writer, scalingMatrix!![i] != null, "SPS: ")
                    if (scalingMatrix!![i] != null) writeScalingList(writer, scalingMatrix!!, i)
                }
            }
        }
        CAVLCWriter.writeUEtrace(writer, log2MaxFrameNumMinus4, "SPS: log2_max_frame_num_minus4")
        CAVLCWriter.writeUEtrace(writer, picOrderCntType, "SPS: pic_order_cnt_type")
        if (picOrderCntType == 0) {
            CAVLCWriter.writeUEtrace(writer, log2MaxPicOrderCntLsbMinus4, "SPS: log2_max_pic_order_cnt_lsb_minus4")
        } else if (picOrderCntType == 1) {
            CAVLCWriter.writeBool(writer, isDeltaPicOrderAlwaysZeroFlag, "SPS: delta_pic_order_always_zero_flag")
            CAVLCWriter.writeSEtrace(writer, offsetForNonRefPic, "SPS: offset_for_non_ref_pic")
            CAVLCWriter.writeSEtrace(writer, offsetForTopToBottomField, "SPS: offset_for_top_to_bottom_field")
            CAVLCWriter.writeUEtrace(writer, offsetForRefFrame!!.size, "SPS: ")
            for (i in offsetForRefFrame!!.indices) CAVLCWriter.writeSEtrace(writer, offsetForRefFrame!![i], "SPS: ")
        }
        CAVLCWriter.writeUEtrace(writer, numRefFrames, "SPS: num_ref_frames")
        CAVLCWriter.writeBool(writer, isGapsInFrameNumValueAllowedFlag, "SPS: gaps_in_frame_num_value_allowed_flag")
        CAVLCWriter.writeUEtrace(writer, picWidthInMbsMinus1, "SPS: pic_width_in_mbs_minus1")
        CAVLCWriter.writeUEtrace(writer, picHeightInMapUnitsMinus1, "SPS: pic_height_in_map_units_minus1")
        CAVLCWriter.writeBool(writer, isFrameMbsOnlyFlag, "SPS: frame_mbs_only_flag")
        if (!isFrameMbsOnlyFlag) {
            CAVLCWriter.writeBool(writer, isMbAdaptiveFrameFieldFlag, "SPS: mb_adaptive_frame_field_flag")
        }
        CAVLCWriter.writeBool(writer, isDirect8x8InferenceFlag, "SPS: direct_8x8_inference_flag")
        CAVLCWriter.writeBool(writer, isFrameCroppingFlag, "SPS: frame_cropping_flag")
        if (isFrameCroppingFlag) {
            CAVLCWriter.writeUEtrace(writer, frameCropLeftOffset, "SPS: frame_crop_left_offset")
            CAVLCWriter.writeUEtrace(writer, frameCropRightOffset, "SPS: frame_crop_right_offset")
            CAVLCWriter.writeUEtrace(writer, frameCropTopOffset, "SPS: frame_crop_top_offset")
            CAVLCWriter.writeUEtrace(writer, frameCropBottomOffset, "SPS: frame_crop_bottom_offset")
        }
        CAVLCWriter.writeBool(writer, vuiParams != null, "SPS: ")
        if (vuiParams != null) writeVUIParameters(vuiParams!!, writer)
        CAVLCWriter.writeTrailingBits(writer)
    }

    private fun writeVUIParameters(vuip: VUIParameters, writer: BitWriter) {
        CAVLCWriter.writeBool(writer, vuip.aspectRatioInfoPresentFlag, "VUI: aspect_ratio_info_present_flag")
        if (vuip.aspectRatioInfoPresentFlag) {
            CAVLCWriter.writeNBit(writer, vuip.aspectRatio!!.value.toLong(), 8, "VUI: aspect_ratio")
            if (vuip.aspectRatio == AspectRatio.Extended_SAR) {
                CAVLCWriter.writeNBit(writer, vuip.sarWidth.toLong(), 16, "VUI: sar_width")
                CAVLCWriter.writeNBit(writer, vuip.sarHeight.toLong(), 16, "VUI: sar_height")
            }
        }
        CAVLCWriter.writeBool(writer, vuip.overscanInfoPresentFlag, "VUI: overscan_info_present_flag")
        if (vuip.overscanInfoPresentFlag) {
            CAVLCWriter.writeBool(writer, vuip.overscanAppropriateFlag, "VUI: overscan_appropriate_flag")
        }
        CAVLCWriter.writeBool(writer, vuip.videoSignalTypePresentFlag, "VUI: video_signal_type_present_flag")
        if (vuip.videoSignalTypePresentFlag) {
            CAVLCWriter.writeNBit(writer, vuip.videoFormat.toLong(), 3, "VUI: video_format")
            CAVLCWriter.writeBool(writer, vuip.videoFullRangeFlag, "VUI: video_full_range_flag")
            CAVLCWriter.writeBool(writer, vuip.colourDescriptionPresentFlag, "VUI: colour_description_present_flag")
            if (vuip.colourDescriptionPresentFlag) {
                CAVLCWriter.writeNBit(writer, vuip.colourPrimaries.toLong(), 8, "VUI: colour_primaries")
                CAVLCWriter.writeNBit(writer, vuip.transferCharacteristics.toLong(), 8, "VUI: transfer_characteristics")
                CAVLCWriter.writeNBit(writer, vuip.matrixCoefficients.toLong(), 8, "VUI: matrix_coefficients")
            }
        }
        CAVLCWriter.writeBool(writer, vuip.chromaLocInfoPresentFlag, "VUI: chroma_loc_info_present_flag")
        if (vuip.chromaLocInfoPresentFlag) {
            CAVLCWriter.writeUEtrace(writer, vuip.chromaSampleLocTypeTopField, "VUI: chroma_sample_loc_type_top_field")
            CAVLCWriter.writeUEtrace(writer, vuip.chromaSampleLocTypeBottomField, "VUI: chroma_sample_loc_type_bottom_field")
        }
        CAVLCWriter.writeBool(writer, vuip.timingInfoPresentFlag, "VUI: timing_info_present_flag")
        if (vuip.timingInfoPresentFlag) {
            CAVLCWriter.writeNBit(writer, vuip.numUnitsInTick.toLong(), 32, "VUI: num_units_in_tick")
            CAVLCWriter.writeNBit(writer, vuip.timeScale.toLong(), 32, "VUI: time_scale")
            CAVLCWriter.writeBool(writer, vuip.fixedFrameRateFlag, "VUI: fixed_frame_rate_flag")
        }
        CAVLCWriter.writeBool(writer, vuip.nalHRDParams != null, "VUI: ")
        if (vuip.nalHRDParams != null) {
            writeHRDParameters(vuip.nalHRDParams!!, writer)
        }
        CAVLCWriter.writeBool(writer, vuip.vclHRDParams != null, "VUI: ")
        if (vuip.vclHRDParams != null) {
            writeHRDParameters(vuip.vclHRDParams!!, writer)
        }
        if (vuip.nalHRDParams != null || vuip.vclHRDParams != null) {
            CAVLCWriter.writeBool(writer, vuip.lowDelayHrdFlag, "VUI: low_delay_hrd_flag")
        }
        CAVLCWriter.writeBool(writer, vuip.picStructPresentFlag, "VUI: pic_struct_present_flag")
        CAVLCWriter.writeBool(writer, vuip.bitstreamRestriction != null, "VUI: ")
        if (vuip.bitstreamRestriction != null) {
            CAVLCWriter.writeBool(writer, vuip.bitstreamRestriction!!.motionVectorsOverPicBoundariesFlag,
                    "VUI: motion_vectors_over_pic_boundaries_flag")
            CAVLCWriter.writeUEtrace(writer, vuip.bitstreamRestriction!!.maxBytesPerPicDenom, "VUI: max_bytes_per_pic_denom")
            CAVLCWriter.writeUEtrace(writer, vuip.bitstreamRestriction!!.maxBitsPerMbDenom, "VUI: max_bits_per_mb_denom")
            CAVLCWriter.writeUEtrace(writer, vuip.bitstreamRestriction!!.log2MaxMvLengthHorizontal,
                    "VUI: log2_max_mv_length_horizontal")
            CAVLCWriter.writeUEtrace(writer, vuip.bitstreamRestriction!!.log2MaxMvLengthVertical, "VUI: log2_max_mv_length_vertical")
            CAVLCWriter.writeUEtrace(writer, vuip.bitstreamRestriction!!.numReorderFrames, "VUI: num_reorder_frames")
            CAVLCWriter.writeUEtrace(writer, vuip.bitstreamRestriction!!.maxDecFrameBuffering, "VUI: max_dec_frame_buffering")
        }
    }

    private fun writeHRDParameters(hrd: HRDParameters, writer: BitWriter) {
        CAVLCWriter.writeUEtrace(writer, hrd.cpbCntMinus1, "HRD: cpb_cnt_minus1")
        CAVLCWriter.writeNBit(writer, hrd.bitRateScale.toLong(), 4, "HRD: bit_rate_scale")
        CAVLCWriter.writeNBit(writer, hrd.cpbSizeScale.toLong(), 4, "HRD: cpb_size_scale")
        for (SchedSelIdx in 0..hrd.cpbCntMinus1) {
            CAVLCWriter.writeUEtrace(writer, hrd.bitRateValueMinus1!![SchedSelIdx], "HRD: ")
            CAVLCWriter.writeUEtrace(writer, hrd.cpbSizeValueMinus1!![SchedSelIdx], "HRD: ")
            CAVLCWriter.writeBool(writer, hrd.cbrFlag!![SchedSelIdx], "HRD: ")
        }
        CAVLCWriter.writeNBit(writer, hrd.initialCpbRemovalDelayLengthMinus1.toLong(), 5,
                "HRD: initial_cpb_removal_delay_length_minus1")
        CAVLCWriter.writeNBit(writer, hrd.cpbRemovalDelayLengthMinus1.toLong(), 5, "HRD: cpb_removal_delay_length_minus1")
        CAVLCWriter.writeNBit(writer, hrd.dpbOutputDelayLengthMinus1.toLong(), 5, "HRD: dpb_output_delay_length_minus1")
        CAVLCWriter.writeNBit(writer, hrd.timeOffsetLength.toLong(), 5, "HRD: time_offset_length")
    }

    fun copy(): SeqParameterSet {
        val buf = ByteBuffer.allocate(2048)
        write(buf)
        buf.flip()
        return read(buf)
    }

    companion object {
        fun getColor(id: Int): ColorSpace {
            when (id) {
                0 -> return ColorSpace.MONO
                1 -> return ColorSpace.YUV420J
                2 -> return ColorSpace.YUV422
                3 -> return ColorSpace.YUV444
            }
            throw RuntimeException("Colorspace not supported")
        }

        fun fromColor(color: ColorSpace?): Int {
            return when (color) {
                ColorSpace.MONO -> 0
                ColorSpace.YUV420J -> 1
                ColorSpace.YUV422 -> 2
                ColorSpace.YUV444 -> 3
                else -> throw RuntimeException("Colorspace not supported")
            }
        }

        @JvmStatic
        fun read(`is`: ByteBuffer?): SeqParameterSet {
            val _in = BitReader.createBitReader(`is`)
            val sps = SeqParameterSet()
            sps.profileIdc = CAVLCReader.readNBit(_in, 8, "SPS: profile_idc")
            sps.isConstraintSet0Flag = CAVLCReader.readBool(_in, "SPS: constraint_set_0_flag")
            sps.isConstraintSet1Flag = CAVLCReader.readBool(_in, "SPS: constraint_set_1_flag")
            sps.isConstraintSet2Flag = CAVLCReader.readBool(_in, "SPS: constraint_set_2_flag")
            sps.isConstraintSet3Flag = CAVLCReader.readBool(_in, "SPS: constraint_set_3_flag")
            sps.isConstraintSet4Flag = CAVLCReader.readBool(_in, "SPS: constraint_set_4_flag")
            sps.isConstraintSet5Flag = CAVLCReader.readBool(_in, "SPS: constraint_set_5_flag")
            CAVLCReader.readNBit(_in, 2, "SPS: reserved_zero_2bits")
            sps.levelIdc = CAVLCReader.readNBit(_in, 8, "SPS: level_idc")
            sps.seqParameterSetId = CAVLCReader.readUEtrace(_in, "SPS: seq_parameter_set_id")
            if (sps.profileIdc == 100 || sps.profileIdc == 110 || sps.profileIdc == 122 || sps.profileIdc == 144) {
                sps.chromaFormatIdc = getColor(CAVLCReader.readUEtrace(_in, "SPS: chroma_format_idc"))
                if (sps.chromaFormatIdc == ColorSpace.YUV444) {
                    sps.isResidualColorTransformFlag = CAVLCReader.readBool(_in, "SPS: separate_colour_plane_flag")
                }
                sps.bitDepthLumaMinus8 = CAVLCReader.readUEtrace(_in, "SPS: bit_depth_luma_minus8")
                sps.bitDepthChromaMinus8 = CAVLCReader.readUEtrace(_in, "SPS: bit_depth_chroma_minus8")
                sps.isQpprimeYZeroTransformBypassFlag = CAVLCReader.readBool(_in, "SPS: qpprime_y_zero_transform_bypass_flag")
                val seqScalingMatrixPresent = CAVLCReader.readBool(_in, "SPS: seq_scaling_matrix_present_lag")
                if (seqScalingMatrixPresent) {
                    readScalingListMatrix(_in, sps)
                }
            } else {
                sps.chromaFormatIdc = ColorSpace.YUV420J
            }
            sps.log2MaxFrameNumMinus4 = CAVLCReader.readUEtrace(_in, "SPS: log2_max_frame_num_minus4")
            sps.picOrderCntType = CAVLCReader.readUEtrace(_in, "SPS: pic_order_cnt_type")
            if (sps.picOrderCntType == 0) {
                sps.log2MaxPicOrderCntLsbMinus4 = CAVLCReader.readUEtrace(_in, "SPS: log2_max_pic_order_cnt_lsb_minus4")
            } else if (sps.picOrderCntType == 1) {
                sps.isDeltaPicOrderAlwaysZeroFlag = CAVLCReader.readBool(_in, "SPS: delta_pic_order_always_zero_flag")
                sps.offsetForNonRefPic = CAVLCReader.readSE(_in, "SPS: offset_for_non_ref_pic")
                sps.offsetForTopToBottomField = CAVLCReader.readSE(_in, "SPS: offset_for_top_to_bottom_field")
                sps.numRefFramesInPicOrderCntCycle = CAVLCReader.readUEtrace(_in, "SPS: num_ref_frames_in_pic_order_cnt_cycle")
                sps.offsetForRefFrame = IntArray(sps.numRefFramesInPicOrderCntCycle)
                for (i in 0 until sps.numRefFramesInPicOrderCntCycle) {
                    sps.offsetForRefFrame!![i] = CAVLCReader.readSE(_in, "SPS: offsetForRefFrame [$i]")
                }
            }
            sps.numRefFrames = CAVLCReader.readUEtrace(_in, "SPS: num_ref_frames")
            sps.isGapsInFrameNumValueAllowedFlag = CAVLCReader.readBool(_in, "SPS: gaps_in_frame_num_value_allowed_flag")
            sps.picWidthInMbsMinus1 = CAVLCReader.readUEtrace(_in, "SPS: pic_width_in_mbs_minus1")
            sps.picHeightInMapUnitsMinus1 = CAVLCReader.readUEtrace(_in, "SPS: pic_height_in_map_units_minus1")
            sps.isFrameMbsOnlyFlag = CAVLCReader.readBool(_in, "SPS: frame_mbs_only_flag")
            if (!sps.isFrameMbsOnlyFlag) {
                sps.isMbAdaptiveFrameFieldFlag = CAVLCReader.readBool(_in, "SPS: mb_adaptive_frame_field_flag")
            }
            sps.isDirect8x8InferenceFlag = CAVLCReader.readBool(_in, "SPS: direct_8x8_inference_flag")
            sps.isFrameCroppingFlag = CAVLCReader.readBool(_in, "SPS: frame_cropping_flag")
            if (sps.isFrameCroppingFlag) {
                sps.frameCropLeftOffset = CAVLCReader.readUEtrace(_in, "SPS: frame_crop_left_offset")
                sps.frameCropRightOffset = CAVLCReader.readUEtrace(_in, "SPS: frame_crop_right_offset")
                sps.frameCropTopOffset = CAVLCReader.readUEtrace(_in, "SPS: frame_crop_top_offset")
                sps.frameCropBottomOffset = CAVLCReader.readUEtrace(_in, "SPS: frame_crop_bottom_offset")
            }
            val vuiParametersPresentFlag = CAVLCReader.readBool(_in, "SPS: vui_parameters_present_flag")
            if (vuiParametersPresentFlag) sps.vuiParams = readVUIParameters(_in)
            return sps
        }

        fun writeScalingList(out: BitWriter, scalingMatrix: Array<IntArray?>, which: Int) {
            // Want to find out if the default scaling list is actually used
            var useDefaultScalingMatrixFlag = false
            when (which) {
                0 -> useDefaultScalingMatrixFlag = Platform.arrayEqualsInt(scalingMatrix[which], H264Const.defaultScalingList4x4Intra)
                1, 2 -> useDefaultScalingMatrixFlag = Platform.arrayEqualsInt(scalingMatrix[which], scalingMatrix[0])
                3 -> useDefaultScalingMatrixFlag = Platform.arrayEqualsInt(scalingMatrix[which], H264Const.defaultScalingList4x4Inter)
                4, 5 -> useDefaultScalingMatrixFlag = Platform.arrayEqualsInt(scalingMatrix[which], scalingMatrix[3])
                6 -> useDefaultScalingMatrixFlag = Platform.arrayEqualsInt(scalingMatrix[which], H264Const.defaultScalingList8x8Intra)
                7 -> useDefaultScalingMatrixFlag = Platform.arrayEqualsInt(scalingMatrix[which], H264Const.defaultScalingList8x8Inter)
            }
            val scalingList = scalingMatrix[which]
            if (useDefaultScalingMatrixFlag) {
                CAVLCWriter.writeSEtrace(out, -8, "SPS: ")
                return
            }
            var lastScale = 8
            val nextScale = 8
            for (j in scalingList!!.indices) {
                if (nextScale != 0) {
                    val deltaScale = scalingList[j] - lastScale - 256
                    CAVLCWriter.writeSEtrace(out, deltaScale, "SPS: ")
                }
                lastScale = scalingList[j]
            }
        }

        //Wrong usage of Javascript keyword:in
        fun readScalingList(src: BitReader, sizeOfScalingList: Int): IntArray? {
            val scalingList = IntArray(sizeOfScalingList)
            var lastScale = 8
            var nextScale = 8
            for (j in 0 until sizeOfScalingList) {
                if (nextScale != 0) {
                    val deltaScale = CAVLCReader.readSE(src, "deltaScale")
                    nextScale = (lastScale + deltaScale + 256) % 256
                    if (j == 0 && nextScale == 0) return null
                }
                scalingList[j] = if (nextScale == 0) lastScale else nextScale
                lastScale = scalingList[j]
            }
            return scalingList
        }

        //Wrong usage of Javascript keyword:in
        private fun readScalingListMatrix(src: BitReader, sps: SeqParameterSet) {
            sps.scalingMatrix = arrayOfNulls(8)
            for (i in 0..7) {
                val seqScalingListPresentFlag = CAVLCReader.readBool(src, "SPS: seqScalingListPresentFlag")
                if (seqScalingListPresentFlag) {
                    val scalingListSize = if (i < 6) 16 else 64
                    sps.scalingMatrix!![i] = readScalingList(src, scalingListSize)
                }
            }
        }

        private fun readVUIParameters(_in: BitReader): VUIParameters {
            val vuip = VUIParameters()
            vuip.aspectRatioInfoPresentFlag = CAVLCReader.readBool(_in, "VUI: aspect_ratio_info_present_flag")
            if (vuip.aspectRatioInfoPresentFlag) {
                vuip.aspectRatio = fromValue(CAVLCReader.readNBit(_in, 8, "VUI: aspect_ratio"))
                if (vuip.aspectRatio == AspectRatio.Extended_SAR) {
                    vuip.sarWidth = CAVLCReader.readNBit(_in, 16, "VUI: sar_width")
                    vuip.sarHeight = CAVLCReader.readNBit(_in, 16, "VUI: sar_height")
                }
            }
            vuip.overscanInfoPresentFlag = CAVLCReader.readBool(_in, "VUI: overscan_info_present_flag")
            if (vuip.overscanInfoPresentFlag) {
                vuip.overscanAppropriateFlag = CAVLCReader.readBool(_in, "VUI: overscan_appropriate_flag")
            }
            vuip.videoSignalTypePresentFlag = CAVLCReader.readBool(_in, "VUI: video_signal_type_present_flag")
            if (vuip.videoSignalTypePresentFlag) {
                vuip.videoFormat = CAVLCReader.readNBit(_in, 3, "VUI: video_format")
                vuip.videoFullRangeFlag = CAVLCReader.readBool(_in, "VUI: video_full_range_flag")
                vuip.colourDescriptionPresentFlag = CAVLCReader.readBool(_in, "VUI: colour_description_present_flag")
                if (vuip.colourDescriptionPresentFlag) {
                    vuip.colourPrimaries = CAVLCReader.readNBit(_in, 8, "VUI: colour_primaries")
                    vuip.transferCharacteristics = CAVLCReader.readNBit(_in, 8, "VUI: transfer_characteristics")
                    vuip.matrixCoefficients = CAVLCReader.readNBit(_in, 8, "VUI: matrix_coefficients")
                }
            }
            vuip.chromaLocInfoPresentFlag = CAVLCReader.readBool(_in, "VUI: chroma_loc_info_present_flag")
            if (vuip.chromaLocInfoPresentFlag) {
                vuip.chromaSampleLocTypeTopField = CAVLCReader.readUEtrace(_in, "VUI chroma_sample_loc_type_top_field")
                vuip.chromaSampleLocTypeBottomField = CAVLCReader.readUEtrace(_in, "VUI chroma_sample_loc_type_bottom_field")
            }
            vuip.timingInfoPresentFlag = CAVLCReader.readBool(_in, "VUI: timing_info_present_flag")
            if (vuip.timingInfoPresentFlag) {
                vuip.numUnitsInTick = CAVLCReader.readNBit(_in, 32, "VUI: num_units_in_tick")
                vuip.timeScale = CAVLCReader.readNBit(_in, 32, "VUI: time_scale")
                vuip.fixedFrameRateFlag = CAVLCReader.readBool(_in, "VUI: fixed_frame_rate_flag")
            }
            val nalHRDParametersPresentFlag = CAVLCReader.readBool(_in, "VUI: nal_hrd_parameters_present_flag")
            if (nalHRDParametersPresentFlag) vuip.nalHRDParams = readHRDParameters(_in)
            val vclHRDParametersPresentFlag = CAVLCReader.readBool(_in, "VUI: vcl_hrd_parameters_present_flag")
            if (vclHRDParametersPresentFlag) vuip.vclHRDParams = readHRDParameters(_in)
            if (nalHRDParametersPresentFlag || vclHRDParametersPresentFlag) {
                vuip.lowDelayHrdFlag = CAVLCReader.readBool(_in, "VUI: low_delay_hrd_flag")
            }
            vuip.picStructPresentFlag = CAVLCReader.readBool(_in, "VUI: pic_struct_present_flag")
            val bitstreamRestrictionFlag = CAVLCReader.readBool(_in, "VUI: bitstream_restriction_flag")
            if (bitstreamRestrictionFlag) {
                vuip.bitstreamRestriction = BitstreamRestriction()
                val bitstreamRestriction = vuip.bitstreamRestriction!!
                bitstreamRestriction.motionVectorsOverPicBoundariesFlag = CAVLCReader.readBool(_in,
                        "VUI: motion_vectors_over_pic_boundaries_flag")
                bitstreamRestriction.maxBytesPerPicDenom = CAVLCReader.readUEtrace(_in, "VUI max_bytes_per_pic_denom")
                bitstreamRestriction.maxBitsPerMbDenom = CAVLCReader.readUEtrace(_in, "VUI max_bits_per_mb_denom")
                bitstreamRestriction.log2MaxMvLengthHorizontal = CAVLCReader.readUEtrace(_in, "VUI log2_max_mv_length_horizontal")
                bitstreamRestriction.log2MaxMvLengthVertical = CAVLCReader.readUEtrace(_in, "VUI log2_max_mv_length_vertical")
                bitstreamRestriction.numReorderFrames = CAVLCReader.readUEtrace(_in, "VUI num_reorder_frames")
                bitstreamRestriction.maxDecFrameBuffering = CAVLCReader.readUEtrace(_in, "VUI max_dec_frame_buffering")
            }
            return vuip
        }

        private fun readHRDParameters(_in: BitReader): HRDParameters {
            val hrd = HRDParameters()
            hrd.cpbCntMinus1 = CAVLCReader.readUEtrace(_in, "SPS: cpb_cnt_minus1")
            hrd.bitRateScale = CAVLCReader.readNBit(_in, 4, "HRD: bit_rate_scale")
            hrd.cpbSizeScale = CAVLCReader.readNBit(_in, 4, "HRD: cpb_size_scale")
            hrd.bitRateValueMinus1 = IntArray(hrd.cpbCntMinus1 + 1)
            hrd.cpbSizeValueMinus1 = IntArray(hrd.cpbCntMinus1 + 1)
            hrd.cbrFlag = BooleanArray(hrd.cpbCntMinus1 + 1)
            for (SchedSelIdx in 0..hrd.cpbCntMinus1) {
                hrd.bitRateValueMinus1!![SchedSelIdx] = CAVLCReader.readUEtrace(_in, "HRD: bit_rate_value_minus1")
                hrd.cpbSizeValueMinus1!![SchedSelIdx] = CAVLCReader.readUEtrace(_in, "HRD: cpb_size_value_minus1")
                hrd.cbrFlag!![SchedSelIdx] = CAVLCReader.readBool(_in, "HRD: cbr_flag")
            }
            hrd.initialCpbRemovalDelayLengthMinus1 = CAVLCReader.readNBit(_in, 5,
                    "HRD: initial_cpb_removal_delay_length_minus1")
            hrd.cpbRemovalDelayLengthMinus1 = CAVLCReader.readNBit(_in, 5, "HRD: cpb_removal_delay_length_minus1")
            hrd.dpbOutputDelayLengthMinus1 = CAVLCReader.readNBit(_in, 5, "HRD: dpb_output_delay_length_minus1")
            hrd.timeOffsetLength = CAVLCReader.readNBit(_in, 5, "HRD: time_offset_length")
            return hrd
        }

        @JvmStatic
        fun getPicHeightInMbs(sps: SeqParameterSet): Int {
            return sps.picHeightInMapUnitsMinus1 + 1 shl if (sps.isFrameMbsOnlyFlag) 0 else 1
        }
    }
}