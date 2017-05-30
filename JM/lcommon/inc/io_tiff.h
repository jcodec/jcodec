/*!
************************************************************************
* \file io_tiff.h
*
* \brief
*    I/O functions related to tiff images
*    Part of code was based on libtiff (see http://www.libtiff.org/)
*
* \author
*     - Larry Luther                    <lzl@dolby.com>
*     - Alexis Michael Tourapis         <alexismt@ieee.org>
*
************************************************************************
*/

#ifndef _IO_TIFF_H_
#define _IO_TIFF_H_
// See TIFF 6.0 Specification
// http://partners.adobe.com/public/developer/tiff/index.html


extern int ReadTIFFImage (InputParameters *p_Inp, VideoDataFile *input_file, int FrameNoInFile, FrameFormat *source, unsigned char *buf);

#endif
