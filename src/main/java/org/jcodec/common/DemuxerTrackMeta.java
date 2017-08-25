package org.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Meta information about this media track.
 * 
 * @author The JCodec project
 * 
 */
public class DemuxerTrackMeta {
    private TrackType type;
    private double totalDuration;
    private int[] seekFrames;
    private int totalFrames;
    private CodecMeta codecMeta;
    private int index;
    private Orientation orientation;

    public enum Orientation {
        D_0, D_90, D_180, D_270
    }

    public DemuxerTrackMeta(TrackType type, double totalDuration, int[] seekFrames, int totalFrames,
            CodecMeta codecMeta) {
        this.type = type;
        this.totalDuration = totalDuration;
        this.seekFrames = seekFrames;
        this.totalFrames = totalFrames;
        this.codecMeta = codecMeta;
        this.orientation = Orientation.D_0;
    }

    public TrackType getType() {
        return type;
    }

    /**
     * @return Total duration in seconds of the media track
     */
    public double getTotalDuration() {
        return totalDuration;
    }

    /**
     * @return Array of frame indexes that can be used to seek to, i.e. which
     *         don't require any previous frames to be decoded. Is null when
     *         every frame is a seek frame.
     */
    public int[] getSeekFrames() {
        return seekFrames;
    }

    /**
     * @return Total number of frames in this media track.
     */
    public int getTotalFrames() {
        return totalFrames;
    }

    public int getIndex() {
        return index;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public CodecMeta getCodecMeta() {
        return codecMeta;
    }
}
