package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.io.NIOUtils;

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

//@formatter:off
    public static final int FLAG_DROPFRAME      = 0x1;
    public static final int FLAG_24HOURMAX      = 0x2;
    public static final int FLAG_NEGATIVETIMEOK = 0x4;
    public static final int FLAG_COUNTER        = 0x8;
//@formatter:on

    private static final MyFactory FACTORY = new MyFactory();
    private int flags;
    private int timescale;
    private int frameDuration;
    private byte numFrames;

    public TimecodeSampleEntry(Header header) {
        super(header);
        factory = FACTORY;
    }

    public TimecodeSampleEntry() {
        super(new Header("tmcd"));
        factory = FACTORY;
    }

    public TimecodeSampleEntry(int flags, int timescale, int frameDuration, int numFrames) {
        super(new Header("tmcd"));
        this.flags = flags;
        this.timescale = timescale;
        this.frameDuration = frameDuration;
        this.numFrames = (byte) numFrames;
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

    public static class MyFactory extends BoxFactory {
        private Map<String, Class<? extends Box>> mappings = new HashMap<String, Class<? extends Box>>();

        public MyFactory() {
        }

        public Class<? extends Box> toClass(String fourcc) {
            return mappings.get(fourcc);
        }
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
