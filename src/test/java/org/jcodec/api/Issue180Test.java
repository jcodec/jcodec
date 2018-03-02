package org.jcodec.api;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.Assert;
import org.junit.Test;

//https://github.com/jcodec/jcodec/issues/180
public class Issue180Test {

	@Test
	public void testFrameGrabDoesNotThrowException() throws Exception {
		SeekableByteChannel _in = NIOUtils.readableChannel(new File("src/test/resources/issue180/big_buck_bunny.mp4"));
		FrameGrab fg = FrameGrab.createFrameGrab(_in);
		int totalFrames = fg.getVideoTrack().getMeta().getTotalFrames();
		assertEquals(1440, totalFrames);

		fg.seekToFramePrecise(386);
		compareOneFrame(fg, 387);

		_in.close();
	}

	private void compareOneFrame(FrameGrab fg, int frameNo) throws IOException {
		Picture decoded = fg.getNativeFrame();
		if (decoded == null)
			return;
		Picture raw = readRaw(new File("src/test/resources/issue180/frame_00000387.yuv"),
				decoded.getCroppedWidth(), decoded.getCroppedHeight());
		decoded = decoded.cropped();
		
//		ImageIO.write(AWTUtil.toBufferedImage(decoded), "png", new File("/tmp/decoded.png"));
//		ImageIO.write(AWTUtil.toBufferedImage(raw), "png", new File("/tmp/raw.png"));

		assertByteArrayApproximatelyEquals(raw.getPlaneData(0), decoded.getPlaneData(0), 20);
		assertByteArrayApproximatelyEquals(raw.getPlaneData(1), decoded.getPlaneData(1), 20);
		assertByteArrayApproximatelyEquals(raw.getPlaneData(2), decoded.getPlaneData(2), 20);
	}

	private void assertByteArrayApproximatelyEquals(byte[] rand, byte[] newRand, int threash) {
		int maxDiff = 0;
		for (int i = 0; i < rand.length; i++) {
			int diff = Math.abs(rand[i] - newRand[i]);
			if (diff > maxDiff)
				maxDiff = diff;
		}
		Assert.assertTrue("Maxdiff: " + maxDiff, maxDiff < threash);
	}

	private Picture readRaw(File file, int width, int height) throws IOException {
		ByteBuffer rawBuffer = NIOUtils.fetchFromFile(file);
		Picture result = Picture.create(width, height, ColorSpace.YUV420);

		for (int i = 0; i < width * height; i++) {
			result.getPlaneData(0)[i] = (byte) ((rawBuffer.get() & 0xff) - 128);
		}

		for (int comp = 1; comp < 3; comp++) {
			for (int i = 0; i < ((width * height) >> 2); i++) {
				result.getPlaneData(comp)[i] = (byte) ((rawBuffer.get() & 0xff) - 128);
			}
		}

		return result;
	}
}
