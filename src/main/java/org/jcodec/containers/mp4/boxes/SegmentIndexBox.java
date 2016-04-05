package org.jcodec.containers.mp4.boxes;

import org.jcodec.platform.Platform;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class SegmentIndexBox extends FullBox {
    public SegmentIndexBox(Header atom) {
        super(atom);
    }

    public static SegmentIndexBox createSegmentIndexBox() {
        return new SegmentIndexBox(new Header(fourcc()));
    }

    public long reference_ID;
    public long timescale;
    public long earliest_presentation_time;
    public long first_offset;
    public int reserved;
    public int reference_count;
    public Reference[] references;

    public static class Reference {
        public boolean reference_type;
        public long referenced_size;
        public long subsegment_duration;
        public boolean starts_with_SAP;
        public int SAP_type;
        public long SAP_delta_time;

        @Override
        public String toString() {
            return "Reference [reference_type=" + reference_type + ", referenced_size=" + referenced_size
                    + ", subsegment_duration=" + subsegment_duration + ", starts_with_SAP=" + starts_with_SAP
                    + ", SAP_type=" + SAP_type + ", SAP_delta_time=" + SAP_delta_time + "]";
        }

    }

    public static String fourcc() {
        return "sidx";
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        this.reference_ID = input.getInt() & 0xffffffffL;
        this.timescale = input.getInt() & 0xffffffffL;
        if (version == 0) {
            this.earliest_presentation_time = input.getInt() & 0xffffffffL;
            this.first_offset = input.getInt() & 0xffffffffL;
        } else {
            this.earliest_presentation_time = input.getLong();
            this.first_offset = input.getLong();
        }
        this.reserved = input.getShort();
        this.reference_count = input.getShort() & 0xffff;
        this.references = new Reference[this.reference_count];
        for (int i = 0; i < this.reference_count; i++) {
            long i0 = input.getInt() & 0xffffffffL;
            long i1 = input.getInt() & 0xffffffffL;
            long i2 = input.getInt() & 0xffffffffL;

            Reference ref = new Reference();
            ref.reference_type = (i0 >> 31) == 1;
            ref.referenced_size = i0 & 0x7fffffffL;
            ref.subsegment_duration = i1;
            ref.starts_with_SAP = (i2 >> 31) == 1;
            ref.SAP_type = (int) ((i2 >> 28) & 7);
            ref.SAP_delta_time = i2 & 0xFFFFFFFL;

            references[i] = ref;
        }
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt((int) reference_ID);
        out.putInt((int) timescale);
        if (version == 0) {
            out.putInt((int) earliest_presentation_time);
            out.putInt((int) first_offset);
        } else {
            out.putLong(earliest_presentation_time);
            out.putLong(first_offset);
        }
        out.putShort((short) reserved);
        out.putShort((short) reference_count);
        for (int i = 0; i < reference_count; i++) {
            Reference ref = references[i];
            int i0 = (int) (((ref.reference_type ? 1 : 0) << 31) | ref.referenced_size);
            int i1 = (int) ref.subsegment_duration;
            int i2 = 0;
            if (ref.starts_with_SAP) {
                i2 |= (1 << 31);
            }
            i2 |= ((ref.SAP_type & 7) << 28);
            i2 |= (ref.SAP_delta_time & 0xFFFFFFFL);

            out.putInt(i0);
            out.putInt(i1);
            out.putInt(i2);
        }
    }

    @Override
    public String toString() {
        return "SegmentIndexBox [reference_ID=" + reference_ID + ", timescale=" + timescale
                + ", earliest_presentation_time=" + earliest_presentation_time + ", first_offset=" + first_offset
                + ", reserved=" + reserved + ", reference_count=" + reference_count + ", references="
                + Platform.arrayToString(references) + ", version=" + version + ", flags=" + flags + ", header="
                + header + "]";
    }

}
