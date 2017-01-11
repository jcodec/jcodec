/*!
 ***********************************************************************
 * \file macroblock.c
 *
 * \brief
 *     Decode a Macroblock
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Inge Lille-Langøy               <inge.lille-langoy@telenor.com>
 *    - Rickard Sjoberg                 <rickard.sjoberg@era.ericsson.se>
 *    - Jani Lainema                    <jani.lainema@nokia.com>
 *    - Sebastian Purreiter             <sebastian.purreiter@mch.siemens.de>
 *    - Thomas Wedi                     <wedi@tnt.uni-hannover.de>
 *    - Detlev Marpe
 *    - Gabi Blaettermann
 *    - Ye-Kui Wang                     <wyk@ieee.org>
 *    - Lowell Winger                   <lwinger@lsil.com>
 *    - Alexis Michael Tourapis         <alexismt@ieee.org>
 ***********************************************************************
*/

#include "contributors.h"

#include <math.h>

#include "block.h"
#include "global.h"
#include "mbuffer.h"
#include "mbuffer_mvc.h"
#include "elements.h"
//#include "errorconcealment.h"
#include "macroblock.h"
#include "fmo.h"
#include "cabac.h"
#include "vlc.h"
#include "image.h"
#include "mb_access.h"
#include "biaridecod.h"
#include "transform.h"
#include "mc_prediction.h"
#include "quant.h"
#include "mv_prediction.h"
#include "mb_prediction.h"
#include "fast_memory.h"
#include "filehandle.h"

#if TRACE
#define TRACE_STRING(s) strncpy(currSE.tracestring, s, TRACESTRING_SIZE)
#define TRACE_DECBITS(i) dectracebitcnt(1)
#define TRACE_PRINTF(s) sprintf(type, "%s", s);
#define TRACE_STRING_P(s) strncpy(currSE->tracestring, s, TRACESTRING_SIZE)
#else
#define TRACE_STRING(s)
#define TRACE_DECBITS(i)
#define TRACE_PRINTF(s) 
#define TRACE_STRING_P(s)
#endif

extern void set_read_comp_coeff_cabac     (Macroblock *currMB);
extern void set_read_comp_coeff_cavlc     (Macroblock *currMB);

static inline void update_pixel_pos8(PixelPos *pos_block, const PixelPos *pos_mb, int pos)
{
  *pos_block = *pos_mb;
  if (pos_block->available)
  {
    if (pos == 1)
    {
      pos_block->pos_x += 2;
    }
    else if (pos == 2)
    {
      pos_block->pos_y += 2;
    }
    else if (pos == 3)
    {
      pos_block->pos_y += 2;
      pos_block->pos_x += 2;
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Read Intra 8x8 Prediction modes
 *
 ************************************************************************
 */
static void read_ipred_8x8_modes_mbaff(Macroblock *currMB)
{
  int b8, bi, bj, bx, by, dec;
  SyntaxElement currSE;
  DataPartition *dP;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  VideoParameters *p_Vid = currMB->p_Vid;

  int mostProbableIntraPredMode;
  int upIntraPredMode;
  int leftIntraPredMode;

  PixelPos left_block, top_block;

  currSE.type = SE_INTRAPREDMODE;

  TRACE_STRING("intra4x4_pred_mode");
  dP = &(currSlice->partArr[partMap[SE_INTRAPREDMODE]]);

  if (!(p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag))
    currSE.reading = readIntraPredMode_CABAC;

  for(b8 = 0; b8 < 4; ++b8)  //loop 8x8 blocks
  {
    by = (b8 & 0x02);
    bj = currMB->block_y + by;

    bx = ((b8 & 0x01) << 1);
    bi = currMB->block_x + bx;
    //get from stream
    if (p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag)
      readSyntaxElement_Intra4x4PredictionMode(&currSE, dP->bitstream);
    else
    {
      currSE.context = (b8 << 2);
      dP->readSyntaxElement(currMB, &currSE, dP);
    }

    get4x4Neighbour(currMB, (bx << 2) - 1, (by << 2),     p_Vid->mb_size[IS_LUMA], &left_block);
    get4x4Neighbour(currMB, (bx << 2),     (by << 2) - 1, p_Vid->mb_size[IS_LUMA], &top_block );

    //get from array and decode

    if (p_Vid->active_pps->constrained_intra_pred_flag)
    {
      left_block.available = left_block.available ? currSlice->intra_block[left_block.mb_addr] : 0;
      top_block.available  = top_block.available  ? currSlice->intra_block[top_block.mb_addr]  : 0;
    }

    upIntraPredMode            = (top_block.available ) ? currSlice->ipredmode[top_block.pos_y ][top_block.pos_x ] : -1;
    leftIntraPredMode          = (left_block.available) ? currSlice->ipredmode[left_block.pos_y][left_block.pos_x] : -1;

    mostProbableIntraPredMode  = (upIntraPredMode < 0 || leftIntraPredMode < 0) ? DC_PRED : upIntraPredMode < leftIntraPredMode ? upIntraPredMode : leftIntraPredMode;

    dec = (currSE.value1 == -1) ? mostProbableIntraPredMode : currSE.value1 + (currSE.value1 >= mostProbableIntraPredMode);

    //set
    //loop 4x4s in the subblock for 8x8 prediction setting
    currSlice->ipredmode[bj    ][bi    ] = (byte) dec;
    currSlice->ipredmode[bj    ][bi + 1] = (byte) dec;
    currSlice->ipredmode[bj + 1][bi    ] = (byte) dec;
    currSlice->ipredmode[bj + 1][bi + 1] = (byte) dec;             
  }
}

/*!
 ************************************************************************
 * \brief
 *    Read Intra 8x8 Prediction modes
 *
 ************************************************************************
 */
static void read_ipred_8x8_modes(Macroblock *currMB)
{
  int b8, bi, bj, bx, by, dec;
  SyntaxElement currSE;
  DataPartition *dP;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  VideoParameters *p_Vid = currMB->p_Vid;

  int mostProbableIntraPredMode;
  int upIntraPredMode;
  int leftIntraPredMode;

  PixelPos left_mb, top_mb;
  PixelPos left_block, top_block;

  currSE.type = SE_INTRAPREDMODE;

  TRACE_STRING("intra4x4_pred_mode");
  dP = &(currSlice->partArr[partMap[SE_INTRAPREDMODE]]);

  if (!(p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag))
    currSE.reading = readIntraPredMode_CABAC;

  get4x4Neighbour(currMB, -1,  0, p_Vid->mb_size[IS_LUMA], &left_mb);
  get4x4Neighbour(currMB,  0, -1, p_Vid->mb_size[IS_LUMA], &top_mb );

  for(b8 = 0; b8 < 4; ++b8)  //loop 8x8 blocks
  {


    by = (b8 & 0x02);
    bj = currMB->block_y + by;

    bx = ((b8 & 0x01) << 1);
    bi = currMB->block_x + bx;

    //get from stream
    if (p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag)
      readSyntaxElement_Intra4x4PredictionMode(&currSE, dP->bitstream);
    else
    {
      currSE.context = (b8 << 2);
      dP->readSyntaxElement(currMB, &currSE, dP);
    }

    get4x4Neighbour(currMB, (bx<<2) - 1, (by<<2),     p_Vid->mb_size[IS_LUMA], &left_block);
    get4x4Neighbour(currMB, (bx<<2),     (by<<2) - 1, p_Vid->mb_size[IS_LUMA], &top_block );
    
    //get from array and decode

    if (p_Vid->active_pps->constrained_intra_pred_flag)
    {
      left_block.available = left_block.available ? currSlice->intra_block[left_block.mb_addr] : 0;
      top_block.available  = top_block.available  ? currSlice->intra_block[top_block.mb_addr]  : 0;
    }

    upIntraPredMode            = (top_block.available ) ? currSlice->ipredmode[top_block.pos_y ][top_block.pos_x ] : -1;
    leftIntraPredMode          = (left_block.available) ? currSlice->ipredmode[left_block.pos_y][left_block.pos_x] : -1;

    mostProbableIntraPredMode  = (upIntraPredMode < 0 || leftIntraPredMode < 0) ? DC_PRED : upIntraPredMode < leftIntraPredMode ? upIntraPredMode : leftIntraPredMode;

    dec = (currSE.value1 == -1) ? mostProbableIntraPredMode : currSE.value1 + (currSE.value1 >= mostProbableIntraPredMode);

    //set
    //loop 4x4s in the subblock for 8x8 prediction setting
    currSlice->ipredmode[bj    ][bi    ] = (byte) dec;
    currSlice->ipredmode[bj    ][bi + 1] = (byte) dec;
    currSlice->ipredmode[bj + 1][bi    ] = (byte) dec;
    currSlice->ipredmode[bj + 1][bi + 1] = (byte) dec;             
  }
}

/*!
 ************************************************************************
 * \brief
 *    Read Intra 4x4 Prediction modes
 *
 ************************************************************************
 */
static void read_ipred_4x4_modes_mbaff(Macroblock *currMB)
{
  int b8,i,j,bi,bj,bx,by;
  SyntaxElement currSE;
  DataPartition *dP;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  VideoParameters *p_Vid = currMB->p_Vid;
  BlockPos *PicPos = p_Vid->PicPos;

  int ts, ls;
  int mostProbableIntraPredMode;
  int upIntraPredMode;
  int leftIntraPredMode;

  PixelPos left_block, top_block;

  currSE.type = SE_INTRAPREDMODE;

  TRACE_STRING("intra4x4_pred_mode");
  dP = &(currSlice->partArr[partMap[SE_INTRAPREDMODE]]);

  if (!(p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag))
    currSE.reading = readIntraPredMode_CABAC;

  for(b8 = 0; b8 < 4; ++b8)  //loop 8x8 blocks
  {           
    for(j = 0; j < 2; j++)  //loop subblocks
    {
      by = (b8 & 0x02) + j;
      bj = currMB->block_y + by;

      for(i = 0; i < 2; i++)
      {
        bx = ((b8 & 1) << 1) + i;
        bi = currMB->block_x + bx;
        //get from stream
        if (p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag)
          readSyntaxElement_Intra4x4PredictionMode(&currSE, dP->bitstream);
        else
        {
          currSE.context=(b8<<2) + (j<<1) +i;
          dP->readSyntaxElement(currMB, &currSE, dP);
        }

        get4x4Neighbour(currMB, (bx<<2) - 1, (by<<2),     p_Vid->mb_size[IS_LUMA], &left_block);
        get4x4Neighbour(currMB, (bx<<2),     (by<<2) - 1, p_Vid->mb_size[IS_LUMA], &top_block );

        //get from array and decode

        if (p_Vid->active_pps->constrained_intra_pred_flag)
        {
          left_block.available = left_block.available ? currSlice->intra_block[left_block.mb_addr] : 0;
          top_block.available  = top_block.available  ? currSlice->intra_block[top_block.mb_addr]  : 0;
        }

        // !! KS: not sure if the following is still correct...
        ts = ls = 0;   // Check to see if the neighboring block is SI
        if (currSlice->slice_type == SI_SLICE)           // need support for MBINTLC1
        {
          if (left_block.available)
            if (currSlice->siblock [PicPos[left_block.mb_addr].y][PicPos[left_block.mb_addr].x])
              ls=1;

          if (top_block.available)
            if (currSlice->siblock [PicPos[top_block.mb_addr].y][PicPos[top_block.mb_addr].x])
              ts=1;
        }

        upIntraPredMode            = (top_block.available  &&(ts == 0)) ? currSlice->ipredmode[top_block.pos_y ][top_block.pos_x ] : -1;
        leftIntraPredMode          = (left_block.available &&(ls == 0)) ? currSlice->ipredmode[left_block.pos_y][left_block.pos_x] : -1;

        mostProbableIntraPredMode  = (upIntraPredMode < 0 || leftIntraPredMode < 0) ? DC_PRED : upIntraPredMode < leftIntraPredMode ? upIntraPredMode : leftIntraPredMode;

        currSlice->ipredmode[bj][bi] = (byte) ((currSE.value1 == -1) ? mostProbableIntraPredMode : currSE.value1 + (currSE.value1 >= mostProbableIntraPredMode));
      }
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    Read Intra 4x4 Prediction modes
 *
 ************************************************************************
 */
static void read_ipred_4x4_modes(Macroblock *currMB)
{
  int b8,i,j,bi,bj,bx,by;
  SyntaxElement currSE;
  DataPartition *dP;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  VideoParameters *p_Vid = currMB->p_Vid;
  BlockPos *PicPos = p_Vid->PicPos;

  int ts, ls;
  int mostProbableIntraPredMode;
  int upIntraPredMode;
  int leftIntraPredMode;

  PixelPos left_mb, top_mb;
  PixelPos left_block, top_block;

  currSE.type = SE_INTRAPREDMODE;

  TRACE_STRING("intra4x4_pred_mode");
  dP = &(currSlice->partArr[partMap[SE_INTRAPREDMODE]]);

  if (!(p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag))
    currSE.reading = readIntraPredMode_CABAC;

  get4x4Neighbour(currMB, -1,  0, p_Vid->mb_size[IS_LUMA], &left_mb);
  get4x4Neighbour(currMB,  0, -1, p_Vid->mb_size[IS_LUMA], &top_mb );

  for(b8 = 0; b8 < 4; ++b8)  //loop 8x8 blocks
  {       
    for(j = 0; j < 2; j++)  //loop subblocks
    {
      by = (b8 & 0x02) + j;
      bj = currMB->block_y + by;

      for(i = 0; i < 2; i++)
      {
        bx = ((b8 & 1) << 1) + i;
        bi = currMB->block_x + bx;
        //get from stream
        if (p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag)
          readSyntaxElement_Intra4x4PredictionMode(&currSE, dP->bitstream);
        else
        {
          currSE.context=(b8<<2) + (j<<1) +i;
          dP->readSyntaxElement(currMB, &currSE, dP);
        }

        get4x4Neighbour(currMB, (bx<<2) - 1, (by<<2),     p_Vid->mb_size[IS_LUMA], &left_block);
        get4x4Neighbour(currMB, (bx<<2),     (by<<2) - 1, p_Vid->mb_size[IS_LUMA], &top_block );

        //get from array and decode

        if (p_Vid->active_pps->constrained_intra_pred_flag)
        {
          left_block.available = left_block.available ? currSlice->intra_block[left_block.mb_addr] : 0;
          top_block.available  = top_block.available  ? currSlice->intra_block[top_block.mb_addr]  : 0;
        }

        // !! KS: not sure if the following is still correct...
        ts = ls = 0;   // Check to see if the neighboring block is SI
        if (currSlice->slice_type == SI_SLICE)           // need support for MBINTLC1
        {
          if (left_block.available)
            if (currSlice->siblock [PicPos[left_block.mb_addr].y][PicPos[left_block.mb_addr].x])
              ls=1;

          if (top_block.available)
            if (currSlice->siblock [PicPos[top_block.mb_addr].y][PicPos[top_block.mb_addr].x])
              ts=1;
        }

        upIntraPredMode            = (top_block.available  &&(ts == 0)) ? currSlice->ipredmode[top_block.pos_y ][top_block.pos_x ] : -1;
        leftIntraPredMode          = (left_block.available &&(ls == 0)) ? currSlice->ipredmode[left_block.pos_y][left_block.pos_x] : -1;

        mostProbableIntraPredMode  = (upIntraPredMode < 0 || leftIntraPredMode < 0) ? DC_PRED : upIntraPredMode < leftIntraPredMode ? upIntraPredMode : leftIntraPredMode;

        currSlice->ipredmode[bj][bi] = (byte) ((currSE.value1 == -1) ? mostProbableIntraPredMode : currSE.value1 + (currSE.value1 >= mostProbableIntraPredMode));
      }
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    Read Intra Prediction modes
 *
 ************************************************************************
 */
static void read_ipred_modes(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  StorablePicture *dec_picture = currSlice->dec_picture;

  if (currSlice->mb_aff_frame_flag)
  {
    if (currMB->mb_type == I8MB)
      read_ipred_8x8_modes_mbaff(currMB);
    else if (currMB->mb_type == I4MB)
      read_ipred_4x4_modes_mbaff(currMB);
  }
  else
  {
  if (currMB->mb_type == I8MB)
    read_ipred_8x8_modes(currMB);
  else if (currMB->mb_type == I4MB)
    read_ipred_4x4_modes(currMB);
  }

  if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444))
  {
    SyntaxElement currSE;
    DataPartition *dP;
    const byte *partMap = assignSE2partition[currSlice->dp_mode];
    VideoParameters *p_Vid = currMB->p_Vid;

    currSE.type = SE_INTRAPREDMODE;
    TRACE_STRING("intra_chroma_pred_mode");
    dP = &(currSlice->partArr[partMap[SE_INTRAPREDMODE]]);

    if (p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC || dP->bitstream->ei_flag) 
      currSE.mapping = linfo_ue;
    else
      currSE.reading = readCIPredMode_CABAC;

    dP->readSyntaxElement(currMB, &currSE, dP);
    currMB->c_ipred_mode = (char) currSE.value1;

    if (currMB->c_ipred_mode < DC_PRED_8 || currMB->c_ipred_mode > PLANE_8)
    {
      error("illegal chroma intra pred mode!\n", 600);
    }
  }
}


static inline void reset_mbs(Macroblock *currMB)
{
  currMB->slice_nr = -1; 
  currMB->ei_flag  =  1;
  currMB->dpl_flag =  0;
}

static inline void reset_mv_info(PicMotionParams *mv_info, int slice_no)
{
  mv_info->ref_pic[LIST_0] = NULL;
  mv_info->ref_pic[LIST_1] = NULL;
  mv_info->mv[LIST_0] = zero_mv;
  mv_info->mv[LIST_1] = zero_mv;
  mv_info->ref_idx[LIST_0] = -1;
  mv_info->ref_idx[LIST_1] = -1;
  mv_info->slice_no = slice_no;
}

static inline void reset_mv_info_list(PicMotionParams *mv_info, int list, int slice_no)
{
  mv_info->ref_pic[list] = NULL;
  mv_info->mv[list] = zero_mv;
  mv_info->ref_idx[list] = -1;
  mv_info->slice_no = slice_no;
}

/*!
 ************************************************************************
 * \brief
 *    init macroblock for skip mode. Only L1 info needs to be reset
 ************************************************************************
 */
static void init_macroblock_basic(Macroblock *currMB)
{
  int j, i;
  PicMotionParams **mv_info = &currMB->p_Slice->dec_picture->mv_info[currMB->block_y]; //&p_Vid->dec_picture->mv_info[currMB->block_y];
  int slice_no =  currMB->p_Slice->current_slice_nr;
  // reset vectors and pred. modes
  for(j = 0; j < BLOCK_SIZE; ++j)
  {                        
    i = currMB->block_x;
    reset_mv_info_list(*mv_info + (i++), LIST_1, slice_no);
    reset_mv_info_list(*mv_info + (i++), LIST_1, slice_no);
    reset_mv_info_list(*mv_info + (i++), LIST_1, slice_no);
    reset_mv_info_list(*(mv_info++) + i, LIST_1, slice_no);
  }
}

/*!
 ************************************************************************
 * \brief
 *    init macroblock (direct)
 ************************************************************************
 */
static void init_macroblock_direct(Macroblock *currMB)
{
  int slice_no = currMB->p_Slice->current_slice_nr;
  PicMotionParams **mv_info = &currMB->p_Slice->dec_picture->mv_info[currMB->block_y]; 
  int i, j;

  set_read_comp_coeff_cabac(currMB);
  set_read_comp_coeff_cavlc(currMB);
  i = currMB->block_x;
  for(j = 0; j < BLOCK_SIZE; ++j)
  {                        
    (*mv_info+i)->slice_no = slice_no;
    (*mv_info+i+1)->slice_no = slice_no;
    (*mv_info+i+2)->slice_no = slice_no;
    (*(mv_info++)+i+3)->slice_no = slice_no;
  }
}


/*!
 ************************************************************************
 * \brief
 *    init macroblock
 ************************************************************************
 */
static void init_macroblock(Macroblock *currMB)
{
  int j, i;
  Slice *currSlice = currMB->p_Slice;
  PicMotionParams **mv_info = &currSlice->dec_picture->mv_info[currMB->block_y]; 
  int slice_no = currSlice->current_slice_nr;
  // reset vectors and pred. modes

  for(j = 0; j < BLOCK_SIZE; ++j)
  {                        
    i = currMB->block_x;
    reset_mv_info(*mv_info + (i++), slice_no);
    reset_mv_info(*mv_info + (i++), slice_no);
    reset_mv_info(*mv_info + (i++), slice_no);
    reset_mv_info(*(mv_info++) + i, slice_no);
  }

  set_read_comp_coeff_cabac(currMB);
  set_read_comp_coeff_cavlc(currMB);
}

static void concealIPCMcoeffs(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  StorablePicture *dec_picture = currSlice->dec_picture;
  int i, j, k;

  for(i=0;i<MB_BLOCK_SIZE;++i)
  {
    for(j=0;j<MB_BLOCK_SIZE;++j)
    {
      currSlice->cof[0][i][j] = p_Vid->dc_pred_value_comp[0];
      //currSlice->fcf[0][i][j] = p_Vid->dc_pred_value_comp[0];
    }
  }

  if ((dec_picture->chroma_format_idc != YUV400) && (p_Vid->separate_colour_plane_flag == 0))
  {
    for (k = 0; k < 2; ++k)
    {
      for(i=0;i<p_Vid->mb_cr_size_y;++i)
      {
        for(j=0;j<p_Vid->mb_cr_size_x;++j)
        {
          currSlice->cof[k][i][j] = p_Vid->dc_pred_value_comp[k];
          //currSlice->fcf[k][i][j] = p_Vid->dc_pred_value_comp[k];
        }
      }
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Initialize decoding engine after decoding an IPCM macroblock
 *    (for IPCM CABAC  28/11/2003)
 *
 * \author
 *    Dong Wang <Dong.Wang@bristol.ac.uk>
 ************************************************************************
 */
static void init_decoding_engine_IPCM(Slice *currSlice)
{   
  Bitstream *currStream;
  int ByteStartPosition;
  int PartitionNumber;
  int i;

  if(currSlice->dp_mode==PAR_DP_1)
    PartitionNumber=1;
  else if(currSlice->dp_mode==PAR_DP_3)
    PartitionNumber=3;
  else
  {
    printf("Partition Mode is not supported\n");
    exit(1);
  }

  for(i=0;i<PartitionNumber;++i)
  {
    currStream = currSlice->partArr[i].bitstream;
    ByteStartPosition = currStream->read_len;

    arideco_start_decoding (&currSlice->partArr[i].de_cabac, currStream->streamBuffer, ByteStartPosition, &currStream->read_len);
  }
}

/*!
 ************************************************************************
 * \brief
 *    Read IPCM pcm_alignment_zero_bit and pcm_byte[i] from stream to currSlice->cof
 *    (for IPCM CABAC and IPCM CAVLC)
 *
 * \author
 *    Dong Wang <Dong.Wang@bristol.ac.uk>
 ************************************************************************
 */
static void read_IPCM_coeffs_from_NAL(Slice *currSlice, struct datapartition_dec *dP)
{
  VideoParameters *p_Vid = currSlice->p_Vid;

  StorablePicture *dec_picture = currSlice->dec_picture;
  SyntaxElement currSE;
  int i,j;

  //For CABAC, we don't need to read bits to let stream byte aligned
  //  because we have variable for integer bytes position
  if(p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CABAC)
  {
    readIPCM_CABAC(currSlice, dP);
    init_decoding_engine_IPCM(currSlice);
  }
  else
  {
    //read bits to let stream byte aligned

    if(((dP->bitstream->frame_bitoffset) & 0x07) != 0)
    {
      TRACE_STRING("pcm_alignment_zero_bit");
      currSE.len = (8 - ((dP->bitstream->frame_bitoffset) & 0x07));
      readSyntaxElement_FLC(&currSE, dP->bitstream);
    }

    //read luma and chroma IPCM coefficients
    currSE.len=p_Vid->bitdepth_luma;
    TRACE_STRING("pcm_sample_luma");

    for(i=0;i<MB_BLOCK_SIZE;++i)
    {
      for(j=0;j<MB_BLOCK_SIZE;++j)
      {
        readSyntaxElement_FLC(&currSE, dP->bitstream);
        currSlice->cof[0][i][j] = currSE.value1;
        //currSlice->fcf[0][i][j] = currSE.value1;
      }
    }
    currSE.len=p_Vid->bitdepth_chroma;
    if ((dec_picture->chroma_format_idc != YUV400) && (p_Vid->separate_colour_plane_flag == 0))
    {
      TRACE_STRING("pcm_sample_chroma (u)");
      for(i=0;i<p_Vid->mb_cr_size_y;++i)
      {
        for(j=0;j<p_Vid->mb_cr_size_x;++j)
        {
          readSyntaxElement_FLC(&currSE, dP->bitstream);
          currSlice->cof[1][i][j] = currSE.value1;
          //currSlice->fcf[1][i][j] = currSE.value1;
        }
      }
      TRACE_STRING("pcm_sample_chroma (v)");
      for(i=0;i<p_Vid->mb_cr_size_y;++i)
      {
        for(j=0;j<p_Vid->mb_cr_size_x;++j)
        {
          readSyntaxElement_FLC(&currSE, dP->bitstream);
          currSlice->cof[2][i][j] = currSE.value1;
          //currSlice->fcf[2][i][j] = currSE.value1;
        }
      }
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Sets mode for 8x8 block
 ************************************************************************
 */
static inline void SetB8Mode (Macroblock* currMB, int value, int i)
{
  Slice* currSlice = currMB->p_Slice;
  static const char p_v2b8 [ 5] = {4, 5, 6, 7, IBLOCK};
  static const char p_v2pd [ 5] = {0, 0, 0, 0, -1};
  static const char b_v2b8 [14] = {0, 4, 4, 4, 5, 6, 5, 6, 5, 6, 7, 7, 7, IBLOCK};
  static const char b_v2pd [14] = {2, 0, 1, 2, 0, 0, 1, 1, 2, 2, 0, 1, 2, -1};

  if (currSlice->slice_type==B_SLICE)
  {
    currMB->b8mode[i] = b_v2b8[value];
    currMB->b8pdir[i] = b_v2pd[value];
  }
  else
  {
    currMB->b8mode[i] = p_v2b8[value];
    currMB->b8pdir[i] = p_v2pd[value];
  }
}

static inline void reset_coeffs(Macroblock *currMB)
{
  VideoParameters *p_Vid = currMB->p_Vid;

  // CAVLC
  if (p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CAVLC)
    fast_memset(p_Vid->nz_coeff[currMB->mbAddrX][0][0], 0, 3 * BLOCK_PIXELS * sizeof(byte));
}

static inline void field_flag_inference(Macroblock *currMB)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  if (currMB->mbAvailA)
  {
    currMB->mb_field = p_Vid->mb_data[currMB->mbAddrA].mb_field;
  }
  else
  {
    // check top macroblock pair
    currMB->mb_field = currMB->mbAvailB ? p_Vid->mb_data[currMB->mbAddrB].mb_field : FALSE;
  }
}


void skip_macroblock(Macroblock *currMB)
{
  MotionVector pred_mv;
  int zeroMotionAbove;
  int zeroMotionLeft;
  PixelPos mb[4];    // neighbor blocks
  int   i, j;
  int   a_mv_y = 0;
  int   a_ref_idx = 0;
  int   b_mv_y = 0;
  int   b_ref_idx = 0;
  int   img_block_y   = currMB->block_y;
  VideoParameters *p_Vid = currMB->p_Vid;
  Slice *currSlice = currMB->p_Slice;
  int   list_offset = LIST_0 + currMB->list_offset;
  StorablePicture *dec_picture = currSlice->dec_picture;
  MotionVector *a_mv = NULL;
  MotionVector *b_mv = NULL;

  get_neighbors(currMB, mb, 0, 0, MB_BLOCK_SIZE);
  if (currSlice->mb_aff_frame_flag == 0)
  {
    if (mb[0].available)
    {
      a_mv      = &dec_picture->mv_info[mb[0].pos_y][mb[0].pos_x].mv[LIST_0];
      a_mv_y    = a_mv->mv_y;    
      a_ref_idx = dec_picture->mv_info[mb[0].pos_y][mb[0].pos_x].ref_idx[LIST_0];
    }

    if (mb[1].available)
    {
      b_mv      = &dec_picture->mv_info[mb[1].pos_y][mb[1].pos_x].mv[LIST_0];
      b_mv_y    = b_mv->mv_y;
      b_ref_idx = dec_picture->mv_info[mb[1].pos_y][mb[1].pos_x].ref_idx[LIST_0];
    }
  }
  else
  {
    if (mb[0].available)
    {
      a_mv      = &dec_picture->mv_info[mb[0].pos_y][mb[0].pos_x].mv[LIST_0];
      a_mv_y    = a_mv->mv_y;    
      a_ref_idx = dec_picture->mv_info[mb[0].pos_y][mb[0].pos_x].ref_idx[LIST_0];

      if (currMB->mb_field && !p_Vid->mb_data[mb[0].mb_addr].mb_field)
      {
        a_mv_y    /=2;
        a_ref_idx *=2;
      }
      if (!currMB->mb_field && p_Vid->mb_data[mb[0].mb_addr].mb_field)
      {
        a_mv_y    *=2;
        a_ref_idx >>=1;
      }
    }

    if (mb[1].available)
    {
      b_mv      = &dec_picture->mv_info[mb[1].pos_y][mb[1].pos_x].mv[LIST_0];
      b_mv_y    = b_mv->mv_y;
      b_ref_idx = dec_picture->mv_info[mb[1].pos_y][mb[1].pos_x].ref_idx[LIST_0];

      if (currMB->mb_field && !p_Vid->mb_data[mb[1].mb_addr].mb_field)
      {
        b_mv_y    /=2;
        b_ref_idx *=2;
      }
      if (!currMB->mb_field && p_Vid->mb_data[mb[1].mb_addr].mb_field)
      {
        b_mv_y    *=2;
        b_ref_idx >>=1;
      }
    }
  }

  zeroMotionLeft  = !mb[0].available ? 1 : a_ref_idx==0 && a_mv->mv_x == 0 && a_mv_y==0 ? 1 : 0;
  zeroMotionAbove = !mb[1].available ? 1 : b_ref_idx==0 && b_mv->mv_x == 0 && b_mv_y==0 ? 1 : 0;

  currMB->cbp = 0;
  reset_coeffs(currMB);

  if (zeroMotionAbove || zeroMotionLeft)
  {
    PicMotionParams **dec_mv_info = &dec_picture->mv_info[img_block_y];
    StorablePicture *cur_pic = currSlice->listX[list_offset][0];
    PicMotionParams *mv_info = NULL;
    
    for(j = 0; j < BLOCK_SIZE; ++j)
    {
      for(i = currMB->block_x; i < currMB->block_x + BLOCK_SIZE; ++i)
      {
        mv_info = &dec_mv_info[j][i];
        mv_info->ref_pic[LIST_0] = cur_pic;
        mv_info->mv     [LIST_0] = zero_mv;
        mv_info->ref_idx[LIST_0] = 0;
      }
    }
  }
  else
  {
    PicMotionParams **dec_mv_info = &dec_picture->mv_info[img_block_y];
    PicMotionParams *mv_info = NULL;
    StorablePicture *cur_pic = currSlice->listX[list_offset][0];
    currMB->GetMVPredictor (currMB, mb, &pred_mv, 0, dec_picture->mv_info, LIST_0, 0, 0, MB_BLOCK_SIZE, MB_BLOCK_SIZE);

    // Set first block line (position img_block_y)
    for(j = 0; j < BLOCK_SIZE; ++j)
    {
      for(i = currMB->block_x; i < currMB->block_x + BLOCK_SIZE; ++i)
      {
        mv_info = &dec_mv_info[j][i];
        mv_info->ref_pic[LIST_0] = cur_pic;
        mv_info->mv     [LIST_0] = pred_mv;
        mv_info->ref_idx[LIST_0] = 0;
      }
    }
  }
}
/*!
 ************************************************************************
 * \brief
 *   read and set skip macroblock information
 ************************************************************************
 */
static void read_skip_macroblock(Macroblock *currMB)
{
  currMB->luma_transform_size_8x8_flag = FALSE;

  if(currMB->p_Vid->active_pps->constrained_intra_pred_flag)
  {
    int mb_nr = currMB->mbAddrX; 
    currMB->p_Slice->intra_block[mb_nr] = 0;
  }

  //--- init macroblock data ---
  init_macroblock_basic(currMB);

  skip_macroblock(currMB);
}

/*!
 ************************************************************************
 * \brief
 *   read and set intra (other than 4x4/8x8) mode macroblock information
 ************************************************************************
 */
static void read_intra_macroblock(Macroblock *currMB)
{
  //init NoMbPartLessThan8x8Flag
  currMB->NoMbPartLessThan8x8Flag = TRUE;

  //============= Transform Size Flag for INTRA MBs =============
  //-------------------------------------------------------------
  //transform size flag for INTRA_4x4 and INTRA_8x8 modes
  currMB->luma_transform_size_8x8_flag = FALSE;

  //--- init macroblock data ---
  init_macroblock(currMB);

  // intra prediction modes for a macroblock 4x4 **********************************************
  read_ipred_modes(currMB);

  // read CBP and Coeffs  ***************************************************************
  currMB->p_Slice->read_CBP_and_coeffs_from_NAL (currMB);
}


/*!
 ************************************************************************
 * \brief
 *   read and set intra (4x4/8x8) mode macroblock information (CAVLC)
 ************************************************************************
 */
static void read_intra4x4_macroblock_cavlc(Macroblock *currMB, const byte *partMap)
{
  Slice *currSlice = currMB->p_Slice;
  //============= Transform Size Flag for INTRA MBs =============
  //-------------------------------------------------------------
  //transform size flag for INTRA_4x4 and INTRA_8x8 modes
  if (currSlice->Transform8x8Mode)
  {    
    SyntaxElement currSE;
    DataPartition *dP = &(currSlice->partArr[partMap[SE_HEADER]]);
    currSE.type   =  SE_HEADER;
    TRACE_STRING("transform_size_8x8_flag");

    // read CAVLC transform_size_8x8_flag
    currSE.len = (int64) 1;
    readSyntaxElement_FLC(&currSE, dP->bitstream);

    currMB->luma_transform_size_8x8_flag = (Boolean) currSE.value1;

    if (currMB->luma_transform_size_8x8_flag)
    {      
      currMB->mb_type = I8MB;
      memset(&currMB->b8mode, I8MB, 4 * sizeof(char));
      memset(&currMB->b8pdir, -1, 4 * sizeof(char));
    }
  }
  else
  {
    currMB->luma_transform_size_8x8_flag = FALSE;
  }

  //--- init macroblock data ---
  init_macroblock(currMB);

  // intra prediction modes for a macroblock 4x4 **********************************************
  read_ipred_modes(currMB);

  // read CBP and Coeffs  ***************************************************************
  currSlice->read_CBP_and_coeffs_from_NAL (currMB);
}

/*!
 ************************************************************************
 * \brief
 *   read and set intra (4x4/8x8) mode macroblock information (CAVLC)
 ************************************************************************
 */
static void read_intra4x4_macroblock_cabac(Macroblock *currMB, const byte *partMap)
{
  Slice *currSlice = currMB->p_Slice;
  //============= Transform Size Flag for INTRA MBs =============
  //-------------------------------------------------------------
  //transform size flag for INTRA_4x4 and INTRA_8x8 modes
  if (currSlice->Transform8x8Mode)
  {
   SyntaxElement currSE;
    DataPartition *dP = &(currSlice->partArr[partMap[SE_HEADER]]); 
    currSE.type   =  SE_HEADER;
    currSE.reading = readMB_transform_size_flag_CABAC;
    TRACE_STRING("transform_size_8x8_flag");

    // read CAVLC transform_size_8x8_flag
    if (dP->bitstream->ei_flag)
    {
      currSE.len = (int64) 1;
      readSyntaxElement_FLC(&currSE, dP->bitstream);
    }
    else
    {
      dP->readSyntaxElement(currMB, &currSE, dP);
    }

    currMB->luma_transform_size_8x8_flag = (Boolean) currSE.value1;

    if (currMB->luma_transform_size_8x8_flag)
    {      
      currMB->mb_type = I8MB;
      memset(&currMB->b8mode, I8MB, 4 * sizeof(char));
      memset(&currMB->b8pdir, -1, 4 * sizeof(char));
    }
  }
  else
  {
    currMB->luma_transform_size_8x8_flag = FALSE;
  }

  //--- init macroblock data ---
  init_macroblock(currMB);

  // intra prediction modes for a macroblock 4x4 **********************************************
  read_ipred_modes(currMB);

  // read CBP and Coeffs  ***************************************************************
  currSlice->read_CBP_and_coeffs_from_NAL (currMB);
}


/*!
 ************************************************************************
 * \brief
 *   read and set generic (non skip/direct and P8x8) inter 
 *   mode macroblock information
 ************************************************************************
 */
static void read_inter_macroblock(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  //init NoMbPartLessThan8x8Flag
  currMB->NoMbPartLessThan8x8Flag = TRUE;
  currMB->luma_transform_size_8x8_flag = FALSE;

  if(currMB->p_Vid->active_pps->constrained_intra_pred_flag)
  {
    int mb_nr = currMB->mbAddrX;
    currSlice->intra_block[mb_nr] = 0;
  }

  //--- init macroblock data ---
  init_macroblock(currMB);

  // read inter frame vector data *********************************************************
  currSlice->read_motion_info_from_NAL (currMB);
  // read CBP and Coeffs  ***************************************************************
  currSlice->read_CBP_and_coeffs_from_NAL (currMB);
}

/*!
 ************************************************************************
 * \brief
 *   read and set I_PCM mode macroblock information
 ************************************************************************
 */
static void read_i_pcm_macroblock(Macroblock *currMB, const byte *partMap)
{
  Slice *currSlice = currMB->p_Slice;
  currMB->NoMbPartLessThan8x8Flag = TRUE;
  currMB->luma_transform_size_8x8_flag = FALSE;

  //--- init macroblock data ---
  init_macroblock(currMB);

  //read pcm_alignment_zero_bit and pcm_byte[i]

  // here dP is assigned with the same dP as SE_MBTYPE, because IPCM syntax is in the
  // same category as MBTYPE
  if ( currSlice->dp_mode && currSlice->dpB_NotPresent )
  {
    concealIPCMcoeffs(currMB);
  }
  else
  {
    DataPartition *dP = &(currSlice->partArr[partMap[SE_LUM_DC_INTRA]]);
    read_IPCM_coeffs_from_NAL(currSlice, dP);
  }
}

/*!
 ************************************************************************
 * \brief
 *   read and set P8x8 mode macroblock information
 ************************************************************************
 */
static void read_P8x8_macroblock(Macroblock *currMB, DataPartition *dP, SyntaxElement *currSE)
{
  int i;
  Slice *currSlice = currMB->p_Slice;
  //====== READ 8x8 SUB-PARTITION MODES (modes of 8x8 blocks) and Intra VBST block modes ======
  currMB->NoMbPartLessThan8x8Flag = TRUE;
  currMB->luma_transform_size_8x8_flag = FALSE;

  for (i = 0; i < 4; ++i)
  {
    TRACE_STRING_P("sub_mb_type");
    dP->readSyntaxElement (currMB, currSE, dP);
    SetB8Mode (currMB, currSE->value1, i);

    //set NoMbPartLessThan8x8Flag for P8x8 mode
    currMB->NoMbPartLessThan8x8Flag &= (currMB->b8mode[i] == 0 && currSlice->active_sps->direct_8x8_inference_flag) ||
      (currMB->b8mode[i] == 4);
  }
  
  //--- init macroblock data ---
  init_macroblock (currMB);
  currSlice->read_motion_info_from_NAL (currMB);  

  if(currMB->p_Vid->active_pps->constrained_intra_pred_flag)
  {
    int mb_nr = currMB->mbAddrX;
    currSlice->intra_block[mb_nr] = 0;
  }

  // read CBP and Coeffs  ***************************************************************
  currSlice->read_CBP_and_coeffs_from_NAL (currMB);
}

/*!
 ************************************************************************
 * \brief
 *    Get the syntax elements from the NAL
 ************************************************************************
 */
static void read_one_macroblock_i_slice_cavlc(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;

  SyntaxElement currSE;
  int mb_nr = currMB->mbAddrX; 

  DataPartition *dP;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  StorablePicture *dec_picture = currSlice->dec_picture; 
  PicMotionParamsOld *motion = &dec_picture->motion;

  currMB->mb_field = ((mb_nr&0x01) == 0)? FALSE : currSlice->mb_data[mb_nr-1].mb_field; 

  update_qp(currMB, currSlice->qp);
  currSE.type = SE_MBTYPE;

  //  read MB mode *****************************************************************
  dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);
  
  currSE.mapping = linfo_ue;

  // read MB aff
  if (currSlice->mb_aff_frame_flag && (mb_nr&0x01)==0)
  {
    TRACE_STRING("mb_field_decoding_flag");
    currSE.len = (int64) 1;
    readSyntaxElement_FLC(&currSE, dP->bitstream);
    currMB->mb_field = (Boolean) currSE.value1;
  }

  //  read MB type
  TRACE_STRING("mb_type");
  dP->readSyntaxElement(currMB, &currSE, dP);

  currMB->mb_type = (short) currSE.value1;
  if(!dP->bitstream->ei_flag)
    currMB->ei_flag = 0;

  motion->mb_field[mb_nr] = (byte) currMB->mb_field;

  currMB->block_y_aff = ((currSlice->mb_aff_frame_flag) && (currMB->mb_field)) ? (mb_nr&0x01) ? (currMB->block_y - 4)>>1 : currMB->block_y >> 1 : currMB->block_y;

  currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;

  currSlice->interpret_mb_mode(currMB);

  //init NoMbPartLessThan8x8Flag
  currMB->NoMbPartLessThan8x8Flag = TRUE;

  if(currMB->mb_type == IPCM)
  {
    read_i_pcm_macroblock(currMB, partMap);
  }
  else if (currMB->mb_type == I4MB)
  {
    read_intra4x4_macroblock_cavlc(currMB, partMap);
  }
  else // all other modes
  {
    read_intra_macroblock(currMB);
  }
  return;
}

/*!
 ************************************************************************
 * \brief
 *    Get the syntax elements from the NAL
 ************************************************************************
 */
static void read_one_macroblock_i_slice_cabac(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;

  SyntaxElement currSE;
  int mb_nr = currMB->mbAddrX; 

  DataPartition *dP;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  StorablePicture *dec_picture = currSlice->dec_picture; 
  PicMotionParamsOld *motion = &dec_picture->motion;

  currMB->mb_field = ((mb_nr&0x01) == 0)? FALSE : currSlice->mb_data[mb_nr-1].mb_field; 

  update_qp(currMB, currSlice->qp);
  currSE.type = SE_MBTYPE;

  //  read MB mode *****************************************************************
  dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

  if (dP->bitstream->ei_flag)   
    currSE.mapping = linfo_ue;

  // read MB aff
  if (currSlice->mb_aff_frame_flag && (mb_nr&0x01)==0)
  {
    TRACE_STRING("mb_field_decoding_flag");
    if (dP->bitstream->ei_flag)
    {
      currSE.len = (int64) 1;
      readSyntaxElement_FLC(&currSE, dP->bitstream);
    }
    else
    {
      currSE.reading = readFieldModeInfo_CABAC;
      dP->readSyntaxElement(currMB, &currSE, dP);
    }
    currMB->mb_field = (Boolean) currSE.value1;
  }

  CheckAvailabilityOfNeighborsCABAC(currMB);

  //  read MB type
  TRACE_STRING("mb_type");
  currSE.reading = readMB_typeInfo_CABAC_i_slice;
  dP->readSyntaxElement(currMB, &currSE, dP);

  currMB->mb_type = (short) currSE.value1;
  if(!dP->bitstream->ei_flag)
    currMB->ei_flag = 0;

  motion->mb_field[mb_nr] = (byte) currMB->mb_field;

  currMB->block_y_aff = ((currSlice->mb_aff_frame_flag) && (currMB->mb_field)) ? (mb_nr&0x01) ? (currMB->block_y - 4)>>1 : currMB->block_y >> 1 : currMB->block_y;

  currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;

  currSlice->interpret_mb_mode(currMB);

  //init NoMbPartLessThan8x8Flag
  currMB->NoMbPartLessThan8x8Flag = TRUE;

  if(currMB->mb_type == IPCM)
  {
    read_i_pcm_macroblock(currMB, partMap);
  }
  else if (currMB->mb_type == I4MB)
  {
    //============= Transform Size Flag for INTRA MBs =============
    //-------------------------------------------------------------
    //transform size flag for INTRA_4x4 and INTRA_8x8 modes
    if (currSlice->Transform8x8Mode)
    {
      currSE.type   =  SE_HEADER;
      dP = &(currSlice->partArr[partMap[SE_HEADER]]);
      currSE.reading = readMB_transform_size_flag_CABAC;
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      if (dP->bitstream->ei_flag)
      {
        currSE.len = (int64) 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
      }
      else
      {
        dP->readSyntaxElement(currMB, &currSE, dP);
      }

      currMB->luma_transform_size_8x8_flag = (Boolean) currSE.value1;

      if (currMB->luma_transform_size_8x8_flag)
      {      
        currMB->mb_type = I8MB;
        memset(&currMB->b8mode, I8MB, 4 * sizeof(char));
        memset(&currMB->b8pdir, -1, 4 * sizeof(char));
      }
    }
    else
    {
      currMB->luma_transform_size_8x8_flag = FALSE;
    }

    //--- init macroblock data ---
    init_macroblock(currMB);

    // intra prediction modes for a macroblock 4x4 **********************************************
    read_ipred_modes(currMB);

    // read CBP and Coeffs  ***************************************************************
    currSlice->read_CBP_and_coeffs_from_NAL (currMB);        
  }
  else // all other modes
  {
    read_intra_macroblock(currMB);
  }
  return;
}

/*!
 ************************************************************************
 * \brief
 *    Get the syntax elements from the NAL
 ************************************************************************
 */
static void read_one_macroblock_p_slice_cavlc(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  SyntaxElement currSE;
  int mb_nr = currMB->mbAddrX; 

  DataPartition *dP;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];

  if (currSlice->mb_aff_frame_flag == 0)
  {
    StorablePicture *dec_picture = currSlice->dec_picture;
    PicMotionParamsOld *motion = &dec_picture->motion;

    currMB->mb_field = FALSE;

    update_qp(currMB, currSlice->qp);
    currSE.type = SE_MBTYPE;

    //  read MB mode *****************************************************************
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

    currSE.mapping = linfo_ue;  

    // VLC Non-Intra  
    if(currSlice->cod_counter == -1)
    {
      TRACE_STRING("mb_skip_run");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currSlice->cod_counter = currSE.value1;
    }

    if (currSlice->cod_counter==0)
    {
      // read MB type
      TRACE_STRING("mb_type");
      dP->readSyntaxElement(currMB, &currSE, dP);
      ++(currSE.value1);
      currMB->mb_type = (short) currSE.value1;
      if(!dP->bitstream->ei_flag)
        currMB->ei_flag = 0;
      currSlice->cod_counter--;
      currMB->skip_flag = 0;
    }
    else
    {
      currSlice->cod_counter--;
      currMB->mb_type = 0;
      currMB->ei_flag = 0;
      currMB->skip_flag = 1;      
    }
    //update the list offset;
    currMB->list_offset = 0;  

    motion->mb_field[mb_nr] = (byte) FALSE;

    currMB->block_y_aff = currMB->block_y;

    currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;

    currSlice->interpret_mb_mode(currMB);    
  }
  else
  {
    VideoParameters *p_Vid = currMB->p_Vid;

    Macroblock *topMB = NULL;
    int  prevMbSkipped = 0;
    StorablePicture *dec_picture = currSlice->dec_picture;
    PicMotionParamsOld *motion = &dec_picture->motion;

    if (mb_nr&0x01)
    {
      topMB= &p_Vid->mb_data[mb_nr-1];
      prevMbSkipped = (topMB->mb_type == 0);
    }
    else
      prevMbSkipped = 0;

    currMB->mb_field = ((mb_nr&0x01) == 0)? FALSE : p_Vid->mb_data[mb_nr-1].mb_field;

    update_qp(currMB, currSlice->qp);
    currSE.type = SE_MBTYPE;

    //  read MB mode *****************************************************************
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

    currSE.mapping = linfo_ue;  

    // VLC Non-Intra  
    if(currSlice->cod_counter == -1)
    {
      TRACE_STRING("mb_skip_run");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currSlice->cod_counter = currSE.value1;
    }

    if (currSlice->cod_counter==0)
    {
      // read MB aff
      if ((((mb_nr&0x01)==0) || ((mb_nr&0x01) && prevMbSkipped)))
      {
        TRACE_STRING("mb_field_decoding_flag");
        currSE.len = (int64) 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
        currMB->mb_field = (Boolean) currSE.value1;
      }

      // read MB type
      TRACE_STRING("mb_type");
      dP->readSyntaxElement(currMB, &currSE, dP);
      ++(currSE.value1);
      currMB->mb_type = (short) currSE.value1;
      if(!dP->bitstream->ei_flag)
        currMB->ei_flag = 0;
      currSlice->cod_counter--;
      currMB->skip_flag = 0;
    }
    else
    {
      currSlice->cod_counter--;
      currMB->mb_type = 0;
      currMB->ei_flag = 0;
      currMB->skip_flag = 1;

      // read field flag of bottom block
      if(currSlice->cod_counter == 0 && ((mb_nr&0x01) == 0))
      {
        TRACE_STRING("mb_field_decoding_flag (of coded bottom mb)");
        currSE.len = (int64) 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
        dP->bitstream->frame_bitoffset--;
        TRACE_DECBITS(1);
        currMB->mb_field = (Boolean) currSE.value1;
      }
      else if (currSlice->cod_counter > 0 && ((mb_nr & 0x01) == 0))
      {
        // check left macroblock pair first
        if (mb_is_available(mb_nr - 2, currMB) && ((mb_nr % (p_Vid->PicWidthInMbs * 2))!=0))
        {
          currMB->mb_field = p_Vid->mb_data[mb_nr-2].mb_field;
        }
        else
        {
          // check top macroblock pair
          if (mb_is_available(mb_nr - 2*p_Vid->PicWidthInMbs, currMB))
          {
            currMB->mb_field = p_Vid->mb_data[mb_nr-2*p_Vid->PicWidthInMbs].mb_field;
          }
          else
            currMB->mb_field = FALSE;
        }        
      }
    }
    //update the list offset;
    currMB->list_offset = (currMB->mb_field)? ((mb_nr&0x01)? 4: 2): 0;  

    motion->mb_field[mb_nr] = (byte) currMB->mb_field;

    currMB->block_y_aff = (currMB->mb_field) ? (mb_nr&0x01) ? (currMB->block_y - 4)>>1 : currMB->block_y >> 1 : currMB->block_y;

    currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;

    currSlice->interpret_mb_mode(currMB);

    if(currMB->mb_field)
    {
      currSlice->num_ref_idx_active[LIST_0] <<=1;
      currSlice->num_ref_idx_active[LIST_1] <<=1;
    }
  }
    //init NoMbPartLessThan8x8Flag
    currMB->NoMbPartLessThan8x8Flag = TRUE;

    if (currMB->mb_type == IPCM) // I_PCM mode
    {     
      read_i_pcm_macroblock(currMB, partMap);
    }
    else if (currMB->mb_type == I4MB)
    {      
      read_intra4x4_macroblock_cavlc(currMB, partMap);
    }
    else if (currMB->mb_type == P8x8)
    {
      currSE.type = SE_MBTYPE;
      currSE.mapping = linfo_ue;
      dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

      read_P8x8_macroblock(currMB, dP, &currSE);
    }
    else if (currMB->mb_type == PSKIP)
    {
      read_skip_macroblock(currMB);
    }    
    else if (currMB->is_intra_block) // all other intra modes
    {
      read_intra_macroblock(currMB);
    }
    else // all other remaining modes
    {
      read_inter_macroblock(currMB);
    }
  

  return;
}

/*!
 ************************************************************************
 * \brief
 *    Get the syntax elements from the NAL
 ************************************************************************
 */
static void read_one_macroblock_p_slice_cabac(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;  
  VideoParameters *p_Vid = currMB->p_Vid;
  int mb_nr = currMB->mbAddrX; 
  SyntaxElement currSE;
  DataPartition *dP;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];

  if (currSlice->mb_aff_frame_flag == 0)
  {
    StorablePicture *dec_picture = currSlice->dec_picture;
    PicMotionParamsOld *motion = &dec_picture->motion;

    currMB->mb_field = FALSE;

    update_qp(currMB, currSlice->qp);
    currSE.type = SE_MBTYPE;

    //  read MB mode *****************************************************************
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

    if (dP->bitstream->ei_flag)   
      currSE.mapping = linfo_ue;

    CheckAvailabilityOfNeighborsCABAC(currMB);
    TRACE_STRING("mb_skip_flag");
    currSE.reading = read_skip_flag_CABAC_p_slice;
    dP->readSyntaxElement(currMB, &currSE, dP);

    currMB->mb_type   = (short) currSE.value1;
    currMB->skip_flag = (char) (!(currSE.value1));

    if (!dP->bitstream->ei_flag)
      currMB->ei_flag = 0;    

    // read MB type
    if (currMB->mb_type != 0 )
    {
      currSE.reading = readMB_typeInfo_CABAC_p_slice;
      TRACE_STRING("mb_type");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currMB->mb_type = (short) currSE.value1;
      if(!dP->bitstream->ei_flag)
        currMB->ei_flag = 0;
    }

    motion->mb_field[mb_nr] = (byte) FALSE;
    currMB->block_y_aff = currMB->block_y;
    currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;
    currSlice->interpret_mb_mode(currMB);    
  }
  else
  {
    Macroblock *topMB = NULL;
    int  prevMbSkipped = 0;
    int  check_bottom, read_bottom, read_top;  
    StorablePicture *dec_picture = currSlice->dec_picture;
    PicMotionParamsOld *motion = &dec_picture->motion;
    if (mb_nr&0x01)
    {
      topMB= &p_Vid->mb_data[mb_nr-1];
      prevMbSkipped = (topMB->mb_type == 0);
    }
    else
      prevMbSkipped = 0;

    currMB->mb_field = ((mb_nr&0x01) == 0)? FALSE : p_Vid->mb_data[mb_nr-1].mb_field;

    update_qp(currMB, currSlice->qp);
    currSE.type = SE_MBTYPE;

    //  read MB mode *****************************************************************
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

    if (dP->bitstream->ei_flag)   
      currSE.mapping = linfo_ue;

    // read MB skip_flag
    if (((mb_nr&0x01) == 0||prevMbSkipped))
      field_flag_inference(currMB);

    CheckAvailabilityOfNeighborsCABAC(currMB);
    TRACE_STRING("mb_skip_flag");
    currSE.reading = read_skip_flag_CABAC_p_slice;
    dP->readSyntaxElement(currMB, &currSE, dP);

    currMB->mb_type   = (short) currSE.value1;
    currMB->skip_flag = (char) (!(currSE.value1));

    if (!dP->bitstream->ei_flag)
      currMB->ei_flag = 0;

    // read MB AFF
    check_bottom=read_bottom=read_top=0;
    if ((mb_nr&0x01)==0)
    {
      check_bottom =  currMB->skip_flag;
      read_top = !check_bottom;
    }
    else
    {
      read_bottom = (topMB->skip_flag && (!currMB->skip_flag));
    }

    if (read_bottom || read_top)
    {
      TRACE_STRING("mb_field_decoding_flag");
      currSE.reading = readFieldModeInfo_CABAC;
      dP->readSyntaxElement(currMB, &currSE, dP);
      currMB->mb_field = (Boolean) currSE.value1;
    }

    if (check_bottom)
      check_next_mb_and_get_field_mode_CABAC_p_slice(currSlice, &currSE, dP);

    //update the list offset;
    currMB->list_offset = (currMB->mb_field)? ((mb_nr&0x01)? 4: 2): 0;

    //if (currMB->mb_type != 0 )
    CheckAvailabilityOfNeighborsCABAC(currMB);    

    // read MB type
    if (currMB->mb_type != 0 )
    {
      currSE.reading = readMB_typeInfo_CABAC_p_slice;
      TRACE_STRING("mb_type");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currMB->mb_type = (short) currSE.value1;
      if(!dP->bitstream->ei_flag)
        currMB->ei_flag = 0;
    }

    motion->mb_field[mb_nr] = (byte) currMB->mb_field;

    currMB->block_y_aff = (currMB->mb_field) ? (mb_nr&0x01) ? (currMB->block_y - 4)>>1 : currMB->block_y >> 1 : currMB->block_y;

    currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;

    currSlice->interpret_mb_mode(currMB);

    if(currMB->mb_field)
    {
      currSlice->num_ref_idx_active[LIST_0] <<=1;
      currSlice->num_ref_idx_active[LIST_1] <<=1;
    }
  }
  //init NoMbPartLessThan8x8Flag
  currMB->NoMbPartLessThan8x8Flag = TRUE;

  if (currMB->mb_type == IPCM) // I_PCM mode
  {
    read_i_pcm_macroblock(currMB, partMap);
  }
  else if (currMB->mb_type == I4MB)
  {
    read_intra4x4_macroblock_cabac(currMB, partMap);
  }
  else if (currMB->mb_type == P8x8)
  {
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);
    currSE.type = SE_MBTYPE;      

    if (dP->bitstream->ei_flag) 
      currSE.mapping = linfo_ue;
    else
      currSE.reading = readB8_typeInfo_CABAC_p_slice;

    read_P8x8_macroblock(currMB, dP, &currSE);
  }
  else if (currMB->mb_type == PSKIP)
  {
    read_skip_macroblock (currMB);
  }
  else if (currMB->is_intra_block == TRUE) // all other intra modes
  {
    read_intra_macroblock(currMB);
  }
  else // all other remaining modes
  {
    read_inter_macroblock(currMB);
  }

  return;
}

/*!
 ************************************************************************
 * \brief
 *    Get the syntax elements from the NAL
 ************************************************************************
 */
static void read_one_macroblock_b_slice_cavlc(Macroblock *currMB)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  Slice *currSlice = currMB->p_Slice;
  int mb_nr = currMB->mbAddrX; 
  DataPartition *dP;
  SyntaxElement currSE;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];

  if (currSlice->mb_aff_frame_flag == 0)
  {
    StorablePicture *dec_picture = currSlice->dec_picture;
    PicMotionParamsOld *motion = &dec_picture->motion;

    currMB->mb_field = FALSE;

    update_qp(currMB, currSlice->qp);
    currSE.type = SE_MBTYPE;

    //  read MB mode *****************************************************************
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

    currSE.mapping = linfo_ue;

    if(currSlice->cod_counter == -1)
    {
      TRACE_STRING("mb_skip_run");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currSlice->cod_counter = currSE.value1;
    }
    if (currSlice->cod_counter==0)
    {
      // read MB type
      TRACE_STRING("mb_type");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currMB->mb_type = (short) currSE.value1;
      if(!dP->bitstream->ei_flag)
        currMB->ei_flag = 0;
      currSlice->cod_counter--;
      currMB->skip_flag = 0;
    }
    else
    {
      currSlice->cod_counter--;
      currMB->mb_type = 0;
      currMB->ei_flag = 0;
      currMB->skip_flag = 1;

    }
    //update the list offset;
    currMB->list_offset = 0;

    motion->mb_field[mb_nr] = FALSE;
    currMB->block_y_aff = currMB->block_y;
    currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;

    currSlice->interpret_mb_mode(currMB);
  }
  else
  {   
    Macroblock *topMB = NULL;
    int  prevMbSkipped = 0;
    StorablePicture *dec_picture = currSlice->dec_picture;
    PicMotionParamsOld *motion = &dec_picture->motion;

    if (mb_nr&0x01)
    {
      topMB= &p_Vid->mb_data[mb_nr-1];
      prevMbSkipped = topMB->skip_flag;
    }
    else
      prevMbSkipped = 0;

    currMB->mb_field = ((mb_nr&0x01) == 0)? FALSE : p_Vid->mb_data[mb_nr-1].mb_field;

    update_qp(currMB, currSlice->qp);
    currSE.type = SE_MBTYPE;

    //  read MB mode *****************************************************************
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

    currSE.mapping = linfo_ue;

    if(currSlice->cod_counter == -1)
    {
      TRACE_STRING("mb_skip_run");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currSlice->cod_counter = currSE.value1;
    }
    if (currSlice->cod_counter==0)
    {
      // read MB aff
      if ((((mb_nr&0x01)==0) || ((mb_nr&0x01) && prevMbSkipped)))
      {
        TRACE_STRING("mb_field_decoding_flag");
        currSE.len = (int64) 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
        currMB->mb_field = (Boolean) currSE.value1;
      }

      // read MB type
      TRACE_STRING("mb_type");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currMB->mb_type = (short) currSE.value1;
      if(!dP->bitstream->ei_flag)
        currMB->ei_flag = 0;
      currSlice->cod_counter--;
      currMB->skip_flag = 0;
    }
    else
    {
      currSlice->cod_counter--;
      currMB->mb_type = 0;
      currMB->ei_flag = 0;
      currMB->skip_flag = 1;

      // read field flag of bottom block
      if(currSlice->cod_counter == 0 && ((mb_nr&0x01) == 0))
      {
        TRACE_STRING("mb_field_decoding_flag (of coded bottom mb)");
        currSE.len = (int64) 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
        dP->bitstream->frame_bitoffset--;
        TRACE_DECBITS(1);
        currMB->mb_field = (Boolean) currSE.value1;
      }
      else if (currSlice->cod_counter > 0 && ((mb_nr & 0x01) == 0))
      {
        // check left macroblock pair first
        if (mb_is_available(mb_nr - 2, currMB) && ((mb_nr % (p_Vid->PicWidthInMbs * 2))!=0))
        {
          currMB->mb_field = p_Vid->mb_data[mb_nr-2].mb_field;
        }
        else
        {
          // check top macroblock pair
          if (mb_is_available(mb_nr - 2*p_Vid->PicWidthInMbs, currMB))
          {
            currMB->mb_field = p_Vid->mb_data[mb_nr-2*p_Vid->PicWidthInMbs].mb_field;
          }
          else
            currMB->mb_field = FALSE;
        }
      }      
    }
    //update the list offset;
    currMB->list_offset = (currMB->mb_field)? ((mb_nr&0x01)? 4: 2): 0;

    motion->mb_field[mb_nr] = (byte) currMB->mb_field;

    currMB->block_y_aff = (currMB->mb_field) ? (mb_nr&0x01) ? (currMB->block_y - 4)>>1 : currMB->block_y >> 1 : currMB->block_y;

    currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;

    currSlice->interpret_mb_mode(currMB);

    if(currSlice->mb_aff_frame_flag)
    {
      if(currMB->mb_field)
      {
        currSlice->num_ref_idx_active[LIST_0] <<=1;
        currSlice->num_ref_idx_active[LIST_1] <<=1;
      }
    }
  }

  if (currMB->mb_type == IPCM)
  {
    read_i_pcm_macroblock(currMB, partMap);
  }
  else if (currMB->mb_type == I4MB)
  {
    read_intra4x4_macroblock_cavlc(currMB, partMap);
  }
  else if (currMB->mb_type == P8x8)
  {
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);
    currSE.type = SE_MBTYPE;
    currSE.mapping = linfo_ue;

    read_P8x8_macroblock(currMB, dP, &currSE);
  }
  else if (currMB->mb_type == BSKIP_DIRECT)
  {
    //init NoMbPartLessThan8x8Flag
    currMB->NoMbPartLessThan8x8Flag = (!(currSlice->active_sps->direct_8x8_inference_flag))? FALSE: TRUE;

    currMB->luma_transform_size_8x8_flag = FALSE;

    if(p_Vid->active_pps->constrained_intra_pred_flag)
    {
      currSlice->intra_block[mb_nr] = 0;
    }

    //--- init macroblock data ---
    init_macroblock_direct(currMB);

    if (currSlice->cod_counter >= 0)
    {
      currMB->cbp = 0;
      reset_coeffs(currMB);
    }
    else
    {
      // read CBP and Coeffs  ***************************************************************
      currSlice->read_CBP_and_coeffs_from_NAL (currMB);
    }     
  }
  else if (currMB->is_intra_block == TRUE) // all other intra modes
  {
    read_intra_macroblock(currMB);
  }
  else // all other remaining modes
  {
    read_inter_macroblock(currMB);
  }

  return;
}

/*!
 ************************************************************************
 * \brief
 *    Get the syntax elements from the NAL
 ************************************************************************
 */
static void read_one_macroblock_b_slice_cabac(Macroblock *currMB)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  int mb_nr = currMB->mbAddrX; 
  SyntaxElement currSE;

  DataPartition *dP;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];

  if (currSlice->mb_aff_frame_flag == 0)
  {
    StorablePicture *dec_picture = currSlice->dec_picture;
    PicMotionParamsOld *motion = &dec_picture->motion;

    currMB->mb_field = FALSE;

    update_qp(currMB, currSlice->qp);
    currSE.type = SE_MBTYPE;

    //  read MB mode *****************************************************************
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

    if (dP->bitstream->ei_flag)   
      currSE.mapping = linfo_ue;

    CheckAvailabilityOfNeighborsCABAC(currMB);
    TRACE_STRING("mb_skip_flag");
    currSE.reading = read_skip_flag_CABAC_b_slice;
    dP->readSyntaxElement(currMB, &currSE, dP);

    currMB->mb_type   = (short) currSE.value1;
    currMB->skip_flag = (char) (!(currSE.value1));

    currMB->cbp = currSE.value2;

    if (!dP->bitstream->ei_flag)
      currMB->ei_flag = 0;

    if (currSE.value1 == 0 && currSE.value2 == 0)
      currSlice->cod_counter=0;   

    // read MB type
    if (currMB->mb_type != 0 )
    {
      currSE.reading = readMB_typeInfo_CABAC_b_slice;
      TRACE_STRING("mb_type");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currMB->mb_type = (short) currSE.value1;
      if(!dP->bitstream->ei_flag)
        currMB->ei_flag = 0;
    }

    motion->mb_field[mb_nr] = (byte) FALSE;
    currMB->block_y_aff = currMB->block_y;
    currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;
    currSlice->interpret_mb_mode(currMB);
  }
  else
  {
    Macroblock *topMB = NULL;
    int  prevMbSkipped = 0;
    int  check_bottom, read_bottom, read_top;  
    StorablePicture *dec_picture = currSlice->dec_picture;
    PicMotionParamsOld *motion = &dec_picture->motion;

    if (mb_nr&0x01)
    {
      topMB= &p_Vid->mb_data[mb_nr-1];
      prevMbSkipped = topMB->skip_flag;
    }
    else
      prevMbSkipped = 0;

    currMB->mb_field = ((mb_nr&0x01) == 0)? FALSE : p_Vid->mb_data[mb_nr-1].mb_field;

    update_qp(currMB, currSlice->qp);
    currSE.type = SE_MBTYPE;

    //  read MB mode *****************************************************************
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);

    if (dP->bitstream->ei_flag)   
      currSE.mapping = linfo_ue;

    // read MB skip_flag
    if (((mb_nr&0x01) == 0||prevMbSkipped))
      field_flag_inference(currMB);

    CheckAvailabilityOfNeighborsCABAC(currMB);
    TRACE_STRING("mb_skip_flag");
    currSE.reading = read_skip_flag_CABAC_b_slice;
    dP->readSyntaxElement(currMB, &currSE, dP);

    currMB->mb_type   = (short) currSE.value1;
    currMB->skip_flag = (char) (!(currSE.value1));

    currMB->cbp = currSE.value2;

    if (!dP->bitstream->ei_flag)
      currMB->ei_flag = 0;

    if (currSE.value1 == 0 && currSE.value2 == 0)
      currSlice->cod_counter=0;

    // read MB AFF
    check_bottom=read_bottom=read_top=0;
    if ((mb_nr & 0x01) == 0)
    {
      check_bottom =  currMB->skip_flag;
      read_top = !check_bottom;
    }
    else
    {
      read_bottom = (topMB->skip_flag && (!currMB->skip_flag));
    }

    if (read_bottom || read_top)
    {
      TRACE_STRING("mb_field_decoding_flag");
      currSE.reading = readFieldModeInfo_CABAC;
      dP->readSyntaxElement(currMB, &currSE, dP);
      currMB->mb_field = (Boolean) currSE.value1;
    }
    if (check_bottom)
      check_next_mb_and_get_field_mode_CABAC_b_slice(currSlice, &currSE, dP);

    //update the list offset;
    currMB->list_offset = (currMB->mb_field)? ((mb_nr&0x01)? 4: 2): 0;
    //if (currMB->mb_type != 0 )
    CheckAvailabilityOfNeighborsCABAC(currMB);

    // read MB type
    if (currMB->mb_type != 0 )
    {
      currSE.reading = readMB_typeInfo_CABAC_b_slice;
      TRACE_STRING("mb_type");
      dP->readSyntaxElement(currMB, &currSE, dP);
      currMB->mb_type = (short) currSE.value1;
      if(!dP->bitstream->ei_flag)
        currMB->ei_flag = 0;
    }


    motion->mb_field[mb_nr] = (byte) currMB->mb_field;

    currMB->block_y_aff = (currMB->mb_field) ? (mb_nr&0x01) ? (currMB->block_y - 4)>>1 : currMB->block_y >> 1 : currMB->block_y;

    currSlice->siblock[currMB->mb.y][currMB->mb.x] = 0;

    currSlice->interpret_mb_mode(currMB);

    if(currMB->mb_field)
    {
      currSlice->num_ref_idx_active[LIST_0] <<=1;
      currSlice->num_ref_idx_active[LIST_1] <<=1;
    }
  }

  if(currMB->mb_type == IPCM)
  {
    read_i_pcm_macroblock(currMB, partMap);
  }
  else if (currMB->mb_type == I4MB)
  {
    read_intra4x4_macroblock_cabac(currMB, partMap);
  }
  else if (currMB->mb_type == P8x8)
  {
    dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);
    currSE.type = SE_MBTYPE;      

    if (dP->bitstream->ei_flag) 
      currSE.mapping = linfo_ue;
    else
      currSE.reading = readB8_typeInfo_CABAC_b_slice;

    read_P8x8_macroblock(currMB, dP, &currSE);
  }
  else if (currMB->mb_type == BSKIP_DIRECT)
  {
    //init NoMbPartLessThan8x8Flag
    currMB->NoMbPartLessThan8x8Flag = (!(currSlice->active_sps->direct_8x8_inference_flag))? FALSE: TRUE;

    //============= Transform Size Flag for INTRA MBs =============
    //-------------------------------------------------------------
    //transform size flag for INTRA_4x4 and INTRA_8x8 modes
    currMB->luma_transform_size_8x8_flag = FALSE;

    if(p_Vid->active_pps->constrained_intra_pred_flag)
    {
      currSlice->intra_block[mb_nr] = 0;
    }

    //--- init macroblock data ---
    init_macroblock_direct(currMB);

    if (currSlice->cod_counter >= 0)
    {
      currSlice->is_reset_coeff = TRUE;
      currMB->cbp = 0;
      currSlice->cod_counter = -1;
    }
    else
    {
      // read CBP and Coeffs  ***************************************************************
      currSlice->read_CBP_and_coeffs_from_NAL (currMB);
    }      
  }
  else if (currMB->is_intra_block == TRUE) // all other intra modes
  {
    read_intra_macroblock(currMB);
  }
  else // all other remaining modes
  {
    read_inter_macroblock(currMB);
  }

  return;
}


void setup_read_macroblock(Slice *currSlice)
{
  if (currSlice->p_Vid->active_pps->entropy_coding_mode_flag == (Boolean) CABAC)
  {
    switch (currSlice->slice_type)
    {
    case P_SLICE: 
    case SP_SLICE:
      currSlice->read_one_macroblock = read_one_macroblock_p_slice_cabac;
      break;
    case B_SLICE:
      currSlice->read_one_macroblock = read_one_macroblock_b_slice_cabac;
      break;
    case I_SLICE: 
    case SI_SLICE: 
      currSlice->read_one_macroblock = read_one_macroblock_i_slice_cabac;
      break;
    default:
      printf("Unsupported slice type\n");
      break;
    }
  }
  else
  {
    switch (currSlice->slice_type)
    {
    case P_SLICE: 
    case SP_SLICE:
      currSlice->read_one_macroblock = read_one_macroblock_p_slice_cavlc;
      break;
    case B_SLICE:
      currSlice->read_one_macroblock = read_one_macroblock_b_slice_cavlc;
      break;
    case I_SLICE: 
    case SI_SLICE:     
      currSlice->read_one_macroblock = read_one_macroblock_i_slice_cavlc;
      break;
    default:
      printf("Unsupported slice type\n");
      break;
    }
  }
}
