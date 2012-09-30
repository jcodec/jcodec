package org.jcodec.containers.matroska.ebml;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * FixedSizeInputStream extends InputStream to prevent
 * over-reading. 
 *
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class FixedSizeInputStream extends InputStream
{
	private int inputStreamSize = 0;
	private int usedBytes = 0;
	private InputStream inputStream;

	public FixedSizeInputStream(InputStream in, long length)
	{
		inputStream = in;
		inputStreamSize = (int)length;
	}

	public int read() throws IOException
	{
		if (inputStreamSize == -1)
		{
			int i = inputStream.read();
			if (i == -1) inputStreamSize = usedBytes;
			return i;
		}
		if (usedBytes < inputStreamSize)
		{
			usedBytes++;
			return inputStream.read();
		}
		return -1;
	}

	public int available()
	{
		if ((inputStreamSize == -1) || (inputStreamSize - usedBytes > Integer.MAX_VALUE)) return Integer.MAX_VALUE;
		return inputStreamSize - usedBytes;
	}

	public void skipToEnd() throws IOException
	{
		while(!(read() == -1));	
	}
}