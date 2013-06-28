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
public class SourceClip extends MXFMetadataSet {
    private long duration;
    private long startPosition;
    private int sourceTrackId;
    private UL sourcePackageUid;
    
    public SourceClip(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Entry<Integer, ByteBuffer> entry : tags.entrySet()) {
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x0202:
                duration = _bb.getLong();
                break;
            case 0x1201:
                startPosition = _bb.getLong();
                break;
            case 0x1101:
                sourcePackageUid = UL.read(_bb);
                break;
            case 0x1102:
                sourceTrackId = _bb.getInt();
                break;
            }
        }
    }
    
    public UL getSourcePackageUid() {
        return sourcePackageUid;
    }

    public long getDuration() {
        return duration;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public int getSourceTrackId() {
        return sourceTrackId;
    }
}
