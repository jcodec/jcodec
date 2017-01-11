
/*!
 **************************************************************************************
 * \file
 *    parset.h
 * \brief
 *    Picture and Sequence Parameter Sets, decoder operations
 * 
 * \date 25 November 2002
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Stephan Wenger        <stewe@cs.tu-berlin.de>
 ***************************************************************************************
 */


#ifndef _PARSET_H_
#define _PARSET_H_

#include "parsetcommon.h"
#include "nalucommon.h"

static const byte ZZ_SCAN[16]  =
{  0,  1,  4,  8,  5,  2,  3,  6,  9, 12, 13, 10,  7, 11, 14, 15
};

static const byte ZZ_SCAN8[64] =
{  0,  1,  8, 16,  9,  2,  3, 10, 17, 24, 32, 25, 18, 11,  4,  5,
   12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13,  6,  7, 14, 21, 28,
   35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51,
   58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63
};

extern void Scaling_List(int *scalingList, int sizeOfScalingList, Boolean *UseDefaultScalingMatrix, Bitstream *s);

extern void InitVUI(seq_parameter_set_rbsp_t *sps);
extern int  ReadVUI(DataPartition *p, seq_parameter_set_rbsp_t *sps);
extern int  ReadHRDParameters(DataPartition *p, hrd_parameters_t *hrd);

extern void PPSConsistencyCheck (pic_parameter_set_rbsp_t *pps);
extern void SPSConsistencyCheck (seq_parameter_set_rbsp_t *sps);

extern void MakePPSavailable (VideoParameters *p_Vid, int id, pic_parameter_set_rbsp_t *pps);
extern void MakeSPSavailable (VideoParameters *p_Vid, int id, seq_parameter_set_rbsp_t *sps);

extern void ProcessSPS (VideoParameters *p_Vid, NALU_t *nalu);
extern void ProcessPPS (VideoParameters *p_Vid, NALU_t *nalu);

extern void CleanUpPPS(VideoParameters *p_Vid);

extern void activate_sps (VideoParameters *p_Vid, seq_parameter_set_rbsp_t *sps);
extern void activate_pps (VideoParameters *p_Vid, pic_parameter_set_rbsp_t *pps);

extern void UseParameterSet (Slice *currSlice);

#if (MVC_EXTENSION_ENABLE)
extern void SubsetSPSConsistencyCheck (subset_seq_parameter_set_rbsp_t *subset_sps);
extern void ProcessSubsetSPS (VideoParameters *p_Vid, NALU_t *nalu);

extern void mvc_vui_parameters_extension(MVCVUI_t *pMVCVUI, Bitstream *s);
extern void seq_parameter_set_mvc_extension(subset_seq_parameter_set_rbsp_t *subset_sps, Bitstream *s);
extern void init_subset_sps_list(subset_seq_parameter_set_rbsp_t *subset_sps_list, int iSize);
extern void reset_subset_sps(subset_seq_parameter_set_rbsp_t *subset_sps);
extern int  GetBaseViewId(VideoParameters *p_Vid, subset_seq_parameter_set_rbsp_t **subset_sps);
extern void get_max_dec_frame_buf_size(seq_parameter_set_rbsp_t *sps);
#endif

#endif
