package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.SampleFrequency;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
public class PCE extends Element {

	private static final int MAX_FRONT_CHANNEL_ELEMENTS = 16;
	private static final int MAX_SIDE_CHANNEL_ELEMENTS = 16;
	private static final int MAX_BACK_CHANNEL_ELEMENTS = 16;
	private static final int MAX_LFE_CHANNEL_ELEMENTS = 4;
	private static final int MAX_ASSOC_DATA_ELEMENTS = 8;
	private static final int MAX_VALID_CC_ELEMENTS = 16;

	public static class TaggedElement {

		private final boolean isCPE;
		private final int tag;

		public TaggedElement(boolean isCPE, int tag) {
			this.isCPE = isCPE;
			this.tag = tag;
		}

		public boolean isIsCPE() {
			return isCPE;
		}

		public int getTag() {
			return tag;
		}
	}

	public static class CCE {

		private final boolean isIndSW;
		private final int tag;

		public CCE(boolean isIndSW, int tag) {
			this.isIndSW = isIndSW;
			this.tag = tag;
		}

		public boolean isIsIndSW() {
			return isIndSW;
		}

		public int getTag() {
			return tag;
		}
	}
	private Profile profile;
	private SampleFrequency sampleFrequency;
	private int frontChannelElementsCount, sideChannelElementsCount, backChannelElementsCount;
	private int lfeChannelElementsCount, assocDataElementsCount;
	private int validCCElementsCount;
	private boolean monoMixdown, stereoMixdown, matrixMixdownIDXPresent;
	private int monoMixdownElementNumber, stereoMixdownElementNumber, matrixMixdownIDX;
	private boolean pseudoSurround;
	private final TaggedElement[] frontElements, sideElements, backElements;
	private final int[] lfeElementTags;
	private final int[] assocDataElementTags;
	private final CCE[] ccElements;
	private byte[] commentFieldData;

	public PCE() {
		super();
		frontElements = new TaggedElement[MAX_FRONT_CHANNEL_ELEMENTS];
		sideElements = new TaggedElement[MAX_SIDE_CHANNEL_ELEMENTS];
		backElements = new TaggedElement[MAX_BACK_CHANNEL_ELEMENTS];
		lfeElementTags = new int[MAX_LFE_CHANNEL_ELEMENTS];
		assocDataElementTags = new int[MAX_ASSOC_DATA_ELEMENTS];
		ccElements = new CCE[MAX_VALID_CC_ELEMENTS];
		sampleFrequency = SampleFrequency.SAMPLE_FREQUENCY_NONE;
	}

	public void decode(IBitStream in) throws AACException {
		readElementInstanceTag(in);

		profile = Profile.forInt(in.readBits(2));

		sampleFrequency = SampleFrequency.forInt(in.readBits(4));

		frontChannelElementsCount = in.readBits(4);
		sideChannelElementsCount = in.readBits(4);
		backChannelElementsCount = in.readBits(4);
		lfeChannelElementsCount = in.readBits(2);
		assocDataElementsCount = in.readBits(3);
		validCCElementsCount = in.readBits(4);

		if(monoMixdown = in.readBool()) {
			SyntaxConstants.LOGGER.warning("mono mixdown present, but not yet supported");
			monoMixdownElementNumber = in.readBits(4);
		}
		if(stereoMixdown = in.readBool()) {
			SyntaxConstants.LOGGER.warning("stereo mixdown present, but not yet supported");
			stereoMixdownElementNumber = in.readBits(4);
		}
		if(matrixMixdownIDXPresent = in.readBool()) {
			SyntaxConstants.LOGGER.warning("matrix mixdown present, but not yet supported");
			matrixMixdownIDX = in.readBits(2);
			pseudoSurround = in.readBool();
		}

		readTaggedElementArray(frontElements, in, frontChannelElementsCount);

		readTaggedElementArray(sideElements, in, sideChannelElementsCount);

		readTaggedElementArray(backElements, in, backChannelElementsCount);

		int i;
		for(i = 0; i<lfeChannelElementsCount; ++i) {
			lfeElementTags[i] = in.readBits(4);
		}

		for(i = 0; i<assocDataElementsCount; ++i) {
			assocDataElementTags[i] = in.readBits(4);
		}

		for(i = 0; i<validCCElementsCount; ++i) {
			ccElements[i] = new CCE(in.readBool(), in.readBits(4));
		}

		in.byteAlign();

		final int commentFieldBytes = in.readBits(8);
		commentFieldData = new byte[commentFieldBytes];
		for(i = 0; i<commentFieldBytes; i++) {
			commentFieldData[i] = (byte) in.readBits(8);
		}
	}

	private void readTaggedElementArray(TaggedElement[] te, IBitStream in, int len) throws AACException {
		for(int i = 0; i<len; ++i) {
			te[i] = new TaggedElement(in.readBool(), in.readBits(4));
		}
	}

	public Profile getProfile() {
		return profile;
	}

	public SampleFrequency getSampleFrequency() {
		return sampleFrequency;
	}

	public int getChannelCount() {
		return frontChannelElementsCount+sideChannelElementsCount+backChannelElementsCount
				+lfeChannelElementsCount+assocDataElementsCount;
	}
}
