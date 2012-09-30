package org.jcodec.codecs.h264.testtool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A special input stream that puts all the data into a 'pocket'
 * 
 * The contents of pocket can be accessed at any time as well as erased
 * 
 * 
 * @author Jay Codec
 * 
 */
public class PocketInputStream extends InputStream {

	private ByteArrayOutputStream baos;
	private InputStream src;

	public PocketInputStream(InputStream src) {
		this.src = src;

		baos = new ByteArrayOutputStream();
	}

	@Override
	public int read() throws IOException {

		int val = src.read();
		baos.write(val);

		return val;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int res = src.read(b, off, len);
		baos.write(b, off, len);

		return res;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int res = src.read(b);
		baos.write(b);

		return res;
	}

	@Override
	public int available() throws IOException {
		return src.available();
	}

	@Override
	public void close() throws IOException {
		src.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		throw new RuntimeException("CCCC!!!0000");
	}

	@Override
	public boolean markSupported() {
		throw new RuntimeException("BBBB!!!0000");
	}

	@Override
	public long skip(long n) throws IOException {
		throw new RuntimeException("AAAAA!!!0000");
	}

	public byte[] getPocket() {
		return baos.toByteArray();
	}

	public void reset() {
		baos = new ByteArrayOutputStream();
	}

}
