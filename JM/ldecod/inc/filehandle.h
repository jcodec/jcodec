
/*!
 **************************************************************************************
 * \file
 *    filehandle.h
 * \brief
 *     Trace file handling and standard error handling function headers.
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Karsten Suehring
 *      - Alexis Michael Tourapis     <alexismt@ieee.org>
 ***************************************************************************************
 */

#include "contributors.h"

#if TRACE
extern void dectracebitcnt(int count);
extern void tracebits     ( const char *trace_str, int len, int info, int value1);
extern void tracebits2    ( const char *trace_str, int len, int info);
extern void trace_info    ( SyntaxElement *currSE, const char *description_str, int value1 );
#endif

