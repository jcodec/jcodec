/*!
 ***********************************************************************
 *  \file
 *      mbuffer.h
 *
 *  \brief
 *      Frame buffer functions
 *
 *  \author
 *      Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Karsten Suehring
 *      - Alexis Michael Tourapis  <alexismt@ieee.org>
 
 *      - Jill Boyce               <jill.boyce@thomson.net>
 *      - Saurav K Bandyopadhyay   <saurav@ieee.org>
 *      - Zhenyu Wu                <Zhenyu.Wu@thomson.net
 *      - Purvin Pandit            <Purvin.Pandit@thomson.net>
 *
 ***********************************************************************
 */
#ifndef _MBUFFERDEC_H_
#define _MBUFFERDEC_H_

#include "global.h"

#define MAX_LIST_SIZE 33
//! definition of pic motion parameters
typedef struct pic_motion_params_old
{
  byte *      mb_field;      //!< field macroblock indicator
} PicMotionParamsOld;

//! definition of pic motion parameters
typedef struct pic_motion_params
{
  struct storable_picture *ref_pic[2];  //!< referrence picture pointer
  MotionVector             mv[2];       //!< motion vector  
  char                     ref_idx[2];  //!< reference picture   [list][subblock_y][subblock_x]
  //byte                   mb_field;    //!< field macroblock indicator
  byte                     slice_no;
} PicMotionParams;

//! definition a picture (field or frame)
typedef struct storable_picture
{
  PictureStructure structure;

  int         poc;
  int         top_poc;
  int         bottom_poc;
  int         frame_poc;
  unsigned int  frame_num;
  unsigned int  recovery_frame;

  int         pic_num;
  int         long_term_pic_num;
  int         long_term_frame_idx;

  byte        is_long_term;
  int         used_for_reference;
  int         is_output;
  int         non_existing;
  int         separate_colour_plane_flag;

  short       max_slice_id;

  int         size_x, size_y, size_x_cr, size_y_cr;
  int         size_x_m1, size_y_m1, size_x_cr_m1, size_y_cr_m1;
  int         coded_frame;
  int         mb_aff_frame_flag;
  unsigned    PicWidthInMbs;
  unsigned    PicSizeInMbs;
  int         iLumaPadY, iLumaPadX;
  int         iChromaPadY, iChromaPadX;


  imgpel **     imgY;         //!< Y picture component
  imgpel ***    imgUV;        //!< U and V picture components

  struct pic_motion_params **mv_info;          //!< Motion info
  struct pic_motion_params **JVmv_info[MAX_PLANE];          //!< Motion info

  struct pic_motion_params_old  motion;              //!< Motion info  
  struct pic_motion_params_old  JVmotion[MAX_PLANE]; //!< Motion info for 4:4:4 independent mode decoding

  struct storable_picture *top_field;     // for mb aff, if frame for referencing the top field
  struct storable_picture *bottom_field;  // for mb aff, if frame for referencing the bottom field
  struct storable_picture *frame;         // for mb aff, if field for referencing the combined frame

  int         slice_type;
  int         idr_flag;
  int         no_output_of_prior_pics_flag;
  int         long_term_reference_flag;
  int         adaptive_ref_pic_buffering_flag;

  int         chroma_format_idc;
  int         frame_mbs_only_flag;
  int         frame_cropping_flag;
  int         frame_crop_left_offset;
  int         frame_crop_right_offset;
  int         frame_crop_top_offset;
  int         frame_crop_bottom_offset;
  int         qp;
  int         chroma_qp_offset[2];
  int         slice_qp_delta;
  DecRefPicMarking_t *dec_ref_pic_marking_buffer;                    //!< stores the memory management control operations

  // picture error concealment
  int         concealed_pic; //indicates if this is a concealed picture
  
  // variables for tone mapping
  int         seiHasTone_mapping;
  int         tone_mapping_model_id;
  int         tonemapped_bit_depth;  
  imgpel*     tone_mapping_lut;                //!< tone mapping look up table

  int         proc_flag;
#if (MVC_EXTENSION_ENABLE)
  int         view_id;
  int         inter_view_flag;
  int         anchor_pic_flag;
#endif
  int         iLumaStride;
  int         iChromaStride;
  int         iLumaExpandedHeight;
  int         iChromaExpandedHeight;
  imgpel **cur_imgY; // for more efficient get_block_luma
  int no_ref;
  int iCodingType;
  //
  char listXsize[MAX_NUM_SLICES][2];
  struct storable_picture **listX[MAX_NUM_SLICES][2];
  int         layer_id;
} StorablePicture;

typedef StorablePicture *StorablePicturePtr;

//! Frame Stores for Decoded Picture Buffer
typedef struct frame_store
{
  int       is_used;                //!< 0=empty; 1=top; 2=bottom; 3=both fields (or frame)
  int       is_reference;           //!< 0=not used for ref; 1=top used; 2=bottom used; 3=both fields (or frame) used
  int       is_long_term;           //!< 0=not used for ref; 1=top used; 2=bottom used; 3=both fields (or frame) used
  int       is_orig_reference;      //!< original marking by nal_ref_idc: 0=not used for ref; 1=top used; 2=bottom used; 3=both fields (or frame) used

  int       is_non_existent;

  unsigned  frame_num;
  unsigned  recovery_frame;

  int       frame_num_wrap;
  int       long_term_frame_idx;
  int       is_output;
  int       poc;

  // picture error concealment
  int concealment_reference;

  StorablePicture *frame;
  StorablePicture *top_field;
  StorablePicture *bottom_field;

#if (MVC_EXTENSION_ENABLE)
  int       view_id;
  int       inter_view_flag[2];
  int       anchor_pic_flag[2];
#endif
  int       layer_id;
} FrameStore;


//! Decoded Picture Buffer
typedef struct decoded_picture_buffer
{
  VideoParameters *p_Vid;
  InputParameters *p_Inp;
  FrameStore  **fs;
  FrameStore  **fs_ref;
  FrameStore  **fs_ltref;
  FrameStore  **fs_ilref; // inter-layer reference (for multi-layered codecs)
  unsigned      size;
  unsigned      used_size;
  unsigned      ref_frames_in_buffer;
  unsigned      ltref_frames_in_buffer;
  int           last_output_poc;
#if (MVC_EXTENSION_ENABLE)
  int           last_output_view_id;
#endif
  int           max_long_term_pic_idx;  


  int           init_done;
  int           num_ref_frames;

  FrameStore   *last_picture;
  unsigned     used_size_il;
  int          layer_id;

  //DPB related function;

} DecodedPictureBuffer;


extern void              init_dpb(VideoParameters *p_Vid, DecodedPictureBuffer *p_Dpb, int type);
extern void              re_init_dpb(VideoParameters *p_Vid, DecodedPictureBuffer *p_Dpb, int type);
extern void              free_dpb(DecodedPictureBuffer *p_Dpb);
extern FrameStore*       alloc_frame_store(void);
extern void              free_frame_store (FrameStore* f);
extern StorablePicture*  alloc_storable_picture(VideoParameters *p_Vid, PictureStructure type, int size_x, int size_y, int size_x_cr, int size_y_cr, int is_output);
extern void              free_storable_picture (StorablePicture* p);
extern void              store_picture_in_dpb(DecodedPictureBuffer *p_Dpb, StorablePicture* p);
extern StorablePicture*  get_short_term_pic (Slice *currSlice, DecodedPictureBuffer *p_Dpb, int picNum);

#if (MVC_EXTENSION_ENABLE)
extern void             idr_memory_management(DecodedPictureBuffer *p_Dpb, StorablePicture* p);
extern void             flush_dpbs(DecodedPictureBuffer **p_Dpb, int nLayers);
extern int              GetMaxDecFrameBuffering(VideoParameters *p_Vid);
extern void             append_interview_list(DecodedPictureBuffer *p_Dpb, 
                                              PictureStructure currPicStructure, int list_idx, 
                                              FrameStore **list, int *listXsize, int currPOC, 
                                              int curr_view_id, int anchor_pic_flag);
#endif

extern void unmark_for_reference(FrameStore* fs);
extern void unmark_for_long_term_reference(FrameStore* fs);
extern void remove_frame_from_dpb(DecodedPictureBuffer *p_Dpb, int pos);

extern void             flush_dpb(DecodedPictureBuffer *p_Dpb);
extern void             init_lists_p_slice (Slice *currSlice);
extern void             init_lists_b_slice (Slice *currSlice);
extern void             init_lists_i_slice (Slice *currSlice);
extern void             update_pic_num     (Slice *currSlice);

extern void             dpb_split_field      (VideoParameters *p_Vid, FrameStore *fs);
extern void             dpb_combine_field    (VideoParameters *p_Vid, FrameStore *fs);
extern void             dpb_combine_field_yuv(VideoParameters *p_Vid, FrameStore *fs);

extern void             reorder_ref_pic_list(Slice *currSlice, int cur_list);

extern void             init_mbaff_lists     (VideoParameters *p_Vid, Slice *currSlice);
extern void             alloc_ref_pic_list_reordering_buffer(Slice *currSlice);
extern void             free_ref_pic_list_reordering_buffer(Slice *currSlice);

extern void             fill_frame_num_gap(VideoParameters *p_Vid, Slice *pSlice);

extern void compute_colocated (Slice *currSlice, StorablePicture **listX[6]);


extern int init_img_data(VideoParameters *p_Vid, ImageData *p_ImgData, seq_parameter_set_rbsp_t *sps);
extern void free_img_data(VideoParameters *p_Vid, ImageData *p_ImgData);
extern void pad_dec_picture(VideoParameters *p_Vid, StorablePicture *dec_picture);
extern void pad_buf(imgpel *pImgBuf, int iWidth, int iHeight, int iStride, int iPadX, int iPadY);
extern void process_picture_in_dpb_s(VideoParameters *p_Vid, StorablePicture *p_pic);
extern StorablePicture * clone_storable_picture( VideoParameters *p_Vid, StorablePicture *p_pic );
extern void store_proc_picture_in_dpb(DecodedPictureBuffer *p_Dpb, StorablePicture* p);
#endif

