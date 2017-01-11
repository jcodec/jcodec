/*!
 *************************************************************************************
 * \file cabac.c
 *
 * \brief
 *    CABAC entropy coding routines
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Detlev Marpe
 **************************************************************************************
 */

#include "global.h"
#include "cabac.h"
#include "memalloc.h"
#include "elements.h"
#include "image.h"
#include "biaridecod.h"
#include "mb_access.h"
#include "vlc.h"

#if TRACE
int symbolCount = 0;
#endif

static const short maxpos       [] = {15, 14, 63, 31, 31, 15,  3, 14,  7, 15, 15, 14, 63, 31, 31, 15, 15, 14, 63, 31, 31, 15};
static const short c1isdc       [] = { 1,  0,  1,  1,  1,  1,  1,  0,  1,  1,  1,  0,  1,  1,  1,  1,  1,  0,  1,  1,  1,  1};
static const short type2ctx_bcbp[] = { 0,  1,  2,  3,  3,  4,  5,  6,  5,  5, 10, 11, 12, 13, 13, 14, 16, 17, 18, 19, 19, 20};
static const short type2ctx_map [] = { 0,  1,  2,  3,  4,  5,  6,  7,  6,  6, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21}; // 8
static const short type2ctx_last[] = { 0,  1,  2,  3,  4,  5,  6,  7,  6,  6, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21}; // 8
static const short type2ctx_one [] = { 0,  1,  2,  3,  3,  4,  5,  6,  5,  5, 10, 11, 12, 13, 13, 14, 16, 17, 18, 19, 19, 20}; // 7
static const short type2ctx_abs [] = { 0,  1,  2,  3,  3,  4,  5,  6,  5,  5, 10, 11, 12, 13, 13, 14, 16, 17, 18, 19, 19, 20}; // 7
static const short max_c2       [] = { 4,  4,  4,  4,  4,  4,  3,  4,  3,  3,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4}; // 9

/***********************************************************************
 * L O C A L L Y   D E F I N E D   F U N C T I O N   P R O T O T Y P E S
 ***********************************************************************
 */
static unsigned int unary_bin_decode             ( DecodingEnvironmentPtr dep_dp, BiContextTypePtr ctx, int ctx_offset);
static unsigned int unary_bin_max_decode         ( DecodingEnvironmentPtr dep_dp, BiContextTypePtr ctx, int ctx_offset, unsigned int max_symbol);
static unsigned int unary_exp_golomb_level_decode( DecodingEnvironmentPtr dep_dp, BiContextTypePtr ctx);
static unsigned int unary_exp_golomb_mv_decode   ( DecodingEnvironmentPtr dep_dp, BiContextTypePtr ctx, unsigned int max_bin);

void CheckAvailabilityOfNeighborsCABAC(Macroblock *currMB)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  PixelPos up, left;
  int *mb_size = p_Vid->mb_size[IS_LUMA];

  p_Vid->getNeighbour(currMB, -1,  0, mb_size, &left);
  p_Vid->getNeighbour(currMB,  0, -1, mb_size, &up);

  if (up.available)
    currMB->mb_up = &currMB->p_Slice->mb_data[up.mb_addr]; //&p_Vid->mb_data[up.mb_addr];
  else
    currMB->mb_up = NULL;

  if (left.available)
    currMB->mb_left = &currMB->p_Slice->mb_data[left.mb_addr]; //&p_Vid->mb_data[left.mb_addr];
  else
    currMB->mb_left = NULL;
}

void cabac_new_slice(Slice *currSlice)
{
  currSlice->last_dquant = 0;
}

/*!
 ************************************************************************
 * \brief
 *    Allocation of contexts models for the motion info
 *    used for arithmetic decoding
 *
 ************************************************************************
 */
MotionInfoContexts* create_contexts_MotionInfo(void)
{
  MotionInfoContexts *deco_ctx;

  deco_ctx = (MotionInfoContexts*) calloc(1, sizeof(MotionInfoContexts) );
  if( deco_ctx == NULL )
    no_mem_exit("create_contexts_MotionInfo: deco_ctx");

  return deco_ctx;
}


/*!
 ************************************************************************
 * \brief
 *    Allocates of contexts models for the texture info
 *    used for arithmetic decoding
 ************************************************************************
 */
TextureInfoContexts* create_contexts_TextureInfo(void)
{
  TextureInfoContexts *deco_ctx;

  deco_ctx = (TextureInfoContexts*) calloc(1, sizeof(TextureInfoContexts) );
  if( deco_ctx == NULL )
    no_mem_exit("create_contexts_TextureInfo: deco_ctx");

  return deco_ctx;
}


/*!
 ************************************************************************
 * \brief
 *    Frees the memory of the contexts models
 *    used for arithmetic decoding of the motion info.
 ************************************************************************
 */
void delete_contexts_MotionInfo(MotionInfoContexts *deco_ctx)
{
  if( deco_ctx == NULL )
    return;

  free( deco_ctx );
}


/*!
 ************************************************************************
 * \brief
 *    Frees the memory of the contexts models
 *    used for arithmetic decoding of the texture info.
 ************************************************************************
 */
void delete_contexts_TextureInfo(TextureInfoContexts *deco_ctx)
{
  if( deco_ctx == NULL )
    return;

  free( deco_ctx );
}

void readFieldModeInfo_CABAC(Macroblock *currMB,  
                             SyntaxElement *se,
                             DecodingEnvironmentPtr dep_dp)
{  
  Slice *currSlice = currMB->p_Slice;
  //VideoParameters *p_Vid = currMB->p_Vid;
  MotionInfoContexts *ctx  = currSlice->mot_ctx;
  int a = currMB->mbAvailA ? currSlice->mb_data[currMB->mbAddrA].mb_field : 0;
  int b = currMB->mbAvailB ? currSlice->mb_data[currMB->mbAddrB].mb_field : 0;
  int act_ctx = a + b;

  se->value1 = biari_decode_symbol (dep_dp, &ctx->mb_aff_contexts[act_ctx]);

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}


int check_next_mb_and_get_field_mode_CABAC_p_slice( Slice *currSlice,
                                           SyntaxElement *se,                                           
                                           DataPartition  *act_dp)
{
  VideoParameters *p_Vid = currSlice->p_Vid;
  BiContextTypePtr          mb_type_ctx_copy[3];
  BiContextTypePtr          mb_aff_ctx_copy;
  DecodingEnvironmentPtr    dep_dp_copy;
  MotionInfoContexts *mot_ctx  = currSlice->mot_ctx;  

  int length;
  DecodingEnvironmentPtr    dep_dp = &(act_dp->de_cabac);

  int skip   = 0;
  int field  = 0;
  int i;

  Macroblock *currMB;

  //get next MB
  ++currSlice->current_mb_nr; // ++p_Vid->current_mb_nr;
  
  currMB = &currSlice->mb_data[currSlice->current_mb_nr];
  currMB->p_Vid    = p_Vid;
  currMB->p_Slice  = currSlice; 
  currMB->slice_nr = currSlice->current_slice_nr;
  currMB->mb_field = currSlice->mb_data[currSlice->current_mb_nr-1].mb_field;
  currMB->mbAddrX  = currSlice->current_mb_nr;
  currMB->list_offset = ((currSlice->mb_aff_frame_flag)&&(currMB->mb_field))? (currMB->mbAddrX&0x01) ? 4 : 2 : 0;

  CheckAvailabilityOfNeighborsMBAFF(currMB);
  CheckAvailabilityOfNeighborsCABAC(currMB);

  //create
  dep_dp_copy = (DecodingEnvironmentPtr) calloc(1, sizeof(DecodingEnvironment) );
  for (i=0;i<3;++i)
    mb_type_ctx_copy[i] = (BiContextTypePtr) calloc(NUM_MB_TYPE_CTX, sizeof(BiContextType) );
  mb_aff_ctx_copy = (BiContextTypePtr) calloc(NUM_MB_AFF_CTX, sizeof(BiContextType) );

  //copy
  memcpy(dep_dp_copy,dep_dp,sizeof(DecodingEnvironment));
  length = *(dep_dp_copy->Dcodestrm_len) = *(dep_dp->Dcodestrm_len);
  for (i=0;i<3;++i)
    memcpy(mb_type_ctx_copy[i], mot_ctx->mb_type_contexts[i],NUM_MB_TYPE_CTX*sizeof(BiContextType) );
  memcpy(mb_aff_ctx_copy, mot_ctx->mb_aff_contexts,NUM_MB_AFF_CTX*sizeof(BiContextType) );

  //check_next_mb
#if TRACE
  strncpy(se->tracestring, "mb_skip_flag (of following bottom MB)", TRACESTRING_SIZE);
#endif
  currSlice->last_dquant = 0;
  read_skip_flag_CABAC_p_slice(currMB, se, dep_dp);

  skip = (se->value1==0);

  if (!skip)
  {
#if TRACE
    strncpy(se->tracestring, "mb_field_decoding_flag (of following bottom MB)", TRACESTRING_SIZE);
#endif
    readFieldModeInfo_CABAC( currMB, se,dep_dp);
    field = se->value1;
    currSlice->mb_data[currSlice->current_mb_nr-1].mb_field = field;
  }

  //reset
  currSlice->current_mb_nr--;

  memcpy(dep_dp,dep_dp_copy,sizeof(DecodingEnvironment));
  *(dep_dp->Dcodestrm_len) = length;
  for (i=0;i<3;++i)
    memcpy(mot_ctx->mb_type_contexts[i],mb_type_ctx_copy[i], NUM_MB_TYPE_CTX*sizeof(BiContextType) );
  memcpy( mot_ctx->mb_aff_contexts,mb_aff_ctx_copy,NUM_MB_AFF_CTX*sizeof(BiContextType) );

  CheckAvailabilityOfNeighborsCABAC(currMB);

  //delete
  free(dep_dp_copy);
  for (i=0;i<3;++i)
    free(mb_type_ctx_copy[i]);
  free(mb_aff_ctx_copy);

  return skip;
}

int check_next_mb_and_get_field_mode_CABAC_b_slice( Slice *currSlice,
                                           SyntaxElement *se,                                           
                                           DataPartition  *act_dp)
{
  VideoParameters *p_Vid = currSlice->p_Vid;
  BiContextTypePtr          mb_type_ctx_copy[3];
  BiContextTypePtr          mb_aff_ctx_copy;
  DecodingEnvironmentPtr    dep_dp_copy;

  int length;
  DecodingEnvironmentPtr    dep_dp = &(act_dp->de_cabac);
  MotionInfoContexts  *mot_ctx = currSlice->mot_ctx;

  int skip   = 0;
  int field  = 0;
  int i;

  Macroblock *currMB;

  //get next MB
  ++currSlice->current_mb_nr; // ++p_Vid->current_mb_nr;
  
  currMB = &currSlice->mb_data[currSlice->current_mb_nr];
  currMB->p_Vid    = p_Vid;
  currMB->p_Slice  = currSlice; 
  currMB->slice_nr = currSlice->current_slice_nr;
  currMB->mb_field = currSlice->mb_data[currSlice->current_mb_nr-1].mb_field;
  currMB->mbAddrX  = currSlice->current_mb_nr;
  currMB->list_offset = ((currSlice->mb_aff_frame_flag)&&(currMB->mb_field))? (currMB->mbAddrX & 0x01) ? 4 : 2 : 0;

  CheckAvailabilityOfNeighborsMBAFF(currMB);
  CheckAvailabilityOfNeighborsCABAC(currMB);

  //create
  dep_dp_copy = (DecodingEnvironmentPtr) calloc(1, sizeof(DecodingEnvironment) );
  for (i=0;i<3;++i)
    mb_type_ctx_copy[i] = (BiContextTypePtr) calloc(NUM_MB_TYPE_CTX, sizeof(BiContextType) );
  mb_aff_ctx_copy = (BiContextTypePtr) calloc(NUM_MB_AFF_CTX, sizeof(BiContextType) );

  //copy
  memcpy(dep_dp_copy,dep_dp,sizeof(DecodingEnvironment));
  length = *(dep_dp_copy->Dcodestrm_len) = *(dep_dp->Dcodestrm_len);

  for (i=0;i<3;++i)
    memcpy(mb_type_ctx_copy[i], mot_ctx->mb_type_contexts[i],NUM_MB_TYPE_CTX*sizeof(BiContextType) );

  memcpy(mb_aff_ctx_copy, mot_ctx->mb_aff_contexts,NUM_MB_AFF_CTX*sizeof(BiContextType) );

  //check_next_mb
#if TRACE
  strncpy(se->tracestring, "mb_skip_flag (of following bottom MB)", TRACESTRING_SIZE);
#endif
  currSlice->last_dquant = 0;
  read_skip_flag_CABAC_b_slice(currMB, se, dep_dp);

  skip = (se->value1==0 && se->value2==0);
  if (!skip)
  {
#if TRACE
    strncpy(se->tracestring, "mb_field_decoding_flag (of following bottom MB)", TRACESTRING_SIZE);
#endif
    readFieldModeInfo_CABAC( currMB, se,dep_dp);
    field = se->value1;
    currSlice->mb_data[currSlice->current_mb_nr-1].mb_field = field;
  }

  //reset
  currSlice->current_mb_nr--;

  memcpy(dep_dp,dep_dp_copy,sizeof(DecodingEnvironment));
  *(dep_dp->Dcodestrm_len) = length;
  
  for (i=0;i<3;++i)
    memcpy(mot_ctx->mb_type_contexts[i],mb_type_ctx_copy[i], NUM_MB_TYPE_CTX * sizeof(BiContextType) );

  memcpy( mot_ctx->mb_aff_contexts, mb_aff_ctx_copy, NUM_MB_AFF_CTX * sizeof(BiContextType) );

  CheckAvailabilityOfNeighborsCABAC(currMB);

  //delete
  free(dep_dp_copy);
  for (i=0;i<3;++i)
    free(mb_type_ctx_copy[i]);
  free(mb_aff_ctx_copy);

  return skip;
}

/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the motion
 *    vector data of a B-frame MB.
 ************************************************************************
 */
void read_MVD_CABAC( Macroblock *currMB, 
                    SyntaxElement *se,
                    DecodingEnvironmentPtr dep_dp)
{  
  int *mb_size = currMB->p_Vid->mb_size[IS_LUMA];
  Slice *currSlice = currMB->p_Slice;
  MotionInfoContexts *ctx = currSlice->mot_ctx;
  int i = currMB->subblock_x;
  int j = currMB->subblock_y;
  int a = 0;
  //int act_ctx;
  int act_sym;  
  int list_idx = se->value2 & 0x01;
  int k = (se->value2 >> 1); // MVD component

  PixelPos block_a, block_b;

  get4x4NeighbourBase(currMB, i - 1, j    , mb_size, &block_a);
  get4x4NeighbourBase(currMB, i    , j - 1, mb_size, &block_b);
  if (block_a.available)
  {
    a = iabs(currSlice->mb_data[block_a.mb_addr].mvd[list_idx][block_a.y][block_a.x][k]);
  }
  if (block_b.available)
  {
    a += iabs(currSlice->mb_data[block_b.mb_addr].mvd[list_idx][block_b.y][block_b.x][k]);
  }

  //a += b;

  if (a < 3)
    a = 5 * k;
  else if (a > 32)
    a = 5 * k + 3;
  else
    a = 5 * k + 2;

  se->context = a;

  act_sym = biari_decode_symbol(dep_dp, ctx->mv_res_contexts[0] + a );

  if (act_sym != 0)
  {
    a = 5 * k;
    act_sym = unary_exp_golomb_mv_decode(dep_dp, ctx->mv_res_contexts[1] + a, 3) + 1;

    if(biari_decode_symbol_eq_prob(dep_dp))
      act_sym = -act_sym;
  }
  se->value1 = act_sym;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}


/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the motion
 *    vector data of a B-frame MB.
 ************************************************************************
 */
void read_mvd_CABAC_mbaff( Macroblock *currMB, 
                    SyntaxElement *se,
                    DecodingEnvironmentPtr dep_dp)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  Slice *currSlice = currMB->p_Slice;
  MotionInfoContexts *ctx = currSlice->mot_ctx;
  int i = currMB->subblock_x;
  int j = currMB->subblock_y;
  int a = 0, b = 0;
  int act_ctx;
  int act_sym;  
  int list_idx = se->value2 & 0x01;
  int k = (se->value2 >> 1); // MVD component

  PixelPos block_a, block_b;

  get4x4NeighbourBase(currMB, i - 1, j    , p_Vid->mb_size[IS_LUMA], &block_a);
  if (block_a.available)
  {
    a = iabs(currSlice->mb_data[block_a.mb_addr].mvd[list_idx][block_a.y][block_a.x][k]);
    if (currSlice->mb_aff_frame_flag && (k==1))
    {
      if ((currMB->mb_field==0) && (currSlice->mb_data[block_a.mb_addr].mb_field==1))
        a *= 2;
      else if ((currMB->mb_field==1) && (currSlice->mb_data[block_a.mb_addr].mb_field==0))
        a /= 2;
    }
  }

  get4x4NeighbourBase(currMB, i    , j - 1, p_Vid->mb_size[IS_LUMA], &block_b);
  if (block_b.available)
  {
    b = iabs(currSlice->mb_data[block_b.mb_addr].mvd[list_idx][block_b.y][block_b.x][k]);
    if (currSlice->mb_aff_frame_flag && (k==1))
    {
      if ((currMB->mb_field==0) && (currSlice->mb_data[block_b.mb_addr].mb_field==1))
        b *= 2;
      else if ((currMB->mb_field==1) && (currSlice->mb_data[block_b.mb_addr].mb_field==0))
        b /= 2;
    }
  }
  a += b;

  if (a < 3)
    act_ctx = 5 * k;
  else if (a > 32)
    act_ctx = 5 * k + 3;
  else
    act_ctx = 5 * k + 2;

  se->context = act_ctx;

  act_sym = biari_decode_symbol(dep_dp,&ctx->mv_res_contexts[0][act_ctx] );

  if (act_sym != 0)
  {
    act_ctx = 5 * k;
    act_sym = unary_exp_golomb_mv_decode(dep_dp, ctx->mv_res_contexts[1] + act_ctx, 3) + 1;

    if(biari_decode_symbol_eq_prob(dep_dp))
      act_sym = -act_sym;
  }
  se->value1 = act_sym;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}


/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the 8x8 block type.
 ************************************************************************
 */
void readB8_typeInfo_CABAC_p_slice (Macroblock *currMB, 
                                    SyntaxElement *se,
                                    DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  int act_sym = 0;

  MotionInfoContexts *ctx = currSlice->mot_ctx;
  BiContextType *b8_type_contexts = &ctx->b8_type_contexts[0][1];

  if (biari_decode_symbol (dep_dp, b8_type_contexts++))
    act_sym = 0;
  else
  {
    if (biari_decode_symbol (dep_dp, ++b8_type_contexts))
    {
      act_sym = (biari_decode_symbol (dep_dp, ++b8_type_contexts))? 2: 3;
    }
    else
    {
      act_sym = 1;
    }
  } 

  se->value1 = act_sym;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}


/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the 8x8 block type.
 ************************************************************************
 */
void readB8_typeInfo_CABAC_b_slice (Macroblock *currMB, 
                                    SyntaxElement *se,
                                    DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  int act_sym = 0;

  MotionInfoContexts *ctx = currSlice->mot_ctx;
  BiContextType *b8_type_contexts = ctx->b8_type_contexts[1];

  if (biari_decode_symbol (dep_dp, b8_type_contexts++))
  {
    if (biari_decode_symbol (dep_dp, b8_type_contexts++))
    {
      if (biari_decode_symbol (dep_dp, b8_type_contexts++))
      {
        if (biari_decode_symbol (dep_dp, b8_type_contexts))
        {
          act_sym = 10;
          if (biari_decode_symbol (dep_dp, b8_type_contexts)) 
            act_sym++;
        }
        else
        {
          act_sym = 6;
          if (biari_decode_symbol (dep_dp, b8_type_contexts)) 
            act_sym += 2;
          if (biari_decode_symbol (dep_dp, b8_type_contexts)) 
            act_sym++;
        }
      }
      else
      {
        act_sym = 2;
        if (biari_decode_symbol (dep_dp, b8_type_contexts)) 
          act_sym += 2;
        if (biari_decode_symbol (dep_dp, b8_type_contexts)) 
          act_sym ++;
      }
    }
    else
    {
      act_sym = (biari_decode_symbol (dep_dp, ++b8_type_contexts)) ? 1: 0;
    }
    ++act_sym;
  }
  else
  {
    act_sym = 0;
  }

  se->value1 = act_sym;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}

/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the macroblock
 *    type info of a given MB.
 ************************************************************************
 */
void read_skip_flag_CABAC_p_slice( Macroblock *currMB, 
                                  SyntaxElement *se,
                                  DecodingEnvironmentPtr dep_dp)
{
  int a = (currMB->mb_left != NULL) ? (currMB->mb_left->skip_flag == 0) : 0;
  int b = (currMB->mb_up   != NULL) ? (currMB->mb_up  ->skip_flag == 0) : 0;
  BiContextType *mb_type_contexts = &currMB->p_Slice->mot_ctx->mb_type_contexts[1][a + b];

  se->value1 = (biari_decode_symbol(dep_dp, mb_type_contexts) != 1);

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
  if (!se->value1)
  {
    currMB->p_Slice->last_dquant = 0;
  }
}

/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the macroblock
 *    type info of a given MB.
 ************************************************************************
 */
void read_skip_flag_CABAC_b_slice( Macroblock *currMB, 
                                  SyntaxElement *se,
                                  DecodingEnvironmentPtr dep_dp)
{
  int a = (currMB->mb_left != NULL) ? (currMB->mb_left->skip_flag == 0) : 0;
  int b = (currMB->mb_up   != NULL) ? (currMB->mb_up  ->skip_flag == 0) : 0;
  BiContextType *mb_type_contexts = &currMB->p_Slice->mot_ctx->mb_type_contexts[2][7 + a + b];

  se->value1 = se->value2 = (biari_decode_symbol (dep_dp, mb_type_contexts) != 1);

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n", symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
  if (!se->value1)
  {
    currMB->p_Slice->last_dquant = 0;
  }
}

/*!
***************************************************************************
* \brief
*    This function is used to arithmetically decode the macroblock
*    intra_pred_size flag info of a given MB.
***************************************************************************
*/

void readMB_transform_size_flag_CABAC( Macroblock *currMB, 
                                      SyntaxElement *se,
                                      DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  TextureInfoContexts*ctx = currSlice->tex_ctx;

  int b = (currMB->mb_up   == NULL) ? 0 : currMB->mb_up->luma_transform_size_8x8_flag;
  int a = (currMB->mb_left == NULL) ? 0 : currMB->mb_left->luma_transform_size_8x8_flag;

  int act_sym = biari_decode_symbol(dep_dp, ctx->transform_size_contexts + a + b );

  se->value1 = act_sym;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif

}

/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the macroblock
 *    type info of a given MB.
 ************************************************************************
 */
void readMB_typeInfo_CABAC_i_slice(Macroblock *currMB,  
                           SyntaxElement *se,
                           DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  MotionInfoContexts *ctx = currSlice->mot_ctx;

  int a = 0, b = 0;
  int act_ctx;
  int act_sym;
  int mode_sym;
  int curr_mb_type = 0;

  if(currSlice->slice_type == I_SLICE)  // INTRA-frame
  {
    if (currMB->mb_up != NULL)
      b = (((currMB->mb_up)->mb_type != I4MB && currMB->mb_up->mb_type != I8MB) ? 1 : 0 );

    if (currMB->mb_left != NULL)
      a = (((currMB->mb_left)->mb_type != I4MB && currMB->mb_left->mb_type != I8MB) ? 1 : 0 );

    act_ctx = a + b;
    act_sym = biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx);
    se->context = act_ctx; // store context

    if (act_sym==0) // 4x4 Intra
    {
      curr_mb_type = act_sym;
    }
    else // 16x16 Intra
    {
      mode_sym = biari_decode_final(dep_dp);
      if(mode_sym == 1)
      {
        curr_mb_type = 25;
      }
      else
      {
        act_sym = 1;
        act_ctx = 4;
        mode_sym =  biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx ); // decoding of AC/no AC
        act_sym += mode_sym*12;
        act_ctx = 5;
        // decoding of cbp: 0,1,2
        mode_sym =  biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx );
        if (mode_sym!=0)
        {
          act_ctx=6;
          mode_sym = biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx );
          act_sym+=4;
          if (mode_sym!=0)
            act_sym+=4;
        }
        // decoding of I pred-mode: 0,1,2,3
        act_ctx = 7;
        mode_sym =  biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx );
        act_sym += mode_sym*2;
        act_ctx = 8;
        mode_sym =  biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx );
        act_sym += mode_sym;
        curr_mb_type = act_sym;
      }
    }
  }
  else if(currSlice->slice_type == SI_SLICE)  // SI-frame
  {
    // special ctx's for SI4MB
    if (currMB->mb_up != NULL)
      b = (( (currMB->mb_up)->mb_type != SI4MB) ? 1 : 0 );

    if (currMB->mb_left != NULL)
      a = (( (currMB->mb_left)->mb_type != SI4MB) ? 1 : 0 );

    act_ctx = a + b;
    act_sym = biari_decode_symbol(dep_dp, ctx->mb_type_contexts[1] + act_ctx);
    se->context = act_ctx; // store context

    if (act_sym==0) //  SI 4x4 Intra
    {
      curr_mb_type = 0;
    }
    else // analog INTRA_IMG
    {
      if (currMB->mb_up != NULL)
        b = (( (currMB->mb_up)->mb_type != I4MB) ? 1 : 0 );

      if (currMB->mb_left != NULL)
        a = (( (currMB->mb_left)->mb_type != I4MB) ? 1 : 0 );

      act_ctx = a + b;
      act_sym = biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx);
      se->context = act_ctx; // store context

      if (act_sym==0) // 4x4 Intra
      {
        curr_mb_type = 1;
      }
      else // 16x16 Intra
      {
        mode_sym = biari_decode_final(dep_dp);
        if( mode_sym==1 )
        {
          curr_mb_type = 26;
        }
        else
        {
          act_sym = 2;
          act_ctx = 4;
          mode_sym =  biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx ); // decoding of AC/no AC
          act_sym += mode_sym*12;
          act_ctx = 5;
          // decoding of cbp: 0,1,2
          mode_sym =  biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx );
          if (mode_sym!=0)
          {
            act_ctx=6;
            mode_sym = biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx );
            act_sym+=4;
            if (mode_sym!=0)
              act_sym+=4;
          }
          // decoding of I pred-mode: 0,1,2,3
          act_ctx = 7;
          mode_sym =  biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx );
          act_sym += mode_sym*2;
          act_ctx = 8;
          mode_sym =  biari_decode_symbol(dep_dp, ctx->mb_type_contexts[0] + act_ctx );
          act_sym += mode_sym;
          curr_mb_type = act_sym;
        }
      }
    }
  }

  se->value1 = curr_mb_type;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}


/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the macroblock
 *    type info of a given MB.
 ************************************************************************
 */
void readMB_typeInfo_CABAC_p_slice(Macroblock *currMB,  
                           SyntaxElement *se,
                           DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  MotionInfoContexts *ctx = currSlice->mot_ctx;

  int act_ctx;
  int act_sym;
  int mode_sym;
  int curr_mb_type;
  BiContextType *mb_type_contexts = ctx->mb_type_contexts[1];

  if (biari_decode_symbol(dep_dp, &mb_type_contexts[4] ))
  {
    if (biari_decode_symbol(dep_dp, &mb_type_contexts[7] ))   
      act_sym = 7;
    else                                                              
      act_sym = 6;
  }
  else
  {
    if (biari_decode_symbol(dep_dp, &mb_type_contexts[5] ))
    {
      if (biari_decode_symbol(dep_dp, &mb_type_contexts[7] )) 
        act_sym = 2;
      else
        act_sym = 3;
    }
    else
    {
      if (biari_decode_symbol(dep_dp, &mb_type_contexts[6] ))
        act_sym = 4;
      else                                                            
        act_sym = 1;
    }
  }

  if (act_sym <= 6)
  {
    curr_mb_type = act_sym;
  }
  else  // additional info for 16x16 Intra-mode
  {
    mode_sym = biari_decode_final(dep_dp);
    if( mode_sym==1 )
    {
      curr_mb_type = 31;
    }
    else
    {
      act_ctx = 8;
      mode_sym =  biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx ); // decoding of AC/no AC
      act_sym += mode_sym*12;

      // decoding of cbp: 0,1,2
      act_ctx = 9;
      mode_sym = biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx );
      if (mode_sym != 0)
      {
        act_sym+=4;
        mode_sym = biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx );
        if (mode_sym != 0)
          act_sym+=4;
      }

      // decoding of I pred-mode: 0,1,2,3
      act_ctx = 10;
      mode_sym = biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx );
      act_sym += mode_sym*2;
      mode_sym = biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx );
      act_sym += mode_sym;
      curr_mb_type = act_sym;
    }
  }

  se->value1 = curr_mb_type;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}


/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the macroblock
 *    type info of a given MB.
 ************************************************************************
 */
void readMB_typeInfo_CABAC_b_slice(Macroblock *currMB,  
                           SyntaxElement *se,
                           DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  MotionInfoContexts *ctx = currSlice->mot_ctx;

  int a = 0, b = 0;
  int act_ctx;
  int act_sym;
  int mode_sym;
  int curr_mb_type;
  BiContextType *mb_type_contexts = ctx->mb_type_contexts[2];

  if (currMB->mb_up != NULL)
    b = (( (currMB->mb_up)->mb_type != 0) ? 1 : 0 );

  if (currMB->mb_left != NULL)
    a = (( (currMB->mb_left)->mb_type != 0) ? 1 : 0 );

  act_ctx = a + b;

  if (biari_decode_symbol (dep_dp, &mb_type_contexts[act_ctx]))
  {
    if (biari_decode_symbol (dep_dp, &mb_type_contexts[4]))
    {
      if (biari_decode_symbol (dep_dp, &mb_type_contexts[5]))
      {
        act_sym = 12;
        if (biari_decode_symbol (dep_dp, &mb_type_contexts[6])) 
          act_sym += 8;
        if (biari_decode_symbol (dep_dp, &mb_type_contexts[6])) 
          act_sym += 4;
        if (biari_decode_symbol (dep_dp, &mb_type_contexts[6])) 
          act_sym += 2;

        if      (act_sym == 24)  
          act_sym=11;
        else if (act_sym == 26)  
          act_sym = 22;
        else
        {
          if (act_sym == 22)     
            act_sym = 23;
          if (biari_decode_symbol (dep_dp, &mb_type_contexts[6])) 
            act_sym += 1;
        }
      }
      else
      {
        act_sym = 3;
        if (biari_decode_symbol (dep_dp, &mb_type_contexts[6])) 
          act_sym += 4;
        if (biari_decode_symbol (dep_dp, &mb_type_contexts[6])) 
          act_sym += 2;
        if (biari_decode_symbol (dep_dp, &mb_type_contexts[6])) 
          act_sym += 1;
      }
    }
    else
    {
      if (biari_decode_symbol (dep_dp, &mb_type_contexts[6])) 
        act_sym=2;
      else
        act_sym=1;
    }
  }
  else
  {
    act_sym = 0;
  }


  if (act_sym <= 23)
  {
    curr_mb_type = act_sym;
  }
  else  // additional info for 16x16 Intra-mode
  {
    mode_sym = biari_decode_final(dep_dp);
    if( mode_sym == 1 )
    {
      curr_mb_type = 48;
    }
    else
    {
      mb_type_contexts = ctx->mb_type_contexts[1];
      act_ctx = 8;
      mode_sym =  biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx ); // decoding of AC/no AC
      act_sym += mode_sym*12;

      // decoding of cbp: 0,1,2
      act_ctx = 9;
      mode_sym = biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx );
      if (mode_sym != 0)
      {
        act_sym+=4;
        mode_sym = biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx );
        if (mode_sym != 0)
          act_sym+=4;
      }

      // decoding of I pred-mode: 0,1,2,3
      act_ctx = 10;
      mode_sym = biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx );
      act_sym += mode_sym*2;
      mode_sym = biari_decode_symbol(dep_dp, mb_type_contexts + act_ctx );
      act_sym += mode_sym;
      curr_mb_type = act_sym;
    }
  }

  se->value1 = curr_mb_type;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}

/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode a pair of
 *    intra prediction modes of a given MB.
 ************************************************************************
 */
void readIntraPredMode_CABAC( Macroblock *currMB, 
                              SyntaxElement *se,
                              DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  TextureInfoContexts *ctx     = currSlice->tex_ctx;
  // use_most_probable_mode
  int act_sym = biari_decode_symbol(dep_dp, ctx->ipr_contexts);

  // remaining_mode_selector
  if (act_sym == 1)
    se->value1 = -1;
  else
  {
    se->value1  = (biari_decode_symbol(dep_dp, ctx->ipr_contexts + 1)     );
    se->value1 |= (biari_decode_symbol(dep_dp, ctx->ipr_contexts + 1) << 1);
    se->value1 |= (biari_decode_symbol(dep_dp, ctx->ipr_contexts + 1) << 2);
  }

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}
/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the reference
 *    parameter of a given MB.
 ************************************************************************
 */
void readRefFrame_CABAC(Macroblock *currMB, 
                        SyntaxElement *se,
                        DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  StorablePicture *dec_picture = currSlice->dec_picture;
  MotionInfoContexts *ctx = currSlice->mot_ctx;
  Macroblock *neighborMB = NULL;

  int   addctx  = 0;
  int   a = 0, b = 0;
  int   act_ctx;
  int   act_sym;
  int   list = se->value2;

  PixelPos block_a, block_b;

  get4x4Neighbour(currMB, currMB->subblock_x - 1, currMB->subblock_y    , p_Vid->mb_size[IS_LUMA], &block_a);
  get4x4Neighbour(currMB, currMB->subblock_x,     currMB->subblock_y - 1, p_Vid->mb_size[IS_LUMA], &block_b);

  if (block_b.available)
  {
    int b8b=((block_b.x >> 1) & 0x01)+(block_b.y & 0x02);    
    neighborMB = &currSlice->mb_data[block_b.mb_addr];
    if (!( (neighborMB->mb_type==IPCM) || IS_DIRECT(neighborMB) || (neighborMB->b8mode[b8b]==0 && neighborMB->b8pdir[b8b]==2)))
    {
      if (currSlice->mb_aff_frame_flag && (currMB->mb_field == FALSE) && (neighborMB->mb_field == TRUE))
        b = (dec_picture->mv_info[block_b.pos_y][block_b.pos_x].ref_idx[list] > 1 ? 2 : 0);
      else
        b = (dec_picture->mv_info[block_b.pos_y][block_b.pos_x].ref_idx[list] > 0 ? 2 : 0);
    }
  }

  if (block_a.available)
  {    
    int b8a=((block_a.x >> 1) & 0x01)+(block_a.y & 0x02);    
    neighborMB = &currSlice->mb_data[block_a.mb_addr];
    if (!((neighborMB->mb_type==IPCM) || IS_DIRECT(neighborMB) || (neighborMB->b8mode[b8a]==0 && neighborMB->b8pdir[b8a]==2)))
    {
      if (currSlice->mb_aff_frame_flag && (currMB->mb_field == FALSE) && (neighborMB->mb_field == 1))
        a = (dec_picture->mv_info[block_a.pos_y][block_a.pos_x].ref_idx[list] > 1 ? 1 : 0);
      else
        a = (dec_picture->mv_info[block_a.pos_y][block_a.pos_x].ref_idx[list] > 0 ? 1 : 0);
    }
  }

  act_ctx = a + b;
  se->context = act_ctx; // store context

  act_sym = biari_decode_symbol(dep_dp,ctx->ref_no_contexts[addctx] + act_ctx );

  if (act_sym != 0)
  {
    act_ctx = 4;
    act_sym = unary_bin_decode(dep_dp,ctx->ref_no_contexts[addctx] + act_ctx,1);
    ++act_sym;
  }
  se->value1 = act_sym;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
//  fprintf(p_Dec->p_trace," c: %d :%d \n",ctx->ref_no_contexts[addctx][act_ctx].cum_freq[0],ctx->ref_no_contexts[addctx][act_ctx].cum_freq[1]);
  fflush(p_Dec->p_trace);
#endif
}


/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the delta qp
 *     of a given MB.
 ************************************************************************
 */
void read_dQuant_CABAC( Macroblock *currMB, 
                       SyntaxElement *se,                       
                       DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  MotionInfoContexts *ctx = currSlice->mot_ctx;
  int *dquant = &se->value1;
  int act_ctx = ((currSlice->last_dquant != 0) ? 1 : 0);
  int act_sym = biari_decode_symbol(dep_dp,ctx->delta_qp_contexts + act_ctx );

  if (act_sym != 0)
  {
    act_ctx = 2;
    act_sym = unary_bin_decode(dep_dp,ctx->delta_qp_contexts + act_ctx,1);
    ++act_sym;
    *dquant = (act_sym + 1) >> 1;
    if((act_sym & 0x01)==0)                           // lsb is signed bit
      *dquant = -*dquant;
  }
  else
    *dquant = 0;

  currSlice->last_dquant = *dquant;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}
/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the coded
 *    block pattern of a given MB.
 ************************************************************************
 */
void read_CBP_CABAC(Macroblock *currMB, 
                    SyntaxElement *se,
                    DecodingEnvironmentPtr dep_dp)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  StorablePicture *dec_picture = currMB->p_Slice->dec_picture;
  Slice *currSlice = currMB->p_Slice;
  TextureInfoContexts *ctx = currSlice->tex_ctx;  
  Macroblock *neighborMB = NULL;

  int mb_x, mb_y;
  int a = 0, b = 0;
  int curr_cbp_ctx;
  int cbp = 0;
  int cbp_bit;
  int mask;
  PixelPos block_a;

  //  coding of luma part (bit by bit)
  for (mb_y=0; mb_y < 4; mb_y += 2)
  {
    if (mb_y == 0)
    {
      neighborMB = currMB->mb_up;
      b = 0;
    }

    for (mb_x=0; mb_x < 4; mb_x += 2)
    {
      if (mb_y == 0)
      {
        if (neighborMB != NULL)
        {
          if(neighborMB->mb_type!=IPCM)
            b = (( (neighborMB->cbp & (1<<(2 + (mb_x>>1)))) == 0) ? 2 : 0);
        }
      }
      else
        b = ( ((cbp & (1<<(mb_x/2))) == 0) ? 2: 0);

      if (mb_x == 0)
      {
        get4x4Neighbour(currMB, (mb_x<<2) - 1, (mb_y << 2), p_Vid->mb_size[IS_LUMA], &block_a);
        if (block_a.available)
        {
          if(currSlice->mb_data[block_a.mb_addr].mb_type==IPCM)
            a = 0;
          else
            a = (( (currSlice->mb_data[block_a.mb_addr].cbp & (1<<(2*(block_a.y/2)+1))) == 0) ? 1 : 0);
        }
        else
          a=0;
      }
      else
        a = ( ((cbp & (1<<mb_y)) == 0) ? 1: 0);

      curr_cbp_ctx = a + b;
      mask = (1 << (mb_y + (mb_x >> 1)));
      cbp_bit = biari_decode_symbol(dep_dp, ctx->cbp_contexts[0] + curr_cbp_ctx );
      if (cbp_bit) 
        cbp += mask;
    }
  }

  if ((dec_picture->chroma_format_idc != YUV400) && (dec_picture->chroma_format_idc != YUV444)) 
  {
    // coding of chroma part
    // CABAC decoding for BinIdx 0
    b = 0;
    neighborMB = currMB->mb_up;
    if (neighborMB != NULL)
    {
      if (neighborMB->mb_type==IPCM || (neighborMB->cbp > 15))
        b = 2;
    }

    a = 0;
    neighborMB = currMB->mb_left;
    if (neighborMB != NULL)
    {
      if (neighborMB->mb_type==IPCM || (neighborMB->cbp > 15))
        a = 1;
    }

    curr_cbp_ctx = a + b;
    cbp_bit = biari_decode_symbol(dep_dp, ctx->cbp_contexts[1] + curr_cbp_ctx );

    // CABAC decoding for BinIdx 1
    if (cbp_bit) // set the chroma bits
    {
      b = 0;
      neighborMB = currMB->mb_up;
      if (neighborMB != NULL)
      {
        //if ((neighborMB->mb_type == IPCM) || ((neighborMB->cbp > 15) && ((neighborMB->cbp >> 4) == 2)))
        if ((neighborMB->mb_type == IPCM) || ((neighborMB->cbp >> 4) == 2))
          b = 2;
      }


      a = 0;
      neighborMB = currMB->mb_left;
      if (neighborMB != NULL)
      {
        if ((neighborMB->mb_type == IPCM) || ((neighborMB->cbp >> 4) == 2))
          a = 1;
      }

      curr_cbp_ctx = a + b;
      cbp_bit = biari_decode_symbol(dep_dp, ctx->cbp_contexts[2] + curr_cbp_ctx );
      cbp += (cbp_bit == 1) ? 32 : 16;
    }
  }

  se->value1 = cbp;

  if (!cbp)
  {
    currSlice->last_dquant = 0;
  }

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif
}

/*!
 ************************************************************************
 * \brief
 *    This function is used to arithmetically decode the chroma
 *    intra prediction mode of a given MB.
 ************************************************************************
 */
void readCIPredMode_CABAC(Macroblock *currMB, 
                          SyntaxElement *se,
                          DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  TextureInfoContexts *ctx = currSlice->tex_ctx;
  int                 *act_sym  = &se->value1;

  Macroblock          *MbUp   = currMB->mb_up;
  Macroblock          *MbLeft = currMB->mb_left;

  int b = (MbUp != NULL)   ? (((MbUp->c_ipred_mode   != 0) && (MbUp->mb_type != IPCM)) ? 1 : 0) : 0;
  int a = (MbLeft != NULL) ? (((MbLeft->c_ipred_mode != 0) && (MbLeft->mb_type != IPCM)) ? 1 : 0) : 0;
  int act_ctx = a + b;

  *act_sym = biari_decode_symbol(dep_dp, ctx->cipr_contexts + act_ctx );

  if (*act_sym != 0)
    *act_sym = unary_bin_max_decode(dep_dp, ctx->cipr_contexts + 3, 0, 1) + 1;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, se->tracestring, se->value1);
  fflush(p_Dec->p_trace);
#endif

}


/*!
 ************************************************************************
 * \brief
 *    Read CBP4-BIT
 ************************************************************************
*/
static int read_and_store_CBP_block_bit_444 (Macroblock              *currMB,
                                             DecodingEnvironmentPtr  dep_dp,
                                             int                     type)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  StorablePicture *dec_picture = currSlice->dec_picture;
  TextureInfoContexts *tex_ctx = currSlice->tex_ctx;
  Macroblock *mb_data = currSlice->mb_data;
  int y_ac        = (type==LUMA_16AC || type==LUMA_8x8 || type==LUMA_8x4 || type==LUMA_4x8 || type==LUMA_4x4
                    || type==CB_16AC || type==CB_8x8 || type==CB_8x4 || type==CB_4x8 || type==CB_4x4
                    || type==CR_16AC || type==CR_8x8 || type==CR_8x4 || type==CR_4x8 || type==CR_4x4);
  int y_dc        = (type==LUMA_16DC || type==CB_16DC || type==CR_16DC); 
  int u_ac        = (type==CHROMA_AC && !currMB->is_v_block);
  int v_ac        = (type==CHROMA_AC &&  currMB->is_v_block);
  int chroma_dc   = (type==CHROMA_DC || type==CHROMA_DC_2x4 || type==CHROMA_DC_4x4);
  int u_dc        = (chroma_dc && !currMB->is_v_block);
  int v_dc        = (chroma_dc &&  currMB->is_v_block);
  int j           = (y_ac || u_ac || v_ac ? currMB->subblock_y : 0);
  int i           = (y_ac || u_ac || v_ac ? currMB->subblock_x : 0);
  int bit         = (y_dc ? 0 : y_ac ? 1 : u_dc ? 17 : v_dc ? 18 : u_ac ? 19 : 35);
  int default_bit = (currMB->is_intra_block ? 1 : 0);
  int upper_bit   = default_bit;
  int left_bit    = default_bit;
  int cbp_bit     = 1;  // always one for 8x8 mode
  int ctx;
  int bit_pos_a   = 0;
  int bit_pos_b   = 0;
  
  PixelPos block_a, block_b;
  if (y_ac)
  {
    get4x4Neighbour(currMB, i - 1, j    , p_Vid->mb_size[IS_LUMA], &block_a);
    get4x4Neighbour(currMB, i    , j - 1, p_Vid->mb_size[IS_LUMA], &block_b);
    if (block_a.available)
        bit_pos_a = 4*block_a.y + block_a.x;
      if (block_b.available)
        bit_pos_b = 4*block_b.y + block_b.x;
  }
  else if (y_dc)
  {
    get4x4Neighbour(currMB, i - 1, j    , p_Vid->mb_size[IS_LUMA], &block_a);
    get4x4Neighbour(currMB, i    , j - 1, p_Vid->mb_size[IS_LUMA], &block_b);
  }
  else if (u_ac||v_ac)
  {
    get4x4Neighbour(currMB, i - 1, j    , p_Vid->mb_size[IS_CHROMA], &block_a);
    get4x4Neighbour(currMB, i    , j - 1, p_Vid->mb_size[IS_CHROMA], &block_b);
    if (block_a.available)
      bit_pos_a = 4*block_a.y + block_a.x;
    if (block_b.available)
      bit_pos_b = 4*block_b.y + block_b.x;
  }
  else
  {
    get4x4Neighbour(currMB, i - 1, j    , p_Vid->mb_size[IS_CHROMA], &block_a);
    get4x4Neighbour(currMB, i    , j - 1, p_Vid->mb_size[IS_CHROMA], &block_b);
  }
  
  if (dec_picture->chroma_format_idc!=YUV444)
  {
    if (type!=LUMA_8x8)
    {
      //--- get bits from neighboring blocks ---
      if (block_b.available)
      {
        if(mb_data[block_b.mb_addr].mb_type==IPCM)
          upper_bit=1;
        else
          upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[0].bits, bit + bit_pos_b);
      }
            
      if (block_a.available)
      {
        if(mb_data[block_a.mb_addr].mb_type==IPCM)
          left_bit=1;
        else
          left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[0].bits, bit + bit_pos_a);
      }
      
      
      ctx = 2 * upper_bit + left_bit;     
      //===== encode symbol =====
      cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);
    }
  }
  else if( (p_Vid->separate_colour_plane_flag != 0) )
  {
    if (type!=LUMA_8x8)
    {
      //--- get bits from neighbouring blocks ---
      if (block_b.available)
      {
        if(mb_data[block_b.mb_addr].mb_type==IPCM)
          upper_bit = 1;
        else
          upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[0].bits,bit+bit_pos_b);
      }
      
      
      if (block_a.available)
      {
        if(mb_data[block_a.mb_addr].mb_type==IPCM)
          left_bit = 1;
        else
          left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[0].bits,bit+bit_pos_a);
      }
      
      
      ctx = 2 * upper_bit + left_bit;     
      //===== encode symbol =====
      cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);
    }
  }
  else 
  {
    if (block_b.available)
    {
      if(mb_data[block_b.mb_addr].mb_type==IPCM)
      {
        upper_bit=1;
      }
      else if((type==LUMA_8x8 || type==CB_8x8 || type==CR_8x8) &&
         !mb_data[block_b.mb_addr].luma_transform_size_8x8_flag)
      {
        upper_bit = 0;
      }
      else
      {
        if(type==LUMA_8x8)
          upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[0].bits_8x8, bit + bit_pos_b);
        else if (type==CB_8x8)
          upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[1].bits_8x8, bit + bit_pos_b);
        else if (type==CR_8x8)
          upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[2].bits_8x8, bit + bit_pos_b);
        else if ((type==CB_4x4)||(type==CB_4x8)||(type==CB_8x4)||(type==CB_16AC)||(type==CB_16DC))
          upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[1].bits,bit+bit_pos_b);
        else if ((type==CR_4x4)||(type==CR_4x8)||(type==CR_8x4)||(type==CR_16AC)||(type==CR_16DC))
          upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[2].bits,bit+bit_pos_b);
        else
          upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[0].bits,bit+bit_pos_b);
      }
    }
    
    
    if (block_a.available)
    {
      if(mb_data[block_a.mb_addr].mb_type==IPCM)
      {
        left_bit=1;
      }
      else if((type==LUMA_8x8 || type==CB_8x8 || type==CR_8x8) &&
         !mb_data[block_a.mb_addr].luma_transform_size_8x8_flag)
      {
        left_bit=0;
      }
      else
      {
        if(type==LUMA_8x8)
          left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[0].bits_8x8,bit+bit_pos_a);
        else if (type==CB_8x8)
          left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[1].bits_8x8,bit+bit_pos_a);
        else if (type==CR_8x8)
          left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[2].bits_8x8,bit+bit_pos_a);
        else if ((type==CB_4x4)||(type==CB_4x8)||(type==CB_8x4)||(type==CB_16AC)||(type==CB_16DC))
          left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[1].bits,bit+bit_pos_a);
        else if ((type==CR_4x4)||(type==CR_4x8)||(type==CR_8x4)||(type==CR_16AC)||(type==CR_16DC))
          left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[2].bits,bit+bit_pos_a);
        else
          left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[0].bits,bit+bit_pos_a);
      }
    }
    
    ctx = 2 * upper_bit + left_bit;
    //===== encode symbol =====
    cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);
  }
 
  //--- set bits for current block ---
  bit = (y_dc ? 0 : y_ac ? 1 + j + (i >> 2) : u_dc ? 17 : v_dc ? 18 : u_ac ? 19 + j + (i >> 2) : 35 + j + (i >> 2)); 

  if (cbp_bit)
  {  
    CBPStructure  *s_cbp = currMB->s_cbp;
    if (type==LUMA_8x8) 
    {      
      s_cbp[0].bits |= ((int64) 0x33 << bit   );
      
      if (dec_picture->chroma_format_idc==YUV444)
      {
        s_cbp[0].bits_8x8   |= ((int64) 0x33 << bit   );
      }
    }
    else if (type==CB_8x8)
    {
      s_cbp[1].bits_8x8   |= ((int64) 0x33 << bit   );      
      s_cbp[1].bits   |= ((int64) 0x33 << bit   );
    }
    else if (type==CR_8x8)
    {
      s_cbp[2].bits_8x8   |= ((int64) 0x33 << bit   );      
      s_cbp[2].bits   |= ((int64) 0x33 << bit   );
    }
    else if (type==LUMA_8x4)
    {
      s_cbp[0].bits   |= ((int64) 0x03 << bit   );
    }
    else if (type==CB_8x4)
    {
      s_cbp[1].bits   |= ((int64) 0x03 << bit   );
    }
    else if (type==CR_8x4)
    {
      s_cbp[2].bits   |= ((int64) 0x03 << bit   );
    }
    else if (type==LUMA_4x8)
    {
      s_cbp[0].bits   |= ((int64) 0x11<< bit   );
    }
    else if (type==CB_4x8)
    {
      s_cbp[1].bits   |= ((int64)0x11<< bit   );
    }
    else if (type==CR_4x8)
    {
      s_cbp[2].bits   |= ((int64)0x11<< bit   );
    }
    else if ((type==CB_4x4)||(type==CB_16AC)||(type==CB_16DC))
    {
      s_cbp[1].bits   |= i64_power2(bit);
    }
    else if ((type==CR_4x4)||(type==CR_16AC)||(type==CR_16DC))
    {
      s_cbp[2].bits   |= i64_power2(bit);
    }
    else
    {
      s_cbp[0].bits   |= i64_power2(bit);
    }
  }
  return cbp_bit;
}


static inline int set_cbp_bit(Macroblock *neighbor_mb)
{
  if(neighbor_mb->mb_type == IPCM)
    return 1;
  else
    return (int) (neighbor_mb->s_cbp[0].bits & 0x01);
}

static inline int set_cbp_bit_ac(Macroblock *neighbor_mb, PixelPos *block)
{
  if (neighbor_mb->mb_type == IPCM)
    return 1;
  else
  {
    int bit_pos = 1 + (block->y << 2) + block->x;
    return get_bit(neighbor_mb->s_cbp[0].bits, bit_pos);
  }
}

/*!
 ************************************************************************
 * \brief
 *    Read CBP4-BIT
 ************************************************************************
 */
static int read_and_store_CBP_block_bit_normal (Macroblock              *currMB,
                                                DecodingEnvironmentPtr  dep_dp,
                                                int                     type)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  TextureInfoContexts *tex_ctx = currSlice->tex_ctx;
  int cbp_bit     = 1;  // always one for 8x8 mode
  Macroblock *mb_data = currSlice->mb_data;

  if (type==LUMA_16DC)
  {
    int upper_bit   = 1;
    int left_bit    = 1;
    int ctx;

    PixelPos block_a, block_b;

    get4x4NeighbourBase(currMB, -1,  0, p_Vid->mb_size[IS_LUMA], &block_a);
    get4x4NeighbourBase(currMB,  0, -1, p_Vid->mb_size[IS_LUMA], &block_b);

    //--- get bits from neighboring blocks ---
    if (block_b.available)
    {
      upper_bit = set_cbp_bit(&mb_data[block_b.mb_addr]);
    }

    if (block_a.available)
    {
      left_bit = set_cbp_bit(&mb_data[block_a.mb_addr]);
    }

    ctx = 2 * upper_bit + left_bit;     
    //===== encode symbol =====
    cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);

    //--- set bits for current block ---

    if (cbp_bit)
    {  
      currMB->s_cbp[0].bits |= 1;
    }
  }
  else if (type==LUMA_16AC)
  {
    int j           = currMB->subblock_y;
    int i           = currMB->subblock_x;
    int bit         = 1;
    int default_bit = (currMB->is_intra_block ? 1 : 0);
    int upper_bit   = default_bit;
    int left_bit    = default_bit;
    int ctx;

    PixelPos block_a, block_b;

    get4x4NeighbourBase(currMB, i - 1, j    , p_Vid->mb_size[IS_LUMA], &block_a);
    get4x4NeighbourBase(currMB, i    , j - 1, p_Vid->mb_size[IS_LUMA], &block_b);

    //--- get bits from neighboring blocks ---
    if (block_b.available)
    {
      upper_bit = set_cbp_bit_ac(&mb_data[block_b.mb_addr], &block_b);
    }

    if (block_a.available)
    {
      left_bit = set_cbp_bit_ac(&mb_data[block_a.mb_addr], &block_a);
    }

    ctx = 2 * upper_bit + left_bit;     
    //===== encode symbol =====
    cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);

    if (cbp_bit)
    {
      //--- set bits for current block ---
      bit = 1 + j + (i >> 2); 
      currMB->s_cbp[0].bits   |= i64_power2(bit);
    }
  }
  else if (type==LUMA_8x4)
  {
    int j           = currMB->subblock_y;
    int i           = currMB->subblock_x;
    int bit         = 1;
    int default_bit = (currMB->is_intra_block ? 1 : 0);
    int upper_bit   = default_bit;
    int left_bit    = default_bit;
    int ctx;

    PixelPos block_a, block_b;

    get4x4NeighbourBase(currMB, i - 1, j    , p_Vid->mb_size[IS_LUMA], &block_a);
    get4x4NeighbourBase(currMB, i    , j - 1, p_Vid->mb_size[IS_LUMA], &block_b);

    //--- get bits from neighboring blocks ---
    if (block_b.available)
    {      
      upper_bit = set_cbp_bit_ac(&mb_data[block_b.mb_addr], &block_b);
    }

    if (block_a.available)
    {      
      left_bit = set_cbp_bit_ac(&mb_data[block_a.mb_addr], &block_a);
    }

    ctx = 2 * upper_bit + left_bit;     
    //===== encode symbol =====
    cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);

    if (cbp_bit)
    {  
      //--- set bits for current block ---
      bit = 1 + j + (i >> 2); 
      currMB->s_cbp[0].bits   |= ((int64) 0x03 << bit   );
    }
  }
  else if (type==LUMA_4x8)
  {
    int j           = currMB->subblock_y;
    int i           = currMB->subblock_x;
    int bit         = 1;
    int default_bit = (currMB->is_intra_block ? 1 : 0);
    int upper_bit   = default_bit;
    int left_bit    = default_bit;
    int ctx;

    PixelPos block_a, block_b;

    get4x4NeighbourBase(currMB, i - 1, j    , p_Vid->mb_size[IS_LUMA], &block_a);
    get4x4NeighbourBase(currMB, i    , j - 1, p_Vid->mb_size[IS_LUMA], &block_b);

    //--- get bits from neighboring blocks ---
    if (block_b.available)
    {      
      upper_bit = set_cbp_bit_ac(&mb_data[block_b.mb_addr], &block_b);
    }

    if (block_a.available)
    {      
      left_bit = set_cbp_bit_ac(&mb_data[block_a.mb_addr], &block_a);
    }

    ctx = 2 * upper_bit + left_bit;     
    //===== encode symbol =====
    cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);

    if (cbp_bit)
    { 
      //--- set bits for current block ---
      bit = 1 + j + (i >> 2); 

      currMB->s_cbp[0].bits   |= ((int64) 0x11 << bit   );
    }
  }
  else if (type==LUMA_4x4)
  {
    int j           = currMB->subblock_y;
    int i           = currMB->subblock_x;
    int bit         = 1;
    int default_bit = (currMB->is_intra_block ? 1 : 0);
    int upper_bit   = default_bit;
    int left_bit    = default_bit;
    int ctx;

    PixelPos block_a, block_b;

    get4x4NeighbourBase(currMB, i - 1, j    , p_Vid->mb_size[IS_LUMA], &block_a);
    get4x4NeighbourBase(currMB, i    , j - 1, p_Vid->mb_size[IS_LUMA], &block_b);

    //--- get bits from neighboring blocks ---
    if (block_b.available)
    {      
      upper_bit = set_cbp_bit_ac(&mb_data[block_b.mb_addr], &block_b);
    }

    if (block_a.available)
    {      
      left_bit = set_cbp_bit_ac(&mb_data[block_a.mb_addr], &block_a);
    }

    ctx = 2 * upper_bit + left_bit;     
    //===== encode symbol =====
    cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);

    if (cbp_bit)
    { 
      //--- set bits for current block ---
      bit = 1 + j + (i >> 2); 

      currMB->s_cbp[0].bits   |= i64_power2(bit);
    }
  }
  else if (type == LUMA_8x8)
  {
    int j           = currMB->subblock_y;
    int i           = currMB->subblock_x;
    //--- set bits for current block ---
    int bit         = 1 + j + (i >> 2);

    currMB->s_cbp[0].bits |= ((int64) 0x33 << bit   );      
  }
  else if (type==CHROMA_DC || type==CHROMA_DC_2x4 || type==CHROMA_DC_4x4)
  {
    int u_dc        = (!currMB->is_v_block);
    int j           = 0;
    int i           = 0;
    int bit         = (u_dc ? 17 : 18);
    int default_bit = (currMB->is_intra_block ? 1 : 0);
    int upper_bit   = default_bit;
    int left_bit    = default_bit;
    int ctx;

    PixelPos block_a, block_b;

    get4x4NeighbourBase(currMB, i - 1, j    , p_Vid->mb_size[IS_CHROMA], &block_a);
    get4x4NeighbourBase(currMB, i    , j - 1, p_Vid->mb_size[IS_CHROMA], &block_b);    

    //--- get bits from neighboring blocks ---
    if (block_b.available)
    {
      if(mb_data[block_b.mb_addr].mb_type==IPCM)
        upper_bit = 1;
      else
        upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[0].bits, bit);
    }

    if (block_a.available)
    {
      if(mb_data[block_a.mb_addr].mb_type==IPCM)
        left_bit = 1;
      else
        left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[0].bits, bit);
    }

    ctx = 2 * upper_bit + left_bit;     
    //===== encode symbol =====
    cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);

    if (cbp_bit)
    {
      //--- set bits for current block ---
      bit = (u_dc ? 17 : 18); 
      currMB->s_cbp[0].bits   |= i64_power2(bit);
    }
  }
  else
  {
    int u_ac        = (!currMB->is_v_block);
    int j           = currMB->subblock_y;
    int i           = currMB->subblock_x;
    int bit         = (u_ac ? 19 : 35);
    int default_bit = (currMB->is_intra_block ? 1 : 0);
    int upper_bit   = default_bit;
    int left_bit    = default_bit;
    int ctx;

    PixelPos block_a, block_b;

    get4x4NeighbourBase(currMB, i - 1, j    , p_Vid->mb_size[IS_CHROMA], &block_a);
    get4x4NeighbourBase(currMB, i    , j - 1, p_Vid->mb_size[IS_CHROMA], &block_b);    

    //--- get bits from neighboring blocks ---
    if (block_b.available)
    {
      if(mb_data[block_b.mb_addr].mb_type==IPCM)
        upper_bit=1;
      else
      {
        int bit_pos_b = 4*block_b.y + block_b.x;
        upper_bit = get_bit(mb_data[block_b.mb_addr].s_cbp[0].bits, bit + bit_pos_b);
      }
    }

    if (block_a.available)
    {
      if(mb_data[block_a.mb_addr].mb_type==IPCM)
        left_bit=1;
      else
      {
        int bit_pos_a = 4*block_a.y + block_a.x;
        left_bit = get_bit(mb_data[block_a.mb_addr].s_cbp[0].bits,bit + bit_pos_a);
      }
    }

    ctx = 2 * upper_bit + left_bit;     
    //===== encode symbol =====
    cbp_bit = biari_decode_symbol (dep_dp, tex_ctx->bcbp_contexts[type2ctx_bcbp[type]] + ctx);

    if (cbp_bit)
    {
      //--- set bits for current block ---
      bit = (u_ac ? 19 + j + (i >> 2) : 35 + j + (i >> 2)); 
      currMB->s_cbp[0].bits   |= i64_power2(bit);
    }
  }
  return cbp_bit;
}


void set_read_and_store_CBP(Macroblock **currMB, int chroma_format_idc)
{
  if (chroma_format_idc == YUV444)
    (*currMB)->read_and_store_CBP_block_bit = read_and_store_CBP_block_bit_444;
  else
    (*currMB)->read_and_store_CBP_block_bit = read_and_store_CBP_block_bit_normal; 
}




//===== position -> ctx for MAP =====
//--- zig-zag scan ----
static const byte  pos2ctx_map8x8 [] = { 0,  1,  2,  3,  4,  5,  5,  4,  4,  3,  3,  4,  4,  4,  5,  5,
                                         4,  4,  4,  4,  3,  3,  6,  7,  7,  7,  8,  9, 10,  9,  8,  7,
                                         7,  6, 11, 12, 13, 11,  6,  7,  8,  9, 14, 10,  9,  8,  6, 11,
                                        12, 13, 11,  6,  9, 14, 10,  9, 11, 12, 13, 11 ,14, 10, 12, 14}; // 15 CTX
static const byte  pos2ctx_map8x4 [] = { 0,  1,  2,  3,  4,  5,  7,  8,  9, 10, 11,  9,  8,  6,  7,  8,
                                         9, 10, 11,  9,  8,  6, 12,  8,  9, 10, 11,  9, 13, 13, 14, 14}; // 15 CTX
static const byte  pos2ctx_map4x4 [] = { 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 14}; // 15 CTX
static const byte  pos2ctx_map2x4c[] = { 0,  0,  1,  1,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2}; // 15 CTX
static const byte  pos2ctx_map4x4c[] = { 0,  0,  0,  0,  1,  1,  1,  1,  2,  2,  2,  2,  2,  2,  2,  2}; // 15 CTX
static const byte* pos2ctx_map    [] = {pos2ctx_map4x4, pos2ctx_map4x4, pos2ctx_map8x8, pos2ctx_map8x4,
                                        pos2ctx_map8x4, pos2ctx_map4x4, pos2ctx_map4x4, pos2ctx_map4x4,
                                        pos2ctx_map2x4c, pos2ctx_map4x4c, 
                                        pos2ctx_map4x4, pos2ctx_map4x4, pos2ctx_map8x8,pos2ctx_map8x4,
                                        pos2ctx_map8x4, pos2ctx_map4x4,
                                        pos2ctx_map4x4, pos2ctx_map4x4, pos2ctx_map8x8,pos2ctx_map8x4,
                                        pos2ctx_map8x4,pos2ctx_map4x4};
//--- interlace scan ----
//taken from ABT
static const byte  pos2ctx_map8x8i[] = { 0,  1,  1,  2,  2,  3,  3,  4,  5,  6,  7,  7,  7,  8,  4,  5,
                                         6,  9, 10, 10,  8, 11, 12, 11,  9,  9, 10, 10,  8, 11, 12, 11,
                                         9,  9, 10, 10,  8, 11, 12, 11,  9,  9, 10, 10,  8, 13, 13,  9,
                                         9, 10, 10,  8, 13, 13,  9,  9, 10, 10, 14, 14, 14, 14, 14, 14}; // 15 CTX
static const byte  pos2ctx_map8x4i[] = { 0,  1,  2,  3,  4,  5,  6,  3,  4,  5,  6,  3,  4,  7,  6,  8,
                                         9,  7,  6,  8,  9, 10, 11, 12, 12, 10, 11, 13, 13, 14, 14, 14}; // 15 CTX
static const byte  pos2ctx_map4x8i[] = { 0,  1,  1,  1,  2,  3,  3,  4,  4,  4,  5,  6,  2,  7,  7,  8,
                                         8,  8,  5,  6,  9, 10, 10, 11, 11, 11, 12, 13, 13, 14, 14, 14}; // 15 CTX
static const byte* pos2ctx_map_int[] = {pos2ctx_map4x4, pos2ctx_map4x4, pos2ctx_map8x8i,pos2ctx_map8x4i,
                                        pos2ctx_map4x8i,pos2ctx_map4x4, pos2ctx_map4x4, pos2ctx_map4x4,
                                        pos2ctx_map2x4c, pos2ctx_map4x4c,
                                        pos2ctx_map4x4, pos2ctx_map4x4, pos2ctx_map8x8i,pos2ctx_map8x4i,
                                        pos2ctx_map8x4i,pos2ctx_map4x4,
                                        pos2ctx_map4x4, pos2ctx_map4x4, pos2ctx_map8x8i,pos2ctx_map8x4i,
                                        pos2ctx_map8x4i,pos2ctx_map4x4};

//===== position -> ctx for LAST =====
static const byte  pos2ctx_last8x8 [] = { 0,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
                                          2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,
                                          3,  3,  3,  3,  3,  3,  3,  3,  4,  4,  4,  4,  4,  4,  4,  4,
                                          5,  5,  5,  5,  6,  6,  6,  6,  7,  7,  7,  7,  8,  8,  8,  8}; //  9 CTX
static const byte  pos2ctx_last8x4 [] = { 0,  1,  1,  1,  1,  1,  1,  1,  2,  2,  2,  2,  2,  2,  2,  2,
                                          3,  3,  3,  3,  4,  4,  4,  4,  5,  5,  6,  6,  7,  7,  8,  8}; //  9 CTX

static const byte  pos2ctx_last4x4 [] = { 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15}; // 15 CTX
static const byte  pos2ctx_last2x4c[] = { 0,  0,  1,  1,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2}; // 15 CTX
static const byte  pos2ctx_last4x4c[] = { 0,  0,  0,  0,  1,  1,  1,  1,  2,  2,  2,  2,  2,  2,  2,  2}; // 15 CTX
static const byte* pos2ctx_last    [] = {pos2ctx_last4x4, pos2ctx_last4x4, pos2ctx_last8x8, pos2ctx_last8x4,
                                         pos2ctx_last8x4, pos2ctx_last4x4, pos2ctx_last4x4, pos2ctx_last4x4,
                                         pos2ctx_last2x4c, pos2ctx_last4x4c,
                                         pos2ctx_last4x4, pos2ctx_last4x4, pos2ctx_last8x8,pos2ctx_last8x4,
                                         pos2ctx_last8x4, pos2ctx_last4x4,
                                         pos2ctx_last4x4, pos2ctx_last4x4, pos2ctx_last8x8,pos2ctx_last8x4,
                                         pos2ctx_last8x4, pos2ctx_last4x4};



/*!
 ************************************************************************
 * \brief
 *    Read Significance MAP
 ************************************************************************
 */
static int read_significance_map (Macroblock              *currMB,
                                  DecodingEnvironmentPtr  dep_dp,
                                  int                     type,
                                  int                     coeff[])
{
  Slice *currSlice = currMB->p_Slice;
  int               fld    = ( currSlice->structure!=FRAME || currMB->mb_field );
  const byte *pos2ctx_Map = (fld) ? pos2ctx_map_int[type] : pos2ctx_map[type];
  const byte *pos2ctx_Last = pos2ctx_last[type];

  BiContextTypePtr  map_ctx  = currSlice->tex_ctx->map_contexts [fld][type2ctx_map [type]];
  BiContextTypePtr  last_ctx = currSlice->tex_ctx->last_contexts[fld][type2ctx_last[type]];

  int   i;
  int   coeff_ctr = 0;
  int   i0        = 0;
  int   i1        = maxpos[type];


  if (!c1isdc[type])
  {
    ++i0; 
    ++i1; 
  }

  for (i=i0; i < i1; ++i) // if last coeff is reached, it has to be significant
  {
    //--- read significance symbol ---
    if (biari_decode_symbol   (dep_dp, map_ctx + pos2ctx_Map[i]))
    {
      *(coeff++) = 1;
      ++coeff_ctr;
      //--- read last coefficient symbol ---
      if (biari_decode_symbol (dep_dp, last_ctx + pos2ctx_Last[i]))
      {
        memset(coeff, 0, (i1 - i) * sizeof(int));
        return coeff_ctr;
      }
    }
    else
    {
      *(coeff++) = 0;
    }
  }
  //--- last coefficient must be significant if no last symbol was received ---
  if (i < i1 + 1)
  {
    *coeff = 1;
    ++coeff_ctr;
  }

  return coeff_ctr;
}



/*!
 ************************************************************************
 * \brief
 *    Read Levels
 ************************************************************************
 */
static void read_significant_coefficients (DecodingEnvironmentPtr  dep_dp,
                                           TextureInfoContexts    *tex_ctx,
                                           int                     type,
                                           int                    *coeff)
{
  BiContextType *one_contexts = tex_ctx->one_contexts[type2ctx_one[type]];
  BiContextType *abs_contexts = tex_ctx->abs_contexts[type2ctx_abs[type]];
  const short max_type = max_c2[type];
  int i = maxpos[type];
  int *cof = coeff + i;
  int   c1 = 1;
  int   c2 = 0;

  for (; i>=0; i--)
  {
    if (*cof != 0)
    {
      *cof += biari_decode_symbol (dep_dp, one_contexts + c1);

      if (*cof == 2)
      {        
        *cof += unary_exp_golomb_level_decode (dep_dp, abs_contexts + c2);
        c2 = imin (++c2, max_type);
        c1 = 0;
      }
      else if (c1)
      {
        c1 = imin (++c1, 4);
      }

      if (biari_decode_symbol_eq_prob(dep_dp))
      {
        *cof = - *cof;
      }
    }
    cof--;
  }
}


/*!
 ************************************************************************
 * \brief
 *    Read Block-Transform Coefficients
 ************************************************************************
 */
void readRunLevel_CABAC (Macroblock *currMB, 
                         SyntaxElement  *se,
                         DecodingEnvironmentPtr dep_dp)
{
  Slice *currSlice = currMB->p_Slice;
  int  *coeff_ctr = &currSlice->coeff_ctr;
  int  *coeff = currSlice->coeff;

  //--- read coefficients for whole block ---
  if (*coeff_ctr < 0)
  {
    //===== decode CBP-BIT =====
    if ((*coeff_ctr = currMB->read_and_store_CBP_block_bit (currMB, dep_dp, se->context) ) != 0)
    {
      //===== decode significance map =====
      *coeff_ctr = read_significance_map (currMB, dep_dp, se->context, coeff);

      //===== decode significant coefficients =====
      read_significant_coefficients    (dep_dp, currSlice->tex_ctx, se->context, coeff);
    }
  }

  //--- set run and level ---
  if (*coeff_ctr)
  {
    //--- set run and level (coefficient) ---
    for (se->value2 = 0; coeff[currSlice->pos] == 0; ++currSlice->pos, ++se->value2);
    se->value1 = coeff[currSlice->pos++];
  }
  else
  {
    //--- set run and level (EOB) ---
    se->value1 = se->value2 = 0;
  }
  //--- decrement coefficient counter and re-set position ---
  if ((*coeff_ctr)-- == 0) 
    currSlice->pos = 0;

#if TRACE
  fprintf(p_Dec->p_trace, "@%-6d %-53s %3d  %3d\n",symbolCount++, se->tracestring, se->value1,se->value2);
  fflush(p_Dec->p_trace);
#endif
}

/*!
 ************************************************************************
 * \brief
 *    arithmetic decoding
 ************************************************************************
 */
int readSyntaxElement_CABAC(Macroblock *currMB, SyntaxElement *se, DataPartition *this_dataPart)
{
  DecodingEnvironmentPtr dep_dp = &(this_dataPart->de_cabac);
  int curr_len = arideco_bits_read(dep_dp);

  // perform the actual decoding by calling the appropriate method
  se->reading(currMB, se, dep_dp);
  //read again and minus curr_len = arideco_bits_read(dep_dp); from above
  se->len = (arideco_bits_read(dep_dp) - curr_len);

#if (TRACE==2)
  fprintf(p_Dec->p_trace, "curr_len: %d\n",curr_len);
  fprintf(p_Dec->p_trace, "se_len: %d\n",se->len);
#endif

  return (se->len); 
}


/*!
 ************************************************************************
 * \brief
 *    decoding of unary binarization using one or 2 distinct
 *    models for the first and all remaining bins; no terminating
 *    "0" for max_symbol
 ***********************************************************************
 */
static unsigned int unary_bin_max_decode(DecodingEnvironmentPtr dep_dp,
                                  BiContextTypePtr ctx,
                                  int ctx_offset,
                                  unsigned int max_symbol)
{
  unsigned int symbol =  biari_decode_symbol(dep_dp, ctx );

  if (symbol == 0 || (max_symbol == 0))
    return symbol;
  else
  {    
    unsigned int l;
    ctx += ctx_offset;
    symbol = 0;
    do
    {
      l = biari_decode_symbol(dep_dp, ctx);
      ++symbol;
    }
    while( (l != 0) && (symbol < max_symbol) );

    if ((l != 0) && (symbol == max_symbol))
      ++symbol;
    return symbol;
  }
}


/*!
 ************************************************************************
 * \brief
 *    decoding of unary binarization using one or 2 distinct
 *    models for the first and all remaining bins
 ***********************************************************************
 */
static unsigned int unary_bin_decode(DecodingEnvironmentPtr dep_dp,
                                     BiContextTypePtr ctx,
                                     int ctx_offset)
{
  unsigned int symbol = biari_decode_symbol(dep_dp, ctx );

  if (symbol == 0)
    return 0;
  else
  {
    unsigned int l;
    ctx += ctx_offset;;
    symbol = 0;
    do
    {
      l = biari_decode_symbol(dep_dp, ctx);
      ++symbol;
    }
    while( l != 0 );
    return symbol;
  }
}


/*!
 ************************************************************************
 * \brief
 *    finding end of a slice in case this is not the end of a frame
 *
 * Unsure whether the "correction" below actually solves an off-by-one
 * problem or whether it introduces one in some cases :-(  Anyway,
 * with this change the bit stream format works with CABAC again.
 * StW, 8.7.02
 ************************************************************************
 */
int cabac_startcode_follows(Slice *currSlice, int eos_bit)
{
  unsigned int  bit;

  if( eos_bit )
  {
    const byte   *partMap    = assignSE2partition[currSlice->dp_mode];
    DataPartition *dP = &(currSlice->partArr[partMap[SE_MBTYPE]]);  
    DecodingEnvironmentPtr dep_dp = &(dP->de_cabac);

    bit = biari_decode_final (dep_dp); //GB

#if TRACE
    fprintf(p_Dec->p_trace, "@%-6d %-63s (%3d)\n",symbolCount++, "end_of_slice_flag", bit);
    fflush(p_Dec->p_trace);
#endif
  }
  else
  {
    bit = 0;
  }

  return (bit == 1 ? 1 : 0);
}

/*!
 ************************************************************************
 * \brief
 *    Exp Golomb binarization and decoding of a symbol
 *    with prob. of 0.5
 ************************************************************************
 */
static unsigned int exp_golomb_decode_eq_prob( DecodingEnvironmentPtr dep_dp,
                                              int k)
{
  unsigned int l;
  int symbol = 0;
  int binary_symbol = 0;

  do
  {
    l = biari_decode_symbol_eq_prob(dep_dp);
    if (l == 1)
    {
      symbol += (1<<k);
      ++k;
    }
  }
  while (l!=0);

  while (k--)                             //next binary part
    if (biari_decode_symbol_eq_prob(dep_dp)==1)
      binary_symbol |= (1<<k);

  return (unsigned int) (symbol + binary_symbol);
}


/*!
 ************************************************************************
 * \brief
 *    Exp-Golomb decoding for LEVELS
 ***********************************************************************
 */
static unsigned int unary_exp_golomb_level_decode( DecodingEnvironmentPtr dep_dp,
                                                  BiContextTypePtr ctx)
{
  unsigned int symbol = biari_decode_symbol(dep_dp, ctx );

  if (symbol==0)
    return 0;
  else
  {
    unsigned int l, k = 1;
    unsigned int exp_start = 13;

    symbol = 0;

    do
    {
      l=biari_decode_symbol(dep_dp, ctx);
      ++symbol;
      ++k;
    }
    while((l != 0) && (k != exp_start));
    if (l!=0)
      symbol += exp_golomb_decode_eq_prob(dep_dp,0)+1;
    return symbol;
  }
}




/*!
 ************************************************************************
 * \brief
 *    Exp-Golomb decoding for Motion Vectors
 ***********************************************************************
 */
static unsigned int unary_exp_golomb_mv_decode(DecodingEnvironmentPtr dep_dp,
                                               BiContextTypePtr ctx,
                                               unsigned int max_bin)
{
  unsigned int symbol = biari_decode_symbol(dep_dp, ctx );

  if (symbol == 0)
    return 0;
  else
  {
    unsigned int exp_start = 8;
    unsigned int l,k = 1;
    unsigned int bin = 1;

    symbol=0;

    ++ctx;
    do
    {
      l=biari_decode_symbol(dep_dp, ctx);
      if ((++bin)==2) ctx++;
      if (bin==max_bin) 
        ++ctx;
      ++symbol;
      ++k;
    }
    while((l!=0) && (k!=exp_start));
    if (l!=0)
      symbol += exp_golomb_decode_eq_prob(dep_dp,3) + 1;
    return symbol;
  }
}


/*!
 ************************************************************************
 * \brief
 *    Read I_PCM macroblock 
 ************************************************************************
*/
void readIPCM_CABAC(Slice *currSlice, struct datapartition_dec *dP)
{
  VideoParameters *p_Vid = currSlice->p_Vid;
  StorablePicture *dec_picture = currSlice->dec_picture;
  Bitstream* currStream = dP->bitstream;
  DecodingEnvironmentPtr dep = &(dP->de_cabac);
  byte *buf = currStream->streamBuffer;
  int BitstreamLengthInBits = (dP->bitstream->bitstream_length << 3) + 7;

  int val = 0;

  int bits_read = 0;
  int bitoffset, bitdepth;
  int uv, i, j;

  while (dep->DbitsLeft >= 8)
  {
    dep->Dvalue   >>= 8;
    dep->DbitsLeft -= 8;
    (*dep->Dcodestrm_len)--;
  }
  
  bitoffset = (*dep->Dcodestrm_len) << 3;

  // read luma values
  bitdepth = p_Vid->bitdepth_luma;
  for(i=0;i<MB_BLOCK_SIZE;++i)
  {
    for(j=0;j<MB_BLOCK_SIZE;++j)
    {
      bits_read += GetBits(buf, bitoffset, &val, BitstreamLengthInBits, bitdepth);
#if TRACE
      tracebits2("pcm_byte luma", bitdepth, val);
#endif
      currSlice->cof[0][i][j] = val;

      bitoffset += bitdepth;
    }
  }

  // read chroma values
  bitdepth = p_Vid->bitdepth_chroma;
  if ((dec_picture->chroma_format_idc != YUV400) && (p_Vid->separate_colour_plane_flag == 0))
  {
    for (uv = 1; uv < 3; ++uv)
    {
      for(i = 0; i < p_Vid->mb_cr_size_y; ++i)
      {
        for(j = 0; j < p_Vid->mb_cr_size_x; ++j)
        {
          bits_read += GetBits(buf, bitoffset, &val, BitstreamLengthInBits, bitdepth);
#if TRACE
          tracebits2("pcm_byte chroma", bitdepth, val);
#endif
          currSlice->cof[uv][i][j] = val;

          bitoffset += bitdepth;
        }
      }
    }
  }

  (*dep->Dcodestrm_len) += ( bits_read >> 3);
  if (bits_read & 7)
  {
    ++(*dep->Dcodestrm_len);
  }
}

