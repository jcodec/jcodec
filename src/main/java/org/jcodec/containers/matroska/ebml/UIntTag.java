package org.jcodec.containers.matroska.ebml;

import java.io.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Extract long from EBML UnsignedSigned Integer type.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */
public class UIntTag extends Tag
{	
	public UIntTag(byte[] id, FixedSizeInputStream in)
	{
		super(id, in);
		type = TagType.TYPE_UINTEGER;
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
		for (int i = 0; i < data.length; i++)
		{
			tmp = ((long)data[data.length - 1 - i]) << 56;
			tmp >>>= (56 - (i * 8));
			l |= tmp;
		}
		return l;
	}
}
