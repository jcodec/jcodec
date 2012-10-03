package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

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

    public CompositionOffsetsBox() {
        super(new Header(fourcc()));
    }

    public CompositionOffsetsBox(Entry[] entries) {
        super(new Header(fourcc()));
        this.entries = entries;
    }

    public static String fourcc() {
        return "ctts";
    }

    @Override
    public void parse(InputStream input) throws IOException {
        super.parse(input);
        int num = (int) ReaderBE.readInt32(input);

        entries = new Entry[num];
        for (int i = 0; i < num; i++) {
            entries[i] = new Entry((int) ReaderBE.readInt32(input), (int) ReaderBE.readInt32(input));
        }
    }

    @Override
    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);

        out.writeInt(entries.length);
        for (int i = 0; i < entries.length; i++) {
            out.writeInt(entries[i].count);
            out.writeInt(entries[i].offset);
        }
    }

    public Entry[] getEntries() {
        return entries;
    }
}