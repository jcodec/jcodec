package org.jcodec.containers.mp4;

import org.jcodec.common.TrackType;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public final class MP4TrackType {
    public final static MP4TrackType VIDEO = new MP4TrackType("vide", TrackType.VIDEO);
    public final static MP4TrackType SOUND = new MP4TrackType("soun", TrackType.AUDIO);
    public final static MP4TrackType TIMECODE = new MP4TrackType("tmcd", TrackType.OTHER);
    public final static MP4TrackType HINT = new MP4TrackType("hint", TrackType.OTHER);
    public final static MP4TrackType TEXT = new MP4TrackType("text", TrackType.OTHER);
    public final static MP4TrackType HYPER_TEXT = new MP4TrackType("wtxt", TrackType.OTHER);
    public final static MP4TrackType CC = new MP4TrackType("clcp", TrackType.OTHER);
    public final static MP4TrackType SUB = new MP4TrackType("sbtl", TrackType.OTHER);
    public final static MP4TrackType MUSIC = new MP4TrackType("musi", TrackType.AUDIO);
    public final static MP4TrackType MPEG1 = new MP4TrackType("MPEG", TrackType.VIDEO);
    public final static MP4TrackType SPRITE = new MP4TrackType("sprt", TrackType.OTHER);
    public final static MP4TrackType TWEEN = new MP4TrackType("twen", TrackType.OTHER);
    public final static MP4TrackType CHAPTERS = new MP4TrackType("chap", TrackType.OTHER);
    public final static MP4TrackType THREE_D = new MP4TrackType("qd3d", TrackType.OTHER);
    public final static MP4TrackType STREAMING = new MP4TrackType("strm", TrackType.OTHER);
    public final static MP4TrackType OBJECTS = new MP4TrackType("obje", TrackType.OTHER);
    public final static MP4TrackType DATA = new MP4TrackType("url ", TrackType.OTHER);
    public final static MP4TrackType META = new MP4TrackType("meta", TrackType.META);

    private final static MP4TrackType[] _values = new MP4TrackType[] { VIDEO, SOUND, TIMECODE, HINT, TEXT, HYPER_TEXT, CC,
            SUB, MUSIC, MPEG1, SPRITE, TWEEN, CHAPTERS, THREE_D, STREAMING, OBJECTS, DATA, META };

    private String handler;
    private TrackType trackType;

    private MP4TrackType(String handler, TrackType trackType) {
        this.handler = handler;
        this.trackType = trackType;
    }

    public String getHandler() {
        return handler;
    }

    public static MP4TrackType fromHandler(String handler) {
        for (int i = 0; i < _values.length; i++) {
            MP4TrackType val = _values[i];
            if (val.getHandler().equals(handler))
                return val;
        }
        return null;
    }

    public TrackType getTrackType() {
        return trackType;
    }
}
