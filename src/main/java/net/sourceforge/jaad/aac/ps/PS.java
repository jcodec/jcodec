package net.sourceforge.jaad.aac.ps;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.syntax.IBitStream;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
public class PS implements PSConstants, PSTables, PSHuffmanTables {

	/* bitstream parameters */
	boolean enable_iid, enable_icc, enable_ext;
	int iid_mode;
	int icc_mode;
	int nr_iid_par;
	int nr_ipdopd_par;
	int nr_icc_par;
	int frame_class;
	int num_env;
	int[] border_position;
	boolean[] iid_dt;
	boolean[] icc_dt;
	boolean enable_ipdopd;
	int ipd_mode;
	boolean[] ipd_dt;
	boolean[] opd_dt;

	/* indices */
	int[] iid_index_prev;
	int[] icc_index_prev;
	int[] ipd_index_prev;
	int[] opd_index_prev;
	int[][] iid_index;
	int[][] icc_index;
	int[][] ipd_index;
	int[][] opd_index;

	int[] ipd_index_1;
	int[] opd_index_1;
	int[] ipd_index_2;
	int[] opd_index_2;
	/* ps data was correctly read */
	int ps_data_available;
	/* a header has been read */
	public boolean header_read;
	/* hybrid filterbank parameters */
	PSFilterbank hyb;
	boolean use34hybrid_bands;
	int numTimeSlotsRate;
	int num_groups;
	int num_hybrid_groups;
	int nr_par_bands;
	int nr_allpass_bands;
	int decay_cutoff;
	int[] group_border;
	int[] map_group2bk;
	/* filter delay handling */
	int saved_delay;
	int[] delay_buf_index_ser;
	int[] num_sample_delay_ser;
	int[] delay_D;
	int[] delay_buf_index_delay;
	float[][][] delay_Qmf; /* 14 samples delay max, 64 QMF channels */

	float[][][] delay_SubQmf; /* 2 samples delay max (SubQmf is always allpass filtered) */

	float[][][][] delay_Qmf_ser; /* 5 samples delay max (table 8.34), 64 QMF channels */

	float[][][][] delay_SubQmf_ser; /* 5 samples delay max (table 8.34) */
	/* transients */

	float alpha_decay;
	float alpha_smooth;
	float[] P_PeakDecayNrg;
	float[] P_prev;
	float[] P_SmoothPeakDecayDiffNrg_prev;
	/* mixing and phase */
	float[][] h11_prev;
	float[][] h12_prev;
	float[][] h21_prev;
	float[][] h22_prev;
	int phase_hist;
	float[][][] ipd_prev;
	float[][][] opd_prev;

	public PS(SampleFrequency sr, int numTimeSlotsRate) {
	    this.border_position = new int[MAX_PS_ENVELOPES+1];
        this.iid_dt = new boolean[MAX_PS_ENVELOPES];
        this.icc_dt = new boolean[MAX_PS_ENVELOPES];
        this.ipd_dt = new boolean[MAX_PS_ENVELOPES];
        this.opd_dt = new boolean[MAX_PS_ENVELOPES];
        this.iid_index_prev = new int[34];
        this.icc_index_prev = new int[34];
        this.ipd_index_prev = new int[17];
        this.opd_index_prev = new int[17];
        this.iid_index = new int[MAX_PS_ENVELOPES][34];
        this.icc_index = new int[MAX_PS_ENVELOPES][34];
        this.ipd_index = new int[MAX_PS_ENVELOPES][17];
        this.opd_index = new int[MAX_PS_ENVELOPES][17];
        this.ipd_index_1 = new int[17];
        this.opd_index_1 = new int[17];
        this.ipd_index_2 = new int[17];
        this.opd_index_2 = new int[17];
        this.delay_buf_index_ser = new int[NO_ALLPASS_LINKS];
        this.num_sample_delay_ser = new int[NO_ALLPASS_LINKS];
        this.delay_D = new int[64];
        this.delay_buf_index_delay = new int[64];
        this.delay_Qmf = new float[14][64][2]; /* 14 samples delay max, 64 QMF channels */
        this.delay_SubQmf = new float[2][32][2]; /* 2 samples delay max (SubQmf is always allpass filtered) */
        this.delay_Qmf_ser = new float[NO_ALLPASS_LINKS][5][64][2]; /* 5 samples delay max (table 8.34), 64 QMF channels */
        this.delay_SubQmf_ser = new float[NO_ALLPASS_LINKS][5][32][2]; /* 5 samples delay max (table 8.34) */
        this.P_PeakDecayNrg = new float[34];
        this.P_prev = new float[34];
        this.P_SmoothPeakDecayDiffNrg_prev = new float[34];
        this.h11_prev = new float[50][2];
        this.h12_prev = new float[50][2];
        this.h21_prev = new float[50][2];
        this.h22_prev = new float[50][2];
        this.ipd_prev = new float[20][2][2];
        this.opd_prev = new float[20][2][2];

		int i;
		int short_delay_band;

		hyb = new PSFilterbank(numTimeSlotsRate);
		this.numTimeSlotsRate = numTimeSlotsRate;

		this.ps_data_available = 0;

		/* delay stuff*/
		this.saved_delay = 0;

		for(i = 0; i<64; i++) {
			this.delay_buf_index_delay[i] = 0;
		}

		for(i = 0; i<NO_ALLPASS_LINKS; i++) {
			this.delay_buf_index_ser[i] = 0;
			/* THESE ARE CONSTANTS NOW */
			this.num_sample_delay_ser[i] = delay_length_d[i];
		}

		/* THESE ARE CONSTANTS NOW */
		short_delay_band = 35;
		this.nr_allpass_bands = 22;
		this.alpha_decay = 0.76592833836465f;
		this.alpha_smooth = 0.25f;

		/* THESE ARE CONSTANT NOW IF PS IS INDEPENDANT OF SAMPLERATE */
		for(i = 0; i<short_delay_band; i++) {
			this.delay_D[i] = 14;
		}
		for(i = short_delay_band; i<64; i++) {
			this.delay_D[i] = 1;
		}

		/* mixing and phase */
		for(i = 0; i<50; i++) {
			this.h11_prev[i][0] = 1;
			this.h12_prev[i][1] = 1;
			this.h11_prev[i][0] = 1;
			this.h12_prev[i][1] = 1;
		}

		this.phase_hist = 0;

		for(i = 0; i<20; i++) {
			this.ipd_prev[i][0][0] = 0;
			this.ipd_prev[i][0][1] = 0;
			this.ipd_prev[i][1][0] = 0;
			this.ipd_prev[i][1][1] = 0;
			this.opd_prev[i][0][0] = 0;
			this.opd_prev[i][0][1] = 0;
			this.opd_prev[i][1][0] = 0;
			this.opd_prev[i][1][1] = 0;
		}
	}

	public int decode(IBitStream ld) throws AACException {
		int tmp, n;
		long bits = ld.getPosition();

		/* check for new PS header */
		if(ld.readBool()) {
			this.header_read = true;

			this.use34hybrid_bands = false;

			/* Inter-channel Intensity Difference (IID) parameters enabled */
			this.enable_iid = ld.readBool();

			if(this.enable_iid) {
				this.iid_mode = ld.readBits(3);

				this.nr_iid_par = nr_iid_par_tab[this.iid_mode];
				this.nr_ipdopd_par = nr_ipdopd_par_tab[this.iid_mode];

				if(this.iid_mode==2||this.iid_mode==5)
					this.use34hybrid_bands = true;

				/* IPD freq res equal to IID freq res */
				this.ipd_mode = this.iid_mode;
			}

			/* Inter-channel Coherence (ICC) parameters enabled */
			this.enable_icc = ld.readBool();

			if(this.enable_icc) {
				this.icc_mode = ld.readBits(3);

				this.nr_icc_par = nr_icc_par_tab[this.icc_mode];

				if(this.icc_mode==2||this.icc_mode==5)
					this.use34hybrid_bands = true;
			}

			/* PS extension layer enabled */
			this.enable_ext = ld.readBool();
		}

		/* we are here, but no header has been read yet */
		if(this.header_read==false) {
			this.ps_data_available = 0;
			return 1;
		}

		this.frame_class = ld.readBit();
		tmp = ld.readBits(2);

		this.num_env = num_env_tab[this.frame_class][tmp];

		if(this.frame_class!=0) {
			for(n = 1; n<this.num_env+1; n++) {
				this.border_position[n] = ld.readBits(5)+1;
			}
		}

		if(this.enable_iid) {
			for(n = 0; n<this.num_env; n++) {
				this.iid_dt[n] = ld.readBool();

				/* iid_data */
				if(this.iid_mode<3) {
					huff_data(ld, this.iid_dt[n], this.nr_iid_par, t_huff_iid_def,
						f_huff_iid_def, this.iid_index[n]);
				}
				else {
					huff_data(ld, this.iid_dt[n], this.nr_iid_par, t_huff_iid_fine,
						f_huff_iid_fine, this.iid_index[n]);
				}
			}
		}

		if(this.enable_icc) {
			for(n = 0; n<this.num_env; n++) {
				this.icc_dt[n] = ld.readBool();

				/* icc_data */
				huff_data(ld, this.icc_dt[n], this.nr_icc_par, t_huff_icc,
					f_huff_icc, this.icc_index[n]);
			}
		}

		if(this.enable_ext) {
			int num_bits_left;
			int cnt = ld.readBits(4);
			if(cnt==15) {
				cnt += ld.readBits(8);
			}

			num_bits_left = 8*cnt;
			while(num_bits_left>7) {
				int ps_extension_id = ld.readBits(2);

				num_bits_left -= 2;
				num_bits_left -= ps_extension(ld, ps_extension_id, num_bits_left);
			}

			ld.skipBits(num_bits_left);
		}

		int bits2 = (int) (ld.getPosition()-bits);

		this.ps_data_available = 1;

		return bits2;
	}

	private int ps_extension(IBitStream ld,
		int ps_extension_id,
		int num_bits_left) throws AACException {
		int n;
		long bits = ld.getPosition();

		if(ps_extension_id==0) {
			this.enable_ipdopd = ld.readBool();

			if(this.enable_ipdopd) {
				for(n = 0; n<this.num_env; n++) {
					this.ipd_dt[n] = ld.readBool();

					/* ipd_data */
					huff_data(ld, this.ipd_dt[n], this.nr_ipdopd_par, t_huff_ipd,
						f_huff_ipd, this.ipd_index[n]);

					this.opd_dt[n] = ld.readBool();

					/* opd_data */
					huff_data(ld, this.opd_dt[n], this.nr_ipdopd_par, t_huff_opd,
						f_huff_opd, this.opd_index[n]);
				}
			}
			ld.readBit(); //reserved
		}

		/* return number of bits read */
		int bits2 = (int) (ld.getPosition()-bits);

		return bits2;
	}

	/* read huffman data coded in either the frequency or the time direction */
	private void huff_data(IBitStream ld, boolean dt, int nr_par,
		int[][] t_huff, int[][] f_huff, int[] par) throws AACException {
		int n;

		if(dt) {
			/* coded in time direction */
			for(n = 0; n<nr_par; n++) {
				par[n] = ps_huff_dec(ld, t_huff);
			}
		}
		else {
			/* coded in frequency direction */
			par[0] = ps_huff_dec(ld, f_huff);

			for(n = 1; n<nr_par; n++) {
				par[n] = ps_huff_dec(ld, f_huff);
			}
		}
	}

	/* binary search huffman decoding */
	private int ps_huff_dec(IBitStream ld, int[][] t_huff) throws AACException {
		int bit;
		int index = 0;

		while(index>=0) {
			bit = ld.readBit();
			index = t_huff[index][bit];
		}

		return index+31;
	}

	/* limits the value i to the range [min,max] */
	private int delta_clip(int i, int min, int max) {
		if(i<min) return min;
		else if(i>max) return max;
		else return i;
	}


	/* delta decode array */
	private void delta_decode(boolean enable, int[] index, int[] index_prev,
		boolean dt_flag, int nr_par, int stride,
		int min_index, int max_index) {
		int i;

		if(enable) {
			if(!dt_flag) {
				/* delta coded in frequency direction */
				index[0] = 0+index[0];
				index[0] = delta_clip(index[0], min_index, max_index);

				for(i = 1; i<nr_par; i++) {
					index[i] = index[i-1]+index[i];
					index[i] = delta_clip(index[i], min_index, max_index);
				}
			}
			else {
				/* delta coded in time direction */
				for(i = 0; i<nr_par; i++) {
                //int8_t tmp2;
					//int8_t tmp = index[i];

					//printf("%d %d\n", index_prev[i*stride], index[i]);
					//printf("%d\n", index[i]);
					index[i] = index_prev[i*stride]+index[i];
					//tmp2 = index[i];
					index[i] = delta_clip(index[i], min_index, max_index);

					//if (iid)
					//{
					//    if (index[i] == 7)
					//    {
					//        printf("%d %d %d\n", index_prev[i*stride], tmp, tmp2);
					//    }
					//}
				}
			}
		}
		else {
			/* set indices to zero */
			for(i = 0; i<nr_par; i++) {
				index[i] = 0;
			}
		}

		/* coarse */
		if(stride==2) {
			for(i = (nr_par<<1)-1; i>0; i--) {
				index[i] = index[i>>1];
			}
		}
	}

	/* delta modulo decode array */
	/* in: log2 value of the modulo value to allow using AND instead of MOD */
	private void delta_modulo_decode(boolean enable, int[] index, int[] index_prev,
		boolean dt_flag, int nr_par, int stride,
		int and_modulo) {
		int i;

		if(enable) {
			if(!dt_flag) {
				/* delta coded in frequency direction */
				index[0] = 0+index[0];
				index[0] &= and_modulo;

				for(i = 1; i<nr_par; i++) {
					index[i] = index[i-1]+index[i];
					index[i] &= and_modulo;
				}
			}
			else {
				/* delta coded in time direction */
				for(i = 0; i<nr_par; i++) {
					index[i] = index_prev[i*stride]+index[i];
					index[i] &= and_modulo;
				}
			}
		}
		else {
			/* set indices to zero */
			for(i = 0; i<nr_par; i++) {
				index[i] = 0;
			}
		}

		/* coarse */
		if(stride==2) {
			index[0] = 0;
			for(i = (nr_par<<1)-1; i>0; i--) {
				index[i] = index[i>>1];
			}
		}
	}

	private void map20indexto34(int[] index, int bins) {
		//index[0] = index[0];
		index[1] = (index[0]+index[1])/2;
		index[2] = index[1];
		index[3] = index[2];
		index[4] = (index[2]+index[3])/2;
		index[5] = index[3];
		index[6] = index[4];
		index[7] = index[4];
		index[8] = index[5];
		index[9] = index[5];
		index[10] = index[6];
		index[11] = index[7];
		index[12] = index[8];
		index[13] = index[8];
		index[14] = index[9];
		index[15] = index[9];
		index[16] = index[10];

		if(bins==34) {
			index[17] = index[11];
			index[18] = index[12];
			index[19] = index[13];
			index[20] = index[14];
			index[21] = index[14];
			index[22] = index[15];
			index[23] = index[15];
			index[24] = index[16];
			index[25] = index[16];
			index[26] = index[17];
			index[27] = index[17];
			index[28] = index[18];
			index[29] = index[18];
			index[30] = index[18];
			index[31] = index[18];
			index[32] = index[19];
			index[33] = index[19];
		}
	}

	/* parse the bitstream data decoded in ps_data() */
	private void ps_data_decode() {
		int env, bin;

		/* ps data not available, use data from previous frame */
		if(this.ps_data_available==0) {
			this.num_env = 0;
		}

		for(env = 0; env<this.num_env; env++) {
			int[] iid_index_prev;
			int[] icc_index_prev;
			int[] ipd_index_prev;
			int[] opd_index_prev;

			int num_iid_steps = (this.iid_mode<3) ? 7 : 15 /*fine quant*/;

			if(env==0) {
				/* take last envelope from previous frame */
				iid_index_prev = this.iid_index_prev;
				icc_index_prev = this.icc_index_prev;
				ipd_index_prev = this.ipd_index_prev;
				opd_index_prev = this.opd_index_prev;
			}
			else {
				/* take index values from previous envelope */
				iid_index_prev = this.iid_index[env-1];
				icc_index_prev = this.icc_index[env-1];
				ipd_index_prev = this.ipd_index[env-1];
				opd_index_prev = this.opd_index[env-1];
			}

//        iid = 1;
        /* delta decode iid parameters */
			delta_decode(this.enable_iid, this.iid_index[env], iid_index_prev,
				this.iid_dt[env], this.nr_iid_par,
				(this.iid_mode==0||this.iid_mode==3) ? 2 : 1,
				-num_iid_steps, num_iid_steps);
//        iid = 0;

			/* delta decode icc parameters */
			delta_decode(this.enable_icc, this.icc_index[env], icc_index_prev,
				this.icc_dt[env], this.nr_icc_par,
				(this.icc_mode==0||this.icc_mode==3) ? 2 : 1,
				0, 7);

			/* delta modulo decode ipd parameters */
			delta_modulo_decode(this.enable_ipdopd, this.ipd_index[env], ipd_index_prev,
				this.ipd_dt[env], this.nr_ipdopd_par, 1, 7);

			/* delta modulo decode opd parameters */
			delta_modulo_decode(this.enable_ipdopd, this.opd_index[env], opd_index_prev,
				this.opd_dt[env], this.nr_ipdopd_par, 1, 7);
		}

		/* handle error case */
		if(this.num_env==0) {
			/* force to 1 */
			this.num_env = 1;

			if(this.enable_iid) {
				for(bin = 0; bin<34; bin++) {
					this.iid_index[0][bin] = this.iid_index_prev[bin];
				}
			}
			else {
				for(bin = 0; bin<34; bin++) {
					this.iid_index[0][bin] = 0;
				}
			}

			if(this.enable_icc) {
				for(bin = 0; bin<34; bin++) {
					this.icc_index[0][bin] = this.icc_index_prev[bin];
				}
			}
			else {
				for(bin = 0; bin<34; bin++) {
					this.icc_index[0][bin] = 0;
				}
			}

			if(this.enable_ipdopd) {
				for(bin = 0; bin<17; bin++) {
					this.ipd_index[0][bin] = this.ipd_index_prev[bin];
					this.opd_index[0][bin] = this.opd_index_prev[bin];
				}
			}
			else {
				for(bin = 0; bin<17; bin++) {
					this.ipd_index[0][bin] = 0;
					this.opd_index[0][bin] = 0;
				}
			}
		}

		/* update previous indices */
		for(bin = 0; bin<34; bin++) {
			this.iid_index_prev[bin] = this.iid_index[this.num_env-1][bin];
		}
		for(bin = 0; bin<34; bin++) {
			this.icc_index_prev[bin] = this.icc_index[this.num_env-1][bin];
		}
		for(bin = 0; bin<17; bin++) {
			this.ipd_index_prev[bin] = this.ipd_index[this.num_env-1][bin];
			this.opd_index_prev[bin] = this.opd_index[this.num_env-1][bin];
		}

		this.ps_data_available = 0;

		if(this.frame_class==0) {
			this.border_position[0] = 0;
			for(env = 1; env<this.num_env; env++) {
				this.border_position[env] = (env*this.numTimeSlotsRate)/this.num_env;
			}
			this.border_position[this.num_env] = this.numTimeSlotsRate;
		}
		else {
			this.border_position[0] = 0;

			if(this.border_position[this.num_env]<this.numTimeSlotsRate) {
				for(bin = 0; bin<34; bin++) {
					this.iid_index[this.num_env][bin] = this.iid_index[this.num_env-1][bin];
					this.icc_index[this.num_env][bin] = this.icc_index[this.num_env-1][bin];
				}
				for(bin = 0; bin<17; bin++) {
					this.ipd_index[this.num_env][bin] = this.ipd_index[this.num_env-1][bin];
					this.opd_index[this.num_env][bin] = this.opd_index[this.num_env-1][bin];
				}
				this.num_env++;
				this.border_position[this.num_env] = this.numTimeSlotsRate;
			}

			for(env = 1; env<this.num_env; env++) {
				int thr = this.numTimeSlotsRate-(this.num_env-env);

				if(this.border_position[env]>thr) {
					this.border_position[env] = thr;
				}
				else {
					thr = this.border_position[env-1]+1;
					if(this.border_position[env]<thr) {
						this.border_position[env] = thr;
					}
				}
			}
		}

		/* make sure that the indices of all parameters can be mapped
		 * to the same hybrid synthesis filterbank
		 */
		if(this.use34hybrid_bands) {
			for(env = 0; env<this.num_env; env++) {
				if(this.iid_mode!=2&&this.iid_mode!=5)
					map20indexto34(this.iid_index[env], 34);
				if(this.icc_mode!=2&&this.icc_mode!=5)
					map20indexto34(this.icc_index[env], 34);
				if(this.ipd_mode!=2&&this.ipd_mode!=5) {
					map20indexto34(this.ipd_index[env], 17);
					map20indexto34(this.opd_index[env], 17);
				}
			}
		}
	}

	/* decorrelate the mono signal using an allpass filter */
	private void ps_decorrelate(float[][][] X_left, float[][][] X_right,
		float[][][] X_hybrid_left, float[][][] X_hybrid_right) {
		int gr, n, m, bk;
		int temp_delay = 0;
		int sb, maxsb;
		int[] temp_delay_ser = new int[NO_ALLPASS_LINKS];
		float P_SmoothPeakDecayDiffNrg, nrg;
		float[][] P = new float[32][34];
		float[][] G_TransientRatio = new float[32][34];
		float[] inputLeft = new float[2];


		/* chose hybrid filterbank: 20 or 34 band case */
		float[][] Phi_Fract_SubQmf;
		if(this.use34hybrid_bands) {
			Phi_Fract_SubQmf = Phi_Fract_SubQmf34;
		}
		else {
			Phi_Fract_SubQmf = Phi_Fract_SubQmf20;
		}

		/* clear the energy values */
		for(n = 0; n<32; n++) {
			for(bk = 0; bk<34; bk++) {
				P[n][bk] = 0;
			}
		}

		/* calculate the energy in each parameter band b(k) */
		for(gr = 0; gr<this.num_groups; gr++) {
			/* select the parameter index b(k) to which this group belongs */
			bk = (~NEGATE_IPD_MASK)&this.map_group2bk[gr];

			/* select the upper subband border for this group */
			maxsb = (gr<this.num_hybrid_groups) ? this.group_border[gr]+1 : this.group_border[gr+1];

			for(sb = this.group_border[gr]; sb<maxsb; sb++) {
				for(n = this.border_position[0]; n<this.border_position[this.num_env]; n++) {

					/* input from hybrid subbands or QMF subbands */
					if(gr<this.num_hybrid_groups) {
						inputLeft[0] = X_hybrid_left[n][sb][0];
						inputLeft[1] = X_hybrid_left[n][sb][1];
					}
					else {
						inputLeft[0] = X_left[n][sb][0];
						inputLeft[1] = X_left[n][sb][1];
					}

					/* accumulate energy */
					P[n][bk] += (inputLeft[0]*inputLeft[0])+(inputLeft[1]*inputLeft[1]);
				}
			}
		}

		/* calculate transient reduction ratio for each parameter band b(k) */
		for(bk = 0; bk<this.nr_par_bands; bk++) {
			for(n = this.border_position[0]; n<this.border_position[this.num_env]; n++) {
				float gamma = 1.5f;

				this.P_PeakDecayNrg[bk] = (this.P_PeakDecayNrg[bk]*this.alpha_decay);
				if(this.P_PeakDecayNrg[bk]<P[n][bk])
					this.P_PeakDecayNrg[bk] = P[n][bk];

				/* apply smoothing filter to peak decay energy */
				P_SmoothPeakDecayDiffNrg = this.P_SmoothPeakDecayDiffNrg_prev[bk];
				P_SmoothPeakDecayDiffNrg += ((this.P_PeakDecayNrg[bk]-P[n][bk]-this.P_SmoothPeakDecayDiffNrg_prev[bk])*alpha_smooth);
				this.P_SmoothPeakDecayDiffNrg_prev[bk] = P_SmoothPeakDecayDiffNrg;

				/* apply smoothing filter to energy */
				nrg = this.P_prev[bk];
				nrg += ((P[n][bk]-this.P_prev[bk])*this.alpha_smooth);
				this.P_prev[bk] = nrg;

				/* calculate transient ratio */
				if((P_SmoothPeakDecayDiffNrg*gamma)<=nrg) {
					G_TransientRatio[n][bk] = 1.0f;
				}
				else {
					G_TransientRatio[n][bk] = (nrg/(P_SmoothPeakDecayDiffNrg*gamma));
				}
			}
		}

		/* apply stereo decorrelation filter to the signal */
		for(gr = 0; gr<this.num_groups; gr++) {
			if(gr<this.num_hybrid_groups)
				maxsb = this.group_border[gr]+1;
			else
				maxsb = this.group_border[gr+1];

			/* QMF channel */
			for(sb = this.group_border[gr]; sb<maxsb; sb++) {
				float g_DecaySlope;
				float[] g_DecaySlope_filt = new float[NO_ALLPASS_LINKS];

				/* g_DecaySlope: [0..1] */
				if(gr<this.num_hybrid_groups||sb<=this.decay_cutoff) {
					g_DecaySlope = 1.0f;
				}
				else {
					int decay = this.decay_cutoff-sb;
					if(decay<=-20 /* -1/DECAY_SLOPE */) {
						g_DecaySlope = 0;
					}
					else {
						/* decay(int)*decay_slope(frac) = g_DecaySlope(frac) */
						g_DecaySlope = 1.0f+DECAY_SLOPE*decay;
					}
				}

				/* calculate g_DecaySlope_filt for every m multiplied by filter_a[m] */
				for(m = 0; m<NO_ALLPASS_LINKS; m++) {
					g_DecaySlope_filt[m] = g_DecaySlope*filter_a[m];
				}


				/* set delay indices */
				temp_delay = this.saved_delay;
				for(n = 0; n<NO_ALLPASS_LINKS; n++) {
					temp_delay_ser[n] = this.delay_buf_index_ser[n];
				}

				for(n = this.border_position[0]; n<this.border_position[this.num_env]; n++) {
					float[] tmp = new float[2], tmp0 = new float[2], R0 = new float[2];

					if(gr<this.num_hybrid_groups) {
						/* hybrid filterbank input */
						inputLeft[0] = X_hybrid_left[n][sb][0];
						inputLeft[1] = X_hybrid_left[n][sb][1];
					}
					else {
						/* QMF filterbank input */
						inputLeft[0] = X_left[n][sb][0];
						inputLeft[1] = X_left[n][sb][1];
					}

					if(sb>this.nr_allpass_bands&&gr>=this.num_hybrid_groups) {
						/* delay */

						/* never hybrid subbands here, always QMF subbands */
						tmp[0] = this.delay_Qmf[this.delay_buf_index_delay[sb]][sb][0];
						tmp[1] = this.delay_Qmf[this.delay_buf_index_delay[sb]][sb][1];
						R0[0] = tmp[0];
						R0[1] = tmp[1];
						this.delay_Qmf[this.delay_buf_index_delay[sb]][sb][0] = inputLeft[0];
						this.delay_Qmf[this.delay_buf_index_delay[sb]][sb][1] = inputLeft[1];
					}
					else {
						/* allpass filter */
						//int m;
						float[] Phi_Fract = new float[2];

						/* fetch parameters */
						if(gr<this.num_hybrid_groups) {
							/* select data from the hybrid subbands */
							tmp0[0] = this.delay_SubQmf[temp_delay][sb][0];
							tmp0[1] = this.delay_SubQmf[temp_delay][sb][1];

							this.delay_SubQmf[temp_delay][sb][0] = inputLeft[0];
							this.delay_SubQmf[temp_delay][sb][1] = inputLeft[1];

							Phi_Fract[0] = Phi_Fract_SubQmf[sb][0];
							Phi_Fract[1] = Phi_Fract_SubQmf[sb][1];
						}
						else {
							/* select data from the QMF subbands */
							tmp0[0] = this.delay_Qmf[temp_delay][sb][0];
							tmp0[1] = this.delay_Qmf[temp_delay][sb][1];

							this.delay_Qmf[temp_delay][sb][0] = inputLeft[0];
							this.delay_Qmf[temp_delay][sb][1] = inputLeft[1];

							Phi_Fract[0] = Phi_Fract_Qmf[sb][0];
							Phi_Fract[1] = Phi_Fract_Qmf[sb][1];
						}

						/* z^(-2) * Phi_Fract[k] */
						tmp[0] = (tmp[0]*Phi_Fract[0])+(tmp0[1]*Phi_Fract[1]);
						tmp[1] = (tmp0[1]*Phi_Fract[0])-(tmp0[0]*Phi_Fract[1]);

						R0[0] = tmp[0];
						R0[1] = tmp[1];
						for(m = 0; m<NO_ALLPASS_LINKS; m++) {
							float[] Q_Fract_allpass = new float[2], tmp2 = new float[2];

							/* fetch parameters */
							if(gr<this.num_hybrid_groups) {
								/* select data from the hybrid subbands */
								tmp0[0] = this.delay_SubQmf_ser[m][temp_delay_ser[m]][sb][0];
								tmp0[1] = this.delay_SubQmf_ser[m][temp_delay_ser[m]][sb][1];

								if(this.use34hybrid_bands) {
									Q_Fract_allpass[0] = Q_Fract_allpass_SubQmf34[sb][m][0];
									Q_Fract_allpass[1] = Q_Fract_allpass_SubQmf34[sb][m][1];
								}
								else {
									Q_Fract_allpass[0] = Q_Fract_allpass_SubQmf20[sb][m][0];
									Q_Fract_allpass[1] = Q_Fract_allpass_SubQmf20[sb][m][1];
								}
							}
							else {
								/* select data from the QMF subbands */
								tmp0[0] = this.delay_Qmf_ser[m][temp_delay_ser[m]][sb][0];
								tmp0[1] = this.delay_Qmf_ser[m][temp_delay_ser[m]][sb][1];

								Q_Fract_allpass[0] = Q_Fract_allpass_Qmf[sb][m][0];
								Q_Fract_allpass[1] = Q_Fract_allpass_Qmf[sb][m][1];
							}

							/* delay by a fraction */
							/* z^(-d(m)) * Q_Fract_allpass[k,m] */
							tmp[0] = (tmp0[0]*Q_Fract_allpass[0])+(tmp0[1]*Q_Fract_allpass[1]);
							tmp[1] = (tmp0[1]*Q_Fract_allpass[0])-(tmp0[0]*Q_Fract_allpass[1]);

							/* -a(m) * g_DecaySlope[k] */
							tmp[0] += -(g_DecaySlope_filt[m]*R0[0]);
							tmp[1] += -(g_DecaySlope_filt[m]*R0[1]);

							/* -a(m) * g_DecaySlope[k] * Q_Fract_allpass[k,m] * z^(-d(m)) */
							tmp2[0] = R0[0]+(g_DecaySlope_filt[m]*tmp[0]);
							tmp2[1] = R0[1]+(g_DecaySlope_filt[m]*tmp[1]);

							/* store sample */
							if(gr<this.num_hybrid_groups) {
								this.delay_SubQmf_ser[m][temp_delay_ser[m]][sb][0] = tmp2[0];
								this.delay_SubQmf_ser[m][temp_delay_ser[m]][sb][1] = tmp2[1];
							}
							else {
								this.delay_Qmf_ser[m][temp_delay_ser[m]][sb][0] = tmp2[0];
								this.delay_Qmf_ser[m][temp_delay_ser[m]][sb][1] = tmp2[1];
							}

							/* store for next iteration (or as output value if last iteration) */
							R0[0] = tmp[0];
							R0[1] = tmp[1];
						}
					}

					/* select b(k) for reading the transient ratio */
					bk = (~NEGATE_IPD_MASK)&this.map_group2bk[gr];

					/* duck if a past transient is found */
					R0[0] = (G_TransientRatio[n][bk]*R0[0]);
					R0[1] = (G_TransientRatio[n][bk]*R0[1]);

					if(gr<this.num_hybrid_groups) {
						/* hybrid */
						X_hybrid_right[n][sb][0] = R0[0];
						X_hybrid_right[n][sb][1] = R0[1];
					}
					else {
						/* QMF */
						X_right[n][sb][0] = R0[0];
						X_right[n][sb][1] = R0[1];
					}

					/* Update delay buffer index */
					if(++temp_delay>=2) {
						temp_delay = 0;
					}

					/* update delay indices */
					if(sb>this.nr_allpass_bands&&gr>=this.num_hybrid_groups) {
						/* delay_D depends on the samplerate, it can hold the values 14 and 1 */
						if(++this.delay_buf_index_delay[sb]>=this.delay_D[sb]) {
							this.delay_buf_index_delay[sb] = 0;
						}
					}

					for(m = 0; m<NO_ALLPASS_LINKS; m++) {
						if(++temp_delay_ser[m]>=this.num_sample_delay_ser[m]) {
							temp_delay_ser[m] = 0;
						}
					}
				}
			}
		}

		/* update delay indices */
		this.saved_delay = temp_delay;
		for(m = 0; m<NO_ALLPASS_LINKS; m++) {
			this.delay_buf_index_ser[m] = temp_delay_ser[m];
		}
	}

	private float magnitude_c(float[] c) {
		return (float) Math.sqrt(c[0]*c[0]+c[1]*c[1]);
	}

	private void ps_mix_phase(float[][][] X_left, float[][][] X_right,
		float[][][] X_hybrid_left, float[][][] X_hybrid_right) {
		int n;
		int gr;
		int bk = 0;
		int sb, maxsb;
		int env;
		int nr_ipdopd_par;
		float[] h11 = new float[2], h12 = new float[2], h21 = new float[2], h22 = new float[2];
		float[] H11 = new float[2], H12 = new float[2], H21 = new float[2], H22 = new float[2];
		float[] deltaH11 = new float[2], deltaH12 = new float[2], deltaH21 = new float[2], deltaH22 = new float[2];
		float[] tempLeft = new float[2];
		float[] tempRight = new float[2];
		float[] phaseLeft = new float[2];
		float[] phaseRight = new float[2];
		float L;
		float[] sf_iid;
		int no_iid_steps;

		if(this.iid_mode>=3) {
			no_iid_steps = 15;
			sf_iid = sf_iid_fine;
		}
		else {
			no_iid_steps = 7;
			sf_iid = sf_iid_normal;
		}

		if(this.ipd_mode==0||this.ipd_mode==3) {
			nr_ipdopd_par = 11; /* resolution */

		}
		else {
			nr_ipdopd_par = this.nr_ipdopd_par;
		}

		for(gr = 0; gr<this.num_groups; gr++) {
			bk = (~NEGATE_IPD_MASK)&this.map_group2bk[gr];

			/* use one channel per group in the subqmf domain */
			maxsb = (gr<this.num_hybrid_groups) ? this.group_border[gr]+1 : this.group_border[gr+1];

			for(env = 0; env<this.num_env; env++) {
				if(this.icc_mode<3) {
					/* type 'A' mixing as described in 8.6.4.6.2.1 */
					float c_1, c_2;
					float cosa, sina;
					float cosb, sinb;
					float ab1, ab2;
					float ab3, ab4;

					/*
					 c_1 = sqrt(2.0 / (1.0 + pow(10.0, quant_iid[no_iid_steps + iid_index] / 10.0)));
					 c_2 = sqrt(2.0 / (1.0 + pow(10.0, quant_iid[no_iid_steps - iid_index] / 10.0)));
					 alpha = 0.5 * acos(quant_rho[icc_index]);
					 beta = alpha * ( c_1 - c_2 ) / sqrt(2.0);
					 */
					//printf("%d\n", ps.iid_index[env][bk]);

					/* calculate the scalefactors c_1 and c_2 from the intensity differences */
					c_1 = sf_iid[no_iid_steps+this.iid_index[env][bk]];
					c_2 = sf_iid[no_iid_steps-this.iid_index[env][bk]];

					/* calculate alpha and beta using the ICC parameters */
					cosa = cos_alphas[this.icc_index[env][bk]];
					sina = sin_alphas[this.icc_index[env][bk]];

					if(this.iid_mode>=3) {
						if(this.iid_index[env][bk]<0) {
							cosb = cos_betas_fine[-this.iid_index[env][bk]][this.icc_index[env][bk]];
							sinb = -sin_betas_fine[-this.iid_index[env][bk]][this.icc_index[env][bk]];
						}
						else {
							cosb = cos_betas_fine[this.iid_index[env][bk]][this.icc_index[env][bk]];
							sinb = sin_betas_fine[this.iid_index[env][bk]][this.icc_index[env][bk]];
						}
					}
					else {
						if(this.iid_index[env][bk]<0) {
							cosb = cos_betas_normal[-this.iid_index[env][bk]][this.icc_index[env][bk]];
							sinb = -sin_betas_normal[-this.iid_index[env][bk]][this.icc_index[env][bk]];
						}
						else {
							cosb = cos_betas_normal[this.iid_index[env][bk]][this.icc_index[env][bk]];
							sinb = sin_betas_normal[this.iid_index[env][bk]][this.icc_index[env][bk]];
						}
					}

					ab1 = (cosb*cosa);
					ab2 = (sinb*sina);
					ab3 = (sinb*cosa);
					ab4 = (cosb*sina);

					/* h_xy: COEF */
					h11[0] = (c_2*(ab1-ab2));
					h12[0] = (c_1*(ab1+ab2));
					h21[0] = (c_2*(ab3+ab4));
					h22[0] = (c_1*(ab3-ab4));
				}
				else {
					/* type 'B' mixing as described in 8.6.4.6.2.2 */
					float sina, cosa;
					float cosg, sing;

					if(this.iid_mode>=3) {
						int abs_iid = Math.abs(this.iid_index[env][bk]);

						cosa = sincos_alphas_B_fine[no_iid_steps+this.iid_index[env][bk]][this.icc_index[env][bk]];
						sina = sincos_alphas_B_fine[30-(no_iid_steps+this.iid_index[env][bk])][this.icc_index[env][bk]];
						cosg = cos_gammas_fine[abs_iid][this.icc_index[env][bk]];
						sing = sin_gammas_fine[abs_iid][this.icc_index[env][bk]];
					}
					else {
						int abs_iid = Math.abs(this.iid_index[env][bk]);

						cosa = sincos_alphas_B_normal[no_iid_steps+this.iid_index[env][bk]][this.icc_index[env][bk]];
						sina = sincos_alphas_B_normal[14-(no_iid_steps+this.iid_index[env][bk])][this.icc_index[env][bk]];
						cosg = cos_gammas_normal[abs_iid][this.icc_index[env][bk]];
						sing = sin_gammas_normal[abs_iid][this.icc_index[env][bk]];
					}

					h11[0] = (COEF_SQRT2*(cosa*cosg));
					h12[0] = (COEF_SQRT2*(sina*cosg));
					h21[0] = (COEF_SQRT2*(-cosa*sing));
					h22[0] = (COEF_SQRT2*(sina*sing));
				}

				/* calculate phase rotation parameters H_xy */
				/* note that the imaginary part of these parameters are only calculated when
				 IPD and OPD are enabled
				 */
				if((this.enable_ipdopd)&&(bk<nr_ipdopd_par)) {
					float xy, pq, xypq;

					/* ringbuffer index */
					int i = this.phase_hist;

					/* previous value */
					tempLeft[0] = (this.ipd_prev[bk][i][0]*0.25f);
					tempLeft[1] = (this.ipd_prev[bk][i][1]*0.25f);
					tempRight[0] = (this.opd_prev[bk][i][0]*0.25f);
					tempRight[1] = (this.opd_prev[bk][i][1]*0.25f);

					/* save current value */
					this.ipd_prev[bk][i][0] = ipdopd_cos_tab[Math.abs(this.ipd_index[env][bk])];
					this.ipd_prev[bk][i][1] = ipdopd_sin_tab[Math.abs(this.ipd_index[env][bk])];
					this.opd_prev[bk][i][0] = ipdopd_cos_tab[Math.abs(this.opd_index[env][bk])];
					this.opd_prev[bk][i][1] = ipdopd_sin_tab[Math.abs(this.opd_index[env][bk])];

					/* add current value */
					tempLeft[0] += this.ipd_prev[bk][i][0];
					tempLeft[1] += this.ipd_prev[bk][i][1];
					tempRight[0] += this.opd_prev[bk][i][0];
					tempRight[1] += this.opd_prev[bk][i][1];

					/* ringbuffer index */
					if(i==0) {
						i = 2;
					}
					i--;

					/* get value before previous */
					tempLeft[0] += (this.ipd_prev[bk][i][0]*0.5f);
					tempLeft[1] += (this.ipd_prev[bk][i][1]*0.5f);
					tempRight[0] += (this.opd_prev[bk][i][0]*0.5f);
					tempRight[1] += (this.opd_prev[bk][i][1]*0.5f);

					xy = magnitude_c(tempRight);
					pq = magnitude_c(tempLeft);

					if(xy!=0) {
						phaseLeft[0] = (tempRight[0]/xy);
						phaseLeft[1] = (tempRight[1]/xy);
					}
					else {
						phaseLeft[0] = 0;
						phaseLeft[1] = 0;
					}

					xypq = (xy*pq);

					if(xypq!=0) {
						float tmp1 = (tempRight[0]*tempLeft[0])+(tempRight[1]*tempLeft[1]);
						float tmp2 = (tempRight[1]*tempLeft[0])-(tempRight[0]*tempLeft[1]);

						phaseRight[0] = (tmp1/xypq);
						phaseRight[1] = (tmp2/xypq);
					}
					else {
						phaseRight[0] = 0;
						phaseRight[1] = 0;
					}

					/* MUL_F(COEF, REAL) = COEF */
					h11[1] = (h11[0]*phaseLeft[1]);
					h12[1] = (h12[0]*phaseRight[1]);
					h21[1] = (h21[0]*phaseLeft[1]);
					h22[1] = (h22[0]*phaseRight[1]);

					h11[0] = (h11[0]*phaseLeft[0]);
					h12[0] = (h12[0]*phaseRight[0]);
					h21[0] = (h21[0]*phaseLeft[0]);
					h22[0] = (h22[0]*phaseRight[0]);
				}

				/* length of the envelope n_e+1 - n_e (in time samples) */
				/* 0 < L <= 32: integer */
				L = (float) (this.border_position[env+1]-this.border_position[env]);

				/* obtain final H_xy by means of linear interpolation */
				deltaH11[0] = (h11[0]-this.h11_prev[gr][0])/L;
				deltaH12[0] = (h12[0]-this.h12_prev[gr][0])/L;
				deltaH21[0] = (h21[0]-this.h21_prev[gr][0])/L;
				deltaH22[0] = (h22[0]-this.h22_prev[gr][0])/L;

				H11[0] = this.h11_prev[gr][0];
				H12[0] = this.h12_prev[gr][0];
				H21[0] = this.h21_prev[gr][0];
				H22[0] = this.h22_prev[gr][0];

				this.h11_prev[gr][0] = h11[0];
				this.h12_prev[gr][0] = h12[0];
				this.h21_prev[gr][0] = h21[0];
				this.h22_prev[gr][0] = h22[0];

				/* only calculate imaginary part when needed */
				if((this.enable_ipdopd)&&(bk<nr_ipdopd_par)) {
					/* obtain final H_xy by means of linear interpolation */
					deltaH11[1] = (h11[1]-this.h11_prev[gr][1])/L;
					deltaH12[1] = (h12[1]-this.h12_prev[gr][1])/L;
					deltaH21[1] = (h21[1]-this.h21_prev[gr][1])/L;
					deltaH22[1] = (h22[1]-this.h22_prev[gr][1])/L;

					H11[1] = this.h11_prev[gr][1];
					H12[1] = this.h12_prev[gr][1];
					H21[1] = this.h21_prev[gr][1];
					H22[1] = this.h22_prev[gr][1];

					if((NEGATE_IPD_MASK&this.map_group2bk[gr])!=0) {
						deltaH11[1] = -deltaH11[1];
						deltaH12[1] = -deltaH12[1];
						deltaH21[1] = -deltaH21[1];
						deltaH22[1] = -deltaH22[1];

						H11[1] = -H11[1];
						H12[1] = -H12[1];
						H21[1] = -H21[1];
						H22[1] = -H22[1];
					}

					this.h11_prev[gr][1] = h11[1];
					this.h12_prev[gr][1] = h12[1];
					this.h21_prev[gr][1] = h21[1];
					this.h22_prev[gr][1] = h22[1];
				}

				/* apply H_xy to the current envelope band of the decorrelated subband */
				for(n = this.border_position[env]; n<this.border_position[env+1]; n++) {
					/* addition finalises the interpolation over every n */
					H11[0] += deltaH11[0];
					H12[0] += deltaH12[0];
					H21[0] += deltaH21[0];
					H22[0] += deltaH22[0];
					if((this.enable_ipdopd)&&(bk<nr_ipdopd_par)) {
						H11[1] += deltaH11[1];
						H12[1] += deltaH12[1];
						H21[1] += deltaH21[1];
						H22[1] += deltaH22[1];
					}

					/* channel is an alias to the subband */
					for(sb = this.group_border[gr]; sb<maxsb; sb++) {
						float[] inLeft = new float[2], inRight = new float[2];

						/* load decorrelated samples */
						if(gr<this.num_hybrid_groups) {
							inLeft[0] = X_hybrid_left[n][sb][0];
							inLeft[1] = X_hybrid_left[n][sb][1];
							inRight[0] = X_hybrid_right[n][sb][0];
							inRight[1] = X_hybrid_right[n][sb][1];
						}
						else {
							inLeft[0] = X_left[n][sb][0];
							inLeft[1] = X_left[n][sb][1];
							inRight[0] = X_right[n][sb][0];
							inRight[1] = X_right[n][sb][1];
						}

						/* apply mixing */
						tempLeft[0] = (H11[0]*inLeft[0])+(H21[0]*inRight[0]);
						tempLeft[1] = (H11[0]*inLeft[1])+(H21[0]*inRight[1]);
						tempRight[0] = (H12[0]*inLeft[0])+(H22[0]*inRight[0]);
						tempRight[1] = (H12[0]*inLeft[1])+(H22[0]*inRight[1]);

						/* only perform imaginary operations when needed */
						if((this.enable_ipdopd)&&(bk<nr_ipdopd_par)) {
							/* apply rotation */
							tempLeft[0] -= (H11[1]*inLeft[1])+(H21[1]*inRight[1]);
							tempLeft[1] += (H11[1]*inLeft[0])+(H21[1]*inRight[0]);
							tempRight[0] -= (H12[1]*inLeft[1])+(H22[1]*inRight[1]);
							tempRight[1] += (H12[1]*inLeft[0])+(H22[1]*inRight[0]);
						}

						/* store final samples */
						if(gr<this.num_hybrid_groups) {
							X_hybrid_left[n][sb][0] = tempLeft[0];
							X_hybrid_left[n][sb][1] = tempLeft[1];
							X_hybrid_right[n][sb][0] = tempRight[0];
							X_hybrid_right[n][sb][1] = tempRight[1];
						}
						else {
							X_left[n][sb][0] = tempLeft[0];
							X_left[n][sb][1] = tempLeft[1];
							X_right[n][sb][0] = tempRight[0];
							X_right[n][sb][1] = tempRight[1];
						}
					}
				}

				/* shift phase smoother's circular buffer index */
				this.phase_hist++;
				if(this.phase_hist==2) {
					this.phase_hist = 0;
				}
			}
		}
	}

	/* main Parametric Stereo decoding function */
	public int process(float[][][] X_left, float[][][] X_right) {
		float[][][] X_hybrid_left = new float[32][32][2];
		float[][][] X_hybrid_right = new float[32][32][2];

		/* delta decoding of the bitstream data */
		ps_data_decode();

		/* set up some parameters depending on filterbank type */
		if(this.use34hybrid_bands) {
			this.group_border = group_border34;
			this.map_group2bk = map_group2bk34;
			this.num_groups = 32+18;
			this.num_hybrid_groups = 32;
			this.nr_par_bands = 34;
			this.decay_cutoff = 5;
		}
		else {
			this.group_border = group_border20;
			this.map_group2bk = map_group2bk20;
			this.num_groups = 10+12;
			this.num_hybrid_groups = 10;
			this.nr_par_bands = 20;
			this.decay_cutoff = 3;
		}

		/* Perform further analysis on the lowest subbands to get a higher
		 * frequency resolution
		 */
		hyb.hybrid_analysis(X_left, X_hybrid_left,
			this.use34hybrid_bands, this.numTimeSlotsRate);

		/* decorrelate mono signal */
		ps_decorrelate(X_left, X_right, X_hybrid_left, X_hybrid_right);

		/* apply mixing and phase parameters */
		ps_mix_phase(X_left, X_right, X_hybrid_left, X_hybrid_right);

		/* hybrid synthesis, to rebuild the SBR QMF matrices */
		hyb.hybrid_synthesis(X_left, X_hybrid_left,
			this.use34hybrid_bands, this.numTimeSlotsRate);

		hyb.hybrid_synthesis(X_right, X_hybrid_right,
			this.use34hybrid_bands, this.numTimeSlotsRate);

		return 0;
	}

}
