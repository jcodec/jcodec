package org.jcodec.containers.matroska.ebml;

import java.io.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Extract String from EBML String type.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class StringTag extends Tag
{
	private String charset = "UTF-8";
	
	public StringTag(byte[] id, FixedSizeInputStream in)
	{
		super(id, in);
		type = TagType.TYPE_ASCII_STRING;
		data = new byte[size];
		try
		{
			in.read(data);
		}
		catch(IOException ex)
		{
		}		
	}
	
	public String getValue()
	{
		try
		{
			return new String(data, "UTF8");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return "UNSUPPORTED ENCODING";
		}
    }
}