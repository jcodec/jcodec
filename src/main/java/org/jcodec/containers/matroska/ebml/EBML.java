package org.jcodec.containers.matroska.ebml;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Some static methods to read EBML codes and lengths
 * from byte arrays.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class EBML
{
	public static long readEBMLCode(byte [] source) 
	{
		return readEBMLCode(source, 0);
	}

	public static long readEBMLCode(byte [] source, int offset) 
	{
		byte firstByte = source[offset];
		int numBytes = 0;
		long mask = 0x0080;
		for (int i = 0; i < 8; i++) 
		{
			if ((firstByte & mask) == mask) 
			{
				numBytes = i + 1;
				i = 8;
			}
			mask >>>= 1;
		}
		if (numBytes == 0) return 0;
		byte[] data = new byte[numBytes];
		data[0] = (byte)(firstByte & ((0xFF >>> (numBytes))));
		if (numBytes > 1) 
		{
			System.arraycopy(data, 1, source, offset+1, numBytes - 1);
		}
		long size = 0;
		long n = 0;
		for (int i = 0; i < numBytes; i++) 
		{
			n = ((long)data[numBytes - 1 - i] << 56) >>> 56;
			size = size | (n << (8 * i));
		}
		return size;
	}

	public static long readSignedEBMLCode(byte [] source) 
	{
		return readSignedEBMLCode(source, 0);
	}

	public static long readSignedEBMLCode(byte [] source, int offset) 
	{
		byte firstByte = source[offset];
		int numBytes = 0;
		long mask = 0x0080;
		for (int i = 0; i < 8; i++) 
		{
			if ((firstByte & mask) == mask) 
			{ 
				numBytes = i + 1;
				i = 8;
			}
			mask >>>= 1;
		}
		if (numBytes == 0) return 0;
		byte[] data = new byte[numBytes];
		data[0] = (byte)(firstByte & ((0xFF >>> (numBytes))));
		if (numBytes > 1) 
		{
			System.arraycopy(data, 1, source, offset+1, numBytes - 1);
		}
		long size = 0;
		long n = 0;
		for (int i = 0; i < numBytes; i++) 
		{
			n = ((long)data[numBytes - 1 - i] << 56) >>> 56;
			size = size | (n << (8 * i));
		}
		if (numBytes == 1) 
		{
			size -= 63;
		} 
		else if (numBytes == 2) 
		{
			size -= 8191;
		} 
		else if (numBytes == 3) 
		{
			size -= 1048575;
		}
		else if (numBytes == 4) 
		{
			size -= 134217727;
		}
		return size;
	}
	
	public static int codedSizeLength(long value)
	{
		int codedSize = 0;
		if (value < 127)
		{
			codedSize = 1;
		}
		else if (value < 16383)
		{
			codedSize = 2;
		}
		else if (value < 2097151)
		{
			codedSize = 3;
		}
		else if (value < 268435455)
		{
			codedSize = 4;
		}
		return codedSize;
	}	
}