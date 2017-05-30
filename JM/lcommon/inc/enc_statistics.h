/*!
 **************************************************************************
 *  \file enc_statistics.h
 *
 *  \brief
 *     statistics reports for the encoding process.
 *
 *  \author
 *      Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Alexis Tourapis                 <alexismt@ieee.org>
 *      - Karsten Suehring
 *
 **************************************************************************
 */

#ifndef _ENC_STATISTICS_H_
#define _ENC_STATISTICS_H_
#include "global.h"

struct stat_parameters
{
  float  bitrate;                     //!< average bit rate for the sequence except first frame
  int64  bit_ctr;                     //!< counter for bit usage
  int64  bit_ctr_n;                   //!< bit usage for the current frame
  int64  bit_ctr_emulation_prevention; //!< stored bits needed to prevent start code emulation
  int    bit_slice;                   //!< number of bits in current slice
  int    stored_bit_slice;            //!< keep number of bits in current slice (to restore status in case of MB re-encoding)  
  int    b8_mode_0_use     [NUM_SLICE_TYPES][2];
  int64  mode_use_transform[NUM_SLICE_TYPES][MAXMODE][2];
  int64  intra_chroma_mode[4];

  // B pictures
  int    NumberBFrames;

  int    frame_counter;
  int64  quant                [NUM_SLICE_TYPES];
  int64  num_macroblocks      [NUM_SLICE_TYPES];
  int    frame_ctr            [NUM_SLICE_TYPES];
  int64  bit_counter          [NUM_SLICE_TYPES];
  float  bitrate_st           [NUM_SLICE_TYPES];
  int64  mode_use             [NUM_SLICE_TYPES][MAXMODE]; //!< Macroblock mode usage for Intra frames
  int64  bit_use_mode         [NUM_SLICE_TYPES][MAXMODE]; //!< statistics of bit usage  
  int64  bit_use_mb_type      [NUM_SLICE_TYPES];
  int64  bit_use_header       [NUM_SLICE_TYPES];
  int64  tmp_bit_use_cbp      [NUM_SLICE_TYPES];
  int64  bit_use_coeffC       [NUM_SLICE_TYPES];
  int64  bit_use_coeff     [3][NUM_SLICE_TYPES];  
  int64  bit_use_delta_quant  [NUM_SLICE_TYPES];
  int64  bit_use_stuffing_bits[NUM_SLICE_TYPES];

  int    bit_ctr_parametersets;
  int    bit_ctr_parametersets_n;
  int64  bit_ctr_filler_data;
  int64  bit_ctr_filler_data_n;

#if (MVC_EXTENSION_ENABLE)
  float  bitrate_v[2];                       //!< average bit rate for the sequence except first frame
  int64  bit_ctr_v[2];                     //!< counter for bit usage
  int64  bit_ctr_n_v[2];                   //!< bit usage for the current frame
  int64  bit_ctr_emulationprevention_v[2]; //!< stored bits needed to prevent start code emulation
  int64  bit_counter_v[2][NUM_SLICE_TYPES];
  int    bit_ctr_parametersets_v[2];
  int    bit_ctr_parametersets_n_v[2];
  int64  bit_ctr_filler_data_v[2];
  int64  bit_ctr_filler_data_n_v[2];
#endif
};
typedef struct stat_parameters StatParameters;

#endif
