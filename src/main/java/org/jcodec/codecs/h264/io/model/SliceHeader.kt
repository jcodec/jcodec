package org.jcodec.codecs.h264.io.model

import org.jcodec.platform.Platform

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Slice header H264 bitstream entity
 *
 * capable to serialize / deserialize with CAVLC bitstream
 *
 * @author The JCodec project
 */
class SliceHeader {
    @JvmField
    var sps: SeqParameterSet? = null
    @JvmField
    var pps: PictureParameterSet? = null
    @JvmField
    var refPicMarkingNonIDR: RefPicMarking? = null
    @JvmField
    var refPicMarkingIDR: RefPicMarkingIDR? = null
    @JvmField
    var refPicReordering: Array<Array<IntArray?>?>? = null

    // pred_weight_table
    @JvmField
    var predWeightTable: PredictionWeightTable? = null

    // first_mb_in_slice
    @JvmField
    var firstMbInSlice = 0

    // field_pic_flag
    @JvmField
    var fieldPicFlag = false

    //  slice_type
    @JvmField
    var sliceType: SliceType? = null

    // slice_type_restr
    @JvmField
    var sliceTypeRestr = false

    // pic_parameter_set_id
    @JvmField
    var picParameterSetId = 0

    // frame_num
    @JvmField
    var frameNum = 0

    // bottom_field_flag
    @JvmField
    var bottomFieldFlag = false

    // idr_pic_id
    @JvmField
    var idrPicId = 0

    // pic_order_cnt_lsb
    @JvmField
    var picOrderCntLsb = 0

    // delta_pic_order_cnt_bottom
    @JvmField
    var deltaPicOrderCntBottom = 0

    // delta_pic_order_cnt
    @JvmField
    var deltaPicOrderCnt: IntArray? = null

    // redundant_pic_cnt
    @JvmField
    var redundantPicCnt = 0

    // direct_spatial_mv_pred_flag
    @JvmField
    var directSpatialMvPredFlag = false

    // num_ref_idx_active_override_flag
    @JvmField
    var numRefIdxActiveOverrideFlag = false

    // num_ref_idx_active_minus1
    @JvmField
    var numRefIdxActiveMinus1: IntArray

    // cabac_init_idc
    @JvmField
    var cabacInitIdc = 0

    // slice_qp_delta
    @JvmField
    var sliceQpDelta = 0

    // sp_for_switch_flag
    @JvmField
    var spForSwitchFlag = false

    // slice_qs_delta
    @JvmField
    var sliceQsDelta = 0

    // disable_deblocking_filter_idc
    @JvmField
    var disableDeblockingFilterIdc = 0

    // slice_alpha_c0_offset_div2
    @JvmField
    var sliceAlphaC0OffsetDiv2 = 0

    // slice_beta_offset_div2
    @JvmField
    var sliceBetaOffsetDiv2 = 0

    // slice_group_change_cycle
    @JvmField
    var sliceGroupChangeCycle = 0
    override fun toString(): String {
        return Platform.toJSON(this)
    }

    init {
        numRefIdxActiveMinus1 = IntArray(2)
    }
}