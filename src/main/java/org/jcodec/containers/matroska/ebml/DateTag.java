package org.jcodec.containers.matroska.ebml;

import java.io.*;
import java.util.Date;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Extract Date from EBML Date type.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class DateTag extends Tag
{
	public static long UnixEpochDelay = 978307200; // 2001/01/01 00:00:00 UTC
	
	public DateTag(byte[] id, FixedSizeInputStream in)
	{
		super(id, in);
		type = TagType.TYPE_DATE;
		data = new byte[size];
		try
		{
			in.read(data);
		}
		catch(IOException ex)
		{
		}			
	}
	
	public Date getValue()
	{
		long l = 0;
		long tmp = 0;
		for (int i = 0; i < data.length; i++)
		{
			tmp = ((long)data[data.length - 1 - i]) << 56;
			tmp >>>= (56 - (i * 8));
			l |= tmp;
		}		
		l = l / 1000000000 + UnixEpochDelay;
		return new Date(l);
    }
}