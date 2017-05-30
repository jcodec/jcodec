/*!
 *************************************************************************************
 * \file intra_pred_common.c
 *
 * \brief
 *    functions for setting up intra prediction modes
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
#include "intra8x8_pred.h"
#include "intra16x16_pred.h"
#include "mb_access.h"
#include "image.h"


extern void intra_pred_chroma      (Macroblock *currMB);
extern void intra_pred_chroma_mbaff(Macroblock *currMB);


void set_intra_prediction_modes(Slice *currSlice)
{ 
  if (currSlice->mb_aff_frame_flag)
  {
    currSlice->intra_pred_4x4    = intra_pred_4x4_mbaff;
    currSlice->intra_pred_8x8    = intra_pred_8x8_mbaff;
    currSlice->intra_pred_16x16  = intra_pred_16x16_mbaff;    
    currSlice->intra_pred_chroma = intra_pred_chroma_mbaff;
  }
  else
  {
    currSlice->intra_pred_4x4    = intra_pred_4x4_normal;  
    currSlice->intra_pred_8x8    = intra_pred_8x8_normal;
    currSlice->intra_pred_16x16  = intra_pred_16x16_normal;
    currSlice->intra_pred_chroma = intra_pred_chroma;   
  }
}
