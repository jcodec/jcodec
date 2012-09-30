package org.jcodec.codecs.common.biari;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Binary ariphmetic decoder
 * 
 * Half-way to MQ Coder
 * 
 * @author Jay Codec
 */
public class MQDecoder {

	// 24bit 'C' register
	private int range;

	// 16bit 'A' register
	private int value;

	// 't' variable
	private int availableBits;

	// 'T' variable
	private int lastByte;

	// 'L' variable
	private int decodedBytes;

	private InputStream is;

	public MQDecoder(InputStream is) throws IOException {
		this.is = is;

		range = 0x8000;
		value = 0;

		fetchByte();
		value <<= 8;
		fetchByte();
		value <<= (availableBits - 1);

		availableBits = 1;
	}

	public int decode(Context cm) throws IOException {

		int rangeLps = MQConst.pLps[cm.getState()];

		int decoded;
		if (value > rangeLps) {
			// MPS
			range -= rangeLps;
			value -= rangeLps;
			if (range < 0x8000) {
				while (range < 0x8000)
					renormalize();

				cm.setState(MQConst.transitMPS[cm.getState()]);
			}
			decoded = cm.getMps();
		} else {
			// LPS
			range = rangeLps;
			while (range < 0x8000)
				renormalize();

			if (MQConst.mpsSwitch[cm.getState()] != 0)
				cm.setMps(1 - cm.getMps());
			cm.setState(MQConst.transitLPS[cm.getState()]);

			decoded = 1 - cm.getMps();
		}

		return decoded;
	}

	private void fetchByte() throws IOException {
		availableBits = 8;
		if (decodedBytes > 0 && lastByte == 0xff) {
			availableBits = 7;
		}
		lastByte = is.read();
		int shiftCarry = 8 - availableBits;
		value += (lastByte << shiftCarry);

		++decodedBytes;
	}

	private void renormalize() throws IOException {
		value <<= 1;
		range <<= 1;
		range &= 0xffff;

		--availableBits;
		if (availableBits == 0)
			fetchByte();
	}
}