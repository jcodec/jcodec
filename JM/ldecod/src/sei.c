/*!
 ************************************************************************
 * \file  sei.c
 *
 * \brief
 *    Functions to implement SEI messages
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Dong Tian        <tian@cs.tut.fi>
 *    - Karsten Suehring
 ************************************************************************
 */

#include "contributors.h"

#include <math.h>
#include "global.h"
#include "memalloc.h"
#include "sei.h"
#include "vlc.h"
#include "header.h"
#include "mbuffer.h"
#include "parset.h"


// #define PRINT_BUFFERING_PERIOD_INFO    // uncomment to print buffering period SEI info
// #define PRINT_PICTURE_TIMING_INFO      // uncomment to print picture timing SEI info
// #define WRITE_MAP_IMAGE                // uncomment to write spare picture map
// #define PRINT_SUBSEQUENCE_INFO         // uncomment to print sub-sequence SEI info
// #define PRINT_SUBSEQUENCE_LAYER_CHAR   // uncomment to print sub-sequence layer characteristics SEI info
// #define PRINT_SUBSEQUENCE_CHAR         // uncomment to print sub-sequence characteristics SEI info
// #define PRINT_SCENE_INFORMATION        // uncomment to print scene information SEI info
// #define PRINT_PAN_SCAN_RECT            // uncomment to print pan-scan rectangle SEI info
// #define PRINT_RECOVERY_POINT           // uncomment to print random access point SEI info
// #define PRINT_FILLER_PAYLOAD_INFO      // uncomment to print filler payload SEI info
// #define PRINT_DEC_REF_PIC_MARKING      // uncomment to print decoded picture buffer management repetition SEI info
// #define PRINT_RESERVED_INFO            // uncomment to print reserved SEI info
// #define PRINT_USER_DATA_UNREGISTERED_INFO          // uncomment to print unregistered user data SEI info
// #define PRINT_USER_DATA_REGISTERED_ITU_T_T35_INFO  // uncomment to print ITU-T T.35 user data SEI info
// #define PRINT_FULL_FRAME_FREEZE_INFO               // uncomment to print full-frame freeze SEI info
// #define PRINT_FULL_FRAME_FREEZE_RELEASE_INFO       // uncomment to print full-frame freeze release SEI info
// #define PRINT_FULL_FRAME_SNAPSHOT_INFO             // uncomment to print full-frame snapshot SEI info
// #define PRINT_PROGRESSIVE_REFINEMENT_END_INFO      // uncomment to print Progressive refinement segment start SEI info
// #define PRINT_PROGRESSIVE_REFINEMENT_END_INFO      // uncomment to print Progressive refinement segment end SEI info
// #define PRINT_MOTION_CONST_SLICE_GROUP_SET_INFO    // uncomment to print Motion-constrained slice group set SEI info
// #define PRINT_FILM_GRAIN_CHARACTERISTICS_INFO      // uncomment to print Film grain characteristics SEI info
// #define PRINT_DEBLOCKING_FILTER_DISPLAY_PREFERENCE_INFO // uncomment to print deblocking filter display preference SEI info
// #define PRINT_STEREO_VIDEO_INFO_INFO               // uncomment to print stereo video SEI info
// #define PRINT_TONE_MAPPING                         // uncomment to print tone-mapping SEI info
// #define PRINT_POST_FILTER_HINT_INFO                // uncomment to print post-filter hint SEI info
// #define PRINT_FRAME_PACKING_ARRANGEMENT_INFO       // uncomment to print frame packing arrangement SEI info
// #define PRINT_GREEN_METADATA_INFO      // uncomment to print Green Metadata SEI info

/*!
 ************************************************************************
 *  \brief
 *     Interpret the SEI rbsp
 *  \param msg
 *     a pointer that point to the sei message.
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void InterpretSEIMessage(byte* msg, int size, VideoParameters *p_Vid, Slice *pSlice)
{
  int payload_type = 0;
  int payload_size = 0;
  int offset = 1;
  byte tmp_byte;
  
  do
  {
    // sei_message();
    payload_type = 0;
    tmp_byte = msg[offset++];
    while (tmp_byte == 0xFF)
    {
      payload_type += 255;
      tmp_byte = msg[offset++];
    }
    payload_type += tmp_byte;   // this is the last byte

    payload_size = 0;
    tmp_byte = msg[offset++];
    while (tmp_byte == 0xFF)
    {
      payload_size += 255;
      tmp_byte = msg[offset++];
    }
    payload_size += tmp_byte;   // this is the last byte

    switch ( payload_type )     // sei_payload( type, size );
    {
    case  SEI_BUFFERING_PERIOD:
      interpret_buffering_period_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_PIC_TIMING:
      interpret_picture_timing_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_PAN_SCAN_RECT:
      interpret_pan_scan_rect_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_FILLER_PAYLOAD:
      interpret_filler_payload_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_USER_DATA_REGISTERED_ITU_T_T35:
      interpret_user_data_registered_itu_t_t35_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_USER_DATA_UNREGISTERED:
      interpret_user_data_unregistered_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_RECOVERY_POINT:
      interpret_recovery_point_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_DEC_REF_PIC_MARKING_REPETITION:
      interpret_dec_ref_pic_marking_repetition_info( msg+offset, payload_size, p_Vid, pSlice );
      break;
    case  SEI_SPARE_PIC:
      interpret_spare_pic( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_SCENE_INFO:
      interpret_scene_information( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_SUB_SEQ_INFO:
      interpret_subsequence_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_SUB_SEQ_LAYER_CHARACTERISTICS:
      interpret_subsequence_layer_characteristics_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_SUB_SEQ_CHARACTERISTICS:
      interpret_subsequence_characteristics_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_FULL_FRAME_FREEZE:
      interpret_full_frame_freeze_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_FULL_FRAME_FREEZE_RELEASE:
      interpret_full_frame_freeze_release_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_FULL_FRAME_SNAPSHOT:
      interpret_full_frame_snapshot_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_PROGRESSIVE_REFINEMENT_SEGMENT_START:
      interpret_progressive_refinement_start_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_PROGRESSIVE_REFINEMENT_SEGMENT_END:
      interpret_progressive_refinement_end_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_MOTION_CONSTRAINED_SLICE_GROUP_SET:
      interpret_motion_constrained_slice_group_set_info( msg+offset, payload_size, p_Vid );
    case  SEI_FILM_GRAIN_CHARACTERISTICS:
      interpret_film_grain_characteristics_info ( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_DEBLOCKING_FILTER_DISPLAY_PREFERENCE:
      interpret_deblocking_filter_display_preference_info ( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_STEREO_VIDEO_INFO:
      interpret_stereo_video_info_info ( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_TONE_MAPPING:
      interpret_tone_mapping( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_POST_FILTER_HINTS:
      interpret_post_filter_hints_info ( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_FRAME_PACKING_ARRANGEMENT:
      interpret_frame_packing_arrangement_info( msg+offset, payload_size, p_Vid );
      break;
    case  SEI_GREEN_METADATA:
      interpret_green_metadata_info( msg+offset, payload_size, p_Vid );
      break;
    default:
      interpret_reserved_info( msg+offset, payload_size, p_Vid );
      break;    
    }
    offset += payload_size;

  } while( msg[offset] != 0x80 );    // more_rbsp_data()  msg[offset] != 0x80
  // ignore the trailing bits rbsp_trailing_bits();
  assert(msg[offset] == 0x80);      // this is the trailing bits
  assert( offset+1 == size );
}


/*!
************************************************************************
*  \brief
*     Interpret the spare picture SEI message
*  \param payload
*     a pointer that point to the sei payload
*  \param size
*     the size of the sei message
*  \param p_Vid
*     the image pointer
*
************************************************************************
*/
void interpret_spare_pic( byte* payload, int size, VideoParameters *p_Vid )
{
  int i,x,y;
  Bitstream* buf;
  int bit0, bit1, bitc, no_bit0;
  int target_frame_num = 0;
  int num_spare_pics;
  int delta_spare_frame_num, CandidateSpareFrameNum, SpareFrameNum = 0;
  int ref_area_indicator;

  int m, n, left, right, top, bottom,directx, directy;
  byte ***map;

#ifdef WRITE_MAP_IMAGE
  int symbol_size_in_bytes = p_Vid->pic_unit_bitsize_on_disk/8;
  int  j, k, i0, j0, tmp, kk;
  char filename[20] = "map_dec.yuv";
  FILE *fp;
  imgpel** Y;
  static int old_pn=-1;
  static int first = 1;

  printf("Spare picture SEI message\n");
#endif

  p_Dec->UsedBits = 0;

  assert( payload!=NULL);
  assert( p_Vid!=NULL);

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  target_frame_num = read_ue_v("SEI: target_frame_num", buf, &p_Dec->UsedBits);

#ifdef WRITE_MAP_IMAGE
  printf( "target_frame_num is %d\n", target_frame_num );
#endif

  num_spare_pics = 1 + read_ue_v("SEI: num_spare_pics_minus1", buf, &p_Dec->UsedBits);

#ifdef WRITE_MAP_IMAGE
  printf( "num_spare_pics is %d\n", num_spare_pics );
#endif

  get_mem3D(&map, num_spare_pics, p_Vid->height >> 4, p_Vid->width >> 4);

  for (i=0; i<num_spare_pics; i++)
  {
    if (i==0)
    {
      CandidateSpareFrameNum = target_frame_num - 1;
      if ( CandidateSpareFrameNum < 0 ) CandidateSpareFrameNum = MAX_FN - 1;
    }
    else
      CandidateSpareFrameNum = SpareFrameNum;

    delta_spare_frame_num = read_ue_v("SEI: delta_spare_frame_num", buf, &p_Dec->UsedBits);

    SpareFrameNum = CandidateSpareFrameNum - delta_spare_frame_num;
    if( SpareFrameNum < 0 )
      SpareFrameNum = MAX_FN + SpareFrameNum;

    ref_area_indicator = read_ue_v("SEI: ref_area_indicator", buf, &p_Dec->UsedBits);

    switch ( ref_area_indicator )
    {
    case 0:   // The whole frame can serve as spare picture
      for (y=0; y<p_Vid->height >> 4; y++)
        for (x=0; x<p_Vid->width >> 4; x++)
          map[i][y][x] = 0;
      break;
    case 1:   // The map is not compressed
      for (y=0; y<p_Vid->height >> 4; y++)
        for (x=0; x<p_Vid->width >> 4; x++)
        {
          map[i][y][x] = (byte) read_u_1("SEI: ref_mb_indicator", buf, &p_Dec->UsedBits);
        }
      break;
    case 2:   // The map is compressed
              //!KS: could not check this function, description is unclear (as stated in Ed. Note)
      bit0 = 0;
      bit1 = 1;
      bitc = bit0;
      no_bit0 = -1;

      x = ( (p_Vid->width >> 4) - 1 ) / 2;
      y = ( (p_Vid->height >> 4) - 1 ) / 2;
      left = right = x;
      top = bottom = y;
      directx = 0;
      directy = 1;

      for (m=0; m<p_Vid->height >> 4; m++)
        for (n=0; n<p_Vid->width >> 4; n++)
        {

          if (no_bit0<0)
          {
            no_bit0 = read_ue_v("SEI: zero_run_length", buf, &p_Dec->UsedBits);
          }
          if (no_bit0>0) 
            map[i][y][x] = (byte) bit0;
          else 
            map[i][y][x] = (byte) bit1;
          no_bit0--;

          // go to the next mb:
          if ( directx == -1 && directy == 0 )
          {
            if (x > left) x--;
            else if (x == 0)
            {
              y = bottom + 1;
              bottom++;
              directx = 1;
              directy = 0;
            }
            else if (x == left)
            {
              x--;
              left--;
              directx = 0;
              directy = 1;
            }
          }
          else if ( directx == 1 && directy == 0 )
          {
            if (x < right) x++;
            else if (x == (p_Vid->width >> 4) - 1)
            {
              y = top - 1;
              top--;
              directx = -1;
              directy = 0;
            }
            else if (x == right)
            {
              x++;
              right++;
              directx = 0;
              directy = -1;
            }
          }
          else if ( directx == 0 && directy == -1 )
          {
            if ( y > top) y--;
            else if (y == 0)
            {
              x = left - 1;
              left--;
              directx = 0;
              directy = 1;
            }
            else if (y == top)
            {
              y--;
              top--;
              directx = -1;
              directy = 0;
            }
          }
          else if ( directx == 0 && directy == 1 )
          {
            if (y < bottom) y++;
            else if (y == (p_Vid->height >> 4) - 1)
            {
              x = right+1;
              right++;
              directx = 0;
              directy = -1;
            }
            else if (y == bottom)
            {
              y++;
              bottom++;
              directx = 1;
              directy = 0;
            }
          }


        }
      break;
    default:
      printf( "Wrong ref_area_indicator %d!\n", ref_area_indicator );
      exit(0);
      break;
    }

  } // end of num_spare_pics

#ifdef WRITE_MAP_IMAGE
  // begin to write map seq
  if ( old_pn != p_Vid->number )
  {
    old_pn = p_Vid->number;
    get_mem2Dpel(&Y, p_Vid->height, p_Vid->width);
    if (first)
    {
      fp = fopen( filename, "wb" );
      first = 0;
    }
    else
      fp = fopen( filename, "ab" );
    assert( fp != NULL );
    for (kk=0; kk<num_spare_pics; kk++)
    {
      for (i=0; i < p_Vid->height >> 4; i++)
        for (j=0; j < p_Vid->width >> 4; j++)
        {
          tmp=map[kk][i][j]==0? p_Vid->max_pel_value_comp[0] : 0;
          for (i0=0; i0<16; i0++)
            for (j0=0; j0<16; j0++)
              Y[i*16+i0][j*16+j0]=tmp;
        }

      // write the map image
      for (i=0; i < p_Vid->height; i++)
        for (j=0; j < p_Vid->width; j++)
          fwrite(&(Y[i][j]), symbol_size_in_bytes, 1, p_out);

      for (k=0; k < 2; k++)
        for (i=0; i < p_Vid->height>>1; i++)
          for (j=0; j < p_Vid->width>>1; j++)
            fwrite(&(p_Vid->dc_pred_value_comp[1]), symbol_size_in_bytes, 1, p_out);
    }
    fclose( fp );
    free_mem2Dpel( Y );
  }
  // end of writing map image
#undef WRITE_MAP_IMAGE
#endif

  free_mem3D( map );

  free(buf);
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Sub-sequence information SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_subsequence_info( byte* payload, int size, VideoParameters *p_Vid )
{
  Bitstream* buf;
  int sub_seq_layer_num, sub_seq_id, first_ref_pic_flag, leading_non_ref_pic_flag, last_pic_flag,
    sub_seq_frame_num_flag, sub_seq_frame_num;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  sub_seq_layer_num        = read_ue_v("SEI: sub_seq_layer_num"       , buf, &p_Dec->UsedBits);
  sub_seq_id               = read_ue_v("SEI: sub_seq_id"              , buf, &p_Dec->UsedBits);
  first_ref_pic_flag       = read_u_1 ("SEI: first_ref_pic_flag"      , buf, &p_Dec->UsedBits);
  leading_non_ref_pic_flag = read_u_1 ("SEI: leading_non_ref_pic_flag", buf, &p_Dec->UsedBits);
  last_pic_flag            = read_u_1 ("SEI: last_pic_flag"           , buf, &p_Dec->UsedBits);
  sub_seq_frame_num_flag   = read_u_1 ("SEI: sub_seq_frame_num_flag"  , buf, &p_Dec->UsedBits);
  if (sub_seq_frame_num_flag)
  {
    sub_seq_frame_num        = read_ue_v("SEI: sub_seq_frame_num"       , buf, &p_Dec->UsedBits);
  }

#ifdef PRINT_SUBSEQUENCE_INFO
  printf("Sub-sequence information SEI message\n");
  printf("sub_seq_layer_num        = %d\n", sub_seq_layer_num );
  printf("sub_seq_id               = %d\n", sub_seq_id);
  printf("first_ref_pic_flag       = %d\n", first_ref_pic_flag);
  printf("leading_non_ref_pic_flag = %d\n", leading_non_ref_pic_flag);
  printf("last_pic_flag            = %d\n", last_pic_flag);
  printf("sub_seq_frame_num_flag   = %d\n", sub_seq_frame_num_flag);
  if (sub_seq_frame_num_flag)
  {
    printf("sub_seq_frame_num        = %d\n", sub_seq_frame_num);
  }
#endif

  free(buf);
#ifdef PRINT_SUBSEQUENCE_INFO
#undef PRINT_SUBSEQUENCE_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the Sub-sequence layer characteristics SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_subsequence_layer_characteristics_info( byte* payload, int size, VideoParameters *p_Vid )
{
  Bitstream* buf;
  long num_sub_layers, accurate_statistics_flag, average_bit_rate, average_frame_rate;
  int i;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  num_sub_layers = 1 + read_ue_v("SEI: num_sub_layers_minus1", buf, &p_Dec->UsedBits);

#ifdef PRINT_SUBSEQUENCE_LAYER_CHAR
  printf("Sub-sequence layer characteristics SEI message\n");
  printf("num_sub_layers_minus1 = %d\n", num_sub_layers - 1);
#endif

  for (i=0; i<num_sub_layers; i++)
  {
    accurate_statistics_flag = read_u_1(   "SEI: accurate_statistics_flag", buf, &p_Dec->UsedBits);
    average_bit_rate         = read_u_v(16,"SEI: average_bit_rate"        , buf, &p_Dec->UsedBits);
    average_frame_rate       = read_u_v(16,"SEI: average_frame_rate"      , buf, &p_Dec->UsedBits);

#ifdef PRINT_SUBSEQUENCE_LAYER_CHAR
    printf("layer %d: accurate_statistics_flag = %ld \n", i, accurate_statistics_flag);
    printf("layer %d: average_bit_rate         = %ld \n", i, average_bit_rate);
    printf("layer %d: average_frame_rate       = %ld \n", i, average_frame_rate);
#endif
  }
  free (buf);
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Sub-sequence characteristics SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_subsequence_characteristics_info( byte* payload, int size, VideoParameters *p_Vid )
{
  Bitstream* buf;
  int i;
  int sub_seq_layer_num, sub_seq_id, duration_flag, average_rate_flag, accurate_statistics_flag;
  unsigned long sub_seq_duration, average_bit_rate, average_frame_rate;
  int num_referenced_subseqs, ref_sub_seq_layer_num, ref_sub_seq_id, ref_sub_seq_direction;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  sub_seq_layer_num = read_ue_v("SEI: sub_seq_layer_num", buf, &p_Dec->UsedBits);
  sub_seq_id        = read_ue_v("SEI: sub_seq_id", buf, &p_Dec->UsedBits);
  duration_flag     = read_u_1 ("SEI: duration_flag", buf, &p_Dec->UsedBits);

#ifdef PRINT_SUBSEQUENCE_CHAR
  printf("Sub-sequence characteristics SEI message\n");
  printf("sub_seq_layer_num = %d\n", sub_seq_layer_num );
  printf("sub_seq_id        = %d\n", sub_seq_id);
  printf("duration_flag     = %d\n", duration_flag);
#endif

  if ( duration_flag )
  {
    sub_seq_duration = read_u_v (32, "SEI: duration_flag", buf, &p_Dec->UsedBits);
#ifdef PRINT_SUBSEQUENCE_CHAR
    printf("sub_seq_duration = %ld\n", sub_seq_duration);
#endif
  }

  average_rate_flag = read_u_1 ("SEI: average_rate_flag", buf, &p_Dec->UsedBits);

#ifdef PRINT_SUBSEQUENCE_CHAR
  printf("average_rate_flag = %d\n", average_rate_flag);
#endif

  if ( average_rate_flag )
  {
    accurate_statistics_flag = read_u_1 (    "SEI: accurate_statistics_flag", buf, &p_Dec->UsedBits);
    average_bit_rate         = read_u_v (16, "SEI: average_bit_rate", buf, &p_Dec->UsedBits);
    average_frame_rate       = read_u_v (16, "SEI: average_frame_rate", buf, &p_Dec->UsedBits);

#ifdef PRINT_SUBSEQUENCE_CHAR
    printf("accurate_statistics_flag = %d\n", accurate_statistics_flag);
    printf("average_bit_rate         = %ld\n", average_bit_rate);
    printf("average_frame_rate       = %ld\n", average_frame_rate);
#endif
  }

  num_referenced_subseqs  = read_ue_v("SEI: num_referenced_subseqs", buf, &p_Dec->UsedBits);

#ifdef PRINT_SUBSEQUENCE_CHAR
  printf("num_referenced_subseqs = %d\n", num_referenced_subseqs);
#endif

  for (i=0; i<num_referenced_subseqs; i++)
  {
    ref_sub_seq_layer_num  = read_ue_v("SEI: ref_sub_seq_layer_num", buf, &p_Dec->UsedBits);
    ref_sub_seq_id         = read_ue_v("SEI: ref_sub_seq_id", buf, &p_Dec->UsedBits);
    ref_sub_seq_direction  = read_u_1 ("SEI: ref_sub_seq_direction", buf, &p_Dec->UsedBits);

#ifdef PRINT_SUBSEQUENCE_CHAR
    printf("ref_sub_seq_layer_num = %d\n", ref_sub_seq_layer_num);
    printf("ref_sub_seq_id        = %d\n", ref_sub_seq_id);
    printf("ref_sub_seq_direction = %d\n", ref_sub_seq_direction);
#endif
  }

  free( buf );
#ifdef PRINT_SUBSEQUENCE_CHAR
#undef PRINT_SUBSEQUENCE_CHAR
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Scene information SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_scene_information( byte* payload, int size, VideoParameters *p_Vid )
{
  Bitstream* buf;
  int scene_id, scene_transition_type, second_scene_id;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  scene_id              = read_ue_v("SEI: scene_id"             , buf, &p_Dec->UsedBits);
  scene_transition_type = read_ue_v("SEI: scene_transition_type", buf, &p_Dec->UsedBits);
  if ( scene_transition_type > 3 )
  {
    second_scene_id     = read_ue_v("SEI: scene_transition_type", buf, &p_Dec->UsedBits);
  }

#ifdef PRINT_SCENE_INFORMATION
  printf("Scene information SEI message\n");
  printf("scene_transition_type = %d\n", scene_transition_type);
  printf("scene_id              = %d\n", scene_id);
  if ( scene_transition_type > 3 )
  {
    printf("second_scene_id       = %d\n", second_scene_id);
  }
#endif
  free( buf );
#ifdef PRINT_SCENE_INFORMATION
#undef PRINT_SCENE_INFORMATION
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Filler payload SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_filler_payload_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int payload_cnt = 0;

  while (payload_cnt<size)
  {
    if (payload[payload_cnt] == 0xFF)
    {
       payload_cnt++;
    }
  }


#ifdef PRINT_FILLER_PAYLOAD_INFO
  printf("Filler payload SEI message\n");
  if (payload_cnt==size)
  {
    printf("read %d bytes of filler payload\n", payload_cnt);
  }
  else
  {
    printf("error reading filler payload: not all bytes are 0xFF (%d of %d)\n", payload_cnt, size);
  }
#endif

#ifdef PRINT_FILLER_PAYLOAD_INFO
#undef PRINT_FILLER_PAYLOAD_INFO
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the User data unregistered SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_user_data_unregistered_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int offset = 0;
  byte payload_byte;

#ifdef PRINT_USER_DATA_UNREGISTERED_INFO
  printf("User data unregistered SEI message\n");
  printf("uuid_iso_11578 = 0x");
#endif
  assert (size>=16);

  for (offset = 0; offset < 16; offset++)
  {
#ifdef PRINT_USER_DATA_UNREGISTERED_INFO
    printf("%02x",payload[offset]);
#endif
  }

#ifdef PRINT_USER_DATA_UNREGISTERED_INFO
    printf("\n");
#endif

  while (offset < size)
  {
    payload_byte = payload[offset];
    offset ++;
#ifdef PRINT_USER_DATA_UNREGISTERED_INFO
    printf("Unreg data payload_byte = %d\n", payload_byte);
#endif
  }
#ifdef PRINT_USER_DATA_UNREGISTERED_INFO
#undef PRINT_USER_DATA_UNREGISTERED_INFO
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the User data registered by ITU-T T.35 SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_user_data_registered_itu_t_t35_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int offset = 0;
  byte itu_t_t35_country_code, itu_t_t35_country_code_extension_byte, payload_byte;

  itu_t_t35_country_code = payload[offset];
  offset++;
#ifdef PRINT_USER_DATA_REGISTERED_ITU_T_T35_INFO
  printf("User data registered by ITU-T T.35 SEI message\n");
  printf(" itu_t_t35_country_code = %d \n", itu_t_t35_country_code);
#endif
  if(itu_t_t35_country_code == 0xFF)
  {
    itu_t_t35_country_code_extension_byte = payload[offset];
    offset++;
#ifdef PRINT_USER_DATA_REGISTERED_ITU_T_T35_INFO
    printf(" ITU_T_T35_COUNTRY_CODE_EXTENSION_BYTE %d \n", itu_t_t35_country_code_extension_byte);
#endif
  }
  while (offset < size)
  {
    payload_byte = payload[offset];
    offset ++;
#ifdef PRINT_USER_DATA_REGISTERED_ITU_T_T35_INFO
    printf("itu_t_t35 payload_byte = %d\n", payload_byte);
#endif
  }
#ifdef PRINT_USER_DATA_REGISTERED_ITU_T_T35_INFO
#undef PRINT_USER_DATA_REGISTERED_ITU_T_T35_INFO
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Pan scan rectangle SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_pan_scan_rect_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int pan_scan_rect_cancel_flag;
  int pan_scan_cnt_minus1, i;
  int pan_scan_rect_repetition_period;
  int pan_scan_rect_id, pan_scan_rect_left_offset, pan_scan_rect_right_offset;
  int pan_scan_rect_top_offset, pan_scan_rect_bottom_offset;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  pan_scan_rect_id = read_ue_v("SEI: pan_scan_rect_id", buf, &p_Dec->UsedBits);

  pan_scan_rect_cancel_flag = read_u_1("SEI: pan_scan_rect_cancel_flag", buf, &p_Dec->UsedBits);
  if (!pan_scan_rect_cancel_flag) 
  {
    pan_scan_cnt_minus1 = read_ue_v("SEI: pan_scan_cnt_minus1", buf, &p_Dec->UsedBits);
    for (i = 0; i <= pan_scan_cnt_minus1; i++) 
    {
      pan_scan_rect_left_offset   = read_se_v("SEI: pan_scan_rect_left_offset"  , buf, &p_Dec->UsedBits);
      pan_scan_rect_right_offset  = read_se_v("SEI: pan_scan_rect_right_offset" , buf, &p_Dec->UsedBits);
      pan_scan_rect_top_offset    = read_se_v("SEI: pan_scan_rect_top_offset"   , buf, &p_Dec->UsedBits);
      pan_scan_rect_bottom_offset = read_se_v("SEI: pan_scan_rect_bottom_offset", buf, &p_Dec->UsedBits);
#ifdef PRINT_PAN_SCAN_RECT
      printf("Pan scan rectangle SEI message %d/%d\n", i, pan_scan_cnt_minus1);
      printf("pan_scan_rect_id            = %d\n", pan_scan_rect_id);
      printf("pan_scan_rect_left_offset   = %d\n", pan_scan_rect_left_offset);
      printf("pan_scan_rect_right_offset  = %d\n", pan_scan_rect_right_offset);
      printf("pan_scan_rect_top_offset    = %d\n", pan_scan_rect_top_offset);
      printf("pan_scan_rect_bottom_offset = %d\n", pan_scan_rect_bottom_offset);
#endif
    }
    pan_scan_rect_repetition_period = read_ue_v("SEI: pan_scan_rect_repetition_period", buf, &p_Dec->UsedBits);
  }

  free (buf);
#ifdef PRINT_PAN_SCAN_RECT
#undef PRINT_PAN_SCAN_RECT
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Random access point SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_recovery_point_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int recovery_frame_cnt, exact_match_flag, broken_link_flag, changing_slice_group_idc;


  Bitstream* buf;


  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  recovery_frame_cnt       = read_ue_v(    "SEI: recovery_frame_cnt"      , buf, &p_Dec->UsedBits);
  exact_match_flag         = read_u_1 (    "SEI: exact_match_flag"        , buf, &p_Dec->UsedBits);
  broken_link_flag         = read_u_1 (    "SEI: broken_link_flag"        , buf, &p_Dec->UsedBits);
  changing_slice_group_idc = read_u_v ( 2, "SEI: changing_slice_group_idc", buf, &p_Dec->UsedBits);

  p_Vid->recovery_point = 1;
  p_Vid->recovery_frame_cnt = recovery_frame_cnt;

#ifdef PRINT_RECOVERY_POINT
  printf("Recovery point SEI message\n");
  printf("recovery_frame_cnt       = %d\n", recovery_frame_cnt);
  printf("exact_match_flag         = %d\n", exact_match_flag);
  printf("broken_link_flag         = %d\n", broken_link_flag);
  printf("changing_slice_group_idc = %d\n", changing_slice_group_idc);
#endif
  free (buf);
#ifdef PRINT_RECOVERY_POINT
#undef PRINT_RECOVERY_POINT
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Decoded Picture Buffer Management Repetition SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_dec_ref_pic_marking_repetition_info( byte* payload, int size, VideoParameters *p_Vid, Slice *pSlice )
{
  int original_idr_flag, original_frame_num;
  int original_field_pic_flag, original_bottom_field_flag;

  DecRefPicMarking_t *tmp_drpm;
  DecRefPicMarking_t *old_drpm;
  int old_idr_flag, old_no_output_of_prior_pics_flag, old_long_term_reference_flag , old_adaptive_ref_pic_buffering_flag;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  original_idr_flag     = read_u_1 (    "SEI: original_idr_flag"    , buf, &p_Dec->UsedBits);
  original_frame_num    = read_ue_v(    "SEI: original_frame_num"   , buf, &p_Dec->UsedBits);

  if ( !p_Vid->active_sps->frame_mbs_only_flag )
  {
    original_field_pic_flag = read_u_1 ( "SEI: original_field_pic_flag", buf, &p_Dec->UsedBits);
    if ( original_field_pic_flag )
    {
      original_bottom_field_flag = read_u_1 ( "SEI: original_bottom_field_flag", buf, &p_Dec->UsedBits);
    }
  }

#ifdef PRINT_DEC_REF_PIC_MARKING
  printf("Decoded Picture Buffer Management Repetition SEI message\n");
  printf("original_idr_flag       = %d\n", original_idr_flag);
  printf("original_frame_num      = %d\n", original_frame_num);
  if ( active_sps->frame_mbs_only_flag )
  {
    printf("original_field_pic_flag = %d\n", original_field_pic_flag);
    if ( original_field_pic_flag )
    {
      printf("original_bottom_field_flag = %d\n", original_bottom_field_flag);
    }
  }
#endif

  // we need to save everything that is probably overwritten in dec_ref_pic_marking()
  old_drpm = pSlice->dec_ref_pic_marking_buffer;
  old_idr_flag = pSlice->idr_flag; //p_Vid->idr_flag;

  old_no_output_of_prior_pics_flag = pSlice->no_output_of_prior_pics_flag; //p_Vid->no_output_of_prior_pics_flag;
  old_long_term_reference_flag = pSlice->long_term_reference_flag;
  old_adaptive_ref_pic_buffering_flag = pSlice->adaptive_ref_pic_buffering_flag;

  // set new initial values
  //p_Vid->idr_flag = original_idr_flag;
  pSlice->idr_flag = original_idr_flag;
  pSlice->dec_ref_pic_marking_buffer = NULL;

  dec_ref_pic_marking(p_Vid, buf, pSlice);

  // print out decoded values
#ifdef PRINT_DEC_REF_PIC_MARKING
  if (p_Vid->idr_flag)
  {
    printf("no_output_of_prior_pics_flag = %d\n", p_Vid->no_output_of_prior_pics_flag);
    printf("long_term_reference_flag     = %d\n", p_Vid->long_term_reference_flag);
  }
  else
  {
    printf("adaptive_ref_pic_buffering_flag  = %d\n", p_Vid->adaptive_ref_pic_buffering_flag);
    if (p_Vid->adaptive_ref_pic_buffering_flag)
    {
      tmp_drpm=p_Vid->dec_ref_pic_marking_buffer;
      while (tmp_drpm != NULL)
      {
        printf("memory_management_control_operation  = %d\n", tmp_drpm->memory_management_control_operation);

        if ((tmp_drpm->memory_management_control_operation==1)||(tmp_drpm->memory_management_control_operation==3))
        {
          printf("difference_of_pic_nums_minus1        = %d\n", tmp_drpm->difference_of_pic_nums_minus1);
        }
        if (tmp_drpm->memory_management_control_operation==2)
        {
          printf("long_term_pic_num                    = %d\n", tmp_drpm->long_term_pic_num);
        }
        if ((tmp_drpm->memory_management_control_operation==3)||(tmp_drpm->memory_management_control_operation==6))
        {
          printf("long_term_frame_idx                  = %d\n", tmp_drpm->long_term_frame_idx);
        }
        if (tmp_drpm->memory_management_control_operation==4)
        {
          printf("max_long_term_pic_idx_plus1          = %d\n", tmp_drpm->max_long_term_frame_idx_plus1);
        }
        tmp_drpm = tmp_drpm->Next;
      }
    }
  }
#endif

  while (pSlice->dec_ref_pic_marking_buffer)
  {
    tmp_drpm=pSlice->dec_ref_pic_marking_buffer;

    pSlice->dec_ref_pic_marking_buffer=tmp_drpm->Next;
    free (tmp_drpm);
  }

  // restore old values in p_Vid
  pSlice->dec_ref_pic_marking_buffer = old_drpm;
  pSlice->idr_flag = old_idr_flag;
  pSlice->no_output_of_prior_pics_flag = old_no_output_of_prior_pics_flag;
  p_Vid->no_output_of_prior_pics_flag = pSlice->no_output_of_prior_pics_flag;
  pSlice->long_term_reference_flag = old_long_term_reference_flag;
  pSlice->adaptive_ref_pic_buffering_flag = old_adaptive_ref_pic_buffering_flag;

  free (buf);
#ifdef PRINT_DEC_REF_PIC_MARKING
#undef PRINT_DEC_REF_PIC_MARKING
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the Full-frame freeze SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_full_frame_freeze_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int full_frame_freeze_repetition_period;
  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  full_frame_freeze_repetition_period  = read_ue_v(    "SEI: full_frame_freeze_repetition_period"   , buf, &p_Dec->UsedBits);

#ifdef PRINT_FULL_FRAME_FREEZE_INFO
  printf("full_frame_freeze_repetition_period = %d\n", full_frame_freeze_repetition_period);
#endif

  free (buf);
#ifdef PRINT_FULL_FRAME_FREEZE_INFO
#undef PRINT_FULL_FRAME_FREEZE_INFO
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Full-frame freeze release SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_full_frame_freeze_release_info( byte* payload, int size, VideoParameters *p_Vid )
{
#ifdef PRINT_FULL_FRAME_FREEZE_RELEASE_INFO
  printf("Full-frame freeze release SEI message\n");
  if (size)
  {
    printf("payload size of this message should be zero, but is %d bytes.\n", size);
  }
#endif

#ifdef PRINT_FULL_FRAME_FREEZE_RELEASE_INFO
#undef PRINT_FULL_FRAME_FREEZE_RELEASE_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the Full-frame snapshot SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_full_frame_snapshot_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int snapshot_id;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  snapshot_id = read_ue_v("SEI: snapshot_id", buf, &p_Dec->UsedBits);

#ifdef PRINT_FULL_FRAME_SNAPSHOT_INFO
  printf("Full-frame snapshot SEI message\n");
  printf("snapshot_id = %d\n", snapshot_id);
#endif
  free (buf);
#ifdef PRINT_FULL_FRAME_SNAPSHOT_INFO
#undef PRINT_FULL_FRAME_SNAPSHOT_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the Progressive refinement segment start SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_progressive_refinement_start_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int progressive_refinement_id, num_refinement_steps_minus1;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  progressive_refinement_id   = read_ue_v("SEI: progressive_refinement_id"  , buf, &p_Dec->UsedBits);
  num_refinement_steps_minus1 = read_ue_v("SEI: num_refinement_steps_minus1", buf, &p_Dec->UsedBits);

#ifdef PRINT_PROGRESSIVE_REFINEMENT_START_INFO
  printf("Progressive refinement segment start SEI message\n");
  printf("progressive_refinement_id   = %d\n", progressive_refinement_id);
  printf("num_refinement_steps_minus1 = %d\n", num_refinement_steps_minus1);
#endif
  free (buf);
#ifdef PRINT_PROGRESSIVE_REFINEMENT_START_INFO
#undef PRINT_PROGRESSIVE_REFINEMENT_START_INFO
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Progressive refinement segment end SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_progressive_refinement_end_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int progressive_refinement_id;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  progressive_refinement_id   = read_ue_v("SEI: progressive_refinement_id"  , buf, &p_Dec->UsedBits);

#ifdef PRINT_PROGRESSIVE_REFINEMENT_END_INFO
  printf("Progressive refinement segment end SEI message\n");
  printf("progressive_refinement_id   = %d\n", progressive_refinement_id);
#endif
  free (buf);
#ifdef PRINT_PROGRESSIVE_REFINEMENT_END_INFO
#undef PRINT_PROGRESSIVE_REFINEMENT_END_INFO
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Motion-constrained slice group set SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_motion_constrained_slice_group_set_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int num_slice_groups_minus1, slice_group_id, exact_match_flag, pan_scan_rect_flag, pan_scan_rect_id;
  int i;
  int sliceGroupSize;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  num_slice_groups_minus1   = read_ue_v("SEI: num_slice_groups_minus1"  , buf, &p_Dec->UsedBits);
  sliceGroupSize = CeilLog2( num_slice_groups_minus1 + 1 );
#ifdef PRINT_MOTION_CONST_SLICE_GROUP_SET_INFO
  printf("Motion-constrained slice group set SEI message\n");
  printf("num_slice_groups_minus1   = %d\n", num_slice_groups_minus1);
#endif

  for (i=0; i<=num_slice_groups_minus1;i++)
  {

    slice_group_id   = read_u_v (sliceGroupSize, "SEI: slice_group_id" , buf, &p_Dec->UsedBits);
#ifdef PRINT_MOTION_CONST_SLICE_GROUP_SET_INFO
    printf("slice_group_id            = %d\n", slice_group_id);
#endif
  }

  exact_match_flag   = read_u_1("SEI: exact_match_flag"  , buf, &p_Dec->UsedBits);
  pan_scan_rect_flag = read_u_1("SEI: pan_scan_rect_flag"  , buf, &p_Dec->UsedBits);

#ifdef PRINT_MOTION_CONST_SLICE_GROUP_SET_INFO
  printf("exact_match_flag         = %d\n", exact_match_flag);
  printf("pan_scan_rect_flag       = %d\n", pan_scan_rect_flag);
#endif

  if (pan_scan_rect_flag)
  {
    pan_scan_rect_id = read_ue_v("SEI: pan_scan_rect_id"  , buf, &p_Dec->UsedBits);
#ifdef PRINT_MOTION_CONST_SLICE_GROUP_SET_INFO
    printf("pan_scan_rect_id         = %d\n", pan_scan_rect_id);
#endif
  }

  free (buf);
#ifdef PRINT_MOTION_CONST_SLICE_GROUP_SET_INFO
#undef PRINT_MOTION_CONST_SLICE_GROUP_SET_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the film grain characteristics SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_film_grain_characteristics_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int film_grain_characteristics_cancel_flag;
  int model_id, separate_colour_description_present_flag;
  int film_grain_bit_depth_luma_minus8, film_grain_bit_depth_chroma_minus8, film_grain_full_range_flag, film_grain_colour_primaries, film_grain_transfer_characteristics, film_grain_matrix_coefficients;
  int blending_mode_id, log2_scale_factor, comp_model_present_flag[3];
  int num_intensity_intervals_minus1, num_model_values_minus1;
  int intensity_interval_lower_bound, intensity_interval_upper_bound;
  int comp_model_value;
  int film_grain_characteristics_repetition_period;

  int c, i, j;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  film_grain_characteristics_cancel_flag = read_u_1("SEI: film_grain_characteristics_cancel_flag", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
  printf("film_grain_characteristics_cancel_flag = %d\n", film_grain_characteristics_cancel_flag);
#endif
  if(!film_grain_characteristics_cancel_flag)
  {

    model_id                                    = read_u_v(2, "SEI: model_id", buf, &p_Dec->UsedBits);
    separate_colour_description_present_flag    = read_u_1("SEI: separate_colour_description_present_flag", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
    printf("model_id = %d\n", model_id);
    printf("separate_colour_description_present_flag = %d\n", separate_colour_description_present_flag);
#endif
    if (separate_colour_description_present_flag)
    {
      film_grain_bit_depth_luma_minus8          = read_u_v(3, "SEI: film_grain_bit_depth_luma_minus8", buf, &p_Dec->UsedBits);
      film_grain_bit_depth_chroma_minus8        = read_u_v(3, "SEI: film_grain_bit_depth_chroma_minus8", buf, &p_Dec->UsedBits);
      film_grain_full_range_flag                = read_u_v(1, "SEI: film_grain_full_range_flag", buf, &p_Dec->UsedBits);
      film_grain_colour_primaries               = read_u_v(8, "SEI: film_grain_colour_primaries", buf, &p_Dec->UsedBits);
      film_grain_transfer_characteristics       = read_u_v(8, "SEI: film_grain_transfer_characteristics", buf, &p_Dec->UsedBits);
      film_grain_matrix_coefficients            = read_u_v(8, "SEI: film_grain_matrix_coefficients", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
      printf("film_grain_bit_depth_luma_minus8 = %d\n", film_grain_bit_depth_luma_minus8);
      printf("film_grain_bit_depth_chroma_minus8 = %d\n", film_grain_bit_depth_chroma_minus8);
      printf("film_grain_full_range_flag = %d\n", film_grain_full_range_flag);
      printf("film_grain_colour_primaries = %d\n", film_grain_colour_primaries);
      printf("film_grain_transfer_characteristics = %d\n", film_grain_transfer_characteristics);
      printf("film_grain_matrix_coefficients = %d\n", film_grain_matrix_coefficients);
#endif
    }
    blending_mode_id                            = read_u_v(2, "SEI: blending_mode_id", buf, &p_Dec->UsedBits);
    log2_scale_factor                           = read_u_v(4, "SEI: log2_scale_factor", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
    printf("blending_mode_id = %d\n", blending_mode_id);
    printf("log2_scale_factor = %d\n", log2_scale_factor);
#endif
    for (c = 0; c < 3; c ++)
    {
      comp_model_present_flag[c]                = read_u_1("SEI: comp_model_present_flag", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
      printf("comp_model_present_flag = %d\n", comp_model_present_flag[c]);
#endif
    }
    for (c = 0; c < 3; c ++)
      if (comp_model_present_flag[c])
      {
        num_intensity_intervals_minus1          = read_u_v(8, "SEI: num_intensity_intervals_minus1", buf, &p_Dec->UsedBits);
        num_model_values_minus1                 = read_u_v(3, "SEI: num_model_values_minus1", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
        printf("num_intensity_intervals_minus1 = %d\n", num_intensity_intervals_minus1);
        printf("num_model_values_minus1 = %d\n", num_model_values_minus1);
#endif
        for (i = 0; i <= num_intensity_intervals_minus1; i ++)
        {
          intensity_interval_lower_bound        = read_u_v(8, "SEI: intensity_interval_lower_bound", buf, &p_Dec->UsedBits);
          intensity_interval_upper_bound        = read_u_v(8, "SEI: intensity_interval_upper_bound", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
          printf("intensity_interval_lower_bound = %d\n", intensity_interval_lower_bound);
          printf("intensity_interval_upper_bound = %d\n", intensity_interval_upper_bound);
#endif
          for (j = 0; j <= num_model_values_minus1; j++)
          {
            comp_model_value                    = read_se_v("SEI: comp_model_value", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
            printf("comp_model_value = %d\n", comp_model_value);
#endif
          }
        }
      }
    film_grain_characteristics_repetition_period = read_ue_v("SEI: film_grain_characteristics_repetition_period", buf, &p_Dec->UsedBits);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
    printf("film_grain_characteristics_repetition_period = %d\n", film_grain_characteristics_repetition_period);
#endif
  }

  free (buf);
#ifdef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
#undef PRINT_FILM_GRAIN_CHARACTERISTICS_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the deblocking filter display preference SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_deblocking_filter_display_preference_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int deblocking_display_preference_cancel_flag;
  int display_prior_to_deblocking_preferred_flag, dec_frame_buffering_constraint_flag, deblocking_display_preference_repetition_period;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  deblocking_display_preference_cancel_flag             = read_u_1("SEI: deblocking_display_preference_cancel_flag", buf, &p_Dec->UsedBits);
#ifdef PRINT_DEBLOCKING_FILTER_DISPLAY_PREFERENCE_INFO
  printf("deblocking_display_preference_cancel_flag = %d\n", deblocking_display_preference_cancel_flag);
#endif
  if(!deblocking_display_preference_cancel_flag)
  {
    display_prior_to_deblocking_preferred_flag            = read_u_1("SEI: display_prior_to_deblocking_preferred_flag", buf, &p_Dec->UsedBits);
    dec_frame_buffering_constraint_flag                   = read_u_1("SEI: dec_frame_buffering_constraint_flag", buf, &p_Dec->UsedBits);
    deblocking_display_preference_repetition_period       = read_ue_v("SEI: deblocking_display_preference_repetition_period", buf, &p_Dec->UsedBits);
#ifdef PRINT_DEBLOCKING_FILTER_DISPLAY_PREFERENCE_INFO
    printf("display_prior_to_deblocking_preferred_flag = %d\n", display_prior_to_deblocking_preferred_flag);
    printf("dec_frame_buffering_constraint_flag = %d\n", dec_frame_buffering_constraint_flag);
    printf("deblocking_display_preference_repetition_period = %d\n", deblocking_display_preference_repetition_period);
#endif
  }

  free (buf);
#ifdef PRINT_DEBLOCKING_FILTER_DISPLAY_PREFERENCE_INFO
#undef PRINT_DEBLOCKING_FILTER_DISPLAY_PREFERENCE_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the stereo video info SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_stereo_video_info_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int field_views_flags;
  int top_field_is_left_view_flag, current_frame_is_left_view_flag, next_frame_is_second_view_flag;
  int left_view_self_contained_flag;
  int right_view_self_contained_flag;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  field_views_flags = read_u_1("SEI: field_views_flags", buf, &p_Dec->UsedBits);
#ifdef PRINT_STEREO_VIDEO_INFO_INFO
  printf("field_views_flags = %d\n", field_views_flags);
#endif
  if (field_views_flags)
  {
    top_field_is_left_view_flag         = read_u_1("SEI: top_field_is_left_view_flag", buf, &p_Dec->UsedBits);
#ifdef PRINT_STEREO_VIDEO_INFO_INFO
    printf("top_field_is_left_view_flag = %d\n", top_field_is_left_view_flag);
#endif
  }
  else
  {
    current_frame_is_left_view_flag     = read_u_1("SEI: current_frame_is_left_view_flag", buf, &p_Dec->UsedBits);
    next_frame_is_second_view_flag      = read_u_1("SEI: next_frame_is_second_view_flag", buf, &p_Dec->UsedBits);
#ifdef PRINT_STEREO_VIDEO_INFO_INFO
    printf("current_frame_is_left_view_flag = %d\n", current_frame_is_left_view_flag);
    printf("next_frame_is_second_view_flag = %d\n", next_frame_is_second_view_flag);
#endif
  }

  left_view_self_contained_flag         = read_u_1("SEI: left_view_self_contained_flag", buf, &p_Dec->UsedBits);
  right_view_self_contained_flag        = read_u_1("SEI: right_view_self_contained_flag", buf, &p_Dec->UsedBits);
#ifdef PRINT_STEREO_VIDEO_INFO_INFO
  printf("left_view_self_contained_flag = %d\n", left_view_self_contained_flag);
  printf("right_view_self_contained_flag = %d\n", right_view_self_contained_flag);
#endif

  free (buf);
#ifdef PRINT_STEREO_VIDEO_INFO_INFO
#undef PRINT_STEREO_VIDEO_INFO_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the Reserved SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_reserved_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int offset = 0;
  byte payload_byte;

#ifdef PRINT_RESERVED_INFO
  printf("Reserved SEI message\n");
#endif

  while (offset < size)
  {
    payload_byte = payload[offset];
    offset ++;
#ifdef PRINT_RESERVED_INFO
    printf("reserved_sei_message_payload_byte = %d\n", payload_byte);
#endif
  }
#ifdef PRINT_RESERVED_INFO
#undef PRINT_RESERVED_INFO
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Buffering period SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_buffering_period_info( byte* payload, int size, VideoParameters *p_Vid )
{
  int seq_parameter_set_id, initial_cpb_removal_delay, initial_cpb_removal_delay_offset;
  unsigned int k;

  Bitstream* buf;
  seq_parameter_set_rbsp_t *sps;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  seq_parameter_set_id   = read_ue_v("SEI: seq_parameter_set_id"  , buf, &p_Dec->UsedBits);

  sps = &p_Vid->SeqParSet[seq_parameter_set_id];

  activate_sps(p_Vid, sps);

#ifdef PRINT_BUFFERING_PERIOD_INFO
  printf("Buffering period SEI message\n");
  printf("seq_parameter_set_id   = %d\n", seq_parameter_set_id);
#endif

  // Note: NalHrdBpPresentFlag and CpbDpbDelaysPresentFlag can also be set "by some means not specified in this Recommendation | International Standard"
  if (sps->vui_parameters_present_flag)
  {

    if (sps->vui_seq_parameters.nal_hrd_parameters_present_flag)
    {
      for (k=0; k<sps->vui_seq_parameters.nal_hrd_parameters.cpb_cnt_minus1+1; k++)
      {
        initial_cpb_removal_delay        = read_u_v(sps->vui_seq_parameters.nal_hrd_parameters.initial_cpb_removal_delay_length_minus1+1, "SEI: initial_cpb_removal_delay"        , buf, &p_Dec->UsedBits);
        initial_cpb_removal_delay_offset = read_u_v(sps->vui_seq_parameters.nal_hrd_parameters.initial_cpb_removal_delay_length_minus1+1, "SEI: initial_cpb_removal_delay_offset" , buf, &p_Dec->UsedBits);

#ifdef PRINT_BUFFERING_PERIOD_INFO
        printf("nal initial_cpb_removal_delay[%d]        = %d\n", k, initial_cpb_removal_delay);
        printf("nal initial_cpb_removal_delay_offset[%d] = %d\n", k, initial_cpb_removal_delay_offset);
#endif
      }
    }

    if (sps->vui_seq_parameters.vcl_hrd_parameters_present_flag)
    {
      for (k=0; k<sps->vui_seq_parameters.vcl_hrd_parameters.cpb_cnt_minus1+1; k++)
      {
        initial_cpb_removal_delay        = read_u_v(sps->vui_seq_parameters.vcl_hrd_parameters.initial_cpb_removal_delay_length_minus1+1, "SEI: initial_cpb_removal_delay"        , buf, &p_Dec->UsedBits);
        initial_cpb_removal_delay_offset = read_u_v(sps->vui_seq_parameters.vcl_hrd_parameters.initial_cpb_removal_delay_length_minus1+1, "SEI: initial_cpb_removal_delay_offset" , buf, &p_Dec->UsedBits);

#ifdef PRINT_BUFFERING_PERIOD_INFO
        printf("vcl initial_cpb_removal_delay[%d]        = %d\n", k, initial_cpb_removal_delay);
        printf("vcl initial_cpb_removal_delay_offset[%d] = %d\n", k, initial_cpb_removal_delay_offset);
#endif
      }
    }
  }

  free (buf);
#ifdef PRINT_BUFFERING_PERIOD_INFO
#undef PRINT_BUFFERING_PERIOD_INFO
#endif
}


/*!
 ************************************************************************
 *  \brief
 *     Interpret the Picture timing SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
void interpret_picture_timing_info( byte* payload, int size, VideoParameters *p_Vid )
{
  seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;

  int cpb_removal_delay, dpb_output_delay, pic_struct_present_flag, pic_struct;
  int clock_timestamp_flag;
  int ct_type, nuit_field_based_flag, counting_type, full_timestamp_flag, discontinuity_flag, cnt_dropped_flag, nframes;
  int seconds_value, minutes_value, hours_value, seconds_flag, minutes_flag, hours_flag, time_offset;
  int NumClockTs = 0;
  int i;

  int cpb_removal_len = 24;
  int dpb_output_len  = 24;

  Boolean CpbDpbDelaysPresentFlag;

  Bitstream* buf;

  if (NULL==active_sps)
  {
    fprintf (stderr, "Warning: no active SPS, timing SEI cannot be parsed\n");
    return;
  }

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;


#ifdef PRINT_PICTURE_TIMING_INFO
  printf("Picture timing SEI message\n");
#endif

  // CpbDpbDelaysPresentFlag can also be set "by some means not specified in this Recommendation | International Standard"
  CpbDpbDelaysPresentFlag =  (Boolean) (active_sps->vui_parameters_present_flag
                              && (   (active_sps->vui_seq_parameters.nal_hrd_parameters_present_flag != 0)
                                   ||(active_sps->vui_seq_parameters.vcl_hrd_parameters_present_flag != 0)));

  if (CpbDpbDelaysPresentFlag )
  {
    if (active_sps->vui_parameters_present_flag)
    {
      if (active_sps->vui_seq_parameters.nal_hrd_parameters_present_flag)
      {
        cpb_removal_len = active_sps->vui_seq_parameters.nal_hrd_parameters.cpb_removal_delay_length_minus1 + 1;
        dpb_output_len  = active_sps->vui_seq_parameters.nal_hrd_parameters.dpb_output_delay_length_minus1  + 1;
      }
      else if (active_sps->vui_seq_parameters.vcl_hrd_parameters_present_flag)
      {
        cpb_removal_len = active_sps->vui_seq_parameters.vcl_hrd_parameters.cpb_removal_delay_length_minus1 + 1;
        dpb_output_len  = active_sps->vui_seq_parameters.vcl_hrd_parameters.dpb_output_delay_length_minus1  + 1;
      }
    }

    if ((active_sps->vui_seq_parameters.nal_hrd_parameters_present_flag)||
      (active_sps->vui_seq_parameters.vcl_hrd_parameters_present_flag))
    {
      cpb_removal_delay = read_u_v(cpb_removal_len, "SEI: cpb_removal_delay" , buf, &p_Dec->UsedBits);
      dpb_output_delay  = read_u_v(dpb_output_len,  "SEI: dpb_output_delay"  , buf, &p_Dec->UsedBits);
#ifdef PRINT_PICTURE_TIMING_INFO
      printf("cpb_removal_delay = %d\n",cpb_removal_delay);
      printf("dpb_output_delay  = %d\n",dpb_output_delay);
#endif
    }
  }

  if (!active_sps->vui_parameters_present_flag)
  {
    pic_struct_present_flag = 0;
  }
  else
  {
    pic_struct_present_flag  =  active_sps->vui_seq_parameters.pic_struct_present_flag;
  }

  if (pic_struct_present_flag)
  {
    pic_struct = read_u_v(4, "SEI: pic_struct" , buf, &p_Dec->UsedBits);
#ifdef PRINT_PICTURE_TIMING_INFO
    printf("pic_struct = %d\n",pic_struct);
#endif
    switch (pic_struct)
    {
    case 0:
    case 1:
    case 2:
      NumClockTs = 1;
      break;
    case 3:
    case 4:
    case 7:
      NumClockTs = 2;
      break;
    case 5:
    case 6:
    case 8:
      NumClockTs = 3;
      break;
    default:
      error("reserved pic_struct used (can't determine NumClockTs)", 500);
    }
    for (i=0; i<NumClockTs; i++)
    {
      clock_timestamp_flag = read_u_1("SEI: clock_timestamp_flag"  , buf, &p_Dec->UsedBits);
#ifdef PRINT_PICTURE_TIMING_INFO
      printf("clock_timestamp_flag = %d\n",clock_timestamp_flag);
#endif
      if (clock_timestamp_flag)
      {
        ct_type               = read_u_v(2, "SEI: ct_type"               , buf, &p_Dec->UsedBits);
        nuit_field_based_flag = read_u_1(   "SEI: nuit_field_based_flag" , buf, &p_Dec->UsedBits);
        counting_type         = read_u_v(5, "SEI: counting_type"         , buf, &p_Dec->UsedBits);
        full_timestamp_flag   = read_u_1(   "SEI: full_timestamp_flag"   , buf, &p_Dec->UsedBits);
        discontinuity_flag    = read_u_1(   "SEI: discontinuity_flag"    , buf, &p_Dec->UsedBits);
        cnt_dropped_flag      = read_u_1(   "SEI: cnt_dropped_flag"      , buf, &p_Dec->UsedBits);
        nframes               = read_u_v(8, "SEI: nframes"               , buf, &p_Dec->UsedBits);

#ifdef PRINT_PICTURE_TIMING_INFO
        printf("ct_type               = %d\n",ct_type);
        printf("nuit_field_based_flag = %d\n",nuit_field_based_flag);
        printf("full_timestamp_flag   = %d\n",full_timestamp_flag);
        printf("discontinuity_flag    = %d\n",discontinuity_flag);
        printf("cnt_dropped_flag      = %d\n",cnt_dropped_flag);
        printf("nframes               = %d\n",nframes);
#endif
        if (full_timestamp_flag)
        {
          seconds_value         = read_u_v(6, "SEI: seconds_value"   , buf, &p_Dec->UsedBits);
          minutes_value         = read_u_v(6, "SEI: minutes_value"   , buf, &p_Dec->UsedBits);
          hours_value           = read_u_v(5, "SEI: hours_value"     , buf, &p_Dec->UsedBits);
#ifdef PRINT_PICTURE_TIMING_INFO
          printf("seconds_value = %d\n",seconds_value);
          printf("minutes_value = %d\n",minutes_value);
          printf("hours_value   = %d\n",hours_value);
#endif
        }
        else
        {
          seconds_flag          = read_u_1(   "SEI: seconds_flag" , buf, &p_Dec->UsedBits);
#ifdef PRINT_PICTURE_TIMING_INFO
          printf("seconds_flag = %d\n",seconds_flag);
#endif
          if (seconds_flag)
          {
            seconds_value         = read_u_v(6, "SEI: seconds_value"   , buf, &p_Dec->UsedBits);
            minutes_flag          = read_u_1(   "SEI: minutes_flag" , buf, &p_Dec->UsedBits);
#ifdef PRINT_PICTURE_TIMING_INFO
            printf("seconds_value = %d\n",seconds_value);
            printf("minutes_flag  = %d\n",minutes_flag);
#endif
            if(minutes_flag)
            {
              minutes_value         = read_u_v(6, "SEI: minutes_value"   , buf, &p_Dec->UsedBits);
              hours_flag            = read_u_1(   "SEI: hours_flag" , buf, &p_Dec->UsedBits);
#ifdef PRINT_PICTURE_TIMING_INFO
              printf("minutes_value = %d\n",minutes_value);
              printf("hours_flag    = %d\n",hours_flag);
#endif
              if(hours_flag)
              {
                hours_value           = read_u_v(5, "SEI: hours_value"     , buf, &p_Dec->UsedBits);
#ifdef PRINT_PICTURE_TIMING_INFO
                printf("hours_value   = %d\n",hours_value);
#endif
              }
            }
          }
        }
        {
          int time_offset_length;
          if (active_sps->vui_seq_parameters.vcl_hrd_parameters_present_flag)
            time_offset_length = active_sps->vui_seq_parameters.vcl_hrd_parameters.time_offset_length;
          else if (active_sps->vui_seq_parameters.nal_hrd_parameters_present_flag)
            time_offset_length = active_sps->vui_seq_parameters.nal_hrd_parameters.time_offset_length;
          else
            time_offset_length = 24;
          if (time_offset_length)
            time_offset = read_i_v(time_offset_length, "SEI: time_offset"   , buf, &p_Dec->UsedBits);
          else
            time_offset = 0;
#ifdef PRINT_PICTURE_TIMING_INFO
          printf("time_offset   = %d\n",time_offset);
#endif
        }
      }
    }
  }

  free (buf);
#ifdef PRINT_PICTURE_TIMING_INFO
#undef PRINT_PICTURE_TIMING_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the Frame Packing Arrangement SEI message
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 ************************************************************************
 */
void interpret_frame_packing_arrangement_info( byte* payload, int size, VideoParameters *p_Vid )
{
  frame_packing_arrangement_information_struct seiFramePackingArrangement;
  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

#ifdef PRINT_FRAME_PACKING_ARRANGEMENT_INFO
  printf("Frame packing arrangement SEI message\n");
#endif

  seiFramePackingArrangement.frame_packing_arrangement_id = (unsigned int)read_ue_v( "SEI: frame_packing_arrangement_id", buf, &p_Dec->UsedBits );
  seiFramePackingArrangement.frame_packing_arrangement_cancel_flag = read_u_1( "SEI: frame_packing_arrangement_cancel_flag", buf, &p_Dec->UsedBits );
#ifdef PRINT_FRAME_PACKING_ARRANGEMENT_INFO
  printf("frame_packing_arrangement_id                 = %d\n", seiFramePackingArrangement.frame_packing_arrangement_id);
  printf("frame_packing_arrangement_cancel_flag        = %d\n", seiFramePackingArrangement.frame_packing_arrangement_cancel_flag);
#endif
  if ( seiFramePackingArrangement.frame_packing_arrangement_cancel_flag == FALSE )
  {
    seiFramePackingArrangement.frame_packing_arrangement_type = (unsigned char)read_u_v( 7, "SEI: frame_packing_arrangement_type", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.quincunx_sampling_flag         = read_u_1( "SEI: quincunx_sampling_flag", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.content_interpretation_type    = (unsigned char)read_u_v( 6, "SEI: content_interpretation_type", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.spatial_flipping_flag          = read_u_1( "SEI: spatial_flipping_flag", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.frame0_flipped_flag            = read_u_1( "SEI: frame0_flipped_flag", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.field_views_flag               = read_u_1( "SEI: field_views_flag", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.current_frame_is_frame0_flag   = read_u_1( "SEI: current_frame_is_frame0_flag", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.frame0_self_contained_flag     = read_u_1( "SEI: frame0_self_contained_flag", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.frame1_self_contained_flag     = read_u_1( "SEI: frame1_self_contained_flag", buf, &p_Dec->UsedBits );
#ifdef PRINT_FRAME_PACKING_ARRANGEMENT_INFO
    printf("frame_packing_arrangement_type    = %d\n", seiFramePackingArrangement.frame_packing_arrangement_type);
    printf("quincunx_sampling_flag            = %d\n", seiFramePackingArrangement.quincunx_sampling_flag);
    printf("content_interpretation_type       = %d\n", seiFramePackingArrangement.content_interpretation_type);
    printf("spatial_flipping_flag             = %d\n", seiFramePackingArrangement.spatial_flipping_flag);
    printf("frame0_flipped_flag               = %d\n", seiFramePackingArrangement.frame0_flipped_flag);
    printf("field_views_flag                  = %d\n", seiFramePackingArrangement.field_views_flag);
    printf("current_frame_is_frame0_flag      = %d\n", seiFramePackingArrangement.current_frame_is_frame0_flag);
    printf("frame0_self_contained_flag        = %d\n", seiFramePackingArrangement.frame0_self_contained_flag);
    printf("frame1_self_contained_flag        = %d\n", seiFramePackingArrangement.frame1_self_contained_flag);
#endif
    if ( seiFramePackingArrangement.quincunx_sampling_flag == FALSE && seiFramePackingArrangement.frame_packing_arrangement_type != 5 )
    {
      seiFramePackingArrangement.frame0_grid_position_x = (unsigned char)read_u_v( 4, "SEI: frame0_grid_position_x", buf, &p_Dec->UsedBits );
      seiFramePackingArrangement.frame0_grid_position_y = (unsigned char)read_u_v( 4, "SEI: frame0_grid_position_y", buf, &p_Dec->UsedBits );
      seiFramePackingArrangement.frame1_grid_position_x = (unsigned char)read_u_v( 4, "SEI: frame1_grid_position_x", buf, &p_Dec->UsedBits );
      seiFramePackingArrangement.frame1_grid_position_y = (unsigned char)read_u_v( 4, "SEI: frame1_grid_position_y", buf, &p_Dec->UsedBits );
#ifdef PRINT_FRAME_PACKING_ARRANGEMENT_INFO
      printf("frame0_grid_position_x      = %d\n", seiFramePackingArrangement.frame0_grid_position_x);
      printf("frame0_grid_position_y      = %d\n", seiFramePackingArrangement.frame0_grid_position_y);
      printf("frame1_grid_position_x      = %d\n", seiFramePackingArrangement.frame1_grid_position_x);
      printf("frame1_grid_position_y      = %d\n", seiFramePackingArrangement.frame1_grid_position_y);
#endif
    }
    seiFramePackingArrangement.frame_packing_arrangement_reserved_byte = (unsigned char)read_u_v( 8, "SEI: frame_packing_arrangement_reserved_byte", buf, &p_Dec->UsedBits );
    seiFramePackingArrangement.frame_packing_arrangement_repetition_period = (unsigned int)read_ue_v( "SEI: frame_packing_arrangement_repetition_period", buf, &p_Dec->UsedBits );
#ifdef PRINT_FRAME_PACKING_ARRANGEMENT_INFO
    printf("frame_packing_arrangement_reserved_byte          = %d\n", seiFramePackingArrangement.frame_packing_arrangement_reserved_byte);
    printf("frame_packing_arrangement_repetition_period      = %d\n", seiFramePackingArrangement.frame_packing_arrangement_repetition_period);
#endif
  }
  seiFramePackingArrangement.frame_packing_arrangement_extension_flag = read_u_1( "SEI: frame_packing_arrangement_extension_flag", buf, &p_Dec->UsedBits );
#ifdef PRINT_FRAME_PACKING_ARRANGEMENT_INFO
  printf("frame_packing_arrangement_extension_flag          = %d\n", seiFramePackingArrangement.frame_packing_arrangement_extension_flag);
#endif

  free (buf);
#ifdef PRINT_FRAME_PACKING_ARRANGEMENT_INFO
#undef PRINT_FRAME_PACKING_ARRANGEMENT_INFO
#endif
}

/*!
 ************************************************************************
 *  \brief
 *     Interpret the HDR tone-mapping SEI message (JVT-T060)
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *
 ************************************************************************
 */
typedef struct
{
  unsigned int  tone_map_id;
  unsigned char tone_map_cancel_flag;
  unsigned int  tone_map_repetition_period;
  unsigned char coded_data_bit_depth;
  unsigned char sei_bit_depth;
  unsigned int  model_id;
  // variables for model 0
  int  min_value;
  int  max_value;
  // variables for model 1
  int  sigmoid_midpoint;
  int  sigmoid_width;
  // variables for model 2
  int start_of_coded_interval[1<<MAX_SEI_BIT_DEPTH];
  // variables for model 3
  int num_pivots;
  int coded_pivot_value[MAX_NUM_PIVOTS];
  int sei_pivot_value[MAX_NUM_PIVOTS];
} tone_mapping_struct_tmp;

void interpret_tone_mapping( byte* payload, int size, VideoParameters *p_Vid )
{
  tone_mapping_struct_tmp seiToneMappingTmp;
  Bitstream* buf;
  int i = 0, max_coded_num, max_output_num;

  memset (&seiToneMappingTmp, 0, sizeof (tone_mapping_struct_tmp));

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  seiToneMappingTmp.tone_map_id = read_ue_v("SEI: tone_map_id", buf, &p_Dec->UsedBits);
  seiToneMappingTmp.tone_map_cancel_flag = (unsigned char) read_u_1("SEI: tone_map_cancel_flag", buf, &p_Dec->UsedBits);

#ifdef PRINT_TONE_MAPPING
  printf("Tone-mapping SEI message\n");
  printf("tone_map_id = %d\n", seiToneMappingTmp.tone_map_id);

  if (seiToneMappingTmp.tone_map_id != 0)
    printf("WARNING! Tone_map_id != 0, print the SEI message info only. The tone mapping is actually applied only when Tone_map_id==0\n\n");
  printf("tone_map_cancel_flag = %d\n", seiToneMappingTmp.tone_map_cancel_flag);
#endif

  if (!seiToneMappingTmp.tone_map_cancel_flag) 
  {
    seiToneMappingTmp.tone_map_repetition_period  = read_ue_v(  "SEI: tone_map_repetition_period", buf, &p_Dec->UsedBits);
    seiToneMappingTmp.coded_data_bit_depth        = (unsigned char)read_u_v (8,"SEI: coded_data_bit_depth"      , buf, &p_Dec->UsedBits);
    seiToneMappingTmp.sei_bit_depth               = (unsigned char)read_u_v (8,"SEI: sei_bit_depth"             , buf, &p_Dec->UsedBits);
    seiToneMappingTmp.model_id                    = read_ue_v(  "SEI: model_id"                  , buf, &p_Dec->UsedBits);

#ifdef PRINT_TONE_MAPPING
    printf("tone_map_repetition_period = %d\n", seiToneMappingTmp.tone_map_repetition_period);
    printf("coded_data_bit_depth = %d\n", seiToneMappingTmp.coded_data_bit_depth);
    printf("sei_bit_depth = %d\n", seiToneMappingTmp.sei_bit_depth);
    printf("model_id = %d\n", seiToneMappingTmp.model_id);
#endif

    max_coded_num  = 1<<seiToneMappingTmp.coded_data_bit_depth;
    max_output_num = 1<<seiToneMappingTmp.sei_bit_depth;

    if (seiToneMappingTmp.model_id == 0) 
    { // linear mapping with clipping
      seiToneMappingTmp.min_value   = read_u_v (32,"SEI: min_value", buf, &p_Dec->UsedBits);
      seiToneMappingTmp.max_value   = read_u_v (32,"SEI: min_value", buf, &p_Dec->UsedBits);
#ifdef PRINT_TONE_MAPPING
      printf("min_value = %d, max_value = %d\n", seiToneMappingTmp.min_value, seiToneMappingTmp.max_value);
#endif
    }
    else if (seiToneMappingTmp.model_id == 1) 
    { // sigmoidal mapping
      seiToneMappingTmp.sigmoid_midpoint = read_u_v (32,"SEI: sigmoid_midpoint", buf, &p_Dec->UsedBits);
      seiToneMappingTmp.sigmoid_width    = read_u_v (32,"SEI: sigmoid_width", buf, &p_Dec->UsedBits);
#ifdef PRINT_TONE_MAPPING
      printf("sigmoid_midpoint = %d, sigmoid_width = %d\n", seiToneMappingTmp.sigmoid_midpoint, seiToneMappingTmp.sigmoid_width);
#endif
    }
    else if (seiToneMappingTmp.model_id == 2) 
    { // user defined table mapping
      for (i=0; i<max_output_num; i++) 
      {
        seiToneMappingTmp.start_of_coded_interval[i] = read_u_v((((seiToneMappingTmp.coded_data_bit_depth+7)>>3)<<3), "SEI: start_of_coded_interval"  , buf, &p_Dec->UsedBits);
#ifdef PRINT_TONE_MAPPING // too long to print
        //printf("start_of_coded_interval[%d] = %d\n", i, seiToneMappingTmp.start_of_coded_interval[i]);
#endif
      }
    }
    else if (seiToneMappingTmp.model_id == 3) 
    {  // piece-wise linear mapping
      seiToneMappingTmp.num_pivots = read_u_v (16,"SEI: num_pivots", buf, &p_Dec->UsedBits);
#ifdef PRINT_TONE_MAPPING
      printf("num_pivots = %d\n", seiToneMappingTmp.num_pivots);
#endif
      seiToneMappingTmp.coded_pivot_value[0] = 0;
      seiToneMappingTmp.sei_pivot_value[0] = 0;
      seiToneMappingTmp.coded_pivot_value[seiToneMappingTmp.num_pivots+1] = max_coded_num-1;
      seiToneMappingTmp.sei_pivot_value[seiToneMappingTmp.num_pivots+1] = max_output_num-1;

      for (i=1; i < seiToneMappingTmp.num_pivots+1; i++) 
      {
        seiToneMappingTmp.coded_pivot_value[i] = read_u_v( (((seiToneMappingTmp.coded_data_bit_depth+7)>>3)<<3), "SEI: coded_pivot_value", buf, &p_Dec->UsedBits);
        seiToneMappingTmp.sei_pivot_value[i] = read_u_v( (((seiToneMappingTmp.sei_bit_depth+7)>>3)<<3), "SEI: sei_pivot_value", buf, &p_Dec->UsedBits);
#ifdef PRINT_TONE_MAPPING
        printf("coded_pivot_value[%d] = %d, sei_pivot_value[%d] = %d\n", i, seiToneMappingTmp.coded_pivot_value[i], i, seiToneMappingTmp.sei_pivot_value[i]);
#endif
      }
    }

#if (ENABLE_OUTPUT_TONEMAPPING)
    // Currently, only when the map_id == 0, the tone-mapping is actually applied.
    if (seiToneMappingTmp.tone_map_id== 0) 
    {
      int j;
      p_Vid->seiToneMapping->seiHasTone_mapping = TRUE;
      p_Vid->seiToneMapping->tone_map_repetition_period = seiToneMappingTmp.tone_map_repetition_period;
      p_Vid->seiToneMapping->coded_data_bit_depth = seiToneMappingTmp.coded_data_bit_depth;
      p_Vid->seiToneMapping->sei_bit_depth = seiToneMappingTmp.sei_bit_depth;
      p_Vid->seiToneMapping->model_id = seiToneMappingTmp.model_id;
      p_Vid->seiToneMapping->count = 0;

      // generate the look up table of tone mapping
      switch(seiToneMappingTmp.model_id)
      {
      case 0:            // linear mapping with clipping
        for (i=0; i<=seiToneMappingTmp.min_value; i++)
          p_Vid->seiToneMapping->lut[i] = 0;

        for (i=seiToneMappingTmp.min_value+1; i < seiToneMappingTmp.max_value; i++)
          p_Vid->seiToneMapping->lut[i] = (imgpel) ((i-seiToneMappingTmp.min_value) * (max_output_num-1)/(seiToneMappingTmp.max_value- seiToneMappingTmp.min_value));

        for (i=seiToneMappingTmp.max_value; i<max_coded_num; i++)
          p_Vid->seiToneMapping->lut[i] = (imgpel) (max_output_num - 1);
        break;
      case 1: // sigmoid mapping

        for (i=0; i < max_coded_num; i++) 
        {
          double tmp = 1.0 + exp( -6*(double)(i-seiToneMappingTmp.sigmoid_midpoint)/seiToneMappingTmp.sigmoid_width);
          p_Vid->seiToneMapping->lut[i] = (imgpel)( (double)(max_output_num-1)/ tmp + 0.5);
        }
        break;
      case 2: // user defined table
        if (0 < max_output_num-1)
        {
          for (j=0; j<max_output_num-1; j++) 
          {
            for (i=seiToneMappingTmp.start_of_coded_interval[j]; i<seiToneMappingTmp.start_of_coded_interval[j+1]; i++) 
            {
              p_Vid->seiToneMapping->lut[i] = (imgpel) j;
            }
          }
          p_Vid->seiToneMapping->lut[i] = (imgpel) (max_output_num - 1);
        }
        break;
      case 3: // piecewise linear mapping
        for (j=0; j<seiToneMappingTmp.num_pivots+1; j++) 
        {
          double slope = (double)(seiToneMappingTmp.sei_pivot_value[j+1] - seiToneMappingTmp.sei_pivot_value[j])/(seiToneMappingTmp.coded_pivot_value[j+1]-seiToneMappingTmp.coded_pivot_value[j]);
          for (i=seiToneMappingTmp.coded_pivot_value[j]; i <= seiToneMappingTmp.coded_pivot_value[j+1]; i++) 
          {
            p_Vid->seiToneMapping->lut[i] = (imgpel) (seiToneMappingTmp.sei_pivot_value[j] + (int)(( (i - seiToneMappingTmp.coded_pivot_value[j]) * slope)));
          }
        }
        break;

      default:
        break;
      } // end switch
    }
#endif
  } // end !tone_map_cancel_flag
  free (buf);
}

#if (ENABLE_OUTPUT_TONEMAPPING)
// tone map using the look-up-table generated according to SEI tone mapping message
void tone_map (imgpel** imgX, imgpel* lut, int size_x, int size_y)
{
  int i, j;

  for(i=0;i<size_y;i++)
  {
    for(j=0;j<size_x;j++)
    {
      imgX[i][j] = (imgpel)lut[imgX[i][j]];
    }
  }
}

void init_tone_mapping_sei(ToneMappingSEI *seiToneMapping) 
{
  seiToneMapping->seiHasTone_mapping = FALSE;
  seiToneMapping->count = 0;
}

void update_tone_mapping_sei(ToneMappingSEI *seiToneMapping) 
{

  if(seiToneMapping->tone_map_repetition_period == 0)
  {
    seiToneMapping->seiHasTone_mapping = FALSE;
    seiToneMapping->count = 0;
  }
  else if (seiToneMapping->tone_map_repetition_period>1)
  {
    seiToneMapping->count++;
    if (seiToneMapping->count>=seiToneMapping->tone_map_repetition_period) 
    {
      seiToneMapping->seiHasTone_mapping = FALSE;
      seiToneMapping->count = 0;
    }
  }
}
#endif

/*!
 ************************************************************************
 *  \brief
 *     Interpret the post filter hints SEI message (JVT-U035)
 *  \param payload
 *     a pointer that point to the sei payload
 *  \param size
 *     the size of the sei message
 *  \param p_Vid
 *     the image pointer
 *    
 ************************************************************************
 */
void interpret_post_filter_hints_info( byte* payload, int size, VideoParameters *p_Vid )
{
  Bitstream* buf;
  unsigned int filter_hint_size_y, filter_hint_size_x, filter_hint_type, color_component, cx, cy, additional_extension_flag;
  int ***filter_hint;

  buf = malloc(sizeof(Bitstream));
  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

  filter_hint_size_y = read_ue_v("SEI: filter_hint_size_y", buf, &p_Dec->UsedBits); // interpret post-filter hint SEI here
  filter_hint_size_x = read_ue_v("SEI: filter_hint_size_x", buf, &p_Dec->UsedBits); // interpret post-filter hint SEI here
  filter_hint_type   = read_u_v(2, "SEI: filter_hint_type", buf, &p_Dec->UsedBits); // interpret post-filter hint SEI here

  get_mem3Dint (&filter_hint, 3, filter_hint_size_y, filter_hint_size_x);

  for (color_component = 0; color_component < 3; color_component ++)
    for (cy = 0; cy < filter_hint_size_y; cy ++)
      for (cx = 0; cx < filter_hint_size_x; cx ++)
        filter_hint[color_component][cy][cx] = read_se_v("SEI: filter_hint", buf, &p_Dec->UsedBits); // interpret post-filter hint SEI here

  additional_extension_flag = read_u_1("SEI: additional_extension_flag", buf, &p_Dec->UsedBits); // interpret post-filter hint SEI here

#ifdef PRINT_POST_FILTER_HINT_INFO
  printf(" Post-filter hint SEI message\n");
  printf(" post_filter_hint_size_y %d \n", filter_hint_size_y);
  printf(" post_filter_hint_size_x %d \n", filter_hint_size_x);
  printf(" post_filter_hint_type %d \n",   filter_hint_type);
  for (color_component = 0; color_component < 3; color_component ++)
    for (cy = 0; cy < filter_hint_size_y; cy ++)
      for (cx = 0; cx < filter_hint_size_x; cx ++)
        printf(" post_filter_hint[%d][%d][%d] %d \n", color_component, cy, cx, filter_hint[color_component][cy][cx]);

  printf(" additional_extension_flag %d \n", additional_extension_flag);

#undef PRINT_POST_FILTER_HINT_INFO
#endif

  free_mem3Dint (filter_hint);
  free( buf );
}


void interpret_green_metadata_info(byte* payload, int size, VideoParameters *p_Vid )
{
  Green_metadata_information_struct seiGreenMetadataInfo;

  Bitstream* buf;

  buf = malloc(sizeof(Bitstream));

  buf->bitstream_length = size;
  buf->streamBuffer = payload;
  buf->frame_bitoffset = 0;

  p_Dec->UsedBits = 0;

#ifdef PRINT_GREEN_METADATA_INFO
  printf("Green Metadata Info SEI message\n");
#endif

  seiGreenMetadataInfo.green_metadata_type=(unsigned char)read_u_v(8, "SEI: green_metadata_type", buf, &p_Dec->UsedBits );
#ifdef PRINT_GREEN_METADATA_INFO
  printf("green_metadata_type                 = %d\n", seiGreenMetadataInfo.green_metadata_type);
#endif
  if ( seiGreenMetadataInfo.green_metadata_type == 0)
  {
      seiGreenMetadataInfo.period_type=(unsigned char)read_u_v(8, "SEI: green_metadata_period_type", buf, &p_Dec->UsedBits );
#ifdef PRINT_GREEN_METADATA_INFO
    printf("green_metadata_period_type     = %d\n", seiGreenMetadataInfo.period_type);
#endif

    if ( seiGreenMetadataInfo.period_type == 2)
    {
      seiGreenMetadataInfo.num_seconds = (unsigned short)read_u_v(16, "SEI: green_metadata_num_seconds", buf, &p_Dec->UsedBits );
#ifdef PRINT_GREEN_METADATA_INFO
      printf("green_metadata_num_seconds      = %d\n", seiGreenMetadataInfo.num_seconds);
#endif
    }
    else if ( seiGreenMetadataInfo.period_type == 3)
    {
      seiGreenMetadataInfo.num_pictures = (unsigned short)read_u_v(16, "SEI: green_metadata_num_pictures", buf, &p_Dec->UsedBits );
  #ifdef PRINT_GREEN_METADATA_INFO
      printf("green_metadata_num_pictures      = %d\n", seiGreenMetadataInfo.num_pictures);
  #endif
    }

    seiGreenMetadataInfo.percent_non_zero_macroblocks=(unsigned char)read_u_v(8, "SEI: percent_non_zero_macroblocks", buf, &p_Dec->UsedBits );
    seiGreenMetadataInfo.percent_intra_coded_macroblocks=(unsigned char)read_u_v(8, "SEI: percent_intra_coded_macroblocks", buf, &p_Dec->UsedBits );
    seiGreenMetadataInfo.percent_six_tap_filtering=(unsigned char)read_u_v(8, "SEI: percent_six_tap_filtering", buf, &p_Dec->UsedBits );
    seiGreenMetadataInfo.percent_alpha_point_deblocking_instance=(unsigned char)read_u_v(8, "SEI: percent_alpha_point_deblocking_instance", buf, &p_Dec->UsedBits );

#ifdef PRINT_GREEN_METADATA_INFO
    printf("percent_non_zero_macroblocks      = %f\n", (float)seiGreenMetadataInfo.percent_non_zero_macroblocks/255);
    printf("percent_intra_coded_macroblocks      = %f\n", (float)seiGreenMetadataInfo.percent_intra_coded_macroblocks/255);
    printf("percent_six_tap_filtering      = %f\n", (float)seiGreenMetadataInfo.percent_six_tap_filtering/255);
    printf("percent_alpha_point_deblocking_instance      = %f\n", (float)seiGreenMetadataInfo.percent_alpha_point_deblocking_instance/255);
#endif

  }
  else if( seiGreenMetadataInfo.green_metadata_type == 1)
  {
    seiGreenMetadataInfo.xsd_metric_type=(unsigned char)read_u_v(8, "SEI: xsd_metric_type", buf, &p_Dec->UsedBits );
    seiGreenMetadataInfo.xsd_metric_value=(unsigned short)read_u_v(16, "SEI: xsd_metric_value", buf, &p_Dec->UsedBits );
#ifdef PRINT_GREEN_METADATA_INFO
    printf("xsd_metric_type      = %d\n", seiGreenMetadataInfo.xsd_metric_type);
    if ( seiGreenMetadataInfo.xsd_metric_type == 0)
        printf("xsd_metric_value      = %f\n", (float)seiGreenMetadataInfo.xsd_metric_value/100);
#endif

  }

  free (buf);
}
