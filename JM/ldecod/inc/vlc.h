
/*!
 ************************************************************************
 * \file vlc.h
 *
 * \brief
 *    header for (CA)VLC coding functions
 *
 * \author
 *    Karsten Suehring
 *
 ************************************************************************
 */

#ifndef _VLC_H_
#define _VLC_H_

//! gives CBP value from codeword number, both for intra and inter
static const byte NCBP[2][48][2]=
{
  {  // 0      1        2       3       4       5       6       7       8       9      10      11
    {15, 0},{ 0, 1},{ 7, 2},{11, 4},{13, 8},{14, 3},{ 3, 5},{ 5,10},{10,12},{12,15},{ 1, 7},{ 2,11},
    { 4,13},{ 8,14},{ 6, 6},{ 9, 9},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},
    { 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},
    { 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0},{ 0, 0}
  },
  {
    {47, 0},{31,16},{15, 1},{ 0, 2},{23, 4},{27, 8},{29,32},{30, 3},{ 7, 5},{11,10},{13,12},{14,15},
    {39,47},{43, 7},{45,11},{46,13},{16,14},{ 3, 6},{ 5, 9},{10,31},{12,35},{19,37},{21,42},{26,44},
    {28,33},{35,34},{37,36},{42,40},{44,39},{ 1,43},{ 2,45},{ 4,46},{ 8,17},{17,18},{18,20},{20,24},
    {24,19},{ 6,21},{ 9,26},{22,28},{25,23},{32,27},{33,29},{34,30},{36,22},{40,25},{38,38},{41,41}
  }
};

//! for the linfo_levrun_inter routine
static const byte NTAB1[4][8][2] =
{
  {{1,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}},
  {{1,1},{1,2},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}},
  {{2,0},{1,3},{1,4},{1,5},{0,0},{0,0},{0,0},{0,0}},
  {{3,0},{2,1},{2,2},{1,6},{1,7},{1,8},{1,9},{4,0}},
};

static const byte LEVRUN1[16]=
{
  4,2,2,1,1,1,1,1,1,1,0,0,0,0,0,0,
};


static const byte NTAB2[4][8][2] =
{
  {{1,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}},
  {{1,1},{2,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}},
  {{1,2},{3,0},{4,0},{5,0},{0,0},{0,0},{0,0},{0,0}},
  {{1,3},{1,4},{2,1},{3,1},{6,0},{7,0},{8,0},{9,0}},
};

//! for the linfo_levrun__c2x2 routine
static const byte LEVRUN3[4] =
{
  2,1,0,0
};

static const byte NTAB3[2][2][2] =
{
  {{1,0},{0,0}},
  {{2,0},{1,1}},
};

extern int read_se_v (char *tracestring, Bitstream *bitstream, int *used_bits);
extern int read_ue_v (char *tracestring, Bitstream *bitstream, int *used_bits);
extern Boolean read_u_1 (char *tracestring, Bitstream *bitstream, int *used_bits);
extern int read_u_v (int LenInBits, char *tracestring, Bitstream *bitstream, int *used_bits);
extern int read_i_v (int LenInBits, char *tracestring, Bitstream *bitstream, int *used_bits);

// CAVLC mapping
extern void linfo_ue(int len, int info, int *value1, int *dummy);
extern void linfo_se(int len, int info, int *value1, int *dummy);

extern void linfo_cbp_intra_normal(int len,int info,int *cbp, int *dummy);
extern void linfo_cbp_inter_normal(int len,int info,int *cbp, int *dummy);
extern void linfo_cbp_intra_other(int len,int info,int *cbp, int *dummy);
extern void linfo_cbp_inter_other(int len,int info,int *cbp, int *dummy);

extern void linfo_levrun_inter(int len,int info,int *level,int *irun);
extern void linfo_levrun_c2x2(int len,int info,int *level,int *irun);

extern int  uvlc_startcode_follows(Slice *currSlice, int dummy);

extern int  readSyntaxElement_VLC (SyntaxElement *sym, Bitstream *currStream);
extern int  readSyntaxElement_UVLC(Macroblock *currMB, SyntaxElement *sym, struct datapartition_dec *dp);
extern int  readSyntaxElement_Intra4x4PredictionMode(SyntaxElement *sym, Bitstream   *currStream);

extern int  GetVLCSymbol (byte buffer[],int totbitoffset,int *info, int bytecount);
extern int  GetVLCSymbol_IntraMode (byte buffer[],int totbitoffset,int *info, int bytecount);

extern int readSyntaxElement_FLC                         (SyntaxElement *sym, Bitstream *currStream);
extern int readSyntaxElement_NumCoeffTrailingOnes        (SyntaxElement *sym,  Bitstream *currStream, char *type);
extern int readSyntaxElement_NumCoeffTrailingOnesChromaDC(VideoParameters *p_Vid, SyntaxElement *sym, Bitstream *currStream);
extern int readSyntaxElement_Level_VLC0                  (SyntaxElement *sym, Bitstream *currStream);
extern int readSyntaxElement_Level_VLCN                  (SyntaxElement *sym, int vlc, Bitstream *currStream);
extern int readSyntaxElement_TotalZeros                  (SyntaxElement *sym, Bitstream *currStream);
extern int readSyntaxElement_TotalZerosChromaDC          (VideoParameters *p_Vid, SyntaxElement *sym, Bitstream *currStream);
extern int readSyntaxElement_Run                         (SyntaxElement *sym, Bitstream *currStream);
extern int GetBits  (byte buffer[],int totbitoffset,int *info, int bitcount, int numbits);
extern int ShowBits (byte buffer[],int totbitoffset,int bitcount, int numbits);

extern int more_rbsp_data (byte buffer[],int totbitoffset,int bytecount);


#endif

