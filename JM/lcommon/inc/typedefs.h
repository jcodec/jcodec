/*!
 *************************************************************************************
 * \file typedefs.h
 *
 * \brief
 *    Common type definitions
 *    Currently only supports Windows and Linux operating systems. 
 *    Need to add support for other "older systems such as VAX, DECC, Unix Alpha etc
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>
 *************************************************************************************
 */

#ifndef _TYPEDEFS_H_
#define _TYPEDEFS_H_

#include "win32.h"

typedef unsigned char  byte;     //!< byte type definition
typedef unsigned char  uint8;    //!< type definition for unsigned char (same as byte, 8 bits)
typedef unsigned short uint16;   //!< type definition for unsigned short (16 bits)
typedef unsigned int   uint32;   //!< type definition for unsigned int (32 bits)

typedef          char  int8;
typedef          short int16;
typedef          int   int32;

#if IMGTYPE == 0
typedef byte   imgpel;           //!< pixel type
typedef uint16 distpel;          //!< distortion type (for pixels)
typedef int32  distblk;          //!< distortion type (for Macroblock)
typedef int32  transpel;         //!< transformed coefficient type
#else
typedef uint16 imgpel;
typedef uint32 distpel;
typedef int64  distblk;
typedef int32  transpel;
#endif

//! Boolean Type
#ifdef FALSE
#  define Boolean int
#else
typedef enum {
  FALSE,
  TRUE
} Boolean;
#endif

#ifndef MAXINT64
#define MAXINT64     0x7fffffffffffffff
#endif

/*
#define MAXUINT8     0xff
#define MAXUINT16    0xffff
#define MAXUINT32    0xffffffff
#define MAXUINT64    0xffffffffffffffff

#define MAXINT8      0x7f
#define MININT8      (-MAXINT8)
#define MAXINT16     0x7fff
#define MININT16     (-MAXINT16)
#define MAXINT32     0x7fffffff
#define MININT32     (-MAXINT32)
#define MAXINT64     0x7fffffffffffffff
#define MININT64     (-MAXINT64)
*/

#endif

