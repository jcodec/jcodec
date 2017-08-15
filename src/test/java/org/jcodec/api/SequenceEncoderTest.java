package org.jcodec.api;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.junit.Assert;
import org.junit.Test;

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
	
	@Test(expected=IllegalArgumentException.class)
	public void testWrongColor() throws IOException {
		File temp = File.createTempFile("temp-file-name", ".tmp");
		SequenceEncoder sequenceEncoder = new SequenceEncoder(NIOUtils.writableChannel(temp), Rational.ONE, Format.MOV,
				Codec.H264, null);
		Picture picture = Picture.create(32, 32, ColorSpace.YUV420J);
		sequenceEncoder.encodeNativeFrame(picture);
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
}
