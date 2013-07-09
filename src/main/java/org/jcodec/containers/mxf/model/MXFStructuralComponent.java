package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MXFStructuralComponent extends MXFInterchangeObject {
    
    private long duration;
    private UL dataDefinitionUL;

    public MXFStructuralComponent(UL ul) {
        super(ul);
    }
    
    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();
            
            switch (entry.getKey()) {
            case 0x0202:
                duration = entry.getValue().getLong();
                break;
            case 0x0201:
                dataDefinitionUL = UL.read(entry.getValue());
                break;
            default:
//                System.out.println(String.format("Unknown tag [ MXFStructuralComponent: " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public long getDuration() {
        return duration;
    }

    public UL getDataDefinitionUL() {
        return dataDefinitionUL;
    }
}
