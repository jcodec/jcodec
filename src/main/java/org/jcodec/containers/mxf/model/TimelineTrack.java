package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jcodec.common.model.Rational;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class TimelineTrack extends GenericTrack {

    private Rational editRate;
    private long origin;

    public TimelineTrack(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();
            switch (entry.getKey()) {
            case 0x4B01:
                editRate = new Rational(_bb.getInt(), _bb.getInt());
                break;
            case 0x4b02:
                origin = _bb.getLong();
                break;
            default:
                System.out.println(String.format("Unknown tag [ TimelineTrack: " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public Rational getEditRate() {
        return editRate;
    }

    public long getOrigin() {
        return origin;
    }
}