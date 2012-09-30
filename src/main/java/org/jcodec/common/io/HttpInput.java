package org.jcodec.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * A source that reads from HTTP with random access
 * 
 * 
 * @author The JCodec project
 * 
 */
public class HttpInput extends RandomAccessInputStream {
	private URL url;
	private InputStream is;
	private long length;
	private long pos;

	public HttpInput(URL url) throws IOException {
		this.url = url;
		URLConnection connection = url.openConnection();
		is = connection.getInputStream();
		length = connection.getContentLength();
	}

	@Override
	public long getPos() {
		return pos;
	}

	@Override
	public int read() throws IOException {

		int r = is.read();
		++pos;
		return r;
	}

	@Override
	public int read(byte[] buf) throws IOException {
		int r = is.read(buf);
		pos += buf.length;
		return r;
	}

	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		int r = is.read(buf, off, len);
		pos += len;
		return r;
	}

	@Override
	public void seek(long where) throws IOException {
		if (is != null)
			IOUtils.closeQuietly(is);
		URLConnection con = url.openConnection();
		con.addRequestProperty("range", where + "-" + length);
		is = con.getInputStream();
		pos = where;
	}

	@Override
	public long skip(long i) {
		throw new RuntimeException("Not implemented");
	}

    @Override
    public long length() throws IOException {
        return length;
    }
}