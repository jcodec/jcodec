package org.jcodec.codecs.vpx;

import org.jcodec.codecs.vpx.VPXDCT;
import org.junit.Assert;
import org.junit.Test;

public class VP8DCTTest {
    
    @Test
    public void testDCT() {
        int[] input = {87,-38,1,0,87,-38,1,0,87,-38,1,0,87,116,155,154};
        int[] output = {331,85,171,155,-301,132,101,55,231,-101,-77,-42,-125,55,42,23};
        
        VPXDCT.fdct4x4(input);
        
        Assert.assertArrayEquals(output, input);
    }
    
    @Test
    public void testWalsh() {
        int[] input = {-696,625,104,856,856,163,625,656,-896,856,-128,856,856,336,856,-896};
        int[] output = {2515,-414,-922,-937,674,-878,1077,-473,-473,615,-878,1136,-937,-1384,-414,-3871};
        
        VPXDCT.walsh4x4(input);
        Assert.assertArrayEquals(output, input);
    }
    
    @Test
    public void testIDCT() {
        int[] input = {331,85,171,155,-301,132,101,55,231,-101,-77,-42,-125,55,42,23};
        int[] output = {87,-38,1,0,87,-38,1,0,87,-38,1,0,87,116,155,154};
        
        VPXDCT.idct4x4(input);
        
        Assert.assertArrayEquals(output, input);
    }
    
    @Test
    public void testIWalsh() {
        int[] input = {2515,-414,-922,-937,674,-878,1077,-473,-473,615,-878,1136,-937,-1384,-414,-3871};
        int[] output = {-696,625,104,856,856,163,625,655,-896,856,-128,856,856,336,856,-896};
        
        VPXDCT.iwalsh4x4(input);
        Assert.assertArrayEquals(output, input);
    }

}
