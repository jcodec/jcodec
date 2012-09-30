package org.jcodec.codecs.h264;

import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.junit.Test;

public class TestCoeffTransformer extends JAVCTestCase {

	@Test
	public void testDCT() throws Exception {

		int[] coeffs = { 2560, -4160, -2816, -1920, 960, -1200, 320, 0, 768, -640, -512, 320, 0, 0, 0, 0 };

		int[] expected = { -86, 92, 136, 126, -80, 72, 66, 84, -81, 71, 47, 45, -89, 91, 97, 49 };

		CoeffTransformer ct = new CoeffTransformer(null);
		int[] actual = ct.transformIDCT4x4(coeffs);

		assertArrayEquals(expected, actual);
	}

	@Test
	public void testHadamard() throws Exception {

		int[] coeffs = { 61, -11, -13, -14, 9, -4, -2, 2, -12, 6, 6, 4, 8, 1, -2, 2 };

		int[] expected = { 41, 75, 79, 69, 15, 89, 97, 95, 23, 97, 85, 83, 13, 47, 23, 45 };

		CoeffTransformer ct = new CoeffTransformer(null);
		int[] actual = ct.transformIHadamard4x4(coeffs);

		assertArrayEquals(expected, actual);

	}

	@Test
	public void testRescaleAfterIHadamard4x4() throws Exception {
		int[] coeffs = { 41, 75, 79, 69, 15, 89, 97, 95, 23, 97, 85, 83, 13, 47, 23, 45 };
		int[] expected = { 2624, 4800, 5056, 4416, 960, 5696, 6208, 6080, 1472, 6208, 5440, 5312, 832, 3008, 1472, 2880 };

		CoeffTransformer ct = new CoeffTransformer(null);
		int[] actual = ct.rescaleAfterIHadamard4x4(coeffs, 28);

		assertArrayEquals(expected, actual);
	}

	@Test
	public void testRescaleBeforeIDCT4x4() throws Exception {
		int[] coeffs = { 10, -13, -11, -6, 3, -3, 1, 0, 3, -2, -2, 1, 0, 0, 0, 0 };
		int[] expected = { 2560, -4160, -2816, -1920, 960, -1200, 320, 0, 768, -640, -512, 320, 0, 0, 0, 0 };

		CoeffTransformer ct = new CoeffTransformer(null);
		int[] actual = ct.rescaleBeforeIDCT4x4(coeffs, 28);

		assertArrayEquals(expected, actual);
	}

	@Test
	public void testReorder() throws Exception {
		int[] coeffs = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
		int[] expected = { 0, 1, 5, 6, 2, 4, 7, 12, 3, 8, 11, 13, 9, 10, 14, 15 };

		CoeffTransformer ct = new CoeffTransformer(null);
		int[] reordered = ct.reorderCoeffs(coeffs);

		assertArrayEquals(expected, reordered);
	}
}
