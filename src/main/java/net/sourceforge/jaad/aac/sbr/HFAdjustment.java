package net.sourceforge.jaad.aac.sbr;

import static java.lang.System.arraycopy;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
class HFAdjustment implements SBRConstants, NoiseTable {

	private static final float[] h_smooth = {
		0.03183050093751f, 0.11516383427084f,
		0.21816949906249f, 0.30150283239582f,
		0.33333333333333f
	};
	private static final int[] phi_re = {1, 0, -1, 0};
	private static final int[] phi_im = {0, 1, 0, -1};
	private static final float[] limGain = {0.5f, 1.0f, 2.0f, 1e10f};
	private static final float EPS = 1e-12f;
	private float[][] G_lim_boost = new float[MAX_L_E][MAX_M];
	private float[][] Q_M_lim_boost = new float[MAX_L_E][MAX_M];
	private float[][] S_M_boost = new float[MAX_L_E][MAX_M];

	public static int hf_adjustment(SBR sbr, float[][][] Xsbr, int ch) {
		HFAdjustment adj = new HFAdjustment();
		int ret = 0;

		if(sbr.bs_frame_class[ch]==FIXFIX) {
			sbr.l_A[ch] = -1;
		}
		else if(sbr.bs_frame_class[ch]==VARFIX) {
			if(sbr.bs_pointer[ch]>1)
				sbr.l_A[ch] = sbr.bs_pointer[ch]-1;
			else
				sbr.l_A[ch] = -1;
		}
		else {
			if(sbr.bs_pointer[ch]==0)
				sbr.l_A[ch] = -1;
			else
				sbr.l_A[ch] = sbr.L_E[ch]+1-sbr.bs_pointer[ch];
		}

		ret = estimate_current_envelope(sbr, adj, Xsbr, ch);
		if(ret>0) return 1;

		calculate_gain(sbr, adj, ch);

		hf_assembly(sbr, adj, Xsbr, ch);

		return 0;
	}

	private static int get_S_mapped(SBR sbr, int ch, int l, int current_band) {
		if(sbr.f[ch][l]==HI_RES) {
			/* in case of using f_table_high we just have 1 to 1 mapping
			 * from bs_add_harmonic[l][k]
			 */
			if((l>=sbr.l_A[ch])
				||(sbr.bs_add_harmonic_prev[ch][current_band]!=0&&sbr.bs_add_harmonic_flag_prev[ch])) {
				return sbr.bs_add_harmonic[ch][current_band];
			}
		}
		else {
			int b, lb, ub;

			/* in case of f_table_low we check if any of the HI_RES bands
			 * within this LO_RES band has bs_add_harmonic[l][k] turned on
			 * (note that borders in the LO_RES table are also present in
			 * the HI_RES table)
			 */

			/* find first HI_RES band in current LO_RES band */
			lb = 2*current_band-((sbr.N_high&1)!=0 ? 1 : 0);
			/* find first HI_RES band in next LO_RES band */
			ub = 2*(current_band+1)-((sbr.N_high&1)!=0 ? 1 : 0);

			/* check all HI_RES bands in current LO_RES band for sinusoid */
			for(b = lb; b<ub; b++) {
				if((l>=sbr.l_A[ch])
					||(sbr.bs_add_harmonic_prev[ch][b]!=0&&sbr.bs_add_harmonic_flag_prev[ch])) {
					if(sbr.bs_add_harmonic[ch][b]==1)
						return 1;
				}
			}
		}

		return 0;
	}

	private static int estimate_current_envelope(SBR sbr, HFAdjustment adj,
		float[][][] Xsbr, int ch) {
		int m, l, j, k, k_l, k_h, p;
		float nrg, div;

		if(sbr.bs_interpol_freq) {
			for(l = 0; l<sbr.L_E[ch]; l++) {
				int i, l_i, u_i;

				l_i = sbr.t_E[ch][l];
				u_i = sbr.t_E[ch][l+1];

				div = (float) (u_i-l_i);

				if(div==0)
					div = 1;

				for(m = 0; m<sbr.M; m++) {
					nrg = 0;

					for(i = l_i+sbr.tHFAdj; i<u_i+sbr.tHFAdj; i++) {
						nrg += (Xsbr[i][m+sbr.kx][0]*Xsbr[i][m+sbr.kx][0])
							+(Xsbr[i][m+sbr.kx][1]*Xsbr[i][m+sbr.kx][1]);
					}

					sbr.E_curr[ch][m][l] = nrg/div;
				}
			}
		}
		else {
			for(l = 0; l<sbr.L_E[ch]; l++) {
				for(p = 0; p<sbr.n[sbr.f[ch][l]]; p++) {
					k_l = sbr.f_table_res[sbr.f[ch][l]][p];
					k_h = sbr.f_table_res[sbr.f[ch][l]][p+1];

					for(k = k_l; k<k_h; k++) {
						int i, l_i, u_i;
						nrg = 0;

						l_i = sbr.t_E[ch][l];
						u_i = sbr.t_E[ch][l+1];

						div = (float) ((u_i-l_i)*(k_h-k_l));

						if(div==0)
							div = 1;

						for(i = l_i+sbr.tHFAdj; i<u_i+sbr.tHFAdj; i++) {
							for(j = k_l; j<k_h; j++) {
								nrg += (Xsbr[i][j][0]*Xsbr[i][j][0])
									+(Xsbr[i][j][1]*Xsbr[i][j][1]);
							}
						}

						sbr.E_curr[ch][k-sbr.kx][l] = nrg/div;
					}
				}
			}
		}

		return 0;
	}

	private static void hf_assembly(SBR sbr, HFAdjustment adj,
		float[][][] Xsbr, int ch) {

		int m, l, i, n;
		int fIndexNoise = 0;
		int fIndexSine = 0;
		boolean assembly_reset = false;

		float G_filt, Q_filt;

		int h_SL;

		if(sbr.Reset) {
			assembly_reset = true;
			fIndexNoise = 0;
		}
		else {
			fIndexNoise = sbr.index_noise_prev[ch];
		}
		fIndexSine = sbr.psi_is_prev[ch];

		for(l = 0; l<sbr.L_E[ch]; l++) {
			boolean no_noise = (l==sbr.l_A[ch]||l==sbr.prevEnvIsShort[ch]);

			h_SL = (sbr.bs_smoothing_mode) ? 0 : 4;
			h_SL = (no_noise ? 0 : h_SL);

			if(assembly_reset) {
				for(n = 0; n<4; n++) {
					arraycopy(adj.G_lim_boost[l], 0, sbr.G_temp_prev[ch][n], 0, sbr.M);
					arraycopy(adj.Q_M_lim_boost[l], 0, sbr.Q_temp_prev[ch][n], 0, sbr.M);
				}
				/* reset ringbuffer index */
				sbr.GQ_ringbuf_index[ch] = 4;
				assembly_reset = false;
			}

			for(i = sbr.t_E[ch][l]; i<sbr.t_E[ch][l+1]; i++) {
				/* load new values into ringbuffer */
				arraycopy(adj.G_lim_boost[l], 0, sbr.G_temp_prev[ch][sbr.GQ_ringbuf_index[ch]], 0, sbr.M);
				arraycopy(adj.Q_M_lim_boost[l], 0, sbr.Q_temp_prev[ch][sbr.GQ_ringbuf_index[ch]], 0, sbr.M);

				for(m = 0; m<sbr.M; m++) {
					float[] psi = new float[2];

					G_filt = 0;
					Q_filt = 0;

					if(h_SL!=0) {
						int ri = sbr.GQ_ringbuf_index[ch];
						for(n = 0; n<=4; n++) {
							float curr_h_smooth = h_smooth[n];
							ri++;
							if(ri>=5)
								ri -= 5;
							G_filt += (sbr.G_temp_prev[ch][ri][m]*curr_h_smooth);
							Q_filt += (sbr.Q_temp_prev[ch][ri][m]*curr_h_smooth);
						}
					}
					else {
						G_filt = sbr.G_temp_prev[ch][sbr.GQ_ringbuf_index[ch]][m];
						Q_filt = sbr.Q_temp_prev[ch][sbr.GQ_ringbuf_index[ch]][m];
					}

					Q_filt = (adj.S_M_boost[l][m]!=0||no_noise) ? 0 : Q_filt;

					/* add noise to the output */
					fIndexNoise = (fIndexNoise+1)&511;

					/* the smoothed gain values are applied to Xsbr */
					/* V is defined, not calculated */
					Xsbr[i+sbr.tHFAdj][m+sbr.kx][0] = G_filt*Xsbr[i+sbr.tHFAdj][m+sbr.kx][0]
						+(Q_filt*NOISE_TABLE[fIndexNoise][0]);
					if(sbr.bs_extension_id==3&&sbr.bs_extension_data==42)
						Xsbr[i+sbr.tHFAdj][m+sbr.kx][0] = 16428320;
					Xsbr[i+sbr.tHFAdj][m+sbr.kx][1] = G_filt*Xsbr[i+sbr.tHFAdj][m+sbr.kx][1]
						+(Q_filt*NOISE_TABLE[fIndexNoise][1]);

					{
						int rev = (((m+sbr.kx)&1)!=0 ? -1 : 1);
						psi[0] = adj.S_M_boost[l][m]*phi_re[fIndexSine];
						Xsbr[i+sbr.tHFAdj][m+sbr.kx][0] += psi[0];

						psi[1] = rev*adj.S_M_boost[l][m]*phi_im[fIndexSine];
						Xsbr[i+sbr.tHFAdj][m+sbr.kx][1] += psi[1];
					}
				}

				fIndexSine = (fIndexSine+1)&3;

				/* update the ringbuffer index used for filtering G and Q with h_smooth */
				sbr.GQ_ringbuf_index[ch]++;
				if(sbr.GQ_ringbuf_index[ch]>=5)
					sbr.GQ_ringbuf_index[ch] = 0;
			}
		}

		sbr.index_noise_prev[ch] = fIndexNoise;
		sbr.psi_is_prev[ch] = fIndexSine;
	}

	private static void calculate_gain(SBR sbr, HFAdjustment adj, int ch) {
		int m, l, k;

		int current_t_noise_band = 0;
		int S_mapped;

		float[] Q_M_lim = new float[MAX_M];
		float[] G_lim = new float[MAX_M];
		float G_boost;
		float[] S_M = new float[MAX_M];

		for(l = 0; l<sbr.L_E[ch]; l++) {
			int current_f_noise_band = 0;
			int current_res_band = 0;
			int current_res_band2 = 0;
			int current_hi_res_band = 0;

			float delta = (l==sbr.l_A[ch]||l==sbr.prevEnvIsShort[ch]) ? 0 : 1;

			S_mapped = get_S_mapped(sbr, ch, l, current_res_band2);

			if(sbr.t_E[ch][l+1]>sbr.t_Q[ch][current_t_noise_band+1]) {
				current_t_noise_band++;
			}

			for(k = 0; k<sbr.N_L[sbr.bs_limiter_bands]; k++) {
				float G_max;
				float den = 0;
				float acc1 = 0;
				float acc2 = 0;
				int current_res_band_size = 0;

				int ml1, ml2;

				ml1 = sbr.f_table_lim[sbr.bs_limiter_bands][k];
				ml2 = sbr.f_table_lim[sbr.bs_limiter_bands][k+1];


				/* calculate the accumulated E_orig and E_curr over the limiter band */
				for(m = ml1; m<ml2; m++) {
					if((m+sbr.kx)==sbr.f_table_res[sbr.f[ch][l]][current_res_band+1]) {
						current_res_band++;
					}
					acc1 += sbr.E_orig[ch][current_res_band][l];
					acc2 += sbr.E_curr[ch][m][l];
				}


				/* calculate the maximum gain */
				/* ratio of the energy of the original signal and the energy
				 * of the HF generated signal
				 */
				G_max = ((EPS+acc1)/(EPS+acc2))*limGain[sbr.bs_limiter_gains];
				G_max = Math.min(G_max, 1e10f);

				for(m = ml1; m<ml2; m++) {
					float Q_M, G;
					float Q_div, Q_div2;
					int S_index_mapped;


					/* check if m is on a noise band border */
					if((m+sbr.kx)==sbr.f_table_noise[current_f_noise_band+1]) {
						/* step to next noise band */
						current_f_noise_band++;
					}


					/* check if m is on a resolution band border */
					if((m+sbr.kx)==sbr.f_table_res[sbr.f[ch][l]][current_res_band2+1]) {
						/* step to next resolution band */
						current_res_band2++;

						/* if we move to a new resolution band, we should check if we are
						 * going to add a sinusoid in this band
						 */
						S_mapped = get_S_mapped(sbr, ch, l, current_res_band2);
					}


					/* check if m is on a HI_RES band border */
					if((m+sbr.kx)==sbr.f_table_res[HI_RES][current_hi_res_band+1]) {
						/* step to next HI_RES band */
						current_hi_res_band++;
					}


					/* find S_index_mapped
					 * S_index_mapped can only be 1 for the m in the middle of the
					 * current HI_RES band
					 */
					S_index_mapped = 0;
					if((l>=sbr.l_A[ch])
						||(sbr.bs_add_harmonic_prev[ch][current_hi_res_band]!=0&&sbr.bs_add_harmonic_flag_prev[ch])) {
						/* find the middle subband of the HI_RES frequency band */
						if((m+sbr.kx)==(sbr.f_table_res[HI_RES][current_hi_res_band+1]+sbr.f_table_res[HI_RES][current_hi_res_band])>>1)
							S_index_mapped = sbr.bs_add_harmonic[ch][current_hi_res_band];
					}


					/* Q_div: [0..1] (1/(1+Q_mapped)) */
					Q_div = sbr.Q_div[ch][current_f_noise_band][current_t_noise_band];


					/* Q_div2: [0..1] (Q_mapped/(1+Q_mapped)) */
					Q_div2 = sbr.Q_div2[ch][current_f_noise_band][current_t_noise_band];


					/* Q_M only depends on E_orig and Q_div2:
					 * since N_Q <= N_Low <= N_High we only need to recalculate Q_M on
					 * a change of current noise band
					 */
					Q_M = sbr.E_orig[ch][current_res_band2][l]*Q_div2;


					/* S_M only depends on E_orig, Q_div and S_index_mapped:
					 * S_index_mapped can only be non-zero once per HI_RES band
					 */
					if(S_index_mapped==0) {
						S_M[m] = 0;
					}
					else {
						S_M[m] = sbr.E_orig[ch][current_res_band2][l]*Q_div;

						/* accumulate sinusoid part of the total energy */
						den += S_M[m];
					}


					/* calculate gain */
					/* ratio of the energy of the original signal and the energy
					 * of the HF generated signal
					 */
					G = sbr.E_orig[ch][current_res_band2][l]/(1.0f+sbr.E_curr[ch][m][l]);
					if((S_mapped==0)&&(delta==1))
						G *= Q_div;
					else if(S_mapped==1)
						G *= Q_div2;


					/* limit the additional noise energy level */
					/* and apply the limiter */
					if(G_max>G) {
						Q_M_lim[m] = Q_M;
						G_lim[m] = G;
					}
					else {
						Q_M_lim[m] = Q_M*G_max/G;
						G_lim[m] = G_max;
					}


					/* accumulate the total energy */
					den += sbr.E_curr[ch][m][l]*G_lim[m];
					if((S_index_mapped==0)&&(l!=sbr.l_A[ch]))
						den += Q_M_lim[m];
				}

				/* G_boost: [0..2.51188643] */
				G_boost = (acc1+EPS)/(den+EPS);
				G_boost = Math.min(G_boost, 2.51188643f /* 1.584893192 ^ 2 */);

				for(m = ml1; m<ml2; m++) {
					/* apply compensation to gain, noise floor sf's and sinusoid levels */
					adj.G_lim_boost[l][m] = (float) Math.sqrt(G_lim[m]*G_boost);
					adj.Q_M_lim_boost[l][m] = (float) Math.sqrt(Q_M_lim[m]*G_boost);

					if(S_M[m]!=0) {
						adj.S_M_boost[l][m] = (float) Math.sqrt(S_M[m]*G_boost);
					}
					else {
						adj.S_M_boost[l][m] = 0;
					}
				}
			}
		}
	}

}
