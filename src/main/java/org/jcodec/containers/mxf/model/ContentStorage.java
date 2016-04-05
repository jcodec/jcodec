package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;

import js.nio.ByteBuffer;
import js.util.Map;
import js.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ContentStorage extends MXFInterchangeObject {
    private UL[] packageRefs;
    private UL[] essenceContainerData;

    public ContentStorage(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();
            
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x1901:
                packageRefs = readULBatch(_bb);
                break;
            case 0x1902:
                essenceContainerData = readULBatch(_bb);
                break;
            default:
                Logger.warn(String.format("Unknown tag [ ContentStorage: " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public UL[] getPackageRefs() {
        return packageRefs;
    }

    public UL[] getEssenceContainerData() {
        return essenceContainerData;
    }
}