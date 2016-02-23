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

    public PredictionWeightTable pred_weight_table;
    public int first_mb_in_slice;

    public boolean field_pic_flag;

    public SliceType slice_type;
    public boolean slice_type_restr;

    public int pic_parameter_set_id;

    public int frame_num;

    public boolean bottom_field_flag;

    public int idr_pic_id;

    public int pic_order_cnt_lsb;

    public int delta_pic_order_cnt_bottom;

    public int[] delta_pic_order_cnt;

    public int redundant_pic_cnt;

    public boolean direct_spatial_mv_pred_flag;

    public boolean num_ref_idx_active_override_flag;

    public int[] num_ref_idx_active_minus1;

    public int cabac_init_idc;

    public int slice_qp_delta;

    public boolean sp_for_switch_flag;

    public int slice_qs_delta;

    public int disable_deblocking_filter_idc;

    public int slice_alpha_c0_offset_div2;

    public int slice_beta_offset_div2;

    public int slice_group_change_cycle;
    
    public SliceHeader() {
        this.num_ref_idx_active_minus1 = new int[2];
    }
    
    @Override
    public String toString() {
        return ToJSON.toJSON(this);
    }
}
