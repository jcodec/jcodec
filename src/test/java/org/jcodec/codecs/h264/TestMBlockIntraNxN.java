package org.jcodec.codecs.h264;

import java.io.ByteArrayInputStream;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.IntraNxNPrediction;
import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;
import org.jcodec.codecs.h264.io.model.MBlockNeighbourhood;
import org.jcodec.codecs.h264.io.model.ResidualBlock;
import org.jcodec.codecs.h264.io.read.IntraMBlockReader;
import org.jcodec.codecs.util.BinUtil;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.InBits;
import org.junit.Test;

public class TestMBlockIntraNxN extends JAVCTestCase {

	@Test
	public void test1() throws Exception {
		// MB 92

		String bits = "0011 0000 0010 0011 0000 0100 0011 1 0100 1 1 0110 0101 1 0001 1 1 1 1"
				+ "000011 101 1 101 11 10 10 1100 001 101 00 1011 010 01 011 010 10 1 01111 0 01"
				+ "100 00 01110 01 1 0101 0100 100 01 101 11 011 10 0100 010 0001 101 010 00"
				+ "1110 0 1 1010 110 1 11 110 000 01011 01 1 0011 0101 1 00 0001010 10 1 10 011"
				+ "011 0011 11 011 11 10 01 1 0 001010 0 1 11 011 0010 10 11 11 10 001 0 1110 1"
				+ "1 00011 101 100 010 1 01001 11 01 11 10 111 10 01 1 1 1010 011 1 10 101 11 010"
				+ "1 01 000011 1 011 010 1 000011 0001 10 0011 0 1 0 1 00011 010 111 0 1 11 0000100"
				+ "011 0000001 0101 111 11 01 0 1101 01 101 1 11 00000101 10 1 11 00011";

		InBits reader = new BitstreamReader(new ByteArrayInputStream(BinUtil
				.binaryStringToBytes(bits)));

		IntraMBlockReader intraMBlockReader = new IntraMBlockReader(false,
				ChromaFormat.YUV_420, false);

		// MB 81
		IntraNxNPrediction predTop = new IntraNxNPrediction(new int[] { 5, 6,
				4, 5, 1, 4, 4, 5, 5, 5, 0, 4, 6, 4, 4, 4 }, 1);

		CoeffToken[] lumaTop = new CoeffToken[] { new CoeffToken(3, 1),
				new CoeffToken(1, 1), new CoeffToken(8, 3),
				new CoeffToken(2, 2), new CoeffToken(2, 2),
				new CoeffToken(1, 1), new CoeffToken(0, 0),
				new CoeffToken(1, 1), new CoeffToken(2, 1),
				new CoeffToken(7, 3), new CoeffToken(0, 0),
				new CoeffToken(3, 3), new CoeffToken(5, 3),
				new CoeffToken(0, 0), new CoeffToken(9, 3),
				new CoeffToken(3, 3) };

		CoeffToken[] cbTop = new CoeffToken[] { new CoeffToken(1, 1),
				new CoeffToken(0, 0), new CoeffToken(1, 1),
				new CoeffToken(0, 0) };

		CoeffToken[] crTop = new CoeffToken[] { new CoeffToken(3, 2),
				new CoeffToken(0, 0), new CoeffToken(2, 2),
				new CoeffToken(2, 2) };

		// MB 91
		IntraNxNPrediction predLeft = new IntraNxNPrediction(new int[] { 2, 4,
				2, 5, 5, 0, 5, 5, 1, 1, 3, 7, 8, 5, 7, 5 }, 3);

		CoeffToken[] lumaLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(4, 3), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(2, 2),
				new CoeffToken(6, 1), new CoeffToken(5, 2),
				new CoeffToken(7, 3), new CoeffToken(3, 3),
				new CoeffToken(4, 3), new CoeffToken(2, 2),
				new CoeffToken(3, 3), new CoeffToken(6, 3),
				new CoeffToken(3, 3) };

		CoeffToken[] cbLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };
		CoeffToken[] crLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };

		MBlockNeighbourhood neighbourhood = new MBlockNeighbourhood(lumaLeft,
				lumaTop, cbLeft, cbTop, crLeft, crTop, predLeft, predTop, true, true);

		MBlockIntraNxN actual = intraMBlockReader.readMBlockIntraNxN(reader,
				neighbourhood);

		IntraNxNPrediction actualPred = actual.getPrediction();

		assertArrayEquals(new int[] { 4, 0, 2, 4, 1, 5, 4, 4, 5, 4, 5, 7, 6, 4,
				1, 1 }, actualPred.getLumaModes());

		ResidualBlock[] actualLuma = actual.getLuma();

		assertArrayEquals(new int[] { 0, 0, 0, 1, 0, -1, 0, 1, -1, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[0].getCoeffs());
		assertArrayEquals(new int[] { -1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[1].getCoeffs());
		assertArrayEquals(new int[] { 0, 0, -1, 1, 0, -1, 0, 0, 0, 0, 0, 1, 0,
				0, 0, 0 }, actualLuma[2].getCoeffs());
		assertArrayEquals(new int[] { -2, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[3].getCoeffs());
		assertArrayEquals(new int[] { 2, -1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[4].getCoeffs());
		assertArrayEquals(new int[] { 0, 0, -1, 0, 1, 0, 0, 1, -1, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[5].getCoeffs());
		assertArrayEquals(new int[] { -2, 1, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[6].getCoeffs());
		assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0 }, actualLuma[7].getCoeffs());
		assertArrayEquals(new int[] { -1, 1, 1, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[8].getCoeffs());
		assertArrayEquals(new int[] { -3, 2, 0, 0, -1, 1, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[9].getCoeffs());
		assertArrayEquals(new int[] { -2, -1, -3, -2, -2, 0, 1, 2, 0, 1, 0, -1,
				0, 0, 0, 0 }, actualLuma[10].getCoeffs());
		assertArrayEquals(new int[] { -1, 1, 3, -2, 0, -1, 0, 0, 0, 2, 0, 1, 0,
				0, 0, 0 }, actualLuma[11].getCoeffs());
		assertArrayEquals(new int[] { -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[12].getCoeffs());
		assertArrayEquals(new int[] { 0, 0, -1, 1, 0, 0, 0, 0, -1, 0, 0, 0, 0,
				0, 0, 0 }, actualLuma[13].getCoeffs());
		assertArrayEquals(new int[] { 0, 1, -1, -2, 0, -1, 0, -1, 0, 0, 0, 0,
				0, 0, 0, 0 }, actualLuma[14].getCoeffs());
		assertArrayEquals(new int[] { 0, 1, 0, 1, -1, 0, 0, 0, -1, 1, 0, 0, 0,
				0, 0, 0 }, actualLuma[15].getCoeffs());

		assertEquals(0, actual.getQpDelta());

		assertArrayEquals(new int[] { 2, -2, 2, 0 }, actual.getChroma()
				.getCbDC().getCoeffs());
		assertArrayEquals(new int[] { -3, 0, 1, -3 }, actual.getChroma()
				.getCrDC().getCoeffs());

		assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0 }, actual.getChroma().getCbAC()[0].getCoeffs());
		assertArrayEquals(new int[] { 1, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0 }, actual.getChroma().getCbAC()[1].getCoeffs());
		assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0 }, actual.getChroma().getCbAC()[2].getCoeffs());
		assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0 }, actual.getChroma().getCbAC()[3].getCoeffs());

		assertArrayEquals(new int[] { -3, 4, 0, -1, 0, 0, -1, 1, 0, 0, 0, 0, 0,
				0, 0 }, actual.getChroma().getCrAC()[0].getCoeffs());
		assertArrayEquals(new int[] { 0, 0, -1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0 }, actual.getChroma().getCrAC()[1].getCoeffs());
		assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0 }, actual.getChroma().getCrAC()[2].getCoeffs());
		assertArrayEquals(new int[] { -1, 2, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0 }, actual.getChroma().getCrAC()[3].getCoeffs());

	}
}
