package org.jcodec.codecs.prores;

import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static org.jcodec.codecs.prores.ProresConsts.dcCodebooks;
import static org.jcodec.codecs.prores.ProresConsts.firstDCCodebook;
import static org.jcodec.codecs.prores.ProresConsts.interlaced_scan;
import static org.jcodec.codecs.prores.ProresConsts.levCodebooks;
import static org.jcodec.codecs.prores.ProresConsts.progressive_scan;
import static org.jcodec.codecs.prores.ProresConsts.runCodebooks;
import static org.jcodec.common.dct.SimpleIDCT10Bit.idct10;
import static org.jcodec.common.tools.MathUtil.log2;

import java.io.DataInput;
import java.io.IOException;

import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.codecs.prores.ProresConsts.PictureHeader;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.BitstreamReaderBB;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.InBits;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decoder for Apple ProRes format
 * 
 * As posted at http://git.videolan.org/?p=ffmpeg.git;a=commitdiff;h=5554d
 * e13b29b9bb812ee5cfd606349873ddf0945
 * 
 * @author The JCodec project
 * 
 */
public class ProresDecoder implements VideoDecoder {

    public ProresDecoder() {
    }

    static final int[] table = new int[] { 7, 6, 5, 5, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    static final int[] mask = new int[] { 0, 0, 0, 0, 0, 0, 0, -1 };

    public static final int nZeros(int check14Bit) {
        int low = table[check14Bit & 0x7f];
        check14Bit >>= 7;
        int high = table[check14Bit & 0x7f];
        return high + (mask[high & 0x7] & low);
    }

    public static final int readCodeword(InBits reader, Codebook codebook) throws IOException {
        int q = nZeros(reader.checkNBit(14));

        int val;
        if (q > codebook.switchBits) {
            int bits = codebook.expOrder - codebook.switchBits + (q << 1);
            // if (bits + q + 1 > 32) {
            // throw new RuntimeException("Codeword damaged");
            // }
            val = (int) reader.readNBit(bits); // Read value
            val -= (1 << codebook.expOrder); // Offset to zero
            // Offset to next number after max rice
            val += ((codebook.switchBits + 1) << codebook.riceOrder);
        } else if (codebook.riceOrder > 0) {
            reader.skip(q + 1);
            val = (q << codebook.riceOrder) + reader.readNBit(codebook.riceOrder);
        } else {
            reader.skip(q + 1);
            val = q;
        }
        return val;
    }

    public static final int golumbToSigned(int val) {
        return (val >> 1) ^ golumbSign(val);
    }

    private static final int golumbSign(int val) {
        return -(val & 1);
    }

    public static final int toSigned(int val, int sign) {
        return (val ^ sign) - sign;
    }

    static final void readDCCoeffs(InBits bits, int[] qMat, int[] out, int blocksPerSlice) throws IOException {
        int c = readCodeword(bits, firstDCCodebook);
        if (c < 0) {
            return;
            // throw new RuntimeException("DC tex damaged");
        }
        int prevDc = golumbToSigned(c);
        out[0] = 4096 + qScale(qMat, 0, prevDc);

        int code = 5, sign = 0, idx = 64;
        for (int i = 1; i < blocksPerSlice; i++, idx += 64) {
            code = readCodeword(bits, dcCodebooks[min(code, 6)]);
            if (code < 0) {
                return;
                // throw new RuntimeException("DC tex damaged");
            }

            if (code != 0)
                sign ^= golumbSign(code);
            else
                sign = 0;

            prevDc += toSigned((code + 1) >> 1, sign);
            out[idx] = 4096 + qScale(qMat, 0, prevDc);
        }
    }

    static final void readACCoeffs(InBits bits, int[] qMat, int[] out, int blocksPerSlice, int[] scan, int max)
            throws IOException {
        int run = 4;
        int level = 2;

        int blockMask = blocksPerSlice - 1; // since blocksPerSlice is 1 << n
        int log2BlocksPerSlice = log2(blocksPerSlice);
        int maxCoeffs = 64 << log2BlocksPerSlice;

        int pos = blockMask;
        while (bits.moreData() && (pos >> log2BlocksPerSlice) < max) {
            run = readCodeword(bits, runCodebooks[min(run, 15)]);
            if (run < 0 || run >= maxCoeffs - pos - 1) {
                return;
                // throw new RuntimeException("AC tex damaged, RUN");
            }
            pos += run + 1;

            level = readCodeword(bits, levCodebooks[min(level, 9)]) + 1;
            if (level < 0 || level > 65535) {
                return;
                // throw new RuntimeException("DC tex damaged, LEV");
            }
            int sign = -bits.read1Bit();
            int ind = pos >> log2BlocksPerSlice;
            out[((pos & blockMask) << 6) + scan[ind]] = qScale(qMat, ind, toSigned(level, sign));
        }
    }

    private static final int qScale(int[] qMat, int ind, int val) {
        return ((val * qMat[ind]) >> 2);
    }

    int[] decodeOnePlane(InBits bits, int blocksPerSlice, int[] qMat, int[] scan, int mbX, int mbY, int plane)
            throws IOException {
        int[] out = new int[blocksPerSlice << 6];
        // try {
        readDCCoeffs(bits, qMat, out, blocksPerSlice);
        readACCoeffs(bits, qMat, out, blocksPerSlice, scan, 64);
        // } catch (RuntimeException e) {
        // int x = mbX << 4;
        // int y = mbY << 4;
        // System.out.println(e.getMessage() + " at (" + x + ", " + y + ", " +
        // (x + (blocksPerSlice << 2)) + ", "
        // + (y + 16) + "), plane " + plane);
        // }

        for (int i = 0; i < blocksPerSlice; i++) {
            idct10(out, i << 6);
        }

        return out;
    }

    public Picture decodeFrame(Buffer data, int[][] target) {
        try {
            DataInput inp = data.dinp();

            FrameHeader fh = readFrameHeader(inp);

            int codedWidth = (fh.width + 15) & ~0xf;
            int codedHeight = (fh.height + 15) & ~0xf;

            int lumaSize = codedWidth * codedHeight;
            int chromaSize = lumaSize >> 1;

            if (target == null || target[0].length < lumaSize || target[1].length < chromaSize
                    || target[2].length < chromaSize) {
                throw new RuntimeException("Provided output picture won't fit into provided buffer");
            }

            if (fh.frameType == 0) {
                decodePicture(data, target, fh.width, fh.height, fh.qMatLuma, fh.qMatChroma, fh.scan, 0);
            } else {
                decodePicture(data, target, fh.width, fh.height >> 1, fh.qMatLuma, fh.qMatChroma, fh.scan,
                        fh.topFieldFirst ? 1 : 2);

                decodePicture(data, target, fh.width, fh.height >> 1, fh.qMatLuma, fh.qMatChroma, fh.scan,
                        fh.topFieldFirst ? 2 : 1);
            }

            return new Picture(codedWidth, codedHeight, target, ColorSpace.YUV422_10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Picture[] decodeFields(Buffer data, int[][][] target) {
        try {
            DataInput inp = data.dinp();

            FrameHeader fh = readFrameHeader(inp);

            int codedWidth = (fh.width + 15) & ~0xf;
            int codedHeight = (fh.height + 15) & ~0xf;

            int lumaSize = codedWidth * codedHeight;
            int chromaSize = lumaSize >> 1;

            if (fh.frameType == 0) {
                if (target == null || target[0][0].length < lumaSize || target[0][1].length < chromaSize
                        || target[0][2].length < chromaSize) {
                    throw new RuntimeException("Provided output picture won't fit into provided buffer");
                }

                decodePicture(data, target[0], fh.width, fh.height, fh.qMatLuma, fh.qMatChroma, fh.scan, 0);
                return new Picture[] { new Picture(codedWidth, codedHeight, target[0], ColorSpace.YUV422_10) };
            } else {
                lumaSize >>= 1;
                chromaSize >>= 1;
                if (target == null || target[0][0].length < lumaSize || target[0][1].length < chromaSize
                        || target[0][2].length < chromaSize || target[1][0].length < lumaSize
                        || target[1][1].length < chromaSize || target[1][2].length < chromaSize) {
                    throw new RuntimeException("Provided output picture won't fit into provided buffer");
                }

                decodePicture(data, target[fh.topFieldFirst ? 0 : 1], fh.width, fh.height >> 1, fh.qMatLuma,
                        fh.qMatChroma, fh.scan, 0);

                decodePicture(data, target[fh.topFieldFirst ? 1 : 0], fh.width, fh.height >> 1, fh.qMatLuma,
                        fh.qMatChroma, fh.scan, 0);

                return new Picture[] { new Picture(codedWidth, codedHeight >> 1, target[0], ColorSpace.YUV422_10),
                        new Picture(codedWidth, codedHeight >> 1, target[1], ColorSpace.YUV422_10) };
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static FrameHeader readFrameHeader(DataInput inp) throws IOException {
        int frameSize = inp.readInt();
        String sig = readSig(inp);
        if (!"icpf".equals(sig))
            throw new RuntimeException("Not a prores frame");

        short hdrSize = inp.readShort();
        short version = inp.readShort();

        int res1 = inp.readInt();

        short width = inp.readShort();
        short height = inp.readShort();

        int flags1 = inp.readByte();

        int frameType = (flags1 >> 2) & 3;

        int[] scan;
        boolean topFieldFirst = false;

        if (frameType == 0) {
            scan = progressive_scan;
        } else {
            scan = interlaced_scan;
            if (frameType == 1)
                topFieldFirst = true;
        }

        byte res2 = inp.readByte();
        byte prim = inp.readByte();
        byte transFunc = inp.readByte();
        byte matrix = inp.readByte();
        byte pixFmt = inp.readByte();
        byte res3 = inp.readByte();

        int flags2 = inp.readByte() & 0xff;

        int[] qMatLuma = new int[64];
        int[] qMatChroma = new int[64];

        if (hasQMatLuma(flags2)) {
            readQMat(inp, qMatLuma, scan);
        } else {
            fill(qMatLuma, 4);
        }

        if (hasQMatChroma(flags2)) {
            readQMat(inp, qMatChroma, scan);
        } else {
            fill(qMatChroma, 4);
        }

        inp.skipBytes(hdrSize - (20 + (hasQMatLuma(flags2) ? 64 : 0) + (hasQMatChroma(flags2) ? 64 : 0)));

        return new FrameHeader(frameSize - hdrSize - 8, width, height, frameType, topFieldFirst, scan, qMatLuma, qMatChroma);
    }

    static final String readSig(DataInput inp) throws IOException {
        byte[] sig = new byte[4];
        inp.readFully(sig);
        return new String(sig);
    }

    private void decodePicture(Buffer data, int[][] result, int width, int height, int[] qMatLuma, int[] qMatChroma,
            int[] scan, int pictureType) throws IOException {
        DataInput inp = data.dinp();

        ProresConsts.PictureHeader ph = readPictureHeader(inp);
        
        int mbWidth = (width + 15) >> 4;
//        int mbHeight = (height + 15) >> 4;

        int mbX = 0, mbY = 0;
        int sliceMbCount = 1 << ph.log2SliceMbWidth;
        for (int i = 0; i < ph.sliceSizes.length; i++) {

            while (mbWidth - mbX < sliceMbCount)
                sliceMbCount >>= 1;

            decodeSlice(data, qMatLuma, qMatChroma, scan, sliceMbCount, mbX, mbY, ph.sliceSizes[i], result, width,
                    pictureType);

            mbX += sliceMbCount;
            if (mbX == mbWidth) {
                sliceMbCount = 1 << ph.log2SliceMbWidth;
                mbX = 0;
                mbY++;
            }
        }
    }

    public static PictureHeader readPictureHeader(DataInput inp) throws IOException {
        int hdrSize = (inp.readByte() & 0xff) >> 3;
        inp.skipBytes(4);
        int sliceCount = inp.readShort();

        int a = inp.readByte() & 0xff;
        int log2SliceMbWidth = a >> 4;
        
        short[] sliceSizes = new short[sliceCount];
        for (int i = 0; i < sliceCount; i++) {
            sliceSizes[i] = inp.readShort();
        }
        return new PictureHeader(log2SliceMbWidth, sliceSizes);
    }

    private void decodeSlice(Buffer data, int[] qMatLuma, int[] qMatChroma, int[] scan, int sliceMbCount, int mbX,
            int mbY, short sliceSize, int[][] result, int lumaStride, int pictureType) throws IOException {
        DataInput inp = data.dinp();

        int hdrSize = (inp.readByte() & 0xff) >> 3;
        int qScale = clip(inp.readByte() & 0xff, 1, 224);
        qScale = qScale > 128 ? qScale - 96 << 2 : qScale;
        int yDataSize = inp.readShort();
        int uDataSize = inp.readShort();
        int vDataSize = sliceSize - uDataSize - yDataSize - hdrSize;

        int[] y = decodeOnePlane(bitstream(data, yDataSize), sliceMbCount << 2, scaleMat(qMatLuma, qScale), scan, mbX,
                mbY, 0);
        int[] u = decodeOnePlane(bitstream(data, uDataSize), sliceMbCount << 1, scaleMat(qMatChroma, qScale), scan,
                mbX, mbY, 1);
        int[] v = decodeOnePlane(bitstream(data, vDataSize), sliceMbCount << 1, scaleMat(qMatChroma, qScale), scan,
                mbX, mbY, 2);

        putSlice(result, lumaStride, mbX, mbY, y, u, v, pictureType == 0 ? 0 : 1, pictureType == 2 ? 1 : 0);
    }

    static final int[] scaleMat(int[] qMatLuma, int qScale) {
        int[] res = new int[qMatLuma.length];
        for (int i = 0; i < qMatLuma.length; i++)
            res[i] = qMatLuma[i] * qScale;

        return res;
    }

    static final InBits bitstream(Buffer data, int dataSize) throws IOException {
        return new BitstreamReaderBB(data.read(dataSize));
    }

    static final int clip(int val, int min, int max) {
        return val < min ? min : (val > max ? max : val);
    }

    private void putSlice(int[][] result, int lumaStride, int mbX, int mbY, int[] y, int[] u, int[] v, int dist,
            int shift) {
        int mbPerSlice = y.length >> 8;

        int chromaStride = lumaStride >> 1;

        putLuma(result[0], shift * lumaStride, lumaStride << dist, mbX, mbY, y, mbPerSlice, dist, shift);
        putChroma(result[1], shift * chromaStride, chromaStride << dist, mbX, mbY, u, mbPerSlice, dist, shift);
        putChroma(result[2], shift * chromaStride, chromaStride << dist, mbX, mbY, v, mbPerSlice, dist, shift);
    }

    private void putLuma(int[] y, int off, int stride, int mbX, int mbY, int[] luma, int mbPerSlice, int dist, int shift) {
        off += (mbX << 4) + (mbY << 4) * stride;
        for (int k = 0; k < mbPerSlice; k++) {
            putBlock(y, off, stride, luma, k << 8, dist, shift);
            putBlock(y, off + 8, stride, luma, (k << 8) + 64, dist, shift);
            putBlock(y, off + 8 * stride, stride, luma, (k << 8) + 128, dist, shift);
            putBlock(y, off + 8 * stride + 8, stride, luma, (k << 8) + 192, dist, shift);
            off += 16;
        }
    }

    private void putChroma(int[] y, int off, int stride, int mbX, int mbY, int[] chroma, int mbPerSlice, int dist,
            int shift) {
        off += (mbX << 3) + (mbY << 4) * stride;
        for (int k = 0; k < mbPerSlice; k++) {
            putBlock(y, off, stride, chroma, k << 7, dist, shift);
            putBlock(y, off + 8 * stride, stride, chroma, (k << 7) + 64, dist, shift);
            off += 8;
        }
    }

    private void putBlock(int[] square, int sqOff, int sqStride, int[] flat, int flOff, int dist, int shift) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++)
                square[j + sqOff] = clip(flat[j + flOff], 4, 1019);
            sqOff += sqStride;
            flOff += 8;
        }
    }

    static final boolean hasQMatChroma(int flags2) {
        return (flags2 & 1) != 0;
    }

    static final void readQMat(DataInput inp, int[] qMatLuma, int[] scan) throws IOException {
        byte[] b = new byte[64];
        inp.readFully(b);
        for (int i = 0; i < 64; i++) {
            qMatLuma[i] = b[scan[i]] & 0xff;
        }
    }

    static final boolean hasQMatLuma(int flags2) {
        return (flags2 & 2) != 0;
    }

    public boolean isProgressive(Buffer data) {
        return (((data.get(20) & 0xff) >> 2) & 3) == 0;
    }

    public int probe(Buffer data) {
        if (data.get(4) == 'i' && data.get(5) == 'c' && data.get(6) == 'p' && data.get(7) == 'f')
            return 100;
        return 0;
    }
}