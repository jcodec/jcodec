/*!
 ***********************************************************************
 * \file read_comp_cabac.c
 *
 * \brief
 *     Read Coefficient Components
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
#include "cabac.h"
#include "vlc.h"
#include "transform.h"

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
*    Get coefficients (run/level) of 4x4 blocks in a SMB
*    from the NAL (CABAC Mode)
************************************************************************
*/
static void read_comp_coeff_4x4_smb_CABAC (Macroblock *currMB, SyntaxElement *currSE, ColorPlane pl, int block_y, int block_x, int start_scan, int64 *cbp_blk)
{
  int i,j,k;
  int i0, j0;
  int level = 1;
  DataPartition *dP;
  //VideoParameters *p_Vid = currMB->p_Vid;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];

  const byte (*pos_scan4x4)[2] = ((currSlice->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];
  int **cof = currSlice->cof[pl];

  for (j = block_y; j < block_y + BLOCK_SIZE_8x8; j += 4)
  {
    currMB->subblock_y = j; // position for coeff_count ctx

    for (i = block_x; i < block_x + BLOCK_SIZE_8x8; i += 4)
    {
      currMB->subblock_x = i; // position for coeff_count ctx
      pos_scan_4x4 = pos_scan4x4[start_scan];
      level = 1;

      if (start_scan == 0)
      {
        /*
        * make distinction between INTRA and INTER coded
        * luminance coefficients
        */
        currSE->type = (currMB->is_intra_block ? SE_LUM_DC_INTRA : SE_LUM_DC_INTER);  
        dP = &(currSlice->partArr[partMap[currSE->type]]);
        if (dP->bitstream->ei_flag)  
          currSE->mapping = linfo_levrun_inter;
        else                                                     
          currSE->reading = readRunLevel_CABAC;

#if TRACE
        if (pl == PLANE_Y)
          sprintf(currSE->tracestring, "Luma sng ");
        else if (pl == PLANE_U)
          sprintf(currSE->tracestring, "Cb   sng ");
        else
          sprintf(currSE->tracestring, "Cr   sng ");  
#endif

        dP->readSyntaxElement(currMB, currSE, dP);
        level = currSE->value1;

        if (level != 0)    /* leave if level == 0 */
        {
          pos_scan_4x4 += 2 * currSE->value2;

          i0 = *pos_scan_4x4++;
          j0 = *pos_scan_4x4++;

          *cbp_blk |= i64_power2(j + (i >> 2)) ;
          //cof[j + j0][i + i0]= rshift_rnd_sf((level * InvLevelScale4x4[j0][i0]) << qp_per, 4);
          cof[j + j0][i + i0]= level;
          //currSlice->fcf[pl][j + j0][i + i0]= level;
        }
      }

      if (level != 0)
      {
        // make distinction between INTRA and INTER coded luminance coefficients
        currSE->type = (currMB->is_intra_block ? SE_LUM_AC_INTRA : SE_LUM_AC_INTER);  
        dP = &(currSlice->partArr[partMap[currSE->type]]);

        if (dP->bitstream->ei_flag)  
          currSE->mapping = linfo_levrun_inter;
        else                                                     
          currSE->reading = readRunLevel_CABAC;

        for(k = 1; (k < 17) && (level != 0); ++k)
        {
#if TRACE
          if (pl == PLANE_Y)
            sprintf(currSE->tracestring, "Luma sng ");
          else if (pl == PLANE_U)
            sprintf(currSE->tracestring, "Cb   sng ");
          else
            sprintf(currSE->tracestring, "Cr   sng ");  
#endif

          dP->readSyntaxElement(currMB, currSE, dP);
          level = currSE->value1;

          if (level != 0)    /* leave if level == 0 */
          {
            pos_scan_4x4 += 2 * currSE->value2;

            i0 = *pos_scan_4x4++;
            j0 = *pos_scan_4x4++;

            cof[j + j0][i + i0] = level;
            //currSlice->fcf[pl][j + j0][i + i0]= level;
          }
        }
      }
    }
  }
}

/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of all 4x4 blocks in a MB
*    from the NAL (CABAC Mode)
************************************************************************
*/
static void read_comp_coeff_4x4_CABAC (Macroblock *currMB, SyntaxElement *currSE, ColorPlane pl, int (*InvLevelScale4x4)[4], int qp_per, int cbp)
{
  Slice *currSlice = currMB->p_Slice;
  VideoParameters *p_Vid = currMB->p_Vid;
  int start_scan = IS_I16MB (currMB)? 1 : 0; 
  int block_y, block_x;
  int i, j;
  int64 *cbp_blk = &currMB->s_cbp[pl].blk;

  if( pl == PLANE_Y || (p_Vid->separate_colour_plane_flag != 0) )
    currSE->context = (IS_I16MB(currMB) ? LUMA_16AC: LUMA_4x4);
  else if (pl == PLANE_U)
    currSE->context = (IS_I16MB(currMB) ? CB_16AC: CB_4x4);
  else
    currSE->context = (IS_I16MB(currMB) ? CR_16AC: CR_4x4);  

  for (block_y = 0; block_y < MB_BLOCK_SIZE; block_y += BLOCK_SIZE_8x8) /* all modes */
  {
    int **cof = &currSlice->cof[pl][block_y];
    for (block_x = 0; block_x < MB_BLOCK_SIZE; block_x += BLOCK_SIZE_8x8)
    {
      if (cbp & (1 << ((block_y >> 2) + (block_x >> 3))))  // are there any coeff in current block at all
      {
        read_comp_coeff_4x4_smb_CABAC (currMB, currSE, pl, block_y, block_x, start_scan, cbp_blk);

        if (start_scan == 0)
        {
          for (j = 0; j < BLOCK_SIZE_8x8; ++j)
          {
            int *coef = &cof[j][block_x];
            int jj = j & 0x03;
            for (i = 0; i < BLOCK_SIZE_8x8; i+=4)
            {
              if (*coef)
                *coef = rshift_rnd_sf((*coef * InvLevelScale4x4[jj][0]) << qp_per, 4);
              coef++;
              if (*coef)
                *coef = rshift_rnd_sf((*coef * InvLevelScale4x4[jj][1]) << qp_per, 4);
              coef++;
              if (*coef)
                *coef = rshift_rnd_sf((*coef * InvLevelScale4x4[jj][2]) << qp_per, 4);
              coef++;
              if (*coef)
                *coef = rshift_rnd_sf((*coef * InvLevelScale4x4[jj][3]) << qp_per, 4);
              coef++;
            }
          }
        }
        else
        {                        
          for (j = 0; j < BLOCK_SIZE_8x8; ++j)
          {
            int *coef = &cof[j][block_x];
            int jj = j & 0x03;
            for (i = 0; i < BLOCK_SIZE_8x8; i += 4)
            {
              if ((jj != 0) && *coef)
                *coef= rshift_rnd_sf((*coef * InvLevelScale4x4[jj][0]) << qp_per, 4);
              coef++;
              if (*coef)
                *coef= rshift_rnd_sf((*coef * InvLevelScale4x4[jj][1]) << qp_per, 4);
              coef++;
              if (*coef)
                *coef= rshift_rnd_sf((*coef * InvLevelScale4x4[jj][2]) << qp_per, 4);
              coef++;
              if (*coef)
                *coef= rshift_rnd_sf((*coef * InvLevelScale4x4[jj][3]) << qp_per, 4);
              coef++;
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
*    Get coefficients (run/level) of all 4x4 blocks in a MB
*    from the NAL (CABAC Mode)
************************************************************************
*/
static void read_comp_coeff_4x4_CABAC_ls (Macroblock *currMB, SyntaxElement *currSE, ColorPlane pl, int (*InvLevelScale4x4)[4], int qp_per, int cbp)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  int start_scan = IS_I16MB (currMB)? 1 : 0; 
  int block_y, block_x;
  int64 *cbp_blk = &currMB->s_cbp[pl].blk;

  if( pl == PLANE_Y || (p_Vid->separate_colour_plane_flag != 0) )
    currSE->context = (IS_I16MB(currMB) ? LUMA_16AC: LUMA_4x4);
  else if (pl == PLANE_U)
    currSE->context = (IS_I16MB(currMB) ? CB_16AC: CB_4x4);
  else
    currSE->context = (IS_I16MB(currMB) ? CR_16AC: CR_4x4);  

  for (block_y = 0; block_y < MB_BLOCK_SIZE; block_y += BLOCK_SIZE_8x8) /* all modes */
  {
    for (block_x = 0; block_x < MB_BLOCK_SIZE; block_x += BLOCK_SIZE_8x8)
    {
      if (cbp & (1 << ((block_y >> 2) + (block_x >> 3))))  // are there any coeff in current block at all
      {
        read_comp_coeff_4x4_smb_CABAC (currMB, currSE, pl, block_y, block_x, start_scan, cbp_blk);
      }
    }
  }
}


/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of one 8x8 block
*    from the NAL (CABAC Mode)
************************************************************************
*/
static void readCompCoeff8x8_CABAC (Macroblock *currMB, SyntaxElement *currSE, ColorPlane pl, int b8)
{
  if (currMB->cbp & (1<<b8))  // are there any coefficients in the current block
  {
    VideoParameters *p_Vid = currMB->p_Vid;
    int transform_pl = (p_Vid->separate_colour_plane_flag != 0) ? currMB->p_Slice->colour_plane_id : pl;

    int **tcoeffs;
    int i,j,k;
    int level = 1;

    DataPartition *dP;
    Slice *currSlice = currMB->p_Slice;
    const byte *partMap = assignSE2partition[currSlice->dp_mode];
    int boff_x, boff_y;

    int64 cbp_mask = (int64) 51 << (4 * b8 - 2 * (b8 & 0x01)); // corresponds to 110011, as if all four 4x4 blocks contain coeff, shifted to block position            
    int64 *cur_cbp = &currMB->s_cbp[pl].blk;

    // select scan type
    const byte (*pos_scan8x8) = ((currSlice->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN8x8[0] : FIELD_SCAN8x8[0];

    int qp_per = p_Vid->qp_per_matrix[ currMB->qp_scaled[pl] ];
    int qp_rem = p_Vid->qp_rem_matrix[ currMB->qp_scaled[pl] ];
    
    int (*InvLevelScale8x8)[8] = (currMB->is_intra_block == TRUE) ? currSlice->InvLevelScale8x8_Intra[transform_pl][qp_rem] : currSlice->InvLevelScale8x8_Inter[transform_pl][qp_rem];

    // === set offset in current macroblock ===
    boff_x = (b8&0x01) << 3;
    boff_y = (b8 >> 1) << 3;
    tcoeffs = &currSlice->mb_rres[pl][boff_y];

    currMB->subblock_x = boff_x; // position for coeff_count ctx
    currMB->subblock_y = boff_y; // position for coeff_count ctx

    if (pl==PLANE_Y || (p_Vid->separate_colour_plane_flag != 0))  
      currSE->context = LUMA_8x8;
    else if (pl==PLANE_U)
      currSE->context = CB_8x8;
    else
      currSE->context = CR_8x8;  

    currSE->reading = readRunLevel_CABAC;

    // Read DC
    currSE->type = ((currMB->is_intra_block == 1) ? SE_LUM_DC_INTRA : SE_LUM_DC_INTER ); // Intra or Inter?
    dP = &(currSlice->partArr[partMap[currSE->type]]);

#if TRACE
    if (pl==PLANE_Y)
      sprintf(currSE->tracestring, "Luma8x8 DC sng ");
    else if (pl==PLANE_U)
      sprintf(currSE->tracestring, "Cb  8x8 DC sng "); 
    else 
      sprintf(currSE->tracestring, "Cr  8x8 DC sng "); 
#endif        

    dP->readSyntaxElement(currMB, currSE, dP);
    level = currSE->value1;

    //============ decode =============
    if (level != 0)    /* leave if level == 0 */
    {
      *cur_cbp |= cbp_mask; 

      pos_scan8x8 += 2 * (currSE->value2);

      i = *pos_scan8x8++;
      j = *pos_scan8x8++;

      tcoeffs[j][boff_x + i] = rshift_rnd_sf((level * InvLevelScale8x8[j][i]) << qp_per, 6); // dequantization
      //tcoeffs[ j][boff_x + i] = level;

      // AC coefficients
      currSE->type    = ((currMB->is_intra_block == 1) ? SE_LUM_AC_INTRA : SE_LUM_AC_INTER);
      dP = &(currSlice->partArr[partMap[currSE->type]]);

      for(k = 1;(k < 65) && (level != 0);++k)
      {
#if TRACE
        if (pl==PLANE_Y)
          sprintf(currSE->tracestring, "Luma8x8 sng ");
        else if (pl==PLANE_U)
          sprintf(currSE->tracestring, "Cb  8x8 sng "); 
        else 
          sprintf(currSE->tracestring, "Cr  8x8 sng "); 
#endif

        dP->readSyntaxElement(currMB, currSE, dP);
        level = currSE->value1;

        //============ decode =============
        if (level != 0)    /* leave if level == 0 */
        {
          pos_scan8x8 += 2 * (currSE->value2);

          i = *pos_scan8x8++;
          j = *pos_scan8x8++;

          tcoeffs[ j][boff_x + i] = rshift_rnd_sf((level * InvLevelScale8x8[j][i]) << qp_per, 6); // dequantization
          //tcoeffs[ j][boff_x + i] = level;
        }
      }
      /*
      for (j = 0; j < 8; j++)
      {
      for (i = 0; i < 8; i++)
      {
      if (tcoeffs[ j][boff_x + i])
      tcoeffs[ j][boff_x + i] = rshift_rnd_sf((tcoeffs[ j][boff_x + i] * InvLevelScale8x8[j][i]) << qp_per, 6); // dequantization
      }
      }
      */
    }        
  }
}

/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of one 8x8 block
*    from the NAL (CABAC Mode - lossless)
************************************************************************
*/
static void readCompCoeff8x8_CABAC_lossless (Macroblock *currMB, SyntaxElement *currSE, ColorPlane pl, int b8)
{
  if (currMB->cbp & (1<<b8))  // are there any coefficients in the current block
  {
    VideoParameters *p_Vid = currMB->p_Vid;

    int **tcoeffs;
    int i,j,k;
    int level = 1;

    DataPartition *dP;
    Slice *currSlice = currMB->p_Slice;
    const byte *partMap = assignSE2partition[currSlice->dp_mode];
    int boff_x, boff_y;

    int64 cbp_mask = (int64) 51 << (4 * b8 - 2 * (b8 & 0x01)); // corresponds to 110011, as if all four 4x4 blocks contain coeff, shifted to block position            
    int64 *cur_cbp = &currMB->s_cbp[pl].blk;

    // select scan type
    const byte (*pos_scan8x8) = ((currSlice->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN8x8[0] : FIELD_SCAN8x8[0];

    // === set offset in current macroblock ===
    boff_x = (b8&0x01) << 3;
    boff_y = (b8 >> 1) << 3;
    tcoeffs = &currSlice->mb_rres[pl][boff_y];

    currMB->subblock_x = boff_x; // position for coeff_count ctx
    currMB->subblock_y = boff_y; // position for coeff_count ctx

    if (pl==PLANE_Y || (p_Vid->separate_colour_plane_flag != 0))  
      currSE->context = LUMA_8x8;
    else if (pl==PLANE_U)
      currSE->context = CB_8x8;
    else
      currSE->context = CR_8x8;  

    currSE->reading = readRunLevel_CABAC;

    for(k=0; (k < 65) && (level != 0);++k)
    {
      //============ read =============
      /*
      * make distinction between INTRA and INTER coded
      * luminance coefficients
      */

      currSE->type    = ((currMB->is_intra_block == 1)
        ? (k==0 ? SE_LUM_DC_INTRA : SE_LUM_AC_INTRA) 
        : (k==0 ? SE_LUM_DC_INTER : SE_LUM_AC_INTER));

#if TRACE
      if (pl==PLANE_Y)
        sprintf(currSE->tracestring, "Luma8x8 sng ");
      else if (pl==PLANE_U)
        sprintf(currSE->tracestring, "Cb  8x8 sng "); 
      else 
        sprintf(currSE->tracestring, "Cr  8x8 sng "); 
#endif

      dP = &(currSlice->partArr[partMap[currSE->type]]);
      currSE->reading = readRunLevel_CABAC;

      dP->readSyntaxElement(currMB, currSE, dP);
      level = currSE->value1;

      //============ decode =============
      if (level != 0)    /* leave if level == 0 */
      {
        pos_scan8x8 += 2 * (currSE->value2);

        i = *pos_scan8x8++;
        j = *pos_scan8x8++;

        *cur_cbp |= cbp_mask;

        tcoeffs[j][boff_x + i] = level;
      }
    }
  }
}


/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of 8x8 blocks in a MB
*    from the NAL (CABAC Mode)
************************************************************************
*/
static void read_comp_coeff_8x8_MB_CABAC (Macroblock *currMB, SyntaxElement *currSE, ColorPlane pl)
{
  //======= 8x8 transform size & CABAC ========
  readCompCoeff8x8_CABAC (currMB, currSE, pl, 0); 
  readCompCoeff8x8_CABAC (currMB, currSE, pl, 1); 
  readCompCoeff8x8_CABAC (currMB, currSE, pl, 2); 
  readCompCoeff8x8_CABAC (currMB, currSE, pl, 3); 
}


/*!
************************************************************************
* \brief
*    Get coefficients (run/level) of 8x8 blocks in a MB
*    from the NAL (CABAC Mode)
************************************************************************
*/
static void read_comp_coeff_8x8_MB_CABAC_ls (Macroblock *currMB, SyntaxElement *currSE, ColorPlane pl)
{
  //======= 8x8 transform size & CABAC ========
  readCompCoeff8x8_CABAC_lossless (currMB, currSE, pl, 0); 
  readCompCoeff8x8_CABAC_lossless (currMB, currSE, pl, 1); 
  readCompCoeff8x8_CABAC_lossless (currMB, currSE, pl, 2); 
  readCompCoeff8x8_CABAC_lossless (currMB, currSE, pl, 3); 
}


/*!
 ************************************************************************
 * \brief
 *    Get coded block pattern and coefficients (run/level)
 *    from the NAL
 ************************************************************************
 */
static void read_CBP_and_coeffs_from_NAL_CABAC_420(Macroblock *currMB)
{
  int i,j;
  int level;
  int cbp;
  SyntaxElement currSE;
  DataPartition *dP = NULL;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  int i0, j0;

  int qp_per, qp_rem;
  VideoParameters *p_Vid = currMB->p_Vid;
  int smb = ((p_Vid->type==SP_SLICE) && (currMB->is_intra_block == FALSE)) || (p_Vid->type == SI_SLICE && currMB->mb_type == SI4MB);

  int qp_per_uv[2];
  int qp_rem_uv[2];

  int intra = (currMB->is_intra_block == TRUE);  

  StorablePicture *dec_picture = currSlice->dec_picture;
  int yuv = dec_picture->chroma_format_idc - 1;

  int (*InvLevelScale4x4)[4] = NULL;

  // select scan type
  const byte (*pos_scan4x4)[2] = ((currSlice->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];

  if (!IS_I16MB (currMB))
  {
    int need_transform_size_flag;
    //=====   C B P   =====
    //---------------------
    currSE.type = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB) 
      ? SE_CBP_INTRA
      : SE_CBP_INTER;

    dP = &(currSlice->partArr[partMap[currSE.type]]);

    if (dP->bitstream->ei_flag)
    {
      currSE.mapping = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB)
        ? currSlice->linfo_cbp_intra
        : currSlice->linfo_cbp_inter;
    }
    else
    {
      currSE.reading = read_CBP_CABAC;
    }

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
      currSE.reading = readMB_transform_size_flag_CABAC;
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      if (dP->bitstream->ei_flag)
      {
        currSE.len = 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
      } 
      else
      {
        dP->readSyntaxElement(currMB, &currSE, dP);
      }
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
  else // read DC coeffs for new intra modes
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
      int **cof = currSlice->cof[0];
      int k;
      pos_scan_4x4 = pos_scan4x4[0];

      currSE.type = SE_LUM_DC_INTRA;
      dP = &(currSlice->partArr[partMap[currSE.type]]);

      currSE.context      = LUMA_16DC;
      currSE.type         = SE_LUM_DC_INTRA;

      if (dP->bitstream->ei_flag)
      {
        currSE.mapping = linfo_levrun_inter;
      }
      else
      {
        currSE.reading = readRunLevel_CABAC;
      }

      level = 1;                            // just to get inside the loop

      for(k = 0; (k < 17) && (level != 0); ++k)
      {
#if TRACE
        snprintf(currSE.tracestring, TRACESTRING_SIZE, "DC luma 16x16 ");
#endif
        dP->readSyntaxElement(currMB, &currSE, dP);
        level = currSE.value1;

        if (level != 0)    /* leave if level == 0 */
        {
          pos_scan_4x4 += (2 * currSE.value2);

          i0 = ((*pos_scan_4x4++) << 2);
          j0 = ((*pos_scan_4x4++) << 2);

          cof[j0][i0] = level;// add new intra DC coeff
          //currSlice->fcf[0][j0][i0] = level;// add new intra DC coeff
        }
      }

      if(currMB->is_lossless == FALSE)
        itrans_2(currMB, (ColorPlane) currSlice->colour_plane_id);// transform new intra DC
    }
  }

  update_qp(currMB, currSlice->qp);

  qp_per = p_Vid->qp_per_matrix[ currMB->qp_scaled[currSlice->colour_plane_id] ];
  qp_rem = p_Vid->qp_rem_matrix[ currMB->qp_scaled[currSlice->colour_plane_id] ];

  // luma coefficients
  //======= Other Modes & CABAC ========
  //------------------------------------          
  if (cbp)
  {
    if(currMB->luma_transform_size_8x8_flag) 
    {
      //======= 8x8 transform size & CABAC ========
      currMB->read_comp_coeff_8x8_CABAC (currMB, &currSE, PLANE_Y); 
    }
    else
    {
      InvLevelScale4x4 = intra? currSlice->InvLevelScale4x4_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale4x4_Inter[currSlice->colour_plane_id][qp_rem];
      currMB->read_comp_coeff_4x4_CABAC (currMB, &currSE, PLANE_Y, InvLevelScale4x4, qp_per, cbp);        
    }
  }

  //init quant parameters for chroma 
  for(i=0; i < 2; ++i)
  {
    qp_per_uv[i] = p_Vid->qp_per_matrix[ currMB->qp_scaled[i + 1] ];
    qp_rem_uv[i] = p_Vid->qp_rem_matrix[ currMB->qp_scaled[i + 1] ];
  }

  //========================== CHROMA DC ============================
  //-----------------------------------------------------------------
  // chroma DC coeff
  if(cbp>15)
  {
    CBPStructure  *s_cbp = &currMB->s_cbp[0];
    int uv, ll, k, coef_ctr;

    for (ll = 0; ll < 3; ll += 2)
    {
      uv = ll >> 1;

      InvLevelScale4x4 = intra ? currSlice->InvLevelScale4x4_Intra[uv + 1][qp_rem_uv[uv]] : currSlice->InvLevelScale4x4_Inter[uv + 1][qp_rem_uv[uv]];
      //===================== CHROMA DC YUV420 ======================
      memset(currSlice->cofu, 0, 4 *sizeof(int));
      coef_ctr=-1;

      level = 1;
      currMB->is_v_block  = ll;
      currSE.context      = CHROMA_DC;
      currSE.type         = (intra ? SE_CHR_DC_INTRA : SE_CHR_DC_INTER);

      dP = &(currSlice->partArr[partMap[currSE.type]]);

      if (dP->bitstream->ei_flag)
        currSE.mapping = linfo_levrun_c2x2;
      else
        currSE.reading = readRunLevel_CABAC;

      for(k = 0; (k < (p_Vid->num_cdc_coeff + 1))&&(level!=0);++k)
      {
#if TRACE
        snprintf(currSE.tracestring, TRACESTRING_SIZE, "2x2 DC Chroma ");
#endif

        dP->readSyntaxElement(currMB, &currSE, dP);
        level = currSE.value1;

        if (level != 0)
        {
          s_cbp->blk |= 0xf0000 << (ll<<1) ;
          coef_ctr += currSE.value2 + 1;

          // Bug: currSlice->cofu has only 4 entries, hence coef_ctr MUST be <4 (which is
          // caught by the assert().  If it is bigger than 4, it starts patching the
          // p_Vid->predmode pointer, which leads to bugs later on.
          //
          // This assert() should be left in the code, because it captures a very likely
          // bug early when testing in error prone environments (or when testing NAL
          // functionality).

          assert (coef_ctr < p_Vid->num_cdc_coeff);
          currSlice->cofu[coef_ctr] = level;
        }
      }


      if (smb || (currMB->is_lossless == TRUE)) // check to see if MB type is SPred or SIntra4x4
      {        
        currSlice->cof[uv + 1][0][0] = currSlice->cofu[0];
        currSlice->cof[uv + 1][0][4] = currSlice->cofu[1];
        currSlice->cof[uv + 1][4][0] = currSlice->cofu[2];
        currSlice->cof[uv + 1][4][4] = currSlice->cofu[3];
        //currSlice->fcf[uv + 1][0][0] = currSlice->cofu[0];
        //currSlice->fcf[uv + 1][4][0] = currSlice->cofu[1];
        //currSlice->fcf[uv + 1][0][4] = currSlice->cofu[2];
        //currSlice->fcf[uv + 1][4][4] = currSlice->cofu[3];
      }
      else
      {
        int temp[4];
        int scale_dc = InvLevelScale4x4[0][0];
        int **cof = currSlice->cof[uv + 1];

        ihadamard2x2(currSlice->cofu, temp);
        
        //currSlice->fcf[uv + 1][0][0] = temp[0];
        //currSlice->fcf[uv + 1][0][4] = temp[1];
        //currSlice->fcf[uv + 1][4][0] = temp[2];
        //currSlice->fcf[uv + 1][4][4] = temp[3];

        cof[0][0] = (((temp[0] * scale_dc) << qp_per_uv[uv]) >> 5);
        cof[0][4] = (((temp[1] * scale_dc) << qp_per_uv[uv]) >> 5);
        cof[4][0] = (((temp[2] * scale_dc) << qp_per_uv[uv]) >> 5);
        cof[4][4] = (((temp[3] * scale_dc) << qp_per_uv[uv]) >> 5);
      }          
    }      
  }

  //========================== CHROMA AC ============================
  //-----------------------------------------------------------------
  // chroma AC coeff, all zero fram start_scan
  if (cbp >31)
  {
    currSE.context      = CHROMA_AC;
    currSE.type         = (currMB->is_intra_block ? SE_CHR_AC_INTRA : SE_CHR_AC_INTER);

    dP = &(currSlice->partArr[partMap[currSE.type]]);

    if (dP->bitstream->ei_flag)
      currSE.mapping = linfo_levrun_inter;
    else
      currSE.reading = readRunLevel_CABAC;

    if(currMB->is_lossless == FALSE)
    {
      int b4, b8, uv, k;
      int **cof;
      CBPStructure  *s_cbp = &currMB->s_cbp[0];
      for (b8=0; b8 < p_Vid->num_blk8x8_uv; ++b8)
      {
        currMB->is_v_block = uv = (b8 > ((p_Vid->num_uv_blocks) - 1 ));
        InvLevelScale4x4 = intra ? currSlice->InvLevelScale4x4_Intra[uv + 1][qp_rem_uv[uv]] : currSlice->InvLevelScale4x4_Inter[uv + 1][qp_rem_uv[uv]];
        cof = currSlice->cof[uv + 1];

        for (b4 = 0; b4 < 4; ++b4)
        {
          i = cofuv_blk_x[yuv][b8][b4];
          j = cofuv_blk_y[yuv][b8][b4];

          currMB->subblock_y = subblk_offset_y[yuv][b8][b4];
          currMB->subblock_x = subblk_offset_x[yuv][b8][b4];

          pos_scan_4x4 = pos_scan4x4[1];
          level = 1;

          for(k = 0; (k < 16) && (level != 0);++k)
          {
#if TRACE
            snprintf(currSE.tracestring, TRACESTRING_SIZE, "AC Chroma ");
#endif

            dP->readSyntaxElement(currMB, &currSE, dP);
            level = currSE.value1;

            if (level != 0)
            {
              s_cbp->blk |= i64_power2(cbp_blk_chroma[b8][b4]);
              pos_scan_4x4 += (currSE.value2 << 1);

              i0 = *pos_scan_4x4++;
              j0 = *pos_scan_4x4++;

              cof[(j<<2) + j0][(i<<2) + i0] = rshift_rnd_sf((level * InvLevelScale4x4[j0][i0])<<qp_per_uv[uv], 4);
              //currSlice->fcf[uv + 1][(j<<2) + j0][(i<<2) + i0] = level;
            }
          } //for(k=0;(k<16)&&(level!=0);++k)
        }
      }
    }
    else
    {
      CBPStructure  *s_cbp = &currMB->s_cbp[0];
      int b4, b8, k;
      int uv;
      for (b8=0; b8 < p_Vid->num_blk8x8_uv; ++b8)
      {
        currMB->is_v_block = uv = (b8 > ((p_Vid->num_uv_blocks) - 1 ));

        for (b4=0; b4 < 4; ++b4)
        {
          i = cofuv_blk_x[yuv][b8][b4];
          j = cofuv_blk_y[yuv][b8][b4];

          pos_scan_4x4 = pos_scan4x4[1];
          level=1;

          currMB->subblock_y = subblk_offset_y[yuv][b8][b4];
          currMB->subblock_x = subblk_offset_x[yuv][b8][b4];

          for(k=0;(k<16)&&(level!=0);++k)
          {
#if TRACE
            snprintf(currSE.tracestring, TRACESTRING_SIZE, "AC Chroma ");
#endif
            dP->readSyntaxElement(currMB, &currSE, dP);
            level = currSE.value1;

            if (level != 0)
            {
              s_cbp->blk |= i64_power2(cbp_blk_chroma[b8][b4]);
              pos_scan_4x4 += (currSE.value2 << 1);

              i0 = *pos_scan_4x4++;
              j0 = *pos_scan_4x4++;

              currSlice->cof[uv + 1][(j<<2) + j0][(i<<2) + i0] = level;
              //currSlice->fcf[uv + 1][(j<<2) + j0][(i<<2) + i0] = level;
            }
          } 
        }
      } 
    } //for (b4=0; b4 < 4; b4++)      
  }  
}


/*!
 ************************************************************************
 * \brief
 *    Get coded block pattern and coefficients (run/level)
 *    from the NAL
 ************************************************************************
 */
static void read_CBP_and_coeffs_from_NAL_CABAC_400(Macroblock *currMB)
{
  int k;
  int level;
  int cbp;
  SyntaxElement currSE;
  DataPartition *dP = NULL;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  int i0, j0;

  int qp_per, qp_rem;
  VideoParameters *p_Vid = currMB->p_Vid;

  int intra = (currMB->is_intra_block == TRUE);

  int need_transform_size_flag;

  int (*InvLevelScale4x4)[4] = NULL;
  // select scan type
  const byte (*pos_scan4x4)[2] = ((currSlice->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
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

    if (dP->bitstream->ei_flag)
    {
      currSE.mapping = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB)
        ? currSlice->linfo_cbp_intra
        : currSlice->linfo_cbp_inter;
    }
    else
    {
      currSE.reading = read_CBP_CABAC;
    }

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
      currSE.reading = readMB_transform_size_flag_CABAC;
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      if (dP->bitstream->ei_flag)
      {
        currSE.len = 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
      } 
      else
      {
        dP->readSyntaxElement(currMB, &currSE, dP);
      }
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
  else // read DC coeffs for new intra modes
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

      {
        currSE.type = SE_LUM_DC_INTRA;
        dP = &(currSlice->partArr[partMap[currSE.type]]);

        currSE.context      = LUMA_16DC;
        currSE.type         = SE_LUM_DC_INTRA;

        if (dP->bitstream->ei_flag)
        {
          currSE.mapping = linfo_levrun_inter;
        }
        else
        {
          currSE.reading = readRunLevel_CABAC;
        }

        level = 1;                            // just to get inside the loop

        for(k = 0; (k < 17) && (level != 0); ++k)
        {
#if TRACE
          snprintf(currSE.tracestring, TRACESTRING_SIZE, "DC luma 16x16 ");
#endif
          dP->readSyntaxElement(currMB, &currSE, dP);
          level = currSE.value1;

          if (level != 0)    /* leave if level == 0 */
          {
            pos_scan_4x4 += (2 * currSE.value2);

            i0 = ((*pos_scan_4x4++) << 2);
            j0 = ((*pos_scan_4x4++) << 2);

            currSlice->cof[0][j0][i0] = level;// add new intra DC coeff
            //currSlice->fcf[0][j0][i0] = level;// add new intra DC coeff
          }
        }
      }

      if(currMB->is_lossless == FALSE)
        itrans_2(currMB, (ColorPlane) currSlice->colour_plane_id);// transform new intra DC
    }
  }

  update_qp(currMB, currSlice->qp);

  qp_per = p_Vid->qp_per_matrix[ currMB->qp_scaled[PLANE_Y] ];
  qp_rem = p_Vid->qp_rem_matrix[ currMB->qp_scaled[PLANE_Y] ];

  //======= Other Modes & CABAC ========
  //------------------------------------          
  if (cbp)
  {
    if(currMB->luma_transform_size_8x8_flag) 
    {
      //======= 8x8 transform size & CABAC ========
      currMB->read_comp_coeff_8x8_CABAC (currMB, &currSE, PLANE_Y); 
    }
    else
    {
      InvLevelScale4x4 = intra? currSlice->InvLevelScale4x4_Intra[currSlice->colour_plane_id][qp_rem] : currSlice->InvLevelScale4x4_Inter[currSlice->colour_plane_id][qp_rem];
      currMB->read_comp_coeff_4x4_CABAC (currMB, &currSE, PLANE_Y, InvLevelScale4x4, qp_per, cbp);        
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
static void read_CBP_and_coeffs_from_NAL_CABAC_444(Macroblock *currMB)
{
  int i, k;
  int level;
  int cbp;
  SyntaxElement currSE;
  DataPartition *dP = NULL;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  int coef_ctr, i0, j0;

  int qp_per, qp_rem;
  VideoParameters *p_Vid = currMB->p_Vid;

  int uv; 
  int qp_per_uv[2];
  int qp_rem_uv[2];

  int intra = (currMB->is_intra_block == TRUE);


  int need_transform_size_flag;

  int (*InvLevelScale4x4)[4] = NULL;
  // select scan type
  const byte (*pos_scan4x4)[2] = ((currSlice->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];

  // QPI
  //init constants for every chroma qp offset
  for (i=0; i<2; ++i)
  {
    qp_per_uv[i] = p_Vid->qp_per_matrix[ currMB->qp_scaled[i + 1] ];
    qp_rem_uv[i] = p_Vid->qp_rem_matrix[ currMB->qp_scaled[i + 1] ];
  }


  // read CBP if not new intra mode
  if (!IS_I16MB (currMB))
  {
    //=====   C B P   =====
    //---------------------
    currSE.type = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB) 
      ? SE_CBP_INTRA
      : SE_CBP_INTER;

    dP = &(currSlice->partArr[partMap[currSE.type]]);

    if (dP->bitstream->ei_flag)
    {
      currSE.mapping = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB)
        ? currSlice->linfo_cbp_intra
        : currSlice->linfo_cbp_inter;
    }
    else
    {
      currSE.reading = read_CBP_CABAC;
    }

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
      currSE.reading = readMB_transform_size_flag_CABAC;
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      if (dP->bitstream->ei_flag)
      {
        currSE.len = 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
      } 
      else
      {
        dP->readSyntaxElement(currMB, &currSE, dP);
      }
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
  else // read DC coeffs for new intra modes
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

      {
        currSE.type = SE_LUM_DC_INTRA;
        dP = &(currSlice->partArr[partMap[currSE.type]]);

        currSE.context      = LUMA_16DC;
        currSE.type         = SE_LUM_DC_INTRA;

        if (dP->bitstream->ei_flag)
        {
          currSE.mapping = linfo_levrun_inter;
        }
        else
        {
          currSE.reading = readRunLevel_CABAC;
        }

        level = 1;                            // just to get inside the loop

        for(k = 0; (k < 17) && (level != 0); ++k)
        {
#if TRACE
          snprintf(currSE.tracestring, TRACESTRING_SIZE, "DC luma 16x16 ");
#endif
          dP->readSyntaxElement(currMB, &currSE, dP);
          level = currSE.value1;

          if (level != 0)    /* leave if level == 0 */
          {
            pos_scan_4x4 += (2 * currSE.value2);

            i0 = ((*pos_scan_4x4++) << 2);
            j0 = ((*pos_scan_4x4++) << 2);

            currSlice->cof[0][j0][i0] = level;// add new intra DC coeff
            //currSlice->fcf[0][j0][i0] = level;// add new intra DC coeff
          }
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

  // luma coefficients
  {
    //======= Other Modes & CABAC ========
    //------------------------------------          
    if (cbp)
    {
      if(currMB->luma_transform_size_8x8_flag) 
      {
        //======= 8x8 transform size & CABAC ========
        currMB->read_comp_coeff_8x8_CABAC (currMB, &currSE, PLANE_Y); 
      }
      else
      {
        currMB->read_comp_coeff_4x4_CABAC (currMB, &currSE, PLANE_Y, InvLevelScale4x4, qp_per, cbp);        
      }
    }
  }

  for (uv = 0; uv < 2; ++uv )
  {
    /*----------------------16x16DC Luma_Add----------------------*/
    if (IS_I16MB (currMB)) // read DC coeffs for new intra modes       
    {
      {              
        currSE.type = SE_LUM_DC_INTRA;
        dP = &(currSlice->partArr[partMap[currSE.type]]);

        if( (p_Vid->separate_colour_plane_flag != 0) )
          currSE.context = LUMA_16DC; 
        else
          currSE.context = (uv==0) ? CB_16DC : CR_16DC;

        if (dP->bitstream->ei_flag)
        {
          currSE.mapping = linfo_levrun_inter;
        }
        else
        {
          currSE.reading = readRunLevel_CABAC;
        }

        coef_ctr = -1;
        level = 1;                            // just to get inside the loop

        for(k=0;(k<17) && (level!=0);++k)
        {
#if TRACE
          if (uv == 0)
            snprintf(currSE.tracestring, TRACESTRING_SIZE, "DC Cb   16x16 "); 
          else
            snprintf(currSE.tracestring, TRACESTRING_SIZE, "DC Cr   16x16 ");
#endif

          dP->readSyntaxElement(currMB, &currSE, dP);
          level = currSE.value1;

          if (level != 0)                     // leave if level == 0
          {
            coef_ctr += currSE.value2 + 1;

            i0 = pos_scan4x4[coef_ctr][0];
            j0 = pos_scan4x4[coef_ctr][1];
            currSlice->cof[uv + 1][j0<<2][i0<<2] = level;
            //currSlice->fcf[uv + 1][j0<<2][i0<<2] = level;
          }                        
        } //k loop
      } // else CAVLC

      if(currMB->is_lossless == FALSE)
      {
        itrans_2(currMB, (ColorPlane) (uv + 1)); // transform new intra DC
      }
    } //IS_I16MB

    update_qp(currMB, currSlice->qp);

    qp_per = p_Vid->qp_per_matrix[ (currSlice->qp + p_Vid->bitdepth_luma_qp_scale) ];
    qp_rem = p_Vid->qp_rem_matrix[ (currSlice->qp + p_Vid->bitdepth_luma_qp_scale) ];

    //init constants for every chroma qp offset
    qp_per_uv[uv] = p_Vid->qp_per_matrix[ (currMB->qpc[uv] + p_Vid->bitdepth_chroma_qp_scale) ];
    qp_rem_uv[uv] = p_Vid->qp_rem_matrix[ (currMB->qpc[uv] + p_Vid->bitdepth_chroma_qp_scale) ];

    InvLevelScale4x4 = intra? currSlice->InvLevelScale4x4_Intra[uv + 1][qp_rem_uv[uv]] : currSlice->InvLevelScale4x4_Inter[uv + 1][qp_rem_uv[uv]];

    {  
      if (cbp)
      {
        if(currMB->luma_transform_size_8x8_flag) 
        {
          //======= 8x8 transform size & CABAC ========
          currMB->read_comp_coeff_8x8_CABAC (currMB, &currSE, (ColorPlane) (PLANE_U + uv)); 
        }
        else //4x4
        {        
          currMB->read_comp_coeff_4x4_CABAC (currMB, &currSE, (ColorPlane) (PLANE_U + uv), InvLevelScale4x4,  qp_per_uv[uv], cbp);
        }
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
static void read_CBP_and_coeffs_from_NAL_CABAC_422(Macroblock *currMB)
{
  int i,j,k;
  int level;
  int cbp;
  SyntaxElement currSE;
  DataPartition *dP = NULL;
  Slice *currSlice = currMB->p_Slice;
  const byte *partMap = assignSE2partition[currSlice->dp_mode];
  int coef_ctr, i0, j0, b8;
  int ll;

  int qp_per, qp_rem;
  VideoParameters *p_Vid = currMB->p_Vid;

  int uv; 
  int qp_per_uv[2];
  int qp_rem_uv[2];

  int intra = (currMB->is_intra_block == TRUE);

  int b4;
  StorablePicture *dec_picture = currSlice->dec_picture;
  int yuv = dec_picture->chroma_format_idc - 1;
  int m6[4];

  int need_transform_size_flag;

  int (*InvLevelScale4x4)[4] = NULL;
  // select scan type
  const byte (*pos_scan4x4)[2] = ((currSlice->structure == FRAME) && (!currMB->mb_field)) ? SNGL_SCAN : FIELD_SCAN;
  const byte *pos_scan_4x4 = pos_scan4x4[0];

  // QPI
  //init constants for every chroma qp offset
  for (i=0; i<2; ++i)
  {
    qp_per_uv[i] = p_Vid->qp_per_matrix[ currMB->qp_scaled[i + 1] ];
    qp_rem_uv[i] = p_Vid->qp_rem_matrix[ currMB->qp_scaled[i + 1] ];
  }

  // read CBP if not new intra mode
  if (!IS_I16MB (currMB))
  {
    //=====   C B P   =====
    //---------------------
    currSE.type = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB) 
      ? SE_CBP_INTRA
      : SE_CBP_INTER;

    dP = &(currSlice->partArr[partMap[currSE.type]]);

    if (dP->bitstream->ei_flag)
    {
      currSE.mapping = (currMB->mb_type == I4MB || currMB->mb_type == SI4MB || currMB->mb_type == I8MB)
        ? currSlice->linfo_cbp_intra
        : currSlice->linfo_cbp_inter;
    }
    else
    {
      currSE.reading = read_CBP_CABAC;
    }

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
      currSE.reading = readMB_transform_size_flag_CABAC;
      TRACE_STRING("transform_size_8x8_flag");

      // read CAVLC transform_size_8x8_flag
      if (dP->bitstream->ei_flag)
      {
        currSE.len = 1;
        readSyntaxElement_FLC(&currSE, dP->bitstream);
      } 
      else
      {
        dP->readSyntaxElement(currMB, &currSE, dP);
      }
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
  else // read DC coeffs for new intra modes
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

      {
        currSE.type = SE_LUM_DC_INTRA;
        dP = &(currSlice->partArr[partMap[currSE.type]]);

        currSE.context      = LUMA_16DC;
        currSE.type         = SE_LUM_DC_INTRA;

        if (dP->bitstream->ei_flag)
        {
          currSE.mapping = linfo_levrun_inter;
        }
        else
        {
          currSE.reading = readRunLevel_CABAC;
        }

        level = 1;                            // just to get inside the loop

        for(k = 0; (k < 17) && (level != 0); ++k)
        {
#if TRACE
          snprintf(currSE.tracestring, TRACESTRING_SIZE, "DC luma 16x16 ");
#endif
          dP->readSyntaxElement(currMB, &currSE, dP);
          level = currSE.value1;

          if (level != 0)    /* leave if level == 0 */
          {
            pos_scan_4x4 += (2 * currSE.value2);

            i0 = ((*pos_scan_4x4++) << 2);
            j0 = ((*pos_scan_4x4++) << 2);

            currSlice->cof[0][j0][i0] = level;// add new intra DC coeff
            //currSlice->fcf[0][j0][i0] = level;// add new intra DC coeff
          }
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

  // luma coefficients
  {
    //======= Other Modes & CABAC ========
    //------------------------------------          
    if (cbp)
    {
      if(currMB->luma_transform_size_8x8_flag) 
      {
        //======= 8x8 transform size & CABAC ========
        currMB->read_comp_coeff_8x8_CABAC (currMB, &currSE, PLANE_Y); 
      }
      else
      {
        currMB->read_comp_coeff_4x4_CABAC (currMB, &currSE, PLANE_Y, InvLevelScale4x4, qp_per, cbp);        
      }
    }
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
        int **imgcof = currSlice->cof[uv + 1];
        int m3[2][4] = {{0,0,0,0},{0,0,0,0}};
        int m4[2][4] = {{0,0,0,0},{0,0,0,0}};
        int qp_per_uv_dc = p_Vid->qp_per_matrix[ (currMB->qpc[uv] + 3 + p_Vid->bitdepth_chroma_qp_scale) ];       //for YUV422 only
        int qp_rem_uv_dc = p_Vid->qp_rem_matrix[ (currMB->qpc[uv] + 3 + p_Vid->bitdepth_chroma_qp_scale) ];       //for YUV422 only
        if (intra)
          InvLevelScale4x4 = currSlice->InvLevelScale4x4_Intra[uv + 1][qp_rem_uv_dc];
        else 
          InvLevelScale4x4 = currSlice->InvLevelScale4x4_Inter[uv + 1][qp_rem_uv_dc];


        //===================== CHROMA DC YUV422 ======================
        {
          CBPStructure  *s_cbp = &currMB->s_cbp[0];
          coef_ctr=-1;
          level=1;
          for(k=0;(k<9)&&(level!=0);++k)
          {
            currSE.context      = CHROMA_DC_2x4;
            currSE.type         = ((currMB->is_intra_block == TRUE) ? SE_CHR_DC_INTRA : SE_CHR_DC_INTER);
            currMB->is_v_block     = ll;

#if TRACE
            snprintf(currSE.tracestring, TRACESTRING_SIZE, "2x4 DC Chroma ");
#endif
            dP = &(currSlice->partArr[partMap[currSE.type]]);

            if (dP->bitstream->ei_flag)
              currSE.mapping = linfo_levrun_c2x2;
            else
              currSE.reading = readRunLevel_CABAC;

            dP->readSyntaxElement(currMB, &currSE, dP);

            level = currSE.value1;

            if (level != 0)
            {
              s_cbp->blk |= ((int64)0xff0000) << (ll<<2) ;
              coef_ctr += currSE.value2 + 1;
              assert (coef_ctr < p_Vid->num_cdc_coeff);
              i0=SCAN_YUV422[coef_ctr][0];
              j0=SCAN_YUV422[coef_ctr][1];

              m3[i0][j0]=level;
            }
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
            for(i=0;i<2;++i)                
            {
              currSlice->cof[uv + 1][j<<2][i<<2] = m3[i][j];
              //currSlice->fcf[uv + 1][j<<2][i<<2] = m3[i][j];
            }
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
  }
  else
  {
    {
      currSE.context      = CHROMA_AC;
      currSE.type         = (currMB->is_intra_block ? SE_CHR_AC_INTRA : SE_CHR_AC_INTER);

      dP = &(currSlice->partArr[partMap[currSE.type]]);

      if (dP->bitstream->ei_flag)
        currSE.mapping = linfo_levrun_inter;
      else
        currSE.reading = readRunLevel_CABAC;

      if(currMB->is_lossless == FALSE)
      {          
        CBPStructure  *s_cbp = &currMB->s_cbp[0];
        for (b8=0; b8 < p_Vid->num_blk8x8_uv; ++b8)
        {
          currMB->is_v_block = uv = (b8 > ((p_Vid->num_uv_blocks) - 1 ));
          InvLevelScale4x4 = intra ? currSlice->InvLevelScale4x4_Intra[uv + 1][qp_rem_uv[uv]] : currSlice->InvLevelScale4x4_Inter[uv + 1][qp_rem_uv[uv]];

          for (b4 = 0; b4 < 4; ++b4)
          {
            i = cofuv_blk_x[yuv][b8][b4];
            j = cofuv_blk_y[yuv][b8][b4];

            currMB->subblock_y = subblk_offset_y[yuv][b8][b4];
            currMB->subblock_x = subblk_offset_x[yuv][b8][b4];

            pos_scan_4x4 = pos_scan4x4[1];
            level=1;

            for(k = 0; (k < 16) && (level != 0);++k)
            {
#if TRACE
              snprintf(currSE.tracestring, TRACESTRING_SIZE, "AC Chroma ");
#endif

              dP->readSyntaxElement(currMB, &currSE, dP);
              level = currSE.value1;

              if (level != 0)
              {
                s_cbp->blk |= i64_power2(cbp_blk_chroma[b8][b4]);
                pos_scan_4x4 += (currSE.value2 << 1);

                i0 = *pos_scan_4x4++;
                j0 = *pos_scan_4x4++;

                currSlice->cof[uv + 1][(j<<2) + j0][(i<<2) + i0] = rshift_rnd_sf((level * InvLevelScale4x4[j0][i0])<<qp_per_uv[uv], 4);
                //currSlice->fcf[uv + 1][(j<<2) + j0][(i<<2) + i0] = level;
              }
            } //for(k=0;(k<16)&&(level!=0);++k)
          }
        }
      }
      else
      {
        CBPStructure  *s_cbp = &currMB->s_cbp[0];
        for (b8=0; b8 < p_Vid->num_blk8x8_uv; ++b8)
        {
          currMB->is_v_block = uv = (b8 > ((p_Vid->num_uv_blocks) - 1 ));

          for (b4=0; b4 < 4; ++b4)
          {
            i = cofuv_blk_x[yuv][b8][b4];
            j = cofuv_blk_y[yuv][b8][b4];

            pos_scan_4x4 = pos_scan4x4[1];
            level=1;

            currMB->subblock_y = subblk_offset_y[yuv][b8][b4];
            currMB->subblock_x = subblk_offset_x[yuv][b8][b4];

            for(k=0;(k<16)&&(level!=0);++k)
            {
#if TRACE
              snprintf(currSE.tracestring, TRACESTRING_SIZE, "AC Chroma ");
#endif
              dP->readSyntaxElement(currMB, &currSE, dP);
              level = currSE.value1;

              if (level != 0)
              {
                s_cbp->blk |= i64_power2(cbp_blk_chroma[b8][b4]);
                pos_scan_4x4 += (currSE.value2 << 1);

                i0 = *pos_scan_4x4++;
                j0 = *pos_scan_4x4++;

                currSlice->cof[uv + 1][(j<<2) + j0][(i<<2) + i0] = level;
                //currSlice->fcf[uv + 1][(j<<2) + j0][(i<<2) + i0] = level;
              }
            } 
          }
        } 
      } //for (b4=0; b4 < 4; b4++)
    } //for (b8=0; b8 < p_Vid->num_blk8x8_uv; b8++)
  } //if (dec_picture->chroma_format_idc != YUV400)  
}

void set_read_CBP_and_coeffs_cabac(Slice *currSlice)
{
  switch (currSlice->p_Vid->active_sps->chroma_format_idc)
  {
  case YUV444:
    if (currSlice->p_Vid->separate_colour_plane_flag == 0)
    {
      currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CABAC_444;
    }
    else
    {
      currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CABAC_400;
    }
    break;
  case YUV422:
    currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CABAC_422;
    break;
  case YUV420:
    currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CABAC_420;
    break;
  case YUV400:
    currSlice->read_CBP_and_coeffs_from_NAL = read_CBP_and_coeffs_from_NAL_CABAC_400;
    break;
  default:
    assert (1);
    currSlice->read_CBP_and_coeffs_from_NAL = NULL;
    break;
  }
}


/*!
************************************************************************
* \brief
*    setup coefficient reading functions for CABAC
*
************************************************************************
*/
void set_read_comp_coeff_cabac(Macroblock *currMB)
{
  if (currMB->is_lossless == FALSE)
  {
    currMB->read_comp_coeff_4x4_CABAC = read_comp_coeff_4x4_CABAC;
    currMB->read_comp_coeff_8x8_CABAC = read_comp_coeff_8x8_MB_CABAC;
  }
  else
  {
    currMB->read_comp_coeff_4x4_CABAC = read_comp_coeff_4x4_CABAC_ls;
    currMB->read_comp_coeff_8x8_CABAC = read_comp_coeff_8x8_MB_CABAC_ls;
  }
}

