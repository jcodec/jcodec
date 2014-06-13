package org.jcodec.samples.audio;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * Generates noise in the input file
 * 
 * @author Jay Codec
 * 
 */
public class Noise {
    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put("level", "Desired noise level, between -90 and -15.");
                }
            }, "input.wav", "output.wav");
            System.exit(-1);
        }

        WavInput.Adaptor in = new WavInput.Adaptor(new File(cmd.getArg(0)));
        WavOutput.Adaptor out = new WavOutput.Adaptor(new File(cmd.getArg(1)), in.getFormat());

        int dB = cmd.getIntegerFlag("level", -22);
        if (dB > -15 || dB < -90) {
            System.out.println("Impractical noise level of: " + dB + ", exiting!");
            System.exit(-1);
        }

        float[] buf = new float[4096];

        double maxamp = (double) (0x7fffff >> (-dB / 6)) / 0x7fffff, half = maxamp / 2;

        while (in.read(buf, buf.length) > 0) {
            for (int i = 0; i < buf.length; i++)
                buf[i] = (float) Math.min(1, Math.max(-1, buf[i] + Math.random() * maxamp - half));
            out.write(buf, buf.length);
        }
        in.close();
        out.close();

    }
}
