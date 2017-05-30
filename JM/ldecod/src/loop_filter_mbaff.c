
/*!
 *************************************************************************************
 * \file loop_filter_mbaff.c
 *
 * \brief
 *    Filter to reduce blocking artifacts on a macroblock level.
 *    The filter strength is QP dependent.
 *
 * \author
 *    Contributors:
 *    - Peter List       Peter.List@t-systems.de:  Original code                                 (13-Aug-2001)
 *    - Jani Lainema     Jani.Lainema@nokia.com:   Some bug fixing, removal of recursiveness     (16-Aug-2001)
 *    - Peter List       Peter.List@t-systems.de:  inplace filtering and various simplifications (10-Jan-2002)
 *    - Anthony Joch     anthony@ubvideo.com:      Simplified switching between filters and
 *                                                 non-recursive default filter.                 (08-Jul-2002)
 *    - Cristina Gomila  cristina.gomila@thomson.net: Simplification of the chroma deblocking
 *                                                    from JVT-E089                              (21-Nov-2002)
 *    - Alexis Michael Tourapis atour@dolby.com:   Speed/Architecture improvements               (08-Feb-2007)
 *************************************************************************************
 */

#include "global.h"
#include "image.h"
#include "mb_access.h"
#include "loopfilter.h"
#include "loop_filter.h"

//static void get_strength_ver_MBAff     (Macroblock *MbQ, int edge, int mvlimit, StorablePicture *p);
//static void get_strength_hor_MBAff     (Macroblock *MbQ, int edge, int mvlimit, StorablePicture *p);
static void edge_loop_luma_ver_MBAff   (ColorPlane pl, imgpel** Img, byte *Strength, Macroblock *MbQ, int edge);
static void edge_loop_luma_hor_MBAff   (ColorPlane pl, imgpel** Img, byte *Strength, Macroblock *MbQ, int edge, StorablePicture *p);
static void edge_loop_chroma_ver_MBAff (imgpel** Img, byte *Strength, Macroblock *MbQ, int edge, int uv, StorablePicture *p);
static void edge_loop_chroma_hor_MBAff (imgpel** Img, byte *Strength, Macroblock *MbQ, int edge, int uv, StorablePicture *p);

void set_loop_filter_functions_mbaff(VideoParameters *p_Vid)
{
  //p_Vid->GetStrengthVer    = get_strength_ver_MBAff;
  //p_Vid->GetStrengthHor    = get_strength_hor_MBAff;
  p_Vid->EdgeLoopLumaVer   = edge_loop_luma_ver_MBAff;
  p_Vid->EdgeLoopLumaHor   = edge_loop_luma_hor_MBAff;
  p_Vid->EdgeLoopChromaVer = edge_loop_chroma_ver_MBAff;
  p_Vid->EdgeLoopChromaHor = edge_loop_chroma_hor_MBAff;
}


Macroblock* get_non_aff_neighbor_luma(Macroblock *mb, int xN, int yN)
{
  if (xN < 0)
    return(mb->mbleft);
  else if (yN < 0)
    return(mb->mbup);
  else
    return(mb);
}

Macroblock* get_non_aff_neighbor_chroma(Macroblock *mb, int xN, int yN, int block_width,int block_height)
{
  if (xN < 0) 
  {
    if (yN < block_height)
      return(mb->mbleft);
    else
      return(NULL);
  }
  else if (xN < block_width) 
  {
    if (yN < 0)
      return(mb->mbup);
    else if (yN < block_height)
      return(mb);
    else
      return(NULL);
  }
  else
    return(NULL);
}


/*!
 *********************************************************************************************
 * \brief
 *    returns a buffer of 16 Strength values for one stripe in a mb (for MBAFF)
 *********************************************************************************************
 */
void get_strength_ver_MBAff(byte *Strength, Macroblock *MbQ, int edge, int mvlimit, StorablePicture *p)
{
  //byte *Strength = MbQ->strength_ver[edge];
  short  blkP, blkQ, idx;
  //short  blk_x, blk_x2, blk_y, blk_y2 ;

  int    StrValue, i;
  short  mb_x, mb_y;

  Macroblock *MbP;

  PixelPos pixP;
  VideoParameters *p_Vid = MbQ->p_Vid;
  BlockPos *PicPos = p_Vid->PicPos;

  if ((p->slice_type==SP_SLICE)||(p->slice_type==SI_SLICE) )
  {
    for( idx = 0; idx < MB_BLOCK_SIZE; ++idx )
    {
      getAffNeighbour(MbQ, edge - 1, idx, p_Vid->mb_size[IS_LUMA], &pixP);
      blkQ = (short) ((idx & 0xFFFC) + (edge >> 2));
      blkP = (short) ((pixP.y & 0xFFFC) + (pixP.x >> 2));

      MbP = &(p_Vid->mb_data[pixP.mb_addr]);
      MbQ->mixedModeEdgeFlag = (byte) (MbQ->mb_field != MbP->mb_field);    //currSlice->mixedModeEdgeFlag = (byte) (MbQ->mb_field != MbP->mb_field);   

      Strength[idx] = (edge == 0) ? 4 : 3;
    }
  }
  else
  {
    getAffNeighbour(MbQ, edge - 1, 0, p_Vid->mb_size[IS_LUMA], &pixP);

    MbP = &(p_Vid->mb_data[pixP.mb_addr]);
    // Neighboring Frame MBs
    if ((MbQ->mb_field == FALSE && MbP->mb_field == FALSE))
    {
      MbQ->mixedModeEdgeFlag = (byte) (MbQ->mb_field != MbP->mb_field); 
      if (MbQ->is_intra_block == TRUE || MbP->is_intra_block == TRUE)
      {
        //printf("idx %d %d %d %d %d\n", idx, pixP.x, pixP.y, pixP.pos_x, pixP.pos_y);
        // Start with Strength=3. or Strength=4 for Mb-edge
        StrValue = (edge == 0) ? 4 : 3;
        for( i = 0; i < MB_BLOCK_SIZE; i ++ ) Strength[i] = StrValue;
      }
      else
      {
        get_mb_block_pos_mbaff (PicPos, MbQ->mbAddrX, &mb_x, &mb_y);
        for( idx = 0; idx < MB_BLOCK_SIZE; idx += BLOCK_SIZE)
        {
          blkQ = (short) ((idx & 0xFFFC) + (edge >> 2));
          blkP = (short) ((pixP.y & 0xFFFC) + (pixP.x >> 2));

          if (((MbQ->s_cbp[0].blk & i64_power2(blkQ)) != 0) || ((MbP->s_cbp[0].blk & i64_power2(blkP)) != 0))
            StrValue = 2;
          else if (edge && ((MbQ->mb_type == 1)  || (MbQ->mb_type == 2)))
            StrValue = 0; // if internal edge of certain types, we already know StrValue should be 0
          else // for everything else, if no coefs, but vector difference >= 1 set Strength=1
          {                       
            int blk_y  = ((mb_y<<2) + (blkQ >> 2));
            int blk_x  = ((mb_x<<2) + (blkQ  & 3));
            int blk_y2 = (pixP.pos_y >> 2);
            int blk_x2 = (pixP.pos_x >> 2);

            PicMotionParams *mv_info_p = &p->mv_info[blk_y ][blk_x ];
            PicMotionParams *mv_info_q = &p->mv_info[blk_y2][blk_x2];
            StorablePicturePtr ref_p0 = mv_info_p->ref_pic[LIST_0];
            StorablePicturePtr ref_q0 = mv_info_q->ref_pic[LIST_0];
            StorablePicturePtr ref_p1 = mv_info_p->ref_pic[LIST_1];
            StorablePicturePtr ref_q1 = mv_info_q->ref_pic[LIST_1];

            if ( ((ref_p0==ref_q0) && (ref_p1==ref_q1))||((ref_p0==ref_q1) && (ref_p1==ref_q0)))
            {
              // L0 and L1 reference pictures of p0 are different; q0 as well
              if (ref_p0 != ref_p1)
              {
                // compare MV for the same reference picture
                if (ref_p0==ref_q0)
                {
                  StrValue =  (byte) (
                    compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_0], mvlimit) ||
                    compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_1], mvlimit));
                }
                else
                {
                  StrValue =  (byte) (
                    compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_1], mvlimit) ||
                    compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_0], mvlimit));
                }
              }
              else
              { // L0 and L1 reference pictures of p0 are the same; q0 as well

                StrValue = (byte) ((
                  compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_0], mvlimit) ||
                  compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_1], mvlimit))
                  &&(
                  compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_1], mvlimit) ||
                  compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_0], mvlimit)));
              }
            }
            else
            {
              StrValue = 1;
            }                      
          }
          Strength[idx] = StrValue;
          Strength[idx + 1] = StrValue;
          Strength[idx + 2] = StrValue;
          Strength[idx + 3] = StrValue;
          pixP.y += 4;
          pixP.pos_y += 4;
        }
      }
    }
    else
    {
      for( idx = 0; idx < MB_BLOCK_SIZE; ++idx )
      {
        getAffNeighbour(MbQ, edge - 1, idx, p_Vid->mb_size[IS_LUMA], &pixP);
        blkQ = (short) ((idx & 0xFFFC) + (edge >> 2));
        blkP = (short) ((pixP.y & 0xFFFC) + (pixP.x >> 2));

        MbP = &(p_Vid->mb_data[pixP.mb_addr]);
        MbQ->mixedModeEdgeFlag = (byte) (MbQ->mb_field != MbP->mb_field); 

        // Start with Strength=3. or Strength=4 for Mb-edge
        Strength[idx] = (edge == 0 && (((!p->mb_aff_frame_flag && (p->structure==FRAME)) ||
          (p->mb_aff_frame_flag && !MbP->mb_field && !MbQ->mb_field)) ||
          ((p->mb_aff_frame_flag || (p->structure!=FRAME))))) ? 4 : 3;

        if (MbQ->is_intra_block == FALSE && MbP->is_intra_block == FALSE)
        {
          if (((MbQ->s_cbp[0].blk & i64_power2(blkQ)) != 0) || ((MbP->s_cbp[0].blk & i64_power2(blkP)) != 0))
            Strength[idx] = 2 ;
          else
          {
            // if no coefs, but vector difference >= 1 set Strength=1
            // if this is a mixed mode edge then one set of reference pictures will be frame and the
            // other will be field
            if(MbQ->mixedModeEdgeFlag) //if (currSlice->mixedModeEdgeFlag)
            {
              Strength[idx] = 1;
            }
            else
            {
              get_mb_block_pos_mbaff (PicPos, MbQ->mbAddrX, &mb_x, &mb_y);
              {
                int blk_y  = ((mb_y<<2) + (blkQ >> 2));
                int blk_x  = ((mb_x<<2) + (blkQ  & 3));
                int blk_y2 = (pixP.pos_y >> 2);
                int blk_x2 = (pixP.pos_x >> 2);

                PicMotionParams *mv_info_p = &p->mv_info[blk_y ][blk_x ];
                PicMotionParams *mv_info_q = &p->mv_info[blk_y2][blk_x2];
                StorablePicturePtr ref_p0 = mv_info_p->ref_pic[LIST_0];
                StorablePicturePtr ref_q0 = mv_info_q->ref_pic[LIST_0];
                StorablePicturePtr ref_p1 = mv_info_p->ref_pic[LIST_1];
                StorablePicturePtr ref_q1 = mv_info_q->ref_pic[LIST_1];

                if ( ((ref_p0==ref_q0) && (ref_p1==ref_q1))||((ref_p0==ref_q1) && (ref_p1==ref_q0)))
                {
                  Strength[idx]=0;
                  // L0 and L1 reference pictures of p0 are different; q0 as well
                  if (ref_p0 != ref_p1)
                  {
                    // compare MV for the same reference picture
                    if (ref_p0==ref_q0)
                    {
                      Strength[idx] =  (byte) (
                        compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_0], mvlimit) ||
                        compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_1], mvlimit));
                    }
                    else
                    {
                      Strength[idx] =  (byte) (
                        compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_1], mvlimit) ||
                        compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_0], mvlimit));
                    }
                  }
                  else
                  { // L0 and L1 reference pictures of p0 are the same; q0 as well

                    Strength[idx] = (byte) ((
                      compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_0], mvlimit) ||
                      compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_1], mvlimit))
                      &&(
                      compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_1], mvlimit) ||
                      compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_0], mvlimit)));
                  }
                }
                else
                {
                  Strength[idx] = 1;
                }
              }
            }
          }
        }
      }
    }
  }
}

/*!
 *********************************************************************************************
 * \brief
 *    returns a buffer of 16 Strength values for one stripe in a mb (for MBAFF)
 *********************************************************************************************
 */
void get_strength_hor_MBAff(byte *Strength, Macroblock *MbQ, int edge, int mvlimit, StorablePicture *p)
{
  short  blkP, blkQ, idx;
  short  blk_x, blk_x2, blk_y, blk_y2 ;

  int    StrValue, i;
  int    xQ, yQ = (edge < MB_BLOCK_SIZE ? edge : 1);
  short  mb_x, mb_y;

  Macroblock *MbP;

  PixelPos pixP;
  VideoParameters *p_Vid = MbQ->p_Vid;
  BlockPos *PicPos = p_Vid->PicPos;

  if ((p->slice_type==SP_SLICE)||(p->slice_type==SI_SLICE) )
  {
    for( idx = 0; idx < MB_BLOCK_SIZE; idx += BLOCK_SIZE)
    {
      xQ = idx;    
      getAffNeighbour(MbQ, xQ, yQ - 1, p_Vid->mb_size[IS_LUMA], &pixP);

      blkQ = (short) ((yQ & 0xFFFC) + (xQ >> 2));
      blkP = (short) ((pixP.y & 0xFFFC) + (pixP.x >> 2));

      MbP = &(p_Vid->mb_data[pixP.mb_addr]);
      MbQ->mixedModeEdgeFlag = (byte) (MbQ->mb_field != MbP->mb_field);

      StrValue = (edge == 0 && (!MbP->mb_field && !MbQ->mb_field)) ? 4 : 3;
      
      Strength[idx] = StrValue;
      Strength[idx+1] = StrValue;
      Strength[idx+2] = StrValue;
      Strength[idx+3] = StrValue;
    }
  }
  else
  {
    getAffNeighbour(MbQ, 0, yQ - 1, p_Vid->mb_size[IS_LUMA], &pixP);
    MbP = &(p_Vid->mb_data[pixP.mb_addr]);
    MbQ->mixedModeEdgeFlag = (byte) (MbQ->mb_field != MbP->mb_field); 

    // Set intra mode deblocking
    if (MbQ->is_intra_block == TRUE || MbP->is_intra_block == TRUE)
    {      
      StrValue = (edge == 0 && (!MbP->mb_field && !MbQ->mb_field)) ? 4 : 3;
      for( i = 0; i < MB_BLOCK_SIZE; i ++ ) Strength[i] = StrValue;
    }
    else
    {
      for( idx = 0; idx < MB_BLOCK_SIZE; idx += BLOCK_SIZE )
      {
        xQ = idx;    
        getAffNeighbour(MbQ, xQ, yQ - 1, p_Vid->mb_size[IS_LUMA], &pixP);

        blkQ = (short) ((yQ & 0xFFFC) + (xQ >> 2));
        blkP = (short) ((pixP.y & 0xFFFC) + (pixP.x >> 2));

        if (((MbQ->s_cbp[0].blk & i64_power2(blkQ)) != 0) || ((MbP->s_cbp[0].blk & i64_power2(blkP)) != 0))
        {
          StrValue = 2;
        }
        else
        {
          // if no coefs, but vector difference >= 1 set Strength=1
          // if this is a mixed mode edge then one set of reference pictures will be frame and the
          // other will be field
          if(MbQ->mixedModeEdgeFlag) //if (currSlice->mixedModeEdgeFlag)
          {
            StrValue = 1;
          }
          else
          {
            get_mb_block_pos_mbaff (PicPos, MbQ->mbAddrX, &mb_x, &mb_y);
            blk_y  = (short) ((mb_y<<2) + (blkQ >> 2));
            blk_x  = (short) ((mb_x<<2) + (blkQ  & 3));
            blk_y2 = (short) (pixP.pos_y >> 2);
            blk_x2 = (short) (pixP.pos_x >> 2);

            {
              PicMotionParams *mv_info_p = &p->mv_info[blk_y ][blk_x ];
              PicMotionParams *mv_info_q = &p->mv_info[blk_y2][blk_x2];
              StorablePicturePtr ref_p0 = mv_info_p->ref_pic[LIST_0];
              StorablePicturePtr ref_q0 = mv_info_q->ref_pic[LIST_0];
              StorablePicturePtr ref_p1 = mv_info_p->ref_pic[LIST_1];
              StorablePicturePtr ref_q1 = mv_info_q->ref_pic[LIST_1];

              if ( ((ref_p0==ref_q0) && (ref_p1==ref_q1)) ||
                ((ref_p0==ref_q1) && (ref_p1==ref_q0)))
              {
                StrValue = 0;
                // L0 and L1 reference pictures of p0 are different; q0 as well
                if (ref_p0 != ref_p1)
                {
                  // compare MV for the same reference picture
                  if (ref_p0==ref_q0)
                  {
                    StrValue =  (byte) (
                      compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_0], mvlimit) ||
                      compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_1], mvlimit));
                  }
                  else
                  {
                    StrValue =  (byte) (
                      compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_1], mvlimit) ||
                      compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_0], mvlimit));
                  }
                }
                else
                { // L0 and L1 reference pictures of p0 are the same; q0 as well
                  StrValue = (byte) ((
                    compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_0], mvlimit) ||
                    compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_1], mvlimit))
                    &&(
                    compare_mvs(&mv_info_p->mv[LIST_0], &mv_info_q->mv[LIST_1], mvlimit) ||
                    compare_mvs(&mv_info_p->mv[LIST_1], &mv_info_q->mv[LIST_0], mvlimit)));
                }
              }
              else
              {
                StrValue = 1;
              }
            }
          }
        }
        Strength[idx] = StrValue;
        Strength[idx + 1] = StrValue;
        Strength[idx + 2] = StrValue;
        Strength[idx + 3] = StrValue;
      }
    }
  }
}


/*!
 *****************************************************************************************
 * \brief
 *    Filters 16 pel block edge of Super MB Frame coded MBs
 *****************************************************************************************
 */
static void edge_loop_luma_ver_MBAff(ColorPlane pl, imgpel** Img, byte *Strength, Macroblock *MbQ, int edge)
{
  int      pel, Strng ;
  imgpel   L2 = 0, L1, L0, R0, R1, R2 = 0;  
  int      Alpha = 0, Beta = 0 ;
  const byte* ClipTab = NULL;
  int      indexA, indexB;

  int      QP;

  PixelPos pixP, pixQ;

  VideoParameters *p_Vid = MbQ->p_Vid;
  int      bitdepth_scale = pl? p_Vid->bitdepth_scale[IS_CHROMA] : p_Vid->bitdepth_scale[IS_LUMA];
  int      max_imgpel_value = p_Vid->max_pel_value_comp[pl];

  int AlphaC0Offset = MbQ->DFAlphaC0Offset;
  int BetaOffset = MbQ->DFBetaOffset;  

  Macroblock *MbP;
  imgpel   *SrcPtrP, *SrcPtrQ;

  for( pel = 0 ; pel < MB_BLOCK_SIZE ; ++pel )
  {
    getAffNeighbour(MbQ, edge - 1, pel, p_Vid->mb_size[IS_LUMA], &pixP);     

    if ( pixP.available || (MbQ->DFDisableIdc == 0))
    {
      if( (Strng = Strength[pel]) != 0)
      {
        getAffNeighbour(MbQ, edge, pel, p_Vid->mb_size[IS_LUMA], &pixQ);

        MbP = &(p_Vid->mb_data[pixP.mb_addr]);

        SrcPtrQ = &(Img[pixQ.pos_y][pixQ.pos_x]);
        SrcPtrP = &(Img[pixP.pos_y][pixP.pos_x]);

        // Average QP of the two blocks
        QP = pl? ((MbP->qpc[pl-1] + MbQ->qpc[pl-1] + 1) >> 1) : (MbP->qp + MbQ->qp + 1) >> 1;

        indexA = iClip3(0, MAX_QP, QP + AlphaC0Offset);
        indexB = iClip3(0, MAX_QP, QP + BetaOffset);

        Alpha   = ALPHA_TABLE[indexA] * bitdepth_scale;
        Beta    = BETA_TABLE [indexB] * bitdepth_scale;
        ClipTab = CLIP_TAB[indexA];

        L0  = SrcPtrP[ 0] ;
        R0  = SrcPtrQ[ 0] ;      

        if( iabs( R0 - L0 ) < Alpha )
        {          
          L1  = SrcPtrP[-1];
          R1  = SrcPtrQ[ 1];                

          if ((iabs( R0 - R1) < Beta )   && (iabs(L0 - L1) < Beta ))
          {
            L2  = SrcPtrP[-2];
            R2  = SrcPtrQ[ 2];
            if(Strng == 4 )    // INTRA strong filtering
            {
              int RL0 = L0 + R0;
              int small_gap = (iabs( R0 - L0 ) < ((Alpha >> 2) + 2));
              int aq  = ( iabs( R0 - R2) < Beta ) & small_gap;               
              int ap  = ( iabs( L0 - L2) < Beta ) & small_gap;

              if (ap)
              {
                int L3  = SrcPtrP[-3];
                SrcPtrP[-2 ] = (imgpel) ((((L3 + L2) << 1) + L2 + L1 + RL0 + 4) >> 3);
                SrcPtrP[-1 ] = (imgpel) (( L2 + L1 + L0 + R0 + 2) >> 2);
                SrcPtrP[ 0 ] = (imgpel) (( R1 + ((L1 + RL0) << 1) +  L2 + 4) >> 3);
              }
              else
              {
                SrcPtrP[ 0 ] = (imgpel) (((L1 << 1) + L0 + R1 + 2) >> 2) ;
              }

              if (aq)
              {
                imgpel R3  = SrcPtrQ[ 3];
                SrcPtrQ[ 0 ] = (imgpel) (( L1 + ((R1 + RL0) << 1) +  R2 + 4) >> 3);
                SrcPtrQ[ 1 ] = (imgpel) (( R2 + R0 + R1 + L0 + 2) >> 2);
                SrcPtrQ[ 2 ] = (imgpel) ((((R3 + R2) << 1) + R2 + R1 + RL0 + 4) >> 3);
              }
              else
              {
                SrcPtrQ[ 0 ] = (imgpel) (((R1 << 1) + R0 + L1 + 2) >> 2);
              }
            }
            else   // normal filtering
            {              
              int RL0 = (L0 + R0 + 1) >> 1;
              int aq  = (iabs( R0 - R2) < Beta);
              int ap  = (iabs( L0 - L2) < Beta);

              int C0  = ClipTab[ Strng ] * bitdepth_scale;
              int tc0  = (C0 + ap + aq) ;
              int dif = iClip3( -tc0, tc0, (((R0 - L0) << 2) + (L1 - R1) + 4) >> 3) ;

              if( ap && (C0 != 0))
                *(SrcPtrP - 1) += iClip3( -C0,  C0, ( L2 + RL0 - (L1 << 1)) >> 1 ) ;

              if (dif)
              {
                *SrcPtrP  = (imgpel) iClip1 (max_imgpel_value, L0 + dif) ;
                *SrcPtrQ  = (imgpel) iClip1 (max_imgpel_value, R0 - dif) ;
              }

              if( aq  && (C0 != 0))
                *(SrcPtrQ + 1) += iClip3( -C0,  C0, ( R2 + RL0 - (R1 << 1)) >> 1 ) ;
            }            
          }
        }
      }
    }
  }
}

/*!
 *****************************************************************************************
 * \brief
 *    Filters 16 pel block edge of Super MB Frame coded MBs
 *****************************************************************************************
 */
static void edge_loop_luma_hor_MBAff(ColorPlane pl, imgpel** Img, byte *Strength, Macroblock *MbQ, 
              int edge, StorablePicture *p)
{
  int      width = p->iLumaStride; //p->size_x;
  int      pel, Strng ;
  int      PelNum = pl? pelnum_cr[1][p->chroma_format_idc] : MB_BLOCK_SIZE;

  int      yQ = (edge < MB_BLOCK_SIZE ? edge : 1);

  PixelPos pixP, pixQ;

  VideoParameters *p_Vid = MbQ->p_Vid;
  int      bitdepth_scale = pl? p_Vid->bitdepth_scale[IS_CHROMA] : p_Vid->bitdepth_scale[IS_LUMA];
  int      max_imgpel_value = p_Vid->max_pel_value_comp[pl];

  getAffNeighbour(MbQ, 0, yQ - 1, p_Vid->mb_size[IS_LUMA], &pixP);     

  if (pixP.available || (MbQ->DFDisableIdc == 0))
  {
    int AlphaC0Offset = MbQ->DFAlphaC0Offset;
    int BetaOffset = MbQ->DFBetaOffset;

    Macroblock *MbP = &(p_Vid->mb_data[pixP.mb_addr]);

    int incQ    = ((MbP->mb_field && !MbQ->mb_field) ? 2 * width : width);
    int incP    = ((MbQ->mb_field && !MbP->mb_field) ? 2 * width : width);

    // Average QP of the two blocks
    int QP = pl? ((MbP->qpc[pl - 1] + MbQ->qpc[pl - 1] + 1) >> 1) : (MbP->qp + MbQ->qp + 1) >> 1;

    int indexA = iClip3(0, MAX_QP, QP + AlphaC0Offset);
    int indexB = iClip3(0, MAX_QP, QP + BetaOffset);

    int Alpha   = ALPHA_TABLE[indexA] * bitdepth_scale;
    int Beta    = BETA_TABLE [indexB] * bitdepth_scale;    

    if ((Alpha | Beta )!= 0)
    {
      const byte* ClipTab = CLIP_TAB[indexA];
      getAffNeighbour(MbQ, 0, yQ, p_Vid->mb_size[IS_LUMA], &pixQ);

      for( pel = 0 ; pel < PelNum ; ++pel )
      {
        if( (Strng = Strength[pel]) != 0)
        {
          imgpel *SrcPtrQ = &(Img[pixQ.pos_y][pixQ.pos_x]);
          imgpel *SrcPtrP = &(Img[pixP.pos_y][pixP.pos_x]);

          imgpel L0  = *SrcPtrP;
          imgpel R0  = *SrcPtrQ;

          if( iabs( R0 - L0 ) < Alpha )
          {
            imgpel L1  = SrcPtrP[-incP];
            imgpel R1  = SrcPtrQ[ incQ];      

            if ((iabs( R0 - R1) < Beta )   && (iabs(L0 - L1) < Beta ))
            {
              imgpel L2  = SrcPtrP[-incP*2];
              imgpel R2  = SrcPtrQ[ incQ*2];
              if(Strng == 4 )    // INTRA strong filtering
              {
                int RL0 = L0 + R0;
                int small_gap = (iabs( R0 - L0 ) < ((Alpha >> 2) + 2));
                int aq  = ( iabs( R0 - R2) < Beta ) & small_gap;               
                int ap  = ( iabs( L0 - L2) < Beta ) & small_gap;

                if (ap)
                {
                  imgpel L3  = SrcPtrP[-incP*3];
                  SrcPtrP[-incP * 2] = (imgpel) ((((L3 + L2) << 1) + L2 + L1 + RL0 + 4) >> 3);
                  SrcPtrP[-incP    ] = (imgpel) (( L2 + L1 + L0 + R0 + 2) >> 2);
                  SrcPtrP[    0    ] = (imgpel) (( R1 + ((L1 + RL0) << 1) +  L2 + 4) >> 3);
                }
                else
                {
                  SrcPtrP[     0     ] = (imgpel) (((L1 << 1) + L0 + R1 + 2) >> 2) ;
                }

                if (aq)
                {
                  imgpel R3 = SrcPtrQ[ incQ*3];
                  SrcPtrQ[    0     ] = (imgpel) (( L1 + ((R1 + RL0) << 1) +  R2 + 4) >> 3);
                  SrcPtrQ[ incQ     ] = (imgpel) (( R2 + R0 + R1 + L0 + 2) >> 2);
                  SrcPtrQ[ incQ * 2 ] = (imgpel) ((((R3 + R2) << 1) + R2 + R1 + RL0 + 4) >> 3);
                }
                else
                {
                  SrcPtrQ[    0     ] = (imgpel) (((R1 << 1) + R0 + L1 + 2) >> 2);
                }
              }
              else   // normal filtering
              {              
                int RL0 = (L0 + R0 + 1) >> 1;
                int aq  = (iabs( R0 - R2) < Beta);
                int ap  = (iabs( L0 - L2) < Beta);

                int C0  = ClipTab[ Strng ] * bitdepth_scale;
                int tc0  = (C0 + ap + aq) ;
                int dif = iClip3( -tc0, tc0, (((R0 - L0) << 2) + (L1 - R1) + 4) >> 3) ;

                if( ap && (C0 != 0))
                  *(SrcPtrP - incP) += iClip3( -C0,  C0, ( L2 + RL0 - (L1 << 1)) >> 1 ) ;

                if (dif)
                {
                  *SrcPtrP  = (imgpel) iClip1 (max_imgpel_value, L0 + dif) ;
                  *SrcPtrQ  = (imgpel) iClip1 (max_imgpel_value, R0 - dif) ;
                }

                if( aq  && (C0 != 0))
                  *(SrcPtrQ + incQ) += iClip3( -C0,  C0, ( R2 + RL0 - (R1 << 1)) >> 1 ) ;
              }            
            }
          }        
        }  
        pixP.pos_x++;
        pixQ.pos_x++;
      }
    }
  }
}



/*!
*****************************************************************************************
* \brief
*    Filters chroma block edge for MBAFF types
*****************************************************************************************
 */
static void edge_loop_chroma_ver_MBAff(imgpel** Img, byte *Strength, Macroblock *MbQ, int edge, int uv, StorablePicture *p)
{
  int      pel, Strng ;

  imgpel   L1, L0, R0, R1;
  int      Alpha = 0, Beta = 0;
  const byte* ClipTab = NULL;
  int      indexA, indexB;
  VideoParameters *p_Vid = MbQ->p_Vid;
  int      PelNum = pelnum_cr[0][p->chroma_format_idc];
  int      StrengthIdx;
  int      QP;
  int      xQ = edge, yQ;
  PixelPos pixP, pixQ;
  int      bitdepth_scale = p_Vid->bitdepth_scale[IS_CHROMA];
  int      max_imgpel_value = p_Vid->max_pel_value_comp[uv + 1];
  
  int      AlphaC0Offset = MbQ->DFAlphaC0Offset;
  int      BetaOffset    = MbQ->DFBetaOffset;
  Macroblock *MbP;
  imgpel   *SrcPtrP, *SrcPtrQ;


  for( pel = 0 ; pel < PelNum ; ++pel )
  {
    yQ = pel;
    getAffNeighbour(MbQ, xQ, yQ, p_Vid->mb_size[IS_CHROMA], &pixQ);
    getAffNeighbour(MbQ, xQ - 1, yQ, p_Vid->mb_size[IS_CHROMA], &pixP);    
    MbP = &(p_Vid->mb_data[pixP.mb_addr]);    
    StrengthIdx = (PelNum == 8) ? ((MbQ->mb_field && !MbP->mb_field) ? pel << 1 :((pel >> 1) << 2) + (pel & 0x01)) : pel;

    if (pixP.available || (MbQ->DFDisableIdc == 0))
    {
      if( (Strng = Strength[StrengthIdx]) != 0)
      {
        SrcPtrQ = &(Img[pixQ.pos_y][pixQ.pos_x]);
        SrcPtrP = &(Img[pixP.pos_y][pixP.pos_x]);

        // Average QP of the two blocks
        QP = (MbP->qpc[uv] + MbQ->qpc[uv] + 1) >> 1;

        indexA = iClip3(0, MAX_QP, QP + AlphaC0Offset);
        indexB = iClip3(0, MAX_QP, QP + BetaOffset);

        Alpha   = ALPHA_TABLE[indexA] * bitdepth_scale;
        Beta    = BETA_TABLE [indexB] * bitdepth_scale;
        ClipTab = CLIP_TAB[indexA];

        L0  = *SrcPtrP;
        R0  = *SrcPtrQ;

        if( iabs( R0 - L0 ) < Alpha )
        {
          L1  = SrcPtrP[-1];
          R1  = SrcPtrQ[ 1];      
          if( ((iabs( R0 - R1) - Beta < 0)  && (iabs(L0 - L1) - Beta < 0 ))  )
          {
            if( Strng == 4 )    // INTRA strong filtering
            {
              SrcPtrQ[0] = (imgpel) ( ((R1 << 1) + R0 + L1 + 2) >> 2 );
              SrcPtrP[0] = (imgpel) ( ((L1 << 1) + L0 + R1 + 2) >> 2 );
            }
            else
            {
              int C0  = ClipTab[ Strng ] * bitdepth_scale;
              int tc0  = (C0 + 1);
              int dif = iClip3( -tc0, tc0, ( ((R0 - L0) << 2) + (L1 - R1) + 4) >> 3 );

              if (dif)
              {
                *SrcPtrP = (imgpel) iClip1 ( max_imgpel_value, L0 + dif );
                *SrcPtrQ = (imgpel) iClip1 ( max_imgpel_value, R0 - dif );
              }
            }
          }
        }
      }
    }
  }
}

/*!
*****************************************************************************************
* \brief
*    Filters chroma block edge for MBAFF types
*****************************************************************************************
 */
static void edge_loop_chroma_hor_MBAff(imgpel** Img, byte *Strength, Macroblock *MbQ, int edge, int uv, StorablePicture *p)
{  
  VideoParameters *p_Vid = MbQ->p_Vid;
  int      PelNum = pelnum_cr[1][p->chroma_format_idc];
  int      yQ = (edge < MB_BLOCK_SIZE? edge : 1);
  PixelPos pixP, pixQ;
  int      bitdepth_scale = p_Vid->bitdepth_scale[IS_CHROMA];
  int      max_imgpel_value = p_Vid->max_pel_value_comp[uv + 1];
  
  int      AlphaC0Offset = MbQ->DFAlphaC0Offset;
  int      BetaOffset    = MbQ->DFBetaOffset;
  int      width = p->iChromaStride; //p->size_x_cr;

  getAffNeighbour(MbQ, 0, yQ - 1, p_Vid->mb_size[IS_CHROMA], &pixP);    
  getAffNeighbour(MbQ, 0, yQ, p_Vid->mb_size[IS_CHROMA], &pixQ);

  if (pixP.available || (MbQ->DFDisableIdc == 0))
  {
    Macroblock *MbP = &(p_Vid->mb_data[pixP.mb_addr]);    

    int incQ = ((MbP->mb_field && !MbQ->mb_field) ? 2 * width : width);
    int incP = ((MbQ->mb_field  && !MbP->mb_field) ? 2 * width : width);

    // Average QP of the two blocks
    int QP = (MbP->qpc[uv] + MbQ->qpc[uv] + 1) >> 1;

    int indexA = iClip3(0, MAX_QP, QP + AlphaC0Offset);
    int indexB = iClip3(0, MAX_QP, QP + BetaOffset);

    int Alpha   = ALPHA_TABLE[indexA] * bitdepth_scale;
    int Beta    = BETA_TABLE [indexB] * bitdepth_scale;    

    if ((Alpha | Beta )!= 0)
    {
      const byte* ClipTab = CLIP_TAB[indexA];
      int      pel, Strng ; 
      int      StrengthIdx;
      for( pel = 0 ; pel < PelNum ; ++pel )
      {
        StrengthIdx = (PelNum == 8) ? ((MbQ->mb_field && !MbP->mb_field) ? pel << 1 :((pel >> 1) << 2) + (pel & 0x01)) : pel;

        if( (Strng = Strength[StrengthIdx]) != 0)
        {
          imgpel *SrcPtrQ = &(Img[pixQ.pos_y][pixQ.pos_x]);
          imgpel *SrcPtrP = &(Img[pixP.pos_y][pixP.pos_x]);

          imgpel L0  = *SrcPtrP;
          imgpel R0  = *SrcPtrQ;

          if( iabs( R0 - L0 ) < Alpha )
          {
            imgpel L1  = SrcPtrP[-incP];
            imgpel R1  = SrcPtrQ[ incQ];      
            if( ((iabs( R0 - R1) - Beta < 0)  && (iabs(L0 - L1) - Beta < 0 ))  )
            {
              if( Strng == 4 )    // INTRA strong filtering
              {
                SrcPtrQ[0] = (imgpel) ( ((R1 << 1) + R0 + L1 + 2) >> 2 );
                SrcPtrP[0] = (imgpel) ( ((L1 << 1) + L0 + R1 + 2) >> 2 );
              }
              else
              {
                int C0  = ClipTab[ Strng ] * bitdepth_scale;
                int tc0  = (C0 + 1);
                int dif = iClip3( -tc0, tc0, ( ((R0 - L0) << 2) + (L1 - R1) + 4) >> 3 );

                if (dif)
                {
                  *SrcPtrP = (imgpel) iClip1 ( max_imgpel_value, L0 + dif );
                  *SrcPtrQ = (imgpel) iClip1 ( max_imgpel_value, R0 - dif );
                }
              }
            }
          }
        }    
        pixP.pos_x++;
        pixQ.pos_x++;
      }
    }
  }
}


/*!
 *****************************************************************************************
 * \brief
 *    Get Deblocking filter strength for one macroblock.
 *****************************************************************************************
 */
void get_db_strength_mbaff(VideoParameters *p_Vid, StorablePicture *p, int MbQAddr)
{
  Macroblock   *MbQ = &(p_Vid->mb_data[MbQAddr]) ; // current Mb

  // return, if filter is disabled
  if (MbQ->DFDisableIdc == 1) 
  {
    MbQ->DeblockCall = 0;
  }
  else
  {
    int           edge;

    short         mb_x, mb_y;

    int           filterNon8x8LumaEdgesFlag[4] = {1,1,1,1};
    int           filterLeftMbEdgeFlag;
    int           filterTopMbEdgeFlag;

    Slice  *currSlice = MbQ->p_Slice;
    int       mvlimit = ((p->structure!=FRAME) || (p->mb_aff_frame_flag && MbQ->mb_field)) ? 2 : 4;

    seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;

    MbQ->DeblockCall = 1;
    get_mb_pos (p_Vid, MbQAddr, p_Vid->mb_size[IS_LUMA], &mb_x, &mb_y);

    if (MbQ->mb_type == I8MB)
      assert(MbQ->luma_transform_size_8x8_flag);

    filterNon8x8LumaEdgesFlag[1] =
      filterNon8x8LumaEdgesFlag[3] = !(MbQ->luma_transform_size_8x8_flag);

    filterLeftMbEdgeFlag = (mb_x != 0);
    filterTopMbEdgeFlag  = (mb_y != 0);

    if (p->mb_aff_frame_flag && mb_y == MB_BLOCK_SIZE && MbQ->mb_field)
      filterTopMbEdgeFlag = 0;

    if (MbQ->DFDisableIdc==2)
    {
      // don't filter at slice boundaries
      filterLeftMbEdgeFlag = MbQ->mbAvailA;
      // if this the bottom of a frame macroblock pair then always filter the top edge
      filterTopMbEdgeFlag  = (p->mb_aff_frame_flag && !MbQ->mb_field && (MbQAddr & 0x01)) ? 1 : MbQ->mbAvailB;
    }

    CheckAvailabilityOfNeighborsMBAFF(MbQ);

    // Vertical deblocking
    for (edge = 0; edge < 4 ; ++edge )    
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0)
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc != YUV444)
          continue;
        else if (edge > 0)
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P16x8)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P8x16) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterLeftMbEdgeFlag )
      {      
        // Strength for 4 blks in 1 stripe
        p_Vid->GetStrengthVer(MbQ, edge, mvlimit, p);
      }
    }//end edge

    // horizontal deblocking  
    for( edge = 0; edge < 4 ; ++edge )
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0)
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc==YUV420)
          continue;
        else if (edge > 0)
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P8x16)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P16x8) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterTopMbEdgeFlag )
      {
        p_Vid->GetStrengthHor(MbQ, edge, mvlimit, p);
      }
    }//end edge  

    MbQ->DeblockCall = 0;
  }
}


/*!
 *****************************************************************************************
 * \brief
 *    Performing Deblocking for one macroblock.
 *****************************************************************************************
 */
void perform_db_mbaff(VideoParameters *p_Vid, StorablePicture *p, int MbQAddr)
{
  Macroblock   *MbQ = &(p_Vid->mb_data[MbQAddr]) ; // current Mb

  // return, if filter is disabled
  if (MbQ->DFDisableIdc == 1) 
  {
    MbQ->DeblockCall = 0;
  }
  else
  {
    int           edge;

    short         mb_x, mb_y;

    int           filterNon8x8LumaEdgesFlag[4] = {1,1,1,1};
    int           filterLeftMbEdgeFlag;
    int           filterTopMbEdgeFlag;
    int           edge_cr;

    imgpel     **imgY = p->imgY;
    imgpel   ***imgUV = p->imgUV;
    Slice  *currSlice = MbQ->p_Slice;
    int       mvlimit = ((p->structure!=FRAME) || (p->mb_aff_frame_flag && MbQ->mb_field)) ? 2 : 4;

    seq_parameter_set_rbsp_t *active_sps = p_Vid->active_sps;

    MbQ->DeblockCall = 1;
    get_mb_pos (p_Vid, MbQAddr, p_Vid->mb_size[IS_LUMA], &mb_x, &mb_y);

    if (MbQ->mb_type == I8MB)
      assert(MbQ->luma_transform_size_8x8_flag);

    filterNon8x8LumaEdgesFlag[1] =
      filterNon8x8LumaEdgesFlag[3] = !(MbQ->luma_transform_size_8x8_flag);

    filterLeftMbEdgeFlag = (mb_x != 0);
    filterTopMbEdgeFlag  = (mb_y != 0);

    if (p->mb_aff_frame_flag && mb_y == MB_BLOCK_SIZE && MbQ->mb_field)
      filterTopMbEdgeFlag = 0;

    if (MbQ->DFDisableIdc==2)
    {
      // don't filter at slice boundaries
      filterLeftMbEdgeFlag = MbQ->mbAvailA;
      // if this the bottom of a frame macroblock pair then always filter the top edge
      filterTopMbEdgeFlag  = (p->mb_aff_frame_flag && !MbQ->mb_field && (MbQAddr & 0x01)) ? 1 : MbQ->mbAvailB;
    }

    CheckAvailabilityOfNeighborsMBAFF(MbQ);

    // Vertical deblocking
    for (edge = 0; edge < 4 ; ++edge )    
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0)
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc != YUV444)
          continue;
        else if (edge > 0 && (currSlice->slice_type == P_SLICE || currSlice->slice_type == B_SLICE))
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P16x8)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P8x16) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterLeftMbEdgeFlag )
      {      
        byte *Strength = MbQ->strength_ver[edge];

        if ((*((int64 *) Strength)) || ((*(((int64 *) Strength) + 1)))) // only if one of the 16 Strength bytes is != 0
        {
          if (filterNon8x8LumaEdgesFlag[edge])
          {
            p_Vid->EdgeLoopLumaVer( PLANE_Y, imgY, Strength, MbQ, edge << 2);
            if(currSlice->chroma444_not_separate)
            {
              p_Vid->EdgeLoopLumaVer(PLANE_U, imgUV[0], Strength, MbQ, edge << 2);
              p_Vid->EdgeLoopLumaVer(PLANE_V, imgUV[1], Strength, MbQ, edge << 2);
            }
          }
          if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422)
          {
            edge_cr = chroma_edge[0][edge][p->chroma_format_idc];
            if( (imgUV != NULL) && (edge_cr >= 0))
            {
              p_Vid->EdgeLoopChromaVer( imgUV[0], Strength, MbQ, edge_cr, 0, p);
              p_Vid->EdgeLoopChromaVer( imgUV[1], Strength, MbQ, edge_cr, 1, p);
            }
          }
        }        
      }
    }//end edge

    // horizontal deblocking  
    for( edge = 0; edge < 4 ; ++edge )
    {
      // If cbp == 0 then deblocking for some macroblock types could be skipped
      if (MbQ->cbp == 0)
      {
        if (filterNon8x8LumaEdgesFlag[edge] == 0 && active_sps->chroma_format_idc==YUV420)
          continue;
        else if (edge > 0 && (currSlice->slice_type == P_SLICE || currSlice->slice_type == B_SLICE))
        {
          if (((MbQ->mb_type == PSKIP && currSlice->slice_type == P_SLICE) || (MbQ->mb_type == P16x16) || (MbQ->mb_type == P8x16)))
            continue;
          else if ((edge & 0x01) && ((MbQ->mb_type == P16x8) || (currSlice->slice_type == B_SLICE && MbQ->mb_type == BSKIP_DIRECT && active_sps->direct_8x8_inference_flag)))
            continue;
        }
      }

      if( edge || filterTopMbEdgeFlag )
      {
        byte *Strength = MbQ->strength_hor[edge];

        if ((*((int64 *) Strength)) || ((*(((int64 *) Strength) + 1)))) // only if one of the 16 Strength bytes is != 0
        {
          if (filterNon8x8LumaEdgesFlag[edge])
          {
            p_Vid->EdgeLoopLumaHor( PLANE_Y, imgY, Strength, MbQ, edge << 2, p) ;
            if(currSlice->chroma444_not_separate)
            {
              p_Vid->EdgeLoopLumaHor(PLANE_U, imgUV[0], Strength, MbQ, edge << 2, p);
              p_Vid->EdgeLoopLumaHor(PLANE_V, imgUV[1], Strength, MbQ, edge << 2, p);
            }
          }

          if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422)
          {
            edge_cr = chroma_edge[1][edge][p->chroma_format_idc];
            if( (imgUV != NULL) && (edge_cr >= 0))
            {
              p_Vid->EdgeLoopChromaHor( imgUV[0], Strength, MbQ, edge_cr, 0, p);
              p_Vid->EdgeLoopChromaHor( imgUV[1], Strength, MbQ, edge_cr, 1, p);
            }
          }
        }

        if (!edge && !MbQ->mb_field && MbQ->mixedModeEdgeFlag) //currSlice->mixedModeEdgeFlag) 
        {          
          // this is the extra horizontal edge between a frame macroblock pair and a field above it
          MbQ->DeblockCall = 2;
          p_Vid->GetStrengthHor(MbQ, 4, mvlimit, p); // Strength for 4 blks in 1 stripe

          //if( *((int*)Strength) )                      // only if one of the 4 Strength bytes is != 0
          {
            if (filterNon8x8LumaEdgesFlag[edge])
            {

              p_Vid->EdgeLoopLumaHor(PLANE_Y, imgY, Strength, MbQ, MB_BLOCK_SIZE, p) ;
              if(currSlice->chroma444_not_separate)
              {
                p_Vid->EdgeLoopLumaHor(PLANE_U, imgUV[0], Strength, MbQ, MB_BLOCK_SIZE, p) ;
                p_Vid->EdgeLoopLumaHor(PLANE_V, imgUV[1], Strength, MbQ, MB_BLOCK_SIZE, p) ;
              }
            }
            if (active_sps->chroma_format_idc==YUV420 || active_sps->chroma_format_idc==YUV422) 
            {
              edge_cr = chroma_edge[1][edge][p->chroma_format_idc];
              if( (imgUV != NULL) && (edge_cr >= 0))
              {
                p_Vid->EdgeLoopChromaHor( imgUV[0], Strength, MbQ, MB_BLOCK_SIZE, 0, p) ;
                p_Vid->EdgeLoopChromaHor( imgUV[1], Strength, MbQ, MB_BLOCK_SIZE, 1, p) ;
              }
            }
          }
          MbQ->DeblockCall = 1;
        }
      }
    }//end edge  

    MbQ->DeblockCall = 0;
  }
}

