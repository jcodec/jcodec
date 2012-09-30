package org.jcodec.codecs.h264;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import junit.framework.TestCase;

import org.jcodec.codecs.h264.annexb.AnnexBDemuxer;

public class TestPFrame extends TestCase {
	public void test8x8NoCabac() throws Exception {
		String path = "src/test/resources/h264/pframe_nocabac.264";

		AnnexBDemuxer reader = new AnnexBDemuxer(new BufferedInputStream(
				new FileInputStream(path)));

		H264Decoder h264Decoder = new H264Decoder(reader);
		h264Decoder.nextPicture();
		h264Decoder.nextPicture();
	}
}
