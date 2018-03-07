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
import static org.jcodec.common.tools.MathUtil.toSigned;

import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.codecs.prores.ProresConsts.PictureHeader;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.platform.Platform;

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
public class ProresDecoder extends VideoDecoder {

    public ProresDecoder() {
    }

    static final int[] table = new int[] { 8, 7, 6, 6, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    static final int[] mask = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, -1 };

    public static final int nZeros(int check16Bit) {
        int low = table[check16Bit & 0xff];
        check16Bit >>= 8;
        int high = table[check16Bit];

        return high + (mask[high] & low);
    }

    public static final int readCodeword(BitReader reader, Codebook codebook) {
        int q = nZeros(reader.check16Bits());
        reader.skipFast(q + 1);

        if (q > codebook.switchBits) {
            int bits = codebook.golombBits + q;
            if (bits > 16)
                Logger.error("Broken prores slice");
            return ((1 << bits) | reader.readFast16(bits)) - codebook.golombOffset;
        } else if (codebook.riceOrder > 0)
            return (q << codebook.riceOrder) | reader.readFast16(codebook.riceOrder);
        else
            return q;
    }

    public static final int golumbToSigned(int val) {
        return (val >> 1) ^ golumbSign(val);
    }

    public static final int golumbSign(int val) {
        return -(val & 1);
    }

    public static final void readDCCoeffs(BitReader bits, int[] qMat, int[] out, int blocksPerSlice, int blkSize) {
        int c = readCodeword(bits, firstDCCodebook);
        if (c < 0) {
            return;
            // throw new RuntimeException("DC tex damaged");
        }
        int prevDc = golumbToSigned(c);
        out[0] = 4096 + qScale(qMat, 0, prevDc);

        int code = 5, sign = 0, idx = blkSize;
        for (int i = 1; i < blocksPerSlice; i++, idx += blkSize) {
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

    protected static final void readACCoeffs(BitReader bits, int[] qMat, int[] out, int blocksPerSlice, int[] scan,
            int max, int log2blkSize) {
        int run = 4;
        int level = 2;

        int blockMask = blocksPerSlice - 1; // since blocksPerSlice is 1 << n
        int log2BlocksPerSlice = log2(blocksPerSlice);
        int maxCoeffs = 64 << log2BlocksPerSlice;

        int pos = blockMask;
        while (bits.remaining() > 32 || bits.checkAllBits() != 0) {
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
            if (ind >= max)
                break;
            out[((pos & blockMask) << log2blkSize) + scan[ind]] = qScale(qMat, ind, toSigned(level, sign));
        }
    }

    private static final int qScale(int[] qMat, int ind, int val) {
        return ((val * qMat[ind]) >> 2);
    }

    protected void decodeOnePlane(BitReader bits, int blocksPerSlice, int[] out, int[] qMat, int[] scan, int mbX,
            int mbY, int plane) {
        try {
            readDCCoeffs(bits, qMat, out, blocksPerSlice, 64);
            readACCoeffs(bits, qMat, out, blocksPerSlice, scan, 64, 6);
        } catch (RuntimeException e) {
            System.err.println("Suppressing slice error at [" + mbX + ", " + mbY + "].");
        }

        for (int i = 0; i < blocksPerSlice; i++) {
            idct10(out, i << 6);
        }
    }
    
    public Picture decodeFrame(ByteBuffer data, byte[][] target) {
        return decodeFrameHiBD(data, target, null);
    }
    
    public Picture decodeFrameHiBD(ByteBuffer data, byte[][] target, byte[][] lowBits) {
        FrameHeader fh = readFrameHeader(data);

        int codedWidth = (fh.width + 15) & ~0xf;
        int codedHeight = (fh.height + 15) & ~0xf;

        int lumaSize = codedWidth * codedHeight;
        int chromaSize = lumaSize >> (3 - fh.chromaType);

        if (target == null || target[0].length < lumaSize || target[1].length < chromaSize
                || target[2].length < chromaSize) {
            throw new RuntimeException("Provided output picture won't fit into provided buffer");
        }

        if (fh.frameType == 0) {
            decodePicture(data, target, lowBits, codedWidth, codedHeight, codedWidth >> 4, fh.qMatLuma, fh.qMatChroma, fh.scan, 0,
                    fh.chromaType);
        } else {
            decodePicture(data, target, lowBits, codedWidth, codedHeight >> 1, codedWidth >> 4, fh.qMatLuma, fh.qMatChroma, fh.scan,
                    fh.topFieldFirst ? 1 : 2, fh.chromaType);

            decodePicture(data, target, lowBits, codedWidth, codedHeight >> 1, codedWidth >> 4, fh.qMatLuma, fh.qMatChroma, fh.scan,
                    fh.topFieldFirst ? 2 : 1, fh.chromaType);
        }
        
        ColorSpace color = fh.chromaType == 2 ? ColorSpace.YUV422 : ColorSpace.YUV444;
        
        return new Picture(codedWidth, codedHeight, target, lowBits, color, lowBits == null ? 0 : 2, new Rect(0,
                0, fh.width & color.getWidthMask(), fh.height & color.getHeightMask()));
    }

    public Picture[] decodeFields(ByteBuffer data, byte[][][] target) {
        return decodeFieldsHiBD(data, target, null);
    }
    
    public Picture[] decodeFieldsHiBD(ByteBuffer data, byte[][][] target, byte[][][] lowBits) {
        FrameHeader fh = readFrameHeader(data);

        int codedWidth = (fh.width + 15) & ~0xf;
        int codedHeight = (fh.height + 15) & ~0xf;

        int lumaSize = codedWidth * codedHeight;
        int chromaSize = lumaSize >> 1;

        if (fh.frameType == 0) {
            if (target == null || target[0][0].length < lumaSize || target[0][1].length < chromaSize
                    || target[0][2].length < chromaSize) {
                throw new RuntimeException("Provided output picture won't fit into provided buffer");
            }

            decodePicture(data, target[0], lowBits[0], fh.width, fh.height, codedWidth >> 4, fh.qMatLuma,
                    fh.qMatChroma, fh.scan, 0, fh.chromaType);
            return new Picture[] {
                    Picture.createPicture(codedWidth, codedHeight, target[0], ColorSpace.YUV422) };
        } else {
            lumaSize >>= 1;
            chromaSize >>= 1;
            if (target == null || target[0][0].length < lumaSize || target[0][1].length < chromaSize
                    || target[0][2].length < chromaSize || target[1][0].length < lumaSize
                    || target[1][1].length < chromaSize || target[1][2].length < chromaSize) {
                throw new RuntimeException("Provided output picture won't fit into provided buffer");
            }

            decodePicture(data, target[fh.topFieldFirst ? 0 : 1], lowBits[fh.topFieldFirst ? 0 : 1], fh.width,
                    fh.height >> 1, codedWidth >> 4, fh.qMatLuma, fh.qMatChroma, fh.scan, 0, fh.chromaType);

            decodePicture(data, target[fh.topFieldFirst ? 1 : 0], lowBits[fh.topFieldFirst ? 0 : 1], fh.width,
                    fh.height >> 1, codedWidth >> 4, fh.qMatLuma, fh.qMatChroma, fh.scan, 0, fh.chromaType);

            return new Picture[] {
                    Picture.createPicture(codedWidth, codedHeight >> 1, target[0], ColorSpace.YUV422),
                    Picture.createPicture(codedWidth, codedHeight >> 1, target[1], ColorSpace.YUV422) };
        }
    }

    public static FrameHeader readFrameHeader(ByteBuffer inp) {
        int frameSize = inp.getInt();
        String sig = readSig(inp);
        if (!"icpf".equals(sig))
            throw new RuntimeException("Not a prores frame");

        short hdrSize = inp.getShort();
        short version = inp.getShort();

        int res1 = inp.getInt();

        short width = inp.getShort();
        short height = inp.getShort();

        int flags1 = inp.get();

        int frameType = (flags1 >> 2) & 3;
        int chromaType = (flags1 >> 6) & 3;

        int[] scan;
        boolean topFieldFirst = false;

        if (frameType == 0) {
            scan = progressive_scan;
        } else {
            scan = interlaced_scan;
            if (frameType == 1)
                topFieldFirst = true;
        }

        byte res2 = inp.get();
        byte prim = inp.get();
        byte transFunc = inp.get();
        byte matrix = inp.get();
        byte pixFmt = inp.get();
        byte res3 = inp.get();

        int flags2 = inp.get() & 0xff;

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

        inp.position(
                inp.position() + hdrSize - (20 + (hasQMatLuma(flags2) ? 64 : 0) + (hasQMatChroma(flags2) ? 64 : 0)));

        return new FrameHeader(frameSize - hdrSize - 8, width, height, frameType, topFieldFirst, scan, qMatLuma,
                qMatChroma, chromaType);
    }

    static final String readSig(ByteBuffer inp) {
        byte[] sig = new byte[4];
        inp.get(sig);
        return Platform.stringFromBytes(sig);
    }

    protected void decodePicture(ByteBuffer data, byte[][] result, byte[][] lowBits, int width, int height,
            int mbWidth, int[] qMatLuma, int[] qMatChroma, int[] scan, int pictureType, int chromaType) {
        ProresConsts.PictureHeader ph = readPictureHeader(data);

        int mbX = 0, mbY = 0;
        int sliceMbCount = 1 << ph.log2SliceMbWidth;
        for (int i = 0; i < ph.sliceSizes.length; i++) {

            while (mbWidth - mbX < sliceMbCount)
                sliceMbCount >>= 1;

            decodeSlice(NIOUtils.read(data, ph.sliceSizes[i]), qMatLuma, qMatChroma, scan, sliceMbCount, mbX, mbY,
                    ph.sliceSizes[i], result, lowBits, width, pictureType, chromaType);

            mbX += sliceMbCount;
            if (mbX == mbWidth) {
                sliceMbCount = 1 << ph.log2SliceMbWidth;
                mbX = 0;
                mbY++;
            }
        }
    }

    public static PictureHeader readPictureHeader(ByteBuffer inp) {
        int hdrSize = (inp.get() & 0xff) >> 3;
        inp.getInt();
        int sliceCount = inp.getShort();

        int a = inp.get() & 0xff;
        int log2SliceMbWidth = a >> 4;

        short[] sliceSizes = new short[sliceCount];
        for (int i = 0; i < sliceCount; i++) {
            sliceSizes[i] = inp.getShort();
        }
        return new PictureHeader(log2SliceMbWidth, sliceSizes);
    }

    private void decodeSlice(ByteBuffer data, int[] qMatLuma, int[] qMatChroma, int[] scan, int sliceMbCount, int mbX,
            int mbY, short sliceSize, byte[][] result, byte[][] lowBits, int lumaStride, int pictureType, int chromaType) {

        int hdrSize = (data.get() & 0xff) >> 3;
        int qScale = clip(data.get() & 0xff, 1, 224);
        qScale = qScale > 128 ? qScale - 96 << 2 : qScale;
        int yDataSize = data.getShort();
        int uDataSize = data.getShort();
        int vDataSize = hdrSize > 7 ? data.getShort() : sliceSize - uDataSize - yDataSize - hdrSize;

        int[] y = new int[sliceMbCount << 8];
        decodeOnePlane(bitstream(data, yDataSize), sliceMbCount << 2, y, scaleMat(qMatLuma, qScale), scan, mbX, mbY, 0);
        int chromaBlkCount = (sliceMbCount << chromaType) >> 1;
        int[] u = new int[chromaBlkCount << 6];
        decodeOnePlane(bitstream(data, uDataSize), chromaBlkCount, u, scaleMat(qMatChroma, qScale), scan, mbX, mbY, 1);
        int[] v = new int[chromaBlkCount << 6];
        decodeOnePlane(bitstream(data, vDataSize), chromaBlkCount, v, scaleMat(qMatChroma, qScale), scan, mbX, mbY, 2);

        putSlice(result, lowBits, lumaStride, mbX, mbY, y, u, v, pictureType == 0 ? 0 : 1, pictureType == 2 ? 1 : 0,
                chromaType, sliceMbCount);
    }

    public static final int[] scaleMat(int[] qMatLuma, int qScale) {
        int[] res = new int[qMatLuma.length];
        for (int i = 0; i < qMatLuma.length; i++)
            res[i] = qMatLuma[i] * qScale;

        return res;
    }

    static final BitReader bitstream(ByteBuffer data, int dataSize) {
        return BitReader.createBitReader(NIOUtils.read(data, dataSize));
    }

    static final int clip(int val, int min, int max) {
        return val < min ? min : (val > max ? max : val);
    }

    protected void putSlice(byte[][] result, byte[][] lowBits, int lumaStride, int mbX, int mbY, int[] y, int[] u,
            int[] v, int dist, int shift, int chromaType, int sliceMbCount) {
        int chromaStride = lumaStride >> 1;

        putLuma(result[0], lowBits != null ? lowBits[0] : null, shift * lumaStride, lumaStride << dist, mbX, mbY,
                y, sliceMbCount, dist, shift);
        if (chromaType == 2) {
            putChroma(result[1], lowBits != null ? lowBits[1] : null, shift * chromaStride, chromaStride << dist,
                    mbX, mbY, u, sliceMbCount, dist, shift);
            putChroma(result[2], lowBits != null ? lowBits[2] : null, shift * chromaStride, chromaStride << dist,
                    mbX, mbY, v, sliceMbCount, dist, shift);
        } else {
            putLuma(result[1], lowBits != null ? lowBits[1] : null, shift * lumaStride, lumaStride << dist, mbX,
                    mbY, u, sliceMbCount, dist, shift);
            putLuma(result[2], lowBits != null ? lowBits[2] : null, shift * lumaStride, lumaStride << dist, mbX,
                    mbY, v, sliceMbCount, dist, shift);
        }
    }

    private void putLuma(byte[] y, byte[] lowBits, int off, int stride, int mbX, int mbY, int[] luma, int mbPerSlice, int dist,
            int shift) {
        off += (mbX << 4) + (mbY << 4) * stride;
        for (int k = 0; k < mbPerSlice; k++) {
            putBlock(y, lowBits, off, stride, luma, k << 8, dist, shift);
            putBlock(y, lowBits, off + 8, stride, luma, (k << 8) + 64, dist, shift);
            putBlock(y, lowBits, off + 8 * stride, stride, luma, (k << 8) + 128, dist, shift);
            putBlock(y, lowBits, off + 8 * stride + 8, stride, luma, (k << 8) + 192, dist, shift);
            off += 16;
        }
    }

    private void putChroma(byte[] y, byte[] lowBits, int off, int stride, int mbX, int mbY, int[] chroma, int mbPerSlice, int dist,
            int shift) {
        off += (mbX << 3) + (mbY << 4) * stride;
        for (int k = 0; k < mbPerSlice; k++) {
            putBlock(y, lowBits, off, stride, chroma, k << 7, dist, shift);
            putBlock(y, lowBits, off + 8 * stride, stride, chroma, (k << 7) + 64, dist, shift);
            off += 8;
        }
    }

    private void putBlock(byte[] square, byte[] lowBits, int sqOff, int sqStride, int[] flat, int flOff, int dist,
            int shift) {
        for (int i = 0, dstOff = sqOff, srcOff = flOff; i < 8; i++, dstOff += sqStride, srcOff += 8) {
            for (int j = 0; j < 8; j++) {
                int round = MathUtil.clip((flat[j + srcOff] + 2) >> 2, 1, 255);
                square[j + dstOff] = (byte) (round - 128);
            }
        }

        if (lowBits != null) {
            for (int i = 0, dstOff = sqOff, srcOff = flOff; i < 8; i++, dstOff += sqStride, srcOff += 8) {
                for (int j = 0; j < 8; j++) {
                    int val = MathUtil.clip(flat[j + srcOff], 4, 1019);
                    int round = MathUtil.clip((flat[j + srcOff] + 2) >> 2, 1, 255);
                    lowBits[j + dstOff] = (byte) (val - (round << 2));
                }
            }
        }
    }
    
    static final boolean hasQMatChroma(int flags2) {
        return (flags2 & 1) != 0;
    }

    static final void readQMat(ByteBuffer inp, int[] qMatLuma, int[] scan) {
        byte[] b = new byte[64];
        inp.get(b);
        for (int i = 0; i < 64; i++) {
            qMatLuma[i] = b[scan[i]] & 0xff;
        }
    }

    static final boolean hasQMatLuma(int flags2) {
        return (flags2 & 2) != 0;
    }

    public boolean isProgressive(ByteBuffer data) {
        return (((data.get(20) & 0xff) >> 2) & 3) == 0;
    }

    @UsedViaReflection
    public static int probe(ByteBuffer data) {
        if (data.get(4) == 'i' && data.get(5) == 'c' && data.get(6) == 'p' && data.get(7) == 'f')
            return 100;
        return 0;
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        FrameHeader fh = readFrameHeader(data);
        return org.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta(new Size(fh.width, fh.height),
                fh.chromaType == 2 ? ColorSpace.YUV422 : ColorSpace.YUV444);
    }
}