package org.jcodec.samples.player.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class TestFixedLevelInputStream {
	public static void main(String[] args) throws Exception {
		FixedLevelInputStream stream = new FixedLevelInputStream(
				new BufferedInputStream(
						new URL("http://jcodec.org/admiral.264").openStream()),
				1024 * 1024, 5 * 1024 * 1024);
		BufferedOutputStream outputStream = new BufferedOutputStream(
				new FileOutputStream("/tmp/test.264"));
		IOUtils.copy(stream, outputStream);

		outputStream.flush();
		outputStream.close();

	}

}
