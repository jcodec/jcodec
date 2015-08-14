package net.sourceforge.jaad.aac;

/**
 * An enumeration that represents all possible sample frequencies AAC data can 
 * have.
 * 
 * @author in-somnia
 */
public enum SampleFrequency {

	SAMPLE_FREQUENCY_96000(0, 96000, new int[]{33, 512}, new int[]{31, 9}),
	SAMPLE_FREQUENCY_88200(1, 88200, new int[]{33, 512}, new int[]{31, 9}),
	SAMPLE_FREQUENCY_64000(2, 64000, new int[]{38, 664}, new int[]{34, 10}),
	SAMPLE_FREQUENCY_48000(3, 48000, new int[]{40, 672}, new int[]{40, 14}),
	SAMPLE_FREQUENCY_44100(4, 44100, new int[]{40, 672}, new int[]{42, 14}),
	SAMPLE_FREQUENCY_32000(5, 32000, new int[]{40, 672}, new int[]{51, 14}),
	SAMPLE_FREQUENCY_24000(6, 24000, new int[]{41, 652}, new int[]{46, 14}),
	SAMPLE_FREQUENCY_22050(7, 22050, new int[]{41, 652}, new int[]{46, 14}),
	SAMPLE_FREQUENCY_16000(8, 16000, new int[]{37, 664}, new int[]{42, 14}),
	SAMPLE_FREQUENCY_12000(9, 12000, new int[]{37, 664}, new int[]{42, 14}),
	SAMPLE_FREQUENCY_11025(10, 11025, new int[]{37, 664}, new int[]{42, 14}),
	SAMPLE_FREQUENCY_8000(11, 8000, new int[]{34, 664}, new int[]{39, 14}),
	SAMPLE_FREQUENCY_NONE(-1, 0, new int[]{0, 0}, new int[]{0, 0});

	/**
	 * Returns a sample frequency instance for the given index. If the index
	 * is not between 0 and 11 inclusive, SAMPLE_FREQUENCY_NONE is returned.
	 * @return a sample frequency with the given index
	 */
	public static SampleFrequency forInt(int i) {
		final SampleFrequency freq;
		if(i>=0&&i<12) freq = values()[i];
		else freq = SAMPLE_FREQUENCY_NONE;
		return freq;
	}

	public static SampleFrequency forFrequency(int i) {
		final SampleFrequency[] all = values();

		SampleFrequency freq = null;
		for(int j = 0; freq==null&&j<12; j++) {
			if(i==all[j].frequency) freq = all[j];
		}

		if(freq==null) freq = SAMPLE_FREQUENCY_NONE;
		return freq;
	}
	private final int index, frequency;
	private final int[] prediction, maxTNS_SFB;

	private SampleFrequency(int index, int freqency, int[] prediction, int[] maxTNS_SFB) {
		this.index = index;
		this.frequency = freqency;
		this.prediction = prediction;
		this.maxTNS_SFB = maxTNS_SFB;
	}

	/**
	 * Returns this sample frequency's index between 0 (96000) and 11 (8000)
	 * or -1 if this is SAMPLE_FREQUENCY_NONE.
	 * @return the sample frequency's index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the sample frequency as integer value. This may be a value
	 * between 96000 and 8000, or 0 if this is SAMPLE_FREQUENCY_NONE.
	 * @return the sample frequency
	 */
	public int getFrequency() {
		return frequency;
	}

	/**
	 * Returns the highest scale factor band allowed for ICPrediction at this
	 * sample frequency.
	 * This method is mainly used internally.
	 * @return the highest prediction SFB
	 */
	public int getMaximalPredictionSFB() {
		return prediction[0];
	}

	/**
	 * Returns the number of predictors allowed for ICPrediction at this
	 * sample frequency.
	 * This method is mainly used internally.
	 * @return the number of ICPredictors
	 */
	public int getPredictorCount() {
		return prediction[1];
	}

	/**
	 * Returns the highest scale factor band allowed for TNS at this
	 * sample frequency.
	 * This method is mainly used internally.
	 * @return the highest SFB for TNS
	 */
	public int getMaximalTNS_SFB(boolean shortWindow) {
		return maxTNS_SFB[shortWindow ? 1 : 0];
	}

	/**
	 * Returns a string representation of this sample frequency.
	 * The method is identical to <code>getDescription()</code>.
	 * @return the sample frequency's description
	 */
	@Override
	public String toString() {
		return Integer.toString(frequency);
	}
}
