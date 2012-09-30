package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Sample to chunk mapping box
 * 
 * @author Jay Codec
 * 
 */
public class SampleToChunkBox extends FullBox {

    public static class SampleToChunkEntry {
        private long first;
        private int count;
        private int entry;

        public SampleToChunkEntry(long first, int count, int entry) {
            this.first = first;
            this.count = count;
            this.entry = entry;
        }

        public long getFirst() {
            return first;
        }

        public void setFirst(long first) {
            this.first = first;
        }

        public int getCount() {
            return count;
        }

        public int getEntry() {
            return entry;
        }

        public void setEntry(int entry) {
            this.entry = entry;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static String fourcc() {
        return "stsc";
    }

    private SampleToChunkEntry[] sampleToChunk;

    public SampleToChunkBox(SampleToChunkEntry[] sampleToChunk) {
        super(new Header(fourcc()));
        this.sampleToChunk = sampleToChunk;
    }

    public SampleToChunkBox() {
        super(new Header(fourcc()));
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        int size = (int) ReaderBE.readInt32(input);

        sampleToChunk = new SampleToChunkEntry[size];
        for (int i = 0; i < size; i++) {
            sampleToChunk[i] = new SampleToChunkEntry(ReaderBE.readInt32(input), (int) ReaderBE.readInt32(input),
                    (int) ReaderBE.readInt32(input));
        }
    }

    public SampleToChunkEntry[] getSampleToChunk() {
        return sampleToChunk;
    }

    @Override
    public void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt(sampleToChunk.length);

        for (SampleToChunkEntry stc : sampleToChunk) {
            out.writeInt((int) stc.getFirst());
            out.writeInt((int) stc.getCount());
            out.writeInt((int) stc.getEntry());
        }
    }

    public void setSampleToChunk(SampleToChunkEntry[] sampleToChunk) {
        this.sampleToChunk = sampleToChunk;
    }
}