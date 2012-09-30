package org.jcodec.containers.matroska.ebml;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Creates a new parent type from EBML root tag.
 *
 * Children of type Tag are returned by read method
 * until no more remain and null is returned.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class RootTag extends Tag
{
	EBMLReader myEBMLReader;
	
	public RootTag(byte[] id, FixedSizeInputStream in)
	{
		super(id, in);
		type = TagType.TYPE_ROOT;
		myEBMLReader = new EBMLReader(in);
	}
		
	public Tag read() throws IOException, EBMLException
	{
		if(in.available() > 0)
		{
			return myEBMLReader.getNextTag();
		}
		else
		{
			return null;
		}
	}
	
	public Tag syncOn(TagType t) throws IOException, EBMLException
	{
		return myEBMLReader.getNext(t);
	}
}