package org.jcodec.containers.mxf.read;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;

import org.jcodec.common.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SourcePackage extends MXFMetadataSet {
    private UL[] trackRefs;
    private UL descriptorRef;
    private UL packageUid;
    
    public SourcePackage(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Entry<Integer, ByteBuffer> entry : tags.entrySet()) {
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x4403:
                trackRefs = readULs(_bb);
                break;
            case 0x4401:
                NIOUtils.skip(_bb, 16);
                packageUid = UL.read(_bb);
                break;
            case 0x4701:
                descriptorRef = UL.read(_bb);
                break;
            }
        }
    }

    public UL[] getTrackRefs() {
        return trackRefs;
    }

    public UL getDescriptorRef() {
        return descriptorRef;
    }

    public UL getPackageUid() {
        return packageUid;
    }
}