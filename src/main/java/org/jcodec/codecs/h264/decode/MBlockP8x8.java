package org.jcodec.codecs.h264.decode;

public class MBlockP8x8 extends MBlock {

    public int[][] refIdx;
    public int[] subMbTypes;
    public int[] mvdX1;
    public int[] mvdY1;
    public int[] mvdX2;
    public int[] mvdY2;
    public int[] mvdX3;
    public int[] mvdY3;
    public int[] mvdX4;
    public int[] mvdY4;

    public MBlockP8x8() {
        refIdx = new int[2][4];
        subMbTypes = new int[4];
        mvdX1 = new int[4];
        mvdY1 = new int[4];
        mvdX2 = new int[4];
        mvdY2 = new int[4];
        mvdX3 = new int[4];
        mvdY3 = new int[4];
        mvdX4 = new int[4];
        mvdY4 = new int[4];
    }
}
