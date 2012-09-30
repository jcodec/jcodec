package org.jcodec.codecs.h264.io.model;

import static org.jcodec.codecs.h264.io.read.CAVLCReader.moreRBSPData;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readTrailingBits;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readU;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readUE;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeBool;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeNBit;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSE;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeU;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Picture Parameter Set entity of H264 bitstream
 * 
 * capable to serialize / deserialize with CAVLC bitstream
 * 
 * @author Jay Codec
 * 
 */
public class PictureParameterSet extends BitstreamElement {

	public static class PPSExt {
		public boolean transform_8x8_mode_flag;
		public ScalingMatrix scalindMatrix;
		public int second_chroma_qp_index_offset;
		public boolean[] pic_scaling_list_present_flag;
	}

	public boolean entropy_coding_mode_flag;
	public int num_ref_idx_l0_active_minus1;
	public int num_ref_idx_l1_active_minus1;
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

	public static PictureParameterSet read(InputStream is) throws IOException {
		InBits in = new BitstreamReader(is);
		PictureParameterSet pps = new PictureParameterSet();

		pps.pic_parameter_set_id = readUE(in, "PPS: pic_parameter_set_id");
		pps.seq_parameter_set_id = readUE(in, "PPS: seq_parameter_set_id");
		pps.entropy_coding_mode_flag = 
				readBool(in, "PPS: entropy_coding_mode_flag");
		pps.pic_order_present_flag = 
				readBool(in, "PPS: pic_order_present_flag");
		pps.num_slice_groups_minus1 = 
				readUE(in, "PPS: num_slice_groups_minus1");
		if (pps.num_slice_groups_minus1 > 0) {
			pps.slice_group_map_type = 
					readUE(in, "PPS: slice_group_map_type");
			pps.top_left = new int[pps.num_slice_groups_minus1 + 1];
			pps.bottom_right = new int[pps.num_slice_groups_minus1 + 1];
			pps.run_length_minus1 = new int[pps.num_slice_groups_minus1 + 1];
			if (pps.slice_group_map_type == 0)
				for (int iGroup = 0; iGroup <= pps.num_slice_groups_minus1; iGroup++)
					pps.run_length_minus1[iGroup] = 
							readUE(in, "PPS: run_length_minus1");
			else if (pps.slice_group_map_type == 2)
				for (int iGroup = 0; iGroup < pps.num_slice_groups_minus1; iGroup++) {
					pps.top_left[iGroup] = readUE(in, "PPS: top_left");
					pps.bottom_right[iGroup] = 
							readUE(in, "PPS: bottom_right");
				}
			else if (pps.slice_group_map_type == 3
					|| pps.slice_group_map_type == 4
					|| pps.slice_group_map_type == 5) {
				pps.slice_group_change_direction_flag = 
						readBool(in, "PPS: slice_group_change_direction_flag");
				pps.slice_group_change_rate_minus1 = 
						readUE(in, "PPS: slice_group_change_rate_minus1");
			} else if (pps.slice_group_map_type == 6) {
				int NumberBitsPerSliceGroupId;
				if (pps.num_slice_groups_minus1 + 1 > 4)
					NumberBitsPerSliceGroupId = 3;
				else if (pps.num_slice_groups_minus1 + 1 > 2)
					NumberBitsPerSliceGroupId = 2;
				else
					NumberBitsPerSliceGroupId = 1;
				int pic_size_in_map_units_minus1 = 
						readUE(in, "PPS: pic_size_in_map_units_minus1");
				pps.slice_group_id = new int[pic_size_in_map_units_minus1 + 1];
				for (int i = 0; i <= pic_size_in_map_units_minus1; i++) {
					pps.slice_group_id[i] = readU(in,
							NumberBitsPerSliceGroupId, "PPS: slice_group_id ["
									+ i + "]f");
				}
			}
		}
		pps.num_ref_idx_l0_active_minus1 = 
				readUE(in, "PPS: num_ref_idx_l0_active_minus1");
		pps.num_ref_idx_l1_active_minus1 = 
				readUE(in, "PPS: num_ref_idx_l1_active_minus1");
		pps.weighted_pred_flag = readBool(in, "PPS: weighted_pred_flag");
		pps.weighted_bipred_idc = readNBit(in, 2,
				"PPS: weighted_bipred_idc");
		pps.pic_init_qp_minus26 = readSE(in, "PPS: pic_init_qp_minus26");
		pps.pic_init_qs_minus26 = readSE(in, "PPS: pic_init_qs_minus26");
		pps.chroma_qp_index_offset = 
				readSE(in, "PPS: chroma_qp_index_offset");
		pps.deblocking_filter_control_present_flag = 
				readBool(in, "PPS: deblocking_filter_control_present_flag");
		pps.constrained_intra_pred_flag = 
				readBool(in, "PPS: constrained_intra_pred_flag");
		pps.redundant_pic_cnt_present_flag = 
				readBool(in, "PPS: redundant_pic_cnt_present_flag");
		if (moreRBSPData(in)) {
			pps.extended = new PictureParameterSet.PPSExt();
			pps.extended.transform_8x8_mode_flag = 
					readBool(in, "PPS: transform_8x8_mode_flag");
			boolean pic_scaling_matrix_present_flag = 
					readBool(in, "PPS: pic_scaling_matrix_present_flag");
			if (pic_scaling_matrix_present_flag) {
				for (int i = 0; i < 6 + 2 * (pps.extended.transform_8x8_mode_flag ? 1
						: 0); i++) {
					boolean pic_scaling_list_present_flag = 
							readBool(in, "PPS: pic_scaling_list_present_flag");
					if (pic_scaling_list_present_flag) {
						pps.extended.scalindMatrix.ScalingList4x4 = new ScalingList[8];
						pps.extended.scalindMatrix.ScalingList8x8 = new ScalingList[8];
						if (i < 6) {
							pps.extended.scalindMatrix.ScalingList4x4[i] = ScalingList
									.read(in, 16);
						} else {
							pps.extended.scalindMatrix.ScalingList8x8[i - 6] = ScalingList
									.read(in, 64);
						}
					}
				}
			}
			pps.extended.second_chroma_qp_index_offset = 
					readSE(in, "PPS: second_chroma_qp_index_offset");
		}

		readTrailingBits(in);

		return pps;
	}

	public void write(OutputStream out) throws IOException {
		OutBits writer = new BitstreamWriter(out);

		writeUE(writer, pic_parameter_set_id, "PPS: pic_parameter_set_id");
		writeUE(writer, seq_parameter_set_id, "PPS: seq_parameter_set_id");
		writeBool(writer, entropy_coding_mode_flag,
				"PPS: entropy_coding_mode_flag");
		writeBool(writer, pic_order_present_flag, "PPS: pic_order_present_flag");
		writeUE(writer, num_slice_groups_minus1, "PPS: num_slice_groups_minus1");
		if (num_slice_groups_minus1 > 0) {
			writeUE(writer, slice_group_map_type, "PPS: slice_group_map_type");
			int[] top_left = new int[1];
			int[] bottom_right = new int[1];
			int[] run_length_minus1 = new int[1];
			if (slice_group_map_type == 0) {
				for (int iGroup = 0; iGroup <= num_slice_groups_minus1; iGroup++) {
					writeUE(writer, run_length_minus1[iGroup], "PPS: ");
				}
			} else if (slice_group_map_type == 2) {
				for (int iGroup = 0; iGroup < num_slice_groups_minus1; iGroup++) {
					writeUE(writer, top_left[iGroup], "PPS: ");
					writeUE(writer, bottom_right[iGroup], "PPS: ");
				}
			} else if (slice_group_map_type == 3 || slice_group_map_type == 4
					|| slice_group_map_type == 5) {
				writeBool(writer, slice_group_change_direction_flag,
						"PPS: slice_group_change_direction_flag");
				writeUE(writer, slice_group_change_rate_minus1,
						"PPS: slice_group_change_rate_minus1");
			} else if (slice_group_map_type == 6) {
				int NumberBitsPerSliceGroupId;
				if (num_slice_groups_minus1 + 1 > 4)
					NumberBitsPerSliceGroupId = 3;
				else if (num_slice_groups_minus1 + 1 > 2)
					NumberBitsPerSliceGroupId = 2;
				else
					NumberBitsPerSliceGroupId = 1;
				writeUE(writer, slice_group_id.length, "PPS: ");
				for (int i = 0; i <= slice_group_id.length; i++) {
					writeU(writer, slice_group_id[i], NumberBitsPerSliceGroupId);
				}
			}
		}
		writeUE(writer, num_ref_idx_l0_active_minus1,
				"PPS: num_ref_idx_l0_active_minus1");
		writeUE(writer, num_ref_idx_l1_active_minus1,
				"PPS: num_ref_idx_l1_active_minus1");
		writeBool(writer, weighted_pred_flag, "PPS: weighted_pred_flag");
		writeNBit(writer, weighted_bipred_idc, 2, "PPS: weighted_bipred_idc");
		writeSE(writer, pic_init_qp_minus26, "PPS: pic_init_qp_minus26");
		writeSE(writer, pic_init_qs_minus26, "PPS: pic_init_qs_minus26");
		writeSE(writer, chroma_qp_index_offset, "PPS: chroma_qp_index_offset");
		writeBool(writer, deblocking_filter_control_present_flag,
				"PPS: deblocking_filter_control_present_flag");
		writeBool(writer, constrained_intra_pred_flag,
				"PPS: constrained_intra_pred_flag");
		writeBool(writer, redundant_pic_cnt_present_flag,
				"PPS: redundant_pic_cnt_present_flag");
		if (extended != null) {
			writeBool(writer, extended.transform_8x8_mode_flag,
					"PPS: transform_8x8_mode_flag");
			writeBool(writer, extended.scalindMatrix != null,
					"PPS: scalindMatrix");
			if (extended.scalindMatrix != null) {
				for (int i = 0; i < 6 + 2 * (extended.transform_8x8_mode_flag ? 1
						: 0); i++) {
					if (i < 6) {
						
								writeBool(writer,
										extended.scalindMatrix.ScalingList4x4[i] != null,
										"PPS: ");
						if (extended.scalindMatrix.ScalingList4x4[i] != null) {
							extended.scalindMatrix.ScalingList4x4[i]
									.write(writer);
						}

					} else {
						
								writeBool(writer,
										extended.scalindMatrix.ScalingList8x8[i - 6] != null,
										"PPS: ");
						if (extended.scalindMatrix.ScalingList8x8[i - 6] != null) {
							extended.scalindMatrix.ScalingList8x8[i - 6]
									.write(writer);
						}
					}
				}
			}
			writeSE(writer, extended.second_chroma_qp_index_offset, "PPS: ");
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
		result = prime * result
				+ (deblocking_filter_control_present_flag ? 1231 : 1237);
		result = prime * result + (entropy_coding_mode_flag ? 1231 : 1237);
		result = prime * result
				+ ((extended == null) ? 0 : extended.hashCode());
		result = prime * result + num_ref_idx_l0_active_minus1;
		result = prime * result + num_ref_idx_l1_active_minus1;
		result = prime * result + num_slice_groups_minus1;
		result = prime * result + pic_init_qp_minus26;
		result = prime * result + pic_init_qs_minus26;
		result = prime * result + (pic_order_present_flag ? 1231 : 1237);
		result = prime * result + pic_parameter_set_id;
		result = prime * result
				+ (redundant_pic_cnt_present_flag ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(run_length_minus1);
		result = prime * result + seq_parameter_set_id;
		result = prime * result
				+ (slice_group_change_direction_flag ? 1231 : 1237);
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
		if (!Arrays.equals(bottom_right, other.bottom_right))
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
		if (num_ref_idx_l0_active_minus1 != other.num_ref_idx_l0_active_minus1)
			return false;
		if (num_ref_idx_l1_active_minus1 != other.num_ref_idx_l1_active_minus1)
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
		if (!Arrays.equals(run_length_minus1, other.run_length_minus1))
			return false;
		if (seq_parameter_set_id != other.seq_parameter_set_id)
			return false;
		if (slice_group_change_direction_flag != other.slice_group_change_direction_flag)
			return false;
		if (slice_group_change_rate_minus1 != other.slice_group_change_rate_minus1)
			return false;
		if (!Arrays.equals(slice_group_id, other.slice_group_id))
			return false;
		if (slice_group_map_type != other.slice_group_map_type)
			return false;
		if (!Arrays.equals(top_left, other.top_left))
			return false;
		if (weighted_bipred_idc != other.weighted_bipred_idc)
			return false;
		if (weighted_pred_flag != other.weighted_pred_flag)
			return false;
		return true;
	}

    public PictureParameterSet copy() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            write(bytes);
            return read(new ByteArrayInputStream(bytes.toByteArray()));
        } catch (IOException e) {
            return null;
        }
    }
}
