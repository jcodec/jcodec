package org.jcodec.codecs.h264.io.write

import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeBool
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSEtrace
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeU
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUEtrace
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUtrace
import org.jcodec.common.io.BitWriter
import org.jcodec.common.model.ColorSpace

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A writer for slice header data structure
 *
 * @author The JCodec project
 */
object SliceHeaderWriter {
    @JvmStatic
    fun write(sliceHeader: SliceHeader, idrSlice: Boolean, nalRefIdc: Int, writer: BitWriter) {
        val sps = sliceHeader.sps
        val pps = sliceHeader.pps
        writeUEtrace(writer, sliceHeader.firstMbInSlice, "SH: first_mb_in_slice")
        writeUEtrace(writer, sliceHeader.sliceType!!.ordinal() + if (sliceHeader.sliceTypeRestr) 5 else 0, "SH: slice_type")
        writeUEtrace(writer, sliceHeader.picParameterSetId, "SH: pic_parameter_set_id")
        require(sliceHeader.frameNum <= 1 shl sps!!.log2MaxFrameNumMinus4 + 4) { "frame_num > " + (1 shl sps.log2MaxFrameNumMinus4 + 4) }
        writeUtrace(writer, sliceHeader.frameNum, sps.log2MaxFrameNumMinus4 + 4, "SH: frame_num")
        if (!sps.isFrameMbsOnlyFlag) {
            writeBool(writer, sliceHeader.fieldPicFlag, "SH: field_pic_flag")
            if (sliceHeader.fieldPicFlag) {
                writeBool(writer, sliceHeader.bottomFieldFlag, "SH: bottom_field_flag")
            }
        }
        if (idrSlice) {
            writeUEtrace(writer, sliceHeader.idrPicId, "SH: idr_pic_id")
        }
        if (sps.picOrderCntType == 0) {
            require(sliceHeader.picOrderCntLsb <= 1 shl sps.log2MaxPicOrderCntLsbMinus4 + 4) { "pic_order_cnt_lsb > " + (1 shl sps.log2MaxPicOrderCntLsbMinus4 + 4) }
            writeU(writer, sliceHeader.picOrderCntLsb, sps.log2MaxPicOrderCntLsbMinus4 + 4)
            if (pps!!.isPicOrderPresentFlag && !sps.isFieldPicFlag) {
                writeSEtrace(writer, sliceHeader.deltaPicOrderCntBottom, "SH: delta_pic_order_cnt_bottom")
            }
        }
        if (sps.picOrderCntType == 1 && !sps.isDeltaPicOrderAlwaysZeroFlag) {
            writeSEtrace(writer, sliceHeader.deltaPicOrderCnt!![0], "SH: delta_pic_order_cnt")
            if (pps!!.isPicOrderPresentFlag && !sps.isFieldPicFlag) writeSEtrace(writer, sliceHeader.deltaPicOrderCnt!![1], "SH: delta_pic_order_cnt")
        }
        if (pps!!.isRedundantPicCntPresentFlag) {
            writeUEtrace(writer, sliceHeader.redundantPicCnt, "SH: redundant_pic_cnt")
        }
        if (sliceHeader.sliceType == SliceType.B) {
            writeBool(writer, sliceHeader.directSpatialMvPredFlag, "SH: direct_spatial_mv_pred_flag")
        }
        if (sliceHeader.sliceType == SliceType.P || sliceHeader.sliceType == SliceType.SP || sliceHeader.sliceType == SliceType.B) {
            writeBool(writer, sliceHeader.numRefIdxActiveOverrideFlag, "SH: num_ref_idx_active_override_flag")
            if (sliceHeader.numRefIdxActiveOverrideFlag) {
                writeUEtrace(writer, sliceHeader.numRefIdxActiveMinus1[0], "SH: num_ref_idx_l0_active_minus1")
                if (sliceHeader.sliceType == SliceType.B) {
                    writeUEtrace(writer, sliceHeader.numRefIdxActiveMinus1[1], "SH: num_ref_idx_l1_active_minus1")
                }
            }
        }
        writeRefPicListReordering(sliceHeader, writer)
        if (pps.isWeightedPredFlag && (sliceHeader.sliceType == SliceType.P || sliceHeader.sliceType == SliceType.SP)
                || pps.weightedBipredIdc == 1 && sliceHeader.sliceType == SliceType.B) writePredWeightTable(sliceHeader, writer)
        if (nalRefIdc != 0) writeDecRefPicMarking(sliceHeader, idrSlice, writer)
        if (pps.isEntropyCodingModeFlag && sliceHeader.sliceType!!.isInter) {
            writeUEtrace(writer, sliceHeader.cabacInitIdc, "SH: cabac_init_idc")
        }
        writeSEtrace(writer, sliceHeader.sliceQpDelta, "SH: slice_qp_delta")
        if (sliceHeader.sliceType == SliceType.SP || sliceHeader.sliceType == SliceType.SI) {
            if (sliceHeader.sliceType == SliceType.SP) {
                writeBool(writer, sliceHeader.spForSwitchFlag, "SH: sp_for_switch_flag")
            }
            writeSEtrace(writer, sliceHeader.sliceQsDelta, "SH: slice_qs_delta")
        }
        if (pps.isDeblockingFilterControlPresentFlag) {
            writeUEtrace(writer, sliceHeader.disableDeblockingFilterIdc, "SH: disable_deblocking_filter_idc")
            if (sliceHeader.disableDeblockingFilterIdc != 1) {
                writeSEtrace(writer, sliceHeader.sliceAlphaC0OffsetDiv2, "SH: slice_alpha_c0_offset_div2")
                writeSEtrace(writer, sliceHeader.sliceBetaOffsetDiv2, "SH: slice_beta_offset_div2")
            }
        }
        if (pps.numSliceGroupsMinus1 > 0 && pps.sliceGroupMapType >= 3 && pps.sliceGroupMapType <= 5) {
            var len = ((sps.picHeightInMapUnitsMinus1 + 1) * (sps.picWidthInMbsMinus1 + 1)
                    / (pps.sliceGroupChangeRateMinus1 + 1))
            if ((sps.picHeightInMapUnitsMinus1 + 1) * (sps.picWidthInMbsMinus1 + 1)
                    % (pps.sliceGroupChangeRateMinus1 + 1) > 0) len += 1
            len = CeilLog2(len + 1)
            writeU(writer, sliceHeader.sliceGroupChangeCycle, len)
        }
    }

    private fun CeilLog2(uiVal: Int): Int {
        var uiTmp = uiVal - 1
        var uiRet = 0
        while (uiTmp != 0) {
            uiTmp = uiTmp shr 1
            uiRet++
        }
        return uiRet
    }

    private fun writeDecRefPicMarking(sliceHeader: SliceHeader, idrSlice: Boolean, writer: BitWriter) {
        if (idrSlice) {
            val drpmidr = sliceHeader.refPicMarkingIDR
            writeBool(writer, drpmidr!!.isDiscardDecodedPics, "SH: no_output_of_prior_pics_flag")
            writeBool(writer, drpmidr.isUseForlongTerm, "SH: long_term_reference_flag")
        } else {
            writeBool(writer, sliceHeader.refPicMarkingNonIDR != null, "SH: adaptive_ref_pic_marking_mode_flag")
            if (sliceHeader.refPicMarkingNonIDR != null) {
                val drpmidr = sliceHeader.refPicMarkingNonIDR
                val instructions = drpmidr!!.instructions
                for (i in instructions.indices) {
                    val mmop = instructions[i]
                    when (mmop.type) {
                        InstrType.REMOVE_SHORT -> {
                            writeUEtrace(writer, 1, "SH: memory_management_control_operation")
                            writeUEtrace(writer, mmop.arg1 - 1, "SH: difference_of_pic_nums_minus1")
                        }
                        InstrType.REMOVE_LONG -> {
                            writeUEtrace(writer, 2, "SH: memory_management_control_operation")
                            writeUEtrace(writer, mmop.arg1, "SH: long_term_pic_num")
                        }
                        InstrType.CONVERT_INTO_LONG -> {
                            writeUEtrace(writer, 3, "SH: memory_management_control_operation")
                            writeUEtrace(writer, mmop.arg1 - 1, "SH: difference_of_pic_nums_minus1")
                            writeUEtrace(writer, mmop.arg2, "SH: long_term_frame_idx")
                        }
                        InstrType.TRUNK_LONG -> {
                            writeUEtrace(writer, 4, "SH: memory_management_control_operation")
                            writeUEtrace(writer, mmop.arg1 + 1, "SH: max_long_term_frame_idx_plus1")
                        }
                        InstrType.CLEAR -> writeUEtrace(writer, 5, "SH: memory_management_control_operation")
                        InstrType.MARK_LONG -> {
                            writeUEtrace(writer, 6, "SH: memory_management_control_operation")
                            writeUEtrace(writer, mmop.arg1, "SH: long_term_frame_idx")
                        }
                    }
                }
                writeUEtrace(writer, 0, "SH: memory_management_control_operation")
            }
        }
    }

    private fun writePredWeightTable(sliceHeader: SliceHeader, writer: BitWriter) {
        val sps = sliceHeader.sps
        writeUEtrace(writer, sliceHeader.predWeightTable!!.lumaLog2WeightDenom, "SH: luma_log2_weight_denom")
        if (sps!!.chromaFormatIdc != ColorSpace.MONO) {
            writeUEtrace(writer, sliceHeader.predWeightTable!!.chromaLog2WeightDenom, "SH: chroma_log2_weight_denom")
        }
        writeOffsetWeight(sliceHeader, writer, 0)
        if (sliceHeader.sliceType == SliceType.B) {
            writeOffsetWeight(sliceHeader, writer, 1)
        }
    }

    private fun writeOffsetWeight(sliceHeader: SliceHeader, writer: BitWriter, list: Int) {
        val sps = sliceHeader.sps
        val predWeightTable = sliceHeader.predWeightTable!!
        val defaultLW = 1 shl predWeightTable.lumaLog2WeightDenom
        val defaultCW = 1 shl predWeightTable.chromaLog2WeightDenom
        for (i in 0 until predWeightTable.lumaWeight[list]!!.size) {
            val flagLuma = (predWeightTable.lumaWeight[list]!![i] != defaultLW
                    || predWeightTable.lumaOffset[list]!![i] != 0)
            writeBool(writer, flagLuma, "SH: luma_weight_l0_flag")
            if (flagLuma) {
                writeSEtrace(writer, predWeightTable.lumaWeight[list]!![i], "SH: luma_weight_l$list")
                writeSEtrace(writer, predWeightTable.lumaOffset[list]!![i], "SH: luma_offset_l$list")
            }
            if (sps!!.chromaFormatIdc != ColorSpace.MONO) {
                val flagChroma = predWeightTable.chromaWeight[list]!![0][i] != defaultCW || predWeightTable.chromaOffset[list]!![0][i] != 0 || predWeightTable.chromaWeight[list]!![1][i] != defaultCW || predWeightTable.chromaOffset[list]!![1][i] != 0
                writeBool(writer, flagChroma, "SH: chroma_weight_l0_flag")
                if (flagChroma) for (j in 0..1) {
                    writeSEtrace(writer, predWeightTable.chromaWeight[list]!![j][i], "SH: chroma_weight_l$list")
                    writeSEtrace(writer, predWeightTable.chromaOffset[list]!![j][i], "SH: chroma_offset_l$list")
                }
            }
        }
    }

    private fun writeRefPicListReordering(sliceHeader: SliceHeader, writer: BitWriter) {
        val refPicReordering = sliceHeader.refPicReordering
        if (sliceHeader.sliceType!!.isInter) {
            val l0ReorderingPresent = (refPicReordering != null
                    && refPicReordering[0] != null)
            writeBool(writer, l0ReorderingPresent, "SH: ref_pic_list_reordering_flag_l0")
            if (l0ReorderingPresent) writeReorderingList(refPicReordering!![0], writer)
        }
        if (sliceHeader.sliceType == SliceType.B) {
            val l1ReorderingPresent = (refPicReordering != null
                    && refPicReordering[1] != null)
            writeBool(writer, l1ReorderingPresent, "SH: ref_pic_list_reordering_flag_l1")
            if (l1ReorderingPresent) writeReorderingList(refPicReordering!![1], writer)
        }
    }

    private fun writeReorderingList(reordering: Array<IntArray?>?, writer: BitWriter) {
        if (reordering == null) return
        for (i in 0 until reordering[0]!!.size) {
            writeUEtrace(writer, reordering[0]!![i], "SH: reordering_of_pic_nums_idc")
            writeUEtrace(writer, reordering[1]!![i], "SH: abs_diff_pic_num_minus1")
        }
        writeUEtrace(writer, 3, "SH: reordering_of_pic_nums_idc")
    }
}