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
public class Track extends MXFMetadataSet {
    private int trackId;
    private int trackNumber;
    private int erDen;
    private int erNum;
    private UL sequenceRef;

    public Track(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Entry<Integer, ByteBuffer> entry : tags.entrySet()) {
            ByteBuffer _bb = entry.getValue();
            switch (entry.getKey()) {
            case 0x4801:
                trackId = _bb.getInt();
                break;
            case 0x4804:
                trackNumber = _bb.getInt();
                break;
            case 0x4B01:
                erDen = _bb.getInt();
                erNum = _bb.getInt();
                break;
            case 0x4803:
                sequenceRef = UL.read(_bb);
                break;
            }
        }
    }

    public int getTrackId() {
        return trackId;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public int getErDen() {
        return erDen;
    }

    public int getErNum() {
        return erNum;
    }

    public UL getSequenceRef() {
        return sequenceRef;
    }
}