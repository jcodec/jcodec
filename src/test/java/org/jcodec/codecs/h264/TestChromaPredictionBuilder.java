package org.jcodec.codecs.h264;

import org.jcodec.codecs.h264.decode.ChromaIntraPredictionBuilder;
import org.jcodec.codecs.h264.decode.model.BlockBorder;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.jcodec.codecs.h264.io.model.ChromaFormat;

public class TestChromaPredictionBuilder extends JAVCTestCase {

	
	public void testVertical() throws Exception {
		// MB 11
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
		
		int[] leftCb = null;
		int[] topCb = new int[] {129, 127, 122, 119, 116, 116, 116, 116};
		Integer topLeftCb = null;
		
		int[] leftCr = null;
		int[] topCr = new int[] {128, 128, 128, 128, 132, 132, 132, 132};
		Integer topLeftCr = null;
		BlockBorder borderCb = new BlockBorder(leftCb, topCb, topLeftCb);
		BlockBorder borderCr = new BlockBorder(leftCr, topCr, topLeftCr);

		ChromaIntraPredictionBuilder builder = new ChromaIntraPredictionBuilder(8, ChromaFormat.YUV_420);
		int[] actualCb = new int[64];
		int[] actualCr = new int[64];
		builder.predictVertical(borderCb, new PixelBuffer(actualCb, 0, 3));
		builder.predictVertical(borderCr, new PixelBuffer(actualCr, 0, 3));
		
		assertArrayEquals(expectedCb, actualCb);
		assertArrayEquals(expectedCr, actualCr);
	}
	
	public void testHorizontal() throws Exception {
		// MB 98
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
		int[] topCb = new int[] {115, 115, 115, 115, 115, 115, 115, 115};
		int topLeftCb = 115;
		
		int[] leftCr = new int[] {137, 137, 137, 137, 131, 134, 139, 141};
		int[] topCr = new int[] {135, 135, 135, 135, 135, 135, 135, 135};
		int topLeftCr = 135;
		BlockBorder borderCb = new BlockBorder(leftCb, topCb, topLeftCb);
		BlockBorder borderCr = new BlockBorder(leftCr, topCr, topLeftCr);

		ChromaIntraPredictionBuilder builder = new ChromaIntraPredictionBuilder(8, ChromaFormat.YUV_420);
		int[] actualCb = new int[64];
		int[] actualCr = new int[64];
		builder.predictHorizontal(borderCb, new PixelBuffer(actualCb, 0, 3));
		builder.predictHorizontal(borderCr, new PixelBuffer(actualCr, 0, 3));
		
		assertArrayEquals(expectedCb, actualCb);
		assertArrayEquals(expectedCr, actualCr);
	}
	
	public void testDC() throws Exception {
		// MB 64
		int[][] expectedCb = new int[][] {
			{
				119, 119, 119, 119, 
				119, 119, 119, 119, 
				119, 119, 119, 119, 
				119, 119, 119, 119
			},
			{
				120, 120, 120, 120,
				120, 120, 120, 120,
				120, 120, 120, 120,
				120, 120, 120, 120
			},
			{
				118, 118, 118, 118, 
				118, 118, 118, 118, 
				118, 118, 118, 118, 
				118, 118, 118, 118
			},
			{
				119, 119, 119, 119,
				119, 119, 119, 119,
				119, 119, 119, 119,
				119, 119, 119, 119
			}
		};
		
		int[][] expectedCr = new int[][] {
			{
				131, 131, 131, 131,  
				131, 131, 131, 131, 
				131, 131, 131, 131,  
				131, 131, 131, 131
			},
			{
				132, 132, 132, 132,
				132, 132, 132, 132, 
				132, 132, 132, 132,
				132, 132, 132, 132
			},
			{
				132, 132, 132, 132, 
				132, 132, 132, 132, 
				132, 132, 132, 132, 
				132, 132, 132, 132 
			},
			{
				132, 132, 132, 132,
				132, 132, 132, 132,
				132, 132, 132, 132,
				132, 132, 132, 132
			}
		};
		
		int[][] leftCb = new int[][] {
			{118, 118, 118, 118},
			{119, 119, 119, 119},
			{118, 118, 118, 118},
			{118, 118, 118, 118}
		};
		int[][] topCb = new int[][] {
			{120, 120, 120, 120},
			{120, 120, 120, 120},
			{119, 119, 119, 119},
			{120, 120, 120, 120}
		};
		int[] topLeftCb = new int[] {120, 120, 118, 119};
		
		int[][] leftCr = new int[][] {
			{131, 131, 131, 131},
			{131, 131, 131, 131},
			{132, 132, 132, 132},
			{132, 132, 132, 132}
		};
		int[][] topCr = new int[][] {
			{131, 131, 131, 131},
			{132, 132, 132, 132},
			{131, 131, 131, 131},
			{132, 132, 132, 132}
		};
		int[] topLeftCr = new int[] {131, 131, 131, 131};
		BlockBorder border;

		ChromaIntraPredictionBuilder builder = new ChromaIntraPredictionBuilder(8, ChromaFormat.YUV_420);
		int[] actual = new int[16];
		
		border = new BlockBorder(leftCb[0], topCb[0], topLeftCb[0]);
		builder.predictDCInside(border, new PixelBuffer(actual, 0, 2));
		assertArrayEquals(expectedCb[0], actual);
		
		border = new BlockBorder(leftCb[1], topCb[1], topLeftCb[1]);
		builder.predictDCTopBorder(border, new PixelBuffer(actual, 0, 2));
		assertArrayEquals(expectedCb[1], actual);
		
		border = new BlockBorder(leftCb[2], topCb[2], topLeftCb[2]);
		builder.predictDCLeftBorder(border, new PixelBuffer(actual, 0, 2));
		assertArrayEquals(expectedCb[2], actual);
		
		border = new BlockBorder(leftCb[3], topCb[3], topLeftCb[3]);
		builder.predictDCInside(border, new PixelBuffer(actual, 0, 2));
		assertArrayEquals(expectedCb[3], actual);
		
		border = new BlockBorder(leftCr[0], topCr[0], topLeftCr[0]);
		builder.predictDCInside(border, new PixelBuffer(actual, 0, 2));
		assertArrayEquals(expectedCr[0], actual);
		
		border = new BlockBorder(leftCr[1], topCr[1], topLeftCr[1]);
		builder.predictDCTopBorder(border, new PixelBuffer(actual, 0, 2));
		assertArrayEquals(expectedCr[1], actual);
		
		border = new BlockBorder(leftCr[2], topCr[2], topLeftCr[2]);
		builder.predictDCLeftBorder(border, new PixelBuffer(actual, 0, 2));
		assertArrayEquals(expectedCr[2], actual);
		
		border = new BlockBorder(leftCr[3], topCr[3], topLeftCr[3]);
		builder.predictDCInside(border, new PixelBuffer(actual, 0, 2));
		assertArrayEquals(expectedCr[3], actual);
	}
	
	public void testPlane() throws Exception {
		
		// MB 41
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
		
		int[] leftCb = new int[] {116, 116, 116, 116, 119, 119, 119, 119};
		int[] topCb = new int[] {118, 118, 118, 118, 119, 119, 119, 119};
		int topLeftCb = 113;

		int[] leftCr = new int[] {137, 137, 137, 137, 138, 138, 138, 138};
		int[] topCr = new int[] {132, 132, 132, 132, 132, 132, 132, 132};
		int topLeftCr = 141;
		
		BlockBorder borderCb = new BlockBorder(leftCb, topCb, topLeftCb);
		BlockBorder borderCr = new BlockBorder(leftCr, topCr, topLeftCr);

		ChromaIntraPredictionBuilder builder = new ChromaIntraPredictionBuilder(8, ChromaFormat.YUV_420);
		int[] actualCb = new int[64];
		int[] actualCr = new int[64];
		builder.predictPlane(borderCb, new PixelBuffer(actualCb, 0, 3));
		builder.predictPlane(borderCr, new PixelBuffer(actualCr, 0, 3));
		
		assertArrayEquals(expectedCb, actualCb);
		assertArrayEquals(expectedCr, actualCr);
	}
}