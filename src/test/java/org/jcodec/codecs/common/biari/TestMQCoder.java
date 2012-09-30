package org.jcodec.codecs.common.biari;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jcodec.codecs.h264.JAVCTestCase;

/**
 * 
 * @author Jay Codec
 * 
 */
public class TestMQCoder extends JAVCTestCase {

	public void testSmoke() throws Exception {
		int[] input = new int[] { 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1 };

		actuallyTest(input);
	}

	public void testSmoke2() throws Exception {
		int[] input = new int[] { 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0,
				1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 1, 1, 0, 1, 1, 0 };

		actuallyTest(input);
	}

	public void testSmokeBig() throws Exception {

		byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/resources/jpeg/test.bmp"));

		int[] bits = BitIO.decompressBits(bytes);

		actuallyTest(bits);
	}

	private void actuallyTest(int[] input) throws IOException {
		byte[] encoded = encode(input);
		// printSequence(encoded, 100);

		// System.out.println("------------------------------------------------");

		int[] decoded = decode(encoded, input.length);
		// printSequence(decoded, 100);

		// System.out.println("------------------------------------------------");

		// printSequence(input, 100);

		assertArrayEquals("Array [" + input.length + "]", input, decoded);
	}

	private int[] decode(byte[] encoded, int len) throws IOException {
		MQDecoder decoder = new MQDecoder(new ByteArrayInputStream(encoded));
		Context cm = new Context(0, 0);
		int nZeros = 1, nOnes = 1;
		int[] decoded = new int[len];
		for (int i = 0; i < len; i++) {
			decoded[i] = decoder.decode(cm);
			if (decoded[i] == 0)
				++nZeros;
			else
				++nOnes;
		}

		return decoded;
	}

	private byte[] encode(int[] input) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MQEncoder encoder = new MQEncoder(baos);
		Context cm = new Context(0, 0);
		int nZeros = 1, nOnes = 1;
		for (int bin : input) {
			encoder.encode(bin, cm);
			if (bin == 0)
				++nZeros;
			else
				++nOnes;
		}
		encoder.finish();

		return baos.toByteArray();
	}

	private static void printSequence(int[] encoded, int max) {
		for (int i = 0; i < max && i < encoded.length; i++) {
			System.out.print("" + encoded[i]);
		}
		System.out.println();
	}

	private static void assertArrayEquals(String message, int[] expected,
			int[] actual) {
		assertEquals(message + " length", expected.length, actual.length);

		for (int i = 0; i < expected.length; i++) {
			assertEquals(message + " element " + i, expected[i], actual[i]);
		}
	}
}