package org.jcodec.containers.mxf.read;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;

public class TimecodeComponent extends MXFMetadataSet {
    private long start;
    private int base;
    private int dropFrame;

    public TimecodeComponent(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Entry<Integer, ByteBuffer> entry : tags.entrySet()) {
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x1501:
                start = _bb.getLong();
                break;
            case 0x1502:
                base = _bb.getShort();
                break;
            case 0x1503:
                dropFrame = _bb.get();
                ;
                break;
            }
        }
    }

    public long getStart() {
        return start;
    }

    public int getBase() {
        return base;
    }

    public int getDropFrame() {
        return dropFrame;
    }
}
