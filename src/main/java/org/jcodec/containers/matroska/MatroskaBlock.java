package org.jcodec.containers.matroska;

import org.jcodec.containers.matroska.ebml.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Parses data from a Matroska Block
 *
 * Provides a structure for Matroska Block data.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */
 
public class MatroskaBlock
{
	protected int [] Sizes = null;
	protected int HeaderSize = 0;
	protected int BlockTimecode = 0;
	protected int TrackNo = 0;
	protected boolean seekable = false;
	private byte[] data;
	
	public MatroskaBlock(byte[] data)
	{
		this.data = data;
		parseBlock();
	}

	public void parseBlock()
	{
		int index = 0;
		TrackNo = (int)EBML.readEBMLCode(data);
		index = EBML.codedSizeLength(TrackNo);
		HeaderSize += index;
		short BlockTimecode1 = (short)(data[index++] & 0xFF);
		short BlockTimecode2 = (short)(data[index++] & 0xFF);
		if (BlockTimecode1 != 0 || BlockTimecode2 != 0)
		{
			BlockTimecode = (BlockTimecode1 << 8) | BlockTimecode2;
		}
		
		int flags = data[index++] & 0xFF;		
		int LaceFlag = flags & 0x06;
		int SeekableFlag = flags & 0x80;
		
		if(SeekableFlag == 0x80) seekable = true;
		HeaderSize += 3;
		if (LaceFlag != 0x00)
		{
			System.out.println("LACED");
			byte LaceCount = data[index++];
			HeaderSize += 1;
			if (LaceFlag == 0x02)
			{
				Sizes = readXiphLaceSizes(index, LaceCount);
			}
			else if (LaceFlag == 0x06)
			{
				Sizes = readEBMLLaceSizes(index, LaceCount);
			}
			else if (LaceFlag == 0x04)
			{
				Sizes = new int[LaceCount+1];
				Sizes[0] = (int)(data.length - HeaderSize) / (LaceCount+1);
				for (int s = 0; s < LaceCount; s++)
				Sizes[s+1] = Sizes[0];
			}
			else
			{
				throw new RuntimeException("Unsupported lacing type flag.");
			}
		}
	}

	public int[] readEBMLLaceSizes(int index, short LaceCount)
	{
		int [] LaceSizes = new int[LaceCount+1];
		LaceSizes[LaceCount] = data.length;
		int startIndex = index;
		LaceSizes[0] = (int)EBML.readEBMLCode(data, index);
		index += EBML.codedSizeLength(LaceSizes[0]);
		LaceSizes[LaceCount] -= LaceSizes[0];
		long FirstEBMLSize = LaceSizes[0];
		long LastEBMLSize = 0;
		for (int l = 0; l < LaceCount-1; l++)
		{
			LastEBMLSize = EBML.readSignedEBMLCode(data, index);
			index += EBML.codedSizeLength(LastEBMLSize);
			FirstEBMLSize += LastEBMLSize;
			LaceSizes[l+1] = (int)FirstEBMLSize;
			LaceSizes[LaceCount] -= LaceSizes[l+1];
		}
		HeaderSize = HeaderSize + (int)(index - startIndex);
		LaceSizes[LaceCount] -= HeaderSize;
		return LaceSizes;
	}

	public int[] readXiphLaceSizes(int index, short LaceCount)
	{
		int [] LaceSizes = new int[LaceCount+1];
		LaceSizes[LaceCount] = data.length;
		for (int l = 0; l < LaceCount; l++)
		{
			short LaceSizeByte = 255;
			while (LaceSizeByte == 255)
			{
				LaceSizeByte = (short)(data[index++] & 0xFF);
				HeaderSize += 1;
				LaceSizes[l] += LaceSizeByte;
			}
			LaceSizes[LaceCount] -= LaceSizes[l];
		}
		LaceSizes[LaceCount] -= HeaderSize;
		return LaceSizes;
	}

	public int getFrameCount()
	{
		if (Sizes == null)
		{
			return 1;
		}
		return Sizes.length;
	}

	public byte [] getFrame(int frame)
	{
		if (Sizes == null)
		{
			int StartOffset = HeaderSize;
			byte [] FrameData = new byte[data.length - HeaderSize];
			System.arraycopy(data, StartOffset, FrameData, 0, FrameData.length);
			return FrameData;
		}		
		byte [] FrameData = new byte[Sizes[frame]];
		int StartOffset = HeaderSize;
		for (int s = 0; s < frame; s++)
		{
			StartOffset += Sizes[s];
		}
		System.arraycopy(data, StartOffset, FrameData, 0, FrameData.length);
		return FrameData;
	}

	// Faster than getFrame, but not as pretty.
	public int frameCopy(int frame, byte[] buffer, int bufferStartOffset)
	{
		int bytesCopied;
		if (Sizes == null)
		{
			int StartOffset = HeaderSize;
			bytesCopied = data.length - HeaderSize;
			if((bufferStartOffset + bytesCopied) > buffer.length)
			{
				int copyToStart = ((bufferStartOffset + bytesCopied) - buffer.length);
				int copyToEnd = bytesCopied - copyToStart;
				System.arraycopy(data, StartOffset, buffer, bufferStartOffset, copyToEnd);
				System.arraycopy(data, StartOffset + copyToEnd, buffer, 0, copyToStart);
			}
			else
			{
				System.arraycopy(data, StartOffset, buffer, bufferStartOffset, bytesCopied);
			}
			return bytesCopied;
		}
		
		bytesCopied = Sizes[frame];
		int StartOffset = HeaderSize;
		for (int s = 0; s < frame; s++)
		{
			StartOffset += Sizes[s];
		}
		
		if((bufferStartOffset + bytesCopied) > buffer.length)
		{
			int copyToStart = ((bufferStartOffset + bytesCopied) - buffer.length);
			int copyToEnd = bytesCopied - copyToStart;
			System.arraycopy(data, StartOffset, buffer, bufferStartOffset, copyToEnd);
			System.arraycopy(data, StartOffset + copyToEnd, buffer, 0, copyToStart);
		}
		else
		{
			System.arraycopy(data, StartOffset, buffer, bufferStartOffset, bytesCopied);
		}
		return bytesCopied;	
	}
	
	
	public long getAdjustedBlockTimecode(long ClusterTimecode, long TimecodeScale)
	{
		return ClusterTimecode + (BlockTimecode);
	}

	public int getTrackNo()
	{
		return TrackNo;
	}

	public int getBlockTimecode()
	{
		return BlockTimecode;
	}
	
	public boolean isSeekable()
	{
		return seekable;
	}
}