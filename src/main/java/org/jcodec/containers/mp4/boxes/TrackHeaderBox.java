package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.io.ReaderBE.readInt32;
import static org.jcodec.common.io.ReaderBE.readInt64;
import static org.jcodec.containers.mp4.TimeUtil.fromMovTime;
import static org.jcodec.containers.mp4.TimeUtil.toMovTime;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;
import org.jcodec.common.tools.ToJSON;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class TrackHeaderBox extends FullBox {
    private int trackId;
    private long duration;
    private float width;
    private float height;
    private long created;
    private long modified;
    private float volume;
    private short layer;
    private long altGroup;
    private int[] matrix;

    public static String fourcc() {
        return "tkhd";
    }

    public TrackHeaderBox(int trackId, long duration, float width, float height, long created, long modified,
            float volume, short layer, long altGroup, int[] matrix) {
        super(new Header(fourcc()));
        this.trackId = trackId;
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.created = created;
        this.modified = modified;
        this.volume = volume;
        this.layer = layer;
        this.altGroup = altGroup;
        this.matrix = matrix;
    }

    public TrackHeaderBox() {
        super(new Header(fourcc()));
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);

        if (version == 0) {
            created = fromMovTime((int) readInt32(input)); // Creation time
            modified = fromMovTime((int) readInt32(input)); // Modification time
        } else {
            created = fromMovTime((int) readInt64(input));
            modified = fromMovTime((int) readInt64(input));
        }
        trackId = (int) readInt32(input);
        readInt32(input);

        if (version == 0) {
            duration = readInt32(input);
        } else {
            duration = readInt64(input);
        }

        ReaderBE.readInt32(input); // Reserved
        ReaderBE.readInt32(input);

        layer = (short) ReaderBE.readInt16(input);
        altGroup = ReaderBE.readInt16(input);

        volume = readVolume(input);

        ReaderBE.readInt16(input);

        readMatrix(input);

        width = ReaderBE.readInt32(input) / 65536f;
        height = ReaderBE.readInt32(input) / 65536f;
    }

    private void readMatrix(InputStream input) throws IOException {
        matrix = new int[9];
        for (int i = 0; i < 9; i++)
            matrix[i] = (int) ReaderBE.readInt32(input);
    }

    private float readVolume(InputStream input) throws IOException {
        return (float) (ReaderBE.readInt16(input) / 256.);
    }

    public int getNo() {
        return trackId;
    }

    public long getDuration() {
        return duration;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);

        out.writeInt(toMovTime(created));
        out.writeInt(toMovTime(modified));

        out.writeInt(trackId);
        out.writeInt(0);

        out.writeInt((int) duration);

        out.writeInt(0);
        out.writeInt(0);

        out.writeShort((short) layer);
        out.writeShort((short) altGroup);

        writeVolume(out);

        out.writeShort(0);

        writeMatrix(out);

        out.writeInt((int) (width * 65536));
        out.writeInt((int) (height * 65536));
    }

    private void writeMatrix(DataOutput out) throws IOException {
        for (int i = 0; i < 9; i++)
            out.writeInt(matrix[i]);

    }

    private void writeVolume(DataOutput out) throws IOException {
        out.writeShort((short) (volume * 256.));
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setNo(int no) {
        this.trackId = no;
    }

    public int[] getMatrix() {
        return matrix;
    }

    public short getLayer() {
        return layer;
    }

    public float getVolume() {
        return volume;
    }

    public void setWidth(float width2) {
        this.width = width2;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    @Override
    protected void dump(StringBuilder sb) {
        super.dump(sb);
        sb.append(": ");
        ToJSON.toJSON(this, sb, "trackId", "duration", "width", "height", "created", "modified", "volume", "layer",
                "altGroup");
    }
}