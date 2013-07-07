package org.jcodec.containers.mp4.boxes;

import static org.jcodec.containers.mp4.TimeUtil.fromMovTime;
import static org.jcodec.containers.mp4.TimeUtil.toMovTime;

import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.tools.ToJSON;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A movie header box
 * 
 * @author Jay Codec
 * 
 */
public class MovieHeaderBox extends FullBox {
    private int timescale;
    private long duration;
    private float rate;
    private float volume;
    private long created;
    private long modified;
    private int[] matrix;
    private int nextTrackId;

    public static String fourcc() {
        return "mvhd";
    }

    public MovieHeaderBox(int timescale, long duration, float rate, float volume, long created, long modified,
            int[] matrix, int nextTrackId) {
        super(new Header(fourcc()));

        this.timescale = timescale;
        this.duration = duration;
        this.rate = rate;
        this.volume = volume;
        this.created = created;
        this.modified = modified;
        this.matrix = matrix;
        this.nextTrackId = nextTrackId;
    }

    public MovieHeaderBox() {
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
        rate = readRate(input);
        volume = readVolume(input);
        NIOUtils.skip(input, 10);
        matrix = readMatrix(input);
        NIOUtils.skip(input, 24);
        nextTrackId = input.getInt();
    }

    private int[] readMatrix(ByteBuffer input) {
        int[] matrix = new int[9];
        for (int i = 0; i < 9; i++)
            matrix[i] = input.getInt();
        return matrix;
    }

    private float readVolume(ByteBuffer input) {
        return (float) input.getShort() / 256f;
    }

    private float readRate(ByteBuffer input) {
        return (float) input.getInt() / 65536f;
    }

    public void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(toMovTime(created));
        out.putInt(toMovTime(modified));
        out.putInt(timescale);
        out.putInt((int) duration);
        writeFixed1616(out, rate);
        writeFixed88(out, volume);
        out.put(new byte[10]);
        writeMatrix(out);
        out.put(new byte[24]);
        out.putInt(nextTrackId);
    }

    private void writeMatrix(ByteBuffer out) {
        for (int i = 0; i < Math.min(9, matrix.length); i++)
            out.putInt(matrix[i]);
        for (int i = Math.min(9, matrix.length); i < 9; i++)
            out.putInt(0);
    }

    private void writeFixed88(ByteBuffer out, float volume) {
        out.putShort((short) (volume * 256.));
    }

    private void writeFixed1616(ByteBuffer out, float rate) {
        out.putInt((int) (rate * 65536.));
    }

    public int getTimescale() {
        return timescale;
    }

    public long getDuration() {
        return duration;
    }

    public void setTimescale(int newTs) {
        this.timescale = newTs;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getNextTrackId() {
        return nextTrackId;
    }

    public void setNextTrackId(int nextTrackId) {
        this.nextTrackId = nextTrackId;
    }

    @Override
    protected void dump(StringBuilder sb) {
        super.dump(sb);
        sb.append(": ");
        ToJSON.toJSON(this, sb, "timescale", "duration", "rate", "volume", "created", "modified", "nextTrackId");
    }
}