package net.sourceforge.jaad.aac.syntax;

import java.util.Arrays;
import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.tools.MSMask;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license. 
 *
 * @author in-somnia
 */
public class CPE extends Element implements SyntaxConstants {

	private MSMask msMask;
	private boolean[] msUsed;
	private boolean commonWindow;
	ICStream icsL, icsR;

	CPE(int frameLength) {
		super();
		msUsed = new boolean[MAX_MS_MASK];
		icsL = new ICStream(frameLength);
		icsR = new ICStream(frameLength);
	}

	void decode(IBitStream in, DecoderConfig conf) throws AACException {
		final Profile profile = conf.getProfile();
		final SampleFrequency sf = conf.getSampleFrequency();
		if(sf.equals(SampleFrequency.SAMPLE_FREQUENCY_NONE)) throw new AACException("invalid sample frequency");

		readElementInstanceTag(in);

		commonWindow = in.readBool();
		final ICSInfo info = icsL.getInfo();
		if(commonWindow) {
			info.decode(in, conf, commonWindow);
			icsR.getInfo().setData(info);

			msMask = MSMask.forInt(in.readBits(2));
			if(msMask.equals(MSMask.TYPE_USED)) {
				final int maxSFB = info.getMaxSFB();
				final int windowGroupCount = info.getWindowGroupCount();

				for(int idx = 0; idx<windowGroupCount*maxSFB; idx++) {
					msUsed[idx] = in.readBool();
				}
			}
			else if(msMask.equals(MSMask.TYPE_ALL_1)) Arrays.fill(msUsed, true);
			else if(msMask.equals(MSMask.TYPE_ALL_0)) Arrays.fill(msUsed, false);
			else throw new AACException("reserved MS mask type used");
		}
		else {
			msMask = MSMask.TYPE_ALL_0;
			Arrays.fill(msUsed, false);
		}

		if(profile.isErrorResilientProfile()&&(info.isLTPrediction1Present())) {
			if(info.ltpData2Present = in.readBool()) info.getLTPrediction2().decode(in, info, profile);
		}

		icsL.decode(in, commonWindow, conf);
		icsR.decode(in, commonWindow, conf);
	}

	public ICStream getLeftChannel() {
		return icsL;
	}

	public ICStream getRightChannel() {
		return icsR;
	}

	public MSMask getMSMask() {
		return msMask;
	}

	public boolean isMSUsed(int off) {
		return msUsed[off];
	}

	public boolean isMSMaskPresent() {
		return !msMask.equals(MSMask.TYPE_ALL_0);
	}

	public boolean isCommonWindow() {
		return commonWindow;
	}
}
