package org.jcodec.containers.mp4;

import java.nio.ByteBuffer;

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
}