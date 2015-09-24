package org.jcodec.samples.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.jcodec.api.JCodecException;
import org.jcodec.api.awt.FrameGrab8Bit;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class FrameGrabDemo {
    private static final String FLAG_NUM_FRAMES = "num-frames";
    private static final String FLAG_OUT_PATTERN = "out-pattern";

    public static void main(String[] args) throws IOException, JCodecException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put(FLAG_NUM_FRAMES, "Maximum frames to decode.");
                    put(FLAG_OUT_PATTERN, "Output folder for the frames.");
                }
            }, "input file name");
            return;
        }

        int maxFrames = cmd.getIntegerFlag(FLAG_NUM_FRAMES, Integer.MAX_VALUE);
        String outDir = cmd.getStringFlag(FLAG_OUT_PATTERN,
                new File(System.getProperty("user.home"), "frame%08d.jpg").getAbsolutePath());
        FileChannelWrapper in = null;
        try {
            in = NIOUtils.readableFileChannel(MainUtils.tildeExpand(cmd.getArg(0)));
            FrameGrab8Bit fg = new FrameGrab8Bit(in);
            for (int i = 0; i < maxFrames; i++) {
                BufferedImage frame = fg.getFrame();
                ImageIO.write(frame, "jpeg", new File(String.format(outDir, i)));
            }
        } finally {
            NIOUtils.closeQuietly(in);
        }
    }
}
