package org.jcodec.codecs.h264;

import static org.junit.Assert.assertArrayEquals;
import junit.framework.TestCase;

import org.jcodec.codecs.h264.decode.ChromaPredictionBuilder;

public class TestChromaPredictionBuilder extends TestCase {

	
	public void testVertical() throws Exception {
		int[] expectedCb = new int[] {
			129, 127, 122, 119, 116, 116, 116, 116,
			129, 127, 122, 119, 116, 116, 116, 116,
			129, 127, 122, 119, 116, 116, 116, 116,
			129, 127, 122, 119, 116, 116, 116, 116,
			129, 127, 122, 119, 116, 116, 116, 116,
			129, 127, 122, 119, 116, 116, 116, 116,
			129, 127, 122, 119, 116, 116, 116, 116,
			129, 127, 122, 119, 116, 116, 116, 116
		};
		int[] expectedCr = new int[] {
			128, 128, 128, 128, 132, 132, 132, 132,
			128, 128, 128, 128, 132, 132, 132, 132,
			128, 128, 128, 128, 132, 132, 132, 132,
			128, 128, 128, 128, 132, 132, 132, 132,
			128, 128, 128, 128, 132, 132, 132, 132,
			128, 128, 128, 128, 132, 132, 132, 132,
			128, 128, 128, 128, 132, 132, 132, 132,
			128, 128, 128, 128, 132, 132, 132, 132
		};
		
		int[] topCb = new int[] {129, 127, 122, 119, 116, 116, 116, 116};
		int[] topCr = new int[] {128, 128, 128, 128, 132, 132, 132, 132};
		
        int[] actualCb = new int[64];
        int[] actualCr = new int[64];

		ChromaPredictionBuilder.predictVertical(actualCb, 0, true,
                 topCb);
        ChromaPredictionBuilder.predictVertical(actualCr, 0, true,
                 topCr);
		
		assertArrayEquals(expectedCb, actualCb);
		assertArrayEquals(expectedCr, actualCr);
	}
	
	public void testHorizontal() throws Exception {
		int[] expectedCb = new int[] {
			115,115,115,115,115,115,115,115,
			115,115,115,115,115,115,115,115,
			115,115,115,115,115,115,115,115,
			115,115,115,115,115,115,115,115,
			118,118,118,118,118,118,118,118,
			116,116,116,116,116,116,116,116,
			111,111,111,111,111,111,111,111,
			108,108,108,108,108,108,108,108
		};
		int[] expectedCr = new int[] {
			137,137,137,137,137,137,137,137,
			137,137,137,137,137,137,137,137,
			137,137,137,137,137,137,137,137,
			137,137,137,137,137,137,137,137,
			131,131,131,131,131,131,131,131,
			134,134,134,134,134,134,134,134,
			139,139,139,139,139,139,139,139,
			141,141,141,141,141,141,141,141
		};
		
		int[] leftCb = new int[] {115, 115, 115, 115, 118, 116, 111, 108};
//		int[] topCb = new int[] {115, 115, 115, 115, 115, 115, 115, 115};
		
		int[] leftCr = new int[] { 137, 137, 137, 137, 131, 134, 139, 141};
//		int[] topCr = new int[] {135, 135, 135, 135, 135, 135, 135, 135};

		int[] actualCb = new int[64];
		int[] actualCr = new int[64];
		
		ChromaPredictionBuilder.predictHorizontal(actualCb, 0, true,
                leftCb);
        ChromaPredictionBuilder.predictHorizontal(actualCr, 0, true,
                leftCr);
		
		assertArrayEquals(expectedCb, actualCb);
		assertArrayEquals(expectedCr, actualCr);
	}
	
	public void testDC() throws Exception {
		int[] expectedCb = new int[] {
				119, 119, 119, 119, 120, 120, 120, 120,
				119, 119, 119, 119, 120, 120, 120, 120,
				119, 119, 119, 119, 120, 120, 120, 120,
				119, 119, 119, 119, 120, 120, 120, 120,
				118, 118, 118, 118, 119, 119, 119, 119,
				118, 118, 118, 118, 119, 119, 119, 119,
				118, 118, 118, 118, 119, 119, 119, 119,
				118, 118, 118, 118, 119, 119, 119, 119
		};
		
		int[] expectedCr = new int[] {
				131, 131, 131, 131,  132, 132, 132, 132,
				131, 131, 131, 131, 132, 132, 132, 132,
				131, 131, 131, 131,  132, 132, 132, 132,
				131, 131, 131, 131,132, 132, 132, 132,
				132, 132, 132, 132, 132, 132, 132, 132,
				132, 132, 132, 132, 132, 132, 132, 132,
				132, 132, 132, 132, 132, 132, 132, 132,
				132, 132, 132, 132 , 132, 132, 132, 132
		};
		
		int[] leftCb = new int[] {
			 118, 118, 118, 118, 118, 118, 118, 118
		};
		int[] topCb = new int[] {
			120, 120, 120, 120, 120, 120, 120, 120
		};
		
		int[] leftCr = new int[] {
			 131, 131, 131, 131, 132, 132, 132, 132
		};
		
		int[] topCr = new int[] {
			131, 131, 131, 131, 132, 132, 132, 132
		};

		int[] actualCb = new int[64];
		int[] actualCr = new int[64];
		
		ChromaPredictionBuilder.predictDC(actualCb, 0, true, true,
                leftCb, topCb);
        ChromaPredictionBuilder.predictDC(actualCr, 0, true, true,
                leftCr, topCr);
        
        assertArrayEquals(expectedCb, actualCb);
        assertArrayEquals(expectedCr, actualCr);
	}
	
	public void testPlane() throws Exception {
		
		int[] expectedCb = new int[] {
			115, 116, 116, 117, 117, 118, 118, 119, 
			116, 117, 117, 118, 118, 119, 119, 120, 
			117, 117, 118, 118, 119, 119, 120, 120, 
			118, 118, 119, 119, 120, 120, 121, 121, 
			118, 119, 119, 120, 120, 121, 121, 122, 
			119, 119, 120, 120, 121, 121, 122, 122, 
			120, 120, 121, 121, 122, 122, 123, 123, 
			120, 121, 121, 122, 122, 123, 123, 124
		};
		int[] expectedCr = new int[] {
			137, 136, 136, 135, 135, 134, 134, 133, 
			137, 136, 136, 135, 135, 134, 133, 133, 
			137, 136, 136, 135, 135, 134, 133, 133, 
			137, 136, 136, 135, 134, 134, 133, 133, 
			137, 136, 136, 135, 134, 134, 133, 133, 
			137, 136, 135, 135, 134, 134, 133, 132, 
			137, 136, 135, 135, 134, 134, 133, 132, 
			136, 136, 135, 135, 134, 133, 133, 132
		};
		
		int[] leftCb = {116, 116, 116, 116, 119, 119, 119, 119};
		int[] tlCb = {113};
		int[] topCb = {118, 118, 118, 118, 119, 119, 119, 119};

		int[] leftCr =  {137, 137, 137, 137, 138, 138, 138, 138};
		int[] tlCr = {141};
		int[] topCr =  {132, 132, 132, 132, 132, 132, 132, 132};
		

		int[] actualCb = new int[64];
		int[] actualCr = new int[64];
		ChromaPredictionBuilder.predictPlane(actualCb, 0, true, true,
	            leftCb, topCb, tlCb);
		ChromaPredictionBuilder.predictPlane(actualCr, 0, true, true,
                leftCr, topCr, tlCr);
		
		assertArrayEquals(expectedCb, actualCb);
		assertArrayEquals(expectedCr, actualCr);
	}
}