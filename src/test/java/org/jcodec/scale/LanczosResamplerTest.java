package org.jcodec.scale;

import java.awt.image.BufferedImage;

import org.jcodec.Utils;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.junit.Assert;
import org.junit.Test;

/**
 * Sanity test for Lanczos resampling filter. It doesn't test the quality of
 * output.
 * 
 * @author Stanislav Vitvitskiy
 */
public class LanczosResamplerTest {

    @Test
    public void randomTest() {
        LanczosResampler resampler = new LanczosResampler(new Size(320, 240), new Size(320 / 3, 240 / 5));

        for (int i = 0; i < 100; i++) {
            int smooth = (int) (Math.random() * 20) + 5;
            double vector = Math.random();
            Picture8Bit pic = Utils.buildSmoothRandomPic(320, 240, smooth, vector);
            Picture8Bit out = Picture8Bit.create(320 / 3, 240 / 5, pic.getColor());
            resampler.resample(pic, out);
            BufferedImage scale = Utils.scale(AWTUtil.toBufferedImage8Bit(out), 320 / 3, 240 / 5);
            Picture8Bit outRef = AWTUtil.fromBufferedImage8Bit(scale, out.getColor());
            // System.out.println(smooth/2);
            Assert.assertTrue("Not equal for smooth=" + smooth + ", vector=" + vector,
                    Utils.picturesRoughlyEqual(outRef, out, smooth / 2));
        }
    }
}
