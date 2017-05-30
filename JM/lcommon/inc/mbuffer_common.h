/*!
***********************************************************************
*  \file
*      mbuffer_common.h
*
*  \brief
*      Frame buffer functions
*
*  \author
*      Main contributors (see contributors.h for copyright, address and affiliation details)
*      - Karsten Suehring
*      - Alexis Michael Tourapis  <alexismt@ieee.org>
*      - Yuwen He                 <yhe@dolby.com>
***********************************************************************
*/
#ifndef _MBUFFER_COMMON_H_
#define _MBUFFER_COMMON_H_

/*!
 ************************************************************************
 * \brief
 *    compares two stored pictures by picture number for qsort in descending order
 *
 ************************************************************************
 */
static inline int compare_pic_by_pic_num_desc( const void *arg1, const void *arg2 )
{
  int pic_num1 = (*(StorablePicture**)arg1)->pic_num;
  int pic_num2 = (*(StorablePicture**)arg2)->pic_num;

  if (pic_num1 < pic_num2)
    return 1;
  if (pic_num1 > pic_num2)
    return -1;
  else
    return 0;
}

/*!
 ************************************************************************
 * \brief
 *    compares two stored pictures by picture number for qsort in descending order
 *
 ************************************************************************
 */
static inline int compare_pic_by_lt_pic_num_asc( const void *arg1, const void *arg2 )
{
  int long_term_pic_num1 = (*(StorablePicture**)arg1)->long_term_pic_num;
  int long_term_pic_num2 = (*(StorablePicture**)arg2)->long_term_pic_num;

  if ( long_term_pic_num1 < long_term_pic_num2)
    return -1;
  if ( long_term_pic_num1 > long_term_pic_num2)
    return 1;
  else
    return 0;
}

/*!
 ************************************************************************
 * \brief
 *    compares two frame stores by pic_num for qsort in descending order
 *
 ************************************************************************
 */
static inline int compare_fs_by_frame_num_desc( const void *arg1, const void *arg2 )
{
  int frame_num_wrap1 = (*(FrameStore**)arg1)->frame_num_wrap;
  int frame_num_wrap2 = (*(FrameStore**)arg2)->frame_num_wrap;
  if ( frame_num_wrap1 < frame_num_wrap2)
    return 1;
  if ( frame_num_wrap1 > frame_num_wrap2)
    return -1;
  else
    return 0;
}


/*!
 ************************************************************************
 * \brief
 *    compares two frame stores by lt_pic_num for qsort in descending order
 *
 ************************************************************************
 */
static inline int compare_fs_by_lt_pic_idx_asc( const void *arg1, const void *arg2 )
{
  int long_term_frame_idx1 = (*(FrameStore**)arg1)->long_term_frame_idx;
  int long_term_frame_idx2 = (*(FrameStore**)arg2)->long_term_frame_idx;

  if ( long_term_frame_idx1 < long_term_frame_idx2)
    return -1;
  else if ( long_term_frame_idx1 > long_term_frame_idx2)
    return 1;
  else
    return 0;
}


/*!
 ************************************************************************
 * \brief
 *    compares two stored pictures by poc for qsort in ascending order
 *
 ************************************************************************
 */
static inline int compare_pic_by_poc_asc( const void *arg1, const void *arg2 )
{
  int poc1 = (*(StorablePicture**)arg1)->poc;
  int poc2 = (*(StorablePicture**)arg2)->poc;

  if ( poc1 < poc2)
    return -1;  
  else if ( poc1 > poc2)
    return 1;
  else
    return 0;
}


/*!
 ************************************************************************
 * \brief
 *    compares two stored pictures by poc for qsort in descending order
 *
 ************************************************************************
 */
static inline int compare_pic_by_poc_desc( const void *arg1, const void *arg2 )
{
  int poc1 = (*(StorablePicture**)arg1)->poc;
  int poc2 = (*(StorablePicture**)arg2)->poc;

  if (poc1 < poc2)
    return 1;
  else if (poc1 > poc2)
    return -1;
  else
    return 0;
}


/*!
 ************************************************************************
 * \brief
 *    compares two frame stores by poc for qsort in ascending order
 *
 ************************************************************************
 */
static inline int compare_fs_by_poc_asc( const void *arg1, const void *arg2 )
{
  int poc1 = (*(FrameStore**)arg1)->poc;
  int poc2 = (*(FrameStore**)arg2)->poc;

  if (poc1 < poc2)
    return -1;
  else if (poc1 > poc2)
    return 1;
  else
    return 0;
}


/*!
 ************************************************************************
 * \brief
 *    compares two frame stores by poc for qsort in descending order
 *
 ************************************************************************
 */
static inline int compare_fs_by_poc_desc( const void *arg1, const void *arg2 )
{
  int poc1 = (*(FrameStore**)arg1)->poc;
  int poc2 = (*(FrameStore**)arg2)->poc;

  if (poc1 < poc2)
    return 1;
  else if (poc1 > poc2)
    return -1;
  else
    return 0;
}

/*!
 ************************************************************************
 * \brief
 *    returns true, if picture is short term reference picture
 *
 ************************************************************************
 */
static inline int is_short_ref(StorablePicture *s)
{
  return ((s->used_for_reference) && (!(s->is_long_term)));
}


/*!
 ************************************************************************
 * \brief
 *    returns true, if picture is long term reference picture
 *
 ************************************************************************
 */
static inline int is_long_ref(StorablePicture *s)
{
  return ((s->used_for_reference) && (s->is_long_term));
}


extern void gen_pic_list_from_frame_list(PictureStructure currStructure, FrameStore **fs_list, int list_idx, StorablePicture **list, char *list_size, int long_term);
extern StorablePicture*  get_long_term_pic(Slice *currSlice, DecodedPictureBuffer *p_Dpb, int LongtermPicNum);
extern void update_ref_list(DecodedPictureBuffer *p_Dpb);
extern void update_ltref_list(DecodedPictureBuffer *p_Dpb);
extern void mm_mark_current_picture_long_term(DecodedPictureBuffer *p_Dpb, StorablePicture *p, int long_term_frame_idx);
extern void mm_unmark_short_term_for_reference(DecodedPictureBuffer *p_Dpb, StorablePicture *p, int difference_of_pic_nums_minus1);
extern void mm_unmark_long_term_for_reference(DecodedPictureBuffer *p_Dpb, StorablePicture *p, int long_term_pic_num);
extern void mm_assign_long_term_frame_idx(DecodedPictureBuffer *p_Dpb, StorablePicture* p, int difference_of_pic_nums_minus1, int long_term_frame_idx);
extern void mm_update_max_long_term_frame_idx(DecodedPictureBuffer *p_Dpb, int max_long_term_frame_idx_plus1);
extern void mm_unmark_all_short_term_for_reference (DecodedPictureBuffer *p_Dpb);
extern void mm_unmark_all_long_term_for_reference  (DecodedPictureBuffer *p_Dpb);
extern int  is_used_for_reference        (FrameStore* fs);
extern void get_smallest_poc(DecodedPictureBuffer *p_Dpb, int *poc,int * pos);
extern int remove_unused_frame_from_dpb(DecodedPictureBuffer *p_Dpb);

#endif

