package org.jcodec.codecs.h264;

import java.util.Arrays;

import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class DecodedMBlock {
    private Picture8Bit pixels;

    // Deblocker related fields
    private boolean deblockTop;
    private boolean deblockLeft;
    private MBType type;
    private int[] qp;
    private int[] nc;

    private int[] mxL0;
    private int[] myL0;
    private int[] refIdxL0;
    private int[] refPOCL0;
    private boolean[] refShortTermL0;

    private int[] mxL1;
    private int[] myL1;
    private int[] refIdxL1;
    private int[] refPOCL1;
    private boolean[] refShortTermL1;

    private boolean transform8x8Used;
    private int alphaC0Offset;
    private int betaOffset;

    public DecodedMBlock() {
        pixels = Picture8Bit.create(16, 16, ColorSpace.YUV420J);
        qp = new int[3];
        nc = new int[16];
        mxL0 = new int[16];
        myL0 = new int[16];
        refIdxL0 = new int[16];
        refPOCL0 = new int[16];
        refShortTermL0 = new boolean[16];
        Arrays.fill(refIdxL0, Frame.MVS.POC_NO_REFERENCE);
        Arrays.fill(refPOCL0, Frame.MVS.POC_NO_REFERENCE);
        Arrays.fill(refShortTermL0, true);

        mxL1 = new int[16];
        myL1 = new int[16];
        refIdxL1 = new int[16];
        refPOCL1 = new int[16];
        refShortTermL1 = new boolean[16];
        Arrays.fill(refIdxL1, Frame.MVS.POC_NO_REFERENCE);
        Arrays.fill(refPOCL1, Frame.MVS.POC_NO_REFERENCE);
        Arrays.fill(refShortTermL1, true);
    }

    public Picture8Bit getPixels() {
        return pixels;
    }

    public MBType getType() {
        return type;
    }

    public void setType(MBType type) {
        this.type = type;
    }

    public int getQp(int comp) {
        return qp[comp];
    }

    public void setQp(int comp, int qp) {
        this.qp[comp] = qp;
    }

    public int[] getNc() {
        return nc;
    }

    public int[] getMxL0() {
        return mxL0;
    }

    public int[] getMyL0() {
        return myL0;
    }

    public int getAlphaC0Offset() {
        return alphaC0Offset;
    }

    public void setAlphaC0Offset(int alphaC0Offset) {
        this.alphaC0Offset = alphaC0Offset;
    }

    public int getBetaOffset() {
        return betaOffset;
    }

    public void setBetaOffset(int betaOffset) {
        this.betaOffset = betaOffset;
    }

    public boolean isDeblockTop() {
        return deblockTop;
    }

    public void setDeblockTop(boolean deblockTop) {
        this.deblockTop = deblockTop;
    }

    public boolean isDeblockLeft() {
        return deblockLeft;
    }

    public void setDeblockLeft(boolean deblockLeft) {
        this.deblockLeft = deblockLeft;
    }

    public int[] getRefPOCL0() {
        return refPOCL0;
    }

    public int[] getMxL1() {
        return mxL1;
    }

    public int[] getMyL1() {
        return myL1;
    }

    public int[] getRefPOCL1() {
        return refPOCL1;
    }

    public boolean isTransform8x8Used() {
        return transform8x8Used;
    }

    public void setTransform8x8Used(boolean transform8x8Used) {
        this.transform8x8Used = transform8x8Used;
    }

    public void clear() {
        deblockTop = false;
        deblockLeft = false;
        type = null;
        qp[0] = qp[1] = qp[2] = 0;
        Arrays.fill(nc, 0);

        Arrays.fill(mxL0, 0);
        Arrays.fill(myL0, 0);
        Arrays.fill(refPOCL0, Frame.MVS.POC_NO_REFERENCE);
        Arrays.fill(refIdxL0, Frame.MVS.POC_NO_REFERENCE);
        Arrays.fill(refShortTermL0, true);

        Arrays.fill(mxL1, 0);
        Arrays.fill(myL1, 0);
        Arrays.fill(refPOCL1, Frame.MVS.POC_NO_REFERENCE);
        Arrays.fill(refIdxL1, Frame.MVS.POC_NO_REFERENCE);
        Arrays.fill(refShortTermL1, true);

        transform8x8Used = false;
        alphaC0Offset = 0;
        betaOffset = 0;
    }

    public void copyFrom(DecodedMBlock mBlock) {
        pixels.copyFrom(mBlock.pixels);

        deblockTop = mBlock.deblockTop;
        deblockLeft = mBlock.deblockLeft;
        type = mBlock.type;
        qp[0] = mBlock.qp[0];
        qp[1] = mBlock.qp[1];
        qp[2] = mBlock.qp[2];
        System.arraycopy(mBlock.nc, 0, nc, 0, 16);

        System.arraycopy(mBlock.mxL0, 0, mxL0, 0, 16);
        System.arraycopy(mBlock.myL0, 0, myL0, 0, 16);
        System.arraycopy(mBlock.refPOCL0, 0, refPOCL0, 0, 16);
        System.arraycopy(mBlock.refIdxL0, 0, refIdxL0, 0, 16);
        System.arraycopy(mBlock.refShortTermL0, 0, refShortTermL0, 0, 16);

        System.arraycopy(mBlock.mxL1, 0, mxL1, 0, 16);
        System.arraycopy(mBlock.myL1, 0, myL1, 0, 16);
        System.arraycopy(mBlock.refPOCL1, 0, refPOCL1, 0, 16);
        System.arraycopy(mBlock.refIdxL1, 0, refIdxL1, 0, 16);
        System.arraycopy(mBlock.refShortTermL1, 0, refShortTermL1, 0, 16);

        transform8x8Used = mBlock.transform8x8Used;
        alphaC0Offset = mBlock.alphaC0Offset;
        betaOffset = mBlock.betaOffset;
    }

    public void setMvL0(int mvIdx, int mvX, int mvY, int refIdx, int poc, boolean shortTerm) {
        mxL0[mvIdx] = mvX;
        myL0[mvIdx] = mvY;
        refIdxL0[mvIdx] = refIdx;
        refPOCL0[mvIdx] = poc;
        refShortTermL0[mvIdx] = shortTerm;
//        System.out.println("=============================================L0: idx=" + refIdx + ",poc=" + poc);
    }

    public void setMvL1(int mvIdx, int mvX, int mvY, int refIdx, int poc, boolean shortTerm) {
        mxL1[mvIdx] = mvX;
        myL1[mvIdx] = mvY;
        refIdxL1[mvIdx] = refIdx;
        refPOCL1[mvIdx] = poc;
        refShortTermL1[mvIdx] = shortTerm;
//        System.out.println("=============================================L1: idx=" + refIdx + ",poc=" + poc);
    }

    public int[] getRefIdxL0() {
        return refIdxL0;
    }

    public int[] getRefIdxL1() {
        return refIdxL1;
    }
    
    public boolean[] getRefShortTermL0() {
        return refShortTermL0;
    }

    public boolean[] getRefShortTermL1() {
        return refShortTermL1;
    }
}
