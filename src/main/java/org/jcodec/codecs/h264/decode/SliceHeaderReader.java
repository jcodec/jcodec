package org.jcodec.codecs.h264.decode;
import static org.jcodec.codecs.h264.io.model.SeqParameterSet.getPicHeightInMbs;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readU;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUEtrace;
import static org.jcodec.common.model.ColorSpace.MONO;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.PredictionWeightTable;
import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType;
import org.jcodec.codecs.h264.io.model.RefPicMarking.Instruction;
import org.jcodec.codecs.h264.io.model.RefPicMarkingIDR;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.io.BitReader;

import js.util.ArrayList;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads header of the coded slice
 * 
 * @author The JCodec project
 * 
 */
public class SliceHeaderReader {

    public SliceHeader readPart1(BitReader _in) {

        SliceHeader sh = new SliceHeader();
        sh.first_mb_in_slice = readUEtrace(_in, "SH: first_mb_in_slice");
        int sh_type = readUEtrace(_in, "SH: slice_type");
        sh.slice_type = SliceType.fromValue(sh_type % 5);
        sh.slice_type_restr = (sh_type / 5) > 0;

        sh.pic_parameter_set_id = readUEtrace(_in, "SH: pic_parameter_set_id");

        return sh;
    }

    public SliceHeader readPart2(SliceHeader sh, NALUnit nalUnit, SeqParameterSet sps, PictureParameterSet pps,
            BitReader _in) {
        sh.pps = pps;
        sh.sps = sps;

        sh.frame_num = readU(_in, sps.log2_max_frame_num_minus4 + 4, "SH: frame_num");
        if (!sps.frame_mbs_only_flag) {
            sh.field_pic_flag = readBool(_in, "SH: field_pic_flag");
            if (sh.field_pic_flag) {
                sh.bottom_field_flag = readBool(_in, "SH: bottom_field_flag");
            }
        }
        if (nalUnit.type == NALUnitType.IDR_SLICE) {
            sh.idr_pic_id = readUEtrace(_in, "SH: idr_pic_id");
        }
        if (sps.pic_order_cnt_type == 0) {
            sh.pic_order_cnt_lsb = readU(_in, sps.log2_max_pic_order_cnt_lsb_minus4 + 4, "SH: pic_order_cnt_lsb");
            if (pps.pic_order_present_flag && !sps.field_pic_flag) {
                sh.delta_pic_order_cnt_bottom = readSE(_in, "SH: delta_pic_order_cnt_bottom");
            }
        }
        sh.delta_pic_order_cnt = new int[2];
        if (sps.pic_order_cnt_type == 1 && !sps.delta_pic_order_always_zero_flag) {
            sh.delta_pic_order_cnt[0] = readSE(_in, "SH: delta_pic_order_cnt[0]");
            if (pps.pic_order_present_flag && !sps.field_pic_flag)
                sh.delta_pic_order_cnt[1] = readSE(_in, "SH: delta_pic_order_cnt[1]");
        }
        if (pps.redundant_pic_cnt_present_flag) {
            sh.redundant_pic_cnt = readUEtrace(_in, "SH: redundant_pic_cnt");
        }
        if (sh.slice_type == SliceType.B) {
            sh.direct_spatial_mv_pred_flag = readBool(_in, "SH: direct_spatial_mv_pred_flag");
        }
        if (sh.slice_type == SliceType.P || sh.slice_type == SliceType.SP || sh.slice_type == SliceType.B) {
            sh.num_ref_idx_active_override_flag = readBool(_in, "SH: num_ref_idx_active_override_flag");
            if (sh.num_ref_idx_active_override_flag) {
                sh.num_ref_idx_active_minus1[0] = readUEtrace(_in, "SH: num_ref_idx_l0_active_minus1");
                if (sh.slice_type == SliceType.B) {
                    sh.num_ref_idx_active_minus1[1] = readUEtrace(_in, "SH: num_ref_idx_l1_active_minus1");
                }
            }
        }
        readRefPicListReordering(sh, _in);
        if ((pps.weighted_pred_flag && (sh.slice_type == SliceType.P || sh.slice_type == SliceType.SP))
                || (pps.weighted_bipred_idc == 1 && sh.slice_type == SliceType.B))
            readPredWeightTable(sps, pps, sh, _in);
        if (nalUnit.nal_ref_idc != 0)
            readDecoderPicMarking(nalUnit, sh, _in);
        if (pps.entropy_coding_mode_flag && sh.slice_type.isInter()) {
            sh.cabac_init_idc = readUEtrace(_in, "SH: cabac_init_idc");
        }
        sh.slice_qp_delta = readSE(_in, "SH: slice_qp_delta");
        if (sh.slice_type == SliceType.SP || sh.slice_type == SliceType.SI) {
            if (sh.slice_type == SliceType.SP) {
                sh.sp_for_switch_flag = readBool(_in, "SH: sp_for_switch_flag");
            }
            sh.slice_qs_delta = readSE(_in, "SH: slice_qs_delta");
        }
        if (pps.deblocking_filter_control_present_flag) {
            sh.disable_deblocking_filter_idc = readUEtrace(_in, "SH: disable_deblocking_filter_idc");
            if (sh.disable_deblocking_filter_idc != 1) {
                sh.slice_alpha_c0_offset_div2 = readSE(_in, "SH: slice_alpha_c0_offset_div2");
                sh.slice_beta_offset_div2 = readSE(_in, "SH: slice_beta_offset_div2");
            }
        }
        if (pps.num_slice_groups_minus1 > 0 && pps.slice_group_map_type >= 3 && pps.slice_group_map_type <= 5) {
            int len = getPicHeightInMbs(sps) * (sps.pic_width_in_mbs_minus1 + 1)
                    / (pps.slice_group_change_rate_minus1 + 1);
            if ((getPicHeightInMbs(sps) * (sps.pic_width_in_mbs_minus1 + 1))
                    % (pps.slice_group_change_rate_minus1 + 1) > 0)
                len += 1;

            len = CeilLog2(len + 1);
            sh.slice_group_change_cycle = readU(_in, len, "SH: slice_group_change_cycle");
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

    // static int i = 0;

    private static void readDecoderPicMarking(NALUnit nalUnit, SliceHeader sh, BitReader _in) {
        if (nalUnit.type == NALUnitType.IDR_SLICE) {
            boolean no_output_of_prior_pics_flag = readBool(_in, "SH: no_output_of_prior_pics_flag");
            boolean long_term_reference_flag = readBool(_in, "SH: long_term_reference_flag");
            sh.refPicMarkingIDR = new RefPicMarkingIDR(no_output_of_prior_pics_flag, long_term_reference_flag);
        } else {
            boolean adaptive_ref_pic_marking_mode_flag = readBool(_in, "SH: adaptive_ref_pic_marking_mode_flag");
            if (adaptive_ref_pic_marking_mode_flag) {
                ArrayList<Instruction> mmops = new ArrayList<Instruction>();
                int memory_management_control_operation;
                do {
                    memory_management_control_operation = readUEtrace(_in, "SH: memory_management_control_operation");

                    Instruction instr = null;

                    switch (memory_management_control_operation) {
                    case 1:
                        instr = new RefPicMarking.Instruction(InstrType.REMOVE_SHORT, readUEtrace(_in,
                                "SH: difference_of_pic_nums_minus1") + 1, 0);
                        break;
                    case 2:
                        instr = new RefPicMarking.Instruction(InstrType.REMOVE_LONG,
                                readUEtrace(_in, "SH: long_term_pic_num"), 0);
                        break;
                    case 3:
                        instr = new RefPicMarking.Instruction(InstrType.CONVERT_INTO_LONG, readUEtrace(_in,
                                "SH: difference_of_pic_nums_minus1") + 1, readUEtrace(_in, "SH: long_term_frame_idx"));
                        break;
                    case 4:
                        instr = new RefPicMarking.Instruction(InstrType.TRUNK_LONG, readUEtrace(_in,
                                "SH: max_long_term_frame_idx_plus1") - 1, 0);
                        break;
                    case 5:
                        instr = new RefPicMarking.Instruction(InstrType.CLEAR, 0, 0);
                        break;
                    case 6:
                        instr = new RefPicMarking.Instruction(InstrType.MARK_LONG,
                                readUEtrace(_in, "SH: long_term_frame_idx"), 0);
                        break;
                    }
                    if (instr != null)
                        mmops.add(instr);
                } while (memory_management_control_operation != 0);
                sh.refPicMarkingNonIDR = new RefPicMarking(mmops.toArray(new Instruction[] {}));
            }
        }
    }

    private static void readPredWeightTable(SeqParameterSet sps, PictureParameterSet pps, SliceHeader sh, BitReader _in) {
        sh.pred_weight_table = new PredictionWeightTable();
        int[] numRefsMinus1 = sh.num_ref_idx_active_override_flag ? sh.num_ref_idx_active_minus1
                : pps.num_ref_idx_active_minus1;
        int[] nr = new int[] {numRefsMinus1[0] + 1, numRefsMinus1[1] + 1};

        sh.pred_weight_table.luma_log2_weight_denom = readUEtrace(_in, "SH: luma_log2_weight_denom");
        if (sps.chroma_format_idc != MONO) {
            sh.pred_weight_table.chroma_log2_weight_denom = readUEtrace(_in, "SH: chroma_log2_weight_denom");
        }
        int defaultLW = 1 << sh.pred_weight_table.luma_log2_weight_denom;
        int defaultCW = 1 << sh.pred_weight_table.chroma_log2_weight_denom;

        for (int list = 0; list < 2; list++) {
            sh.pred_weight_table.luma_weight[list] = new int[nr[list]];
            sh.pred_weight_table.luma_offset[list] = new int[nr[list]];
            sh.pred_weight_table.chroma_weight[list] = new int[2][nr[list]];
            sh.pred_weight_table.chroma_offset[list] = new int[2][nr[list]];
            for (int i = 0; i < nr[list]; i++) {
                sh.pred_weight_table.luma_weight[list][i] = defaultLW;
                sh.pred_weight_table.luma_offset[list][i] = 0;
                sh.pred_weight_table.chroma_weight[list][0][i] = defaultCW;
                sh.pred_weight_table.chroma_offset[list][0][i] = 0;
                sh.pred_weight_table.chroma_weight[list][1][i] = defaultCW;
                sh.pred_weight_table.chroma_offset[list][1][i] = 0;
            }
        }

        readWeightOffset(sps, pps, sh, _in, nr, 0);
        if (sh.slice_type == SliceType.B) {
            readWeightOffset(sps, pps, sh, _in, nr, 1);
        }
    }

    private static void readWeightOffset(SeqParameterSet sps, PictureParameterSet pps, SliceHeader sh, BitReader _in,
            int[] numRefs, int list) {

        for (int i = 0; i < numRefs[list]; i++) {
            boolean luma_weight_l0_flag = readBool(_in, "SH: luma_weight_l0_flag");
            if (luma_weight_l0_flag) {
                sh.pred_weight_table.luma_weight[list][i] = readSE(_in, "SH: weight");
                sh.pred_weight_table.luma_offset[list][i] = readSE(_in, "SH: offset");
            }
            if (sps.chroma_format_idc != MONO) {
                boolean chroma_weight_l0_flag = readBool(_in, "SH: chroma_weight_l0_flag");
                if (chroma_weight_l0_flag) {
                    sh.pred_weight_table.chroma_weight[list][0][i] = readSE(_in, "SH: weight");
                    sh.pred_weight_table.chroma_offset[list][0][i] = readSE(_in, "SH: offset");
                    sh.pred_weight_table.chroma_weight[list][1][i] = readSE(_in, "SH: weight");
                    sh.pred_weight_table.chroma_offset[list][1][i] = readSE(_in, "SH: offset");
                }
            }
        }
    }

    private static void readRefPicListReordering(SliceHeader sh, BitReader _in) {
        sh.refPicReordering = new int[2][][];
        // System.out.println(i++);
        if (sh.slice_type.isInter()) {
            boolean ref_pic_list_reordering_flag_l0 = readBool(_in, "SH: ref_pic_list_reordering_flag_l0");
            if (ref_pic_list_reordering_flag_l0) {
                sh.refPicReordering[0] = readReorderingEntries(_in);
            }
        }
        if (sh.slice_type == SliceType.B) {
            boolean ref_pic_list_reordering_flag_l1 = readBool(_in, "SH: ref_pic_list_reordering_flag_l1");
            if (ref_pic_list_reordering_flag_l1) {
                sh.refPicReordering[1] = readReorderingEntries(_in);
            }
        }
    }

    private static int[][] readReorderingEntries(BitReader _in) {
        IntArrayList ops = IntArrayList.createIntArrayList();
        IntArrayList args = IntArrayList.createIntArrayList();
        do {
            int idc = readUEtrace(_in, "SH: reordering_of_pic_nums_idc");
            if (idc == 3)
                break;
            ops.add(idc);
            args.add(readUEtrace(_in, "SH: abs_diff_pic_num_minus1"));
        } while (true);
        return new int[][] { ops.toArray(), args.toArray() };
    }
}
