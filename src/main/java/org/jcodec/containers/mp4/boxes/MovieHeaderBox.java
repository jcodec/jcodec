package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.io.ReaderBE.readInt16;
import static org.jcodec.common.io.ReaderBE.readInt32;
import static org.jcodec.common.io.ReaderBE.readInt64;
import static org.jcodec.containers.mp4.TimeUtil.fromMovTime;
import static org.jcodec.containers.mp4.TimeUtil.toMovTime;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.codecs.wav.StringReader;
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

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        if (version == 0) {
            created = fromMovTime((int) readInt32(input));
            modified = fromMovTime((int) readInt32(input));
            timescale = (int) readInt32(input);
            duration = readInt32(input);
        } else if (version == 1) {
            created = fromMovTime((int) readInt64(input));
            modified = fromMovTime((int) readInt64(input));
            timescale = (int) readInt32(input);
            duration = readInt64(input);
        } else {
            throw new RuntimeException("Unsupported version");
        }
        rate = readRate(input);
        volume = readVolume(input);
        StringReader.sureSkip(input, 10);
        matrix = readMatrix(input);
        StringReader.sureSkip(input, 24);
        nextTrackId = (int) readInt32(input);
    }

    private int[] readMatrix(InputStream input) throws IOException {
        int[] matrix = new int[9];
        for (int i = 0; i < 9; i++)
            matrix[i] = (int) readInt32(input);
        return matrix;
    }

    private float readVolume(InputStream input) throws IOException {
        long val = readInt16(input);
        return (float) val / 256f;
    }

    private float readRate(InputStream input) throws IOException {
        long val = readInt32(input);
        return (float) val / 65536f;
    }

    public void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt(toMovTime(created));
        out.writeInt(toMovTime(modified));
        out.writeInt(timescale);
        out.writeInt((int) duration);
        writeFixed1616(out, rate);
        writeFixed88(out, volume);
        out.write(new byte[10]);
        writeMatrix(out);
        out.write(new byte[24]);
        out.writeInt(nextTrackId);
    }

    private void writeMatrix(DataOutput out) throws IOException {
        for (int i = 0; i < Math.min(9, matrix.length); i++)
            out.writeInt(matrix[i]);
        for (int i = Math.min(9, matrix.length); i < 9; i++)
            out.writeInt(0);
    }

    private void writeFixed88(DataOutput out, float volume) throws IOException {
        out.writeShort((int) (volume * 256.));
    }

    private void writeFixed1616(DataOutput out, float rate) throws IOException {
        out.writeInt((int) (rate * 65536.));
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