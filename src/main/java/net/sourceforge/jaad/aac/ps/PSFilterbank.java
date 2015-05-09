package net.sourceforge.jaad.aac.ps;


class PSFilterbank implements PSTables {

	private int frame_len;
	private int[] resolution20 = new int[3];
	private int[] resolution34 = new int[5];

	private float[][] work;
	private float[][][] buffer;
	private float[][][] temp;

	PSFilterbank(int numTimeSlotsRate) {
		int i;

		this.resolution34[0] = 12;
		this.resolution34[1] = 8;
		this.resolution34[2] = 4;
		this.resolution34[3] = 4;
		this.resolution34[4] = 4;

		this.resolution20[0] = 8;
		this.resolution20[1] = 2;
		this.resolution20[2] = 2;

		this.frame_len = numTimeSlotsRate;

		this.work = new float[(this.frame_len+12)][2];

		this.buffer = new float[5][2][2];

		temp = new float[frame_len][12][2];
	}

	void hybrid_analysis(float[][][] X, float[][][] X_hybrid, boolean use34, int numTimeSlotsRate) {
		int k, n, band;
		int offset = 0;
		int qmf_bands = (use34) ? 5 : 3;
		int[] resolution = (use34) ? this.resolution34 : this.resolution20;

		for(band = 0; band<qmf_bands; band++) {
			/* build working buffer */
			//memcpy(this.work, this.buffer[band], 12*sizeof(qmf_t));
			for(int i = 0; i<12; i++) {
				work[i][0] = buffer[band][i][0];
				work[i][1] = buffer[band][i][1];
			}

			/* add new samples */
			for(n = 0; n<this.frame_len; n++) {
				this.work[12+n][0] = X[n+6 /*delay*/][band][0];
				this.work[12+n][0] = X[n+6 /*delay*/][band][0];
			}

			/* store samples */
			//memcpy(this.buffer[band], this.work+this.frame_len, 12*sizeof(qmf_t));
			for(int i = 0; i<12; i++) {
				buffer[band][i][0] = work[frame_len+i][0];
				buffer[band][i][1] = work[frame_len+i][1];
			}

			switch(resolution[band]) {
				case 2:
					/* Type B real filter, Q[p] = 2 */
					channel_filter2(this.frame_len, p2_13_20, this.work, this.temp);
					break;
				case 4:
					/* Type A complex filter, Q[p] = 4 */
					channel_filter4(this.frame_len, p4_13_34, this.work, this.temp);
					break;
				case 8:
					/* Type A complex filter, Q[p] = 8 */
					channel_filter8(this.frame_len, (use34) ? p8_13_34 : p8_13_20,
						this.work, this.temp);
					break;
				case 12:
					/* Type A complex filter, Q[p] = 12 */
					channel_filter12(this.frame_len, p12_13_34, this.work, this.temp);
					break;
			}

			for(n = 0; n<this.frame_len; n++) {
				for(k = 0; k<resolution[band]; k++) {
					X_hybrid[n][offset+k][0] = this.temp[n][k][0];
					X_hybrid[n][offset+k][1] = this.temp[n][k][1];
				}
			}
			offset += resolution[band];
		}

		/* group hybrid channels */
		if(!use34) {
			for(n = 0; n<numTimeSlotsRate; n++) {
				X_hybrid[n][3][0] += X_hybrid[n][4][0];
				X_hybrid[n][3][1] += X_hybrid[n][4][1];
				X_hybrid[n][4][0] = 0;
				X_hybrid[n][4][1] = 0;

				X_hybrid[n][2][0] += X_hybrid[n][5][0];
				X_hybrid[n][2][1] += X_hybrid[n][5][1];
				X_hybrid[n][5][0] = 0;
				X_hybrid[n][5][1] = 0;
			}
		}
	}

	/* real filter, size 2 */
	static void channel_filter2(int frame_len, float[] filter,
		float[][] buffer, float[][][] X_hybrid) {
		int i;

		for(i = 0; i<frame_len; i++) {
			float r0 = (filter[0]*(buffer[0+i][0]+buffer[12+i][0]));
			float r1 = (filter[1]*(buffer[1+i][0]+buffer[11+i][0]));
			float r2 = (filter[2]*(buffer[2+i][0]+buffer[10+i][0]));
			float r3 = (filter[3]*(buffer[3+i][0]+buffer[9+i][0]));
			float r4 = (filter[4]*(buffer[4+i][0]+buffer[8+i][0]));
			float r5 = (filter[5]*(buffer[5+i][0]+buffer[7+i][0]));
			float r6 = (filter[6]*buffer[6+i][0]);
			float i0 = (filter[0]*(buffer[0+i][1]+buffer[12+i][1]));
			float i1 = (filter[1]*(buffer[1+i][1]+buffer[11+i][1]));
			float i2 = (filter[2]*(buffer[2+i][1]+buffer[10+i][1]));
			float i3 = (filter[3]*(buffer[3+i][1]+buffer[9+i][1]));
			float i4 = (filter[4]*(buffer[4+i][1]+buffer[8+i][1]));
			float i5 = (filter[5]*(buffer[5+i][1]+buffer[7+i][1]));
			float i6 = (filter[6]*buffer[6+i][1]);

			/* q = 0 */
			X_hybrid[i][0][0] = r0+r1+r2+r3+r4+r5+r6;
			X_hybrid[i][0][1] = i0+i1+i2+i3+i4+i5+i6;

			/* q = 1 */
			X_hybrid[i][1][0] = r0-r1+r2-r3+r4-r5+r6;
			X_hybrid[i][1][1] = i0-i1+i2-i3+i4-i5+i6;
		}
	}

	/* complex filter, size 4 */
	static void channel_filter4(int frame_len, float[] filter,
		float[][] buffer, float[][][] X_hybrid) {
		int i;
		float[] input_re1 = new float[2], input_re2 = new float[2];
		float[] input_im1 = new float[2], input_im2 = new float[2];

		for(i = 0; i<frame_len; i++) {
			input_re1[0] = -(filter[2]*(buffer[i+2][0]+buffer[i+10][0]))
				+(filter[6]*buffer[i+6][0]);
			input_re1[1] = (-0.70710678118655f
				*((filter[1]*(buffer[i+1][0]+buffer[i+11][0]))
				+(filter[3]*(buffer[i+3][0]+buffer[i+9][0]))
				-(filter[5]*(buffer[i+5][0]+buffer[i+7][0]))));

			input_im1[0] = (filter[0]*(buffer[i+0][1]-buffer[i+12][1]))
				-(filter[4]*(buffer[i+4][1]-buffer[i+8][1]));
			input_im1[1] = (0.70710678118655f
				*((filter[1]*(buffer[i+1][1]-buffer[i+11][1]))
				-(filter[3]*(buffer[i+3][1]-buffer[i+9][1]))
				-(filter[5]*(buffer[i+5][1]-buffer[i+7][1]))));

			input_re2[0] = (filter[0]*(buffer[i+0][0]-buffer[i+12][0]))
				-(filter[4]*(buffer[i+4][0]-buffer[i+8][0]));
			input_re2[1] = (0.70710678118655f
				*((filter[1]*(buffer[i+1][0]-buffer[i+11][0]))
				-(filter[3]*(buffer[i+3][0]-buffer[i+9][0]))
				-(filter[5]*(buffer[i+5][0]-buffer[i+7][0]))));

			input_im2[0] = -(filter[2]*(buffer[i+2][1]+buffer[i+10][1]))
				+(filter[6]*buffer[i+6][1]);
			input_im2[1] = (-0.70710678118655f
				*((filter[1]*(buffer[i+1][1]+buffer[i+11][1]))
				+(filter[3]*(buffer[i+3][1]+buffer[i+9][1]))
				-(filter[5]*(buffer[i+5][1]+buffer[i+7][1]))));

			/* q == 0 */
			X_hybrid[i][0][0] = input_re1[0]+input_re1[1]+input_im1[0]+input_im1[1];
			X_hybrid[i][0][1] = -input_re2[0]-input_re2[1]+input_im2[0]+input_im2[1];

			/* q == 1 */
			X_hybrid[i][1][0] = input_re1[0]-input_re1[1]-input_im1[0]+input_im1[1];
			X_hybrid[i][1][1] = input_re2[0]-input_re2[1]+input_im2[0]-input_im2[1];

			/* q == 2 */
			X_hybrid[i][2][0] = input_re1[0]-input_re1[1]+input_im1[0]-input_im1[1];
			X_hybrid[i][2][1] = -input_re2[0]+input_re2[1]+input_im2[0]-input_im2[1];

			/* q == 3 */
			X_hybrid[i][3][0] = input_re1[0]+input_re1[1]-input_im1[0]-input_im1[1];
			X_hybrid[i][3][1] = input_re2[0]+input_re2[1]+input_im2[0]+input_im2[1];
		}
	}

	static void DCT3_4_unscaled(float[] y, float[] x) {
		float f0, f1, f2, f3, f4, f5, f6, f7, f8;

		f0 = (x[2]*0.7071067811865476f);
		f1 = x[0]-f0;
		f2 = x[0]+f0;
		f3 = x[1]+x[3];
		f4 = (x[1]*1.3065629648763766f);
		f5 = (f3*(-0.9238795325112866f));
		f6 = (x[3]*(-0.5411961001461967f));
		f7 = f4+f5;
		f8 = f6-f5;
		y[3] = f2-f8;
		y[0] = f2+f8;
		y[2] = f1-f7;
		y[1] = f1+f7;
	}

	/* complex filter, size 8 */
	void channel_filter8(int frame_len, float[] filter,
		float[][] buffer, float[][][] X_hybrid) {
		int i, n;
		float[] input_re1 = new float[4], input_re2 = new float[4];
		float[] input_im1 = new float[4], input_im2 = new float[4];
		float[] x = new float[4];

		for(i = 0; i<frame_len; i++) {
			input_re1[0] = (filter[6]*buffer[6+i][0]);
			input_re1[1] = (filter[5]*(buffer[5+i][0]+buffer[7+i][0]));
			input_re1[2] = -(filter[0]*(buffer[0+i][0]+buffer[12+i][0]))+(filter[4]*(buffer[4+i][0]+buffer[8+i][0]));
			input_re1[3] = -(filter[1]*(buffer[1+i][0]+buffer[11+i][0]))+(filter[3]*(buffer[3+i][0]+buffer[9+i][0]));

			input_im1[0] = (filter[5]*(buffer[7+i][1]-buffer[5+i][1]));
			input_im1[1] = (filter[0]*(buffer[12+i][1]-buffer[0+i][1]))+(filter[4]*(buffer[8+i][1]-buffer[4+i][1]));
			input_im1[2] = (filter[1]*(buffer[11+i][1]-buffer[1+i][1]))+(filter[3]*(buffer[9+i][1]-buffer[3+i][1]));
			input_im1[3] = (filter[2]*(buffer[10+i][1]-buffer[2+i][1]));

			for(n = 0; n<4; n++) {
				x[n] = input_re1[n]-input_im1[3-n];
			}
			DCT3_4_unscaled(x, x);
			X_hybrid[i][7][0] = x[0];
			X_hybrid[i][5][0] = x[2];
			X_hybrid[i][3][0] = x[3];
			X_hybrid[i][1][0] = x[1];

			for(n = 0; n<4; n++) {
				x[n] = input_re1[n]+input_im1[3-n];
			}
			DCT3_4_unscaled(x, x);
			X_hybrid[i][6][0] = x[1];
			X_hybrid[i][4][0] = x[3];
			X_hybrid[i][2][0] = x[2];
			X_hybrid[i][0][0] = x[0];

			input_im2[0] = (filter[6]*buffer[6+i][1]);
			input_im2[1] = (filter[5]*(buffer[5+i][1]+buffer[7+i][1]));
			input_im2[2] = -(filter[0]*(buffer[0+i][1]+buffer[12+i][1]))+(filter[4]*(buffer[4+i][1]+buffer[8+i][1]));
			input_im2[3] = -(filter[1]*(buffer[1+i][1]+buffer[11+i][1]))+(filter[3]*(buffer[3+i][1]+buffer[9+i][1]));

			input_re2[0] = (filter[5]*(buffer[7+i][0]-buffer[5+i][0]));
			input_re2[1] = (filter[0]*(buffer[12+i][0]-buffer[0+i][0]))+(filter[4]*(buffer[8+i][0]-buffer[4+i][0]));
			input_re2[2] = (filter[1]*(buffer[11+i][0]-buffer[1+i][0]))+(filter[3]*(buffer[9+i][0]-buffer[3+i][0]));
			input_re2[3] = (filter[2]*(buffer[10+i][0]-buffer[2+i][0]));

			for(n = 0; n<4; n++) {
				x[n] = input_im2[n]+input_re2[3-n];
			}
			DCT3_4_unscaled(x, x);
			X_hybrid[i][7][1] = x[0];
			X_hybrid[i][5][1] = x[2];
			X_hybrid[i][3][1] = x[3];
			X_hybrid[i][1][1] = x[1];

			for(n = 0; n<4; n++) {
				x[n] = input_im2[n]-input_re2[3-n];
			}
			DCT3_4_unscaled(x, x);
			X_hybrid[i][6][1] = x[1];
			X_hybrid[i][4][1] = x[3];
			X_hybrid[i][2][1] = x[2];
			X_hybrid[i][0][1] = x[0];
		}
	}

	void DCT3_6_unscaled(float[] y, float[] x) {
		float f0, f1, f2, f3, f4, f5, f6, f7;

		f0 = (x[3]*0.70710678118655f);
		f1 = x[0]+f0;
		f2 = x[0]-f0;
		f3 = ((x[1]-x[5])*0.70710678118655f);
		f4 = (x[2]*0.86602540378444f)+(x[4]*0.5f);
		f5 = f4-x[4];
		f6 = (x[1]*0.96592582628907f)+(x[5]*0.25881904510252f);
		f7 = f6-f3;
		y[0] = f1+f6+f4;
		y[1] = f2+f3-x[4];
		y[2] = f7+f2-f5;
		y[3] = f1-f7-f5;
		y[4] = f1-f3-x[4];
		y[5] = f2-f6+f4;
	}

	/* complex filter, size 12 */
	void channel_filter12(int frame_len, float[] filter,
		float[][] buffer, float[][][] X_hybrid) {
		int i, n;
		float[] input_re1 = new float[6], input_re2 = new float[6];
		float[] input_im1 = new float[6], input_im2 = new float[6];
		float[] out_re1 = new float[6], out_re2 = new float[6];
		float[] out_im1 = new float[6], out_im2 = new float[6];

		for(i = 0; i<frame_len; i++) {
			for(n = 0; n<6; n++) {
				if(n==0) {
					input_re1[0] = (buffer[6+i][0]*filter[6]);
					input_re2[0] = (buffer[6+i][1]*filter[6]);
				}
				else {
					input_re1[6-n] = ((buffer[n+i][0]+buffer[12-n+i][0])*filter[n]);
					input_re2[6-n] = ((buffer[n+i][1]+buffer[12-n+i][1])*filter[n]);
				}
				input_im2[n] = ((buffer[n+i][0]-buffer[12-n+i][0])*filter[n]);
				input_im1[n] = ((buffer[n+i][1]-buffer[12-n+i][1])*filter[n]);
			}

			DCT3_6_unscaled(out_re1, input_re1);
			DCT3_6_unscaled(out_re2, input_re2);

			DCT3_6_unscaled(out_im1, input_im1);
			DCT3_6_unscaled(out_im2, input_im2);

			for(n = 0; n<6; n += 2) {
				X_hybrid[i][n][0] = out_re1[n]-out_im1[n];
				X_hybrid[i][n][1] = out_re2[n]+out_im2[n];
				X_hybrid[i][n+1][0] = out_re1[n+1]+out_im1[n+1];
				X_hybrid[i][n+1][1] = out_re2[n+1]-out_im2[n+1];

				X_hybrid[i][10-n][0] = out_re1[n+1]-out_im1[n+1];
				X_hybrid[i][10-n][1] = out_re2[n+1]+out_im2[n+1];
				X_hybrid[i][11-n][0] = out_re1[n]+out_im1[n];
				X_hybrid[i][11-n][1] = out_re2[n]-out_im2[n];
			}
		}
	}

	void hybrid_synthesis(float[][][] X, float[][][] X_hybrid,
		boolean use34, int numTimeSlotsRate) {
		int k, n, band;
		int offset = 0;
		int qmf_bands = (use34) ? 5 : 3;
		int[] resolution = (use34) ? this.resolution34 : this.resolution20;

		for(band = 0; band<qmf_bands; band++) {
			for(n = 0; n<this.frame_len; n++) {
				X[n][band][0] = 0;
				X[n][band][1] = 0;

				for(k = 0; k<resolution[band]; k++) {
					X[n][band][0] += X_hybrid[n][offset+k][0];
					X[n][band][1] += X_hybrid[n][offset+k][1];
				}
			}
			offset += resolution[band];
		}
	}
}
