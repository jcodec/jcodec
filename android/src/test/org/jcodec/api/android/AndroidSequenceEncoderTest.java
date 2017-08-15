package org.jcodec.api.android;

import java.io.File;
import java.io.IOException;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.junit.Assert;
import org.junit.Test;

import android.graphics.Bitmap;

public class AndroidSequenceEncoderTest {

	static class MyBitmap extends Bitmap {
		private Picture pic;
		
		public MyBitmap(Picture pic) {
			this.pic = pic;
		}

		@Override
		public int getWidth() {
			return pic.getWidth();
		}

		@Override
		public int getHeight() {
			return pic.getHeight();
		}

		@Override
		public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
			byte[] dstData = pic.getPlaneData(0);
			for (int i = 0, srcOff = 0, dstOff = 0; i < height; i++) {
	            for (int j = 0; j < width; j++, srcOff++, dstOff += 3) {
	            	int rgb = dstData[dstOff] + 128;
	            	rgb <<= 8;
	            	rgb |= dstData[dstOff + 1] + 128;
	            	rgb <<= 8;
	            	rgb |= dstData[dstOff + 2] + 128;
	            	
	            	pixels[srcOff] = rgb;
	            }
	        }
		}
	}

	@Test
	public void testEncoder() throws IOException {
		final Picture[] enc = new Picture[1];
		File temp = File.createTempFile("temp-file-name", ".tmp");
		AndroidSequenceEncoder se = new AndroidSequenceEncoder(NIOUtils.writableChannel(temp), Rational.ONE) {
			@Override
			public void encodeNativeFrame(Picture pic) throws IOException {
				enc[0] = pic;
			}
		};

		final Picture pic = Picture.create(32, 32, ColorSpace.RGB);
		fillGradient(pic);

		se.encodeImage(new MyBitmap(pic));

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
