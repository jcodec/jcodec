package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;

import org.jcodec.audio.Audio;
import org.jcodec.audio.AudioSource;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.codecs.wav.WavOutput.Sink;
import org.jcodec.codecs.wav.WavOutput.WavOutFile;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Illustrates work with Wave files
 * 
 * @author The JCodec project
 * 
 */
public class ToneGen {

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.args.length < 1) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put("freq", "Frequency of tone");
                    put("channels", "Number of channels");
                }
            }, "filename");
            System.exit(-1);
        }

        int channels = cmd.getIntegerFlagD("channels", 1);
        int[] freq = cmd.getMultiIntegerFlagD("freq", new int[] { 500 });

        ToneSource tone = new ToneSource(channels, freq);
        WavOutFile wavOutFile = new WavOutput.WavOutFile(new File(cmd.getArg(0)), tone.getFormat());
        Sink wavOutput = new WavOutput.Sink(wavOutFile);

        Audio.transfer(tone, wavOutput);

        wavOutput.close();
    }

    public static class ToneSource implements AudioSource {
        private double[] coeff;
        private double[] mul;
        private int[] sample;
        private int[] freq;
        private int channels;

        public ToneSource(int channels, int[] freq) {
            this.freq = freq;
            this.channels = channels;
            coeff = new double[freq.length];
            mul = new double[freq.length];
            sample = new int[channels];

            for (int i = 0; i < coeff.length; i++) {
                coeff[i] = 2 * Math.PI * freq[i] / 48000;
                mul[i] = .5 / (1 << i);
            }

            for (int i = 0; i < sample.length; i++) {
                sample[i] = i * (freq[0] / sample.length);
            }
        }

        @Override
        public int readFloat(FloatBuffer buffer) throws IOException {
            if (sample[0] > 480000)
                return -1;
            int rem = buffer.remaining();
            while (buffer.hasRemaining()) {
                for (int ch = 0; ch < channels; ch++) {
                    double result = 0;
                    for (int fi = 0; fi < freq.length; fi++)
                        result += (float) (Math.sin(sample[ch] * coeff[fi]) * mul[fi]);
                    ++sample[ch];
                    buffer.put((float) result);
                }
            }
            return buffer.remaining() - rem;
        }

        @Override
        public AudioFormat getFormat() {
            return AudioFormat.NCH_48K_S16_LE(channels);
        }
    }
}