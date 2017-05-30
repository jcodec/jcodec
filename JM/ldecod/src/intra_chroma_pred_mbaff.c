/*!
 *************************************************************************************
 * \file intra_chroma_pred.c
 *
 * \brief
 *    Functions for intra chroma prediction
 *
 * \author
 *      Main contributors (see contributors.h for copyright, 
 *                         address and affiliation details)
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 *
 *************************************************************************************
 */
#include "global.h"
#include "block.h"
#include "mb_access.h"
#include "image.h"

static void intra_chroma_DC_single_mbaff(imgpel **curr_img, int up_avail, int left_avail, PixelPos up, PixelPos left[17], int blk_x, int blk_y, int *pred, int direction )
{
  int i;
  int s0 = 0;

  if ((direction && up_avail) || (!left_avail && up_avail))
  {
    for (i = blk_x; i < (blk_x + 4);++i)  
      s0 += curr_img[up.pos_y][up.pos_x + i];
    *pred = (s0 + 2) >> 2;
  }
  else if (left_avail)  
  {
    for (i = blk_y; i < (blk_y + 4);++i)  
      s0 += curr_img[left[i].pos_y][left[i].pos_x];
    *pred = (s0 + 2) >> 2;
  }
}

void intrapred_chroma_ver_mbaff(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  int j;
  StorablePicture *dec_picture = currSlice->dec_picture;

  PixelPos up;        //!< pixel position  p(0,-1)
  int up_avail;
  int cr_MB_x = p_Vid->mb_cr_size_x;
  int cr_MB_y = p_Vid->mb_cr_size_y;

  getAffNeighbour(currMB, 0, -1, p_Vid->mb_size[IS_CHROMA], &up);

  if (!p_Vid->active_pps->constrained_intra_pred_flag)
    up_avail      = up.available;
  else
    up_avail = up.available ? currSlice->intra_block[up.mb_addr] : 0;
  // Vertical Prediction
  if (!up_avail)
    error("unexpected VERT_PRED_8 chroma intra prediction mode",-1);
  else
  {
    imgpel **mb_pred0 = currSlice->mb_pred[1];
    imgpel **mb_pred1 = currSlice->mb_pred[2];
    imgpel *i0 = &(dec_picture->imgUV[0][up.pos_y][up.pos_x]);
    imgpel *i1 = &(dec_picture->imgUV[1][up.pos_y][up.pos_x]);

    for (j = 0; j < cr_MB_y; ++j) 
    {
      memcpy(&(mb_pred0[j][0]),i0, cr_MB_x * sizeof(imgpel));
      memcpy(&(mb_pred1[j][0]),i1, cr_MB_x * sizeof(imgpel));
    }
  }
}


static void intra_chroma_DC_all_mbaff(imgpel **curr_img, int up_avail, int left_avail, PixelPos up, PixelPos left[17], int blk_x, int blk_y, int *pred )
{
  int i;
  int s0 = 0, s1 = 0;

  if (up_avail)       
    for (i = blk_x; i < (blk_x + 4);++i)  
      s0 += curr_img[up.pos_y][up.pos_x + i];

  if (left_avail)  
    for (i = blk_y; i < (blk_y + 4);++i)  
      s1 += curr_img[left[i].pos_y][left[i].pos_x];

  if (up_avail && left_avail)
    *pred = (s0 + s1 + 4) >> 3;
  else if (up_avail)
    *pred = (s0 + 2) >> 2;
  else if (left_avail)
    *pred = (s1 + 2) >> 2;
}


/*!
 ************************************************************************
 * \brief
 *    Chroma Intra prediction. Note that many operations can be moved
 *    outside since they are repeated for both components for no reason.
 ************************************************************************
 */
void intra_pred_chroma_mbaff(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  int i,j, ii, jj;
  StorablePicture *dec_picture = currSlice->dec_picture;

  int ih, iv, ib, ic, iaa;

  int        b8, b4;
  int        yuv = dec_picture->chroma_format_idc - 1;
  int        blk_x, blk_y;
  int        pred;
  static const int block_pos[3][4][4]= //[yuv][b8][b4]
  {
    { {0, 1, 4, 5},{0, 0, 0, 0},{0, 0, 0, 0},{0, 0, 0, 0}},
    { {0, 1, 2, 3},{4, 5, 4, 5},{0, 0, 0, 0},{0, 0, 0, 0}},
    { {0, 1, 2, 3},{1, 1, 3, 3},{4, 5, 4, 5},{5, 5, 5, 5}}
  };

  switch (currMB->c_ipred_mode) 
  {
  case DC_PRED_8:  
    {
      PixelPos up;        //!< pixel position  p(0,-1)
      PixelPos left[17];  //!< pixel positions p(-1, -1..16)

      int up_avail, left_avail[2];

      int cr_MB_y = p_Vid->mb_cr_size_y;
      int cr_MB_y2 = (cr_MB_y >> 1);

      for (i=0; i < cr_MB_y + 1 ; ++i)
        getAffNeighbour(currMB, -1, i-1, p_Vid->mb_size[IS_CHROMA], &left[i]);
      getAffNeighbour(currMB, 0, -1, p_Vid->mb_size[IS_CHROMA], &up);

      if (!p_Vid->active_pps->constrained_intra_pred_flag)
      {
        up_avail      = up.available;
        left_avail[0] = left_avail[1] = left[1].available;
      }
      else
      {
        up_avail = up.available ? currSlice->intra_block[up.mb_addr] : 0;
        for (i=0, left_avail[0] = 1; i < cr_MB_y2;++i)
          left_avail[0]  &= left[i + 1].available ? currSlice->intra_block[left[i + 1].mb_addr]: 0;

        for (i = cr_MB_y2, left_avail[1] = 1; i<cr_MB_y;++i)
          left_avail[1]  &= left[i + 1].available ? currSlice->intra_block[left[i + 1].mb_addr]: 0;

      }
      // DC prediction
      // Note that unlike what is stated in many presentations and papers, this mode does not operate
      // the same way as I_16x16 DC prediction.
      {
        int pred1;
        imgpel **imgUV0 = dec_picture->imgUV[0];
        imgpel **imgUV1 = dec_picture->imgUV[1];
        imgpel **mb_pred0 = currSlice->mb_pred[0 + 1];
        imgpel **mb_pred1 = currSlice->mb_pred[1 + 1];
        for(b8 = 0; b8 < (p_Vid->num_uv_blocks) ;++b8)
        {
          for (b4 = 0; b4 < 4; ++b4)
          {
            blk_y = subblk_offset_y[yuv][b8][b4];
            blk_x = subblk_offset_x[yuv][b8][b4];

            pred = p_Vid->dc_pred_value_comp[1];
            pred1 = p_Vid->dc_pred_value_comp[2];
            //===== get prediction value =====
            switch (block_pos[yuv][b8][b4])
            {
            case 0:  //===== TOP TOP-LEFT =====
              intra_chroma_DC_all_mbaff    (imgUV0, up_avail, left_avail[0], up, left, blk_x, blk_y + 1, &pred);
              intra_chroma_DC_all_mbaff    (imgUV1, up_avail, left_avail[0], up, left, blk_x, blk_y + 1, &pred1);
              break;
            case 1: //===== TOP TOP-RIGHT =====
              intra_chroma_DC_single_mbaff (imgUV0, up_avail, left_avail[0], up, left, blk_x, blk_y + 1, &pred, 1);
              intra_chroma_DC_single_mbaff (imgUV1, up_avail, left_avail[0], up, left, blk_x, blk_y + 1, &pred1, 1);
              break;
            case 2:  //===== TOP BOTTOM-LEFT =====
              intra_chroma_DC_single_mbaff (imgUV0, up_avail, left_avail[0], up, left, blk_x, blk_y + 1, &pred, 0);
              intra_chroma_DC_single_mbaff (imgUV1, up_avail, left_avail[0], up, left, blk_x, blk_y + 1, &pred1, 0);
              break;
            case 3: //===== TOP BOTTOM-RIGHT =====
              intra_chroma_DC_all_mbaff    (imgUV0, up_avail, left_avail[0], up, left, blk_x, blk_y + 1, &pred);
              intra_chroma_DC_all_mbaff    (imgUV1, up_avail, left_avail[0], up, left, blk_x, blk_y + 1, &pred1);
              break;

            case 4: //===== BOTTOM LEFT =====
              intra_chroma_DC_single_mbaff (imgUV0, up_avail, left_avail[1], up, left, blk_x, blk_y + 1, &pred, 0);
              intra_chroma_DC_single_mbaff (imgUV1, up_avail, left_avail[1], up, left, blk_x, blk_y + 1, &pred1, 0);
              break;
            case 5: //===== BOTTOM RIGHT =====
              intra_chroma_DC_all_mbaff   (imgUV0, up_avail, left_avail[1], up, left, blk_x, blk_y + 1, &pred);
              intra_chroma_DC_all_mbaff   (imgUV1, up_avail, left_avail[1], up, left, blk_x, blk_y + 1, &pred1);
              break;
            }

            for (jj = blk_y; jj < blk_y + BLOCK_SIZE; ++jj)
            {
              for (ii = blk_x; ii < blk_x + BLOCK_SIZE; ++ii)
              {
                mb_pred0[jj][ii]=(imgpel) pred;
                mb_pred1[jj][ii]=(imgpel) pred1;
              }
            }
          }
        }
      }
    }
    break;
  case HOR_PRED_8:
    {
      PixelPos left[17];  //!< pixel positions p(-1, -1..16)

      int left_avail[2];

      int cr_MB_x = p_Vid->mb_cr_size_x;
      int cr_MB_y = p_Vid->mb_cr_size_y;
      int cr_MB_y2 = (cr_MB_y >> 1);

      for (i=0; i < cr_MB_y + 1 ; ++i)
        getAffNeighbour(currMB, -1, i-1, p_Vid->mb_size[IS_CHROMA], &left[i]);

      if (!p_Vid->active_pps->constrained_intra_pred_flag)
      {
        left_avail[0] = left_avail[1] = left[1].available;
      }
      else
      {
        for (i=0, left_avail[0] = 1; i < cr_MB_y2;++i)
          left_avail[0]  &= left[i + 1].available ? currSlice->intra_block[left[i + 1].mb_addr]: 0;

        for (i = cr_MB_y2, left_avail[1] = 1; i<cr_MB_y;++i)
          left_avail[1]  &= left[i + 1].available ? currSlice->intra_block[left[i + 1].mb_addr]: 0;
      }
      // Horizontal Prediction
      if (!left_avail[0] || !left_avail[1])
        error("unexpected HOR_PRED_8 chroma intra prediction mode",-1);
      else
      {
        int pred1;
        imgpel **mb_pred0 = currSlice->mb_pred[0 + 1];
        imgpel **mb_pred1 = currSlice->mb_pred[1 + 1];
        imgpel **i0 = dec_picture->imgUV[0];
        imgpel **i1 = dec_picture->imgUV[1];
        for (j = 0; j < cr_MB_y; ++j)
        {
          pred = i0[left[1 + j].pos_y][left[1 + j].pos_x];
          pred1 = i1[left[1 + j].pos_y][left[1 + j].pos_x];
          for (i = 0; i < cr_MB_x; ++i)
          {
            mb_pred0[j][i]=(imgpel) pred;
            mb_pred1[j][i]=(imgpel) pred1;
          }
        }
      }
    }
    break;
  case VERT_PRED_8:
    {
      PixelPos up;        //!< pixel position  p(0,-1)

      int up_avail;

      int cr_MB_x = p_Vid->mb_cr_size_x;
      int cr_MB_y = p_Vid->mb_cr_size_y;

      getAffNeighbour(currMB, 0, -1, p_Vid->mb_size[IS_CHROMA], &up);

      if (!p_Vid->active_pps->constrained_intra_pred_flag)
        up_avail      = up.available;
      else
        up_avail = up.available ? currSlice->intra_block[up.mb_addr] : 0;
      // Vertical Prediction
      if (!up_avail)
        error("unexpected VERT_PRED_8 chroma intra prediction mode",-1);
      else
      {
        imgpel **mb_pred0 = currSlice->mb_pred[0 + 1];
        imgpel **mb_pred1 = currSlice->mb_pred[1 + 1];
        imgpel *i0 = &(dec_picture->imgUV[0][up.pos_y][up.pos_x]);
        imgpel *i1 = &(dec_picture->imgUV[1][up.pos_y][up.pos_x]);
        for (j = 0; j < cr_MB_y; ++j)
        {
          memcpy(&(mb_pred0[j][0]),i0, cr_MB_x * sizeof(imgpel));
          memcpy(&(mb_pred1[j][0]),i1, cr_MB_x * sizeof(imgpel));
        }
      }
    }
    break;
  case PLANE_8:
    {
      PixelPos up;        //!< pixel position  p(0,-1)
      PixelPos left[17];  //!< pixel positions p(-1, -1..16)

      int up_avail, left_avail[2], left_up_avail;

      int cr_MB_x = p_Vid->mb_cr_size_x;
      int cr_MB_y = p_Vid->mb_cr_size_y;
      int cr_MB_y2 = (cr_MB_y >> 1);
      int cr_MB_x2 = (cr_MB_x >> 1);

      for (i=0; i < cr_MB_y + 1 ; ++i)
        getAffNeighbour(currMB, -1, i-1, p_Vid->mb_size[IS_CHROMA], &left[i]);
      getAffNeighbour(currMB, 0, -1, p_Vid->mb_size[IS_CHROMA], &up);

      if (!p_Vid->active_pps->constrained_intra_pred_flag)
      {
        up_avail      = up.available;
        left_avail[0] = left_avail[1] = left[1].available;
        left_up_avail = left[0].available;
      }
      else
      {
        up_avail = up.available ? currSlice->intra_block[up.mb_addr] : 0;
        for (i=0, left_avail[0] = 1; i < cr_MB_y2;++i)
          left_avail[0]  &= left[i + 1].available ? currSlice->intra_block[left[i + 1].mb_addr]: 0;

        for (i = cr_MB_y2, left_avail[1] = 1; i<cr_MB_y;++i)
          left_avail[1]  &= left[i + 1].available ? currSlice->intra_block[left[i + 1].mb_addr]: 0;

        left_up_avail = left[0].available ? currSlice->intra_block[left[0].mb_addr]: 0;
      }
      // plane prediction
      if (!left_up_avail || !left_avail[0] || !left_avail[1] || !up_avail)
        error("unexpected PLANE_8 chroma intra prediction mode",-1);
      else
      {
        int uv;
        for (uv = 0; uv < 2; uv++) 
        {
          imgpel **imgUV = dec_picture->imgUV[uv];
          imgpel **mb_pred = currSlice->mb_pred[uv + 1];
          int max_imgpel_value = p_Vid->max_pel_value_comp[uv + 1];
          imgpel *upPred = &imgUV[up.pos_y][up.pos_x];

          ih = cr_MB_x2 * (upPred[cr_MB_x - 1] - imgUV[left[0].pos_y][left[0].pos_x]);
          for (i = 0; i < cr_MB_x2 - 1; ++i)
            ih += (i + 1) * (upPred[cr_MB_x2 + i] - upPred[cr_MB_x2 - 2 - i]);

          iv = cr_MB_y2 * (imgUV[left[cr_MB_y].pos_y][left[cr_MB_y].pos_x] - imgUV[left[0].pos_y][left[0].pos_x]);
          for (i = 0; i < cr_MB_y2 - 1; ++i)
            iv += (i + 1)*(imgUV[left[cr_MB_y2 + 1 + i].pos_y][left[cr_MB_y2 + 1 + i].pos_x] -
            imgUV[left[cr_MB_y2 - 1 - i].pos_y][left[cr_MB_y2 - 1 - i].pos_x]);

          ib= ((cr_MB_x == 8 ? 17 : 5) * ih + 2 * cr_MB_x)>>(cr_MB_x == 8 ? 5 : 6);
          ic= ((cr_MB_y == 8 ? 17 : 5) * iv + 2 * cr_MB_y)>>(cr_MB_y == 8 ? 5 : 6);

          iaa=16*(imgUV[left[cr_MB_y].pos_y][left[cr_MB_y].pos_x] + upPred[cr_MB_x-1]);

          for (j = 0; j < cr_MB_y; ++j)
            for (i = 0; i < cr_MB_x; ++i)
              mb_pred[j][i]=(imgpel) iClip1(max_imgpel_value, ((iaa + (i - cr_MB_x2 + 1) * ib + (j - cr_MB_y2 + 1) * ic + 16) >> 5));  
        }
      }
    }
    break;
  default:
    error("illegal chroma intra prediction mode", 600);
    break;
  }
}
