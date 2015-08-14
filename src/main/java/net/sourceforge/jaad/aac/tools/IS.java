package net.sourceforge.jaad.aac.tools;

import net.sourceforge.jaad.aac.huffman.HCB;
import net.sourceforge.jaad.aac.syntax.CPE;
import net.sourceforge.jaad.aac.syntax.SyntaxConstants;
import net.sourceforge.jaad.aac.syntax.ICSInfo;
import net.sourceforge.jaad.aac.syntax.ICStream;

/**
 * Intensity stereo
 * @author in-somnia
 */
public final class IS implements SyntaxConstants, ISScaleTable, HCB {

	private IS() {
	}

	public static void process(CPE cpe, float[] specL, float[] specR) {
		final ICStream ics = cpe.getRightChannel();
		final ICSInfo info = ics.getInfo();
		final int[] offsets = info.getSWBOffsets();
		final int windowGroups = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();
		final int[] sfbCB = ics.getSfbCB();
		final int[] sectEnd = ics.getSectEnd();
		final float[] scaleFactors = ics.getScaleFactors();

		int w, i, j, c, end, off;
		int idx = 0, groupOff = 0;
		float scale;
		for(int g = 0; g<windowGroups; g++) {
			for(i = 0; i<maxSFB;) {
				if(sfbCB[idx]==INTENSITY_HCB||sfbCB[idx]==INTENSITY_HCB2) {
					end = sectEnd[idx];
					for(; i<end; i++, idx++) {
						c = sfbCB[idx]==INTENSITY_HCB ? 1 : -1;
						if(cpe.isMSMaskPresent())
							c *= cpe.isMSUsed(idx) ? -1 : 1;
						scale = c*scaleFactors[idx];
						for(w = 0; w<info.getWindowGroupLength(g); w++) {
							off = groupOff+w*128+offsets[i];
							for(j = 0; j<offsets[i+1]-offsets[i]; j++) {
								specR[off+j] = specL[off+j]*scale;
							}
						}
					}
				}
				else {
					end = sectEnd[idx];
					idx += end-i;
					i = end;
				}
			}
			groupOff += info.getWindowGroupLength(g)*128;
		}
	}
}
