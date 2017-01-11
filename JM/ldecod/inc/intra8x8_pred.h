/*!
 *************************************************************************************
 * \file intra8x8_pred.h
 *
 * \brief
 *    definitions for intra 8x8 prediction
 *
 * \author
 *      Main contributors (see contributors.h for copyright, 
 *                         address and affiliation details)
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 *
 *************************************************************************************
 */

#ifndef _INTRA8x8_PRED_H_
#define _INTRA8x8_PRED_H_

#include "global.h"
#include "mbuffer.h"

extern int intra_pred_8x8_normal(Macroblock *currMB, ColorPlane pl, int ioff, int joff);
extern int intra_pred_8x8_mbaff(Macroblock *currMB, ColorPlane pl, int ioff, int joff);

#endif

