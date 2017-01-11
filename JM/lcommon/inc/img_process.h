/*!
 ************************************************************************  
 * \file img_process.h
 *
 * \brief
 *    Input data Image Processing functions
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Alexis Michael Tourapis <alexis.tourapis@dolby.com>
 *
 ************************************************************************
 */

#ifndef _IMG_PROCESS_H_
#define _IMG_PROCESS_H_


extern int  init_process_image ( VideoParameters *p_Vid, InputParameters *p_Inp );
extern void clear_process_image( VideoParameters *p_Vid, InputParameters *p_Inp);
extern void process_image      ( VideoParameters *p_Vid, InputParameters *p_Inp );



#endif
