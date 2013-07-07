package org.jcodec.containers.mp4.boxes;

import static org.jcodec.containers.mp4.TimeUtil.fromMovTime;
import static org.jcodec.containers.mp4.TimeUtil.toMovTime;

import java.nio.ByteBuffer;

import org.jcodec.common.tools.ToJSON;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A media header atom
 * 
 * @author Jay Codec
 * 
 */
public class MediaHeaderBox extends FullBox {
    private long created;
    private long modified;
    private int timescale;
    private long duration;
    private int language;
    private int quality;

    public static String fourcc() {
        return "mdhd";
    }

    public MediaHeaderBox(int timescale, long duration, int language, long created, long modified, int quality) {
        super(new Header(fourcc()));
        this.timescale = timescale;
        this.duration = duration;
        this.language = language;
        this.created = created;
        this.modified = modified;
        this.quality = quality;
    }

    public MediaHeaderBox() {
        super(new Header(fourcc()));
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        if (version == 0) {
            created = fromMovTime(input.getInt());
            modified = fromMovTime(input.getInt());
            timescale = input.getInt();
            duration = input.getInt();
        } else if (version == 1) {
            created = fromMovTime(input.getLong());
            modified = fromMovTime(input.getLong());
            timescale = input.getInt();
            duration = input.getLong();
        } else {
            throw new RuntimeException("Unsupported version");
        }
    }

    public int getTimescale() {
        return timescale;
    }

    public long getDuration() {
        return duration;
    }

    public void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(toMovTime(created));
        out.putInt(toMovTime(modified));
        out.putInt(timescale);
        out.putInt((int) duration);
        out.putShort((short) language);
        out.putShort((short) quality);
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setTimescale(int timescale) {
        this.timescale = timescale;
    }

    @Override
    protected void dump(StringBuilder sb) {
        super.dump(sb);
        sb.append(": ");
        ToJSON.toJSON(this, sb, "created", "modified", "timescale", "duration", "language", "quality");
    }
}