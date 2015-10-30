package net.sourceforge.jaad.aac.sbr;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license. 
 *
 * @author in-somnia
 */
class TFGrid implements SBRConstants {


	/* function constructs new time border vector */
	/* first build into temp vector to be able to use previous vector on error */
	public static int envelope_time_border_vector(SBR sbr, int ch) {
		int l, border, temp;
		int[] t_E_temp = new int[6];

		t_E_temp[0] = sbr.rate*sbr.abs_bord_lead[ch];
		t_E_temp[sbr.L_E[ch]] = sbr.rate*sbr.abs_bord_trail[ch];

		switch(sbr.bs_frame_class[ch]) {
			case FIXFIX:
				switch(sbr.L_E[ch]) {
					case 4:
						temp = (sbr.numTimeSlots/4);
						t_E_temp[3] = sbr.rate*3*temp;
						t_E_temp[2] = sbr.rate*2*temp;
						t_E_temp[1] = sbr.rate*temp;
						break;
					case 2:
						t_E_temp[1] = sbr.rate*(sbr.numTimeSlots/2);
						break;
					default:
						break;
				}
				break;

			case FIXVAR:
				if(sbr.L_E[ch]>1) {
					int i = sbr.L_E[ch];
					border = sbr.abs_bord_trail[ch];

					for(l = 0; l<(sbr.L_E[ch]-1); l++) {
						if(border<sbr.bs_rel_bord[ch][l])
							return 1;

						border -= sbr.bs_rel_bord[ch][l];
						t_E_temp[--i] = sbr.rate*border;
					}
				}
				break;

			case VARFIX:
				if(sbr.L_E[ch]>1) {
					int i = 1;
					border = sbr.abs_bord_lead[ch];

					for(l = 0; l<(sbr.L_E[ch]-1); l++) {
						border += sbr.bs_rel_bord[ch][l];

						if(sbr.rate*border+sbr.tHFAdj>sbr.numTimeSlotsRate+sbr.tHFGen)
							return 1;

						t_E_temp[i++] = sbr.rate*border;
					}
				}
				break;

			case VARVAR:
				if(sbr.bs_num_rel_0[ch]!=0) {
					int i = 1;
					border = sbr.abs_bord_lead[ch];

					for(l = 0; l<sbr.bs_num_rel_0[ch]; l++) {
						border += sbr.bs_rel_bord_0[ch][l];

						if(sbr.rate*border+sbr.tHFAdj>sbr.numTimeSlotsRate+sbr.tHFGen)
							return 1;

						t_E_temp[i++] = sbr.rate*border;
					}
				}

				if(sbr.bs_num_rel_1[ch]!=0) {
					int i = sbr.L_E[ch];
					border = sbr.abs_bord_trail[ch];

					for(l = 0; l<sbr.bs_num_rel_1[ch]; l++) {
						if(border<sbr.bs_rel_bord_1[ch][l])
							return 1;

						border -= sbr.bs_rel_bord_1[ch][l];
						t_E_temp[--i] = sbr.rate*border;
					}
				}
				break;
		}

		/* no error occured, we can safely use this t_E vector */
		for(l = 0; l<6; l++) {
			sbr.t_E[ch][l] = t_E_temp[l];
		}

		return 0;
	}

	public static void noise_floor_time_border_vector(SBR sbr, int ch) {
		sbr.t_Q[ch][0] = sbr.t_E[ch][0];

		if(sbr.L_E[ch]==1) {
			sbr.t_Q[ch][1] = sbr.t_E[ch][1];
			sbr.t_Q[ch][2] = 0;
		}
		else {
			int index = middleBorder(sbr, ch);
			sbr.t_Q[ch][1] = sbr.t_E[ch][index];
			sbr.t_Q[ch][2] = sbr.t_E[ch][sbr.L_E[ch]];
		}
	}

	private static int middleBorder(SBR sbr, int ch) {
		int retval = 0;

		switch(sbr.bs_frame_class[ch]) {
			case FIXFIX:
				retval = sbr.L_E[ch]/2;
				break;
			case VARFIX:
				if(sbr.bs_pointer[ch]==0)
					retval = 1;
				else if(sbr.bs_pointer[ch]==1)
					retval = sbr.L_E[ch]-1;
				else
					retval = sbr.bs_pointer[ch]-1;
				break;
			case FIXVAR:
			case VARVAR:
				if(sbr.bs_pointer[ch]>1)
					retval = sbr.L_E[ch]+1-sbr.bs_pointer[ch];
				else
					retval = sbr.L_E[ch]-1;
				break;
		}

		return (retval>0) ? retval : 0;
	}

}
