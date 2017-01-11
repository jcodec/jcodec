
/*!
 ************************************************************************
 * \file  erc_do.h
 *
 * \brief
 *      Header for the I & P frame error concealment common functions
 *
 * \author
 *      - Viktor Varsa                     <viktor.varsa@nokia.com>
 *      - Ye-Kui Wang                   <wyk@ieee.org>
 *
 ************************************************************************
 */

#ifndef _ERC_DO_H_
#define _ERC_DO_H_


#include "erc_api.h"

void ercPixConcealIMB    (VideoParameters *p_Vid, imgpel *currFrame, int row, int column, int predBlocks[], int frameWidth, int mbWidthInBlocks);

int ercCollect8PredBlocks( int predBlocks[], int currRow, int currColumn, char *condition,
                          int maxRow, int maxColumn, int step, byte fNoCornerNeigh );
int ercCollectColumnBlocks( int predBlocks[], int currRow, int currColumn, char *condition, int maxRow, int maxColumn, int step );

#define isSplitted(object_list,currMBNum) \
    ((object_list+((currMBNum)<<2))->regionMode >= REGMODE_SPLITTED)

/* this can be used as isBlock(...,INTRA) or isBlock(...,INTER_COPY) */
#define isBlock(object_list,currMBNum,comp,regMode) \
    (isSplitted(object_list,currMBNum) ? \
     ((object_list+((currMBNum)<<2)+(comp))->regionMode == REGMODE_##regMode##_8x8) : \
     ((object_list+((currMBNum)<<2))->regionMode == REGMODE_##regMode))

/* this can be used as getParam(...,mv) or getParam(...,xMin) or getParam(...,yMin) */
#define getParam(object_list,currMBNum,comp,param) \
    (isSplitted(object_list,currMBNum) ? \
     ((object_list+((currMBNum)<<2)+(comp))->param) : \
     ((object_list+((currMBNum)<<2))->param))

#endif

