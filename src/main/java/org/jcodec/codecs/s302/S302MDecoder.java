package org.jcodec.codecs.s302;

import java.io.DataInput;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.AudioDecoder;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * SMPTE 302m decoder
 * 
 * @author The JCodec project
 * 
 */
public class S302MDecoder implements AudioDecoder {
    public static int SAMPLE_RATE = 48000;

    public AudioBuffer decodeFrame(Buffer frame, byte[] dst) throws IOException {
        DataInput dinp = frame.dinp();

        int h = dinp.readInt();
        int frameSize = (h >> 16) & 0xffff;
        if (frame.remaining() != frameSize)
            throw new IllegalArgumentException("Wrong s302m frame");
        int channels = ((h >> 14) & 0x0003) * 2 + 2;
        int sampleSizeInBits = ((h >> 4) & 0x0003) * 4 + 16;

        byte[] src = frame.buffer;
        int srcPos = frame.pos, dstPos = 0, bufSize = frame.remaining();

        if (sampleSizeInBits == 24) {
            int nSamples = (bufSize / 7) * 2;
            for (; bufSize > 6; bufSize -= 7) {

                dst[dstPos++] = (byte) MathUtil.reverse(src[srcPos + 2] & 0xff);
                dst[dstPos++] = (byte) MathUtil.reverse(src[srcPos + 1] & 0xff);
                dst[dstPos++] = (byte) MathUtil.reverse(src[srcPos + 0] & 0xff);

                int a = MathUtil.reverse(src[srcPos + 6] & 0xf0);
                int b = MathUtil.reverse(src[srcPos + 5] & 0xff);
                int c = MathUtil.reverse(src[srcPos + 4] & 0xff);
                int d = MathUtil.reverse(src[srcPos + 3] & 0x0f);

                dst[dstPos++] = (byte) ((a << 4) | (b >> 4));
                dst[dstPos++] = (byte) ((b << 4) | (c >> 4));
                dst[dstPos++] = (byte) ((c << 4) | (d >> 4));

                srcPos += 7;
            }

            return new AudioBuffer(new Buffer(dst, 0, (frameSize * 6) / 7), new AudioFormat(SAMPLE_RATE, 24, channels,
                    true, true), nSamples / channels);
        } else if (sampleSizeInBits == 20) {
            int nSamples = (bufSize / 6) * 2;
            for (; bufSize > 5; bufSize -= 6) {
                int a = MathUtil.reverse(src[srcPos + 2] & 0xf0);
                int b = MathUtil.reverse(src[srcPos + 1] & 0xff);
                int c = MathUtil.reverse(src[srcPos + 0] & 0xff);
                dst[dstPos++] = (byte) ((a << 4) | (b >> 4));
                dst[dstPos++] = (byte) ((b << 4) | (c >> 4));
                dst[dstPos++] = (byte) (c << 4);

                srcPos += 3;

                a = MathUtil.reverse(src[srcPos + 2] & 0xf0);
                b = MathUtil.reverse(src[srcPos + 1] & 0xff);
                c = MathUtil.reverse(src[srcPos + 0] & 0xff);
                dst[dstPos++] = (byte) ((a << 4) | (b >> 4));
                dst[dstPos++] = (byte) ((b << 4) | (c >> 4));
                dst[dstPos++] = (byte) (c << 4);

                srcPos += 3;
            }
            return new AudioBuffer(new Buffer(dst, 0, frameSize),
                    new AudioFormat(SAMPLE_RATE, 24, channels, true, true), nSamples / channels);
        } else {
            int nSamples = (bufSize / 5) * 2;
            for (; bufSize > 4; bufSize -= 5) {
                dst[dstPos++] = (byte) MathUtil.reverse(src[srcPos + 1] & 0xff);
                dst[dstPos++] = (byte) MathUtil.reverse(src[srcPos + 0] & 0xff);

                int a = MathUtil.reverse(src[srcPos + 4] & 0xf0);
                int b = MathUtil.reverse(src[srcPos + 3] & 0xff);
                int c = MathUtil.reverse(src[srcPos + 2] & 0xff);

                dst[dstPos++] = (byte) ((a << 4) | (b >> 4));
                dst[dstPos++] = (byte) ((b << 4) | (c >> 4));

                srcPos += 5;
            }
            return new AudioBuffer(new Buffer(dst, 0, (frameSize * 4) / 5), new AudioFormat(SAMPLE_RATE, 16, channels,
                    true, true), nSamples / channels);
        }
    }
}