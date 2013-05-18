package org.jcodec.codecs.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.jcodec.common.IOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

public class PGMIO {

	public static Picture readPGM(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		String p5 = dis.readLine();
		if (!p5.equals("P5")) {
			throw new IOException("Only P5 is supported");
		}
		String dim = dis.readLine();
		String depth = dis.readLine();

		String[] tmp = dim.split(" ");
		int width = Integer.parseInt(tmp[0]);
		int height = Integer.parseInt(tmp[1]);

		byte[] buf = new byte[width * height];
		int[] y = new int[width * height];

		int read = dis.read(buf, 0, width * height);

		if (read != width * height) {
			throw new IOException("Could not read data fully");
		}

		for (int i = 0; i < width * height; i++) {
			y[i] = buf[i] & 0xff;
		}

		return new Picture(width, height, new int[][] {y}, ColorSpace.GREY);
	}

	public static Picture readPGM(File name) throws IOException {
		InputStream is = null;

		try {
			is = new BufferedInputStream(new FileInputStream(name));
			return readPGM(is);
		} finally {
			IOUtils.closeQuietly(is);
		}

	}

	public static void savePGM(Picture ref, String string) throws IOException {
		OutputStream out = null;
		try {
			out = new FileOutputStream(string);
			PrintStream ps = new PrintStream(new BufferedOutputStream(out));
			ps.println("P5");
			ps.println(ref.getWidth() + " " + ref.getHeight());
			ps.println("255");
			ps.flush();
			int[] data = ref.getPlaneData(0);
			for (int i = 0; i < data.length; i++)
				ps.write(data[i]);
			ps.flush();
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
}
