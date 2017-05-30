
/*!
 ***********************************************************************
 *  \file
 *      mbuffer_common.c
 *
 *  \brief
 *      Common (Encoder/Decoder) Frame buffer functions
 *
 *  \author
 *      Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Karsten Suehring
 *      - Alexis Tourapis                 <alexismt@ieee.org>
 *      - Yuwen He                        <yhe@dolby.com>
 ***********************************************************************
 */

#include <limits.h>

#include "global.h"
#include "header.h"
#include "image.h"
#include "mbuffer_common.h"
#include "mbuffer.h"
#include "memalloc.h"
#include "output.h"
#include "fast_memory.h"
#include "input.h"

static void unmark_long_term_field_for_reference_by_frame_idx(DecodedPictureBuffer *p_Dpb, PictureStructure structure, int long_term_frame_idx, int mark_current, unsigned curr_frame_num, int curr_pic_num);
/*!
 ************************************************************************
 * \brief
 *    Check if one of the frames/fields in frame store is used for short-term reference
 ************************************************************************
 */
static int is_short_term_reference(FrameStore* fs)
{

  if (fs->is_used==3) // frame
  {
    if ((fs->frame->used_for_reference)&&(!fs->frame->is_long_term))
    {
      return 1;
    }
  }

  if (fs->is_used & 1) // top field
  {
    if (fs->top_field)
    {
      if ((fs->top_field->used_for_reference)&&(!fs->top_field->is_long_term))
      {
        return 1;
      }
    }
  }

  if (fs->is_used & 2) // bottom field
  {
    if (fs->bottom_field)
    {
      if ((fs->bottom_field->used_for_reference)&&(!fs->bottom_field->is_long_term))
      {
        return 1;
      }
    }
  }
  return 0;
}


/*!
 ************************************************************************
 * \brief
 *    Check if one of the frames/fields in frame store is used for short-term reference
 ************************************************************************
 */
static int is_long_term_reference(FrameStore* fs)
{

  if (fs->is_used==3) // frame
  {
    if ((fs->frame->used_for_reference)&&(fs->frame->is_long_term))
    {
      return 1;
    }
  }

  if (fs->is_used & 1) // top field
  {
    if (fs->top_field)
    {
      if ((fs->top_field->used_for_reference)&&(fs->top_field->is_long_term))
      {
        return 1;
      }
    }
  }

  if (fs->is_used & 2) // bottom field
  {
    if (fs->bottom_field)
    {
      if ((fs->bottom_field->used_for_reference)&&(fs->bottom_field->is_long_term))
      {
        return 1;
      }
    }
  }
  return 0;
}

/*!
 ************************************************************************
 * \brief
 *    Generates a alternating field list from a given FrameStore list
 *
 ************************************************************************
 */
void gen_pic_list_from_frame_list(PictureStructure currStructure, FrameStore **fs_list, int list_idx, StorablePicture **list, char *list_size, int long_term)
{
  int top_idx = 0;
  int bot_idx = 0;

  int (*is_ref)(StorablePicture *s) = (long_term) ? is_long_ref : is_short_ref;


  if (currStructure == TOP_FIELD)
  {
    while ((top_idx<list_idx)||(bot_idx<list_idx))
    {
      for ( ; top_idx<list_idx; top_idx++)
      {
        if(fs_list[top_idx]->is_used & 1)
        {
          if(is_ref(fs_list[top_idx]->top_field))
          {
            // short term ref pic
            list[(short) *list_size] = fs_list[top_idx]->top_field;
            (*list_size)++;
            top_idx++;
            break;
          }
        }
      }
      for ( ; bot_idx<list_idx; bot_idx++)
      {
        if(fs_list[bot_idx]->is_used & 2)
        {
          if(is_ref(fs_list[bot_idx]->bottom_field))
          {
            // short term ref pic
            list[(short) *list_size] = fs_list[bot_idx]->bottom_field;
            (*list_size)++;
            bot_idx++;
            break;
          }
        }
      }
    }
  }
  if (currStructure == BOTTOM_FIELD)
  {
    while ((top_idx<list_idx)||(bot_idx<list_idx))
    {
      for ( ; bot_idx<list_idx; bot_idx++)
      {
        if(fs_list[bot_idx]->is_used & 2)
        {
          if(is_ref(fs_list[bot_idx]->bottom_field))
          {
            // short term ref pic
            list[(short) *list_size] = fs_list[bot_idx]->bottom_field;
            (*list_size)++;
            bot_idx++;
            break;
          }
        }
      }
      for ( ; top_idx<list_idx; top_idx++)
      {
        if(fs_list[top_idx]->is_used & 1)
        {
          if(is_ref(fs_list[top_idx]->top_field))
          {
            // short term ref pic
            list[(short) *list_size] = fs_list[top_idx]->top_field;
            (*list_size)++;
            top_idx++;
            break;
          }
        }
      }
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Returns long term pic with given LongtermPicNum
 *
 ************************************************************************
 */
StorablePicture*  get_long_term_pic(Slice *currSlice, DecodedPictureBuffer *p_Dpb, int LongtermPicNum)
{
  uint32 i;

  for (i=0; i<p_Dpb->ltref_frames_in_buffer; i++)
  {
    if (currSlice->structure==FRAME)
    {
      if (p_Dpb->fs_ltref[i]->is_reference == 3)
        if ((p_Dpb->fs_ltref[i]->frame->is_long_term)&&(p_Dpb->fs_ltref[i]->frame->long_term_pic_num == LongtermPicNum))
          return p_Dpb->fs_ltref[i]->frame;
    }
    else
    {
      if (p_Dpb->fs_ltref[i]->is_reference & 1)
        if ((p_Dpb->fs_ltref[i]->top_field->is_long_term)&&(p_Dpb->fs_ltref[i]->top_field->long_term_pic_num == LongtermPicNum))
          return p_Dpb->fs_ltref[i]->top_field;
      if (p_Dpb->fs_ltref[i]->is_reference & 2)
        if ((p_Dpb->fs_ltref[i]->bottom_field->is_long_term)&&(p_Dpb->fs_ltref[i]->bottom_field->long_term_pic_num == LongtermPicNum))
          return p_Dpb->fs_ltref[i]->bottom_field;
    }
  }
  return NULL;
}

/*!
 ************************************************************************
 * \brief
 *    Update the list of frame stores that contain reference frames/fields
 *
 ************************************************************************
 */
void update_ref_list(DecodedPictureBuffer *p_Dpb)
{
  unsigned i, j;
  for (i=0, j=0; i<p_Dpb->used_size; i++)
  {
    if (is_short_term_reference(p_Dpb->fs[i]))
    {
      p_Dpb->fs_ref[j++]=p_Dpb->fs[i];
    }
  }

  p_Dpb->ref_frames_in_buffer = j;

  while (j<p_Dpb->size)
  {
    p_Dpb->fs_ref[j++]=NULL;
  }
}



/*!
 ************************************************************************
 * \brief
 *    Update the list of frame stores that contain long-term reference
 *    frames/fields
 *
 ************************************************************************
 */
void update_ltref_list(DecodedPictureBuffer *p_Dpb)
{
  unsigned i, j;
  for (i=0, j=0; i<p_Dpb->used_size; i++)
  {
    if (is_long_term_reference(p_Dpb->fs[i]))
    {
      p_Dpb->fs_ltref[j++] = p_Dpb->fs[i];
    }
  }

  p_Dpb->ltref_frames_in_buffer = j;

  while (j<p_Dpb->size)
  {
    p_Dpb->fs_ltref[j++]=NULL;
  }
}

/*!
 ************************************************************************
 * \brief
 *    Calculate picNumX
 ************************************************************************
 */
static int get_pic_num_x (StorablePicture *p, int difference_of_pic_nums_minus1)
{
  int currPicNum;

  if (p->structure == FRAME)
    currPicNum = p->frame_num;
  else
    currPicNum = 2 * p->frame_num + 1;

  return currPicNum - (difference_of_pic_nums_minus1 + 1);
}

/*!
 ************************************************************************
 * \brief
 *    Adaptive Memory Management: Mark short term picture unused
 ************************************************************************
 */
void mm_unmark_short_term_for_reference(DecodedPictureBuffer *p_Dpb, StorablePicture *p, int difference_of_pic_nums_minus1)
{
  int picNumX;

  uint32 i;

  picNumX = get_pic_num_x(p, difference_of_pic_nums_minus1);

  for (i=0; i<p_Dpb->ref_frames_in_buffer; i++)
  {
    if (p->structure == FRAME)
    {
      if ((p_Dpb->fs_ref[i]->is_reference==3) && (p_Dpb->fs_ref[i]->is_long_term==0))
      {
        if (p_Dpb->fs_ref[i]->frame->pic_num == picNumX)
        {
          unmark_for_reference(p_Dpb->fs_ref[i]);
          return;
        }
      }
    }
    else
    {
      if ((p_Dpb->fs_ref[i]->is_reference & 1) && (!(p_Dpb->fs_ref[i]->is_long_term & 1)))
      {
        if (p_Dpb->fs_ref[i]->top_field->pic_num == picNumX)
        {
          p_Dpb->fs_ref[i]->top_field->used_for_reference = 0;
          p_Dpb->fs_ref[i]->is_reference &= 2;
          if (p_Dpb->fs_ref[i]->is_used == 3)
          {
            p_Dpb->fs_ref[i]->frame->used_for_reference = 0;
          }
          return;
        }
      }
      if ((p_Dpb->fs_ref[i]->is_reference & 2) && (!(p_Dpb->fs_ref[i]->is_long_term & 2)))
      {
        if (p_Dpb->fs_ref[i]->bottom_field->pic_num == picNumX)
        {
          p_Dpb->fs_ref[i]->bottom_field->used_for_reference = 0;
          p_Dpb->fs_ref[i]->is_reference &= 1;
          if (p_Dpb->fs_ref[i]->is_used == 3)
          {
            p_Dpb->fs_ref[i]->frame->used_for_reference = 0;
          }
          return;
        }
      }
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    Adaptive Memory Management: Mark long term picture unused
 ************************************************************************
 */
void mm_unmark_long_term_for_reference(DecodedPictureBuffer *p_Dpb, StorablePicture *p, int long_term_pic_num)
{
  uint32 i;
  for (i=0; i<p_Dpb->ltref_frames_in_buffer; i++)
  {
    if (p->structure == FRAME)
    {
      if ((p_Dpb->fs_ltref[i]->is_reference==3) && (p_Dpb->fs_ltref[i]->is_long_term==3))
      {
        if (p_Dpb->fs_ltref[i]->frame->long_term_pic_num == long_term_pic_num)
        {
          unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
        }
      }
    }
    else
    {
      if ((p_Dpb->fs_ltref[i]->is_reference & 1) && ((p_Dpb->fs_ltref[i]->is_long_term & 1)))
      {
        if (p_Dpb->fs_ltref[i]->top_field->long_term_pic_num == long_term_pic_num)
        {
          p_Dpb->fs_ltref[i]->top_field->used_for_reference = 0;
          p_Dpb->fs_ltref[i]->top_field->is_long_term = 0;
          p_Dpb->fs_ltref[i]->is_reference &= 2;
          p_Dpb->fs_ltref[i]->is_long_term &= 2;
          if (p_Dpb->fs_ltref[i]->is_used == 3)
          {
            p_Dpb->fs_ltref[i]->frame->used_for_reference = 0;
            p_Dpb->fs_ltref[i]->frame->is_long_term = 0;
          }
          return;
        }
      }
      if ((p_Dpb->fs_ltref[i]->is_reference & 2) && ((p_Dpb->fs_ltref[i]->is_long_term & 2)))
      {
        if (p_Dpb->fs_ltref[i]->bottom_field->long_term_pic_num == long_term_pic_num)
        {
          p_Dpb->fs_ltref[i]->bottom_field->used_for_reference = 0;
          p_Dpb->fs_ltref[i]->bottom_field->is_long_term = 0;
          p_Dpb->fs_ltref[i]->is_reference &= 1;
          p_Dpb->fs_ltref[i]->is_long_term &= 1;
          if (p_Dpb->fs_ltref[i]->is_used == 3)
          {
            p_Dpb->fs_ltref[i]->frame->used_for_reference = 0;
            p_Dpb->fs_ltref[i]->frame->is_long_term = 0;
          }
          return;
        }
      }
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    Mark a long-term reference frame or complementary field pair unused for referemce
 ************************************************************************
 */
static void unmark_long_term_frame_for_reference_by_frame_idx(DecodedPictureBuffer *p_Dpb, int long_term_frame_idx)
{
  uint32 i;
  for(i=0; i<p_Dpb->ltref_frames_in_buffer; i++)
  {
    if (p_Dpb->fs_ltref[i]->long_term_frame_idx == long_term_frame_idx)
      unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
  }
}



/*!
 ************************************************************************
 * \brief
 *    mark a picture as long-term reference
 ************************************************************************
 */
static void mark_pic_long_term(DecodedPictureBuffer *p_Dpb, StorablePicture* p, int long_term_frame_idx, int picNumX)
{
  uint32 i;
  int add_top, add_bottom;

  if (p->structure == FRAME)
  {
    for (i=0; i<p_Dpb->ref_frames_in_buffer; i++)
    {
      if (p_Dpb->fs_ref[i]->is_reference == 3)
      {
        if ((!p_Dpb->fs_ref[i]->frame->is_long_term)&&(p_Dpb->fs_ref[i]->frame->pic_num == picNumX))
        {
          p_Dpb->fs_ref[i]->long_term_frame_idx = p_Dpb->fs_ref[i]->frame->long_term_frame_idx
                                             = long_term_frame_idx;
          p_Dpb->fs_ref[i]->frame->long_term_pic_num = long_term_frame_idx;
          p_Dpb->fs_ref[i]->frame->is_long_term = 1;

          if (p_Dpb->fs_ref[i]->top_field && p_Dpb->fs_ref[i]->bottom_field)
          {
            p_Dpb->fs_ref[i]->top_field->long_term_frame_idx = p_Dpb->fs_ref[i]->bottom_field->long_term_frame_idx
                                                          = long_term_frame_idx;
            p_Dpb->fs_ref[i]->top_field->long_term_pic_num = long_term_frame_idx;
            p_Dpb->fs_ref[i]->bottom_field->long_term_pic_num = long_term_frame_idx;

            p_Dpb->fs_ref[i]->top_field->is_long_term = p_Dpb->fs_ref[i]->bottom_field->is_long_term
                                                   = 1;

          }
          p_Dpb->fs_ref[i]->is_long_term = 3;
          return;
        }
      }
    }
    printf ("Warning: reference frame for long term marking not found\n");
  }
  else
  {
    if (p->structure == TOP_FIELD)
    {
      add_top    = 1;
      add_bottom = 0;
    }
    else
    {
      add_top    = 0;
      add_bottom = 1;
    }
    for (i=0; i<p_Dpb->ref_frames_in_buffer; i++)
    {
      if (p_Dpb->fs_ref[i]->is_reference & 1)
      {
        if ((!p_Dpb->fs_ref[i]->top_field->is_long_term)&&(p_Dpb->fs_ref[i]->top_field->pic_num == picNumX))
        {
          if ((p_Dpb->fs_ref[i]->is_long_term) && (p_Dpb->fs_ref[i]->long_term_frame_idx != long_term_frame_idx))
          {
              printf ("Warning: assigning long_term_frame_idx different from other field\n");
          }

          p_Dpb->fs_ref[i]->long_term_frame_idx = p_Dpb->fs_ref[i]->top_field->long_term_frame_idx
                                             = long_term_frame_idx;
          p_Dpb->fs_ref[i]->top_field->long_term_pic_num = 2 * long_term_frame_idx + add_top;
          p_Dpb->fs_ref[i]->top_field->is_long_term = 1;
          p_Dpb->fs_ref[i]->is_long_term |= 1;
          if (p_Dpb->fs_ref[i]->is_long_term == 3)
          {
            p_Dpb->fs_ref[i]->frame->is_long_term = 1;
            p_Dpb->fs_ref[i]->frame->long_term_frame_idx = p_Dpb->fs_ref[i]->frame->long_term_pic_num = long_term_frame_idx;
          }
          return;
        }
      }
      if (p_Dpb->fs_ref[i]->is_reference & 2)
      {
        if ((!p_Dpb->fs_ref[i]->bottom_field->is_long_term)&&(p_Dpb->fs_ref[i]->bottom_field->pic_num == picNumX))
        {
          if ((p_Dpb->fs_ref[i]->is_long_term) && (p_Dpb->fs_ref[i]->long_term_frame_idx != long_term_frame_idx))
          {
              printf ("Warning: assigning long_term_frame_idx different from other field\n");
          }

          p_Dpb->fs_ref[i]->long_term_frame_idx = p_Dpb->fs_ref[i]->bottom_field->long_term_frame_idx
                                             = long_term_frame_idx;
          p_Dpb->fs_ref[i]->bottom_field->long_term_pic_num = 2 * long_term_frame_idx + add_bottom;
          p_Dpb->fs_ref[i]->bottom_field->is_long_term = 1;
          p_Dpb->fs_ref[i]->is_long_term |= 2;
          if (p_Dpb->fs_ref[i]->is_long_term == 3)
          {
            p_Dpb->fs_ref[i]->frame->is_long_term = 1;
            p_Dpb->fs_ref[i]->frame->long_term_frame_idx = p_Dpb->fs_ref[i]->frame->long_term_pic_num = long_term_frame_idx;
          }
          return;
        }
      }
    }
    printf ("Warning: reference field for long term marking not found\n");
  }
}


/*!
 ************************************************************************
 * \brief
 *    Assign a long term frame index to a short term picture
 ************************************************************************
 */
void mm_assign_long_term_frame_idx(DecodedPictureBuffer *p_Dpb, StorablePicture* p, int difference_of_pic_nums_minus1, int long_term_frame_idx)
{
  int picNumX = get_pic_num_x(p, difference_of_pic_nums_minus1);

  // remove frames/fields with same long_term_frame_idx
  if (p->structure == FRAME)
  {
    unmark_long_term_frame_for_reference_by_frame_idx(p_Dpb, long_term_frame_idx);
  }
  else
  {
    unsigned i;
    PictureStructure structure = FRAME;

    for (i=0; i<p_Dpb->ref_frames_in_buffer; i++)
    {
      if (p_Dpb->fs_ref[i]->is_reference & 1)
      {
        if (p_Dpb->fs_ref[i]->top_field->pic_num == picNumX)
        {
          structure = TOP_FIELD;
          break;
        }
      }
      if (p_Dpb->fs_ref[i]->is_reference & 2)
      {
        if (p_Dpb->fs_ref[i]->bottom_field->pic_num == picNumX)
        {
          structure = BOTTOM_FIELD;
          break;
        }
      }
    }
    if (structure==FRAME)
    {
      error ("field for long term marking not found",200);
    }

    unmark_long_term_field_for_reference_by_frame_idx(p_Dpb, structure, long_term_frame_idx, 0, 0, picNumX);
  }

  mark_pic_long_term(p_Dpb, p, long_term_frame_idx, picNumX);
}

/*!
 ************************************************************************
 * \brief
 *    Set new max long_term_frame_idx
 ************************************************************************
 */
void mm_update_max_long_term_frame_idx(DecodedPictureBuffer *p_Dpb, int max_long_term_frame_idx_plus1)
{
  uint32 i;

  p_Dpb->max_long_term_pic_idx = max_long_term_frame_idx_plus1 - 1;

  // check for invalid frames
  for (i=0; i<p_Dpb->ltref_frames_in_buffer; i++)
  {
    if (p_Dpb->fs_ltref[i]->long_term_frame_idx > p_Dpb->max_long_term_pic_idx)
    {
      unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
    }
  }
}


/*!
 ************************************************************************
 * \brief
 *    Mark all long term reference pictures unused for reference
 ************************************************************************
 */
void mm_unmark_all_long_term_for_reference (DecodedPictureBuffer *p_Dpb)
{
  mm_update_max_long_term_frame_idx(p_Dpb, 0);
}

/*!
 ************************************************************************
 * \brief
 *    Mark all short term reference pictures unused for reference
 ************************************************************************
 */
void mm_unmark_all_short_term_for_reference (DecodedPictureBuffer *p_Dpb)
{
  unsigned int i;
  for (i=0; i<p_Dpb->ref_frames_in_buffer; i++)
  {
    unmark_for_reference(p_Dpb->fs_ref[i]);
  }
  update_ref_list(p_Dpb);
}


/*!
 ************************************************************************
 * \brief
 *    Mark the current picture used for long term reference
 ************************************************************************
 */
void mm_mark_current_picture_long_term(DecodedPictureBuffer *p_Dpb, StorablePicture *p, int long_term_frame_idx)
{
  // remove long term pictures with same long_term_frame_idx
  if (p->structure == FRAME)
  {
    unmark_long_term_frame_for_reference_by_frame_idx(p_Dpb, long_term_frame_idx);
  }
  else
  {
    unmark_long_term_field_for_reference_by_frame_idx(p_Dpb, p->structure, long_term_frame_idx, 1, p->pic_num, 0);
  }

  p->is_long_term = 1;
  p->long_term_frame_idx = long_term_frame_idx;
}


/*!
 ************************************************************************
 * \brief
 *    Check if one of the frames/fields in frame store is used for reference
 ************************************************************************
 */
int is_used_for_reference(FrameStore* fs)
{
  if (fs->is_reference)
  {
    return 1;
  }

  if (fs->is_used == 3) // frame
  {
    if (fs->frame->used_for_reference)
    {
      return 1;
    }
  }

  if (fs->is_used & 1) // top field
  {
    if (fs->top_field)
    {
      if (fs->top_field->used_for_reference)
      {
        return 1;
      }
    }
  }

  if (fs->is_used & 2) // bottom field
  {
    if (fs->bottom_field)
    {
      if (fs->bottom_field->used_for_reference)
      {
        return 1;
      }
    }
  }
  return 0;
}

/*!
 ************************************************************************
 * \brief
 *    find smallest POC in the DPB.
 ************************************************************************
 */
void get_smallest_poc(DecodedPictureBuffer *p_Dpb, int *poc,int * pos)
{
  uint32 i;

  if (p_Dpb->used_size<1)
  {
    error("Cannot determine smallest POC, DPB empty.",150);
  }

  *pos=-1;
  *poc = INT_MAX;
  for (i = 0; i < p_Dpb->used_size; i++)
  {
    if ((*poc > p_Dpb->fs[i]->poc)&&(!p_Dpb->fs[i]->is_output))
    {
      *poc = p_Dpb->fs[i]->poc;
      *pos = i;
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *    Remove a picture from DPB which is no longer needed.
 ************************************************************************
 */
int remove_unused_frame_from_dpb(DecodedPictureBuffer *p_Dpb)
{
  uint32 i;

  // check for frames that were already output and no longer used for reference
  for (i = 0; i < p_Dpb->used_size; i++)
  {
    if (p_Dpb->fs[i]->is_output && (!is_used_for_reference(p_Dpb->fs[i])))
    {
      remove_frame_from_dpb(p_Dpb, i);
      return 1;
    }
  }
  return 0;
}

/*!
 ************************************************************************
 * \brief
 *    Mark a long-term reference field unused for reference only if it's not
 *    the complementary field of the picture indicated by picNumX
 ************************************************************************
 */
static void unmark_long_term_field_for_reference_by_frame_idx(DecodedPictureBuffer *p_Dpb, PictureStructure structure, int long_term_frame_idx, int mark_current, unsigned curr_frame_num, int curr_pic_num)
{
  VideoParameters *p_Vid = p_Dpb->p_Vid;
  unsigned i;

  assert(structure!=FRAME);
  if (curr_pic_num<0)
    curr_pic_num += (2 * p_Vid->max_frame_num);

  for(i=0; i<p_Dpb->ltref_frames_in_buffer; i++)
  {
    if (p_Dpb->fs_ltref[i]->long_term_frame_idx == long_term_frame_idx)
    {
      if (structure == TOP_FIELD)
      {
        if (p_Dpb->fs_ltref[i]->is_long_term == 3)
        {
          unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
        }
        else
        {
          if (p_Dpb->fs_ltref[i]->is_long_term == 1)
          {
            unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
          }
          else
          {
            if (mark_current)
            {
              if (p_Dpb->last_picture)
              {
                if ( ( p_Dpb->last_picture != p_Dpb->fs_ltref[i] )|| p_Dpb->last_picture->frame_num != curr_frame_num)
                  unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
              }
              else
              {
                unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
              }
            }
            else
            {
              if ((p_Dpb->fs_ltref[i]->frame_num) != (unsigned)(curr_pic_num >> 1))
              {
                unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
              }
            }
          }
        }
      }
      if (structure == BOTTOM_FIELD)
      {
        if (p_Dpb->fs_ltref[i]->is_long_term == 3)
        {
          unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
        }
        else
        {
          if (p_Dpb->fs_ltref[i]->is_long_term == 2)
          {
            unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
          }
          else
          {
            if (mark_current)
            {
              if (p_Dpb->last_picture)
              {
                if ( ( p_Dpb->last_picture != p_Dpb->fs_ltref[i] )|| p_Dpb->last_picture->frame_num != curr_frame_num)
                  unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
              }
              else
              {
                unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
              }
            }
            else
            {
              if ((p_Dpb->fs_ltref[i]->frame_num) != (unsigned)(curr_pic_num >> 1))
              {
                unmark_for_long_term_reference(p_Dpb->fs_ltref[i]);
              }
            }
          }
        }
      }
    }
  }
}



