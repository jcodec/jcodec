package org.jcodec.codecs.pcmdvd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.AudioDecoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.AudioBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * PCM DVD decoder
 * 
 * @author The JCodec project
 * 
 */
public class PCMDVDDecoder implements AudioDecoder {
    private static final int[] lpcm_freq_tab = new int[] { 48000, 96000, 44100, 32000 };

    @Override
    public AudioBuffer decodeFrame(ByteBuffer _frame, ByteBuffer _dst) throws IOException {
        ByteBuffer dst = _dst.duplicate();
        ByteBuffer frame = _frame.duplicate();

        frame.order(ByteOrder.BIG_ENDIAN);
        dst.order(ByteOrder.LITTLE_ENDIAN);

        int dvdaudioSubstreamType = frame.get() & 0xff;
        NIOUtils.skip(frame, 3);

        if ((dvdaudioSubstreamType & 0xe0) == 0xa0) {
            // OK CODEC_ID_PCM_DVD
        } else if ((dvdaudioSubstreamType & 0xe0) == 0x80) {
            if ((dvdaudioSubstreamType & 0xf8) == 0x88)
                throw new RuntimeException("CODEC_ID_DTS");
            else
                throw new RuntimeException("CODEC_ID_AC3");
        } else
            throw new RuntimeException("MPEG DVD unknown coded");

        // emphasis (1), muse(1), reserved(1), frame number(5)
        int b0 = frame.get() & 0xff;
        // quant (2), freq(2), reserved(1), channels(3)
        int b1 = frame.get() & 0xff;
        // dynamic range control (0x80 = off)
        int b2 = frame.get() & 0xff;
        int freq = (b1 >> 4) & 3;
        int sampleRate = lpcm_freq_tab[freq];
        int channelCount = 1 + (b1 & 7);
        int sampleSizeInBits = 16 + ((b1 >> 6) & 3) * 4;
        int nFrames = frame.remaining() / (channelCount * (sampleSizeInBits >> 3));

        switch (sampleSizeInBits) {
        case 20:
            for (int n = 0; n < (nFrames >> 1); n++) {
                for (int c = 0; c < channelCount; c++) {
                    short s0 = frame.getShort();
                    dst.putShort(s0);
                    short s1 = frame.getShort();
                    dst.putShort(s1);
                }
                NIOUtils.skip(frame, channelCount);
            }
            break;
        case 24:
            for (int n = 0; n < (nFrames >> 1); n++) {
                for (int c = 0; c < channelCount; c++) {
                    short s0 = frame.getShort();
                    dst.putShort(s0);
                    short s1 = frame.getShort();
                    dst.putShort(s1);
                }
                NIOUtils.skip(frame, channelCount << 1);
            }
            break;
        }
        dst.flip();

        return new AudioBuffer(dst, new AudioFormat(sampleRate, 16, channelCount, true, false), nFrames);
    }
}
