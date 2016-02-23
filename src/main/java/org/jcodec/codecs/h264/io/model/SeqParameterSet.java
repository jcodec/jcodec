package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUE;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeBool;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeNBit;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSE;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUE;
import static org.jcodec.common.model.ColorSpace.MONO;
import static org.jcodec.common.model.ColorSpace.YUV420J;
import static org.jcodec.common.model.ColorSpace.YUV422;
import static org.jcodec.common.model.ColorSpace.YUV444;

import java.nio.ByteBuffer;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.ColorSpace;

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
    public int pic_order_cnt_type;
    public boolean field_pic_flag;
    public boolean delta_pic_order_always_zero_flag;
    public boolean mb_adaptive_frame_field_flag;
    public boolean direct_8x8_inference_flag;
    public ColorSpace chroma_format_idc;
    public int log2_max_frame_num_minus4;
    public int log2_max_pic_order_cnt_lsb_minus4;
    public int pic_height_in_map_units_minus1;
    public int pic_width_in_mbs_minus1;
    public int bit_depth_luma_minus8;
    public int bit_depth_chroma_minus8;
    public boolean qpprime_y_zero_transform_bypass_flag;
    public int profile_idc;
    public boolean constraint_set_0_flag;
    public boolean constraint_set_1_flag;
    public boolean constraint_set_2_flag;
    public boolean constraint_set_3_flag;
    public boolean constraint_set_4_flag;
    public boolean constraint_set_5_flag;
    public int level_idc;
    public int seq_parameter_set_id;
    public boolean residual_color_transform_flag;
    public int offset_for_non_ref_pic;
    public int offset_for_top_to_bottom_field;
    public int num_ref_frames;
    public boolean gaps_in_frame_num_value_allowed_flag;
    public boolean frame_mbs_only_flag;
    public boolean frame_cropping_flag;
    public int frame_crop_left_offset;
    public int frame_crop_right_offset;
    public int frame_crop_top_offset;
    public int frame_crop_bottom_offset;
    public int[] offsetForRefFrame;
    public VUIParameters vuiParams;
    public ScalingMatrix scalingMatrix;
    public int num_ref_frames_in_pic_order_cnt_cycle;

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
        switch (color) {
        case MONO:
            return 0;
        case YUV420J:
            return 1;
        case YUV422:
            return 2;
        case YUV444:
            return 3;
        }
        throw new RuntimeException("Colorspace not supported");
    }

    public static SeqParameterSet read(ByteBuffer is) {
        BitReader _in = new BitReader(is);
        SeqParameterSet sps = new SeqParameterSet();

        sps.profile_idc = readNBit(_in, 8, "SPS: profile_idc");
        sps.constraint_set_0_flag = readBool(_in, "SPS: constraint_set_0_flag");
        sps.constraint_set_1_flag = readBool(_in, "SPS: constraint_set_1_flag");
        sps.constraint_set_2_flag = readBool(_in, "SPS: constraint_set_2_flag");
        sps.constraint_set_3_flag = readBool(_in, "SPS: constraint_set_3_flag");
        sps.constraint_set_4_flag = readBool(_in, "SPS: constraint_set_4_flag");
        sps.constraint_set_5_flag = readBool(_in, "SPS: constraint_set_5_flag");
        readNBit(_in, 2, "SPS: reserved_zero_2bits");
        sps.level_idc = (int) readNBit(_in, 8, "SPS: level_idc");
        sps.seq_parameter_set_id = readUE(_in, "SPS: seq_parameter_set_id");

        if (sps.profile_idc == 100 || sps.profile_idc == 110 || sps.profile_idc == 122 || sps.profile_idc == 144) {
            sps.chroma_format_idc = getColor(readUE(_in, "SPS: chroma_format_idc"));
            if (sps.chroma_format_idc == YUV444) {
                sps.residual_color_transform_flag = readBool(_in, "SPS: residual_color_transform_flag");
            }
            sps.bit_depth_luma_minus8 = readUE(_in, "SPS: bit_depth_luma_minus8");
            sps.bit_depth_chroma_minus8 = readUE(_in, "SPS: bit_depth_chroma_minus8");
            sps.qpprime_y_zero_transform_bypass_flag = readBool(_in, "SPS: qpprime_y_zero_transform_bypass_flag");
            boolean seqScalingMatrixPresent = readBool(_in, "SPS: seq_scaling_matrix_present_lag");
            if (seqScalingMatrixPresent) {
                readScalingListMatrix(_in, sps);
            }
        } else {
            sps.chroma_format_idc = YUV420J;
        }
        sps.log2_max_frame_num_minus4 = readUE(_in, "SPS: log2_max_frame_num_minus4");
        sps.pic_order_cnt_type = readUE(_in, "SPS: pic_order_cnt_type");
        if (sps.pic_order_cnt_type == 0) {
            sps.log2_max_pic_order_cnt_lsb_minus4 = readUE(_in, "SPS: log2_max_pic_order_cnt_lsb_minus4");
        } else if (sps.pic_order_cnt_type == 1) {
            sps.delta_pic_order_always_zero_flag = readBool(_in, "SPS: delta_pic_order_always_zero_flag");
            sps.offset_for_non_ref_pic = readSE(_in, "SPS: offset_for_non_ref_pic");
            sps.offset_for_top_to_bottom_field = readSE(_in, "SPS: offset_for_top_to_bottom_field");
            sps.num_ref_frames_in_pic_order_cnt_cycle = readUE(_in, "SPS: num_ref_frames_in_pic_order_cnt_cycle");
            sps.offsetForRefFrame = new int[sps.num_ref_frames_in_pic_order_cnt_cycle];
            for (int i = 0; i < sps.num_ref_frames_in_pic_order_cnt_cycle; i++) {
                sps.offsetForRefFrame[i] = readSE(_in, "SPS: offsetForRefFrame [" + i + "]");
            }
        }
        sps.num_ref_frames = readUE(_in, "SPS: num_ref_frames");
        sps.gaps_in_frame_num_value_allowed_flag = readBool(_in, "SPS: gaps_in_frame_num_value_allowed_flag");
        sps.pic_width_in_mbs_minus1 = readUE(_in, "SPS: pic_width_in_mbs_minus1");
        sps.pic_height_in_map_units_minus1 = readUE(_in, "SPS: pic_height_in_map_units_minus1");
        sps.frame_mbs_only_flag = readBool(_in, "SPS: frame_mbs_only_flag");
        if (!sps.frame_mbs_only_flag) {
            sps.mb_adaptive_frame_field_flag = readBool(_in, "SPS: mb_adaptive_frame_field_flag");
        }
        sps.direct_8x8_inference_flag = readBool(_in, "SPS: direct_8x8_inference_flag");
        sps.frame_cropping_flag = readBool(_in, "SPS: frame_cropping_flag");
        if (sps.frame_cropping_flag) {
            sps.frame_crop_left_offset = readUE(_in, "SPS: frame_crop_left_offset");
            sps.frame_crop_right_offset = readUE(_in, "SPS: frame_crop_right_offset");
            sps.frame_crop_top_offset = readUE(_in, "SPS: frame_crop_top_offset");
            sps.frame_crop_bottom_offset = readUE(_in, "SPS: frame_crop_bottom_offset");
        }
        boolean vui_parameters_present_flag = readBool(_in, "SPS: vui_parameters_present_flag");
        if (vui_parameters_present_flag)
            sps.vuiParams = readVUIParameters(_in);

        return sps;
    }

    private static void readScalingListMatrix(BitReader _in, SeqParameterSet sps) {
        sps.scalingMatrix = new ScalingMatrix();
        for (int i = 0; i < 8; i++) {
            boolean seqScalingListPresentFlag = readBool(_in, "SPS: seqScalingListPresentFlag");
            if (seqScalingListPresentFlag) {
                sps.scalingMatrix.ScalingList4x4 = new ScalingList[8];
                sps.scalingMatrix.ScalingList8x8 = new ScalingList[8];
                if (i < 6) {
                    sps.scalingMatrix.ScalingList4x4[i] = ScalingList.read(_in, 16);
                } else {
                    sps.scalingMatrix.ScalingList8x8[i - 6] = ScalingList.read(_in, 64);
                }
            }
        }
    }

    private static VUIParameters readVUIParameters(BitReader _in) {
        VUIParameters vuip = new VUIParameters();
        vuip.aspect_ratio_info_present_flag = readBool(_in, "VUI: aspect_ratio_info_present_flag");
        if (vuip.aspect_ratio_info_present_flag) {
            vuip.aspect_ratio = AspectRatio.fromValue((int) readNBit(_in, 8, "VUI: aspect_ratio"));
            if (vuip.aspect_ratio == AspectRatio.Extended_SAR) {
                vuip.sar_width = (int) readNBit(_in, 16, "VUI: sar_width");
                vuip.sar_height = (int) readNBit(_in, 16, "VUI: sar_height");
            }
        }
        vuip.overscan_info_present_flag = readBool(_in, "VUI: overscan_info_present_flag");
        if (vuip.overscan_info_present_flag) {
            vuip.overscan_appropriate_flag = readBool(_in, "VUI: overscan_appropriate_flag");
        }
        vuip.video_signal_type_present_flag = readBool(_in, "VUI: video_signal_type_present_flag");
        if (vuip.video_signal_type_present_flag) {
            vuip.video_format = (int) readNBit(_in, 3, "VUI: video_format");
            vuip.video_full_range_flag = readBool(_in, "VUI: video_full_range_flag");
            vuip.colour_description_present_flag = readBool(_in, "VUI: colour_description_present_flag");
            if (vuip.colour_description_present_flag) {
                vuip.colour_primaries = (int) readNBit(_in, 8, "VUI: colour_primaries");
                vuip.transfer_characteristics = (int) readNBit(_in, 8, "VUI: transfer_characteristics");
                vuip.matrix_coefficients = (int) readNBit(_in, 8, "VUI: matrix_coefficients");
            }
        }
        vuip.chroma_loc_info_present_flag = readBool(_in, "VUI: chroma_loc_info_present_flag");
        if (vuip.chroma_loc_info_present_flag) {
            vuip.chroma_sample_loc_type_top_field = readUE(_in, "VUI chroma_sample_loc_type_top_field");
            vuip.chroma_sample_loc_type_bottom_field = readUE(_in, "VUI chroma_sample_loc_type_bottom_field");
        }
        vuip.timing_info_present_flag = readBool(_in, "VUI: timing_info_present_flag");
        if (vuip.timing_info_present_flag) {
            vuip.num_units_in_tick = (int) readNBit(_in, 32, "VUI: num_units_in_tick");
            vuip.time_scale = (int) readNBit(_in, 32, "VUI: time_scale");
            vuip.fixed_frame_rate_flag = readBool(_in, "VUI: fixed_frame_rate_flag");
        }
        boolean nal_hrd_parameters_present_flag = readBool(_in, "VUI: nal_hrd_parameters_present_flag");
        if (nal_hrd_parameters_present_flag)
            vuip.nalHRDParams = readHRDParameters(_in);
        boolean vcl_hrd_parameters_present_flag = readBool(_in, "VUI: vcl_hrd_parameters_present_flag");
        if (vcl_hrd_parameters_present_flag)
            vuip.vclHRDParams = readHRDParameters(_in);
        if (nal_hrd_parameters_present_flag || vcl_hrd_parameters_present_flag) {
            vuip.low_delay_hrd_flag = readBool(_in, "VUI: low_delay_hrd_flag");
        }
        vuip.pic_struct_present_flag = readBool(_in, "VUI: pic_struct_present_flag");
        boolean bitstream_restriction_flag = readBool(_in, "VUI: bitstream_restriction_flag");
        if (bitstream_restriction_flag) {
            vuip.bitstreamRestriction = new VUIParameters.BitstreamRestriction();
            vuip.bitstreamRestriction.motion_vectors_over_pic_boundaries_flag = readBool(_in,
                    "VUI: motion_vectors_over_pic_boundaries_flag");
            vuip.bitstreamRestriction.max_bytes_per_pic_denom = readUE(_in, "VUI max_bytes_per_pic_denom");
            vuip.bitstreamRestriction.max_bits_per_mb_denom = readUE(_in, "VUI max_bits_per_mb_denom");
            vuip.bitstreamRestriction.log2_max_mv_length_horizontal = readUE(_in, "VUI log2_max_mv_length_horizontal");
            vuip.bitstreamRestriction.log2_max_mv_length_vertical = readUE(_in, "VUI log2_max_mv_length_vertical");
            vuip.bitstreamRestriction.num_reorder_frames = readUE(_in, "VUI num_reorder_frames");
            vuip.bitstreamRestriction.max_dec_frame_buffering = readUE(_in, "VUI max_dec_frame_buffering");
        }

        return vuip;
    }

    private static HRDParameters readHRDParameters(BitReader _in) {
        HRDParameters hrd = new HRDParameters();
        hrd.cpb_cnt_minus1 = readUE(_in, "SPS: cpb_cnt_minus1");
        hrd.bit_rate_scale = (int) readNBit(_in, 4, "HRD: bit_rate_scale");
        hrd.cpb_size_scale = (int) readNBit(_in, 4, "HRD: cpb_size_scale");
        hrd.bit_rate_value_minus1 = new int[hrd.cpb_cnt_minus1 + 1];
        hrd.cpb_size_value_minus1 = new int[hrd.cpb_cnt_minus1 + 1];
        hrd.cbr_flag = new boolean[hrd.cpb_cnt_minus1 + 1];

        for (int SchedSelIdx = 0; SchedSelIdx <= hrd.cpb_cnt_minus1; SchedSelIdx++) {
            hrd.bit_rate_value_minus1[SchedSelIdx] = readUE(_in, "HRD: bit_rate_value_minus1");
            hrd.cpb_size_value_minus1[SchedSelIdx] = readUE(_in, "HRD: cpb_size_value_minus1");
            hrd.cbr_flag[SchedSelIdx] = readBool(_in, "HRD: cbr_flag");
        }
        hrd.initial_cpb_removal_delay_length_minus1 = (int) readNBit(_in, 5,
                "HRD: initial_cpb_removal_delay_length_minus1");
        hrd.cpb_removal_delay_length_minus1 = (int) readNBit(_in, 5, "HRD: cpb_removal_delay_length_minus1");
        hrd.dpb_output_delay_length_minus1 = (int) readNBit(_in, 5, "HRD: dpb_output_delay_length_minus1");
        hrd.time_offset_length = (int) readNBit(_in, 5, "HRD: time_offset_length");
        return hrd;
    }

    public void write(ByteBuffer out) {
        BitWriter writer = new BitWriter(out);

        writeNBit(writer, profile_idc, 8, "SPS: profile_idc");
        writeBool(writer, constraint_set_0_flag, "SPS: constraint_set_0_flag");
        writeBool(writer, constraint_set_1_flag, "SPS: constraint_set_1_flag");
        writeBool(writer, constraint_set_2_flag, "SPS: constraint_set_2_flag");
        writeBool(writer, constraint_set_3_flag, "SPS: constraint_set_3_flag");
        writeBool(writer, constraint_set_4_flag, "SPS: constraint_set_4_flag");
        writeBool(writer, constraint_set_5_flag, "SPS: constraint_set_5_flag");
        writeNBit(writer, 0, 2, "SPS: reserved");
        writeNBit(writer, level_idc, 8, "SPS: level_idc");
        writeUE(writer, seq_parameter_set_id, "SPS: seq_parameter_set_id");

        if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122 || profile_idc == 144) {
            writeUE(writer, fromColor(chroma_format_idc), "SPS: chroma_format_idc");
            if (chroma_format_idc == YUV444) {
                writeBool(writer, residual_color_transform_flag, "SPS: residual_color_transform_flag");
            }
            writeUE(writer, bit_depth_luma_minus8, "SPS: ");
            writeUE(writer, bit_depth_chroma_minus8, "SPS: ");
            writeBool(writer, qpprime_y_zero_transform_bypass_flag, "SPS: qpprime_y_zero_transform_bypass_flag");
            writeBool(writer, scalingMatrix != null, "SPS: ");
            if (scalingMatrix != null) {
                for (int i = 0; i < 8; i++) {
                    if (i < 6) {
                        writeBool(writer, scalingMatrix.ScalingList4x4[i] != null, "SPS: ");
                        if (scalingMatrix.ScalingList4x4[i] != null) {
                            scalingMatrix.ScalingList4x4[i].write(writer);
                        }
                    } else {
                        writeBool(writer, scalingMatrix.ScalingList8x8[i - 6] != null, "SPS: ");
                        if (scalingMatrix.ScalingList8x8[i - 6] != null) {
                            scalingMatrix.ScalingList8x8[i - 6].write(writer);
                        }
                    }
                }
            }
        }
        writeUE(writer, log2_max_frame_num_minus4, "SPS: log2_max_frame_num_minus4");
        writeUE(writer, pic_order_cnt_type, "SPS: pic_order_cnt_type");
        if (pic_order_cnt_type == 0) {
            writeUE(writer, log2_max_pic_order_cnt_lsb_minus4, "SPS: log2_max_pic_order_cnt_lsb_minus4");
        } else if (pic_order_cnt_type == 1) {
            writeBool(writer, delta_pic_order_always_zero_flag, "SPS: delta_pic_order_always_zero_flag");
            writeSE(writer, offset_for_non_ref_pic, "SPS: offset_for_non_ref_pic");
            writeSE(writer, offset_for_top_to_bottom_field, "SPS: offset_for_top_to_bottom_field");
            writeUE(writer, offsetForRefFrame.length, "SPS: ");
            for (int i = 0; i < offsetForRefFrame.length; i++)
                writeSE(writer, offsetForRefFrame[i], "SPS: ");
        }
        writeUE(writer, num_ref_frames, "SPS: num_ref_frames");
        writeBool(writer, gaps_in_frame_num_value_allowed_flag, "SPS: gaps_in_frame_num_value_allowed_flag");
        writeUE(writer, pic_width_in_mbs_minus1, "SPS: pic_width_in_mbs_minus1");
        writeUE(writer, pic_height_in_map_units_minus1, "SPS: pic_height_in_map_units_minus1");
        writeBool(writer, frame_mbs_only_flag, "SPS: frame_mbs_only_flag");
        if (!frame_mbs_only_flag) {
            writeBool(writer, mb_adaptive_frame_field_flag, "SPS: mb_adaptive_frame_field_flag");
        }
        writeBool(writer, direct_8x8_inference_flag, "SPS: direct_8x8_inference_flag");
        writeBool(writer, frame_cropping_flag, "SPS: frame_cropping_flag");
        if (frame_cropping_flag) {
            writeUE(writer, frame_crop_left_offset, "SPS: frame_crop_left_offset");
            writeUE(writer, frame_crop_right_offset, "SPS: frame_crop_right_offset");
            writeUE(writer, frame_crop_top_offset, "SPS: frame_crop_top_offset");
            writeUE(writer, frame_crop_bottom_offset, "SPS: frame_crop_bottom_offset");
        }
        writeBool(writer, vuiParams != null, "SPS: ");
        if (vuiParams != null)
            writeVUIParameters(vuiParams, writer);

        writeTrailingBits(writer);
    }

    private void writeVUIParameters(VUIParameters vuip, BitWriter writer) {
        writeBool(writer, vuip.aspect_ratio_info_present_flag, "VUI: aspect_ratio_info_present_flag");
        if (vuip.aspect_ratio_info_present_flag) {
            writeNBit(writer, vuip.aspect_ratio.getValue(), 8, "VUI: aspect_ratio");
            if (vuip.aspect_ratio == AspectRatio.Extended_SAR) {
                writeNBit(writer, vuip.sar_width, 16, "VUI: sar_width");
                writeNBit(writer, vuip.sar_height, 16, "VUI: sar_height");
            }
        }
        writeBool(writer, vuip.overscan_info_present_flag, "VUI: overscan_info_present_flag");
        if (vuip.overscan_info_present_flag) {
            writeBool(writer, vuip.overscan_appropriate_flag, "VUI: overscan_appropriate_flag");
        }
        writeBool(writer, vuip.video_signal_type_present_flag, "VUI: video_signal_type_present_flag");
        if (vuip.video_signal_type_present_flag) {
            writeNBit(writer, vuip.video_format, 3, "VUI: video_format");
            writeBool(writer, vuip.video_full_range_flag, "VUI: video_full_range_flag");
            writeBool(writer, vuip.colour_description_present_flag, "VUI: colour_description_present_flag");
            if (vuip.colour_description_present_flag) {
                writeNBit(writer, vuip.colour_primaries, 8, "VUI: colour_primaries");
                writeNBit(writer, vuip.transfer_characteristics, 8, "VUI: transfer_characteristics");
                writeNBit(writer, vuip.matrix_coefficients, 8, "VUI: matrix_coefficients");
            }
        }
        writeBool(writer, vuip.chroma_loc_info_present_flag, "VUI: chroma_loc_info_present_flag");
        if (vuip.chroma_loc_info_present_flag) {
            writeUE(writer, vuip.chroma_sample_loc_type_top_field, "VUI: chroma_sample_loc_type_top_field");
            writeUE(writer, vuip.chroma_sample_loc_type_bottom_field, "VUI: chroma_sample_loc_type_bottom_field");
        }
        writeBool(writer, vuip.timing_info_present_flag, "VUI: timing_info_present_flag");
        if (vuip.timing_info_present_flag) {
            writeNBit(writer, vuip.num_units_in_tick, 32, "VUI: num_units_in_tick");
            writeNBit(writer, vuip.time_scale, 32, "VUI: time_scale");
            writeBool(writer, vuip.fixed_frame_rate_flag, "VUI: fixed_frame_rate_flag");
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
            writeBool(writer, vuip.low_delay_hrd_flag, "VUI: low_delay_hrd_flag");
        }
        writeBool(writer, vuip.pic_struct_present_flag, "VUI: pic_struct_present_flag");
        writeBool(writer, vuip.bitstreamRestriction != null, "VUI: ");
        if (vuip.bitstreamRestriction != null) {
            writeBool(writer, vuip.bitstreamRestriction.motion_vectors_over_pic_boundaries_flag,
                    "VUI: motion_vectors_over_pic_boundaries_flag");
            writeUE(writer, vuip.bitstreamRestriction.max_bytes_per_pic_denom, "VUI: max_bytes_per_pic_denom");
            writeUE(writer, vuip.bitstreamRestriction.max_bits_per_mb_denom, "VUI: max_bits_per_mb_denom");
            writeUE(writer, vuip.bitstreamRestriction.log2_max_mv_length_horizontal,
                    "VUI: log2_max_mv_length_horizontal");
            writeUE(writer, vuip.bitstreamRestriction.log2_max_mv_length_vertical, "VUI: log2_max_mv_length_vertical");
            writeUE(writer, vuip.bitstreamRestriction.num_reorder_frames, "VUI: num_reorder_frames");
            writeUE(writer, vuip.bitstreamRestriction.max_dec_frame_buffering, "VUI: max_dec_frame_buffering");
        }

    }

    private void writeHRDParameters(HRDParameters hrd, BitWriter writer) {
        writeUE(writer, hrd.cpb_cnt_minus1, "HRD: cpb_cnt_minus1");
        writeNBit(writer, hrd.bit_rate_scale, 4, "HRD: bit_rate_scale");
        writeNBit(writer, hrd.cpb_size_scale, 4, "HRD: cpb_size_scale");

        for (int SchedSelIdx = 0; SchedSelIdx <= hrd.cpb_cnt_minus1; SchedSelIdx++) {
            writeUE(writer, hrd.bit_rate_value_minus1[SchedSelIdx], "HRD: ");
            writeUE(writer, hrd.cpb_size_value_minus1[SchedSelIdx], "HRD: ");
            writeBool(writer, hrd.cbr_flag[SchedSelIdx], "HRD: ");
        }
        writeNBit(writer, hrd.initial_cpb_removal_delay_length_minus1, 5,
                "HRD: initial_cpb_removal_delay_length_minus1");
        writeNBit(writer, hrd.cpb_removal_delay_length_minus1, 5, "HRD: cpb_removal_delay_length_minus1");
        writeNBit(writer, hrd.dpb_output_delay_length_minus1, 5, "HRD: dpb_output_delay_length_minus1");
        writeNBit(writer, hrd.time_offset_length, 5, "HRD: time_offset_length");
    }

    public SeqParameterSet copy() {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        write(buf);
        buf.flip();
        return read(buf);
    }

    public int getPic_order_cnt_type() {
        return pic_order_cnt_type;
    }

    public boolean isField_pic_flag() {
        return field_pic_flag;
    }

    public boolean isDelta_pic_order_always_zero_flag() {
        return delta_pic_order_always_zero_flag;
    }

    public boolean isMb_adaptive_frame_field_flag() {
        return mb_adaptive_frame_field_flag;
    }

    public boolean isDirect_8x8_inference_flag() {
        return direct_8x8_inference_flag;
    }

    public ColorSpace getChroma_format_idc() {
        return chroma_format_idc;
    }

    public int getLog2_max_frame_num_minus4() {
        return log2_max_frame_num_minus4;
    }

    public int getLog2_max_pic_order_cnt_lsb_minus4() {
        return log2_max_pic_order_cnt_lsb_minus4;
    }

    public int getPic_height_in_map_units_minus1() {
        return pic_height_in_map_units_minus1;
    }

    public int getPic_width_in_mbs_minus1() {
        return pic_width_in_mbs_minus1;
    }

    public int getBit_depth_luma_minus8() {
        return bit_depth_luma_minus8;
    }

    public int getBit_depth_chroma_minus8() {
        return bit_depth_chroma_minus8;
    }

    public boolean isQpprime_y_zero_transform_bypass_flag() {
        return qpprime_y_zero_transform_bypass_flag;
    }

    public int getProfile_idc() {
        return profile_idc;
    }

    public boolean isConstraint_set_0_flag() {
        return constraint_set_0_flag;
    }

    public boolean isConstraint_set_1_flag() {
        return constraint_set_1_flag;
    }

    public boolean isConstraint_set_2_flag() {
        return constraint_set_2_flag;
    }

    public boolean isConstraint_set_3_flag() {
        return constraint_set_3_flag;
    }
    
    public boolean isConstraint_set_4_flag() {
        return constraint_set_4_flag;
    }
    
    public boolean isConstraint_set_5_flag() {
        return constraint_set_5_flag;
    }

    public int getLevel_idc() {
        return level_idc;
    }

    public int getSeq_parameter_set_id() {
        return seq_parameter_set_id;
    }

    public boolean isResidual_color_transform_flag() {
        return residual_color_transform_flag;
    }

    public int getOffset_for_non_ref_pic() {
        return offset_for_non_ref_pic;
    }

    public int getOffset_for_top_to_bottom_field() {
        return offset_for_top_to_bottom_field;
    }

    public int getNum_ref_frames() {
        return num_ref_frames;
    }

    public boolean isGaps_in_frame_num_value_allowed_flag() {
        return gaps_in_frame_num_value_allowed_flag;
    }

    public boolean isFrame_mbs_only_flag() {
        return frame_mbs_only_flag;
    }

    public boolean isFrame_cropping_flag() {
        return frame_cropping_flag;
    }

    public int getFrame_crop_left_offset() {
        return frame_crop_left_offset;
    }

    public int getFrame_crop_right_offset() {
        return frame_crop_right_offset;
    }

    public int getFrame_crop_top_offset() {
        return frame_crop_top_offset;
    }

    public int getFrame_crop_bottom_offset() {
        return frame_crop_bottom_offset;
    }

    public int[] getOffsetForRefFrame() {
        return offsetForRefFrame;
    }

    public VUIParameters getVuiParams() {
        return vuiParams;
    }

    public ScalingMatrix getScalingMatrix() {
        return scalingMatrix;
    }

    public int getNum_ref_frames_in_pic_order_cnt_cycle() {
        return num_ref_frames_in_pic_order_cnt_cycle;
    }
}