
/*!
 **************************************************************************************
 * \file
 *    nalucommon.h
 * \brief
 *    NALU handling common to encoder and decoder
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Stephan Wenger        <stewe@cs.tu-berlin.de>
 *      - Karsten Suehring
 ***************************************************************************************
 */

#ifndef _NALUCOMMON_H_
#define _NALUCOMMON_H_

#include "defines.h"

#define MAXRBSPSIZE 64000
#define MAXNALUSIZE 64000

//! values for nal_unit_type
typedef enum {
 NALU_TYPE_SLICE    = 1,
 NALU_TYPE_DPA      = 2,
 NALU_TYPE_DPB      = 3,
 NALU_TYPE_DPC      = 4,
 NALU_TYPE_IDR      = 5,
 NALU_TYPE_SEI      = 6,
 NALU_TYPE_SPS      = 7,
 NALU_TYPE_PPS      = 8,
 NALU_TYPE_AUD      = 9,
 NALU_TYPE_EOSEQ    = 10,
 NALU_TYPE_EOSTREAM = 11,
 NALU_TYPE_FILL     = 12,
#if (MVC_EXTENSION_ENABLE)
 NALU_TYPE_PREFIX   = 14,
 NALU_TYPE_SUB_SPS  = 15,
 NALU_TYPE_SLC_EXT  = 20,
 NALU_TYPE_VDRD     = 24  // View and Dependency Representation Delimiter NAL Unit
#endif
} NaluType;

//! values for nal_ref_idc
typedef enum {
 NALU_PRIORITY_HIGHEST     = 3,
 NALU_PRIORITY_HIGH        = 2,
 NALU_PRIORITY_LOW         = 1,
 NALU_PRIORITY_DISPOSABLE  = 0
} NalRefIdc;

//! NAL unit structure
typedef struct nalu_t
{
  int       startcodeprefix_len;   //!< 4 for parameter sets and first slice in picture, 3 for everything else (suggested)
  unsigned  len;                   //!< Length of the NAL unit (Excluding the start code, which does not belong to the NALU)
  unsigned  max_size;              //!< NAL Unit Buffer size
  int       forbidden_bit;         //!< should be always FALSE
  NaluType  nal_unit_type;         //!< NALU_TYPE_xxxx
  NalRefIdc nal_reference_idc;     //!< NALU_PRIORITY_xxxx  
  byte     *buf;                   //!< contains the first byte followed by the EBSP
  uint16    lost_packets;          //!< true, if packet loss is detected
#if (MVC_EXTENSION_ENABLE)
  int       svc_extension_flag;    //!< should be always 0, for MVC
  int       non_idr_flag;          //!< 0 = current is IDR
  int       priority_id;           //!< a lower value of priority_id specifies a higher priority
  int       view_id;               //!< view identifier for the NAL unit
  int       temporal_id;           //!< temporal identifier for the NAL unit
  int       anchor_pic_flag;       //!< anchor access unit
  int       inter_view_flag;       //!< inter-view prediction enable
  int       reserved_one_bit;      //!< shall be equal to 1
#endif
} NALU_t;

//! allocate one NAL Unit
extern NALU_t *AllocNALU(int);

//! free one NAL Unit
extern void FreeNALU(NALU_t *n);

#if (MVC_EXTENSION_ENABLE)
extern void nal_unit_header_svc_extension();
extern void prefix_nal_unit_svc();
#endif

#endif
