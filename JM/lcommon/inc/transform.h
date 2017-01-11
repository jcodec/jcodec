
/*!
 ***************************************************************************
 *
 * \file transform.h
 *
 * \brief
 *    prototypes of transform functions
 *
 * \date
 *    10 July 2007
 *
 * \author
 *    Main contributors (see contributors.h for copyright, address and affiliation details)
 *    Alexis Michael Tourapis
 **************************************************************************/

#ifndef _TRANSFORM_H_
#define _TRANSFORM_H_

extern void forward4x4   (int **block , int **tblock, int pos_y, int pos_x);
extern void inverse4x4   (int **tblock, int **block , int pos_y, int pos_x);
extern void forward8x8   (int **block , int **tblock, int pos_y, int pos_x);
extern void inverse8x8   (int **tblock, int **block , int pos_x);
extern void hadamard4x4  (int **block , int **tblock);
extern void ihadamard4x4 (int **tblock, int **block);
extern void hadamard4x2  (int **block , int **tblock);
extern void ihadamard4x2 (int **tblock, int **block);
extern void hadamard2x2  (int **block , int tblock[4]);
extern void ihadamard2x2 (int block[4], int tblock[4]);

#endif //_TRANSFORM_H_
