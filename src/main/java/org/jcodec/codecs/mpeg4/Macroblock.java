package org.jcodec.codecs.mpeg4;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Macroblock {
    public final static int MBPRED_SIZE = 15;

    public static Vector vec() {
        return new Vector(0,0);
    }

    public static class Vector {
        public Vector(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public int x;
        public int y;
    }

    public Vector[] mvs;

    public short[][] predValues = new short[6][MBPRED_SIZE];
    public int[] acpredDirections = new int[6];

    public int mode;
    public int quant;

    public boolean fieldDCT;
    public boolean fieldPred;
    public boolean fieldForTop;
    public boolean fieldForBottom;

    private Vector[] pmvs;
    private Vector[] qmvs;

    public int cbp;

    public Vector[] bmvs;
    public Vector[] bqmvs;

    public Vector amv = vec();

    public Vector mvsAvg;

    public int x;

    public int y;

    public int bound;

    public boolean acpredFlag;

    public short[] predictors = new short[8];
    public short[][] block = new short[6][64];

    public boolean coded;

    public boolean mcsel;

    public byte[][] pred;

    public Macroblock() {
        mvs = new Vector[4];
        pmvs = new Vector[4];
        qmvs = new Vector[4];
        bmvs = new Vector[4];
        bqmvs = new Vector[4];

        for (int i = 0; i < 4; i++) {
            mvs[i] = vec();
            pmvs[i] = vec();
            qmvs[i] = vec();
            bmvs[i] = vec();
            bqmvs[i] = vec();
        }
        pred = new byte[][] { new byte[256], new byte[64], new byte[64], new byte[256], new byte[64], new byte[64] };
    }

    public void reset(int x2, int y2, int bound2) {
        this.x = x2;
        this.y = y2;
        this.bound = bound2;

    }
}