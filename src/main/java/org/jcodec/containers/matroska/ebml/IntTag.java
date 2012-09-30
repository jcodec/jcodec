package org.jcodec.containers.matroska.ebml;

import java.io.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Extract long from EBML Signed Integer type.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */
 
public class IntTag extends Tag
{	
	public IntTag(byte[] id, FixedSizeInputStream in)
	{
		super(id, in);
		type = TagType.TYPE_SINTEGER;
		data = new byte[in.available()];
		try
		{
			in.read(data);
		}
		catch(IOException ex)
		{
		}	
	}
	
	public long getValue()
	{
		long l = 0;
		long tmp = 0;
		l |= ((long)data[0] << (56 - ((8 - data.length) * 8)));
		for (int i = 1; i < data.length; i++)
		{
			tmp = ((long)data[data.length - i]) << 56;
			tmp >>>= 56 - (8 * (i - 1));
			l |= tmp;
		}
		return l;
	}
}