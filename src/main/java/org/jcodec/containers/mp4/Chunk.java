package org.jcodec.containers.mp4;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.jcodec.common.Tuple._2;

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

    public static Chunk createSameSizeAndDuration(long offset, long tv, int sampleSize, int sampleDuration,
            int sampleCount) {
        return new Chunk(offset, tv, sampleCount, sampleSize, null, sampleDuration, null, 1);
    }

    public static Chunk createSameSize(long offset, long tv, int sampleSize, int[] sampleDurations) {
        return new Chunk(offset, tv, sampleDurations.length, sampleSize, null, UNEQUAL_DUR, sampleDurations, 1);
    }

    public static Chunk createSameDuration(long offset, long tv, int[] sampleSizes, int sampleDuration) {
        return new Chunk(offset, tv, sampleSizes.length, UNEQUAL_SIZES, sampleSizes, sampleDuration, null, 1);
    }

    public static Chunk create(long offset, long tv, int[] sampleSizes, int sampleDurations[]) {
        if (sampleSizes.length != sampleDurations.length)
            throw new IllegalArgumentException("Sizes and durations array lenghts should match");
        return new Chunk(offset, tv, sampleSizes.length, UNEQUAL_SIZES, sampleSizes, UNEQUAL_DUR, sampleDurations, 1);
    }
    
    Chunk(long offset, long startTv, int sampleCount, int sampleSize, int[] sampleSizes, int sampleDur,
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

    public static Chunk createFrom(Chunk other) {
        return new Chunk(other.getOffset(), other.getStartTv(), other.getSampleCount(), other.getSampleSize(),
                other.getSampleSizes(), other.getSampleDur(), other.getSampleDurs(), other.getEntry());
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
        if (sampleSize != UNEQUAL_SIZES) {
            return (long)sampleSize * sampleCount;
        }
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

    public void setSampleSizes(int[] sampleSizes) {
        this.sampleSizes = sampleSizes;
    }

    public _2<Chunk, Chunk> split(long cutDur, boolean roundUp) {
        int drop = 0;
        long tvOff = 0;
        long byteOff = 0;
        for (int s = 0; s < sampleCount; s++) {
            long dur = sampleDur == Chunk.UNEQUAL_DUR ? sampleDurs[s] : sampleDur;
            long size = sampleSize == Chunk.UNEQUAL_SIZES ? sampleSizes[s] : sampleSize;
            if (dur > cutDur && !roundUp) {
                break;
            }
            drop++;
            tvOff += dur;
            byteOff += size;
            cutDur -= dur;
            if (cutDur < 0) {
                break;
            }
        }
        Chunk left = new Chunk(offset, startTv, drop, sampleSize,
                sampleSizes == null ? null : Arrays.copyOfRange(sampleSizes, 0, drop), sampleDur,
                sampleDurs == null ? null : Arrays.copyOfRange(sampleDurs, 0, drop), entry);
        Chunk right = new Chunk(offset + byteOff, startTv + tvOff, sampleCount - drop, sampleSize,
                sampleSizes == null ? null : Arrays.copyOfRange(sampleSizes, drop, sampleSizes.length), sampleDur,
                sampleDurs == null ? null : Arrays.copyOfRange(sampleDurs, drop, sampleDurs.length), entry);
        return new _2<Chunk, Chunk>(left, right);
    }
    
    public void unpackDurations() {
        if (sampleDur == UNEQUAL_DUR) {
            return;
        }
        sampleDurs = new int[sampleCount];
        Arrays.fill(sampleDurs, sampleDur);
        sampleDur = UNEQUAL_DUR;
    }
    
    public void unpackSampleSizes() {
        if (sampleSize == UNEQUAL_SIZES) {
            return;
        }
        sampleSizes = new int[sampleCount];
        Arrays.fill(sampleSizes, sampleSize);
        sampleSize = UNEQUAL_SIZES;
    }

    public void trimLastSample(long l) {
        if (l == 0) {
            return;
        }
        if (sampleCount == 0) {
            throw new IllegalStateException("Trimming empty chunk");
        }
        if (sampleDur != UNEQUAL_DUR) {
            unpackDurations();
        }
        if (sampleDurs[sampleCount - 1] < l)
            throw new IllegalArgumentException("Trimming more then one sample duration");
        sampleDurs[sampleCount - 1] -= l;
    }

    public void trimFirstSample(long l) {
        if (l == 0) {
            return;
        }
        if (sampleCount == 0) {
            throw new IllegalStateException("Trimming empty chunk");
        }
        if (sampleDur != UNEQUAL_DUR) {
            unpackDurations();
        }
        if (sampleDurs[0] < l)
            throw new IllegalArgumentException("Trimming more then one sample duration");
        sampleDurs[0] -= l;
        startTv += l;
    }
}
