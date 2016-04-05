package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;

import js.nio.ByteBuffer;
import js.util.Date;
import js.util.Map;
import js.util.Map.Entry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Preface extends MXFInterchangeObject {

    private Date lastModifiedDate;
    private int objectModelVersion;
    private UL op;
    private UL[] essenceContainers;
    private UL[] dmSchemes;

    public Preface(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();
            ByteBuffer _bb = entry.getValue();
            switch (entry.getKey()) {
            case 0x3b02:
                lastModifiedDate = readDate(_bb);
                break;
            case 0x3b07:
                objectModelVersion = _bb.getInt();
                break;
            case 0x3b09:
                op = UL.read(_bb);
                break;
            case 0x3b0a:
                essenceContainers = readULBatch(_bb);
                break;
            case 0x3b0b:
                dmSchemes = readULBatch(_bb);
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getObjectModelVersion() {
        return objectModelVersion;
    }

    public UL getOp() {
        return op;
    }

    public UL[] getEssenceContainers() {
        return essenceContainers;
    }

    public UL[] getDmSchemes() {
        return dmSchemes;
    }
}
