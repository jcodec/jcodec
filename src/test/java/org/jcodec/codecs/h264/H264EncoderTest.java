package org.jcodec.codecs.h264;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.Assert;
import org.junit.Test;

public class H264EncoderTest {
    @Test
    public void testCanEncodeYuv444() throws Exception {
        try {
            H264Encoder encoder = H264Encoder.createH264Encoder();
            encode(encoder, ColorSpace.YUV444);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testCanEncodeYuv420() throws Exception {
        try {
            H264Encoder encoder = H264Encoder.createH264Encoder();
            encode(encoder, ColorSpace.YUV420);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testEncodeYuv420J() throws Exception {
        H264Encoder encoder = H264Encoder.createH264Encoder();
        encode(encoder, ColorSpace.YUV420J);
    }

    /**
     * test for issue https://github.com/jcodec/jcodec/issues/231
     */
    @Test
    public void canEncodeYuv444WithRateControl() throws Exception {
        try {
            H264Encoder encoder = new H264Encoder(new H264FixedRateControl(4));
            encode(encoder, ColorSpace.YUV444);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    private static void encode(H264Encoder encoder, ColorSpace colorSpace) {
        int displayWidth = 1920;
        int displayHeight = 1080;
        int uncompressedSize = displayWidth * displayHeight * 3;

        Picture picture = Picture.create(displayWidth, displayHeight, colorSpace);

        ByteBuffer buffer = ByteBuffer.allocate(uncompressedSize);

        EncodedFrame encodedFrame = encoder.encodeFrame(picture, buffer);
        assertNotNull(encodedFrame);
    }

    @Test
    public void testEncodeDecode() {
        int w = 320;
        int h = 240;
        int uncompressedSize = w * h * 3;

        H264Encoder encoder = H264Encoder.createH264Encoder();
        H264Decoder decoder = new H264Decoder();
        Picture picture = Picture.create(w, h, ColorSpace.YUV420J);
        Picture out = Picture.create(w, h, ColorSpace.YUV420J);

        for (int i = 0; i < 10; i++) {
            fillImage(w, h, i, picture);

            ByteBuffer buffer = ByteBuffer.allocate(uncompressedSize);

            EncodedFrame encodedFrame = encoder.encodeFrame(picture, buffer);
            assertNotNull(encodedFrame);
            Frame decodeFrame = decoder.decodeFrame(encodedFrame.getData(), out.getData());

            assertByteArrayApproximatelyEquals(picture.getData()[0], decodeFrame.getData()[0], 10);
            assertByteArrayApproximatelyEquals(picture.getData()[1], decodeFrame.getData()[1], 10);
            assertByteArrayApproximatelyEquals(picture.getData()[2], decodeFrame.getData()[2], 10);
        }
    }

    private static void assertByteArrayApproximatelyEquals(byte[] ref, byte[] dec, int threash) {
        int maxDiff = 0;
        for (int i = 0; i < ref.length; i++) {
            int diff = Math.abs(ref[i] - dec[i]);
            if (diff > maxDiff)
                maxDiff = diff;
        }
        Assert.assertTrue("Maxdiff: " + maxDiff, maxDiff < threash);
    }

    private static void fillImage(int w, int h, int n, Picture picture) {
        byte[][] data = picture.getData();

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                data[0][i * w + j] = (byte) (n * 10 + i * w + j);
            }
        }
        for (int c = 1; c < 3; c++) {
            for (int i = 0; i < h / 2; i++) {
                for (int j = 0; j < w / 2; j++) {
                    data[0][i * w / 2 + j] = (byte) (n * 10 + i * w / 2 + j);
                }
            }
        }
    }

    @Test
    public void testRandomImage() {
        H264Encoder encoder = H264Encoder.createH264Encoder();
        Picture pic = Picture.create(640, 360, ColorSpace.YUV420J);

        for (int i = 0; i < 3; i++) {
            byte[] planeData = pic.getPlaneData(i);
            new Random().nextBytes(planeData);
        }

        ByteBuffer out = ByteBuffer.allocate((3 * 640 * 360) / 2);
        encoder.encodeFrame(pic, out);
    }

    @Test
    public void testBufferOverflowImage() throws IOException {
        H264Encoder encoder = H264Encoder.createH264Encoder();
        Picture picture = AWTUtil.decodePNG((new File("src/test/resources/h264/buffer_overflow.png")), ColorSpace.YUV420J);
        ByteBuffer buf = ByteBuffer.allocate(picture.getWidth() * picture.getHeight());
        encoder.encodeFrame(picture, buf);
    }
}
