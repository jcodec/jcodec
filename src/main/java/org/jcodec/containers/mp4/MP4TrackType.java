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
public final class MP4TrackType {
    public final static MP4TrackType VIDEO = new MP4TrackType("vide");
    public final static MP4TrackType SOUND = new MP4TrackType("soun");
    public final static MP4TrackType TIMECODE = new MP4TrackType("tmcd");
    public final static MP4TrackType HINT = new MP4TrackType("hint");
    public final static MP4TrackType TEXT = new MP4TrackType("text");
    public final static MP4TrackType HYPER_TEXT = new MP4TrackType("wtxt");
    public final static MP4TrackType CC = new MP4TrackType("clcp");
    public final static MP4TrackType SUB = new MP4TrackType("sbtl");
    public final static MP4TrackType MUSIC = new MP4TrackType("musi");
    public final static MP4TrackType MPEG1 = new MP4TrackType("MPEG");
    public final static MP4TrackType SPRITE = new MP4TrackType("sprt");
    public final static MP4TrackType TWEEN = new MP4TrackType("twen");
    public final static MP4TrackType CHAPTERS = new MP4TrackType("chap");
    public final static MP4TrackType THREE_D = new MP4TrackType("qd3d");
    public final static MP4TrackType STREAMING = new MP4TrackType("strm");
    public final static MP4TrackType OBJECTS = new MP4TrackType("obje");

    private final static MP4TrackType[] _values = new MP4TrackType[] { VIDEO, SOUND, TIMECODE, HINT, TEXT, HYPER_TEXT, CC,
            SUB, MUSIC, MPEG1, SPRITE, TWEEN, CHAPTERS, THREE_D, STREAMING, OBJECTS };

    private String handler;

    private MP4TrackType(String handler) {
        this.handler = handler;
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
}
