package org.jcodec.codecs.h264.decode.aso;

import org.jcodec.codecs.h264.JAVCTestCase;

public class TestPrebuiltMBlockMapper extends JAVCTestCase {
	
	public void test1() throws Exception {
		
		int[] groups = new int[] {
			0, 0, 0, 1, 1, 1, 2, 2, 2,
			0, 0, 0, 1, 1, 1, 2, 2, 2,
			0, 0, 0, 0, 0, 0, 0, 0, 0,
			1, 1, 1, 1, 1, 1, 1, 1, 1,
			2, 2, 2, 2, 2, 2, 2, 2, 2,
			2, 2, 2, 1, 1, 1, 0, 0, 0,
			2, 2, 2, 1, 1, 1, 0, 0, 0,
			2, 2, 2, 1, 1, 1, 0, 0, 0,
			0, 0, 0, 1, 1, 1, 2, 2, 2
		};
		
		int[] indices = new int[] {
			 0,  1,  2,  0,  1,  2,  0,  1,  2,
			 3,  4,  5,  3,  4,  5,  3,  4,  5,
			 6,  7,  8,  9, 10, 11, 12, 13, 14,
			 6,  7,  8,  9, 10, 11, 12, 13, 14,
			 6,  7,  8,  9, 10, 11, 12, 13, 14,
			15, 16, 17, 15, 16, 17, 15, 16, 17,
			18, 19, 20, 18, 19, 20, 18, 19, 20,
			21, 22, 23, 21, 22, 23, 21, 22, 23,
			24, 25, 26, 24, 25, 26, 24, 25, 26
		};
		
		int[][] inverse = new int[][] {
				new int[] { 0, 1, 2, 9, 10, 11, 18, 19, 20, 21, 22, 23, 24, 25,
						26, 51, 52, 53, 60, 61, 62, 69, 70, 71, 72, 73, 74 },
				new int[] { 3, 4, 5, 12, 13, 14, 27, 28, 29, 30, 31, 32, 33,
						34, 35, 48, 49, 50, 57, 58, 59, 66, 67, 68, 75, 76, 77 },
				new int[] { 6, 7, 8, 15, 16, 17, 36, 37, 38, 39, 40, 41, 42,
						43, 44, 45, 46, 47, 54, 55, 56, 63, 64, 65, 78, 79, 80 } };
		
		MBToSliceGroupMap map = new MBToSliceGroupMap(groups, indices, inverse);
		PrebuiltMBlockMapper mapper = new PrebuiltMBlockMapper(map, 13, 9);
		assertEquals(mapper.getLeftMBIdx(12), 11);
		assertEquals(mapper.getTopMBIdx(12), -1);
		assertEquals(mapper.getTopLeftMBIndex(12), -1);
		assertEquals(mapper.getTopRightMBIndex(12), -1);
		assertEquals(mapper.getLeftMBIdx(16), 15);
		assertEquals(mapper.getTopMBIdx(16), 13);
		assertEquals(mapper.getTopLeftMBIndex(16), 12);
		assertEquals(mapper.getTopRightMBIndex(16), -1);
		
		assertEquals(mapper.getLeftMBIdx(2), -1);
		assertEquals(mapper.getTopMBIdx(2), -1);
		assertEquals(mapper.getTopLeftMBIndex(2), -1);
		assertEquals(mapper.getTopRightMBIndex(2), -1);
		
		MBToSliceGroupMap map1 = new MBToSliceGroupMap(groups, indices, inverse);
		PrebuiltMBlockMapper mapper1 = new PrebuiltMBlockMapper(map1, 16, 9);
		assertEquals(12, mapper1.getLeftMBIdx(13));
		assertEquals(4, mapper1.getTopMBIdx(13));
		assertEquals(3, mapper1.getTopLeftMBIndex(13));
		assertEquals(5, mapper1.getTopRightMBIndex(13));
	}
}
