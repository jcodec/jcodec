package org.jcodec.scale;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv420pToRgb extends ConcurrentTransform {

	
    public Yuv420pToRgb() {
		super("Yuv420pToRgb");
    }

    
	@Override
	protected List<Runnable> getTasks(boolean willBeConcurrent, Picture src, Picture dst) {

    	int slices = Runtime.getRuntime().availableProcessors();
    	boolean okToSlice = false;
    	while (!okToSlice)
    	{
    		if (slices == 1)
    		{
    			break;
    		}

    		if (dst.getWidth() < 72)
    		{
    			// TOO SMALL TO SLICE
    			break;
    		}
    		
// FIXME - broken for odd length pictures - need special case task.
    		if ((dst.getWidth() / slices > 16) && dst.getWidth() % slices == 0)
    		{
    			okToSlice = true;
    			break;
    		}
    		
    		slices = slices / 2;
    	}
    	
		int sliceWidth = dst.getWidth() / slices;
		
//		System.out.println("willBeConcurrent = " + willBeConcurrent + " oktoslice=" + okToSlice + "  slices = " + slices + " slicewidth: " + sliceWidth + " width: " + dst.getWidth());    		
        
		final List<Runnable> tasks = new ArrayList<Runnable>();
        if (willBeConcurrent && okToSlice)
        {
	        for (int sliceNo = 0 ; sliceNo < slices ; sliceNo++)
	        {
	        	int wStart = ((sliceNo * sliceWidth) / 2 ) ;
	        	int wEnd = ( ((sliceNo+1) * sliceWidth) / 2) ; 
	        	
	        	tasks.add(new Yuv420pToRgbRunnable(src, dst, wStart, wEnd));
	        }
        }
        else
        {
        	tasks.add(new Yuv420pToRgbRunnable(src, dst, 0, dst.getWidth() / 2));
        }
		
		return tasks;
	}
	
    
    private static final class Yuv420pToRgbRunnable implements Runnable {
    	
        private final Picture dst;
        private final Picture src;
        private final int wStart;
        private final int wEnd;
        
        private Yuv420pToRgbRunnable(Picture src, Picture dst, int wStart, int wEnd) {
        	this.dst = dst;
			this.src = src;
        	this.wStart = wStart;
        	this.wEnd = wEnd;
        }

        public void run() {
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
        	
        	int stride = dst.getWidth();
        	int offLuma = 0, offChroma = 0;
	        for (int i = 0; i < (dst.getHeight() /2 ); i++) {
	        	for (int k = wStart ; k < wEnd ; k++) {
		        		

	        		offLuma = i * (2 * stride);
	        		offChroma = ((stride / 2) * i) + (k);
	        		int j = k * 2;
	        		
	        		if (dst.getWidth() < 10)
	        		{
	        			System.out.println("i: " + i+ " k: " + k + " j: " + j + " offChroma: " + offChroma + " offLuma: " + offLuma);
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