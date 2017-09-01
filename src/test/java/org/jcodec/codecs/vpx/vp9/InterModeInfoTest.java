package org.jcodec.codecs.vpx.vp9;

import org.junit.Assert;
import org.junit.Test;

public class InterModeInfoTest {
	
	@Test
	public void testReadInterModeInfo() {
		MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] {}, new int[] {});
		DecodingContext c = new DecodingContext();
		int miCol = 0;
		int miRow = 0;
		int blSz = 0;
		
		InterModeInfo modeInfo = InterModeInfo.read(miCol, miRow, blSz, decoder, c);
		
		Assert.assertEquals(true, modeInfo.isInter());
		Assert.assertEquals(MVList.create(MV.create(0, 0, 0), MV.create(0, 0, 0)), modeInfo.getMvl0());
		Assert.assertEquals(MVList.create(MV.create(0, 0, 0), MV.create(0, 0, 0)), modeInfo.getMvl1());
		Assert.assertEquals(MVList.create(MV.create(0, 0, 0), MV.create(0, 0, 0)), modeInfo.getMvl2());
		Assert.assertEquals(MVList.create(MV.create(0, 0, 0), MV.create(0, 0, 0)), modeInfo.getMvl3());
		
		Assert.assertEquals(0, modeInfo.getSegmentId());
		Assert.assertEquals(false, modeInfo.isSkip());
		Assert.assertEquals(0, modeInfo.getTxSize());
		Assert.assertEquals(0, modeInfo.getYMode());
		Assert.assertEquals(0, modeInfo.getSubModes());
		Assert.assertEquals(0, modeInfo.getUvMode());
	}
}
