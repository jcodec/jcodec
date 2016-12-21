package org.jcodec.codecs.s302;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.AudioFormat;
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

    public AudioBuffer decodeFrame(ByteBuffer frame, ByteBuffer dst) {
        frame.order(ByteOrder.BIG_ENDIAN);
        ByteBuffer dup = dst.duplicate();

        int h = frame.getInt();
        int frameSize = (h >> 16) & 0xffff;
        if (frame.remaining() != frameSize)
            throw new IllegalArgumentException("Wrong s302m frame");
        int channels = ((h >> 14) & 0x0003) * 2 + 2;
        int sampleSizeInBits = ((h >> 4) & 0x0003) * 4 + 16;

        if (sampleSizeInBits == 24) {
            int nSamples = (frame.remaining() / 7) * 2;
            while (frame.remaining() > 6) {

                byte c = (byte) MathUtil.reverse(frame.get() & 0xff);
                byte b = (byte) MathUtil.reverse(frame.get() & 0xff);
                byte a = (byte) MathUtil.reverse(frame.get() & 0xff);
                int g = MathUtil.reverse(frame.get() & 0x0f);
                int f = MathUtil.reverse(frame.get() & 0xff);
                int e = MathUtil.reverse(frame.get() & 0xff);
                int d = MathUtil.reverse(frame.get() & 0xf0);

                dup.put(a);
                dup.put(b);
                dup.put(c);
                dup.put((byte) ((d << 4) | (e >> 4)));
                dup.put((byte) ((e << 4) | (f >> 4)));
                dup.put((byte) ((f << 4) | (g >> 4)));
            }

            dup.flip();
            return new AudioBuffer(dup, new AudioFormat(SAMPLE_RATE, 24, channels, true, true), nSamples / channels);
        } else if (sampleSizeInBits == 20) {
            int nSamples = (frame.remaining() / 6) * 2;
            while (frame.remaining() > 5) {
                int c = MathUtil.reverse(frame.get() & 0xff);
                int b = MathUtil.reverse(frame.get() & 0xff);
                int a = MathUtil.reverse(frame.get() & 0xf0);
                dup.put((byte) ((a << 4) | (b >> 4)));
                dup.put((byte) ((b << 4) | (c >> 4)));
                dup.put((byte) (c << 4));

                int cc = MathUtil.reverse(frame.get() & 0xff);
                int bb = MathUtil.reverse(frame.get() & 0xff);
                int aa = MathUtil.reverse(frame.get() & 0xf0);
                dup.put((byte) ((aa << 4) | (bb >> 4)));
                dup.put((byte) ((bb << 4) | (cc >> 4)));
                dup.put((byte) (cc << 4));

            }
            dup.flip();
            return new AudioBuffer(dup, new AudioFormat(SAMPLE_RATE, 24, channels, true, true), nSamples / channels);
        } else {
            int nSamples = (frame.remaining() / 5) * 2;
            while (frame.remaining() > 4) {
                byte bb = (byte) MathUtil.reverse(frame.get() & 0xff);
                byte aa = (byte) MathUtil.reverse(frame.get() & 0xff);
                int c = MathUtil.reverse(frame.get() & 0xff);
                int b = MathUtil.reverse(frame.get() & 0xff);
                int a = MathUtil.reverse(frame.get() & 0xf0);

                dst.put(aa);
                dst.put(bb);
                dst.put((byte) ((a << 4) | (b >> 4)));
                dst.put((byte) ((b << 4) | (c >> 4)));
            }

            dup.flip();
            return new AudioBuffer(dup, new AudioFormat(SAMPLE_RATE, 16, channels, true, true), nSamples / channels);
        }
    }

    @Override
    public AudioCodecMeta getCodecMeta(ByteBuffer _data) throws IOException {
        ByteBuffer frame = _data.duplicate();
        frame.order(ByteOrder.BIG_ENDIAN);

        int h = frame.getInt();
        int frameSize = (h >> 16) & 0xffff;
        if (frame.remaining() != frameSize)
            throw new IllegalArgumentException("Wrong s302m frame");
        int channels = ((h >> 14) & 0x0003) * 2 + 2;
        int sampleSizeInBits = ((h >> 4) & 0x0003) * 4 + 16;
        return new AudioCodecMeta(new AudioFormat(SAMPLE_RATE, sampleSizeInBits, channels, true, true));
    }
}