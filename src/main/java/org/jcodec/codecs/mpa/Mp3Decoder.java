package org.jcodec.codecs.mpa;

import static org.jcodec.codecs.mpa.Mp3Bitstream.readCoeffs;
import static org.jcodec.codecs.mpa.Mp3Bitstream.readLSFScaleFactors;
import static org.jcodec.codecs.mpa.Mp3Bitstream.readScaleFactors;
import static org.jcodec.codecs.mpa.Mp3Mdct.oneLong;
import static org.jcodec.codecs.mpa.Mp3Mdct.threeShort;
import static org.jcodec.codecs.mpa.MpaConst.JOINT_STEREO;
import static org.jcodec.codecs.mpa.MpaConst.MPEG1;
import static org.jcodec.codecs.mpa.MpaConst.MPEG25_LSF;
import static org.jcodec.codecs.mpa.MpaConst.SINGLE_CHANNEL;
import static org.jcodec.codecs.mpa.MpaConst.ca;
import static org.jcodec.codecs.mpa.MpaConst.cs;
import static org.jcodec.codecs.mpa.MpaConst.frequencies;
import static org.jcodec.codecs.mpa.MpaConst.pretab;
import static org.jcodec.codecs.mpa.MpaConst.power43Tab;
import static org.jcodec.codecs.mpa.MpaConst.quantizerTab;
import static org.jcodec.codecs.mpa.MpaConst.sfbLong;
import static org.jcodec.codecs.mpa.MpaConst.sfbShort;
import static org.jcodec.codecs.mpa.MpaConst.win;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jcodec.codecs.mpa.Mp3Bitstream.Granule;
import org.jcodec.codecs.mpa.Mp3Bitstream.MP3SideInfo;
import org.jcodec.codecs.mpa.Mp3Bitstream.ScaleFactors;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class Mp3Decoder implements AudioDecoder {
    private static boolean[] ALL_TRUE = new boolean[] { true, true, true, true };

    private static final int SAMPLES_PER_BAND = 18;
    private static final int NUM_BANDS = 32;

    private ChannelSynthesizer filter[] = {null, null};
    private boolean initialized;

    final double fourByThree = (4.0 / 3.0);

    private float[][] prevBlk;
    private ByteBuffer frameData = ByteBuffer.allocate(4096);

    private int channels;
    private int sfreq;

    private float[] samples = new float[32];
    private float[] mdctIn = new float[18];
    private float[] mdctOut = new float[36];
    private float[][] dequant = new float[2][576];
    private short[][] tmpOut = new short[2][576];

    private void init(MpaHeader header) {
        float scalefactor = 32700.0f;

        channels = (header.mode == SINGLE_CHANNEL) ? 1 : 2;
        
        filter[0] = new ChannelSynthesizer(0, scalefactor);
        if (channels == 2)
            filter[1] = new ChannelSynthesizer(1, scalefactor);

        prevBlk = new float[2][NUM_BANDS * SAMPLES_PER_BAND];

        sfreq = header.sampleFreq + ((header.version == MPEG1) ? 3 : (header.version == MPEG25_LSF) ? 6 : 0);

        for (int ch = 0; ch < 2; ch++)
            Arrays.fill(prevBlk[ch], 0.0f);

        initialized = true;
    }

    private void decodeGranule(MpaHeader header, ByteBuffer output, MP3SideInfo si, BitReader br,
            ScaleFactors[] scalefac, int grInd) {
        Arrays.fill(dequant[0], 0);
        Arrays.fill(dequant[1], 0);

        for (int ch = 0; ch < channels; ch++) {
            int part2Start = br.position();

            Granule granule = si.granule[ch][grInd];
            if (header.version == MPEG1) {
                ScaleFactors old = scalefac[ch];
                boolean[] scfi = grInd == 0 ? ALL_TRUE : si.scfsi[ch];
                scalefac[ch] = readScaleFactors(br, si.granule[ch][grInd], scfi);
                mergeScaleFac(scalefac[ch], old, scfi);
            } else {
                scalefac[ch] = readLSFScaleFactors(br, header, granule, ch);
            }

            int[] coeffs = new int[NUM_BANDS * SAMPLES_PER_BAND + 4];
            int nonzero = readCoeffs(br, granule, ch, part2Start, sfreq, coeffs);
            dequantizeCoeffs(coeffs, nonzero, granule, scalefac[ch], dequant[ch]);
        }

        boolean msStereo = ((header.mode == JOINT_STEREO) && ((header.modeExtension & 0x2) != 0));
        if (msStereo && channels == 2)
            decodeMsStereo(header, si.granule[0][grInd], scalefac, dequant);

        for (int ch = 0; ch < channels; ch++) {
            float[] out = dequant[ch];
            Granule granule = si.granule[ch][grInd];

            antialias(granule, out);

            mdctDecode(ch, granule, out);

            for (int sb18 = 18; sb18 < 576; sb18 += 36) {
                for (int ss = 1; ss < SAMPLES_PER_BAND; ss += 2)
                    out[sb18 + ss] = -out[sb18 + ss];
            }

            for (int ss = 0, off = 0; ss < SAMPLES_PER_BAND; ss++, off += 32) {
                for (int sb18 = 0, sb = 0; sb18 < 576; sb18 += 18, sb++) {
                    samples[sb] = out[sb18 + ss];
                }
                filter[ch].synthesize(samples, tmpOut[ch], off);
            }
        }

        if (channels == 2) {
            appendSamplesInterleave(output, tmpOut[0], tmpOut[1], 576);
        } else {
            appendSamples(output, tmpOut[0], 576);
        }
    }
    
    public static void appendSamples(ByteBuffer buf, short[] f, int n) {
        for (int i = 0; i < n; i++) {
            buf.putShort(f[i]);
        }
    }
    
    public static void appendSamplesInterleave(ByteBuffer buf, short[] f0, short[] f1, int n) {
        for (int i = 0; i < n; i++) {
            buf.putShort(f0[i]);
            buf.putShort(f1[i]);
        }
    }

    private void mergeScaleFac(ScaleFactors sf, ScaleFactors old, boolean[] scfsi) {
        if ((!scfsi[0])) {
            for (int i = 0; i < 6; i++)
                sf.large[i] = old.large[i];
        }
        if ((!scfsi[1])) {
            for (int i = 6; i < 11; i++)
                sf.large[i] = old.large[i];
        }
        if ((!scfsi[2])) {
            for (int i = 11; i < 16; i++)
                sf.large[i] = old.large[i];
        }
        if ((!scfsi[3])) {
            for (int i = 16; i < 21; i++)
                sf.large[i] = old.large[i];
        }
    }

    private void dequantizeCoeffs(int[] input, int nonzero, Granule granule, ScaleFactors scalefac, float out[]) {
        float globalGain = (float) Math.pow(2.0, (0.25 * (granule.globalGain - 210.0)));

        if (granule.windowSwitchingFlag && (granule.blockType == 2)) {
            if (granule.mixedBlockFlag) {
                dequantMixed(input, nonzero, granule, scalefac, globalGain, out);
            } else {
                dequantShort(input, nonzero, granule, scalefac, globalGain, out);
            }
        } else {
            dequantLong(input, nonzero, granule, scalefac, globalGain, out);
        }
    }

    private void dequantMixed(int[] input, int nonzero, Granule granule, ScaleFactors scalefac, float globalGain,
            float[] out) {
        int i = 0;
        for (int sfb = 0; sfb < 8 && i < nonzero; sfb++) {
            for (; i < sfbLong[sfreq][sfb + 1] && i < nonzero; i++) {
                int idx = (scalefac.large[sfb] + (granule.preflag ? pretab[sfb] : 0)) << granule.scalefacScale;
                out[i] = globalGain * pow43(input[i]) * quantizerTab[idx];
            }
        }
        for (int sfb = 3; sfb < 12 && i < nonzero; sfb++) {
            int sfbSz = sfbShort[sfreq][sfb + 1] - sfbShort[sfreq][sfb];
            int sfbStart = i;
            for (int wnd = 0; wnd < 3; wnd ++) {
                for (int j = 0; j < sfbSz && i < nonzero; j++, i++) {
                    int idx = (scalefac.small[wnd][sfb] << granule.scalefacScale) + (granule.subblockGain[wnd] << 2);
                    // interleaving samples of the short windows
                    out[sfbStart + j * 3 + wnd] = globalGain * pow43(input[i]) * quantizerTab[idx];
                }
            }
        }
    }

    private void dequantShort(int[] input, int nonzero, Granule granule, ScaleFactors scalefac, float globalGain,
            float[] out) {
        for (int sfb = 0, i = 0; i < nonzero; sfb++) {
            int sfbSz = sfbShort[sfreq][sfb + 1] - sfbShort[sfreq][sfb];
            int sfbStart = i;
            for (int wnd = 0; wnd < 3; wnd ++) {
                for (int j = 0; j < sfbSz && i < nonzero; j++, i++) {
                    int idx = (scalefac.small[wnd][sfb] << granule.scalefacScale) + (granule.subblockGain[wnd] << 2);
                    // interleaving samples of the short windows
                    out[sfbStart + j * 3 + wnd] = globalGain * pow43(input[i]) * quantizerTab[idx];
                }
            }
        }
    }

    private void dequantLong(int[] input, int nonzero, Granule granule, ScaleFactors scalefac,
            float globalGain, float[] out) {
        for (int i = 0, sfb = 0; i < nonzero; i++) {
            if (i == sfbLong[sfreq][sfb + 1])
                ++sfb;

            int idx = (scalefac.large[sfb] + (granule.preflag ? pretab[sfb] : 0)) << granule.scalefacScale;
            out[i] = globalGain * pow43(input[i]) * quantizerTab[idx];
        }
    }

    private float pow43(int val) {
        if (val == 0) {
            return 0.0f;
        } else {
            int sign = 1 - ((val >>> 31) << 1);
            int abs = MathUtil.abs(val);

            if (abs < power43Tab.length)
                return sign * power43Tab[abs];
            else
                return sign * (float) Math.pow(abs, fourByThree);
        }
    }

    private void decodeMsStereo(MpaHeader header, Granule granule, ScaleFactors[] scalefac, float[][] ro) {
        for (int i = 0; i < 576; i++) {
            float a = ro[0][i];
            float b = ro[1][i];
            ro[0][i] = (a + b) * 0.707106781f;
            ro[1][i] = (a - b) * 0.707106781f;
        }
    }

    private void antialias(Granule granule, float[] out) {
        if (granule.windowSwitchingFlag && (granule.blockType == 2) && !granule.mixedBlockFlag)
            return;

        int bands = granule.windowSwitchingFlag && granule.mixedBlockFlag && (granule.blockType == 2) ? 1 : 31;

        for (int band = 0, bandStart = 0; band < bands; band++, bandStart += 18) {
            for (int sample = 0; sample < 8; sample++) {
                int src_idx1 = bandStart + 17 - sample;
                int src_idx2 = bandStart + 18 + sample;
                float bu = out[src_idx1];
                float bd = out[src_idx2];
                out[src_idx1] = (bu * cs[sample]) - (bd * ca[sample]);
                out[src_idx2] = (bd * cs[sample]) + (bu * ca[sample]);
            }
        }
    }

    private void mdctDecode(int ch, Granule granule, float[] out) {
        for (int sb18 = 0; sb18 < 576; sb18 += 18) {
            int blockType = (granule.windowSwitchingFlag && granule.mixedBlockFlag && (sb18 < 36)) ? 0
                    : granule.blockType;

            for (int cc = 0; cc < 18; cc++)
                mdctIn[cc] = out[cc + sb18];

            if (blockType == 2) {
                threeShort(mdctIn, mdctOut);
            } else {
                oneLong(mdctIn, mdctOut);
                for (int i = 0; i < 36; i++)
                    mdctOut[i] *= win[blockType][i];
            }

            for (int i = 0; i < 18; i++) {
                out[i + sb18] = mdctOut[i] + prevBlk[ch][sb18 + i];
                prevBlk[ch][sb18 + i] = mdctOut[18 + i];
            }
        }
    }

    @Override
    public AudioBuffer decodeFrame(ByteBuffer frame, ByteBuffer dst) throws IOException {
        MpaHeader header = MpaHeader.read_header(frame);
        if (!initialized) {
            init(header);
        }
        boolean intensityStereo = ((header.mode == JOINT_STEREO) && ((header.modeExtension & 0x1) != 0));
        if (intensityStereo)
            throw new RuntimeException("Intensity stereo is not supported.");

        dst.order(ByteOrder.LITTLE_ENDIAN);
        MP3SideInfo si = Mp3Bitstream.readSideInfo(header, frame, channels);
        int reserve = frameData.position();
        frameData.put(NIOUtils.read(frame, header.frameBytes));
        frameData.flip();
        if (header.protectionBit == 0) {
            frame.getShort();
        }
        NIOUtils.skip(frameData, reserve - si.mainDataBegin);
        BitReader br = BitReader.createBitReader(frameData);

        ScaleFactors[] scalefac = new ScaleFactors[2];
        decodeGranule(header, dst, si, br, scalefac, 0);
        if (header.version == MPEG1)
            decodeGranule(header, dst, si, br, scalefac, 1);

        br.terminate();

        NIOUtils.relocateLeftover(frameData);
        dst.flip();

        return new AudioBuffer(dst, null, 1);
    }

    @Override
    public AudioCodecMeta getCodecMeta(ByteBuffer data) throws IOException {
        MpaHeader header = MpaHeader.read_header(data.duplicate());
        AudioFormat format = new AudioFormat(frequencies[header.version][header.sampleFreq], 16,
                header.mode == SINGLE_CHANNEL ? 1 : 2, true, false);
        return AudioCodecMeta.fromAudioFormat(format);
    };
}
