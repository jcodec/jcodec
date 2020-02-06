package org.jcodec.common;

import java.nio.ByteBuffer;

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
    private Codec codec;
    private double totalDuration;
    private int[] seekFrames;
    private int totalFrames;
    private ByteBuffer codecPrivate;
    private VideoCodecMeta videoCodecMeta;
    private AudioCodecMeta audioCodecMeta;
    private int index;
    private Orientation orientation;

	public enum Orientation {
		D_0, D_90, D_180, D_270
	}

    public DemuxerTrackMeta(TrackType type, Codec codec, double totalDuration, int[] seekFrames, int totalFrames, ByteBuffer codecPrivate, VideoCodecMeta videoCodecMeta, AudioCodecMeta audioCodecMeta) {
        this.type = type;
        this.codec = codec;
        this.totalDuration = totalDuration;
        this.seekFrames = seekFrames;
        this.totalFrames = totalFrames;
        this.codecPrivate = codecPrivate;
        this.videoCodecMeta = videoCodecMeta;
        this.audioCodecMeta = audioCodecMeta;
        this.orientation = Orientation.D_0;
    }

    public TrackType getType() {
        return type;
    }

    public Codec getCodec() {
        return codec;
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
    
    public void setIndex(int index) {
        this.index = index;
    }

    public ByteBuffer getCodecPrivate() {
        return codecPrivate;
    }

    public VideoCodecMeta getVideoCodecMeta() {
        return videoCodecMeta;
    }

    public AudioCodecMeta getAudioCodecMeta() {
        return audioCodecMeta;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public Orientation getOrientation() {
        return orientation;
    }
}
