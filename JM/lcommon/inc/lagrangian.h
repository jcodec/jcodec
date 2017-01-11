/*!
 **************************************************************************
 *  \file lagrangian.h
 *  \brief  
 *     Distortion data header file
 *  \date 2.23.2009,
 *
 *  \author 
 *   Alexis Michael Tourapis        <alexismt@ieee.org>
 *
 **************************************************************************
 */

#ifndef _LAGRANGIAN_H_
#define _LAGRANGIAN_H_

typedef struct lambda_params
{
  double md;     //!< Mode decision Lambda
  double me[3];  //!< Motion Estimation Lambda
  int    mf[3];  //!< Integer formatted Motion Estimation Lambda
} LambdaParams;

#endif

