package net.sourceforge.jaad.aac.sbr;

import js.util.Arrays;
import net.sourceforge.jaad.aac.SampleFrequency;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
class FBT implements SBRConstants {

	/* calculate the start QMF channel for the master frequency band table */
	/* parameter is also called k0 */
	public static int qmf_start_channel(int bs_start_freq, int bs_samplerate_mode,
		SampleFrequency sample_rate) {

		int startMin = startMinTable[sample_rate.getIndex()];
		int offsetIndex = offsetIndexTable[sample_rate.getIndex()];

		if(bs_samplerate_mode!=0) {
			return startMin+OFFSET[offsetIndex][bs_start_freq];

		}
		else {
			return startMin+OFFSET[6][bs_start_freq];
		}
	}
	private static final int[] stopMinTable = {13, 15, 20, 21, 23,
		32, 32, 35, 48, 64, 70, 96};
	private static final int[][] STOP_OFFSET_TABLE = {
		{0, 2, 4, 6, 8, 11, 14, 18, 22, 26, 31, 37, 44, 51},
		{0, 2, 4, 6, 8, 11, 14, 18, 22, 26, 31, 36, 42, 49},
		{0, 2, 4, 6, 8, 11, 14, 17, 21, 25, 29, 34, 39, 44},
		{0, 2, 4, 6, 8, 11, 14, 17, 20, 24, 28, 33, 38, 43},
		{0, 2, 4, 6, 8, 11, 14, 17, 20, 24, 28, 32, 36, 41},
		{0, 2, 4, 6, 8, 10, 12, 14, 17, 20, 23, 26, 29, 32},
		{0, 2, 4, 6, 8, 10, 12, 14, 17, 20, 23, 26, 29, 32},
		{0, 1, 3, 5, 7, 9, 11, 13, 15, 17, 20, 23, 26, 29},
		{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16},
		{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{0, -1, -2, -3, -4, -5, -6, -6, -6, -6, -6, -6, -6, -6},
		{0, -3, -6, -9, -12, -15, -18, -20, -22, -24, -26, -28, -30, -32}
	};
	/* calculate the stop QMF channel for the master frequency band table */
	/* parameter is also called k2 */

	public static int qmf_stop_channel(int bs_stop_freq, SampleFrequency sample_rate,
		int k0) {
		if(bs_stop_freq==15) {
			return Math.min(64, k0*3);
		}
		else if(bs_stop_freq==14) {
			return Math.min(64, k0*2);
		}
		else {

			int stopMin = stopMinTable[sample_rate.getIndex()];

			/* bs_stop_freq <= 13 */
			return Math.min(64, stopMin+STOP_OFFSET_TABLE[sample_rate.getIndex()][Math.min(bs_stop_freq, 13)]);
		}
	}

	/* calculate the master frequency table from k0, k2, bs_freq_scale
	 and bs_alter_scale

	 version for bs_freq_scale = 0
	 */
	public static int master_frequency_table_fs0(SBR sbr, int k0, int k2,
		boolean bs_alter_scale) {
		int incr;
		int k;
		int dk;
		int nrBands, k2Achieved;
		int k2Diff;
		int[] vDk = new int[64];

		/* mft only defined for k2 > k0 */
		if(k2<=k0) {
			sbr.N_master = 0;
			return 1;
		}

		dk = bs_alter_scale ? 2 : 1;

		if(bs_alter_scale) {
			nrBands = (((k2-k0+2)>>2)<<1);
		}
		else {
			nrBands = (((k2-k0)>>1)<<1);
		}
		nrBands = Math.min(nrBands, 63);
		if(nrBands<=0)
			return 1;

		k2Achieved = k0+nrBands*dk;
		k2Diff = k2-k2Achieved;
		for(k = 0; k<nrBands; k++) {
			vDk[k] = dk;
		}

		if(k2Diff!=0) {
			incr = (k2Diff>0) ? -1 : 1;
			k = ((k2Diff>0) ? (nrBands-1) : 0);

			while(k2Diff!=0) {
				vDk[k] -= incr;
				k += incr;
				k2Diff += incr;
			}
		}

		sbr.f_master[0] = k0;
		for(k = 1; k<=nrBands; k++) {
			sbr.f_master[k] = (sbr.f_master[k-1]+vDk[k-1]);
		}

		sbr.N_master = nrBands;
		sbr.N_master = Math.min(sbr.N_master, 64);

		return 0;
	}

	/*
	 This function finds the number of bands using this formula:
	 bands * log(a1/a0)/log(2.0) + 0.5
	 */
	public static int find_bands(int warp, int bands, int a0, int a1) {
		float div = (float) Math.log(2.0);
		if(warp!=0) div *= 1.3f;

		return (int) (bands*Math.log((float) a1/(float) a0)/div+0.5);
	}

	public static float find_initial_power(int bands, int a0, int a1) {
		return (float) Math.pow((float) a1/(float) a0, 1.0f/(float) bands);
	}

	/*
	 version for bs_freq_scale > 0
	 */
	public static int master_frequency_table(SBR sbr, int k0, int k2,
		int bs_freq_scale, boolean bs_alter_scale) {
		int k, bands;
		boolean twoRegions;
		int k1;
		int nrBand0, nrBand1;
		int[] vDk0 = new int[64], vDk1 = new int[64];
		int[] vk0 = new int[64], vk1 = new int[64];
		int[] temp1 = {6, 5, 4};
		float q, qk;
		int A_1;

		/* mft only defined for k2 > k0 */
		if(k2<=k0) {
			sbr.N_master = 0;
			return 1;
		}

		bands = temp1[bs_freq_scale-1];

		if((float) k2/(float) k0>2.2449) {
			twoRegions = true;
			k1 = k0<<1;
		}
		else {
			twoRegions = false;
			k1 = k2;
		}

		nrBand0 = (2*find_bands(0, bands, k0, k1));
		nrBand0 = Math.min(nrBand0, 63);
		if(nrBand0<=0)
			return 1;

		q = find_initial_power(nrBand0, k0, k1);
		qk = k0;
		A_1 = (int) (qk+0.5f);
		for(k = 0; k<=nrBand0; k++) {
			int A_0 = A_1;
			qk *= q;
			A_1 = (int) (qk+0.5f);
			vDk0[k] = A_1-A_0;
		}

		/* needed? */
		//qsort(vDk0, nrBand0, sizeof(vDk0[0]), longcmp);
		Arrays.sort(vDk0, 0, nrBand0);

		vk0[0] = k0;
		for(k = 1; k<=nrBand0; k++) {
			vk0[k] = vk0[k-1]+vDk0[k-1];
			if(vDk0[k-1]==0)
				return 1;
		}

		if(!twoRegions) {
			for(k = 0; k<=nrBand0; k++) {
				sbr.f_master[k] = vk0[k];
			}

			sbr.N_master = nrBand0;
			sbr.N_master = Math.min(sbr.N_master, 64);
			return 0;
		}

		nrBand1 = (2*find_bands(1 /* warped */, bands, k1, k2));
		nrBand1 = Math.min(nrBand1, 63);

		q = find_initial_power(nrBand1, k1, k2);
		qk = k1;
		A_1 = (int) (qk+0.5f);
		for(k = 0; k<=nrBand1-1; k++) {
			int A_0 = A_1;
			qk *= q;
			A_1 = (int) (qk+0.5f);
			vDk1[k] = A_1-A_0;
		}

		if(vDk1[0]<vDk0[nrBand0-1]) {
			int change;

			/* needed? */
			//qsort(vDk1, nrBand1+1, sizeof(vDk1[0]), longcmp);
			Arrays.sort(vDk1, 0, nrBand1+1);
			change = vDk0[nrBand0-1]-vDk1[0];
			vDk1[0] = vDk0[nrBand0-1];
			vDk1[nrBand1-1] = vDk1[nrBand1-1]-change;
		}

		/* needed? */
		//qsort(vDk1, nrBand1, sizeof(vDk1[0]), longcmp);
		Arrays.sort(vDk1, 0, nrBand1);
		vk1[0] = k1;
		for(k = 1; k<=nrBand1; k++) {
			vk1[k] = vk1[k-1]+vDk1[k-1];
			if(vDk1[k-1]==0)
				return 1;
		}

		sbr.N_master = nrBand0+nrBand1;
		sbr.N_master = Math.min(sbr.N_master, 64);
		for(k = 0; k<=nrBand0; k++) {
			sbr.f_master[k] = vk0[k];
		}
		for(k = nrBand0+1; k<=sbr.N_master; k++) {
			sbr.f_master[k] = vk1[k-nrBand0];
		}

		return 0;
	}

	/* calculate the derived frequency border tables from f_master */
	public static int derived_frequency_table(SBR sbr, int bs_xover_band,
		int k2) {
		int k, i = 0;
		int minus;

		/* The following relation shall be satisfied: bs_xover_band < N_Master */
		if(sbr.N_master<=bs_xover_band)
			return 1;

		sbr.N_high = sbr.N_master-bs_xover_band;
		sbr.N_low = (sbr.N_high>>1)+(sbr.N_high-((sbr.N_high>>1)<<1));

		sbr.n[0] = sbr.N_low;
		sbr.n[1] = sbr.N_high;

		for(k = 0; k<=sbr.N_high; k++) {
			sbr.f_table_res[HI_RES][k] = sbr.f_master[k+bs_xover_band];
		}

		sbr.M = sbr.f_table_res[HI_RES][sbr.N_high]-sbr.f_table_res[HI_RES][0];
		sbr.kx = sbr.f_table_res[HI_RES][0];
		if(sbr.kx>32)
			return 1;
		if(sbr.kx+sbr.M>64)
			return 1;

		minus = ((sbr.N_high&1)!=0) ? 1 : 0;

		for(k = 0; k<=sbr.N_low; k++) {
			if(k==0)
				i = 0;
			else
				i = (2*k-minus);
			sbr.f_table_res[LO_RES][k] = sbr.f_table_res[HI_RES][i];
		}

		sbr.N_Q = 0;
		if(sbr.bs_noise_bands==0) {
			sbr.N_Q = 1;
		}
		else {
			sbr.N_Q = (Math.max(1, find_bands(0, sbr.bs_noise_bands, sbr.kx, k2)));
			sbr.N_Q = Math.min(5, sbr.N_Q);
		}

		for(k = 0; k<=sbr.N_Q; k++) {
			if(k==0) {
				i = 0;
			}
			else {
				/* i = i + (int32_t)((sbr.N_low - i)/(sbr.N_Q + 1 - k)); */
				i += (sbr.N_low-i)/(sbr.N_Q+1-k);
			}
			sbr.f_table_noise[k] = sbr.f_table_res[LO_RES][i];
		}

		/* build table for mapping k to g in hf patching */
		for(k = 0; k<64; k++) {
			int g;
			for(g = 0; g<sbr.N_Q; g++) {
				if((sbr.f_table_noise[g]<=k)
					&&(k<sbr.f_table_noise[g+1])) {
					sbr.table_map_k_to_g[k] = g;
					break;
				}
			}
		}
		return 0;
	}

	/* TODO: blegh, ugly */
	/* Modified to calculate for all possible bs_limiter_bands always
	 * This reduces the number calls to this functions needed (now only on
	 * header reset)
	 */
	private static final float[] limiterBandsCompare = {1.327152f,
		1.185093f, 1.119872f};

	public static void limiter_frequency_table(SBR sbr) {

		int k, s;
		int nrLim;

		sbr.f_table_lim[0][0] = sbr.f_table_res[LO_RES][0]-sbr.kx;
		sbr.f_table_lim[0][1] = sbr.f_table_res[LO_RES][sbr.N_low]-sbr.kx;
		sbr.N_L[0] = 1;

		for(s = 1; s<4; s++) {
			int[] limTable = new int[100 /*TODO*/];
			int[] patchBorders = new int[64/*??*/];

			patchBorders[0] = sbr.kx;
			for(k = 1; k<=sbr.noPatches; k++) {
				patchBorders[k] = patchBorders[k-1]+sbr.patchNoSubbands[k-1];
			}

			for(k = 0; k<=sbr.N_low; k++) {
				limTable[k] = sbr.f_table_res[LO_RES][k];
			}
			for(k = 1; k<sbr.noPatches; k++) {
				limTable[k+sbr.N_low] = patchBorders[k];
			}

			/* needed */
			//qsort(limTable, sbr.noPatches+sbr.N_low, sizeof(limTable[0]), longcmp);
			Arrays.sort(limTable, 0, sbr.noPatches+sbr.N_low);
			k = 1;
			nrLim = sbr.noPatches+sbr.N_low-1;

			if(nrLim<0) // TODO: BIG FAT PROBLEM
				return;

			restart:
			while(k<=nrLim) {
				float nOctaves;

				if(limTable[k-1]!=0)
					nOctaves = (float) limTable[k]/(float) limTable[k-1];
				else
					nOctaves = 0;

				if(nOctaves<limiterBandsCompare[s-1]) {
					int i;
					if(limTable[k]!=limTable[k-1]) {
						boolean found = false, found2 = false;
						for(i = 0; i<=sbr.noPatches; i++) {
							if(limTable[k]==patchBorders[i])
								found = true;
						}
						if(found) {
							found2 = false;
							for(i = 0; i<=sbr.noPatches; i++) {
								if(limTable[k-1]==patchBorders[i])
									found2 = true;
							}
							if(found2) {
								k++;
								continue;
							}
							else {
								/* remove (k-1)th element */
								limTable[k-1] = sbr.f_table_res[LO_RES][sbr.N_low];
								//qsort(limTable, sbr.noPatches+sbr.N_low, sizeof(limTable[0]), longcmp);
								Arrays.sort(limTable, 0, sbr.noPatches+sbr.N_low);
								nrLim--;
								continue;
							}
						}
					}
					/* remove kth element */
					limTable[k] = sbr.f_table_res[LO_RES][sbr.N_low];
					//qsort(limTable, nrLim, sizeof(limTable[0]), longcmp);
					Arrays.sort(limTable, 0, nrLim);
					nrLim--;
					//continue;
				}
				else {
					k++;
					//continue;
				}
			}

			sbr.N_L[s] = nrLim;
			for(k = 0; k<=nrLim; k++) {
				sbr.f_table_lim[s][k] = limTable[k]-sbr.kx;
			}

		}
	}

}
