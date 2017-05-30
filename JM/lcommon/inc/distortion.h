/*!
 **************************************************************************
 *  \file distortion.h
 *  \brief  
 *     Distortion data header file
 *  \date 2.23.2009,
 *
 *  \author 
 *   Alexis Michael Tourapis        <alexismt@ieee.org>
 *
 **************************************************************************
 */

#ifndef _DISTORTION_H_
#define _DISTORTION_H_

// Distortion data structure. Could be extended in the future to support
// other data 
typedef struct distortion_data
{
  int      i4x4rd[4][4];         //! i4x4 rd cost
  distblk  i4x4  [4][4];         //! i4x4 cost
  distblk  i8x8  [2][2];         //! i8x8 cost
  int      i8x8rd[2][2];         //! i8x8 rd cost
  int      i16x16;
  int      i16x16rd;
  double   rd_cost;
} DistortionData;

#endif

