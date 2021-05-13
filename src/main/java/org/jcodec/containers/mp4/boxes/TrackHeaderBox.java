package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import static org.jcodec.containers.mp4.TimeUtil.fromMovTime;
import static org.jcodec.containers.mp4.TimeUtil.toMovTime;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
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

    public static TrackHeaderBox createTrackHeaderBox(int trackId, long duration, float width, float height,
            long created, long modified, float volume, short layer, long altGroup, int[] matrix) {
        TrackHeaderBox box = new TrackHeaderBox(new Header(fourcc()));
        box.trackId = trackId;
        box.duration = duration;
        box.width = width;
        box.height = height;
        box.created = created;
        box.modified = modified;
        box.volume = volume;
        box.layer = layer;
        box.altGroup = altGroup;
        box.matrix = matrix;
        return box;
    }

    public TrackHeaderBox(Header header) {
        super(header);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);

        if (version == 0) {
            created = fromMovTime(input.getInt()); // Creation time
            modified = fromMovTime(input.getInt()); // Modification time
        } else {
            created = fromMovTime((int) input.getLong());
            modified = fromMovTime((int) input.getLong());
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
    @AtomField(idx=1)
    public long getDuration() {
        return duration;
    }
    @AtomField(idx=2)
    public float getWidth() {
        return width;
    }
    @AtomField(idx=3)
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
    
    @Override
    public int estimateSize() {
        return 92;
    }

    private void writeMatrix(ByteBuffer out) {
        for (int i = 0; i < Math.min(9, matrix.length); i++)
            out.putInt(matrix[i]);
        for (int i = Math.min(9, matrix.length); i < 9; i++)
            out.putInt(0);
    }

    private void writeVolume(ByteBuffer out) {
        out.putShort((short) (volume * 256.));
    }

    @AtomField(idx=0)
    public int getTrackId() {
        return trackId;
    }

    @AtomField(idx=4)
    public long getCreated() {
        return created;
    }

    @AtomField(idx=5)
    public long getModified() {
        return modified;
    }

    @AtomField(idx=6)
    public float getVolume() {
        return volume;
    }

    @AtomField(idx=7)
    public short getLayer() {
        return layer;
    }

    @AtomField(idx=8)
    public long getAltGroup() {
        return altGroup;
    }

    @AtomField(idx=9)
    public int[] getMatrix() {
        return matrix;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setNo(int no) {
        this.trackId = no;
    }

    public boolean isOrientation0() { return matrix != null && matrix[0] == 65536 && matrix[4] == 65536;}
    public boolean isOrientation90() { return matrix != null && matrix[1] == 65536 && matrix[3] == -65536;}
    public boolean isOrientation180() { return matrix != null && matrix[0] == -65536 && matrix[4] == -65536;}
    public boolean isOrientation270() { return matrix != null && matrix[1] == -65536 && matrix[3] == 65536;}
}