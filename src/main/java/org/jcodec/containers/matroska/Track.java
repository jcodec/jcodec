package org.jcodec.containers.matroska;

import java.util.LinkedList;
import java.io.InputStream;
import java.io.IOException;

import org.jcodec.containers.matroska.ebml.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Provides ability to read elementary streams
 * from a Matroska track.
 *
 * Provides a structure for information about an
 * audio / video track.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class Track extends InputStream
{
	public static int TRACK_VIDEO		= 1; 
	public static int TRACK_AUDIO		= 2; 
	public static int TRACK_COMPLEX		= 3; 
	public static int TRACK_LOGO		= 10; 
	public static int TRACK_SUBTITLE	= 11; 
	public static int TRACK_CONTROL		= 20;
	
	private final int max_samples = 256;
	private final int buffer_size = max_samples * 1024;
	
	// All tracks
	public int trackNumber;
	public long trackUID;
	public int trackType;
	public long defaultTrackDuration;
	public String trackName;
	public String trackLanguage;
	public String trackCodecID;
	public boolean trackCodecPrivate;
	
	// Audio tracks
	public float audioTrackSamplingFrequency;
	public float audioTrackOutputSamplingFrequency;
	public int audioTrackChannels;
	public int audioTrackBitDepth;
	
	// Video tracks
	public int videoTrackPixelWidth;
	public int videoTrackPixelHeight;
	public int videoTrackDisplayWidth;
	public int videoTrackDisplayHeight;
	
	private int read_offset = buffer_size;
	private int write_offset = 0;
	private int bytes_in_buffer = 0;
	private int samples_in_buffer = 0;
	
	private byte[] trackData;
	
	private long[] trackTimeStamps;
	private int[] sampleOffsets;
	
	private boolean hasCodecPrivate;
	private byte[] codecPrivate;
	
	private MatroskaParser parent;
	
	public Track(MatroskaParser parent)
	{
		trackData = new byte[buffer_size];
		trackTimeStamps = new long[max_samples];
		sampleOffsets = new int[max_samples];	
		this.parent = parent;
    }
	
	public int getBytesBuffered()
	{
		return bytes_in_buffer;
	}

	public void addBlock(MatroskaBlock mb, long refTimeCode)
	{		
		if(mb.isSeekable() && hasCodecPrivate)
		{
			if((write_offset + codecPrivate.length) > trackData.length)
			{
				int copyToStart = ((write_offset + codecPrivate.length) - trackData.length);
				int copyToEnd = codecPrivate.length - copyToStart;
				System.arraycopy(codecPrivate, 0, trackData, write_offset, copyToEnd);
				System.arraycopy(codecPrivate, copyToEnd, trackData, 0, copyToStart);
			}
			else
			{
				System.arraycopy(codecPrivate, 0, trackData, write_offset, codecPrivate.length);
			}	
			write_offset+=codecPrivate.length;
			if(write_offset >= buffer_size) write_offset-=buffer_size;
			bytes_in_buffer+=codecPrivate.length;
		}
		
		for(int i=0; i<mb.getFrameCount(); i++)
		{
			int bytesCopied = mb.frameCopy(i, trackData, write_offset);
			write_offset+=bytesCopied;
			if(write_offset >= buffer_size) write_offset-=buffer_size;
			bytes_in_buffer+=bytesCopied;
		}
	}
	
	public void addCodecPrivate(byte[] codecPrivate)
	{
		hasCodecPrivate = true;
		this.codecPrivate = codecPrivate;
		if((write_offset + codecPrivate.length) >= trackData.length)
		{
			int copyToStart = ((write_offset + codecPrivate.length) - trackData.length);
			int copyToEnd = codecPrivate.length - copyToStart;
			System.arraycopy(codecPrivate, 0, trackData, write_offset, copyToEnd);
			System.arraycopy(codecPrivate, copyToEnd, trackData, 0, copyToStart);
		}
		else
		{
			System.arraycopy(codecPrivate, 0, trackData, write_offset, codecPrivate.length);
		}	
		write_offset+=codecPrivate.length;
		if(write_offset >= buffer_size) write_offset-=buffer_size;
		bytes_in_buffer+=codecPrivate.length;		
	}
		
	public int read() throws IOException
    {
		while(bytes_in_buffer == 0)
		{
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException ex3)
			{
			}
		}
		read_offset++;
		bytes_in_buffer--;
		if(read_offset >= buffer_size) read_offset = 0;
		return (int)trackData[read_offset] & 0xFF;
	}
}