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


static void intra_chroma_DC_single(imgpel **curr_img, int up_avail, int left_avail, PixelPos up, PixelPos left, int blk_x, int blk_y, int *pred, int direction )
{
  int i;
  int s0 = 0;

  if ((direction && up_avail) || (!left_avail && up_avail))
  {
    imgpel *cur_pel = &curr_img[up.pos_y][up.pos_x + blk_x];
    for (i = 0; i < 4;++i)  
      s0 += *(cur_pel++);
    *pred = (s0 + 2) >> 2;
  }
  else if (left_avail)  
  {
    imgpel **cur_pel = &(curr_img[left.pos_y + blk_y - 1]);
    int pos_x = left.pos_x;
    for (i = 0; i < 4;++i)  
      s0 += *((*cur_pel++) + pos_x);
    *pred = (s0 + 2) >> 2;
  }
}


static void intra_chroma_DC_all(imgpel **curr_img, int up_avail, int left_avail, PixelPos up, PixelPos left, int blk_x, int blk_y, int *pred )
{
  int i;
  int s0 = 0, s1 = 0;

  if (up_avail)  
  {    
    imgpel *cur_pel = &curr_img[up.pos_y][up.pos_x + blk_x];
    for (i = 0; i < 4;++i)  
      s0 += *(cur_pel++);
  }

  if (left_avail)
  {
    imgpel **cur_pel = &(curr_img[left.pos_y + blk_y - 1]);
    int pos_x = left.pos_x;
    for (i = 0; i < 4;++i)  
      s1 += *((*cur_pel++) + pos_x);
  }

  if (up_avail && left_avail)
    *pred = (s0 + s1 + 4) >> 3;
  else if (up_avail)
    *pred = (s0 + 2) >> 2;
  else if (left_avail)
    *pred = (s1 + 2) >> 2;
}

static void intrapred_chroma_dc(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  StorablePicture *dec_picture = currSlice->dec_picture;
  int        b8, b4;
  int        yuv = dec_picture->chroma_format_idc - 1;
  int        blk_x, blk_y;
  int        pred, pred1;
  static const int block_pos[3][4][4]= //[yuv][b8][b4]
  {
    { {0, 1, 2, 3},{0, 0, 0, 0},{0, 0, 0, 0},{0, 0, 0, 0}},
    { {0, 1, 2, 3},{2, 3, 2, 3},{0, 0, 0, 0},{0, 0, 0, 0}},
    { {0, 1, 2, 3},{1, 1, 3, 3},{2, 3, 2, 3},{3, 3, 3, 3}}
  };

  PixelPos up;        //!< pixel position  p(0,-1)
  PixelPos left;      //!< pixel positions p(-1, -1..16)
  int up_avail, left_avail;
  imgpel **imgUV0 = dec_picture->imgUV[0];
  imgpel **imgUV1 = dec_picture->imgUV[1];
  imgpel **mb_pred0 = currSlice->mb_pred[0 + 1];
  imgpel **mb_pred1 = currSlice->mb_pred[1 + 1];


  getNonAffNeighbour(currMB, -1,  0, p_Vid->mb_size[IS_CHROMA], &left);
  getNonAffNeighbour(currMB,  0, -1, p_Vid->mb_size[IS_CHROMA], &up);

  if (!p_Vid->active_pps->constrained_intra_pred_flag) 
  {
    up_avail      = up.available;
    left_avail    = left.available;
  }
  else 
  {
    up_avail = up.available ? currSlice->intra_block[up.mb_addr] : 0;
    left_avail = left.available ? currSlice->intra_block[left.mb_addr]: 0;
  }

  // DC prediction
  // Note that unlike what is stated in many presentations and papers, this mode does not operate
  // the same way as I_16x16 DC prediction.
  for(b8 = 0; b8 < (p_Vid->num_uv_blocks) ;++b8) 
  {
    for (b4 = 0; b4 < 4; ++b4) 
    {
      blk_y = subblk_offset_y[yuv][b8][b4];
      blk_x = subblk_offset_x[yuv][b8][b4];
      pred  = p_Vid->dc_pred_value_comp[1];
      pred1 = p_Vid->dc_pred_value_comp[2];
      //===== get prediction value =====
      switch (block_pos[yuv][b8][b4])
      {
      case 0:  //===== TOP LEFT =====
        intra_chroma_DC_all   (imgUV0, up_avail, left_avail, up, left, blk_x, blk_y + 1, &pred);
        intra_chroma_DC_all   (imgUV1, up_avail, left_avail, up, left, blk_x, blk_y + 1, &pred1);
        break;
      case 1: //===== TOP RIGHT =====
        intra_chroma_DC_single(imgUV0, up_avail, left_avail, up, left, blk_x, blk_y + 1, &pred, 1);
        intra_chroma_DC_single(imgUV1, up_avail, left_avail, up, left, blk_x, blk_y + 1, &pred1, 1);
        break;
      case 2: //===== BOTTOM LEFT =====
        intra_chroma_DC_single(imgUV0, up_avail, left_avail, up, left, blk_x, blk_y + 1, &pred, 0);
        intra_chroma_DC_single(imgUV1, up_avail, left_avail, up, left, blk_x, blk_y + 1, &pred1, 0);
        break;
      case 3: //===== BOTTOM RIGHT =====
        intra_chroma_DC_all   (imgUV0, up_avail, left_avail, up, left, blk_x, blk_y + 1, &pred);
        intra_chroma_DC_all   (imgUV1, up_avail, left_avail, up, left, blk_x, blk_y + 1, &pred1);
        break;
      }

#if (IMGTYPE == 0)
      {
        int jj;
        for (jj = blk_y; jj < blk_y + BLOCK_SIZE; ++jj) 
        {
          memset(&mb_pred0[jj][blk_x],  pred, BLOCK_SIZE * sizeof(imgpel));
          memset(&mb_pred1[jj][blk_x], pred1, BLOCK_SIZE * sizeof(imgpel));
        }
      }
#else
      {
        int jj, ii;
        for (jj = blk_y; jj < blk_y + BLOCK_SIZE; ++jj) 
        {
          for (ii = blk_x; ii < blk_x + BLOCK_SIZE; ++ii) 
          {
            mb_pred0[jj][ii]=(imgpel) pred;
            mb_pred1[jj][ii]=(imgpel) pred1;
          }
        }
      }
#endif
    }
  }
}

static void intrapred_chroma_hor(Macroblock *currMB)
{
  VideoParameters *p_Vid = currMB->p_Vid;  
  PixelPos a;  //!< pixel positions p(-1, -1..16)
  int left_avail;
 
  getNonAffNeighbour(currMB, -1, 0, p_Vid->mb_size[IS_CHROMA], &a);
  
  if (!p_Vid->active_pps->constrained_intra_pred_flag)
    left_avail = a.available;
  else
    left_avail = a.available ? currMB->p_Slice->intra_block[a.mb_addr]: 0;
  // Horizontal Prediction
  if (!left_avail )
    error("unexpected HOR_PRED_8 chroma intra prediction mode",-1);
  else 
  {
    Slice *currSlice = currMB->p_Slice;
    int cr_MB_x = p_Vid->mb_cr_size_x;
    int cr_MB_y = p_Vid->mb_cr_size_y;

    int j;  
    StorablePicture *dec_picture = currSlice->dec_picture;
#if (IMGTYPE != 0)
    int i, pred, pred1;
#endif
    int pos_y = a.pos_y;
    int pos_x = a.pos_x;
    imgpel **mb_pred0 = currSlice->mb_pred[0 + 1];
    imgpel **mb_pred1 = currSlice->mb_pred[1 + 1];
    imgpel **i0 = &dec_picture->imgUV[0][pos_y];
    imgpel **i1 = &dec_picture->imgUV[1][pos_y];
    
    for (j = 0; j < cr_MB_y; ++j) 
    {
#if (IMGTYPE == 0)
      memset(mb_pred0[j], (*i0++)[pos_x], cr_MB_x * sizeof(imgpel));
      memset(mb_pred1[j], (*i1++)[pos_x], cr_MB_x * sizeof(imgpel));
#else
      pred  = (*i0++)[pos_x];
      pred1 = (*i1++)[pos_x];
      for (i = 0; i < cr_MB_x; ++i) 
      {
        mb_pred0[j][i]=(imgpel) pred;
        mb_pred1[j][i]=(imgpel) pred1;
      }
#endif

    }
  }
}

static void intrapred_chroma_ver(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  int j;
  StorablePicture *dec_picture = currSlice->dec_picture;

  PixelPos up;        //!< pixel position  p(0,-1)
  int up_avail;
  int cr_MB_x = p_Vid->mb_cr_size_x;
  int cr_MB_y = p_Vid->mb_cr_size_y;
  getNonAffNeighbour(currMB, 0, -1, p_Vid->mb_size[IS_CHROMA], &up);

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
      memcpy(mb_pred0[j], i0, cr_MB_x * sizeof(imgpel));
      memcpy(mb_pred1[j], i1, cr_MB_x * sizeof(imgpel));
    }
  }
}

static void intrapred_chroma_plane(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  StorablePicture *dec_picture = currSlice->dec_picture;

  PixelPos up;        //!< pixel position  p(0,-1)
  PixelPos up_left;
  PixelPos left;  //!< pixel positions p(-1, -1..16)
  int up_avail, left_avail, left_up_avail;

  getNonAffNeighbour(currMB, -1, -1, p_Vid->mb_size[IS_CHROMA], &up_left);
  getNonAffNeighbour(currMB, -1,  0, p_Vid->mb_size[IS_CHROMA], &left);
  getNonAffNeighbour(currMB,  0, -1, p_Vid->mb_size[IS_CHROMA], &up);

  if (!p_Vid->active_pps->constrained_intra_pred_flag) 
  {
    up_avail      = up.available;
    left_avail    = left.available;
    left_up_avail = up_left.available;
  }
  else 
  {
    up_avail      = up.available ? currSlice->intra_block[up.mb_addr] : 0;
    left_avail    = left.available ? currSlice->intra_block[left.mb_addr]: 0;
    left_up_avail = up_left.available ? currSlice->intra_block[up_left.mb_addr]: 0;
  }
  // plane prediction
  if (!left_up_avail || !left_avail || !up_avail)
    error("unexpected PLANE_8 chroma intra prediction mode",-1);
  else 
  {
    int cr_MB_x = p_Vid->mb_cr_size_x;
    int cr_MB_y = p_Vid->mb_cr_size_y;
    int cr_MB_y2 = (cr_MB_y >> 1);
    int cr_MB_x2 = (cr_MB_x >> 1);

    int i,j;
    int ih, iv, ib, ic, iaa;
    int uv;
    for (uv = 0; uv < 2; uv++) 
    {
      imgpel **imgUV = dec_picture->imgUV[uv];
      imgpel **mb_pred = currSlice->mb_pred[uv + 1];
      int max_imgpel_value = p_Vid->max_pel_value_comp[uv + 1];
      imgpel *upPred = &imgUV[up.pos_y][up.pos_x];
      int pos_x  = up_left.pos_x;
      int pos_y1 = left.pos_y + cr_MB_y2;
      int pos_y2 = pos_y1 - 2;
      //imgpel **predU1 = &imgUV[pos_y1];
      imgpel **predU2 = &imgUV[pos_y2];
      ih = cr_MB_x2 * (upPred[cr_MB_x - 1] - imgUV[up_left.pos_y][pos_x]);

      for (i = 0; i < cr_MB_x2 - 1; ++i)
        ih += (i + 1) * (upPred[cr_MB_x2 + i] - upPred[cr_MB_x2 - 2 - i]);

      iv = cr_MB_y2 * (imgUV[left.pos_y + cr_MB_y - 1][pos_x] - imgUV[up_left.pos_y][pos_x]);
           
      for (i = 0; i < cr_MB_y2 - 1; ++i)
      {
        iv += (i + 1)*(*(imgUV[pos_y1++] + pos_x) - *((*predU2--) + pos_x));
      }

      ib= ((cr_MB_x == 8 ? 17 : 5) * ih + 2 * cr_MB_x)>>(cr_MB_x == 8 ? 5 : 6);
      ic= ((cr_MB_y == 8 ? 17 : 5) * iv + 2 * cr_MB_y)>>(cr_MB_y == 8 ? 5 : 6);

      iaa = ((imgUV[pos_y1][pos_x] + upPred[cr_MB_x-1]) << 4);

      for (j = 0; j < cr_MB_y; ++j)
      {
        int plane = iaa + (j - cr_MB_y2 + 1) * ic + 16 - (cr_MB_x2 - 1) * ib;

        for (i = 0; i < cr_MB_x; ++i)
          mb_pred[j][i]=(imgpel) iClip1(max_imgpel_value, ((i * ib + plane) >> 5));
      }
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Chroma Intra prediction. Note that many operations can be moved
 *    outside since they are repeated for both components for no reason.
 ************************************************************************
 */
void intra_pred_chroma(Macroblock *currMB)
{
  switch (currMB->c_ipred_mode) 
  {
  case DC_PRED_8:  
    intrapred_chroma_dc(currMB);
    break;
  case HOR_PRED_8: 
    intrapred_chroma_hor(currMB);
    break;
  case VERT_PRED_8: 
    intrapred_chroma_ver(currMB);
    break;
  case PLANE_8: 
    intrapred_chroma_plane(currMB);
    break;
  default:
    error("illegal chroma intra prediction mode", 600);
    break;
  }
}

