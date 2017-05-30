
/*!
 ************************************************************************
 * \file  nalu.c
 *
 * \brief
 *    Decoder NALU support functions
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Stephan Wenger   <stewe@cs.tu-berlin.de>
 ************************************************************************
 */

#include "global.h"
#include "annexb.h"
#include "nalu.h"
#include "memalloc.h"
#include "rtp.h"
#if (MVC_EXTENSION_ENABLE)
#include "vlc.h"
#endif

/*!
 *************************************************************************************
 * \brief
 *    Converts a NALU to an RBSP
 *
 * \param
 *    nalu: nalu structure to be filled
 *
 * \return
 *    length of the RBSP in bytes
 *************************************************************************************
 */

static int NALUtoRBSP (NALU_t *nalu)
{
  assert (nalu != NULL);

  nalu->len = EBSPtoRBSP (nalu->buf, nalu->len, 1) ;

  return nalu->len ;
}

/*!
************************************************************************
* \brief
*    Read the next NAL unit (with error handling)
************************************************************************
*/
int read_next_nalu(VideoParameters *p_Vid, NALU_t *nalu)
{
  InputParameters *p_Inp = p_Vid->p_Inp;
  int ret;

  switch( p_Inp->FileFormat )
  {
  default:
  case PAR_OF_ANNEXB:
    ret = get_annex_b_NALU(p_Vid, nalu, p_Vid->annex_b);
    break;
  case PAR_OF_RTP:
    ret = GetRTPNALU(p_Vid, nalu, p_Vid->BitStreamFile);
    break;   
  }

  if (ret < 0)
  {
    snprintf (errortext, ET_SIZE, "Error while getting the NALU in file format %s, exit\n", p_Inp->FileFormat==PAR_OF_ANNEXB?"Annex B":"RTP");
    error (errortext, 601);
  }
  if (ret == 0)
  {
    //FreeNALU(nalu);
    return 0;
  }

  //In some cases, zero_byte shall be present. If current NALU is a VCL NALU, we can't tell
  //whether it is the first VCL NALU at this point, so only non-VCL NAL unit is checked here.
  CheckZeroByteNonVCL(p_Vid, nalu);

  ret = NALUtoRBSP(nalu);

  if (ret < 0)
    error ("Invalid startcode emulation prevention found.", 602);

  // Got a NALU
  if (nalu->forbidden_bit)
  {
    error ("Found NALU with forbidden_bit set, bit error?", 603);
  }

  return nalu->len;
}

void CheckZeroByteNonVCL(VideoParameters *p_Vid, NALU_t *nalu)
{
  int CheckZeroByte=0;

  //This function deals only with non-VCL NAL units
  if(nalu->nal_unit_type>=1&&nalu->nal_unit_type<=5)
    return;

  //for SPS and PPS, zero_byte shall exist
  if(nalu->nal_unit_type==NALU_TYPE_SPS || nalu->nal_unit_type==NALU_TYPE_PPS)
    CheckZeroByte=1;
  //check the possibility of the current NALU to be the start of a new access unit, according to 7.4.1.2.3
  if(nalu->nal_unit_type==NALU_TYPE_AUD  || nalu->nal_unit_type==NALU_TYPE_SPS ||
    nalu->nal_unit_type==NALU_TYPE_PPS || nalu->nal_unit_type==NALU_TYPE_SEI ||
    (nalu->nal_unit_type>=13 && nalu->nal_unit_type<=18))
  {
    if(p_Vid->LastAccessUnitExists)
    {
      p_Vid->LastAccessUnitExists=0;    //deliver the last access unit to decoder
      p_Vid->NALUCount=0;
    }
  }
  p_Vid->NALUCount++;
  //for the first NAL unit in an access unit, zero_byte shall exists
  if(p_Vid->NALUCount==1)
    CheckZeroByte=1;
  if(CheckZeroByte && nalu->startcodeprefix_len==3)   
  {
    printf("Warning: zero_byte shall exist\n");
    //because it is not a very serious problem, we do not exit here
  }
}

void CheckZeroByteVCL(VideoParameters *p_Vid, NALU_t *nalu)
{
  int CheckZeroByte=0;

  //This function deals only with VCL NAL units
  if(!(nalu->nal_unit_type>=NALU_TYPE_SLICE && nalu->nal_unit_type <= NALU_TYPE_IDR))
    return;

  if(p_Vid->LastAccessUnitExists)
  {
    p_Vid->NALUCount=0;
  }
  p_Vid->NALUCount++;
  //the first VCL NAL unit that is the first NAL unit after last VCL NAL unit indicates
  //the start of a new access unit and hence the first NAL unit of the new access unit.           (sounds like a tongue twister :-)
  if(p_Vid->NALUCount == 1)
    CheckZeroByte = 1;
  p_Vid->LastAccessUnitExists = 1;
  if(CheckZeroByte && nalu->startcodeprefix_len==3)
  {
    printf("warning: zero_byte shall exist\n");
    //because it is not a very serious problem, we do not exit here
  }
}

#if (MVC_EXTENSION_ENABLE)
void nal_unit_header_mvc_extension(NALUnitHeaderMVCExt_t *NaluHeaderMVCExt, Bitstream *s)
{  
  //to be implemented;  
  NaluHeaderMVCExt->non_idr_flag     = read_u_v (1, "non_idr_flag",     s, &p_Dec->UsedBits);
  NaluHeaderMVCExt->priority_id      = read_u_v (6, "priority_id",      s, &p_Dec->UsedBits);
  NaluHeaderMVCExt->view_id          = read_u_v (10, "view_id",         s, &p_Dec->UsedBits);
  NaluHeaderMVCExt->temporal_id      = read_u_v (3, "temporal_id",      s, &p_Dec->UsedBits);
  NaluHeaderMVCExt->anchor_pic_flag  = read_u_v (1, "anchor_pic_flag",  s, &p_Dec->UsedBits);
  NaluHeaderMVCExt->inter_view_flag  = read_u_v (1, "inter_view_flag",  s, &p_Dec->UsedBits);
  NaluHeaderMVCExt->reserved_one_bit = read_u_v (1, "reserved_one_bit", s, &p_Dec->UsedBits);
  if(NaluHeaderMVCExt->reserved_one_bit != 1)
  {
    printf("Nalu Header MVC Extension: reserved_one_bit is not 1!\n");
  }
}

void nal_unit_header_svc_extension( void )
{
  //to be implemented for Annex G;
}

void prefix_nal_unit_svc( void )
{
  //to be implemented for Annex G;
}

#endif
