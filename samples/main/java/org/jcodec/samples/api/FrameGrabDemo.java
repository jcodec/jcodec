package org.jcodec.samples.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jcodec.api.JCodecException;
import org.jcodec.api.awt.AWTFrameGrab;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class FrameGrabDemo {
    private static final Flag FLAG_FRAMES = Flag.flag("num-frames", "num-frames", "Maximum frames to decode.");
    private static final Flag FLAG_PATTERN = Flag.flag("out-pattern", "out-pattern", "Output folder/frame%04.png pattern.");
    private static final Flag[] FLAGS = new MainUtils.Flag[] {FLAG_FRAMES, FLAG_PATTERN};

    public static void main(String[] args) throws IOException, JCodecException {
        Cmd cmd = MainUtils.parseArguments(args, FLAGS);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelpArgs(FLAGS, new String[]{"input file name"});
            return;
        }

        int maxFrames = cmd.getIntegerFlagD(FLAG_FRAMES, Integer.MAX_VALUE);
        String outDir = cmd.getStringFlagD(FLAG_PATTERN,
                new File(System.getProperty("user.home"), "frame%08d.jpg").getAbsolutePath());
        FileChannelWrapper in = null;
        try {
            in = NIOUtils.readableChannel(MainUtils.tildeExpand(cmd.getArg(0)));
            AWTFrameGrab fg = AWTFrameGrab.createAWTFrameGrab(in);
            for (int i = 0; i < maxFrames; i++) {
                BufferedImage frame = fg.getFrame();
                ImageIO.write(frame, "jpeg", new File(String.format(outDir, i)));
            }
        } finally {
            NIOUtils.closeQuietly(in);
        }
    }
}
