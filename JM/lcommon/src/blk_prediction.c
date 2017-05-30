/*!
 *************************************************************************************
 * \file blk_prediction.c
 *
 * \brief
 *    Block Prediction related functions
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Alexis Michael Tourapis         <alexismt@ieee.org>
 *
 *************************************************************************************
 */

#include "contributors.h"

#include <stdlib.h>
#include <assert.h>
#include <limits.h>
#include <math.h>

#include "block.h"
#include "global.h"

#include "macroblock.h"
#include "mc_prediction.h"
#include "image.h"
#include "mb_access.h"

void compute_residue (imgpel **curImg, imgpel **mpr, int **mb_rres, int mb_x, int opix_x, int width, int height)
{
  imgpel *imgOrg, *imgPred;
  int    *m7;
  int i, j;

  for (j = 0; j < height; j++)
  {
    imgOrg = &curImg[j][opix_x];    
    imgPred = &mpr[j][mb_x];
    m7 = &mb_rres[j][mb_x]; 
    for (i = 0; i < width; i++)
    {
      *m7++ = *imgOrg++ - *imgPred++;
    }
  }
}

void sample_reconstruct (imgpel **curImg, imgpel **mpr, int **mb_rres, int mb_x, int opix_x, int width, int height, int max_imgpel_value, int dq_bits)
{
  imgpel *imgOrg, *imgPred;
  int    *m7;
  int i, j;

  for (j = 0; j < height; j++)
  {
    imgOrg = &curImg[j][opix_x];
    imgPred = &mpr[j][mb_x];
    m7 = &mb_rres[j][mb_x]; 
    for (i=0;i<width;i++)
      *imgOrg++ = (imgpel) iClip1( max_imgpel_value, rshift_rnd_sf(*m7++, dq_bits) + *imgPred++);
  }
}

