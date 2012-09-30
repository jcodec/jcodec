package org.jcodec.containers.matroska.ebml;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Provides a structure for tag types.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class TagType
{
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_ROOT = 1;
	public static final int TYPE_BINARY = 2;
	public static final int TYPE_SINTEGER = 3;
	public static final int TYPE_UINTEGER = 4;
	public static final int TYPE_FLOAT = 5;
	public static final int TYPE_STRING = 6;
	public static final int TYPE_ASCII_STRING = 7;
	public static final int TYPE_DATE = 8;
	
	public String name;
	public byte[] id;
	public int type;

	public TagType(String name, int type, byte[] id)
	{
		this.id = id;
		this.type = type;
		this.name = name;
	}
}