package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class GenericPackage extends MXFInterchangeObject {
    private UL[] tracks;
    private UL packageUID;
    private String name;
    private Date packageModifiedDate;
    private Date packageCreationDate;

    public GenericPackage(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();
            
            ByteBuffer _bb = entry.getValue();
            switch (entry.getKey()) {
            case 0x4401:
                packageUID = UL.read(_bb);
                break;
            case 0x4402:
                name = readUtf16String(_bb);
                break;
            case 0x4403:
                tracks = readULBatch(_bb);
                break;
            case 0x4404:
                packageModifiedDate = readDate(_bb);
                break;
            case 0x4405:
                packageCreationDate = readDate(_bb);
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public UL[] getTracks() {
        return tracks;
    }

    public UL getPackageUID() {
        return packageUID;
    }

    public String getName() {
        return name;
    }

    public Date getPackageModifiedDate() {
        return packageModifiedDate;
    }

    public Date getPackageCreationDate() {
        return packageCreationDate;
    }
}
