package org.jcodec.containers.mp4.boxes;

import static org.jcodec.containers.mp4.TimeUtil.fromMovTime;
import static org.jcodec.containers.mp4.TimeUtil.toMovTime;

import java.nio.ByteBuffer;

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

    public void parse(ByteBuffer input) {
        super.parse(input);

        if (version == 0) {
            created = fromMovTime(input.getInt()); // Creation time
            modified = fromMovTime(input.getInt()); // Modification time
        } else {
            created = fromMovTime(input.getLong());
            modified = fromMovTime(input.getLong());
        }
        trackId = input.getInt();
        input.getInt();

        if (version == 0) {
            duration = input.getInt();
        } else {
            duration = input.getLong();
        }

        input.getInt(); // Reserved
        input.getInt();

        layer = input.getShort();
        altGroup = input.getShort();

        volume = readVolume(input);

        input.getShort();

        readMatrix(input);

        width = input.getInt() / 65536f;
        height = input.getInt() / 65536f;
    }

    private void readMatrix(ByteBuffer input) {
        matrix = new int[9];
        for (int i = 0; i < 9; i++)
            matrix[i] = input.getInt();
    }

    private float readVolume(ByteBuffer input) {
        return (float) (input.getShort() / 256.);
    }

    public int getNo() {
        return trackId;
    }
    
    public long getCreated() {
    	return created;
    }

    public long getDuration() {
    	return duration;
    }
    
    public long getModified() {
    	return modified;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void doWrite(ByteBuffer out) {
        super.doWrite(out);

        out.putInt(toMovTime(created));
        out.putInt(toMovTime(modified));

        out.putInt(trackId);
        out.putInt(0);

        out.putInt((int) duration);

        out.putInt(0);
        out.putInt(0);

        out.putShort((short) layer);
        out.putShort((short) altGroup);

        writeVolume(out);

        out.putShort((short) 0);

        writeMatrix(out);

        out.putInt((int) (width * 65536));
        out.putInt((int) (height * 65536));
    }

    private void writeMatrix(ByteBuffer out) {
        for (int i = 0; i < 9; i++)
            out.putInt(matrix[i]);

    }

    private void writeVolume(ByteBuffer out) {
        out.putShort((short) (volume * 256.));
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