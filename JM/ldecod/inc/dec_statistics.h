/*!
 **************************************************************************
 *  \file dec_statistics.h
 *
 *  \brief
 *     Various decoding statistics
 *
 *  \author
 *      Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Alexis Tourapis                 <alexismt@ieee.org>
 *
 **************************************************************************
 */

#ifndef _DEC_STATISTICS_H_
#define _DEC_STATISTICS_H_
#include "global.h"

typedef struct dec_stat_parameters
{
  int    frame_ctr           [NUM_SLICE_TYPES];          //!< Counter for different frame coding types (assumes one slice type per frame)
  int64  mode_use            [NUM_SLICE_TYPES][MAXMODE]; //!< Macroblock mode usage per slice
  int64  mode_use_transform  [NUM_SLICE_TYPES][MAXMODE][2];

  int64  *histogram_mv  [2][2];    //!< mv histogram (per list and per direction)
  int64  *histogram_refs[2];       //!< reference histogram (per list)
} DecStatParameters;

extern void init_dec_stats  (DecStatParameters *stats);
extern void delete_dec_stats(DecStatParameters *stats);


#endif
