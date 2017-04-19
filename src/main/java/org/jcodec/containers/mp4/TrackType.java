package org.jcodec.containers.mp4;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 * 
 */
public final class TrackType {
    public final static TrackType VIDEO = new TrackType("vide");
    public final static TrackType SOUND = new TrackType("soun");
    public final static TrackType TIMECODE = new TrackType("tmcd");
    public final static TrackType HINT = new TrackType("hint");
    public final static TrackType TEXT = new TrackType("text");
    public final static TrackType HYPER_TEXT = new TrackType("wtxt");
    public final static TrackType CC = new TrackType("clcp");
    public final static TrackType SUB = new TrackType("sbtl");
    public final static TrackType MUSIC = new TrackType("musi");
    public final static TrackType MPEG1 = new TrackType("MPEG");
    public final static TrackType SPRITE = new TrackType("sprt");
    public final static TrackType TWEEN = new TrackType("twen");
    public final static TrackType CHAPTERS = new TrackType("chap");
    public final static TrackType THREE_D = new TrackType("qd3d");
    public final static TrackType STREAMING = new TrackType("strm");
    public final static TrackType OBJECTS = new TrackType("obje");
    public final static TrackType DATA = new TrackType("url ");

    private final static TrackType[] _values = new TrackType[] { VIDEO, SOUND, TIMECODE, HINT, TEXT, HYPER_TEXT, CC,
            SUB, MUSIC, MPEG1, SPRITE, TWEEN, CHAPTERS, THREE_D, STREAMING, OBJECTS, DATA };

    private String handler;

    private TrackType(String handler) {
        this.handler = handler;
    }

    public String getHandler() {
        return handler;
    }

    public static TrackType fromHandler(String handler) {
        for (int i = 0; i < _values.length; i++) {
            TrackType val = _values[i];
            if (val.getHandler().equals(handler))
                return val;
        }
        return null;
    }
}
