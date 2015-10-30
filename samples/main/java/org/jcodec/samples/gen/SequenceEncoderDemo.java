package org.jcodec.samples.gen;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.api.awt.SequenceEncoder8Bit;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A demo of SequenceEncoder that generates a video with a bouncing ball
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class SequenceEncoderDemo {

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelp(new HashMap<String, String>() {
                {
                    put("n-frames", "Total frames to encode");
                }
            }, "output file");
            return;
        }
        final int speed = 4;
        final int ballSize = 40;

        SequenceEncoder8Bit enc = new SequenceEncoder8Bit(new File(cmd.getArg(0)));
        enc.getEncoder().setKeyInterval(25);
        int framesToEncode = cmd.getIntegerFlag("n-frames", 100);

        long totalNano = 0;
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
        for (int i = 0, x = 0, y = 0, incX = speed, incY = speed; i < framesToEncode; i++, x += incX, y += incY) {
            Graphics g = image.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setColor(Color.YELLOW);
            if (x >= image.getWidth() - ballSize)
                incX = -speed;
            if (y >= image.getHeight() - ballSize)
                incY = -speed;
            if (x <= 0)
                incX = speed;
            if (y <= 0)
                incY = speed;
            g.fillOval(x, y, ballSize, ballSize);
            long start = System.nanoTime();
            enc.encodeImage(image);
            totalNano += System.nanoTime() - start;
        }
        enc.finish();

        System.out.println("FPS: " + ((1000000000L * framesToEncode) / totalNano));
    }
}
