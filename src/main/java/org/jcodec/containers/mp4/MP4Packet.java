package org.jcodec.containers.mp4;

import java.nio.ByteBuffer;

import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class MP4Packet extends Packet {
    private long mediaPts;
    private int entryNo;
    private long fileOff;
    private int size;
    private boolean psync;

    public MP4Packet(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean iframe,
            TapeTimecode tapeTimecode, long mediaPts, int entryNo) {
        super(data, pts, timescale, duration, frameNo, iframe, tapeTimecode, 0);
        this.mediaPts = mediaPts;
        this.entryNo = entryNo;
    }
    
    public MP4Packet(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean iframe,
            TapeTimecode tapeTimecode, long mediaPts, int entryNo, long fileOff, int size, boolean psync) {
        super(data, pts, timescale, duration, frameNo, iframe, tapeTimecode, 0);
        this.mediaPts = mediaPts;
        this.entryNo = entryNo;
        this.fileOff = fileOff;
        this.size = size;
        this.psync = psync;
    }

    public MP4Packet(MP4Packet other, ByteBuffer frm) {
        super(frm, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, other.tapeTimecode, other.displayOrder);
        this.mediaPts = other.mediaPts;
        this.entryNo = other.entryNo;
    }

    public MP4Packet(MP4Packet other, TapeTimecode timecode) {
        super(other.data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, timecode, other.displayOrder);
        this.mediaPts = other.mediaPts;
        this.entryNo = other.entryNo;
    }

    public MP4Packet(Packet other, long mediaPts, int entryNo) {
        super(other.data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, other.tapeTimecode, other.displayOrder);
        this.mediaPts = mediaPts;
        this.entryNo = entryNo;
    }

    public MP4Packet(MP4Packet other) {
        super(other.data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, other.tapeTimecode, other.displayOrder);
        this.mediaPts = other.mediaPts;
        this.entryNo = other.entryNo;
    }

    /**
     * Zero-offset sample entry index
     * 
     * @return
     */
    public int getEntryNo() {
        return entryNo;
    }

    public long getMediaPts() {
        return mediaPts;
    }

    public long getFileOff() {
        return fileOff;
    }

    public int getSize() {
        return size;
    }

    public boolean isPsync() {
        return psync;
    }
}