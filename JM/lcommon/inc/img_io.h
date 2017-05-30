/*!
 *************************************************************************************
 * \file img_io.h
 *
 * \brief
 *    image I/O related functions
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>
 *************************************************************************************
 */
#include "global.h"

#ifndef _IMG_IO_H_
#define _IMG_IO_H_

#include "io_video.h"
#include "io_raw.h"
#include "io_tiff.h"

extern int ParseSizeFromString           (VideoDataFile *input_file, int *xlen, int *ylen, double *fps);
extern void ParseFrameNoFormatFromString (VideoDataFile *input_file);
extern void OpenFrameFile                (VideoDataFile *input_file, int FrameNumberInFile);
extern void OpenFiles                    (VideoDataFile *input_file);
extern void CloseFiles                   (VideoDataFile *input_file);
extern VideoFileType ParseVideoType      (VideoDataFile *input_file);

#endif

