package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.decode.CAVLCReader.moreRBSPData;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readU;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUEtrace;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeBool;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeNBit;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSEtrace;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeU;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUEtrace;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Picture Parameter Set entity of H264 bitstream
 * 
 * capable to serialize / deserialize with CAVLC bitstream
 * 
 * @author The JCodec project
 * 
 */
public class PictureParameterSet {

    public static class PPSExt {
        public boolean transform_8x8_mode_flag;
        public ScalingMatrix scalindMatrix;
        public int second_chroma_qp_index_offset;
        public boolean[] pic_scaling_list_present_flag;
        public boolean isTransform_8x8_mode_flag() {
            return transform_8x8_mode_flag;
        }
        public ScalingMatrix getScalindMatrix() {
            return scalindMatrix;
        }
        public int getSecond_chroma_qp_index_offset() {
            return second_chroma_qp_index_offset;
        }
        public boolean[] getPic_scaling_list_present_flag() {
            return pic_scaling_list_present_flag;
        }
    }

    public boolean entropy_coding_mode_flag;
    public int[] num_ref_idx_active_minus1;
    public int slice_group_change_rate_minus1;
    public int pic_parameter_set_id;
    public int seq_parameter_set_id;
    public boolean pic_order_present_flag;
    public int num_slice_groups_minus1;
    public int slice_group_map_type;
    public boolean weighted_pred_flag;
    public int weighted_bipred_idc;
    public int pic_init_qp_minus26;
    public int pic_init_qs_minus26;
    public int chroma_qp_index_offset;
    public boolean deblocking_filter_control_present_flag;
    public boolean constrained_intra_pred_flag;
    public boolean redundant_pic_cnt_present_flag;
    public int[] top_left;
    public int[] bottom_right;
    public int[] run_length_minus1;
    public boolean slice_group_change_direction_flag;
    public int[] slice_group_id;
    public PPSExt extended;
    
    public PictureParameterSet() {
        this.num_ref_idx_active_minus1 = new int[2];
    }
    
    public static PictureParameterSet read(ByteBuffer is) {
        BitReader _in = BitReader.createBitReader(is);
        PictureParameterSet pps = new PictureParameterSet();

        pps.pic_parameter_set_id = readUEtrace(_in, "PPS: pic_parameter_set_id");
        pps.seq_parameter_set_id = readUEtrace(_in, "PPS: seq_parameter_set_id");
        pps.entropy_coding_mode_flag = readBool(_in, "PPS: entropy_coding_mode_flag");
        pps.pic_order_present_flag = readBool(_in, "PPS: pic_order_present_flag");
        pps.num_slice_groups_minus1 = readUEtrace(_in, "PPS: num_slice_groups_minus1");
        if (pps.num_slice_groups_minus1 > 0) {
            pps.slice_group_map_type = readUEtrace(_in, "PPS: slice_group_map_type");
            pps.top_left = new int[pps.num_slice_groups_minus1 + 1];
            pps.bottom_right = new int[pps.num_slice_groups_minus1 + 1];
            pps.run_length_minus1 = new int[pps.num_slice_groups_minus1 + 1];
            if (pps.slice_group_map_type == 0)
                for (int iGroup = 0; iGroup <= pps.num_slice_groups_minus1; iGroup++)
                    pps.run_length_minus1[iGroup] = readUEtrace(_in, "PPS: run_length_minus1");
            else if (pps.slice_group_map_type == 2)
                for (int iGroup = 0; iGroup < pps.num_slice_groups_minus1; iGroup++) {
                    pps.top_left[iGroup] = readUEtrace(_in, "PPS: top_left");
                    pps.bottom_right[iGroup] = readUEtrace(_in, "PPS: bottom_right");
                }
            else if (pps.slice_group_map_type == 3 || pps.slice_group_map_type == 4 || pps.slice_group_map_type == 5) {
                pps.slice_group_change_direction_flag = readBool(_in, "PPS: slice_group_change_direction_flag");
                pps.slice_group_change_rate_minus1 = readUEtrace(_in, "PPS: slice_group_change_rate_minus1");
            } else if (pps.slice_group_map_type == 6) {
                int NumberBitsPerSliceGroupId;
                if (pps.num_slice_groups_minus1 + 1 > 4)
                    NumberBitsPerSliceGroupId = 3;
                else if (pps.num_slice_groups_minus1 + 1 > 2)
                    NumberBitsPerSliceGroupId = 2;
                else
                    NumberBitsPerSliceGroupId = 1;
                int pic_size_in_map_units_minus1 = readUEtrace(_in, "PPS: pic_size_in_map_units_minus1");
                pps.slice_group_id = new int[pic_size_in_map_units_minus1 + 1];
                for (int i = 0; i <= pic_size_in_map_units_minus1; i++) {
                    pps.slice_group_id[i] = readU(_in, NumberBitsPerSliceGroupId, "PPS: slice_group_id [" + i + "]f");
                }
            }
        }
        pps.num_ref_idx_active_minus1 = new int[] {readUEtrace(_in, "PPS: num_ref_idx_l0_active_minus1"), readUEtrace(_in, "PPS: num_ref_idx_l1_active_minus1")};
        pps.weighted_pred_flag = readBool(_in, "PPS: weighted_pred_flag");
        pps.weighted_bipred_idc = readNBit(_in, 2, "PPS: weighted_bipred_idc");
        pps.pic_init_qp_minus26 = readSE(_in, "PPS: pic_init_qp_minus26");
        pps.pic_init_qs_minus26 = readSE(_in, "PPS: pic_init_qs_minus26");
        pps.chroma_qp_index_offset = readSE(_in, "PPS: chroma_qp_index_offset");
        pps.deblocking_filter_control_present_flag = readBool(_in, "PPS: deblocking_filter_control_present_flag");
        pps.constrained_intra_pred_flag = readBool(_in, "PPS: constrained_intra_pred_flag");
        pps.redundant_pic_cnt_present_flag = readBool(_in, "PPS: redundant_pic_cnt_present_flag");
        if (moreRBSPData(_in)) {
            pps.extended = new PictureParameterSet.PPSExt();
            pps.extended.transform_8x8_mode_flag = readBool(_in, "PPS: transform_8x8_mode_flag");
            boolean pic_scaling_matrix_present_flag = readBool(_in, "PPS: pic_scaling_matrix_present_flag");
            if (pic_scaling_matrix_present_flag) {
                for (int i = 0; i < 6 + 2 * (pps.extended.transform_8x8_mode_flag ? 1 : 0); i++) {
                    boolean pic_scaling_list_present_flag = readBool(_in, "PPS: pic_scaling_list_present_flag");
                    if (pic_scaling_list_present_flag) {
                        pps.extended.scalindMatrix = new ScalingMatrix();
                        pps.extended.scalindMatrix.ScalingList4x4 = new ScalingList[8];
                        pps.extended.scalindMatrix.ScalingList8x8 = new ScalingList[8];
                        if (i < 6) {
                            pps.extended.scalindMatrix.ScalingList4x4[i] = ScalingList.read(_in, 16);
                        } else {
                            pps.extended.scalindMatrix.ScalingList8x8[i - 6] = ScalingList.read(_in, 64);
                        }
                    }
                }
            }
            pps.extended.second_chroma_qp_index_offset = readSE(_in, "PPS: second_chroma_qp_index_offset");
        }

        return pps;
    }

    public void write(ByteBuffer out) {
        BitWriter writer = new BitWriter(out);

        writeUEtrace(writer, pic_parameter_set_id, "PPS: pic_parameter_set_id");
        writeUEtrace(writer, seq_parameter_set_id, "PPS: seq_parameter_set_id");
        writeBool(writer, entropy_coding_mode_flag, "PPS: entropy_coding_mode_flag");
        writeBool(writer, pic_order_present_flag, "PPS: pic_order_present_flag");
        writeUEtrace(writer, num_slice_groups_minus1, "PPS: num_slice_groups_minus1");
        if (num_slice_groups_minus1 > 0) {
            writeUEtrace(writer, slice_group_map_type, "PPS: slice_group_map_type");
            int[] top_left = new int[1];
            int[] bottom_right = new int[1];
            int[] run_length_minus1 = new int[1];
            if (slice_group_map_type == 0) {
                for (int iGroup = 0; iGroup <= num_slice_groups_minus1; iGroup++) {
                    writeUEtrace(writer, run_length_minus1[iGroup], "PPS: ");
                }
            } else if (slice_group_map_type == 2) {
                for (int iGroup = 0; iGroup < num_slice_groups_minus1; iGroup++) {
                    writeUEtrace(writer, top_left[iGroup], "PPS: ");
                    writeUEtrace(writer, bottom_right[iGroup], "PPS: ");
                }
            } else if (slice_group_map_type == 3 || slice_group_map_type == 4 || slice_group_map_type == 5) {
                writeBool(writer, slice_group_change_direction_flag, "PPS: slice_group_change_direction_flag");
                writeUEtrace(writer, slice_group_change_rate_minus1, "PPS: slice_group_change_rate_minus1");
            } else if (slice_group_map_type == 6) {
                int NumberBitsPerSliceGroupId;
                if (num_slice_groups_minus1 + 1 > 4)
                    NumberBitsPerSliceGroupId = 3;
                else if (num_slice_groups_minus1 + 1 > 2)
                    NumberBitsPerSliceGroupId = 2;
                else
                    NumberBitsPerSliceGroupId = 1;
                writeUEtrace(writer, slice_group_id.length, "PPS: ");
                for (int i = 0; i <= slice_group_id.length; i++) {
                    writeU(writer, slice_group_id[i], NumberBitsPerSliceGroupId);
                }
            }
        }
        writeUEtrace(writer, num_ref_idx_active_minus1[0], "PPS: num_ref_idx_l0_active_minus1");
        writeUEtrace(writer, num_ref_idx_active_minus1[1], "PPS: num_ref_idx_l1_active_minus1");
        writeBool(writer, weighted_pred_flag, "PPS: weighted_pred_flag");
        writeNBit(writer, weighted_bipred_idc, 2, "PPS: weighted_bipred_idc");
        writeSEtrace(writer, pic_init_qp_minus26, "PPS: pic_init_qp_minus26");
        writeSEtrace(writer, pic_init_qs_minus26, "PPS: pic_init_qs_minus26");
        writeSEtrace(writer, chroma_qp_index_offset, "PPS: chroma_qp_index_offset");
        writeBool(writer, deblocking_filter_control_present_flag, "PPS: deblocking_filter_control_present_flag");
        writeBool(writer, constrained_intra_pred_flag, "PPS: constrained_intra_pred_flag");
        writeBool(writer, redundant_pic_cnt_present_flag, "PPS: redundant_pic_cnt_present_flag");
        if (extended != null) {
            writeBool(writer, extended.transform_8x8_mode_flag, "PPS: transform_8x8_mode_flag");
            writeBool(writer, extended.scalindMatrix != null, "PPS: scalindMatrix");
            if (extended.scalindMatrix != null) {
                for (int i = 0; i < 6 + 2 * (extended.transform_8x8_mode_flag ? 1 : 0); i++) {
                    if (i < 6) {

                        writeBool(writer, extended.scalindMatrix.ScalingList4x4[i] != null, "PPS: ");
                        if (extended.scalindMatrix.ScalingList4x4[i] != null) {
                            extended.scalindMatrix.ScalingList4x4[i].write(writer);
                        }

                    } else {

                        writeBool(writer, extended.scalindMatrix.ScalingList8x8[i - 6] != null, "PPS: ");
                        if (extended.scalindMatrix.ScalingList8x8[i - 6] != null) {
                            extended.scalindMatrix.ScalingList8x8[i - 6].write(writer);
                        }
                    }
                }
            }
            writeSEtrace(writer, extended.second_chroma_qp_index_offset, "PPS: ");
        }

        writeTrailingBits(writer);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bottom_right);
        result = prime * result + chroma_qp_index_offset;
        result = prime * result + (constrained_intra_pred_flag ? 1231 : 1237);
        result = prime * result + (deblocking_filter_control_present_flag ? 1231 : 1237);
        result = prime * result + (entropy_coding_mode_flag ? 1231 : 1237);
        result = prime * result + ((extended == null) ? 0 : extended.hashCode());
        result = prime * result + num_ref_idx_active_minus1[0];
        result = prime * result + num_ref_idx_active_minus1[1];
        result = prime * result + num_slice_groups_minus1;
        result = prime * result + pic_init_qp_minus26;
        result = prime * result + pic_init_qs_minus26;
        result = prime * result + (pic_order_present_flag ? 1231 : 1237);
        result = prime * result + pic_parameter_set_id;
        result = prime * result + (redundant_pic_cnt_present_flag ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(run_length_minus1);
        result = prime * result + seq_parameter_set_id;
        result = prime * result + (slice_group_change_direction_flag ? 1231 : 1237);
        result = prime * result + slice_group_change_rate_minus1;
        result = prime * result + Arrays.hashCode(slice_group_id);
        result = prime * result + slice_group_map_type;
        result = prime * result + Arrays.hashCode(top_left);
        result = prime * result + weighted_bipred_idc;
        result = prime * result + (weighted_pred_flag ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PictureParameterSet other = (PictureParameterSet) obj;
        if (!Platform.arrayEquals(bottom_right, other.bottom_right))
            return false;
        if (chroma_qp_index_offset != other.chroma_qp_index_offset)
            return false;
        if (constrained_intra_pred_flag != other.constrained_intra_pred_flag)
            return false;
        if (deblocking_filter_control_present_flag != other.deblocking_filter_control_present_flag)
            return false;
        if (entropy_coding_mode_flag != other.entropy_coding_mode_flag)
            return false;
        if (extended == null) {
            if (other.extended != null)
                return false;
        } else if (!extended.equals(other.extended))
            return false;
        if (num_ref_idx_active_minus1[0] != other.num_ref_idx_active_minus1[0])
            return false;
        if (num_ref_idx_active_minus1[1] != other.num_ref_idx_active_minus1[1])
            return false;
        if (num_slice_groups_minus1 != other.num_slice_groups_minus1)
            return false;
        if (pic_init_qp_minus26 != other.pic_init_qp_minus26)
            return false;
        if (pic_init_qs_minus26 != other.pic_init_qs_minus26)
            return false;
        if (pic_order_present_flag != other.pic_order_present_flag)
            return false;
        if (pic_parameter_set_id != other.pic_parameter_set_id)
            return false;
        if (redundant_pic_cnt_present_flag != other.redundant_pic_cnt_present_flag)
            return false;
        if (!Platform.arrayEquals(run_length_minus1, other.run_length_minus1))
            return false;
        if (seq_parameter_set_id != other.seq_parameter_set_id)
            return false;
        if (slice_group_change_direction_flag != other.slice_group_change_direction_flag)
            return false;
        if (slice_group_change_rate_minus1 != other.slice_group_change_rate_minus1)
            return false;
        if (!Platform.arrayEquals(slice_group_id, other.slice_group_id))
            return false;
        if (slice_group_map_type != other.slice_group_map_type)
            return false;
        if (!Platform.arrayEquals(top_left, other.top_left))
            return false;
        if (weighted_bipred_idc != other.weighted_bipred_idc)
            return false;
        if (weighted_pred_flag != other.weighted_pred_flag)
            return false;
        return true;
    }

    public PictureParameterSet copy() {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        write(buf);
        buf.flip();
        return read(buf);
    }

    public boolean isEntropy_coding_mode_flag() {
        return entropy_coding_mode_flag;
    }

    public int[] getNum_ref_idx_active_minus1() {
        return num_ref_idx_active_minus1;
    }

    public int getSlice_group_change_rate_minus1() {
        return slice_group_change_rate_minus1;
    }

    public int getPic_parameter_set_id() {
        return pic_parameter_set_id;
    }

    public int getSeq_parameter_set_id() {
        return seq_parameter_set_id;
    }

    public boolean isPic_order_present_flag() {
        return pic_order_present_flag;
    }

    public int getNum_slice_groups_minus1() {
        return num_slice_groups_minus1;
    }

    public int getSlice_group_map_type() {
        return slice_group_map_type;
    }

    public boolean isWeighted_pred_flag() {
        return weighted_pred_flag;
    }

    public int getWeighted_bipred_idc() {
        return weighted_bipred_idc;
    }

    public int getPic_init_qp_minus26() {
        return pic_init_qp_minus26;
    }

    public int getPic_init_qs_minus26() {
        return pic_init_qs_minus26;
    }

    public int getChroma_qp_index_offset() {
        return chroma_qp_index_offset;
    }

    public boolean isDeblocking_filter_control_present_flag() {
        return deblocking_filter_control_present_flag;
    }

    public boolean isConstrained_intra_pred_flag() {
        return constrained_intra_pred_flag;
    }

    public boolean isRedundant_pic_cnt_present_flag() {
        return redundant_pic_cnt_present_flag;
    }

    public int[] getTop_left() {
        return top_left;
    }

    public int[] getBottom_right() {
        return bottom_right;
    }

    public int[] getRun_length_minus1() {
        return run_length_minus1;
    }

    public boolean isSlice_group_change_direction_flag() {
        return slice_group_change_direction_flag;
    }

    public int[] getSlice_group_id() {
        return slice_group_id;
    }

    public PPSExt getExtended() {
        return extended;
    }
}
