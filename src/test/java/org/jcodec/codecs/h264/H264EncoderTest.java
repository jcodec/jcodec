package org.jcodec.codecs.h264;

import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.Test;

public class H264EncoderTest {
    @Test
    public void canEncodeYuv444() throws Exception {
        H264Encoder encoder = H264Encoder.createH264Encoder();
        encode444(encoder);
    }

    /**
     * test for issue https://github.com/jcodec/jcodec/issues/231
     */
    @Test
    public void canEncodeYuv444WithRateControl() throws Exception {
        H264Encoder encoder = new H264Encoder(new H264FixedRateControl(4));
        encode444(encoder);
    }

    private void encode444(H264Encoder encoder) {
        int displayWidth = 1920;
        int displayHeight = 1080;
        int uncompressedSize = displayWidth * displayHeight * 3;

        Picture picture = Picture.create(displayWidth, displayHeight, ColorSpace.YUV444);

        ByteBuffer buffer = ByteBuffer.allocate(uncompressedSize);

        EncodedFrame encodedFrame = encoder.encodeFrame(picture, buffer);
        assertNotNull(encodedFrame);
    }

}
