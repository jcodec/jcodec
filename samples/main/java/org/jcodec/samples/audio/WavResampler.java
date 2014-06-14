package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.algo.AudioFilter;
import org.jcodec.algo.LanczosInterpolator;
import org.jcodec.algo.SincLowPassFilter;
import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * Basic wave file resampler using jcodec lanczos filter
 * 
 * @author Jay Codec
 * 
 */
public class WavResampler {

    public static void main(String[] args) throws IOException {

        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put("out_rate", "Output sample rate");
                }
            }, "input file", "output file");
            System.exit(-1);
        }

        int outRate = cmd.getIntegerFlag("out_rate", 44100);

        WavInput.Adaptor in = new WavInput.Adaptor(new File(cmd.getArg(0)));
        AudioFormat inf = in.getFormat();

        float[] values = new float[4096];
        float[] values1 = new float[4096];
        float[] out = new float[(int) Math.ceil(values.length * outRate / inf.getSampleRate()) + 3];

        WavOutput.Adaptor a = new WavOutput.Adaptor(new File(cmd.getArg(1)), new AudioFormat(inf, outRate));
        AudioFilter interp = new LanczosInterpolator(inf.getSampleRate(), outRate);
        AudioFilter lowPass = new SincLowPassFilter(outRate / 3, inf.getSampleRate());

        int read = 0;
        while ((read = in.read(values, values.length)) > 0) {
            int produced1 = lowPass.filter(values, read, values1);
            int produced = interp.filter(values1, produced1, out);
            a.write(out, produced);
        }
        a.close();
    }

}
