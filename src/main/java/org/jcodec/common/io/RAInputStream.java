package org.jcodec.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * An interface for random access source
 * 
 * needed to correctly find metadata of mp4 container which can be located in
 * the end of the file as well
 * 
 * @author The JCodec project
 * 
 */
public abstract class RAInputStream extends InputStream {
	/**
	 * Seeks to the certain absolute position in this input
	 * 
	 * @param where
	 * @throws IOException
	 */
	public abstract void seek(long where) throws IOException;

	/**
	 * Returns the current position in the input
	 * 
	 * @return
	 * @throws IOException
	 */
	public abstract long getPos() throws IOException;
	
	public abstract long length() throws IOException;
}
