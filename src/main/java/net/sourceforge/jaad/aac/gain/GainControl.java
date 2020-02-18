package net.sourceforge.jaad.aac.gain;

import static java.lang.System.arraycopy;

import org.jcodec.common.io.BitReader;
import org.jcodec.platform.Platform;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.ICSInfo.WindowSequence;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
public class GainControl implements GCConstants {

    private final int frameLen, lbLong, lbShort;
    private final IMDCT imdct;
    private final IPQF ipqf;
    private final float[] buffer1, _function;
    private final float[][] buffer2, overlap;
    private int maxBand;
    private int[][][] level, levelPrev;
    private int[][][] location, locationPrev;

    public GainControl(int frameLen) {
        this.frameLen = frameLen;
        lbLong = frameLen / BANDS;
        lbShort = lbLong / 8;
        imdct = new IMDCT(frameLen);
        ipqf = new IPQF();
        levelPrev = new int[0][][];
        locationPrev = new int[0][][];
        buffer1 = new float[frameLen / 2];
        buffer2 = new float[BANDS][lbLong];
        _function = new float[lbLong * 2];
        overlap = new float[BANDS][lbLong * 2];
    }

    public void decode(BitReader _in, WindowSequence winSeq) throws AACException {
        maxBand = _in.readNBit(2) + 1;

        int wdLen, locBits, locBits2 = 0;
        switch (winSeq) {
        case ONLY_LONG_SEQUENCE:
            wdLen = 1;
            locBits = 5;
            locBits2 = 5;
            break;
        case EIGHT_SHORT_SEQUENCE:
            wdLen = 8;
            locBits = 2;
            locBits2 = 2;
            break;
        case LONG_START_SEQUENCE:
            wdLen = 2;
            locBits = 4;
            locBits2 = 2;
            break;
        case LONG_STOP_SEQUENCE:
            wdLen = 2;
            locBits = 4;
            locBits2 = 5;
            break;
        default:
            return;
        }
        level = new int[maxBand][wdLen][];
        location = new int[maxBand][wdLen][];

        int wd, k, len, bits;
        for (int bd = 1; bd < maxBand; bd++) {
            for (wd = 0; wd < wdLen; wd++) {
                len = _in.readNBit(3);
                level[bd][wd] = new int[len];
                location[bd][wd] = new int[len];
                for (k = 0; k < len; k++) {
                    level[bd][wd][k] = _in.readNBit(4);
                    bits = (wd == 0) ? locBits : locBits2;
                    location[bd][wd][k] = _in.readNBit(bits);
                }
            }
        }
    }

    public void process(float[] data, int winShape, int winShapePrev, WindowSequence winSeq) throws AACException {
        imdct.process(data, buffer1, winShape, winShapePrev, winSeq);

        for (int i = 0; i < BANDS; i++) {
            compensate(buffer1, buffer2, winSeq, i);
        }

        ipqf.process(buffer2, frameLen, maxBand, data);
    }

    /**
     * gain compensation and overlap-add: - the gain control function is calculated
     * - the gain control function applies to IMDCT output samples as a another
     * IMDCT window - the reconstructed time domain signal produces by overlap-add
     */
    private void compensate(float[] _in, float[][] out, WindowSequence winSeq, int band) {
        int j;
        if (winSeq.equals(WindowSequence.EIGHT_SHORT_SEQUENCE)) {
            int a, b;
            for (int k = 0; k < 8; k++) {
                // calculation
                calculateFunctionData(lbShort * 2, band, winSeq, k);
                // applying
                for (j = 0; j < lbShort * 2; j++) {
                    a = band * lbLong * 2 + k * lbShort * 2 + j;
                    _in[a] *= _function[j];
                }
                // overlapping
                for (j = 0; j < lbShort; j++) {
                    a = j + lbLong * 7 / 16 + lbShort * k;
                    b = band * lbLong * 2 + k * lbShort * 2 + j;
                    overlap[band][a] += _in[b];
                }
                // store for next frame
                for (j = 0; j < lbShort; j++) {
                    a = j + lbLong * 7 / 16 + lbShort * (k + 1);
                    b = band * lbLong * 2 + k * lbShort * 2 + lbShort + j;

                    overlap[band][a] = _in[b];
                }
                locationPrev[band][0] = Platform.copyOfInt(location[band][k], location[band][k].length);
                levelPrev[band][0] = Platform.copyOfInt(level[band][k], level[band][k].length);
            }
            arraycopy(overlap[band], 0, out[band], 0, lbLong);
            arraycopy(overlap[band], lbLong, overlap[band], 0, lbLong);
        } else {
            // calculation
            calculateFunctionData(lbLong * 2, band, winSeq, 0);
            // applying
            for (j = 0; j < lbLong * 2; j++) {
                _in[band * lbLong * 2 + j] *= _function[j];
            }
            // overlapping
            for (j = 0; j < lbLong; j++) {
                out[band][j] = overlap[band][j] + _in[band * lbLong * 2 + j];
            }
            // store for next frame
            for (j = 0; j < lbLong; j++) {
                overlap[band][j] = _in[band * lbLong * 2 + lbLong + j];
            }
            final int lastBlock = winSeq.equals(WindowSequence.ONLY_LONG_SEQUENCE) ? 1 : 0;
            locationPrev[band][0] = Platform.copyOfInt(location[band][lastBlock], location[band][lastBlock].length);
            levelPrev[band][0] = Platform.copyOfInt(level[band][lastBlock], level[band][lastBlock].length);
        }
    }

    // produces gain control function data, stores it in 'function' array
    private void calculateFunctionData(int samples, int band, WindowSequence winSeq, int blockID) {
        final int[] locA = new int[10];
        final float[] levA = new float[10];
        final float[] modFunc = new float[samples];
        final float[] buf1 = new float[samples / 2];
        final float[] buf2 = new float[samples / 2];
        final float[] buf3 = new float[samples / 2];

        int maxLocGain0 = 0, maxLocGain1 = 0, maxLocGain2 = 0;
        switch (winSeq) {
        case ONLY_LONG_SEQUENCE:
        case EIGHT_SHORT_SEQUENCE:
            maxLocGain0 = maxLocGain1 = samples / 2;
            maxLocGain2 = 0;
            break;
        case LONG_START_SEQUENCE:
            maxLocGain0 = samples / 2;
            maxLocGain1 = samples * 7 / 32;
            maxLocGain2 = samples / 16;
            break;
        case LONG_STOP_SEQUENCE:
            maxLocGain0 = samples / 16;
            maxLocGain1 = samples * 7 / 32;
            maxLocGain2 = samples / 2;
            break;
        }

        // calculate the fragment modification functions
        // for the first half region
        calculateFMD(band, 0, true, maxLocGain0, samples, locA, levA, buf1);

        // for the latter half region
        int block = (winSeq.equals(WindowSequence.EIGHT_SHORT_SEQUENCE)) ? blockID : 0;
        float secLevel = calculateFMD(band, block, false, maxLocGain1, samples, locA, levA, buf2);

        // for the non-overlapped region
        if (winSeq.equals(WindowSequence.LONG_START_SEQUENCE) || winSeq.equals(WindowSequence.LONG_STOP_SEQUENCE)) {
            calculateFMD(band, 1, false, maxLocGain2, samples, locA, levA, buf3);
        }

        // calculate a gain modification function
        int i;
        int flatLen = 0;
        if (winSeq.equals(WindowSequence.LONG_STOP_SEQUENCE)) {
            flatLen = samples / 2 - maxLocGain0 - maxLocGain1;
            for (i = 0; i < flatLen; i++) {
                modFunc[i] = 1.0f;
            }
        }
        if (winSeq.equals(WindowSequence.ONLY_LONG_SEQUENCE) || winSeq.equals(WindowSequence.EIGHT_SHORT_SEQUENCE))
            levA[0] = 1.0f;

        for (i = 0; i < maxLocGain0; i++) {
            modFunc[i + flatLen] = levA[0] * secLevel * buf1[i];
        }
        for (i = 0; i < maxLocGain1; i++) {
            modFunc[i + flatLen + maxLocGain0] = levA[0] * buf2[i];
        }

        if (winSeq.equals(WindowSequence.LONG_START_SEQUENCE)) {
            for (i = 0; i < maxLocGain2; i++) {
                modFunc[i + maxLocGain0 + maxLocGain1] = buf3[i];
            }
            flatLen = samples / 2 - maxLocGain1 - maxLocGain2;
            for (i = 0; i < flatLen; i++) {
                modFunc[i + maxLocGain0 + maxLocGain1 + maxLocGain2] = 1.0f;
            }
        } else if (winSeq.equals(WindowSequence.LONG_STOP_SEQUENCE)) {
            for (i = 0; i < maxLocGain2; i++) {
                modFunc[i + flatLen + maxLocGain0 + maxLocGain1] = buf3[i];
            }
        }

        // calculate a gain control function
        for (i = 0; i < samples; i++) {
            _function[i] = 1.0f / modFunc[i];
        }
    }

    /*
     * calculates a fragment modification function by interpolating the gain values
     * of the gain change positions
     */
    private float calculateFMD(int bd, int wd, boolean prev, int maxLocGain, int samples, int[] loc, float[] lev,
            float[] fmd) {
        final int[] m = new int[samples / 2];
        final int[] lct = prev ? locationPrev[bd][wd] : location[bd][wd];
        final int[] lvl = prev ? levelPrev[bd][wd] : level[bd][wd];
        final int length = lct.length;

        int lngain;
        int i;
        for (i = 0; i < length; i++) {
            loc[i + 1] = 8 * lct[i]; // gainc
            lngain = getGainChangePointID(lvl[i]); // gainc
            if (lngain < 0)
                lev[i + 1] = 1.0f / (float) Math.pow(2, -lngain);
            else
                lev[i + 1] = (float) Math.pow(2, lngain);
        }

        // set start point values
        loc[0] = 0;
        if (length == 0)
            lev[0] = 1.0f;
        else
            lev[0] = lev[1];
        float secLevel = lev[0];

        // set end point values
        loc[length + 1] = maxLocGain;
        lev[length + 1] = 1.0f;

        int j;
        for (i = 0; i < maxLocGain; i++) {
            m[i] = 0;
            for (j = 0; j <= length + 1; j++) {
                if (loc[j] <= i)
                    m[i] = j;
            }
        }

        for (i = 0; i < maxLocGain; i++) {
            if ((i >= loc[m[i]]) && (i <= loc[m[i]] + 7))
                fmd[i] = interpolateGain(lev[m[i]], lev[m[i] + 1], i - loc[m[i]]);
            else
                fmd[i] = lev[m[i] + 1];
        }

        return secLevel;
    }

    /**
     * transformes the exponent value of the gain to the id of the gain change point
     */
    private int getGainChangePointID(int lngain) {
        for (int i = 0; i < ID_GAIN; i++) {
            if (lngain == LN_GAIN[i])
                return i;
        }
        return 0; // shouldn't happen
    }

    /**
     * calculates a fragment modification function the interpolated gain value
     * between the gain values of two gain change positions is calculated by the
     * formula: f(a,b,j) = 2^(((8-j)log2(a)+j*log2(b))/8)
     */
    private float interpolateGain(float alev0, float alev1, int iloc) {
        final float a0 = (float) (Math.log(alev0) / Math.log(2));
        final float a1 = (float) (Math.log(alev1) / Math.log(2));
        return (float) Math.pow(2.0f, (((8 - iloc) * a0 + iloc * a1) / 8));
    }
}
