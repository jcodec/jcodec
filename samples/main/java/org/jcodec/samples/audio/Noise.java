package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;

import org.jcodec.audio.Audio;
import org.jcodec.audio.AudioFilter;
import org.jcodec.audio.ChannelMerge;
import org.jcodec.audio.ChannelSplit;
import org.jcodec.audio.FilterGraph;
import org.jcodec.codecs.wav.WavInput;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Generates noise in the input file
 * 
 * @author The JCodec project
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

        WavInput.Source source = new WavInput.Source(new File(cmd.getArg(0)));
        WavOutput.Sink sink = new WavOutput.Sink(new File(cmd.getArg(1)), source.getFormat());

        int dB = cmd.getIntegerFlag("level", -22);
        if (dB > -15 || dB < -90) {
            System.out.println("Impractical noise level of: " + dB + ", exiting!");
            System.exit(-1);
        }

//@formatter:off
        AudioFilter filter = FilterGraph
                .addLevel(new ChannelSplit(source.getFormat()))
                .addLevelSpan(new NoiseFilter(dB))
                .addLevel(new ChannelMerge(source.getFormat()))
                .create();
//@formatter:on
        Audio.transfer(source, filter, sink);

        source.close();
        sink.close();

    }

    public static class NoiseFilter implements AudioFilter {

        private double maxamp;
        private double half;

        public NoiseFilter(int dB) {
            maxamp = (double) (0x7fffff >> (-dB / 6)) / 0x7fffff;
            half = maxamp / 2;
        }

        @Override
        public void filter(FloatBuffer[] in, long[] inPos, FloatBuffer[] out) {
            FloatBuffer in0 = in[0];
            FloatBuffer out0 = out[0];

            while (in0.hasRemaining() && out0.hasRemaining())
                out0.put((float) Math.min(1, Math.max(-1, in0.get() + Math.random() * maxamp - half)));
        }

        @Override
        public int getDelay() {
            return 0;
        }

        @Override
        public int getNInputs() {
            return 1;
        }

        @Override
        public int getNOutputs() {
            return 1;
        }
    }
}
