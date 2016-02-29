package org.jcodec.codecs.h264.conformance;

import static org.jcodec.common.model.ColorSpace.YUV420;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.model.Picture;

/**
 * 
 * Reads raw yuv file
 * 
 * @author Jay Codec
 * 
 */
public class RawReader {
	private File rawFileName;
	private int width;
	private int height;

	private InputStream is;

	public RawReader(File rawFile, int width, int height) {
		this.rawFileName = rawFile;
		this.width = width;
		this.height = height;
	}

	public Picture readNextFrame() throws IOException {
		if (is == null) {
			is = new BufferedInputStream(new FileInputStream(rawFileName));
			if (is == null)
				return null;
		}

		return readFrame();
	}

	private Picture readFrame() throws IOException {

		int size = width * height;
		int[] luma = new int[size];
		int[] cb = new int[size >> 2];
		int[] cr = new int[size >> 2];

		int first = is.read();
		if (first == -1)
			return null;
		luma[0] = first;
		for (int i = 1; i < size; i++) {
			luma[i] = is.read();
		}

		for (int i = 0; i < (size >> 2); i++) {
			cb[i] = is.read();
		}

		for (int i = 0; i < (size >> 2); i++) {
			cr[i] = is.read();
		}

		return Picture.createPicture(width, height, new int[][] {luma, cb, cr}, YUV420);
	}

}
