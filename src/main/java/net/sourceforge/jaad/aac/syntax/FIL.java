package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.SampleFrequency;

class FIL extends Element implements SyntaxConstants {

	public static class DynamicRangeInfo {

		private static final int MAX_NBR_BANDS = 7;
		private final boolean[] excludeMask;
		private final boolean[] additionalExcludedChannels;
		private boolean pceTagPresent;
		private int pceInstanceTag;
		private int tagReservedBits;
		private boolean excludedChannelsPresent;
		private boolean bandsPresent;
		private int bandsIncrement, interpolationScheme;
		private int[] bandTop;
		private boolean progRefLevelPresent;
		private int progRefLevel, progRefLevelReservedBits;
		private boolean[] dynRngSgn;
		private int[] dynRngCtl;

		public DynamicRangeInfo() {
			excludeMask = new boolean[MAX_NBR_BANDS];
			additionalExcludedChannels = new boolean[MAX_NBR_BANDS];
		}
	}
	private static final int TYPE_FILL = 0;
	private static final int TYPE_FILL_DATA = 1;
	private static final int TYPE_EXT_DATA_ELEMENT = 2;
	private static final int TYPE_DYNAMIC_RANGE = 11;
	private static final int TYPE_SBR_DATA = 13;
	private static final int TYPE_SBR_DATA_CRC = 14;
	private final boolean downSampledSBR;
	private DynamicRangeInfo dri;

	FIL(boolean downSampledSBR) {
		super();
		this.downSampledSBR = downSampledSBR;
	}

	void decode(IBitStream in, Element prev, SampleFrequency sf, boolean sbrEnabled, boolean smallFrames) throws AACException {
		int count = in.readBits(4);
		if(count==15) count += in.readBits(8)-1;
		count *= 8; //convert to bits

		final int cpy = count;
		final int pos = in.getPosition();

		while(count>0) {
			count = decodeExtensionPayload(in, count, prev, sf, sbrEnabled, smallFrames);
		}

		final int pos2 = in.getPosition()-pos;
		final int bitsLeft = cpy-pos2;
		if(bitsLeft>0) in.skipBits(pos2);
		else if(bitsLeft<0) throw new AACException("FIL element overread: "+bitsLeft);
	}

	private int decodeExtensionPayload(IBitStream in, int count, Element prev, SampleFrequency sf, boolean sbrEnabled, boolean smallFrames) throws AACException {
		final int type = in.readBits(4);
		int ret = count-4;
		switch(type) {
			case TYPE_DYNAMIC_RANGE:
				ret = decodeDynamicRangeInfo(in, ret);
				break;
			case TYPE_SBR_DATA:
			case TYPE_SBR_DATA_CRC:
				if(sbrEnabled) {
					if(prev instanceof SCE_LFE||prev instanceof CPE||prev instanceof CCE) {
						prev.decodeSBR(in, sf, ret, (prev instanceof CPE), (type==TYPE_SBR_DATA_CRC), downSampledSBR, smallFrames);
						ret = 0;
						break;
					}
					else throw new AACException("SBR applied on unexpected element: "+prev);
				}
				else {
					in.skipBits(ret);
					ret = 0;
				}
			case TYPE_FILL:
			case TYPE_FILL_DATA:
			case TYPE_EXT_DATA_ELEMENT:
			default:
				in.skipBits(ret);
				ret = 0;
				break;
		}
		return ret;
	}

	private int decodeDynamicRangeInfo(IBitStream in, int count) throws AACException {
		if(dri==null) dri = new DynamicRangeInfo();
		int ret = count;

		int bandCount = 1;

		//pce tag
		if(dri.pceTagPresent = in.readBool()) {
			dri.pceInstanceTag = in.readBits(4);
			dri.tagReservedBits = in.readBits(4);
		}

		//excluded channels
		if(dri.excludedChannelsPresent = in.readBool()) {
			ret -= decodeExcludedChannels(in);
		}

		//bands
		if(dri.bandsPresent = in.readBool()) {
			dri.bandsIncrement = in.readBits(4);
			dri.interpolationScheme = in.readBits(4);
			ret -= 8;
			bandCount += dri.bandsIncrement;
			dri.bandTop = new int[bandCount];
			for(int i = 0; i<bandCount; i++) {
				dri.bandTop[i] = in.readBits(8);
				ret -= 8;
			}
		}

		//prog ref level
		if(dri.progRefLevelPresent = in.readBool()) {
			dri.progRefLevel = in.readBits(7);
			dri.progRefLevelReservedBits = in.readBits(1);
			ret -= 8;
		}

		dri.dynRngSgn = new boolean[bandCount];
		dri.dynRngCtl = new int[bandCount];
		for(int i = 0; i<bandCount; i++) {
			dri.dynRngSgn[i] = in.readBool();
			dri.dynRngCtl[i] = in.readBits(7);
			ret -= 8;
		}
		return ret;
	}

	private int decodeExcludedChannels(IBitStream in) throws AACException {
		int i;
		int exclChs = 0;

		do {
			for(i = 0; i<7; i++) {
				dri.excludeMask[exclChs] = in.readBool();
				exclChs++;
			}
		}
		while(exclChs<57&&in.readBool());

		return (exclChs/7)*8;
	}
}
