package org.jcodec.codecs.h264;

import static java.lang.System.arraycopy;
import static org.jcodec.codecs.h264.H264Utils.escapeNAL;

import org.jcodec.codecs.h264.encode.DumbRateControl;
import org.jcodec.codecs.h264.encode.EncodedMB;
import org.jcodec.codecs.h264.encode.MBEncoderHelper;
import org.jcodec.codecs.h264.encode.MBWriterI16x16;
import org.jcodec.codecs.h264.encode.MBWriterP16x16;
import org.jcodec.codecs.h264.encode.MotionEstimator;
import org.jcodec.codecs.h264.encode.RateControl;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.RefPicMarkingIDR;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.io.write.CAVLCWriter;
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MathUtil;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 4 AVC ( H.264 ) Encoder
 * 
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 * 
 * @author The JCodec project
 * 
 */
public class H264Encoder extends VideoEncoder {

    // private static final int QP = 20;
    private static final int KEY_INTERVAL_DEFAULT = 25;
    private static final int MOTION_SEARCH_RANGE_DEFAULT = 16;

    public static H264Encoder createH264Encoder() {
        return new H264Encoder(new DumbRateControl());
    }

    private CAVLC[] cavlc;
    private byte[][] leftRow;
    private byte[][] topLine;
    private RateControl rc;
    private int frameNumber;
    private int keyInterval;
    private int motionSearchRange;

    private int maxPOC;

    private int maxFrameNumber;

    private SeqParameterSet sps;

    private PictureParameterSet pps;

    private MBWriterI16x16 mbEncoderI16x16;

    private MBWriterP16x16 mbEncoderP16x16;

    private Picture ref;
    private Picture picOut;
    private EncodedMB[] topEncoded;

    private EncodedMB outMB;
    private boolean psnrEn;
    private long[] sum_se = new long[3];
    private long[] g_sum_se = new long[3];
    private int frameCount;
    private long totalSize;

    public H264Encoder(RateControl rc) {
        this.rc = rc;
        this.keyInterval = KEY_INTERVAL_DEFAULT;
        this.motionSearchRange = MOTION_SEARCH_RANGE_DEFAULT;
    }

    public int getKeyInterval() {
        return keyInterval;
    }

    public void setKeyInterval(int keyInterval) {
        this.keyInterval = keyInterval;
    }

    public int getMotionSearchRange() {
        return motionSearchRange;
    }

    public void setMotionSearchRange(int motionSearchRange) {
        this.motionSearchRange = motionSearchRange;
    }

    public boolean isPsnrEn() {
        return psnrEn;
    }

    public void setPsnrEn(boolean psnrEn) {
        this.psnrEn = psnrEn;
    }
    
    /**
     * Encode this picture into h.264 frame. Frame type will be selected by encoder.
     */
    public EncodedFrame encodeFrame(Picture pic, ByteBuffer _out) {
        if (pic.getColor() != ColorSpace.YUV420J)
            throw new IllegalArgumentException("Input picture color is not supported: " + pic.getColor());

        if (frameNumber >= keyInterval) {
            frameNumber = 0;
        }

        SliceType sliceType = frameNumber == 0 ? SliceType.I : SliceType.P;
        boolean idr = frameNumber == 0;

        ByteBuffer data = doEncodeFrame(pic, _out, idr, frameNumber++, sliceType);
        if (psnrEn) {
//            System.out.println(String.format("PSNR %.3f %.3f %.3f", calcPsnr(sum_se[0], 0), calcPsnr(sum_se[1], 1),
//                    calcPsnr(sum_se[2], 2)));
            savePsnrStats(data.remaining());
        }

        return new EncodedFrame(data, idr);
    }

    private void savePsnrStats(int size) {
        for (int p = 0; p < 3; p++) {
            g_sum_se[p] += sum_se[p];
            sum_se[p] = 0;
        }
        frameCount++;
        totalSize += size;
    }

    private double calcPsnr(long sum, int p) {
        int luma = p == 0 ? 1 : 0;
        int pixCnt = (sps.picHeightInMapUnitsMinus1 + 1) * (sps.picWidthInMbsMinus1 + 1) << (6 + (luma * 2));
        double mse = (double) sum / pixCnt;
        return 10 * Math.log10((255*255) / mse);
    }

    /**
     * Encode this picture as an IDR frame. IDR frame starts a new independently
     * decodeable video sequence
     * 
     * @param pic
     * @param _out
     * @return
     */
    public ByteBuffer encodeIDRFrame(Picture pic, ByteBuffer _out) {
        frameNumber = 0;
        return doEncodeFrame(pic, _out, true, frameNumber, SliceType.I);
    }

    /**
     * Encode this picture as a P-frame. P-frame is an frame predicted from one or
     * more of the previosly decoded frame and is usually 10x less in size then the
     * IDR frame.
     * 
     * @param pic
     * @param _out
     * @return
     */
    public ByteBuffer encodePFrame(Picture pic, ByteBuffer _out) {
        frameNumber++;
        return doEncodeFrame(pic, _out, true, frameNumber, SliceType.P);
    }

    public ByteBuffer doEncodeFrame(Picture pic, ByteBuffer _out, boolean idr, int frameNumber, SliceType frameType) {
        ByteBuffer dup = _out.duplicate();
        int maxSize = Math.min(dup.remaining(), pic.getWidth() * pic.getHeight());
        maxSize -= (maxSize >>> 6); // 1.5% to account for escaping
        int qp = rc.startPicture(pic.getSize(), maxSize, frameType);

        if (idr) {
            sps = initSPS(new Size(pic.getCroppedWidth(), pic.getCroppedHeight()));
            pps = initPPS();

            maxPOC = 1 << (sps.log2MaxPicOrderCntLsbMinus4 + 4);
            maxFrameNumber = 1 << (sps.log2MaxFrameNumMinus4 + 4);
        }

        if (idr) {
            dup.putInt(0x1);
            new NALUnit(NALUnitType.SPS, 3).write(dup);
            writeSPS(dup, sps);

            dup.putInt(0x1);
            new NALUnit(NALUnitType.PPS, 3).write(dup);
            writePPS(dup, pps);
        }

        int mbWidth = sps.picWidthInMbsMinus1 + 1;
        int mbHeight = sps.picHeightInMapUnitsMinus1 + 1;

        leftRow = new byte[][] { new byte[16], new byte[8], new byte[8] };
        topLine = new byte[][] { new byte[mbWidth << 4], new byte[mbWidth << 3], new byte[mbWidth << 3] };
        picOut = Picture.create(mbWidth << 4, mbHeight << 4, ColorSpace.YUV420J);

        outMB = new EncodedMB();
        topEncoded = new EncodedMB[mbWidth];
        for (int i = 0; i < mbWidth; i++)
            topEncoded[i] = new EncodedMB();

        encodeSlice(sps, pps, pic, dup, idr, frameNumber, frameType, qp);

        putLastMBLine();

        ref = picOut;

        dup.flip();
        return dup;
    }

    private void writePPS(ByteBuffer dup, PictureParameterSet pps) {
        ByteBuffer tmp = ByteBuffer.allocate(1024);
        pps.write(tmp);
        tmp.flip();
        escapeNAL(tmp, dup);
    }

    private void writeSPS(ByteBuffer dup, SeqParameterSet sps) {
        ByteBuffer tmp = ByteBuffer.allocate(1024);
        sps.write(tmp);
        tmp.flip();
        escapeNAL(tmp, dup);
    }

    public PictureParameterSet initPPS() {
        PictureParameterSet pps = new PictureParameterSet();
        pps.picInitQpMinus26 = 0; // start with qp = 26
        return pps;
    }

    public SeqParameterSet initSPS(Size sz) {
        SeqParameterSet sps = new SeqParameterSet();
        sps.picWidthInMbsMinus1 = ((sz.getWidth() + 15) >> 4) - 1;
        sps.picHeightInMapUnitsMinus1 = ((sz.getHeight() + 15) >> 4) - 1;
        sps.chromaFormatIdc = ColorSpace.YUV420J;
        sps.profileIdc = 66;
        sps.levelIdc = 40;
        sps.numRefFrames = 1;
        sps.frameMbsOnlyFlag = true;
        sps.log2MaxFrameNumMinus4 = Math.max(0, MathUtil.log2(keyInterval) - 3);

        int codedWidth = (sps.picWidthInMbsMinus1 + 1) << 4;
        int codedHeight = (sps.picHeightInMapUnitsMinus1 + 1) << 4;
        sps.frameCroppingFlag = codedWidth != sz.getWidth() || codedHeight != sz.getHeight();
        sps.frameCropRightOffset = (codedWidth - sz.getWidth() + 1) >> 1;
        sps.frameCropBottomOffset = (codedHeight - sz.getHeight() + 1) >> 1;

        return sps;
    }

    private void encodeSlice(SeqParameterSet sps, PictureParameterSet pps, Picture pic, ByteBuffer dup, boolean idr,
            int frameNum, SliceType sliceType, int qp) {
        if (idr && sliceType != SliceType.I) {
            idr = false;
            Logger.warn("Illegal value of idr = true when sliceType != I");
        }
        cavlc = new CAVLC[] { new CAVLC(sps, pps, 2, 2), new CAVLC(sps, pps, 1, 1), new CAVLC(sps, pps, 1, 1) };
        mbEncoderI16x16 = new MBWriterI16x16(cavlc, leftRow, topLine);
        mbEncoderP16x16 = new MBWriterP16x16(sps, ref, cavlc);

        dup.putInt(0x1);
        new NALUnit(idr ? NALUnitType.IDR_SLICE : NALUnitType.NON_IDR_SLICE, 3).write(dup);
        SliceHeader sh = new SliceHeader();
        sh.sliceType = sliceType;
        if (idr)
            sh.refPicMarkingIDR = new RefPicMarkingIDR(false, false);
        sh.pps = pps;
        sh.sps = sps;
        sh.picOrderCntLsb = (frameNum << 1) % maxPOC;
        sh.frameNum = frameNum % maxFrameNumber;
        sh.sliceQpDelta = qp - (pps.picInitQpMinus26 + 26);

        ByteBuffer buf = ByteBuffer.allocate(pic.getWidth() * pic.getHeight());
        BitWriter sliceData = new BitWriter(buf);
        SliceHeaderWriter.write(sh, idr, 2, sliceData);
        MotionEstimator estimator = new MotionEstimator(ref, sps, motionSearchRange);

        for (int mbY = 0, mbAddr = 0; mbY < sps.picHeightInMapUnitsMinus1 + 1; mbY++) {
            for (int mbX = 0; mbX < sps.picWidthInMbsMinus1 + 1; mbX++, mbAddr++) {
                if (sliceType == SliceType.P) {
                    CAVLCWriter.writeUE(sliceData, 0); // number of skipped mbs
                }

                MBType mbType = selectMBType(sliceType);

                if (mbType == MBType.I_16x16) {
                    // I16x16 carries part of layout information in the
                    // macroblock type
                    // itself for this reason we'll have to decide it now to
                    // embed into
                    // macroblock type
                    int predMode = mbEncoderI16x16.getPredMode(pic, mbX, mbY);
                    int cbpChroma = mbEncoderI16x16.getCbpChroma(pic, mbX, mbY);
                    int cbpLuma = mbEncoderI16x16.getCbpLuma(pic, mbX, mbY);

                    int i16x16TypeOffset = (cbpLuma / 15) * 12 + cbpChroma * 4 + predMode;
                    int mbTypeOffset = sliceType == SliceType.P ? 5 : 0;

                    CAVLCWriter.writeUE(sliceData, mbTypeOffset + mbType.code() + i16x16TypeOffset);
                } else {
                    CAVLCWriter.writeUE(sliceData, mbType.code());
                }

                int[] mv = null;
                if (ref != null)
                    mv = estimator.mvEstimate(pic, mbX, mbY);

                BitWriter candidate;
                int totalQpDelta = 0;
                int qpDelta = rc.initialQpDelta();
                do {
                    candidate = sliceData.fork();
                    totalQpDelta += qpDelta;
                    encodeMacroblock(mbType, pic, mbX, mbY, candidate, qp, totalQpDelta, mv);
                    qpDelta = rc.accept(candidate.position() - sliceData.position());
                    if (qpDelta != 0)
                        restoreMacroblock(mbType);
                } while (qpDelta != 0);
                sliceData = candidate;
                qp += totalQpDelta;

                collectPredictors(outMB.getPixels(), mbX);
                if (psnrEn)
                    calcMse(pic, outMB, mbX, mbY);
                addToReference(mbX, mbY);
            }
        }
        sliceData.write1Bit(1);
        sliceData.flush();
        buf = sliceData.getBuffer();
        buf.flip();

        escapeNAL(buf, dup);
    }

    private void calcMse(Picture pic, EncodedMB out, int mbX, int mbY) {
        byte[] patch = new byte[256];
        for (int p = 0; p < 3; p++) {
            byte[] outPix = out.getPixels().getData()[p];
            int luma = p == 0 ? 1 : 0;
            MBEncoderHelper.take(pic.getPlaneData(p), pic.getPlaneWidth(p), pic.getPlaneHeight(p), mbX << (3 + luma),
                    mbY << (3 + luma), patch, 8 << luma, 8 << luma);
            for (int i = 0; i < (64 << (luma * 2)); i++) {
                int q = outPix[i] - patch[i];
                sum_se[p] += q * q;
            }
        }
    }

    private void encodeMacroblock(MBType mbType, Picture pic, int mbX, int mbY, BitWriter candidate, int qp,
            int qpDelta, int[] mv) {
        if (mbType == MBType.I_16x16) {
            mbEncoderI16x16.save();
            mbEncoderI16x16.encodeMacroblock(pic, mbX, mbY, candidate, outMB, mbX > 0 ? topEncoded[mbX - 1] : null,
                    mbY > 0 ? topEncoded[mbX] : null, qp + qpDelta, qpDelta);
        } else if (mbType == MBType.P_16x16) {
            mbEncoderP16x16.save();
            mbEncoderP16x16.encodeMacroblock(pic, mbX, mbY, candidate, outMB, mbX > 0 ? topEncoded[mbX - 1] : null,
                    mbY > 0 ? topEncoded[mbX] : null, qp + qpDelta, qpDelta, mv);
        } else
            throw new RuntimeException("Macroblock of type " + mbType + " is not supported.");
    }

    private void restoreMacroblock(MBType mbType) {
        if (mbType == MBType.I_16x16) {
            mbEncoderI16x16.restore();
        } else if (mbType == MBType.P_16x16) {
            mbEncoderP16x16.restore();
        } else
            throw new RuntimeException("Macroblock of type " + mbType + " is not supported.");
    }

    private MBType selectMBType(SliceType sliceType) {
        if (sliceType == SliceType.I)
            return MBType.I_16x16;
        else if (sliceType == SliceType.P)
            return MBType.P_16x16;
        else
            throw new RuntimeException("Unsupported slice type");
    }

    private void addToReference(int mbX, int mbY) {
        if (mbY > 0)
            MBEncoderHelper.putBlkPic(picOut, topEncoded[mbX].getPixels(), mbX << 4, (mbY - 1) << 4);
        EncodedMB tmp = topEncoded[mbX];
        topEncoded[mbX] = outMB;
        outMB = tmp;
    }

    private void putLastMBLine() {
        int mbWidth = sps.picWidthInMbsMinus1 + 1;
        int mbHeight = sps.picHeightInMapUnitsMinus1 + 1;
        for (int mbX = 0; mbX < mbWidth; mbX++)
            MBEncoderHelper.putBlkPic(picOut, topEncoded[mbX].getPixels(), mbX << 4, (mbHeight - 1) << 4);
    }

    private void collectPredictors(Picture outMB, int mbX) {
        arraycopy(outMB.getPlaneData(0), 240, topLine[0], mbX << 4, 16);
        arraycopy(outMB.getPlaneData(1), 56, topLine[1], mbX << 3, 8);
        arraycopy(outMB.getPlaneData(2), 56, topLine[2], mbX << 3, 8);

        copyCol(outMB.getPlaneData(0), 15, 16, leftRow[0]);
        copyCol(outMB.getPlaneData(1), 7, 8, leftRow[1]);
        copyCol(outMB.getPlaneData(2), 7, 8, leftRow[2]);
    }

    private void copyCol(byte[] planeData, int off, int stride, byte[] out) {
        for (int i = 0; i < out.length; i++) {
            out[i] = planeData[off];
            off += stride;
        }
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[] { ColorSpace.YUV420J };
    }

    @Override
    public int estimateBufferSize(Picture frame) {
        return Math.max(1 << 16, frame.getWidth() * frame.getHeight());
    }

    @Override
    public void finish() {
        if (psnrEn) {
            long avgSum = (g_sum_se[0] + g_sum_se[1]*4 + g_sum_se[2]*4) / 3;
            Logger.info(String.format("PSNR AVG:%.3f Y:%.3f U:%.3f V:%.3f kbps:%.3f",
                    frameCount != 0 ? calcPsnr(avgSum / frameCount, 0) : 0,
                    frameCount != 0 ? calcPsnr(g_sum_se[0] / frameCount, 0) : 0,
                    frameCount != 0 ? calcPsnr(g_sum_se[1] / frameCount, 1) : 0,
                    frameCount != 0 ? calcPsnr(g_sum_se[2] / frameCount, 2) : 0,
                    frameCount != 0 ? (double) (8 * 25 * (totalSize / frameCount)) / 1000 : 0));
        }
    }
}
