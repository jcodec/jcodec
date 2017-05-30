/*!
 ***********************************************************************
 * \file read_comp_cavlc.c
 *
 * \brief
 *     Read Coefficient Components (CAVLC version)
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Alexis Michael Tourapis         <alexismt@ieee.org>
 ***********************************************************************
*/

#include "contributors.h"

#include "global.h"
#include "elements.h"
#include "macroblock.h"
#include "vlc.h"
#include "fast_memory.h"
#include "transform.h"
#include "mb_access.h"

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

extern void  check_dp_neighbors (Macroblock *currMB);
extern void  read_delta_quant   (SyntaxElement *currSE, DataPartition *dP, Macroblock *currMB, const byte *partMap, int type);

/*!
 ************************************************************************
 * \brief
 *    Get the Prediction from the Neighboring Blocks for Number of 
 *    Nonzero Coefficients
 *
 *    Luma Blocks
 ************************************************************************
 */
static int predict_nnz(Macroblock *currMB, int block_type, int i,int j)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  Slice *currSlice = currMB->p_Slice;

  PixelPos pix;

  int pred_nnz = 0;
  int cnt      = 0;

  // left block
  get4x4Neighbour(currMB, i - 1, j, p_Vid->mb_size[IS_LUMA], &pix);

  if ((currMB->is_intra_block == TRUE) && pix.available && p_Vid->active_pps->constrained_intra_pred_flag && (currSlice->dp_mode == PAR_DP_3))
  {
    pix.available &= currSlice->intra_block[pix.mb_addr];
    if (!pix.available)
      ++cnt;
  }

  if (pix.available)
  { 
    switch (block_type)
    {
    case LUMA:
      pred_nnz = p_Vid->nz_coeff [pix.mb_addr ][0][pix.y][pix.x];
      ++cnt;
      break;
    case CB:
      pred_nnz = p_Vid->nz_coeff [pix.mb_addr ][1][pix.y][pix.x];
      ++cnt;
      break;
    case CR:
      pred_nnz = p_Vid->nz_coeff [pix.mb_addr ][2][pix.y][pix.x];
      ++cnt;
      break;
    default:
      error("writeCoeff4x4_CAVLC: Invalid block type", 600);
      break;
    }
  }

  // top block
  get4x4Neighbour(currMB, i, j - 1, p_Vid->mb_size[IS_LUMA], &pix);

  if ((currMB->is_intra_block == TRUE) && pix.available && p_Vid->active_pps->constrained_intra_pred_flag && (currSlice->dp_mode==PAR_DP_3))
  {
    pix.available &= currSlice->intra_block[pix.mb_addr];
    if (!pix.available)
      ++cnt;
  }

  if (pix.available)
  {
    switch (block_type)
    {
    case LUMA:
      pred_nnz += p_Vid->nz_coeff [pix.mb_addr ][0][pix.y][pix.x];
      ++cnt;
      break;
    case CB:
      pred_nnz += p_Vid->nz_coeff [pix.mb_addr ][1][pix.y][pix.x];
      ++cnt;
      break;
    case CR:
      pred_nnz += p_Vid->nz_coeff [pix.mb_addr ][2][pix.y][pix.x];
      ++cnt;
      break;
    default:
      error("writeCoeff4x4_CAVLC: Invalid block type", 600);
      break;
    }
  }

  if (cnt==2)
  {
    ++pred_nnz;
    pred_nnz >>= 1;
  }

  return pred_nnz;
}


/*!
 ************************************************************************
 * \brief
 *    Get the Prediction from the Neighboring Blocks for Number of 
 *    Nonzero Coefficients
 *
 *    Chroma Blocks
 ************************************************************************
 */
static int predict_nnz_chroma(Macroblock *currMB, int i,int j)
{
  StorablePicture *dec_picture = currMB->p_Slice->dec_picture;

  if (dec_picture->chroma_format_idc != YUV444)
  {
    VideoParameters *p_Vid = currMB->p_Vid;    
    Slice *currSlice = currMB->p_Slice;
    PixelPos pix;
    int pred_nnz = 0;
    int cnt      = 0;

    //YUV420 and YUV422
    // left block
    get4x4Neighbour(currMB, ((i&0x01)<<2) - 1, j, p_Vid->mb_size[IS_CHROMA], &pix);

    if ((currMB->is_intra_block == TRUE) && pix.available && p_Vid->active_pps->constrained_intra_pred_flag && (currSlice->dp_mode==PAR_DP_3))
    {
      pix.available &= currSlice->intra_block[pix.mb_addr];
      if (!pix.available)
        ++cnt;
    }

    if (pix.available)
    {
      pred_nnz = p_Vid->nz_coeff [pix.mb_addr ][1][pix.y][2 * (i>>1) + pix.x];
      ++cnt;
    }

    // top block
    get4x4Neighbour(currMB, ((i&0x01)<<2), j - 1, p_Vid->mb_size[IS_CHROMA], &pix);

    if ((currMB->is_intra_block == TRUE) && pix.available && p_Vid->active_pps->constrained_intra_pred_flag && (currSlice->dp_mode==PAR_DP_3))
    {
      pix.available &= currSlice->intra_block[pix.mb_addr];
      if (!pix.available)
        ++cnt;
    }

    if (pix.available)
    {
      pred_nnz += p_Vid->nz_coeff [pix.mb_addr ][1][pix.y][2 * (i>>1) + pix.x];
      ++cnt;
    }

    if (cnt==2)
    {
      ++pred_nnz;
      pred_nnz >>= 1;
    }
    return pred_nnz;
  }
  else
    return 0;
}

/*!
 ************************************************************************
 * \brief
 *    Reads coeff of an 4x4 block (CAVLC)
 *
 * \author
 *    Karl Lillevold <karll@real.com>
 *    contributions by James Au <james@ubvideo.com>
 ************************************************************************
 */
void read_coeff_4x4_CAVLC (Macroblock *currMB, 
                           int block_type,
                           int i, int j, int levarr[16], int runarr[16],
                           int *number_coefficients)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  int mb_nr = currMB->mbAddrX;
  SyntaxElement currSE;
  DataPartition *dP;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  Bitstream *currStream;

  int k, code, vlcnum;
  int numcoeff = 0, numtrailingones;
  int level_two_or_higher;
  int numones, totzeros, abslevel, cdc=0, cac=0;
  int zerosleft, ntr, dptype = 0;
  int max_coeff_num = 0, nnz;
  char type[15];
  static const int incVlc[] = {0, 3, 6, 12, 24, 48, 32768};    // maximum vlc = 6

  switch (block_type)
  {
  case LUMA:
    max_coeff_num = 16;
    TRACE_PRINTF("Luma");
    dptype = (currMB->is_intra_block == TRUE) ? SE_LUM_AC_INTRA : SE_LUM_AC_INTER;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  case LUMA_INTRA16x16DC:
    max_coeff_num = 16;
    TRACE_PRINTF("Lum16DC");
    dptype = SE_LUM_DC_INTRA;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  case LUMA_INTRA16x16AC:
    max_coeff_num = 15;
    TRACE_PRINTF("Lum16AC");
    dptype = SE_LUM_AC_INTRA;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  case CHROMA_DC:
    max_coeff_num = p_Vid->num_cdc_coeff;
    cdc = 1;
    TRACE_PRINTF("ChrDC");
    dptype = (currMB->is_intra_block == TRUE) ? SE_CHR_DC_INTRA : SE_CHR_DC_INTER;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  case CHROMA_AC:
    max_coeff_num = 15;
    cac = 1;
    TRACE_PRINTF("ChrAC");
    dptype = (currMB->is_intra_block == TRUE) ? SE_CHR_AC_INTRA : SE_CHR_AC_INTER;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  default:
    error ("read_coeff_4x4_CAVLC: invalid block type", 600);
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  }

  currSE.type = dptype;
  dP = &(currSlice->partArr[partMap[dptype]]);
  currStream = dP->bitstream;  

  if (!cdc)
  {    
    // luma or chroma AC    
    nnz = (!cac) ? predict_nnz(currMB, LUMA, i<<2, j<<2) : predict_nnz_chroma(currMB, i, ((j-4)<<2));

    currSE.value1 = (nnz < 2) ? 0 : ((nnz < 4) ? 1 : ((nnz < 8) ? 2 : 3));

    readSyntaxElement_NumCoeffTrailingOnes(&currSE, currStream, type);

    numcoeff        =  currSE.value1;
    numtrailingones =  currSE.value2;

    p_Vid->nz_coeff[mb_nr][0][j][i] = (byte) numcoeff;
  }
  else
  {
    // chroma DC
    readSyntaxElement_NumCoeffTrailingOnesChromaDC(p_Vid, &currSE, currStream);

    numcoeff        =  currSE.value1;
    numtrailingones =  currSE.value2;
  }

  memset(levarr, 0, max_coeff_num * sizeof(int));
  memset(runarr, 0, max_coeff_num * sizeof(int));

  numones = numtrailingones;
  *number_coefficients = numcoeff;

  if (numcoeff)
  {
    if (numtrailingones)
    {      
      currSE.len = numtrailingones;

#if TRACE
      snprintf(currSE.tracestring,
        TRACESTRING_SIZE, "%s trailing ones sign (%d,%d)", type, i, j);
#endif

      readSyntaxElement_FLC (&currSE, currStream);

      code = currSE.inf;
      ntr = numtrailingones;
      for (k = numcoeff - 1; k > numcoeff - 1 - numtrailingones; k--)
      {
        ntr --;
        levarr[k] = (code>>ntr)&1 ? -1 : 1;
      }
    }

    // decode levels
    level_two_or_higher = (numcoeff > 3 && numtrailingones == 3)? 0 : 1;
    vlcnum = (numcoeff > 10 && numtrailingones < 3) ? 1 : 0;

    for (k = numcoeff - 1 - numtrailingones; k >= 0; k--)
    {

#if TRACE
      snprintf(currSE.tracestring,
        TRACESTRING_SIZE, "%s lev (%d,%d) k=%d vlc=%d ", type, i, j, k, vlcnum);
#endif

      if (vlcnum == 0)
        readSyntaxElement_Level_VLC0(&currSE, currStream);
      else
        readSyntaxElement_Level_VLCN(&currSE, vlcnum, currStream);

      if (level_two_or_higher)
      {
        currSE.inf += (currSE.inf > 0) ? 1 : -1;
        level_two_or_higher = 0;
      }

      levarr[k] = currSE.inf;
      abslevel = iabs(levarr[k]);
      if (abslevel  == 1)
        ++numones;

      // update VLC table
      if (abslevel  > incVlc[vlcnum])
        ++vlcnum;

      if (k == numcoeff - 1 - numtrailingones && abslevel >3)
        vlcnum = 2;      
    }

    if (numcoeff < max_coeff_num)
    {
      // decode total run
      vlcnum = numcoeff - 1;
      currSE.value1 = vlcnum;

#if TRACE
      snprintf(currSE.tracestring,
        TRACESTRING_SIZE, "%s totalrun (%d,%d) vlc=%d ", type, i,j, vlcnum);
#endif
      if (cdc)
        readSyntaxElement_TotalZerosChromaDC(p_Vid, &currSE, currStream);
      else
        readSyntaxElement_TotalZeros(&currSE, currStream);

      totzeros = currSE.value1;
    }
    else
    {
      totzeros = 0;
    }

    // decode run before each coefficient
    zerosleft = totzeros;
    i = numcoeff - 1;

    if (zerosleft > 0 && i > 0)
    {
      do
      {
        // select VLC for runbefore
        vlcnum = imin(zerosleft - 1, RUNBEFORE_NUM_M1);

        currSE.value1 = vlcnum;
#if TRACE
        snprintf(currSE.tracestring,
          TRACESTRING_SIZE, "%s run (%d,%d) k=%d vlc=%d ",
          type, i, j, i, vlcnum);
#endif

        readSyntaxElement_Run(&currSE, currStream);
        runarr[i] = currSE.value1;

        zerosleft -= runarr[i];
        i --;
      } while (zerosleft != 0 && i != 0);
    }
    runarr[i] = zerosleft;    
  } // if numcoeff
}

/*!
 ************************************************************************
 * \brief
 *    Reads coeff of an 4x4 block (CAVLC)
 *
 * \author
 *    Karl Lillevold <karll@real.com>
 *    contributions by James Au <james@ubvideo.com>
 ************************************************************************
 */
void read_coeff_4x4_CAVLC_444 (Macroblock *currMB, 
                               int block_type,
                               int i, int j, int levarr[16], int runarr[16],
                               int *number_coefficients)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  int mb_nr = currMB->mbAddrX;
  SyntaxElement currSE;
  DataPartition *dP;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  Bitstream *currStream;

  int k, code, vlcnum;
  int numcoeff = 0, numtrailingones;
  int level_two_or_higher;
  int numones, totzeros, abslevel, cdc=0, cac=0;
  int zerosleft, ntr, dptype = 0;
  int max_coeff_num = 0, nnz;
  char type[15];
  static const int incVlc[] = {0, 3, 6, 12, 24, 48, 32768};    // maximum vlc = 6

  switch (block_type)
  {
  case LUMA:
    max_coeff_num = 16;
    TRACE_PRINTF("Luma");
    dptype = (currMB->is_intra_block == TRUE) ? SE_LUM_AC_INTRA : SE_LUM_AC_INTER;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  case LUMA_INTRA16x16DC:
    max_coeff_num = 16;
    TRACE_PRINTF("Lum16DC");
    dptype = SE_LUM_DC_INTRA;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  case LUMA_INTRA16x16AC:
    max_coeff_num = 15;
    TRACE_PRINTF("Lum16AC");
    dptype = SE_LUM_AC_INTRA;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  case CB:
    max_coeff_num = 16;
    TRACE_PRINTF("Luma_add1");
    dptype = ((currMB->is_intra_block == TRUE)) ? SE_LUM_AC_INTRA : SE_LUM_AC_INTER;
    p_Vid->nz_coeff[mb_nr][1][j][i] = 0; 
    break;
  case CB_INTRA16x16DC:
    max_coeff_num = 16;
    TRACE_PRINTF("Luma_add1_16DC");
    dptype = SE_LUM_DC_INTRA;
    p_Vid->nz_coeff[mb_nr][1][j][i] = 0; 
    break;
  case CB_INTRA16x16AC:
    max_coeff_num = 15;
    TRACE_PRINTF("Luma_add1_16AC");
    dptype = SE_LUM_AC_INTRA;
    p_Vid->nz_coeff[mb_nr][1][j][i] = 0; 
    break;
  case CR:
    max_coeff_num = 16;
    TRACE_PRINTF("Luma_add2");
    dptype = ((currMB->is_intra_block == TRUE)) ? SE_LUM_AC_INTRA : SE_LUM_AC_INTER;
    p_Vid->nz_coeff[mb_nr][2][j][i] = 0; 
    break;
  case CR_INTRA16x16DC:
    max_coeff_num = 16;
    TRACE_PRINTF("Luma_add2_16DC");
    dptype = SE_LUM_DC_INTRA;
    p_Vid->nz_coeff[mb_nr][2][j][i] = 0; 
    break;
  case CR_INTRA16x16AC:
    max_coeff_num = 15;
    TRACE_PRINTF("Luma_add1_16AC");
    dptype = SE_LUM_AC_INTRA;
    p_Vid->nz_coeff[mb_nr][2][j][i] = 0; 
    break;        
  case CHROMA_DC:
    max_coeff_num = p_Vid->num_cdc_coeff;
    cdc = 1;
    TRACE_PRINTF("ChrDC");
    dptype = (currMB->is_intra_block == TRUE) ? SE_CHR_DC_INTRA : SE_CHR_DC_INTER;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  case CHROMA_AC:
    max_coeff_num = 15;
    cac = 1;
    TRACE_PRINTF("ChrAC");
    dptype = (currMB->is_intra_block == TRUE) ? SE_CHR_AC_INTRA : SE_CHR_AC_INTER;
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  default:
    error ("read_coeff_4x4_CAVLC: invalid block type", 600);
    p_Vid->nz_coeff[mb_nr][0][j][i] = 0; 
    break;
  }

  currSE.type = dptype;
  dP = &(currSlice->partArr[partMap[dptype]]);
  currStream = dP->bitstream;  

  if (!cdc)
  {    
    // luma or chroma AC    
    if(block_type==LUMA || block_type==LUMA_INTRA16x16DC || block_type==LUMA_INTRA16x16AC ||block_type==CHROMA_AC)
    {
      nnz = (!cac) ? predict_nnz(currMB, LUMA, i<<2, j<<2) : predict_nnz_chroma(currMB, i, ((j-4)<<2));
    }
    else if (block_type==CB || block_type==CB_INTRA16x16DC || block_type==CB_INTRA16x16AC)
    {   
      nnz = predict_nnz(currMB, CB, i<<2, j<<2);
    }
    else
    { 
      nnz = predict_nnz(currMB, CR, i<<2, j<<2);
    }

    currSE.value1 = (nnz < 2) ? 0 : ((nnz < 4) ? 1 : ((nnz < 8) ? 2 : 3));

    readSyntaxElement_NumCoeffTrailingOnes(&currSE, currStream, type);

    numcoeff        =  currSE.value1;
    numtrailingones =  currSE.value2;

    if(block_type==LUMA || block_type==LUMA_INTRA16x16DC || block_type==LUMA_INTRA16x16AC ||block_type==CHROMA_AC)
      p_Vid->nz_coeff[mb_nr][0][j][i] = (byte) numcoeff;
    else if (block_type==CB || block_type==CB_INTRA16x16DC || block_type==CB_INTRA16x16AC)
      p_Vid->nz_coeff[mb_nr][1][j][i] = (byte) numcoeff;
    else
      p_Vid->nz_coeff[mb_nr][2][j][i] = (byte) numcoeff;        
  }
  else
  {
    // chroma DC
    readSyntaxElement_NumCoeffTrailingOnesChromaDC(p_Vid, &currSE, currStream);

    numcoeff        =  currSE.value1;
    numtrailingones =  currSE.value2;
  }

  memset(levarr, 0, max_coeff_num * sizeof(int));
  memset(runarr, 0, max_coeff_num * sizeof(int));

  numones = numtrailingones;
  *number_coefficients = numcoeff;

  if (numcoeff)
  {
    if (numtrailingones)
    {      
      currSE.len = numtrailingones;

#if TRACE
      snprintf(currSE.tracestring,
        TRACESTRING_SIZE, "%s trailing ones sign (%d,%d)", type, i, j);
#endif

      readSyntaxElement_FLC (&currSE, currStream);

      code = currSE.inf;
      ntr = numtrailingones;
      for (k = numcoeff - 1; k > numcoeff - 1 - numtrailingones; k--)
      {
        ntr --;
        levarr[k] = (code>>ntr)&1 ? -1 : 1;
      }
    }

    // decode levels
    level_two_or_higher = (numcoeff > 3 && numtrailingones == 3)? 0 : 1;
    vlcnum = (numcoeff > 10 && numtrailingones < 3) ? 1 : 0;

    for (k = numcoeff - 1 - numtrailingones; k >= 0; k--)
    {

#if TRACE
      snprintf(currSE.tracestring,
        TRACESTRING_SIZE, "%s lev (%d,%d) k=%d vlc=%d ", type, i, j, k, vlcnum);
#endif

      if (vlcnum == 0)
        readSyntaxElement_Level_VLC0(&currSE, currStream);
      else
        readSyntaxElement_Level_VLCN(&currSE, vlcnum, currStream);

      if (level_two_or_higher)
      {
        currSE.inf += (currSE.inf > 0) ? 1 : -1;
        level_two_or_higher = 0;
      }

      levarr[k] = currSE.inf;
      abslevel = iabs(levarr[k]);
      if (abslevel  == 1)
        ++numones;

      // update VLC table
      if (abslevel  > incVlc[vlcnum])
        ++vlcnum;

      if (k == numcoeff - 1 - numtrailingones && abslevel >3)
        vlcnum = 2;      
    }

    if (numcoeff < max_coeff_num)
    {
      // decode total run
      vlcnum = numcoeff - 1;
      currSE.value1 = vlcnum;

#if TRACE
      snprintf(currSE.tracestring,
        TRACESTRING_SIZE, "%s totalrun (%d,%d) vlc=%d ", type, i,j, vlcnum);
#endif
      if (cdc)
        readSyntaxElement_TotalZerosChromaDC(p_Vid, &currSE, currStream);
      else
        readSyntaxElement_TotalZeros(&currSE, currStream);

      totzeros = currSE.value1;
    }
    else
    {
      totzeros = 0;
    }

    // decode run before each coefficient
    zerosleft = totzeros;
    i = numcoeff - 1;

    if (zerosleft > 0 && i > 0)
    {
      do
      {
        // select VLC for runbefore
        vlcnum = imin(zerosleft - 1, RUNBEFORE_NUM_M1);

        currSE.value1 = vlcnum;
#if TRACE
        snprintf(currSE.tracestring,
          TRACESTRING_SIZE, "%s run (%d,%d) k=%d vlc=%d ",
          type, i, j, i, vlcnum);
#endif

        readSyntaxElement_Run(&currSE, currStream);
        runarr[i] = currSE.value1;

        zerosleft -= runarr[i];
        i --;
      } while (zerosleft != 0 && i != 0);
    }
    runarr[i] = zerosleft;    
  } // if numcoeff
}

/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of 4x4 blocks in a MB
*    from the NAL (CABAC Mode)
************************************************************************
*/
static void read_comp_coeff_4x4_CAVLC (Macroblock *currMB, ColorPlane pl, int (*InvLevelScale4x4)[4], int qp_per, int cbp, byte **nzcoeff)
{
  int block_y, block_x, b8;
  int i, j, k;
  int i0, j0;
  int levarr[16] = {0}, runarr[16] = {0}, numcoeff;
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  const byte (*pos_scan4x4)[2] = ((p_Vid->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];
  int start_scan = IS_I16MB(currMB) ? 1 : 0;
  int64 *cur_cbp = &currMB->s_cbp[pl].blk;
  int cur_context; 
  int block_y4, block_x4;

  if (IS_I16MB(currMB))
  {
    if (pl == PLANE_Y)
      cur_context = LUMA_INTRA16x16AC;
    else if (pl == PLANE_U)
      cur_context = CB_INTRA16x16AC;
    else
      cur_context = CR_INTRA16x16AC;
  }
  else
  {
    if (pl == PLANE_Y)
      cur_context = LUMA;
    else if (pl == PLANE_U)
      cur_context = CB;
    else
      cur_context = CR;
  }


  for (block_y = 0; block_y < 4; block_y += 2) /* all modes */
  {
    block_y4 = block_y << 2;
    for (block_x = 0; block_x < 4; block_x += 2)
    {
      block_x4 = block_x << 2;
      b8 = (block_y + (block_x >> 1));

      if (cbp & (1 << b8))  // test if the block contains any coefficients
      {
        for (j = block_y4; j < block_y4 + 8; j += BLOCK_SIZE)
        {
          for (i = block_x4; i < block_x4 + 8; i += BLOCK_SIZE)
          {
            currSlice->read_coeff_4x4_CAVLC(currMB, cur_context, i >> 2, j >> 2, levarr, runarr, &numcoeff);
            pos_scan_4x4 = pos_scan4x4[start_scan];

            for (k = 0; k < numcoeff; ++k)
            {
              if (levarr[k] != 0)
              {
                pos_scan_4x4 += (runarr[k] << 1);

                i0 = *pos_scan_4x4++;
                j0 = *pos_scan_4x4++;

                // inverse quant for 4x4 transform only
                *cur_cbp |= i64_power2(j + (i >> 2));

                currSlice->cof[pl][j + j0][i + i0]= rshift_rnd_sf((levarr[k] * InvLevelScale4x4[j0][i0])<<qp_per, 4);
                //currSlice->fcf[pl][j + j0][i + i0]= levarr[k];
              }
            }
          }
        }
      }
      else
      {
        nzcoeff[block_y    ][block_x    ] = 0;
        nzcoeff[block_y    ][block_x + 1] = 0;
        nzcoeff[block_y + 1][block_x    ] = 0;
        nzcoeff[block_y + 1][block_x + 1] = 0;
      }
    }
  }      
}

/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of 4x4 blocks in a MB
*    from the NAL (CAVLC Lossless Mode)
************************************************************************
*/
static void read_comp_coeff_4x4_CAVLC_ls (Macroblock *currMB, ColorPlane pl, int (*InvLevelScale4x4)[4], int qp_per, int cbp, byte **nzcoeff)
{
  int block_y, block_x, b8;
  int i, j, k;
  int i0, j0;
  int levarr[16] = {0}, runarr[16] = {0}, numcoeff;
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  const byte (*pos_scan4x4)[2] = ((p_Vid->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  int start_scan = IS_I16MB(currMB) ? 1 : 0;
  int64 *cur_cbp = &currMB->s_cbp[pl].blk;
  int coef_ctr, cur_context; 

  if (IS_I16MB(currMB))
  {
    if (pl == PLANE_Y)
      cur_context = LUMA_INTRA16x16AC;
    else if (pl == PLANE_U)
      cur_context = CB_INTRA16x16AC;
    else
      cur_context = CR_INTRA16x16AC;
  }
  else
  {
    if (pl == PLANE_Y)
      cur_context = LUMA;
    else if (pl == PLANE_U)
      cur_context = CB;
    else
      cur_context = CR;
  }

  for (block_y=0; block_y < 4; block_y += 2) /* all modes */
  {
    for (block_x=0; block_x < 4; block_x += 2)
    {
      b8 = 2*(block_y>>1) + (block_x>>1);

      if (cbp & (1<<b8))  /* are there any coeff in current block at all */
      {
        for (j=block_y; j < block_y+2; ++j)
        {
          for (i=block_x; i < block_x+2; ++i)
          {
            currSlice->read_coeff_4x4_CAVLC(currMB, cur_context, i, j, levarr, runarr, &numcoeff);

            coef_ctr = start_scan - 1;

            for (k = 0; k < numcoeff; ++k)
            {
              if (levarr[k] != 0)
              {
                coef_ctr += runarr[k]+1;

                i0=pos_scan4x4[coef_ctr][0];
                j0=pos_scan4x4[coef_ctr][1];

                *cur_cbp |= i64_power2((j<<2) + i);
                currSlice->cof[pl][(j<<2) + j0][(i<<2) + i0]= levarr[k];
                //currSlice->fcf[pl][(j<<2) + j0][(i<<2) + i0]= levarr[k];
              }
            }
          }
        }
      }
      else
      {
        nzcoeff[block_y    ][block_x    ] = 0;
        nzcoeff[block_y    ][block_x + 1] = 0;
        nzcoeff[block_y + 1][block_x    ] = 0;
        nzcoeff[block_y + 1][block_x + 1] = 0;
      }
    }
  }    
}

/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of 4x4 blocks in a MB
*    from the NAL (CABAC Mode)
************************************************************************
*/
static void read_comp_coeff_8x8_CAVLC (Macroblock *currMB, ColorPlane pl, int (*InvLevelScale8x8)[8], int qp_per, int cbp, byte **nzcoeff)
{
  int block_y, block_x, b4, b8;
  int block_y4, block_x4;
  int i, j, k;
  int i0, j0;
  int levarr[16] = {0}, runarr[16] = {0}, numcoeff;
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  const byte (*pos_scan8x8)[2] = ((p_Vid->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN8x8 : FIELD_SCAN8x8;
  int start_scan = IS_I16MB(currMB) ? 1 : 0;
  int64 *cur_cbp = &currMB->s_cbp[pl].blk;
  int coef_ctr, cur_context; 

  if (IS_I16MB(currMB))
  {
    if (pl == PLANE_Y)
      cur_context = LUMA_INTRA16x16AC;
    else if (pl == PLANE_U)
      cur_context = CB_INTRA16x16AC;
    else
      cur_context = CR_INTRA16x16AC;
  }
  else
  {
    if (pl == PLANE_Y)
      cur_context = LUMA;
    else if (pl == PLANE_U)
      cur_context = CB;
    else
      cur_context = CR;
  }

  for (block_y = 0; block_y < 4; block_y += 2) /* all modes */
  {
    block_y4 = block_y << 2;

    for (block_x = 0; block_x < 4; block_x += 2)
    {
      block_x4 = block_x << 2;
      b8 = block_y + (block_x>>1);

      if (cbp & (1<<b8))  /* are there any coeff in current block at all */
      {
        for (j = block_y; j < block_y + 2; ++j)
        {
          for (i = block_x; i < block_x + 2; ++i)
          {
            currSlice->read_coeff_4x4_CAVLC(currMB, cur_context, i, j, levarr, runarr, &numcoeff);

            coef_ctr = start_scan - 1;

            for (k = 0; k < numcoeff; ++k)
            {
              if (levarr[k] != 0)
              {
                coef_ctr += runarr[k] + 1;

                // do same as CABAC for deblocking: any coeff in the 8x8 marks all the 4x4s
                //as containing coefficients
                *cur_cbp |= 51 << (block_y4 + block_x);

                b4 = (coef_ctr << 2) + 2*(j - block_y) + (i - block_x);

                i0 = pos_scan8x8[b4][0];
                j0 = pos_scan8x8[b4][1];

                currSlice->mb_rres[pl][block_y4 +j0][block_x4 +i0] = rshift_rnd_sf((levarr[k] * InvLevelScale8x8[j0][i0])<<qp_per, 6); // dequantization
              }
            }//else (!currMB->luma_transform_size_8x8_flag)
          }
        }
      }
      else
      {
        nzcoeff[block_y    ][block_x    ] = 0;
        nzcoeff[block_y    ][block_x + 1] = 0;
        nzcoeff[block_y + 1][block_x    ] = 0;
        nzcoeff[block_y + 1][block_x + 1] = 0;
      }
    }
  }   
}

/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of 8x8 blocks in a MB
*    from the NAL (CAVLC Lossless Mode)
************************************************************************
*/
static void read_comp_coeff_8x8_CAVLC_ls (Macroblock *currMB, ColorPlane pl, int (*InvLevelScale8x8)[8], int qp_per, int cbp, byte **nzcoeff)
{
  int block_y, block_x, b4, b8;
  int i, j, k;
  int levarr[16] = {0}, runarr[16] = {0}, numcoeff;
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  const byte (*pos_scan8x8)[2] = ((p_Vid->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN8x8 : FIELD_SCAN8x8;
  int start_scan = IS_I16MB(currMB) ? 1 : 0;
  int64 *cur_cbp = &currMB->s_cbp[pl].blk;
  int coef_ctr, cur_context; 

  if (IS_I16MB(currMB))
  {
    if (pl == PLANE_Y)
      cur_context = LUMA_INTRA16x16AC;
    else if (pl == PLANE_U)
      cur_context = CB_INTRA16x16AC;
    else
      cur_context = CR_INTRA16x16AC;
  }
  else
  {
    if (pl == PLANE_Y)
      cur_context = LUMA;
    else if (pl == PLANE_U)
      cur_context = CB;
    else
      cur_context = CR;
  }

  for (block_y=0; block_y < 4; block_y += 2) /* all modes */
  {
    for (block_x=0; block_x < 4; block_x += 2)
    {
      b8 = 2*(block_y>>1) + (block_x>>1);

      if (cbp & (1<<b8))  /* are there any coeff in current block at all */
      {
        int iz, jz;

        for (j=block_y; j < block_y+2; ++j)
        {
          for (i=block_x; i < block_x+2; ++i)
          {

            currSlice->read_coeff_4x4_CAVLC(currMB, cur_context, i, j, levarr, runarr, &numcoeff);

            coef_ctr = start_scan - 1;

            for (k = 0; k < numcoeff; ++k)
            {
              if (levarr[k] != 0)
              {
                coef_ctr += runarr[k]+1;

                // do same as CABAC for deblocking: any coeff in the 8x8 marks all the 4x4s
                //as containing coefficients
                *cur_cbp  |= 51 << ((block_y<<2) + block_x);

                b4 = 2*(j-block_y)+(i-block_x);

                iz=pos_scan8x8[coef_ctr*4+b4][0];
                jz=pos_scan8x8[coef_ctr*4+b4][1];

                currSlice->mb_rres[pl][block_y*4 +jz][block_x*4 +iz] = levarr[k];
              }
            }
          }
        }
      }
      else
      {
        nzcoeff[block_y    ][block_x    ] = 0;
        nzcoeff[block_y    ][block_x + 1] = 0;
        nzcoeff[block_y + 1][block_x    ] = 0;
        nzcoeff[block_y + 1][block_x + 1] = 0;
      }
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Get coded block pattern and coefficients (run/level)
 *    from the NAL
 ************************************************************************
 */
static void read_CBP_and_coeffs_from_NAL_CAVLC_400(Macroblock *currMB)
{
  int k;
  int mb_nr = currMB->mbAddrX;
  int cbp;
  SyntaxElement currSE;
  DataPartition *dP = NULL;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  int i0, j0;

  int levarr[16], runarr[16], numcoeff;

  int qp_per, qp_rem;
  VideoParameters *p_Vid = currMB->p_Vid;

  int intra = (currMB->is_intra_block == TRUE);

  int need_transform_size_flag;

  int (*InvLevelScale4x4)[4] = NULL;
  int (*InvLevelScale8x8)[8] = NULL;
  // select scan type
  const byte (*pos_scan4x4)[2] = ((p_Vid->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];


  // read CBP if not new intra mode
  if (!IS_I16MB (currMB))
  {
    //=====   C B P   =====
    //---------------------
    currSE.type = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB) 
      ? SE_CBP_INTRA
      : SE_CBP_INTER;

    dP = &(currSlice->partArr[partMap[currSE.type]]);

    currSE.mapping = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB)
      ? currSlice->linfo_cbp_intra
      : currSlice->linfo_cbp_inter;

    TRACE_STRING("coded_block_pattern");
    dP->readSyntaxElement(currMB, &currSE, dP);
    currMB->cbp = cbp = currSE.value1;


    //============= Transform size flag for INTER MBs =============
    //-------------------------------------------------------------
    need_transform_size_flag = (((currMB->mb_type >= 1 && currMB->mb_type <= 3)||
      (IS_DIRECT(currMB) && p_Vid->active_sps->direct_8x8_inference_flag) ||
      (currMB->NoMbPartLessThan8x8Flag))
      && currMB->mb_type != I8MB && currMB->mb_type != I4MB
      && (currMB->cbp&15)
      && currSlice->Transform8x8Mode);

    if (need_transform_size_flag)
    {
      currSE.type   =  SE_HEADER;
      dP = &(currSlice->partArr[partMap[SE_HEADER]]);
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      currSE.len = 1;
      readSyntaxElement_FLC(&currSE, dP->bitstream);

      currMB->luma_transform_size_8x8_flag = (Boolean) currSE.value1;
    }

    //=====   DQUANT   =====
    //----------------------
    // Delta quant only if nonzero coeffs
    if (cbp !=0)
    {
      read_delta_quant(&currSE, dP, currMB, partMap, ((currMB->is_intra_block == FALSE)) ? SE_DELTA_QUANT_INTER : SE_DELTA_QUANT_INTRA);

      if (currSlice->dp_mode)
      {
        if ((currMB->is_intra_block == FALSE) && currSlice->dpC_NotPresent ) 
          currMB->dpl_flag = 1;

        if( intra && currSlice->dpB_NotPresent )
        {
          currMB->ei_flag = 1;
          currMB->dpl_flag = 1;
        }

        // check for prediction from neighbours
        check_dp_neighbors (currMB);
        if (currMB->dpl_flag)
        {
          cbp = 0; 
          currMB->cbp = cbp;
        }
      }
    }
  }
  else  // read DC coeffs for new intra modes
  {
    cbp = currMB->cbp;
  
    read_delta_quant(&currSE, dP, currMB, partMap, SE_DELTA_QUANT_INTRA);

    if (currSlice->dp_mode)
    {  
      if (currSlice->dpB_NotPresent)
      {
        currMB->ei_flag  = 1;
        currMB->dpl_flag = 1;
      }
      check_dp_neighbors (currMB);
      if (currMB->dpl_flag)
      {
        currMB->cbp = cbp = 0; 
      }
    }

    if (!currMB->dpl_flag)
    {
      pos_scan_4x4 = pos_scan4x4[0];

      currSlice->read_coeff_4x4_CAVLC(currMB, LUMA_INTRA16x16DC, 0, 0, levarr, runarr, &numcoeff);

      for(k = 0; k < numcoeff; ++k)
      {
        if (levarr[k] != 0)                     // leave if level == 0
        {
          pos_scan_4x4 += 2 * runarr[k];

          i0 = ((*pos_scan_4x4++) << 2);
          j0 = ((*pos_scan_4x4++) << 2);

          currSlice->cof[0][j0][i0] = levarr[k];// add new intra DC coeff
          //currSlice->fcf[0][j0][i0] = levarr[k];// add new intra DC coeff
        }
      }


      if(currMB->is_lossless == FALSE)
        itrans_2(currMB, (ColorPlane) currSlice->colour_plane_id);// transform new intra DC
    }
  }

  update_qp(currMB, currSlice->qp);

  qp_per = p_Vid->qp_per_matrix[ currMB->qp_scaled[PLANE_Y] ];
  qp_rem = p_Vid->qp_rem_matrix[ currMB->qp_scaled[PLANE_Y] ];

  InvLevelScale4x4 = intra? currSlice->InvLevelScale4x4_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale4x4_Inter[currSlice->colour_plane_id][qp_rem];
  InvLevelScale8x8 = intra? currSlice->InvLevelScale8x8_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale8x8_Inter[currSlice->colour_plane_id][qp_rem];

  // luma coefficients
  if (cbp)
  {
    if (!currMB->luma_transform_size_8x8_flag) // 4x4 transform
    {
      currMB->read_comp_coeff_4x4_CAVLC (currMB, PLANE_Y, InvLevelScale4x4, qp_per, cbp, p_Vid->nz_coeff[mb_nr][PLANE_Y]);
    }
    else // 8x8 transform
    {
      currMB->read_comp_coeff_8x8_CAVLC (currMB, PLANE_Y, InvLevelScale8x8, qp_per, cbp, p_Vid->nz_coeff[mb_nr][PLANE_Y]);
    }
  }
  else
  {
    fast_memset(p_Vid->nz_coeff[mb_nr][0][0], 0, BLOCK_PIXELS * sizeof(byte));
  }
}

/*!
 ************************************************************************
 * \brief
 *    Get coded block pattern and coefficients (run/level)
 *    from the NAL
 ************************************************************************
 */
static void read_CBP_and_coeffs_from_NAL_CAVLC_422(Macroblock *currMB)
{
  int i,j,k;
  int mb_nr = currMB->mbAddrX;
  int cbp;
  SyntaxElement currSE;
  DataPartition *dP = NULL;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  int coef_ctr, i0, j0, b8;
  int ll;
  int levarr[16], runarr[16], numcoeff;

  int qp_per, qp_rem;
  VideoParameters *p_Vid = currMB->p_Vid;

  int uv; 
  int qp_per_uv[2];
  int qp_rem_uv[2];

  int intra = (currMB->is_intra_block == TRUE);

  int b4;
  //StorablePicture *dec_picture = currSlice->dec_picture;
  int m6[4];

  int need_transform_size_flag;

  int (*InvLevelScale4x4)[4] = NULL;
  int (*InvLevelScale8x8)[8] = NULL;
  // select scan type
  const byte (*pos_scan4x4)[2] = ((p_Vid->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];


  // read CBP if not new intra mode
  if (!IS_I16MB (currMB))
  {
    //=====   C B P   =====
    //---------------------
    currSE.type = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB) 
      ? SE_CBP_INTRA
      : SE_CBP_INTER;

    dP = &(currSlice->partArr[partMap[currSE.type]]);

    currSE.mapping = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB)
      ? currSlice->linfo_cbp_intra
      : currSlice->linfo_cbp_inter;

    TRACE_STRING("coded_block_pattern");
    dP->readSyntaxElement(currMB, &currSE, dP);
    currMB->cbp = cbp = currSE.value1;


    //============= Transform size flag for INTER MBs =============
    //-------------------------------------------------------------
    need_transform_size_flag = (((currMB->mb_type >= 1 && currMB->mb_type <= 3)||
      (IS_DIRECT(currMB) && p_Vid->active_sps->direct_8x8_inference_flag) ||
      (currMB->NoMbPartLessThan8x8Flag))
      && currMB->mb_type != I8MB && currMB->mb_type != I4MB
      && (currMB->cbp&15)
      && currSlice->Transform8x8Mode);

    if (need_transform_size_flag)
    {
      currSE.type   =  SE_HEADER;
      dP = &(currSlice->partArr[partMap[SE_HEADER]]);
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      currSE.len = 1;
      readSyntaxElement_FLC(&currSE, dP->bitstream);

      currMB->luma_transform_size_8x8_flag = (Boolean) currSE.value1;
    }

    //=====   DQUANT   =====
    //----------------------
    // Delta quant only if nonzero coeffs
    if (cbp !=0)
    {
      read_delta_quant(&currSE, dP, currMB, partMap, ((currMB->is_intra_block == FALSE)) ? SE_DELTA_QUANT_INTER : SE_DELTA_QUANT_INTRA);

      if (currSlice->dp_mode)
      {
        if ((currMB->is_intra_block == FALSE) && currSlice->dpC_NotPresent ) 
          currMB->dpl_flag = 1;

        if( intra && currSlice->dpB_NotPresent )
        {
          currMB->ei_flag = 1;
          currMB->dpl_flag = 1;
        }

        // check for prediction from neighbours
        check_dp_neighbors (currMB);
        if (currMB->dpl_flag)
        {
          cbp = 0; 
          currMB->cbp = cbp;
        }
      }
    }
  }
  else  // read DC coeffs for new intra modes
  {
    cbp = currMB->cbp;

    read_delta_quant(&currSE, dP, currMB, partMap, SE_DELTA_QUANT_INTRA);

    if (currSlice->dp_mode)
    {  
      if (currSlice->dpB_NotPresent)
      {
        currMB->ei_flag  = 1;
        currMB->dpl_flag = 1;
      }
      check_dp_neighbors (currMB);
      if (currMB->dpl_flag)
      {
        currMB->cbp = cbp = 0; 
      }
    }

    if (!currMB->dpl_flag)
    {
      pos_scan_4x4 = pos_scan4x4[0];

      currSlice->read_coeff_4x4_CAVLC(currMB, LUMA_INTRA16x16DC, 0, 0, levarr, runarr, &numcoeff);

      for(k = 0; k < numcoeff; ++k)
      {
        if (levarr[k] != 0)                     // leave if level == 0
        {
          pos_scan_4x4 += 2 * runarr[k];

          i0 = ((*pos_scan_4x4++) << 2);
          j0 = ((*pos_scan_4x4++) << 2);

          currSlice->cof[0][j0][i0] = levarr[k];// add new intra DC coeff
          //currSlice->fcf[0][j0][i0] = levarr[k];// add new intra DC coeff
        }
      }


      if(currMB->is_lossless == FALSE)
        itrans_2(currMB, (ColorPlane) currSlice->colour_plane_id);// transform new intra DC
    }
  }

  update_qp(currMB, currSlice->qp);

  qp_per = p_Vid->qp_per_matrix[ currMB->qp_scaled[currSlice->colour_plane_id] ];
  qp_rem = p_Vid->qp_rem_matrix[ currMB->qp_scaled[currSlice->colour_plane_id] ];

  //init quant parameters for chroma 
  for(i=0; i < 2; ++i)
  {
    qp_per_uv[i] = p_Vid->qp_per_matrix[ currMB->qp_scaled[i + 1] ];
    qp_rem_uv[i] = p_Vid->qp_rem_matrix[ currMB->qp_scaled[i + 1] ];
  }

  InvLevelScale4x4 = intra? currSlice->InvLevelScale4x4_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale4x4_Inter[currSlice->colour_plane_id][qp_rem];
  InvLevelScale8x8 = intra? currSlice->InvLevelScale8x8_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale8x8_Inter[currSlice->colour_plane_id][qp_rem];

  // luma coefficients
  if (cbp)
  {
    if (!currMB->luma_transform_size_8x8_flag) // 4x4 transform
    {
      currMB->read_comp_coeff_4x4_CAVLC (currMB, PLANE_Y, InvLevelScale4x4, qp_per, cbp, p_Vid->nz_coeff[mb_nr][PLANE_Y]);
    }
    else // 8x8 transform
    {
      currMB->read_comp_coeff_8x8_CAVLC (currMB, PLANE_Y, InvLevelScale8x8, qp_per, cbp, p_Vid->nz_coeff[mb_nr][PLANE_Y]);
    }
  }
  else
  {
    fast_memset(p_Vid->nz_coeff[mb_nr][0][0], 0, BLOCK_PIXELS * sizeof(byte));
  }

  //========================== CHROMA DC ============================
  //-----------------------------------------------------------------
  // chroma DC coeff
  if(cbp>15)
  {    
    for (ll=0;ll<3;ll+=2)
    {
      int (*InvLevelScale4x4)[4] = NULL;
      uv = ll>>1;
      {
        int **imgcof = currSlice->cof[PLANE_U + uv];
        int m3[2][4] = {{0,0,0,0},{0,0,0,0}};
        int m4[2][4] = {{0,0,0,0},{0,0,0,0}};
        int qp_per_uv_dc = p_Vid->qp_per_matrix[ (currMB->qpc[uv] + 3 + p_Vid->bitdepth_chroma_qp_scale) ];       //for YUV422 only
        int qp_rem_uv_dc = p_Vid->qp_rem_matrix[ (currMB->qpc[uv] + 3 + p_Vid->bitdepth_chroma_qp_scale) ];       //for YUV422 only
        if (intra)
          InvLevelScale4x4 = currSlice->InvLevelScale4x4_Intra[PLANE_U + uv][qp_rem_uv_dc];
        else 
          InvLevelScale4x4 = currSlice->InvLevelScale4x4_Inter[PLANE_U + uv][qp_rem_uv_dc];


        //===================== CHROMA DC YUV422 ======================
        currSlice->read_coeff_4x4_CAVLC(currMB, CHROMA_DC, 0, 0, levarr, runarr, &numcoeff);
        coef_ctr=-1;
        for(k = 0; k < numcoeff; ++k)
        {
          if (levarr[k] != 0)
          {
            currMB->s_cbp[0].blk |= ((int64)0xff0000) << (ll<<2);
            coef_ctr += runarr[k]+1;
            i0 = SCAN_YUV422[coef_ctr][0];
            j0 = SCAN_YUV422[coef_ctr][1];

            m3[i0][j0]=levarr[k];
          }
        }

        // inverse CHROMA DC YUV422 transform
        // horizontal
        if(currMB->is_lossless == FALSE)
        {
          m4[0][0] = m3[0][0] + m3[1][0];
          m4[0][1] = m3[0][1] + m3[1][1];
          m4[0][2] = m3[0][2] + m3[1][2];
          m4[0][3] = m3[0][3] + m3[1][3];

          m4[1][0] = m3[0][0] - m3[1][0];
          m4[1][1] = m3[0][1] - m3[1][1];
          m4[1][2] = m3[0][2] - m3[1][2];
          m4[1][3] = m3[0][3] - m3[1][3];

          for (i = 0; i < 2; ++i)
          {
            m6[0] = m4[i][0] + m4[i][2];
            m6[1] = m4[i][0] - m4[i][2];
            m6[2] = m4[i][1] - m4[i][3];
            m6[3] = m4[i][1] + m4[i][3];

            imgcof[ 0][i<<2] = m6[0] + m6[3];
            imgcof[ 4][i<<2] = m6[1] + m6[2];
            imgcof[ 8][i<<2] = m6[1] - m6[2];
            imgcof[12][i<<2] = m6[0] - m6[3];
          }//for (i=0;i<2;++i)

          for(j = 0;j < p_Vid->mb_cr_size_y; j += BLOCK_SIZE)
          {
            for(i=0;i < p_Vid->mb_cr_size_x;i+=BLOCK_SIZE)
            {
              imgcof[j][i] = rshift_rnd_sf((imgcof[j][i] * InvLevelScale4x4[0][0]) << qp_per_uv_dc, 6);
            }
          }
        }
        else
        {
          for(j=0;j<4;++j)
          {
            currSlice->cof[PLANE_U + uv][j<<2][0] = m3[0][j];
            currSlice->cof[PLANE_U + uv][j<<2][4] = m3[1][j];
          }
        }

      }
    }//for (ll=0;ll<3;ll+=2)    
  }

  //========================== CHROMA AC ============================
  //-----------------------------------------------------------------
  // chroma AC coeff, all zero fram start_scan
  if (cbp<=31)
  {
    fast_memset(p_Vid->nz_coeff [mb_nr ][1][0], 0, 2 * BLOCK_PIXELS * sizeof(byte));
  }
  else
  {
    if(currMB->is_lossless == FALSE)
    {
      for (b8=0; b8 < p_Vid->num_blk8x8_uv; ++b8)
      {
        currMB->is_v_block = uv = (b8 > ((p_Vid->num_uv_blocks) - 1 ));
        InvLevelScale4x4 = intra ? currSlice->InvLevelScale4x4_Intra[PLANE_U + uv][qp_rem_uv[uv]] : currSlice->InvLevelScale4x4_Inter[PLANE_U + uv][qp_rem_uv[uv]];

        for (b4=0; b4 < 4; ++b4)
        {
          i = cofuv_blk_x[1][b8][b4];
          j = cofuv_blk_y[1][b8][b4];

          currSlice->read_coeff_4x4_CAVLC(currMB, CHROMA_AC, i + 2*uv, j + 4, levarr, runarr, &numcoeff);
          coef_ctr = 0;

          for(k = 0; k < numcoeff;++k)
          {
            if (levarr[k] != 0)
            {
              currMB->s_cbp[0].blk |= i64_power2(cbp_blk_chroma[b8][b4]);
              coef_ctr += runarr[k] + 1;

              i0=pos_scan4x4[coef_ctr][0];
              j0=pos_scan4x4[coef_ctr][1];

              currSlice->cof[PLANE_U + uv][(j<<2) + j0][(i<<2) + i0] = rshift_rnd_sf((levarr[k] * InvLevelScale4x4[j0][i0])<<qp_per_uv[uv], 4);
              //currSlice->fcf[PLANE_U + uv][(j<<2) + j0][(i<<2) + i0] = levarr[k];
            }
          }
        }
      }        
    }
    else
    {
      for (b8=0; b8 < p_Vid->num_blk8x8_uv; ++b8)
      {
        currMB->is_v_block = uv = (b8 > ((p_Vid->num_uv_blocks) - 1 ));

        for (b4=0; b4 < 4; ++b4)
        {
          i = cofuv_blk_x[1][b8][b4];
          j = cofuv_blk_y[1][b8][b4];

          currSlice->read_coeff_4x4_CAVLC(currMB, CHROMA_AC, i + 2*uv, j + 4, levarr, runarr, &numcoeff);
          coef_ctr = 0;

          for(k = 0; k < numcoeff;++k)
          {
            if (levarr[k] != 0)
            {
              currMB->s_cbp[0].blk |= i64_power2(cbp_blk_chroma[b8][b4]);
              coef_ctr += runarr[k] + 1;

              i0=pos_scan4x4[coef_ctr][0];
              j0=pos_scan4x4[coef_ctr][1];

              currSlice->cof[PLANE_U + uv][(j<<2) + j0][(i<<2) + i0] = levarr[k];
            }
          }
        }
      }        
    }
  } //if (dec_picture->chroma_format_idc != YUV400)
}

/*!
 ************************************************************************
 * \brief
 *    Get coded block pattern and coefficients (run/level)
 *    from the NAL
 ************************************************************************
 */
static void read_CBP_and_coeffs_from_NAL_CAVLC_444(Macroblock *currMB)
{
  int i,k;
  int mb_nr = currMB->mbAddrX;
  int cbp;
  SyntaxElement currSE;
  DataPartition *dP = NULL;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  int coef_ctr, i0, j0;
  int levarr[16], runarr[16], numcoeff;

  int qp_per, qp_rem;
  VideoParameters *p_Vid = currMB->p_Vid;

  int uv; 
  int qp_per_uv[3];
  int qp_rem_uv[3];

  int intra = (currMB->is_intra_block == TRUE);

  int need_transform_size_flag;

  int (*InvLevelScale4x4)[4] = NULL;
  int (*InvLevelScale8x8)[8] = NULL;
  // select scan type
  const byte (*pos_scan4x4)[2] = ((p_Vid->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];

  // read CBP if not new intra mode
  if (!IS_I16MB (currMB))
  {
    //=====   C B P   =====
    //---------------------
    currSE.type = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB) 
      ? SE_CBP_INTRA
      : SE_CBP_INTER;

    dP = &(currSlice->partArr[partMap[currSE.type]]);

    currSE.mapping = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB)
      ? currSlice->linfo_cbp_intra
      : currSlice->linfo_cbp_inter;

    TRACE_STRING("coded_block_pattern");
    dP->readSyntaxElement(currMB, &currSE, dP);
    currMB->cbp = cbp = currSE.value1;


    //============= Transform size flag for INTER MBs =============
    //-------------------------------------------------------------
    need_transform_size_flag = (((currMB->mb_type >= 1 && currMB->mb_type <= 3)||
      (IS_DIRECT(currMB) && p_Vid->active_sps->direct_8x8_inference_flag) ||
      (currMB->NoMbPartLessThan8x8Flag))
      && currMB->mb_type != I8MB && currMB->mb_type != I4MB
      && (currMB->cbp&15)
      && currSlice->Transform8x8Mode);

    if (need_transform_size_flag)
    {
      currSE.type   =  SE_HEADER;
      dP = &(currSlice->partArr[partMap[SE_HEADER]]);
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      currSE.len = 1;
      readSyntaxElement_FLC(&currSE, dP->bitstream);

      currMB->luma_transform_size_8x8_flag = (Boolean) currSE.value1;
    }

    //=====   DQUANT   =====
    //----------------------
    // Delta quant only if nonzero coeffs
    if (cbp !=0)
    {
      read_delta_quant(&currSE, dP, currMB, partMap, ((currMB->is_intra_block == FALSE)) ? SE_DELTA_QUANT_INTER : SE_DELTA_QUANT_INTRA);

      if (currSlice->dp_mode)
      {
        if ((currMB->is_intra_block == FALSE) && currSlice->dpC_NotPresent ) 
          currMB->dpl_flag = 1;

        if( intra && currSlice->dpB_NotPresent )
        {
          currMB->ei_flag = 1;
          currMB->dpl_flag = 1;
        }

        // check for prediction from neighbours
        check_dp_neighbors (currMB);
        if (currMB->dpl_flag)
        {
          cbp = 0; 
          currMB->cbp = cbp;
        }
      }
    }
  }
  else  // read DC coeffs for new intra modes
  {
    cbp = currMB->cbp;

    read_delta_quant(&currSE, dP, currMB, partMap, SE_DELTA_QUANT_INTRA);

    if (currSlice->dp_mode)
    {  
      if (currSlice->dpB_NotPresent)
      {
        currMB->ei_flag  = 1;
        currMB->dpl_flag = 1;
      }
      check_dp_neighbors (currMB);
      if (currMB->dpl_flag)
      {
        currMB->cbp = cbp = 0; 
      }
    }

    if (!currMB->dpl_flag)
    {
      pos_scan_4x4 = pos_scan4x4[0];

      currSlice->read_coeff_4x4_CAVLC(currMB, LUMA_INTRA16x16DC, 0, 0, levarr, runarr, &numcoeff);

      for(k = 0; k < numcoeff; ++k)
      {
        if (levarr[k] != 0)                     // leave if level == 0
        {
          pos_scan_4x4 += 2 * runarr[k];

          i0 = ((*pos_scan_4x4++) << 2);
          j0 = ((*pos_scan_4x4++) << 2);

          currSlice->cof[0][j0][i0] = levarr[k];// add new intra DC coeff
          //currSlice->fcf[0][j0][i0] = levarr[k];// add new intra DC coeff
        }
      }


      if(currMB->is_lossless == FALSE)
        itrans_2(currMB, (ColorPlane) currSlice->colour_plane_id);// transform new intra DC
    }
  }

  update_qp(currMB, currSlice->qp);

  qp_per = p_Vid->qp_per_matrix[ currMB->qp_scaled[currSlice->colour_plane_id] ];
  qp_rem = p_Vid->qp_rem_matrix[ currMB->qp_scaled[currSlice->colour_plane_id] ];

  //init quant parameters for chroma 
  for(i=PLANE_U; i <= PLANE_V; ++i)
  {
    qp_per_uv[i] = p_Vid->qp_per_matrix[ currMB->qp_scaled[i] ];
    qp_rem_uv[i] = p_Vid->qp_rem_matrix[ currMB->qp_scaled[i] ];
  }

  InvLevelScale4x4 = intra? currSlice->InvLevelScale4x4_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale4x4_Inter[currSlice->colour_plane_id][qp_rem];
  InvLevelScale8x8 = intra? currSlice->InvLevelScale8x8_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale8x8_Inter[currSlice->colour_plane_id][qp_rem];

  // luma coefficients
  if (cbp)
  {
    if (!currMB->luma_transform_size_8x8_flag) // 4x4 transform
    {
      currMB->read_comp_coeff_4x4_CAVLC (currMB, PLANE_Y, InvLevelScale4x4, qp_per, cbp, p_Vid->nz_coeff[mb_nr][PLANE_Y]);
    }
    else // 8x8 transform
    {
      currMB->read_comp_coeff_8x8_CAVLC (currMB, PLANE_Y, InvLevelScale8x8, qp_per, cbp, p_Vid->nz_coeff[mb_nr][PLANE_Y]);
    }
  }
  else
  {
    fast_memset(p_Vid->nz_coeff[mb_nr][0][0], 0, BLOCK_PIXELS * sizeof(byte));
  }

  for (uv = PLANE_U; uv <= PLANE_V; ++uv )
  {
    /*----------------------16x16DC Luma_Add----------------------*/
    if (IS_I16MB (currMB)) // read DC coeffs for new intra modes       
    {
      if (uv == PLANE_U)
        currSlice->read_coeff_4x4_CAVLC(currMB, CB_INTRA16x16DC, 0, 0, levarr, runarr, &numcoeff);
      else
        currSlice->read_coeff_4x4_CAVLC(currMB, CR_INTRA16x16DC, 0, 0, levarr, runarr, &numcoeff);

      coef_ctr=-1;

      for(k = 0; k < numcoeff; ++k)
      {
        if (levarr[k] != 0)                     // leave if level == 0
        {
          coef_ctr += runarr[k] + 1;

          i0 = pos_scan4x4[coef_ctr][0];
          j0 = pos_scan4x4[coef_ctr][1];
          currSlice->cof[uv][j0<<2][i0<<2] = levarr[k];// add new intra DC coeff
          //currSlice->fcf[uv][j0<<2][i0<<2] = levarr[k];// add new intra DC coeff
        } //if leavarr[k]
      } //k loop

      if(currMB->is_lossless == FALSE)
      {
        itrans_2(currMB, (ColorPlane) (uv)); // transform new intra DC
      }
    } //IS_I16MB

    update_qp(currMB, currSlice->qp);

    //init constants for every chroma qp offset
    qp_per_uv[uv] = p_Vid->qp_per_matrix[ currMB->qp_scaled[uv] ];
    qp_rem_uv[uv] = p_Vid->qp_rem_matrix[ currMB->qp_scaled[uv] ];

    InvLevelScale4x4 = intra? currSlice->InvLevelScale4x4_Intra[uv][qp_rem_uv[uv]] : currSlice->InvLevelScale4x4_Inter[uv][qp_rem_uv[uv]];
    InvLevelScale8x8 = intra? currSlice->InvLevelScale8x8_Intra[uv][qp_rem_uv[uv]] : currSlice->InvLevelScale8x8_Inter[uv][qp_rem_uv[uv]];

    if (!currMB->luma_transform_size_8x8_flag) // 4x4 transform
    {
      currMB->read_comp_coeff_4x4_CAVLC (currMB, (ColorPlane) (uv), InvLevelScale4x4, qp_per_uv[uv], cbp, p_Vid->nz_coeff[mb_nr][uv]);
    }
    else // 8x8 transform
    {
      currMB->read_comp_coeff_8x8_CAVLC (currMB, (ColorPlane) (uv), InvLevelScale8x8, qp_per_uv[uv], cbp, p_Vid->nz_coeff[mb_nr][uv]);
    }   
  }   
}

/*!
 ************************************************************************
 * \brief
 *    Get coded block pattern and coefficients (run/level)
 *    from the NAL
 ************************************************************************
 */
static void read_CBP_and_coeffs_from_NAL_CAVLC_420(Macroblock *currMB)
{
  int i,j,k;
  int mb_nr = currMB->mbAddrX;
  int cbp;
  SyntaxElement currSE;
  DataPartition *dP = NULL;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  int coef_ctr, i0, j0, b8;
  int ll;
  int levarr[16], runarr[16], numcoeff;

  int qp_per, qp_rem;
  VideoParameters *p_Vid = currMB->p_Vid;
  int smb = ((p_Vid->type==SP_SLICE) && (currMB->is_intra_block == FALSE)) || (p_Vid->type == SI_SLICE && currMB->mb_type == SI4MB);

  int uv; 
  int qp_per_uv[2];
  int qp_rem_uv[2];

  int intra = (currMB->is_intra_block == TRUE);
  int temp[4];

  int b4;
  //StorablePicture *dec_picture = currSlice->dec_picture;

  int need_transform_size_flag;

  int (*InvLevelScale4x4)[4] = NULL;
  int (*InvLevelScale8x8)[8] = NULL;
  // select scan type
  const byte (*pos_scan4x4)[2] = ((p_Vid->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];

  // read CBP if not new intra mode
  if (!IS_I16MB (currMB))
  {
    //=====   C B P   =====
    //---------------------
    currSE.type = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB) 
      ? SE_CBP_INTRA
      : SE_CBP_INTER;

    dP = &(currSlice->partArr[partMap[currSE.type]]);

    currSE.mapping = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB)
      ? currSlice->linfo_cbp_intra
      : currSlice->linfo_cbp_inter;

    TRACE_STRING("coded_block_pattern");
    dP->readSyntaxElement(currMB, &currSE, dP);
    currMB->cbp = cbp = currSE.value1;

    //============= Transform size flag for INTER MBs =============
    //-------------------------------------------------------------
    need_transform_size_flag = (((currMB->mb_type >= 1 && currMB->mb_type <= 3)||
      (IS_DIRECT(currMB) && p_Vid->active_sps->direct_8x8_inference_flag) ||
      (currMB->NoMbPartLessThan8x8Flag))
      && currMB->mb_type != I8MB && currMB->mb_type != I4MB
      && (currMB->cbp&15)
      && currSlice->Transform8x8Mode);

    if (need_transform_size_flag)
    {
      currSE.type   =  SE_HEADER;
      dP = &(currSlice->partArr[partMap[SE_HEADER]]);
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      currSE.len = 1;
      readSyntaxElement_FLC(&currSE, dP->bitstream);

      currMB->luma_transform_size_8x8_flag = (Boolean) currSE.value1;
    }

    //=====   DQUANT   =====
    //----------------------
    // Delta quant only if nonzero coeffs
    if (cbp !=0)
    {
      read_delta_quant(&currSE, dP, currMB, partMap, ((currMB->is_intra_block == FALSE)) ? SE_DELTA_QUANT_INTER : SE_DELTA_QUANT_INTRA);

      if (currSlice->dp_mode)
      {
        if ((currMB->is_intra_block == FALSE) && currSlice->dpC_NotPresent ) 
          currMB->dpl_flag = 1;

        if( intra && currSlice->dpB_NotPresent )
        {
          currMB->ei_flag = 1;
          currMB->dpl_flag = 1;
        }

        // check for prediction from neighbours
        check_dp_neighbors (currMB);
        if (currMB->dpl_flag)
        {
          cbp = 0; 
          currMB->cbp = cbp;
        }
      }
    }
  }
  else
  {
    cbp = currMB->cbp;  
    read_delta_quant(&currSE, dP, currMB, partMap, SE_DELTA_QUANT_INTRA);

    if (currSlice->dp_mode)
    {  
      if (currSlice->dpB_NotPresent)
      {
        currMB->ei_flag  = 1;
        currMB->dpl_flag = 1;
      }
      check_dp_neighbors (currMB);
      if (currMB->dpl_flag)
      {
        currMB->cbp = cbp = 0; 
      }
    }

    if (!currMB->dpl_flag)
    {
      pos_scan_4x4 = pos_scan4x4[0];

      currSlice->read_coeff_4x4_CAVLC(currMB, LUMA_INTRA16x16DC, 0, 0, levarr, runarr, &numcoeff);

      for(k = 0; k < numcoeff; ++k)
      {
        if (levarr[k] != 0)                     // leave if level == 0
        {
          pos_scan_4x4 += 2 * runarr[k];

          i0 = ((*pos_scan_4x4++) << 2);
          j0 = ((*pos_scan_4x4++) << 2);

          currSlice->cof[0][j0][i0] = levarr[k];// add new intra DC coeff
          //currSlice->fcf[0][j0][i0] = levarr[k];// add new intra DC coeff
        }
      }


      if(currMB->is_lossless == FALSE)
        itrans_2(currMB, (ColorPlane) currSlice->colour_plane_id);// transform new intra DC
    }
  }

  update_qp(currMB, currSlice->qp);

  qp_per = p_Vid->qp_per_matrix[ currMB->qp_scaled[currSlice->colour_plane_id] ];
  qp_rem = p_Vid->qp_rem_matrix[ currMB->qp_scaled[currSlice->colour_plane_id] ];

  //init quant parameters for chroma 
  for(i=0; i < 2; ++i)
  {
    qp_per_uv[i] = p_Vid->qp_per_matrix[ currMB->qp_scaled[i + 1] ];
    qp_rem_uv[i] = p_Vid->qp_rem_matrix[ currMB->qp_scaled[i + 1] ];
  }

  InvLevelScale4x4 = intra? currSlice->InvLevelScale4x4_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale4x4_Inter[currSlice->colour_plane_id][qp_rem];
  InvLevelScale8x8 = intra? currSlice->InvLevelScale8x8_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale8x8_Inter[currSlice->colour_plane_id][qp_rem];

  // luma coefficients
  if (cbp)
  {
    if (!currMB->luma_transform_size_8x8_flag) // 4x4 transform
    {
      currMB->read_comp_coeff_4x4_CAVLC (currMB, PLANE_Y, InvLevelScale4x4, qp_per, cbp, p_Vid->nz_coeff[mb_nr][PLANE_Y]);
    }
    else // 8x8 transform
    {
      currMB->read_comp_coeff_8x8_CAVLC (currMB, PLANE_Y, InvLevelScale8x8, qp_per, cbp, p_Vid->nz_coeff[mb_nr][PLANE_Y]);
    }
  }
  else
  {
    fast_memset(p_Vid->nz_coeff[mb_nr][0][0], 0, BLOCK_PIXELS * sizeof(byte));
  }

  //========================== CHROMA DC ============================
  //-----------------------------------------------------------------
  // chroma DC coeff
  if(cbp>15)
  {
    for (ll=0;ll<3;ll+=2)
    {
      uv = ll>>1;          

      InvLevelScale4x4 = intra ? currSlice->InvLevelScale4x4_Intra[PLANE_U + uv][qp_rem_uv[uv]] : currSlice->InvLevelScale4x4_Inter[PLANE_U + uv][qp_rem_uv[uv]];
      //===================== CHROMA DC YUV420 ======================
      memset(currSlice->cofu, 0, 4 *sizeof(int));
      coef_ctr=-1;

      currSlice->read_coeff_4x4_CAVLC(currMB, CHROMA_DC, 0, 0, levarr, runarr, &numcoeff);

      for(k = 0; k < numcoeff; ++k)
      {
        if (levarr[k] != 0)
        {
          currMB->s_cbp[0].blk |= 0xf0000 << (ll<<1) ;
          coef_ctr += runarr[k] + 1;
          currSlice->cofu[coef_ctr]=levarr[k];
        }
      }


      if (smb || (currMB->is_lossless == TRUE)) // check to see if MB type is SPred or SIntra4x4
      {
        currSlice->cof[PLANE_U + uv][0][0] = currSlice->cofu[0];
        currSlice->cof[PLANE_U + uv][0][4] = currSlice->cofu[1];
        currSlice->cof[PLANE_U + uv][4][0] = currSlice->cofu[2];
        currSlice->cof[PLANE_U + uv][4][4] = currSlice->cofu[3];
        //currSlice->fcf[PLANE_U + uv][0][0] = currSlice->cofu[0];
        //currSlice->fcf[PLANE_U + uv][4][0] = currSlice->cofu[1];
        //currSlice->fcf[PLANE_U + uv][0][4] = currSlice->cofu[2];
        //currSlice->fcf[PLANE_U + uv][4][4] = currSlice->cofu[3];
      }
      else
      {
        ihadamard2x2(currSlice->cofu, temp);
        //currSlice->fcf[PLANE_U + uv][0][0] = temp[0];
        //currSlice->fcf[PLANE_U + uv][0][4] = temp[1];
        //currSlice->fcf[PLANE_U + uv][4][0] = temp[2];
        //currSlice->fcf[PLANE_U + uv][4][4] = temp[3];

        currSlice->cof[PLANE_U + uv][0][0] = (((temp[0] * InvLevelScale4x4[0][0])<<qp_per_uv[uv])>>5);
        currSlice->cof[PLANE_U + uv][0][4] = (((temp[1] * InvLevelScale4x4[0][0])<<qp_per_uv[uv])>>5);
        currSlice->cof[PLANE_U + uv][4][0] = (((temp[2] * InvLevelScale4x4[0][0])<<qp_per_uv[uv])>>5);
        currSlice->cof[PLANE_U + uv][4][4] = (((temp[3] * InvLevelScale4x4[0][0])<<qp_per_uv[uv])>>5);
      }          
    }     
  }

  //========================== CHROMA AC ============================
  //-----------------------------------------------------------------
  // chroma AC coeff, all zero fram start_scan
  if (cbp<=31)
  {
    fast_memset(p_Vid->nz_coeff [mb_nr ][1][0], 0, 2 * BLOCK_PIXELS * sizeof(byte));
  }
  else
  {
    if(currMB->is_lossless == FALSE)
    {
      for (b8=0; b8 < p_Vid->num_blk8x8_uv; ++b8)
      {
        currMB->is_v_block = uv = (b8 > ((p_Vid->num_uv_blocks) - 1 ));
        InvLevelScale4x4 = intra ? currSlice->InvLevelScale4x4_Intra[PLANE_U + uv][qp_rem_uv[uv]] : currSlice->InvLevelScale4x4_Inter[PLANE_U + uv][qp_rem_uv[uv]];

        for (b4=0; b4 < 4; ++b4)
        {
          i = cofuv_blk_x[0][b8][b4];
          j = cofuv_blk_y[0][b8][b4];

          currSlice->read_coeff_4x4_CAVLC(currMB, CHROMA_AC, i + 2*uv, j + 4, levarr, runarr, &numcoeff);
          coef_ctr = 0;

          for(k = 0; k < numcoeff;++k)
          {
            if (levarr[k] != 0)
            {
              currMB->s_cbp[0].blk |= i64_power2(cbp_blk_chroma[b8][b4]);
              coef_ctr += runarr[k] + 1;

              i0=pos_scan4x4[coef_ctr][0];
              j0=pos_scan4x4[coef_ctr][1];

              currSlice->cof[PLANE_U + uv][(j<<2) + j0][(i<<2) + i0] = rshift_rnd_sf((levarr[k] * InvLevelScale4x4[j0][i0])<<qp_per_uv[uv], 4);
              //currSlice->fcf[PLANE_U + uv][(j<<2) + j0][(i<<2) + i0] = levarr[k];
            }
          }
        }
      }        
    }
    else
    {
      for (b8=0; b8 < p_Vid->num_blk8x8_uv; ++b8)
      {
        currMB->is_v_block = uv = (b8 > ((p_Vid->num_uv_blocks) - 1 ));

        for (b4=0; b4 < 4; ++b4)
        {
          i = cofuv_blk_x[0][b8][b4];
          j = cofuv_blk_y[0][b8][b4];

          currSlice->read_coeff_4x4_CAVLC(currMB, CHROMA_AC, i + 2*uv, j + 4, levarr, runarr, &numcoeff);
          coef_ctr = 0;

          for(k = 0; k < numcoeff;++k)
          {
            if (levarr[k] != 0)
            {
              currMB->s_cbp[0].blk |= i64_power2(cbp_blk_chroma[b8][b4]);
              coef_ctr += runarr[k] + 1;

              i0=pos_scan4x4[coef_ctr][0];
              j0=pos_scan4x4[coef_ctr][1];

              currSlice->cof[PLANE_U + uv][(j<<2) + j0][(i<<2) + i0] = levarr[k];
            }
          }
        }
      }        
    } 
  }
}


/*!
************************************************************************
* \brief
*    setup coefficient reading functions for CAVLC
*
************************************************************************
*/
void set_read_comp_coeff_cavlc(Macroblock *currMB)
{
  if (currMB->is_lossless == FALSE)
  {
    currMB->read_comp_coeff_4x4_CAVLC = read_comp_coeff_4x4_CAVLC;
    currMB->read_comp_coeff_8x8_CAVLC = read_comp_coeff_8x8_CAVLC;
  }
  else
  {
    currMB->read_comp_coeff_4x4_CAVLC = read_comp_coeff_4x4_CAVLC_ls;
    currMB->read_comp_coeff_8x8_CAVLC = read_comp_coeff_8x8_CAVLC_ls;
  }
}


void set_read_CBP_and_coeffs_cavlc(Slice *currSlice)
{
  switch (currSlice->p_Vid->active_sps->chroma_format_idc)
  {
  case YUV444:
    if (currSlice->p_Vid->separate_colour_plane_flag == 0)
    {
      currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CAVLC_444;
    }
    else
    {
      currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CAVLC_400;
    }
    break;
  case YUV422:
    currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CAVLC_422;
    break;
  case YUV420:
    currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CAVLC_420;
    break;
  case YUV400:
    currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CAVLC_400;
    break;
  default:
    assert (1);
    currSlice->read_CBP_and_coeffs_from_NAL = NULL;
    break;
  }
}
