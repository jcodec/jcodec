package org.jcodec.codecs.h264.decode;

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

}
