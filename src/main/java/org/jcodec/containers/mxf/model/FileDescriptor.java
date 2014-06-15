package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Rational;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class FileDescriptor extends GenericDescriptor {

    private int linkedTrackId;
    private Rational sampleRate;
    private long containerDuration;
    private UL essenceContainer;
    private UL codec;

    public FileDescriptor(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);
        
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();
            
            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3006:
                linkedTrackId = _bb.getInt();
                break;
            case 0x3001:
                sampleRate = new Rational(_bb.getInt(), _bb.getInt());
                break;
            case 0x3002:
                containerDuration = _bb.getLong();
                break;
            case 0x3004:
                essenceContainer = UL.read(_bb);
                break;
            case 0x3005:
                codec = UL.read(_bb);
                break;
            default:
                Logger.warn(String.format("Unknown tag [ " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public int getLinkedTrackId() {
        return linkedTrackId;
    }

    public Rational getSampleRate() {
        return sampleRate;
    }

    public long getContainerDuration() {
        return containerDuration;
    }

    public UL getEssenceContainer() {
        return essenceContainer;
    }

    public UL getCodec() {
        return codec;
    }
}
