package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class EssenceContainerData extends MXFInterchangeObject {

    private UL linkedPackageUID;
    private int indexSID;
    private int bodySID;

    public EssenceContainerData(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();
            switch (entry.getKey()) {
            case 0x2701:
                linkedPackageUID = UL.read(_bb);
                break;
            case 0x3f06:
                indexSID = _bb.getInt();
                break;
            case 0x3f07:
                bodySID = _bb.getInt();
                break;
            default:
                Logger.warn(String.format("Unknown tag [ EssenceContainerData: " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public UL getLinkedPackageUID() {
        return linkedPackageUID;
    }

    public int getIndexSID() {
        return indexSID;
    }

    public int getBodySID() {
        return bodySID;
    }
}
