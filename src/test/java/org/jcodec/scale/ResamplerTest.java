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
public class ResamplerTest {

    @Test
    public void randomLanczosTest() {
        Size sz = new Size(320, 240);

        for (int i = 0; i < 10; i++) {
            int w = (int) (Math.random() * 200);
            w = w * 2 + 100;
            int h = (int) (Math.random() * 200);
            h = h * 2 + 100;
            System.out.println("Size: " + w + ", " + h);
            LanczosResampler resampler = new LanczosResampler(sz, new Size(w, h));
            testResampler(resampler, w, h);
        }
    }

    @Test
    public void randomBicubicTest() {
        Size sz = new Size(320, 240);

        for (int i = 0; i < 10; i++) {
            int w = (int) (Math.random() * 200);
            w = w * 2 + 100;
            int h = (int) (Math.random() * 200);
            h = h * 2 + 100;
            System.out.println("Size: " + w + ", " + h);
            BicubicResampler resampler = new BicubicResampler(sz, new Size(w, h));
            testResampler(resampler, w, h);
        }
    }

    private void testResampler(BaseResampler resampler, int w, int h) {
        for (int i = 0; i < 20; i++) {
            int smooth = (int) (Math.random() * 20) + 5;
            double vector = Math.random();
            Picture8Bit pic = Utils.buildSmoothRandomPic(320, 240, smooth, vector);
            Picture8Bit out = Picture8Bit.create(w, h, pic.getColor());
            resampler.resample(pic, out);
            BufferedImage scale = Utils.scale(AWTUtil.toBufferedImage8Bit(out), w, h);
            Picture8Bit outRef = AWTUtil.fromBufferedImage8Bit(scale, out.getColor());
            // System.out.println(smooth/2);
            Assert.assertTrue("Not equal for smooth=" + smooth + ", vector=" + vector,
                    Utils.picturesRoughlyEqual(outRef, out, smooth / 2));
        }
    }
}
