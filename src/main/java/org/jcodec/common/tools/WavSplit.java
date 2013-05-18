package org.jcodec.common.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jcodec.codecs.wav.StringReader;
import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.common.Assert;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Splits a multichannel wave file into a number of single-channel wavs
 * 
 * @author The JCodec project
 * 
 */
public class WavSplit {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Syntax: <file>");
            return;
        }

        File s = new File(args[0]);

        WavHeader wavHeader = WavHeader.read(s);
        int bits = wavHeader.fmt.bitsPerSample;
        int channels = wavHeader.fmt.numChannels;
        long rate = wavHeader.fmt.sampleRate;

        System.out.println("WAV " + rate + " " + channels + " channels, " + bits + "bit");

        Assert.assertEquals(2, wavHeader.fmt.numChannels);
        int dataOffset = wavHeader.dataOffset;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(s));
        StringReader.sureSkip(is, dataOffset);

        OutputStream[] out = new OutputStream[channels];
        for (int i = 0; i < channels; i++) {
            out[i] = new BufferedOutputStream(new FileOutputStream(new File(s.getParentFile(), String.format("c%02d.wav", i))));
            createHeader(wavHeader, bits, channels, rate).write(out[i]);
        }

        copy(bits, channels, is, out);
        for (int i = 0; i < channels; i++) {
            out[i].close();
        }
    }

    private static void copy(int bits, int channels, BufferedInputStream is, OutputStream[] out) throws IOException {
        int bps = bits >> 3;
        byte b[] = new byte[bps];
        while (true) {
            for (int i = 0; i < channels; i++) {
                int read = is.read(b);
                if (read != bps)
                    return;
                out[i].write(b);
            }
        }
    }

    private static WavHeader createHeader(WavHeader wavHeader, int bits, int channels, long rate) {
        WavHeader w = WavHeader.emptyWavHeader();

        w.fmt.audioFormat = 1;
        w.fmt.bitsPerSample = (short)bits;
        w.fmt.blockAlign = (short)(bits >> 3);
        w.fmt.byteRate = (short)((rate * bits) >> 3);
        w.fmt.numChannels = 1;
        w.fmt.sampleRate = (short)rate;
        w.dataSize = wavHeader.dataSize / channels;
        return w;
    }
}
