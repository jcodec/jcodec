/*!
 *************************************************************************************
 * \file mb_prediction.c
 *
 * \brief
 *    Macroblock prediction functions
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Alexis Michael Tourapis         <alexismt@ieee.org>
 *************************************************************************************
 */

#include "contributors.h"

#include "block.h"
#include "global.h"
#include "mbuffer.h"
#include "elements.h"
#include "errorconcealment.h"
#include "macroblock.h"
#include "fmo.h"
#include "cabac.h"
#include "vlc.h"
#include "image.h"
#include "mb_access.h"
#include "biaridecod.h"
#include "transform8x8.h"
#include "transform.h"
#include "mc_prediction.h"
#include "quant.h"
#include "intra4x4_pred.h"
#include "intra8x8_pred.h"
#include "intra16x16_pred.h"
#include "mv_prediction.h"
#include "mb_prediction.h"

extern int  get_colocated_info_8x8 (Macroblock *currMB, StorablePicture *list1, int i, int j);
extern int  get_colocated_info_4x4 (Macroblock *currMB, StorablePicture *list1, int i, int j);


int mb_pred_intra4x4(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture)
{
  Slice *currSlice = currMB->p_Slice;
  int yuv = dec_picture->chroma_format_idc - 1;
  int i=0, j=0,k, j4=0,i4=0;  
  int j_pos, i_pos;
  int ioff,joff;
  int block8x8;   // needed for ABT
  currMB->itrans_4x4 = (currMB->is_lossless == FALSE) ? itrans4x4 : Inv_Residual_trans_4x4;    

  for (block8x8 = 0; block8x8 < 4; block8x8++)
  {
    for (k = block8x8 * 4; k < block8x8 * 4 + 4; k ++)
    {
      i =  (decode_block_scan[k] & 3);
      j = ((decode_block_scan[k] >> 2) & 3);

      ioff = (i << 2);
      joff = (j << 2);
      i4   = currMB->block_x + i;
      j4   = currMB->block_y + j;
      j_pos = j4 * BLOCK_SIZE;
      i_pos = i4 * BLOCK_SIZE;

      // PREDICTION
      //===== INTRA PREDICTION =====
      if (currSlice->intra_pred_4x4(currMB, curr_plane, ioff,joff,i4,j4) == SEARCH_SYNC)  /* make 4x4 prediction block mpr from given prediction p_Vid->mb_mode */
        return SEARCH_SYNC;                   /* bit error */
      // =============== 4x4 itrans ================
      // -------------------------------------------
      currMB->itrans_4x4  (currMB, curr_plane, ioff, joff);

      copy_image_data_4x4(&currImg[j_pos], &currSlice->mb_rec[curr_plane][joff], i_pos, ioff);
    }
  }

  // chroma decoding *******************************************************
  if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
  {
    intra_cr_decoding(currMB, yuv);
  }

  if (currMB->cbp != 0)
    currSlice->is_reset_coeff = FALSE;
  return 1;
}


int mb_pred_intra16x16(Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture)
{
  int yuv = dec_picture->chroma_format_idc - 1;

  currMB->p_Slice->intra_pred_16x16(currMB, curr_plane, currMB->i16mode);
  currMB->ipmode_DPCM = (char) currMB->i16mode; //For residual DPCM
  // =============== 4x4 itrans ================
  // -------------------------------------------
  iMBtrans4x4(currMB, curr_plane, 0);

  // chroma decoding *******************************************************
  if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
  {
    intra_cr_decoding(currMB, yuv);
  }

  currMB->p_Slice->is_reset_coeff = FALSE;
  return 1;
}

int mb_pred_intra8x8(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture)
{
  Slice *currSlice = currMB->p_Slice;
  int yuv = dec_picture->chroma_format_idc - 1;

  int block8x8;   // needed for ABT
  currMB->itrans_8x8 = (currMB->is_lossless == FALSE) ? itrans8x8 : Inv_Residual_trans_8x8;

  for (block8x8 = 0; block8x8 < 4; block8x8++)
  {
    //=========== 8x8 BLOCK TYPE ============
    int ioff = (block8x8 & 0x01) << 3;
    int joff = (block8x8 >> 1  ) << 3;

    //PREDICTION
    currSlice->intra_pred_8x8(currMB, curr_plane, ioff, joff);
    if (currMB->cbp & (1 << block8x8)) 
      currMB->itrans_8x8    (currMB, curr_plane, ioff,joff);      // use inverse integer transform and make 8x8 block m7 from prediction block mpr
    else
      icopy8x8(currMB, curr_plane, ioff,joff);

    copy_image_data_8x8(&currImg[currMB->pix_y + joff], &currSlice->mb_rec[curr_plane][joff], currMB->pix_x + ioff, ioff);
  }
  // chroma decoding *******************************************************
  if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
  {
    intra_cr_decoding(currMB, yuv);
  }

  if (currMB->cbp != 0)
    currSlice->is_reset_coeff = FALSE;
  return 1;
}


static void set_chroma_vector(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  if (!currSlice->mb_aff_frame_flag)
  {
    if(currSlice->structure == TOP_FIELD)
    {
      int k,l;  
      for (l = LIST_0; l <= (LIST_1); l++)
      {
        for(k = 0; k < currSlice->listXsize[l]; k++)
        {
          if(currSlice->structure != currSlice->listX[l][k]->structure)
            currSlice->chroma_vector_adjustment[l][k] = -2; 
          else
            currSlice->chroma_vector_adjustment[l][k] = 0; 
        }
      }
    }
    else if(currSlice->structure == BOTTOM_FIELD)
    {
      int k,l;  
      for (l = LIST_0; l <= (LIST_1); l++)
      {
        for(k = 0; k < currSlice->listXsize[l]; k++)
        {
          if (currSlice->structure != currSlice->listX[l][k]->structure)
            currSlice->chroma_vector_adjustment[l][k] = 2; 
          else
            currSlice->chroma_vector_adjustment[l][k] = 0; 
        }
      }
    }
    else
    {
      int k,l;  
      for (l = LIST_0; l <= (LIST_1); l++)
      {
        for(k = 0; k < currSlice->listXsize[l]; k++)
        {
          currSlice->chroma_vector_adjustment[l][k] = 0; 
        }
      }
    }
  }
  else
  {
    int mb_nr = (currMB->mbAddrX & 0x01);
    int k,l;  

    //////////////////////////
    // find out the correct list offsets
    if (currMB->mb_field)
    {
      int list_offset = currMB->list_offset;

      for (l = LIST_0 + list_offset; l <= (LIST_1 + list_offset); l++)
      {
        for(k = 0; k < currSlice->listXsize[l]; k++)
        {          
          if(mb_nr == 0 && currSlice->listX[l][k]->structure == BOTTOM_FIELD)
            currSlice->chroma_vector_adjustment[l][k] = -2; 
          else if(mb_nr == 1 && currSlice->listX[l][k]->structure == TOP_FIELD)
            currSlice->chroma_vector_adjustment[l][k] = 2; 
          else
            currSlice->chroma_vector_adjustment[l][k] = 0; 
        }
      }
    }
    else
    {
      for (l = LIST_0; l <= (LIST_1); l++)
      {
        for(k = 0; k < currSlice->listXsize[l]; k++)
        {
          currSlice->chroma_vector_adjustment[l][k] = 0; 
        }
      }
    }
  }

  currSlice->max_mb_vmv_r = (currSlice->structure != FRAME || ( currMB->mb_field )) ? p_Vid->max_vmv_r >> 1 : p_Vid->max_vmv_r;
}

int mb_pred_skip(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  set_chroma_vector(currMB);

  perform_mc(currMB, curr_plane, dec_picture, LIST_0, 0, 0, MB_BLOCK_SIZE, MB_BLOCK_SIZE);

  copy_image_data_16x16(&currImg[currMB->pix_y], currSlice->mb_pred[curr_plane], currMB->pix_x, 0);

  if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
  {

    copy_image_data(&dec_picture->imgUV[0][currMB->pix_c_y], currSlice->mb_pred[1], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
    copy_image_data(&dec_picture->imgUV[1][currMB->pix_c_y], currSlice->mb_pred[2], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
  }
  return 1;
}

int mb_pred_sp_skip(Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture)
{
  set_chroma_vector(currMB);

  perform_mc(currMB, curr_plane, dec_picture, LIST_0, 0, 0, MB_BLOCK_SIZE, MB_BLOCK_SIZE);
  iTransform(currMB, curr_plane, 1);
  return 1;
}

int mb_pred_p_inter8x8(Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture)
{
  int block8x8;   // needed for ABT
  int i=0, j=0,k;  

  Slice *currSlice = currMB->p_Slice;
  int smb = currSlice->slice_type == SP_SLICE && (currMB->is_intra_block == FALSE);

  set_chroma_vector(currMB);

  for (block8x8=0; block8x8<4; block8x8++)
  {
    int mv_mode  = currMB->b8mode[block8x8];
    int pred_dir = currMB->b8pdir[block8x8];

    int k_start = (block8x8 << 2);
    int k_inc = (mv_mode == SMB8x4) ? 2 : 1;
    int k_end = (mv_mode == SMB8x8) ? k_start + 1 : ((mv_mode == SMB4x4) ? k_start + 4 : k_start + k_inc + 1);

    int block_size_x = ( mv_mode == SMB8x4 || mv_mode == SMB8x8 ) ? SMB_BLOCK_SIZE : BLOCK_SIZE;
    int block_size_y = ( mv_mode == SMB4x8 || mv_mode == SMB8x8 ) ? SMB_BLOCK_SIZE : BLOCK_SIZE;

    for (k = k_start; k < k_end; k += k_inc)
    {
      i =  (decode_block_scan[k] & 3);
      j = ((decode_block_scan[k] >> 2) & 3);
      perform_mc(currMB, curr_plane, dec_picture, pred_dir, i, j, block_size_x, block_size_y);
    }        
  }

  iTransform(currMB, curr_plane, smb); 

  if (currMB->cbp != 0)
    currSlice->is_reset_coeff = FALSE;
  return 1;
}

int mb_pred_p_inter16x16(Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture)
{
  Slice *currSlice = currMB->p_Slice;
  int smb = (currSlice->slice_type == SP_SLICE);

  set_chroma_vector(currMB);
  perform_mc(currMB, curr_plane, dec_picture, currMB->b8pdir[0], 0, 0, MB_BLOCK_SIZE, MB_BLOCK_SIZE);
  iTransform(currMB, curr_plane, smb);

  if (currMB->cbp != 0)
    currSlice->is_reset_coeff = FALSE;
  return 1;
}

int mb_pred_p_inter16x8(Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture)
{
  Slice *currSlice = currMB->p_Slice;
  int smb = (currSlice->slice_type == SP_SLICE);

  set_chroma_vector(currMB);

  perform_mc(currMB, curr_plane, dec_picture, currMB->b8pdir[0], 0, 0, MB_BLOCK_SIZE, BLOCK_SIZE_8x8);
  perform_mc(currMB, curr_plane, dec_picture, currMB->b8pdir[2], 0, 2, MB_BLOCK_SIZE, BLOCK_SIZE_8x8);
  iTransform(currMB, curr_plane, smb); 
  
  if (currMB->cbp != 0)
    currSlice->is_reset_coeff = FALSE;
  return 1;
}

int mb_pred_p_inter8x16(Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture)
{
  Slice *currSlice = currMB->p_Slice;
  int smb = (currSlice->slice_type == SP_SLICE);

  set_chroma_vector(currMB);

  perform_mc(currMB, curr_plane, dec_picture, currMB->b8pdir[0], 0, 0, BLOCK_SIZE_8x8, MB_BLOCK_SIZE);
  perform_mc(currMB, curr_plane, dec_picture, currMB->b8pdir[1], 2, 0, BLOCK_SIZE_8x8, MB_BLOCK_SIZE);
  iTransform(currMB, curr_plane, smb);

  if (currMB->cbp != 0)
    currSlice->is_reset_coeff = FALSE;
  return 1;
}

static inline void update_neighbor_mvs(PicMotionParams **motion, const PicMotionParams *mv_info, int i4)
{
  (*motion++)[i4 + 1] = *mv_info;
  (*motion  )[i4    ] = *mv_info;
  (*motion  )[i4 + 1] = *mv_info;
}

int mb_pred_b_d8x8temporal(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture)
{
  short ref_idx;
  int refList;

  int k, i, j, i4, j4, j6;
  int block8x8;   // needed for ABT
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  PicMotionParams *mv_info = NULL, *colocated = NULL;
  
  int list_offset = currMB->list_offset;
  StorablePicture **list0 = currSlice->listX[LIST_0 + list_offset];
  StorablePicture **list1 = currSlice->listX[LIST_1 + list_offset];

  set_chroma_vector(currMB);

  //printf("currMB %d\n", currMB->mbAddrX);
  for (block8x8=0; block8x8<4; block8x8++)
  {      
    int pred_dir = currMB->b8pdir[block8x8];

    int k_start = (block8x8 << 2);
    int k_end = k_start + 1;

    for (k = k_start; k < k_start + BLOCK_MULTIPLE; k ++)
    {

      i =  (decode_block_scan[k] & 3);
      j = ((decode_block_scan[k] >> 2) & 3);
      i4   = currMB->block_x + i;
      j4   = currMB->block_y + j;
      j6   = currMB->block_y_aff + j;
      mv_info = &dec_picture->mv_info[j4][i4];
      colocated = &list1[0]->mv_info[RSD(j6)][RSD(i4)];
      if(currMB->p_Vid->separate_colour_plane_flag && currMB->p_Vid->yuv_format==YUV444)
        colocated = &list1[0]->JVmv_info[currMB->p_Slice->colour_plane_id][RSD(j6)][RSD(i4)];
      if(currSlice->mb_aff_frame_flag /*&& (!p_Vid->active_sps->frame_mbs_only_flag || p_Vid->active_sps->direct_8x8_inference_flag)*/)
      {
        assert(p_Vid->active_sps->direct_8x8_inference_flag);
        if(!currMB->mb_field && ((currSlice->listX[LIST_1][0]->iCodingType==FRAME_MB_PAIR_CODING && currSlice->listX[LIST_1][0]->motion.mb_field[currMB->mbAddrX]) ||
          (currSlice->listX[LIST_1][0]->iCodingType==FIELD_CODING)))
        {
          if (iabs(dec_picture->poc - currSlice->listX[LIST_1+4][0]->poc)> iabs(dec_picture->poc -currSlice->listX[LIST_1+2][0]->poc) )
          {
            colocated = p_Vid->active_sps->direct_8x8_inference_flag ? 
              &currSlice->listX[LIST_1+2][0]->mv_info[RSD(j6)>>1][RSD(i4)] : &currSlice->listX[LIST_1+2][0]->mv_info[j6>>1][i4];
          }
          else
          {
            colocated = p_Vid->active_sps->direct_8x8_inference_flag ? 
              &currSlice->listX[LIST_1+4][0]->mv_info[RSD(j6)>>1][RSD(i4)] : &currSlice->listX[LIST_1+4][0]->mv_info[j6>>1][i4];
          }
        }
      }
      else if(/*!currSlice->mb_aff_frame_flag &&*/ !p_Vid->active_sps->frame_mbs_only_flag && 
        (!currSlice->field_pic_flag && currSlice->listX[LIST_1][0]->iCodingType!=FRAME_CODING))
      {
        if (iabs(dec_picture->poc - list1[0]->bottom_field->poc)> iabs(dec_picture->poc -list1[0]->top_field->poc) )
        {
          colocated = p_Vid->active_sps->direct_8x8_inference_flag ? 
            &list1[0]->top_field->mv_info[RSD(j6)>>1][RSD(i4)] : &list1[0]->top_field->mv_info[j6>>1][i4];
        }
        else
        {
          colocated = p_Vid->active_sps->direct_8x8_inference_flag ? 
            &list1[0]->bottom_field->mv_info[RSD(j6)>>1][RSD(i4)] : &list1[0]->bottom_field->mv_info[j6>>1][i4];
        }
      }
      else if(!p_Vid->active_sps->frame_mbs_only_flag && currSlice->field_pic_flag && currSlice->structure!=list1[0]->structure && list1[0]->coded_frame)
      {
        if (currSlice->structure == TOP_FIELD)
        {
          colocated = p_Vid->active_sps->direct_8x8_inference_flag ? 
            &list1[0]->frame->top_field->mv_info[RSD(j6)][RSD(i4)] : &list1[0]->frame->top_field->mv_info[j6][i4];
        }
        else
        {
          colocated = p_Vid->active_sps->direct_8x8_inference_flag ? 
            &list1[0]->frame->bottom_field->mv_info[RSD(j6)][RSD(i4)] : &list1[0]->frame->bottom_field->mv_info[j6][i4];
        }
      }


      assert (pred_dir<=2);

      refList = (colocated->ref_idx[LIST_0]== -1 ? LIST_1 : LIST_0);
      ref_idx =  colocated->ref_idx[refList];

      if(ref_idx==-1) // co-located is intra mode
      {
        mv_info->mv[LIST_0] = zero_mv;
        mv_info->mv[LIST_1] = zero_mv;

        mv_info->ref_idx[LIST_0] = 0;
        mv_info->ref_idx[LIST_1] = 0;
      }
      else // co-located skip or inter mode
      {
        int mapped_idx=0;
        int iref;
        if( (currSlice->mb_aff_frame_flag && ( (currMB->mb_field && colocated->ref_pic[refList]->structure==FRAME) || 
          (!currMB->mb_field && colocated->ref_pic[refList]->structure!=FRAME))) ||
          (!currSlice->mb_aff_frame_flag && ((currSlice->field_pic_flag==0 && colocated->ref_pic[refList]->structure!=FRAME)
          ||(currSlice->field_pic_flag==1 && colocated->ref_pic[refList]->structure==FRAME))) )
        {
          for (iref = 0; iref < imin(currSlice->num_ref_idx_active[LIST_0], currSlice->listXsize[LIST_0 + list_offset]);iref++)
          {
            if(currSlice->listX[LIST_0 + list_offset][iref]->top_field == colocated->ref_pic[refList] || 
              currSlice->listX[LIST_0 + list_offset][iref]->bottom_field == colocated->ref_pic[refList] ||
              currSlice->listX[LIST_0 + list_offset][iref]->frame == colocated->ref_pic[refList])
            {
              if ((currSlice->field_pic_flag==1) && (currSlice->listX[LIST_0 + list_offset][iref]->structure != currSlice->structure))
              {
                mapped_idx=INVALIDINDEX;
              }
              else
              {
                mapped_idx = iref;            
                break;
              }
            }
            else //! invalid index. Default to zero even though this case should not happen
            {
              mapped_idx=INVALIDINDEX;
            }
          }
        }
        else
        {
          for (iref = 0; iref < imin(currSlice->num_ref_idx_active[LIST_0], currSlice->listXsize[LIST_0 + list_offset]);iref++)
          {
            if(currSlice->listX[LIST_0 + list_offset][iref] == colocated->ref_pic[refList])
            {
              mapped_idx = iref;            
              break;
            }
            else //! invalid index. Default to zero even though this case should not happen
            {
              mapped_idx=INVALIDINDEX;
            }
          }
        }

        if(INVALIDINDEX != mapped_idx)
        {
          int mv_scale = currSlice->mvscale[LIST_0 + list_offset][mapped_idx];
          int mv_y = colocated->mv[refList].mv_y; 
          if((currSlice->mb_aff_frame_flag && !currMB->mb_field && colocated->ref_pic[refList]->structure!=FRAME) ||
            (!currSlice->mb_aff_frame_flag && currSlice->field_pic_flag==0 && colocated->ref_pic[refList]->structure!=FRAME) )
            mv_y *= 2;
          else if((currSlice->mb_aff_frame_flag && currMB->mb_field && colocated->ref_pic[refList]->structure==FRAME) ||
            (!currSlice->mb_aff_frame_flag && currSlice->field_pic_flag==1 && colocated->ref_pic[refList]->structure==FRAME) )
            mv_y /= 2;

          //! In such case, an array is needed for each different reference.
          if (mv_scale == 9999 || currSlice->listX[LIST_0 + list_offset][mapped_idx]->is_long_term)
          {
            mv_info->mv[LIST_0].mv_x = colocated->mv[refList].mv_x;
            mv_info->mv[LIST_0].mv_y = (short) mv_y;
            mv_info->mv[LIST_1] = zero_mv;
          }
          else
          {
            mv_info->mv[LIST_0].mv_x = (short) ((mv_scale * colocated->mv[refList].mv_x + 128 ) >> 8);
            mv_info->mv[LIST_0].mv_y = (short) ((mv_scale * mv_y/*colocated->mv[refList].mv_y*/ + 128 ) >> 8);

            mv_info->mv[LIST_1].mv_x = (short) (mv_info->mv[LIST_0].mv_x - colocated->mv[refList].mv_x);
            mv_info->mv[LIST_1].mv_y = (short) (mv_info->mv[LIST_0].mv_y - mv_y/*colocated->mv[refList].mv_y*/);
          }

          mv_info->ref_idx[LIST_0] = (char) mapped_idx; //colocated->ref_idx[refList];
          mv_info->ref_idx[LIST_1] = 0;
        }
        else if (INVALIDINDEX == mapped_idx)
        {
          error("temporal direct error: colocated block has ref that is unavailable",-1111);
        }

      }
      // store reference picture ID determined by direct mode
      mv_info->ref_pic[LIST_0] = list0[(short)mv_info->ref_idx[LIST_0]];
      mv_info->ref_pic[LIST_1] = list1[(short)mv_info->ref_idx[LIST_1]];
    }

    for (k = k_start; k < k_end; k ++)
    {
      int i =  (decode_block_scan[k] & 3);
      int j = ((decode_block_scan[k] >> 2) & 3);
      perform_mc(currMB, curr_plane, dec_picture, pred_dir, i, j, SMB_BLOCK_SIZE, SMB_BLOCK_SIZE);
    }
  }

  if (currMB->cbp == 0)
  {
    copy_image_data_16x16(&currImg[currMB->pix_y], currSlice->mb_pred[curr_plane], currMB->pix_x, 0);

    if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
    {
      copy_image_data(&dec_picture->imgUV[0][currMB->pix_c_y], currSlice->mb_pred[1], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
      copy_image_data(&dec_picture->imgUV[1][currMB->pix_c_y], currSlice->mb_pred[2], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
    }
  }
  else
  {
    iTransform(currMB, curr_plane, 0); 
    currSlice->is_reset_coeff = FALSE;
  }
  return 1;
}

int mb_pred_b_d4x4temporal(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture)
{
  short ref_idx;
  int refList;

  int k;
  int block8x8;   // needed for ABT
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int list_offset = currMB->list_offset;
  StorablePicture **list0 = currSlice->listX[LIST_0 + list_offset];
  StorablePicture **list1 = currSlice->listX[LIST_1 + list_offset];

  set_chroma_vector(currMB);

  for (block8x8=0; block8x8<4; block8x8++)
  {      
    int pred_dir = currMB->b8pdir[block8x8];

    int k_start = (block8x8 << 2);
    int k_end = k_start + BLOCK_MULTIPLE;

    for (k = k_start; k < k_end; k ++)
    {

      int i =  (decode_block_scan[k] & 3);
      int j = ((decode_block_scan[k] >> 2) & 3);
      int i4   = currMB->block_x + i;
      int j4   = currMB->block_y + j;
      int j6   = currMB->block_y_aff + j;
      PicMotionParams *mv_info = &dec_picture->mv_info[j4][i4];
      PicMotionParams *colocated = &list1[0]->mv_info[j6][i4];
      if(currMB->p_Vid->separate_colour_plane_flag && currMB->p_Vid->yuv_format==YUV444)
        colocated = &list1[0]->JVmv_info[currMB->p_Slice->colour_plane_id][RSD(j6)][RSD(i4)];
      assert (pred_dir<=2);

      refList = (colocated->ref_idx[LIST_0]== -1 ? LIST_1 : LIST_0);
      ref_idx =  colocated->ref_idx[refList];

      if(ref_idx==-1) // co-located is intra mode
      {
        mv_info->mv[LIST_0] = zero_mv;
        mv_info->mv[LIST_1] = zero_mv;

        mv_info->ref_idx[LIST_0] = 0;
        mv_info->ref_idx[LIST_1] = 0;
      }
      else // co-located skip or inter mode
      {
        int mapped_idx=0;
        int iref;

        for (iref=0;iref<imin(currSlice->num_ref_idx_active[LIST_0], currSlice->listXsize[LIST_0 + list_offset]);iref++)
        {
          if(currSlice->listX[LIST_0 + list_offset][iref] == colocated->ref_pic[refList])
          {
            mapped_idx=iref;
            break;
          }
          else //! invalid index. Default to zero even though this case should not happen
          {
            mapped_idx=INVALIDINDEX;
          }
        }
        if (INVALIDINDEX == mapped_idx)
        {
          error("temporal direct error: colocated block has ref that is unavailable",-1111);
        }
        else
        {
          int mv_scale = currSlice->mvscale[LIST_0 + list_offset][mapped_idx];

          //! In such case, an array is needed for each different reference.
          if (mv_scale == 9999 || currSlice->listX[LIST_0+list_offset][mapped_idx]->is_long_term)
          {
            mv_info->mv[LIST_0] = colocated->mv[refList];
            mv_info->mv[LIST_1] = zero_mv;
          }
          else
          {
            mv_info->mv[LIST_0].mv_x = (short) ((mv_scale * colocated->mv[refList].mv_x + 128 ) >> 8);
            mv_info->mv[LIST_0].mv_y = (short) ((mv_scale * colocated->mv[refList].mv_y + 128 ) >> 8);

            mv_info->mv[LIST_1].mv_x = (short) (mv_info->mv[LIST_0].mv_x - colocated->mv[refList].mv_x);
            mv_info->mv[LIST_1].mv_y = (short) (mv_info->mv[LIST_0].mv_y - colocated->mv[refList].mv_y);
          }

          mv_info->ref_idx[LIST_0] = (char) mapped_idx; //colocated->ref_idx[refList];
          mv_info->ref_idx[LIST_1] = 0;
        }
      }
      // store reference picture ID determined by direct mode
      mv_info->ref_pic[LIST_0] = list0[(short)mv_info->ref_idx[LIST_0]];
      mv_info->ref_pic[LIST_1] = list1[(short)mv_info->ref_idx[LIST_1]];
    }

    for (k = k_start; k < k_end; k ++)
    {
      int i =  (decode_block_scan[k] & 3);
      int j = ((decode_block_scan[k] >> 2) & 3);
      perform_mc(currMB, curr_plane, dec_picture, pred_dir, i, j, BLOCK_SIZE, BLOCK_SIZE);

    }
  }

  if (currMB->cbp == 0)
  {
    copy_image_data_16x16(&currImg[currMB->pix_y], currSlice->mb_pred[curr_plane], currMB->pix_x, 0);

    if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
    {
      copy_image_data(&dec_picture->imgUV[0][currMB->pix_c_y], currSlice->mb_pred[1], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
      copy_image_data(&dec_picture->imgUV[1][currMB->pix_c_y], currSlice->mb_pred[2], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
    }
  }
  else
  {
    iTransform(currMB, curr_plane, 0); 
    currSlice->is_reset_coeff = FALSE;
  }

  return 1;
}


int mb_pred_b_d8x8spatial(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture)
{
  char l0_rFrame = -1, l1_rFrame = -1;
  MotionVector pmvl0 = zero_mv, pmvl1 = zero_mv;
  int i4, j4;
  int block8x8;
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  PicMotionParams *mv_info;
  int list_offset = currMB->list_offset;
  StorablePicture **list0 = currSlice->listX[LIST_0 + list_offset];
  StorablePicture **list1 = currSlice->listX[LIST_1 + list_offset];

  int pred_dir = 0;

  set_chroma_vector(currMB);

  prepare_direct_params(currMB, dec_picture, &pmvl0, &pmvl1, &l0_rFrame, &l1_rFrame);

  if (l0_rFrame == 0 || l1_rFrame == 0)
  {
    int is_not_moving;

    for (block8x8 = 0; block8x8 < 4; block8x8++)
    {
      int k_start = (block8x8 << 2);

      int i  =  (decode_block_scan[k_start] & 3);
      int j  = ((decode_block_scan[k_start] >> 2) & 3);
      i4  = currMB->block_x + i;
      j4  = currMB->block_y + j;

      is_not_moving = (get_colocated_info_8x8(currMB, list1[0], i4, currMB->block_y_aff + j) == 0);

      mv_info = &dec_picture->mv_info[j4][i4];

      //===== DIRECT PREDICTION =====
      if (l1_rFrame == -1)
      {
        if (is_not_moving)
        {
          mv_info->ref_pic[LIST_0] = list0[0];
          mv_info->ref_pic[LIST_1] = NULL;
          mv_info->mv[LIST_0] = zero_mv;
          mv_info->mv[LIST_1] = zero_mv;
          mv_info->ref_idx[LIST_0] = 0;
          mv_info->ref_idx[LIST_1] = -1;
        }
        else
        {
          mv_info->ref_pic[LIST_0] = list0[(short) l0_rFrame];
          mv_info->ref_pic[LIST_1] = NULL;
          mv_info->mv[LIST_0] = pmvl0;
          mv_info->mv[LIST_1] = zero_mv;
          mv_info->ref_idx[LIST_0] = l0_rFrame;
          mv_info->ref_idx[LIST_1] = -1;
        }
        pred_dir = 0;
      }
      else if (l0_rFrame == -1) 
      {
        if  (is_not_moving)
        {
          mv_info->ref_pic[LIST_0] = NULL;
          mv_info->ref_pic[LIST_1] = list1[0];
          mv_info->mv[LIST_0] = zero_mv;
          mv_info->mv[LIST_1] = zero_mv;
          mv_info->ref_idx[LIST_0] = -1;
          mv_info->ref_idx[LIST_1] = 0;
        }
        else
        {
          mv_info->ref_pic[LIST_0] = NULL;
          mv_info->ref_pic[LIST_1] = list1[(short) l1_rFrame];
          mv_info->mv[LIST_0] = zero_mv;            
          mv_info->mv[LIST_1] = pmvl1;
          mv_info->ref_idx[LIST_0] = -1;
          mv_info->ref_idx[LIST_1] = l1_rFrame;
        }
        pred_dir = 1;
      }
      else
      {
        if (l0_rFrame == 0 && ((is_not_moving)))
        {
          mv_info->ref_pic[LIST_0] = list0[0];
          mv_info->mv[LIST_0] = zero_mv;
          mv_info->ref_idx[LIST_0] = 0;
        }
        else
        {
          mv_info->ref_pic[LIST_0] = list0[(short) l0_rFrame];
          mv_info->mv[LIST_0] = pmvl0;
          mv_info->ref_idx[LIST_0] = l0_rFrame;
        }

        if  (l1_rFrame == 0 && ((is_not_moving)))
        {
          mv_info->ref_pic[LIST_1] = list1[0];
          mv_info->mv[LIST_1] = zero_mv;
          mv_info->ref_idx[LIST_1]    = 0;
        }
        else
        {
          mv_info->ref_pic[LIST_1] = list1[(short) l1_rFrame];
          mv_info->mv[LIST_1] = pmvl1;
          mv_info->ref_idx[LIST_1] = l1_rFrame;              
        } 
        pred_dir = 2;
      }

      update_neighbor_mvs(&dec_picture->mv_info[j4], mv_info, i4);
      perform_mc(currMB, curr_plane, dec_picture, pred_dir, i, j, SMB_BLOCK_SIZE, SMB_BLOCK_SIZE);
    }
  }
  else
  {
    //===== DIRECT PREDICTION =====
    if (l0_rFrame < 0 && l1_rFrame < 0)
    {
      pred_dir = 2;
      for (j4 = currMB->block_y; j4 < currMB->block_y + BLOCK_MULTIPLE; j4 += 2)
      {
        for (i4 = currMB->block_x; i4 < currMB->block_x + BLOCK_MULTIPLE; i4 += 2)
        {
          mv_info = &dec_picture->mv_info[j4][i4];

          mv_info->ref_pic[LIST_0] = list0[0];
          mv_info->ref_pic[LIST_1] = list1[0];
          mv_info->mv[LIST_0] = zero_mv;
          mv_info->mv[LIST_1] = zero_mv;
          mv_info->ref_idx[LIST_0] = 0;
          mv_info->ref_idx[LIST_1] = 0;            

          update_neighbor_mvs(&dec_picture->mv_info[j4], mv_info, i4);
        }
      }
    }
    else if (l1_rFrame == -1) 
    {            
      pred_dir = 0;

      for (j4 = currMB->block_y; j4 < currMB->block_y + BLOCK_MULTIPLE; j4 += 2)
      {
        for (i4 = currMB->block_x; i4 < currMB->block_x + BLOCK_MULTIPLE; i4 += 2)
        {
          mv_info = &dec_picture->mv_info[j4][i4];

          mv_info->ref_pic[LIST_0] = list0[(short) l0_rFrame];
          mv_info->ref_pic[LIST_1] = NULL;
          mv_info->mv[LIST_0] = pmvl0;
          mv_info->mv[LIST_1] = zero_mv;
          mv_info->ref_idx[LIST_0] = l0_rFrame;
          mv_info->ref_idx[LIST_1] = -1;

          update_neighbor_mvs(&dec_picture->mv_info[j4], mv_info, i4);
        }
      }
    }
    else if (l0_rFrame == -1) 
    {
      pred_dir = 1;
      for (j4 = currMB->block_y; j4 < currMB->block_y + BLOCK_MULTIPLE; j4 += 2)
      {
        for (i4 = currMB->block_x; i4 < currMB->block_x + BLOCK_MULTIPLE; i4 += 2)
        {
          mv_info = &dec_picture->mv_info[j4][i4];

          mv_info->ref_pic[LIST_0] = NULL;
          mv_info->ref_pic[LIST_1] = list1[(short) l1_rFrame];
          mv_info->mv[LIST_0] = zero_mv;
          mv_info->mv[LIST_1] = pmvl1;
          mv_info->ref_idx[LIST_0] = -1;
          mv_info->ref_idx[LIST_1] = l1_rFrame;

          update_neighbor_mvs(&dec_picture->mv_info[j4], mv_info, i4);
        }
      }
    }
    else
    {
      pred_dir = 2;

      for (j4 = currMB->block_y; j4 < currMB->block_y + BLOCK_MULTIPLE; j4 += 2)
      {
        for (i4 = currMB->block_x; i4 < currMB->block_x + BLOCK_MULTIPLE; i4 += 2)
        {
          mv_info = &dec_picture->mv_info[j4][i4];

          mv_info->ref_pic[LIST_0] = list0[(short) l0_rFrame];
          mv_info->ref_pic[LIST_1] = list1[(short) l1_rFrame];
          mv_info->mv[LIST_0] = pmvl0;
          mv_info->mv[LIST_1] = pmvl1;
          mv_info->ref_idx[LIST_0] = l0_rFrame;
          mv_info->ref_idx[LIST_1] = l1_rFrame;            

          update_neighbor_mvs(&dec_picture->mv_info[j4], mv_info, i4);
        }
      }
    }
    // Now perform Motion Compensation
    perform_mc(currMB, curr_plane, dec_picture, pred_dir, 0, 0, MB_BLOCK_SIZE, MB_BLOCK_SIZE);
  }

  if (currMB->cbp == 0)
  {
    copy_image_data_16x16(&currImg[currMB->pix_y], currSlice->mb_pred[curr_plane], currMB->pix_x, 0);

    if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
     {
      copy_image_data(&dec_picture->imgUV[0][currMB->pix_c_y], currSlice->mb_pred[1], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
      copy_image_data(&dec_picture->imgUV[1][currMB->pix_c_y], currSlice->mb_pred[2], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
    }
  }
  else
  {
    iTransform(currMB, curr_plane, 0); 
    currSlice->is_reset_coeff = FALSE;
  }

  return 1;
}


int mb_pred_b_d4x4spatial(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture)
{
  char l0_rFrame = -1, l1_rFrame = -1;
  MotionVector pmvl0 = zero_mv, pmvl1 = zero_mv;
  int k;
  int block8x8;
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  PicMotionParams *mv_info;
  int list_offset = currMB->list_offset;
  StorablePicture **list0 = currSlice->listX[LIST_0 + list_offset];
  StorablePicture **list1 = currSlice->listX[LIST_1 + list_offset];

  int pred_dir = 0;

  set_chroma_vector(currMB);

  prepare_direct_params(currMB, dec_picture, &pmvl0, &pmvl1, &l0_rFrame, &l1_rFrame);

  for (block8x8 = 0; block8x8 < 4; block8x8++)
  {
    int k_start = (block8x8 << 2);
    int k_end = k_start + BLOCK_MULTIPLE;

    for (k = k_start; k < k_end; k ++)
    {
      int i  =  (decode_block_scan[k] & 3);
      int j  = ((decode_block_scan[k] >> 2) & 3);
      int i4  = currMB->block_x + i;
      int j4  = currMB->block_y + j;
      
      mv_info = &dec_picture->mv_info[j4][i4];
      //===== DIRECT PREDICTION =====      
      if (l0_rFrame == 0 || l1_rFrame == 0)
      {
        int is_not_moving = (get_colocated_info_4x4(currMB, list1[0], i4, currMB->block_y_aff + j) == 0);

        if (l1_rFrame == -1)
        {
          if (is_not_moving)
          {
            mv_info->ref_pic[LIST_0] = list0[0];
            mv_info->ref_pic[LIST_1] = NULL;
            mv_info->mv[LIST_0] = zero_mv;
            mv_info->mv[LIST_1] = zero_mv;
            mv_info->ref_idx[LIST_0] = 0;
            mv_info->ref_idx[LIST_1] = -1;
          }
          else
          {
            mv_info->ref_pic[LIST_0] = list0[(short) l0_rFrame];
            mv_info->ref_pic[LIST_1] = NULL;
            mv_info->mv[LIST_0] = pmvl0;
            mv_info->mv[LIST_1] = zero_mv;
            mv_info->ref_idx[LIST_0] = l0_rFrame;
            mv_info->ref_idx[LIST_1] = -1;
          }
          pred_dir = 0;
        }
        else if (l0_rFrame == -1) 
        {
          if  (is_not_moving)
          {
            mv_info->ref_pic[LIST_0] = NULL;
            mv_info->ref_pic[LIST_1] = list1[0];
            mv_info->mv[LIST_0] = zero_mv;
            mv_info->mv[LIST_1] = zero_mv;
            mv_info->ref_idx[LIST_0] = -1;
            mv_info->ref_idx[LIST_1] = 0;
          }
          else
          {
            mv_info->ref_pic[LIST_0] = NULL;
            mv_info->ref_pic[LIST_1] = list1[(short) l1_rFrame];
            mv_info->mv[LIST_0] = zero_mv;            
            mv_info->mv[LIST_1] = pmvl1;
            mv_info->ref_idx[LIST_0] = -1;
            mv_info->ref_idx[LIST_1] = l1_rFrame;
          }
          pred_dir = 1;
        }
        else
        {
          if (l0_rFrame == 0 && ((is_not_moving)))
          {
            mv_info->ref_pic[LIST_0] = list0[0];
            mv_info->mv[LIST_0] = zero_mv;
            mv_info->ref_idx[LIST_0] = 0;
          }
          else
          {
            mv_info->ref_pic[LIST_0] = list0[(short) l0_rFrame];
            mv_info->mv[LIST_0] = pmvl0;
            mv_info->ref_idx[LIST_0] = l0_rFrame;
          }

          if  (l1_rFrame == 0 && ((is_not_moving)))
          {
            mv_info->ref_pic[LIST_1] = list1[0];
            mv_info->mv[LIST_1] = zero_mv;
            mv_info->ref_idx[LIST_1] = 0;
          }
          else
          {
            mv_info->ref_pic[LIST_1] = list1[(short) l1_rFrame];
            mv_info->mv[LIST_1] = pmvl1;
            mv_info->ref_idx[LIST_1] = l1_rFrame;              
          }            
          pred_dir = 2;
        }
      }
      else 
      {       
        mv_info = &dec_picture->mv_info[j4][i4];

        if (l0_rFrame < 0 && l1_rFrame < 0)
        {
          pred_dir = 2;
          mv_info->ref_pic[LIST_0] = list0[0];
          mv_info->ref_pic[LIST_1] = list1[0];
          mv_info->mv[LIST_0] = zero_mv;
          mv_info->mv[LIST_1] = zero_mv;
          mv_info->ref_idx[LIST_0] = 0;
          mv_info->ref_idx[LIST_1] = 0;
        }
        else if (l1_rFrame == -1)
        {
          pred_dir = 0;
          mv_info->ref_pic[LIST_0] = list0[(short) l0_rFrame];
          mv_info->ref_pic[LIST_1] = NULL;
          mv_info->mv[LIST_0] = pmvl0;
          mv_info->mv[LIST_1] = zero_mv;
          mv_info->ref_idx[LIST_0] = l0_rFrame;
          mv_info->ref_idx[LIST_1] = -1;
        }
        else if (l0_rFrame == -1) 
        {
          pred_dir = 1;
          mv_info->ref_pic[LIST_0] = NULL;
          mv_info->ref_pic[LIST_1] = list1[(short) l1_rFrame];
          mv_info->mv[LIST_0] = zero_mv;
          mv_info->mv[LIST_1] = pmvl1;
          mv_info->ref_idx[LIST_0] = -1;
          mv_info->ref_idx[LIST_1] = l1_rFrame;
        }
        else
        {
          pred_dir = 2;
          mv_info->ref_pic[LIST_0] = list0[(short) l0_rFrame];
          mv_info->ref_pic[LIST_1] = list1[(short) l1_rFrame];
          mv_info->mv[LIST_0] = pmvl0;
          mv_info->mv[LIST_1] = pmvl1;
          mv_info->ref_idx[LIST_0] = l0_rFrame;
          mv_info->ref_idx[LIST_1] = l1_rFrame;            
        }
      }
    }

    for (k = k_start; k < k_end; k ++)
    {        
      int i =  (decode_block_scan[k] & 3);
      int j = ((decode_block_scan[k] >> 2) & 3);

      perform_mc(currMB, curr_plane, dec_picture, pred_dir, i, j, BLOCK_SIZE, BLOCK_SIZE);
    }
  }

  if (currMB->cbp == 0)
  {
    copy_image_data_16x16(&currImg[currMB->pix_y], currSlice->mb_pred[curr_plane], currMB->pix_x, 0);

    if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
    {
      copy_image_data(&dec_picture->imgUV[0][currMB->pix_c_y], currSlice->mb_pred[1], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
      copy_image_data(&dec_picture->imgUV[1][currMB->pix_c_y], currSlice->mb_pred[2], currMB->pix_c_x, 0, p_Vid->mb_size[1][0], p_Vid->mb_size[1][1]);
    }
  }
  else
  {
    iTransform(currMB, curr_plane, 0); 
    currSlice->is_reset_coeff = FALSE;
  }

  return 1;
}

int mb_pred_b_inter8x8(Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture)
{
  char l0_rFrame = -1, l1_rFrame = -1;
  MotionVector pmvl0 = zero_mv, pmvl1 = zero_mv;
  int block_size_x, block_size_y;
  int k;
  int block8x8;   // needed for ABT
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  int list_offset = currMB->list_offset;
  StorablePicture **list0 = currSlice->listX[LIST_0 + list_offset];
  StorablePicture **list1 = currSlice->listX[LIST_1 + list_offset];

  set_chroma_vector(currMB);
  
  // prepare direct modes
  if (currSlice->direct_spatial_mv_pred_flag && (!(currMB->b8mode[0] && currMB->b8mode[1] && currMB->b8mode[2] && currMB->b8mode[3])))
    prepare_direct_params(currMB, dec_picture, &pmvl0, &pmvl1, &l0_rFrame, &l1_rFrame);

  for (block8x8=0; block8x8<4; block8x8++)
  {
    int mv_mode  = currMB->b8mode[block8x8];
    int pred_dir = currMB->b8pdir[block8x8];

    if ( mv_mode != 0 )
    {
      int k_start = (block8x8 << 2);
      int k_inc = (mv_mode == SMB8x4) ? 2 : 1;
      int k_end = (mv_mode == SMB8x8) ? k_start + 1 : ((mv_mode == SMB4x4) ? k_start + 4 : k_start + k_inc + 1);

      block_size_x = ( mv_mode == SMB8x4 || mv_mode == SMB8x8 ) ? SMB_BLOCK_SIZE : BLOCK_SIZE;
      block_size_y = ( mv_mode == SMB4x8 || mv_mode == SMB8x8 ) ? SMB_BLOCK_SIZE : BLOCK_SIZE;

      for (k = k_start; k < k_end; k += k_inc)
      {
        int i =  (decode_block_scan[k] & 3);
        int j = ((decode_block_scan[k] >> 2) & 3);
        perform_mc(currMB, curr_plane, dec_picture, pred_dir, i, j, block_size_x, block_size_y);
      }        
    }
    else
    {
      int k_start = (block8x8 << 2);
      int k_end = k_start;

      if (p_Vid->active_sps->direct_8x8_inference_flag)
      {
        block_size_x = SMB_BLOCK_SIZE;
        block_size_y = SMB_BLOCK_SIZE;
        k_end ++;
      }
      else
      {
        block_size_x = BLOCK_SIZE;
        block_size_y = BLOCK_SIZE;
        k_end += BLOCK_MULTIPLE;
      }

      // Prepare mvs (needed for deblocking and mv prediction
      if (currSlice->direct_spatial_mv_pred_flag)
      {
        for (k = k_start; k < k_start + BLOCK_MULTIPLE; k ++)
        {
          int i  =  (decode_block_scan[k] & 3);
          int j  = ((decode_block_scan[k] >> 2) & 3);
          int i4  = currMB->block_x + i;
          int j4  = currMB->block_y + j;
          PicMotionParams *mv_info = &dec_picture->mv_info[j4][i4];

          assert (pred_dir<=2);
          
          //===== DIRECT PREDICTION =====
          // motion information should be already set 
          if (mv_info->ref_idx[LIST_1] == -1) 
          {
            pred_dir = 0;
          }
          else if (mv_info->ref_idx[LIST_0] == -1) 
          {
            pred_dir = 1;
          }
          else               
          {
            pred_dir = 2;
          }
        }
      }
      else
      {
        for (k = k_start; k < k_start + BLOCK_MULTIPLE; k ++)
        {

          int i =  (decode_block_scan[k] & 3);
          int j = ((decode_block_scan[k] >> 2) & 3);
          int i4   = currMB->block_x + i;
          int j4   = currMB->block_y + j;
          PicMotionParams *mv_info = &dec_picture->mv_info[j4][i4];

          assert (pred_dir<=2);

          // store reference picture ID determined by direct mode
          mv_info->ref_pic[LIST_0] = list0[(short)mv_info->ref_idx[LIST_0]];
          mv_info->ref_pic[LIST_1] = list1[(short)mv_info->ref_idx[LIST_1]];
        }
      }

      for (k = k_start; k < k_end; k ++)
      {
        int i =  (decode_block_scan[k] & 3);
        int j = ((decode_block_scan[k] >> 2) & 3);
        perform_mc(currMB, curr_plane, dec_picture, pred_dir, i, j, block_size_x, block_size_y);
      } 
    }
  }

  iTransform(currMB, curr_plane, 0);
  if (currMB->cbp != 0)
    currSlice->is_reset_coeff = FALSE;
  return 1;
}

/*!
 ************************************************************************
 * \brief
 *    Copy IPCM coefficients to decoded picture buffer and set parameters for this MB
 *    (for IPCM CABAC and IPCM CAVLC  28/11/2003)
 *
 * \author
 *    Dong Wang <Dong.Wang@bristol.ac.uk>
 ************************************************************************
 */

int mb_pred_ipcm(Macroblock *currMB)
{
  int i, j, k;
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  StorablePicture *dec_picture = currSlice->dec_picture;

  //Copy coefficients to decoded picture buffer
  //IPCM coefficients are stored in currSlice->cof which is set in function read_IPCM_coeffs_from_NAL()

  for(i = 0; i < MB_BLOCK_SIZE; ++i)
  {
    for(j = 0;j < MB_BLOCK_SIZE ; ++j)
    {
      dec_picture->imgY[currMB->pix_y + i][currMB->pix_x + j] = (imgpel) currSlice->cof[0][i][j];
    }
  }

  if ((dec_picture->chroma_format_idc != YUV400) && (p_Vid->separate_colour_plane_flag == 0))
  {
    for (k = 0; k < 2; ++k)
    {
      for(i = 0; i < p_Vid->mb_cr_size_y; ++i)
      {
        for(j = 0;j < p_Vid->mb_cr_size_x; ++j)
        {
          dec_picture->imgUV[k][currMB->pix_c_y+i][currMB->pix_c_x + j] = (imgpel) currSlice->cof[k + 1][i][j];  
        }
      }
    }
  }

  // for deblocking filter
  update_qp(currMB, 0);

  // for CAVLC: Set the nz_coeff to 16.
  // These parameters are to be used in CAVLC decoding of neighbour blocks  
  memset(p_Vid->nz_coeff[currMB->mbAddrX][0][0], 16, 3 * BLOCK_PIXELS * sizeof(byte));

  // for CABAC decoding of MB skip flag
  currMB->skip_flag = 0;

  //for deblocking filter CABAC
  currMB->s_cbp[0].blk = 0xFFFF;

  //For CABAC decoding of Dquant
  currSlice->last_dquant = 0;
  currSlice->is_reset_coeff = FALSE;
  currSlice->is_reset_coeff_cr = FALSE;
  return 1;
}

