
/*!
 *************************************************************************************
 * \file input.c
 *
 * \brief
 *    Input related functions
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Karsten Suehring
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>
 *     
 *************************************************************************************
 */
#include "contributors.h"

#include <math.h>
#include <time.h>

#include "global.h"
#include "defines.h"
#include "input.h"
#include "img_io.h"
#include "memalloc.h"
#include "fast_memory.h"

void buf2img_basic    ( imgpel** imgX, unsigned char* buf, int size_x, int size_y, int o_size_x, int o_size_y, int symbol_size_in_bytes, int bitshift);
void buf2img_endian   ( imgpel** imgX, unsigned char* buf, int size_x, int size_y, int o_size_x, int o_size_y, int symbol_size_in_bytes, int bitshift);
void buf2img_bitshift ( imgpel** imgX, unsigned char* buf, int size_x, int size_y, int o_size_x, int o_size_y, int symbol_size_in_bytes, int bitshift);
void fillPlane        ( imgpel** imgX, int nVal, int size_x, int size_y);

/*!
 ************************************************************************
 * \brief
 *      checks if the System is big- or little-endian
 * \return
 *      0, little-endian (e.g. Intel architectures)
 *      1, big-endian (e.g. SPARC, MIPS, PowerPC)
 ************************************************************************
 */
void initInput(VideoParameters *p_Vid, FrameFormat *source, FrameFormat *output)
{
  if (source->bit_depth[0] == output->bit_depth[0] && source->bit_depth[1] == output->bit_depth[1])
  {
    if (( sizeof(char) != sizeof (imgpel)) && testEndian())
      p_Vid->buf2img = buf2img_endian;
    else
      p_Vid->buf2img = buf2img_basic;
  }
  else
    p_Vid->buf2img = buf2img_bitshift;
}


/*!
 ************************************************************************
 * \brief
 *      checks if the System is big- or little-endian
 * \return
 *      0, little-endian (e.g. Intel architectures)
 *      1, big-endian (e.g. SPARC, MIPS, PowerPC)
 ************************************************************************
 */
int testEndian(void)
{
  short s;
  byte *p;

  p=(byte*)&s;

  s=1;

  return (*p==0);
}

#if (DEBUG_BITDEPTH)
/*!
 ************************************************************************
 * \brief
 *    Masking to ensure data within appropriate range
 ************************************************************************
 */
static void MaskMSBs (imgpel** imgX, int mask, int width, int height)
{
  int i,j;

  for (j=0; j < height; j++)
  {
    for (i=0; i < width; i++)
    {
      imgX[j][i]=(imgpel) (imgX[j][i] & mask);
    }
  }
}
#endif

/*!
 ************************************************************************
 * \brief
 *    Fill plane with constant value
 ************************************************************************
 */
void fillPlane ( imgpel** imgX,                 //!< Pointer to image plane
                 int nVal,                      //!< Fill value (currently 0 <= nVal < 256)
                 int size_x,                    //!< horizontal size of picture
                 int size_y                     //!< vertical size of picture
                )
{
  int j, i;

  if (sizeof(imgpel) == sizeof(char))
  {
    fast_memset(imgX[0], nVal, size_y * size_x);
  }
  else
  {
    for (j = 0; j < size_y; j++) 
    {
      for (i = 0; i < size_x; i++) 
      {
        imgX[j][i] = (imgpel) nVal;
      }
    }
  }
}

static void deinterleave_yuv420( unsigned char** input,       //!< input buffer
  unsigned char** output,      //!< output buffer
  FrameFormat *source,         //!< format of source buffer
  int symbol_size_in_bytes     //!< number of bytes per symbol
  ) 
{
  int i;  
  // original buffer
  unsigned char *icmp  = *input;
  // final buffer
  unsigned char *ocmp0 = *output;

  unsigned char *ocmp1 = ocmp0 + symbol_size_in_bytes * source->size_cmp[Y_COMP];
  unsigned char *ocmp2 = ocmp1 + symbol_size_in_bytes * source->size_cmp[U_COMP];

  for (i = 0; i < source->size_cmp[U_COMP]; i++) 
  {
    memcpy(ocmp1, icmp, symbol_size_in_bytes);
    ocmp1 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    memcpy(ocmp0, icmp, 2 * symbol_size_in_bytes);
    ocmp0 += 2 * symbol_size_in_bytes;
    icmp  += 2 * symbol_size_in_bytes;
    memcpy(ocmp2, icmp, symbol_size_in_bytes);
    ocmp2 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    memcpy(ocmp0, icmp, 2 * symbol_size_in_bytes);
    ocmp0 += 2 * symbol_size_in_bytes;
    icmp  += 2 * symbol_size_in_bytes;
  }

  // flip buffers
  icmp    = *input;
  *input  = *output;
  *output = icmp;  
}

static void deinterleave_yuv444( unsigned char** input,       //!< input buffer
  unsigned char** output,      //!< output buffer
  FrameFormat *source,         //!< format of source buffer
  int symbol_size_in_bytes     //!< number of bytes per symbol
  ) 
{
  int i;  
  // original buffer
  unsigned char *icmp  = *input;
  // final buffer
  unsigned char *ocmp0 = *output;

  unsigned char *ocmp1 = ocmp0 + symbol_size_in_bytes * source->size_cmp[Y_COMP];
  unsigned char *ocmp2 = ocmp1 + symbol_size_in_bytes * source->size_cmp[U_COMP];

  for (i = 0; i < source->size_cmp[Y_COMP]; i++) 
  {
    memcpy(ocmp0, icmp, symbol_size_in_bytes);
    ocmp0 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    memcpy(ocmp1, icmp, symbol_size_in_bytes);
    ocmp1 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    memcpy(ocmp2, icmp, symbol_size_in_bytes);
    ocmp2 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
  }
  // flip buffers
  icmp    = *input;
  *input  = *output;
  *output = icmp;
}

static void deinterleave_yuyv ( unsigned char** input,       //!< input buffer
  unsigned char** output,      //!< output buffer
  FrameFormat *source,         //!< format of source buffer
  int symbol_size_in_bytes     //!< number of bytes per symbol
  ) 
{
  int i;  
  // original buffer
  unsigned char *icmp = *input;
  // final buffer
  unsigned char *ocmp0 = *output;

  unsigned char *ocmp1 = ocmp0 + symbol_size_in_bytes * source->size_cmp[Y_COMP];
  unsigned char *ocmp2 = ocmp1 + symbol_size_in_bytes * source->size_cmp[U_COMP];

  for (i = 0; i < source->size_cmp[U_COMP]; i++) 
  {
    // Y
    memcpy(ocmp0, icmp, symbol_size_in_bytes);
    ocmp0 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // U
    memcpy(ocmp1, icmp, symbol_size_in_bytes);
    ocmp1 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // Y
    memcpy(ocmp0, icmp, symbol_size_in_bytes);
    ocmp0 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // V
    memcpy(ocmp2, icmp, symbol_size_in_bytes);
    ocmp2 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
  }
  // flip buffers
  icmp    = *input;
  *input  = *output;
  *output = icmp;
}

static void deinterleave_yvyu ( unsigned char** input,       //!< input buffer
  unsigned char** output,      //!< output buffer
  FrameFormat *source,         //!< format of source buffer
  int symbol_size_in_bytes     //!< number of bytes per symbol
  ) 
{
  int i;  
  // original buffer
  unsigned char *icmp  = *input;
  // final buffer
  unsigned char *ocmp0 = *output;

  unsigned char *ocmp1 = ocmp0 + symbol_size_in_bytes * source->size_cmp[Y_COMP];
  unsigned char *ocmp2 = ocmp1 + symbol_size_in_bytes * source->size_cmp[U_COMP];

  for (i = 0; i < source->size_cmp[U_COMP]; i++) 
  {
    // Y
    memcpy(ocmp0, icmp, symbol_size_in_bytes);
    ocmp0 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // V
    memcpy(ocmp2, icmp, symbol_size_in_bytes);
    ocmp2 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // Y
    memcpy(ocmp0, icmp, symbol_size_in_bytes);
    ocmp0 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // U
    memcpy(ocmp1, icmp, symbol_size_in_bytes);
    ocmp1 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
  }
  // flip buffers
  icmp    = *input;
  *input  = *output;
  *output = icmp;
}

static void deinterleave_uyvy ( unsigned char** input,       //!< input buffer
  unsigned char** output,      //!< output buffer
  FrameFormat *source,         //!< format of source buffer
  int symbol_size_in_bytes     //!< number of bytes per symbol
  ) 
{
  int i;  
  // original buffer
  unsigned char *icmp  = *input;
  // final buffer
  unsigned char *ocmp0 = *output;

  unsigned char *ocmp1 = ocmp0 + symbol_size_in_bytes * source->size_cmp[Y_COMP];
  unsigned char *ocmp2 = ocmp1 + symbol_size_in_bytes * source->size_cmp[U_COMP];

  for (i = 0; i < source->size_cmp[U_COMP]; i++) 
  {
    // U
    memcpy(ocmp1, icmp, symbol_size_in_bytes);
    ocmp1 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // Y
    memcpy(ocmp0, icmp, symbol_size_in_bytes);
    ocmp0 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // V
    memcpy(ocmp2, icmp, symbol_size_in_bytes);
    ocmp2 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
    // Y
    memcpy(ocmp0, icmp, symbol_size_in_bytes);
    ocmp0 += symbol_size_in_bytes;
    icmp  += symbol_size_in_bytes;
  }
  // flip buffers
  icmp    = *input;
  *input  = *output;
  *output = icmp;
}

static void deinterleave_v210 ( unsigned char** input,       //!< input buffer
  unsigned char** output,      //!< output buffer
  FrameFormat *source,         //!< format of source buffer
  int symbol_size_in_bytes     //!< number of bytes per symbol
  ) 
{
  int i;  
  // original buffer
  unsigned char *icmp  = *input;

  unsigned int   *ui32cmp = (unsigned int *) *input;
  unsigned short *ui16cmp0 = (unsigned short *) *output;
  unsigned short *ui16cmp1 = ui16cmp0 + source->size_cmp[0];
  unsigned short *ui16cmp2 = ui16cmp1 + source->size_cmp[1];

  for (i = 0; i < source->size_cmp[U_COMP] / 3; i++) 
  {
    // Byte 3          Byte 2          Byte 1          Byte 0
    // Cr 0                Y 0                 Cb 0
    // X X 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0
    *ui16cmp2   = (*ui32cmp & 0x3FF00000)>>20;         // Cr 0
    *ui16cmp0   = (*ui32cmp & 0xffc00)>>10;            // Y 0
    *ui16cmp1   = (*(ui32cmp++) & 0x3FF);              // Cb 0

    // Byte 7          Byte 6          Byte 5          Byte 4
    // Y 2                 Cb 1                Y 1
    // X X 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0
    *(ui16cmp0 + 2) = (*ui32cmp & 0x3FF00000)>>20;     // Y 2
    *(ui16cmp1 + 1) = (*ui32cmp & 0xffc00)>>10;        // Cb 1
    *(ui16cmp0 + 1) = (*(ui32cmp++) & 0x3FF);          // Y 1

    // Byte 11         Byte 10         Byte 9          Byte 8
    // Cb 2                Y 3                 Cr 1
    // X X 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0
    *(ui16cmp1 + 2) = (*ui32cmp & 0x3FF00000)>>20;     // Cb 2
    *(ui16cmp0 + 3) = (*ui32cmp & 0xffc00)>>10;        // Y 3
    *(ui16cmp2 + 1) = (*(ui32cmp++) & 0x3FF);          // Cr 1

    // Byte 15         Byte 14         Byte 13         Byte 12
    // Y 5                 Cr 2                Y 4
    // X X 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0
    *(ui16cmp0 + 5) = (*ui32cmp & 0x3FF00000)>>20;     // Y 2
    *(ui16cmp2 + 2) = (*ui32cmp & 0xffc00)>>10;        // Cb 1
    *(ui16cmp0 + 4) = (*(ui32cmp++) & 0x3FF);          // Y 1

    //printf("luma: %d %d %d %d %d %d\n", *(ui16cmp0 + 0), *(ui16cmp0 + 1), *(ui16cmp0 + 2), *(ui16cmp0 + 3), *(ui16cmp0 + 4), *(ui16cmp0 + 5));

    ui16cmp0 += 6;
    ui16cmp1 += 3;
    ui16cmp2 += 3;
  }
  // flip buffers
  icmp    = *input;
  *input  = *output;
  *output = icmp;
}

/*!
 ************************************************************************
 * \brief
 *    Deinterleave file read buffer to source picture structure
 ************************************************************************
 */
static void deinterleave ( unsigned char** input,       //!< input buffer
                           unsigned char** output,      //!< output buffer
                           FrameFormat *source,         //!< format of source buffer
                           int symbol_size_in_bytes     //!< number of bytes per symbol
                          )
{
  if (source->yuv_format == YUV420) 
  { // UYYVYY 
    deinterleave_yuv420(input, output, source, symbol_size_in_bytes);
  }  
  else if (source->yuv_format == YUV422)
  {
    if (source->pixel_format == YUYV || source->pixel_format == YUY2) 
    {
      deinterleave_yuyv(input, output, source, symbol_size_in_bytes);
    }
    else if (source->pixel_format == YVYU)
    {
      deinterleave_yvyu(input, output, source, symbol_size_in_bytes);
    }
    else if (source->pixel_format == UYVY) 
    {
      deinterleave_uyvy(input, output, source, symbol_size_in_bytes);
    }
    else if (source->pixel_format == V210) 
    {
      deinterleave_v210(input, output, source, symbol_size_in_bytes);
    }
    else {
      fprintf(stderr, "Unsupported pixel format.\n");
      exit(EXIT_FAILURE);
    }
  }
  else if (source->yuv_format == YUV444)  
    deinterleave_yuv444(input, output, source, symbol_size_in_bytes);
}

/*!
 ************************************************************************
 * \brief
 *    Convert file read buffer to source picture structure
 ************************************************************************
 */
void buf2img_bitshift ( imgpel** imgX,            //!< Pointer to image plane
                        unsigned char* buf,       //!< Buffer for file output
                        int size_x,               //!< horizontal size of picture
                        int size_y,               //!< vertical size of picture
                        int o_size_x,             //!< horizontal size of picture
                        int o_size_y,             //!< vertical size of picture
                        int symbol_size_in_bytes, //!< number of bytes in file used for one pixel
                        int bitshift              //!< variable for bitdepth expansion
                       )
{
  int i,j;

  uint16 tmp16, ui16;
  unsigned long  tmp32, ui32;

  // This test should be done once.
  if (((symbol_size_in_bytes << 3) - bitshift) > (sizeof(imgpel)<< 3))
  {
    error ("Source picture has higher bit depth than imgpel data type. \nPlease recompile with larger data type for imgpel.", 500);
  }

  if (testEndian())
  {
    if (size_x != o_size_x || size_y != o_size_y)
    {
      error ("Rescaling not supported in big endian architectures. ", 500);
    }

    // big endian
    switch (symbol_size_in_bytes)
    {
    case 1:
      {
        for(j = 0; j < o_size_y; j++)
          for(i = 0; i < o_size_x; i++)
          {
            imgX[j][i]= (imgpel) rshift_rnd(buf[i + j*size_x], bitshift);
          }
          break;
      }
    case 2:
      {
        for(j = 0; j < o_size_y; j++)
          for(i = 0; i < o_size_x; i++)
          {
            memcpy(&tmp16, buf + ((i + j * size_x) * 2), 2);
            ui16  = (tmp16 >> 8) | ((tmp16 & 0xFF) << 8);
            imgX[j][i] = (imgpel) rshift_rnd(ui16, bitshift);
          }
          break;
      }
    case 4:
      {
        for(j = 0; j < o_size_y; j++)
          for(i = 0; i < o_size_x; i++)
          {
            memcpy(&tmp32, buf + ((i + j * size_x) * 4), 4);
            ui32  = ((tmp32 & 0xFF00) << 8) | ((tmp32 & 0xFF)<<24) | ((tmp32 & 0xFF0000)>>8) | ((tmp32 & 0xFF000000)>>24);
            imgX[j][i] = (imgpel) rshift_rnd(ui32, bitshift);
          }
      }
    default:
      {
        error ("reading only from formats of 8, 16 or 32 bit allowed on big endian architecture", 500);
        break;
      }
    }
  }
  else
  {
    // little endian
    int j_pos;
    if (size_x == o_size_x && size_y == o_size_y)
    {
      for (j = 0; j < o_size_y; j++)
      {
        j_pos = j*size_x;
        for (i = 0; i < o_size_x; i++)
        {
          ui16=0;
          memcpy(&(ui16), buf + ((i + j_pos) * symbol_size_in_bytes), symbol_size_in_bytes);
          imgX[j][i] = (imgpel) rshift_rnd(ui16,bitshift);
        }
      }  
    }
    else
    {
      int iminwidth   = imin(size_x, o_size_x);
      int iminheight  = imin(size_y, o_size_y);
      int dst_offset_x  = 0, dst_offset_y = 0;        
      int offset_x = 0, offset_y = 0; // currently not used

      // determine whether we need to center the copied frame or crop it
      if ( o_size_x >= size_x ) 
        dst_offset_x = ( o_size_x  - size_x  ) >> 1;

      if (o_size_y >= size_y) 
        dst_offset_y = ( o_size_y - size_y ) >> 1;

      // check copied area to avoid copying memory garbage
      // source
      iminwidth  =  ( (offset_x + iminwidth ) > size_x ) ? (size_x  - offset_x) : iminwidth;
      iminheight =  ( (offset_y + iminheight) > size_y ) ? (size_y - offset_y) : iminheight;
      // destination
      iminwidth  =  ( (dst_offset_x + iminwidth ) > o_size_x  ) ? (o_size_x  - dst_offset_x) : iminwidth;
      iminheight =  ( (dst_offset_y + iminheight) > o_size_y )  ? (o_size_y - dst_offset_y) : iminheight;

      for (j=0; j < iminheight; j++)
      {        
        j_pos = (j + offset_y) * size_x + offset_x;
        for (i=0; i < iminwidth; i++)
        {
          ui16=0;
          memcpy(&(ui16), buf + ((i + j_pos) * symbol_size_in_bytes), symbol_size_in_bytes);
          imgX[j + dst_offset_y][i + dst_offset_x] = (imgpel) rshift_rnd(ui16,bitshift);
        }
      }    
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    Convert file read buffer to source picture structure
 ************************************************************************
 */
void buf2img_basic (imgpel** imgX,            //!< Pointer to image plane
                    unsigned char* buf,       //!< Buffer for file output
                    int size_x,               //!< horizontal size of picture
                    int size_y,               //!< vertical size of picture
                    int o_size_x,             //!< horizontal size of picture
                    int o_size_y,             //!< vertical size of picture
                    int symbol_size_in_bytes, //!< number of bytes in file used for one pixel
                    int dummy                 //!< dummy variable used for allowing function pointer use
                    )
{
  int i,j;
  unsigned char* temp_buf = buf;

  if (symbol_size_in_bytes> sizeof(imgpel))
  {
    error ("Source picture has higher bit depth than imgpel data type. \nPlease recompile with larger data type for imgpel.", 500);
  }

  if (( sizeof (imgpel) == symbol_size_in_bytes))
  {    
    // imgpel == pixel_in_file -> simple copy
    if (size_x == o_size_x && size_y == o_size_y)
      memcpy(&imgX[0][0], temp_buf, size_x * size_y * sizeof(imgpel));
    else
    {
      int iminwidth   = imin(size_x, o_size_x);
      int iminheight  = imin(size_y, o_size_y);
      int dst_offset_x  = 0, dst_offset_y = 0;
      int offset_x = 0, offset_y = 0; // currently not used

      // determine whether we need to center the copied frame or crop it
      if ( o_size_x >= size_x ) 
        dst_offset_x = ( o_size_x  - size_x  ) >> 1;

      if (o_size_y >= size_y) 
        dst_offset_y = ( o_size_y - size_y ) >> 1;

      // check copied area to avoid copying memory garbage
      // source
      iminwidth  =  ( (offset_x + iminwidth ) > size_x ) ? (size_x  - offset_x) : iminwidth;
      iminheight =  ( (offset_y + iminheight) > size_y ) ? (size_y - offset_y) : iminheight;
      // destination
      iminwidth  =  ( (dst_offset_x + iminwidth ) > o_size_x  ) ? (o_size_x  - dst_offset_x) : iminwidth;
      iminheight =  ( (dst_offset_y + iminheight) > o_size_y )  ? (o_size_y - dst_offset_y) : iminheight;

      for (i=0; i<iminheight;i++) {
        memcpy(&imgX[i + dst_offset_y][dst_offset_x], &(temp_buf[(i + offset_y) * size_x + offset_x]), iminwidth * sizeof(imgpel));
      }
    }
  }
  else
  {
    int j_pos;
    uint16 ui16;
    if (size_x == o_size_x && size_y == o_size_y)
    {
      for (j=0; j < o_size_y; j++)
      {
        j_pos = j * size_x;
        for (i=0; i < o_size_x; i++)
        {
          ui16=0;          
          memcpy(&(ui16), buf + ((i + j_pos) * symbol_size_in_bytes), symbol_size_in_bytes);
          imgX[j][i]= (imgpel) ui16;
        }
      }    
    }
    else
    {
      int iminwidth   = imin(size_x, o_size_x);
      int iminheight  = imin(size_y, o_size_y);
      int dst_offset_x  = 0, dst_offset_y = 0;
      int offset_x = 0, offset_y = 0; // currently not used

      // determine whether we need to center the copied frame or crop it
      if ( o_size_x >= size_x ) 
        dst_offset_x = ( o_size_x  - size_x  ) >> 1;

      if (o_size_y >= size_y) 
        dst_offset_y = ( o_size_y - size_y ) >> 1;

      // check copied area to avoid copying memory garbage
      // source
      iminwidth  =  ( (offset_x + iminwidth ) > size_x ) ? (size_x  - offset_x) : iminwidth;
      iminheight =  ( (offset_y + iminheight) > size_y ) ? (size_y - offset_y) : iminheight;
      // destination
      iminwidth  =  ( (dst_offset_x + iminwidth ) > o_size_x  ) ? (o_size_x  - dst_offset_x) : iminwidth;
      iminheight =  ( (dst_offset_y + iminheight) > o_size_y )  ? (o_size_y - dst_offset_y) : iminheight;

      for (j = 0; j < iminheight; j++) 
      {
        memcpy(&imgX[j + dst_offset_y][dst_offset_x], &(temp_buf[(j + offset_y) * size_x + offset_x]), iminwidth * symbol_size_in_bytes);
      }
      for (j=0; j < iminheight; j++)
      {        
        j_pos = (j + offset_y) * size_x + offset_x;
        for (i=0; i < iminwidth; i++)
        {
          ui16 = 0;
          memcpy(&(ui16), buf + ((i + j_pos) * symbol_size_in_bytes), symbol_size_in_bytes);
          imgX[j + dst_offset_y][i + dst_offset_x]= (imgpel) ui16;
        }
      }    
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Convert file read buffer to source picture structure
 ************************************************************************
 */
void buf2img_endian (imgpel** imgX,            //!< Pointer to image plane
                     unsigned char* buf,       //!< Buffer for file output
                     int size_x,               //!< horizontal size of picture
                     int size_y,               //!< vertical size of picture
                     int o_size_x,             //!< horizontal size of picture
                     int o_size_y,             //!< vertical size of picture
                     int symbol_size_in_bytes, //!< number of bytes in file used for one pixel
                     int dummy                 //!< dummy variable used for allowing function pointer use
                     )
{
  int i,j;

  uint16 tmp16, ui16;
  unsigned long  tmp32, ui32;

  if (symbol_size_in_bytes > sizeof(imgpel))
  {
    error ("Source picture has higher bit depth than imgpel data type. \nPlease recompile with larger data type for imgpel.", 500);
  }

  if (size_x != o_size_x || size_y != o_size_y)
  {
    error ("Rescaling not supported in big endian architectures. ", 500);
  }

  // big endian
  switch (symbol_size_in_bytes)
  {
  case 1:
    {
      for(j=0;j<size_y;j++)
      {
        for(i=0;i<size_x;i++)
        {
          imgX[j][i]= (imgpel) buf[i + j*size_x];
        }
      }
      break;
    }
  case 2:
    {
      for(j=0;j<size_y;j++)
      {
        for(i=0;i<size_x;i++)
        {
          memcpy(&tmp16, buf+((i+j*size_x)*2), 2);
          ui16  = (tmp16 >> 8) | ((tmp16&0xFF)<<8);
          imgX[j][i] = (imgpel) ui16;
        }
      }
      break;
    }
  case 4:
    {
      for(j=0;j<size_y;j++)
      {
        for(i=0;i<size_x;i++)
        {
          memcpy(&tmp32, buf+((i+j*size_x)*4), 4);
          ui32  = ((tmp32&0xFF00)<<8) | ((tmp32&0xFF)<<24) | ((tmp32&0xFF0000)>>8) | ((tmp32&0xFF000000)>>24);
          imgX[j][i] = (imgpel) ui32;
        }
      }
      break;
    }
  default:
    {
      error ("reading only from formats of 8, 16 or 32 bit allowed on big endian architecture", 500);
      break;
    }
  }   
}

/*!
 ************************************************************************
 * \brief
 *    Create Frame Memory buffer
 *
 ************************************************************************
 */
void AllocateFrameMemory (VideoParameters *p_Vid, InputParameters *p_Inp, FrameFormat *source)
{
  // Note that size seems to be ok even for v210 formats (wasteful yes, but should not 
  // create any issues with how we manage that format.
  if (NULL == (p_Vid->buf = malloc (source->size * source->pic_unit_size_shift3)))
    no_mem_exit("AllocateFrameMemory: p_Vid->buf");
  if (p_Inp->input_file1.is_interleaved)
  {
    if (NULL == (p_Vid->ibuf = malloc (source->size * source->pic_unit_size_shift3)))
      no_mem_exit("AllocateFrameMemory: p_Vid->ibuf");
  }
}

/*!
 ************************************************************************
 * \brief
 *    Delete Frame Memory buffer
 *
 ************************************************************************
 */
void DeleteFrameMemory (VideoParameters *p_Vid)
{
  if (p_Vid->buf != NULL)
    free (p_Vid->buf);
  if (p_Vid->ibuf != NULL)
    free (p_Vid->ibuf);
}

/*!
 ************************************************************************
 * \brief
 *    Reads one new frame from file
 *
 * \param input_file
 *    structure containing information (filename, format) about the source file
 * \param FrameNoInFile
 *    Frame number in the source file
 * \param HeaderSize
 *    Number of bytes in the source file to be skipped
 * \param source
 *    source file (on disk) information 
 * \param output
 *    output file (for encoding) information
 * \param pImage
 *    Image planes
 ************************************************************************
 */
int read_one_frame (VideoParameters *p_Vid, VideoDataFile *input_file, int FrameNoInFile, int HeaderSize, FrameFormat *source, FrameFormat *output, imgpel **pImage[3])
{
  InputParameters *p_Inp = p_Vid->p_Inp;
  int file_read = 0;
  unsigned int symbol_size_in_bytes = source->pic_unit_size_shift3;

  const int bytes_y  = source->size_cmp[0] * symbol_size_in_bytes;
  const int bytes_uv = source->size_cmp[1] * symbol_size_in_bytes;
  int bit_scale;  

  Boolean rgb_input = (Boolean) (source->color_model == CM_RGB && source->yuv_format == YUV444);

  if (input_file->is_concatenated == 0)
  {    
    if (input_file->vdtype == VIDEO_TIFF)
    {
      file_read = ReadTIFFImage     (p_Inp, input_file, FrameNoInFile, source, p_Vid->buf);
    }
    else
    {
      file_read = ReadFrameSeparate (p_Inp, input_file, FrameNoInFile, HeaderSize, source, p_Vid->buf);
    }
  }
  else
  {
    file_read = ReadFrameConcatenated (p_Inp, input_file, FrameNoInFile, HeaderSize, source, p_Vid->buf);
  }

  if ( !file_read )
  {
    return 0;
  }

  // De-interleave input source
  if (input_file->is_interleaved)
  {
    deinterleave ( &p_Vid->buf, &p_Vid->ibuf, source, symbol_size_in_bytes);
  }

  bit_scale = source->bit_depth[0] - output->bit_depth[0];  

  if(rgb_input)
    p_Vid->buf2img(pImage[0], p_Vid->buf + bytes_y, source->width[0], source->height[0], output->width[0], output->height[0], symbol_size_in_bytes, bit_scale);
  else
    p_Vid->buf2img(pImage[0], p_Vid->buf, source->width[0], source->height[0], output->width[0], output->height[0], symbol_size_in_bytes, bit_scale);

#if (DEBUG_BITDEPTH)
  MaskMSBs(pImage[0], ((1 << output->bit_depth[0]) - 1), output->width[0], output->height[0]);
#endif

  if (p_Vid->yuv_format != YUV400)
  {
    bit_scale = source->bit_depth[1] - output->bit_depth[1];
#if (ALLOW_GRAYSCALE)
    if (!p_Inp->grayscale) 
#endif
    {
      if(rgb_input)
        p_Vid->buf2img(pImage[1], p_Vid->buf + bytes_y + bytes_uv, source->width[1], source->height[1], output->width[1], output->height[1], symbol_size_in_bytes, bit_scale);
      else 
        p_Vid->buf2img(pImage[1], p_Vid->buf + bytes_y, source->width[1], source->height[1], output->width[1], output->height[1], symbol_size_in_bytes, bit_scale);

      bit_scale = source->bit_depth[2] - output->bit_depth[2];
      if(rgb_input)
        p_Vid->buf2img(pImage[2], p_Vid->buf, source->width[1], source->height[1], output->width[1], output->height[1], symbol_size_in_bytes, bit_scale);
      else
        p_Vid->buf2img(pImage[2], p_Vid->buf + bytes_y + bytes_uv, source->width[1], source->height[1], output->width[1], output->height[1], symbol_size_in_bytes, bit_scale);
    }
#if (DEBUG_BITDEPTH)
    MaskMSBs(pImage[1], ((1 << output->bit_depth[1]) - 1), output->width[1], output->height[1]);
    MaskMSBs(pImage[2], ((1 << output->bit_depth[2]) - 1), output->width[1], output->height[1]);
#endif
  }

  return file_read;
}

/*!
 ************************************************************************
 * \brief
 *    Padding of automatically added border for picture sizes that are not
 *     multiples of macroblock/macroblock pair size
 *
 * \param output
 *    Image dimensions
 * \param img_size_x
 *    coded image horizontal size (luma)
 * \param img_size_y
 *    code image vertical size (luma)
 * \param img_size_x_cr
 *    coded image horizontal size (chroma)
 * \param img_size_y_cr
 *    code image vertical size (chroma)
 * \param pImage
 *    image planes
 ************************************************************************
 */
void pad_borders (FrameFormat output, int img_size_x, int img_size_y, int img_size_x_cr, int img_size_y_cr, imgpel **pImage[3])
{
  int x, y;

  // Luma or 1st component
  // padding right border
  if (output.width[0] < img_size_x)
    for (y = 0; y < output.height[0]; y++)
      for (x = output.width[0]; x < img_size_x; x++)
        pImage[0][y][x] = pImage[0][y][x-1];

  // padding bottom border
  if (output.height[0] < img_size_y)
    for (y = output.height[0]; y<img_size_y; y++)
      memcpy(pImage[0][y], pImage[0][y - 1], img_size_x * sizeof(imgpel));

  // Chroma or all other components
  if (output.yuv_format != YUV400)
  {
    int k;

    for (k = 1; k < 3; k++)
    {
      //padding right border
      if (output.width[1] < img_size_x_cr)
        for (y=0; y < output.height[1]; y++)
          for (x = output.width[1]; x < img_size_x_cr; x++)
            pImage [k][y][x] = pImage[k][y][x-1];

      //padding bottom border
      if (output.height[1] < img_size_y_cr)
        for (y = output.height[1]; y < img_size_y_cr; y++)
          memcpy(pImage[k][y], pImage[k][y - 1], img_size_x_cr * sizeof(imgpel));
    }
  }
}
