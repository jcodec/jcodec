package org.jcodec.codecs.prores;

import static java.lang.Math.min;
import static org.jcodec.codecs.prores.ProresConsts.QMAT_CHROMA_APCH;
import static org.jcodec.codecs.prores.ProresConsts.QMAT_CHROMA_APCN;
import static org.jcodec.codecs.prores.ProresConsts.QMAT_CHROMA_APCO;
import static org.jcodec.codecs.prores.ProresConsts.QMAT_CHROMA_APCS;
import static org.jcodec.codecs.prores.ProresConsts.QMAT_LUMA_APCH;
import static org.jcodec.codecs.prores.ProresConsts.QMAT_LUMA_APCN;
import static org.jcodec.codecs.prores.ProresConsts.QMAT_LUMA_APCO;
import static org.jcodec.codecs.prores.ProresConsts.QMAT_LUMA_APCS;
import static org.jcodec.codecs.prores.ProresConsts.dcCodebooks;
import static org.jcodec.codecs.prores.ProresConsts.firstDCCodebook;
import static org.jcodec.codecs.prores.ProresConsts.interlaced_scan;
import static org.jcodec.codecs.prores.ProresConsts.levCodebooks;
import static org.jcodec.codecs.prores.ProresConsts.progressive_scan;
import static org.jcodec.codecs.prores.ProresConsts.runCodebooks;
import static org.jcodec.common.dct.DCTRef.fdct;
import static org.jcodec.common.model.ColorSpace.YUV422_10;
import static org.jcodec.common.tools.MathUtil.log2;
import gnu.trove.list.array.TShortArrayList;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jcodec.codecs.prores.ProresConsts.PictureHeader;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.OutBits;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.common.tools.ImageOP;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Apple ProRes encoder
 * 
 * @author The JCodec project
 * 
 */
public class ProresEncoder {

    private static final int DEFAULT_SLICE_MB_WIDTH = 8;

    public static enum Profile {
        PROXY(QMAT_LUMA_APCO, QMAT_CHROMA_APCO, "apco", 1000, 4, 8), LT(QMAT_LUMA_APCS, QMAT_CHROMA_APCS, "apcs", 2100,
                1, 9), STANDARD(QMAT_LUMA_APCN, QMAT_CHROMA_APCN, "apcn", 3500, 1, 6), HQ(QMAT_LUMA_APCH,
                QMAT_CHROMA_APCH, "apch", 5400, 1, 6);

        final int[] qmatLuma;
        final int[] qmatChroma;
        final public String fourcc;
        // Per 1024 pixels
        final int bitrate;
        final int firstQp;
        final int lastQp;

        private Profile(int[] qmatLuma, int[] qmatChroma, String fourcc, int bitrate, int firstQp, int lastQp) {
            this.qmatLuma = qmatLuma;
            this.qmatChroma = qmatChroma;
            this.fourcc = fourcc;
            this.bitrate = bitrate;
            this.firstQp = firstQp;
            this.lastQp = lastQp;
        }
    };

    protected Profile profile;
    private int[][] scaledLuma;
    private int[][] scaledChroma;

    public ProresEncoder(Profile profile) {
        this.profile = profile;
        scaledLuma = scaleQMat(profile.qmatLuma, 1, 16);
        scaledChroma = scaleQMat(profile.qmatChroma, 1, 16);
    }

    private int[][] scaleQMat(int[] qmatLuma, int start, int count) {
        int[][] result = new int[count][];
        for (int i = 0; i < count; i++) {
            result[i] = new int[qmatLuma.length];
            for (int j = 0; j < qmatLuma.length; j++)
                result[i][j] = qmatLuma[j] * (i + start);
        }
        return result;
    }

    public static final void writeCodeword(OutBits writer, Codebook codebook, int val) throws IOException {
        int firstExp = ((codebook.switchBits + 1) << codebook.riceOrder);
        if (val >= firstExp) {
            val -= firstExp;
            val += (1 << codebook.expOrder); // Offset to zero

            int exp = log2(val);
            int zeros = exp - codebook.expOrder + codebook.switchBits + 1;
            for (int i = 0; i < zeros; i++)
                writer.write1Bit(0);
            writer.write1Bit(1);
            writer.writeNBit(val, exp);

        } else if (codebook.riceOrder > 0) {
            for (int i = 0; i < (val >> codebook.riceOrder); i++)
                writer.write1Bit(0);
            writer.write1Bit(1);
            writer.writeNBit(val & ((1 << codebook.riceOrder) - 1), codebook.riceOrder);
        } else {
            for (int i = 0; i < val; i++)
                writer.write1Bit(0);
            writer.write1Bit(1);
        }
    }

    private static final int qScale(int[] qMat, int ind, int val) {
        return val / qMat[ind];
    }

    private static final int toGolumb(int val) {
        return (val << 1) ^ (val >> 31);
    }

    private static final int toGolumb(int val, int sign) {
        if (val == 0)
            return 0;
        return (val << 1) + sign;
    }

    private static final int diffSign(int val, int sign) {
        return (val >> 31) ^ sign;
    }

    public static final int isNegative(int val) {
        return ((val >> 31) ^ -1) + 1;
    }

    public static final int getLevel(int val) {
        int sign = (val >> 31);
        return (val ^ sign) - sign;
    }

    static final void writeDCCoeffs(OutBits bits, int[] qMat, int[] in, int blocksPerSlice) throws IOException {
        int prevDc = qScale(qMat, 0, in[0] - 16384);
        writeCodeword(bits, firstDCCodebook, toGolumb(prevDc));

        int code = 5, sign = 0, idx = 64;
        for (int i = 1; i < blocksPerSlice; i++, idx += 64) {
            int newDc = qScale(qMat, 0, in[idx] - 16384);
            int delta = newDc - prevDc;
            int newCode = toGolumb(getLevel(delta), diffSign(delta, sign));
            writeCodeword(bits, dcCodebooks[min(code, 6)], newCode);
            code = newCode;
            sign = delta >> 31;
            prevDc = newDc;
        }
    }

    static final void writeACCoeffs(OutBits bits, int[] qMat, int[] in, int blocksPerSlice, int[] scan, int maxCoeff)
            throws IOException {
        int prevRun = 4;
        int prevLevel = 2;

        int run = 0;
        for (int i = 1; i < maxCoeff; i++) {
            int indp = scan[i];
            for (int j = 0; j < blocksPerSlice; j++) {
                int val = qScale(qMat, indp, in[(j << 6) + indp]);
                if (val == 0)
                    run++;
                else {
                    writeCodeword(bits, runCodebooks[min(prevRun, 15)], run);
                    prevRun = run;
                    run = 0;
                    int level = getLevel(val);
                    writeCodeword(bits, levCodebooks[min(prevLevel, 9)], level - 1);
                    prevLevel = level;
                    bits.write1Bit(isNegative(val));
                }
            }
        }
    }

    static final void encodeOnePlane(OutBits bits, int blocksPerSlice, int[] qMat, int[] scan, int[] in)
            throws IOException {

        writeDCCoeffs(bits, qMat, in, blocksPerSlice);
        writeACCoeffs(bits, qMat, in, blocksPerSlice, scan, 64);
    }

    private void dctOnePlane(int blocksPerSlice, int[] in) {
        for (int i = 0; i < blocksPerSlice; i++) {
            fdct(in, i << 6);
        }
    }

    protected int encodeSlice(DataOutput out, int[][] scaledLuma, int[][] scaledChroma, int[] scan, int sliceMbCount,
            int mbX, int mbY, Picture result, int prevQp, int mbWidth, int mbHeight, boolean unsafe) throws IOException {

        Picture striped = splitSlice(result, mbX, mbY, sliceMbCount, unsafe);
        dctOnePlane(sliceMbCount << 2, striped.getPlaneData(0));
        dctOnePlane(sliceMbCount << 1, striped.getPlaneData(1));
        dctOnePlane(sliceMbCount << 1, striped.getPlaneData(2));

        int est = (sliceMbCount >> 2) * profile.bitrate;
        int low = est - (est >> 3); // 12% bitrate fluctuation
        int high = est + (est >> 3);

        int qp = prevQp;
        byte[][] data = encodeSliceData(scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp);
        if (bits(data) > high && qp < profile.lastQp) {
            do {
                ++qp;
                data = encodeSliceData(scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp);
            } while (bits(data) > high && qp < profile.lastQp);
        } else if (bits(data) < low && qp > profile.firstQp) {
            do {
                --qp;
                data = encodeSliceData(scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp);
            } while (bits(data) < low && qp > profile.firstQp);
        }

        out.write(6 << 3); // hdr size
        out.write(qp); // qscale
        out.writeShort(data[0].length);
        out.writeShort(data[1].length);

        out.write(data[0]);
        out.write(data[1]);
        out.write(data[2]);

        return qp;
    }

    protected int bits(byte[][] data) {
        return (data[0].length + data[1].length + data[2].length) << 3;
    }

    protected static final byte[][] encodeSliceData(int[] qmatLuma, int[] qmatChroma, int[] scan, int sliceMbCount,
            Picture striped, int qp) throws IOException {

        return new byte[][] { onePlane(sliceMbCount << 2, qmatLuma, scan, striped.getPlaneData(0)),
                onePlane(sliceMbCount << 1, qmatChroma, scan, striped.getPlaneData(1)),
                onePlane(sliceMbCount << 1, qmatChroma, scan, striped.getPlaneData(2)) };
    }

    static final byte[] onePlane(int blocksPerSlice, int[] qmatLuma, int[] scan, int[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BitstreamWriter bits = new BitstreamWriter(bytes);
        encodeOnePlane(bits, blocksPerSlice, qmatLuma, scan, data);
        bits.flush();
        return bytes.toByteArray();
    }

    protected byte[][] encodePicture(int[][] scaledLuma, int[][] scaledChroma, int[] scan, Picture picture)
            throws IOException {

        int mbWidth = (picture.getWidth() + 15) >> 4;
        int mbHeight = (picture.getHeight() + 15) >> 4;
        int qp = profile.firstQp;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(buffer);
        TShortArrayList sliceSizes = new TShortArrayList();
        for (int mbY = 0; mbY < mbHeight; mbY++) {
            int mbX = 0;
            int sliceMbCount = DEFAULT_SLICE_MB_WIDTH;
            while (mbX < mbWidth) {
                while (mbWidth - mbX < sliceMbCount)
                    sliceMbCount >>= 1;

                int sliceStart = buffer.size();
                boolean unsafeBottom = (picture.getHeight() % 16) != 0 && mbY == mbHeight - 1;
                boolean unsafeRight = (picture.getWidth() % 16) != 0 && mbX + sliceMbCount == mbWidth;
                qp = encodeSlice(dout, scaledLuma, scaledChroma, scan, sliceMbCount, mbX, mbY, picture, qp, mbWidth,
                        mbHeight, unsafeBottom || unsafeRight);
                sliceSizes.add((short) (buffer.size() - sliceStart));

                mbX += sliceMbCount;
            }
        }

        ByteArrayOutputStream header = new ByteArrayOutputStream();
        writePictureHeader(new PictureHeader(log2(DEFAULT_SLICE_MB_WIDTH), sliceSizes.toArray()), new DataOutputStream(
                header));

        return new byte[][] { header.toByteArray(), buffer.toByteArray() };
    }

    public static void writePictureHeader(PictureHeader ph, DataOutput out) throws IOException {
        int headerLen = 8;
        out.write(headerLen << 3);
        out.writeInt(0);
        out.writeShort(ph.sliceSizes.length);

        out.writeByte(ph.log2SliceMbWidth << 4);

        for (int size : ph.sliceSizes) {
            out.writeShort(size);
        }
    }

    private Picture splitSlice(Picture result, int mbX, int mbY, int sliceMbCount, boolean unsafe) {
        Picture out = Picture.create(sliceMbCount << 4, 16, YUV422_10);
        if (unsafe) {
            Picture filled = Picture.create(sliceMbCount << 4, 16, YUV422_10);
            ImageOP.subImageWithFill(result, filled, new Rect(mbX << 4, mbY << 4, sliceMbCount << 4, 16));

            split(filled, out, 0, 0, sliceMbCount);
        } else {
            split(result, out, mbX, mbY, sliceMbCount);
        }

        return out;
    }

    private void split(Picture in, Picture out, int mbX, int mbY, int sliceMbCount) {

        split(in.getPlaneData(0), out.getPlaneData(0), in.getPlaneWidth(0), mbX, mbY, sliceMbCount, 0);
        split(in.getPlaneData(1), out.getPlaneData(1), in.getPlaneWidth(1), mbX, mbY, sliceMbCount, 1);
        split(in.getPlaneData(2), out.getPlaneData(2), in.getPlaneWidth(2), mbX, mbY, sliceMbCount, 1);

    }

    private int[] split(int[] in, int[] out, int stride, int mbX, int mbY, int sliceMbCount, int chroma) {
        int outOff = 0;
        int off = (mbY << 4) * stride + (mbX << (4 - chroma));

        for (int i = 0; i < sliceMbCount; i++) {
            splitBlock(in, stride, off, out, outOff);
            splitBlock(in, stride, off + (stride << 3), out, outOff + (128 >> chroma));

            if (chroma == 0) {
                splitBlock(in, stride, off + 8, out, outOff + 64);
                splitBlock(in, stride, off + (stride << 3) + 8, out, outOff + 192);
            }

            outOff += (256 >> chroma);
            off += (16 >> chroma);
        }

        return out;
    }

    private void splitBlock(int[] y, int stride, int off, int[] out, int outOff) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++)
                out[outOff++] = y[off++];
            off += stride - 8;
        }
    }

    public void encodeFrame(OutputStream out, Picture... pics) throws IOException {

        int[] scan = pics.length > 1 ? interlaced_scan : progressive_scan;
        byte[][] picture1 = encodePicture(scaledLuma, scaledChroma, scan, pics[0]);
        byte[][] picture2 = pics.length == 1 ? null : encodePicture(scaledLuma, scaledChroma, scan, pics[1]);

        DataOutput outp = new DataOutputStream(out);

        int payloadSize = picture1[0].length + picture1[1].length
                + (picture2 != null ? picture2[0].length + picture2[1].length : 0);
        writeFrameHeader(outp, new ProresConsts.FrameHeader(payloadSize, pics[0].getWidth(), pics[0].getHeight()
                * pics.length, pics.length == 1 ? 0 : 1, true, scan, profile.qmatLuma, profile.qmatChroma));

        out.write(picture1[0]);
        out.write(picture1[1]);

        if (picture2 != null) {
            out.write(picture2[0]);
            out.write(picture2[1]);
        }
    }

    public static void writeFrameHeader(DataOutput outp, ProresConsts.FrameHeader header) throws IOException {

        int headerSize = 148;
        outp.writeInt(headerSize + 8 + header.payloadSize);
        outp.write(new byte[] { 'i', 'c', 'p', 'f' });

        outp.writeShort(headerSize); // header size
        outp.writeShort(0);

        outp.write(new byte[] { 'a', 'p', 'l', '0' });

        outp.writeShort(header.width);
        outp.writeShort(header.height);

        outp.write(header.frameType == 0 ? 0x83 : 0x87); // {10}(422){00}[{00}(frame),{01}(field)}{11}

        outp.write(new byte[] { 0, 2, 2, 6, 32, 0 });

        outp.write(3); // flags2
        writeQMat(outp, header.qMatLuma);
        writeQMat(outp, header.qMatChroma);
    }

    static final void writeQMat(DataOutput out, int[] qmat) throws IOException {
        for (int i = 0; i < 64; i++)
            out.write(qmat[i]);
    }
}