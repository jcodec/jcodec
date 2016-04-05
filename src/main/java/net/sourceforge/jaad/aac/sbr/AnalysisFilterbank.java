package net.sourceforge.jaad.aac.sbr;

import js.util.Arrays;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
class AnalysisFilterbank implements FilterbankTable {

	private float[] x; //x is implemented as double ringbuffer
	private int x_index; //ringbuffer index
	private int channels;

	AnalysisFilterbank(int channels) {
		this.channels = channels;
		x = new float[2*channels*10];
		x_index = 0;
	}

	public void reset() {
		Arrays.fill(x, 0);
	}

	void sbr_qmf_analysis_32(SBR sbr, float[] input,
		float[][][] X, int offset, int kx) {
		float[] u = new float[64];
		float[] in_real = new float[32], in_imag = new float[32];
		float[] out_real = new float[32], out_imag = new float[32];
		int _in = 0;
		int l;

		/* qmf subsample l */
		for(l = 0; l<sbr.numTimeSlotsRate; l++) {
			int n;

			/* shift input buffer x */
			/* input buffer is not shifted anymore, x is implemented as double ringbuffer */
			//memmove(qmfa.x + 32, qmfa.x, (320-32)*sizeof(real_t));

			/* add new samples to input buffer x */
			for(n = 32-1; n>=0; n--) {
				this.x[this.x_index+n] = this.x[this.x_index+n+320] = input[_in++];
			}

			/* window and summation to create array u */
			for(n = 0; n<64; n++) {
				u[n] = (this.x[this.x_index+n]*qmf_c[2*n])
					+(this.x[this.x_index+n+64]*qmf_c[2*(n+64)])
					+(this.x[this.x_index+n+128]*qmf_c[2*(n+128)])
					+(this.x[this.x_index+n+192]*qmf_c[2*(n+192)])
					+(this.x[this.x_index+n+256]*qmf_c[2*(n+256)]);
			}

			/* update ringbuffer index */
			this.x_index -= 32;
			if(this.x_index<0)
				this.x_index = (320-32);

			/* calculate 32 subband samples by introducing X */
			// Reordering of data moved from DCT_IV to here
			in_imag[31] = u[1];
			in_real[0] = u[0];
			for(n = 1; n<31; n++) {
				in_imag[31-n] = u[n+1];
				in_real[n] = -u[64-n];
			}
			in_imag[0] = u[32];
			in_real[31] = -u[33];

			// dct4_kernel is DCT_IV without reordering which is done before and after FFT
			DCT.dct4_kernel(in_real, in_imag, out_real, out_imag);

			// Reordering of data moved from DCT_IV to here
			for(n = 0; n<16; n++) {
				if(2*n+1<kx) {
					X[l+offset][2*n][0] = 2.0f*out_real[n];
					X[l+offset][2*n][1] = 2.0f*out_imag[n];
					X[l+offset][2*n+1][0] = -2.0f*out_imag[31-n];
					X[l+offset][2*n+1][1] = -2.0f*out_real[31-n];
				}
				else {
					if(2*n<kx) {
						X[l+offset][2*n][0] = 2.0f*out_real[n];
						X[l+offset][2*n][1] = 2.0f*out_imag[n];
					}
					else {
						X[l+offset][2*n][0] = 0;
						X[l+offset][2*n][1] = 0;
					}
					X[l+offset][2*n+1][0] = 0;
					X[l+offset][2*n+1][1] = 0;
				}
			}
		}
	}
}
