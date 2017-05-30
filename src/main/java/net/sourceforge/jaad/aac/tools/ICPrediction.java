package net.sourceforge.jaad.aac.tools;
import org.jcodec.common.logging.Logger;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.syntax.IBitStream;
import net.sourceforge.jaad.aac.syntax.ICSInfo;
import net.sourceforge.jaad.aac.syntax.ICStream;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * Intra-channel prediction used in profile Main
 * @author in-somnia
 */
public class ICPrediction {

	private static final float SF_SCALE = 1.0f/-1024.0f;
	private static final float INV_SF_SCALE = 1.0f/SF_SCALE;
	private static final int MAX_PREDICTORS = 672;
	private static final float A = 0.953125f; //61.0 / 64
	private static final float ALPHA = 0.90625f;  //29.0 / 32
	private boolean predictorReset;
	private int predictorResetGroup;
	private boolean[] predictionUsed;
	private PredictorState[] states;

	private static final class PredictorState {

		float cor0 = 0.0f;
		float cor1 = 0.0f;
		float var0 = 0.0f;
		float var1 = 0.0f;
		float r0 = 1.0f;
		float r1 = 1.0f;
	}

	public ICPrediction() {
		states = new PredictorState[MAX_PREDICTORS];
		resetAllPredictors();
	}

	public void decode(IBitStream _in, int maxSFB, SampleFrequency sf) throws AACException {
		final int predictorCount = sf.getPredictorCount();

		if(predictorReset = _in.readBool()) predictorResetGroup = _in.readBits(5);

		final int maxPredSFB = sf.getMaximalPredictionSFB();
		final int length = Math.min(maxSFB, maxPredSFB);
		predictionUsed = new boolean[length];
		for(int sfb = 0; sfb<length; sfb++) {
			predictionUsed[sfb] = _in.readBool();
		}
		Logger.warn("ICPrediction: maxSFB={0}, maxPredSFB={1}", new int[]{maxSFB, maxPredSFB});
		/*//if maxSFB<maxPredSFB set remaining to false
		for(int sfb = length; sfb<maxPredSFB; sfb++) {
		predictionUsed[sfb] = false;
		}*/
	}

	public void setPredictionUnused(int sfb) {
		predictionUsed[sfb] = false;
	}

	public void process(ICStream ics, float[] data, SampleFrequency sf) {
		final ICSInfo info = ics.getInfo();

		if(info.isEightShortFrame()) resetAllPredictors();
		else {
			final int len = Math.min(sf.getMaximalPredictionSFB(), info.getMaxSFB());
			final int[] swbOffsets = info.getSWBOffsets();
			int k;
			for(int sfb = 0; sfb<len; sfb++) {
				for(k = swbOffsets[sfb]; k<swbOffsets[sfb+1]; k++) {
					predict(data, k, predictionUsed[sfb]);
				}
			}
			if(predictorReset) resetPredictorGroup(predictorResetGroup);
		}
	}

	private void resetPredictState(int index) {
		if(states[index]==null) states[index] = new PredictorState();
		states[index].r0 = 0;
		states[index].r1 = 0;
		states[index].cor0 = 0;
		states[index].cor1 = 0;
		states[index].var0 = 0x3F80;
		states[index].var1 = 0x3F80;
	}

	private void resetAllPredictors() {
		int i;
		for(i = 0; i<states.length; i++) {
			resetPredictState(i);
		}
	}

	private void resetPredictorGroup(int group) {
		int i;
		for(i = group-1; i<states.length; i += 30) {
			resetPredictState(i);
		}
	}

	private void predict(float[] data, int off, boolean output) {
		if(states[off]==null) states[off] = new PredictorState();
		final PredictorState state = states[off];
		final float r0 = state.r0, r1 = state.r1;
		final float cor0 = state.cor0, cor1 = state.cor1;
		final float var0 = state.var0, var1 = state.var1;

		final float k1 = var0>1 ? cor0*even(A/var0) : 0;
		final float k2 = var1>1 ? cor1*even(A/var1) : 0;

		final float pv = round(k1*r0+k2*r1);
		if(output) data[off] += pv*SF_SCALE;

		final float e0 = (data[off]*INV_SF_SCALE);
		final float e1 = e0-k1*r0;

		state.cor1 = trunc(ALPHA*cor1+r1*e1);
		state.var1 = trunc(ALPHA*var1+0.5f*(r1*r1+e1*e1));
		state.cor0 = trunc(ALPHA*cor0+r0*e0);
		state.var0 = trunc(ALPHA*var0+0.5f*(r0*r0+e0*e0));

		state.r1 = trunc(A*(r0-k1*e0));
		state.r0 = trunc(A*e0);
	}

	private float round(float pf) {
		return Float.intBitsToFloat((Float.floatToIntBits(pf)+0x00008000)&0xFFFF0000);
	}

	private float even(float pf) {
		int i = Float.floatToIntBits(pf);
		i = (i+0x00007FFF+(i&0x00010000>>16))&0xFFFF0000;
		return Float.intBitsToFloat(i);
	}

	private float trunc(float pf) {
		return Float.intBitsToFloat(Float.floatToIntBits(pf)&0xFFFF0000);
	}
}
