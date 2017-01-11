
/*!
 *************************************************************************************
 * \file mb_prediction.h
 *
 * \brief
 *    Functions for macroblock prediction
 *
 * \author
 *     Main contributors (see contributors.h for copyright, address and affiliation details)
 *     - Alexis Michael Tourapis         <alexismt@ieee.org>  
 *************************************************************************************
 */

#ifndef _MB_PREDICTION_H_
#define _MB_PREDICTION_H_

extern int mb_pred_intra4x4      (Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture);
extern int mb_pred_intra16x16    (Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture);
extern int mb_pred_intra8x8      (Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture);

extern int mb_pred_skip          (Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture);
extern int mb_pred_sp_skip       (Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture);
extern int mb_pred_p_inter8x8    (Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture);
extern int mb_pred_p_inter16x16  (Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture);
extern int mb_pred_p_inter16x8   (Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture);
extern int mb_pred_p_inter8x16   (Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture);
extern int mb_pred_b_d4x4spatial (Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture);
extern int mb_pred_b_d8x8spatial (Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture);
extern int mb_pred_b_d4x4temporal(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture);
extern int mb_pred_b_d8x8temporal(Macroblock *currMB, ColorPlane curr_plane, imgpel **currImg, StorablePicture *dec_picture);
extern int mb_pred_b_inter8x8    (Macroblock *currMB, ColorPlane curr_plane, StorablePicture *dec_picture);
extern int mb_pred_ipcm          (Macroblock *currMB);

#endif
