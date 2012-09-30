package org.jcodec.codecs.h264.decode;

import java.io.File;

import org.jcodec.codecs.h264.JAVCTestCase;
import org.jcodec.codecs.h264.decode.model.MVMatrix;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.codecs.util.PGMIO;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Point;
import org.junit.Test;

public class TestChromaInterPredictionBuilder extends JAVCTestCase {

    @Test
    public void testMB1() throws Exception {
	
		int[] expected = new int[] {
			129, 130, 130, 130, 130, 130, 130, 130,
        	129, 130, 130, 130, 130, 130, 130, 130, 
        	129, 130, 130, 130, 130, 130, 130, 130, 
        	129, 130, 130, 130, 130, 130, 130, 130, 
        	129, 130, 130, 130, 130, 130, 130, 130, 
        	129, 130, 130, 130, 130, 130, 130, 130, 
        	129, 130, 130, 130, 130, 130, 130, 130, 
        	129, 130, 130, 130, 130, 130, 130, 130 
		};
	
		Picture rawRef = PGMIO
				.readPGM(new File("src/test/resources/h264/ref_d0cb.pgm"));

		Interpolator interpolator = new Interpolator();

		int refWidth = rawRef.getWidth();
		int refHeight = rawRef.getHeight();
		int[] interpolated = interpolator.interpolateChroma(rawRef.getPlaneData(0),
				refWidth, refHeight);

		Picture reference = new Picture(refWidth * 8 + 128,
				refHeight * 8 + 128, new int[][] {null, interpolated, null}, ColorSpace.YUV420);

		ChromaInterPredictionBuilder predictionBuilder = new ChromaInterPredictionBuilder();

		MVMatrix mvMatrix = new MVMatrix(new Vector[] { new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0) });

		int[] result = predictionBuilder.predictCb(
				new Picture[] { reference }, mvMatrix, new Point(16, 0));

		assertArrayEquals(expected, result);
	}
    
    public void testMB2() throws Exception {
		int[] expected = new int[] {
			130, 130, 130, 130, 127, 126, 126, 126,
			130, 130, 130, 130, 127, 126, 126, 126,
			130, 130, 130, 130, 127, 126, 126, 126,
			130, 130, 130, 130, 127, 126, 126, 126,
			130, 130, 130, 130, 127, 126, 126, 126,
			130, 130, 130, 130, 127, 126, 126, 126,
			130, 130, 130, 130, 127, 126, 126, 126,
			130, 130, 130, 130, 127, 126, 126, 126
		};
	
		Picture rawRef = PGMIO
				.readPGM(new File("src/test/resources/h264/ref_d0cb.pgm"));

		Interpolator interpolator = new Interpolator();

		int refWidth = rawRef.getWidth();
		int refHeight = rawRef.getHeight();
		int[] interpolated = interpolator.interpolateChroma(rawRef.getPlaneData(0),
				refWidth, refHeight);

		Picture reference = new Picture(refWidth * 8 + 128,
				refHeight * 8 + 128, new int[][] {null, interpolated, null}, ColorSpace.YUV420);

		ChromaInterPredictionBuilder predictionBuilder = new ChromaInterPredictionBuilder();

		MVMatrix mvMatrix = new MVMatrix(new Vector[] { new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(64, -28, 0),
				new Vector(64, -28, 0), new Vector(64, -28, 0),
				new Vector(64, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(52, -28, 0),
				new Vector(52, -28, 0), new Vector(64, -28, 0),
				new Vector(64, -28, 0), new Vector(64, -28, 0),
				new Vector(64, -28, 0) });

		int[] result = predictionBuilder.predictCb(
				new Picture[] { reference }, mvMatrix, new Point(32, 0));

		assertArrayEquals(expected, result);
	}
}
