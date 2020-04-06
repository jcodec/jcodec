package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.io.model.*
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType
import org.jcodec.codecs.h264.io.model.SeqParameterSet.Companion.getPicHeightInMbs
import org.jcodec.common.IntArrayList
import org.jcodec.common.io.BitReader
import org.jcodec.common.model.ColorSpace
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Reads header of the coded slice
 *
 * @author The JCodec project
 */
object SliceHeaderReader {
    @JvmStatic
    fun readPart1(_in: BitReader): SliceHeader {
        val sh = SliceHeader()
        sh.firstMbInSlice = CAVLCReader.readUEtrace(_in, "SH: first_mb_in_slice")
        val shType = CAVLCReader.readUEtrace(_in, "SH: slice_type")
        sh.sliceType = SliceType.fromValue(shType % 5)
        sh.sliceTypeRestr = shType / 5 > 0
        sh.picParameterSetId = CAVLCReader.readUEtrace(_in, "SH: pic_parameter_set_id")
        return sh
    }

    @JvmStatic
    fun readPart2(sh: SliceHeader, nalUnit: NALUnit, sps: SeqParameterSet, pps: PictureParameterSet,
                  _in: BitReader): SliceHeader {
        sh.pps = pps
        sh.sps = sps
        sh.frameNum = CAVLCReader.readU(_in, sps.log2MaxFrameNumMinus4 + 4, "SH: frame_num")
        if (!sps.isFrameMbsOnlyFlag) {
            sh.fieldPicFlag = CAVLCReader.readBool(_in, "SH: field_pic_flag")
            if (sh.fieldPicFlag) {
                sh.bottomFieldFlag = CAVLCReader.readBool(_in, "SH: bottom_field_flag")
            }
        }
        if (nalUnit.type == NALUnitType.IDR_SLICE) {
            sh.idrPicId = CAVLCReader.readUEtrace(_in, "SH: idr_pic_id")
        }
        if (sps.picOrderCntType == 0) {
            sh.picOrderCntLsb = CAVLCReader.readU(_in, sps.log2MaxPicOrderCntLsbMinus4 + 4, "SH: pic_order_cnt_lsb")
            if (pps.isPicOrderPresentFlag && !sps.isFieldPicFlag) {
                sh.deltaPicOrderCntBottom = CAVLCReader.readSE(_in, "SH: delta_pic_order_cnt_bottom")
            }
        }
        sh.deltaPicOrderCnt = IntArray(2)
        if (sps.picOrderCntType == 1 && !sps.isDeltaPicOrderAlwaysZeroFlag) {
            sh.deltaPicOrderCnt!![0] = CAVLCReader.readSE(_in, "SH: delta_pic_order_cnt[0]")
            if (pps.isPicOrderPresentFlag && !sps.isFieldPicFlag) sh.deltaPicOrderCnt!![1] = CAVLCReader.readSE(_in, "SH: delta_pic_order_cnt[1]")
        }
        if (pps.isRedundantPicCntPresentFlag) {
            sh.redundantPicCnt = CAVLCReader.readUEtrace(_in, "SH: redundant_pic_cnt")
        }
        if (sh.sliceType == SliceType.B) {
            sh.directSpatialMvPredFlag = CAVLCReader.readBool(_in, "SH: direct_spatial_mv_pred_flag")
        }
        if (sh.sliceType == SliceType.P || sh.sliceType == SliceType.SP || sh.sliceType == SliceType.B) {
            sh.numRefIdxActiveOverrideFlag = CAVLCReader.readBool(_in, "SH: num_ref_idx_active_override_flag")
            if (sh.numRefIdxActiveOverrideFlag) {
                sh.numRefIdxActiveMinus1[0] = CAVLCReader.readUEtrace(_in, "SH: num_ref_idx_l0_active_minus1")
                if (sh.sliceType == SliceType.B) {
                    sh.numRefIdxActiveMinus1[1] = CAVLCReader.readUEtrace(_in, "SH: num_ref_idx_l1_active_minus1")
                }
            }
        }
        readRefPicListReordering(sh, _in)
        if (pps.isWeightedPredFlag && (sh.sliceType == SliceType.P || sh.sliceType == SliceType.SP)
                || pps.weightedBipredIdc == 1 && sh.sliceType == SliceType.B) readPredWeightTable(sps, pps, sh, _in)
        if (nalUnit.nal_ref_idc != 0) readDecoderPicMarking(nalUnit, sh, _in)
        if (pps.isEntropyCodingModeFlag && sh.sliceType!!.isInter) {
            sh.cabacInitIdc = CAVLCReader.readUEtrace(_in, "SH: cabac_init_idc")
        }
        sh.sliceQpDelta = CAVLCReader.readSE(_in, "SH: slice_qp_delta")
        if (sh.sliceType == SliceType.SP || sh.sliceType == SliceType.SI) {
            if (sh.sliceType == SliceType.SP) {
                sh.spForSwitchFlag = CAVLCReader.readBool(_in, "SH: sp_for_switch_flag")
            }
            sh.sliceQsDelta = CAVLCReader.readSE(_in, "SH: slice_qs_delta")
        }
        if (pps.isDeblockingFilterControlPresentFlag) {
            sh.disableDeblockingFilterIdc = CAVLCReader.readUEtrace(_in, "SH: disable_deblocking_filter_idc")
            if (sh.disableDeblockingFilterIdc != 1) {
                sh.sliceAlphaC0OffsetDiv2 = CAVLCReader.readSE(_in, "SH: slice_alpha_c0_offset_div2")
                sh.sliceBetaOffsetDiv2 = CAVLCReader.readSE(_in, "SH: slice_beta_offset_div2")
            }
        }
        if (pps.numSliceGroupsMinus1 > 0 && pps.sliceGroupMapType >= 3 && pps.sliceGroupMapType <= 5) {
            var len = (getPicHeightInMbs(sps) * (sps.picWidthInMbsMinus1 + 1)
                    / (pps.sliceGroupChangeRateMinus1 + 1))
            if (getPicHeightInMbs(sps) * (sps.picWidthInMbsMinus1 + 1)
                    % (pps.sliceGroupChangeRateMinus1 + 1) > 0) len += 1
            len = CeilLog2(len + 1)
            sh.sliceGroupChangeCycle = CAVLCReader.readU(_in, len, "SH: slice_group_change_cycle")
        }
        return sh
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

    // static int i = 0;
    private fun readDecoderPicMarking(nalUnit: NALUnit, sh: SliceHeader, _in: BitReader) {
        if (nalUnit.type == NALUnitType.IDR_SLICE) {
            val noOutputOfPriorPicsFlag = CAVLCReader.readBool(_in, "SH: no_output_of_prior_pics_flag")
            val longTermReferenceFlag = CAVLCReader.readBool(_in, "SH: long_term_reference_flag")
            sh.refPicMarkingIDR = RefPicMarkingIDR(noOutputOfPriorPicsFlag, longTermReferenceFlag)
        } else {
            val adaptiveRefPicMarkingModeFlag = CAVLCReader.readBool(_in, "SH: adaptive_ref_pic_marking_mode_flag")
            if (adaptiveRefPicMarkingModeFlag) {
                val mmops = ArrayList<RefPicMarking.Instruction>()
                var memoryManagementControlOperation: Int
                do {
                    memoryManagementControlOperation = CAVLCReader.readUEtrace(_in, "SH: memory_management_control_operation")
                    var instr: RefPicMarking.Instruction? = null
                    when (memoryManagementControlOperation) {
                        1 -> instr = RefPicMarking.Instruction(InstrType.REMOVE_SHORT, CAVLCReader.readUEtrace(_in,
                                "SH: difference_of_pic_nums_minus1") + 1, 0)
                        2 -> instr = RefPicMarking.Instruction(InstrType.REMOVE_LONG,
                                CAVLCReader.readUEtrace(_in, "SH: long_term_pic_num"), 0)
                        3 -> instr = RefPicMarking.Instruction(InstrType.CONVERT_INTO_LONG, CAVLCReader.readUEtrace(_in,
                                "SH: difference_of_pic_nums_minus1") + 1, CAVLCReader.readUEtrace(_in, "SH: long_term_frame_idx"))
                        4 -> instr = RefPicMarking.Instruction(InstrType.TRUNK_LONG, CAVLCReader.readUEtrace(_in,
                                "SH: max_long_term_frame_idx_plus1") - 1, 0)
                        5 -> instr = RefPicMarking.Instruction(InstrType.CLEAR, 0, 0)
                        6 -> instr = RefPicMarking.Instruction(InstrType.MARK_LONG,
                                CAVLCReader.readUEtrace(_in, "SH: long_term_frame_idx"), 0)
                    }
                    if (instr != null) mmops.add(instr)
                } while (memoryManagementControlOperation != 0)
                sh.refPicMarkingNonIDR = RefPicMarking(mmops.toArray(arrayOf()))
            }
        }
    }

    private fun readPredWeightTable(sps: SeqParameterSet, pps: PictureParameterSet, sh: SliceHeader, _in: BitReader) {
        sh.predWeightTable = PredictionWeightTable()
        val numRefsMinus1 = if (sh.numRefIdxActiveOverrideFlag) sh.numRefIdxActiveMinus1 else pps.numRefIdxActiveMinus1
        val nr = intArrayOf(numRefsMinus1[0] + 1, numRefsMinus1[1] + 1)
        sh.predWeightTable!!.lumaLog2WeightDenom = CAVLCReader.readUEtrace(_in, "SH: luma_log2_weight_denom")
        if (sps.chromaFormatIdc != ColorSpace.MONO) {
            sh.predWeightTable!!.chromaLog2WeightDenom = CAVLCReader.readUEtrace(_in, "SH: chroma_log2_weight_denom")
        }
        val defaultLW = 1 shl sh.predWeightTable!!.lumaLog2WeightDenom
        val defaultCW = 1 shl sh.predWeightTable!!.chromaLog2WeightDenom
        for (list in 0..1) {
            sh.predWeightTable!!.lumaWeight[list] = IntArray(nr[list])
            sh.predWeightTable!!.lumaOffset[list] = IntArray(nr[list])
            sh.predWeightTable!!.chromaWeight[list] = Array(2) { IntArray(nr[list]) }
            sh.predWeightTable!!.chromaOffset[list] = Array(2) { IntArray(nr[list]) }
            for (i in 0 until nr[list]) {
                sh.predWeightTable!!.lumaWeight[list]!![i] = defaultLW
                sh.predWeightTable!!.lumaOffset[list]!![i] = 0
                sh.predWeightTable!!.chromaWeight[list]!![0][i] = defaultCW
                sh.predWeightTable!!.chromaOffset[list]!![0][i] = 0
                sh.predWeightTable!!.chromaWeight[list]!![1][i] = defaultCW
                sh.predWeightTable!!.chromaOffset[list]!![1][i] = 0
            }
        }
        readWeightOffset(sps, pps, sh, _in, nr, 0)
        if (sh.sliceType == SliceType.B) {
            readWeightOffset(sps, pps, sh, _in, nr, 1)
        }
    }

    private fun readWeightOffset(sps: SeqParameterSet, pps: PictureParameterSet, sh: SliceHeader, _in: BitReader,
                                 numRefs: IntArray, list: Int) {
        for (i in 0 until numRefs[list]) {
            val lumaWeightL0Flag = CAVLCReader.readBool(_in, "SH: luma_weight_l0_flag")
            if (lumaWeightL0Flag) {
                sh.predWeightTable!!.lumaWeight[list]!![i] = CAVLCReader.readSE(_in, "SH: weight")
                sh.predWeightTable!!.lumaOffset[list]!![i] = CAVLCReader.readSE(_in, "SH: offset")
            }
            if (sps.chromaFormatIdc != ColorSpace.MONO) {
                val chromaWeightL0Flag = CAVLCReader.readBool(_in, "SH: chroma_weight_l0_flag")
                if (chromaWeightL0Flag) {
                    sh.predWeightTable!!.chromaWeight[list]!![0][i] = CAVLCReader.readSE(_in, "SH: weight")
                    sh.predWeightTable!!.chromaOffset[list]!![0][i] = CAVLCReader.readSE(_in, "SH: offset")
                    sh.predWeightTable!!.chromaWeight[list]!![1][i] = CAVLCReader.readSE(_in, "SH: weight")
                    sh.predWeightTable!!.chromaOffset[list]!![1][i] = CAVLCReader.readSE(_in, "SH: offset")
                }
            }
        }
    }

    private fun readRefPicListReordering(sh: SliceHeader, _in: BitReader) {
        sh.refPicReordering = arrayOfNulls<Array<IntArray?>?>(2)
        // System.out.println(i++);
        if (sh.sliceType!!.isInter) {
            val refPicListReorderingFlagL0 = CAVLCReader.readBool(_in, "SH: ref_pic_list_reordering_flag_l0")
            if (refPicListReorderingFlagL0) {
                sh.refPicReordering!![0] = readReorderingEntries(_in)
            }
        }
        if (sh.sliceType == SliceType.B) {
            val refPicListReorderingFlagL1 = CAVLCReader.readBool(_in, "SH: ref_pic_list_reordering_flag_l1")
            if (refPicListReorderingFlagL1) {
                sh.refPicReordering!![1] = readReorderingEntries(_in)
            }
        }
    }

    private fun readReorderingEntries(_in: BitReader): Array<IntArray?> {
        val ops = IntArrayList.createIntArrayList()
        val args = IntArrayList.createIntArrayList()
        do {
            val idc = CAVLCReader.readUEtrace(_in, "SH: reordering_of_pic_nums_idc")
            if (idc == 3) break
            ops.add(idc)
            args.add(CAVLCReader.readUEtrace(_in, "SH: abs_diff_pic_num_minus1"))
        } while (true)
        return arrayOf(ops.toArray(), args.toArray())
    }
}