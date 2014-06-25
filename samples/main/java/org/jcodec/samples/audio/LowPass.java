package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.audio.Audio;
import org.jcodec.audio.AudioFilter;
import org.jcodec.audio.FilterGraph;
import org.jcodec.audio.SincLowPassFilter;
import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demos the usage of low-pass filtering
 * 
 * @author The JCodec project
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

        WavInput.Source source = new WavInput.Source(new File(cmd.getArg(0)));
        WavOutput.Sink sink = new WavOutput.Sink(new File(cmd.getArg(1)), source.getFormat());

        int cutOff = cmd.getIntegerFlag("freq", 8000);
        int size = cmd.getIntegerFlag("size", 40);

//@formatter:off
        AudioFilter filter = FilterGraph
                .addLevel(new SincLowPassFilter(size, (double) cutOff / source.getFormat().getSampleRate()))
                .create();
//@formatter:on
        Audio.transfer(source, filter, sink);

        source.close();
        sink.close();
    }
}
