package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.codecs.wav.StringReader;
import org.jcodec.common.io.ReaderBE;
import org.jcodec.common.tools.ToJSON;

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

    public static final int FLAG_DROPFRAME = 0x1;
    public static final int FLAG_24HOURMAX = 0x2;
    public static final int FLAG_NEGATIVETIMEOK = 0x4;
    public static final int FLAG_COUNTER = 0x8;

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

    public void parse(InputStream input) throws IOException {
        super.parse(input);

        StringReader.sureSkip(input, 4);
        flags = (int) ReaderBE.readInt32(input);
        timescale = (int) ReaderBE.readInt32(input);
        frameDuration = (int) ReaderBE.readInt32(input);
        numFrames = (byte) input.read();
        int reserved = input.read();
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt(0);
        out.writeInt(flags);
        out.writeInt(timescale);
        out.writeInt(frameDuration);
        out.writeByte(numFrames);
        out.write(0);
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

    @Override
    public void dump(StringBuilder sb) {
        sb.append(header.getFourcc() + ": {\n");
        sb.append("entry: ");

        ToJSON.toJSON(this, sb, "flags", "timescale", "frameDuration", "numFrames");
        sb.append(",\nexts: [\n");
        dumpBoxes(sb);
        sb.append("\n]\n");
        sb.append("}\n");
    }
}
