/*!
 *************************************************************************************
 * \file intra16x16_pred.h
 *
 * \brief
 *    definitions for intra 16x16 prediction
 *
 * \author
 *      Main contributors (see contributors.h for copyright, 
 *                         address and affiliation details)
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 *
 *************************************************************************************
 */

#ifndef _INTRA16x16_PRED_H_
#define _INTRA16x16_PRED_H_

#include "global.h"
#include "mbuffer.h"

extern int intra_pred_16x16_mbaff (Macroblock *currMB, ColorPlane pl, int predmode);
extern int intra_pred_16x16_normal(Macroblock *currMB, ColorPlane pl, int predmode);

#endif

