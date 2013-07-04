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
public class Sequence extends MXFStructuralComponent {
    private UL[] structuralComponentsRefs;

    public Sequence(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            switch (entry.getKey()) {
            case 0x1001:
                structuralComponentsRefs = readULBatch(entry.getValue());
                break;
            default:
//                System.out.println(String.format("Unknown tag [ Sequence: " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public UL[] getStructuralComponentsRefs() {
        return structuralComponentsRefs;
    }
}