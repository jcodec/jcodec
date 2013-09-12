package org.jcodec.common;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class AudioUtil {
    public final static float r16 = 1f / 32768f;
    public final static float r24 = 1f / 8388608f;

    public static int toFloat(AudioFormat format, ByteBuffer buf, float[] floatBuf) {
        if (format.isBigEndian()) {
            if (format.getSampleSizeInBits() == 16) {
                return toFloat16BE(buf, floatBuf);
            } else {
                return toFloat24BE(buf, floatBuf);
            }
        } else {
            if (format.getSampleSizeInBits() == 16) {
                return toFloat16LE(buf, floatBuf);
            } else {
                return toFloat24LE(buf, floatBuf);
            }
        }
    }

    private static int toFloat24LE(ByteBuffer buf, float[] out) {
        int samples = 0;
        while (buf.remaining() >= 3 && samples < out.length) {
            out[samples++] = r24
                    * ((((buf.get() & 0xff) << 8) | ((buf.get() & 0xff) << 16) | ((buf.get() & 0xff) << 24)) >> 8);
        }
        return samples;
    }

    private static int toFloat16LE(ByteBuffer buf, float[] out) {
        int samples = 0;
        while (buf.remaining() >= 2 && samples < out.length) {
            out[samples++] = r16 * (short) ((buf.get() & 0xff) | ((buf.get() & 0xff) << 8));
        }
        return samples;
    }

    private static int toFloat24BE(ByteBuffer buf, float[] out) {
        int samples = 0;
        while (buf.remaining() >= 3 && samples < out.length) {
            out[samples++] = r24
                    * ((((buf.get() & 0xff) << 24) | ((buf.get() & 0xff) << 16) | ((buf.get() & 0xff) << 8)) >> 8);
        }
        return samples;
    }

    private static int toFloat16BE(ByteBuffer buf, float[] out) {
        int samples = 0;
        while (buf.remaining() >= 2 && samples < out.length) {
            out[samples++] = r16 * (short) (((buf.get() & 0xff) << 8) | (buf.get() & 0xff));
        }
        return samples;
    }
}
