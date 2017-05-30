
/*!
 ************************************************************************
 * \file  leaky_bucket.c
 *
 * \brief
 *   Calculate if decoder leaky bucket parameters meets HRD constraints specified by encoder.
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Shankar Regunathan                   <shanre@microsoft.com>
 ************************************************************************
 */

#include "contributors.h"
#include "global.h"

#ifdef _LEAKYBUCKET_
/*!
 ***********************************************************************
 * \brief
 *   Function to get unsigned long word from a file.
 * \param fp
 *    Filepointer
 * \return
 *    unsigned long double word
 * \par SideEffects
 *     None.
 *  \par Notes
 *     File should be opened to read in binary format.
 * \author
 *    Shankar Regunathan                   shanre@microsoft.com
 *  \date
 *      December 06, 2001.
 ***********************************************************************
 */
/* gets unsigned double stored in Big Endian Order */
unsigned long GetBigDoubleWord(FILE *fp)
{
  unsigned long dw;
  dw =  (unsigned long) (fgetc(fp) & 0xFF);
  dw = ((unsigned long) (fgetc(fp) & 0xFF)) | (dw << 0x08);
  dw = ((unsigned long) (fgetc(fp) & 0xFF)) | (dw << 0x08);
  dw = ((unsigned long) (fgetc(fp) & 0xFF)) | (dw << 0x08);
  return(dw);
}

/*!
 ***********************************************************************
 * \brief
 *   Calculates if decoder leaky bucket parameters meets HRD constraints specified by encoder.
 * \param p_Inp
 *    Structure which contains decoder leaky bucket parameters.
 * \return
 *    None
 * \par SideEffects
 *     None.
 * \par Notes
 *     Failure if LeakyBucketParam file is missing or if it does not have
 *     the correct number of entries.
 * \author
 *    Shankar Regunathan                   shanre@microsoft.com
 *  \date
 *      December 06, 2001.
 ***********************************************************************
 */

/* Main Routine to verify HRD compliance */
void calc_buffer(InputParameters *p_Inp)
{
  unsigned long NumberLeakyBuckets, *Rmin, *Bmin, *Fmin;
  float B_interp,  F_interp;
  unsigned long iBucket;
  float dnr, frac1, frac2;
  unsigned long R_decoder, B_decoder, F_decoder;
  FILE *outf;

  if ((outf=fopen(p_Inp->LeakyBucketParamFile,"rb"))==NULL)
    {
    snprintf(errortext, ET_SIZE, "Error open file %s \n",p_Inp->LeakyBucketParamFile);
    error(errortext,1);
    }

  NumberLeakyBuckets = GetBigDoubleWord(outf);
  printf(" Number Leaky Buckets: %8ld \n\n", NumberLeakyBuckets);
  Rmin = calloc(NumberLeakyBuckets, sizeof(unsigned long));
  Bmin = calloc(NumberLeakyBuckets, sizeof(unsigned long));
  Fmin = calloc(NumberLeakyBuckets, sizeof(unsigned long));

  for(iBucket =0; iBucket < NumberLeakyBuckets; iBucket++)
  {
    Rmin[iBucket] = GetBigDoubleWord(outf);
    Bmin[iBucket] = GetBigDoubleWord(outf);
    Fmin[iBucket] = GetBigDoubleWord(outf);
    printf(" %8ld %8ld %8ld \n", Rmin[iBucket], Bmin[iBucket], Fmin[iBucket]);
  }
  fclose(outf);

  R_decoder = p_Inp->R_decoder;
  F_decoder = p_Inp->F_decoder;
  B_decoder = p_Inp->B_decoder;

  for( iBucket =0; iBucket < NumberLeakyBuckets; iBucket++)
  {
    if(R_decoder < Rmin[iBucket])
      break;
  }

  printf("\n");
  if(iBucket > 0 ) 
  {
    if(iBucket < NumberLeakyBuckets) 
    {
      dnr = (float) (Rmin[iBucket] - Rmin[iBucket-1]);
      frac1 = (float) (R_decoder - Rmin[iBucket-1]);
      frac2 = (float) (Rmin[iBucket] - R_decoder);
      B_interp = (float) (Bmin[iBucket] * frac1 + Bmin[iBucket-1] * frac2) /dnr;
      F_interp = (float) (Fmin[iBucket] * frac1 + Fmin[iBucket-1] * frac2) /dnr;
    }
    else {
      B_interp = (float) Bmin[iBucket-1];
      F_interp = (float) Fmin[iBucket-1];
    }
    printf(" Min.buffer %8.2f Decoder buffer size %ld \n Minimum Delay %8.2f DecoderDelay %ld \n", B_interp, B_decoder, F_interp, F_decoder);
    if(B_decoder > B_interp && F_decoder > F_interp)
      printf(" HRD Compliant \n");
    else
      printf(" HRD Non Compliant \n");
  }
  else { // (iBucket = 0)
    printf(" Decoder Rate is too small; HRD cannot be verified \n");
  }

  free(Rmin);
  free(Bmin);
  free(Fmin);
  return;
}
#endif
