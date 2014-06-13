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

        int freq = cmd.getIntegerFlag("freq", 500);

        double coeff = 2 * Math.PI * freq / format.getSampleRate();

        int sample = 0;
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < buf.length; j++)
                buf[j] = (float) Math.sin(sample++ * coeff);
            wavOutput.write(buf, buf.length);
        }
        wavOutput.close();
    }
}