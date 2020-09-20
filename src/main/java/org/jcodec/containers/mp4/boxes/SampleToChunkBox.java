package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.containers.mp4.boxes.Box.AtomField;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Sample to chunk mapping box
 * 
 * @author The JCodec project
 * 
 */
public class SampleToChunkBox extends FullBox {

    public SampleToChunkBox(Header atom) {
        super(atom);
    }

    public static class SampleToChunkEntry {
        private long first;
        private int count;
        private int entry;

        public SampleToChunkEntry(long first, int count, int entry) {
            this.first = first;
            this.count = count;
            this.entry = entry;
        }

        @AtomField(idx=0)
        public long getFirst() {
            return first;
        }

        public void setFirst(long first) {
            this.first = first;
        }
        @AtomField(idx=1)
        public int getCount() {
            return count;
        }
        @AtomField(idx=2)
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

    public static SampleToChunkBox createSampleToChunkBox(SampleToChunkEntry[] sampleToChunk) {
        SampleToChunkBox box = new SampleToChunkBox(new Header(fourcc()));
        box.sampleToChunk = sampleToChunk;
        return box;
    }
    private SampleToChunkEntry[] sampleToChunk;

    public void parse(ByteBuffer input) {
        super.parse(input);
        int size = input.getInt();

        sampleToChunk = new SampleToChunkEntry[size];
        for (int i = 0; i < size; i++) {
            sampleToChunk[i] = new SampleToChunkEntry(input.getInt(), input.getInt(),
                    input.getInt());
        }
    }

    @AtomField(idx=0)
    public SampleToChunkEntry[] getSampleToChunk() {
        return sampleToChunk;
    }

    @Override
    public void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(sampleToChunk.length);

        for (int i = 0; i < sampleToChunk.length; i++) {
            SampleToChunkEntry stc = sampleToChunk[i];
            out.putInt((int) stc.getFirst());
            out.putInt((int) stc.getCount());
            out.putInt((int) stc.getEntry());
        }
    }
    
    @Override
    public int estimateSize() {
        return 16 + sampleToChunk.length * 12;
    }

    public void setSampleToChunk(SampleToChunkEntry[] sampleToChunk) {
        this.sampleToChunk = sampleToChunk;
    }
}