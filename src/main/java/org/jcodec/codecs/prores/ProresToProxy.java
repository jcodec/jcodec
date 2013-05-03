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

import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresConsts.FrameHeader;
import org.jcodec.codecs.prores.ProresConsts.PictureHeader;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;

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

    void requant(BitReader ib, BitWriter ob, int blocksPerSlice, int[] qMatFrom, int[] qMatTo, int[] scan, int mbX,
            int mbY, int plane) {
        int[] out = new int[blocksPerSlice << 6];
        try {
            readDCCoeffs(ib, qMatFrom, out, blocksPerSlice, 64);
            readACCoeffs(ib, qMatFrom, out, blocksPerSlice, scan, nCoeffs, 6);
        } catch (RuntimeException e) {
        }
        for (int i = 0; i < out.length; i++)
            out[i] <<= 2;
        writeDCCoeffs(ob, qMatTo, out, blocksPerSlice);
        writeACCoeffs(ob, qMatTo, out, blocksPerSlice, scan, nCoeffs);
        ob.flush();
    }

    public void transcode(ByteBuffer inBuf, ByteBuffer outBuf) {
        ByteBuffer fork = outBuf.duplicate();

        FrameHeader fh = ProresDecoder.readFrameHeader(inBuf);
        ProresEncoder.writeFrameHeader(outBuf, fh);

        int beforePicture = outBuf.position();
        if (fh.frameType == 0) {
            transcodePicture(inBuf, outBuf, fh);
        } else {
            transcodePicture(inBuf, outBuf, fh);
            transcodePicture(inBuf, outBuf, fh);
        }
        fh.qMatLuma = qMatLumaTo;
        fh.qMatChroma = qMatChromaTo;
        fh.payloadSize = outBuf.position() - beforePicture;
        ProresEncoder.writeFrameHeader(fork, fh);
    }

    private void transcodePicture(ByteBuffer inBuf, ByteBuffer outBuf, FrameHeader fh) {

        PictureHeader ph = ProresDecoder.readPictureHeader(inBuf);
        ProresEncoder.writePictureHeader(ph.log2SliceMbWidth, ph.sliceSizes.length, outBuf);
        ByteBuffer sliceSizes = outBuf.duplicate();
        outBuf.position(outBuf.position() + (ph.sliceSizes.length << 1));

        int mbX = 0, mbY = 0;
        int mbWidth = (fh.width + 15) >> 4;
        int sliceMbCount = 1 << ph.log2SliceMbWidth;
        int balance = 0, qp = START_QP;
        for (int i = 0; i < ph.sliceSizes.length; i++) {

            while (mbWidth - mbX < sliceMbCount)
                sliceMbCount >>= 1;

            int savedPoint = outBuf.position();

            transcodeSlice(inBuf, outBuf, fh.qMatLuma, fh.qMatChroma, fh.scan, sliceMbCount, mbX, mbY,
                    ph.sliceSizes[i], qp);
            sliceSizes.putShort((short) (outBuf.position() - savedPoint));

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
    }

    private void transcodeSlice(ByteBuffer inBuf, ByteBuffer outBuf, int[] qMatLuma, int[] qMatChroma, int[] scan,
            int sliceMbCount, int mbX, int mbY, short sliceSize, int qp) {

        int hdrSize = (inBuf.get() & 0xff) >> 3;
        int qScaleOrig = clip(inBuf.get() & 0xff, 1, 224);
        int qScale = qScaleOrig > 128 ? qScaleOrig - 96 << 2 : qScaleOrig;
        int yDataSize = inBuf.getShort();
        int uDataSize = inBuf.getShort();
        int vDataSize = sliceSize - uDataSize - yDataSize - hdrSize;

        outBuf.put((byte) (6 << 3)); // hdr size
        outBuf.put((byte) qp); // qscale
        ByteBuffer beforeSizes = outBuf.duplicate();
        outBuf.putInt(0);

        int beforeY = outBuf.position();

        requant(bitstream(inBuf, yDataSize), new BitWriter(outBuf), sliceMbCount << 2, scaleMat(qMatLuma, qScale),
                scaleMat(qMatLumaTo, qp), scan, mbX, mbY, 0);
        int beforeCb = outBuf.position();
        requant(bitstream(inBuf, uDataSize), new BitWriter(outBuf), sliceMbCount << 1, scaleMat(qMatChroma, qScale),
                scaleMat(qMatChromaTo, qp), scan, mbX, mbY, 1);
        int beforeCr = outBuf.position();
        requant(bitstream(inBuf, vDataSize), new BitWriter(outBuf), sliceMbCount << 1, scaleMat(qMatChroma, qScale),
                scaleMat(qMatChromaTo, qp), scan, mbX, mbY, 2);

        beforeSizes.putShort((short) (beforeCb - beforeY));
        beforeSizes.putShort((short) (beforeCr - beforeCb));
    }
}