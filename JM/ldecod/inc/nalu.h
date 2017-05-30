
/*!
 **************************************************************************************
 * \file
 *    nalu.h
 * \brief
 *    Common NALU support functions
 *
 * \date 25 November 2002
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Stephan Wenger        <stewe@cs.tu-berlin.de>
 ***************************************************************************************
 */


#ifndef _NALU_H_
#define _NALU_H_

#include "nalucommon.h"

extern void CheckZeroByteNonVCL(VideoParameters *p_Vid, NALU_t *nalu);
extern void CheckZeroByteVCL   (VideoParameters *p_Vid, NALU_t *nalu);

extern int read_next_nalu(VideoParameters *p_Vid, NALU_t *nalu);

#endif
