package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.audio.Audio;
import org.jcodec.audio.AudioFilter;
import org.jcodec.audio.ChannelMerge;
import org.jcodec.audio.ChannelSplit;
import org.jcodec.audio.FilterGraph;
import org.jcodec.audio.LanczosInterpolator;
import org.jcodec.audio.SincLowPassFilter;
import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.IOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Basic wave file resampler using jcodec lanczos filter
 * 
 * @author The JCodec project
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

        WavInput.Source wavIn = null;
        WavOutput.Sink wavOut = null;
        try {
            wavIn = new WavInput.Source(new File(cmd.getArg(0)));
            AudioFormat inf = wavIn.getFormat();

            wavOut = new WavOutput.Sink(new File(cmd.getArg(1)), new AudioFormat(inf, outRate));
            AudioFilter lowPass = new SincLowPassFilter(outRate / 3, inf.getSampleRate());

            // Two interpolations for the sake of the demo, just to make our
            // lives a little bit harder
            AudioFilter r0 = new LanczosInterpolator(inf.getSampleRate(), 44100);
            AudioFilter r1 = new LanczosInterpolator(44100, outRate);
            ChannelSplit split = new ChannelSplit(inf);
            ChannelMerge merge = new ChannelMerge(inf);

//@formatter:off
            FilterGraph cf = FilterGraph
                    .addLevel(split)
                    .addLevelSpan(lowPass)
                    .addLevelSpan(r0)
                    .addLevelSpan(r1)
                    .addLevel(merge)
                    .create();
//@formatter:on
            Audio.transfer(wavIn, cf, wavOut);
        } finally {
            IOUtils.closeQuietly(wavIn);
            IOUtils.closeQuietly(wavOut);
        }
    }
}
