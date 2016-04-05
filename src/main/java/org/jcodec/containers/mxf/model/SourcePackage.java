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
public class SourcePackage extends GenericPackage {
    private UL[] trackRefs;
    private UL descriptorRef;
    
    public SourcePackage(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);
        
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();
            
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x4701:
                descriptorRef = UL.read(_bb);
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public UL[] getTrackRefs() {
        return trackRefs;
    }

    public UL getDescriptorRef() {
        return descriptorRef;
    }
}