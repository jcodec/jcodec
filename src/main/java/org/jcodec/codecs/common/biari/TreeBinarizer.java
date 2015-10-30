package org.jcodec.codecs.common.biari;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Binarization and context modeling using binary search tree
 * 
 * @author The JCodec project
 * 
 */
public class TreeBinarizer {

	private Context[] models;

	// private int nZeros[];
	// private int nOnes[];

	public TreeBinarizer() {

		initContextModels();
	}

	private void initContextModels() {
		// nZeros = new int[255];
		// nOnes = new int[255];
		models = new Context[255];
		for (int i = 0; i < 255; i++) {
			models[i] = new Context(0, 0);
			// nZeros[i] = 1;
			// nOnes[i] = 1;
		}
	}

	public void binarize(int symbol, MQEncoder encoder) throws IOException {

		int inverted = 0;
		int nextModel = 0;
		int levelOffset = 0;
		for (int i = 0; i < 8; ++i) {
			int bin = (symbol >> (7 - i)) & 0x1;
			encoder.encode(bin, models[nextModel]);
			// updateModel(nextModel, bin);

			inverted |= bin << i;
			levelOffset += (1 << i);
			nextModel = levelOffset + inverted;
		}

	}

	public int debinarize(MQDecoder decoder) throws IOException {

		int symbol = 0;
		int inverted = 0;
		int nextModel = 0;
		int levelOffset = 0;
		for (int i = 0; i < 8; ++i) {
			int bin = decoder.decode(models[nextModel]);
			symbol |= (bin << (7 - i));
			// updateModel(nextModel, bin);

			inverted |= bin << i;
			levelOffset += (1 << i);
			nextModel = levelOffset + inverted;
		}

		return symbol;
	}

	// private void updateModel(int mId, int bin) {
	// if (bin == 0)
	// ++nZeros[mId];
	// else
	// ++nOnes[mId];
	//
	// ContextModel cm = models[mId];
	// if (nZeros[mId] > nOnes[mId]) {
	// cm.setMps(0);
	// cm.setState((double) nOnes[mId] / (nZeros[mId] + nOnes[mId]));
	// } else {
	// cm.setMps(1);
	// cm.setState((double) nZeros[mId] / (nZeros[mId] + nOnes[mId]));
	// }
	// }
}