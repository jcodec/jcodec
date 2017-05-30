
/*!
 ************************************************************************
 * \file io_raw.h
 *
 * \brief
 *    I/O functions related to raw images
 *
 * \author
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>
 *
 ************************************************************************
 */

#ifndef _IO_RAW_H_
#define _IO_RAW_H_

extern int ReadFrameConcatenated  (InputParameters *p_Inp, VideoDataFile *input_file, int FrameNoInFile, int HeaderSize, FrameFormat *source, unsigned char *buf);
extern int ReadFrameSeparate      (InputParameters *p_Inp, VideoDataFile *input_file, int FrameNoInFile, int HeaderSize, FrameFormat *source, unsigned char *buf);

#endif

