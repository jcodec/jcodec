package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.algo.SincLowPassFilter;
import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * 
 * Demoes the usage of low-pass filtering
 * 
 * @author Jay Codec
 * 
 */
public class LowPass {
    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put("freq", "Cut-off frequency");
                    put("size", "Kernel size of this filter");
                }
            }, "input file", "output file");
            System.exit(-1);
        }

        WavInput.Adaptor in = new WavInput.Adaptor(new File(cmd.getArg(0)));
        WavOutput.Adaptor out = new WavOutput.Adaptor(new File(cmd.getArg(1)), in.getFormat());

        int cutOff = cmd.getIntegerFlag("freq", 8000);
        int size = cmd.getIntegerFlag("size", 40);

        SincLowPassFilter filter = new SincLowPassFilter(size, (double) cutOff
                / in.getFormat().getSampleRate());

        float[] bufi = new float[4096];
        float[] bufo = new float[4096];
        int read;
        while ((read = in.read(bufi, bufi.length)) > 0) {
            int produced = filter.filter(bufi, bufi.length, bufo);
            out.write(bufo, produced);
        }
        in.close();
        out.close();
    }
}
