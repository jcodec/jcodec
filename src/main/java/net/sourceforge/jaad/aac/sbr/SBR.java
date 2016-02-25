package net.sourceforge.jaad.aac.sbr;

import java.util.Arrays;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.ps.PS;
import net.sourceforge.jaad.aac.syntax.IBitStream;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
public class SBR implements SBRConstants, net.sourceforge.jaad.aac.syntax.SyntaxConstants, HuffmanTables {

	private final boolean downSampledSBR;
	final SampleFrequency sample_rate;
	int maxAACLine;

	int rate;
	boolean just_seeked;
	int ret;

	boolean[] amp_res;

	int k0;
	int kx;
	int M;
	int N_master;
	int N_high;
	int N_low;
	int N_Q;
	int[] N_L;
	int[] n;

	int[] f_master;
	int[][] f_table_res;
	int[] f_table_noise;
	int[][] f_table_lim;

	int[] table_map_k_to_g;

	int[] abs_bord_lead;
	int[] abs_bord_trail;
	int[] n_rel_lead;
	int[] n_rel_trail;

	int[] L_E;
	int[] L_E_prev;
	int[] L_Q;

	int[][] t_E;
	int[][] t_Q;
	int[][] f;
	int[] f_prev;

	float[][][] G_temp_prev;
	float[][][] Q_temp_prev;
	int[] GQ_ringbuf_index;

	int[][][] E;
	int[][] E_prev;
	float[][][] E_orig;
	float[][][] E_curr;
	int[][][] Q;
	float[][][] Q_div;
	float[][][] Q_div2;
	int[][] Q_prev;

	int[] l_A;
	int[] l_A_prev;

	int[][] bs_invf_mode;
	int[][] bs_invf_mode_prev;
	float[][] bwArray;
	float[][] bwArray_prev;

	int noPatches;
	int[] patchNoSubbands;
	int[] patchStartSubband;

	int[][] bs_add_harmonic;
	int[][] bs_add_harmonic_prev;

	int[] index_noise_prev;
	int[] psi_is_prev;

	int bs_start_freq_prev;
	int bs_stop_freq_prev;
	int bs_xover_band_prev;
	int bs_freq_scale_prev;
	boolean bs_alter_scale_prev;
	int bs_noise_bands_prev;

	int[] prevEnvIsShort;

	int kx_prev;
	int bsco;
	int bsco_prev;
	int M_prev;

	boolean Reset;
	int frame;
	int header_count;

	boolean stereo;
	AnalysisFilterbank[] qmfa;
	SynthesisFilterbank[] qmfs;

	float[][][][] Xsbr;

	int numTimeSlotsRate;
	int numTimeSlots;
	int tHFGen;
	int tHFAdj;

	PS ps;
	boolean ps_used;
	boolean psResetFlag;

	/* to get it compiling */
	/* we'll see during the coding of all the tools, whether
	 these are all used or not.
	 */
	boolean bs_header_flag;
	int bs_crc_flag;
	int bs_sbr_crc_bits;
	int bs_protocol_version;
	boolean bs_amp_res;
	int bs_start_freq;
	int bs_stop_freq;
	int bs_xover_band;
	int bs_freq_scale;
	boolean bs_alter_scale;
	int bs_noise_bands;
	int bs_limiter_bands;
	int bs_limiter_gains;
	boolean bs_interpol_freq;
	boolean bs_smoothing_mode;
	int bs_samplerate_mode;
	boolean[] bs_add_harmonic_flag;
	boolean[] bs_add_harmonic_flag_prev;
	boolean bs_extended_data;
	int bs_extension_id;
	int bs_extension_data;
	boolean bs_coupling;
	int[] bs_frame_class;
	int[][] bs_rel_bord;
	int[][] bs_rel_bord_0;
	int[][] bs_rel_bord_1;
	int[] bs_pointer;
	int[] bs_abs_bord_0;
	int[] bs_abs_bord_1;
	int[] bs_num_rel_0;
	int[] bs_num_rel_1;
	int[][] bs_df_env;
	int[][] bs_df_noise;

	public SBR(boolean smallFrames, boolean stereo, SampleFrequency sample_rate, boolean downSampledSBR) {
        this.amp_res = new boolean[2];
        this.N_L = new int[4];
        this.n = new int[2];
        this.f_master = new int[64];
        this.f_table_res = new int[2][64];
        this.f_table_noise = new int[64];
        this.f_table_lim = new int[4][64];
        this.table_map_k_to_g = new int[64];
        this.abs_bord_lead = new int[2];
        this.abs_bord_trail = new int[2];
        this.n_rel_lead = new int[2];
        this.n_rel_trail = new int[2];
        this.L_E = new int[2];
        this.L_E_prev = new int[2];
        this.L_Q = new int[2];
        this.t_E = new int[2][MAX_L_E+1];
        this.t_Q = new int[2][3];
        this.f = new int[2][MAX_L_E+1];
        this.f_prev = new int[2];
        this.G_temp_prev = new float[2][5][64];
        this.Q_temp_prev = new float[2][5][64];
        this.GQ_ringbuf_index = new int[2];
        this.E = new int[2][64][MAX_L_E];
        this.E_prev = new int[2][64];
        this.E_orig = new float[2][64][MAX_L_E];
        this.E_curr = new float[2][64][MAX_L_E];
        this.Q = new int[2][64][2];
        this.Q_div = new float[2][64][2];
        this.Q_div2 = new float[2][64][2];
        this.Q_prev = new int[2][64];
        this.l_A = new int[2];
        this.l_A_prev = new int[2];
        this.bs_invf_mode = new int[2][MAX_L_E];
        this.bs_invf_mode_prev = new int[2][MAX_L_E];
        this.bwArray = new float[2][64];
        this.bwArray_prev = new float[2][64];
        this.patchNoSubbands = new int[64];
        this.patchStartSubband = new int[64];
        this.bs_add_harmonic = new int[2][64];
        this.bs_add_harmonic_prev = new int[2][64];
        this.index_noise_prev = new int[2];
        this.psi_is_prev = new int[2];
        this.prevEnvIsShort = new int[2];
        this.qmfa = new AnalysisFilterbank[2];
        this.qmfs = new SynthesisFilterbank[2];
        this.Xsbr = new float[2][MAX_NTSRHFG][64][2];
        this.bs_add_harmonic_flag = new boolean[2];
        this.bs_add_harmonic_flag_prev = new boolean[2];
        this.bs_frame_class = new int[2];
        this.bs_rel_bord = new int[2][9];
        this.bs_rel_bord_0 = new int[2][9];
        this.bs_rel_bord_1 = new int[2][9];
        this.bs_pointer = new int[2];
        this.bs_abs_bord_0 = new int[2];
        this.bs_abs_bord_1 = new int[2];
        this.bs_num_rel_0 = new int[2];
        this.bs_num_rel_1 = new int[2];
        this.bs_df_env = new int[2][9];
        this.bs_df_noise = new int[2][3];

	    
		this.downSampledSBR = downSampledSBR;
		this.stereo = stereo;
		this.sample_rate = sample_rate;

		this.bs_freq_scale = 2;
		this.bs_alter_scale = true;
		this.bs_noise_bands = 2;
		this.bs_limiter_bands = 2;
		this.bs_limiter_gains = 2;
		this.bs_interpol_freq = true;
		this.bs_smoothing_mode = true;
		this.bs_start_freq = 5;
		this.bs_amp_res = true;
		this.bs_samplerate_mode = 1;
		this.prevEnvIsShort[0] = -1;
		this.prevEnvIsShort[1] = -1;
		this.header_count = 0;
		this.Reset = true;

		this.tHFGen = T_HFGEN;
		this.tHFAdj = T_HFADJ;

		this.bsco = 0;
		this.bsco_prev = 0;
		this.M_prev = 0;

		/* force sbr reset */
		this.bs_start_freq_prev = -1;

		if(smallFrames) {
			this.numTimeSlotsRate = RATE*NO_TIME_SLOTS_960;
			this.numTimeSlots = NO_TIME_SLOTS_960;
		}
		else {
			this.numTimeSlotsRate = RATE*NO_TIME_SLOTS;
			this.numTimeSlots = NO_TIME_SLOTS;
		}

		this.GQ_ringbuf_index[0] = 0;
		this.GQ_ringbuf_index[1] = 0;

		if(stereo) {
			/* stereo */
			int j;
			this.qmfa[0] = new AnalysisFilterbank(32);
			this.qmfa[1] = new AnalysisFilterbank(32);
			this.qmfs[0] = new SynthesisFilterbank((downSampledSBR) ? 32 : 64);
			this.qmfs[1] = new SynthesisFilterbank((downSampledSBR) ? 32 : 64);
		}
		else {
			/* mono */
			this.qmfa[0] = new AnalysisFilterbank(32);
			this.qmfs[0] = new SynthesisFilterbank((downSampledSBR) ? 32 : 64);
			this.qmfs[1] = null;
		}
	}

	void sbrReset() {
		int j;
		if(this.qmfa[0]!=null) qmfa[0].reset();
		if(this.qmfa[1]!=null) qmfa[1].reset();
		if(this.qmfs[0]!=null) qmfs[0].reset();
		if(this.qmfs[1]!=null) qmfs[1].reset();

		for(j = 0; j<5; j++) {
			if(this.G_temp_prev[0][j]!=null) Arrays.fill(G_temp_prev[0][j], 0);
			if(this.G_temp_prev[1][j]!=null) Arrays.fill(G_temp_prev[1][j], 0);
			if(this.Q_temp_prev[0][j]!=null) Arrays.fill(Q_temp_prev[0][j], 0);
			if(this.Q_temp_prev[1][j]!=null) Arrays.fill(Q_temp_prev[1][j], 0);
		}

		for(int i = 0; i<40; i++) {
			for(int k = 0; k<64; k++) {
				Xsbr[0][i][j][0] = 0;
				Xsbr[0][i][j][1] = 0;
				Xsbr[1][i][j][0] = 0;
				Xsbr[1][i][j][1] = 0;
			}
		}

		this.GQ_ringbuf_index[0] = 0;
		this.GQ_ringbuf_index[1] = 0;
		this.header_count = 0;
		this.Reset = true;

		this.L_E_prev[0] = 0;
		this.L_E_prev[1] = 0;
		this.bs_freq_scale = 2;
		this.bs_alter_scale = true;
		this.bs_noise_bands = 2;
		this.bs_limiter_bands = 2;
		this.bs_limiter_gains = 2;
		this.bs_interpol_freq = true;
		this.bs_smoothing_mode = true;
		this.bs_start_freq = 5;
		this.bs_amp_res = true;
		this.bs_samplerate_mode = 1;
		this.prevEnvIsShort[0] = -1;
		this.prevEnvIsShort[1] = -1;
		this.bsco = 0;
		this.bsco_prev = 0;
		this.M_prev = 0;
		this.bs_start_freq_prev = -1;

		this.f_prev[0] = 0;
		this.f_prev[1] = 0;
		for(j = 0; j<MAX_M; j++) {
			this.E_prev[0][j] = 0;
			this.Q_prev[0][j] = 0;
			this.E_prev[1][j] = 0;
			this.Q_prev[1][j] = 0;
			this.bs_add_harmonic_prev[0][j] = 0;
			this.bs_add_harmonic_prev[1][j] = 0;
		}
		this.bs_add_harmonic_flag_prev[0] = false;
		this.bs_add_harmonic_flag_prev[1] = false;
	}

	void sbr_reset() {

		/* if these are different from the previous frame: Reset = 1 */
		if((this.bs_start_freq!=this.bs_start_freq_prev)
			||(this.bs_stop_freq!=this.bs_stop_freq_prev)
			||(this.bs_freq_scale!=this.bs_freq_scale_prev)
			||(this.bs_alter_scale!=this.bs_alter_scale_prev)
			||(this.bs_xover_band!=this.bs_xover_band_prev)
			||(this.bs_noise_bands!=this.bs_noise_bands_prev)) {
			this.Reset = true;
		}
		else {
			this.Reset = false;
		}

		this.bs_start_freq_prev = this.bs_start_freq;
		this.bs_stop_freq_prev = this.bs_stop_freq;
		this.bs_freq_scale_prev = this.bs_freq_scale;
		this.bs_alter_scale_prev = this.bs_alter_scale;
		this.bs_xover_band_prev = this.bs_xover_band;
		this.bs_noise_bands_prev = this.bs_noise_bands;
	}

	int calc_sbr_tables(int start_freq, int stop_freq,
		int samplerate_mode, int freq_scale,
		boolean alter_scale, int xover_band) {
		int result = 0;
		int k2;

		/* calculate the Master Frequency Table */
		this.k0 = FBT.qmf_start_channel(start_freq, samplerate_mode, this.sample_rate);
		k2 = FBT.qmf_stop_channel(stop_freq, this.sample_rate, this.k0);

		/* check k0 and k2 */
		if(this.sample_rate.getFrequency()>=48000) {
			if((k2-this.k0)>32)
				result += 1;
		}
		else if(this.sample_rate.getFrequency()<=32000) {
			if((k2-this.k0)>48)
				result += 1;
		}
		else { /* (sbr.sample_rate == 44100) */

			if((k2-this.k0)>45)
				result += 1;
		}

		if(freq_scale==0) {
			result += FBT.master_frequency_table_fs0(this, this.k0, k2, alter_scale);
		}
		else {
			result += FBT.master_frequency_table(this, this.k0, k2, freq_scale, alter_scale);
		}
		result += FBT.derived_frequency_table(this, xover_band, k2);

		result = (result>0) ? 1 : 0;

		return result;
	}

	/* table 2 */
	public int decode(IBitStream ld, int cnt) throws AACException {
		int result = 0;
		int num_align_bits = 0;
		long num_sbr_bits1 = ld.getPosition();
		int num_sbr_bits2;

		int saved_start_freq, saved_samplerate_mode;
		int saved_stop_freq, saved_freq_scale;
		int saved_xover_band;
		boolean saved_alter_scale;

		int bs_extension_type = ld.readBits(4);

		if(bs_extension_type==EXT_SBR_DATA_CRC) {
			this.bs_sbr_crc_bits = ld.readBits(10);
		}

		/* save old header values, in case the new ones are corrupted */
		saved_start_freq = this.bs_start_freq;
		saved_samplerate_mode = this.bs_samplerate_mode;
		saved_stop_freq = this.bs_stop_freq;
		saved_freq_scale = this.bs_freq_scale;
		saved_alter_scale = this.bs_alter_scale;
		saved_xover_band = this.bs_xover_band;

		this.bs_header_flag = ld.readBool();

		if(this.bs_header_flag)
			sbr_header(ld);

		/* Reset? */
		sbr_reset();

		/* first frame should have a header */
		//if (!(sbr.frame == 0 && sbr.bs_header_flag == 0))
		if(this.header_count!=0) {
			if(this.Reset||(this.bs_header_flag&&this.just_seeked)) {
				int rt = calc_sbr_tables(this.bs_start_freq, this.bs_stop_freq,
					this.bs_samplerate_mode, this.bs_freq_scale,
					this.bs_alter_scale, this.bs_xover_band);

				/* if an error occured with the new header values revert to the old ones */
				if(rt>0) {
					calc_sbr_tables(saved_start_freq, saved_stop_freq,
						saved_samplerate_mode, saved_freq_scale,
						saved_alter_scale, saved_xover_band);
				}
			}

			if(result==0) {
				result = sbr_data(ld);

				/* sbr_data() returning an error means that there was an error in
				 envelope_time_border_vector().
				 In this case the old time border vector is saved and all the previous
				 data normally read after sbr_grid() is saved.
				 */
				/* to be on the safe side, calculate old sbr tables in case of error */
				if((result>0)
					&&(this.Reset||(this.bs_header_flag&&this.just_seeked))) {
					calc_sbr_tables(saved_start_freq, saved_stop_freq,
						saved_samplerate_mode, saved_freq_scale,
						saved_alter_scale, saved_xover_band);
				}

				/* we should be able to safely set result to 0 now, */
				/* but practise indicates this doesn't work well */
			}
		}
		else {
			result = 1;
		}

		num_sbr_bits2 = (int) (ld.getPosition()-num_sbr_bits1);

		/* check if we read more bits then were available for sbr */
		if(8*cnt<num_sbr_bits2) {
			throw new AACException("frame overread");
			//faad_resetbits(ld, num_sbr_bits1+8*cnt);
			//num_sbr_bits2 = 8*cnt;

			/* turn off PS for the unfortunate case that we randomly read some
			 * PS data that looks correct */
			//this.ps_used = 0;

			/* Make sure it doesn't decode SBR in this frame, or we'll get glitches */
			//return 1;
		}

		{
			/* -4 does not apply, bs_extension_type is re-read in this function */
			num_align_bits = 8*cnt /*- 4*/-num_sbr_bits2;

			while(num_align_bits>7) {
				ld.readBits(8);
				num_align_bits -= 8;
			}
			ld.readBits(num_align_bits);
		}

		return result;
	}

	/* table 3 */
	private void sbr_header(IBitStream ld) throws AACException {
		boolean bs_header_extra_1, bs_header_extra_2;

		this.header_count++;

		this.bs_amp_res = ld.readBool();

		/* bs_start_freq and bs_stop_freq must define a fequency band that does
		 not exceed 48 channels */
		this.bs_start_freq = ld.readBits(4);
		this.bs_stop_freq = ld.readBits(4);
		this.bs_xover_band = ld.readBits(3);
		ld.readBits(2); //reserved
		bs_header_extra_1 = ld.readBool();
		bs_header_extra_2 = ld.readBool();

		if(bs_header_extra_1) {
			this.bs_freq_scale = ld.readBits(2);
			this.bs_alter_scale = ld.readBool();
			this.bs_noise_bands = ld.readBits(2);
		}
		else {
			/* Default values */
			this.bs_freq_scale = 2;
			this.bs_alter_scale = true;
			this.bs_noise_bands = 2;
		}

		if(bs_header_extra_2) {
			this.bs_limiter_bands = ld.readBits(2);
			this.bs_limiter_gains = ld.readBits(2);
			this.bs_interpol_freq = ld.readBool();
			this.bs_smoothing_mode = ld.readBool();
		}
		else {
			/* Default values */
			this.bs_limiter_bands = 2;
			this.bs_limiter_gains = 2;
			this.bs_interpol_freq = true;
			this.bs_smoothing_mode = true;
		}

	}

	/* table 4 */
	private int sbr_data(IBitStream ld) throws AACException {
		int result;

		this.rate = (this.bs_samplerate_mode!=0) ? 2 : 1;

		if(stereo) {
			if((result = sbr_channel_pair_element(ld))>0)
				return result;
		}
		else {
			if((result = sbr_single_channel_element(ld))>0)
				return result;
		}

		return 0;
	}

	/* table 5 */
	private int sbr_single_channel_element(IBitStream ld) throws AACException {
		int result;

		if(ld.readBool()) {
			ld.readBits(4); //reserved
		}

		if((result = sbr_grid(ld, 0))>0)
			return result;

		sbr_dtdf(ld, 0);
		invf_mode(ld, 0);
		sbr_envelope(ld, 0);
		sbr_noise(ld, 0);

		NoiseEnvelope.dequantChannel(this, 0);

		Arrays.fill(bs_add_harmonic[0], 0, 64, 0);
		Arrays.fill(bs_add_harmonic[1], 0, 64, 0);

		this.bs_add_harmonic_flag[0] = ld.readBool();
		if(this.bs_add_harmonic_flag[0])
			sinusoidal_coding(ld, 0);

		this.bs_extended_data = ld.readBool();

		if(this.bs_extended_data) {
			int nr_bits_left;
			int ps_ext_read = 0;
			int cnt = ld.readBits(4);
			if(cnt==15) {
				cnt += ld.readBits(8);
			}

			nr_bits_left = 8*cnt;
			while(nr_bits_left>7) {
				int tmp_nr_bits = 0;

				this.bs_extension_id = ld.readBits(2);
				tmp_nr_bits += 2;

				/* allow only 1 PS extension element per extension data */
				if(this.bs_extension_id==EXTENSION_ID_PS) {
					if(ps_ext_read==0) {
						ps_ext_read = 1;
					}
					else {
						/* to be safe make it 3, will switch to "default"
						 * in sbr_extension() */
						this.bs_extension_id = 3;
					}
				}

				tmp_nr_bits += sbr_extension(ld, this.bs_extension_id, nr_bits_left);

				/* check if the data read is bigger than the number of available bits */
				if(tmp_nr_bits>nr_bits_left)
					return 1;

				nr_bits_left -= tmp_nr_bits;
			}

			/* Corrigendum */
			if(nr_bits_left>0) {
				ld.readBits(nr_bits_left);
			}
		}

		return 0;
	}

	/* table 6 */
	private int sbr_channel_pair_element(IBitStream ld) throws AACException {
		int n, result;

		if(ld.readBool()) {
			//reserved
			ld.readBits(4);
			ld.readBits(4);
		}

		this.bs_coupling = ld.readBool();

		if(this.bs_coupling) {
			if((result = sbr_grid(ld, 0))>0)
				return result;

			/* need to copy some data from left to right */
			this.bs_frame_class[1] = this.bs_frame_class[0];
			this.L_E[1] = this.L_E[0];
			this.L_Q[1] = this.L_Q[0];
			this.bs_pointer[1] = this.bs_pointer[0];

			for(n = 0; n<=this.L_E[0]; n++) {
				this.t_E[1][n] = this.t_E[0][n];
				this.f[1][n] = this.f[0][n];
			}
			for(n = 0; n<=this.L_Q[0]; n++) {
				this.t_Q[1][n] = this.t_Q[0][n];
			}

			sbr_dtdf(ld, 0);
			sbr_dtdf(ld, 1);
			invf_mode(ld, 0);

			/* more copying */
			for(n = 0; n<this.N_Q; n++) {
				this.bs_invf_mode[1][n] = this.bs_invf_mode[0][n];
			}

			sbr_envelope(ld, 0);
			sbr_noise(ld, 0);
			sbr_envelope(ld, 1);
			sbr_noise(ld, 1);

			Arrays.fill(bs_add_harmonic[0], 0, 64, 0);
			Arrays.fill(bs_add_harmonic[1], 0, 64, 0);

			this.bs_add_harmonic_flag[0] = ld.readBool();
			if(this.bs_add_harmonic_flag[0])
				sinusoidal_coding(ld, 0);

			this.bs_add_harmonic_flag[1] = ld.readBool();
			if(this.bs_add_harmonic_flag[1])
				sinusoidal_coding(ld, 1);
		}
		else {
			int[] saved_t_E = new int[6], saved_t_Q = new int[3];
			int saved_L_E = this.L_E[0];
			int saved_L_Q = this.L_Q[0];
			int saved_frame_class = this.bs_frame_class[0];

			for(n = 0; n<saved_L_E; n++) {
				saved_t_E[n] = this.t_E[0][n];
			}
			for(n = 0; n<saved_L_Q; n++) {
				saved_t_Q[n] = this.t_Q[0][n];
			}

			if((result = sbr_grid(ld, 0))>0)
				return result;
			if((result = sbr_grid(ld, 1))>0) {
				/* restore first channel data as well */
				this.bs_frame_class[0] = saved_frame_class;
				this.L_E[0] = saved_L_E;
				this.L_Q[0] = saved_L_Q;
				for(n = 0; n<6; n++) {
					this.t_E[0][n] = saved_t_E[n];
				}
				for(n = 0; n<3; n++) {
					this.t_Q[0][n] = saved_t_Q[n];
				}

				return result;
			}
			sbr_dtdf(ld, 0);
			sbr_dtdf(ld, 1);
			invf_mode(ld, 0);
			invf_mode(ld, 1);
			sbr_envelope(ld, 0);
			sbr_envelope(ld, 1);
			sbr_noise(ld, 0);
			sbr_noise(ld, 1);

			Arrays.fill(bs_add_harmonic[0], 0, 64, 0);
			Arrays.fill(bs_add_harmonic[1], 0, 64, 0);

			this.bs_add_harmonic_flag[0] = ld.readBool();
			if(this.bs_add_harmonic_flag[0])
				sinusoidal_coding(ld, 0);

			this.bs_add_harmonic_flag[1] = ld.readBool();
			if(this.bs_add_harmonic_flag[1])
				sinusoidal_coding(ld, 1);
		}
		NoiseEnvelope.dequantChannel(this, 0);
		NoiseEnvelope.dequantChannel(this, 1);

		if(this.bs_coupling)
			NoiseEnvelope.unmap(this);

		this.bs_extended_data = ld.readBool();
		if(this.bs_extended_data) {
			int nr_bits_left;
			int cnt = ld.readBits(4);
			if(cnt==15) {
				cnt += ld.readBits(8);
			}

			nr_bits_left = 8*cnt;
			while(nr_bits_left>7) {
				int tmp_nr_bits = 0;

				this.bs_extension_id = ld.readBits(2);
				tmp_nr_bits += 2;
				tmp_nr_bits += sbr_extension(ld, this.bs_extension_id, nr_bits_left);

				/* check if the data read is bigger than the number of available bits */
				if(tmp_nr_bits>nr_bits_left)
					return 1;

				nr_bits_left -= tmp_nr_bits;
			}

			/* Corrigendum */
			if(nr_bits_left>0) {
				ld.readBits(nr_bits_left);
			}
		}

		return 0;
	}

	/* integer log[2](x): input range [0,10) */
	private int sbr_log2(int val) {
		int log2tab[] = {0, 0, 1, 2, 2, 3, 3, 3, 3, 4};
		if(val<10&&val>=0)
			return log2tab[val];
		else
			return 0;
	}


	/* table 7 */
	private int sbr_grid(IBitStream ld, int ch) throws AACException {
		int i, env, rel, result;
		int bs_abs_bord, bs_abs_bord_1;
		int bs_num_env = 0;
		int saved_L_E = this.L_E[ch];
		int saved_L_Q = this.L_Q[ch];
		int saved_frame_class = this.bs_frame_class[ch];

		this.bs_frame_class[ch] = ld.readBits(2);

		switch(this.bs_frame_class[ch]) {
			case FIXFIX:
				i = ld.readBits(2);

				bs_num_env = Math.min(1<<i, 5);

				i = ld.readBit();
				for(env = 0; env<bs_num_env; env++) {
					this.f[ch][env] = i;
				}

				this.abs_bord_lead[ch] = 0;
				this.abs_bord_trail[ch] = this.numTimeSlots;
				this.n_rel_lead[ch] = bs_num_env-1;
				this.n_rel_trail[ch] = 0;
				break;

			case FIXVAR:
				bs_abs_bord = ld.readBits(2)+this.numTimeSlots;
				bs_num_env = ld.readBits(2)+1;

				for(rel = 0; rel<bs_num_env-1; rel++) {
					this.bs_rel_bord[ch][rel] = 2*ld.readBits(2)+2;
				}
				i = sbr_log2(bs_num_env+1);
				this.bs_pointer[ch] = ld.readBits(i);

				for(env = 0; env<bs_num_env; env++) {
					this.f[ch][bs_num_env-env-1] = ld.readBit();
				}

				this.abs_bord_lead[ch] = 0;
				this.abs_bord_trail[ch] = bs_abs_bord;
				this.n_rel_lead[ch] = 0;
				this.n_rel_trail[ch] = bs_num_env-1;
				break;

			case VARFIX:
				bs_abs_bord = ld.readBits(2);
				bs_num_env = ld.readBits(2)+1;

				for(rel = 0; rel<bs_num_env-1; rel++) {
					this.bs_rel_bord[ch][rel] = 2*ld.readBits(2)+2;
				}
				i = sbr_log2(bs_num_env+1);
				this.bs_pointer[ch] = ld.readBits(i);

				for(env = 0; env<bs_num_env; env++) {
					this.f[ch][env] = ld.readBit();
				}

				this.abs_bord_lead[ch] = bs_abs_bord;
				this.abs_bord_trail[ch] = this.numTimeSlots;
				this.n_rel_lead[ch] = bs_num_env-1;
				this.n_rel_trail[ch] = 0;
				break;

			case VARVAR:
				bs_abs_bord = ld.readBits(2);
				bs_abs_bord_1 = ld.readBits(2)+this.numTimeSlots;
				this.bs_num_rel_0[ch] = ld.readBits(2);
				this.bs_num_rel_1[ch] = ld.readBits(2);

				bs_num_env = Math.min(5, this.bs_num_rel_0[ch]+this.bs_num_rel_1[ch]+1);

				for(rel = 0; rel<this.bs_num_rel_0[ch]; rel++) {
					this.bs_rel_bord_0[ch][rel] = 2*ld.readBits(2)+2;
				}
				for(rel = 0; rel<this.bs_num_rel_1[ch]; rel++) {
					this.bs_rel_bord_1[ch][rel] = 2*ld.readBits(2)+2;
				}
				i = sbr_log2(this.bs_num_rel_0[ch]+this.bs_num_rel_1[ch]+2);
				this.bs_pointer[ch] = ld.readBits(i);

				for(env = 0; env<bs_num_env; env++) {
					this.f[ch][env] = ld.readBit();
				}

				this.abs_bord_lead[ch] = bs_abs_bord;
				this.abs_bord_trail[ch] = bs_abs_bord_1;
				this.n_rel_lead[ch] = this.bs_num_rel_0[ch];
				this.n_rel_trail[ch] = this.bs_num_rel_1[ch];
				break;
		}

		if(this.bs_frame_class[ch]==VARVAR)
			this.L_E[ch] = Math.min(bs_num_env, 5);
		else
			this.L_E[ch] = Math.min(bs_num_env, 4);

		if(this.L_E[ch]<=0)
			return 1;

		if(this.L_E[ch]>1)
			this.L_Q[ch] = 2;
		else
			this.L_Q[ch] = 1;

		/* TODO: this code can probably be integrated into the code above! */
		if((result = TFGrid.envelope_time_border_vector(this, ch))>0) {
			this.bs_frame_class[ch] = saved_frame_class;
			this.L_E[ch] = saved_L_E;
			this.L_Q[ch] = saved_L_Q;
			return result;
		}
		TFGrid.noise_floor_time_border_vector(this, ch);

		return 0;
	}

	/* table 8 */
	private void sbr_dtdf(IBitStream ld, int ch) throws AACException {
		int i;

		for(i = 0; i<this.L_E[ch]; i++) {
			this.bs_df_env[ch][i] = ld.readBit();
		}

		for(i = 0; i<this.L_Q[ch]; i++) {
			this.bs_df_noise[ch][i] = ld.readBit();
		}
	}

	/* table 9 */
	private void invf_mode(IBitStream ld, int ch) throws AACException {
		int n;

		for(n = 0; n<this.N_Q; n++) {
			this.bs_invf_mode[ch][n] = ld.readBits(2);
		}
	}

	private int sbr_extension(IBitStream ld, int bs_extension_id, int num_bits_left) throws AACException {
		int ret;

		switch(bs_extension_id) {
			case EXTENSION_ID_PS:
				if(ps==null) {
					this.ps = new PS(this.sample_rate, this.numTimeSlotsRate);
				}
				if(this.psResetFlag) {
					this.ps.header_read = false;
				}
				ret = ps.decode(ld);

				/* enable PS if and only if: a header has been decoded */
				if(!ps_used&&ps.header_read) {
					this.ps_used = true;
				}

				if(ps.header_read) {
					this.psResetFlag = false;
				}

				return ret;
			default:
				this.bs_extension_data = ld.readBits(6);
				return 6;
		}
	}

	/* table 12 */
	private void sinusoidal_coding(IBitStream ld, int ch) throws AACException {
		int n;

		for(n = 0; n<this.N_high; n++) {
			this.bs_add_harmonic[ch][n] = ld.readBit();
		}
	}
	/* table 10 */

	private void sbr_envelope(IBitStream ld, int ch) throws AACException {
		int env, band;
		int delta = 0;
		int[][] t_huff, f_huff;

		if((this.L_E[ch]==1)&&(this.bs_frame_class[ch]==FIXFIX))
			this.amp_res[ch] = false;
		else
			this.amp_res[ch] = this.bs_amp_res;

		if((this.bs_coupling)&&(ch==1)) {
			delta = 1;
			if(this.amp_res[ch]) {
				t_huff = T_HUFFMAN_ENV_BAL_3_0DB;
				f_huff = F_HUFFMAN_ENV_BAL_3_0DB;
			}
			else {
				t_huff = T_HUFFMAN_ENV_BAL_1_5DB;
				f_huff = F_HUFFMAN_ENV_BAL_1_5DB;
			}
		}
		else {
			delta = 0;
			if(this.amp_res[ch]) {
				t_huff = T_HUFFMAN_ENV_3_0DB;
				f_huff = F_HUFFMAN_ENV_3_0DB;
			}
			else {
				t_huff = T_HUFFMAN_ENV_1_5DB;
				f_huff = F_HUFFMAN_ENV_1_5DB;
			}
		}

		for(env = 0; env<this.L_E[ch]; env++) {
			if(this.bs_df_env[ch][env]==0) {
				if(this.bs_coupling&&(ch==1)) {
					if(this.amp_res[ch]) {
						this.E[ch][0][env] = ld.readBits(5)<<delta;
					}
					else {
						this.E[ch][0][env] = ld.readBits(6)<<delta;
					}
				}
				else {
					if(this.amp_res[ch]) {
						this.E[ch][0][env] = ld.readBits(6)<<delta;
					}
					else {
						this.E[ch][0][env] = ld.readBits(7)<<delta;
					}
				}

				for(band = 1; band<this.n[this.f[ch][env]]; band++) {
					this.E[ch][band][env] = (decodeHuffman(ld, f_huff)<<delta);
				}

			}
			else {
				for(band = 0; band<this.n[this.f[ch][env]]; band++) {
					this.E[ch][band][env] = (decodeHuffman(ld, t_huff)<<delta);
				}
			}
		}

		NoiseEnvelope.extract_envelope_data(this, ch);
	}

	/* table 11 */
	private void sbr_noise(IBitStream ld, int ch) throws AACException {
		int noise, band;
		int delta = 0;
		int[][] t_huff, f_huff;

		if(this.bs_coupling&&(ch==1)) {
			delta = 1;
			t_huff = T_HUFFMAN_NOISE_BAL_3_0DB;
			f_huff = F_HUFFMAN_ENV_BAL_3_0DB;
		}
		else {
			delta = 0;
			t_huff = T_HUFFMAN_NOISE_3_0DB;
			f_huff = F_HUFFMAN_ENV_3_0DB;
		}

		for(noise = 0; noise<this.L_Q[ch]; noise++) {
			if(this.bs_df_noise[ch][noise]==0) {
				if(this.bs_coupling&&(ch==1)) {
					this.Q[ch][0][noise] = ld.readBits(5)<<delta;
				}
				else {
					this.Q[ch][0][noise] = ld.readBits(5)<<delta;
				}
				for(band = 1; band<this.N_Q; band++) {
					this.Q[ch][band][noise] = (decodeHuffman(ld, f_huff)<<delta);
				}
			}
			else {
				for(band = 0; band<this.N_Q; band++) {
					this.Q[ch][band][noise] = (decodeHuffman(ld, t_huff)<<delta);
				}
			}
		}

		NoiseEnvelope.extract_noise_floor_data(this, ch);
	}

	private int decodeHuffman(IBitStream ld, int[][] t_huff) throws AACException {
		int bit;
		int index = 0;

		while(index>=0) {
			bit = ld.readBit();
			index = t_huff[index][bit];
		}

		return index+64;
	}

	private int sbr_save_prev_data(int ch) {
		int i;

		/* save data for next frame */
		this.kx_prev = this.kx;
		this.M_prev = this.M;
		this.bsco_prev = this.bsco;

		this.L_E_prev[ch] = this.L_E[ch];

		/* sbr.L_E[ch] can become 0 on files with bit errors */
		if(this.L_E[ch]<=0)
			return 19;

		this.f_prev[ch] = this.f[ch][this.L_E[ch]-1];
		for(i = 0; i<MAX_M; i++) {
			this.E_prev[ch][i] = this.E[ch][i][this.L_E[ch]-1];
			this.Q_prev[ch][i] = this.Q[ch][i][this.L_Q[ch]-1];
		}

		for(i = 0; i<MAX_M; i++) {
			this.bs_add_harmonic_prev[ch][i] = this.bs_add_harmonic[ch][i];
		}
		this.bs_add_harmonic_flag_prev[ch] = this.bs_add_harmonic_flag[ch];

		if(this.l_A[ch]==this.L_E[ch])
			this.prevEnvIsShort[ch] = 0;
		else
			this.prevEnvIsShort[ch] = -1;

		return 0;
	}

	private void sbr_save_matrix(int ch) {
		int i;

		for(i = 0; i<this.tHFGen; i++) {
			for(int j = 0; j<64; j++) {
				Xsbr[ch][i][j][0] = Xsbr[ch][i+numTimeSlotsRate][j][0];
				Xsbr[ch][i][j][1] = Xsbr[ch][i+numTimeSlotsRate][j][1];
			}
		}
		for(i = this.tHFGen; i<MAX_NTSRHFG; i++) {
			for(int j = 0; j<64; j++) {
				Xsbr[ch][i][j][0] = 0;
				Xsbr[ch][i][j][1] = 0;
			}
		}
	}

	private int sbr_process_channel(float[] channel_buf, float[][][] X,
		int ch, boolean dont_process) {
		int k, l;
		int ret = 0;

		this.bsco = 0;

		/* subband analysis */
		if(dont_process)
			qmfa[ch].sbr_qmf_analysis_32(this, channel_buf, this.Xsbr[ch], this.tHFGen, 32);
		else
			qmfa[ch].sbr_qmf_analysis_32(this, channel_buf, this.Xsbr[ch], this.tHFGen, this.kx);

		if(!dont_process) {
			/* insert high frequencies here */
			/* hf generation using patching */
			HFGeneration.hf_generation(this, this.Xsbr[ch], this.Xsbr[ch], ch);


			/* hf adjustment */
			ret = HFAdjustment.hf_adjustment(this, this.Xsbr[ch], ch);
			if(ret>0) {
				dont_process = true;
			}
		}

		if(this.just_seeked||dont_process) {
			for(l = 0; l<this.numTimeSlotsRate; l++) {
				for(k = 0; k<32; k++) {
					X[l][k][0] = this.Xsbr[ch][l+this.tHFAdj][k][0];
					X[l][k][1] = this.Xsbr[ch][l+this.tHFAdj][k][1];
				}
				for(k = 32; k<64; k++) {
					X[l][k][0] = 0;
					X[l][k][1] = 0;
				}
			}
		}
		else {
			for(l = 0; l<this.numTimeSlotsRate; l++) {
				int kx_band, M_band, bsco_band;

				if(l<this.t_E[ch][0]) {
					kx_band = this.kx_prev;
					M_band = this.M_prev;
					bsco_band = this.bsco_prev;
				}
				else {
					kx_band = this.kx;
					M_band = this.M;
					bsco_band = this.bsco;
				}

				for(k = 0; k<kx_band+bsco_band; k++) {
					X[l][k][0] = this.Xsbr[ch][l+this.tHFAdj][k][0];
					X[l][k][1] = this.Xsbr[ch][l+this.tHFAdj][k][1];
				}
				for(k = kx_band+bsco_band; k<kx_band+M_band; k++) {
					X[l][k][0] = this.Xsbr[ch][l+this.tHFAdj][k][0];
					X[l][k][1] = this.Xsbr[ch][l+this.tHFAdj][k][1];
				}
				for(k = Math.max(kx_band+bsco_band, kx_band+M_band); k<64; k++) {
					X[l][k][0] = 0;
					X[l][k][1] = 0;
				}
			}
		}

		return ret;
	}

	public int _process(float[] left_chan, float[] right_chan,
		boolean just_seeked) {
		boolean dont_process = false;
		int ret = 0;
		float[][][] X = new float[MAX_NTSR][64][2];

		/* case can occur due to bit errors */
		if(!stereo) return 21;

		if(this.ret!=0||(this.header_count==0)) {
			/* don't process just upsample */
			dont_process = true;

			/* Re-activate reset for next frame */
			if(this.ret!=0&&this.Reset)
				this.bs_start_freq_prev = -1;
		}

		if(just_seeked) {
			this.just_seeked = true;
		}
		else {
			this.just_seeked = false;
		}

		this.ret += sbr_process_channel(left_chan, X, 0, dont_process);
		/* subband synthesis */
		if(downSampledSBR) {
			qmfs[0].sbr_qmf_synthesis_32(this, X, left_chan);
		}
		else {
			qmfs[0].sbr_qmf_synthesis_64(this, X, left_chan);
		}

		this.ret += sbr_process_channel(right_chan, X, 1, dont_process);
		/* subband synthesis */
		if(downSampledSBR) {
			qmfs[1].sbr_qmf_synthesis_32(this, X, right_chan);
		}
		else {
			qmfs[1].sbr_qmf_synthesis_64(this, X, right_chan);
		}

		if(this.bs_header_flag)
			this.just_seeked = false;

		if(this.header_count!=0&&this.ret==0) {
			ret = sbr_save_prev_data(0);
			if(ret!=0) return ret;
			ret = sbr_save_prev_data(1);
			if(ret!=0) return ret;
		}

		sbr_save_matrix(0);
		sbr_save_matrix(1);

		this.frame++;

		return 0;
	}

	public int process(float[] channel,
		boolean just_seeked) {
		boolean dont_process = false;
		int ret = 0;
		float[][][] X = new float[MAX_NTSR][64][2];

		/* case can occur due to bit errors */
		if(stereo) return 21;

		if(this.ret!=0||(this.header_count==0)) {
			/* don't process just upsample */
			dont_process = true;

			/* Re-activate reset for next frame */
			if(this.ret!=0&&this.Reset)
				this.bs_start_freq_prev = -1;
		}

		if(just_seeked) {
			this.just_seeked = true;
		}
		else {
			this.just_seeked = false;
		}

		this.ret += sbr_process_channel(channel, X, 0, dont_process);
		/* subband synthesis */
		if(downSampledSBR) {
			qmfs[0].sbr_qmf_synthesis_32(this, X, channel);
		}
		else {
			qmfs[0].sbr_qmf_synthesis_64(this, X, channel);
		}

		if(this.bs_header_flag)
			this.just_seeked = false;

		if(this.header_count!=0&&this.ret==0) {
			ret = sbr_save_prev_data(0);
			if(ret!=0) return ret;
		}

		sbr_save_matrix(0);

		this.frame++;

		return 0;
	}

	public int processPS(float[] left_channel, float[] right_channel,
		boolean just_seeked) {
		int l, k;
		boolean dont_process = false;
		int ret = 0;
		float[][][] X_left = new float[38][64][2];
		float[][][] X_right = new float[38][64][2];

		/* case can occur due to bit errors */
		if(stereo) return 21;

		if(this.ret!=0||(this.header_count==0)) {
			/* don't process just upsample */
			dont_process = true;

			/* Re-activate reset for next frame */
			if(this.ret!=0&&this.Reset)
				this.bs_start_freq_prev = -1;
		}

		if(just_seeked) {
			this.just_seeked = true;
		}
		else {
			this.just_seeked = false;
		}

		if(this.qmfs[1]==null) {
			this.qmfs[1] = new SynthesisFilterbank((downSampledSBR) ? 32 : 64);
		}

		this.ret += sbr_process_channel(left_channel, X_left, 0, dont_process);

		/* copy some extra data for PS */
		for(l = this.numTimeSlotsRate; l<this.numTimeSlotsRate+6; l++) {
			for(k = 0; k<5; k++) {
				X_left[l][k][0] = this.Xsbr[0][this.tHFAdj+l][k][0];
				X_left[l][k][1] = this.Xsbr[0][this.tHFAdj+l][k][1];
			}
		}

		/* perform parametric stereo */
		ps.process(X_left, X_right);

		/* subband synthesis */
		if(downSampledSBR) {
			qmfs[0].sbr_qmf_synthesis_32(this, X_left, left_channel);
			qmfs[1].sbr_qmf_synthesis_32(this, X_right, right_channel);
		}
		else {
			qmfs[0].sbr_qmf_synthesis_64(this, X_left, left_channel);
			qmfs[1].sbr_qmf_synthesis_64(this, X_right, right_channel);
		}

		if(this.bs_header_flag)
			this.just_seeked = false;

		if(this.header_count!=0&&this.ret==0) {
			ret = sbr_save_prev_data(0);
			if(ret!=0) return ret;
		}

		sbr_save_matrix(0);

		this.frame++;

		return 0;
	}

	public boolean isPSUsed() {
		return ps_used;
	}
}
