package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A box containing sample presentation time information
 * 
 * @author Jay Codec
 * 
 */
public class TimeToSampleBox extends FullBox {

    public static class TimeToSampleEntry {
        int sampleCount;
        int sampleDuration;

        public TimeToSampleEntry(int sampleCount, int sampleDuration) {
            this.sampleCount = sampleCount;
            this.sampleDuration = sampleDuration;
        }

        public int getSampleCount() {
            return sampleCount;
        }

        public int getSampleDuration() {
            return sampleDuration;
        }

        public void setSampleDuration(int sampleDuration) {
            this.sampleDuration = sampleDuration;
        }

        public void setSampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
        }

        public long getSegmentDuration() {
            return sampleCount * sampleDuration;
        }
    }

    public static String fourcc() {
        return "stts";
    }

    private TimeToSampleEntry[] entries;

    public TimeToSampleBox(TimeToSampleEntry[] timeToSamples) {
        super(new Header(fourcc()));
        this.entries = timeToSamples;
    }

    public TimeToSampleBox() {
        super(new Header(fourcc()));
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        int foo = input.getInt();
        entries = new TimeToSampleEntry[foo];
        for (int i = 0; i < foo; i++) {
            entries[i] = new TimeToSampleEntry(input.getInt(), input.getInt());
        }
    }

    public TimeToSampleEntry[] getEntries() {
        return entries;
    }

    @Override
    public void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(entries.length);
        for (TimeToSampleEntry timeToSampleEntry : entries) {
            out.putInt(timeToSampleEntry.getSampleCount());
            out.putInt(timeToSampleEntry.getSampleDuration());
        }
    }

    public void setEntries(TimeToSampleEntry[] entries) {
        this.entries = entries;
    }
}