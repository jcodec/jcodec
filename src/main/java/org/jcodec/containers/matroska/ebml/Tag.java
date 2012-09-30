package org.jcodec.containers.matroska.ebml;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Base class for all EBML tag types.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class Tag
{
	public byte[] id;
	public byte[] data;
	public FixedSizeInputStream in;
	public int size;
	public int type;
	
	public Tag(byte[] id, FixedSizeInputStream in)
	{
		this.id = id;
		this.in = in;
		this.type = type;
		size = in.available();
	}
	
	public int getType()
	{
		return type;
	}
	
	public void skip() throws IOException
	{
		in.skipToEnd();
	}
	
	public byte[] getID()
	{
		return id;
	}
	
	public int getSize()
	{
		return size;
	}
}