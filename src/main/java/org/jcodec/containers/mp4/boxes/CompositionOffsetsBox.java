package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Default box factory
 * 
 * @author The JCodec project
 * 
 */
public class CompositionOffsetsBox extends FullBox {

    private Entry[] entries;

    public static class Entry {
        public int count;
        public int offset;

        public Entry(int count, int offset) {
            this.count = count;
            this.offset = offset;
        }

        public int getCount() {
            return count;
        }

        public int getOffset() {
            return offset;
        }
    }

    public CompositionOffsetsBox(Header header) {
        super(header);
    }

    public static String fourcc() {
        return "ctts";
    }

    public static CompositionOffsetsBox createCompositionOffsetsBox(Entry[] entries) {
        CompositionOffsetsBox ctts = new CompositionOffsetsBox(new Header(fourcc()));
        ctts.entries = entries;
        return ctts;
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        int num = input.getInt();

        entries = new Entry[num];
        for (int i = 0; i < num; i++) {
            entries[i] = new Entry(input.getInt(), input.getInt());
        }
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        out.putInt(entries.length);
        for (int i = 0; i < entries.length; i++) {
            out.putInt(entries[i].count);
            out.putInt(entries[i].offset);
        }
    }

    public Entry[] getEntries() {
        return entries;
    }
}