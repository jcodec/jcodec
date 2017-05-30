/*!
 ************************************************************************
 * \file input.h
 *
 * \brief
 *    Input related definitions
 *
 * \author
 *
 ************************************************************************
 */

#ifndef _INPUT_H_
#define _INPUT_H_

extern int testEndian(void);
extern void initInput(VideoParameters *p_Vid, FrameFormat *source, FrameFormat *output);
extern void AllocateFrameMemory (VideoParameters *p_Vid, InputParameters *p_Inp, FrameFormat *source);
extern void DeleteFrameMemory (VideoParameters *p_Vid);

extern int  read_one_frame (VideoParameters *p_Vid, VideoDataFile *input_file, int FrameNoInFile, int HeaderSize, FrameFormat *source, FrameFormat *output, imgpel **pImage[3]);
extern void pad_borders    ( FrameFormat output, int img_size_x, int img_size_y, int img_size_x_cr, int img_size_y_cr, imgpel **pImage[3]);

#endif

