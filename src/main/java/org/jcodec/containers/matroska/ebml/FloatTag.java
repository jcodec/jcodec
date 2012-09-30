package org.jcodec.containers.matroska.ebml;

import java.io.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Extract double from EBML Float type.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

 public class FloatTag extends Tag
{
	public FloatTag(byte[] id, FixedSizeInputStream in)
	{
		super(id, in);
		type = TagType.TYPE_FLOAT;
		data = new byte[in.available()];
		try
		{
			in.read(data);
		}
		catch(IOException ex)
		{
		}	
	}
	
	public double getValue()
	{
		try
		{
			if (size == 4)
			{
				float value = 0;
				ByteArrayInputStream bIS = new ByteArrayInputStream(data);
				DataInputStream dIS = new DataInputStream(bIS);
				value = dIS.readFloat();
				return value;
			}
			else if (size == 8)
			{
				double value = 0;
				ByteArrayInputStream bIS = new ByteArrayInputStream(data);
				DataInputStream dIS = new DataInputStream(bIS);
				value = dIS.readDouble();
				return value;
			}
			else 
			{
				throw new ArithmeticException("80-bit floats are not supported");
			}
		}
		catch (IOException ex)
		{
			return 0;
		}
	}
}