package org.jcodec.containers.matroska.ebml;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Usually thrown when malformed EBML code is encountered.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class EBMLException extends Exception
{	
	public EBMLException(String msg)
	{
		super(msg);
	}	
}