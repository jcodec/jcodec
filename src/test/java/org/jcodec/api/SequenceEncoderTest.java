package org.jcodec.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

public class SequenceEncoderTest {

    public void testSequenceEncoder() throws IOException {
        File folder = new File(System.getProperty("user.home"), "Desktop/frames");
        SequenceEncoder enc = new SequenceEncoder(new File(System.getProperty("user.home"), "Desktop/enc.mov"));
        for (int i = 1; i <= 100; i++) {
            BufferedImage image = ImageIO.read(new File(folder, String.format("frame%08d.png", i)));
            enc.encodeImage(image);
        }
        enc.finish();
    }
}
