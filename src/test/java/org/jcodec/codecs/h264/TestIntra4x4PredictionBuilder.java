package org.jcodec.codecs.h264;

import org.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import org.jcodec.codecs.h264.decode.model.BlockBorder;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.junit.Test;

public class TestIntra4x4PredictionBuilder extends JAVCTestCase {

	@Test
	public void testDC() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		int[] left = new int[] { 9, 10, 11, 12 };
		int topLeft = 13;

		ipb.predictDC(new BlockBorder(left, top, topLeft), new PixelBuffer(
				pred, 0, 2));

		assertArrayEquals(new int[] { 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
				7, 7 }, pred);
	}

	@Test
	public void testVertical() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		int[] left = new int[] { 9, 10, 11, 12 };
		int topLeft = 13;
		ipb.predictVertical(new BlockBorder(left, top, topLeft),
				new PixelBuffer(pred, 0, 2));

		assertArrayEquals(new int[] { 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2,
				3, 4 }, pred);
	}

	@Test
	public void testHorizontal() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		int[] left = new int[] { 9, 10, 11, 12 };
		int topLeft = 13;
		ipb.predictHorizontal(new BlockBorder(left, top, topLeft),
				new PixelBuffer(pred, 0, 2));

		assertArrayEquals(new int[] { 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11,
				11, 12, 12, 12, 12 }, pred);
	}

	@Test
	public void testDiagonalDownLeft() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 209, 212, 218, 222, 216, 219, 225, 229 };
		int[] left = new int[] { 0, 0, 0, 0 };
		int topLeft = 0;
		ipb.predictDiagonalDownLeft(new BlockBorder(left, top, topLeft),
				new PixelBuffer(pred, 0, 2));

		assertArrayEquals(new int[] { 213, 218, 220, 218, 218, 220, 218, 220,
				220, 218, 220, 225, 218, 220, 225, 228 }, pred);
	}

	@Test
	public void testDiagonalDownRight() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 183, 196, 170, 131, 0, 0, 0, 0 };
		int[] left = new int[] { 207, 207, 207, 207 };
		int topLeft = 196;
		ipb.predictDiagonalDownRight(new BlockBorder(left, top, topLeft),
				new PixelBuffer(pred, 0, 2));

		assertArrayEquals(new int[] { 196, 190, 186, 167, 204, 196, 190, 186,
				207, 204, 196, 190, 207, 207, 204, 196 }, pred);
	}

	@Test
	public void testVerticalRight() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 207, 201, 197, 175, 0, 0, 0, 0 };
		int[] left = new int[] { 208, 176, 129, 122 };
		int topLeft = 206;
		ipb.predictVerticalRight(new BlockBorder(left, top, topLeft),
				new PixelBuffer(pred, 0, 2));

		assertArrayEquals(new int[] { 207, 204, 199, 186, 207, 205, 202, 193,
				200, 207, 204, 199, 172, 207, 205, 202 }, pred);
	}

	@Test
	public void testHorizontalDown() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 209, 157, 114, 118 };
		int[] left = new int[] { 197, 198, 202, 205 };
		int topLeft = 204;
		ipb.predictHorizontalDown(new BlockBorder(left, top, topLeft),
				new PixelBuffer(pred, 0, 2));

		assertArrayEquals(new int[] { 201, 204, 195, 159, 198, 199, 201, 204,
				200, 199, 198, 199, 204, 202, 200, 199 }, pred);
	}

	@Test
	public void testVerticalLeft() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 215, 201, 173, 159, 137, 141, 150, 155 };
		int[] left = new int[] { 0, 0, 0, 0 };
		int topLeft = 0;
		ipb.predictVerticalLeft(new BlockBorder(left, top, topLeft),
				new PixelBuffer(pred, 0, 2));

		assertArrayEquals(new int[] { 208, 187, 166, 148, 198, 177, 157, 144,
				187, 166, 148, 139, 177, 157, 144, 142 }, pred);
	}

	@Test
	public void testHorizontalUp() throws Exception {

		Intra4x4PredictionBuilder ipb = new Intra4x4PredictionBuilder(8);

		int[] pred = new int[16];

		int[] top = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		int[] left = new int[] { 175, 180, 216, 221 };
		int topLeft = 0;
		ipb.predictHorizontalUp(new BlockBorder(left, top, topLeft),
				new PixelBuffer(pred, 0, 2));

		assertArrayEquals(new int[] { 178, 188, 198, 208, 198, 208, 219, 220,
				219, 220, 221, 221, 221, 221, 221, 221 }, pred);
	}

}
