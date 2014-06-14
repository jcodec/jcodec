package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.codecs.wav.WavOutput.Adaptor;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * 
 * Illustrates work with Wave files
 * 
 * @author Jay Codec
 * 
 */
public class ToneGen {

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.args.length < 1) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put("freq", "Frequency of tone");
                }
            }, "filename");
            System.exit(-1);
        }

        float[] buf = new float[1024];

        AudioFormat format = AudioFormat.MONO_48K_S16_LE;
        Adaptor wavOutput = new WavOutput.Adaptor(new File(cmd.getArg(0)), format);

        int[] freq = cmd.getMultiIntegerFlag("freq", new int[] { 500 });

        double[] coeff = new double[freq.length];
        double[] mul = new double[freq.length];
        for (int i = 0; i < coeff.length; i++) {
            coeff[i] = 2 * Math.PI * freq[i] / format.getSampleRate();
            mul[i] = .5 / (1 << i);
        }

        int sample = 0;
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < buf.length; j++) {
                double result = 0;
                for (int fi = 0; fi < freq.length; fi++)
                    result += (float) (Math.sin(sample * coeff[fi]) * mul[fi]);
                ++sample;
                buf[j] = (float) result;
            }
            wavOutput.write(buf, buf.length);
        }
        wavOutput.close();
    }
}