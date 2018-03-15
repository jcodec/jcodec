package org.jcodec.scale;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.platform.Platform;
import org.junit.Ignore;
import org.junit.Test;

import static org.jcodec.Utils.picturesRoughlyEqual;
import static org.jcodec.scale.AWTUtil.decodePNG;
import static org.junit.Assert.assertTrue;

/**
 * Sanity test for Lanczos resampling filter. It doesn't test the quality of
 * output.
 *
 * @author Stanislav Vitvitskiy
 */
public class ResamplerTest {
    private final static Map<String, String[]> expectedScaled = new LinkedHashMap<String, String[]>();
    private final static File dir = new File("src/test/resources/scale");

    static {
        expectedScaled.put("randompic_6.png", new String[]{"122x328_6.png", "356x280_6.png", "436x452_6.png"});
        expectedScaled.put("randompic_10.png", new String[]{"140x114_10.png", "202x132_10.png", "248x160_10.png"});
//        expectedScaled.put("randompic_13.png", new String[]{"264x490_13.png", "382x182_13.png", "458x374_13.png"});
        expectedScaled.put("randompic_13.png", new String[]{"382x182_13.png", "458x374_13.png"});
        expectedScaled.put("randompic_19.png", new String[]{"112x462_19.png", "210x440_19.png", "362x216_19.png"});
        expectedScaled.put("randompic_21.png", new String[]{"212x436_21.png", "308x264_21.png", "402x340_21.png"});
    }

    @Test
    @Ignore
    public void _testLanczosFails() throws IOException {
        int smooth = 13;
        Picture orig = decodePNG(new File(dir, "randompic_13.png"), ColorSpace.YUV420J);
        Picture expected = decodePNG(new File(dir, "264x490_13.png"), orig.getColor());
        LanczosResampler resampler = new LanczosResampler(orig.getSize(), expected.getSize());
        Picture scaled = Picture.create(expected.getWidth(), expected.getHeight(), expected.getColor());
        resampler.resample(orig, scaled);
        boolean eq = picturesRoughlyEqual(expected, scaled, smooth / 2);
        assertTrue("Not equal for smooth=" + smooth, eq);
    }

    @Test
    public void testRandomLanczos() throws IOException {
        _testResampler(LanczosResampler.class);
    }

    @Test
    public void testRandomBicubic() throws IOException {
        _testResampler(BicubicResampler.class);
    }

    private static void _testResampler(Class<? extends BaseResampler> resamplerClass) throws IOException {
        for (Map.Entry<String, String[]> e : expectedScaled.entrySet()) {
            String origFile = e.getKey();
            System.out.println(origFile);
            Picture orig = decodePNG(new File(dir, origFile), ColorSpace.YUV420J);
            for (String scaledFile : e.getValue()) {
                System.out.println(scaledFile);
                Picture expected = decodePNG(new File(dir, scaledFile), orig.getColor());
                int smooth = Integer.parseInt(scaledFile.replaceAll("^.*_", "").replaceAll(".png$", ""));
                Picture scaled = Picture.create(expected.getWidth(), expected.getHeight(), orig.getColor());
                BaseResampler resampler = Platform.newInstance(resamplerClass, new Object[]{orig.getSize(), expected.getSize()});
                resampler.resample(orig, scaled);

                boolean eq = picturesRoughlyEqual(expected, scaled, smooth / 2);
                assertTrue("Not equal for smooth=" + smooth, eq);
            }
        }
    }
}