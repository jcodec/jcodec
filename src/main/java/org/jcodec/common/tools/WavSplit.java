package org.jcodec.common.tools;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.common.Assert;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.AudioUtil;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Splits a multichannel wave file into a number of single-channel wavs
 * 
 * @author The JCodec project
 * 
 */
public class WavSplit {
    public static void main(String[] args) throws Exception {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 1) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("pattern", "Output file name pattern, i.e. out%02d.wav");
            MainUtils.printHelp(map, "filename.wav");
            System.exit(-1);
        }

        File s = new File(args[0]);
        String pattern = cmd.getStringFlag("pattern", "c%02d.wav");

        WavHeader wavHeader = WavHeader.read(s);

        System.out.println("WAV: " + wavHeader.getFormat());

        Assert.assertEquals(2, wavHeader.fmt.numChannels);
        int dataOffset = wavHeader.dataOffset;
        FileChannelWrapper is = NIOUtils.readableFileChannel(s);
        is.position(dataOffset);

        int channels = wavHeader.getFormat().getChannels();
        SeekableByteChannel[] out = new SeekableByteChannel[channels];
        for (int i = 0; i < channels; i++) {
            out[i] = NIOUtils.writableFileChannel((new File(s.getParentFile(), String.format(pattern, i))));
            WavHeader.copyWithChannels(wavHeader, 1).write(out[i]);
        }

        copy(wavHeader.getFormat(), is, out);

        for (int i = 0; i < channels; i++) {
            out[i].close();
        }
    }

    private static void copy(AudioFormat format, ReadableByteChannel is, SeekableByteChannel[] out) throws IOException {
        ByteBuffer[] outs = new ByteBuffer[out.length];
        for (int i = 0; i < out.length; i++) {
            outs[i] = ByteBuffer.allocate(format.framesToBytes(4096));
        }
        ByteBuffer inb = ByteBuffer.allocate(format.framesToBytes(4096) * out.length);

        while (is.read(inb) != -1) {
            inb.flip();
            AudioUtil.deinterleave(format, inb, outs);
            inb.clear();
            for (int i = 0; i < out.length; i++) {
                outs[i].flip();
                out[i].write(outs[i]);
                outs[i].clear();
            }
        }
    }
}
