package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;

import org.jcodec.audio.Audio;
import org.jcodec.audio.AudioFilter;
import org.jcodec.audio.FilterGraph;
import org.jcodec.audio.SincLowPassFilter;
import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavInput.WavFile;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.codecs.wav.WavOutput.WavOutFile;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;

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
    private static final Flag FLAG_FREQ = Flag.createFlag("cut-off-frequency", "freq", "Cut-off frequency");
    private static final Flag FLAG_SIZE = Flag.createFlag("kernel-size", "size", "Kernel size of this filter");
    private static final Flag[] FLAGS = new MainUtils.Flag[] {FLAG_FREQ, FLAG_SIZE};
    
    public static void main(String[] args) throws IOException {

        Cmd cmd = MainUtils.parseArguments(args, FLAGS);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpVarArgs(FLAGS, "input file", "output file");
            System.exit(-1);
        }

        WavFile wavFile = new WavInput.WavFile(new File(cmd.getArg(0)));
        WavInput.Source source = new WavInput.Source(wavFile);
        WavOutFile wavOutFile = new WavOutput.WavOutFile(new File(cmd.getArg(1)), source.getFormat());
        WavOutput.Sink sink = new WavOutput.Sink(wavOutFile);

        int cutOff = cmd.getIntegerFlagD(FLAG_FREQ, 8000);
        int size = cmd.getIntegerFlagD(FLAG_SIZE, 40);

//@formatter:off
        AudioFilter filter = FilterGraph
                .addLevel(new SincLowPassFilter(size, (double) cutOff / source.getFormat().getSampleRate()))
                .create();
//@formatter:on
        Audio.filterTransfer(source, filter, sink);

        source.close();
        sink.close();
    }
}
