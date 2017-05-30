
/*!
 *************************************************************************************
 * \file header.c
 *
 * \brief
 *    H.264 Slice headers
 *
 *************************************************************************************
 */

#include "global.h"
#include "elements.h"
#include "defines.h"
#include "fmo.h"
#include "vlc.h"
#include "mbuffer.h"
#include "header.h"

#include "ctx_tables.h"


#if TRACE
#define SYMTRACESTRING(s) strncpy(sym.tracestring,s,TRACESTRING_SIZE)
#else
#define SYMTRACESTRING(s) // do nothing
#endif

static void ref_pic_list_reordering(Slice *currSlice);
static void pred_weight_table(Slice *currSlice);
#if (MVC_EXTENSION_ENABLE)
static void ref_pic_list_mvc_modification(Slice *currSlice);
#endif


/*!
 ************************************************************************
 * \brief
 *    calculate Ceil(Log2(uiVal))
 ************************************************************************
 */
unsigned CeilLog2( unsigned uiVal)
{
  unsigned uiTmp = uiVal-1;
  unsigned uiRet = 0;

  while( uiTmp != 0 )
  {
    uiTmp >>= 1;
    uiRet++;
  }
  return uiRet;
}

unsigned CeilLog2_sf( unsigned uiVal)
{
  unsigned uiTmp = uiVal-1;
  unsigned uiRet = 0;

  while( uiTmp > 0 )
  {
    uiTmp >>= 1;
    uiRet++;
  }
  return uiRet;
}

/*!
 ************************************************************************
 * \brief
 *    read the first part of the header (only the pic_parameter_set_id)
 * \return
 *    Length of the first part of the slice header (in bits)
 ************************************************************************
 */
int FirstPartOfSliceHeader(Slice *currSlice)
{
  VideoParameters *p_Vid = currSlice->p_Vid;
  byte dP_nr = assignSE2partition[currSlice->dp_mode][SE_HEADER];
  DataPartition *partition = &(currSlice->partArr[dP_nr]);
  Bitstream *currStream = partition->bitstream;
  int tmp;

  p_Dec->UsedBits= partition->bitstream->frame_bitoffset; // was hardcoded to 31 for previous start-code. This is better.

  // Get first_mb_in_slice
  currSlice->start_mb_nr = read_ue_v ("SH: first_mb_in_slice", currStream, &p_Dec->UsedBits);

  tmp = read_ue_v ("SH: slice_type", currStream, &p_Dec->UsedBits);

  if (tmp > 4) tmp -= 5;

  p_Vid->type = currSlice->slice_type = (SliceType) tmp;

  currSlice->pic_parameter_set_id = read_ue_v ("SH: pic_parameter_set_id", currStream, &p_Dec->UsedBits);

  if( p_Vid->separate_colour_plane_flag )
    currSlice->colour_plane_id = read_u_v (2, "SH: colour_plane_id", currStream, &p_Dec->UsedBits);
  else
    currSlice->colour_plane_id = PLANE_Y;

  return p_Dec->UsedBits;
}

/*!
 ************************************************************************
 * \brief
 *    read the scond part of the header (without the pic_parameter_set_id
 * \return
 *    Length of the second part of the Slice header in bits
 ************************************************************************
 */
int RestOfSliceHeader(Slice *currSlice)
{
  VideoParameters *p_Vid = currSlice->p_Vid;
  InputParameters *p_Inp = currSlice->p_Inp;
  seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;

  byte dP_nr = assignSE2partition[currSlice->dp_mode][SE_HEADER];
  DataPartition *partition = &(currSlice->partArr[dP_nr]);
  Bitstream *currStream = partition->bitstream;

  int val, len;

  currSlice->frame_num = read_u_v (active_sps->log2_max_frame_num_minus4 + 4, "SH: frame_num", currStream, &p_Dec->UsedBits);

  /* Tian Dong: frame_num gap processing, if found */
  if(currSlice->idr_flag) //if (p_Vid->idr_flag)
  {
    p_Vid->pre_frame_num = currSlice->frame_num;
    // picture error concealment
    p_Vid->last_ref_pic_poc = 0;
    assert(currSlice->frame_num == 0);
  }

  if (active_sps->frame_mbs_only_flag)
  {
    p_Vid->structure = FRAME;
    currSlice->field_pic_flag=0;
  }
  else
  {
    // field_pic_flag   u(1)
    currSlice->field_pic_flag = read_u_1("SH: field_pic_flag", currStream, &p_Dec->UsedBits);
    if (currSlice->field_pic_flag)
    {
      // bottom_field_flag  u(1)
      currSlice->bottom_field_flag = (byte) read_u_1("SH: bottom_field_flag", currStream, &p_Dec->UsedBits);
      p_Vid->structure = currSlice->bottom_field_flag ? BOTTOM_FIELD : TOP_FIELD;
    }
    else
    {
      p_Vid->structure = FRAME;
      currSlice->bottom_field_flag = FALSE;
    }
  }

  currSlice->structure = (PictureStructure) p_Vid->structure;

  currSlice->mb_aff_frame_flag = (active_sps->mb_adaptive_frame_field_flag && (currSlice->field_pic_flag==0));
  //currSlice->mb_aff_frame_flag = p_Vid->mb_aff_frame_flag;

  if (currSlice->structure == FRAME       ) 
    assert (currSlice->field_pic_flag == 0);
  if (currSlice->structure == TOP_FIELD   ) 
    assert (currSlice->field_pic_flag == 1 && (currSlice->bottom_field_flag == FALSE));
  if (currSlice->structure == BOTTOM_FIELD) 
    assert (currSlice->field_pic_flag == 1 && (currSlice->bottom_field_flag == TRUE ));

  if (currSlice->idr_flag)
  {
    currSlice->idr_pic_id = read_ue_v("SH: idr_pic_id", currStream, &p_Dec->UsedBits);
  }
#if (MVC_EXTENSION_ENABLE)
  else if ( currSlice->svc_extension_flag == 0 && currSlice->NaluHeaderMVCExt.non_idr_flag == 0 )
  {
    currSlice->idr_pic_id = read_ue_v("SH: idr_pic_id", currStream, &p_Dec->UsedBits);
  }
#endif

  if (active_sps->pic_order_cnt_type == 0)
  {
    currSlice->pic_order_cnt_lsb = read_u_v(active_sps->log2_max_pic_order_cnt_lsb_minus4 + 4, "SH: pic_order_cnt_lsb", currStream, &p_Dec->UsedBits);
    if( p_Vid->active_pps->bottom_field_pic_order_in_frame_present_flag  ==  1 &&  !currSlice->field_pic_flag )
      currSlice->delta_pic_order_cnt_bottom = read_se_v("SH: delta_pic_order_cnt_bottom", currStream, &p_Dec->UsedBits);
    else
      currSlice->delta_pic_order_cnt_bottom = 0;
  }
  if( active_sps->pic_order_cnt_type == 1 )
  {
    if ( !active_sps->delta_pic_order_always_zero_flag )
    {
      currSlice->delta_pic_order_cnt[ 0 ] = read_se_v("SH: delta_pic_order_cnt[0]", currStream, &p_Dec->UsedBits);
      if( p_Vid->active_pps->bottom_field_pic_order_in_frame_present_flag  ==  1  &&  !currSlice->field_pic_flag )
        currSlice->delta_pic_order_cnt[ 1 ] = read_se_v("SH: delta_pic_order_cnt[1]", currStream, &p_Dec->UsedBits);
      else
        currSlice->delta_pic_order_cnt[ 1 ] = 0;  // set to zero if not in stream
    }
    else
    {
      currSlice->delta_pic_order_cnt[ 0 ] = 0;
      currSlice->delta_pic_order_cnt[ 1 ] = 0;
    }
  }

  //! redundant_pic_cnt is missing here
  if (p_Vid->active_pps->redundant_pic_cnt_present_flag)
  {
    currSlice->redundant_pic_cnt = read_ue_v ("SH: redundant_pic_cnt", currStream, &p_Dec->UsedBits);
  }

  if(currSlice->slice_type == B_SLICE)
  {
    currSlice->direct_spatial_mv_pred_flag = read_u_1 ("SH: direct_spatial_mv_pred_flag", currStream, &p_Dec->UsedBits);
  }

  currSlice->num_ref_idx_active[LIST_0] = p_Vid->active_pps->num_ref_idx_l0_default_active_minus1 + 1;
  currSlice->num_ref_idx_active[LIST_1] = p_Vid->active_pps->num_ref_idx_l1_default_active_minus1 + 1;

  if(currSlice->slice_type == P_SLICE || currSlice->slice_type == SP_SLICE || currSlice->slice_type == B_SLICE)
  {
    val = read_u_1 ("SH: num_ref_idx_override_flag", currStream, &p_Dec->UsedBits);
    if (val)
    {
      currSlice->num_ref_idx_active[LIST_0] = 1 + read_ue_v ("SH: num_ref_idx_l0_active_minus1", currStream, &p_Dec->UsedBits);

      if(currSlice->slice_type == B_SLICE)
      {
        currSlice->num_ref_idx_active[LIST_1] = 1 + read_ue_v ("SH: num_ref_idx_l1_active_minus1", currStream, &p_Dec->UsedBits);
      }
    }
  }
  if (currSlice->slice_type!=B_SLICE)
  {
    currSlice->num_ref_idx_active[LIST_1] = 0;
  }

#if (MVC_EXTENSION_ENABLE)
  if (currSlice->svc_extension_flag == 0 || currSlice->svc_extension_flag == 1)
    ref_pic_list_mvc_modification(currSlice);
  else
    ref_pic_list_reordering(currSlice);
#else
  ref_pic_list_reordering(currSlice);
#endif

  currSlice->weighted_pred_flag = (unsigned short) ((currSlice->slice_type == P_SLICE || currSlice->slice_type == SP_SLICE) 
    ? p_Vid->active_pps->weighted_pred_flag 
    : (currSlice->slice_type == B_SLICE && p_Vid->active_pps->weighted_bipred_idc == 1));
  currSlice->weighted_bipred_idc = (unsigned short) (currSlice->slice_type == B_SLICE && p_Vid->active_pps->weighted_bipred_idc > 0);

  if ((p_Vid->active_pps->weighted_pred_flag&&(currSlice->slice_type == P_SLICE|| currSlice->slice_type == SP_SLICE))||
      (p_Vid->active_pps->weighted_bipred_idc==1 && (currSlice->slice_type == B_SLICE)))
  {
    pred_weight_table(currSlice);
  }

  if (currSlice->nal_reference_idc)
    dec_ref_pic_marking(p_Vid, currStream, currSlice);

  if (p_Vid->active_pps->entropy_coding_mode_flag && currSlice->slice_type != I_SLICE && currSlice->slice_type != SI_SLICE)
  {
    currSlice->model_number = read_ue_v("SH: cabac_init_idc", currStream, &p_Dec->UsedBits);
  }
  else
  {
    currSlice->model_number = 0;
  }

  currSlice->slice_qp_delta = val = read_se_v("SH: slice_qp_delta", currStream, &p_Dec->UsedBits);
  //currSlice->qp = p_Vid->qp = 26 + p_Vid->active_pps->pic_init_qp_minus26 + val;
  currSlice->qp = 26 + p_Vid->active_pps->pic_init_qp_minus26 + val;

  if ((currSlice->qp < -p_Vid->bitdepth_luma_qp_scale) || (currSlice->qp > 51))
    error ("slice_qp_delta makes slice_qp_y out of range", 500);

  if(currSlice->slice_type == SP_SLICE || currSlice->slice_type == SI_SLICE)
  {
    if(currSlice->slice_type==SP_SLICE)
    {
      currSlice->sp_switch = read_u_1 ("SH: sp_for_switch_flag", currStream, &p_Dec->UsedBits);
    }
    currSlice->slice_qs_delta = val = read_se_v("SH: slice_qs_delta", currStream, &p_Dec->UsedBits);
    currSlice->qs = 26 + p_Vid->active_pps->pic_init_qs_minus26 + val;    
    if ((currSlice->qs < 0) || (currSlice->qs > 51))
      error ("slice_qs_delta makes slice_qs_y out of range", 500);
  }

#if DPF_PARAM_DISP
  printf("deblocking_filter_control_present_flag:%d\n", p_Vid->active_pps->deblocking_filter_control_present_flag);
#endif
  if (p_Vid->active_pps->deblocking_filter_control_present_flag)
  {
    currSlice->DFDisableIdc = (short) read_ue_v ("SH: disable_deblocking_filter_idc", currStream, &p_Dec->UsedBits);

    if (currSlice->DFDisableIdc!=1)
    {
      currSlice->DFAlphaC0Offset = (short) (2 * read_se_v("SH: slice_alpha_c0_offset_div2", currStream, &p_Dec->UsedBits));
      currSlice->DFBetaOffset    = (short) (2 * read_se_v("SH: slice_beta_offset_div2", currStream, &p_Dec->UsedBits));
    }
    else
    {
      currSlice->DFAlphaC0Offset = currSlice->DFBetaOffset = 0;
    }
  }
  else
  {
    currSlice->DFDisableIdc = currSlice->DFAlphaC0Offset = currSlice->DFBetaOffset = 0;
  }
#if DPF_PARAM_DISP
  printf("Slice:%d, DFParameters:(%d,%d,%d)\n\n", currSlice->current_slice_nr, currSlice->DFDisableIdc, currSlice->DFAlphaC0Offset, currSlice->DFBetaOffset);
#endif

  // The conformance point for intra profiles is without deblocking, but decoders are still recommended to filter the output.
  // We allow in the decoder config to skip the loop filtering. This is achieved by modifying the parameters here.
  if ( is_HI_intra_only_profile(active_sps->profile_idc, active_sps->constrained_set3_flag) && (p_Inp->intra_profile_deblocking == 0) )
  {
    currSlice->DFDisableIdc =1;
    currSlice->DFAlphaC0Offset = currSlice->DFBetaOffset = 0;
  }


  if (p_Vid->active_pps->num_slice_groups_minus1>0 && p_Vid->active_pps->slice_group_map_type>=3 &&
      p_Vid->active_pps->slice_group_map_type<=5)
  {
    len = (active_sps->pic_height_in_map_units_minus1+1)*(active_sps->pic_width_in_mbs_minus1+1)/
          (p_Vid->active_pps->slice_group_change_rate_minus1+1);
    if (((active_sps->pic_height_in_map_units_minus1+1)*(active_sps->pic_width_in_mbs_minus1+1))%
          (p_Vid->active_pps->slice_group_change_rate_minus1+1))
          len +=1;

    len = CeilLog2(len+1);

    currSlice->slice_group_change_cycle = read_u_v (len, "SH: slice_group_change_cycle", currStream, &p_Dec->UsedBits);
  }
  p_Vid->PicHeightInMbs = p_Vid->FrameHeightInMbs / ( 1 + currSlice->field_pic_flag );
  p_Vid->PicSizeInMbs   = p_Vid->PicWidthInMbs * p_Vid->PicHeightInMbs;
  p_Vid->FrameSizeInMbs = p_Vid->PicWidthInMbs * p_Vid->FrameHeightInMbs;

  return p_Dec->UsedBits;
}


/*!
 ************************************************************************
 * \brief
 *    read the reference picture reordering information
 ************************************************************************
 */
static void ref_pic_list_reordering(Slice *currSlice)
{
  //VideoParameters *p_Vid = currSlice->p_Vid;
  byte dP_nr = assignSE2partition[currSlice->dp_mode][SE_HEADER];
  DataPartition *partition = &(currSlice->partArr[dP_nr]);
  Bitstream *currStream = partition->bitstream;
  int i, val;

  alloc_ref_pic_list_reordering_buffer(currSlice);

  if (currSlice->slice_type != I_SLICE && currSlice->slice_type != SI_SLICE)
  {
    val = currSlice->ref_pic_list_reordering_flag[LIST_0] = read_u_1 ("SH: ref_pic_list_reordering_flag_l0", currStream, &p_Dec->UsedBits);

    if (val)
    {
      i=0;
      do
      {
        val = currSlice->modification_of_pic_nums_idc[LIST_0][i] = read_ue_v("SH: modification_of_pic_nums_idc_l0", currStream, &p_Dec->UsedBits);
        if (val==0 || val==1)
        {
          currSlice->abs_diff_pic_num_minus1[LIST_0][i] = read_ue_v("SH: abs_diff_pic_num_minus1_l0", currStream, &p_Dec->UsedBits);
        }
        else
        {
          if (val==2)
          {
            currSlice->long_term_pic_idx[LIST_0][i] = read_ue_v("SH: long_term_pic_idx_l0", currStream, &p_Dec->UsedBits);
          }
        }
        i++;
        // assert (i>currSlice->num_ref_idx_active[LIST_0]);
      } while (val != 3);
    }
  }

  if (currSlice->slice_type == B_SLICE)
  {
    val = currSlice->ref_pic_list_reordering_flag[LIST_1] = read_u_1 ("SH: ref_pic_list_reordering_flag_l1", currStream, &p_Dec->UsedBits);

    if (val)
    {
      i=0;
      do
      {
        val = currSlice->modification_of_pic_nums_idc[LIST_1][i] = read_ue_v("SH: modification_of_pic_nums_idc_l1", currStream, &p_Dec->UsedBits);
        if (val==0 || val==1)
        {
          currSlice->abs_diff_pic_num_minus1[LIST_1][i] = read_ue_v("SH: abs_diff_pic_num_minus1_l1", currStream, &p_Dec->UsedBits);
        }
        else
        {
          if (val==2)
          {
            currSlice->long_term_pic_idx[LIST_1][i] = read_ue_v("SH: long_term_pic_idx_l1", currStream, &p_Dec->UsedBits);
          }
        }
        i++;
        // assert (i>currSlice->num_ref_idx_active[LIST_1]);
      } while (val != 3);
    }
  }

  // set reference index of redundant slices.
  if(currSlice->redundant_pic_cnt && (currSlice->slice_type != I_SLICE) )
  {
    currSlice->redundant_slice_ref_idx = currSlice->abs_diff_pic_num_minus1[LIST_0][0] + 1;
  }
}

/*!
 ************************************************************************
 * \brief
 *    read the MVC reference picture reordering information
 ************************************************************************
 */
#if (MVC_EXTENSION_ENABLE)
static void ref_pic_list_mvc_modification(Slice *currSlice)
{
  //VideoParameters *p_Vid = currSlice->p_Vid;
  byte dP_nr = assignSE2partition[currSlice->dp_mode][SE_HEADER];
  DataPartition *partition = &(currSlice->partArr[dP_nr]);
  Bitstream *currStream = partition->bitstream;
  int i, val;

  alloc_ref_pic_list_reordering_buffer(currSlice);

  if ((currSlice->slice_type % 5) != I_SLICE && (currSlice->slice_type % 5) != SI_SLICE)
  {
    val = currSlice->ref_pic_list_reordering_flag[LIST_0] = read_u_1 ("SH: ref_pic_list_modification_flag_l0", currStream, &p_Dec->UsedBits);

    if (val)
    {
      i=0;
      do
      {
        val = currSlice->modification_of_pic_nums_idc[LIST_0][i] = read_ue_v("SH: modification_of_pic_nums_idc_l0", currStream, &p_Dec->UsedBits);
        if (val==0 || val==1)
        {
          currSlice->abs_diff_pic_num_minus1[LIST_0][i] = read_ue_v("SH: abs_diff_pic_num_minus1_l0", currStream, &p_Dec->UsedBits);
        }
        else
        {
          if (val==2)
          {
            currSlice->long_term_pic_idx[LIST_0][i] = read_ue_v("SH: long_term_pic_idx_l0", currStream, &p_Dec->UsedBits);
          }
          else if (val==4 || val==5)
          {
            currSlice->abs_diff_view_idx_minus1[LIST_0][i] = read_ue_v("SH: abs_diff_view_idx_minus1_l0", currStream, &p_Dec->UsedBits);
          }
        }
        i++;
        // assert (i>img->num_ref_idx_l0_active);
      } while (val != 3);
    }
  }

  if ((currSlice->slice_type % 5) == B_SLICE)
  {
    val = currSlice->ref_pic_list_reordering_flag[LIST_1] = read_u_1 ("SH: ref_pic_list_reordering_flag_l1", currStream, &p_Dec->UsedBits);

    if (val)
    {
      i=0;
      do
      {
        val = currSlice->modification_of_pic_nums_idc[LIST_1][i] = read_ue_v("SH: modification_of_pic_nums_idc_l1", currStream, &p_Dec->UsedBits);
        if (val==0 || val==1)
        {
          currSlice->abs_diff_pic_num_minus1[LIST_1][i] = read_ue_v("SH: abs_diff_pic_num_minus1_l1", currStream, &p_Dec->UsedBits);
        }
        else
        {
          if (val==2)
          {
            currSlice->long_term_pic_idx[LIST_1][i] = read_ue_v("SH: long_term_pic_idx_l1", currStream, &p_Dec->UsedBits);
          }
          else if (val==4 || val==5)
          {
            currSlice->abs_diff_view_idx_minus1[LIST_1][i] = read_ue_v("SH: abs_diff_view_idx_minus1_l1", currStream, &p_Dec->UsedBits);
          }
        }
        i++;
        // assert (i>img->num_ref_idx_l1_active);
      } while (val != 3);
    }
  }

  // set reference index of redundant slices.
  if(currSlice->redundant_pic_cnt && (currSlice->slice_type != I_SLICE) )
  {
    currSlice->redundant_slice_ref_idx = currSlice->abs_diff_pic_num_minus1[LIST_0][0] + 1;
  }
}
#endif

static void reset_wp_params(Slice *currSlice)
{
  int i,comp;
  int log_weight_denom;

  for (i=0; i<MAX_REFERENCE_PICTURES; i++)
  {
    for (comp=0; comp<3; comp++)
    {
      log_weight_denom = (comp == 0) ? currSlice->luma_log2_weight_denom : currSlice->chroma_log2_weight_denom;
      currSlice->wp_weight[0][i][comp] = 1 << log_weight_denom;
      currSlice->wp_weight[1][i][comp] = 1 << log_weight_denom;
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    read the weighted prediction tables
 ************************************************************************
 */
static void pred_weight_table(Slice *currSlice)
{
  VideoParameters *p_Vid = currSlice->p_Vid;
  seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;
  byte dP_nr = assignSE2partition[currSlice->dp_mode][SE_HEADER];
  DataPartition *partition = &(currSlice->partArr[dP_nr]);
  Bitstream *currStream = partition->bitstream;
  int luma_weight_flag_l0, luma_weight_flag_l1, chroma_weight_flag_l0, chroma_weight_flag_l1;
  int i,j;

  currSlice->luma_log2_weight_denom = (unsigned short) read_ue_v ("SH: luma_log2_weight_denom", currStream, &p_Dec->UsedBits);
  currSlice->wp_round_luma = currSlice->luma_log2_weight_denom ? 1<<(currSlice->luma_log2_weight_denom - 1): 0;

  if ( 0 != active_sps->chroma_format_idc)
  {
    currSlice->chroma_log2_weight_denom = (unsigned short) read_ue_v ("SH: chroma_log2_weight_denom", currStream, &p_Dec->UsedBits);
    currSlice->wp_round_chroma = currSlice->chroma_log2_weight_denom ? 1<<(currSlice->chroma_log2_weight_denom - 1): 0;
  }

  reset_wp_params(currSlice);

  for (i=0; i<currSlice->num_ref_idx_active[LIST_0]; i++)
  {
    luma_weight_flag_l0 = read_u_1("SH: luma_weight_flag_l0", currStream, &p_Dec->UsedBits);

    if (luma_weight_flag_l0)
    {
      currSlice->wp_weight[LIST_0][i][0] = read_se_v ("SH: luma_weight_l0", currStream, &p_Dec->UsedBits);
      currSlice->wp_offset[LIST_0][i][0] = read_se_v ("SH: luma_offset_l0", currStream, &p_Dec->UsedBits);
      currSlice->wp_offset[LIST_0][i][0] = currSlice->wp_offset[LIST_0][i][0]<<(p_Vid->bitdepth_luma - 8);
    }
    else
    {
      currSlice->wp_weight[LIST_0][i][0] = 1 << currSlice->luma_log2_weight_denom;
      currSlice->wp_offset[LIST_0][i][0] = 0;
    }

    if (active_sps->chroma_format_idc != 0)
    {
      chroma_weight_flag_l0 = read_u_1 ("SH: chroma_weight_flag_l0", currStream, &p_Dec->UsedBits);

      for (j=1; j<3; j++)
      {
        if (chroma_weight_flag_l0)
        {
          currSlice->wp_weight[LIST_0][i][j] = read_se_v("SH: chroma_weight_l0", currStream, &p_Dec->UsedBits);
          currSlice->wp_offset[LIST_0][i][j] = read_se_v("SH: chroma_offset_l0", currStream, &p_Dec->UsedBits);
          currSlice->wp_offset[LIST_0][i][j] = currSlice->wp_offset[LIST_0][i][j]<<(p_Vid->bitdepth_chroma-8);
        }
        else
        {
          currSlice->wp_weight[LIST_0][i][j] = 1<<currSlice->chroma_log2_weight_denom;
          currSlice->wp_offset[LIST_0][i][j] = 0;
        }
      }
    }
  }
  if ((currSlice->slice_type == B_SLICE) && p_Vid->active_pps->weighted_bipred_idc == 1)
  {
    for (i=0; i<currSlice->num_ref_idx_active[LIST_1]; i++)
    {
      luma_weight_flag_l1 = read_u_1("SH: luma_weight_flag_l1", currStream, &p_Dec->UsedBits);

      if (luma_weight_flag_l1)
      {
        currSlice->wp_weight[LIST_1][i][0] = read_se_v ("SH: luma_weight_l1", currStream, &p_Dec->UsedBits);
        currSlice->wp_offset[LIST_1][i][0] = read_se_v ("SH: luma_offset_l1", currStream, &p_Dec->UsedBits);
        currSlice->wp_offset[LIST_1][i][0] = currSlice->wp_offset[LIST_1][i][0]<<(p_Vid->bitdepth_luma-8);
      }
      else
      {
        currSlice->wp_weight[LIST_1][i][0] = 1<<currSlice->luma_log2_weight_denom;
        currSlice->wp_offset[LIST_1][i][0] = 0;
      }

      if (active_sps->chroma_format_idc != 0)
      {
        chroma_weight_flag_l1 = read_u_1 ("SH: chroma_weight_flag_l1", currStream, &p_Dec->UsedBits);

        for (j=1; j<3; j++)
        {
          if (chroma_weight_flag_l1)
          {
            currSlice->wp_weight[LIST_1][i][j] = read_se_v("SH: chroma_weight_l1", currStream, &p_Dec->UsedBits);
            currSlice->wp_offset[LIST_1][i][j] = read_se_v("SH: chroma_offset_l1", currStream, &p_Dec->UsedBits);
            currSlice->wp_offset[LIST_1][i][j] = currSlice->wp_offset[LIST_1][i][j]<<(p_Vid->bitdepth_chroma-8);
          }
          else
          {
            currSlice->wp_weight[LIST_1][i][j] = 1<<currSlice->chroma_log2_weight_denom;
            currSlice->wp_offset[LIST_1][i][j] = 0;
          }
        }
      }
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    read the memory control operations
 ************************************************************************
 */
void dec_ref_pic_marking(VideoParameters *p_Vid, Bitstream *currStream, Slice *pSlice)
{
  int val;

  DecRefPicMarking_t *tmp_drpm,*tmp_drpm2;

  // free old buffer content
  while (pSlice->dec_ref_pic_marking_buffer)
  {
    tmp_drpm=pSlice->dec_ref_pic_marking_buffer;

    pSlice->dec_ref_pic_marking_buffer=tmp_drpm->Next;
    free (tmp_drpm);
  }

#if (MVC_EXTENSION_ENABLE)
  if ( pSlice->idr_flag || (pSlice->svc_extension_flag == 0 && pSlice->NaluHeaderMVCExt.non_idr_flag == 0) )
#else
  if (pSlice->idr_flag)
#endif
  {
    pSlice->no_output_of_prior_pics_flag = read_u_1("SH: no_output_of_prior_pics_flag", currStream, &p_Dec->UsedBits);
    p_Vid->no_output_of_prior_pics_flag = pSlice->no_output_of_prior_pics_flag;
    pSlice->long_term_reference_flag = read_u_1("SH: long_term_reference_flag", currStream, &p_Dec->UsedBits);
  }
  else
  {
    pSlice->adaptive_ref_pic_buffering_flag = read_u_1("SH: adaptive_ref_pic_buffering_flag", currStream, &p_Dec->UsedBits);
    if (pSlice->adaptive_ref_pic_buffering_flag)
    {
      // read Memory Management Control Operation
      do
      {
        tmp_drpm=(DecRefPicMarking_t*)calloc (1,sizeof (DecRefPicMarking_t));
        tmp_drpm->Next=NULL;

        val = tmp_drpm->memory_management_control_operation = read_ue_v("SH: memory_management_control_operation", currStream, &p_Dec->UsedBits);

        if ((val==1)||(val==3))
        {
          tmp_drpm->difference_of_pic_nums_minus1 = read_ue_v("SH: difference_of_pic_nums_minus1", currStream, &p_Dec->UsedBits);
        }
        if (val==2)
        {
          tmp_drpm->long_term_pic_num = read_ue_v("SH: long_term_pic_num", currStream, &p_Dec->UsedBits);
        }

        if ((val==3)||(val==6))
        {
          tmp_drpm->long_term_frame_idx = read_ue_v("SH: long_term_frame_idx", currStream, &p_Dec->UsedBits);
        }
        if (val==4)
        {
          tmp_drpm->max_long_term_frame_idx_plus1 = read_ue_v("SH: max_long_term_pic_idx_plus1", currStream, &p_Dec->UsedBits);
        }

        // add command
        if (pSlice->dec_ref_pic_marking_buffer==NULL)
        {
          pSlice->dec_ref_pic_marking_buffer=tmp_drpm;
        }
        else
        {
          tmp_drpm2=pSlice->dec_ref_pic_marking_buffer;
          while (tmp_drpm2->Next!=NULL) tmp_drpm2=tmp_drpm2->Next;
          tmp_drpm2->Next=tmp_drpm;
        }

      }
      while (val != 0);
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    To calculate the poc values
 *        based upon JVT-F100d2
 *  POC200301: Until Jan 2003, this function will calculate the correct POC
 *    values, but the management of POCs in buffered pictures may need more work.
 * \return
 *    none
 ************************************************************************
 */
void decode_poc(VideoParameters *p_Vid, Slice *pSlice)
{
  seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;
  int i;
  // for POC mode 0:
  unsigned int MaxPicOrderCntLsb = (1<<(active_sps->log2_max_pic_order_cnt_lsb_minus4+4));

  switch ( active_sps->pic_order_cnt_type )
  {
  case 0: // POC MODE 0
    // 1st
    if(pSlice->idr_flag)
    {
      p_Vid->PrevPicOrderCntMsb = 0;
      p_Vid->PrevPicOrderCntLsb = 0;
    }
    else
    {
      if (p_Vid->last_has_mmco_5)
      {
        if (p_Vid->last_pic_bottom_field)
        {
          p_Vid->PrevPicOrderCntMsb = 0;
          p_Vid->PrevPicOrderCntLsb = 0;
        }
        else
        {
          p_Vid->PrevPicOrderCntMsb = 0;
          p_Vid->PrevPicOrderCntLsb = pSlice->toppoc;
        }
      }
    }
    // Calculate the MSBs of current picture
    if( pSlice->pic_order_cnt_lsb  <  p_Vid->PrevPicOrderCntLsb  &&
      ( p_Vid->PrevPicOrderCntLsb - pSlice->pic_order_cnt_lsb )  >=  ( MaxPicOrderCntLsb / 2 ) )
      pSlice->PicOrderCntMsb = p_Vid->PrevPicOrderCntMsb + MaxPicOrderCntLsb;
    else if ( pSlice->pic_order_cnt_lsb  >  p_Vid->PrevPicOrderCntLsb  &&
      ( pSlice->pic_order_cnt_lsb - p_Vid->PrevPicOrderCntLsb )  >  ( MaxPicOrderCntLsb / 2 ) )
      pSlice->PicOrderCntMsb = p_Vid->PrevPicOrderCntMsb - MaxPicOrderCntLsb;
    else
      pSlice->PicOrderCntMsb = p_Vid->PrevPicOrderCntMsb;

    // 2nd

    if(pSlice->field_pic_flag==0)
    {           //frame pix
      pSlice->toppoc = pSlice->PicOrderCntMsb + pSlice->pic_order_cnt_lsb;
      pSlice->bottompoc = pSlice->toppoc + pSlice->delta_pic_order_cnt_bottom;
      pSlice->ThisPOC = pSlice->framepoc = (pSlice->toppoc < pSlice->bottompoc)? pSlice->toppoc : pSlice->bottompoc; // POC200301
    }
    else if (pSlice->bottom_field_flag == FALSE)
    {  //top field
      pSlice->ThisPOC= pSlice->toppoc = pSlice->PicOrderCntMsb + pSlice->pic_order_cnt_lsb;
    }
    else
    {  //bottom field
      pSlice->ThisPOC= pSlice->bottompoc = pSlice->PicOrderCntMsb + pSlice->pic_order_cnt_lsb;
    }
    pSlice->framepoc = pSlice->ThisPOC;

    p_Vid->ThisPOC = pSlice->ThisPOC;

    //if ( pSlice->frame_num != p_Vid->PreviousFrameNum) //Seems redundant
      p_Vid->PreviousFrameNum = pSlice->frame_num;

    if(pSlice->nal_reference_idc)
    {
      p_Vid->PrevPicOrderCntLsb = pSlice->pic_order_cnt_lsb;
      p_Vid->PrevPicOrderCntMsb = pSlice->PicOrderCntMsb;
    }

    break;

  case 1: // POC MODE 1
    // 1st
    if(pSlice->idr_flag)
    {
      p_Vid->FrameNumOffset=0;     //  first pix of IDRGOP,
      if(pSlice->frame_num)
        error("frame_num not equal to zero in IDR picture", -1020);
    }
    else
    {
      if (p_Vid->last_has_mmco_5)
      {
        p_Vid->PreviousFrameNumOffset = 0;
        p_Vid->PreviousFrameNum = 0;
      }
      if (pSlice->frame_num<p_Vid->PreviousFrameNum)
      {             //not first pix of IDRGOP
        p_Vid->FrameNumOffset = p_Vid->PreviousFrameNumOffset + p_Vid->max_frame_num;
      }
      else
      {
        p_Vid->FrameNumOffset = p_Vid->PreviousFrameNumOffset;
      }
    }

    // 2nd
    if(active_sps->num_ref_frames_in_pic_order_cnt_cycle)
      pSlice->AbsFrameNum = p_Vid->FrameNumOffset+pSlice->frame_num;
    else
      pSlice->AbsFrameNum=0;
    if( (!pSlice->nal_reference_idc) && pSlice->AbsFrameNum > 0)
      pSlice->AbsFrameNum--;

    // 3rd
    p_Vid->ExpectedDeltaPerPicOrderCntCycle=0;

    if(active_sps->num_ref_frames_in_pic_order_cnt_cycle)
    for(i=0;i<(int) active_sps->num_ref_frames_in_pic_order_cnt_cycle;i++)
      p_Vid->ExpectedDeltaPerPicOrderCntCycle += active_sps->offset_for_ref_frame[i];

    if(pSlice->AbsFrameNum)
    {
      p_Vid->PicOrderCntCycleCnt = (pSlice->AbsFrameNum-1)/active_sps->num_ref_frames_in_pic_order_cnt_cycle;
      p_Vid->FrameNumInPicOrderCntCycle = (pSlice->AbsFrameNum-1)%active_sps->num_ref_frames_in_pic_order_cnt_cycle;
      p_Vid->ExpectedPicOrderCnt = p_Vid->PicOrderCntCycleCnt*p_Vid->ExpectedDeltaPerPicOrderCntCycle;
      for(i=0;i<=(int)p_Vid->FrameNumInPicOrderCntCycle;i++)
        p_Vid->ExpectedPicOrderCnt += active_sps->offset_for_ref_frame[i];
    }
    else
      p_Vid->ExpectedPicOrderCnt=0;

    if(!pSlice->nal_reference_idc)
      p_Vid->ExpectedPicOrderCnt += active_sps->offset_for_non_ref_pic;

    if(pSlice->field_pic_flag==0)
    {           //frame pix
      pSlice->toppoc = p_Vid->ExpectedPicOrderCnt + pSlice->delta_pic_order_cnt[0];
      pSlice->bottompoc = pSlice->toppoc + active_sps->offset_for_top_to_bottom_field + pSlice->delta_pic_order_cnt[1];
      pSlice->ThisPOC = pSlice->framepoc = (pSlice->toppoc < pSlice->bottompoc)? pSlice->toppoc : pSlice->bottompoc; // POC200301
    }
    else if (pSlice->bottom_field_flag == FALSE)
    {  //top field
      pSlice->ThisPOC = pSlice->toppoc = p_Vid->ExpectedPicOrderCnt + pSlice->delta_pic_order_cnt[0];
    }
    else
    {  //bottom field
      pSlice->ThisPOC = pSlice->bottompoc = p_Vid->ExpectedPicOrderCnt + active_sps->offset_for_top_to_bottom_field + pSlice->delta_pic_order_cnt[0];
    }
    pSlice->framepoc=pSlice->ThisPOC;

    p_Vid->PreviousFrameNum=pSlice->frame_num;
    p_Vid->PreviousFrameNumOffset=p_Vid->FrameNumOffset;

    break;


  case 2: // POC MODE 2
    if(pSlice->idr_flag) // IDR picture
    {
      p_Vid->FrameNumOffset=0;     //  first pix of IDRGOP,
      pSlice->ThisPOC = pSlice->framepoc = pSlice->toppoc = pSlice->bottompoc = 0;
      if(pSlice->frame_num)
        error("frame_num not equal to zero in IDR picture", -1020);
    }
    else
    {
      if (p_Vid->last_has_mmco_5)
      {
        p_Vid->PreviousFrameNum = 0;
        p_Vid->PreviousFrameNumOffset = 0;
      }
      if (pSlice->frame_num<p_Vid->PreviousFrameNum)
        p_Vid->FrameNumOffset = p_Vid->PreviousFrameNumOffset + p_Vid->max_frame_num;
      else
        p_Vid->FrameNumOffset = p_Vid->PreviousFrameNumOffset;


      pSlice->AbsFrameNum = p_Vid->FrameNumOffset+pSlice->frame_num;
      if(!pSlice->nal_reference_idc)
        pSlice->ThisPOC = (2*pSlice->AbsFrameNum - 1);
      else
        pSlice->ThisPOC = (2*pSlice->AbsFrameNum);

      if (pSlice->field_pic_flag==0)
        pSlice->toppoc = pSlice->bottompoc = pSlice->framepoc = pSlice->ThisPOC;
      else if (pSlice->bottom_field_flag == FALSE)
         pSlice->toppoc = pSlice->framepoc = pSlice->ThisPOC;
      else 
        pSlice->bottompoc = pSlice->framepoc = pSlice->ThisPOC;
    }

    p_Vid->PreviousFrameNum=pSlice->frame_num;
    p_Vid->PreviousFrameNumOffset=p_Vid->FrameNumOffset;
    break;


  default:
    //error must occurs
    assert( 1==0 );
    break;
  }
}

/*!
 ************************************************************************
 * \brief
 *    A little helper for the debugging of POC code
 * \return
 *    none
 ************************************************************************
 */
int dumppoc(VideoParameters *p_Vid) 
{
  seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;

  printf ("\nPOC locals...\n");
  printf ("toppoc                                %d\n", (int) p_Vid->ppSliceList[0]->toppoc);
  printf ("bottompoc                             %d\n", (int) p_Vid->ppSliceList[0]->bottompoc);
  printf ("frame_num                             %d\n", (int) p_Vid->ppSliceList[0]->frame_num);
  printf ("field_pic_flag                        %d\n", (int) p_Vid->ppSliceList[0]->field_pic_flag);
  printf ("bottom_field_flag                     %d\n", (int) p_Vid->ppSliceList[0]->bottom_field_flag);
  printf ("POC SPS\n");
  printf ("log2_max_frame_num_minus4             %d\n", (int) active_sps->log2_max_frame_num_minus4);         // POC200301
  printf ("log2_max_pic_order_cnt_lsb_minus4     %d\n", (int) active_sps->log2_max_pic_order_cnt_lsb_minus4);
  printf ("pic_order_cnt_type                    %d\n", (int) active_sps->pic_order_cnt_type);
  printf ("num_ref_frames_in_pic_order_cnt_cycle %d\n", (int) active_sps->num_ref_frames_in_pic_order_cnt_cycle);
  printf ("delta_pic_order_always_zero_flag      %d\n", (int) active_sps->delta_pic_order_always_zero_flag);
  printf ("offset_for_non_ref_pic                %d\n", (int) active_sps->offset_for_non_ref_pic);
  printf ("offset_for_top_to_bottom_field        %d\n", (int) active_sps->offset_for_top_to_bottom_field);
  printf ("offset_for_ref_frame[0]               %d\n", (int) active_sps->offset_for_ref_frame[0]);
  printf ("offset_for_ref_frame[1]               %d\n", (int) active_sps->offset_for_ref_frame[1]);
  printf ("POC in SLice Header\n");
  printf ("bottom_field_pic_order_in_frame_present_flag                %d\n", (int) p_Vid->active_pps->bottom_field_pic_order_in_frame_present_flag);
  printf ("delta_pic_order_cnt[0]                %d\n", (int) p_Vid->ppSliceList[0]->delta_pic_order_cnt[0]);
  printf ("delta_pic_order_cnt[1]                %d\n", (int) p_Vid->ppSliceList[0]->delta_pic_order_cnt[1]);
  printf ("idr_flag                              %d\n", (int) p_Vid->ppSliceList[0]->idr_flag);
  printf ("max_frame_num                         %d\n", (int) p_Vid->max_frame_num);

  return 0;
}

/*!
 ************************************************************************
 * \brief
 *    return the poc of p_Vid as per (8-1) JVT-F100d2
 *  POC200301
 ************************************************************************
 */
int picture_order( Slice *pSlice )
{
  if (pSlice->field_pic_flag==0) // is a frame
    return pSlice->framepoc;
  else if (pSlice->bottom_field_flag == FALSE) // top field
    return pSlice->toppoc;
  else // bottom field
    return pSlice->bottompoc;
}

