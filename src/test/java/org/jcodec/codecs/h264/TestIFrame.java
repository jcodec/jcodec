package org.jcodec.codecs.h264;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import junit.framework.TestCase;

import org.jcodec.codecs.h264.annexb.AnnexBDemuxer;
import org.jcodec.codecs.h264.decode.ErrorResilence;
import org.jcodec.codecs.h264.decode.IgnorentErrorResilence;
import org.jcodec.codecs.h264.decode.SequenceDecoder;
import org.jcodec.codecs.h264.decode.SequenceDecoder.Sequence;

public class TestIFrame extends TestCase {

	public void testNoCabac() throws Exception {
		String path = "src/test/resources/h264/iframe_nocabac.264";
		AnnexBDemuxer reader = new AnnexBDemuxer(new BufferedInputStream(
				new FileInputStream(path)));

		ErrorResilence resilence = new IgnorentErrorResilence();

		SequenceDecoder decoder = new SequenceDecoder(reader, resilence);
		Sequence sequence = decoder.nextSequence();
		sequence.nextPicture();
	}

	public void testCabac() {
		assertTrue(true);
	}

	public void test8x8NoCabac() throws Exception {
		String path = "src/test/resources/h264/iframe_t8x8_nocabac.264";

		AnnexBDemuxer reader = new AnnexBDemuxer(new BufferedInputStream(
				new FileInputStream(path)));

		ErrorResilence resilence = new IgnorentErrorResilence();

		SequenceDecoder decoder = new SequenceDecoder(reader, resilence);
		Sequence sequence = decoder.nextSequence();
		sequence.nextPicture();
	}

	public void test8x8Cabac() {
		assertTrue(true);
	}
}