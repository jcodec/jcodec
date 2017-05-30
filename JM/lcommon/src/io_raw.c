/*!
 *************************************************************************************
 * \file io_raw.c
 *
 * \brief
 *    I/O functions related to raw images
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Karsten Suehring
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>
 *     
 *************************************************************************************
 */
#include "contributors.h"

#include "global.h"
#include "img_io.h"

#define FAST_READ 1

#if FAST_READ
static inline int ReadData (int vfile,  FrameFormat *source, unsigned char *buf)
{
  unsigned char *cur_buf = buf;
  int read_size = source->pic_unit_size_shift3 * source->width[0];
  int i, j;

  for (i = 0; i < source->height[0]; i++)
  {
    if (read(vfile, cur_buf, read_size) != read_size)
    {
      printf ("read_one_frame: cannot read %d bytes from input file, unexpected EOF!\n", source->width[0]);
      return 0;
    }
    cur_buf += read_size;
  }

  if (source->yuv_format != YUV400)
  {
    read_size = source->pic_unit_size_shift3 * source->width[1];
    for (j = 0; j < 2; j++)
    {
      for (i = 0; i < source->height[1]; i++)
      {
        if (read(vfile, cur_buf, read_size) != read_size)
        {
          printf ("read_one_frame: cannot read %d bytes from input file, unexpected EOF!\n", source->width[1]);
          return 0;
        }
        cur_buf += read_size;
      }
    }
  }
  return 1;
}
#else
static inline int ReadData (int vfile, int framesize_in_bytes, unsigned char *buf)
{
  if (read(vfile, buf, (int) framesize_in_bytes) != (int) framesize_in_bytes)
  {
    printf ("read_one_frame: cannot read %d bytes from input file, unexpected EOF!\n", (int) framesize_in_bytes);
    return 0;
  }
  else
  {
    return 1;
  }
}
#endif

static inline int ReadDataV210 (int vfile, int framesize_in_bytes, unsigned char *buf)
{
  if (read(vfile, buf, (int) framesize_in_bytes) != (int) framesize_in_bytes)
  {
    printf ("read_one_frame: cannot read %d bytes from input file, unexpected EOF!\n", (int) framesize_in_bytes);
    return 0;
  }
  else
  {
    return 1;
  }
}


/*!
 ************************************************************************
 * \brief
 *    Reads one new frame from concatenated raw file
 *    Code does not distinguish between planar and interleaved data
 *
 * \param input_file
 *    Input file to read from
 * \param FrameNoInFile
 *    Frame number in the source file
 * \param HeaderSize
 *    Number of bytes in the source file to be skipped
 * \param source
 *    source file (on disk) information 
 * \param buf
 *    image buffer data
 ************************************************************************
 */
int ReadFrameConcatenated (InputParameters *p_Inp, VideoDataFile *input_file, int FrameNoInFile, int HeaderSize, FrameFormat *source, unsigned char *buf)
{
  int file_read = 0;
  int vfile = input_file->f_num;
  Boolean is_v210 = (input_file->is_interleaved && source->pixel_format == V210);
  unsigned int symbol_size_in_bytes = source->pic_unit_size_shift3;

  const int bytes_y  = source->size_cmp[0] * symbol_size_in_bytes;
  const int bytes_uv = source->size_cmp[1] * symbol_size_in_bytes;

  const int64 framesize_in_bytes = (is_v210 == TRUE) ? 16 * (source->size_cmp[0] / 6) : bytes_y + 2 * bytes_uv;
  
  // Let us seek directly to the current frame
  if (lseek (vfile, HeaderSize + framesize_in_bytes * (FrameNoInFile + p_Inp->start_frame), SEEK_SET) == -1)
  {
    snprintf(errortext, ET_SIZE, "read_one_frame: cannot advance file pointer in input file beyond frame %d\n", p_Inp->start_frame + FrameNoInFile);
    error (errortext,-1);
  }

  // Here we are at the correct position for the source frame in the file.  
  // Now read it.
  if ((source->pic_unit_size_on_disk & 0x07) == 0)
  {
    if (is_v210 == TRUE)
    {
      file_read = ReadDataV210(vfile, (int) framesize_in_bytes, buf);
    }
    else 
    {
#if FAST_READ
      file_read = ReadData (vfile, source, buf);
#else
      file_read = ReadData (vfile, (int) framesize_in_bytes, buf);
#endif
    }
  }
  else
  {
    printf ("read_one_frame (NOT IMPLEMENTED): pic unit size on disk must be divisible by 8");
    exit (-1);
  }
  return file_read;
}

/*!
 ************************************************************************
 * \brief
 *    Reads one new frame from separate data files
 *    Code does not distinguish between planar and interleaved data
 *
 * \param input_file
 *    Input file to read from
 * \param FrameNoInFile
 *    Frame number in the source file
 * \param HeaderSize
 *    Number of bytes in the source file to be skipped
 * \param source
 *    source file (on disk) information 
 * \param buf
 *    taget buffer
 ************************************************************************
 */
int ReadFrameSeparate (InputParameters *p_Inp, VideoDataFile *input_file, int FrameNoInFile, int HeaderSize, FrameFormat *source, unsigned char *buf)
{
  int file_read = 0;
  int vfile = input_file->f_num;

  OpenFrameFile( input_file, FrameNoInFile + p_Inp->start_frame);

  // skip Header
  if (lseek (vfile, HeaderSize, SEEK_SET) != HeaderSize)
  {
    error ("read_one_frame: cannot fseek to (Header size) in input file", -1);
  }

  // Read data
  if ((source->pic_unit_size_on_disk & 0x07) == 0)
  {
#if FAST_READ
    file_read = ReadData (vfile, source, buf);
#else
    unsigned int symbol_size_in_bytes = source->pic_unit_size_shift3;

    const int bytes_y = source->size_cmp[0] * symbol_size_in_bytes;
    const int bytes_uv = source->size_cmp[1] * symbol_size_in_bytes;
    const int64 framesize_in_bytes = bytes_y + 2*bytes_uv;

    file_read = ReadData (vfile, (int) framesize_in_bytes, buf);
#endif
  }
  else
  {
    printf ("read_one_frame (NOT IMPLEMENTED): pic unit size on disk must be divisible by 8");
    exit (-1);
  }

  if (vfile != -1)
    close(vfile);

  return file_read;
}
