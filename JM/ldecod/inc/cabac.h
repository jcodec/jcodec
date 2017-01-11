
/*!
 ***************************************************************************
 * \file
 *    cabac.h
 *
 * \brief
 *    Header file for entropy coding routines
 *
 * \author
 *    Detlev Marpe                                                         \n
 *    Copyright (C) 2000 HEINRICH HERTZ INSTITUTE All Rights Reserved.
 *
 * \date
 *    21. Oct 2000 (Changes by Tobias Oelbaum 28.08.2001)
 ***************************************************************************
 */

#ifndef _CABAC_H_
#define _CABAC_H_

#include "global.h"

extern MotionInfoContexts*  create_contexts_MotionInfo(void);
extern TextureInfoContexts* create_contexts_TextureInfo(void);
extern void delete_contexts_MotionInfo(MotionInfoContexts *enco_ctx);
extern void delete_contexts_TextureInfo(TextureInfoContexts *enco_ctx);

extern void cabac_new_slice(Slice *currSlice);

extern void readMB_typeInfo_CABAC_i_slice   (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readMB_typeInfo_CABAC_p_slice   (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readMB_typeInfo_CABAC_b_slice   (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readB8_typeInfo_CABAC_p_slice   (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readB8_typeInfo_CABAC_b_slice   (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readIntraPredMode_CABAC         (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readRefFrame_CABAC              (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void read_MVD_CABAC                  (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void read_mvd_CABAC_mbaff            (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void read_CBP_CABAC                  (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readRunLevel_CABAC              (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void read_dQuant_CABAC               (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readCIPredMode_CABAC            (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void read_skip_flag_CABAC_p_slice    (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void read_skip_flag_CABAC_b_slice    (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readFieldModeInfo_CABAC         (Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);
extern void readMB_transform_size_flag_CABAC(Macroblock *currMB, SyntaxElement *se, DecodingEnvironmentPtr dep_dp);

extern void readIPCM_CABAC(Slice *currSlice, struct datapartition_dec *dP);

extern int  cabac_startcode_follows(Slice *currSlice, int eos_bit);

extern int  readSyntaxElement_CABAC         (Macroblock *currMB, SyntaxElement *se, DataPartition *this_dataPart);

extern int check_next_mb_and_get_field_mode_CABAC_p_slice( Slice *currSlice, SyntaxElement *se, DataPartition  *act_dp);
extern int check_next_mb_and_get_field_mode_CABAC_b_slice( Slice *currSlice, SyntaxElement *se, DataPartition  *act_dp);

extern void CheckAvailabilityOfNeighborsCABAC(Macroblock *currMB);

extern void set_read_and_store_CBP(Macroblock **currMB, int chroma_format_idc);

#endif  // _CABAC_H_

