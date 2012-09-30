package org.jcodec.containers.matroska;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * High-level information about a semple
 * 
 * No binding to container information
 * 
 * @author Jay Codec
 * 
 */
public class Sample
{
	private boolean syncSample;
    public final long timestamp;
    private final long duration;
    public byte[] payload;

	public Sample(int track, long duration, boolean syncSample, long timestamp, byte[] payload)
	{
		this.duration = duration;
		this.syncSample = syncSample;
		this.timestamp = timestamp;
		this.payload = payload;
	}

    public long getDuration()
	{
		return duration;
    }

    public boolean isSyncSample()
	{
		return syncSample;
    }

    public long getTimestamp()
	{
		return timestamp;
    }

    public byte[] getPayload()
	{
		return payload;
	}
}
