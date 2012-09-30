package org.jcodec.codecs.common.biari;

import junit.framework.TestCase;

import org.jcodec.codecs.common.biari.BitIO.InputBits;
import org.jcodec.codecs.common.biari.BitIO.OutputBits;
import org.junit.Test;

public class TestBitIO extends TestCase {

    @Test
	public void testSmoke() throws Exception {
		byte[] someData = new byte[] {-95, 34, 23, 76, 112, 56, 20, 11, -57};
		byte[] outData = new byte[9];
		InputBits inputFromArray = BitIO.inputFromArray(someData);
		OutputBits outputBits = BitIO.outputFromArray(outData);
		int bit;
		while((bit = inputFromArray.getBit()) != -1)
			outputBits.putBit(bit);
		
		outputBits.flush();
		
		assertArrayEquals("Data ", someData, outData);
	}
	private static void assertArrayEquals(String message, byte[] expected,
			byte[] actual) {
		assertEquals(message + " length", expected.length, actual.length);

		for (int i = 0; i < expected.length; i++) {
			assertEquals(message + " element " + i, expected[i], actual[i]);
		}
	}
}
