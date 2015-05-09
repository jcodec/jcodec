package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.DecoderConfig;

class SCE_LFE extends Element {

	private final ICStream ics;

	SCE_LFE(int frameLength) {
		super();
		ics = new ICStream(frameLength);
	}

	void decode(IBitStream in, DecoderConfig conf) throws AACException {
		readElementInstanceTag(in);
		ics.decode(in, false, conf);
	}

	public ICStream getICStream() {
		return ics;
	}
}
