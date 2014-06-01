package org.jcodec.codecs.h264;

import static org.jcodec.codecs.h264.H264Const.BLK_X;
import static org.jcodec.codecs.h264.H264Const.BLK_Y;
import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_LEFT;
import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_TOP;
import static org.jcodec.codecs.h264.H264Utils.escapeNAL;
import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;
import static org.jcodec.common.tools.MathUtil.clamp;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.decode.ChromaPredictionBuilder;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.decode.Intra16x16PredictionBuilder;
import org.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import org.jcodec.codecs.h264.encode.DumbRateControl;
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
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MathUtil;

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

    private CAVLC[] cavlc;
    private int[][] leftRow;
    private int[][] topLine;
    private RateControl rc;
    private int[][] topLeft;
    private MBType[] topMBType;
    private MBType leftMBType;
    private int[] i4x4PredLeft;
    private int[] i4x4PredTop;
    private int mbWidth;

    public H264Encoder() {
        this(new DumbRateControl());
    }

    public H264Encoder(RateControl rc) {
        this.rc = rc;
    }

    public ByteBuffer encodeFrame(Picture pic, ByteBuffer _out) {
        return encodeFrame(pic, _out, true, 0);
    }

    public ByteBuffer encodeFrame(Picture pic, ByteBuffer _out, boolean idr, int poc) {
        ByteBuffer dup = _out.duplicate();

        dup.putInt(0x1);
        new NALUnit(NALUnitType.SPS, 3).write(dup);
        SeqParameterSet sps = initSPS(new Size(pic.getCroppedWidth(), pic.getCroppedHeight()));
        H264Utils.writeSPS(dup, sps);

        dup.putInt(0x1);
        new NALUnit(NALUnitType.PPS, 3).write(dup);
        PictureParameterSet pps = initPPS();
        H264Utils.writePPS(dup, pps);

        mbWidth = sps.pic_width_in_mbs_minus1 + 1;

        leftRow = new int[][] { new int[16], new int[8], new int[8] };
        topLine = new int[][] { new int[mbWidth << 4], new int[mbWidth << 3], new int[mbWidth << 3] };
        topLeft = new int[][] { new int[16], new int[8], new int[8] };

        topMBType = new MBType[mbWidth];

        i4x4PredLeft = new int[4];
        i4x4PredTop = new int[mbWidth << 2];

        encodeSlice(sps, pps, pic, dup, idr, poc);

        dup.flip();
        return dup;
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
            int poc) {
        cavlc = new CAVLC[] { new CAVLC(sps, pps, 2, 2), new CAVLC(sps, pps, 1, 1), new CAVLC(sps, pps, 1, 1) };

        rc.reset();
        int qp = rc.getInitQp();

        dup.putInt(0x1);
        new NALUnit(idr ? NALUnitType.IDR_SLICE : NALUnitType.NON_IDR_SLICE, 2).write(dup);
        SliceHeader sh = new SliceHeader();
        sh.slice_type = SliceType.I;
        if (idr)
            sh.refPicMarkingIDR = new RefPicMarkingIDR(false, false);
        sh.pps = pps;
        sh.sps = sps;
        sh.pic_order_cnt_lsb = poc << 1;

        ByteBuffer buf = ByteBuffer.allocate(pic.getWidth() * pic.getHeight());
        BitWriter sliceData = new BitWriter(buf);
        new SliceHeaderWriter().write(sh, idr, 2, sliceData);

        Picture mbPix = Picture.create(16, 16, ColorSpace.YUV420J);

        for (int mbY = 0; mbY < sps.pic_height_in_map_units_minus1 + 1; mbY++) {
            for (int mbX = 0; mbX < sps.pic_width_in_mbs_minus1 + 1; mbX++) {
                BitWriter candidate;
                int qpDelta;
                do {
                    candidate = sliceData.fork();
                    qpDelta = rc.getQpDelta();
                    // encodeMacroblock16x16(pic, mbX, mbY, candidate, mbPix, qp
                    // + qpDelta, qpDelta);
                    encodeMacroblockNxN(pic, mbX, mbY, candidate, mbPix, qp + qpDelta, qpDelta);
                } while (!rc.accept(candidate.position() - sliceData.position()));
                sliceData = candidate;
                qp += qpDelta;

                collectPredictors(mbPix, mbX);
            }
        }
        sliceData.write1Bit(1);
        sliceData.flush();
        buf = sliceData.getBuffer();
        buf.flip();

        escapeNAL(buf, dup);
    }

    private void collectPredictors(Picture outMB, int mbX) {
        topLeft[0][0] = topLine[0][15];
        topLeft[1][0] = topLine[1][7];
        topLeft[2][0] = topLine[2][7];

        System.arraycopy(outMB.getPlaneData(0), 240, topLine[0], mbX << 4, 16);
        System.arraycopy(outMB.getPlaneData(1), 56, topLine[1], mbX << 3, 8);
        System.arraycopy(outMB.getPlaneData(2), 56, topLine[2], mbX << 3, 8);

        copyCol(outMB.getPlaneData(0), 15, 16, leftRow[0], 0);
        copyCol(outMB.getPlaneData(1), 7, 8, leftRow[1], 0);
        copyCol(outMB.getPlaneData(2), 7, 8, leftRow[2], 0);

        copyCol(outMB.getPlaneData(0), 31, 16, topLeft[0], 1);
        copyCol(outMB.getPlaneData(1), 15, 8, topLeft[1], 1);
        copyCol(outMB.getPlaneData(2), 15, 8, topLeft[2], 1);
    }

    public static void copyCol(int[] planeData, int off, int stride, int[] out, int fi) {
        for (int i = fi; i < out.length; i++) {
            out[i] = planeData[off];
            off += stride;
        }
    }

    private void encodeMacroblock16x16(Picture pic, int mbX, int mbY, BitWriter out, Picture mbPix, int qp, int qpDelta) {
        int chromaMode = mbY * 2 + mbX, lumaMode = new int[] { 2, 1, 0, 3 }[chromaMode];
        CAVLCWriter.writeUE(out, 21 + lumaMode); // I_16x16_<lumaMode>_2_1
        // System.out.println(chromaMode);
        writeChromaMode(out, chromaMode);
        CAVLCWriter.writeSE(out, qpDelta); // MB QP delta

        take(pic, mbPix, mbX, mbY);

        luma(mbX, mbY, out, qp, mbPix, lumaMode);
        chroma(mbX, mbY, out, qp, mbPix, chromaMode, MBType.I_16x16);

        leftMBType = topMBType[mbX] = MBType.I_16x16;
    }

    private void encodeMacroblockNxN(Picture pic, int mbX, int mbY, BitWriter out, Picture mbPix, int qp, int qpDelta) {
        int chromaMode = mbY * 2 + mbX;
        CAVLCWriter.writeUE(out, 0); // I_16x16_<lumaMode>_2_1
        int m = mbX == 0 && mbY == 0 ? 2 : (mbX == 0 ? 0 : 1);
        int[] predMode = new int[] { m, m, m, 8, m, m, 3, 0, m, 7, m, 6, 4, 1, 5, 1 };

        for (int i = 0; i < 16; i++) {
            writePredictionI4x4Block(out, predMode[i], mbX != 0, mbY != 0, leftMBType, topMBType[mbX],
                    H264Const.MB_BLK_OFF_LEFT[i], H264Const.MB_BLK_OFF_TOP[i], mbX);
        }
        writeChromaMode(out, chromaMode);
        CAVLCWriter.writeUE(out, H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR_INV[47]);
        CAVLCWriter.writeSE(out, qpDelta); // MB QP delta

        take(pic, mbPix, mbX, mbY);

        lumaNxN(mbX, mbY, out, qp, mbPix, predMode);
        chroma(mbX, mbY, out, qp, mbPix, chromaMode, MBType.I_NxN);

        leftMBType = topMBType[mbX] = MBType.I_NxN;
    }

    private void lumaNxN(int mbX, int mbY, BitWriter out, int qp, Picture mbPix, int[] predMode) {
        int[] lumaPix = mbPix.getPlaneData(0);
        int[] pred = new int[16];
        int[] ac = new int[16];
        for (int i = 0; i < 16; i++) {
            int blkOffX = MB_BLK_OFF_LEFT[i], blkOffY = MB_BLK_OFF_TOP[i];
            int blkX = blkOffX << 2, blkY = blkOffY << 2;

            int oo = (mbX << 4) + blkX;
            Intra4x4PredictionBuilder.predictWithMode(predMode[i], mbX > 0 || blkOffX > 0, mbY > 0 || blkOffY > 0,
                    blkOffY == 0 && (mbX < mbWidth - 1 || blkOffX < 3), leftRow[0], topLine[0], topLeft[0], oo, blkY,
                    pred);
            subtractPix(lumaPix, pred, ac, blkX, blkY);
            CoeffTransformer.fdct4x4(ac);
            CoeffTransformer.quantizeAC(ac, qp);

            MBType leftMBType2 = blkOffX == 0 ? leftMBType : MBType.I_NxN;
            MBType topMBType2 = blkOffY == 0 ? topMBType[mbX] : MBType.I_NxN;

            cavlc[0].writeACBlock(out, (mbX << 2) + blkOffX, (mbY << 2) + blkOffY, leftMBType2, topMBType2, ac,
                    H264Const.totalZeros16, 0, 16, CoeffTransformer.zigzag4x4);
            CoeffTransformer.dequantizeAC(ac, qp);
            CoeffTransformer.idct4x4(ac);

            topLeft[0][blkY] = topLine[0][oo + 3];

            topLeft[0][blkY + 1] = leftRow[0][blkY] = MathUtil.clip(ac[3] + pred[3], 0, 255);
            topLeft[0][blkY + 2] = leftRow[0][blkY + 1] = MathUtil.clip(ac[7] + pred[7], 0, 255);
            topLeft[0][blkY + 3] = leftRow[0][blkY + 2] = MathUtil.clip(ac[11] + pred[11], 0, 255);
            leftRow[0][blkY + 3] = MathUtil.clip(ac[15] + pred[15], 0, 255);

            topLine[0][oo] = MathUtil.clip(ac[12] + pred[12], 0, 255);
            topLine[0][oo + 1] = MathUtil.clip(ac[13] + pred[13], 0, 255);
            topLine[0][oo + 2] = MathUtil.clip(ac[14] + pred[14], 0, 255);
            topLine[0][oo + 3] = MathUtil.clip(ac[15] + pred[15], 0, 255);
        }
    }

    private void subtractPix(int[] pix, int[] pred, int[] ac, int blkX, int blkY) {
        for (int j = 0, off = (blkY << 4) + blkX; j < 16; j += 4, off += 16) {
            ac[j] = pix[off] - pred[j];
            ac[j + 1] = pix[off + 1] - pred[j + 1];
            ac[j + 2] = pix[off + 2] - pred[j + 2];
            ac[j + 3] = pix[off + 3] - pred[j + 3];
        }
    }

    protected void writePredictionI4x4Block(BitWriter out, int mode, boolean leftAvailable, boolean topAvailable,
            MBType leftMBType, MBType topMBType, int blkOffLeft, int blkOffTop, int mbX) {

        int predMode = 2;
        if ((leftAvailable || blkOffLeft > 0) && (topAvailable || blkOffTop > 0)) {
            int predModeB = topMBType == MBType.I_NxN || blkOffTop > 0 ? i4x4PredTop[(mbX << 2) + blkOffLeft] : 2;
            int predModeA = leftMBType == MBType.I_NxN || blkOffLeft > 0 ? i4x4PredLeft[blkOffTop] : 2;
            predMode = Math.min(predModeB, predModeA);
        }

        i4x4PredTop[(mbX << 2) + blkOffLeft] = i4x4PredLeft[blkOffTop] = mode;

        if (mode != predMode) {
            CAVLCWriter.writeBool(out, false);
            if (mode > predMode)
                mode -= 1;
            CAVLCWriter.writeU(out, mode, 3);
        } else {
            CAVLCWriter.writeBool(out, true);
        }
    }

    private void writeChromaMode(BitWriter out, int chromaMode) {
        CAVLCWriter.writeUE(out, chromaMode); // Chroma prediction mode -- DC
    }

    private void chroma(int mbX, int mbY, BitWriter out, int qp, Picture outMB, int chromaMode, MBType curMBType) {
        int x = mbX << 3;
        int y = mbY << 3;

        int[][][] ac = new int[2][][];
        int[][] pred = new int[2][256];
        int[][] dc = new int[2][];
        for (int i = 0; i < 2; i++) {
            ChromaPredictionBuilder.predictWithMode(pred[i], chromaMode, mbX, mbX != 0, mbY != 0, leftRow[i + 1],
                    topLine[i + 1], topLeft[i + 1]);

            ac[i] = transformChroma(i + 1, qp, outMB, pred[i]);
            dc[i] = extractDC(ac[i]);
            writeDC(i + 1, mbX, mbY, out, qp, mbX << 1, mbY << 1, dc[i]);
        }

        for (int i = 0; i < 2; i++) {
            writeAC(i + 1, mbX, mbY, out, mbX << 1, mbY << 1, ac[i], qp, curMBType);
            restorePlane(dc[i], ac[i], qp);
            putChroma(outMB.getData()[i + 1], i + 1, x, y, ac[i], pred[i]);
        }
    }

    private void luma(int mbX, int mbY, BitWriter out, int qp, Picture outMB, int lumaMode) {
        int x = mbX << 4;
        int y = mbY << 4;
        int[] pred = new int[256];
        Intra16x16PredictionBuilder.predictWithMode(lumaMode, pred, mbX != 0, mbY != 0, leftRow[0], topLine[0],
                topLeft[0], x);
        int[][] ac = transform(outMB, 0, qp, 0, 0, pred);
        int[] dc = extractDC(ac);
        writeDC(0, mbX, mbY, out, qp, mbX << 2, mbY << 2, dc);
        writeAC(0, mbX, mbY, out, mbX << 2, mbY << 2, ac, qp, MBType.I_16x16);

        restorePlane(dc, ac, qp);
        putLuma(outMB.getPlaneData(0), pred, ac, 4);
    }

    private void putChroma(int[] mb, int comp, int x, int y, int[][] ac, int[] pred) {
        putBlk(mb, pred, ac[0], 3, 0, 0);

        putBlk(mb, pred, ac[1], 3, 4, 0);

        putBlk(mb, pred, ac[2], 3, 0, 4);

        putBlk(mb, pred, ac[3], 3, 4, 4);
    }

    private void putLuma(int[] planeData, int[] pred, int[][] ac, int log2stride) {
        for (int blk = 0; blk < ac.length; blk++) {
            putBlk(planeData, pred, ac[blk], log2stride, BLK_X[blk], BLK_Y[blk]);
        }
    }

    public static void putBlk(int[] planeData, int[] pred, int[] block, int log2stride, int blkX, int blkY) {
        int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < 4; line++) {
            planeData[dstOff] = clamp(block[srcOff] + pred[dstOff]);
            planeData[dstOff + 1] = clamp(block[srcOff + 1] + pred[dstOff + 1]);
            planeData[dstOff + 2] = clamp(block[srcOff + 2] + pred[dstOff + 2]);
            planeData[dstOff + 3] = clamp(block[srcOff + 3] + pred[dstOff + 3]);
            srcOff += 4;
            dstOff += stride;
        }
    }

    private void restorePlane(int[] dc, int[][] ac, int qp) {
        if (dc.length == 4) {
            CoeffTransformer.invDC2x2(dc);
            CoeffTransformer.dequantizeDC2x2(dc, qp);
        } else if (dc.length == 8) {
            CoeffTransformer.invDC4x2(dc);
            CoeffTransformer.dequantizeDC4x2(dc, qp);
        } else {
            CoeffTransformer.invDC4x4(dc);
            CoeffTransformer.dequantizeDC4x4(dc, qp);
            reorderDC4x4(dc);
        }
        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.dequantizeAC(ac[i], qp);
            ac[i][0] = dc[i];
            CoeffTransformer.idct4x4(ac[i]);
        }
    }

    private int[] extractDC(int[][] ac) {
        int[] dc = new int[ac.length];
        for (int i = 0; i < ac.length; i++) {
            dc[i] = ac[i][0];
            ac[i][0] = 0;
        }
        return dc;
    }

    private void writeAC(int comp, int mbX, int mbY, BitWriter out, int mbLeftBlk, int mbTopBlk, int[][] ac, int qp, MBType curMBType) {
        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.quantizeAC(ac[i], qp);
            int blkOffX = MB_BLK_OFF_LEFT[i], blkOffY = MB_BLK_OFF_TOP[i];

            MBType leftMBType2 = blkOffX == 0 ? leftMBType : curMBType;
            MBType topMBType2 = blkOffY == 0 ? topMBType[mbX] : curMBType;
            // TODO: calc here
            cavlc[comp].writeACBlock(out, mbLeftBlk + blkOffX, mbTopBlk + blkOffY, leftMBType2, topMBType2, ac[i],
                    H264Const.totalZeros16, 1, 15, CoeffTransformer.zigzag4x4);
        }
    }

    private void writeDC(int comp, int mbX, int mbY, BitWriter out, int qp, int mbLeftBlk, int mbTopBlk, int[] dc) {
        if (dc.length == 4) {
            CoeffTransformer.quantizeDC2x2(dc, qp);
            CoeffTransformer.fvdDC2x2(dc);
            cavlc[comp].writeChrDCBlock(out, dc, H264Const.totalZeros4, 0, dc.length, new int[] { 0, 1, 2, 3 });
        } else if (dc.length == 8) {
            CoeffTransformer.quantizeDC4x2(dc, qp);
            CoeffTransformer.fvdDC4x2(dc);
            cavlc[comp].writeChrDCBlock(out, dc, H264Const.totalZeros8, 0, dc.length, new int[] { 0, 1, 2, 3, 4, 5, 6,
                    7 });
        } else {
            reorderDC4x4(dc);
            CoeffTransformer.quantizeDC4x4(dc, qp);
            CoeffTransformer.fvdDC4x4(dc);
            // TODO: calc here
            cavlc[comp].writeLumaDCBlock(out, mbLeftBlk, mbTopBlk, leftMBType, topMBType[mbX], dc,
                    H264Const.totalZeros16, 0, 16, CoeffTransformer.zigzag4x4);
        }
    }

    private int[][] transformChroma(int comp, int qp, Picture mbPix, int[] pred) {
        int[][] ac = new int[4][16];

        takeResidual(mbPix.getPlaneData(comp), 3, 0, 0, ac[0], pred);
        takeResidual(mbPix.getPlaneData(comp), 3, 4, 0, ac[1], pred);
        takeResidual(mbPix.getPlaneData(comp), 3, 0, 4, ac[2], pred);
        takeResidual(mbPix.getPlaneData(comp), 3, 4, 4, ac[3], pred);

        CoeffTransformer.fdct4x4(ac[0]);
        CoeffTransformer.fdct4x4(ac[1]);
        CoeffTransformer.fdct4x4(ac[2]);
        CoeffTransformer.fdct4x4(ac[3]);

        return ac;
    }

    private int[][] transform(Picture mbPix, int comp, int qp, int cw, int ch, int[] pred) {
        int[][] ac = new int[16 >> (cw + ch)][16];
        for (int i = 0; i < ac.length; i++) {
            int[] coeff = ac[i];
            takeResidual(mbPix.getPlaneData(comp), 4, BLK_X[i], BLK_Y[i], coeff, pred);
            CoeffTransformer.fdct4x4(coeff);
        }
        return ac;
    }

    private void take(Picture pic, Picture mb, int mbX, int mbY) {
        for (int i = 0, logBlkS = 4; i < 3; i++, logBlkS = 3) {
            int planeWidth = pic.getPlaneWidth(i), planeHeight = pic.getPlaneHeight(i), x = mbX << logBlkS, y = mbY << logBlkS, blkS = 1 << logBlkS;
            if (x + blkS < planeWidth && y + blkS < planeHeight)
                takeSafe(pic.getPlaneData(i), planeWidth, planeHeight, x, y, mb.getPlaneData(i), blkS);
            else
                takeUnsafe(pic.getPlaneData(i), planeWidth, planeHeight, x, y, mb.getPlaneData(i), blkS);
        }
    }

    private final void takeSafe(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] out, int outSize) {
        for (int srcOff = y * planeWidth + x, dstOff = 0; dstOff < out.length; srcOff += planeWidth - outSize) {
            for (int j = 0; j < outSize; j += 4, dstOff += 4, srcOff += 4) {
                out[dstOff] = planeData[srcOff];
                out[dstOff + 1] = planeData[srcOff + 1];
                out[dstOff + 2] = planeData[srcOff + 2];
                out[dstOff + 3] = planeData[srcOff + 3];
            }
        }
    }

    private final void takeUnsafe(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
            int outSize) {
        int outOff = 0;

        int i;
        for (i = y; i < Math.min(y + outSize, planeHeight); i++) {
            int off = i * planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + outSize, planeWidth); j++, off++)
                coeff[outOff++] = planeData[off];
            --off;
            for (; j < x + outSize; j++)
                coeff[outOff++] = planeData[off];
        }
        for (; i < y + outSize; i++) {
            int off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + outSize, planeWidth); j++, off++)
                coeff[outOff++] = planeData[off];
            --off;
            for (; j < x + outSize; j++)
                coeff[outOff++] = planeData[off];
        }
    }

    private final void takeResidual(int[] planeData, int log2PlaneWidth, int x, int y, int[] coeff, int[] dc) {
        int planeWidth = 1 << log2PlaneWidth;
        for (int i = 0, srcOff = (y << log2PlaneWidth) + x, dstOff = 0; i < 4; i++, srcOff += planeWidth, dstOff += 4) {
            coeff[dstOff] = planeData[srcOff] - dc[srcOff];
            coeff[dstOff + 1] = planeData[srcOff + 1] - dc[srcOff + 1];
            coeff[dstOff + 2] = planeData[srcOff + 2] - dc[srcOff + 2];
            coeff[dstOff + 3] = planeData[srcOff + 3] - dc[srcOff + 3];
        }
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[] { ColorSpace.YUV420J };
    }
}