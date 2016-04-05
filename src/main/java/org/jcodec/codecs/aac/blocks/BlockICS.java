package org.jcodec.codecs.aac.blocks;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.String.format;
import static org.jcodec.codecs.aac.Profile.LC;
import static org.jcodec.codecs.aac.Profile.MAIN;
import static org.jcodec.codecs.aac.blocks.BlockICS.BandType.INTENSITY_BT;
import static org.jcodec.codecs.aac.blocks.BlockICS.BandType.INTENSITY_BT2;
import static org.jcodec.codecs.aac.blocks.BlockICS.BandType.NOISE_BT;
import static org.jcodec.codecs.aac.blocks.BlockICS.BandType.ZERO_BT;
import static org.jcodec.common.io.VLCBuilder.createVLCBuilder;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.aac.Profile;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;
import org.jcodec.common.tools.MathUtil;

import js.lang.System;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Individual Channel Stream info, AAC block
 * 
 * @author The JCodec project
 * 
 */
public class BlockICS extends Block {

    private boolean commonWindow;
    private boolean scaleFlag;
    private Profile profile;
    private int samplingIndex;

    private static VLC[] spectral;
    private static VLC vlc;

    static {
        vlc = new VLC(AACTab.ff_aac_scalefactor_code, AACTab.ff_aac_scalefactor_bits);
        spectral = new VLC[] { createVLCBuilder(AACTab.codes1, AACTab.bits1, AACTab.codebook_vector02_idx).getVLC(),
                createVLCBuilder(AACTab.codes2, AACTab.bits2, AACTab.codebook_vector02_idx).getVLC(),
                createVLCBuilder(AACTab.codes3, AACTab.bits3, AACTab.codebook_vector02_idx).getVLC(),
                createVLCBuilder(AACTab.codes4, AACTab.bits4, AACTab.codebook_vector02_idx).getVLC(),
                createVLCBuilder(AACTab.codes5, AACTab.bits5, AACTab.codebook_vector4_idx).getVLC(),
                createVLCBuilder(AACTab.codes6, AACTab.bits6, AACTab.codebook_vector4_idx).getVLC(),
                createVLCBuilder(AACTab.codes7, AACTab.bits7, AACTab.codebook_vector6_idx).getVLC(),
                createVLCBuilder(AACTab.codes8, AACTab.bits8, AACTab.codebook_vector6_idx).getVLC(),
                createVLCBuilder(AACTab.codes9, AACTab.bits9, AACTab.codebook_vector8_idx).getVLC(),
                createVLCBuilder(AACTab.codes10, AACTab.bits10, AACTab.codebook_vector8_idx).getVLC(),
                createVLCBuilder(AACTab.codes11, AACTab.bits11, AACTab.codebook_vector10_idx).getVLC() };
    }

    float[][] ff_aac_codebook_vector_vals;

    private static final int MAX_LTP_LONG_SFB = 40;
    private int windowSequence;
    int num_window_groups;
    private int[] group_len;
    int maxSfb;
    private int[] band_type;
    private int[] band_type_run_end;
    private int globalGain;

    public BlockICS() {
        this.ff_aac_codebook_vector_vals = new float[][]{ AACTab.codebook_vector0_vals, AACTab.codebook_vector0_vals,
                AACTab.codebook_vector10_vals, AACTab.codebook_vector10_vals, AACTab.codebook_vector4_vals,
                AACTab.codebook_vector4_vals, AACTab.codebook_vector10_vals, AACTab.codebook_vector10_vals,
                AACTab.codebook_vector10_vals, AACTab.codebook_vector10_vals, AACTab.codebook_vector10_vals, };

        this.group_len = new int[8];
        this.band_type = new int[120];
        this.band_type_run_end = new int[120];
    }
    
    private static enum WindowSequence {
        ONLY_LONG_SEQUENCE, LONG_START_SEQUENCE, EIGHT_SHORT_SEQUENCE, LONG_STOP_SEQUENCE;
    }

    protected int parseICSInfo(BitReader _in) {
        _in.read1Bit();
        windowSequence = (int) _in.readNBit(2);
        int useKbWindow = _in.read1Bit();
        num_window_groups = 1;
        group_len[0] = 1;
        if (windowSequence == WindowSequence.EIGHT_SHORT_SEQUENCE.ordinal()) {
            int max_sfb = (int) _in.readNBit(4);

            for (int i = 0; i < 7; i++) {
                if (_in.read1Bit() != 0) {
                    group_len[num_window_groups - 1]++;
                } else {
                    num_window_groups++;
                    group_len[num_window_groups - 1] = 1;
                }
            }
            numSwb = AACTab.ff_aac_num_swb_128[samplingIndex];
            swbOffset = AACTab.ff_swb_offset_128[samplingIndex];
            numWindows = 8;
        } else {
            maxSfb = (int) _in.readNBit(6);
            numSwb = AACTab.ff_aac_num_swb_1024[samplingIndex];
            swbOffset = AACTab.ff_swb_offset_1024[samplingIndex];
            numWindows = 1;

            int predictor_present = _in.read1Bit();
            if (predictor_present != 0) {
                if (profile == MAIN) {
                    decodePrediction(_in, maxSfb);
                } else if (profile == LC) {
                    throw new RuntimeException("Prediction is not allowed _in AAC-LC.\n");
                } else {
                    int ltpPresent = _in.read1Bit();
                    if (ltpPresent != 0)
                        decodeLtp(_in, maxSfb);
                }
            }
        }

        return 0;
    }

    private void decodePrediction(BitReader _in, int maxSfb) {
        if (_in.read1Bit() != 0) {
            int predictorResetGroup = (int) _in.readNBit(5);
        }
        for (int sfb = 0; sfb < min(maxSfb, AACTab.maxSfbTab[samplingIndex]); sfb++) {
            _in.read1Bit();
        }
    }

    private void decodeLtp(BitReader _in, int maxSfb) {

        int lag = (int) _in.readNBit(11);
        float coef = AACTab.ltpCoefTab[(int) _in.readNBit(3)];
        for (int sfb = 0; sfb < min(maxSfb, MAX_LTP_LONG_SFB); sfb++)
            _in.read1Bit();
    }

    private void decodeBandTypes(BitReader _in) {
        int g, idx = 0;
        int bits = (windowSequence == WindowSequence.EIGHT_SHORT_SEQUENCE.ordinal()) ? 3 : 5;
        for (g = 0; g < num_window_groups; g++) {
            int k = 0;
            while (k < maxSfb) {
                int sect_end = k;
                int sect_len_incr;
                int sect_band_type = (int) _in.readNBit(4);
                if (sect_band_type == 12) {
                    throw new RuntimeException("invalid band type");
                }
                while ((sect_len_incr = (int) _in.readNBit(bits)) == (1 << bits) - 1)
                    sect_end += sect_len_incr;
                sect_end += sect_len_incr;
                if (!_in.moreData() || sect_len_incr == (1 << bits) - 1) {
                    throw new RuntimeException("Overread");
                }
                if (sect_end > maxSfb) {
                    throw new RuntimeException(format("Number of bands (%d) exceeds limit (%d).\n", sect_end, maxSfb));
                }
                for (; k < sect_end; k++) {
                    band_type[idx] = sect_band_type;
                    band_type_run_end[idx++] = sect_end;
                }
            }
        }
    }

    enum BandType {
        ZERO_BT, BT_1, BT_2, BT_3, BT_4, FIRST_PAIR_BT, BT_6, BT_7, BT_8, BT_9, BT_10, ESC_BT, BT_12, NOISE_BT, INTENSITY_BT2, INTENSITY_BT
    };

    static float[] ff_aac_pow2sf_tab = new float[428];
    private final static int POW_SF2_ZERO = 200;
    private double[] sfs;
    private int numSwb;
    private int[] swbOffset;
    private int numWindows;

    static {
        int i;
        for (i = 0; i < 428; i++)
            ff_aac_pow2sf_tab[i] = (float) pow(2, (i - POW_SF2_ZERO) / 4.);
    }

    private void decodeScalefactors(BitReader _in) {
        int[] offset = new int[] { globalGain, globalGain - 90, 0 };
        int clipped_offset;
        int noise_flag = 1;
        String[] sf_str = new String[] { "Global gain", "Noise gain", "Intensity stereo position" };
        int idx = 0;
        for (int g = 0; g < num_window_groups; g++) {
            for (int i = 0; i < maxSfb;) {
                int run_end = band_type_run_end[idx];
                if (band_type[idx] == ZERO_BT.ordinal()) {
                    for (; i < run_end; i++, idx++)
                        sfs[idx] = 0.;
                } else if ((band_type[idx] == INTENSITY_BT.ordinal()) || (band_type[idx] == INTENSITY_BT2.ordinal())) {
                    for (; i < run_end; i++, idx++) {
                        offset[2] += vlc.readVLC(_in) - 60;
                        clipped_offset = clip(offset[2], -155, 100);
                        if (offset[2] != clipped_offset) {
                            System.out.println(String.format("Intensity stereo "
                                    + "position clipped (%d -> %d).\nIf you heard an "
                                    + "audible artifact, there may be a bug _in the " + "decoder. ", offset[2],
                                    clipped_offset));
                        }
                        sfs[idx] = ff_aac_pow2sf_tab[-clipped_offset + POW_SF2_ZERO];
                    }
                } else if (band_type[idx] == NOISE_BT.ordinal()) {
                    for (; i < run_end; i++, idx++) {
                        if (noise_flag-- > 0)
                            offset[1] += _in.readNBit(9) - 256;
                        else
                            offset[1] += vlc.readVLC(_in) - 60;
                        clipped_offset = clip(offset[1], -100, 155);
                        if (offset[1] != clipped_offset) {
                            System.out.println(String.format("Noise gain clipped "
                                    + "(%d -> %d).\nIf you heard an audible "
                                    + "artifact, there may be a bug _in the decoder. ", offset[1], clipped_offset));
                        }
                        sfs[idx] = -ff_aac_pow2sf_tab[clipped_offset + POW_SF2_ZERO];
                    }
                } else {
                    for (; i < run_end; i++, idx++) {
                        offset[0] += vlc.readVLC(_in) - 60;
                        if (offset[0] > 255) {
                            throw new RuntimeException(String.format("%s (%d) out of range.\n", sf_str[0], offset[0]));
                        }
                        sfs[idx] = -ff_aac_pow2sf_tab[offset[0] - 100 + POW_SF2_ZERO];
                    }
                }
            }
        }
    }

    public static class Pulse {

        private int numPulse;
        private int[] pos;
        private int[] amp;

        public Pulse(int numPulse, int[] pos, int[] amp) {
            this.numPulse = numPulse;
            this.pos = pos;
            this.amp = amp;
        }

        public int getNumPulse() {
            return numPulse;
        }

        public int[] getPos() {
            return pos;
        }

        public int[] getAmp() {
            return amp;
        }
    }

    private Pulse decodePulses(BitReader _in) {
        int[] pos = new int[4];
        int[] amp = new int[4];

        int numPulse = (int) _in.readNBit(2) + 1;
        int pulseSwb = (int) _in.readNBit(6);
        if (pulseSwb >= numSwb)
            throw new RuntimeException("pulseSwb >= numSwb");
        pos[0] = swbOffset[pulseSwb];
        pos[0] += (int) _in.readNBit(5);
        if (pos[0] > 1023)
            throw new RuntimeException("pos[0] > 1023");
        amp[0] = (int) _in.readNBit(4);
        for (int i = 1; i < numPulse; i++) {
            pos[i] = (int) _in.readNBit(5) + pos[i - 1];
            if (pos[i] > 1023)
                throw new RuntimeException("pos[" + i + "] > 1023");
            amp[i] = (int) _in.readNBit(5);
        }
        return new Pulse(numPulse, pos, amp);
    }

    public static class Tns {

        private int[] nFilt;
        private int[][] length;
        private int[][] order;
        private int[][] direction;
        private float[][][] coeff;

        public Tns(int[] nFilt, int[][] length, int[][] order, int[][] direction, float[][][] coeff) {
            this.nFilt = nFilt;
            this.length = length;
            this.order = order;
            this.direction = direction;
            this.coeff = coeff;
        }
    }

    private Tns decodeTns(BitReader _in) {
        int is8 = windowSequence == WindowSequence.EIGHT_SHORT_SEQUENCE.ordinal() ? 1 : 0;
        int tns_max_order = is8 != 0 ? 7 : profile == Profile.MAIN ? 20 : 12;
        int[] nFilt = new int[numWindows];
        int[][] length = new int[numWindows][2];
        int[][] order = new int[numWindows][2];
        int[][] direction = new int[numWindows][2];
        float[][][] coeff = new float[numWindows][2][1 << (5 - 2 * is8)];
        for (int w = 0; w < numWindows; w++) {
            if ((nFilt[w] = (int) _in.readNBit(2 - is8)) != 0) {
                int coefRes = _in.read1Bit();

                for (int filt = 0; filt < nFilt[w]; filt++) {
                    int tmp2_idx;
                    length[w][filt] = (int) _in.readNBit(6 - 2 * is8);

                    if ((order[w][filt] = (int) _in.readNBit(5 - 2 * is8)) > tns_max_order) {
                        throw new RuntimeException(String.format("TNS filter order %d is greater than maximum %d.\n",
                                order[w][filt], tns_max_order));
                    }
                    if (order[w][filt] != 0) {
                        direction[w][filt] = _in.read1Bit();
                        int coefCompress = _in.read1Bit();
                        int coefLen = coefRes + 3 - coefCompress;
                        tmp2_idx = 2 * coefCompress + coefRes;

                        for (int i = 0; i < order[w][filt]; i++)
                            coeff[w][filt][i] = AACTab.tns_tmp2_map[tmp2_idx][(int) _in.readNBit(coefLen)];
                    }
                }
            }
        }
        return new Tns(nFilt, length, order, direction, coeff);
    }

    void VMUL4(float[] result, int idx, float[] v, int code, float scale) {
        result[idx] = v[code & 3] * scale;
        result[idx + 1] = v[code >> 2 & 3] * scale;
        result[idx + 2] = v[code >> 4 & 3] * scale;
        result[idx + 3] = v[code >> 6 & 3] * scale;
    }

    void VMUL4S(float[] result, int idx, float[] v, int code, int sign, float scale) {
        int nz = code >> 12;

        // t.i = s.i ^ (sign & 1U<<31);
        result[idx + 0] = v[idx & 3] * scale;

        sign <<= nz & 1;
        nz >>= 1;
        // t.i = s.i ^ (sign & 1U<<31);
        result[idx + 1] = v[idx >> 2 & 3] * scale;

        sign <<= nz & 1;
        nz >>= 1;
        // t.i = s.i ^ (sign & 1U<<31);
        result[idx + 2] = v[idx >> 4 & 3] * scale;

        sign <<= nz & 1;
        nz >>= 1;
        // t.i = s.i ^ (sign & 1U<<31);
        result[idx + 3] = v[idx >> 6 & 3] * scale;
    }

    void VMUL2(float[] result, int idx, float[] v, int code, float scale) {
        result[idx] = v[code & 15] * scale;
        result[idx + 1] = v[code >> 4 & 15] * scale;
    }

    void VMUL2S(float[] result, int idx, float[] v, int code, int sign, float scale) {
        result[idx] = v[code & 15] * scale;
        result[idx + 1] = v[code >> 4 & 15] * scale;
    }

    private void decodeSpectrum(BitReader _in) {
        float[] coef = new float[1024];
        int idx = 0;

        for (int g = 0; g < num_window_groups; g++) {
            for (int i = 0; i < maxSfb; i++, idx++) {
                int cbt_m1 = band_type[idx] - 1;
                if (cbt_m1 < INTENSITY_BT2.ordinal() - 1 && cbt_m1 != NOISE_BT.ordinal() - 1) {
                    float[] vq = ff_aac_codebook_vector_vals[cbt_m1];
                    VLC vlc = spectral[cbt_m1];
                    switch (cbt_m1 >> 1) {
                    case 0:
                        readBandType1And2(_in, coef, idx, g, i, vq, vlc);
                        break;

                    case 1:
                        readBandType3And4(_in, coef, idx, g, i, vq, vlc);
                        break;

                    case 2:
                        readBandType5And6(_in, coef, idx, g, i, vq, vlc);
                        break;

                    case 3:
                    case 4:
                        readBandType7Through10(_in, coef, idx, g, i, vq, vlc);
                        break;
                    default:
                        readOther(_in, coef, idx, g, i, vq, vlc);
                    }
                }
            }
        }
    }

    private void readBandType3And4(BitReader _in, float[] coef, int idx, int g, int sfb, float[] vq, VLC vlc) {
        int g_len = group_len[g];
        int cfo = swbOffset[sfb];
        int off_len = swbOffset[sfb + 1] - swbOffset[sfb];

        for (int group = 0; group < g_len; group++, cfo += 128) {
            int cf = cfo;
            int len = off_len;

            do {
                int cb_idx = vlc.readVLC(_in);
                int nnz = cb_idx >> 8 & 15;
                int bits = nnz == 0 ? 0 : _in.readNBit(nnz);
                VMUL4S(coef, cf, vq, cb_idx, bits, (float) sfs[idx]);
                cf += 4;
                len -= 4;
            } while (len > 0);
        }
    }

    private void readBandType7Through10(BitReader _in, float[] coef, int idx, int g, int sfb, float[] vq, VLC vlc) {
        int g_len = group_len[g];
        int cfo = swbOffset[sfb];
        int off_len = swbOffset[sfb + 1] - swbOffset[sfb];

        for (int group = 0; group < g_len; group++, cfo += 128) {
            int cf = cfo;
            int len = off_len;

            do {
                int cb_idx = vlc.readVLC(_in);
                int nnz = cb_idx >> 8 & 15;
                int bits = nnz == 0 ? 0 : (_in.readNBit(nnz) << (cb_idx >> 12));
                VMUL2S(coef, cf, vq, cb_idx, bits, (float) sfs[idx]);
                cf += 2;
                len -= 2;
            } while (len > 0);
        }
    }

    private void readOther(BitReader _in, float[] coef, int idx, int g, int sfb, float[] vq, VLC vlc) {
        int g_len = group_len[g];
        int cfo = swbOffset[sfb];
        int off_len = swbOffset[sfb + 1] - swbOffset[sfb];

        for (int group = 0; group < g_len; group++, cfo += 128) {
            int cf = cfo;
            int len = off_len;

            do {
                int cb_idx = vlc.readVLC(_in);

                if (cb_idx != 0) {
                    int nnz = cb_idx >> 12;
                    int nzt = cb_idx >> 8;
                    int bits = _in.readNBit(nnz) << (32 - nnz);

                    for (int j = 0; j < 2; j++) {
                        if ((nzt & 1 << j) != 0) {
                            int b;
                            int n;
                            /*
                             * The total length of escape_sequence must be < 22
                             * bits according to the specification (i.e. max is
                             * 111111110xxxxxxxxxxxx).
                             */

                            b = ProresDecoder.nZeros(~_in.checkNBit(14));

                            if (b > 8) {
                                throw new RuntimeException("error _in spectral data, ESC overflow\n");
                            }

                            _in.skip(b + 1);
                            b += 4;
                            n = (1 << b) + _in.readNBit(b);
                            coef[cf++] = MathUtil.cubeRoot(n) | (bits & 1 << 31);
                            bits <<= 1;
                        } else {
                            int v = (int) vq[cb_idx & 15];
                            coef[cf++] = (bits & 1 << 31) | v;
                            // bits <<= !!v;
                        }
                        cb_idx >>= 4;
                    }
                    cf += 2;
                    len += 2;
                }
            } while (len > 0);
        }
    }

    private void readBandType1And2(BitReader _in, float[] coef, int idx, int g, int sfb, float[] vq, VLC vlc) {
        int g_len = group_len[g];
        int cfo = swbOffset[sfb];
        int off_len = swbOffset[sfb + 1] - swbOffset[sfb];

        for (int group = 0; group < g_len; group++, cfo += 128) {
            int cf = cfo;
            int len = off_len;

            do {
                int cb_idx = vlc.readVLC(_in);
                VMUL4(coef, cf, vq, cb_idx, (float) sfs[idx]);
                cf += 4;
                len -= 4;
            } while (len > 0);
        }
    }

    private void readBandType5And6(BitReader _in, float[] coef, int idx, int g, int sfb, float[] vq, VLC vlc) {
        int g_len = group_len[g];
        int cfo = swbOffset[sfb];
        int off_len = swbOffset[sfb + 1] - swbOffset[sfb];
        for (int group = 0; group < g_len; group++, cfo += 128) {
            int cf = cfo;
            int len = off_len;

            do {
                int cb_idx = vlc.readVLC(_in);
                VMUL2(coef, cf, vq, cb_idx, (float) sfs[idx]);
                cf += 2;
                len -= 2;
            } while (len > 0);
        }
    }

    public void parse(BitReader _in) {
        globalGain = (int) _in.readNBit(8);

        if (!commonWindow && !scaleFlag) {
            parseICSInfo(_in);
        }

        decodeBandTypes(_in);
        decodeScalefactors(_in);

        int pulse_present = 0;
        int tns_present;
        if (!scaleFlag) {
            if ((pulse_present = _in.read1Bit()) != 0) {
                if (windowSequence == WindowSequence.EIGHT_SHORT_SEQUENCE.ordinal()) {
                    throw new RuntimeException("Pulse tool not allowed _in eight short sequence.");
                }
                decodePulses(_in);
            }
            if ((tns_present = _in.read1Bit()) != 0) {
                decodeTns(_in);
            }
            if (_in.read1Bit() != 0) {
                throw new RuntimeException("SSR is not supported");
            }
        }

        decodeSpectrum(_in);
    }
}
