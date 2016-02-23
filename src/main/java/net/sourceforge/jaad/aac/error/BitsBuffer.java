package net.sourceforge.jaad.aac.error;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.IBitStream;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
public class BitsBuffer {

	int bufa, bufb, len;

	public BitsBuffer() {
		len = 0;
	}

	public int getLength() {
		return len;
	}

	public int showBits(int bits) {
		if(bits==0) return 0;
		if(len<=32) {
			//huffman_spectral_data_2 needs to read more than may be available,
			//bits maybe > len, deliver 0 than
			if(len>=bits) return ((bufa>>(len-bits))&(0xFFFFFFFF>>(32-bits)));
			else return ((bufa<<(bits-len))&(0xFFFFFFFF>>(32-bits)));
		}
		else {
			if((len-bits)<32) return ((bufb&(0xFFFFFFFF>>(64-len)))<<(bits-len+32))|(bufa>>(len-bits));
			else return ((bufb>>(len-bits-32))&(0xFFFFFFFF>>(32-bits)));
		}
	}

	public boolean flushBits(int bits) {
		len -= bits;

		boolean b;
		if(len<0) {
			len = 0;
			b = false;
		}
		else b = true;
		return b;
	}

	public int getBits(int n) {
		int i = showBits(n);
		if(!flushBits(n)) i = -1;
		return i;
	}

	public int getBit() {
		int i = showBits(1);
		if(!flushBits(1)) i = -1;
		return i;
	}

	public void rewindReverse() {
		if(len==0) return;
		final int[] i = HCR.rewindReverse64(bufb, bufa, len);
		bufb = i[0];
		bufa = i[1];
	}

	//merge bits of a to b
	public void concatBits(BitsBuffer a) {
		if(a.len==0) return;
		int al = a.bufa;
		int ah = a.bufb;

		int bl, bh;
		if(len>32) {
			//mask off superfluous high b bits
			bl = bufa;
			bh = bufb&((1<<(len-32))-1);
			//left shift a len bits
			ah = al<<(len-32);
			al = 0;
		}
		else {
			bl = bufa&((1<<(len))-1);
			bh = 0;
			ah = (ah<<(len))|(al>>(32-len));
			al = al<<len;
		}

		//merge
		bufa = bl|al;
		bufb = bh|ah;

		len += a.len;
	}

	public void readSegment(int segwidth, IBitStream _in) throws AACException {
		len = segwidth;

		if(segwidth>32) {
			bufb = _in.readBits(segwidth-32);
			bufa = _in.readBits(32);
		}
		else {
			bufa = _in.readBits(segwidth);
			bufb = 0;
		}
	}
}
