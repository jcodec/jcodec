package org.jcodec.movtool.streaming.tracks;
import java.lang.IllegalStateException;
import java.lang.System;
import java.lang.ThreadLocal;


import static org.jcodec.common.model.Label.Center;
import static org.jcodec.common.model.Label.Discrete;
import static org.jcodec.common.model.Label.LFE2;
import static org.jcodec.common.model.Label.LFEScreen;
import static org.jcodec.common.model.Label.Left;
import static org.jcodec.common.model.Label.LeftCenter;
import static org.jcodec.common.model.Label.LeftSurround;
import static org.jcodec.common.model.Label.LeftTotal;
import static org.jcodec.common.model.Label.Mono;
import static org.jcodec.common.model.Label.RearSurroundLeft;
import static org.jcodec.common.model.Label.RearSurroundRight;
import static org.jcodec.common.model.Label.Right;
import static org.jcodec.common.model.Label.RightCenter;
import static org.jcodec.common.model.Label.RightSurround;
import static org.jcodec.common.model.Label.RightTotal;
import static org.jcodec.common.model.Label.Unused;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.IntArrayList;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Label;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Downmixes PCM audio data into 16 bit stereo track
 * 
 * @author The JCodec project
 * 
 */
public class DownmixHelper {

    private int nSamples;
    private ThreadLocal<float[][]> fltBuf;
    private float[][] matrix;
    private int[][] counts;
    private int[][] channels;
    private AudioCodecMeta[] se;

    public DownmixHelper(AudioCodecMeta[] se, int nSamples, boolean[][] solo) {
        this.fltBuf = new ThreadLocal<float[][]>();
        this.nSamples = nSamples;
        this.se = se;

        List<float[]> matrixBuilder = new ArrayList<float[]>();
        List<int[]> countsBuilder = new ArrayList<int[]>();
        List<int[]> channelsBuilder = new ArrayList<int[]>();
        for (int tr = 0; tr < se.length; tr++) {
            Label[] channels = se[tr].getChannelLabels();
            IntArrayList tmp = IntArrayList.createIntArrayList();
            for (int ch = 0; ch < channels.length; ch++) {
                if (solo != null && !solo[tr][ch])
                    continue;
                tmp.add(ch);
                Label label = channels[ch];
                if (label == Left || label == LeftTotal || label == LeftCenter) {
                    matrixBuilder.add(new float[] { 1f, 0f });
                    countsBuilder.add(new int[] { 1, 0 });
                } else if (label == LeftSurround || label == RearSurroundLeft ) {
                    matrixBuilder.add(new float[] { .7f, 0f });
                    countsBuilder.add(new int[] { 1, 0 });
                } else if (label == Right || label == RightTotal || label == RightCenter ) {
                    matrixBuilder.add(new float[] { 0f, 1f });
                    countsBuilder.add(new int[] { 0, 1 });
                } else if (label == RightSurround || label == RearSurroundRight ) {
                    matrixBuilder.add(new float[] { 0f, .7f });
                    countsBuilder.add(new int[] { 0, 1 });
                } else if (label == Mono || label == LFEScreen || label == Center || label == LFE2 || label == Discrete ) {
                    matrixBuilder.add(new float[] { .7f, .7f });
                    countsBuilder.add(new int[] { 1, 1 });
                } else if (label == Unused) {
                } else {
                    if((label.getVal() >>> 16) == 1) {
                        matrixBuilder.add(new float[] { .7f, .7f });
                        countsBuilder.add(new int[] { 1, 1 });
                        Logger.info("Discrete" + (label.getVal() & 0xffff));
                    }
                }
            }
            channelsBuilder.add(tmp.toArray());
        }
        matrix = matrixBuilder.toArray(new float[0][]);
        counts = countsBuilder.toArray(new int[0][]);
        channels = channelsBuilder.toArray(new int[0][]);
    }

    public void downmix(ByteBuffer[] data, ByteBuffer out) {
        out.order(ByteOrder.LITTLE_ENDIAN);
        
        if (matrix.length == 0) {
            out.limit(nSamples << 2);
            return;
        }

        float[][] flt = fltBuf.get();
        if (flt == null) {
            flt = new float[matrix.length][nSamples];
            fltBuf.set(flt);
        }

        for (int tr = 0, i = 0; tr < se.length; tr++) {
            for (int ch = 0; ch < channels[tr].length; ch++, i++) {
                toFloat(flt[i], se[tr], data[tr], channels[tr][ch], se[tr].getChannelCount());
            }
        }

        for (int s = 0; s < nSamples; s++) {
            int lcount = 0, rcount = 0;
            float lSum = 0, lMul = 1, rSum = 0, rMul = 1;
            for (int inp = 0; inp < matrix.length; inp++) {
                float sample = flt[inp][s];
                float l = matrix[inp][0] * sample;
                float r = matrix[inp][1] * sample;
                lSum += l;
                lMul *= l;
                rSum += r;
                rMul *= r;
                lcount += counts[inp][0];
                rcount += counts[inp][1];
            }

            float outLeft = lcount > 1 ? clamp1f(lSum - lMul) : lSum;
            float outRight = rcount > 1 ? clamp1f(rSum - rMul) : rSum;
            short left = (short) (outLeft * 32767f);
            short right = (short) (outRight * 32767f);
            out.putShort(left);
            out.putShort(right);
        }

        out.flip();
    }

    private void toFloat(float[] fSamples, AudioCodecMeta se, ByteBuffer bb, int ch, int nCh) {

        byte[] ba;
        int off, len;

        if (bb.hasArray()) {
            ba = bb.array();
            off = bb.arrayOffset() + bb.position();
            len = bb.remaining();
        } else {
            ba = NIOUtils.toArray(bb);
            off = 0;
            len = ba.length;
        }

        int maxSamples;
        if (se.getSampleSize() == 3) {
            int step = nCh * 3;
            maxSamples = Math.min(nSamples, len / step);
            if (se.getEndian() == ByteOrder.BIG_ENDIAN) {
                for (int s = 0, bi = off + ch * 3; s < maxSamples; s++, bi += step) {
                    fSamples[s] = nextSample24BE(ba, bi);
                }
            } else {
                for (int s = 0, bi = off + ch * 3; s < maxSamples; s++, bi += step) {
                    fSamples[s] = nextSample24LE(ba, bi);
                }
            }
        } else {
            int step = nCh * 2;
            maxSamples = Math.min(nSamples, len / step);
            if (se.getEndian() == ByteOrder.BIG_ENDIAN) {
                for (int s = 0, bi = off + ch * 2; s < maxSamples; s++, bi += step) {
                    fSamples[s] = nextSample16BE(ba, bi);
                }
            } else {
                for (int s = 0, bi = off + ch * 2; s < maxSamples; s++, bi += step) {
                    fSamples[s] = nextSample16LE(ba, bi);
                }
            }
        }
        for (int s = maxSamples; s < nSamples; s++)
            fSamples[s] = 0;
    }

    public final static float clamp1f(float f) {
        if (f > 1f)
            return 1f;
        if (f < -1f)
            return -1f;
        return f;
    }

    private static float rev = 1f / 2147483647;

    private static final float nextSample24BE(byte[] ba, int bi) {
        return rev * (((ba[bi] & 0xff) << 24) | ((ba[bi + 1] & 0xff) << 16) | ((ba[bi + 2] & 0xff) << 8));
    }

    private static final float nextSample24LE(byte[] ba, int bi) {
        return rev * (((ba[bi] & 0xff) << 8) | ((ba[bi + 1] & 0xff) << 16) | ((ba[bi + 2] & 0xff) << 24));
    }

    private static final float nextSample16BE(byte[] ba, int bi) {
        return rev * (((ba[bi] & 0xff) << 24) | ((ba[bi + 1] & 0xff) << 16));
    }

    private static final float nextSample16LE(byte[] ba, int bi) {
        return rev * (((ba[bi] & 0xff) << 16) | ((ba[bi + 1] & 0xff) << 24));
    }
}