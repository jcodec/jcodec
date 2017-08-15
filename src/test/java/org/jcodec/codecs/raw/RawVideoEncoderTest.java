package org.jcodec.codecs.raw;

import java.nio.ByteBuffer;

import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.junit.Assert;
import org.junit.Test;

public class RawVideoEncoderTest {

	@Test
	public void testPlanar() {
		int capacity = 3 * 32 * 32 / 2;
		ByteBuffer ref = ByteBuffer.allocate(capacity);
		ByteBuffer enc = ByteBuffer.allocate(capacity);

		Picture picture = Picture.create(32, 32, ColorSpace.YUV420);
		fillRandom(picture, ref);

		RAWVideoEncoder encoder = new RAWVideoEncoder();
		EncodedFrame frame = encoder.encodeFrame(picture, enc);
		ref.flip();

		Assert.assertNotNull(frame);
		Assert.assertNotNull(frame.getData());
		Assert.assertArrayEquals(NIOUtils.toArray(ref), NIOUtils.toArray(frame.getData()));
	}

	@Test
	public void testPlanarCropped() {
		int capacity = 3 * 32 * 32;
		ByteBuffer ref = ByteBuffer.allocate(capacity);
		ByteBuffer enc = ByteBuffer.allocate(capacity);

		Picture picture = Picture.createCropped(32, 32, ColorSpace.YUV420, new Rect(2, 2, 28, 28));

		for (int c = 0; c < 3; c++) {
			int dim = 32 >> (c > 0 ? 1 : 0);
			byte[] planeData = picture.getPlaneData(c);
			for (int i = 0, off = 0; i < dim; i++) {
				for (int j = 0; j < dim; j++, off++) {
					int rand = (int) (Math.random() * 255);
					planeData[off] = (byte) (rand - 128);
					if (i >= 2 && i < 30 && j >= 2 && j < 30)
						ref.put((byte) rand);
				}
			}
		}

		RAWVideoEncoder encoder = new RAWVideoEncoder();
		EncodedFrame frame = encoder.encodeFrame(picture, enc);
		ref.flip();

		Assert.assertNotNull(frame);
		Assert.assertNotNull(frame.getData());
		Assert.assertArrayEquals(NIOUtils.toArray(ref), NIOUtils.toArray(frame.getData()));
	}

	@Test
	public void testInterleaved() {
		int capacity = 3 * 32 * 32;
		ByteBuffer ref = ByteBuffer.allocate(capacity);
		ByteBuffer enc = ByteBuffer.allocate(capacity);

		Picture picture = Picture.create(32, 32, ColorSpace.RGB);
		fillRandom(picture, ref);

		RAWVideoEncoder encoder = new RAWVideoEncoder();
		EncodedFrame frame = encoder.encodeFrame(picture, enc);
		ref.flip();

		Assert.assertNotNull(frame);
		Assert.assertNotNull(frame.getData());
		Assert.assertArrayEquals(NIOUtils.toArray(ref), NIOUtils.toArray(frame.getData()));
	}

	@Test
	public void testInterleavedCropped() {
		int capacity = 3 * 32 * 32;
		ByteBuffer ref = ByteBuffer.allocate(capacity);
		ByteBuffer enc = ByteBuffer.allocate(capacity);

		Picture picture = Picture.createCropped(32, 32, ColorSpace.RGB, new Rect(2, 2, 28, 28));

		byte[] planeData = picture.getPlaneData(0);
		for (int i = 0, off = 0; i < 32; i++) {
			for (int j = 0; j < 32; j++) {
				for (int c = 0; c < 3; c++, off++) {
					int rand = (int) (Math.random() * 255);
					planeData[off] = (byte) (rand - 128);
					if (i >= 2 && i < 30 && j >= 2 && j < 30)
						ref.put((byte) rand);
				}
			}

		}

		RAWVideoEncoder encoder = new RAWVideoEncoder();
		EncodedFrame frame = encoder.encodeFrame(picture, enc);
		ref.flip();

		Assert.assertNotNull(frame);
		Assert.assertNotNull(frame.getData());
		Assert.assertArrayEquals(NIOUtils.toArray(ref), NIOUtils.toArray(frame.getData()));

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
}
