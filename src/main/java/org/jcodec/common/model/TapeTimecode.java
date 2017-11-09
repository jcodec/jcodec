package org.jcodec.common.model;

import static java.lang.String.format;

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
    public static final TapeTimecode ZERO_TAPE_TIMECODE = new TapeTimecode((short) 0, (byte) 0, (byte) 0, (byte) 0, false, 0);

    private final short hour;
    private final byte minute;
    private final byte second;
    private final byte frame;
    private final boolean dropFrame;
    private final int tapeFps;

    public TapeTimecode(short hour, byte minute, byte second, byte frame, boolean dropFrame, int tapeFps) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.frame = frame;
        this.dropFrame = dropFrame;
        this.tapeFps = tapeFps;
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

    public int getTapeFps() {
        return tapeFps;
    }

    @Override
    public String toString() {
        String tcfmt = dropFrame ? "%02d:%02d:%02d;%02d" : "%02d:%02d:%02d:%02d";
        return format(tcfmt, hour, minute, second, frame);
    }

    public static TapeTimecode tapeTimecode(long frame, boolean dropFrame, int tapeFps) {
        if (dropFrame) {
            long D = frame / 17982;
            long M = frame % 17982;
            frame += 18 * D + 2 * ((M - 2) / 1798);
        }
        long sec = frame / tapeFps;
        return new TapeTimecode((short) (sec / 3600), (byte) ((sec / 60) % 60), (byte) (sec % 60),
                (byte) (frame % tapeFps), dropFrame, tapeFps);
    }
}
