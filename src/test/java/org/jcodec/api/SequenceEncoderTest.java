package org.jcodec.api;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.jcodec.Utils;
import org.jcodec.api.transcode.filters.ScaleFilter;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.platform.Platform;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.LanczosResampler;
import org.jcodec.scale.Transform;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;

public class SequenceEncoderTest {

    @Test
    public void testSequenceEncoder() throws IOException {
        int capacity = 3 * 32 * 32 * 10;
        ByteBuffer ref = ByteBuffer.allocate(capacity);
        ByteBuffer enc = ByteBuffer.allocate(capacity);
        ByteBufferSeekableByteChannel ch = new ByteBufferSeekableByteChannel(enc, capacity);

        SequenceEncoder sequenceEncoder = new SequenceEncoder(ch, Rational.ONE, Format.RAW, Codec.RAW, null);

        for (int i = 0; i < 10; i++) {
            Picture picture = Picture.create(32, 32, ColorSpace.RGB);
            fillRandom(picture, ref);
            sequenceEncoder.encodeNativeFrame(picture);
        }
        sequenceEncoder.finish();
        ref.flip();
        enc.flip();

        Assert.assertArrayEquals(NIOUtils.toArray(ref), NIOUtils.toArray(enc));
    }

    @Test
    public void testWrongColor() throws IOException {
        try {
        File temp = File.createTempFile("temp-file-name", ".tmp");
        SequenceEncoder sequenceEncoder = new SequenceEncoder(NIOUtils.writableChannel(temp), Rational.ONE, Format.MOV,
                Codec.H264, null);
        Picture picture = Picture.create(32, 32, ColorSpace.YUV420J);
        sequenceEncoder.encodeNativeFrame(picture);
        fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testRuns() throws IOException {
        File temp = File.createTempFile("temp-file-name", ".tmp");
        SequenceEncoder sequenceEncoder = new SequenceEncoder(NIOUtils.writableChannel(temp), Rational.ONE, Format.MOV,
                Codec.H264, null);

        for (int i = 0; i < 10; i++) {
            Picture picture = Picture.create(32, 32, ColorSpace.RGB);
            fillGradient(picture, i);
            sequenceEncoder.encodeNativeFrame(picture);
        }
        sequenceEncoder.finish();

        Assert.assertTrue("", temp.length() >= 128);
    }

    private void fillGradient(Picture picture, int ind) {
        for (int comp = 0; comp < picture.getData().length; comp++) {
            byte[] planeData = picture.getPlaneData(comp);
            for (int i = 0; i < planeData.length; i++) {
                planeData[i] = (byte) (i + ind * 2);
            }
        }
    }

    private void fillRandom(Picture picture, ByteBuffer ref) {
        for (int comp = 0; comp < picture.getData().length; comp++) {
            byte[] planeData = picture.getPlaneData(comp);
            for (int i = 0; i < planeData.length; i++) {
                int rand = (int) (Math.random() * 255);
                planeData[i] = (byte) (rand - 128);
                ref.put((byte) rand);
            }
        }
    }

    @Ignore @Test
    public void testBufferOverflow() throws IOException, JCodecException {
        File output = new File("/tmp/test.mp4");
        SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), new Rational(1, 1));
        Picture p0 = AWTUtil.decodePNG(new File("src/test/resources/h264/buffer_overflow.png"), ColorSpace.RGB);
        Picture p1 = AWTUtil.decodePNG(new File("src/test/resources/h264/buffer_overflow1.png"), ColorSpace.RGB);
        Picture p2 = AWTUtil.decodePNG(new File("src/test/resources/h264/buffer_overflow2.png"), ColorSpace.RGB);

        Picture[] encoded = new Picture[]{p0, p1, p2};
        for (int i = 0; i < 3; i++) {
            enc.encodeNativeFrame(encoded[i]);
        }
        enc.finish();
        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(output));
        Transform transform = ColorUtil.getTransform(ColorSpace.YUV420, encoded[0].getColor());
        Picture rgb = Picture.create(p0.getWidth(), p0.getHeight(), ColorSpace.RGB);
        for (int i = 0; i < 3; i++) {
            Picture yuv = grab.getNativeFrame();
            transform.transform(yuv, rgb);
            Assert.assertTrue(Utils.picturesRoughlyEqual(encoded[i], rgb, 5));
        }
        Platform.deleteFile(output);
    }
}
