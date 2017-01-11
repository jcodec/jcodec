
/*!
 *************************************************************************************
 * \file loopFilter.c
 *
 * \brief
 *    Filter to reduce blocking artifacts on a macroblock level.
 *    The filter strength is QP dependent.
 *
 * \author
 *    Contributors:
 *    - Peter List       Peter.List@t-systems.de:  Original code                                 (13-Aug-2001)
 *    - Jani Lainema     Jani.Lainema@nokia.com:   Some bug fixing, removal of recursiveness     (16-Aug-2001)
 *    - Peter List       Peter.List@t-systems.de:  inplace filtering and various simplifications (10-Jan-2002)
 *    - Anthony Joch     anthony@ubvideo.com:      Simplified switching between filters and
 *                                                 non-recursive default filter.                 (08-Jul-2002)
 *    - Cristina Gomila  cristina.gomila@thomson.net: Simplification of the chroma deblocking
 *                                                    from JVT-E089                              (21-Nov-2002)
 *    - Alexis Michael Tourapis atour@dolby.com:   Speed/Architecture improvements               (08-Feb-2007)
 *************************************************************************************
 */

#include "global.h"
#include "image.h"
#include "mb_access.h"
#include "loopfilter.h"
#include "loop_filter.h"

static void DeblockMb      (VideoParameters *p_Vid, StorablePicture *p, int MbQAddr);
static void perform_db     (VideoParameters *p_Vid, StorablePicture *p, int MbQAddr);
static void get_db_strength(VideoParameters *p_Vid, StorablePicture *p, int MbQAddr);

extern void set_loop_filter_functions_mbaff(VideoParameters *p_Vid);
extern void set_loop_filter_functions_normal(VideoParameters *p_Vid);
extern void deblock_normal(VideoParameters *p_Vid, StorablePicture *p);
extern void get_strength_ver_MBAff     (byte *Strength, Macroblock *MbQ, int edge, int mvlimit, StorablePicture *p);
extern void get_strength_hor_MBAff     (byte *Strength, Macroblock *MbQ, int edge, int mvlimit, StorablePicture *p);

#if (JM_PARALLEL_DEBLOCK == 0)
/*!
 *****************************************************************************************
 * \brief
 *    Filter all macroblocks in order of increasing macroblock address.
 *****************************************************************************************
 */
void DeblockPicture(VideoParameters *p_Vid, StorablePicture *p)
{
  unsigned i;
  if (p->mb_aff_frame_flag)
  {
    for (i = 0; i < p->PicSizeInMbs; ++i)
    {
      DeblockMb( p_Vid, p, i ) ;
    }
  }
  else
  {
   // deblock_normal( p_Vid, p);
    
    for (i = 0; i < p->PicSizeInMbs; ++i)
    {
      get_db_strength( p_Vid, p, i ) ;
    }
    for (i = 0; i < p->PicSizeInMbs; ++i)
    {
      perform_db( p_Vid, p, i ) ;
    }
    
  }
}
#else
static void DeblockParallel(VideoParameters *p_Vid, StorablePicture *p, unsigned int column, int block, int n_last)
{
  int i, j;
  
  for (j = 0; j < GROUP_SIZE; j++)
  {
    i = block++ * (p_Vid->PicWidthInMbs - 2) + column;

    perform_db( p_Vid, p, i ) ;
    if (block == n_last) break;
  }
}

/*!
 *****************************************************************************************
 * \brief
 *    Filter all macroblocks in a diagonal manner to enable parallelization.
 *****************************************************************************************
 */
void DeblockPicture(VideoParameters *p_Vid, StorablePicture *p)
{
  int iheightMBs =(p_Vid->PicSizeInMbs/p_Vid->PicWidthInMbs);
  unsigned int i, k = p->PicWidthInMbs + 2 * (iheightMBs - 1);

#if defined(OPENMP)
  int j;
    #pragma omp parallel for
#endif
  for (j = 0; j < p->PicSizeInMbs; ++j)
  {
    get_db_strength( p_Vid, p, j ) ;
  }
 
  for (i = 0; i < k; i++)
  {
    int nn;    
    int n_last = imin(iheightMBs, (i >> 1) + 1);
    int n_start = (i < p->PicWidthInMbs) ? 0 : ((i - p->PicWidthInMbs) >> 1) + 1;

#if defined(OPENMP)
    #pragma omp parallel for
#endif
    for (nn = n_start; nn < n_last; nn += GROUP_SIZE)
      DeblockParallel(p_Vid, p, i, nn, n_last);
  }
}
#endif

// likely already set - see testing via asserts
static void init_neighbors(VideoParameters *p_Vid)
{
  int i, j;
  int width = p_Vid->PicWidthInMbs;
  int height = p_Vid->PicHeightInMbs;
  int size = p_Vid->PicSizeInMbs;
  Macroblock *currMB = &p_Vid->mb_data[0];
  // do the top left corner
  currMB->mbup = NULL;
  currMB->mbleft = NULL;
  currMB++;
  // do top row
  for (i = 1; i < width; i++) 
  {
    currMB->mbup = NULL;
    currMB->mbleft = currMB - 1;
    currMB++;
  }

  // do left edge
  for (i = width; i < size; i += width) 
  {
    currMB->mbup = currMB - width;
    currMB->mbleft = NULL;   
    currMB += width;
  }
  // do all others
  for (j = width + 1; j < width * height + 1; j += width) 
  {
    currMB = &p_Vid->mb_data[j];
    for (i = 1; i < width; i++) 
    {
      currMB->mbup   = currMB - width;
      currMB->mbleft = currMB - 1;
      currMB++;
    }
  }
}


void  init_Deblock(VideoParameters *p_Vid, int mb_aff_frame_flag)
{
  if(p_Vid->yuv_format == YUV444 && p_Vid->separate_colour_plane_flag)
  {
    change_plane_JV(p_Vid, PLANE_Y, NULL);
    init_neighbors(p_Dec->p_Vid);
    change_plane_JV(p_Vid, PLANE_U, NULL);
    init_neighbors(p_Dec->p_Vid);
    change_plane_JV(p_Vid, PLANE_V, NULL);
    init_neighbors(p_Dec->p_Vid);
    change_plane_JV(p_Vid, PLANE_Y, NULL);
  }
  else 
    init_neighbors(p_Dec->p_Vid);
  if (mb_aff_frame_flag == 1) 
  {
    set_loop_filter_functions_mbaff(p_Vid);
  }
  else
  {
    set_loop_filter_functions_normal(p_Vid);
  }
}

/*!
 *****************************************************************************************
 * \brief
 *    Deblocking filter for one macroblock.
 *****************************************************************************************
 */

static void DeblockMb(VideoParameters *p_Vid, StorablePicture *p, int MbQAddr)
{
  Macroblock   *MbQ = &(p_Vid->mb_data[MbQAddr]) ; // current Mb

  // return, if filter is disabled
  if (MbQ->DFDisableIdc == 1) 
  {
    MbQ->DeblockCall = 0;
  }
  else
  {
    int           edge;

    byte Strength[16];
    short         mb_x, mb_y;

    int           filterNon8x8LumaEdgesFlag[4] = {1,1,1,1};
    int           filterLeftMbEdgeFlag;
    int           filterTopMbEdgeFlag;
    int           edge_cr;

    imgpel     **imgY = p->imgY;
    imgpel   ***imgUV = p->imgUV;
    Slice  *currSlice = MbQ->p_Slice;
    int       mvlimit = ((p->structure!=FRAME) || (p->mb_aff_frame_flag && MbQ->mb_field)) ? 2 : 4;

    seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;

    MbQ->DeblockCall = 1;
    get_mb_pos (p_Vid, MbQAddr, p_Vid->mb_size[IS_LUMA], &mb_x, &mb_y);

    if (MbQ->mb_type == I8MB)
      assert(MbQ->luma_transform_size_8x8_flag);

    filterNon8x8LumaEdgesFlag[1] =
      filterNon8x8LumaEdgesFlag[3] = !(MbQ->luma_transform_size_8x8_flag);

    filterLeftMbEdgeFlag = (mb_x != 0);
    filterTopMbEdgeFlag  = (mb_y != 0);

    if (p->mb_aff_frame_flag && mb_y == MB_BLOCK_SIZE && MbQ->mb_field)
      filterTopMbEdgeFlag = 0;

    if (MbQ->DFDisableIdc==2)
    {
      // don't filter at slice boundaries
      filterLeftMbEdgeFlag = MbQ->mbAvailA;
      // if this the bottom of a frame macroblock pair then always filter the top edge
      filterTopMbEdgeFlag  = (p->mb_aff_frame_flag && !MbQ->mb_field && (MbQAddr & 0x01)) ? 1 : MbQ->mbAvailB;
    }

    if (p->mb_aff_frame_flag == 1) 
      CheckAvailabilityOfNeighborsMBAFF(MbQ);

    // Vertical deblocking
    for (edge = 0; edge < 4 ; ++edge )    
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0 && (currSlice->slice_type == P_SLICE || currSlice->slice_type == B_SLICE))
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc != YUV444)
          continue;
        else if (edge > 0)
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P16x8)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P8x16) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterLeftMbEdgeFlag )
      {   
        // Strength for 4 blks in 1 stripe
        get_strength_ver_MBAff(Strength, MbQ, edge << 2, mvlimit, p);

        if ( Strength[0] != 0 || Strength[1] != 0 || Strength[2] != 0 || Strength[3] !=0 ||
        Strength[4] != 0 || Strength[5] != 0 || Strength[6] != 0 || Strength[7] !=0 ||
        Strength[8] != 0 || Strength[9] != 0 || Strength[10] != 0 || Strength[11] !=0 ||
        Strength[12] != 0 || Strength[13] != 0 || Strength[14] != 0 || Strength[15] !=0 ) // only if one of the 16 Strength bytes is != 0
        {
          if (filterNon8x8LumaEdgesFlag[edge])
          {
            p_Vid->EdgeLoopLumaVer( PLANE_Y, imgY, Strength, MbQ, edge << 2);
            if(currSlice->chroma444_not_separate)
            {
              p_Vid->EdgeLoopLumaVer(PLANE_U, imgUV[0], Strength, MbQ, edge << 2);
              p_Vid->EdgeLoopLumaVer(PLANE_V, imgUV[1], Strength, MbQ, edge << 2);
            }
          }
          if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422)
          {
            edge_cr = chroma_edge[0][edge][p->chroma_format_idc];
            if( (imgUV != NULL) && (edge_cr >= 0))
            {
              p_Vid->EdgeLoopChromaVer( imgUV[0], Strength, MbQ, edge_cr, 0, p);
              p_Vid->EdgeLoopChromaVer( imgUV[1], Strength, MbQ, edge_cr, 1, p);
            }
          }
        }        
      }
    }//end edge

    // horizontal deblocking  
    for( edge = 0; edge < 4 ; ++edge )
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0 && (currSlice->slice_type == P_SLICE || currSlice->slice_type == B_SLICE))
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc==YUV420)
          continue;
        else if (edge > 0)
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P8x16)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P16x8) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterTopMbEdgeFlag )
      {
        // Strength for 4 blks in 1 stripe
        get_strength_hor_MBAff(Strength, MbQ, edge << 2, mvlimit, p);

        if ( Strength[0] != 0 || Strength[1] != 0 || Strength[2] != 0 || Strength[3] !=0 ||
        Strength[4] != 0 || Strength[5] != 0 || Strength[6] != 0 || Strength[7] !=0 ||
        Strength[8] != 0 || Strength[9] != 0 || Strength[10] != 0 || Strength[11] !=0 ||
        Strength[12] != 0 || Strength[13] != 0 || Strength[14] != 0 || Strength[15] !=0 ) // only if one of the 16 Strength bytes is != 0
        {
          if (filterNon8x8LumaEdgesFlag[edge])
          {
            p_Vid->EdgeLoopLumaHor( PLANE_Y, imgY, Strength, MbQ, edge << 2, p) ;
            if(currSlice->chroma444_not_separate)
            {
              p_Vid->EdgeLoopLumaHor(PLANE_U, imgUV[0], Strength, MbQ, edge << 2, p);
              p_Vid->EdgeLoopLumaHor(PLANE_V, imgUV[1], Strength, MbQ, edge << 2, p);
            }
          }

          if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422)
          {
            edge_cr = chroma_edge[1][edge][p->chroma_format_idc];
            if( (imgUV != NULL) && (edge_cr >= 0))
            {
              p_Vid->EdgeLoopChromaHor( imgUV[0], Strength, MbQ, edge_cr, 0, p);
              p_Vid->EdgeLoopChromaHor( imgUV[1], Strength, MbQ, edge_cr, 1, p);
            }
          }
        }

        if (!edge && !MbQ->mb_field && MbQ->mixedModeEdgeFlag) //currSlice->mixedModeEdgeFlag) 
        {        
          // this is the extra horizontal edge between a frame macroblock pair and a field above it
          MbQ->DeblockCall = 2;
          get_strength_hor_MBAff(Strength, MbQ, MB_BLOCK_SIZE, mvlimit, p); // Strength for 4 blks in 1 stripe

          //if( *((int*)Strength) )                      // only if one of the 4 Strength bytes is != 0
          {
            if (filterNon8x8LumaEdgesFlag[edge])
            {

              p_Vid->EdgeLoopLumaHor(PLANE_Y, imgY, Strength, MbQ, MB_BLOCK_SIZE, p) ;
              if(currSlice->chroma444_not_separate)
              {
                p_Vid->EdgeLoopLumaHor(PLANE_U, imgUV[0], Strength, MbQ, MB_BLOCK_SIZE, p) ;
                p_Vid->EdgeLoopLumaHor(PLANE_V, imgUV[1], Strength, MbQ, MB_BLOCK_SIZE, p) ;
              }
            }
            if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422) 
            {
              edge_cr = chroma_edge[1][edge][p->chroma_format_idc];
              if( (imgUV != NULL) && (edge_cr >= 0))
              {
                p_Vid->EdgeLoopChromaHor( imgUV[0], Strength, MbQ, MB_BLOCK_SIZE, 0, p) ;
                p_Vid->EdgeLoopChromaHor( imgUV[1], Strength, MbQ, MB_BLOCK_SIZE, 1, p) ;
              }
            }
          }
          MbQ->DeblockCall = 1;
        }
      }
    }//end edge  

    MbQ->DeblockCall = 0;
  }
}

/*!
 *****************************************************************************************
 * \brief
 *    Deblocking filter for one macroblock.
 *****************************************************************************************
 */
static void get_db_strength(VideoParameters *p_Vid, StorablePicture *p, int MbQAddr)
{
  Macroblock   *MbQ = &(p_Vid->mb_data[MbQAddr]) ; // current Mb

  // return, if filter is disabled
  if (MbQ->DFDisableIdc == 1) 
  {
    MbQ->DeblockCall = 0;
  }
  else
  {
    int           edge;

    short         mb_x, mb_y;

    int           filterNon8x8LumaEdgesFlag[4] = {1,1,1,1};
    int           filterLeftMbEdgeFlag;
    int           filterTopMbEdgeFlag;

    Slice  *currSlice = MbQ->p_Slice;
    int       mvlimit = ((p->structure!=FRAME) || (p->mb_aff_frame_flag && MbQ->mb_field)) ? 2 : 4;

    seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;

    MbQ->DeblockCall = 1;
    get_mb_pos (p_Vid, MbQAddr, p_Vid->mb_size[IS_LUMA], &mb_x, &mb_y);

    if (MbQ->mb_type == I8MB)
      assert(MbQ->luma_transform_size_8x8_flag);

    filterNon8x8LumaEdgesFlag[1] =
      filterNon8x8LumaEdgesFlag[3] = !(MbQ->luma_transform_size_8x8_flag);

    filterLeftMbEdgeFlag = (mb_x != 0);
    filterTopMbEdgeFlag  = (mb_y != 0);

    if (p->mb_aff_frame_flag && mb_y == MB_BLOCK_SIZE && MbQ->mb_field)
      filterTopMbEdgeFlag = 0;

    if (MbQ->DFDisableIdc==2)
    {
      // don't filter at slice boundaries
      filterLeftMbEdgeFlag = MbQ->mbAvailA;
      // if this the bottom of a frame macroblock pair then always filter the top edge
      filterTopMbEdgeFlag  = (p->mb_aff_frame_flag && !MbQ->mb_field && (MbQAddr & 0x01)) ? 1 : MbQ->mbAvailB;
    }

    if (p->mb_aff_frame_flag == 1) 
      CheckAvailabilityOfNeighborsMBAFF(MbQ);

    // Vertical deblocking
    for (edge = 0; edge < 4 ; ++edge )    
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0 && (currSlice->slice_type == P_SLICE || currSlice->slice_type == B_SLICE))
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc != YUV444)
          continue;
        else if (edge > 0)
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P16x8)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P8x16) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterLeftMbEdgeFlag )
      {      
        // Strength for 4 blks in 1 stripe
        p_Vid->GetStrengthVer(MbQ, edge, mvlimit, p);
      }
    }//end edge

    // horizontal deblocking  
    for( edge = 0; edge < 4 ; ++edge )
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0 && (currSlice->slice_type == P_SLICE || currSlice->slice_type == B_SLICE))
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc==YUV420)
          continue;
        else if (edge > 0)
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P8x16)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P16x8) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterTopMbEdgeFlag )
      {
        p_Vid->GetStrengthHor(MbQ, edge, mvlimit, p);
      }
    }//end edge

    MbQ->DeblockCall = 0;
  }
}


static void perform_db(VideoParameters *p_Vid, StorablePicture *p, int MbQAddr)
{
  Macroblock   *MbQ = &(p_Vid->mb_data[MbQAddr]) ; // current Mb

  // return, if filter is disabled
  if (MbQ->DFDisableIdc == 1) 
  {
    MbQ->DeblockCall = 0;
  }
  else
  {
    int           edge;

    short         mb_x, mb_y;

    int           filterNon8x8LumaEdgesFlag[4] = {1,1,1,1};
    int           filterLeftMbEdgeFlag;
    int           filterTopMbEdgeFlag;
    int           edge_cr;

    imgpel     **imgY = p->imgY;
    imgpel   ***imgUV = p->imgUV;
    Slice  *currSlice = MbQ->p_Slice;
    int       mvlimit = ((p->structure!=FRAME) || (p->mb_aff_frame_flag && MbQ->mb_field)) ? 2 : 4;

    seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;

    MbQ->DeblockCall = 1;
    get_mb_pos (p_Vid, MbQAddr, p_Vid->mb_size[IS_LUMA], &mb_x, &mb_y);

    if (MbQ->mb_type == I8MB)
      assert(MbQ->luma_transform_size_8x8_flag);

    filterNon8x8LumaEdgesFlag[1] =
      filterNon8x8LumaEdgesFlag[3] = !(MbQ->luma_transform_size_8x8_flag);

    filterLeftMbEdgeFlag = (mb_x != 0);
    filterTopMbEdgeFlag  = (mb_y != 0);

    if (p->mb_aff_frame_flag && mb_y == MB_BLOCK_SIZE && MbQ->mb_field)
      filterTopMbEdgeFlag = 0;

    if (MbQ->DFDisableIdc==2)
    {
      // don't filter at slice boundaries
      filterLeftMbEdgeFlag = MbQ->mbAvailA;
      // if this the bottom of a frame macroblock pair then always filter the top edge
      filterTopMbEdgeFlag  = (p->mb_aff_frame_flag && !MbQ->mb_field && (MbQAddr & 0x01)) ? 1 : MbQ->mbAvailB;
    }

    if (p->mb_aff_frame_flag == 1) 
      CheckAvailabilityOfNeighborsMBAFF(MbQ);

    // Vertical deblocking
    for (edge = 0; edge < 4 ; ++edge )    
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0 && (currSlice->slice_type == P_SLICE || currSlice->slice_type == B_SLICE))
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc != YUV444)
          continue;
        else if (edge > 0)
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P16x8)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P8x16) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterLeftMbEdgeFlag )
      {      
        byte *Strength = MbQ->strength_ver[edge];

        if ( Strength[0] != 0 || Strength[1] != 0 || Strength[2] != 0 || Strength[3] != 0 ) // only if one of the 4 first Strength bytes is != 0
        {
          if (filterNon8x8LumaEdgesFlag[edge])
          {
            p_Vid->EdgeLoopLumaVer( PLANE_Y, imgY, Strength, MbQ, edge << 2);
            if(currSlice->chroma444_not_separate)
            {
              p_Vid->EdgeLoopLumaVer(PLANE_U, imgUV[0], Strength, MbQ, edge << 2);
              p_Vid->EdgeLoopLumaVer(PLANE_V, imgUV[1], Strength, MbQ, edge << 2);
            }
          }
          if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422)
          {
            edge_cr = chroma_edge[0][edge][p->chroma_format_idc];
            if( (imgUV != NULL) && (edge_cr >= 0))
            {
              p_Vid->EdgeLoopChromaVer( imgUV[0], Strength, MbQ, edge_cr, 0, p);
              p_Vid->EdgeLoopChromaVer( imgUV[1], Strength, MbQ, edge_cr, 1, p);
            }
          }
        }        
      }
    }//end edge

    // horizontal deblocking  
    for( edge = 0; edge < 4 ; ++edge )
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0 && (currSlice->slice_type == P_SLICE || currSlice->slice_type == B_SLICE))
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc==YUV420)
          continue;
        else if (edge > 0)
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P8x16)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P16x8) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterTopMbEdgeFlag )
      {
        byte *Strength = MbQ->strength_hor[edge];

        if ( Strength[0] != 0 || Strength[1] != 0 || Strength[2] != 0 || Strength[3] !=0 ||
        Strength[4] != 0 || Strength[5] != 0 || Strength[6] != 0 || Strength[7] !=0 ||
        Strength[8] != 0 || Strength[9] != 0 || Strength[10] != 0 || Strength[11] !=0 ||
        Strength[12] != 0 || Strength[13] != 0 || Strength[14] != 0 || Strength[15] !=0 ) // only if one of the 16 Strength bytes is != 0
        {
          if (filterNon8x8LumaEdgesFlag[edge])
          {
            p_Vid->EdgeLoopLumaHor( PLANE_Y, imgY, Strength, MbQ, edge << 2, p) ;
            if(currSlice->chroma444_not_separate)
            {
              p_Vid->EdgeLoopLumaHor(PLANE_U, imgUV[0], Strength, MbQ, edge << 2, p);
              p_Vid->EdgeLoopLumaHor(PLANE_V, imgUV[1], Strength, MbQ, edge << 2, p);
            }
          }

          if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422)
          {
            edge_cr = chroma_edge[1][edge][p->chroma_format_idc];
            if( (imgUV != NULL) && (edge_cr >= 0))
            {
              p_Vid->EdgeLoopChromaHor( imgUV[0], Strength, MbQ, edge_cr, 0, p);
              p_Vid->EdgeLoopChromaHor( imgUV[1], Strength, MbQ, edge_cr, 1, p);
            }
          }
        }

        if (!edge && !MbQ->mb_field && MbQ->mixedModeEdgeFlag) //currSlice->mixedModeEdgeFlag) 
        {          
          // this is the extra horizontal edge between a frame macroblock pair and a field above it
          MbQ->DeblockCall = 2;
          p_Vid->GetStrengthHor(MbQ, 4, mvlimit, p); // Strength for 4 blks in 1 stripe

          //if( *((int*)Strength) )                      // only if one of the 4 Strength bytes is != 0
          {
            if (filterNon8x8LumaEdgesFlag[edge])
            {

              p_Vid->EdgeLoopLumaHor(PLANE_Y, imgY, Strength, MbQ, MB_BLOCK_SIZE, p) ;
              if(currSlice->chroma444_not_separate)
              {
                p_Vid->EdgeLoopLumaHor(PLANE_U, imgUV[0], Strength, MbQ, MB_BLOCK_SIZE, p) ;
                p_Vid->EdgeLoopLumaHor(PLANE_V, imgUV[1], Strength, MbQ, MB_BLOCK_SIZE, p) ;
              }
            }
            if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422) 
            {
              edge_cr = chroma_edge[1][edge][p->chroma_format_idc];
              if( (imgUV != NULL) && (edge_cr >= 0))
              {
                p_Vid->EdgeLoopChromaHor( imgUV[0], Strength, MbQ, MB_BLOCK_SIZE, 0, p) ;
                p_Vid->EdgeLoopChromaHor( imgUV[1], Strength, MbQ, MB_BLOCK_SIZE, 1, p) ;
              }
            }
          }
          MbQ->DeblockCall = 1;
        }
      }
    }//end edge  

    MbQ->DeblockCall = 0;
  }
}

