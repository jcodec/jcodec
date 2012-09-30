package org.jcodec.codecs.h264.decode.aso;

import org.jcodec.codecs.h264.JAVCTestCase;
import org.junit.Test;

public class TestSliceGroupMapBuilder extends JAVCTestCase {
	
	@Test
	public void testInterleaved() throws Exception {
		int[] expectedMap = new int[] {
			0, 0, 1, 1, 1, 2, 2, 2, 2, 0, 0, 1,
			1, 1, 2, 2, 2, 2, 0, 0, 1, 1, 1, 2,
			2, 2, 2, 0, 0, 1, 1, 1, 2, 2, 2, 2,
			0, 0, 1, 1, 1, 2, 2, 2, 2, 0, 0, 1,
			1, 1, 2, 2, 2, 2, 0, 0, 1, 1, 1, 2,
			2, 2, 2, 0, 0, 1, 1, 1, 2, 2, 2, 2,
			0, 0, 1, 1, 1, 2, 2, 2, 2, 0, 0, 1,
			1, 1, 2, 2, 2, 2, 0, 0, 1, 1, 1, 2,
			2, 2, 2, 0, 0, 1, 1, 1, 2, 2, 2, 2,
			0, 0, 1, 1, 1, 2, 2, 2, 2, 0, 0, 1,
			1, 1, 2, 2, 2, 2, 0, 0, 1, 1, 1, 2,
			2, 2, 2, 0, 0, 1, 1, 1, 2, 2, 2, 2
		};
		
		int[] groups = SliceGroupMapBuilder.buildInterleavedMap(12, 12, new int[] {2, 3, 4});
		
		assertArrayEquals(expectedMap, groups);
	}
	
	public void testDispersed() throws Exception {
		int[] expectedMap = new int[] {
			0, 1, 2, 0, 1, 2, 0, 1,
			1, 2, 0, 1, 2, 0, 1, 2,
			0, 1, 2, 0, 1, 2, 0, 1,
			1, 2, 0, 1, 2, 0, 1, 2,
			0, 1, 2, 0, 1, 2, 0, 1,
			1, 2, 0, 1, 2, 0, 1, 2,
			0, 1, 2, 0, 1, 2, 0, 1,
			1, 2, 0, 1, 2, 0, 1, 2
		};
		
		int[] groups = SliceGroupMapBuilder.buildDispersedMap(8, 8, 3);

		assertArrayEquals(expectedMap, groups);
	}
	
	public void testForeground() throws Exception {
		int[] expectedMap = new int[] {
			2, 2, 2, 2, 2, 2, 2, 2,
			2, 1, 1, 1, 2, 2, 2, 2,
			2, 1, 1, 1, 2, 2, 2, 2,
			2, 1, 1, 1, 2, 2, 2, 2,
			2, 2, 0, 0, 0, 0, 2, 2,
			2, 2, 0, 0, 0, 0, 2, 2,
			2, 2, 0, 0, 0, 0, 2, 2,
			2, 2, 2, 2, 2, 2, 2, 2
		};

		int[] groups = SliceGroupMapBuilder.buildForegroundMap(8, 8,
				3, new int[] { 34, 9 }, new int[] { 53, 27 });

		assertArrayEquals(expectedMap, groups);
	}
	
	public void testBoxout() throws Exception {
		int[] expectedMap = new int[] {
			1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 0, 0, 0, 0, 0, 1,
			1, 1, 0, 0, 0, 0, 0, 1,
			1, 0, 0, 0, 0, 0, 0, 1,
			1, 0, 0, 0, 0, 0, 0, 1,
			1, 0, 0, 0, 0, 0, 0, 1,
			1, 1, 1, 1, 1, 1, 1, 1
		};

		int[] groups = SliceGroupMapBuilder.buildBoxOutMap(8, 8,
				false, 28);
		assertArrayEquals(expectedMap, groups);
	}
	
	public void testRaster() throws Exception {
		int[] expectedMap = new int[] {
			0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1
		};
		
		int[] groups = SliceGroupMapBuilder.buildRasterScanMap(8, 8,
				12, false);
		assertArrayEquals(expectedMap, groups);
	}
	
	public void testWipe() throws Exception {
		int[] expectedMap = new int[] {
			0, 0, 0, 0, 1, 1, 1, 1,
			0, 0, 0, 0, 1, 1, 1, 1,
			0, 0, 0, 0, 1, 1, 1, 1,
			0, 0, 0, 1, 1, 1, 1, 1,
			0, 0, 0, 1, 1, 1, 1, 1,
			0, 0, 0, 1, 1, 1, 1, 1,
			0, 0, 0, 1, 1, 1, 1, 1,
			0, 0, 0, 1, 1, 1, 1, 1
		};

		int[] groups = SliceGroupMapBuilder.buildWipeMap(8, 8, 27,
				false);
		assertArrayEquals(expectedMap, groups);
	}
}
