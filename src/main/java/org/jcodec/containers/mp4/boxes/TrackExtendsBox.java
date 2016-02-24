package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Movie fragment header box
 * 
 * 
 * @author The JCodec project
 * 
 */
public class TrackExtendsBox extends FullBox {

    public TrackExtendsBox(Header atom) {
        super(atom);
    }

    private int trackId;
    private int defaultSampleDescriptionIndex;
    private int defaultSampleDuration;
    private int defaultSampleBytes;
    private int defaultSampleFlags;
 
    public static String fourcc() {
        return "trex";
    }

    @Override
    public void parse(ByteBuffer input) {
        super.parse(input);
        trackId = input.getInt();
        defaultSampleDescriptionIndex = input.getInt();
        defaultSampleDuration = input.getInt();
        defaultSampleBytes = input.getInt();
        defaultSampleFlags = input.getInt();
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(trackId);
        out.putInt(defaultSampleDescriptionIndex);
        out.putInt(defaultSampleDuration);
        out.putInt(defaultSampleBytes);
        out.putInt(defaultSampleFlags);
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public int getDefaultSampleDescriptionIndex() {
        return defaultSampleDescriptionIndex;
    }

    public void setDefaultSampleDescriptionIndex(int defaultSampleDescriptionIndex) {
        this.defaultSampleDescriptionIndex = defaultSampleDescriptionIndex;
    }

    public int getDefaultSampleDuration() {
        return defaultSampleDuration;
    }

    public void setDefaultSampleDuration(int defaultSampleDuration) {
        this.defaultSampleDuration = defaultSampleDuration;
    }

    public int getDefaultSampleBytes() {
        return defaultSampleBytes;
    }

    public void setDefaultSampleBytes(int defaultSampleBytes) {
        this.defaultSampleBytes = defaultSampleBytes;
    }

    public int getDefaultSampleFlags() {
        return defaultSampleFlags;
    }

    public void setDefaultSampleFlags(int defaultSampleFlags) {
        this.defaultSampleFlags = defaultSampleFlags;
    }

}
