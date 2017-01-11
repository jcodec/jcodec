
/*!
 ************************************************************************
 * \file
 *    blk_prediction.h
 *
 * \brief
 *    block prediction header
 *
 * \author
 *      Main contributors (see contributors.h for copyright, 
 *                         address and affiliation details)
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 *
 *************************************************************************************
 */

#ifndef _BLK_PREDICTION_H_
#define _BLK_PREDICTION_H_
#include "mbuffer.h"

extern void compute_residue    (imgpel **curImg, imgpel **mb_pred, int **mb_rres, int mb_x, int opix_x, int width, int height);
extern void sample_reconstruct (imgpel **curImg, imgpel **mb_pred, int **mb_rres, int mb_x, int opix_x, int width, int height, int max_imgpel_value, int dq_bits);
#endif

