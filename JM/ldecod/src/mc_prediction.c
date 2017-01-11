
/*!
 *************************************************************************************
 * \file mc_prediction.c
 *
 * \brief
 *    Functions for motion compensated prediction
 *
 * \author
 *      Main contributors (see contributors.h for copyright, 
 *                         address and affiliation details)
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 *      - Chris Vogt
 *
 *************************************************************************************
 */
#include "global.h"
#include "block.h"
#include "mc_prediction.h"
#include "mbuffer.h"
#include "mb_access.h"
#include "macroblock.h"
#include "memalloc.h"
#include "dec_statistics.h"

int allocate_pred_mem(Slice *currSlice)
{
  int alloc_size = 0;
  alloc_size += get_mem2Dpel(&currSlice->tmp_block_l0, MB_BLOCK_SIZE, MB_BLOCK_SIZE);
  alloc_size += get_mem2Dpel(&currSlice->tmp_block_l1, MB_BLOCK_SIZE, MB_BLOCK_SIZE);
  alloc_size += get_mem2Dpel(&currSlice->tmp_block_l2, MB_BLOCK_SIZE, MB_BLOCK_SIZE);
  alloc_size += get_mem2Dpel(&currSlice->tmp_block_l3, MB_BLOCK_SIZE, MB_BLOCK_SIZE);
  alloc_size += get_mem2Dint(&currSlice->tmp_res, MB_BLOCK_SIZE + 5, MB_BLOCK_SIZE + 5);
  return (alloc_size);
}

void free_pred_mem(Slice *currSlice)
{
  free_mem2Dint(currSlice->tmp_res);
  free_mem2Dpel(currSlice->tmp_block_l0);
  free_mem2Dpel(currSlice->tmp_block_l1);
  free_mem2Dpel(currSlice->tmp_block_l2);
  free_mem2Dpel(currSlice->tmp_block_l3);
}

static const int COEF[6] = { 1, -5, 20, 20, -5, 1 };

/*!
 ************************************************************************
 * \brief
 *    block single list prediction
 ************************************************************************
 */
static void mc_prediction(imgpel **mb_pred, imgpel **block, int block_size_y, int block_size_x, int ioff)
{  

  int j;

  for (j = 0; j < block_size_y; j++)
  {
    memcpy(&mb_pred[j][ioff], block[j], block_size_x * sizeof(imgpel));
  }
}

/*!
 ************************************************************************
 * \brief
 *    block single list weighted prediction
 ************************************************************************
 */
static void weighted_mc_prediction(imgpel **mb_pred, 
                                   imgpel **block, 
                                   int block_size_y, 
                                   int block_size_x, 
                                   int ioff,
                                   int wp_scale,
                                   int wp_offset,
                                   int weight_denom,
                                   int color_clip)
{
  int i, j;
  int result;

  for(j = 0; j < block_size_y; j++) 
  {
    for(i = 0; i < block_size_x; i++) 
    {
      result = rshift_rnd((wp_scale * block[j][i]), weight_denom) + wp_offset;      
      mb_pred[j][i + ioff] = (imgpel)iClip3(0, color_clip, result);
    }
  }
}



/*!
 ************************************************************************
 * \brief
 *    block bi-prediction
 ************************************************************************
 */
static void bi_prediction(imgpel **mb_pred, 
                          imgpel **block_l0, 
                          imgpel **block_l1,
                          int block_size_y, 
                          int block_size_x,
                          int ioff)
{
  imgpel *mpr = &mb_pred[0][ioff];
  imgpel *b0 = block_l0[0];
  imgpel *b1 = block_l1[0];
  int ii, jj;
  int row_inc = MB_BLOCK_SIZE - block_size_x;
  for(jj = 0;jj < block_size_y;jj++)
  {
    // unroll the loop 
    for(ii = 0; ii < block_size_x; ii += 2) 
    {
      *(mpr++) = (imgpel)(((*(b0++) + *(b1++)) + 1) >> 1);
      *(mpr++) = (imgpel)(((*(b0++) + *(b1++)) + 1) >> 1);
    }
    mpr += row_inc;
    b0  += row_inc;
    b1  += row_inc;
  }
}



/*!
 ************************************************************************
 * \brief
 *    block weighted biprediction
 ************************************************************************
 */
static void weighted_bi_prediction(imgpel *mb_pred, 
                                   imgpel *block_l0, 
                                   imgpel *block_l1, 
                                   int block_size_y, 
                                   int block_size_x, 
                                   int wp_scale_l0, 
                                   int wp_scale_l1, 
                                   int wp_offset, 
                                   int weight_denom, 
                                   int color_clip)
{
  int i, j, result;
  int row_inc = MB_BLOCK_SIZE - block_size_x;

  for(j = 0; j < block_size_y; j++)
  {
    for(i = 0; i < block_size_x; i++) 
    {
      result = rshift_rnd_sf((wp_scale_l0 * *(block_l0++) + wp_scale_l1 * *(block_l1++)),  weight_denom);

      *(mb_pred++) = (imgpel) iClip1(color_clip, result + wp_offset);
    }
    mb_pred += row_inc;
    block_l0 += row_inc;
    block_l1 += row_inc;
  }
}

/*!
 ************************************************************************
 * \brief
 *    Integer positions
 ************************************************************************
 */ 
static void get_block_00(imgpel *block, imgpel *cur_img, int span, int block_size_y)
{
  // fastest to just move an entire block, since block is a temp block is a 256 byte block (16x16)
  // writes 2 lines of 16 imgpel 1 to 8 times depending in block_size_y
  int j;
  
  for (j = 0; j < block_size_y; j += 2)
  { 
    memcpy(block, cur_img, MB_BLOCK_SIZE * sizeof(imgpel));
    block += MB_BLOCK_SIZE;
    cur_img += span;
    memcpy(block, cur_img, MB_BLOCK_SIZE * sizeof(imgpel));
    block += MB_BLOCK_SIZE;
    cur_img += span;
  }
}


/*!
 ************************************************************************
 * \brief
 *    Qpel (1,0) horizontal
 ************************************************************************
 */ 
static void get_luma_10(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos , int max_imgpel_value)
{
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line, *cur_line;
  int i, j;
  int result;
  
  for (j = 0; j < block_size_y; j++)
  {
    cur_line = &(cur_imgY[j][x_pos]);
    p0 = &cur_imgY[j][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;
    orig_line = block[j];            

    for (i = 0; i < block_size_x; i++)
    {        
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
      *orig_line = (imgpel) ((*orig_line + *(cur_line++) + 1 ) >> 1);
      orig_line++;
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Half horizontal
 ************************************************************************
 */ 
static void get_luma_20(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos , int max_imgpel_value)
{
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line;
  int i, j;
  int result;
  for (j = 0; j < block_size_y; j++)
  {
    p0 = &cur_imgY[j][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {        
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line++ = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Qpel (3,0) horizontal
 ************************************************************************
 */ 
static void get_luma_30(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos , int max_imgpel_value)
{
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line, *cur_line;
  int i, j;
  int result;
  
  for (j = 0; j < block_size_y; j++)
  {
    cur_line = &(cur_imgY[j][x_pos + 1]);
    p0 = &cur_imgY[j][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;
    orig_line = block[j];            

    for (i = 0; i < block_size_x; i++)
    {        
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
      *orig_line = (imgpel) ((*orig_line + *(cur_line++) + 1 ) >> 1);
      orig_line++;
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Qpel vertical (0, 1)
 ************************************************************************
 */ 
static void get_luma_01(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line, *cur_line;
  int i, j;
  int result;
  int jj = 0;
  p0 = &(cur_imgY[ - 2][x_pos]);
  for (j = 0; j < block_size_y; j++)
  {                  
    p1 = p0 + shift_x;          
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    orig_line = block[j];
    cur_line = &(cur_imgY[jj++][x_pos]);

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
      *orig_line = (imgpel) ((*orig_line + *(cur_line++) + 1 ) >> 1);
      orig_line++;
    }
    p0 = p1 - block_size_x;
  }
}


/*!
 ************************************************************************
 * \brief
 *    Half vertical
 ************************************************************************
 */ 
static void get_luma_02(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line;
  int i, j;
  int result;
  p0 = &(cur_imgY[ - 2][x_pos]);
  for (j = 0; j < block_size_y; j++)
  {                  
    p1 = p0 + shift_x;          
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line++ = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
    }
    p0 = p1 - block_size_x;
  }
}


/*!
 ************************************************************************
 * \brief
 *    Qpel vertical (0, 3)
 ************************************************************************
 */ 
static void get_luma_03(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line, *cur_line;
  int i, j;
  int result;
  int jj = 1;

  p0 = &(cur_imgY[ -2][x_pos]);
  for (j = 0; j < block_size_y; j++)
  {                  
    p1 = p0 + shift_x;          
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    orig_line = block[j];
    cur_line = &(cur_imgY[jj++][x_pos]);

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
      *orig_line = (imgpel) ((*orig_line + *(cur_line++) + 1 ) >> 1);
      orig_line++;
    }
    p0 = p1 - block_size_x;
  }
}

/*!
 ************************************************************************
 * \brief
 *    Hpel horizontal, Qpel vertical (2, 1)
 ************************************************************************
 */ 
static void get_luma_21(imgpel **block, imgpel **cur_imgY, int **tmp_res, int block_size_y, int block_size_x, int x_pos, int max_imgpel_value)
{
  int i, j;
  /* Vertical & horizontal interpolation */
  int *tmp_line;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  int    *x0, *x1, *x2, *x3, *x4, *x5;  
  imgpel *orig_line;  
  int result;      

  int jj = -2;

  for (j = 0; j < block_size_y + 5; j++)
  {
    p0 = &cur_imgY[jj++][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;          
    tmp_line  = tmp_res[j];

    for (i = 0; i < block_size_x; i++)
    {        
      *(tmp_line++) = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));
    }
  }  

  jj = 2;
  for (j = 0; j < block_size_y; j++)
  {
    tmp_line  = tmp_res[jj++];
    x0 = tmp_res[j    ];
    x1 = tmp_res[j + 1];
    x2 = tmp_res[j + 2];
    x3 = tmp_res[j + 3];
    x4 = tmp_res[j + 4];
    x5 = tmp_res[j + 5];
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*x0++ + *x5++) - 5 * (*x1++ + *x4++) + 20 * (*x2++ + *x3++);

      *orig_line = (imgpel) iClip1(max_imgpel_value, ((result + 512)>>10));
      *orig_line = (imgpel) ((*orig_line + iClip1(max_imgpel_value, ((*(tmp_line++) + 16) >> 5)) + 1 )>> 1);
      orig_line++;
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Hpel horizontal, Hpel vertical (2, 2)
 ************************************************************************
 */ 
static void get_luma_22(imgpel **block, imgpel **cur_imgY, int **tmp_res, int block_size_y, int block_size_x, int x_pos, int max_imgpel_value)
{
  int i, j;
  /* Vertical & horizontal interpolation */
  int *tmp_line;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  int    *x0, *x1, *x2, *x3, *x4, *x5;  
  imgpel *orig_line;  
  int result;      

  int jj = - 2;

  for (j = 0; j < block_size_y + 5; j++)
  {
    p0 = &cur_imgY[jj++][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;          
    tmp_line  = tmp_res[j];

    for (i = 0; i < block_size_x; i++)
    {        
      *(tmp_line++) = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));
    }
  }

  for (j = 0; j < block_size_y; j++)
  {
    x0 = tmp_res[j    ];
    x1 = tmp_res[j + 1];
    x2 = tmp_res[j + 2];
    x3 = tmp_res[j + 3];
    x4 = tmp_res[j + 4];
    x5 = tmp_res[j + 5];
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*x0++ + *x5++) - 5 * (*x1++ + *x4++) + 20 * (*x2++ + *x3++);

      *(orig_line++) = (imgpel) iClip1(max_imgpel_value, ((result + 512)>>10));
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Hpel horizontal, Qpel vertical (2, 3)
 ************************************************************************
 */ 
static void get_luma_23(imgpel **block, imgpel **cur_imgY, int **tmp_res, int block_size_y, int block_size_x, int x_pos, int max_imgpel_value)
{
  int i, j;
  /* Vertical & horizontal interpolation */
  int *tmp_line;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  int    *x0, *x1, *x2, *x3, *x4, *x5;  
  imgpel *orig_line;  
  int result;      

  int jj = -2;

  for (j = 0; j < block_size_y + 5; j++)
  {
    p0 = &cur_imgY[jj++][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;          
    tmp_line  = tmp_res[j];

    for (i = 0; i < block_size_x; i++)
    {        
      *(tmp_line++) = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));
    }
  }

  jj = 3;
  for (j = 0; j < block_size_y; j++)
  {
    tmp_line  = tmp_res[jj++];
    x0 = tmp_res[j    ];
    x1 = tmp_res[j + 1];
    x2 = tmp_res[j + 2];
    x3 = tmp_res[j + 3];
    x4 = tmp_res[j + 4];
    x5 = tmp_res[j + 5];
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*x0++ + *x5++) - 5 * (*x1++ + *x4++) + 20 * (*x2++ + *x3++);

      *orig_line = (imgpel) iClip1(max_imgpel_value, ((result + 512)>>10));
      *orig_line = (imgpel) ((*orig_line + iClip1(max_imgpel_value, ((*(tmp_line++) + 16) >> 5)) + 1 )>> 1);
      orig_line++;
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Qpel horizontal, Hpel vertical (1, 2)
 ************************************************************************
 */ 
static void get_luma_12(imgpel **block, imgpel **cur_imgY, int **tmp_res, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  int i, j;
  int *tmp_line;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;        
  int    *x0, *x1, *x2, *x3, *x4, *x5;  
  imgpel *orig_line;  
  int result;      

  p0 = &(cur_imgY[ -2][x_pos - 2]);
  for (j = 0; j < block_size_y; j++)
  {                    
    p1 = p0 + shift_x;
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    tmp_line  = tmp_res[j];

    for (i = 0; i < block_size_x + 5; i++)
    {
      *(tmp_line++)  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));
    }
    p0 = p1 - (block_size_x + 5);
  }

  for (j = 0; j < block_size_y; j++)
  {
    tmp_line  = &tmp_res[j][2];
    orig_line = block[j];
    x0 = tmp_res[j];
    x1 = x0 + 1;
    x2 = x1 + 1;
    x3 = x2 + 1;
    x4 = x3 + 1;
    x5 = x4 + 1;

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(x0++) + *(x5++)) - 5 * (*(x1++) + *(x4++)) + 20 * (*(x2++) + *(x3++));

      *orig_line = (imgpel) iClip1(max_imgpel_value, ((result + 512)>>10));
      *orig_line = (imgpel) ((*orig_line + iClip1(max_imgpel_value, ((*(tmp_line++) + 16)>>5))+1)>>1);
      orig_line ++;
    }
  }  
}


/*!
 ************************************************************************
 * \brief
 *    Qpel horizontal, Hpel vertical (3, 2)
 ************************************************************************
 */ 
static void get_luma_32(imgpel **block, imgpel **cur_imgY, int **tmp_res, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  int i, j;
  int *tmp_line;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;        
  int    *x0, *x1, *x2, *x3, *x4, *x5;  
  imgpel *orig_line;  
  int result;      

  p0 = &(cur_imgY[ -2][x_pos - 2]);
  for (j = 0; j < block_size_y; j++)
  {                    
    p1 = p0 + shift_x;
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    tmp_line  = tmp_res[j];

    for (i = 0; i < block_size_x + 5; i++)
    {
      *(tmp_line++)  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));
    }
    p0 = p1 - (block_size_x + 5);
  }

  for (j = 0; j < block_size_y; j++)
  {
    tmp_line  = &tmp_res[j][3];
    orig_line = block[j];
    x0 = tmp_res[j];
    x1 = x0 + 1;
    x2 = x1 + 1;
    x3 = x2 + 1;
    x4 = x3 + 1;
    x5 = x4 + 1;

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(x0++) + *(x5++)) - 5 * (*(x1++) + *(x4++)) + 20 * (*(x2++) + *(x3++));

      *orig_line = (imgpel) iClip1(max_imgpel_value, ((result + 512)>>10));
      *orig_line = (imgpel) ((*orig_line + iClip1(max_imgpel_value, ((*(tmp_line++) + 16)>>5))+1)>>1);
      orig_line ++;
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Qpel horizontal, Qpel vertical (3, 3)
 ************************************************************************
 */ 
static void get_luma_33(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  int i, j;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line;  
  int result;      

  int jj = 1;

  for (j = 0; j < block_size_y; j++)
  {
    p0 = &cur_imgY[jj++][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;

    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {        
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *(orig_line++) = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
    }
  }

  p0 = &(cur_imgY[-2][x_pos + 1]);
  for (j = 0; j < block_size_y; j++)
  {        
    p1 = p0 + shift_x;
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line = (imgpel) ((*orig_line + iClip1(max_imgpel_value, ((result + 16) >> 5)) + 1) >> 1);
      orig_line++;
    }
    p0 = p1 - block_size_x ;
  }      
}



/*!
 ************************************************************************
 * \brief
 *    Qpel horizontal, Qpel vertical (1, 1)
 ************************************************************************
 */ 
static void get_luma_11(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  int i, j;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line;  
  int result;      

  int jj = 0;

  for (j = 0; j < block_size_y; j++)
  {
    p0 = &cur_imgY[jj++][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;

    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {        
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *(orig_line++) = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
    }
  }

  p0 = &(cur_imgY[-2][x_pos]);
  for (j = 0; j < block_size_y; j++)
  {        
    p1 = p0 + shift_x;
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line = (imgpel) ((*orig_line + iClip1(max_imgpel_value, ((result + 16) >> 5)) + 1) >> 1);
      orig_line++;
    }
    p0 = p1 - block_size_x ;
  }      
}

/*!
 ************************************************************************
 * \brief
 *    Qpel horizontal, Qpel vertical (1, 3)
 ************************************************************************
 */ 
static void get_luma_13(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  /* Diagonal interpolation */
  int i, j;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line;  
  int result;      

  int jj = 1;

  for (j = 0; j < block_size_y; j++)
  {
    p0 = &cur_imgY[jj++][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;

    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {        
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *(orig_line++) = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
    }
  }

  p0 = &(cur_imgY[-2][x_pos]);
  for (j = 0; j < block_size_y; j++)
  {        
    p1 = p0 + shift_x;
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line = (imgpel) ((*orig_line + iClip1(max_imgpel_value, ((result + 16) >> 5)) + 1) >> 1);
      orig_line++;
    }
    p0 = p1 - block_size_x ;
  }      
}

/*!
 ************************************************************************
 * \brief
 *    Qpel horizontal, Qpel vertical (3, 1)
 ************************************************************************
 */ 
static void get_luma_31(imgpel **block, imgpel **cur_imgY, int block_size_y, int block_size_x, int x_pos, int shift_x, int max_imgpel_value)
{
  /* Diagonal interpolation */
  int i, j;
  imgpel *p0, *p1, *p2, *p3, *p4, *p5;
  imgpel *orig_line;  
  int result;      

  int jj = 0;

  for (j = 0; j < block_size_y; j++)
  {
    p0 = &cur_imgY[jj++][x_pos - 2];
    p1 = p0 + 1;
    p2 = p1 + 1;
    p3 = p2 + 1;
    p4 = p3 + 1;
    p5 = p4 + 1;

    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {        
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *(orig_line++) = (imgpel) iClip1(max_imgpel_value, ((result + 16)>>5));
    }
  }

  p0 = &(cur_imgY[-2][x_pos + 1]);
  for (j = 0; j < block_size_y; j++)
  {        
    p1 = p0 + shift_x;
    p2 = p1 + shift_x;
    p3 = p2 + shift_x;
    p4 = p3 + shift_x;
    p5 = p4 + shift_x;
    orig_line = block[j];

    for (i = 0; i < block_size_x; i++)
    {
      result  = (*(p0++) + *(p5++)) - 5 * (*(p1++) + *(p4++)) + 20 * (*(p2++) + *(p3++));

      *orig_line = (imgpel) ((*orig_line + iClip1(max_imgpel_value, ((result + 16) >> 5)) + 1) >> 1);
      orig_line++;
    }
    p0 = p1 - block_size_x ;
  }      
}

/*!
 ************************************************************************
 * \brief
 *    Interpolation of 1/4 subpixel
 ************************************************************************
 */ 
void get_block_luma(StorablePicture *curr_ref, int x_pos, int y_pos, int block_size_x, int block_size_y, imgpel **block,
                    int shift_x, int maxold_x, int maxold_y, int **tmp_res, int max_imgpel_value, imgpel no_ref_value, Macroblock *currMB)
{
  if (curr_ref->no_ref) {
    //printf("list[ref_frame] is equal to 'no reference picture' before RAP\n");
    memset(block[0],no_ref_value,block_size_y * block_size_x * sizeof(imgpel));
  }
  else
  {
    imgpel **cur_imgY = (currMB->p_Vid->separate_colour_plane_flag && currMB->p_Slice->colour_plane_id>PLANE_Y)? curr_ref->imgUV[currMB->p_Slice->colour_plane_id-1] : curr_ref->cur_imgY;
    int dx = (x_pos & 3);
    int dy = (y_pos & 3);
    x_pos >>= 2;
    y_pos >>= 2;
    x_pos = iClip3(-18, maxold_x+2, x_pos);
    y_pos = iClip3(-10, maxold_y+2, y_pos);

    if (dx == 0 && dy == 0)
      get_block_00(&block[0][0], &cur_imgY[y_pos][x_pos], curr_ref->iLumaStride, block_size_y);
    else
    { /* other positions */
      if (dy == 0) /* No vertical interpolation */
      {         
        if (dx == 1)
          get_luma_10(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, max_imgpel_value);
        else if (dx == 2)
          get_luma_20(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, max_imgpel_value);
        else
          get_luma_30(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, max_imgpel_value);
      }
      else if (dx == 0) /* No horizontal interpolation */        
      {         
        if (dy == 1)
          get_luma_01(block, &cur_imgY[y_pos], block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
        else if (dy == 2)
          get_luma_02(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
        else
          get_luma_03(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
      }
      else if (dx == 2)  /* Vertical & horizontal interpolation */
      {  
        if (dy == 1)
          get_luma_21(block, &cur_imgY[ y_pos], tmp_res, block_size_y, block_size_x, x_pos, max_imgpel_value);
        else if (dy == 2)
          get_luma_22(block, &cur_imgY[ y_pos], tmp_res, block_size_y, block_size_x, x_pos, max_imgpel_value);
        else
          get_luma_23(block, &cur_imgY[ y_pos], tmp_res, block_size_y, block_size_x, x_pos, max_imgpel_value);
      }
      else if (dy == 2)
      {
        if (dx == 1)
          get_luma_12(block, &cur_imgY[ y_pos], tmp_res, block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
        else
          get_luma_32(block, &cur_imgY[ y_pos], tmp_res, block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
      }
      else
      {
        if (dx == 1)
        {
          if (dy == 1)
            get_luma_11(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
          else
            get_luma_13(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
        }
        else
        {
          if (dy == 1)
            get_luma_31(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
          else
            get_luma_33(block, &cur_imgY[ y_pos], block_size_y, block_size_x, x_pos, shift_x, max_imgpel_value);
        }
      }
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    Chroma (0,X)
 ************************************************************************
 */ 
static void get_chroma_0X(imgpel *block, imgpel *cur_img, int span, int block_size_y, int block_size_x, int w00, int w01, int total_scale)
{
  imgpel *cur_row = cur_img;
  imgpel *nxt_row = cur_img + span;


  imgpel *cur_line, *cur_line_p1;
  imgpel *blk_line;
  int result;
  int i, j;
  for (j = 0; j < block_size_y; j++)
  {
      cur_line    = cur_row;
      cur_line_p1 = nxt_row;
      blk_line = block;
      block += 16;
      cur_row = nxt_row;
      nxt_row += span;
    for (i = 0; i < block_size_x; i++)
    {
      result = (w00 * *cur_line++ + w01 * *cur_line_p1++);
      *(blk_line++) = (imgpel) rshift_rnd_sf(result, total_scale);
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    Chroma (X,0)
 ************************************************************************
 */ 
static void get_chroma_X0(imgpel *block, imgpel *cur_img, int span, int block_size_y, int block_size_x, int w00, int w10, int total_scale)
{
  imgpel *cur_row = cur_img;
 

    imgpel *cur_line, *cur_line_p1;
    imgpel *blk_line;
    int result;
    int i, j;
    for (j = 0; j < block_size_y; j++)
    {
      cur_line    = cur_row;
      cur_line_p1 = cur_line + 1;
      blk_line = block;
      block += 16;
      cur_row += span;
      for (i = 0; i < block_size_x; i++)
      {
        result = (w00 * *cur_line++ + w10 * *cur_line_p1++);
        //*(blk_line++) = (imgpel) iClip1(max_imgpel_value, rshift_rnd_sf(result, total_scale));
        *(blk_line++) = (imgpel) rshift_rnd_sf(result, total_scale);
      }
    }
}

/*!
 ************************************************************************
 * \brief
 *    Chroma (X,X)
 ************************************************************************
 */ 
static void get_chroma_XY(imgpel *block, imgpel *cur_img, int span, int block_size_y, int block_size_x, int w00, int w01, int w10, int w11, int total_scale)
{ 
  imgpel *cur_row = cur_img;
  imgpel *nxt_row = cur_img + span;


  {
    imgpel *cur_line, *cur_line_p1;
    imgpel *blk_line;
    int result;
    int i, j;
    for (j = 0; j < block_size_y; j++)
    {
      cur_line    = cur_row;
      cur_line_p1 = nxt_row;
      blk_line = block;
      block += 16;
      cur_row = nxt_row;
      nxt_row += span;
      for (i = 0; i < block_size_x; i++)
      {
        result  = (w00 * *(cur_line++) + w01 * *(cur_line_p1++));
        result += (w10 * *(cur_line  ) + w11 * *(cur_line_p1  ));
        *(blk_line++) = (imgpel) rshift_rnd_sf(result, total_scale);
      }
    }
  }
}

static void get_block_chroma(StorablePicture *curr_ref, int x_pos, int y_pos, int subpel_x, int subpel_y, int maxold_x, int maxold_y,
                             int block_size_x, int vert_block_size, int shiftpel_x, int shiftpel_y,
                             imgpel *block1, imgpel *block2, int total_scale, imgpel no_ref_value, VideoParameters *p_Vid)
{
  imgpel *img1,*img2;
  short dx,dy;
  int span = curr_ref->iChromaStride;
  if (curr_ref->no_ref) {
    //printf("list[ref_frame] is equal to 'no reference picture' before RAP\n");
    memset(block1,no_ref_value,vert_block_size * block_size_x * sizeof(imgpel));
    memset(block2,no_ref_value,vert_block_size * block_size_x * sizeof(imgpel));
  }
  else
  {
    dx = (short) (x_pos & subpel_x);
    dy = (short) (y_pos & subpel_y);
    x_pos = x_pos >> shiftpel_x;
    y_pos = y_pos >> shiftpel_y;
    //clip MV;
    assert(vert_block_size <=p_Vid->iChromaPadY && block_size_x<=p_Vid->iChromaPadX);
    x_pos = iClip3(-p_Vid->iChromaPadX, maxold_x, x_pos); //16
    y_pos = iClip3(-p_Vid->iChromaPadY, maxold_y, y_pos); //8
    img1 = &curr_ref->imgUV[0][y_pos][x_pos];
    img2 = &curr_ref->imgUV[1][y_pos][x_pos];

    if (dx == 0 && dy == 0) 
    {
      get_block_00(block1, img1, span, vert_block_size);
      get_block_00(block2, img2, span, vert_block_size);
    }
    else 
    {
      short dxcur = (short) (subpel_x + 1 - dx);
      short dycur = (short) (subpel_y + 1 - dy);
      short w00 = dxcur * dycur;
      if (dx == 0)
      {
        short w01 = dxcur * dy;
        get_chroma_0X(block1, img1, span, vert_block_size, block_size_x, w00, w01, total_scale);
        get_chroma_0X(block2, img2, span, vert_block_size, block_size_x, w00, w01, total_scale);
      }
      else if (dy == 0)
      {
        short w10 = dx * dycur;
        get_chroma_X0(block1, img1, span, vert_block_size, block_size_x, w00, w10, total_scale);
        get_chroma_X0(block2, img2, span, vert_block_size, block_size_x, w00, w10, total_scale);
      }
      else
      {
        short w01 = dxcur * dy;
        short w10 = dx * dycur;
        short w11 = dx * dy;
        get_chroma_XY(block1, img1, span, vert_block_size, block_size_x, w00, w01, w10, w11, total_scale);
        get_chroma_XY(block2, img2, span, vert_block_size, block_size_x, w00, w01, w10, w11, total_scale);
      }
    }
  }
}

void intra_cr_decoding(Macroblock *currMB, int yuv)
{
  VideoParameters *p_Vid = currMB->p_Vid;
  Slice *currSlice = currMB->p_Slice;
  StorablePicture *dec_picture = currSlice->dec_picture;
  imgpel **curUV;
  int uv;
  int b8,b4;
  int ioff, joff;
  int i,j;

  currSlice->intra_pred_chroma(currMB);// last argument is ignored, computes needed data for both uv channels

  for(uv = 0; uv < 2; uv++)
  {
    currMB->itrans_4x4 = (currMB->is_lossless == FALSE) ? itrans4x4 : itrans4x4_ls;

    curUV = dec_picture->imgUV[uv];

    if(currMB->is_lossless)
    {
      if ((currMB->c_ipred_mode == VERT_PRED_8)||(currMB->c_ipred_mode == HOR_PRED_8))
        Inv_Residual_trans_Chroma(currMB, uv) ;
      else
      {
        for(j=0;j<p_Vid->mb_cr_size_y;j++)
          for(i=0;i<p_Vid->mb_cr_size_x;i++)
            currSlice->mb_rres [uv+1][j][i]=currSlice->cof[uv+1][j][i];
      }
    }

    if ((!(currMB->mb_type == SI4MB) && (currMB->cbp >> 4)) )
    {
      for (b8 = 0; b8 < (p_Vid->num_uv_blocks); b8++)
      {
        for(b4 = 0; b4 < 4; b4++)
        {
          joff = subblk_offset_y[yuv][b8][b4];          
          ioff = subblk_offset_x[yuv][b8][b4];          

          currMB->itrans_4x4(currMB, (ColorPlane) (uv + 1), ioff, joff);

          copy_image_data_4x4(&curUV[currMB->pix_c_y + joff], &(currSlice->mb_rec[uv + 1][joff]), currMB->pix_c_x + ioff, ioff);
        }
      }
      currSlice->is_reset_coeff_cr = FALSE;
    }
    else if (currMB->mb_type == SI4MB)
    {
      itrans_sp_cr(currMB, uv);

      for (joff  = 0; joff < 8; joff += 4)
      {
        for(ioff = 0; ioff < 8;ioff+=4)
        {          
          currMB->itrans_4x4(currMB, (ColorPlane) (uv + 1), ioff, joff);
        
          copy_image_data_4x4(&curUV[currMB->pix_c_y + joff], &(currSlice->mb_rec[uv + 1][joff]), currMB->pix_c_x + ioff, ioff);
        }
      }
      currSlice->is_reset_coeff_cr = FALSE;
    }
    else
    {      
      for (b8 = 0; b8 < (p_Vid->num_uv_blocks); b8++)
      {
        for(b4 = 0; b4 < 4; b4++)
        {
          joff = subblk_offset_y[yuv][b8][b4];
          ioff = subblk_offset_x[yuv][b8][b4];          

          copy_image_data_4x4(&curUV[currMB->pix_c_y + joff], &(currSlice->mb_pred[uv + 1][joff]), currMB->pix_c_x + ioff, ioff);
        }
      }
    }
  }
}

static inline void set_direct_references(const PixelPos *mb, char *l0_rFrame, char *l1_rFrame, PicMotionParams **mv_info)
{
  if (mb->available)
  {
    char *ref_idx = mv_info[mb->pos_y][mb->pos_x].ref_idx;
    *l0_rFrame  = ref_idx[LIST_0];
    *l1_rFrame  = ref_idx[LIST_1];
  }
  else
  {
    *l0_rFrame  = -1;
    *l1_rFrame  = -1;
  }
}


static void set_direct_references_mb_field(const PixelPos *mb, char *l0_rFrame, char *l1_rFrame, PicMotionParams **mv_info, Macroblock *mb_data)
{
  if (mb->available)
  {
    char *ref_idx = mv_info[mb->pos_y][mb->pos_x].ref_idx;
    if (mb_data[mb->mb_addr].mb_field)
    {
      *l0_rFrame  = ref_idx[LIST_0];
      *l1_rFrame  = ref_idx[LIST_1];
    }
    else
    {
      *l0_rFrame  = (ref_idx[LIST_0] < 0) ? ref_idx[LIST_0] : ref_idx[LIST_0] * 2;
      *l1_rFrame  = (ref_idx[LIST_1] < 0) ? ref_idx[LIST_1] : ref_idx[LIST_1] * 2;
    }
  }
  else
  {
    *l0_rFrame  = -1;
    *l1_rFrame  = -1;
  }
}

static void set_direct_references_mb_frame(const PixelPos *mb, char *l0_rFrame, char *l1_rFrame, PicMotionParams **mv_info, Macroblock *mb_data)
{
  if (mb->available)
  {
    char *ref_idx = mv_info[mb->pos_y][mb->pos_x].ref_idx;
    if (mb_data[mb->mb_addr].mb_field)
    {
      *l0_rFrame  = (ref_idx[LIST_0] >> 1);
      *l1_rFrame  = (ref_idx[LIST_1] >> 1);
    }
    else
    {
      *l0_rFrame  = ref_idx[LIST_0];
      *l1_rFrame  = ref_idx[LIST_1];
    }
  }
  else
  {
    *l0_rFrame  = -1;
    *l1_rFrame  = -1;
  }
}

void prepare_direct_params(Macroblock *currMB, StorablePicture *dec_picture, MotionVector *pmvl0, MotionVector *pmvl1, char *l0_rFrame, char *l1_rFrame)
{
  Slice *currSlice = currMB->p_Slice;
  char l0_refA, l0_refB, l0_refC;
  char l1_refA, l1_refB, l1_refC;
  PicMotionParams **mv_info = dec_picture->mv_info;
  
  PixelPos mb[4];

  get_neighbors(currMB, mb, 0, 0, 16);

  if (!currSlice->mb_aff_frame_flag)
  {
    set_direct_references(&mb[0], &l0_refA, &l1_refA, mv_info);
    set_direct_references(&mb[1], &l0_refB, &l1_refB, mv_info);
    set_direct_references(&mb[2], &l0_refC, &l1_refC, mv_info);
  }
  else
  {
    VideoParameters *p_Vid = currMB->p_Vid;
    if (currMB->mb_field)
    {
      set_direct_references_mb_field(&mb[0], &l0_refA, &l1_refA, mv_info, p_Vid->mb_data);
      set_direct_references_mb_field(&mb[1], &l0_refB, &l1_refB, mv_info, p_Vid->mb_data);
      set_direct_references_mb_field(&mb[2], &l0_refC, &l1_refC, mv_info, p_Vid->mb_data);
    }
    else
    {
      set_direct_references_mb_frame(&mb[0], &l0_refA, &l1_refA, mv_info, p_Vid->mb_data);
      set_direct_references_mb_frame(&mb[1], &l0_refB, &l1_refB, mv_info, p_Vid->mb_data);
      set_direct_references_mb_frame(&mb[2], &l0_refC, &l1_refC, mv_info, p_Vid->mb_data);
    }
  }

  *l0_rFrame = (char) imin(imin((unsigned char) l0_refA, (unsigned char) l0_refB), (unsigned char) l0_refC);
  *l1_rFrame = (char) imin(imin((unsigned char) l1_refA, (unsigned char) l1_refB), (unsigned char) l1_refC);

  if (*l0_rFrame >=0)
    currMB->GetMVPredictor (currMB, mb, pmvl0, *l0_rFrame, mv_info, LIST_0, 0, 0, 16, 16);

  if (*l1_rFrame >=0)
    currMB->GetMVPredictor (currMB, mb, pmvl1, *l1_rFrame, mv_info, LIST_1, 0, 0, 16, 16);
}

static void check_motion_vector_range(const MotionVector *mv, Slice *pSlice)
{  
  if (mv->mv_x > 8191 || mv->mv_x < -8192)
  {
    fprintf(stderr,"WARNING! Horizontal motion vector %d is out of allowed range {-8192, 8191} in picture %d, macroblock %d\n", mv->mv_x, pSlice->p_Vid->number, pSlice->current_mb_nr);
    //error("invalid stream: too big horizontal motion vector", 500);
  }

  if (mv->mv_y > (pSlice->max_mb_vmv_r - 1) || mv->mv_y < (-pSlice->max_mb_vmv_r))
  {
    fprintf(stderr,"WARNING! Vertical motion vector %d is out of allowed range {%d, %d} in picture %d, macroblock %d\n", mv->mv_y, (-pSlice->max_mb_vmv_r), (pSlice->max_mb_vmv_r - 1), pSlice->p_Vid->number, pSlice->current_mb_nr);
    //error("invalid stream: too big vertical motion vector", 500);
  }
}

static inline int check_vert_mv(int llimit, int vec1_y,int rlimit)
{
  int y_pos = vec1_y >> 2;
  if(y_pos < llimit || y_pos > rlimit)
    return 1;
  else
    return 0;
}

static void perform_mc_single_wp(Macroblock *currMB, ColorPlane pl, StorablePicture *dec_picture, int pred_dir, int i, int j, int block_size_x, int block_size_y)
{
  VideoParameters *p_Vid = currMB->p_Vid;  
  Slice *currSlice = currMB->p_Slice;
  seq_parameter_set_rbsp_t *active_sps = currSlice->active_sps;
  imgpel **tmp_block_l0 = currSlice->tmp_block_l0;
  imgpel **tmp_block_l1 = currSlice->tmp_block_l1;
  static const int mv_mul = 16; // 4 * 4
  int i4   = currMB->block_x + i;
  int j4   = currMB->block_y + j;
  int type = currSlice->slice_type;
  int chroma_format_idc = dec_picture->chroma_format_idc;
  //===== Single List Prediction =====
  int ioff = (i << 2);
  int joff = (j << 2);         
  PicMotionParams *mv_info = &dec_picture->mv_info[j4][i4];
  short       ref_idx = mv_info->ref_idx[pred_dir];
  short       ref_idx_wp = ref_idx;
  MotionVector *mv_array = &mv_info->mv[pred_dir];
  int list_offset = currMB->list_offset;
  StorablePicture *list = currSlice->listX[list_offset + pred_dir][ref_idx];
  int vec1_x, vec1_y;
  // vars for get_block_luma
  int maxold_x = dec_picture->size_x_m1;
  int maxold_y = (currMB->mb_field) ? (dec_picture->size_y >> 1) - 1 : dec_picture->size_y_m1;   
  int shift_x  = dec_picture->iLumaStride;
  int **tmp_res = currSlice->tmp_res;
  int max_imgpel_value = p_Vid->max_pel_value_comp[pl];
  imgpel no_ref_value = (imgpel) p_Vid->dc_pred_value_comp[pl];
  //

#if ENABLE_DEC_STATS
  p_Vid->dec_stats->histogram_mv[LIST_0][0][mv_array->mv_x]++;
  p_Vid->dec_stats->histogram_mv[LIST_0][1][mv_array->mv_y]++;
#endif

  check_motion_vector_range(mv_array, currSlice);
  vec1_x = i4 * mv_mul + mv_array->mv_x;
  vec1_y = (currMB->block_y_aff + j) * mv_mul + mv_array->mv_y;
  if(block_size_y > (p_Vid->iLumaPadY-4) && CheckVertMV(currMB, vec1_y, block_size_y))
  {
    get_block_luma(list, vec1_x, vec1_y, block_size_x, BLOCK_SIZE_8x8, tmp_block_l0, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
    get_block_luma(list, vec1_x, vec1_y+BLOCK_SIZE_8x8_SP, block_size_x, block_size_y-BLOCK_SIZE_8x8, tmp_block_l0+BLOCK_SIZE_8x8, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  }
  else
    get_block_luma(list, vec1_x, vec1_y, block_size_x, block_size_y, tmp_block_l0,shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  

  {
    int alpha_l0, wp_offset, wp_denom;
    if (currMB->mb_field && ((p_Vid->active_pps->weighted_pred_flag&&(type==P_SLICE|| type == SP_SLICE))||(p_Vid->active_pps->weighted_bipred_idc==1 && (type==B_SLICE))))
      ref_idx_wp >>=1;
    alpha_l0  = currSlice->wp_weight[pred_dir][ref_idx_wp][pl];
    wp_offset = currSlice->wp_offset[pred_dir][ref_idx_wp][pl];
    wp_denom  = pl > 0 ? currSlice->chroma_log2_weight_denom : currSlice->luma_log2_weight_denom;
    weighted_mc_prediction(&currSlice->mb_pred[pl][joff], tmp_block_l0, block_size_y, block_size_x, ioff, alpha_l0, wp_offset, wp_denom, max_imgpel_value);
  }

  if ((chroma_format_idc != YUV400) && (chroma_format_idc != YUV444) ) 
  {
    int ioff_cr,joff_cr,block_size_x_cr,block_size_y_cr;
    int vec1_y_cr = vec1_y + ((active_sps->chroma_format_idc == 1)? currSlice->chroma_vector_adjustment[list_offset + pred_dir][ref_idx] : 0);
    int total_scale = p_Vid->total_scale;
    int maxold_x = dec_picture->size_x_cr_m1;
    int maxold_y = (currMB->mb_field) ? (dec_picture->size_y_cr >> 1) - 1 : dec_picture->size_y_cr_m1;
    int chroma_log2_weight = currSlice->chroma_log2_weight_denom;
    if (p_Vid->mb_cr_size_x == MB_BLOCK_SIZE) 
    {
      ioff_cr = ioff;
      block_size_x_cr = block_size_x;
    }
    else 
    {
      ioff_cr = ioff >> 1;
      block_size_x_cr = block_size_x >> 1;
    }
    if (p_Vid->mb_cr_size_y == MB_BLOCK_SIZE) 
    {
      joff_cr = joff;
      block_size_y_cr = block_size_y;
    }
    else 
    {
      joff_cr = joff >> 1;
      block_size_y_cr = block_size_y >> 1;
    }
    no_ref_value = (imgpel)p_Vid->dc_pred_value_comp[1];
    {
      int *weight = currSlice->wp_weight[pred_dir][ref_idx_wp];
      int *offset = currSlice->wp_offset[pred_dir][ref_idx_wp];
      get_block_chroma(list,vec1_x,vec1_y_cr,p_Vid->subpel_x,p_Vid->subpel_y,maxold_x,maxold_y,block_size_x_cr,block_size_y_cr,p_Vid->shiftpel_x,p_Vid->shiftpel_y,&tmp_block_l0[0][0],&tmp_block_l1[0][0] ,total_scale,no_ref_value,p_Vid);
      weighted_mc_prediction(&currSlice->mb_pred[1][joff_cr], tmp_block_l0, block_size_y_cr, block_size_x_cr, ioff_cr, weight[1], offset[1], chroma_log2_weight, p_Vid->max_pel_value_comp[1]);
      weighted_mc_prediction(&currSlice->mb_pred[2][joff_cr], tmp_block_l1, block_size_y_cr, block_size_x_cr, ioff_cr, weight[2], offset[2], chroma_log2_weight, p_Vid->max_pel_value_comp[2]);
    }
  }
}


static void perform_mc_single(Macroblock *currMB, ColorPlane pl, StorablePicture *dec_picture, int pred_dir, int i, int j, int block_size_x, int block_size_y)
{
  VideoParameters *p_Vid = currMB->p_Vid;  
  Slice *currSlice = currMB->p_Slice;
  seq_parameter_set_rbsp_t *active_sps = currSlice->active_sps;
  imgpel **tmp_block_l0 = currSlice->tmp_block_l0;
  imgpel **tmp_block_l1 = currSlice->tmp_block_l1;
  static const int mv_mul = 16; // 4 * 4
  int i4   = currMB->block_x + i;
  int j4   = currMB->block_y + j;
  int chroma_format_idc = dec_picture->chroma_format_idc;
  //===== Single List Prediction =====
  int ioff = (i << 2);
  int joff = (j << 2);         
  PicMotionParams *mv_info = &dec_picture->mv_info[j4][i4];
  MotionVector *mv_array = &mv_info->mv[pred_dir];
  short          ref_idx =  mv_info->ref_idx[pred_dir];  
  int list_offset = currMB->list_offset;
  StorablePicture *list = currSlice->listX[list_offset + pred_dir][ref_idx];
  int vec1_x, vec1_y;
  // vars for get_block_luma
  int maxold_x = dec_picture->size_x_m1;
  int maxold_y = (currMB->mb_field) ? (dec_picture->size_y >> 1) - 1 : dec_picture->size_y_m1;   
  int shift_x  = dec_picture->iLumaStride;
  int **tmp_res = currSlice->tmp_res;
  int max_imgpel_value = p_Vid->max_pel_value_comp[pl];
  imgpel no_ref_value = (imgpel) p_Vid->dc_pred_value_comp[pl];
  //
#if ENABLE_DEC_STATS
  p_Vid->dec_stats->histogram_mv[LIST_0][0][mv_array->mv_x]++;
  p_Vid->dec_stats->histogram_mv[LIST_0][1][mv_array->mv_y]++;
#endif

  //if (iabs(mv_array->mv_x) > 4 * 126 || iabs(mv_array->mv_y) > 4 * 126)
    //printf("motion vector %d %d\n", mv_array->mv_x, mv_array->mv_y);

  check_motion_vector_range(mv_array, currSlice);
  vec1_x = i4 * mv_mul + mv_array->mv_x;
  vec1_y = (currMB->block_y_aff + j) * mv_mul + mv_array->mv_y;
  
  if (block_size_y > (p_Vid->iLumaPadY-4) && CheckVertMV(currMB, vec1_y, block_size_y))
  {
    get_block_luma(list, vec1_x, vec1_y, block_size_x, BLOCK_SIZE_8x8, tmp_block_l0, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
    get_block_luma(list, vec1_x, vec1_y+BLOCK_SIZE_8x8_SP, block_size_x, block_size_y-BLOCK_SIZE_8x8, tmp_block_l0+BLOCK_SIZE_8x8, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  }
  else
    get_block_luma(list, vec1_x, vec1_y, block_size_x, block_size_y, tmp_block_l0,shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);

  mc_prediction(&currSlice->mb_pred[pl][joff], tmp_block_l0, block_size_y, block_size_x, ioff); 

  if ((chroma_format_idc != YUV400) && (chroma_format_idc != YUV444) ) 
  {
    int ioff_cr,joff_cr,block_size_x_cr,block_size_y_cr;
    int vec1_y_cr = vec1_y + ((active_sps->chroma_format_idc == 1)? currSlice->chroma_vector_adjustment[list_offset + pred_dir][ref_idx] : 0);
    int total_scale = p_Vid->total_scale;
    int maxold_x = dec_picture->size_x_cr_m1;
    int maxold_y = (currMB->mb_field) ? (dec_picture->size_y_cr >> 1) - 1 : dec_picture->size_y_cr_m1;
    if (p_Vid->mb_cr_size_x == MB_BLOCK_SIZE) 
    {
      ioff_cr = ioff;
      block_size_x_cr = block_size_x;
    }
    else 
    {
      ioff_cr = ioff >> 1;
      block_size_x_cr = block_size_x >> 1;
    }
    if (p_Vid->mb_cr_size_y == MB_BLOCK_SIZE) 
    {
      joff_cr = joff;
      block_size_y_cr = block_size_y;
    }
    else 
    {
      joff_cr = joff >> 1;
      block_size_y_cr = block_size_y >> 1;
    }
    no_ref_value = (imgpel)p_Vid->dc_pred_value_comp[1];        
    get_block_chroma(list,vec1_x,vec1_y_cr,p_Vid->subpel_x,p_Vid->subpel_y,maxold_x,maxold_y,block_size_x_cr,block_size_y_cr,p_Vid->shiftpel_x,p_Vid->shiftpel_y,&tmp_block_l0[0][0],&tmp_block_l1[0][0] ,total_scale,no_ref_value,p_Vid);
    mc_prediction(&currSlice->mb_pred[1][joff_cr], tmp_block_l0, block_size_y_cr, block_size_x_cr, ioff_cr);
    mc_prediction(&currSlice->mb_pred[2][joff_cr], tmp_block_l1, block_size_y_cr, block_size_x_cr, ioff_cr);
  }
}

static void perform_mc_bi_wp(Macroblock *currMB, ColorPlane pl, StorablePicture *dec_picture, int i, int j, int block_size_x, int block_size_y)
{
  static const int mv_mul = 16;
  int  vec1_x, vec1_y, vec2_x, vec2_y;
  VideoParameters *p_Vid = currMB->p_Vid;    
  Slice *currSlice = currMB->p_Slice;

  int weighted_bipred_idc = p_Vid->active_pps->weighted_bipred_idc;
  int block_y_aff = currMB->block_y_aff;
  int i4 = currMB->block_x + i;
  int j4 = currMB->block_y + j;
  int ioff = (i << 2);
  int joff = (j << 2);
  int chroma_format_idc = dec_picture->chroma_format_idc;
  int list_offset = currMB->list_offset;
  PicMotionParams *mv_info = &dec_picture->mv_info[j4][i4];
  MotionVector *l0_mv_array = &mv_info->mv[LIST_0];
  MotionVector *l1_mv_array = &mv_info->mv[LIST_1];
  short l0_refframe = mv_info->ref_idx[LIST_0];
  short l1_refframe = mv_info->ref_idx[LIST_1];
  int l0_ref_idx  = (currMB->mb_field && weighted_bipred_idc == 1) ? l0_refframe >> 1: l0_refframe;
  int l1_ref_idx  = (currMB->mb_field && weighted_bipred_idc == 1) ? l1_refframe >> 1: l1_refframe;

  
  /// WP Parameters
  int wt_list_offset = (weighted_bipred_idc==2)? list_offset : 0;
  int *weight0 = currSlice->wbp_weight[LIST_0 + wt_list_offset][l0_ref_idx][l1_ref_idx];
  int *weight1 = currSlice->wbp_weight[LIST_1 + wt_list_offset][l0_ref_idx][l1_ref_idx];
  int *offset0 = currSlice->wp_offset[LIST_0 + wt_list_offset][l0_ref_idx];
  int *offset1 = currSlice->wp_offset[LIST_1 + wt_list_offset][l1_ref_idx];
  int maxold_y = (currMB->mb_field) ? (dec_picture->size_y >> 1) - 1 : dec_picture->size_y_m1;   
  int pady = p_Vid->iLumaPadY;
  int rlimit = maxold_y + pady - block_size_y - 2;
  int llimit = 2 - pady;
  int big_blocky = block_size_y > (pady - 4);
  StorablePicture *list0 = currSlice->listX[LIST_0 + list_offset][l0_refframe];
  StorablePicture *list1 = currSlice->listX[LIST_1 + list_offset][l1_refframe];
  imgpel **tmp_block_l0 = currSlice->tmp_block_l0;
  imgpel *block0 = tmp_block_l0[0];
  imgpel **tmp_block_l1 = currSlice->tmp_block_l1;
  imgpel *block1 = tmp_block_l1[0];
  imgpel **tmp_block_l2 = currSlice->tmp_block_l2;
  imgpel *block2 = tmp_block_l2[0];
  imgpel **tmp_block_l3 = currSlice->tmp_block_l3;
  imgpel *block3 = tmp_block_l3[0];
  int wp_offset;
  int wp_denom;

  // vars for get_block_luma
  int maxold_x = dec_picture->size_x_m1;
  int shift_x  = dec_picture->iLumaStride;
  int **tmp_res = currSlice->tmp_res;
  int max_imgpel_value = p_Vid->max_pel_value_comp[pl];
  imgpel no_ref_value = (imgpel) p_Vid->dc_pred_value_comp[pl];

  check_motion_vector_range(l0_mv_array, currSlice);
  check_motion_vector_range(l1_mv_array, currSlice);
  vec1_x = i4 * mv_mul + l0_mv_array->mv_x;
  vec2_x = i4 * mv_mul + l1_mv_array->mv_x;
  vec1_y = (block_y_aff + j) * mv_mul + l0_mv_array->mv_y;
  vec2_y = (block_y_aff + j) * mv_mul + l1_mv_array->mv_y;

  if (big_blocky && check_vert_mv(llimit, vec1_y, rlimit))
  {
    get_block_luma(list0, vec1_x, vec1_y, block_size_x, BLOCK_SIZE_8x8, tmp_block_l0, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
    get_block_luma(list0, vec1_x, vec1_y+BLOCK_SIZE_8x8_SP, block_size_x, block_size_y-BLOCK_SIZE_8x8, tmp_block_l0+BLOCK_SIZE_8x8, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  }
  else
    get_block_luma(list0, vec1_x, vec1_y, block_size_x, block_size_y, tmp_block_l0,shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  if (big_blocky && check_vert_mv(llimit, vec2_y,rlimit))
  {
    get_block_luma(list1, vec2_x, vec2_y, block_size_x, BLOCK_SIZE_8x8, tmp_block_l1, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
    get_block_luma(list1, vec2_x, vec2_y+BLOCK_SIZE_8x8_SP, block_size_x, block_size_y-BLOCK_SIZE_8x8, tmp_block_l1 + BLOCK_SIZE_8x8, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  }
  else
    get_block_luma(list1, vec2_x, vec2_y, block_size_x, block_size_y, tmp_block_l1,shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);


  wp_offset = ((offset0[pl] + offset1[pl] + 1) >>1);
  wp_denom  = pl > 0 ? currSlice->chroma_log2_weight_denom : currSlice->luma_log2_weight_denom;
  weighted_bi_prediction(&currSlice->mb_pred[pl][joff][ioff], block0, block1, block_size_y, block_size_x, weight0[pl], weight1[pl], wp_offset, wp_denom + 1, max_imgpel_value);

  if ((chroma_format_idc != YUV400) && (chroma_format_idc != YUV444) ) 
  {
    int ioff_cr, joff_cr,block_size_y_cr,block_size_x_cr,vec2_y_cr,vec1_y_cr;    
    int maxold_x = dec_picture->size_x_cr_m1;
    int maxold_y = (currMB->mb_field) ? (dec_picture->size_y_cr >> 1) - 1 : dec_picture->size_y_cr_m1;
    int shiftpel_x = p_Vid->shiftpel_x;
    int shiftpel_y = p_Vid->shiftpel_y;
    int subpel_x = p_Vid->subpel_x;
    int subpel_y =  p_Vid->subpel_y;
    int total_scale = p_Vid->total_scale;
    int chroma_log2 = currSlice->chroma_log2_weight_denom + 1;

    if (p_Vid->mb_cr_size_x == MB_BLOCK_SIZE) 
    {
      ioff_cr =  ioff;
      block_size_x_cr =  block_size_x;
    }
    else 
    {
      ioff_cr = ioff >> 1;
      block_size_x_cr =  block_size_x >> 1;
    }

    if (p_Vid->mb_cr_size_y == MB_BLOCK_SIZE) 
    {
      joff_cr = joff;
      block_size_y_cr = block_size_y;
    }
    else 
    {
      joff_cr = joff >> 1;
      block_size_y_cr = block_size_y >> 1;
    }
    if (chroma_format_idc == 1) 
    {
      vec1_y_cr = vec1_y + currSlice->chroma_vector_adjustment[LIST_0 + list_offset][l0_refframe]; 
      vec2_y_cr = vec2_y + currSlice->chroma_vector_adjustment[LIST_1 + list_offset][l1_refframe]; 
    }
    else 
    {
      vec1_y_cr = vec1_y;
      vec2_y_cr = vec2_y;
    }
    no_ref_value = (imgpel)p_Vid->dc_pred_value_comp[1];

    wp_offset = ((offset0[1] + offset1[1] + 1) >>1);
    get_block_chroma(list0,vec1_x,vec1_y_cr,subpel_x,subpel_y,maxold_x,maxold_y,block_size_x_cr,block_size_y_cr,shiftpel_x,shiftpel_y,block0,block2 ,total_scale,no_ref_value,p_Vid);
    get_block_chroma(list1,vec2_x,vec2_y_cr,subpel_x,subpel_y,maxold_x,maxold_y,block_size_x_cr,block_size_y_cr,shiftpel_x,shiftpel_y,block1,block3 ,total_scale,no_ref_value,p_Vid);
    weighted_bi_prediction(&currSlice->mb_pred[1][joff_cr][ioff_cr],block0,block1,block_size_y_cr,block_size_x_cr,weight0[1],weight1[1],wp_offset,chroma_log2,p_Vid->max_pel_value_comp[1]);
    wp_offset = ((offset0[2] + offset1[2] + 1) >>1);
    weighted_bi_prediction(&currSlice->mb_pred[2][joff_cr][ioff_cr],block2,block3,block_size_y_cr,block_size_x_cr,weight0[2],weight1[2],wp_offset,chroma_log2,p_Vid->max_pel_value_comp[2]);
  }    
}

static void perform_mc_bi(Macroblock *currMB, ColorPlane pl, StorablePicture *dec_picture, int i, int j, int block_size_x, int block_size_y)
{
  static const int mv_mul = 16;
  int vec1_x=0, vec1_y=0, vec2_x=0, vec2_y=0;
  VideoParameters *p_Vid = currMB->p_Vid;    
  Slice *currSlice = currMB->p_Slice;
  
  int block_y_aff = currMB->block_y_aff;
  int i4 = currMB->block_x + i;
  int j4 = currMB->block_y + j;
  int ioff = (i << 2);
  int joff = (j << 2);
  int chroma_format_idc = dec_picture->chroma_format_idc;
  PicMotionParams *mv_info = &dec_picture->mv_info[j4][i4];
  MotionVector *l0_mv_array = &mv_info->mv[LIST_0];
  MotionVector *l1_mv_array = &mv_info->mv[LIST_1];
  short l0_refframe = mv_info->ref_idx[LIST_0];
  short l1_refframe = mv_info->ref_idx[LIST_1];
  int list_offset = currMB->list_offset;

  int maxold_y = (currMB->mb_field) ? (dec_picture->size_y >> 1) - 1 : dec_picture->size_y_m1;   
  int pady = p_Vid->iLumaPadY;
  int rlimit = maxold_y + pady - block_size_y - 2;
  int llimit = 2 - pady;
  int big_blocky = block_size_y > (pady - 4);
  StorablePicture *list0 = currSlice->listX[LIST_0 + list_offset][l0_refframe];
  StorablePicture *list1 = currSlice->listX[LIST_1 + list_offset][l1_refframe];
  imgpel **tmp_block_l0 = currSlice->tmp_block_l0;
  imgpel *block0 = tmp_block_l0[0];
  imgpel **tmp_block_l1 = currSlice->tmp_block_l1;
  imgpel *block1 = tmp_block_l1[0];
  imgpel **tmp_block_l2 = currSlice->tmp_block_l2;
  imgpel *block2 = tmp_block_l2[0];
  imgpel **tmp_block_l3 = currSlice->tmp_block_l3;
  imgpel *block3 = tmp_block_l3[0];
  // vars for get_block_luma
  int maxold_x = dec_picture->size_x_m1;
  int shift_x  = dec_picture->iLumaStride;
  int **tmp_res = currSlice->tmp_res;
  int max_imgpel_value = p_Vid->max_pel_value_comp[pl];
  imgpel no_ref_value = (imgpel) p_Vid->dc_pred_value_comp[pl];
  check_motion_vector_range(l0_mv_array, currSlice);
  check_motion_vector_range(l1_mv_array, currSlice);
  vec1_x = i4 * mv_mul + l0_mv_array->mv_x;
  vec2_x = i4 * mv_mul + l1_mv_array->mv_x;
  vec1_y = (block_y_aff + j) * mv_mul + l0_mv_array->mv_y;
  vec2_y = (block_y_aff + j) * mv_mul + l1_mv_array->mv_y;
  if (big_blocky && check_vert_mv(llimit, vec1_y, rlimit))
  {
    get_block_luma(list0, vec1_x, vec1_y, block_size_x, BLOCK_SIZE_8x8, tmp_block_l0, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
    get_block_luma(list0, vec1_x, vec1_y+BLOCK_SIZE_8x8_SP, block_size_x, block_size_y-BLOCK_SIZE_8x8, tmp_block_l0+BLOCK_SIZE_8x8, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  }
  else
    get_block_luma(list0, vec1_x, vec1_y, block_size_x, block_size_y, tmp_block_l0,shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  if (big_blocky && check_vert_mv(llimit, vec2_y,rlimit))
  {
    get_block_luma(list1, vec2_x, vec2_y, block_size_x, BLOCK_SIZE_8x8, tmp_block_l1, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
    get_block_luma(list1, vec2_x, vec2_y+BLOCK_SIZE_8x8_SP, block_size_x, block_size_y-BLOCK_SIZE_8x8, tmp_block_l1 + BLOCK_SIZE_8x8, shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  }
  else
    get_block_luma(list1, vec2_x, vec2_y, block_size_x, block_size_y, tmp_block_l1,shift_x,maxold_x,maxold_y,tmp_res,max_imgpel_value,no_ref_value, currMB);
  bi_prediction(&currSlice->mb_pred[pl][joff],tmp_block_l0,tmp_block_l1, block_size_y, block_size_x, ioff); 

  if ((chroma_format_idc != YUV400) && (chroma_format_idc != YUV444) ) 
  {
    int ioff_cr, joff_cr,block_size_y_cr,block_size_x_cr,vec2_y_cr,vec1_y_cr;    
    int chroma_format_idc = p_Vid->active_sps->chroma_format_idc;
    int maxold_x = dec_picture->size_x_cr_m1;
    int maxold_y = (currMB->mb_field) ? (dec_picture->size_y_cr >> 1) - 1 : dec_picture->size_y_cr_m1;
    int shiftpel_x = p_Vid->shiftpel_x;
    int shiftpel_y = p_Vid->shiftpel_y;
    int subpel_x = p_Vid->subpel_x;
    int subpel_y =  p_Vid->subpel_y;
    int total_scale = p_Vid->total_scale;
    if (p_Vid->mb_cr_size_x == MB_BLOCK_SIZE) 
    {
      ioff_cr =  ioff;
      block_size_x_cr =  block_size_x;
    }
    else 
    {
      ioff_cr = ioff >> 1;
      block_size_x_cr =  block_size_x >> 1;
    }
    if (p_Vid->mb_cr_size_y == MB_BLOCK_SIZE) 
    {
      joff_cr = joff;
      block_size_y_cr = block_size_y;
    }
    else 
    {
      joff_cr = joff >> 1;
      block_size_y_cr = block_size_y >> 1;
    }
    if (chroma_format_idc == 1) 
    {
      vec1_y_cr = vec1_y + currSlice->chroma_vector_adjustment[LIST_0 + list_offset][l0_refframe]; 
      vec2_y_cr = vec2_y + currSlice->chroma_vector_adjustment[LIST_1 + list_offset][l1_refframe]; 
    }
    else 
    {
      vec1_y_cr = vec1_y;
      vec2_y_cr = vec2_y;
    }
    no_ref_value = (imgpel)p_Vid->dc_pred_value_comp[1];
    get_block_chroma(list0,vec1_x,vec1_y_cr,subpel_x,subpel_y,maxold_x,maxold_y,block_size_x_cr,block_size_y_cr,shiftpel_x,shiftpel_y,block0,block2 ,total_scale,no_ref_value,p_Vid);
    get_block_chroma(list1,vec2_x,vec2_y_cr,subpel_x,subpel_y,maxold_x,maxold_y,block_size_x_cr,block_size_y_cr,shiftpel_x,shiftpel_y,block1,block3 ,total_scale,no_ref_value,p_Vid);
    bi_prediction(&currSlice->mb_pred[1][joff_cr],tmp_block_l0,tmp_block_l1, block_size_y_cr, block_size_x_cr, ioff_cr);
    bi_prediction(&currSlice->mb_pred[2][joff_cr],tmp_block_l2,tmp_block_l3, block_size_y_cr, block_size_x_cr, ioff_cr);
  }
}


void perform_mc(Macroblock *currMB, ColorPlane pl, StorablePicture *dec_picture, int pred_dir, int i, int j, int block_size_x, int block_size_y)
{
  Slice *currSlice = currMB->p_Slice;
  assert (pred_dir<=2);
  if (pred_dir != 2)
  {
    if (currSlice->weighted_pred_flag)
      perform_mc_single_wp(currMB, pl, dec_picture, pred_dir, i, j, block_size_x, block_size_y);
    else
      perform_mc_single(currMB, pl, dec_picture, pred_dir, i, j, block_size_x, block_size_y);
  }
  else
  {
    if (currSlice->weighted_bipred_idc)
      perform_mc_bi_wp(currMB, pl, dec_picture, i, j, block_size_x, block_size_y);
    else
      perform_mc_bi(currMB, pl, dec_picture, i, j, block_size_x, block_size_y);
  }
}


