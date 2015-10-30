package net.sourceforge.jaad.aac.sbr;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license. 
 *
 * @author in-somnia
 */
class HFGeneration {

	private static final int[] goalSbTab = {21, 23, 32, 43, 46, 64, 85, 93, 128, 0, 0, 0};

	private static class acorr_coef {

		float[] r01 = new float[2];
		float[] r02 = new float[2];
		float[] r11 = new float[2];
		float[] r12 = new float[2];
		float[] r22 = new float[2];
		float det;
	}

	public static void hf_generation(SBR sbr, float[][][] Xlow,
		float[][][] Xhigh, int ch) {
		int l, i, x;
		float[][] alpha_0 = new float[64][2], alpha_1 = new float[64][2];

		int offset = sbr.tHFAdj;
		int first = sbr.t_E[ch][0];
		int last = sbr.t_E[ch][sbr.L_E[ch]];

		calc_chirp_factors(sbr, ch);

		if((ch==0)&&(sbr.Reset))
			patch_construction(sbr);

		/* calculate the prediction coefficients */

		/* actual HF generation */
		for(i = 0; i<sbr.noPatches; i++) {
			for(x = 0; x<sbr.patchNoSubbands[i]; x++) {
				float a0_r, a0_i, a1_r, a1_i;
				float bw, bw2;
				int q, p, k, g;

				/* find the low and high band for patching */
				k = sbr.kx+x;
				for(q = 0; q<i; q++) {
					k += sbr.patchNoSubbands[q];
				}
				p = sbr.patchStartSubband[i]+x;

				g = sbr.table_map_k_to_g[k];

				bw = sbr.bwArray[ch][g];
				bw2 = bw*bw;

				/* do the patching */
				/* with or without filtering */
				if(bw2>0) {
					float temp1_r, temp2_r, temp3_r;
					float temp1_i, temp2_i, temp3_i;
					calc_prediction_coef(sbr, Xlow, alpha_0, alpha_1, p);

					a0_r = (alpha_0[p][0]*bw);
					a1_r = (alpha_1[p][0]*bw2);
					a0_i = (alpha_0[p][1]*bw);
					a1_i = (alpha_1[p][1]*bw2);

					temp2_r = (Xlow[first-2+offset][p][0]);
					temp3_r = (Xlow[first-1+offset][p][0]);
					temp2_i = (Xlow[first-2+offset][p][1]);
					temp3_i = (Xlow[first-1+offset][p][1]);
					for(l = first; l<last; l++) {
						temp1_r = temp2_r;
						temp2_r = temp3_r;
						temp3_r = (Xlow[l+offset][p][0]);
						temp1_i = temp2_i;
						temp2_i = temp3_i;
						temp3_i = (Xlow[l+offset][p][1]);

						Xhigh[l+offset][k][0]
							= temp3_r
							+((a0_r*temp2_r)
							-(a0_i*temp2_i)
							+(a1_r*temp1_r)
							-(a1_i*temp1_i));
						Xhigh[l+offset][k][1]
							= temp3_i
							+((a0_i*temp2_r)
							+(a0_r*temp2_i)
							+(a1_i*temp1_r)
							+(a1_r*temp1_i));
					}
				}
				else {
					for(l = first; l<last; l++) {
						Xhigh[l+offset][k][0] = Xlow[l+offset][p][0];
						Xhigh[l+offset][k][1] = Xlow[l+offset][p][1];
					}
				}
			}
		}

		if(sbr.Reset) {
			FBT.limiter_frequency_table(sbr);
		}
	}

	private static void auto_correlation(SBR sbr, acorr_coef ac, float[][][] buffer,
		int bd, int len) {
		float r01r = 0, r01i = 0, r02r = 0, r02i = 0, r11r = 0;
		float temp1_r, temp1_i, temp2_r, temp2_i, temp3_r, temp3_i, temp4_r, temp4_i, temp5_r, temp5_i;
		float rel = 1.0f/(1+1e-6f);
		int j;
		int offset = sbr.tHFAdj;

		temp2_r = buffer[offset-2][bd][0];
		temp2_i = buffer[offset-2][bd][1];
		temp3_r = buffer[offset-1][bd][0];
		temp3_i = buffer[offset-1][bd][1];
		// Save these because they are needed after loop
		temp4_r = temp2_r;
		temp4_i = temp2_i;
		temp5_r = temp3_r;
		temp5_i = temp3_i;

		for(j = offset; j<len+offset; j++) {
			temp1_r = temp2_r; // temp1_r = QMF_RE(buffer[j-2][bd];
			temp1_i = temp2_i; // temp1_i = QMF_IM(buffer[j-2][bd];
			temp2_r = temp3_r; // temp2_r = QMF_RE(buffer[j-1][bd];
			temp2_i = temp3_i; // temp2_i = QMF_IM(buffer[j-1][bd];
			temp3_r = buffer[j][bd][0];
			temp3_i = buffer[j][bd][1];
			r01r += temp3_r*temp2_r+temp3_i*temp2_i;
			r01i += temp3_i*temp2_r-temp3_r*temp2_i;
			r02r += temp3_r*temp1_r+temp3_i*temp1_i;
			r02i += temp3_i*temp1_r-temp3_r*temp1_i;
			r11r += temp2_r*temp2_r+temp2_i*temp2_i;
		}

		// These are actual values in temporary variable at this point
		// temp1_r = QMF_RE(buffer[len+offset-1-2][bd];
		// temp1_i = QMF_IM(buffer[len+offset-1-2][bd];
		// temp2_r = QMF_RE(buffer[len+offset-1-1][bd];
		// temp2_i = QMF_IM(buffer[len+offset-1-1][bd];
		// temp3_r = QMF_RE(buffer[len+offset-1][bd]);
		// temp3_i = QMF_IM(buffer[len+offset-1][bd]);
		// temp4_r = QMF_RE(buffer[offset-2][bd]);
		// temp4_i = QMF_IM(buffer[offset-2][bd]);
		// temp5_r = QMF_RE(buffer[offset-1][bd]);
		// temp5_i = QMF_IM(buffer[offset-1][bd]);
		ac.r12[0] = r01r
			-(temp3_r*temp2_r+temp3_i*temp2_i)
			+(temp5_r*temp4_r+temp5_i*temp4_i);
		ac.r12[1] = r01i
			-(temp3_i*temp2_r-temp3_r*temp2_i)
			+(temp5_i*temp4_r-temp5_r*temp4_i);
		ac.r22[0] = r11r
			-(temp2_r*temp2_r+temp2_i*temp2_i)
			+(temp4_r*temp4_r+temp4_i*temp4_i);

		ac.r01[0] = r01r;
		ac.r01[1] = r01i;
		ac.r02[0] = r02r;
		ac.r02[1] = r02i;
		ac.r11[0] = r11r;

		ac.det = (ac.r11[0]*ac.r22[0])-(rel*((ac.r12[0]*ac.r12[0])+(ac.r12[1]*ac.r12[1])));
	}

	/* calculate linear prediction coefficients using the covariance method */
	private static void calc_prediction_coef(SBR sbr, float[][][] Xlow,
		float[][] alpha_0, float[][] alpha_1, int k) {
		float tmp;
		acorr_coef ac = new acorr_coef();

		auto_correlation(sbr, ac, Xlow, k, sbr.numTimeSlotsRate+6);

		if(ac.det==0) {
			alpha_1[k][0] = 0;
			alpha_1[k][1] = 0;
		}
		else {
			tmp = 1.0f/ac.det;
			alpha_1[k][0] = ((ac.r01[0]*ac.r12[0])-(ac.r01[1]*ac.r12[1])-(ac.r02[0]*ac.r11[0]))*tmp;
			alpha_1[k][1] = ((ac.r01[1]*ac.r12[0])+(ac.r01[0]*ac.r12[1])-(ac.r02[1]*ac.r11[0]))*tmp;
		}

		if(ac.r11[0]==0) {
			alpha_0[k][0] = 0;
			alpha_0[k][1] = 0;
		}
		else {
			tmp = 1.0f/ac.r11[0];
			alpha_0[k][0] = -(ac.r01[0]+(alpha_1[k][0]*ac.r12[0])+(alpha_1[k][1]*ac.r12[1]))*tmp;
			alpha_0[k][1] = -(ac.r01[1]+(alpha_1[k][1]*ac.r12[0])-(alpha_1[k][0]*ac.r12[1]))*tmp;
		}

		if(((alpha_0[k][0]*alpha_0[k][0])+(alpha_0[k][1]*alpha_0[k][1])>=16.0f)
			||((alpha_1[k][0]*alpha_1[k][0])+(alpha_1[k][1]*alpha_1[k][1])>=16.0f)) {
			alpha_0[k][0] = 0;
			alpha_0[k][1] = 0;
			alpha_1[k][0] = 0;
			alpha_1[k][1] = 0;
		}
	}

	/* FIXED POINT: bwArray = COEF */
	private static float mapNewBw(int invf_mode, int invf_mode_prev) {
		switch(invf_mode) {
			case 1: /* LOW */

				if(invf_mode_prev==0) /* NONE */
					return 0.6f;
				else
					return 0.75f;

			case 2: /* MID */

				return 0.9f;

			case 3: /* HIGH */

				return 0.98f;

			default: /* NONE */

				if(invf_mode_prev==1) /* LOW */
					return 0.6f;
				else
					return 0.0f;
		}
	}

	/* FIXED POINT: bwArray = COEF */
	private static void calc_chirp_factors(SBR sbr, int ch) {
		int i;

		for(i = 0; i<sbr.N_Q; i++) {
			sbr.bwArray[ch][i] = mapNewBw(sbr.bs_invf_mode[ch][i], sbr.bs_invf_mode_prev[ch][i]);

			if(sbr.bwArray[ch][i]<sbr.bwArray_prev[ch][i])
				sbr.bwArray[ch][i] = (sbr.bwArray[ch][i]*0.75f)+(sbr.bwArray_prev[ch][i]*0.25f);
			else
				sbr.bwArray[ch][i] = (sbr.bwArray[ch][i]*0.90625f)+(sbr.bwArray_prev[ch][i]*0.09375f);

			if(sbr.bwArray[ch][i]<0.015625f)
				sbr.bwArray[ch][i] = 0.0f;

			if(sbr.bwArray[ch][i]>=0.99609375f)
				sbr.bwArray[ch][i] = 0.99609375f;

			sbr.bwArray_prev[ch][i] = sbr.bwArray[ch][i];
			sbr.bs_invf_mode_prev[ch][i] = sbr.bs_invf_mode[ch][i];
		}
	}

	private static void patch_construction(SBR sbr) {
		int i, k;
		int odd, sb;
		int msb = sbr.k0;
		int usb = sbr.kx;
		/* (uint8_t)(2.048e6/sbr.sample_rate + 0.5); */
		int goalSb = goalSbTab[sbr.sample_rate.getIndex()];

		sbr.noPatches = 0;

		if(goalSb<(sbr.kx+sbr.M)) {
			for(i = 0, k = 0; sbr.f_master[i]<goalSb; i++) {
				k = i+1;
			}
		}
		else {
			k = sbr.N_master;
		}

		if(sbr.N_master==0) {
			sbr.noPatches = 0;
			sbr.patchNoSubbands[0] = 0;
			sbr.patchStartSubband[0] = 0;

			return;
		}

		do {
			int j = k+1;

			do {
				j--;

				sb = sbr.f_master[j];
				odd = (sb-2+sbr.k0)%2;
			}
			while(sb>(sbr.k0-1+msb-odd));

			sbr.patchNoSubbands[sbr.noPatches] = Math.max(sb-usb, 0);
			sbr.patchStartSubband[sbr.noPatches] = sbr.k0-odd
				-sbr.patchNoSubbands[sbr.noPatches];

			if(sbr.patchNoSubbands[sbr.noPatches]>0) {
				usb = sb;
				msb = sb;
				sbr.noPatches++;
			}
			else {
				msb = sbr.kx;
			}

			if(sbr.f_master[k]-sb<3)
				k = sbr.N_master;
		}
		while(sb!=(sbr.kx+sbr.M));

		if((sbr.patchNoSubbands[sbr.noPatches-1]<3)&&(sbr.noPatches>1)) {
			sbr.noPatches--;
		}

		sbr.noPatches = Math.min(sbr.noPatches, 5);
	}
}
