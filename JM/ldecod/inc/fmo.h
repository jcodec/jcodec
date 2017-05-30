
/*!
 ***************************************************************************
 *
 * \file fmo.h
 *
 * \brief
 *    Support for Flexilble Macroblock Ordering (FMO)
 *
 * \date
 *    19 June, 2002
 *
 * \author
 *    Stephan Wenger   stewe@cs.tu-berlin.de
 **************************************************************************/

#ifndef _FMO_H_
#define _FMO_H_


extern int fmo_init (VideoParameters *p_Vid, Slice *pSlice);
extern int FmoFinit (VideoParameters *p_Vid);

extern int FmoGetNumberOfSliceGroup(VideoParameters *p_Vid);
extern int FmoGetLastMBOfPicture   (VideoParameters *p_Vid);
extern int FmoGetLastMBInSliceGroup(VideoParameters *p_Vid, int SliceGroup);
extern int FmoGetSliceGroupId      (VideoParameters *p_Vid, int mb);
extern int FmoGetNextMBNr          (VideoParameters *p_Vid, int CurrentMbNr);

#endif
