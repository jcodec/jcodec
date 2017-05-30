/*!
 ************************************************************************
 * \file frame.h
 *
 * \brief
 *    headers for frame format related information
 *
 * \author
 *
 ************************************************************************
 */
#ifndef _FRAME_H_
#define _FRAME_H_

typedef enum {
  CM_UNKNOWN = -1,
  CM_YUV     =  0,
  CM_RGB     =  1,
  CM_XYZ     =  2
} ColorModel;

typedef enum {
  CF_UNKNOWN = -1,     //!< Unknown color format
  YUV400     =  0,     //!< Monochrome
  YUV420     =  1,     //!< 4:2:0
  YUV422     =  2,     //!< 4:2:2
  YUV444     =  3      //!< 4:4:4
} ColorFormat;

typedef enum {
  PF_UNKNOWN = -1,     //!< Unknown color ordering
  UYVY       =  0,     //!< UYVY
  YUY2       =  1,     //!< YUY2
  YUYV       =  1,     //!< YUYV
  YVYU       =  2,     //!< YVYU
  BGR        =  3,     //!< BGR
  V210       =  4      //!< Video Clarity 422 format (10 bits)
} PixelFormat;


typedef struct frame_format
{  
  ColorFormat yuv_format;                    //!< YUV format (0=4:0:0, 1=4:2:0, 2=4:2:2, 3=4:4:4)
  ColorModel  color_model;                   //!< 4:4:4 format (0: YUV, 1: RGB, 2: XYZ)
  PixelFormat pixel_format;                  //!< pixel format support for certain interleaved yuv sources
  double      frame_rate;                    //!< frame rate
  int         width[3];                      //!< component frame width
  int         height[3];                     //!< component frame height    
  int         auto_crop_right;               //!< luma component auto crop right
  int         auto_crop_bottom;              //!< luma component auto crop bottom
  int         auto_crop_right_cr;            //!< chroma component auto crop right
  int         auto_crop_bottom_cr;           //!< chroma component auto crop bottom
  int         width_crop;                    //!< width after cropping consideration
  int         height_crop;                   //!< height after cropping consideration
  int         mb_width;                      //!< luma component frame width
  int         mb_height;                     //!< luma component frame height    
  int         size_cmp[3];                   //!< component sizes (width * height)
  int         size;                          //!< total image size (sum of size_cmp)
  int         bit_depth[3];                  //!< component bit depth  
  int         max_value[3];                  //!< component max value
  int         max_value_sq[3];               //!< component max value squared
  int         pic_unit_size_on_disk;         //!< picture sample unit size on storage medium
  int         pic_unit_size_shift3;          //!< pic_unit_size_on_disk >> 3
} FrameFormat;

#endif
