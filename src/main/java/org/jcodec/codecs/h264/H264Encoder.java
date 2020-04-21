package org.jcodec.codecs.h264;

import static org.jcodec.codecs.h264.H264Utils.escapeNAL;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.codecs.h264.encode.CQPRateControl;
import org.jcodec.codecs.h264.encode.EncodedMB;
import org.jcodec.codecs.h264.encode.EncodingContext;
import org.jcodec.codecs.h264.encode.IntraPredEstimator;
import org.jcodec.codecs.h264.encode.MBDeblocker;
import org.jcodec.codecs.h264.encode.MBEncoderHelper;
import org.jcodec.codecs.h264.encode.MBWriterI16x16;
import org.jcodec.codecs.h264.encode.MBWriterINxN;
import org.jcodec.codecs.h264.encode.MBWriterP16x16;
import org.jcodec.codecs.h264.encode.MotionEstimator;
import org.jcodec.codecs.h264.encode.RateControl;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.Frame;
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
import org.jcodec.codecs.png.PNGEncoder;
import org.jcodec.common.Tuple._3;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

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
        return new H264Encoder(new CQPRateControl(24));
    }

    private RateControl rc;
    private int frameNumber;
    private int keyInterval;
    private int motionSearchRange;

    private int maxPOC;

    private int maxFrameNumber;

    private SeqParameterSet sps;

    private PictureParameterSet pps;

    private MBWriterI16x16 mbEncoderI16x16;
    private MBWriterINxN mbEncoderINxN;
    private MBWriterP16x16 mbEncoderP16x16;

    private Picture ref;
    private Picture picOut;
    private EncodedMB[] topEncoded;

    private boolean psnrEn;
    private long[] sum_se = new long[3];
    private long[] g_sum_se = new long[3];
    private int frameCount;
    private long totalSize;
    private EncodingContext context;
    private H264Decoder decoder;
    private boolean enableRdo;
    private String decodedDump;
    private FileChannelWrapper dumpOut;

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

    public void setEncDecMismatch(boolean test) {
        this.decoder = new H264Decoder();
    }

    public void setEnableRdo(boolean enableRdo) {
        this.enableRdo = enableRdo;
    }

    public void setDecodedDump(String decodedDump) {
        this.decodedDump = decodedDump;
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
            savePsnrStats(data.remaining());
        }
        if (decoder != null) {
            checkEncDecMatch(NIOUtils.cloneBuffer(data));
        }
        if (decodedDump != null)
            dumpDecoded();

        frameCount++;
        return new EncodedFrame(data, idr);
    }

    private void dumpDecoded() {
        try {
            if (decodedDump.endsWith(".png")) {
                PNGEncoder pngEncoder = new PNGEncoder();
                Transform transform = ColorUtil.getTransform(picOut.getColor(), ColorSpace.RGB);
                Picture rgb = Picture.create(picOut.getWidth(), picOut.getHeight(), ColorSpace.RGB);
                transform.transform(picOut, rgb);
                EncodedFrame frame = pngEncoder.encodeFrame(rgb,
                        ByteBuffer.allocate(picOut.getWidth() * picOut.getHeight() * 4));
                NIOUtils.writeTo(frame.getData(), new File(String.format(decodedDump, frameCount)));
            } else {
                if (dumpOut == null) {
                    dumpOut = NIOUtils.writableFileChannel(decodedDump);
                    if (decodedDump.endsWith(".y4m")) {
                        dumpOut.write(ByteBuffer.wrap(String
                                .format("YUV4MPEG2 W%d H%d\n", picOut.getWidth(), picOut.getHeight()).getBytes()));
                    }
                }
                dumpOut.write(ByteBuffer.wrap(picOut.getPlaneData(0)));
                dumpOut.write(ByteBuffer.wrap(picOut.getPlaneData(1)));
                dumpOut.write(ByteBuffer.wrap(picOut.getPlaneData(2)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkEncDecMatch(ByteBuffer data) {
        Picture tmp = picOut.createCompatible();
        Frame decoded = decoder.decodeFrame(data.duplicate(), tmp.getData());
        decoded.setCrop(null);
        decoded.setColor(picOut.getColor());
        _3<Integer, Integer, Integer> mm = decoded.firstMismatch(picOut);
        if (mm != null) {
            int cw = 3 + (mm.v2 == 0 ? 1 : 0);
            throw new RuntimeException(
                    String.format("Encoder-decoder mismatch %d vs %d, f:%d pl:%d x:%d y:%d mbX:%d mbY:%d",
                            decoded.pixAt(mm.v0, mm.v1, mm.v2), picOut.pixAt(mm.v0, mm.v1, mm.v2), frameCount, mm.v2,
                            mm.v0, mm.v1, mm.v0 >> cw, mm.v1 >> cw));
        }
    }

    private void savePsnrStats(int size) {
        for (int p = 0; p < 3; p++) {
            g_sum_se[p] += sum_se[p];
            sum_se[p] = 0;
        }
        totalSize += size;
    }

    private double calcPsnr(long sum, int p) {
        int luma = p == 0 ? 1 : 0;
        int pixCnt = (sps.picHeightInMapUnitsMinus1 + 1) * (sps.picWidthInMbsMinus1 + 1) << (6 + (luma * 2));
        double mse = (double) sum / pixCnt;
        return 10 * Math.log10((255 * 255) / mse);
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

        context = new EncodingContext(mbWidth, mbHeight);

        picOut = Picture.create(mbWidth << 4, mbHeight << 4, ColorSpace.YUV420J);

        topEncoded = new EncodedMB[mbWidth];

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
            int frameNum, SliceType sliceType, int sliceQp) {
        if (idr && sliceType != SliceType.I) {
            idr = false;
            Logger.warn("Illegal value of idr = true when sliceType != I");
        }
        context.cavlc = new CAVLC[] { new CAVLC(sps, pps, 2, 2), new CAVLC(sps, pps, 1, 1), new CAVLC(sps, pps, 1, 1) };
        mbEncoderI16x16 = new MBWriterI16x16();
        mbEncoderINxN = new MBWriterINxN();
        mbEncoderP16x16 = new MBWriterP16x16(sps, ref);

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
        sh.sliceQpDelta = sliceQp - (pps.picInitQpMinus26 + 26);

        ByteBuffer buf = ByteBuffer.allocate(pic.getWidth() * pic.getHeight());
        BitWriter sliceData = new BitWriter(buf);
        SliceHeaderWriter.write(sh, idr, 2, sliceData);
        MotionEstimator estimator = new MotionEstimator(ref, sps, motionSearchRange);
        context.prevQp = sliceQp;

        int mbWidth = sps.picWidthInMbsMinus1 + 1;
        int mbHeight = sps.picHeightInMapUnitsMinus1 + 1;
        int oldQp = sliceQp;
        for (int mbY = 0, mbAddr = 0; mbY < mbHeight; mbY++) {
            for (int mbX = 0; mbX < mbWidth; mbX++, mbAddr++) {
                if (sliceType == SliceType.P) {
                    CAVLCWriter.writeUE(sliceData, 0); // number of skipped mbs
                }

                int qpDelta = rc.initialQpDelta(pic, mbX, mbY);
                int mbQp = oldQp + qpDelta;

                int[] mv = null;
                if (ref != null)
                    mv = estimator.mvEstimate(pic, mbX, mbY);

                NonRdVector params = new NonRdVector(mv, IntraPredEstimator.getLumaMode(pic, context, mbX, mbY),
                        IntraPredEstimator.getLumaPred4x4(pic, context, mbX, mbY, mbQp),
                        IntraPredEstimator.getChromaMode(pic, context, mbX, mbY));

                EncodedMB outMB = new EncodedMB();
                outMB.setPos(mbX, mbY);
                BitWriter candidate;
                EncodingContext fork;
                do {
                    candidate = sliceData.fork();
                    fork = context.fork();
                    rdMacroblock(fork, outMB, sliceType, pic, mbX, mbY, candidate, sliceQp, mbQp, params);
                    qpDelta = rc.accept(candidate.position() - sliceData.position());
                    if (qpDelta != 0)
                        mbQp += qpDelta;
                } while (qpDelta != 0);
                estimator.mvSave(mbX, mbY, new int[] { outMB.mx[0], outMB.my[0], outMB.mr[0] });
                sliceData = candidate;
                context = fork;
                oldQp = mbQp;

                context.update(outMB);
                if (psnrEn)
                    calcMse(pic, outMB, mbX, mbY, sum_se);

                new MBDeblocker().deblockMBP(outMB, mbX > 0 ? topEncoded[mbX - 1] : null,
                        mbY > 0 ? topEncoded[mbX] : null);
                addToReference(outMB, mbX, mbY);
            }
        }
        sliceData.write1Bit(1);
        sliceData.flush();
        buf = sliceData.getBuffer();
        buf.flip();

        escapeNAL(buf, dup);
    }

    private void calcMse(Picture pic, EncodedMB out, int mbX, int mbY, long[] out_se) {
        byte[] patch = new byte[256];
        for (int p = 0; p < 3; p++) {
            byte[] outPix = out.getPixels().getData()[p];
            int luma = p == 0 ? 1 : 0;
            MBEncoderHelper.take(pic.getPlaneData(p), pic.getPlaneWidth(p), pic.getPlaneHeight(p), mbX << (3 + luma),
                    mbY << (3 + luma), patch, 8 << luma, 8 << luma);
            for (int i = 0; i < (64 << (luma * 2)); i++) {
                int q = outPix[i] - patch[i];
                out_se[p] += q * q;
            }
        }
    }

    public static class RdVector {
        public MBType mbType;
        public int qp;

        public RdVector(MBType mbType, int qp) {
            this.mbType = mbType;
            this.qp = qp;
        }
    }

    public static class NonRdVector {
        public int[] mv;
        public int lumaPred16x16;
        public int[] lumaPred4x4;
        public int chrPred;

        public NonRdVector(int[] mv, int lumaPred16x16, int[] lumaPred4x4, int chrPred) {
            this.mv = mv;
            this.lumaPred16x16 = lumaPred16x16;
            this.lumaPred4x4 = lumaPred4x4;
            this.chrPred = chrPred;
        }
    }

    private void rdMacroblock(EncodingContext ctx, EncodedMB outMB, SliceType sliceType, Picture pic, int mbX, int mbY,
            BitWriter candidate, int sliceQp, int mbQp, NonRdVector params) {
        if (!enableRdo) {
            RdVector vector = sliceType == SliceType.P ? new RdVector(MBType.P_16x16, mbQp)
                    : new RdVector(MBType.I_16x16, mbQp);
            encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, params, vector);
            return;
        }

        List<RdVector> cands = new LinkedList<RdVector>();
        cands.add(new RdVector(MBType.I_16x16, mbQp));
        cands.add(new RdVector(MBType.I_NxN, mbQp));
        if (sliceType == SliceType.P) {
            cands.add(new RdVector(MBType.P_16x16, mbQp));
        }
        long bestRd = Long.MAX_VALUE;
        RdVector bestVector = null;

        for (RdVector rdVector : cands) {
            EncodingContext candCtx = ctx.fork();
            BitWriter candBits = candidate.fork();
            long rdCost = tryVector(candCtx, sliceType, pic, mbX, mbY, candBits, sliceQp, params, rdVector);
            if (rdCost < bestRd) {
                bestRd = rdCost;
                bestVector = rdVector;
            }
        }
        encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, params, bestVector);
    }

    private long tryVector(EncodingContext ctx, SliceType sliceType, Picture pic, int mbX, int mbY, BitWriter candidate,
            int sliceQp, NonRdVector params, RdVector vector) {
        int start = candidate.position();
        EncodedMB outMB = new EncodedMB();
        outMB.setPos(mbX, mbY);
        encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, params, vector);

        long[] se = new long[3];
        calcMse(pic, outMB, mbX, mbY, se);
        long mse = (se[0] + se[1] + se[2]) / 384;
        int bits = candidate.position() - start;
        return rdCost(mse, bits, H264Const.lambda[sliceQp]);
    }

    private long rdCost(long mse, int bits, int lambda) {
        return mse + ((lambda * bits) >> 8);
    }

    private void encodeCand(EncodingContext ctx, EncodedMB outMB, SliceType sliceType, Picture pic, int mbX, int mbY,
            BitWriter candidate, NonRdVector params, RdVector vector) {
        if (vector.mbType == MBType.I_16x16) {
            BitWriter tmp = new BitWriter(ByteBuffer.allocate(1024));
            boolean cbpLuma = mbEncoderI16x16.encodeMacroblock(ctx, pic, mbX, mbY, tmp, outMB, vector.qp, params);
            int cbpChroma = mbEncoderI16x16.getCbpChroma(pic, mbX, mbY);

            int i16x16TypeOffset = (cbpLuma ? 12 : 0) + cbpChroma * 4 + params.lumaPred16x16;
            int mbTypeOffset = sliceType == SliceType.P ? 5 : 0;

            CAVLCWriter.writeUE(candidate, mbTypeOffset + vector.mbType.code() + i16x16TypeOffset);
            candidate.writeOther(tmp);
        } else if (vector.mbType == MBType.P_16x16) {
            CAVLCWriter.writeUE(candidate, vector.mbType.code());
            mbEncoderP16x16.encodeMacroblock(ctx, pic, mbX, mbY, candidate, outMB, vector.qp, params);
        } else if (vector.mbType == MBType.I_NxN) {
            CAVLCWriter.writeUE(candidate, sliceType == SliceType.P ? 5 : 0);
            mbEncoderINxN.encodeMacroblock(ctx, pic, mbX, mbY, candidate, outMB, vector.qp, params);
        } else
            throw new RuntimeException("Macroblock of type " + vector.mbType + " is not supported.");
    }

    private void addToReference(EncodedMB outMB, int mbX, int mbY) {
        if (mbY > 0)
            MBEncoderHelper.putBlkPic(picOut, topEncoded[mbX].getPixels(), mbX << 4, (mbY - 1) << 4);
        topEncoded[mbX] = outMB;
    }

    private void putLastMBLine() {
        int mbWidth = sps.picWidthInMbsMinus1 + 1;
        int mbHeight = sps.picHeightInMapUnitsMinus1 + 1;
        for (int mbX = 0; mbX < mbWidth; mbX++)
            MBEncoderHelper.putBlkPic(picOut, topEncoded[mbX].getPixels(), mbX << 4, (mbHeight - 1) << 4);
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
            int fc = frameCount + 1;
            long avgSum = (g_sum_se[0] + g_sum_se[1] * 4 + g_sum_se[2] * 4) / 3;
            double avgPsnr = calcPsnr(avgSum / fc, 0);
            double yPsnr = calcPsnr(g_sum_se[0] / fc, 0);
            double uPsnr = calcPsnr(g_sum_se[1] / fc, 1);
            double vPsnr = calcPsnr(g_sum_se[2] / fc, 2);
            Logger.info(String.format("PSNR AVG:%.3f Y:%.3f U:%.3f V:%.3f kbps:%.3f", avgPsnr, yPsnr, uPsnr, vPsnr,
                    (double) (8 * 25 * (totalSize / fc)) / 1000));
        }
        if (dumpOut != null)
            NIOUtils.closeQuietly(dumpOut);
    }
}
