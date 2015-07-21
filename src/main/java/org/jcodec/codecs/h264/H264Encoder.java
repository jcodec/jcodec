package org.jcodec.codecs.h264;

import static org.jcodec.codecs.h264.H264Utils.escapeNAL;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.encode.DumbRateControl;
import org.jcodec.codecs.h264.encode.MBEncoderI16x16;
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
public class H264Encoder implements VideoEncoder {

    // private static final int QP = 20;
    private static final int KEY_INTERVAL_DEFAULT = 1;

    private CAVLC[] cavlc;
    private int[][] leftRow;
    private int[][] topLine;
    private RateControl rc;
    private int frameNumber;
    private int keyInterval = KEY_INTERVAL_DEFAULT;

    private int maxPOC;

    private int maxFrameNumber;

    private SeqParameterSet sps;

    private PictureParameterSet pps;

    private MBEncoderI16x16 mbEncoderI16x16;

    public H264Encoder() {
        this(new DumbRateControl());
    }

    public H264Encoder(RateControl rc) {
        this.rc = rc;
    }

    public int getKeyInterval() {
        return keyInterval;
    }

    public void setKeyInterval(int keyInterval) {
        this.keyInterval = keyInterval;
    }

    /**
     * Encode this picture into h.264 frame. Frame type will be selected by
     * encoder.
     */
    public ByteBuffer encodeFrame(Picture pic, ByteBuffer _out) {
        if (frameNumber >= keyInterval) {
            frameNumber = 0;
        }

        SliceType sliceType = frameNumber == 0 ? SliceType.I : SliceType.P;
        boolean idr = frameNumber == 0;

        return encodeFrame(pic, _out, idr, frameNumber++, sliceType);
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
        return encodeFrame(pic, _out, true, frameNumber, SliceType.I);
    }

    /**
     * Encode this picture as a P-frame. P-frame is an frame predicted from one
     * or more of the previosly decoded frame and is usually 10x less in size
     * then the IDR frame.
     * 
     * @param pic
     * @param _out
     * @return
     */
    public ByteBuffer encodePFrame(Picture pic, ByteBuffer _out) {
        frameNumber++;
        return encodeFrame(pic, _out, true, frameNumber, SliceType.P);
    }

    public ByteBuffer encodeFrame(Picture pic, ByteBuffer _out, boolean idr, int poc, SliceType frameType) {
        ByteBuffer dup = _out.duplicate();

        if (idr) {
            sps = initSPS(new Size(pic.getCroppedWidth(), pic.getCroppedHeight()));
            pps = initPPS();

            maxPOC = 1 << (sps.log2_max_pic_order_cnt_lsb_minus4 + 4);
            maxFrameNumber = 1 << (sps.log2_max_frame_num_minus4 + 4);
        }

        if (idr) {
            dup.putInt(0x1);
            new NALUnit(NALUnitType.SPS, 3).write(dup);
            writeSPS(dup, sps);

            dup.putInt(0x1);
            new NALUnit(NALUnitType.PPS, 3).write(dup);
            writePPS(dup, pps);
        }

        int mbWidth = sps.pic_width_in_mbs_minus1 + 1;

        leftRow = new int[][] { new int[16], new int[8], new int[8] };
        topLine = new int[][] { new int[mbWidth << 4], new int[mbWidth << 3], new int[mbWidth << 3] };

        encodeSlice(sps, pps, pic, dup, idr, poc, frameType);

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
        pps.pic_init_qp_minus26 = rc.getInitQp() - 26;
        return pps;
    }

    public SeqParameterSet initSPS(Size sz) {
        SeqParameterSet sps = new SeqParameterSet();
        sps.pic_width_in_mbs_minus1 = ((sz.getWidth() + 15) >> 4) - 1;
        sps.pic_height_in_map_units_minus1 = ((sz.getHeight() + 15) >> 4) - 1;
        sps.chroma_format_idc = ColorSpace.YUV420;
        sps.profile_idc = 66;
        sps.level_idc = 40;
        sps.frame_mbs_only_flag = true;

        int codedWidth = (sps.pic_width_in_mbs_minus1 + 1) << 4;
        int codedHeight = (sps.pic_height_in_map_units_minus1 + 1) << 4;
        sps.frame_cropping_flag = codedWidth != sz.getWidth() || codedHeight != sz.getHeight();
        sps.frame_crop_right_offset = (codedWidth - sz.getWidth() + 1) >> 1;
        sps.frame_crop_bottom_offset = (codedHeight - sz.getHeight() + 1) >> 1;

        return sps;
    }

    private void encodeSlice(SeqParameterSet sps, PictureParameterSet pps, Picture pic, ByteBuffer dup, boolean idr,
            int frameNum, SliceType sliceType) {
        if (idr && sliceType != SliceType.I) {
            idr = false;
            Logger.warn("Illegal value of idr = true when sliceType != I");
        }
        cavlc = new CAVLC[] { new CAVLC(sps, pps, 2, 2), new CAVLC(sps, pps, 1, 1), new CAVLC(sps, pps, 1, 1) };
        mbEncoderI16x16 = new MBEncoderI16x16(cavlc, leftRow, topLine);

        rc.reset();
        int qp = rc.getInitQp();

        dup.putInt(0x1);
        new NALUnit(idr ? NALUnitType.IDR_SLICE : NALUnitType.NON_IDR_SLICE, 2).write(dup);
        SliceHeader sh = new SliceHeader();
        sh.slice_type = sliceType;
        if (idr)
            sh.refPicMarkingIDR = new RefPicMarkingIDR(false, false);
        sh.pps = pps;
        sh.sps = sps;
        sh.pic_order_cnt_lsb = (frameNum << 1) % maxPOC;
        sh.frame_num = frameNum % maxFrameNumber;

        ByteBuffer buf = ByteBuffer.allocate(pic.getWidth() * pic.getHeight());
        BitWriter sliceData = new BitWriter(buf);
        new SliceHeaderWriter().write(sh, idr, 2, sliceData);

        Picture outMB = Picture.create(16, 16, ColorSpace.YUV420);

        int mbTypeOffset = sliceType == SliceType.P ? 5 : 0;

        for (int mbY = 0; mbY < sps.pic_height_in_map_units_minus1 + 1; mbY++) {
            for (int mbX = 0; mbX < sps.pic_width_in_mbs_minus1 + 1; mbX++) {
                if (sliceType == SliceType.P) {
                    CAVLCWriter.writeUE(sliceData, 0); // number of skipped mbs
                }
                
                MBType mbType = selectMBType();
                
                if (mbType == MBType.I_16x16) {
                    // I16x16 carries part of layout information in the macroblock type
                    // itself for this reason we'll have to decide it now to embed into
                    // macroblock type
                    int predMode = mbEncoderI16x16.getPredMode(pic, mbX, mbY);
                    int cbpChroma = mbEncoderI16x16.getCbpChroma(pic, mbX, mbY);
                    int cbpLuma = mbEncoderI16x16.getCbpLuma(pic, mbX, mbY);

                    int i16x16TypeOffset = (cbpLuma / 15) * 12 + cbpChroma * 4 + predMode;

                    CAVLCWriter.writeUE(sliceData, mbTypeOffset + mbType.code() + i16x16TypeOffset);
                } else {
                    CAVLCWriter.writeUE(sliceData, mbTypeOffset + mbType.code());
                }
                
                BitWriter candidate;
                int qpDelta;
                do {
                    candidate = sliceData.fork();
                    qpDelta = rc.getQpDelta();
                    encodeMacroblock(mbType, pic, mbX, mbY, candidate, outMB, qp, qpDelta);
                } while (!rc.accept(candidate.position() - sliceData.position()));
                sliceData = candidate;
                qp += qpDelta;

                collectPredictors(outMB, mbX);
            }
        }
        sliceData.write1Bit(1);
        sliceData.flush();
        buf = sliceData.getBuffer();
        buf.flip();

        escapeNAL(buf, dup);
    }

    private void encodeMacroblock(MBType mbType, Picture pic, int mbX, int mbY, BitWriter candidate, Picture outMB,
            int qp, int qpDelta) {
        if (mbType == MBType.I_16x16)
            mbEncoderI16x16.encodeMacroblock(pic, mbX, mbY, candidate, outMB, qp + qpDelta, qpDelta);
        else
            throw new RuntimeException("Macroblock of type " + mbType + " is not supported.");
    }

    private MBType selectMBType() {
        return MBType.I_16x16; // _2_2_1;
    }

    private void collectPredictors(Picture outMB, int mbX) {
        System.arraycopy(outMB.getPlaneData(0), 240, topLine[0], mbX << 4, 16);
        System.arraycopy(outMB.getPlaneData(1), 56, topLine[1], mbX << 3, 8);
        System.arraycopy(outMB.getPlaneData(2), 56, topLine[2], mbX << 3, 8);

        copyCol(outMB.getPlaneData(0), 15, 16, leftRow[0]);
        copyCol(outMB.getPlaneData(1), 7, 8, leftRow[1]);
        copyCol(outMB.getPlaneData(2), 7, 8, leftRow[2]);
    }

    private void copyCol(int[] planeData, int off, int stride, int[] out) {
        for (int i = 0; i < out.length; i++) {
            out[i] = planeData[off];
            off += stride;
        }
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[] { ColorSpace.YUV420J };
    }
}