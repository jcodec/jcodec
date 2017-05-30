

/*!
 ****************************************************************************
 * \file errorconcealment.h
 *
 * \brief
 *    Header file for errorconcealment.c
 *
 ****************************************************************************
 */

#ifndef _ERRORCONCEALMENT_H_
#define _ERRORCONCEALMENT_H_

extern int  get_concealed_element(VideoParameters *p_Vid, SyntaxElement *sym);
extern int  set_ec_flag          (VideoParameters *p_Vid, int se);
extern void reset_ec_flags       (VideoParameters *p_Vid);

#endif

