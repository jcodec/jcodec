package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class MXFInterchangeObject extends MXFMetadata {

    private UL generationUID;
    private UL objectClass;

    public MXFInterchangeObject(UL ul) {
        super(ul);
    }

    public void read(ByteBuffer bb) {
        bb.order(ByteOrder.BIG_ENDIAN);
        Map<Integer, ByteBuffer> tags = new HashMap<Integer, ByteBuffer>();
        while (bb.hasRemaining()) {
            int tag = bb.getShort() & 0xffff;
            int size = bb.getShort() & 0xffff;
            ByteBuffer _bb = NIOUtils.read(bb, size);
            switch (tag) {
            case 0x3C0A:
                uid = UL.read(_bb);
                break;
            case 0x0102:
                generationUID = UL.read(_bb);
                break;
            case 0x0101:
                objectClass = UL.read(_bb);
                break;
            default:
                tags.put(tag, _bb);
            }
        }

        if (tags.size() > 0)
            read(tags);
    }

    protected abstract void read(Map<Integer, ByteBuffer> tags);

    public UL getGenerationUID() {
        return generationUID;
    }

    public UL getObjectClass() {
        return objectClass;
    }
}
