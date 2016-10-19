package org.jcodec.scale;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.codecs.y4m.Y4MDecoder;
import org.jcodec.codecs.y4m.Y4MEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

public class ResampleMain {
    private static final String ARG_SIZE = "size";
    private static final String ARG_FILTER = "filter";
    private static final String ARG_FRAMES = "frames";

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put(ARG_SIZE, "Output size of an image.");
                    put(ARG_FILTER, "The filter to use.");
                    put(ARG_FRAMES, "The number of frames to encode.");
                }
            }, "in file", "out file");
            return;
        }
        String input = cmd.getArg(0);
        String outName = cmd.getArg(1);
        FileChannelWrapper inCh = NIOUtils.readableChannel(new File(input));
        FileChannelWrapper outCh = NIOUtils.writableChannel(new File(outName));
        Y4MDecoder decoder = new Y4MDecoder(inCh);
        Y4MEncoder encoder = new Y4MEncoder(outCh);
        String tgtSize = cmd.getStringFlag(ARG_SIZE);
        if (tgtSize == null) {
            System.err.println("Specify size");
            return;
        }
        Integer frames = cmd.getIntegerFlag(ARG_FRAMES);
        InterpFilter filter = InterpFilter.BICUBIC;
        if (cmd.getStringFlag(ARG_FILTER) != null) {
            filter = InterpFilter.valueOf(cmd.getStringFlag(ARG_FILTER).toUpperCase());
            System.err.println("Using filter: " + filter);
        }
        String[] wh = tgtSize.split("x");
        Size sz = new Size(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));

        Size inputSize = decoder.getSize();
        Picture8Bit buf = Picture8Bit.create(inputSize.getWidth(), inputSize.getHeight(), ColorSpace.YUV420);
        Picture8Bit out = Picture8Bit.create(sz.getWidth(), sz.getHeight(), ColorSpace.YUV420);

        Picture8Bit pic;
        BaseResampler resampler;
        if (filter == InterpFilter.BICUBIC) {
            resampler = new BicubicResampler(inputSize, sz);
        } else {
            resampler = new LanczosResampler(inputSize, sz);
        }
        for (int frame = 0; (pic = decoder.nextFrame8Bit(buf.getData())) != null; frame++) {
            if (frames != null && frame > frames)
                break;
            resampler.resample(pic, out);
            encoder.encodeFrame(out);
        }
    }
}
