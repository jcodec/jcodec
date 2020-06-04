package org.jcodec.scale;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

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

    private ExecutorService tp;

	boolean threaded;
	
    public Yuv420pToRgb() {
    	
        this.threaded = Runtime.getRuntime().availableProcessors() > 1;
        
//        threaded = false;
  
// TODO - name the thread ppol based on trasnsform
// TODO - create base class abstract ConcurrnetTransform
        
        if (threaded) {
            tp = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
        }
    }

    @Override
    public final void transform(Picture src, Picture dst) {
    	
    	long start = System.nanoTime();
//        byte[] yh = src.getPlaneData(0);
//        byte[] uh = src.getPlaneData(1);
//        byte[] vh = src.getPlaneData(2);
//        byte[] yl = null;
//        byte[] ul = null;
//        byte[] vl = null;
//        byte[][] low = src.getLowBits();
//        
//        if (low != null) {
//            yl = low[0];
//            ul = low[1];
//            vl = low[2];
//        }
//        byte[] data = dst.getPlaneData(0);
//        byte[] lowBits = dst.getLowBits() == null ? null : dst.getLowBits()[0];
//        boolean hbd = src.isHiBD() && dst.isHiBD();
//        int lowBitsNumSrc = src.getLowBitsNum();
//        int lowBitsNumDst = dst.getLowBitsNum();
//
//        int offLuma = 0, offChroma = 0;
//        int stride = dst.getWidth();
        
        
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
    	int slices = Runtime.getRuntime().availableProcessors();
//    	int slices = 2;
    	boolean okToSlice = false;
    	while (!okToSlice)
    	{
    		if (dst.getWidth() < 72)
    		{
    			// TOO SMALL TO SLICE
    			break;
    		}
    		
    		if ((dst.getWidth() / slices > 16) && dst.getWidth() % slices == 0)
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

		
System.out.println("isthreaded = " + threaded + " oktoslice=" + okToSlice + "  slices = " + slices + " slicewidth: " + sliceWidth + " width: " + dst.getWidth());    		
        

        if (threaded && okToSlice)
        {
        	List<Yuv420pToRgbRunnable> tasks = new ArrayList<Yuv420pToRgbRunnable>(slices);
	        for (int sliceNo = 0 ; sliceNo < slices ; sliceNo++)
	        {
	        	int wStart = ((sliceNo * sliceWidth) / 2 ) ;
	        	int wEnd = ( ((sliceNo+1) * sliceWidth) / 2) ; 
	        	
	        	tasks.add(new Yuv420pToRgbRunnable(src, dst, wStart, wEnd));
	        	 
	        }
	        
	        
	        List<Future<?>> futures = new ArrayList<Future<?>>();
            for (Yuv420pToRgbRunnable task : tasks) {
System.out.println("************** submitted tasl");
                futures.add(tp.submit(task));
            }

            for (Future<?> future : futures) {
                waitForSure(future);
            }

            System.out.println("************** COMPLETE");
       	
        }
        else
        {
        	Yuv420pToRgbRunnable task = new Yuv420pToRgbRunnable(src, dst, 0, dst.getWidth() / 2);
        	task.run();
        }
        	
//            for (SliceReader sliceReader : sliceReaders) {
//                futures.add(dec.tp.submit(new SliceDecoderRunnable(this, sliceReader, result)));
//            }
//
//            for (Future<?> future : futures) {
//                waitForSure(future);
//            }
        	
        	
        	
//	        for (int i = 0; i < (dst.getHeight() /2 ); i++) {
//	        	for (int k = ((sliceNo * sliceWidth) / 2 ) ; k < ( ((sliceNo+1) * sliceWidth) / 2) ; k++) {
//	
///* 
// * k = 0 ; k < 2 ; k+1
// * k = 0 1
// * j = 0 2
// * */
//	        		
///* 
// * k = 0 ; k < 4 ; k+2
// * k = 0 2
// * 
// * */
///*
// * s = 0 sw = 2
// * k = 0 * 2 ; k < 1 * 2 ; k+2
// * k = 0 
// * 
// * s = 1 
// * k = 1 * 2 ; k < 2 * 2 ; k+2
// * k = 2
// * 
// * */	        		
//	        		
//
//	        		int pretendOffLuma = i * (2 * stride);
//	        		int pretendOffChroma = ((stride / 2) * i) + (k);
//	        		int j = k * 2;
//	        		
//	        		offLuma = pretendOffLuma;
//	        	    offChroma = pretendOffChroma;
//	        		
//	        		if (dst.getWidth() < 10)
//	        		{
//	        			System.out.println("slice: "+ sliceNo +" i: " + i+ " k: " + k + " j: " + j + " offChroma: " + offChroma + " offLuma: " + offLuma + " calc-c: " + pretendOffChroma + " calc-l: " + pretendOffLuma);
//	        		}
//	        		
//	                if (hbd) {
//	                    YUV420pToRGBH2H(yh[offLuma + j], yl[offLuma + j], uh[offChroma], ul[offChroma], vh[offChroma],
//	                            vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst, (offLuma + j) * 3);
//	                    YUV420pToRGBH2H(yh[offLuma + j + 1], yl[offLuma + j + 1], uh[offChroma], ul[offChroma],
//	                            vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
//	                            (offLuma + j + 1) * 3);
//	                    YUV420pToRGBH2H(yh[offLuma + j + stride], yl[offLuma + j + stride], uh[offChroma], ul[offChroma],
//	                            vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
//	                            (offLuma + j + stride) * 3);
//	                    YUV420pToRGBH2H(yh[offLuma + j + stride + 1], yl[offLuma + j + stride + 1], uh[offChroma],
//	                            ul[offChroma], vh[offChroma], vl[offChroma], lowBitsNumSrc, data, lowBits, lowBitsNumDst,
//	                            (offLuma + j + stride + 1) * 3);
//	                } else {
//	                    YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3);
//	                    YUV420pToRGBN2N(yh[offLuma + j + 1], uh[offChroma], vh[offChroma], data, (offLuma + j + 1) * 3);
//	
//	                    YUV420pToRGBN2N(yh[offLuma + j + stride], uh[offChroma], vh[offChroma], data,
//	                            (offLuma + j + stride) * 3);
//	                    YUV420pToRGBN2N(yh[offLuma + j + stride + 1], uh[offChroma], vh[offChroma], data, (offLuma + j
//	                            + stride + 1) * 3);
//	                }
//
//	            }
//	        	
//	        	// TODO - add unit test for odd width and height !!!
//	            if ((dst.getWidth() & 0x1) != 0) {
//	                int j = dst.getWidth() - 1;
//	
//	                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3);
//	                YUV420pToRGBN2N(yh[offLuma + j + stride], uh[offChroma], vh[offChroma], data, (offLuma + j + stride) * 3);
//	
//	                ++offChroma;
//	            }
//	        }
//	        
//	        offChroma = 0;
//	        offLuma = 0;
//	    }

        
        
// FIXME - values will be wrong here - offChroma and offLuma need to be calcd for max i and max k        
        
        
//        if ((dst.getHeight() & 0x1) != 0) {
//            for (int k = 0; k < (dst.getWidth() >> 1); k++) {
//                int j = k << 1;
//                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3);
//                YUV420pToRGBN2N(yh[offLuma + j + 1], uh[offChroma], vh[offChroma], data, (offLuma + j + 1) * 3);
//
//                ++offChroma;
//            }
//            if ((dst.getWidth() & 0x1) != 0) {
//                int j = dst.getWidth() - 1;
//
//                YUV420pToRGBN2N(yh[offLuma + j], uh[offChroma], vh[offChroma], data, (offLuma + j) * 3);
//
//                ++offChroma;
//            }
//        }
        
        
        System.out.println("Time in ns to complete - " + (System.nanoTime() - start));
    }

    
    
    
    private static final class Yuv420pToRgbRunnable implements Runnable {
    	
        private final Picture dst;
        private final Picture src;
//        private final byte[] data;
        private final int wStart;
        private final int wEnd;
//        byte[] yh;
//        byte[] uh;
//        byte[] vh;
//        byte[] yl;
//        byte[] ul;
//        byte[] vl;
//        boolean hbd;
        
        private Yuv420pToRgbRunnable(Picture src, Picture dst, int wStart, int wEnd) {
        	/*
        }
        , byte[] data, int wStart, int wEnd,
        		boolean hbd, byte[] yh, byte[] uh, byte[] vh, byte[] yl, byte[] ul, byte[] vl) {
        			*/
        	this.dst = dst;
			this.src = src;
//        	this.data = data;
        	this.wStart = wStart;
        	this.wEnd = wEnd;
//        	this.hbd = hbd;
//        	this.yh=yh;
//        	this.uh = uh;
//        	this.vh= vh;
//        	this.yl=yl;
//        	this.ul=ul;
//        	this.vl=vl;
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
    
    
    
    
    
    
    
    private void waitForSure(Future<?> future) {
        while (true) {
            try {
                future.get();
                break;
            } catch (Exception e) {
                throw new RuntimeException(e);
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