/*!
 **************************************************************************
 *  \file fast_memory.h
 *
 *  \brief
 *     Memory handling operations
 *
 *  \author
 *      Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Chris Vogt
 *
 **************************************************************************
 */

#ifndef _FAST_MEMORY_H_
#define _FAST_MEMORY_H_

#include "typedefs.h"


static inline void fast_memset(void *dst,int value,int width)
{
  memset(dst,value,width);
}

static inline void fast_memcpy(void *dst,void *src,int width)
{
  memcpy(dst,src,width);
}

static inline void fast_memset_zero(void *dst, int width)
{
  memset(dst,0,width);
}

#endif 

