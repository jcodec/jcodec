package org.jcodec.codecs.h264.io.model;

import java.util.Arrays;
import java.util.Comparator;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Picture extension with frame number, makes it easier to debug reordering
 * 
 * @author The JCodec project
 * 
 */
public class Frame extends Picture8Bit {
    private int frameNo;
    private SliceType frameType;
    private MVS mvs;
    private boolean shortTerm;
    private int poc;

    /**
     * Stores motion vectors used in a picture.
     * 
     * Each macroblock has 16 entries in the arrays below: [mbX * 4][mbY * 4]
     * ... [mbX * 4 + 3][mbY * 4 + 3] The 2d array is represented as 1d arrays,
     * the contents are stored in row-order
     */
    public static class MVS {
        /**
         * A special POC value that means the particular list (L0,L1) was not
         * used for reference when predicting a block at location (blkX,blkY)
         */
        public static final int POC_NO_REFERENCE = -1;

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

        private int stride;

        public MVS(int mbWidth, int mbHeight) {
            this.stride = mbWidth << 2;
            int size = (mbWidth * mbHeight) << 4;

            mxL0 = new int[size];
            myL0 = new int[size];
            refIdxL0 = new int[size];
            refPOCL0 = new int[size];
            refShortTermL0 = new boolean[size];
            Arrays.fill(refIdxL0, -1);
            Arrays.fill(refPOCL0, -1);

            mxL1 = new int[size];
            myL1 = new int[size];
            refIdxL1 = new int[size];
            refPOCL1 = new int[size];
            refShortTermL1 = new boolean[size];
            Arrays.fill(refIdxL1, -1);
            Arrays.fill(refPOCL1, -1);
        }

        public void saveMBVectors(int mbX, int mbY, int[] mxL0, int[] myL0, int[] refIdxL0, int[] refPOCL0,
                boolean[] refShortTermL0, int[] mxL1, int[] myL1, int[] refIdxL1, int[] refPOCL1,
                boolean[] refShortTermL1) {
            for (int i = 0, off = (mbY << 2) * stride + (mbX << 2); i < 16; i += 4, off += stride) {
                for (int j = 0; j < 4; j++) {
                    this.mxL0[off + j] = mxL0[i + j];
                    this.myL0[off + j] = myL0[i + j];
                    this.refIdxL0[off + j] = refIdxL0[i + j];
                    this.refPOCL0[off + j] = refPOCL0[i + j];
                    this.refShortTermL0[off + j] = refShortTermL0[i + j];
                    this.mxL1[off + j] = mxL1[i + j];
                    this.myL1[off + j] = myL1[i + j];
                    this.refIdxL1[off + j] = refIdxL1[i + j];
                    this.refPOCL1[off + j] = refPOCL1[i + j];
                    this.refShortTermL1[off + j] = refShortTermL1[i + j];
                }
            }
        }

        public int getRefPOCL0(int blkPosY, int blkPosX) {
            return refPOCL0[blkPosY * stride + blkPosX];
        }

        public int getRefPOCL1(int blkPosY, int blkPosX) {
            return refPOCL1[blkPosY * stride + blkPosX];
        }

        public int getMxL0(int blkPosY, int blkPosX) {
            return mxL0[blkPosY * stride + blkPosX];
        }

        public int getMyL0(int blkPosY, int blkPosX) {
            return myL0[blkPosY * stride + blkPosX];
        }

        public int getMxL1(int blkPosY, int blkPosX) {
            return mxL0[blkPosY * stride + blkPosX];
        }

        public int getMyL1(int blkPosY, int blkPosX) {
            return myL0[blkPosY * stride + blkPosX];
        }

        public int getRefIdxL0(int blkPosY, int blkPosX) {
            return refIdxL0[blkPosY * stride + blkPosX];
        }

        public int getRefIdxL1(int blkPosY, int blkPosX) {
            return refIdxL1[blkPosY * stride + blkPosX];
        }

        public boolean getRefShortTermL0(int blkPosY, int blkPosX) {
            return refShortTermL0[blkPosY * stride + blkPosX];
        }

        public boolean getRefShortTermL1(int blkPosY, int blkPosX) {
            return refShortTermL1[blkPosY * stride + blkPosX];
        }
    }

    public Frame(int width, int height, byte[][] data, ColorSpace color, Rect crop, int frameNo, SliceType frameType,
            MVS mvs, int poc) {
        super(width, height, data, color, crop);
        this.frameNo = frameNo;
        this.mvs = mvs;
        this.poc = poc;
        shortTerm = true;
    }

    public static Frame createFrame(Frame pic) {
        Picture8Bit comp = pic.createCompatible();
        return new Frame(comp.getWidth(), comp.getHeight(), comp.getData(), comp.getColor(), pic.getCrop(),
                pic.frameNo, pic.frameType, pic.mvs, pic.poc);
    }

    public void copyFrom(Frame src) {
        super.copyFrom(src);
        this.frameNo = src.frameNo;
        this.mvs = src.mvs;
        this.shortTerm = src.shortTerm;
        this.poc = src.poc;
    }

    public int getFrameNo() {
        return frameNo;
    }

    public MVS getMvs() {
        return mvs;
    }

    public boolean isShortTerm() {
        return shortTerm;
    }

    public void setShortTerm(boolean shortTerm) {
        this.shortTerm = shortTerm;
    }

    public int getPOC() {
        return poc;
    }

    public static Comparator<Frame> POCAsc = new Comparator<Frame>() {
        public int compare(Frame o1, Frame o2) {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return 1;
            else if (o2 == null)
                return -1;
            else
                return o1.poc > o2.poc ? 1 : (o1.poc == o2.poc ? 0 : -1);
        }
    };

    public static Comparator<Frame> POCDesc = new Comparator<Frame>() {
        public int compare(Frame o1, Frame o2) {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return 1;
            else if (o2 == null)
                return -1;
            else
                return o1.poc < o2.poc ? 1 : (o1.poc == o2.poc ? 0 : -1);
        }
    };

    public SliceType getFrameType() {
        return frameType;
    }
}