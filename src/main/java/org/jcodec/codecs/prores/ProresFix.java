package org.jcodec.codecs.prores;
import static java.lang.Math.min;
import static org.jcodec.codecs.prores.ProresConsts.dcCodebooks;
import static org.jcodec.codecs.prores.ProresConsts.firstDCCodebook;
import static org.jcodec.codecs.prores.ProresConsts.levCodebooks;
import static org.jcodec.codecs.prores.ProresConsts.runCodebooks;
import static org.jcodec.codecs.prores.ProresDecoder.bitstream;
import static org.jcodec.codecs.prores.ProresDecoder.readCodeword;
import static org.jcodec.codecs.prores.ProresEncoder.getLevel;
import static org.jcodec.codecs.prores.ProresEncoder.writeCodeword;
import static org.jcodec.common.tools.MathUtil.log2;
import static org.jcodec.common.tools.MathUtil.sign;
import static org.jcodec.common.tools.MathUtil.toSigned;

import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.codecs.prores.ProresConsts.PictureHeader;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.io.NIOUtils;

import js.nio.ByteBuffer;
import js.util.ArrayList;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Rewrites DCT coefficients to new ProRes bitstream concealing errors
 * 
 * @author The JCodec project
 * 
 */
public class ProresFix {

    static final void readDCCoeffs(BitReader bits, int[] out, int blocksPerSlice) {
        out[0] = readCodeword(bits, firstDCCodebook);
        if (out[0] < 0) {
            throw new RuntimeException("First DC coeff damaged");
        }

        int code = 5, idx = 64;
        for (int i = 1; i < blocksPerSlice; i++, idx += 64) {
            code = readCodeword(bits, dcCodebooks[min(code, 6)]);
            if (code < 0) {
                throw new RuntimeException("DC coeff damaged");
            }

            out[idx] = code;
        }
    }

    static final void readACCoeffs(BitReader bits, int[] out, int blocksPerSlice, int[] scan) {
        int run = 4;
        int level = 2;

        int blockMask = blocksPerSlice - 1;
        int log2BlocksPerSlice = log2(blocksPerSlice);
        int maxCoeffs = 64 << log2BlocksPerSlice;

        int pos = blockMask;
        while (bits.remaining() > 32 || bits.checkNBit(24) != 0) {
            run = readCodeword(bits, runCodebooks[min(run, 15)]);
            if (run < 0 || run >= maxCoeffs - pos - 1) {
                throw new RuntimeException("Run codeword damaged");
            }
            pos += run + 1;

            level = readCodeword(bits, levCodebooks[min(level, 9)]) + 1;
            if (level < 0 || level > 65535) {
                throw new RuntimeException("Level codeword damaged");
            }
            int sign = -bits.read1Bit();
            int ind = pos >> log2BlocksPerSlice;
            out[((pos & blockMask) << 6) + scan[ind]] = toSigned(level, sign);
        }
    }

    static final void writeDCCoeffs(BitWriter bits, int[] _in, int blocksPerSlice) {
        writeCodeword(bits, firstDCCodebook, _in[0]);

        int code = 5, idx = 64;
        for (int i = 1; i < blocksPerSlice; i++, idx += 64) {
            writeCodeword(bits, dcCodebooks[min(code, 6)], _in[idx]);
            code = _in[idx];
        }
    }

    static final void writeACCoeffs(BitWriter bits, int[] _in, int blocksPerSlice, int[] scan) {
        int prevRun = 4;
        int prevLevel = 2;

        int run = 0;
        for (int i = 1; i < 64; i++) {
            int indp = scan[i];
            for (int j = 0; j < blocksPerSlice; j++) {
                int val = _in[(j << 6) + indp];
                if (val == 0)
                    run++;
                else {
                    writeCodeword(bits, runCodebooks[min(prevRun, 15)], run);
                    prevRun = run;
                    run = 0;
                    int level = getLevel(val);
                    writeCodeword(bits, levCodebooks[min(prevLevel, 9)], level - 1);
                    prevLevel = level;
                    bits.write1Bit(sign(val));
                }
            }
        }
    }

    static void copyCoeff(BitReader ib, BitWriter ob, int blocksPerSlice, int[] scan) {
        int[] out = new int[blocksPerSlice << 6];
        try {
            readDCCoeffs(ib, out, blocksPerSlice);
            readACCoeffs(ib, out, blocksPerSlice, scan);
        } catch (RuntimeException e) {
        }
        writeDCCoeffs(ob, out, blocksPerSlice);
        writeACCoeffs(ob, out, blocksPerSlice, scan);
        ob.flush();
    }

    public static ByteBuffer transcode(ByteBuffer inBuf, ByteBuffer _outBuf) {
        ByteBuffer outBuf = _outBuf.slice();
        ByteBuffer fork = outBuf.duplicate();

        FrameHeader fh = ProresDecoder.readFrameHeader(inBuf);
        ProresEncoder.writeFrameHeader(outBuf, fh);

        if (fh.frameType == 0) {
            transcodePicture(inBuf, outBuf, fh);
        } else {
            transcodePicture(inBuf, outBuf, fh);

            transcodePicture(inBuf, outBuf, fh);
        }

        ProresEncoder.writeFrameHeader(fork, fh);

        outBuf.flip();

        return outBuf;
    }

    private static void transcodePicture(ByteBuffer inBuf, ByteBuffer outBuf, FrameHeader fh) {

        PictureHeader ph = ProresDecoder.readPictureHeader(inBuf);
        ProresEncoder.writePictureHeader(ph.log2SliceMbWidth, ph.sliceSizes.length, outBuf);
        ByteBuffer fork = outBuf.duplicate();
        outBuf.setPosition(outBuf.position() + (ph.sliceSizes.length << 1));

        int mbWidth = (fh.width + 15) >> 4;

        int sliceMbCount = 1 << ph.log2SliceMbWidth;
        int mbX = 0;
        for (int i = 0; i < ph.sliceSizes.length; i++) {

            while (mbWidth - mbX < sliceMbCount)
                sliceMbCount >>= 1;

            int savedPoint = outBuf.position();

            transcodeSlice(inBuf, outBuf, sliceMbCount, ph.sliceSizes[i], fh);
            fork.putShort((short) (outBuf.position() - savedPoint));

            mbX += sliceMbCount;
            if (mbX == mbWidth) {
                sliceMbCount = 1 << ph.log2SliceMbWidth;
                mbX = 0;
            }
        }
    }

    private static void transcodeSlice(ByteBuffer inBuf, ByteBuffer outBuf, int sliceMbCount, short sliceSize,
            FrameHeader fh) {

        int hdrSize = (inBuf.get() & 0xff) >> 3;
        int qScaleOrig = inBuf.get() & 0xff;
        int yDataSize = inBuf.getShort();
        int uDataSize = inBuf.getShort();
        int vDataSize = sliceSize - uDataSize - yDataSize - hdrSize;

        outBuf.put((byte) (6 << 3)); // hdr size
        outBuf.put((byte) qScaleOrig); // qscale
        ByteBuffer beforeSizes = outBuf.duplicate();
        outBuf.putInt(0);

        int beforeY = outBuf.position();
        copyCoeff(bitstream(inBuf, yDataSize), new BitWriter(outBuf), sliceMbCount << 2, fh.scan);
        int beforeCb = outBuf.position();
        copyCoeff(bitstream(inBuf, uDataSize), new BitWriter(outBuf), sliceMbCount << 1, fh.scan);
        int beforeCr = outBuf.position();
        copyCoeff(bitstream(inBuf, vDataSize), new BitWriter(outBuf), sliceMbCount << 1, fh.scan);

        beforeSizes.putShort((short) (beforeCb - beforeY));
        beforeSizes.putShort((short) (beforeCr - beforeCb));
    }

    public static List<String> check(ByteBuffer data) {
        List<String> messages = new ArrayList<String>();
        int frameSize = data.getInt();

        if (!"icpf".equals(ProresDecoder.readSig(data))) {
            messages.add("[ERROR] Missing ProRes signature (icpf).");
            return messages;
        }

        short headerSize = data.getShort();
        if (headerSize > 148) {
            messages.add("[ERROR] Wrong ProRes frame header.");
            return messages;
        }
        short version = data.getShort();

        int res1 = data.getInt();

        short width = data.getShort();
        short height = data.getShort();
        if (width < 0 || width > 10000 || height < 0 || height > 10000) {
            messages.add("[ERROR] Wrong ProRes frame header, invalid image size [" + width + "x" + height + "].");
            return messages;
        }

        int flags1 = data.get();

        data.setPosition(data.position() + headerSize - 13);

        if (((flags1 >> 2) & 3) == 0) {
            checkPicture(data, width, height, messages);
        } else {
            checkPicture(data, width, height / 2, messages);
            checkPicture(data, width, height / 2, messages);
        }

        return messages;
    }

    private static void checkPicture(ByteBuffer data, int width, int height, List<String> messages) {

        PictureHeader ph = ProresDecoder.readPictureHeader(data);

        int mbWidth = (width + 15) >> 4;
        int mbHeight = (height + 15) >> 4;

        int sliceMbCount = 1 << ph.log2SliceMbWidth;
        int mbX = 0, mbY = 0;
        for (int i = 0; i < ph.sliceSizes.length; i++) {

            while (mbWidth - mbX < sliceMbCount)
                sliceMbCount >>= 1;

            try {
                checkSlice(NIOUtils.read(data, ph.sliceSizes[i]), sliceMbCount);
            } catch (Exception e) {
                messages.add("[ERROR] Slice data corrupt: mbX = " + mbX + ", mbY = " + mbY + ". " + e.getMessage());
            }

            mbX += sliceMbCount;
            if (mbX == mbWidth) {
                sliceMbCount = 1 << ph.log2SliceMbWidth;
                mbX = 0;
                mbY++;
            }
        }
    }

    private static void checkSlice(ByteBuffer sliceData, int sliceMbCount) {
        int sliceSize = sliceData.remaining();

        int hdrSize = (sliceData.get() & 0xff) >> 3;
        int qScaleOrig = sliceData.get() & 0xff;
        int yDataSize = sliceData.getShort();
        int uDataSize = sliceData.getShort();
        int vDataSize = sliceSize - uDataSize - yDataSize - hdrSize;

        checkCoeff(bitstream(sliceData, yDataSize), sliceMbCount << 2);
        checkCoeff(bitstream(sliceData, uDataSize), sliceMbCount << 1);
        checkCoeff(bitstream(sliceData, vDataSize), sliceMbCount << 1);
    }

    private static void checkCoeff(BitReader ib, int blocksPerSlice) {
        int[] scan = new int[64];
        int[] out = new int[blocksPerSlice << 6];
        readDCCoeffs(ib, out, blocksPerSlice);
        readACCoeffs(ib, out, blocksPerSlice, scan);
    }
}