
/*!
 *************************************************************************************
 * \file resize.c
 *
 * \brief
 *    Scaling common functions
 *
 * \author
 *  Yuwen He <yhe@dolby.com>
 *************************************************************************************
 */

//#include <stdlib.h>
//#include <malloc.h>
#include "global.h"
#include "resize.h"

/****************************************************************************************\
Down-sampling pyramids macros
\****************************************************************************************/

//////////// Filtering macros /////////////

/* COMMON CASE */
/* 1/16[1    4    6    4    1]       */
/* ...| x0 | x1 | x2 | x3 | x4 |...  */
#define  PD_FILTER( x0, x1, x2, x3, x4 ) ((x2)*6+((x1)+(x3))*4+(x0)+(x4))

/* MACROS FOR BORDERS */
/* | b I a | b | reflection used ("I" denotes the image boundary) */
/* LEFT/TOP */
/* 1/16[1    4    6    4    1]       */
/*    | x2 | x1 I x0 | x1 | x2 |...  */
#define  PD_LT(x0,x1,x2)                 ((x0)*6 + (x1)*8 + (x2)*2)

/* RIGHT/BOTTOM */
/* 1/16[1    4    6    4    1]       */
/* ...| x0 | x1 | x2 | x3 I x2 |     */
#define  PD_RB(x0,x1,x2,x3)              ((x0) + ((x1) + (x3))*4 + (x2)*7)

/* SINGULAR CASE ( width == 2 || height == 2 ) */
/* 1/16[1    4    6    4    1]       */
/*    | x0 | x1 I x0 | x1 I x0 |     */
#define  PD_SINGULAR(x0,x1)    (((x0) + (x1))*8)

#define  PD_SCALE_INT(x)       (((x) + (1<<7)) >> 8)
#define  PD_SCALE_FLT(x)       ((x)*0.00390625f)

#define  PD_SZ  5


/****************************************************************************************\
Up-sampling pyramids macros
\****************************************************************************************/

/////////// filtering macros //////////////

/* COMMON CASE: NON ZERO */
/* 1/16[1    4   6    4   1]       */
/* ...| x0 | 0 | x1 | 0 | x2 |...  */
#define  PU_FILTER( x0, x1, x2 )         ((x1)*6 + (x0) + (x2))

/* ZERO POINT AT CENTER */
/* 1/16[1   4    6   4    1]      */
/* ...| 0 | x0 | 0 | x1 | 0 |...  */
#define  PU_FILTER_ZI( x0, x1 )          (((x0) + (x1))*4)

/* MACROS FOR BORDERS */

/* | b I a | b | reflection */

/* LEFT/TOP */
/* 1/16[1    4   6    4   1]       */
/*    | x1 | 0 I x0 | 0 | x1 |...  */
#define  PU_LT( x0, x1 )                 ((x0)*6 + (x1)*2)

/* 1/16[1   4    6   4    1]       */
/*    | 0 I x0 | 0 | x1 | 0 |...   */
#define  PU_LT_ZI( x0, x1 )              PU_FILTER_ZI((x0),(x1))

/* RIGHT/BOTTOM: NON ZERO */
/* 1/16[1    4   6    4   1]       */
/* ...| x0 | 0 | x1 | 0 I x1 |     */
#define  PU_RB( x0, x1 )                 ((x0) + (x1)*7)

/* RIGHT/BOTTOM: ZERO POINT AT CENTER */
/* 1/16[1   4    6   4    1]       */
/* ...| 0 | x0 | 0 I x0 | 0 |      */
#define  PU_RB_ZI( x0 )                  ((x0)*8)

/* SINGULAR CASE */
/* 1/16[1    4   6    4   1]       */
/*    | x0 | 0 I x0 | 0 I x0 |     */
#define  PU_SINGULAR( x0 )               PU_RB_ZI((x0)) /* <--| the same formulas */
#define  PU_SINGULAR_ZI( x0 )            PU_RB_ZI((x0)) /* <--| */

/* x/64  - scaling in up-sampling functions */
#define  PU_SCALE_INT(x)                 (((x) + (1<<5)) >> 6)
#define  PU_SCALE_FLT(x)                 ((x)*0.015625f)

#define  PU_SZ  3

typedef int worktype;

/*******************************************************
only downsample 2:1, the destination is width/2*(height+1)/2;
********************************************************/
int PyrDownG5x5_U8CnR(  const imgpel* src, 
                      int srcstep, 
                      int width,        //width of source;
                      int height,       //height of source;
                      imgpel* dst,
                      int dststep,
                      int Cs 
                      ) 
{
  worktype *buf=NULL;
  worktype *buffer;  /* pointer to temporary buffer */
  worktype *rows[PD_SZ]; /* array of rows pointers. dim(rows) is PD_SZ */
  int  y, top_row = 0;
  int  Wd = width/2, Wdn = Wd*Cs;
  int  buffer_step = Wdn;
  int  pd_sz = (PD_SZ + 1)*buffer_step;
  int  fst = 0, lst = height <= PD_SZ/2 ? height : PD_SZ/2 + 1;

  assert( Cs == 1 || Cs == 3 );

  //alloc buffer;
  buf = malloc((width&-2)*2*Cs*sizeof(worktype)*(PD_SZ + 1)); //buf size is 2 times larger than required;
  buffer = buf;

  srcstep /= sizeof(src[0]);
  dststep /= sizeof(dst[0]);

  /* main loop */
  for(y = 0; y < height; y += 2, dst += dststep )
  {
    /* set first and last indices of buffer rows which are need to be filled */
    int x, y1, k = top_row;
    int x1 = buffer_step;
    worktype *row01, *row23, *row4;

    /* assign rows pointers */
    for( y1 = 0; y1 < PD_SZ; y1++)
    {
      rows[y1] = buffer + k;
      k += buffer_step;
      k &= k < pd_sz ? -1 : 0;
    }

    row01 = rows[0];
    row23 = rows[2];
    row4  = rows[4];

    /* fill new buffer rows with filtered source (horizontal conv) */
    if( Cs == 1 )
    {
      if( width > PD_SZ/2 )
      {
        for( y1 = fst; y1 < lst; y1++, src += srcstep )
        {
          worktype *row = rows[y1];

          /* process left & right bounds */
          row[0]    = PD_LT( src[0], src[1], src[2] );
          row[Wd-1] = PD_RB( src[Wd*2-4], src[Wd*2-3], src[Wd*2-2], src[Wd*2-1]);
          /* other points (even) */
          for( x = 1; x < Wd - 1; x++ )
          {
            row[x] = PD_FILTER( src[2*x-2], src[2*x-1], src[2*x], src[2*x+1], src[2*x+2] );
          }
        }
      }
      else
      {
        for( y1 = fst; y1 < lst; y1++, src += srcstep )
        {
          rows[y1][0] = PD_SINGULAR( src[0], src[1] );
        }
      }
    }
    else /* Cs == 3 */
    {
      for( y1 = fst; y1 < lst; y1++, src += srcstep )
      {
        worktype *row = rows[y1];
        if( width > PD_SZ/2 )
        {
          int c;
          for( c = 0; c < 3; c++ )
          {
            /* process left & right bounds  */
            row[c] = PD_LT( src[c], src[3+c], src[6+c] );
            row[Wdn-3+c] = PD_RB( src[Wdn*2-12+c], src[Wdn*2-9+c], src[Wdn*2-6+c], src[Wdn*2-3+c] );
          }
          /* other points (even) */
          for( x = 3; x < Wdn - 3; x += 3 )
          {
            row[x]   = PD_FILTER( src[2*x-6], src[2*x-3], src[2*x], src[2*x+3], src[2*x+6] );
            row[x+1] = PD_FILTER( src[2*x-5], src[2*x-2], src[2*x+1], src[2*x+4], src[2*x+7] );
            row[x+2] = PD_FILTER( src[2*x-4], src[2*x-1], src[2*x+2], src[2*x+5], src[2*x+8] );
          }
        }
        else /* size.width <= PD_SZ/2 */
        {
          row[0] = PD_SINGULAR( src[0], src[3] );
          row[1] = PD_SINGULAR( src[1], src[4] );
          row[2] = PD_SINGULAR( src[2], src[5] );
        }
      }
    }

    /* second pass. Do vertical conv and write results do destination image */
    if( y > 0 )
    {
      if( y < height - PD_SZ/2 )
      {
        for( x = 0; x < Wdn; x++, x1++ )
        {
          dst[x] = (imgpel)PD_SCALE_INT( PD_FILTER( row01[x],  row01[x1], row23[x], row23[x1], row4[x] ));
        }
        top_row += 2*buffer_step;
        top_row &= top_row < pd_sz ? -1 : 0;
      }
      else /* bottom */
        for( x = 0; x < Wdn; x++, x1++ )
          dst[x] = (imgpel)PD_SCALE_INT( PD_RB( row01[x], row01[x1], row23[x], row23[x1]));
    }
    else
    {
      if( height > PD_SZ/2 ) /* top */
      {
        for( x = 0; x < Wdn; x++, x1++ )
          dst[x] = (imgpel)PD_SCALE_INT( PD_LT( row01[x], row01[x1], row23[x] ));
      }
      else /* size.height <= PD_SZ/2 */
      {
        for( x = 0; x < Wdn; x++, x1++ )
          dst[x] = (imgpel)PD_SCALE_INT( PD_SINGULAR( row01[x], row01[x1] ));
      }
      fst = PD_SZ - 2;
    }

    lst = y + 2 + PD_SZ/2 < height ? PD_SZ : height - y;
  }

  if(buf)
  {
    free(buf);
    buf = NULL;
  }

  return 0;
}
