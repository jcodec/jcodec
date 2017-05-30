/*!
 ************************************************************************
 * \file macroblock.h
 *
 * \brief
 *    Arrays for macroblock encoding
 *
 * \author
 *    Inge Lille-Langoy               <inge.lille-langoy@telenor.com>
 *    Copyright (C) 1999 Telenor Satellite Services, Norway
 ************************************************************************
 */

#ifndef _MACROBLOCK_H_
#define _MACROBLOCK_H_

#include "global.h"
#include "mbuffer.h"
#include "block.h"

//! single scan pattern
static const byte SNGL_SCAN[16][2] =
{
  {0,0},{1,0},{0,1},{0,2},
  {1,1},{2,0},{3,0},{2,1},
  {1,2},{0,3},{1,3},{2,2},
  {3,1},{3,2},{2,3},{3,3}
};

//! field scan pattern
static const byte FIELD_SCAN[16][2] =
{
  {0,0},{0,1},{1,0},{0,2},
  {0,3},{1,1},{1,2},{1,3},
  {2,0},{2,1},{2,2},{2,3},
  {3,0},{3,1},{3,2},{3,3}
};

//! used to control block sizes : Not used/16x16/16x8/8x16/8x8/8x4/4x8/4x4
static const int BLOCK_STEP[8][2]=
{
  {0,0},{4,4},{4,2},{2,4},{2,2},{2,1},{1,2},{1,1}
};

//! single scan pattern
static const byte SNGL_SCAN8x8[64][2] = {
  {0,0}, {1,0}, {0,1}, {0,2}, {1,1}, {2,0}, {3,0}, {2,1}, {1,2}, {0,3}, {0,4}, {1,3}, {2,2}, {3,1}, {4,0}, {5,0},
  {4,1}, {3,2}, {2,3}, {1,4}, {0,5}, {0,6}, {1,5}, {2,4}, {3,3}, {4,2}, {5,1}, {6,0}, {7,0}, {6,1}, {5,2}, {4,3},
  {3,4}, {2,5}, {1,6}, {0,7}, {1,7}, {2,6}, {3,5}, {4,4}, {5,3}, {6,2}, {7,1}, {7,2}, {6,3}, {5,4}, {4,5}, {3,6},
  {2,7}, {3,7}, {4,6}, {5,5}, {6,4}, {7,3}, {7,4}, {6,5}, {5,6}, {4,7}, {5,7}, {6,6}, {7,5}, {7,6}, {6,7}, {7,7}
};


//! field scan pattern
static const byte FIELD_SCAN8x8[64][2] = {   // 8x8
  {0,0}, {0,1}, {0,2}, {1,0}, {1,1}, {0,3}, {0,4}, {1,2}, {2,0}, {1,3}, {0,5}, {0,6}, {0,7}, {1,4}, {2,1}, {3,0},
  {2,2}, {1,5}, {1,6}, {1,7}, {2,3}, {3,1}, {4,0}, {3,2}, {2,4}, {2,5}, {2,6}, {2,7}, {3,3}, {4,1}, {5,0}, {4,2},
  {3,4}, {3,5}, {3,6}, {3,7}, {4,3}, {5,1}, {6,0}, {5,2}, {4,4}, {4,5}, {4,6}, {4,7}, {5,3}, {6,1}, {6,2}, {5,4},
  {5,5}, {5,6}, {5,7}, {6,3}, {7,0}, {7,1}, {6,4}, {6,5}, {6,6}, {6,7}, {7,2}, {7,3}, {7,4}, {7,5}, {7,6}, {7,7}
};

//! single scan pattern
static const byte SCAN_YUV422[8][2] =
{
  {0,0},{0,1},
  {1,0},{0,2},
  {0,3},{1,1},
  {1,2},{1,3}
};

static const unsigned char cbp_blk_chroma[8][4] =
{ {16, 17, 18, 19},
  {20, 21, 22, 23},
  {24, 25, 26, 27},
  {28, 29, 30, 31},
  {32, 33, 34, 35},
  {36, 37, 38, 39},
  {40, 41, 42, 43},
  {44, 45, 46, 47} 
};

static const unsigned char cofuv_blk_x[3][8][4] =
{ { {0, 1, 0, 1},
    {0, 1, 0, 1},
    {0, 0, 0, 0},
    {0, 0, 0, 0},
    {0, 0, 0, 0},
    {0, 0, 0, 0},
    {0, 0, 0, 0},
    {0, 0, 0, 0} },

  { {0, 1, 0, 1},
    {0, 1, 0, 1},
    {0, 1, 0, 1},
    {0, 1, 0, 1},
    {0, 0, 0, 0},
    {0, 0, 0, 0},
    {0, 0, 0, 0},
    {0, 0, 0, 0} },

  { {0, 1, 0, 1},
    {2, 3, 2, 3},
    {0, 1, 0, 1},
    {2, 3, 2, 3},
    {0, 1, 0, 1},
    {2, 3, 2, 3},
    {0, 1, 0, 1},
    {2, 3, 2, 3} }
};

static const unsigned char cofuv_blk_y[3][8][4] =
{
  { { 0, 0, 1, 1},
    { 0, 0, 1, 1},
    { 0, 0, 0, 0},
    { 0, 0, 0, 0},
    { 0, 0, 0, 0},
    { 0, 0, 0, 0},
    { 0, 0, 0, 0},
    { 0, 0, 0, 0} },

  { { 0, 0, 1, 1},
    { 2, 2, 3, 3},
    { 0, 0, 1, 1},
    { 2, 2, 3, 3},
    { 0, 0, 0, 0},
    { 0, 0, 0, 0},
    { 0, 0, 0, 0},
    { 0, 0, 0, 0} },

  { { 0, 0, 1, 1},
    { 0, 0, 1, 1},
    { 2, 2, 3, 3},
    { 2, 2, 3, 3},
    { 0, 0, 1, 1},
    { 0, 0, 1, 1},
    { 2, 2, 3, 3},
    { 2, 2, 3, 3}}
};

extern void setup_slice_methods_mbaff(Slice *currSlice);
extern void setup_slice_methods      (Slice *currSlice);
extern void get_neighbors(Macroblock *currMB, PixelPos *block, int mb_x, int mb_y, int blockshape_x);

extern void start_macroblock     (Slice *currSlice, Macroblock **currMB);
extern int  decode_one_macroblock(Macroblock *currMB, StorablePicture *dec_picture);
extern Boolean  exit_macroblock  (Slice *currSlice, int eos_bit);
extern void update_qp            (Macroblock *currMB, int qp);


#endif

