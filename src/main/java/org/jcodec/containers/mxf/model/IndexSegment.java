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
 * MXF demuxer, Index segment structure. Contains information about frame
 * offsets. Used for rapid random positioning within the movie.
 * 
 * @author The JCodec project
 * 
 */
public class IndexSegment extends MXFInterchangeObject {
    private IndexEntries ie;
    private int editUnitByteCount;
    private DeltaEntries deltaEntries;
    private int indexSID;
    private int bodySID;
    private int indexEditRateNum;
    private int indexEditRateDen;
    private long indexStartPosition;
    private long indexDuration;
    private UL instanceUID;
    private int sliceCount;
    private int posTableCount;

    public IndexSegment(UL ul) {
        super(ul);
    }

    @Override
    protected void read(Map<Integer, ByteBuffer> tags) {
        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3c0a:
                instanceUID = UL.read(_bb);
                break;
            case 0x3f05:
                editUnitByteCount = _bb.getInt();
                break;
            case 0x3f06:
                indexSID = _bb.getInt();
                break;
            case 0x3f07:
                bodySID = _bb.getInt();
                break;
            case 0x3f08:
                sliceCount = _bb.get() & 0xff;
                break;
            case 0x3f09:
                deltaEntries = DeltaEntries.read(_bb);
                break;
            case 0x3f0a:
                ie = IndexEntries.read(_bb);
                break;
            case 0x3f0b:
                indexEditRateNum = _bb.getInt();
                indexEditRateDen = _bb.getInt();
                break;
            case 0x3f0c:
                indexStartPosition = _bb.getLong();
                break;
            case 0x3f0d:
                indexDuration = _bb.getLong();
                break;
            case 0x3f0e:
                posTableCount = _bb.get() & 0xff;
                break;
            default:
                Logger.warn(String.format("Unknown tag [" + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public IndexEntries getIe() {
        return ie;
    }

    public int getEditUnitByteCount() {
        return editUnitByteCount;
    }

    public DeltaEntries getDeltaEntries() {
        return deltaEntries;
    }

    public int getIndexSID() {
        return indexSID;
    }

    public int getBodySID() {
        return bodySID;
    }

    public int getIndexEditRateNum() {
        return indexEditRateNum;
    }

    public int getIndexEditRateDen() {
        return indexEditRateDen;
    }

    public long getIndexStartPosition() {
        return indexStartPosition;
    }

    public long getIndexDuration() {
        return indexDuration;
    }

    public UL getInstanceUID() {
        return instanceUID;
    }

    public int getSliceCount() {
        return sliceCount;
    }

    public int getPosTableCount() {
        return posTableCount;
    }
}