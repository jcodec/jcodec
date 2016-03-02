package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.io.NIOUtils;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Describes timecode payload sample
 * 
 * @author The JCodec project
 * 
 */
public class TimecodeSampleEntry extends SampleEntry {

    private static final String TMCD = "tmcd";

    //@formatter:off
    public static final int FLAG_DROPFRAME = 0x1;
    public static final int FLAG_24HOURMAX = 0x2;
    public static final int FLAG_NEGATIVETIMEOK = 0x4;
    public static final int FLAG_COUNTER = 0x8;
    //@formatter:on

    public static TimecodeSampleEntry createTimecodeSampleEntry(int flags, int timescale, int frameDuration,
            int numFrames) {
        TimecodeSampleEntry tmcd = new TimecodeSampleEntry(new Header(TMCD));
        tmcd.flags = flags;
        tmcd.timescale = timescale;
        tmcd.frameDuration = frameDuration;
        tmcd.numFrames = (byte) numFrames;
        return tmcd;
    }

    private int flags;
    private int timescale;
    private int frameDuration;
    private byte numFrames;

    public TimecodeSampleEntry(Header header) {
        super(header);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);

        NIOUtils.skip(input, 4);
        flags = input.getInt();
        timescale = input.getInt();
        frameDuration = input.getInt();
        numFrames = input.get();
        NIOUtils.skip(input, 1);
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(0);
        out.putInt(flags);
        out.putInt(timescale);
        out.putInt(frameDuration);
        out.put(numFrames);
        out.put((byte) 207);
    }

    public int getFlags() {
        return flags;
    }

    public int getTimescale() {
        return timescale;
    }

    public int getFrameDuration() {
        return frameDuration;
    }

    public byte getNumFrames() {
        return numFrames;
    }

    public boolean isDropFrame() {
        return (flags & FLAG_DROPFRAME) != 0;
    }
}
