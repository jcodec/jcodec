package net.sourceforge.jaad.aac.tools;

import net.sourceforge.jaad.aac.huffman.HCB;
import net.sourceforge.jaad.aac.syntax.CPE;
import net.sourceforge.jaad.aac.syntax.SyntaxConstants;
import net.sourceforge.jaad.aac.syntax.ICSInfo;
import net.sourceforge.jaad.aac.syntax.ICStream;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * Mid/side stereo
 * @author in-somnia
 */
public final class MS implements SyntaxConstants, HCB {

	private MS() {
	}

	public static void process(CPE cpe, float[] specL, float[] specR) {
		final ICStream ics = cpe.getLeftChannel();
		final ICSInfo info = ics.getInfo();
		final int[] offsets = info.getSWBOffsets();
		final int windowGroups = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();
		final int[] sfbCBl = ics.getSfbCB();
		final int[] sfbCBr = cpe.getRightChannel().getSfbCB();
		int groupOff = 0;
		int g, i, w, j, idx = 0;

		for(g = 0; g<windowGroups; g++) {
			for(i = 0; i<maxSFB; i++, idx++) {
				if(cpe.isMSUsed(idx)&&sfbCBl[idx]<NOISE_HCB&&sfbCBr[idx]<NOISE_HCB) {
					for(w = 0; w<info.getWindowGroupLength(g); w++) {
						final int off = groupOff+w*128+offsets[i];
						for(j = 0; j<offsets[i+1]-offsets[i]; j++) {
							float t = specL[off+j]-specR[off+j];
							specL[off+j] += specR[off+j];
							specR[off+j] = t;
						}
					}
				}
			}
			groupOff += info.getWindowGroupLength(g)*128;
		}
	}
}
