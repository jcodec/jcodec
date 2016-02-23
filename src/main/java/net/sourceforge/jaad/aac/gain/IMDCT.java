package net.sourceforge.jaad.aac.gain;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.ICSInfo.WindowSequence;

import static java.lang.System.arraycopy;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
//inverse modified discrete cosine transform
class IMDCT implements GCConstants, IMDCTTables, Windows {

	private static final float[][] LONG_WINDOWS = {SINE_256, KBD_256};
	private static final float[][] SHORT_WINDOWS = {SINE_32, KBD_32};
	private final int frameLen, shortFrameLen, lbLong, lbShort, lbMid;

	IMDCT(int frameLen) {
		this.frameLen = frameLen;
		lbLong = frameLen/BANDS;
		shortFrameLen = frameLen/8;
		lbShort = shortFrameLen/BANDS;
		lbMid = (lbLong-lbShort)/2;
	}

	void process(float[] _in, float[] out, int winShape, int winShapePrev, WindowSequence winSeq) throws AACException {
		final float[] buf = new float[frameLen];

		int b, j, i;
		if(winSeq.equals(WindowSequence.EIGHT_SHORT_SEQUENCE)) {
			for(b = 0; b<BANDS; b++) {
				for(j = 0; j<8; j++) {
					for(i = 0; i<lbShort; i++) {
						if(b%2==0) buf[lbLong*b+lbShort*j+i] = _in[shortFrameLen*j+lbShort*b+i];
						else buf[lbLong*b+lbShort*j+i] = _in[shortFrameLen*j+lbShort*b+lbShort-1-i];
					}
				}
			}
		}
		else {
			for(b = 0; b<BANDS; b++) {
				for(i = 0; i<lbLong; i++) {
					if(b%2==0) buf[lbLong*b+i] = _in[lbLong*b+i];
					else buf[lbLong*b+i] = _in[lbLong*b+lbLong-1-i];
				}
			}
		}

		for(b = 0; b<BANDS; b++) {
			process2(buf, out, winSeq, winShape, winShapePrev, b);
		}
	}

	private void process2(float[] _in, float[] out, WindowSequence winSeq, int winShape, int winShapePrev, int band) throws AACException {
		final float[] bufIn = new float[lbLong];
		final float[] bufOut = new float[lbLong*2];
		final float[] window = new float[lbLong*2];
		final float[] window1 = new float[lbShort*2];
		final float[] window2 = new float[lbShort*2];

		//init windows
		int i;
		switch(winSeq) {
			case ONLY_LONG_SEQUENCE:
				for(i = 0; i<lbLong; i++) {
					window[i] = LONG_WINDOWS[winShapePrev][i];
					window[lbLong*2-1-i] = LONG_WINDOWS[winShape][i];
				}
				break;
			case EIGHT_SHORT_SEQUENCE:
				for(i = 0; i<lbShort; i++) {
					window1[i] = SHORT_WINDOWS[winShapePrev][i];
					window1[lbShort*2-1-i] = SHORT_WINDOWS[winShape][i];
					window2[i] = SHORT_WINDOWS[winShape][i];
					window2[lbShort*2-1-i] = SHORT_WINDOWS[winShape][i];
				}
				break;
			case LONG_START_SEQUENCE:
				for(i = 0; i<lbLong; i++) {
					window[i] = LONG_WINDOWS[winShapePrev][i];
				}
				for(i = 0; i<lbMid; i++) {
					window[i+lbLong] = 1.0f;
				}

				for(i = 0; i<lbShort; i++) {
					window[i+lbMid+lbLong] = SHORT_WINDOWS[winShape][lbShort-1-i];
				}
				for(i = 0; i<lbMid; i++) {
					window[i+lbMid+lbLong+lbShort] = 0.0f;
				}
				break;
			case LONG_STOP_SEQUENCE:
				for(i = 0; i<lbMid; i++) {
					window[i] = 0.0f;
				}
				for(i = 0; i<lbShort; i++) {
					window[i+lbMid] = SHORT_WINDOWS[winShapePrev][i];
				}
				for(i = 0; i<lbMid; i++) {
					window[i+lbMid+lbShort] = 1.0f;
				}
				for(i = 0; i<lbLong; i++) {
					window[i+lbMid+lbShort+lbMid] = LONG_WINDOWS[winShape][lbLong-1-i];
				}
				break;
		}

		int j;
		if(winSeq.equals(WindowSequence.EIGHT_SHORT_SEQUENCE)) {
			int k;
			for(j = 0; j<8; j++) {
				for(k = 0; k<lbShort; k++) {
					bufIn[k] = _in[band*lbLong+j*lbShort+k];
				}
				if(j==0) arraycopy(window1, 0, window, 0, lbShort * 2);
				else arraycopy(window2, 0, window, 0, lbShort * 2);
				imdct(bufIn, bufOut, window, lbShort);
				for(k = 0; k<lbShort*2; k++) {
					out[band*lbLong*2+j*lbShort*2+k] = bufOut[k]/32.0f;
				}
			}
		}
		else {
			for(j = 0; j<lbLong; j++) {
				bufIn[j] = _in[band*lbLong+j];
			}
			imdct(bufIn, bufOut, window, lbLong);
			for(j = 0; j<lbLong*2; j++) {
				out[band*lbLong*2+j] = bufOut[j]/256.0f;
			}
		}
	}

	private void imdct(float[] _in, float[] out, float[] window, int n) throws AACException {
		final int n2 = n/2;
		float[][] table, table2;
		if(n==256) {
			table = IMDCT_TABLE_256;
			table2 = IMDCT_POST_TABLE_256;
		}
		else if(n==32) {
			table = IMDCT_TABLE_32;
			table2 = IMDCT_POST_TABLE_32;
		}
		else throw new AACException("gain control: unexpected IMDCT length");

		final float[] tmp = new float[n];
		int i;
		for(i = 0; i<n2; ++i) {
			tmp[i] = _in[2*i];
		}
		for(i = n2; i<n; ++i) {
			tmp[i] = -_in[2*n-1-2*i];
		}

		//pre-twiddle
		final float[][] buf = new float[n2][2];
		for(i = 0; i<n2; i++) {
			buf[i][0] = (table[i][0]*tmp[2*i])-(table[i][1]*tmp[2*i+1]);
			buf[i][1] = (table[i][0]*tmp[2*i+1])+(table[i][1]*tmp[2*i]);
		}

		//fft
		FFT.process(buf, n2);

		//post-twiddle and reordering
		for(i = 0; i<n2; i++) {
			tmp[i] = table2[i][0]*buf[i][0]+table2[i][1]*buf[n2-1-i][0]
					+table2[i][2]*buf[i][1]+table2[i][3]*buf[n2-1-i][1];
			tmp[n-1-i] = table2[i][2]*buf[i][0]-table2[i][3]*buf[n2-1-i][0]
					-table2[i][0]*buf[i][1]+table2[i][1]*buf[n2-1-i][1];
		}

		//copy to output and apply window
		arraycopy(tmp, n2, out, 0, n2);
		for(i = n2; i<n*3/2; ++i) {
			out[i] = -tmp[n*3/2-1-i];
		}
		for(i = n*3/2; i<n*2; ++i) {
			out[i] = -tmp[i-n*3/2];
		}

		for(i = 0; i<n; i++) {
			out[i] *= window[i];
		}
	}
}
