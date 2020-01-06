package org.jcodec.containers.mp4;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class Chunk {
    public static final int UNEQUAL_DUR = -1;
    public static final int UNEQUAL_SIZES = -1;
    private long offset;
    private long startTv;
    private int sampleCount;
    private int sampleSize;
    private int sampleSizes[];
    private int sampleDur;
    private int sampleDurs[];
    private int entry;
    private ByteBuffer data;

    public Chunk(long offset, long startTv, int sampleCount, int sampleSize, int[] sampleSizes, int sampleDur,
            int[] sampleDurs, int entry) {
        this.offset = offset;
        this.startTv = startTv;
        this.sampleCount = sampleCount;
        this.sampleSize = sampleSize;
        this.sampleSizes = sampleSizes;
        this.sampleDur = sampleDur;
        this.sampleDurs = sampleDurs;
        this.entry = entry;
    }

    public long getOffset() {
        return offset;
    }

    public long getStartTv() {
        return startTv;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int[] getSampleSizes() {
        return sampleSizes;
    }

    public int getSampleDur() {
        return sampleDur;
    }

    public int[] getSampleDurs() {
        return sampleDurs;
    }

    public int getEntry() {
        return entry;
    }

    public int getDuration() {
        if (sampleDur != UNEQUAL_DUR)
            return sampleDur * sampleCount;
        int sum = 0;
        for (int j = 0; j < sampleDurs.length; j++) {
            int i = sampleDurs[j];
            sum += i;
        }
        return sum;
    }

    public long getSize() {
        if (sampleSize != UNEQUAL_SIZES)
            return sampleSize * sampleCount;
        long sum = 0;
        for (int j = 0; j < sampleSizes.length; j++) {
            int i = sampleSizes[j];
            sum += i;
        }
        return sum;
    }

	public ByteBuffer getData() {
		return data;
	}

	public void setData(ByteBuffer data) {
		this.data = data;
	}

	public void setStartTv(long startTv) {
		this.startTv = startTv;
	}

	public void dropFrontSamples(int drop) {
	    if (sampleSize == UNEQUAL_SIZES) {
	    	for (int i = 0; i < drop; i++) {
	    		offset += sampleSizes[i];
	    		if (data != null)
	    			NIOUtils.skip(data, sampleSizes[i]);
	    	}
	    	sampleSizes = Arrays.copyOfRange(sampleSizes, drop, sampleSizes.length);
	    } else {
	    	offset += sampleSize * drop;
	    	NIOUtils.skip(data, sampleSize * drop);
	    }
	
	    if (sampleDur == UNEQUAL_DUR) {
	    	sampleDurs = Arrays.copyOfRange(sampleDurs, drop, sampleDurs.length);
	    }
	    sampleCount -= drop;
	}
	
	public void dropTailSamples(int drop) {
	    if (sampleSize == UNEQUAL_SIZES) {
	    	sampleSizes = Arrays.copyOf(sampleSizes, sampleSizes.length - drop);
	    }
	
	    if (sampleDur == UNEQUAL_DUR) {
	    	sampleDurs = Arrays.copyOf(sampleDurs, sampleDurs.length - drop);
	    }
	    sampleCount -= drop;
	}

	public boolean trimFront(long cutDur) {
		if (sampleDur != Chunk.UNEQUAL_DUR && sampleCount != 1)
			return false;
		
		startTv += cutDur;
		if (sampleCount > 1) {
	    	int drop = 0;
	    	for (int s = 0; s < sampleCount; s++) {
	    		long dur = sampleDurs[s];
	    		if (dur > cutDur)
	    			break;
				drop++;
				cutDur -= dur;
	        }
	    	dropFrontSamples(drop);
		}
    	if (sampleDur == Chunk.UNEQUAL_DUR)
    		sampleDurs[0] -= cutDur;
    	else
    		sampleDur -= cutDur;
    	
    	return true;
	}
	
	public boolean trimTail(long cutDur) {
		if (sampleDur != Chunk.UNEQUAL_DUR && sampleCount != 1)
			return false;
		if (sampleCount > 1) {
			int drop = 0;
	    	for (int s = 0; s < sampleCount; s++) {
	    		long dur = sampleDurs[sampleCount - s - 1];
	    		if (dur > cutDur)
	    			break;
				drop++;
				cutDur -= dur;
	        }
	    	dropTailSamples(drop);
		}
		if (sampleDur == Chunk.UNEQUAL_DUR)
			sampleDurs[sampleDurs.length - 1] -= cutDur;
    	else
    		sampleDur -= cutDur;
    	
    	return true;
	}
}