package org.jcodec.scale;

import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv420pToRgb implements Transform {

    public Yuv420pToRgb() {
    }

    @Override
    public final void transform(Picture src, Picture dst) {
        byte[] yh = src.getPlaneData(0);
        byte[] uh = src.getPlaneData(1);
        byte[] vh = src.getPlaneData(2);
        byte[] yl = null;
        byte[] ul = null;
        byte[] vl = null;
        byte[][] low = src.getLowBits();
        
        if (low != null) {
            yl = low[0];
            ul = low[1];
            vl = low[2];
        }
        byte[] data = dst.getPlaneData(0);
        byte[] lowBits = dst.getLowBits() == null ? null : dst.getLowBits()[0];
        boolean hbd = src.isHiBD() && dst.isHiBD();
        int lowBitsNumSrc = src.getLowBitsNum();
        int lowBitsNumDst = dst.getLowBitsNum();

        int offLuma = 0, offChroma = 0;
        int stride = dst.getWidth();
        
        
/*
     	w = 16 old 0 -> 15
     	slices = 2
     	new:
     	  slicew = 16 / slices = 8
     	  
     	  0 -> 7 and 8 -> 15
     	
     	slicewidth = 8
     	

		
*
*
*
*/
    	int slices = 2;
//    	int slices = 2;
    	boolean okToSlice = false;
    	while (!okToSlice)
    	{
    		if (dst.getWidth() % slices == 0)
    		{
    			okToSlice = true;
    			break;
    		}
    		
    		slices = slices / 2;
    		if (slices == 1)
    		{
    			break;
    		}
    	}
    	
    	if (slices == 1)
    	{
    		// skip - threading.
    	}

		// slicing 
		
		int sliceWidth = dst.getWidth() / slices;

		
System.out.println("slices = " + slices + " slicewidth: " + sliceWidth + " width: " + dst.getWidth());    		
        

        
        
        for (int sliceNo = 0 ; sliceNo < slices ; sliceNo++)
        {
        	
	        for (int i = 0; i < (dst.getHeight() /2 ); i++) {
	        	for (int k = ((sliceNo * sliceWidth) / 2 ) ; k < ( ((sliceNo+1) * sliceWidth) / 2) ; k++) {
	
/* 
 * k = 0 ; k < 2 ; k+1
 * k = 0 1
 * j = 0 2
 * */
	        		
/* 
 * k = 0 ; k < 4 ; k+2
 * k = 0 2
 * 
 * */
/*
 * s = 0 sw = 2
 * k = 0 * 2 ; k < 1 * 2 ; k+2
 * k = 0 
 * 
 * s = 1 
 * k = 1 * 2 ; k < 2 * 2 ; k+2
 * k = 2
 * 
 * */	        		
	        		

	        		int pretendOffLuma = i * (2 * stride);
	        		int pretendOffChroma = ((stride / 2) * i) + (k);
	        		int j = k * 2;
	        		
	        		offLuma = pretendOffLuma;
	        	    offChroma = pretendOffChroma;
	        		
	        		if (dst.getWidth() < 10)
	        		{
	        			System.out.println("slice: "+ sliceNo +" i: " + i+ " k: " + k + " j: " + j + " offChroma: " + offChroma + " offLuma: " + offLuma + " calc-c: " + pretendOffChroma + " calc-l: " + pretendOffLuma);
	        		}
	        		
	                if (hbd) {
	                    YUV420pToRGBH2H(yh[offLuma + j], yl[offLuma + j], uh[offChroma], ul[offChroma], vh[offChroma],
	                            vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst, (offLuma + j) * 3);
	                    YUV420pToRGBH2H(yh[offLuma + j + 1], yl[offLuma + j + 1], uh[offChroma], ul[offChroma],
	                            vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
	                            (offLuma + j + 1) * 3);
	                    YUV420pToRGBH2H(yh[offLuma + j + stride], yl[offLuma + j + stride], uh[offChroma], ul[offChroma],
	                            vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
	                            (offLuma + j + stride) * 3);
	                    YUV420pToRGBH2H(yh[offLuma + j + stride + 1], yl[offLuma + j + stride + 1], uh[offChroma],
	                            ul[offChroma], vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
	                            (offLuma + j + stride + 1) * 3);
	                } else {
	                    YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3);
	                    YUV420pToRGBN2N(yh[offLuma + j + 1], uh[offChroma], vh[offChroma], data, (offLuma + j + 1) * 3);
	
	                    YUV420pToRGBN2N(yh[offLuma + j + stride], uh[offChroma], vh[offChroma], data,
	                            (offLuma + j + stride) * 3);
	                    YUV420pToRGBN2N(yh[offLuma + j + stride + 1], uh[offChroma], vh[offChroma], data, (offLuma + j
	                            + stride + 1) * 3);
	                }

	            }
	        	
	        	// TODO - add unit test for odd width and height !!!
	            if ((dst.getWidth() & 0x1) != 0) {
	                int j = dst.getWidth() - 1;
	
	                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3);
	                YUV420pToRGBN2N(yh[offLuma + j + stride], uh[offChroma], vh[offChroma], data, (offLuma + j + stride) * 3);
	
	                ++offChroma;
	            }
	        }
	        
	        offChroma = 0;
	        offLuma = 0;
	    }

        
        
// FIXME - values will be wrong here - offChroma and offLuma need to be calcd for max i and max k        
        
        
        if ((dst.getHeight() & 0x1) != 0) {
            for (int k = 0; k < (dst.getWidth() >> 1); k++) {
                int j = k << 1;
                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3);
                YUV420pToRGBN2N(yh[offLuma + j + 1], uh[offChroma], vh[offChroma], data, (offLuma + j + 1) * 3);

                ++offChroma;
            }
            if ((dst.getWidth() & 0x1) != 0) {
                int j = dst.getWidth() - 1;

                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3);

                ++offChroma;
            }
        }
    }

    public static void YUV420pToRGBN2N(byte y, byte u, byte v, byte[] data, int off) {
        int c = y + 112;
        int r = (298 * c + 409 * v + 128) >> 8;
        int g = (298 * c - 100 * u - 208 * v + 128) >> 8;
        int b = (298 * c + 516 * u + 128) >> 8;
        data[off] = (byte) (MathUtil.clip(r, 0, 255) - 128);
        data[off + 1] = (byte) (MathUtil.clip(g, 0, 255) - 128);
        data[off + 2] = (byte) (MathUtil.clip(b, 0, 255) - 128);
    }
    
    public static void YUV420pToRGBH2H(byte yh, byte yl, byte uh, byte ul, byte vh, byte vl, int nlbi,
            byte[] data, byte[] lowBits, int nlbo, int off) {
        int clipMax = ((1 << nlbo) << 8) - 1;
        int round = (1 << nlbo) >> 1;

        int c = ((yh + 128) << nlbi) + yl - 64;
        int d = ((uh + 128) << nlbi) + ul - 512;
        int e = ((vh + 128) << nlbi) + vl - 512;

        int r = MathUtil.clip((298 * c + 409 * e + 128) >> 8, 0, clipMax);
        int g = MathUtil.clip((298 * c - 100 * d - 208 * e + 128) >> 8, 0, clipMax);
        int b = MathUtil.clip((298 * c + 516 * d + 128) >> 8, 0, clipMax);

        int valR = MathUtil.clip((r + round) >> nlbo, 0, 255);
        data[off] = (byte) (valR - 128);
        lowBits[off] = (byte) (r - (valR << nlbo));

        int valG = MathUtil.clip((g + round) >> nlbo, 0, 255);
        data[off + 1] = (byte) (valG - 128);
        lowBits[off + 1] = (byte) (g - (valG << nlbo));

        int valB = MathUtil.clip((b + round) >> nlbo, 0, 255);
        data[off + 2] = (byte) (valB - 128);
        lowBits[off + 2] = (byte) (b - (valB << nlbo));
    }
}