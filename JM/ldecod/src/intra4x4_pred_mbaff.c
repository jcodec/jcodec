/*!
 *************************************************************************************
 * \file intra4x4_pred_mbaff.c
 *
 * \brief
 *    Functions for intra 4x4 prediction
 *
 * \author
 *      Main contributors (see contributors.h for copyright, 
 *                         address and affiliation details)
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 *
 *************************************************************************************
 */
#include "global.h"
#include "intra4x4_pred.h"
#include "mb_access.h"
#include "image.h"

// Notation for comments regarding prediction and predictors.
// The pels of the 4x4 block are labelled a..p. The predictor pels above
// are labelled A..H, from the left I..L, and from above left X, as follows:
//
//  X A B C D E F G H
//  I a b c d
//  J e f g h
//  K i j k l
//  L m n o p
//

// Predictor array index definitions
#define P_X (PredPel[0])
#define P_A (PredPel[1])
#define P_B (PredPel[2])
#define P_C (PredPel[3])
#define P_D (PredPel[4])
#define P_E (PredPel[5])
#define P_F (PredPel[6])
#define P_G (PredPel[7])
#define P_H (PredPel[8])
#define P_I (PredPel[9])
#define P_J (PredPel[10])
#define P_K (PredPel[11])
#define P_L (PredPel[12])

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 DC prediction mode
 *
 * \param currMB
 *    current MB structure
 * \param pl
 *    color plane
 * \param ioff
 *    pixel offset X within MB
 * \param joff
 *    pixel offset Y within MB
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra4x4_dc_pred_mbaff(Macroblock *currMB, 
                                   ColorPlane pl,               
                                   int ioff,
                                   int joff)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;  

  int i,j;
  int s0 = 0;  
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;

  PixelPos pix_a[4], pix_b;

  int block_available_up;
  int block_available_left;  

  imgpel **mb_pred = currSlice->mb_pred[pl];    

  for (i=0;i<4;++i)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, p_Vid->mb_size[IS_LUMA], &pix_a[i]);
  }
  getAffNeighbour(currMB, ioff    , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_b);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<4;++i)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
  }

  // form predictor pels
  if (block_available_up)
  {
    s0 += imgY[pix_b.pos_y][pix_b.pos_x + 0];
    s0 += imgY[pix_b.pos_y][pix_b.pos_x + 1];
    s0 += imgY[pix_b.pos_y][pix_b.pos_x + 2];
    s0 += imgY[pix_b.pos_y][pix_b.pos_x + 3];
  }

  if (block_available_left)
  {
    s0 += imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    s0 += imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    s0 += imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    s0 += imgY[pix_a[3].pos_y][pix_a[3].pos_x];
  }

  if (block_available_up && block_available_left)
  {
    // no edge
    s0 = (s0 + 4)>>3;
  }
  else if (!block_available_up && block_available_left)
  {
    // upper edge
    s0 = (s0 + 2)>>2;
  }
  else if (block_available_up && !block_available_left)
  {
    // left edge
    s0 = (s0 + 2)>>2;
  }
  else //if (!block_available_up && !block_available_left)
  {
    // top left corner, nothing to predict from
    s0 = p_Vid->dc_pred_value_comp[pl];
  }

  for (j=joff; j < joff + BLOCK_SIZE; ++j)
  {
    // store DC prediction
    mb_pred[j][ioff    ] = (imgpel) s0;
    mb_pred[j][ioff + 1] = (imgpel) s0;
    mb_pred[j][ioff + 2] = (imgpel) s0;
    mb_pred[j][ioff + 3] = (imgpel) s0;
  }
  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 vertical prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra4x4_vert_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                     ColorPlane pl,         //!< current image plane
                                     int ioff,              //!< pixel offset X within MB
                                     int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
   
  int block_available_up;
  PixelPos pix_b;

  getAffNeighbour(currMB, ioff, joff - 1 , p_Vid->mb_size[IS_LUMA], &pix_b);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    block_available_up = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
  }
  else
  {
    block_available_up = pix_b.available;
  }

  if (!block_available_up)
  {
    printf ("warning: Intra_4x4_Vertical prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);
  }
  else
  {
    imgpel **mb_pred = currSlice->mb_pred[pl];
    imgpel *imgY = (pl) ? &currSlice->dec_picture->imgUV[pl - 1][pix_b.pos_y][pix_b.pos_x] : &currSlice->dec_picture->imgY[pix_b.pos_y][pix_b.pos_x];
    memcpy(&(mb_pred[joff++][ioff]), imgY, BLOCK_SIZE * sizeof(imgpel));
    memcpy(&(mb_pred[joff++][ioff]), imgY, BLOCK_SIZE * sizeof(imgpel));
    memcpy(&(mb_pred[joff++][ioff]), imgY, BLOCK_SIZE * sizeof(imgpel));
    memcpy(&(mb_pred[joff  ][ioff]), imgY, BLOCK_SIZE * sizeof(imgpel));
  }
  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 horizontal prediction mode
 *
 * \param currMB
 *    current MB structure
 * \param pl
 *    color plane
 * \param ioff
 *    pixel offset X within MB
 * \param joff
 *    pixel offset Y within MB
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful
 *
 ***********************************************************************
 */
static int intra4x4_hor_pred_mbaff(Macroblock *currMB, 
                                    ColorPlane pl,               
                                    int ioff,
                                    int joff)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  Slice *currSlice = currMB->p_Slice;

  int i,j;
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;

  PixelPos pix_a[4];

  int block_available_left;

  imgpel *predrow, prediction, **mb_pred = currSlice->mb_pred[pl];    

  for (i=0;i<4;++i)
  {
    getAffNeighbour(currMB, ioff -1 , joff +i , p_Vid->mb_size[IS_LUMA], &pix_a[i]);
  }

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<4;++i)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
  }

  if (!block_available_left)
    printf ("warning: Intra_4x4_Horizontal prediction mode not allowed at mb %d\n",(int) currSlice->current_mb_nr);

  for(j=0;j<BLOCK_SIZE;++j)
  {
    predrow = mb_pred[j+joff];
    prediction = imgY[pix_a[j].pos_y][pix_a[j].pos_x];
    for(i = ioff;i < ioff + BLOCK_SIZE;++i)
      predrow[i]= prediction; /* store predicted 4x4 block */
  }

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 diagonal down right prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra4x4_diag_down_right_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                                      ColorPlane pl,         //!< current image plane
                                                      int ioff,              //!< pixel offset X within MB
                                                      int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i;
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;

  PixelPos pix_a[4];
  PixelPos pix_b, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;

  imgpel **mb_pred = currSlice->mb_pred[pl];    

  for (i=0;i<4;++i)
  {
    getAffNeighbour(currMB, ioff -1 , joff +i , p_Vid->mb_size[IS_LUMA], &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_b);
  getAffNeighbour(currMB, ioff -1 , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_d);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<4;++i)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_left  = pix_d.available;
  }

  if ((!block_available_up)||(!block_available_left)||(!block_available_up_left))
    printf ("warning: Intra_4x4_Diagonal_Down_Right prediction mode not allowed at mb %d\n",(int) currSlice->current_mb_nr);
  else
  {
    imgpel PredPixel[7];
    imgpel PredPel[13];
    imgpel *pred_pel = &imgY[pix_b.pos_y][pix_b.pos_x];
    // form predictor pels
    // P_A through P_D
    memcpy(&PredPel[1], pred_pel, BLOCK_SIZE * sizeof(imgpel));


    P_I = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_J = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_K = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_L = imgY[pix_a[3].pos_y][pix_a[3].pos_x];

    P_X = imgY[pix_d.pos_y][pix_d.pos_x];
    
    PredPixel[0] = (imgpel) ((P_L + (P_K << 1) + P_J + 2) >> 2);
    PredPixel[1] = (imgpel) ((P_K + (P_J << 1) + P_I + 2) >> 2);
    PredPixel[2] = (imgpel) ((P_J + (P_I << 1) + P_X + 2) >> 2);
    PredPixel[3] = (imgpel) ((P_I + (P_X << 1) + P_A + 2) >> 2);
    PredPixel[4] = (imgpel) ((P_X + 2*P_A + P_B + 2) >> 2);
    PredPixel[5] = (imgpel) ((P_A + 2*P_B + P_C + 2) >> 2);
    PredPixel[6] = (imgpel) ((P_B + 2*P_C + P_D + 2) >> 2);

    memcpy(&mb_pred[joff++][ioff], &PredPixel[3], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[2], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[1], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff  ][ioff], &PredPixel[0], 4 * sizeof(imgpel));
  }

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 diagonal down left prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra4x4_diag_down_left_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                        ColorPlane pl,         //!< current image plane
                                        int ioff,              //!< pixel offset X within MB
                                        int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  PixelPos pix_b, pix_c;

  int block_available_up;
  int block_available_up_right;

  getAffNeighbour(currMB, ioff    , joff - 1, p_Vid->mb_size[IS_LUMA], &pix_b);
  getAffNeighbour(currMB, ioff + 4, joff - 1, p_Vid->mb_size[IS_LUMA], &pix_c);

  pix_c.available = pix_c.available && !((ioff==4) && ((joff==4)||(joff==12)));

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
  }
  else
  {
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
  }

  if (!block_available_up)
    printf ("warning: Intra_4x4_Diagonal_Down_Left prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);
  else
  {
    imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;
    imgpel **mb_pred = currSlice->mb_pred[pl];    

    imgpel PredPixel[8];
    imgpel PredPel[25];
    imgpel *pred_pel = &imgY[pix_b.pos_y][pix_b.pos_x];

    // form predictor pels
    // P_A through P_D
    memcpy(&PredPel[1], pred_pel, BLOCK_SIZE * sizeof(imgpel));

    // P_E through P_H
    if (block_available_up_right)
    {
      memcpy(&PredPel[5], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE * sizeof(imgpel));
    }
    else
    {
      P_E = P_F = P_G = P_H = P_D;
    }

    PredPixel[0] = (imgpel) ((P_A + P_C + 2*(P_B) + 2) >> 2);
    PredPixel[1] = (imgpel) ((P_B + P_D + 2*(P_C) + 2) >> 2);
    PredPixel[2] = (imgpel) ((P_C + P_E + 2*(P_D) + 2) >> 2);
    PredPixel[3] = (imgpel) ((P_D + P_F + 2*(P_E) + 2) >> 2);
    PredPixel[4] = (imgpel) ((P_E + P_G + 2*(P_F) + 2) >> 2);
    PredPixel[5] = (imgpel) ((P_F + P_H + 2*(P_G) + 2) >> 2);
    PredPixel[6] = (imgpel) ((P_G + 3*(P_H) + 2) >> 2);

    memcpy(&mb_pred[joff++][ioff], &PredPixel[0], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[1], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[2], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff  ][ioff], &PredPixel[3], 4 * sizeof(imgpel));
  }

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 vertical right prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra4x4_vert_right_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                          ColorPlane pl,         //!< current image plane
                                          int ioff,              //!< pixel offset X within MB
                                          int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i;
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;

  PixelPos pix_a[4];
  PixelPos pix_b, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;

  imgpel **mb_pred = currSlice->mb_pred[pl];    

  for (i=0;i<4;++i)
  {
    getAffNeighbour(currMB, ioff -1 , joff +i , p_Vid->mb_size[IS_LUMA], &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_b);
  getAffNeighbour(currMB, ioff -1 , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_d);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<4;++i)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_left  = pix_d.available;
  }

  if ((!block_available_up)||(!block_available_left)||(!block_available_up_left))
    printf ("warning: Intra_4x4_Vertical_Right prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);
  {
    imgpel PredPixel[10];
    imgpel PredPel[13];
    imgpel *pred_pel = &imgY[pix_b.pos_y][pix_b.pos_x];
    // form predictor pels
    // P_A through P_D
    memcpy(&PredPel[1], pred_pel, BLOCK_SIZE * sizeof(imgpel));


    P_I = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_J = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_K = imgY[pix_a[2].pos_y][pix_a[2].pos_x];

    P_X = imgY[pix_d.pos_y][pix_d.pos_x];
    
    PredPixel[0] = (imgpel) ((P_X + (P_I << 1) + P_J + 2) >> 2);
    PredPixel[1] = (imgpel) ((P_X + P_A + 1) >> 1);
    PredPixel[2] = (imgpel) ((P_A + P_B + 1) >> 1);
    PredPixel[3] = (imgpel) ((P_B + P_C + 1) >> 1);
    PredPixel[4] = (imgpel) ((P_C + P_D + 1) >> 1);
    PredPixel[5] = (imgpel) ((P_I + (P_J << 1) + P_K + 2) >> 2);
    PredPixel[6] = (imgpel) ((P_I + (P_X << 1) + P_A + 2) >> 2);
    PredPixel[7] = (imgpel) ((P_X + 2*P_A + P_B + 2) >> 2);
    PredPixel[8] = (imgpel) ((P_A + 2*P_B + P_C + 2) >> 2);
    PredPixel[9] = (imgpel) ((P_B + 2*P_C + P_D + 2) >> 2);

    memcpy(&mb_pred[joff++][ioff], &PredPixel[1], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[6], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[0], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff  ][ioff], &PredPixel[5], 4 * sizeof(imgpel));    
    
  }

  return DECODING_OK;
}


/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 vertical left prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra4x4_vert_left_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                          ColorPlane pl,         //!< current image plane
                                          int ioff,              //!< pixel offset X within MB
                                          int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  PixelPos pix_b, pix_c;

  int block_available_up;
  int block_available_up_right;

  getAffNeighbour(currMB, ioff    , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_b);
  getAffNeighbour(currMB, ioff +4 , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_c);

  pix_c.available = pix_c.available && !((ioff==4) && ((joff==4)||(joff==12)));
  
  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
  }
  else
  {
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
  }


  if (!block_available_up)
    printf ("warning: Intra_4x4_Vertical_Left prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);
  {
    imgpel PredPixel[10];
    imgpel PredPel[13];
    imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;
    imgpel **mb_pred = currSlice->mb_pred[pl];    
    imgpel *pred_pel = &imgY[pix_b.pos_y][pix_b.pos_x];

    // form predictor pels
    // P_A through P_D
    memcpy(&PredPel[1], pred_pel, BLOCK_SIZE * sizeof(imgpel));

    // P_E through P_H
    if (block_available_up_right)
    {
      memcpy(&PredPel[5], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE * sizeof(imgpel));
    }
    else
    {
      P_E = P_F = P_G = P_H = P_D;
    }

    PredPixel[0] = (imgpel) ((P_A + P_B + 1) >> 1);
    PredPixel[1] = (imgpel) ((P_B + P_C + 1) >> 1);
    PredPixel[2] = (imgpel) ((P_C + P_D + 1) >> 1);
    PredPixel[3] = (imgpel) ((P_D + P_E + 1) >> 1);
    PredPixel[4] = (imgpel) ((P_E + P_F + 1) >> 1);
    PredPixel[5] = (imgpel) ((P_A + 2*P_B + P_C + 2) >> 2);
    PredPixel[6] = (imgpel) ((P_B + 2*P_C + P_D + 2) >> 2);
    PredPixel[7] = (imgpel) ((P_C + 2*P_D + P_E + 2) >> 2);
    PredPixel[8] = (imgpel) ((P_D + 2*P_E + P_F + 2) >> 2);
    PredPixel[9] = (imgpel) ((P_E + 2*P_F + P_G + 2) >> 2);

    memcpy(&mb_pred[joff++][ioff], &PredPixel[0], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[5], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[1], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff  ][ioff], &PredPixel[6], 4 * sizeof(imgpel));
  }
  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 horizontal up prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra4x4_hor_up_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                      ColorPlane pl,         //!< current image plane
                                      int ioff,              //!< pixel offset X within MB
                                      int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i;
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;

  PixelPos pix_a[4];

  int block_available_left;

  imgpel **mb_pred = currSlice->mb_pred[pl];    

  for (i=0;i<4;++i)
  {
    getAffNeighbour(currMB, ioff -1 , joff +i , p_Vid->mb_size[IS_LUMA], &pix_a[i]);
  }

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<4;++i)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
  }

  if (!block_available_left)
    printf ("warning: Intra_4x4_Horizontal_Up prediction mode not allowed at mb %d\n",(int) currSlice->current_mb_nr);
  else
  {
    imgpel PredPixel[10];
    imgpel PredPel[13];

    // form predictor pels
    P_I = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_J = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_K = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_L = imgY[pix_a[3].pos_y][pix_a[3].pos_x];

    PredPixel[0] = (imgpel) ((P_I + P_J + 1) >> 1);
    PredPixel[1] = (imgpel) ((P_I + (P_J << 1) + P_K + 2) >> 2);
    PredPixel[2] = (imgpel) ((P_J + P_K + 1) >> 1);
    PredPixel[3] = (imgpel) ((P_J + (P_K << 1) + P_L + 2) >> 2);
    PredPixel[4] = (imgpel) ((P_K + P_L + 1) >> 1);
    PredPixel[5] = (imgpel) ((P_K + (P_L << 1) + P_L + 2) >> 2);
    PredPixel[6] = (imgpel) P_L;
    PredPixel[7] = (imgpel) P_L;
    PredPixel[8] = (imgpel) P_L;
    PredPixel[9] = (imgpel) P_L;

    memcpy(&mb_pred[joff++][ioff], &PredPixel[0], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[2], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[4], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[6], 4 * sizeof(imgpel));
  }

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 horizontal down prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static int intra4x4_hor_down_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                         ColorPlane pl,         //!< current image plane
                                         int ioff,              //!< pixel offset X within MB
                                         int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i; 
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY;

  PixelPos pix_a[4];
  PixelPos pix_b, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;

  imgpel **mb_pred = currSlice->mb_pred[pl];    
  
  for (i=0;i<4;++i)
  {
    getAffNeighbour(currMB, ioff -1 , joff +i , p_Vid->mb_size[IS_LUMA], &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_b);
  getAffNeighbour(currMB, ioff -1 , joff -1 , p_Vid->mb_size[IS_LUMA], &pix_d);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<4;++i)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_left  = pix_d.available;
  }

  if ((!block_available_up)||(!block_available_left)||(!block_available_up_left))
    printf ("warning: Intra_4x4_Horizontal_Down prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);
  else
  {
    imgpel PredPixel[10];
    imgpel PredPel[13];
    imgpel *pred_pel = &imgY[pix_b.pos_y][pix_b.pos_x];

    // form predictor pels
    // P_A through P_D
    memcpy(&PredPel[1], pred_pel, BLOCK_SIZE * sizeof(imgpel));

    P_I = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_J = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_K = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_L = imgY[pix_a[3].pos_y][pix_a[3].pos_x];

    P_X = imgY[pix_d.pos_y][pix_d.pos_x];

    PredPixel[0] = (imgpel) ((P_K + P_L + 1) >> 1);
    PredPixel[1] = (imgpel) ((P_J + (P_K << 1) + P_L + 2) >> 2);
    PredPixel[2] = (imgpel) ((P_J + P_K + 1) >> 1);
    PredPixel[3] = (imgpel) ((P_I + (P_J << 1) + P_K + 2) >> 2);
    PredPixel[4] = (imgpel) ((P_I + P_J + 1) >> 1);
    PredPixel[5] = (imgpel) ((P_X + (P_I << 1) + P_J + 2) >> 2);
    PredPixel[6] = (imgpel) ((P_X + P_I + 1) >> 1);
    PredPixel[7] = (imgpel) ((P_I + (P_X << 1) + P_A + 2) >> 2);
    PredPixel[8] = (imgpel) ((P_X + 2*P_A + P_B + 2) >> 2);
    PredPixel[9] = (imgpel) ((P_A + 2*P_B + P_C + 2) >> 2);

    memcpy(&mb_pred[joff++][ioff], &PredPixel[6], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[4], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff++][ioff], &PredPixel[2], 4 * sizeof(imgpel));
    memcpy(&mb_pred[joff  ][ioff], &PredPixel[0], 4 * sizeof(imgpel));
  }

  return DECODING_OK;
}


/*!
 ***********************************************************************
 * \brief
 *    makes and returns 4x4 intra prediction blocks 
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *    SEARCH_SYNC   search next sync element as errors while decoding occured
 ***********************************************************************
 */
int intra_pred_4x4_mbaff(Macroblock *currMB,    //!< current macroblock
                        ColorPlane pl,         //!< current image plane
                        int ioff,              //!< pixel offset X within MB
                        int joff,              //!< pixel offset Y within MB
                        int img_block_x,       //!< location of block X, multiples of 4
                        int img_block_y)       //!< location of block Y, multiples of 4
{
  VideoParameters *p_Vid = currMB->p_Vid;
  byte predmode = p_Vid->ipredmode[img_block_y][img_block_x];
  currMB->ipmode_DPCM = predmode; //For residual DPCM

  switch (predmode)
  {
  case DC_PRED:
    return (intra4x4_dc_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case VERT_PRED:
    return (intra4x4_vert_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case HOR_PRED:
    return (intra4x4_hor_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case DIAG_DOWN_RIGHT_PRED:
    return (intra4x4_diag_down_right_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case DIAG_DOWN_LEFT_PRED:
    return (intra4x4_diag_down_left_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case VERT_RIGHT_PRED:
    return (intra4x4_vert_right_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case VERT_LEFT_PRED:
    return (intra4x4_vert_left_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case HOR_UP_PRED:
    return (intra4x4_hor_up_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case HOR_DOWN_PRED:  
    return (intra4x4_hor_down_pred_mbaff(currMB, pl, ioff, joff));
  default:
    printf("Error: illegal intra_4x4 prediction mode: %d\n", (int) predmode);
    return SEARCH_SYNC;
    break;
  }
}
