package org.jcodec.codecs.util;

import java.io.FileInputStream;
import java.io.IOException;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.model.Picture;

public class FrameUtil {

	
	public static void displayComponent(int[] component, int width, int height,
			boolean decorate) {
		int widthInBlks = width >> 2;
		int heightInBlks = height >> 2;
		int stride = width;

		for (int blkY = 0; blkY < heightInBlks; blkY++) {
			if (decorate) {
				if (blkY % 4 == 0) {
					for (int i = 0; i < stride; i++) {
						System.out.print("====");
					}
					System.out.print("=========================");
				} else {
					for (int i = 0; i < stride; i++) {
						System.out.print("----");
					}
					System.out.print("-------------------------");
				}
			}
			System.out.println();
			for (int j = 0; j < 4; j++) {
				for (int blkX = 0; blkX < widthInBlks; blkX++) {
					for (int i = 0; i < 4; i++) {
						int offset = ((blkY << 2) + j) * stride
								+ ((blkX << 2) + i);
						if (decorate)
							System.out.printf("%3d ", component[offset]);
						else
							System.out.printf("%3d, ", component[offset]);
					}
					if (decorate) {
						if (blkX % 4 == 0)
							System.out.print(" || ");
						else
							System.out.print(" | ");
					}
				}
				System.out.println();
			}

		}
	}

	public static void main(String[] args) {
		FileInputStream is = null;
		try {
			is = new FileInputStream(args[0]);
			Picture pgm = PGMIO.readPGM(is);
			displayComponent(pgm.getPlaneData(0), pgm.getWidth(), pgm.getHeight(), false);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
}
