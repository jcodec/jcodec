package org.jcodec.samples.audio;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import org.jcodec.audio.Audio;
import org.jcodec.audio.AudioFilter;
import org.jcodec.audio.AudioSource;
import org.jcodec.audio.FilterGraph;
import org.jcodec.audio.LanczosInterpolator;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.codecs.wav.WavOutput.WavOutFile;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;

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
    private static final Flag FLAG_FREQ = Flag.flag("tone_freq", "freq", "Frequency of the tone to generate");
    private static final Flag FLAG_TRATE = Flag.flag("tone_rate", "trate", "Sampling rate of the tone");
    private static final Flag FLAG_ORATE = Flag.flag("out_rate", "orate", "Output sample rate");
    private static final Flag[] FLAGS = new MainUtils.Flag[] {FLAG_FREQ, FLAG_TRATE, FLAG_ORATE};
    
    public static void main(String[] args) throws IOException {

        Cmd cmd = MainUtils.parseArguments(args, FLAGS);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelpVarArgs(FLAGS, "output file");
            System.exit(-1);
        }

        int toneFreq = cmd.getIntegerFlagD(FLAG_FREQ, 500);
        int toneRate = cmd.getIntegerFlagD(FLAG_TRATE, 48000);
        int outRate = cmd.getIntegerFlagD(FLAG_ORATE, 44100);

        Tone source = new Tone(toneRate, toneFreq);
        WavOutFile wavOutFile = new WavOutput.WavOutFile(new File(cmd.getArg(0)), AudioFormat.MONO_S16_LE(outRate));
        WavOutput.Sink sink = new WavOutput.Sink(wavOutFile);
//@formatter:off
        AudioFilter filter = FilterGraph
                .addLevel(new LanczosInterpolator(toneRate, outRate))
                .create();
//@formatter:on
        Audio.filterTransfer(source, filter, sink);

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
        public int readFloat(FloatBuffer buffer) throws IOException {
            if (sample > 480000)
                return -1;
            int i = 0;
            for (; buffer.hasRemaining(); i++)
                buffer.put((float) Math.sin(factor * (sample++)));
            return i;
        }
    }
}
