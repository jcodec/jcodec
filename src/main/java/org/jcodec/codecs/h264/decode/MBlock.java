package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.io.model.MBType;

public class MBlock {

    public int chromaPredictionMode;
    public int mbQPDelta;
    public int[] dc;
    public int[][][] ac;
    public boolean transform8x8Used;
    public int[] lumaModes;
    public int[] dc1;
    public int[] dc2;
    public int cbp;
    public int mbType;
    public MBType curMbType;
    
    public PB16x16 pb16x16 = new PB16x16();
    public PB168x168 pb168x168 = new PB168x168();
    public PB8x8 pb8x8 = new PB8x8();
    public IPCM ipcm = new IPCM();
    public int mbIdx;
    public boolean fieldDecoding;
    public MBType prevMbType;
    public int luma16x16Mode;
    
    public boolean skipped;

    public MBlock() {
        dc = new int[16];
        ac = new int[][][] { new int[16][64], new int[4][16], new int[4][16] };
        lumaModes = new int[16];
    }

    public int cbpLuma() {
        return cbp & 0xf;
    }

    public int cbpChroma() {
        return cbp >> 4;
    }

    public void cbp(int cbpLuma, int cbpChroma) {
        cbp = (cbpLuma & 0xf) | (cbpChroma << 4);
    }
    
    static class PB16x16 {
        public int[] refIdx;
        public int[] mvdX = new int[2];
        public int[] mvdY = new int[2];
    }
    
    static class PB168x168 {
        public int[] refIdx1;
        public int[] refIdx2;
        public int[] mvdX1 = new int[2];
        public int[] mvdY1 = new int[2];
        public int[] mvdX2 = new int[2];
        public int[] mvdY2 = new int[2];
    }
    
    static class PB8x8 {
        public int[][] refIdx = new int[2][4];
        public int[] subMbTypes = new int[4];
        public int[] mvdX1 = new int[4];
        public int[] mvdY1 = new int[4];
        public int[] mvdX2 = new int[4];
        public int[] mvdY2 = new int[4];
        public int[] mvdX3 = new int[4];
        public int[] mvdY3 = new int[4];
        public int[] mvdX4 = new int[4];
        public int[] mvdY4 = new int[4];
    }
    
    static class IPCM {

        public int[] samplesLuma;
        public int[] samplesChroma;
        
    }
}