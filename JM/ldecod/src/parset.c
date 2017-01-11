
/*!
 ************************************************************************
 *  \file
 *     parset.c
 *  \brief
 *     Parameter Sets
 *  \author
 *     Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Stephan Wenger          <stewe@cs.tu-berlin.de>
 *
 ***********************************************************************
 */

#include "global.h"
#include "image.h"
#include "parsetcommon.h"
#include "parset.h"
#include "nalu.h"
#include "memalloc.h"
#include "fmo.h"
#include "cabac.h"
#include "vlc.h"
#include "mbuffer.h"
#include "erc_api.h"

#if TRACE
#define SYMTRACESTRING(s) strncpy(sym->tracestring,s,TRACESTRING_SIZE)
#else
#define SYMTRACESTRING(s) // do nothing
#endif

extern void init_frext(VideoParameters *p_Vid);

// syntax for scaling list matrix values
void Scaling_List(int *scalingList, int sizeOfScalingList, Boolean *UseDefaultScalingMatrix, Bitstream *s)
{
  int j, scanj;
  int delta_scale, lastScale, nextScale;

  lastScale      = 8;
  nextScale      = 8;

  for(j=0; j<sizeOfScalingList; j++)
  {
    scanj = (sizeOfScalingList==16) ? ZZ_SCAN[j]:ZZ_SCAN8[j];

    if(nextScale!=0)
    {
      delta_scale = read_se_v (   "   : delta_sl   "                           , s, &p_Dec->UsedBits);
      nextScale = (lastScale + delta_scale + 256) % 256;
      *UseDefaultScalingMatrix = (Boolean) (scanj==0 && nextScale==0);
    }

    scalingList[scanj] = (nextScale==0) ? lastScale:nextScale;
    lastScale = scalingList[scanj];
  }
}
// fill sps with content of p

int InterpretSPS (VideoParameters *p_Vid, DataPartition *p, seq_parameter_set_rbsp_t *sps)
{
  unsigned i;
  unsigned n_ScalingList;
  int reserved_zero;
  Bitstream *s = p->bitstream;

  assert (p != NULL);
  assert (p->bitstream != NULL);
  assert (p->bitstream->streamBuffer != 0);
  assert (sps != NULL);

  p_Dec->UsedBits = 0;

  sps->profile_idc                            = read_u_v  (8, "SPS: profile_idc"                           , s, &p_Dec->UsedBits);

  if ((sps->profile_idc!=BASELINE       ) &&
      (sps->profile_idc!=MAIN           ) &&
      (sps->profile_idc!=EXTENDED       ) &&
      (sps->profile_idc!=FREXT_HP       ) &&
      (sps->profile_idc!=FREXT_Hi10P    ) &&
      (sps->profile_idc!=FREXT_Hi422    ) &&
      (sps->profile_idc!=FREXT_Hi444    ) &&
      (sps->profile_idc!=FREXT_CAVLC444 )
#if (MVC_EXTENSION_ENABLE)
      && (sps->profile_idc!=MVC_HIGH)
      && (sps->profile_idc!=STEREO_HIGH)
#endif
      )
  {
    printf("Invalid Profile IDC (%d) encountered. \n", sps->profile_idc);
    return p_Dec->UsedBits;
  }

  sps->constrained_set0_flag                  = read_u_1  (   "SPS: constrained_set0_flag"                 , s, &p_Dec->UsedBits);
  sps->constrained_set1_flag                  = read_u_1  (   "SPS: constrained_set1_flag"                 , s, &p_Dec->UsedBits);
  sps->constrained_set2_flag                  = read_u_1  (   "SPS: constrained_set2_flag"                 , s, &p_Dec->UsedBits);
  sps->constrained_set3_flag                  = read_u_1  (   "SPS: constrained_set3_flag"                 , s, &p_Dec->UsedBits);
#if (MVC_EXTENSION_ENABLE)
  sps->constrained_set4_flag                  = read_u_1  (   "SPS: constrained_set4_flag"                 , s, &p_Dec->UsedBits);
  sps->constrained_set5_flag                  = read_u_1  (   "SPS: constrained_set5_flag"                 , s, &p_Dec->UsedBits);
  reserved_zero                               = read_u_v  (2, "SPS: reserved_zero_2bits"                   , s, &p_Dec->UsedBits);
#else
  reserved_zero                               = read_u_v  (4, "SPS: reserved_zero_4bits"                   , s, &p_Dec->UsedBits);
#endif
  //assert (reserved_zero==0);
  if (reserved_zero != 0)
  {
    printf("Warning, reserved_zero flag not equal to 0. Possibly new constrained_setX flag introduced.\n");
  }

  sps->level_idc                              = read_u_v  (8, "SPS: level_idc"                             , s, &p_Dec->UsedBits);

  sps->seq_parameter_set_id                   = read_ue_v ("SPS: seq_parameter_set_id"                     , s, &p_Dec->UsedBits);

  // Fidelity Range Extensions stuff
  sps->chroma_format_idc = 1;
  sps->bit_depth_luma_minus8   = 0;
  sps->bit_depth_chroma_minus8 = 0;
  sps->lossless_qpprime_flag   = 0;
  sps->separate_colour_plane_flag = 0;

  if((sps->profile_idc==FREXT_HP   ) ||
     (sps->profile_idc==FREXT_Hi10P) ||
     (sps->profile_idc==FREXT_Hi422) ||
     (sps->profile_idc==FREXT_Hi444) ||
     (sps->profile_idc==FREXT_CAVLC444)
#if (MVC_EXTENSION_ENABLE)
     || (sps->profile_idc==MVC_HIGH)
     || (sps->profile_idc==STEREO_HIGH)
#endif
     )
  {
    sps->chroma_format_idc                      = read_ue_v ("SPS: chroma_format_idc"                       , s, &p_Dec->UsedBits);

    if(sps->chroma_format_idc == YUV444)
    {
      sps->separate_colour_plane_flag           = read_u_1  ("SPS: separate_colour_plane_flag"              , s, &p_Dec->UsedBits);
    }

    sps->bit_depth_luma_minus8                  = read_ue_v ("SPS: bit_depth_luma_minus8"                   , s, &p_Dec->UsedBits);
    sps->bit_depth_chroma_minus8                = read_ue_v ("SPS: bit_depth_chroma_minus8"                 , s, &p_Dec->UsedBits);
    //checking;
    if((sps->bit_depth_luma_minus8+8 > sizeof(imgpel)*8) || (sps->bit_depth_chroma_minus8+8> sizeof(imgpel)*8))
      error ("Source picture has higher bit depth than imgpel data type. \nPlease recompile with larger data type for imgpel.", 500);

    sps->lossless_qpprime_flag                  = read_u_1  ("SPS: lossless_qpprime_y_zero_flag"            , s, &p_Dec->UsedBits);

    sps->seq_scaling_matrix_present_flag        = read_u_1  (   "SPS: seq_scaling_matrix_present_flag"       , s, &p_Dec->UsedBits);
    
    if(sps->seq_scaling_matrix_present_flag)
    {
      n_ScalingList = (sps->chroma_format_idc != YUV444) ? 8 : 12;
      for(i=0; i<n_ScalingList; i++)
      {
        sps->seq_scaling_list_present_flag[i]   = read_u_1  (   "SPS: seq_scaling_list_present_flag"         , s, &p_Dec->UsedBits);
        if(sps->seq_scaling_list_present_flag[i])
        {
          if(i<6)
            Scaling_List(sps->ScalingList4x4[i], 16, &sps->UseDefaultScalingMatrix4x4Flag[i], s);
          else
            Scaling_List(sps->ScalingList8x8[i-6], 64, &sps->UseDefaultScalingMatrix8x8Flag[i-6], s);
        }
      }
    }
  }

  sps->log2_max_frame_num_minus4              = read_ue_v ("SPS: log2_max_frame_num_minus4"                , s, &p_Dec->UsedBits);
  sps->pic_order_cnt_type                     = read_ue_v ("SPS: pic_order_cnt_type"                       , s, &p_Dec->UsedBits);

  if (sps->pic_order_cnt_type == 0)
    sps->log2_max_pic_order_cnt_lsb_minus4 = read_ue_v ("SPS: log2_max_pic_order_cnt_lsb_minus4"           , s, &p_Dec->UsedBits);
  else if (sps->pic_order_cnt_type == 1)
  {
    sps->delta_pic_order_always_zero_flag      = read_u_1  ("SPS: delta_pic_order_always_zero_flag"       , s, &p_Dec->UsedBits);
    sps->offset_for_non_ref_pic                = read_se_v ("SPS: offset_for_non_ref_pic"                 , s, &p_Dec->UsedBits);
    sps->offset_for_top_to_bottom_field        = read_se_v ("SPS: offset_for_top_to_bottom_field"         , s, &p_Dec->UsedBits);
    sps->num_ref_frames_in_pic_order_cnt_cycle = read_ue_v ("SPS: num_ref_frames_in_pic_order_cnt_cycle"  , s, &p_Dec->UsedBits);
    for(i=0; i<sps->num_ref_frames_in_pic_order_cnt_cycle; i++)
      sps->offset_for_ref_frame[i]               = read_se_v ("SPS: offset_for_ref_frame[i]"              , s, &p_Dec->UsedBits);
  }
  sps->num_ref_frames                        = read_ue_v ("SPS: num_ref_frames"                         , s, &p_Dec->UsedBits);
  sps->gaps_in_frame_num_value_allowed_flag  = read_u_1  ("SPS: gaps_in_frame_num_value_allowed_flag"   , s, &p_Dec->UsedBits);
  sps->pic_width_in_mbs_minus1               = read_ue_v ("SPS: pic_width_in_mbs_minus1"                , s, &p_Dec->UsedBits);
  sps->pic_height_in_map_units_minus1        = read_ue_v ("SPS: pic_height_in_map_units_minus1"         , s, &p_Dec->UsedBits);
  sps->frame_mbs_only_flag                   = read_u_1  ("SPS: frame_mbs_only_flag"                    , s, &p_Dec->UsedBits);
  if (!sps->frame_mbs_only_flag)
  {
    sps->mb_adaptive_frame_field_flag        = read_u_1  ("SPS: mb_adaptive_frame_field_flag"           , s, &p_Dec->UsedBits);
  }
  //printf("interlace flags %d %d\n", sps->frame_mbs_only_flag, sps->mb_adaptive_frame_field_flag);
  sps->direct_8x8_inference_flag             = read_u_1  ("SPS: direct_8x8_inference_flag"              , s, &p_Dec->UsedBits);
  sps->frame_cropping_flag                   = read_u_1  ("SPS: frame_cropping_flag"                    , s, &p_Dec->UsedBits);

  if (sps->frame_cropping_flag)
  {
    sps->frame_crop_left_offset      = read_ue_v ("SPS: frame_crop_left_offset"           , s, &p_Dec->UsedBits);
    sps->frame_crop_right_offset     = read_ue_v ("SPS: frame_crop_right_offset"          , s, &p_Dec->UsedBits);
    sps->frame_crop_top_offset       = read_ue_v ("SPS: frame_crop_top_offset"            , s, &p_Dec->UsedBits);
    sps->frame_crop_bottom_offset    = read_ue_v ("SPS: frame_crop_bottom_offset"         , s, &p_Dec->UsedBits);
  }
  sps->vui_parameters_present_flag           = (Boolean) read_u_1  ("SPS: vui_parameters_present_flag"      , s, &p_Dec->UsedBits);

  InitVUI(sps);
  ReadVUI(p, sps);

  sps->Valid = TRUE;
  return p_Dec->UsedBits;
}

// fill subset_sps with content of p
#if (MVC_EXTENSION_ENABLE)
static int InterpretSubsetSPS (VideoParameters *p_Vid, DataPartition *p, int *curr_seq_set_id)
{
  subset_seq_parameter_set_rbsp_t *subset_sps;
  unsigned int additional_extension2_flag;
  Bitstream *s = p->bitstream;
  seq_parameter_set_rbsp_t *sps = AllocSPS();

  assert (p != NULL);
  assert (p->bitstream != NULL);
  assert (p->bitstream->streamBuffer != 0);

  InterpretSPS (p_Vid, p, sps);
  get_max_dec_frame_buf_size(sps);

  *curr_seq_set_id = sps->seq_parameter_set_id;
  subset_sps = p_Vid->SubsetSeqParSet + sps->seq_parameter_set_id;
  if(subset_sps->Valid || subset_sps->num_views_minus1>=0)
  {
    if(memcmp(&subset_sps->sps, sps, sizeof (seq_parameter_set_rbsp_t)-sizeof(int)))
      assert(0);
    reset_subset_sps(subset_sps);
  }
  memcpy (&subset_sps->sps, sps, sizeof (seq_parameter_set_rbsp_t));

  assert (subset_sps != NULL);
  subset_sps->Valid = FALSE;

  /*if(subset_sps->sps.profile_idc == SCALABLE_BASELINE_PROFILE || subset_sps->sps.profile_idc == SCALABLE_HIGH_PROFILE)
  {
    printf("\nScalable profile is not supported yet!\n");
  }
  else*/ 
  if( is_MVC_profile(subset_sps->sps.profile_idc))
  {
    subset_sps->bit_equal_to_one = read_u_1("bit_equal_to_one", s, &p_Dec->UsedBits);

    if(subset_sps->bit_equal_to_one !=1 )
    {
      printf("\nbit_equal_to_one is not equal to 1!\n");
      return p_Dec->UsedBits;
    }

    seq_parameter_set_mvc_extension(subset_sps, s);

    subset_sps->mvc_vui_parameters_present_flag = read_u_1("mvc_vui_parameters_present_flag", s, &p_Dec->UsedBits);
    if(subset_sps->mvc_vui_parameters_present_flag)
      mvc_vui_parameters_extension(&(subset_sps->MVCVUIParams), s);
  }

  additional_extension2_flag = read_u_1("additional_extension2_flag", s, &p_Dec->UsedBits);
  if(additional_extension2_flag)
  {
    while (more_rbsp_data(s->streamBuffer, s->frame_bitoffset,s->bitstream_length))
      additional_extension2_flag = read_u_1("additional_extension2_flag", s, &p_Dec->UsedBits);
  }

  if (subset_sps->sps.Valid)
    subset_sps->Valid = TRUE;

  FreeSPS (sps);
  return p_Dec->UsedBits;

}
#endif

void InitVUI(seq_parameter_set_rbsp_t *sps)
{
  sps->vui_seq_parameters.matrix_coefficients = 2;
}


int ReadVUI(DataPartition *p, seq_parameter_set_rbsp_t *sps)
{
  Bitstream *s = p->bitstream;
  if (sps->vui_parameters_present_flag)
  {
    sps->vui_seq_parameters.aspect_ratio_info_present_flag = read_u_1  ("VUI: aspect_ratio_info_present_flag"   , s, &p_Dec->UsedBits);
    if (sps->vui_seq_parameters.aspect_ratio_info_present_flag)
    {
      sps->vui_seq_parameters.aspect_ratio_idc             = read_u_v  ( 8, "VUI: aspect_ratio_idc"              , s, &p_Dec->UsedBits);
      if (255==sps->vui_seq_parameters.aspect_ratio_idc)
      {
        sps->vui_seq_parameters.sar_width                  = (unsigned short) read_u_v  (16, "VUI: sar_width"                     , s, &p_Dec->UsedBits);
        sps->vui_seq_parameters.sar_height                 = (unsigned short) read_u_v  (16, "VUI: sar_height"                    , s, &p_Dec->UsedBits);
      }
    }

    sps->vui_seq_parameters.overscan_info_present_flag     = read_u_1  ("VUI: overscan_info_present_flag"        , s, &p_Dec->UsedBits);
    if (sps->vui_seq_parameters.overscan_info_present_flag)
    {
      sps->vui_seq_parameters.overscan_appropriate_flag    = read_u_1  ("VUI: overscan_appropriate_flag"         , s, &p_Dec->UsedBits);
    }

    sps->vui_seq_parameters.video_signal_type_present_flag = read_u_1  ("VUI: video_signal_type_present_flag"    , s, &p_Dec->UsedBits);
    if (sps->vui_seq_parameters.video_signal_type_present_flag)
    {
      sps->vui_seq_parameters.video_format                    = read_u_v  ( 3,"VUI: video_format"                      , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.video_full_range_flag           = read_u_1  (   "VUI: video_full_range_flag"             , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.colour_description_present_flag = read_u_1  (   "VUI: color_description_present_flag"    , s, &p_Dec->UsedBits);
      if(sps->vui_seq_parameters.colour_description_present_flag)
      {
        sps->vui_seq_parameters.colour_primaries              = read_u_v  ( 8,"VUI: colour_primaries"                  , s, &p_Dec->UsedBits);
        sps->vui_seq_parameters.transfer_characteristics      = read_u_v  ( 8,"VUI: transfer_characteristics"          , s, &p_Dec->UsedBits);
        sps->vui_seq_parameters.matrix_coefficients           = read_u_v  ( 8,"VUI: matrix_coefficients"               , s, &p_Dec->UsedBits);
      }
    }
    sps->vui_seq_parameters.chroma_location_info_present_flag = read_u_1  (   "VUI: chroma_loc_info_present_flag"      , s, &p_Dec->UsedBits);
    if(sps->vui_seq_parameters.chroma_location_info_present_flag)
    {
      sps->vui_seq_parameters.chroma_sample_loc_type_top_field     = read_ue_v  ( "VUI: chroma_sample_loc_type_top_field"    , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.chroma_sample_loc_type_bottom_field  = read_ue_v  ( "VUI: chroma_sample_loc_type_bottom_field" , s, &p_Dec->UsedBits);
    }
    sps->vui_seq_parameters.timing_info_present_flag          = read_u_1  ("VUI: timing_info_present_flag"           , s, &p_Dec->UsedBits);
    if (sps->vui_seq_parameters.timing_info_present_flag)
    {
      sps->vui_seq_parameters.num_units_in_tick               = read_u_v  (32,"VUI: num_units_in_tick"               , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.time_scale                      = read_u_v  (32,"VUI: time_scale"                      , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.fixed_frame_rate_flag           = read_u_1  (   "VUI: fixed_frame_rate_flag"           , s, &p_Dec->UsedBits);
    }
    sps->vui_seq_parameters.nal_hrd_parameters_present_flag   = read_u_1  ("VUI: nal_hrd_parameters_present_flag"    , s, &p_Dec->UsedBits);
    if (sps->vui_seq_parameters.nal_hrd_parameters_present_flag)
    {
      ReadHRDParameters(p, &(sps->vui_seq_parameters.nal_hrd_parameters));
    }
    sps->vui_seq_parameters.vcl_hrd_parameters_present_flag   = read_u_1  ("VUI: vcl_hrd_parameters_present_flag"    , s, &p_Dec->UsedBits);
    if (sps->vui_seq_parameters.vcl_hrd_parameters_present_flag)
    {
      ReadHRDParameters(p, &(sps->vui_seq_parameters.vcl_hrd_parameters));
    }
    if (sps->vui_seq_parameters.nal_hrd_parameters_present_flag || sps->vui_seq_parameters.vcl_hrd_parameters_present_flag)
    {
      sps->vui_seq_parameters.low_delay_hrd_flag             =  read_u_1  ("VUI: low_delay_hrd_flag"                 , s, &p_Dec->UsedBits);
    }
    sps->vui_seq_parameters.pic_struct_present_flag          =  read_u_1  ("VUI: pic_struct_present_flag   "         , s, &p_Dec->UsedBits);
    sps->vui_seq_parameters.bitstream_restriction_flag       =  read_u_1  ("VUI: bitstream_restriction_flag"         , s, &p_Dec->UsedBits);
    if (sps->vui_seq_parameters.bitstream_restriction_flag)
    {
      sps->vui_seq_parameters.motion_vectors_over_pic_boundaries_flag =  read_u_1  ("VUI: motion_vectors_over_pic_boundaries_flag", s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.max_bytes_per_pic_denom                 =  read_ue_v ("VUI: max_bytes_per_pic_denom"                , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.max_bits_per_mb_denom                   =  read_ue_v ("VUI: max_bits_per_mb_denom"                  , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.log2_max_mv_length_horizontal           =  read_ue_v ("VUI: log2_max_mv_length_horizontal"          , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.log2_max_mv_length_vertical             =  read_ue_v ("VUI: log2_max_mv_length_vertical"            , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.num_reorder_frames                      =  read_ue_v ("VUI: num_reorder_frames"                     , s, &p_Dec->UsedBits);
      sps->vui_seq_parameters.max_dec_frame_buffering                 =  read_ue_v ("VUI: max_dec_frame_buffering"                , s, &p_Dec->UsedBits);
    }
  }

  return 0;
}


int ReadHRDParameters(DataPartition *p, hrd_parameters_t *hrd)
{
  Bitstream *s = p->bitstream;
  unsigned int SchedSelIdx;

  hrd->cpb_cnt_minus1                                      = read_ue_v (   "VUI: cpb_cnt_minus1"                       , s, &p_Dec->UsedBits);
  hrd->bit_rate_scale                                      = read_u_v  ( 4,"VUI: bit_rate_scale"                       , s, &p_Dec->UsedBits);
  hrd->cpb_size_scale                                      = read_u_v  ( 4,"VUI: cpb_size_scale"                       , s, &p_Dec->UsedBits);

  for( SchedSelIdx = 0; SchedSelIdx <= hrd->cpb_cnt_minus1; SchedSelIdx++ )
  {
    hrd->bit_rate_value_minus1[ SchedSelIdx ]             = read_ue_v  ( "VUI: bit_rate_value_minus1"                  , s, &p_Dec->UsedBits);
    hrd->cpb_size_value_minus1[ SchedSelIdx ]             = read_ue_v  ( "VUI: cpb_size_value_minus1"                  , s, &p_Dec->UsedBits);
    hrd->cbr_flag[ SchedSelIdx ]                          = read_u_1   ( "VUI: cbr_flag"                               , s, &p_Dec->UsedBits);
  }

  hrd->initial_cpb_removal_delay_length_minus1            = read_u_v  ( 5,"VUI: initial_cpb_removal_delay_length_minus1" , s, &p_Dec->UsedBits);
  hrd->cpb_removal_delay_length_minus1                    = read_u_v  ( 5,"VUI: cpb_removal_delay_length_minus1"         , s, &p_Dec->UsedBits);
  hrd->dpb_output_delay_length_minus1                     = read_u_v  ( 5,"VUI: dpb_output_delay_length_minus1"          , s, &p_Dec->UsedBits);
  hrd->time_offset_length                                 = read_u_v  ( 5,"VUI: time_offset_length"          , s, &p_Dec->UsedBits);

  return 0;
}


int InterpretPPS (VideoParameters *p_Vid, DataPartition *p, pic_parameter_set_rbsp_t *pps)
{
  unsigned i;
  unsigned n_ScalingList;
  int chroma_format_idc;
  int NumberBitsPerSliceGroupId;
  Bitstream *s = p->bitstream;

  assert (p != NULL);
  assert (p->bitstream != NULL);
  assert (p->bitstream->streamBuffer != 0);
  assert (pps != NULL);

  p_Dec->UsedBits = 0;

  pps->pic_parameter_set_id                  = read_ue_v ("PPS: pic_parameter_set_id"                   , s, &p_Dec->UsedBits);
  pps->seq_parameter_set_id                  = read_ue_v ("PPS: seq_parameter_set_id"                   , s, &p_Dec->UsedBits);
  pps->entropy_coding_mode_flag              = read_u_1  ("PPS: entropy_coding_mode_flag"               , s, &p_Dec->UsedBits);

  //! Note: as per JVT-F078 the following bit is unconditional.  If F078 is not accepted, then
  //! one has to fetch the correct SPS to check whether the bit is present (hopefully there is
  //! no consistency problem :-(
  //! The current encoder code handles this in the same way.  When you change this, don't forget
  //! the encoder!  StW, 12/8/02
  pps->bottom_field_pic_order_in_frame_present_flag = read_u_1  ("PPS: bottom_field_pic_order_in_frame_present_flag"                 , s, &p_Dec->UsedBits);

  pps->num_slice_groups_minus1               = read_ue_v ("PPS: num_slice_groups_minus1"                , s, &p_Dec->UsedBits);

  // FMO stuff begins here
  if (pps->num_slice_groups_minus1 > 0)
  {
    pps->slice_group_map_type               = read_ue_v ("PPS: slice_group_map_type"                , s, &p_Dec->UsedBits);
    if (pps->slice_group_map_type == 0)
    {
      for (i=0; i<=pps->num_slice_groups_minus1; i++)
        pps->run_length_minus1 [i]                  = read_ue_v ("PPS: run_length_minus1 [i]"              , s, &p_Dec->UsedBits);
    }
    else if (pps->slice_group_map_type == 2)
    {
      for (i=0; i<pps->num_slice_groups_minus1; i++)
      {
        //! JVT-F078: avoid reference of SPS by using ue(v) instead of u(v)
        pps->top_left [i]                          = read_ue_v ("PPS: top_left [i]"                        , s, &p_Dec->UsedBits);
        pps->bottom_right [i]                      = read_ue_v ("PPS: bottom_right [i]"                    , s, &p_Dec->UsedBits);
      }
    }
    else if (pps->slice_group_map_type == 3 ||
             pps->slice_group_map_type == 4 ||
             pps->slice_group_map_type == 5)
    {
      pps->slice_group_change_direction_flag     = read_u_1  ("PPS: slice_group_change_direction_flag"      , s, &p_Dec->UsedBits);
      pps->slice_group_change_rate_minus1        = read_ue_v ("PPS: slice_group_change_rate_minus1"         , s, &p_Dec->UsedBits);
    }
    else if (pps->slice_group_map_type == 6)
    {
      if (pps->num_slice_groups_minus1+1 >4)
        NumberBitsPerSliceGroupId = 3;
      else if (pps->num_slice_groups_minus1+1 > 2)
        NumberBitsPerSliceGroupId = 2;
      else
        NumberBitsPerSliceGroupId = 1;
      pps->pic_size_in_map_units_minus1      = read_ue_v ("PPS: pic_size_in_map_units_minus1"               , s, &p_Dec->UsedBits);
      if ((pps->slice_group_id = calloc (pps->pic_size_in_map_units_minus1+1, 1)) == NULL)
        no_mem_exit ("InterpretPPS: slice_group_id");
      for (i=0; i<=pps->pic_size_in_map_units_minus1; i++)
        pps->slice_group_id[i] = (byte) read_u_v (NumberBitsPerSliceGroupId, "slice_group_id[i]", s, &p_Dec->UsedBits);
    }
  }

  // End of FMO stuff

  pps->num_ref_idx_l0_default_active_minus1  = read_ue_v ("PPS: num_ref_idx_l0_default_active_minus1"   , s, &p_Dec->UsedBits);
  pps->num_ref_idx_l1_default_active_minus1  = read_ue_v ("PPS: num_ref_idx_l1_default_active_minus1"   , s, &p_Dec->UsedBits);
  pps->weighted_pred_flag                    = read_u_1  ("PPS: weighted_pred_flag"                     , s, &p_Dec->UsedBits);
  pps->weighted_bipred_idc                   = read_u_v  ( 2, "PPS: weighted_bipred_idc"                , s, &p_Dec->UsedBits);
  pps->pic_init_qp_minus26                   = read_se_v ("PPS: pic_init_qp_minus26"                    , s, &p_Dec->UsedBits);
  pps->pic_init_qs_minus26                   = read_se_v ("PPS: pic_init_qs_minus26"                    , s, &p_Dec->UsedBits);

  pps->chroma_qp_index_offset                = read_se_v ("PPS: chroma_qp_index_offset"                 , s, &p_Dec->UsedBits);

  pps->deblocking_filter_control_present_flag = read_u_1 ("PPS: deblocking_filter_control_present_flag" , s, &p_Dec->UsedBits);
  pps->constrained_intra_pred_flag           = read_u_1  ("PPS: constrained_intra_pred_flag"            , s, &p_Dec->UsedBits);
  pps->redundant_pic_cnt_present_flag        = read_u_1  ("PPS: redundant_pic_cnt_present_flag"         , s, &p_Dec->UsedBits);

  if(more_rbsp_data(s->streamBuffer, s->frame_bitoffset,s->bitstream_length)) // more_data_in_rbsp()
  {
    //Fidelity Range Extensions Stuff
    pps->transform_8x8_mode_flag           =  read_u_1  ("PPS: transform_8x8_mode_flag"                , s, &p_Dec->UsedBits);
    pps->pic_scaling_matrix_present_flag   =  read_u_1  ("PPS: pic_scaling_matrix_present_flag"        , s, &p_Dec->UsedBits);

    if(pps->pic_scaling_matrix_present_flag)
    {
      chroma_format_idc = p_Vid->SeqParSet[pps->seq_parameter_set_id].chroma_format_idc;
      n_ScalingList = 6 + ((chroma_format_idc != YUV444) ? 2 : 6) * pps->transform_8x8_mode_flag;
      for(i=0; i<n_ScalingList; i++)
      {
        pps->pic_scaling_list_present_flag[i]= read_u_1  ("PPS: pic_scaling_list_present_flag"          , s, &p_Dec->UsedBits);

        if(pps->pic_scaling_list_present_flag[i])
        {
          if(i<6)
            Scaling_List(pps->ScalingList4x4[i], 16, &pps->UseDefaultScalingMatrix4x4Flag[i], s);
          else
            Scaling_List(pps->ScalingList8x8[i-6], 64, &pps->UseDefaultScalingMatrix8x8Flag[i-6], s);
        }
      }
    }
    pps->second_chroma_qp_index_offset      = read_se_v ("PPS: second_chroma_qp_index_offset"          , s, &p_Dec->UsedBits);
  }
  else
  {
    pps->second_chroma_qp_index_offset      = pps->chroma_qp_index_offset;
  }

  pps->Valid = TRUE;
  return p_Dec->UsedBits;
}


void PPSConsistencyCheck (pic_parameter_set_rbsp_t *pps)
{
  printf ("Consistency checking a picture parset, to be implemented\n");
//  if (pps->seq_parameter_set_id invalid then do something)
}

void SPSConsistencyCheck (seq_parameter_set_rbsp_t *sps)
{
  printf ("Consistency checking a sequence parset, to be implemented\n");
}

#if (MVC_EXTENSION_ENABLE)
void SubsetSPSConsistencyCheck (subset_seq_parameter_set_rbsp_t *subset_sps)
{
  printf ("Consistency checking a subset sequence parset, to be implemented\n");
}
#endif

void MakePPSavailable (VideoParameters *p_Vid, int id, pic_parameter_set_rbsp_t *pps)
{
  assert (pps->Valid == TRUE);

  if (p_Vid->PicParSet[id].Valid == TRUE && p_Vid->PicParSet[id].slice_group_id != NULL)
    free (p_Vid->PicParSet[id].slice_group_id);

  memcpy (&p_Vid->PicParSet[id], pps, sizeof (pic_parameter_set_rbsp_t));

  // we can simply use the memory provided with the pps. the PPS is destroyed after this function
  // call and will not try to free if pps->slice_group_id == NULL
  p_Vid->PicParSet[id].slice_group_id = pps->slice_group_id;
  pps->slice_group_id          = NULL;
}

void CleanUpPPS(VideoParameters *p_Vid)
{
  int i;

  for (i=0; i<MAXPPS; i++)
  {
    if (p_Vid->PicParSet[i].Valid == TRUE && p_Vid->PicParSet[i].slice_group_id != NULL)
      free (p_Vid->PicParSet[i].slice_group_id);

    p_Vid->PicParSet[i].Valid = FALSE;
  }
}


void MakeSPSavailable (VideoParameters *p_Vid, int id, seq_parameter_set_rbsp_t *sps)
{
  assert (sps->Valid == TRUE);
  memcpy (&p_Vid->SeqParSet[id], sps, sizeof (seq_parameter_set_rbsp_t));
}


void ProcessSPS (VideoParameters *p_Vid, NALU_t *nalu)
{  
  DataPartition *dp = AllocPartition(1);
  seq_parameter_set_rbsp_t *sps = AllocSPS();

  memcpy (dp->bitstream->streamBuffer, &nalu->buf[1], nalu->len-1);
  dp->bitstream->code_len = dp->bitstream->bitstream_length = RBSPtoSODB (dp->bitstream->streamBuffer, nalu->len-1);
  dp->bitstream->ei_flag = 0;
  dp->bitstream->read_len = dp->bitstream->frame_bitoffset = 0;

  InterpretSPS (p_Vid, dp, sps);
#if (MVC_EXTENSION_ENABLE)
  get_max_dec_frame_buf_size(sps);
#endif

  if (sps->Valid)
  {
    if (p_Vid->active_sps)
    {
      if (sps->seq_parameter_set_id == p_Vid->active_sps->seq_parameter_set_id)
      {
        if (!sps_is_equal(sps, p_Vid->active_sps))
        {
          if (p_Vid->dec_picture) // && p_Vid->num_dec_mb == p_Vid->PicSizeInMbs) //?
          {
            // this may only happen on slice loss
            exit_picture(p_Vid, &p_Vid->dec_picture);
          }
          p_Vid->active_sps=NULL;
        }
      }
    }
    // SPSConsistencyCheck (pps);
    MakeSPSavailable (p_Vid, sps->seq_parameter_set_id, sps);
#if (MVC_EXTENSION_ENABLE)
    if (p_Vid->profile_idc < (int) sps->profile_idc)
    {
      p_Vid->profile_idc = sps->profile_idc;
    }
#else
    p_Vid->profile_idc = sps->profile_idc;
#endif
    p_Vid->separate_colour_plane_flag = sps->separate_colour_plane_flag;
    if( p_Vid->separate_colour_plane_flag )
    {
      p_Vid->ChromaArrayType = 0;
    }
    else
    {
      p_Vid->ChromaArrayType = sps->chroma_format_idc;
    }
  }

  FreePartition (dp, 1);
  FreeSPS (sps);
}

#if (MVC_EXTENSION_ENABLE)
void ProcessSubsetSPS (VideoParameters *p_Vid, NALU_t *nalu)
{
  DataPartition *dp = AllocPartition(1);
  subset_seq_parameter_set_rbsp_t *subset_sps;
  int curr_seq_set_id;

  memcpy (dp->bitstream->streamBuffer, &nalu->buf[1], nalu->len-1);
  dp->bitstream->code_len = dp->bitstream->bitstream_length = RBSPtoSODB (dp->bitstream->streamBuffer, nalu->len-1);
  dp->bitstream->ei_flag = 0;
  dp->bitstream->read_len = dp->bitstream->frame_bitoffset = 0;
  InterpretSubsetSPS (p_Vid, dp, &curr_seq_set_id);

  subset_sps = p_Vid->SubsetSeqParSet + curr_seq_set_id;
  get_max_dec_frame_buf_size(&(subset_sps->sps));
  //check capability;
  if(subset_sps->num_views_minus1>1)
  {
    printf("Warning: num_views:%d is greater than 2, only decode baselayer!\n", subset_sps->num_views_minus1+1);
    subset_sps->Valid = 0;
    subset_sps->sps.Valid = 0;
    p_Vid->p_Inp->DecodeAllLayers = 0;
  }
  else if(subset_sps->num_views_minus1==1 && (subset_sps->view_id[0]!=0 || subset_sps->view_id[1]!=1))
  {
    OpenOutputFiles(p_Vid, subset_sps->view_id[0], subset_sps->view_id[1]);
  }

  if (subset_sps->Valid)
  {
    // SubsetSPSConsistencyCheck (subset_sps);
    p_Vid->profile_idc = subset_sps->sps.profile_idc;
    p_Vid->separate_colour_plane_flag = subset_sps->sps.separate_colour_plane_flag;
    if( p_Vid->separate_colour_plane_flag )
    {
      p_Vid->ChromaArrayType = 0;
    }
    else
    {
      p_Vid->ChromaArrayType = subset_sps->sps.chroma_format_idc;
    }
  }

  FreePartition (dp, 1);  
}
#endif

void ProcessPPS (VideoParameters *p_Vid, NALU_t *nalu)
{
  DataPartition *dp = AllocPartition(1);
  pic_parameter_set_rbsp_t *pps = AllocPPS();

  memcpy (dp->bitstream->streamBuffer, &nalu->buf[1], nalu->len-1);
  dp->bitstream->code_len = dp->bitstream->bitstream_length = RBSPtoSODB (dp->bitstream->streamBuffer, nalu->len-1);
  dp->bitstream->ei_flag = 0;
  dp->bitstream->read_len = dp->bitstream->frame_bitoffset = 0;
  InterpretPPS (p_Vid, dp, pps);
  // PPSConsistencyCheck (pps);
  if (p_Vid->active_pps)
  {
    if (pps->pic_parameter_set_id == p_Vid->active_pps->pic_parameter_set_id)
    {
      if(!pps_is_equal(pps, p_Vid->active_pps))
      {
        //copy to next PPS;
        memcpy(p_Vid->pNextPPS, p_Vid->active_pps, sizeof (pic_parameter_set_rbsp_t));
        {
          if (p_Vid->dec_picture) // && p_Vid->num_dec_mb == p_Vid->PicSizeInMbs)
          {
            // this may only happen on slice loss
            exit_picture(p_Vid, &p_Vid->dec_picture);
          }
          p_Vid->active_pps = NULL;
        }
      }
    }
  }
  MakePPSavailable (p_Vid, pps->pic_parameter_set_id, pps);
  FreePartition (dp, 1);
  FreePPS (pps);
}

/*!
 ************************************************************************
 * \brief
 *    Updates images max values
 *
 ************************************************************************
 */
static void updateMaxValue(FrameFormat *format)
{
  format->max_value[0] = (1 << format->bit_depth[0]) - 1;
  format->max_value_sq[0] = format->max_value[0] * format->max_value[0];
  format->max_value[1] = (1 << format->bit_depth[1]) - 1;
  format->max_value_sq[1] = format->max_value[1] * format->max_value[1];
  format->max_value[2] = (1 << format->bit_depth[2]) - 1;
  format->max_value_sq[2] = format->max_value[2] * format->max_value[2];
}

/*!
 ************************************************************************
 * \brief
 *    Reset format information
 *
 ************************************************************************
 */
void reset_format_info(seq_parameter_set_rbsp_t *sps, VideoParameters *p_Vid, FrameFormat *source, FrameFormat *output)
{
  InputParameters *p_Inp = p_Vid->p_Inp;
  static const int SubWidthC  [4]= { 1, 2, 2, 1};
  static const int SubHeightC [4]= { 1, 2, 1, 1};

  int crop_left, crop_right;
  int crop_top, crop_bottom;

  // cropping for luma
  if (sps->frame_cropping_flag)
  {
    crop_left   = SubWidthC [sps->chroma_format_idc] * sps->frame_crop_left_offset;
    crop_right  = SubWidthC [sps->chroma_format_idc] * sps->frame_crop_right_offset;
    crop_top    = SubHeightC[sps->chroma_format_idc] * ( 2 - sps->frame_mbs_only_flag ) *  sps->frame_crop_top_offset;
    crop_bottom = SubHeightC[sps->chroma_format_idc] * ( 2 - sps->frame_mbs_only_flag ) *  sps->frame_crop_bottom_offset;
  }
  else
  {
    crop_left = crop_right = crop_top = crop_bottom = 0;
  }

  source->width[0] = p_Vid->width - crop_left - crop_right;
  source->height[0] = p_Vid->height - crop_top - crop_bottom;

  // cropping for chroma
  if (sps->frame_cropping_flag)
  {
    crop_left   = sps->frame_crop_left_offset;
    crop_right  = sps->frame_crop_right_offset;
    crop_top    = ( 2 - sps->frame_mbs_only_flag ) *  sps->frame_crop_top_offset;
    crop_bottom = ( 2 - sps->frame_mbs_only_flag ) *  sps->frame_crop_bottom_offset;
  }
  else
  {
    crop_left = crop_right = crop_top = crop_bottom = 0;
  }

  if ((sps->chroma_format_idc==YUV400) && p_Inp->write_uv)
  {
    source->width[1]  = (source->width[0] >> 1);
    source->width[2]  = source->width[1];
    source->height[1] = (source->height[0] >> 1);
    source->height[2] = source->height[1];
  }
  else
  {
    source->width[1]  = p_Vid->width_cr - crop_left - crop_right;
    source->width[2]  = source->width[1];
    source->height[1] = p_Vid->height_cr - crop_top - crop_bottom;
    source->height[2] = source->height[1];
  }

  output->width[0]  = p_Vid->width;
  source->width[1]  = p_Vid->width_cr;
  source->width[2]  = p_Vid->width_cr;
  output->height[0] = p_Vid->height;
  output->height[1] = p_Vid->height_cr;
  output->height[2] = p_Vid->height_cr;

  source->size_cmp[0] = source->width[0] * source->height[0];
  source->size_cmp[1] = source->width[1] * source->height[1];
  source->size_cmp[2] = source->size_cmp[1];
  source->size        = source->size_cmp[0] + source->size_cmp[1] + source->size_cmp[2];
  source->mb_width    = source->width[0]  / MB_BLOCK_SIZE;
  source->mb_height   = source->height[0] / MB_BLOCK_SIZE;

  // output size (excluding padding)
  output->size_cmp[0] = output->width[0] * output->height[0];
  output->size_cmp[1] = output->width[1] * output->height[1];
  output->size_cmp[2] = output->size_cmp[1];
  output->size        = output->size_cmp[0] + output->size_cmp[1] + output->size_cmp[2];
  output->mb_width    = output->width[0]  / MB_BLOCK_SIZE;
  output->mb_height   = output->height[0] / MB_BLOCK_SIZE;


  output->bit_depth[0] = source->bit_depth[0] = p_Vid->bitdepth_luma;
  output->bit_depth[1] = source->bit_depth[1] = p_Vid->bitdepth_chroma;
  output->bit_depth[2] = source->bit_depth[2] = p_Vid->bitdepth_chroma;  
  output->pic_unit_size_on_disk = (imax(output->bit_depth[0], output->bit_depth[1]) > 8) ? 16 : 8;
  output->pic_unit_size_shift3 = output->pic_unit_size_on_disk >> 3;

  output->frame_rate  = source->frame_rate;
  output->color_model = source->color_model;
  output->yuv_format  = source->yuv_format = (ColorFormat) sps->chroma_format_idc;

  output->auto_crop_bottom    = crop_bottom;
  output->auto_crop_right     = crop_right;
  output->auto_crop_bottom_cr = (crop_bottom * p_Vid->mb_cr_size_y) / MB_BLOCK_SIZE;
  output->auto_crop_right_cr  = (crop_right * p_Vid->mb_cr_size_x) / MB_BLOCK_SIZE;

  source->auto_crop_bottom    = output->auto_crop_bottom;
  source->auto_crop_right     = output->auto_crop_right;
  source->auto_crop_bottom_cr = output->auto_crop_bottom_cr;
  source->auto_crop_right_cr  = output->auto_crop_right_cr;

  updateMaxValue(source);
  updateMaxValue(output);

  if (p_Vid->first_sps == TRUE) {
    p_Vid->first_sps = FALSE;
    if(!p_Inp->bDisplayDecParams) {
      fprintf(stdout,"Profile IDC  : %d\n", sps->profile_idc);
      fprintf(stdout,"Image Format : %dx%d (%dx%d)\n", source->width[0], source->height[0], p_Vid->width, p_Vid->height);
      if (p_Vid->yuv_format == YUV400)
        fprintf(stdout,"Color Format : 4:0:0 ");
      else if (p_Vid->yuv_format == YUV420)
        fprintf(stdout,"Color Format : 4:2:0 ");
      else if (p_Vid->yuv_format == YUV422)
        fprintf(stdout,"Color Format : 4:2:2 ");
      else
        fprintf(stdout,"Color Format : 4:4:4 ");

      fprintf(stdout,"(%d:%d:%d)\n", source->bit_depth[0], source->bit_depth[1], source->bit_depth[2]);
      fprintf(stdout,"--------------------------------------------------------------------------\n");
    }
    if (!p_Inp->silent)
    {
      fprintf(stdout,"POC must = frame# or field# for SNRs to be correct\n");
      fprintf(stdout,"--------------------------------------------------------------------------\n");
      fprintf(stdout,"  Frame          POC  Pic#   QP    SnrY     SnrU     SnrV   Y:U:V Time(ms)\n");
      fprintf(stdout,"--------------------------------------------------------------------------\n");
    }
  }
}

static void setup_layer_info(VideoParameters *p_Vid, seq_parameter_set_rbsp_t *sps, LayerParameters *p_Lps)
{
  int layer_id = p_Lps->layer_id;
  p_Lps->p_Vid = p_Vid;
  p_Lps->p_Cps = p_Vid->p_EncodePar[layer_id];
  p_Lps->p_SPS = sps;
  p_Lps->p_Dpb = p_Vid->p_Dpb_layer[layer_id];
}

static void set_coding_par(seq_parameter_set_rbsp_t *sps, CodingParameters *cps)
{
  // maximum vertical motion vector range in luma quarter pixel units
  cps->profile_idc = sps->profile_idc;
  cps->lossless_qpprime_flag   = sps->lossless_qpprime_flag;
  if (sps->level_idc <= 10)
  {
    cps->max_vmv_r = 64 * 4;
  }
  else if (sps->level_idc <= 20)
  {
    cps->max_vmv_r = 128 * 4;
  }
  else if (sps->level_idc <= 30)
  {
    cps->max_vmv_r = 256 * 4;
  }
  else
  {
    cps->max_vmv_r = 512 * 4; // 512 pixels in quarter pixels
  }

  // Fidelity Range Extensions stuff (part 1)
  cps->bitdepth_chroma = 0;
  cps->width_cr        = 0;
  cps->height_cr       = 0;
  cps->bitdepth_luma       = (short) (sps->bit_depth_luma_minus8 + 8);
  cps->bitdepth_scale[0]   = 1 << sps->bit_depth_luma_minus8;
  if (sps->chroma_format_idc != YUV400)
  {
    cps->bitdepth_chroma   = (short) (sps->bit_depth_chroma_minus8 + 8);
    cps->bitdepth_scale[1] = 1 << sps->bit_depth_chroma_minus8;
  }

  cps->max_frame_num = 1<<(sps->log2_max_frame_num_minus4+4);
  cps->PicWidthInMbs = (sps->pic_width_in_mbs_minus1 +1);
  cps->PicHeightInMapUnits = (sps->pic_height_in_map_units_minus1 +1);
  cps->FrameHeightInMbs = ( 2 - sps->frame_mbs_only_flag ) * cps->PicHeightInMapUnits;
  cps->FrameSizeInMbs = cps->PicWidthInMbs * cps->FrameHeightInMbs;

  cps->yuv_format=sps->chroma_format_idc;
  cps->separate_colour_plane_flag = sps->separate_colour_plane_flag;
  if( cps->separate_colour_plane_flag )
  {
    cps->ChromaArrayType = 0;
  }
  else
  {
    cps->ChromaArrayType = sps->chroma_format_idc;
  }

  cps->width = cps->PicWidthInMbs * MB_BLOCK_SIZE;
  cps->height = cps->FrameHeightInMbs * MB_BLOCK_SIZE;  

  cps->iLumaPadX = MCBUF_LUMA_PAD_X;
  cps->iLumaPadY = MCBUF_LUMA_PAD_Y;
  cps->iChromaPadX = MCBUF_CHROMA_PAD_X;
  cps->iChromaPadY = MCBUF_CHROMA_PAD_Y;
  if (sps->chroma_format_idc == YUV420)
  {
    cps->width_cr  = (cps->width  >> 1);
    cps->height_cr = (cps->height >> 1);
  }
  else if (sps->chroma_format_idc == YUV422)
  {
    cps->width_cr  = (cps->width >> 1);
    cps->height_cr = cps->height;
    cps->iChromaPadY = MCBUF_CHROMA_PAD_Y*2;
  }
  else if (sps->chroma_format_idc == YUV444)
  {
    //YUV444
    cps->width_cr = cps->width;
    cps->height_cr = cps->height;
    cps->iChromaPadX = cps->iLumaPadX;
    cps->iChromaPadY = cps->iLumaPadY;
  }
  //pel bitdepth init
  cps->bitdepth_luma_qp_scale   = 6 * (cps->bitdepth_luma - 8);

  if(cps->bitdepth_luma > cps->bitdepth_chroma || sps->chroma_format_idc == YUV400)
    cps->pic_unit_bitsize_on_disk = (cps->bitdepth_luma > 8)? 16:8;
  else
    cps->pic_unit_bitsize_on_disk = (cps->bitdepth_chroma > 8)? 16:8;
  cps->dc_pred_value_comp[0]    = 1<<(cps->bitdepth_luma - 1);
  cps->max_pel_value_comp[0] = (1<<cps->bitdepth_luma) - 1;
  cps->mb_size[0][0] = cps->mb_size[0][1] = MB_BLOCK_SIZE;

  if (sps->chroma_format_idc != YUV400)
  {
    //for chrominance part
    cps->bitdepth_chroma_qp_scale = 6 * (cps->bitdepth_chroma - 8);
    cps->dc_pred_value_comp[1]    = (1 << (cps->bitdepth_chroma - 1));
    cps->dc_pred_value_comp[2]    = cps->dc_pred_value_comp[1];
    cps->max_pel_value_comp[1]    = (1 << cps->bitdepth_chroma) - 1;
    cps->max_pel_value_comp[2]    = (1 << cps->bitdepth_chroma) - 1;
    cps->num_blk8x8_uv = (1 << sps->chroma_format_idc) & (~(0x1));
    cps->num_uv_blocks = (cps->num_blk8x8_uv >> 1);
    cps->num_cdc_coeff = (cps->num_blk8x8_uv << 1);
    cps->mb_size[1][0] = cps->mb_size[2][0] = cps->mb_cr_size_x  = (sps->chroma_format_idc==YUV420 || sps->chroma_format_idc==YUV422)?  8 : 16;
    cps->mb_size[1][1] = cps->mb_size[2][1] = cps->mb_cr_size_y  = (sps->chroma_format_idc==YUV444 || sps->chroma_format_idc==YUV422)? 16 :  8;

    cps->subpel_x    = cps->mb_cr_size_x == 8 ? 7 : 3;
    cps->subpel_y    = cps->mb_cr_size_y == 8 ? 7 : 3;
    cps->shiftpel_x  = cps->mb_cr_size_x == 8 ? 3 : 2;
    cps->shiftpel_y  = cps->mb_cr_size_y == 8 ? 3 : 2;
    cps->total_scale = cps->shiftpel_x + cps->shiftpel_y;
  }
  else
  {
    cps->bitdepth_chroma_qp_scale = 0;
    cps->max_pel_value_comp[1] = 0;
    cps->max_pel_value_comp[2] = 0;
    cps->num_blk8x8_uv = 0;
    cps->num_uv_blocks = 0;
    cps->num_cdc_coeff = 0;
    cps->mb_size[1][0] = cps->mb_size[2][0] = cps->mb_cr_size_x  = 0;
    cps->mb_size[1][1] = cps->mb_size[2][1] = cps->mb_cr_size_y  = 0;
    cps->subpel_x      = 0;
    cps->subpel_y      = 0;
    cps->shiftpel_x    = 0;
    cps->shiftpel_y    = 0;
    cps->total_scale   = 0;
  }

  cps->mb_cr_size = cps->mb_cr_size_x * cps->mb_cr_size_y;
  cps->mb_size_blk[0][0] = cps->mb_size_blk[0][1] = cps->mb_size[0][0] >> 2;
  cps->mb_size_blk[1][0] = cps->mb_size_blk[2][0] = cps->mb_size[1][0] >> 2;
  cps->mb_size_blk[1][1] = cps->mb_size_blk[2][1] = cps->mb_size[1][1] >> 2;

  cps->mb_size_shift[0][0] = cps->mb_size_shift[0][1] = CeilLog2_sf (cps->mb_size[0][0]);
  cps->mb_size_shift[1][0] = cps->mb_size_shift[2][0] = CeilLog2_sf (cps->mb_size[1][0]);
  cps->mb_size_shift[1][1] = cps->mb_size_shift[2][1] = CeilLog2_sf (cps->mb_size[1][1]);

  cps->rgb_output =  (sps->vui_seq_parameters.matrix_coefficients==0);
}

/*!
 ************************************************************************
 * \brief
 *    Activate Sequence Parameter Sets
 *
 ************************************************************************
 */
void activate_sps (VideoParameters *p_Vid, seq_parameter_set_rbsp_t *sps)
{
  InputParameters *p_Inp = p_Vid->p_Inp;  

  if (p_Vid->active_sps != sps)
  {
    if (p_Vid->dec_picture)
    {
      // this may only happen on slice loss
      exit_picture(p_Vid, &p_Vid->dec_picture);
    }
    p_Vid->active_sps = sps;

    if(p_Vid->dpb_layer_id==0 && is_BL_profile(sps->profile_idc) && !p_Vid->p_Dpb_layer[0]->init_done)
    {
      set_coding_par(sps, p_Vid->p_EncodePar[0]);
      setup_layer_info( p_Vid, sps, p_Vid->p_LayerPar[0]);
    }
    else if(p_Vid->dpb_layer_id==1 && is_EL_profile(sps->profile_idc) && !p_Vid->p_Dpb_layer[1]->init_done)
    {
      set_coding_par(sps, p_Vid->p_EncodePar[1]);
      setup_layer_info(p_Vid, sps, p_Vid->p_LayerPar[1]);
    }

//to be removed in future;
    set_global_coding_par(p_Vid, p_Vid->p_EncodePar[p_Vid->dpb_layer_id]);
//end;

#if (MVC_EXTENSION_ENABLE)
    //init_frext(p_Vid);
    if (/*p_Vid->last_pic_width_in_mbs_minus1 != p_Vid->active_sps->pic_width_in_mbs_minus1
        || p_Vid->last_pic_height_in_map_units_minus1 != p_Vid->active_sps->pic_height_in_map_units_minus1
        || p_Vid->last_max_dec_frame_buffering != GetMaxDecFrameBuffering(p_Vid)
        || */(p_Vid->last_profile_idc != p_Vid->active_sps->profile_idc && is_BL_profile(p_Vid->active_sps->profile_idc) && !p_Vid->p_Dpb_layer[0]->init_done /*&& is_BL_profile(p_Vid->last_profile_idc)*/))
    {
      //init_frext(p_Vid);
      init_global_buffers(p_Vid, 0);

      if (!p_Vid->no_output_of_prior_pics_flag)
      {
        flush_dpb(p_Vid->p_Dpb_layer[0]);
        flush_dpb(p_Vid->p_Dpb_layer[1]);
      }
      init_dpb(p_Vid, p_Vid->p_Dpb_layer[0], 1);
    }
    else if(p_Vid->last_profile_idc != p_Vid->active_sps->profile_idc && (
            is_MVC_profile(p_Vid->last_profile_idc) || is_MVC_profile(p_Vid->active_sps->profile_idc)
            )&& (!p_Vid->p_Dpb_layer[1]->init_done))
    {
      assert(p_Vid->p_Dpb_layer[0]->init_done);
      //init_frext(p_Vid);
      if(p_Vid->p_Dpb_layer[0]->init_done)
      {
        free_dpb(p_Vid->p_Dpb_layer[0]);
        init_dpb(p_Vid, p_Vid->p_Dpb_layer[0], 1);
      }
      init_global_buffers(p_Vid, 1);
      // for now lets re_init both buffers. Later, we should only re_init appropriate one
      // Note that we seem to be doing this for every frame which seems not good.
      //re_init_dpb(p_Vid, p_Vid->p_Dpb_layer[1], 2);
#if MVC_EXTENSION_ENABLE
      init_dpb(p_Vid, p_Vid->p_Dpb_layer[1], 2);
#endif
      //p_Vid->last_profile_idc = p_Vid->active_sps->profile_idc;
    }
    //p_Vid->p_Dpb_layer[0]->num_ref_frames = p_Vid->active_sps->num_ref_frames;
    //p_Vid->p_Dpb_layer[1]->num_ref_frames = p_Vid->active_sps->num_ref_frames;
    p_Vid->last_pic_width_in_mbs_minus1 = p_Vid->active_sps->pic_width_in_mbs_minus1;  
    p_Vid->last_pic_height_in_map_units_minus1 = p_Vid->active_sps->pic_height_in_map_units_minus1;
    p_Vid->last_max_dec_frame_buffering = GetMaxDecFrameBuffering(p_Vid);
    p_Vid->last_profile_idc = p_Vid->active_sps->profile_idc;

#else
    //init_frext(p_Vid);
    init_global_buffers(p_Vid, 0);

    if (!p_Vid->no_output_of_prior_pics_flag)
    {
      flush_dpb(p_Vid->p_Dpb_layer[0]);
    }
    init_dpb(p_Vid, p_Vid->p_Dpb_layer[0], 0);
    // for now lets init both buffers. Later, we should only re_init appropriate one
    //init_dpb(p_Vid, p_Vid->p_Dpb_layer[0], 1);
    // obviously this is not needed her but just adding it for completeness
    //init_dpb(p_Vid, p_Vid->p_Dpb_layer[1], 2);
#endif

#if (DISABLE_ERC == 0)
    ercInit(p_Vid, p_Vid->width, p_Vid->height, 1);
    if(p_Vid->dec_picture)
    {
      ercReset(p_Vid->erc_errorVar, p_Vid->PicSizeInMbs, p_Vid->PicSizeInMbs, p_Vid->dec_picture->size_x);
      p_Vid->erc_mvperMB = 0;
    }
#endif
  }
  
  reset_format_info(sps, p_Vid, &p_Inp->source, &p_Inp->output);
}

void activate_pps(VideoParameters *p_Vid, pic_parameter_set_rbsp_t *pps)
{  
  if (p_Vid->active_pps != pps)
  {
    if (p_Vid->dec_picture) // && p_Vid->num_dec_mb == p_Vid->pi)
    {
      // this may only happen on slice loss
      exit_picture(p_Vid, &p_Vid->dec_picture);
    }

    p_Vid->active_pps = pps;
  }
}

void UseParameterSet (Slice *currSlice)
{
  VideoParameters *p_Vid = currSlice->p_Vid;
  int PicParsetId = currSlice->pic_parameter_set_id;  
  pic_parameter_set_rbsp_t *pps = &p_Vid->PicParSet[PicParsetId];
  seq_parameter_set_rbsp_t *sps = &p_Vid->SeqParSet[pps->seq_parameter_set_id];
  int i;

  if (pps->Valid != TRUE)
    printf ("Trying to use an invalid (uninitialized) Picture Parameter Set with ID %d, expect the unexpected...\n", PicParsetId);
#if (MVC_EXTENSION_ENABLE)
  if (currSlice->svc_extension_flag == -1)
  {
    if (sps->Valid != TRUE)
      printf ("PicParset %d references an invalid (uninitialized) Sequence Parameter Set with ID %d, expect the unexpected...\n", 
      PicParsetId, (int) pps->seq_parameter_set_id);
  }
  else
  {
    // Set SPS to the subset SPS parameters
    p_Vid->active_subset_sps = p_Vid->SubsetSeqParSet + pps->seq_parameter_set_id;
    sps = &(p_Vid->active_subset_sps->sps);
    if (p_Vid->SubsetSeqParSet[pps->seq_parameter_set_id].Valid != TRUE)
      printf ("PicParset %d references an invalid (uninitialized) Subset Sequence Parameter Set with ID %d, expect the unexpected...\n", 
      PicParsetId, (int) pps->seq_parameter_set_id);
  }
#else
  if (sps->Valid != TRUE)
    printf ("PicParset %d references an invalid (uninitialized) Sequence Parameter Set with ID %d, expect the unexpected...\n", 
    PicParsetId, (int) pps->seq_parameter_set_id);
#endif

  // In theory, and with a well-designed software, the lines above
  // are everything necessary.  In practice, we need to patch many values
  // in p_Vid-> (but no more in p_Inp-> -- these have been taken care of)

  // Set Sequence Parameter Stuff first
  //  printf ("Using Picture Parameter set %d and associated Sequence Parameter Set %d\n", PicParsetId, pps->seq_parameter_set_id);
  if ((int) sps->pic_order_cnt_type < 0 || sps->pic_order_cnt_type > 2)  // != 1
  {
    printf ("invalid sps->pic_order_cnt_type = %d\n", (int) sps->pic_order_cnt_type);
    error ("pic_order_cnt_type != 1", -1000);
  }

  if (sps->pic_order_cnt_type == 1)
  {
    if(sps->num_ref_frames_in_pic_order_cnt_cycle >= MAXnum_ref_frames_in_pic_order_cnt_cycle)
    {
      error("num_ref_frames_in_pic_order_cnt_cycle too large",-1011);
    }
  }
  p_Vid->dpb_layer_id = currSlice->layer_id;
  activate_sps(p_Vid, sps);
  activate_pps(p_Vid, pps);

  // currSlice->dp_mode is set by read_new_slice (NALU first byte available there)
  if (pps->entropy_coding_mode_flag == (Boolean) CAVLC)
  {
    currSlice->nal_startcode_follows = uvlc_startcode_follows;
    for (i=0; i<3; i++)
    {
      currSlice->partArr[i].readSyntaxElement = readSyntaxElement_UVLC;      
    }
  }
  else
  {
    currSlice->nal_startcode_follows = cabac_startcode_follows;
    for (i=0; i<3; i++)
    {
      currSlice->partArr[i].readSyntaxElement = readSyntaxElement_CABAC;
    }
  }
  p_Vid->type = currSlice->slice_type;
}

#if (MVC_EXTENSION_ENABLE)
void seq_parameter_set_mvc_extension(subset_seq_parameter_set_rbsp_t *subset_sps, Bitstream *s)
{
  int i, j, num_views;

  subset_sps->num_views_minus1 = read_ue_v("num_views_minus1", s, &p_Dec->UsedBits);
  num_views = 1+subset_sps->num_views_minus1;
  if( num_views >0)
  {
    if ((subset_sps->view_id = (int*) calloc(num_views, sizeof(int))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->view_id");
    if ((subset_sps->num_anchor_refs_l0 = (int*) calloc(num_views, sizeof(int))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->num_anchor_refs_l0");
    if ((subset_sps->num_anchor_refs_l1 = (int*) calloc(num_views, sizeof(int))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->num_anchor_refs_l1");
    if ((subset_sps->anchor_ref_l0 = (int**) calloc(num_views, sizeof(int*))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->anchor_ref_l0");
    if ((subset_sps->anchor_ref_l1 = (int**) calloc(num_views, sizeof(int*))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->anchor_ref_l1");
    if ((subset_sps->num_non_anchor_refs_l0 = (int*) calloc(num_views, sizeof(int))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->num_non_anchor_refs_l0");
    if ((subset_sps->num_non_anchor_refs_l1 = (int*) calloc(num_views, sizeof(int))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->num_non_anchor_refs_l1");
    if ((subset_sps->non_anchor_ref_l0 = (int**) calloc(num_views, sizeof(int*))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->non_anchor_ref_l0");
    if ((subset_sps->non_anchor_ref_l1 = (int**) calloc(num_views, sizeof(int*))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->non_anchor_ref_l1");
  }
  for(i=0; i<num_views; i++)
  {
    subset_sps->view_id[i] = read_ue_v("view_id", s, &p_Dec->UsedBits);
  }
  for(i=1; i<num_views; i++)
  {
    subset_sps->num_anchor_refs_l0[i] = read_ue_v("num_anchor_refs_l0", s, &p_Dec->UsedBits);
    if(subset_sps->num_anchor_refs_l0[i]>0)
    {
      if ((subset_sps->anchor_ref_l0[i] = (int*) calloc(subset_sps->num_anchor_refs_l0[i], sizeof(int))) == NULL)
        no_mem_exit("init_subset_seq_parameter_set: subset_sps->anchor_ref_l0[i]");
      for(j=0; j<subset_sps->num_anchor_refs_l0[i]; j++)
        subset_sps->anchor_ref_l0[i][j] = read_ue_v("anchor_ref_l0", s, &p_Dec->UsedBits);
    }

    subset_sps->num_anchor_refs_l1[i] = read_ue_v("num_anchor_refs_l1", s, &p_Dec->UsedBits);
    if(subset_sps->num_anchor_refs_l1[i]>0)
    {
      if ((subset_sps->anchor_ref_l1[i] = (int*) calloc(subset_sps->num_anchor_refs_l1[i], sizeof(int))) == NULL)
        no_mem_exit("init_subset_seq_parameter_set: subset_sps->anchor_ref_l1[i]");
      for(j=0; j<subset_sps->num_anchor_refs_l1[i]; j++)
        subset_sps->anchor_ref_l1[i][j] = read_ue_v("anchor_ref_l1", s, &p_Dec->UsedBits);
    }
  }
  for(i=1; i<num_views; i++)
  {
    subset_sps->num_non_anchor_refs_l0[i] = read_ue_v("num_non_anchor_refs_l0", s, &p_Dec->UsedBits);
    if(subset_sps->num_non_anchor_refs_l0[i]>0)
    {
      if ((subset_sps->non_anchor_ref_l0[i] = (int*) calloc(subset_sps->num_non_anchor_refs_l0[i], sizeof(int))) == NULL)
        no_mem_exit("init_subset_seq_parameter_set: subset_sps->non_anchor_ref_l0[i]");
      for(j=0; j<subset_sps->num_non_anchor_refs_l0[i]; j++)
        subset_sps->non_anchor_ref_l0[i][j] = read_ue_v("non_anchor_ref_l0", s, &p_Dec->UsedBits);
    }
    subset_sps->num_non_anchor_refs_l1[i] = read_ue_v("num_non_anchor_refs_l1", s, &p_Dec->UsedBits);
    if(subset_sps->num_non_anchor_refs_l1[i]>0)
    {
      if ((subset_sps->non_anchor_ref_l1[i] = (int*) calloc(subset_sps->num_non_anchor_refs_l1[i], sizeof(int))) == NULL)
        no_mem_exit("init_subset_seq_parameter_set: subset_sps->non_anchor_ref_l1[i]");
      for(j=0; j<subset_sps->num_non_anchor_refs_l1[i]; j++)
        subset_sps->non_anchor_ref_l1[i][j] = read_ue_v("non_anchor_ref_l1", s, &p_Dec->UsedBits);
    }
  }
  subset_sps->num_level_values_signalled_minus1 = read_ue_v("num_level_values_signalled_minus1", s, &p_Dec->UsedBits);
  if(subset_sps->num_level_values_signalled_minus1 >=0)
  {
    i = 1+ subset_sps->num_level_values_signalled_minus1;
    if ((subset_sps->level_idc = (int*) calloc(i, sizeof(int))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->level_idc");
    if ((subset_sps->num_applicable_ops_minus1 = (int*) calloc(i, sizeof(int))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->num_applicable_ops_minus1");
    if ((subset_sps->applicable_op_temporal_id = (int**) calloc(i, sizeof(int*))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_temporal_id");
    if ((subset_sps->applicable_op_num_target_views_minus1 = (int**) calloc(i, sizeof(int*))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_num_target_views_minus1");
    if ((subset_sps->applicable_op_target_view_id = (int***) calloc(i, sizeof(int**))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_target_view_id");
    if ((subset_sps->applicable_op_num_views_minus1 = (int**) calloc(i, sizeof(int*))) == NULL)
      no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_num_views_minus1");
  }
  for(i=0; i<=subset_sps->num_level_values_signalled_minus1; i++)
  {
    subset_sps->level_idc[i] = read_u_v(8, "level_idc", s, &p_Dec->UsedBits);
    subset_sps->num_applicable_ops_minus1[i] = read_ue_v("num_applicable_ops_minus1", s, &p_Dec->UsedBits);
    if(subset_sps->num_applicable_ops_minus1[i]>=0)
    {
      if ((subset_sps->applicable_op_temporal_id[i] = (int*) calloc(1+subset_sps->num_applicable_ops_minus1[i], sizeof(int))) == NULL)
        no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_temporal_id[i]");
      if ((subset_sps->applicable_op_num_target_views_minus1[i] = (int*) calloc(1+subset_sps->num_applicable_ops_minus1[i], sizeof(int))) == NULL)
        no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_num_target_views_minus1[i]");
      if ((subset_sps->applicable_op_target_view_id[i] = (int**) calloc(1+subset_sps->num_applicable_ops_minus1[i], sizeof(int *))) == NULL)
        no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_target_view_id[i]");
      if ((subset_sps->applicable_op_num_views_minus1[i] = (int*) calloc(1+subset_sps->num_applicable_ops_minus1[i], sizeof(int))) == NULL)
        no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_num_views_minus1[i]");

      for(j=0; j<=subset_sps->num_applicable_ops_minus1[i]; j++)
      {
        int k;
        subset_sps->applicable_op_temporal_id[i][j] = read_u_v(3, "applicable_op_temporal_id", s, &p_Dec->UsedBits);
        subset_sps->applicable_op_num_target_views_minus1[i][j] = read_ue_v("applicable_op_num_target_views_minus1", s, &p_Dec->UsedBits);
        if(subset_sps->applicable_op_num_target_views_minus1[i][j]>=0)
        {
          if ((subset_sps->applicable_op_target_view_id[i][j] = (int*) calloc(1+subset_sps->applicable_op_num_target_views_minus1[i][j], sizeof(int))) == NULL)
            no_mem_exit("init_subset_seq_parameter_set: subset_sps->applicable_op_target_view_id[i][j]");
          for(k = 0; k <= subset_sps->applicable_op_num_target_views_minus1[i][j]; k++)
            subset_sps->applicable_op_target_view_id[i][j][k] = read_ue_v("applicable_op_target_view_id", s, &p_Dec->UsedBits);
        }
        subset_sps->applicable_op_num_views_minus1[i][j] = read_ue_v("applicable_op_num_views_minus1", s, &p_Dec->UsedBits);
      }
    }
  }
}

int MemAlloc1D(void** ppBuf, int iEleSize, int iNum)
{
  if(iEleSize*iNum <=0)
    return 1;

  *ppBuf = calloc(iNum, iEleSize);
  return (*ppBuf == NULL);
}

void hrd_parameters(MVCVUI_t *pMVCVUI, Bitstream *s)
{
  int i;

  pMVCVUI->cpb_cnt_minus1 = (char) read_ue_v("cpb_cnt_minus1", s, &p_Dec->UsedBits);
  assert(pMVCVUI->cpb_cnt_minus1<=31);
  pMVCVUI->bit_rate_scale = (char) read_u_v(4, "bit_rate_scale", s, &p_Dec->UsedBits);
  pMVCVUI->cpb_size_scale = (char) read_u_v(4, "cpb_size_scale", s, &p_Dec->UsedBits);
  for(i=0; i<=pMVCVUI->cpb_cnt_minus1; i++)
  {
    pMVCVUI->bit_rate_value_minus1[i] = read_ue_v("bit_rate_value_minus1"                    , s, &p_Dec->UsedBits);
    pMVCVUI->cpb_size_value_minus1[i] = read_ue_v("cpb_size_value_minus1"                    , s, &p_Dec->UsedBits);
    pMVCVUI->cbr_flag[i]              = (char) read_u_1 ("cbr_flag"                          , s, &p_Dec->UsedBits);
  }
  pMVCVUI->initial_cpb_removal_delay_length_minus1 = (char) read_u_v(5, "initial_cpb_removal_delay_length_minus1", s, &p_Dec->UsedBits);
  pMVCVUI->cpb_removal_delay_length_minus1         = (char) read_u_v(5, "cpb_removal_delay_length_minus1",         s, &p_Dec->UsedBits);
  pMVCVUI->dpb_output_delay_length_minus1          = (char) read_u_v(5, "dpb_output_delay_length_minus1",          s, &p_Dec->UsedBits);
  pMVCVUI->time_offset_length                      = (char) read_u_v(5, "time_offset_length",                      s, &p_Dec->UsedBits);

}

void mvc_vui_parameters_extension(MVCVUI_t *pMVCVUI, Bitstream *s)
{
  int i, j, iNumOps;

  pMVCVUI->num_ops_minus1 = read_ue_v("vui_mvc_num_ops_minus1", s, &p_Dec->UsedBits);
  iNumOps = 1+ pMVCVUI->num_ops_minus1;
  if(iNumOps > 0)
  {
    MemAlloc1D((void **)&(pMVCVUI->temporal_id), sizeof(pMVCVUI->temporal_id[0]), iNumOps);
    MemAlloc1D((void **)&(pMVCVUI->num_target_output_views_minus1), sizeof(pMVCVUI->num_target_output_views_minus1[0]), iNumOps);
    if ((pMVCVUI->view_id = (int**) calloc(iNumOps, sizeof(int*))) == NULL)
      no_mem_exit("mvc_vui_parameters_extension: pMVCVUI->view_id");
    MemAlloc1D((void **)&(pMVCVUI->timing_info_present_flag), sizeof(pMVCVUI->timing_info_present_flag[0]), iNumOps);
    MemAlloc1D((void **)&(pMVCVUI->num_units_in_tick), sizeof(pMVCVUI->num_units_in_tick[0]), iNumOps);
    MemAlloc1D((void **)&(pMVCVUI->time_scale), sizeof(pMVCVUI->time_scale[0]), iNumOps);
    MemAlloc1D((void **)&(pMVCVUI->fixed_frame_rate_flag), sizeof(pMVCVUI->fixed_frame_rate_flag[0]), iNumOps);
    MemAlloc1D((void **)&(pMVCVUI->nal_hrd_parameters_present_flag), sizeof(pMVCVUI->nal_hrd_parameters_present_flag[0]), iNumOps);
    MemAlloc1D((void **)&(pMVCVUI->vcl_hrd_parameters_present_flag), sizeof(pMVCVUI->vcl_hrd_parameters_present_flag[0]), iNumOps);
    MemAlloc1D((void **)&(pMVCVUI->low_delay_hrd_flag), sizeof(pMVCVUI->low_delay_hrd_flag[0]), iNumOps);
    MemAlloc1D((void **)&(pMVCVUI->pic_struct_present_flag), sizeof(pMVCVUI->pic_struct_present_flag[0]), iNumOps);

    for(i=0; i<iNumOps; i++)
    {
      pMVCVUI->temporal_id[i] = (char) read_u_v(3, "vui_mvc_temporal_id", s, &p_Dec->UsedBits);
      pMVCVUI->num_target_output_views_minus1[i] = read_ue_v("vui_mvc_num_target_output_views_minus1", s, &p_Dec->UsedBits);
      if(pMVCVUI->num_target_output_views_minus1[i] >= 0)
        MemAlloc1D((void **)&(pMVCVUI->view_id[i]), sizeof(pMVCVUI->view_id[0][0]), pMVCVUI->num_target_output_views_minus1[i]+1);
      for(j=0; j<=pMVCVUI->num_target_output_views_minus1[i]; j++)
        pMVCVUI->view_id[i][j] = read_ue_v("vui_mvc_view_id", s, &p_Dec->UsedBits);
      pMVCVUI->timing_info_present_flag[i] = (char) read_u_1("vui_mvc_timing_info_present_flag", s, &p_Dec->UsedBits);
      if(pMVCVUI->timing_info_present_flag[i])
      {
        pMVCVUI->num_units_in_tick[i]     = read_u_v(32, "vui_mvc_num_units_in_tick", s, &p_Dec->UsedBits); 
        pMVCVUI->time_scale[i]            = read_u_v(32, "vui_mvc_time_scale"          , s, &p_Dec->UsedBits); 
        pMVCVUI->fixed_frame_rate_flag[i] = (char) read_u_1("vui_mvc_fixed_frame_rate_flag", s, &p_Dec->UsedBits);
      }
      pMVCVUI->nal_hrd_parameters_present_flag[i] = (char) read_u_1("vui_mvc_nal_hrd_parameters_present_flag", s, &p_Dec->UsedBits);
      if(pMVCVUI->nal_hrd_parameters_present_flag[i])
        hrd_parameters(pMVCVUI, s);
      pMVCVUI->vcl_hrd_parameters_present_flag[i] = (char) read_u_1("vcl_hrd_parameters_present_flag", s, &p_Dec->UsedBits);
      if(pMVCVUI->vcl_hrd_parameters_present_flag[i])
        hrd_parameters(pMVCVUI, s);
      if(pMVCVUI->nal_hrd_parameters_present_flag[i]||pMVCVUI->vcl_hrd_parameters_present_flag[i])
        pMVCVUI->low_delay_hrd_flag[i]    = (char) read_u_1("vui_mvc_low_delay_hrd_flag", s, &p_Dec->UsedBits);
      pMVCVUI->pic_struct_present_flag[i] = (char) read_u_1("vui_mvc_pic_struct_present_flag", s, &p_Dec->UsedBits);
    }
  }
}

void init_subset_sps_list(subset_seq_parameter_set_rbsp_t *subset_sps_list, int iSize)
{
  int i;
  memset(subset_sps_list, 0, iSize*sizeof(subset_sps_list[0]));
  for(i=0; i<iSize; i++)
  {
    subset_sps_list[i].sps.seq_parameter_set_id = (unsigned int) -1;
    subset_sps_list[i].num_views_minus1 = -1;
    subset_sps_list[i].num_level_values_signalled_minus1 = -1;
    subset_sps_list[i].MVCVUIParams.num_ops_minus1 = -1;
  }
}

void reset_subset_sps(subset_seq_parameter_set_rbsp_t *subset_sps)
{
  int i, j;

  if(subset_sps && subset_sps->num_views_minus1>=0)
  {
    subset_sps->sps.seq_parameter_set_id = (unsigned int) -1;

    free_pointer(subset_sps->view_id);
    for(i=0; i<=subset_sps->num_views_minus1; i++)
    {
      free_pointer(subset_sps->anchor_ref_l0[i]);
      free_pointer(subset_sps->anchor_ref_l1[i]);
    }
    free_pointer(subset_sps->anchor_ref_l0);
    free_pointer(subset_sps->anchor_ref_l1);
    free_pointer(subset_sps->num_anchor_refs_l0);
    free_pointer(subset_sps->num_anchor_refs_l1);

    for(i=0; i<=subset_sps->num_views_minus1; i++)
    {
      free_pointer(subset_sps->non_anchor_ref_l0[i]);
      free_pointer(subset_sps->non_anchor_ref_l1[i]);
    }
    free_pointer(subset_sps->non_anchor_ref_l0);
    free_pointer(subset_sps->non_anchor_ref_l1);
    free_pointer(subset_sps->num_non_anchor_refs_l0);
    free_pointer(subset_sps->num_non_anchor_refs_l1);

    if(subset_sps->num_level_values_signalled_minus1 >= 0)
    {
      free_pointer(subset_sps->level_idc);
      for(i=0; i<=subset_sps->num_level_values_signalled_minus1; i++)
      {
        for(j=0; j<=subset_sps->num_applicable_ops_minus1[i]; j++)
        {
          free_pointer(subset_sps->applicable_op_target_view_id[i][j]);
        }
        free_pointer(subset_sps->applicable_op_target_view_id[i]);
        free_pointer(subset_sps->applicable_op_temporal_id[i]);
        free_pointer(subset_sps->applicable_op_num_target_views_minus1[i]);
        free_pointer(subset_sps->applicable_op_num_views_minus1[i]);
      }
      free_pointer(subset_sps->applicable_op_target_view_id);
      free_pointer(subset_sps->applicable_op_temporal_id);
      free_pointer(subset_sps->applicable_op_num_target_views_minus1);
      free_pointer(subset_sps->applicable_op_num_views_minus1);      
      free_pointer(subset_sps->num_applicable_ops_minus1);

      subset_sps->num_level_values_signalled_minus1 = -1;
    }

    //end;
    subset_sps->num_views_minus1 = -1;
  }

  if(subset_sps && subset_sps->mvc_vui_parameters_present_flag)
  {
    MVCVUI_t *pMVCVUI = &(subset_sps->MVCVUIParams);
    if(pMVCVUI->num_ops_minus1 >=0)
    {
      free_pointer(pMVCVUI->temporal_id);
      free_pointer(pMVCVUI->num_target_output_views_minus1);
      for(i=0; i<=pMVCVUI->num_ops_minus1; i++)
        free_pointer(pMVCVUI->view_id[i]);
      free_pointer(pMVCVUI->view_id);
      free_pointer(pMVCVUI->timing_info_present_flag);
      free_pointer(pMVCVUI->num_units_in_tick);
      free_pointer(pMVCVUI->time_scale);
      free_pointer(pMVCVUI->fixed_frame_rate_flag);
      free_pointer(pMVCVUI->nal_hrd_parameters_present_flag);
      free_pointer(pMVCVUI->vcl_hrd_parameters_present_flag);
      free_pointer(pMVCVUI->low_delay_hrd_flag);
      free_pointer(pMVCVUI->pic_struct_present_flag);

      pMVCVUI->num_ops_minus1 = -1;
    }
    subset_sps->mvc_vui_parameters_present_flag = 0;
  }
}

int GetBaseViewId(VideoParameters *p_Vid, subset_seq_parameter_set_rbsp_t **subset_sps)
{
  subset_seq_parameter_set_rbsp_t *curr_subset_sps;
  int i, iBaseViewId=0; //-1;

  *subset_sps = NULL;
  curr_subset_sps = p_Vid->SubsetSeqParSet;
  for(i=0; i<MAXSPS; i++)
  {
    if(curr_subset_sps->num_views_minus1>=0 && curr_subset_sps->sps.Valid) // && curr_subset_sps->sps.seq_parameter_set_id < MAXSPS)
    {
      iBaseViewId = curr_subset_sps->view_id[BASE_VIEW_IDX];
      break;
    }
    curr_subset_sps++;
  }

  if(i<MAXSPS)
    *subset_sps = curr_subset_sps;
  return iBaseViewId;
}

void get_max_dec_frame_buf_size(seq_parameter_set_rbsp_t *sps)
{
  int pic_size_mb = (sps->pic_width_in_mbs_minus1 + 1) * (sps->pic_height_in_map_units_minus1 + 1) * (sps->frame_mbs_only_flag?1:2);

  int size = 0;

  switch (sps->level_idc)
  {
  case 0:
    // if there is no level defined, we expect experimental usage and return a DPB size of 16
    size = 16 * pic_size_mb;
  case 9:
    size = 396;
    break;
  case 10:
    size = 396;
    break;
  case 11:
    if (!is_FREXT_profile(sps->profile_idc) && (sps->constrained_set3_flag == 1))
      size = 396;
    else
      size = 900;
    break;
  case 12:
    size = 2376;
    break;
  case 13:
    size = 2376;
    break;
  case 20:
    size = 2376;
    break;
  case 21:
    size = 4752;
    break;
  case 22:
    size = 8100;
    break;
  case 30:
    size = 8100;
    break;
  case 31:
    size = 18000;
    break;
  case 32:
    size = 20480;
    break;
  case 40:
    size = 32768;
    break;
  case 41:
    size = 32768;
    break;
  case 42:
    size = 34816;
    break;
  case 50:
    size = 110400;
    break;
  case 51:
    size = 184320;
    break;
  case 52:
    size = 184320;
    break;
  case 60:
  case 61:
  case 62:
    size = 696320;
    break;
  default:
    error ("undefined level", 500);
    break;
  }

  size /= pic_size_mb;
  size = imin( size, 16);
  sps->max_dec_frame_buffering = size;
}
#endif
