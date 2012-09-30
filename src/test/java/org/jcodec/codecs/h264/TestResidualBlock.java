package org.jcodec.codecs.h264;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jcodec.codecs.h264.io.model.ChromaFormat;
import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.ResidualBlock.BlockType;
import org.jcodec.codecs.h264.io.read.CAVLCReader;
import org.jcodec.codecs.h264.io.read.ResidualCoeffsCAVLCReader;
import org.jcodec.codecs.util.BinUtil;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.InBits;
import org.junit.Test;

public class TestResidualBlock extends JAVCTestCase {

	@Test
	public void testLuma1() throws Exception {

		// 0000100
		String code = "01110010111101101";
		int[] coeffs = { 0, 3, 0, 1, -1, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

		testResidual(code, coeffs, new CoeffToken(5, 3),
				BlockType.BLOCK_LUMA_4x4);
	}

	@Test
	public void testLuma2() throws Exception {
		// 0000000110
		String code = "10001001000010111001100";
		int[] coeffs = { -2, 4, 3, -3, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		testResidual(code, coeffs, new CoeffToken(5, 1),
				BlockType.BLOCK_LUMA_4x4);
	}

	@Test
	public void testLuma3() throws Exception {
		// 00011
		String code = "10001110010";
		int[] coeffs = { 0, 0, 0, 1, 0, 1, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0 };

		testResidual(code, coeffs, new CoeffToken(3, 3),
				BlockType.BLOCK_LUMA_4x4);
	}

	@Test
	public void testChromaAC3() throws Exception {
		// 00011
		String code = "000 1 010 0011 10 101 00";

		int[] coeffs = { 1, -3, 2, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 };

		testResidual(code, coeffs, new CoeffToken(7, 3),
				BlockType.BLOCK_CHROMA_AC);
	}

	private void testResidual(String code, int[] expected, CoeffToken token,
			BlockType blockType) throws IOException {
		InBits reader = new BitstreamReader(new ByteArrayInputStream(BinUtil
				.binaryStringToBytes(code)));

		ResidualCoeffsCAVLCReader residualReader = new ResidualCoeffsCAVLCReader(
				ChromaFormat.YUV_420);
		int[] actual = residualReader.readCoeffs(reader, blockType, token);

		assertArrayEquals(expected, actual);
	}
}
