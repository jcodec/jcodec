
/*!
 *************************************************************************************
 * \file img_io.c
 *
 * \brief
 *    image I/O related functions
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>
 *************************************************************************************
 */
#include "contributors.h"
#include "global.h"
#include "img_io.h"
#include "report.h"

static const VIDEO_SIZE VideoRes[] = {
  { "qcif"  ,  176,  144},
  { "qqvga" ,  160,  128},
  { "qvga"  ,  320,  240},
  { "sif"   ,  352,  240},
  { "cif"   ,  352,  288},
  { "vga"   ,  640,  480},
  { "sd1"   ,  720,  480},
  { "sd2"   ,  704,  576},
  { "sd3"   ,  720,  576},
  { "720p"  , 1280,  720},
  { "1080p" , 1920, 1080},
  { NULL, 0, 0}
};

/*!
 ************************************************************************
 * \brief
 *    Parse Size from from file name
 *
 ************************************************************************
 */
int ParseSizeFromString (VideoDataFile *input_file, int *x_size, int *y_size, double *fps) 
{
  char *p1, *p2, *tail;
  char *fn = input_file->fname;
  char c;
  int i = 0;

  *x_size = *y_size = -1;
  p1 = p2 = fn;
  while (p1 != NULL && p2 != NULL) 
  {
    // Search for first '_'
    p1 = strstr( p1, "_");
    if (p1 == NULL)
      break;

    // Search for end character of x_size (first 'x' after last '_')
    p2 = strstr( p1, "x");

    // If no 'x' is found, exit
    if (p2 == NULL)    
      break;

    // Try conversion of number
    *p2 = 0;
    *x_size = strtol( p1 + 1, &tail, 10);

    // If there are characters left in the string, or the string is null, discard conversion
    if (*tail != '\0' || *(p1 + 1) == '\0') 
    {
      *p2 = 'x';
      p1 = tail;
      continue;
    }

    // Conversion was correct. Restore string
    *p2 = 'x';

    // Search for end character of y_size (first '_' or '.' after last 'x')
    p1 = strpbrk( p2 + 1, "_.");
    // If no '_' or '.' is found, try again from current position
    if (p1 == NULL) 
    {
      p1 = p2 + 1;
      continue;
    }

    // Try conversion of number
    c = *p1;
    *p1 = 0;
    *y_size = strtol( p2 + 1, &tail, 10);

    // If there are characters left in the string, or the string is null, discard conversion
    if (*tail != '\0' || *(p2 + 1) == '\0') 
    {
      *p1 = c;
      p1 = tail;
      continue;
    }

    // Conversion was correct. Restore string
    *p1 = c;

    // Search for end character of y_size (first 'i' or 'p' after last '_')
    p2 = strstr( p1 + 1, "ip");

    // If no 'i' or 'p' is found, exit
    if (p2 == NULL)      
      break;

    // Try conversion of number
    c = *p2;
    *p2 = 0;
    *fps = strtod( p1 + 1, &tail);

    // If there are characters left in the string, or the string is null, discard conversion
    if (*tail != '\0' || *(p1 + 1) == '\0') 
    {
      *p2 = c;
      p1 = tail;
      continue;
    }

    // Conversion was correct. Restore string
    *p2 = c;
    break;
  }

  // Now lets test some common video file formats
  if (p1 == NULL || p2 == NULL)
  {       
    for (i = 0; VideoRes[i].name != NULL; i++) 
    {
      if (strcasecmp (fn, VideoRes[i].name)) 
      {
        *x_size = VideoRes[i].x_size;
        *y_size = VideoRes[i].y_size;       
        // Should add frame rate support as well
        break;
      }
    }
  }

  return (*x_size == -1 || *y_size == -1) ? 0 : 1; 
}

/*!
 ************************************************************************
 * \brief
 *    Parse Size from from file name
 *
 ************************************************************************
 */
void ParseFrameNoFormatFromString (VideoDataFile *input_file)
{
  char *p1, *p2, *tail;  
  char *fn         = input_file->fname;
  char *fhead      = input_file->fhead;
  char *ftail      = input_file->ftail;
  int  *zero_pad   = &input_file->zero_pad;
  int  *num_digits = &input_file->num_digits;

  *zero_pad = 0;
  *num_digits = -1;
  p1 = p2 = fn;
  while (p1 != NULL && p2 != NULL) 
  {
    // Search for first '_'
    p1 = strstr( p1, "%");
    if (p1 == NULL)
      break;

    strncpy(fhead, fn, p1 - fn);

    // Search for end character of x_size (first 'x' after last '_')
    p2 = strstr( p1, "d");

    // If no 'x' is found, exit
    if (p2 == NULL)    
      break;
    
    // Try conversion of number
    *p2 = 0;

    if (*(p1 + 1) == '0')
      *zero_pad = 1;

    *num_digits = strtol( p1 + 1, &tail, 10);

    // If there are characters left in the string, or the string is null, discard conversion
    if (*tail != '\0' || *(p1 + 1) == '\0') 
    {
      *p2 = 'd';
      p1 = tail;
      continue;
    }

    // Conversion was correct. Restore string
    *p2 = 'd';

    tail++;
    strncpy(ftail, tail, (int) strlen(tail));
    break;
  }

  if (input_file->vdtype == VIDEO_TIFF)
  {
    input_file->is_concatenated = 0;
  }
  else
    input_file->is_concatenated = (*num_digits == -1) ? 1 : 0;
}

/*!
 ************************************************************************
 * \brief
 *    Open file containing a single frame
 ************************************************************************
 */
void OpenFrameFile( VideoDataFile *input_file, int FrameNumberInFile)
{
  char infile [FILE_NAME_SIZE], in_number[16];
  infile[FILE_NAME_SIZE-1]='\0';
  strncpy(infile, input_file->fhead, FILE_NAME_SIZE-1);

  if (input_file->zero_pad)       
    snprintf(in_number, 16, "%0*d", input_file->num_digits, FrameNumberInFile);
  else
    snprintf(in_number, 16, "%*d", input_file->num_digits, FrameNumberInFile);

  strncat(infile, in_number, FILE_NAME_SIZE-strlen(in_number)-1);
  strncat(infile, input_file->ftail, FILE_NAME_SIZE-strlen(input_file->ftail)-1);

  if ((input_file->f_num = open(infile, OPENFLAGS_READ)) == -1)
  {
    printf ("OpenFrameFile: cannot open file %s\n", infile);
    report_stats_on_error();
  }    
}

/*!
 ************************************************************************
 * \brief
 *    Open file(s) containing the entire frame sequence
 ************************************************************************
 */
void OpenFiles( VideoDataFile *input_file)
{
  if (input_file->is_concatenated == 1)
  {
    if ((int) strlen(input_file->fname) == 0)
    {
      snprintf(errortext, ET_SIZE, "No input sequence name was provided. Please check settings.");
      error (errortext, 500);
    }

    if ((input_file->f_num = open(input_file->fname, OPENFLAGS_READ)) == -1)
    {
      snprintf(errortext, ET_SIZE, "Input file %s does not exist",input_file->fname);
      error (errortext, 500);
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Close input file
 ************************************************************************
 */
void CloseFiles(VideoDataFile *input_file)
{
  if (input_file->f_num != -1)
    close(input_file->f_num);
  input_file->f_num = -1;
}

/* ==========================================================================
 *
 * ParseVideoType
 *
 * ==========================================================================
*/
VideoFileType ParseVideoType (VideoDataFile *input_file)
{
  char *format;

  format = input_file->fname + (int) strlen(input_file->fname) - 3;

  if (strcasecmp (format, "yuv") == 0)
  {
    input_file->vdtype = VIDEO_YUV;
    input_file->format.yuv_format = YUV420;
    input_file->avi = NULL;
  }
  else if (strcasecmp (format, "rgb") == 0)
  {
    input_file->vdtype = VIDEO_RGB;
    input_file->format.yuv_format = YUV444;
    input_file->avi = NULL;
  }
  else if (strcasecmp (format, "tif") == 0)
  {
    input_file->vdtype = VIDEO_TIFF;
    input_file->avi = NULL;
  }
  else if (strcasecmp (format, "avi") == 0) 
  {
    input_file->vdtype = VIDEO_AVI;
  }
  else
  {
    //snprintf(errortext, ET_SIZE, "ERROR: video file format not supported");
    //error (errortext, 500);
    input_file->vdtype = VIDEO_YUV;
    input_file->format.yuv_format = YUV420;
    input_file->avi = NULL;
  }

  return input_file->vdtype;
}
