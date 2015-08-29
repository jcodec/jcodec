package net.sourceforge.jaad.aac.filterbank;

import net.sourceforge.jaad.aac.AACException;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
class FFT implements FFTTables {

	private final int length;
	private final float[][] roots;
	private final float[][] rev;
	private float[] a, b, c, d, e1, e2;

	FFT(int length) throws AACException {
		this.length = length;

		switch(length) {
			case 64:
				roots = FFT_TABLE_64;
				break;
			case 512:
				roots = FFT_TABLE_512;
				break;
			case 60:
				roots = FFT_TABLE_60;
				break;
			case 480:
				roots = FFT_TABLE_480;
				break;
			default:
				throw new AACException("unexpected FFT length: "+length);
		}

		//processing buffers
		rev = new float[length][2];
		a = new float[2];
		b = new float[2];
		c = new float[2];
		d = new float[2];
		e1 = new float[2];
		e2 = new float[2];
	}

	void process(float[][] in, boolean forward) {
		final int imOff = (forward ? 2 : 1);
		final int scale = (forward ? length: 1);
		//bit-reversal
		int ii = 0;
		for(int i = 0; i<length; i++) {
			rev[i][0] = in[ii][0];
			rev[i][1] = in[ii][1];
			int k = length>>1;
			while(ii>=k&&k>0) {
				ii -= k;
				k >>= 1;
			}
			ii += k;
		}
		for(int i = 0; i<length; i++) {
			in[i][0] = rev[i][0];
			in[i][1] = rev[i][1];
		}

		//bottom base-4 round
		for(int i = 0; i<length; i += 4) {
			a[0] = in[i][0]+in[i+1][0];
			a[1] = in[i][1]+in[i+1][1];
			b[0] = in[i+2][0]+in[i+3][0];
			b[1] = in[i+2][1]+in[i+3][1];
			c[0] = in[i][0]-in[i+1][0];
			c[1] = in[i][1]-in[i+1][1];
			d[0] = in[i+2][0]-in[i+3][0];
			d[1] = in[i+2][1]-in[i+3][1];
			in[i][0] = a[0]+b[0];
			in[i][1] = a[1]+b[1];
			in[i+2][0] = a[0]-b[0];
			in[i+2][1] = a[1]-b[1];

			e1[0] = c[0]-d[1];
			e1[1] = c[1]+d[0];
			e2[0] = c[0]+d[1];
			e2[1] = c[1]-d[0];
			if(forward) {
				in[i+1][0] = e2[0];
				in[i+1][1] = e2[1];
				in[i+3][0] = e1[0];
				in[i+3][1] = e1[1];
			}
			else {
				in[i+1][0] = e1[0];
				in[i+1][1] = e1[1];
				in[i+3][0] = e2[0];
				in[i+3][1] = e2[1];
			}
		}

		//iterations from bottom to top
		int shift, m, km;
		float rootRe, rootIm, zRe, zIm;
		for(int i = 4; i<length; i <<= 1) {
			shift = i<<1;
			m = length/shift;
			for(int j = 0; j<length; j += shift) {
				for(int k = 0; k<i; k++) {
					km = k*m;
					rootRe = roots[km][0];
					rootIm = roots[km][imOff];
					zRe = in[i+j+k][0]*rootRe-in[i+j+k][1]*rootIm;
					zIm = in[i+j+k][0]*rootIm+in[i+j+k][1]*rootRe;

					in[i+j+k][0] = (in[j+k][0]-zRe)*scale;
					in[i+j+k][1] = (in[j+k][1]-zIm)*scale;
					in[j+k][0] = (in[j+k][0]+zRe)*scale;
					in[j+k][1] = (in[j+k][1]+zIm)*scale;
				}
			}
		}
	}
}
