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
    private static final float f24 = 8388607f;
    private static final float f16 = 32767f;

    public final static float r16 = 1f / 32768f;
    public final static float r24 = 1f / 8388608f;

    /**
     * Converts PCM samples stored in buf and described with format to float
     * array representation
     * 
     * @param format
     *            Supported formats - *_*_S16_LE, *_*_S24_LE, *_*_S16_BE,
     *            *_*_S24_LE
     * @param buf
     * @param floatBuf
     * @return Total number of samples read from the buffer
     */
    public static int toFloat(AudioFormat format, ByteBuffer buf, float[] floatBuf) {
        if (!format.isSigned())
            throw new IllegalArgumentException("Unsigned PCM is not supported ( yet? ).");

        if (format.getSampleSizeInBits() != 16 && format.getSampleSizeInBits() != 24)
            throw new IllegalArgumentException(format.getSampleSizeInBits() + " bit PCM is not supported ( yet? ).");

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

    /**
     * Converts float PCM samples stored in floatBuf to integer representation
     * according to format and stores them in buf
     * 
     * @param format
     *            Supported formats - *_*_S16_LE, *_*_S24_LE, *_*_S16_BE,
     *            *_*_S24_LE
     * @param buf
     * @param floatBuf
     * @return Total number of samples written to the buffer
     */
    public static int fromFloat(float[] floatBuf, int len, AudioFormat format, ByteBuffer buf) {
        if (!format.isSigned())
            throw new IllegalArgumentException("Unsigned PCM is not supported ( yet? ).");

        if (format.getSampleSizeInBits() != 16 && format.getSampleSizeInBits() != 24)
            throw new IllegalArgumentException(format.getSampleSizeInBits() + " bit PCM is not supported ( yet? ).");

        if (format.isBigEndian()) {
            if (format.getSampleSizeInBits() == 16) {
                return fromFloat16BE(buf, floatBuf, len);
            } else {
                return fromFloat24BE(buf, floatBuf, len);
            }
        } else {
            if (format.getSampleSizeInBits() == 16) {
                return fromFloat16LE(buf, floatBuf, len);
            } else {
                return fromFloat24LE(buf, floatBuf, len);
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

    private static int fromFloat24LE(ByteBuffer buf, float[] out, int len) {
        int samples = 0;
        while (buf.remaining() >= 3 && samples < len) {
            int val = ((int) (out[samples++] * f24)) & 0xffffff;
            buf.put((byte) val);
            buf.put((byte) (val >> 8));
            buf.put((byte) (val >> 16));
        }
        return samples;
    }

    private static int fromFloat16LE(ByteBuffer buf, float[] out, int len) {
        int samples = 0;
        while (buf.remaining() >= 2 && samples < len) {
            int val = ((int) (out[samples++] * f16)) & 0xffff;
            buf.put((byte) val);
            buf.put((byte) (val >> 8));
        }
        return samples;
    }

    private static int fromFloat24BE(ByteBuffer buf, float[] out, int len) {
        int samples = 0;
        while (buf.remaining() >= 3 && samples < len) {
            int val = ((int) (out[samples++] * f24)) & 0xffffff;
            buf.put((byte) (val >> 16));
            buf.put((byte) (val >> 8));
            buf.put((byte) val);
        }
        return samples;
    }

    private static int fromFloat16BE(ByteBuffer buf, float[] out, int len) {
        int samples = 0;
        while (buf.remaining() >= 2 && samples < len) {
            int val = ((int) (out[samples++] * f16)) & 0xffff;
            buf.put((byte) (val >> 8));
            buf.put((byte) val);
        }
        return samples;
    }

    public static int fromInt(int[] data, int len, AudioFormat format, ByteBuffer buf) {
        if (!format.isSigned())
            throw new IllegalArgumentException("Unsigned PCM is not supported ( yet? ).");

        if (format.getSampleSizeInBits() != 16 && format.getSampleSizeInBits() != 24)
            throw new IllegalArgumentException(format.getSampleSizeInBits() + " bit PCM is not supported ( yet? ).");

        if (format.isBigEndian()) {
            if (format.getSampleSizeInBits() == 16) {
                return fromInt16BE(buf, data, len);
            } else {
                return fromInt24BE(buf, data, len);
            }
        } else {
            if (format.getSampleSizeInBits() == 16) {
                return fromInt16LE(buf, data, len);
            } else {
                return fromInt24LE(buf, data, len);
            }
        }
    }

    private static int fromInt24LE(ByteBuffer buf, int[] out, int len) {
        int samples = 0;
        while (buf.remaining() >= 3 && samples < len) {
            int val = out[samples++];
            buf.put((byte) val);
            buf.put((byte) (val >> 8));
            buf.put((byte) (val >> 16));
        }
        return samples;
    }

    private static int fromInt16LE(ByteBuffer buf, int[] out, int len) {
        int samples = 0;
        while (buf.remaining() >= 2 && samples < len) {
            int val = out[samples++];
            buf.put((byte) val);
            buf.put((byte) (val >> 8));
        }
        return samples;
    }

    private static int fromInt24BE(ByteBuffer buf, int[] out, int len) {
        int samples = 0;
        while (buf.remaining() >= 3 && samples < len) {
            int val = out[samples++];
            buf.put((byte) (val >> 16));
            buf.put((byte) (val >> 8));
            buf.put((byte) val);
        }
        return samples;
    }

    private static int fromInt16BE(ByteBuffer buf, int[] out, int len) {
        int samples = 0;
        while (buf.remaining() >= 2 && samples < len) {
            int val = out[samples++];
            buf.put((byte) (val >> 8));
            buf.put((byte) val);
        }
        return samples;
    }

    public static int toInt(AudioFormat format, ByteBuffer buf, int[] samples) {
        if (!format.isSigned())
            throw new IllegalArgumentException("Unsigned PCM is not supported ( yet? ).");

        if (format.getSampleSizeInBits() != 16 && format.getSampleSizeInBits() != 24)
            throw new IllegalArgumentException(format.getSampleSizeInBits() + " bit PCM is not supported ( yet? ).");

        if (format.isBigEndian()) {
            if (format.getSampleSizeInBits() == 16) {
                return toInt16BE(buf, samples);
            } else {
                return toInt24BE(buf, samples);
            }
        } else {
            if (format.getSampleSizeInBits() == 16) {
                return toInt16LE(buf, samples);
            } else {
                return toInt24LE(buf, samples);
            }
        }
    }

    private static int toInt24LE(ByteBuffer buf, int[] out) {
        int samples = 0;
        while (buf.remaining() >= 3 && samples < out.length) {
            out[samples++] = (((buf.get() & 0xff) << 8) | ((buf.get() & 0xff) << 16) | ((buf.get() & 0xff) << 24)) >> 8;
        }
        return samples;
    }

    private static int toInt16LE(ByteBuffer buf, int[] out) {
        int samples = 0;
        while (buf.remaining() >= 2 && samples < out.length) {
            out[samples++] = (short) ((buf.get() & 0xff) | ((buf.get() & 0xff) << 8));
        }
        return samples;
    }

    private static int toInt24BE(ByteBuffer buf, int[] out) {
        int samples = 0;
        while (buf.remaining() >= 3 && samples < out.length) {
            out[samples++] = (((buf.get() & 0xff) << 24) | ((buf.get() & 0xff) << 16) | ((buf.get() & 0xff) << 8)) >> 8;
        }
        return samples;
    }

    private static int toInt16BE(ByteBuffer buf, int[] out) {
        int samples = 0;
        while (buf.remaining() >= 2 && samples < out.length) {
            out[samples++] = (short) (((buf.get() & 0xff) << 8) | (buf.get() & 0xff));
        }
        return samples;
    }

    /**
     * Interleaves audio samples in ins into outb using sample size from the
     * format
     * 
     * @param format
     * @param ins
     * @param outb
     */
    public static void interleave(AudioFormat format, ByteBuffer[] ins, ByteBuffer outb) {
        int bytesPerSample = format.getSampleSizeInBits() >> 3;
        int bytesPerFrame = bytesPerSample * ins.length;

        int max = 0;
        for (int i = 0; i < ins.length; i++)
            if (ins[i].remaining() > max)
                max = ins[i].remaining();

        for (int frames = 0; frames < max && outb.remaining() >= bytesPerFrame; frames++) {
            for (int j = 0; j < ins.length; j++) {
                if (ins[j].remaining() < bytesPerSample) {
                    for (int i = 0; i < bytesPerSample; i++)
                        outb.put((byte) 0);
                } else {
                    for (int i = 0; i < bytesPerSample; i++) {
                        outb.put(ins[j].get());
                    }
                }
            }
        }
    }

    /**
     * Deinterleaves audio samples from inb into outs using sample size from
     * format
     * 
     * @param format
     * @param inb
     * @param outs
     */
    public static void deinterleave(AudioFormat format, ByteBuffer inb, ByteBuffer[] outs) {
        int bytesPerSample = format.getSampleSizeInBits() >> 3;
        int bytesPerFrame = bytesPerSample * outs.length;
        
        while (inb.remaining() >= bytesPerFrame) {
            for (int j = 0; j < outs.length; j++) {
                for (int i = 0; i < bytesPerSample; i++) {
                    outs[j].put(inb.get());
                }
            }
        }
    }
}