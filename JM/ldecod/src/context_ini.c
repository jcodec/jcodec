
/*!
 *************************************************************************************
 * \file context_ini.c
 *
 * \brief
 *    CABAC context initializations
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Detlev Marpe
 *    - Heiko Schwarz
 **************************************************************************************
 */

#define CONTEXT_INI_C

#include "defines.h"
#include "global.h"
#include "biaridecod.h"
#include "ctx_tables.h"


#define IBIARI_CTX_INIT2(ii,jj,ctx,tab,num, qp) \
{ \
  for (i=0; i<ii; ++i) \
  for (j=0; j<jj; ++j) \
  { \
  biari_init_context (qp, &(ctx[i][j]), tab ## _I[num][i][j]); \
  } \
}

#define PBIARI_CTX_INIT2(ii,jj,ctx,tab,num, qp) \
{ \
  for (i=0; i<ii; ++i) \
  for (j=0; j<jj; ++j) \
  { \
  biari_init_context (qp, &(ctx[i][j]), tab ## _P[num][i][j]); \
  } \
}


#define IBIARI_CTX_INIT1(jj,ctx,tab,num, qp) \
{ \
  for (j=0; j<jj; ++j) \
  { \
  biari_init_context (qp, &(ctx[j]), tab ## _I[num][0][j]); \
  } \
}


#define PBIARI_CTX_INIT1(jj,ctx,tab,num, qp) \
{ \
  { \
  for (j=0; j<jj; ++j) \
  { \
  biari_init_context (qp, &(ctx[j]), tab ## _P[num][0][j]); \
  } \
  } \
}

void init_contexts (Slice *currSlice)
{
  MotionInfoContexts*  mc = currSlice->mot_ctx;
  TextureInfoContexts* tc = currSlice->tex_ctx;
  int i, j;
  int qp = imax(0, currSlice->qp); //p_Vid->qp);
  int model_number = currSlice->model_number;

  //printf("%d -", p_Vid->currentSlice->model_number);

  //--- motion coding contexts ---
  if ((currSlice->slice_type == I_SLICE)||(currSlice->slice_type == SI_SLICE))
  {
    IBIARI_CTX_INIT2 (3, NUM_MB_TYPE_CTX,   mc->mb_type_contexts,     INIT_MB_TYPE,    model_number, qp);
    IBIARI_CTX_INIT2 (2, NUM_B8_TYPE_CTX,   mc->b8_type_contexts,     INIT_B8_TYPE,    model_number, qp);
    IBIARI_CTX_INIT2 (2, NUM_MV_RES_CTX,    mc->mv_res_contexts,      INIT_MV_RES,     model_number, qp);
    IBIARI_CTX_INIT2 (2, NUM_REF_NO_CTX,    mc->ref_no_contexts,      INIT_REF_NO,     model_number, qp);
    IBIARI_CTX_INIT1 (   NUM_DELTA_QP_CTX,  mc->delta_qp_contexts,    INIT_DELTA_QP,   model_number, qp);
    IBIARI_CTX_INIT1 (   NUM_MB_AFF_CTX,    mc->mb_aff_contexts,      INIT_MB_AFF,     model_number, qp);    

    //--- texture coding contexts ---
    IBIARI_CTX_INIT1 (   NUM_TRANSFORM_SIZE_CTX, tc->transform_size_contexts, INIT_TRANSFORM_SIZE, model_number, qp);
    IBIARI_CTX_INIT1 (                 NUM_IPR_CTX,  tc->ipr_contexts,     INIT_IPR,       model_number, qp);
    IBIARI_CTX_INIT1 (                 NUM_CIPR_CTX, tc->cipr_contexts,    INIT_CIPR,      model_number, qp);
    IBIARI_CTX_INIT2 (3,               NUM_CBP_CTX,  tc->cbp_contexts,     INIT_CBP,       model_number, qp);
    IBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_BCBP_CTX, tc->bcbp_contexts,    INIT_BCBP,      model_number, qp);
    IBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_MAP_CTX,  tc->map_contexts[0],  INIT_MAP,       model_number, qp);
#if ENABLE_FIELD_CTX
    IBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_MAP_CTX,  tc->map_contexts[1],  INIT_FLD_MAP,   model_number, qp);
    IBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_LAST_CTX, tc->last_contexts[1], INIT_FLD_LAST,  model_number, qp);
#endif
    IBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_LAST_CTX, tc->last_contexts[0], INIT_LAST,      model_number, qp);    
    IBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_ONE_CTX,  tc->one_contexts,     INIT_ONE,       model_number, qp);
    IBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_ABS_CTX,  tc->abs_contexts,     INIT_ABS,       model_number, qp);
  }
  else
  {
    PBIARI_CTX_INIT2 (3, NUM_MB_TYPE_CTX,   mc->mb_type_contexts,     INIT_MB_TYPE,    model_number, qp);
    PBIARI_CTX_INIT2 (2, NUM_B8_TYPE_CTX,   mc->b8_type_contexts,     INIT_B8_TYPE,    model_number, qp);
    PBIARI_CTX_INIT2 (2, NUM_MV_RES_CTX,    mc->mv_res_contexts,      INIT_MV_RES,     model_number, qp);
    PBIARI_CTX_INIT2 (2, NUM_REF_NO_CTX,    mc->ref_no_contexts,      INIT_REF_NO,     model_number, qp);
    PBIARI_CTX_INIT1 (   NUM_DELTA_QP_CTX,  mc->delta_qp_contexts,    INIT_DELTA_QP,   model_number, qp);
    PBIARI_CTX_INIT1 (   NUM_MB_AFF_CTX,    mc->mb_aff_contexts,      INIT_MB_AFF,     model_number, qp);    

    //--- texture coding contexts ---
    PBIARI_CTX_INIT1 (   NUM_TRANSFORM_SIZE_CTX, tc->transform_size_contexts, INIT_TRANSFORM_SIZE, model_number, qp);
    PBIARI_CTX_INIT1 (                 NUM_IPR_CTX,  tc->ipr_contexts,     INIT_IPR,       model_number, qp);
    PBIARI_CTX_INIT1 (                 NUM_CIPR_CTX, tc->cipr_contexts,    INIT_CIPR,      model_number, qp);
    PBIARI_CTX_INIT2 (3,               NUM_CBP_CTX,  tc->cbp_contexts,     INIT_CBP,       model_number, qp);
    PBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_BCBP_CTX, tc->bcbp_contexts,    INIT_BCBP,      model_number, qp);
    PBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_MAP_CTX,  tc->map_contexts[0],  INIT_MAP,       model_number, qp);
#if ENABLE_FIELD_CTX
    PBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_MAP_CTX,  tc->map_contexts[1],  INIT_FLD_MAP,   model_number, qp);
    PBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_LAST_CTX, tc->last_contexts[1], INIT_FLD_LAST,  model_number, qp);
#endif
    PBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_LAST_CTX, tc->last_contexts[0], INIT_LAST,      model_number, qp);    
    PBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_ONE_CTX,  tc->one_contexts,     INIT_ONE,       model_number, qp);
    PBIARI_CTX_INIT2 (NUM_BLOCK_TYPES, NUM_ABS_CTX,  tc->abs_contexts,     INIT_ABS,       model_number, qp);
  }
}

