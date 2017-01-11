/*!
 *************************************************************************************
 * \file intra4x4_pred.h
 *
 * \brief
 *    definitions for intra 4x4 prediction
 *
 * \author
 *      Main contributors (see contributors.h for copyright, 
 *                         address and affiliation details)
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 *
 *************************************************************************************
 */

#ifndef _INTRA4x4_PRED_H_
#define _INTRA4x4_PRED_H_

#include "global.h"
#include "mbuffer.h"

extern int intra_pred_4x4_mbaff (Macroblock *currMB, ColorPlane pl, int ioff, int joff, int img_block_x, int img_block_y);
extern int intra_pred_4x4_normal(Macroblock *currMB, ColorPlane pl, int ioff, int joff, int img_block_x, int img_block_y);

#endif

