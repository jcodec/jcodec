package org.jcodec.containers.matroska.ebml;

import java.io.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Extract byte[] from EBML Binary type.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class BinaryTag extends Tag
{	
	public BinaryTag(byte[] id, FixedSizeInputStream in)
	{
		super(id, in);
		type = TagType.TYPE_BINARY;
		data = new byte[in.available()];
		try
		{
			in.read(data);
		}
		catch(IOException ex)
		{
		}	
	}
		
	public byte[] getValue()
	{
		return data;
	}
}