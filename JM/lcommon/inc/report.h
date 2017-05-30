
/*!
 ************************************************************************
 * \file report.h
 *
 * \brief
 *    headers for frame format related information
 *
 * \author
 *
 ************************************************************************
 */
#ifndef _REPORT_H_
#define _REPORT_H_
#include "contributors.h"
#include "global.h"
#include "enc_statistics.h"

extern void report                ( VideoParameters *p_Vid, InputParameters *p_Inp, StatParameters *p_Stats );
extern void information_init      ( VideoParameters *p_Vid, InputParameters *p_Inp, StatParameters *p_Stats );
extern void report_frame_statistic( VideoParameters *p_Vid, InputParameters *p_Inp );
extern void report_stats_on_error (void);

#endif

