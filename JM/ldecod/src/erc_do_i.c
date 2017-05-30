
/*!
 *************************************************************************************
 * \file
 *      erc_do_i.c
 *
 * \brief
 *      Intra (I) frame error concealment algorithms for decoder
 *
 *  \author
 *      - Ari Hourunranta              <ari.hourunranta@nokia.com>
 *      - Viktor Varsa                 <viktor.varsa@nokia.com>
 *      - Ye-Kui Wang                  <wyk@ieee.org>
 *
 *************************************************************************************
 */

#include "global.h"
#include "erc_do.h"

static void concealBlocks          ( VideoParameters *p_Vid, int lastColumn, int lastRow, int comp, frame *recfr, int picSizeX, char *condition );
static void pixMeanInterpolateBlock( VideoParameters *p_Vid, imgpel *src[], imgpel *block, int blockSize, int frameWidth );

/*!
 ************************************************************************
 * \brief
 *      The main function for Intra frame concealment.
 *      Calls "concealBlocks" for each color component (Y,U,V) separately
 * \return
 *      0, if the concealment was not successful and simple concealment should be used
 *      1, otherwise (even if none of the blocks were concealed)
 * \param p_Vid
 *      video encoding parameters for current picture
 * \param recfr
 *      Reconstructed frame buffer
 * \param picSizeX
 *      Width of the frame in pixels
 * \param picSizeY
 *      Height of the frame in pixels
 * \param errorVar
 *      Variables for error concealment
 ************************************************************************
 */
int ercConcealIntraFrame( VideoParameters *p_Vid, frame *recfr, int picSizeX, int picSizeY, ercVariables_t *errorVar )
{
  int lastColumn = 0, lastRow = 0;

  // if concealment is on
  if ( errorVar && errorVar->concealment )
  {
    // if there are segments to be concealed
    if ( errorVar->nOfCorruptedSegments )
    {
      // Y
      lastRow = (int) (picSizeY>>3);
      lastColumn = (int) (picSizeX>>3);
      concealBlocks( p_Vid, lastColumn, lastRow, 0, recfr, picSizeX, errorVar->yCondition );

      // U (dimensions halved compared to Y)
      lastRow = (int) (picSizeY>>4);
      lastColumn = (int) (picSizeX>>4);
      concealBlocks( p_Vid, lastColumn, lastRow, 1, recfr, picSizeX, errorVar->uCondition );

      // V ( dimensions equal to U )
      concealBlocks( p_Vid, lastColumn, lastRow, 2, recfr, picSizeX, errorVar->vCondition );
    }
    return 1;
  }
  else
    return 0;
}

/*!
 ************************************************************************
 * \brief
 *      Conceals the MB at position (row, column) using pixels from predBlocks[]
 *      using pixMeanInterpolateBlock()
 * \param p_Vid
 *      video encoding parameters for current picture
 * \param currFrame
 *      current frame
 * \param row
 *      y coordinate in blocks
 * \param column
 *      x coordinate in blocks
 * \param predBlocks[]
 *      list of neighboring source blocks (numbering 0 to 7, 1 means: use the neighbor)
 * \param frameWidth
 *      width of frame in pixels
 * \param mbWidthInBlocks
 *      2 for Y, 1 for U/V components
 ************************************************************************
 */
void ercPixConcealIMB(VideoParameters *p_Vid, imgpel *currFrame, int row, int column, int predBlocks[], int frameWidth, int mbWidthInBlocks)
{
   imgpel *src[8]={NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL};
   imgpel *currBlock = NULL;

   // collect the reliable neighboring blocks
   if (predBlocks[0])
      src[0] = currFrame + (row-mbWidthInBlocks)*frameWidth*8 + (column+mbWidthInBlocks)*8;
   if (predBlocks[1])
      src[1] = currFrame + (row-mbWidthInBlocks)*frameWidth*8 + (column-mbWidthInBlocks)*8;
   if (predBlocks[2])
      src[2] = currFrame + (row+mbWidthInBlocks)*frameWidth*8 + (column-mbWidthInBlocks)*8;
   if (predBlocks[3])
      src[3] = currFrame + (row+mbWidthInBlocks)*frameWidth*8 + (column+mbWidthInBlocks)*8;
   if (predBlocks[4])
      src[4] = currFrame + (row-mbWidthInBlocks)*frameWidth*8 + column*8;
   if (predBlocks[5])
      src[5] = currFrame + row*frameWidth*8 + (column-mbWidthInBlocks)*8;
   if (predBlocks[6])
      src[6] = currFrame + (row+mbWidthInBlocks)*frameWidth*8 + column*8;
   if (predBlocks[7])
      src[7] = currFrame + row*frameWidth*8 + (column+mbWidthInBlocks)*8;

   currBlock = currFrame + row*frameWidth*8 + column*8;
   pixMeanInterpolateBlock( p_Vid, src, currBlock, mbWidthInBlocks*8, frameWidth );
}

/*!
 ************************************************************************
 * \brief
 *      This function checks the neighbors of a Macroblock for usability in
 *      concealment. First the OK macroblocks are marked, and if there is not
 *      enough of them, then the CONCEALED ones as well.
 *      A "1" in the the output array means reliable, a "0" non reliable MB.
 *      The block order in "predBlocks":
 *              1 4 0
 *              5 x 7
 *              2 6 3
 *      i.e., corners first.
 * \return
 *      Number of useable neighbor macroblocks for concealment.
 * \param predBlocks[]
 *      Array for indicating the valid neighbor blocks
 * \param currRow
 *      Current block row in the frame
 * \param currColumn
 *      Current block column in the frame
 * \param condition
 *      The block condition (ok, lost) table
 * \param maxRow
 *      Number of block rows in the frame
 * \param maxColumn
 *      Number of block columns in the frame
 * \param step
 *      Number of blocks belonging to a MB, when counting
 *      in vertical/horizontal direction. (Y:2 U,V:1)
 * \param fNoCornerNeigh
 *      No corner neighbors are considered
 ************************************************************************
 */
int ercCollect8PredBlocks( int predBlocks[], int currRow, int currColumn, char *condition,
                           int maxRow, int maxColumn, int step, byte fNoCornerNeigh )
{
  int srcCounter  = 0;
  int srcCountMin = (fNoCornerNeigh ? 2 : 4);
  int threshold   = ERC_BLOCK_OK;

  memset( predBlocks, 0, 8*sizeof(int) );

  // collect the reliable neighboring blocks
  do
  {
    srcCounter = 0;
    // top
    if (currRow > 0 && condition[ (currRow-1)*maxColumn + currColumn ] >= threshold )
    {                           //ERC_BLOCK_OK (3) or ERC_BLOCK_CONCEALED (2)
      predBlocks[4] = condition[ (currRow-1)*maxColumn + currColumn ];
      srcCounter++;
    }
    // bottom
    if ( currRow < (maxRow-step) && condition[ (currRow+step)*maxColumn + currColumn ] >= threshold )
    {
      predBlocks[6] = condition[ (currRow+step)*maxColumn + currColumn ];
      srcCounter++;
    }

    if ( currColumn > 0 )
    {
      // left
      if ( condition[ currRow*maxColumn + currColumn - 1 ] >= threshold )
      {
        predBlocks[5] = condition[ currRow*maxColumn + currColumn - 1 ];
        srcCounter++;
      }

      if ( !fNoCornerNeigh )
      {
        // top-left
        if ( currRow > 0 && condition[ (currRow-1)*maxColumn + currColumn - 1 ] >= threshold )
        {
          predBlocks[1] = condition[ (currRow-1)*maxColumn + currColumn - 1 ];
          srcCounter++;
        }
        // bottom-left
        if ( currRow < (maxRow-step) && condition[ (currRow+step)*maxColumn + currColumn - 1 ] >= threshold )
        {
          predBlocks[2] = condition[ (currRow+step)*maxColumn + currColumn - 1 ];
          srcCounter++;
        }
      }
    }

    if ( currColumn < (maxColumn-step) )
    {
      // right
      if ( condition[ currRow*maxColumn+currColumn + step ] >= threshold )
      {
        predBlocks[7] = condition[ currRow*maxColumn+currColumn + step ];
        srcCounter++;
      }

      if ( !fNoCornerNeigh )
      {
        // top-right
        if ( currRow > 0 && condition[ (currRow-1)*maxColumn + currColumn + step ] >= threshold )
        {
          predBlocks[0] = condition[ (currRow-1)*maxColumn + currColumn + step ];
          srcCounter++;
        }
        // bottom-right
        if ( currRow < (maxRow-step) && condition[ (currRow+step)*maxColumn + currColumn + step ] >= threshold )
        {
          predBlocks[3] = condition[ (currRow+step)*maxColumn + currColumn + step ];
          srcCounter++;
        }
      }
    }
    // prepare for the next round
    threshold--;
    if (threshold < ERC_BLOCK_CONCEALED)
      break;
  } while ( srcCounter < srcCountMin);

  return srcCounter;
}

/*!
 ************************************************************************
 * \brief
 *      collects prediction blocks only from the current column
 * \return
 *      Number of usable neighbour Macroblocks for concealment.
 * \param predBlocks[]
 *      Array for indicating the valid neighbor blocks
 * \param currRow
 *      Current block row in the frame
 * \param currColumn
 *      Current block column in the frame
 * \param condition
 *      The block condition (ok, lost) table
 * \param maxRow
 *      Number of block rows in the frame
 * \param maxColumn
 *      Number of block columns in the frame
 * \param step
 *      Number of blocks belonging to a MB, when counting
 *      in vertical/horizontal direction. (Y:2 U,V:1)
 ************************************************************************
 */
int ercCollectColumnBlocks( int predBlocks[], int currRow, int currColumn, char *condition, int maxRow, int maxColumn, int step )
{
  int srcCounter = 0, threshold = ERC_BLOCK_CORRUPTED;

  memset( predBlocks, 0, 8*sizeof(int) );

  // in this case, row > 0 and row < 17
  if ( condition[ (currRow-1)*maxColumn + currColumn ] > threshold )
  {
    predBlocks[4] = 1;
    srcCounter++;
  }
  if ( condition[ (currRow+step)*maxColumn + currColumn ] > threshold )
  {
    predBlocks[6] = 1;
    srcCounter++;
  }

  return srcCounter;
}

/*!
 ************************************************************************
 * \brief
 *      Core for the Intra blocks concealment.
 *      It is called for each color component (Y,U,V) separately
 *      Finds the corrupted blocks and calls pixel interpolation functions
 *      to correct them, one block at a time.
 *      Scanning is done vertically and each corrupted column is corrected
 *      bi-directionally, i.e., first block, last block, first block+1, last block -1 ...
 * \param p_Vid
 *      video encoding parameters for current picture
 * \param lastColumn
 *      Number of block columns in the frame
 * \param lastRow
 *      Number of block rows in the frame
 * \param comp
 *      color component
 * \param recfr
 *      Reconstructed frame buffer
 * \param picSizeX
 *      Width of the frame in pixels
 * \param condition
 *      The block condition (ok, lost) table
 ************************************************************************
 */
static void concealBlocks( VideoParameters *p_Vid, int lastColumn, int lastRow, int comp, frame *recfr, int picSizeX, char *condition )
{
  int row, column, srcCounter = 0,  thr = ERC_BLOCK_CORRUPTED,
      lastCorruptedRow = -1, firstCorruptedRow = -1, currRow = 0,
      areaHeight = 0, i = 0, smoothColumn = 0;
  int predBlocks[8], step = 1;

  // in the Y component do the concealment MB-wise (not block-wise):
  // this is useful if only whole MBs can be damaged or lost
  if ( comp == 0 )
    step = 2;
  else
    step = 1;

  for ( column = 0; column < lastColumn; column += step )
  {
    for ( row = 0; row < lastRow; row += step )
    {
      if ( condition[row*lastColumn+column] <= thr )
      {
        firstCorruptedRow = row;
        // find the last row which has corrupted blocks (in same continuous area)
        for ( lastCorruptedRow = row+step; lastCorruptedRow < lastRow; lastCorruptedRow += step )
        {
          // check blocks in the current column
          if ( condition[ lastCorruptedRow*lastColumn + column ] > thr )
          {
            // current one is already OK, so the last was the previous one
            lastCorruptedRow -= step;
            break;
          }
        }
        if ( lastCorruptedRow >= lastRow )
        {
          // correct only from above
          lastCorruptedRow = lastRow-step;
          for ( currRow = firstCorruptedRow; currRow < lastRow; currRow += step )
          {
            srcCounter = ercCollect8PredBlocks( predBlocks, currRow, column, condition, lastRow, lastColumn, step, 1 );

            switch( comp )
            {
            case 0 :
              ercPixConcealIMB( p_Vid, recfr->yptr, currRow, column, predBlocks, picSizeX, 2 );
              break;
            case 1 :
              ercPixConcealIMB( p_Vid, recfr->uptr, currRow, column, predBlocks, (picSizeX>>1), 1 );
              break;
            case 2 :
              ercPixConcealIMB( p_Vid, recfr->vptr, currRow, column, predBlocks, (picSizeX>>1), 1 );
              break;
            }

            if ( comp == 0 )
            {
              condition[ currRow*lastColumn+column] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + 1] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + lastColumn] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + lastColumn + 1] = ERC_BLOCK_CONCEALED;
            }
            else
            {
              condition[ currRow*lastColumn+column] = ERC_BLOCK_CONCEALED;
            }

          }
          row = lastRow;
        }
        else if ( firstCorruptedRow == 0 )
        {
          // correct only from below
          for ( currRow = lastCorruptedRow; currRow >= 0; currRow -= step )
          {
            srcCounter = ercCollect8PredBlocks( predBlocks, currRow, column, condition, lastRow, lastColumn, step, 1 );

            switch( comp )
            {
            case 0 :
              ercPixConcealIMB( p_Vid, recfr->yptr, currRow, column, predBlocks, picSizeX, 2 );
              break;
            case 1 :
              ercPixConcealIMB( p_Vid, recfr->uptr, currRow, column, predBlocks, (picSizeX>>1), 1 );
              break;
            case 2 :
              ercPixConcealIMB( p_Vid, recfr->vptr, currRow, column, predBlocks, (picSizeX>>1), 1 );
              break;
            }

            if ( comp == 0 )
            {
              condition[ currRow*lastColumn+column] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + 1] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + lastColumn] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + lastColumn + 1] = ERC_BLOCK_CONCEALED;
            }
            else
            {
              condition[ currRow*lastColumn+column] = ERC_BLOCK_CONCEALED;
            }

          }

          row = lastCorruptedRow+step;
        }
        else
        {
          // correct bi-directionally

          row = lastCorruptedRow+step;
          areaHeight = lastCorruptedRow-firstCorruptedRow+step;

          // Conceal the corrupted area switching between the up and the bottom rows
          for ( i = 0; i < areaHeight; i += step )
          {
            if ( i % 2 )
            {
              currRow = lastCorruptedRow;
              lastCorruptedRow -= step;
            }
            else
            {
              currRow = firstCorruptedRow;
              firstCorruptedRow += step;
            }

            if (smoothColumn > 0)
            {
              srcCounter = ercCollectColumnBlocks( predBlocks, currRow, column, condition, lastRow, lastColumn, step );
            }
            else
            {
              srcCounter = ercCollect8PredBlocks( predBlocks, currRow, column, condition, lastRow, lastColumn, step, 1 );
            }

            switch( comp )
            {
            case 0 :
              ercPixConcealIMB( p_Vid, recfr->yptr, currRow, column, predBlocks, picSizeX, 2 );
              break;

            case 1 :
              ercPixConcealIMB( p_Vid, recfr->uptr, currRow, column, predBlocks, (picSizeX>>1), 1 );
              break;

            case 2 :
              ercPixConcealIMB( p_Vid, recfr->vptr, currRow, column, predBlocks, (picSizeX>>1), 1 );
              break;
            }

            if ( comp == 0 )
            {
              condition[ currRow*lastColumn+column] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + 1] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + lastColumn] = ERC_BLOCK_CONCEALED;
              condition[ currRow*lastColumn+column + lastColumn + 1] = ERC_BLOCK_CONCEALED;
            }
            else
            {
              condition[ currRow*lastColumn+column ] = ERC_BLOCK_CONCEALED;
            }
          }
        }

        lastCorruptedRow = -1;
        firstCorruptedRow = -1;

      }
    }
  }
}

/*!
 ************************************************************************
 * \brief
 *      Does the actual pixel based interpolation for block[]
 *      using weighted average
 * \param p_Vid
 *      video encoding parameters for current picture
 * \param src[]
 *      pointers to neighboring source blocks
 * \param block
 *      destination block
 * \param blockSize
 *      16 for Y, 8 for U/V components
 * \param frameWidth
 *      Width of the frame in pixels
 ************************************************************************
 */
static void pixMeanInterpolateBlock( VideoParameters *p_Vid, imgpel *src[], imgpel *block, int blockSize, int frameWidth )
{
  int row, column, k, tmp, srcCounter = 0, weight = 0, bmax = blockSize - 1;

  k = 0;
  for ( row = 0; row < blockSize; row++ )
  {
    for ( column = 0; column < blockSize; column++ )
    {
      tmp = 0;
      srcCounter = 0;
      // above
      if ( src[4] != NULL )
      {
        weight = blockSize-row;
        tmp += weight * (*(src[4]+bmax*frameWidth+column));
        srcCounter += weight;
      }
      // left
      if ( src[5] != NULL )
      {
        weight = blockSize-column;
        tmp += weight * (*(src[5]+row*frameWidth+bmax));
        srcCounter += weight;
      }
      // below
      if ( src[6] != NULL )
      {
        weight = row+1;
        tmp += weight * (*(src[6]+column));
        srcCounter += weight;
      }
      // right
      if ( src[7] != NULL )
      {
        weight = column+1;
        tmp += weight * (*(src[7]+row*frameWidth));
        srcCounter += weight;
      }

      if ( srcCounter > 0 )
        block[ k + column ] = (imgpel)(tmp/srcCounter);
      else
        block[ k + column ] = (imgpel) (blockSize == 8 ? p_Vid->dc_pred_value_comp[1] : p_Vid->dc_pred_value_comp[0]);
    }
    k += frameWidth;
  }
}
