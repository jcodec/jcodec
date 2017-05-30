/*!
 *************************************************************************************
 * \file resize.h
 *
 * \brief
 *    Common type definitions
 *    Currently only supports Windows and Linux operating systems. 
 *    Need to add support for other "older systems such as VAX, DECC, Unix Alpha etc
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Yuwen He         <yhe@dolby.com>
 *************************************************************************************
 */

#ifndef _RESIZE_H_
#define _RESIZE_H_
extern int PyrDownG5x5_U8CnR(  const imgpel* src, 
                        int srcstep,      //stride of source in bytes;
                        int width,        //width of source;
                        int height,       //height of source;
                        imgpel* dst,
                        int dststep,
                        int Cs 
                     );
#endif

