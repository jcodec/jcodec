/*!
 ***************************************************************************
 * \file
 *    quant_params.h
 *
 * \author
 *    Alexis Michael Tourapis
 *
 * \brief
 *    Headerfile for Quantization parameters
 **************************************************************************
 */

#ifndef _QUANT_PARAMS_H_
#define _QUANT_PARAMS_H_

typedef struct level_quant_params {
  int   OffsetComp;
  int    ScaleComp;
  int InvScaleComp;
} LevelQuantParams;

typedef struct quant_params {
  int AdaptRndWeight;
  int AdaptRndCrWeight;

  LevelQuantParams *****q_params_4x4;
  LevelQuantParams *****q_params_8x8;

  int *qp_per_matrix;
  int *qp_rem_matrix;

  short **OffsetList4x4input;
  short **OffsetList8x8input;
  short ***OffsetList4x4;
  short ***OffsetList8x8;
} QuantParameters;

typedef struct quant_methods {
  int   block_y; 
  int   block_x;
  int   qp; 
  int*  ACLevel;
  int*  ACRun;
  int **fadjust; 
  LevelQuantParams **q_params;
  int *coeff_cost;
  const byte (*pos_scan)[2];
  const byte *c_cost;
  char type;
} QuantMethods;

#endif

