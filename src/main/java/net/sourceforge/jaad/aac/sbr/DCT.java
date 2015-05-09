package net.sourceforge.jaad.aac.sbr;

class DCT {

	private static final int n = 32;

	// w_array_real[i] = cos(2*M_PI*i/32)
	private static final float[] w_array_real = {
		1.000000000000000f, 0.980785279337272f,
		0.923879528329380f, 0.831469603195765f,
		0.707106765732237f, 0.555570210304169f,
		0.382683402077046f, 0.195090284503576f,
		0.000000000000000f, -0.195090370246552f,
		-0.382683482845162f, -0.555570282993553f,
		-0.707106827549476f, -0.831469651765257f,
		-0.923879561784627f, -0.980785296392607f
	};

	// w_array_imag[i] = sin(-2*M_PI*i/32)
	private static final float[] w_array_imag = {
		0.000000000000000f, -0.195090327375064f,
		-0.382683442461104f, -0.555570246648862f,
		-0.707106796640858f, -0.831469627480512f,
		-0.923879545057005f, -0.980785287864940f,
		-1.000000000000000f, -0.980785270809601f,
		-0.923879511601754f, -0.831469578911016f,
		-0.707106734823616f, -0.555570173959476f,
		-0.382683361692986f, -0.195090241632088f
	};

	private static final float[] dct4_64_tab = {
		0.999924719333649f, 0.998118102550507f,
		0.993906974792480f, 0.987301409244537f,
		0.978317379951477f, 0.966976463794708f,
		0.953306019306183f, 0.937339007854462f,
		0.919113874435425f, 0.898674488067627f,
		0.876070082187653f, 0.851355195045471f,
		0.824589252471924f, 0.795836925506592f,
		0.765167236328125f, 0.732654273509979f,
		0.698376238346100f, 0.662415742874146f,
		0.624859452247620f, 0.585797846317291f,
		0.545324981212616f, 0.503538429737091f,
		0.460538715124130f, 0.416429549455643f,
		0.371317148208618f, 0.325310230255127f,
		0.278519600629807f, 0.231058135628700f,
		0.183039888739586f, 0.134580686688423f,
		0.085797272622585f, 0.036807164549828f,
		-1.012196302413940f, -1.059438824653626f,
		-1.104129195213318f, -1.146159529685974f,
		-1.185428738594055f, -1.221842169761658f,
		-1.255311965942383f, -1.285757660865784f,
		-1.313105940818787f, -1.337290763854981f,
		-1.358253836631775f, -1.375944852828980f,
		-1.390321016311646f, -1.401347875595093f,
		-1.408998727798462f, -1.413255214691162f,
		-1.414107084274292f, -1.411552190780640f,
		-1.405596733093262f, -1.396255016326904f,
		-1.383549690246582f, -1.367511272430420f,
		-1.348178386688232f, -1.325597524642944f,
		-1.299823284149170f, -1.270917654037476f,
		-1.238950133323669f, -1.203998088836670f,
		-1.166145324707031f, -1.125483393669128f,
		-1.082109928131104f, -1.036129593849182f,
		-0.987653195858002f, -0.936797380447388f,
		-0.883684754371643f, -0.828443288803101f,
		-0.771206021308899f, -0.712110757827759f,
		-0.651300072669983f, -0.588920354843140f,
		-0.525121808052063f, -0.460058242082596f,
		-0.393886327743530f, -0.326765477657318f,
		-0.258857429027557f, -0.190325915813446f,
		-0.121335685253143f, -0.052053272724152f,
		0.017354607582092f, 0.086720645427704f,
		0.155877828598022f, 0.224659323692322f,
		0.292899727821350f, 0.360434412956238f,
		0.427100926637650f, 0.492738455533981f,
		0.557188928127289f, 0.620297133922577f,
		0.681910991668701f, 0.741881847381592f,
		0.800065577030182f, 0.856321990489960f,
		0.910515367984772f, 0.962515234947205f,
		1.000000000000000f, 0.998795449733734f,
		0.995184719562531f, 0.989176511764526f,
		0.980785250663757f, 0.970031261444092f,
		0.956940352916718f, 0.941544055938721f,
		0.923879504203796f, 0.903989315032959f,
		0.881921231746674f, 0.857728600502014f,
		0.831469595432281f, 0.803207516670227f,
		0.773010432720184f, 0.740951120853424f,
		0.707106769084930f, 0.671558916568756f,
		0.634393274784088f, 0.595699310302734f,
		0.555570185184479f, 0.514102697372437f,
		0.471396654844284f, 0.427555114030838f,
		0.382683426141739f, 0.336889833211899f,
		0.290284633636475f, 0.242980122566223f,
		0.195090234279633f, 0.146730497479439f,
		0.098017133772373f, 0.049067649990320f,
		-1.000000000000000f, -1.047863125801086f,
		-1.093201875686646f, -1.135906934738159f,
		-1.175875544548035f, -1.213011503219605f,
		-1.247225046157837f, -1.278433918952942f,
		-1.306562900543213f, -1.331544399261475f,
		-1.353317975997925f, -1.371831417083740f,
		-1.387039899826050f, -1.398906826972961f,
		-1.407403707504273f, -1.412510156631470f,
		0f, -1.412510156631470f,
		-1.407403707504273f, -1.398906826972961f,
		-1.387039899826050f, -1.371831417083740f,
		-1.353317975997925f, -1.331544399261475f,
		-1.306562900543213f, -1.278433918952942f,
		-1.247225046157837f, -1.213011384010315f,
		-1.175875544548035f, -1.135907053947449f,
		-1.093201875686646f, -1.047863125801086f,
		-1.000000000000000f, -0.949727773666382f,
		-0.897167563438416f, -0.842446029186249f,
		-0.785694956779480f, -0.727051079273224f,
		-0.666655659675598f, -0.604654192924500f,
		-0.541196048259735f, -0.476434230804443f,
		-0.410524487495422f, -0.343625843524933f,
		-0.275899350643158f, -0.207508206367493f,
		-0.138617098331451f, -0.069392144680023f,
		0f, 0.069392263889313f,
		0.138617157936096f, 0.207508206367493f,
		0.275899469852448f, 0.343625962734222f,
		0.410524636507034f, 0.476434201002121f,
		0.541196107864380f, 0.604654192924500f,
		0.666655719280243f, 0.727051138877869f,
		0.785695075988770f, 0.842446029186249f,
		0.897167563438416f, 0.949727773666382f
	};

	private static final int[] bit_rev_tab = {0, 16, 8, 24, 4, 20, 12, 28, 2, 18, 10, 26, 6, 22, 14, 30, 1, 17, 9, 25, 5, 21, 13, 29, 3, 19, 11, 27, 7, 23, 15, 31};

	// FFT decimation in frequency
	// 4*16*2+16=128+16=144 multiplications
	// 6*16*2+10*8+4*16*2=192+80+128=400 additions
	private static void fft_dif(float[] Real, float[] Imag) {
		float w_real, w_imag; // For faster access
		float point1_real, point1_imag, point2_real, point2_imag; // For faster access
		int j, i, i2, w_index; // Counters

		// First 2 stages of 32 point FFT decimation in frequency
		// 4*16*2=64*2=128 multiplications
		// 6*16*2=96*2=192 additions
		// Stage 1 of 32 point FFT decimation in frequency
		for(i = 0; i<16; i++) {
			point1_real = Real[i];
			point1_imag = Imag[i];
			i2 = i+16;
			point2_real = Real[i2];
			point2_imag = Imag[i2];

			w_real = w_array_real[i];
			w_imag = w_array_imag[i];

			// temp1 = x[i] - x[i2]
			point1_real -= point2_real;
			point1_imag -= point2_imag;

			// x[i1] = x[i] + x[i2]
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// x[i2] = (x[i] - x[i2]) * w
			Real[i2] = ((point1_real*w_real)-(point1_imag*w_imag));
			Imag[i2] = ((point1_real*w_imag)+(point1_imag*w_real));
		}
		// Stage 2 of 32 point FFT decimation in frequency
		for(j = 0, w_index = 0; j<8; j++, w_index += 2) {
			w_real = w_array_real[w_index];
			w_imag = w_array_imag[w_index];

			i = j;
			point1_real = Real[i];
			point1_imag = Imag[i];
			i2 = i+8;
			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// temp1 = x[i] - x[i2]
			point1_real -= point2_real;
			point1_imag -= point2_imag;

			// x[i1] = x[i] + x[i2]
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// x[i2] = (x[i] - x[i2]) * w
			Real[i2] = ((point1_real*w_real)-(point1_imag*w_imag));
			Imag[i2] = ((point1_real*w_imag)+(point1_imag*w_real));

			i = j+16;
			point1_real = Real[i];
			point1_imag = Imag[i];
			i2 = i+8;
			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// temp1 = x[i] - x[i2]
			point1_real -= point2_real;
			point1_imag -= point2_imag;

			// x[i1] = x[i] + x[i2]
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// x[i2] = (x[i] - x[i2]) * w
			Real[i2] = ((point1_real*w_real)-(point1_imag*w_imag));
			Imag[i2] = ((point1_real*w_imag)+(point1_imag*w_real));
		}

		// Stage 3 of 32 point FFT decimation in frequency
		// 2*4*2=16 multiplications
		// 4*4*2+6*4*2=10*8=80 additions
		for(i = 0; i<n; i += 8) {
			i2 = i+4;
			point1_real = Real[i];
			point1_imag = Imag[i];

			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// out[i1] = point1 + point2
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// out[i2] = point1 - point2
			Real[i2] = point1_real-point2_real;
			Imag[i2] = point1_imag-point2_imag;
		}
		w_real = w_array_real[4]; // = sqrt(2)/2
		// w_imag = -w_real; // = w_array_imag[4]; // = -sqrt(2)/2
		for(i = 1; i<n; i += 8) {
			i2 = i+4;
			point1_real = Real[i];
			point1_imag = Imag[i];

			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// temp1 = x[i] - x[i2]
			point1_real -= point2_real;
			point1_imag -= point2_imag;

			// x[i1] = x[i] + x[i2]
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// x[i2] = (x[i] - x[i2]) * w
			Real[i2] = (point1_real+point1_imag)*w_real;
			Imag[i2] = (point1_imag-point1_real)*w_real;
		}
		for(i = 2; i<n; i += 8) {
			i2 = i+4;
			point1_real = Real[i];
			point1_imag = Imag[i];

			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// x[i] = x[i] + x[i2]
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// x[i2] = (x[i] - x[i2]) * (-i)
			Real[i2] = point1_imag-point2_imag;
			Imag[i2] = point2_real-point1_real;
		}
		w_real = w_array_real[12]; // = -sqrt(2)/2
		// w_imag = w_real; // = w_array_imag[12]; // = -sqrt(2)/2
		for(i = 3; i<n; i += 8) {
			i2 = i+4;
			point1_real = Real[i];
			point1_imag = Imag[i];

			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// temp1 = x[i] - x[i2]
			point1_real -= point2_real;
			point1_imag -= point2_imag;

			// x[i1] = x[i] + x[i2]
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// x[i2] = (x[i] - x[i2]) * w
			Real[i2] = (point1_real-point1_imag)*w_real;
			Imag[i2] = (point1_real+point1_imag)*w_real;
		}

		// Stage 4 of 32 point FFT decimation in frequency (no multiplications)
		// 16*4=64 additions
		for(i = 0; i<n; i += 4) {
			i2 = i+2;
			point1_real = Real[i];
			point1_imag = Imag[i];

			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// x[i1] = x[i] + x[i2]
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// x[i2] = x[i] - x[i2]
			Real[i2] = point1_real-point2_real;
			Imag[i2] = point1_imag-point2_imag;
		}
		for(i = 1; i<n; i += 4) {
			i2 = i+2;
			point1_real = Real[i];
			point1_imag = Imag[i];

			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// x[i] = x[i] + x[i2]
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// x[i2] = (x[i] - x[i2]) * (-i)
			Real[i2] = point1_imag-point2_imag;
			Imag[i2] = point2_real-point1_real;
		}

		// Stage 5 of 32 point FFT decimation in frequency (no multiplications)
		// 16*4=64 additions
		for(i = 0; i<n; i += 2) {
			i2 = i+1;
			point1_real = Real[i];
			point1_imag = Imag[i];

			point2_real = Real[i2];
			point2_imag = Imag[i2];

			// out[i1] = point1 + point2
			Real[i] += point2_real;
			Imag[i] += point2_imag;

			// out[i2] = point1 - point2
			Real[i2] = point1_real-point2_real;
			Imag[i2] = point1_imag-point2_imag;
		}

		//FFTReorder(Real, Imag);
	}

	/* size 64 only! */
	public static void dct4_kernel(float[] in_real, float[] in_imag, float[] out_real, float[] out_imag) {
		// Tables with bit reverse values for 5 bits, bit reverse of i at i-th position
		int i, i_rev;

		/* Step 2: modulate */
		// 3*32=96 multiplications
		// 3*32=96 additions
		for(i = 0; i<32; i++) {
			float x_re, x_im, tmp;
			x_re = in_real[i];
			x_im = in_imag[i];
			tmp = (x_re+x_im)*dct4_64_tab[i];
			in_real[i] = (x_im*dct4_64_tab[i+64])+tmp;
			in_imag[i] = (x_re*dct4_64_tab[i+32])+tmp;
		}

		/* Step 3: FFT, but with output in bit reverse order */
		fft_dif(in_real, in_imag);

		/* Step 4: modulate + bitreverse reordering */
		// 3*31+2=95 multiplications
		// 3*31+2=95 additions
		for(i = 0; i<16; i++) {
			float x_re, x_im, tmp;
			i_rev = bit_rev_tab[i];
			x_re = in_real[i_rev];
			x_im = in_imag[i_rev];

			tmp = (x_re+x_im)*dct4_64_tab[i+3*32];
			out_real[i] = (x_im*dct4_64_tab[i+5*32])+tmp;
			out_imag[i] = (x_re*dct4_64_tab[i+4*32])+tmp;
		}
		// i = 16, i_rev = 1 = rev(16);
		out_imag[16] = (in_imag[1]-in_real[1])*dct4_64_tab[16+3*32];
		out_real[16] = (in_real[1]+in_imag[1])*dct4_64_tab[16+3*32];
		for(i = 17; i<32; i++) {
			float x_re, x_im, tmp;
			i_rev = bit_rev_tab[i];
			x_re = in_real[i_rev];
			x_im = in_imag[i_rev];
			tmp = (x_re+x_im)*dct4_64_tab[i+3*32];
			out_real[i] = (x_im*dct4_64_tab[i+5*32])+tmp;
			out_imag[i] = (x_re*dct4_64_tab[i+4*32])+tmp;
		}

	}
}
