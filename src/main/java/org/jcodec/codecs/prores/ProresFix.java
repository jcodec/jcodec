package org.jcodec.codecs.prores;

import static java.lang.Math.min;
import static org.jcodec.codecs.prores.ProresConsts.dcCodebooks;
import static org.jcodec.codecs.prores.ProresConsts.firstDCCodebook;
import static org.jcodec.codecs.prores.ProresConsts.levCodebooks;
import static org.jcodec.codecs.prores.ProresConsts.runCodebooks;
import static org.jcodec.codecs.prores.ProresDecoder.readCodeword;
import static org.jcodec.codecs.prores.ProresDecoder.toSigned;
import static org.jcodec.codecs.prores.ProresEncoder.getLevel;
import static org.jcodec.codecs.prores.ProresEncoder.isNegative;
import static org.jcodec.codecs.prores.ProresEncoder.writeCodeword;
import static org.jcodec.common.tools.MathUtil.log2;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.codecs.prores.ProresConsts.PictureHeader;
import org.jcodec.common.io.BitstreamReaderBB;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;

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

    static final void readDCCoeffs(InBits bits, int[] out, int blocksPerSlice) throws IOException {
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

    static final void readACCoeffs(BitstreamReaderBB bits, int[] out, int blocksPerSlice, int[] scan)
            throws IOException {
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

    static final void writeDCCoeffs(OutBits bits, int[] in, int blocksPerSlice) throws IOException {
        writeCodeword(bits, firstDCCodebook, in[0]);

        int code = 5, idx = 64;
        for (int i = 1; i < blocksPerSlice; i++, idx += 64) {
            writeCodeword(bits, dcCodebooks[min(code, 6)], in[idx]);
            code = in[idx];
        }
    }

    static final void writeACCoeffs(OutBits bits, int[] in, int blocksPerSlice, int[] scan) throws IOException {
        int prevRun = 4;
        int prevLevel = 2;

        int run = 0;
        for (int i = 1; i < 64; i++) {
            int indp = scan[i];
            for (int j = 0; j < blocksPerSlice; j++) {
                int val = in[(j << 6) + indp];
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

    static void copyCoeff(BitstreamReaderBB ib, OutBits ob, int blocksPerSlice, int[] scan) throws IOException {
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

    public static Buffer transcode(Buffer inBuf, byte[] buf) throws IOException {
        Buffer outBuf = new Buffer(buf);
        Buffer fork = outBuf.fork();

        DataOutput out = outBuf.dout();
        DataInput inp = inBuf.dinp();

        FrameHeader fh = ProresDecoder.readFrameHeader(inp);
        ProresEncoder.writeFrameHeader(out, fh);

        if (fh.frameType == 0) {
            transcodePicture(inBuf, outBuf, fh);
        } else {
            transcodePicture(inBuf, outBuf, fh);

            transcodePicture(inBuf, outBuf, fh);
        }

        ProresEncoder.writeFrameHeader(fork.dout(), fh);

        return new Buffer(buf, 0, outBuf.pos);
    }

    private static void transcodePicture(Buffer inBuf, Buffer outBuf, FrameHeader fh) throws IOException {
        DataOutput out = outBuf.dout();
        DataInput inp = inBuf.dinp();

        PictureHeader ph = ProresDecoder.readPictureHeader(inp);
        Buffer fork = outBuf.from(0);
        ProresEncoder.writePictureHeader(ph, out);

        int mbWidth = (fh.width + 15) >> 4;
        int mbHeight = (fh.height + 15) >> 4;

        int sliceMbCount = 1 << ph.log2SliceMbWidth;
        int mbX = 0, mbY = 0;
        for (int i = 0; i < ph.sliceSizes.length; i++) {

            while (mbWidth - mbX < sliceMbCount)
                sliceMbCount >>= 1;

            int savedPoint = outBuf.pos;

            transcodeSlice(inBuf, outBuf, sliceMbCount, ph.sliceSizes[i], fh);
            ph.sliceSizes[i] = (short) (outBuf.pos - savedPoint);

            mbX += sliceMbCount;
            if (mbX == mbWidth) {
                sliceMbCount = 1 << ph.log2SliceMbWidth;
                mbX = 0;
                mbY++;
            }
        }

        ProresEncoder.writePictureHeader(ph, fork.dout());
    }

    private static void transcodeSlice(Buffer inBuf, Buffer outBuf, int sliceMbCount, short sliceSize, FrameHeader fh)
            throws IOException {
        DataInput inp = inBuf.dinp();
        DataOutput out = outBuf.dout();

        int hdrSize = (inp.readByte() & 0xff) >> 3;
        int qScaleOrig = inp.readByte() & 0xff;
        int yDataSize = inp.readShort();
        int uDataSize = inp.readShort();
        int vDataSize = sliceSize - uDataSize - yDataSize - hdrSize;

        out.write(6 << 3); // hdr size
        out.write(qScaleOrig); // qscale
        Buffer beforeSizes = outBuf.fork();
        out.writeShort(0);
        out.writeShort(0);

        int beforeY = outBuf.pos;
        copyCoeff(bitstream(inBuf, yDataSize), new BitstreamWriter(outBuf.os()), sliceMbCount << 2, fh.scan);
        int beforeCb = outBuf.pos;
        copyCoeff(bitstream(inBuf, uDataSize), new BitstreamWriter(outBuf.os()), sliceMbCount << 1, fh.scan);
        int beforeCr = outBuf.pos;
        copyCoeff(bitstream(inBuf, vDataSize), new BitstreamWriter(outBuf.os()), sliceMbCount << 1, fh.scan);

        out = beforeSizes.dout();
        out.writeShort(beforeCb - beforeY);
        out.writeShort(beforeCr - beforeCb);
    }

    static final BitstreamReaderBB bitstream(Buffer data, int dataSize) throws IOException {
        return new BitstreamReaderBB(data.read(dataSize));
    }

    public static List<String> check(Buffer data) throws IOException {
        List<String> messages = new ArrayList<String>();
        DataInput inp = data.dinp();
        int frameSize = inp.readInt();

        if (!"icpf".equals(ProresDecoder.readSig(inp))) {
            messages.add("[ERROR] Missing ProRes signature (icpf).");
            return messages;
        }

        short headerSize = inp.readShort();
        if (headerSize > 148) {
            messages.add("[ERROR] Wrong ProRes frame header.");
            return messages;
        }
        short version = inp.readShort();

        int res1 = inp.readInt();

        short width = inp.readShort();
        short height = inp.readShort();
        if (width < 0 || width > 10000 || height < 0 || height > 10000) {
            messages.add("[ERROR] Wrong ProRes frame header, invalid image size [" + width + "x" + height + "].");
            return messages;
        }

        int flags1 = inp.readByte();

        inp.skipBytes(headerSize - 13);

        if (((flags1 >> 2) & 3) == 0) {
            checkPicture(data, width, height, messages);
        } else {
            checkPicture(data, width, height / 2, messages);
            checkPicture(data, width, height / 2, messages);
        }

        return messages;
    }

    private static void checkPicture(Buffer data, int width, int height, List<String> messages) throws IOException {
        DataInput inp = data.dinp();

        PictureHeader ph = ProresDecoder.readPictureHeader(inp);

        int mbWidth = (width + 15) >> 4;
        int mbHeight = (height + 15) >> 4;

        int sliceMbCount = 1 << ph.log2SliceMbWidth;
        int mbX = 0, mbY = 0;
        for (int i = 0; i < ph.sliceSizes.length; i++) {

            while (mbWidth - mbX < sliceMbCount)
                sliceMbCount >>= 1;

            try {
                checkSlice(data.read(ph.sliceSizes[i]), sliceMbCount);
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

    private static void checkSlice(Buffer sliceData, int sliceMbCount) throws IOException {
        DataInput inp = sliceData.dinp();
        int sliceSize = sliceData.remaining();

        int hdrSize = (inp.readByte() & 0xff) >> 3;
        int qScaleOrig = inp.readByte() & 0xff;
        int yDataSize = inp.readShort();
        int uDataSize = inp.readShort();
        int vDataSize = sliceSize - uDataSize - yDataSize - hdrSize;

        checkCoeff(bitstream(sliceData, yDataSize), sliceMbCount << 2);
        checkCoeff(bitstream(sliceData, uDataSize), sliceMbCount << 1);
        checkCoeff(bitstream(sliceData, vDataSize), sliceMbCount << 1);
    }

    private static void checkCoeff(BitstreamReaderBB ib, int blocksPerSlice) throws IOException {
        int[] scan = new int[64];
        int[] out = new int[blocksPerSlice << 6];
        readDCCoeffs(ib, out, blocksPerSlice);
        readACCoeffs(ib, out, blocksPerSlice, scan);
    }
}