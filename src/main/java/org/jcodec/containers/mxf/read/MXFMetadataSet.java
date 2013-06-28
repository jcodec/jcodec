package org.jcodec.containers.mxf.read;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.NIOUtils;

public abstract class MXFMetadataSet extends MXFMetadata {

    public MXFMetadataSet(UL ul) {
        super(ul);
    }

    public void read(ByteBuffer bb) {
        bb.order(ByteOrder.BIG_ENDIAN);
        Map<Integer, ByteBuffer> tags = new HashMap<Integer, ByteBuffer>();
        while (bb.hasRemaining()) {
            int tag = bb.getShort() & 0xffff;
            int size = bb.getShort() & 0xffff;
            ByteBuffer _bb = NIOUtils.read(bb, size);
            if (tag == 0x3C0A)
                uid = UL.read(_bb);
            else
                tags.put(tag, _bb);
        }

        read(tags);
    }

    protected abstract void read(Map<Integer, ByteBuffer> tags);
}
