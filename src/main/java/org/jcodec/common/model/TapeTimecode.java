package org.jcodec.common.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Tape timecode
 * 
 * @author The JCodec project
 * 
 */
public class TapeTimecode {
    private short hour;
    private byte minute;
    private byte second;
    private byte frame;
    private boolean dropFrame;

    public TapeTimecode(short hour, byte minute, byte second, byte frame, boolean dropFrame) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.frame = frame;
        this.dropFrame = dropFrame;
    }

    public short getHour() {
        return hour;
    }

    public byte getMinute() {
        return minute;
    }

    public byte getSecond() {
        return second;
    }

    public byte getFrame() {
        return frame;
    }

    public boolean isDropFrame() {
        return dropFrame;
    }

    public String toString() {
        return String.format("%02d:%02d:%02d", hour, minute, second) + (dropFrame ? ";" : ":")
                + String.format("%02d", frame);
    }
}
