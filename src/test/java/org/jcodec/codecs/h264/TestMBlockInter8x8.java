package org.jcodec.codecs.h264;

import static org.jcodec.codecs.h264.io.model.SubMBType.L0_4x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_8x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_8x8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.Inter8x8Prediction;
import org.jcodec.codecs.h264.io.model.MBlockInter8x8;
import org.jcodec.codecs.h264.io.model.MBlockNeighbourhood;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.io.model.SubMBType;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.codecs.h264.io.read.CAVLCReader;
import org.jcodec.codecs.h264.io.read.InterMBlockReader;
import org.jcodec.codecs.util.BinUtil;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.DummyBitstreamReader;
import org.jcodec.common.io.InBits;
import org.jcodec.common.io.LogInBits;
import org.unitils.thirdparty.org.apache.commons.io.FileUtils;

public class TestMBlockInter8x8 extends JAVCTestCase {

	public void testMB61() throws Exception {

		// @25562 mb_type 00101 ( 4)

		// MB 50
		CoeffToken[] coeffsLumaTop = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(1, 1), new CoeffToken(0, 0),
				new CoeffToken(2, 2), new CoeffToken(0, 0),
				new CoeffToken(1, 1), new CoeffToken(0, 0),
				new CoeffToken(2, 2) };
		CoeffToken[] coeffsCbTop = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };
		CoeffToken[] coeffsCrTop = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };

		// MB 60
		CoeffToken[] coeffsLumaLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };
		CoeffToken[] coeffsCbLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };
		CoeffToken[] coeffsCrLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };

		MBlockNeighbourhood neighbourhood = new MBlockNeighbourhood(
				coeffsLumaLeft, coeffsLumaTop, coeffsCbLeft, coeffsCbTop,
				coeffsCrLeft, coeffsCrTop, null, null, true, true);

		// MB 61

		String bits = "1 010 1 010 0001001 011 0001011 1 011 1 010 1 00111 011 00110 1"
				+ "00000101000 1 001 11 100 00 000101 01 1 1 1 001 01 101 00 1 1 1 1 1"
				+ "001 1 0 001";

		InterMBlockReader mBlockReader = new InterMBlockReader(false,
				ChromaFormat.YUV_420, false, 1, 5, true);

		InBits reader = new BitstreamReader(new ByteArrayInputStream(BinUtil
				.binaryStringToBytes(bits)));

		MBlockInter8x8 actual = mBlockReader.readMBlockInter8x8(reader,
				neighbourhood, 4, SliceType.P, false);

		Inter8x8Prediction actualPred = actual.getPrediction();

		assertArrayEquals(new SubMBType[] { L0_8x8, L0_8x4, L0_8x8, L0_8x4 },
				actualPred.getSubMbTypes());

		assertArrayEquals(new Vector[] { new Vector(-4, -1, 0) }, actualPred
				.getDecodedMVsL0()[0]);
		assertArrayEquals(new Vector[] { new Vector(-5, 0, 0),
				new Vector(-1, 0, 0) }, actualPred.getDecodedMVsL0()[1]);
		assertArrayEquals(new Vector[] { new Vector(1, 0, 0) }, actualPred
				.getDecodedMVsL0()[2]);
		assertArrayEquals(new Vector[] { new Vector(-3, -1, 0),
				new Vector(3, 0, 0) }, actualPred.getDecodedMVsL0()[3]);

	}

	public void testMB51() throws Exception {
		// MB 40
		CoeffToken[] coeffsLumaTop = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(1, 1), new CoeffToken(0, 0),
				new CoeffToken(1, 1), new CoeffToken(1, 1),
				new CoeffToken(2, 2), new CoeffToken(0, 0),
				new CoeffToken(4, 1) };
		CoeffToken[] coeffsCbTop = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };
		CoeffToken[] coeffsCrTop = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };

		// MB 50
		CoeffToken[] coeffsLumaLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(1, 1), new CoeffToken(0, 0),
				new CoeffToken(2, 2), new CoeffToken(0, 0),
				new CoeffToken(1, 1), new CoeffToken(0, 0),
				new CoeffToken(2, 2) };
		CoeffToken[] coeffsCbLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };
		CoeffToken[] coeffsCrLeft = new CoeffToken[] { new CoeffToken(0, 0),
				new CoeffToken(0, 0), new CoeffToken(0, 0),
				new CoeffToken(0, 0) };

		MBlockNeighbourhood neighbourhood = new MBlockNeighbourhood(
				coeffsLumaLeft, coeffsLumaTop, coeffsCbLeft, coeffsCbTop,
				coeffsCrLeft, coeffsCrTop, null, null, true, true);

		// MB 51
		// @25084 mb_skip_run 1 ( 0)
		// @25085 mb_type 00101 ( 4)
		String bits = "1 011 011 1 0001010 011 00110 1 0001111 0001110 1 00110 0001000"
				+ "011 000011010 000010111 0001100 1 01 1 1 01 1 1 10 0 011 01 1 011 1"
				+ "011 01 100 01 01 1 010 011 10 100 00 1 01 0 1 01 0 1 01 0 1 0000000110"
				+ "1 1 11 011 011 011 010 1 01 0 1100 100 100 100 011 00 100 01 11 1111111111111";

		InterMBlockReader mBlockReader = new InterMBlockReader(false,
				ChromaFormat.YUV_420, false, 1, 5, true);

		InBits reader = new BitstreamReader(new ByteArrayInputStream(BinUtil
				.binaryStringToBytes(bits)));

		MBlockInter8x8 actual = mBlockReader.readMBlockInter8x8(reader,
				neighbourhood, 4, SliceType.P, false);

		Inter8x8Prediction actualPred = actual.getPrediction();

		assertArrayEquals(new SubMBType[] { L0_8x8, L0_4x8, L0_4x8, L0_8x8 },
				actualPred.getSubMbTypes());

		assertArrayEquals(new Vector[] { new Vector(5, -1, 0) }, actualPred
				.getDecodedMVsL0()[0]);
		assertArrayEquals(new Vector[] { new Vector(3, 0, 0),
				new Vector(-7, 7, 0) }, actualPred.getDecodedMVsL0()[1]);
		assertArrayEquals(new Vector[] { new Vector(0, 3, 0),
				new Vector(4, -1, 0) }, actualPred.getDecodedMVsL0()[2]);
		assertArrayEquals(new Vector[] { new Vector(13, -11, 0) }, actualPred
				.getDecodedMVsL0()[3]);
	}
}
