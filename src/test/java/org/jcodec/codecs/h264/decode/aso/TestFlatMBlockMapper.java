package org.jcodec.codecs.h264.decode.aso;

import org.jcodec.codecs.h264.JAVCTestCase;

public class TestFlatMBlockMapper extends JAVCTestCase {

	public void test1() throws Exception {
		FlatMBlockMapper mapper = new FlatMBlockMapper(40, 100);

		assertEquals(-1, mapper.getLeftMBIdx(0));
		assertEquals(-1, mapper.getTopMBIdx(0));
		assertEquals(-1, mapper.getTopLeftMBIndex(0));
		assertEquals(-1, mapper.getTopRightMBIndex(0));

		assertEquals(0, mapper.getLeftMBIdx(1));
		assertEquals(0, mapper.getTopMBIdx(40));
		assertEquals(39, mapper.getLeftMBIdx(40));
		assertEquals(-1, mapper.getTopLeftMBIndex(40));
		assertEquals(1, mapper.getTopRightMBIndex(40));

		assertArrayEquals(new int[] { 100, 101, 102, 103, 104, 105, 106, 107,
				108, 109 }, mapper.getAddresses(10));
	}
}
