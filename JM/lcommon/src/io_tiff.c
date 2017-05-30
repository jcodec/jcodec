/*!
 *************************************************************************************
 * \file io_tiff.c
 *
 * \brief
 *    I/O functions related to TIFF images
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Larry Luther                    <lzl@dolby.com>
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>
 *     
 *************************************************************************************
 */
#include "contributors.h"
#include "report.h"
#include "io_tiff.h"


// Maximum number of rows in image
#define YRES 1080

// Order of member variables is critical

//! TIFF Field Type Code
typedef enum TiffType {
  T_BYTE      = 1,   //!< 8-bit unsigned integer.
  T_ASCII     = 2,   //!< 8-bit byte that contains a 7-bit ASCII code; the last byte must be NUL (binary zero).
  T_SHORT     = 3,   //!< 16-bit (2-byte) unsigned integer.
  T_LONG      = 4,   //!< 32-bit (4-byte) unsigned integer.
  T_RATIONAL  = 5,   //!< Two LONGs:  the first represents the numerator of a fraction; the second, the denominator.
  T_SBYTE     = 6,   //!< An 8-bit signed (twos-complement) integer.
  T_UNDEFINED = 7,   //!< An 8-bit byte that may contain anything, depending on the definition of the field.
  T_SSHORT    = 8,   //!< A 16-bit (2-byte) signed (twos-complement) integer.
  T_SLONG     = 9,   //!< A 32-bit (4-byte) signed (twos-complement) integer.
  T_SRATIONAL = 10,  //!< Two SLONG’s:  the first represents the numerator of a fraction, the second the denominator.
  T_FLOAT     = 11,  //!< Single precision (4-byte) IEEE format.
  T_DOUBLE    = 12   //!< Double precision (8-byte) IEEE format.
} TiffType;


typedef struct TiffDirectoryEntry {
  uint16  tag;                                //!< The tag that identifies the field.
  uint16  type;                               //!< The field type.
  uint32  count;                              //!< Number of values of the indicated type.
  uint32  offset;                             //!< Value or offset.
} TiffDirectoryEntry;


//! TIFF Image File Directory
typedef struct TiffIFD {
  uint16             nEntries;
  TiffDirectoryEntry *directoryEntry;
} TiffIFD;


typedef struct TiffImageFileHeader {
  uint16 byteOrder;                          //!< "II" (4949H) or "MM" (4D4DH)
  uint16 arbitraryNumber;                    //!< 42
  uint32 offset;                             //!< Offset of the 0th IFD
} TiffImageFileHeader;


//! TIFF file data
typedef struct Tiff {
  uint16 *img;                               //!< Image data
  uint8  *fileInMemory;                      //!< The file will be read into memory in one gulp here.
  uint8  *mp;                                //!< Memory pointer.
  int    le;                                 //!< Little endian - 0 false, 1 - true
  int    nStrips;
  TiffImageFileHeader ifh;
                                        // Information from TAGs
  uint16  Orientation;
  uint32
    BitsPerSample[3],
    RowsPerStrip,
    ImageLength,
    ImageWidth,
    StripByteCounts[YRES],
    StripOffsets[YRES],
    XResolution[2],
    YResolution[2];

  uint32 (*getU16) (struct Tiff *);
  uint32 (*getU32) (struct Tiff *);
} Tiff;

//! Video codes for RGB ==> YUV conversions
typedef enum VideoCode {
  VC_NULL             = 0,
  VC_ITU_REC709       = 1,
  VC_CCIR_601         = 3,
  VC_FCC              = 4,
  VC_ITU_REC624BG     = 5,
  VC_SMPTE_170M       = 6,
  VC_SMPTE_240M       = 7,
  VC_SMPTE_260M       = 7,
  VC_ITU_REC709_EXACT = 8,
  VC_MAX              = 8
} VideoCode;


static const double Coef[VC_MAX+1][3] = {     
  {0.299   , 0.587   , 0.114},          //  0  unspecified
  {0.2126  , 0.7152  , 0.0722},         //  1  SMPTE RP-177 & 274M, ITU-R Rec. 709
  {0.299   , 0.587   , 0.114},          //  2  unspecified
  {0.299   , 0.587   , 0.114},          //  3  CCIR 601 ITU-R BT.601 / SMPTE 125M
  {0.30    , 0.59    , 0.11},           //  4  FCC
  {0.299   , 0.587   , 0.114},          //  5  ITU-R Rec. 624-4 System B, G
  {0.299   , 0.587   , 0.114},          //  6  SMPTE 170M
  {0.212   , 0.701   , 0.087},          //  7  SMPTE 240M (1987)  260M
  {0.212639, 0.715169, 0.072192}        //  8  SMPTE RP-177 & 274M, ITU-R Rec. 709 exact
};

#define CR Coef[videoCode][0]
#define CG Coef[videoCode][1]
#define CB Coef[videoCode][2]
#define CU (0.5/(CB-1.0))
#define CV (0.5/(CR-1.0))


#define stdScaleY   219.0  // nominal range: 16..235
#define stdScaleUV  224.0  // nominal range: 16..240

#define INTEGER_SCALE 16384
#define INTEGER_SHIFT    14


//! Parameters for RGB ==> YUV conversions.
typedef struct RGB_YUV {
  uint16
    pixMax;                             //!< Maximum pixel value.
  int
    stdRange,                           //!< 0 = full range, 1 = std range
    y, u, v,
    r, g, b,
    yr, yg, yb,
    ur, ug, ub,
    vr, vg, vb,
    offy, offuv;
  double
    sy, suv;
} RGB_YUV;

/*!
 ************************************************************************
 * \brief
 *   Initialize pointer variables, since they are maintained across multiple calls to ReadTIFFImage
 *
 ************************************************************************
 */
void constructTiff (Tiff * t) 
{
  t->fileInMemory = 0;
  t->img = 0;
  t->mp = 0;
}


/*!
 ************************************************************************
 * \brief
 *   free allocated memory
 *
 ************************************************************************
 */
void destructTiff (Tiff * t) 
{
  free_pointer( t->fileInMemory);
  free_pointer( t->img);  
}


// No swap versions

/*!
 ************************************************************************
 * \brief
 *   Get an unsigned short without swapping.
 *
 ************************************************************************
 */
static uint32 getU16 (Tiff * t) 
{
  union {
    uint8 in[2];
    uint16 out;
  } u;
  u.in[0] = *t->mp++;
  u.in[1] = *t->mp++;
  return u.out;
}


/*!
 ************************************************************************
 * \brief
 *   Get an unsigned long without swapping.
 *
 ************************************************************************
 */
static uint32 getU32 (Tiff * t) 
{
  union {
    uint8 in[4];
    uint32 out;
  } u;
  u.in[0] = *t->mp++;
  u.in[1] = *t->mp++;
  u.in[2] = *t->mp++;
  u.in[3] = *t->mp++;
  return u.out;
}


// Swap versions

/*!
 ************************************************************************
 * \brief
 *   Get an unsigned short and swap.
 *
 ************************************************************************
 */
static uint32 getSwappedU16 (Tiff * t) 
{
  union {
    uint8 in[2];
    uint16 out;
  } u;
  u.in[1] = *t->mp++;
  u.in[0] = *t->mp++;
  return u.out;
}


/*!
 ************************************************************************
 * \brief
 *   Get an unsigned long and swap.
 *
 ************************************************************************
 */
static uint32 getSwappedU32 (Tiff * t) 
{
  union {
    uint8 in[4];
    uint32 out;
  } u;
  u.in[3] = *t->mp++;
  u.in[2] = *t->mp++;
  u.in[1] = *t->mp++;
  u.in[0] = *t->mp++;
  return u.out;
}


/*!
 ************************************************************************
 * \brief
 *   Get an array of (uint16|uint32|rational).
 *
 ************************************************************************
 */
static void getIntArray (Tiff * t, uint32 offset, TiffType type, uint32 a[], int n) 
{
  int    i;
  uint8  *mp = t->mp;                           // save memory pointer

  t->mp = t->fileInMemory + offset;
  switch (type) 
  {
  case T_SHORT:
    for (i=0; i < n; ++i) 
    {
      a[i] = getU16( t);
    }
    break;
  case T_LONG:
    for (i=0; i < n; ++i) 
    {
      a[i] = getU32( t);
    }
    break;
  case T_RATIONAL:
    for (i=0; i < 2*n; ++i) 
    {
      a[i] = getU32( t);
    }
    break;
  default:
    assert( type == T_SHORT || type == T_LONG || type == T_RATIONAL);
  }
  t->mp = mp;                           // restore memory pointer
}


/*!
 ************************************************************************
 * \brief
 *   Read DirectoryEntry and store important results in 't'.
 *
 ************************************************************************
 */
static int readDirectoryEntry (Tiff * t) 
{
  uint32   tag    = t->getU16( t);
  TiffType type   = (TiffType) t->getU16( t);
  uint32   count  = t->getU32( t);
  uint32   offset = t->getU32( t);

  switch (tag) 
  {
  case 256:                           // ImageWidth  SHORT or LONG
    assert( count == 1);
    //printf( "256:  ImageWidth          = %u\n", offset);
    t->ImageWidth = offset;
    break;
  case 257:                           // ImageLength    SHORT or LONG
    assert( count == 1);
    //printf( "257:  ImageLength         = %u\n", offset);
    t->ImageLength = offset;
    if (offset > YRES) 
    {
      fprintf( stderr, "readDirectoryEntry:  ImageLength (%d) exceeds builtin maximum of %d\n", offset, YRES);
      return 1;
    }
    break;
  case 258:                           // BitsPerSample  SHORT 8,8,8
    if (count != 3) 
    {
      fprintf( stderr, "BitsPerSample (only [3] supported)\n");
      return 1;
    }
    getIntArray( t, offset, type, t->BitsPerSample, 3);
    //printf( "258:  BitsPerSample[%d]    = %u,%u,%u\n", count, t->BitsPerSample[0], t->BitsPerSample[1], t->BitsPerSample[2]);
    if (t->BitsPerSample[0] != t->BitsPerSample[1] || t->BitsPerSample[0] != t->BitsPerSample[2]) 
    {
      fprintf( stderr, "BitsPerSample must be the same for all samples\n");
      return 1;
    }
    if (t->BitsPerSample[0] != 8 && t->BitsPerSample[0] != 16) 
    {
      fprintf( stderr, "Only 8 or 16 BitsPerSample is supported\n");
      return 1;
    }
    break;
  case 259:                           // Compression SHORT 1 or 32773
    assert( count == 1);
    //printf( "259:  Compression         = %u\n", offset);
    if (offset != 1) 
    {
      fprintf( stderr, "Only uncompressed TIFF files supported\n");
      return 1;
    }
    break;
  case 262:                           // PhotometricInterpretation SHORT 2
    assert( count == 1);
    //printf( "262:  PhotometricInterpretation = %u\n", offset);
    assert( offset == 2);
    break;
  case 273:                           // StripOffsets  SHORT or LONG
    //printf( "273:  StripOffsets[%d] = %u\n", count, offset);
    getIntArray( t, offset, type, t->StripOffsets, count);
    t->nStrips = count;
    break;
  case 274:                           // Orientation  SHORT
    assert( count == 1);
    //printf( "274:  Orientation         = %u\n", offset);
    t->Orientation = (uint16) offset;
    if (t->Orientation != 1) 
    {
      fprintf( stderr, "Only Orientation 1 is supported\n");
      return 1;
    }
    break;
  case 277:                           // SamplesPerPixel SHORT 3 or more
    assert( count == 1);
    //printf( "277:  SamplesPerPixel     = %u\n", offset);
    assert( offset == 3);
    break;
  case 278:                           // RowsPerStrip  SHORT or LONG
    assert( count == 1);
    //printf( "278:  RowsPerStrip        = %u\n", offset);
    t->RowsPerStrip = offset;
    break;
  case 279:                           // StripByteCounts LONG or SHORT
    //printf( "279:  StripByteCounts[%u] = %u\n", count, offset);
    getIntArray( t, offset, type, t->StripByteCounts, count);
    break;
  case 282:                           // XResolution  RATIONAL
    assert( count == 1);
    getIntArray( t, offset, type, t->XResolution, 1);
    //printf( "282:  XResolution         = %u/%u\n", offset, t->XResolution[0], t->XResolution[1]);
    break;
  case 283:                           // YResolution  RATIONAL
    assert( count == 1);
    getIntArray( t, offset, type, t->YResolution, 1);
    //printf( "283:  YResolution         = %u/%u\n", offset, t->YResolution[0], t->YResolution[1]);
    break;
  case 284:                           // PlanarConfiguration  SHORT
    assert( count == 1);
    //printf( "284:  PlanarConfiguration = %u\n", offset);
    assert( offset == 1);
    break;
  case 296:                           // ResolutionUnit  SHORT 1, 2 or 3
    //printf( "296:  ResolutionUnit      = %u\n", offset);
    assert( count == 1);
    break;
  case 305:                           // Software  ASCII
    //printf( "305:  Software            = %s\n", t->fileInMemory + offset);
    break;
  case 339:                           // SampleFormat  SHORT 1
  default:
    //printf( "%3d:  Unforseen           = %u\n", tag, offset);
    ;
  }    
  return 0;
}


/*!
 ************************************************************************
 * \brief
 *   Read file named 'path' into memory buffer 't->fileInMemory'.
 *
 * \return
 *   0 if successful
 ************************************************************************
 */
static int readFileIntoMemory (Tiff * t, const char * path) 
{
  long
    cnt, result;
  uint16
    byteOrder;
  int
    endian = 1,
    machineLittleEndian = (*( (char *)(&endian) ) == 1) ? 1 : 0,
    fd;

  assert( t);
  assert( path);

  fd = open( path, OPENFLAGS_READ);
  if (fd == -1) 
  {
    fprintf( stderr, "Couldn't open to read:  %s\n", path);
    return 1;
  }

  cnt = (long) lseek( fd, 0, SEEK_END); // TIFF files by definition cannot exceed 2^32
  if (cnt == -1L)
    return 1;

  if (lseek( fd, 0, SEEK_SET) == -1L)   // reposition file at beginning
    return 1;

  t->fileInMemory = (uint8 *) realloc( t->fileInMemory, cnt);
  if (t->fileInMemory == 0) 
  {
    close( fd);
    return 1;
  }

  result = (long) read( fd, t->fileInMemory, cnt);
  if (result != cnt) 
  {
    close( fd);
    return 1;
  }

  close( fd);

  byteOrder = (t->fileInMemory[0] << 8) | t->fileInMemory[1];
  switch (byteOrder) 
  {
    case 0x4949:                        // little endian file
      t->le = 1;
      //printf( "Little endian file\n");
      break;
    case 0x4D4D:                        // big endian file
      t->le = 0;
      //printf( "Big endian file\n");
      break;
    default:
      fprintf( stderr, "First two bytes indicate:  Not a TIFF file\n");
      free_pointer( t->fileInMemory);
      return 1;
  }
  if (t->le == machineLittleEndian)   // endianness of machine matches file
  {
    t->getU16 = getU16;
    t->getU32 = getU32;
  } 
  else                               // endianness of machine does not match file
  {
    t->getU16 = getSwappedU16;
    t->getU32 = getSwappedU32;
  }
  t->mp = t->fileInMemory;
  return 0;
}

/*!
 ************************************************************************
 * \brief
 *    Read image data into 't->img'.
 *
 ************************************************************************
 */
static int readImageData (Tiff * t) 
{
  int     i, j, n;
  uint8  *mp, *s;
  uint16 *p;
  uint32  size;

  assert( t);

  size = t->ImageWidth * t->ImageLength * 3 * sizeof(uint16);
  t->img = (uint16 *) realloc( t->img, size);
  if (t->img == 0)
    return 1;

  switch (t->BitsPerSample[0]) 
  {
  case 8:
    p = t->img;
    for (i=0; i < t->nStrips; ++i) 
    {
      n = t->StripByteCounts[i];
      s = t->fileInMemory + t->StripOffsets[i];
      for (j=0; j < n; ++j) 
      {
        *p++ = *s++;
      }
    }
    break;
  case 16:
    mp = t->mp;                       // save memory pointer
    p = t->img;
    for (i=0; i < t->nStrips; ++i) 
    {
      n = t->StripByteCounts[i] / 2;
      t->mp = t->fileInMemory + t->StripOffsets[i];
      for (j=0; j < n; ++j) 
      {
        *p++ = (uint16) getU16( t);
      }
    }
    t->mp = mp;                       // restore memory pointer
    break;
  }
  return 0;
}


/*!
 *****************************************************************************
 * \brief
 *    Read the ImageFileDirectory.
 *    
 *****************************************************************************
*/
static int readImageFileDirectory (Tiff * t) 
{
  uint32 i;
  uint32 nEntries = t->getU16( t);

  for (i=0; i < nEntries; ++i) 
  {
    readDirectoryEntry( t);
  }
  return 0;
}


/*!
 *****************************************************************************
 * \brief
 *    Read the ImageFileHeader.
 *    
 *****************************************************************************
*/
static int readImageFileHeader (Tiff * t) 
{
  t->ifh.byteOrder = (uint16) getU16( t);
  t->ifh.arbitraryNumber = (uint16) getU16( t);
  t->ifh.offset = getU32( t);
  if (t->ifh.arbitraryNumber != 42) 
  {
    fprintf( stderr, "ImageFileHeader.arbitrary != 42\n");
    return 1;
  }    
  t->mp = t->fileInMemory + t->ifh.offset;
  return 0;
}

/*!
 *****************************************************************************
 * \brief
 *    Read the TIFF file named 'path' into 't'.
 *    
 *****************************************************************************
*/
static int readTiff (Tiff * t, char * path) {
  assert( t);
  assert( path);

  if (readFileIntoMemory( t, path))
    goto Error;
  if (readImageFileHeader( t)) 
    goto Error;
  if (readImageFileDirectory( t))
    goto Error;
  if (readImageData( t))
    goto Error;
  return 0;

Error:
  free_pointer( t->fileInMemory);
  free_pointer( t->img);
  return 1;
}

/*!
 *****************************************************************************
 * \brief
 *    Initialize RGB ==> YUV conversion factors
 *    
 *****************************************************************************
*/
static int RGB_YUV_initialize (RGB_YUV * T,
                               VideoCode videoCode,   //!< <-- Describes color space.
                               int stdRange,          //!< <-- 1 = Standard range, 0 = Full range
                               uint16 pixMax          //!< <-- Maximum component value.
                              )
{
  int i, pixScale;

  switch (videoCode) 
  {
    case VC_ITU_REC709:
    case VC_CCIR_601:
      break;
    default:
      fprintf( stderr, "RGB_YUV_initialize:  Unsupported videoCode (%d)\n", videoCode);
      return 1;
  }

  T->stdRange = stdRange;
  T->pixMax = pixMax;

  pixScale = (int)pixMax + ((int)pixMax & 1);

  if (stdRange) 
  {
    T->offy = (int)(INTEGER_SCALE * (pixScale * 16 / 256.0 + 0.5));  // setup + rounding
    T->sy =   INTEGER_SCALE * stdScaleY  / 255.0;
    T->suv =  INTEGER_SCALE * stdScaleUV / 255.0;
  } 
  else                             // full range
  {
    T->offy = (int)(INTEGER_SCALE * 0.5);  // rounding
    T->sy =   INTEGER_SCALE;
    T->suv =  INTEGER_SCALE;
  }
  T->offuv = (int)(INTEGER_SCALE * (0.5 * pixScale + 0.5));  // offset + rounding

  // IMPORTANT NOTE: sign of 0.5 for rounding must agree with sign of CU & CV
  T->yr = (int)(T-> sy*CR   +0.5);  T->yg = (int)(T-> sy*CG   +0.5);  T->yb = (int)(T-> sy*CB   +0.5);
  T->ur = (int)(T->suv*CR*CU-0.5);  T->ug = (int)(T->suv*CG*CU-0.5);  T->ub = (int)(T->suv*0.5  +0.5);
  T->vr = (int)(T->suv*0.5  +0.5);  T->vg = (int)(T->suv*CG*CV-0.5);  T->vb = (int)(T->suv*CB*CV-0.5);

  i = (unsigned int)(T->sy + 0.5);
  if (T->yr+T->yg+T->yb != i) 
  {
    fprintf( stderr, "ERROR: RGB_YUV_initialize: yr+yg+yb=%d sy=%u\n", T->yr+T->yg+T->yb, i);
    return 1;
  }
  if (T->ur+T->ug+T->ub) 
  {
    fprintf( stderr, "ERROR: RGB_YUV_initialize: ur+ug+ub=%d\n", T->ur+T->ug+T->ub);
    return 1;
  }
  if (T->vr+T->vg+T->vb) 
  {
    fprintf( stderr, "ERROR: RGB_YUV_initialize: vr+vg+vb=%d\n", T->vr+T->vg+T->vb);
    return 1;
  }

  return 0;
}

/*!
 *****************************************************************************
 * \brief
 *    Convert interleaved/planar RGB components to interleaved/planar YUV components
 *    
 *****************************************************************************
*/
static void RGB_YUV_rgb_to_yuv (RGB_YUV * T,
                                uint16 *rp,     //!< <-- red components
                                uint16 *gp,     //!< <-- green components
                                uint16 *bp,     //!< <-- blue components
                                int xres,       //!< <-- number of pixels in a row
                                int yres,       //!< <-- number of rows
                                int rgb_stride, //!< <-- stride to next R,G,B component
                                uint16 *yp,     //!< --> Y components
                                uint16 *up,     //!< --> U components
                                uint16 *vp,     //!< --> V components
                                int yuv_stride  //!< <-- stride to next Y,U,V component
                               )
{
  int i;
  int count = xres * yres;

  if (T->stdRange)                     // clipping not needed
  {
    for (i=0; i < count; ++i) 
    {
      T->r = (int) *rp;  rp += rgb_stride;
      T->g = (int) *gp;  gp += rgb_stride;
      T->b = (int) *bp;  bp += rgb_stride;
      // convert to YUV
      *yp = (uint16)((T->yr*T->r + T->yg*T->g + T->yb*T->b + T-> offy) >> INTEGER_SHIFT);  yp += yuv_stride;
      *up = (uint16)((T->ur*T->r + T->ug*T->g + T->ub*T->b + T->offuv) >> INTEGER_SHIFT);  up += yuv_stride;
      *vp = (uint16)((T->vr*T->r + T->vg*T->g + T->vb*T->b + T->offuv) >> INTEGER_SHIFT);  vp += yuv_stride;
    }
  } 
  else                             // full range -- need to clip
  {
    for (i=0; i < count; ++i) 
    {
      T->r = (int) *rp;  rp += rgb_stride;
      T->g = (int) *gp;  gp += rgb_stride;
      T->b = (int) *bp;  bp += rgb_stride;

      // convert to YUV
      // right shift of neg value is wrong, but will get clipped so o.k.
      T->y = (T->yr*T->r + T->yg*T->g + T->yb*T->b + T-> offy) >> INTEGER_SHIFT;
      if (T->y < 0) 
        T->y = 0;
      else if (T->y > T->pixMax) 
        T->y = (int) T->pixMax;
      *yp = (uint16) T->y;  yp += yuv_stride;

      T->u = (T->ur*T->r + T->ug*T->g + T->ub*T->b + T->offuv) >> INTEGER_SHIFT;
      if (T->u < 0) 
        T->u = 0;
      else if (T->u > T->pixMax) 
          T->u = (int) T->pixMax;
      *up = (uint16) T->u;  up += yuv_stride;

      T->v = (T->vr*T->r + T->vg*T->g + T->vb*T->b + T->offuv) >> INTEGER_SHIFT;
      if (T->v < 0) 
        T->v = 0;
      else if (T->v > T->pixMax) 
        T->v = (int) T->pixMax;
      *vp = (uint16) T->v;  vp += yuv_stride;
    }
  }                                   // clipping
}


/*!
 *****************************************************************************
 *  \brief FIR horizontal filter and 2:1 subsampling (with no phase shift)
 *
 *  \note
 *    FILTER SPECIFICATION FILE\n
 *    FILTER TYPE:HALF BAND       10H\n
 *    STOPBAND RIPPLE IN -dB      -.4000E+02\n
 *    PASSBAND CUTOFF FREQUENCY   0.170000E+03 HERTZ\n
 *    SAMPLING FREQUENCY          0.100000E+04 HERTZ\n
 *    number of taps in decimal   17   (normalized & rounded in kevin_filter.xls)\n
 *    Window                      Kaiser                    *4096 *8192\n
 *      0.000000000000000000E+00   coefficient of tap   0\n
 *      0.274349475697109923E-02   coefficient of tap   1     11     22\n
 *      0.000000000000000000E+00   coefficient of tap   2\n
 *      0.116164603817880207E-01   coefficient of tap   3     47     94\n
 *      0.000000000000000000E+00   coefficient of tap   4\n
 *      -.645752746253402865E-01   coefficient of tap   5   -262   -524\n
 *      0.000000000000000000E+00   coefficient of tap   6\n
 *      0.302322754093924128E+00   coefficient of tap   7   1228   2456\n
 *      0.500000000000000000E+00   coefficient of tap   8   2048\n
 *      0.302322754093924128E+00   coefficient of tap   9\n
 *      0.000000000000000000E+00   coefficient of tap  10\n
 *      -.645752746253402865E-01   coefficient of tap  11\n
 *      0.000000000000000000E+00   coefficient of tap  12\n
 *      0.116164603817880207E-01   coefficient of tap  13\n
 *      0.000000000000000000E+00   coefficient of tap  14\n
 *      0.274349475697109923E-02   coefficient of tap  15\n
 *      0.000000000000000000E+00   coefficient of tap  16\n
 *****************************************************************************
*/
void horizontal_half_1chan_cosite (uint16  *srcPtr,
                                   int     srcXres,
                                   int     yres,
                                   int     srcZres,
                                   uint16  *dstPtr,
                                   int     dstZres,
                                   int     pixMax   //!< needed for clipping
                                  )
{
  int    x, y, limit, n7, n5, n3, n1;
  int    result;
  uint16 *src = srcPtr;
  uint16 *dst = dstPtr;

  for (y=0; y < yres; y++) 
  {
    for (x=0; x < 8; x+=2) 
    {
      n1 = (x >= 1) ? 1 : x;
      n3 = (x >= 3) ? 3 : x;
      n5 = (x >= 5) ? 5 : x;
      n7 = (x >= 7) ? 7 : x;
      result = (  2048*  *(src)
        + 1228*( *(src-n1*srcZres) + *(src+  srcZres) )
        -  262*( *(src-n3*srcZres) + *(src+3*srcZres) )
        +   47*( *(src-n5*srcZres) + *(src+5*srcZres) )
        +   11*( *(src-n7*srcZres) + *(src+7*srcZres) )+2048) / 4096;
      if (result < 0) 
        result = 0;
      else if (result > pixMax) 
        result = pixMax;
      *dst = (uint16) result;
      dst += dstZres;
      src += 2*srcZres;
    }

    limit = srcXres - 8;
    for (x=8; x < limit; x+=2) 
    {
      result = (  2048*  *(src)
        + 1228*( *(src-  srcZres) + *(src+  srcZres) )
        -  262*( *(src-3*srcZres) + *(src+3*srcZres) )
        +   47*( *(src-5*srcZres) + *(src+5*srcZres) )
        +   11*( *(src-7*srcZres) + *(src+7*srcZres) )+2048) / 4096;
      if (result < 0) 
        result = 0;
      else if (result > pixMax) 
        result = pixMax;
      *dst = (uint16) result;
      dst += dstZres;
      src += 2*srcZres;
    }

    limit = srcXres - (srcXres & 1);  // must round down to not exceed dst
    for (; x < limit; x+=2) 
    {
      n1 = (x < srcXres-1) ? 1 : 0;
      n3 = (x < srcXres-3) ? 3 : (srcXres-1-x);
      n5 = (x < srcXres-5) ? 5 : (srcXres-1-x);
      n7 = (x < srcXres-7) ? 7 : (srcXres-1-x);
      result = (  2048*  *(src)
        + 1228*( *(src-  srcZres) + *(src+n1*srcZres) )
        -  262*( *(src-3*srcZres) + *(src+n3*srcZres) )
        +   47*( *(src-5*srcZres) + *(src+n5*srcZres) )
        +   11*( *(src-7*srcZres) + *(src+n7*srcZres) )+2048) / 4096;
      if (result < 0) 
        result = 0;
      else if (result > pixMax) 
        result = pixMax;
      *dst = (uint16) result;
      dst += dstZres;
      src += 2*srcZres;
    }
  }
}                                     // horizontal_half_1chan_cosite


/*!
 *****************************************************************************
 * \brief
 *    FIR vertical filter and 2:1 subsampling with 0.5 sample interval phase shift
 *    see vertical_half_1chan.pdf for response
 *****************************************************************************
*/
void vertical_half_1chan (uint16  *srcPtr,
                          int      xres,
                          int      srcYres,
                          int      srcZres,
                          uint16  *dstPtr,
                          int      dstZres,
                          int      pixMax  //!< needed for clipping
                         )
{
  int x, y, limit, n6, n5, n4, n3, n2, n1;
  int result;

  int srcRowCount = xres * srcZres;
  uint16 *src = srcPtr;
  uint16 *dst = dstPtr;

  for (y=0; y < 6; y+=2) 
  {
    n1 = (y >= 1) ? 1 : y;
    n2 = (y >= 2) ? 2 : y;
    n3 = (y >= 3) ? 3 : y;
    n4 = (y >= 4) ? 4 : y;
    /*n5 = (y >= 5) ? 5 : y;*/
    for (x=0; x < xres; x++) 
    {
      result = (225*( *(src               ) + *(src+  srcRowCount) )
        +69*( *(src-n1*srcRowCount) + *(src+2*srcRowCount) )
        -30*( *(src-n2*srcRowCount) + *(src+3*srcRowCount) )
        -16*( *(src-n3*srcRowCount) + *(src+4*srcRowCount) )
        + 6*( *(src-n4*srcRowCount) + *(src+5*srcRowCount) )
        + 2*( *(src- y*srcRowCount) + *(src+6*srcRowCount) )+256) / 512;
      if (result < 0) 
        result = 0;
      else if (result > pixMax) 
        result = pixMax;
      *dst = (uint16) result;
      dst += dstZres;
      src += srcZres;
    }
    src += srcRowCount;
  }

  limit = srcYres - 6;
  for (y=6; y < limit; y+=2) 
  {
    for (x=0; x < xres; x++) 
    {
      result = (225*( *(src              ) + *(src+  srcRowCount) )
        +69*( *(src-  srcRowCount) + *(src+2*srcRowCount) )
        -30*( *(src-2*srcRowCount) + *(src+3*srcRowCount) )
        -16*( *(src-3*srcRowCount) + *(src+4*srcRowCount) )
        + 6*( *(src-4*srcRowCount) + *(src+5*srcRowCount) )
        + 2*( *(src-5*srcRowCount) + *(src+6*srcRowCount) )+256) / 512;
      if (result < 0) 
        result = 0;
      else if (result > pixMax) 
        result = pixMax;
      *dst = (uint16) result;
      dst += dstZres;
      src += srcZres;
    }
    src += srcRowCount;
  }

  limit = srcYres - (srcYres & 1);    // must round down to not exceed dst
  for (; y < limit; y+=2) 
  {
    n1 = (y < srcYres-1) ? 1 : 0;
    n2 = (y < srcYres-2) ? 2 : (srcYres-1-y);
    n3 = (y < srcYres-3) ? 3 : (srcYres-1-y);
    n4 = (y < srcYres-4) ? 4 : (srcYres-1-y);
    n5 = (y < srcYres-5) ? 5 : (srcYres-1-y);
    n6 = (y < srcYres-6) ? 6 : (srcYres-1-y);
    for (x=0; x < xres; x++) 
    {
      result = (225*( *(src              ) + *(src+n1*srcRowCount) )
        +69*( *(src-  srcRowCount) + *(src+n2*srcRowCount) )
        -30*( *(src-2*srcRowCount) + *(src+n3*srcRowCount) )
        -16*( *(src-3*srcRowCount) + *(src+n4*srcRowCount) )
        + 6*( *(src-4*srcRowCount) + *(src+n5*srcRowCount) )
        + 2*( *(src-5*srcRowCount) + *(src+n6*srcRowCount) )+256) / 512;
      if (result < 0) 
        result = 0;
      else if (result > pixMax) 
        result = pixMax;
      *dst = (uint16) result;
      dst += dstZres;
      src += srcZres;
    }
    src += srcRowCount;
  }
}                                     // vertical_half_1chan


/*!
 *****************************************************************************
 * \brief
 *    Reads entire tiff file from harddrive. Any processing is done
 *    in memory, reducing I/O overhead
 * \param input_file
 *    [in] Input file name etc. to read from
 * \param FrameNoInFile
 *    [in] Frame number in the source file
 * \param source
 *    [in] source file (on disk) information 
 * \param buf
 *    [out] memory buffer where image will be stored
 * \return
 *    1, success
 *    0, failure
 *****************************************************************************
 */
int ReadTIFFImage (InputParameters *p_Inp, VideoDataFile *input_file, int FrameNoInFile, FrameFormat *source, unsigned char *buf) 
{
  static   Tiff t;                          // Declaring it static allows it to "remember" memory allocations; ATOUR: Not a good idea since this affects reentrancy
  char     path[FILE_NAME_SIZE];
  int      frameNumberInFile, n, nComponents,   height, i, width, y;
  RGB_YUV  rgb_yuv;
  uint16
    *img, *temp = NULL, *temp2 = NULL,
    *p, *yp, *up, *vp;

  assert( p_Inp);
  assert( input_file);
  assert( source);
  assert( buf);

  width  = source->width[0];
  height = source->height[0];
  assert( (width & 1) == 0 && (height & 1) == 0);  // width & height must be even.

  if (source->color_model == CM_RGB && !input_file->is_interleaved) 
  {
    fprintf( stderr, "ReadTIFFImage:  RGB input file has not been declared as interleaved but only interleaved is supported\n");
    goto Error;
  }

  frameNumberInFile = FrameNoInFile + p_Inp->start_frame;
  if (input_file->num_digits > 0) 
  {
    if (input_file->zero_pad) 
    {
      n = snprintf( path, sizeof(path), "%s%0*d%s", input_file->fhead, input_file->num_digits, frameNumberInFile, input_file->ftail);
    } 
    else 
    {
      n = snprintf( path, sizeof(path), "%s%*d%s", input_file->fhead, input_file->num_digits, frameNumberInFile, input_file->ftail);
    }
    if (n == FILE_NAME_SIZE || n == -1) 
    {
      fprintf( stderr, "ReadTIFFImage:  file name is too large\n");
      return 0;
    }
  } 
  else 
  {
    strcpy( path, input_file->fname);    
  }

  if (readTiff( &t, path)) 
  {
    goto Error;
  }

  if ((int) t.ImageLength != height) 
  {
    fprintf( stderr, "ReadTIFFImage:  Tiff height (%u) different from encoder input height (%d) . Exiting...\n", t.ImageLength, height);
    goto Error;
  }
  if ((int) t.ImageWidth != width) 
  {
    fprintf( stderr, "ReadTIFFImage:  Tiff width (%u) different from encoder input width (%d) . Exiting...\n", t.ImageWidth, width);
    goto Error;
  }

  if (source->pic_unit_size_on_disk != 8 && source->pic_unit_size_on_disk != 16) {
    fprintf( stderr, "ReadTIFFImage only supports pic_unit_size_on_disk of 8 or 16 not %d\n", source->pic_unit_size_on_disk);
    goto Error;
  }

  // Transfer image to 'buf'

  img = t.img;                          // default setting points at TIFF image data.
  nComponents = width * height * 3;     // for RGB, which will be overridden by YUV420 etc.

  if (source->color_model == CM_YUV) 
  {
    if (RGB_YUV_initialize( &rgb_yuv, (VideoCode) p_Inp->videoCode, p_Inp->stdRange, 65535))
      goto Error;

    RGB_YUV_rgb_to_yuv( &rgb_yuv, img, img+1, img+2, width, height, 3, img, img+1, img+2, 3);
    switch (source->yuv_format) 
    {
    case YUV420:
      // allocate planar buffer
      nComponents = width * height * 3 / 2;
      yp = temp = (uint16 *) malloc( nComponents * sizeof(uint16));
      up = yp + width * height;
      vp = up + width * height / 4;
      // Y
      p = img;
      for (i=0; i < width*height; ++i) 
      {
        yp[i] = *p;  p += 3;
      }
      // subsample into planar buffer
      temp2 = (uint16 *) malloc( height * width * sizeof(uint16) / 2);
      // U
      horizontal_half_1chan_cosite( img+1, width, height, 3, temp2, 1, 65535);
      vertical_half_1chan( temp2, width/2, height, 1, up, 1, 65535);
      // V
      horizontal_half_1chan_cosite( img+2, width, height, 3, temp2, 1, 65535);
      vertical_half_1chan( temp2, width/2, height, 1, vp, 1, 65535);
      free_pointer(temp2);
      img = yp;                       // img points at result
      break;
    case YUV422:
      // allocate planar buffer
      nComponents = width * height * 2;
      yp = temp = (uint16 *) malloc( nComponents * sizeof(uint16));
      up = yp + width*height;
      vp = yp + width*height/2;
      // Y
      p = img;
      for (i=0; i < width*height; ++i) 
      {
        yp[i] = *p;  p += 3;
      }
      // U & V
      for (y=0; y < height; ++y) {
        horizontal_half_1chan_cosite( img + 1 + y*width*3, width, 1, 3, up + y * width/2, 1, 65535);
        horizontal_half_1chan_cosite( img + 2 + y*width*3, width, 1, 3, vp + y * width/2, 1, 65535);
      }
      img = yp;                       // img points at result
      break;
    case YUV444:
      break;
    default:
      fprintf( stderr, "ReadTIFFImage:  Unsupported ColorFormat (%d)\n", source->yuv_format);
      goto Error;
    }
  }

  switch (source->pic_unit_size_shift3) 
  {
  case 1:
    for (i=0; i < nComponents; ++i) 
    {
      buf[i] = (uint8)(img[i] >> 8);
    }
    break;
  case 2:
    for (i=0; i < nComponents; ++i) 
    {
      ((uint16 *) buf)[i] = img[i];
    }
    break;
  default:
    fprintf( stderr, "ReadTIFFImage only supports pic_unit_size_shift3 of 1 or 2 not %d\n", source->pic_unit_size_shift3);
    goto Error;
  }

  free_pointer(temp);
  return 1;

Error:

  free_pointer(temp);
    report_stats_on_error();

  return 0;
}
