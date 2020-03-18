package org.jcodec.codecs.h264.encode;

import static java.lang.System.arraycopy;

import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.MBType;

public class EncodingContext {
    public CAVLC[] cavlc;
    public byte[][] leftRow;
    public byte[][] topLine;
    public byte[] topLeft;
    public int[] mvTopX;
    public int[] mvTopY;
    public int[] mvTopR;
    public int[] mvLeftX;
    public int[] mvLeftY;
    public int[] mvLeftR;
    public int mvTopLeftX;
    public int mvTopLeftY;
    public int mvTopLeftR;
    public int mbHeight;
    public int mbWidth;
    public int prevQp;
    
    public int[] i4x4PredTop;
    public int[] i4x4PredLeft;
    public MBType leftMBType;
    public MBType[] topMBType;

    public EncodingContext(int mbWidth, int mbHeight) {
        this.mbWidth = mbWidth;
        this.mbHeight = mbHeight;
        leftRow = new byte[][] { new byte[16], new byte[8], new byte[8] };
        topLine = new byte[][] { new byte[mbWidth << 4], new byte[mbWidth << 3], new byte[mbWidth << 3] };
        topLeft = new byte[4];

        mvTopX = new int[mbWidth << 2];
        mvTopY = new int[mbWidth << 2];
        mvTopR = new int[mbWidth << 2];
        mvLeftX = new int[4];
        mvLeftY = new int[4];
        mvLeftR = new int[4];
        i4x4PredTop = new int[mbWidth << 2];
        i4x4PredLeft = new int[4];
        topMBType = new MBType[mbWidth];
    }

    public void update(EncodedMB mb) {
        if (mb.getType() != MBType.I_NxN) {
            topLeft[0] = topLine[0][(mb.mbX << 4) + 15];
            arraycopy(mb.pixels.getPlaneData(0), 240, topLine[0], mb.mbX << 4, 16);
            copyCol(mb.pixels.getPlaneData(0), 15, 16, leftRow[0]);
        }
        topLeft[1] = topLine[1][(mb.mbX << 3) + 7];
        topLeft[2] = topLine[2][(mb.mbX << 3) + 7];
        arraycopy(mb.pixels.getPlaneData(1), 56, topLine[1], mb.mbX << 3, 8);
        arraycopy(mb.pixels.getPlaneData(2), 56, topLine[2], mb.mbX << 3, 8);

        copyCol(mb.pixels.getPlaneData(1), 7, 8, leftRow[1]);
        copyCol(mb.pixels.getPlaneData(2), 7, 8, leftRow[2]);

        mvTopLeftX = mvTopX[mb.mbX << 2];
        mvTopLeftY = mvTopY[mb.mbX << 2];
        mvTopLeftR = mvTopR[mb.mbX << 2];
        for (int i = 0; i < 4; i++) {
            mvTopX[(mb.mbX << 2) + i] = mb.mx[12 + i];
            mvTopY[(mb.mbX << 2) + i] = mb.my[12 + i];
            mvTopR[(mb.mbX << 2) + i] = mb.mr[12 + i];
            mvLeftX[i] = mb.mx[(i << 2)];
            mvLeftY[i] = mb.my[(i << 2)];
            mvLeftR[i] = mb.mr[(i << 2)];
        }
        topMBType[mb.mbX] = leftMBType = mb.getType();
    }

    private void copyCol(byte[] planeData, int off, int stride, byte[] out) {
        for (int i = 0; i < out.length; i++) {
            out[i] = planeData[off];
            off += stride;
        }
    }

    public EncodingContext fork() {
        EncodingContext ret = new EncodingContext(mbWidth, mbHeight);
        ret.cavlc = new CAVLC[3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(leftRow[i], 0, ret.leftRow[i], 0, leftRow[i].length);
            System.arraycopy(topLine[i], 0, ret.topLine[i], 0, topLine[i].length);
            ret.topLeft[i] = topLeft[i];
            ret.cavlc[i] = cavlc[i].fork();
        }
        System.arraycopy(mvTopX, 0, ret.mvTopX, 0, ret.mvTopX.length);
        System.arraycopy(mvTopY, 0, ret.mvTopY, 0, ret.mvTopY.length);
        System.arraycopy(mvTopR, 0, ret.mvTopR, 0, ret.mvTopR.length);
        System.arraycopy(mvLeftX, 0, ret.mvLeftX, 0, ret.mvLeftX.length);
        System.arraycopy(mvLeftY, 0, ret.mvLeftY, 0, ret.mvLeftY.length);
        System.arraycopy(mvLeftR, 0, ret.mvLeftR, 0, ret.mvLeftR.length);
        ret.mvTopLeftX = mvTopLeftX;
        ret.mvTopLeftY = mvTopLeftY;
        ret.mvTopLeftR = mvTopLeftR;
        ret.prevQp = prevQp;
        
        for (int i = 0; i < mbWidth; i++)
            ret.topMBType[i] = topMBType[i];
        ret.leftMBType = leftMBType;
        
        System.arraycopy(i4x4PredTop, 0, ret.i4x4PredTop, 0, mbWidth << 2);
        System.arraycopy(i4x4PredLeft, 0, ret.i4x4PredLeft, 0, 4);
        return ret;
    }
}
