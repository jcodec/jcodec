/*!
 *************************************************************************************
 * \file intra8x8_pred.c
 *
 * \brief
 *    Functions for intra 8x8 prediction
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
#include "intra8x8_pred.h"
#include "mb_access.h"
#include "image.h"

// Notation for comments regarding prediction and predictors.
// The pels of the 8x8 block are labeled a..p. The predictor pels above
// are labeled A..H, from the left I..P, and from above left X, as follows:
//
//  Z  A  B  C  D  E  F  G  H  I  J  K  L  M   N  O  P
//  Q  a1 b1 c1 d1 e1 f1 g1 h1
//  R  a2 b2 c2 d2 e2 f2 g2 h2
//  S  a3 b3 c3 d3 e3 f3 g3 h3
//  T  a4 b4 c4 d4 e4 f4 g4 h4
//  U  a5 b5 c5 d5 e5 f5 g5 h5
//  V  a6 b6 c6 d6 e6 f6 g6 h6
//  W  a7 b7 c7 d7 e7 f7 g7 h7
//  X  a8 b8 c8 d8 e8 f8 g8 h8


// Predictor array index definitions
#define P_Z (PredPel[0])
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
#define P_M (PredPel[13])
#define P_N (PredPel[14])
#define P_O (PredPel[15])
#define P_P (PredPel[16])
#define P_Q (PredPel[17])
#define P_R (PredPel[18])
#define P_S (PredPel[19])
#define P_T (PredPel[20])
#define P_U (PredPel[21])
#define P_V (PredPel[22])
#define P_W (PredPel[23])
#define P_X (PredPel[24])

/*!
 *************************************************************************************
 * \brief
 *    Prefiltering for Intra8x8 prediction
 *************************************************************************************
 */
static inline void LowPassForIntra8x8Pred(imgpel *PredPel, int block_up_left, int block_up, int block_left)
{
  int i;
  imgpel LoopArray[25];

  memcpy(&LoopArray[0], &PredPel[0], 25 * sizeof(imgpel));

  if(block_up_left)
  {
    if(block_up && block_left)
    {
      LoopArray[0] = (imgpel) ((P_Q + (P_Z<<1) + P_A + 2)>>2);
    }
    else
    {
      if(block_up)
        LoopArray[0] = (imgpel) ((P_Z + (P_Z<<1) + P_A + 2)>>2);
      else if (block_left)
        LoopArray[0] = (imgpel) ((P_Z + (P_Z<<1) + P_Q + 2)>>2);
    }
  }
  
  if(block_up)
  {    
    if(block_up_left)
    {
      LoopArray[1] = (imgpel) ((PredPel[0] + (PredPel[1]<<1) + PredPel[2] + 2)>>2);
    }
    else
      LoopArray[1] = (imgpel) ((PredPel[1] + (PredPel[1]<<1) + PredPel[2] + 2)>>2);


    for(i = 2; i <16; i++)
    {
      LoopArray[i] = (imgpel) ((PredPel[i-1] + (PredPel[i]<<1) + PredPel[i+1] + 2)>>2);
    }
    LoopArray[16] = (imgpel) ((P_P + (P_P<<1) + P_O + 2)>>2);
  }

  if(block_left)
  {
    if(block_up_left)
      LoopArray[17] = (imgpel) ((P_Z + (P_Q<<1) + P_R + 2)>>2);
    else
      LoopArray[17] = (imgpel) ((P_Q + (P_Q<<1) + P_R + 2)>>2);

    for(i = 18; i <24; i++)
    {
      LoopArray[i] = (imgpel) ((PredPel[i-1] + (PredPel[i]<<1) + PredPel[i+1] + 2)>>2);
    }
    LoopArray[24] = (imgpel) ((P_W + (P_X<<1) + P_X + 2) >> 2);
  }

  memcpy(&PredPel[0], &LoopArray[0], 25 * sizeof(imgpel));
}

/*!
 *************************************************************************************
 * \brief
 *    Prefiltering for Intra8x8 prediction (Horizontal)
 *************************************************************************************
 */
static inline void LowPassForIntra8x8PredHor(imgpel *PredPel, int block_up_left, int block_up, int block_left)
{
  int i;
  imgpel LoopArray[25];

  memcpy(&LoopArray[0], &PredPel[0], 25 * sizeof(imgpel));

  if(block_up_left)
  {
    if(block_up && block_left)
    {
      LoopArray[0] = (imgpel) ((P_Q + (P_Z<<1) + P_A + 2)>>2);
    }
    else
    {
      if(block_up)
        LoopArray[0] = (imgpel) ((P_Z + (P_Z<<1) + P_A + 2)>>2);
      else if (block_left)
        LoopArray[0] = (imgpel) ((P_Z + (P_Z<<1) + P_Q + 2)>>2);
    }
  }
  
  if(block_up)
  {    
    if(block_up_left)
    {
      LoopArray[1] = (imgpel) ((PredPel[0] + (PredPel[1]<<1) + PredPel[2] + 2)>>2);
    }
    else
      LoopArray[1] = (imgpel) ((PredPel[1] + (PredPel[1]<<1) + PredPel[2] + 2)>>2);


    for(i = 2; i <16; i++)
    {
      LoopArray[i] = (imgpel) ((PredPel[i-1] + (PredPel[i]<<1) + PredPel[i+1] + 2)>>2);
    }
    LoopArray[16] = (imgpel) ((P_P + (P_P<<1) + P_O + 2)>>2);
  }


  memcpy(&PredPel[0], &LoopArray[0], 17 * sizeof(imgpel));
}

/*!
 *************************************************************************************
 * \brief
 *    Prefiltering for Intra8x8 prediction (Vertical)
 *************************************************************************************
 */
static inline void LowPassForIntra8x8PredVer(imgpel *PredPel, int block_up_left, int block_up, int block_left)
{
  // These functions need some cleanup and can be further optimized. 
  // For convenience, let us copy all data for now. It is obvious that the filtering makes things a bit more "complex"
  int i;
  imgpel LoopArray[25];

  memcpy(&LoopArray[0], &PredPel[0], 25 * sizeof(imgpel));

  if(block_up_left)
  {
    if(block_up && block_left)
    {
      LoopArray[0] = (imgpel) ((P_Q + (P_Z<<1) + P_A + 2)>>2);
    }
    else
    {
      if(block_up)
        LoopArray[0] = (imgpel) ((P_Z + (P_Z<<1) + P_A + 2)>>2);
      else if (block_left)
        LoopArray[0] = (imgpel) ((P_Z + (P_Z<<1) + P_Q + 2)>>2);
    }
  }
  
  if(block_left)
  {
    if(block_up_left)
      LoopArray[17] = (imgpel) ((P_Z + (P_Q<<1) + P_R + 2)>>2);
    else
      LoopArray[17] = (imgpel) ((P_Q + (P_Q<<1) + P_R + 2)>>2);

    for(i = 18; i <24; i++)
    {
      LoopArray[i] = (imgpel) ((PredPel[i-1] + (PredPel[i]<<1) + PredPel[i+1] + 2)>>2);
    }
    LoopArray[24] = (imgpel) ((P_W + (P_X<<1) + P_X + 2) >> 2);
  }

  memcpy(&PredPel[0], &LoopArray[0], 25 * sizeof(imgpel));
}


/*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 DC prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra_prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_dc_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                   ColorPlane pl,         //!< current image plane
                                   int ioff,              //!< pixel offset X within MB
                                   int joff)              //!< pixel offset Y within MB
{
  int i,j;
  int s0 = 0;
  imgpel PredPel[25];  // array of predictor pels
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  StorablePicture *dec_picture = currSlice->dec_picture;
  imgpel **imgY = (pl) ? dec_picture->imgUV[pl - 1] : dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;
  int block_available_up_right;
  

  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
    block_available_up_left  = pix_d.available;
  }

  // form predictor pels
  if (block_available_up)
  {
    memcpy(&PredPel[1], &imgY[pix_b.pos_y][pix_b.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[1], p_Vid->dc_pred_value_comp[pl], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_A = P_B = P_C = P_D = P_E = P_F = P_G = P_H = (imgpel) p_Vid->dc_pred_value_comp[pl];
#endif
  }

  if (block_available_up_right)
  {
    memcpy(&PredPel[9], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[9], PredPel[8], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_I = P_J = P_K = P_L = P_M = P_N = P_O = P_P = P_H;
#endif
  }

  if (block_available_left)
  {
    P_Q = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_R = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_S = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_T = imgY[pix_a[3].pos_y][pix_a[3].pos_x];
    P_U = imgY[pix_a[4].pos_y][pix_a[4].pos_x];
    P_V = imgY[pix_a[5].pos_y][pix_a[5].pos_x];
    P_W = imgY[pix_a[6].pos_y][pix_a[6].pos_x];
    P_X = imgY[pix_a[7].pos_y][pix_a[7].pos_x];
  }
  else
  {
    P_Q = P_R = P_S = P_T = P_U = P_V = P_W = P_X = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8Pred(PredPel, block_available_up_left, block_available_up, block_available_left);
  
  if (block_available_up && block_available_left)
  {
    // no edge
    s0 = (P_A + P_B + P_C + P_D + P_E + P_F + P_G + P_H + P_Q + P_R + P_S + P_T + P_U + P_V + P_W + P_X + 8) >> 4;
  }
  else if (!block_available_up && block_available_left)
  {
    // upper edge
    s0 = (P_Q + P_R + P_S + P_T + P_U + P_V + P_W + P_X + 4) >> 3;
  }
  else if (block_available_up && !block_available_left)
  {
    // left edge
    s0 = (P_A + P_B + P_C + P_D + P_E + P_F + P_G + P_H + 4) >> 3;
  }
  else //if (!block_available_up && !block_available_left)
  {
    // top left corner, nothing to predict from
    s0 = p_Vid->dc_pred_value_comp[pl];
  }

  for(j = joff; j < joff + BLOCK_SIZE_8x8; j++)
    for(i = ioff; i < ioff + BLOCK_SIZE_8x8; i++)
      mpr[j][i] = (imgpel) s0;

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 vertical prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra_prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_vert_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                     ColorPlane pl,         //!< current image plane
                                     int ioff,              //!< pixel offset X within MB
                                     int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i;
  imgpel PredPel[25];  // array of predictor pels  
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;
  int block_available_up_right;

  
  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
    block_available_up_left  = pix_d.available;
  }

  if (!block_available_up)
    printf ("warning: Intra_8x8_Vertical prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);

  // form predictor pels
  if (block_available_up)
  {
    memcpy(&PredPel[1], &imgY[pix_b.pos_y][pix_b.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[1], p_Vid->dc_pred_value_comp[pl], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_A = P_B = P_C = P_D = P_E = P_F = P_G = P_H = (imgpel) p_Vid->dc_pred_value_comp[pl];
#endif
  }

  if (block_available_up_right)
  {
    memcpy(&PredPel[9], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[9], PredPel[8], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_I = P_J = P_K = P_L = P_M = P_N = P_O = P_P = P_H;
#endif
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8PredHor(&(P_Z), block_available_up_left, block_available_up, block_available_left);
  
  for (i=joff; i < joff + BLOCK_SIZE_8x8; i++)
  {
    memcpy(&mpr[i][ioff], &PredPel[1], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 horizontal prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra_prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_hor_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                    ColorPlane pl,         //!< current image plane
                                    int ioff,              //!< pixel offset X within MB
                                    int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  

  int i,j;
  imgpel PredPel[25];  // array of predictor pels
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;

#if (IMGTYPE != 0)
  int ipos0 = ioff    , ipos1 = ioff + 1, ipos2 = ioff + 2, ipos3 = ioff + 3;
  int ipos4 = ioff + 4, ipos5 = ioff + 5, ipos6 = ioff + 6, ipos7 = ioff + 7;
#endif
  int jpos;  
  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
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

  if (!block_available_left)
    printf ("warning: Intra_8x8_Horizontal prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);

  // form predictor pels
  if (block_available_left)
  {
    P_Q = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_R = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_S = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_T = imgY[pix_a[3].pos_y][pix_a[3].pos_x];
    P_U = imgY[pix_a[4].pos_y][pix_a[4].pos_x];
    P_V = imgY[pix_a[5].pos_y][pix_a[5].pos_x];
    P_W = imgY[pix_a[6].pos_y][pix_a[6].pos_x];
    P_X = imgY[pix_a[7].pos_y][pix_a[7].pos_x];
  }
  else
  {
    P_Q = P_R = P_S = P_T = P_U = P_V = P_W = P_X = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8PredVer(&(P_Z), block_available_up_left, block_available_up, block_available_left);

  for (j=0; j < BLOCK_SIZE_8x8; j++)
  {
    jpos = j + joff;
#if (IMGTYPE == 0)
    memset(&mpr[jpos][ioff], (imgpel) (&P_Q)[j], 8 * sizeof(imgpel));
#else
    mpr[jpos][ipos0]  =
      mpr[jpos][ipos1]  =
      mpr[jpos][ipos2]  =
      mpr[jpos][ipos3]  =
      mpr[jpos][ipos4]  =
      mpr[jpos][ipos5]  =
      mpr[jpos][ipos6]  =
      mpr[jpos][ipos7]  = (imgpel) (&P_Q)[j];
#endif
  }
 
  return DECODING_OK;
}

                                    /*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 diagonal down right prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_diag_down_right_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                                ColorPlane pl,         //!< current image plane
                                                int ioff,              //!< pixel offset X within MB
                                                int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  

  int i;
  imgpel PredPel[25];  // array of predictor pels
  imgpel PredArray[16];  // array of final prediction values
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;
  int block_available_up_right;

  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
    block_available_up_left  = pix_d.available;
  }

  if ((!block_available_up)||(!block_available_left)||(!block_available_up_left))
    printf ("warning: Intra_8x8_Diagonal_Down_Right prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);

  // form predictor pels
  if (block_available_up)
  {
    memcpy(&PredPel[1], &imgY[pix_b.pos_y][pix_b.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[1], p_Vid->dc_pred_value_comp[pl], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_A = P_B = P_C = P_D = P_E = P_F = P_G = P_H = (imgpel) p_Vid->dc_pred_value_comp[pl];
#endif
  }

  if (block_available_up_right)
  {
    memcpy(&PredPel[9], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[9], PredPel[8], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_I = P_J = P_K = P_L = P_M = P_N = P_O = P_P = P_H;
#endif
  }

  if (block_available_left)
  {
    P_Q = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_R = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_S = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_T = imgY[pix_a[3].pos_y][pix_a[3].pos_x];
    P_U = imgY[pix_a[4].pos_y][pix_a[4].pos_x];
    P_V = imgY[pix_a[5].pos_y][pix_a[5].pos_x];
    P_W = imgY[pix_a[6].pos_y][pix_a[6].pos_x];
    P_X = imgY[pix_a[7].pos_y][pix_a[7].pos_x];
  }
  else
  {
    P_Q = P_R = P_S = P_T = P_U = P_V = P_W = P_X = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8Pred(PredPel, block_available_up_left, block_available_up, block_available_left);

  // Mode DIAG_DOWN_RIGHT_PRED
  PredArray[ 0] = (imgpel) ((P_X + P_V + 2*(P_W) + 2) >> 2);
  PredArray[ 1] = (imgpel) ((P_W + P_U + 2*(P_V) + 2) >> 2);
  PredArray[ 2] = (imgpel) ((P_V + P_T + 2*(P_U) + 2) >> 2);
  PredArray[ 3] = (imgpel) ((P_U + P_S + 2*(P_T) + 2) >> 2);
  PredArray[ 4] = (imgpel) ((P_T + P_R + 2*(P_S) + 2) >> 2);
  PredArray[ 5] = (imgpel) ((P_S + P_Q + 2*(P_R) + 2) >> 2);
  PredArray[ 6] = (imgpel) ((P_R + P_Z + 2*(P_Q) + 2) >> 2);
  PredArray[ 7] = (imgpel) ((P_Q + P_A + 2*(P_Z) + 2) >> 2);
  PredArray[ 8] = (imgpel) ((P_Z + P_B + 2*(P_A) + 2) >> 2);
  PredArray[ 9] = (imgpel) ((P_A + P_C + 2*(P_B) + 2) >> 2);
  PredArray[10] = (imgpel) ((P_B + P_D + 2*(P_C) + 2) >> 2);
  PredArray[11] = (imgpel) ((P_C + P_E + 2*(P_D) + 2) >> 2);
  PredArray[12] = (imgpel) ((P_D + P_F + 2*(P_E) + 2) >> 2);
  PredArray[13] = (imgpel) ((P_E + P_G + 2*(P_F) + 2) >> 2);
  PredArray[14] = (imgpel) ((P_F + P_H + 2*(P_G) + 2) >> 2);

  memcpy(&mpr[joff++][ioff], &PredArray[7], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[6], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[5], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[4], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[3], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[2], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[1], 8 * sizeof(imgpel));
  memcpy(&mpr[joff  ][ioff], &PredArray[0], 8 * sizeof(imgpel));
 
  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 diagonal down left prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_diag_down_left_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                               ColorPlane pl,         //!< current image plane
                                               int ioff,              //!< pixel offset X within MB
                                               int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i;
  imgpel PredPel[25];  // array of predictor pels
  imgpel PredArray[16];  // array of final prediction values
  imgpel *Pred = &PredArray[0];
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;
  int block_available_up_right;

  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
    block_available_up_left  = pix_d.available;
  }

  if (!block_available_up)
    printf ("warning: Intra_8x8_Diagonal_Down_Left prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);

  // form predictor pels
  if (block_available_up)
  {
    memcpy(&PredPel[1], &imgY[pix_b.pos_y][pix_b.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[1], p_Vid->dc_pred_value_comp[pl], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_A = P_B = P_C = P_D = P_E = P_F = P_G = P_H = (imgpel) p_Vid->dc_pred_value_comp[pl];
#endif
  }

  if (block_available_up_right)
  {
    memcpy(&PredPel[9], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[9], PredPel[8], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_I = P_J = P_K = P_L = P_M = P_N = P_O = P_P = P_H;
#endif
  }

  if (block_available_left)
  {
    P_Q = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_R = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_S = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_T = imgY[pix_a[3].pos_y][pix_a[3].pos_x];
    P_U = imgY[pix_a[4].pos_y][pix_a[4].pos_x];
    P_V = imgY[pix_a[5].pos_y][pix_a[5].pos_x];
    P_W = imgY[pix_a[6].pos_y][pix_a[6].pos_x];
    P_X = imgY[pix_a[7].pos_y][pix_a[7].pos_x];
  }
  else
  {
    P_Q = P_R = P_S = P_T = P_U = P_V = P_W = P_X = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8Pred(PredPel, block_available_up_left, block_available_up, block_available_left);

  // Mode DIAG_DOWN_LEFT_PRED
  *Pred++ = (imgpel) ((P_A + P_C + 2*(P_B) + 2) >> 2);
  *Pred++ = (imgpel) ((P_B + P_D + 2*(P_C) + 2) >> 2);
  *Pred++ = (imgpel) ((P_C + P_E + 2*(P_D) + 2) >> 2);
  *Pred++ = (imgpel) ((P_D + P_F + 2*(P_E) + 2) >> 2);
  *Pred++ = (imgpel) ((P_E + P_G + 2*(P_F) + 2) >> 2);
  *Pred++ = (imgpel) ((P_F + P_H + 2*(P_G) + 2) >> 2);
  *Pred++ = (imgpel) ((P_G + P_I + 2*(P_H) + 2) >> 2);
  *Pred++ = (imgpel) ((P_H + P_J + 2*(P_I) + 2) >> 2);
  *Pred++ = (imgpel) ((P_I + P_K + 2*(P_J) + 2) >> 2);
  *Pred++ = (imgpel) ((P_J + P_L + 2*(P_K) + 2) >> 2);
  *Pred++ = (imgpel) ((P_K + P_M + 2*(P_L) + 2) >> 2);
  *Pred++ = (imgpel) ((P_L + P_N + 2*(P_M) + 2) >> 2);
  *Pred++ = (imgpel) ((P_M + P_O + 2*(P_N) + 2) >> 2);
  *Pred++ = (imgpel) ((P_N + P_P + 2*(P_O) + 2) >> 2);
  *Pred   = (imgpel) ((P_O + 3*(P_P) + 2) >> 2);

  Pred = &PredArray[ 0];

  memcpy(&mpr[joff++][ioff], Pred++, 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], Pred++, 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], Pred++, 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], Pred++, 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], Pred++, 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], Pred++, 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], Pred++, 8 * sizeof(imgpel));
  memcpy(&mpr[joff  ][ioff], Pred  , 8 * sizeof(imgpel));

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 vertical right prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_vert_right_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                           ColorPlane pl,         //!< current image plane
                                           int ioff,              //!< pixel offset X within MB
                                           int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i;
  imgpel PredPel[25];  // array of predictor pels
  imgpel PredArray[22];  // array of final prediction values
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;
  int block_available_up_right;

  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
    block_available_up_left  = pix_d.available;
  }

  if ((!block_available_up)||(!block_available_left)||(!block_available_up_left))
    printf ("warning: Intra_8x8_Vertical_Right prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);

  // form predictor pels
  if (block_available_up)
  {
    memcpy(&PredPel[1], &imgY[pix_b.pos_y][pix_b.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[1], p_Vid->dc_pred_value_comp[pl], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_A = P_B = P_C = P_D = P_E = P_F = P_G = P_H = (imgpel) p_Vid->dc_pred_value_comp[pl];
#endif
  }

  if (block_available_up_right)
  {
    memcpy(&PredPel[9], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[9], PredPel[8], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_I = P_J = P_K = P_L = P_M = P_N = P_O = P_P = P_H;
#endif
  }

  if (block_available_left)
  {
    P_Q = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_R = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_S = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_T = imgY[pix_a[3].pos_y][pix_a[3].pos_x];
    P_U = imgY[pix_a[4].pos_y][pix_a[4].pos_x];
    P_V = imgY[pix_a[5].pos_y][pix_a[5].pos_x];
    P_W = imgY[pix_a[6].pos_y][pix_a[6].pos_x];
    P_X = imgY[pix_a[7].pos_y][pix_a[7].pos_x];
  }
  else
  {
    P_Q = P_R = P_S = P_T = P_U = P_V = P_W = P_X = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8Pred(PredPel, block_available_up_left, block_available_up, block_available_left);

  PredArray[ 0] = (imgpel) ((P_V + P_T + (P_U << 1) + 2) >> 2);
  PredArray[ 1] = (imgpel) ((P_T + P_R + (P_S << 1) + 2) >> 2);
  PredArray[ 2] = (imgpel) ((P_R + P_Z + (P_Q << 1) + 2) >> 2);
  PredArray[ 3] = (imgpel) ((P_Z + P_A + 1) >> 1);
  PredArray[ 4] = (imgpel) ((P_A + P_B + 1) >> 1);
  PredArray[ 5] = (imgpel) ((P_B + P_C + 1) >> 1);
  PredArray[ 6] = (imgpel) ((P_C + P_D + 1) >> 1);
  PredArray[ 7] = (imgpel) ((P_D + P_E + 1) >> 1);
  PredArray[ 8] = (imgpel) ((P_E + P_F + 1) >> 1);
  PredArray[ 9] = (imgpel) ((P_F + P_G + 1) >> 1);
  PredArray[10] = (imgpel) ((P_G + P_H + 1) >> 1);

  PredArray[11] = (imgpel) ((P_W + P_U + (P_V << 1) + 2) >> 2);
  PredArray[12] = (imgpel) ((P_U + P_S + (P_T << 1) + 2) >> 2);
  PredArray[13] = (imgpel) ((P_S + P_Q + (P_R << 1) + 2) >> 2);
  PredArray[14] = (imgpel) ((P_Q + P_A + 2*P_Z + 2) >> 2);
  PredArray[15] = (imgpel) ((P_Z + P_B + 2*P_A + 2) >> 2);
  PredArray[16] = (imgpel) ((P_A + P_C + 2*P_B + 2) >> 2);
  PredArray[17] = (imgpel) ((P_B + P_D + 2*P_C + 2) >> 2);
  PredArray[18] = (imgpel) ((P_C + P_E + 2*P_D + 2) >> 2);
  PredArray[19] = (imgpel) ((P_D + P_F + 2*P_E + 2) >> 2);
  PredArray[20] = (imgpel) ((P_E + P_G + 2*P_F + 2) >> 2);
  PredArray[21] = (imgpel) ((P_F + P_H + 2*P_G + 2) >> 2);

  memcpy(&mpr[joff++][ioff], &PredArray[ 3], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[14], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[ 2], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[13], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[ 1], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[12], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[ 0], 8 * sizeof(imgpel));
  memcpy(&mpr[joff  ][ioff], &PredArray[11], 8 * sizeof(imgpel));

  return DECODING_OK;
}


/*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 vertical left prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_vert_left_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                          ColorPlane pl,         //!< current image plane
                                          int ioff,              //!< pixel offset X within MB
                                          int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i;
  imgpel PredPel[25];  // array of predictor pels  
  imgpel PredArray[22];  // array of final prediction values
  imgpel *pred_pel = &PredArray[0];
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;
  int block_available_up_right;

  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
    block_available_up_left  = pix_d.available;
  }

  if (!block_available_up)
    printf ("warning: Intra_4x4_Vertical_Left prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);

  // form predictor pels
  if (block_available_up)
  {
    memcpy(&PredPel[1], &imgY[pix_b.pos_y][pix_b.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[1], p_Vid->dc_pred_value_comp[pl], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_A = P_B = P_C = P_D = P_E = P_F = P_G = P_H = (imgpel) p_Vid->dc_pred_value_comp[pl];
#endif
  }

  if (block_available_up_right)
  {
    memcpy(&PredPel[9], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[9], PredPel[8], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_I = P_J = P_K = P_L = P_M = P_N = P_O = P_P = P_H;
#endif
  }

  if (block_available_left)
  {
    P_Q = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_R = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_S = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_T = imgY[pix_a[3].pos_y][pix_a[3].pos_x];
    P_U = imgY[pix_a[4].pos_y][pix_a[4].pos_x];
    P_V = imgY[pix_a[5].pos_y][pix_a[5].pos_x];
    P_W = imgY[pix_a[6].pos_y][pix_a[6].pos_x];
    P_X = imgY[pix_a[7].pos_y][pix_a[7].pos_x];
  }
  else
  {
    P_Q = P_R = P_S = P_T = P_U = P_V = P_W = P_X = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8Pred(PredPel, block_available_up_left, block_available_up, block_available_left);

  *pred_pel++ = (imgpel) ((P_A + P_B + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_B + P_C + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_C + P_D + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_D + P_E + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_E + P_F + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_F + P_G + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_G + P_H + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_H + P_I + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_I + P_J + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_J + P_K + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_K + P_L + 1) >> 1);
  *pred_pel++ = (imgpel) ((P_A + P_C + 2*P_B + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_B + P_D + 2*P_C + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_C + P_E + 2*P_D + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_D + P_F + 2*P_E + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_E + P_G + 2*P_F + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_F + P_H + 2*P_G + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_G + P_I + 2*P_H + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_H + P_J + (P_I << 1) + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_I + P_K + (P_J << 1) + 2) >> 2);
  *pred_pel++ = (imgpel) ((P_J + P_L + (P_K << 1) + 2) >> 2);
  *pred_pel   = (imgpel) ((P_K + P_M + (P_L << 1) + 2) >> 2);

  memcpy(&mpr[joff++][ioff], &PredArray[ 0], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[11], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[ 1], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[12], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[ 2], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[13], 8 * sizeof(imgpel));
  memcpy(&mpr[joff++][ioff], &PredArray[ 3], 8 * sizeof(imgpel));
  memcpy(&mpr[joff  ][ioff], &PredArray[14], 8 * sizeof(imgpel));

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 horizontal up prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_hor_up_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                       ColorPlane pl,         //!< current image plane
                                       int ioff,              //!< pixel offset X within MB
                                       int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  
  int i;
  imgpel PredPel[25];  // array of predictor pels
  imgpel PredArray[22];   // array of final prediction values
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;
  int block_available_up_right;
  int jpos0 = joff    , jpos1 = joff + 1, jpos2 = joff + 2, jpos3 = joff + 3;
  int jpos4 = joff + 4, jpos5 = joff + 5, jpos6 = joff + 6, jpos7 = joff + 7;

  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
    block_available_up_left  = pix_d.available;
  }

  if (!block_available_left)
    printf ("warning: Intra_8x8_Horizontal_Up prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);

  // form predictor pels
  if (block_available_up)
  {
    memcpy(&PredPel[1], &imgY[pix_b.pos_y][pix_b.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[1], p_Vid->dc_pred_value_comp[pl], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_A = P_B = P_C = P_D = P_E = P_F = P_G = P_H = (imgpel) p_Vid->dc_pred_value_comp[pl];
#endif
  }

  if (block_available_up_right)
  {
    memcpy(&PredPel[9], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[9], PredPel[8], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_I = P_J = P_K = P_L = P_M = P_N = P_O = P_P = P_H;
#endif
  }

  if (block_available_left)
  {
    P_Q = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_R = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_S = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_T = imgY[pix_a[3].pos_y][pix_a[3].pos_x];
    P_U = imgY[pix_a[4].pos_y][pix_a[4].pos_x];
    P_V = imgY[pix_a[5].pos_y][pix_a[5].pos_x];
    P_W = imgY[pix_a[6].pos_y][pix_a[6].pos_x];
    P_X = imgY[pix_a[7].pos_y][pix_a[7].pos_x];
  }
  else
  {
    P_Q = P_R = P_S = P_T = P_U = P_V = P_W = P_X = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8Pred(PredPel, block_available_up_left, block_available_up, block_available_left);

  PredArray[ 0] = (imgpel) ((P_Q + P_R + 1) >> 1);
  PredArray[ 1] = (imgpel) ((P_S + P_Q + (P_R << 1) + 2) >> 2);
  PredArray[ 2] = (imgpel) ((P_R + P_S + 1) >> 1);
  PredArray[ 3] = (imgpel) ((P_T + P_R + (P_S << 1) + 2) >> 2);
  PredArray[ 4] = (imgpel) ((P_S + P_T + 1) >> 1);
  PredArray[ 5] = (imgpel) ((P_U + P_S + (P_T << 1) + 2) >> 2);
  PredArray[ 6] = (imgpel) ((P_T + P_U + 1) >> 1);
  PredArray[ 7] = (imgpel) ((P_V + P_T + (P_U << 1) + 2) >> 2);
  PredArray[ 8] = (imgpel) ((P_U + P_V + 1) >> 1);
  PredArray[ 9] = (imgpel) ((P_W + P_U + (P_V << 1) + 2) >> 2);
  PredArray[10] = (imgpel) ((P_V + P_W + 1) >> 1);
  PredArray[11] = (imgpel) ((P_X + P_V + (P_W << 1) + 2) >> 2);
  PredArray[12] = (imgpel) ((P_W + P_X + 1) >> 1);
  PredArray[13] = (imgpel) ((P_W + P_X + (P_X << 1) + 2) >> 2);
  PredArray[14] = (imgpel) P_X;
  PredArray[15] = (imgpel) P_X;
  PredArray[16] = (imgpel) P_X;
  PredArray[17] = (imgpel) P_X;
  PredArray[18] = (imgpel) P_X;
  PredArray[19] = (imgpel) P_X;
  PredArray[20] = (imgpel) P_X;
  PredArray[21] = (imgpel) P_X;

  memcpy(&mpr[jpos0][ioff], &PredArray[0], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos1][ioff], &PredArray[2], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos2][ioff], &PredArray[4], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos3][ioff], &PredArray[6], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos4][ioff], &PredArray[8], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos5][ioff], &PredArray[10], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos6][ioff], &PredArray[12], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos7][ioff], &PredArray[14], 8 * sizeof(imgpel));

  return DECODING_OK;
}

/*!
 ***********************************************************************
 * \brief
 *    makes and returns 8x8 horizontal down prediction mode
 *
 * \return
 *    DECODING_OK   decoding of intra prediction mode was successful            \n
 *
 ***********************************************************************
 */
static inline int intra8x8_hor_down_pred_mbaff(Macroblock *currMB,    //!< current macroblock
                                         ColorPlane pl,         //!< current image plane
                                         int ioff,              //!< pixel offset X within MB
                                         int joff)              //!< pixel offset Y within MB
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;

  int i;
  imgpel PredPel[25];  // array of predictor pels
  imgpel PredArray[22];   // array of final prediction values
  imgpel **imgY = (pl) ? currSlice->dec_picture->imgUV[pl - 1] : currSlice->dec_picture->imgY; // For MB level frame/field coding tools -- set default to imgY

  PixelPos pix_a[8];
  PixelPos pix_b, pix_c, pix_d;

  int block_available_up;
  int block_available_left;
  int block_available_up_left;
  int block_available_up_right;
  int jpos0 = joff    , jpos1 = joff + 1, jpos2 = joff + 2, jpos3 = joff + 3;
  int jpos4 = joff + 4, jpos5 = joff + 5, jpos6 = joff + 6, jpos7 = joff + 7;
  
  imgpel **mpr = currSlice->mb_pred[pl];
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  for (i=0;i<8;i++)
  {
    getAffNeighbour(currMB, ioff - 1, joff + i, mb_size, &pix_a[i]);
  }

  getAffNeighbour(currMB, ioff    , joff - 1, mb_size, &pix_b);
  getAffNeighbour(currMB, ioff + 8, joff - 1, mb_size, &pix_c);
  getAffNeighbour(currMB, ioff - 1, joff - 1, mb_size, &pix_d);

  pix_c.available = pix_c.available &&!(ioff == 8 && joff == 8);

  if (p_Vid->active_pps->constrained_intra_pred_flag)
  {
    for (i=0, block_available_left=1; i<8;i++)
      block_available_left  &= pix_a[i].available ? currSlice->intra_block[pix_a[i].mb_addr]: 0;
    block_available_up       = pix_b.available ? currSlice->intra_block [pix_b.mb_addr] : 0;
    block_available_up_right = pix_c.available ? currSlice->intra_block [pix_c.mb_addr] : 0;
    block_available_up_left  = pix_d.available ? currSlice->intra_block [pix_d.mb_addr] : 0;
  }
  else
  {
    block_available_left     = pix_a[0].available;
    block_available_up       = pix_b.available;
    block_available_up_right = pix_c.available;
    block_available_up_left  = pix_d.available;
  }

  if ((!block_available_up)||(!block_available_left)||(!block_available_up_left))
    printf ("warning: Intra_8x8_Horizontal_Down prediction mode not allowed at mb %d\n", (int) currSlice->current_mb_nr);

  // form predictor pels
  if (block_available_up)
  {
    memcpy(&PredPel[1], &imgY[pix_b.pos_y][pix_b.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[1], p_Vid->dc_pred_value_comp[pl], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_A = P_B = P_C = P_D = P_E = P_F = P_G = P_H = (imgpel) p_Vid->dc_pred_value_comp[pl];
#endif
  }

  if (block_available_up_right)
  {
    memcpy(&PredPel[9], &imgY[pix_c.pos_y][pix_c.pos_x], BLOCK_SIZE_8x8 * sizeof(imgpel));
  }
  else
  {
#if (IMGTYPE == 0)
    memset(&PredPel[9], PredPel[8], BLOCK_SIZE_8x8 * sizeof(imgpel));
#else
    P_I = P_J = P_K = P_L = P_M = P_N = P_O = P_P = P_H;
#endif
  }

  if (block_available_left)
  {
    P_Q = imgY[pix_a[0].pos_y][pix_a[0].pos_x];
    P_R = imgY[pix_a[1].pos_y][pix_a[1].pos_x];
    P_S = imgY[pix_a[2].pos_y][pix_a[2].pos_x];
    P_T = imgY[pix_a[3].pos_y][pix_a[3].pos_x];
    P_U = imgY[pix_a[4].pos_y][pix_a[4].pos_x];
    P_V = imgY[pix_a[5].pos_y][pix_a[5].pos_x];
    P_W = imgY[pix_a[6].pos_y][pix_a[6].pos_x];
    P_X = imgY[pix_a[7].pos_y][pix_a[7].pos_x];
  }
  else
  {
    P_Q = P_R = P_S = P_T = P_U = P_V = P_W = P_X = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  if (block_available_up_left)
  {
    P_Z = imgY[pix_d.pos_y][pix_d.pos_x];
  }
  else
  {
    P_Z = (imgpel) p_Vid->dc_pred_value_comp[pl];
  }

  LowPassForIntra8x8Pred(PredPel, block_available_up_left, block_available_up, block_available_left);

  PredArray[ 0] = (imgpel) ((P_X + P_W + 1) >> 1);
  PredArray[ 1] = (imgpel) ((P_V + P_X + (P_W << 1) + 2) >> 2);
  PredArray[ 2] = (imgpel) ((P_W + P_V + 1) >> 1);
  PredArray[ 3] = (imgpel) ((P_U + P_W + (P_V << 1) + 2) >> 2);
  PredArray[ 4] = (imgpel) ((P_V + P_U + 1) >> 1);
  PredArray[ 5] = (imgpel) ((P_T + P_V + (P_U << 1) + 2) >> 2);
  PredArray[ 6] = (imgpel) ((P_U + P_T + 1) >> 1);
  PredArray[ 7] = (imgpel) ((P_S + P_U + (P_T << 1) + 2) >> 2);
  PredArray[ 8] = (imgpel) ((P_T + P_S + 1) >> 1);
  PredArray[ 9] = (imgpel) ((P_R + P_T + (P_S << 1) + 2) >> 2);
  PredArray[10] = (imgpel) ((P_S + P_R + 1) >> 1);
  PredArray[11] = (imgpel) ((P_Q + P_S + (P_R << 1) + 2) >> 2);
  PredArray[12] = (imgpel) ((P_R + P_Q + 1) >> 1);
  PredArray[13] = (imgpel) ((P_Z + P_R + (P_Q << 1) + 2) >> 2);
  PredArray[14] = (imgpel) ((P_Q + P_Z + 1) >> 1);
  PredArray[15] = (imgpel) ((P_Q + P_A + 2*P_Z + 2) >> 2);
  PredArray[16] = (imgpel) ((P_Z + P_B + 2*P_A + 2) >> 2);
  PredArray[17] = (imgpel) ((P_A + P_C + 2*P_B + 2) >> 2);
  PredArray[18] = (imgpel) ((P_B + P_D + 2*P_C + 2) >> 2);
  PredArray[19] = (imgpel) ((P_C + P_E + 2*P_D + 2) >> 2);
  PredArray[20] = (imgpel) ((P_D + P_F + 2*P_E + 2) >> 2);
  PredArray[21] = (imgpel) ((P_E + P_G + 2*P_F + 2) >> 2);

  memcpy(&mpr[jpos0][ioff], &PredArray[14], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos1][ioff], &PredArray[12], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos2][ioff], &PredArray[10], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos3][ioff], &PredArray[ 8], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos4][ioff], &PredArray[ 6], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos5][ioff], &PredArray[ 4], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos6][ioff], &PredArray[ 2], 8 * sizeof(imgpel));
  memcpy(&mpr[jpos7][ioff], &PredArray[ 0], 8 * sizeof(imgpel));

  return DECODING_OK;
}

/*!
 ************************************************************************
 * \brief
 *    Make intra 8x8 prediction according to all 9 prediction modes.
 *    The routine uses left and upper neighbouring points from
 *    previous coded blocks to do this (if available). Notice that
 *    inaccessible neighbouring points are signalled with a negative
 *    value in the predmode array .
 *
 *  \par Input:
 *     Starting point of current 8x8 block image position
 *
 ************************************************************************
 */
int intra_pred_8x8_mbaff(Macroblock *currMB,    //!< Current Macroblock
                   ColorPlane pl,         //!< Current color plane
                   int ioff,              //!< ioff
                   int joff)              //!< joff

{  
  int block_x = (currMB->block_x) + (ioff >> 2);
  int block_y = (currMB->block_y) + (joff >> 2);
  byte predmode = currMB->p_Slice->ipredmode[block_y][block_x];

  currMB->ipmode_DPCM = predmode;  //For residual DPCM

  switch (predmode)
  {
  case DC_PRED:
    return (intra8x8_dc_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case VERT_PRED:
    return (intra8x8_vert_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case HOR_PRED:
    return (intra8x8_hor_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case DIAG_DOWN_RIGHT_PRED:
    return (intra8x8_diag_down_right_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case DIAG_DOWN_LEFT_PRED:
    return (intra8x8_diag_down_left_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case VERT_RIGHT_PRED:
    return (intra8x8_vert_right_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case VERT_LEFT_PRED:
    return (intra8x8_vert_left_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case HOR_UP_PRED:
    return (intra8x8_hor_up_pred_mbaff(currMB, pl, ioff, joff));
    break;
  case HOR_DOWN_PRED:  
    return (intra8x8_hor_down_pred_mbaff(currMB, pl, ioff, joff));
  default:
    printf("Error: illegal intra_8x8 prediction mode: %d\n", (int) predmode);
    return SEARCH_SYNC;
    break;
  }  
}



