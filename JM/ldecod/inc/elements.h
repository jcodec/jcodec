
/*!
 *************************************************************************************
 * \file elements.h
 *
 * \brief
 *    Header file for elements in H.264 streams
 *
 * \date
 *    6.10.2000
 *
 * \version
 *    1.0
 *
 *  \author
 *     Sebastian Purreiter     <sebastian.purreiter@mch.siemens.de>  \n
 *     Siemens AG, Information and Communication Mobile              \n
 *     P.O.Box 80 17 07                                              \n
 *     D-81617 Munich, Germany                                       \n
 *************************************************************************************
 */

#ifndef _ELEMENTS_H_
#define _ELEMENTS_H_

/*!
 *  definition of H.264 syntax elements
 *  order of elements follow dependencies for picture reconstruction
 */
/*!
 * \brief   Assignment of old TYPE partition elements to new
 *          elements
 *
 *  old element     | new elements
 *  ----------------+-------------------------------------------------------------------
 *  TYPE_HEADER     | SE_HEADER, SE_PTYPE
 *  TYPE_MBHEADER   | SE_MBTYPE, SE_REFFRAME, SE_INTRAPREDMODE
 *  TYPE_MVD        | SE_MVD
 *  TYPE_CBP        | SE_CBP_INTRA, SE_CBP_INTER
 *  SE_DELTA_QUANT_INTER
 *  SE_DELTA_QUANT_INTRA
 *  TYPE_COEFF_Y    | SE_LUM_DC_INTRA, SE_LUM_AC_INTRA, SE_LUM_DC_INTER, SE_LUM_AC_INTER
 *  TYPE_2x2DC      | SE_CHR_DC_INTRA, SE_CHR_DC_INTER
 *  TYPE_COEFF_C    | SE_CHR_AC_INTRA, SE_CHR_AC_INTER
 *  TYPE_EOS        | SE_EOS
*/

#define SE_HEADER           0
#define SE_PTYPE            1
#define SE_MBTYPE           2
#define SE_REFFRAME         3
#define SE_INTRAPREDMODE    4
#define SE_MVD              5
#define SE_CBP_INTRA        6
#define SE_LUM_DC_INTRA     7
#define SE_CHR_DC_INTRA     8
#define SE_LUM_AC_INTRA     9
#define SE_CHR_AC_INTRA     10
#define SE_CBP_INTER        11
#define SE_LUM_DC_INTER     12
#define SE_CHR_DC_INTER     13
#define SE_LUM_AC_INTER     14
#define SE_CHR_AC_INTER     15
#define SE_DELTA_QUANT_INTER      16
#define SE_DELTA_QUANT_INTRA      17
#define SE_BFRAME           18
#define SE_EOS              19
#define SE_MAX_ELEMENTS     20


#define NO_EC               0   //!< no error concealment necessary
#define EC_REQ              1   //!< error concealment required
#define EC_SYNC             2   //!< search and sync on next header element

#define MAXPARTITIONMODES   2   //!< maximum possible partition modes as defined in assignSE2partition[][]

/*!
 *  \brief  lookup-table to assign different elements to partition
 *
 *  \note   here we defined up to 6 different partitions similar to
 *          document Q15-k-18 described in the PROGFRAMEMODE.
 *          The Sliceheader contains the PSYNC information. \par
 *
 *          Elements inside a partition are not ordered. They are
 *          ordered by occurence in the stream.
 *          Assumption: Only partitionlosses are considered. \par
 *
 *          The texture elements luminance and chrominance are
 *          not ordered in the progressive form
 *          This may be changed in image.c \par
 *
 *          We also defined the proposed internet partition mode
 *          of Stephan Wenger here. To select the desired mode
 *          uncomment one of the two following lines. \par
 *
 *  -IMPORTANT:
 *          Picture- or Sliceheaders must be assigned to partition 0. \par
 *          Furthermore partitions must follow syntax dependencies as
 *          outlined in document Q15-J-23.
 */


static const byte assignSE2partition[][SE_MAX_ELEMENTS] =
{
  // 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19  // element number (do not uncomment)
  {  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },   //!< all elements in one partition no data partitioning
  {  0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 2, 2, 2, 2, 0, 0, 0, 0 }    //!< three partitions per slice
};


#endif

