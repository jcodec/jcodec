package org.jcodec.codecs.vpx;

import static org.jcodec.common.tools.MathUtil.clamp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jcodec.codecs.common.biari.VPxBooleanEncoder;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VP8Encoder implements VideoEncoder {

    private VPXBitstream bitstream;
    private int[][] leftRow;
    private int[][] topLine;
    private VPXQuantizer quantizer;
    private int[] tmp = new int[16];
    private int[][] topLeft;

    public ByteBuffer encodeFrame(Picture pic, ByteBuffer _buf) {
        ByteBuffer out = _buf.duplicate();
        int mbWidth = ((pic.getWidth() + 15) >> 4);
        int mbHeight = ((pic.getHeight() + 15) >> 4);

        bitstream = new VPXBitstream(VPXConst.tokenDefaultBinProbs, mbWidth);
        leftRow = new int[][] { new int[16], new int[8], new int[8] };
        topLeft = new int[][] { new int[16], new int[8], new int[8] };
        topLine = new int[][] { new int[mbWidth << 4], new int[mbWidth << 3], new int[mbWidth << 3] };
        initValue(leftRow, 129);
        initValue(topLine, 127);

        quantizer = new VPXQuantizer();

        Picture mbPix = Picture.create(16, 16, ColorSpace.YUV420);

        int qp = 20;

        out.order(ByteOrder.LITTLE_ENDIAN);
        NIOUtils.skip(out, 3);
        writeHeaderWH(out, pic.getWidth(), pic.getHeight());

        int start = out.position();
        VPxBooleanEncoder boolEnc = new VPxBooleanEncoder(out);

        writeHeaderKey(boolEnc, qp);

        // MB modes
        for (int mbY = 0; mbY < mbHeight; mbY++) {
            for (int mbX = 0; mbX < mbWidth; mbX++) {
                // Luma mode DC
                boolEnc.writeBit(145, 1);
                boolEnc.writeBit(156, 0);
                boolEnc.writeBit(163, 0);

                // Chroma mode DC
                boolEnc.writeBit(142, 0);
            }
        }
        boolEnc.stop();
        int firstPart = out.position() - start;
        boolEnc = new VPxBooleanEncoder(out);

        // MB residuals
        for (int mbY = 0; mbY < mbHeight; mbY++) {
            initValue(leftRow, 129);

            for (int mbX = 0; mbX < mbWidth; mbX++) {
                take(pic, mbPix, mbX, mbY);

                luma(mbX, mbY, boolEnc, qp, mbPix);
                chroma(mbX, mbY, boolEnc, qp, mbPix);

                collectPredictors(mbPix, mbX);
            }
        }
        boolEnc.stop();
        out.flip();
        writeHeader(out.duplicate(), firstPart, true, 0);

        return out;
    }

    private void initValue(int[][] leftRow2, int val) {
        Arrays.fill(leftRow2[0], val);
        Arrays.fill(leftRow2[1], val);
        Arrays.fill(leftRow2[2], val);
    }

    public static void writeHeader(ByteBuffer duplicate, int firstPart, boolean keyFrame, int version) {
        int type = keyFrame ? 0 : 1, showFrame = 1;
        int header = (firstPart << 5) | (showFrame << 4) | (version << 1) | type;

        duplicate.put((byte) (header & 0xff));
        duplicate.put((byte) ((header >> 8) & 0xff));
        duplicate.put((byte) ((header >> 16) & 0xff));
    }

    public static void writeHeaderKey(VPxBooleanEncoder boolEnc, int qp) {
        boolEnc.writeBit(128, 0); // clr_type
        boolEnc.writeBit(128, 0); // clamp_type
        boolEnc.writeBit(128, 0); // segmentation enabled
        boolEnc.writeBit(128, 0); // filter type
        writeInt(boolEnc, 1, 6); // filter level
        writeInt(boolEnc, 0, 3); // sharpness level
        boolEnc.writeBit(128, 0); // deltas enabled
        writeInt(boolEnc, 0, 2); // partition type
        writeInt(boolEnc, qp, 7);
        boolEnc.writeBit(128, 0); // y1dc_delta_q
        boolEnc.writeBit(128, 0); // y2dc_delta_q
        boolEnc.writeBit(128, 0); // y2ac_delta_q
        boolEnc.writeBit(128, 0); // uvdc_delta_q
        boolEnc.writeBit(128, 0); // uvac_delta_q
        boolEnc.writeBit(128, 0); // refresh entropy probs

        int[][][][] probFlags = VPXConst.tokenProbUpdateFlagProbs;
        for (int i = 0; i < probFlags.length; i++) {
            for (int j = 0; j < probFlags[i].length; j++) {
                for (int k = 0; k < probFlags[i][j].length; k++) {
                    for (int l = 0; l < probFlags[i][j][k].length; l++)
                        boolEnc.writeBit(probFlags[i][j][k][l], 0);
                }
            }
        }

        boolEnc.writeBit(128, 0); // mb_no_coeff_skip
    }

    public static void writeInt(VPxBooleanEncoder boolEnc, int data, int bits) {
        int bit;

        for (bit = bits - 1; bit >= 0; bit--)
            boolEnc.writeBit(128, (1 & (data >> bit)));
    }

    public static void writeHeaderWH(ByteBuffer out, int width, int height) {
        out.put((byte) 0x9d);
        out.put((byte) 0x01);
        out.put((byte) 0x2a);

        out.putShort((short) width);
        out.putShort((short) height);
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

    private void luma(int mbX, int mbY, VPxBooleanEncoder out, int qp, Picture mb) {
        int x = mbX << 4;
        int y = mbY << 4;
        int[] pred = new int[256];
        int lumaPred = VPXConst.DC_PRED;
        VPXPred.pred816(lumaPred, x, y, leftRow[0], topLine[0], topLeft[0], pred, 4);
        int[][] ac = transform(mb, qp, pred);
        int[] dc = extractDC(ac);
        writeLumaDC(mbX, mbY, out, qp, dc);
        writeLumaAC(mbX, mbY, out, ac, qp);

        restorePlaneLuma(dc, ac, qp);
        putLuma(mb.getPlaneData(0), pred, ac, 4);
    }

    private void writeLumaAC(int mbX, int mbY, VPxBooleanEncoder out, int[][] ac, int qp) {
        for (int i = 0; i < 16; i++) {
            quantizer.quantizeY(ac[i], qp);
            bitstream.encodeCoeffsDCT15(out, zigzag(ac[i], tmp), mbX, i & 3, i >> 2);
        }
    }

    private void writeLumaDC(int mbX, int mbY, VPxBooleanEncoder out, int qp, int[] dc) {
        VPXDCT.walsh4x4(dc);
        quantizer.quantizeY2(dc, qp);
        bitstream.encodeCoeffsWHT(out, zigzag(dc, tmp), mbX);
    }

    private void writeChroma(int comp, int mbX, int mbY, VPxBooleanEncoder boolEnc, int[][] ac, int qp) {
        for (int i = 0; i < 4; i++) {
            quantizer.quantizeUV(ac[i], qp);
            bitstream.encodeCoeffsDCTUV(boolEnc, zigzag(ac[i], tmp), comp, mbX, i & 1, i >> 1);
        }
    }

    public static int[] zigzag(int[] zz, int[] tmp2) {
        for (int i = 0; i < 16; i++)
            tmp2[i] = zz[VPXConst.zigzag[i]];
        return tmp2;
    }

    private void chroma(int mbX, int mbY, VPxBooleanEncoder boolEnc, int qp, Picture mbPix) {
        int x = mbX << 3;
        int y = mbY << 3;
        int chromaMode = VPXConst.DC_PRED;
        for (int c = 0; c < 2; c++) {
            int[] pred = new int[64];
            VPXPred.pred816(chromaMode, x, y, leftRow[c + 1], topLine[c + 1], topLeft[c + 1], pred, 3);
            int[][] ac = transformChroma(c + 1, qp, mbPix, pred);

            writeChroma(c + 1, mbX, mbY, boolEnc, ac, qp);

            restorePlaneChroma(ac, qp);
            putChroma(mbPix.getData()[c + 1], c + 1, x, y, ac, pred);
        }
    }

    private int[][] transformChroma(int comp, int qp, Picture mbPix, int[] chromaPred) {
        int[][] ac = new int[4][16];

        for (int blk = 0; blk < ac.length; blk++) {

            takeResidual(mbPix.getPlaneData(comp), 3, (blk & 1) << 2, (blk >> 1) << 2, ac[blk], chromaPred);
            VPXDCT.fdct4x4(ac[blk]);
        }

        return ac;
    }

    private void putChroma(int[] mb, int comp, int x, int y, int[][] ac, int chromaPred[]) {
        for (int blk = 0; blk < 4; blk++)
            putBlk(mb, chromaPred, 8, ac[blk], 3, (blk & 1) << 2, (blk >> 1) << 2);
    }

    private void putLuma(int[] planeData, int[] pred, int[][] ac, int log2stride) {
        for (int blk = 0; blk < ac.length; blk++) {
            int blkOffX = (blk & 3) << 2;
            int blkOffY = blk & ~3;
            putBlk(planeData, pred, 1 << log2stride, ac[blk], log2stride, blkOffX, blkOffY);
        }
    }

    public static void putBlk(int[] planeData, int[] pred, int predStride, int[] block, int log2stride, int blkX,
            int blkY) {
        int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX, predOff = blkY * predStride + blkX; line < 4; line++) {
            planeData[dstOff] = clamp(block[srcOff] + pred[predOff]);
            planeData[dstOff + 1] = clamp(block[srcOff + 1] + pred[predOff + 1]);
            planeData[dstOff + 2] = clamp(block[srcOff + 2] + pred[predOff + 2]);
            planeData[dstOff + 3] = clamp(block[srcOff + 3] + pred[predOff + 3]);
            srcOff += 4;
            dstOff += stride;
            predOff += predStride;
        }
    }

    private void restorePlaneChroma(int[][] ac, int qp) {
        for (int i = 0; i < 4; i++) {
            quantizer.dequantizeUV(ac[i], qp);
            VPXDCT.idct4x4(ac[i]);
        }
    }

    private void restorePlaneLuma(int[] dc, int[][] ac, int qp) {
        quantizer.dequantizeY2(dc, qp);
        VPXDCT.iwalsh4x4(dc);
        for (int i = 0; i < 16; i++) {
            quantizer.dequantizeY(ac[i], qp);
            ac[i][0] = dc[i];
            VPXDCT.idct4x4(ac[i]);
        }
    }

    private int[] extractDC(int[][] ac) {
        int[] dc = new int[ac.length];
        for (int i = 0; i < ac.length; i++) {
            dc[i] = ac[i][0];
        }
        return dc;
    }

    private int[][] transform(Picture mb, int qp, int[] pred) {
        int[][] ac = new int[16][16];
        for (int i = 0; i < ac.length; i++) {
            int[] coeff = ac[i];
            takeResidual(mb.getPlaneData(0), 4, (i & 3) << 2, i & ~3, coeff, pred);
            VPXDCT.fdct4x4(coeff);
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