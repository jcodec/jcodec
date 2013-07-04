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
public class SourceClip extends MXFStructuralComponent {
    private long startPosition;
    private int sourceTrackId;
    private UL sourcePackageUid;
    
    public SourceClip(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);
        
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();
            
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x1201:
                startPosition = _bb.getLong();
                break;
            case 0x1101:
                sourcePackageUid = UL.read(_bb);
                break;
            case 0x1102:
                sourceTrackId = _bb.getInt();
                break;
            default:
                System.out.println(String.format("Unknown tag [ SourceClip: " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }
    
    public UL getSourcePackageUid() {
        return sourcePackageUid;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public int getSourceTrackId() {
        return sourceTrackId;
    }
}
