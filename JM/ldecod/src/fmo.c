
/*!
 *****************************************************************************
 *
 * \file fmo.c
 *
 * \brief
 *    Support for Flexible Macroblock Ordering (FMO)
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Stephan Wenger      stewe@cs.tu-berlin.de
 *    - Karsten Suehring
 ******************************************************************************
 */

#include "global.h"
#include "elements.h"
#include "defines.h"
#include "header.h"
#include "fmo.h"
#include "fast_memory.h"

//#define PRINT_FMO_MAPS

static void FmoGenerateType0MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits );
static void FmoGenerateType1MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits );
static void FmoGenerateType2MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits );
static void FmoGenerateType3MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits, Slice *currSlice );
static void FmoGenerateType4MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits, Slice *currSlice );
static void FmoGenerateType5MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits, Slice *currSlice );
static void FmoGenerateType6MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits );


/*!
 ************************************************************************
 * \brief
 *    Generates p_Vid->MapUnitToSliceGroupMap
 *    Has to be called every time a new Picture Parameter Set is used
 *
 * \param p_Vid
 *      video encoding parameters for current picture
 *
 ************************************************************************
 */
static int FmoGenerateMapUnitToSliceGroupMap (VideoParameters *p_Vid, Slice *currSlice)
{
  seq_parameter_set_rbsp_t* sps = p_Vid->active_sps;
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps;

  unsigned int NumSliceGroupMapUnits;

  NumSliceGroupMapUnits = (sps->pic_height_in_map_units_minus1+1)* (sps->pic_width_in_mbs_minus1+1);

  if (pps->slice_group_map_type == 6)
  {
    if ((pps->pic_size_in_map_units_minus1 + 1) != NumSliceGroupMapUnits)
    {
      error ("wrong pps->pic_size_in_map_units_minus1 for used SPS and FMO type 6", 500);
    }
  }

  // allocate memory for p_Vid->MapUnitToSliceGroupMap
  if (p_Vid->MapUnitToSliceGroupMap)
    free (p_Vid->MapUnitToSliceGroupMap);
  if ((p_Vid->MapUnitToSliceGroupMap = malloc ((NumSliceGroupMapUnits) * sizeof (int))) == NULL)
  {
    printf ("cannot allocated %d bytes for p_Vid->MapUnitToSliceGroupMap, exit\n", (int) ( (pps->pic_size_in_map_units_minus1+1) * sizeof (int)));
    exit (-1);
  }

  if (pps->num_slice_groups_minus1 == 0)    // only one slice group
  {
    fast_memset (p_Vid->MapUnitToSliceGroupMap, 0, NumSliceGroupMapUnits * sizeof (int));
    return 0;
  }

  switch (pps->slice_group_map_type)
  {
  case 0:
    FmoGenerateType0MapUnitMap (p_Vid, NumSliceGroupMapUnits);
    break;
  case 1:
    FmoGenerateType1MapUnitMap (p_Vid, NumSliceGroupMapUnits);
    break;
  case 2:
    FmoGenerateType2MapUnitMap (p_Vid, NumSliceGroupMapUnits);
    break;
  case 3:
    FmoGenerateType3MapUnitMap (p_Vid, NumSliceGroupMapUnits, currSlice);
    break;
  case 4:
    FmoGenerateType4MapUnitMap (p_Vid, NumSliceGroupMapUnits, currSlice);
    break;
  case 5:
    FmoGenerateType5MapUnitMap (p_Vid, NumSliceGroupMapUnits, currSlice);
    break;
  case 6:
    FmoGenerateType6MapUnitMap (p_Vid, NumSliceGroupMapUnits);
    break;
  default:
    printf ("Illegal slice_group_map_type %d , exit \n", (int) pps->slice_group_map_type);
    exit (-1);
  }
  return 0;
}


/*!
 ************************************************************************
 * \brief
 *    Generates p_Vid->MbToSliceGroupMap from p_Vid->MapUnitToSliceGroupMap
 *
 * \param p_Vid
 *      video encoding parameters for current picture
 *
 ************************************************************************
 */
static int FmoGenerateMbToSliceGroupMap (VideoParameters *p_Vid, Slice *pSlice)
{
  seq_parameter_set_rbsp_t* sps = p_Vid->active_sps;

  unsigned i;

  // allocate memory for p_Vid->MbToSliceGroupMap
  if (p_Vid->MbToSliceGroupMap)
    free (p_Vid->MbToSliceGroupMap);

  if ((p_Vid->MbToSliceGroupMap = malloc ((p_Vid->PicSizeInMbs) * sizeof (int))) == NULL)
  {
    printf ("cannot allocate %d bytes for p_Vid->MbToSliceGroupMap, exit\n", (int) ((p_Vid->PicSizeInMbs) * sizeof (int)));
    exit (-1);
  }


  if ((sps->frame_mbs_only_flag)|| pSlice->field_pic_flag)
  {
    int *MbToSliceGroupMap = p_Vid->MbToSliceGroupMap;
    int *MapUnitToSliceGroupMap = p_Vid->MapUnitToSliceGroupMap;
    for (i=0; i<p_Vid->PicSizeInMbs; i++)
    {
      *MbToSliceGroupMap++ = *MapUnitToSliceGroupMap++;
    }
  }
  else
    if (sps->mb_adaptive_frame_field_flag  &&  (!pSlice->field_pic_flag))
    {
      for (i=0; i<p_Vid->PicSizeInMbs; i++)
      {
        p_Vid->MbToSliceGroupMap[i] = p_Vid->MapUnitToSliceGroupMap[i/2];
      }
    }
    else
    {
      for (i=0; i<p_Vid->PicSizeInMbs; i++)
      {
        p_Vid->MbToSliceGroupMap[i] = p_Vid->MapUnitToSliceGroupMap[(i/(2*p_Vid->PicWidthInMbs))*p_Vid->PicWidthInMbs+(i%p_Vid->PicWidthInMbs)];
      }
    }
  return 0;
}


/*!
 ************************************************************************
 * \brief
 *    FMO initialization: Generates p_Vid->MapUnitToSliceGroupMap and p_Vid->MbToSliceGroupMap.
 *
 * \param p_Vid
 *      video encoding parameters for current picture
 ************************************************************************
 */
int fmo_init(VideoParameters *p_Vid, Slice *pSlice)
{
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps;

#ifdef PRINT_FMO_MAPS
  unsigned i,j;
#endif

  FmoGenerateMapUnitToSliceGroupMap(p_Vid, pSlice);
  FmoGenerateMbToSliceGroupMap(p_Vid, pSlice);

  p_Vid->NumberOfSliceGroups = pps->num_slice_groups_minus1 + 1;

#ifdef PRINT_FMO_MAPS
  printf("\n");
  printf("FMO Map (Units):\n");

  for (j=0; j<p_Vid->PicHeightInMapUnits; j++)
  {
    for (i=0; i<p_Vid->PicWidthInMbs; i++)
    {
      printf("%c",48+p_Vid->MapUnitToSliceGroupMap[i+j*p_Vid->PicWidthInMbs]);
    }
    printf("\n");
  }
  printf("\n");
  printf("FMO Map (Mb):\n");

  for (j=0; j<p_Vid->PicHeightInMbs; j++)
  {
    for (i=0; i<p_Vid->PicWidthInMbs; i++)
    {
      printf("%c",48 + p_Vid->MbToSliceGroupMap[i + j * p_Vid->PicWidthInMbs]);
    }
    printf("\n");
  }
  printf("\n");

#endif

  return 0;
}


/*!
 ************************************************************************
 * \brief
 *    Free memory allocated by FMO functions
 ************************************************************************
 */
int FmoFinit(VideoParameters *p_Vid)
{
  if (p_Vid->MbToSliceGroupMap)
  {
    free (p_Vid->MbToSliceGroupMap);
    p_Vid->MbToSliceGroupMap = NULL;
  }
  if (p_Vid->MapUnitToSliceGroupMap)
  {
    free (p_Vid->MapUnitToSliceGroupMap);
    p_Vid->MapUnitToSliceGroupMap = NULL;
  }
  return 0;
}


/*!
 ************************************************************************
 * \brief
 *    FmoGetNumberOfSliceGroup(p_Vid)
 *
 * \par p_Vid:
 *    VideoParameters
 ************************************************************************
 */
int FmoGetNumberOfSliceGroup(VideoParameters *p_Vid)
{
  return p_Vid->NumberOfSliceGroups;
}


/*!
 ************************************************************************
 * \brief
 *    FmoGetLastMBOfPicture(p_Vid)
 *    returns the macroblock number of the last MB in a picture.  This
 *    mb happens to be the last macroblock of the picture if there is only
 *    one slice group
 *
 * \par Input:
 *    None
 ************************************************************************
 */
int FmoGetLastMBOfPicture(VideoParameters *p_Vid)
{
  return FmoGetLastMBInSliceGroup (p_Vid, FmoGetNumberOfSliceGroup(p_Vid)-1);
}


/*!
 ************************************************************************
 * \brief
 *    FmoGetLastMBInSliceGroup: Returns MB number of last MB in SG
 *
 * \par Input:
 *    SliceGroupID (0 to 7)
 ************************************************************************
 */

int FmoGetLastMBInSliceGroup (VideoParameters *p_Vid, int SliceGroup)
{
  int i;

  for (i=p_Vid->PicSizeInMbs-1; i>=0; i--)
    if (FmoGetSliceGroupId (p_Vid, i) == SliceGroup)
      return i;
  return -1;

}


/*!
 ************************************************************************
 * \brief
 *    Returns SliceGroupID for a given MB
 *
 * \param p_Vid
 *      video encoding parameters for current picture
 * \param mb
 *    Macroblock number (in scan order)
 ************************************************************************
 */
int FmoGetSliceGroupId (VideoParameters *p_Vid, int mb)
{
  assert (mb < (int) p_Vid->PicSizeInMbs);
  assert (p_Vid->MbToSliceGroupMap != NULL);
  return p_Vid->MbToSliceGroupMap[mb];
}


/*!
 ************************************************************************
 * \brief
 *    FmoGetNextMBBr: Returns the MB-Nr (in scan order) of the next
 *    MB in the (scattered) Slice, -1 if the slice is finished
 * \param p_Vid
 *      video encoding parameters for current picture
 *
 * \param CurrentMbNr
 *    number of the current macroblock
 ************************************************************************
 */
int FmoGetNextMBNr (VideoParameters *p_Vid, int CurrentMbNr)
{
  int SliceGroup = FmoGetSliceGroupId (p_Vid, CurrentMbNr);

  while (++CurrentMbNr<(int)p_Vid->PicSizeInMbs && p_Vid->MbToSliceGroupMap [CurrentMbNr] != SliceGroup)
    ;

  if (CurrentMbNr >= (int)p_Vid->PicSizeInMbs)
    return -1;    // No further MB in this slice (could be end of picture)
  else
    return CurrentMbNr;
}


/*!
 ************************************************************************
 * \brief
 *    Generate interleaved slice group map type MapUnit map (type 0)
 *
 ************************************************************************
 */
static void FmoGenerateType0MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits )
{
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps;
  unsigned iGroup, j;
  unsigned i = 0;
  do
  {
    for( iGroup = 0;
         (iGroup <= pps->num_slice_groups_minus1) && (i < PicSizeInMapUnits);
         i += pps->run_length_minus1[iGroup++] + 1 )
    {
      for( j = 0; j <= pps->run_length_minus1[ iGroup ] && i + j < PicSizeInMapUnits; j++ )
        p_Vid->MapUnitToSliceGroupMap[i+j] = iGroup;
    }
  }
  while( i < PicSizeInMapUnits );
}


/*!
 ************************************************************************
 * \brief
 *    Generate dispersed slice group map type MapUnit map (type 1)
 *
 ************************************************************************
 */
static void FmoGenerateType1MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits )
{
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps;
  unsigned i;
  for( i = 0; i < PicSizeInMapUnits; i++ )
  {
    p_Vid->MapUnitToSliceGroupMap[i] = ((i%p_Vid->PicWidthInMbs)+(((i/p_Vid->PicWidthInMbs)*(pps->num_slice_groups_minus1+1))/2))
                                %(pps->num_slice_groups_minus1+1);
  }
}

/*!
 ************************************************************************
 * \brief
 *    Generate foreground with left-over slice group map type MapUnit map (type 2)
 *
 ************************************************************************
 */
static void FmoGenerateType2MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits )
{
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps;
  int iGroup;
  unsigned i, x, y;
  unsigned yTopLeft, xTopLeft, yBottomRight, xBottomRight;

  for( i = 0; i < PicSizeInMapUnits; i++ )
    p_Vid->MapUnitToSliceGroupMap[ i ] = pps->num_slice_groups_minus1;

  for( iGroup = pps->num_slice_groups_minus1 - 1 ; iGroup >= 0; iGroup-- )
  {
    yTopLeft = pps->top_left[ iGroup ] / p_Vid->PicWidthInMbs;
    xTopLeft = pps->top_left[ iGroup ] % p_Vid->PicWidthInMbs;
    yBottomRight = pps->bottom_right[ iGroup ] / p_Vid->PicWidthInMbs;
    xBottomRight = pps->bottom_right[ iGroup ] % p_Vid->PicWidthInMbs;
    for( y = yTopLeft; y <= yBottomRight; y++ )
      for( x = xTopLeft; x <= xBottomRight; x++ )
        p_Vid->MapUnitToSliceGroupMap[ y * p_Vid->PicWidthInMbs + x ] = iGroup;
 }
}


/*!
 ************************************************************************
 * \brief
 *    Generate box-out slice group map type MapUnit map (type 3)
 *
 ************************************************************************
 */
static void FmoGenerateType3MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits, Slice *currSlice )
{
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps;
  unsigned i, k;
  int leftBound, topBound, rightBound, bottomBound;
  int x, y, xDir, yDir;
  int mapUnitVacant;

  unsigned mapUnitsInSliceGroup0 = imin((pps->slice_group_change_rate_minus1 + 1) * currSlice->slice_group_change_cycle, PicSizeInMapUnits);

  for( i = 0; i < PicSizeInMapUnits; i++ )
    p_Vid->MapUnitToSliceGroupMap[ i ] = 2;

  x = ( p_Vid->PicWidthInMbs - pps->slice_group_change_direction_flag ) / 2;
  y = ( p_Vid->PicHeightInMapUnits - pps->slice_group_change_direction_flag ) / 2;

  leftBound   = x;
  topBound    = y;
  rightBound  = x;
  bottomBound = y;

  xDir =  pps->slice_group_change_direction_flag - 1;
  yDir =  pps->slice_group_change_direction_flag;

  for( k = 0; k < PicSizeInMapUnits; k += mapUnitVacant )
  {
    mapUnitVacant = ( p_Vid->MapUnitToSliceGroupMap[ y * p_Vid->PicWidthInMbs + x ]  ==  2 );
    if( mapUnitVacant )
       p_Vid->MapUnitToSliceGroupMap[ y * p_Vid->PicWidthInMbs + x ] = ( k >= mapUnitsInSliceGroup0 );

    if( xDir  ==  -1  &&  x  ==  leftBound )
    {
      leftBound = imax( leftBound - 1, 0 );
      x = leftBound;
      xDir = 0;
      yDir = 2 * pps->slice_group_change_direction_flag - 1;
    }
    else
      if( xDir  ==  1  &&  x  ==  rightBound )
      {
        rightBound = imin( rightBound + 1, (int)p_Vid->PicWidthInMbs - 1 );
        x = rightBound;
        xDir = 0;
        yDir = 1 - 2 * pps->slice_group_change_direction_flag;
      }
      else
        if( yDir  ==  -1  &&  y  ==  topBound )
        {
          topBound = imax( topBound - 1, 0 );
          y = topBound;
          xDir = 1 - 2 * pps->slice_group_change_direction_flag;
          yDir = 0;
         }
        else
          if( yDir  ==  1  &&  y  ==  bottomBound )
          {
            bottomBound = imin( bottomBound + 1, (int)p_Vid->PicHeightInMapUnits - 1 );
            y = bottomBound;
            xDir = 2 * pps->slice_group_change_direction_flag - 1;
            yDir = 0;
          }
          else
          {
            x = x + xDir;
            y = y + yDir;
          }
  }

}

/*!
 ************************************************************************
 * \brief
 *    Generate raster scan slice group map type MapUnit map (type 4)
 *
 ************************************************************************
 */
static void FmoGenerateType4MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits, Slice *currSlice )
{
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps;

  unsigned mapUnitsInSliceGroup0 = imin((pps->slice_group_change_rate_minus1 + 1) * currSlice->slice_group_change_cycle, PicSizeInMapUnits);
  unsigned sizeOfUpperLeftGroup = pps->slice_group_change_direction_flag ? ( PicSizeInMapUnits - mapUnitsInSliceGroup0 ) : mapUnitsInSliceGroup0;

  unsigned i;

  for( i = 0; i < PicSizeInMapUnits; i++ )
    if( i < sizeOfUpperLeftGroup )
        p_Vid->MapUnitToSliceGroupMap[ i ] = pps->slice_group_change_direction_flag;
    else
        p_Vid->MapUnitToSliceGroupMap[ i ] = 1 - pps->slice_group_change_direction_flag;

}

/*!
 ************************************************************************
 * \brief
 *    Generate wipe slice group map type MapUnit map (type 5)
 *
 ************************************************************************
 */
static void FmoGenerateType5MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits, Slice *currSlice )
{
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps;

  unsigned mapUnitsInSliceGroup0 = imin((pps->slice_group_change_rate_minus1 + 1) * currSlice->slice_group_change_cycle, PicSizeInMapUnits);
  unsigned sizeOfUpperLeftGroup = pps->slice_group_change_direction_flag ? ( PicSizeInMapUnits - mapUnitsInSliceGroup0 ) : mapUnitsInSliceGroup0;

  unsigned i,j, k = 0;

  for( j = 0; j < p_Vid->PicWidthInMbs; j++ )
    for( i = 0; i < p_Vid->PicHeightInMapUnits; i++ )
        if( k++ < sizeOfUpperLeftGroup )
            p_Vid->MapUnitToSliceGroupMap[ i * p_Vid->PicWidthInMbs + j ] = pps->slice_group_change_direction_flag;
        else
            p_Vid->MapUnitToSliceGroupMap[ i * p_Vid->PicWidthInMbs + j ] = 1 - pps->slice_group_change_direction_flag;

}

/*!
 ************************************************************************
 * \brief
 *    Generate explicit slice group map type MapUnit map (type 6)
 *
 ************************************************************************
 */
static void FmoGenerateType6MapUnitMap (VideoParameters *p_Vid, unsigned PicSizeInMapUnits )
{
  pic_parameter_set_rbsp_t* pps = p_Vid->active_pps; 
  unsigned i;
  for (i=0; i<PicSizeInMapUnits; i++)
  {
    p_Vid->MapUnitToSliceGroupMap[i] = pps->slice_group_id[i];
  }
}

