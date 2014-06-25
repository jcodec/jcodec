package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;

import org.jcodec.audio.Audio;
import org.jcodec.audio.AudioFilter;
import org.jcodec.audio.AudioSource;
import org.jcodec.audio.FilterGraph;
import org.jcodec.audio.LanczosInterpolator;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demonstrates audio interpolation usign tone
 * 
 * @author The JCodec project
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

        Tone source = new Tone(toneRate, toneFreq);
        WavOutput.Sink sink = new WavOutput.Sink(new File(cmd.getArg(0)), AudioFormat.MONO_S16_LE(outRate));
//@formatter:off
        AudioFilter filter = FilterGraph
                .addLevel(new LanczosInterpolator(toneRate, outRate))
                .create();
//@formatter:on
        Audio.transfer(source, filter, sink);

        sink.close();
    }

    public static class Tone implements AudioSource {

        private double factor;
        private int sample;
        private AudioFormat format;

        public Tone(int toneRate, int toneFreq) {
            factor = 2 * Math.PI * toneFreq / toneRate;
            format = AudioFormat.MONO_S16_LE(toneRate);
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public int read(FloatBuffer buffer) throws IOException {
            if (sample > 480000)
                return -1;
            int i = 0;
            for (; buffer.hasRemaining(); i++)
                buffer.put((float) Math.sin(factor * (sample++)));
            return i;
        }
    }
}
