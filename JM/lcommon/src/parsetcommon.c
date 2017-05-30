
/*!
 **************************************************************************************
 * \file
 *    parsetcommon.c
 * \brief
 *    Picture and Sequence Parameter set generation and handling
 *  \date 25 November 2002
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Stephan Wenger        <stewe@cs.tu-berlin.de>
 *
 **************************************************************************************
 */

#include "global.h"
#include "parsetcommon.h"
#include "memalloc.h"
/*!
 *************************************************************************************
 * \brief
 *    Allocates memory for a picture paramater set
 *
 * \return
 *    pointer to a pps
 *************************************************************************************
 */

pic_parameter_set_rbsp_t *AllocPPS ()
 {
   pic_parameter_set_rbsp_t *p;

   if ((p=calloc (1, sizeof (pic_parameter_set_rbsp_t))) == NULL)
     no_mem_exit ("AllocPPS: PPS");
   p->slice_group_id = NULL;
   return p;
 }


/*!
 *************************************************************************************
 * \brief
 *    Allocates memory for am sequence paramater set
 *
 * \return
 *    pointer to a sps
 *************************************************************************************
 */

seq_parameter_set_rbsp_t *AllocSPS ()
 {
   seq_parameter_set_rbsp_t *p;

   if ((p=calloc (1, sizeof (seq_parameter_set_rbsp_t))) == NULL)
     no_mem_exit ("AllocSPS: SPS");
   return p;
 }


/*!
 *************************************************************************************
 * \brief
 *    Frees a picture parameter set
 *
 * \param pps to be freed
 *   Picture parameter set to be freed
 *************************************************************************************
 */

 void FreePPS (pic_parameter_set_rbsp_t *pps)
 {
   assert (pps != NULL);
   if (pps->slice_group_id != NULL) 
     free (pps->slice_group_id);
   free (pps);
 }


 /*!
 *************************************************************************************
 * \brief
 *    Frees a sps
 *
 * \param sps
 *   Sequence parameter set to be freed
 *************************************************************************************
 */

 void FreeSPS (seq_parameter_set_rbsp_t *sps)
 {
   assert (sps != NULL);
   free (sps);
 }


int sps_is_equal(seq_parameter_set_rbsp_t *sps1, seq_parameter_set_rbsp_t *sps2)
{
  unsigned i;
  int equal = 1;

  if ((!sps1->Valid) || (!sps2->Valid))
    return 0;

  equal &= (sps1->profile_idc == sps2->profile_idc);
  equal &= (sps1->constrained_set0_flag == sps2->constrained_set0_flag);
  equal &= (sps1->constrained_set1_flag == sps2->constrained_set1_flag);
  equal &= (sps1->constrained_set2_flag == sps2->constrained_set2_flag);
  equal &= (sps1->level_idc == sps2->level_idc);
  equal &= (sps1->seq_parameter_set_id == sps2->seq_parameter_set_id);
  equal &= (sps1->log2_max_frame_num_minus4 == sps2->log2_max_frame_num_minus4);
  equal &= (sps1->pic_order_cnt_type == sps2->pic_order_cnt_type);

  if (!equal) return equal;

  if( sps1->pic_order_cnt_type == 0 )
  {
    equal &= (sps1->log2_max_pic_order_cnt_lsb_minus4 == sps2->log2_max_pic_order_cnt_lsb_minus4);
  }

  else if( sps1->pic_order_cnt_type == 1 )
  {
    equal &= (sps1->delta_pic_order_always_zero_flag == sps2->delta_pic_order_always_zero_flag);
    equal &= (sps1->offset_for_non_ref_pic == sps2->offset_for_non_ref_pic);
    equal &= (sps1->offset_for_top_to_bottom_field == sps2->offset_for_top_to_bottom_field);
    equal &= (sps1->num_ref_frames_in_pic_order_cnt_cycle == sps2->num_ref_frames_in_pic_order_cnt_cycle);
    if (!equal) return equal;

    for ( i = 0 ; i< sps1->num_ref_frames_in_pic_order_cnt_cycle ;i ++)
      equal &= (sps1->offset_for_ref_frame[i] == sps2->offset_for_ref_frame[i]);
  }

  equal &= (sps1->num_ref_frames == sps2->num_ref_frames);
  equal &= (sps1->gaps_in_frame_num_value_allowed_flag == sps2->gaps_in_frame_num_value_allowed_flag);
  equal &= (sps1->pic_width_in_mbs_minus1 == sps2->pic_width_in_mbs_minus1);
  equal &= (sps1->pic_height_in_map_units_minus1 == sps2->pic_height_in_map_units_minus1);
  equal &= (sps1->frame_mbs_only_flag == sps2->frame_mbs_only_flag);

  if (!equal) return equal;
  if( !sps1->frame_mbs_only_flag )
    equal &= (sps1->mb_adaptive_frame_field_flag == sps2->mb_adaptive_frame_field_flag);

  equal &= (sps1->direct_8x8_inference_flag == sps2->direct_8x8_inference_flag);
  equal &= (sps1->frame_cropping_flag == sps2->frame_cropping_flag);
  if (!equal) return equal;
  if (sps1->frame_cropping_flag)
  {
    equal &= (sps1->frame_crop_left_offset == sps2->frame_crop_left_offset);
    equal &= (sps1->frame_crop_right_offset == sps2->frame_crop_right_offset);
    equal &= (sps1->frame_crop_top_offset == sps2->frame_crop_top_offset);
    equal &= (sps1->frame_crop_bottom_offset == sps2->frame_crop_bottom_offset);
  }
  equal &= (sps1->vui_parameters_present_flag == sps2->vui_parameters_present_flag);

  return equal;
}

int pps_is_equal(pic_parameter_set_rbsp_t *pps1, pic_parameter_set_rbsp_t *pps2)
{
  unsigned i, j;
  int equal = 1;

  if ((!pps1->Valid) || (!pps2->Valid))
    return 0;

  equal &= (pps1->pic_parameter_set_id == pps2->pic_parameter_set_id);
  equal &= (pps1->seq_parameter_set_id == pps2->seq_parameter_set_id);
  equal &= (pps1->entropy_coding_mode_flag == pps2->entropy_coding_mode_flag);
  equal &= (pps1->bottom_field_pic_order_in_frame_present_flag == pps2->bottom_field_pic_order_in_frame_present_flag);
  equal &= (pps1->num_slice_groups_minus1 == pps2->num_slice_groups_minus1);

  if (!equal) return equal;

  if (pps1->num_slice_groups_minus1>0)
  {
      equal &= (pps1->slice_group_map_type == pps2->slice_group_map_type);
      if (!equal) return equal;
      if (pps1->slice_group_map_type == 0)
      {
        for (i=0; i<=pps1->num_slice_groups_minus1; i++)
          equal &= (pps1->run_length_minus1[i] == pps2->run_length_minus1[i]);
      }
      else if( pps1->slice_group_map_type == 2 )
      {
        for (i=0; i<pps1->num_slice_groups_minus1; i++)
        {
          equal &= (pps1->top_left[i] == pps2->top_left[i]);
          equal &= (pps1->bottom_right[i] == pps2->bottom_right[i]);
        }
      }
      else if( pps1->slice_group_map_type == 3 || pps1->slice_group_map_type==4 || pps1->slice_group_map_type==5 )
      {
        equal &= (pps1->slice_group_change_direction_flag == pps2->slice_group_change_direction_flag);
        equal &= (pps1->slice_group_change_rate_minus1 == pps2->slice_group_change_rate_minus1);
      }
      else if( pps1->slice_group_map_type == 6 )
      {
        equal &= (pps1->pic_size_in_map_units_minus1 == pps2->pic_size_in_map_units_minus1);
        if (!equal) return equal;
        for (i=0; i<=pps1->pic_size_in_map_units_minus1; i++)
          equal &= (pps1->slice_group_id[i] == pps2->slice_group_id[i]);
      }
  }

  equal &= (pps1->num_ref_idx_l0_default_active_minus1 == pps2->num_ref_idx_l0_default_active_minus1);
  equal &= (pps1->num_ref_idx_l1_default_active_minus1 == pps2->num_ref_idx_l1_default_active_minus1);
  equal &= (pps1->weighted_pred_flag == pps2->weighted_pred_flag);
  equal &= (pps1->weighted_bipred_idc == pps2->weighted_bipred_idc);
  equal &= (pps1->pic_init_qp_minus26 == pps2->pic_init_qp_minus26);
  equal &= (pps1->pic_init_qs_minus26 == pps2->pic_init_qs_minus26);
  equal &= (pps1->chroma_qp_index_offset == pps2->chroma_qp_index_offset);
  equal &= (pps1->deblocking_filter_control_present_flag == pps2->deblocking_filter_control_present_flag);
  equal &= (pps1->constrained_intra_pred_flag == pps2->constrained_intra_pred_flag);
  equal &= (pps1->redundant_pic_cnt_present_flag == pps2->redundant_pic_cnt_present_flag);

  if (!equal) return equal;

  //Fidelity Range Extensions Stuff
  //It is initialized to zero, so should be ok to check all the time.
  equal &= (pps1->transform_8x8_mode_flag == pps2->transform_8x8_mode_flag);
  equal &= (pps1->pic_scaling_matrix_present_flag == pps2->pic_scaling_matrix_present_flag);
  if(pps1->pic_scaling_matrix_present_flag)
  {
    for(i = 0; i < (6 + ((unsigned)pps1->transform_8x8_mode_flag << 1)); i++)
    {
      equal &= (pps1->pic_scaling_list_present_flag[i] == pps2->pic_scaling_list_present_flag[i]);
      if(pps1->pic_scaling_list_present_flag[i])
      {
        if(i < 6)
        {
          for (j = 0; j < 16; j++)
            equal &= (pps1->ScalingList4x4[i][j] == pps2->ScalingList4x4[i][j]);
        }
        else
        {
          for (j = 0; j < 64; j++)
            equal &= (pps1->ScalingList8x8[i-6][j] == pps2->ScalingList8x8[i-6][j]);
        }
      }
    }
  }
  equal &= (pps1->second_chroma_qp_index_offset == pps2->second_chroma_qp_index_offset);

  return equal;
}
