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
public class GenericDataEssenceDescriptor extends FileDescriptor {

    private UL dataEssenceCoding;

    public GenericDataEssenceDescriptor(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3e01:
                dataEssenceCoding = UL.read(_bb);
                break;
            default:
                Logger.warn(String.format("Unknown tag [ FileDescriptor: " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public UL getDataEssenceCoding() {
        return dataEssenceCoding;
    }
}