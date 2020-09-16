package org.jcodec.codecs.h264.io.model;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUEtrace;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeBool;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeNBit;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSEtrace;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUEtrace;
import static org.jcodec.common.model.ColorSpace.MONO;
import static org.jcodec.common.model.ColorSpace.YUV420J;
import static org.jcodec.common.model.ColorSpace.YUV422;
import static org.jcodec.common.model.ColorSpace.YUV444;
import static org.jcodec.platform.Platform.arrayEqualsInt;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.ColorSpace;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Sequence Parameter Set structure of h264 bitstream
 * 
 * capable to serialize and deserialize with CAVLC bitstream
 * 
 * @author The JCodec project
 * 
 */
public class SeqParameterSet {
    // pic_order_cnt_type
    public int picOrderCntType;
    // field_pic_flag
    public boolean fieldPicFlag;
    // delta_pic_order_always_zero_flag
    public boolean deltaPicOrderAlwaysZeroFlag;
    // mb_adaptive_frame_field_flag
    public boolean mbAdaptiveFrameFieldFlag;
    // direct_8x8_inference_flag
    public boolean direct8x8InferenceFlag;
    // chroma_format_idc
    public ColorSpace chromaFormatIdc;
    // log2_max_frame_num_minus4
    public int log2MaxFrameNumMinus4;
    // log2_max_pic_order_cnt_lsb_minus4
    public int log2MaxPicOrderCntLsbMinus4;
    // pic_height_in_map_units_minus1
    public int picHeightInMapUnitsMinus1;
    // pic_width_in_mbs_minus1
    public int picWidthInMbsMinus1;
    // bit_depth_luma_minus8
    public int bitDepthLumaMinus8;
    // bit_depth_chroma_minus8
    public int bitDepthChromaMinus8;
    // qpprime_y_zero_transform_bypass_flag
    public boolean qpprimeYZeroTransformBypassFlag;
    // profile_idc
    public int profileIdc;
    // constraint_set0_flag
    public boolean constraintSet0Flag;
    // constraint_set1_flag
    public boolean constraintSet1Flag;
    // constraint_set2_flag
    public boolean constraintSet2Flag;
    // constraint_set_3_flag
    public boolean constraintSet3Flag;
    // constraint_set_4_flag
    public boolean constraintSet4Flag;
    // constraint_set_5_flag
    public boolean constraintSet5Flag;
    // level_idc
    public int levelIdc;
    // seq_parameter_set_id
    public int seqParameterSetId;
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
    public boolean separateColourPlaneFlag;
    
    /**
     * offset_for_non_ref_pic is used to calculate the picture order count of a
     * non-reference picture as specified in 8.2.1. The value of
     * offset_for_non_ref_pic shall be in the range of -231 to 231 - 1,
     * inclusive.
     */
    public int offsetForNonRefPic;
    
    /**
     * offset_for_top_to_bottom_field is used to calculate the picture order
     * count of a bottom field as specified in subclause 8.2.1. The value of
     * offset_for_top_to_bottom_field shall be in the range of -231 to 231 - 1,
     * inclusive.
     */
    public int offsetForTopToBottomField;
    
    // num_ref_frames
    public int numRefFrames;
    
    /**
     * gaps_in_frame_num_value_allowed_flag specifies the allowed values of
     * frame_num as specified in subclause 7.4.3 and the decoding process in
     * case of an inferred gap between values of frame_num as specified in
     * subclause 8.2.5.2.
     */
    public boolean gapsInFrameNumValueAllowedFlag;
    
    /**
     * frame_mbs_only_flag equal to 0 specifies that coded pictures of the coded
     * video sequence may either be coded fields or coded frames.
     * frame_mbs_only_flag equal to 1 specifies that every coded picture of the
     * coded video sequence is a coded frame containing only frame macroblocks.
     */
    public boolean frameMbsOnlyFlag;
    
    // frame_cropping_flag
    public boolean frameCroppingFlag;
    
    // frame_crop_left_offset
    public int frameCropLeftOffset;
    
    // frame_crop_right_offset
    public int frameCropRightOffset;
    
    // frame_crop_top_offset
    public int frameCropTopOffset;
    
    // frame_crop_bottom_offset
    public int frameCropBottomOffset;
    
    public int[] offsetForRefFrame;
    public VUIParameters vuiParams;
    public int[][] scalingMatrix;
    
    // num_ref_frames_in_pic_order_cnt_cycle
    public int numRefFramesInPicOrderCntCycle;

    public static ColorSpace getColor(int id) {
        switch (id) {
        case 0:
            return MONO;
        case 1:
            return YUV420J;
        case 2:
            return YUV422;
        case 3:
            return YUV444;
        }
        throw new RuntimeException("Colorspace not supported");
    }

    public static int fromColor(ColorSpace color) {
        if (color == MONO) {
            return 0;
        } else if (color == YUV420J) {
            return 1;
        } else if (color == YUV422) {
            return 2;
        } else if (color == YUV444) {
            return 3;
        }
        throw new RuntimeException("Colorspace not supported");
    }

    public static SeqParameterSet read(ByteBuffer is) {
        BitReader _in = BitReader.createBitReader(is);
        return read(_in);
    }

    public static SeqParameterSet read(BitReader _in) {
        SeqParameterSet sps = new SeqParameterSet();

        sps.profileIdc = readNBit(_in, 8, "SPS: profile_idc");
        sps.constraintSet0Flag = readBool(_in, "SPS: constraint_set_0_flag");
        sps.constraintSet1Flag = readBool(_in, "SPS: constraint_set_1_flag");
        sps.constraintSet2Flag = readBool(_in, "SPS: constraint_set_2_flag");
        sps.constraintSet3Flag = readBool(_in, "SPS: constraint_set_3_flag");
        sps.constraintSet4Flag = readBool(_in, "SPS: constraint_set_4_flag");
        sps.constraintSet5Flag = readBool(_in, "SPS: constraint_set_5_flag");
        readNBit(_in, 2, "SPS: reserved_zero_2bits");
        sps.levelIdc = (int) readNBit(_in, 8, "SPS: level_idc");
        sps.seqParameterSetId = readUEtrace(_in, "SPS: seq_parameter_set_id");

        if (sps.profileIdc == 100 || sps.profileIdc == 110 || sps.profileIdc == 122 || sps.profileIdc == 144) {
            sps.chromaFormatIdc = getColor(readUEtrace(_in, "SPS: chroma_format_idc"));
            if (sps.chromaFormatIdc == YUV444) {
                sps.separateColourPlaneFlag = readBool(_in, "SPS: separate_colour_plane_flag");
            }
            sps.bitDepthLumaMinus8 = readUEtrace(_in, "SPS: bit_depth_luma_minus8");
            sps.bitDepthChromaMinus8 = readUEtrace(_in, "SPS: bit_depth_chroma_minus8");
            sps.qpprimeYZeroTransformBypassFlag = readBool(_in, "SPS: qpprime_y_zero_transform_bypass_flag");
            boolean seqScalingMatrixPresent = readBool(_in, "SPS: seq_scaling_matrix_present_lag");
            if (seqScalingMatrixPresent) {
                readScalingListMatrix(_in, sps);
            }
        } else {
            sps.chromaFormatIdc = YUV420J;
        }
        sps.log2MaxFrameNumMinus4 = readUEtrace(_in, "SPS: log2_max_frame_num_minus4");
        sps.picOrderCntType = readUEtrace(_in, "SPS: pic_order_cnt_type");
        if (sps.picOrderCntType == 0) {
            sps.log2MaxPicOrderCntLsbMinus4 = readUEtrace(_in, "SPS: log2_max_pic_order_cnt_lsb_minus4");
        } else if (sps.picOrderCntType == 1) {
            sps.deltaPicOrderAlwaysZeroFlag = readBool(_in, "SPS: delta_pic_order_always_zero_flag");
            sps.offsetForNonRefPic = readSE(_in, "SPS: offset_for_non_ref_pic");
            sps.offsetForTopToBottomField = readSE(_in, "SPS: offset_for_top_to_bottom_field");
            sps.numRefFramesInPicOrderCntCycle = readUEtrace(_in, "SPS: num_ref_frames_in_pic_order_cnt_cycle");
            sps.offsetForRefFrame = new int[sps.numRefFramesInPicOrderCntCycle];
            for (int i = 0; i < sps.numRefFramesInPicOrderCntCycle; i++) {
                sps.offsetForRefFrame[i] = readSE(_in, "SPS: offsetForRefFrame [" + i + "]");
            }
        }
        sps.numRefFrames = readUEtrace(_in, "SPS: num_ref_frames");
        sps.gapsInFrameNumValueAllowedFlag = readBool(_in, "SPS: gaps_in_frame_num_value_allowed_flag");
        sps.picWidthInMbsMinus1 = readUEtrace(_in, "SPS: pic_width_in_mbs_minus1");
        sps.picHeightInMapUnitsMinus1 = readUEtrace(_in, "SPS: pic_height_in_map_units_minus1");
        sps.frameMbsOnlyFlag = readBool(_in, "SPS: frame_mbs_only_flag");
        if (!sps.frameMbsOnlyFlag) {
            sps.mbAdaptiveFrameFieldFlag = readBool(_in, "SPS: mb_adaptive_frame_field_flag");
        }
        sps.direct8x8InferenceFlag = readBool(_in, "SPS: direct_8x8_inference_flag");
        sps.frameCroppingFlag = readBool(_in, "SPS: frame_cropping_flag");
        if (sps.frameCroppingFlag) {
            sps.frameCropLeftOffset = readUEtrace(_in, "SPS: frame_crop_left_offset");
            sps.frameCropRightOffset = readUEtrace(_in, "SPS: frame_crop_right_offset");
            sps.frameCropTopOffset = readUEtrace(_in, "SPS: frame_crop_top_offset");
            sps.frameCropBottomOffset = readUEtrace(_in, "SPS: frame_crop_bottom_offset");
        }
        boolean vuiParametersPresentFlag = readBool(_in, "SPS: vui_parameters_present_flag");
        if (vuiParametersPresentFlag)
            sps.vuiParams = readVUIParameters(_in);

        return sps;
    }
    
    public static void writeScalingList(BitWriter out, int[][] scalingMatrix, int which)  {
        // Want to find out if the default scaling list is actually used
        boolean useDefaultScalingMatrixFlag = false;
        switch(which) {
        case 0: // 4x4 intra y
            useDefaultScalingMatrixFlag = arrayEqualsInt(scalingMatrix[which], H264Const.defaultScalingList4x4Intra);
            break;
        case 1:
        case 2:
            useDefaultScalingMatrixFlag = arrayEqualsInt(scalingMatrix[which], scalingMatrix[0]);
            break;
        case 3:
            useDefaultScalingMatrixFlag = arrayEqualsInt(scalingMatrix[which], H264Const.defaultScalingList4x4Inter);
            break;
        case 4:
        case 5:
            useDefaultScalingMatrixFlag = arrayEqualsInt(scalingMatrix[which], scalingMatrix[3]);
            break;
        case 6:
            useDefaultScalingMatrixFlag = arrayEqualsInt(scalingMatrix[which], H264Const.defaultScalingList8x8Intra);
            break;
        case 7:
            useDefaultScalingMatrixFlag = arrayEqualsInt(scalingMatrix[which], H264Const.defaultScalingList8x8Inter);
            break;
        }
        int[] scalingList = scalingMatrix[which];
        
        if (useDefaultScalingMatrixFlag) {
            writeSEtrace(out, -8, "SPS: ");
            return;
        }

        int lastScale = 8;
        int nextScale = 8;
        for (int j = 0; j < scalingList.length; j++) {
            if (nextScale != 0) {
                int deltaScale = scalingList[j] - lastScale - 256;
                writeSEtrace(out, deltaScale, "SPS: ");
            }
            lastScale = scalingList[j];
        }
    }

    //Wrong usage of Javascript keyword:in
    public static int[] readScalingList(BitReader src, int sizeOfScalingList)  {

        int[] scalingList = new int[sizeOfScalingList];
        int lastScale = 8;
        int nextScale = 8;
        for (int j = 0; j < sizeOfScalingList; j++) {
            if (nextScale != 0) {
                int deltaScale = readSE(src, "deltaScale");
                nextScale = (lastScale + deltaScale + 256) % 256;
                if (j == 0 && nextScale == 0)
                    return null;
            }
            scalingList[j] = nextScale == 0 ? lastScale : nextScale;
            lastScale = scalingList[j];
        }
        return scalingList;
    }

    //Wrong usage of Javascript keyword:in
    private static void readScalingListMatrix(BitReader src, SeqParameterSet sps) {
        sps.scalingMatrix = new int[8][];
        for (int i = 0; i < 8; i++) {
            boolean seqScalingListPresentFlag = readBool(src, "SPS: seqScalingListPresentFlag");
            if (seqScalingListPresentFlag) {
                int scalingListSize = i < 6 ? 16 : 64;
                sps.scalingMatrix[i] = readScalingList(src, scalingListSize);
            }
        }
    }

    private static VUIParameters readVUIParameters(BitReader _in) {
        VUIParameters vuip = new VUIParameters();
        vuip.aspectRatioInfoPresentFlag = readBool(_in, "VUI: aspect_ratio_info_present_flag");
        if (vuip.aspectRatioInfoPresentFlag) {
            vuip.aspectRatio = AspectRatio.fromValue((int) readNBit(_in, 8, "VUI: aspect_ratio"));
            if (vuip.aspectRatio == AspectRatio.Extended_SAR) {
                vuip.sarWidth = (int) readNBit(_in, 16, "VUI: sar_width");
                vuip.sarHeight = (int) readNBit(_in, 16, "VUI: sar_height");
            }
        }
        vuip.overscanInfoPresentFlag = readBool(_in, "VUI: overscan_info_present_flag");
        if (vuip.overscanInfoPresentFlag) {
            vuip.overscanAppropriateFlag = readBool(_in, "VUI: overscan_appropriate_flag");
        }
        vuip.videoSignalTypePresentFlag = readBool(_in, "VUI: video_signal_type_present_flag");
        if (vuip.videoSignalTypePresentFlag) {
            vuip.videoFormat = (int) readNBit(_in, 3, "VUI: video_format");
            vuip.videoFullRangeFlag = readBool(_in, "VUI: video_full_range_flag");
            vuip.colourDescriptionPresentFlag = readBool(_in, "VUI: colour_description_present_flag");
            if (vuip.colourDescriptionPresentFlag) {
                vuip.colourPrimaries = (int) readNBit(_in, 8, "VUI: colour_primaries");
                vuip.transferCharacteristics = (int) readNBit(_in, 8, "VUI: transfer_characteristics");
                vuip.matrixCoefficients = (int) readNBit(_in, 8, "VUI: matrix_coefficients");
            }
        }
        vuip.chromaLocInfoPresentFlag = readBool(_in, "VUI: chroma_loc_info_present_flag");
        if (vuip.chromaLocInfoPresentFlag) {
            vuip.chromaSampleLocTypeTopField = readUEtrace(_in, "VUI chroma_sample_loc_type_top_field");
            vuip.chromaSampleLocTypeBottomField = readUEtrace(_in, "VUI chroma_sample_loc_type_bottom_field");
        }
        vuip.timingInfoPresentFlag = readBool(_in, "VUI: timing_info_present_flag");
        if (vuip.timingInfoPresentFlag) {
            vuip.numUnitsInTick = (int) readNBit(_in, 32, "VUI: num_units_in_tick");
            vuip.timeScale = (int) readNBit(_in, 32, "VUI: time_scale");
            vuip.fixedFrameRateFlag = readBool(_in, "VUI: fixed_frame_rate_flag");
        }
        boolean nalHRDParametersPresentFlag = readBool(_in, "VUI: nal_hrd_parameters_present_flag");
        if (nalHRDParametersPresentFlag)
            vuip.nalHRDParams = readHRDParameters(_in);
        boolean vclHRDParametersPresentFlag = readBool(_in, "VUI: vcl_hrd_parameters_present_flag");
        if (vclHRDParametersPresentFlag)
            vuip.vclHRDParams = readHRDParameters(_in);
        if (nalHRDParametersPresentFlag || vclHRDParametersPresentFlag) {
            vuip.lowDelayHrdFlag = readBool(_in, "VUI: low_delay_hrd_flag");
        }
        vuip.picStructPresentFlag = readBool(_in, "VUI: pic_struct_present_flag");
        boolean bitstreamRestrictionFlag = readBool(_in, "VUI: bitstream_restriction_flag");
        if (bitstreamRestrictionFlag) {
            vuip.bitstreamRestriction = new VUIParameters.BitstreamRestriction();
            vuip.bitstreamRestriction.motionVectorsOverPicBoundariesFlag = readBool(_in,
                    "VUI: motion_vectors_over_pic_boundaries_flag");
            vuip.bitstreamRestriction.maxBytesPerPicDenom = readUEtrace(_in, "VUI max_bytes_per_pic_denom");
            vuip.bitstreamRestriction.maxBitsPerMbDenom = readUEtrace(_in, "VUI max_bits_per_mb_denom");
            vuip.bitstreamRestriction.log2MaxMvLengthHorizontal = readUEtrace(_in, "VUI log2_max_mv_length_horizontal");
            vuip.bitstreamRestriction.log2MaxMvLengthVertical = readUEtrace(_in, "VUI log2_max_mv_length_vertical");
            vuip.bitstreamRestriction.numReorderFrames = readUEtrace(_in, "VUI num_reorder_frames");
            vuip.bitstreamRestriction.maxDecFrameBuffering = readUEtrace(_in, "VUI max_dec_frame_buffering");
        }

        return vuip;
    }

    private static HRDParameters readHRDParameters(BitReader _in) {
        HRDParameters hrd = new HRDParameters();
        hrd.cpbCntMinus1 = readUEtrace(_in, "SPS: cpb_cnt_minus1");
        hrd.bitRateScale = (int) readNBit(_in, 4, "HRD: bit_rate_scale");
        hrd.cpbSizeScale = (int) readNBit(_in, 4, "HRD: cpb_size_scale");
        hrd.bitRateValueMinus1 = new int[hrd.cpbCntMinus1 + 1];
        hrd.cpbSizeValueMinus1 = new int[hrd.cpbCntMinus1 + 1];
        hrd.cbrFlag = new boolean[hrd.cpbCntMinus1 + 1];

        for (int SchedSelIdx = 0; SchedSelIdx <= hrd.cpbCntMinus1; SchedSelIdx++) {
            hrd.bitRateValueMinus1[SchedSelIdx] = readUEtrace(_in, "HRD: bit_rate_value_minus1");
            hrd.cpbSizeValueMinus1[SchedSelIdx] = readUEtrace(_in, "HRD: cpb_size_value_minus1");
            hrd.cbrFlag[SchedSelIdx] = readBool(_in, "HRD: cbr_flag");
        }
        hrd.initialCpbRemovalDelayLengthMinus1 = (int) readNBit(_in, 5,
                "HRD: initial_cpb_removal_delay_length_minus1");
        hrd.cpbRemovalDelayLengthMinus1 = (int) readNBit(_in, 5, "HRD: cpb_removal_delay_length_minus1");
        hrd.dpbOutputDelayLengthMinus1 = (int) readNBit(_in, 5, "HRD: dpb_output_delay_length_minus1");
        hrd.timeOffsetLength = (int) readNBit(_in, 5, "HRD: time_offset_length");
        return hrd;
    }

    public void write(ByteBuffer out) {
        BitWriter writer = new BitWriter(out);

        writeNBit(writer, profileIdc, 8, "SPS: profile_idc");
        writeBool(writer, constraintSet0Flag, "SPS: constraint_set_0_flag");
        writeBool(writer, constraintSet1Flag, "SPS: constraint_set_1_flag");
        writeBool(writer, constraintSet2Flag, "SPS: constraint_set_2_flag");
        writeBool(writer, constraintSet3Flag, "SPS: constraint_set_3_flag");
        writeBool(writer, constraintSet4Flag, "SPS: constraint_set_4_flag");
        writeBool(writer, constraintSet5Flag, "SPS: constraint_set_5_flag");
        writeNBit(writer, 0, 2, "SPS: reserved");
        writeNBit(writer, levelIdc, 8, "SPS: level_idc");
        writeUEtrace(writer, seqParameterSetId, "SPS: seq_parameter_set_id");

        if (profileIdc == 100 || profileIdc == 110 || profileIdc == 122 || profileIdc == 144) {
            writeUEtrace(writer, fromColor(chromaFormatIdc), "SPS: chroma_format_idc");
            if (chromaFormatIdc == YUV444) {
                writeBool(writer, separateColourPlaneFlag, "SPS: residual_color_transform_flag");
            }
            writeUEtrace(writer, bitDepthLumaMinus8, "SPS: ");
            writeUEtrace(writer, bitDepthChromaMinus8, "SPS: ");
            writeBool(writer, qpprimeYZeroTransformBypassFlag, "SPS: qpprime_y_zero_transform_bypass_flag");
            writeBool(writer, scalingMatrix != null, "SPS: ");
            if (scalingMatrix != null) {
                for (int i = 0; i < 8; i++) {
                    writeBool(writer, scalingMatrix[i] != null, "SPS: ");
                    if (scalingMatrix[i] != null)
                        writeScalingList(writer, scalingMatrix, i);
                }
            }
        }
        writeUEtrace(writer, log2MaxFrameNumMinus4, "SPS: log2_max_frame_num_minus4");
        writeUEtrace(writer, picOrderCntType, "SPS: pic_order_cnt_type");
        if (picOrderCntType == 0) {
            writeUEtrace(writer, log2MaxPicOrderCntLsbMinus4, "SPS: log2_max_pic_order_cnt_lsb_minus4");
        } else if (picOrderCntType == 1) {
            writeBool(writer, deltaPicOrderAlwaysZeroFlag, "SPS: delta_pic_order_always_zero_flag");
            writeSEtrace(writer, offsetForNonRefPic, "SPS: offset_for_non_ref_pic");
            writeSEtrace(writer, offsetForTopToBottomField, "SPS: offset_for_top_to_bottom_field");
            writeUEtrace(writer, offsetForRefFrame.length, "SPS: ");
            for (int i = 0; i < offsetForRefFrame.length; i++)
                writeSEtrace(writer, offsetForRefFrame[i], "SPS: ");
        }
        writeUEtrace(writer, numRefFrames, "SPS: num_ref_frames");
        writeBool(writer, gapsInFrameNumValueAllowedFlag, "SPS: gaps_in_frame_num_value_allowed_flag");
        writeUEtrace(writer, picWidthInMbsMinus1, "SPS: pic_width_in_mbs_minus1");
        writeUEtrace(writer, picHeightInMapUnitsMinus1, "SPS: pic_height_in_map_units_minus1");
        writeBool(writer, frameMbsOnlyFlag, "SPS: frame_mbs_only_flag");
        if (!frameMbsOnlyFlag) {
            writeBool(writer, mbAdaptiveFrameFieldFlag, "SPS: mb_adaptive_frame_field_flag");
        }
        writeBool(writer, direct8x8InferenceFlag, "SPS: direct_8x8_inference_flag");
        writeBool(writer, frameCroppingFlag, "SPS: frame_cropping_flag");
        if (frameCroppingFlag) {
            writeUEtrace(writer, frameCropLeftOffset, "SPS: frame_crop_left_offset");
            writeUEtrace(writer, frameCropRightOffset, "SPS: frame_crop_right_offset");
            writeUEtrace(writer, frameCropTopOffset, "SPS: frame_crop_top_offset");
            writeUEtrace(writer, frameCropBottomOffset, "SPS: frame_crop_bottom_offset");
        }
        writeBool(writer, vuiParams != null, "SPS: ");
        if (vuiParams != null)
            writeVUIParameters(vuiParams, writer);

        writeTrailingBits(writer);
    }

    private void writeVUIParameters(VUIParameters vuip, BitWriter writer) {
        writeBool(writer, vuip.aspectRatioInfoPresentFlag, "VUI: aspect_ratio_info_present_flag");
        if (vuip.aspectRatioInfoPresentFlag) {
            writeNBit(writer, vuip.aspectRatio.getValue(), 8, "VUI: aspect_ratio");
            if (vuip.aspectRatio == AspectRatio.Extended_SAR) {
                writeNBit(writer, vuip.sarWidth, 16, "VUI: sar_width");
                writeNBit(writer, vuip.sarHeight, 16, "VUI: sar_height");
            }
        }
        writeBool(writer, vuip.overscanInfoPresentFlag, "VUI: overscan_info_present_flag");
        if (vuip.overscanInfoPresentFlag) {
            writeBool(writer, vuip.overscanAppropriateFlag, "VUI: overscan_appropriate_flag");
        }
        writeBool(writer, vuip.videoSignalTypePresentFlag, "VUI: video_signal_type_present_flag");
        if (vuip.videoSignalTypePresentFlag) {
            writeNBit(writer, vuip.videoFormat, 3, "VUI: video_format");
            writeBool(writer, vuip.videoFullRangeFlag, "VUI: video_full_range_flag");
            writeBool(writer, vuip.colourDescriptionPresentFlag, "VUI: colour_description_present_flag");
            if (vuip.colourDescriptionPresentFlag) {
                writeNBit(writer, vuip.colourPrimaries, 8, "VUI: colour_primaries");
                writeNBit(writer, vuip.transferCharacteristics, 8, "VUI: transfer_characteristics");
                writeNBit(writer, vuip.matrixCoefficients, 8, "VUI: matrix_coefficients");
            }
        }
        writeBool(writer, vuip.chromaLocInfoPresentFlag, "VUI: chroma_loc_info_present_flag");
        if (vuip.chromaLocInfoPresentFlag) {
            writeUEtrace(writer, vuip.chromaSampleLocTypeTopField, "VUI: chroma_sample_loc_type_top_field");
            writeUEtrace(writer, vuip.chromaSampleLocTypeBottomField, "VUI: chroma_sample_loc_type_bottom_field");
        }
        writeBool(writer, vuip.timingInfoPresentFlag, "VUI: timing_info_present_flag");
        if (vuip.timingInfoPresentFlag) {
            writeNBit(writer, vuip.numUnitsInTick, 32, "VUI: num_units_in_tick");
            writeNBit(writer, vuip.timeScale, 32, "VUI: time_scale");
            writeBool(writer, vuip.fixedFrameRateFlag, "VUI: fixed_frame_rate_flag");
        }
        writeBool(writer, vuip.nalHRDParams != null, "VUI: ");
        if (vuip.nalHRDParams != null) {
            writeHRDParameters(vuip.nalHRDParams, writer);
        }
        writeBool(writer, vuip.vclHRDParams != null, "VUI: ");
        if (vuip.vclHRDParams != null) {
            writeHRDParameters(vuip.vclHRDParams, writer);
        }

        if (vuip.nalHRDParams != null || vuip.vclHRDParams != null) {
            writeBool(writer, vuip.lowDelayHrdFlag, "VUI: low_delay_hrd_flag");
        }
        writeBool(writer, vuip.picStructPresentFlag, "VUI: pic_struct_present_flag");
        writeBool(writer, vuip.bitstreamRestriction != null, "VUI: ");
        if (vuip.bitstreamRestriction != null) {
            writeBool(writer, vuip.bitstreamRestriction.motionVectorsOverPicBoundariesFlag,
                    "VUI: motion_vectors_over_pic_boundaries_flag");
            writeUEtrace(writer, vuip.bitstreamRestriction.maxBytesPerPicDenom, "VUI: max_bytes_per_pic_denom");
            writeUEtrace(writer, vuip.bitstreamRestriction.maxBitsPerMbDenom, "VUI: max_bits_per_mb_denom");
            writeUEtrace(writer, vuip.bitstreamRestriction.log2MaxMvLengthHorizontal,
                    "VUI: log2_max_mv_length_horizontal");
            writeUEtrace(writer, vuip.bitstreamRestriction.log2MaxMvLengthVertical, "VUI: log2_max_mv_length_vertical");
            writeUEtrace(writer, vuip.bitstreamRestriction.numReorderFrames, "VUI: num_reorder_frames");
            writeUEtrace(writer, vuip.bitstreamRestriction.maxDecFrameBuffering, "VUI: max_dec_frame_buffering");
        }

    }

    private void writeHRDParameters(HRDParameters hrd, BitWriter writer) {
        writeUEtrace(writer, hrd.cpbCntMinus1, "HRD: cpb_cnt_minus1");
        writeNBit(writer, hrd.bitRateScale, 4, "HRD: bit_rate_scale");
        writeNBit(writer, hrd.cpbSizeScale, 4, "HRD: cpb_size_scale");

        for (int SchedSelIdx = 0; SchedSelIdx <= hrd.cpbCntMinus1; SchedSelIdx++) {
            writeUEtrace(writer, hrd.bitRateValueMinus1[SchedSelIdx], "HRD: ");
            writeUEtrace(writer, hrd.cpbSizeValueMinus1[SchedSelIdx], "HRD: ");
            writeBool(writer, hrd.cbrFlag[SchedSelIdx], "HRD: ");
        }
        writeNBit(writer, hrd.initialCpbRemovalDelayLengthMinus1, 5,
                "HRD: initial_cpb_removal_delay_length_minus1");
        writeNBit(writer, hrd.cpbRemovalDelayLengthMinus1, 5, "HRD: cpb_removal_delay_length_minus1");
        writeNBit(writer, hrd.dpbOutputDelayLengthMinus1, 5, "HRD: dpb_output_delay_length_minus1");
        writeNBit(writer, hrd.timeOffsetLength, 5, "HRD: time_offset_length");
    }

    public SeqParameterSet copy() {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        write(buf);
        buf.flip();
        return read(buf);
    }

    public int getPicOrderCntType() {
        return picOrderCntType;
    }

    public boolean isFieldPicFlag() {
        return fieldPicFlag;
    }

    public boolean isDeltaPicOrderAlwaysZeroFlag() {
        return deltaPicOrderAlwaysZeroFlag;
    }

    public boolean isMbAdaptiveFrameFieldFlag() {
        return mbAdaptiveFrameFieldFlag;
    }

    public boolean isDirect8x8InferenceFlag() {
        return direct8x8InferenceFlag;
    }

    public ColorSpace getChromaFormatIdc() {
        return chromaFormatIdc;
    }

    public int getLog2MaxFrameNumMinus4() {
        return log2MaxFrameNumMinus4;
    }

    public int getLog2MaxPicOrderCntLsbMinus4() {
        return log2MaxPicOrderCntLsbMinus4;
    }

    public int getPicHeightInMapUnitsMinus1() {
        return picHeightInMapUnitsMinus1;
    }

    public int getPicWidthInMbsMinus1() {
        return picWidthInMbsMinus1;
    }

    public int getBitDepthLumaMinus8() {
        return bitDepthLumaMinus8;
    }

    public int getBitDepthChromaMinus8() {
        return bitDepthChromaMinus8;
    }

    public boolean isQpprimeYZeroTransformBypassFlag() {
        return qpprimeYZeroTransformBypassFlag;
    }

    public int getProfileIdc() {
        return profileIdc;
    }

    public boolean isConstraintSet0Flag() {
        return constraintSet0Flag;
    }

    public boolean isConstraintSet1Flag() {
        return constraintSet1Flag;
    }

    public boolean isConstraintSet2Flag() {
        return constraintSet2Flag;
    }

    public boolean isConstraintSet3Flag() {
        return constraintSet3Flag;
    }
    
    public boolean isConstraintSet4Flag() {
        return constraintSet4Flag;
    }
    
    public boolean isConstraintSet5Flag() {
        return constraintSet5Flag;
    }

    public int getLevelIdc() {
        return levelIdc;
    }

    public int getSeqParameterSetId() {
        return seqParameterSetId;
    }

    public boolean isResidualColorTransformFlag() {
        return separateColourPlaneFlag;
    }

    public int getOffsetForNonRefPic() {
        return offsetForNonRefPic;
    }

    public int getOffsetForTopToBottomField() {
        return offsetForTopToBottomField;
    }

    public int getNumRefFrames() {
        return numRefFrames;
    }

    public boolean isGapsInFrameNumValueAllowedFlag() {
        return gapsInFrameNumValueAllowedFlag;
    }

    public boolean isFrameMbsOnlyFlag() {
        return frameMbsOnlyFlag;
    }

    public boolean isFrameCroppingFlag() {
        return frameCroppingFlag;
    }

    public int getFrameCropLeftOffset() {
        return frameCropLeftOffset;
    }

    public int getFrameCropRightOffset() {
        return frameCropRightOffset;
    }

    public int getFrameCropTopOffset() {
        return frameCropTopOffset;
    }

    public int getFrameCropBottomOffset() {
        return frameCropBottomOffset;
    }

    public int[] getOffsetForRefFrame() {
        return offsetForRefFrame;
    }

    public VUIParameters getVuiParams() {
        return vuiParams;
    }

    public int[][] getScalingMatrix() {
        return scalingMatrix;
    }

    public int getNumRefFramesInPicOrderCntCycle() {
        return numRefFramesInPicOrderCntCycle;
    }

    public static int getPicHeightInMbs(SeqParameterSet sps) {
        int picHeightInMbs = (sps.picHeightInMapUnitsMinus1 + 1) << (sps.frameMbsOnlyFlag ? 0 : 1);
        return picHeightInMbs;
    }
}