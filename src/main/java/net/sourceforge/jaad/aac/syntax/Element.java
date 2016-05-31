package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.sbr.SBR;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license. 
 *
 * @author in-somnia
 */
public abstract class Element implements SyntaxConstants {

	private int elementInstanceTag;
	private SBR sbr;

	protected void readElementInstanceTag(IBitStream _in) throws AACException {
		elementInstanceTag = _in.readBits(4);
	}

	public int getElementInstanceTag() {
		return elementInstanceTag;
	}

	void decodeSBR(IBitStream _in, SampleFrequency sf, int count, boolean stereo, boolean crc, boolean downSampled,boolean smallFrames) throws AACException {
		if(sbr==null) sbr = new SBR(smallFrames,elementInstanceTag==ELEMENT_CPE,sf,downSampled);
		sbr.decode(_in, count);
	}

	boolean isSBRPresent() {
		return sbr!=null;
	}

	SBR getSBR() {
		return sbr;
	}
}
