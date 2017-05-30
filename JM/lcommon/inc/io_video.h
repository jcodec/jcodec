/*!
 ************************************************************************
 * \file io_video.h
 *
 * \brief
 *    Video I/O 
 *
 * \author
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>
 *
 ************************************************************************
 */

#ifndef _IO_VIDEO_H_
#define _IO_VIDEO_H_

#include "frame.h"

typedef struct video_size {
  char* name;
  int x_size;
  int y_size;
} VIDEO_SIZE;

typedef enum {
  VIDEO_UNKNOWN = -1,
  VIDEO_YUV     =  0,
  VIDEO_RGB     =  1,
  VIDEO_XYZ     =  2,
  VIDEO_TIFF    =  3,
  VIDEO_AVI     =  4
} VideoFileType;

typedef struct video_data_file
{
  //char*         fname;          //!< video file name
  char          fname[FILE_NAME_SIZE]; //!< video file name
  char          fhead[FILE_NAME_SIZE]; //!< header of video file
  char          ftail[FILE_NAME_SIZE]; //!< tail of video file
  int           f_num;                 //!< video file number     
  VideoFileType vdtype;                //!< File format
  FrameFormat   format;                //!< video format information
  int           is_concatenated;       //!< Single or multifile input?
  int           is_interleaved;        //!< Support for interleaved and non-interleaved input sources
  int           zero_pad;              //!< Used when separate image files are used as input. Enables zero padding for file numbering
  int           num_digits;            //!< Number of digits for file numbering
  int           start_frame;           //!< start frame
  int           end_frame;             //!< end frame
  int           nframes;               //!< number of frames
  int           crop_x_size;           //!< crop information (x component)
  int           crop_y_size;           //!< crop information (y component)
  int           crop_x_offset;         //!< crop offset (x component);
  int           crop_y_offset;         //!< crop offset (y component);

  // AVI related information to be added here
  int* avi;
  //avi_t* avi;
  //int    header;
  //char   compressor[8];    
} VideoDataFile;

#endif

