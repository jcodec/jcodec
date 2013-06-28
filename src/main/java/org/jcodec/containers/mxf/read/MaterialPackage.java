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
public class MaterialPackage extends MXFMetadataSet {
    private UL[] tracks;
    
    public MaterialPackage(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Entry<Integer, ByteBuffer> entry : tags.entrySet()) {
            switch (entry.getKey()) {
            case 0x4403:
                tracks = readULs(entry.getValue());
                break;
            }
        }
    }

    public UL[] getTracks() {
        return tracks;
    }
}