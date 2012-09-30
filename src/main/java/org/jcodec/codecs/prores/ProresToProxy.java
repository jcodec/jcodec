package org.jcodec.codecs.prores;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jcodec.codecs.prores.ProresDecoder.bitstream;
import static org.jcodec.codecs.prores.ProresDecoder.clip;
import static org.jcodec.codecs.prores.ProresDecoder.readACCoeffs;
import static org.jcodec.codecs.prores.ProresDecoder.readDCCoeffs;
import static org.jcodec.codecs.prores.ProresDecoder.scaleMat;
import static org.jcodec.codecs.prores.ProresEncoder.writeACCoeffs;
import static org.jcodec.codecs.prores.ProresEncoder.writeDCCoeffs;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.codecs.prores.ProresConsts.PictureHeader;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Turns a ProRes frame into ProRes proxy frame
 * 
 * @author The JCodec project
 * 
 */
public class ProresToProxy {

    private int[] qMatLumaTo;
    private int[] qMatChromaTo;
    private int frameSize;

    private static final int START_QP = 6;
    private int bitsPer1024;
    private int bitsPer1024High;
    private int bitsPer1024Low;
    private int nCoeffs;

    public ProresToProxy(int width, int height, int frameSize) {
        qMatLumaTo = ProresConsts.QMAT_LUMA_APCO;
        qMatChromaTo = ProresConsts.QMAT_CHROMA_APCO;
        this.frameSize = frameSize;

        int headerBytes = (height >> 4) * (((width >> 4) + 7) >> 3) * 8 + 148;
        int dataBits = (frameSize - headerBytes) << 3;
        bitsPer1024 = (dataBits << 10) / (width * height);

        bitsPer1024High = bitsPer1024 - bitsPer1024 / 10;
        bitsPer1024Low = bitsPer1024 - bitsPer1024 / 20;

        nCoeffs = max(min(33000 / (width * height >> 8), 64), 4);
    }

    public int getFrameSize() {
        return frameSize;
    }

    void requant(InBits ib, OutBits ob, int blocksPerSlice, int[] qMatFrom, int[] qMatTo, int[] scan, int mbX, int mbY,
            int plane) throws IOException {
        int[] out = new int[blocksPerSlice << 6];
        try {
            readDCCoeffs(ib, qMatFrom, out, blocksPerSlice);
            readACCoeffs(ib, qMatFrom, out, blocksPerSlice, scan, nCoeffs);
        } catch (RuntimeException e) {
        }
        for (int i = 0; i < out.length; i++)
            out[i] <<= 2;
        writeDCCoeffs(ob, qMatTo, out, blocksPerSlice);
        writeACCoeffs(ob, qMatTo, out, blocksPerSlice, scan, nCoeffs);
        ob.flush();
    }

    public void transcode(Buffer inBuf, Buffer outBuf) throws IOException {
        DataInput inp = inBuf.dinp();
        DataOutput out = outBuf.dout();
        Buffer fork = outBuf.from(0);

        FrameHeader fh = ProresDecoder.readFrameHeader(inp);
        ProresEncoder.writeFrameHeader(out, fh);

        int beforePicture = outBuf.pos;
        if (fh.frameType == 0) {
            transcodePicture(inBuf, outBuf, fh);
        } else {
            transcodePicture(inBuf, outBuf, fh);
            transcodePicture(inBuf, outBuf, fh);
        }
        fh.qMatLuma = qMatLumaTo;
        fh.qMatChroma = qMatChromaTo;
        fh.payloadSize = outBuf.pos - beforePicture;
        ProresEncoder.writeFrameHeader(fork.dout(), fh);
    }

    private void transcodePicture(Buffer inBuf, Buffer outBuf, FrameHeader fh) throws IOException {
        Buffer fork = outBuf.fork();

        PictureHeader ph = ProresDecoder.readPictureHeader(inBuf.dinp());
        ProresEncoder.writePictureHeader(ph, outBuf.dout());

        int mbX = 0, mbY = 0;
        int mbWidth = (fh.width + 15) >> 4;
        int sliceMbCount = 1 << ph.log2SliceMbWidth;
        int balance = 0, qp = START_QP;
        for (int i = 0; i < ph.sliceSizes.length; i++) {

            while (mbWidth - mbX < sliceMbCount)
                sliceMbCount >>= 1;

            int savedPoint = outBuf.pos;

            transcodeSlice(inBuf, outBuf, fh.qMatLuma, fh.qMatChroma, fh.scan, sliceMbCount, mbX, mbY,
                    ph.sliceSizes[i], qp);
            ph.sliceSizes[i] = (short) (outBuf.pos - savedPoint);

            int max = (sliceMbCount * bitsPer1024High >> 5) + 6;
            int low = (sliceMbCount * bitsPer1024Low >> 5) + 6;

            if (ph.sliceSizes[i] > max && qp < 128) {
                qp++;
                if ((ph.sliceSizes[i] > max + balance) && qp < 128)
                    qp++;
            } else {
                if (ph.sliceSizes[i] < low && qp > 2 && balance > 0)
                    qp--;
            }
            balance += max - ph.sliceSizes[i];

            mbX += sliceMbCount;
            if (mbX == mbWidth) {
                sliceMbCount = 1 << ph.log2SliceMbWidth;
                mbX = 0;
                mbY++;
            }
        }
        ProresEncoder.writePictureHeader(ph, fork.dout());
    }

    private void transcodeSlice(Buffer inBuf, Buffer outBuf, int[] qMatLuma, int[] qMatChroma, int[] scan,
            int sliceMbCount, int mbX, int mbY, short sliceSize, int qp) throws IOException {
        DataInput inp = inBuf.dinp();
        DataOutput out = outBuf.dout();

        int hdrSize = (inp.readByte() & 0xff) >> 3;
        int qScaleOrig = clip(inp.readByte() & 0xff, 1, 224);
        int qScale = qScaleOrig > 128 ? qScaleOrig - 96 << 2 : qScaleOrig;
        int yDataSize = inp.readShort();
        int uDataSize = inp.readShort();
        int vDataSize = sliceSize - uDataSize - yDataSize - hdrSize;

        out.write(6 << 3); // hdr size
        out.write(qp); // qscale
        Buffer beforeSizes = outBuf.from(0);
        out.writeShort(0);
        out.writeShort(0);

        int beforeY = outBuf.pos;

        requant(bitstream(inBuf, yDataSize), new BitstreamWriter(outBuf.os()), sliceMbCount << 2,
                scaleMat(qMatLuma, qScale), scaleMat(qMatLumaTo, qp), scan, mbX, mbY, 0);
        int beforeCb = outBuf.pos;
        requant(bitstream(inBuf, uDataSize), new BitstreamWriter(outBuf.os()), sliceMbCount << 1,
                scaleMat(qMatChroma, qScale), scaleMat(qMatChromaTo, qp), scan, mbX, mbY, 1);
        int beforeCr = outBuf.pos;
        requant(bitstream(inBuf, vDataSize), new BitstreamWriter(outBuf.os()), sliceMbCount << 1,
                scaleMat(qMatChroma, qScale), scaleMat(qMatChromaTo, qp), scan, mbX, mbY, 2);

        out = beforeSizes.dout();
        out.writeShort(beforeCb - beforeY);
        out.writeShort(beforeCr - beforeCb);
    }
}