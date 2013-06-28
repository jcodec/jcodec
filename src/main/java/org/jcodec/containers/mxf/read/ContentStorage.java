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
public class ContentStorage extends MXFMetadataSet {
    private UL[] packageRefs;

    public ContentStorage(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Entry<Integer, ByteBuffer> entry : tags.entrySet()) {
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x1901:
                packageRefs = readULs(_bb);
                break;
            }
        }
    }

    public UL[] getPackageRefs() {
        return packageRefs;
    }
}