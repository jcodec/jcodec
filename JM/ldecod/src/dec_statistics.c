/*!
 ***********************************************************************
 * \file
 *    dec_statistics.c
 * \brief
 *    Decoder statistics handling.
 * \author
 *  Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Alexis Michael Tourapis <alexismt@ieee.org>
 ***********************************************************************
 */

#include "global.h"
#include "dec_statistics.h"
#include "memalloc.h"

/*!
 ***********************************************************************
 * \brief
 *    allocates and initializes decoder statistics memory
 * \param stats
 *    Decoder statistics
 * \return
 ***********************************************************************
 */
void init_dec_stats(DecStatParameters *stats)
{ 
  int i, j;
  int64 *hist;
  for (i = 0; i < NUM_SLICE_TYPES; i++)
  {
    stats->frame_ctr[i] = 0;
    for (j = 0; j < MAXMODE; j++)
    {
      stats->mode_use          [i][j]    = 0; 
      stats->mode_use_transform[i][j][0] = 0;
      stats->mode_use_transform[i][j][1] = 0;
    }
  }

  for (i = 0; i < 2; i++)
  {
    for (j = 0; j < 2; j++)
    {
      if ((hist = (int64 *) malloc (4096 * sizeof (int64)))== NULL)
        no_mem_exit ("init_dec_stats: stats->histogram_mv");
      memset(hist, 0, 4096 * sizeof (int64));
      stats->histogram_mv[i][j] = hist + 2048;
    }
    if ((hist = (int64 *) malloc (17 * sizeof (int64)))== NULL)
      no_mem_exit ("init_dec_stats: stats->histogram_refs");
    memset(hist, 0, 17 * sizeof (int64));
    stats->histogram_refs[i] = hist + 1;
  }
}

void delete_dec_stats(DecStatParameters *stats)
{ 
  int i, j;

  for (i = 0; i < 2; i++)
  {
    for (j = 0; j < 2; j++)
    {
      stats->histogram_mv[i][j] -= 2048;
      free(stats->histogram_mv[i][j]);
    }
    stats->histogram_refs[i] -= 1;
    free(stats->histogram_refs[i]);
  }
}
