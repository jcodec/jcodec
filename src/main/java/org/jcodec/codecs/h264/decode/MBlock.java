package org.jcodec.codecs.h264.decode;
import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.common.model.ColorSpace;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A codec macroblock
 * 
 * @author The JCodec project
 * 
 */
public class MBlock {

    public int chromaPredictionMode;
    public int mbQPDelta;
    public int[] dc;
    public int[][][] ac;
    public boolean transform8x8Used;
    public int[] lumaModes;
    public int[] dc1;
    public int[] dc2;
    public int _cbp;
    public int mbType;
    public MBType curMbType;
    public PB16x16 pb16x16;
    public PB168x168 pb168x168;
    public PB8x8 pb8x8;
    public IPCM ipcm;
    public int mbIdx;
    public boolean fieldDecoding;
    public MBType prevMbType;
    public int luma16x16Mode;
    public H264Utils.MvList x;
    public PartPred[] partPreds;

    public boolean skipped;
    // Number of coefficients in AC blocks, stored in 8x8 encoding order: 0 1 4 5 2 3 6 7 8 9 12 13 10 11 14 15
    public int[] nCoeff;

    public MBlock(ColorSpace chromaFormat) {
        this.pb8x8 = new PB8x8();
        this.pb16x16 = new PB16x16();
        this.pb168x168 = new PB168x168();

        dc = new int[16];
        ac = new int[][][] { new int[16][64], new int[4][16], new int[4][16] };
        lumaModes = new int[16];
        nCoeff = new int[16];
        dc1 = new int[(16 >> chromaFormat.compWidth[1]) >> chromaFormat.compHeight[1]];
        dc2 = new int[(16 >> chromaFormat.compWidth[2]) >> chromaFormat.compHeight[2]];
        ipcm = new IPCM(chromaFormat);
        x = new H264Utils.MvList(16);
        partPreds = new PartPred[4];
    }

    public int cbpLuma() {
        return _cbp & 0xf;
    }

    public int cbpChroma() {
        return _cbp >> 4;
    }

    public void cbp(int cbpLuma, int cbpChroma) {
        _cbp = (cbpLuma & 0xf) | (cbpChroma << 4);
    }

    static class PB16x16 {
        public int[] refIdx;
        public int[] mvdX;
        public int[] mvdY;

        public PB16x16() {
            this.refIdx = new int[2];
            this.mvdX = new int[2];
            this.mvdY = new int[2];
        }

        public void clean() {
            refIdx[0] = refIdx[1] = 0;
            mvdX[0] = mvdX[1] = 0;
            mvdY[0] = mvdY[1] = 0;
        }
    }

    static class PB168x168 {
        public int[] refIdx1;
        public int[] refIdx2;
        public int[] mvdX1;
        public int[] mvdY1;
        public int[] mvdX2;
        public int[] mvdY2;
        
        public PB168x168() {
            this.refIdx1 = new int[2];
            this.refIdx2 = new int[2];
            this.mvdX1 = new int[2];
            this.mvdY1 = new int[2];
            this.mvdX2 = new int[2];
            this.mvdY2 = new int[2];
        }
        
        public void clean() {
            refIdx1[0] = refIdx1[1] = 0;
            refIdx2[0] = refIdx2[1] = 0;

            mvdX1[0] = mvdX1[1] = 0;
            mvdY1[0] = mvdY1[1] = 0;
            mvdX2[0] = mvdX2[1] = 0;
            mvdY2[0] = mvdY2[1] = 0;
        }
    }

    static class PB8x8 {
        public int[][] refIdx;
        public int[] subMbTypes;
        public int[][] mvdX1;
        public int[][] mvdY1;
        public int[][] mvdX2;
        public int[][] mvdY2;
        public int[][] mvdX3;
        public int[][] mvdY3;
        public int[][] mvdX4;
        public int[][] mvdY4;
        
        public PB8x8() {
            this.refIdx = new int[2][4];
            this.subMbTypes = new int[4];
            this.mvdX1 = new int[2][4];
            this.mvdY1 = new int[2][4];
            this.mvdX2 = new int[2][4];
            this.mvdY2 = new int[2][4];
            this.mvdX3 = new int[2][4];
            this.mvdY3 = new int[2][4];
            this.mvdX4 = new int[2][4];
            this.mvdY4 = new int[2][4];
        }

        public void clean() {
            mvdX1[0][0] = mvdX1[0][1] = mvdX1[0][2] = mvdX1[0][3] = 0;
            mvdX2[0][0] = mvdX2[0][1] = mvdX2[0][2] = mvdX2[0][3] = 0;
            mvdX3[0][0] = mvdX3[0][1] = mvdX3[0][2] = mvdX3[0][3] = 0;
            mvdX4[0][0] = mvdX4[0][1] = mvdX4[0][2] = mvdX4[0][3] = 0;

            mvdY1[0][0] = mvdY1[0][1] = mvdY1[0][2] = mvdY1[0][3] = 0;
            mvdY2[0][0] = mvdY2[0][1] = mvdY2[0][2] = mvdY2[0][3] = 0;
            mvdY3[0][0] = mvdY3[0][1] = mvdY3[0][2] = mvdY3[0][3] = 0;
            mvdY4[0][0] = mvdY4[0][1] = mvdY4[0][2] = mvdY4[0][3] = 0;

            mvdX1[1][0] = mvdX1[1][1] = mvdX1[1][2] = mvdX1[1][3] = 0;
            mvdX2[1][0] = mvdX2[1][1] = mvdX2[1][2] = mvdX2[1][3] = 0;
            mvdX3[1][0] = mvdX3[1][1] = mvdX3[1][2] = mvdX3[1][3] = 0;
            mvdX4[1][0] = mvdX4[1][1] = mvdX4[1][2] = mvdX4[1][3] = 0;

            mvdY1[1][0] = mvdY1[1][1] = mvdY1[1][2] = mvdY1[1][3] = 0;
            mvdY2[1][0] = mvdY2[1][1] = mvdY2[1][2] = mvdY2[1][3] = 0;
            mvdY3[1][0] = mvdY3[1][1] = mvdY3[1][2] = mvdY3[1][3] = 0;
            mvdY4[1][0] = mvdY4[1][1] = mvdY4[1][2] = mvdY4[1][3] = 0;

            subMbTypes[0] = subMbTypes[1] = subMbTypes[2] = subMbTypes[3] = 0;
            refIdx[0][0] = refIdx[0][1] = refIdx[0][2] = refIdx[0][3] = 0;
            refIdx[1][0] = refIdx[1][1] = refIdx[1][2] = refIdx[1][3] = 0;
        }
    }

    static class IPCM {

        public int[] samplesLuma;
        public int[] samplesChroma;

        public IPCM(ColorSpace chromaFormat) {
            this.samplesLuma = new int[256];
            int MbWidthC = 16 >> chromaFormat.compWidth[1];
            int MbHeightC = 16 >> chromaFormat.compHeight[1];

            samplesChroma = new int[2 * MbWidthC * MbHeightC];
        }

        public void clean() {
            Arrays.fill(samplesLuma, 0);
            Arrays.fill(samplesChroma, 0);
        }
    }

    public void clear() {
        chromaPredictionMode = 0;
        mbQPDelta = 0;
        Arrays.fill(dc, 0);
        for (int i = 0; i < ac.length; i++)
            for (int j = 0; j < ac[i].length; j++)
                for (int k = 0; k < ac[i][j].length; k++)
                    ac[i][j][k] = 0;
        transform8x8Used = false;
        Arrays.fill(lumaModes, 0);
        Arrays.fill(dc1, 0);
        Arrays.fill(dc2, 0);
        Arrays.fill(nCoeff, 0);
        _cbp = 0;
        mbType = 0;
        pb16x16.clean();
        pb168x168.clean();
        pb8x8.clean();
        if (curMbType == MBType.I_PCM)
            ipcm.clean();
        mbIdx = 0;
        fieldDecoding = false;
        prevMbType = null;
        luma16x16Mode = 0;
        skipped = false;
        curMbType = null;
        x.clear();
        partPreds[0] = partPreds[1] = partPreds[2] = partPreds[3] = null; 
    }
}