package org.jcodec.containers.matroska.ebml;

import java.io.InputStream;
import java.io.IOException;

/* 	TODO: Get shot of this silly import.
	We need a better way to identify a tag type, 
	not using Matroska spec as a dictionary
*/
import org.jcodec.containers.matroska.MatroskaDocument;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Takes care of the business of reading EBML bitstreams
 * and returns correctly formed Tags.
 *
 * Protected from overreading by FixedSizeInputStream
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class EBMLReader
{
	private FixedSizeInputStream inputStream;
	private Tag lastTag;
	
	public EBMLReader(FixedSizeInputStream in)
	{
		this.inputStream = in;
	}

	public Tag getNext(TagType syncOn) throws IOException, EBMLException
	{
		byte[] readByte = new byte[1];
		byte[] tagID = syncOn.id;
		boolean outOfSync = true;
		int position = 0;
		while(outOfSync)
		{
			inputStream.read(readByte);
			if(readByte[0] == syncOn.id[position])
			{
				position++;
				if(position == syncOn.id.length)
				{
					outOfSync = false;
				}
			}
			else
			{
				position = 0;
			}
		}
		long tagSize = readSize();
		return getTag(tagID, tagSize);
	}
	
	public Tag getNextTag() throws IOException, EBMLException
	{
		if(lastTag != null) lastTag.skip();
		if(inputStream.available() < 1) return null;
		byte[] tagID = readEBML();
		long tagSize = readSize();
		return getTag(tagID, tagSize);
	}
		
	public byte[] readEBML() throws IOException, EBMLException
	{
		byte firstByte = (byte)inputStream.read();
		int numBytes = 0;
		//Begin by counting the bits unset before the first '1'.
		long mask = 0x0080;
		for (int i = 0; i < 8; i++)
		{
			if ((firstByte & mask) == mask)
			{
				numBytes = i + 1;
				break;
			}
			mask >>>= 1;
		}
		if (numBytes == 0) throw new EBMLException("Error reading tag. You may need to resynchronize the document.");
		byte[] data = new byte[numBytes];
		data[0] = firstByte;
		if (numBytes > 1)
		{
			inputStream.read(data, 1, numBytes - 1);
		}
		return data;
	}
	
	public long readSize() throws IOException, EBMLException
	{
		byte[] size = readEBML();
		if(size == null) throw new EBMLException("Error reading tag size. You may need to resynchronize the document.");
		byte firstByte = size[0];
		size[0] = (byte)(firstByte & ((0xFF >>> (size.length))));
		//Put this into a long
		long dataSize = 0;
		long n = 0;
		for (int i = 0; i < size.length; i++)
		{
			n = ((long)size[size.length - 1 - i] << 56) >>> 56;
			dataSize = dataSize | (n << (8 * i));
		}
		return dataSize;
	}
	
	
	/*
		TODO : Probably the wrong place for this.
		Reading the MatroskaDocument in package org.jcodec.containers.matroska
		is not very nice structure as EBML reading is a useful tool in its
		own right, and should not be tied like this to Matroska.
	*/
	private Tag getTag(byte[] tagID, long tagSize)
	{
		Tag tag;		
		switch (MatroskaDocument.getTagType(tagID))
		{		
			case TagType.TYPE_UNKNOWN :
			{
				tag = new Tag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}

			case TagType.TYPE_ROOT :
			{
				tag = new RootTag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}

			case TagType.TYPE_BINARY :
			{
				tag = new BinaryTag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}
			
			case TagType.TYPE_SINTEGER :
			{
				tag = new IntTag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}			

			case TagType.TYPE_UINTEGER :
			{
				tag = new UIntTag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}

			case TagType.TYPE_FLOAT :
			{
				tag = new FloatTag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}

			case TagType.TYPE_STRING :
			{
				tag = new StringTag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}

			case TagType.TYPE_ASCII_STRING :
			{
				tag = new StringTag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}

			case TagType.TYPE_DATE :
			{
				tag = new DateTag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}
		
			default :
			{
				tag = new Tag(tagID, new FixedSizeInputStream(inputStream, tagSize));
				break;
			}
		}
		lastTag = tag;
		return tag;	
	}
}