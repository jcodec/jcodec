package org.jcodec.samples.api;

import org.jcodec.api.awt.SequenceEncoder8Bit;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Stanislav Vitvitskyy
 * 
 */
public class SequenceEncoderDemo {
    private static final String FLAG_NUM_FRAMES = "num-frames";
    private static final String FLAG_IN_PATTERN = "in-pattern";

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put(FLAG_NUM_FRAMES, "Maximum frames to encode.");
                    put(FLAG_IN_PATTERN, "Output folder for the frames.");
                }
            }, "output file name");
            return;
        }

        int maxFrames = cmd.getIntegerFlag(FLAG_NUM_FRAMES, Integer.MAX_VALUE);
        String outDir = cmd.getStringFlag(FLAG_IN_PATTERN,
                new File(System.getProperty("user.home"), "frame%08d.jpg").getAbsolutePath());
        FileChannelWrapper out = null;
        try {
            out = NIOUtils.writableFileChannel(MainUtils.tildeExpand(cmd.getArg(0)));
            SequenceEncoder8Bit encoder = new SequenceEncoder8Bit(out);

            for (int i = 0; i < maxFrames; i++) {
                File file = new File(String.format(outDir, i));
                if (!file.exists())
                    break;
                BufferedImage image = ImageIO.read(file);
                encoder.encodeImage(image);
            }
            encoder.finish();
        } finally {
            NIOUtils.closeQuietly(out);
        }
    }
}
