package org.jcodec.codecs.vpx;

import static java.lang.System.arraycopy;
import static org.jcodec.common.tools.MathUtil.clip;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jcodec.codecs.common.biari.VPxBooleanEncoder;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VP8Encoder extends VideoEncoder {

    private VPXBitstream bitstream;
    private int[][] leftRow;
    private int[][] topLine;
    private VPXQuantizer quantizer;
    private int[] tmp = new int[16];
    private RateControl rc;

    private ByteBuffer headerBuffer;
    private ByteBuffer dataBuffer;

    public VP8Encoder(int qp) {
        this(new NopRateControl(qp));
    }

    public VP8Encoder(RateControl rc) {
        this.rc = rc;
    }

    public ByteBuffer encodeFrame(Picture pic, ByteBuffer _buf) {
        ByteBuffer out = _buf.duplicate();
        int mbWidth = ((pic.getWidth() + 15) >> 4);
        int mbHeight = ((pic.getHeight() + 15) >> 4);

        prepareBuffers(mbWidth, mbHeight);

        bitstream = new VPXBitstream(VPXConst.tokenDefaultBinProbs, mbWidth);
        leftRow = new int[][] { new int[16], new int[8], new int[8] };
        topLine = new int[][] { new int[mbWidth << 4], new int[mbWidth << 3], new int[mbWidth << 3] };
        initValue(leftRow, 129);
        initValue(topLine, 127);

        quantizer = new VPXQuantizer();

        Picture outMB = Picture.create(16, 16, ColorSpace.YUV420);

        int[] segmentQps = rc.getSegmentQps();

        VPxBooleanEncoder boolEnc = new VPxBooleanEncoder(dataBuffer);

        int[] segmentMap = new int[mbWidth * mbHeight];

        // MB residuals
        for (int mbY = 0, mbAddr = 0; mbY < mbHeight; mbY++) {
            initValue(leftRow, 129);

            for (int mbX = 0; mbX < mbWidth; mbX++, mbAddr++) {

                int before = boolEnc.position();
                int segment = rc.getSegment();
                segmentMap[mbAddr] = segment;

                luma(pic, mbX, mbY, boolEnc, segmentQps[segment], outMB);
                chroma(pic, mbX, mbY, boolEnc, segmentQps[segment], outMB);

                rc.report(boolEnc.position() - before);

                collectPredictors(outMB, mbX);
            }
        }
        boolEnc.stop();
        dataBuffer.flip();

        boolEnc = new VPxBooleanEncoder(headerBuffer);
        int[] probs = calcSegmentProbs(segmentMap);

        writeHeader2(boolEnc, segmentQps, probs);

        // MB modes
        for (int mbY = 0, mbAddr = 0; mbY < mbHeight; mbY++) {
            for (int mbX = 0; mbX < mbWidth; mbX++, mbAddr++) {
                writeSegmetId(boolEnc, segmentMap[mbAddr], probs);

                // Luma mode DC
                boolEnc.writeBit(145, 1);
                boolEnc.writeBit(156, 0);
                boolEnc.writeBit(163, 0);

                // Chroma mode DC
                boolEnc.writeBit(142, 0);
            }
        }
        boolEnc.stop();
        headerBuffer.flip();

        out.order(ByteOrder.LITTLE_ENDIAN);
        writeHeader(out, pic.getWidth(), pic.getHeight(), headerBuffer.remaining());
        out.put(headerBuffer);
        out.put(dataBuffer);

        out.flip();

        return out;
    }

    private void prepareBuffers(int mbWidth, int mbHeight) {
        int dataBufSize = (mbHeight * mbHeight) << 10;
        int headerBufSize = 256 + mbWidth * mbHeight;

        if (headerBuffer == null || headerBuffer.capacity() < headerBufSize)
            headerBuffer = ByteBuffer.allocate(headerBufSize);
        else
            headerBuffer.clear();

        if (dataBuffer == null || dataBuffer.capacity() < dataBufSize)
            dataBuffer = ByteBuffer.allocate(dataBufSize);
        else
            dataBuffer.clear();
    }

    private void writeSegmetId(VPxBooleanEncoder boolEnc, int id, int[] probs) {
        int bit1 = (id >> 1) & 1;
        boolEnc.writeBit(probs[0], bit1);
        boolEnc.writeBit(probs[1 + bit1], id & 1);
    }

    private int[] calcSegmentProbs(int[] segmentMap) {
        int[] result = new int[3];
        for (int i = 0; i < segmentMap.length; i++) {
            switch (segmentMap[i]) {
            case 0:
                result[0]++;
                result[1]++;
                break;
            case 1:
                result[0]++;
                break;
            case 2:
                result[2]++;
            }
        }
        for (int i = 0; i < 3; i++)
            result[i] = MathUtil.clip((result[i] << 8) / segmentMap.length, 1, 255);

        return result;
    }

    private void initValue(int[][] leftRow2, int val) {
        Arrays.fill(leftRow2[0], val);
        Arrays.fill(leftRow2[1], val);
        Arrays.fill(leftRow2[2], val);
    }

    private void writeHeader2(VPxBooleanEncoder boolEnc, int[] segmentQps, int[] probs) {
        boolEnc.writeBit(128, 0); // clr_type
        boolEnc.writeBit(128, 0); // clamp_type
        boolEnc.writeBit(128, 1); // segmentation enabled

        boolEnc.writeBit(128, 1); // update_mb_segmentation_map
        boolEnc.writeBit(128, 1); // update_segment_feature_data

        boolEnc.writeBit(128, 1); // segment_feature_mode - absolute

        for (int i = 0; i < segmentQps.length; i++) {
            boolEnc.writeBit(128, 1); // quantizer_update
            writeInt(boolEnc, segmentQps[i], 7); // quantizer_update_value
            boolEnc.writeBit(128, 0);
        }
        for (int i = segmentQps.length; i < 4; i++)
            boolEnc.writeBit(128, 0); // quantizer_update

        boolEnc.writeBit(128, 0); // loop_filter_update
        boolEnc.writeBit(128, 0); // loop_filter_update
        boolEnc.writeBit(128, 0); // loop_filter_update
        boolEnc.writeBit(128, 0); // loop_filter_update

        for (int i = 0; i < 3; i++) {
            boolEnc.writeBit(128, 1); // segment_prob_update
            writeInt(boolEnc, probs[i], 8);
        }

        boolEnc.writeBit(128, 0); // filter type
        writeInt(boolEnc, 1, 6); // filter level
        writeInt(boolEnc, 0, 3); // sharpness level
        boolEnc.writeBit(128, 0); // deltas enabled
        writeInt(boolEnc, 0, 2); // partition type
        writeInt(boolEnc, segmentQps[0], 7);
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

    void writeInt(VPxBooleanEncoder boolEnc, int data, int bits) {
        int bit;

        for (bit = bits - 1; bit >= 0; bit--)
            boolEnc.writeBit(128, (1 & (data >> bit)));
    }

    private void writeHeader(ByteBuffer out, int width, int height, int firstPart) {
        int version = 0, type = 0, showFrame = 1;
        int header = (firstPart << 5) | (showFrame << 4) | (version << 1) | type;

        out.put((byte) (header & 0xff));
        out.put((byte) ((header >> 8) & 0xff));
        out.put((byte) ((header >> 16) & 0xff));

        out.put((byte) 0x9d);
        out.put((byte) 0x01);
        out.put((byte) 0x2a);

        out.putShort((short) width);
        out.putShort((short) height);
    }

    private void collectPredictors(Picture outMB, int mbX) {
        arraycopy(outMB.getPlaneData(0), 240, topLine[0], mbX << 4, 16);
        arraycopy(outMB.getPlaneData(1), 56, topLine[1], mbX << 3, 8);
        arraycopy(outMB.getPlaneData(2), 56, topLine[2], mbX << 3, 8);

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

    private void luma(Picture pic, int mbX, int mbY, VPxBooleanEncoder out, int qp, Picture outMB) {
        int x = mbX << 4;
        int y = mbY << 4;
        int[][] ac = transform(pic, 0, qp, x, y);
        int[] dc = extractDC(ac);
        writeLumaDC(mbX, mbY, out, qp, dc);
        writeLumaAC(mbX, mbY, out, ac, qp);

        restorePlaneLuma(dc, ac, qp);
        putLuma(outMB.getPlaneData(0), lumaDCPred(x, y), ac, 4);
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

    private int[] zigzag(int[] zz, int[] tmp2) {
        for (int i = 0; i < 16; i++)
            tmp2[i] = zz[VPXConst.zigzag[i]];
        return tmp2;
    }

    private void chroma(Picture pic, int mbX, int mbY, VPxBooleanEncoder boolEnc, int qp, Picture outMB) {
        int x = mbX << 3;
        int y = mbY << 3;
        int chromaPred1 = chromaPredBlk(1, x, y);
        int chromaPred2 = chromaPredBlk(2, x, y);

        int[][] ac1 = transformChroma(pic, 1, qp, x, y, outMB, chromaPred1);
        int[][] ac2 = transformChroma(pic, 2, qp, x, y, outMB, chromaPred2);

        writeChroma(1, mbX, mbY, boolEnc, ac1, qp);
        writeChroma(2, mbX, mbY, boolEnc, ac2, qp);

        restorePlaneChroma(ac1, qp);
        putChroma(outMB.getData()[1], 1, x, y, ac1, chromaPred1);
        restorePlaneChroma(ac2, qp);
        putChroma(outMB.getData()[2], 2, x, y, ac2, chromaPred2);
    }

    private int[][] transformChroma(Picture pic, int comp, int qp, int x, int y, Picture outMB, int chromaPred) {
        int[][] ac = new int[4][16];

        for (int blk = 0; blk < ac.length; blk++) {

            int blkOffX = (blk & 1) << 2;
            int blkOffY = (blk >> 1) << 2;
            takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + blkOffX, y
                    + blkOffY, ac[blk], chromaPred);
            VPXDCT.fdct4x4(ac[blk]);
        }

        return ac;
    }

    private void putChroma(int[] mb, int comp, int x, int y, int[][] ac, int chromaPred) {
        for (int blk = 0; blk < 4; blk++)
            putBlk(mb, chromaPred, ac[blk], 3, (blk & 1) << 2, (blk >> 1) << 2);
    }

    private final int chromaPredOne(int[] pix, int x) {
        return (pix[x] + pix[x + 1] + pix[x + 2] + pix[x + 3] + pix[x + 4] + pix[x + 5] + pix[x + 6] + pix[x + 7] + 4) >> 3;
    }

    private final int chromaPredTwo(int[] pix1, int[] pix2, int x, int y) {
        return (pix1[x] + pix1[x + 1] + pix1[x + 2] + pix1[x + 3] + pix1[x + 4] + pix1[x + 5] + pix1[x + 6]
                + pix1[x + 7] + pix2[y] + pix2[y + 1] + pix2[y + 2] + pix2[y + 3] + pix2[y + 4] + pix2[y + 5]
                + pix2[y + 6] + pix2[y + 7] + 8) >> 4;
    }

    private int chromaPredBlk(int comp, int x, int y) {
        int predY = y & 0x7;
        if (x != 0 && y != 0)
            return chromaPredTwo(leftRow[comp], topLine[comp], predY, x);
        else if (x != 0)
            return chromaPredOne(leftRow[comp], predY);
        else if (y != 0)
            return chromaPredOne(topLine[comp], x);
        else
            return 128;
    }

    private void putLuma(int[] planeData, int pred, int[][] ac, int log2stride) {
        for (int blk = 0; blk < ac.length; blk++) {
            int blkOffX = (blk & 3) << 2;
            int blkOffY = blk & ~3;
            putBlk(planeData, pred, ac[blk], log2stride, blkOffX, blkOffY);
        }
    }

    private void putBlk(int[] planeData, int pred, int[] block, int log2stride, int blkX, int blkY) {
        int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < 4; line++) {
            planeData[dstOff] = clip(block[srcOff] + pred, 0, 255);
            planeData[dstOff + 1] = clip(block[srcOff + 1] + pred, 0, 255);
            planeData[dstOff + 2] = clip(block[srcOff + 2] + pred, 0, 255);
            planeData[dstOff + 3] = clip(block[srcOff + 3] + pred, 0, 255);
            srcOff += 4;
            dstOff += stride;
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

    private int lumaDCPred(int x, int y) {
        if (x == 0 && y == 0)
            return 128;

        if (y == 0)
            return (ArrayUtil.sum(leftRow[0]) + 8) >> 4;
        if (x == 0)
            return (ArrayUtil.sum(topLine[0], x, 16) + 8) >> 4;

        return (ArrayUtil.sum(leftRow[0]) + ArrayUtil.sum(topLine[0], x, 16) + 16) >> 5;
    }

    private int[][] transform(Picture pic, int comp, int qp, int x, int y) {
        int dcc = lumaDCPred(x, y);

        int[][] ac = new int[16][16];
        for (int i = 0; i < ac.length; i++) {
            int[] coeff = ac[i];
            int blkOffX = (i & 3) << 2;
            int blkOffY = i & ~3;
            takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + blkOffX, y
                    + blkOffY, coeff, dcc);
            VPXDCT.fdct4x4(coeff);
        }
        return ac;
    }

    private final void takeSubtract(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff, int dc) {
        if (x + 4 < planeWidth && y + 4 < planeHeight)
            takeSubtractSafe(planeData, planeWidth, planeHeight, x, y, coeff, dc);
        else
            takeSubtractUnsafe(planeData, planeWidth, planeHeight, x, y, coeff, dc);

    }

    private final void takeSubtractSafe(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
            int dc) {
        for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < 4; i++, srcOff += planeWidth, dstOff += 4) {
            coeff[dstOff] = planeData[srcOff] - dc;
            coeff[dstOff + 1] = planeData[srcOff + 1] - dc;
            coeff[dstOff + 2] = planeData[srcOff + 2] - dc;
            coeff[dstOff + 3] = planeData[srcOff + 3] - dc;
        }
    }

    private final void takeSubtractUnsafe(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
            int dc) {
        int outOff = 0;

        int i;
        for (i = y; i < Math.min(y + 4, planeHeight); i++) {
            int off = i * planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + 4, planeWidth); j++)
                coeff[outOff++] = planeData[off++] - dc;
            --off;
            for (; j < x + 4; j++)
                coeff[outOff++] = planeData[off] - dc;
        }
        for (; i < y + 4; i++) {
            int off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + 4, planeWidth); j++)
                coeff[outOff++] = planeData[off++] - dc;
            --off;
            for (; j < x + 4; j++)
                coeff[outOff++] = planeData[off] - dc;
        }
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[] { ColorSpace.YUV420J };
    }

    @Override
    public ByteBuffer encodeFrame8Bit(Picture8Bit pic, ByteBuffer _out) {
        throw new RuntimeException("Unsupported");
    }
}