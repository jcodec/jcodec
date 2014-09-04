package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

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

    public void parse(ByteBuffer input) {
        super.parse(input);
        int size = input.getInt();

        sampleToChunk = new SampleToChunkEntry[size];
        for (int i = 0; i < size; i++) {
            sampleToChunk[i] = new SampleToChunkEntry(input.getInt(), input.getInt(),
                    input.getInt());
        }
    }

    public SampleToChunkEntry[] getSampleToChunk() {
        return sampleToChunk;
    }

    @Override
    public void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(sampleToChunk.length);

        for (SampleToChunkEntry stc : sampleToChunk) {
            out.putInt((int) stc.getFirst());
            out.putInt(stc.getCount());
            out.putInt(stc.getEntry());
        }
    }

    public void setSampleToChunk(SampleToChunkEntry[] sampleToChunk) {
        this.sampleToChunk = sampleToChunk;
    }
}