
/*!
 *************************************************************************************
 * \file win32.c
 *
 * \brief
 *    Platform dependent code
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *      - Karsten Suehring
 *************************************************************************************
 */

#include "global.h"


#ifdef _WIN32

static LARGE_INTEGER freq;

void gettime(TIME_T* time)
{
#ifndef TIMING_DISABLE
  QueryPerformanceCounter(time);
#endif
}

int64 timediff(TIME_T* start, TIME_T* end)
{
#ifndef TIMING_DISABLE
  return (int64)((end->QuadPart - start->QuadPart));
#else
  return 0;
#endif
}

void init_time(void)
{
  QueryPerformanceFrequency(&freq);
}

int64 timenorm(int64  cur_time)
{
#ifndef TIMING_DISABLE
  return (int64)(cur_time * 1000 /(freq.QuadPart));
#else
  return 1;
#endif
}

#else

static struct timezone tz;

void gettime(TIME_T* time)
{
  gettimeofday(time, &tz);
}

void init_time(void)
{
}

int64 timediff(TIME_T* start, TIME_T* end)
{
  int t1, t2;

  t1 =  end->tv_sec  - start->tv_sec;
  t2 =  end->tv_usec - start->tv_usec;
  return (int64) t2 + (int64) t1 * (int64) 1000000;
}

int64 timenorm(int64 cur_time)
{
  return (int64)(cur_time / (int64) 1000);
}
#endif
