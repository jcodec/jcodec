package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.algo.AudioFilter;
import org.jcodec.algo.LanczosInterpolator;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * 
 * Demonstrates audio interpolation usign tone
 * 
 * @author Jay Codec
 * 
 */
public class ToneInterpolation {

    public static void main(String[] args) throws IOException {

        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put("tone_freq", "Frequency of the tone to generate");
                    put("tone_rate", "Sampling rate of the tone");
                    put("out_rate", "Output sample rate");
                }
            }, "output file");
            System.exit(-1);
        }

        int toneFreq = cmd.getIntegerFlag("tone_freq", 500);
        int toneRate = cmd.getIntegerFlag("tone_rate", 48000);
        int outRate = cmd.getIntegerFlag("out_rate", 44100);

        float[] values = new float[1024];
        float[] out = new float[(int) Math.ceil(values.length * outRate / toneRate) + 3];

        WavOutput.Adaptor a = new WavOutput.Adaptor(new File(cmd.getArg(0)), AudioFormat.MONO_S16_LE(outRate));
        AudioFilter interp = new LanczosInterpolator(toneRate, outRate);

        double factor = 2 * Math.PI * toneFreq / toneRate;

        for (int ch = 0, sample = 0; ch < 1000; ch++) {

            for (int i = 0; i < values.length; i++) {
                values[i] = (float) Math.sin(factor * (sample++));
            }

            int produced = interp.filter(values, values.length, out);

            a.write(out, produced);
        }
        a.close();
    }

}
