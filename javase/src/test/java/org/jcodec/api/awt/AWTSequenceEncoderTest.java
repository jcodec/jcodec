package org.jcodec.api.awt;

import java.io.File;
import java.io.IOException;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.javase.api.awt.AWTSequenceEncoder;
import org.jcodec.javase.scale.AWTUtil;
import org.junit.Assert;
import org.junit.Test;

public class AWTSequenceEncoderTest {

	@Test
	public void testEncoder() throws IOException {
		final Picture[] enc = new Picture[1];
		File temp = File.createTempFile("temp-file-name", ".tmp");
		AWTSequenceEncoder se = new AWTSequenceEncoder(NIOUtils.writableChannel(temp), Rational.ONE) {
			@Override
			public void encodeNativeFrame(Picture pic) throws IOException {
				enc[0] = pic;
			}
		};

		Picture pic = Picture.create(32, 32, ColorSpace.RGB);
		fillGradient(pic);

		se.encodeImage(AWTUtil.toBufferedImage(pic));

		Assert.assertNotNull(enc[0]);
		Assert.assertArrayEquals(pic.getPlaneData(0), enc[0].getPlaneData(0));

	}

	private void fillGradient(Picture picture) {
		for (int comp = 0; comp < picture.getData().length; comp++) {
			byte[] planeData = picture.getPlaneData(comp);
			for (int i = 0; i < planeData.length; i++) {
				planeData[i] = (byte) (i);
			}
		}
	}
}
