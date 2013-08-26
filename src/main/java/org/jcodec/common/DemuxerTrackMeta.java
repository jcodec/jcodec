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

    public static enum Type {
        VIDEO, AUDIO, OTHER
    };

    private Type type;
    private int[] seekFrames;
    private int totalFrames;
    private double totalDuration;

    public DemuxerTrackMeta(Type type, int[] seekFrames, int totalFrames, double totalDuration) {
        this.type = type;
        this.seekFrames = seekFrames;
        this.totalFrames = totalFrames;
        this.totalDuration = totalDuration;
    }

    public Type getType() {
        return type;
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

    /**
     * @return Total duration in seconds of the media track
     */
    public double getTotalDuration() {
        return totalDuration;
    }
}