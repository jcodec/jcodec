package net.sourceforge.jaad.aac.syntax;

import static net.sourceforge.jaad.aac.Profile.*;

import org.jcodec.platform.Platform;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.AACDecoderConfig;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.tools.ICPrediction;
import static js.lang.System.arraycopy;

import org.jcodec.platform.Platform;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.filterbank.FilterBank;
import net.sourceforge.jaad.aac.syntax.IBitStream;
import net.sourceforge.jaad.aac.syntax.ICSInfo;
import net.sourceforge.jaad.aac.syntax.ICStream;
import net.sourceforge.jaad.aac.syntax.SyntaxConstants;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license. 
 *
 * @author in-somnia
 */
public class ICSInfo implements SyntaxConstants, ScaleFactorBands {
    public static class LTPrediction implements SyntaxConstants {

        private static final float[] CODEBOOK = {
            0.570829f,
            0.696616f,
            0.813004f,
            0.911304f,
            0.984900f,
            1.067894f,
            1.194601f,
            1.369533f
        };
        private final int frameLength;
        private final int[] states;
        private int coef, lag, lastBand;
        private boolean lagUpdate;
        private boolean[] shortUsed, shortLagPresent, longUsed;
        private int[] shortLag;

        public LTPrediction(int frameLength) {
            this.frameLength = frameLength;
            states = new int[4*frameLength];
        }

        public void decode(IBitStream _in, ICSInfo info, Profile profile) throws AACException {
            lag = 0;
            if(profile.equals(Profile.AAC_LD)) {
                lagUpdate = _in.readBool();
                if(lagUpdate) lag = _in.readBits(10);
            }
            else lag = _in.readBits(11);
            if(lag>(frameLength<<1)) throw new AACException("LTP lag too large: "+lag);
            coef = _in.readBits(3);

            final int windowCount = info.getWindowCount();

            if(info.isEightShortFrame()) {
                shortUsed = new boolean[windowCount];
                shortLagPresent = new boolean[windowCount];
                shortLag = new int[windowCount];
                for(int w = 0; w<windowCount; w++) {
                    if((shortUsed[w] = _in.readBool())) {
                        shortLagPresent[w] = _in.readBool();
                        if(shortLagPresent[w]) shortLag[w] = _in.readBits(4);
                    }
                }
            }
            else {
                lastBand = Math.min(info.getMaxSFB(), MAX_LTP_SFB);
                longUsed = new boolean[lastBand];
                for(int i = 0; i<lastBand; i++) {
                    longUsed[i] = _in.readBool();
                }
            }
        }

        public void setPredictionUnused(int sfb) {
            if(longUsed!=null) longUsed[sfb] = false;
        }

        public void process(ICStream ics, float[] data, FilterBank filterBank, SampleFrequency sf) {
            final ICSInfo info = ics.getInfo();

            if(!info.isEightShortFrame()) {
                final int samples = frameLength<<1;
                final float[] _in = new float[2048];
                final float[] out = new float[2048];

                for(int i = 0; i<samples; i++) {
                    _in[i] = states[samples+i-lag]*CODEBOOK[coef];
                }

                filterBank.processLTP(info.getWindowSequence(), info.getWindowShape(ICSInfo.CURRENT),
                        info.getWindowShape(ICSInfo.PREVIOUS), _in, out);

                if(ics.isTNSDataPresent()) ics.getTNS().process(ics, out, sf, true);

                final int[] swbOffsets = info.getSWBOffsets();
                final int swbOffsetMax = info.getSWBOffsetMax();
                int low, high, bin;
                for(int sfb = 0; sfb<lastBand; sfb++) {
                    if(longUsed[sfb]) {
                        low = swbOffsets[sfb];
                        high = Math.min(swbOffsets[sfb+1], swbOffsetMax);

                        for(bin = low; bin<high; bin++) {
                            data[bin] += out[bin];
                        }
                    }
                }
            }
        }

        public void updateState(float[] time, float[] overlap, Profile profile) {
            int i;
            if(profile.equals(Profile.AAC_LD)) {
                for(i = 0; i<frameLength; i++) {
                    states[i] = states[i+frameLength];
                    states[frameLength+i] = states[i+(frameLength*2)];
                    states[(frameLength*2)+i] = Math.round(time[i]);
                    states[(frameLength*3)+i] = Math.round(overlap[i]);
                }
            }
            else {
                for(i = 0; i<frameLength; i++) {
                    states[i] = states[i+frameLength];
                    states[frameLength+i] = Math.round(time[i]);
                    states[(frameLength*2)+i] = Math.round(overlap[i]);
                }
            }
        }

        public static boolean isLTPProfile(Profile profile) {
            return profile.equals(Profile.AAC_LTP)||profile.equals(Profile.ER_AAC_LTP)||profile.equals(Profile.AAC_LD);
        }

        public void copy(LTPrediction ltp) {
            arraycopy(ltp.states, 0, states, 0, states.length);
            coef = ltp.coef;
            lag = ltp.lag;
            lastBand = ltp.lastBand;
            lagUpdate = ltp.lagUpdate;
            shortUsed = Platform.copyOfBool(ltp.shortUsed, ltp.shortUsed.length);
            shortLagPresent = Platform.copyOfBool(ltp.shortLagPresent, ltp.shortLagPresent.length);
            shortLag = Platform.copyOfInt(ltp.shortLag, ltp.shortLag.length);
            longUsed = Platform.copyOfBool(ltp.longUsed, ltp.longUsed.length);
        }
    }

	public static final int WINDOW_SHAPE_SINE = 0;
	public static final int WINDOW_SHAPE_KAISER = 1;
	public static final int PREVIOUS = 0;
	public static final int CURRENT = 1;

	public static enum WindowSequence {
		ONLY_LONG_SEQUENCE,
		LONG_START_SEQUENCE,
		EIGHT_SHORT_SEQUENCE,
		LONG_STOP_SEQUENCE;
	}

	public static WindowSequence windowSequenceFromInt(int i) throws AACException {
        WindowSequence[] values = WindowSequence.values();
        if (values.length >= i) {
            throw new AACException("unknown window sequence type");
        }
        return values[i];
    }
    
	private final int frameLength;
	private WindowSequence windowSequence;
	private int[] windowShape;
	private int maxSFB;
	//prediction
	private boolean predictionDataPresent;
	private ICPrediction icPredict;
	boolean ltpData1Present, ltpData2Present;
	private LTPrediction ltPredict1, ltPredict2;
	//windows/sfbs
	private int windowCount;
	private int windowGroupCount;
	private int[] windowGroupLength;
	private int swbCount;
	private int[] swbOffsets;

	public ICSInfo(int frameLength) {
		this.frameLength = frameLength;
		windowShape = new int[2];
		windowSequence = WindowSequence.ONLY_LONG_SEQUENCE;
		windowGroupLength = new int[MAX_WINDOW_GROUP_COUNT];
		ltpData1Present = false;
		ltpData2Present = false;
	}

	/* ========== decoding ========== */
	public void decode(IBitStream _in, AACDecoderConfig conf, boolean commonWindow) throws AACException {
		final SampleFrequency sf = conf.getSampleFrequency();
		if(sf.equals(SampleFrequency.SAMPLE_FREQUENCY_NONE)) throw new AACException("invalid sample frequency");

		_in.skipBit(); //reserved
		windowSequence = windowSequenceFromInt(_in.readBits(2));
		windowShape[PREVIOUS] = windowShape[CURRENT];
		windowShape[CURRENT] = _in.readBit();

		windowGroupCount = 1;
		windowGroupLength[0] = 1;
		if(windowSequence.equals(WindowSequence.EIGHT_SHORT_SEQUENCE)) {
			maxSFB = _in.readBits(4);
			int i;
			for(i = 0; i<7; i++) {
				if(_in.readBool()) windowGroupLength[windowGroupCount-1]++;
				else {
					windowGroupCount++;
					windowGroupLength[windowGroupCount-1] = 1;
				}
			}
			windowCount = 8;
			swbOffsets = SWB_OFFSET_SHORT_WINDOW[sf.getIndex()];
			swbCount = SWB_SHORT_WINDOW_COUNT[sf.getIndex()];
			predictionDataPresent = false;
		}
		else {
			maxSFB = _in.readBits(6);
			windowCount = 1;
			swbOffsets = SWB_OFFSET_LONG_WINDOW[sf.getIndex()];
			swbCount = SWB_LONG_WINDOW_COUNT[sf.getIndex()];
			predictionDataPresent = _in.readBool();
			if(predictionDataPresent) readPredictionData(_in, conf.getProfile(), sf, commonWindow);
		}
	}

	private void readPredictionData(IBitStream _in, Profile profile, SampleFrequency sf, boolean commonWindow) throws AACException {
	    if (AAC_MAIN == profile) {
            if(icPredict==null) icPredict = new ICPrediction();
            icPredict.decode(_in, maxSFB, sf);
	    } else if (AAC_LTP == profile) {
            if(ltpData1Present = _in.readBool()) {
                if(ltPredict1==null) ltPredict1 = new LTPrediction(frameLength);
                ltPredict1.decode(_in, this, profile);
            }
            if(commonWindow) {
                if(ltpData2Present = _in.readBool()) {
                    if(ltPredict2==null) ltPredict2 = new LTPrediction(frameLength);
                    ltPredict2.decode(_in, this, profile);
                }
            }
	    } else if(ER_AAC_LTP == profile) {
            if(!commonWindow) {
                if(ltpData1Present = _in.readBool()) {
                    if(ltPredict1==null) ltPredict1 = new LTPrediction(frameLength);
                    ltPredict1.decode(_in, this, profile);
                }
            }
	    } else {
            throw new AACException("unexpected profile for LTP: "+profile);
	    }
	}

	/* =========== gets ============ */
	public int getMaxSFB() {
		return maxSFB;
	}

	public int getSWBCount() {
		return swbCount;
	}

	public int[] getSWBOffsets() {
		return swbOffsets;
	}

	public int getSWBOffsetMax() {
		return swbOffsets[swbCount];
	}

	public int getWindowCount() {
		return windowCount;
	}

	public int getWindowGroupCount() {
		return windowGroupCount;
	}

	public int getWindowGroupLength(int g) {
		return windowGroupLength[g];
	}

	public WindowSequence getWindowSequence() {
		return windowSequence;
	}

	public boolean isEightShortFrame() {
		return windowSequence.equals(WindowSequence.EIGHT_SHORT_SEQUENCE);
	}

	public int getWindowShape(int index) {
		return windowShape[index];
	}

	public boolean isICPredictionPresent() {
		return predictionDataPresent;
	}

	public ICPrediction getICPrediction() {
		return icPredict;
	}

	public boolean isLTPrediction1Present() {
		return ltpData1Present;
	}

	public LTPrediction getLTPrediction1() {
		return ltPredict1;
	}

	public boolean isLTPrediction2Present() {
		return ltpData2Present;
	}

	public LTPrediction getLTPrediction2() {
		return ltPredict2;
	}

	public void unsetPredictionSFB(int sfb) {
		if(predictionDataPresent) icPredict.setPredictionUnused(sfb);
		if(ltpData1Present) ltPredict1.setPredictionUnused(sfb);
		if(ltpData2Present) ltPredict2.setPredictionUnused(sfb);
	}

	public void setData(ICSInfo info) {
		windowSequence = WindowSequence.valueOf(info.windowSequence.name());
		windowShape[PREVIOUS] = windowShape[CURRENT];
		windowShape[CURRENT] = info.windowShape[CURRENT];
		maxSFB = info.maxSFB;
		predictionDataPresent = info.predictionDataPresent;
		if(predictionDataPresent) icPredict = info.icPredict;
		ltpData1Present = info.ltpData1Present;
		if(ltpData1Present) {
			ltPredict1.copy(info.ltPredict1);
			ltPredict2.copy(info.ltPredict2);
		}
		windowCount = info.windowCount;
		windowGroupCount = info.windowGroupCount;
		windowGroupLength = Platform.copyOfInt(info.windowGroupLength, info.windowGroupLength.length);
		swbCount = info.swbCount;
		swbOffsets = Platform.copyOfInt(info.swbOffsets, info.swbOffsets.length);
	}
}
