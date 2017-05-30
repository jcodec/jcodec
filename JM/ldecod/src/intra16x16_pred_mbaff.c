/*!
 *************************************************************************************
 * \file intra16x16_pred_mbaff.c
 *
 * \brief
 *    Functions for intra 16x16 prediction (MBAFF)
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
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra16x16_dc_pred_mbaff(Macroblock *currMB, ColorPlane pl)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  int s0 = 0, s1 = 0, s2 = 0;

  int i,j;

  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;
  imgpel **mb_pred = &(currSlice->mb_pred[pl][0]); 

  PixelPos b;          //!< pixel position p(0,-1)
  PixelPos left[17];    //!< pixel positions p(-1, -1..15)

  int up_avail, left_avail;

  s1=s2=0;

  for (i=0;i<17;++i)
  {
    getAffNeighbour(currMB, -1,  i-1, p_Vid->mb_size[IS_LUMA], &left[i]);
  }
  getAffNeighbour(currMB,    0,   -1, p_Vid->mb_size[IS_LUMA], &b);

  if (!p_Vid->active_pps->constrained_intra_pred_flag)
  {
    up_avail      = b.available;
    left_avail    = left[1].available;
  }
  else
  {
    up_avail      = b.available ? currSlice->intra_block[b.mb_addr] : 0;
    for (i = 1, left_avail = 1; i < 17; ++i)
      left_avail  &= left[i].available ? currSlice->intra_block[left[i].mb_addr]: 0;
  }

  for (i = 0; i < MB_BLOCK_SIZE; ++i)
  {
    if (up_avail)
      s1 += imgY[b.pos_y][b.pos_x+i];    // sum hor pix
    if (left_avail)
      s2 += imgY[left[i + 1].pos_y][left[i + 1].pos_x];    // sum vert pix
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
    for(i = 0; i < MB_BLOCK_SIZE; ++i)
    {
      mb_pred[j][i]=(imgpel) s0;
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
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra16x16_vert_pred_mbaff(Macroblock *currMB, ColorPlane pl)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int j;

  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;

  PixelPos b;          //!< pixel position p(0,-1)

  int up_avail;

  getAffNeighbour(currMB,    0,   -1, p_Vid->mb_size[IS_LUMA], &b);

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
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra16x16_hor_pred_mbaff(Macroblock *currMB, ColorPlane pl)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  int i,j;

  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;
  imgpel **mb_pred = &(currSlice->mb_pred[pl][0]); 
  imgpel prediction;

  PixelPos left[17];    //!< pixel positions p(-1, -1..15)

  int left_avail;

  for (i=0;i<17;++i)
  {
    getAffNeighbour(currMB, -1,  i-1, p_Vid->mb_size[IS_LUMA], &left[i]);
  }

  if (!p_Vid->active_pps->constrained_intra_pred_flag)
  {
    left_avail    = left[1].available;
  }
  else
  {
    for (i = 1, left_avail = 1; i < 17; ++i)
      left_avail  &= left[i].available ? currSlice->intra_block[left[i].mb_addr]: 0;
  }

  if (!left_avail)
    error ("invalid 16x16 intra pred Mode HOR_PRED_16",500);

  for(j = 0; j < MB_BLOCK_SIZE; ++j)
  {
    prediction = imgY[left[j+1].pos_y][left[j+1].pos_x];
    for(i = 0; i < MB_BLOCK_SIZE; ++i)
      mb_pred[j][i]= prediction; // store predicted 16x16 block
  }

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 16x16 horizontal prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra16x16_plane_pred_mbaff(Macroblock *currMB, ColorPlane pl)
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

  PixelPos b;          //!< pixel position p(0,-1)
  PixelPos left[17];    //!< pixel positions p(-1, -1..15)

  int up_avail, left_avail, left_up_avail;

  for (i=0;i<17; ++i)
  {
    getAffNeighbour(currMB, -1,  i-1, p_Vid->mb_size[IS_LUMA], &left[i]);
  }
  getAffNeighbour(currMB,    0,   -1, p_Vid->mb_size[IS_LUMA], &b);

  if (!p_Vid->active_pps->constrained_intra_pred_flag)
  {
    up_avail      = b.available;
    left_avail    = left[1].available;
    left_up_avail = left[0].available;
  }
  else
  {
    up_avail      = b.available ? currSlice->intra_block[b.mb_addr] : 0;
    for (i = 1, left_avail = 1; i < 17; ++i)
      left_avail  &= left[i].available ? currSlice->intra_block[left[i].mb_addr]: 0;
    left_up_avail = left[0].available ? currSlice->intra_block[left[0].mb_addr]: 0;
  }

  if (!up_avail || !left_up_avail  || !left_avail)
    error ("invalid 16x16 intra pred Mode PLANE_16",500);

  mpr_line = &imgY[b.pos_y][b.pos_x+7];
  for (i = 1; i < 8; ++i)
  {
    ih += i*(mpr_line[i] - mpr_line[-i]);
    iv += i*(imgY[left[8+i].pos_y][left[8+i].pos_x] - imgY[left[8-i].pos_y][left[8-i].pos_x]);
  }

  ih += 8*(mpr_line[8] - imgY[left[0].pos_y][left[0].pos_x]);
  iv += 8*(imgY[left[16].pos_y][left[16].pos_x] - imgY[left[0].pos_y][left[0].pos_x]);

  ib=(5 * ih + 32)>>6;
  ic=(5 * iv + 32)>>6;

  iaa=16 * (mpr_line[8] + imgY[left[16].pos_y][left[16].pos_x]);
  for (j = 0;j < MB_BLOCK_SIZE; ++j)
  {
    for (i = 0;i < MB_BLOCK_SIZE; ++i)
    {
      mb_pred[j][i] = (imgpel) iClip1(max_imgpel_value, ((iaa + (i - 7) * ib + (j - 7) * ic + 16) >> 5));
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
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *    SEARCH_SYNC   search next sync element as errors while decoding occured
 ***********************************************************************
 */
int intra_pred_16x16_mbaff(Macroblock *currMB,  //!< Current Macroblock
                          ColorPlane pl,       //!< Current colorplane (for 4:4:4)                         
                          int predmode)        //!< prediction mode
{
  switch (predmode)
  {
  case VERT_PRED_16:                       // vertical prediction from block above
    return (intra16x16_vert_pred_mbaff(currMB, pl));
    break;
  case HOR_PRED_16:                        // horizontal prediction from left block
    return (intra16x16_hor_pred_mbaff(currMB, pl));
    break;
  case DC_PRED_16:                         // DC prediction
    return (intra16x16_dc_pred_mbaff(currMB, pl));
    break;
  case PLANE_16:// 16 bit integer plan pred
    return (intra16x16_plane_pred_mbaff(currMB, pl));
    break;
  default:
    {                                    // indication of fault in bitstream,exit
      printf("illegal 16x16 intra prediction mode input: %d\n",predmode);
      return SEARCH_SYNC;
    }
  }  
}

