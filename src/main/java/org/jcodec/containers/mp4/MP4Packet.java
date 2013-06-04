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

    public MP4Packet(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean iframe,
            TapeTimecode tapeTimecode, long mediaPts, int entryNo) {
        super(data, pts, timescale, duration, frameNo, iframe, tapeTimecode);
        this.mediaPts = mediaPts;
        this.entryNo = entryNo;
    }
    
    public MP4Packet(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean iframe,
            TapeTimecode tapeTimecode, long mediaPts, int entryNo, long fileOff, int size) {
        super(data, pts, timescale, duration, frameNo, iframe, tapeTimecode);
        this.mediaPts = mediaPts;
        this.entryNo = entryNo;
        this.fileOff = fileOff;
        this.size = size;
    }

    public MP4Packet(MP4Packet packet, ByteBuffer frm) {
        super(packet, frm);
        this.mediaPts = packet.mediaPts;
        this.entryNo = packet.entryNo;
    }

    public MP4Packet(MP4Packet packet, TapeTimecode timecode) {
        super(packet, timecode);
        this.mediaPts = packet.mediaPts;
        this.entryNo = packet.entryNo;
    }

    public MP4Packet(Packet packet, long mediaPts, int entryNo) {
        super(packet);
        this.mediaPts = mediaPts;
        this.entryNo = entryNo;
    }

    public MP4Packet(MP4Packet packet) {
        super(packet);
        this.mediaPts = packet.mediaPts;
        this.entryNo = packet.entryNo;
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
}