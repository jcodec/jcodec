package org.jcodec.codecs.h264;

import org.jcodec.codecs.h264.encode.DumbRateControl;
import org.jcodec.common.model.ColorSpace;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 *
 */
public class H264EncoderTest {

    @Test
    public void testGetColorSpace_Default() throws Exception {
        H264Encoder encoder = new H264Encoder(new DumbRateControl());
        Assert.assertEquals("H264Encoder should default to color space YUV420J.", ColorSpace.YUV420J, encoder.getColorSpace());
    }

    @Test
    public void testSetColorSpace() throws Exception {
        H264Encoder encoder = new H264Encoder(new DumbRateControl());
        encoder.setColorSpace(ColorSpace.YUV420);
        Assert.assertEquals("H264Encoder did not change to the requested color space.", ColorSpace.YUV420, encoder.getColorSpace());
        // and set it back to the default
        encoder.setColorSpace(ColorSpace.YUV420J);
        Assert.assertEquals("H264Encoder did not change to the requested color space.", ColorSpace.YUV420J, encoder.getColorSpace());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetColorSpace_Unsupported() throws Exception {
        H264Encoder encoder = new H264Encoder(new DumbRateControl());
        encoder.setColorSpace(ColorSpace.BGR);
    }

}