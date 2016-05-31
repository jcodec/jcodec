package net.sourceforge.jaad.aac.huffman;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.IBitStream;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
//TODO: implement decodeSpectralDataER
public class Huffman implements Codebooks {

	private static final boolean[] UNSIGNED = {false, false, true, true, false, false, true, true, true, true, true};
	private static final int QUAD_LEN = 4, PAIR_LEN = 2;

	private Huffman() {
	}

	private static int findOffset(IBitStream _in, int[][] table) throws AACException {
		int off = 0;
		int len = table[off][0];
		int cw = _in.readBits(len);
		int j;
		while(cw!=table[off][1]) {
			off++;
			j = table[off][0]-len;
			len = table[off][0];
			cw <<= j;
			cw |= _in.readBits(j);
		}
		return off;
	}

	private static void signValues(IBitStream _in, int[] data, int off, int len) throws AACException {
		for(int i = off; i<off+len; i++) {
			if(data[i]!=0) {
				if(_in.readBool()) data[i] = -data[i];
			}
		}
	}

	private static int getEscape(IBitStream _in, int s) throws AACException {
		final boolean neg = s<0;

		int i = 4;
		while(_in.readBool()) {
			i++;
		}
		final int j = _in.readBits(i)|(1<<i);

		return (neg ? -j : j);
	}

	public static int decodeScaleFactor(IBitStream _in) throws AACException {
		final int offset = findOffset(_in, HCB_SF);
		return HCB_SF[offset][2];
	}

	public static void decodeSpectralData(IBitStream _in, int cb, int[] data, int off) throws AACException {
		final int[][] HCB = CODEBOOKS[cb-1];

		//find index
		final int offset = findOffset(_in, HCB);

		//copy data
		data[off] = HCB[offset][2];
		data[off+1] = HCB[offset][3];
		if(cb<5) {
			data[off+2] = HCB[offset][4];
			data[off+3] = HCB[offset][5];
		}

		//sign & escape
		if(cb<11) {
			if(UNSIGNED[cb-1]) signValues(_in, data, off, cb<5 ? QUAD_LEN : PAIR_LEN);
		}
		else if(cb==11||cb>15) {
			signValues(_in, data, off, cb<5 ? QUAD_LEN : PAIR_LEN); //virtual codebooks are always unsigned
			if(Math.abs(data[off])==16) data[off] = getEscape(_in, data[off]);
			if(Math.abs(data[off+1])==16) data[off+1] = getEscape(_in, data[off+1]);
		}
		else throw new AACException("Huffman: unknown spectral codebook: "+cb);
	}
}
