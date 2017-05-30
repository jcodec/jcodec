
/*!
 ************************************************************************
 * \file erc_globals.h
 *
 * \brief
 *      global header file for error concealment module
 *
 * \author
 *      - Viktor Varsa                     <viktor.varsa@nokia.com>
 *      - Ye-Kui Wang                   <wyk@ieee.org>
 ************************************************************************
 */

#ifndef _ERC_GLOBALS_H_
#define _ERC_GLOBALS_H_

#include "defines.h"

/* "block" means an 8x8 pixel area */

/* Region modes */
#define REGMODE_INTER_COPY       0  //!< Copy region
#define REGMODE_INTER_PRED       1  //!< Inter region with motion vectors
#define REGMODE_INTRA            2  //!< Intra region
#define REGMODE_SPLITTED         3  //!< Any region mode higher than this indicates that the region
                                    //!< is splitted which means 8x8 block
#define REGMODE_INTER_COPY_8x8   4
#define REGMODE_INTER_PRED_8x8   5
#define REGMODE_INTRA_8x8        6

//! YUV pixel domain image arrays for a video frame
typedef struct frame_s
{
  VideoParameters *p_Vid;
  imgpel *yptr;
  imgpel *uptr;
  imgpel *vptr;
} frame;

//! region structure stores information about a region that is needed for concealment
typedef struct object_buffer
{
  byte regionMode;  //!< region mode as above
  int xMin;         //!< X coordinate of the pixel position of the top-left corner of the region
  int yMin;         //!< Y coordinate of the pixel position of the top-left corner of the region
  int mv[3];        //!< motion vectors in 1/4 pixel units: mvx = mv[0], mvy = mv[1],
                    //!< and ref_frame = mv[2]
} objectBuffer_t;

#endif

