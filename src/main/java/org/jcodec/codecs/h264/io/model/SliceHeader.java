package org.jcodec.codecs.h264.io.model;
import org.jcodec.common.tools.ToJSON;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Slice header H264 bitstream entity
 * 
 * capable to serialize / deserialize with CAVLC bitstream
 * 
 * @author The JCodec project
 * 
 */
public class SliceHeader {

    public SeqParameterSet sps;
    public PictureParameterSet pps;

    public RefPicMarking refPicMarkingNonIDR;
    public RefPicMarkingIDR refPicMarkingIDR;

    public int[][][] refPicReordering;

    // pred_weight_table
    public PredictionWeightTable predWeightTable;
    // first_mb_in_slice
    public int firstMbInSlice;

    // field_pic_flag
    public boolean fieldPicFlag;

    //  slice_type
    public SliceType sliceType;
    
    // slice_type_restr
    public boolean sliceTypeRestr;

    // pic_parameter_set_id
    public int picParameterSetId;

    // frame_num
    public int frameNum;

    // bottom_field_flag
    public boolean bottomFieldFlag;

    // idr_pic_id
    public int idrPicId;

    // pic_order_cnt_lsb
    public int picOrderCntLsb;

    // delta_pic_order_cnt_bottom
    public int deltaPicOrderCntBottom;

    // delta_pic_order_cnt
    public int[] deltaPicOrderCnt;

    // redundant_pic_cnt
    public int redundantPicCnt;

    // direct_spatial_mv_pred_flag
    public boolean directSpatialMvPredFlag;

    // num_ref_idx_active_override_flag
    public boolean numRefIdxActiveOverrideFlag;

    // num_ref_idx_active_minus1
    public int[] numRefIdxActiveMinus1;

    // cabac_init_idc
    public int cabacInitIdc;

    // slice_qp_delta
    public int sliceQpDelta;

    // sp_for_switch_flag
    public boolean spForSwitchFlag;

    // slice_qs_delta
    public int sliceQsDelta;

    // disable_deblocking_filter_idc
    public int disableDeblockingFilterIdc;

    // slice_alpha_c0_offset_div2
    public int sliceAlphaC0OffsetDiv2;

    // slice_beta_offset_div2
    public int sliceBetaOffsetDiv2;

    // slice_group_change_cycle
    public int sliceGroupChangeCycle;
    
    public SliceHeader() {
        this.numRefIdxActiveMinus1 = new int[2];
    }
    
    @Override
    public String toString() {
        return ToJSON.toJSON(this);
    }
}
