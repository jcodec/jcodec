package net.sourceforge.jaad.aac.gain;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
//inverse polyphase quadrature filter
class IPQF implements GCConstants, PQFTables {

	private final float[] buf;
	private final float[][] tmp1, tmp2;

	IPQF() {
		buf = new float[BANDS];
		tmp1 = new float[BANDS/2][NPQFTAPS/BANDS];
		tmp2 = new float[BANDS/2][NPQFTAPS/BANDS];
	}

	void process(float[][] in, int frameLen, int maxBand, float[] out) {
		int i, j;
		for(i = 0; i<frameLen; i++) {
			out[i] = 0.0f;
		}

		for(i = 0; i<frameLen/BANDS; i++) {
			for(j = 0; j<BANDS; j++) {
				buf[j] = in[j][i];
			}
			performSynthesis(buf, out, i*BANDS);
		}
	}

	private void performSynthesis(float[] in, float[] out, int outOff) {
		final int kk = NPQFTAPS/(2*BANDS);
		int i, n, k;
		float acc;

		for(n = 0; n<BANDS/2; ++n) {
			for(k = 0; k<2*kk-1; ++k) {
				tmp1[n][k] = tmp1[n][k+1];
				tmp2[n][k] = tmp2[n][k+1];
			}
		}

		for(n = 0; n<BANDS/2; ++n) {
			acc = 0.0f;
			for(i = 0; i<BANDS; ++i) {
				acc += COEFS_Q0[n][i]*in[i];
			}
			tmp1[n][2*kk-1] = acc;

			acc = 0.0f;
			for(i = 0; i<BANDS; ++i) {
				acc += COEFS_Q1[n][i]*in[i];
			}
			tmp2[n][2*kk-1] = acc;
		}

		for(n = 0; n<BANDS/2; ++n) {
			acc = 0.0f;
			for(k = 0; k<kk; ++k) {
				acc += COEFS_T0[n][k]*tmp1[n][2*kk-1-2*k];
			}
			for(k = 0; k<kk; ++k) {
				acc += COEFS_T1[n][k]*tmp2[n][2*kk-2-2*k];
			}
			out[outOff+n] = acc;

			acc = 0.0f;
			for(k = 0; k<kk; ++k) {
				acc += COEFS_T0[BANDS-1-n][k]*tmp1[n][2*kk-1-2*k];
			}
			for(k = 0; k<kk; ++k) {
				acc -= COEFS_T1[BANDS-1-n][k]*tmp2[n][2*kk-2-2*k];
			}
			out[outOff+BANDS-1-n] = acc;
		}
	}
}
