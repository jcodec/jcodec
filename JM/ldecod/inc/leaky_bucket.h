
/*!
 *************************************************************************************
 * \file leaky_bucket.h
 *
 * \brief
 *    Header for Leaky Buffer parameters
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Shankar Regunathan                   <shanre@microsoft.com>
 *************************************************************************************
 */
#ifndef _LEAKY_BUCKET_H_
#define _LEAKY_BUCKET_H_

#include "global.h"

#ifdef _LEAKYBUCKET_
// Leaky Bucket functions
unsigned long GetBigDoubleWord(FILE *fp);
void calc_buffer(InputParameters *p_Inp);
#endif

#endif

