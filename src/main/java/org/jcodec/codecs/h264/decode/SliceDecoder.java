package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.aso.MBlockMapper;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.DecodedSlice;
import org.jcodec.codecs.h264.decode.model.MVMatrix;
import org.jcodec.codecs.h264.decode.model.NearbyMotionVectors;
import org.jcodec.codecs.h264.decode.model.NearbyPixels;
import org.jcodec.codecs.h264.decode.model.NearbyPixels.Plane;
import org.jcodec.codecs.h264.io.model.CodedMacroblock;
import org.jcodec.codecs.h264.io.model.CodedSlice;
import org.jcodec.codecs.h264.io.model.MBlockInter;
import org.jcodec.codecs.h264.io.model.MBlockInter8x8;
import org.jcodec.codecs.h264.io.model.MBlockIntra16x16;
import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;
import org.jcodec.codecs.h264.io.model.Macroblock;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Point;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A decoder for an individual slice
 * 
 * @author Jay Codec
 * 
 */
public class SliceDecoder {

    private MBlockDecoderI4x4 decoderI4x4;
    private MBlockDecoderI16x16 decoderI16x16;
    private MBlockDecoderInter decoderInter;
    private int initialQp;
    private int picWidthInMbs;
    private boolean constrainedIntraPred;

    public SliceDecoder(int initialQp, int[] chromaQpOffset, int picWidthInMbs, int bitDepthLuma, int bitDepthChroma,
            boolean constrainedIntraPred) {
        decoderI4x4 = new MBlockDecoderI4x4(chromaQpOffset, bitDepthLuma, bitDepthChroma);
        decoderI16x16 = new MBlockDecoderI16x16(chromaQpOffset, bitDepthChroma, bitDepthChroma);
        decoderInter = new MBlockDecoderInter(chromaQpOffset);

        this.picWidthInMbs = picWidthInMbs;
        this.initialQp = initialQp;

        this.constrainedIntraPred = constrainedIntraPred;
    }

    /**
     * Constructor used by testcase
     */
    protected SliceDecoder(int initialQp, int picWidthInMbs, MBlockDecoderI4x4 decoderI4x4,
            MBlockDecoderI16x16 decoderI16x16, MBlockDecoderInter decoderInter) {
        this.decoderI4x4 = decoderI4x4;
        this.decoderI16x16 = decoderI16x16;
        this.decoderInter = decoderInter;

        this.picWidthInMbs = picWidthInMbs;
        this.initialQp = initialQp;
    }

    public DecodedSlice decodeSlice(CodedSlice slice, Picture[] reference, MBlockMapper mBlockMap) {

        int qp = initialQp + slice.getHeader().slice_qp_delta;

        Macroblock[] mblocks = slice.getMblocks();
        CodedMacroblock[] cmbs = new CodedMacroblock[mblocks.length];
        DecodedMBlock[] decoded = new DecodedMBlock[mblocks.length];
        int[] addresses = mBlockMap.getAddresses(mblocks.length);

        for (int i = 0; i < mblocks.length; i++) {
            Macroblock mblock = mblocks[i];

            if (mblock instanceof CodedMacroblock) {
                CodedMacroblock cmb = (CodedMacroblock) mblock;
                qp = (qp + cmb.getQpDelta() + 52) % 52;
                cmbs[i] = cmb;
            }

            int mbAddr = addresses[i];
            Point origin = new Point((mbAddr % picWidthInMbs) << 4, (mbAddr / picWidthInMbs) << 4);
            DecodedMBlock decodedMblock;
            if (mblock instanceof MBlockIntraNxN) {
                NearbyPixels np = getNearbyPixels(i, decoded, mBlockMap, mblocks);
                decodedMblock = decoderI4x4.decodeINxN((MBlockIntraNxN) mblock, qp, np);
            } else if (mblock instanceof MBlockIntra16x16) {
                NearbyPixels np = getNearbyPixels(i, decoded, mBlockMap, mblocks);
                decodedMblock = decoderI16x16.decodeI16x16((MBlockIntra16x16) mblock, qp, np);
            } else if (mblock instanceof MBlockInter) {
                if (reference == null)
                    throw new RuntimeException("Not allowed P frame before I frame");
                NearbyMotionVectors nearMV = getNearbyMotionVectors(i, decoded, mBlockMap);
                MBlockInter mb = (MBlockInter) mblock;
                decodedMblock = decoderInter.decodeP(reference, mb, nearMV, origin, qp);

            } else if (mblock instanceof MBlockInter8x8) {
                if (reference == null)
                    throw new RuntimeException("Not allowed P frame before I frame");
                MBlockInter8x8 mb = (MBlockInter8x8) mblock;

                NearbyMotionVectors nearMV = getNearbyMotionVectors(i, decoded, mBlockMap);

                decodedMblock = decoderInter.decodeP8x8(reference, mb, nearMV, origin, qp);

            } else {
                if (mblock != null)
                    throw new RuntimeException();
                else {
                    NearbyMotionVectors nearMV = getNearbyMotionVectors(i, decoded, mBlockMap);
                    decodedMblock = decoderInter.decodePSkip(reference, nearMV, origin, qp);
                }
            }

            decoded[i] = decodedMblock;
        }

        return new DecodedSlice(decoded);
    }

    private NearbyMotionVectors getNearbyMotionVectors(int i, DecodedMBlock[] decoded, MBlockMapper mBlockMap) {

        Vector[] left;
        boolean leftAvailable = false;
        int leftAddr = mBlockMap.getLeftMBIdx(i);
        if (leftAddr != -1) {
            leftAvailable = true;
            left = getRightEdge(decoded[leftAddr]);
        } else {
            left = new Vector[] { null, null, null, null };
        }

        Vector[] top;
        boolean topAvailable = false;
        int topAddr = mBlockMap.getTopMBIdx(i);
        if (topAddr != -1) {
            topAvailable = true;
            top = getBottomEdge(decoded[topAddr]);
        } else {
            top = new Vector[] { null, null, null, null };
        }

        Vector rightTop = null;
        boolean rightTopAvailable = false;
        int rightTopAddr = mBlockMap.getTopRightMBIndex(i);
        if (rightTopAddr != -1) {
            rightTopAvailable = true;
            rightTop = getBottomLeft(decoded[rightTopAddr]);
        }

        Vector leftTop = null;
        boolean leftTopAvailable = false;
        int leftTopAddr = mBlockMap.getTopLeftMBIndex(i);
        if (leftTopAddr != -1) {
            leftTopAvailable = true;
            leftTop = getBottomRight(decoded[leftTopAddr]);
        }

        return new NearbyMotionVectors(left, top, rightTop, leftTop, leftAvailable, topAvailable, rightTopAvailable,
                leftTopAvailable);
    }

    private Vector getBottomRight(DecodedMBlock decodedMBlock) {
        MVMatrix mvMatrix = decodedMBlock.getDecodedMVs();
        if (mvMatrix != null) {
            return mvMatrix.getVectors()[15];
        }

        return null;
    }

    private Vector getBottomLeft(DecodedMBlock decodedMBlock) {
        MVMatrix mvMatrix = decodedMBlock.getDecodedMVs();
        if (mvMatrix != null) {
            return mvMatrix.getVectors()[10];
        }

        return null;
    }

    private Vector[] getBottomEdge(DecodedMBlock decodedMBlock) {
        MVMatrix mvMatrix = decodedMBlock.getDecodedMVs();
        if (mvMatrix != null) {
            Vector[] v = mvMatrix.getVectors();
            return new Vector[] { v[10], v[11], v[14], v[15] };
        }

        return null;
    }

    private Vector[] getRightEdge(DecodedMBlock decodedMBlock) {
        MVMatrix mvMatrix = decodedMBlock.getDecodedMVs();
        if (mvMatrix != null) {
            Vector[] v = mvMatrix.getVectors();
            return new Vector[] { v[5], v[7], v[13], v[15] };
        }

        return null;
    }

    private NearbyPixels getNearbyPixels(int mbAddr, DecodedMBlock[] decoded, MBlockMapper mBlockMap, Macroblock[] coded) {
        int[] lumaLeft = null;
        int[] cbLeft = null;
        int[] crLeft = null;
        int leftAddr = mBlockMap.getLeftMBIdx(mbAddr);
        if (leftAddr != -1) {
            boolean leftIntra = (coded[leftAddr] instanceof MBlockIntra16x16)
                    || (coded[leftAddr] instanceof MBlockIntraNxN);
            if (!constrainedIntraPred || leftIntra) {
                lumaLeft = decoded[leftAddr].getLuma();
                cbLeft = decoded[leftAddr].getChroma().getCb();
                crLeft = decoded[leftAddr].getChroma().getCr();
            }
        }

        int[] lumaTop = null;
        int[] cbTop = null;
        int[] crTop = null;
        int topAddr = mBlockMap.getTopMBIdx(mbAddr);
        if (topAddr != -1) {
            boolean topIntra = (coded[topAddr] instanceof MBlockIntra16x16)
                    || (coded[topAddr] instanceof MBlockIntraNxN);
            if (!constrainedIntraPred || topIntra) {
                lumaTop = decoded[topAddr].getLuma();
                cbTop = decoded[topAddr].getChroma().getCb();
                crTop = decoded[topAddr].getChroma().getCr();
            }
        }

        int[] lumaTopLeft = null;
        int[] cbTopLeft = null;
        int[] crTopLeft = null;
        int topLeftAddr = mBlockMap.getTopLeftMBIndex(mbAddr);
        if (topLeftAddr != -1) {
            boolean topLeftIntra = (coded[topLeftAddr] instanceof MBlockIntra16x16)
                    || (coded[topLeftAddr] instanceof MBlockIntraNxN);
            if (!constrainedIntraPred || topLeftIntra) {
                lumaTopLeft = decoded[topLeftAddr].getLuma();
                cbTopLeft = decoded[topLeftAddr].getChroma().getCb();
                crTopLeft = decoded[topLeftAddr].getChroma().getCr();
            }
        }

        int[] lumaTopRight = null;
        int[] cbTopRight = null;
        int[] crTopRight = null;
        int topRightAddr = mBlockMap.getTopRightMBIndex(mbAddr);
        if (topRightAddr != -1) {
            boolean topRightIntra = (coded[topRightAddr] instanceof MBlockIntra16x16)
                    || (coded[topRightAddr] instanceof MBlockIntraNxN);
            if (!constrainedIntraPred || topRightIntra) {
                lumaTopRight = decoded[topRightAddr].getLuma();
                cbTopRight = decoded[topRightAddr].getChroma().getCb();
                crTopRight = decoded[topRightAddr].getChroma().getCr();
            }
        }

        return new NearbyPixels(new Plane(lumaLeft, lumaTop, lumaTopLeft, lumaTopRight), new Plane(cbLeft, cbTop,
                cbTopLeft, cbTopRight), new Plane(crLeft, crTop, crTopLeft, crTopRight));
    }

}
