package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
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

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        int foo = (int) ReaderBE.readInt32(input);
        entries = new TimeToSampleEntry[foo];
        for (int i = 0; i < foo; i++) {
            entries[i] = new TimeToSampleEntry((int) ReaderBE.readInt32(input),
                    (int) ReaderBE.readInt32(input));
        }
    }

    public TimeToSampleEntry[] getEntries() {
        return entries;
    }

    @Override
    public void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt(entries.length);
        for (TimeToSampleEntry timeToSampleEntry : entries) {
            out.writeInt((int) timeToSampleEntry.getSampleCount());
            out.writeInt((int) timeToSampleEntry.getSampleDuration());
        }
    }

    public void setEntries(TimeToSampleEntry[] entries) {
        this.entries = entries;
    }
}