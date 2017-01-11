/*!
 *************************************************************************************
 * \file intra16x16_pred.c
 *
 * \brief
 *    Functions for intra 16x16 prediction
 *
 * \author
 *      Main contributors (see contributors.h for copyright, 
 *                         address and affiliation details)
 *      - Yuri Vatis
 *      - Jan Muenster
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 *
 *************************************************************************************
 */
#include "global.h"
#include "intra16x16_pred.h"
#include "mb_access.h"
#include "image.h"

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 16x16 DC prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intraprediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra16x16_dc_pred(Macroblock *currMB, ColorPlane pl)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  int s0 = 0, s1 = 0, s2 = 0;

  int i,j;

  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;
  imgpel **mb_pred = &(currSlice->mb_pred[pl][0]); 

  PixelPos a, b; 

  int up_avail, left_avail;

  getNonAffNeighbour(currMB,   -1,   0, p_Vid->mb_size[IS_LUMA], &a);
  getNonAffNeighbour(currMB,    0,  -1, p_Vid->mb_size[IS_LUMA], &b);

  if (!p_Vid->active_pps->constrained_intra_pred_flag)
  {
    up_avail      = b.available;
    left_avail    = a.available;    
  }
  else
  {
    up_avail      = b.available ? currSlice->intra_block[b.mb_addr] : 0;
    left_avail    = a.available ? currSlice->intra_block[a.mb_addr]: 0;
  }

  // Sum top predictors
  if (up_avail)
  {
    imgpel *pel = &imgY[b.pos_y][b.pos_x];
    for (i = 0; i < MB_BLOCK_SIZE; ++i)
    {
      s1 += *pel++;
    }
  }

  // Sum left predictors
  if (left_avail)
  {
    int pos_y = a.pos_y;
    int pos_x = a.pos_x;
    for (i = 0; i < MB_BLOCK_SIZE; ++i)
    {
      s2 += imgY[pos_y++][pos_x]; 
    }
  }

  if (up_avail && left_avail)
    s0 = (s1 + s2 + 16)>>5;       // no edge
  else if (!up_avail && left_avail)
    s0 = (s2 + 8)>>4;              // upper edge
  else if (up_avail && !left_avail)
    s0 = (s1 + 8)>>4;              // left edge
  else
    s0 = p_Vid->dc_pred_value_comp[pl];                            // top left corner, nothing to predict from

  for(j = 0; j < MB_BLOCK_SIZE; ++j)
  {
#if (IMGTYPE == 0)
    memset(mb_pred[j], s0, MB_BLOCK_SIZE * sizeof(imgpel));
#else
    for(i = 0; i < MB_BLOCK_SIZE; i += 4)
    {
      mb_pred[j][i    ]=(imgpel) s0;
      mb_pred[j][i + 1]=(imgpel) s0;
      mb_pred[j][i + 2]=(imgpel) s0;
      mb_pred[j][i + 3]=(imgpel) s0;
    }
#endif
  }

  return DECODING_OK;

}


/*!
 ***********************************************************************
 * \brief
 *    makes and returns 16x16 vertical prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intraprediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra16x16_vert_pred(Macroblock *currMB, ColorPlane pl)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int j;

  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;

  PixelPos b;          //!< pixel position p(0,-1)

  int up_avail;

  //getNonAffNeighbour(currMB,    0,   -1, p_Vid->mb_size[IS_LUMA], &b);
  p_Vid->getNeighbour(currMB,    0,   -1, p_Vid->mb_size[IS_LUMA], &b);

  if (!p_Vid->active_pps->constrained_intra_pred_flag)
  {
    up_avail = b.available;
  }
  else
  {
    up_avail = b.available ? currSlice->intra_block[b.mb_addr] : 0;
  }

  if (!up_avail)
    error ("invalid 16x16 intra pred Mode VERT_PRED_16",500);
  {
    imgpel **prd = &currSlice->mb_pred[pl][0];
    imgpel *src = &(imgY[b.pos_y][b.pos_x]);

    for(j=0;j<MB_BLOCK_SIZE; j+= 4)
    {
      memcpy(*prd++, src, MB_BLOCK_SIZE * sizeof(imgpel));
      memcpy(*prd++, src, MB_BLOCK_SIZE * sizeof(imgpel));
      memcpy(*prd++, src, MB_BLOCK_SIZE * sizeof(imgpel));
      memcpy(*prd++, src, MB_BLOCK_SIZE * sizeof(imgpel));
    }
  }

  return DECODING_OK;
}


/*!
 ***********************************************************************
 * \brief
 *    makes and returns 16x16 horizontal prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intraprediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra16x16_hor_pred(Macroblock *currMB, ColorPlane pl)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
#if (IMGTYPE == 0)
  int j;
#else
  int i, j;
#endif

  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;
  imgpel **mb_pred = &(currSlice->mb_pred[pl][0]); 
  imgpel prediction;
  int pos_y, pos_x;

  PixelPos a;

  int left_avail;

  getNonAffNeighbour(currMB, -1,  0, p_Vid->mb_size[IS_LUMA], &a);

  if (!p_Vid->active_pps->constrained_intra_pred_flag)
  {
    left_avail    = a.available;
  }
  else
  {
    left_avail  = a.available ? currSlice->intra_block[a.mb_addr]: 0;
  }

  if (!left_avail)
    error ("invalid 16x16 intra pred Mode HOR_PRED_16",500);

  pos_y = a.pos_y;
  pos_x = a.pos_x;

  for(j = 0; j < MB_BLOCK_SIZE; ++j)
  {
    imgpel *prd = mb_pred[j];
    prediction = imgY[pos_y++][pos_x];
#if (IMGTYPE == 0)
    memset(prd, prediction, MB_BLOCK_SIZE * sizeof(imgpel));
#else
    for(i = 0; i < MB_BLOCK_SIZE; i += 4)
    {
      *prd++= prediction; // store predicted 16x16 block
      *prd++= prediction; // store predicted 16x16 block
      *prd++= prediction; // store predicted 16x16 block
      *prd++= prediction; // store predicted 16x16 block
    }
#endif
  }

  return DECODING_OK;
}


/*!
 ***********************************************************************
 * \brief
 *    makes and returns 16x16 horizontal prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intraprediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra16x16_plane_pred(Macroblock *currMB, ColorPlane pl)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i,j;

  int ih = 0, iv = 0;
  int ib,ic,iaa;

  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;
  imgpel **mb_pred = &(currSlice->mb_pred[pl][0]); 
  imgpel *mpr_line;
  int max_imgpel_value = p_Vid->max_pel_value_comp[pl];
  int pos_y, pos_x;

  PixelPos a, b, d;  

  int up_avail, left_avail, left_up_avail;

  getNonAffNeighbour(currMB, -1,  -1, p_Vid->mb_size[IS_LUMA], &d);
  getNonAffNeighbour(currMB, -1,   0, p_Vid->mb_size[IS_LUMA], &a);
  getNonAffNeighbour(currMB,  0,  -1, p_Vid->mb_size[IS_LUMA], &b);

  if (!p_Vid->active_pps->constrained_intra_pred_flag)
  {
    up_avail      = b.available;
    left_avail    = a.available;
    left_up_avail = d.available;
  }
  else
  {
    up_avail      = b.available ? currSlice->intra_block[b.mb_addr] : 0;
    left_avail    = a.available ? currSlice->intra_block[a.mb_addr] : 0;
    left_up_avail = d.available ? currSlice->intra_block[d.mb_addr] : 0;
  }

  if (!up_avail || !left_up_avail  || !left_avail)
    error ("invalid 16x16 intra pred Mode PLANE_16",500);

  mpr_line = &imgY[b.pos_y][b.pos_x+7];
  pos_y = a.pos_y + 7;
  pos_x = a.pos_x;
  for (i = 1; i < 8; ++i)
  {
    ih += i*(mpr_line[i] - mpr_line[-i]);
    iv += i*(imgY[pos_y + i][pos_x] - imgY[pos_y - i][pos_x]);
  }

  ih += 8*(mpr_line[8] - imgY[d.pos_y][d.pos_x]);
  iv += 8*(imgY[pos_y + 8][pos_x] - imgY[d.pos_y][d.pos_x]);

  ib=(5 * ih + 32)>>6;
  ic=(5 * iv + 32)>>6;

  iaa=16 * (mpr_line[8] + imgY[pos_y + 8][pos_x]);
  for (j = 0;j < MB_BLOCK_SIZE; ++j)
  {
    int ibb = iaa + (j - 7) * ic + 16;
    imgpel *prd = mb_pred[j];
    for (i = 0;i < MB_BLOCK_SIZE; i += 4)
    {
      *prd++ = (imgpel) iClip1(max_imgpel_value, ((ibb + (i - 7) * ib) >> 5));
      *prd++ = (imgpel) iClip1(max_imgpel_value, ((ibb + (i - 6) * ib) >> 5));
      *prd++ = (imgpel) iClip1(max_imgpel_value, ((ibb + (i - 5) * ib) >> 5));
      *prd++ = (imgpel) iClip1(max_imgpel_value, ((ibb + (i - 4) * ib) >> 5));
    }
  }// store plane prediction

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 16x16 intra prediction blocks 
 *
 * \return
 *    DECODING_OK   decoding of intraprediction mode was successful            \n
 *    SEARCH_SYNC   search next sync element as errors while decoding occured
 ***********************************************************************
 */
int intrapred_16x16_normal(Macroblock *currMB,  //!< Current Macroblock
                           ColorPlane pl,       //!< Current colorplane (for 4:4:4)                         
                           int predmode)        //!< prediction mode
{
  switch (predmode)
  {
  case VERT_PRED_16:                       // vertical prediction from block above
    return (intra16x16_vert_pred(currMB, pl));
    break;
  case HOR_PRED_16:                        // horizontal prediction from left block
    return (intra16x16_hor_pred(currMB, pl));
    break;
  case DC_PRED_16:                         // DC prediction
    return (intra16x16_dc_pred(currMB, pl));
    break;
  case PLANE_16:// 16 bit integer plan pred
    return (intra16x16_plane_pred(currMB, pl));
    break;
  default:
    {                                    // indication of fault in bitstream,exit
      printf("illegal 16x16 intra prediction mode input: %d\n",predmode);
      return SEARCH_SYNC;
    }
  }
}

