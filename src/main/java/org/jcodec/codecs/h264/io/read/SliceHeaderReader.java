package org.jcodec.codecs.h264.io.read;

import static org.jcodec.codecs.h264.io.model.RefPicReordering.InstrType.BACKWARD;
import static org.jcodec.codecs.h264.io.model.RefPicReordering.InstrType.FORWARD;
import static org.jcodec.codecs.h264.io.model.RefPicReordering.InstrType.LONG_TERM;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readU;
import static org.jcodec.codecs.h264.io.read.CAVLCReader.readUE;

import java.io.IOException;
import java.util.ArrayList;

import org.jcodec.codecs.h264.StreamParams;
import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.PredictionWeightTable;
import org.jcodec.codecs.h264.io.model.PredictionWeightTable.OffsetWeight;
import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType;
import org.jcodec.codecs.h264.io.model.RefPicMarking.Instruction;
import org.jcodec.codecs.h264.io.model.RefPicMarkingIDR;
import org.jcodec.codecs.h264.io.model.RefPicReordering;
import org.jcodec.codecs.h264.io.model.RefPicReordering.ReorderOp;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads header of the coded slice
 * 
 * @author Jay Codec
 * 
 */
public class SliceHeaderReader {
    private StreamParams streamParams;

    public SliceHeaderReader(StreamParams demuxer) {
        this.streamParams = demuxer;
    }

    public SliceHeader read(NALUnit nalUnit, InBits in) throws IOException {

        SliceHeader sh = new SliceHeader();
        sh.first_mb_in_slice = readUE(in, "SH: first_mb_in_slice");
        int sh_type = readUE(in, "SH: slice_type");
        sh.slice_type = SliceType.fromValue(sh_type % 5);
        sh.slice_type_restr = (sh_type / 5) > 0;

        sh.pic_parameter_set_id = readUE(in, "SH: pic_parameter_set_id");

        PictureParameterSet pps = streamParams.getPPS(sh.pic_parameter_set_id);
        SeqParameterSet sps = streamParams.getSPS(pps.seq_parameter_set_id);

        sh.pps = pps;
        sh.sps = sps;

        sh.frame_num = readU(in, sps.log2_max_frame_num_minus4 + 4, "SH: frame_num");
        if (!sps.frame_mbs_only_flag) {
            sh.field_pic_flag = readBool(in, "SH: field_pic_flag");
            if (sh.field_pic_flag) {
                sh.bottom_field_flag = readBool(in, "SH: bottom_field_flag");
            }
        }
        if (nalUnit.type == NALUnitType.IDR_SLICE) {
            sh.idr_pic_id = readUE(in, "SH: idr_pic_id");
        }
        if (sps.pic_order_cnt_type == 0) {
            sh.pic_order_cnt_lsb = readU(in, sps.log2_max_pic_order_cnt_lsb_minus4 + 4, "SH: pic_order_cnt_lsb");
            if (pps.pic_order_present_flag && !sps.field_pic_flag) {
                sh.delta_pic_order_cnt_bottom = readSE(in, "SH: delta_pic_order_cnt_bottom");
            }
        }
        sh.delta_pic_order_cnt = new int[2];
        if (sps.pic_order_cnt_type == 1 && !sps.delta_pic_order_always_zero_flag) {
            sh.delta_pic_order_cnt[0] = readSE(in, "SH: delta_pic_order_cnt[0]");
            if (pps.pic_order_present_flag && !sps.field_pic_flag)
                sh.delta_pic_order_cnt[1] = readSE(in, "SH: delta_pic_order_cnt[1]");
        }
        if (pps.redundant_pic_cnt_present_flag) {
            sh.redundant_pic_cnt = readUE(in, "SH: redundant_pic_cnt");
        }
        if (sh.slice_type == SliceType.B) {
            sh.direct_spatial_mv_pred_flag = readBool(in, "SH: direct_spatial_mv_pred_flag");
        }
        if (sh.slice_type == SliceType.P || sh.slice_type == SliceType.SP || sh.slice_type == SliceType.B) {
            sh.num_ref_idx_active_override_flag = readBool(in, "SH: num_ref_idx_active_override_flag");
            if (sh.num_ref_idx_active_override_flag) {
                sh.num_ref_idx_l0_active_minus1 = readUE(in, "SH: num_ref_idx_l0_active_minus1");
                if (sh.slice_type == SliceType.B) {
                    sh.num_ref_idx_l1_active_minus1 = readUE(in, "SH: num_ref_idx_l1_active_minus1");
                }
            }
        }
        readRefPicListReordering(sh, in);
        if ((pps.weighted_pred_flag && (sh.slice_type == SliceType.P || sh.slice_type == SliceType.SP))
                || (pps.weighted_bipred_idc == 1 && sh.slice_type == SliceType.B))
            readPredWeightTable(sps, pps, sh, in);
        if (nalUnit.nal_ref_idc != 0)
            readDecoderPicMarking(nalUnit, sh, in);
        if (pps.entropy_coding_mode_flag && sh.slice_type.isInter()) {
            sh.cabac_init_idc = readUE(in, "SH: cabac_init_idc");
        }
        sh.slice_qp_delta = readSE(in, "SH: slice_qp_delta");
        if (sh.slice_type == SliceType.SP || sh.slice_type == SliceType.SI) {
            if (sh.slice_type == SliceType.SP) {
                sh.sp_for_switch_flag = readBool(in, "SH: sp_for_switch_flag");
            }
            sh.slice_qs_delta = readSE(in, "SH: slice_qs_delta");
        }
        if (pps.deblocking_filter_control_present_flag) {
            sh.disable_deblocking_filter_idc = readUE(in, "SH: disable_deblocking_filter_idc");
            if (sh.disable_deblocking_filter_idc != 1) {
                sh.slice_alpha_c0_offset_div2 = readSE(in, "SH: slice_alpha_c0_offset_div2");
                sh.slice_beta_offset_div2 = readSE(in, "SH: slice_beta_offset_div2");
            }
        }
        if (pps.num_slice_groups_minus1 > 0 && pps.slice_group_map_type >= 3 && pps.slice_group_map_type <= 5) {
            int len = (sps.pic_height_in_map_units_minus1 + 1) * (sps.pic_width_in_mbs_minus1 + 1)
                    / (pps.slice_group_change_rate_minus1 + 1);
            if (((sps.pic_height_in_map_units_minus1 + 1) * (sps.pic_width_in_mbs_minus1 + 1))
                    % (pps.slice_group_change_rate_minus1 + 1) > 0)
                len += 1;

            len = CeilLog2(len + 1);
            sh.slice_group_change_cycle = readU(in, len, "SH: slice_group_change_cycle");
        }

        return sh;
    }

    private static int CeilLog2(int uiVal) {
        int uiTmp = uiVal - 1;
        int uiRet = 0;

        while (uiTmp != 0) {
            uiTmp >>= 1;
            uiRet++;
        }
        return uiRet;
    }

    static int i = 0;

    private static void readDecoderPicMarking(NALUnit nalUnit, SliceHeader sh, InBits in) throws IOException {
        if (nalUnit.type == NALUnitType.IDR_SLICE) {
            boolean no_output_of_prior_pics_flag = readBool(in, "SH: no_output_of_prior_pics_flag");
            boolean long_term_reference_flag = readBool(in, "SH: long_term_reference_flag");
            sh.refPicMarkingIDR = new RefPicMarkingIDR(no_output_of_prior_pics_flag, long_term_reference_flag);
        } else {
            boolean adaptive_ref_pic_marking_mode_flag = readBool(in, "SH: adaptive_ref_pic_marking_mode_flag");
            if (adaptive_ref_pic_marking_mode_flag) {
                ArrayList<Instruction> mmops = new ArrayList<Instruction>();
                int memory_management_control_operation;
                do {
                    memory_management_control_operation = readUE(in, "SH: memory_management_control_operation");

                    Instruction instr = null;

                    switch (memory_management_control_operation) {
                    case 1:
                        instr = new RefPicMarking.Instruction(InstrType.REMOVE_SHORT, readUE(in,
                                "SH: difference_of_pic_nums_minus1") + 1, 0);
                        break;
                    case 2:
                        instr = new RefPicMarking.Instruction(InstrType.REMOVE_LONG,
                                readUE(in, "SH: long_term_pic_num"), 0);
                        break;
                    case 3:
                        instr = new RefPicMarking.Instruction(InstrType.CONVERT_INTO_LONG, readUE(in,
                                "SH: difference_of_pic_nums_minus1") + 1, readUE(in, "SH: long_term_frame_idx"));
                        break;
                    case 4:
                        instr = new RefPicMarking.Instruction(InstrType.TRUNK_LONG, readUE(in,
                                "SH: max_long_term_frame_idx_plus1") - 1, 0);
                        break;
                    case 5:
                        instr = new RefPicMarking.Instruction(InstrType.CLEAR, 0, 0);
                        break;
                    case 6:
                        instr = new RefPicMarking.Instruction(InstrType.MARK_LONG,
                                readUE(in, "SH: long_term_frame_idx"), 0);
                        break;
                    }
                    if (instr != null)
                        mmops.add(instr);
                } while (memory_management_control_operation != 0);
                sh.refPicMarkingNonIDR = new RefPicMarking(mmops.toArray(new Instruction[] {}));
            }
        }
    }

    private static void readPredWeightTable(SeqParameterSet sps, PictureParameterSet pps, SliceHeader sh, InBits in)
            throws IOException {
        sh.pred_weight_table = new PredictionWeightTable();
        sh.pred_weight_table.luma_log2_weight_denom = readUE(in, "SH: luma_log2_weight_denom");
        if (sps.chroma_format_idc != ChromaFormat.MONOCHROME) {
            sh.pred_weight_table.chroma_log2_weight_denom = readUE(in, "SH: chroma_log2_weight_denom");
        }
        int numRefsL0 = sh.num_ref_idx_active_override_flag ? sh.num_ref_idx_l0_active_minus1 + 1
                : pps.num_ref_idx_l0_active_minus1 + 1;
        sh.pred_weight_table.luma_offset_weight_l0 = new OffsetWeight[numRefsL0];
        sh.pred_weight_table.chroma_offset_weight_l0 = new OffsetWeight[numRefsL0][];
        for (int i = 0; i < numRefsL0; i++) {
            boolean luma_weight_l0_flag = readBool(in, "SH: luma_weight_l0_flag");
            if (luma_weight_l0_flag) {
                OffsetWeight ow = new OffsetWeight();
                ow.weight = readSE(in, "SH: weight");
                ow.offset = readSE(in, "SH: offset");

                sh.pred_weight_table.luma_offset_weight_l0[i] = ow;
            }
            if (sps.chroma_format_idc != ChromaFormat.MONOCHROME) {
                boolean chroma_weight_l0_flag = readBool(in, "SH: chroma_weight_l0_flag");
                if (chroma_weight_l0_flag) {
                    sh.pred_weight_table.chroma_offset_weight_l0[i] = new OffsetWeight[2];
                    for (int j = 0; j < 2; j++) {
                        OffsetWeight ow = new OffsetWeight();
                        ow.weight = readSE(in, "SH: weight");
                        ow.offset = readSE(in, "SH: offset");
                        sh.pred_weight_table.chroma_offset_weight_l0[i][j] = ow;
                    }
                }
            }
        }
        if (sh.slice_type == SliceType.B) {
            int numRefsL1 = sh.num_ref_idx_active_override_flag ? sh.num_ref_idx_l1_active_minus1 + 1
                    : pps.num_ref_idx_l1_active_minus1 + 1;
            sh.pred_weight_table.luma_offset_weight_l1 = new OffsetWeight[numRefsL1];
            sh.pred_weight_table.chroma_offset_weight_l1 = new OffsetWeight[numRefsL1][];
            for (int i = 0; i < numRefsL1; i++) {
                boolean luma_weight_l1_flag = readBool(in, "SH: luma_weight_l1_flag");
                if (luma_weight_l1_flag) {
                    OffsetWeight ow = new OffsetWeight();
                    ow.weight = readSE(in, "SH: weight");
                    ow.offset = readSE(in, "SH: offset");
                    sh.pred_weight_table.luma_offset_weight_l1[i] = ow;
                }
                if (sps.chroma_format_idc != ChromaFormat.MONOCHROME) {
                    boolean chroma_weight_l1_flag = readBool(in, "SH: chroma_weight_l1_flag");
                    if (chroma_weight_l1_flag) {
                        sh.pred_weight_table.chroma_offset_weight_l1[i] = new OffsetWeight[2];
                        for (int j = 0; j < 2; j++) {
                            OffsetWeight ow = new OffsetWeight();
                            ow.weight = readSE(in, "SH: weight");
                            ow.offset = readSE(in, "SH: offset");
                            sh.pred_weight_table.chroma_offset_weight_l1[i][j] = ow;
                        }
                    }
                }
            }
        }
    }

    private static void readRefPicListReordering(SliceHeader sh, InBits in) throws IOException {
        System.out.println(i++);
        if (sh.slice_type.isInter()) {
            boolean ref_pic_list_reordering_flag_l0 = readBool(in, "SH: ref_pic_list_reordering_flag_l0");
            if (ref_pic_list_reordering_flag_l0) {
                sh.refPicReorderingL0 = readReorderingEntries(in);
            }
        }
        if (sh.slice_type == SliceType.B) {
            boolean ref_pic_list_reordering_flag_l1 = readBool(in, "SH: ref_pic_list_reordering_flag_l1");
            if (ref_pic_list_reordering_flag_l1) {
                sh.refPicReorderingL1 = readReorderingEntries(in);
            }
        }
    }

    private static RefPicReordering readReorderingEntries(InBits in) throws IOException {
        ArrayList<ReorderOp> reordering = new ArrayList<ReorderOp>();
        int reordering_of_pic_nums_idc;
        do {
            reordering_of_pic_nums_idc = readUE(in, "SH: reordering_of_pic_nums_idc");
            switch (reordering_of_pic_nums_idc) {
            case 0:
                reordering.add(new ReorderOp(BACKWARD, readUE(in, "SH: abs_diff_pic_num_minus1") + 1));
                break;
            case 1:
                reordering.add(new ReorderOp(FORWARD, readUE(in, "SH: abs_diff_pic_num_minus1") + 1));
                break;
            case 2:
                reordering.add(new ReorderOp(LONG_TERM, readUE(in, "SH: long_term_pic_num")));
                break;
            }
        } while (reordering_of_pic_nums_idc != 3);
        return new RefPicReordering(reordering.toArray(new ReorderOp[] {}));
    }
}
