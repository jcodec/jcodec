package org.jcodec.containers.mxf.read;

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
public class Sequence extends MXFMetadataSet {
    private long duration;
    private UL[] structuralComponentsRefs;
    private UL dataDefinitionUL;
    
    public Sequence(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {

        for (Entry<Integer, ByteBuffer> entry : tags.entrySet()) {
            switch (entry.getKey()) {
            case 0x0202:
                duration = entry.getValue().getLong();
                break;
            case 0x0201:
                dataDefinitionUL = UL.read(entry.getValue());
                break;
            case 0x1001:
                structuralComponentsRefs = readULs(entry.getValue());
                break;
            }
        }
    }

    public long getDuration() {
        return duration;
    }

    public UL getDataDefinitionUL() {
        return dataDefinitionUL;
    }

    public UL[] getStructuralComponentsRefs() {
        return structuralComponentsRefs;
    }
}