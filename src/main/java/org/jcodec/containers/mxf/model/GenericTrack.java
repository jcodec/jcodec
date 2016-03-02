package org.jcodec.containers.mxf.model;
import java.util.Iterator;

import org.jcodec.common.logging.Logger;

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
public class GenericTrack extends MXFInterchangeObject {
    private int trackId;
    private String name;
    private UL sequenceRef;
    private int trackNumber;

    public GenericTrack(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();
            switch (entry.getKey()) {
            case 0x4801:
                trackId = _bb.getInt();
                break;
            case 0x4802:
                name = readUtf16String(_bb);
                break;
            case 0x4803:
                sequenceRef = UL.read(_bb);
                break;
            case 0x4804:
                trackNumber = _bb.getInt();
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public int getTrackId() {
        return trackId;
    }

    public String getName() {
        return name;
    }

    public UL getSequenceRef() {
        return sequenceRef;
    }

    public int getTrackNumber() {
        return trackNumber;
    }
}
