package org.jcodec.common;

import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;

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
    private Codec codec;
    private int[] seekFrames;
    private int totalFrames;
    private double totalDuration;
    private Size dimensions;
    private byte[] codecPrivate;
    private Rational pixelAspectRatio;

    public DemuxerTrackMeta(Type type, Codec codec, int[] seekFrames, int totalFrames, double totalDuration, Size dimensions, byte[] codecPrivate) {
        this.type = type;
        this.codec = codec;
        this.seekFrames = seekFrames;
        this.totalFrames = totalFrames;
        this.totalDuration = totalDuration;
        this.dimensions = dimensions;
        this.codecPrivate = codecPrivate;
    }

    public Type getType() {
        return type;
    }
    
    public Codec getCodec() {
        return codec;
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

    public Size getDimensions() {
        return dimensions;
    }

    public byte[] getCodecPrivate() {
        return codecPrivate;
    }

    public Rational getPixelAspectRatio() {
        return pixelAspectRatio;
    }

    public void setPixelAspectRatio(Rational pixelAspectRatio) {
        this.pixelAspectRatio = pixelAspectRatio;
    }
}